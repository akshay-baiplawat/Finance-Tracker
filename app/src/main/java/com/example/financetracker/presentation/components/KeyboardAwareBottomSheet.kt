package com.example.financetracker.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Checks if the software keyboard is currently visible.
 */
@Composable
fun isKeyboardVisible(): Boolean {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    return imeBottom > 0
}

/**
 * Tracks keyboard visibility with debounce to handle animation race conditions.
 * Returns true if keyboard is visible OR was visible within the debounce window.
 *
 * This solves the race condition where WindowInsets.ime reports 0 during the
 * keyboard close animation, causing dismiss logic to incorrectly think the
 * keyboard is already closed.
 *
 * @param debounceMs Duration in milliseconds to consider keyboard "probably visible"
 *                   after it starts closing. Default 150ms covers typical animation.
 */
@Composable
fun rememberKeyboardState(debounceMs: Long = 150L): Boolean {
    val isCurrentlyVisible = isKeyboardVisible()
    var wasRecentlyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyVisible) {
        if (isCurrentlyVisible) {
            wasRecentlyVisible = true
        } else {
            delay(debounceMs)
            wasRecentlyVisible = false
        }
    }

    return isCurrentlyVisible || wasRecentlyVisible
}

/**
 * Creates a keyboard-aware onClick handler for header back/close buttons.
 * When keyboard is visible, hides keyboard instead of dismissing.
 * When keyboard is hidden, calls onDismiss.
 *
 * Uses debounced keyboard state to handle animation race conditions, and
 * rememberUpdatedState to ensure the lambda always has access to the
 * latest keyboard state, avoiding stale closure issues.
 */
@Composable
fun rememberKeyboardAwareOnClick(onDismiss: () -> Unit): () -> Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isKeyboardProbablyVisible = rememberKeyboardState()

    // Use rememberUpdatedState to always have the latest values in the lambda
    val currentIsKeyboardOpen by rememberUpdatedState(isKeyboardProbablyVisible)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    return remember {
        {
            if (currentIsKeyboardOpen) {
                keyboardController?.hide()
                focusManager.clearFocus()
            } else {
                currentOnDismiss()
            }
        }
    }
}

/**
 * Standalone BackHandler that intercepts back press when keyboard is visible.
 * Uses debounced state and is always enabled to intercept before ModalBottomSheet.
 *
 * @param onDismiss Called when keyboard is not visible and back is pressed
 */
@Composable
fun KeyboardAwareBackHandler(onDismiss: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isKeyboardProbablyVisible = rememberKeyboardState()

    // Always enabled to intercept back before ModalBottomSheet's internal handler
    BackHandler(enabled = true) {
        if (isKeyboardProbablyVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onDismiss()
        }
    }
}

/**
 * A custom keyboard-aware bottom sheet built using Dialog.
 *
 * This implementation gives us FULL control over back press handling, unlike
 * Material3's ModalBottomSheet which has internal back handling we cannot override.
 *
 * Features:
 * - System back button: Hides keyboard first, then dismisses on second press
 * - Scrim tap: Hides keyboard first, then dismisses on second tap
 * - Swipe down gesture: Dismisses sheet (with threshold)
 * - Smooth slide-in/slide-out animations
 *
 * Header button handling should use rememberKeyboardAwareOnClick().
 *
 * @param sheetState Ignored - kept for API compatibility with Material3 ModalBottomSheet.
 *                   Use rememberModalBottomSheetState() at call sites for consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardAwareModalBottomSheet(
    onDismissRequest: () -> Unit,
    @Suppress("UNUSED_PARAMETER") sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    @Suppress("UNUSED_PARAMETER") dragHandle: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isKeyboardProbablyVisible = rememberKeyboardState()

    val currentIsKeyboardOpen by rememberUpdatedState(isKeyboardProbablyVisible)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    // Track visibility for animation
    var isVisible by remember { mutableStateOf(false) }

    // Track drag offset for swipe-to-dismiss
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 200f // pixels to trigger dismiss

    // Trigger entrance animation
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Keyboard-aware dismiss handler
    val handleDismissAttempt: () -> Unit = remember {
        {
            if (currentIsKeyboardOpen) {
                keyboardController?.hide()
                focusManager.clearFocus()
            } else {
                currentOnDismissRequest()
            }
        }
    }

    // Direct dismiss (bypasses keyboard check - for swipe gesture)
    val handleDirectDismiss: () -> Unit = remember {
        {
            // Hide keyboard first if open, then dismiss
            if (currentIsKeyboardOpen) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            currentOnDismissRequest()
        }
    }

    // Capture nav bar height BEFORE Dialog
    // WindowInsets may not work reliably inside Dialog's separate window
    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density)

    Dialog(
        onDismissRequest = { /* We handle dismiss ourselves via BackHandler */ },
        properties = DialogProperties(
            dismissOnBackPress = false, // We handle back press ourselves
            dismissOnClickOutside = false, // We handle scrim tap ourselves
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // BackHandler with FULL control - no Material3 interference
        BackHandler(enabled = true) {
            handleDismissAttempt()
        }

        // Calculate max sheet height - leave room for status bar
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        val maxSheetHeight = screenHeightDp * 0.92f  // 92% of screen height max

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Scrim tap - keyboard aware
                    handleDismissAttempt()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(0)) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset > swipeThreshold) {
                                        // Swipe threshold exceeded - dismiss directly
                                        handleDirectDismiss()
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragOffset = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    // Only allow dragging down (positive dragAmount)
                                    dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Consume clicks on sheet content (don't dismiss)
                        },
                    shape = shape,
                    color = containerColor,
                    shadowElevation = 8.dp
                ) {
                    // Use the navBarHeight captured BEFORE Dialog
                    // (WindowInsets may return 0 inside Dialog's separate window)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = with(density) { navBarHeight.toDp() })
                            .imePadding()
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
