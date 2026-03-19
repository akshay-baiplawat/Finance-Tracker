package com.example.financetracker.presentation.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberSheet(
    sheetState: SheetState,
    availablePeople: List<PersonEntity>,
    onDismiss: () -> Unit,
    onAddNewPerson: () -> Unit,
    onSave: (List<Long>) -> Unit
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddMemberContent(
            availablePeople = availablePeople,
            onDismiss = onDismiss,
            onAddNewPerson = onAddNewPerson,
            onSave = onSave
        )
    }
}

@Composable
private fun AddMemberContent(
    availablePeople: List<PersonEntity>,
    onDismiss: () -> Unit,
    onAddNewPerson: () -> Unit,
    onSave: (List<Long>) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var selectedMemberIds by remember { mutableStateOf(setOf<Long>()) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinosMint)
                .padding(16.dp)
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = keyboardAwareOnClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Title
            Text(
                text = "Add Members",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (availablePeople.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = "Add member",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No people available to add",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a new person first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddNewPerson()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FinosMint
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add person",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Person")
                    }
                }
            } else {
                // Member selection label
                Text(
                    text = "Select Members",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Member selector
                MemberSelector(
                    people = availablePeople,
                    selectedIds = selectedMemberIds,
                    onSelectionChanged = { selectedMemberIds = it },
                    onAddNewPerson = {
                        onDismiss()
                        onAddNewPerson()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(selectedMemberIds.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedMemberIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinosMint,
                        disabledContainerColor = FinosMint.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedMemberIds.size == 1) "Add Member"
                               else "Add ${selectedMemberIds.size} Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MemberSelector(
    people: List<PersonEntity>,
    selectedIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit,
    onAddNewPerson: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add New Person button
        item {
            FilterChip(
                selected = false,
                onClick = onAddNewPerson,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New")
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = FinosMint.copy(alpha = 0.1f),
                    labelColor = FinosMint,
                    iconColor = FinosMint
                )
            )
        }

        // Existing people
        items(people) { person ->
            val isSelected = selectedIds.contains(person.id)
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectionChanged(
                        if (isSelected) selectedIds - person.id
                        else selectedIds + person.id
                    )
                },
                label = { Text(person.name) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinosMint.copy(alpha = 0.2f),
                    selectedLabelColor = FinosMint,
                    selectedLeadingIconColor = FinosMint
                )
            )
        }
    }
}
