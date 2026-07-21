package com.qdash.presentation.components

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
        "celebration" -> Icons.Default.Celebration
        "mail" -> Icons.Default.Mail
        "backpack" -> Icons.Default.Backpack
        "pets" -> Icons.Default.Pets
        "nights_stay" -> Icons.Default.NightsStay
        "cake" -> Icons.Default.Cake
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "house" -> Icons.Default.House
        "bolt" -> Icons.Default.Bolt
        "water_drop" -> Icons.Default.WaterDrop
        "wifi" -> Icons.Default.Wifi
        "chair" -> Icons.Default.Chair
        "shopping_cart" -> Icons.Default.ShoppingCart
        "coffee" -> Icons.Default.Coffee
        "local_gas_station" -> Icons.Default.LocalGasStation
        "local_taxi" -> Icons.Default.LocalTaxi
        "directions_bus" -> Icons.Default.DirectionsBus
        "checkroom" -> Icons.Default.Checkroom
        "spa" -> Icons.Default.Spa
        "child_care" -> Icons.Default.ChildCare
        "ic_cat_marche", "shopping_cart" -> Icons.Default.ShoppingCart
        "ic_cat_flexy", "phone", "wifi" -> Icons.Default.Wifi
        "ic_cat_transport", "local_taxi", "transport", "car" -> Icons.Default.DirectionsCar
        "ic_cat_health", "medical_services", "health", "local_pharmacy" -> Icons.Default.MedicalServices
        "ic_cat_fastfood", "fastfood", "restaurant", "food" -> Icons.Default.Restaurant
        "ic_cat_bricolage", "build" -> Icons.Default.Build
        "ic_cat_education", "school", "education" -> Icons.Default.School
        "ic_cat_family", "groups", "family" -> Icons.Default.Groups
        "ic_cat_default", "category", "folder" -> Icons.Default.Category
        else -> Icons.Default.Category
    }
}
