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
import com.example.model.ActivityLogItem
import com.example.theme.DarkBackground
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.ElectricBlue
import com.example.theme.LossRed
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import com.example.theme.WarningYellow
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<ActivityLogItem>,
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
                        text = "TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Activity Log",
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
                    modifier = Modifier.testTag("log_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Terminal Log Container
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_panel")
                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(2.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .testTag("log_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity yet. Actions and broker events will appear here.", color = TextDarkMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        logs.forEach { logItem ->
                            LogItemRow(log = logItem)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LogItemRow(
    log: ActivityLogItem,
    modifier: Modifier = Modifier
) {
    val levelColor = when (log.level.lowercase(Locale.US)) {
        "info" -> ElectricBlue
        "warn" -> WarningYellow
        "error" -> LossRed
        else -> TextDarkMuted
    }

    val timeStr = try {
        log.created_at.substringAfter("T").take(8)
    } catch (e: Exception) {
        log.created_at
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F), RoundedCornerShape(2.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeStr,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextDarkMuted,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = log.level.uppercase(Locale.US),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = levelColor,
            modifier = Modifier.width(44.dp)
        )

        Text(
            text = log.event,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.width(110.dp)
        )

        Text(
            text = log.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}
