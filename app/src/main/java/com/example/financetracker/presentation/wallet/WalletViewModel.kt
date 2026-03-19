package com.example.financetracker.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.local.mapper.toAccountUiModel
import com.example.financetracker.data.local.mapper.toPersonUiModel
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.presentation.model.WalletUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.core.util.CurrencyFormatter

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WalletUiState> = combine(
        repository.getAllWallets(),
        repository.getAllPeople(),
        userPreferencesRepository.currency,
        _error
    ) { wallets, people, currency, error ->
        val peopleUiModels = people.map { it.toPersonUiModel(currency) }

        // Segregate Assets vs Liabilities
        val assets = wallets.filter { it.type == "BANK" || it.type == "CASH" || it.type == "INVESTMENT" }
        val liabilities = wallets.filter { it.type == "CREDIT" }

        // Calculate Totals using rawBalance
        val totalAssets = assets.sumOf { it.rawBalance }
        val totalLiabilities = liabilities.sumOf { it.rawBalance }

        // Calculate Debt Totals
        val themOweYou = people.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val youOweThem = people.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

        WalletUiState(
            assetWallets = assets,
            liabilityWallets = liabilities,
            people = peopleUiModels,
            totalAssets = CurrencyFormatter.format(totalAssets, currency),
            totalLiabilities = CurrencyFormatter.format(kotlin.math.abs(totalLiabilities), currency),
            totalOwedToYouFormatted = CurrencyFormatter.format(themOweYou, currency),
            totalOwedByYouFormatted = CurrencyFormatter.format(youOweThem, currency),
            isLoading = false,
            error = error
        )
    }.catch { e ->
        emit(WalletUiState(
            isLoading = false,
            error = "Failed to load wallets: ${e.localizedMessage ?: "Unknown error"}"
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletUiState()
    )

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun addWallet(name: String, amount: String, type: String) {
        viewModelScope.launch {
            try {
                val amountLong = amount.toDoubleOrNull()?.let { (it * 100).toLong() }
                if (amountLong == null) {
                    _error.value = "Invalid amount"
                    return@launch
                }
                val color = "#2196F3"
                repository.addWallet(name, type, amountLong, color)
            } catch (e: Exception) {
                _error.value = "Failed to add wallet: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun addPerson(name: String, amount: String) {
        viewModelScope.launch {
            try {
                val amountLong = amount.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                repository.addPerson(name, amountLong)
            } catch (e: Exception) {
                _error.value = "Failed to add person: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteWallet(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteWallet(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete wallet: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePerson(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete person: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateWallet(id: Long, name: String, amount: String, type: String) {
        viewModelScope.launch {
            try {
                val amountLong = amount.toDoubleOrNull()?.let { (it * 100).toLong() }
                if (amountLong == null) {
                    _error.value = "Invalid amount"
                    return@launch
                }
                val color = "#2196F3"
                repository.updateWallet(id, name, type, amountLong, color)
            } catch (e: Exception) {
                _error.value = "Failed to update wallet: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updatePerson(id: Long, name: String, amount: String) {
        viewModelScope.launch {
            try {
                val amountLong = amount.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                repository.updatePerson(id, name, amountLong)
            } catch (e: Exception) {
                _error.value = "Failed to update person: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
