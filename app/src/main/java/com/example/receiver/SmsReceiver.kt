package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.SmsProcessWorker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: continue
                val body = sms.displayMessageBody ?: continue
                val ts = sms.timestampMillis

                val inputData = Data.Builder()
                    .putString("sender", sender)
                    .putString("body", body)
                    .putLong("timestamp", ts)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
