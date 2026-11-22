# V4: Delete History, Card Rules and Editing, Budget, Reminders, Summary, Report

*   **Author:** Sharath
*   **Status:** Approved (2026-11-23)
*   **Builds on:** V1-V3 docs in this folder and the V3.1 code. Everything not
    changed here stays.

## 1. Deleting history entries

*   History offers: delete one entry (long-press or row menu), delete a whole
    cycle (cycle header menu), delete everything (History top bar menu). Each
    asks for confirmation stating the amount removed from the current total.
*   **Deleted entries are removed from all totals.** Deleting entries in a
    past cycle does not change the current total.
*   Mechanism (keeps the ledger append-only and recurring-safe): append a
    tombstone line `D,<time>,<fromId>-<toId>` covering a range of line ids
    (single entry: `5-5`). Every entry line inside any tombstone range is
    ignored by all totals, buckets, history and reports. Reset lines, `D`
    lines and `N` lines are never deleted by a range (they keep cycle
    boundaries intact). Deleting an entry also deletes its undo pair (both
    the original and any `undo#<id>` line), and vice versa.
*   A deleted automatic recurring entry still marks its month as added
    (its source is still read for `appliedSources`), so it is not re-added.
*   Cycles with no remaining entries are hidden in History.
*   Note: deleted lines stay in the private file but are never shown or
    counted.

## 2. Card rules

*   **CC needs a card.** Add screen: category CC shows the card dropdown with
    no "No card" item; the last-used CC card is preselected; Add is disabled
    until a card is chosen. If no cards exist, the dropdown says "Add a card
    first" with a button to the Cards tab.
*   **CC UPI default card.** One card may be marked "Default for CC UPI".
    Category CC UPI preselects that card (the user may pick another). A CC UPI
    entry is stored with category `CCUPI` and that card, and counts toward the
    **card's** bucket, shown with a "CC UPI" tag. If no default is set and no
    card is picked, Add is disabled with the hint "Set a CC UPI card in Cards".
*   Notification grammar changes:
    *   `450 cc` (no card) → rejected: "Add the card code, like 450 abc."
    *   `450 ccupi` → CC UPI on the default CC UPI card; if none:
        "Set a CC UPI card in the app."
    *   All other forms unchanged.
*   Card bucket display: "HDFC Millennia (ABC) ₹9,200" with a second line
    "incl. CC UPI ₹1,200" when the card has CC UPI entries.
*   **Existing data**: untagged CC and CC UPI entries made before V4 still
    count, shown as buckets "CC (no card)" and "CC UPI (no card)". New entries
    cannot create them.
*   Recurring payments: category CC requires a card; CC UPI defaults to the
    CC UPI card. Existing recurring items without a card keep working as
    before.

## 3. Editing saved cards

*   Tapping a card opens an edit dialog: name, bill due day (Off or 1-31),
    "Default for CC UPI" switch, and Delete. The 3-letter code is shown but
    cannot be changed (old entries are linked to it); to change a code, add a
    new card.
*   Turning on "Default for CC UPI" turns it off on any other card.
*   `cards.txt` lines become `<code>,<name>,<dueDay or empty>,<1|0>` (CC UPI
    default flag). Old 2-field lines read as no due day, not default.

## 4. Budget limit

*   Setting (gear dialog): monthly budget in rupees, or Off. Stored in
    `settings.txt` as `budget=<n>` (empty = off).
*   Home hero card: a progress bar "₹14,500 of ₹30,000 · ₹15,500 left", amber
    from 80%, red when over ("₹1,200 over budget").
*   Notification text (first line when no message): "₹15,500 left of
    ₹30,000" or "₹1,200 over budget"; the "Last entry" line follows.
