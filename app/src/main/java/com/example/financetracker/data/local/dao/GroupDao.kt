package com.example.financetracker.data.local.dao

import androidx.room.*
import com.example.financetracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Data class for group with calculated stats
 */
data class GroupWithStats(
    @Embedded val group: GroupEntity,
    val memberCount: Int,
    val totalSpent: Long,
    val totalContributed: Long
)

/**
 * Data class for member balance calculation
 */
data class MemberBalanceResult(
    val memberId: Long?,
    val isSelfUser: Boolean,
    val totalPaid: Long,
    val totalOwes: Long
)

/**
 * Data class for group transaction with transaction details
 */
data class GroupTransactionWithDetails(
    @Embedded val groupTransaction: GroupTransactionEntity,
    val transactionAmount: Long,
    val transactionNote: String,
    val transactionTimestamp: Long,
    val transactionType: String?,
    val paidByName: String?,
    val categoryName: String?,
    val categoryColorHex: String?,
    val categoryIconId: Int?
)

@Dao
interface GroupDao {

    // ==================== GROUP CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query("SELECT * FROM groups ORDER BY createdTimestamp DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY createdTimestamp DESC")
    fun getActiveGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE type = :type ORDER BY createdTimestamp DESC")
    fun getGroupsByType(type: String): Flow<List<GroupEntity>>

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("UPDATE groups SET isActive = :isActive WHERE id = :id")
    suspend fun setGroupActive(id: Long, isActive: Boolean)

    // Get all groups with stats (member count, total spent, total contributions)
    // For GENERAL groups, totalSpent = income - expense (net value) from group_transactions
    // For TRIP groups, totalSpent = sum from main transactions filtered by category
    // For other groups, totalSpent = sum of all transaction amounts from group_transactions
    @Query("""
        SELECT g.*,
            (SELECT COUNT(*) FROM group_members gm WHERE gm.groupId = g.id) + 1 as memberCount,
            CASE
                WHEN g.type = 'GENERAL' THEN
                    COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                        FROM transactions t
                        INNER JOIN group_transactions gt ON t.id = gt.transactionId
                        WHERE gt.groupId = g.id), 0)
                WHEN g.type = 'TRIP' THEN
                    COALESCE((SELECT SUM(t.amount)
                        FROM transactions t
                        INNER JOIN categories c ON c.name = t.categoryName AND c.type = t.type
                        WHERE (',' || g.filter_category_ids || ',') LIKE ('%,' || c.id || ',%')
                        AND (g.filter_start_date IS NULL OR t.timestamp >= g.filter_start_date)
                        AND (g.filter_end_date IS NULL OR t.timestamp <= g.filter_end_date)
                        AND t.type = 'EXPENSE'), 0)
                ELSE
                    COALESCE((SELECT SUM(t.amount) FROM transactions t
                        INNER JOIN group_transactions gt ON t.id = gt.transactionId
                        WHERE gt.groupId = g.id), 0)
            END as totalSpent,
            COALESCE((SELECT SUM(gc.amount) FROM group_contributions gc
                WHERE gc.groupId = g.id), 0) as totalContributed
        FROM groups g
        ORDER BY g.createdTimestamp DESC
    """)
    fun getAllGroupsWithStats(): Flow<List<GroupWithStats>>

    // ==================== GROUP MEMBERS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMemberEntity>)

