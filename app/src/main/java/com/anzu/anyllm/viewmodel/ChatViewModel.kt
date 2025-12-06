package com.anzu.anyllm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anzu.anyllm.data.preferences.AppPreferences
import com.anzu.anyllm.data.repository.MessageRepository
import com.anzu.anyllm.data.repository.ProfileRepository
import com.anzu.anyllm.data.repository.SessionRepository
import com.anzu.anyllm.model.Message
import com.anzu.anyllm.model.Profile
import com.anzu.anyllm.model.Role
import com.anzu.anyllm.model.Session
import com.anzu.anyllm.model.Status
import com.anzu.anyllm.network.LLMClient
import com.anzu.anyllm.network.LLMResult
import com.anzu.anyllm.network.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val appPreferences: AppPreferences,
    private val llmClient: LLMClient
) : ViewModel() {

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private var streamingJob: Job? = null
    private var sessionsJob: Job? = null

    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _currentProfile.value = profileRepository.getProfileById(profileId)
            loadSessions(profileId)
        }
    }

    private fun loadSessions(profileId: String) {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            sessionRepository.getSessionsByProfileId(profileId)
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
                .collect { sessionList ->
                    _sessions.value = sessionList
                    if (_currentSession.value == null && sessionList.isNotEmpty()) {
                        loadSession(sessionList.first().id)
                    }
                }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            _currentSession.value = session
            if (session != null) {
                messageRepository.getMessagesBySessionId(session.id).collect { messageList ->
                    _messages.value = messageList
                }
            }
        }
    }

    fun createSession(title: String, systemPrompt: String? = null) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            val session = Session(
                profileId = profile.id,
                title = title,
                systemPrompt = systemPrompt
            )
            sessionRepository.insertSession(session)
            loadSession(session.id)
        }
    }

    fun updateSessionTitle(title: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val updated = session.copy(title = title)
            sessionRepository.updateSession(updated)
            _currentSession.value = updated
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
            if (_currentSession.value?.id == session.id) {
                _currentSession.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun sendMessage(content: String) {
        val profile = _currentProfile.value ?: return
        val session = _currentSession.value ?: return

        viewModelScope.launch {
            val userMessage = Message(
                sessionId = session.id,
                role = Role.USER,
                content = content
            )
            messageRepository.insertMessage(userMessage)

            val assistantMessage = Message(
                sessionId = session.id,
                role = Role.ASSISTANT,
                content = "",
                status = Status.SENDING
            )
            messageRepository.insertMessage(assistantMessage)

            _isLoading.value = true

            val allMessages = buildMessageList(session)
            val apiKey = appPreferences.getApiKey(profile.id) ?: ""
            val variables = mapOf("api_key" to apiKey)

            if (profile.streamEnabled) {
                sendStreamingMessage(profile, allMessages, variables, assistantMessage)
            } else {
                sendNormalMessage(profile, allMessages, variables, assistantMessage)
            }
        }
    }

    private suspend fun buildMessageList(session: Session): List<Message> {
        val messages = _messages.value.toMutableList()

        if (!session.systemPrompt.isNullOrBlank()) {
            messages.add(
                0,
                Message(
                    sessionId = session.id,
                    role = Role.SYSTEM,
                    content = session.systemPrompt
                )
            )
        }

        return messages
    }

    private suspend fun sendNormalMessage(
        profile: Profile,
        messages: List<Message>,
        variables: Map<String, String>,
        assistantMessage: Message
    ) {
        when (val result = llmClient.sendRequest(profile, messages, variables)) {
            is LLMResult.Success -> {
                val updated = assistantMessage.copy(
                    content = result.content,
                    status = Status.COMPLETED
                )
                messageRepository.updateMessage(updated)
            }
            is LLMResult.Error -> {
                val updated = assistantMessage.copy(
                    content = result.message,
                    status = Status.ERROR
                )
                messageRepository.updateMessage(updated)
            }
        }
        _isLoading.value = false
        sessionRepository.updateSessionTimestamp(_currentSession.value?.id ?: "")
    }

    private fun sendStreamingMessage(
        profile: Profile,
        messages: List<Message>,
        variables: Map<String, String>,
        assistantMessage: Message
    ) {
        streamingJob?.cancel()
        _streamingContent.value = ""

        streamingJob = viewModelScope.launch {
            var accumulatedContent = ""
            val updatedMessage = assistantMessage.copy(status = Status.STREAMING)
            messageRepository.updateMessage(updatedMessage)

            llmClient.sendStreamRequest(profile, messages, variables).collect { result ->
                when (result) {
                    is StreamResult.Started -> {}
                    is StreamResult.Delta -> {
                        accumulatedContent += result.content
                        _streamingContent.value = accumulatedContent
                    }
                    is StreamResult.Completed -> {
                        val completed = updatedMessage.copy(
                            content = accumulatedContent,
                            status = Status.COMPLETED
                        )
                        messageRepository.updateMessage(completed)
                        _streamingContent.value = ""
                        _isLoading.value = false
                    }
                    is StreamResult.Error -> {
                        val error = updatedMessage.copy(
                            content = result.message,
                            status = Status.ERROR
                        )
                        messageRepository.updateMessage(error)
                        _streamingContent.value = ""
                        _isLoading.value = false
                    }
                }
            }
            sessionRepository.updateSessionTimestamp(_currentSession.value?.id ?: "")
        }
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _isLoading.value = false
        _streamingContent.value = ""
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.deleteMessage(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}
