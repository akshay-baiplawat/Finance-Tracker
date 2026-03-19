package com.example.financetracker.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utility object for mapping category iconIds to Material Icons.
 * Provides consistent icon handling across the app.
 */
object CategoryIconUtil {

    /**
     * Icon data class for picker display
     */
    data class IconItem(
        val id: Int,
        val icon: ImageVector,
        val name: String
    )

    /**
     * All available icons for the picker (used for user-created categories)
     */
    val availableIcons: List<IconItem> = listOf(
        // Default & General
        IconItem(0, Icons.Rounded.Receipt, "Receipt"),
        IconItem(1, Icons.Rounded.Restaurant, "Food"),
        IconItem(2, Icons.Rounded.ShoppingCart, "Shopping"),
        IconItem(3, Icons.Rounded.DirectionsCar, "Transport"),
        IconItem(4, Icons.Rounded.ReceiptLong, "Bills"),
        IconItem(5, Icons.Rounded.Movie, "Entertainment"),
        IconItem(6, Icons.Rounded.LocalHospital, "Health"),
        IconItem(7, Icons.Rounded.Flight, "Trip"),
        IconItem(8, Icons.Rounded.SwapHoriz, "Transfer"),
        IconItem(9, Icons.Rounded.AccountBalance, "Salary"),
        IconItem(10, Icons.Rounded.Payments, "Payment"),

        // Additional icons for user selection
        IconItem(11, Icons.Rounded.Home, "Home"),
        IconItem(12, Icons.Rounded.School, "Education"),
        IconItem(13, Icons.Rounded.FitnessCenter, "Fitness"),
        IconItem(14, Icons.Rounded.Pets, "Pets"),
        IconItem(15, Icons.Rounded.ChildCare, "Kids"),
        IconItem(16, Icons.Rounded.CardGiftcard, "Gifts"),
        IconItem(17, Icons.Rounded.LocalGroceryStore, "Groceries"),
        IconItem(18, Icons.Rounded.LocalCafe, "Cafe"),
        IconItem(19, Icons.Rounded.LocalBar, "Bar"),
        IconItem(20, Icons.Rounded.Checkroom, "Clothing"),
        IconItem(21, Icons.Rounded.Devices, "Electronics"),
        IconItem(22, Icons.Rounded.Build, "Repairs"),
        IconItem(23, Icons.Rounded.Subscriptions, "Subscriptions"),
        IconItem(24, Icons.Rounded.SportsEsports, "Gaming"),
        IconItem(25, Icons.Rounded.MusicNote, "Music"),
        IconItem(26, Icons.Rounded.LocalParking, "Parking"),
        IconItem(27, Icons.Rounded.LocalGasStation, "Fuel"),
        IconItem(28, Icons.Rounded.Savings, "Savings"),
        IconItem(29, Icons.Rounded.Work, "Work"),
        IconItem(30, Icons.Rounded.VolunteerActivism, "Charity")
    )

    /**
     * Get icon by iconId
     */
    fun getIconById(iconId: Int): ImageVector {
        return availableIcons.find { it.id == iconId }?.icon ?: Icons.Rounded.Receipt
    }

    /**
     * Get default iconId for system category names
     */
    fun getDefaultIconIdForCategory(name: String): Int {
        return when (name.lowercase()) {
            "uncategorized" -> 0
            "food & dining", "food" -> 1
            "shopping" -> 2
            "transportation", "transport" -> 3
            "bills & utilities", "bills" -> 4
            "entertainment" -> 5
            "health & wellness", "health" -> 6
            "trip" -> 7
            "money transfer", "transfer" -> 8
            "salary" -> 9
            "money received", "income" -> 10
            "investment" -> 28
            else -> 0 // Default to Receipt
        }
    }

    /**
     * Get icon for a category - uses iconId if set, otherwise falls back to name-based mapping
     */
    fun getIconForCategory(name: String, iconId: Int): ImageVector {
        return if (iconId > 0) {
            getIconById(iconId)
        } else {
            getIconById(getDefaultIconIdForCategory(name))
        }
    }
}
