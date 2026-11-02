---
phase: 41-bounded-insert-timeout
plan: "02"
subsystem: sink-db
tags: [insert-timeout, socket-timeout, client-v2, operator-override, fork-delta, fork-anchors, progress-headers, junit5, clickhouse-kafka-connect]

requires:
  - phase: 41-bounded-insert-timeout
    provides: 41-red-pins.patch (the red stub and 15-case pin class), the measured baseline (E-01), the red runs (E-02, E-03)
provides:
  - "Pin/seam commit 0a8e1e53: StubClickHouseServer (stalling, okAfter), InsertStallBoundPinUnitTest (15 cases) and the FORK-DELTA harness row, red by design, no src/main file"
  - "Fix commit 14952562: ClickHouseHelperClient.setInsertSocketTimeoutV2 with its operator skip, one shared jdbcConnectionProperties parser, and three one-line calls in ClickHouseWriter; a stalled client V2 async=false insert now ends after clickhouseClientInsertTimeoutMs as a retriable SocketTimeoutException with no DLQ record under both tolerances"
  - "InsertSocketTimeoutOptionUnitTest (11 Docker-free cases), FORK-DELTA per-insert timeout row and Known cost 14 rewritten, four TMO-01 anchor rows (mirror rows=184 bad=0)"
  - "Evidence E-04..E-07: the red re-run and pin commit, the JSON-site tracer, the fix commit with the progress-header probe, the operator-guard mutation"
affects: [41-03]

actuals:
  tokens: 20000
  tasks: 3
  commits: 5

plan_head_before: e26068786a5e4541132ebf05d089487f14b9c8b1
plan_head_after: 3bef5f94245fea483c9e33aaab6502bcc865f4ff
commits: 5

tech-stack:
  added: []
  patterns:
    - "Per-insert InsertSettings.setNetworkTimeout replaces the client-level socket_timeout, so it is skipped when the operator's jdbcConnectionProperties names the key (exact, case-sensitive); the flag is computed once per helper client from the same parser createClientV2 applies"
    - "Pin commit apart from the fix commit; the pair is the revert unit (git revert <fix> <pin>), no gate runs on the red intermediate"
    - "Scratch mutation of the guard expression to prove the operator cases discriminate, restored by git checkout of the one file"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md

key-decisions:
  - "The per-insert socket_timeout is set at all three V2 insert sites through one helper method placed beside setReplicaTagHeaderV2; src/main differs from phase start only in ClickHouseWriter.java (3 added lines, 0 removed) and ClickHouseHelperClient.java (two added imports); ClickHouseSinkConfig.java stays byte-identical and the CFG-02 anchor counts 1"
  - "An operator socket_timeout in jdbcConnectionProperties, 0 included, keeps its value: the helper leaves the per-insert option unset, and the null or blank connection-properties string is guarded explicitly in the once-computed flag"
  - "The progress-header probe outcome is recorded unmeasured with label inferred, not adjusted: a connector-shaped body insert sent no progress header until its end on ClickHouse 26.10.3.7, so the run did not exercise header streaming; Known cost 14 states what was measured and marks the header-streaming escape as inferred"

patterns-established:
  - "Probe rule applied as written: bounded needs headers streaming on the wire, not just a timed-out insert; a different observation is recorded as observed"

requirements-completed: [PIN-01, TMO-01, TMO-02, TMO-03]

