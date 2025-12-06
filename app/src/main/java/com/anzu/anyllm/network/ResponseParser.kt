package com.anzu.anyllm.network

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseParser @Inject constructor() {

    fun extractContent(json: String, jsonPath: String): String? {
        return try {
            val result: Any? = JsonPath.read(json, jsonPath)
            when (result) {
                is String -> result
                is List<*> -> result.firstOrNull()?.toString()
                else -> result?.toString()
            }
        } catch (e: PathNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun extractContentWithFallback(json: String, jsonPaths: List<String>): String? {
        for (path in jsonPaths) {
            val result = extractContent(json, path)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun validateJsonPath(jsonPath: String): Boolean {
        return try {
            JsonPath.compile(jsonPath)
            true
        } catch (e: Exception) {
            false
        }
    }
}
