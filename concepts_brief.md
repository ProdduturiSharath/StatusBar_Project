# StatusBar Running Total (personal offline Android app)

* **Author:** Sharath
* **Date:** 2026-11-22

## Reviewer Status

| Reviewer | Status |
| --- | --- |
| `egm-design` | completed (persona file `design.md` was missing; reviewer used `design_critic.md`) |
| `egm-codebase-details` | completed (claims come from model knowledge, not fetched docs or a device test; items marked "verify" need a phone check) |
| `egm-zoom-out` | completed |
| `egm-comprehension` | completed |
| `egm-implementation-readiness` | completed |
| `egm-structural` | completed |
| `egm-inventive-catalyst` | completed |

All seven ran on Sonnet. No codebase exists yet, so no reviewer checked anything against code.

## Overview

A personal app for one user (iQOO Neo 10, OriginOS 6 / Android 16, sideloaded, no root, no internet). It keeps one running money total and shows it all the time in the status bar. It does this by posting a silent, ongoing notification (IMPORTANCE_LOW) whose small icon is a bitmap drawn at runtime: the total in white on transparency, in compact Indian format ("850", "14.5" over "K", "1.2" over "L"). The user already confirmed on this phone that a third-party app can do this.

Version 1 is deliberately small. The user types an amount (positive adds, negative corrects) either in a "+ Add" reply box on the notification or on a simple main screen. A Reset button, with confirmation, starts the total from zero while every earlier entry stays stored. There are no categories, no edit or delete, no history screen, no automatic reset, and no internet. The notification comes back after a swipe-away and after reboot. The only permissions are POST_NOTIFICATIONS and RECEIVE_BOOT_COMPLETED, and there is no background service or polling.

## Analysis

**What the reviewers agree on.** No reviewer found a blocker in the V1 scope itself. The mechanism is sound: once posted, the notification is owned by Android, not by the app, so it survives the app process dying. The real weak spots are in the details the scope leaves undefined, and in one thing no code can fix (force-stop, below).

**1. The one failure that cannot be designed away: force-stop.** If OriginOS force-stops the app (for example when it is swiped from Recents), the notification is removed and the app receives no broadcasts, including the boot broadcast, until the user opens it again. Autostart must also be on for the boot broadcast to arrive on this phone. Android and OriginOS decide this, not the app. The design can only (a) make recovery free whenever the app does get a chance to run, and (b) detect and explain the loss.
* Recovery: one function, "refresh", reads the stored data and posts the notification with a fixed id. Every entry point calls only this function: app open (on resume), the reply receiver, the delete-intent receiver, the boot receiver, and a package-replaced receiver.
* Detection: on app open, compare what is stored against what is actually posted (`getActiveNotifications`). If the notification is missing, show a banner telling the user to check the Recents lock and Autostart. Also check `areNotificationsEnabled()` and the channel's importance (the user or OS can lower it, and the app cannot raise it again).
* Setup checklist: the app cannot read the Recents lock or Autostart settings, so the checklist is an in-app screen with manual "I did this" boxes and links to the app settings and battery settings screens. The exact OriginOS menu paths should be confirmed on the phone. The app must not request the battery-optimization permission, which is outside the allowed list.
* Several reviewers independently raised `MY_PACKAGE_REPLACED` (sent after every sideload update or reinstall, needs no permission). Without it the number vanishes after each update until the app is opened.

**2. Data integrity: store entries, compute the total.** Reviewers converged on one rule: never keep a stored, changeable total. Store every entry as a row (signed amount, timestamp), store Reset as a marker, and compute the total as the sum of entries after the latest marker. This gives one source of truth, so the total cannot drift, and it makes "all earlier entries stay stored" true by construction.
* Amounts are whole numbers (a Long), never floating point. **Open question for the user: whole rupees or paise?** The scope says "amount only" and does not say whether "450.50" is allowed. Parse with BigDecimal if decimals are allowed.
* Each write is one atomic database transaction, so a reply from the notification and an entry from the main screen cannot lose each other's updates. Use one process, one database instance.
* Order: write first, then post the notification. If the post fails, the next refresh repairs it from the data.
* The marker must be ordered by row id, not by clock time, so a clock or timezone change or two events in the same millisecond cannot misplace an entry. Reset must be one transaction and a no-op if there is nothing since the last marker.
* Receivers have about 10 seconds. Do short synchronous work or use `goAsync()`. Fire-and-forget saves (such as SharedPreferences `apply()`) can be lost if the process dies.

