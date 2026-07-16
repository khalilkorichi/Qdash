package com.qdash.domain.model

import com.qdash.domain.model.common.Identifiable

data class UserProfile(
    override val /* contract */ id: Long = 1,
    val name: String = "ضيف قداشّ",
    val email: String? = null,
    val birthDate: String? = null,
    val avatarUrl: String? = null,
    val isGoogleLinked: Boolean = false
) : Identifiable