    @Query("SELECT p.* FROM people p INNER JOIN group_members gm ON p.id = gm.memberId WHERE gm.groupId = :groupId")
    fun getGroupMembers(groupId: Long): Flow<List<PersonEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getGroupMemberEntities(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND memberId = :memberId")
    suspend fun removeGroupMember(groupId: Long, memberId: Long)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun removeAllGroupMembers(groupId: Long)

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    suspend fun getGroupMemberCount(groupId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM group_members WHERE groupId = :groupId AND memberId = :memberId)")
    suspend fun isMemberOfGroup(groupId: Long, memberId: Long): Boolean

    // ==================== GROUP TRANSACTIONS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupTransaction(groupTransaction: GroupTransactionEntity): Long

    @Update
    suspend fun updateGroupTransaction(groupTransaction: GroupTransactionEntity)

    @Query("SELECT * FROM group_transactions WHERE id = :id")
    suspend fun getGroupTransactionById(id: Long): GroupTransactionEntity?

    @Query("SELECT * FROM group_transactions WHERE groupId = :groupId ORDER BY id DESC")
    fun getGroupTransactions(groupId: Long): Flow<List<GroupTransactionEntity>>

    @Query("""
        SELECT gt.*, t.amount as transactionAmount, t.note as transactionNote,
               t.timestamp as transactionTimestamp, t.type as transactionType,
               p.name as paidByName,
               t.categoryName as categoryName, c.colorHex as categoryColorHex,
               c.iconId as categoryIconId
        FROM group_transactions gt
        INNER JOIN transactions t ON t.id = gt.transactionId
        LEFT JOIN people p ON p.id = gt.paidByPersonId
        LEFT JOIN categories c ON c.name = t.categoryName AND c.type = t.type
        WHERE gt.groupId = :groupId
        ORDER BY t.timestamp DESC
    """)
    fun getGroupTransactionsWithDetails(groupId: Long): Flow<List<GroupTransactionWithDetails>>

    @Query("DELETE FROM group_transactions WHERE id = :id")
    suspend fun deleteGroupTransaction(id: Long)

    @Query("DELETE FROM group_transactions WHERE transactionId = :transactionId")
    suspend fun deleteGroupTransactionByTransactionId(transactionId: Long)

    // Total spent in group
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM transactions t
        INNER JOIN group_transactions gt ON t.id = gt.transactionId
        WHERE gt.groupId = :groupId
    """)
    fun getGroupTotalSpent(groupId: Long): Flow<Long>

    // Get distinct tags used in a group
    @Query("SELECT DISTINCT tag FROM group_transactions WHERE groupId = :groupId AND tag != ''")
    fun getGroupTags(groupId: Long): Flow<List<String>>

    // ==================== GROUP SPLITS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: GroupSplitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<GroupSplitEntity>)

    @Query("SELECT * FROM group_splits WHERE groupTransactionId = :groupTransactionId")
    fun getSplitsForTransaction(groupTransactionId: Long): Flow<List<GroupSplitEntity>>

    @Query("SELECT * FROM group_splits WHERE groupTransactionId = :groupTransactionId")
    suspend fun getSplitsForTransactionSync(groupTransactionId: Long): List<GroupSplitEntity>

    @Query("UPDATE group_splits SET isPaid = 1 WHERE id = :splitId")
    suspend fun markSplitAsPaid(splitId: Long)

    @Query("UPDATE group_splits SET isPaid = 1 WHERE groupTransactionId = :groupTransactionId AND memberId = :memberId")
    suspend fun markMemberSplitAsPaid(groupTransactionId: Long, memberId: Long?)

    @Query("DELETE FROM group_splits WHERE groupTransactionId = :groupTransactionId")
    suspend fun deleteSplitsForTransaction(groupTransactionId: Long)

    // Get user's share of a specific transaction
    @Query("SELECT * FROM group_splits WHERE groupTransactionId = :groupTransactionId AND isSelfUser = 1 LIMIT 1")
    suspend fun getUserSplitForTransaction(groupTransactionId: Long): GroupSplitEntity?

    // Get user's share as Flow
    @Query("SELECT * FROM group_splits WHERE groupTransactionId = :groupTransactionId AND isSelfUser = 1 LIMIT 1")
    fun getUserSplitForTransactionFlow(groupTransactionId: Long): Flow<GroupSplitEntity?>

    // ==================== BALANCE CALCULATIONS ====================

    /**
     * Calculate member balances for a group.
     * Balance = totalPaid - totalOwes
     * totalPaid = amount paid for expenses + settlements received
     * totalOwes = share of expenses - settlements paid
     * Positive = owed money (paid more than share)
     * Negative = owes money (paid less than share)
     */
    @Query("""
        SELECT
            gm.memberId,
            0 as isSelfUser,
            COALESCE((
                SELECT SUM(t.amount) FROM transactions t
                INNER JOIN group_transactions gt ON t.id = gt.transactionId
                WHERE gt.groupId = :groupId AND gt.paidByPersonId = gm.memberId
            ), 0) + COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.toPersonId = gm.memberId
            ), 0) as totalPaid,
            COALESCE((
                SELECT SUM(gs.shareAmount) FROM group_splits gs
                INNER JOIN group_transactions gt ON gs.groupTransactionId = gt.id
                WHERE gt.groupId = :groupId AND gs.memberId = gm.memberId
            ), 0) - COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.fromPersonId = gm.memberId
            ), 0) as totalOwes
        FROM group_members gm
        WHERE gm.groupId = :groupId
    """)
    suspend fun getMemberBalances(groupId: Long): List<MemberBalanceResult>

    /**
     * Get member balances as Flow
     */
    @Query("""
        SELECT
            gm.memberId,
            0 as isSelfUser,
            COALESCE((
                SELECT SUM(t.amount) FROM transactions t
                INNER JOIN group_transactions gt ON t.id = gt.transactionId
                WHERE gt.groupId = :groupId AND gt.paidByPersonId = gm.memberId
            ), 0) + COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.toPersonId = gm.memberId
            ), 0) as totalPaid,
            COALESCE((
                SELECT SUM(gs.shareAmount) FROM group_splits gs
                INNER JOIN group_transactions gt ON gs.groupTransactionId = gt.id
                WHERE gt.groupId = :groupId AND gs.memberId = gm.memberId
            ), 0) - COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.fromPersonId = gm.memberId
            ), 0) as totalOwes
        FROM group_members gm
        WHERE gm.groupId = :groupId
    """)
    fun getMemberBalancesFlow(groupId: Long): Flow<List<MemberBalanceResult>>

    /**
     * Get current user's balance for a group
     */
    @Query("""
        SELECT
            NULL as memberId,
            1 as isSelfUser,
            COALESCE((
                SELECT SUM(t.amount) FROM transactions t
                INNER JOIN group_transactions gt ON t.id = gt.transactionId
                WHERE gt.groupId = :groupId AND gt.paidByUserSelf = 1
            ), 0) + COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.toSelfUser = 1
            ), 0) as totalPaid,
            COALESCE((
                SELECT SUM(gs.shareAmount) FROM group_splits gs
                INNER JOIN group_transactions gt ON gs.groupTransactionId = gt.id
                WHERE gt.groupId = :groupId AND gs.isSelfUser = 1
            ), 0) - COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.fromSelfUser = 1
            ), 0) as totalOwes
    """)
    suspend fun getUserBalance(groupId: Long): MemberBalanceResult

    /**
     * Get current user's balance as Flow
     */
    @Query("""
        SELECT
            NULL as memberId,
            1 as isSelfUser,
            COALESCE((
                SELECT SUM(t.amount) FROM transactions t
                INNER JOIN group_transactions gt ON t.id = gt.transactionId
                WHERE gt.groupId = :groupId AND gt.paidByUserSelf = 1
            ), 0) + COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.toSelfUser = 1
            ), 0) as totalPaid,
            COALESCE((
                SELECT SUM(gs.shareAmount) FROM group_splits gs
                INNER JOIN group_transactions gt ON gs.groupTransactionId = gt.id
                WHERE gt.groupId = :groupId AND gs.isSelfUser = 1
            ), 0) - COALESCE((
                SELECT SUM(s.amount) FROM settlements s
                WHERE s.groupId = :groupId AND s.fromSelfUser = 1
            ), 0) as totalOwes
    """)
    fun getUserBalanceFlow(groupId: Long): Flow<MemberBalanceResult>

    // ==================== CONTRIBUTIONS (SAVINGS GOALS) ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GroupContributionEntity): Long

    @Query("SELECT * FROM group_contributions WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getGroupContributions(groupId: Long): Flow<List<GroupContributionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM group_contributions WHERE groupId = :groupId")
    fun getTotalContributions(groupId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM group_contributions WHERE groupId = :groupId AND memberId = :memberId")
    fun getMemberContributions(groupId: Long, memberId: Long?): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM group_contributions WHERE groupId = :groupId AND isSelfUser = 1")
    fun getUserContributions(groupId: Long): Flow<Long>

    @Query("SELECT * FROM group_contributions WHERE id = :id")
    suspend fun getContributionById(id: Long): GroupContributionEntity?

    @Update
    suspend fun updateContribution(contribution: GroupContributionEntity)

    @Query("DELETE FROM group_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    // ==================== SETTLEMENTS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getSettlements(groupId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun getSettlementById(id: Long): SettlementEntity?

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlement(id: Long)

    // Total settlements received by user in a group
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM settlements
        WHERE groupId = :groupId AND toSelfUser = 1
    """)
    fun getTotalSettlementsReceived(groupId: Long): Flow<Long>

    // Total settlements paid by user in a group
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM settlements
        WHERE groupId = :groupId AND fromSelfUser = 1
    """)
    fun getTotalSettlementsPaid(groupId: Long): Flow<Long>

    // ==================== BULK OPERATIONS ====================

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM group_members")
    suspend fun deleteAllGroupMembers()

    @Query("DELETE FROM group_transactions")
    suspend fun deleteAllGroupTransactions()

    @Query("DELETE FROM group_splits")
    suspend fun deleteAllGroupSplits()

    @Query("DELETE FROM group_contributions")
    suspend fun deleteAllGroupContributions()

    @Query("DELETE FROM settlements")
    suspend fun deleteAllSettlements()
}
