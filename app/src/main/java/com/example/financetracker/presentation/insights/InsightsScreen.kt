package com.example.financetracker.presentation.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState 
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.model.InsightsUiState
import com.example.financetracker.presentation.model.CategoryBreakdownUiModel
import com.example.financetracker.presentation.model.BudgetUiModel
import com.example.financetracker.presentation.model.SubscriptionUiModel
import com.example.financetracker.presentation.components.NotificationIconWithBadge
import com.example.financetracker.presentation.components.CategoryIcon
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost
import com.example.financetracker.core.util.AppLogger
import androidx.hilt.navigation.compose.hiltViewModel

// Local Model
// Local Model removed (Using DetectedSubscription)

@Composable
fun InsightsScreen(
    onSettingsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: InsightsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabs = listOf("Overview", "Budgets", "Subscriptions")

    val uiState by viewModel.uiState.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = uiState.error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track your spending patterns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FinosMint,
                edgePadding = 16.dp,
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
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) },
                        selectedContentColor = FinosMint,
                        unselectedContentColor = TextSecondaryDark
                    )
                }
            }
        }

        // State for Add/Edit Budget Sheet
        var showAddBudgetSheet by remember { mutableStateOf(false) }
        var editingBudget by remember { mutableStateOf<BudgetUiModel?>(null) }
        
        // State for Subscriptions
        var showAddSubSheet by remember { mutableStateOf(false) }
        var editingSubscription by remember { mutableStateOf<SubscriptionUiModel?>(null) }

        when (selectedTab) {
            0 -> OverviewContent(uiState)
            1 -> BudgetsContent(
                budgets = uiState.budgetStatus,
                onAddBudget = { 
                    editingBudget = null
                    showAddBudgetSheet = true 
                },
                onEditBudget = { budget ->
                    editingBudget = budget
                    showAddBudgetSheet = true
                },
                onDeleteBudget = { budget ->
                    viewModel.deleteBudget(budget.id)
                }
            )
            2 -> SubscriptionsContent(
                subscriptions = uiState.upcomingBills,
                totalFormatted = uiState.totalUpcomingBillsFormatted,
                onAdd = {
                    editingSubscription = null
                    showAddSubSheet = true
                },
                onEdit = { sub ->
                    editingSubscription = sub
                    showAddSubSheet = true
                },
                onDelete = { sub ->
                    viewModel.deleteSubscription(sub.id)
                }
            )
        }

        if (showAddBudgetSheet) {
            AddBudgetSheet(
                existingBudget = editingBudget,
                categories = uiState.categories,
                onDismiss = { 
                    showAddBudgetSheet = false 
                    editingBudget = null
                },//
                onSave = { id, cat, limit, col, roll ->
                    if (id != null) {
                        viewModel.updateBudget(id, cat, limit, col, roll)
                    } else {
                        viewModel.addBudget(cat, limit, col, roll)
                    }
                    showAddBudgetSheet = false
                    editingBudget = null
                }
            )
        }
        
        if (showAddSubSheet) {
            AddSubscriptionSheet(
                existingSubscription = editingSubscription,
                onDismiss = {
                    showAddSubSheet = false
                    editingSubscription = null
                },
                onSave = { id, merchant, amount, freq, nextDue, color ->
                    if (id != null && id > 0) {
                        viewModel.updateSubscription(id, merchant, amount, freq, nextDue, color)
                    } else {
                        viewModel.addSubscription(merchant, amount, freq, nextDue, color)
                    }
                    showAddSubSheet = false
                    editingSubscription = null
                }
            )
        }
    }
    } // Scaffold
}