**3. Storage choice (the reviewers split here).**
* Room: fits the deferred features (migrations), but adds KSP, schema export and build churn on a fresh Windows/Android Studio setup.
* Framework `SQLiteOpenHelper`: zero dependencies, atomic transactions, safe between receiver and activity. Recommended by design and zoom-out-adjacent reasoning for "few dependencies".
* Plain append-only text file with a reset marker line: smallest, human-readable, but needs care for atomic writes and a half-written last line.
* SharedPreferences holding a total: rejected by every reviewer who addressed it (loses entries, risks lost updates).
* Recommendation to carry forward: `SQLiteOpenHelper` with one entries table and the marker, shaped so it can move to Room later. The user should confirm; Room remains acceptable if the user prefers its migration tooling.

**4. Platform details that fail silently if wrong** (from the codebase-details and design reviewers; verify on the phone).
* The reply PendingIntent must be mutable (`FLAG_MUTABLE`) and addressed to an explicit, non-exported receiver component, with its own request code. An immutable one silently drops the typed text. The delete and content PendingIntents use `FLAG_IMMUTABLE`. Send the reply to a receiver, not an activity.
* After every reply, including rejected ones, the notification must be re-posted, or the "+ Add" box shows a stuck spinner. Re-post with the same id; do not cancel first (that flickers and can fire the delete intent).
* Ongoing notifications can be swiped away on Android 14+, so the delete-intent re-post is needed. It is best effort: it does not fire after force-stop, may not fire on some "Clear all" paths on OEM skins, and can fight the user if they turned notifications off. Check `areNotificationsEnabled()` and channel importance before re-posting; re-post once per dismissal with no retries or alarms.
* POST_NOTIFICATIONS is a runtime permission. After two denials the prompt stops appearing, so the main screen needs a visible "notifications off" state and a link to the app notification settings.
* Boot: use BOOT_COMPLETED only (not LOCKED_BOOT_COMPLETED, since storage is unreadable before first unlock). The number reappears after first unlock.
* Icon: use `NotificationCompat` with `IconCompat.createWithBitmap` (or framework `Icon.createWithBitmap`), not a drawable resource id. Draw solid white on transparent; the status bar uses only the alpha channel, so glows or semi-transparent text look muddy. 96x96 is about 37 KB, far under Binder limits.
* Channel importance is fixed after first creation, so choose the channel id and notification id up front; a later importance change needs a new channel id.
* Android 16: do not opt in to promoted/live notifications. Targeting SDK 36 forces edge-to-edge and predictive back, which the main screen would have to handle; reviewers lean to targetSdk 35 until the device spike passes.
* `allowBackup` defaults to true and would send the database to Google Drive, which cuts against "fully offline". Decide explicitly (turn it off, or limit it).

**5. Two-line icon legibility is the biggest unproven visual risk.** "14.5" over "K" in a 24dp slot leaves very small glyphs. The third-party app does not prove this layout is readable. Options: keep the stacked layout, or use a single line ("14K", "1.2L"). Decide after seeing it on the phone. Keep the exact figure in the expanded notification so the icon only needs to be approximately readable.

