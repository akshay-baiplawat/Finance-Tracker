package com.example.financetracker.core.parser

import java.util.Locale

/**
 * The "Brain" of the application.
 * Parses raw SMS bodies into structured Transaction data.
 * Pure Kotlin. No Android dependencies.
 */
class SmsParserEngine {

    data class ParsedTransaction(
        val amount: Long, // Stored as Paisa/Cents (smallest unit)
        val type: TransactionType,
        val merchant: String,
        val currency: String = "INR", // Currency code (INR, USD, EUR, GBP)
        val date: Long? = null, // Epoch
        val confidence: Float = 1.0f // Parsing confidence (0.0 to 1.0)
    )

    enum class TransactionType {
        DEBIT, CREDIT
    }

    companion object {
        // --- 1. DETECTION KEYWORDS ---
        private val DEBIT_KEYWORDS = listOf("spent", "debited", "paid", "sent", "transferred", "purchase")
        private val CREDIT_KEYWORDS = listOf("credited", "received", "refund", "deposited")
        
        // --- 2. THE "ENEMY" (The Ignore List) ---
        private val IGNORE_KEYWORDS = listOf(
            "otp", "auth code", "login", "verification code", // Security
            "account balance", "avail bal", "available limit", // Balance updates
            "loan", "offer", "apply now", "congrats", "winner", "prize" // Spam
        )

        // --- 3. PATTERNS ---
        // Multi-currency amount patterns (ordered by specificity)
        private val CURRENCY_PATTERNS = mapOf(
            "INR" to Regex("""(?i)(?:Rs\.?|INR|₹|rupees?)\s*(\d+(?:,\d+)*(?:\.\d{1,2})?)"""),
            "USD" to Regex("""(?i)(?:\$|USD|dollars?)\s*(\d+(?:,\d+)*(?:\.\d{1,2})?)"""),
            "EUR" to Regex("""(?i)(?:€|EUR|euros?)\s*(\d+(?:,\d+)*(?:\.\d{1,2})?)"""),
            "GBP" to Regex("""(?i)(?:£|GBP|pounds?)\s*(\d+(?:,\d+)*(?:\.\d{1,2})?)""")
        )

        // Matches typical date formats in banking SMS: 12-10-25, 12/10/2025, 30-NOV-25
        private val DATE_PATTERN = Regex("""(\d{1,2}[-./](?:\d{1,2}|[a-zA-Z]{3})[-./]\d{2,4})""")

        // Date trap pattern to avoid setting merchant as a date
        private val DATE_LIKE_MERCHANT_PATTERN = Regex("""^\d{1,2}[-./](?:\d{1,2}|[a-zA-Z]{3})[-./]\d{2,4}$""")
    }

