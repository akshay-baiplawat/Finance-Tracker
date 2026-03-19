package com.example.financetracker.domain.repository

import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.model.MonthlyReport
import com.example.financetracker.presentation.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    // Observables
    fun getNetWorth(): Flow<Long>
    fun getRecentTransactions(): Flow<List<TransactionUiModel>>
    fun searchTransactions(query: String): Flow<List<TransactionUiModel>>
    fun getAllWallets(): Flow<List<AccountUiModel>>
    fun getAllTransactions(): Flow<List<TransactionUiModel>>
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionUiModel>>
    fun getAllPeople(): Flow<List<com.example.financetracker.data.local.entity.PersonEntity>>
    fun getMonthlyCashFlow(monthStart: Long, monthEnd: Long): Flow<MonthlyReport>
    fun getAllBudgets(): Flow<List<com.example.financetracker.data.local.entity.BudgetEntity>>
    fun getCategoryBreakdown(): Flow<List<com.example.financetracker.data.local.CategoryTuple>>
    fun getIncomeBreakdown(): Flow<List<com.example.financetracker.data.local.CategoryTuple>>

    // Operations
    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun addBudget(category: String, limit: Long, color: String, rolloverEnabled: Boolean = false)
    suspend fun deleteBudget(id: Long)
    suspend fun updateBudget(id: Long, category: String, limit: Long, color: String, rolloverEnabled: Boolean)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun addWallet(name: String, type: String, amount: Long, color: String): Long
    suspend fun addPerson(name: String, amount: Long)
    suspend fun deleteWallet(id: Long)
    suspend fun deletePerson(id: Long)
    suspend fun updateWallet(id: Long, name: String, type: String, amount: Long, color: String)
    suspend fun updatePerson(id: Long, name: String, amount: Long)
    suspend fun syncTransactions(startTime: Long): Int
    suspend fun getAllTransactionsForExport(): List<TransactionEntity>
    suspend fun getTransactionsChunked(limit: Int, offset: Int): List<TransactionEntity>
    suspend fun getTransactionCountForExport(): Int
    fun getAllSubscriptions(): Flow<List<com.example.financetracker.data.local.entity.SubscriptionEntity>>
    suspend fun addSubscription(merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String, isAuto: Boolean = false)
    suspend fun deleteSubscription(id: Long)
    suspend fun updateSubscription(id: Long, merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String)

    // Categories
    fun getAllCategories(): Flow<List<com.example.financetracker.data.local.entity.CategoryEntity>>
    suspend fun addCategory(name: String, type: String, colorHex: String, iconId: Int): Boolean
    suspend fun updateCategory(id: Long, name: String, type: String, colorHex: String, iconId: Int): Boolean
    suspend fun deleteCategory(id: Long)
    suspend fun seedDefaultCategories()
    suspend fun ensureTripCategoryExists()
    suspend fun clearAllData()
}
