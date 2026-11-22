package com.sharath.statusbartotal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.core.Recurring
import com.sharath.statusbartotal.core.RecurringRules
import com.sharath.statusbartotal.core.RecurringType

@Composable
fun RecurringTab(state: AppState, modifier: Modifier = Modifier) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Recurring?>(null) }
    var failed by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        if (state.recurring.isEmpty()) {
            Text(
                stringResource(R.string.none_yet),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.recurring, key = { it.id }) { r ->
                RecurringCard(state, r, onDelete = { deleteTarget = r })
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recurring)) }
    }

    if (showAdd) {
        AddRecurringDialog(state.cards, onAdd = { state.addRecurring(it) }, onDismiss = { showAdd = false })
    }
    deleteTarget?.let { r ->
        ConfirmDialog(
            title = stringResource(R.string.delete_recurring_title),
            message = stringResource(R.string.delete_recurring_message, r.name),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                deleteTarget = null
                if (!state.deleteRecurring(r.id)) failed = true
            },
            onDismiss = { deleteTarget = null }
        )
    }
    if (failed) InfoDialog(stringResource(R.string.error_save_failed)) { failed = false }
}

@Composable
private fun RecurringCard(state: AppState, r: Recurring, onDelete: () -> Unit) {
    val applied = state.appliedMonths(r)
    val bucket = Fmt.bucketLabel(
        if (r.card != null) "card:${r.card}" else r.category.name, state.cardNames
    )
    val color = AppColors.bucket(
        if (r.card != null) "card:${r.card}" else r.category.name, state.cards.map { it.code }
    )
    val left: String? = if (r.type == RecurringType.EMI && r.months != null) {
        when (val n = RecurringRules.monthsLeft(r, applied)) {
            0 -> stringResource(R.string.finished)
            1 -> stringResource(R.string.month_left)
            else -> stringResource(R.string.months_left, n)
        }
    } else null
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        typeLabel(r.type), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(Fmt.rupees(r.amount), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {}, label = { Text(bucket) },
                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            labelColor = color
                        )
                    )
                    Text("Every ${Fmt.ordinal(r.day)}")
                }
                if (left != null) {
                    Text(left, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
