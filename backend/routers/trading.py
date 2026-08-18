"""Real trading endpoints: place order, list open orders/positions, cancel."""
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, Query

from auth import get_current_user
from db import trades, engine_states, log_activity
from models import OrderIn
from routers.settings import get_broker_creds
import delta_client

router = APIRouter(prefix="/api", tags=["trading"])


async def _engine_running(user_id: str) -> bool:
    doc = await engine_states.find_one({"user_id": user_id}, {"_id": 0})
    return bool(doc and doc.get("running"))


@router.post("/orders")
async def place_order(order: OrderIn, user=Depends(get_current_user)):
    if order.order_type == "limit_order" and not order.limit_price:
        raise HTTPException(status_code=422, detail="limit_price required for limit_order")

    api_key, api_secret = await get_broker_creds(user["user_id"])
    payload = {
        "product_id": order.product_id,
        "size": order.size,
        "side": order.side,
        "order_type": order.order_type,
        "reduce_only": order.reduce_only,
    }
    if order.order_type == "limit_order":
        payload["limit_price"] = order.limit_price
        payload["time_in_force"] = order.time_in_force or "gtc"

    try:
        result = await delta_client.place_order(api_key, api_secret, payload)
    except delta_client.DeltaError as e:
        await log_activity(user["user_id"], "error", "order_failed",
                           f"{order.side.upper()} {order.symbol or order.product_id} x{order.size} failed",
                           {"detail": e.detail})
        raise HTTPException(status_code=e.status, detail=e.detail)

    r = result.get("result", {}) if isinstance(result, dict) else {}
    trade_doc = {
        "user_id": user["user_id"],
        "mode": "real",
        "exchange": "delta_india",
        "exchange_order_id": r.get("id"),
        "client_order_id": r.get("client_order_id"),
        "symbol": order.symbol or r.get("product_symbol"),
        "product_id": order.product_id,
        "side": order.side,
        "order_type": order.order_type,
        "quantity": order.size,
        "entry_price": r.get("average_fill_price") or r.get("limit_price"),
        "exit_price": None,
        "realized_pnl": None,
        "status": r.get("state") or "submitted",
        "created_at": datetime.now(timezone.utc).isoformat(),
        "raw": r,
    }
    await trades.insert_one(trade_doc)
    await log_activity(user["user_id"], "info", "order_placed",
                       f"{order.side.upper()} {trade_doc['symbol']} x{order.size} @ {order.order_type}",
                       {"order_id": r.get("id"), "state": r.get("state")})
    trade_doc.pop("_id", None)
    return {"ok": True, "order": trade_doc}


@router.get("/orders/open")
async def open_orders(user=Depends(get_current_user)):
    api_key, api_secret = await get_broker_creds(user["user_id"])
    try:
        data = await delta_client.get_open_orders(api_key, api_secret)
        return {"orders": data.get("result", [])}
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)


@router.get("/positions")
async def positions(user=Depends(get_current_user)):
    api_key, api_secret = await get_broker_creds(user["user_id"])
    try:
        data = await delta_client.get_positions(api_key, api_secret)
        return {"positions": data.get("result", [])}
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)


@router.get("/wallet")
async def wallet(user=Depends(get_current_user)):
    api_key, api_secret = await get_broker_creds(user["user_id"])
    try:
        data = await delta_client.get_wallet(api_key, api_secret)
        return {"wallet": data.get("result", [])}
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)
