package com.sharath.statusbartotal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.EntryParser
import com.sharath.statusbartotal.core.Recurring
import com.sharath.statusbartotal.core.RecurringRules
import com.sharath.statusbartotal.core.RecurringType
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.Card
import com.sharath.statusbartotal.data.Cards
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun categoryLabel(c: Category): String = stringResource(
    when (c) {
        Category.UPI -> R.string.cat_upi
        Category.CC -> R.string.cat_cc
        Category.CCUPI -> R.string.cat_ccupi
    }
)

@Composable
fun typeLabel(t: RecurringType): String = stringResource(
    when (t) {
        RecurringType.EMI -> R.string.type_emi
        RecurringType.AUTOPAY -> R.string.type_autopay
        RecurringType.SUBSCRIPTION -> R.string.type_subscription
    }
)

fun categoryColor(c: Category): Color = when (c) {
    Category.UPI -> AppColors.Upi
    Category.CC -> AppColors.Cc
    Category.CCUPI -> AppColors.CcUpi
}

/** UPI / CC / CC UPI chips in bucket colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(selected: Category, onSelect: (Category) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in Category.entries) {
            val color = categoryColor(c)
            FilterChip(
                selected = c == selected,
                onClick = { onSelect(c) },
                label = { Text(categoryLabel(c)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = c == selected, borderColor = color
                )
            )
        }
    }
}

/**
 * Card dropdown; [selected] is a card code or null. With [allowNone] a "No card" item comes
 * first; otherwise [placeholder] is shown while nothing is chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDropdown(
    cards: List<Card>, selected: String?, onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier, allowNone: Boolean = true, placeholder: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val noCard = placeholder ?: stringResource(R.string.no_card)
    val current = cards.firstOrNull { it.code == selected }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = current?.let { it.name + " (" + it.code.uppercase() + ")" } ?: noCard,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Card") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNone) {
                DropdownMenuItem(text = { Text(noCard) }, onClick = { onSelect(null); expanded = false })
            }
            for (c in cards) {
                DropdownMenuItem(
                    text = { Text(c.name + " (" + c.code.uppercase() + ")") },
                    onClick = { onSelect(c.code); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String, message: String, confirmText: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun InfoDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } }
    )
}

@Composable
private fun ErrorText(msg: String?) {
    if (msg != null) Text(msg, color = MaterialTheme.colorScheme.error)
}

/** Settings: auto-reset day (Off or 1-31) and monthly budget (blank = Off). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentDay: Int?, currentBudget: Long?, currentShowLeft: Boolean,
    onSave: (Int?, Long?, Boolean) -> Unit, onDismiss: () -> Unit
) {
    var day by rememberSaveable { mutableStateOf(currentDay) }
    var budgetText by rememberSaveable { mutableStateOf(currentBudget?.toString() ?: "") }
    var showLeft by rememberSaveable { mutableStateOf(currentShowLeft) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto-reset day. On that day of the month the total resets to ₹0 at midnight.")
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = day?.toString() ?: "Off",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Auto-reset day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Off") }, onClick = { day = null; expanded = false })
                        for (d in 1..31) {
                            DropdownMenuItem(
                                text = { Text(d.toString()) },
                                onClick = { day = d; expanded = false }
                            )
                        }
                    }
                }
                Text("Monthly budget in rupees. Leave empty for Off.")
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it; error = null },
                    singleLine = true,
                    label = { Text("Monthly budget (₹)") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Status bar shows")
                val hasBudget = budgetText.isNotBlank()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !showLeft || !hasBudget, onClick = { showLeft = false })
                    Text("Total spent", Modifier.clickable { showLeft = false })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = showLeft && hasBudget,
                        onClick = { showLeft = true },
                        enabled = hasBudget
                    )
                    Text(
                        if (hasBudget) "Budget left" else "Budget left (set a budget first)",
                        Modifier.clickable(enabled = hasBudget) { showLeft = true }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (budgetText.isBlank()) {
                    onSave(day, null, false)
                } else {
                    val b = EntryParser.parse(budgetText)?.takeIf { it > 0 }
                    if (b == null) error = "Budget must be a positive whole number."
                    else onSave(day, b, showLeft)
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Manual reset confirmation with a "Save report" checkbox (default on). */
@Composable
fun ResetDialog(total: Long, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var save by rememberSaveable { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reset_message, RupeeFormat.exact(total)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = save, onCheckedChange = { save = it })
                    Text("Save report to Downloads/StatusBarTotal")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(save) }) { Text(stringResource(R.string.reset)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Edit a saved card: name, bill due day, CC UPI default switch, Delete. The code is fixed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: Card, onSave: (String, Int?, Boolean) -> Boolean,
    onDelete: () -> Unit, onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(card.name) }
    var due by rememberSaveable { mutableStateOf(card.dueDay) }
    var default by rememberSaveable { mutableStateOf(card.ccUpiDefault) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val nameErr = stringResource(R.string.error_card_name)
    val saveErr = stringResource(R.string.error_save_failed)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit card ${card.code.uppercase()}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Code ${card.code.uppercase()} cannot be changed. To use another code, add a new card.")
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_card_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = due?.toString() ?: "Off",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Bill due day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Off") }, onClick = { due = null; expanded = false })
                        for (d in 1..31) {
                            DropdownMenuItem(
                                text = { Text(d.toString()) },
                                onClick = { due = d; expanded = false }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Default for CC UPI", modifier = Modifier.weight(1f))
                    Switch(checked = default, onCheckedChange = { default = it })
                }
                ErrorText(error)
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = Cards.cleanName(name)
                error = when {
                    n == null -> nameErr
                    !onSave(n, due, default) -> saveErr
                    else -> null
                }
                if (error == null) onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun AddCardDialog(
    existing: List<Card>, onAdd: (String, String) -> Boolean, onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val codeErr = stringResource(R.string.error_card_code)
    val existsErr = stringResource(R.string.error_card_exists)
    val nameErr = stringResource(R.string.error_card_name)
    val saveErr = stringResource(R.string.error_save_failed)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_card_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code, onValueChange = { code = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_card_code)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_card_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val c = code.trim().lowercase()
                val n = Cards.cleanName(name)
                error = when {
                    !Cards.isValidCode(c) -> codeErr
                    existing.any { it.code == c } -> existsErr
                    n == null -> nameErr
                    !onAdd(c, n) -> saveErr
                    else -> null
                }
                if (error == null) onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(
    cards: List<Card>, ccUpiDefault: String?, onAdd: (Recurring) -> Boolean, onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(RecurringType.SUBSCRIPTION) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.UPI) }
    var card by rememberSaveable { mutableStateOf<String?>(null) }
    var dayText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val amount = EntryParser.parse(amountText)?.takeIf { it > 0 }
    val day = dayText.trim().toIntOrNull()?.takeIf { it in 1..31 }
    val passed = amount != null && day != null &&
        !RecurringRules.dueDate(day, YearMonth.now()).isAfter(LocalDate.now())
    val saveLabel = if (passed) stringResource(R.string.save_and_add_now, RupeeFormat.exact(amount!!))
    else stringResource(R.string.save)

    val errName = stringResource(R.string.error_name)
    val errAmount = stringResource(R.string.error_recurring_amount)
    val errDay = stringResource(R.string.error_day)
    val errMonths = stringResource(R.string.error_months)
    val errCard = stringResource(R.string.error_card_missing)
    val errNeedCard = stringResource(R.string.error_need_card_recurring)
    val effectiveCard = if (category == Category.UPI) null else card
        ?: if (category == Category.CCUPI) ccUpiDefault else null
    val errSave = stringResource(R.string.error_save_failed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_recurring_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in RecurringType.entries) {
                        FilterChip(
                            selected = t == type, onClick = { type = t },
                            label = { Text(typeLabel(t)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_recurring_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryChips(category) { category = it; card = null }
                if (category != Category.UPI) {
                    CardDropdown(
                        cards, effectiveCard, { card = it }, allowNone = false,
                        placeholder = if (cards.isEmpty()) "Add a card first" else "Choose a card"
                    )
                }
                OutlinedTextField(
                    value = dayText, onValueChange = { dayText = it }, singleLine = true,
                    label = { Text(stringResource(R.string.hint_day)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == RecurringType.EMI) {
                    OutlinedTextField(
                        value = monthsText, onValueChange = { monthsText = it }, singleLine = true,
                        label = { Text(stringResource(R.string.hint_months)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanName = Cards.cleanName(name)
                val months = monthsText.trim().toIntOrNull()?.takeIf { it in 1..360 }
                val cardCode = if (category == Category.UPI) null else effectiveCard
                error = when {
                    cleanName == null -> errName
                    amount == null -> errAmount
                    day == null -> errDay
                    type == RecurringType.EMI && months == null -> errMonths
                    category != Category.UPI && cardCode == null -> errNeedCard
                    cardCode != null && cards.none { it.code == cardCode } -> errCard
                    else -> null
                }
                if (error == null) {
                    val item = Recurring(
                        id = System.currentTimeMillis(), type = type, amount = amount!!,
                        category = category, card = cardCode, day = day!!,
                        startMonth = YearMonth.now(),
                        months = if (type == RecurringType.EMI) months else null,
                        name = cleanName!!
                    )
                    if (onAdd(item)) onDismiss() else error = errSave
                }
            }) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
