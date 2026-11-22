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
     * total, lastEntry, buckets and cycleStart cover entries after the last reset; appliedSources
     * and autoResetMonths cover the whole file. Bucket keys: "UPI", "CC", "CCUPI", "card:<code>".
     * cycleStart is the time of the last reset line, or of the first entry if there is none.
     */
    data class Snapshot(
        val total: Long,
        val lastEntry: Long?,
        val buckets: Map<String, Long>,
        val appliedSources: Set<String>,
        val autoResetMonths: Set<YearMonth> = emptySet(),
        val cycleStart: Long? = null
    )

    private fun empty() = Snapshot(0L, null, emptyMap(), emptySet())

    fun addEntry(
        amount: Long, timeMillis: Long, category: Category, card: String?, source: String?
    ): Boolean = append("E,$timeMillis,$amount,${category.name},${card ?: ""},${source ?: ""}\n")

    /** A manual reset, or an automatic one when [autoMonth] is given. */
    fun reset(timeMillis: Long, autoMonth: YearMonth? = null): Boolean =
        append(if (autoMonth == null) "R,$timeMillis\n" else "R,$timeMillis,auto@$autoMonth\n")

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

    /** Every entry and reset, oldest first. The id is the 1-based line number. */
    fun history(): List<HistoryItem> {
        val text = completeText() ?: return emptyList()
        val lines = text.split('\n').dropLast(1)
        val out = ArrayList<HistoryItem>()
        val undoneIds = HashSet<Int>()
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
                if (source != null && source.startsWith(UNDO_PREFIX)) {
                    source.removePrefix(UNDO_PREFIX).toIntOrNull()?.let { undoneIds.add(it) }
                }
                out.add(HistoryItem.Entry(id, time, amount, category, card, source, false))
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
                out.add(HistoryItem.Reset(id, time, month != null, month))
            }
        }
        if (undoneIds.isEmpty()) return out
        return out.map {
            if (it is HistoryItem.Entry && !it.isUndoEntry && it.id in undoneIds)
                it.copy(undone = true) else it
        }
    }

    fun snapshot(): Snapshot {
        val items = history()
        if (items.isEmpty()) return empty()
        var total = 0L
        var last: Long? = null
        var buckets = HashMap<String, Long>()
        val sources = HashSet<String>()
        val resets = HashSet<YearMonth>()
        var firstEntry: Long? = null
        var lastReset: Long? = null
        for (item in items) {
            when (item) {
                is HistoryItem.Entry -> {
                    if (item.source != null) sources.add(item.source)
                    if (firstEntry == null) firstEntry = item.timeMillis
                    total += item.amount
                    last = item.amount
                    buckets[item.bucketKey] = (buckets[item.bucketKey] ?: 0L) + item.amount
                }
                is HistoryItem.Reset -> {
                    item.autoMonth?.let { resets.add(it) }
                    lastReset = item.timeMillis
                    total = 0L
                    last = null
                    buckets = HashMap()
                }
            }
        }
        return Snapshot(total, last, buckets, sources, resets, lastReset ?: firstEntry)
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
