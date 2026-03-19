package com.example.financetracker.presentation.ledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.presentation.model.TransactionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val userPreferencesRepository: com.example.financetracker.domain.repository.UserPreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Tab state persistence - survives navigation and process death
    val selectedTab: StateFlow<Int> = savedStateHandle.getStateFlow("ledger_tab", 0)

    fun selectTab(index: Int) {
        savedStateHandle["ledger_tab"] = index
    }

    fun resetTab() {
        savedStateHandle["ledger_tab"] = 0
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionUiModel>> = kotlinx.coroutines.flow.combine(
            searchQuery.debounce(300),
            selectedFilter
        ) { query, filter ->
            Pair(query, filter)
        }
        .flatMapLatest { (query, filter) ->
            repository.searchTransactions(query).map { transactionList: List<TransactionUiModel> ->
                when (filter) {
                    "Income" -> transactionList.filter { tx -> tx.type == "INCOME" }
                    "Expense" -> transactionList.filter { tx -> tx.type == "EXPENSE" && tx.category != "Money Transfer" }
                    "Transfer" -> transactionList.filter { tx -> tx.category == "Money Transfer" || tx.category == "Money Received" }
                    else -> transactionList
                }
            }
        }
        .onEach { _isLoading.value = false }
        .catch { e ->
            _error.value = "Failed to load transactions: ${e.localizedMessage ?: "Unknown error"}"
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currencySymbol = userPreferencesRepository.currency.map { currencyCode ->
        try {
            java.util.Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            AppLogger.w("LedgerViewModel", "Invalid currency code: $currencyCode, using code as fallback", e)
            currencyCode
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "₹"
    )

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun clearError() {
        _error.value = null
    }
}
