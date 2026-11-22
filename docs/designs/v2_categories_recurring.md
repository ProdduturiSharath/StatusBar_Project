# V2: Categories, Cards, Recurring Payments, Reset

*   **Author:** Sharath
*   **Status:** Proposed
*   **Date:** 2026-11-23
*   **Builds on:** `statusbar_running_total.md` (V1, approved and working on the
    phone). Everything in V1 stays unless this document changes it.

## What changes for the user

1.  **Every entry has a category**: UPI, CC, or CC UPI.
2.  **Cards**: the user saves credit cards once, each with a 3-letter code and
    a name (for example `abc` = "HDFC Millennia"). An entry tagged with a card
    counts toward **that card**, not toward the CC or CC UPI bucket.
3.  **Recurring payments**: EMI, Autopay, or Subscription. Each has a name, an
    amount, a category (and optional card), and a day of the month. The app
    adds the amount automatically every month on that day. An EMI also has
    "months left" and stops by itself after that many additions.
4.  **Reset** on the main screen sets the total to ₹0. Recurring payments and
    cards are kept; recurring payments keep adding on their days.
5.  The status bar still shows **one number**: the total of everything since
    the last reset (thousands, numbers only, as in V1).
6.  The expanded notification shows the split by bucket.

## Buckets

Every entry counts toward exactly one bucket:

| Entry | Bucket |
| --- | --- |
| UPI | UPI |
| CC, no card | CC |
| CC UPI, no card | CC UPI |
| CC with card ABC | card ABC |
| CC UPI with card ABC | card ABC |

The status bar number is the sum of all buckets. Buckets with ₹0 are not shown
in the breakdown.

## Notification "+ Add" grammar

The text is split on spaces. The first word is the amount (V1 rules: optional
sign, digits, commas allowed, no decimals, 1 to 9 digits, not zero; Unicode
minus and non-breaking spaces handled as in V1). Then up to two more words,
case-insensitive:

| Typed | Result |
| --- | --- |
| `450` | UPI |
| `450 upi` | UPI |
| `450 cc` | CC, no card |
| `450 ccupi` | CC UPI, no card |
| `450 abc` | CC on card ABC |
| `450 cc abc` | CC on card ABC |
| `450 ccupi abc` | CC UPI on card ABC |
| `-200 abc` | correction on card ABC |

Rejected, with a message in the notification:
*   Amount invalid: "Type an amount like 450, 450 cc, 450 ccupi or 450 abc."
*   Unknown word that is not a saved card: "No card 'xyz'. Add it in the app."
*   `upi` followed by a card, or more than three words: same as amount invalid.

A card code is exactly 3 letters a-z, stored lowercase, unique, and cannot be
`upi` or `cc` (or start a conflict with `ccupi`, which is 5 letters, so it
cannot clash).

## Main screen (replaces the V1 milestone 2 screen)

Top to bottom, built with plain Android views and built-in dialogs:

1.  Notification banner (V1).
2.  **Total** (exact, ₹) and the **breakdown** list: one row per non-zero
    bucket ("UPI ₹4,100", "HDFC Millennia (ABC) ₹9,200", ...).
3.  **Add entry**: amount field; category choice UPI / CC / CC UPI (UPI
    selected by default); a card picker shown when CC or CC UPI is selected,
    with "No card" first; Add button. Same validation as V1, plus card rules.
4.  **Recurring payments**: list rows "Netflix · Subscription · ₹649 · UPI ·
    every 5th" and for EMIs "· 7 months left". Each row has Delete (with
    confirmation). "Add recurring" opens a dialog: name, type (EMI / Autopay /
    Subscription), amount (positive only), category, card (optional), day of
    month (1-31), and months left (EMI only, 1-360). To change one, delete it
    and add it again.
5.  **Cards**: list rows "ABC · HDFC Millennia" with Delete (with
    confirmation). "Add card": code (3 letters) and name. Deleting a card keeps
    its past entries; they show as "ABC (deleted)" in the breakdown. A card
    used by a recurring payment cannot be deleted until that recurring payment
    is deleted (the app says so).
6.  **Reset** with confirmation: "Reset ₹14,500 to ₹0? Entries, cards and
    recurring payments are kept."
7.  "Keep the number visible" checklist (V1).

## Recurring rules

*   **Due date**: the chosen day in each month. If the month is shorter (day
    31 in a 30-day month, or 29-31 in February), it is the month's last day.
