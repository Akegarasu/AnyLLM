package com.anzu.anyllm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anzu.anyllm.model.Profile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val method: String,
    val headersJson: String,        // JSON 序列化的 Map
    val bodyTemplate: String,
    val responseJsonPath: String,
    val streamEnabled: Boolean,
    val streamJsonPath: String?,
    val timeoutSeconds: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toProfile(): Profile {
        val headers: Map<String, String> = try {
            Json.decodeFromString(headersJson)
        } catch (e: Exception) {
            emptyMap()
        }
        return Profile(
            id = id,
            name = name,
            baseUrl = baseUrl,
            method = method,
            headers = headers,
            bodyTemplate = bodyTemplate,
            responseJsonPath = responseJsonPath,
            streamEnabled = streamEnabled,
            streamJsonPath = streamJsonPath,
            timeoutSeconds = timeoutSeconds,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromProfile(profile: Profile): ProfileEntity {
            return ProfileEntity(
                id = profile.id,
                name = profile.name,
                baseUrl = profile.baseUrl,
                method = profile.method,
                headersJson = Json.encodeToString(profile.headers),
                bodyTemplate = profile.bodyTemplate,
                responseJsonPath = profile.responseJsonPath,
                streamEnabled = profile.streamEnabled,
                streamJsonPath = profile.streamJsonPath,
                timeoutSeconds = profile.timeoutSeconds,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}

