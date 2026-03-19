package com.example.financetracker.data.local.entity

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    DEBIT,  // Mapping DEBIT -> EXPENSE
    CREDIT  // Mapping CREDIT -> INCOME
}
