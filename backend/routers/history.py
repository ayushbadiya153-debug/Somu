"""Local trade history (persisted). Optionally merges with Delta order history."""
from fastapi import APIRouter, Depends, HTTPException, Query
from auth import get_current_user
from db import trades
from routers.settings import get_broker_creds
import delta_client

router = APIRouter(prefix="/api/history", tags=["history"])


@router.get("")
async def get_history(limit: int = Query(200, le=500), user=Depends(get_current_user)):
    cursor = trades.find({"user_id": user["user_id"]}, {"_id": 0}).sort("created_at", -1).limit(limit)
    items = await cursor.to_list(length=limit)
    return {"trades": items}


@router.get("/exchange")
async def get_exchange_history(user=Depends(get_current_user)):
    try:
        api_key, api_secret = await get_broker_creds(user["user_id"])
    except HTTPException as e:
        raise e
    try:
        data = await delta_client.get_order_history(api_key, api_secret, page_size=100)
        return {"orders": data.get("result", [])}
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)
