---
phase: 41-bounded-insert-timeout
verified: 2026-11-02T11:45:00Z
status: passed
score: 8/8 must-haves verified
covered_files:
  - .planning/phases/41-bounded-insert-timeout/41-01-PLAN.md
  - .planning/phases/41-bounded-insert-timeout/41-01-SUMMARY.md
  - .planning/phases/41-bounded-insert-timeout/41-02-PLAN.md
  - .planning/phases/41-bounded-insert-timeout/41-02-SUMMARY.md
  - .planning/phases/41-bounded-insert-timeout/41-03-PLAN.md
  - .planning/phases/41-bounded-insert-timeout/41-03-SUMMARY.md
  - .planning/scripts/fork-anchors.txt
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
covered_digest: "v4:sha256:2b62b25915792e5e5a8da15bfec3fcedbeb13c0c8e5fb779eb8e4d273e6b31d1"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 41: Bounded Insert Timeout Verification Report

**Phase Goal:** With `async=false`, an insert into a server that stops answering fails after `clickhouseClientInsertTimeoutMs` as a retriable error, instead of blocking the task forever (F-2). An operator who sets `socket_timeout` in `jdbcConnectionProperties` keeps that value. A red pin proves the hang before the fix, and the cases that stay unbounded are written down. There is no new config key, and `async_insert` is not touched.
**Verified:** 2026-11-02T11:45:00Z
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1 / PIN-01: `StubClickHouseServer` has a stall responder; a pin wrapped in `assertTimeoutPreemptively` drives a V2 `async=false` insert against it; on the unfixed tree it was run and failed (not hung), and the run is recorded | VERIFIED | `StubClickHouseServer.stalling()` exists (pin commit `0a8e1e53` adds 90 lines to the stub, no `src/main` file). `InsertStallBoundPinUnitTest` uses `assertTimeoutPreemptively` on the writer-level pin (line 184) and the `put` pin (line 231). E-03 records the red run on the unfixed tree: 15 cases, 9 failed (3 writer-level + 6 `put`), every failure an `AssertionFailedError` containing `execution timed out after`, slowest 10.691 s, so none hung. The 6 operator guards were green on the unfixed tree, as designed. The red tree is saved as `41-red-pins.patch` (SHA-256 `10c738b2...`). Pin commit `0a8e1e53` touches no `src/main` file, so the red-first order is confirmed. |
| 2 | SC2 / TMO-01: a per-insert `socket_timeout` from `clickhouseClientInsertTimeoutMs` at the V2 insert sites makes the pin pass with a retriable error | VERIFIED | `ClickHouseWriter.java` calls `chc.setInsertSocketTimeoutV2(insertSettings, csc.getClickhouseClientInsertTimeoutMs())` at lines 1340, 1617 and 1766. These are the only three `new InsertSettings()` sites, one each in `doInsertRawBinaryV2`, `doInsertJsonV2` and `doInsertStringV2`. `setInsertSocketTimeoutV2` calls `settings.setNetworkTimeout(Math.min(timeoutMs, Integer.MAX_VALUE), MILLIS)`. Re-run by me: `./gradlew testUnit --rerun` gives `InsertStallBoundPinUnitTest` 15/0 failures, so the stalled insert ends inside the 10 s bound with a `SocketTimeoutException` root cause under `assertInstanceOf`. |
| 3 | SC2 / TMO-01 override: an operator `socket_timeout` in `jdbcConnectionProperties` keeps its value | VERIFIED | `operatorSetSocketTimeout` is computed once from `parseJdbcConnectionProperties`, the same parser `createClientV2` applies, and the per-insert option is skipped when it is true. The two operator guards in the pin class are green. `InsertSocketTimeoutOptionUnitTest` ran 23 cases with 0 failures (11 at plan time, plus 12 added by the review fixes). E-07 records a mutation: with the skip removed, exactly the operator cases fail, so the guards discriminate. |
| 4 | SC2 scope: `ClickHouseSinkConfig` unchanged, no new config key, `async_insert` untouched, CFG-02 anchor unchanged | VERIFIED | `git diff 69d5da3a HEAD -- ClickHouseSinkConfig.java` is 0 bytes. The CFG-02 line count is 1 (`grep -cF`). `src/main` differs from phase start only in `ClickHouseWriter.java` and `ClickHouseHelperClient.java`. The only added `src/main` lines that mention async are Javadoc prose (`async=false` and `async=true` in `awaitInsertResponse`), with no code change to that method. No added log line. `VERSION`, `CHANGELOG.md` and the published rollout notes are unchanged. |
| 5 | SC3 / TMO-02: through `ProxySinkTask.put`, the stalled insert is retriable under `errors.tolerance=none` and `all`, with no DLQ record | VERIFIED | `stalledV2InsertThroughPutIsRetriableWithNoDlqRecord` is parameterized over 3 routes and 2 tolerances. It uses a real `ProxySinkTask`, `assertThrows(RetriableException.class, ...)`, `assertEquals(0, dlq.size())`, a `SocketTimeoutException` root cause and one POST. It passed in my `testUnit` re-run (15/0 failures). It was retriable on the fixed tree under both tolerances, so the stop-for-user condition did not fire. |
| 6 | SC4 / TMO-03: FORK-DELTA records the write-side stall and the three V1 `future.get()` sites as unbounded limits, and the F-2 rollout item for Phase 44 is recorded | VERIFIED | `docs/FORK-DELTA.md` Known cost 14 opens with "Scope: client V2 only." and lists the limits: a write-side stall is not bounded (Probe 6), the three client V1 `future.get()` sites have no connector timeout, and non-insert requests are unbounded unless the operator sets `socket_timeout`. It also records the behaviour change for a quiet slow insert, the duplicate risk with `exactlyOnce=false`, and the remedies. E-09 holds 13 labelled claims for Phase 44 (scope first, `exactlyOnce=false` duplicate risk, override, limits). A harness row for `StubClickHouseServer` and a row for the per-insert timeout are in the table. |
| 7 | Progress-header claim: FORK-DELTA wording and the E-09 rollout item agree with E-06 | VERIFIED | E-06 records `progress_probe_outcome: unmeasured` and `progress_probe_label: inferred`. The facts are identical in all three places: a 3000 ms value failed at 3030 ms, an 8000 ms control succeeded at 4110 ms, 4000 rows committed, and an `INSERT ... SELECT` with a 1000 ms value finished at 2056 ms. All three label the consequence "inferred", with no upgrade to "measured". |
| 8 | Revert unit and anchors: pin commit `0a8e1e53` plus fix commit `14952562`; anchors mirror unchanged at HEAD | VERIFIED | `0a8e1e53` touches FORK-DELTA, the pin class and the stub. `14952562` touches the two `src/main` files, the option test, FORK-DELTA and `fork-anchors.txt`, with 5 added lines (a comment plus 4 rows). The anchors mirror re-run by me reads `rows=184 bad=0`. The 4 TMO-01 rows are `ClickHouseWriter` call 3, `settings.setNetworkTimeout(` 1, `operatorSetSocketTimeout` 3 and `clientBuilder.setSocketTimeout(` 0. |

