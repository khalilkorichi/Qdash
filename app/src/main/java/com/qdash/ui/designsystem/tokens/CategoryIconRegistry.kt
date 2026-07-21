package com.qdash.ui.designsystem.tokens

import com.qdash.R

/**
 * Isolated icon registry mapping category tokens to Android Vector Drawable resource IDs
 * and standard material design icon names.
 */
object CategoryIconRegistry {

    data class IconResourceToken(
        val key: String,
        val labelAr: String,
        val drawableResId: Int? = null,
        val isVectorDrawable: Boolean = false
    )

    val registeredIcons: List<IconResourceToken> = listOf(
        IconResourceToken("ic_cat_marche", "سوق وخضرة", R.drawable.ic_cat_marche, true),
        IconResourceToken("ic_cat_flexy", "فليكسي وشحن", R.drawable.ic_cat_flexy, true),
        IconResourceToken("ic_cat_transport", "مواصلات وتاكسي", R.drawable.ic_cat_transport, true),
        IconResourceToken("ic_cat_health", "صحة وصيدلية", R.drawable.ic_cat_health, true),
        IconResourceToken("ic_cat_fastfood", "أكل سريع وبلاطو", R.drawable.ic_cat_fastfood, true),
        IconResourceToken("ic_cat_bricolage", "خردوات ودروغري", R.drawable.ic_cat_bricolage, true),
        IconResourceToken("ic_cat_education", "تعليم ودروس", R.drawable.ic_cat_education, true),
        IconResourceToken("ic_cat_family", "عائلة وكفالة", R.drawable.ic_cat_family, true),
        IconResourceToken("ic_cat_default", "فئة مخصصة افتراضية", R.drawable.ic_cat_default, true),

        // Standard icon tokens fallback
        IconResourceToken("person", "شخصي"),
        IconResourceToken("home", "منزلي"),
        IconResourceToken("restaurant", "مطاعم"),
        IconResourceToken("shopping_bag", "تسوق"),
        IconResourceToken("work", "عمل وراتب"),
        IconResourceToken("sports_esports", "ترفيه"),
        IconResourceToken("celebration", "مناسبات"),
        IconResourceToken("account_balance", "البريد وCCP")
    )

    val DEFAULT_ICON_KEY = "ic_cat_default"

    fun getDrawableResId(key: String): Int? {
        val token = registeredIcons.find { it.key.equals(key, ignoreCase = true) }
        return token?.drawableResId ?: R.drawable.ic_cat_default
    }

    fun isCustomVector(key: String): Boolean {
        return registeredIcons.any { it.key.equals(key, ignoreCase = true) && it.isVectorDrawable }
    }
}
