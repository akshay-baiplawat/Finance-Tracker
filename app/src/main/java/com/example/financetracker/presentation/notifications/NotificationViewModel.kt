package com.example.financetracker.presentation.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.data.local.entity.NotificationEntity
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class NotificationUiModel(
    val id: Long,
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType,
    val isUnread: Boolean,
    val referenceId: Long?,
    val referenceType: String?
)

enum class NotificationType {
    INCOME, EXPENSE, BUDGET_ALERT, TRIP, DEBT, UPCOMING_BILL, SUMMARY
}

data class NotificationUiState(
    val notifications: List<NotificationUiModel> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Filter state persistence - survives navigation and process death
    val selectedFilter: StateFlow<Int> = savedStateHandle.getStateFlow("notification_filter", 0)

    fun selectFilter(index: Int) {
        savedStateHandle["notification_filter"] = index
    }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        // Generate notifications on init
        viewModelScope.launch {
            notificationRepository.generateNotifications()
        }

        // Observe notifications
        viewModelScope.launch {
            combine(
                notificationRepository.getAllNotifications(),
                notificationRepository.getUnreadCount()
            ) { notifications, unreadCount ->
                NotificationUiState(
                    notifications = notifications.map { it.toUiModel() },
                    unreadCount = unreadCount,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark as read: ${e.localizedMessage}") }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark all as read: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete notification: ${e.localizedMessage}") }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                notificationRepository.generateNotifications()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to refresh: ${e.localizedMessage}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun NotificationEntity.toUiModel(): NotificationUiModel {
        return NotificationUiModel(
            id = id,
            title = title,
            message = message,
            time = formatRelativeTime(timestamp),
            type = try {
                NotificationType.valueOf(type)
            } catch (e: Exception) {
                AppLogger.w("NotificationViewModel", "Unknown notification type: $type, defaulting to SUMMARY", e)
                NotificationType.SUMMARY
            },
            isUnread = !isRead,
            referenceId = referenceId,
            referenceType = referenceType
        )
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now = Instant.now()
        val then = Instant.ofEpochMilli(timestamp)
        val zone = ZoneId.systemDefault()

        val minutesAgo = ChronoUnit.MINUTES.between(then, now)
        val hoursAgo = ChronoUnit.HOURS.between(then, now)
        val daysAgo = ChronoUnit.DAYS.between(then.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())

        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "$minutesAgo min${if (minutesAgo > 1) "s" else ""} ago"
            hoursAgo < 24 -> "$hoursAgo hour${if (hoursAgo > 1) "s" else ""} ago"
            daysAgo == 1L -> "Yesterday"
            daysAgo < 7 -> "$daysAgo days ago"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
                then.atZone(zone).format(formatter)
            }
        }
    }
}