*   **First month**: the month the recurring payment is created in, even if
    the day has already passed. In that case it is added immediately (user's
    choice: "add once now too"). Then every following month.
*   **Amount added** is the amount at the time of adding. Recurring amounts are
    positive.
*   **EMI months left**: N means the EMI is added N more times, counting the
    first one. When all N have been added, the EMI shows "Finished" and adds
    nothing more; the user can delete it.
*   **Never twice**: every automatic entry records which recurring payment and
    which month it is for. Before adding, the app checks the ledger for that
    pair; if it is already there, it does not add again. This makes the
    catch-up safe to run any number of times.
*   **Catch-up**: on every wake-up (app open, notification reply, dismiss,
    boot, update, and the alarm below) the app first adds every due month that
    is missing, oldest first, then refreshes the notification. If the phone
    was off on the 5th, the payment is added the next time the app runs.
*   **Reset and recurring**: Reset does not change recurring payments. A
    payment due after a reset counts in the new total. A catch-up that runs
    after a reset for a month whose due date was before the reset still adds
    that entry (it was owed), and it counts in the new total.

## The alarm (the only background work)

*   After every catch-up the app sets **one** alarm, for 00:05 local time on
    the earliest upcoming due date among active recurring payments, using
    `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, ...)` with an explicit,
    immutable pending intent to a non-exported `RecurringAlarmReceiver`. This
    alarm type needs no permission. It may fire some minutes late; that is
    acceptable.
*   The receiver runs catch-up and refresh, which sets the next alarm.
*   Alarms are lost on reboot and force-stop. Boot and update already run
    catch-up, which sets the alarm again. After a force-stop, nothing runs
    until the app is opened (V1 limit); catch-up then adds what was missed.
*   If there are no active recurring payments, any set alarm is cancelled.
*   Manifest: no new permission. New receiver `RecurringAlarmReceiver`
    (`exported="false"`).

## Storage

All files are in app-private storage, as in V1. No database is added.

*   **`ledger.txt`** (append-only, V1 rules for cut-off lines and failed
    writes). Entry lines gain fields:
    `E,<time>,<amount>,<category>,<card>,<source>`
    *   `<category>`: `UPI`, `CC`, or `CCUPI`.
    *   `<card>`: the 3-letter code or empty.
    *   `<source>`: empty for manual entries, or `<recurringId>@<yyyy-MM>` for
        automatic ones.
    *   V1 lines `E,<time>,<amount>` are read as UPI, no card, manual.
    *   `R,<time>` (reset) is unchanged.
*   **`cards.txt`** and **`recurring.txt`**: small lists that can change
    (add, delete), so they are rewritten whole: write to a temporary file in
    the same folder, force it to disk, then rename it over the old file (an
    atomic replace). One line per item:
    *   card: `<code>,<name>` (name has commas removed on save).
    *   recurring: `<id>,<type>,<amount>,<category>,<card>,<day>,<startMonth yyyy-MM>,<months or empty>,<name>`
        where `<id>` is the creation time in milliseconds and `<name>` is last
        (commas removed on save).
*   **Months left** is not stored as a changing number. It is computed as
    `<months> - (number of ledger entries with source <id>@...)`, so it can
    never drift from what was actually added.

## Code changes (for the implementer)

*   `core/EntryParser.kt`: new `parseEntry(text, knownCards): ParsedEntry?`
    returning amount, category, card, or a rejection reason; V1 `parse` stays
    for the amount word.
*   `core/Category.kt` (new): enum `UPI, CC, CCUPI`; `core/RecurringType.kt`
    (new): enum `EMI, AUTOPAY, SUBSCRIPTION`.
*   `core/Recurring.kt` (new, pure Kotlin): due-date calculation
    (`dueDate(day, yearMonth)` with month-end clamping), and
    `missingEntries(recurring, appliedMonths, today): List<YearMonth>` honoring
    the start month and EMI months. Uses `java.time` (available on API 33).
*   `data/Ledger.kt`: new entry format, V1 lines read as UPI; `snapshot()`
    returns total, last entry, per-bucket totals since the last reset, and the
    set of applied `<id>@<month>` sources (over the whole file, not only since
    reset).
*   `data/Cards.kt`, `data/RecurringStore.kt` (new): read and atomically
    rewrite their files.
*   `RecurringRunner.kt` (new): `catchUpAndSchedule(context)`: read stores and
    ledger, append missing entries, set or cancel the alarm.
