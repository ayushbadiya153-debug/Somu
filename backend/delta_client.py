"""Signed REST client for Delta Exchange India (per-user credentials)."""
import hashlib
import hmac
import json
import os
import time
from typing import Any, Optional
from urllib.parse import urlencode

import httpx

DELTA_BASE_URL = os.environ.get("DELTA_BASE_URL", "https://api.india.delta.exchange").rstrip("/")


class DeltaError(Exception):
    def __init__(self, status: int, detail: Any):
        self.status = status
        self.detail = detail
        super().__init__(f"Delta {status}: {detail}")


def _sign(api_secret: str, method: str, path: str, query: str, body: str) -> tuple[str, str]:
    timestamp = str(int(time.time()))
    prehash = method.upper() + timestamp + path + query + body
    signature = hmac.new(api_secret.encode(), prehash.encode(), hashlib.sha256).hexdigest()
    return timestamp, signature


async def request(
    api_key: str,
    api_secret: str,
    method: str,
    path: str,
    params: Optional[dict] = None,
    payload: Optional[dict] = None,
    authenticated: bool = True,
) -> dict:
    query = ""
    if params:
        cleaned = [(k, v) for k, v in params.items() if v is not None]
        if cleaned:
            query = "?" + urlencode(cleaned)

    body = ""
    if payload is not None:
        body = json.dumps(payload, separators=(",", ":"), ensure_ascii=False)

    headers = {
        "User-Agent": "nexustrade-fastapi/1.0",
        "Accept": "application/json",
    }
    if body:
        headers["Content-Type"] = "application/json"
    if authenticated:
        if not api_key or not api_secret:
            raise DeltaError(400, "Delta API credentials not configured for user")
        timestamp, signature = _sign(api_secret, method, path, query, body)
        headers["api-key"] = api_key
        headers["timestamp"] = timestamp
        headers["signature"] = signature

    url = DELTA_BASE_URL + path + query
    async with httpx.AsyncClient(timeout=15) as client:
        r = await client.request(method.upper(), url, headers=headers, content=body if body else None)
    try:
        data = r.json()
    except Exception:
        data = {"raw": r.text}

    if r.status_code >= 400:
        raise DeltaError(r.status_code, data)
    return data


# --- Public helpers ------------------------------------------------------

async def get_products(contract_types: str = "perpetual_futures,call_options,put_options", page_size: int = 100):
    return await request("", "", "GET", "/v2/products",
                         params={"contract_types": contract_types, "states": "live", "page_size": page_size},
                         authenticated=False)


async def find_product_by_symbol(symbol: str) -> dict | None:
    """Find live product with matching symbol. Public call. Returns full product record."""
    if not symbol:
        return None
    sym_up = symbol.upper()

    # Fast path: /v2/products/{symbol} returns the full product record.
    try:
        r = await request("", "", "GET", f"/v2/products/{sym_up}", authenticated=False)
        res = r.get("result") or {}
        if res.get("id"):
            return res
    except DeltaError:
        pass

    # Fallback: paginated search across perpetuals.
    data = await request("", "", "GET", "/v2/products",
                         params={"contract_types": "perpetual_futures", "states": "live", "page_size": 500},
                         authenticated=False)
    for p in data.get("result", []):
        if (p.get("symbol") or "").upper() == sym_up:
            return p
    # Options fallback
    data = await request("", "", "GET", "/v2/products",
                         params={"contract_types": "call_options,put_options", "states": "live", "page_size": 500},
                         authenticated=False)
    for p in data.get("result", []):
        if (p.get("symbol") or "").upper() == sym_up:
            return p
    return None


async def get_candles(symbol: str, resolution: str, count: int = 200) -> list[dict]:
    """Fetch closed OHLC candles. Public endpoint. Returns ascending time list."""
    import time as _t
    RES_SEC = {
        "1m": 60, "3m": 180, "5m": 300, "15m": 900, "30m": 1800,
        "1h": 3600, "2h": 7200, "4h": 14400, "6h": 21600, "12h": 43200,
        "1d": 86400, "7d": 604800, "1w": 604800, "30d": 2592000,
    }
    step = RES_SEC.get(resolution, 300)
    now = int(_t.time())
    start = now - (count + 2) * step
    data = await request("", "", "GET", "/v2/history/candles",
                         params={"symbol": symbol, "resolution": resolution, "start": start, "end": now},
                         authenticated=False)
    rows = data.get("result", []) or []
    # Normalize: ensure ascending by time, coerce numeric
    out = []
    for r in rows:
        try:
            out.append({
                "time": int(r.get("time") or 0),
                "open": float(r.get("open")),
                "high": float(r.get("high")),
                "low": float(r.get("low")),
                "close": float(r.get("close")),
                "volume": float(r.get("volume") or 0),
            })
        except Exception:
            continue
    out.sort(key=lambda x: x["time"])
    # Drop the current (unclosed) bar if its start >= now - step
    if out and out[-1]["time"] > now - step + 1:
        out = out[:-1]
    return out


async def get_wallet(api_key: str, api_secret: str):
    return await request(api_key, api_secret, "GET", "/v2/wallet/balances")


async def get_positions(api_key: str, api_secret: str):
    return await request(api_key, api_secret, "GET", "/v2/positions/margined")


async def get_open_orders(api_key: str, api_secret: str):
    return await request(api_key, api_secret, "GET", "/v2/orders", params={"state": "open"})


async def get_order_history(api_key: str, api_secret: str, page_size: int = 100):
    return await request(api_key, api_secret, "GET", "/v2/orders/history", params={"page_size": page_size})


async def place_order(api_key: str, api_secret: str, payload: dict):
    return await request(api_key, api_secret, "POST", "/v2/orders", payload=payload)


async def get_ticker(symbol: str) -> dict:
    """Public ticker for a symbol. Returns dict with mark_price, close, spot_price, etc."""
    data = await request("", "", "GET", f"/v2/tickers/{symbol}", authenticated=False)
    return data.get("result") or {}


async def get_last_price(symbol: str) -> Optional[float]:
    try:
        t = await get_ticker(symbol)
    except DeltaError:
        return None
    for key in ("mark_price", "close", "spot_price", "last_price"):
        v = t.get(key)
        if v is not None:
            try:
                return float(v)
            except Exception:
                continue
    return None


async def cancel_order(api_key: str, api_secret: str, order_id: int, product_id: int):
    return await request(api_key, api_secret, "DELETE", "/v2/orders",
                         payload={"id": order_id, "product_id": product_id})
