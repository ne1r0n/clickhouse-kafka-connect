---
phase: 42-isolated-high-risk-edits
verified: 2026-11-02T16:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/42-isolated-high-risk-edits/42-01-PLAN.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-01-SUMMARY.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-02-PLAN.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-02-SUMMARY.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-03-PLAN.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-03-SUMMARY.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-04-PLAN.md"
  - ".planning/phases/42-isolated-high-risk-edits/42-04-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java"
  - "src/main/java/com/clickhouse/kafka/connect/util/Utils.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/HandleExceptionCauseCycleUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java"
covered_digest: "v4:sha256:3d866a2214fce2081928a6a6188398b6ef31e5aad102c5418529fcce97525350"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 42: Isolated High-Risk Edits Verification Report

**Phase Goal:** Three small edits in high-risk files land one commit each, tests first and each behind its own gate, so any one can be reverted alone: RSK-01 (exactly-once processing reads the original topic, state key does not move), RSK-02 (`Utils.handleException` keeps an already-`RetriableException` retriable, chain form, changed-row set written down in advance), RSK-03 (start-up WARN for non-transient codes in `additionalRetriableErrorCodes`), and the OI-35 cases the edit does not fix written down as limits (RSK-04).
**Verified:** 2026-11-02
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP Phase 42 success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | RSK-01: `Processing.doLogic` line 137 reads `originalTopic()`; `KeeperStateProvider`/state key/KeeperMap row unchanged; all 14 `ProcessingTest` cases unchanged and pass; the remaining `onPartitionRemoved` mismatch recorded in FORK-DELTA | VERIFIED | `Processing.java:137` reads `String originalTopic = record.getSinkRecord().originalTopic();` (diff vs `1c808c02` is that one line). `git diff 1c808c02 HEAD -- sink/state sink/kafka` is empty; `ProcessingTest.java` byte-unchanged; the `ProcessingTest` XML shows 14 tests, 0 failures. `OriginalTopicInsertedOffsetsUnitTest` 6/6 pass, including the guard `stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename`. FORK-DELTA row and Known cost 16 state the state key as `<transformed topic>-<partition>` and the `onPartitionRemoved` behaviour. Red-before-green recorded in E-02, gate in E-04. |
| 2 | RSK-02: expected changed rows written before the change; after it the actual diff equals the list exactly; a `RetriableException` passed to `handleException` comes out retriable | VERIFIED | `42-rsk02-expected-added.tsv` (38 rows) committed in `b34d0280`, before the code commit `c8574890` (E-05). E-07: status delta `removed=0 added=38 expected=38 match=yes` for both `testUnit` and the full suite; none of the 216 `WriterFailureClassificationUnitTest` executions, PIN-02 (14) or TMO-02 (15) rows changed. Live re-run: `RetriableExceptionPassThroughUnitTest` 38/0, `WriterFailureClassificationUnitTest` 216/0. `Utils.java` loop throws `(RetriableException) e` or `new RetriableException(e)` when any chain element is a `RetriableException`, placed after the server-code rule and before the `DataException` rule. |
| 3 | RSK-03: a non-transient code in `additionalRetriableErrorCodes` logs one WARN naming the code with no record data; a transient code logs none; start-up still succeeds; no validator or retry budget; `ClickHouseSinkConfig` diff empty; existing pins unchanged | VERIFIED | `ClickHouseSinkTask.warnOnUnvettedRetriableCodes` (called after `validateBufferConfig`, before `ProxySinkTask` construction) logs one WARN with the sorted codes and fixed text only. `UnvettedRetriableCodesWarnUnitTest` 21/0: unset and every vetted code gives 0 WARNs, unvetted shapes (`60`, `62, 60,159`, `2147483647`) give exactly one WARN with exact text, and each case asserts that `start` reaches the connection step (Docker-free). The live class `UnvettedRetriableCodesWarnLiveTest` proves start-up succeeds against a real server (recorded in E-10 full suite; not re-run here). `git diff 1c808c02 HEAD -- ClickHouseSinkConfig.java` empty; `AdditionalRetriableErrorCodesPinUnitTest` 19/0, unmodified. |
| 4 | RSK-04 plus phase shape: FORK-DELTA states OI-35-2 and OI-35-3 as limits (injective rename, partition-preserving SMT), state key unchanged; each of the three code commits touches only its production file, tests and record rows; each followed by cache-busted `testUnit` and a full suite | VERIFIED | Known cost 16 (FORK-DELTA) states both limits and the unchanged key. Commit file lists: `cba0909b` = Processing.java + 1 test + FORK-DELTA + anchors; `c8574890` = Utils.java + 1 test + FORK-DELTA + anchors; `4935e9c5` = ClickHouseSinkTask.java + 2 tests + FORK-DELTA + anchors. E-04, E-07, E-10 each record a cache-busted `testUnit` (Docker unreachable) and a full suite on ClickHouse `latest` x V2, 0 failures, status delta equal to the added rows. E-11 shows each commit reverts alone. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Stale-label notes

- ROADMAP criterion 2 and RSK-02 say "184-case matrix". The matrix runs 216 executions (since Phase 27); the row set of changed matrix rows is empty either way. Recorded in E-05, E-12 and not edited by this phase. Informational, not a gap.
- ROADMAP criterion 3 "start-up still succeeds" is shown by the live class; the Docker-free tests prove the WARN fires and that `start` reaches the connection step, as the orchestrator stated.

