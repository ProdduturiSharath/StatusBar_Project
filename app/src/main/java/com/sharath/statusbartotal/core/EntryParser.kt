package com.sharath.statusbartotal.core

sealed class ParseResult {
    data class Ok(val amount: Long, val category: Category, val card: String?) : ParseResult()
    object BadAmount : ParseResult()
    data class NoCard(val word: String) : ParseResult()
}

object EntryParser {
    private val PATTERN = Regex("[+-]?[0-9]{1,9}")
    private val SPLIT = Regex("[\\s\\u00A0]+")

    /** Returns the whole-rupee amount, or null if the text is rejected. */
    fun parse(text: String): Long? {
        val cleaned = buildString {
            for (c in text) {
                when (c) {
                    ' ', ' ', ',' -> Unit
                    '−' -> append('-')
                    else -> append(c)
                }
            }
        }
        if (!PATTERN.matches(cleaned)) return null
        val value = cleaned.toLong()
        return if (value == 0L) null else value
    }

    /** Amount word, then optional category and/or card word. See the V2 spec. */
    fun parseEntry(text: String, knownCards: Set<String>): ParseResult {
        val trimmed = text.trim { it.isWhitespace() || it == ' ' }
        if (trimmed.isEmpty()) return ParseResult.BadAmount
        val words = trimmed.split(SPLIT)
        if (words.size > 3) return ParseResult.BadAmount
        val amount = parse(words[0]) ?: return ParseResult.BadAmount
        if (words.size == 1) return ParseResult.Ok(amount, Category.UPI, null)

        val w2 = words[1].lowercase()
        val w3 = words.getOrNull(2)?.lowercase()
        return when (w2) {
            "upi" ->
                if (w3 != null) ParseResult.BadAmount else ParseResult.Ok(amount, Category.UPI, null)
            "cc", "ccupi" -> {
                val category = if (w2 == "cc") Category.CC else Category.CCUPI
                if (w3 == null) ParseResult.Ok(amount, category, null)
                else cardResult(amount, category, w3, knownCards)
            }
            else ->
                if (w3 != null) ParseResult.BadAmount
                else cardResult(amount, Category.CC, w2, knownCards)
        }
    }

    private fun cardResult(
        amount: Long, category: Category, word: String, knownCards: Set<String>
    ): ParseResult {
        if (word == "upi" || word == "cc" || word == "ccupi") return ParseResult.BadAmount
        if (word !in knownCards) return ParseResult.NoCard(word)
        return ParseResult.Ok(amount, category, word)
    }
}
