"""Aggregated dashboard metrics: virtual account + real broker snapshot."""
from fastapi import APIRouter, Depends
from auth import get_current_user
from db import trades, user_settings, broker_connections, engine_states
from security import decrypt_secret
import delta_client
import engine_runtime

router = APIRouter(prefix="/api/dashboard", tags=["dashboard"])


DEFAULT_CAPITAL = 100000.0


@router.get("")
async def dashboard(user=Depends(get_current_user)):
    user_id = user["user_id"]

    settings_doc = await user_settings.find_one({"user_id": user_id}, {"_id": 0}) or {}
    virtual_capital = float(settings_doc.get("virtual_capital", DEFAULT_CAPITAL))

    # Local trade stats
    total_trades = await trades.count_documents({"user_id": user_id})
    realized_pnl = 0.0
    async for t in trades.find({"user_id": user_id, "realized_pnl": {"$ne": None}}, {"_id": 0, "realized_pnl": 1}):
        try:
            realized_pnl += float(t["realized_pnl"] or 0)
        except Exception:
            pass

    virtual_cash = round(virtual_capital + realized_pnl, 2)

    # Real broker snapshot
    broker_doc = await broker_connections.find_one({"user_id": user_id}, {"_id": 0})
    broker_configured = bool(broker_doc and broker_doc.get("api_key"))

    positions_list = []
    unrealized_pnl = 0.0
    wallet_balance = None
    wallet_available = None
    wallet_currency = None
    wallet_all = []
    broker_error = None

    if broker_configured:
        try:
            api_key = broker_doc["api_key"]
            api_secret = decrypt_secret(broker_doc.get("api_secret_enc", ""))
            pos = await delta_client.get_positions(api_key, api_secret)
            for p in pos.get("result", []):
                size = p.get("size") or 0
                if not size:
                    continue
                try:
                    upnl = float(p.get("unrealized_pnl") or 0)
                except Exception:
                    upnl = 0.0
                unrealized_pnl += upnl
                positions_list.append({
                    "symbol": p.get("product_symbol") or (p.get("product") or {}).get("symbol"),
                    "product_id": (p.get("product") or {}).get("id") or p.get("product_id"),
                    "size": size,
                    "entry_price": p.get("entry_price"),
                    "mark_price": p.get("mark_price"),
                    "unrealized_pnl": upnl,
                    "liquidation_price": p.get("liquidation_price"),
                })
            wallet = await delta_client.get_wallet(api_key, api_secret)
            balances = wallet.get("result", []) or []
            preferred = ["USDT", "USD", "INR", "BTC", "ETH"]
            def _sym(b):
                return b.get("asset_symbol") or (b.get("asset") or {}).get("symbol") or "?"
            def _bal(b):
                return b.get("balance") or b.get("wallet_balance") or "0"
            def _avail(b):
                return b.get("available_balance") or b.get("balance") or "0"
            # Build normalized list of all non-zero balances
            for b in balances:
                sym = _sym(b)
                try:
                    bal = float(_bal(b) or 0)
                except Exception:
                    bal = 0.0
                try:
                    avail = float(_avail(b) or 0)
                except Exception:
                    avail = 0.0
                if bal == 0 and avail == 0:
                    continue
                wallet_all.append({"currency": sym, "balance": bal, "available": avail})
            # Choose primary display currency
            chosen = None
            for pref in preferred:
                for w in wallet_all:
                    if w["currency"] == pref:
                        chosen = w
                        break
                if chosen:
                    break
            if not chosen and wallet_all:
                chosen = max(wallet_all, key=lambda w: w["balance"])
            if chosen:
                wallet_currency = chosen["currency"]
                wallet_balance = chosen["balance"]
                wallet_available = chosen["available"]
        except delta_client.DeltaError as e:
            broker_error = str(e.detail)[:200]
        except Exception as e:
            broker_error = str(e)[:200]

    engine_doc = await engine_states.find_one({"user_id": user_id}, {"_id": 0}) or {}
    risk = await engine_runtime.get_risk_snapshot(user_id)

    # Preview: max lots allowed by notional cap for each configured symbol.
    max_notional_usd = float((settings_doc or {}).get("max_notional_usd") or 100.0)
    symbols_list = (settings_doc or {}).get("symbols") or [
        ((settings_doc or {}).get("default_symbol") or "BTCUSD").upper()
    ]
    symbols_list = [str(s).strip().upper() for s in symbols_list if s]
    notional_preview = {
        "max_notional_usd": max_notional_usd,
        "symbols": [],
    }
    for sym in symbols_list[:12]:
        row = {"symbol": sym, "notional_per_lot": None, "max_lots": None, "ref_price": None, "error": None}
        try:
            product = await delta_client.find_product_by_symbol(sym)
            if not product:
                row["error"] = "symbol not found on Delta India"
            else:
                cv = float(product.get("contract_value") or 0)
                px = await delta_client.get_last_price(sym)
                if cv and px:
                    npl = cv * px
                    row["notional_per_lot"] = round(npl, 4)
                    row["ref_price"] = px
                    row["max_lots"] = int(max_notional_usd // npl) if npl > 0 else 0
        except Exception as e:  # noqa: BLE001
            row["error"] = str(e)[:150]
        notional_preview["symbols"].append(row)

    return {
        "virtual_account_balance": round(virtual_capital + realized_pnl + unrealized_pnl, 2),
        "virtual_cash_available": virtual_cash,
        "realized_pnl": round(realized_pnl, 2),
        "unrealized_pnl": round(unrealized_pnl, 2),
        "total_trades": total_trades,
        "open_positions_count": len(positions_list),
        "open_positions": positions_list,
        "broker": {
            "configured": broker_configured,
            "wallet_balance": wallet_balance,
            "wallet_available": wallet_available,
            "wallet_currency": wallet_currency,
            "wallet_all": wallet_all,
            "error": broker_error,
        },
        "engine": {
            "running": bool(engine_doc.get("running")),
            "mode": engine_doc.get("mode", "paused"),
        },
        "risk": risk,
        "notional_preview": notional_preview,
    }
