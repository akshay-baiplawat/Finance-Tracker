package com.example.financetracker.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.financetracker.MainActivity
import com.example.financetracker.R
import com.example.financetracker.data.local.entity.NotificationEntity
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        const val CHANNEL_ID = "finance_alerts"
        const val CHANNEL_NAME = "Finance Alerts"

        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TARGET_ROUTE = "target_route"
        const val EXTRA_TARGET_TAB = "target_tab"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for budgets, bills, and financial updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(notification: NotificationEntity) {
        // Check if notifications are enabled
        val isEnabled = runBlocking { userPreferencesRepository.isNotificationsEnabled.first() }
        if (!isEnabled) return

        val (targetRoute, targetTab) = getTargetRouteAndTab(notification.referenceType)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            putExtra(EXTRA_TARGET_TAB, targetTab)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notification.id.toInt(), builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun getTargetRouteAndTab(referenceType: String?): Pair<String, Int> {
        return when (referenceType) {
            "TRANSACTION" -> "ledger" to 0
            "BUDGET" -> "insights" to 1
            "SUBSCRIPTION" -> "insights" to 2
            "PERSON" -> "ledger" to 1
            "WEEKLY_REMINDER" -> "ledger" to 1
            "EVENT" -> "ledger" to 2
            else -> "home" to 0
        }
    }
}