### Post-review fix (1c0d9d00, WR-01)

After the deep code review, `1c0d9d00` added an identity-set cycle guard to the `Utils` loop, plus `HandleExceptionCauseCycleUnitTest` (2 cases, 2/0 on re-run) and one FORK-DELTA sentence. The RSK-02 revert unit is therefore `c8574890` together with `1c0d9d00`; this is recorded in the RSK-02 FORK-DELTA row.

The E-12 invariants-script PASS (`anchor_rows=190`, run on `cca74819`) predates this fix. The fix changed no anchored line: the loop header `for (Throwable t = e; t != null; t = t.getCause())` is byte-identical, `fork-anchors.txt` was not edited by it, and the anchors mirror re-computed in this verification reads `rows=190 bad=0`. The 40-minute script was not re-run; since no anchored line moved, its verdict still applies, but a re-run remains the authoritative check before any push.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `Processing.java` | line 137 `originalTopic()` | VERIFIED | one-line diff, anchored |
| `Utils.java` | chain pass-through with cycle guard | VERIFIED | 14 added lines vs start, wired in `handleException` |
| `ClickHouseSinkTask.java` | start-up WARN | VERIFIED | called from `start()`, 21 added lines |
| `OriginalTopicInsertedOffsetsUnitTest` | 6 cases | VERIFIED | 6/0 |
| `RetriableExceptionPassThroughUnitTest` | 38 cases | VERIFIED | 38/0 |
| `UnvettedRetriableCodesWarnUnitTest` / `...LiveTest` | 21 + live | VERIFIED | 21/0; live recorded in E-10 |
| `HandleExceptionCauseCycleUnitTest` | 2 cases | VERIFIED | 2/0 |
| `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt` | rows, 6 new anchors | VERIFIED | 190 rows, 0 bad |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseSinkTask.start` | `warnOnUnvettedRetriableCodes` | call after `validateBufferConfig` | WIRED |
| `warnOnUnvettedRetriableCodes` | `Utils.DEFAULT_RETRIABLE_ERROR_CODES` | `removeAll` | WIRED |
| `Processing.doLogic` | `StateRecord.originalTopic` | `record.getSinkRecord().originalTopic()` | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite | `./gradlew testUnit --rerun` | BUILD SUCCESSFUL; XML totals 1824 tests, 0 failures, 0 errors, 2 skipped | PASS |
| Anchors mirror | `42-evidence-check.py anchors fork-anchors.txt --rows 190` | `rows=190 bad=0` | PASS |
| `src/main` file list vs `1c808c02` | `git diff --name-only 1c808c02 HEAD -- src/main` | exactly `ClickHouseSinkTask.java`, `Processing.java`, `Utils.java` | PASS |
| Untouched paths | `git diff` on `ClickHouseSinkConfig.java`, `sink/state`, `sink/kafka`, `ProcessingTest.java`, `VERSION`, `CHANGELOG.md` | all empty | PASS |
| No existing test modified | `git diff --diff-filter=MDR 1c808c02 HEAD -- src/test src/testFixtures src/integrationTest` | empty (only 5 added test files) | PASS |

Heavy gates (E-04, E-07, E-10 full suite per commit; post-fix full suite 2423 tests, 18 skips) were read from the evidence and the fix report, not re-run.

### Requirements Coverage

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| RSK-01 | 42-01, 42-04 | SATISFIED | truth 1 |
| RSK-02 | 42-02, 42-04 | SATISFIED | truth 2 (matrix label 184 vs 216 noted) |
| RSK-03 | 42-03, 42-04 | SATISFIED | truth 3 |
| RSK-04 | 42-01, 42-04 | SATISFIED | truth 4 |

No orphaned requirements: REQUIREMENTS.md maps exactly RSK-01..04 to Phase 42, and all four appear in plan frontmatter.

### Anti-Patterns Found

No TBD/FIXME/XXX debt markers in the three modified production files' added lines; no stubs. Open code-review Info findings (all `open` in `42-REVIEW-DISPOSITION.md`, advisory, non-blocking):

| Finding | Severity | Impact |
|---------|----------|--------|
| IN-01 pass-through branch logs nothing; server-error ERROR line contradicts the outcome when a `RetriableException` overrides it | Info | Diagnostics only |
| IN-02 vetted code set defined in two places | Info | Maintainability |
| IN-03 `UnvettedRetriableCodesWarnUnitTest` port race, blank-entry shapes missing | Info | Test hardening |
| IN-04 unused outcome and no `DataException` control in the pass-through test | Info | Test hardening |

### Human Verification Required

None.

### Gaps Summary

No gaps. All four ROADMAP success criteria hold in the codebase, each code commit is isolated, tests-first, and gated, and the post-review fix leaves the anchors and the Docker-free suite green. The WARN-text and chain behaviors are exercised by passing tests, so none is left behavior-unverified. The four open Info findings and the stale 184 label are carried as advisory items for later phases.

---

_Verified: 2026-11-02_
_Verifier: Claude (gsd-verifier)_
