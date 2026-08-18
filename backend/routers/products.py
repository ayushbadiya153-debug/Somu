"""Delta India products (public)."""
from fastapi import APIRouter, Depends, HTTPException, Query
from auth import get_current_user
import delta_client

router = APIRouter(prefix="/api/products", tags=["products"])


@router.get("")
async def list_products(
    contract_types: str = Query("perpetual_futures,call_options,put_options"),
    _user=Depends(get_current_user),
):
    try:
        # Delta returns paginated results; request a larger page so BTC/ETH etc. are included.
        data = await delta_client.request(
            "", "", "GET", "/v2/products",
            params={"contract_types": contract_types, "states": "live", "page_size": 500},
            authenticated=False,
        )
        result = data.get("result", [])
        # Keep essential fields only to reduce payload
        products = [
            {
                "id": p.get("id"),
                "symbol": p.get("symbol"),
                "description": p.get("description"),
                "contract_type": p.get("contract_type"),
                "underlying_asset": (p.get("underlying_asset") or {}).get("symbol"),
                "quoting_asset": (p.get("quoting_asset") or {}).get("symbol"),
                "tick_size": p.get("tick_size"),
                "state": p.get("state"),
            }
            for p in result
            if p.get("state") == "live"
        ]
        return {"products": products}
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)


@router.get("/top")
async def top_perpetuals(
    limit: int = Query(15, ge=1, le=50),
    _user=Depends(get_current_user),
):
    """Top perpetual futures ranked by 24h turnover (public tickers endpoint)."""
    try:
        data = await delta_client.request(
            "", "", "GET", "/v2/tickers",
            params={"contract_types": "perpetual_futures"},
            authenticated=False,
        )
    except delta_client.DeltaError as e:
        raise HTTPException(status_code=e.status, detail=e.detail)

    def turnover(t):
        try:
            return float(t.get("turnover_usd") or t.get("turnover") or 0)
        except Exception:
            return 0.0

    tickers = data.get("result", []) or []
    tickers.sort(key=turnover, reverse=True)
    top = []
    for t in tickers[:limit]:
        try:
            price = float(t.get("mark_price") or 0)
        except Exception:
            price = 0.0
        try:
            cv = float(t.get("contract_value") or 0)
        except Exception:
            cv = 0.0
        top.append({
            "symbol": t.get("symbol"),
            "product_id": t.get("product_id"),
            "mark_price": price,
            "turnover_usd": turnover(t),
            "contract_value": cv,
            "notional_per_lot": round(cv * price, 4) if cv and price else None,
        })
    return {"top": top}
