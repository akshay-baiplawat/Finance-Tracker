package com.example.financetracker.presentation.group

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.core.util.DateUtil
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.domain.model.TransactionWithCategory
import com.example.financetracker.presentation.attachment.AttachmentViewerScreen
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.components.CategoryIcon
import com.example.financetracker.presentation.components.CategoryIconWithBadge
import com.example.financetracker.presentation.model.*
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    onBackClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onSettleUpClick: (SettlementSuggestion) -> Unit,
    onAddContributionClick: () -> Unit,
    onEditTransaction: (TransactionWithCategory) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = hiltViewModel()
) {
    LaunchedEffect(groupId) {
        viewModel.selectGroup(groupId)
    }

    val groupDetail by viewModel.selectedGroupDetail.collectAsState()
    val availablePeople by viewModel.availablePeople.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedTab by viewModel.selectedDetailTab.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val error by viewModel.error.collectAsState()
    val showUndoSnackbar by viewModel.showUndoSnackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showEditExpenseSheet by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<GroupExpenseUiModel?>(null) }
    var showEditContributionSheet by remember { mutableStateOf(false) }
    var selectedContributionForEdit by remember { mutableStateOf<ContributionUiModel?>(null) }

    // Attachment state for edit expense sheet
    var selectedExpenseAttachments by remember { mutableStateOf<List<AttachmentUiModel>>(emptyList()) }
    var attachmentsForViewer by remember { mutableStateOf<List<TransactionAttachmentEntity>>(emptyList()) }
    var showAttachmentViewer by remember { mutableStateOf(false) }
    var attachmentViewerInitialIndex by remember { mutableIntStateOf(0) }

    // Load attachments when an expense is selected for editing
    LaunchedEffect(selectedExpenseForEdit) {
        selectedExpenseForEdit?.let { expense ->
            selectedExpenseAttachments = viewModel.getAttachmentsForGroupExpense(expense.transactionId)
            attachmentsForViewer = viewModel.getAttachmentEntities(expense.transactionId)
        }
    }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    // Show undo snackbar after settlement is recorded
    LaunchedEffect(showUndoSnackbar) {
        if (showUndoSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Settlement recorded",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoLastSettlement()
                SnackbarResult.Dismissed -> viewModel.dismissUndoSnackbar()
            }
        }
    }

    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addMemberSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editExpenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editContributionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val detail = groupDetail

    // Check if this is a category filter group
    val isCategoryFilterGroup = detail?.group?.type == GroupType.TRIP.name

    // Load filtered transactions for category filter groups
    LaunchedEffect(detail) {
        if (isCategoryFilterGroup && detail != null) {
            viewModel.loadFilteredTransactions(
                filterCategoryIds = detail.group.filterCategoryIds,
                filterStartDate = detail.group.filterStartDate,
                filterEndDate = detail.group.filterEndDate
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Group", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete \"${detail?.group?.name}\"? " +
                    "This will remove all expenses, settlements, and contributions in this group. " +
                    "This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(groupId)
                        showDeleteConfirmation = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = FinosMint)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Edit Group Sheet
    if (showEditSheet && detail != null) {
        EditGroupSheet(
            sheetState = editSheetState,
            group = detail.group,
            onDismiss = { showEditSheet = false },
            onSave = { name, budgetLimit, targetAmount, colorHex ->
                viewModel.updateGroup(
                    id = groupId,
                    name = name,
                    budgetLimit = budgetLimit,
                    targetAmount = targetAmount,
                    colorHex = colorHex
                )
            }
        )
    }

    // Add Member Sheet
    if (showAddMemberSheet && detail != null) {
        // Filter out already-added members
        val existingMemberIds = detail.members.mapNotNull { it.personId }.toSet()
        val peopleToAdd = availablePeople.filter { it.id !in existingMemberIds }

        AddMemberSheet(
            sheetState = addMemberSheetState,
            availablePeople = peopleToAdd,
            onDismiss = { showAddMemberSheet = false },
            onAddNewPerson = { /* Navigate to wallet screen to add person */ },
            onSave = { memberIds ->
                viewModel.addMembers(groupId, memberIds)
            }
        )
    }

    // Edit Expense Sheet
    if (showEditExpenseSheet && selectedExpenseForEdit != null && detail != null) {
        val isGeneralGroup = detail.group.type == GroupType.GENERAL.name

        EditGroupExpenseSheet(
            sheetState = editExpenseSheetState,
            expense = selectedExpenseForEdit!!,
            isGeneralGroup = isGeneralGroup,
            existingAttachments = selectedExpenseAttachments,
            wallets = wallets.take(3),
            currentWalletId = 0L, // TODO: Get current wallet ID from transaction if needed
            currencySymbol = currencySymbol,
            onDismiss = {
                showEditExpenseSheet = false
                selectedExpenseForEdit = null
                selectedExpenseAttachments = emptyList()
            },
            onSave = { amount, note, categoryName, timestamp, newAttachmentUris, deletedAttachmentIds ->
                viewModel.updateExpense(
                    groupTransactionId = selectedExpenseForEdit!!.id,
                    amount = amount,
                    note = note,
                    categoryName = categoryName,
                    timestamp = timestamp,
                    newAttachmentUris = newAttachmentUris,
                    deletedAttachmentIds = deletedAttachmentIds
                )
            },
            onSaveGeneral = if (isGeneralGroup) { amount, note, categoryName, transactionType, walletId, timestamp, newAttachmentUris, deletedAttachmentIds ->
                viewModel.updateSimpleTransaction(
                    groupTransactionId = selectedExpenseForEdit!!.id,
                    amount = amount,
                    note = note,
                    categoryName = categoryName,
                    transactionType = transactionType,
                    walletId = walletId,
                    timestamp = timestamp,
                    newAttachmentUris = newAttachmentUris,
                    deletedAttachmentIds = deletedAttachmentIds
                )
            } else null,
            onDelete = {
                viewModel.deleteExpense(selectedExpenseForEdit!!.id)
            },
            onViewAttachment = { index ->
                if (attachmentsForViewer.isNotEmpty()) {
                    attachmentViewerInitialIndex = index
                    showAttachmentViewer = true
                }
            }
        )
    }

    // Attachment Viewer (wrapped in Dialog to appear on top of ModalBottomSheet)
    if (showAttachmentViewer && attachmentsForViewer.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAttachmentViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AttachmentViewerScreen(
                attachments = attachmentsForViewer,
                initialIndex = attachmentViewerInitialIndex,
                onBack = { showAttachmentViewer = false }
            )
        }
    }

    // Edit Contribution Sheet
    if (showEditContributionSheet && selectedContributionForEdit != null) {
        EditContributionSheet(
            sheetState = editContributionSheetState,
            contribution = selectedContributionForEdit!!,
            currencySymbol = currencySymbol,
            onDismiss = {
                showEditContributionSheet = false
                selectedContributionForEdit = null
            },
            onSave = { amount, note, timestamp ->
                viewModel.updateContribution(
                    contributionId = selectedContributionForEdit!!.id,
                    amount = amount,
                    note = note,
                    timestamp = timestamp
                )
            },
            onDelete = {
                viewModel.deleteContribution(selectedContributionForEdit!!.id)
            }
        )
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.group?.name ?: "Group",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            // Hide FAB for category filter groups (TRIP type) since they're read-only
            if (detail != null && !isCategoryFilterGroup) {
                val isGeneralGroup = detail.group.type == GroupType.GENERAL.name
                val fabAction = when {
                    detail.group.type == GroupType.SAVINGS_GOAL.name -> onAddContributionClick
                    else -> onAddExpenseClick
                }
                val fabIcon = when {
                    detail.group.type == GroupType.SAVINGS_GOAL.name -> Icons.Rounded.Add
                    else -> Icons.Rounded.Add
                }
                val fabLabel = when {
                    detail.group.type == GroupType.SAVINGS_GOAL.name -> "Add Contribution"
                    isGeneralGroup -> "Add Transaction"
                    else -> "Add Expense"
                }

                ExtendedFloatingActionButton(
                    onClick = fabAction,
                    containerColor = FinosMint,
                    contentColor = Color.White
                ) {
                    Icon(fabIcon, contentDescription = fabLabel)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(fabLabel)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FinosMint)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Summary Card - hide for category filter groups and GENERAL groups
                val isGeneralGroupType = detail.group.type == GroupType.GENERAL.name
                if (!isCategoryFilterGroup && !isGeneralGroupType) {
                    GroupSummaryCard(
                        group = detail.group,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Tab Row
                val tabs = buildTabList(detail.group.type)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = FinosMint
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { viewModel.selectDetailTab(index) },
                            text = { Text(tab) }
                        )
                    }
                }

                // Tab Content
                val isGeneralGroup = detail.group.type == GroupType.GENERAL.name
                when (tabs.getOrNull(selectedTab)) {
                    "Transactions" -> {
                        if (isGeneralGroup) {
                            // GENERAL group: Show group expenses as transactions
                            ExpensesTabContent(
                                expenses = detail.recentExpenses,
                                onDeleteExpense = { viewModel.deleteExpense(it) },
                                onEditExpense = { expense ->
                                    selectedExpenseForEdit = expense
                                    showEditExpenseSheet = true
                                }
                            )
                        } else {
                            // TRIP (category filter) group: Show filtered personal transactions
                            FilteredTransactionsTabContent(
                                transactions = filteredTransactions,
                                currencySymbol = currencySymbol,
                                onEditTransaction = onEditTransaction
                            )
                        }
                    }
                    "Expenses" -> ExpensesTabContent(
                        expenses = detail.recentExpenses,
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onEditExpense = { expense ->
                            selectedExpenseForEdit = expense
                            showEditExpenseSheet = true
                        }
                    )
                    "Balances" -> BalancesTabContent(
                        members = detail.members,
                        isSavingsGoal = detail.group.type == GroupType.SAVINGS_GOAL.name,
                        onAddMemberClick = { showAddMemberSheet = true }
                    )
                    "Settle Up" -> SettleUpTabContent(
                        suggestions = detail.settlementSuggestions,
                        history = detail.settlementHistory,
                        onSettleClick = onSettleUpClick,
                        onDeleteSettlement = { viewModel.deleteSettlement(it) }
                    )
                    "Contributions" -> ContributionsTabContent(
                        contributions = detail.contributions,
                        onDeleteContribution = { viewModel.deleteContribution(it) },
                        onEditContribution = { contribution ->
                            selectedContributionForEdit = contribution
                            showEditContributionSheet = true
                        }
                    )
                    "Categories" -> CategoriesTabContent(
                        tagBreakdown = detail.tagBreakdown
                    )
                    "Summary" -> FilteredTransactionsSummaryContent(
                        transactions = filteredTransactions,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSummaryCard(
    group: GroupUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(group.colorHex)).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (group.type) {
                            GroupType.SAVINGS_GOAL.name -> "Saved"
                            else -> "Total Spent"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = when (group.type) {
                            GroupType.SAVINGS_GOAL.name -> group.totalContributedFormatted ?: "₹0"
                            else -> group.totalSpentFormatted
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (group.budgetLimitFormatted != null || group.targetAmountFormatted != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (group.type) {
                                GroupType.SAVINGS_GOAL.name -> "Goal"
                                else -> "Budget"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = when (group.type) {
                                GroupType.SAVINGS_GOAL.name -> group.targetAmountFormatted ?: ""
                                else -> group.budgetLimitFormatted ?: ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (group.budgetLimitFormatted != null || group.targetAmountFormatted != null) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { group.progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(android.graphics.Color.parseColor(group.colorHex)),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${(group.progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(android.graphics.Color.parseColor(group.colorHex)),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun ExpensesTabContent(
    expenses: List<GroupExpenseUiModel>,
    onDeleteExpense: (Long) -> Unit,
    onEditExpense: (GroupExpenseUiModel) -> Unit
) {
    if (expenses.isEmpty()) {
        EmptyTabState(
            icon = Icons.Rounded.Receipt,
            message = "No expenses yet"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    onDelete = { onDeleteExpense(expense.id) },
                    onClick = { onEditExpense(expense) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: GroupExpenseUiModel,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val isIncome = expense.transactionType == "INCOME"
    val amountColor = if (isIncome) Color(0xFF22C55E) else Color(0xFFEF4444) // Green for income, Red for expense
    val amountPrefix = if (isIncome) "+" else "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon with color and attachment badge
            CategoryIconWithBadge(
                categoryName = expense.categoryName,
                iconId = expense.categoryIconId,
                colorHex = expense.categoryColorHex,
                size = 44.dp,
                useRoundedCorners = true,
                attachmentCount = expense.attachmentCount
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${expense.amountFormatted}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = expense.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BalancesTabContent(
    members: List<GroupMemberUiModel>,
    isSavingsGoal: Boolean,
    onAddMemberClick: () -> Unit
) {
    if (members.isEmpty()) {
        EmptyTabState(
            icon = Icons.Rounded.People,
            message = "No members yet"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members, key = { it.personId ?: -1 }) { member ->
                MemberBalanceRow(member = member, isSavingsGoal = isSavingsGoal)
            }
            // Add Member card at the end
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddMemberClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = FinosMint.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = "Add Member",
                            tint = FinosMint
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add Member",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FinosMint,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberBalanceRow(
    member: GroupMemberUiModel,
    isSavingsGoal: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.isSelf) FinosMint.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (member.isSelf) FinosMint
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Paid: ${member.totalPaidFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isSavingsGoal) {
                    // Savings Goal: show contribution amount in green
                    Text(
                        text = member.totalPaidFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinosMint
                    )
                    Text(
                        text = "contributed",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinosMint.copy(alpha = 0.7f)
                    )
                } else {
                    // Existing logic for other group types
                    val balanceColor = when (member.balanceSign) {
                        1 -> FinosMint
                        -1 -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                    val balanceText = when (member.balanceSign) {
                        1 -> "gets back"
                        -1 -> "owes"
                        else -> "settled"
                    }

                    Text(
                        text = member.balanceFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Text(
                        text = balanceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = balanceColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettleUpTabContent(
    suggestions: List<SettlementSuggestion>,
    history: List<SettlementHistoryUiModel>,
    onSettleClick: (SettlementSuggestion) -> Unit,
    onDeleteSettlement: (Long) -> Unit
) {
    if (suggestions.isEmpty() && history.isEmpty()) {
        EmptyTabState(
            icon = Icons.Rounded.CheckCircle,
            message = "All settled up!"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pending settlements section
            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(suggestions) { suggestion ->
                    SettlementCard(
                        suggestion = suggestion,
                        onSettleClick = { onSettleClick(suggestion) }
                    )
                }
            }

            // History section
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(history, key = { it.id }) { settlement ->
                    SettlementHistoryCard(
                        settlement = settlement,
                        onDelete = { onDeleteSettlement(settlement.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementCard(
    suggestion: SettlementSuggestion,
    onSettleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = suggestion.fromName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (suggestion.fromIsSelf) FinosMint
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = suggestion.toName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (suggestion.toIsSelf) FinosMint
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = suggestion.amountFormatted,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }

            Button(
                onClick = onSettleClick,
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint)
            ) {
                Text("Settle")
            }
        }
    }
}

@Composable
private fun SettlementHistoryCard(
    settlement: SettlementHistoryUiModel,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FinosMint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Settlement completed",
                    tint = FinosMint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = settlement.fromName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (settlement.fromIsSelf) FinosMint
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = settlement.toName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (settlement.toIsSelf) FinosMint
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = settlement.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (settlement.note.isNotEmpty()) {
                    Text(
                        text = settlement.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = settlement.amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinosMint
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionsTabContent(
    contributions: List<ContributionUiModel>,
    onDeleteContribution: (Long) -> Unit,
    onEditContribution: (ContributionUiModel) -> Unit
) {
    if (contributions.isEmpty()) {
        EmptyTabState(
            icon = Icons.Rounded.Savings,
            message = "No contributions yet"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contributions, key = { it.id }) { contribution ->
                ContributionRow(
                    contribution = contribution,
                    onClick = { onEditContribution(contribution) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ContributionRow(
    contribution: ContributionUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contribution.memberName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (contribution.isSelf) FinosMint
                            else MaterialTheme.colorScheme.onSurface
                )
                if (contribution.note.isNotEmpty()) {
                    Text(
                        text = contribution.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = contribution.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Text(
                text = contribution.amountFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinosMint
            )
        }
    }
}

@Composable
private fun CategoriesTabContent(
    tagBreakdown: List<TagBreakdownUiModel>
) {
    if (tagBreakdown.isEmpty()) {
        EmptyTabState(
            icon = Icons.Rounded.Label,
            message = "No categories yet"
        )
    } else {
        val total = tagBreakdown.sumOf { it.total }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Donut Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Simple donut chart using Canvas
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                var startAngle = -90f
                                tagBreakdown.forEach { item ->
                                    val sweepAngle = item.percentage * 360f
                                    drawArc(
                                        color = Color(android.graphics.Color.parseColor(item.colorHex)),
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40f)
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            // Center text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${tagBreakdown.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Category list
            items(tagBreakdown) { category ->
                CategoryRow(category = category)
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: TagBreakdownUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(category.colorHex)))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Category name and percentage
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.tag,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { category.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(android.graphics.Color.parseColor(category.colorHex)),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount and percentage
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = category.totalFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(category.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = message,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

private fun buildTabList(type: String): List<String> {
    return when (type) {
        GroupType.TRIP.name -> listOf("Transactions", "Summary") // Category filter group - read-only
        GroupType.SAVINGS_GOAL.name -> listOf("Contributions", "Balances")
        GroupType.SPLIT_EXPENSE.name -> listOf("Expenses", "Balances", "Settle Up", "Categories")
        GroupType.GENERAL.name -> listOf("Transactions", "Categories") // GENERAL - simple record-only
        else -> listOf("Expenses", "Balances", "Settle Up", "Categories")
    }
}

// ==================== CATEGORY FILTER GROUP TAB CONTENTS ====================

@Composable
private fun FilteredTransactionsTabContent(
    transactions: List<TransactionWithCategory>,
    currencySymbol: String,
    onEditTransaction: (TransactionWithCategory) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "No matching transactions",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No transactions match the filter criteria",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { txWithCategory ->
                FilteredTransactionItem(
                    transaction = txWithCategory,
                    currencySymbol = currencySymbol,
                    onClick = { onEditTransaction(txWithCategory) }
                )
            }
        }
    }
}

@Composable
private fun FilteredTransactionItem(
    transaction: TransactionWithCategory,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val tx = transaction.transaction
    val context = LocalContext.current
    val formattedDate = remember(tx.timestamp) {
        DateUtil.formatDateWithTime(tx.timestamp, context)
    }

    val amountFormatted = CurrencyFormatter.format(tx.amount, currencySymbol)
    val isExpense = tx.type == "EXPENSE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(transaction.categoryColorHex)).copy(alpha = 0.2f)
                        } catch (e: Exception) {
                            FinosMint.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Category,
                    contentDescription = "Category",
                    tint = try {
                        Color(android.graphics.Color.parseColor(transaction.categoryColorHex))
                    } catch (e: Exception) {
                        FinosMint
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchant.ifEmpty { tx.note },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.categoryName} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Amount
            Text(
                text = if (isExpense) "-$amountFormatted" else "+$amountFormatted",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) MaterialTheme.colorScheme.error else FinosMint
            )
        }
    }
}

@Composable
private fun FilteredTransactionsSummaryContent(
    transactions: List<TransactionWithCategory>,
    currencySymbol: String
) {
    val totalExpense = transactions.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
    val totalIncome = transactions.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount }
    val netAmount = totalIncome - totalExpense
    val transactionCount = transactions.size

    // Category breakdown
    val categoryBreakdown = transactions
        .groupBy { it.categoryName }
        .mapValues { (_, txList) -> txList.sumOf { it.transaction.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Transactions", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("$transactionCount", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Expense", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            text = CurrencyFormatter.format(totalExpense, currencySymbol),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Income", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            text = CurrencyFormatter.format(totalIncome, currencySymbol),
                            fontWeight = FontWeight.Medium,
                            color = FinosMint
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (netAmount >= 0)
                                "+${CurrencyFormatter.format(netAmount, currencySymbol)}"
                            else
                                "-${CurrencyFormatter.format(-netAmount, currencySymbol)}",
                            fontWeight = FontWeight.Bold,
                            color = if (netAmount >= 0) FinosMint else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Category Breakdown
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "By Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(categoryBreakdown) { (category, amount) ->
                val txWithCategory = transactions.find { it.categoryName == category }
                val percentage = if (totalExpense + totalIncome > 0)
                    (amount.toFloat() / (totalExpense + totalIncome) * 100).toInt()
                else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(txWithCategory?.categoryColorHex ?: "#6B7280")).copy(alpha = 0.2f)
                                } catch (e: Exception) {
                                    FinosMint.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = "Category",
                            tint = try {
                                Color(android.graphics.Color.parseColor(txWithCategory?.categoryColorHex ?: "#6B7280"))
                            } catch (e: Exception) {
                                FinosMint
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = try {
                                Color(android.graphics.Color.parseColor(txWithCategory?.categoryColorHex ?: "#6B7280"))
                            } catch (e: Exception) { FinosMint },
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.format(amount, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
