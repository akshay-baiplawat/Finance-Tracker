package com.example.financetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.financetracker.data.worker.DebtReminderWorker
import com.example.financetracker.data.worker.PeriodicSmsSyncWorker
import com.example.financetracker.data.worker.SubscriptionReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FinanceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleWeeklyDebtReminder()
        scheduleDailySubscriptionReminder()
        schedulePeriodicSmsSync()
    }

    private fun schedulePeriodicSmsSync() {
        PeriodicSmsSyncWorker.schedule(this)
    }

    private fun scheduleDailySubscriptionReminder() {
        SubscriptionReminderWorker.schedule(this)
    }

    private fun scheduleWeeklyDebtReminder() {
        // Calculate initial delay to next Sunday 9:00 AM
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If we're past Sunday 9 AM this week, schedule for next week
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val initialDelayMillis = nextSunday.timeInMillis - now.timeInMillis

        val weeklyWorkRequest = PeriodicWorkRequestBuilder<DebtReminderWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "debt_reminder_weekly",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyWorkRequest
        )
    }
}