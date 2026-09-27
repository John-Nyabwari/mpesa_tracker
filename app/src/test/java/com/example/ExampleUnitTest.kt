package com.example

import com.example.data.local.entities.CustomKeywordRuleEntity
import com.example.data.local.entities.TransactionDirection
import com.example.data.parser.ParsingEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val engine = ParsingEngine()
    private val now = 1727395200000L

    @Test
    fun parseAllTwelveMpesaSubtypes_isAccurate() {
        // 1. Send Money
        val sendRes = engine.parse(
            "QA12BC34DE Confirmed. Ksh1,500.00 sent to JOHN DOE 0712345678 on 10/9/26 at 2:47 PM. New M-PESA balance is Ksh4,320.00. Transaction cost, Ksh15.00.",
            now
        )
        assertNotNull(sendRes)
        assertEquals("QA12BC34DE", sendRes!!.mpesaCode)
        assertEquals(TransactionDirection.EXPENSE, sendRes.direction)
        assertEquals(1500.0, sendRes.amount, 0.01)
        assertEquals("JOHN DOE (0712345678)", sendRes.counterparty)
        assertEquals(15.0, sendRes.transactionCost, 0.01)
        assertEquals(4320.0, sendRes.newBalance!!, 0.01)

        // 2. Receive Money
        val recRes = engine.parse(
            "QB23CD45EF Confirmed. You have received Ksh2,000.00 from JANE DOE 0723456789 on 10/9/26 at 1:15 PM. New M-PESA balance is Ksh6,320.00.",
            now
        )
        assertNotNull(recRes)
        assertEquals(TransactionDirection.INCOME, recRes!!.direction)
        assertEquals(2000.0, recRes.amount, 0.01)
        assertEquals("JANE DOE (0723456789)", recRes.counterparty)

        // 3. Pay Bill
        val payBillRes = engine.parse(
            "QC34DE56FG Confirmed. Ksh3,200.00 paid to KPLC PREPAID. for account 14253647589 on 9/9/26 at 8:30 AM. New M-PESA balance is Ksh1,120.00. Transaction cost, Ksh23.00.",
            now
        )
        assertNotNull(payBillRes)
        assertEquals(TransactionDirection.EXPENSE, payBillRes!!.direction)
        assertEquals(3200.0, payBillRes.amount, 0.01)
        assertEquals("KPLC PREPAID", payBillRes.counterparty)
        assertEquals("14253647589", payBillRes.accountOrTill)
        assertEquals(23.0, payBillRes.transactionCost, 0.01)

        // 4. Buy Goods / Till
        val buyGoodsRes = engine.parse(
            "QD45EF67GH Confirmed. Ksh850.00 paid to NAIVAS SUPERMARKET. on 9/9/26 at 7:14 PM. New M-PESA balance is Ksh270.00. Transaction cost, Ksh0.00.",
            now
        )
        assertNotNull(buyGoodsRes)
        assertEquals("Shopping", buyGoodsRes!!.category)
        assertEquals("NAIVAS SUPERMARKET", buyGoodsRes.counterparty)

        // 5. Withdraw from Agent
        val withdrawRes = engine.parse(
            "QE56FG78HI Confirmed. on 8/9/26 at 11:20 AM Withdraw Ksh1,000.00 from 283940 - Quick Agent. New M-PESA balance is Ksh5,000.00. Transaction cost, Ksh28.00.",
            now
        )
        assertNotNull(withdrawRes)
        assertEquals("Cash Withdrawal", withdrawRes!!.category)
        assertEquals(28.0, withdrawRes.transactionCost, 0.01)

        // 6. Deposit
        val depositRes = engine.parse(
            "QF67GH89IJ Confirmed. on 7/9/26 at 10:05 AM Give Ksh5,000.00 to Agent Name. New M-PESA balance is Ksh10,000.00.",
            now
        )
        assertNotNull(depositRes)
        assertEquals(TransactionDirection.INCOME, depositRes!!.direction)
        assertEquals(5000.0, depositRes.amount, 0.01)

        // 7. Airtime Purchase
        val airtimeRes = engine.parse(
            "QG78HI90JK Confirmed. You bought Ksh100.00 of airtime on 6/9/26 at 9:00 AM. New M-PESA balance is Ksh4,900.00. Transaction cost, Ksh0.00.",
            now
        )
        assertNotNull(airtimeRes)
        assertEquals("Airtime & Bundles", airtimeRes!!.category)
        assertEquals(100.0, airtimeRes.amount, 0.01)

        // 8. Bundles
        val bundlesRes = engine.parse(
            "QH89IJ01KL Confirmed. You bought Ksh500.00 of data bundles on 5/9/26 at 3:12 PM. New M-PESA balance is Ksh4,400.00.",
            now
        )
        assertNotNull(bundlesRes)
        assertEquals("Airtime & Bundles", bundlesRes!!.category)
        assertEquals(500.0, bundlesRes.amount, 0.01)

        // 9. M-Shwari / KCB M-Pesa
        val mshwariRes = engine.parse(
            "QI90JK12LM Confirmed. Ksh2,000.00 transferred to M-Shwari on 4/9/26 at 10:00 AM. New M-PESA balance is Ksh2,400.00.",
            now
        )
        assertNotNull(mshwariRes)
        assertEquals("Savings", mshwariRes!!.category)
        assertEquals(TransactionDirection.EXPENSE, mshwariRes.direction)

        // 10. Fuliza Overdraft
        val fulizaRes = engine.parse(
            "QJ01KL23MN Confirmed. Fuliza M-PESA amount is Ksh450.00. Fee charged Ksh5.00.",
            now
        )
        assertNotNull(fulizaRes)
        assertEquals("Fees & Charges", fulizaRes!!.category)
        assertEquals(450.0, fulizaRes.amount, 0.01)
        assertEquals(5.0, fulizaRes.transactionCost, 0.01)

        // 11. Reversal
        val reversalRes = engine.parse(
            "QK12LM34NO Confirmed. Reversal of transaction QA12BC34DE has been completed. Ksh1,500.00 has been credited to your M-PESA account.",
            now
        )
        assertNotNull(reversalRes)
        assertEquals(TransactionDirection.INCOME, reversalRes!!.direction)
        assertEquals(1500.0, reversalRes.amount, 0.01)

        // 12. Balance-Only Filter
        val balOnly = "QL23MN45OP Confirmed. Your M-PESA balance is Ksh4,320.00 on 26/9/26 at 4:00 PM."
        assertTrue(engine.isBalanceOnly(balOnly))
        assertNull(engine.parse(balOnly, now))

        // Custom Keyword Rule
        val customRule = CustomKeywordRuleEntity(
            keywordPattern = "GlobalPay",
            direction = TransactionDirection.EXPENSE,
            defaultCategory = "Entertainment"
        )
        val customSms = "QM99ZZ88XY Confirmed. M-PESA GlobalPay Visa deduction of Ksh4,120.00 at NETFLIX.COM on 26/9/26."
        val customParsed = engine.parse(customSms, now, listOf(customRule))
        assertNotNull(customParsed)
        assertEquals("QM99ZZ88XY", customParsed!!.mpesaCode)
        assertEquals(4120.0, customParsed.amount, 0.01)
        assertEquals("Entertainment", customParsed.category)
    }
}
