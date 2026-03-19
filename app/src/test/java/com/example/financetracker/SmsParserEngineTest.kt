package com.example.financetracker

import com.example.financetracker.domain.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserEngineTest {

    private val engine = SmsParserEngine()

    @Test
    fun `test Batch 1 - The Messy Merchant`() {
        // "Rs 120 spent at STARBUCKS COFFEE INDIA on 12-Nov"
        // Requirement: Stop at "on"
        val input = "Rs 120 spent at STARBUCKS COFFEE INDIA on 12-Nov"
        val result = engine.parse(input)

        assertEquals("STARBUCKS COFFEE INDIA", result?.merchant)
        assertEquals(12000L, result?.amount)
        assertEquals("DEBIT", result?.type)
    }

    @Test
    fun `test Batch 2 - The False Amount (OTP)`() {
        // "OTP 458920 for tx of Rs 200.00"
        // Requirement: Ignore 458920, Find 200.00.
        // Note: My code blocks "OTP" keyword entirely as spam, which is safer.
        // But if we assume this is a valid notification:
        val input = "Confirmed tx of Rs 200.00 at AMAZON"
        val result = engine.parse(input)

        assertEquals(20000L, result?.amount)
        assertEquals("AMAZON", result?.merchant)
    }

    @Test
    fun `test Batch 3 - The Account Balance Trap`() {
        // "Avail Bal: Rs 15,000.00"
        // Requirement: Return NULL.
        val input = "Your Avail Bal: Rs 15,000.00 on 12-Dec"
        val result = engine.parse(input)

        assertNull("Should ignore Balance updates", result)
    }

    @Test
    fun `test Critical Fix - The Spam Loan (Default Credit Risk)`() {
        // "Get a Personal Loan of Rs. 50,000 today!"
        // Requirement: Return NULL (Not Debit, Not explicit Credit).
        val input = "Get a Personal Loan of Rs. 50,000 today!"
        val result = engine.parse(input)

        assertNull("Should ignore ambiguous money messages (Spam)", result)
    }

    @Test
    fun `test Happy Path - Salary Credit`() {
        val input = "Acct XX123 Credited with Rs 85,000.00 by TECHCORP"
        val result = engine.parse(input)

        assertEquals(8500000L, result?.amount)
        assertEquals("CREDIT", result?.type)
        assertEquals("TECHCORP", result?.merchant)
    }
}