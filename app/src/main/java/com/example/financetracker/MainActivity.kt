package com.example.financetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.financetracker.core.notification.NotificationHelper
import com.example.financetracker.data.worker.SmsSyncWorker
import com.example.financetracker.presentation.home.HomeScreen
import com.example.financetracker.presentation.onboarding.OnboardingScreen
import com.example.financetracker.presentation.theme.FinanceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.domain.repository.NotificationRepository
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.presentation.components.PrivacyPolicyDialog
import com.example.financetracker.presentation.components.PrivacyPolicyDeclineDialog
import com.example.financetracker.presentation.main.MainScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: com.example.financetracker.presentation.profile.SettingsViewModel by viewModels()

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var pendingNotificationId: Long? = null
    private var pendingTargetRoute: String? = null
    private var pendingTargetTab: Int = 0

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result - no action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Handle notification click intent
        handleNotificationIntent()

        // Only auto-sync SMS if onboarding was completed (user didn't skip)
        // This prevents auto-sync after logout when user hasn't gone through onboarding yet
        val isOnboardingComplete = runBlocking {
            userPreferencesRepository.isOnboardingComplete.first()
        }
        if (hasSmsPermission() && isOnboardingComplete) {
            triggerSmsSync()
        }

        setContent {
            val themePreference by settingsViewModel.theme.collectAsState(initial = "system")
            val displaySizePreference by settingsViewModel.displaySize.collectAsState(initial = "medium")

            val useDarkTheme = when (themePreference) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // Sync Window Background to avoid flashes during navigation
            val windowColor = if (useDarkTheme) com.example.financetracker.presentation.theme.DarkBackground else com.example.financetracker.presentation.theme.BrandBackground
            window.decorView.setBackgroundColor(windowColor.toArgb())

            FinanceTrackerTheme(darkTheme = useDarkTheme, displaySize = displaySizePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check if privacy policy was accepted
                    val isPrivacyPolicyAccepted = runBlocking {
                        userPreferencesRepository.isPrivacyPolicyAccepted.first()
                    }
                    // Check if onboarding was already completed
                    val isOnboardingComplete = runBlocking {
                        userPreferencesRepository.isOnboardingComplete.first()
                    }
                    val isBatteryPromptDismissed = runBlocking {
                        userPreferencesRepository.isBatteryOptimizationPromptDismissed.first()
                    }

                    // Privacy policy dialog states - use key to force re-evaluation on activity recreate
                    var showPrivacyDialog by remember(isPrivacyPolicyAccepted) {
                        mutableStateOf(!isPrivacyPolicyAccepted)
                    }
                    var showDeclineDialog by remember { mutableStateOf(false) }

                    // Show onboarding when not complete (regardless of SMS permission - user might have logged out)
                    var showOnboarding by remember(isOnboardingComplete) {
                        mutableStateOf(!isOnboardingComplete)
                    }

                    // Privacy Policy Dialog - shown FIRST, blocks all other UI
                    if (showPrivacyDialog) {
                        PrivacyPolicyDialog(
                            onAccept = {
                                lifecycleScope.launch {
                                    userPreferencesRepository.setPrivacyPolicyAccepted(true)
                                }
                                showPrivacyDialog = false
                            },
                            onDecline = {
                                showDeclineDialog = true
                            },
                            isDark = useDarkTheme
                        )
                    }

                    // Decline confirmation dialog
                    if (showDeclineDialog) {
                        PrivacyPolicyDeclineDialog(
                            onGoBack = {
                                showDeclineDialog = false
                                // Privacy dialog remains visible
                            },
                            onExitApp = {
                                finish() // Close the activity
                            },
                            isDark = useDarkTheme
                        )
                    }

                    // Only show app content if privacy policy is accepted
                    if (!showPrivacyDialog) {
                        if (showOnboarding) {
                            OnboardingScreen(
                                onPermissionGranted = { startTime ->
                                    lifecycleScope.launch {
                                        userPreferencesRepository.setOnboardingComplete(true)
                                    }
                                    triggerSmsSync(startTime)
                                    showOnboarding = false
                                },
                                onSkip = {
                                    lifecycleScope.launch {
                                        userPreferencesRepository.setOnboardingComplete(true)
                                        userPreferencesRepository.setPeriodicSyncEnabled(false)
                                        // Also enable Privacy Mode to block ALL SMS sync (including pull-to-refresh)
                                        userPreferencesRepository.setPrivacyMode(true)
                                    }
                                    showOnboarding = false
                                },
                                isBatteryOptimizationPromptDismissed = isBatteryPromptDismissed,
                                onBatteryOptimizationDismissed = { dontAskAgain ->
                                    if (dontAskAgain) {
                                        lifecycleScope.launch {
                                            userPreferencesRepository.setBatteryOptimizationPromptDismissed(true)
                                        }
                                    }
                                }
                            )
                        } else {
                            MainScreen(
                                initialRoute = pendingTargetRoute,
                                initialTab = pendingTargetTab,
                                onInitialRouteConsumed = {
                                    pendingTargetRoute = null
                                    pendingTargetTab = 0
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }

    private fun handleNotificationIntent() {
        val notificationId = intent.getLongExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1L)
        val targetRoute = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ROUTE)
        val targetTab = intent.getIntExtra(NotificationHelper.EXTRA_TARGET_TAB, 0)

        if (notificationId != -1L) {
            pendingNotificationId = notificationId
            pendingTargetRoute = targetRoute
            pendingTargetTab = targetTab

            // Mark notification as read
            lifecycleScope.launch {
                notificationRepository.markAsRead(notificationId)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun triggerSmsSync(startTime: Long = -1L) {
        // Check if Privacy Mode is enabled - skip SMS sync if so
        val isPrivacyMode = runBlocking { userPreferencesRepository.isPrivacyMode.first() }
        if (isPrivacyMode) return

        // Use lastSyncTimestamp if startTime not specified
        val syncFromTime = if (startTime == -1L) {
            runBlocking { userPreferencesRepository.lastSyncTimestamp.first() }
        } else {
            startTime
        }

        val data = androidx.work.workDataOf("START_TIME" to syncFromTime)
        val workRequest = OneTimeWorkRequestBuilder<SmsSyncWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}