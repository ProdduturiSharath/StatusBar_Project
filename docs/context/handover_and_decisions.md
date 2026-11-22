# StatusBar Spend Tracker — Context and Agreed Decisions

* **Date:** 2026-11-22
* **Status:** Input context for EGM brainstorm. Not a design document.

> **Superseded sections:** "Features from the Original Handover" and decision 4
> under "Decisions Agreed with the User" are history only. The authoritative V1
> list is "Version 1 Scope — Final" below, refined further in
> `../../concepts_brief.md`.

## Goal

A personal, fully offline Android app (Kotlin, Gradle) that tracks spending and
dues that the user enters by hand for the current billing cycle. It shows the
live total at all times as a number in the Android status bar, next to the clock
and network icons.

## Device and Distribution

* Device: iQOO Neo 10, OriginOS 6 (Android 16).
* Installed by sideloading a debug or personal APK. No Play Store, no root.
* The user installed a third-party app that draws a number in the status bar
  and confirmed it works on this phone. The technique (a notification whose
  small icon is a bitmap drawn at runtime) is therefore validated on this
  device. The status bar icon style is set to Monochrome in OriginOS settings.
* Build machine: Windows 11. Android Studio will be installed (not yet present).

## Hard Constraints (from the original handover)

* Manual entry only. No SMS reading, bank sync, automatic tracking, or network
  access of any kind.
* Permissions: only `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`.
  No `INTERNET`, `SYSTEM_ALERT_WINDOW`, SMS permissions, or Accessibility
  Services.
* No continuous background service and no polling. The posted notification
  stays in the status bar on its own; the app wakes only when the user adds or
  edits an entry, when the notification is dismissed, or on reboot.
* `compileSdk = 36`, `targetSdk` 35 or 36, `minSdk = 26`. Keep dependencies few.

## Features from the Original Handover

1. **Status bar number.** Silent notification channel at `IMPORTANCE_LOW`
   (not `IMPORTANCE_MIN`, which hides the status bar icon). Ongoing,
   alert-once notification. The cycle total is drawn in white on a transparent
   96x96 ARGB_8888 bitmap (bold condensed font) and set as the small icon.
   Compact Indian formatting for a 24dp slot: under 1,000 on one line;
   1,000-99,999 as two stacked lines ("14.5" over "K"); 1,00,000 and above as
   "1.2" over "L". Text shrinks to fit without clipping. The expanded
   notification shows the exact rupee breakdown
   (e.g. `Total: ₹14,500 • CC: ₹9,200 • EMI: ₹5,300`).
   * Swipe protection: a delete intent re-posts the notification if it is
     swiped away (ongoing notifications are dismissible on Android 14+).
   * Reboot: a boot receiver re-posts the notification.
2. **Fast manual entry.**
   * Inline reply ("+ Add") on the notification. Parses `450` (Other),
     `450 cc`, `2000 emi`, `-200` (subtract/correct).
   * A translucent quick-add dialog activity, excluded from Recents, with an
     auto-focused numeric field, category chips (CC, EMI, Other), add/subtract
     toggle, and Save. Exposed as a static app shortcut.
   * A Quick Settings tile that opens the quick-add dialog using
     `startActivityAndCollapse(PendingIntent)` (Android 14+ rule).
3. **Storage, history, billing cycle.** Room (SQLite) table of transactions
   (id, amount, category, note, timestamp). Main screen with cycle total and
   per-category breakdown, recent transactions with edit/delete, manual reset
   with confirmation, configurable monthly reset day, and a button to grant the
   notification permission and refresh the icon.

## Decisions Agreed with the User (2026-11-22)

1. **History is never deleted on reset.** A reset records a new cycle start
   date. The total shows only entries since the current cycle started. Older
   entries stay in the database.
2. **No WorkManager.** Because the total is computed from the cycle start date,
   a missed scheduled job cannot corrupt data. One alarm at the next reset date
   only refreshes the icon. (Proposed by Claude; accepted by the user.)
3. **Lock screen privacy.** The status bar number stays visible; the exact
   rupee breakdown in the notification is hidden on the lock screen.
4. **Version 1 is trimmed, later phases keep the rest.**
   * V1: status bar notification (with swipe and reboot recovery), inline
     reply, quick-add dialog, main screen with history, edit/delete, and cycle
     reset.
   * Later phases (deferred, not dropped): Quick Settings tile, home screen
     shortcut, and any other handover features not in V1.
5. **Known OriginOS risk.** Swiping the app away in Recents may force-stop it
   and remove its notification. The setup checklist must include locking the
   app in Recents, Autostart, and Unrestricted battery. Code cannot fully
   prevent this.

## Version 1 Scope — Final (2026-11-22, supersedes decision 4 above)

The user asked to keep version 1 as simple as possible. This replaces the
earlier V1 list and the category features in the original handover.

* One running total, shown as a number in the status bar (small-icon bitmap).
* Entry: an amount only. Positive adds; negative (e.g. `-200`) corrects.
  Two entry points: the "+ Add" inline reply on the notification, and a simple
  main screen.
* No categories (no CC/EMI/Other, no per-category breakdown).
* No editing or deleting entries. Corrections are negative entries.
* A Reset button with a confirmation step starts the total from zero. All
  earlier entries stay stored on the phone. No history screen in V1.
* No automatic or scheduled reset (so no reset alarm in V1).
* The notification is re-posted if swiped away and after reboot.
* Deferred to later versions, only when the user asks: categories, history
  screen, edit/delete, automatic monthly reset, quick-add dialog, Quick
  Settings tile, home screen shortcut, lock screen breakdown handling.

## Process Agreed with the User

* Use Google's Elephant-Goldfish Model (EGM) plugin for brainstorming, the
  design document, and independent review.
* Design and review run on Opus. Implementation runs on Sonnet where possible
  (Claude Code "Opus Plan" mode), only after the user approves the final design.
* No production code until the final design is approved.
