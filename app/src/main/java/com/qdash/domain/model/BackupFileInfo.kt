package com.qdash.domain.model

import com.qdash.domain.model.common.Nameable

data class BackupFileInfo(
    override val /* contract */ name: String,
    val sizeBytes: Long,
    val path: String
) : Nameable
