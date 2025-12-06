package com.anzu.anyllm.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: Role,
    val content: String,
    val status: Status = Status.COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int? = null
)

enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class Status {
    SENDING,
    STREAMING,
    COMPLETED,
    ERROR
}