**6. Things the scope leaves undefined (comprehension, structural, implementation-readiness).** These must be written into the design:
* Input grammar: accepted forms (optional sign, digits, commas, rupee sign, leading +), rejected forms (empty, zero, "abc", too large), a maximum size, and how a rejection is shown (a re-posted notification line such as "Couldn't read 'abc'"; silent drops are the worst outcome in a money app).
* Icon text rules: zero (right after Reset), negative totals (minus sign costs width), rounding at the boundaries (999 / 1,000; 99,999 / 1,00,000), truncate versus round (truncating avoids overstating), and values above 99 lakh. Use a pure function from total to text lines, unit-tested on the JVM, using fixed Indian grouping, not the device locale. The expanded text must round the same way as the icon.
* Whether a correction may take the total below zero (reviewers lean: allow it, and define how it looks).
* First run: launch, explain permission, request it, post the notification at 0, show the setup checklist. The app must be opened once after install before it can receive boot broadcasts.
* Notification content for V1: with categories gone, the text is the exact total (and, per several reviewers, the last entry). Lock screen visibility must be set explicitly; the default makes the exact total visible there. Tapping the notification should open the main screen.
* One name for the concept ("Running total") across the notification, main screen and Reset dialog; drop "cycle", "dues", "billing" from user-facing text.
* Reset: the confirmation shows the amount being cleared. In V1 the user cannot see or recover older entries, so the app should say so in one line.

**7. Open design tension: visibility of past entries.** The zoom-out, design and inventive reviewers all point out that "stored but never shown" gives the user no way to verify the total or diagnose a wrong number. Cheap options that stop short of a history screen:
* Show the last entry (amount, time) on the main screen and in the expanded notification.
* "Undo last entry": appends the negative of the last entry, so the log stays append-only and nothing is deleted. Note this is a new capability beyond the user's "corrections are negative entries" rule, so it is the user's call.
* An echo after each accepted entry ("Added 450, total 2,340") in the re-posted notification, so a typo like 4500 is seen right where it was typed.
* Optional later: plain-text export via the share sheet (no permission needed).
* Undo for Reset (adding a "reopen" marker, or deleting the latest marker) was proposed by two reviewers; it would add complexity and the zoom-out reviewer judged it unnecessary. Treat as optional.
* A large-amount confirmation step was proposed and is noted only as friction to avoid in V1.

**8. Install and signing.** A rebuilt APK signed with a different key cannot be installed over the old one, forcing an uninstall that wipes the database. Use one stable personal keystore for every build, keep it backed up outside the repo, fix the applicationId now, and never downgrade versionCode. The project folder is not under version control yet.

**9. Structure.** The structural reviewer recommends these boundaries so deferred features are additive later: pure core logic (parser, total, icon label formatter) with no Android types; one storage class (only code touching the database); an icon renderer (text lines to bitmap); a notification poster (no database access); an add-entry and reset action that every entry point calls (so a future dialog or tile adds no business logic); thin receivers; one main screen. Keep it to one activity, a few receivers and a few small classes, with no dependency-injection framework. Put OS workarounds (re-post on dismiss, boot recovery, checklist text) in one clearly named place, since OriginOS behavior can change with updates. Rule for V1: no code, schema column, manifest entry, string or UI placeholder for any deferred feature.

**10. The context file conflicts with itself.** "Features from the Original Handover" and "Decision 4" still describe categories, Room with category and note columns, a quick-add dialog, edit/delete, a reset alarm and a lock-screen breakdown. The V1-final section supersedes them, but the older text remains. The design document must carry one authoritative V1 list and treat the old sections as history. Also note the "Hard Constraints" line saying the app wakes on "edits" (V1 has none) and the old "button to grant permission" feature, which V1 still needs in some form.

**Contradictions and resolutions (inventive lens).**
* Always visible versus no background process: resolved by letting Android own the notification and using one idempotent refresh from every free wake-up; force-stop is an outside limit, so detect and explain it.
* Tiny slot versus readable: the icon gives the approximate value, the expanded notification gives the exact value; measure text to fit instead of fixed sizes; render a table of sample totals to bitmaps in a test to catch clipping.
* No edit or delete versus typos: echo each entry back, and optionally undo-as-new-entry.
* Append-only versus integrity: the database write is the commit; the notification is always re-derivable and never a source of truth.
* Swipe protection versus respecting the user: treat the delete intent as a signal, skip the re-post when notifications are off.

