"""NexusTrade backend regression tests.

Tests cover: health, auth gating, dashboard, settings (user + broker),
strategies auto-seed + toggle, engine start/stop, logs, orders gating,
products list, and history endpoints.
"""
import os
import time
import uuid
from datetime import datetime, timedelta, timezone

import pytest
import requests
from pymongo import MongoClient

BASE_URL = os.environ["REACT_APP_BACKEND_URL"].rstrip("/") if os.environ.get("REACT_APP_BACKEND_URL") else None
if not BASE_URL:
    # fallback: read from frontend .env
    with open("/app/frontend/.env") as f:
        for line in f:
            if line.startswith("REACT_APP_BACKEND_URL="):
                BASE_URL = line.split("=", 1)[1].strip().rstrip("/")

MONGO_URL = os.environ.get("MONGO_URL", "mongodb://localhost:27017")
DB_NAME = os.environ.get("DB_NAME", "test_database")

DELTA_API_KEY = "Di6L3DlOPHpcBLPnX1Y51XgcNGW0YN"
DELTA_API_SECRET = "zvbuBEZvwyubMDUTBJ1my9hc0F73iSMXQO9e519WzWd193w6Z487bkkEuFA0"


@pytest.fixture(scope="session")
def mongo():
    return MongoClient(MONGO_URL)[DB_NAME]


@pytest.fixture(scope="session")
def session_bootstrap(mongo):
    user_id = f"TEST_user_{uuid.uuid4().hex[:8]}"
    token = f"TEST_session_{uuid.uuid4().hex}"
    now = datetime.now(timezone.utc)
    mongo.users.insert_one({
        "user_id": user_id,
        "email": f"TEST_{user_id}@example.com",
        "name": "Regression Tester",
        "picture": "",
        "created_at": now.isoformat(),
        "updated_at": now.isoformat(),
    })
    mongo.user_sessions.insert_one({
        "user_id": user_id,
        "session_token": token,
        "expires_at": (now + timedelta(days=7)).isoformat(),
        "created_at": now.isoformat(),
    })
    yield {"user_id": user_id, "token": token}
    # cleanup
    for coll in ["users", "user_sessions", "broker_connections", "user_settings",
                 "trades", "strategies", "engine_states", "activity_logs"]:
        mongo[coll].delete_many({"user_id": user_id})


@pytest.fixture(scope="session")
def auth_headers(session_bootstrap):
    return {"Authorization": f"Bearer {session_bootstrap['token']}"}


# -------- health / auth gating --------

def test_health():
    r = requests.get(f"{BASE_URL}/api/health", timeout=15)
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_me_unauthenticated():
    r = requests.get(f"{BASE_URL}/api/auth/me", timeout=15)
    assert r.status_code == 401


