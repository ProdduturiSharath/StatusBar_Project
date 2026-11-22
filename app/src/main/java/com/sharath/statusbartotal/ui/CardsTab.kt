package com.sharath.statusbartotal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.sharath.statusbartotal.data.Card as CardData

@Composable
fun CardsTab(state: AppState, modifier: Modifier = Modifier) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CardData?>(null) }
    var editTarget by remember { mutableStateOf<CardData?>(null) }
    var inUse by remember { mutableStateOf<Pair<CardData, List<String>>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val codes = state.cards.map { it.code }

    Box(modifier.fillMaxSize()) {
        if (state.cards.isEmpty()) {
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
            items(state.cards, key = { it.code }) { c ->
                val total = state.snapshot.buckets["card:" + c.code] ?: 0L
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editTarget = c }
                ) {
                    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(8.dp)
                                .fillMaxHeight()
                                .background(AppColors.bucket("card:" + c.code, codes))
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(c.code.uppercase(), style = MaterialTheme.typography.titleMedium)
                            Text(c.name)
                            Text(
                                "This cycle: " + Fmt.rupees(total),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val upi = state.snapshot.cardCcUpi[c.code] ?: 0L
                            if (upi != 0L) {
                                Text(
                                    "incl. CC UPI " + Fmt.rupees(upi),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val notes = listOfNotNull(
                                c.dueDay?.let { "Bill due on the ${Fmt.ordinal(it)}" },
                                if (c.ccUpiDefault) "Default for CC UPI" else null
                            )
                            if (notes.isNotEmpty()) {
                                Text(
                                    notes.joinToString(" · "),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card)) }
    }

    if (showAdd) {
        AddCardDialog(state.cards, onAdd = { code, name -> state.addCard(code, name) }, onDismiss = { showAdd = false })
    }
    editTarget?.let { c ->
        EditCardDialog(
            card = c,
            onSave = { name, due, default -> state.updateCard(c.code, name, due, default) },
            onDelete = {
                editTarget = null
                val users = state.recurringUsing(c.code)
                if (users.isNotEmpty()) inUse = c to users.map { it.name } else deleteTarget = c
            },
            onDismiss = { editTarget = null }
        )
    }
    inUse?.let { (c, names) ->
        InfoDialog(
            stringResource(R.string.card_in_use, c.code.uppercase()) + " (" + names.joinToString(", ") + ")"
        ) { inUse = null }
    }
    deleteTarget?.let { c ->
        ConfirmDialog(
            title = stringResource(R.string.delete_card_title),
            message = stringResource(R.string.delete_card_message, c.name, c.code.uppercase()),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                deleteTarget = null
                if (!state.deleteCard(c.code)) failed = true
            },
            onDismiss = { deleteTarget = null }
        )
    }
    if (failed) InfoDialog(stringResource(R.string.error_save_failed)) { failed = false }
}
