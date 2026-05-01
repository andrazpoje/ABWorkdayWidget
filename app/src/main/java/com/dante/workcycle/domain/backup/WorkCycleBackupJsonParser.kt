package com.dante.workcycle.domain.backup

internal sealed interface WorkCycleBackupJsonValue

internal data class WorkCycleBackupJsonObject(
    val fields: Map<String, WorkCycleBackupJsonValue>
) : WorkCycleBackupJsonValue

internal data class WorkCycleBackupJsonArray(
    val items: List<WorkCycleBackupJsonValue>
) : WorkCycleBackupJsonValue

internal data class WorkCycleBackupJsonString(
    val value: String
) : WorkCycleBackupJsonValue

internal data class WorkCycleBackupJsonNumber(
    val rawValue: String
) : WorkCycleBackupJsonValue

internal data class WorkCycleBackupJsonBoolean(
    val value: Boolean
) : WorkCycleBackupJsonValue

internal data object WorkCycleBackupJsonNull : WorkCycleBackupJsonValue

internal object WorkCycleBackupJsonParser {

    fun parseObject(json: String): WorkCycleBackupJsonObject {
        return Parser(json).parseObject()
    }

    fun parseArray(json: String): WorkCycleBackupJsonArray {
        return Parser(json).parseArray()
    }

    private class Parser(
        private val source: String
    ) {
        private var index = 0

        fun parseObject(): WorkCycleBackupJsonObject {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            require(index == source.length) { "Unexpected trailing content at position $index." }
            return value as? WorkCycleBackupJsonObject
                ?: error("JSON root is not an object.")
        }

        fun parseArray(): WorkCycleBackupJsonArray {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            require(index == source.length) { "Unexpected trailing content at position $index." }
            return value as? WorkCycleBackupJsonArray
                ?: error("JSON root is not an array.")
        }

        private fun parseValue(): WorkCycleBackupJsonValue {
            skipWhitespace()
            val char = peek()
            return when (char) {
                '{' -> parseObjectBody()
                '[' -> parseArrayBody()
                '"' -> WorkCycleBackupJsonString(parseString())
                't' -> parseTrue()
                'f' -> parseFalse()
                'n' -> parseNull()
                '-', in '0'..'9' -> WorkCycleBackupJsonNumber(parseNumber())
                else -> error("Unexpected character '$char' at position $index.")
            }
        }

        private fun parseObjectBody(): WorkCycleBackupJsonObject {
            expect('{')
            skipWhitespace()

            if (tryConsume('}')) {
                return WorkCycleBackupJsonObject(emptyMap())
            }

            val fields = linkedMapOf<String, WorkCycleBackupJsonValue>()
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                fields[key] = value
                skipWhitespace()

                when {
                    tryConsume('}') -> break
                    tryConsume(',') -> continue
                    else -> error("Expected ',' or '}' at position $index.")
                }
            }

            return WorkCycleBackupJsonObject(fields)
        }

        private fun parseArrayBody(): WorkCycleBackupJsonArray {
            expect('[')
            skipWhitespace()

            if (tryConsume(']')) {
                return WorkCycleBackupJsonArray(emptyList())
            }

            val items = mutableListOf<WorkCycleBackupJsonValue>()
            while (true) {
                items += parseValue()
                skipWhitespace()

                when {
                    tryConsume(']') -> break
                    tryConsume(',') -> continue
                    else -> error("Expected ',' or ']' at position $index.")
                }
            }

            return WorkCycleBackupJsonArray(items)
        }

        private fun parseString(): String {
            expect('"')
            val builder = StringBuilder()

            while (true) {
                require(index < source.length) { "Unterminated string literal." }
                val char = source[index++]
                when (char) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(parseEscape())
                    else -> builder.append(char)
                }
            }
        }

        private fun parseEscape(): Char {
            require(index < source.length) { "Unterminated escape sequence." }
            return when (val escaped = source[index++]) {
                '"', '\\', '/' -> escaped
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    require(index + 4 <= source.length) { "Invalid unicode escape at position $index." }
                    val hex = source.substring(index, index + 4)
                    index += 4
                    hex.toInt(16).toChar()
                }
                else -> error("Invalid escape character '$escaped' at position $index.")
            }
        }

        private fun parseNumber(): String {
            val start = index

            if (peek() == '-') {
                index++
            }

            parseDigits()

            if (hasNext() && peek() == '.') {
                index++
                parseDigits()
            }

            if (hasNext() && (peek() == 'e' || peek() == 'E')) {
                index++
                if (hasNext() && (peek() == '+' || peek() == '-')) {
                    index++
                }
                parseDigits()
            }

            return source.substring(start, index)
        }

        private fun parseDigits() {
            require(hasNext() && peek().isDigit()) { "Expected digit at position $index." }
            while (hasNext() && peek().isDigit()) {
                index++
            }
        }

        private fun parseTrue(): WorkCycleBackupJsonBoolean {
            expectLiteral("true")
            return WorkCycleBackupJsonBoolean(true)
        }

        private fun parseFalse(): WorkCycleBackupJsonBoolean {
            expectLiteral("false")
            return WorkCycleBackupJsonBoolean(false)
        }

        private fun parseNull(): WorkCycleBackupJsonNull {
            expectLiteral("null")
            return WorkCycleBackupJsonNull
        }

        private fun expectLiteral(literal: String) {
            require(source.regionMatches(index, literal, 0, literal.length)) {
                "Expected '$literal' at position $index."
            }
            index += literal.length
        }

        private fun expect(char: Char) {
            require(hasNext() && source[index] == char) {
                "Expected '$char' at position $index."
            }
            index++
        }

        private fun tryConsume(char: Char): Boolean {
            if (hasNext() && source[index] == char) {
                index++
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (hasNext() && source[index].isWhitespace()) {
                index++
            }
        }

        private fun hasNext(): Boolean = index < source.length

        private fun peek(): Char {
            require(hasNext()) { "Unexpected end of JSON input." }
            return source[index]
        }
    }
}
