package com.dante.workcycle.util

private val whitespaceRegex = Regex("\\s+")

fun sanitizeLabel(text: String, fallback: String): String {
    val cleaned = text.trim().replace(whitespaceRegex, " ")
    return cleaned.ifEmpty { fallback }
}

fun parseCycleInput(rawInput: String): List<String> {
    return rawInput
        .split(",")
        .map { sanitizeLabel(it, "") }
        .filter { it.isNotBlank() }
}