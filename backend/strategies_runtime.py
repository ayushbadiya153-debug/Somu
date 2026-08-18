"""Pure signal generators for the three built-in strategies.

Each function receives a list of closed OHLC candles (ascending by time)
and the strategy params. Returns one of: "buy", "sell", None.

Signals are emitted only on the LAST closed bar (last candle in the list).
"""
from typing import Optional


def _closes(candles: list[dict]) -> list[float]:
    return [c["close"] for c in candles]


def _ema(values: list[float], period: int) -> list[float]:
    if not values or period <= 0:
        return []
    k = 2.0 / (period + 1)
    out = []
    ema = values[0]
    for v in values:
        ema = v * k + ema * (1 - k)
        out.append(ema)
    return out


def _rsi(values: list[float], period: int) -> list[float]:
    if len(values) < period + 1:
        return []
    gains, losses = [], []
    for i in range(1, len(values)):
        change = values[i] - values[i - 1]
        gains.append(max(change, 0.0))
        losses.append(max(-change, 0.0))
    # Wilder smoothing
    avg_g = sum(gains[:period]) / period
    avg_l = sum(losses[:period]) / period
    rsis: list[float] = [50.0] * period  # pad initial
    for i in range(period, len(gains)):
        avg_g = (avg_g * (period - 1) + gains[i]) / period
        avg_l = (avg_l * (period - 1) + losses[i]) / period
        rs = (avg_g / avg_l) if avg_l else 999.0
        rsi = 100 - (100 / (1 + rs))
        rsis.append(rsi)
    # prepend one to align length with values
    return [50.0] + rsis


def ema_cross_signal(candles: list[dict], params: dict) -> Optional[str]:
    fast = int(params.get("fast", 9))
    slow = int(params.get("slow", 21))
    closes = _closes(candles)
    if len(closes) < slow + 2:
        return None
    ef = _ema(closes, fast)
    es = _ema(closes, slow)
    # Look at last two closed bars
    prev_diff = ef[-2] - es[-2]
    curr_diff = ef[-1] - es[-1]
    if prev_diff <= 0 and curr_diff > 0:
        return "buy"
    if prev_diff >= 0 and curr_diff < 0:
        return "sell"
    return None


def rsi_reversion_signal(candles: list[dict], params: dict) -> Optional[str]:
    period = int(params.get("period", 14))
    oversold = float(params.get("oversold", 30))
    overbought = float(params.get("overbought", 70))
    closes = _closes(candles)
    if len(closes) < period + 3:
        return None
    r = _rsi(closes, period)
    if len(r) < 3:
        return None
    prev, curr = r[-2], r[-1]
    if prev < oversold and curr >= oversold:
        return "buy"
    if prev > overbought and curr <= overbought:
        return "sell"
    return None


def breakout_signal(candles: list[dict], params: dict) -> Optional[str]:
    lookback = int(params.get("lookback", 20))
    if len(candles) < lookback + 2:
        return None
    window = candles[-(lookback + 1):-1]  # N bars BEFORE the current closed one
    hi = max(c["high"] for c in window)
    lo = min(c["low"] for c in window)
    last = candles[-1]
    if last["close"] > hi:
        return "buy"
    if last["close"] < lo:
        return "sell"
    return None


SIGNAL_FUNCS = {
    "ema_cross": ema_cross_signal,
    "rsi_reversion": rsi_reversion_signal,
    "breakout": breakout_signal,
}


def atr(candles: list[dict], period: int = 14) -> Optional[float]:
    """Wilder-smoothed Average True Range over the last `period` closed bars.
    Returns the ATR value at the latest closed bar, or None if not enough data."""
    if len(candles) < period + 1:
        return None
    trs: list[float] = []
    for i in range(1, len(candles)):
        h = candles[i]["high"]
        l = candles[i]["low"]
        pc = candles[i - 1]["close"]
        tr = max(h - l, abs(h - pc), abs(l - pc))
        trs.append(tr)
    if len(trs) < period:
        return None
    # Wilder smoothing
    a = sum(trs[:period]) / period
    for x in trs[period:]:
        a = (a * (period - 1) + x) / period
    return a


def compute_signal(strategy_key: str, candles: list[dict], params: dict) -> Optional[str]:
    fn = SIGNAL_FUNCS.get(strategy_key)
    if not fn:
        return None
    return fn(candles, params)
