package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val amount: Long, // in cents (positive value usually)
    val frequency: String, // "Monthly", "Yearly", "Weekly"
    val nextDueDate: Long, // Epoch millis
    val colorHex: String,
    val isAutoDetected: Boolean = false
)
