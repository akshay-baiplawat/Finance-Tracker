package com.example.financetracker.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financetracker.domain.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class SubscriptionReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Running subscription reminder check at 9:05 AM...")
            notificationRepository.generateSubscriptionNotifications()
            Log.d(TAG, "Subscription reminder check complete.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Subscription reminder check failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "SubscriptionReminderWorker"
        private const val WORK_NAME = "subscription_reminder_daily"

        fun schedule(context: Context) {
            // Calculate initial delay to next 9:05 AM
            val now = Calendar.getInstance()
            val nextTrigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If we're past 9:05 AM today, schedule for tomorrow
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelayMillis = nextTrigger.timeInMillis - now.timeInMillis

            val dailyWorkRequest = PeriodicWorkRequestBuilder<SubscriptionReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )

            Log.d(TAG, "Scheduled subscription reminder for 9:05 AM daily. Initial delay: ${initialDelayMillis / 1000 / 60} minutes")
        }
    }
}
