package com.anzu.anyllm.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anzu.anyllm.model.Message
import com.anzu.anyllm.model.Role
import com.anzu.anyllm.model.Status

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val status: String,
    val timestamp: Long,
    val tokenCount: Int?
) {
    fun toMessage(): Message {
        return Message(
            id = id,
            sessionId = sessionId,
            role = Role.valueOf(role),
            content = content,
            status = Status.valueOf(status),
            timestamp = timestamp,
            tokenCount = tokenCount
        )
    }

    companion object {
        fun fromMessage(message: Message): MessageEntity {
            return MessageEntity(
                id = message.id,
                sessionId = message.sessionId,
                role = message.role.name,
                content = message.content,
                status = message.status.name,
                timestamp = message.timestamp,
                tokenCount = message.tokenCount
            )
        }
    }
}

