# StatusBar Running Total

*   **Author:** Sharath
*   **Status:** Approved (2026-11-22)
*   **Date:** 2026-11-22

--------------------------------------------------------------------------------

## Overview

A personal Android app for one phone (iQOO Neo 10, OriginOS 6, Android 16). It
keeps one running total of money that the user types in by hand and shows that
total as a number in the status bar, next to the clock, at all times. It works
fully offline, needs no account, and is installed by sideloading an APK.

The number in the status bar is the small icon of one silent notification. The
app draws the total as white text on a transparent image and gives that image
to Android as the notification's icon. Once the notification is posted, Android
keeps showing it; the app does not need to run. The user confirmed on this
phone, with a third-party app, that a number drawn this way shows in the status
bar.

How the status bar treats this icon: Android ignores the icon's colours and
uses only its shape (which pixels are see-through and which are not), then
paints that shape in the status bar's own colour. That is why the number is
drawn in solid white on a transparent background. On OriginOS this works only
when **status bar icons are set to Monochrome** (Settings > Status bar); in
the coloured style OriginOS may show the app's launcher icon instead. The
setting is already on for this phone and is a prerequisite for the whole app.

## The Problem

The user wants to know, at a glance and without opening anything, how much they
have spent or owe in the current period. Existing expense apps hide the number
inside the app and add features the user does not want (categories, budgets,
bank sync, accounts). This app does three things: take a number, add it to the
total, and keep the total visible in the status bar.

## Goals

*   The status bar always shows the current total while the app is set up as in
    the setup checklist, including after a reboot, after the notification is
    swiped away, and after the app is updated.
*   The user can add an amount in about two seconds from any screen, by typing
    it in the "+ Add" box on the notification, without opening the app.
*   A positive amount adds to the total. A negative amount (for example `-200`)
    subtracts, which is how mistakes are corrected.
*   The app's main screen shows the exact total, lets the user add an amount,
    and has a Reset button that sets the total back to ₹0 after a confirmation.
*   Reset never deletes anything. Every entry ever made stays saved on the
    phone.
*   The app uses no internet, no cloud backup, and only two permissions:
    showing notifications and starting after boot.
*   The app does no background work. It runs only for a moment when the user
    adds an amount, opens the app, dismisses the notification, reboots, or
    installs an update.

## Non-Goals

These are left out of version 1 on purpose. Each can be added later if the user
asks for it.

*   Categories (credit card, EMI, other) or any breakdown of the total.
*   Editing or deleting entries. Corrections are negative entries.
*   A screen that lists past entries or past totals.
*   Automatic or monthly reset, and any reset reminder.
*   A quick-add popup, a Quick Settings tile, or a home screen shortcut.
*   Decimals (paise). Amounts are whole rupees.
*   Any network feature, SMS reading, bank sync, or cloud backup.
*   A background service, alarm, or watchdog to fight the operating system. If
    OriginOS force-stops the app, the app cannot run until the user opens it
    (see "Platform limits").
*   Support for other phones. Other Android phones will probably work, but only
    this phone is tested.

## Open Questions

None for the user now. The following can only be answered on the phone and are
checked in implementation milestone 1:

*   **Icon legibility**: whether two stacked lines ("14" over "K") are
    readable in the status bar. If they are not, the icon switches to one line
    ("14K", "2.7K"); the user sees both on the phone and picks one.
*   **Fresh-install icon**: whether a silent (low importance) notification's
    icon shows in the OriginOS status bar right after install, without the user
    changing any per-app or channel setting. If a setting is needed, it goes
    into the setup checklist.
*   **Minus key**: whether the phone's keyboard shows a minus key on the main
    screen's amount field. The notification reply box always opens the full
    keyboard, which has a minus key. If the main screen's number keyboard has
    none, the field switches to the phone-dialer style keypad
    (`inputType="phone"`), which does, and the parser rules stay the same.
*   **Swipe and "Clear all"**: whether OriginOS lets the user swipe this
    notification away, and whether its "Clear all" button fires the dismiss
    signal the app listens for.
*   **Exact OriginOS menu names** for Autostart, battery, and locking the app in
    Recents, for the setup checklist.

## Critical User Journeys (CUJs)

### CUJ 1: Add an amount from the notification

*   **Given**: The app is installed and set up. The status bar shows the total
    (for example `2.3` over `K`, total ₹2,340 before the entry).
*   **When**: The user pulls down the notification shade, taps "+ Add" on the
    notification, types `450`, and sends.
*   **Then**: Within about two seconds the status bar shows `2.7` over `K`. The
    notification reads "Total ₹2,790" with the line "Last entry: +₹450". The
    reply box closes with no spinner left behind.

### CUJ 2: Correct a mistake

*   **Given**: The user meant to add ₹45 but added ₹450.
*   **When**: The user enters `-405`.
*   **Then**: The total drops by ₹405. Both entries stay saved.

### CUJ 3: Reject a bad entry

*   **Given**: The notification is showing.
*   **When**: The user types `abc`, `0`, `450.50`, or nothing, and sends.
*   **Then**: Nothing is saved. The notification shows "Type a whole number
    like 450 or -200." The reply box closes. The total is unchanged.

### CUJ 4: Add from the main screen

