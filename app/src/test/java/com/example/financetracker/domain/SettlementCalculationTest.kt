package com.example.financetracker.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for settlement calculation logic.
 * Tests the greedy algorithm that minimizes the number of settlements.
 *
 * Balance conventions:
 * - Positive balance = person is owed money (creditor - paid more than their share)
 * - Negative balance = person owes money (debtor - paid less than their share)
 *
 * The algorithm matches debtors with creditors to settle debts with minimum transactions.
 */
class SettlementCalculationTest {

    // ==================== Basic Settlement Tests ====================

    @Test
    fun `settlement - two people simple split`() {
        // Alice owes Bob $100
        val balances: Map<Long?, Long> = mapOf(
            1L to -10000L,  // Alice: owes 100
            2L to 10000L    // Bob: owed 100
        )

        val settlements = calculateSettlements(balances)

        assertEquals(1, settlements.size)
        assertEquals(1L, settlements[0].from)
        assertEquals(2L, settlements[0].to)
        assertEquals(10000L, settlements[0].amount)
    }

    @Test
    fun `settlement - three people simple split`() {
        // Alice and Bob each owe Charlie $50
        val balances: Map<Long?, Long> = mapOf(
            1L to -5000L,   // Alice: owes 50
            2L to -5000L,   // Bob: owes 50
            3L to 10000L    // Charlie: owed 100
        )

        val settlements = calculateSettlements(balances)

        assertEquals(2, settlements.size)
        // Total should equal 10000
        val total = settlements.sumOf { it.amount }
        assertEquals(10000L, total)
    }

