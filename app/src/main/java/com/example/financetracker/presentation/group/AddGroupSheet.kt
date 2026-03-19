package com.example.financetracker.presentation.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.presentation.components.CategoryMultiSelect
import com.example.financetracker.presentation.components.DatePickerField
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import com.example.financetracker.presentation.theme.FinosMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupSheet(
    sheetState: SheetState,
    availablePeople: List<PersonEntity>,
    availableCategories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddNewPerson: () -> Unit = {},
    onSave: (
        name: String,
        type: String,
        budgetLimit: Long,
        targetAmount: Long,
        colorHex: String,
        memberIds: List<Long>,
        filterCategoryIds: String?,
        filterStartDate: Long?,
        filterEndDate: Long?
    ) -> Unit
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddGroupContent(
            availablePeople = availablePeople,
            availableCategories = availableCategories,
            onDismiss = onDismiss,
            onAddNewPerson = onAddNewPerson,
            onSave = onSave
        )
    }
}

@Composable
private fun AddGroupContent(
    availablePeople: List<PersonEntity>,
    availableCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddNewPerson: () -> Unit,
    onSave: (String, String, Long, Long, String, List<Long>, String?, Long?, Long?) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var groupName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GroupType.GENERAL.name) }
    var budgetAmount by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2196F3") }
    var selectedMemberIds by remember { mutableStateOf(setOf<Long>()) }

    // Category filter fields (for Group By Category type)
    var selectedCategoryIds by remember { mutableStateOf(setOf<Long>()) }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }
    var isFromDayOneSelected by remember { mutableStateOf(true) }
    var isTillNowSelected by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    // Check if it's a category filter group type
    val isCategoryFilterType = selectedType == GroupType.TRIP.name

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
            // Back button
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
                text = "New Group",
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
            // Group Type Selection
            Text(
                text = "Group Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            GroupTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Group Name
            Text(
                text = "Group Name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = {
                    Text(
                        getPlaceholderForType(selectedType),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinosMint,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Type-specific fields
            when (selectedType) {
                GroupType.TRIP.name -> {
                    // Group By Category: Show category multi-select and date pickers
                    Text(
                        text = "Select Categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CategoryMultiSelect(
                        categories = availableCategories,
                        selectedCategoryIds = selectedCategoryIds,
                        onSelectionChanged = { selectedCategoryIds = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Start Date
                    DatePickerField(
                        label = "Start Date",
                        selectedDate = filterStartDate,
                        quickOptionLabel = "From Day One",
                        isQuickOptionSelected = isFromDayOneSelected,
                        onDateSelected = { date ->
                            filterStartDate = date
                            isFromDayOneSelected = false
                        },
                        onQuickOptionSelected = {
                            isFromDayOneSelected = true
                            filterStartDate = null
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // End Date
                    DatePickerField(
                        label = "End Date",
                        selectedDate = filterEndDate,
                        quickOptionLabel = "Till Now",
                        isQuickOptionSelected = isTillNowSelected,
                        onDateSelected = { date ->
                            filterEndDate = date
                            isTillNowSelected = false
                        },
                        onQuickOptionSelected = {
                            isTillNowSelected = true
                            filterEndDate = null
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                GroupType.GENERAL.name -> {
                    // GENERAL type: No budget limit or special fields needed
                    // This is a simple transaction recording group
                }

                GroupType.SAVINGS_GOAL.name -> {
                    Text(
                        text = "Savings Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) targetAmount = it },
                        placeholder = {
                            Text(
                                "Enter target amount",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        prefix = {
                            Text(
                                "₹",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FinosMint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Color Selection
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ColorSelector(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Members Selection - hide for category filter groups and GENERAL groups
            val isGeneralType = selectedType == GroupType.GENERAL.name
            if (!isCategoryFilterType && !isGeneralType) {
                Text(
                    text = "Add Members",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                MemberSelector(
                    people = availablePeople,
                    selectedIds = selectedMemberIds,
                    onSelectionChanged = { selectedMemberIds = it },
                    onAddNewPerson = onAddNewPerson
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Create Button
            val isCreateEnabled = if (isCategoryFilterType) {
                groupName.isNotBlank() && selectedCategoryIds.isNotEmpty()
            } else {
                groupName.isNotBlank()
            }

            Button(
                onClick = {
                    val budgetPaisa = (budgetAmount.toLongOrNull() ?: 0L) * 100
                    val targetPaisa = (targetAmount.toLongOrNull() ?: 0L) * 100

                    // For category filter groups, build the filter fields
                    val categoryIdsString = if (isCategoryFilterType && selectedCategoryIds.isNotEmpty()) {
                        selectedCategoryIds.joinToString(",")
                    } else null

                    val startDate = if (isCategoryFilterType && !isFromDayOneSelected) {
                        filterStartDate
                    } else null

                    val endDate = if (isCategoryFilterType && !isTillNowSelected) {
                        filterEndDate
                    } else null

                    onSave(
                        groupName,
                        selectedType,
                        budgetPaisa,
                        targetPaisa,
                        selectedColor,
                        if (isCategoryFilterType || isGeneralType) emptyList() else selectedMemberIds.toList(),
                        categoryIdsString,
                        startDate,
                        endDate
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isCreateEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinosMint,
                    disabledContainerColor = FinosMint.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Create Group",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val types = listOf(
        GroupTypeOption(GroupType.GENERAL.name, "General", Icons.Rounded.Group),
        GroupTypeOption(GroupType.TRIP.name, "Group By Category", Icons.Rounded.Category),
        GroupTypeOption(GroupType.SPLIT_EXPENSE.name, "Split Bill", Icons.Rounded.Receipt),
        GroupTypeOption(GroupType.SAVINGS_GOAL.name, "Savings", Icons.Rounded.Savings)
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { type ->
            val isSelected = selectedType == type.value
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type.value) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = type.label,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(type.label)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinosMint.copy(alpha = 0.2f),
                    selectedLabelColor = FinosMint,
                    selectedLeadingIconColor = FinosMint
                )
            )
        }
    }
}

@Composable
private fun ColorSelector(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#2196F3", "#4CAF50", "#FF9800", "#E91E63",
        "#9C27B0", "#00BCD4", "#FF5722", "#607D8B"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors) { colorHex ->
            val isSelected = selectedColor == colorHex
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                    .clickable { onColorSelected(colorHex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
                            contentDescription = "Add person",
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

private data class GroupTypeOption(
    val value: String,
    val label: String,
    val icon: ImageVector
)

private fun getPlaceholderForType(type: String) = when (type) {
    GroupType.TRIP.name -> "e.g., Food Expenses, Office Supplies"
    GroupType.SPLIT_EXPENSE.name -> "e.g., Roommates, Dinner Club"
    GroupType.SAVINGS_GOAL.name -> "e.g., New Phone, Vacation Fund"
    else -> "e.g., Weekend Plans"
}
