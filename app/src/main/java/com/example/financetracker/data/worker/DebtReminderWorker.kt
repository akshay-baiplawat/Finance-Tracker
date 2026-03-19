package com.example.financetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financetracker.core.notification.NotificationHelper
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.dao.NotificationDao
import com.example.financetracker.data.local.entity.NotificationEntity
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DebtReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val financeDao: FinanceDao,
    private val notificationDao: NotificationDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if notifications are enabled
        val isNotificationsEnabled = userPreferencesRepository.isNotificationsEnabled.first()
        if (!isNotificationsEnabled) return Result.success()

        val currency = userPreferencesRepository.currency.first()
        val now = System.currentTimeMillis()
        val people = financeDao.getAllPeople().first()

        // Calculate totals
        var theyOweYou = 0L
        var youOweThem = 0L
        var debtorCount = 0
        var creditorCount = 0

        for (person in people) {
            when {
                person.currentBalance > 0 -> {
                    theyOweYou += person.currentBalance
                    debtorCount++
                }
                person.currentBalance < 0 -> {
                    youOweThem += kotlin.math.abs(person.currentBalance)
                    creditorCount++
                }
            }
        }

        // Only send reminder if there are outstanding debts
        if (theyOweYou > 0 || youOweThem > 0) {
            val messages = mutableListOf<String>()

            if (theyOweYou > 0) {
                val amount = CurrencyFormatter.format(theyOweYou, currency)
                messages.add("$debtorCount ${if (debtorCount == 1) "person owes" else "people owe"} you $amount")
            }
            if (youOweThem > 0) {
                val amount = CurrencyFormatter.format(youOweThem, currency)
                messages.add("You owe $creditorCount ${if (creditorCount == 1) "person" else "people"} $amount")
            }

            val notification = NotificationEntity(
                title = "Weekly Debt Reminder",
                message = messages.joinToString(". ") + ".",
                type = "DEBT_REMINDER",
                timestamp = now,
                isRead = false,
                referenceId = now, // Use timestamp as unique ID for weekly reminders
                referenceType = "WEEKLY_REMINDER"
            )

            val insertedId = notificationDao.insertNotification(notification)
            notificationHelper.showNotification(notification.copy(id = insertedId))
        }

        return Result.success()
    }
}
