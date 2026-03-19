package com.example.financetracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userName: Flow<String>
    val userEmail: Flow<String>
    val userGender: Flow<String>
    val userDescription: Flow<String>
    val userDateOfBirth: Flow<String>
    val currency: Flow<String>
    val isBiometricEnabled: Flow<Boolean>
    val userImagePath: Flow<String?>
    val theme: Flow<String>
    val displaySize: Flow<String>
    val isPrivacyMode: Flow<Boolean>
    val isAmountsHidden: Flow<Boolean>
    val isTripMode: Flow<Boolean>
    val lastSyncTimestamp: Flow<Long>
    val isNotificationsEnabled: Flow<Boolean>
    val isPeriodicSyncEnabled: Flow<Boolean>
    val isOnboardingComplete: Flow<Boolean>
    val isBatteryOptimizationPromptDismissed: Flow<Boolean>
    val isPrivacyPolicyAccepted: Flow<Boolean>

    suspend fun updateName(name: String)
    suspend fun updateEmail(email: String)
    suspend fun updateGender(gender: String)
    suspend fun updateDescription(description: String)
    suspend fun updateDateOfBirth(dateOfBirth: String)
    suspend fun updateCurrency(currency: String)
    suspend fun toggleBiometric(enabled: Boolean)
    suspend fun updateTheme(theme: String)
    suspend fun updateDisplaySize(size: String)
    suspend fun updateUserImage(path: String)
    suspend fun clearUserImage()
    suspend fun setPrivacyMode(enabled: Boolean)
    suspend fun setAmountsHidden(hidden: Boolean)
    suspend fun setTripMode(enabled: Boolean)
    suspend fun updateLastSyncTimestamp(timestamp: Long)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setPeriodicSyncEnabled(enabled: Boolean)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setBatteryOptimizationPromptDismissed(dismissed: Boolean)
    suspend fun setPrivacyPolicyAccepted(accepted: Boolean)
    suspend fun resetAllPreferences()
}
