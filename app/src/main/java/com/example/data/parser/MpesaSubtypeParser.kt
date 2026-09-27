package com.example.data.parser

import com.example.data.local.entities.TransactionDirection
import java.util.regex.Pattern

interface MpesaSubtypeParser {
    val subtypeName: String
    fun matches(sms: String): Boolean
    fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult?
}

internal fun extractCost(sms: String): Double {
    val costRegex = Pattern.compile(
        "(?:Transaction cost|Fee charged|Access fee charged),?\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )
    val matcher = costRegex.matcher(sms)
    return if (matcher.find()) {
        matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    } else 0.0
}

internal fun extractBalance(sms: String): Double? {
    val balRegex = Pattern.compile(
        "New (?:M-PESA|account) balance is\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )
    val matcher = balRegex.matcher(sms)
    return if (matcher.find()) {
        matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
    } else null
}

// 1. Send Money (Person to Person)
class SendMoneyParser : MpesaSubtypeParser {
    override val subtypeName = "Send Money"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+sent to\\s+([A-Za-z0-9\\s\\.'-]+?)(?:\\s+([0-9+]{9,15}))?\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("sent to", ignoreCase = true) && !sms.contains("transferred to", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val recipient = m.group(3)?.trim() ?: "Unknown"
        val phone = m.group(4)?.trim()
        val counterparty = if (!phone.isNullOrEmpty()) "$recipient ($phone)" else recipient
        val dateStr = m.group(5) ?: ""
        val timeStr = m.group(6) ?: ""
        val ts = MpesaDateParser.parseTimestamp(dateStr, timeStr, receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = counterparty,
            category = "Transfers",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 2. Receive Money
class ReceiveMoneyParser : MpesaSubtypeParser {
    override val subtypeName = "Receive Money"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*You have received\\s+Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([A-Za-z0-9\\s\\.'-]+?)(?:\\s+([0-9+]{9,15}))?\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        (sms.contains("You have received", ignoreCase = true) || sms.contains("received Ksh", ignoreCase = true)) &&
            !sms.contains("deposit", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val sender = m.group(3)?.trim() ?: "Unknown"
        val phone = m.group(4)?.trim()
        val counterparty = if (!phone.isNullOrEmpty()) "$sender ($phone)" else sender
        val ts = MpesaDateParser.parseTimestamp(m.group(5) ?: "", m.group(6) ?: "", receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.INCOME,
            amount = amount,
            counterparty = counterparty,
            category = "Transfers",
            newBalance = extractBalance(sms),
            transactionCost = 0.0,
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 3. Pay Bill
class PayBillParser : MpesaSubtypeParser {
    override val subtypeName = "Pay Bill"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+paid to\\s+([A-Za-z0-9\\s\\.\\&\\'-]+?)\\.?\\s+for account\\s+(.+?)\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("paid to", ignoreCase = true) && sms.contains("for account", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchant = m.group(3)?.trim()?.trimEnd('.') ?: "Unknown PayBill"
        val account = m.group(4)?.trim()?.trimEnd('.')
        val ts = MpesaDateParser.parseTimestamp(m.group(5) ?: "", m.group(6) ?: "", receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = merchant,
            accountOrTill = account,
            category = "Utilities",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 4. Buy Goods / Till (Lipa na M-Pesa)
class BuyGoodsParser : MpesaSubtypeParser {
    override val subtypeName = "Buy Goods / Till"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+paid to\\s+([A-Za-z0-9\\s\\.\\&\\'-]+?)\\.?\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("paid to", ignoreCase = true) && !sms.contains("for account", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchant = m.group(3)?.trim()?.trimEnd('.') ?: "Merchant"
        val ts = MpesaDateParser.parseTimestamp(m.group(4) ?: "", m.group(5) ?: "", receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = merchant,
            category = "Shopping",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 5. Agent Withdrawal
class AgentWithdrawParser : MpesaSubtypeParser {
    override val subtypeName = "Agent Withdraw"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))\\s*Withdraw Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([A-Za-z0-9\\s\\-&']+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) = sms.contains("Withdraw Ksh", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val ts = MpesaDateParser.parseTimestamp(m.group(2) ?: "", m.group(3) ?: "", receivedFallbackTime)
        val amount = m.group(4)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val agent = m.group(5)?.split(".")?.firstOrNull()?.trim() ?: "Agent"

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = agent,
            category = "Cash Withdrawal",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 6. Agent / Bank Deposit
class DepositParser : MpesaSubtypeParser {
    override val subtypeName = "Deposit"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*(?:on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))\\s+)?(?:Give|deposit of)\\s+Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:cash\\s+)?(?:to|from)\\s+([A-Za-z0-9\\s\\-&']+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("Give Ksh", ignoreCase = true) || sms.contains("deposit of", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val dateStr = m.group(2) ?: ""
        val timeStr = m.group(3) ?: ""
        val ts = if (dateStr.isNotEmpty() && timeStr.isNotEmpty()) {
            MpesaDateParser.parseTimestamp(dateStr, timeStr, receivedFallbackTime)
        } else receivedFallbackTime
        val amount = m.group(4)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val source = m.group(5)?.split(".")?.firstOrNull()?.trim() ?: "Deposit"

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.INCOME,
            amount = amount,
            counterparty = source,
            category = "Transfers",
            newBalance = extractBalance(sms),
            transactionCost = 0.0,
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 7. Airtime Purchase
class AirtimeParser : MpesaSubtypeParser {
    override val subtypeName = "Airtime Purchase"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*You bought Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+of airtime(?:\\s+for\\s+[0-9+]+)?\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) = sms.contains("of airtime", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val ts = MpesaDateParser.parseTimestamp(m.group(3) ?: "", m.group(4) ?: "", receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = "Safaricom Airtime",
            category = "Airtime & Bundles",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 8. Bundles Purchase
class BundlesParser : MpesaSubtypeParser {
    override val subtypeName = "Bundles Purchase"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*You bought Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+of\\s+([A-Za-z0-9\\s]+?bundles?)\\s+on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) = sms.contains("bundles", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val item = m.group(3)?.trim() ?: "Data Bundles"
        val ts = MpesaDateParser.parseTimestamp(m.group(4) ?: "", m.group(5) ?: "", receivedFallbackTime)

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = "Safaricom ($item)",
            category = "Airtime & Bundles",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 9. M-Shwari / KCB M-Pesa Transfers
class SavingsParser : MpesaSubtypeParser {
    override val subtypeName = "Savings / Loans"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(transferred to|transferred from)\\s+([A-Za-z0-9\\s\\-]+?)\\s+(?:to M-PESA\\s+)?on\\s+([0-9/]+)\\s+at\\s+([0-9:]+\\s*(?:AM|PM|am|pm))",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("M-Shwari", ignoreCase = true) || sms.contains("KCB M-PESA", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val flow = m.group(3)?.lowercase() ?: ""
        val target = m.group(4)?.trim() ?: "Savings Account"
        val ts = MpesaDateParser.parseTimestamp(m.group(5) ?: "", m.group(6) ?: "", receivedFallbackTime)
        val direction = if (flow.contains("from")) TransactionDirection.INCOME else TransactionDirection.EXPENSE

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = direction,
            amount = amount,
            counterparty = target,
            category = "Savings",
            newBalance = extractBalance(sms),
            transactionCost = extractCost(sms),
            timestamp = ts,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 10. Fuliza Overdraft
class FulizaParser : MpesaSubtypeParser {
    override val subtypeName = "Fuliza Overdraft"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Fuliza M-PESA amount is Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\.\\s*(?:Fee|Access fee) charged Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) = sms.contains("Fuliza M-PESA", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val amount = m.group(2)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val fee = m.group(3)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.EXPENSE,
            amount = amount,
            counterparty = "Fuliza M-Pesa Overdraft",
            category = "Fees & Charges",
            newBalance = extractBalance(sms),
            transactionCost = fee,
            timestamp = receivedFallbackTime,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 11. Transaction Reversal
class ReversalParser : MpesaSubtypeParser {
    override val subtypeName = "Reversal"
    private val pattern = Pattern.compile(
        "^([A-Z0-9]+)\\s+Confirmed\\.\\s*Reversal of transaction\\s+([A-Z0-9]+)\\s+has been completed\\.\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+has been (?:credited|reversed)",
        Pattern.CASE_INSENSITIVE
    )

    override fun matches(sms: String) =
        sms.contains("Reversal of transaction", ignoreCase = true) || sms.contains("has been reversed", ignoreCase = true)

    override fun parse(sms: String, receivedFallbackTime: Long): ParsedTransactionResult? {
        val m = pattern.matcher(sms.trim())
        if (!m.find()) return null
        val code = m.group(1) ?: return null
        val origRef = m.group(2) ?: ""
        val amount = m.group(3)?.replace(",", "")?.toDoubleOrNull() ?: return null

        return ParsedTransactionResult(
            mpesaCode = code.uppercase(),
            direction = TransactionDirection.INCOME,
            amount = amount,
            counterparty = "Reversal ($origRef)",
            category = "Transfers",
            newBalance = extractBalance(sms),
            transactionCost = 0.0,
            timestamp = receivedFallbackTime,
            rawSms = sms,
            subtypeName = subtypeName
        )
    }
}

// 12. Balance Only / Statement Inspector
class BalanceOnlyDetector {
    fun isBalanceOnly(sms: String): Boolean {
        val isBalStatement = sms.contains("your account balance was", ignoreCase = true) ||
            sms.contains("your current M-PESA balance is", ignoreCase = true) ||
            sms.contains("your M-PESA balance is Ksh", ignoreCase = true) ||
            sms.contains("Your balance is Ksh", ignoreCase = true)
        val hasMovement = sms.contains("sent to", ignoreCase = true) ||
            sms.contains("paid to", ignoreCase = true) ||
            sms.contains("received", ignoreCase = true) ||
            sms.contains("bought", ignoreCase = true) ||
            sms.contains("Withdraw Ksh", ignoreCase = true) ||
            sms.contains("transferred", ignoreCase = true) ||
            sms.contains("Fuliza M-PESA amount is", ignoreCase = true) ||
            sms.contains("Reversal of transaction", ignoreCase = true)
        return isBalStatement && !hasMovement
    }
}
