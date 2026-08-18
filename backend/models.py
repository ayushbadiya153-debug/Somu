"""Shared Pydantic models."""
from typing import Optional, Literal
from pydantic import BaseModel, Field, field_validator


class BrokerConnectionIn(BaseModel):
    api_key: str = Field(min_length=8)
    api_secret: str = Field(min_length=8)


class BrokerConnectionOut(BaseModel):
    configured: bool
    api_key_masked: Optional[str] = None
    updated_at: Optional[str] = None


class SettingsIn(BaseModel):
    virtual_capital: Optional[float] = Field(default=None, ge=0)
    default_symbol: Optional[str] = None
    symbols: Optional[list[str]] = None
    timeframe: Optional[str] = None
    strategy_params: Optional[dict] = None
    max_notional_usd: Optional[float] = Field(default=None, ge=0)


class SettingsOut(BaseModel):
    virtual_capital: float = 100000.0
    default_symbol: str = "BTCUSD"
    symbols: list[str] = ["BTCUSD", "ETHUSD"]
    timeframe: str = "5m"
    strategy_params: dict = {}
    max_notional_usd: float = 100.0


class OrderIn(BaseModel):
    product_id: int = Field(gt=0)
    symbol: Optional[str] = None
    size: int = Field(gt=0)
    side: Literal["buy", "sell"]
    order_type: Literal["market_order", "limit_order"] = "market_order"
    limit_price: Optional[str] = None
    reduce_only: bool = False
    time_in_force: Optional[str] = "gtc"

    @field_validator("limit_price")
    @classmethod
    def _limit_needed(cls, v, info):
        return v


class StrategyToggle(BaseModel):
    enabled: bool


class StrategyOut(BaseModel):
    key: str
    name: str
    description: str
    enabled: bool
    params: dict = {}


class EngineState(BaseModel):
    running: bool
    mode: Literal["real", "paused"] = "paused"
    updated_at: Optional[str] = None
    last_error: Optional[str] = None
