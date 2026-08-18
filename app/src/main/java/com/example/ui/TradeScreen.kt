package com.example.ui

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrokerConnection
import com.example.model.OrderRequest
import com.example.model.OrderResponse
import com.example.model.Product
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    products: List<Product>,
    broker: BrokerConnection,
    onPlaceOrder: suspend (OrderRequest) -> OrderResponse,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedProductId by remember(products) { mutableStateOf(products.firstOrNull()?.id?.toString() ?: "87") }
    var side by remember { mutableStateOf("buy") } // "buy" or "sell"
    var sizeText by remember { mutableStateOf("1") }
    var orderType by remember { mutableStateOf("market_order") } // "market_order", "limit_order"
    var limitPriceText by remember { mutableStateOf("") }
    var reduceOnly by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<OrderResponse?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Instrument search and filter state
    var instrumentSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // "ALL", "PERPETUALS", "CALLS", "PUTS"
    var selectedAssetFilter by remember { mutableStateOf("ALL") } // "ALL", "BTC", "ETH", "SOL", "XRP", "XAUT", "OTHERS"
    var showAllInstrumentsBrowser by remember { mutableStateOf(false) }

    val selectedProduct = products.find { it.id.toString() == selectedProductId } ?: products.firstOrNull()

    val filteredProducts = products.filter { prod ->
        val matchesQuery = instrumentSearchQuery.isBlank() ||
                prod.symbol.contains(instrumentSearchQuery.trim(), ignoreCase = true) ||
                prod.underlying_asset.contains(instrumentSearchQuery.trim(), ignoreCase = true) ||
                prod.description.contains(instrumentSearchQuery.trim(), ignoreCase = true)

        val matchesCategory = when (selectedCategoryFilter) {
            "PERPETUALS" -> prod.contract_type.contains("perpetual", ignoreCase = true) || prod.contract_type.contains("future", ignoreCase = true)
            "CALLS" -> prod.contract_type.contains("call", ignoreCase = true) || prod.symbol.endsWith("-C")
            "PUTS" -> prod.contract_type.contains("put", ignoreCase = true) || prod.symbol.endsWith("-P")
            else -> true
        }

        val matchesAsset = when (selectedAssetFilter) {
            "BTC" -> prod.underlying_asset.equals("BTC", ignoreCase = true) || prod.symbol.startsWith("BTC")
            "ETH" -> prod.underlying_asset.equals("ETH", ignoreCase = true) || prod.symbol.startsWith("ETH")
            "SOL" -> prod.underlying_asset.equals("SOL", ignoreCase = true) || prod.symbol.startsWith("SOL")
            "XRP" -> prod.underlying_asset.equals("XRP", ignoreCase = true) || prod.symbol.startsWith("XRP")
            "XAUT" -> prod.underlying_asset.equals("XAUT", ignoreCase = true) || prod.symbol.startsWith("XAUT")
            "OTHERS" -> !listOf("BTC", "ETH", "SOL", "XRP", "XAUT").contains(prod.underlying_asset.uppercase())
            else -> true
        }

        matchesQuery && matchesCategory && matchesAsset
    }

    val currentLots = sizeText.toIntOrNull() ?: 1
    val estimatedNotional = (selectedProduct?.mark_price ?: 0.0) * currentLots * (if (selectedProduct?.contract_type?.contains("option", ignoreCase = true) == true) 1.0 else 0.001)

    fun submit() {
        if (selectedProduct == null) {
            statusMessage = "Please select an instrument"
            return
        }
        val sizeInt = sizeText.toIntOrNull() ?: 1
        if (sizeInt <= 0) {
            statusMessage = "Size must be at least 1 lot"
            return
        }
        if (orderType == "limit_order" && limitPriceText.toDoubleOrNull() == null) {
            statusMessage = "Please enter a valid limit price"
            return
        }

        submitting = true
        statusMessage = null
        scope.launch {
            try {
                val req = OrderRequest(
                    product_id = selectedProduct.id,
                    symbol = selectedProduct.symbol,
                    size = sizeInt,
                    side = side,
                    order_type = orderType,
                    limit_price = if (orderType == "limit_order") limitPriceText else null,
                    reduce_only = reduceOnly
                )
                val resp = onPlaceOrder(req)
                lastResult = resp
                statusMessage = "Order filled successfully on Delta Exchange: ${resp.id}"
            } catch (e: Exception) {
                statusMessage = "Order failed: ${e.message}"
            } finally {
                submitting = false
            }
        }
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
                    text = "DELTA EXCHANGE INDIA · ALL INSTRUMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live Order Ticket",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Search and trade all Perpetuals & Options available on Delta Exchange India.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Broker warning banner
        if (!broker.configured) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarningYellow.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .clickable { onNavigateToSettings() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26200A)),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow)
                        Column {
                            Text(
                                text = "Broker Not Configured",
                                color = WarningYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Add Delta India API Key & Whitelist your IP in Settings to execute live orders.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Instrument Spotlight & Picker Toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SELECTED INSTRUMENT", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                            Text(
                                text = selectedProduct?.symbol ?: "Select Instrument",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("MARK PRICE", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                            Text(
                                text = String.format(Locale.US, "$%,.2f", selectedProduct?.mark_price ?: 0.0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = ProfitGreen
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(ElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = (selectedProduct?.contract_type ?: "perpetual").replace("_", " ").uppercase(Locale.US),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = ElectricBlue
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color.Black, RoundedCornerShape(2.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Tick: ${selectedProduct?.tick_size ?: 0.5}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Button(
                            onClick = { showAllInstrumentsBrowser = !showAllInstrumentsBrowser },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showAllInstrumentsBrowser) ElectricBlue else Color.Black
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue)),
                            modifier = Modifier.testTag("trade_browse_all_btn")
                        ) {
                            Icon(
                                imageVector = if (showAllInstrumentsBrowser) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (showAllInstrumentsBrowser) Color.White else ElectricBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAllInstrumentsBrowser) "Close Browser" else "Browse All Instruments (${products.size})",
                                fontSize = 11.sp,
                                color = if (showAllInstrumentsBrowser) Color.White else ElectricBlue
                            )
                        }
                    }
                }
            }
        }

        // All Instruments Explorer Section (Collapsible / Expandable)
        if (showAllInstrumentsBrowser) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricBlue, RoundedCornerShape(2.dp))
                        .testTag("all_instruments_browser"),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Text("DELTA INSTRUMENTS CATALOG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("${filteredProducts.size} / ${products.size} loaded", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                        }

                        // Search Input
                        OutlinedTextField(
                            value = instrumentSearchQuery,
                            onValueChange = { instrumentSearchQuery = it },
                            placeholder = { Text("Search by symbol, strike, coin (BTC, SOL, 68000, Put)...", color = TextDarkMuted, fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (instrumentSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { instrumentSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("instrument_search_input"),
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

                        // Category Tabs (ALL, PERPETUALS, CALLS, PUTS)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("ALL", "PERPETUALS", "CALLS", "PUTS").forEach { cat ->
                                val selected = selectedCategoryFilter == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) ElectricBlue else DarkSurface, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (selected) ElectricBlue else DarkBorder, RoundedCornerShape(2.dp))
                                        .clickable { selectedCategoryFilter = cat }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else TextMuted
                                    )
                                }
                            }
                        }

                        // Underlying Asset Quick Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(listOf("ALL", "BTC", "ETH", "SOL", "XRP", "XAUT", "OTHERS")) { asset ->
                                val selected = selectedAssetFilter == asset
                                Box(
                                    modifier = Modifier
                                        .background(if (selected) ElectricBlue.copy(alpha = 0.2f) else DarkSurface, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (selected) ElectricBlue else DarkBorder, RoundedCornerShape(2.dp))
                                        .clickable { selectedAssetFilter = asset }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = asset,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (selected) ElectricBlue else TextDarkMuted
                                    )
                                }
                            }
                        }

                        // Scrollable List of Filtered Instruments
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .background(DarkSurface, RoundedCornerShape(2.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                        ) {
                            if (filteredProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No instruments match the search filter", color = TextDarkMuted, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredProducts) { item ->
                                        val isCurrent = item.id.toString() == selectedProductId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isCurrent) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent,
                                                    RoundedCornerShape(2.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isCurrent) ElectricBlue.copy(alpha = 0.5f) else Color.Transparent,
                                                    RoundedCornerShape(2.dp)
                                                )
                                                .clickable {
                                                    selectedProductId = item.id.toString()
                                                    limitPriceText = item.mark_price.toString()
                                                    showAllInstrumentsBrowser = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("instrument_item_${item.symbol}"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(item.symbol, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    val isOption = item.contract_type.contains("option", ignoreCase = true)
                                                    val badgeColor = if (isOption) (if (item.symbol.endsWith("-C")) ProfitGreen else LossRed) else ElectricBlue
                                                    Text(
                                                        text = if (isOption) (if (item.symbol.endsWith("-C")) "CALL" else "PUT") else "PERP",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = badgeColor
                                                    )
                                                }
                                                Text(item.description, fontSize = 10.sp, color = TextDarkMuted, maxLines = 1)
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = String.format(Locale.US, "$%,.2f", item.mark_price),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfitGreen
                                                )
                                                Text(
                                                    text = "24h: $${String.format(Locale.US, "%,.0f", item.volume_24h)}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = TextDarkMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Order Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_form")
                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Side Buttons (Buy / Sell)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isBuy = side == "buy"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(if (isBuy) ProfitGreen.copy(alpha = 0.2f) else Color.Black, RoundedCornerShape(2.dp))
                                .border(1.dp, if (isBuy) ProfitGreen else DarkBorder, RoundedCornerShape(2.dp))
                                .clickable { side = "buy" }
                                .testTag("trade_side_buy_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BUY · LONG",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isBuy) ProfitGreen else TextMuted
                            )
                        }

                        val isSell = side == "sell"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(if (isSell) LossRed.copy(alpha = 0.2f) else Color.Black, RoundedCornerShape(2.dp))
                                .border(1.dp, if (isSell) LossRed else DarkBorder, RoundedCornerShape(2.dp))
                                .clickable { side = "sell" }
                                .testTag("trade_side_sell_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SELL · SHORT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSell) LossRed else TextMuted
                            )
                        }
                    }

                    // Size and Order Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Size (lots)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIZE (LOTS)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = sizeText,
                                onValueChange = { sizeText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trade_size_input"),
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

                        // Order Type
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ORDER TYPE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isMarket = orderType == "market_order"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .background(if (isMarket) ElectricBlue.copy(alpha = 0.2f) else Color.Black, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isMarket) ElectricBlue else DarkBorder, RoundedCornerShape(2.dp))
                                        .clickable { orderType = "market_order" }
                                        .testTag("trade_type_market"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Market", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (isMarket) ElectricBlue else TextMuted)
                                }

                                val isLimit = orderType == "limit_order"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .background(if (isLimit) ElectricBlue.copy(alpha = 0.2f) else Color.Black, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isLimit) ElectricBlue else DarkBorder, RoundedCornerShape(2.dp))
                                        .clickable {
                                            orderType = "limit_order"
                                            if (limitPriceText.isBlank()) {
                                                limitPriceText = (selectedProduct?.mark_price ?: 0.0).toString()
                                            }
                                        }
                                        .testTag("trade_type_limit"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Limit", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (isLimit) ElectricBlue else TextMuted)
                                }
                            }
                        }
                    }

                    // Quick Lot Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 5, 10, 25, 50).forEach { lot ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Black, RoundedCornerShape(2.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                    .clickable { sizeText = lot.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$lot", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }

                    // Limit Price (if Limit)
                    if (orderType == "limit_order") {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIMIT PRICE ($)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("Tick Size: ${selectedProduct?.tick_size ?: 0.5}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextDarkMuted)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = limitPriceText,
                                onValueChange = { limitPriceText = it },
                                placeholder = { Text("e.g. 67500.0", color = TextDarkMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trade_limit_price_input"),
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
                    }

                    // Notional & Margin Calculation Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(2.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ESTIMATED NOTIONAL", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                                Text(
                                    text = String.format(Locale.US, "$%,.2f USD", estimatedNotional),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ROUTE DESTINATION", style = MaterialTheme.typography.labelSmall, color = TextDarkMuted)
                                Text(
                                    text = "Delta India Live Engine",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    // Reduce-Only Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reduceOnly = !reduceOnly }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reduceOnly,
                            onCheckedChange = { reduceOnly = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ElectricBlue,
                                uncheckedColor = TextMuted,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("trade_reduce_only_toggle")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reduce-only (close existing exposure only)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    // Status Message
                    if (statusMessage != null) {
                        Text(
                            text = statusMessage ?: "",
                            color = if (statusMessage?.contains("failed", ignoreCase = true) == true) LossRed else ProfitGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = { submit() },
                        enabled = !submitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == "buy") ProfitGreen else LossRed,
                            contentColor = if (side == "buy") Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trade_submit_btn")
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting to Delta Exchange…", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = "SUBMIT ${side.uppercase(Locale.US)} ORDER (${selectedProduct?.symbol ?: "BTCUSD"})",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Last Submission Response Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_result_panel")
                    .border(1.dp, DarkBorder, RoundedCornerShape(2.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXCHANGE RESPONSE & EXECUTION LOG", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (lastResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(2.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(2.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = lastResult?.raw_json ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        Text("Place an order on any Delta Exchange instrument to view the live execution payload.", color = TextDarkMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
