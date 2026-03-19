package com.example.financetracker.domain

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.example.financetracker.core.parser.SmsParserEngine
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType

class SmsReader(private val context: Context) {

    private val TAG = "SmsReader"

    // Use the consolidated core parser engine
    private val parserEngine = SmsParserEngine()

    // Currency pattern for pre-filtering (kept for efficiency)
    private val currencyRegex = Regex("(?i)(?:Rs\\.?|INR|₹|\\$|€|£)\\s*[\\d,]+")

    private val transactionKeywords = listOf(
        "debited", "credited", "spent", "paid", "sent", "withdrawn",
        "txn", "transaction", "purchase", "transfer", "amt", "bk"
    )

    // Known bank sender prefixes for improved detection
    private val KNOWN_BANK_PREFIXES = listOf(
        "VM-", "VD-", "AD-", "AM-", "AX-", "TD-", "BZ-", "JD-", "IM-", "BP-",
        "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "IDBI", "PNB", "BOB", "IOB",
        "CITI", "HSBC", "YES", "INDUS", "RBL", "FEDERAL", "IDFC", "BANDHAN"
    )

    fun getTransactions(startTime: Long = 0): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        val selection = "${Telephony.Sms.Inbox.DATE} >= ?"
        val selectionArgs = arrayOf(startTime.toString())

        // Query recent SMS (Limit 500)
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox._ID),
            selection,
            selectionArgs,
            "${Telephony.Sms.Inbox.DATE} DESC LIMIT 500"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)
            val idIndex = it.getColumnIndex(Telephony.Sms.Inbox._ID)

            while (it.moveToNext()) {
                val sender = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val smsId = it.getLong(idIndex)

                // Pre-filter for efficiency before calling the parser
                if (isPotentialBankSender(sender) && isTransactionMessage(body)) {
                    parseSms(body, sender, date, smsId)?.let { tx ->
                        transactions.add(tx)
                    }
                }
            }
        }
        return transactions
    }

    /**
     * Delegates parsing to the core SmsParserEngine.
     * Converts the parsed result to the domain Transaction model.
     */
    private fun parseSms(body: String, sender: String, date: Long, smsId: Long): Transaction? {
        // Use the core parser engine for all parsing logic
        val parsed = parserEngine.parse(body, cleanSenderId(sender), date) ?: return null

        // Convert ParsedTransaction to domain Transaction
        val type = when (parsed.type) {
            SmsParserEngine.TransactionType.CREDIT -> TransactionType.CREDIT
            SmsParserEngine.TransactionType.DEBIT -> TransactionType.DEBIT
        }

        // Convert paisa back to rupees for the domain model
        val amountInRupees = parsed.amount / 100.0

        return Transaction(
            amount = amountInRupees,
            merchant = parsed.merchant,
            type = type,
            date = date,
            rawSender = sender,
            smsId = smsId
        )
    }

    /**
     * Cleans up sender ID for use as fallback merchant name.
     * Example: "VM-HDFCBK" -> "HDFCBK"
     */
    private fun cleanSenderId(sender: String): String {
        return if (sender.contains("-")) {
            sender.substringAfter("-")
        } else {
            sender
        }
    }

    // --- Helper Filters ---
    private fun isTransactionMessage(body: String): Boolean {
        val hasKeyword = transactionKeywords.any { body.contains(it, ignoreCase = true) }
        val hasCurrency = currencyRegex.containsMatchIn(body)
        return hasKeyword || hasCurrency
    }

    /**
     * Improved bank sender detection with allowlist.
     * Accepts known bank prefixes, short codes, and hyphenated codes.
     * Rejects personal mobile numbers.
     */
    private fun isPotentialBankSender(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Check against known bank prefixes first
        val isKnownBank = KNOWN_BANK_PREFIXES.any { upperSender.startsWith(it) }
        if (isKnownBank) return true

        // Existing logic for unknown senders
        val cleanSender = sender.replace("-", "")
        val isMobileNumber = sender.matches(Regex("^[0-9+]{10,13}$"))

        // Allow if it contains a hyphen (AX-HDFC) OR is short (HDFCBK)
        return !isMobileNumber && (sender.contains("-") || cleanSender.length <= 9)
    }
}