*   **Given**: The user opens the app.
*   **When**: The user types `1200` in the amount field and taps Add (or the
    keyboard's Done key).
*   **Then**: The main screen and the status bar both show the new total, the
    field clears, and the keyboard stays open for another entry.

### CUJ 5: Reset

*   **Given**: The total is ₹14,500.
*   **When**: The user taps Reset on the main screen and confirms the dialog
    "Reset ₹14,500 to ₹0? Your old entries stay saved on this phone."
*   **Then**: The main screen and status bar show `0`. The old entries remain in
    the saved file.

### CUJ 6: Reboot

*   **Given**: The app is set up (opened at least once, Autostart on).
*   **When**: The phone restarts and the user unlocks it.
*   **Then**: The total is back in the status bar without opening the app.

### CUJ 7: Notification swiped away

*   **Given**: The notification is showing and notifications are allowed.
*   **When**: The user swipes it away.
*   **Then**: It is posted again at once with the same total.

### CUJ 8: App updated

*   **Given**: A newer build is installed over the old one with `adb install -r`.
*   **When**: The install finishes.
*   **Then**: The notification is back with the same total, and no entries are
    lost.

### CUJ 9: Notifications blocked

*   **Given**: The user has denied the notification permission or turned off
    this app's notifications.
*   **When**: The user opens the app.
*   **Then**: A banner at the top says "Notifications are off, so the total
    can't show in the status bar" with a button that opens the right settings
    screen. Entries still work and are saved.

### CUJ 10: OriginOS force-stopped the app

*   **Given**: OriginOS force-stopped the app (for example after the user swiped
    it out of Recents without locking it), and the number disappeared.
*   **When**: The user opens the app.
*   **Then**: The number is back in the status bar. The main screen always shows
    the setup checklist, so the user can see which setting prevents this.

## Technical Plan (Architecture)

### Building blocks

The app is one main screen, three small receivers, and a few plain classes. It
uses only Android's built-in APIs and the Kotlin standard library. It has no
third-party libraries and no AndroidX libraries at runtime; JUnit is used for
tests only.

```text
 Inputs (each wakes the app briefly)            Core                     Output
 -----------------------------------            ----                     ------
 "+ Add" reply on notification --> ReplyReceiver --+
 Main screen Add / Reset --------> MainActivity ---+--> Ledger (text file)
 Notification swiped away -------> DismissReceiver-+        |
 Phone booted / app updated -----> BootReceiver ---+        v
 Main screen opened -------------> MainActivity ---+--> TotalNotifier.refresh()
                                                             |
                                        IconLabel (text) --> IconRenderer (image)
                                                             |
                                                             v
                                            Notification in the status bar
```

Every input does the same two steps: (1) if there is something to save, save it
to the ledger; (2) call `TotalNotifier.refresh()`, which reads the ledger and
posts the notification. Nothing else posts or changes the notification. This is
the only recovery mechanism, and every wake-up uses it.

### Data: the ledger file

*   Storage is one text file, `ledger.txt`, in the app's private files folder.
    No other app can read it. It is not backed up to the cloud.
*   Each line is one event, written in the order it happened:
    *   `E,<time in milliseconds>,<amount>`: an entry, for example
        `E,1795400000000,450` or `E,1795400100000,-405`.
    *   `R,<time in milliseconds>`: a reset.
*   The file is only ever added to. No line is changed or removed.
*   **The total is never stored.** It is always calculated as the sum of all
    entries after the last `R` line (or from the start if there is none). This
    means the total cannot drift away from the entries, and "old entries are
    kept" is true by design.
*   The order of lines decides which entries count, not the times in them, so
    a clock or time zone change cannot move an entry across a reset. The times
    are kept only for a future history screen.
*   Writing: open the file to append, write one whole line ending in a newline,
    force it to disk, close.
*   **Cut-off lines.** If the phone loses power during a write, the last line
    can be cut short, for example `E,1795400000000,45` instead of `...,450`.
    Such a line could still look valid, so the rule is: **a line counts only if
    it ends with a newline.** When reading, text after the last newline is
    ignored. Before appending, if the file is not empty and its last byte is
    not a newline, the file is first shortened to end just after its last
    newline (or to zero length if it has none), which removes the cut-off
    piece. A newline is never added to finish a cut-off piece, because that
    would turn `...,45` into a line that counts. Any complete line that does
    not match one of the two formats is also skipped.
*   **Failed writes.** If a write fails (for example, phone storage is full),
    nothing is counted, and the user sees "Couldn't save. Phone storage may be
    full." in the notification (for a reply) or under the amount field (on the
    main screen).
*   Reads and writes happen on the app's main thread. The file stays small
    (about 30 bytes per entry; ten thousand entries is about 300 KB). The whole
    app runs in one process (no component sets `android:process`), and Android
    calls every receiver's `onReceive` and every activity callback on that
    process's single main thread, one after another. Two entries therefore can
    never be written at the same moment, and no extra locking is needed.
*   Totals and amounts are whole rupees stored as 64-bit whole numbers, never
    as floating point.

### Input rules (one parser for both entry points)

`EntryParser.parse(text)` returns either a whole-rupee amount or nothing.

*   First, all spaces (including non-breaking spaces) and commas are removed,
    and the Unicode minus sign `−` (which some keyboards type) is treated as
    `-`.
*   Accepted: an optional `+` or `-`, then 1 to 9 digits (the sign does not
    count as a digit), and the value is not zero. Examples: `450`, `+450`,
    `-200`, `1,200`, `-99,99,99,999`.
*   Everything else is rejected, including: empty text, `0`, any decimal point
    (`450.50`, `1,200.5`), letters, a `₹` sign, or 10 or more digits.
*   Every rejection shows the same message: "Type a whole number like 450 or
    -200."

### Status bar icon text

