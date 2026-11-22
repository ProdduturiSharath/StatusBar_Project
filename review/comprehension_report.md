# Comprehension Report (Stage 6)

**Target:** `docs/designs/statusbar_running_total.md` (FeatureRange 1-494, DetailedRange 495-749)
**Reviewer:** `egm-comprehension` (Sonnet), completed.

## What this proposes

A single-phone Android app (iQOO Neo 10, OriginOS 6, Android 16), sideloaded, offline, with two permissions. The user types whole-rupee amounts (negative values correct mistakes), either in a "+ Add" reply box on a silent ongoing notification or on a one-screen main app. The running total is drawn as white text on a 96x96 bitmap and used as that notification's small icon, so it shows in the status bar. Reset zeroes the displayed total but keeps all history. Every input appends to a text ledger and then calls a single `refresh()` that re-reads the ledger and re-posts the notification. That is the only recovery mechanism, and there is no background service. The plan covers a toolchain, 3 milestones (the first a go/no-go phone check), 19 file-level changes, and a manual phone test list.

## How the existing system works, per this document

There is no existing system. This is a greenfield app, and the only "existing" facts are platform behaviours. Android keeps showing a posted notification without the app running. OriginOS may force-stop the app and then delivers no broadcasts, including boot, until the user reopens it. A newly installed app gets no broadcasts until it is opened once. Updates need the same signing key. Android 14 and later lets users swipe away ongoing notifications. The user has confirmed on this phone, using a third-party app, that a drawn number shows in the status bar.

## Findings and resolution

| # | Severity | Finding | Resolution |
| --- | --- | --- | --- |
| 1 | Major | Input rules: `.` not named as invalid; whether the sign counts toward the 9 digits is unclear. | Fixed: rules name the decimal point and say the sign is not counted. |
| 2 | Major | L and Cr not defined by name; behaviour at 1 crore and above unspecified. | Fixed: units defined; rows added for 10 crore and above. |
| 3 | Major | When the rejection message clears from the notification is unstated. | Fixed: it shows until the next refresh. |
| 4 | Major | No fallback if the keyboard has no minus key. | Fixed: fallback stated (phone-style keypad on the main screen; the notification reply uses the full keyboard). |
| 5 | Major | Small-icon mechanism and the Monochrome prerequisite are under-explained. | Fixed: one paragraph in Overview. |
| 6 | Major | Permission request described two ways; stray "Allow" button. | Fixed: file item matches the Main screen section. |
| 7 | Major | Which channel importance counts as "off" is ambiguous. | Fixed: anything below low counts as off. |
| 8 | Major | Not stated that BOOT_COMPLETED arrives after first unlock. | Fixed. |
| 9 | Major | Single-thread claim not explained. | Fixed: one process, main thread, no `android:process`. |
| 10 | Major | Milestone 1 go/no-go has no criteria; handover reference unexplained. | Fixed: criteria listed; handover described in one line. |
| 11 | Nit | Android/OriginOS terms used without gloss. | Partly fixed (Recents lock, Autostart glossed in Platform limits). |
| 12 | Nit | No reason given for targetSdk 35. | Fixed. |
| 13 | Nit | Grouping test range 4-10 digits vs 9-digit input cap. | Fixed: totals tested up to 12 digits. |
| 14 | Nit | Doc leans on external files for minor rationale. | Accepted: files are linked and inside the project. |
