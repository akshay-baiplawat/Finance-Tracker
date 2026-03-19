package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enhanced transaction entity for group expenses.
 * Links a transaction to a group with additional metadata for splitting.
 */
@Entity(
    tableName = "group_transactions",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByPersonId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId"), Index("transactionId"), Index("paidByPersonId")]
)
data class GroupTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val transactionId: Long,
    val paidByPersonId: Long? = null, // null = paid by current user
    val paidByUserSelf: Boolean = true, // true if current user paid
    val splitType: String = SplitType.EQUAL.name, // "EQUAL", "CUSTOM", "PERCENTAGE", "SHARES"
    val tag: String = "" // Custom tag/category within group
)

enum class SplitType {
    EQUAL,      // Split equally among all members
    CUSTOM,     // Custom amounts per member
    PERCENTAGE, // Percentage-based split
    SHARES      // Share-based split (e.g., 2 shares vs 1 share)
}
