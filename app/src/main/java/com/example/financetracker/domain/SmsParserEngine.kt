package com.example.financetracker.domain

import java.math.BigDecimal

data class ParsedResult(val amount: Long, val merchant: String, val type: String)

class SmsParserEngine {

    // 1. AMOUNT: Expanded to support $, Rs, INR, ₹, and generic numbers if context implies it
    private val amountRegex = Regex("(?i)(?:Rs\\.?|INR|₹|\\$)\\s?([\\d,]+(?:\\.\\d{2})?)")

    // 2. MERCHANT: Expanded char class to allow &, -, . within names
    private val merchantRegex = Regex("(?i)(?:\\sat|\\sto|\\son|\\spaid|\\svia|\\sby|\\sfor)\\s+([A-Za-z0-9&\\-. ]+)")

    private val spamKeywords = listOf("otp", "verification code", "auth code")
    private val balanceKeywords = listOf("avail bal", "balance", "bal:", "available")

    fun parse(rawMessage: String): ParsedResult? {
        val lowerMsg = rawMessage.lowercase()

        // --- FILTER 1: The "Account Balance" Trap ---
        if (balanceKeywords.any { lowerMsg.contains(it) }) return null

        // --- FILTER 2: Spam/OTP ---
        if (spamKeywords.any { lowerMsg.contains(it) }) return null

        // --- STEP 1: Extract Amount ---
        val amountMatch = amountRegex.find(rawMessage) ?: return null
        val amountString = amountMatch.groupValues[1].replace(",", "")

        val amountPaisa = try {
            BigDecimal(amountString).multiply(BigDecimal(100)).toLong()
        } catch (e: Exception) {
            return null
        }

        // --- STEP 2: Strict Type Detection ---
        val isDebit = lowerMsg.contains("debited") ||
                lowerMsg.contains("spent") ||
                lowerMsg.contains("sent") ||
                lowerMsg.contains("paid") ||
                lowerMsg.contains("withdrawn") ||
                lowerMsg.contains("tx") ||
                lowerMsg.contains("transaction") ||
                lowerMsg.contains("purchase")

        val isCredit = lowerMsg.contains("credited") ||
                lowerMsg.contains("received") ||
                lowerMsg.contains("deposited") ||
                lowerMsg.contains("refund")

        val type = when {
            isDebit -> "DEBIT"
            isCredit -> "CREDIT"
            else -> return null
        }

        // --- STEP 3: Merchant Extraction & Cleaning ---
        var merchant = "Unknown"
        val merchantMatch = merchantRegex.find(rawMessage)
        val rawMerchant = merchantMatch?.groupValues?.get(1)

        if (rawMerchant != null) {
            merchant = cleanMerchantName(rawMerchant)
        } else {
             // Fallback: If no preposition, check if we can heuristic match something?
             // For now, let's stick to "Unknown" to be safe, but the Regex now includes "for".
        }

        return ParsedResult(amountPaisa, merchant, type)
    }

    private fun cleanMerchantName(raw: String): String {
        val stopWordsRegex = Regex("(?i)\\s+(on|for|using|via|at|by|in|ref)\\s+")
        var cleaned = raw.split(stopWordsRegex)[0].trim()
        
        // Fix: Don't aggressively strip special chars needed for brands (H&M, 7-Eleven)
        // Only strip strictly invalid chars at the end
        cleaned = cleaned.replace(Regex("[^A-Za-z0-9&\\-. ]+$"), "")
        
        // Extra cleanup: Remove trailing dots or hyphens which might be sentence delimiters
        cleaned = cleaned.trimEnd('.', '-', ' ')

        if (cleaned.all { it.isDigit() || it == '-' || it == '.' || it == '/' }) return "Unknown"
        if (cleaned.length < 2) return "Unknown"

        return cleaned.uppercase()
    }
}