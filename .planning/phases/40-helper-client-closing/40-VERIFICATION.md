---
phase: 40-helper-client-closing
verified: 2026-11-02T10:15:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/40-helper-client-closing/40-01-PLAN.md"
  - ".planning/phases/40-helper-client-closing/40-01-SUMMARY.md"
  - ".planning/phases/40-helper-client-closing/40-02-PLAN.md"
  - ".planning/phases/40-helper-client-closing/40-02-SUMMARY.md"
  - ".planning/phases/40-helper-client-closing/40-03-PLAN.md"
  - ".planning/phases/40-helper-client-closing/40-03-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java"
covered_digest: "v4:sha256:8cca6e90b2574f5f8ec89f280fdb307ac0809162d4f81fa82e5b5c2d7658aa5e"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 40: Helper Client Closing Verification Report

**Phase Goal:** A task that stops releases every ClickHouse helper client it created (writer `chc`, the V1 per-batch client in `getMutationRequest`, the `KeeperStateProvider` client in exactly-once mode). A failed `start()` and a second `stop()` are safe. The writer half and the Keeper half land as separate commits, each behind its own gate. The Keeper half leaves the state read and write paths byte-identical.
**Verified:** 2026-11-02
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A Mockito-free counting seam (`CountingHelperClient` plus `createHelperClient` factory) counts opens and closes at all three sites on V1 and V2, including a failed `start()` and a double `stop()`; each case was run red on the unchanged tree and recorded before any fix (CLS-04) | VERIFIED | Seam exists: `ClickHouseWriter.createHelperClient` (protected, `ClickHouseWriter.java`), Keeper factory constructor `KeeperStateProvider(csc, Function)`, `CountingHelperClient` (no Mockito reference in any of the four new or changed test files; grep clean). Cases: `WriterHelperClientClosingUnitTest` 23 tests, `KeeperStateProviderClosingUnitTest` 5, `ProxySinkTaskStopUnitTest` 4, all 0 failures in my own `testUnit` run. Red-first recorded in `40-COMMAND-EVIDENCE.md`: E-03 (writer class red: 22 tests, 19 failures, reason is the `close count` assertion; the two controls green), E-04 (Keeper and stop cases red: 4 of 7 fail on close count, tripwire green), and red trees saved as `40-red-writer.patch` and `40-red-keeper.patch`. Red runs precede the fix commits `de3faf77` and `b55f108a` in git history. |
| 2 | Writer half is one commit: `stop()` closes `chc` once after the table-sync executor stops; second `stop()` and `stop()` after failed `start()` neither throw nor double-close; every failure path of `start()` closes the opened client; V1 `getMutationRequest` closes its throwaway client per batch while the returned node still serves the insert; field keeps name `chc` (CLS-01, CLS-02) | VERIFIED | Code read at HEAD. `stop()` shuts down and awaits the executor (5 s), then calls `closeHelperClient()`, which snapshots `chc`, nulls it, then closes: a second stop or a stop after a failed start is a no-op. `start()` wraps `startInternal` and closes in `finally` when the result is not `true`, so every `false` return and every thrown exception (ping false, unreadable or old version, version lookup failure, no tables, mapping exception) is covered. `getMutationRequest` reads `server = chcTmp.getServer()` in `try` and releases `chcTmp` through `closeQuietly` in `finally`; the node is used after the close. Field is still `chc`. Tests: `successfulStartThenStopClosesTheHelperClientOnce`, `secondStopNeitherThrowsNorClosesAgain`, five failed-start cases, `stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated`, `clientV1BatchesCloseTheirPerBatchHelperClientAndTheNodeStillServesTheInsert`, `clientV2BatchBuildsNoPerBatchHelperClient`, all pass (V1 and V2 parameterized). Writer-half commit `de3faf77` touches only `ClickHouseWriter.java`, `CountingHelperClient.java`, `WriterHelperClientClosingUnitTest.java`, FORK-DELTA, anchors. Gate A: first full-suite attempt failed 3 `ClickHouseWriterTest` cases (E-07 attempt 1, kept); user-authorised test-only seed `fae1fcb1` (no assertion changed) then E-07 re-measured pass. After the review fix (`e7c4958d`, WR-01) the per-batch close is swallow-and-log through the shared `closeQuietly`, and a new case pins a throwing per-batch close still inserting. REVIEW-FIX records post-fix: `testUnit` 1719 tests and full suite 2317 tests, 0 failed, 18 skips. |
| 3 | Keeper half is its own later commit: `StateProvider` default no-op `close()`; `KeeperStateProvider.close()` closes a client it created and leaves an injected one open; `ProxySinkTask.stop()` stops the writer, unregisters the MBean and closes the provider so a throw in one step still runs the others; the diff of `getStateRecord` and `setStateRecord` is empty; `testUnit` and a full suite pass (CLS-03) | VERIFIED | `StateProvider.java`: `default void close() {}`. `KeeperStateProvider`: `ownedClient` assigned only in the factory constructor (the one the public constructor delegates to), `close()` nulls then closes once. The injected-client constructor never sets `ownedClient`, so it can never close an injected client (code-enforced; no instance of that constructor can be built today because `init()` NPEs on null `csc`, pinned as a tripwire and recorded in FORK-DELTA; see Notes). `ProxySinkTask.stop()` at HEAD runs three separately guarded steps (writer stop, MBean unregister, provider close), each catching `RuntimeException \| Error`; first failure is rethrown with its type and later ones attached via `addSuppressed`. So a throw in one step still runs the others (tests: `writerStopFailureStillUnregistersTheMBeanAndClosesTheStateProvider`, `stateProviderCloseFailureReachesTheCallerAfter...`, `writerStopFailureStaysTheThrownExceptionWhenTheStateProviderCloseAlsoFails`, `stopClosesTheStateProviderOnceAfterTheWriterAndTheMBean`; all pass). I extracted `getStateRecord` and `setStateRecord` from `52ce1cd4` and HEAD and diffed them: IDENTICAL. Keeper-half commit `b55f108a` touches `ProxySinkTask`, `StateProvider`, `KeeperStateProvider`, their tests, FORK-DELTA, anchors, and not `ClickHouseWriter.java`, so it reverts alone. Gate B (E-10) pass; full suite with exactly-once classes recorded. |
| 4 | FORK-DELTA rows and `fork-anchors.txt` rows land in the same commits as the code they describe and the anchors gate passes | VERIFIED | `de3faf77` carries the writer FORK-DELTA row, harness row, Known cost 13 and 4 anchor rows (rows=176); `b55f108a` carries the Keeper row, closes Known cost 13 and adds 4 anchor rows (rows=180). I re-ran `python3 -I 40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 180`: `rows=180 bad=0`. The WR-01 change renamed the `chcTmp` anchor row to `closeQuietly(chcTmp)` (count 1) and the `ClickHouseHelperClient chcTmp = new` zero-row still reads 0; both pass. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseWriter.java` | factory, `closeHelperClient`, start wrapper, stop close, per-batch close | VERIFIED | Substantive and wired (all three `new ClickHouseHelperClient...build()` sites go through `createHelperClient`). |
| `StateProvider.java` | default `close()` | VERIFIED | One line, implemented by `KeeperStateProvider`, called from `ProxySinkTask.stop()`. |
| `KeeperStateProvider.java` | `ownedClient`, `close()`, factory constructor | VERIFIED | Wired through `ProxySinkTask` (`stateProvider.close()`). |
| `ProxySinkTask.java` | guarded three-step `stop()` | VERIFIED | `ClickHouseSinkTask.stop()` calls `proxySinkTask.stop()` when non-null. |
| `CountingHelperClient.java`, 3 new test classes | Mockito-free seam and matrix | VERIFIED | 23 + 5 + 4 tests green. |
| `docs/FORK-DELTA.md`, `fork-anchors.txt` | rows with the code | VERIFIED | See truth 4. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseSinkTask.stop()` | `ProxySinkTask.stop()` | direct call, null-guarded | WIRED |
| `ProxySinkTask.stop()` | `ClickHouseWriter.stop()`, `SinkTaskStatistics.unregisterMBean()`, `StateProvider.close()` | three guarded steps | WIRED |
| `ClickHouseWriter.start()` | `closeHelperClient()` | `finally` when `startInternal` is not `true` | WIRED |
| `ClickHouseWriter.getMutationRequest` | `closeQuietly(chcTmp)` | `finally` after reading the node | WIRED |
| `KeeperStateProvider(csc)` | `ownedClient` | factory constructor assigns it | WIRED |

