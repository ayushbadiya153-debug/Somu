"""Strategy list + enable/disable. Actual strategy logic is deferred (user will supply)."""
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException

from auth import get_current_user
from db import strategies, log_activity
from models import StrategyToggle, StrategyOut

router = APIRouter(prefix="/api/strategies", tags=["strategies"])

BUILT_IN = [
    {"key": "ema_cross", "name": "EMA Crossover",
     "description": "Fast EMA crossing slow EMA on the closed bar. Long on up-cross, short on down-cross.",
     "params": {"fast": 9, "slow": 21, "size": 1, "atr_period": 14, "sl_atr_mult": 1.0, "tp_atr_mult": 2.1}},
    {"key": "rsi_reversion", "name": "RSI Mean Reversion",
     "description": "Buys when RSI closes back above the oversold line, shorts when RSI closes back below overbought.",
     "params": {"period": 14, "oversold": 30, "overbought": 70, "size": 1, "atr_period": 14, "sl_atr_mult": 1.0, "tp_atr_mult": 2.1}},
    {"key": "breakout", "name": "Range Breakout",
     "description": "Long on close above N-bar high, short on close below N-bar low.",
     "params": {"lookback": 20, "size": 1, "atr_period": 14, "sl_atr_mult": 1.0, "tp_atr_mult": 2.1}},
]


async def _ensure_user_strategies(user_id: str):
    existing = {}
    async for s in strategies.find({"user_id": user_id}, {"_id": 0}):
        existing[s["key"]] = s
    for s in BUILT_IN:
        if s["key"] not in existing:
            await strategies.insert_one({
                "user_id": user_id,
                "key": s["key"],
                "name": s["name"],
                "description": s["description"],
                "enabled": False,
                "params": s["params"],
                "created_at": datetime.now(timezone.utc).isoformat(),
            })
        else:
            # Backfill any missing default params and refresh description/name.
            cur = existing[s["key"]]
            merged = {**s["params"], **(cur.get("params") or {})}
            await strategies.update_one(
                {"user_id": user_id, "key": s["key"]},
                {"$set": {"params": merged, "description": s["description"], "name": s["name"]}},
            )


@router.get("")
async def list_strategies(user=Depends(get_current_user)):
    await _ensure_user_strategies(user["user_id"])
    docs = await strategies.find({"user_id": user["user_id"]}, {"_id": 0}).to_list(length=100)
    return {"strategies": [
        StrategyOut(
            key=d["key"], name=d["name"], description=d["description"],
            enabled=bool(d.get("enabled")), params=d.get("params") or {},
        ).model_dump()
        for d in docs
    ]}


@router.post("/{key}/toggle")
async def toggle_strategy(key: str, payload: StrategyToggle, user=Depends(get_current_user)):
    await _ensure_user_strategies(user["user_id"])
    res = await strategies.update_one(
        {"user_id": user["user_id"], "key": key},
        {"$set": {"enabled": payload.enabled, "updated_at": datetime.now(timezone.utc).isoformat()}},
    )
    if res.matched_count == 0:
        raise HTTPException(status_code=404, detail="Strategy not found")
    await log_activity(user["user_id"], "info", "strategy_toggled",
                       f"Strategy {key} {'enabled' if payload.enabled else 'disabled'}")
    return {"ok": True, "key": key, "enabled": payload.enabled}


@router.put("/{key}/params")
async def update_params(key: str, params: dict, user=Depends(get_current_user)):
    await _ensure_user_strategies(user["user_id"])
    res = await strategies.update_one(
        {"user_id": user["user_id"], "key": key},
        {"$set": {"params": params, "updated_at": datetime.now(timezone.utc).isoformat()}},
    )
    if res.matched_count == 0:
        raise HTTPException(status_code=404, detail="Strategy not found")
    return {"ok": True}
