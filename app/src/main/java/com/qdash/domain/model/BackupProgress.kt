package com.qdash.domain.model

sealed class BackupProgress {
    object Idle : BackupProgress()
    data class Running(val stage: String, val progressPercent: Int) : BackupProgress()
    object Success : BackupProgress()
    data class Failure(val error: String) : BackupProgress()
}
