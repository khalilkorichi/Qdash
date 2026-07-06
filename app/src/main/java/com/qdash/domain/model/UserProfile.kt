package com.qdash.domain.model

data class UserProfile(
    val id: Long = 1,
    val name: String = "ضيف قداشّ",
    val email: String? = null,
    val birthDate: String? = null,
    val avatarUrl: String? = null,
    val isGoogleLinked: Boolean = false
)