@Composable
fun OverviewContent(uiState: InsightsUiState) {
    val styling = cardStyling()

    var showAllIncome by remember { mutableStateOf(false) }
    var showAllSpending by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Income/Spent Card (Note: Income is not in InsightsUiState, using placeholder)
        item {
            Card(
               modifier = Modifier.fillMaxWidth(),
               shape = RoundedCornerShape(24.dp),
               colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
               elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
               border = styling.border
            ) {
                Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                         Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 16.dp).background(FinosMint, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Income", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            }
                            Text(uiState.totalIncomeThisMonth, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 16.dp).background(FinosCoral, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spent", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            }
                            Text(uiState.totalSpentThisMonth, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    // Text("74%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) 
                }
            }
        }
        
        // 1.5 Income Breakdown
        if (uiState.incomeBreakdown.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Income Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    if (uiState.incomeBreakdown.size > 2) {
                        Text(
                            text = if (showAllIncome) "See Less" else "See All >", 
                            color = FinosMint, 
                            modifier = Modifier.clickable { showAllIncome = !showAllIncome }
                        )
                    }
                }
            }
            
            val incomeItems = if (showAllIncome) uiState.incomeBreakdown else uiState.incomeBreakdown.take(2)
            items(incomeItems) { item ->
                SpendingBreakdownRow(item)
            }
             item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // 2. Spending Breakdown
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                if (uiState.categoryBreakdown.size > 2) {
                    Text(
                        text = if (showAllSpending) "See Less" else "See All >", 
                        color = FinosMint,
                        modifier = Modifier.clickable { showAllSpending = !showAllSpending }
                    )
                }
            }
        }
        
        val spendingItems = if (showAllSpending) uiState.categoryBreakdown else uiState.categoryBreakdown.take(2)
        items(spendingItems) { item ->
            SpendingBreakdownRow(item)
        }

        // 4. Smart Tip
        item {
            val isDark = isDarkTheme()
            val containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFBEA)
            val borderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFFFD54F)
            val border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)

            val iconTint = Color(0xFFFBC02D) // Gold/Yellow
            val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
            val bodyColor = if (isDark) TextSecondaryDark else Color.Black

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = border
            ) {
                 Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                     Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                     Spacer(modifier = Modifier.width(16.dp))
                     Column {
                         Text("Coming Soon", fontWeight = FontWeight.Bold, color = titleColor) 
                         Spacer(modifier = Modifier.height(4.dp))
                         Text("We are working on bringing you personalized smart tips to help you save more.", style = MaterialTheme.typography.bodySmall, color = bodyColor, lineHeight = 18.sp)
                     }
                 }
            }
        }
    }
}

