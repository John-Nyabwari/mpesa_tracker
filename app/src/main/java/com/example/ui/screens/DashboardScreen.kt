package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.TransactionDirection
import com.example.data.local.entities.TransactionEntity
import com.example.data.parser.SampleMpesaSms
import com.example.ui.theme.*
import com.example.ui.viewmodels.DashboardUiState
import com.example.ui.viewmodels.DateFilterPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    unparsedCount: Int,
    onPeriodSelected: (DateFilterPeriod) -> Unit,
    onCategoryFilterSelected: (String?) -> Unit,
    onDirectionFilterSelected: (TransactionDirection?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onScanInboxNow: () -> Unit,
    onIngestSingleSms: (sender: String, body: String) -> Unit,
    onIngestAllSampleSubtypes: () -> Unit,
    onDismissBanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    var showSmsWorkbench by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = DarkGreen
                        )
                        Text(
                            text = state.displayName,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSmsWorkbench = true },
                        modifier = Modifier.testTag("open_sms_workbench_button")
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = stringResource(R.string.test_sms_nav),
                            tint = DarkGreen
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCategories,
                        modifier = Modifier.testTag("nav_categories_button")
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = stringResource(R.string.categories_nav),
                            tint = DarkGreen
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (unparsedCount > 0) {
                                Badge(
                                    containerColor = ExpenseRed,
                                    contentColor = SurfaceWhite
                                ) {
                                    Text(unparsedCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNavigateToRules,
                            modifier = Modifier.testTag("nav_rules_button")
                        ) {
                            Icon(
                                Icons.Default.Rule,
                                contentDescription = stringResource(R.string.rules_nav),
                                tint = DarkGreen
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_nav),
                            tint = DarkGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSmsWorkbench = true },
                containerColor = DarkGreen,
                contentColor = BrandLime,
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("Paste / Test SMS", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("fab_paste_sms")
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                item {
                    AnimatedVisibility(visible = state.statusBannerMessage != null) {
                        state.statusBannerMessage?.let { msg ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        color = BrandLime,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissBanner,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = SurfaceWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    PeriodFilterRow(
                        selected = state.selectedPeriod,
                        onSelected = onPeriodSelected
                    )
                }

                item {
                    SummaryCard(state = state)
                }

                if (state.categoryBreakdown.isNotEmpty()) {
                    item {
                        CategoryBreakdownCard(
                            breakdown = state.categoryBreakdown,
                            selectedCategory = state.selectedCategory,
                            currency = state.currencySymbol,
                            onCategoryClick = { clickedCat ->
                                if (state.selectedCategory == clickedCat) {
                                    onCategoryFilterSelected(null)
                                } else {
                                    onCategoryFilterSelected(clickedCat)
                                }
                            }
                        )
                    }
                }

                item {
                    FilterBar(
                        searchQuery = state.searchQuery,
                        selectedCategory = state.selectedCategory,
                        selectedDirection = state.selectedDirection,
                        onSearchChange = onSearchQueryChange,
                        onClearCategory = { onCategoryFilterSelected(null) },
                        onDirectionSelect = onDirectionFilterSelected
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.transactions_header)} (${state.transactions.size})",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            fontSize = 16.sp
                        )
                        TextButton(
                            onClick = onScanInboxNow,
                            modifier = Modifier.testTag("scan_inbox_header_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DarkGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Inbox", fontSize = 12.sp, color = DarkGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (state.transactions.isEmpty()) {
                    item {
                        EmptyLedgerCard(
                            onScanInbox = onScanInboxNow,
                            onLoadSamples = onIngestAllSampleSubtypes
                        )
                    }
                } else {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionListItem(
                            tx = tx,
                            currency = state.currencySymbol,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }

    if (showSmsWorkbench) {
        SmsWorkbenchDialog(
            onDismiss = { showSmsWorkbench = false },
            onIngestSingle = { sender, body ->
                onIngestSingleSms(sender, body)
                showSmsWorkbench = false
            },
            onIngestAllTemplates = {
                onIngestAllSampleSubtypes()
                showSmsWorkbench = false
            }
        )
    }
}

@Composable
fun SummaryCard(state: DashboardUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.net_period_balance),
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val prefix = if (state.netBalance > 0) "+" else ""
                    Text(
                        text = "$prefix${state.currencySymbol} ${String.format(Locale.US, "%,.2f", state.netBalance)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.netBalance >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.testTag("net_balance_value")
                    )
                }

                if (state.latestMpesaBalance != null) {
                    Surface(
                        color = SurfaceBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.latest_mpesa_balance),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "${state.currencySymbol} ${String.format(Locale.US, "%,.2f", state.latestMpesaBalance)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen,
                                modifier = Modifier.testTag("latest_mpesa_balance_value")
                            )
                            Text(
                                text = stringResource(R.string.from_latest_sms),
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.income_label), fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "+${state.currencySymbol} ${String.format(Locale.US, "%,.2f", state.totalIncome)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IncomeGreen,
                        modifier = Modifier.testTag("total_income_value")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.expenses_label), fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "-${state.currencySymbol} ${String.format(Locale.US, "%,.2f", state.totalExpense)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ExpenseRed,
                        modifier = Modifier.testTag("total_expense_value")
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(stringResource(R.string.fees_and_fuliza_label), fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "${state.currencySymbol} ${String.format(Locale.US, "%,.2f", state.totalFulizaAndCost)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkGreen,
                        modifier = Modifier.testTag("total_fees_value")
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    breakdown: Map<String, Double>,
    selectedCategory: String?,
    currency: String,
    onCategoryClick: (String) -> Unit
) {
    val totalExpense = breakdown.values.sum().coerceAtLeast(1.0)
    val sorted = breakdown.toList().sortedByDescending { it.second }.take(6)
    val palette = listOf(
        DarkGreen,
        BrandGreen,
        ForestTeal,
        OliveChart,
        BronzeChart,
        TextMuted
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.top_expense_categories),
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    fontSize = 14.sp
                )
                Text(
                    text = "Tap row to filter",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                var startX = 0f
                sorted.forEachIndexed { i, entry ->
                    val ratio = (entry.second.toFloat() / totalExpense.toFloat()).coerceIn(0f, 1f)
                    val segWidth = ratio * size.width
                    drawRoundRect(
                        color = palette[i % palette.size],
                        topLeft = Offset(startX, 0f),
                        size = Size(segWidth, size.height),
                        cornerRadius = CornerRadius(0f, 0f)
                    )
                    startX += segWidth
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            sorted.forEachIndexed { idx, (cat, amount) ->
                val pct = (amount / totalExpense) * 100.0
                val isSelected = selectedCategory == cat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceBg else Color.Transparent)
                        .clickable { onCategoryClick(cat) }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(palette[idx % palette.size], shape = CircleShape)
                        )
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = DarkGreen
                        )
                    }
                    Text(
                        text = "$currency ${String.format(Locale.US, "%,.2f", amount)} (${String.format(Locale.US, "%.1f", pct)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodFilterRow(
    selected: DateFilterPeriod,
    onSelected: (DateFilterPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateFilterPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) DarkGreen else SurfaceWhite)
                    .clickable { onSelected(period) }
                    .padding(vertical = 8.dp)
                    .testTag("period_filter_${period.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (period) {
                        DateFilterPeriod.ALL_TIME -> "All Time"
                        DateFilterPeriod.THIS_MONTH -> "Month"
                        DateFilterPeriod.LAST_30_DAYS -> "30 Days"
                        DateFilterPeriod.THIS_WEEK -> "Week"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) BrandLime else DarkGreen
                )
            }
        }
    }
}

