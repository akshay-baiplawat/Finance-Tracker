package com.example.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financetracker.data.local.entity.MerchantCategoryPreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user's merchant category preferences.
 * Used to store and retrieve user corrections for automatic category inference.
 */
@Dao
interface MerchantCategoryPreferenceDao {

    /**
     * Insert or update a merchant category preference.
     * On conflict, replaces the existing preference.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: MerchantCategoryPreferenceEntity)

    /**
     * Get the user's preferred category for a specific merchant.
     * Returns null if no preference exists.
     */
    @Query("SELECT categoryName FROM merchant_category_preferences WHERE merchantNormalized = :merchantNormalized")
    suspend fun getPreferredCategory(merchantNormalized: String): String?

    /**
     * Get all merchant preferences as a Flow (for reactive UI).
     */
    @Query("SELECT * FROM merchant_category_preferences ORDER BY lastUpdated DESC")
    fun getAllPreferencesFlow(): Flow<List<MerchantCategoryPreferenceEntity>>

    /**
     * Get all merchant preferences as a map (for batch lookup during sync).
     * Returns a list that can be converted to a map.
     */
    @Query("SELECT * FROM merchant_category_preferences")
    suspend fun getAllPreferences(): List<MerchantCategoryPreferenceEntity>

    /**
     * Delete a specific merchant preference.
     */
    @Query("DELETE FROM merchant_category_preferences WHERE merchantNormalized = :merchantNormalized")
    suspend fun deletePreference(merchantNormalized: String)

    /**
     * Delete all merchant preferences (for logout/reset).
     */
    @Query("DELETE FROM merchant_category_preferences")
    suspend fun deleteAllPreferences()

    /**
     * Get the count of merchant preferences (for statistics).
     */
    @Query("SELECT COUNT(*) FROM merchant_category_preferences")
    suspend fun getPreferenceCount(): Int

    /**
     * Increment the correction count for a merchant.
     * Called when user confirms the same category again.
     */
    @Query("""
        UPDATE merchant_category_preferences
        SET correctionCount = correctionCount + 1, lastUpdated = :timestamp
        WHERE merchantNormalized = :merchantNormalized
    """)
    suspend fun incrementCorrectionCount(merchantNormalized: String, timestamp: Long = System.currentTimeMillis())
}
