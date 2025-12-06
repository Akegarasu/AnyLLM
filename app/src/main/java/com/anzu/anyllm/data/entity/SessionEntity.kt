package com.anzu.anyllm.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anzu.anyllm.model.Session

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val title: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toSession(): Session {
        return Session(
            id = id,
            profileId = profileId,
            title = title,
            systemPrompt = systemPrompt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromSession(session: Session): SessionEntity {
            return SessionEntity(
                id = session.id,
                profileId = session.profileId,
                title = session.title,
                systemPrompt = session.systemPrompt,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        }
    }
}

