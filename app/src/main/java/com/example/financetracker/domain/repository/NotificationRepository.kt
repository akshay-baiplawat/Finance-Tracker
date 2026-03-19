package com.example.financetracker.domain.repository

import com.example.financetracker.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markAsRead(id: Long)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(id: Long)
    suspend fun generateNotifications()
    suspend fun generateSubscriptionNotifications()
    suspend fun cleanupOldNotifications()
    fun showSystemNotification(notification: NotificationEntity)
}
