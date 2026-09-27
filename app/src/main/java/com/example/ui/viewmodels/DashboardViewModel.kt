package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.TransactionDirection
import com.example.data.local.entities.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SmsIngestOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilterPeriod {
    ALL_TIME,
    THIS_MONTH,
    LAST_30_DAYS,
    THIS_WEEK
}

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val latestMpesaBalance: Double? = null,
    val totalFulizaAndCost: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.ALL_TIME,
    val selectedCategory: String? = null,
    val selectedDirection: TransactionDirection? = null,
    val searchQuery: String = "",
    val currencySymbol: String = "KSh",
    val displayName: String = "M-Pesa User",
    val statusBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: ExpenseRepository,
    private val appContext: Context
) : ViewModel() {

    private val _period = MutableStateFlow(DateFilterPeriod.ALL_TIME)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedDirection = MutableStateFlow<TransactionDirection?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _statusBanner = MutableStateFlow<String?>(null)

    private data class FilterConfig(
        val period: DateFilterPeriod,
        val category: String?,
        val direction: TransactionDirection?,
        val searchQuery: String,
        val statusMessage: String?
    )

    private val filterFlow = combine(
        _period,
        _selectedCategory,
        _selectedDirection,
        _searchQuery,
        _statusBanner
    ) { period, cat, dir, search, status ->
        FilterConfig(period, cat, dir, search, status)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getUserPreferences(),
        filterFlow,
        repository.getLatestBalanceTransaction()
    ) { prefs, filter, latestBalTx ->
        val trackingStart = prefs?.trackingStartDate ?: 0L
        val currency = prefs?.currencySymbol ?: "KSh"
        val displayName = prefs?.displayName ?: "M-Pesa User"
        val bounds = calculatePeriodBounds(filter.period, trackingStart)

        DashboardQueryState(
            bounds = bounds,
            filter = filter,
            currency = currency,
            displayName = displayName,
            latestBal = latestBalTx?.newBalance
        )
    }.flatMapLatest { query ->
        repository.getTransactionsBetween(query.bounds.first, query.bounds.second).map { txList ->
            val q = query.filter.searchQuery.trim()
            val filtered = txList.filter { tx ->
                val matchesCategory = query.filter.category == null || tx.category == query.filter.category
                val matchesDirection = query.filter.direction == null || tx.direction == query.filter.direction
                val matchesSearch = q.isEmpty() ||
                    tx.counterparty.contains(q, ignoreCase = true) ||
                    tx.mpesaCode.contains(q, ignoreCase = true) ||
                    tx.category.contains(q, ignoreCase = true) ||
                    (tx.accountOrTill?.contains(q, ignoreCase = true) == true)
                matchesCategory && matchesDirection && matchesSearch
            }

            val income = filtered.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amount }
            val expense = filtered.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amount }
            val fulizaAndCost = filtered.filter {
                it.category == "Fees & Charges" || it.isCostLineItem || it.counterparty.contains("Fuliza", ignoreCase = true)
            }.sumOf { it.amount }

            val breakdown = filtered.filter { it.direction == TransactionDirection.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            DashboardUiState(
                transactions = filtered,
                totalIncome = income,
                totalExpense = expense,
                netBalance = income - expense,
                latestMpesaBalance = query.latestBal,
                totalFulizaAndCost = fulizaAndCost,
                categoryBreakdown = breakdown,
                selectedPeriod = query.filter.period,
                selectedCategory = query.filter.category,
                selectedDirection = query.filter.direction,
                searchQuery = query.filter.searchQuery,
                currencySymbol = query.currency,
                displayName = query.displayName,
                statusBannerMessage = query.filter.statusMessage
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun setPeriod(period: DateFilterPeriod) {
        _period.value = period
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setDirectionFilter(direction: TransactionDirection?) {
        _selectedDirection.value = direction
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateTransactionCategory(tx: TransactionEntity, newCategory: String, rememberMerchant: Boolean) {
        viewModelScope.launch {
            repository.updateTransactionCategory(tx.id, tx.counterparty, newCategory, rememberMerchant)
            _statusBanner.value = if (rememberMerchant) {
                "Updated to '$newCategory' & remembered for ${tx.counterparty}"
            } else {
                "Transaction updated to '$newCategory'"
            }
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            _statusBanner.value = "Removed transaction ${tx.mpesaCode}"
        }
    }

    fun scanDeviceInboxNow() {
        viewModelScope.launch {
            val prefs = repository.getUserPreferences().firstOrNull()
            val startDate = prefs?.trackingStartDate ?: 0L
            val count = repository.runHistoricalScan(appContext.contentResolver, startDate)
            _statusBanner.value = if (count > 0) {
                "Scanned SMS inbox: $count M-Pesa transactions imported"
            } else {
                "Inbox scan complete (0 new M-Pesa SMS found in device inbox)"
            }
        }
    }

    fun ingestSingleSms(sender: String, body: String) {
        viewModelScope.launch {
            val outcome = repository.processIncomingSms(sender, body, System.currentTimeMillis())
            _statusBanner.value = when (outcome) {
                SmsIngestOutcome.PARSED_TRANSACTION -> "SMS parsed and added to ledger"
                SmsIngestOutcome.IGNORED_BALANCE_ONLY -> "Balance-only statement detected & ignored (no money movement)"
                SmsIngestOutcome.IGNORED_SENDER -> "Ignored: Sender '$sender' is not in allowed sender IDs"
                SmsIngestOutcome.QUEUED_UNPARSED -> "Unrecognized format: Added to Unparsed SMS Review Queue"
            }
        }
    }

    fun ingestAllSampleSubtypes() {
        viewModelScope.launch {
            val parsedCount = repository.ingestAllSampleTemplates()
            _statusBanner.value = "Processed 13 SMS templates: $parsedCount parsed, 1 balance-only ignored, 1 queued for rule review"
        }
    }

    private data class DashboardQueryState(
        val bounds: Pair<Long, Long>,
        val filter: FilterConfig,
        val currency: String,
        val displayName: String,
        val latestBal: Double?
    )

    private fun calculatePeriodBounds(period: DateFilterPeriod, trackingStart: Long): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val start = when (period) {
            DateFilterPeriod.ALL_TIME -> trackingStart
            DateFilterPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis.coerceAtLeast(trackingStart)
            }
            DateFilterPeriod.LAST_30_DAYS -> (now - 30L * 24 * 3600 * 1000).coerceAtLeast(trackingStart)
            DateFilterPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis.coerceAtLeast(trackingStart)
            }
        }
        return Pair(start, now + 86_400_000L * 365L)
    }
}
