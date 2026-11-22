package com.sharath.statusbartotal.data

import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.Recurring
import com.sharath.statusbartotal.core.RecurringType
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeParseException

class RecurringStore(private val file: File) {

    fun read(): List<Recurring> = AtomicWrite.readLines(file).mapNotNull { parseLine(it) }

    private fun write(items: List<Recurring>): Boolean =
        AtomicWrite.write(file, items.joinToString("") { format(it) + "\n" })

    fun add(r: Recurring): Boolean = write(read() + r)

    fun delete(id: Long): Boolean = write(read().filter { it.id != id })

    companion object {
        fun format(r: Recurring): String = listOf(
            r.id, r.type.name, r.amount, r.category.name, r.card ?: "", r.day,
            r.startMonth, r.months?.toString() ?: "", r.name
        ).joinToString(",")

        fun parseLine(line: String): Recurring? {
            val p = line.split(",", limit = 9)
            if (p.size != 9) return null
            return try {
                val type = RecurringType.valueOf(p[1])
                val category = Category.valueOf(p[3])
                val id = p[0].toLongOrNull() ?: return null
                val amount = p[2].toLongOrNull() ?: return null
                val day = p[5].toIntOrNull() ?: return null
                if (day !in 1..31) return null
                val months = if (p[7].isEmpty()) null else (p[7].toIntOrNull() ?: return null)
                if (p[8].isBlank()) return null
                Recurring(
                    id, type, amount, category, p[4].ifEmpty { null }, day,
                    YearMonth.parse(p[6]), months, p[8]
                )
            } catch (e: IllegalArgumentException) {
                null
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }
}
