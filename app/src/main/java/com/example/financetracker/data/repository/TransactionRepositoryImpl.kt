package com.example.financetracker.data.repository

import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val financeDao: FinanceDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return financeDao.getRecentTransactions()
    }

    override fun getTotalNetWorth(): Flow<Long> {
        return financeDao.getTotalNetWorth()
    }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        financeDao.insertTransaction(transaction)
    }

    override suspend fun clearAllTransactions() {
        financeDao.deleteAllTransactions()
    }

    override suspend fun batchInsert(transactions: List<com.example.financetracker.domain.model.ImportedTransactionData>) {
        transactions.forEach { item ->
            val typeStr = when (item.type.name) {
                "EXPENSE", "DEBIT" -> "EXPENSE"
                "INCOME", "CREDIT" -> "INCOME"
                "TRANSFER" -> "TRANSFER"
                else -> "EXPENSE"
            }
            financeDao.insertTransaction(
                TransactionEntity(
                    amount = item.amount,
                    note = item.merchant,
                    type = typeStr,
                    categoryName = item.categoryName,
                    timestamp = item.timestamp,
                    walletId = 1L
                )
            )
        }
    }
}