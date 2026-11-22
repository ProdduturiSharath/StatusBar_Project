# Consolidated Review Report

**Target:** `docs/designs/statusbar_running_total.md`
**Date:** 2026-11-22. All reviewers ran once on Sonnet. Stage 8 (intermediate
zoom-out) was skipped at the user's request to avoid repeat reviews.

| Stage | Reviewers | Blocker | Major | Nit | Report |
| --- | --- | --- | --- | --- | --- |
| 5 Zoom out | zoom-out, inventive-catalyst | 0 | 4 | 11 | `zoom_out_report.md` |
| 6 Comprehension | comprehension | 0 | 10 | 4 | `comprehension_report.md` |
| 7 Critic | design, structural, style | 0 | 6 | 17 | `critic_report.md` |
| 9 Implementation readiness | implementation-readiness, codebase-details, style | 0 | 11 | 16 | `implementation_readiness_report.md` |

## Applied (by theme)

| Theme | Change in the design | From stage |
| --- | --- | --- |
| Data safety | Only newline-terminated lines count; a cut-off last line is trimmed from the file before the next write (never completed with a newline); failed writes show "Couldn't save". | 5, 7 |
| Simplicity | minSdk 33 (no version branches); one rejection message; Reset always appends; no locking or shared ledger instance. | 5 |
| Permission flow | Ask in `onCreate` only (no loop), refresh after the answer, banner opens settings. | 5, 6, 7, 9 |
| Clarity | K/L/Cr defined; ≥1 crore rows; message lifetime; boot-after-unlock; single-thread reasoning; channel "off" threshold; glossary; Monochrome prerequisite. | 6 |
| Phone checks | Fresh-install icon, minus key (with `inputType="phone"` fallback), crowded status bar, go/no-go criteria and outcome handling. | 5, 6, 7, 9 |
| Exact Android details | Template clean-up list, theme parent, channel creation, notification flags and request codes, settings intents, icon geometry, one-line fallback constant, main screen flow, insets. | 9 |

## Not applied

* Collapse the implementation plan (stage 5): kept, because the Sonnet
  implementer needs it.
* Drop entry timestamps (stage 5): kept; they cost nothing and enable a future
  history screen.
* A separate "Risks & Mitigations" heading (stage 7): covered by "Platform
  limits".
* Remaining wording and formatting nits.

## Residual risks (checked on the phone in milestone 1)

* Two-line icon legibility (fallback: one-line constant).
* Fresh-install icon visibility on OriginOS with a silent channel.
* Swipe / "Clear all" dismiss signal on OriginOS.
* Crowded status bar hiding the icon.
* Force-stop from Recents (cannot be fixed in code; setup checklist).
