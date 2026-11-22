# Goldfish Review Report

**Target:** `docs/designs/statusbar_running_total.md` (lines 1-523; the Detailed Implementation Plan from line 524 is out of scope)

## Reviewer Status

| Reviewer | Status |
| --- | --- |
| `egm-design` | completed |
| `egm-structural` | completed |
| `egm-style` | completed |

Single pass, one instance per reviewer, all run with model sonnet. Severity totals: 0 Blocker, 6 Major, 17 Nit.

## Prioritized Findings (Blockers -> Major -> Nits)

No Blockers.

### Torn-line repair turns a cut-off line into a counted entry - Severity: Major
* **Reviewer Lens:** `egm-design`, `egm-style` (reported independently by both)
* **Affected Scope:** L213-230 (read rule, append rule, torn-line rule), plus the matching unit-test description
* **Discrepancy Description**: The read rule ignores text after the last newline, so a cut-off `E,1795400000000,45` is not counted. The append rule then writes a newline before the next entry when the last byte is not a newline. That newline completes the cut-off line, which now matches the format and counts as 45 instead of the intended 450. The total changes silently on the next write, which is the failure the rule exists to prevent.
* **Rationale & Recommendation**: Before appending, either truncate the file back to just after the last newline, or write a terminator that cannot match either format (for example `!` then newline), so the fragment is skipped permanently. Alternatively, give valid lines an explicit end marker and count only those. Add "torn line, then append, then total" to the stated tests.

### Permission flow can loop and leaves no notification after the first grant - Severity: Major
* **Reviewer Lens:** `egm-design`, `egm-structural`, `egm-style` (all three)
* **Affected Scope:** L376-380 (also L309-312, CUJ 9 at L159-166)
* **Discrepancy Description**: "Every time the main screen opens, it asks for the permission" does not say which lifecycle callback makes the request. If it is `onResume`, the permission dialog pauses and resumes the activity, which can re-prompt in a loop. After repeated denials the request can flicker and leave the app unusable. Separately, on a fresh install `refresh()` runs before the grant and stops at step 2 (L309). Nothing re-posts the notification after the user taps Allow, so the status bar stays empty until the next open or entry. That looks like the app failing on first use.
* **Rationale & Recommendation**: Request the permission once per activity creation (`onCreate`), only if it is not granted. Call `refresh()` and update the banner from the permission-result callback and from `onResume` (which also covers returning from settings and from Recents, CUJ 10). Add "grant the permission, then the number appears without reopening the app" to the milestone 1 checks.

### "Always shows" in the status bar is not guaranteed on a crowded status bar - Severity: Major
* **Reviewer Lens:** `egm-design`, `egm-style`
* **Affected Scope:** L11-13, L41-43, L84-87, L305-308, L387-415 (Platform limits)
* **Discrepancy Description**: The design assumes the icon of a low-importance ongoing notification is always visible. The user's third-party test proves a number can be drawn. It does not show that the icon survives when other apps have notifications. Phone status bars show only a few notification icons and collapse the rest, and a low-ranked ongoing icon is dropped first. The icon is also hidden in full-screen apps, video and games. Channel importance is declared permanent (L305-308), so a wrong choice cannot be fixed later without a new channel id.
* **Rationale & Recommendation**: Add to the milestone 1 checks "icon still visible with WhatsApp, Gmail and 5 or more other notifications present". Decide the channel importance before coding if it fails (for example default importance with sound and vibration disabled). Add a Platform-limits bullet and soften "at all times" in the Goals to what is verifiable.

### Icon spec contradicts itself on anti-aliasing - Severity: Major
* **Reviewer Lens:** `egm-style`
* **Affected Scope:** L290-293
* **Discrepancy Description**: The spec requires smoothing on but also "no semi-transparent pixel". Smoothed text always produces partial-alpha edge pixels, and the status bar tint relies on alpha. An implementer must guess which rule wins, and the choice affects legibility at about 24 dp.
* **Rationale & Recommendation**: Reword to "smoothing on; partial alpha at glyph edges is expected; no colour, shadow or outline".

