"""Activity/log panel."""
from fastapi import APIRouter, Depends, Query
from auth import get_current_user
from db import activity_logs

router = APIRouter(prefix="/api/logs", tags=["logs"])


@router.get("")
async def list_logs(limit: int = Query(200, le=500), user=Depends(get_current_user)):
    cursor = activity_logs.find({"user_id": user["user_id"]}, {"_id": 0}).sort("created_at", -1).limit(limit)
    items = await cursor.to_list(length=limit)
    return {"logs": items}
