package com.dante.workcycle.domain.backup

internal object WorkCycleBackupJsonWriter {

    fun objectString(fields: List<Pair<String, String>>): String {
        return buildString {
            append("{\n")
            fields.forEachIndexed { index, (key, value) ->
                append("  ")
                append(stringLiteral(key))
                append(": ")
                append(value)
                if (index != fields.lastIndex) {
                    append(',')
                }
                append('\n')
            }
            append('}')
        }
    }

    fun arrayString(items: List<String>): String {
        return buildString {
            append("[\n")
            items.forEachIndexed { index, item ->
                append(indent(item))
                if (index != items.lastIndex) {
                    append(',')
                }
                append('\n')
            }
            append(']')
        }
    }

    fun stringLiteral(value: String): String {
        return buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
            append('"')
        }
    }

    fun literal(value: String?): String {
        return value?.let(::stringLiteral) ?: "null"
    }

    fun literal(value: Boolean): String = if (value) "true" else "false"

    fun literal(value: Number): String = value.toString()

    private fun indent(value: String): String {
        return value.lines().joinToString(separator = "\n") { "  $it" }
    }
}
