package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.financetracker.presentation.theme.FinosCoral
import com.example.financetracker.presentation.theme.FinosMint

/**
 * Confirmation dialog shown when user declines the Privacy Policy.
 * Offers options to go back and review, or exit the app.
 */
@Composable
fun PrivacyPolicyDeclineDialog(
    onGoBack: () -> Unit,
    onExitApp: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onGoBack,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = FinosCoral,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Privacy Policy Required",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "You must accept the Privacy Policy to use Finance Tracker. Without acceptance, the app cannot function.\n\nWould you like to go back and review the policy, or exit the app?",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint)
            ) {
                Text("Go Back", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onExitApp) {
                Text("Exit App", color = FinosCoral)
            }
        }
    )
}
