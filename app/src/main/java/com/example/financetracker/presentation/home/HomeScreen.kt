package com.example.financetracker.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.presentation.model.TransactionUiModel
import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.NotificationIconWithBadge
import com.example.financetracker.presentation.components.CategoryIcon
import com.example.financetracker.presentation.components.CategoryIconWithBadge
import java.util.Locale
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost
import com.example.financetracker.core.util.TextHighlightUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onTransactionClick: (TransactionUiModel) -> Unit = {},
    viewModel: com.example.financetracker.presentation.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val dims = LocalDimensions.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAmountsHidden by viewModel.isAmountsHidden.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = uiState.error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FinosMint)
                }
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dims.lg),
                verticalArrangement = Arrangement.spacedBy(dims.lg)
            ) {
            // 1. Header
            item {
                Spacer(modifier = Modifier.height(dims.lg))
                HomeHeader(
                    userName = uiState.userName,
                    userImagePath = uiState.userImagePath,
                    unreadNotificationCount = uiState.unreadNotificationCount,
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onNotificationClick = onNotificationClick
                )
            }

            // 2. Filters
            item {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::onFilterSelected
                )
            }
            // 2b. Trip Banner
            if (uiState.isTripModeActive) {
                item {
                    TripBanner()
                }
            }

            // 3. Net Worth Card
            item {
                NetWorthCard(
                    netWorthFormatted = uiState.totalNetWorth,
                    changePercentage = uiState.netWorthChangePercentage,
                    currencySymbol = uiState.currencySymbol,
                    isAmountsHidden = isAmountsHidden,
                    onToggleHidden = { viewModel.toggleAmountsHidden() }
                )
            }

            // 4. Income/Spent Donut
            item {
                IncomeSpentCard(
                    income = uiState.income,
                    expense = uiState.expense,
                    incomeFormatted = uiState.incomeThisMonth,
                    expenseFormatted = uiState.expenseThisMonth,
                    currencySymbol = uiState.currencySymbol,
                    isAmountsHidden = isAmountsHidden
                )
            }

            // 5. Accounts Row
            item {
                AccountsRow(uiState.wallets, uiState.currencySymbol, isAmountsHidden)
            }

            // 6. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onSeeAllClick) {
                        Text("See All >", color = TextSecondaryDark)
                    }
                }
            }

            // 7. Transaction List
            val displayTransactions = uiState.recentTransactions.take(5)
            if (displayTransactions.isEmpty()) {
                item {
                    Text("No transactions yet.", color = TextSecondaryDark, modifier = Modifier.padding(vertical = dims.lg))
                }
            } else {
                items(displayTransactions) { uiModel ->
                    TransactionRowItem(uiModel, uiState.currencySymbol, isAmountsHidden, onLongClick = { onTransactionClick(uiModel) })
                    Spacer(modifier = Modifier.height(dims.sm))
                }
            }

            item { Spacer(modifier = Modifier.height(dims.fabClearance)) } // Bottom Padding for FAB
            }
            } // end else
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    userImagePath: String?,
    unreadNotificationCount: Int = 0,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val dims = LocalDimensions.current
    val displayName = if (userName.isNotEmpty()) {
        userName.split(" ").firstOrNull() ?: userName
    } else {
        "User"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(dims.avatarMd)
                    .background(FinosMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (userImagePath != null) {
                    AsyncImage(
                        model = userImagePath,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = userName
                        .split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "?" }
                    Text(
                        initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(dims.md))
            Column {
                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondaryDark
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dims.sm)) {
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
}

@Composable
fun FilterChipsRow(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val dims = LocalDimensions.current
    val filters = listOf("All", "Daily", "Weekly", "Monthly")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.sm)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null,
                shape = RoundedCornerShape(dims.xxl),
                modifier = Modifier.height(dims.xxxl)
            )
        }
    }
}

