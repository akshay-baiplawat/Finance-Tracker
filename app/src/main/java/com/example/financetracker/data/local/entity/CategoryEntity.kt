package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "EXPENSE", // EXPENSE or INCOME
    val colorHex: String,
    val iconId: Int = 0,
    val isSystemDefault: Boolean = false
)
