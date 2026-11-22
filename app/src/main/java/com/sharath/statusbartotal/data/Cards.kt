package com.sharath.statusbartotal.data

import java.io.File

data class Card(val code: String, val name: String)

class Cards(private val file: File) {

    fun read(): List<Card> {
        val out = ArrayList<Card>()
        for (line in AtomicWrite.readLines(file)) {
            val i = line.indexOf(',')
            if (i < 0) continue
            val code = line.substring(0, i)
            val name = line.substring(i + 1)
            if (!isValidCode(code) || name.isBlank()) continue
            if (out.any { it.code == code }) continue
            out.add(Card(code, name))
        }
        return out
    }

    private fun write(cards: List<Card>): Boolean =
        AtomicWrite.write(file, cards.joinToString("") { "${it.code},${it.name}\n" })

    /** Returns false if the code or name is invalid, the code exists, or the write fails. */
    fun add(code: String, name: String): Boolean {
        val c = code.trim().lowercase()
        val n = cleanName(name) ?: return false
        if (!isValidCode(c)) return false
        val cards = read()
        if (cards.any { it.code == c }) return false
        return write(cards + Card(c, n))
    }

    fun delete(code: String): Boolean = write(read().filter { it.code != code })

    companion object {
        private val CODE = Regex("[a-z]{3}")

        /** Valid stored code: 3 lowercase ASCII letters, not "upi" or "cc". */
        fun isValidCode(code: String): Boolean =
            CODE.matches(code) && code != "upi" && code != "cc"

        /** Commas and line breaks become spaces; trimmed; 1 to 40 characters, else null. */
        fun cleanName(name: String): String? {
            val n = name.map { if (it == ',' || it == '\n' || it == '\r') ' ' else it }
                .joinToString("").trim()
            return if (n.length in 1..40) n else null
        }
    }
}