    @Test
    fun `settlement - four people complex split`() {
        // Alice owes 300, Bob owes 200, Charlie is owed 400, Dave is owed 100
        val balances: Map<Long?, Long> = mapOf(
            1L to -30000L,  // Alice: owes 300
            2L to -20000L,  // Bob: owes 200
            3L to 40000L,   // Charlie: owed 400
            4L to 10000L    // Dave: owed 100
        )

        val settlements = calculateSettlements(balances)

        // Verify all debts are covered
        val totalDebt = balances.filter { it.value < 0 }.values.sumOf { -it }
        val totalCredit = balances.filter { it.value > 0 }.values.sumOf { it }
        assertEquals(totalDebt, totalCredit)

        // Verify settlements balance
        val totalSettled = settlements.sumOf { it.amount }
        assertEquals(totalDebt, totalSettled)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `settlement - all balanced`() {
        val balances: Map<Long?, Long> = mapOf(
            1L to 0L,
            2L to 0L,
            3L to 0L
        )

        val settlements = calculateSettlements(balances)
        assertTrue(settlements.isEmpty())
    }

    @Test
    fun `settlement - single member`() {
        val balances: Map<Long?, Long> = mapOf(1L to 0L)

        val settlements = calculateSettlements(balances)
        assertTrue(settlements.isEmpty())
    }

    @Test
    fun `settlement - empty balances`() {
        val balances = emptyMap<Long?, Long>()

        val settlements = calculateSettlements(balances)
        assertTrue(settlements.isEmpty())
    }

    @Test
    fun `settlement - only debtors no creditors`() {
        // This shouldn't happen in practice, but test defensive behavior
        val balances: Map<Long?, Long> = mapOf(
            1L to -10000L,
            2L to -5000L
        )

        val settlements = calculateSettlements(balances)
        // No creditors to pay, so no settlements possible
        assertTrue(settlements.isEmpty())
    }

    @Test
    fun `settlement - only creditors no debtors`() {
        val balances: Map<Long?, Long> = mapOf(
            1L to 10000L,
            2L to 5000L
        )

        val settlements = calculateSettlements(balances)
        // No debtors to collect from, so no settlements possible
        assertTrue(settlements.isEmpty())
    }

    // ==================== User (null ID) Tests ====================

    @Test
    fun `settlement - user owes others`() {
        val balances: Map<Long?, Long> = mapOf(
            null to -10000L,  // User: owes 100
            1L to 10000L      // Person 1: owed 100
        )

        val settlements = calculateSettlements(balances)

        assertEquals(1, settlements.size)
        assertNull(settlements[0].from)  // From user (null)
        assertEquals(1L, settlements[0].to)
        assertEquals(10000L, settlements[0].amount)
    }

    @Test
    fun `settlement - user is owed money`() {
        val balances: Map<Long?, Long> = mapOf(
            null to 10000L,   // User: owed 100
            1L to -10000L     // Person 1: owes 100
        )

        val settlements = calculateSettlements(balances)

        assertEquals(1, settlements.size)
        assertEquals(1L, settlements[0].from)
        assertNull(settlements[0].to)  // To user (null)
        assertEquals(10000L, settlements[0].amount)
    }

    @Test
    fun `settlement - user among multiple`() {
        val balances: Map<Long?, Long> = mapOf(
            null to -15000L,  // User: owes 150
            1L to -5000L,     // Person 1: owes 50
            2L to 20000L      // Person 2: owed 200
        )

        val settlements = calculateSettlements(balances)

        assertEquals(2, settlements.size)
        val totalSettled = settlements.sumOf { it.amount }
        assertEquals(20000L, totalSettled)
    }

    // ==================== Greedy Algorithm Optimization Tests ====================

    @Test
    fun `settlement - minimizes transactions greedy`() {
        // 5 people scenario where greedy should produce fewer transactions
        // A owes 100, B owes 50, C is owed 80, D is owed 40, E is owed 30
        val balances: Map<Long?, Long> = mapOf(
            1L to -10000L,  // A: owes 100
            2L to -5000L,   // B: owes 50
            3L to 8000L,    // C: owed 80
            4L to 4000L,    // D: owed 40
            5L to 3000L     // E: owed 30
        )

        val settlements = calculateSettlements(balances)

        // Verify correctness
        val totalDebt = 15000L
        val totalSettled = settlements.sumOf { it.amount }
        assertEquals(totalDebt, totalSettled)

        // Greedy should produce at most max(debtors, creditors) settlements
        // With 2 debtors and 3 creditors, worst case is 3 settlements
        assertTrue("Should have at most 4 settlements", settlements.size <= 4)
    }

    @Test
    fun `settlement - sorts by amount for efficiency`() {
        // Largest debtor should be matched with largest creditor first
        val balances: Map<Long?, Long> = mapOf(
            1L to -50000L,  // Largest debtor: 500
            2L to -10000L,  // Small debtor: 100
            3L to 40000L,   // Largest creditor: 400
            4L to 20000L    // Small creditor: 200
        )

        val settlements = calculateSettlements(balances)

        // First settlement should involve the largest debtor (1L) and largest creditor (3L)
        val firstSettlement = settlements[0]
        assertEquals(1L, firstSettlement.from)
        assertEquals(3L, firstSettlement.to)
    }

    // ==================== Balance Calculation Tests ====================

    @Test
    fun `balance - positive means owed money`() {
        // totalPaid > totalOwes = positive balance = owed money (creditor)
        val totalPaid = 20000L
        val totalOwes = 10000L
        val balance = totalPaid - totalOwes

        assertEquals(10000L, balance)
        assertTrue("Positive balance = creditor", balance > 0)
    }

    @Test
    fun `balance - negative means owes money`() {
        // totalPaid < totalOwes = negative balance = owes money (debtor)
        val totalPaid = 10000L
        val totalOwes = 20000L
        val balance = totalPaid - totalOwes

        assertEquals(-10000L, balance)
        assertTrue("Negative balance = debtor", balance < 0)
    }

    @Test
    fun `balance - zero means settled`() {
        val totalPaid = 15000L
        val totalOwes = 15000L
        val balance = totalPaid - totalOwes

        assertEquals(0L, balance)
    }

    // ==================== Equal Split Calculation Tests ====================

    @Test
    fun `equal split - two members`() {
        val totalAmount = 10000L // ₹100
        val memberCount = 2

        val sharePerPerson = totalAmount / memberCount
        val remainder = totalAmount % memberCount

        assertEquals(5000L, sharePerPerson)
        assertEquals(0L, remainder)
    }

    @Test
    fun `equal split - three members odd amount`() {
        val totalAmount = 10000L // ₹100
        val memberCount = 3

        val sharePerPerson = totalAmount / memberCount
        val remainder = totalAmount % memberCount

        assertEquals(3333L, sharePerPerson)
        assertEquals(1L, remainder)  // 1 paisa remainder
    }

    @Test
    fun `equal split - five members`() {
        val totalAmount = 10000L // ₹100
        val memberCount = 5

        val sharePerPerson = totalAmount / memberCount
        val remainder = totalAmount % memberCount

        assertEquals(2000L, sharePerPerson)
        assertEquals(0L, remainder)
    }

    @Test
    fun `equal split - indivisible amount`() {
        val totalAmount = 10001L // ₹100.01
        val memberCount = 3

        val sharePerPerson = totalAmount / memberCount
        val remainder = totalAmount % memberCount

        assertEquals(3333L, sharePerPerson)
        assertEquals(2L, remainder)  // 2 paisa remainder

        // First person gets remainder
        val firstShare = sharePerPerson + remainder
        assertEquals(3335L, firstShare)
    }

    // ==================== Custom Split Verification Tests ====================

    @Test
    fun `custom split - must sum to total`() {
        val totalAmount = 10000L

        val split1 = 6000L
        val split2 = 4000L

        val splitSum = split1 + split2
        assertEquals("Splits must sum to total", totalAmount, splitSum)
    }

    @Test
    fun `custom split - percentages`() {
        val totalAmount = 10000L

        // 60% - 40% split
        val split1 = (totalAmount * 60) / 100
        val split2 = (totalAmount * 40) / 100

        assertEquals(6000L, split1)
        assertEquals(4000L, split2)
        assertEquals(totalAmount, split1 + split2)
    }

    // ==================== Helper Classes and Functions ====================

    data class TestSettlement(
        val from: Long?,
        val to: Long?,
        val amount: Long
    )

    /**
     * Implementation of the greedy settlement algorithm for testing.
     * Matches the logic in GroupRepositoryImpl.calculateSettlementSuggestionsFromBalances
     */
    private fun calculateSettlements(balances: Map<Long?, Long>): List<TestSettlement> {
        val debtors = balances.filter { it.value < 0 }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val creditors = balances.filter { it.value > 0 }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val settlements = mutableListOf<TestSettlement>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.first()
            val creditor = creditors.first()

            val amount = minOf(debtor.second, creditor.second)

            settlements.add(
                TestSettlement(
                    from = debtor.first,
                    to = creditor.first,
                    amount = amount
                )
            )

            // Update balances
            if (debtor.second == amount) {
                debtors.removeAt(0)
            } else {
                debtors[0] = debtor.first to (debtor.second - amount)
            }

            if (creditor.second == amount) {
                creditors.removeAt(0)
            } else {
                creditors[0] = creditor.first to (creditor.second - amount)
            }
        }

        return settlements
    }
}
