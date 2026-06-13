package com.example.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconByName(name: String): ImageVector {
    return when (name.lowercase()) {
        "person" -> Icons.Default.Person
        "groups", "family" -> Icons.Default.Groups
        "home" -> Icons.Default.Home
        "restaurant", "food" -> Icons.Default.Restaurant
        "directions_car", "transport", "car" -> Icons.Default.DirectionsCar
        "receipt_long", "bills", "invoice" -> Icons.Default.ReceiptLong
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "medical_services", "health" -> Icons.Default.MedicalServices
        "school", "education" -> Icons.Default.School
        "sports_esports", "entertainment" -> Icons.Default.SportsEsports
        "work", "salary" -> Icons.Default.Work
        "monetization_on", "money", "other" -> Icons.Default.MonetizationOn
        "savings" -> Icons.Default.Savings
        "alarm" -> Icons.Default.Alarm
        "star" -> Icons.Default.Star
        "local_mall" -> Icons.Default.LocalMall
        "flight", "travel" -> Icons.Default.Flight
        else -> Icons.Default.AttachMoney
    }
}
