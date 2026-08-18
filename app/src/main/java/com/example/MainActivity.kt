package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.DarkBackground
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.ElectricBlue
import com.example.theme.NexusTradeTheme
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import com.example.ui.DashboardScreen
import com.example.ui.HistoryScreen
import com.example.ui.LogsScreen
import com.example.ui.SettingsScreen
import com.example.ui.StrategiesScreen
import com.example.ui.TradeScreen
import com.example.ui.components.EngineStatusBadge
import com.example.ui.components.LiveTradingBadge
import kotlinx.coroutines.launch

enum class AppNavTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Assessment, "nav_tab_dashboard"),
    TRADE("Trade", Icons.Default.ShowChart, "nav_tab_trade"),
    STRATEGIES("Strategies", Icons.Default.Memory, "nav_tab_strategies"),
    HISTORY("History", Icons.Default.History, "nav_tab_history"),
    LOGS("Logs", Icons.Default.Terminal, "nav_tab_logs"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NexusTradeTheme {
                val repository = NexusTradeApp.instance.repository
                val scope = rememberCoroutineScope()

                var currentTab by remember { mutableStateOf(AppNavTab.DASHBOARD) }

                val dashboardData by repository.dashboard.collectAsState()
                val products by repository.products.collectAsState()
                val strategies by repository.strategies.collectAsState()
                val trades by repository.trades.collectAsState()
                val logs by repository.logs.collectAsState()
                val settings by repository.settings.collectAsState()
                val broker by repository.broker.collectAsState()
                val engine by repository.engine.collectAsState()
                val geminiSettings by repository.geminiSettings.collectAsState()
                val geminiAnalysis by repository.geminiAnalysis.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    topBar = {
                        NexusTradeTopBar(engineRunning = engine.running)
                    },
                    bottomBar = {
                        NexusTradeBottomBar(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppNavTab.DASHBOARD -> DashboardScreen(
                                data = dashboardData,
                                geminiSettings = geminiSettings,
                                geminiAnalysis = geminiAnalysis,
                                onStartEngine = { scope.launch { repository.startEngine() } },
                                onStopEngine = { scope.launch { repository.stopEngine() } },
                                onRefresh = { scope.launch { repository.refreshData() } },
                                onValidateConnection = { scope.launch { repository.testBrokerConnection() } },
                                onRequestGeminiAnalysis = { symbol -> repository.requestGeminiMarketAnalysis(symbol) },
                                onNavigateToTrade = { currentTab = AppNavTab.TRADE },
                                onNavigateToSettings = { currentTab = AppNavTab.SETTINGS }
                            )

                            AppNavTab.TRADE -> TradeScreen(
                                products = products,
                                broker = broker,
                                onPlaceOrder = { req -> repository.placeOrder(req) },
                                onNavigateToSettings = { currentTab = AppNavTab.SETTINGS }
                            )

                            AppNavTab.STRATEGIES -> StrategiesScreen(
                                strategies = strategies,
                                onToggleStrategy = { key, en -> scope.launch { repository.toggleStrategy(key, en) } },
                                onUpdateParams = { key, p -> scope.launch { repository.updateStrategyParams(key, p) } }
                            )

                            AppNavTab.HISTORY -> HistoryScreen(
                                trades = trades,
                                onRefresh = { scope.launch { repository.refreshData() } }
                            )

                            AppNavTab.LOGS -> LogsScreen(
                                logs = logs,
                                onRefresh = { scope.launch { repository.refreshData() } }
                            )

                            AppNavTab.SETTINGS -> SettingsScreen(
                                currentSettings = settings,
                                broker = broker,
                                products = products,
                                geminiSettings = geminiSettings,
                                onSaveSettings = { s -> repository.updateSettings(s) },
                                onSaveBroker = { k, sec -> repository.saveBroker(k, sec) },
                                onTestBroker = { repository.testBrokerConnection() },
                                onRemoveBroker = { repository.removeBroker() },
                                onSaveGemini = { key, model, sig, sent, risk -> repository.saveGeminiSettings(key, model, sig, sent, risk) },
                                onTestGemini = { key -> repository.testGeminiConnection(key) },
                                onRemoveGemini = { repository.removeGeminiSettings() },
                                onFetchPublicIp = { repository.getPublicIp() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NexusTradeTopBar(
    engineRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkBorder)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(ElectricBlue, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Column {
                    Text(
                        text = "NEXUS TRADE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Delta Exchange India",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveTradingBadge()
                EngineStatusBadge(running = engineRunning)
            }
        }
    }
}

@Composable
fun NexusTradeBottomBar(
    currentTab: AppNavTab,
    onSelectTab: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .border(1.dp, DarkBorder)
            .testTag("main_bottom_nav"),
        containerColor = DarkSurface,
        contentColor = TextMuted,
        tonalElevation = 0.dp
    ) {
        AppNavTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricBlue,
                    selectedTextColor = ElectricBlue,
                    unselectedIconColor = TextDarkMuted,
                    unselectedTextColor = TextDarkMuted,
                    indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag(tab.tag)
            )
        }
    }
}
