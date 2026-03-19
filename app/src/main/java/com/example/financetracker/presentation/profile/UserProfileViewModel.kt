package com.example.financetracker.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userName: Flow<String> = userPreferencesRepository.userName
    val userEmail: Flow<String> = userPreferencesRepository.userEmail
    val userGender: Flow<String> = userPreferencesRepository.userGender
    val userDescription: Flow<String> = userPreferencesRepository.userDescription
    val userDateOfBirth: Flow<String> = userPreferencesRepository.userDateOfBirth
    val userImagePath: Flow<String?> = userPreferencesRepository.userImagePath

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var nameUpdateJob: Job? = null
    private var emailUpdateJob: Job? = null
    private var descriptionUpdateJob: Job? = null

    fun updateName(name: String) {
        nameUpdateJob?.cancel()
        nameUpdateJob = viewModelScope.launch {
            try {
                delay(500L)
                userPreferencesRepository.updateName(name)
            } catch (e: Exception) {
                _error.value = "Failed to update name: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateEmail(email: String) {
        emailUpdateJob?.cancel()
        emailUpdateJob = viewModelScope.launch {
            try {
                delay(500L)
                userPreferencesRepository.updateEmail(email)
            } catch (e: Exception) {
                _error.value = "Failed to update email: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateGender(gender: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateGender(gender)
            } catch (e: Exception) {
                _error.value = "Failed to update gender: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateDescription(description: String) {
        descriptionUpdateJob?.cancel()
        descriptionUpdateJob = viewModelScope.launch {
            try {
                delay(500L)
                userPreferencesRepository.updateDescription(description)
            } catch (e: Exception) {
                _error.value = "Failed to update description: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateDateOfBirth(dateOfBirth: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateDateOfBirth(dateOfBirth)
            } catch (e: Exception) {
                _error.value = "Failed to update date of birth: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateUserImage(path: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateUserImage(path)
            } catch (e: Exception) {
                _error.value = "Failed to update profile photo: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearUserImage() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.clearUserImage()
            } catch (e: Exception) {
                _error.value = "Failed to remove profile photo: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
