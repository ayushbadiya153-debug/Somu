package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotionalPreview
import com.example.model.Position
import com.example.model.RiskStatus
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.ElectricBlue
import com.example.theme.LossRed
import com.example.theme.ProfitGreen
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import com.example.theme.WarningYellow
import java.util.Locale

enum class KpiTone {
    DEFAULT, UP, DOWN, ACCENT
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    tone: KpiTone = KpiTone.DEFAULT,
    icon: ImageVector? = null,
    testTag: String = ""
) {
    val valueColor = when (tone) {
        KpiTone.DEFAULT -> Color.White
        KpiTone.UP -> ProfitGreen
        KpiTone.DOWN -> LossRed
        KpiTone.ACCENT -> ElectricBlue
    }

    Card(
        modifier = modifier
            .testTag(testTag)
            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(14.dp),
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = valueColor
            )

            if (hint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextDarkMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LiveTradingBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .testTag("nav_live_badge")
            .background(Color(0xFF2B0E0D), RoundedCornerShape(2.dp))
            .border(1.dp, LossRed.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha)
                .background(LossRed, CircleShape)
        )
        Text(
            text = "LIVE REAL TRADING",
            color = LossRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun EngineStatusBadge(
    running: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (running) ProfitGreen.copy(alpha = 0.5f) else DarkBorder
    val bgColor = if (running) Color(0xFF092812) else Color(0xFF181818)
    val dotColor = if (running) ProfitGreen else TextDarkMuted
    val textColor = if (running) ProfitGreen else TextMuted

    Row(
        modifier = modifier
            .testTag("nav_engine_status")
            .background(bgColor, RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = if (running) "ENGINE RUNNING" else "ENGINE IDLE",
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RiskManagerPanel(
    risk: RiskStatus?,
    modifier: Modifier = Modifier
) {
    if (risk == null) return
    val isBlocked = risk.blocked_reason != null
    val borderCol = if (isBlocked) LossRed.copy(alpha = 0.5f) else DarkBorder
    val bgCol = if (isBlocked) Color(0xFF200A0A) else DarkSurface

    Card(
        modifier = modifier
            .testTag("risk_panel")
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = bgCol),
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Risk Manager",
                        modifier = Modifier.size(14.dp),
                        tint = if (isBlocked) LossRed else ElectricBlue
                    )
                    Text(
                        text = "RISK MANAGER · TODAY (${risk.date})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Text(
                    text = if (isBlocked) "ENTRIES BLOCKED" else "ENTRIES ALLOWED",
                    color = if (isBlocked) LossRed else ProfitGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entries Today", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = "${risk.entries} / ${risk.entries_max}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (risk.entries >= risk.entries_max) LossRed else Color.White,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text("SL Hits Today", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = "${risk.sl_hits} / ${risk.sl_max}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (risk.sl_hits >= risk.sl_max) LossRed else Color.White,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text("SL Cooldown", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = "${risk.cooldown_min} min",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotionalPreviewSection(
    preview: NotionalPreview?,
    modifier: Modifier = Modifier
) {
    if (preview == null || preview.symbols.isEmpty()) return

    Card(
        modifier = modifier
            .testTag("notional_preview")
            .fillMaxWidth()
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
                Text(
                    text = "NOTIONAL CAP · $${preview.max_notional_usd.toInt()} PER ENTRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "${preview.symbols.size} symbols",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDarkMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SYMBOL", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted, modifier = Modifier.weight(1.2f))
                Text("PRICE", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted, modifier = Modifier.weight(1f))
                Text("MAX LOTS", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted, modifier = Modifier.weight(1f))
                Text("NOTIONAL", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted, modifier = Modifier.weight(1f))
            }

            preview.symbols.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.symbol,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = if (item.ref_price != null) String.format(Locale.US, "$%.2f", item.ref_price) else "—",
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.max_lots ?: 1}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfitGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val used = (item.max_lots ?: 1) * (item.notional_per_lot ?: 0.0)
                    Text(
                        text = String.format(Locale.US, "$%.2f", used),
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