**Score:** 8/8 truths verified (0 present, behavior-unverified). The behavior-dependent truths (2, 3 and 5) each have a named test that passed in my own `testUnit` run, so none rests on symbol presence alone.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseHelperClient.setInsertSocketTimeoutV2` | per-insert timeout setter with operator skip | VERIFIED | Substantive and wired: called from 3 writer sites. Early return when `settings == null`, the operator flag is set, or `timeoutMs <= 0` (the guard added by e49be8c0). |
| `ClickHouseWriter` call sites | 3 calls | VERIFIED | Lines 1340, 1617, 1766. |
| `StubClickHouseServer.stalling()` / `okAfter` | stall responder | VERIFIED | Used by the pin class. Per the E-03 record, the 4 existing stub users still pass. |
| `InsertStallBoundPinUnitTest` | 15 cases, no Mockito or `@Tag` or `@Disabled` | VERIFIED | The prohibited-pattern grep returns only `{displayName}` in the parameterized name template, not an `@DisplayName` annotation. 15/0 failures. |
| `InsertSocketTimeoutOptionUnitTest` | helper cases | VERIFIED | 23/0 failures. |
| `docs/FORK-DELTA.md` | rows plus Known cost 14 | VERIFIED | See truth 6. |
| `.planning/scripts/fork-anchors.txt` | 4 TMO-01 rows | VERIFIED | `rows=184 bad=0`. |
| `41-COMMAND-EVIDENCE.md` | E-01..E-10, all `pass` in the index | VERIFIED | Present and consistent with what I re-ran. |

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| Writer V2 insert sites | `setInsertSocketTimeoutV2` | WIRED | 3 call sites, pattern match. |
| `ClickHouseHelperClient` constructor | `parseJdbcConnectionProperties` | WIRED | The constructor computes `operatorSetSocketTimeout`, and `createClientV2` uses the same parser. |
| `ProxySinkTask.put` | `Utils.handleException` `SocketTimeoutException` clause | WIRED | Proven by the 6 `put` cases (retriable, no DLQ record). |
| `StubClickHouseServer.close` | held request of `stalling()` | WIRED | The latch is released before the stop. The suite runs with no hang. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Real Data | Status |
|----------|---------------|--------|-----------|--------|
| `setNetworkTimeout` argument | `csc.getClickhouseClientInsertTimeoutMs()` | `Long.parseLong(props.getOrDefault(...))` at `ClickHouseSinkConfig:387` | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite is green with the stall pins, the helper cases and the planning-ID guard | `DOCKER_HOST=unix:///nonexistent/docker.sock ./gradlew testUnit -PjavaVersion=21 --rerun` | 1757 tests, 0 failures, 0 errors, 2 skips; `InsertStallBoundPinUnitTest` 15/0, `InsertSocketTimeoutOptionUnitTest` 23/0, `SourcePlanningIdGuardUnitTest` 18/0; no core dump files | PASS |
| Anchors mirror | `41-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 184` | `rows=184 bad=0` | PASS |
| Config unchanged | `git diff 69d5da3a HEAD -- ClickHouseSinkConfig.java` | empty | PASS |
| Full suite on ClickHouse latest x V2 | not re-run, as instructed | Recorded by the executors: E-08 2343 tests / 0 failures / 18 skips on the fix tree; the review-fix record reads 2355 / 0 failures / 18 skips after the three fixes | Recorded, not re-run |

