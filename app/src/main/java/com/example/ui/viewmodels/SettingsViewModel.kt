package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.entities.UserPreferencesEntity
import com.example.data.repository.ExpenseRepository
import com.example.worker.HistoricalScanWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val context: Context
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val preferences: StateFlow<UserPreferencesEntity?> = repository.getUserPreferences().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferencesEntity()
    )

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun updateProfile(name: String, currency: String, allowedSenders: String) {
        viewModelScope.launch {
            val current = preferences.value ?: UserPreferencesEntity()
            val updated = current.copy(
                displayName = name.trim().ifEmpty { "M-Pesa User" },
                currencySymbol = currency.trim().ifEmpty { "KSh" },
                allowedSenderIds = allowedSenders.trim().ifEmpty { "MPESA,M-PESA,SAFARICOM" }
            )
            repository.savePreferences(updated)
            _feedbackMessage.value = "Profile & sender filters saved"
        }
    }

    fun updateTrackingStartDate(newStartDate: Long) {
        viewModelScope.launch {
            val current = preferences.value ?: UserPreferencesEntity()
            val updated = current.copy(trackingStartDate = newStartDate)
            repository.savePreferences(updated)

            // Trigger immediate repository scan + enqueue background HistoricalScanWorker
            repository.runHistoricalScan(context.contentResolver, newStartDate)

            val inputData = Data.Builder()
                .putLong("trackingStartDate", newStartDate)
                .build()

            val scanRequest = OneTimeWorkRequestBuilder<HistoricalScanWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(scanRequest)
            _feedbackMessage.value = if (newStartDate == 0L) {
                "Tracking start date reset to All Time & historical rescan queued"
            } else {
                "Tracking start date updated & historical inbox rescan triggered"
            }
        }
    }
}