*   **One alert per cycle** when the total first goes over the budget: a
    separate notification (id 2) "Over budget: ₹31,200 of ₹30,000" on a new
    channel `alerts` ("Budget and reminders", IMPORTANCE_DEFAULT, so it makes
    a normal sound/heads-up per the user's settings). Recorded with a note line
    `N,<time>,budget@<cycleStartLineId>` so it is not repeated in that cycle.
    The status bar total notification stays silent.

## 5. Card due-date reminders

*   For each card with a due day: a reminder at 09:00 local, 3 days before the
    due date (`dueDate(day, month)` with month-end clamping, minus 3 days).
    Notification on the `alerts` channel, id `1000 + index`: "HDFC Millennia
    (ABC) bill due 15 Dec · ₹9,200 this cycle". Tapping opens the app.
*   Once per card per month: note line `N,<time>,remind:<code>@<yyyy-MM>`.
    Missed reminders (phone off) are shown on the next wake-up only if the due
    date has not passed yet.

## 6. Summary tab

*   New fifth tab "Summary" (bottom bar: Home, History, Summary, Recurring,
    Cards).
*   One card per cycle, newest first (current cycle on top, marked "So far"):
    dates, total, change vs the previous cycle ("▲ ₹3,200" red / "▼ ₹1,100"
    green), and a horizontal stacked bar of buckets in bucket colours with a
    small legend (amounts per bucket).
*   An "Export report" button at the top exports the **current** cycle on
    demand (same format as below).

## 7. Cycle report in Downloads

*   When an auto-reset happens (V3 rules, including catch-up), the app first
    writes a report of the cycle that is closing, then appends the reset line.
    Manual Reset also offers "Save report" in its confirmation dialog
    (default on).
*   File: `Downloads/StatusBarTotal/StatusBarTotal_<cycle start yyyy-MM-dd>_to_<end yyyy-MM-dd>.csv`
    written with `MediaStore.Downloads` (`RELATIVE_PATH` =
    `Download/StatusBarTotal/`). No permission needed.
*   Content (UTF-8 CSV, opens in Sheets/Excel):
    *   Header block: cycle dates, total, budget (if set), per-bucket totals.
    *   Then one row per entry: date, time, amount, category, card code, card
        name, type (Manual / Auto: <recurring name> / Undo), excluding deleted
        entries.
*   Recorded with `N,<time>,report@<cycleStartLineId>` so an automatic report
    is written once per cycle. If writing fails, the reset still happens and a
    notification says "Couldn't save the report".
*   After saving, a notification on `alerts`: "Report saved to
    Downloads/StatusBarTotal".

## 8. Scheduling (extends V3)

The single alarm is set for the earliest of: next recurring due (00:05), next
auto-reset (00:00), next card reminder (09:00). Catch-up processes every due
event in time order: auto-reset (report first, then `R`), recurring entries,
reminders (only if their due date has not passed). Budget alert is checked
after every refresh.

## 9. Ledger summary of line types

| Line | Meaning |
| --- | --- |
| `E,<t>,<amt>,<cat>,<card>,<source>` | entry (V2/V3); V1 3-field form still read |
| `R,<t>` / `R,<t>,auto@<yyyy-MM>` | reset |
| `D,<t>,<from>-<to>` | delete entries with line ids in range |
| `N,<t>,<key>` | note: `budget@<id>`, `remind:<code>@<yyyy-MM>`, `report@<id>` |

Line ids count every complete line (all types), as in V3.

## Review decisions (win over the sections above)

1.  **Undo pairs at read time.** An undo line is hidden whenever its original
    is tombstoned, wherever the undo sits. Undo rows cannot be deleted on their
    own (no delete option on them). Deleting a past cycle therefore also hides
    undo lines in later cycles that undo its entries, which changes the current
    total by those amounts. Every delete confirmation computes "change to the
    current total" by running the same snapshot function with and without the
    proposed tombstone (preview), and shows "No change to the current total"
    when it is 0.
2.  **Cycle key** = line id of the last `R` line before the cycle, or `0` for
    the first cycle. Used in `budget@`, `report@`. Tombstones never touch `R`
    lines, so the key is stable.
3.  **Report writing.** MediaStore insert with `IS_PENDING=1`, write, then set
    `IS_PENDING=0`; catch every exception (null insert, Security/IO/FileNotFound).
    Append `N,report@<cycleKey>` only after success, then the `R` line. On
    failure: still reset, show "Couldn't save the report", and on every later
    app open retry the most recent closed auto-reset cycle that has no
    `report@` note (the ledger still holds it). Automatic file names are unique
    per cycle; manual "Export report" names add the time
    (`..._exported_<yyyyMMdd-HHmm>.csv`) so nothing collides. Small file;
    main thread is fine.
4.  **Reminders.** Key `remind:<code>@<due yyyy-MM>` (the due date's month).
    Notification id `1000 + abs(code.hashCode()) % 1000`. Amount computed when
    shown.
5.  **Alarm** stays `setAndAllowWhileIdle` (no exact-alarm permission). Boot,
    update, time/timezone change and every app open already run catch-up and
    reschedule (V2/V3 receivers); the next event is always recomputed from
    files.
6.  **Budget alert.** Key `budget@<cycleKey>:<budgetAmount>`, so changing the
    budget re-arms it (an alert can fire right after lowering the budget below
    the current total; that is intended). The note is written only if the
    alerts notification can actually be shown (permission granted,
    notifications enabled, `alerts` channel not NONE). Checked inside
    `TotalNotifier.refresh` after catch-up, so every path (reply, recurring,
    open, boot) checks it, using totals after tombstones.
7.  **Cards and old data.**
    *   Recurring CC items created before V4 without a card keep running and
        create "CC (no card)" entries; this is the only way such entries are
        created after V4.
    *   A card cannot be deleted while any recurring item uses it (message
        names the items). Deleting the CC UPI default card clears the default.
    *   Recurring CC UPI items store their card when created (the default at
        that time). If a CC UPI recurring item has no card (pre-V4), it keeps
        creating "CC UPI (no card)" entries.
    *   Bucket keying is unchanged from V2 (card entries always went to the
        card's bucket), so historical totals do not move; add a test with a
        pre-V4 ledger proving bucket totals are identical.
8.  **Delete ranges** use the last line id read when the dialog was built as
    the upper bound (never "to end"), so entries added meanwhile are not
    deleted.

## 10. Tests (JVM)

Tombstones (single, range, all, undo pairs, recurring month stays applied,
totals after deleting current vs past cycle); card parsing old/new format and
single CC UPI default; grammar changes; budget states and once-per-cycle
alert key; reminder dates (clamping, 3 days before, skip passed); summary
cycle totals and deltas; CSV content builder (pure function); scheduling order
with all event kinds.
