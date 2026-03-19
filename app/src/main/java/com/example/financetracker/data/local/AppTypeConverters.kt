package com.example.financetracker.data.local

import androidx.room.TypeConverter
import com.example.financetracker.data.local.entity.TransactionType
import com.example.financetracker.data.local.entity.WalletType

class AppTypeConverters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.DEBIT // Fallback
        }
    }

    @TypeConverter
    fun fromWalletType(value: WalletType): String {
        return value.name
    }

    @TypeConverter
    fun toWalletType(value: String): WalletType {
        return try {
            WalletType.valueOf(value)
        } catch (e: Exception) {
            WalletType.ASSET // Fallback
        }
    }
}
