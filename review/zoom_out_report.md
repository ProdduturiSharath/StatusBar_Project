# Goldfish Review Report

**Target:** `C:\users\shara\claude_projects\StatusBar_Project\docs\designs\statusbar_running_total.md` (`lines 1-735`)

Review guidance: the author wants the simplest possible app ("I enter numbers, do the math and show it in the status bar"). Findings flag excess beyond that and gaps that would stop it working on an iQOO Neo 10 / OriginOS 6 / Android 16. No new user-facing features are proposed.

## Reviewer Status

| Reviewer | Status |
| --- | --- |
| `egm-zoom-out` | completed |
| `egm-inventive-catalyst` | completed |

Overall: no Blockers. Both reviewers found the architecture sound (no libraries, no service, no database, append-only ledger, one idempotent refresh). Zoom-out verdict: PASS WITH CHANGES. No codebase exists, so there was nothing to reuse.

## Prioritized Findings (Blockers -> Major -> Nits)

None at Blocker severity.

### minSdk 26 forces version branches and a likely lint failure - Severity: Major
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L65-66, L167, L308-310, L335-337, L423, L496-499, L538-541, L617, L643-647, L660-662, L697`
* **Discrepancy Description**: minSdk 26 supports phones the document says are out of scope (non-goal, L65). It forces branches ("FLAG_MUTABLE on Android 12+", the Android 13+ permission prompt). `Theme.DeviceDefault.DayNight` is API 29+, so `lintDebug` ("no errors", L697) will likely flag a NewApi error and the stated gate cannot pass as written. The template's `values-night/themes.xml`, `backup_rules.xml` and `data_extraction_rules.xml` may also carry Material/AppCompat references or conflict with `allowBackup=false`, and the plan only mentions the dependency list.
* **Rationale & Recommendation**: Set minSdk to 36 (or at least 33/34) to remove all SDK-version branches. Otherwise use a theme available from API 26. State that the template's night theme and backup/extraction XML files are deleted or edited. The handover lists minSdk 26 as a constraint (handover L38), so the author must approve the change.

### Torn-line handling in the ledger can silently produce a wrong amount - Severity: Major (zoom-out) / Nit (inventive-catalyst); kept as Major
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L205-209, L596-598`
* **Discrepancy Description**: A cut-off write is skipped only if it does not match the line formats. A cut inside the amount digits still matches (`E,1795400000000,45` from `450`) and is counted with the wrong value. The next append also lands directly after a line with no newline, gluing a valid new entry onto garbage so it is skipped. Probability is very low (tiny synced write), but it is a money total.
* **Rationale & Recommendation**: When reading, count only lines that end in a newline. Before each append, write a newline first if the file does not end with one. Two sentences of design text plus one test.

### No check that a fresh install's silent notification icon is shown by default - Severity: Major (zoom-out) / Nit (inventive-catalyst); kept as Major
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L17-22, L346-348, L353-356, L373-376, L719-731`
* **Discrepancy Description**: OriginOS / Android 16 can hide silent (IMPORTANCE_LOW) notification icons via per-app or per-channel status-bar-icon toggles, or a "show silent notifications in status bar" setting. The third-party app proves the technique but not that a new app's new channel is shown by default. Neither reviewer could verify OriginOS behaviour.
* **Rationale & Recommendation**: Add to milestone 1's checks "fresh install shows the icon without touching any per-app or channel setting". If a toggle is needed, add it to the checklist card (L346-348) and README checklist (L725-729), and record the exact OriginOS wording.

### Detailed Implementation Plan is at the wrong altitude for this need - Severity: Major
* **Reviewer Lens:** `egm-zoom-out`
* **Affected Scope:** `L489-691` (also `L264-276, L308-310, L615-617`)
* **Discrepancy Description**: 19 file-level entries with method signatures, request codes, PendingIntent flags and pixel constants (96 px, 60/40 split, 92 px width). Too long for a non-developer to review, and code-level detail in a design document.
* **Rationale & Recommendation**: Keep the data flow diagram (L170-188), ledger format (L190-216), and input and icon rules. Collapse the per-file section to a short component list; leave signatures, constants and flags to the implementer.

### "Ask once" permission needs hidden state - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L349-350, L643-644`
* **Discrepancy Description**: "Ask once if never asked" implies a stored flag, contradicting "no state other than the ledger".
* **Rationale & Recommendation**: Call `requestPermissions` on every launch while not granted. Android stops prompting after repeated denials and the banner covers that case. Drop the flag.

### Edge-to-edge and the keyboard inset - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L114, L353-355, L552-553`
* **Discrepancy Description**: targetSdk 35 enforces edge-to-edge; with `adjustResize` the design pads for system bars but not the IME inset, so the amount field or button may sit under the keyboard.
* **Rationale & Recommendation**: Include IME insets (or rely on the ScrollView) in the main screen step and verify on the phone.

