"""NexusTrade FastAPI entrypoint."""
import logging
import os
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI
from starlette.middleware.cors import CORSMiddleware

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / ".env")

# Configure logging early
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("nexustrade")

from auth import router as auth_router
from routers.settings import router as settings_router
from routers.products import router as products_router
from routers.trading import router as trading_router
from routers.history import router as history_router
from routers.dashboard import router as dashboard_router
from routers.strategies import router as strategies_router
from routers.engine import router as engine_router
from routers.logs import router as logs_router
from db import close_client
import engine_runtime

app = FastAPI(title="NexusTrade — Delta India Trading Dashboard")

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=os.environ.get("CORS_ORIGINS", "*").split(","),
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/")
async def root():
    return {"service": "nexustrade", "status": "ok"}


@app.get("/api/health")
async def health():
    return {"status": "ok"}


app.include_router(auth_router)
app.include_router(settings_router)
app.include_router(products_router)
app.include_router(trading_router)
app.include_router(history_router)
app.include_router(dashboard_router)
app.include_router(strategies_router)
app.include_router(engine_router)
app.include_router(logs_router)


@app.on_event("startup")
async def _startup():
    await engine_runtime.resume_all()


@app.on_event("shutdown")
async def _shutdown():
    await engine_runtime.shutdown_all()
    close_client()
