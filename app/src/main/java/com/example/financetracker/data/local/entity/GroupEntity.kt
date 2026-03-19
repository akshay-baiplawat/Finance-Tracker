package com.example.financetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Main group entity that replaces EventEntity.
 * Supports multiple group types: TRIP, SPLIT_EXPENSE, SAVINGS_GOAL, GENERAL
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = GroupType.GENERAL.name, // "TRIP", "SPLIT_EXPENSE", "SAVINGS_GOAL", "GENERAL"
    val budgetLimit: Long = 0L,    // For trips/events (in paisa)
    val targetAmount: Long = 0L,   // For savings goals (in paisa)
    val description: String = "",
    val colorHex: String = "#2196F3",
    val iconId: Int = 0,
    val isActive: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis(),
    // Filter fields for "Group By Category" type (TRIP)
    @ColumnInfo(name = "filter_category_ids") val filterCategoryIds: String? = null,  // Comma-separated category IDs
    @ColumnInfo(name = "filter_start_date") val filterStartDate: Long? = null,         // Timestamp, null = oldest transaction
    @ColumnInfo(name = "filter_end_date") val filterEndDate: Long? = null              // Timestamp, null = current time
)

enum class GroupType {
    TRIP,           // Budget tracking with expenses
    SPLIT_EXPENSE,  // Expense sharing with settlement suggestions
    SAVINGS_GOAL,   // Collaborative target with contributions
    GENERAL         // All features combined
}
