# Goldfish Review Report (Stage 9, Implementation Readiness)

**Target:** `C:\users\shara\claude_projects\StatusBar_Project\docs\designs\statusbar_running_total.md` (lines 542-814; lines 1-541 read for behaviour)

## Reviewer Status

| Reviewer | Status |
| --- | --- |
| `egm-implementation-readiness` | completed |
| `egm-codebase-details` | completed |
| `egm-style` | completed |

Single pass, all reviewers on model sonnet. Caveats: codebase-details API claims are from model knowledge, not fetched docs or device tests. Style did not read the context files or earlier reports; implementation-readiness did not read the earlier reports, so some Nits may repeat applied fixes.

Severity totals: 0 Blocker, 10 Major, 16 Nit.

## Prioritized Findings (Blockers -> Major -> Nits)

No Blockers.

### Template clean-up and file list incomplete - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 557-558, 586-623, 735-739
* **Discrepancy Description**: Wizard-generated files are not in the file list as Delete entries: `androidTest/.../ExampleInstrumentedTest.kt`, `test/.../ExampleUnitTest.kt`, `res/values/colors.xml`, `app/proguard-rules.pro`, `app/.gitignore`, template `activity_main.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `values-night/themes.xml`. Removing AndroidX test libraries leaves androidTest sources broken, and `testInstrumentationRunner` plus `androidTestImplementation` lines are not mentioned. The wizard Save location must be the `StatusBar_Project` folder itself, or a nested `StatusBarTotal` folder breaks every path. The manifest theme name is unspecified (the template's `Theme.StatusBarTotal` reference breaks if the style is renamed).
* **Rationale & Recommendation**: Add an explicit Delete list, state item 2 removes the runner line and all `androidTestImplementation` lines, state the Save location, and state the theme style name kept (parent replaced).

### Milestone 1 cannot reach its Go criterion - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`, `egm-style`
* **Affected Scope:** lines 567-582, 710-715
* **Discrepancy Description**: The "bare" main screen only calls `refresh()`, but on a fresh install POST_NOTIFICATIONS is not granted, so no total appears. The files (manifest, strings, theme, layout) that milestone 1 needs are listed under later items and not tagged by milestone. Go criteria also do not say how each Open Question outcome is classified (no-go versus record). Milestone 2 has no done condition.
* **Rationale & Recommendation**: Specify the milestone 1 `MainActivity` (permission request in `onCreate` with code 1, `refresh()` in `onResume`, trivial or no layout) and tag each file with its milestone. State that lint and unit tests run at the end of milestones 1 and 2. Add one line classifying each Open Question outcome, and a done line for milestone 2 (for example CUJs 1-5 and 9 pass).

### One-line icon fallback undefined - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 572-574 (and Alternative G, lines 81-83, 486-491)
* **Discrepancy Description**: No one-line formatting table (truncation, K/L/Cr forms, negatives), no selection mechanism, no tests, no renderer path.
* **Rationale & Recommendation**: Define `IconLabel.oneLine(total)` with a table and tests. Make the switch a single `const val` in `IconLabel.kt`, changed by rebuilding. A UI toggle would contradict the Non-Goals.

### `canNotify` null channel - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 685-689 (behaviour lines 314-320)
* **Discrepancy Description**: `getNotificationChannel("running_total")` returns null until `refresh()` step 1 has created it. `DismissReceiver` and the main-screen banner may call `canNotify` first, giving an NPE or a wrong result.
* **Rationale & Recommendation**: Add a private `ensureChannel(context)` called by both `refresh` and `canNotify` before reading importance, or state that null counts as allowed. `areNotificationsEnabled()` already covers the runtime permission on API 33+.

### Settings intents missing extras - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 719-722
* **Discrepancy Description**: `ACTION_APP_NOTIFICATION_SETTINGS` needs `putExtra(Settings.EXTRA_APP_PACKAGE, packageName)`. `ACTION_APPLICATION_DETAILS_SETTINGS` needs `data = Uri.fromParts("package", packageName, null)`. `FLAG_ACTIVITY_NEW_TASK` is needed from a non-activity context. If `ACTION_CHANNEL_NOTIFICATION_SETTINGS` is used, `EXTRA_CHANNEL_ID` too.
* **Rationale & Recommendation**: Write both intents out with their extras. CUJ 9 is not implementable otherwise.

### IconRenderer geometry left to guesswork - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 673-677 (behaviour lines 295-308)
* **Discrepancy Description**: No formula for the initial text size, vertical centering (`FontMetrics` versus text bounds), single-scale versus loop shrink, shared versus per-line size, or typeface. `sans-serif-condensed` may fall back silently on OriginOS.
* **Rationale & Recommendation**: Specify: initial size equals band height in px; scale once by `92f / measureText` if over 92; baseline from band centre minus `(ascent + descent) / 2`; `Paint.Align.CENTER` at x=48; typeface and its fallback; whether both lines share a size.

### Notification build details unspecified - Severity: Major
* **Reviewer Lens:** `egm-codebase-details`, `egm-implementation-readiness`
* **Affected Scope:** lines 679-690, 694
* **Discrepancy Description**: Needed: framework `android.app.RemoteInput` (no AndroidX), `Notification.Action.Builder` icon argument (`null as Icon?` or a drawable), request codes (0, 1, 2), `FLAG_UPDATE_CURRENT` with the mutable/immutable flags, explicit `Intent(context, ReplyReceiver::class.java)`. Reply extraction must treat a null Bundle or text as "" and reject it. Timestamp source (`System.currentTimeMillis()`) is unnamed.
* **Rationale & Recommendation**: State these explicitly. Optionally `setAllowGeneratedReplies(false)`.

