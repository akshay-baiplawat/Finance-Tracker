package com.example.financetracker.core.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ValidationUtils.
 * Tests all validation functions with valid inputs, invalid inputs, and edge cases.
 */
class ValidationUtilsTest {

    // ==================== isValidAmount Tests ====================

    @Test
    fun `isValidAmount - valid integer amounts`() {
        assertTrue(ValidationUtils.isValidAmount("1"))
        assertTrue(ValidationUtils.isValidAmount("100"))
        assertTrue(ValidationUtils.isValidAmount("9999999"))
    }

    @Test
    fun `isValidAmount - valid decimal amounts`() {
        assertTrue(ValidationUtils.isValidAmount("0.01"))
        assertTrue(ValidationUtils.isValidAmount("100.00"))
        assertTrue(ValidationUtils.isValidAmount("100.5"))
        assertTrue(ValidationUtils.isValidAmount("99999999.99")) // Max value
    }

    @Test
    fun `isValidAmount - invalid formats`() {
        assertFalse("Should reject blank", ValidationUtils.isValidAmount(""))
        assertFalse("Should reject whitespace only", ValidationUtils.isValidAmount("   "))
        assertFalse("Should reject 3 decimals", ValidationUtils.isValidAmount("100.000"))
        assertFalse("Should reject double dots", ValidationUtils.isValidAmount("100.99.99"))
        assertFalse("Should reject leading dot", ValidationUtils.isValidAmount(".99"))
        // Note: "100." is allowed by the regex (matches 0 decimal digits after dot)
        assertFalse("Should reject negative", ValidationUtils.isValidAmount("-100"))
        assertFalse("Should reject letters", ValidationUtils.isValidAmount("abc"))
        assertFalse("Should reject mixed", ValidationUtils.isValidAmount("1a0"))
        assertFalse("Should reject scientific notation", ValidationUtils.isValidAmount("1e10"))
    }

    @Test
    fun `isValidAmount - boundary values`() {
        // Minimum: 1 paisa = 0.01
        assertTrue("Minimum value should pass", ValidationUtils.isValidAmount("0.01"))
        assertFalse("Zero should fail", ValidationUtils.isValidAmount("0"))
        assertFalse("Zero decimal should fail", ValidationUtils.isValidAmount("0.00"))

        // Maximum: ₹99,99,99,999.99 = 9999999999 paisa
        assertTrue("Max value should pass", ValidationUtils.isValidAmount("99999999.99"))
        assertFalse("Over max should fail", ValidationUtils.isValidAmount("100000000.00"))
    }

    @Test
    fun `isValidAmount - whitespace handling`() {
        assertTrue("Should trim leading spaces", ValidationUtils.isValidAmount("  100"))
        assertTrue("Should trim trailing spaces", ValidationUtils.isValidAmount("100  "))
        assertTrue("Should trim both", ValidationUtils.isValidAmount("  100  "))
    }

    // ==================== isValidAmountPaisa Tests ====================

    @Test
    fun `isValidAmountPaisa - valid amounts`() {
        assertTrue(ValidationUtils.isValidAmountPaisa(1L))
        assertTrue(ValidationUtils.isValidAmountPaisa(10000L)) // ₹100
        assertTrue(ValidationUtils.isValidAmountPaisa(9999999999L)) // Max
    }

    @Test
    fun `isValidAmountPaisa - boundary values`() {
        // Minimum
        assertTrue("1 paisa should pass", ValidationUtils.isValidAmountPaisa(1L))
        assertFalse("0 paisa should fail", ValidationUtils.isValidAmountPaisa(0L))

        // Maximum
        assertTrue("Max paisa should pass", ValidationUtils.isValidAmountPaisa(9999999999L))
        assertFalse("Over max should fail", ValidationUtils.isValidAmountPaisa(10000000000L))
    }

    @Test
    fun `isValidAmountPaisa - negative values`() {
        assertFalse(ValidationUtils.isValidAmountPaisa(-1L))
        assertFalse(ValidationUtils.isValidAmountPaisa(-100L))
        assertFalse(ValidationUtils.isValidAmountPaisa(Long.MIN_VALUE))
    }

    // ==================== isValidCategoryName Tests ====================

    @Test
    fun `isValidCategoryName - valid names`() {
        assertTrue(ValidationUtils.isValidCategoryName("Food"))
        assertTrue(ValidationUtils.isValidCategoryName("Food & Dining"))
        assertTrue(ValidationUtils.isValidCategoryName("Utilities-Gas"))
        assertTrue(ValidationUtils.isValidCategoryName("Category's Name"))
        assertTrue(ValidationUtils.isValidCategoryName("Category, Inc."))
        assertTrue(ValidationUtils.isValidCategoryName("Category123"))
    }

