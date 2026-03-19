package com.example.financetracker.core.util

import com.example.financetracker.data.local.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CSV utilities.
 * Tests the CSV escaping, unescaping, and type mapping logic.
 *
 * Note: Full integration tests (export/import round-trip) require
 * instrumented tests with Android context.
 */
class CsvUtilsTest {

    // ==================== CSV Escaping Tests ====================
    // Testing the escapeCsv logic as documented in CsvExportUtil

    /**
     * Helper to test CSV escaping logic.
     * Duplicates the private escapeCsv method from CsvExportUtil for unit testing.
     */
    private fun escapeCsv(value: String): String {
        var result = value

        // Protect against CSV formula injection
        val formulaChars = setOf('=', '+', '-', '@', '\t', '\r')
        if (result.isNotEmpty() && result[0] in formulaChars) {
            result = "'$result"
        }

        if (result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("'")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }

    @Test
    fun `escapeCsv - normal value unchanged`() {
        assertEquals("Starbucks", escapeCsv("Starbucks"))
        assertEquals("Coffee Shop", escapeCsv("Coffee Shop"))
    }

    @Test
    fun `escapeCsv - formula injection equals sign`() {
        val result = escapeCsv("=SUM(A1:A10)")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
        assertEquals("\"'=SUM(A1:A10)\"", result)
    }

    @Test
    fun `escapeCsv - formula injection plus sign`() {
        val result = escapeCsv("+1000")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
        assertEquals("\"'+1000\"", result)
    }

    @Test
    fun `escapeCsv - formula injection minus sign`() {
        val result = escapeCsv("-500")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
        assertEquals("\"'-500\"", result)
    }

    @Test
    fun `escapeCsv - formula injection at sign`() {
        val result = escapeCsv("@example.com")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
        assertEquals("\"'@example.com\"", result)
    }

    @Test
    fun `escapeCsv - formula injection tab`() {
        val result = escapeCsv("\tValue")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
    }

    @Test
    fun `escapeCsv - formula injection carriage return`() {
        val result = escapeCsv("\rValue")
        assertTrue("Should prefix with single quote", result.startsWith("\"'"))
    }

    @Test
    fun `escapeCsv - comma requires quoting`() {
        val result = escapeCsv("Starbucks, Inc")
        assertEquals("\"Starbucks, Inc\"", result)
    }

    @Test
    fun `escapeCsv - double quote escaping`() {
        val result = escapeCsv("He said \"yes\"")
        assertEquals("\"He said \"\"yes\"\"\"", result)
    }

    @Test
    fun `escapeCsv - newline requires quoting`() {
        val result = escapeCsv("Line1\nLine2")
        assertTrue("Should be quoted", result.startsWith("\""))
        assertTrue("Should be quoted", result.endsWith("\""))
    }

    @Test
    fun `escapeCsv - single quote requires quoting`() {
        val result = escapeCsv("O'Brien")
        assertEquals("\"O'Brien\"", result)
    }

    @Test
    fun `escapeCsv - empty string unchanged`() {
        assertEquals("", escapeCsv(""))
    }

    @Test
    fun `escapeCsv - combined formula injection and comma`() {
        // A formula with a comma should be both prefixed and quoted
        val result = escapeCsv("=SUM(A1,A10)")
        assertTrue("Should start with quote", result.startsWith("\""))
        assertTrue("Should contain prefix", result.contains("'="))
    }

    // ==================== CSV Unescaping Tests ====================
    // Testing the unescapeCsv logic as documented in CsvImportUtil

    /**
     * Helper to test CSV unescaping logic.
     * Duplicates the private unescapeCsv method from CsvImportUtil for unit testing.
     */
    private fun unescapeCsv(value: String): String {
        var result = value
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }
        return result.replace("\"\"", "\"")
    }

    @Test
    fun `unescapeCsv - normal value unchanged`() {
        assertEquals("Starbucks", unescapeCsv("Starbucks"))
    }

