package com.example.financetracker.presentation.insights

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.local.mapper.formatAsCurrency
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.presentation.model.BudgetUiModel
import com.example.financetracker.presentation.model.CategoryBreakdownUiModel
import com.example.financetracker.presentation.model.InsightsUiState
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
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.toArgb

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val subscriptionLogic: com.example.financetracker.domain.logic.SubscriptionLogic,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    // Tab state persistence - survives navigation and process death
    val selectedTab: StateFlow<Int> = savedStateHandle.getStateFlow("insights_tab", 0)

    fun selectTab(index: Int) {
        savedStateHandle["insights_tab"] = index
    }

    fun resetTab() {
        savedStateHandle["insights_tab"] = 0
    }

    init {
        // Ensure default categories exist for budget dropdown
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
        // Generate notifications on screen load
        viewModelScope.launch {
            notificationRepository.generateNotifications()
        }
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        combine(repository.getCategoryBreakdown(), repository.getIncomeBreakdown()) { exp, inc -> Pair(exp, inc) },
        combine(repository.getAllBudgets(), repository.getMonthlyCashFlow(
            java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            java.time.LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )) { budgets, report -> Pair(budgets, report) },
        combine(repository.getAllTransactions(), repository.getAllSubscriptions(), repository.getAllCategories()) { tx, sub, cat -> Triple(tx, sub, cat) },
        userPreferencesRepository.currency,
        _error
    ) { (breakdown, incomeData), (budgets, report), (allTransactions, persistentSubscriptions, categories), currency, error ->
        val income = report.totalIncome
        
        // Subscriptions Logic
        val detected = subscriptionLogic.detectSubscriptions(allTransactions)
        val mergedSubscriptions = mutableListOf<com.example.financetracker.presentation.model.SubscriptionUiModel>()
        
        // 1. Add Persistent
        mergedSubscriptions.addAll(persistentSubscriptions.map { entity ->
            val date = java.time.Instant.ofEpochMilli(entity.nextDueDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
            
            com.example.financetracker.presentation.model.SubscriptionUiModel(
                id = entity.id,
                merchantName = entity.merchantName,
                amount = entity.amount,
                amountFormatted = CurrencyFormatter.format(entity.amount, currency),
                frequency = entity.frequency,
                nextDueDate = entity.nextDueDate,
                nextDueDateFormatted = date.format(formatter),
                colorHex = entity.colorHex,
                isDetected = false
            )
        })
        
        // 2. Add Detected (deduplicated)
        detected.forEach { ds ->
            val exists = persistentSubscriptions.any { ps ->
                ps.merchantName.equals(ds.merchant, ignoreCase = true)
            }
            
            if (!exists) {
                val date = java.time.Instant.ofEpochMilli(ds.nextDueDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
                
                mergedSubscriptions.add(
                    com.example.financetracker.presentation.model.SubscriptionUiModel(
                        id = 0,
                        merchantName = ds.merchant,
                        amount = ds.amount,
                        amountFormatted = CurrencyFormatter.format(ds.amount, currency),
                        frequency = ds.frequency,
                        nextDueDate = ds.nextDueDate,
                        nextDueDateFormatted = date.format(formatter),
                        colorHex = "#808080",
                        isDetected = true,
                        probability = ds.probability
                    )
                )
            }
        }
        
        val subscriptions = mergedSubscriptions.sortedBy { it.nextDueDate }
        
        // 1. Calculate Total Spent
        val totalSpent = breakdown.sumOf { it.total }
        
        // Create category map for color lookup
        val categoryMap = categories.associateBy { it.name }

        // 2. Map Categorized Breakdown (Expense)
        val breakdownUiModels = breakdown.map { tuple ->
            val percentage = if (totalSpent > 0) (tuple.total.toFloat() / totalSpent.toFloat()) * 100 else 0f
            // Use actual category color, fallback to budget color, then default grey
            val categoryEntity = categoryMap[tuple.categoryName]
            val resolvedColor = categoryEntity?.colorHex
                ?: budgets.find { it.categoryName == tuple.categoryName }?.colorHex
                ?: "#808080"

            CategoryBreakdownUiModel(
                categoryName = tuple.categoryName,
                amount = CurrencyFormatter.format(tuple.total, currency),
                percentage = percentage,
                colorHex = resolvedColor
            )
        }.sortedByDescending { it.percentage }

        // 2.1 Map Income Breakdown
        val totalIncomeVal = incomeData.sumOf { it.total }
        val incomeBreakdownUiModels = incomeData.map { tuple ->
             val percentage = if (totalIncomeVal > 0) (tuple.total.toFloat() / totalIncomeVal.toFloat()) * 100 else 0f
             // Use actual category color from categories
             val categoryEntity = categoryMap[tuple.categoryName]
             val resolvedColor = categoryEntity?.colorHex ?: "#10B981" // Fallback to Mint

             CategoryBreakdownUiModel(
                categoryName = tuple.categoryName,
                amount = CurrencyFormatter.format(tuple.total, currency),
                percentage = percentage,
                colorHex = resolvedColor
             )
        }.sortedByDescending { it.percentage }

        val currentMonthStart = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 3. Map Budgets
        val budgetUiModels = budgets.map { budget ->
            // Find spending for CURRENT month
            val currentSpent = if (budget.categoryName == "All Expenses") {
                allTransactions.filter { 
                    it.type == "EXPENSE" && 
                    it.category != "Money Transfer" &&
                    it.timestamp >= currentMonthStart
                }.sumOf { it.amount }
            } else {
                breakdown.find { it.categoryName == budget.categoryName }?.total ?: 0L
            }
            
            var effectiveLimit = budget.limitAmount
            
            if (budget.rolloverEnabled) {
                val budgetStart = java.time.Instant.ofEpochMilli(budget.createdTimestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .withDayOfMonth(1)
                
                val currentMonthDate = java.time.LocalDate.now().withDayOfMonth(1)
                val monthsPassed = java.time.temporal.ChronoUnit.MONTHS.between(budgetStart, currentMonthDate)
                
                if (monthsPassed > 0) {
                    val totalPastBudget = monthsPassed * budget.limitAmount
                    val pastSpent = if (budget.categoryName == "All Expenses") {
                         allTransactions
                            .filter { 
                                it.type == "EXPENSE" && 
                                it.category != "Money Transfer" &&
                                it.timestamp < currentMonthStart &&
                                it.timestamp >= budget.createdTimestamp
                            }
                            .sumOf { it.amount }
                    } else {
                        allTransactions
                            .filter { 
                                it.category == budget.categoryName && 
                                it.timestamp < currentMonthStart &&
                                it.timestamp >= budget.createdTimestamp
                            }
                            .sumOf { it.amount }
                    }
                    val rolloverAmount = totalPastBudget - pastSpent
                    effectiveLimit += rolloverAmount
                }
            }

            val progress = if (effectiveLimit > 0) currentSpent.toFloat() / effectiveLimit.toFloat() else if (currentSpent > 0) 1.2f else 0f
            
            val colorHex = when {
                progress > 1.0f -> "#B00020" // Error Red
                progress > 0.8f -> "#F57F17" // Orange
                else -> budget.colorHex
            }

            BudgetUiModel(
                id = budget.id,
                categoryName = budget.categoryName,
                spentFormatted = CurrencyFormatter.format(currentSpent, currency),
                limitFormatted = CurrencyFormatter.format(effectiveLimit, currency),
                remainingFormatted = CurrencyFormatter.format(effectiveLimit - currentSpent, currency),
                rolloverEnabled = budget.rolloverEnabled,
                spentAmount = currentSpent,
                limitAmount = effectiveLimit,
                progress = progress,
                colorHex = colorHex,
                isOverBudget = progress > 1.0f,
                isWarning = progress > 0.8f && progress <= 1.0f
            )
        }
        
        val totalBills = subscriptions.sumOf { it.amount }

        InsightsUiState(
            totalSpentThisMonth = CurrencyFormatter.format(totalSpent, currency),
            totalIncomeThisMonth = CurrencyFormatter.format(income, currency),
            categoryBreakdown = breakdownUiModels,
            incomeBreakdown = incomeBreakdownUiModels,
            budgetStatus = budgetUiModels,
            upcomingBills = subscriptions,
            totalUpcomingBillsFormatted = CurrencyFormatter.format(totalBills, currency),
            categories = categories,
            isLoading = false,
            error = error
        )
    }.catch { e ->
        emit(InsightsUiState(
            isLoading = false,
            error = "Failed to load insights: ${e.localizedMessage ?: "Unknown error"}"
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun addBudget(category: String, limit: Long, color: String, rolloverEnabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.addBudget(category, limit, color, rolloverEnabled)
                // Regenerate notifications to check budget thresholds
                notificationRepository.generateNotifications()
            } catch (e: Exception) {
                _error.value = "Failed to add budget: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete budget: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateBudget(id: Long, category: String, limit: Long, color: String, rolloverEnabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateBudget(id, category, limit, color, rolloverEnabled)
                // Regenerate notifications to check budget thresholds
                notificationRepository.generateNotifications()
            } catch (e: Exception) {
                _error.value = "Failed to update budget: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // Subscription Operations
    fun addSubscription(merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String, isAuto: Boolean = false) {
        viewModelScope.launch {
            try {
                repository.addSubscription(merchant, amount, frequency, nextDueDate, color, isAuto)
                // Generate notification for upcoming bill if within 3 days
                notificationRepository.generateNotifications()
            } catch (e: Exception) {
                _error.value = "Failed to add subscription: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSubscription(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete subscription: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateSubscription(id: Long, merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String) {
        viewModelScope.launch {
            try {
                repository.updateSubscription(id, merchant, amount, frequency, nextDueDate, color)
                // Regenerate notifications for updated due date
                notificationRepository.generateNotifications()
            } catch (e: Exception) {
                _error.value = "Failed to update subscription: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
