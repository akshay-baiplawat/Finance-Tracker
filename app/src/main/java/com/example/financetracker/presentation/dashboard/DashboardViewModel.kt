package com.example.financetracker.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.local.mapper.toAccountUiModel
import com.example.financetracker.data.local.mapper.toTransactionUiModel
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.TransactionAttachmentRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.presentation.model.DashboardUiState
import com.example.financetracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.presentation.components.AttachmentUiModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val transactionAttachmentRepository: TransactionAttachmentRepository
) : ViewModel() {

    init {
        // Seed default categories and generate notifications on app launch
        viewModelScope.launch {
            // Ensure all default categories exist (including Trip for existing users)
            repository.seedDefaultCategories()
            repository.ensureTripCategoryExists()
            notificationRepository.generateNotifications()
            notificationRepository.cleanupOldNotifications()
        }
    }

    private val _selectedFilter = kotlinx.coroutines.flow.MutableStateFlow("All")

    private val _error = MutableStateFlow<String?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val isAmountsHidden: StateFlow<Boolean> = userPreferencesRepository.isAmountsHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTripMode: StateFlow<Boolean> = userPreferencesRepository.isTripMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleAmountsHidden() {
        viewModelScope.launch {
            userPreferencesRepository.setAmountsHidden(!isAmountsHidden.value)
        }
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            try {
                // Sync SMS from last sync time (if Privacy Mode is disabled)
                val isPrivacyMode = userPreferencesRepository.isPrivacyMode.first()
                if (!isPrivacyMode) {
                    val lastSyncTime = userPreferencesRepository.lastSyncTimestamp.first()
                    repository.syncTransactions(lastSyncTime)
                }

                // Regenerate notifications
                notificationRepository.generateNotifications()

                // Trigger a filter change to refresh data
                val currentFilter = _selectedFilter.value
                _selectedFilter.value = ""
                kotlinx.coroutines.delay(100)
                _selectedFilter.value = currentFilter
            } catch (e: Exception) {
                _error.value = "Failed to refresh: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    @ExperimentalCoroutinesApi
    val uiState: StateFlow<DashboardUiState> = _selectedFilter.flatMapLatest { filter ->
        val (start, end) = getDateRangeForFilter(filter)

        val netWorthFlow = repository.getNetWorth()
        val cashFlow = repository.getMonthlyCashFlow(start, end)
        val (cmStart, cmEnd) = DateUtils.getCurrentMonthStartAndEnd()
        val currentMonthStatsFlow = repository.getMonthlyCashFlow(cmStart, cmEnd)

        val walletsFlow = repository.getAllWallets()
        val transactionsFlow = repository.getTransactionsBetween(start, end)
        val currencyFlow = userPreferencesRepository.currency
        val userNameFlow = userPreferencesRepository.userName
        val userImageFlow = userPreferencesRepository.userImagePath
        val unreadCountFlow = notificationRepository.getUnreadCount()

        combine(
            combine(netWorthFlow, cashFlow, currentMonthStatsFlow, ::Triple),
            combine(walletsFlow, transactionsFlow, currencyFlow, ::Triple),
            combine(userNameFlow, userImageFlow, unreadCountFlow, ::Triple),
            _error
        ) { (netWorth, monthlyReport, cmStats), (wallets, transactions, currency), (userName, userImage, unreadCount), error ->

            // Calculate Net Worth Change
            val netChange = cmStats.totalIncome - cmStats.totalExpense
            val previousNetWorth = netWorth - netChange
            val percentChange = if (previousNetWorth != 0L) {
                 (netChange.toDouble() / previousNetWorth.toDouble()) * 100
            } else {
                 if (netChange > 0) 100.0 else 0.0 // Edge case
            }

            val symbol = try {
                java.util.Currency.getInstance(currency).symbol // e.g., "$" or "₹"
            } catch (e: Exception) {
                AppLogger.w("DashboardViewModel", "Invalid currency code: $currency, using code as fallback", e)
                currency // Fallback to code if symbol fails
            }

            DashboardUiState(
                totalNetWorth = CurrencyFormatter.format(netWorth, currency),
                netWorthChangePercentage = percentChange,
                incomeThisMonth = CurrencyFormatter.format(monthlyReport.totalIncome, currency),
                expenseThisMonth = CurrencyFormatter.format(monthlyReport.totalExpense, currency),
                income = monthlyReport.totalIncome,
                expense = monthlyReport.totalExpense,
                wallets = wallets,
                recentTransactions = transactions,
                isLoading = false,  // When we have actual data, loading is complete
                selectedFilter = filter,
                currencySymbol = symbol,
                userName = userName,
                userImagePath = userImage,
                unreadNotificationCount = unreadCount,
                error = error
            )
        }.catch { e ->
            emit(DashboardUiState(
                isLoading = false,
                error = "Failed to load dashboard: ${e.localizedMessage ?: "Unknown error"}"
            ))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun getDateRangeForFilter(filter: String): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val zone = java.time.ZoneId.systemDefault()
        return when (filter) {
            "Daily" -> {
               val start = java.time.LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
               Pair(start, now)
            }
            "Weekly" -> {
               val start = java.time.LocalDate.now().minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
               Pair(start, now)
            }
            "Monthly" -> DateUtils.getCurrentMonthStartAndEnd()
            else -> Pair(0L, now) // All Time
        }
    }

    fun addTransaction(amount: String, category: String, type: String, paymentType: String, note: String, timestamp: Long = System.currentTimeMillis(), attachmentUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            try {
                val parsedAmount = amount.toDoubleOrNull()?.let { (it * 100).toLong() }
                if (parsedAmount == null || parsedAmount <= 0) {
                    _error.value = "Invalid amount"
                    return@launch
                }

                // Resolve Wallet by name (passed from AddTransactionSheet)
                val wallets = repository.getAllWallets().first()
                val targetWallet = wallets.find { it.name == paymentType } ?: wallets.firstOrNull()

                val walletId = targetWallet?.id ?: repository.addWallet("Default Cash", "CASH", 0L, "#4CAF50")

                val entity = TransactionEntity(
                    amount = parsedAmount,
                    note = note,
                    timestamp = timestamp,
                    type = type,
                    categoryName = category,
                    walletId = walletId
                )

                repository.addTransaction(entity)

                // Get the newly added transaction ID (it's the most recent one with same timestamp)
                // Since Room returns ID on insert, we need to get it differently
                // For now, we'll get the latest transaction
                val transactions = repository.getAllTransactions().first()
                val newTransaction = transactions.find {
                    it.timestamp == timestamp && it.amount == parsedAmount && it.merchant == ""
                }

                // Add attachments if any
                if (attachmentUris.isNotEmpty() && newTransaction != null) {
                    transactionAttachmentRepository.addAttachments(newTransaction.id, attachmentUris)
                }
            } catch (e: Exception) {
                _error.value = "Failed to add transaction: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateTransaction(
        id: Long,
        amount: Long,
        note: String,
        category: String,
        type: String,
        timestamp: Long,
        walletId: Long,
        personId: Long?,
        groupId: Long?,
        smsId: Long?,
        newAttachmentUris: List<Uri> = emptyList(),
        deletedAttachmentIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                // We reconstruct the entity.
                // IMPORTANT: The repository's updateTransaction handles the balance revert/apply logic.
                val entity = TransactionEntity(
                    id = id,
                    amount = amount,
                    note = note,
                    categoryName = category,
                    type = type,
                    timestamp = timestamp,
                    walletId = walletId,
                    personId = personId,
                    groupId = groupId,
                    smsId = smsId
                )
                repository.updateTransaction(entity)

                // Handle attachment changes
                // Delete removed attachments
                deletedAttachmentIds.forEach { attachmentId ->
                    transactionAttachmentRepository.deleteAttachmentById(attachmentId)
                }

                // Add new attachments
                if (newAttachmentUris.isNotEmpty()) {
                    transactionAttachmentRepository.addAttachments(id, newAttachmentUris)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update transaction: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                val entity = repository.getTransactionById(id)
                if (entity != null) {
                    repository.deleteTransaction(entity)
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete transaction: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Get attachments for a transaction to display in EditTransactionSheet
     */
    suspend fun getAttachmentsForTransaction(transactionId: Long): List<AttachmentUiModel> {
        return transactionAttachmentRepository.getAttachmentsForTransactionSync(transactionId)
            .map { entity ->
                AttachmentUiModel(
                    id = entity.id,
                    filePath = entity.filePath,
                    fileName = entity.fileName,
                    mimeType = entity.mimeType,
                    isNew = false
                )
            }
    }

    /**
     * Get attachments as entities for the AttachmentViewerScreen
     */
    suspend fun getAttachmentEntities(transactionId: Long): List<TransactionAttachmentEntity> {
        return transactionAttachmentRepository.getAttachmentsForTransactionSync(transactionId)
    }
}
