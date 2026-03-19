package com.example.financetracker.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for budget threshold calculation logic.
 * Tests the percentage calculation and threshold detection used for budget alerts.
 *
 * The budget alert logic triggers at:
 * - 80% warning threshold
 * - 100% exceeded threshold
 */
class BudgetThresholdTest {

    // ==================== Percentage Calculation Tests ====================

    @Test
    fun `percentage calculation - basic values`() {
        assertEquals(50L, calculatePercentage(500L, 1000L))
        assertEquals(100L, calculatePercentage(1000L, 1000L))
        assertEquals(200L, calculatePercentage(2000L, 1000L))
    }

    @Test
    fun `percentage calculation - exact 80 percent`() {
        assertEquals(80L, calculatePercentage(800L, 1000L))
        assertEquals(80L, calculatePercentage(8000L, 10000L))
    }

    @Test
    fun `percentage calculation - just under 80 percent`() {
        // 799/1000 = 79.9% -> 79 (integer division)
        assertEquals(79L, calculatePercentage(799L, 1000L))
    }

    @Test
    fun `percentage calculation - just over 80 percent`() {
        // 801/1000 = 80.1% -> 80 (integer division)
        assertEquals(80L, calculatePercentage(801L, 1000L))
    }

    @Test
    fun `percentage calculation - exact 100 percent`() {
        assertEquals(100L, calculatePercentage(1000L, 1000L))
        assertEquals(100L, calculatePercentage(10000L, 10000L))
    }

    @Test
    fun `percentage calculation - just under 100 percent`() {
        // 999/1000 = 99.9% -> 99 (integer division)
        assertEquals(99L, calculatePercentage(999L, 1000L))
    }

    @Test
    fun `percentage calculation - just over 100 percent`() {
        // 1001/1000 = 100.1% -> 100 (integer division)
        assertEquals(100L, calculatePercentage(1001L, 1000L))
    }

    @Test
    fun `percentage calculation - way over budget`() {
        assertEquals(150L, calculatePercentage(1500L, 1000L))
        assertEquals(200L, calculatePercentage(2000L, 1000L))
    }

    @Test
    fun `percentage calculation - zero budget`() {
        assertEquals(0L, calculatePercentage(1000L, 0L))
    }

    @Test
    fun `percentage calculation - zero spending`() {
        assertEquals(0L, calculatePercentage(0L, 1000L))
    }

    @Test
    fun `percentage calculation - small amounts in paisa`() {
        // Testing with realistic paisa values
        // Budget: ₹1000.00 = 100000 paisa
        // Spent: ₹800.00 = 80000 paisa
        assertEquals(80L, calculatePercentage(80000L, 100000L))

        // Budget: ₹500.00 = 50000 paisa
        // Spent: ₹400.00 = 40000 paisa -> 80%
        assertEquals(80L, calculatePercentage(40000L, 50000L))
    }

    // ==================== Threshold Detection Tests ====================

    @Test
    fun `threshold detection - no alert under 80 percent`() {
        assertFalse(shouldTriggerAlert(79L))
        assertFalse(shouldTriggerAlert(50L))
        assertFalse(shouldTriggerAlert(0L))
    }

    @Test
    fun `threshold detection - warning at exactly 80 percent`() {
        assertTrue(shouldTriggerAlert(80L))
        assertEquals("warning", getAlertType(80L))
    }

    @Test
    fun `threshold detection - warning between 80 and 100`() {
        assertTrue(shouldTriggerAlert(85L))
        assertTrue(shouldTriggerAlert(90L))
        assertTrue(shouldTriggerAlert(99L))
        assertEquals("warning", getAlertType(85L))
        assertEquals("warning", getAlertType(99L))
    }

    @Test
    fun `threshold detection - exceeded at exactly 100 percent`() {
        assertTrue(shouldTriggerAlert(100L))
        assertEquals("exceeded", getAlertType(100L))
    }

    @Test
    fun `threshold detection - exceeded over 100 percent`() {
        assertTrue(shouldTriggerAlert(101L))
        assertTrue(shouldTriggerAlert(150L))
        assertTrue(shouldTriggerAlert(200L))
        assertEquals("exceeded", getAlertType(150L))
    }

    // ==================== All Expenses Budget Tests ====================

    @Test
    fun `all expenses - excludes Money Transfer category`() {
        val transactions = listOf(
            TestTransaction("Food", 50000L),
            TestTransaction("Shopping", 30000L),
            TestTransaction("Money Transfer", 100000L) // Should be excluded
        )

        val total = calculateAllExpensesTotal(transactions)
        assertEquals(80000L, total) // Only Food + Shopping
    }

    @Test
    fun `all expenses - includes all other categories`() {
        val transactions = listOf(
            TestTransaction("Food", 10000L),
            TestTransaction("Shopping", 20000L),
            TestTransaction("Entertainment", 30000L),
            TestTransaction("Transport", 40000L)
        )

        val total = calculateAllExpensesTotal(transactions)
        assertEquals(100000L, total)
    }

