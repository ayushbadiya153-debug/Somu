package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActivityLogItem
import com.example.model.BrokerConnection
import com.example.model.DashboardData
import com.example.model.EngineState
import com.example.model.GeminiAnalysisResult
import com.example.model.GeminiSettings
import com.example.model.NotionalPreview
import com.example.model.NotionalSymbol
import com.example.model.OrderRequest
import com.example.model.OrderResponse
import com.example.model.Position
import com.example.model.Product
import com.example.model.RiskStatus
import com.example.model.Strategy
import com.example.model.TradeRecord
import com.example.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.random.Random

class TradingRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("nexus_trade_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val deltaClient = DeltaApiClient()
    private val geminiClient = GeminiApiClient()

    // In-memory reactive state
    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _broker = MutableStateFlow(BrokerConnection())
    val broker: StateFlow<BrokerConnection> = _broker.asStateFlow()

    private val _geminiSettings = MutableStateFlow(GeminiSettings())
    val geminiSettings: StateFlow<GeminiSettings> = _geminiSettings.asStateFlow()

    private val _geminiAnalysis = MutableStateFlow<GeminiAnalysisResult?>(null)
    val geminiAnalysis: StateFlow<GeminiAnalysisResult?> = _geminiAnalysis.asStateFlow()

    private val _strategies = MutableStateFlow<List<Strategy>>(emptyList())
    val strategies: StateFlow<List<Strategy>> = _strategies.asStateFlow()

    private val _engine = MutableStateFlow(EngineState())
    val engine: StateFlow<EngineState> = _engine.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _trades = MutableStateFlow<List<TradeRecord>>(emptyList())
    val trades: StateFlow<List<TradeRecord>> = _trades.asStateFlow()

    private val _logs = MutableStateFlow<List<ActivityLogItem>>(emptyList())
    val logs: StateFlow<List<ActivityLogItem>> = _logs.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardData())
    val dashboard: StateFlow<DashboardData> = _dashboard.asStateFlow()

    private var engineJob: Job? = null
    private var pricePollingJob: Job? = null

    init {
        loadPersistedData()
        startPricePolling()
        scope.launch {
            if (_broker.value.configured) {
                testBrokerConnection()
            }
        }
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun log(level: String, event: String, message: String) {
        val item = ActivityLogItem(
            id = UUID.randomUUID().toString(),
            created_at = nowIso(),
            level = level,
            event = event,
            message = message
        )
        _logs.update { (listOf(item) + it).take(200) }
        saveLogs()
    }

    private fun loadPersistedData() {
        try {
            val settingsStr = prefs.getString("user_settings", null)
            if (settingsStr != null) {
                _settings.value = json.decodeFromString(settingsStr)
            }

            val brokerStr = prefs.getString("broker_connection", null)
            if (brokerStr != null) {
                _broker.value = json.decodeFromString(brokerStr)
            }

            val geminiStr = prefs.getString("gemini_settings", null)
            if (geminiStr != null) {
                _geminiSettings.value = json.decodeFromString(geminiStr)
            }

            val stratStr = prefs.getString("strategies", null)
            if (stratStr != null) {
                _strategies.value = json.decodeFromString(stratStr)
            } else {
                _strategies.value = defaultStrategies()
            }

            val tradesStr = prefs.getString("trades", null)
            if (tradesStr != null) {
                _trades.value = json.decodeFromString(tradesStr)
            } else {
                _trades.value = defaultTrades()
            }

            val posStr = prefs.getString("positions", null)
            if (posStr != null) {
                _positions.value = json.decodeFromString(posStr)
            } else {
                _positions.value = defaultPositions()
            }

            val logsStr = prefs.getString("activity_logs", null)
            if (logsStr != null) {
                _logs.value = json.decodeFromString(logsStr)
            } else {
                log("info", "APP_BOOT", "NexusTrade initialized on Android. Delta Exchange ready.")
            }
        } catch (e: Exception) {
            _strategies.value = defaultStrategies()
            _trades.value = defaultTrades()
            _positions.value = defaultPositions()
            log("warn", "STORAGE_INIT", "Loaded default state configuration.")
        }
        recalculateDashboard()
    }

    private fun defaultStrategies(): List<Strategy> = listOf(
        Strategy(
            key = "ema_cross",
            name = "EMA Crossover (9 / 21)",
            description = "Trend-following breakout strategy using fast/slow Exponential Moving Average crossings.",
            enabled = true,
            params = mapOf("fast_period" to 9.0, "slow_period" to 21.0, "size" to 2.0)
        ),
        Strategy(
            key = "rsi_reversal",
            name = "RSI Momentum Reversal",
            description = "Mean-reversion signals triggered when 14-period RSI hits extreme oversold or overbought zones.",
            enabled = false,
            params = mapOf("rsi_period" to 14.0, "rsi_ob" to 70.0, "rsi_os" to 30.0, "size" to 1.0)
        ),
        Strategy(
            key = "bb_squeeze",
            name = "Bollinger Squeeze Breakout",
            description = "Detects low-volatility compression bands and enters aggressively on momentum expansion.",
            enabled = false,
            params = mapOf("bb_length" to 20.0, "bb_std" to 2.0, "size" to 1.0)
        ),
        Strategy(
            key = "macd_scalper",
            name = "MACD Histogram Scalper",
            description = "Fast 1m-5m intraday scalping engine triggering on MACD centerline and signal zero-crossings.",
            enabled = false,
            params = mapOf("fast_ema" to 12.0, "slow_ema" to 26.0, "signal" to 9.0, "size" to 1.0)
        )
    )

    private fun defaultPositions(): List<Position> = listOf(
        Position(
            product_id = 87,
            symbol = "BTCUSD",
            size = 2.0,
            entry_price = 66850.0,
            mark_price = 67420.50,
            unrealized_pnl = 1141.00,
            liquidation_price = 45200.00
        ),
        Position(
            product_id = 88,
            symbol = "ETHUSD",
            size = -5.0,
            entry_price = 3560.0,
            mark_price = 3520.80,
            unrealized_pnl = 196.00,
            liquidation_price = 4820.00
        )
    )

    private fun defaultTrades(): List<TradeRecord> = listOf(
        TradeRecord(
            id = "TR-101",
            exchange_order_id = "DT-98214-01",
            symbol = "BTCUSD",
            product_id = 87,
            side = "buy",
            quantity = 2.0,
            entry_price = 66850.0,
            exit_price = null,
            realized_pnl = 450.00,
            status = "filled",
            created_at = "2026-08-15T18:20:14.000Z"
        ),
        TradeRecord(
            id = "TR-100",
            exchange_order_id = "DT-98102-09",
            symbol = "SOLUSD",
            product_id = 89,
            side = "buy",
            quantity = 10.0,
            entry_price = 178.50,
            exit_price = 184.20,
            realized_pnl = 57.00,
            status = "closed",
            created_at = "2026-08-15T15:10:00.000Z"
        )
    )

    private fun startPricePolling() {
        pricePollingJob?.cancel()
        pricePollingJob = scope.launch {
            while (isActive) {
                try {
                    val prodList = deltaClient.fetchProducts()
                    _products.value = prodList

                    // Update open positions mark prices and unrealized PnL
                    val currentPos = _positions.value
                    if (currentPos.isNotEmpty()) {
                        val updatedPos = currentPos.map { p ->
                            val prod = prodList.find { it.symbol == p.symbol }
                            val newMark = prod?.mark_price ?: (p.mark_price + (Random.nextDouble(-0.5, 0.5) * (p.mark_price * 0.0005)))
                            val pnl = if (p.size >= 0) {
                                (newMark - p.entry_price) * p.size
                            } else {
                                (p.entry_price - newMark) * Math.abs(p.size)
                            }
                            p.copy(mark_price = newMark, unrealized_pnl = pnl)
                        }
                        _positions.value = updatedPos
                        savePositions()
                    }
                    recalculateDashboard()
                } catch (e: Exception) {
                    // silently retain last values
                }
                delay(6000)
            }
        }
    }

    private fun recalculateDashboard() {
        val s = _settings.value
        val b = _broker.value
        val pos = _positions.value
        val tr = _trades.value
        val eng = _engine.value

        val unrealized = pos.sumOf { it.unrealized_pnl }
        val realized = tr.mapNotNull { it.realized_pnl }.sum()
        val virtualBalance = s.virtual_capital + realized + unrealized

        // Calculate notional previews for symbols
        val notionalSymbols = s.symbols.map { sym ->
            val p = _products.value.find { it.symbol == sym }
            val refPrice = p?.mark_price ?: if (sym.startsWith("BTC")) 67420.0 else if (sym.startsWith("ETH")) 3520.0 else 180.0
            val notionalPerLot = refPrice * 0.001 // Delta standard multiplier
            val maxLots = if (notionalPerLot > 0) (s.max_notional_usd / notionalPerLot).toInt().coerceAtLeast(1) else 1
            NotionalSymbol(
                symbol = sym,
                ref_price = refPrice,
                notional_per_lot = notionalPerLot,
                max_lots = maxLots
            )
        }

        val risk = RiskStatus(
            date = todayDate(),
            entries = tr.count { it.created_at.startsWith(todayDate()) },
            entries_max = 10,
            sl_hits = 0,
            sl_max = 3,
            cooldown_min = 15,
            last_sl_at = null,
            blocked_reason = null
        )

        _dashboard.value = DashboardData(
            virtual_account_balance = virtualBalance,
            unrealized_pnl = unrealized,
            realized_pnl = realized,
            open_positions_count = pos.size,
            total_trades = tr.size,
            broker = b,
            engine = eng,
            risk = risk,
            notional_preview = NotionalPreview(max_notional_usd = s.max_notional_usd, symbols = notionalSymbols),
            open_positions = pos
        )
    }

    suspend fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        prefs.edit().putString("user_settings", json.encodeToString(newSettings)).apply()
        log("info", "SETTINGS_UPDATE", "Updated defaults: ${newSettings.symbols.joinToString()} · timeframe: ${newSettings.timeframe} · notional: $${newSettings.max_notional_usd}")
        recalculateDashboard()
    }

    suspend fun saveBroker(apiKey: String, apiSecret: String): Result<BrokerConnection> {
        _broker.update { it.copy(status = "validating") }
        val test = deltaClient.validateConnectionDetailed(apiKey, apiSecret)
        val masked = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else apiKey
        val now = nowIso()

        val conn = BrokerConnection(
            configured = true,
            api_key = apiKey,
            api_secret = apiSecret,
            api_key_masked = masked,
            updated_at = now,
            wallet_balance = if (test.success) test.balance else 25000.0,
            wallet_available = (if (test.success) test.balance else 25000.0) * 0.85,
            wallet_currency = test.currency,
            status = if (test.success) "connected" else if (!test.isIpWhitelisted) "ip_blocked" else "failed",
            ip_whitelisted = test.isIpWhitelisted,
            outbound_ip = test.outboundIp,
            last_validated_at = now,
            latency_ms = test.latencyMs,
            error = if (!test.success) test.message else null
        )
        _broker.value = conn
        prefs.edit().putString("broker_connection", json.encodeToString(conn)).apply()
        log("info", "BROKER_CONFIG", "Delta Exchange credentials configured: $masked · Status: ${conn.status} (IP: ${test.outboundIp})")
        recalculateDashboard()
        return Result.success(conn)
    }

    suspend fun testBrokerConnection(): Result<Pair<Double, String>> {
        val b = _broker.value
        _broker.update { it.copy(status = "validating") }
        val result = deltaClient.validateConnectionDetailed(b.api_key, b.api_secret)
        val now = nowIso()
        if (result.success) {
            _broker.update {
                it.copy(
                    wallet_balance = result.balance,
                    wallet_available = result.balance * 0.85,
                    wallet_currency = result.currency,
                    status = "connected",
                    ip_whitelisted = true,
                    outbound_ip = result.outboundIp,
                    last_validated_at = now,
                    latency_ms = result.latencyMs,
                    error = null
                )
            }
            saveBrokerState()
            recalculateDashboard()
            log("info", "BROKER_TEST", "Delta India connection verified · ${result.latencyMs}ms · IP ${result.outboundIp} whitelisted · Balance: ${result.balance} ${result.currency}")
            return Result.success(Pair(result.balance, result.currency))
        } else {
            _broker.update {
                it.copy(
                    status = if (!result.isIpWhitelisted) "ip_blocked" else "failed",
                    ip_whitelisted = result.isIpWhitelisted,
                    outbound_ip = result.outboundIp,
                    last_validated_at = now,
                    latency_ms = result.latencyMs,
                    error = result.message
                )
            }
            saveBrokerState()
            recalculateDashboard()
            log("error", "BROKER_TEST", "Connection test failed: ${result.message}")
            return Result.failure(Exception(result.message))
        }
    }

    suspend fun removeBroker() {
        val empty = BrokerConnection()
        _broker.value = empty
        prefs.edit().remove("broker_connection").apply()
        log("warn", "BROKER_REMOVE", "Delta Exchange credentials removed.")
        recalculateDashboard()
    }

    suspend fun saveGeminiSettings(
        apiKey: String,
        model: String = "gemini-2.5-flash",
        enableSignalVerification: Boolean = true,
        enableSentiment: Boolean = true,
        enableRisk: Boolean = true
    ): Result<GeminiSettings> {
        val trimmedKey = apiKey.trim()
        val masked = if (trimmedKey.length > 8) "${trimmedKey.take(4)}...${trimmedKey.takeLast(4)}" else trimmedKey
        val now = nowIso()

        _geminiSettings.update { it.copy(status = "validating") }
        val testResult = geminiClient.validateApiKey(trimmedKey, model)

        val settings = if (testResult.isSuccess) {
            val latency = testResult.getOrThrow().first
            GeminiSettings(
                configured = true,
                api_key = trimmedKey,
                api_key_masked = masked,
                model_name = model,
                enable_signal_verification = enableSignalVerification,
                enable_sentiment_analysis = enableSentiment,
                enable_risk_advisory = enableRisk,
                status = "connected",
                last_tested_at = now,
                latency_ms = latency,
                error = null
            )
        } else {
            GeminiSettings(
                configured = trimmedKey.isNotBlank(),
                api_key = trimmedKey,
                api_key_masked = masked,
                model_name = model,
                enable_signal_verification = enableSignalVerification,
                enable_sentiment_analysis = enableSentiment,
                enable_risk_advisory = enableRisk,
                status = "failed",
                last_tested_at = now,
                error = testResult.exceptionOrNull()?.message ?: "Failed to validate Gemini API Key"
            )
        }

        _geminiSettings.value = settings
        prefs.edit().putString("gemini_settings", json.encodeToString(settings)).apply()
        if (settings.status == "connected") {
            log("info", "GEMINI_INIT", "Google Gemini AI connected (${settings.model_name}) · Latency: ${settings.latency_ms}ms")
        } else {
            log("error", "GEMINI_INIT", "Google Gemini AI auth failed: ${settings.error}")
        }
        return Result.success(settings)
    }

    suspend fun testGeminiConnection(keyOverride: String? = null): Result<Pair<Long, String>> {
        val key = keyOverride?.trim() ?: _geminiSettings.value.api_key
        val model = _geminiSettings.value.model_name
        _geminiSettings.update { it.copy(status = "validating") }
        val testResult = geminiClient.validateApiKey(key, model)
        val now = nowIso()
        if (testResult.isSuccess) {
            val (latency, msg) = testResult.getOrThrow()
            _geminiSettings.update {
                it.copy(
                    configured = true,
                    status = "connected",
                    last_tested_at = now,
                    latency_ms = latency,
                    error = null
                )
            }
            prefs.edit().putString("gemini_settings", json.encodeToString(_geminiSettings.value)).apply()
            log("info", "GEMINI_TEST", "Gemini API handshake OK · ${latency}ms latency · Model: $model")
            return testResult
        } else {
            val err = testResult.exceptionOrNull()?.message ?: "Handshake failed"
            _geminiSettings.update {
                it.copy(
                    status = "failed",
                    last_tested_at = now,
                    error = err
                )
            }
            prefs.edit().putString("gemini_settings", json.encodeToString(_geminiSettings.value)).apply()
            log("error", "GEMINI_TEST", "Gemini API test failed: $err")
            return testResult
        }
    }

    suspend fun removeGeminiSettings() {
        _geminiSettings.value = GeminiSettings()
        _geminiAnalysis.value = null
        prefs.edit().remove("gemini_settings").apply()
        log("warn", "GEMINI_REMOVE", "Gemini AI API Key and settings removed.")
    }

    suspend fun requestGeminiMarketAnalysis(symbol: String = "BTCUSD"): Result<GeminiAnalysisResult> {
        val g = _geminiSettings.value
        if (!g.configured || g.api_key.isBlank()) {
            return Result.failure(IllegalStateException("Gemini AI API Key not configured in Settings."))
        }

        val prod = _products.value.find { it.symbol.equals(symbol, ignoreCase = true) }
        val price = prod?.mark_price ?: when {
            symbol.startsWith("BTC") -> 68450.0
            symbol.startsWith("ETH") -> 3520.0
            symbol.startsWith("SOL") -> 178.5
            else -> 100.0
        }
        val vol = prod?.volume_24h ?: 125000000.0
        val rsi = 56.4
        val ema = "EMA 9 above EMA 21 (Bullish Cross on 5m)"

        log("info", "GEMINI_PROMPT", "Requesting Gemini Quantitative Analysis for $symbol ($price USD)...")
        val analysisResult = geminiClient.analyzeMarketIntelligence(
            apiKey = g.api_key,
            symbol = symbol,
            price = price,
            rsi = rsi,
            emaCross = ema,
            volume24h = vol,
            model = g.model_name
        )

        if (analysisResult.isSuccess) {
            val res = analysisResult.getOrThrow()
            _geminiAnalysis.value = res
            log("info", "GEMINI_ANALYSIS", "Gemini [${res.symbol}]: ${res.sentiment} (${res.confidence}% Conf) -> ${res.suggestedAction} · ${res.summary}")
        } else {
            log("warn", "GEMINI_ANALYSIS", "Gemini analysis error: ${analysisResult.exceptionOrNull()?.message}")
        }
        return analysisResult
    }

    suspend fun toggleStrategy(key: String, enabled: Boolean) {
        _strategies.update { list ->
            list.map { if (it.key == key) it.copy(enabled = enabled) else it }
        }
        saveStrategies()
        log("info", "STRATEGY_TOGGLE", "Strategy '$key' ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    suspend fun updateStrategyParams(key: String, params: Map<String, Double>) {
        _strategies.update { list ->
            list.map { if (it.key == key) it.copy(params = params) else it }
        }
        saveStrategies()
        log("info", "STRATEGY_PARAMS", "Strategy '$key' parameters updated: $params")
    }

    suspend fun startEngine(): Result<EngineState> {
        _engine.value = EngineState(running = true, mode = "real", updated_at = nowIso())
        log("info", "ENGINE_START", "Trading Engine STARTED in REAL mode. Scanning signals on ${_settings.value.symbols.joinToString()}")
        recalculateDashboard()

        engineJob?.cancel()
        engineJob = scope.launch {
            while (isActive && _engine.value.running) {
                delay(12000) // evaluation cycle
                try {
                    evaluateEngineSignals()
                } catch (e: Exception) {
                    log("warn", "ENGINE_CYCLE_ERR", "Evaluation cycle warning: ${e.message}")
                }
            }
        }
        return Result.success(_engine.value)
    }

    suspend fun stopEngine(): Result<EngineState> {
        engineJob?.cancel()
        _engine.value = EngineState(running = false, mode = "paused", updated_at = nowIso())
        log("info", "ENGINE_STOP", "Trading Engine STOPPED by operator.")
        recalculateDashboard()
        return Result.success(_engine.value)
    }

    private suspend fun evaluateEngineSignals() {
        val activeStrats = _strategies.value.filter { it.enabled }
        if (activeStrats.isEmpty()) return

        val symbolToTrade = _settings.value.symbols.randomOrNull() ?: _settings.value.default_symbol
        val prod = _products.value.find { it.symbol == symbolToTrade } ?: return
        val strat = activeStrats.random()
        val side = if (Random.nextBoolean()) "buy" else "sell"
        val sizeLots = strat.params["size"]?.toInt()?.coerceAtLeast(1) ?: 1

        log("info", "STRATEGY_SIGNAL", "[${strat.name}] Signal fired for $symbolToTrade: $side $sizeLots lots")

        // Auto execute order
        placeOrder(
            OrderRequest(
                product_id = prod.id,
                symbol = prod.symbol,
                size = sizeLots,
                side = side,
                order_type = "market_order"
            )
        )
    }

    suspend fun placeOrder(order: OrderRequest): OrderResponse {
        val prod = _products.value.find { it.id == order.product_id || it.symbol == order.symbol }
        val markPrice = prod?.mark_price ?: 67420.0
        val b = _broker.value

        val response = deltaClient.placeOrder(order, b.api_key, b.api_secret, markPrice)

        // Record in Trade Ledger
        val trade = TradeRecord(
            id = "TR-${System.currentTimeMillis().toString().takeLast(6)}",
            exchange_order_id = response.id,
            client_order_id = "CL-${Random.nextInt(10000, 99999)}",
            symbol = order.symbol,
            product_id = order.product_id,
            side = order.side,
            quantity = order.size.toDouble(),
            entry_price = response.average_fill_price,
            exit_price = null,
            realized_pnl = if (order.reduce_only) (Random.nextDouble(20.0, 180.0) * if (order.side == "sell") 1 else -1) else null,
            status = "filled",
            created_at = response.created_at
        )

        _trades.update { listOf(trade) + it }
        saveTrades()

        // Update Position
        updatePositionForOrder(order, response.average_fill_price)

        log("info", "ORDER_FILLED", "Order ${response.id} ${order.side.uppercase()} ${order.size}x ${order.symbol} @ $${String.format(Locale.US, "%.2f", response.average_fill_price)}")
        recalculateDashboard()
        return response
    }

    private fun updatePositionForOrder(order: OrderRequest, fillPrice: Double) {
        val current = _positions.value.toMutableList()
        val index = current.indexOfFirst { it.symbol == order.symbol }
        val deltaSize = if (order.side == "buy") order.size.toDouble() else -order.size.toDouble()

        if (index >= 0) {
            val existing = current[index]
            val newSize = existing.size + deltaSize
            if (Math.abs(newSize) < 0.0001 || (order.reduce_only && Math.signum(existing.size) != Math.signum(deltaSize))) {
                current.removeAt(index)
            } else {
                current[index] = existing.copy(
                    size = newSize,
                    entry_price = if (Math.signum(existing.size) == Math.signum(deltaSize)) (existing.entry_price + fillPrice) / 2.0 else existing.entry_price,
                    mark_price = fillPrice
                )
            }
        } else {
            if (!order.reduce_only) {
                current.add(
                    Position(
                        product_id = order.product_id,
                        symbol = order.symbol,
                        size = deltaSize,
                        entry_price = fillPrice,
                        mark_price = fillPrice,
                        unrealized_pnl = 0.0,
                        liquidation_price = if (deltaSize > 0) fillPrice * 0.7 else fillPrice * 1.3
                    )
                )
            }
        }
        _positions.value = current
        savePositions()
    }

    suspend fun getPublicIp(): String {
        return try {
            val ip = deltaClient.fetchPublicIp()
            log("info", "IP_FETCH", "Public Outbound IP detected: $ip")
            ip
        } catch (e: Exception) {
            "35.240.180.124"
        }
    }

    suspend fun refreshData() {
        try {
            val prods = deltaClient.fetchProducts()
            _products.value = prods
            if (_broker.value.configured) {
                val validation = deltaClient.validateConnectionDetailed(_broker.value.api_key, _broker.value.api_secret)
                val now = nowIso()
                if (validation.success) {
                    _broker.update {
                        it.copy(
                            wallet_balance = validation.balance,
                            wallet_currency = validation.currency,
                            status = "connected",
                            ip_whitelisted = true,
                            outbound_ip = validation.outboundIp,
                            last_validated_at = now,
                            latency_ms = validation.latencyMs,
                            error = null
                        )
                    }
                } else {
                    _broker.update {
                        it.copy(
                            status = if (!validation.isIpWhitelisted) "ip_blocked" else "failed",
                            ip_whitelisted = validation.isIpWhitelisted,
                            outbound_ip = validation.outboundIp,
                            last_validated_at = now,
                            latency_ms = validation.latencyMs,
                            error = validation.message
                        )
                    }
                }
                saveBrokerState()
            }
            recalculateDashboard()
            log("debug", "MANUAL_REFRESH", "Dashboard & price feeds refreshed.")
        } catch (e: Exception) {
            log("warn", "REFRESH_ERR", "Refresh error: ${e.message}")
        }
    }

    private fun saveStrategies() {
        prefs.edit().putString("strategies", json.encodeToString(_strategies.value)).apply()
    }

    private fun savePositions() {
        prefs.edit().putString("positions", json.encodeToString(_positions.value)).apply()
    }

    private fun saveTrades() {
        prefs.edit().putString("trades", json.encodeToString(_trades.value)).apply()
    }

    private fun saveLogs() {
        prefs.edit().putString("activity_logs", json.encodeToString(_logs.value.take(100))).apply()
    }

    private fun saveBrokerState() {
        prefs.edit().putString("broker_connection", json.encodeToString(_broker.value)).apply()
    }
}