### Data-Flow Trace (Level 4)

Not applicable: lifecycle and resource-release change, no rendered data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite | `./gradlew testUnit --rerun` (no OpenJ9 crash, no core dumps left) | 1719 tests, 0 failures, 0 errors, 2 skipped; matches the expected count | PASS |
| New closing classes | read from `build/test-results/testUnit` | Writer 23/0 failures, Keeper 5/0, ProxySinkTaskStop 4/0 | PASS |
| State paths byte-identical | diff of `getStateRecord` and `setStateRecord` bodies, `52ce1cd4` vs HEAD | IDENTICAL | PASS |
| Anchors | `40-evidence-check.py anchors ... --rows 180` | `rows=180 bad=0` | PASS |
| `src/main` scope | `git diff --stat 52ce1cd4 HEAD -- src/main`; `VERSION`, `CHANGELOG.md` | Four files only; `VERSION` and `CHANGELOG.md` unchanged | PASS |

The full Docker suite (ClickHouse `latest` x V2) was not re-run by me; it relies on recorded evidence (E-07 re-measure, E-10, and REVIEW-FIX's 2317 tests, 0 failed, 18 skips post-fix).

### Probe Execution

Step 7c: SKIPPED. The plans declare no `probe-*.sh` scripts.

### Invariants gate versus the review fixes

The cumulative invariants gate (E-11, `result=PASS`, anchor_rows=180) ran on `6775cd93`, before the three review fixes (`e7c4958d`, `53c0f06b`, `1d5e943d`). I did not re-run the 40-minute script. I checked by reading that the fixes stay within what the anchors and FORK-DELTA describe:
- `e7c4958d` changes only `ClickHouseWriter.closeHelperClient` and `getMutationRequest` (try-with-resources replaced by `try/finally closeQuietly`), plus one anchors row (text changed, count 1 stays 1, row count stays 180) and the FORK-DELTA writer row. The anchors mirror at HEAD is `rows=180 bad=0`.
- `53c0f06b` changes only `ProxySinkTask.stop()` and its test and the FORK-DELTA Keeper clause. `KeeperStateProvider` is untouched by it, and the `stateProvider.close()` anchors row still holds.
- `1d5e943d` is test-only.
- No `src/main` file outside the four declared ones changed; no `VERSION` or `CHANGELOG.md` change.

The one residual gap in coverage: the script's per-commit scan across the three fix commits was not executed, so the cumulative gate itself is stale for those commits, while the single-commit anchors mirror is current.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CLS-01 | 40-02 | `stop()` closes `chc` after executor stops, idempotent, null-safe; closed on `start()` failure paths | SATISFIED | Truth 2 |
| CLS-02 | 40-02 | V1 `getMutationRequest` closes throwaway client every batch; node stays usable | SATISFIED | Truth 2 |
| CLS-03 | 40-03 | `StateProvider.close()` default, Keeper override closes only own client, `ProxySinkTask.stop()` guarded; state read/write unchanged | SATISFIED | Truth 3 |
| CLS-04 | 40-01, 40-02, 40-03 | Mockito-free counting seam pins all three sites on V1 and V2 | SATISFIED | Truth 1 |

All four IDs appear in PLAN frontmatter (40-01: CLS-04; 40-02: CLS-01, CLS-02, CLS-04; 40-03: CLS-03, CLS-04) and in REQUIREMENTS.md (marked complete). No orphaned Phase 40 requirements.

### Anti-Patterns Found

No `TBD`, `FIXME` or `XXX` markers introduced in the four changed `src/main` files (the diff adds none). No stubs; no empty implementations other than the intentional `default void close() {}`.

### Advisory (documented residuals, not gaps)

These are recorded in FORK-DELTA Known cost 13 with owner and re-open trigger, so they are not silent. They are reported for the maintainer's awareness.
1. A `KeeperStateProvider` constructor that throws on client V1 (KeeperMap table creation) leaks its client, because no `ProxySinkTask` exists to stop. This is a failed-construction path, not a stopping task.
2. `ClickHouseSinkTask.stop()` has no `try/finally` around `deliveryStrategy.stop()`; a throwing strategy would skip `proxySinkTask.stop()` and its closes. No strategy throws today.
3. The `stop()` 5 s executor wait can time out and then close `chc` anyway (user-accepted limit).
4. Review info items IN-01, IN-02 and IN-03 remain open. IN-01 matters to truth 3's wording: "leaves an injected one open" is code-enforced (`ownedClient` is only assigned in the factory constructor) but has no behavioural pin, since no instance can be built with an injected client. That is stated in FORK-DELTA and guarded by a tripwire.

### Human Verification Required

None.

### Gaps Summary

No gaps. All four ROADMAP success criteria and all four requirement IDs are satisfied in the codebase. The behavior-dependent truths (close once after executor, idempotent stop, failed-start close, per-batch close, finally-ordering) each have a passing named test in the Docker-free suite I re-ran. The Docker-backed full-suite and invariants-script results are carried from recorded evidence and are disclosed above. The invariants run predates the review fixes, which I checked by reading and by re-running the anchors mirror.

---

_Verified: 2026-11-02_
_Verifier: Claude (gsd-verifier)_
