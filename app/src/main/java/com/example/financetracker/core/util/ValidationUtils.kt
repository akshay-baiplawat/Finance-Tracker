package com.example.financetracker.core.util

import android.util.Patterns

/**
 * Utility object for input validation across the app.
 * Centralizes validation logic to ensure consistency.
 */
object ValidationUtils {

    // Amount constraints
    private const val MAX_AMOUNT_PAISA = 99_999_999_99L // ₹99,99,99,999.99 (max 10 digits)
    private const val MIN_AMOUNT_PAISA = 1L // Minimum 1 paisa

    // Category name constraints
    private const val MIN_CATEGORY_NAME_LENGTH = 1
    private const val MAX_CATEGORY_NAME_LENGTH = 50

    // Merchant name constraints
    private const val MAX_MERCHANT_NAME_LENGTH = 100

    /**
     * Validates if the amount string is a valid currency amount.
     * @param amount String representation of amount (e.g., "500.00" or "500")
     * @return true if valid, false otherwise
     */
    fun isValidAmount(amount: String): Boolean {
        if (amount.isBlank()) return false

        val trimmed = amount.trim()

        // Check for valid decimal format
        val regex = Regex("^\\d+(\\.\\d{0,2})?\$")
        if (!regex.matches(trimmed)) return false

        // Parse and check bounds
        return try {
            val value = trimmed.toDouble()
            val paisa = (value * 100).toLong()
            paisa in MIN_AMOUNT_PAISA..MAX_AMOUNT_PAISA
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Validates if the amount in paisa is within acceptable bounds.
     * @param amountPaisa Amount in paisa (1/100 of rupee)
     * @return true if valid, false otherwise
     */
    fun isValidAmountPaisa(amountPaisa: Long): Boolean {
        return amountPaisa in MIN_AMOUNT_PAISA..MAX_AMOUNT_PAISA
    }

    /**
     * Validates if the category name is valid.
     * @param name Category name to validate
     * @return true if valid, false otherwise
     */
    fun isValidCategoryName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length !in MIN_CATEGORY_NAME_LENGTH..MAX_CATEGORY_NAME_LENGTH) return false

        // Allow alphanumeric, spaces, and common punctuation
        val regex = Regex("^[\\p{L}\\p{N}\\s&',.-]+\$")
        return regex.matches(trimmed)
    }

    /**
     * Validates if the email address is valid.
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /**
     * Sanitizes merchant name by removing extra whitespace and trimming.
     * @param name Raw merchant name
     * @return Sanitized merchant name
     */
    fun sanitizeMerchantName(name: String): String {
        return name
            .trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .take(MAX_MERCHANT_NAME_LENGTH)
    }

    /**
     * Validates if the wallet/account name is valid.
     * @param name Wallet name to validate
     * @return true if valid, false otherwise
     */
    fun isValidWalletName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 1..50
    }

    /**
     * Validates if the person name is valid.
     * @param name Person name to validate
     * @return true if valid, false otherwise
     */
    fun isValidPersonName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length !in 1..100) return false

        // Allow letters, spaces, and common name characters
        val regex = Regex("^[\\p{L}\\s.',-]+\$")
        return regex.matches(trimmed)
    }

    /**
     * Validates if the event/goal name is valid.
     * @param name Event name to validate
     * @return true if valid, false otherwise
     */
    fun isValidEventName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 1..100
    }

    /**
     * Validates if a color hex string is valid.
     * @param colorHex Color hex string (e.g., "#FF5733" or "#F53")
     * @return true if valid, false otherwise
     */
    fun isValidColorHex(colorHex: String): Boolean {
        val regex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})\$")
        return regex.matches(colorHex.trim())
    }
}
