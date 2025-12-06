package com.anzu.anyllm.data.repository

import com.anzu.anyllm.data.database.MessageDao
import com.anzu.anyllm.data.entity.MessageEntity
import com.anzu.anyllm.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    fun getMessagesBySessionId(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessageById(id)?.toMessage()
    }

    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(MessageEntity.fromMessage(message))
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(MessageEntity.fromMessage(message))
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(MessageEntity.fromMessage(message))
    }

    suspend fun deleteMessageById(id: String) {
        messageDao.deleteMessageById(id)
    }

    suspend fun deleteMessagesBySessionId(sessionId: String) {
        messageDao.deleteMessagesBySessionId(sessionId)
    }

    suspend fun getRecentMessages(sessionId: String, limit: Int): List<Message> {
        return messageDao.getRecentMessages(sessionId, limit).map { it.toMessage() }.reversed()
    }
}

