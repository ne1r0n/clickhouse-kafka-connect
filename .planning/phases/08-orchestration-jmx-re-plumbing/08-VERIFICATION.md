---
phase: 08-orchestration-jmx-re-plumbing
verified: 2026-09-19T19:15:00Z
status: passed
score: 3/4 must-haves fully verified (1 verified with a disclosed caveat)
covered_files:

  - .planning/MERGE-CHECKLIST.md
  - .planning/REQUIREMENTS.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-01-PLAN.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-01-SUMMARY.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-02-PLAN.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-02-SUMMARY.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-03-PLAN.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-03-SUMMARY.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md
  - .planning/phases/08-orchestration-jmx-re-plumbing/08-REVIEW.md
  - build.gradle.kts
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/kafa/RangeContainerTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java

covered_digest: "v1:sha256:3c048d96624ee035ded5715e7976d345cd766566ab5901568737e160271ca5c2"
behavior_unverified: 0
overrides_applied: 0
human_verification:

  - test: "Decide whether ProcessingTest.java's one substantive, non-import diff line against v1.6.0 (`new SinkTaskStatistics(0)` -> `new SinkTaskStatistics(null, 0)`, a forced adaptation to Phase 8's own SinkTaskStatistics constructor change) still satisfies ROADMAP criterion 4 / ORCH-04's wording that 'ProcessingTest take[s] upstream's versions', and whether plan 08-03's own must_haves.truths #4 ('a diff that is... non-empty where byte-identity was expected[] is a failure signal and not a pass') should be honored literally."
    expected: "Either (a) accept the forced one-line adaptation as satisfying ORCH-04 in substance (all test assertions/scenarios are unchanged from upstream, only the constructor call site changed) and record that acceptance explicitly, or (b) treat it as an open gap against the plan's own stricter self-imposed bar."
    why_human: "This is a judgment call the executing plan itself flagged (08-03-SUMMARY.md coverage item D2 marks it `human_judgment: true`) — grep/diff can show the fact (60-line raw diff, 1 substantive line) but cannot decide which reading of the requirement's wording governs."
  - test: "Confirm whether `.planning/REQUIREMENTS.md`'s checkboxes and traceability-table rows for ORCH-01, ORCH-04 and FREE-01 should be flipped to `[x]`/`Complete` now, given that plan 08-03's SUMMARY (`requirements-completed: [ORCH-01, ORCH-04, FREE-01]`) and `.planning/MERGE-CHECKLIST.md`'s Recorded Facts 11-13 already carry the completing evidence, but `REQUIREMENTS.md` itself still reads `[ ]` / `Pending` for all three (verified live in the file at lines 79, 82, 114, 198, 201, 202)."
    expected: "REQUIREMENTS.md is either updated to `[x]`/`Complete` for ORCH-01, ORCH-04 and FREE-01 (matching ORCH-02/ORCH-03's precedent, which plan 08-02 did flip in commit 9088bf12), or a reason is recorded for deliberately leaving it stale pending a later phase's own closure pass."
    why_human: "This is a documentation-consistency judgment, not a code defect — the underlying work is evidenced in MERGE-CHECKLIST.md, but the milestone's primary requirement-tracking document disagrees with the phase's own summaries about whether the requirement is done."
---

# Phase 8: Orchestration & JMX Re-plumbing Verification Report

**Phase Goal:** Restore the JMX `connector=<name>` segment in its new home (`util.jmx.SinkTaskStatistics`), since upstream v1.6.0 deleted the fork's carrier method with no conflict marker and no compile error. The milestone's "prefer upstream" policy explicitly does not apply here because upstream's version fails JMX-01/JMX-02.

