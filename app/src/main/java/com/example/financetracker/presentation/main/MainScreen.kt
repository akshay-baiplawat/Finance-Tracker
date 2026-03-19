package com.example.financetracker.presentation.main

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financetracker.presentation.add_transaction.AddTransactionSheet
import com.example.financetracker.presentation.home.HomeScreen
import com.example.financetracker.presentation.insights.InsightsScreen
import com.example.financetracker.presentation.ledger.LedgerScreen
import com.example.financetracker.presentation.notifications.NotificationScreen
import com.example.financetracker.presentation.profile.PrivacyPolicyScreen
import com.example.financetracker.presentation.profile.ProfileScreen
import com.example.financetracker.presentation.profile.UserProfileScreen
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.LocalDimensions
import com.example.financetracker.presentation.wallet.WalletScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.presentation.dashboard.DashboardViewModel
import com.example.financetracker.presentation.group.GroupViewModel
import com.example.financetracker.presentation.ledger.LedgerViewModel
import com.example.financetracker.presentation.insights.InsightsViewModel
import com.example.financetracker.presentation.group.AddGroupExpenseSheet
import com.example.financetracker.presentation.group.SettleUpSheet
import com.example.financetracker.presentation.group.AddContributionSheet
import com.example.financetracker.presentation.model.SettlementSuggestion
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.domain.model.TransactionWithCategory
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.attachment.AttachmentViewerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialRoute: String? = null,
    initialTab: Int = 0,
    onInitialRouteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val viewModel: DashboardViewModel = hiltViewModel()
    val groupViewModel: GroupViewModel = hiltViewModel()
    val ledgerViewModel: LedgerViewModel = hiltViewModel()
    val insightsViewModel: InsightsViewModel = hiltViewModel()
    val isTripMode by viewModel.isTripMode.collectAsState()

    var showAddTransaction by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<com.example.financetracker.presentation.model.TransactionUiModel?>(null) }

    // Attachment states
    var selectedTransactionAttachments by remember { mutableStateOf<List<AttachmentUiModel>>(emptyList()) }
    var attachmentsForViewer by remember { mutableStateOf<List<TransactionAttachmentEntity>>(emptyList()) }
    var showAttachmentViewer by remember { mutableStateOf(false) }
    var attachmentViewerInitialIndex by remember { mutableIntStateOf(0) }

    // Load attachments when a transaction is selected
    LaunchedEffect(selectedTransaction?.id) {
        selectedTransaction?.id?.let { transactionId ->
            val attachments = viewModel.getAttachmentsForTransaction(transactionId)
            selectedTransactionAttachments = attachments
            // Also load entities for the viewer
            attachmentsForViewer = viewModel.getAttachmentEntities(transactionId)
        } ?: run {
            selectedTransactionAttachments = emptyList()
            attachmentsForViewer = emptyList()
        }
    }

    // Group sheet states
    var showAddGroupExpenseSheet by remember { mutableStateOf(false) }
    var showSettleUpSheet by remember { mutableStateOf(false) }
    var showAddContributionSheet by remember { mutableStateOf(false) }
    var selectedSettlementSuggestion by remember { mutableStateOf<SettlementSuggestion?>(null) }
    var selectedGroupIdForSheet by remember { mutableStateOf<Long?>(null) }
    val addGroupExpenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settleUpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addContributionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Group data for sheets
    val groupDetail by groupViewModel.selectedGroupDetail.collectAsState()
    val wallets by groupViewModel.wallets.collectAsState()
    val currencySymbol by groupViewModel.currencySymbol.collectAsState()

    // Handle initial route from notification click
    LaunchedEffect(initialRoute, initialTab) {
        if (initialRoute != null && initialRoute != "home") {
            // Set the appropriate tab before navigating
            when (initialRoute) {
                "ledger" -> ledgerViewModel.selectTab(initialTab)
                "insights" -> insightsViewModel.selectTab(initialTab)
            }
            navController.navigate(initialRoute) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onInitialRouteConsumed()
        }
    }

    // Show Bottom Bar only on main tabs
    val bottomBarRoutes = listOf("home", "ledger", "wallet", "insights")
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(bottom = if (showBottomBar) 0.dp else 0.dp) 
            ) {
                composable("home") {
                    ContentWrapper(showBottomBar) {
                        HomeScreen(
                            onSettingsClick = { navController.navigate("profile") },
                            onNotificationClick = { navController.navigate("notifications") },
                            onProfileClick = { navController.navigate("user") },
                            onTransactionClick = { selectedTransaction = it },
                            onSeeAllClick = {
                                navController.navigate("ledger") {
                                    // Treat as tab switch
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
                composable("ledger") {
                    ContentWrapper(showBottomBar) {
                        LedgerScreen(
                            onTransactionLongClick = { selectedTransaction = it },
                            onSettingsClick = { navController.navigate("profile") },
                            onNotificationClick = { navController.navigate("notifications") },
                            onGroupClick = { groupId -> navController.navigate("groupDetail/$groupId") },
                            viewModel = ledgerViewModel
                        )
                    }
                }
                composable("wallet") {
                     ContentWrapper(showBottomBar) {
                         WalletScreen(
                             onSettingsClick = { navController.navigate("profile") },
                             onNotificationClick = { navController.navigate("notifications") }
                         )
                     }
                }
                composable("insights") {
                     ContentWrapper(showBottomBar) {
                         InsightsScreen(
                             onSettingsClick = { navController.navigate("profile") },
                             onNotificationClick = { navController.navigate("notifications") },
                             viewModel = insightsViewModel
                         )
                     }
                }
                composable("profile") {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onCategoriesClick = { navController.navigate("categories") },
                        onUserProfileClick = { navController.navigate("user") },
                        onPrivacyPolicyClick = { navController.navigate("privacyPolicy") }
                    )
                }
                composable("user") {
                    UserProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("privacyPolicy") {
                    PrivacyPolicyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("categories") {
                    com.example.financetracker.presentation.category.CategoryListScreen(onBack = { navController.popBackStack() })
                }
                composable("notifications") {
                    NotificationScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToLedger = {
                            ledgerViewModel.selectTab(0) // All Transactions tab
                            navController.popBackStack("notifications", inclusive = true)
                            navController.navigate("ledger") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToInsightsBudgets = {
                            insightsViewModel.selectTab(1) // Budgets tab
                            navController.popBackStack("notifications", inclusive = true)
                            navController.navigate("insights") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToInsightsSubscriptions = {
                            insightsViewModel.selectTab(2) // Subscriptions tab
                            navController.popBackStack("notifications", inclusive = true)
                            navController.navigate("insights") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToLedgerPeople = {
                            ledgerViewModel.selectTab(1) // People tab
                            navController.popBackStack("notifications", inclusive = true)
                            navController.navigate("ledger") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToLedgerEvents = {
                            ledgerViewModel.selectTab(2) // Groups tab
                            navController.popBackStack("notifications", inclusive = true)
                            navController.navigate("ledger") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("groupDetail/{groupId}") { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId")?.toLongOrNull() ?: 0L
                    com.example.financetracker.presentation.group.GroupDetailScreen(
                        groupId = groupId,
                        onBackClick = { navController.popBackStack() },
                        onAddExpenseClick = {
                            selectedGroupIdForSheet = groupId
                            groupViewModel.selectGroup(groupId)
                            showAddGroupExpenseSheet = true
                        },
                        onSettleUpClick = { suggestion ->
                            selectedGroupIdForSheet = groupId
                            selectedSettlementSuggestion = suggestion
                            showSettleUpSheet = true
                        },
                        onAddContributionClick = {
                            selectedGroupIdForSheet = groupId
                            groupViewModel.selectGroup(groupId)
                            showAddContributionSheet = true
                        },
                        onEditTransaction = { txWithCategory ->
                            // Convert TransactionWithCategory to TransactionUiModel
                            val tx = txWithCategory.transaction
                            val uiModel = com.example.financetracker.presentation.model.TransactionUiModel(
                                id = tx.id,
                                title = tx.note.ifEmpty { tx.merchant },
                                amount = tx.amount,
                                amountFormatted = CurrencyFormatter.format(tx.amount, currencySymbol),
                                amountColor = if (tx.type == "EXPENSE") android.graphics.Color.RED else android.graphics.Color.GREEN,
                                date = java.time.Instant.ofEpochMilli(tx.timestamp)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate(),
                                dateFormatted = "",
                                category = txWithCategory.categoryName,
                                categoryColorHex = txWithCategory.categoryColorHex,
                                type = tx.type,
                                merchant = tx.merchant,
                                timestamp = tx.timestamp,
                                walletId = tx.walletId,
                                smsId = tx.smsId,
                                personId = tx.personId,
                                groupId = tx.groupId
                            )
                            selectedTransaction = uiModel
                        }
                    )
                }
            }
            
            // Custom Overlapping Bottom Bar
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // 1. The Bar
                    FinOSBottomBar(
                        currentRoute = currentRoute ?: "home",
                        onNavigate = { route ->
                            // Reset tab to first tab when navigating via bottom bar
                            when (route) {
                                "ledger" -> ledgerViewModel.resetTab()
                                "insights" -> insightsViewModel.resetTab()
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onFabClick = { showAddTransaction = true }
                    )

                    // 2. The FAB (Overlapping)
                    val dims = LocalDimensions.current
                    FloatingActionButton(
                        onClick = { showAddTransaction = true },
                        containerColor = FinosMint,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter) // Align to top of the container
                            .offset(y = -(dims.avatarXs)) // Pull up
                            .size(dims.fabSize)
                            .border(dims.xs, MaterialTheme.colorScheme.background, CircleShape) // Matches Scaffold bg
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            modifier = Modifier.size(dims.iconLg),
                            tint = Color.White
                        )
                    }
                }
            }
            
            if (showAddTransaction) {
                AddTransactionSheet(
                    onDismiss = { showAddTransaction = false },
                    onAdd = { amount, category, type, paymentType, note, timestamp, attachmentUris ->
                        viewModel.addTransaction(amount, category, type, paymentType, note, timestamp, attachmentUris)
                        showAddTransaction = false
                    },
                    isTripMode = isTripMode
                )
            }

            // Edit Transaction Sheet
            selectedTransaction?.let { transaction ->
                com.example.financetracker.presentation.add_transaction.EditTransactionSheet(
                    transaction = transaction,
                    existingAttachments = selectedTransactionAttachments,
                    wallets = wallets.take(3),
                    onDismiss = { selectedTransaction = null },
                    onDelete = { id ->
                        viewModel.deleteTransaction(id)
                        selectedTransaction = null
                    },
                    onUpdate = { id, amount, note, category, type, timestamp, walletId, newAttachmentUris, deletedAttachmentIds ->
                        viewModel.updateTransaction(
                            id = id,
                            amount = amount,
                            note = note,
                            category = category,
                            type = type,
                            timestamp = timestamp,
                            walletId = walletId,
                            personId = transaction.personId,
                            groupId = transaction.groupId,
                            smsId = transaction.smsId,
                            newAttachmentUris = newAttachmentUris,
                            deletedAttachmentIds = deletedAttachmentIds
                        )
                        selectedTransaction = null
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

            // Group Expense Sheet
            if (showAddGroupExpenseSheet && selectedGroupIdForSheet != null) {
                val members = groupDetail?.members?.mapNotNull { member ->
                    if (!member.isSelf && member.personId != null) {
                        PersonEntity(id = member.personId, name = member.name, currentBalance = 0L)
                    } else null
                } ?: emptyList()

                val isGeneralGroup = groupDetail?.group?.type == com.example.financetracker.data.local.entity.GroupType.GENERAL.name

                AddGroupExpenseSheet(
                    sheetState = addGroupExpenseSheetState,
                    groupId = selectedGroupIdForSheet!!,
                    isGeneralGroup = isGeneralGroup,
                    members = members,
                    wallets = wallets,
                    currencySymbol = currencySymbol,
                    onDismiss = {
                        showAddGroupExpenseSheet = false
                        selectedGroupIdForSheet = null
                    },
                    onSave = { amount, note, paidByPersonId, paidBySelf, splitType, splits, categoryName, walletId, timestamp, attachmentUris ->
                        groupViewModel.addExpense(
                            groupId = selectedGroupIdForSheet!!,
                            amount = amount,
                            note = note,
                            categoryName = categoryName,
                            paidByPersonId = paidByPersonId,
                            paidBySelf = paidBySelf,
                            splitType = splitType,
                            splits = splits,
                            walletId = walletId,
                            timestamp = timestamp,
                            attachmentUris = attachmentUris
                        )
                    },
                    onSaveSimple = if (isGeneralGroup) { amount, note, categoryName, transactionType, timestamp, attachmentUris ->
                        groupViewModel.addSimpleTransaction(
                            groupId = selectedGroupIdForSheet!!,
                            amount = amount,
                            note = note,
                            categoryName = categoryName,
                            transactionType = transactionType,
                            timestamp = timestamp,
                            attachmentUris = attachmentUris
                        )
                    } else null
                )
            }

            // Settle Up Sheet
            if (showSettleUpSheet && selectedGroupIdForSheet != null && selectedSettlementSuggestion != null) {
                SettleUpSheet(
                    sheetState = settleUpSheetState,
                    groupId = selectedGroupIdForSheet!!,
                    suggestion = selectedSettlementSuggestion!!,
                    currencySymbol = currencySymbol,
                    onDismiss = {
                        showSettleUpSheet = false
                        selectedGroupIdForSheet = null
                        selectedSettlementSuggestion = null
                    },
                    onSave = { fromPersonId, fromSelf, toPersonId, toSelf, amount, note ->
                        groupViewModel.recordSettlement(
                            groupId = selectedGroupIdForSheet!!,
                            fromPersonId = fromPersonId,
                            fromSelf = fromSelf,
                            toPersonId = toPersonId,
                            toSelf = toSelf,
                            amount = amount,
                            note = note
                        )
                    }
                )
            }

            // Add Contribution Sheet (for Savings Goals)
            if (showAddContributionSheet && selectedGroupIdForSheet != null) {
                val members = groupDetail?.members?.mapNotNull { member ->
                    if (!member.isSelf && member.personId != null) {
                        PersonEntity(id = member.personId, name = member.name, currentBalance = 0L)
                    } else null
                } ?: emptyList()

                AddContributionSheet(
                    sheetState = addContributionSheetState,
                    groupId = selectedGroupIdForSheet!!,
                    members = members,
                    currencySymbol = currencySymbol,
                    onDismiss = {
                        showAddContributionSheet = false
                        selectedGroupIdForSheet = null
                    },
                    onSave = { amount, memberId, isSelf, note, timestamp ->
                        groupViewModel.addContribution(
                            groupId = selectedGroupIdForSheet!!,
                            amount = amount,
                            memberId = memberId,
                            isSelf = isSelf,
                            note = note,
                            timestamp = timestamp
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ContentWrapper(showBottomBar: Boolean, content: @Composable () -> Unit) {
    val dims = LocalDimensions.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = if (showBottomBar) dims.fabClearance else 0.dp) // Reserve space for bar
    ) {
        content()
    }
}

@Composable
fun FinOSBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val dims = LocalDimensions.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = FinosMint,
        tonalElevation = dims.sm
    ) {
        val items = listOf(
            BottomNavItem("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
            BottomNavItem("ledger", "Ledger", Icons.Rounded.Book, Icons.Outlined.Book),
            // Middle Placeholder for FAB
            BottomNavItem("fab_placeholder", "", null, null),
            BottomNavItem("wallet", "Wallet", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            BottomNavItem("insights", "Insights", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
        )

        items.forEach { item ->
            if (item.route == "fab_placeholder") {
                // Disabled middle item for spacing
                 NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Box(modifier = Modifier.size(dims.iconMd)) }, // Invisible spacer
                    enabled = false,
                    label = { Text("") }
                )
            } else {
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
                        icon?.let { Icon(it, contentDescription = item.label) }
                    },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FinosMint,
                        selectedTextColor = FinosMint,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f)
                    )
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String, 
    val label: String, 
    val selectedIcon: ImageVector?,
    val unselectedIcon: ImageVector?
)
