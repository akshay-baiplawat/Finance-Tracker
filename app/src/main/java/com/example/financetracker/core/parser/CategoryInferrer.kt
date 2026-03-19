package com.example.financetracker.core.parser

import java.util.Locale

/**
 * Infers transaction category from merchant name using pattern matching.
 * Categories align with the app's predefined category list.
 *
 * Note: This is a rule-based approach. User overrides are handled separately
 * via MerchantCategoryPreferenceEntity.
 */
object CategoryInferrer {

    /**
     * Category pattern definitions.
     * Patterns are checked in order - first match wins.
     * Use regex for flexible matching.
     */
    private val categoryPatterns: List<Pair<Regex, String>> = listOf(
        // Food & Dining (highest priority for food-related merchants)
        Regex("(?i)zomato|swiggy|dominos|domino's|mcdonald|mcdonalds|starbucks|cafe|coffee|restaurant|pizza|burger|kfc|subway|hotel|food|dine|dining|eat|meal|biryani|kebab|chicken|noodles|chinese|italian|thai|mughlai|north indian|south indian|bakery|sweet|ice cream|dessert|juice|bar|pub|dhaba|canteen|tiffin|mess|zepto|blinkit|bigbasket|grofers|dunzo|instamart")
            to "Food & Dining",

        // Shopping (e-commerce and retail)
        Regex("(?i)amazon|flipkart|myntra|ajio|nykaa|meesho|shopsy|snapdeal|paytm mall|tata cliq|reliance retail|reliance digital|reliance trends|croma|dmart|big bazaar|lifestyle|westside|h&m|zara|decathlon|shoppers stop|pantaloons|max fashion|central|brand factory|lenskart|firstcry|purplle")
            to "Shopping",

        // Transportation (rides, fuel, metro)
        Regex("(?i)uber|ola|rapido|metro|irctc|railways|petrol|diesel|fuel|indian oil|iocl|hpcl|bpcl|shell|esso|parking|toll|fastag|parivahan|car|bike|auto|cab|taxi|ride|drive|commute|meru|namma yatri")
            to "Transportation",

        // Bills & Utilities
        Regex("(?i)electricity|bescom|msedcl|tatapower|adani electricity|gas|indane|hp gas|bharat gas|mahanagar gas|igl|water|bwssb|broadband|internet|jio|airtel|vodafone|vi|bsnl|act fibernet|hathway|tata sky|dish tv|d2h|sun direct|den|asianet|cable|dth|postpaid|prepaid|recharge|utility|bill|emi|loan|insurance premium|rent|maintenance|society|association")
            to "Bills & Utilities",

        // Entertainment
        Regex("(?i)netflix|spotify|hotstar|disney|prime video|youtube|jiocinema|zee5|sonyliv|voot|mubi|mxplayer|aha|hoichoi|alt balaji|eros now|hungama|gaana|wynk|jiosaavn|apple music|audible|kindle|book|movie|cinema|pvr|inox|multiplex|theatre|concert|show|event|game|gaming|playstation|xbox|steam|epic games|pubg|bgmi|free fire|ludo|rummy|poker|casino|bet")
            to "Entertainment",

        // Health & Wellness
        Regex("(?i)pharmacy|apollo|medplus|pharmeasy|1mg|onemg|netmeds|hospital|clinic|doctor|dentist|dental|lab|diagnostic|pathology|thyrocare|lal path|dr lal|healthians|health|wellness|gym|fitness|cult|cure\\.fit|yoga|spa|salon|parlour|grooming|haircut|massage|therapy|physio|medical|medicine|drug|healthcare")
            to "Health & Wellness",

        // Trip (travel and vacation)
        Regex("(?i)makemytrip|mmt|goibibo|cleartrip|yatra|ixigo|redbus|abhibus|easemytrip|via\\.com|booking\\.com|oyo|treebo|fabhotels|airbnb|hostelworld|agoda|trivago|expedia|kayak|skyscanner|flight|airline|indigo|spicejet|air india|vistara|goair|akasa|airport|train|bus|travel|trip|tour|vacation|holiday|resort|cruise|pilgrimage|visa|passport")
            to "Trip",

        // Money Transfer (internal transfers, P2P)
        Regex("(?i)transfer|sent to|received from|neft|rtgs|imps|upi|gpay|phonepe|paytm|bhim|mobikwik|freecharge|paypal|wise|remittance|self transfer|internal|own account|linked account")
            to "Money Transfer",

        // Salary/Income (credits)
        Regex("(?i)salary|payroll|stipend|wage|bonus|incentive|commission|reimbursement|refund|cashback|reward|dividend|interest|maturity|proceeds|settlement|credit[^\\s]*card|insurance claim")
            to "Money Received",

        // Insurance (specific insurance companies)
        Regex("(?i)lic|life insurance|icici prudential|hdfc life|max life|sbi life|bajaj allianz|star health|new india|national insurance|oriental insurance|united india|tata aig|digit|acko|policy ?bazaar|coverfox|term insurance|health insurance|motor insurance|vehicle insurance")
            to "Bills & Utilities",

        // Investments (mutual funds, stocks)
        Regex("(?i)mutual fund|sip|groww|zerodha|upstox|angel|5paisa|iifl|motilal|hdfc securities|icici direct|kotak securities|axis direct|paytm money|kuvera|et money|coin|smallcase|ipo|nsdl|cdsl|demat|share|stock|equity|bond|nps|ppf|fd|fixed deposit|recurring|nsc|gold|sovereign gold")
            to "Bills & Utilities",

        // Education
        Regex("(?i)school|college|university|institute|academy|tuition|coaching|class|course|udemy|coursera|skillshare|linkedin learning|unacademy|byjus|vedantu|toppr|extramarks|whitehat|upgrad|simplilearn|great learning|exam|fee|semester|admission|books|stationery")
            to "Shopping"
    )

    /**
     * Infers the category for a transaction based on merchant name and type.
     *
     * @param merchant The merchant/payee name
     * @param isIncome Whether this is an income transaction (CREDIT)
     * @param isTripMode Whether Trip Mode is enabled (defaults expenses to Trip)
     * @return The inferred category name
     */
    fun inferCategory(
        merchant: String,
        isIncome: Boolean,
        isTripMode: Boolean = false
    ): String {
        // Income transactions always go to "Money Received"
        if (isIncome) {
            return "Money Received"
        }

        // Try to match merchant against category patterns
        val normalizedMerchant = MerchantNormalizer.normalize(merchant)

        for ((pattern, category) in categoryPatterns) {
            if (pattern.containsMatchIn(normalizedMerchant) || pattern.containsMatchIn(merchant)) {
                return category
            }
        }

        // No match - use default based on Trip Mode
        return if (isTripMode) "Trip" else "Uncategorized"
    }

    /**
     * Gets the confidence level for a category inference.
     *
     * @return 1.0 if pattern matched, 0.5 if defaulted
     */
    fun getInferenceConfidence(merchant: String, isIncome: Boolean): Float {
        if (isIncome) return 1.0f

        val normalizedMerchant = MerchantNormalizer.normalize(merchant)

        for ((pattern, _) in categoryPatterns) {
            if (pattern.containsMatchIn(normalizedMerchant) || pattern.containsMatchIn(merchant)) {
                return 1.0f
            }
        }

        return 0.5f
    }

    /**
     * Gets all category names that can be inferred.
     */
    fun getInferrableCategories(): Set<String> {
        return categoryPatterns.map { it.second }.toSet()
    }
}
