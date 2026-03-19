package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for group membership.
 * Links groups to people (PersonEntity).
 * The current user is implicitly a member of all their groups.
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "memberId"],
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId"), Index("memberId")]
)
data class GroupMemberEntity(
    val groupId: Long,
    val memberId: Long,
    val isOwner: Boolean = false,
    val joinedTimestamp: Long = System.currentTimeMillis()
)
