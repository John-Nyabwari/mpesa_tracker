package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.CategoryEntity
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val categories = repository.getAllCategories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun createCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val cleanColor = if (colorHex.startsWith("#")) colorHex.trim() else "#${colorHex.trim()}"
            repository.addCategory(CategoryEntity(name = name.trim(), colorHex = cleanColor, isDefault = false))
        }
    }

    fun updateCategory(oldName: String, newName: String, colorHex: String) {
        viewModelScope.launch {
            val cleanColor = if (colorHex.startsWith("#")) colorHex.trim() else "#${colorHex.trim()}"
            repository.updateCategory(oldName, newName.trim(), cleanColor)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
