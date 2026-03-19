package com.example.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user's manual category corrections for merchants.
 * When a user changes the category for a transaction from a specific merchant,
 * this preference is remembered for future SMS syncs.
 *
 * This allows the app to "learn" from user corrections and apply them
 * automatically to new transactions from the same merchant.
 */
@Entity(tableName = "merchant_category_preferences")
data class MerchantCategoryPreferenceEntity(
    /**
     * Normalized merchant name (from MerchantNormalizer).
     * Example: "Amazon", "Zomato", "Uber"
     */
    @PrimaryKey
    val merchantNormalized: String,

    /**
     * User's preferred category for this merchant.
     * Example: "Food & Dining", "Shopping", "Transportation"
     */
    val categoryName: String,

    /**
     * Timestamp when this preference was last updated.
     * Used to show most recently corrected merchants first.
     */
    val lastUpdated: Long = System.currentTimeMillis(),

    /**
     * Number of times user has set this preference.
     * Higher count = stronger preference (user is consistent).
     */
    val correctionCount: Int = 1
)
