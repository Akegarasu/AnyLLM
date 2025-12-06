package com.anzu.anyllm.network

import com.anzu.anyllm.model.Message
import com.anzu.anyllm.model.Profile
import com.anzu.anyllm.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMClient @Inject constructor(
    private val templateEngine: TemplateEngine,
    private val responseParser: ResponseParser,
    private val sseHandler: SSEHandler
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendRequest(
        profile: Profile,
        messages: List<Message>,
        variables: Map<String, String>
    ): LLMResult = withContext(Dispatchers.IO) {
        try {
            val client = createClient(profile.timeoutSeconds)
            val request = buildRequest(profile, messages, variables)

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext LLMResult.Error(
                    code = response.code,
                    message = "HTTP ${response.code}: ${response.message}"
                )
            }

            val responseBody = response.body?.string() ?: return@withContext LLMResult.Error(
                code = -1,
                message = "Empty response body"
            )

            val content = responseParser.extractContent(responseBody, profile.responseJsonPath)
                ?: return@withContext LLMResult.Error(
                    code = -1,
                    message = "Failed to extract content from response",
                    rawResponse = responseBody
                )

            LLMResult.Success(content)
        } catch (e: Exception) {
            LLMResult.Error(
                code = -1,
                message = e.message ?: "Unknown error"
            )
        }
    }

    fun sendStreamRequest(
        profile: Profile,
        messages: List<Message>,
        variables: Map<String, String>
    ): Flow<StreamResult> = flow {
        val client = createClient(profile.timeoutSeconds)
        val request = buildRequest(profile, messages, variables)

        val jsonPath = profile.streamJsonPath ?: profile.responseJsonPath

        sseHandler.connect(request, jsonPath).collect { event ->
            when (event) {
                is SSEEvent.Connected -> emit(StreamResult.Started)
                is SSEEvent.Data -> emit(StreamResult.Delta(event.content))
                is SSEEvent.Done -> emit(StreamResult.Completed)
                is SSEEvent.Closed -> emit(StreamResult.Completed)
                is SSEEvent.Error -> emit(StreamResult.Error(event.message))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun testConnection(
        profile: Profile,
        testMessage: String,
        variables: Map<String, String>
    ): LLMResult {
        val testMessages = listOf(
            Message(
                id = "test",
                sessionId = "test",
                role = Role.USER,
                content = testMessage
            )
        )
        return sendRequest(profile, testMessages, variables)
    }

    private fun createClient(timeoutSeconds: Int): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }

    private fun buildRequest(
        profile: Profile,
        messages: List<Message>,
        variables: Map<String, String>
    ): Request {
        val messagesJson = buildMessagesJson(messages)

        val allVariables = variables.toMutableMap().apply {
            put("messages", messagesJson)
            put("stream", profile.streamEnabled.toString())
        }

        val body = templateEngine.render(profile.bodyTemplate, allVariables)

        val requestBuilder = Request.Builder()
            .url(profile.baseUrl)

        profile.headers.forEach { (key, value) ->
            val renderedValue = templateEngine.render(value, allVariables)
            requestBuilder.addHeader(key, renderedValue)
        }

        val requestBody = body.toRequestBody("application/json".toMediaType())
        when (profile.method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody)
            "PUT" -> requestBuilder.put(requestBody)
            "PATCH" -> requestBuilder.patch(requestBody)
            "DELETE" -> requestBuilder.delete(requestBody)
            else -> requestBuilder.post(requestBody)
        }

        return requestBuilder.build()
    }

    private fun buildMessagesJson(messages: List<Message>): String {
        val jsonArray = JsonArray(messages.map { message ->
            JsonObject(
                mapOf(
                    "role" to JsonPrimitive(message.role.name.lowercase()),
                    "content" to JsonPrimitive(message.content)
                )
            )
        })
        return json.encodeToString(JsonArray.serializer(), jsonArray)
    }
}

sealed class LLMResult {
    data class Success(val content: String) : LLMResult()
    data class Error(
        val code: Int,
        val message: String,
        val rawResponse: String? = null
    ) : LLMResult()
}

sealed class StreamResult {
    data object Started : StreamResult()
    data class Delta(val content: String) : StreamResult()
    data object Completed : StreamResult()
    data class Error(val message: String) : StreamResult()
}
