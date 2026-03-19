package com.example.financetracker.core.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtil {
    fun formatPaisa(amountPaisa: Long, currencyCode: String = "INR"): String {
        // Convert to Major Unit (Double)
        val amount = amountPaisa / 100.0
        
        // Attempt to find a locale that uses this currency, fallback to US or generic
        val format = try {
            val currency = java.util.Currency.getInstance(currencyCode)
            val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            numberFormat.currency = currency
            numberFormat.maximumFractionDigits = currency.defaultFractionDigits
            numberFormat.minimumFractionDigits = currency.defaultFractionDigits
            numberFormat
        } catch (e: Exception) {
            // Fallback for invalid codes
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.currency = java.util.Currency.getInstance("INR")
            format
        }
        
        return format.format(amount)
    }
}
