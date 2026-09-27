package com.example.data.parser

import com.example.data.local.entities.CustomKeywordRuleEntity
import java.util.regex.Pattern
import kotlin.math.abs

class ParsingEngine {
    private val balanceDetector = BalanceOnlyDetector()
    private val registeredParsers = listOf<MpesaSubtypeParser>(
        SendMoneyParser(),
        ReceiveMoneyParser(),
        PayBillParser(),
        BuyGoodsParser(),
        AgentWithdrawParser(),
        DepositParser(),
        AirtimeParser(),
        BundlesParser(),
        SavingsParser(),
        FulizaParser(),
        ReversalParser()
    )

    fun isBalanceOnly(body: String): Boolean {
        return balanceDetector.isBalanceOnly(body)
    }

    fun parse(
        sms: String,
        fallbackTime: Long,
        customRules: List<CustomKeywordRuleEntity> = emptyList()
    ): ParsedTransactionResult? {
        if (balanceDetector.isBalanceOnly(sms)) {
            return null
        }

        // Try standard M-Pesa subtype parsers first
        for (parser in registeredParsers) {
            if (parser.matches(sms)) {
                val res = parser.parse(sms, fallbackTime)
                if (res != null) return res
            }
        }

        // Try custom keyword rules if standard parsers miss
        for (rule in customRules) {
            if (sms.contains(rule.keywordPattern, ignoreCase = true)) {
                val parsed = parseWithCustomRule(sms, rule, fallbackTime)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    private fun parseWithCustomRule(
        sms: String,
        rule: CustomKeywordRuleEntity,
        fallbackTime: Long
    ): ParsedTransactionResult? {
        val amountRegex = if (!rule.customAmountRegex.isNullOrBlank()) {
            try {
                Pattern.compile(rule.customAmountRegex, Pattern.CASE_INSENSITIVE)
            } catch (_: Exception) {
                Pattern.compile("(?:Ksh\\.?|KES)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
            }
        } else {
            Pattern.compile("(?:Ksh\\.?|KES)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        }

        val codeMatcher = Pattern.compile("^([A-Z0-9]{8,12})\\s+", Pattern.CASE_INSENSITIVE).matcher(sms.trim())
        val code = if (codeMatcher.find()) {
            codeMatcher.group(1)?.uppercase()
        } else {
            "KW" + abs(sms.hashCode()).toString().take(8)
        }

        val amtMatcher = amountRegex.matcher(sms)
        val amount = if (amtMatcher.find()) {
            val groupVal = if (amtMatcher.groupCount() >= 1) amtMatcher.group(1) else amtMatcher.group(0)
            groupVal?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
        } else 0.0

        if (amount <= 0.0) return null

        return ParsedTransactionResult(
            mpesaCode = code ?: "KW${System.currentTimeMillis()}",
            direction = rule.direction,
            amount = amount,
            counterparty = "Custom Rule (${rule.keywordPattern})",
            category = rule.defaultCategory,
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = fallbackTime,
            rawSms = sms,
            subtypeName = "Custom Keyword Rule"
        )
    }
}
