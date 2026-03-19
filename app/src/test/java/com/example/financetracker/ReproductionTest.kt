package com.example.financetracker

import com.example.financetracker.core.parser.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for previously known limitations that should now pass.
 * These tests document edge cases that were fixed in the SMS parsing improvements.
 */
class ReproductionTest {

    private val engine = SmsParserEngine()

    @Test
    fun `repro - Merchant with ampersand`() {
        // Fixed: Special characters like & are now preserved in brand names
        val input = "Paid Rs 500.00 to H&M Store for clothes"
        val result = engine.parse(input, "HDFC-BANK", System.currentTimeMillis())

        assertNotNull(result)
        assertEquals("H&M Store", result?.merchant)
    }

    @Test
    fun `repro - Merchant with hyphen`() {
        // Fixed: Hyphens in brand names (7-Eleven) are preserved
        val input = "Spent INR 1,200 at 7-Eleven."
        val result = engine.parse(input, "SBI-UPI", System.currentTimeMillis())

        assertNotNull(result)
        assertEquals("7-Eleven", result?.merchant)
    }

    @Test
    fun `repro - Missing Preposition - debited for`() {
        // Fixed: "debited for" pattern is now supported
        val input = "Rs 200 debited for Starbucks Coffee"
        val result = engine.parse(input, "ICICI-BANK", System.currentTimeMillis())

        assertNotNull(result)
        assertEquals("Starbucks Coffee", result?.merchant)
    }

    @Test
    fun `repro - Spent on pattern`() {
        // Fixed: "spent on" pattern is now supported
        val input = "Rs 350 spent on Zomato delivery"
        val result = engine.parse(input, "HDFC-BANK", System.currentTimeMillis())

        assertNotNull(result)
        assertEquals("Zomato delivery", result?.merchant)
    }

    @Test
    fun `repro - Dollar Currency`() {
        // Fixed: Dollar amounts are now supported via multi-currency patterns
        val input = "You paid $100.00 at Walmart on your card"
        val result = engine.parse(input, "CITI-BANK", System.currentTimeMillis())

        assertNotNull("Should parse Dollar amounts", result)
        assertEquals(10000L, result?.amount) // $100.00 = 10000 cents
        assertEquals("USD", result?.currency)
        assertEquals("Walmart", result?.merchant)
    }

    @Test
    fun `repro - UPI VPA pattern`() {
        // New: UPI VPA patterns are now supported
        val input = "Rs 500 paid to merchant@upi via UPI"
        val result = engine.parse(input, "PAYTM-UPI", System.currentTimeMillis())

        assertNotNull(result)
        assertEquals("merchant@upi", result?.merchant)
    }
}
