package com.example.financetracker.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    val categories = repository.getAllCategories()

    private val _selectedTab = MutableStateFlow(0) // 0 = Expense, 1 = Income
    val selectedTab = _selectedTab.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Seed on init
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun addCategory(name: String, type: String, color: String, iconId: Int = 0) {
        viewModelScope.launch {
            try {
                val success = repository.addCategory(name, type, color, iconId)
                if (!success) {
                    _error.value = "Category '$name' already exists!"
                }
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateCategory(id: Long, name: String, type: String, color: String, iconId: Int = 0) {
        viewModelScope.launch {
            try {
                val success = repository.updateCategory(id, name, type, color, iconId)
                if (!success) {
                    _error.value = "Name '$name' is already taken!"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
