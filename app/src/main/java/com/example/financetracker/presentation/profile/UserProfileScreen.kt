package com.example.financetracker.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState(initial = "")
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val userGender by viewModel.userGender.collectAsState(initial = "")
    val userDescription by viewModel.userDescription.collectAsState(initial = "")
    val userDateOfBirth by viewModel.userDateOfBirth.collectAsState(initial = "")
    val userImagePath by viewModel.userImagePath.collectAsState(initial = null)
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    LaunchedEffect(userName) { if (nameText.isEmpty() && userName.isNotEmpty()) nameText = userName }
    LaunchedEffect(userEmail) { if (emailText.isEmpty() && userEmail.isNotEmpty()) emailText = userEmail }
    LaunchedEffect(userDescription) { if (descriptionText.isEmpty() && userDescription.isNotEmpty()) descriptionText = userDescription }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val internalPath = copyImageToInternalStorage(context, it)
            internalPath?.let { path -> viewModel.updateUserImage(path) }
        }
    }

    val isDark = isDarkTheme()

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (userDateOfBirth.isNotEmpty()) {
                try {
                    LocalDate.parse(userDateOfBirth, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    AppLogger.w("UserProfileScreen", "Failed to parse date of birth: $userDateOfBirth", e)
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            viewModel.updateDateOfBirth(formatted)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = FinosMint)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = FinosMint,
                    todayDateBorderColor = FinosMint
                )
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                ProfilePhotoSection(
                    imagePath = userImagePath,
                    userName = nameText,
                    onChangePhoto = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeletePhoto = { viewModel.clearUserImage() }
                )
            }

            item {
                ProfileTextField(
                    label = "Name",
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        viewModel.updateName(it)
                    },
                    placeholder = "Enter your name",
                    isDark = isDark
                )
            }

            item {
                ProfileTextField(
                    label = "Email",
                    value = emailText,
                    onValueChange = { newEmail ->
                        emailText = newEmail
                        emailError = if (newEmail.isNotEmpty() && !isValidEmail(newEmail)) {
                            "Please enter a valid email"
                        } else {
                            if (newEmail.isEmpty() || isValidEmail(newEmail)) {
                                viewModel.updateEmail(newEmail)
                            }
                            null
                        }
                    },
                    placeholder = "Enter your email",
                    keyboardType = KeyboardType.Email,
                    isDark = isDark,
                    isError = emailError != null,
                    errorMessage = emailError
                )
            }

            item {
                DateOfBirthField(
                    dateOfBirth = userDateOfBirth,
                    onClick = { showDatePicker = true },
                    isDark = isDark
                )
            }

            item {
                GenderDropdown(
                    selectedGender = userGender,
                    onGenderSelected = { viewModel.updateGender(it) },
                    isDark = isDark
                )
            }

            item {
                ProfileTextField(
                    label = "About Me",
                    value = descriptionText,
                    onValueChange = {
                        descriptionText = it
                        viewModel.updateDescription(it)
                    },
                    placeholder = "Tell us about yourself...",
                    singleLine = false,
                    maxLines = 4,
                    minHeight = 120.dp,
                    isDark = isDark
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@Composable
private fun ProfilePhotoSection(
    imagePath: String?,
    userName: String,
    onChangePhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(FinosMint, CircleShape)
                    .clickable { onChangePhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = userName
                        .split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "?" }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, FinosMint, CircleShape)
                    .clickable { onChangePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CameraAlt,
                    contentDescription = "Change Photo",
                    tint = FinosMint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onChangePhoto) {
                Text("Change Photo", color = FinosMint)
            }
            if (imagePath != null) {
                TextButton(onClick = onDeletePhoto) {
                    Text("Remove Photo", color = FinosCoral)
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minHeight: Dp = 56.dp,
    isDark: Boolean,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val borderColor = when {
        isError -> FinosCoral
        isDark -> DarkSurfaceHighlight
        else -> Color.LightGray.copy(alpha = 0.5f)
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondaryDark) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = borderColor,
                focusedBorderColor = if (isError) FinosCoral else FinosMint,
                errorBorderColor = FinosCoral,
                cursorColor = FinosMint
            )
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = FinosCoral
            )
        }
    }
}

@Composable
private fun DateOfBirthField(
    dateOfBirth: String,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val borderColor = if (isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha = 0.5f)

    Column {
        Text(
            text = "Date of Birth",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateOfBirth.ifEmpty { "Select date of birth" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Rounded.CalendarToday,
                    contentDescription = "Select Date",
                    tint = FinosMint
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = borderColor,
                disabledTrailingIconColor = FinosMint
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Male", "Female", "Other", "Prefer not to say")
    val borderColor = if (isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha = 0.5f)

    Column {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedGender.ifEmpty { "Select Gender" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = borderColor,
                    focusedBorderColor = FinosMint
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onGenderSelected(option)
                            expanded = false
                        },
                        leadingIcon = if (option == selectedGender) {
                            { Icon(Icons.Rounded.Check, null, tint = FinosMint) }
                        } else null
                    )
                }
            }
        }
    }
}

private fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        AppLogger.e("UserProfileScreen", "Failed to copy profile image to internal storage", e)
        null
    }
}