> **Superseded on 2026-11-23 after phone tests.** The user rejected unit
> letters (K/L/Cr), both stacked and on one line. The current rule, in
> `IconLabel.text(total)`: one line, numbers only, the total **in thousands of
> rupees**, rounded toward zero, trailing zeros dropped. Below ₹10,000 two
> decimals, below ₹1,00,000 one decimal, otherwise whole thousands. Examples:
> 150 → `0.15`, 2,750 → `2.75`, 14,500 → `14.5`, 99,999 → `99.9`,
> 1,45,500 → `145`, 12,34,567 → `1234`, under ₹10 → `0`, negatives get a
> leading minus. The renderer always draws one line. The table and two-line
> rules below are history.

`IconLabel.lines(total)` turns a total into one or two short lines. It always
rounds toward zero, so the icon never shows more than the real amount.

| Total | Line 1 | Line 2 |
| --- | --- | --- |
| 0 | `0` | |
| 850 | `850` | |
| 999 | `999` | |
| 1,000 | `1` | `K` |
| 2,700 | `2.7` | `K` |
| 9,999 | `9.9` | `K` |
| 14,560 | `14` | `K` |
| 99,999 | `99` | `K` |
| 1,00,000 | `1` | `L` |
| 1,23,456 | `1.2` | `L` |
| 12,34,567 | `12` | `L` |
| 99,99,999 | `99` | `L` |
| 1,00,00,000 | `1` | `Cr` |
| 2,35,00,000 | `2.3` | `Cr` |
| 10,00,00,000 | `10` | `Cr` |
| 12,34,56,78,901 | `1234` | `Cr` |
| -450 | `-450` | |
| -2,700 | `-2.7` | `K` |
| -14,500 | `-14` | `K` |

Units: K is thousand (1,000), L is lakh (1,00,000), Cr is crore (1,00,00,000).
Rules: below 1,000 the whole number is shown on one line. From 1,000 up, the
amount is divided by 1,000 (K), 1,00,000 (L) or 1,00,00,000 (Cr) and cut
toward zero to one decimal place; the `.0` is dropped. When the result is 10
or more units, the decimal is dropped entirely: whole units only, rounded
toward zero (bigger digits are easier to read). Cr is the largest unit; any larger
total stays in Cr and the renderer shrinks the text to fit. Negative totals get a leading minus on
line 1. These rules use fixed Indian grouping, not the phone's language
setting.

Changed after the first phone test (2026-11-22): '14.7' over 'K' was too small to read.

### Status bar icon image

`IconRenderer.render(lines)` draws the lines on a 96 x 96 pixel transparent
image.

*   Text is pure white, fully opaque, bold condensed system font
    (`sans-serif-condensed`, bold), with anti-aliasing on. The status bar uses
    only the shape of the pixels, so there is no shadow, outline or
    see-through fill; the only partly see-through pixels are the soft edges
    that anti-aliasing adds to each character.
*   One line: the text is sized to fill as much of the square as fits, centered.
*   Two lines: line 1 gets the top 66 px of the height and line 2 the bottom
    30 px, each centered.
*   For each line the renderer starts with a font size that fills the line's
    height, measures the text width, and shrinks the size until the text fits
    within 94 pixels of width. Text can never be clipped.

### Notification

`TotalNotifier.refresh(context, message = null)` does the following:

1.  Make sure the notification channel exists: id `running_total`, name
    "Running total", importance low (silent, but still shown in the status
    bar), no badge, no sound, no vibration. The channel's importance cannot be
    changed after the first install, so the id and importance are fixed now.
2.  If notifications are not allowed (the permission is denied, the app's
    notifications are off, or this channel is turned off), stop. Nothing can be
    shown.
3.  Read the total and the last entry since the last reset from the ledger.
4.  Build the notification:
    *   Small icon: the rendered image (`Icon.createWithBitmap`).
    *   Title: the exact total, for example "Total ₹14,500" (Indian grouping;
        "Total -₹450" when negative).
    *   Text: the `message` if one was passed (a rejection message), otherwise
        "Last entry: +₹450", or "No entries since reset" if there are none.
        The message is not saved: it shows until the next `refresh()` without
        a message (the next entry, app open, dismiss, boot or update), which
        shows "Last entry" again.
    *   Ongoing, alert only once, no timestamp, visible in full on the lock
        screen.
    *   Tapping the notification opens the main screen.
    *   One action, "+ Add", with a reply box labelled "Amount (e.g. 450 or
        -200)". The reply is sent to `ReplyReceiver`.
    *   Dismiss signal: sent to `DismissReceiver`.
5.  Post it with the fixed notification id `1`. Posting with the same id
    replaces the old one in place, so it does not flicker.

Every reply, accepted or rejected, ends with a call to `refresh()`. This is
what closes the reply box; without it Android leaves a spinner in the box.

The reply's pending intent is mutable, because Android must add the typed text
to it, and is addressed to `ReplyReceiver` by name. The tap and dismiss pending
intents are immutable. Each uses its own request code.

### Receivers

*   **ReplyReceiver** (private to the app): reads the typed text, parses it,
    appends an entry if valid, then calls `refresh()` with the rejection message
    if it was not valid.
*   **DismissReceiver** (private): when the notification is swiped away, calls
    `refresh()` once, but only if notifications are allowed and the channel is
    still on. There are no retries and no timers. If the user swipes it away
    again, it comes back again; turning off the app's notifications is the way
    to hide it for good.
