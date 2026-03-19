package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contribution entity for savings goals.
 * Tracks contributions from members toward a group's target amount.
 */
@Entity(
    tableName = "group_contributions",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId"), Index("memberId")]
)
data class GroupContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val memberId: Long? = null, // null = current user
    val isSelfUser: Boolean = true,
    val amount: Long, // in paisa
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