*   `TotalNotifier.refresh()`: calls `catchUpAndSchedule` first; notification
    text shows the breakdown (big-text style so several buckets fit), with the
    rejection message or "Last entry" as the first line.
*   `receivers/RecurringAlarmReceiver.kt` (new); manifest entry.
*   `MainActivity.kt` + `res/layout/activity_main.xml` + dialog layouts +
    `strings.xml`: the main screen above.
*   Tests (JVM): parser grammar table above; due dates (31st in February, leap
    year, 30-day months); missing-months logic (first month already passed,
    catch-up over several months, EMI stops at N, no duplicates when run
    twice); ledger reading of V1 and V2 lines and bucket totals; card code
    validation.

## Implementation Decisions (from the review, 2026-11-23)

These settle every open point. Where they differ from the sections above,
these win.

**Order of work on every wake-up.** `TotalNotifier.refresh(context, message)`
calls `RecurringRunner.catchUpAndSchedule(context)` as its very first statement,
before the `canNotify` guard, so automatic entries are added even when
notifications are off. Every receiver only calls `TotalNotifier.refresh`
(nothing runs catch-up twice). All file work is synchronous on the main thread;
no threads or coroutines.

**Types.**
*   `enum class Category { UPI, CC, CCUPI }`,
    `enum class RecurringType { EMI, AUTOPAY, SUBSCRIPTION }`.
*   `data class Recurring(id: Long, type: RecurringType, amount: Long,
    category: Category, card: String?, day: Int, startMonth: YearMonth,
    months: Int?, name: String)`.
*   `Ledger.addEntry(amount: Long, timeMillis: Long, category: Category,
    card: String?, source: String?): Boolean`; `Ledger.reset(timeMillis): Boolean`.
*   `Ledger.Snapshot(total: Long, lastEntry: Long?, buckets: Map<String, Long>,
    appliedSources: Set<String>)`. Bucket keys: `"UPI"`, `"CC"`, `"CCUPI"`,
    `"card:<code>"`. A card entry never goes into `CC` or `CCUPI`. `total`,
    `lastEntry` and `buckets` cover entries after the last reset (automatic
    entries included); `appliedSources` covers the whole file.
*   Ledger line parsing uses `split(",", limit = -1)`. `E` with 3 parts is a V1
    line (UPI, no card, manual); `E` with 6 parts is V2; an unknown category or
    anything else is skipped. Writing V2: card and source empty when null.

**Parser.** `sealed class ParseResult { data class Ok(amount: Long, category:
Category, card: String?); object BadAmount; data class NoCard(word: String) }`
and `EntryParser.parseEntry(text, knownCards: Set<String>): ParseResult`.
*   Trim, then split on runs of whitespace including U+00A0. Spaces inside an
    amount are no longer ignored (`1 200` is now rejected; `1,200` still
    works). The first word goes through V1 `parse` (commas, Unicode minus).
*   Words 2 and 3 are lowercased. Word 2 `upi`: UPI, and any word 3 gives
    BadAmount. Word 2 `cc` or `ccupi`: sets the category; optional word 3 is a
    card word. Any other word 2 is a card word with category CC; a word 3 then
    gives BadAmount. A card word that is `upi`, `cc` or `ccupi` gives
    BadAmount. Any other card word not in `knownCards` gives
    `NoCard(word)`. More than 3 words gives BadAmount.
*   Messages (strings.xml): BadAmount "Type an amount like 450, 450 cc,
    450 ccupi or 450 abc."; NoCard "No card '%s'. Add it in the app."

**Recurring logic (`core/Recurring.kt`, pure, java.time).**
*   `dueDate(day, month)`: day clamped to the month's length.
*   `missingEntries(r, applied: Set<YearMonth>, today: LocalDate)`: months
    from `r.startMonth` through `YearMonth.from(today)` that are not applied
    and whose `dueDate <= today`, oldest first. For an EMI the list is cut to
    `months - applied.size` items (empty if that is 0 or less). Never a month
    after the current one.
*   `nextDue(r, applied, now: LocalDateTime): LocalDateTime?`: the first month
    from `startMonth` on that is not applied, at 00:05 on its due date; null
    for a finished EMI.
*   Applied months for `r` come from `appliedSources` entries
    `"<id>@<yyyy-MM>"`. EMI months left = `months - applied.size`, minimum 0;
    0 shows "Finished".

