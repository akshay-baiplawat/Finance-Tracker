package com.example.financetracker.core.parser

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for MerchantNormalizer - merchant name normalization.
 */
class MerchantNormalizerTest {

    @Test
    fun `normalize Amazon variations`() {
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZON"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZON INDIA"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZONPAY"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMZN"))
        assertEquals("Amazon", MerchantNormalizer.normalize("amazon.in"))
    }

    @Test
    fun `normalize Zomato variations`() {
        assertEquals("Zomato", MerchantNormalizer.normalize("ZOMATO"))
        assertEquals("Zomato", MerchantNormalizer.normalize("ZOMATO.COM"))
        assertEquals("Zomato", MerchantNormalizer.normalize("BLINKIT"))
        assertEquals("Zomato", MerchantNormalizer.normalize("ZOMATO BLINKIT"))
    }

    @Test
    fun `normalize Swiggy variations`() {
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY"))
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY INSTAMART"))
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY GENIE"))
    }

    @Test
    fun `normalize ride services`() {
        assertEquals("Uber", MerchantNormalizer.normalize("UBER"))
        assertEquals("Uber", MerchantNormalizer.normalize("UBER INDIA"))
        assertEquals("Uber", MerchantNormalizer.normalize("UBER BV"))
        assertEquals("Ola", MerchantNormalizer.normalize("OLA CABS"))
        assertEquals("Rapido", MerchantNormalizer.normalize("RAPIDO"))
    }

    @Test
    fun `normalize streaming services`() {
        assertEquals("Netflix", MerchantNormalizer.normalize("NETFLIX"))
        assertEquals("Netflix", MerchantNormalizer.normalize("NETFLIX.COM"))
        assertEquals("Spotify", MerchantNormalizer.normalize("SPOTIFY AB"))
        assertEquals("Hotstar", MerchantNormalizer.normalize("DISNEY+ HOTSTAR"))
    }

    @Test
    fun `normalize telecom providers`() {
        assertEquals("Jio", MerchantNormalizer.normalize("RELIANCE JIO"))
        assertEquals("Jio", MerchantNormalizer.normalize("JIO PREPAID"))
        assertEquals("Airtel", MerchantNormalizer.normalize("BHARTI AIRTEL"))
        assertEquals("Vodafone", MerchantNormalizer.normalize("VODAFONE IDEA"))
    }

    @Test
    fun `normalize retail stores`() {
        assertEquals("H&M", MerchantNormalizer.normalize("H&M"))
        assertEquals("H&M", MerchantNormalizer.normalize("H AND M"))
        assertEquals("7-Eleven", MerchantNormalizer.normalize("7-ELEVEN"))
        assertEquals("7-Eleven", MerchantNormalizer.normalize("SEVEN ELEVEN"))
        assertEquals("DMart", MerchantNormalizer.normalize("D MART"))
    }

    @Test
    fun `preserve unknown merchant names`() {
        assertEquals("RANDOM SHOP", MerchantNormalizer.normalize("RANDOM SHOP"))
        assertEquals("Local Store", MerchantNormalizer.normalize("Local Store"))
        assertEquals("XYZ CORP", MerchantNormalizer.normalize("XYZ CORP"))
    }

    @Test
    fun `handle case insensitivity`() {
        assertEquals("Amazon", MerchantNormalizer.normalize("amazon"))
        assertEquals("Amazon", MerchantNormalizer.normalize("Amazon"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZON"))
    }

    @Test
    fun `isKnownMerchant returns true for known merchants`() {
        assertTrue(MerchantNormalizer.isKnownMerchant("AMAZON"))
        assertTrue(MerchantNormalizer.isKnownMerchant("ZOMATO"))
        assertTrue(MerchantNormalizer.isKnownMerchant("NETFLIX"))
    }

    @Test
    fun `isKnownMerchant returns false for unknown merchants`() {
        assertFalse(MerchantNormalizer.isKnownMerchant("RANDOM XYZ"))
        assertFalse(MerchantNormalizer.isKnownMerchant("LOCAL SHOP"))
    }

    @Test
    fun `getAllKnownMerchants returns sorted list`() {
        val merchants = MerchantNormalizer.getAllKnownMerchants()
        assertTrue(merchants.isNotEmpty())
        assertTrue(merchants.contains("Amazon"))
        assertTrue(merchants.contains("Zomato"))
        // Check sorted order
        assertEquals(merchants, merchants.sorted())
    }

    @Test
    fun `normalize handles partial matches`() {
        // "AMAZON SELLER SERVICES" should still normalize to "Amazon"
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZON SELLER SERVICES"))
        assertEquals("Zomato", MerchantNormalizer.normalize("ZOMATO MEDIA PVT LTD"))
    }
}
