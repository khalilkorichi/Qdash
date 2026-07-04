package com.qdash.domain.model

sealed class AiFailureException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkFailure(message: String, cause: Throwable? = null) : AiFailureException(message, cause)
    class AiServiceFailure(message: String, cause: Throwable? = null) : AiFailureException(message, cause)
}
