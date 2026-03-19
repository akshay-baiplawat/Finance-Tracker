package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "BANK", "CASH", "CREDIT"
    val balance: Long, // Paisa
    val colorHex: String,
    val accountNumber: String
)
