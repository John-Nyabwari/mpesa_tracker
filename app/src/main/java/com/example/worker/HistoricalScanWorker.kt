package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MpesaTrackerApp

class HistoricalScanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val trackingStartDate = inputData.getLong("trackingStartDate", 0L)
        val app = applicationContext as? MpesaTrackerApp ?: return Result.failure()
        return try {
            app.repository.runHistoricalScan(appContext.contentResolver, trackingStartDate)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
