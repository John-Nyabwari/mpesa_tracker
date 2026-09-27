package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionDirection {
    INCOME,
    EXPENSE
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["mpesaCode"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mpesaCode: String,
    val direction: TransactionDirection,
    val amount: Double,
    val counterparty: String,
    val accountOrTill: String? = null,
    val category: String,
    val newBalance: Double? = null,
    val transactionCost: Double = 0.0,
    val timestamp: Long,
    val rawSmsBody: String,
    val isCostLineItem: Boolean = false,
    val parentMpesaCode: String? = null,
    val subtypeName: String = "Standard"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "merchant_category_rules",
    indices = [Index(value = ["merchantIdentifier"], unique = true)]
)
data class MerchantCategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantIdentifier: String,
    val categoryName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_keyword_rules")
data class CustomKeywordRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keywordPattern: String,
    val direction: TransactionDirection,
    val defaultCategory: String,
    val customAmountRegex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "unparsed_sms",
    indices = [Index(value = ["smsBodyHash"], unique = true)]
)
data class UnparsedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val smsBodyHash: Int,
    val receivedTimestamp: Long,
    val status: String = "PENDING"
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "M-Pesa User",
    val currencySymbol: String = "KSh",
    val trackingStartDate: Long = 0L,
    val allowedSenderIds: String = "MPESA,M-PESA,SAFARICOM",
    val defaultPeriod: String = "THIS_MONTH"
)
