package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CustomKeywordRuleEntity
import com.example.data.local.entities.TransactionDirection
import com.example.data.parser.ParsingEngine
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SmsIngestOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepository(db, ParsingEngine())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("M-Pesa Tracker", appName)
    }

    @Test
    fun `ingest SMS with cost creates both main transaction and secondary fee line item`() = runBlocking {
        val outcome = repository.processIncomingSms(
            sender = "MPESA",
            body = "QA12BC34DE Confirmed. Ksh1,500.00 sent to JOHN DOE 0712345678 on 10/9/26 at 2:47 PM. New M-PESA balance is Ksh4,320.00. Transaction cost, Ksh15.00.",
            timestamp = 1727395200000L
        )
        assertEquals(SmsIngestOutcome.PARSED_TRANSACTION, outcome)

        val allTx = repository.getTransactions(0L).first()
        assertEquals(2, allTx.size)
        val mainTx = allTx.first { !it.isCostLineItem }
        val feeTx = allTx.first { it.isCostLineItem }
        assertEquals(1500.0, mainTx.amount, 0.01)
        assertEquals(15.0, feeTx.amount, 0.01)
        assertEquals("Fees & Charges", feeTx.category)
    }

    @Test
    fun `unparsed SMS is queued and retroactively parsed when custom keyword rule is added`() = runBlocking {
        val customSms = "QM99ZZ88XY Confirmed. M-PESA GlobalPay Visa deduction of Ksh4,120.00 at NETFLIX.COM on 26/9/26."
        val outcome = repository.processIncomingSms("MPESA", customSms, 1727395200000L)
        assertEquals(SmsIngestOutcome.QUEUED_UNPARSED, outcome)

        assertEquals(1, repository.getPendingUnparsed().first().size)

        val resolvedCount = repository.addKeywordRule(
            CustomKeywordRuleEntity(
                keywordPattern = "GlobalPay",
                direction = TransactionDirection.EXPENSE,
                defaultCategory = "Entertainment"
            )
        )
        assertEquals(1, resolvedCount)
        assertEquals(0, repository.getPendingUnparsed().first().size)

        val allTx = repository.getTransactions(0L).first()
        assertEquals(1, allTx.size)
        assertEquals("Entertainment", allTx.first().category)
        assertEquals(4120.0, allTx.first().amount, 0.01)
    }
}