coverage:
  - id: D1
    description: "The red tree is re-applied byte-identical (SHA-256 equals E-03), re-run red with E-03's failing set (15 tests, 9 timeout failures) and committed as the pin/seam commit with the stub's FORK-DELTA harness row, touching no src/main file"
    requirement: "PIN-01"
    verification:
      - kind: other
        ref: "python3 -I 41-evidence-check.py expect 41-COMMAND-EVIDENCE.md E-04 (10 checks, 0 failed); git diff --name-only 0a8e1e53^1 0a8e1e53 names the stub, the pin class and docs/FORK-DELTA.md"
        status: pass
    human_judgment: false
  - id: D2
    description: "With the helper called at the JSON site only, every route=JSON case turns green while the String and RowBinary cases of the two stall pins stay red by timeout and the parser guards stay green"
    requirement: "TMO-01"
    verification:
      - kind: other
        ref: "E-05 (9 checks, 0 failed); /tmp/gsd41/exec/xml-intermediate: 15 tests, 6 failures, no route=JSON failure, all AssertionFailedError 'execution timed out after'"
        status: pass
    human_judgment: false
  - id: D3
    description: "After the fix commit all three V2 sites carry the per-insert timeout: the stalled insert ends in about 0.3 s with a SocketTimeoutException root classified RetriableException, the stub sees one POST, and through ProxySinkTask.put no DLQ record is written under errors.tolerance none and all on JSON, String and RowBinary"
    requirement: "TMO-01, TMO-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause and #stalledV2InsertThroughPutIsRetriableWithNoDlqRecord (E-06 green run: 15 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D4
    description: "An operator socket_timeout keeps its value at all three sites and in the helper: a 6000 ms operator value outlasts a delayed answer, a 200 ms value ends a stall, the per-insert option is unset for the four named forms and set for the six unnamed forms, Long.MAX_VALUE is clamped, and removing the operator skip fails exactly the 6 + 4 operator cases"
    requirement: "TMO-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java (11 cases) and InsertStallBoundPinUnitTest#operatorSocketTimeout* (6 cases); E-06 green, E-07 mutation: 10 AssertionFailedError, restored"
        status: pass
    human_judgment: false
  - id: D5
    description: "Known cost 14 opens with 'Scope: client V2 only.' and lists the open limits (write-side stall, the three client V1 future.get() sites, unbounded non-insert requests, the quiet-insert behaviour change with the exactlyOnce=false duplicate risk) and the progress-header outcome as measured; the per-insert timeout row and four anchor rows land in the fix commit"
    requirement: "TMO-03"
    verification:
      - kind: other
        ref: "bash /tmp/gsd41/exec/v02-t2.sh prints P41-02-T2-OK (heading_and_scope_ok True, known_cost_14_consistent True, anchors rows=184 bad=0)"
        status: pass
    human_judgment: true
    rationale: "The wording of the progress-header sentence follows an observation that differs from the planning-time expectation; the user should read Known cost 14 and the unmeasured/inferred label before the rollout item (plan 41-03 E-09) repeats it"

duration: 11min
completed: 2026-11-02
status: complete
---

# Phase 41 Plan 02: Per-insert socket_timeout fix and pins Summary

**A stalled client V2 `async=false` insert on every route now ends after `clickhouseClientInsertTimeoutMs` as a retriable `SocketTimeoutException` with no DLQ record, through one `setInsertSocketTimeoutV2` helper that skips the operator's own `socket_timeout`; the pins and the fix are two separate commits forming one revert unit, and a real-server probe shows a connector-shaped insert sends no progress header, which Known cost 14 records as measured and the header-streaming escape as inferred**

## Performance

- **Duration:** 11 min
- **Started:** 2026-11-02T09:02:56Z
- **Completed:** 2026-11-02T09:15Z
- **Tasks:** 3
- **Files modified:** 8 (2 created, 6 modified; the pin commit holds the stub change, the new pin class and FORK-DELTA)

## Accomplishments

- Pin/seam commit `0a8e1e53` (E-04): the red tree re-applied from `41-red-pins.patch` (SHA-256 equal to E-03), re-run red with E-03's failing set (15 tests, 9 failures, all `execution timed out after`), committed with the FORK-DELTA harness row directly after the "Hand-written test doubles" row; no `src/main` file in it.
- Tracer (E-05): the helper at the JSON site only turned every `route=JSON` case green (15 tests, 6 failures left, all String and RowBinary stall cases), with the operator guards and the two parser guards green.
- Fix commit `14952562` (E-06): the other two sites, `InsertSocketTimeoutOptionUnitTest` (5 methods, 11 cases), the FORK-DELTA rows and four anchor rows. The green run is 15 + 11 new cases and 12 guard classes, 0 failures; all six `ProxySinkTask.put` cases are `RetriableException` with no DLQ record under both tolerances, each stalled insert ending in about 0.3 s against the 300 ms test timeout.
- Source scope held: `ClickHouseWriter.java` 3 added lines and 0 removed, `ClickHouseHelperClient.java` two added imports, `ClickHouseSinkConfig.java` byte-identical, CFG-02 anchor 1, no added line naming the asynchronous insert setting, the client asynchronous mode or a logger. Anchors mirror `rows=184 bad=0`.
- Progress-header probe (E-06, scratch ClickHouse 26.10.3.7, container removed): the 3000 ms per-insert value failed at 3030 ms (`DataTransferException > SocketTimeoutException`), the 8000 ms control succeeded at 4110 ms, and 4000 rows were committed, so the timed-out insert was still committed.
- Mutation (E-07): removing the operator skip fails exactly the two writer-level operator guards (6 cases) and `insertCarriesNoSocketTimeoutWhenTheOperatorNamedOne` (4 cases), all `AssertionFailedError`; the helper was restored and `git diff --quiet HEAD -- src` exits 0.

## Task Commits

