package com.sharath.statusbartotal.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.core.AutoReset
import com.sharath.statusbartotal.core.Budget
import com.sharath.statusbartotal.core.BudgetLevel
import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.EntryParser
import com.sharath.statusbartotal.core.RecurringType
import com.sharath.statusbartotal.core.RupeeFormat
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun HomeTab(
    state: AppState, snackbar: SnackbarHostState, onOpenCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    var resetTotal by remember { mutableStateOf<Long?>(null) }
    var resetFailed by remember { mutableStateOf(false) }
    var reportFailed by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.canNotify) item { NotificationBanner() }
        item { HeroCard(state, onReset = { resetTotal = state.prepareReset() }) }
        item { QuickAddCard(state, snackbar, onOpenCards) }
        item { PieCard(state) }
        item { UpcomingCard(state) }
        item { KeepVisibleCard() }
    }

    resetTotal?.let { total ->
        ResetDialog(
            total = total,
            onConfirm = { save ->
                resetTotal = null
                val r = state.reset(save)
                if (!r.ok) resetFailed = true else if (r.reportFailed) reportFailed = true
            },
            onDismiss = { resetTotal = null }
        )
    }
    if (resetFailed) InfoDialog(stringResource(R.string.error_save_failed)) { resetFailed = false }
    if (reportFailed) InfoDialog("Couldn't save the report") { reportFailed = false }
}

@Composable
private fun NotificationBanner() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33D32F2F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.banner_text), color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }) { Text(stringResource(R.string.open_settings)) }
        }
    }
}

@Composable
private fun HeroCard(state: AppState, onReset: () -> Unit) {
    val snap = state.snapshot
    val autoDay = state.settings.autoResetDay
    val since = snap.cycleStart?.let { "Since " + Fmt.date(it) } ?: "No entries yet"
    val resets = if (autoDay != null)
        "resets " + Fmt.date(AutoReset.nextAfter(autoDay, LocalDateTime.now()).toLocalDate())
    else "auto-reset off"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(heroBrush())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Total this cycle", color = Color.White.copy(alpha = 0.85f))
            Text(
                Fmt.rupees(snap.total), color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
            Text("$since · $resets", color = Color.White.copy(alpha = 0.85f))
            val budget = state.settings.budget
            if (budget != null) BudgetBar(snap.total, budget)
            Spacer(Modifier.size(4.dp))
            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) { Text(stringResource(R.string.reset)) }
        }
    }
}

@Composable
private fun BudgetBar(total: Long, budget: Long) {
    val level = Budget.level(total, budget)
    val barColor = when (level) {
        BudgetLevel.OK -> Color.White
        BudgetLevel.WARN -> AppColors.Amber
        BudgetLevel.OVER -> Color(0xFFFF6B6B)
    }
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(Budget.heroLine(total, budget), color = if (level == BudgetLevel.OK) Color.White else barColor)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(Budget.fraction(total, budget))
                    .height(8.dp)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun QuickAddCard(state: AppState, snackbar: SnackbarHostState, onOpenCards: () -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.UPI) }
    var card by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val amountErr = stringResource(R.string.error_amount_main)
    val saveErr = stringResource(R.string.error_save_failed)

    // CC and CC UPI always need a card: the chosen one, else the last-used CC card or the CC UPI default.
    val effectiveCard: String? = when (category) {
        Category.UPI -> null
        Category.CC -> card?.takeIf { c -> state.cards.any { it.code == c } } ?: state.lastCcCard
        Category.CCUPI -> card?.takeIf { c -> state.cards.any { it.code == c } }
            ?: state.ccUpiDefault?.code
    }
    val canAdd = category == Category.UPI || effectiveCard != null

    fun submit() {
        val amount = EntryParser.parse(amountText)
        if (amount == null) {
            error = amountErr
            return
        }
        val cardCode = effectiveCard
        if (category != Category.UPI && cardCode == null) return
        val id = state.add(amount, category, cardCode)
        if (id == null) {
            error = saveErr
            return
        }
        error = null
        amountText = ""
        val label = Fmt.bucketLabel(
            if (cardCode != null) "card:$cardCode" else category.name, state.cardNames
        )
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                "Added ${Fmt.rupees(amount)} to $label",
                actionLabel = "Undo", duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) state.undo(id)
        }
    }

    SectionCard {
        Text("Quick add", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = amountText, onValueChange = { amountText = it; error = null },
            singleLine = true,
            label = { Text(stringResource(R.string.hint_amount)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        CategoryChips(category) { category = it; card = null }
        if (category != Category.UPI) {
            if (state.cards.isEmpty()) {
                Text("Add a card first", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onOpenCards) { Text("Go to Cards") }
            } else {
                CardDropdown(
                    state.cards, effectiveCard, { card = it }, Modifier.fillMaxWidth(),
                    allowNone = false, placeholder = "Choose a card"
                )
                if (category == Category.CCUPI && effectiveCard == null) {
                    Text("Set a CC UPI card in Cards", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Button(onClick = { submit() }, enabled = canAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add))
        }
    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun PieCard(state: AppState) {
    val buckets = state.snapshot.buckets
    val codes = state.cards.map { it.code }
    val keys = buckets.keys.sortedWith(
        compareBy({ when (it) { "UPI" -> 0; "CC" -> 1; "CCUPI" -> 2; else -> 3 } }, { it })
    )
    val positive = keys.filter { (buckets[it] ?: 0L) > 0L }
    val positiveSum = positive.sumOf { buckets.getValue(it) }
    SectionCard {
        Text("Spending by bucket", style = MaterialTheme.typography.titleMedium)
        if (positive.isEmpty()) {
            Text("No spending yet this cycle", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PieChart(
                    slices = positive.map { PieSlice(buckets.getValue(it), AppColors.bucket(it, codes)) },
                    centerText = Fmt.rupees(state.snapshot.total)
                )
            }
        }
        for (key in keys) {
            val v = buckets.getValue(key)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AppColors.bucket(key, codes))
                )
                Spacer(Modifier.width(8.dp))
                Text(Fmt.bucketLabel(key, state.cardNames), Modifier.weight(1f))
                Text(
                    Fmt.rupees(v),
                    color = if (v < 0) negativeColor() else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (v > 0 && positiveSum > 0)
                        String.format(Locale.ENGLISH, "%d%%", Math.round(v * 100.0 / positiveSum))
                    else "-",
                    modifier = Modifier.width(52.dp), textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpcomingCard(state: AppState) {
    SectionCard {
        Text("Upcoming this month", style = MaterialTheme.typography.titleMedium)
        if (state.upcoming.isEmpty()) {
            Text("Nothing else due this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            for (u in state.upcoming) {
                val r = u.item
                val cat = Fmt.bucketLabel(
                    if (r.card != null) "card:${r.card}" else r.category.name, state.cardNames
                )
                val type = when (r.type) {
                    RecurringType.EMI -> "EMI"
                    RecurringType.AUTOPAY -> "Autopay"
                    RecurringType.SUBSCRIPTION -> "Subscription"
                }
                val left = u.monthsLeft?.let { " ($it left)" } ?: ""
                Text(
                    "${Fmt.date(u.date)} · ${r.name} · $type · ${Fmt.rupees(r.amount)} · $cat$left"
                )
            }
            Text(
                "${Fmt.rupees(state.upcoming.sumOf { it.item.amount })} still due this month",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun KeepVisibleCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.heading_keep_visible),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }
        if (expanded) {
            Text(stringResource(R.string.keep_visible_steps))
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }) { Text(stringResource(R.string.open_app_settings)) }
        }
    }
}
