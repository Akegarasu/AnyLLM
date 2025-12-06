package com.anzu.anyllm.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateEngine @Inject constructor() {

    private val placeholderPattern = Regex("""\{\{\s*(\w+)\s*\}\}""")

    fun render(template: String, variables: Map<String, String>): String {
        return placeholderPattern.replace(template) { matchResult ->
            val key = matchResult.groupValues[1]
            variables[key] ?: matchResult.value
        }
    }

    fun extractVariables(template: String): Set<String> {
        return placeholderPattern.findAll(template)
            .map { it.groupValues[1] }
            .toSet()
    }

    fun validateTemplate(template: String): TemplateValidationResult {
        val variables = extractVariables(template)
        return TemplateValidationResult(
            isValid = true,
            variables = variables
        )
    }
}

data class TemplateValidationResult(
    val isValid: Boolean,
    val variables: Set<String>,
    val error: String? = null
)
