package com.example.financetracker.core.parser

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for CategoryInferrer - automatic category detection from merchant names.
 */
class CategoryInferrerTest {

    // Food & Dining tests
    @Test
    fun `infer Food & Dining for Zomato`() {
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("ZOMATO", isIncome = false))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("ZOMATO MEDIA PVT LTD", isIncome = false))
    }

    @Test
    fun `infer Food & Dining for Swiggy`() {
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("SWIGGY", isIncome = false))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("SWIGGY INSTAMART", isIncome = false))
    }

    @Test
    fun `infer Food & Dining for restaurants`() {
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("DOMINOS PIZZA", isIncome = false))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("STARBUCKS COFFEE", isIncome = false))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("MCDONALD'S", isIncome = false))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("CAFE COFFEE DAY", isIncome = false))
    }

    // Shopping tests
    @Test
    fun `infer Shopping for Amazon`() {
        assertEquals("Shopping", CategoryInferrer.inferCategory("AMAZON", isIncome = false))
        assertEquals("Shopping", CategoryInferrer.inferCategory("AMAZON INDIA", isIncome = false))
    }

    @Test
    fun `infer Shopping for Flipkart`() {
        assertEquals("Shopping", CategoryInferrer.inferCategory("FLIPKART", isIncome = false))
        assertEquals("Shopping", CategoryInferrer.inferCategory("FLIPKART.COM", isIncome = false))
    }

    @Test
    fun `infer Shopping for fashion retailers`() {
        assertEquals("Shopping", CategoryInferrer.inferCategory("MYNTRA", isIncome = false))
        assertEquals("Shopping", CategoryInferrer.inferCategory("H&M", isIncome = false))
        assertEquals("Shopping", CategoryInferrer.inferCategory("ZARA", isIncome = false))
    }

    // Transportation tests
    @Test
    fun `infer Transportation for Uber`() {
        assertEquals("Transportation", CategoryInferrer.inferCategory("UBER", isIncome = false))
        assertEquals("Transportation", CategoryInferrer.inferCategory("UBER INDIA", isIncome = false))
    }

    @Test
    fun `infer Transportation for Ola`() {
        assertEquals("Transportation", CategoryInferrer.inferCategory("OLA CABS", isIncome = false))
    }

    @Test
    fun `infer Transportation for fuel`() {
        assertEquals("Transportation", CategoryInferrer.inferCategory("INDIAN OIL", isIncome = false))
        assertEquals("Transportation", CategoryInferrer.inferCategory("SHELL PETROL", isIncome = false))
        assertEquals("Transportation", CategoryInferrer.inferCategory("HP PETROL PUMP", isIncome = false))
    }

    // Entertainment tests
    @Test
    fun `infer Entertainment for Netflix`() {
        assertEquals("Entertainment", CategoryInferrer.inferCategory("NETFLIX", isIncome = false))
        assertEquals("Entertainment", CategoryInferrer.inferCategory("NETFLIX.COM", isIncome = false))
    }

    @Test
    fun `infer Entertainment for Spotify`() {
        assertEquals("Entertainment", CategoryInferrer.inferCategory("SPOTIFY", isIncome = false))
        assertEquals("Entertainment", CategoryInferrer.inferCategory("SPOTIFY AB", isIncome = false))
    }

    @Test
    fun `infer Entertainment for gaming`() {
        assertEquals("Entertainment", CategoryInferrer.inferCategory("STEAM GAMES", isIncome = false))
        assertEquals("Entertainment", CategoryInferrer.inferCategory("PLAYSTATION", isIncome = false))
    }

    // Bills & Utilities tests
    @Test
    fun `infer Bills & Utilities for telecom`() {
        assertEquals("Bills & Utilities", CategoryInferrer.inferCategory("JIO PREPAID", isIncome = false))
        assertEquals("Bills & Utilities", CategoryInferrer.inferCategory("AIRTEL POSTPAID", isIncome = false))
        assertEquals("Bills & Utilities", CategoryInferrer.inferCategory("VODAFONE", isIncome = false))
    }

    @Test
    fun `infer Bills & Utilities for electricity`() {
        assertEquals("Bills & Utilities", CategoryInferrer.inferCategory("BESCOM ELECTRICITY", isIncome = false))
        assertEquals("Bills & Utilities", CategoryInferrer.inferCategory("TATAPOWER", isIncome = false))
    }

    // Health & Wellness tests
    @Test
    fun `infer Health & Wellness for pharmacy`() {
        assertEquals("Health & Wellness", CategoryInferrer.inferCategory("APOLLO PHARMACY", isIncome = false))
        assertEquals("Health & Wellness", CategoryInferrer.inferCategory("1MG", isIncome = false))
        assertEquals("Health & Wellness", CategoryInferrer.inferCategory("PHARMEASY", isIncome = false))
    }

    @Test
    fun `infer Health & Wellness for hospitals`() {
        assertEquals("Health & Wellness", CategoryInferrer.inferCategory("MANIPAL HOSPITAL", isIncome = false))
        assertEquals("Health & Wellness", CategoryInferrer.inferCategory("FORTIS CLINIC", isIncome = false))
    }

    // Trip tests
    @Test
    fun `infer Trip for travel bookings`() {
        assertEquals("Trip", CategoryInferrer.inferCategory("MAKEMYTRIP", isIncome = false))
        assertEquals("Trip", CategoryInferrer.inferCategory("GOIBIBO", isIncome = false))
        assertEquals("Trip", CategoryInferrer.inferCategory("CLEARTRIP", isIncome = false))
    }

    @Test
    fun `infer Trip for airlines`() {
        assertEquals("Trip", CategoryInferrer.inferCategory("INDIGO AIRLINES", isIncome = false))
        assertEquals("Trip", CategoryInferrer.inferCategory("AIR INDIA", isIncome = false))
    }

    // Money Transfer tests
    @Test
    fun `infer Money Transfer for UPI payments`() {
        assertEquals("Money Transfer", CategoryInferrer.inferCategory("UPI TRANSFER", isIncome = false))
        assertEquals("Money Transfer", CategoryInferrer.inferCategory("NEFT TRANSFER", isIncome = false))
    }

    // Income tests
    @Test
    fun `always infer Money Received for income transactions`() {
        assertEquals("Money Received", CategoryInferrer.inferCategory("AMAZON", isIncome = true))
        assertEquals("Money Received", CategoryInferrer.inferCategory("SALARY", isIncome = true))
        assertEquals("Money Received", CategoryInferrer.inferCategory("RANDOM MERCHANT", isIncome = true))
    }

    // Default/Unknown tests
    @Test
    fun `default to Uncategorized for unknown merchants`() {
        assertEquals("Uncategorized", CategoryInferrer.inferCategory("RANDOM XYZ SHOP", isIncome = false))
        assertEquals("Uncategorized", CategoryInferrer.inferCategory("LOCAL VENDOR", isIncome = false))
    }

    @Test
    fun `respect Trip mode for unknown merchants`() {
        assertEquals("Trip", CategoryInferrer.inferCategory("RANDOM XYZ SHOP", isIncome = false, isTripMode = true))
        assertEquals("Trip", CategoryInferrer.inferCategory("LOCAL VENDOR", isIncome = false, isTripMode = true))
    }

    @Test
    fun `Trip mode does not override known categories`() {
        // Known merchants should still get their inferred category even in Trip mode
        assertEquals("Shopping", CategoryInferrer.inferCategory("AMAZON", isIncome = false, isTripMode = true))
        assertEquals("Food & Dining", CategoryInferrer.inferCategory("ZOMATO", isIncome = false, isTripMode = true))
    }

    // Confidence tests
    @Test
    fun `high confidence for matched patterns`() {
        assertEquals(1.0f, CategoryInferrer.getInferenceConfidence("AMAZON", isIncome = false))
        assertEquals(1.0f, CategoryInferrer.getInferenceConfidence("ZOMATO", isIncome = false))
        assertEquals(1.0f, CategoryInferrer.getInferenceConfidence("SALARY", isIncome = true))
    }

    @Test
    fun `low confidence for unmatched patterns`() {
        assertEquals(0.5f, CategoryInferrer.getInferenceConfidence("RANDOM XYZ", isIncome = false))
        assertEquals(0.5f, CategoryInferrer.getInferenceConfidence("LOCAL SHOP", isIncome = false))
    }

    @Test
    fun `getInferrableCategories returns expected categories`() {
        val categories = CategoryInferrer.getInferrableCategories()
        assertTrue(categories.contains("Food & Dining"))
        assertTrue(categories.contains("Shopping"))
        assertTrue(categories.contains("Transportation"))
        assertTrue(categories.contains("Entertainment"))
        assertTrue(categories.contains("Health & Wellness"))
    }
}