@Composable
fun NetWorthCard(netWorthFormatted: String, changePercentage: Double, currencySymbol: String, isAmountsHidden: Boolean, onToggleHidden: () -> Unit) {
    val dims = LocalDimensions.current
    val styling = heroCardStyling()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.heroRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border
    ) {
        Column(modifier = Modifier.padding(dims.xxl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Net Worth", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (isAmountsHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = FinosMint,
                        modifier = Modifier.size(dims.iconMd)
                    )
                }
            }
            Spacer(modifier = Modifier.height(dims.sm))
            Text(
                text = if (isAmountsHidden) "$currencySymbol ●●●●●●" else netWorthFormatted,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(dims.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val changeSymbol = if (changePercentage >= 0) "+" else ""
                val changeColor = if (changePercentage >= 0) FinosMint else FinosCoral
                val changeIcon = if (changePercentage >= 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown

                Icon(
                    changeIcon,
                    contentDescription = if (changePercentage >= 0) "Trending up" else "Trending down",
                    tint = changeColor,
                    modifier = Modifier.size(dims.iconXs)
                )
                Spacer(modifier = Modifier.width(dims.xs))
                Text(
                    text = "${changeSymbol}${String.format("%.1f", changePercentage)}% this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }
        }
    }
}

@Composable
fun IncomeSpentCard(income: Long, expense: Long, incomeFormatted: String, expenseFormatted: String, currencySymbol: String, isAmountsHidden: Boolean) {
    val dims = LocalDimensions.current
    val styling = heroCardStyling()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.heroRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border
    ) {
        Row(
            modifier = Modifier
                .padding(dims.xxl)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dims.lg)) {
                // Income
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(dims.xs, dims.lg).background(FinosMint, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(dims.sm))
                        Text("Income", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    }
                    Text(
                        text = if (isAmountsHidden) "$currencySymbol ●●●●●●" else incomeFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Spent
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(dims.xs, dims.lg).background(FinosCoral, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(dims.sm))
                        Text("Spent", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    }
                    Text(
                        text = if (isAmountsHidden) "$currencySymbol ●●●●●●" else expenseFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Donut Chart
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(dims.donutChartSize)) {
                Canvas(modifier = Modifier.size(dims.fabClearance)) {
                    val strokeWidth = dims.md.toPx()
                    val total = (income + expense).toFloat().coerceAtLeast(1f)
                    val incomeSweep = (income / total) * 360f
                    val expenseSweep = (expense / total) * 360f

                    drawArc(
                        color = FinosCoral,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = FinosMint,
                        startAngle = -90f,
                        sweepAngle = incomeSweep,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = FinosCoral,
                        startAngle = -90f + incomeSweep,
                        sweepAngle = expenseSweep,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsRow(wallets: List<com.example.financetracker.presentation.model.AccountUiModel>, currencySymbol: String, isAmountsHidden: Boolean) {
    val dims = LocalDimensions.current
    val isDark = isDarkTheme()
    val styling = cardStyling()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dims.md),
        contentPadding = PaddingValues(horizontal = dims.xs)
    ) {
        items(wallets) { account ->
            val containerColor = if (isDark) MaterialTheme.colorScheme.surface else account.color
            val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary

            Card(
                modifier = Modifier.width(dims.donutChartSize + dims.xxxl).height(dims.donutChartSize),
                shape = RoundedCornerShape(dims.lg),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
                border = styling.border
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dims.md),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val iconBg = if (isDark) account.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
                    val iconTint = if (isDark) account.color else TextPrimary

                    Icon(
                        imageVector = if(account.type == "Cash") Icons.Rounded.ShoppingBag else Icons.AutoMirrored.Rounded.CallMade,
                        contentDescription = "Account type",
                        tint = iconTint,
                        modifier = Modifier
                            .size(dims.iconLg)
                            .background(iconBg, CircleShape)
                            .padding(6.dp)
                    )
                    Column {
                        Text(account.name, style = MaterialTheme.typography.bodySmall, color = textColor)
                        Text(
                            text = if (isAmountsHidden) "$currencySymbol ●●●●●●" else account.balanceFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    transaction: TransactionUiModel,
    currencySymbol: String,
    isAmountsHidden: Boolean = false,
    searchQuery: String = "",
    onLongClick: () -> Unit = {}
) {
    val dims = LocalDimensions.current
    val isDark = isDarkTheme()
    val containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val styling = listItemCardStyling()
    val highlightColor = FinosMint.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(dims.cardRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border
    ) {
        Row(
            modifier = Modifier
                .padding(dims.lg)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon - using CategoryIconWithBadge composable
            CategoryIconWithBadge(
                categoryName = transaction.category,
                iconId = transaction.categoryIconId,
                colorHex = transaction.categoryColorHex,
                size = dims.iconXl,
                useRoundedCorners = true,
                attachmentCount = transaction.attachmentCount
            )

            Spacer(modifier = Modifier.width(dims.lg))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TextHighlightUtil.buildHighlightedText(transaction.title, searchQuery, highlightColor),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = TextHighlightUtil.buildHighlightedText(transaction.category, searchQuery, highlightColor),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isAmountsHidden) "$currencySymbol ●●●●●●" else transaction.amountFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(transaction.amountColor)
                )
                Text(
                    text = transaction.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }
        }
    }
}



@Composable
fun TripBanner() {
    val dims = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.lg),
        colors = CardDefaults.cardColors(containerColor = FinosMint.copy(alpha = 0.1f)),
        border = accentBorder(FinosMint)
    ) {
        Row(
            modifier = Modifier.padding(dims.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dims.iconXl - dims.sm)
                    .background(FinosMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Flight,
                    contentDescription = "Trip Mode",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(dims.lg))
            Column {
                Text(
                    text = "Trip Mode Active",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tracking expenses for 'Goa Trip'",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }
        }
    }
}
