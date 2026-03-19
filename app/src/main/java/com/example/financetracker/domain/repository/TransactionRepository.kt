package com.example.financetracker.domain.repository

import com.example.financetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTotalNetWorth(): Flow<Long>
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    suspend fun clearAllTransactions()
    suspend fun batchInsert(transactions: List<com.example.financetracker.domain.model.ImportedTransactionData>)

    // Kept for compatibility if needed, but unimplemented in Phase 1
    // fun getAllCategories(): Flow<List<CategoryEntity>>
}