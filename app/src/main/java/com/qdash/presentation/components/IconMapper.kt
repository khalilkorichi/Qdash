package com.qdash.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.CategoryIconRegistry

fun safeParseColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: Exception) {
        fallback
    }
}

fun isEmojiToken(token: String?): Boolean {
    if (token.isNullOrBlank()) return false
    val trimmed = token.trim()
    return trimmed.length <= 8 && trimmed.any { it.code > 0x06FF }
}

@Composable
fun UnifiedCategoryIcon(
    iconKey: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    emojiFontSize: TextUnit = 18.sp,
    contentDescription: String? = null
) {
    val key = iconKey?.trim().orEmpty()
    when {
        key.isEmpty() -> {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
        CategoryIconRegistry.isCustomVector(key) -> {
            val resId = CategoryIconRegistry.getDrawableResId(key)
            if (resId != null) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = modifier
                )
            } else {
                Icon(
                    imageVector = getIconByName(key),
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = modifier
                )
            }
        }
        isEmojiToken(key) -> {
            Text(
                text = key,
                fontSize = emojiFontSize
            )
        }
        else -> {
            Icon(
                imageVector = getIconByName(key),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

fun getIconByName(name: String): ImageVector {
    return when (name.trim().lowercase()) {
        "person" -> Icons.Default.Person
        "groups", "family", "ic_cat_family" -> Icons.Default.Groups
        "home" -> Icons.Default.Home
        "restaurant", "food", "fastfood", "ic_cat_fastfood" -> Icons.Default.Restaurant
        "directions_car", "transport", "car", "local_taxi", "ic_cat_transport" -> Icons.Default.DirectionsCar
        "receipt_long", "bills", "invoice", "receipt" -> Icons.Default.ReceiptLong
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "medical_services", "health", "local_pharmacy", "ic_cat_health" -> Icons.Default.MedicalServices
        "school", "education", "ic_cat_education" -> Icons.Default.School
        "sports_esports", "entertainment", "gamepad" -> Icons.Default.SportsEsports
        "work", "salary" -> Icons.Default.Work
        "monetization_on", "money", "other", "payments" -> Icons.Default.Payments
        "savings" -> Icons.Default.Savings
        "alarm", "schedule" -> Icons.Default.Schedule
        "star" -> Icons.Default.Star
        "local_mall", "storefront" -> Icons.Default.Storefront
        "redeem", "card_giftcard" -> Icons.Default.CardGiftcard
        "flight", "travel" -> Icons.Default.Flight
        "celebration" -> Icons.Default.Celebration
        "mail" -> Icons.Default.Mail
        "backpack" -> Icons.Default.Backpack
        "pets" -> Icons.Default.Pets
        "nights_stay" -> Icons.Default.NightsStay
        "cake" -> Icons.Default.Cake
        "account_balance" -> Icons.Default.AccountBalance
        "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
        "credit_card" -> Icons.Default.CreditCard
        "house" -> Icons.Default.House
        "bolt" -> Icons.Default.Bolt
        "water_drop" -> Icons.Default.WaterDrop
        "wifi", "phone", "ic_cat_flexy" -> Icons.Default.Wifi
        "chair" -> Icons.Default.Chair
        "shopping_cart", "ic_cat_marche" -> Icons.Default.ShoppingCart
        "coffee" -> Icons.Default.Coffee
        "local_gas_station" -> Icons.Default.LocalGasStation
        "directions_bus" -> Icons.Default.DirectionsBus
        "checkroom" -> Icons.Default.Checkroom
        "spa" -> Icons.Default.Spa
        "child_care" -> Icons.Default.ChildCare
        "build", "ic_cat_bricolage", "handyman" -> Icons.Default.Build
        "trending_up", "insights" -> Icons.Default.TrendingUp
        "sync_alt", "swap_horiz", "transfer" -> Icons.Default.SyncAlt
        "volunteer_activism", "charity", "favorite" -> Icons.Default.VolunteerActivism
        "fitness_center", "sports", "gym" -> Icons.Default.FitnessCenter
        "local_shipping", "delivery" -> Icons.Default.LocalShipping
        "smartphone", "devices", "phone_android" -> Icons.Default.Smartphone
        "bakery_dining", "bakery" -> Icons.Default.BakeryDining
        "content_cut", "salon" -> Icons.Default.ContentCut
        "verified_user", "security", "shield" -> Icons.Default.VerifiedUser
        "description", "gavel", "tax" -> Icons.Default.Description
        "campaign", "ads" -> Icons.Default.Campaign
        "kitchen", "appliances" -> Icons.Default.Kitchen
        "apartment", "real_estate" -> Icons.Default.Apartment
        "Subscriptions", "subscriptions" -> Icons.Default.Subscriptions
        else -> Icons.Default.Category
    }
}
