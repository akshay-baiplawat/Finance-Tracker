package com.example.financetracker.domain.model

import androidx.room.Embedded
import com.example.financetracker.data.local.entity.TransactionEntity

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,
    val categoryName: String,
    val categoryColorHex: String
)