def test_me_authenticated(auth_headers, session_bootstrap):
    r = requests.get(f"{BASE_URL}/api/auth/me", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    data = r.json()
    assert data["user_id"] == session_bootstrap["user_id"]
    assert "_id" not in data


PROTECTED_GETS = [
    "/api/dashboard", "/api/settings", "/api/settings/broker",
    "/api/products", "/api/orders/open", "/api/positions", "/api/wallet",
    "/api/history", "/api/history/exchange", "/api/strategies",
    "/api/engine", "/api/logs",
]

@pytest.mark.parametrize("path", PROTECTED_GETS)
def test_protected_requires_auth(path):
    r = requests.get(f"{BASE_URL}{path}", timeout=15)
    assert r.status_code == 401, f"{path} returned {r.status_code}"


# -------- dashboard --------

def test_dashboard_shape(auth_headers):
    r = requests.get(f"{BASE_URL}/api/dashboard", headers=auth_headers, timeout=20)
    assert r.status_code == 200
    d = r.json()
    for k in ["virtual_account_balance", "virtual_cash_available", "realized_pnl",
              "unrealized_pnl", "total_trades", "open_positions", "broker", "engine"]:
        assert k in d, f"missing {k}"
    assert isinstance(d["open_positions"], list)
    assert "configured" in d["broker"]
    assert "running" in d["engine"] and "mode" in d["engine"]


# -------- settings --------

def test_settings_defaults(auth_headers):
    r = requests.get(f"{BASE_URL}/api/settings", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    s = r.json()
    assert s["virtual_capital"] == 100000.0
    assert s["default_symbol"] == "BTCUSD"
    assert s["timeframe"] == "5m"


def test_settings_update_persist(auth_headers):
    r = requests.put(f"{BASE_URL}/api/settings", headers=auth_headers,
                     json={"virtual_capital": 250000, "default_symbol": "ETHUSD", "timeframe": "15m"},
                     timeout=15)
    assert r.status_code == 200
    s = r.json()
    assert s["virtual_capital"] == 250000
    assert s["default_symbol"] == "ETHUSD"
    assert s["timeframe"] == "15m"
    # verify GET
    r2 = requests.get(f"{BASE_URL}/api/settings", headers=auth_headers, timeout=15)
    assert r2.json()["default_symbol"] == "ETHUSD"


# -------- broker flow --------

def test_broker_initial_not_configured(auth_headers):
    r = requests.get(f"{BASE_URL}/api/settings/broker", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    assert r.json()["configured"] is False


def test_broker_save(auth_headers):
    r = requests.put(f"{BASE_URL}/api/settings/broker", headers=auth_headers,
                     json={"api_key": DELTA_API_KEY, "api_secret": DELTA_API_SECRET}, timeout=15)
    assert r.status_code == 200
    data = r.json()
    assert data["configured"] is True
    assert data["api_key_masked"] and DELTA_API_KEY[:4] in data["api_key_masked"]


def test_broker_test_endpoint_no_500(auth_headers):
    """Delta may 4xx (IP restriction) but server must not 5xx."""
    r = requests.post(f"{BASE_URL}/api/settings/broker/test", headers=auth_headers, timeout=25)
    assert r.status_code < 500, f"server errored: {r.status_code} {r.text[:400]}"


def test_orders_requires_broker_message(auth_headers, mongo, session_bootstrap):
    # Temporarily remove broker
    mongo.broker_connections.delete_many({"user_id": session_bootstrap["user_id"]})
    r = requests.post(f"{BASE_URL}/api/orders", headers=auth_headers,
                      json={"product_id": 27, "size": 1, "side": "buy"}, timeout=15)
    assert r.status_code == 400
    assert "Delta" in r.text or "credentials" in r.text.lower()
    # restore
    requests.put(f"{BASE_URL}/api/settings/broker", headers=auth_headers,
                 json={"api_key": DELTA_API_KEY, "api_secret": DELTA_API_SECRET}, timeout=15)


def test_broker_delete(auth_headers):
    r = requests.delete(f"{BASE_URL}/api/settings/broker", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    r2 = requests.get(f"{BASE_URL}/api/settings/broker", headers=auth_headers, timeout=15)
    assert r2.json()["configured"] is False
    # re-save for downstream tests
    requests.put(f"{BASE_URL}/api/settings/broker", headers=auth_headers,
                 json={"api_key": DELTA_API_KEY, "api_secret": DELTA_API_SECRET}, timeout=15)


# -------- strategies --------

def test_strategies_seed_and_toggle(auth_headers):
    r = requests.get(f"{BASE_URL}/api/strategies", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    keys = {s["key"] for s in r.json()["strategies"]}
    assert {"ema_cross", "rsi_reversion", "breakout"}.issubset(keys)

    t = requests.post(f"{BASE_URL}/api/strategies/ema_cross/toggle", headers=auth_headers,
                      json={"enabled": True}, timeout=15)
    assert t.status_code == 200
    assert t.json()["enabled"] is True

    r2 = requests.get(f"{BASE_URL}/api/strategies", headers=auth_headers, timeout=15)
    ema = next(s for s in r2.json()["strategies"] if s["key"] == "ema_cross")
    assert ema["enabled"] is True


# -------- engine --------

def test_engine_start_stop(auth_headers):
    r = requests.post(f"{BASE_URL}/api/engine/start", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    d = r.json()
    assert d["running"] is True and d["mode"] == "real"

    g = requests.get(f"{BASE_URL}/api/engine", headers=auth_headers, timeout=15)
    assert g.json()["running"] is True

    r2 = requests.post(f"{BASE_URL}/api/engine/stop", headers=auth_headers, timeout=15)
    assert r2.status_code == 200
    assert r2.json()["running"] is False and r2.json()["mode"] == "paused"


# -------- logs --------

def test_logs_populated(auth_headers):
    r = requests.get(f"{BASE_URL}/api/logs", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    logs = r.json()["logs"]
    assert isinstance(logs, list) and len(logs) > 0
    events = {l["event"] for l in logs}
    # We toggled a strategy + started/stopped engine + saved broker earlier
    assert events & {"engine_start", "engine_stop", "strategy_toggled", "broker_connected"}


# -------- products --------

def test_products_reachable(auth_headers):
    r = requests.get(f"{BASE_URL}/api/products", headers=auth_headers, timeout=25)
    # If Delta reachable, expect 200 with products array; else 4xx surfaced (not 500)
    assert r.status_code < 500
    if r.status_code == 200:
        products = r.json().get("products", [])
        assert isinstance(products, list)
        # At least try to have some products
        assert len(products) >= 1


# -------- history --------

def test_history_empty_initially(auth_headers):
    r = requests.get(f"{BASE_URL}/api/history", headers=auth_headers, timeout=15)
    assert r.status_code == 200
    assert isinstance(r.json()["trades"], list)


def test_history_exchange_no_500(auth_headers):
    r = requests.get(f"{BASE_URL}/api/history/exchange", headers=auth_headers, timeout=25)
    assert r.status_code < 500


# -------- logout --------

def test_logout_invalidates(mongo, session_bootstrap):
    # Use a fresh throwaway session so we don't invalidate the main test session
    tmp_token = f"TEST_logout_{uuid.uuid4().hex}"
    mongo.user_sessions.insert_one({
        "user_id": session_bootstrap["user_id"],
        "session_token": tmp_token,
        "expires_at": (datetime.now(timezone.utc) + timedelta(days=1)).isoformat(),
        "created_at": datetime.now(timezone.utc).isoformat(),
    })
    h = {"Authorization": f"Bearer {tmp_token}"}
    ok = requests.get(f"{BASE_URL}/api/auth/me", headers=h, timeout=15)
    assert ok.status_code == 200
    # logout uses cookie only per implementation; simulate via cookie
    lo = requests.post(f"{BASE_URL}/api/auth/logout",
                       cookies={"session_token": tmp_token}, timeout=15)
    assert lo.status_code == 200
    # session removed
    after = requests.get(f"{BASE_URL}/api/auth/me", headers=h, timeout=15)
    assert after.status_code == 401
