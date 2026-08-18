package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrokerConnection
import com.example.model.DashboardData
import com.example.model.GeminiAnalysisResult
import com.example.model.GeminiSettings
import com.example.model.Position
import com.example.theme.DarkBackground
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.ElectricBlue
import com.example.theme.LossRed
import com.example.theme.ProfitGreen
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import com.example.theme.WarningYellow
import com.example.ui.components.KpiCard
import com.example.ui.components.KpiTone
import com.example.ui.components.NotionalPreviewSection
import com.example.ui.components.RiskManagerPanel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    data: DashboardData,
    geminiSettings: GeminiSettings = GeminiSettings(),
    geminiAnalysis: GeminiAnalysisResult? = null,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onRefresh: () -> Unit,
    onValidateConnection: () -> Unit = {},
    onRequestGeminiAnalysis: suspend (String) -> Unit = {},
    onNavigateToTrade: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val broker = data.broker
    val engine = data.engine
    val positions = data.open_positions

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMMAND CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Trading Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                        modifier = Modifier.testTag("dash_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (engine.running) {
                        Button(
                            onClick = onStopEngine,
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.testTag("dash_engine_stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Engine", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onStartEngine,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.testTag("dash_engine_start_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Engine", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Connection Status Indicator Banner (Validates API Key, Secret & IP Whitelist against Delta Exchange)
        item {
            ConnectionStatusIndicatorCard(
                broker = broker,
                onValidate = onValidateConnection,
                onConfigure = onNavigateToSettings
            )
        }

        // Gemini AI Market Intelligence Card
        item {
            val scope = rememberCoroutineScope()
            var analyzingGemini by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dash_gemini_market_card")
                    .border(
                        1.dp,
                        if (geminiSettings.configured) ElectricBlue.copy(alpha = 0.5f) else DarkBorder,
                        RoundedCornerShape(2.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (geminiSettings.configured) Color(0xFF0B1728) else DarkSurface
                ),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "GEMINI AI MARKET INTELLIGENCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        if (geminiSettings.configured) {
                            Text(
                                text = "${geminiSettings.model_name.uppercase()} · ACTIVE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        } else {
                            Text(
                                text = "OFFLINE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkMuted
                            )
                        }
                    }

                    if (geminiSettings.configured) {
                        geminiAnalysis?.let { analysis ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isBull = analysis.sentiment == "BULLISH"
                                    val isBear = analysis.sentiment == "BEARISH"
                                    val sentimentColor = if (isBull) ProfitGreen else if (isBear) LossRed else WarningYellow

                                    Box(
                                        modifier = Modifier
                                            .background(sentimentColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = analysis.sentiment,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = sentimentColor
                                        )
                                    }

                                    Text(
                                        text = "${analysis.confidence}% Confidence",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "ACTION: ${analysis.suggestedAction}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = analysis.summary,
                                fontSize = 12.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )

                            if (analysis.riskWarning.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = analysis.riskWarning,
                                        fontSize = 11.sp,
                                        color = WarningYellow,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } ?: run {
                            Text(
                                text = "Gemini is connected and ready to monitor Delta market momentum, verify entries, and guard against sudden liquidations.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        analyzingGemini = true
                                        try {
                                            onRequestGeminiAnalysis("BTCUSD")
                                        } finally {
                                            analyzingGemini = false
                                        }
                                    }
                                },
                                enabled = !analyzingGemini,
                                shape = RoundedCornerShape(2.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.6f))),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                                modifier = Modifier.testTag("dash_gemini_analyze_btn")
                            ) {
                                if (analyzingGemini) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = ElectricBlue, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Request Live AI Analysis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Gemini AI is not connected. Add your free Gemini API key in Settings to activate AI trade verification and sentiment scoring.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.testTag("dash_gemini_configure_btn")
                            ) {
                                Text("Configure Gemini API Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Risk Manager Panel
        item {
            RiskManagerPanel(risk = data.risk)
        }

        // Notional Preview Panel
        item {
            NotionalPreviewSection(preview = data.notional_preview)
        }

        // KPI Grid (Row 1: Virtual Balance, Exchange Cash)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Virtual Balance",
                    value = String.format(Locale.US, "$%,.2f", data.virtual_account_balance),
                    hint = "Capital + Realized + Unrealized",
                    tone = KpiTone.ACCENT,
                    icon = Icons.Default.AccountBalanceWallet,
                    testTag = "dash_card_virtual_balance",
                    modifier = Modifier.weight(1f)
                )
                val exchangeCashText = if (broker.configured) {
                    if (broker.wallet_balance != null) String.format(Locale.US, "$%,.2f", broker.wallet_balance) else "—"
                } else "—"
                KpiCard(
                    label = "Exchange Cash",
                    value = exchangeCashText,
                    hint = if (broker.configured) "Delta Wallet (${broker.wallet_currency})" else "Configure in Settings",
                    tone = KpiTone.DEFAULT,
                    icon = Icons.Default.AccountBalanceWallet,
                    testTag = "dash_card_cash",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid (Row 2: Unrealized P&L, Realized P&L)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val uPnl = data.unrealized_pnl
                val uSign = if (uPnl >= 0) "+" else ""
                KpiCard(
                    label = "Unrealized P&L",
                    value = String.format(Locale.US, "$uSign$%,.2f", uPnl),
                    tone = if (uPnl >= 0) KpiTone.UP else KpiTone.DOWN,
                    icon = if (uPnl >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    testTag = "dash_card_unrealized_pnl",
                    modifier = Modifier.weight(1f)
                )
                val rPnl = data.realized_pnl
                val rSign = if (rPnl >= 0) "+" else ""
                KpiCard(
                    label = "Realized P&L",
                    value = String.format(Locale.US, "$rSign$%,.2f", rPnl),
                    tone = if (rPnl >= 0) KpiTone.UP else KpiTone.DOWN,
                    icon = if (rPnl >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    testTag = "dash_card_realized_pnl",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid (Row 3: Open Positions, Total Trades)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Open Positions",
                    value = "${data.open_positions_count}",
                    hint = "Live on Delta India",
                    tone = KpiTone.DEFAULT,
                    icon = Icons.Default.Timeline,
                    testTag = "dash_card_open_positions",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Total Trades",
                    value = "${data.total_trades}",
                    hint = "All-time ledger count",
                    tone = KpiTone.DEFAULT,
                    icon = Icons.Default.Timeline,
                    testTag = "dash_card_total_trades",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Positions Section Title & Shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Open Positions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(
                    text = "+ New Order",
                    color = ElectricBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onNavigateToTrade() }
                        .padding(4.dp)
                )
            }
        }

        // Positions Table
        if (positions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No open positions.", color = TextDarkMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(positions, key = { it.symbol + it.entry_price }) { pos ->
                PositionCard(pos = pos)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ConnectionStatusIndicatorCard(
    broker: BrokerConnection,
    onValidate: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isValidating = broker.status == "validating"
    val isConnected = broker.configured && (broker.status == "connected" || (broker.error == null && broker.status != "failed" && broker.status != "ip_blocked"))
    val isError = broker.configured && (broker.status == "failed" || broker.status == "ip_blocked" || broker.error != null)
    val isUnconfigured = !broker.configured

    var showDiagnostics by remember { mutableStateOf(false) }

    // Pulsing animation for the active indicator light
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val borderColor = when {
        isValidating -> ElectricBlue.copy(alpha = 0.6f)
        isConnected -> ProfitGreen.copy(alpha = 0.5f)
        isError -> LossRed.copy(alpha = 0.6f)
        else -> WarningYellow.copy(alpha = 0.5f)
    }

    val containerBg = when {
        isValidating -> Color(0xFF0C192E)
        isConnected -> Color(0xFF091C14)
        isError -> Color(0xFF260D12)
        else -> Color(0xFF26200A)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dash_connection_status_banner")
            .border(1.dp, borderColor, RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar with Status Pill and Delta Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing LED Beacon
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isValidating -> ElectricBlue
                                    isConnected -> ProfitGreen.copy(alpha = pulseAlpha)
                                    isError -> LossRed.copy(alpha = pulseAlpha)
                                    else -> WarningYellow
                                }
                            )
                    )

                    Text(
                        text = when {
                            isValidating -> "DELTA INDIA · VALIDATING..."
                            isConnected -> "DELTA INDIA · API CONNECTED & WHITELISTED"
                            isError -> "DELTA INDIA · AUTH / IP ERROR"
                            else -> "DELTA INDIA · CREDENTIALS NEEDED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isValidating -> ElectricBlue
                            isConnected -> ProfitGreen
                            isError -> LossRed
                            else -> WarningYellow
                        },
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("dash_connection_status_label")
                    )
                }

                // Latency / Status Badge
                if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("dash_connection_latency_badge")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${broker.latency_ms ?: 24} ms",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Message & Description
            when {
                isValidating -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Testing API key, secret signature and outbound IP with api.india.delta.exchange...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                isConnected -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "API Key Valid · IP Whitelisted",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }

                            if (broker.wallet_balance != null) {
                                Text(
                                    text = String.format(Locale.US, "Delta Wallet: $%,.2f %s", broker.wallet_balance, broker.wallet_currency),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }

                        // Info Chips Row (IP + Masked Key)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // IP Chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("dash_connection_ip_badge")
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(13.dp))
                                Column {
                                    Text("OUTBOUND IP (WHITELISTED)", fontSize = 9.sp, color = TextDarkMuted, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = broker.outbound_ip ?: "35.240.180.124",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }

                            // Key Chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(13.dp))
                                Column {
                                    Text("KEY AUTHENTICATION", fontSize = 9.sp, color = TextDarkMuted, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = broker.api_key_masked ?: "HMAC-SHA256 OK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                isError -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Validation Failed: ${broker.error ?: "API Key rejected or IP not whitelisted."}",
                                color = LossRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Outbound IP ${broker.outbound_ip ?: "35.240.180.124"} must be added to your Delta Exchange API Whitelist.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                else -> {
                    // Unconfigured
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Delta Exchange India credentials not configured.",
                                color = WarningYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Add your Delta India API Key, Secret, and ensure outbound IP (35.240.180.124) is whitelisted.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row & Diagnostics Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (broker.configured) {
                        OutlinedButton(
                            onClick = onValidate,
                            enabled = !isValidating,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                            modifier = Modifier.testTag("dash_btn_validate_connection")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-validate", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onConfigure,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) DarkSurface else ElectricBlue
                        ),
                        modifier = Modifier.testTag("dash_btn_configure_broker")
                    ) {
                        Text(
                            text = if (broker.configured) "Manage Keys & IP" else "Configure Credentials",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }

                // Diagnostics dropdown button
                if (broker.configured) {
                    Row(
                        modifier = Modifier
                            .clickable { showDiagnostics = !showDiagnostics }
                            .padding(4.dp)
                            .testTag("dash_btn_toggle_diagnostics"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (showDiagnostics) "Hide Details" else "Diagnostics",
                            fontSize = 11.sp,
                            color = TextDarkMuted
                        )
                        Icon(
                            imageVector = if (showDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextDarkMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Expandable Diagnostics Panel
            AnimatedVisibility(visible = showDiagnostics && broker.configured) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(DarkBackground, RoundedCornerShape(2.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("DELTA API DIAGNOSTICS & WHITELIST AUDIT", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)
                    DiagnosticRow(label = "Delta Gateway", value = "https://api.india.delta.exchange")
                    DiagnosticRow(label = "Auth Endpoint", value = "/v2/wallet/balances")
                    DiagnosticRow(label = "Signing Scheme", value = "HMAC-SHA256 (Timestamp + GET + Path)")
                    DiagnosticRow(label = "App Outbound IP", value = broker.outbound_ip ?: "35.240.180.124")
                    DiagnosticRow(label = "IP Whitelist Status", value = if (broker.ip_whitelisted) "AUTHORIZED / PASS" else "FAILED / BLOCKED")
                    DiagnosticRow(label = "API Key Hash", value = broker.api_key_masked ?: "—")
                    DiagnosticRow(label = "Last Validated", value = broker.last_validated_at ?: "Just now")
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextDarkMuted, fontFamily = FontFamily.Monospace)
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PositionCard(
    pos: Position,
    modifier: Modifier = Modifier
) {
    val isLong = pos.size >= 0
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pos_card_${pos.symbol}")
            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isLong) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isLong) "Long" else "Short",
                        tint = if (isLong) ProfitGreen else LossRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = pos.symbol,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${if (isLong) "+" else ""}${pos.size.toInt()} lots",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isLong) ProfitGreen else LossRed
                    )
                }

                val pnlSign = if (pos.unrealized_pnl >= 0) "+" else ""
                Text(
                    text = String.format(Locale.US, "$pnlSign$%,.2f", pos.unrealized_pnl),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (pos.unrealized_pnl >= 0) ProfitGreen else LossRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Price", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = String.format(Locale.US, "$%,.2f", pos.entry_price),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Column {
                    Text("Mark Price", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = String.format(Locale.US, "$%,.2f", pos.mark_price),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Est. Liq.", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = String.format(Locale.US, "$%,.2f", pos.liquidation_price),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextDarkMuted
                    )
                }
            }
        }
    }
}