    @Test
    fun `unescapeCsv - removes surrounding quotes`() {
        assertEquals("Starbucks", unescapeCsv("\"Starbucks\""))
    }

    @Test
    fun `unescapeCsv - unescapes double quotes`() {
        assertEquals("He said \"yes\"", unescapeCsv("\"He said \"\"yes\"\"\""))
    }

    @Test
    fun `unescapeCsv - handles single quote inside`() {
        assertEquals("O'Brien", unescapeCsv("\"O'Brien\""))
    }

    @Test
    fun `unescapeCsv - empty quoted string`() {
        assertEquals("", unescapeCsv("\"\""))
    }

    @Test
    fun `unescapeCsv - formula prefix preserved`() {
        // After unescaping, the formula prefix is still there
        assertEquals("'=SUM(A1:A10)", unescapeCsv("\"'=SUM(A1:A10)\""))
    }

    @Test
    fun `unescapeCsv - partial quotes unchanged`() {
        // Only removes quotes if both start and end with quote
        assertEquals("\"Starbucks", unescapeCsv("\"Starbucks"))
        assertEquals("Starbucks\"", unescapeCsv("Starbucks\""))
    }

    // ==================== Type Mapping Tests ====================

    @Test
    fun `type mapping - EXPENSE variants`() {
        assertEquals(TransactionType.EXPENSE, mapTypeString("EXPENSE"))
        assertEquals(TransactionType.EXPENSE, mapTypeString("expense"))
        assertEquals(TransactionType.EXPENSE, mapTypeString("Expense"))
        assertEquals(TransactionType.EXPENSE, mapTypeString("DEBIT"))
        assertEquals(TransactionType.EXPENSE, mapTypeString("debit"))
        assertEquals(TransactionType.EXPENSE, mapTypeString("Debit"))
    }

    @Test
    fun `type mapping - INCOME variants`() {
        assertEquals(TransactionType.INCOME, mapTypeString("INCOME"))
        assertEquals(TransactionType.INCOME, mapTypeString("income"))
        assertEquals(TransactionType.INCOME, mapTypeString("Income"))
        assertEquals(TransactionType.INCOME, mapTypeString("CREDIT"))
        assertEquals(TransactionType.INCOME, mapTypeString("credit"))
        assertEquals(TransactionType.INCOME, mapTypeString("Credit"))
    }

    @Test
    fun `type mapping - TRANSFER`() {
        assertEquals(TransactionType.TRANSFER, mapTypeString("TRANSFER"))
        assertEquals(TransactionType.TRANSFER, mapTypeString("transfer"))
        assertEquals(TransactionType.TRANSFER, mapTypeString("Transfer"))
    }

    @Test
    fun `type mapping - unknown defaults to EXPENSE`() {
        assertEquals(TransactionType.EXPENSE, mapTypeString("UNKNOWN"))
        assertEquals(TransactionType.EXPENSE, mapTypeString(""))
        assertEquals(TransactionType.EXPENSE, mapTypeString("foo"))
    }