@Composable
fun FilterBar(
    searchQuery: String,
    selectedCategory: String?,
    selectedDirection: TransactionDirection?,
    onSearchChange: (String) -> Unit,
    onClearCategory: () -> Unit,
    onDirectionSelect: (TransactionDirection?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search merchant, M-Pesa code, or account...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = DarkGreen,
                unfocusedBorderColor = BorderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                FilterChip(
                    selected = true,
                    onClick = onClearCategory,
                    label = { Text("Category: $selectedCategory") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear category filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            FilterChip(
                selected = selectedDirection == null,
                onClick = { onDirectionSelect(null) },
                label = { Text("All Flow") },
                modifier = Modifier.testTag("direction_filter_all")
            )
            FilterChip(
                selected = selectedDirection == TransactionDirection.INCOME,
                onClick = { onDirectionSelect(TransactionDirection.INCOME) },
                label = { Text("Income (+)") },
                modifier = Modifier.testTag("direction_filter_income")
            )
            FilterChip(
                selected = selectedDirection == TransactionDirection.EXPENSE,
                onClick = { onDirectionSelect(TransactionDirection.EXPENSE) },
                label = { Text("Expense (-)") },
                modifier = Modifier.testTag("direction_filter_expense")
            )
        }
    }
}

@Composable
fun TransactionListItem(
    tx: TransactionEntity,
    currency: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val isIncome = tx.direction == TransactionDirection.INCOME

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${tx.mpesaCode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isIncome) IncomeGreenBg else ExpenseRedBg,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        tx.isCostLineItem -> Icons.Default.Receipt
                        isIncome -> Icons.Default.CallReceived
                        else -> Icons.Default.CallMade
                    },
                    contentDescription = null,
                    tint = if (isIncome) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.counterparty,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SurfaceBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tx.category,
                            fontSize = 10.sp,
                            color = DarkGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = tx.mpesaCode,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    if (!tx.accountOrTill.isNullOrBlank()) {
                        Text(
                            text = "• Acc: ${tx.accountOrTill}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(tx.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+" else "-") +
                        "$currency ${String.format(Locale.US, "%,.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                if (tx.transactionCost > 0.0) {
                    Text(
                        text = "Fee: $currency ${String.format(Locale.US, "%.2f", tx.transactionCost)}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                } else if (tx.isCostLineItem) {
                    Text(
                        text = "M-Pesa Fee",
                        fontSize = 10.sp,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLedgerCard(
    onScanInbox: () -> Unit,
    onLoadSamples: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_ledger_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_mpesa_hero),
                contentDescription = "M-Pesa Ledger Illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = stringResource(R.string.empty_transactions_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkGreen
            )
            Text(
                text = stringResource(R.string.empty_transactions_subtitle),
                fontSize = 12.sp,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onScanInbox,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_scan_inbox_button")
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_inbox_now), fontSize = 12.sp)
                }

                Button(
                    onClick = onLoadSamples,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_12_subtypes_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandLime, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load 12 Subtypes", fontSize = 12.sp, color = SurfaceWhite)
                }
            }
        }
    }
}

