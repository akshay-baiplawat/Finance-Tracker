package com.example.financetracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Shared DataStore instance
private val Context.dataStore by preferencesDataStore(name = "settings")

class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_GENDER = stringPreferencesKey("user_gender")
        val KEY_USER_DESCRIPTION = stringPreferencesKey("user_description")
        val KEY_USER_DATE_OF_BIRTH = stringPreferencesKey("user_date_of_birth")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_DISPLAY_SIZE = stringPreferencesKey("display_size")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_USER_IMAGE_PATH = stringPreferencesKey("user_image_path")
        val KEY_PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val KEY_AMOUNTS_HIDDEN = booleanPreferencesKey("amounts_hidden")
        val KEY_TRIP_MODE = booleanPreferencesKey("trip_mode")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_PERIODIC_SYNC_ENABLED = booleanPreferencesKey("periodic_sync_enabled")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_BATTERY_OPTIMIZATION_PROMPT_DISMISSED = booleanPreferencesKey("battery_optimization_prompt_dismissed")
        val KEY_PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
    }

    override val userName: Flow<String> = dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    override val userEmail: Flow<String> = dataStore.data.map { it[KEY_USER_EMAIL] ?: "" }
    override val userGender: Flow<String> = dataStore.data.map { it[KEY_USER_GENDER] ?: "" }
    override val userDescription: Flow<String> = dataStore.data.map { it[KEY_USER_DESCRIPTION] ?: "" }
    override val userDateOfBirth: Flow<String> = dataStore.data.map { it[KEY_USER_DATE_OF_BIRTH] ?: "" }
    override val currency: Flow<String> = dataStore.data.map { it[KEY_CURRENCY] ?: "INR" }
    override val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    override val userImagePath: Flow<String?> = dataStore.data.map { it[KEY_USER_IMAGE_PATH] }
    override val theme: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: "system" }
    override val displaySize: Flow<String> = dataStore.data.map { it[KEY_DISPLAY_SIZE] ?: "medium" }
    override val isPrivacyMode: Flow<Boolean> = dataStore.data.map { it[KEY_PRIVACY_MODE] ?: false }
    override val isAmountsHidden: Flow<Boolean> = dataStore.data.map { it[KEY_AMOUNTS_HIDDEN] ?: false }
    override val isTripMode: Flow<Boolean> = dataStore.data.map { it[KEY_TRIP_MODE] ?: false }
    override val lastSyncTimestamp: Flow<Long> = dataStore.data.map { it[KEY_LAST_SYNC_TIMESTAMP] ?: 0L }
    override val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    override val isPeriodicSyncEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PERIODIC_SYNC_ENABLED] ?: true }
    override val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }
    override val isBatteryOptimizationPromptDismissed: Flow<Boolean> = dataStore.data.map { it[KEY_BATTERY_OPTIMIZATION_PROMPT_DISMISSED] ?: false }
    override val isPrivacyPolicyAccepted: Flow<Boolean> = dataStore.data.map { it[KEY_PRIVACY_POLICY_ACCEPTED] ?: false }

    override suspend fun updateName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name }
    }

    override suspend fun updateEmail(email: String) {
        dataStore.edit { it[KEY_USER_EMAIL] = email }
    }

    override suspend fun updateGender(gender: String) {
        dataStore.edit { it[KEY_USER_GENDER] = gender }
    }

    override suspend fun updateDescription(description: String) {
        dataStore.edit { it[KEY_USER_DESCRIPTION] = description }
    }

    override suspend fun updateDateOfBirth(dateOfBirth: String) {
        dataStore.edit { it[KEY_USER_DATE_OF_BIRTH] = dateOfBirth }
    }

    override suspend fun updateCurrency(currency: String) {
        dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    override suspend fun toggleBiometric(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    override suspend fun updateTheme(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    override suspend fun updateDisplaySize(size: String) {
        dataStore.edit { it[KEY_DISPLAY_SIZE] = size }
    }

    override suspend fun updateUserImage(path: String) {
        dataStore.edit { it[KEY_USER_IMAGE_PATH] = path }
    }

    override suspend fun clearUserImage() {
        dataStore.edit { it.remove(KEY_USER_IMAGE_PATH) }
    }

    override suspend fun setPrivacyMode(enabled: Boolean) {
        dataStore.edit { it[KEY_PRIVACY_MODE] = enabled }
    }

    override suspend fun setAmountsHidden(hidden: Boolean) {
        dataStore.edit { it[KEY_AMOUNTS_HIDDEN] = hidden }
    }

    override suspend fun setTripMode(enabled: Boolean) {
        dataStore.edit { it[KEY_TRIP_MODE] = enabled }
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_SYNC_TIMESTAMP] = timestamp }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setPeriodicSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PERIODIC_SYNC_ENABLED] = enabled }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun setBatteryOptimizationPromptDismissed(dismissed: Boolean) {
        dataStore.edit { it[KEY_BATTERY_OPTIMIZATION_PROMPT_DISMISSED] = dismissed }
    }

    override suspend fun setPrivacyPolicyAccepted(accepted: Boolean) {
        dataStore.edit { it[KEY_PRIVACY_POLICY_ACCEPTED] = accepted }
    }

    override suspend fun resetAllPreferences() {
        dataStore.edit { prefs ->
            // Clear user profile data
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_GENDER)
            prefs.remove(KEY_USER_DESCRIPTION)
            prefs.remove(KEY_USER_DATE_OF_BIRTH)
            prefs.remove(KEY_USER_IMAGE_PATH)

            // Reset app settings to defaults
            prefs[KEY_CURRENCY] = "INR"
            prefs[KEY_THEME] = "system"
            prefs[KEY_DISPLAY_SIZE] = "medium"
            prefs[KEY_BIOMETRIC_ENABLED] = false

            // Reset feature flags
            prefs[KEY_PRIVACY_MODE] = false
            prefs[KEY_AMOUNTS_HIDDEN] = false
            prefs[KEY_TRIP_MODE] = false
            prefs[KEY_NOTIFICATIONS_ENABLED] = true
            prefs[KEY_PERIODIC_SYNC_ENABLED] = true

            // Reset sync timestamp
            prefs[KEY_LAST_SYNC_TIMESTAMP] = 0L

            // Reset onboarding and privacy policy to trigger fresh install flow
            prefs[KEY_ONBOARDING_COMPLETE] = false
            prefs[KEY_PRIVACY_POLICY_ACCEPTED] = false
            prefs[KEY_BATTERY_OPTIMIZATION_PROMPT_DISMISSED] = false
        }
    }
}
