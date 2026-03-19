package com.example.financetracker.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Long, currencyCode: String): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = currency
            // Amount is in cents/paisa
            format.format(amount / 100.0)
        } catch (e: Exception) {
            // Fallback to basic formatting if code is invalid
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.format(amount / 100.0)
        }
    }
}
