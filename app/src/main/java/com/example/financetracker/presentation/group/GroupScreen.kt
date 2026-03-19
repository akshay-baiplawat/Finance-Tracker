package com.example.financetracker.presentation.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.presentation.group.components.GroupCard
import com.example.financetracker.presentation.model.GroupUiModel
import com.example.financetracker.presentation.theme.FinosMint

@Composable
fun GroupScreen(
    onGroupClick: (Long) -> Unit,
    onAddGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips
        FilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { viewModel.setFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            // Empty state
            EmptyGroupsState(onAddGroupClick = onAddGroupClick)
        } else {
            // Groups list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group.id) }
                    )
                }

                // Add group button at bottom
                item {
                    AddGroupButton(onClick = onAddGroupClick)
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        "All" to null,
        "By Category" to GroupType.TRIP.name,
        "Split Bills" to GroupType.SPLIT_EXPENSE.name,
        "Savings" to GroupType.SAVINGS_GOAL.name
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (label, filterValue) ->
            val isSelected = (selectedFilter == filterValue) ||
                    (selectedFilter == null && filterValue == null)

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filterValue) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinosMint.copy(alpha = 0.2f),
                    selectedLabelColor = FinosMint
                )
            )
        }
    }
}

@Composable
private fun EmptyGroupsState(
    onAddGroupClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = "No groups",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No groups yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create a group to split expenses with friends, track trip budgets, or save together",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddGroupClick,
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create group",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Group")
            }
        }
    }
}

@Composable
private fun EmptyGroupsMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Group,
            contentDescription = "No groups",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No groups yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a group to split expenses with friends, track trip budgets, or save together",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AddGroupButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50), // Pill Shape - same as Add Person
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add group"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Add New Group",
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Standalone groups content for embedding in other screens (e.g., Ledger tab)
 */
@Composable
fun GroupsContent(
    groups: List<GroupUiModel>,
    onGroupClick: (Long) -> Unit,
    onAddGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (groups.isEmpty()) {
            item {
                EmptyGroupsMessage()
            }
        } else {
            items(groups, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    onClick = { onGroupClick(group.id) }
                )
            }
        }

        // Always show Add button at bottom
        item {
            AddGroupButton(onClick = onAddGroupClick)
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
