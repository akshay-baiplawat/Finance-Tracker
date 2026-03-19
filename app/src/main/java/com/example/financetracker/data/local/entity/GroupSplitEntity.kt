package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual split amounts per member for a group transaction.
 * Tracks how much each member owes for a specific expense.
 */
@Entity(
    tableName = "group_splits",
    foreignKeys = [
        ForeignKey(
            entity = GroupTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupTransactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupTransactionId"), Index("memberId")]
)
data class GroupSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupTransactionId: Long,
    val memberId: Long? = null, // null = current user
    val isSelfUser: Boolean = false,
    val shareAmount: Long, // Amount this person owes (in paisa)
    val isPaid: Boolean = false // For tracking settlements
)
