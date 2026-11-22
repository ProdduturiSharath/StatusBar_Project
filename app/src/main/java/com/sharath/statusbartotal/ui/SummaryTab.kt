package com.sharath.statusbartotal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sharath.statusbartotal.core.Summary
import com.sharath.statusbartotal.core.SummaryRow
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.ReportWriter

@Composable
fun SummaryTab(state: AppState, modifier: Modifier = Modifier) {
    val rows = remember(state.cycles) { Summary.build(state.cycles) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = {
                    message = if (state.exportReport()) "Report saved to ${ReportWriter.SHOWN_FOLDER}"
                    else "Couldn't save the report"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export report") }
        }
        if (rows.isEmpty()) {
            item { Text("No entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(rows, key = { it.cycle.key }) { row -> CycleCard(state, row) }
    }

    message?.let { InfoDialog(it) { message = null } }
}

@Composable
private fun CycleCard(state: AppState, row: SummaryRow) {
    val cycle = row.cycle
    val codes = state.cards.map { it.code }
    val buckets = cycle.buckets
    val keys = buckets.keys
        .filter { buckets.getValue(it) != 0L }
        .sortedWith(compareBy({ when (it) { "UPI" -> 0; "CC" -> 1; "CCUPI" -> 2; else -> 3 } }, { it }))
    val positive = keys.filter { buckets.getValue(it) > 0L }
    val start = cycle.startMillis?.let { Fmt.date(it) } ?: "-"
    val end = cycle.endReset?.let { Fmt.date(it.timeMillis) } ?: "now"
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$start – $end", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (cycle.isCurrent) {
                Text("So far", color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Fmt.rupees(cycle.total), style = MaterialTheme.typography.headlineSmall)
            val d = row.delta
            if (d != null && d != 0L) {
                if (d > 0) {
                    Text("▲ " + RupeeFormat.exact(d), color = negativeColor())
                } else {
                    Text("▼ " + RupeeFormat.exact(-d), color = positiveColor())
                }
            }
        }
        if (positive.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                for (k in positive) {
                    Box(
                        Modifier
                            .weight(buckets.getValue(k).toFloat())
                            .height(14.dp)
                            .background(AppColors.bucket(k, codes))
                    )
                }
            }
        }
        for (k in keys) {
            val v = buckets.getValue(k)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AppColors.bucket(k, codes))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Fmt.bucketLabel(k, state.cardNames), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    Fmt.rupees(v), style = MaterialTheme.typography.bodyMedium,
                    color = if (v < 0) negativeColor() else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
