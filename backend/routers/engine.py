"""Trading engine start/stop.

Starting the engine launches a per-user background task that scans
enabled strategies each POLL_SEC, computes signals on fresh Delta candles,
and places real market orders when signals fire.
"""
from datetime import datetime, timezone
from fastapi import APIRouter, Depends
from auth import get_current_user
from db import engine_states, log_activity
from models import EngineState
import engine_runtime

router = APIRouter(prefix="/api/engine", tags=["engine"])


@router.get("", response_model=EngineState)
async def get_engine(user=Depends(get_current_user)):
    doc = await engine_states.find_one({"user_id": user["user_id"]}, {"_id": 0}) or {}
    return EngineState(
        running=bool(doc.get("running")) and engine_runtime.is_running(user["user_id"]),
        mode=doc.get("mode", "paused"),
        updated_at=doc.get("updated_at"),
        last_error=doc.get("last_error"),
    )


@router.post("/start", response_model=EngineState)
async def start_engine(user=Depends(get_current_user)):
    now = datetime.now(timezone.utc).isoformat()
    await engine_states.update_one(
        {"user_id": user["user_id"]},
        {"$set": {"user_id": user["user_id"], "running": True, "mode": "real",
                  "updated_at": now, "last_error": None}},
        upsert=True,
    )
    engine_runtime.start(user["user_id"])
    await log_activity(user["user_id"], "info", "engine_start", "Trading engine started")
    return EngineState(running=True, mode="real", updated_at=now)


@router.post("/stop", response_model=EngineState)
async def stop_engine(user=Depends(get_current_user)):
    now = datetime.now(timezone.utc).isoformat()
    await engine_states.update_one(
        {"user_id": user["user_id"]},
        {"$set": {"running": False, "mode": "paused", "updated_at": now}},
        upsert=True,
    )
    await engine_runtime.stop(user["user_id"])
    await log_activity(user["user_id"], "info", "engine_stop", "Trading engine stopped")
    return EngineState(running=False, mode="paused", updated_at=now)
