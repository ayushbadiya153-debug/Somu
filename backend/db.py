"""MongoDB client and collection helpers."""
import os
from motor.motor_asyncio import AsyncIOMotorClient

_client = AsyncIOMotorClient(os.environ["MONGO_URL"])
db = _client[os.environ["DB_NAME"]]

# Collections
users = db.users
sessions = db.user_sessions
broker_connections = db.broker_connections
user_settings = db.user_settings
trades = db.trades
strategies = db.strategies
engine_states = db.engine_states
activity_logs = db.activity_logs
managed_positions = db.managed_positions
daily_risk = db.daily_risk


async def log_activity(user_id: str, level: str, event: str, message: str, meta: dict | None = None):
    from datetime import datetime, timezone
    await activity_logs.insert_one({
        "user_id": user_id,
        "level": level,
        "event": event,
        "message": message,
        "meta": meta or {},
        "created_at": datetime.now(timezone.utc).isoformat(),
    })


def close_client():
    _client.close()
