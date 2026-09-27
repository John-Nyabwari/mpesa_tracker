package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.ExpenseRepository

class AppViewModelFactory(
    private val repository: ExpenseRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository, appContext) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, appContext) as T
            modelClass.isAssignableFrom(RulesViewModel::class.java) ->
                RulesViewModel(repository) as T
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) ->
                CategoriesViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
