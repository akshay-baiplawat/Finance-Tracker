package com.example.financetracker.data.repository

import com.example.financetracker.core.notification.NotificationHelper
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.dao.GroupDao
import com.example.financetracker.data.local.dao.NotificationDao
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.data.local.entity.NotificationEntity
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val financeDao: FinanceDao,
    private val groupDao: GroupDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : NotificationRepository {

    // Mutex to prevent concurrent notification generation (avoids duplicates)
    private val generationMutex = Mutex()

    // Trigger days for subscription notifications: 7, 3, 2, 1, 0 days ahead, and 1 day overdue (-1)
    private val subscriptionTriggerDays = setOf(7, 3, 2, 1, 0, -1)

    override fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getAllNotifications()
    }

    override fun getUnreadNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getUnreadNotifications()
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    override suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    override suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotification(id)
    }

    override suspend fun generateNotifications() = generationMutex.withLock {
        // Check if notifications are enabled
        val isNotificationsEnabled = userPreferencesRepository.isNotificationsEnabled.first()
        if (!isNotificationsEnabled) return@withLock

        val currency = userPreferencesRepository.currency.first()
        val now = System.currentTimeMillis()
        val notifications = mutableListOf<NotificationEntity>()

        // Clean up any existing transaction notifications (INCOME, EXPENSE)
        notificationDao.deleteNotificationsByType(listOf("INCOME", "EXPENSE"))

        // 1. Generate budget alert notifications
        generateBudgetAlertNotifications(currency, now, notifications)

        // 2. Generate upcoming bill notifications
        generateUpcomingBillNotifications(currency, now, notifications)

        // 3. Generate debt notifications
        generateDebtNotifications(currency, now, notifications)

        // 4. Generate event/trip progress notifications
        generateEventNotifications(currency, now, notifications)

        // Insert all new notifications and show system notifications
        if (notifications.isNotEmpty()) {
            val insertedIds = notificationDao.insertNotifications(notifications)
            // Show system notifications with correct database IDs
            notifications.forEachIndexed { index, notification ->
                val notificationWithId = notification.copy(id = insertedIds[index])
                showSystemNotification(notificationWithId)
            }
        }
    }

    override suspend fun generateSubscriptionNotifications() = generationMutex.withLock {
        // Check if notifications are enabled
        val isNotificationsEnabled = userPreferencesRepository.isNotificationsEnabled.first()
        if (!isNotificationsEnabled) return@withLock

        val currency = userPreferencesRepository.currency.first()
        val now = System.currentTimeMillis()
        val notifications = mutableListOf<NotificationEntity>()

        // Generate only subscription notifications (for 9:05 AM daily worker)
        generateUpcomingBillNotifications(currency, now, notifications)

        // Insert all new notifications and show system notifications
        if (notifications.isNotEmpty()) {
            val insertedIds = notificationDao.insertNotifications(notifications)
            notifications.forEachIndexed { index, notification ->
                val notificationWithId = notification.copy(id = insertedIds[index])
                showSystemNotification(notificationWithId)
            }
        }
    }

    private suspend fun generateBudgetAlertNotifications(
        currency: String,
        now: Long,
        notifications: MutableList<NotificationEntity>
    ) {
        val budgets = financeDao.getAllBudgets().first()
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.now()
        val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Get ALL transactions for this month to calculate spending per category
        val allTransactions = financeDao.getAllTransactionsList()
        val thisMonthTransactions = allTransactions.filter {
            it.timestamp >= monthStart && it.timestamp < monthEnd && it.type == "EXPENSE"
        }

        // Calculate spending per category for THIS MONTH
        val categorySpending = thisMonthTransactions
            .groupBy { it.categoryName }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        // Month identifier for unique notification per month
        val monthId = currentMonth.year * 100 + currentMonth.monthValue // e.g., 202603

        for (budget in budgets) {
            // Handle "All Expenses" budget
            val spent = if (budget.categoryName == "All Expenses") {
                thisMonthTransactions
                    .filter { it.categoryName != "Money Transfer" }
                    .sumOf { it.amount }
            } else {
                categorySpending[budget.categoryName] ?: 0L
            }

            val percentage = if (budget.limitAmount > 0) (spent * 100) / budget.limitAmount else 0

            // Alert at 80% and 100%
            val thresholds = listOf(100L, 80L) // Check 100% first for priority
            for (threshold in thresholds) {
                if (percentage >= threshold) {
                    // Unique reference: budgetId + monthId + threshold
                    // This allows new notifications each month
                    val referenceId = budget.id * 1000000L + monthId * 10L + (threshold / 10)

                    val spentFormatted = CurrencyFormatter.format(spent, currency)
                    val limitFormatted = CurrencyFormatter.format(budget.limitAmount, currency)

                    val (title, message) = if (percentage >= 100) {
                        "Budget Exceeded!" to "You've exceeded your ${budget.categoryName} budget! Spent $spentFormatted of $limitFormatted."
                    } else {
                        "Budget Alert" to "You've used ${percentage}% of your ${budget.categoryName} budget ($spentFormatted of $limitFormatted)."
                    }

                    // Only recreate if message changed (preserves read status if unchanged)
                    val existingMessage = notificationDao.getMessageByReference(referenceId, "BUDGET")
                    if (existingMessage == message) {
                        break // Same notification exists, keep it
                    }

                    // Delete old and create new (unread) since data changed
                    notificationDao.deleteByReference(referenceId, "BUDGET")

                    notifications.add(
                        NotificationEntity(
                            title = title,
                            message = message,
                            type = "BUDGET_ALERT",
                            timestamp = now,
                            isRead = false,
                            referenceId = referenceId,
                            referenceType = "BUDGET"
                        )
                    )
                    break // Only one alert per budget per check
                }
            }
        }
    }

    private suspend fun generateUpcomingBillNotifications(
        currency: String,
        now: Long,
        notifications: MutableList<NotificationEntity>
    ) {
        val subscriptions = financeDao.getAllSubscriptions().first()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        for (subscription in subscriptions) {
            val dueDate = Instant.ofEpochMilli(subscription.nextDueDate).atZone(zone).toLocalDate()
            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()

            // Only trigger on specific days: 7, 3, 2, 1, 0 days ahead, or 1 day overdue (-1)
            if (daysUntilDue !in subscriptionTriggerDays) continue

            // Use dueDate as part of reference to allow re-notification for next billing cycle
            // Reference = subscription.id + dueDateId (each new trigger replaces the previous one)
            val dueDateId = dueDate.year * 10000 + dueDate.monthValue * 100 + dueDate.dayOfMonth
            val referenceId = subscription.id * 100000000L + dueDateId

            val amountFormatted = CurrencyFormatter.format(subscription.amount, currency)

            val dueText = when {
                daysUntilDue < 0 -> "was due ${-daysUntilDue} day${if (-daysUntilDue > 1) "s" else ""} ago"
                daysUntilDue == 0 -> "is due today"
                daysUntilDue == 1 -> "is due tomorrow"
                else -> "is due in $daysUntilDue days"
            }

            val title = when {
                daysUntilDue < 0 -> "Overdue Bill"
                daysUntilDue == 0 -> "Bill Due Today"
                else -> "Upcoming Bill"
            }
            val message = "${subscription.merchantName} ${subscription.frequency.lowercase()} subscription of $amountFormatted $dueText."

            // Only recreate if message changed (preserves read status if unchanged)
            val existingMessage = notificationDao.getMessageByReference(referenceId, "SUBSCRIPTION")
            if (existingMessage == message) {
                continue // Same notification exists, keep it
            }

            // Delete old and create new (unread) since data changed or trigger day changed
            notificationDao.deleteByReference(referenceId, "SUBSCRIPTION")

            notifications.add(
                NotificationEntity(
                    title = title,
                    message = message,
                    type = "UPCOMING_BILL",
                    timestamp = now,
                    isRead = false,
                    referenceId = referenceId,
                    referenceType = "SUBSCRIPTION"
                )
            )
        }
    }

    private suspend fun generateDebtNotifications(
        currency: String,
        now: Long,
        notifications: MutableList<NotificationEntity>
    ) {
        val people = financeDao.getAllPeople().first()

        for (person in people) {
            if (person.currentBalance != 0L) {
                val amountFormatted = CurrencyFormatter.format(kotlin.math.abs(person.currentBalance), currency)

                val (title, message) = if (person.currentBalance > 0) {
                    "Money Owed" to "${person.name} owes you $amountFormatted."
                } else {
                    "Payment Due" to "You owe ${person.name} $amountFormatted."
                }

                // Check if notification already exists for this person
                val existingTimestamp = notificationDao.getTimestampByReference(person.id, "PERSON")

                if (existingTimestamp != null) {
                    // Notification exists - only update if message changed (amount changed)
                    val existingMessage = notificationDao.getMessageByReference(person.id, "PERSON")
                    if (existingMessage != message) {
                        // Amount changed - update the notification but keep existing timestamp
                        notificationDao.deleteByReference(person.id, "PERSON")
                        notifications.add(
                            NotificationEntity(
                                title = title,
                                message = message,
                                type = "DEBT",
                                timestamp = existingTimestamp, // Keep original timestamp
                                isRead = true, // Mark as read since it's just an update
                                referenceId = person.id,
                                referenceType = "PERSON"
                            )
                        )
                    }
                    // If message same, do nothing (don't regenerate)
                } else {
                    // NEW debt - create notification
                    notifications.add(
                        NotificationEntity(
                            title = title,
                            message = message,
                            type = "DEBT",
                            timestamp = now,
                            isRead = false,
                            referenceId = person.id,
                            referenceType = "PERSON"
                        )
                    )
                }
            } else {
                // Balance is zero - remove any existing debt notification
                notificationDao.deleteByReference(person.id, "PERSON")
            }
        }
    }

    private suspend fun generateEventNotifications(
        currency: String,
        now: Long,
        notifications: MutableList<NotificationEntity>
    ) {
        val groupsWithStats = groupDao.getAllGroupsWithStats().first()

        for (item in groupsWithStats) {
            val group = item.group

            // Only generate notifications for TRIP type groups with budget
            if (group.type != GroupType.TRIP.name || group.budgetLimit <= 0 || !group.isActive) {
                continue
            }

            val spent = item.totalSpent
            val remaining = group.budgetLimit - spent

            if (remaining > 0) {
                val remainingFormatted = CurrencyFormatter.format(remaining, currency)
                val message = "You're $remainingFormatted away from your ${group.name} budget!"

                // Only recreate if message changed (preserves read status if unchanged)
                val existingMessage = notificationDao.getMessageByReference(group.id, "GROUP")
                if (existingMessage == message) {
                    continue // Same notification exists, keep it
                }

                // Delete old and create new (unread) since data changed
                notificationDao.deleteByReference(group.id, "GROUP")

                notifications.add(
                    NotificationEntity(
                        title = group.name,
                        message = message,
                        type = "TRIP",
                        timestamp = now,
                        isRead = false,
                        referenceId = group.id,
                        referenceType = "GROUP"
                    )
                )
            }
        }
    }

    override suspend fun cleanupOldNotifications() {
        // Delete notifications older than 30 days
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        notificationDao.deleteOldNotifications(thirtyDaysAgo)
    }

    override fun showSystemNotification(notification: NotificationEntity) {
        notificationHelper.showNotification(notification)
    }
}
