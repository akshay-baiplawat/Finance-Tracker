package com.example.financetracker.data.local.dao

import androidx.room.*
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalSpend(): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY timestamp DESC")
    fun getTransactionsForWallet(walletId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getTransactionsForGroup(groupId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'EXPENSE'
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalExpenseForMonth(startTime: Long, endTime: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    // For SMS Sync de-duplication
    @Query("SELECT * FROM transactions WHERE smsId = :smsId LIMIT 1")
    suspend fun getTransactionBySmsId(smsId: Long): TransactionEntity?

    // Smart Rules Helpers - update category by merchant name
    @Query("UPDATE transactions SET categoryName = :categoryName WHERE merchant = :merchant")
    suspend fun updateCategoryForMerchant(merchant: String, categoryName: String)

    @Query("UPDATE transactions SET categoryName = :categoryName WHERE id = :id")
    suspend fun updateCategoryForId(id: Long, categoryName: String)
}