**Verified:** 2026-09-19T18:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `MBeanNameUnitTest`'s literal `connector=` assertions are green against the new home; JMX-01..04 pass re-targeted at `SinkTaskStatistics` | ✓ VERIFIED | JMX-01/02/03 all present as `@DisplayName`-named test methods in `MBeanNameUnitTest.java` (11 tests); JMX-04 present in `MBeanRegistrationLiveTest.java` (2 tests). Live XML: `build/test-results/testUnit/TEST-....MBeanNameUnitTest.xml` = 11 tests/0 fail/0 err; `build/test-results/test/TEST-....MBeanRegistrationLiveTest.xml` = 2 tests/0 fail/0 err. Chain confirmed by direct source read: `ClickHouseSinkTask.java:41` `connectorName = props.get("name")` → `ClickHouseSinkTask.java:48` `new ProxySinkTask(clickHouseSinkConfig, errorReporter, connectorName)` → `ProxySinkTask.java:51` `new SinkTaskStatistics(connectorName, id)` → `SinkTaskStatistics.buildMBeanName` renders `connector=%s` with `null → "unknown"` version default and `:` → `_` sanitization intact (source read directly, lines 162-178). |
| 2 | Upstream's new per-topic MBean carries the same `connector=<name>` segment | ✓ VERIFIED | `SinkTaskStatistics.getTopicMBeanName(topic)` = `getMBeanName() + ",topic=" + topic` (composition, not a second format string — one `String.format(` call in the file, confirmed by source read). Live proof: `MBeanRegistrationLiveTest#fourTasksAcrossTwoConnectorsRegisterDistinctMBeansAndSelectiveStopUnregistersOne` — 0 failures/0 errors in the live run. |
| 3 | `./gradlew testUnit` is green with the new layering; no stale 4-arg `Processing` call site anywhere in the tree | ✓ VERIFIED | `grep -rn 'new Processing(' src/ --include='*.java'` = exactly 16 call sites (1 in `ProxySinkTask.java:53`, 15 in `ProcessingTest.java`), independently re-run and confirmed — all pass 5 arguments. `Processing.java` declares exactly one `public Processing(...)` constructor (5-arg). `testUnit`: 317 tests, 0 failures, 0 errors (aggregated fresh from `build/test-results/testUnit/*.xml`, matching 08-02-SUMMARY's claim). Live full suite: 636 tests, 1 failure (`TableTest.extractNullables`, pre-triaged carry-in, not Phase 8's), 0 errors, 35 skipped — matches the D-12 re-baselined pass condition (Phase 8's own two JMX canaries green, ≤1 inherited failure, 0 errors). |
| 4 | `Utils.java`/`Processing.java` read and reviewed; `UtilsTest`/`ProcessingTest` take upstream's versions; FREE-01 closed citing upstream #818 | ⚠️ VERIFIED WITH DISCLOSED CAVEAT | `Utils.java` vs `v1.6.0`: genuinely import-reorder-only (re-confirmed directly: zero non-import/non-blank changed lines). `Processing.java` vs `v1.6.0`: 0-line diff (re-confirmed). `UtilsTest.java` vs `v1.6.0`: 0-line diff (re-confirmed). `RangeContainerTest.java` vs `v1.6.0`: 0-line diff (re-confirmed). FREE-01: `Utils.isServerError()` = `t instanceof ClickHouseException \|\| t instanceof ServerException`, `handleException`'s ternary reads the code off whichever matched — both quotes re-confirmed verbatim against the live file; `#818` and the classification text are present in `MERGE-CHECKLIST.md` Recorded Fact 12. **However:** `ProcessingTest.java` vs `v1.6.0` is **NOT** a 0-line diff — re-confirmed a 35-line raw diff (10 import-reorder lines + 1 substantive line: `new SinkTaskStatistics(0)` → `new SinkTaskStatistics(null, 0)`, forced by Phase 8's own `SinkTaskStatistics` constructor change in plan 08-01). Plan 08-03's own `must_haves.truths` explicitly states: "a diff that is... non-empty where byte-identity was expected[] is a failure signal and not a pass" — by that self-imposed bar, this is not a clean pass, though the test's actual assertions/scenarios are unchanged from upstream (only the call-site adaptation differs). This was honestly disclosed in `08-03-SUMMARY.md`'s Deviations section, but **not** reflected in `MERGE-CHECKLIST.md`'s row text, which still reads "takes upstream's version wholesale" for this file. Routed to human verification below. |

**Score:** 3/4 truths fully verified without qualification, 1 verified with a disclosed caveat that the executing plan itself flagged for human judgment.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/.../util/jmx/SinkTaskStatistics.java` | `connectorName` field, `buildMBeanName` pure static, `getMBeanName()`/`getTopicMBeanName(topic)` instance methods | ✓ VERIFIED | All four symbols present and correctly composed (source read in full). |
| `src/main/java/.../sink/ProxySinkTask.java` | 3-arg constructor passing `connectorName` through | ✓ VERIFIED | `ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, String connectorName)` at line 42; `new SinkTaskStatistics(connectorName, id)` at line 51; `new Processing(...)` 5-arg call unchanged at line 53. |
| `src/main/java/.../sink/ClickHouseSinkTask.java` | `props.get("name")` restored and threaded | ✓ VERIFIED | Line 41: `connectorName = props.get("name");`; line 48: passed to `ProxySinkTask`. |
| `src/test/java/.../MBeanNameUnitTest.java` | Un-excluded, 11 tests (6 original + 5 new), 0 changed literals | ✓ VERIFIED | Present in compiled test source set (no longer excluded in `build.gradle.kts`); 11 `@Test`/`@ParameterizedTest`-derived executions, live XML confirms 0 failures/errors/skips. |
| `src/testFixtures/.../ClickHouseTestHelpers.java` | 3 `InputStreamReader` sites carry `StandardCharsets.UTF_8` | ✓ VERIFIED | Confirmed via source read; matches SUMMARY's claim, and the two previously-failing live classes (`JsonDynamicRoundTripTest`, `SmtSinkLandingTest`) are both green in the live run. |
| `.planning/MERGE-CHECKLIST.md` | 8 Phase-8 rows flipped `done`, 2 Phase-10 rows annotated `pending`, 3 new Recorded Facts | ✓ VERIFIED | All 8 rows confirmed `done` (6 Section 1 + 2 Section 2); both `ClickHouseSinkTaskTest.java` and the HARN-04 `ClickHouseTestHelpers.java` rows confirmed `pending` with a `Phase 8` note naming the commit; items 11/12/13 present exactly once each, closing JSON-path paragraph still last. **Caveat:** the `ProcessingTest.java` row (flipped `done`) reads "takes upstream's version wholesale," which is not literally accurate per Truth 4 above — see human verification. |
| `.planning/REQUIREMENTS.md` | ORCH-01..04, FREE-01 present and traceable to Phase 8 | ⚠️ PRESENT, STATUS STALE | All five requirement IDs exist and are mapped to "Phase 8" in the traceability table. ORCH-02/ORCH-03 are `[x]`/`Complete` (flipped by plan 08-02, commit `9088bf12`). ORCH-01, ORCH-04, FREE-01 remain `[ ]`/`Pending` in this file despite plan 08-03's SUMMARY declaring `requirements-completed: [ORCH-01, ORCH-04, FREE-01]` with full evidence recorded in `MERGE-CHECKLIST.md`. This is a genuine bookkeeping inconsistency between the milestone's canonical requirement tracker and the phase's own completion record. See human verification below. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `ClickHouseSinkTask.start()` | `ProxySinkTask` constructor | `new ProxySinkTask(clickHouseSinkConfig, errorReporter, connectorName)` | ✓ WIRED | Single call site, confirmed by source read and by `grep -n "new ProxySinkTask("`. |
| `ProxySinkTask` constructor | `SinkTaskStatistics` constructor | `new SinkTaskStatistics(connectorName, id)` | ✓ WIRED | Single call site in production code (line 51). |
| `SinkTaskStatistics.getMBeanName()` | `SinkTaskStatistics.getTopicMBeanName(topic)` | string composition `getMBeanName() + ",topic=" + topic` | ✓ WIRED | No second format string exists in the file; per-topic name is a strict extension of the task-level name. |
| `SinkTaskStatistics.registerMBean()` | JMX platform MBean server | `MBeanServerUtils.registerMBean(this, name)` | ✓ WIRED, LIVE-PROVEN | `MBeanRegistrationLiveTest` (2/2 green) exercises this against a real ClickHouse-backed sink task. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `new Processing(` arity is uniformly 5 across the tree | `grep -rn 'new Processing(' src/ --include='*.java' \| wc -l` | 16 (re-run independently) | ✓ PASS |
| `Processing` declares exactly one constructor | `grep -c 'public Processing(' Processing.java` | 1 | ✓ PASS |
| `Utils.java` diff vs `v1.6.0` is import-only | `git diff v1.6.0 HEAD -- Utils.java` inspected line-by-line | Only a blank-line/import-position change; `isServerError`/`handleException` bodies unchanged | ✓ PASS |
| `ProcessingTest.java` diff vs `v1.6.0` is empty (plan's own expectation) | `git diff v1.6.0 HEAD -- ProcessingTest.java` | 35-line raw diff: 10 import-reorder lines + 1 substantive line (`new SinkTaskStatistics(0)` → `new SinkTaskStatistics(null, 0)`) | ✗ FAIL against the plan's own stated expectation (disclosed, see Truth 4) |
| Live suite matches D-12's re-baseline | Aggregated `build/test-results/test/*.xml` | 636 tests, 1 failure, 0 errors, 35 skipped | ✓ PASS |
| `git diff --name-only 9088bf12..HEAD` touches no file outside `.planning/` | `git diff --name-only 9088bf12..HEAD \| grep -v '^\.planning/'` | empty | ✓ PASS — confirms the on-disk test-result XML is current for the reviewed source |

### Probe Execution

Not applicable — this phase has no `scripts/*/tests/probe-*.sh` and none is referenced in its PLAN/SUMMARY files. Step 7c: SKIPPED (no probes declared).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| ORCH-01 | 08-02, 08-03 | Layering resolved against upstream; no stale 4-arg `Processing` call site | ✓ SATISFIED (code) / ⚠️ REQUIREMENTS.md still `Pending` | Grep + diff evidence confirmed above; `REQUIREMENTS.md` checkbox not updated — see human verification. |
| ORCH-02 | 08-01, 08-02 | `connector=<name>` restored on sink-task MBean | ✓ SATISFIED | `REQUIREMENTS.md` correctly `[x]`/`Complete`. |
| ORCH-03 | 08-01, 08-02 | Per-topic MBean carries the same segment | ✓ SATISFIED | `REQUIREMENTS.md` correctly `[x]`/`Complete`. |
| ORCH-04 | 08-03 | `Utils.java`/`Processing.java` reviewed; `UtilsTest`/`ProcessingTest` on upstream's versions | ⚠️ SATISFIED WITH CAVEAT (code) / REQUIREMENTS.md still `Pending` | `ProcessingTest.java` not byte-identical (see Truth 4); `REQUIREMENTS.md` checkbox not updated. |
| FREE-01 | 08-03 | `Utils.handleException` classifies V2's `ServerException`; fork follow-up closed citing #818 | ✓ SATISFIED (code) / ⚠️ REQUIREMENTS.md still `Pending` | Verbatim quotes re-confirmed; `#818` present in `MERGE-CHECKLIST.md`; `REQUIREMENTS.md` checkbox not updated — see human verification. |

No orphaned requirements: `grep -E "Phase 8" .planning/REQUIREMENTS.md` maps exactly ORCH-01..04 and FREE-01 to this phase, and all five appear in at least one of the three plans' `requirements:` frontmatter fields (08-01: ORCH-02/03; 08-02: ORCH-01/02/03; 08-03: ORCH-01/04, FREE-01).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `SinkTaskStatistics.java` | 17 | `LOGGER` bound to `ProxySinkTask.class` instead of `SinkTaskStatistics.class` | ℹ️ Info (pre-existing, not introduced by this phase — flagged in `08-REVIEW.md` WR-01) | Cosmetic/observability only; does not affect the `connector=` naming this phase delivers. |
| `ClickHouseTestHelpers.java` | 140-141, 159-160, 320-321 | Charset-fixed `InputStreamReader`/`BufferedReader` still not explicitly closed (relies on `queryResponse.close()`) | ℹ️ Info (pre-existing pattern, only the charset argument was added by this phase — flagged in `08-REVIEW.md` WR-02) | Test-only code; latent resource-leak risk under a future client change, not a production concern. |

No `TBD`/`FIXME`/`XXX` debt markers found in any file this phase touched (checked via `grep -n -E "TBD|FIXME|XXX"` across the `covered_files` source list).

## Human Verification Required

### 1. ProcessingTest.java byte-identity vs. ORCH-04's wording

**Test:** Decide whether the disclosed one-line, phase-8-forced adaptation to `ProcessingTest.java` (`new SinkTaskStatistics(0)` → `new SinkTaskStatistics(null, 0)`) still satisfies "ProcessingTest take[s] upstream's versions" for ORCH-04, given the executing plan's own `must_haves.truths` states a non-empty diff there is "a failure signal and not a pass."
**Expected:** An explicit acceptance (recorded, e.g. as an override or an amended `MERGE-CHECKLIST.md` row wording) or a decision that this is an open gap.
**Why human:** The executing plan itself marked this `human_judgment: true` — the fact is unambiguous (35-line diff, 1 substantive line), but which reading of the requirement's wording governs is a judgment call, not something grep can resolve.

### 2. REQUIREMENTS.md checkbox staleness for ORCH-01, ORCH-04, FREE-01

**Test:** Confirm whether `.planning/REQUIREMENTS.md` should be updated to flip ORCH-01, ORCH-04 and FREE-01 to `[x]`/`Complete` (matching the precedent plan 08-02 set for ORCH-02/ORCH-03 in commit `9088bf12`), given that `MERGE-CHECKLIST.md`'s Recorded Facts 11-13 and 08-03-SUMMARY.md's `requirements-completed` field both already declare these three done.
**Expected:** Either `REQUIREMENTS.md` is updated to match, or a stated reason for leaving it stale is recorded (e.g. deferred to a phase-closing step outside plan scope).
**Why human:** This is the milestone's canonical requirement tracker disagreeing with the phase's own completion record — a documentation-consistency call, not a code defect.

## Gaps Summary

No functional or structural gap was found: the JMX `connector=<name>` re-plumb is correctly threaded end-to-end, live-proven at both the task and per-topic level, `testUnit` and the full live suite both meet their (correctly re-baselined) pass conditions, and `Utils.java`/`Processing.java` were genuinely read and are byte-identical or import-only against upstream. Two documentation-precision issues were found and are both disclosed already in the phase's own SUMMARY files, just not fully reconciled across `MERGE-CHECKLIST.md` and `REQUIREMENTS.md`: (1) the `ProcessingTest.java` checklist row claims "upstream's version wholesale" when one line differs by necessity, and (2) `REQUIREMENTS.md`'s checkboxes for ORCH-01/ORCH-04/FREE-01 were never flipped despite the phase's own evidence-complete record. Neither blocks the phase's actual deliverable — the JMX naming fix works and is proven live — but both deserve an explicit human decision before the milestone's later phases (in particular Phase 11's zero-pending publication gate) inherit them silently.

---

## Human Verification Resolution (2026-09-19T19:15:00Z)

Both human-verification items above were resolved during `/gsd-verify-work 08` (UAT tests 1 and
2, both `pass`) and the decisions were applied:

1. **ProcessingTest.java row** — option (a): the single forced `new SinkTaskStatistics(null, 0)`
   call-site adaptation is accepted as satisfying ORCH-04 in substance. The
   `.planning/MERGE-CHECKLIST.md` row wording was corrected from "takes upstream's version
   wholesale" to name the adaptation explicitly.
2. **REQUIREMENTS.md** — flipped ORCH-01, ORCH-04 and FREE-01 to `[x]`/`Complete` in both the
   checklist and the status table, matching the ORCH-02/ORCH-03 precedent.

`status` was advanced `human_needed` → `passed` and `covered_digest` recomputed, because applying
these two decisions edited `.planning/MERGE-CHECKLIST.md` and `.planning/REQUIREMENTS.md` — both
covered files — which invalidated the prior fingerprint. Confirmed before recomputing that
`git diff 911a3160..HEAD` touched planning documents only: no file under `src/`, no build script,
and no `VERSION` change. The verified behaviour is therefore unchanged.

---

*Verified: 2026-09-19T18:30:00Z*
*Verifier: Claude (gsd-verifier)*
*Human items resolved + re-fingerprinted: 2026-09-19T19:15:00Z (gsd-verify-work)*
