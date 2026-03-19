package com.example.financetracker.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.core.util.CategoryIconUtil
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.presentation.components.CategoryIcon
import com.example.financetracker.presentation.components.IconPickerButton
import com.example.financetracker.presentation.components.ColorPickerButton
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost
import com.example.financetracker.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val selectedTab by viewModel.selectedTab.collectAsState() // 0 = Expense, 1 = Income
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    // Filter logic
    val filteredCategories = categories.filter {
        if (selectedTab == 0) it.type == "EXPENSE" else it.type == "INCOME"
    }

    if (showAddDialog || categoryToEdit != null) {
        CategoryDialog(
            category = categoryToEdit,
            type = if (selectedTab == 0) "EXPENSE" else "INCOME",
            onDismiss = {
                showAddDialog = false
                categoryToEdit = null
            },
            onSave = { name, color, iconId ->
                if (categoryToEdit == null) {
                    viewModel.addCategory(name, if (selectedTab == 0) "EXPENSE" else "INCOME", color, iconId)
                } else {
                    viewModel.updateCategory(categoryToEdit!!.id, name, categoryToEdit!!.type, color, iconId)
                }
                showAddDialog = false
                categoryToEdit = null
            },
            onDelete = {
                categoryToEdit?.let { viewModel.deleteCategory(it.id) }
                showAddDialog = false
                categoryToEdit = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = FinosMint,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FinosMint
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Expense") },
                    selectedContentColor = FinosMint,
                    unselectedContentColor = TextSecondaryDark
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("Income") },
                    selectedContentColor = FinosMint,
                    unselectedContentColor = TextSecondaryDark
                )
            }
            
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCategories) { category ->
                    CategoryItem(category) {
                        categoryToEdit = category
                    }
                }

                item {
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(56.dp)
                             .border(1.dp, inputBorderColor(), CircleShape)
                             .clip(CircleShape)
                             .clickable { showAddDialog = true }
                             .padding(horizontal = 16.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Rounded.Add, contentDescription = "Add category", tint = MaterialTheme.colorScheme.onSurface)
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Add New Category", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: CategoryEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use CategoryIcon composable
        CategoryIcon(
            categoryName = category.name,
            iconId = category.iconId,
            colorHex = category.colorHex,
            size = 48.dp,
            useRoundedCorners = false
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = TextSecondaryDark)
    }
}


@Composable
fun CategoryDialog(
    category: CategoryEntity?,
    type: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    // Use isSystemDefault from entity to determine if category is protected
    // For system categories: name and icon are protected, but color can be changed
    val isProtected = remember(category) { category?.isSystemDefault == true }

    var name by remember { mutableStateOf(category?.name ?: "") }
    val defaultColor = if (type == "EXPENSE") "#EF4444" else "#10B981"
    var color by remember { mutableStateOf(category?.colorHex ?: defaultColor) }
    var iconId by remember {
        mutableStateOf(
            category?.iconId ?: CategoryIconUtil.getDefaultIconIdForCategory(category?.name ?: "")
        )
    }

    // Track if color was changed (to enable save for system categories)
    val originalColor = remember { category?.colorHex ?: defaultColor }
    val colorChanged = color != originalColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Icon picker row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconPickerButton(
                        currentIconId = iconId,
                        colorHex = color,
                        enabled = !isProtected,
                        onIconSelected = { iconId = it }
                    )

                    Column {
                        Text(
                            "Icon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (isProtected) "System icon" else "Tap to change",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Color picker row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ColorPickerButton(
                        currentColorHex = color,
                        enabled = true, // Color is always changeable
                        onColorSelected = { color = it }
                    )

                    Column {
                        Text(
                            "Color",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap to change",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if(!isProtected) name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    readOnly = isProtected,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                if(isProtected) {
                     Text("System protected category. Only color can be changed.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                }
            }
        },
        confirmButton = {
            // For system categories, allow save only if color changed
            // For user categories, allow save if name is not empty
            val canSave = if (isProtected) colorChanged else name.isNotEmpty()
            TextButton(
                onClick = {
                    if (canSave) {
                        onSave(name, color, iconId)
                    }
                },
                enabled = canSave
            ) {
                Text("Save", color = if(!canSave) Color.Gray.copy(alpha=0.5f) else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
             if (category != null) {
                 TextButton(
                     onClick = onDelete,
                     enabled = !isProtected,
                     colors = ButtonDefaults.textButtonColors(contentColor = FinosCoral)
                 ) {
                     Text("Delete", color = if(isProtected) Color.Gray.copy(alpha = 0.5f) else FinosCoral)
                 }
             } else {
                 TextButton(onClick = onDismiss) {
                     Text("Cancel")
                 }
             }
        }
    )
}
