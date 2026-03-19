package com.example.financetracker.domain

import java.util.Date
import javax.inject.Inject // <--- Added Import

data class ParsedTransaction(
    val amount: Long,      // Stored in Paisa (e.g., 1000 = Rs. 10.00)
    val merchant: String,
    val type: String,      // "DEBIT" or "CREDIT"
    val date: Date? = null
)

// ADDED: @Inject constructor()
class SmsParser @Inject constructor() {

    fun parse(sms: String): ParsedTransaction? {
        // 1. Guard Clause: Ignore OTPs and Spam immediately
        if (sms.contains("OTP", ignoreCase = true) || sms.contains("Auth Code", ignoreCase = true)) {
            return null
        }

        // 2. Extract Amount
        val amountRegex = Regex("""(?i)(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""")
        val amountMatch = amountRegex.find(sms) ?: return null

        // Clean the string: "1,250.00" -> "1250.00"
        val rawAmountString = amountMatch.groupValues[1].replace(",", "")

        // Convert to Paisa (Long)
        val amountPaisa = try {
            (rawAmountString.toDouble() * 100).toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        // 3. Determine Type (Debit/Credit)
        val type = when {
            sms.contains("debited", ignoreCase = true) -> "DEBIT"
            sms.contains("credited", ignoreCase = true) -> "CREDIT"
            else -> return null
        }

        // 4. Extract Merchant
        val merchantRegex = Regex("""(?i)(?:to|at)\s+([A-Za-z0-9]+)""")
        val merchantMatch = merchantRegex.find(sms)

        val merchant = merchantMatch?.groupValues?.get(1) ?: "Unknown"

        return ParsedTransaction(
            amount = amountPaisa,
            merchant = merchant.uppercase(),
            type = type,
            date = Date()
        )
    }
}