**Ideal Final Result.** An append-only list of whole numbers on the phone, and one function that turns it into the notification. Every wake-up Android gives for free calls that function. The user sees the number at all times, and the app tells them plainly when the OS has removed it.

**Minor cuts proposed.** Drop the category-style breakdown text; no settings screen or theming; no Compose (a plain activity with a few views is enough); avoid AppCompat/Material if possible; unit-test only the parser, formatter and total logic; add `MY_PACKAGE_REPLACED` but no watchdog, alarm or service. Keep the main screen: it is the only home for the permission request, the setup checklist, the notification-health banner and Reset.

**Disagreements and caveats.**
* Storage: Room (implementation-readiness, structural) versus plain SQLite or a file (design, zoom-out, codebase-details). Unresolved; see Key Decisions.
* targetSdk 35 versus 36: design and implementation-readiness lean to 35 until the spike passes.
* `exported` on the boot receiver: codebase-details says true is the conventional choice; only the boot and package-replaced broadcasts need it. All other receivers are not exported.
* A hidden local event log (posted, dismissed, boot, reply rejected) was proposed for diagnosing OriginOS kills. A "last posted" time on the main screen is the lighter version; the full log may be more than a personal V1 needs.
* An "Android 14+ ongoing notifications can be swiped" claim and delete-intent behavior on OriginOS are from model knowledge and must be verified on the device.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  * One running total shown in the status bar as a runtime-drawn number icon, always on, in compact Indian format, with the exact figure in the expanded notification.
  * Amount-only entry (positive adds, negative corrects) from the notification reply box and a simple main screen, both going through one parser.
  * Reset with confirmation (showing the amount cleared); all entries kept in storage.
  * Notification re-posted after swipe-away, reboot, app update and app open, through one refresh function.
  * First-run flow: permission request and denied state, setup checklist, notification-health banner.
  * Permissions limited to POST_NOTIFICATIONS and RECEIVE_BOOT_COMPLETED; no service, polling or internet.
  * A real-device spike before the design is final.
* **Out-of-Scope Non-Goals:**
  * Categories, edit or delete of entries, history screen, automatic or monthly reset, reset alarm, quick-add dialog, Quick Settings tile, home screen shortcut, lock-screen breakdown handling (all deferred until the user asks).
  * Any code, schema column, manifest entry or UI placeholder for a deferred feature.
  * A foreground service, alarm or watchdog to survive force-stop (cannot work; rejected).
  * Cloud backup or any network feature.
  * Compose, a dependency-injection framework, a settings screen.

## Alternatives Weighed

* **Foreground service to keep the notification alive.** Rejected by reviewers: forces an extra persistent system notification, breaks the "no service" rule, and does not beat a user or OS force-stop.
* **Reset alarm or WorkManager polling to re-post.** Rejected: no background work is allowed, and V1 has no scheduled reset.
* **Storing a running total in SharedPreferences or DataStore.** Rejected: it violates "entries stay stored", risks lost updates between receiver and activity, and creates a second source of truth.
* **Room for storage.** Not rejected, but weighed against `SQLiteOpenHelper`: Room adds KSP, schema export and migrations for a one-table app; it helps the deferred features. Left for the user to confirm.
* **Append-only text file for entries.** Weighed as the smallest option; atomic writes and a truncated last line need care. Not recommended over SQLite.
* **Removing the main screen.** Rejected: the app needs a launcher activity to request the notification permission, to be opened once so boot broadcasts work, and to host Reset and the setup checklist.
* **Reset from the notification.** Rejected: too easy to tap by accident; Reset stays on the main screen behind a confirmation.
* **Direct boot (`LOCKED_BOOT_COMPLETED`).** Rejected: storage is not readable before first unlock.
* **Cached total column or value alongside the entries.** Rejected: second source of truth; summing a small table per event is cheap for years.
* **Rotating or single-line icon text instead of two stacked lines.** Kept open until the on-device spike shows whether the stacked layout is legible.
* **Large-entry confirmation prompt.** Rejected for V1: adds friction and pending state in the receiver.
* **Undo-last-entry and Undo-reset.** Proposed by reviewers; not adopted without the user's say, since they add scope. See Key Decisions.

