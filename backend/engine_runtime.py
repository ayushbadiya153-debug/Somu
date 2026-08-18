"""Per-user trading engine with ATR-based SL/TP and risk manager.

Two coroutines run per user while the engine is on:

  * Signal loop (every SIGNAL_POLL_SEC): fetches closed candles, evaluates each
    enabled strategy on the latest closed bar, and — subject to the risk manager —
    places a market entry with SL/TP derived from ATR.

  * Exit loop (every EXIT_POLL_SEC): polls the live mark price for each managed
    open position and market-closes it the moment SL or TP is hit.

Risk rules (per UTC day):
  * At most 4 entries per day (exits don't count).
  * At most 3 SL hits per day. Once reached, no more entries that day.
  * After ANY SL hit, no new entries for 30 minutes.
"""
from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import Any, Optional

import delta_client
from db import (
    broker_connections,
    daily_risk,
    engine_states,
    managed_positions,
    strategies as strategies_col,
    trades,
    user_settings,
    log_activity,
)
from security import decrypt_secret
from strategies_runtime import atr, compute_signal

logger = logging.getLogger("nexustrade.engine")

SIGNAL_POLL_SEC = 30
EXIT_POLL_SEC = 2
MAX_ENTRIES_PER_DAY = 4
MAX_SL_PER_DAY = 3
SL_COOLDOWN_MIN = 30

_tasks: dict[str, dict[str, asyncio.Task]] = {}
_bar_state: dict[str, dict[str, dict[str, Any]]] = {}  # user -> strategy -> {last_bar_time, last_side}


def is_running(user_id: str) -> bool:
    slot = _tasks.get(user_id) or {}
    t = slot.get("signal")
    return bool(t and not t.done())


# --- Risk manager ------------------------------------------------------------

def _today() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d")


async def _get_risk(user_id: str) -> dict:
    doc = await daily_risk.find_one({"user_id": user_id}, {"_id": 0})
    today = _today()
    if not doc or doc.get("date") != today:
        doc = {
            "user_id": user_id,
            "date": today,
            "entries": 0,
            "sl_hits": 0,
            "last_sl_at": None,
            "halted": False,
        }
        await daily_risk.update_one({"user_id": user_id}, {"$set": doc}, upsert=True)
    return doc


async def _increment_entry(user_id: str) -> None:
    await daily_risk.update_one(
        {"user_id": user_id},
        {"$set": {"date": _today()}, "$inc": {"entries": 1}},
        upsert=True,
    )


async def _record_sl(user_id: str) -> None:
    now = datetime.now(timezone.utc).isoformat()
    await daily_risk.update_one(
        {"user_id": user_id},
        {"$set": {"date": _today(), "last_sl_at": now}, "$inc": {"sl_hits": 1}},
        upsert=True,
    )


def _entry_blocked_reason(risk: dict) -> Optional[str]:
    if risk.get("sl_hits", 0) >= MAX_SL_PER_DAY:
        return f"Daily SL cap reached ({MAX_SL_PER_DAY}). Trading halted for the day."
    if risk.get("entries", 0) >= MAX_ENTRIES_PER_DAY:
        return f"Daily entry cap reached ({MAX_ENTRIES_PER_DAY} trades)."
    last_sl = risk.get("last_sl_at")
    if last_sl:
        try:
            last_dt = datetime.fromisoformat(last_sl)
            if last_dt.tzinfo is None:
                last_dt = last_dt.replace(tzinfo=timezone.utc)
            elapsed = (datetime.now(timezone.utc) - last_dt).total_seconds() / 60.0
            if elapsed < SL_COOLDOWN_MIN:
                remain = int(SL_COOLDOWN_MIN - elapsed)
                return f"SL cooldown active ({remain} min remaining)."
        except Exception:
            pass
    return None


async def get_risk_snapshot(user_id: str) -> dict:
    risk = await _get_risk(user_id)
    reason = _entry_blocked_reason(risk)
    return {
        "date": risk.get("date"),
        "entries": risk.get("entries", 0),
        "entries_max": MAX_ENTRIES_PER_DAY,
        "sl_hits": risk.get("sl_hits", 0),
        "sl_max": MAX_SL_PER_DAY,
        "last_sl_at": risk.get("last_sl_at"),
        "blocked_reason": reason,
        "cooldown_min": SL_COOLDOWN_MIN,
    }


# --- Order helpers -----------------------------------------------------------

async def _place_market(api_key: str, api_secret: str, product_id: int, side: str,
                        size: int, reduce_only: bool = False) -> dict:
    return await delta_client.place_order(api_key, api_secret, {
        "product_id": product_id,
        "size": max(1, int(size)),
        "side": side,
        "order_type": "market_order",
        "reduce_only": reduce_only,
    })


