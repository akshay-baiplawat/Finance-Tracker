package com.example.financetracker.data.local.dao

import androidx.room.*
import com.example.financetracker.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>): List<Long>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM notifications WHERE timestamp < :olderThan")
    suspend fun deleteOldNotifications(olderThan: Long)

    @Query("DELETE FROM notifications WHERE type IN (:types)")
    suspend fun deleteNotificationsByType(types: List<String>)

    @Query("SELECT COUNT(*) FROM notifications WHERE referenceId = :refId AND referenceType = :refType")
    suspend fun checkNotificationExists(refId: Long, refType: String): Int

    @Query("DELETE FROM notifications WHERE referenceId = :refId AND referenceType = :refType")
    suspend fun deleteByReference(refId: Long, refType: String)

    @Query("SELECT message FROM notifications WHERE referenceId = :refId AND referenceType = :refType LIMIT 1")
    suspend fun getMessageByReference(refId: Long, refType: String): String?

    @Query("SELECT timestamp FROM notifications WHERE referenceId = :refId AND referenceType = :refType LIMIT 1")
    suspend fun getTimestampByReference(refId: Long, refType: String): Long?

    @Query("SELECT * FROM notifications WHERE type = :type")
    suspend fun getNotificationsByType(type: String): List<NotificationEntity>
}
