package com.example.financetracker.presentation.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

import com.example.financetracker.core.util.DateUtil
import com.example.financetracker.presentation.home.TransactionRowItem

import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.NotificationIconWithBadge
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost

import com.example.financetracker.presentation.model.EventUiModel
import com.example.financetracker.presentation.model.PersonUiModel
import com.example.financetracker.presentation.group.GroupViewModel
import com.example.financetracker.presentation.group.GroupsContent
import com.example.financetracker.presentation.group.AddGroupSheet
import com.example.financetracker.presentation.wallet.WalletViewModel
import com.example.financetracker.presentation.wallet.PeopleContent
import com.example.financetracker.presentation.wallet.AddPersonSheet

// Local Model Removed (Moved to UiModels.kt)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onTransactionLongClick: (com.example.financetracker.presentation.model.TransactionUiModel) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    viewModel: LedgerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabs = listOf("All Transactions", "People", "Groups")
    var showAddGroupSheet by remember { mutableStateOf(false) }
    val addGroupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // People sheet states
    var showAddPerson by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<PersonUiModel?>(null) }

    // Search State
    val searchText by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    // Groups State
    val groups by groupViewModel.groups.collectAsState()
    val availablePeople by groupViewModel.availablePeople.collectAsState()
    val availableCategories by groupViewModel.availableCategories.collectAsState()

    // People State from WalletViewModel
    val walletUiState by walletViewModel.uiState.collectAsState()

    // Add Person Sheet
    if (showAddPerson) {
        AddPersonSheet(
            onDismiss = { showAddPerson = false },
            onSave = { name, amount ->
                walletViewModel.addPerson(name, amount)
                showAddPerson = false
            }
        )
    }

    // Edit Person Sheet
    if (editingPerson != null) {
        AddPersonSheet(
            existingPerson = editingPerson,
            onDismiss = { editingPerson = null },
            onSave = { name, amount ->
                walletViewModel.updatePerson(editingPerson!!.id, name, amount)
                editingPerson = null
            }
        )
    }

    // Add Group Sheet
    if (showAddGroupSheet) {
        AddGroupSheet(
            sheetState = addGroupSheetState,
            availablePeople = availablePeople,
            availableCategories = availableCategories,
            onDismiss = { showAddGroupSheet = false },
            onAddNewPerson = {
                showAddGroupSheet = false
                showAddPerson = true  // Show AddPersonSheet instead of navigating
            },
            onSave = { name, type, budgetLimit, targetAmount, colorHex, memberIds, filterCategoryIds, filterStartDate, filterEndDate ->
                groupViewModel.createGroup(
                    name = name,
                    type = type,
                    budgetLimit = budgetLimit,
                    targetAmount = targetAmount,
                    colorHex = colorHex,
                    memberIds = memberIds,
                    filterCategoryIds = filterCategoryIds,
                    filterStartDate = filterStartDate,
                    filterEndDate = filterEndDate
                )
            }
        )
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header & Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 48.dp) // Status bar space
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Ledger",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationIconWithBadge(
                        unreadCount = unreadNotificationCount,
                        onClick = onNotificationClick
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FinosMint,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FinosMint,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) FinosMint else TextSecondaryDark,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        // 2. Content
        when (selectedTab) {
            0 -> {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FinosMint)
                    }
                } else {
                    AllTransactionsContent(
                        searchText = searchText,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        selectedFilter = selectedFilter,
                        onFilterChange = { viewModel.updateFilter(it) },
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        onTransactionLongClick = onTransactionLongClick
                    )
                }
            }
            1 -> PeopleContent(
                uiState = walletUiState,
                onAddClick = { showAddPerson = true },
                onEdit = { editingPerson = it },
                onDelete = { walletViewModel.deletePerson(it.id) }
            )
            2 -> GroupsContent(
                groups = groups,
                onGroupClick = onGroupClick,
                onAddGroupClick = { showAddGroupSheet = true }
            )
        }
    }
    } // Scaffold
}

@Composable
fun AllTransactionsContent(
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    transactions: List<com.example.financetracker.presentation.model.TransactionUiModel>,
    currencySymbol: String,
    onTransactionLongClick: (com.example.financetracker.presentation.model.TransactionUiModel) -> Unit
) {
    // Group by date (LocalDate) to handle date+time in dateFormatted
    val groupedTransactions = remember(transactions) {
        transactions.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }
    val isDark = isDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        item {
            TextField(
                value = searchText,
                onValueChange = onSearchChange,
                placeholder = { Text("Search transactions...", color = TextSecondaryDark) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondaryDark) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                         if(isDark) 1.dp else 0.dp, 
                         if(isDark) DarkSurfaceHighlight else Color.Transparent, 
                         RoundedCornerShape(16.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        // Filter Chips (Visual Only for Phase 4)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("All", "Income", "Expense", "Transfer")
                filters.forEach { filter ->
                    FilterChip(
                        selected = (filter == selectedFilter),
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FinosMint,
                            selectedLabelColor = Color.White,
                            containerColor = if(isDark) DarkSurfaceHighlight else Color.White.copy(alpha=0.5f), 
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Transactions Grouped
        if (transactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = TextSecondaryDark)
                }
            }
        } else {
            groupedTransactions.forEach { (date, txs) ->
                item {
                    // Format date-only header using timestamp from first transaction
                    val dateHeader = txs.firstOrNull()?.timestamp?.let {
                        DateUtil.formatDateOnly(it)
                    } ?: date.toString()
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(txs) { transaction ->
                    TransactionRowItem(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        searchQuery = searchText,
                        onLongClick = { onTransactionLongClick(transaction) }
                    )
                }
            }
        }
    }
}
