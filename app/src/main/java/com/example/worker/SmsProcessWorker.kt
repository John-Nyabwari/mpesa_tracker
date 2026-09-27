package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MpesaTrackerApp

class SmsProcessWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        val app = applicationContext as? MpesaTrackerApp ?: return Result.failure()
        return try {
            app.repository.processIncomingSms(sender, body, timestamp)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
