package com.example.financetracker.data.local.dao

import androidx.room.*
import com.example.financetracker.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    // Net Worth Calculation
    // BANK and CASH are assets (positive), CREDIT is liability (owed amount)
    @Query("""
        SELECT
            (COALESCE((SELECT SUM(balance) FROM wallets WHERE type IN ('BANK', 'CASH')), 0)) -
            (COALESCE((SELECT ABS(SUM(balance)) FROM wallets WHERE type = 'CREDIT'), 0))
    """)
    fun getNetWorth(): Flow<Long>

    @Query("UPDATE wallets SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Long, amount: Long)
}
