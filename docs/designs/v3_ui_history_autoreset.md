# V3: New UI, History and Undo, Upcoming Dues, Auto-Reset, Pie Chart

*   **Author:** Sharath
*   **Status:** Approved (2026-11-23)
*   **Builds on:** V1 (`statusbar_running_total.md`) and V2
    (`v2_categories_recurring.md`), both working on the phone. Their
    "Implementation Decisions" sections still apply unless changed here.
    Notification, status bar icon (thousands, numbers only), parser grammar,
    receivers and file formats stay as they are except where stated.

## What the user gets

A colourful, modern app with four tabs, plus three new functions:

1.  **History with undo**: every entry ever made, newest first, grouped by
    billing cycle, filterable by bucket. Tapping an entry offers "Undo", which
    adds the opposite amount and marks both as undone. Nothing is deleted.
2.  **Upcoming dues**: on Home, the recurring payments still due this month,
    with dates and a total.
3.  **Auto-reset day**: the user picks a day of the month (or Off). On that
    day the total resets to ₹0 automatically. Manual Reset stays.
4.  **Pie chart**: on Home, the split of the current total by bucket.

## UI toolkit

Rebuild the screen with **Jetpack Compose + Material 3** (AndroidX). This is
the only new dependency set: Compose BOM, `material3`, `ui`, `ui-tooling-preview`
(debug tooling optional), `activity-compose`, and
`material-icons-extended` is NOT added (use `material-icons-core` only if
needed, or plain text/emoji-free shapes). Use the Kotlin Compose compiler
Gradle plugin matching the Kotlin version that AGP 9.5.2's built-in Kotlin
uses. No other libraries; the pie chart is drawn with Compose `Canvas`.
`MainActivity` becomes a `ComponentActivity`. No new permissions.

## Visual design

*   **Mood**: a money dashboard. Bold, colourful, calm. Large numbers.
*   **Theme**: custom Material 3 colour scheme (not wallpaper dynamic colour),
    light and dark variants following the system setting.
    *   Primary: deep indigo `#3F3DBA` (dark theme `#A9A7FF`).
    *   Hero header: vertical gradient indigo `#3F3DBA` → violet `#7B3FE4`
        with white text (same gradient in dark theme, slightly darker:
        `#2C2A8F` → `#5A2DB0`).
    *   Bucket colours (chips, pie slices, row dots, same in both themes):
        UPI teal `#00A896`, CC coral `#F2545B`, CC UPI amber `#F4A300`; cards
        cycle through `#3D7DF6`, `#9B5DE5`, `#00B4D8`, `#F15BB5`, `#6A994E`,
        `#E07A5F` by card code sorted alphabetically.
    *   Negative amounts in red `#D32F2F` (dark `#FF6B6B`); undone entries
        grey with strikethrough.
*   **Shapes and type**: cards with 20 dp corners, 16 dp screen padding, 12 dp
    gaps; total in 44 sp bold; section titles 18 sp semibold.
*   **Navigation**: Material 3 `NavigationBar` at the bottom with four tabs:
    Home, History, Recurring, Cards (text labels with simple Material icons
    from `material-icons-core`: Home, List, Refresh, CreditCard-like; if an
    icon is not in core, use `Icons.Default` alternatives such as `Star`,
    `DateRange`, `AccountBox`).
*   Edge-to-edge with proper insets (`Scaffold` handles them); the keyboard
    must not cover input fields (`imePadding`).

## Screens

### Home

1.  Notification-off banner (V1 behaviour) as a red-tinted card with
    "Open settings", only when notifications cannot be shown.