### No explicit "Risks & Mitigations" section - Severity: Major
* **Reviewer Lens:** `egm-structural`
* **Affected Scope:** L387-415 (Platform limits), L477-495 (Non-Functional Requirements)
* **Discrepancy Description**: The feature-spec standard asks for a risks section covering failure modes, security and limits. The risks are spread across the H3 "Platform limits" (nested under Technical Plan), Open Questions and Non-Functional Requirements. A reader checking against the standard cannot find them, and security and privacy sit only in one NFR bullet (L479-483).
* **Rationale & Recommendation**: Promote "Platform limits" to an H2 "Risks & Mitigations", or add a short H2 that lists force-stop, lowered channel, signing-key loss, torn write, crowded status bar and swipe/Clear-all, each pointing to its mitigation. Moving content is not required.

### Append failure is unspecified - Severity: Major
* **Reviewer Lens:** `egm-design`
* **Affected Scope:** L221-222
* **Discrepancy Description**: The design does not say what happens if the write throws (for example storage full). As written the entry would be lost silently, and the concepts brief names silent drops as the worst outcome.
* **Rationale & Recommendation**: Add one sentence. A failed append calls `refresh()` with "Couldn't save. Try again." and the total is unchanged. (Rated Major because it is a data-loss path. It is a one-line fix.)

### Banner wording and target for a lowered channel - Severity: Nit
* **Reviewer Lens:** `egm-design`
* **Affected Scope:** L362-364, L403-407
* **Discrepancy Description**: A channel set to "minimum" is treated as notifications off. The banner text "Notifications are off" is then inaccurate, and "Open settings" opens the app-level page.
* **Rationale & Recommendation**: Open the channel settings (`ACTION_CHANNEL_NOTIFICATION_SETTINGS`) in that case, or use general wording such as "The total can't show in the status bar. Check this app's notification settings".

### Last-entry text for a negative entry is not defined - Severity: Nit
* **Reviewer Lens:** `egm-design`, `egm-structural`, `egm-style`
* **Affected Scope:** L108, L117-122 (CUJ 2), L315-321
* **Discrepancy Description**: Only "Last entry: +₹450" is defined. After CUJ 2 an implementer would produce "+₹-405".
* **Rationale & Recommendation**: Add one row: "Last entry: -₹405". Also confirm the concepts brief's echo ("Added ₹450 · Total ₹2,340", brief L162) versus the design's "Last entry: +₹450". The reviewer found no recorded decision.

### ReplyReceiver sentence can be read as "refresh only on rejection" - Severity: Nit
* **Reviewer Lens:** `egm-structural`
* **Affected Scope:** L340-342 (versus L331-332)
* **Discrepancy Description**: "appends an entry if valid, then calls `refresh()` with the rejection message if it was not valid" can be read as refreshing only on rejection. The spinner fix depends on `refresh()` always running.
* **Rationale & Recommendation**: Reword to "...then always calls `refresh()` (with the rejection message if invalid)".

### minSdk and targetSdk are never stated - Severity: Nit
* **Reviewer Lens:** `egm-design`, `egm-structural`, `egm-style`
* **Affected Scope:** L382-385, L457, L73-74; handover L38 says minSdk 26
* **Discrepancy Description**: The only hint is "lowest supported Android version (13)" in Alternative E. The handover says 26. The edge-to-edge statement (L382-385) holds only for apps targeting 35 or higher. "Other phones will probably work" (L73-74) sits oddly with a hard floor.
* **Rationale & Recommendation**: State minSdk (33) and targetSdk once in the Technical Plan with the reason, and note the departure from the handover. Drop the "probably work" speculation.

### BootReceiver "open to system broadcasts only" is inaccurate - Severity: Nit
* **Reviewer Lens:** `egm-style`
* **Affected Scope:** L348
* **Discrepancy Description**: An exported receiver accepts explicit broadcasts from any app.
* **Rationale & Recommendation**: Say it is exported only because manifest receivers for system broadcasts need the declaration, and that spoofed broadcasts are harmless since it only calls `refresh()`.

