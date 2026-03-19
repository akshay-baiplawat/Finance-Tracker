package com.example.financetracker.presentation.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.BrandPrimary
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.financetracker.core.util.BatteryOptimizationUtil
import com.example.financetracker.presentation.components.BatteryOptimizationDialog
import com.example.financetracker.presentation.components.SyncDialog

@Composable
fun OnboardingScreen(
    onPermissionGranted: (Long) -> Unit,
    onSkip: () -> Unit,
    isBatteryOptimizationPromptDismissed: Boolean = false,
    onBatteryOptimizationDismissed: (dontAskAgain: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showSyncDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var pendingSyncTime by remember { mutableStateOf<Long?>(null) }
    val isDark = isSystemInDarkTheme()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSyncDialog = true
        }
    }

    if (showSyncDialog) {
        SyncDialog(
            onDismiss = { showSyncDialog = false },
            onSync = { startTime ->
                showSyncDialog = false
                // Check if we should show battery optimization dialog
                if (!isBatteryOptimizationPromptDismissed &&
                    BatteryOptimizationUtil.shouldShowBatteryOptimizationPrompt(context)
                ) {
                    pendingSyncTime = startTime
                    showBatteryDialog = true
                } else {
                    onPermissionGranted(startTime)
                }
            },
            isDark = isDark
        )
    }

    if (showBatteryDialog) {
        BatteryOptimizationDialog(
            onDismiss = { dontAskAgain ->
                showBatteryDialog = false
                onBatteryOptimizationDismissed(dontAskAgain)
                pendingSyncTime?.let { onPermissionGranted(it) }
                pendingSyncTime = null
            },
            onEnable = {
                showBatteryDialog = false
                BatteryOptimizationUtil.openBatteryOptimizationSettings(context)
                pendingSyncTime?.let { onPermissionGranted(it) }
                pendingSyncTime = null
            },
            isDark = isDark
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Security Shield",
            tint = BrandPrimary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Headline
        Text(
            text = "Your Data.\nYour Device.",
            style = MaterialTheme.typography.displayLarge.copy(color = BrandPrimary),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Body
        Text(
            text = "We utilize a local engine to parse your bank alerts. No financial data ever leaves this phone. Enable sync to automate your tracking.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Primary Action
        Button(
            onClick = { 
                permissionLauncher.launch(Manifest.permission.READ_SMS)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) {
            Text("Enable Secure Sync", style = MaterialTheme.typography.labelLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Secondary Action
        TextButton(onClick = onSkip) {
            Text("Skip and Use Manually", color = Color.Gray)
        }
    }
}