@Composable
fun SpendingBreakdownRow(item: com.example.financetracker.presentation.model.CategoryBreakdownUiModel) {
    val styling = cardStyling()

    // Parse Color
    val color = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch(e: Exception) {
        AppLogger.w("InsightsScreen", "Invalid color hex in SpendingBreakdownRow: ${item.colorHex}", e)
        FinosMint
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        categoryName = item.categoryName,
                        iconId = item.iconId,
                        colorHex = item.colorHex,
                        size = 36.dp,
                        useRoundedCorners = false
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.categoryName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${"%.1f".format(item.percentage)}% of spending", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    }
                }
                Text(item.amount, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) // Amount is formatted string
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { item.percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
fun BudgetsContent(
    budgets: List<com.example.financetracker.presentation.model.BudgetUiModel>,
    onAddBudget: () -> Unit,
    onEditBudget: (BudgetUiModel) -> Unit,
    onDeleteBudget: (BudgetUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (budgets.isEmpty()) {
             item { 
                 Text(
                     "No budgets set.", 
                     color = TextSecondaryDark, 
                     modifier = Modifier.padding(bottom = 16.dp)
                 ) 
             }
        } else {
            items(budgets) { budget ->
                BudgetCard(
                    budget = budget, 
                    onEdit = { onEditBudget(budget) },
                    onDelete = { onDeleteBudget(budget) }
                )
            }
        }

        // Add New Budget Button (Inline)
        item {
            val isDark = isDarkTheme()
            val borderColor = if (isDark) MaterialTheme.colorScheme.outline else TextSecondaryDark.copy(alpha = 0.5f)
            val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary

            OutlinedButton(
                onClick = onAddBudget,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(100), // Pill shape
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add budget")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Budget", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SubscriptionsContent(
    subscriptions: List<SubscriptionUiModel>,
    totalFormatted: String,
    onAdd: () -> Unit,
    onEdit: (SubscriptionUiModel) -> Unit,
    onDelete: (SubscriptionUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Upcoming Bills Header Card
        item {
            val isDark = isDarkTheme()
            val containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFBEA)
            val borderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFFFD54F)
            val border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)

            val icon = Icons.Outlined.Notifications
            val iconTint = Color(0xFFE65100)
            val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
            val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
            val dateColor = TextSecondaryDark

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = border
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(icon, contentDescription = "Upcoming bills", tint = iconTint, modifier = Modifier.size(20.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("Upcoming Bills", fontWeight = FontWeight.Bold, color = titleColor, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (subscriptions.isNotEmpty()) {
                        subscriptions.filter { !it.isDetected || (it.probability ?: 0f) > 0.8f }.take(2).forEach { sub ->
                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                  Text(sub.merchantName, fontWeight = FontWeight.Medium, color = contentColor) 
                                  Column(horizontalAlignment = Alignment.End) {
                                      Text(sub.amountFormatted, fontWeight = FontWeight.Bold, color = contentColor)
                                      Text("Due ${sub.nextDueDateFormatted}", style = MaterialTheme.typography.bodySmall, color = dateColor)
                                  }
                             }
                             Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        Text("No upcoming bills.", style = MaterialTheme.typography.bodySmall, color = contentColor)
                    }
                }
            }
        }
        
        item { Text("All Subscriptions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground) }
        
        items(subscriptions) { sub ->
            SubscriptionRow(
                sub = sub,
                onClick = { if (sub.isDetected) onEdit(sub) },
                onEdit = { onEdit(sub) },
                onDelete = { onDelete(sub) }
            )
        }

        // Add Button
        item {
            val isDark = isDarkTheme()
            val borderColor = if (isDark) MaterialTheme.colorScheme.outline else TextSecondaryDark.copy(alpha = 0.5f)
            val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary

            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Subscription", fontWeight = FontWeight.SemiBold)
            }
        }

        // Total Monthly Footer
        item {
            val styling = cardStyling()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = styling.border
            ) {
                Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Monthly", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Text("Total Monthly", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Text(totalFormatted, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    budget: BudgetUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val styling = cardStyling()

    var expanded by remember { mutableStateOf(false) }

    // Parse Color
    val color = try {
        Color(android.graphics.Color.parseColor(budget.colorHex))
    } catch(e: Exception) {
        AppLogger.w("InsightsScreen", "Invalid color hex in BudgetCard: ${budget.colorHex}", e)
        FinosMint
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { expanded = true }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(
                            categoryName = budget.categoryName,
                            iconId = 0, // Budget doesn't have iconId, will use name-based lookup
                            colorHex = budget.colorHex,
                            size = 40.dp,
                            useRoundedCorners = false
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                         Column {
                            Text(budget.categoryName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${budget.spentFormatted} of ${budget.limitFormatted}", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val progress = if (budget.limitAmount > 0) (budget.spentAmount.toFloat() / budget.limitAmount.toFloat() * 100).toInt() else 0
                        Text("$progress%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if(budget.spentAmount < budget.limitAmount) "+${budget.remainingFormatted}" else "Over", style = MaterialTheme.typography.bodySmall, color = FinosMint)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val progress = if (budget.limitAmount > 0) (budget.spentAmount.toFloat() / budget.limitAmount.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = color, 
                    trackColor = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Remaining: ${budget.remainingFormatted}", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    Text(
                        if(budget.isWarning) "Warning" else "Safe", 
                        color = if(budget.isWarning) FinosCoral else FinosMint, 
                        style = MaterialTheme.typography.bodySmall, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Dropdown Menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                 DropdownMenuItem(
                    text = { Text("Edit Budget") },
                    onClick = { 
                        expanded = false
                        onEdit() 
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Budget", color = Color.Red) },
                    onClick = { 
                        expanded = false
                        onDelete() 
                    }
                )
            }
        }
    }
}

@Composable
fun SubscriptionRow(
    sub: SubscriptionUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isDarkTheme()
    val styling = cardStyling()
    val border = if (isDark || !sub.isDetected) styling.border else androidx.compose.foundation.BorderStroke(1.dp, FinosMint.copy(alpha=0.5f))

    // Distinguish Detected
    val alpha = if (sub.isDetected) 0.9f else 1.0f

    // Parse Color
    val color = try {
        Color(android.graphics.Color.parseColor(sub.colorHex))
    } catch(e: Exception) {
        AppLogger.w("InsightsScreen", "Invalid color hex in SubscriptionRow: ${sub.colorHex}", e)
        FinosMint
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=alpha)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                if (!sub.isDetected) {
                     detectTapGestures(
                        onLongPress = { expanded = true },
                        onTap = { onClick() }
                     )
                } else {
                    detectTapGestures(onTap = { onClick() })
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = border
    ) {
        Box {
             Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(color.copy(alpha=0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = "Subscription due date", tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sub.merchantName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(sub.frequency + " Plan", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                         if (sub.isDetected) {
                             Spacer(modifier = Modifier.width(8.dp))
                             Box(modifier = Modifier.background(FinosMint.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp, vertical=2.dp)) {
                                 Text("New", style = MaterialTheme.typography.labelSmall, color = FinosMint)
                             }
                         }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(sub.amountFormatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Due " + sub.nextDueDateFormatted, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                }
            }
            
            // Context Menu for Verified only
            if (!sub.isDetected) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                     DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { 
                            expanded = false
                            onEdit() 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { 
                            expanded = false
                            onDelete() 
                        }
                    )
                }
            }
        }
    }
}


