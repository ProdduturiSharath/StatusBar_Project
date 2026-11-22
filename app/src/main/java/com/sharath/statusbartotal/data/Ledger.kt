package com.sharath.statusbartotal.data

import com.sharath.statusbartotal.core.Category
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.time.YearMonth
import java.time.format.DateTimeParseException

sealed class HistoryItem {
    abstract val id: Int
    abstract val timeMillis: Long

    data class Entry(
        override val id: Int,
        override val timeMillis: Long,
        val amount: Long,
        val category: Category,
        val card: String?,
        val source: String?,
        val undone: Boolean
    ) : HistoryItem() {
        val isUndoEntry: Boolean get() = source?.startsWith(Ledger.UNDO_PREFIX) == true
        val bucketKey: String get() = if (card != null) "card:$card" else category.name
    }

    data class Reset(
        override val id: Int,
        override val timeMillis: Long,
        val auto: Boolean,
        val autoMonth: YearMonth? = null
    ) : HistoryItem()
}

class Ledger(private val file: File) {

    /**
     * total, lastEntry, buckets and cycleStart cover entries after the last reset; appliedSources,
     * autoResetMonths and notes cover the whole file (appliedSources includes deleted entries).
     * Bucket keys: "UPI", "CC", "CCUPI", "card:<code>". cycleStart is the time of the last reset
     * line, or of the first remaining entry if there is none. cycleKey is the line id of the last
     * reset line (0 before the first reset). cardCcUpi is the CC UPI part of each card bucket.
     */
    data class Snapshot(
        val total: Long,
        val lastEntry: Long?,
        val buckets: Map<String, Long>,
        val appliedSources: Set<String>,
        val autoResetMonths: Set<YearMonth> = emptySet(),
        val cycleStart: Long? = null,
        val cycleKey: Int = 0,
        val notes: Set<String> = emptySet(),
        val lastLineId: Int = 0,
        val cardCcUpi: Map<String, Long> = emptyMap()
    )

    private class Parsed(
        val items: List<HistoryItem>,
        val sources: Set<String>,
        val notes: Set<String>,
        val lastLineId: Int
    )

    fun addEntry(
        amount: Long, timeMillis: Long, category: Category, card: String?, source: String?
    ): Boolean = append("E,$timeMillis,$amount,${category.name},${card ?: ""},${source ?: ""}\n")

    /** A manual reset, or an automatic one when [autoMonth] is given. */
    fun reset(timeMillis: Long, autoMonth: YearMonth? = null): Boolean =
        append(if (autoMonth == null) "R,$timeMillis\n" else "R,$timeMillis,auto@$autoMonth\n")

    /** A note line, for example "budget@0:30000". Keys must not contain commas or line breaks. */
    fun addNote(timeMillis: Long, key: String): Boolean {
        if (key.isEmpty() || key.any { it == ',' || it == '\n' || it == '\r' }) return false
        return append("N,$timeMillis,$key\n")
    }

    /** Tombstone: every entry line with an id in [fromId]..[toId] is ignored from now on. */
    fun deleteRange(timeMillis: Long, fromId: Int, toId: Int): Boolean {
        if (fromId < 1 || toId < fromId) return false
        return append("D,$timeMillis,$fromId-$toId\n")
    }

    /** Adds the opposite of entry [id]. False if it is missing, an undo, already undone, or the write fails. */
    fun undo(id: Int, timeMillis: Long): Boolean {
        val e = history().firstOrNull { it.id == id } as? HistoryItem.Entry ?: return false
        if (e.undone || e.isUndoEntry) return false
        return addEntry(-e.amount, timeMillis, e.category, e.card, "$UNDO_PREFIX$id")
    }