### Alternatives section labelling and wording - Severity: Nit
* **Reviewer Lens:** `egm-design`, `egm-structural`, `egm-style`
* **Affected Scope:** L419-473
* **Discrepancy Description**: Alternative G is kept as the fallback, but sits under "Reason for Rejection". "Same safety as a database" (A) is overstated, given the torn-line logic. The framework `SQLiteOpenHelper` is not weighed. "Rejected by author" repeats five times with no link.
* **Rationale & Recommendation**: Relabel G as "Status". Soften A to "enough safety for one list of numbers, given the cut-off-line rule" and add one sentence on why `SQLiteOpenHelper` was not chosen. Link the handover once. Use "Rejected: simplicity" where that is the reason.

### Open Questions contradiction still unresolved - Severity: Nit
* **Reviewer Lens:** `egm-design`, `egm-structural`, `egm-style`
* **Affected Scope:** L76-83
* **Discrepancy Description**: "None for the user now" still sits next to "the user sees both on the phone and picks one". The earlier zoom-out report flagged this.
* **Rationale & Recommendation**: Name who decides (for example "the author decides on legibility alone").

### CUJ 3 "or nothing" may be unreachable - Severity: Nit
* **Reviewer Lens:** `egm-structural`, `egm-style`
* **Affected Scope:** L117-122, L318
* **Discrepancy Description**: The notification reply control is normally disabled for empty text, so an empty send may never reach the app.
* **Rationale & Recommendation**: Mark the empty case as main-screen only, or best effort.

### Milestone 1 checks incomplete - Severity: Nit
* **Reviewer Lens:** `egm-style`, `egm-design`
* **Affected Scope:** L26-28, L88-92, L96-97, L140-144 (CUJ 8)
* **Discrepancy Description**: "Settings > Status bar" is an unverified menu path but is not in the unconfirmed list. The check covers the number keyboard's minus key but not the phone-style fallback keypad. CUJ 8 does not list Autostart as a precondition (L395-396 says it may block `MY_PACKAGE_REPLACED` and boot broadcasts). `adb install -r` on vivo/iQOO may need the "Install via USB" developer option.
* **Rationale & Recommendation**: Add these to the milestone 1 verification list or mark them unverified. Add Autostart to CUJ 8's preconditions.

### Parser: Unicode minus and non-breaking space - Severity: Nit
* **Reviewer Lens:** `egm-design`
* **Affected Scope:** L244-249
* **Discrepancy Description**: Keyboards can produce U+2212 or NBSP, which would be rejected with the generic message.
* **Rationale & Recommendation**: Optionally normalise U+2212 to `-` and treat NBSP as a space. It is cheap and does not add a user-facing feature.

### Uncalibrated or defensive wording - Severity: Nit
* **Reviewer Lens:** `egm-style`
* **Affected Scope:** L484-485
* **Discrepancy Description**: "Typically well under 100 ms" has no measurement behind it.
* **Rationale & Recommendation**: Write "not measured", or drop it.

### Heading hierarchy and layout - Severity: Nit
* **Reviewer Lens:** `egm-structural`, `egm-style`
* **Affected Scope:** L280-282, L497-522
* **Discrepancy Description**: "Additional Resources" and "Future Improvements" are H3 under the H2 Non-Functional Requirements, although the horizontal rules imply top-level sections. The line wrapping at L280-282 is irregular. CUJ 9 banner text differs by a trailing period (L164 versus L363).
* **Rationale & Recommendation**: Make both headings H2. Rewrap the lines at L280-282. Make the banner text identical in both places.

## Checked and consistent (no action)

The icon table arithmetic (including the 14,560, 99,999, 2.35 crore and 12-digit rows) and the CUJ 1 totals agree across reviewers. So do rounding toward zero, the append-only total, the single-main-thread reasoning, the `BOOT_COMPLETED` plus `MY_PACKAGE_REPLACED` choice, the mutable explicit reply `PendingIntent`, the refresh-after-every-reply spinner fix, the force-stop and signing-key warnings, and the document's overall heading nesting.
