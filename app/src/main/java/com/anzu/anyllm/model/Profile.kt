package com.anzu.anyllm.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val bodyTemplate: String,
    val responseJsonPath: String,
    val streamEnabled: Boolean = false,
    val streamJsonPath: String? = null,
    val timeoutSeconds: Int = 60,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
