package com.example.financetracker.core.parser

import java.util.Locale

/**
 * Normalizes merchant names to canonical forms for better categorization and reporting.
 * Maps common variations and abbreviations to standardized merchant names.
 *
 * Example: "AMAZON INDIA", "AMAZONPAY", "AMZN" all normalize to "Amazon"
 */
object MerchantNormalizer {

    /**
     * Mapping of normalized merchant name to list of variations/aliases.
     * Variations are checked case-insensitively.
     */
    private val merchantAliases: Map<String, List<String>> = mapOf(
        // E-commerce
        "Amazon" to listOf("AMAZON", "AMAZON INDIA", "AMAZONPAY", "AMZN", "AWS", "AMAZON.IN", "AMAZON.COM"),
        "Flipkart" to listOf("FLIPKART", "FLIPKART.COM", "FKRT", "FLIPKART WHOLESALE"),
        "Myntra" to listOf("MYNTRA", "MYNTRA.COM"),
        "Ajio" to listOf("AJIO", "AJIO.COM"),
        "Nykaa" to listOf("NYKAA", "NYKAA.COM", "NYKAA FASHION"),
        "Meesho" to listOf("MEESHO", "MEESHO.COM"),
        "Shopsy" to listOf("SHOPSY"),

        // Food Delivery
        "Zomato" to listOf("ZOMATO", "ZOMATO.COM", "ZOMATO MEDIA", "BLINKIT", "ZOMATO BLINKIT"),
        "Swiggy" to listOf("SWIGGY", "SWIGGY.COM", "SWIGGY INSTAMART", "SWIGGY GENIE"),
        "Domino's" to listOf("DOMINOS", "DOMINO'S", "DOMINOS PIZZA"),
        "Pizza Hut" to listOf("PIZZA HUT", "PIZZAHUT"),
        "McDonald's" to listOf("MCDONALDS", "MCDONALD'S", "MCD"),
        "KFC" to listOf("KFC", "KENTUCKY FRIED"),
        "Starbucks" to listOf("STARBUCKS", "STARBUCKS COFFEE"),
        "Burger King" to listOf("BURGER KING", "BK"),
        "Subway" to listOf("SUBWAY"),

        // Quick Commerce
        "BigBasket" to listOf("BIGBASKET", "BIG BASKET", "BB DAILY"),
        "Zepto" to listOf("ZEPTO"),
        "Dunzo" to listOf("DUNZO"),

        // Ride Services
        "Uber" to listOf("UBER", "UBER INDIA", "UBER BV", "UBER TRIP", "UBER EATS"),
        "Ola" to listOf("OLA", "OLA CABS", "OLA ELECTRIC"),
        "Rapido" to listOf("RAPIDO"),

        // Streaming
        "Netflix" to listOf("NETFLIX", "NETFLIX.COM", "NETFLIX INC"),
        "Spotify" to listOf("SPOTIFY", "SPOTIFY AB", "SPOTIFY.COM"),
        "Hotstar" to listOf("HOTSTAR", "DISNEY+ HOTSTAR", "DISNEY HOTSTAR"),
        "Amazon Prime" to listOf("PRIME VIDEO", "AMAZON PRIME"),
        "YouTube" to listOf("YOUTUBE", "YOUTUBE PREMIUM", "GOOGLE YOUTUBE"),
        "JioCinema" to listOf("JIOCINEMA", "JIO CINEMA"),
        "Zee5" to listOf("ZEE5"),
        "SonyLIV" to listOf("SONYLIV", "SONY LIV"),

        // Telecom
        "Jio" to listOf("JIO", "RELIANCE JIO", "JIO PREPAID", "JIO POSTPAID", "JIOMART"),
        "Airtel" to listOf("AIRTEL", "BHARTI AIRTEL", "AIRTEL PREPAID", "AIRTEL POSTPAID", "AIRTEL XSTREAM"),
        "Vodafone" to listOf("VODAFONE", "VODAFONE IDEA", "VI"),
        "BSNL" to listOf("BSNL"),

        // Utilities
        "Electricity" to listOf("ELECTRICITY", "BESCOM", "MSEDCL", "TATAPOWER", "ADANI ELECTRICITY"),
        "Gas" to listOf("GAS", "INDANE", "HP GAS", "BHARAT GAS", "MAHANAGAR GAS", "IGL"),
        "Water" to listOf("WATER", "BWSSB"),

        // Fuel
        "Indian Oil" to listOf("INDIAN OIL", "IOCL", "INDIANOIL"),
        "HP Petrol" to listOf("HP PETROL", "HPCL", "HINDUSTAN PETROLEUM"),
        "Bharat Petroleum" to listOf("BHARAT PETROLEUM", "BPCL", "BP"),
        "Shell" to listOf("SHELL", "SHELL PETROL"),

        // Pharmacy
        "Apollo Pharmacy" to listOf("APOLLO", "APOLLO PHARMACY", "APOLLO 247"),
        "MedPlus" to listOf("MEDPLUS", "MED PLUS"),
        "Pharmeasy" to listOf("PHARMEASY", "PHARM EASY"),
        "1mg" to listOf("1MG", "ONE MG", "ONEMG"),
        "Netmeds" to listOf("NETMEDS"),

        // Payment Apps
        "Paytm" to listOf("PAYTM", "PAYTM PAYMENTS"),
        "PhonePe" to listOf("PHONEPE", "PHONE PE"),
        "Google Pay" to listOf("GPAY", "GOOGLE PAY", "GOOGLEPAY"),
        "CRED" to listOf("CRED"),

        // Travel
        "MakeMyTrip" to listOf("MAKEMYTRIP", "MMT"),
        "Goibibo" to listOf("GOIBIBO"),
        "Cleartrip" to listOf("CLEARTRIP"),
        "IRCTC" to listOf("IRCTC", "INDIAN RAILWAYS"),
        "RedBus" to listOf("REDBUS", "RED BUS"),
        "Ixigo" to listOf("IXIGO"),

        // Insurance
        "LIC" to listOf("LIC", "LIFE INSURANCE CORP"),
        "HDFC Life" to listOf("HDFC LIFE"),
        "ICICI Prudential" to listOf("ICICI PRU", "ICICI PRUDENTIAL"),
        "Max Life" to listOf("MAX LIFE"),
        "SBI Life" to listOf("SBI LIFE"),

        // Retail
        "DMart" to listOf("DMART", "D MART", "AVENUE SUPERMARTS"),
        "Big Bazaar" to listOf("BIG BAZAAR", "BIGBAZAAR"),
        "Reliance" to listOf("RELIANCE RETAIL", "RELIANCE FRESH", "RELIANCE DIGITAL", "RELIANCE TRENDS"),
        "H&M" to listOf("H&M", "HM", "H AND M"),
        "Zara" to listOf("ZARA"),
        "Decathlon" to listOf("DECATHLON"),
        "Croma" to listOf("CROMA"),
        "Lifestyle" to listOf("LIFESTYLE"),
        "Westside" to listOf("WESTSIDE"),

        // Convenience Stores
        "7-Eleven" to listOf("7-ELEVEN", "7 ELEVEN", "SEVEN ELEVEN"),

        // International
        "Apple" to listOf("APPLE", "APPLE.COM", "APPLE INC", "ITUNES"),
        "Google" to listOf("GOOGLE", "GOOGLE PLAY", "GOOGLE ONE", "GOOGLE CLOUD"),
        "Microsoft" to listOf("MICROSOFT", "MSFT", "MICROSOFT 365"),
        "Adobe" to listOf("ADOBE", "ADOBE SYSTEMS"),
        "Walmart" to listOf("WALMART"),
        "Costco" to listOf("COSTCO")
    )

