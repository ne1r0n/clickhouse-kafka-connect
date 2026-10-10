---
status: complete
phase: 08-orchestration-jmx-re-plumbing
source: [08-VERIFICATION.md, 08-01-SUMMARY.md, 08-02-SUMMARY.md, 08-03-SUMMARY.md]
started: "2026-09-19T13:35:00Z"
updated: "2026-09-19T14:30:00Z"
---

## Current Test

[testing complete]

## Tests

### 1. ProcessingTest.java is not byte-identical to upstream v1.6.0 — does ORCH-04 still hold?

expected: Either (a) accept the forced one-line adaptation as satisfying ORCH-04 in substance (all test assertions/scenarios unchanged from upstream; only the `new SinkTaskStatistics(0)` → `new SinkTaskStatistics(null, 0)` call site changed, forced by Phase 8's own constructor change in plan 08-01) and record that acceptance — including fixing the `.planning/MERGE-CHECKLIST.md` row that still says "takes upstream's version wholesale"; or (b) treat it as an open gap against plan 08-03's own `must_haves.truths` bar, which states "a diff that is... non-empty where byte-identity was expected[] is a failure signal and not a pass".

why_human: The executing plan itself flagged this as `human_judgment: true` (08-03-SUMMARY.md coverage item D2). Diff can show the fact — 35-line raw diff, 10 import-reorder lines, 1 substantive line — but cannot decide which reading of the requirement's wording governs.

result: pass
decision: "(a) — the one-line `new SinkTaskStatistics(null, 0)` adaptation is accepted as satisfying ORCH-04 in substance; MERGE-CHECKLIST.md's 'takes upstream's version wholesale' wording needs correcting to match."

### 2. Should REQUIREMENTS.md be flipped to Complete for ORCH-01, ORCH-04 and FREE-01?

expected: `.planning/REQUIREMENTS.md` is either updated to `[x]`/`Complete` for ORCH-01, ORCH-04 and FREE-01 — matching the precedent ORCH-02/ORCH-03 set when plan 08-02 flipped them in commit `9088bf12` — or a reason is recorded for deliberately leaving it stale pending a later phase's closure pass.

why_human: A documentation-consistency judgment, not a code defect. The underlying work is evidenced in `.planning/MERGE-CHECKLIST.md` Recorded Facts 11-13 and declared in 08-03-SUMMARY.md's `requirements-completed`, but the milestone's canonical requirement tracker still reads `[ ]`/`Pending` at lines 79, 82, 114, 198, 201, 202 — so the two documents disagree about whether the requirement is done.

result: pass
decision: "Flip ORCH-01, ORCH-04 and FREE-01 to Complete in REQUIREMENTS.md, matching the ORCH-02/ORCH-03 precedent. Action still outstanding — see Follow-Up Actions."

### 3. ORCH-01 evidence: zero stale 4-arg Processing call sites, upstream layering untouched

expected: ORCH-01 is dischargeable by recorded evidence rather than new code. Re-measured this session: `grep -rn 'new Processing(' src/ --include='*.java'` returns 16 call sites and every one is 5-arg; `git diff v1.6.0 HEAD` is a 0-line diff for `Processing.java`, `DeliveryStrategy.java`, `ChunkFlusher.java`, `DirectDeliveryStrategy.java`, `AtLeastOnceBufferStrategy.java` and `ExactlyOnceBufferStrategy.java`. You accept that this grep-plus-diff record is what ORCH-01 asked for.

why_human: 08-03-SUMMARY.md coverage item D1, `human_judgment: true` — the deliverable is the measurement record itself, which no automated test asserts.

result: pass

### 4. ORCH-04 evidence: Utils.java and Processing.java reviewed and recorded

expected: Utils.java (304 lines) and Processing.java (267 lines) were read in full and what was read was written down, rather than the requirement being restated. Re-measured this session: `Utils.java` vs v1.6.0 is import-reorder-only (zero non-import body lines changed); `UtilsTest.java` and `RangeContainerTest.java` are 0-line diffs. You accept that "read and record" has genuinely been discharged. (ProcessingTest.java's one substantive line is Test 1's question — not this one.)

why_human: 08-03-SUMMARY.md coverage item D2, `human_judgment: true` — whether a review was really performed versus asserted is not machine-checkable.

result: pass

### 5. FREE-01: ServerException classification confirmed and closed citing upstream #818

expected: `Utils.handleException` is confirmed to classify `com.clickhouse.client.api.ServerException` (client V2) alongside `ClickHouseException` (client V1) — quoted verbatim from `Utils.java:60-62` (`isServerError`) and `:77-80` (the ternary) — and the fork's "error classification is dead code on client V2" follow-up is closed citing upstream issue #818, with no new test added. You accept closing FREE-01 on a confirmation + citation rather than test coverage.

why_human: 08-03-SUMMARY.md coverage item D3, `human_judgment: true` — the plan's own prohibitions forbid pinning this with a test, so closure rests on your acceptance of the citation.

result: pass

### 6. MERGE-CHECKLIST.md row ownership is honest

expected: In `.planning/MERGE-CHECKLIST.md`, eight Phase-8-owned rows read `| done |`, two Phase-10-owned rows still read `| pending |` and carry a "Phase 8" annotation explaining why they were left, and three Recorded Facts (items 11, 12, 13) were appended before the closing JSON-path paragraph with item 10 un-renumbered. You accept that no other phase's row was flipped and Phase 11's zero-pending publication gate was not silently satisfied.

why_human: 08-03-SUMMARY.md coverage item D4, `human_judgment: true` — row ownership correctness is the judgment a later phase's gate depends on.

result: pass

### 7. Re-baselined acceptance gate: 1 failure / 0 errors accepted as passing

expected: The phase's D-12 gate was re-baselined from "zero failures" to "at most 1 failure, 0 errors, every remaining failure and skip named with its owning phase". The full suite measured 54 classes, 636 tests, 1 failure (`TableTest.extractNullables`), 0 errors, 35 skipped, with Failure and Skip inventories recorded in 08-02-SUMMARY.md. You accept the re-baselined condition as satisfying the phase gate.

why_human: 08-02-SUMMARY.md coverage item D4, `human_judgment: true` — every number is machine-verified, but whether a re-baselined bar counts as "passing" is a call for you, not the tooling.

result: pass

### 8. TableTest.extractNullables triaged (not fixed) with a named cause and owning phase

expected: `TableTest.extractNullables()`'s failure is triaged to a named root cause and assigned to an owning phase rather than fixed in Phase 8, and the read-only constraint was honored — `git diff v1.6.0 HEAD` is empty for both `Table.java` and `TableTest.java`. You accept that the triage names an actual cause rather than restating the symptom.

why_human: 08-02-SUMMARY.md coverage item D5, `human_judgment: true` — triage adequacy is exactly the check the plan's own `<verify>` block delegates to a human.

result: pass
recorded_cause: "`ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally strips Nullable `.null` subcolumns — a fork behavior already pinned by `DESC-01`, not a `Table.java` defect."

### 9. UTF-8 fix in the shared ClickHouse test helper

expected: The three previously charset-less `InputStreamReader` constructions in `src/testFixtures/java/.../ClickHouseTestHelpers.java` now decode UTF-8 explicitly. Re-verified this session: all three (lines 141, 160, 321) carry `StandardCharsets.UTF_8`. Note this plan's gate was compile + source assertion only — the behavioural proof (4 previously-failing live tests turning green) belongs to plan 08-02's live-suite gate, not here.

why_human: 08-01-SUMMARY.md coverage item D4, `human_judgment: true` — accepting a source-level assertion in place of a behavioural one is a scope judgment.

result: pass

### 10. Carry-forward of ORCH-01 / ORCH-04 / FREE-01 evidence from plan 08-01 to 08-03

expected: Plan 08-01's prohibitions reassigned the MERGE-CHECKLIST.md row flips for the ORCH-01/ORCH-04/FREE-01 evidence to plan 08-03, so 08-01-SUMMARY.md carried the evidence forward instead of flipping rows itself. You accept that this hand-off happened cleanly and nothing was dropped between the two plans.

why_human: 08-01-SUMMARY.md coverage item D5, `human_judgment: true` — a record-keeping hand-off across plans that no test observes.

result: pass

### 11. A started sink task's JMX MBean carries connector=<name>

expected: ObjectName carries `connector=<name>` sourced from `ClickHouseSinkTask.start()`'s `props.get("name")`, threaded through `ProxySinkTask` into `SinkTaskStatistics` (ORCH-02).
result: pass
source: automated
coverage_id: 08-01/D1

### 12. Per-topic MBean ObjectName carries the same connector=<name> segment

expected: Composed from the task-level name rather than duplicated (ORCH-03).
result: pass
source: automated
coverage_id: 08-01/D2

### 13. MBeanNameUnitTest un-excluded and green inside testUnit

expected: The canary this phase exists to restore compiles in the test source set and passes.
result: pass
source: automated
coverage_id: 08-01/D3

### 14. testUnit green with the new layering in place

expected: Upstream's DeliveryStrategy/ChunkFlusher above ProxySinkTask and the 5-arg Processing construction below it both survive plan 08-01's constructor change — 317 tests, 0 failures, 0 errors (ORCH-01).
result: pass
source: automated
coverage_id: 08-02/D1

### 15. A real registered MBean carries connector=<name> live at task and topic level

expected: Proven by MBeanRegistrationLiveTest, not only by MBeanNameUnitTest's formatter test (ORCH-02).
result: pass
source: automated
coverage_id: 08-02/D2

### 16. Four-tasks-two-connectors selective unregistration exercises connector=<name> live

expected: Per-topic MBean registration and the selective-stop path both pass live (ORCH-03).
result: pass
source: automated
coverage_id: 08-02/D3

## Summary

total: 16
passed: 16
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none — all 10 human checkpoints passed, 6 auto-covered by passing tests]

## Follow-Up Actions

Two checkpoints passed on a decision that implies a documentation edit. Both were authorized by
the user's `pass` on the checkpoint whose `expected` names the edit:

- test: 1
  action: "Correct the `.planning/MERGE-CHECKLIST.md` ORCH-04 row wording — it reads 'takes upstream's version wholesale', but ProcessingTest.java carries one forced `new SinkTaskStatistics(null, 0)` constructor-arg adaptation."
- test: 2
  action: "Flip ORCH-01, ORCH-04 and FREE-01 to `[x]`/`Complete` in `.planning/REQUIREMENTS.md` (lines 79, 82, 114, 198, 201, 202), matching the ORCH-02/ORCH-03 precedent set in commit `9088bf12`."