    private fun completeText(): String? {
        val bytes = try {
            if (!file.exists()) return null
            file.readBytes()
        } catch (e: IOException) {
            return null
        }
        // Only text up to the last newline counts; a cut-off last line is ignored.
        val end = bytes.lastIndexOf('\n'.code.toByte()) + 1
        if (end == 0) return null
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    private fun parseRange(s: String): IntRange? {
        val i = s.indexOf('-')
        if (i < 1) return null
        val from = s.substring(0, i).toIntOrNull() ?: return null
        val to = s.substring(i + 1).toIntOrNull() ?: return null
        return if (from >= 1 && to >= from) from..to else null
    }

    private fun parse(extra: IntRange?): Parsed {
        val text = completeText() ?: return Parsed(emptyList(), emptySet(), emptySet(), 0)
        val lines = text.split('\n').dropLast(1)
        val entries = ArrayList<HistoryItem.Entry>()
        val resets = ArrayList<HistoryItem.Reset>()
        val ranges = ArrayList<IntRange>()
        if (extra != null) ranges.add(extra)
        val notes = HashSet<String>()
        val sources = HashSet<String>()
        lines.forEachIndexed { index, line ->
            val id = index + 1
            val parts = line.split(",")
            if (parts[0] == "E" && (parts.size == 3 || parts.size == 6)) {
                val time = parts[1].toLongOrNull() ?: return@forEachIndexed
                val amount = parts[2].toLongOrNull() ?: return@forEachIndexed
                var category = Category.UPI
                var card: String? = null
                var source: String? = null
                if (parts.size == 6) {
                    category = try {
                        Category.valueOf(parts[3])
                    } catch (e: IllegalArgumentException) {
                        return@forEachIndexed
                    }
                    card = parts[4].ifEmpty { null }
                    source = parts[5].ifEmpty { null }
                }
                if (source != null) sources.add(source)
                entries.add(HistoryItem.Entry(id, time, amount, category, card, source, false))
            } else if ((parts.size == 2 || parts.size == 3) && parts[0] == "R") {
                val time = parts[1].toLongOrNull() ?: return@forEachIndexed
                var month: YearMonth? = null
                if (parts.size == 3 && parts[2].startsWith("auto@")) {
                    month = try {
                        YearMonth.parse(parts[2].removePrefix("auto@"))
                    } catch (e: DateTimeParseException) {
                        null
                    }
                }
                resets.add(HistoryItem.Reset(id, time, month != null, month))
            } else if (parts.size == 3 && parts[0] == "D") {
                if (parts[1].toLongOrNull() != null) parseRange(parts[2])?.let { ranges.add(it) }
            } else if (parts.size == 3 && parts[0] == "N") {
                if (parts[1].toLongOrNull() != null && parts[2].isNotEmpty()) notes.add(parts[2])
            }
        }
        val deleted = HashSet<Int>()
        for (e in entries) if (ranges.any { e.id in it }) deleted.add(e.id)
        // An undo and its original are deleted together, whichever of the two was tombstoned.
        val entryIds = entries.map { it.id }.toHashSet()
        for (e in entries) {
            val target = undoTarget(e) ?: continue
            if (target !in entryIds) continue
            if (e.id in deleted || target in deleted) {
                deleted.add(e.id)
                deleted.add(target)
            }
        }
        val visible = entries.filter { it.id !in deleted }
        val undoneIds = HashSet<Int>()
        for (e in visible) undoTarget(e)?.let { undoneIds.add(it) }
        val items = ArrayList<HistoryItem>(visible.size + resets.size)
        for (e in visible) {
            items.add(if (!e.isUndoEntry && e.id in undoneIds) e.copy(undone = true) else e)
        }
        items.addAll(resets)
        items.sortBy { it.id }
        return Parsed(items, sources, notes, lines.size)
    }

    private fun undoTarget(e: HistoryItem.Entry): Int? =
        if (e.isUndoEntry) e.source!!.removePrefix(UNDO_PREFIX).toIntOrNull() else null

    /** Every remaining entry and every reset, oldest first. The id is the 1-based line number. */
    fun history(): List<HistoryItem> = parse(null).items

    fun snapshot(): Snapshot = snapshot(null)

    /**
     * The snapshot as it would be after a tombstone covering [extraDeleted] (used to preview what
     * a delete changes in the current total).
     */
    fun snapshot(extraDeleted: IntRange?): Snapshot {
        val parsed = parse(extraDeleted)
        var total = 0L
        var last: Long? = null
        var buckets = HashMap<String, Long>()
        var cardCcUpi = HashMap<String, Long>()
        val resets = HashSet<YearMonth>()
        var firstEntry: Long? = null
        var lastReset: Long? = null
        var cycleKey = 0
        for (item in parsed.items) {
            when (item) {
                is HistoryItem.Entry -> {
                    if (firstEntry == null) firstEntry = item.timeMillis
                    total += item.amount
                    last = item.amount
                    buckets[item.bucketKey] = (buckets[item.bucketKey] ?: 0L) + item.amount
                    if (item.category == Category.CCUPI && item.card != null) {
                        cardCcUpi[item.card] = (cardCcUpi[item.card] ?: 0L) + item.amount
                    }
                }
                is HistoryItem.Reset -> {
                    item.autoMonth?.let { resets.add(it) }
                    lastReset = item.timeMillis
                    cycleKey = item.id
                    total = 0L
                    last = null
                    buckets = HashMap()
                    cardCcUpi = HashMap()
                }
            }
        }
        return Snapshot(
            total, last, buckets, parsed.sources, resets, lastReset ?: firstEntry,
            cycleKey, parsed.notes, parsed.lastLineId, cardCcUpi
        )
    }

    /**
     * Changes card code [old] to [new] on every entry line. Rewrites the file atomically with
     * the same number of lines, so line ids (used by deletes and undo) do not change. A cut-off
     * last piece (never counted) is dropped.
     */
    fun renameCard(old: String, new: String): Boolean {
        return try {
            if (!file.exists()) return true
            val text = file.readText(Charsets.UTF_8)
            val complete = text.substring(0, text.lastIndexOf('\n') + 1)
            val out = StringBuilder(complete.length)
            for (line in complete.split('\n').dropLast(1)) {
                val parts = line.split(",").toMutableList()
                if (parts.size == 6 && parts[0] == "E" && parts[4] == old) parts[4] = new
                out.append(parts.joinToString(",")).append('\n')
            }
            AtomicWrite.write(file, out.toString())
        } catch (e: IOException) {
            false
        }
    }

    private fun append(line: String): Boolean {
        try {
            trimCutOffLine()
            FileOutputStream(file, true).use { out ->
                out.write(line.toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
            return true
        } catch (e: IOException) {
            return false
        }
    }

    /** If the file does not end with a newline, cut it back to just after its last newline. */
    private fun trimCutOffLine() {
        if (!file.exists()) return
        RandomAccessFile(file, "rw").use { raf ->
            val len = raf.length()
            if (len == 0L) return
            raf.seek(len - 1)
            if (raf.read() == '\n'.code) return
            val bytes = ByteArray(len.toInt())
            raf.seek(0)
            raf.readFully(bytes)
            raf.setLength((bytes.lastIndexOf('\n'.code.toByte()) + 1).toLong())
        }
    }

    companion object {
        const val UNDO_PREFIX = "undo#"
    }
}