### Main-screen numberSigned keyboard may lack a minus key - Severity: Nit
* **Reviewer Lens:** `egm-inventive-catalyst`
* **Affected Scope:** `L340, L654-655`
* **Discrepancy Description**: Unverified that the iQOO/OriginOS keyboard shows a minus key, so negatives may not be enterable on the main screen. The notification reply box is unaffected.
* **Rationale & Recommendation**: Add to the milestone 5 phone pass. If it fails, use a plain text input type and let the parser reject bad input.

### Parser accepts more than needed - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L223-232`
* **Discrepancy Description**: Accepts spaces, commas, the rupee sign and plus; five distinct rejection messages; 20-character truncation. The numberSigned keyboard cannot type most of these.
* **Rationale & Recommendation**: Accept an optional sign, digits and commas; one rejection message echoing the input (for example "Type a whole number like 450 or -200."). Slightly less forgiving of pasted "₹1,200".

### Reset "Already at ₹0" special case - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L121-123, L590-593, L680-682`
* **Discrepancy Description**: Reset returns a Boolean, checks for entries since the last reset, and has a separate message and test.
* **Rationale & Recommendation**: Always append `R`. An extra R line is harmless because the total is a sum.

### Contradictory concurrency statements - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L210-214, L598-599`
* **Discrepancy Description**: Text says everything runs one at a time on the main thread, then adds `@Synchronized` and a `Ledger.get(context)` singleton.
* **Rationale & Recommendation**: Pick one protection. Construct a stateless `Ledger(File)` per call, or keep only `@Synchronized`; drop the singleton helper.

### Speculative or optional content (timestamps, last entry, Future Improvements, Alternatives, lint gate) - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`, `egm-inventive-catalyst`
* **Affected Scope:** `L203-205, L289-304, L337-339, L382-438, L455-456, L470-485, L589-595, L697`
* **Discrepancy Description**: Timestamps are stored only for a future history screen and are unused in V1 (and conflict with L472). The "Last entry" line appears in three places; it was approved in the brief (L162) as typo detection, so it is authorised. Future Improvements duplicates Non-Goals (L50-66) with plug-in plans for non-existent requirements. Seven Alternatives include straw men already ruled out by the "no background work" goal.
* **Rationale & Recommendation**: Drop timestamps or state plainly why they are kept. If cutting, keep last entry only in the notification. Delete Future Improvements; keep Alternatives A, B, G and fold the rest into Non-Goals. This is document weight rather than app weight.

### Toolchain: stripping the Empty Views template - Severity: Nit
* **Reviewer Lens:** `egm-inventive-catalyst`
* **Affected Scope:** `L167, L496-500, L527-533`
* **Discrepancy Description**: Hand-removing AppCompat, Material, ConstraintLayout and test dependencies means rewriting the template theme and layout, with more breakage risk than the dependencies cost.
* **Rationale & Recommendation**: Start from a "No Activity" template, or accept AppCompat (which contradicts "no AndroidX at runtime" in L167).

### Milestone 1 uses throwaway in-memory total - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`
* **Affected Scope:** `L509-515`
* **Discrepancy Description**: A temporary stand-in total means code is written twice.
* **Rationale & Recommendation**: Use the real (trivial) Ledger in milestone 1.

### Icon units and Cr case - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`
* **Affected Scope:** `L239-260`
* **Discrepancy Description**: Three units (K, L, Cr) and a 14-row table. Indian format was requested, so keep it; the Cr case (₹1 crore of personal spending) could optionally be dropped.
* **Rationale & Recommendation**: No action required.

### Open Questions: who decides the icon layout - Severity: Nit
* **Reviewer Lens:** `egm-zoom-out`
* **Affected Scope:** `L68-80`
* **Discrepancy Description**: Says "None for the user" but promises to show the user both icon layouts, while the user asked not to be given technical decisions (brief L159).
* **Rationale & Recommendation**: State who decides, for example "author decides, on legibility alone".

### Text inconsistencies - Severity: Nit
* **Reviewer Lens:** `egm-inventive-catalyst`
* **Affected Scope:** `L91, L149, L295, L334`
* **Discrepancy Description**: CUJ 9 banner text (L149) lacks the trailing period of the Main screen text (L334). The brief (L162) says the notification echoes "Added ₹450 · Total ₹2,340" but the design uses "Last entry: +₹450".
* **Rationale & Recommendation**: Make the banner text identical and confirm the echo wording with the user.

## Checked and consistent (no action)
IMPORTANCE_LOW channel; mutable explicit reply PendingIntent; refresh after every reply; delete-intent re-post; BOOT_COMPLETED plus MY_PACKAGE_REPLACED (not LOCKED_BOOT_COMPLETED); `exported` flags; fixed channel and notification ids; allowBackup off; Autostart/battery/Recents-lock checklist; single debug keystore with backup note; force-stop limit accepted; bitmap icon via `Icon.createWithBitmap`; icon table and CUJ 1 arithmetic. Icon legibility, swipe and "Clear all" behaviour, and OriginOS menu names are rightly deferred to the milestone 1 phone check.
