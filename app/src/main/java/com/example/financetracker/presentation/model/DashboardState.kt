package com.example.financetracker.presentation.model

data class DashboardState(
    val income: String = "₹0.00",
    val expense: String = "₹0.00",
    val balance: String = "₹0.00"
)