### Theme and strings details - Severity: Major
* **Reviewer Lens:** `egm-codebase-details`, `egm-implementation-readiness`
* **Affected Scope:** lines 731-739, 618
* **Discrepancy Description**: The XML parent must be `android:Theme.DeviceDefault.DayNight`. That theme has an ActionBar, which with target 35 edge-to-edge may double-pad the top (needs device verification). An unescaped apostrophe in strings ("can't", "Couldn't") is an aapt2 error.
* **Rationale & Recommendation**: State the exact parent string, and either "action bar kept; padding applies to content only" or `windowActionBar=false` with `windowNoTitle=true`. Say to escape apostrophes as `\'`.

### Save-failure and Add/Reset handler flow unspecified - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`
* **Affected Scope:** lines 666-669, 691-696, 713-723
* **Discrepancy Description**: Item 7 defines the "Couldn't save" message on `false`, but items 10 and 13 do not cover the inline error view, the Add order (parse, `addEntry`, then clear field, `refresh`, redraw, or show error), or Reset failure. Reset at total 0 (append `R` or no-op) is unstated.
* **Rationale & Recommendation**: Add these steps and pick one Reset-at-zero behaviour.

### Gradle versions and JVM target conflict - Severity: Major
* **Reviewer Lens:** `egm-style`, `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 546-558, 603-609
* **Discrepancy Description**: "Keep the wizard's versions" conflicts with "Java and Kotlin target 17". The wizard may emit `compileSdk { version = release(36) }`, and JVM-target syntax varies by AGP. Mismatched `compileOptions` and Kotlin target gives an "Inconsistent JVM-target" error. The "runs normally on Android 16" claim for targetSdk 35 is unverified.
* **Rationale & Recommendation**: Say "edit the wizard's existing lines, following its syntax; keep the wizard's JVM target if it differs from 17". Mark targetSdk 35 behaviour as confirmed in milestone 1.

### Insets and input-field mechanics - Severity: Major
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** lines 713-730 (CUJ 4)
* **Discrepancy Description**: The Done key keeps the keyboard open only if `OnEditorActionListener` returns `true`; the amount field must take focus (`requestFocus()`/first focusable). Inset listener: use `systemBars() or ime()`, consider `displayCutout()`, apply all four sides, return the insets, name the root view, and set `clipToPadding=false` on a ScrollView. Rotation re-prompts for permission in `onCreate` (guard with `savedInstanceState == null` or accept).
* **Rationale & Recommendation**: State these. Merge the two inset mentions (lines 714 and 721-723).

### EntryParser digit definition - Severity: Nit
* **Reviewer Lens:** `egm-implementation-readiness`, `egm-codebase-details`
* **Affected Scope:** line 633 (behaviour lines 250-254)
* **Discrepancy Description**: Kotlin `isDigit()` and `toLong()` accept Devanagari digits.
* **Rationale & Recommendation**: State ASCII `'0'..'9'` only, and add a test, with the Unicode minus and non-breaking-space cases.

### LedgerTest and malformed-line gaps - Severity: Nit
* **Reviewer Lens:** `egm-implementation-readiness`
* **Affected Scope:** lines 659-662, 754-763
* **Discrepancy Description**: "Matches the format" is undefined; test list omits malformed lines, `lastEntry` since reset, cut-off `R` line, no-newline file, write failure, 12-digit total.
* **Rationale & Recommendation**: Define "E needs two Long-parseable fields; R needs one" and add the cases.

### Shared notification and banner text has no home - Severity: Nit
* **Reviewer Lens:** `egm-implementation-readiness`
* **Affected Scope:** lines 713-718
* **Discrepancy Description**: Title, "Last entry"/"No entries since reset" text and string resource names are needed by both `TotalNotifier` and `MainActivity`. Checklist copy source is unstated (take the README wording). Banner trailing period differs between lines 372-373 and 164; Reset dialog button labels unspecified.
* **Rationale & Recommendation**: Name a shared helper and resource names with format args; align wording.

### Lint and boundary notes - Severity: Nit
* **Reviewer Lens:** `egm-codebase-details`, `egm-implementation-readiness`
* **Affected Scope:** lines 285-290, 776
* **Discrepancy Description**: Lint will show an `OldTargetApi` warning (compileSdk 36, targetSdk 35); it is not an error. Negative-unit rule should read "unit chosen by absolute value, then truncated toward zero".
* **Rationale & Recommendation**: Note both.

### Style nits (brief) - Severity: Nit
* **Reviewer Lens:** `egm-style`
* **Affected Scope:** lines 546-814
* **Discrepancy Description**: (a) Lines 549-558: drop the API 26 history aside and rewrap the orphaned break at 555. (b) "Empty Views Activity" may be renamed; add "or closest equivalent". (c) Line 508/609: "raised each release" should read "+1 each build". (d) Lines 589-590 vs 511: path casing inconsistent. (e) Lines 592-598: "Generated by Android Studio" and "Create, from the template" are vague. (f) Lines 667-671, 686-688, 715: tighten or rewrap. (g) Line 731-733: "handles dark mode itself" to "provides day/night switching". (h) Line 781-796: step 8 "readable and not clipped" is subjective; say "all characters visible, none cut off". (i) Line 805: menu names are unverified guesses until milestone 1. (j) Line 814 provenance footer: keep or drop per convention.
* **Rationale & Recommendation**: Minor wording fixes; none block implementation.

## Checked and OK (per reviewers)
API levels and names for notifications, `FLAG_MUTABLE`, `Theme.DeviceDefault.DayNight` (API 29), Gradle task names, boot and package-replaced receivers, exported-receiver rule, `FileDescriptor.sync()`, `RandomAccessFile.setLength`, framework `requestPermissions`, no foreground-service or accessibility manifest rules apply, and targeting 35 avoids Android 16 predictive-back and edge-to-edge opt-out removal.