    // Build reverse lookup map for efficiency (variation -> normalized name)
    private val reverseLookup: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        merchantAliases.forEach { (normalized, aliases) ->
            aliases.forEach { alias ->
                map[alias.uppercase(Locale.ROOT)] = normalized
            }
        }
        map
    }

    /**
     * Normalizes a merchant name to its canonical form.
     *
     * @param rawMerchant The raw merchant name from SMS
     * @return The normalized merchant name, or the original if no match found
     */
    fun normalize(rawMerchant: String): String {
        val upper = rawMerchant.uppercase(Locale.ROOT).trim()

        // Direct match
        reverseLookup[upper]?.let { return it }

        // Partial match (check if any alias is contained in the merchant name)
        for ((normalized, aliases) in merchantAliases) {
            for (alias in aliases) {
                if (upper.contains(alias)) {
                    return normalized
                }
            }
        }

        // No match - return cleaned original
        return rawMerchant.trim()
    }

    /**
     * Checks if a merchant name has a known normalization.
     */
    fun isKnownMerchant(rawMerchant: String): Boolean {
        val upper = rawMerchant.uppercase(Locale.ROOT).trim()

        if (reverseLookup.containsKey(upper)) return true

        return merchantAliases.any { (_, aliases) ->
            aliases.any { alias -> upper.contains(alias) }
        }
    }

    /**
     * Gets all known merchant names (for UI autocomplete, etc.)
     */
    fun getAllKnownMerchants(): List<String> = merchantAliases.keys.toList().sorted()
}
