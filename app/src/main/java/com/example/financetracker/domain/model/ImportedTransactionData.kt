package com.example.financetracker.domain.model

import com.example.financetracker.data.local.entity.TransactionType

data class ImportedTransactionData(
    val timestamp: Long,
    val merchant: String,
    val amount: Long,
    val type: TransactionType,
    val categoryName: String
)