    @Test
    fun `isValidCategoryName - length boundaries`() {
        // Minimum: 1 character
        assertTrue("1 char should pass", ValidationUtils.isValidCategoryName("A"))
        assertFalse("Empty should fail", ValidationUtils.isValidCategoryName(""))

        // Maximum: 50 characters
        val fiftyChars = "A".repeat(50)
        assertTrue("50 chars should pass", ValidationUtils.isValidCategoryName(fiftyChars))

        val fiftyOneChars = "A".repeat(51)
        assertFalse("51 chars should fail", ValidationUtils.isValidCategoryName(fiftyOneChars))
    }

    @Test
    fun `isValidCategoryName - invalid characters`() {
        assertFalse("@ should fail", ValidationUtils.isValidCategoryName("Category@Name"))
        assertFalse("# should fail", ValidationUtils.isValidCategoryName("Category#"))
        assertFalse("! should fail", ValidationUtils.isValidCategoryName("Category!"))
        assertFalse("( should fail", ValidationUtils.isValidCategoryName("Category(test)"))
    }

    @Test
    fun `isValidCategoryName - whitespace handling`() {
        assertFalse("Whitespace only should fail", ValidationUtils.isValidCategoryName("   "))
        assertTrue("With spaces should pass", ValidationUtils.isValidCategoryName("Food and Dining"))
    }

    // ==================== isValidEmail Tests ====================
    // Note: These tests may fail in unit tests because Patterns.EMAIL_ADDRESS
    // is an Android framework class that returns null in JVM unit tests.
    // Consider using Robolectric or moving to instrumented tests.

    // ==================== sanitizeMerchantName Tests ====================

    @Test
    fun `sanitizeMerchantName - basic sanitization`() {
        assertEquals("Starbucks", ValidationUtils.sanitizeMerchantName("Starbucks"))
        assertEquals("Starbucks", ValidationUtils.sanitizeMerchantName("  Starbucks  "))
    }

    @Test
    fun `sanitizeMerchantName - multiple whitespace`() {
        assertEquals("Starbucks Coffee", ValidationUtils.sanitizeMerchantName("Starbucks   Coffee"))
        assertEquals("Starbucks Coffee Shop", ValidationUtils.sanitizeMerchantName("Starbucks  Coffee   Shop"))
    }

    @Test
    fun `sanitizeMerchantName - tabs and special whitespace`() {
        assertEquals("Starbucks Coffee", ValidationUtils.sanitizeMerchantName("Starbucks\tCoffee"))
        assertEquals("Starbucks Coffee", ValidationUtils.sanitizeMerchantName("Starbucks\nCoffee"))
    }

    @Test
    fun `sanitizeMerchantName - truncation at 100 chars`() {
        val longName = "A".repeat(150)
        val result = ValidationUtils.sanitizeMerchantName(longName)
        assertEquals(100, result.length)
    }

    @Test
    fun `sanitizeMerchantName - preserves unicode`() {
        assertEquals("Café", ValidationUtils.sanitizeMerchantName("Café"))
        assertEquals("日本レストラン", ValidationUtils.sanitizeMerchantName("日本レストラン"))
    }

    // ==================== isValidWalletName Tests ====================

    @Test
    fun `isValidWalletName - valid names`() {
        assertTrue(ValidationUtils.isValidWalletName("My Bank Account"))
        assertTrue(ValidationUtils.isValidWalletName("Cash"))
        assertTrue(ValidationUtils.isValidWalletName("A"))
    }

    @Test
    fun `isValidWalletName - length boundaries`() {
        // Minimum: 1 character
        assertTrue("1 char should pass", ValidationUtils.isValidWalletName("A"))
        assertFalse("Empty should fail", ValidationUtils.isValidWalletName(""))

        // Maximum: 50 characters
        val fiftyChars = "A".repeat(50)
        assertTrue("50 chars should pass", ValidationUtils.isValidWalletName(fiftyChars))

        val fiftyOneChars = "A".repeat(51)
        assertFalse("51 chars should fail", ValidationUtils.isValidWalletName(fiftyOneChars))
    }

    @Test
    fun `isValidWalletName - whitespace only`() {
        // Note: Current implementation allows whitespace-only names
        // This tests current behavior - may want to change in future
        assertFalse("Whitespace only should fail after trim", ValidationUtils.isValidWalletName("   "))
    }

    // ==================== isValidPersonName Tests ====================

    @Test
    fun `isValidPersonName - valid names`() {
        assertTrue(ValidationUtils.isValidPersonName("John Doe"))
        assertTrue(ValidationUtils.isValidPersonName("Mary-Jane"))
        assertTrue(ValidationUtils.isValidPersonName("O'Brien"))
        assertTrue(ValidationUtils.isValidPersonName("Jean-Claude"))
    }

    @Test
    fun `isValidPersonName - unicode names`() {
        assertTrue(ValidationUtils.isValidPersonName("José"))
        assertTrue(ValidationUtils.isValidPersonName("李明"))
        assertTrue(ValidationUtils.isValidPersonName("Müller"))
        assertTrue(ValidationUtils.isValidPersonName("François"))
    }

