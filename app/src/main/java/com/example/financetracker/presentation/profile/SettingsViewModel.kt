package com.example.financetracker.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetracker.core.util.CsvExportUtil
import com.example.financetracker.data.worker.PeriodicSmsSyncWorker
import com.example.financetracker.di.ApplicationScope
import com.example.financetracker.domain.repository.TransactionRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.financetracker.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val repository: TransactionRepository,
    private val financeRepository: FinanceRepository,
    private val csvExportUtil: CsvExportUtil,
    private val csvImportUtil: com.example.financetracker.core.util.CsvImportUtil,
    private val workManager: androidx.work.WorkManager,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncResult = MutableSharedFlow<String>()
    val syncResult = _syncResult.asSharedFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State Flows delegates
    val userName: Flow<String> = userPreferencesRepository.userName
    val currency: Flow<String> = userPreferencesRepository.currency
    val isBiometricEnabled: Flow<Boolean> = userPreferencesRepository.isBiometricEnabled
    val userImagePath: Flow<String?> = userPreferencesRepository.userImagePath
    val theme: Flow<String> = userPreferencesRepository.theme
    val displaySize: Flow<String> = userPreferencesRepository.displaySize

    val isPrivacyMode: StateFlow<Boolean> = userPreferencesRepository.isPrivacyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTripMode: StateFlow<Boolean> = userPreferencesRepository.isTripMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPeriodicSyncEnabled: StateFlow<Boolean> = userPreferencesRepository.isPeriodicSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBatteryOptimizationPromptDismissed: StateFlow<Boolean> = userPreferencesRepository.isBatteryOptimizationPromptDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateName(newName: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateName(newName)
        }
    }

    fun updateCurrency(currencyCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateCurrency(currencyCode)
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.toggleBiometric(enabled)
        }
    }
    
    fun updateTheme(themeValue: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(themeValue)
        }
    }

    fun updateDisplaySize(size: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateDisplaySize(size)
        }
    }

    fun setPrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyMode(enabled)
            // Update lastSyncTimestamp to current time on both ON and OFF
            // This ensures SMS received during Privacy Mode are permanently skipped
            userPreferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())
        }
    }

    fun setTripMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTripMode(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setPeriodicSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPeriodicSyncEnabled(enabled)
            if (enabled) {
                PeriodicSmsSyncWorker.schedule(context)
            } else {
                PeriodicSmsSyncWorker.cancel(context)
            }
        }
    }

    fun setBatteryOptimizationPromptDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBatteryOptimizationPromptDismissed(dismissed)
        }
    }
    
    fun updateUserImage(path: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserImage(path)
        }
    }

    fun exportData(onResult: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val transactions = financeRepository.getAllTransactionsForExport()
                if (transactions.isNotEmpty()) {
                    val uri = csvExportUtil.generateCsv(transactions)
                    onResult(uri)
                } else {
                    _error.value = "No transactions to export"
                    onResult(null)
                }
            } catch (e: Exception) {
                _error.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                onResult(null)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveCsvToDevice(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val count = financeRepository.getTransactionCountForExport()
                if (count > 0) {
                    // Use streaming export to prevent OOM on large datasets
                    val filename = csvExportUtil.saveToDownloadsStreaming(
                        getChunk = { limit, offset ->
                            financeRepository.getTransactionsChunked(limit, offset)
                        },
                        totalCount = count
                    )
                    onResult(filename)
                } else {
                    _error.value = "No transactions to export"
                    onResult(null)
                }
            } catch (e: Exception) {
                _error.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                onResult(null)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importCsv(uri: android.net.Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val transactions = csvImportUtil.parseCsv(uri)
                if (transactions.isNotEmpty()) {
                    repository.batchInsert(transactions)
                    onResult(transactions.size)
                } else {
                    _error.value = "No valid transactions found in file"
                    onResult(0)
                }
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.localizedMessage ?: "Unknown error"}"
                onResult(-1)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun triggerSync(startTime: Long) {
        viewModelScope.launch {
            if (isPrivacyMode.value) {
                _syncResult.emit("Privacy Mode is enabled. SMS sync is disabled.")
                return@launch
            }
            _isSyncing.value = true
            try {
                val count = financeRepository.syncTransactions(startTime)
                _syncResult.emit("Sync Complete: Added $count new transactions")
            } catch (e: Exception) {
                _error.value = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun wipeData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllTransactions()
                _syncResult.emit("All data cleared successfully")
                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to clear data: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun logout(keepData: Boolean, onComplete: () -> Unit) {
        // Use applicationScope so operations complete even if activity is destroyed
        applicationScope.launch {
            try {
                // If user chose to delete data, clear all database data first
                if (!keepData) {
                    financeRepository.clearAllData()
                }

                // Always reset all preferences to trigger fresh install flow
                userPreferencesRepository.resetAllPreferences()

                // Cancel any scheduled workers
                PeriodicSmsSyncWorker.cancel(context)
                workManager.cancelUniqueWork("debt_reminder_weekly")
                workManager.cancelUniqueWork("subscription_reminder_daily")

                // Small delay to ensure all database operations are flushed
                kotlinx.coroutines.delay(200)

                // Switch to Main thread and then call onComplete
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _error.value = "Logout failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
