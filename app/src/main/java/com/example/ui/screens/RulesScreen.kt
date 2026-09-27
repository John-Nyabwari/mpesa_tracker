package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CustomKeywordRuleEntity
import com.example.data.local.entities.MerchantCategoryRuleEntity
import com.example.data.local.entities.TransactionDirection
import com.example.data.local.entities.UnparsedSmsEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesAndReviewScreen(
    rules: List<CustomKeywordRuleEntity>,
    merchantRules: List<MerchantCategoryRuleEntity>,
    unparsed: List<UnparsedSmsEntity>,
    statusMessage: String?,
    onClearStatus: () -> Unit,
    onBack: () -> Unit,
    onAddRule: (keyword: String, dir: TransactionDirection, cat: String, regex: String?) -> Unit,
    onDeleteRule: (CustomKeywordRuleEntity) -> Unit,
    onDeleteMerchantRule: (MerchantCategoryRuleEntity) -> Unit,
    onDismissUnparsed: (UnparsedSmsEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillKeyword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Rules & Unparsed Queue", fontWeight = FontWeight.Bold, color = DarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("rules_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            prefillKeyword = ""
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_keyword_rule_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = DarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (statusMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(statusMessage, color = BrandLime, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = onClearStatus) {
                                Text("OK", color = SurfaceWhite)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Custom Keyword Rules (${rules.size})",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                    TextButton(
                        onClick = {
                            prefillKeyword = ""
                            showAddDialog = true
                        }
                    ) {
                        Text("+ New Rule", color = DarkGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (rules.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            "No custom keyword rules configured yet. Add a keyword rule to parse custom M-Pesa messages (such as GlobalPay Visa or custom PayBill alerts) and retroactively resolve queued SMS.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Keyword: \"${rule.keywordPattern}\"",
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreen
                                )
                                Text(
                                    "Direction: ${rule.direction} • Category: ${rule.defaultCategory}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                if (!rule.customAmountRegex.isNullOrBlank()) {
                                    Text(
                                        "Regex: ${rule.customAmountRegex}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteRule(rule) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = ExpenseRed)
                            }
                        }
                    }
                }
            }

            if (merchantRules.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Remembered Merchant Mappings (${merchantRules.size})",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                }

                items(merchantRules, key = { it.id }) { mRule ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mRule.merchantIdentifier, fontWeight = FontWeight.SemiBold, color = DarkGreen, fontSize = 13.sp)
                                Text("Auto-assigns to: ${mRule.categoryName}", fontSize = 12.sp, color = BrandGreen)
                            }
                            IconButton(onClick = { onDeleteMerchantRule(mRule) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Merchant Rule", tint = ExpenseRed)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Unparsed SMS Review Queue (${unparsed.size})",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    fontSize = 15.sp
                )
                Text(
                    "M-Pesa messages that did not match built-in patterns are safely held here. Create a keyword rule to parse them automatically:",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            if (unparsed.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            "Queue is empty — all incoming M-Pesa messages have been parsed.",
                            fontSize = 12.sp,
                            color = IncomeGreen,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(unparsed, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Sender: ${item.sender}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreen
                                )
                                TextButton(onClick = { onDismissUnparsed(item) }) {
                                    Text("Dismiss", fontSize = 11.sp, color = ExpenseRed)
                                }
                            }
                            Text(
                                text = item.body,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Button(
                                onClick = {
                                    prefillKeyword = if (item.body.contains("GlobalPay", ignoreCase = true)) {
                                        "GlobalPay"
                                    } else {
                                        item.body.split(" ").firstOrNull { it.length > 4 } ?: ""
                                    }
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                                modifier = Modifier.testTag("create_rule_from_unparsed_${item.id}")
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandLime)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Rule to Parse This SMS", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            initialKeyword = prefillKeyword,
            onDismiss = { showAddDialog = false },
            onConfirm = { kw, dir, cat, rgx ->
                onAddRule(kw, dir, cat, rgx)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddRuleDialog(
    initialKeyword: String = "",
    onDismiss: () -> Unit,
    onConfirm: (keyword: String, dir: TransactionDirection, cat: String, regex: String?) -> Unit
) {
    var keyword by remember(initialKeyword) { mutableStateOf(initialKeyword) }
    var direction by remember { mutableStateOf(TransactionDirection.EXPENSE) }
    var category by remember { mutableStateOf("Shopping") }
    var regex by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Keyword Parsing Rule", fontWeight = FontWeight.Bold, color = DarkGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Trigger Keyword / Phrase (e.g. GlobalPay)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_keyword_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == TransactionDirection.EXPENSE,
                        onClick = { direction = TransactionDirection.EXPENSE },
                        label = { Text("Expense (-)") }
                    )
                    FilterChip(
                        selected = direction == TransactionDirection.INCOME,
                        onClick = { direction = TransactionDirection.INCOME },
                        label = { Text("Income (+)") }
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Assign Category") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_category_input")
                )
                OutlinedTextField(
                    value = regex,
                    onValueChange = { regex = it },
                    label = { Text("Custom Amount Regex (Optional)") },
                    supportingText = { Text("Leave blank to auto-extract Ksh/KES amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyword.isNotBlank()) onConfirm(keyword, direction, category, regex) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                modifier = Modifier.testTag("confirm_add_rule_button")
            ) {
                Text("Save & Apply Retroactively")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
