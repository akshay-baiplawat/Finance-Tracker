package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Settlement entity to track payments between members.
 * Records when one member pays another to settle debts within a group.
 */
@Entity(
    tableName = "settlements",
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
            childColumns = ["fromPersonId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["toPersonId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId"), Index("fromPersonId"), Index("toPersonId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val fromPersonId: Long? = null, // null = current user
    val fromSelfUser: Boolean = false,
    val toPersonId: Long? = null, // null = current user
    val toSelfUser: Boolean = true,
    val amount: Long, // in paisa
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
