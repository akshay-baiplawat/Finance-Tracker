package com.example.financetracker.model

enum class TransactionType {
    DEBIT, CREDIT
}

data class Transaction(
    val amount: Double,
    val merchant: String,
    val type: TransactionType,
    val date: Long,
    val rawSender: String,
    val smsId: Long? = null
) {
    override fun toString(): String {
        return "[${type}] ${amount} @ ${merchant.uppercase()}"
    }
}
