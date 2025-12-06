package com.anzu.anyllm.model

import java.util.UUID

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val title: String,
    val systemPrompt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
