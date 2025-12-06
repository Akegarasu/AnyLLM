package com.anzu.anyllm.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_LAST_PROFILE_ID = stringPreferencesKey("last_profile_id")
        private val KEY_LAST_SESSION_ID = stringPreferencesKey("last_session_id")
        private const val KEY_API_KEY_PREFIX = "api_key_"
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
        }
    }

    val lastProfileId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_PROFILE_ID]
    }

    suspend fun setLastProfileId(profileId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_PROFILE_ID] = profileId
        }
    }

    val lastSessionId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SESSION_ID]
    }

    suspend fun setLastSessionId(sessionId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SESSION_ID] = sessionId
        }
    }

    fun saveApiKey(profileId: String, apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY_PREFIX + profileId, apiKey).apply()
    }

    fun getApiKey(profileId: String): String? {
        return encryptedPrefs.getString(KEY_API_KEY_PREFIX + profileId, null)
    }

    fun deleteApiKey(profileId: String) {
        encryptedPrefs.edit().remove(KEY_API_KEY_PREFIX + profileId).apply()
    }
}
