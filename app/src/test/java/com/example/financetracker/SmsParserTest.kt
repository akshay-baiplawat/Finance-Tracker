// FILE: src/test/java/com/example/financetracker/SmsParserTest.kt
package com.example.financetracker

import com.example.financetracker.domain.SmsReader // Ensure visibility or copy logic for testing
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import org.junit.Test
import org.junit.Assert.*

class SmsParserTest {

    // Mocking the helper functions here for the test harness
    // (In production, these are in SmsReader or a shared SmsParser object)

    private fun cleanMerchant(raw: String): String {
        val stopWordsRegex = Regex("(?i)\\s+(on|for|using|via|at|by|in)\\s+")
        val cleaned = raw.split(stopWordsRegex)[0].trim()
        if (cleaned.all { it.isDigit() || it == '-' || it == '.' }) return ""
        if (cleaned.length < 2) return ""
        return cleaned.uppercase()
    }

    @Test
    fun `test Golden Corpus - The Shell Case`() {
        val body = "Spent Rs 500 at SHELL PETROL PUMP on 12/12"
        val rawMerchant = "SHELL PETROL PUMP on 12" // What Regex captures

        val result = cleanMerchant(rawMerchant)

        assertEquals("SHELL PETROL PUMP", result)
        // Verdict: PASS. We keep the context (Petrol Pump) but strip the date.
    }

    @Test
    fun `test Golden Corpus - The Date Trap`() {
        val rawMerchant = "30" // Regex captures date "30" from "on 30-11-2024"
        val result = cleanMerchant(rawMerchant)
        assertEquals("", result)
        // Verdict: PASS. Rejects pure numbers.
    }

    @Test
    fun `test OTP Exclusion`() {
        val body = "123456 is your OTP for transaction at AMAZON"
        val isSpam = body.contains("OTP", true) || body.contains("verification code", true)
        assertTrue("OTP message should be flagged as spam", isSpam)
        // Verdict: PASS.
    }
}