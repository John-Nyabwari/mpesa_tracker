package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.parser.ParsingEngine
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MpesaTrackerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val parsingEngine: ParsingEngine by lazy { ParsingEngine() }
    val repository: ExpenseRepository by lazy { ExpenseRepository(database, parsingEngine) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.ensureDefaultsInitialized()
        }
    }
}
