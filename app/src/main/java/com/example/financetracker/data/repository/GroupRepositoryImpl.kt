package com.example.financetracker.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.core.util.DateUtil
import com.example.financetracker.data.local.AppDatabase
import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.dao.GroupDao
import com.example.financetracker.data.local.dao.MemberBalanceResult
import com.example.financetracker.data.local.dao.TransactionAttachmentDao
import com.example.financetracker.data.local.entity.*
import com.example.financetracker.domain.model.TransactionWithCategory
import com.example.financetracker.domain.repository.GroupRepository
import com.example.financetracker.domain.repository.SplitInput
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.presentation.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import com.example.financetracker.di.ApplicationScope

class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val financeDao: FinanceDao,
    private val transactionAttachmentDao: TransactionAttachmentDao,
    private val database: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : GroupRepository {

    // Cached attachment counts flow for group transactions
    private val cachedAttachmentCounts = transactionAttachmentDao.getAttachmentCounts()
        .map { counts -> counts.associate { it.transactionId to it.count } }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyMap())

    // ==================== GROUPS ====================

    override fun getAllGroups(): Flow<List<GroupUiModel>> {
        return combine(
            groupDao.getAllGroupsWithStats(),
            userPreferencesRepository.currency
        ) { groups, currency ->
            groups.map { groupWithStats ->
                mapToGroupUiModel(groupWithStats, currency)
            }
        }
    }

    override fun getGroupsByType(type: String): Flow<List<GroupUiModel>> {
        return combine(
            groupDao.getGroupsByType(type),
            userPreferencesRepository.currency,
            groupDao.getAllGroupsWithStats()
        ) { groups, currency, allWithStats ->
            val statsMap = allWithStats.associateBy { it.group.id }
            groups.mapNotNull { group ->
                statsMap[group.id]?.let { mapToGroupUiModel(it, currency) }
            }
        }
    }

    override fun getGroupDetail(groupId: Long): Flow<GroupDetailUiModel?> {
        // Use nested combines since standard combine only supports up to 5 flows
        val baseFlows = combine(
            groupDao.getAllGroupsWithStats(),
            groupDao.getGroupMembers(groupId),
            groupDao.getGroupTransactionsWithDetails(groupId),
            financeDao.getAllPeople(),
            userPreferencesRepository.currency
        ) { allGroupsWithStats, members, transactions, allPeople, currency ->
            GroupDetailBaseData(allGroupsWithStats, members, transactions, allPeople, currency)
        }

        val balanceFlows = combine(
            groupDao.getMemberBalancesFlow(groupId),
            groupDao.getUserBalanceFlow(groupId),
            groupDao.getGroupContributions(groupId),
            groupDao.getSettlements(groupId)
        ) { memberBalances, userBalance, contributions, settlements ->
            GroupDetailBalanceData(memberBalances, userBalance, contributions, settlements)
        }

        return combine(baseFlows, balanceFlows, cachedAttachmentCounts) { base, balances, attachmentCounts ->
            val groupWithStats = base.allGroupsWithStats.find { it.group.id == groupId }
                ?: return@combine null

            val groupUiModel = mapToGroupUiModel(groupWithStats, base.currency)
            val peopleMap = base.allPeople.associateBy { it.id }

            // Map members
            val memberUiModels = mutableListOf<GroupMemberUiModel>()

            val isSavingsGoal = groupWithStats.group.type == GroupType.SAVINGS_GOAL.name

            if (isSavingsGoal) {
                // For SAVINGS_GOAL: "Paid" = contributions, no "owes" concept
                val userContributionTotal = balances.contributions
                    .filter { it.isSelfUser }
                    .sumOf { it.amount }

                memberUiModels.add(
                    GroupMemberUiModel(
                        personId = null,
                        name = "You",
                        isSelf = true,
                        balance = userContributionTotal,
                        balanceFormatted = CurrencyFormatter.format(userContributionTotal, base.currency),
                        balanceSign = if (userContributionTotal > 0) 1 else 0,
                        totalPaid = userContributionTotal,
                        totalPaidFormatted = CurrencyFormatter.format(userContributionTotal, base.currency)
                    )
                )

                // Add other members from the group
                base.members.forEach { member ->
                    val memberContributions = balances.contributions
                        .filter { !it.isSelfUser && it.memberId == member.id }
                        .sumOf { it.amount }

                    memberUiModels.add(
                        GroupMemberUiModel(
                            personId = member.id,
                            name = member.name,
                            isSelf = false,
                            balance = memberContributions,
                            balanceFormatted = CurrencyFormatter.format(memberContributions, base.currency),
                            balanceSign = if (memberContributions > 0) 1 else 0,
                            totalPaid = memberContributions,
                            totalPaidFormatted = CurrencyFormatter.format(memberContributions, base.currency)
                        )
                    )
                }
            } else {
                // Existing logic for SPLIT_EXPENSE, TRIP, GENERAL groups
                // Add current user
                val userBalanceValue = balances.userBalance.totalPaid - balances.userBalance.totalOwes
                memberUiModels.add(
                    GroupMemberUiModel(
                        personId = null,
                        name = "You",
                        isSelf = true,
                        balance = userBalanceValue,
                        balanceFormatted = formatBalance(userBalanceValue, base.currency),
                        balanceSign = when {
                            userBalanceValue > 0 -> 1
                            userBalanceValue < 0 -> -1
                            else -> 0
                        },
                        totalPaid = balances.userBalance.totalPaid,
                        totalPaidFormatted = CurrencyFormatter.format(balances.userBalance.totalPaid, base.currency)
                    )
                )

                // Add other members
                balances.memberBalances.forEach { balance ->
                    val person = balance.memberId?.let { peopleMap[it] }
                    val balanceValue = balance.totalPaid - balance.totalOwes
                    memberUiModels.add(
                        GroupMemberUiModel(
                            personId = balance.memberId,
                            name = person?.name ?: "Unknown",
                            isSelf = false,
                            balance = balanceValue,
                            balanceFormatted = formatBalance(balanceValue, base.currency),
                            balanceSign = when {
                                balanceValue > 0 -> 1
                                balanceValue < 0 -> -1
                                else -> 0
                            },
                            totalPaid = balance.totalPaid,
                            totalPaidFormatted = CurrencyFormatter.format(balance.totalPaid, base.currency)
                        )
                    )
                }
            }

            // Map expenses - simplified to avoid suspend calls in lambda
            val expenseUiModels = base.transactions.map { txWithDetails ->
                GroupExpenseUiModel(
                    id = txWithDetails.groupTransaction.id,
                    transactionId = txWithDetails.groupTransaction.transactionId,
                    amount = txWithDetails.transactionAmount,
                    amountFormatted = CurrencyFormatter.format(txWithDetails.transactionAmount, base.currency),
                    note = txWithDetails.transactionNote,
                    paidByPersonId = txWithDetails.groupTransaction.paidByPersonId,
                    paidByName = if (txWithDetails.groupTransaction.paidByUserSelf) "You"
                                 else txWithDetails.paidByName ?: "Unknown",
                    paidBySelf = txWithDetails.groupTransaction.paidByUserSelf,
                    splitType = txWithDetails.groupTransaction.splitType,
                    tag = txWithDetails.groupTransaction.tag,
                    transactionType = txWithDetails.transactionType ?: "EXPENSE",
                    categoryName = txWithDetails.categoryName ?: "Uncategorized",
                    categoryColorHex = txWithDetails.categoryColorHex ?: "#EF4444",
                    categoryIconId = txWithDetails.categoryIconId ?: 0,
                    timestamp = txWithDetails.transactionTimestamp,
                    dateFormatted = DateUtil.formatDateWithTime(txWithDetails.transactionTimestamp, context),
                    yourShare = 0L, // Simplified - share info shown in balances
                    yourShareFormatted = "",
                    attachmentCount = attachmentCounts[txWithDetails.groupTransaction.transactionId] ?: 0
                )
            }

            // For GENERAL groups, recalculate total as net (income - expense)
            val finalGroupUiModel = if (groupWithStats.group.type == GroupType.GENERAL.name) {
                val incomeTotal = expenseUiModels
                    .filter { it.transactionType == "INCOME" }
                    .sumOf { it.amount }
                val expenseTotal = expenseUiModels
                    .filter { it.transactionType == "EXPENSE" }
                    .sumOf { it.amount }
                val netTotal = incomeTotal - expenseTotal
                groupUiModel.copy(
                    totalSpent = netTotal,
                    totalSpentFormatted = (if (netTotal >= 0) "+" else "") + CurrencyFormatter.format(kotlin.math.abs(netTotal), base.currency)
                )
            } else {
                groupUiModel
            }

            // Calculate settlements
            val settlementSuggestions = calculateSettlementSuggestions(
                memberUiModels, peopleMap, base.currency
            )

            // Map contributions
            val contributionUiModels = balances.contributions.map { contribution ->
                val memberName = if (contribution.isSelfUser) "You"
                                 else contribution.memberId?.let { peopleMap[it]?.name } ?: "Unknown"
                ContributionUiModel(
                    id = contribution.id,
                    memberId = contribution.memberId,
                    memberName = memberName,
                    isSelf = contribution.isSelfUser,
                    amount = contribution.amount,
                    amountFormatted = CurrencyFormatter.format(contribution.amount, base.currency),
                    timestamp = contribution.timestamp,
                    dateFormatted = DateUtil.formatDateWithTime(contribution.timestamp, context),
                    note = contribution.note
                )
            }

            // Tag breakdown
            val tagBreakdown = calculateTagBreakdown(base.transactions, base.currency)

            // Map settlement history
            val settlementHistoryUiModels = balances.settlements.map { settlement ->
                val fromName = if (settlement.fromSelfUser) "You"
                               else settlement.fromPersonId?.let { peopleMap[it]?.name } ?: "Unknown"
                val toName = if (settlement.toSelfUser) "You"
                             else settlement.toPersonId?.let { peopleMap[it]?.name } ?: "Unknown"
                SettlementHistoryUiModel(
                    id = settlement.id,
                    fromPersonId = settlement.fromPersonId,
                    fromName = fromName,
                    fromIsSelf = settlement.fromSelfUser,
                    toPersonId = settlement.toPersonId,
                    toName = toName,
                    toIsSelf = settlement.toSelfUser,
                    amount = settlement.amount,
                    amountFormatted = CurrencyFormatter.format(settlement.amount, base.currency),
                    timestamp = settlement.timestamp,
                    dateFormatted = DateUtil.formatDateWithTime(settlement.timestamp, context),
                    note = settlement.note
                )
            }

            GroupDetailUiModel(
                group = finalGroupUiModel,
                members = memberUiModels,
                recentExpenses = expenseUiModels,
                settlementSuggestions = settlementSuggestions,
                settlementHistory = settlementHistoryUiModels,
                contributions = contributionUiModels,
                tagBreakdown = tagBreakdown
            )
        }
    }

    // Helper data classes for combining flows
    private data class GroupDetailBaseData(
        val allGroupsWithStats: List<com.example.financetracker.data.local.dao.GroupWithStats>,
        val members: List<PersonEntity>,
        val transactions: List<com.example.financetracker.data.local.dao.GroupTransactionWithDetails>,
        val allPeople: List<PersonEntity>,
        val currency: String
    )

    private data class GroupDetailBalanceData(
        val memberBalances: List<MemberBalanceResult>,
        val userBalance: MemberBalanceResult,
        val contributions: List<GroupContributionEntity>,
        val settlements: List<SettlementEntity>
    )

    override suspend fun getGroupById(groupId: Long): GroupEntity? {
        return groupDao.getGroupById(groupId)
    }

    override suspend fun createGroup(
        name: String,
        type: String,
        budgetLimit: Long,
        targetAmount: Long,
        description: String,
        colorHex: String,
        iconId: Int,
        filterCategoryIds: String?,
        filterStartDate: Long?,
        filterEndDate: Long?
    ): Long {
        val group = GroupEntity(
            name = name,
            type = type,
            budgetLimit = budgetLimit,
            targetAmount = targetAmount,
            description = description,
            colorHex = colorHex,
            iconId = iconId,
            isActive = true,
            createdTimestamp = System.currentTimeMillis(),
            filterCategoryIds = filterCategoryIds,
            filterStartDate = filterStartDate,
            filterEndDate = filterEndDate
        )
        return groupDao.insertGroup(group)
    }

    override suspend fun updateGroup(
        id: Long,
        name: String,
        budgetLimit: Long,
        targetAmount: Long,
        description: String,
        colorHex: String,
        iconId: Int
    ) {
        val existing = groupDao.getGroupById(id) ?: return
        val updated = existing.copy(
            name = name,
            budgetLimit = budgetLimit,
            targetAmount = targetAmount,
            description = description,
            colorHex = colorHex,
            iconId = iconId
        )
        groupDao.updateGroup(updated)
    }

    override suspend fun deleteGroup(id: Long) {
        groupDao.deleteGroup(id)
    }

    override suspend fun archiveGroup(id: Long) {
        groupDao.setGroupActive(id, false)
    }

    // ==================== MEMBERS ====================

    override suspend fun addMember(groupId: Long, personId: Long) {
        val member = GroupMemberEntity(
            groupId = groupId,
            memberId = personId,
            isOwner = false,
            joinedTimestamp = System.currentTimeMillis()
        )
        groupDao.insertGroupMember(member)
    }

    override suspend fun addMembers(groupId: Long, personIds: List<Long>) {
        val members = personIds.map { personId ->
            GroupMemberEntity(
                groupId = groupId,
                memberId = personId,
                isOwner = false,
                joinedTimestamp = System.currentTimeMillis()
            )
        }
        groupDao.insertGroupMembers(members)
    }

    override suspend fun removeMember(groupId: Long, personId: Long) {
        groupDao.removeGroupMember(groupId, personId)
    }

    override fun getGroupMembers(groupId: Long): Flow<List<PersonEntity>> {
        return groupDao.getGroupMembers(groupId)
    }

    override suspend fun getGroupMemberCount(groupId: Long): Int {
        return groupDao.getGroupMemberCount(groupId) + 1 // +1 for current user
    }

    // ==================== EXPENSE SPLITTING ====================

    override suspend fun addGroupExpense(
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
        timestamp: Long
    ): Long {
        var resultTransactionId = 0L
        database.withTransaction {
            // 1. Create the transaction
            val transaction = TransactionEntity(
                amount = amount,
                note = note,
                timestamp = timestamp,
                type = "EXPENSE",
                categoryName = categoryName,
                merchant = note,
                walletId = walletId,
                groupId = groupId
            )
            val transactionId = financeDao.insertTransactionAndGetId(transaction)
            resultTransactionId = transactionId

            // 2. Update wallet balance
            financeDao.updateWalletBalance(walletId, -amount)

            // 3. Create group transaction
            val groupTransaction = GroupTransactionEntity(
                groupId = groupId,
                transactionId = transactionId,
                paidByPersonId = paidByPersonId,
                paidByUserSelf = paidBySelf,
                splitType = splitType,
                tag = tag
            )
            val groupTransactionId = groupDao.insertGroupTransaction(groupTransaction)

            // 4. Create splits
            val splitEntities = splits.map { split ->
                GroupSplitEntity(
                    groupTransactionId = groupTransactionId,
                    memberId = split.memberId,
                    isSelfUser = split.isSelf,
                    shareAmount = split.amount,
                    isPaid = false
                )
            }
            groupDao.insertSplits(splitEntities)
        }
        return resultTransactionId
    }

    override suspend fun deleteGroupExpense(groupTransactionId: Long) {
        database.withTransaction {
            val groupTx = groupDao.getGroupTransactionById(groupTransactionId) ?: return@withTransaction
            val transaction = financeDao.getTransactionById(groupTx.transactionId) ?: return@withTransaction

            // Revert wallet balance
            financeDao.updateWalletBalance(transaction.walletId, transaction.amount)

            // Delete the transaction (cascades to group_transactions and splits)
            financeDao.deleteTransaction(transaction)
        }
    }

    override suspend fun updateGroupExpense(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        timestamp: Long
    ): Long {
        var resultTransactionId = 0L
        database.withTransaction {
            // 1. Get group transaction
            val groupTx = groupDao.getGroupTransactionById(groupTransactionId) ?: return@withTransaction

            // 2. Get underlying transaction
            val transaction = financeDao.getTransactionById(groupTx.transactionId) ?: return@withTransaction
            resultTransactionId = transaction.id

            val oldAmount = transaction.amount

            // 3. Adjust wallet balance if amount changed and user paid
            if (amount != oldAmount && groupTx.paidByUserSelf) {
                // wallet += oldAmount - newAmount
                // e.g., old=100, new=80 → wallet += 20 (refund)
                // e.g., old=100, new=120 → wallet -= 20 (charge more)
                financeDao.updateWalletBalance(transaction.walletId, oldAmount - amount)
            }

            // 4. Update transaction entity
            val updatedTransaction = transaction.copy(
                amount = amount,
                note = note,
                merchant = note,
                categoryName = categoryName,
                timestamp = timestamp
            )
            financeDao.updateTransactionEntity(updatedTransaction)

            // 5. Recalculate splits if amount changed
            if (amount != oldAmount) {
                // Get existing splits
                val existingSplits = groupDao.getSplitsForTransactionSync(groupTransactionId)
                val participantCount = existingSplits.size

                if (participantCount > 0) {
                    // Delete old splits
                    groupDao.deleteSplitsForTransaction(groupTransactionId)

                    // Calculate new splits based on split type
                    val newSplits = when (groupTx.splitType) {
                        SplitType.EQUAL.name -> {
                            // Equal split: divide evenly
                            val sharePerPerson = amount / participantCount
                            val remainder = amount % participantCount
                            existingSplits.mapIndexed { index, split ->
                                GroupSplitEntity(
                                    groupTransactionId = groupTransactionId,
                                    memberId = split.memberId,
                                    isSelfUser = split.isSelfUser,
                                    shareAmount = sharePerPerson + if (index == 0) remainder else 0,
                                    isPaid = split.isPaid
                                )
                            }
                        }
                        else -> {
                            // Custom/Percentage/Shares split: scale proportionally
                            val oldTotal = existingSplits.sumOf { it.shareAmount }
                            if (oldTotal > 0) {
                                var allocated = 0L
                                existingSplits.mapIndexed { index, split ->
                                    val newShareAmount = if (index == existingSplits.lastIndex) {
                                        // Last split gets remainder to avoid rounding errors
                                        amount - allocated
                                    } else {
                                        val ratio = split.shareAmount.toDouble() / oldTotal
                                        val computed = (amount * ratio).toLong()
                                        allocated += computed
                                        computed
                                    }
                                    GroupSplitEntity(
                                        groupTransactionId = groupTransactionId,
                                        memberId = split.memberId,
                                        isSelfUser = split.isSelfUser,
                                        shareAmount = newShareAmount,
                                        isPaid = split.isPaid
                                    )
                                }
                            } else {
                                // Fallback: equal split if old total was 0
                                val sharePerPerson = amount / participantCount
                                existingSplits.map { split ->
                                    GroupSplitEntity(
                                        groupTransactionId = groupTransactionId,
                                        memberId = split.memberId,
                                        isSelfUser = split.isSelfUser,
                                        shareAmount = sharePerPerson,
                                        isPaid = split.isPaid
                                    )
                                }
                            }
                        }
                    }

                    // Insert new splits
                    groupDao.insertSplits(newSplits)
                }
            }
        }
        return resultTransactionId
    }

    override suspend fun updateSimpleGroupTransaction(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        transactionType: String,
        walletId: Long,
        timestamp: Long
    ): Long {
        var resultTransactionId = 0L
        database.withTransaction {
            // 1. Get group transaction
            val groupTx = groupDao.getGroupTransactionById(groupTransactionId) ?: return@withTransaction

            // 2. Get underlying transaction
            val transaction = financeDao.getTransactionById(groupTx.transactionId) ?: return@withTransaction
            resultTransactionId = transaction.id

            // 3. Update transaction entity (no wallet adjustment for GENERAL groups)
            val updatedTransaction = transaction.copy(
                amount = amount,
                note = note,
                merchant = note,
                categoryName = categoryName,
                type = transactionType,
                walletId = walletId,
                timestamp = timestamp
            )
            financeDao.updateTransactionEntity(updatedTransaction)

            // 4. Update the single split amount
            groupDao.deleteSplitsForTransaction(groupTransactionId)
            val newSplit = GroupSplitEntity(
                groupTransactionId = groupTransactionId,
                memberId = null,
                isSelfUser = true,
                shareAmount = amount,
                isPaid = true
            )
            groupDao.insertSplit(newSplit)
        }
        return resultTransactionId
    }

    override suspend fun addSimpleGroupTransaction(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        transactionType: String,
        timestamp: Long
    ): Long {
        var resultTransactionId = 0L
        database.withTransaction {
            // Get default wallet (needed for transaction entity, but we won't deduct from it)
            val defaultWallet = financeDao.getAllWalletsList().firstOrNull()

            // 1. Create the transaction WITHOUT deducting from wallet
            val transaction = TransactionEntity(
                amount = amount,
                note = note,
                timestamp = timestamp,
                type = transactionType,  // "EXPENSE" or "INCOME"
                categoryName = categoryName,
                merchant = note,
                walletId = defaultWallet?.id ?: 0L,
                groupId = groupId
            )
            val transactionId = financeDao.insertTransactionAndGetId(transaction)
            resultTransactionId = transactionId

            // NOTE: We do NOT call financeDao.updateWalletBalance() here
            // This is intentional - GENERAL group transactions are record-only

            // 2. Create group transaction for tracking
            val groupTransaction = GroupTransactionEntity(
                groupId = groupId,
                transactionId = transactionId,
                paidByPersonId = null,
                paidByUserSelf = true,
                splitType = SplitType.EQUAL.name,
                tag = ""
            )
            val groupTransactionId = groupDao.insertGroupTransaction(groupTransaction)

            // 3. Create single split for user only (GENERAL groups have no members)
            val splitEntity = GroupSplitEntity(
                groupTransactionId = groupTransactionId,
                memberId = null,
                isSelfUser = true,
                shareAmount = amount,
                isPaid = true  // Mark as paid since no settlement needed
            )
            groupDao.insertSplit(splitEntity)
        }
        return resultTransactionId
    }

    override fun getGroupExpenses(groupId: Long): Flow<List<GroupExpenseUiModel>> {
        return combine(
            groupDao.getGroupTransactionsWithDetails(groupId),
            userPreferencesRepository.currency,
            cachedAttachmentCounts
        ) { transactions, currency, attachmentCounts ->
            transactions.map { txWithDetails ->
                GroupExpenseUiModel(
                    id = txWithDetails.groupTransaction.id,
                    transactionId = txWithDetails.groupTransaction.transactionId,
                    amount = txWithDetails.transactionAmount,
                    amountFormatted = CurrencyFormatter.format(txWithDetails.transactionAmount, currency),
                    note = txWithDetails.transactionNote,
                    paidByPersonId = txWithDetails.groupTransaction.paidByPersonId,
                    paidByName = if (txWithDetails.groupTransaction.paidByUserSelf) "You"
                                 else txWithDetails.paidByName ?: "Unknown",
                    paidBySelf = txWithDetails.groupTransaction.paidByUserSelf,
                    splitType = txWithDetails.groupTransaction.splitType,
                    tag = txWithDetails.groupTransaction.tag,
                    transactionType = txWithDetails.transactionType ?: "EXPENSE",
                    categoryName = txWithDetails.categoryName ?: "Uncategorized",
                    categoryColorHex = txWithDetails.categoryColorHex ?: "#EF4444",
                    categoryIconId = txWithDetails.categoryIconId ?: 0,
                    timestamp = txWithDetails.transactionTimestamp,
                    dateFormatted = DateUtil.formatDateWithTime(txWithDetails.transactionTimestamp, context),
                    yourShare = 0L, // Will be calculated separately
                    yourShareFormatted = "",
                    attachmentCount = attachmentCounts[txWithDetails.groupTransaction.transactionId] ?: 0
                )
            }
        }
    }

    override fun getGroupTotalSpent(groupId: Long): Flow<Long> {
        return groupDao.getGroupTotalSpent(groupId)
    }

    // ==================== SETTLEMENTS ====================

    override fun calculateSettlements(groupId: Long): Flow<List<SettlementSuggestion>> {
        return combine(
            groupDao.getGroupMembers(groupId),
            userPreferencesRepository.currency,
            groupDao.getMemberBalancesFlow(groupId),
            groupDao.getUserBalanceFlow(groupId)
        ) { members, currency, memberBalances, userBalance ->
            val balanceMap = mutableMapOf<Long?, Long>()

            // User balance
            balanceMap[null] = userBalance.totalPaid - userBalance.totalOwes

            // Member balances
            memberBalances.forEach { balance ->
                balanceMap[balance.memberId] = balance.totalPaid - balance.totalOwes
            }

            val peopleMap = members.associateBy { it.id }
            calculateSettlementSuggestionsFromBalances(balanceMap, peopleMap, currency)
        }
    }

    override suspend fun recordSettlement(
        groupId: Long,
        fromPersonId: Long?,
        fromSelf: Boolean,
        toPersonId: Long?,
        toSelf: Boolean,
        amount: Long,
        note: String
    ): Long {
        val settlement = SettlementEntity(
            groupId = groupId,
            fromPersonId = fromPersonId,
            fromSelfUser = fromSelf,
            toPersonId = toPersonId,
            toSelfUser = toSelf,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            note = note
        )
        return groupDao.insertSettlement(settlement)
    }

    override fun getSettlements(groupId: Long): Flow<List<SettlementEntity>> {
        return groupDao.getSettlements(groupId)
    }

    override suspend fun deleteSettlement(settlementId: Long) {
        groupDao.deleteSettlement(settlementId)
    }

    // ==================== SAVINGS GOALS ====================

    override suspend fun addContribution(
        groupId: Long,
        amount: Long,
        memberId: Long?,
        isSelf: Boolean,
        note: String,
        timestamp: Long
    ) {
        val contribution = GroupContributionEntity(
            groupId = groupId,
            memberId = memberId,
            isSelfUser = isSelf,
            amount = amount,
            timestamp = timestamp,
            note = note
        )
        groupDao.insertContribution(contribution)
    }

    override fun getContributions(groupId: Long): Flow<List<GroupContributionEntity>> {
        return groupDao.getGroupContributions(groupId)
    }

    override fun getSavingsProgress(groupId: Long): Flow<SavingsProgressUiModel> {
        return combine(
            groupDao.getAllGroupsWithStats(),
            groupDao.getGroupContributions(groupId),
            groupDao.getGroupMembers(groupId),
            userPreferencesRepository.currency
        ) { allGroupsWithStats, contributions, members, currency ->
            val groupWithStats = allGroupsWithStats.find { it.group.id == groupId }
            val group = groupWithStats?.group

            val totalContributed = contributions.sumOf { it.amount }
            val targetAmount = group?.targetAmount ?: 0L
            val progress = if (targetAmount > 0) totalContributed.toFloat() / targetAmount else 0f

            // Calculate per-member contributions
            val memberContributions = mutableListOf<MemberContributionSummary>()
            val userContributions = contributions.filter { it.isSelfUser }.sumOf { it.amount }

            memberContributions.add(
                MemberContributionSummary(
                    memberId = null,
                    memberName = "You",
                    isSelf = true,
                    totalContributed = userContributions,
                    totalContributedFormatted = CurrencyFormatter.format(userContributions, currency),
                    contributionPercentage = if (totalContributed > 0)
                        userContributions.toFloat() / totalContributed else 0f
                )
            )

            members.forEach { person ->
                val personContributions = contributions
                    .filter { it.memberId == person.id }
                    .sumOf { it.amount }
                memberContributions.add(
                    MemberContributionSummary(
                        memberId = person.id,
                        memberName = person.name,
                        isSelf = false,
                        totalContributed = personContributions,
                        totalContributedFormatted = CurrencyFormatter.format(personContributions, currency),
                        contributionPercentage = if (totalContributed > 0)
                            personContributions.toFloat() / totalContributed else 0f
                    )
                )
            }

            SavingsProgressUiModel(
                currentAmount = totalContributed,
                currentAmountFormatted = CurrencyFormatter.format(totalContributed, currency),
                targetAmount = targetAmount,
                targetAmountFormatted = CurrencyFormatter.format(targetAmount, currency),
                progressPercentage = progress.coerceIn(0f, 1f),
                memberContributions = memberContributions
            )
        }
    }

    override suspend fun deleteContribution(contributionId: Long) {
        groupDao.deleteContribution(contributionId)
    }

    override suspend fun updateContribution(
        contributionId: Long,
        amount: Long,
        note: String,
        timestamp: Long
    ) {
        val existing = groupDao.getContributionById(contributionId) ?: return
        val updated = existing.copy(
            amount = amount,
            note = note,
            timestamp = timestamp
        )
        groupDao.updateContribution(updated)
    }

    // ==================== ANALYTICS ====================

    override fun getExpensesByTag(groupId: Long): Flow<List<TagBreakdownUiModel>> {
        return combine(
            groupDao.getGroupTransactionsWithDetails(groupId),
            userPreferencesRepository.currency
        ) { transactions, currency ->
            calculateTagBreakdown(transactions, currency)
        }
    }

    override fun getGroupStats(groupId: Long): Flow<GroupStatsUiModel> {
        return combine(
            groupDao.getGroupTransactionsWithDetails(groupId),
            groupDao.getGroupMembers(groupId),
            userPreferencesRepository.currency
        ) { transactions, members, currency ->
            val totalSpent = transactions.sumOf { it.transactionAmount }
            val expenseCount = transactions.size
            val averagePerExpense = if (expenseCount > 0) totalSpent / expenseCount else 0L

            // Find top spender
            val spenderTotals = mutableMapOf<String, Long>()
            transactions.forEach { tx ->
                val spenderName = if (tx.groupTransaction.paidByUserSelf) "You"
                                  else tx.paidByName ?: "Unknown"
                spenderTotals[spenderName] = (spenderTotals[spenderName] ?: 0L) + tx.transactionAmount
            }
            val topSpender = spenderTotals.maxByOrNull { it.value }

            // Find most common tag
            val tagCounts = transactions.groupBy { it.groupTransaction.tag }
                .mapValues { it.value.size }
            val mostCommonTag = tagCounts.maxByOrNull { it.value }?.key ?: ""

            GroupStatsUiModel(
                totalSpent = totalSpent,
                totalSpentFormatted = CurrencyFormatter.format(totalSpent, currency),
                averagePerExpense = averagePerExpense,
                averagePerExpenseFormatted = CurrencyFormatter.format(averagePerExpense, currency),
                expenseCount = expenseCount,
                topSpenderName = topSpender?.key ?: "N/A",
                topSpenderAmount = topSpender?.value ?: 0L,
                topSpenderAmountFormatted = CurrencyFormatter.format(topSpender?.value ?: 0L, currency),
                mostCommonTag = mostCommonTag.ifEmpty { "None" }
            )
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private fun mapToGroupUiModel(
        groupWithStats: com.example.financetracker.data.local.dao.GroupWithStats,
        currency: String
    ): GroupUiModel {
        val group = groupWithStats.group

        // Calculate progress based on group type
        val progress = when (group.type) {
            GroupType.TRIP.name, GroupType.GENERAL.name -> {
                if (group.budgetLimit > 0) {
                    (groupWithStats.totalSpent.toFloat() / group.budgetLimit).coerceIn(0f, 1f)
                } else 0f
            }
            GroupType.SAVINGS_GOAL.name -> {
                if (group.targetAmount > 0) {
                    (groupWithStats.totalContributed.toFloat() / group.targetAmount).coerceIn(0f, 1f)
                } else 0f
            }
            else -> 0f
        }

        return GroupUiModel(
            id = group.id,
            name = group.name,
            type = group.type,
            memberCount = groupWithStats.memberCount,
            totalSpent = groupWithStats.totalSpent,
            totalSpentFormatted = if (group.type == GroupType.GENERAL.name) {
                // For GENERAL groups, show net total with +/- sign
                val prefix = if (groupWithStats.totalSpent >= 0) "+" else ""
                prefix + CurrencyFormatter.format(kotlin.math.abs(groupWithStats.totalSpent), currency)
            } else {
                CurrencyFormatter.format(groupWithStats.totalSpent, currency)
            },
            budgetLimit = group.budgetLimit,
            budgetLimitFormatted = if (group.budgetLimit > 0)
                CurrencyFormatter.format(group.budgetLimit, currency) else null,
            targetAmount = group.targetAmount,
            targetAmountFormatted = if (group.targetAmount > 0)
                CurrencyFormatter.format(group.targetAmount, currency) else null,
            totalContributed = groupWithStats.totalContributed,
            totalContributedFormatted = if (group.type == GroupType.SAVINGS_GOAL.name)
                CurrencyFormatter.format(groupWithStats.totalContributed, currency) else null,
            progressPercentage = progress,
            colorHex = group.colorHex,
            iconId = group.iconId,
            isActive = group.isActive,
            yourBalance = 0L, // Will be calculated in detail view
            yourBalanceFormatted = CurrencyFormatter.format(0L, currency),
            yourBalanceSign = 0,
            createdTimestamp = group.createdTimestamp,
            filterCategoryIds = group.filterCategoryIds,
            filterStartDate = group.filterStartDate,
            filterEndDate = group.filterEndDate
        )
    }

    private fun formatBalance(balance: Long, currency: String): String {
        return if (balance >= 0) {
            CurrencyFormatter.format(balance, currency)
        } else {
            "-${CurrencyFormatter.format(-balance, currency)}"
        }
    }

    private fun calculateSettlementSuggestions(
        members: List<GroupMemberUiModel>,
        peopleMap: Map<Long, PersonEntity>,
        currency: String
    ): List<SettlementSuggestion> {
        val balanceMap = members.associate {
            it.personId to it.balance
        }
        return calculateSettlementSuggestionsFromBalances(balanceMap, peopleMap, currency)
    }

    /**
     * Greedy algorithm to minimize number of settlements
     * Balance > 0 = owed money (creditor)
     * Balance < 0 = owes money (debtor)
     */
    private fun calculateSettlementSuggestionsFromBalances(
        balances: Map<Long?, Long>,
        peopleMap: Map<Long, PersonEntity>,
        currency: String
    ): List<SettlementSuggestion> {
        val debtors = balances.filter { it.value < 0 }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val creditors = balances.filter { it.value > 0 }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val settlements = mutableListOf<SettlementSuggestion>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.first()
            val creditor = creditors.first()

            val amount = minOf(debtor.second, creditor.second)

            val fromName = if (debtor.first == null) "You"
                           else peopleMap[debtor.first]?.name ?: "Unknown"
            val toName = if (creditor.first == null) "You"
                         else peopleMap[creditor.first]?.name ?: "Unknown"

            settlements.add(
                SettlementSuggestion(
                    fromPersonId = debtor.first,
                    fromName = fromName,
                    fromIsSelf = debtor.first == null,
                    toPersonId = creditor.first,
                    toName = toName,
                    toIsSelf = creditor.first == null,
                    amount = amount,
                    amountFormatted = CurrencyFormatter.format(amount, currency)
                )
            )

            // Update balances
            if (debtor.second == amount) {
                debtors.removeAt(0)
            } else {
                debtors[0] = debtor.first to (debtor.second - amount)
            }

            if (creditor.second == amount) {
                creditors.removeAt(0)
            } else {
                creditors[0] = creditor.first to (creditor.second - amount)
            }
        }

        return settlements
    }

    private fun calculateTagBreakdown(
        transactions: List<com.example.financetracker.data.local.dao.GroupTransactionWithDetails>,
        currency: String
    ): List<TagBreakdownUiModel> {
        val categoryTotals = mutableMapOf<String, Long>()
        transactions.forEach { tx ->
            val category = tx.categoryName?.ifEmpty { "Uncategorized" } ?: "Uncategorized"
            categoryTotals[category] = (categoryTotals[category] ?: 0L) + tx.transactionAmount
        }

        val total = categoryTotals.values.sum()
        // Default category colors - will be matched with actual category colors
        val defaultColors = mapOf(
            "Food & Dining" to "#EF4444",
            "Shopping" to "#F59E0B",
            "Transportation" to "#3B82F6",
            "Bills & Utilities" to "#8B5CF6",
            "Entertainment" to "#EC4899",
            "Health & Wellness" to "#10B981",
            "Trip" to "#06B6D4",
            "Money Transfer" to "#6366F1",
            "Uncategorized" to "#6B7280"
        )
        val fallbackColors = listOf("#2196F3", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0", "#00BCD4")

        return categoryTotals.entries.mapIndexed { index, (category, amount) ->
            TagBreakdownUiModel(
                tag = category,
                total = amount,
                totalFormatted = CurrencyFormatter.format(amount, currency),
                percentage = if (total > 0) amount.toFloat() / total else 0f,
                colorHex = defaultColors[category] ?: fallbackColors[index % fallbackColors.size]
            )
        }.sortedByDescending { it.total }
    }

    // ==================== CATEGORY FILTER GROUPS ====================

    override fun getFilteredTransactions(
        categoryIds: List<Long>,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<TransactionWithCategory>> {
        val effectiveEndDate = endDate ?: System.currentTimeMillis()

        return combine(
            financeDao.getAllTransactions(),
            financeDao.getAllCategories(),
            userPreferencesRepository.currency
        ) { transactions, categories, _ ->
            val categoryMap = categories.associateBy { it.id }
            val categoryNames = categoryIds.mapNotNull { categoryMap[it]?.name }.toSet()

            // Find the effective start date if "From Day One" is selected
            val effectiveStartDate = startDate ?: transactions.minOfOrNull { it.timestamp } ?: 0L

            transactions
                .filter { tx ->
                    // Filter by category name
                    categoryNames.contains(tx.categoryName) &&
                    // Filter by date range
                    tx.timestamp >= effectiveStartDate &&
                    tx.timestamp <= effectiveEndDate &&
                    // Exclude group transactions
                    tx.groupId == null
                }
                .map { tx ->
                    val category = categories.find { it.name == tx.categoryName }
                    TransactionWithCategory(
                        transaction = tx,
                        categoryName = tx.categoryName,
                        categoryColorHex = category?.colorHex ?: "#6B7280"
                    )
                }
                .sortedByDescending { it.transaction.timestamp }
        }
    }

    override suspend fun getOldestTransactionTimestamp(): Long? {
        return financeDao.getOldestTransactionTimestamp()
    }
}
