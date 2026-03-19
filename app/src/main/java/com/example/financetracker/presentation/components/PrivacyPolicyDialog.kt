package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.financetracker.presentation.theme.FinosMint

/**
 * First-launch Privacy Policy agreement dialog.
 * User must accept to use the app - cannot be dismissed by tapping outside or pressing back.
 */
@Composable
fun PrivacyPolicyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = { /* Cannot dismiss - must accept or decline */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = "Privacy",
                tint = FinosMint,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Privacy Policy",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "We believe your financial data is exactly that—yours. Finance Tracker is built as a \"Local-First\" app.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Summary of key points - matching Local-First messaging
                PolicyHighlight(
                    title = "Local-First Architecture",
                    description = "Every transaction stored locally in an encrypted database on your device",
                    textColor = textColor
                )

                PolicyHighlight(
                    title = "No Cloud Servers",
                    description = "We do not have access to your financial data. We cannot read, share, or sell it.",
                    textColor = textColor
                )

                PolicyHighlight(
                    title = "Your Data, Your Control",
                    description = "Export to CSV or delete everything at any time from Profile settings",
                    textColor = textColor
                )

                PolicyHighlight(
                    title = "Transparent Business Model",
                    description = "You are our customer, not our product. No data harvesting.",
                    textColor = textColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "By tapping \"Accept\", you agree to our Privacy Policy. You can view the full policy anytime in Profile > Privacy Policy.",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint)
            ) {
                Text("Accept", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun PolicyHighlight(
    title: String,
    description: String,
    textColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = FinosMint
        )
        Text(
            text = description,
            fontSize = 13.sp,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}