    /**
     * Parses a single SMS body.
     * Returns a ParsedTransaction if valid, or null if ignored/invalid.
     */
    fun parse(body: String, senderId: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase(Locale.ROOT)

        // 1. Hygiene Check: The "Enemy" Filter
        if (IGNORE_KEYWORDS.any { lowerBody.contains(it) }) {
            return null
        }

        // 2. Detection: Determine Type (Debit vs Credit)
        val isDebit = DEBIT_KEYWORDS.any { lowerBody.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lowerBody.contains(it) }

        if (!isDebit && !isCredit) {
            return null // Not a transactional message
        }

        val type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT

        // 3. Extraction: Amount (with currency detection)
        val amountResult = extractAmount(body) ?: return null
        val (amount, currency) = amountResult

        // 4. Extraction: Merchant / Counterparty
        var merchant = extractMerchant(body, type)
            ?: senderId // Fallback to sender ID if extraction failed
        var confidence = if (merchant == senderId) 0.8f else 1.0f

        // Rule: Short String Fix
        if (merchant.length < 3) {
            merchant = senderId
            confidence = 0.6f
        }

        // Rule: Date Trap Fix
        if (DATE_LIKE_MERCHANT_PATTERN.matches(merchant)) {
            merchant = senderId
            confidence = 0.6f
        }

        // 5. Extraction: Date (Optional override from body)
        val parsedDate = extractDate(body)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant.trim(),
            currency = currency,
            date = parsedDate,
            confidence = confidence
        )
    }

    /**
     * Extracts amount and currency from SMS body.
     * Returns Pair<amount in smallest unit, currency code> or null if not found.
     */
    private fun extractAmount(body: String): Pair<Long, String>? {
        // Try each currency pattern in order (INR first as most common for Indian banks)
        for ((currencyCode, pattern) in CURRENCY_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val numberString = match.groupValues[1].replace(",", "")
                return try {
                    val doubleVal = numberString.toDouble()
                    // Convert to smallest unit (paisa/cents)
                    Pair((doubleVal * 100).toLong(), currencyCode)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Extracts date from SMS body if present.
     * Returns epoch milliseconds or null if not found/parseable.
     */
    private fun extractDate(body: String): Long? {
        val match = DATE_PATTERN.find(body) ?: return null
        val dateStr = match.groupValues[1]

        // Try common date formats
        val dateFormats = listOf(
            java.text.SimpleDateFormat("dd-MM-yyyy", Locale.US),
            java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US),
            java.text.SimpleDateFormat("dd-MM-yy", Locale.US),
            java.text.SimpleDateFormat("dd/MM/yy", Locale.US),
            java.text.SimpleDateFormat("dd-MMM-yyyy", Locale.US),
            java.text.SimpleDateFormat("dd-MMM-yy", Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        for (format in dateFormats) {
            try {
                format.isLenient = false
                val date = format.parse(dateStr)
                if (date != null) {
                    // Validate: date should be within reasonable range (not too old, not in future)
                    val now = System.currentTimeMillis()
                    val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000)
                    val oneWeekFromNow = now + (7L * 24 * 60 * 60 * 1000)

                    if (date.time in oneYearAgo..oneWeekFromNow) {
                        return date.time
                    }
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    private fun extractMerchant(body: String, type: TransactionType): String? {
        // Extended patterns for merchant extraction
        // Priority order: most specific patterns first

        // Pattern definitions with capturing groups for merchant name
        val patterns = listOf(
            // Primary patterns (most reliable)
            Regex("""(?i)\sat\s+([^.]+)"""),              // "at Starbucks"
            Regex("""(?i)\sto\s+([^.]+)"""),              // "to Amazon"
            Regex("""(?i)\sfrom\s+([^.]+)"""),            // "from Salary"

            // Secondary patterns (common variants)
            Regex("""(?i)\sfor\s+([^.]+)"""),             // "for Netflix" / "debited for Starbucks"
            Regex("""(?i)\svia\s+([^.]+)"""),             // "via Paytm"
            Regex("""(?i)spent\s+on\s+([^.]+)"""),        // "spent on Zomato"
            Regex("""(?i)paid\s+for\s+([^.]+)"""),        // "paid for Swiggy"
            Regex("""(?i)debited\s+for\s+([^.]+)"""),     // "debited for Amazon"

            // UPI-specific patterns
            Regex("""(?i)VPA[:\s]+([a-zA-Z0-9._-]+@[a-zA-Z]+)"""),  // "VPA: merchant@bank"
            Regex("""(?i)to\s+([a-zA-Z0-9._-]+@[a-zA-Z]+)"""),      // "to merchant@upi"

            // Card-specific patterns
            Regex("""(?i)card\s+(?:xx|ending)?\s*\d+\s+(?:used\s+)?(?:at|for)\s+([^.]+)""")  // "Card XX1234 used at Merchant"
        )

        // Try each pattern in order, return first valid match
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                val rawMerchant = match.groupValues[1]
                val cleaned = cleanMerchantName(rawMerchant)

                // Validate: not empty, not too short, not a date
                if (cleaned.isNotEmpty() && cleaned.length >= 2) {
                    return cleaned
                }
            }
        }

        return null
    }
    
    private fun cleanMerchantName(raw: String): String {
        // Stop at common delimiters (case-insensitive)
        val delimiters = listOf(" on ", " using ", " via ", " with ", " for ", " ending ", " txn", " ref", " utr")
        var cleaned = raw

        var minIndex = cleaned.length

        delimiters.forEach { delim ->
            val idx = cleaned.lowercase(Locale.ROOT).indexOf(delim)
            if (idx != -1 && idx < minIndex) {
                minIndex = idx
            }
        }

        cleaned = cleaned.substring(0, minIndex).trim()

        // Remove trailing special characters that are likely delimiters (not part of brand name)
        // Keep: & (H&M), - (7-Eleven), ' (McDonald's), . (P.F. Chang's)
        cleaned = cleaned.trimEnd { it in listOf('*', '#', '/', '\\', '|', ',', ';', ':') }

        // If result looks like a date (all digits and separators), reject it
        if (cleaned.matches(Regex("""^[\d\-./]+$"""))) {
            return ""
        }

        // If result is all numbers, reject it (likely a reference number)
        if (cleaned.all { it.isDigit() }) {
            return ""
        }

        return cleaned.trim()
    }
}