*   **BootReceiver** (open to the system broadcasts only): listens for
    "boot completed" and "this app was updated" (`MY_PACKAGE_REPLACED`), and
    calls `refresh()`. The update broadcast needs no extra permission. Android
    sends "boot completed" only after the user unlocks the phone for the first
    time after a restart, which is also when the app's files become readable,
    so the number appears right after that first unlock. The app does not use
    the earlier "locked boot" broadcast, which arrives before unlock, when the
    ledger file cannot be read.

### Main screen

One screen, built with Android's built-in views, no extra libraries. From top to
bottom:

1.  **Notification banner**, shown only when notifications are not allowed:
    "Notifications are off, so the total can't show in the status bar." with an
    "Open settings" button that opens this app's notification settings.
2.  **Total**: large text, for example "₹14,500".
3.  **Last entry** line, the same text as the notification.
4.  **Amount field** with a number keyboard that allows a minus sign, and an
    **Add** button. The keyboard's Done key also adds. A rejected entry shows
    the parser's message under the field.
5.  **Reset** button, with the confirmation dialog from CUJ 5.
6.  **"Keep the number visible"** card, always shown, with three steps
    (Autostart on, battery set to Unrestricted, app locked in Recents) and an
    "Open app settings" button that opens this app's system settings page. The
    app cannot read these settings, so it does not track whether they are done.

Each time the app is launched (when the main screen is created, not each time
it comes back to the front), it asks for the notification permission if the
app does not have it. The permission prompt itself pauses and resumes the
screen, so asking on every resume would ask again in a loop. When the user
answers the prompt, the screen calls `refresh()` and redraws the banner, so on
first install the number appears as soon as the user taps Allow. (Android
itself stops showing the prompt after the user denies it twice; the banner's
button covers that case.) Every time the main screen comes to the front, it
calls `refresh()`; this is how the number comes back after a force-stop
(CUJ 10).

Android 15 and later draw apps under the status and navigation bars. The main
screen adds the system bar sizes, and the keyboard's height when it is open, as
padding to its top-level layout, so nothing sits under the bars and the
keyboard never covers the amount field.

### Platform limits (Android and OriginOS)

*   **Force-stop is the one failure the app cannot prevent.** When OriginOS
    force-stops an app, it removes the app's notification and delivers no
    broadcasts to it, including the boot broadcast, until the user opens the
    app again. No permission in the allowed list changes this. Mitigation: the
    setup checklist on the main screen, and recovery whenever the app is
    opened.
*   **Autostart**: on OriginOS, an app without Autostart permission may not
    receive the boot broadcast. It is part of the checklist.
*   **First launch**: Android delivers no broadcasts to a newly installed app
    until it has been opened once. The setup steps start with opening the app.
*   **Swiping ongoing notifications**: from Android 14, users can swipe away
    most ongoing notifications. The dismiss signal brings it back. Some
    "Clear all" buttons on manufacturer skins may not send that signal; this is
    checked in milestone 1, and opening the app recovers in any case.
*   **Channel importance**: if the user lowers this channel below "low" in
    Settings (to "minimum" or off), the icon disappears from the status bar and
    the app cannot raise it again. The app treats anything below "low" as
    notifications off: the banner shows, and the dismiss receiver does not
    re-post.
*   **Crowded status bar**: when many notifications are showing, Android and
    OriginOS show only some notification icons and replace the rest with a
    dot. The app cannot force its icon to stay. This is checked in milestone 1
    (post several other notifications and see whether the total stays).
*   **Glossary**: *Recents* is the recent-apps screen; *locking* an app's card
    there tells OriginOS not to force-stop it when Recents is cleared.
    *Autostart* is an OriginOS per-app permission that lets an app receive
    events such as boot when it is not running.
*   **Signing key**: Android only installs an update if it is signed with the
    same key as the installed copy. A different key forces an uninstall, which
    deletes the ledger. All builds are made on this PC with its one debug key,
    which must be backed up (see Non-Functional Requirements).

## Alternatives Considered

### Alternative A: A database (SQLite or the Room library)

*   **Pros**: Built-in safe writes and queries. Room has tools for changing the
    data layout later.
*   **Cons**: More code for a list of numbers. Room adds a code generator and
    build setup on a fresh machine.
*   **Reason for Rejection**: Rejected by author: the user asked for the
    simplest app. An append-only text file gives the same safety for one list
    of numbers. If a history or category feature later needs queries, the file
    can be read into a database then.

### Alternative B: Store only the running total

*   **Pros**: Smallest possible storage.
*   **Cons**: Reset would erase the old entries, and a lost write would make the
    total wrong with no way to check it.
*   **Reason for Rejection**: Rejected by author: the user wants all earlier
    entries kept.

### Alternative C: A background service to keep the notification alive

*   **Pros**: A foreground service is harder for the system to kill.
*   **Cons**: Shows a second, permanent system notice, uses battery, needs a
    service, and still dies on force-stop.
*   **Reason for Rejection**: Rejected by author: breaks the "no background
    work" rule and does not fix the one case that matters.

### Alternative D: Timers or alarms to re-post the notification

*   **Pros**: Could repair a missing notification without the user.
*   **Cons**: Background work; alarms are also cancelled on force-stop.
*   **Reason for Rejection**: Rejected by author: no background work, and it
    would not help after force-stop.

### Alternative E: AndroidX libraries (NotificationCompat, AppCompat)

*   **Pros**: Smooth over differences between Android versions.
*   **Cons**: Extra dependencies. The app's lowest supported Android version
    (13) already has every API used here.
*   **Reason for Rejection**: Rejected by author: not needed for one phone on
    Android 16.

