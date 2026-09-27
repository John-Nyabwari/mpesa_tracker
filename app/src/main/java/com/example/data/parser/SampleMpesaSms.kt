package com.example.data.parser

data class MpesaSampleTemplate(
    val title: String,
    val subtype: String,
    val smsBody: String
)

object SampleMpesaSms {
    val templates = listOf(
        MpesaSampleTemplate(
            title = "1. Send Money (P2P)",
            subtype = "Send Money",
            smsBody = "QA12BC34DE Confirmed. Ksh1,500.00 sent to JOHN KAMAU 0712345678 on 25/9/26 at 2:47 PM. New M-PESA balance is Ksh14,320.00. Transaction cost, Ksh15.00."
        ),
        MpesaSampleTemplate(
            title = "2. Receive Money",
            subtype = "Receive Money",
            smsBody = "QB23CD45EF Confirmed. You have received Ksh8,500.00 from GRACE WANJIKU 0723456789 on 24/9/26 at 1:15 PM. New M-PESA balance is Ksh15,835.00."
        ),
        MpesaSampleTemplate(
            title = "3. Pay Bill (KPLC Utility)",
            subtype = "Pay Bill",
            smsBody = "QC34DE56FG Confirmed. Ksh3,200.00 paid to KPLC PREPAID. for account 14253647589 on 23/9/26 at 8:30 AM. New M-PESA balance is Ksh7,335.00. Transaction cost, Ksh23.00."
        ),
        MpesaSampleTemplate(
            title = "4. Buy Goods / Till (Naivas)",
            subtype = "Buy Goods / Till",
            smsBody = "QD45EF67GH Confirmed. Ksh2,450.00 paid to NAIVAS SUPERMARKET. on 22/9/26 at 7:14 PM. New M-PESA balance is Ksh10,558.00. Transaction cost, Ksh0.00."
        ),
        MpesaSampleTemplate(
            title = "5. Agent Withdrawal",
            subtype = "Agent Withdraw",
            smsBody = "QE56FG78HI Confirmed. on 21/9/26 at 11:20 AM Withdraw Ksh2,000.00 from 283940 - QuickMart Agent Westlands. New M-PESA balance is Ksh13,008.00. Transaction cost, Ksh29.00."
        ),
        MpesaSampleTemplate(
            title = "6. Agent / Bank Deposit",
            subtype = "Deposit",
            smsBody = "QF67GH89IJ Confirmed. on 20/9/26 at 10:05 AM Give Ksh12,000.00 to Safaricom Shop Kimathi. New M-PESA balance is Ksh15,037.00."
        ),
        MpesaSampleTemplate(
            title = "7. Airtime Purchase",
            subtype = "Airtime Purchase",
            smsBody = "QG78HI90JK Confirmed. You bought Ksh200.00 of airtime on 19/9/26 at 9:00 AM. New M-PESA balance is Ksh3,037.00. Transaction cost, Ksh0.00."
        ),
        MpesaSampleTemplate(
            title = "8. Data Bundles Purchase",
            subtype = "Bundles Purchase",
            smsBody = "QH89IJ01KL Confirmed. You bought Ksh1,000.00 of data bundles on 18/9/26 at 3:12 PM. New M-PESA balance is Ksh3,237.00. Transaction cost, Ksh0.00."
        ),
        MpesaSampleTemplate(
            title = "9. M-Shwari Transfer",
            subtype = "Savings / Loans",
            smsBody = "QI90JK12LM Confirmed. Ksh2,500.00 transferred to M-Shwari on 17/9/26 at 10:00 AM. New M-PESA balance is Ksh4,237.00. Transaction cost, Ksh0.00."
        ),
        MpesaSampleTemplate(
            title = "10. Fuliza Overdraft",
            subtype = "Fuliza Overdraft",
            smsBody = "QJ01KL23MN Confirmed. Fuliza M-PESA amount is Ksh650.00. Fee charged Ksh10.00. Total Fuliza M-PESA outstanding amount is Ksh660.00."
        ),
        MpesaSampleTemplate(
            title = "11. Transaction Reversal",
            subtype = "Reversal",
            smsBody = "QK12LM34NO Confirmed. Reversal of transaction QA12BC34DE has been completed. Ksh1,500.00 has been credited to your M-PESA account. New M-PESA balance is Ksh15,820.00."
        ),
        MpesaSampleTemplate(
            title = "12. Balance-Only Statement (Ignored)",
            subtype = "Balance Inquiry (Ignored)",
            smsBody = "QL23MN45OP Confirmed. Your M-PESA balance is Ksh14,320.00 on 26/9/26 at 4:00 PM."
        ),
        MpesaSampleTemplate(
            title = "13. Unparsed Custom Format (Queues for Rule)",
            subtype = "Unparsed Queue Test",
            smsBody = "QM99ZZ88XY Confirmed. M-PESA GlobalPay Visa deduction of Ksh4,120.00 at NETFLIX.COM on 26/9/26. New M-PESA balance is Ksh10,200.00."
        )
    )
}
