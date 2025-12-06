package com.anzu.anyllm.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SSEHandler @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val responseParser: ResponseParser
) {
    fun connect(request: Request, jsonPath: String): Flow<SSEEvent> = callbackFlow {
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(SSEEvent.Connected)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data.trim() == "[DONE]") {
                    trySend(SSEEvent.Done)
                    return
                }

                val content = responseParser.extractContent(data, jsonPath)
                if (content != null) {
                    trySend(SSEEvent.Data(content))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SSEEvent.Closed)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMessage = t?.message ?: response?.message ?: "Unknown SSE error"
                trySend(SSEEvent.Error(errorMessage))
                close(t)
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}

sealed class SSEEvent {
    data object Connected : SSEEvent()
    data class Data(val content: String) : SSEEvent()
    data object Done : SSEEvent()
    data object Closed : SSEEvent()
    data class Error(val message: String) : SSEEvent()
}