### Probe Execution

SKIPPED. The phase declares no `probe-*.sh` scripts. The progress-header probe is a one-off scratch program recorded in E-06.

### Invariants script note

The `verify-merge-invariants.sh` PASS in E-10 (`anchor_rows=184 result=PASS`, run at head `c03171b0`) predates the three review-fix commits (`e49be8c0`, `d6a09dcb`, `459354fc`). It is not invalidated: no anchored line changed, the mirror reads `rows=184 bad=0` at HEAD, and `fork-anchors.txt` is unchanged since `14952562`. E-10's own `post_review_recheck` rule says to re-run the script only if a fix changes an anchored line or the anchors file, and neither happened. I did not re-run it.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PIN-01 | 41-01, 41-02 | stall responder plus a red preemptive pin | SATISFIED | Truth 1 |
| TMO-01 | 41-02, 41-03 | per-insert `socket_timeout` at the V2 sites; operator wins; `async_insert` not touched | SATISFIED | Truths 2, 3, 4 |
| TMO-02 | 41-02, 41-03 | classification pinned through `put` under both tolerances | SATISFIED | Truth 5 |
| TMO-03 | 41-02, 41-03 | limits recorded | SATISFIED | Truths 6, 7 |

All four IDs appear in the plan frontmatter and are marked complete in REQUIREMENTS.md. Orphaned requirements: none, since REQUIREMENTS.md maps only these four to Phase 41.

### Prohibitions (judgment tier, non-authoritative)

| Statement | Assessment |
|-----------|------------|
| 41-01: no `src/main` change, no fix, no existing assertion changed | Held. The pin commit has no `src/main` file, and E-03 records `src` equal to HEAD after plan 41-01. |
| 41-01: no Mockito, `@Tag`, `ClickHouseBase`, `@Disabled`, assumption or `@DisplayName` in the new test | Held. The grep finds only the `{displayName}` name template. |
| 41-02: touch only the writer and helper in `src/main`; no config key; no `Processing`/`Utils` change | Held. `awaitInsertResponse` code is unchanged, but its Javadoc was edited later by review fix `459354fc` (see Warnings). |
| 41-02: not the client-level timeout, client async mode or server async insert | Held. The anchor row `clientBuilder.setSocketTimeout(` is 0. |
| 41-02 / 41-03: no `VERSION`, `CHANGELOG`, published-note or push change | Held. `origin/custom_build` is still `72e8ccff`, as at the gate. |

No unresolved judgment item is flagged beyond the notes below.

### Anti-Patterns Found

None. There are no `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` markers in the added `src` lines. `SourcePlanningIdGuardUnitTest` passes, so the new Javadoc carries no planning ID.

### Human Verification Required

None.

### Warnings (non-blocking, recorded)

1. **Progress-header bound is inferred, not measured.** On a connector body insert (ClickHouse 26.10.3.7), no progress header streamed while the server worked, so the per-insert timeout bounded it. For an `INSERT ... SELECT`, streamed headers reset the read timer, so a server version or insert shape that streams progress headers during a body insert could escape the bound. FORK-DELTA, E-06 and E-09 all say this honestly and carry it to Phase 44 ("derived (mechanism, not measured)" or measure it first). The behavior is a documented limit, not a must-have truth, so it does not block.
2. **`41-REVIEW-DISPOSITION.md` is stale for WR-01.** It lists WR-01 as `open`, but commit `459354fc` fixed it and `41-REVIEW-FIX.md` reports 3 of 3 fixed. This is bookkeeping only; re-run the disposition gate to refresh it.
3. **Stale `clickhouseClientInsertTimeoutMs` help text in `ClickHouseSinkConfig`.** The text still reads as a hard cap on a single insert response. Known cost 14 records this as a residual, because `ClickHouseSinkConfig` is held unchanged this phase. A later phase can edit it.
4. **Review Info items IN-01..IN-03** are open and out of scope.
5. **Heavy gates not re-run.** I took the full-suite totals (2343 at the fix tree, 2355 post-review-fix on ClickHouse latest x V2) from the executor and fixer records. My own Docker-free `testUnit` run (1757 / 0 failures / 2 skips) matches the fixer's recorded count exactly.

### Gaps Summary

No gaps. All four ROADMAP success criteria and all four plan-frontmatter requirements hold in the codebase. The revert unit is `0a8e1e53` plus `14952562`.

---

_Verified: 2026-11-02T11:45:00Z_
_Verifier: Claude (gsd-verifier)_
