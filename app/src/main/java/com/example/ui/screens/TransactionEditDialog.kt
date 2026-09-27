package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.TransactionEntity
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TransactionEditDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSaveCategory: (newCategory: String, rememberForMerchant: Boolean) -> Unit,
    onDeleteTransaction: () -> Unit
) {
    var selectedCat by remember(transaction) { mutableStateOf(transaction.category) }
    var rememberForMerchant by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGreen)
                IconButton(
                    onClick = onDeleteTransaction,
                    modifier = Modifier.testTag("delete_transaction_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction", tint = ExpenseRed)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = SurfaceBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.counterparty,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkGreen
                        )
                        Text(
                            text = "Code: ${transaction.mpesaCode} • Subtype: ${transaction.subtypeName}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Amount: $currency ${String.format(Locale.US, "%,.2f", transaction.amount)} | Fee: $currency ${String.format(Locale.US, "%.2f", transaction.transactionCost)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        if (transaction.newBalance != null) {
                            Text(
                                text = "Balance After: $currency ${String.format(Locale.US, "%,.2f", transaction.newBalance)}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Text("Raw SMS Receipt:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = transaction.rawSmsBody,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DarkGreen
                    )
                }

                Text("Assign Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.name }) { cat ->
                        FilterChip(
                            selected = cat.name == selectedCat,
                            onClick = { selectedCat = cat.name },
                            label = { Text(cat.name, fontSize = 11.sp) },
                            modifier = Modifier.testTag("select_category_chip_${cat.name}")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberForMerchant,
                        onCheckedChange = { rememberForMerchant = it },
                        modifier = Modifier.testTag("remember_merchant_checkbox")
                    )
                    Text(
                        text = "Always assign '${transaction.counterparty}' to $selectedCat",
                        fontSize = 12.sp,
                        color = DarkGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveCategory(selectedCat, rememberForMerchant) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                modifier = Modifier.testTag("save_transaction_category_button")
            ) {
                Text("Save Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