1. **Task 1 (tracer): re-apply and re-confirm the red tree, pin/seam commit, JSON-site tracer (E-04, E-05)** - `0a8e1e53` (test, the pin/seam commit); evidence `30da4e90` (docs)
2. **Task 2: the other two sites, the helper's unit class, probe, FORK-DELTA and anchor rows, fix commit (E-06)** - `14952562` (fix); evidence `95d16b50` (docs)
3. **Task 3: operator-guard mutation, restore (E-07)** - `3bef5f94` (docs)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - `SOCKET_TIMEOUT_OPTION`, `operatorSetSocketTimeout` (computed once), `parseJdbcConnectionProperties` (shared with `createClientV2`), `operatorNamesSocketTimeout`, `setInsertSocketTimeoutV2`, two imports
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - one call line at each of `doInsertRawBinaryV2`, `doInsertJsonV2`, `doInsertStringV2`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java` - 11 Docker-free helper cases
- `src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java`, `.../db/helper/StubClickHouseServer.java` - the pin commit (15 pin cases; the public stub with `stalling()` and `okAfter(Duration)`)
- `docs/FORK-DELTA.md` - harness row (pin commit); per-insert timeout row and Known cost 14 (fix commit)
- `.planning/scripts/fork-anchors.txt` - one `# Phase 41` comment and four TMO-01 rows
- `.planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md` - E-04..E-07

## Decisions Made

- One helper method at all three sites, placed beside `setReplicaTagHeaderV2`; no config key, no change to `awaitInsertResponse`, `Processing` or `Utils`.
- The operator skip is by the exact key `socket_timeout` from the same parser `createClientV2` applies, so what the client treats as an option is what the flag sees; `socket_timeout=0` from the operator keeps inserts unbounded (assumption A1, pinned).
- The probe outcome is recorded `unmeasured`/`inferred` as the plan's rule requires, with the observed numbers; Known cost 14 states them.

## Deviations from Plan

None - plan executed exactly as written. One observation differs from the planning-time expectation and is recorded as observed (see Issues Encountered); one extra scratch probe was run to understand it.

## Issues Encountered

- **Progress-header expectation not reproduced (needs the user's eyes before plan 41-03 E-09).** The plan expected progress headers every 0.2 s on a 2000-row body insert. On ClickHouse 26.10.3.7 a body insert (the connector's shape) sent exactly one `X-ClickHouse-Progress` header, at the end of the insert, with and without `wait_end_of_query` and with `http_headers_progress_interval_ms=100` (raw-socket timings in E-06). The plan's rule for `bounded` needs headers on the wire about every 0.2 s, so the outcome is `unmeasured` with label `inferred`, not adjusted. The 0.2 s cadence appears only for an `INSERT ... SELECT`, and for that shape an extra scratch run through the client's query path with a 1000 ms `socket_timeout` finished OK after 2056 ms, so streamed progress headers do reset the client's read timer there (measured). Known cost 14 therefore says: a connector body insert was bounded (3030 ms, committed rows), and an insert shape or server version that streams progress headers during a body insert would escape the bound (inferred). Plan 41-03's rollout item (E-09) must use the same wording and the `inferred` label; the plan checks that expect "did not extend" apply only to the `bounded` branch.
- The planning-time statement that an `INSERT ... SELECT` with progress ended at the 1000 ms bound is contradicted by this run (2056 ms, OK); the planning note was a scratch run and is superseded by E-06.
- STATE.md is left modified for the orchestrator to commit (operator note); `.planning/config.json` and `.planning/state.json` were never staged.

## Known Stubs

None. The only stub-like artifact is the in-JVM stall responder, a deliberate test fixture.

## Threat Flags

None. No new network endpoint, auth path or schema in `src/main`; the loopback stub is test-only and the scratch ClickHouse container was removed. The helper and parser add no log statement (the verify rejects an added `LOGGER` line).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 41-03 gates the result: full `testUnit` and full suite on ClickHouse `latest` x client V2 should total baseline plus 26 (1745 and 2343), anchors mirror `rows=184 bad=0`, `origin/custom_build` still `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`; it records the F-2 rollout item (E-09) and must carry the progress-header wording above.
- The revert unit is the pin commit `0a8e1e53` plus the fix commit `14952562`, reverted together newest first.

## Self-Check: PASSED

- `41-02-SUMMARY.md` and the two new test classes exist; the five Task 2 files and the pin files are in commits `0a8e1e53` and `14952562`, both ancestors of HEAD.
- `bash /tmp/gsd41/exec/v02-t2.sh` printed `P41-02-T2-OK` and `bash /tmp/gsd41/exec/v02-t3.sh` printed `P41-02-T3-OK`; the Task 1 gate was checked piecewise at its intermediate state (E-04, E-05 `expect` OK).
- `git rev-list --count e2606878..HEAD` read 5 before this SUMMARY; `git ls-remote origin refs/heads/custom_build` equals `72e8ccff...`; nothing was pushed.

---
*Phase: 41-bounded-insert-timeout*
*Completed: 2026-11-02*
