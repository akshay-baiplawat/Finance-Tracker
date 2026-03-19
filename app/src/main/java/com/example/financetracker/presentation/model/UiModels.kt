package com.example.financetracker.presentation.model

data class TransactionUiModel(
    val id: Long,
    val title: String, // merchant/note
    val amount: Long, // Raw
    val amountFormatted: String, // "₹ 500"
    val amountColor: Int, // ColorRes
    val date: java.time.LocalDate, // Raw
    val dateFormatted: String, // "12 Dec"
    val category: String,
    val categoryIconId: Int = 0,
    val categoryColorHex: String = "#EF4444",
    val type: String,
    val merchant: String = "",
    val timestamp: Long = 0L,
    val walletId: Long = 0L,
    val smsId: Long? = null,
    val personId: Long? = null,
    val groupId: Long? = null,
    val attachmentCount: Int = 0
)

data class AccountUiModel(
    val id: Long,
    val name: String,
    val balanceFormatted: String,
    val rawBalance: Long, // Added for calculation
    val type: String, // BANK, CARD, CASH
    val color: androidx.compose.ui.graphics.Color
)

data class MonthlyReport(
    val totalIncome: Long,
    val totalExpense: Long,
    val netSavings: Long
)

data class PersonUiModel(
    val id: Long,
    val name: String,
    val balanceFormatted: String,
    val balance: Long, // Raw value for logic
    val statusText: String,
    val statusColor: androidx.compose.ui.graphics.Color
)

// UI States

data class DashboardUiState(
    val totalNetWorth: String = "₹ 0.00",
    val netWorthChangePercentage: Double = 0.0,
    val incomeThisMonth: String = "₹ 0.00",
    val expenseThisMonth: String = "₹ 0.00",
    val income: Long = 0L,
    val expense: Long = 0L,
    val wallets: List<AccountUiModel> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFilter: String = "All",
    val isTripModeActive: Boolean = false,
    val currencySymbol: String = "₹",
    val userName: String = "",
    val userImagePath: String? = null,
    val unreadNotificationCount: Int = 0,
    val error: String? = null
)