2.  **Hero card** (gradient): "Total this cycle", the exact total
    (₹14,500), the cycle line ("Since 15 Nov · resets 15 Dec" or "Since
    15 Nov · auto-reset off"), and a Reset button (outlined, white). Reset
    keeps V2 behaviour: catch-up first, then confirmation dialog.
3.  **Quick add card**: amount field (phone keypad, as V2), three
    `FilterChip`s UPI / CC / CC UPI (UPI default) in bucket colours, a card
    dropdown (`ExposedDropdownMenuBox`, "No card" first) shown for CC and CC
    UPI, and an "Add" button. Errors under the field. After adding, a
    `Snackbar` "Added ₹450 to UPI" with an "Undo" action (same undo as
    History).
4.  **Pie chart card**: donut chart (Canvas arcs, 28 dp stroke) of positive
    bucket totals for the current cycle, with the total in the centre, and a
    legend: colour dot, bucket name, amount, percentage. Buckets with zero or
    negative totals are left out of the chart but listed in the legend with
    their amount. If nothing positive, show "No spending yet this cycle".
5.  **Upcoming this month card**: rows "5 Dec · Netflix · Subscription ·
    ₹649 · UPI" for recurring payments not yet added this month whose due date
    is after today, sorted by date, and a footer "₹2,149 still due this
    month". EMIs show "(3 left)". If none: "Nothing else due this month".
6.  "Keep the number visible" checklist card (V1), collapsed by default
    (expand arrow), with "Open app settings".

### History

*   Top: bucket filter chips (All, UPI, CC, CC UPI, each card) scrolling
    horizontally.
*   List (`LazyColumn`), newest first, grouped under cycle headers
    ("15 Nov – now · ₹14,500", "15 Oct – 15 Nov · ₹22,310"). A cycle starts at
    the beginning of the ledger or at a reset line; header shows the cycle's
    total for the current filter.
*   Each row: coloured dot, amount (signed, red if negative), bucket name,
    date and time ("23 Nov, 9:41 pm"), and a small label "Auto · Netflix" for
    recurring entries or "Undo of ₹450" for undo entries. Reset lines appear
    as a divider row "Reset (manual)" or "Reset (auto)".
*   Tap a normal entry that is not undone → dialog "Undo ₹450 (UPI, 23 Nov)?
    This adds -₹450 and keeps both records." Undone entries and undo entries
    show greyed with strikethrough and cannot be tapped.

### Recurring

V2 list and Add dialog, restyled as cards: name, type badge, amount, bucket
chip, "Every 5th", "3 months left" / "Finished". Floating action button "+"
opens the add dialog. Delete via a trailing icon with the V2 confirmation.

### Cards

V2 list and Add dialog, restyled: card tiles with the card's colour as a left
stripe, code in caps, name, and the card's current-cycle total. "+" FAB adds.
Delete keeps V2 rules (blocked while used by a recurring payment).

### Settings (inside Home, via a gear icon in the top app bar)

A dialog: "Auto-reset day": Off, or 1-31 (a picker). Month-end clamping as
for recurring payments. Saved in `settings.txt` (see Storage).

## History and undo: data rules

*   **Entry id** = its line number in `ledger.txt` (1-based, counting every
    complete line). The file is append-only and only a cut-off, never-counted
    last piece can be trimmed, so line numbers of complete lines never change.
*   **Undo** appends `E,<now>,<-amount>,<same category>,<same card>,undo#<id>`.
    The original entry with id `<id>` is shown as undone. An entry can be
    undone once: the app refuses if an `undo#<id>` line already exists. Undo
    entries themselves cannot be undone.
*   Undoing an automatic recurring entry is allowed; its month stays marked as
    added (its `<id>@<month>` line still exists), so it is not added again.
*   `undo#…` sources are not recurring sources: `appliedMonths` only parses
    sources containing `@`.
*   Snapshot/total rules are unchanged: an undo entry is just a negative entry
    in its bucket.
*   New `Ledger.history(): List<HistoryItem>` reads the whole file once:
    `HistoryItem.Entry(id, timeMillis, amount, category, card, source,
    undone: Boolean)` and `HistoryItem.Reset(id, timeMillis, auto: Boolean)`.

## Auto-reset: rules

*   **Setting**: `autoResetDay` = null (off) or 1-31. Stored in `settings.txt`
    as one line `autoResetDay=<n>` or `autoResetDay=`; written atomically like
    cards (V2 `AtomicWrite`). Missing file = off.
*   **Reset moment**: 00:00 local time on `dueDate(day, month)` (V2 clamping).
*   **Reset lines** gain an optional third field: `R,<time>,auto@<yyyy-MM>`
    for automatic resets; manual resets stay `R,<time>`. V2 parsing of `R`
    must accept 2 or 3 fields.
*   **When turned on**: the first automatic reset is the next reset moment
    strictly after the time the setting is saved. Past months are never
    reset retroactively. Save the "enabled since" time in `settings.txt` as
    `autoResetSince=<millis>`.
*   **Catch-up order** (extends V2 `catchUpAndSchedule`): build one list of due
    events, each with its due instant:
    *   recurring months missing (V2 rules), due at 00:05 on their due date;
    *   auto-resets missing: every reset moment after `autoResetSince` and not
        after now, whose `auto@<yyyy-MM>` line is not in the ledger.
    Sort by due instant (reset at 00:00 comes before a recurring payment at
    00:05 on the same day, so that payment counts in the new cycle), then
    append in that order. Stop at the first failed write (V2 rule).
*   **Alarm**: the single V2 alarm is set for the earliest of the next
    recurring due instant and the next auto-reset moment (strictly after now;
    V2 fallback now + 15 min if not).
*   **Manual Reset** (V2 flow) is unchanged and does not move the auto-reset
    schedule.
*   **Cycle line on Home**: "Since <date of last reset line, or first entry>"
    and "resets <next auto-reset date>" or "auto-reset off".

## Upcoming dues: rules

For each recurring item that is not finished: this month's due date
(`dueDate(day, currentMonth)`); include it if it is strictly after today and
its month is not applied. Amount = item amount. Sorted by due date. Total =
sum. Computed on screen refresh only (no storage).

## Code layout

*   Keep `core/`, `data/`, `notify/`, `receivers/`, `RecurringRunner.kt` and
    extend them as above.
*   New `ui/` package: `Theme.kt` (colours, typography, shapes),
    `MainScreen.kt` (Scaffold + NavigationBar + tab state), `HomeTab.kt`,
    `HistoryTab.kt`, `RecurringTab.kt`, `CardsTab.kt`, `PieChart.kt`,
    `Dialogs.kt`, `AppState.kt` (a plain class that loads ledger snapshot,
    history, cards, recurring, settings; exposes actions add/undo/reset/
    addCard/deleteCard/addRecurring/deleteRecurring/setAutoResetDay; every
    action calls `TotalNotifier.refresh` and reloads). No ViewModel library
    required; state held with `remember`/`mutableStateOf` in the activity's
    composition, reloaded in `onResume`.
*   Delete the V2 XML layouts (`activity_main.xml`, `dialog_recurring.xml`,
    `dialog_card.xml`) once replaced. Keep `strings.xml` for user text.
*   Keep the permission request in `onCreate` (only when
    `savedInstanceState == null`) and refresh after the answer.

## Tests (JVM)

*   `LedgerTest`: line ids, `history()` with undo marking, refusing a second
    undo, reset lines with and without `auto@`, V1/V2/V3 lines mixed.
*   `RecurringTest` / new `AutoResetTest`: reset moments with clamping,
    no retroactive resets before `autoResetSince`, combined ordering (reset
    00:00 before recurring 00:05 same day), catch-up across several months
    with both kinds, no duplicates when run twice, next alarm = earliest.
*   `UpcomingTest`: due-after-today filter, applied months excluded, finished
    EMI excluded.
*   UI is checked on the phone.

## Not in V3

Budget limit, monthly comparison, card statement reminders, export, Quick
Settings tile and home-screen shortcut (all still available later on
request).
