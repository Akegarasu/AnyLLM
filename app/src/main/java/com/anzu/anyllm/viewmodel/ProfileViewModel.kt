package com.anzu.anyllm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anzu.anyllm.data.preferences.AppPreferences
import com.anzu.anyllm.data.repository.ProfileRepository
import com.anzu.anyllm.model.Profile
import com.anzu.anyllm.network.LLMClient
import com.anzu.anyllm.network.LLMResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val appPreferences: AppPreferences,
    private val llmClient: LLMClient
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult: StateFlow<TestResult?> = _testResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }

    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _selectedProfile.value = profileRepository.getProfileById(profileId)
        }
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
            appPreferences.deleteApiKey(profile.id)
        }
    }

    fun saveApiKey(profileId: String, apiKey: String) {
        appPreferences.saveApiKey(profileId, apiKey)
    }

    fun getApiKey(profileId: String): String? {
        return appPreferences.getApiKey(profileId)
    }

    fun testProfile(profile: Profile, testMessage: String) {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null

            val apiKey = appPreferences.getApiKey(profile.id) ?: ""
            val variables = mapOf("api_key" to apiKey)

            when (val result = llmClient.testConnection(profile, testMessage, variables)) {
                is LLMResult.Success -> {
                    _testResult.value = TestResult.Success(result.content)
                }
                is LLMResult.Error -> {
                    _testResult.value = TestResult.Error(
                        message = result.message,
                        rawResponse = result.rawResponse
                    )
                }
            }
            _isTesting.value = false
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun importProfile(jsonString: String): Result<Profile> {
        return try {
            val profile = json.decodeFromString<Profile>(jsonString)
            viewModelScope.launch {
                profileRepository.insertProfile(profile)
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportProfile(profile: Profile): String {
        return json.encodeToString(Profile.serializer(), profile)
    }

    fun clearSelectedProfile() {
        _selectedProfile.value = null
    }
}

sealed class TestResult {
    data class Success(val content: String) : TestResult()
    data class Error(val message: String, val rawResponse: String? = null) : TestResult()
}

