---
status: complete
phase: 20-release-publication
source: 20-01-SUMMARY.md, 20-02-SUMMARY.md, 20-03-SUMMARY.md, 20-04-SUMMARY.md, 20-05-SUMMARY.md, 20-06-SUMMARY.md, 20-07-SUMMARY.md, 20-08-SUMMARY.md
started: 2026-10-15T08:00:00Z
updated: 2026-10-15T13:26:03.539Z
---

## Current Test

[testing complete]

## Tests

### 1. Rollout note dedup item (Item 1) content
expected: Rollout note Item 1 (dedup) gives the measured consequence on server 26.9.18.11, the window values, the exactlyOnce=false outcomes, the KeeperMap self-check and the correction of gg1 item 1 — wording matches what operators need to decide, claims stay inside the measurements (coverage 20-03 D3).
result: pass

### 2. Rollout note Nullable Enum/Tuple item transparency
expected: Rollout note item on Nullable Enum and Tuple states plainly that the connector does NOT repair rows stored earlier — values store as sent, the old startup WARN is gone, the new fail-open WARN is quoted, single-field Nullable(Tuple) with a null field fails with a DataException, and each of the five detection queries is presented as a suspect finder, not proof (coverage 20-03 D4).
result: pass

### 3. CHANGELOG plain language and rollout-note agreement
expected: The 1.7.0_gg2 CHANGELOG section reads as plain operator language (no planning IDs, no jargon) and its claims agree with the rollout note (coverage 20-04 D4).
result: pass

### 4. 19-SECURITY.md threat-closure judgment
expected: 19-SECURITY.md exists, is committed, reads threats_open: 0, and the auditor's written rationale for closing T-19-11 (high) in the face of 19-REVIEW IN-03 and IN-04 is one you accept (coverage 20-05 D1).
result: pass

### 5. Rehearsal push happened only on your explicit approval
expected: The sync/v1.7.0 rehearsal push (plan 20-06) happened only after your own explicit answer, given directly to the orchestrator and recorded verbatim in E-62 (coverage 20-06 D3).
result: pass

### 6. Publication proceeded only on your explicit approval
expected: The custom_build publication (merge M, tag v1.7.0_gg2, plan 20-07) proceeded only on your own explicit answer, given directly to the orchestrator and recorded verbatim in E-70 before any command ran (coverage 20-07 D3).
result: pass

### 7. Post-publication manual-check outcomes recorded
expected: 20-MILESTONE-CLOSE-RECORD.md "Manual checks after publication" records all three D-11 outcomes verbatim (Dependabot enabled; default branch changed to custom_build by you; no dependabot.yml config error), and on GitHub the default branch really is custom_build with no Dependabot configuration error. Residue: the gradle Dependabot entry should show a recent "Last checked" now that the first jobs have finished.
result: pass

### 8. STATE.md Operator Next Steps sequence
expected: .planning/STATE.md Operator Next Steps reads in order — verify Phase 20, /gsd-audit-milestone (replacing the stale untracked 2026-10-10 audit snapshot), /gsd-complete-milestone carrying the close record, then revoke the Keychain token gsd17-gh-actions-read if used.
result: pass

### 9. Server version and dedup windows logged by committed test
expected: Server version and the three deduplication window settings are logged by a committed test, replicated window asserted above zero
result: pass
source: automated
coverage_id: 20-01 D1

### 10. Same-token/different-token retry matrix
expected: Same-token identical and changed-bytes retries are dropped, a different token lands, on ReplicatedMergeTree and on MergeTree with a window; a plain MergeTree stores every insert
result: pass
source: automated
coverage_id: 20-01 D2

### 11. exactlyOnce=true BEFORE_PROCESSING retry behavior
expected: exactlyOnce=true retry of a BEFORE_PROCESSING range - dropped and earlier rows (phantom rows included) kept when the earlier attempt landed, only the real rows stored when it did not, state ends AFTER_PROCESSING
result: pass
source: automated
coverage_id: 20-01 D3

### 12. exactlyOnce=false re-delivery behavior
expected: exactlyOnce=false re-delivery - dropped on ReplicatedMergeTree whether identical or changed, stored again on a plain MergeTree
result: pass
source: automated
coverage_id: 20-01 D4

### 13. Connector-path pins fail without connector token; src/main unchanged
expected: The connector-path pins fail without the connector's token and src/main is unchanged since P20_BASE
result: pass
source: automated
coverage_id: 20-01 D5

### 14. VERSION bump to v1.7.0_gg2 with anchors invariant
expected: VERSION is v1.7.0_gg2 and its anchor row moved with it in one commit with exactly those two files; clean build generates ARTIFACT_VERSION v1.7.0_gg2 and the anchors invariant ends result=PASS
result: pass
source: automated
coverage_id: 20-02 D1

### 15. Detection queries discriminate gg1 vs gg2 fixtures
expected: Each 999.1 shape that gg1 stores silently wrong (tuple-c2-plain, enum8-top, enum8-array, enum8-map, tuple-x1-null-field) has a detection query returning rows on the gg1-written fixture and none on the gg2-written one
result: pass
source: automated
coverage_id: 20-02 D2

