package com.example.financetracker.presentation.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.financetracker.presentation.theme.FinosCoral

/**
 * Launches a side effect to show an error Snackbar when error is not null.
 * Call this composable inside your screen, passing the snackbarHostState used by Scaffold.
 *
 * @param error The error message to display, or null if no error
 * @param snackbarHostState The SnackbarHostState from your Scaffold
 * @param onErrorShown Callback to clear the error after it's displayed
 */
@Composable
fun ErrorSnackbarEffect(
    error: String?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit
) {
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            onErrorShown()
        }
    }
}

/**
 * A styled SnackbarHost for displaying error messages.
 * Use this in your Scaffold's snackbarHost parameter.
 *
 * @param hostState The SnackbarHostState to manage snackbar display
 */
@Composable
fun ErrorSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = FinosCoral.copy(alpha = 0.95f),
            contentColor = Color.White
        )
    }
}
