package com.example.financetracker.domain.repository

import com.example.financetracker.data.local.entity.GroupContributionEntity
import com.example.financetracker.data.local.entity.GroupEntity
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.SettlementEntity
import com.example.financetracker.domain.model.TransactionWithCategory
import com.example.financetracker.presentation.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Input data for split amounts per member
 */
data class SplitInput(
    val memberId: Long?,
    val isSelf: Boolean,
    val amount: Long
)

/**
 * Repository interface for Group operations
 */
interface GroupRepository {

    // ==================== GROUPS ====================

    /**
     * Get all groups with stats (member count, total spent, contributions)
     */
    fun getAllGroups(): Flow<List<GroupUiModel>>

    /**
     * Get groups filtered by type
     */
    fun getGroupsByType(type: String): Flow<List<GroupUiModel>>

    /**
     * Get detailed information for a specific group
     */
    fun getGroupDetail(groupId: Long): Flow<GroupDetailUiModel?>

    /**
     * Get raw group entity by ID
     */
    suspend fun getGroupById(groupId: Long): GroupEntity?

    /**
     * Create a new group
     * @return The ID of the created group
     */
    suspend fun createGroup(
        name: String,
        type: String,
        budgetLimit: Long = 0L,
        targetAmount: Long = 0L,
        description: String = "",
        colorHex: String = "#2196F3",
        iconId: Int = 0,
        filterCategoryIds: String? = null,
        filterStartDate: Long? = null,
        filterEndDate: Long? = null
    ): Long

    /**
     * Update an existing group
     */
    suspend fun updateGroup(
        id: Long,
        name: String,
        budgetLimit: Long,
        targetAmount: Long,
        description: String,
        colorHex: String,
        iconId: Int
    )

    /**
     * Delete a group and all related data
     */
    suspend fun deleteGroup(id: Long)

    /**
     * Archive a group (set isActive = false)
     */
    suspend fun archiveGroup(id: Long)

    // ==================== MEMBERS ====================

    /**
     * Add a person as a member to a group
     */
    suspend fun addMember(groupId: Long, personId: Long)

    /**
     * Add multiple members to a group
     */
    suspend fun addMembers(groupId: Long, personIds: List<Long>)

    /**
     * Remove a member from a group
     */
    suspend fun removeMember(groupId: Long, personId: Long)

    /**
     * Get all members of a group (excluding current user)
     */
    fun getGroupMembers(groupId: Long): Flow<List<PersonEntity>>

    /**
     * Get member count for a group (including current user)
     */
    suspend fun getGroupMemberCount(groupId: Long): Int

    // ==================== EXPENSE SPLITTING ====================

    /**
     * Add an expense to a group with split configuration
     * @return The transaction ID of the created expense (for adding attachments)
     */
    suspend fun addGroupExpense(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        paidByPersonId: Long?,
        paidBySelf: Boolean,
        splitType: String,
        splits: List<SplitInput>,
        tag: String,
        walletId: Long,
        timestamp: Long = System.currentTimeMillis()
    ): Long

    /**
     * Delete a group expense and revert balances
     */
    suspend fun deleteGroupExpense(groupTransactionId: Long)

    /**
     * Update a group expense (for SPLIT_EXPENSE groups)
     * Amount is editable - splits will be automatically recalculated
     * Wallet balance is adjusted if amount changes and user paid
     * @return The underlying transaction ID (for adding attachments)
     */
    suspend fun updateGroupExpense(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        timestamp: Long
    ): Long

    /**
     * Update a simple group transaction (for GENERAL groups)
     * All fields are editable since no wallet/balance impact
     * @return The underlying transaction ID (for adding attachments)
     */
    suspend fun updateSimpleGroupTransaction(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        transactionType: String,
        walletId: Long,
        timestamp: Long
    ): Long

    /**
     * Add a simple transaction to a GENERAL group (no wallet deduction, no splits)
     * Used for record-only transactions in General groups
     * @return The transaction ID of the created transaction (for adding attachments)
     */
    suspend fun addSimpleGroupTransaction(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        transactionType: String,  // "EXPENSE" or "INCOME"
        timestamp: Long = System.currentTimeMillis()
    ): Long

    /**
     * Get all expenses for a group with details
     */
    fun getGroupExpenses(groupId: Long): Flow<List<GroupExpenseUiModel>>

    /**
     * Get total amount spent in a group
     */
    fun getGroupTotalSpent(groupId: Long): Flow<Long>

    // ==================== SETTLEMENTS ====================

    /**
     * Calculate optimal settlements for a group
     * Uses greedy algorithm to minimize number of transactions
     */
    fun calculateSettlements(groupId: Long): Flow<List<SettlementSuggestion>>

    /**
     * Record a settlement between members
     * @return The ID of the created settlement (for undo functionality)
     */
    suspend fun recordSettlement(
        groupId: Long,
        fromPersonId: Long?,
        fromSelf: Boolean,
        toPersonId: Long?,
        toSelf: Boolean,
        amount: Long,
        note: String
    ): Long

    /**
     * Get all settlements for a group
     */
    fun getSettlements(groupId: Long): Flow<List<SettlementEntity>>

    /**
     * Delete a settlement record
     */
    suspend fun deleteSettlement(settlementId: Long)

    // ==================== SAVINGS GOALS ====================

    /**
     * Add a contribution to a savings goal
     */
    suspend fun addContribution(
        groupId: Long,
        amount: Long,
        memberId: Long?,
        isSelf: Boolean,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get all contributions for a group
     */
    fun getContributions(groupId: Long): Flow<List<GroupContributionEntity>>

    /**
     * Get savings progress for a group
     */
    fun getSavingsProgress(groupId: Long): Flow<SavingsProgressUiModel>

    /**
     * Delete a contribution
     */
    suspend fun deleteContribution(contributionId: Long)

    /**
     * Update a contribution in a savings goal
     */
    suspend fun updateContribution(
        contributionId: Long,
        amount: Long,
        note: String,
        timestamp: Long
    )

    // ==================== ANALYTICS ====================

    /**
     * Get expense breakdown by tag for a group
     */
    fun getExpensesByTag(groupId: Long): Flow<List<TagBreakdownUiModel>>

    /**
     * Get statistics for a group
     */
    fun getGroupStats(groupId: Long): Flow<GroupStatsUiModel>

    // ==================== CATEGORY FILTER GROUPS ====================

    /**
     * Get transactions filtered by categories and date range for "Group By Category" type
     * @param categoryIds List of category IDs to filter by
     * @param startDate Start timestamp, null means oldest transaction
     * @param endDate End timestamp, null means current time
     */
    fun getFilteredTransactions(
        categoryIds: List<Long>,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<TransactionWithCategory>>

    /**
     * Get the oldest transaction timestamp in the database
     */
    suspend fun getOldestTransactionTimestamp(): Long?
}
