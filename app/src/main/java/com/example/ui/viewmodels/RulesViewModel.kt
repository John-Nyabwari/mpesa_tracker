package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.CustomKeywordRuleEntity
import com.example.data.local.entities.MerchantCategoryRuleEntity
import com.example.data.local.entities.TransactionDirection
import com.example.data.local.entities.UnparsedSmsEntity
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _ruleStatusMessage = MutableStateFlow<String?>(null)
    val ruleStatusMessage: StateFlow<String?> = _ruleStatusMessage.asStateFlow()

    val keywordRules = repository.getKeywordRules().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val merchantRules = repository.getMerchantRules().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val pendingUnparsed = repository.getPendingUnparsed().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun clearStatusMessage() {
        _ruleStatusMessage.value = null
    }

    fun addRule(
        keyword: String,
        direction: TransactionDirection,
        category: String,
        amountRegex: String?
    ) {
        viewModelScope.launch {
            val rule = CustomKeywordRuleEntity(
                keywordPattern = keyword.trim(),
                direction = direction,
                defaultCategory = category.trim().ifEmpty { "Shopping" },
                customAmountRegex = amountRegex?.takeIf { it.isNotBlank() }
            )
            val resolved = repository.addKeywordRule(rule)
            _ruleStatusMessage.value = if (resolved > 0) {
                "Rule '${rule.keywordPattern}' added and retroactively parsed $resolved unparsed SMS!"
            } else {
                "Rule '${rule.keywordPattern}' saved"
            }
        }
    }

    fun deleteRule(rule: CustomKeywordRuleEntity) {
        viewModelScope.launch {
            repository.deleteKeywordRule(rule)
            _ruleStatusMessage.value = "Deleted rule '${rule.keywordPattern}'"
        }
    }

    fun deleteMerchantRule(rule: MerchantCategoryRuleEntity) {
        viewModelScope.launch {
            repository.deleteMerchantRule(rule)
            _ruleStatusMessage.value = "Removed merchant rule for '${rule.merchantIdentifier}'"
        }
    }

    fun dismissUnparsed(item: UnparsedSmsEntity) {
        viewModelScope.launch {
            repository.dismissUnparsedSms(item)
        }
    }
}
