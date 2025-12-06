package com.anzu.anyllm.data.repository

import com.anzu.anyllm.data.database.SessionDao
import com.anzu.anyllm.data.entity.SessionEntity
import com.anzu.anyllm.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    fun getSessionsByProfileId(profileId: String): Flow<List<Session>> {
        return sessionDao.getSessionsByProfileId(profileId).map { entities ->
            entities.map { it.toSession() }
        }
    }

    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toSession() }
        }
    }

    suspend fun getSessionById(id: String): Session? {
        return sessionDao.getSessionById(id)?.toSession()
    }

    suspend fun insertSession(session: Session) {
        sessionDao.insertSession(SessionEntity.fromSession(session))
    }

    suspend fun updateSession(session: Session) {
        val updated = session.copy(updatedAt = System.currentTimeMillis())
        sessionDao.updateSession(SessionEntity.fromSession(updated))
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(SessionEntity.fromSession(session))
    }

    suspend fun deleteSessionById(id: String) {
        sessionDao.deleteSessionById(id)
    }

    suspend fun updateSessionTimestamp(id: String) {
        sessionDao.updateSessionTimestamp(id, System.currentTimeMillis())
    }
}