    /**
     * Helper to test type mapping logic.
     * Duplicates the type mapping from CsvImportUtil.parseCsv for unit testing.
     */
    private fun mapTypeString(typeStr: String): TransactionType {
        return when (typeStr.uppercase()) {
            "EXPENSE", "DEBIT" -> TransactionType.EXPENSE
            "INCOME", "CREDIT" -> TransactionType.INCOME
            "TRANSFER" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
    }

    // ==================== Amount Conversion Tests ====================

    @Test
    fun `amount string to paisa - valid amounts`() {
        assertEquals(10000L, amountToPaisa("100.00"))
        assertEquals(10050L, amountToPaisa("100.5"))
        assertEquals(100L, amountToPaisa("1"))
        assertEquals(1L, amountToPaisa("0.01"))
    }

    @Test
    fun `amount string to paisa - invalid amounts`() {
        assertEquals(0L, amountToPaisa(""))
        assertEquals(0L, amountToPaisa("abc"))
        assertEquals(0L, amountToPaisa("not a number"))
    }

    @Test
    fun `amount string to paisa - negative amounts`() {
        // Current implementation allows negative amounts
        assertEquals(-10000L, amountToPaisa("-100.00"))
    }

    /**
     * Helper to convert amount string to paisa.
     * Duplicates the conversion logic from CsvImportUtil.
     */
    private fun amountToPaisa(amountStr: String): Long {
        val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
        return (amountDouble * 100).toLong()
    }

    // ==================== Source Detection Tests ====================

    @Test
    fun `source detection - SMS prefix`() {
        assertEquals("SMS", detectSource("SMS: HDFC Bank"))
        assertEquals("SMS", detectSource("Transaction via SMS: alert"))
    }

    @Test
    fun `source detection - manual entries`() {
        assertEquals("MANUAL", detectSource("Coffee at Starbucks"))
        assertEquals("MANUAL", detectSource("Grocery shopping"))
        assertEquals("MANUAL", detectSource(""))
    }

    @Test
    fun `source detection - case sensitivity`() {
        // Current implementation is case-sensitive for SMS:
        assertEquals("MANUAL", detectSource("sms: lowercase"))
        assertEquals("SMS", detectSource("SMS: uppercase"))
    }

    /**
     * Helper to detect source from note.
     * Duplicates the source detection logic from CsvExportUtil.
     */
    private fun detectSource(note: String): String {
        return if (note.contains("SMS:")) "SMS" else "MANUAL"
    }

    // ==================== CSV Line Parsing Tests ====================

    @Test
    fun `csv parsing - simple line`() {
        val line = "2026-03-14 10:30:00,Starbucks,5.50,EXPENSE,Food"
        val tokens = parseCsvLine(line)
        assertEquals(5, tokens.size)
        assertEquals("2026-03-14 10:30:00", tokens[0])
        assertEquals("Starbucks", tokens[1])
        assertEquals("5.50", tokens[2])
        assertEquals("EXPENSE", tokens[3])
        assertEquals("Food", tokens[4])
    }

    @Test
    fun `csv parsing - quoted comma`() {
        val line = "2026-03-14 10:30:00,\"Starbucks, Inc\",5.50,EXPENSE,Food"
        val tokens = parseCsvLine(line)
        assertEquals(5, tokens.size)
        assertEquals("\"Starbucks, Inc\"", tokens[1])
    }

    @Test
    fun `csv parsing - extra columns ignored`() {
        val line = "2026-03-14 10:30:00,Starbucks,5.50,EXPENSE,Food,SMS,extra"
        val tokens = parseCsvLine(line)
        assertTrue(tokens.size >= 5)
    }

    /**
     * Helper to parse CSV line.
     * Uses the same regex as CsvImportUtil.
     */
    private fun parseCsvLine(line: String): List<String> {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
    }

    // ==================== Round-trip Logic Tests ====================

    @Test
    fun `roundtrip - normal value`() {
        val original = "Starbucks"
        val escaped = escapeCsv(original)
        val unescaped = unescapeCsv(escaped)
        assertEquals(original, unescaped)
    }

    @Test
    fun `roundtrip - value with comma`() {
        val original = "Starbucks, Inc"
        val escaped = escapeCsv(original)
        val unescaped = unescapeCsv(escaped)
        assertEquals(original, unescaped)
    }

    @Test
    fun `roundtrip - value with quotes`() {
        val original = "He said \"yes\""
        val escaped = escapeCsv(original)
        val unescaped = unescapeCsv(escaped)
        assertEquals(original, unescaped)
    }

    @Test
    fun `roundtrip - formula injection is one-way`() {
        // Formula injection adds a prefix that isn't removed on import
        // This is intentional - prevents formulas from executing
        val original = "=SUM(A1:A10)"
        val escaped = escapeCsv(original)
        val unescaped = unescapeCsv(escaped)
        // The formula prefix is preserved
        assertEquals("'=SUM(A1:A10)", unescaped)
    }
}
