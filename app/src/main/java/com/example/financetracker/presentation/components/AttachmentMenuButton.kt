package com.example.financetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.presentation.theme.FinosCoral
import com.example.financetracker.presentation.theme.FinosMint

/**
 * Reusable attachment menu button for transaction sheets.
 * Shows an attachment icon that opens a dropdown menu with "Take Photo" and "Choose Files" options.
 *
 * @param attachmentCount Current number of attachments
 * @param maxAttachments Maximum allowed attachments (default 5)
 * @param onTakePhoto Callback when "Take Photo" is selected
 * @param onChooseFiles Callback when "Choose Files" is selected
 * @param enabled Whether the button is enabled
 * @param tint Icon tint color
 * @param modifier Additional modifiers
 */
@Composable
fun AttachmentMenuButton(
    attachmentCount: Int,
    maxAttachments: Int = 5,
    onTakePhoto: () -> Unit,
    onChooseFiles: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val canAddMore = attachmentCount < maxAttachments

    Box(modifier = modifier) {
        IconButton(
            onClick = { if (canAddMore && enabled) showMenu = true },
            enabled = enabled && canAddMore
        ) {
            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = "Add attachment",
                tint = if (enabled && canAddMore) tint else tint.copy(alpha = 0.4f)
            )
        }

        // Badge showing attachment count
        if (attachmentCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(FinosCoral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (attachmentCount > 9) "9+" else attachmentCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = FinosMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Take Photo")
                    }
                },
                onClick = {
                    showMenu = false
                    onTakePhoto()
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = FinosMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Choose Files")
                    }
                },
                onClick = {
                    showMenu = false
                    onChooseFiles()
                }
            )
        }
    }
}

/**
 * Compact attachment preview row for displaying attachments below the header.
 * Uses the existing AttachmentPickerSection but without the add button (since we have header icon).
 */
@Composable
fun AttachmentPreviewRow(
    attachments: List<AttachmentUiModel>,
    onRemoveAttachment: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isNotEmpty()) {
        AttachmentPickerSection(
            attachments = attachments,
            maxAttachments = 5,
            onAddAttachments = { /* No-op - adding is done via header button */ },
            onRemoveAttachment = onRemoveAttachment,
            onAttachmentClick = onAttachmentClick,
            modifier = modifier,
            showAddButton = false // Hide the add button since we use header icon
        )
    }
}
