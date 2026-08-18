"""User settings + broker connection (Delta India)."""
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException

from auth import get_current_user
from db import broker_connections, user_settings, log_activity
from models import BrokerConnectionIn, BrokerConnectionOut, SettingsIn, SettingsOut
from security import encrypt_secret, decrypt_secret, mask_secret
import delta_client

router = APIRouter(prefix="/api/settings", tags=["settings"])

DEFAULTS = SettingsOut().model_dump()


@router.get("", response_model=SettingsOut)
async def get_settings(user=Depends(get_current_user)):
    doc = await user_settings.find_one({"user_id": user["user_id"]}, {"_id": 0}) or {}
    merged = {**DEFAULTS, **{k: v for k, v in doc.items() if k in DEFAULTS}}
    # Backfill symbols from legacy default_symbol if user hasn't set the multi-symbol field
    if not merged.get("symbols"):
        merged["symbols"] = [merged.get("default_symbol") or "BTCUSD"]
    # Normalize
    merged["symbols"] = [s.strip().upper() for s in merged["symbols"] if s and str(s).strip()]
    if not merged.get("default_symbol") and merged["symbols"]:
        merged["default_symbol"] = merged["symbols"][0]
    return SettingsOut(**merged)


@router.put("", response_model=SettingsOut)
async def put_settings(payload: SettingsIn, user=Depends(get_current_user)):
    update = {k: v for k, v in payload.model_dump(exclude_none=True).items()}
    if "symbols" in update:
        clean = []
        seen = set()
        for s in update["symbols"]:
            s2 = str(s).strip().upper()
            if s2 and s2 not in seen:
                clean.append(s2)
                seen.add(s2)
        update["symbols"] = clean
        if clean and not update.get("default_symbol"):
            update["default_symbol"] = clean[0]
    if update:
        update["updated_at"] = datetime.now(timezone.utc).isoformat()
        await user_settings.update_one(
            {"user_id": user["user_id"]},
            {"$set": {"user_id": user["user_id"], **update}},
            upsert=True,
        )
    return await get_settings(user)


@router.get("/broker", response_model=BrokerConnectionOut)
async def get_broker(user=Depends(get_current_user)):
    doc = await broker_connections.find_one({"user_id": user["user_id"]}, {"_id": 0})
    if not doc or not doc.get("api_key"):
        return BrokerConnectionOut(configured=False)
    return BrokerConnectionOut(
        configured=True,
        api_key_masked=mask_secret(doc["api_key"]),
        updated_at=doc.get("updated_at"),
    )


@router.put("/broker", response_model=BrokerConnectionOut)
async def put_broker(payload: BrokerConnectionIn, user=Depends(get_current_user)):
    now = datetime.now(timezone.utc).isoformat()
    await broker_connections.update_one(
        {"user_id": user["user_id"]},
        {"$set": {
            "user_id": user["user_id"],
            "exchange": "delta_india",
            "api_key": payload.api_key,
            "api_secret_enc": encrypt_secret(payload.api_secret),
            "updated_at": now,
        }},
        upsert=True,
    )
    await log_activity(user["user_id"], "info", "broker_connected",
                       "Delta India credentials saved", {"api_key_masked": mask_secret(payload.api_key)})
    return BrokerConnectionOut(configured=True, api_key_masked=mask_secret(payload.api_key), updated_at=now)


@router.post("/broker/test")
async def test_broker(user=Depends(get_current_user)):
    doc = await broker_connections.find_one({"user_id": user["user_id"]}, {"_id": 0})
    if not doc or not doc.get("api_key"):
        raise HTTPException(status_code=400, detail="No broker configured")
    try:
        api_secret = decrypt_secret(doc.get("api_secret_enc", ""))
        wallet = await delta_client.get_wallet(doc["api_key"], api_secret)
        await log_activity(user["user_id"], "info", "broker_test_ok", "Delta wallet reachable")
        return {"ok": True, "wallet": wallet.get("result", wallet)}
    except delta_client.DeltaError as e:
        await log_activity(user["user_id"], "error", "broker_test_fail", str(e.detail))
        raise HTTPException(status_code=e.status, detail=e.detail)


@router.delete("/broker")
async def delete_broker(user=Depends(get_current_user)):
    await broker_connections.delete_one({"user_id": user["user_id"]})
    await log_activity(user["user_id"], "warn", "broker_removed", "Delta credentials removed")
    return {"ok": True}


async def get_broker_creds(user_id: str) -> tuple[str, str]:
    """Internal helper used by trading routes."""
    doc = await broker_connections.find_one({"user_id": user_id}, {"_id": 0})
    if not doc or not doc.get("api_key"):
        raise HTTPException(status_code=400,
                            detail="Delta India credentials not configured. Add them under Settings.")
    return doc["api_key"], decrypt_secret(doc.get("api_secret_enc", ""))
