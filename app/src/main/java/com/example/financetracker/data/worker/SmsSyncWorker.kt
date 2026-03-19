package com.example.financetracker.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financetracker.core.parser.SmsParserEngine
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.domain.repository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FinanceRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val startTime = inputData.getLong("START_TIME", -1L)
            Log.d("SmsSyncWorker", "Starting SMS Sync via Repository... StartTime: $startTime")

            // Delegate to Repository (Single Source of Truth)
            // This ensures we use the robust deduplication logic (ID-based + Hybrid) defined there.
            val addedCount = repository.syncTransactions(startTime)
            
            Log.d("SmsSyncWorker", "Sync Complete. Added $addedCount transactions.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SmsSyncWorker", "Sync Failed", e)
            Result.failure()
        }
    }
}