def _approx_pnl(side: str, entry: float, exit_: float, size: int) -> float:
    direction = 1.0 if side == "buy" else -1.0
    return round((exit_ - entry) * size * direction, 6)


# --- Entry (signal loop) -----------------------------------------------------

async def _try_place_entry(user_id: str, api_key: str, api_secret: str,
                            product: dict, symbol: str, side: str,
                            entry_price_hint: float, params: dict, strategy_key: str,
                            candles: list[dict], max_notional_usd: float) -> None:
    risk = await _get_risk(user_id)
    reason = _entry_blocked_reason(risk)
    if reason:
        await log_activity(user_id, "warn", "engine_blocked_by_risk",
                           f"[{strategy_key}] entry blocked — {reason}",
                           {"side": side, "symbol": symbol})
        return

    atr_period = int(params.get("atr_period", 14))
    sl_mult = float(params.get("sl_atr_mult", 1.0))
    tp_mult = float(params.get("tp_atr_mult", 2.1))
    atr_val = atr(candles, period=atr_period)
    if atr_val is None or atr_val <= 0:
        await log_activity(user_id, "warn", "engine_atr_unavailable",
                           f"[{strategy_key}] ATR could not be computed; skipping",
                           {"period": atr_period})
        return

    requested_size = max(1, int(params.get("size") or 1))

    # ---- Notional cap ----------------------------------------------------
    try:
        contract_value = float(product.get("contract_value") or 0)
    except Exception:
        contract_value = 0.0
    ref_price = float(candles[-1]["close"] or entry_price_hint or 0)
    notional_per_lot = contract_value * ref_price if contract_value and ref_price else 0
    if notional_per_lot <= 0:
        await log_activity(user_id, "error", "engine_notional_unknown",
                           f"[{strategy_key}] Cannot compute notional for {symbol} — skipping",
                           {"contract_value": product.get("contract_value"), "ref_price": ref_price})
        return
    cap_by_notional = int(max_notional_usd // notional_per_lot)  # floor
    if cap_by_notional < 1:
        await log_activity(
            user_id, "warn", "engine_blocked_by_notional",
            f"[{strategy_key}] Skip — 1 lot notional ({notional_per_lot:.2f} USD) exceeds cap ({max_notional_usd:.0f} USD)",
            {"symbol": symbol, "ref_price": ref_price},
        )
        return
    size = min(requested_size, cap_by_notional)
    effective_notional = round(size * notional_per_lot, 2)
    if size < requested_size:
        await log_activity(
            user_id, "info", "engine_size_capped",
            f"[{strategy_key}] Size capped {requested_size}→{size} lots by notional rule ({effective_notional} / {max_notional_usd:.0f} USD)",
            {"symbol": symbol, "notional_per_lot": round(notional_per_lot, 4)},
        )

    try:
        result = await _place_market(api_key, api_secret, product["id"], side, size, reduce_only=False)
    except delta_client.DeltaError as e:
        await log_activity(user_id, "error", "engine_order_failed",
                           f"[{strategy_key}] {side.upper()} {symbol} x{size} failed",
                           {"detail": e.detail})
        return

    r = result.get("result", {}) if isinstance(result, dict) else {}
    # Determine effective entry price. Prefer avg fill; fall back to hint.
    try:
        entry_price = float(r.get("average_fill_price") or entry_price_hint or candles[-1]["close"])
    except Exception:
        entry_price = float(candles[-1]["close"])

    if side == "buy":
        sl_price = entry_price - sl_mult * atr_val
        tp_price = entry_price + tp_mult * atr_val
    else:
        sl_price = entry_price + sl_mult * atr_val
        tp_price = entry_price - tp_mult * atr_val

    now_iso = datetime.now(timezone.utc).isoformat()
    trade_doc = {
        "user_id": user_id,
        "mode": "real",
        "exchange": "delta_india",
        "exchange_order_id": r.get("id"),
        "client_order_id": r.get("client_order_id"),
        "symbol": symbol,
        "product_id": product["id"],
        "side": side,
        "order_type": "market_order",
        "quantity": size,
        "notional_usd": effective_notional,
        "entry_price": entry_price,
        "exit_price": None,
        "realized_pnl": None,
        "status": r.get("state") or "submitted",
        "created_at": now_iso,
        "strategy": strategy_key,
        "sl_price": round(sl_price, 6),
        "tp_price": round(tp_price, 6),
        "atr": round(atr_val, 6),
        "raw": r,
    }
    ins = await trades.insert_one(trade_doc)

    await managed_positions.insert_one({
        "user_id": user_id,
        "trade_id": str(ins.inserted_id),
        "strategy_key": strategy_key,
        "symbol": symbol,
        "product_id": product["id"],
        "side": side,
        "size": size,
        "entry_price": entry_price,
        "sl_price": round(sl_price, 6),
        "tp_price": round(tp_price, 6),
        "atr": round(atr_val, 6),
        "opened_at": now_iso,
        "closed": False,
    })

    await _increment_entry(user_id)
    await log_activity(
        user_id, "info", "engine_order_placed",
        f"[{strategy_key}] {side.upper()} {symbol} x{size} (~{effective_notional} USD) @ {entry_price:.4f} · "
        f"ATR {atr_val:.4f} · SL {sl_price:.4f} · TP {tp_price:.4f}",
        {"order_id": r.get("id"), "state": r.get("state")},
    )


async def _signal_iteration(user_id: str) -> None:
    broker = await broker_connections.find_one({"user_id": user_id}, {"_id": 0})
    if not broker or not broker.get("api_key"):
        await log_activity(user_id, "warn", "engine_no_broker",
                           "Engine cannot trade: broker not configured")
        return
    api_key = broker["api_key"]
    api_secret = decrypt_secret(broker.get("api_secret_enc", ""))

    settings_doc = await user_settings.find_one({"user_id": user_id}, {"_id": 0}) or {}
    symbols = settings_doc.get("symbols") or []
    if not symbols:
        # legacy fallback
        legacy = settings_doc.get("default_symbol") or "BTCUSD"
        symbols = [legacy]
    symbols = [str(s).strip().upper() for s in symbols if s and str(s).strip()]
    timeframe = settings_doc.get("timeframe") or "5m"
    max_notional_usd = float(settings_doc.get("max_notional_usd") or 100.0)

    enabled = await strategies_col.find(
        {"user_id": user_id, "enabled": True}, {"_id": 0}
    ).to_list(length=50)
    if not enabled:
        return

    state_user = _bar_state.setdefault(user_id, {})

    for symbol in symbols:
        product = await delta_client.find_product_by_symbol(symbol)
        if not product:
            await log_activity(user_id, "warn", "engine_symbol_not_found",
                               f"Symbol {symbol} not found on Delta India — skipping")
            continue
        try:
            candles = await delta_client.get_candles(symbol, timeframe, count=200)
        except delta_client.DeltaError as e:
            await log_activity(user_id, "error", "engine_candles_failed",
                               f"Fetching candles for {symbol} {timeframe} failed",
                               {"detail": e.detail})
            continue
        if not candles:
            continue

        last_bar_time = candles[-1]["time"]
        last_close = candles[-1]["close"]
        sym_state = state_user.setdefault(symbol, {})

        for strat in enabled:
            key = strat["key"]
            params = strat.get("params") or {}
            signal = compute_signal(key, candles, params)
            strat_state = sym_state.setdefault(key, {"last_bar_time": 0, "last_side": None})

            if signal is None:
                continue
            if last_bar_time <= strat_state["last_bar_time"]:
                continue
            if strat_state.get("last_side") == signal:
                strat_state["last_bar_time"] = last_bar_time
                continue

            await log_activity(user_id, "info", "engine_signal",
                               f"[{key}] {signal.upper()} on {symbol} {timeframe} @ {last_close:.4f}",
                               {"bar_time": last_bar_time})
            await _try_place_entry(user_id, api_key, api_secret, product, symbol,
                                    signal, last_close, params, key, candles, max_notional_usd)

            strat_state["last_bar_time"] = last_bar_time
            strat_state["last_side"] = signal


# --- Exit (fast loop) --------------------------------------------------------

def _sl_hit(side: str, price: float, sl: float) -> bool:
    return price <= sl if side == "buy" else price >= sl


def _tp_hit(side: str, price: float, tp: float) -> bool:
    return price >= tp if side == "buy" else price <= tp


async def _close_position(user_id: str, pos: dict, exit_price: float, reason: str,
                          api_key: str, api_secret: str) -> None:
    close_side = "sell" if pos["side"] == "buy" else "buy"
    try:
        result = await _place_market(api_key, api_secret, pos["product_id"], close_side,
                                     pos["size"], reduce_only=True)
    except delta_client.DeltaError as e:
        await log_activity(user_id, "error", "engine_exit_failed",
                           f"[{pos['strategy_key']}] Close {pos['symbol']} failed ({reason})",
                           {"detail": e.detail})
        return

    r = result.get("result", {}) if isinstance(result, dict) else {}
    fill_price = None
    try:
        fill_price = float(r.get("average_fill_price") or 0) or None
    except Exception:
        pass
    exit_effective = fill_price or exit_price
    pnl = _approx_pnl(pos["side"], pos["entry_price"], exit_effective, pos["size"])
    now_iso = datetime.now(timezone.utc).isoformat()

    from bson import ObjectId
    try:
        _id = ObjectId(pos["trade_id"])
    except Exception:
        _id = None
    if _id is not None:
        await trades.update_one(
            {"_id": _id},
            {"$set": {"exit_price": exit_effective, "realized_pnl": pnl,
                       "status": "closed", "exit_reason": reason, "closed_at": now_iso}},
        )
    await managed_positions.update_one(
        {"user_id": user_id, "trade_id": pos["trade_id"]},
        {"$set": {"closed": True, "closed_at": now_iso, "exit_price": exit_effective,
                   "exit_reason": reason, "realized_pnl": pnl}},
    )

    await log_activity(
        user_id, "warn" if reason == "sl" else "info",
        "engine_exit_sl" if reason == "sl" else "engine_exit_tp",
        f"[{pos['strategy_key']}] {reason.upper()} hit on {pos['symbol']} @ {exit_effective:.4f} · "
        f"PnL {pnl:+.4f}",
        {"entry": pos["entry_price"], "sl": pos.get("sl_price"), "tp": pos.get("tp_price")},
    )

    if reason == "sl":
        await _record_sl(user_id)


async def _exit_iteration(user_id: str) -> None:
    open_positions = await managed_positions.find(
        {"user_id": user_id, "closed": False}, {"_id": 0}
    ).to_list(length=50)
    if not open_positions:
        return

    broker = await broker_connections.find_one({"user_id": user_id}, {"_id": 0})
    if not broker or not broker.get("api_key"):
        return
    api_key = broker["api_key"]
    api_secret = decrypt_secret(broker.get("api_secret_enc", ""))

    # Group by symbol to minimize ticker calls.
    by_symbol: dict[str, list[dict]] = {}
    for p in open_positions:
        by_symbol.setdefault(p["symbol"], []).append(p)

    for symbol, positions in by_symbol.items():
        price = await delta_client.get_last_price(symbol)
        if price is None:
            continue
        for pos in positions:
            side = pos["side"]
            sl = float(pos["sl_price"])
            tp = float(pos["tp_price"])
            if _sl_hit(side, price, sl):
                await _close_position(user_id, pos, price, "sl", api_key, api_secret)
            elif _tp_hit(side, price, tp):
                await _close_position(user_id, pos, price, "tp", api_key, api_secret)


# --- Loop drivers ------------------------------------------------------------

async def _signal_loop(user_id: str) -> None:
    await log_activity(user_id, "info", "engine_loop_started",
                       f"Engine online · signal poll {SIGNAL_POLL_SEC}s · exit poll {EXIT_POLL_SEC}s")
    try:
        while True:
            try:
                await _signal_iteration(user_id)
            except asyncio.CancelledError:
                raise
            except Exception as e:  # noqa: BLE001
                logger.exception("signal iteration failed")
                await log_activity(user_id, "error", "engine_iteration_error", str(e)[:250])
            await asyncio.sleep(SIGNAL_POLL_SEC)
    except asyncio.CancelledError:
        await log_activity(user_id, "info", "engine_loop_stopped", "Engine loop stopped")
        raise


async def _exit_loop(user_id: str) -> None:
    try:
        while True:
            try:
                await _exit_iteration(user_id)
            except asyncio.CancelledError:
                raise
            except Exception as e:  # noqa: BLE001
                logger.exception("exit iteration failed")
            await asyncio.sleep(EXIT_POLL_SEC)
    except asyncio.CancelledError:
        raise


def start(user_id: str) -> None:
    if is_running(user_id):
        return
    _bar_state[user_id] = {}
    _tasks[user_id] = {
        "signal": asyncio.create_task(_signal_loop(user_id), name=f"engine-signal-{user_id}"),
        "exit": asyncio.create_task(_exit_loop(user_id), name=f"engine-exit-{user_id}"),
    }


async def stop(user_id: str) -> None:
    slot = _tasks.pop(user_id, None)
    _bar_state.pop(user_id, None)
    if not slot:
        return
    for t in slot.values():
        if t and not t.done():
            t.cancel()
    for t in slot.values():
        try:
            await t
        except BaseException:
            pass


async def resume_all() -> None:
    async for doc in engine_states.find({"running": True}, {"_id": 0}):
        uid = doc.get("user_id")
        if uid and not is_running(uid):
            start(uid)


async def shutdown_all() -> None:
    for uid in list(_tasks.keys()):
        await stop(uid)