**`RecurringRunner.catchUpAndSchedule(context)`** (`@Synchronized`): read cards,
recurring and one ledger snapshot; for each recurring, append each missing
month with `source = "<id>@<yyyy-MM>"` and time = now; stop all appending for
this run at the first failed write. Then compute the earliest `nextDue` over
all recurring items, converted with `ZoneId.systemDefault()`. If none, cancel
the alarm. If it is not after now (for example, after a failed write), use now
+ 15 minutes. Set it with `setAndAllowWhileIdle(RTC_WAKEUP, ...)`, request code
10, explicit intent to `RecurringAlarmReceiver`,
`FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`; cancel uses the same intent.

**The alarm is best effort.** Android and OriginOS may delay or drop it
(standby buckets, battery rules, no Autostart). Correctness comes from
catch-up on the next wake-up of any kind, not from the alarm.

**Clock changes.** A new receiver handles `android.intent.action.TIME_SET`
and `android.intent.action.TIMEZONE_CHANGED` (both allowed in the manifest)
and calls `refresh`, which reschedules. Known limit: if the phone's date is
set wrongly into the future, months up to that date are added; the user
corrects them with negative entries (the month stays marked as added, so it is
not added again).

**Reset.** On tapping Reset: run `catchUpAndSchedule` first (so anything owed
lands in the old period), then show the confirmation with the resulting total.
On confirm: `Ledger.reset`, then `refresh`.

**Cards and recurring files.** Write `<file>.tmp` with `fd.sync()`, then
`Files.move(tmp, dest, ATOMIC_MOVE, REPLACE_EXISTING)`. A leftover `.tmp` is
ignored on read and overwritten on the next write. Malformed lines are
skipped. Names: replace `,`, `\n`, `\r` with a space, trim, 1 to 40
characters. Card code: 3 ASCII letters, stored lowercase, not `upi` or `cc`,
unique. A card is in use if any recurring item uses its code; it cannot be
deleted then. Deleting a recurring item removes only its line; its ledger
entries stay. Re-adding a deleted card code is allowed and its old entries
show under the new name (intended).

**Recurring dialog validation.** Amount: V1 cleaning, positive, 1-9 digits.
Day: 1-31. Months: 1-360, required for EMI, stored empty otherwise. Name
rules above. If the due date this month has already passed, the dialog's
Save button reads "Save and add ₹<amount> now". Saving runs `refresh`
(which adds this month at once). Deleting and re-creating a payment is a new
payment and can add the same month again; the delete confirmation says
"Already-added amounts stay in the total."

**Main screen structure.** Root `ScrollView` (the V1 insets listener moves to
it, `clipToPadding=false`) containing a vertical `LinearLayout`. The
breakdown, recurring and card lists are `LinearLayout` containers rebuilt on
every screen refresh with simple row views (TextView + Delete Button); no
ListView. Category is a `RadioGroup` of 3 `RadioButton`s (UPI checked by
default); card picker is a `Spinner` with "No card" first, hidden (GONE)
when UPI is selected. Dialogs use `AlertDialog.Builder` with an inflated
layout; the months row is GONE unless type is EMI. The screen's own refresh
calls `TotalNotifier.refresh(this)` first, then reads the snapshot.

**Notification text.** `setContentText(firstLine)` where firstLine is the
message or "Last entry: +₹450" or "No entries since reset".
`setStyle(Notification.BigTextStyle().bigText(firstLine + "\n" + breakdown))`
where breakdown lines are joined by `\n` in the order UPI, CC, CC UPI, then
cards sorted by code: "UPI ₹4,100", "CC ₹500", "CC UPI ₹1,200",
"HDFC Millennia (ABC) ₹9,200", "ABC (deleted) ₹300". Zero buckets omitted.

**Manifest additions.** `.receivers.RecurringAlarmReceiver`
(`exported="false"`), `.receivers.TimeChangeReceiver` (`exported="true"`,
filters for TIME_SET and TIMEZONE_CHANGED). No new permissions.

**Tests to update or add.** `EntryParserTest` (V1 `parse` unchanged plus the
`parseEntry` table and rejections), `LedgerTest` (V1+V2 mixed lines,
`E,1,450,UPI,,` trailing empties, buckets, applied sources, reset),
`RecurringTest` (Feb 28/29 with day 31, 30-day months, first month already
passed, multi-month catch-up, EMI stops at N, no duplicates when run twice,
`nextDue` never in the past for applied months), `CardsTest` (code
validation, name cleaning, atomic rewrite round trip with a temp folder).

## Not in V2

Editing entries or recurring payments in place, a history screen, reminders
before a due date, and per-card statement dates.
