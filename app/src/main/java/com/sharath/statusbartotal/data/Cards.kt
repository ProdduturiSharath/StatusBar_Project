package com.sharath.statusbartotal.data

import java.io.File

/** [dueDay] is the bill due day of month (1-31) or null; [ccUpiDefault] marks the CC UPI card. */
data class Card(
    val code: String,
    val name: String,
    val dueDay: Int? = null,
    val ccUpiDefault: Boolean = false
)

/** cards.txt: "<code>,<name>,<dueDay or empty>,<1|0>"; old "<code>,<name>" lines still read. */
class Cards(private val file: File) {

    fun read(): List<Card> {
        val out = ArrayList<Card>()
        for (line in AtomicWrite.readLines(file)) {
            val card = parseLine(line) ?: continue
            if (out.any { it.code == card.code }) continue
            // Only one card can be the CC UPI default; the first one wins.
            out.add(if (card.ccUpiDefault && out.any { it.ccUpiDefault }) card.copy(ccUpiDefault = false) else card)
        }
        return out
    }

    private fun write(cards: List<Card>): Boolean =
        AtomicWrite.write(file, cards.joinToString("") { format(it) + "\n" })

    /** Returns false if the code or name is invalid, the code exists, or the write fails. */
    fun add(code: String, name: String): Boolean {
        val c = code.trim().lowercase()
        val n = cleanName(name) ?: return false
        if (!isValidCode(c)) return false
        val cards = read()
        if (cards.any { it.code == c }) return false
        return write(cards + Card(c, n))
    }

    /**
     * Changes name, due day and the CC UPI default flag of an existing card. Turning the flag on
     * turns it off on every other card. False if the card or name is invalid or the write fails.
     */
    fun update(code: String, name: String, dueDay: Int?, ccUpiDefault: Boolean): Boolean {
        val n = cleanName(name) ?: return false
        if (dueDay != null && dueDay !in 1..31) return false
        val cards = read()
        if (cards.none { it.code == code }) return false
        return write(cards.map {
            when {
                it.code == code -> Card(code, n, dueDay, ccUpiDefault)
                ccUpiDefault && it.ccUpiDefault -> it.copy(ccUpiDefault = false)
                else -> it
            }
        })
    }

    fun delete(code: String): Boolean = write(read().filter { it.code != code })

    companion object {
        private val CODE = Regex("[a-z]{3}")

        fun format(c: Card): String = "${c.code},${c.name},${c.dueDay ?: ""},${if (c.ccUpiDefault) 1 else 0}"

        fun parseLine(line: String): Card? {
            val i = line.indexOf(',')
            if (i < 0) return null
            val code = line.substring(0, i)
            if (!isValidCode(code)) return null
            val rest = line.substring(i + 1)
            var name = rest
            var due: Int? = null
            var default = false
            // New format: the last two fields are "<dueDay or empty>,<1|0>".
            val parts = rest.split(",")
            if (parts.size >= 3 && (parts.last() == "0" || parts.last() == "1")) {
                val dueText = parts[parts.size - 2]
                val parsed = dueText.toIntOrNull()
                if (dueText.isEmpty() || (parsed != null && parsed in 1..31)) {
                    name = parts.subList(0, parts.size - 2).joinToString(",")
                    due = parsed
                    default = parts.last() == "1"
                }
            }
            if (name.isBlank()) return null
            return Card(code, name, due, default)
        }

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
