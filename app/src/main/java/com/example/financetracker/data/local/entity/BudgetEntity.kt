package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String,
    val limitAmount: Long, // Paisa
    val colorHex: String,
    val rolloverEnabled: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