### Alternative F: Reset from the notification

*   **Pros**: Faster.
*   **Cons**: Easy to tap by accident in the shade.
*   **Reason for Rejection**: Rejected by author: Reset stays on the main screen
    behind a confirmation.

### Alternative G: One-line icon text ("14K") from the start

*   **Pros**: Larger characters.
*   **Cons**: Less precise ("14K" for ₹14,999).
*   **Reason for Rejection**: Kept as the fallback if the two-line layout is not
    readable on the phone (milestone 1).

--------------------------------------------------------------------------------

## Non-Functional Requirements

*   **Privacy**: No internet permission. `allowBackup` is off, so Android does
    not copy the ledger to Google's cloud. The ledger is in app-private storage.
    The total is visible on the lock screen by choice, since the status bar
    shows it there anyway.
*   **Battery and performance**: The app has no service, alarm, timer or job. It
    runs only while handling one event, typically well under 100 ms (read a
    small file, draw one 96 x 96 image, post one notification).
*   **Data safety**: Append-only file with each line forced to disk; the total
    is always recalculated from the file. Uninstalling the app deletes the
    ledger; updates keep it.
*   **Updates and rollback**: Updates are installed over the old version with
    `adb install -r`. `versionCode` goes up by one each build and never goes
    down. The ledger format is fixed for V1; a later version must read it as is.
*   **Signing key backup**: Builds use Android Studio's debug key at
    `C:\Users\Shara\.android\debug.keystore`. Copy that file somewhere safe. If
    it is lost (for example after reinstalling Windows), the next build cannot
    update the installed app without uninstalling it and losing the ledger.

--------------------------------------------------------------------------------

### Additional Resources

*   `../context/handover_and_decisions.md`: original handover from Gemini and
    the decisions made with the user.
*   `../../concepts_brief.md`: EGM brainstorm brief and Goldfish findings.

### Future Improvements

Each of these is deferred until the user asks. None leaves code, files or
placeholders in V1.

*   **History screen**: read the ledger and list entries and past totals per
    reset.
*   **Categories**: add an optional category field to new `E` lines; old lines
    count as "Other".
*   **Edit or delete**: add correction lines that refer to an earlier line,
    keeping the file append-only.
*   **Monthly auto-reset**: one alarm at the reset date that appends an `R`
    line.
*   **Quick-add popup, Quick Settings tile, home screen shortcut**: new entry
    points that call the same save-then-`refresh()` steps.
*   **Export**: share the ledger as text through Android's share sheet.

--------------------------------------------------------------------------------

## Detailed Implementation Plan

### Toolchain

*   Install the current stable Android Studio on Windows. In its SDK Manager,
    install the Android 16 (API 36) SDK platform and Platform-Tools (which
    includes `adb`).
*   Create the project in Android Studio with the "Empty Views Activity"
    template, Kotlin, Kotlin DSL build files, minimum SDK API 33 (Android 13).
    The original handover (the first requirements list, written with Gemini
    before this design) said API 26; it is raised because the only target
    phone runs Android 16, and API 33 removes every Android-version branch
    from the code (notification permission, mutable pending intents, the
    built-in day/night theme). Keep the
    Android Gradle Plugin, Gradle, and Kotlin versions the wizard chooses and
    record them in the README. Then remove the template's AppCompat, Material
    and AndroidX test dependencies.
*   Build from the command line with Android Studio's bundled Java
    (`JAVA_HOME` pointing to its `jbr` folder): `gradlew.bat assembleDebug` and
    `gradlew.bat testDebugUnitTest`.
*   Put the project under git version control (`git init`) before the first
    build. The ledger and signing key are never committed.

### Milestones

1.  **Core and phone check (go or no-go).** Build the pure logic and ledger
    with their unit tests, then the notification parts (`IconRenderer`,
    `TotalNotifier`, the three receivers) using the real ledger, the manifest,
    strings and theme, and a bare main screen: no layout beyond a single
    TextView, the permission request in `onCreate`, and `refresh()` in
    `onResume` and after the permission answer. Unit tests and lint run at the
    end of this milestone. Install on the phone and check the
    items in "Open Questions", reboot, swipe, "Clear all", reply spinner,
    update, and a crowded status bar (several other apps' notifications
    showing). If the two-line icon is not readable, build the one-line rule too
    and let the user pick on the phone.
    *   **Go**: the total appears in the status bar after install and app open,
        a "+ Add" reply changes it and closes the reply box, and it returns
        after a reboot and first unlock.
    *   **No-go** (stop and report back before building more): the number does
        not appear in the status bar even with Monochrome icons on, or the
        reply box does not deliver text to the app. A gap only in swipe or
        "Clear all" recovery is recorded, not a no-go, because opening the app
        recovers.
    *   Open Question outcomes: icon unreadable in two lines → switch the
        `IconLabel` constant (below), record; fresh-install icon needs a
        setting → add it to the checklist, record; no minus key on the main
        screen → use `inputType="phone"`, record; swipe or "Clear all" gap or
        crowded status bar → record only.
2.  **Main screen**: total, Add, Reset with confirmation, banner, checklist.
    Done when CUJs 1 to 5 and 9 pass on the phone, and unit tests and lint
    pass.
3.  **Phone acceptance pass** using the verification list below.

### Target Files to Create/Modify

All paths are relative to the project folder
`C:\users\shara\claude_projects\StatusBar_Project`. Package and application id:
`com.sharath.statusbartotal`.

#### 1. `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat` (Create, from the template)

