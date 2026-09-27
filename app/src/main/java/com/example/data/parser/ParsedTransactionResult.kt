package com.example.data.parser

import com.example.data.local.entities.TransactionDirection

data class ParsedTransactionResult(
    val mpesaCode: String,
    val direction: TransactionDirection,
    val amount: Double,
    val counterparty: String,
    val accountOrTill: String? = null,
    val category: String,
    val newBalance: Double? = null,
    val transactionCost: Double = 0.0,
    val timestamp: Long,
    val rawSms: String,
    val subtypeName: String = "Standard",
    val isBalanceOnly: Boolean = false
)
