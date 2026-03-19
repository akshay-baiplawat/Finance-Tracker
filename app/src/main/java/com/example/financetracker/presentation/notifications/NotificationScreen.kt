package com.example.financetracker.presentation.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToLedger: () -> Unit = {},
    onNavigateToInsightsBudgets: () -> Unit = {},
    onNavigateToInsightsSubscriptions: () -> Unit = {},
    onNavigateToLedgerPeople: () -> Unit = {},
    onNavigateToLedgerEvents: () -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = uiState.error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    val filteredNotifications = remember(uiState.notifications, selectedFilter) {
        if (selectedFilter == 0) {
            uiState.notifications
        } else {
            uiState.notifications.filter { it.isUnread }
        }
    }

    // Helper function to navigate based on notification
    fun navigateForNotification(notification: NotificationUiModel) {
        when (notification.referenceType) {
            "TRANSACTION" -> onNavigateToLedger()
            "BUDGET" -> onNavigateToInsightsBudgets()
            "SUBSCRIPTION" -> onNavigateToInsightsSubscriptions()
            "PERSON" -> onNavigateToLedgerPeople()
            "WEEKLY_REMINDER" -> onNavigateToLedgerPeople()
            "EVENT" -> onNavigateToLedgerEvents()
            else -> {
                // Fallback based on notification type
                when (notification.type) {
                    NotificationType.INCOME, NotificationType.EXPENSE -> onNavigateToLedger()
                    NotificationType.BUDGET_ALERT -> onNavigateToInsightsBudgets()
                    NotificationType.UPCOMING_BILL -> onNavigateToInsightsSubscriptions()
                    NotificationType.DEBT -> onNavigateToLedgerPeople()
                    NotificationType.TRIP -> onNavigateToLedgerEvents()
                    else -> {} // Do nothing for SUMMARY
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Mark all read", color = FinosMint)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Filter Row
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "All" Pill
                FilterPill(
                    text = "All",
                    isSelected = selectedFilter == 0,
                    onClick = { viewModel.selectFilter(0) }
                )

                // "Unread" Pill with count
                FilterPill(
                    text = "Unread",
                    isSelected = selectedFilter == 1,
                    count = uiState.unreadCount.takeIf { it > 0 },
                    onClick = { viewModel.selectFilter(1) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FinosMint)
                }
            } else if (filteredNotifications.isEmpty()) {
                EmptyNotificationsView(
                    isFiltered = selectedFilter == 1
                )
            } else {
                LazyColumn(
                    modifier = Modifier.animateContentSize()
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notification ->
                        SwipeToDeleteNotificationItem(
                            notification = notification,
                            onClick = {
                                if (notification.isUnread) {
                                    viewModel.markAsRead(notification.id)
                                }
                                navigateForNotification(notification)
                            },
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                        HorizontalDivider(
                            color = if (isSystemInDarkTheme())
                                DarkSurfaceHighlight
                            else
                                Color.LightGray.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    isSelected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (isDark) FinosMint.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        TextSecondaryDark
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (count != null) 16.dp else 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = textColor, fontWeight = FontWeight.Medium)
            if (count != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(FinosMint, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView(isFiltered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = "No notifications",
            modifier = Modifier.size(64.dp),
            tint = TextSecondaryDark.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFiltered) "No unread notifications" else "No notifications yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltered) {
                "All caught up! Check the \"All\" tab for history."
            } else {
                "Notifications about your finances will appear here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNotificationItem(
    notification: NotificationUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart ||
                dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> FinosCoral
                    else -> Color.Transparent
                },
                label = "swipe_bg_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else
                    Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        content = {
            NotificationItem(
                notification = notification,
                onClick = onClick
            )
        }
    )
}

@Composable
fun NotificationItem(
    notification: NotificationUiModel,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (notification.isUnread) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Box
        val (icon, badgeBgBase, iconColor) = when (notification.type) {
            NotificationType.INCOME -> Triple(Icons.Rounded.ArrowDownward, Color(0xFFE8F5E9), FinosMint)
            NotificationType.BUDGET_ALERT -> Triple(Icons.Rounded.Warning, Color(0xFFFFF8E1), Color(0xFFFFC107))
            NotificationType.UPCOMING_BILL -> Triple(Icons.Rounded.CalendarToday, Color(0xFFE3F2FD), Color(0xFF4285F4))
            NotificationType.DEBT -> Triple(Icons.Rounded.Person, Color(0xFFF3E5F5), Color(0xFFAB47BC))
            NotificationType.TRIP -> Triple(Icons.Rounded.Flight, Color(0xFFE1F5FE), Color(0xFF29B6F6))
            NotificationType.EXPENSE -> Triple(Icons.Rounded.ArrowUpward, Color(0xFFFFEBEE), FinosCoral)
            NotificationType.SUMMARY -> Triple(Icons.Rounded.BarChart, Color.LightGray.copy(alpha = 0.2f), TextSecondaryDark)
        }

        val badgeBg = if (isDark && notification.type != NotificationType.SUMMARY) {
            badgeBgBase.copy(alpha = 0.1f)
        } else {
            badgeBgBase
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(badgeBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = notification.title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark.copy(alpha = 0.7f)
                )
                // Show navigation hint
                val hintText = when (notification.referenceType) {
                    "TRANSACTION" -> "View transaction"
                    "BUDGET" -> "View budget"
                    "SUBSCRIPTION" -> "View subscription"
                    "PERSON" -> "View in ledger"
                    "EVENT" -> "View event"
                    else -> null
                }
                if (hintText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "• $hintText",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinosMint
                    )
                }
            }
        }

        // Unread Indicator
        if (notification.isUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(FinosMint, CircleShape)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}
