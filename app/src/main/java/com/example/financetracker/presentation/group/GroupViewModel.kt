package com.example.financetracker.presentation.group

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.SplitType
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.domain.model.TransactionWithCategory
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.GroupRepository
import com.example.financetracker.domain.repository.SplitInput
import com.example.financetracker.domain.repository.TransactionAttachmentRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val financeRepository: FinanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionAttachmentRepository: TransactionAttachmentRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Tab state persistence for GroupDetailScreen - survives navigation and process death
    val selectedDetailTab: StateFlow<Int> = savedStateHandle.getStateFlow("group_detail_tab", 0)

    fun selectDetailTab(index: Int) {
        savedStateHandle["group_detail_tab"] = index
    }

    // Selected group for detail view
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    // Filter for group list
    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    // All groups (filtered by type if selected)
    val groups: StateFlow<List<GroupUiModel>> = _selectedFilter
        .flatMapLatest { filter ->
            if (filter.isNullOrEmpty() || filter == "All") {
                groupRepository.getAllGroups()
            } else {
                groupRepository.getGroupsByType(filter)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected group detail
    val selectedGroupDetail: StateFlow<GroupDetailUiModel?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id != null) {
                groupRepository.getGroupDetail(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Available people (for adding members)
    val availablePeople: StateFlow<List<PersonEntity>> = financeRepository.getAllPeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available categories (for category filter groups)
    val availableCategories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available wallets (for expense payment)
    val wallets: StateFlow<List<AccountUiModel>> = financeRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions for category filter groups
    private val _filteredTransactions = MutableStateFlow<List<TransactionWithCategory>>(emptyList())
    val filteredTransactions: StateFlow<List<TransactionWithCategory>> = _filteredTransactions.asStateFlow()

    // Currency symbol
    val currencySymbol: StateFlow<String> = userPreferencesRepository.currency
        .map { code ->
            try {
                java.util.Currency.getInstance(code).symbol
            } catch (e: Exception) {
                AppLogger.w("GroupViewModel", "Invalid currency code: $code, using code as fallback", e)
                code
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Undo settlement state
    private val _lastSettlementId = MutableStateFlow<Long?>(null)
    val lastSettlementId: StateFlow<Long?> = _lastSettlementId.asStateFlow()

    private val _showUndoSnackbar = MutableStateFlow(false)
    val showUndoSnackbar: StateFlow<Boolean> = _showUndoSnackbar.asStateFlow()

    // ==================== GROUP ACTIONS ====================

    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    fun setFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun createGroup(
        name: String,
        type: String,
        budgetLimit: Long = 0L,
        targetAmount: Long = 0L,
        description: String = "",
        colorHex: String = "#2196F3",
        iconId: Int = 0,
        memberIds: List<Long> = emptyList(),
        filterCategoryIds: String? = null,
        filterStartDate: Long? = null,
        filterEndDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val groupId = groupRepository.createGroup(
                    name = name,
                    type = type,
                    budgetLimit = budgetLimit,
                    targetAmount = targetAmount,
                    description = description,
                    colorHex = colorHex,
                    iconId = iconId,
                    filterCategoryIds = filterCategoryIds,
                    filterStartDate = filterStartDate,
                    filterEndDate = filterEndDate
                )
                // Add members if provided
                if (memberIds.isNotEmpty()) {
                    groupRepository.addMembers(groupId, memberIds)
                }
            } catch (e: Exception) {
                _error.value = "Failed to create group: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateGroup(
        id: Long,
        name: String,
        budgetLimit: Long,
        targetAmount: Long,
        description: String = "",
        colorHex: String = "#2196F3",
        iconId: Int = 0
    ) {
        viewModelScope.launch {
            groupRepository.updateGroup(id, name, budgetLimit, targetAmount, description, colorHex, iconId)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(groupId)
                _selectedGroupId.value = null
            } catch (e: Exception) {
                _error.value = "Failed to delete group: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            groupRepository.archiveGroup(groupId)
        }
    }

    // ==================== MEMBER ACTIONS ====================

    fun addMember(groupId: Long, personId: Long) {
        viewModelScope.launch {
            groupRepository.addMember(groupId, personId)
        }
    }

    fun addMembers(groupId: Long, personIds: List<Long>) {
        viewModelScope.launch {
            groupRepository.addMembers(groupId, personIds)
        }
    }

    fun removeMember(groupId: Long, personId: Long) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, personId)
        }
    }

    // ==================== EXPENSE ACTIONS ====================

    /**
     * Add an expense to a group
     * @param amount Amount in paisa
     * @param splits List of split amounts per member (must sum to amount)
     * @param attachmentUris URIs of attachments to add to the expense
     */
    fun addExpense(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String = "Uncategorized",
        paidByPersonId: Long?,
        paidBySelf: Boolean,
        splitType: String = SplitType.EQUAL.name,
        splits: List<SplitInput>,
        tag: String = "",
        walletId: Long,
        timestamp: Long = System.currentTimeMillis(),
        attachmentUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val transactionId = groupRepository.addGroupExpense(
                    groupId = groupId,
                    amount = amount,
                    note = note,
                    categoryName = categoryName,
                    paidByPersonId = paidByPersonId,
                    paidBySelf = paidBySelf,
                    splitType = splitType,
                    splits = splits,
                    tag = tag,
                    walletId = walletId,
                    timestamp = timestamp
                )
                // Add attachments if any
                if (attachmentUris.isNotEmpty() && transactionId > 0) {
                    transactionAttachmentRepository.addAttachments(transactionId, attachmentUris)
                }
            } catch (e: Exception) {
                _error.value = "Failed to add expense: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Add an equally split expense
     */
    fun addEqualExpense(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String = "Uncategorized",
        paidBySelf: Boolean,
        paidByPersonId: Long?,
        tag: String = "",
        walletId: Long,
        memberIds: List<Long> // List of person IDs to split with (user is implicit)
    ) {
        viewModelScope.launch {
            val totalMembers = memberIds.size + 1 // +1 for current user
            val sharePerPerson = amount / totalMembers
            val remainder = amount % totalMembers

            val splits = mutableListOf<SplitInput>()

            // User's share (gets any remainder)
            splits.add(SplitInput(
                memberId = null,
                isSelf = true,
                amount = sharePerPerson + remainder
            ))

            // Other members' shares
            memberIds.forEach { memberId ->
                splits.add(SplitInput(
                    memberId = memberId,
                    isSelf = false,
                    amount = sharePerPerson
                ))
            }

            addExpense(
                groupId = groupId,
                amount = amount,
                note = note,
                categoryName = categoryName,
                paidByPersonId = paidByPersonId,
                paidBySelf = paidBySelf,
                splitType = SplitType.EQUAL.name,
                splits = splits,
                tag = tag,
                walletId = walletId
            )
        }
    }

    fun deleteExpense(groupTransactionId: Long) {
        viewModelScope.launch {
            groupRepository.deleteGroupExpense(groupTransactionId)
        }
    }

    /**
     * Update an expense in a SPLIT_EXPENSE group
     * Amount is editable - splits will be automatically recalculated
     */
    fun updateExpense(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        timestamp: Long,
        newAttachmentUris: List<Uri> = emptyList(),
        deletedAttachmentIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val transactionId = groupRepository.updateGroupExpense(
                    groupTransactionId = groupTransactionId,
                    amount = amount,
                    note = note,
                    categoryName = categoryName,
                    timestamp = timestamp
                )
                // Handle attachment deletions
                deletedAttachmentIds.forEach { attachmentId ->
                    transactionAttachmentRepository.deleteAttachmentById(attachmentId)
                }
                // Handle new attachments
                if (newAttachmentUris.isNotEmpty() && transactionId > 0) {
                    transactionAttachmentRepository.addAttachments(transactionId, newAttachmentUris)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update expense: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Update a simple transaction in a GENERAL group
     * All fields are editable
     */
    fun updateSimpleTransaction(
        groupTransactionId: Long,
        amount: Long,
        note: String,
        categoryName: String,
        transactionType: String,
        walletId: Long,
        timestamp: Long,
        newAttachmentUris: List<Uri> = emptyList(),
        deletedAttachmentIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val transactionId = groupRepository.updateSimpleGroupTransaction(
                    groupTransactionId = groupTransactionId,
                    amount = amount,
                    note = note,
                    categoryName = categoryName,
                    transactionType = transactionType,
                    walletId = walletId,
                    timestamp = timestamp
                )
                // Handle attachment deletions
                deletedAttachmentIds.forEach { attachmentId ->
                    transactionAttachmentRepository.deleteAttachmentById(attachmentId)
                }
                // Handle new attachments
                if (newAttachmentUris.isNotEmpty() && transactionId > 0) {
                    transactionAttachmentRepository.addAttachments(transactionId, newAttachmentUris)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update transaction: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Add a simple transaction to a GENERAL group (no wallet deduction, no splits)
     * Used for record-only transactions
     */
    fun addSimpleTransaction(
        groupId: Long,
        amount: Long,
        note: String,
        categoryName: String = "Uncategorized",
        transactionType: String = "EXPENSE",
        timestamp: Long = System.currentTimeMillis(),
        attachmentUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            val transactionId = groupRepository.addSimpleGroupTransaction(
                groupId = groupId,
                amount = amount,
                note = note,
                categoryName = categoryName,
                transactionType = transactionType,
                timestamp = timestamp
            )
            // Add attachments if any
            if (attachmentUris.isNotEmpty() && transactionId > 0) {
                transactionAttachmentRepository.addAttachments(transactionId, attachmentUris)
            }
        }
    }

    /**
     * Check if a group is a GENERAL type (simple record-only transactions)
     */
    fun isGeneralGroup(type: String): Boolean {
        return type == GroupType.GENERAL.name
    }

    // ==================== ATTACHMENT METHODS ====================

    /**
     * Get attachments for a group expense (for display in edit sheet)
     */
    suspend fun getAttachmentsForGroupExpense(transactionId: Long): List<AttachmentUiModel> {
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
     * Get attachment entities for the attachment viewer
     */
    suspend fun getAttachmentEntities(transactionId: Long): List<TransactionAttachmentEntity> {
        return transactionAttachmentRepository.getAttachmentsForTransactionSync(transactionId)
    }

    // ==================== SETTLEMENT ACTIONS ====================

    fun recordSettlement(
        groupId: Long,
        fromPersonId: Long?,
        fromSelf: Boolean,
        toPersonId: Long?,
        toSelf: Boolean,
        amount: Long,
        note: String = ""
    ) {
        viewModelScope.launch {
            try {
                val settlementId = groupRepository.recordSettlement(
                    groupId = groupId,
                    fromPersonId = fromPersonId,
                    fromSelf = fromSelf,
                    toPersonId = toPersonId,
                    toSelf = toSelf,
                    amount = amount,
                    note = note
                )
                _lastSettlementId.value = settlementId
                _showUndoSnackbar.value = true
            } catch (e: Exception) {
                _error.value = "Failed to record settlement: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteSettlement(settlementId: Long) {
        viewModelScope.launch {
            groupRepository.deleteSettlement(settlementId)
        }
    }

    // ==================== CONTRIBUTION ACTIONS (SAVINGS GOALS) ====================

    fun addContribution(
        groupId: Long,
        amount: Long,
        memberId: Long? = null,
        isSelf: Boolean = true,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            groupRepository.addContribution(groupId, amount, memberId, isSelf, note, timestamp)
        }
    }

    fun deleteContribution(contributionId: Long) {
        viewModelScope.launch {
            groupRepository.deleteContribution(contributionId)
        }
    }

    /**
     * Update a contribution in a savings goal
     */
    fun updateContribution(
        contributionId: Long,
        amount: Long,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            try {
                groupRepository.updateContribution(
                    contributionId = contributionId,
                    amount = amount,
                    note = note,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                _error.value = "Failed to update contribution: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // ==================== HELPERS ====================

    fun getGroupTypeDisplayName(type: String): String {
        return when (type) {
            GroupType.TRIP.name -> "Group By Category"
            GroupType.SPLIT_EXPENSE.name -> "Split Bill"
            GroupType.SAVINGS_GOAL.name -> "Savings Goal"
            GroupType.GENERAL.name -> "General"
            else -> type
        }
    }

    fun getGroupTypeIcon(type: String): Int {
        return when (type) {
            GroupType.TRIP.name -> 7 // Flight icon
            GroupType.SPLIT_EXPENSE.name -> 8 // SwapHoriz icon
            GroupType.SAVINGS_GOAL.name -> 9 // AccountBalance icon
            else -> 0 // Default icon
        }
    }

    // ==================== CATEGORY FILTER GROUP ACTIONS ====================

    /**
     * Load filtered transactions for a category filter group
     */
    fun loadFilteredTransactions(
        filterCategoryIds: String?,
        filterStartDate: Long?,
        filterEndDate: Long?
    ) {
        if (filterCategoryIds.isNullOrEmpty()) {
            _filteredTransactions.value = emptyList()
            return
        }

        val categoryIds = filterCategoryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (categoryIds.isEmpty()) {
            _filteredTransactions.value = emptyList()
            return
        }

        viewModelScope.launch {
            groupRepository.getFilteredTransactions(categoryIds, filterStartDate, filterEndDate)
                .collect { transactions ->
                    _filteredTransactions.value = transactions
                }
        }
    }

    /**
     * Check if a group is a category filter type
     */
    fun isCategoryFilterGroup(type: String): Boolean {
        return type == GroupType.TRIP.name
    }

    fun clearError() {
        _error.value = null
    }

    // ==================== UNDO SETTLEMENT ====================

    fun undoLastSettlement() {
        val id = _lastSettlementId.value ?: return
        viewModelScope.launch {
            try {
                groupRepository.deleteSettlement(id)
                _lastSettlementId.value = null
                _showUndoSnackbar.value = false
            } catch (e: Exception) {
                _error.value = "Failed to undo settlement: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun dismissUndoSnackbar() {
        _showUndoSnackbar.value = false
        _lastSettlementId.value = null
    }
}