### 16. Empty-CDC phantom-row query discriminates gg1 vs gg2
expected: The empty-CDC phantom-row query returns the two all-defaults rows gg1 writes on the bypassRowBinary route and none for gg2
result: pass
source: automated
coverage_id: 20-02 D3

### 17. Topic-scoped KeeperMap self-check
expected: The topic-scoped KeeperMap self-check, run on a state table created by KeeperStateProvider, returns exactly orders-1 while in flight, excludes orders-v2-0, and returns nothing once orders-1 is AFTER_PROCESSING
result: pass
source: automated
coverage_id: 20-02 D4

### 18. Table-classification query agrees with E-03
expected: The table-classification query marks default ReplicatedMergeTree and MergeTree with a window as deduplicating and plain MergeTree and ReplicatedMergeTree with window 0 as not, agreeing with E-03
result: pass
source: automated
coverage_id: 20-02 D5

### 19. MBean version segment and client name
expected: MBean version segment and client name read v1.7.0_gg2 on the bumped code and v1.7.0_gg1 on the old code
result: pass
source: automated
coverage_id: 20-02 D6

### 20. No probe residue in repository
expected: The probe class, both worktrees and any src change are absent from the repository
result: pass
source: automated
coverage_id: 20-02 D7

### 21. Rollout note structure (six items, TOC, gg1 pointer)
expected: Six items in the fixed order, each with What changed, Am I affected? and What to do; TOC matches the headings; opens with the gg1 pointer
result: pass
source: automated
coverage_id: 20-03 D1

### 22. Rollout note code blocks match evidence, read-only SQL
expected: Every sql and sh block equals an evidence block, every sql block is read-only, no bare planning ID, no long line copied from the gg1 note
result: pass
source: automated
coverage_id: 20-03 D2

### 23. Rollout note items for empty-CDC, log lines, MBean
expected: Empty-CDC item, message and log-line item with the alert-rule grep proven on a scratch directory, MBean and client-name item
result: pass
source: automated
coverage_id: 20-03 D5

### 24. Rollback-to-tag procedure proven on dry-run merge
expected: Rollback to tag pre-v1.7.0_gg2 proven on a dry-run merge D of the current tip into be6b8861; tag local only
result: pass
source: automated
coverage_id: 20-03 D6

### 25. No TRUNCATE instruction anywhere; gg1 note unchanged
expected: No TRUNCATE instruction in docs, CHANGELOG or README; gg1 note unchanged; nothing pushed
result: pass
source: automated
coverage_id: 20-03 D7

### 26. CHANGELOG gg2 section structure with L-38 obligation
expected: Plain-language 1.7.0_gg2 CHANGELOG section above the untouched gg1 section, with the carried L-38 obligation
result: pass
source: automated
coverage_id: 20-04 D1

### 27. FORK-DELTA refreshed and measured
expected: FORK-DELTA refreshed - identity text, resolvable paths, coverage of every Phase 17-19 code ID, measured raw numbers and counts, read-past-end wording
result: pass
source: automated
coverage_id: 20-04 D2

### 28. 19-REVIEW findings dispositioned, close record written
expected: All eight 19-REVIEW findings dispositioned and the milestone close record written
result: pass
source: automated
coverage_id: 20-04 D3

### 29. 20-REVIEW.md scope and structure
expected: 20-REVIEW.md reviews exactly the Phase 20 changes with the eight mandated confirmations and findings only under ### ID headings
result: pass
source: automated
coverage_id: 20-05 D2

### 30. 20-REVIEW-DISPOSITION.md complete, none open
expected: 20-REVIEW-DISPOSITION.md has one row per finding, none open, every fixed row starting with a resolvable commit
result: pass
source: automated
coverage_id: 20-05 D3

### 31. Release documents frozen after fixes
expected: Release documents frozen after the fixes - six items, quoted blocks in the evidence, gg1 tail equals P20_BASE, raw numbers equal git, no TRUNCATE instruction, gg1 note and protected paths unchanged
result: pass
source: automated
coverage_id: 20-05 D4

### 32. Frozen tree gated green
expected: Frozen tree gated - Docker-stopped testUnit green, two full runs green with the 18 named skips, anchors invariant PASS
result: pass
source: automated
coverage_id: 20-06 D1

### 33. Rehearsal push with exactly one green run
expected: Rehearsal push of sync/v1.7.0 at X0 with exactly one green tests.yaml run under X0; origin moved only by the new branch
result: pass
source: automated
coverage_id: 20-06 D2

### 34. Publication merge shape and rollback tag
expected: custom_build published as one --no-ff merge M whose tree equals the rehearsed X; rollback tag on origin before anything moved
result: pass
source: automated
coverage_id: 20-07 D1

### 35. Published SHA green, tag after green, refs exact
expected: M has exactly one green tests.yaml run; v1.7.0_gg2 pushed only after green and started no run; final refs exact; raw numbers reproduce on the tag
result: pass
source: automated
coverage_id: 20-07 D2

## Summary

total: 35
passed: 35
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