data class WalletUiState(
    val assetWallets: List<AccountUiModel> = emptyList(), // Bank, Cash
    val liabilityWallets: List<AccountUiModel> = emptyList(), // Credit Cards
    val people: List<PersonUiModel> = emptyList(), // Debts
    val totalAssets: String = "",
    val totalLiabilities: String = "",
    val totalOwedToYouFormatted: String = "",
    val totalOwedByYouFormatted: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class InsightsUiState(
    val totalSpentThisMonth: String = "₹ 0.00",
    val totalIncomeThisMonth: String = "₹ 0.00",
    val categoryBreakdown: List<CategoryBreakdownUiModel> = emptyList(),
    val incomeBreakdown: List<CategoryBreakdownUiModel> = emptyList(),
    val budgetStatus: List<BudgetUiModel> = emptyList(),
    val upcomingBills: List<SubscriptionUiModel> = emptyList(),
    val totalUpcomingBillsFormatted: String = "",
    val categories: List<com.example.financetracker.data.local.entity.CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class CategoryBreakdownUiModel(
    val categoryName: String,
    val amount: String,
    val percentage: Float, // 0.0 to 100.0
    val colorHex: String,
    val iconId: Int = 0
)

data class BudgetUiModel(
    val id: Long,
    val categoryName: String,
    val spentFormatted: String,
    val limitFormatted: String,
    val remainingFormatted: String,
    val rolloverEnabled: Boolean,
    val spentAmount: Long, // Raw
    val limitAmount: Long, // Raw
    val progress: Float, // 0.0 to 1.0
    val colorHex: String,
    val isOverBudget: Boolean,
    val isWarning: Boolean
)

data class SubscriptionUiModel(
    val id: Long = 0, // 0 for detected
    val merchantName: String,
    val amount: Long,
    val amountFormatted: String,
    val frequency: String,
    val nextDueDate: Long,
    val nextDueDateFormatted: String,
    val colorHex: String,
    val isDetected: Boolean,
    val probability: Float? = null // only for detected
)

data class EventUiModel(
    val id: Long, // Changed to Long for DB compatibility
    val name: String,
    val targetAmount: Long,
    val targetAmountFormatted: String,
    val currentAmount: Long,
    val currentAmountFormatted: String,
    val date: java.time.LocalDate
)

// ==================== GROUP UI MODELS ====================

/**
 * UI model for displaying a group in the list
 */
data class GroupUiModel(
    val id: Long,
    val name: String,
    val type: String, // "TRIP", "SPLIT_EXPENSE", "SAVINGS_GOAL", "GENERAL"
    val memberCount: Int,
    val totalSpent: Long,
    val totalSpentFormatted: String,
    val budgetLimit: Long,
    val budgetLimitFormatted: String?,
    val targetAmount: Long,
    val targetAmountFormatted: String?,
    val totalContributed: Long,
    val totalContributedFormatted: String?,
    val progressPercentage: Float, // 0.0 to 1.0
    val colorHex: String,
    val iconId: Int,
    val isActive: Boolean,
    val yourBalance: Long, // Positive = owed to you, Negative = you owe
    val yourBalanceFormatted: String,
    val yourBalanceSign: Int, // -1 = you owe, 0 = settled, 1 = owed to you
    val createdTimestamp: Long,
    // Filter fields for "Group By Category" type
    val filterCategoryIds: String? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null
)

/**
 * Detailed UI model for a single group view
 */
data class GroupDetailUiModel(
    val group: GroupUiModel,
    val members: List<GroupMemberUiModel>,
    val recentExpenses: List<GroupExpenseUiModel>,
    val settlementSuggestions: List<SettlementSuggestion>,
    val settlementHistory: List<SettlementHistoryUiModel>,
    val contributions: List<ContributionUiModel>,
    val tagBreakdown: List<TagBreakdownUiModel>
)

/**
 * UI model for a group member with balance info
 */
data class GroupMemberUiModel(
    val personId: Long?,
    val name: String,
    val isSelf: Boolean,
    val balance: Long, // Positive = paid more than share (owed), Negative = owes
    val balanceFormatted: String,
    val balanceSign: Int, // -1 = owes group, 0 = settled, 1 = owed by group
    val totalPaid: Long,
    val totalPaidFormatted: String
)

/**
 * UI model for a group expense
 */
data class GroupExpenseUiModel(
    val id: Long, // GroupTransaction ID
    val transactionId: Long,
    val amount: Long,
    val amountFormatted: String,
    val note: String,
    val paidByPersonId: Long?,
    val paidByName: String,
    val paidBySelf: Boolean,
    val splitType: String,
    val tag: String,
    val transactionType: String = "EXPENSE",
    val categoryName: String = "Uncategorized",
    val categoryColorHex: String = "#EF4444",
    val categoryIconId: Int = 0,
    val timestamp: Long,
    val dateFormatted: String,
    val yourShare: Long,
    val yourShareFormatted: String,
    val attachmentCount: Int = 0
)

/**
 * Settlement suggestion between two members
 */
data class SettlementSuggestion(
    val fromPersonId: Long?,
    val fromName: String,
    val fromIsSelf: Boolean,
    val toPersonId: Long?,
    val toName: String,
    val toIsSelf: Boolean,
    val amount: Long,
    val amountFormatted: String
)

/**
 * UI model for settlement history record
 */
data class SettlementHistoryUiModel(
    val id: Long,
    val fromPersonId: Long?,
    val fromName: String,
    val fromIsSelf: Boolean,
    val toPersonId: Long?,
    val toName: String,
    val toIsSelf: Boolean,
    val amount: Long,
    val amountFormatted: String,
    val timestamp: Long,
    val dateFormatted: String,
    val note: String
)

/**
 * UI model for a savings contribution
 */
data class ContributionUiModel(
    val id: Long,
    val memberId: Long?,
    val memberName: String,
    val isSelf: Boolean,
    val amount: Long,
    val amountFormatted: String,
    val timestamp: Long,
    val dateFormatted: String,
    val note: String
)

/**
 * Savings progress for a group
 */
data class SavingsProgressUiModel(
    val currentAmount: Long,
    val currentAmountFormatted: String,
    val targetAmount: Long,
    val targetAmountFormatted: String,
    val progressPercentage: Float, // 0.0 to 1.0
    val memberContributions: List<MemberContributionSummary>
)

/**
 * Summary of a member's contributions
 */
data class MemberContributionSummary(
    val memberId: Long?,
    val memberName: String,
    val isSelf: Boolean,
    val totalContributed: Long,
    val totalContributedFormatted: String,
    val contributionPercentage: Float // 0.0 to 1.0
)

/**
 * Expense breakdown by tag
 */
data class TagBreakdownUiModel(
    val tag: String,
    val total: Long,
    val totalFormatted: String,
    val percentage: Float, // 0.0 to 1.0
    val colorHex: String
)

/**
 * Statistics for a group
 */
data class GroupStatsUiModel(
    val totalSpent: Long,
    val totalSpentFormatted: String,
    val averagePerExpense: Long,
    val averagePerExpenseFormatted: String,
    val expenseCount: Int,
    val topSpenderName: String,
    val topSpenderAmount: Long,
    val topSpenderAmountFormatted: String,
    val mostCommonTag: String
)

// ==================== SCREEN UI STATES ====================

/**
 * UI state for CategoryListScreen
 */
data class CategoryUiState(
    val expenseCategories: List<com.example.financetracker.data.local.entity.CategoryEntity> = emptyList(),
    val incomeCategories: List<com.example.financetracker.data.local.entity.CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * UI state for NotificationScreen
 */
data class NotificationUiState(
    val notifications: List<NotificationUiModel> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * UI model for notifications
 */
data class NotificationUiModel(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val referenceId: Long?,
    val referenceType: String?,
    val isRead: Boolean,
    val timestamp: Long,
    val timeAgo: String
)