*   **Type / Scope**: Build configuration.
*   **Proposed Changes**: Generated by Android Studio. Project name
    "StatusBarTotal". Remove library entries for AppCompat, Material,
    ConstraintLayout, AndroidX Core/KTX and AndroidX test from the version
    catalog; keep JUnit.

#### 2. `app/build.gradle.kts` (Create)

*   **Type / Scope**: App module build configuration.
*   **Proposed Changes**: `namespace` and `applicationId` =
    `com.sharath.statusbartotal`; `compileSdk = 36`, `minSdk = 33`,
    `targetSdk = 35`; `versionCode = 1`, `versionName = "1.0"`, raised each
    release. Target 35 rather than 36 avoids Android 16's extra app-side
    requirements (for example predictive back handling) that give this app
    nothing; it still runs normally on Android 16. Java and Kotlin target 17. Dependencies: `testImplementation`
    JUnit 4 only. No release build or minification is used for V1.

#### 3. `app/src/main/AndroidManifest.xml` (Create)

*   **Type / Scope**: App manifest.
*   **Proposed Changes**:
    *   Permissions: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`. Nothing
        else.
    *   `application`: `allowBackup="false"`, label "StatusBar Total", the
        template launcher icon, the app theme.
    *   `MainActivity`: exported launcher activity, `windowSoftInputMode`
        `stateVisible` so the keyboard opens on the amount field (the keyboard
        height is handled by the layout padding described in "Main screen").
    *   Delete the template's backup and data-extraction XML files and their
        manifest references; `allowBackup="false"` replaces them.
    *   `ReplyReceiver`, `DismissReceiver`: `exported="false"`, no intent
        filters.
    *   `BootReceiver`: `exported="true"`, intent filters for
        `android.intent.action.BOOT_COMPLETED` and
        `android.intent.action.MY_PACKAGE_REPLACED`.

#### 4. `app/src/main/java/com/sharath/statusbartotal/core/EntryParser.kt` (Create)

*   **Type / Scope**: Pure Kotlin, no Android types.
*   **Proposed Changes**: `parse(text: String): Long?` returns the amount, or
    null for any rejected input, following "Input rules". The one rejection
    message lives in `strings.xml`.

#### 5. `app/src/main/java/com/sharath/statusbartotal/core/RupeeFormat.kt` (Create)

*   **Type / Scope**: Pure Kotlin.
*   **Proposed Changes**: `exact(amount: Long): String` returns Indian-grouped
    rupees: `₹0`, `₹850`, `₹14,500`, `₹1,23,456`, `-₹450`. `signed(amount)`
    returns `+₹450` or `-₹405` for the "Last entry" line. Grouping is done by
    hand (last three digits, then pairs), not with the phone's locale.

#### 6. `app/src/main/java/com/sharath/statusbartotal/core/IconLabel.kt` (Create)

*   **Type / Scope**: Pure Kotlin.
*   **Proposed Changes**: `lines(total: Long): List<String>` (one or two
    strings), following the table in "Status bar icon text", using whole-number
    arithmetic only.

#### 7. `app/src/main/java/com/sharath/statusbartotal/data/Ledger.kt` (Create)

*   **Type / Scope**: Storage, plain Kotlin and `java.io` only (no Android
    types, so it can be unit-tested on the PC).
*   **Proposed Changes**: Constructed with a `File`. In the app the file is
    `filesDir/ledger.txt`.
    *   `addEntry(amount: Long, timeMillis: Long)`: append `E,<time>,<amount>`.
    *   `reset(timeMillis: Long)`: append `R,<time>`.
    *   `snapshot(): Snapshot` with `total: Long` and `lastEntry: Long?` (the
        newest entry since the last reset), read in one pass, counting only
        newline-terminated lines and skipping malformed ones.
    *   Appends follow "Cut-off lines": if the file does not end with a
        newline, shorten it to just after its last newline (with
        `RandomAccessFile.setLength`), then open in append mode, write the
        whole line with a newline, call `FileDescriptor.sync()`, and close.
        `addEntry` and `reset` return true on success and false if an
        `IOException` occurs; callers show the "Couldn't save" message on
        false. Callers create a
        `Ledger(File(context.filesDir, "ledger.txt"))` when needed; there is no
        shared instance and no locking.

#### 8. `app/src/main/java/com/sharath/statusbartotal/notify/IconRenderer.kt` (Create)

*   **Type / Scope**: Android graphics.
*   **Proposed Changes**: `render(lines: List<String>): Bitmap` draws a
    96 x 96 `ARGB_8888` transparent bitmap, following "Status bar icon image".

#### 9. `app/src/main/java/com/sharath/statusbartotal/notify/TotalNotifier.kt` (Create)

*   **Type / Scope**: Android notifications.
*   **Proposed Changes**: Constants: channel id `running_total`, notification
    id `1`, reply key `amount`, request codes for the three pending intents.
    `refresh(context: Context, message: String? = null)` follows the
    "Notification" section. `canNotify(context): Boolean` returns true only if
    the permission is granted, `areNotificationsEnabled()` is true, and the
    channel's importance is `IMPORTANCE_LOW` or higher; it
    is shared with `DismissReceiver` and the main screen banner. The reply
    intent uses `FLAG_MUTABLE`; the others use `FLAG_IMMUTABLE`.

#### 10. `app/src/main/java/com/sharath/statusbartotal/receivers/ReplyReceiver.kt` (Create)

*   **Type / Scope**: Broadcast receiver.
*   **Proposed Changes**: Read the text with `RemoteInput.getResultsFromIntent`,
    parse, `addEntry` if valid, then `TotalNotifier.refresh(context, message)`
    with the rejection message or null. Always calls `refresh`.

#### 11. `app/src/main/java/com/sharath/statusbartotal/receivers/DismissReceiver.kt` (Create)

*   **Type / Scope**: Broadcast receiver.
*   **Proposed Changes**: If `TotalNotifier.canNotify(context)`, call
    `TotalNotifier.refresh(context)`.

#### 12. `app/src/main/java/com/sharath/statusbartotal/receivers/BootReceiver.kt` (Create)

*   **Type / Scope**: Broadcast receiver.
*   **Proposed Changes**: For `BOOT_COMPLETED` or `MY_PACKAGE_REPLACED`, call
    `TotalNotifier.refresh(context)`. Ignore any other action.

#### 13. `app/src/main/java/com/sharath/statusbartotal/MainActivity.kt` (Create)

*   **Type / Scope**: Activity (`android.app.Activity`, no AppCompat).
*   **Proposed Changes**: Follows "Main screen". `onCreate`: inflate the
    layout, apply inset padding, and wire Add, the Done key, Reset, the
    banner's "Open settings", and "Open app settings". `onCreate` also requests `POST_NOTIFICATIONS` if it is not
    granted (only here, never in `onResume`). `onRequestPermissionsResult`:
    `TotalNotifier.refresh(this)` and redraw the banner. `onResume`:
    `TotalNotifier.refresh(this)` and redraw the total, last entry and banner.
    Reset uses the built-in `AlertDialog`. The banner's "Open settings"
    opens `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. "Open app settings"
    opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for this app. Window
    insets for system bars and the keyboard (`WindowInsets.Type.systemBars()`
    and `ime()`) are applied as padding to the root view.

