package com.anzu.anyllm.data.repository

import com.anzu.anyllm.data.database.ProfileDao
import com.anzu.anyllm.data.entity.ProfileEntity
import com.anzu.anyllm.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles().map { entities ->
            entities.map { it.toProfile() }
        }
    }

    suspend fun getProfileById(id: String): Profile? {
        return profileDao.getProfileById(id)?.toProfile()
    }

    suspend fun insertProfile(profile: Profile) {
        profileDao.insertProfile(ProfileEntity.fromProfile(profile))
    }

    suspend fun updateProfile(profile: Profile) {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        profileDao.updateProfile(ProfileEntity.fromProfile(updated))
    }

    suspend fun saveProfile(profile: Profile) {
        val existing = profileDao.getProfileById(profile.id)
        if (existing != null) {
            val updated = profile.copy(updatedAt = System.currentTimeMillis())
            profileDao.updateProfile(ProfileEntity.fromProfile(updated))
        } else {
            profileDao.insertProfile(ProfileEntity.fromProfile(profile))
        }
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(ProfileEntity.fromProfile(profile))
    }

    suspend fun deleteProfileById(id: String) {
        profileDao.deleteProfileById(id)
    }
}

