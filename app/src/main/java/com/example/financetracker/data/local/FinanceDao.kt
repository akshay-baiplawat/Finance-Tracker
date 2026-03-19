package com.example.financetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financetracker.data.local.entity.BudgetEntity
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.data.local.entity.WalletEntity
import com.example.financetracker.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Net Worth Calculation ---
    // Sum of (Assets + Cash) - (Credit Cards)
    // Assets: type != 'CREDIT'
    // Liabilities: type == 'CREDIT'
    @Query("""
        SELECT 
        (SELECT COALESCE(SUM(balance), 0) FROM wallets WHERE type != 'CREDIT') - 
        (SELECT COALESCE(SUM(ABS(balance)), 0) FROM wallets WHERE type = 'CREDIT')
    """)
    fun getTotalNetWorth(): Flow<Long>

    // --- Dashboard & Lists ---

    @Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE groupId IS NULL AND (note LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWalletsList(): List<WalletEntity>

    @Query("SELECT * FROM people")
    fun getAllPeople(): Flow<List<PersonEntity>>

    // --- Cash Flow (Monthly) ---

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE groupId IS NULL AND type = 'INCOME' AND timestamp BETWEEN :start AND :end")
    fun getMonthlyIncome(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE groupId IS NULL AND type = 'EXPENSE' AND timestamp BETWEEN :start AND :end")
    fun getMonthlyExpense(start: Long, end: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE groupId IS NULL AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    // --- Charts ---

    @Query("SELECT categoryName, SUM(amount) as total FROM transactions WHERE groupId IS NULL AND type = 'EXPENSE' GROUP BY categoryName")
    fun getCategoryBreakdown(): Flow<List<CategoryTuple>>

    @Query("SELECT categoryName, SUM(amount) as total FROM transactions WHERE groupId IS NULL AND type = 'INCOME' GROUP BY categoryName")
    fun getIncomeCategoryBreakdown(): Flow<List<CategoryTuple>>

    // --- Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionAndGetId(transaction: TransactionEntity): Long

    @androidx.room.Update
    suspend fun updateTransactionEntity(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    // Atomically update wallet balance
    @Query("UPDATE wallets SET balance = balance + :amount WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Long, amount: Long)

    // Atomically update person balance
    @Query("UPDATE people SET currentBalance = currentBalance + :amount WHERE id = :personId")
    suspend fun updatePersonBalance(personId: Long, amount: Long)

    @androidx.room.Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWallet(id: Long)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePerson(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND merchant = :merchant AND timestamp = :date")
    suspend fun checkTransactionExists(amount: Long, merchant: String, date: Long): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE smsId = :smsId")
    suspend fun checkSmsIdExists(smsId: Long): Long

    @Query("SELECT smsId FROM transactions WHERE smsId IN (:smsIds)")
    suspend fun getExistingSmsIds(smsIds: List<Long>): List<Long>

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @androidx.room.Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET categoryName = :category, limitAmount = :limit, colorHex = :color, rolloverEnabled = :rolloverEnabled WHERE id = :id")
    suspend fun updateBudgetDetails(id: Long, category: String, limit: Long, color: String, rolloverEnabled: Boolean)

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY nextDueDate ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Long)

    @androidx.room.Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<com.example.financetracker.data.local.entity.CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: com.example.financetracker.data.local.entity.CategoryEntity)

    @androidx.room.Update
    suspend fun updateCategory(category: com.example.financetracker.data.local.entity.CategoryEntity)

    @androidx.room.Delete
    suspend fun deleteCategory(category: com.example.financetracker.data.local.entity.CategoryEntity)
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    suspend fun checkCategoryNameExists(name: String): Int

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoriesByName(name: String): List<com.example.financetracker.data.local.entity.CategoryEntity>

    @Query("DELETE FROM categories WHERE id NOT IN (SELECT MIN(id) FROM categories GROUP BY name)")
    suspend fun deleteDuplicateCategories()

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND id != :excludeId")
    suspend fun checkCategoryNameExistsExcluding(name: String, excludeId: Long): Int

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<com.example.financetracker.data.local.entity.CategoryEntity>

    @Query("UPDATE categories SET iconId = :iconId WHERE id = :id")
    suspend fun updateCategoryIconId(id: Long, iconId: Int)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): com.example.financetracker.data.local.entity.CategoryEntity?

    @Query("UPDATE categories SET colorHex = :colorHex WHERE id = :id")
    suspend fun updateCategoryColor(id: Long, colorHex: String)

    @Query("UPDATE categories SET name = :name, colorHex = :colorHex, iconId = :iconId WHERE id = :id")
    suspend fun updateCategoryFields(id: Long, name: String, colorHex: String, iconId: Int)

    // --- Clear All Data ---
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()

    // --- Category Filter ---
    @Query("SELECT MIN(timestamp) FROM transactions WHERE groupId IS NULL")
    suspend fun getOldestTransactionTimestamp(): Long?

    // --- Chunked Export (Streaming) ---
    @Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsChunked(limit: Int, offset: Int): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE groupId IS NULL")
    suspend fun getTransactionCountForExport(): Int
}
