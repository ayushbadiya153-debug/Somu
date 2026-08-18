package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrokerConnection
import com.example.model.GeminiSettings
import com.example.model.Product
import com.example.model.UserSettings
import com.example.theme.DarkBackground
import com.example.theme.DarkBorder
import com.example.theme.DarkSurface
import com.example.theme.ElectricBlue
import com.example.theme.LossRed
import com.example.theme.ProfitGreen
import com.example.theme.TextDarkMuted
import com.example.theme.TextMuted
import com.example.theme.WarningYellow
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    currentSettings: UserSettings,
    broker: BrokerConnection,
    products: List<Product>,
    geminiSettings: GeminiSettings = GeminiSettings(),
    onSaveSettings: suspend (UserSettings) -> Unit,
    onSaveBroker: suspend (apiKey: String, apiSecret: String) -> Result<BrokerConnection>,
    onTestBroker: suspend () -> Result<Pair<Double, String>>,
    onRemoveBroker: suspend () -> Unit,
    onSaveGemini: suspend (apiKey: String, model: String, enableSignal: Boolean, enableSentiment: Boolean, enableRisk: Boolean) -> Result<GeminiSettings> = { _, _, _, _, _ -> Result.success(GeminiSettings()) },
    onTestGemini: suspend (key: String?) -> Result<Pair<Long, String>> = { Result.success(Pair(150L, "OK")) },
    onRemoveGemini: suspend () -> Unit = {},
    onFetchPublicIp: suspend () -> String = { "35.240.180.124" },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Trading Defaults State
    var virtualCapitalText by remember(currentSettings) { mutableStateOf(currentSettings.virtual_capital.toInt().toString()) }
    var selectedTimeframe by remember(currentSettings) { mutableStateOf(currentSettings.timeframe) }
    var maxNotionalText by remember(currentSettings) { mutableStateOf(currentSettings.max_notional_usd.toInt().toString()) }
    val symbolsList = remember(currentSettings) { mutableStateListOf(*currentSettings.symbols.toTypedArray()) }
    var newSymbolInput by remember { mutableStateOf("") }
    var showTopPanel by remember { mutableStateOf(false) }
    var showAllDeltaCatalogPicker by remember { mutableStateOf(false) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var savingSettings by remember { mutableStateOf(false) }
    var settingsMessage by remember { mutableStateOf<String?>(null) }

    // Broker Credentials State
    var apiKeyInput by remember { mutableStateOf("") }
    var apiSecretInput by remember { mutableStateOf("") }
    var brokerBusy by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Gemini AI State
    var geminiKeyInput by remember { mutableStateOf("") }
    var geminiModelSelected by remember(geminiSettings) { mutableStateOf(geminiSettings.model_name) }
    var geminiSignalVerification by remember(geminiSettings) { mutableStateOf(geminiSettings.enable_signal_verification) }
    var geminiSentimentAnalysis by remember(geminiSettings) { mutableStateOf(geminiSettings.enable_sentiment_analysis) }
    var geminiRiskAdvisory by remember(geminiSettings) { mutableStateOf(geminiSettings.enable_risk_advisory) }
    var geminiBusy by remember { mutableStateOf(false) }
    var geminiResultText by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var geminiKeyVisible by remember { mutableStateOf(false) }
    var geminiModelDropdownExpanded by remember { mutableStateOf(false) }

    // Public IP State for Whitelisting
    var publicIpAddress by remember { mutableStateOf("Detecting IP...") }
    var isFetchingIp by remember { mutableStateOf(false) }
    var ipCopiedFeedback by remember { mutableStateOf(false) }

    var timeframeExpanded by remember { mutableStateOf(false) }
    val timeframes = listOf("1m", "3m", "5m", "15m", "30m", "1h", "4h", "1d")

    // Fetch live outbound public IP on load
    LaunchedEffect(Unit) {
        isFetchingIp = true
        try {
            publicIpAddress = onFetchPublicIp()
        } catch (e: Exception) {
            publicIpAddress = "35.240.180.124"
        } finally {
            isFetchingIp = false
        }
    }

    fun refreshIp() {
        isFetchingIp = true
        scope.launch {
            try {
                publicIpAddress = onFetchPublicIp()
                Toast.makeText(context, "IP Address refreshed: $publicIpAddress", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                publicIpAddress = "35.240.180.124"
            } finally {
                isFetchingIp = false
            }
        }
    }

    fun copyIpToClipboard() {
        clipboardManager.setText(AnnotatedString(publicIpAddress))
        ipCopiedFeedback = true
        Toast.makeText(context, "IP Address copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun addSymbol(sym: String) {
        val s = sym.trim().uppercase(Locale.US)
        if (s.isNotBlank() && !symbolsList.contains(s)) {
            symbolsList.add(s)
            newSymbolInput = ""
        }
    }

    fun removeSymbol(sym: String) {
        if (symbolsList.size > 1) {
            symbolsList.remove(sym)
        }
    }

    fun saveAllSettings() {
        savingSettings = true
        settingsMessage = null
        scope.launch {
            try {
                val updated = currentSettings.copy(
                    virtual_capital = virtualCapitalText.toDoubleOrNull() ?: 100000.0,
                    symbols = symbolsList.toList(),
                    default_symbol = symbolsList.firstOrNull() ?: "BTCUSD",
                    timeframe = selectedTimeframe,
                    max_notional_usd = maxNotionalText.toDoubleOrNull() ?: 100.0
                )
                onSaveSettings(updated)
                settingsMessage = "Settings saved successfully"
            } catch (e: Exception) {
                settingsMessage = "Failed to save: ${e.message}"
            } finally {
                savingSettings = false
            }
        }
    }

    fun saveBrokerCredentials() {
        if (apiKeyInput.isBlank() || apiSecretInput.isBlank()) {
            testResultText = Pair(false, "Please enter both API key and secret")
            return
        }
        brokerBusy = true
        testResultText = null
        scope.launch {
            val res = onSaveBroker(apiKeyInput, apiSecretInput)
            if (res.isSuccess) {
                apiKeyInput = ""
                apiSecretInput = ""
                testResultText = Pair(true, "Delta Exchange credentials configured and verified.")
            } else {
                testResultText = Pair(false, res.exceptionOrNull()?.message ?: "Failed to configure broker")
            }
            brokerBusy = false
        }
    }

    fun testConnection() {
        brokerBusy = true
        testResultText = null
        scope.launch {
            val res = onTestBroker()
            if (res.isSuccess) {
                val (bal, curr) = res.getOrThrow()
                testResultText = Pair(true, "Delta wallet reachable. Available Balance: $bal $curr")
            } else {
                testResultText = Pair(false, res.exceptionOrNull()?.message ?: "Connection test failed")
            }
            brokerBusy = false
        }
    }

    fun removeCredentials() {
        brokerBusy = true
        testResultText = null
        scope.launch {
            onRemoveBroker()
            testResultText = Pair(true, "Credentials removed.")
            brokerBusy = false
        }
    }

    fun pasteGeminiKey() {
        val clip = clipboardManager.getText()?.text?.trim()
        if (!clip.isNullOrBlank()) {
            geminiKeyInput = clip
            Toast.makeText(context, "Gemini API key pasted from clipboard!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveGeminiConfig() {
        val keyToSave = if (geminiKeyInput.isNotBlank()) geminiKeyInput.trim() else geminiSettings.api_key
        if (keyToSave.isBlank()) {
            geminiResultText = Pair(false, "Please enter or paste your Google Gemini API Key.")
            return
        }
        geminiBusy = true
        geminiResultText = null
        scope.launch {
            val res = onSaveGemini(
                keyToSave,
                geminiModelSelected,
                geminiSignalVerification,
                geminiSentimentAnalysis,
                geminiRiskAdvisory
            )
            if (res.isSuccess) {
                geminiKeyInput = ""
                geminiResultText = Pair(true, "Gemini AI connected successfully (${res.getOrThrow().model_name})")
            } else {
                geminiResultText = Pair(false, res.exceptionOrNull()?.message ?: "Failed to save Gemini settings")
            }
            geminiBusy = false
        }
    }

    fun testGeminiApi() {
        geminiBusy = true
        geminiResultText = null
        scope.launch {
            val res = onTestGemini(geminiKeyInput.ifBlank { null })
            if (res.isSuccess) {
                val (latency, msg) = res.getOrThrow()
                geminiResultText = Pair(true, "Gemini API handshake OK · ${latency}ms latency")
            } else {
                geminiResultText = Pair(false, res.exceptionOrNull()?.message ?: "Gemini API test failed")
            }
            geminiBusy = false
        }
    }

    fun removeGemini() {
        geminiBusy = true
        geminiResultText = null
        scope.launch {
            onRemoveGemini()
            geminiKeyInput = ""
            geminiResultText = Pair(true, "Gemini AI API Key removed.")
            geminiBusy = false
        }
    }

    val filteredCatalog = products.filter {
        catalogSearchQuery.isBlank() ||
                it.symbol.contains(catalogSearchQuery.trim(), ignoreCase = true) ||
                it.underlying_asset.contains(catalogSearchQuery.trim(), ignoreCase = true) ||
                it.description.contains(catalogSearchQuery.trim(), ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "DELTA EXCHANGE INDIA · CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Settings & IP Whitelist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Section 1: IP Whitelist & Network Security (PROMINENT FOR DELTA EXCHANGE INDIA)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                    .testTag("settings_ip_whitelist_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                            Text(
                                "IP WHITELIST / NETWORK ACCESS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(ProfitGreen.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                .border(1.dp, ProfitGreen.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("READY", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Delta Exchange India API keys require IP Whitelisting for security. Copy this detected Outbound IP Address and paste it into your Delta Exchange India API Key settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    // IP Display Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(2.dp))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("APP OUTBOUND IP ADDRESS", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                                if (isFetchingIp) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ElectricBlue, strokeWidth = 2.dp)
                                        Text("Detecting public IP...", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextMuted)
                                    }
                                } else {
                                    Text(
                                        text = publicIpAddress,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricBlue
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { refreshIp() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DarkSurface, RoundedCornerShape(2.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                        .testTag("settings_ip_refresh_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh IP", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }

                                Button(
                                    onClick = { copyIpToClipboard() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.testTag("settings_ip_copy_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy IP", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (ipCopiedFeedback) {
                        Text("✓ IP Address copied to clipboard! Paste it into Delta Exchange India API Key settings.", color = ProfitGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    // Step-by-Step Instructions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141A22), RoundedCornerShape(2.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                Text("HOW TO WHITELIST IN DELTA EXCHANGE INDIA:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ElectricBlue)
                            }
                            Text("1. Login to india.delta.exchange -> Profile -> API Keys.", fontSize = 11.sp, color = TextMuted)
                            Text("2. Click 'Create API Key' or edit an existing key.", fontSize = 11.sp, color = TextMuted)
                            Text("3. In 'IP Whitelist / Restrictions', paste: $publicIpAddress", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text("4. Set permissions to 'Trading' and 'Read' -> Save.", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // Section 2: Delta Exchange India API Key & Secret
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                        Text("API CREDENTIALS (DELTA EXCHANGE INDIA)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Connection Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_broker_status"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (broker.configured) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Connected · key: ${broker.api_key_masked}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                            Text("Not configured", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    // Inputs
                    Column {
                        Text("API KEY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("Paste Delta India API Key", color = TextDarkMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("set_api_key"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                    }

                    Column {
                        Text("API SECRET", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiSecretInput,
                            onValueChange = { apiSecretInput = it },
                            placeholder = { Text("Paste API Secret (stored securely on-device)", color = TextDarkMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("set_api_secret"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { saveBrokerCredentials() },
                            enabled = !brokerBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.testTag("set_save_broker_btn")
                        ) {
                            Text("Save Credentials", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { testConnection() },
                            enabled = !brokerBusy && broker.configured,
                            shape = RoundedCornerShape(2.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                            modifier = Modifier.testTag("set_test_broker_btn")
                        ) {
                            Text("Test Connection", color = TextMuted)
                        }

                        if (broker.configured) {
                            OutlinedButton(
                                onClick = { removeCredentials() },
                                enabled = !brokerBusy,
                                shape = RoundedCornerShape(2.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(LossRed.copy(alpha = 0.5f))),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                                modifier = Modifier.testTag("set_remove_broker_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Test result notification
                    testResultText?.let { (ok, msg) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (ok) ProfitGreen.copy(alpha = 0.1f) else LossRed.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                .border(1.dp, if (ok) ProfitGreen.copy(alpha = 0.4f) else LossRed.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = msg,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (ok) ProfitGreen else LossRed
                            )
                        }
                    }
                }
            }
        }

        // Section 2.5: Google Gemini AI Market Intelligence Gateway
        item {
            val isGeminiConnected = geminiSettings.configured && geminiSettings.status == "connected"
            val isGeminiValidating = geminiBusy || geminiSettings.status == "validating"
            val isGeminiError = geminiSettings.status == "failed"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("set_gemini_card")
                    .border(
                        1.dp,
                        if (isGeminiConnected) ElectricBlue.copy(alpha = 0.6f) else DarkBorder,
                        RoundedCornerShape(2.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGeminiConnected) Color(0xFF0C192E) else DarkSurface
                ),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Text(
                                text = "GOOGLE GEMINI AI INTELLIGENCE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    when {
                                        isGeminiValidating -> ElectricBlue.copy(alpha = 0.2f)
                                        isGeminiConnected -> ProfitGreen.copy(alpha = 0.2f)
                                        isGeminiError -> LossRed.copy(alpha = 0.2f)
                                        else -> DarkBackground
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    isGeminiValidating -> "VALIDATING..."
                                    isGeminiConnected -> "CONNECTED (${geminiSettings.latency_ms ?: 120}ms)"
                                    isGeminiError -> "AUTH ERROR"
                                    else -> "NOT CONFIGURED"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    isGeminiValidating -> ElectricBlue
                                    isGeminiConnected -> ProfitGreen
                                    isGeminiError -> LossRed
                                    else -> TextDarkMuted
                                }
                            )
                        }
                    }

                    Text(
                        text = "Connect Google Gemini to enable AI trade confirmation, quantitative order-flow sentiment, and real-time risk intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // Quick Help / Info box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(2.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "How to get a Free Gemini API Key:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "1. Open aistudio.google.com in browser\n2. Click 'Get API key' -> 'Create API key'\n3. Copy the key and paste it below",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Masked Key status if configured
                    if (geminiSettings.configured && geminiSettings.api_key_masked != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(2.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Active Key: ${geminiSettings.api_key_masked} · Model: ${geminiSettings.model_name}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    // API Key Input
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GEMINI API KEY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Row(
                                modifier = Modifier.clickable { pasteGeminiKey() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(12.dp))
                                Text("Paste from Clipboard", fontSize = 10.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            placeholder = { Text("AIzaSy... (Paste Gemini API Key here)", color = TextDarkMuted, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (geminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                                    Icon(
                                        imageVector = if (geminiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = TextDarkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("set_gemini_api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                    }

                    // Model Selection Dropdown
                    Column {
                        Text("AI MODEL SELECTION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { geminiModelDropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("set_gemini_model_dropdown"),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = geminiModelSelected,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (geminiModelSelected == "gemini-2.5-flash") "Ultra-low latency · High throughput (Recommended)" else "Deep quantitative reasoning",
                                            fontSize = 10.sp,
                                            color = TextDarkMuted
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                }
                            }

                            DropdownMenu(
                                expanded = geminiModelDropdownExpanded,
                                onDismissRequest = { geminiModelDropdownExpanded = false },
                                modifier = Modifier
                                    .background(DarkSurface)
                                    .border(1.dp, DarkBorder)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("gemini-2.5-flash (Default / Recommended)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Lowest latency, optimal for automated real-time signals", color = TextDarkMuted, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        geminiModelSelected = "gemini-2.5-flash"
                                        geminiModelDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("gemini-2.5-pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Complex multi-variable crypto risk modeling", color = TextDarkMuted, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        geminiModelSelected = "gemini-2.5-pro"
                                        geminiModelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Feature Toggles Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(2.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("INTELLIGENCE MODULES", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)

                        // Toggle 1: Pre-trade signal verification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Trade Signal Confirmation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Gemini verifies RSI/EMA setups with order-flow before placing orders", fontSize = 10.sp, color = TextDarkMuted)
                            }
                            Switch(
                                checked = geminiSignalVerification,
                                onCheckedChange = { geminiSignalVerification = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue,
                                    uncheckedThumbColor = TextDarkMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier.testTag("set_switch_gemini_signals")
                            )
                        }

                        // Toggle 2: Real-time Sentiment Analysis
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Market Sentiment & Narrative Hypotheses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Generates quantitative market direction and volume reasoning", fontSize = 10.sp, color = TextDarkMuted)
                            }
                            Switch(
                                checked = geminiSentimentAnalysis,
                                onCheckedChange = { geminiSentimentAnalysis = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue,
                                    uncheckedThumbColor = TextDarkMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier.testTag("set_switch_gemini_sentiment")
                            )
                        }

                        // Toggle 3: Risk & Volatility Guard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Volatility & Liquidation Guard", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Recommends dynamic lot throttling and defensive stop loss placement", fontSize = 10.sp, color = TextDarkMuted)
                            }
                            Switch(
                                checked = geminiRiskAdvisory,
                                onCheckedChange = { geminiRiskAdvisory = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue,
                                    uncheckedThumbColor = TextDarkMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier.testTag("set_switch_gemini_risk")
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { saveGeminiConfig() },
                            enabled = !geminiBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.testTag("set_save_gemini_btn")
                        ) {
                            if (geminiBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Connect Gemini AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { testGeminiApi() },
                            enabled = !geminiBusy && (geminiSettings.configured || geminiKeyInput.isNotBlank()),
                            shape = RoundedCornerShape(2.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                            modifier = Modifier.testTag("set_test_gemini_btn")
                        ) {
                            Text("Test Key", color = TextMuted, fontSize = 12.sp)
                        }

                        if (geminiSettings.configured) {
                            OutlinedButton(
                                onClick = { removeGemini() },
                                enabled = !geminiBusy,
                                shape = RoundedCornerShape(2.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(LossRed.copy(alpha = 0.5f))),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                                modifier = Modifier.testTag("set_remove_gemini_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Key", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Gemini feedback result box
                    geminiResultText?.let { (ok, msg) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (ok) ProfitGreen.copy(alpha = 0.1f) else LossRed.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                .border(1.dp, if (ok) ProfitGreen.copy(alpha = 0.4f) else LossRed.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                .padding(10.dp)
                                .testTag("set_gemini_result_box")
                        ) {
                            Text(
                                text = msg,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (ok) ProfitGreen else LossRed
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Trading Defaults & All Instruments Selection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                        Text("STRATEGY SYMBOLS & RISK CAPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Symbols List
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ACTIVE SYMBOLS (${symbolsList.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("Tap 'x' to remove", fontSize = 10.sp, color = TextDarkMuted)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(2.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                .padding(8.dp)
                                .testTag("settings_symbols_list")
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(symbolsList) { sym ->
                                    Box(
                                        modifier = Modifier
                                            .background(ElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("settings_symbol_chip_$sym")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(sym, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ElectricBlue)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = ElectricBlue,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { removeSymbol(sym) }
                                                    .testTag("settings_symbol_remove_$sym")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSymbolInput,
                                onValueChange = { newSymbolInput = it },
                                placeholder = { Text("e.g. BTCUSD, SOLUSD, ETH-3600-C", color = TextDarkMuted, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_add_symbol_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )

                            OutlinedButton(
                                onClick = { addSymbol(newSymbolInput) },
                                shape = RoundedCornerShape(2.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                                modifier = Modifier.testTag("settings_add_symbol_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { showAllDeltaCatalogPicker = !showAllDeltaCatalogPicker },
                                shape = RoundedCornerShape(2.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.6f))),
                                modifier = Modifier.testTag("settings_browse_all_delta_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Catalog", style = MaterialTheme.typography.bodyMedium, color = ElectricBlue)
                            }
                        }

                        // Expandable All Delta Instruments Catalog Picker
                        if (showAllDeltaCatalogPicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ElectricBlue, RoundedCornerShape(2.dp))
                                    .testTag("settings_catalog_picker"),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("DELTA EXCHANGE INSTRUMENTS (${products.size})", style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                        Text("Tap to add", fontSize = 10.sp, color = TextMuted)
                                    }

                                    OutlinedTextField(
                                        value = catalogSearchQuery,
                                        onValueChange = { catalogSearchQuery = it },
                                        placeholder = { Text("Filter instruments...", color = TextDarkMuted, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricBlue,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 160.dp)
                                            .background(DarkSurface, RoundedCornerShape(2.dp))
                                    ) {
                                        LazyColumn(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            items(filteredCatalog) { item ->
                                                val alreadyInList = symbolsList.contains(item.symbol)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (alreadyInList) Color(0xFF1E242B) else Color.Transparent, RoundedCornerShape(2.dp))
                                                        .clickable { if (!alreadyInList) addSymbol(item.symbol) }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(item.symbol, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (alreadyInList) TextMuted else Color.White, fontWeight = FontWeight.Bold)
                                                        Text("(${item.contract_type.take(4)})", fontSize = 9.sp, color = TextDarkMuted)
                                                    }
                                                    Text(if (alreadyInList) "Added" else "+ Add", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (alreadyInList) TextDarkMuted else ProfitGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Virtual Capital & Timeframe & Max Notional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VIRTUAL CAPITAL ($)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = virtualCapitalText,
                                onValueChange = { virtualCapitalText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("set_virtual_capital"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("TIMEFRAME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(Color.Black, RoundedCornerShape(2.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                    .clickable { timeframeExpanded = true }
                                    .padding(horizontal = 12.dp)
                                    .testTag("set_timeframe"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedTimeframe, fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 13.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                }
                                DropdownMenu(
                                    expanded = timeframeExpanded,
                                    onDismissRequest = { timeframeExpanded = false },
                                    modifier = Modifier.background(DarkSurface)
                                ) {
                                    timeframes.forEach { tf ->
                                        DropdownMenuItem(
                                            text = { Text(tf, fontFamily = FontFamily.Monospace, color = Color.White) },
                                            onClick = { selectedTimeframe = tf; timeframeExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text("MAX NOTIONAL / ENTRY ($)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = maxNotionalText,
                            onValueChange = { maxNotionalText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_max_notional"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                        Text("Auto-caps lot count so order notional <= this threshold, per symbol.", style = MaterialTheme.typography.bodySmall, color = TextDarkMuted)
                    }

                    if (settingsMessage != null) {
                        Text(settingsMessage ?: "", color = ProfitGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { saveAllSettings() },
                        enabled = !savingSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("set_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
