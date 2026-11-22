package com.sharath.statusbartotal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.HistoryItem

private sealed interface HRow {
    data class Header(val text: String) : HRow
    data class Item(val entry: HistoryItem.Entry) : HRow
    data class ResetLine(val reset: HistoryItem.Reset) : HRow
}

private class Cycle(val startReset: HistoryItem.Reset?) {
    val entries = ArrayList<HistoryItem.Entry>()
}

private fun buildRows(history: List<HistoryItem>, filter: String): List<HRow> {
    val cycles = ArrayList<Cycle>()
    var current = Cycle(null)
    cycles.add(current)
    val endOf = HashMap<Cycle, Long>()
    for (item in history) {
        when (item) {
            is HistoryItem.Entry -> current.entries.add(item)
            is HistoryItem.Reset -> {
                endOf[current] = item.timeMillis
                current = Cycle(item)
                cycles.add(current)
            }
        }
    }
    val out = ArrayList<HRow>()
    for (cycle in cycles.asReversed()) {
        val shown = cycle.entries.filter { filter == "ALL" || it.bucketKey == filter }
        if (shown.isNotEmpty()) {
            val start = cycle.startReset?.timeMillis ?: cycle.entries.first().timeMillis
            val end = endOf[cycle]
            val endText = if (end == null) "now" else Fmt.date(end)
            val total = shown.sumOf { it.amount }
            out.add(HRow.Header("${Fmt.date(start)} – $endText · ${RupeeFormat.exact(total)}"))
            for (e in shown.asReversed()) out.add(HRow.Item(e))
        }
        cycle.startReset?.let { out.add(HRow.ResetLine(it)) }
    }
    return out
}

@Composable
fun HistoryTab(state: AppState, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var undoTarget by remember { mutableStateOf<HistoryItem.Entry?>(null) }
    var undoFailed by remember { mutableStateOf(false) }
    val codes = state.cards.map { it.code }
    val rows = remember(state.history, filter) { buildRows(state.history, filter) }
    val recurringNames = state.recurring.associate { it.id to it.name }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf("ALL", "UPI", "CC", "CCUPI") + state.cards.map { "card:" + it.code }
            for (key in options) {
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(if (key == "ALL") "All" else Fmt.bucketLabel(key, state.cardNames)) }
                )
            }
        }
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(rows) { row ->
                    when (row) {
                        is HRow.Header -> Text(
                            row.text, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        is HRow.ResetLine -> Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(Modifier.weight(1f))
                            Text(
                                "  Reset (${if (row.reset.auto) "auto" else "manual"}) · " +
                                    Fmt.dateTime(row.reset.timeMillis) + "  ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(Modifier.weight(1f))
                        }
                        is HRow.Item -> EntryRow(
                            e = row.entry, codes = codes, recurringNames = recurringNames,
                            onClick = { undoTarget = row.entry }
                        )
                    }
                }
            }
        }
    }

    undoTarget?.let { e ->
        val label = Fmt.bucketLabel(e.bucketKey, state.cardNames)
        ConfirmDialog(
            title = "Undo ${RupeeFormat.exact(e.amount)} ($label, ${Fmt.date(e.timeMillis)})?",
            message = "This adds ${RupeeFormat.exact(-e.amount)} and keeps both records.",
            confirmText = "Undo",
            onConfirm = {
                undoTarget = null
                if (!state.undo(e.id)) undoFailed = true
            },
            onDismiss = { undoTarget = null }
        )
    }
    if (undoFailed) InfoDialog("Couldn't undo this entry.") { undoFailed = false }
}

@Composable
private fun EntryRow(
    e: HistoryItem.Entry, codes: List<String>, recurringNames: Map<Long, String>,
    onClick: () -> Unit
) {
    val greyed = e.undone || e.isUndoEntry
    val tappable = !greyed
    val sub = when {
        e.isUndoEntry -> "Undo of ${RupeeFormat.exact(kotlin.math.abs(e.amount))}"
        e.source != null && e.source.contains('@') -> {
            val name = e.source.substringBefore('@').toLongOrNull()?.let { recurringNames[it] }
            if (name != null) "Auto · $name" else "Auto"
        }
        else -> null
    }
    val grey = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val amountColor = when {
        greyed -> grey
        e.amount < 0 -> negativeColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val deco = if (greyed) TextDecoration.LineThrough else TextDecoration.None
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (tappable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (greyed) grey else AppColors.bucket(e.bucketKey, codes))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                RupeeFormat.signed(e.amount), color = amountColor, textDecoration = deco,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                Fmt.dateTime(e.timeMillis) + (sub?.let { " · $it" } ?: ""),
                color = if (greyed) grey else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, textDecoration = deco
            )
        }
        Text(
            bucketShort(e), color = if (greyed) grey else Color.Unspecified,
            textDecoration = deco, style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun bucketShort(e: HistoryItem.Entry): String = when {
    e.card != null -> e.card.uppercase()
    e.category.name == "CCUPI" -> "CC UPI"
    else -> e.category.name
}
