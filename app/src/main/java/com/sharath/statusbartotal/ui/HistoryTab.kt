package com.sharath.statusbartotal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sharath.statusbartotal.core.CycleInfo
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.HistoryItem

private sealed interface HRow {
    data class Header(val text: String, val cycle: CycleInfo) : HRow
    data class Item(val entry: HistoryItem.Entry) : HRow
    data class ResetLine(val reset: HistoryItem.Reset) : HRow
}

private fun buildRows(cycles: List<CycleInfo>, filter: String): List<HRow> {
    val out = ArrayList<HRow>()
    for (cycle in cycles.asReversed()) {
        // Cycles with no remaining entries are hidden (the current one stays, even when empty).
        if (cycle.entries.isEmpty() && !cycle.isCurrent) continue
        val shown = cycle.entries.filter { filter == "ALL" || it.bucketKey == filter }
        if (shown.isNotEmpty()) {
            val start = cycle.startMillis ?: shown.first().timeMillis
            val end = cycle.endReset?.timeMillis
            val endText = if (end == null) "now" else Fmt.date(end)
            val total = shown.sumOf { it.amount }
            out.add(HRow.Header("${Fmt.date(start)} – $endText · ${RupeeFormat.exact(total)}", cycle))
            for (e in shown.asReversed()) out.add(HRow.Item(e))
        }
        cycle.startReset?.let { out.add(HRow.ResetLine(it)) }
    }
    return out
}

/** A confirmed-before-it-happens delete of the line ids [from]..[to]. */
private class DeleteRequest(val title: String, val message: String, val from: Int, val to: Int)

@Composable
fun HistoryTab(state: AppState, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var undoTarget by remember { mutableStateOf<HistoryItem.Entry?>(null) }
    var undoFailed by remember { mutableStateOf(false) }
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    var deleteFailed by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    val codes = state.cards.map { it.code }
    val rows = remember(state.cycles, filter) { buildRows(state.cycles, filter) }
    val recurringNames = state.recurring.associate { it.id to it.name }

    /** Builds a delete request, including what the delete changes in the current total. */
    fun request(title: String, what: String, from: Int, to: Int) {
        val (before, after) = state.previewDelete(from, to)
        val change = if (before == after) "No change to the current total."
        else "The current total changes from ${RupeeFormat.exact(before)} to ${RupeeFormat.exact(after)}."
        deleteRequest = DeleteRequest(title, "$what will be removed from all totals and history. $change", from, to)
    }

    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasCc = state.history.any { it is HistoryItem.Entry && it.bucketKey == "CC" }
                val hasCcUpi = state.history.any { it is HistoryItem.Entry && it.bucketKey == "CCUPI" }
                val options = listOf("ALL", "UPI") + state.cards.map { "card:" + it.code } +
                    listOfNotNull(if (hasCc) "CC" else null, if (hasCcUpi) "CCUPI" else null)
                for (key in options) {
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(if (key == "ALL") "All" else Fmt.bucketLabel(key, state.cardNames)) }
                    )
                }
            }
            Box {
                IconButton(onClick = { topMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete all history") },
                        onClick = {
                            topMenu = false
                            val all = state.history.filterIsInstance<HistoryItem.Entry>()
                            if (all.isNotEmpty()) {
                                request(
                                    "Delete all history?",
                                    "All ${all.size} entries (${RupeeFormat.exact(all.sumOf { it.amount })})",
                                    1, state.snapshot.lastLineId
                                )
                            }
                        }
                    )
                }
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
                        is HRow.Header -> CycleHeader(row.text) {
                            val c = row.cycle
                            val to = (c.endReset?.id?.minus(1)) ?: state.snapshot.lastLineId
                            request(
                                "Delete this cycle?",
                                "All ${c.entries.size} entries (${RupeeFormat.exact(c.total)}) of this cycle",
                                c.firstLineId, to
                            )
                        }
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
                            onClick = { undoTarget = row.entry },
                            onLongClick = {
                                val e = row.entry
                                val label = Fmt.bucketLabel(e.bucketKey, state.cardNames)
                                request(
                                    "Delete this entry?",
                                    "${RupeeFormat.signed(e.amount)} ($label, ${Fmt.date(e.timeMillis)})" +
                                        if (e.undone) " and its undo" else "",
                                    e.id, e.id
                                )
                            }
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
    deleteRequest?.let { r ->
        ConfirmDialog(
            title = r.title,
            message = r.message,
            confirmText = "Delete",
            onConfirm = {
                deleteRequest = null
                if (!state.deleteRange(r.from, r.to)) deleteFailed = true
            },
            onDismiss = { deleteRequest = null }
        )
    }
    if (deleteFailed) InfoDialog("Couldn't delete. Phone storage may be full.") { deleteFailed = false }
}

@Composable
private fun CycleHeader(text: String, onDeleteCycle: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Cycle menu")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete this cycle") },
                    onClick = {
                        menu = false
                        onDeleteCycle()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    e: HistoryItem.Entry, codes: List<String>, recurringNames: Map<Long, String>,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    val greyed = e.undone || e.isUndoEntry
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
    // Undo rows cannot be deleted or undone on their own; deleting the original removes both.
    val clickModifier = when {
        e.isUndoEntry -> Modifier
        e.undone -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
        else -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(clickModifier)
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

private fun bucketShort(e: HistoryItem.Entry): String {
    val tag = if (e.category.name == "CCUPI") " · CC UPI" else ""
    return when {
        e.card != null -> e.card.uppercase() + tag
        e.category.name == "CCUPI" -> "CC UPI"
        else -> e.category.name
    }
}