## Key Decisions & Staging for Next Steps

**Decisions to carry into the design (reviewer consensus, subject to user confirmation):**
1. Data model: append-only entries (signed amount, timestamp) plus a reset marker ordered by row id; total is computed as the sum since the last marker; no stored total.
2. All writes are single atomic transactions; one process; write first, then post.
3. One idempotent refresh function posts the notification, called from app open, reply, delete, boot and package-replaced. Fixed channel id and notification id.
4. Delete-intent re-post only when notifications are enabled and the channel is still IMPORTANCE_LOW; once per dismissal, no retries.
5. Reply PendingIntent mutable and explicit; delete and content PendingIntents immutable; re-post after every reply including rejections.
6. Icon: white on transparent via `IconCompat.createWithBitmap`; text produced by a pure, unit-tested formatter with fixed Indian grouping.
7. Main screen kept, minimal: total, amount field with Add, Reset with confirmation showing the amount, permission and health banner, setup checklist, an on-open refresh.
8. Stable personal signing key, applicationId fixed now, explicit `allowBackup` decision, monotonic versionCode.
9. V1 contains no hooks for deferred features; the design lists how each deferred item would plug in.

**Questions for the user before drafting:**
* Whole rupees or paise (are decimals allowed)?
* Storage: `SQLiteOpenHelper` (recommended for few dependencies) or Room?
* May the total go negative, and how should it show in the icon?
* Rounding at the K and L boundaries (truncate recommended) and what shows above 99 lakh?
* Add a "last entry" line and echo text after each entry (small, no history screen)? Add "Undo last entry" (adds scope)?
* Lock-screen: show the exact total there (default) or hide it?
* `allowBackup` on or off?
* targetSdk 35 or 36?

**Answers (2026-11-22).** The user asked not to be given technical decisions. Claude decided these on the "simplest that works" rule:
* Whole rupees only. Negative totals allowed, shown with a minus sign.
* Icon rounds down (truncates toward zero). Crore ("Cr") above 99 lakh.
* Each accepted entry is echoed in the notification ("Added ₹450 · Total ₹2,340"). No undo button; corrections are negative entries.
* Lock screen shows the total.
* Storage: a plain append-only text file in app-private storage (no database library). `allowBackup` off.
* targetSdk 35, compileSdk 36. Runs normally on the user's Android 16 phone.
* No pre-approval spike: the user wants no code before the design is approved. The spike becomes the first implementation milestone, tested on the phone before the rest is built.

**Staging (from implementation-readiness):**
1. Spike on the phone (about half a day): a throwaway app with the IMPORTANCE_LOW channel, one ongoing notification, a hard-coded number icon (try two lines and one line), a "+ Add" RemoteInput, a delete-intent receiver and a boot receiver. Check: Monochrome rendering and legibility, reply and spinner clearing, swipe re-post, "Clear all" behavior, reboot, Recents swipe, reinstall. Go or no-go before the design is final; a failure may change the architecture.
2. Pure logic with JVM unit tests: parser, formatter, total and reset logic.
3. Storage.
4. Wire storage, refresh function and notification.
5. Main screen with Reset and confirmation.
6. Permission handling and setup checklist.
7. On-device acceptance pass.

**Acceptance criteria to write into the design (examples):** after entering 450 in the notification the status bar shows it within a couple of seconds; after reboot and first unlock the number returns; Reset then restart shows 0 while earlier rows remain; a rejected input produces a visible message and clears the spinner; the full manual matrix (swipe away, Recents swipe with and without lock, reboot, reinstall, permission denied, negative total, rapid double entry, timezone change) is run.

**Toolchain to pin in the design:** Android Studio, AGP, Gradle, Kotlin and JDK versions (compileSdk 36 needs a recent AGP), the install method for the phone, the keystore location, and putting the project under version control.

**Housekeeping:** mark the old handover and Decision 4 sections in the context file as superseded history so the design is not built from the wrong list.