#### 14. `app/src/main/res/layout/activity_main.xml` (Create)

*   **Type / Scope**: Layout.
*   **Proposed Changes**: A vertical `LinearLayout` inside a `ScrollView` with
    the six parts of the main screen. Amount field `inputType="numberSigned"`,
    `imeOptions="actionDone"`.

#### 15. `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml` (Create)

*   **Type / Scope**: Resources.
*   **Proposed Changes**: All user-facing text from this document. The theme is
    the built-in `Theme.DeviceDefault.DayNight` with no AppCompat parent.
    Delete the template's `values-night/themes.xml`; the DayNight theme
    handles dark mode itself.

#### 16. `app/src/main/res/mipmap-*/`, `app/src/main/res/drawable/` (Create, from the template)

*   **Type / Scope**: Launcher icon.
*   **Proposed Changes**: Template icon for V1. The status bar icon is drawn at
    runtime and needs no resource.

#### 17. `app/src/test/java/com/sharath/statusbartotal/core/EntryParserTest.kt`, `RupeeFormatTest.kt`, `IconLabelTest.kt` (Create)

*   **Type / Scope**: JVM unit tests (JUnit 4).
*   **Proposed Changes**: Every example in "Input rules" and the icon text table,
    plus boundary values (999, 1,000, 99,999, 1,00,000, 99,99,999,
    1,00,00,000), negatives, the 9-digit limit, and grouping for totals of 1 to
    12 digits (totals can exceed the 9-digit entry limit by adding entries).

#### 18. `app/src/test/java/com/sharath/statusbartotal/data/LedgerTest.kt` (Create)

*   **Type / Scope**: JVM unit test using a temporary file.
*   **Proposed Changes**: Empty file gives total 0; entries sum; reset sets
    total to 0 and keeps earlier lines; a last line with no newline (for
    example `E,1,45` cut from `450`) is not counted, and after the next entry
    the `45` piece is gone from the file and still not counted; entries after
    a reset with
    an earlier timestamp still count (order is by line, not time); negative
    totals.

#### 19. `.gitignore`, `README.md` (Create)

*   **Type / Scope**: Repository files.
*   **Proposed Changes**: `.gitignore` from the Android Studio template plus
    `*.keystore`. `README.md`: tool versions, build commands, the `adb install`
    command, the phone setup checklist, and the signing key backup note.

### Exact Details (from the implementation readiness review)

These settle details the file list above leaves open. Where this section and a
file item differ, this section wins.

*   **Project creation**: in the Android Studio wizard, set the Save location
    to `C:\users\shara\claude_projects\StatusBar_Project` itself, so `app/` and
    `settings.gradle.kts` sit directly in it (not in a nested folder). Keep the
    wizard's Gradle, AGP, Kotlin and JVM-target lines and their syntax; edit
    only the values this document names (SDK levels, ids, version code). If
    the wizard's JVM target is not 17, keep the wizard's value.
*   **Delete from the template**: `app/src/androidTest/` (whole folder),
    `app/src/test/.../ExampleUnitTest.kt`, `res/values/colors.xml`,
    `res/values-night/themes.xml`, `res/xml/backup_rules.xml`,
    `res/xml/data_extraction_rules.xml`, the template `activity_main.xml`
    (replaced), and from `app/build.gradle.kts` the `testInstrumentationRunner`
    line and every `androidTestImplementation` and `implementation` line. Keep
    `app/proguard-rules.pro` and `app/.gitignore` unchanged.
*   **Theme**: keep the template's style name (`Theme.StatusBarTotal`) so the
    manifest reference stays valid; change its parent to
    `android:Theme.DeviceDefault.DayNight.NoActionBar` (no action bar, so the
    inset padding is the only top spacing). In `strings.xml`, escape
    apostrophes as `\'`.
