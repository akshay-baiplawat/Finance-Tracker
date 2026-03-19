package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.presentation.theme.FinosCoral
import com.example.financetracker.presentation.theme.FinosMint

/**
 * First confirmation dialog when user taps Log Out.
 * Asks "Are you sure you want to log out?"
 */
@Composable
fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = "Log Out",
                tint = FinosCoral,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Log Out",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You will need to complete the onboarding again.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = FinosCoral)
            ) {
                Text("Continue", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * Second dialog asking user whether to keep or delete their data.
 */
@Composable
fun LogoutDataChoiceDialog(
    onDismiss: () -> Unit,
    onKeepData: () -> Unit,
    onDeleteData: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "What about your data?",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Would you like to keep your data for when you return, or start completely fresh?",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Keep Data Option
                DataChoiceButton(
                    icon = Icons.Rounded.Save,
                    title = "Keep My Data",
                    description = "Your transactions, wallets, and budgets will be preserved",
                    iconTint = FinosMint,
                    onClick = onKeepData
                )

                // Delete Data Option
                DataChoiceButton(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Delete Everything",
                    description = "All data will be permanently removed",
                    iconTint = FinosCoral,
                    onClick = onDeleteData
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun DataChoiceButton(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = iconTint
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
