package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TradeRecord
import com.example.theme.DarkBackground
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.LossRed
import com.example.theme.ProfitGreen
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import java.util.Locale

@Composable
fun HistoryScreen(
    trades: List<TradeRecord>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        text = "LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Trade History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    modifier = Modifier.testTag("hist_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Ledger Table
        if (trades.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hist_empty_state")
                        .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trades recorded in ledger yet.", color = TextDarkMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(trades, key = { it.id + it.created_at }) { trade ->
                TradeHistoryCard(trade = trade)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TradeHistoryCard(
    trade: TradeRecord,
    modifier: Modifier = Modifier
) {
    val isBuy = trade.side.equals("buy", ignoreCase = true)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trade_item_${trade.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = trade.symbol,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .background(if (isBuy) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .border(1.dp, if (isBuy) ProfitGreen.copy(alpha = 0.4f) else LossRed.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.side.uppercase(Locale.US),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isBuy) ProfitGreen else LossRed
                        )
                    }
                }

                if (trade.realized_pnl != null) {
                    val pnl = trade.realized_pnl
                    val sign = if (pnl >= 0) "+" else ""
                    Text(
                        text = String.format(Locale.US, "$sign$%,.2f", pnl),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (pnl >= 0) ProfitGreen else LossRed
                    )
                } else {
                    Text(
                        text = trade.status.uppercase(Locale.US),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Qty", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text("${trade.quantity.toInt()} lots", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                }
                Column {
                    Text("Entry Price", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(String.format(Locale.US, "$%,.2f", trade.entry_price), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextMuted)
                }
                Column {
                    Text("Exit Price", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    Text(
                        text = if (trade.exit_price != null) String.format(Locale.US, "$%,.2f", trade.exit_price) else "—",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Time", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                    val timeDisplay = try {
                        trade.created_at.substringAfter("T").take(8)
                    } catch (e: Exception) {
                        trade.created_at
                    }
                    Text(timeDisplay, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextDarkMuted)
                }
            }
        }
    }
}
