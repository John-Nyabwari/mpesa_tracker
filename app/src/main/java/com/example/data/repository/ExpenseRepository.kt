package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.parser.ParsedTransactionResult
import com.example.data.parser.ParsingEngine
import com.example.data.parser.SampleMpesaSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class SmsIngestOutcome {
    PARSED_TRANSACTION,
    IGNORED_BALANCE_ONLY,
    IGNORED_SENDER,
    QUEUED_UNPARSED
}

class ExpenseRepository(
    private val db: AppDatabase,
    private val parsingEngine: ParsingEngine
) {
    fun getTransactions(startDate: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsFrom(startDate)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsBetween(start, end)

    fun getLatestBalanceTransaction(): Flow<TransactionEntity?> =
        db.transactionDao().getLatestBalanceTransaction()

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        db.categoryDao().getAllCategories()

    fun getKeywordRules(): Flow<List<CustomKeywordRuleEntity>> =
        db.ruleDao().getAllKeywordRules()

    fun getMerchantRules(): Flow<List<MerchantCategoryRuleEntity>> =
        db.ruleDao().getAllMerchantRules()

    fun getPendingUnparsed(): Flow<List<UnparsedSmsEntity>> =
        db.unparsedSmsDao().getPendingUnparsed()

    fun getUserPreferences(): Flow<UserPreferencesEntity?> =
        db.preferencesDao().getPreferences()

    suspend fun ensureDefaultsInitialized() = withContext(Dispatchers.IO) {
        val currentCats = db.categoryDao().getAllCategoriesDirect()
        if (currentCats.isEmpty()) {
            db.categoryDao().insertAll(AppDatabase.DEFAULT_CATEGORIES)
        }
        val prefs = db.preferencesDao().getPreferencesDirect()
        if (prefs == null) {
            db.preferencesDao().savePreferences(UserPreferencesEntity())
        }
    }

    suspend fun savePreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        db.preferencesDao().savePreferences(prefs)
    }

    suspend fun processIncomingSms(
        sender: String,
        body: String,
        timestamp: Long
    ): SmsIngestOutcome = withContext(Dispatchers.IO) {
        ensureDefaultsInitialized()
        val prefs = db.preferencesDao().getPreferencesDirect() ?: UserPreferencesEntity()
        val allowedSenders = prefs.allowedSenderIds
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
        val normalizedSender = sender.trim().uppercase()

        if (allowedSenders.isNotEmpty() && !allowedSenders.contains(normalizedSender)) {
            return@withContext SmsIngestOutcome.IGNORED_SENDER
        }

        if (parsingEngine.isBalanceOnly(body)) {
            return@withContext SmsIngestOutcome.IGNORED_BALANCE_ONLY
        }

        val customRules = db.ruleDao().getAllKeywordRulesDirect()
        val parsed = parsingEngine.parse(body, timestamp, customRules)

        if (parsed != null) {
            insertParsedTransaction(parsed)
            return@withContext SmsIngestOutcome.PARSED_TRANSACTION
        } else {
            db.unparsedSmsDao().insert(
                UnparsedSmsEntity(
                    sender = sender,
                    body = body,
                    smsBodyHash = body.hashCode(),
                    receivedTimestamp = timestamp
                )
            )
            return@withContext SmsIngestOutcome.QUEUED_UNPARSED
        }
    }

    suspend fun insertParsedTransaction(parsed: ParsedTransactionResult) = withContext(Dispatchers.IO) {
        val merchantRule = db.ruleDao().getRuleForMerchant(parsed.counterparty)
        val finalCategory = merchantRule?.categoryName ?: parsed.category

        val mainEntity = TransactionEntity(
            mpesaCode = parsed.mpesaCode,
            direction = parsed.direction,
            amount = parsed.amount,
            counterparty = parsed.counterparty,
            accountOrTill = parsed.accountOrTill,
            category = finalCategory,
            newBalance = parsed.newBalance,
            transactionCost = parsed.transactionCost,
            timestamp = parsed.timestamp,
            rawSmsBody = parsed.rawSms,
            isCostLineItem = false,
            subtypeName = parsed.subtypeName
        )
        db.transactionDao().insert(mainEntity)

        if (parsed.transactionCost > 0.0) {
            val costEntity = TransactionEntity(
                mpesaCode = "${parsed.mpesaCode}_COST",
                direction = TransactionDirection.EXPENSE,
                amount = parsed.transactionCost,
                counterparty = "Safaricom Fee (${parsed.counterparty})",
                category = "Fees & Charges",
                newBalance = null,
                transactionCost = 0.0,
                timestamp = parsed.timestamp + 1L,
                rawSmsBody = "Transaction fee for ${parsed.mpesaCode}",
                isCostLineItem = true,
                parentMpesaCode = parsed.mpesaCode,
                subtypeName = "Transaction Cost"
            )
            db.transactionDao().insert(costEntity)
        }
    }

    suspend fun updateTransactionCategory(
        transactionId: Long,
        counterparty: String,
        newCategory: String,
        rememberForMerchant: Boolean
    ) = withContext(Dispatchers.IO) {
        db.transactionDao().updateCategory(transactionId, newCategory)
        if (rememberForMerchant && counterparty.isNotBlank()) {
            db.ruleDao().saveMerchantRule(
                MerchantCategoryRuleEntity(
                    merchantIdentifier = counterparty,
                    categoryName = newCategory
                )
            )
            db.transactionDao().updateCategoryForMerchant(counterparty, newCategory)
        }
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        db.transactionDao().deleteById(id)
    }

    suspend fun addKeywordRule(rule: CustomKeywordRuleEntity): Int = withContext(Dispatchers.IO) {
        db.ruleDao().insertKeywordRule(rule)
        val pending = db.unparsedSmsDao().getPendingUnparsedDirect()
        val customRules = db.ruleDao().getAllKeywordRulesDirect()
        var resolvedCount = 0
        for (item in pending) {
            val parsed = parsingEngine.parse(item.body, item.receivedTimestamp, customRules)
            if (parsed != null) {
                insertParsedTransaction(parsed)
                db.unparsedSmsDao().markResolved(item.id)
                resolvedCount++
            }
        }
        resolvedCount
    }

    suspend fun deleteKeywordRule(rule: CustomKeywordRuleEntity) = withContext(Dispatchers.IO) {
        db.ruleDao().deleteKeywordRule(rule)
    }

    suspend fun deleteMerchantRule(rule: MerchantCategoryRuleEntity) = withContext(Dispatchers.IO) {
        db.ruleDao().deleteMerchantRule(rule)
    }

    suspend fun dismissUnparsedSms(item: UnparsedSmsEntity) = withContext(Dispatchers.IO) {
        db.unparsedSmsDao().delete(item)
    }

    suspend fun addCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategory(category)
    }

    suspend fun updateCategory(oldName: String, newName: String, colorHex: String) = withContext(Dispatchers.IO) {
        db.categoryDao().renameCategory(oldName, newName, colorHex)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategory(category)
    }

    suspend fun runHistoricalScan(contentResolver: ContentResolver, trackingStartDate: Long): Int =
        withContext(Dispatchers.IO) {
            ensureDefaultsInitialized()
            val prefs = db.preferencesDao().getPreferencesDirect() ?: UserPreferencesEntity()
            val allowedSenders = prefs.allowedSenderIds
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
            val customRules = db.ruleDao().getAllKeywordRulesDirect()

            if (trackingStartDate > 0L) {
                db.transactionDao().pruneBefore(trackingStartDate)
            }

            val cursor = try {
                contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("_id", "address", "body", "date"),
                    "date >= ?",
                    arrayOf(trackingStartDate.toString()),
                    "date ASC"
                )
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            } ?: return@withContext 0

            var parsedCount = 0
            cursor.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                if (addrIdx < 0 || bodyIdx < 0 || dateIdx < 0) return@use

                while (it.moveToNext()) {
                    val address = it.getString(addrIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    if (allowedSenders.contains(address.trim().uppercase())) {
                        if (!parsingEngine.isBalanceOnly(body)) {
                            val parsed = parsingEngine.parse(body, date, customRules)
                            if (parsed != null) {
                                insertParsedTransaction(parsed)
                                parsedCount++
                            } else {
                                db.unparsedSmsDao().insert(
                                    UnparsedSmsEntity(
                                        sender = address,
                                        body = body,
                                        smsBodyHash = body.hashCode(),
                                        receivedTimestamp = date
                                    )
                                )
                            }
                        }
                    }
                }
            }
            return@withContext parsedCount
        }

    suspend fun ingestAllSampleTemplates(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val now = System.currentTimeMillis()
        SampleMpesaSms.templates.forEachIndexed { idx, template ->
            val outcome = processIncomingSms(
                sender = "MPESA",
                body = template.smsBody,
                timestamp = now - (SampleMpesaSms.templates.size - idx) * 3600_000L
            )
            if (outcome == SmsIngestOutcome.PARSED_TRANSACTION) {
                count++
            }
        }
        count
    }
}