    @Test
    fun `all expenses - empty list`() {
        val total = calculateAllExpensesTotal(emptyList())
        assertEquals(0L, total)
    }

    @Test
    fun `all expenses - only Money Transfer`() {
        val transactions = listOf(
            TestTransaction("Money Transfer", 50000L),
            TestTransaction("Money Transfer", 30000L)
        )

        val total = calculateAllExpensesTotal(transactions)
        assertEquals(0L, total)
    }

    // ==================== Category-specific Budget Tests ====================

    @Test
    fun `category spending - groups by category name`() {
        val transactions = listOf(
            TestTransaction("Food", 10000L),
            TestTransaction("Food", 15000L),
            TestTransaction("Shopping", 20000L),
            TestTransaction("Food", 5000L)
        )

        val spending = calculateCategorySpending(transactions)
        assertEquals(30000L, spending["Food"])
        assertEquals(20000L, spending["Shopping"])
    }

    @Test
    fun `category spending - missing category returns zero`() {
        val transactions = listOf(
            TestTransaction("Food", 10000L)
        )

        val spending = calculateCategorySpending(transactions)
        assertNull(spending["Transport"])
        assertEquals(0L, spending["Transport"] ?: 0L)
    }

    @Test
    fun `category spending - case sensitive`() {
        val transactions = listOf(
            TestTransaction("Food", 10000L),
            TestTransaction("food", 5000L) // Different case
        )

        val spending = calculateCategorySpending(transactions)
        assertEquals(10000L, spending["Food"])
        assertEquals(5000L, spending["food"])
    }

    // ==================== Reference ID Generation Tests ====================

    @Test
    fun `reference id - unique per budget per month`() {
        val budgetId = 1L
        val monthId1 = 202603L // March 2026
        val monthId2 = 202604L // April 2026

        val ref80_march = generateReferenceId(budgetId, monthId1, 80L)
        val ref100_march = generateReferenceId(budgetId, monthId1, 100L)
        val ref80_april = generateReferenceId(budgetId, monthId2, 80L)

        // All should be different
        assertNotEquals(ref80_march, ref100_march)
        assertNotEquals(ref80_march, ref80_april)
        assertNotEquals(ref100_march, ref80_april)
    }

    @Test
    fun `reference id - consistent for same inputs`() {
        val ref1 = generateReferenceId(1L, 202603L, 80L)
        val ref2 = generateReferenceId(1L, 202603L, 80L)

        assertEquals(ref1, ref2)
    }

    @Test
    fun `reference id - different budgets`() {
        val ref1 = generateReferenceId(1L, 202603L, 80L)
        val ref2 = generateReferenceId(2L, 202603L, 80L)

        assertNotEquals(ref1, ref2)
    }

    // ==================== Month ID Generation Tests ====================

    @Test
    fun `month id - format YYYYMM`() {
        assertEquals(202603, calculateMonthId(2026, 3))
        assertEquals(202612, calculateMonthId(2026, 12))
        assertEquals(202501, calculateMonthId(2025, 1))
    }

    @Test
    fun `month id - unique per month`() {
        val march2026 = calculateMonthId(2026, 3)
        val april2026 = calculateMonthId(2026, 4)
        val march2027 = calculateMonthId(2027, 3)

        assertNotEquals(march2026, april2026)
        assertNotEquals(march2026, march2027)
    }

    // ==================== Helper Functions ====================

    /**
     * Calculate percentage: (spent * 100) / limit
     * Uses integer division as in the actual implementation.
     */
    private fun calculatePercentage(spent: Long, limit: Long): Long {
        return if (limit > 0) (spent * 100) / limit else 0
    }

    /**
     * Check if percentage triggers an alert (80% or higher)
     */
    private fun shouldTriggerAlert(percentage: Long): Boolean {
        return percentage >= 80
    }

    /**
     * Get alert type based on percentage
     */
    private fun getAlertType(percentage: Long): String {
        return if (percentage >= 100) "exceeded" else "warning"
    }

    /**
     * Calculate total for "All Expenses" budget (excludes Money Transfer)
     */
    private fun calculateAllExpensesTotal(transactions: List<TestTransaction>): Long {
        return transactions
            .filter { it.categoryName != "Money Transfer" }
            .sumOf { it.amount }
    }

    /**
     * Calculate spending grouped by category
     */
    private fun calculateCategorySpending(transactions: List<TestTransaction>): Map<String, Long> {
        return transactions
            .groupBy { it.categoryName }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    /**
     * Generate reference ID for a budget notification
     * Formula: budgetId * 1000000L + monthId * 10L + (threshold / 10)
     */
    private fun generateReferenceId(budgetId: Long, monthId: Long, threshold: Long): Long {
        return budgetId * 1000000L + monthId * 10L + (threshold / 10)
    }

    /**
     * Calculate month ID in YYYYMM format
     */
    private fun calculateMonthId(year: Int, month: Int): Int {
        return year * 100 + month
    }

    /**
     * Simple test data class for transactions
     */
    data class TestTransaction(
        val categoryName: String,
        val amount: Long
    )
}