@Composable
fun SmsWorkbenchDialog(
    onDismiss: () -> Unit,
    onIngestSingle: (sender: String, body: String) -> Unit,
    onIngestAllTemplates: () -> Unit
) {
    var sender by remember { mutableStateOf("MPESA") }
    var smsBody by remember { mutableStateOf(SampleMpesaSms.templates.first().smsBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "M-Pesa SMS Parser Workbench",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DarkGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Paste any Safaricom M-Pesa confirmation text or select a subtype template below to test the live ingestion pipeline:",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SampleMpesaSms.templates.forEachIndexed { idx, tpl ->
                        AssistChip(
                            onClick = { smsBody = tpl.smsBody },
                            label = { Text(tpl.title, fontSize = 11.sp) },
                            modifier = Modifier.testTag("template_chip_$idx")
                        )
                    }
                }

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender Address") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workbench_sender_input")
                )

                OutlinedTextField(
                    value = smsBody,
                    onValueChange = { smsBody = it },
                    label = { Text("Raw M-Pesa SMS Body") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workbench_body_input")
                )

                OutlinedButton(
                    onClick = onIngestAllTemplates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workbench_ingest_all_button")
                ) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run All 12 M-Pesa Subtypes Batch")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (smsBody.isNotBlank()) {
                        onIngestSingle(sender.trim(), smsBody.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                modifier = Modifier.testTag("workbench_parse_button")
            ) {
                Text("Parse & Ingest SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
