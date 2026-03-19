package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatteryOptimizationDialog(
    onDismiss: (dontAskAgain: Boolean) -> Unit,
    onEnable: () -> Unit,
    isDark: Boolean
) {
    var dontAskAgain by remember { mutableStateOf(false) }

    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = { onDismiss(dontAskAgain) },
        icon = {
            Icon(
                imageVector = Icons.Rounded.BatteryAlert,
                contentDescription = "Battery Optimization",
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Enable Background Sync",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "To ensure reliable automatic SMS syncing, Finance Tracker needs to run in the background without battery restrictions.\n\nThis won't significantly impact your battery life.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it }
                    )
                    Text(
                        text = "Don't ask again",
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onEnable) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(dontAskAgain) }) {
                Text("Skip")
            }
        }
    )
}
