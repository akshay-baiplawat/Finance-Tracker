package com.example.financetracker.presentation

import com.example.financetracker.presentation.model.DashboardUiState
import com.example.financetracker.presentation.model.WalletUiState
import com.example.financetracker.presentation.model.InsightsUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests to verify error state fields exist in UiState classes
 */
class ErrorStateTest {

    @Test
    fun `DashboardUiState has error field with null default`() {
        val state = DashboardUiState()
        assertNull("Error should be null by default", state.error)

        val stateWithError = state.copy(error = "Test error")
        assertEquals("Test error", stateWithError.error)
    }

    @Test
    fun `WalletUiState has error and isLoading fields`() {
        val state = WalletUiState()
        assertNull("Error should be null by default", state.error)
        assertTrue("isLoading should be true by default", state.isLoading)

        val stateWithError = state.copy(error = "Wallet error", isLoading = false)
        assertEquals("Wallet error", stateWithError.error)
        assertFalse(stateWithError.isLoading)
    }

    @Test
    fun `InsightsUiState has error and isLoading fields`() {
        val state = InsightsUiState()
        assertNull("Error should be null by default", state.error)
        assertTrue("isLoading should be true by default", state.isLoading)

        val stateWithError = state.copy(error = "Insights error", isLoading = false)
        assertEquals("Insights error", stateWithError.error)
        assertFalse(stateWithError.isLoading)
    }

    @Test
    fun `error can be cleared by setting to null`() {
        val state = DashboardUiState(error = "Some error")
        assertEquals("Some error", state.error)

        val clearedState = state.copy(error = null)
        assertNull(clearedState.error)
    }
}