*   **Channel**: `TotalNotifier` has a private `ensureChannel(context)` that
    creates the channel if `getNotificationChannel("running_total")` is null.
    Both `refresh()` and `canNotify()` call it first, so the channel is never
    null when read.
*   **Notification build**: framework classes only (`android.app.Notification`,
    `android.app.RemoteInput`, `android.app.PendingIntent`). Pending intents use
    explicit intents (`Intent(context, ReplyReceiver::class.java)` and so on),
    request codes 0 (tap), 1 (reply), 2 (dismiss), and
    `FLAG_UPDATE_CURRENT` combined with `FLAG_MUTABLE` (reply) or
    `FLAG_IMMUTABLE` (tap, dismiss). The "+ Add" action uses
    `Notification.Action.Builder(null as Icon?, "+ Add", replyIntent)` with
    `setAllowGeneratedReplies(false)`. In `ReplyReceiver`, a null results
    bundle or null text is treated as empty text and rejected. Entry times use
    `System.currentTimeMillis()`.
*   **Settings screens**: notification settings is
    `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)` with
    `putExtra(Settings.EXTRA_APP_PACKAGE, packageName)`. App settings is
    `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` with
    `data = Uri.fromParts("package", packageName, null)`. Both are started from
    the activity, so no extra flags are needed.
*   **Icon geometry**: canvas 96 x 96; text paint white, `ANTI_ALIAS_FLAG`,
    `Paint.Align.CENTER`, x = 48; typeface
    `Typeface.create("sans-serif-condensed", Typeface.BOLD)` (Android falls
    back to the default bold font if the family is missing, which is
    acceptable). Bands: one line uses the full 96 px; two lines use
    0-66 px and 66-96 px. For each line separately: start with text size equal
    to the band height; if `measureText` is over 94 px, multiply the size by
    `94 / measuredWidth` once; then draw at baseline
    `bandCentre - (ascent + descent) / 2` from `paint.fontMetrics`.
*   **One-line fallback**: `IconLabel.kt` has `const val TWO_LINES = true`.
    When false, `lines()` returns one string: the same number and rounding,
    with the unit joined on (`14K`, `2.7K`, `1.2L`, `2.3Cr`, `-450`). Switching is a
    rebuild, not a setting. `IconLabelTest` covers both modes.
*   **Main screen flow**:
    *   Add (button or Done key): parse; if rejected, set the parser message as
        the field's error and stop; otherwise `addEntry`; if it returns false,
        show "Couldn't save. Phone storage may be full." as the field's error
        and stop; otherwise clear the field, call `refresh()`, and redraw. The
        Done-key listener returns true so the keyboard stays open.
    *   Reset: after confirmation, `reset`; on false show the "Couldn't save"
        message as a toast; otherwise `refresh()` and redraw. Reset always
        appends an `R` line, even when the total is already 0.
    *   The amount field calls `requestFocus()` in `onCreate`.
    *   The permission request in `onCreate` runs only when
        `savedInstanceState == null`, so rotating the screen does not ask
        again.
    *   Insets: the root `ScrollView` (id `root`) gets an
        `OnApplyWindowInsetsListener` that sets padding on all four sides from
        `insets.getInsets(WindowInsets.Type.systemBars() or
        WindowInsets.Type.ime() or WindowInsets.Type.displayCutout())` and
        returns the insets; the ScrollView has `clipToPadding="false"`.
*   **targetSdk 35 on Android 16**: expected to run normally; confirmed in
    milestone 1.

### Verification Plan

*   **Compilation/Build**: `gradlew.bat assembleDebug` succeeds with no errors.
*   **Unit Tests**: `gradlew.bat testDebugUnitTest`, all pass.
*   **Static Analysis & Linting**: `gradlew.bat lintDebug` with no errors; any
    warnings are reviewed.
*   **Test Coverage Requirement**: Every rule in "Input rules", "Status bar icon
    text" and "Data: the ledger file" has at least one test. Android-only code
    (renderer, notifier, receivers, screen) is checked on the phone.
*   **E2E/Integration (on the phone, manual)**:
    1.  Fresh install, open, allow notifications: `0` appears in the status bar.
    2.  CUJs 1 to 5 as written.
    3.  Reboot and unlock: total returns (CUJ 6).
    4.  Swipe away; also try "Clear all": total returns or the gap is recorded
        (CUJ 7).
    5.  `adb install -r` a build with a higher `versionCode`: total returns,
        entries kept (CUJ 8).
    6.  Deny the permission, then turn the app's notifications off: banner and
        settings button work (CUJ 9).
    7.  Swipe the app out of Recents while not locked, then open it: total
        returns (CUJ 10).
    8.  Totals of 0, 999, 1,000, 14,560, 99,999, 1,23,456, 1,00,00,000 and
        -14,500: icon readable and not clipped.
    9.  Two quick replies in a row: both counted.
    10. Change the phone's time zone and add an entry: total is correct.

### Install and Phone Setup (for the README)

*   On the phone: Settings > About phone, tap the software version 7 times to
    turn on Developer options; then turn on USB debugging (and, on OriginOS,
    "Install via USB" if shown).
*   Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
*   After install: open the app once, allow notifications, then:
    *   App settings > Battery: Unrestricted (or "Allow background activity").
    *   Autostart: on for StatusBar Total.
    *   Recents: lock the app's card.
    *   Status bar icons stay set to Monochrome.
*   The exact OriginOS menu names are confirmed in milestone 1 and written into
    the README.

--------------------------------------------------------------------------------

*Drafted with EGM*
