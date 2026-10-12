package com.example.gallerydl.util

class CliParseException(message: String) : IllegalArgumentException(message)

object CliTokenizer {

    /**
     * Tokenizes a command line string into a list of argument tokens following shell quoting semantics.
     *
     * @param input The raw input string containing flags, options, and arguments.
     * @param stripExecutablePrefix When true, removes the executable name "gallery-dl" or "gallery-dl.exe"
     *                               only if it appears as the exact first parsed token.
     * @return List of parsed argument tokens.
     * @throws CliParseException If the input has unmatched quotes or invalid escape sequences.
     */
    fun tokenize(input: String, stripExecutablePrefix: Boolean = true): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var state = State.NORMAL
        var hasToken = false

        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]
            when (state) {
                State.NORMAL -> {
                    when {
                        c.isWhitespace() -> {
                            if (hasToken) {
                                tokens.add(currentToken.toString())
                                currentToken.clear()
                                hasToken = false
                            }
                        }
                        c == '"' -> {
                            hasToken = true
                            state = State.IN_DOUBLE_QUOTE
                        }
                        c == '\'' -> {
                            hasToken = true
                            state = State.IN_SINGLE_QUOTE
                        }
                        c == '\\' -> {
                            if (i + 1 < trimmed.length) {
                                val next = trimmed[++i]
                                currentToken.append(next)
                                hasToken = true
                            } else {
                                throw CliParseException("Trailing backslash escape at end of input")
                            }
                        }
                        else -> {
                            currentToken.append(c)
                            hasToken = true
                        }
                    }
                }
                State.IN_DOUBLE_QUOTE -> {
                    when (c) {
                        '"' -> {
                            state = State.NORMAL
                        }
                        '\\' -> {
                            if (i + 1 < trimmed.length) {
                                val next = trimmed[++i]
                                when (next) {
                                    '"', '\\', '$', '`', '\n' -> currentToken.append(next)
                                    else -> {
                                        currentToken.append('\\')
                                        currentToken.append(next)
                                    }
                                }
                            } else {
                                throw CliParseException("Unterminated escape sequence in double-quoted string")
                            }
                        }
                        else -> {
                            currentToken.append(c)
                        }
                    }
                }
                State.IN_SINGLE_QUOTE -> {
                    when (c) {
                        '\'' -> {
                            state = State.NORMAL
                        }
                        '\\' -> {
                            if (i + 1 < trimmed.length && trimmed[i + 1] == '\'') {
                                currentToken.append('\'')
                                i++
                            } else if (i + 1 < trimmed.length && trimmed[i + 1] == '\\') {
                                currentToken.append('\\')
                                i++
                            } else {
                                currentToken.append('\\')
                            }
                        }
                        else -> {
                            currentToken.append(c)
                        }
                    }
                }
            }
            i++
        }

        if (state == State.IN_DOUBLE_QUOTE) {
            throw CliParseException("Unmatched double quote in command line arguments: \"$input\"")
        }
        if (state == State.IN_SINGLE_QUOTE) {
            throw CliParseException("Unmatched single quote in command line arguments: \"$input\"")
        }

        if (hasToken) {
            tokens.add(currentToken.toString())
        }

        if (stripExecutablePrefix && tokens.isNotEmpty()) {
            val first = tokens[0]
            if (first.equals("gallery-dl", ignoreCase = true) || first.equals("gallery-dl.exe", ignoreCase = true)) {
                tokens.removeAt(0)
            }
        }

        return tokens
    }

    private enum class State {
        NORMAL,
        IN_DOUBLE_QUOTE,
        IN_SINGLE_QUOTE
    }
}
