package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Long,
    val symbol: String,
    val contract_type: String, // perpetual_futures, call_options, put_options
    val underlying_asset: String = "BTC",
    val tick_size: Double = 0.5,
    val description: String = "",
    val mark_price: Double = 0.0,
    val volume_24h: Double = 0.0
)

@Serializable
data class OrderRequest(
    val product_id: Long,
    val symbol: String,
    val size: Int,
    val side: String, // "buy" or "sell"
    val order_type: String = "market_order", // "market_order" or "limit_order"
    val limit_price: String? = null,
    val reduce_only: Boolean = false,
    val time_in_force: String = "gtc"
)

@Serializable
data class OrderResponse(
    val id: String = "",
    val product_id: Long = 0,
    val symbol: String = "",
    val size: Int = 0,
    val side: String = "buy",
    val order_type: String = "market_order",
    val limit_price: String? = null,
    val average_fill_price: Double = 0.0,
    val status: String = "filled",
    val created_at: String = "",
    val raw_json: String = ""
)

@Serializable
data class Position(
    val product_id: Long,
    val symbol: String,
    val size: Double,
    val entry_price: Double,
    val mark_price: Double,
    val unrealized_pnl: Double,
    val liquidation_price: Double
)

@Serializable
data class TradeRecord(
    val id: String,
    val exchange_order_id: String? = null,
    val client_order_id: String? = null,
    val symbol: String,
    val product_id: Long = 0,
    val side: String,
    val quantity: Double,
    val entry_price: Double,
    val exit_price: Double? = null,
    val realized_pnl: Double? = null,
    val status: String = "filled",
    val created_at: String
)

@Serializable
data class Strategy(
    val key: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val params: Map<String, Double> = emptyMap()
)

@Serializable
data class EngineState(
    val running: Boolean = false,
    val mode: String = "paused", // "real", "paused"
    val updated_at: String? = null,
    val last_error: String? = null
)

@Serializable
data class UserSettings(
    val virtual_capital: Double = 100000.0,
    val default_symbol: String = "BTCUSD",
    val symbols: List<String> = listOf("BTCUSD", "ETHUSD", "SOLUSD"),
    val timeframe: String = "5m",
    val max_notional_usd: Double = 100.0,
    val strategy_params: Map<String, Double> = emptyMap()
)

@Serializable
data class BrokerConnection(
    val configured: Boolean = false,
    val api_key: String = "",
    val api_secret: String = "",
    val api_key_masked: String? = null,
    val updated_at: String? = null,
    val wallet_balance: Double? = null,
    val wallet_available: Double? = null,
    val wallet_currency: String = "USDT",
    val status: String = "idle", // "connected", "validating", "failed", "unconfigured"
    val ip_whitelisted: Boolean = true,
    val outbound_ip: String? = null,
    val last_validated_at: String? = null,
    val latency_ms: Long? = null,
    val error: String? = null
)

@Serializable
data class GeminiSettings(
    val configured: Boolean = false,
    val api_key: String = "",
    val api_key_masked: String? = null,
    val model_name: String = "gemini-2.5-flash",
    val enable_signal_verification: Boolean = true,
    val enable_sentiment_analysis: Boolean = true,
    val enable_risk_advisory: Boolean = true,
    val status: String = "idle", // "connected", "validating", "failed", "unconfigured"
    val last_tested_at: String? = null,
    val latency_ms: Long? = null,
    val error: String? = null
)

@Serializable
data class GeminiAnalysisResult(
    val timestamp: String,
    val symbol: String,
    val price: Double,
    val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val confidence: Int, // 0-100
    val summary: String,
    val technicalRationale: String,
    val riskWarning: String,
    val suggestedAction: String // "STRONG_BUY", "BUY", "NEUTRAL", "SELL", "STRONG_SELL", "WAIT"
)

@Serializable
data class RiskStatus(
    val date: String,
    val entries: Int,
    val entries_max: Int = 10,
    val sl_hits: Int = 0,
    val sl_max: Int = 3,
    val cooldown_min: Int = 15,
    val last_sl_at: String? = null,
    val blocked_reason: String? = null
)

@Serializable
data class NotionalSymbol(
    val symbol: String,
    val ref_price: Double? = null,
    val notional_per_lot: Double? = null,
    val max_lots: Int? = null,
    val error: String? = null
)

@Serializable
data class NotionalPreview(
    val max_notional_usd: Double = 100.0,
    val symbols: List<NotionalSymbol> = emptyList()
)

@Serializable
data class ActivityLogItem(
    val id: String,
    val created_at: String,
    val level: String, // "info", "warn", "error", "debug"
    val event: String,
    val message: String
)

@Serializable
data class DashboardData(
    val virtual_account_balance: Double = 100000.0,
    val unrealized_pnl: Double = 0.0,
    val realized_pnl: Double = 0.0,
    val open_positions_count: Int = 0,
    val total_trades: Int = 0,
    val broker: BrokerConnection = BrokerConnection(),
    val engine: EngineState = EngineState(),
    val risk: RiskStatus? = null,
    val notional_preview: NotionalPreview? = null,
    val open_positions: List<Position> = emptyList()
)