    @Test
    fun `isValidPersonName - length boundaries`() {
        // Minimum: 1 character
        assertTrue("1 char should pass", ValidationUtils.isValidPersonName("A"))
        assertFalse("Empty should fail", ValidationUtils.isValidPersonName(""))

        // Maximum: 100 characters
        val hundredChars = "A".repeat(100)
        assertTrue("100 chars should pass", ValidationUtils.isValidPersonName(hundredChars))

        val hundredOneChars = "A".repeat(101)
        assertFalse("101 chars should fail", ValidationUtils.isValidPersonName(hundredOneChars))
    }

    @Test
    fun `isValidPersonName - invalid characters`() {
        assertFalse("Numbers should fail", ValidationUtils.isValidPersonName("John123"))
        assertFalse("@ should fail", ValidationUtils.isValidPersonName("John@Doe"))
        assertFalse("# should fail", ValidationUtils.isValidPersonName("John#"))
    }

    @Test
    fun `isValidPersonName - apostrophes allowed`() {
        assertTrue(ValidationUtils.isValidPersonName("'John"))
        assertTrue(ValidationUtils.isValidPersonName("John'"))
        assertTrue(ValidationUtils.isValidPersonName("D'Angelo"))
    }

    // ==================== isValidEventName Tests ====================

    @Test
    fun `isValidEventName - valid names`() {
        assertTrue(ValidationUtils.isValidEventName("Summer Trip"))
        assertTrue(ValidationUtils.isValidEventName("Save for House"))
        assertTrue(ValidationUtils.isValidEventName("Pay Off Debt"))
        assertTrue(ValidationUtils.isValidEventName("Event123!@#"))
    }

    @Test
    fun `isValidEventName - length boundaries`() {
        // Minimum: 1 character
        assertTrue("1 char should pass", ValidationUtils.isValidEventName("A"))
        assertFalse("Empty should fail", ValidationUtils.isValidEventName(""))

        // Maximum: 100 characters
        val hundredChars = "A".repeat(100)
        assertTrue("100 chars should pass", ValidationUtils.isValidEventName(hundredChars))

        val hundredOneChars = "A".repeat(101)
        assertFalse("101 chars should fail", ValidationUtils.isValidEventName(hundredOneChars))
    }

    @Test
    fun `isValidEventName - special characters allowed`() {
        assertTrue(ValidationUtils.isValidEventName("Trip!"))
        assertTrue(ValidationUtils.isValidEventName("Save $$$"))
        assertTrue(ValidationUtils.isValidEventName("Goal @2024"))
    }

    // ==================== isValidColorHex Tests ====================

    @Test
    fun `isValidColorHex - valid 6-digit hex`() {
        assertTrue(ValidationUtils.isValidColorHex("#FF0000"))
        assertTrue(ValidationUtils.isValidColorHex("#00ff00"))
        assertTrue(ValidationUtils.isValidColorHex("#ABCDEF"))
        assertTrue(ValidationUtils.isValidColorHex("#abcdef"))
    }

    @Test
    fun `isValidColorHex - valid 3-digit hex`() {
        assertTrue(ValidationUtils.isValidColorHex("#FFF"))
        assertTrue(ValidationUtils.isValidColorHex("#fff"))
        assertTrue(ValidationUtils.isValidColorHex("#ABC"))
        assertTrue(ValidationUtils.isValidColorHex("#abc"))
    }

    @Test
    fun `isValidColorHex - invalid formats`() {
        assertFalse("4 digits should fail", ValidationUtils.isValidColorHex("#FF00"))
        assertFalse("5 digits should fail", ValidationUtils.isValidColorHex("#FF000"))
        assertFalse("7 digits should fail", ValidationUtils.isValidColorHex("#FF00000"))
        assertFalse("Non-hex chars should fail", ValidationUtils.isValidColorHex("#GGGGGG"))
        assertFalse("Missing hash should fail", ValidationUtils.isValidColorHex("FF0000"))
        assertFalse("Empty should fail", ValidationUtils.isValidColorHex(""))
        assertFalse("Hash only should fail", ValidationUtils.isValidColorHex("#"))
    }

    @Test
    fun `isValidColorHex - case insensitive`() {
        assertTrue(ValidationUtils.isValidColorHex("#ABC"))
        assertTrue(ValidationUtils.isValidColorHex("#abc"))
        assertTrue(ValidationUtils.isValidColorHex("#AbC"))
        assertTrue(ValidationUtils.isValidColorHex("#AABBCC"))
        assertTrue(ValidationUtils.isValidColorHex("#aabbcc"))
        assertTrue(ValidationUtils.isValidColorHex("#AaBbCc"))
    }

    @Test
    fun `isValidColorHex - whitespace handling`() {
        assertTrue("Should trim spaces", ValidationUtils.isValidColorHex("  #FFF  "))
    }
}
