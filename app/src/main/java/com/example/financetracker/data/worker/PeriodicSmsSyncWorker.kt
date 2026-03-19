package com.example.financetracker.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class PeriodicSmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val financeRepository: FinanceRepository,
    private val notificationRepository: NotificationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting periodic SMS sync...")

            // Check if periodic sync is enabled
            val isPeriodicSyncEnabled = userPreferencesRepository.isPeriodicSyncEnabled.first()
            if (!isPeriodicSyncEnabled) {
                Log.d(TAG, "Periodic sync is disabled. Skipping.")
                return@withContext Result.success()
            }

            // Check Privacy Mode
            val isPrivacyMode = userPreferencesRepository.isPrivacyMode.first()
            if (isPrivacyMode) {
                Log.d(TAG, "Privacy Mode is enabled. Skipping SMS sync.")
                return@withContext Result.success()
            }

            // Get lastSyncTimestamp and sync
            val lastSyncTime = userPreferencesRepository.lastSyncTimestamp.first()
            Log.d(TAG, "Syncing SMS from timestamp: $lastSyncTime")

            val addedCount = financeRepository.syncTransactions(lastSyncTime)
            Log.d(TAG, "SMS sync complete. Added $addedCount transactions.")

            // Generate notifications (budget alerts, debt notifications)
            notificationRepository.generateNotifications()
            Log.d(TAG, "Notification generation complete.")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic SMS sync failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "PeriodicSmsSyncWorker"
        const val WORK_NAME = "periodic_sms_sync"

        fun schedule(context: Context) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicSmsSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            Log.d(TAG, "Scheduled periodic SMS sync every 15 minutes")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic SMS sync")
        }
    }
}
