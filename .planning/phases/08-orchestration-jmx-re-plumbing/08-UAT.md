---
status: testing
phase: 08-orchestration-jmx-re-plumbing
source: [08-VERIFICATION.md]
started: "2026-09-19T13:35:00Z"
updated: "2026-09-19T13:35:00Z"
---

## Current Test

number: 1
name: Decide whether ProcessingTest.java's one substantive non-import diff line against v1.6.0 still satisfies ORCH-04 / ROADMAP criterion 4
expected: |
  Either (a) accept the forced one-line adaptation as satisfying ORCH-04 in substance — all test
  assertions and scenarios are unchanged from upstream, only the constructor call site changed —
  and record that acceptance explicitly, correcting the MERGE-CHECKLIST.md row wording that
  currently reads "takes upstream's version wholesale"; or (b) treat it as an open gap against
  plan 08-03's own stricter self-imposed bar.
awaiting: user response

## Tests

### 1. ProcessingTest.java is not byte-identical to upstream v1.6.0 — does ORCH-04 still hold?

expected: Either (a) accept the forced one-line adaptation as satisfying ORCH-04 in substance (all test assertions/scenarios unchanged from upstream; only the `new SinkTaskStatistics(0)` → `new SinkTaskStatistics(null, 0)` call site changed, forced by Phase 8's own constructor change in plan 08-01) and record that acceptance — including fixing the `.planning/MERGE-CHECKLIST.md` row that still says "takes upstream's version wholesale"; or (b) treat it as an open gap against plan 08-03's own `must_haves.truths` bar, which states "a diff that is... non-empty where byte-identity was expected[] is a failure signal and not a pass".

why_human: The executing plan itself flagged this as `human_judgment: true` (08-03-SUMMARY.md coverage item D2). Diff can show the fact — 35-line raw diff, 10 import-reorder lines, 1 substantive line — but cannot decide which reading of the requirement's wording governs.

result: [pending]

### 2. Should REQUIREMENTS.md be flipped to Complete for ORCH-01, ORCH-04 and FREE-01?

expected: `.planning/REQUIREMENTS.md` is either updated to `[x]`/`Complete` for ORCH-01, ORCH-04 and FREE-01 — matching the precedent ORCH-02/ORCH-03 set when plan 08-02 flipped them in commit `9088bf12` — or a reason is recorded for deliberately leaving it stale pending a later phase's closure pass.

why_human: A documentation-consistency judgment, not a code defect. The underlying work is evidenced in `.planning/MERGE-CHECKLIST.md` Recorded Facts 11-13 and declared in 08-03-SUMMARY.md's `requirements-completed`, but the milestone's canonical requirement tracker still reads `[ ]`/`Pending` at lines 79, 82, 114, 198, 201, 202 — so the two documents disagree about whether the requirement is done.

result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
