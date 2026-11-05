---
phase: 46-insert-timeout-limits
plan: "04"
subsystem: docs
tags: [live-measurement, clickhouse-latest, progress-headers, large-body, wire, non-insert-requests, fork-delta, rollout, evidence]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: plan 46-02's V2 write-phase deadline and plan 46-03's V1 bounded wait (src/main unchanged in this plan), the raw-socket stub, B0 3d4ce2e8, testUnit 2152 and anchors rows=212
provides:
  - evidence E-10 (live TMO-02 re-measure on ClickHouse 26.10.3.7, base and fixed tree, 64 MiB body, raw wire, rows left by timed-out inserts)
  - evidence E-11 (non-insert requests and ping against a server that never answers, client V2 and V1)
  - evidence E-12 (FORK-DELTA records the phase)
  - docs/FORK-DELTA.md: one Connector-source row, the StubClickHouseServer row extended, Known costs item 14 rewritten
affects: [46-05]

actuals:
  tokens: 8409
  tasks: 3
  commits: 4
plan_head_before: cc3c9b55dfdcfc52ab779f6bcdf8578f436b7e7f
plan_head_after: b844c366482adcab2583c2b4aca42fdd9a942335

tech-stack:
  added: []
  patterns: [throwaway probe in a removed detached worktree, one probe file per tree with the tree read from the working directory, derived flags recomputed by the verify from the recorded times]

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md

key-decisions:
  - "The client V1 body write is recorded as not reached up to 256 MiB, never as bounded: E-07 and E-09 show the SinkTask thread in the response wait at 16, 64 and 256 MiB, so the V1 bound is claimed for the response wait only"
  - "The cut of a progress-streaming V2 send rests on the stub pin, not on a live measurement: the live 64 MiB body left the client in 2305 ms, inside the 3000 ms bound, so the real-server run never had a body still being sent at the bound"
  - "The non-insert probe builds both helper clients with retry 1, as the research probe did; the connector default of 3 multiplies the V1 ping and the V1 Keeper write (read from the code, not measured) and the item says so"

requirements-completed: [TMO-02, TMO-03]

coverage:
  - id: D1
    description: "Live re-measure on ClickHouse 26.10.3.7 through the real writer on B0 and on the fixed tree: the small-body V2 progress insert ran past a 3000 ms bound on both trees (8315 and 8299 ms, control 8059), the default-async_insert V2 insert failed retriably at 3050 ms, the V1 progress insert ran 8141 ms on B0 and failed retriably at 3034 ms on the fixed tree; each range asserted numerically"
    requirement: TMO-02
    verification:
      - kind: other
        ref: "P46-04-T1-OK (E-10 ranges and derived flags)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Plan-check B2b: the 64 MiB V2 body behind the slow view ended ok on both trees (8324 and 8380 ms, so the deadline did not cut it); the raw wire showed the first progress header at 64 ms, before the body was fully sent, but the whole body left the client in 2305 ms, inside the bound"
    requirement: TMO-02
    verification:
      - kind: other
        ref: "P46-04-T1-OK (v2_large_body_cut_by_deadline no, wire_progress_while_writing yes, both recomputed from the times)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Rollout numbers: after a timed-out V1 insert into a MergeTree table behind the slow view 2500 rows were visible at once and 4000 15 s later (the B0 control landed 4000), a Memory table showed 0 then 4000, and the default-async_insert V2 insert showed 4000 and 4000"
    requirement: TMO-02
    verification:
      - kind: other
        ref: "P46-04-T1-OK (rows 4000, 0 <= 2500 <= 4000 <= 4000)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Non-insert requests: against a server that never answers every client V2 request but the Keeper read was still blocked at the 20 s cap (the V2 ping at 20005 ms with a 2000 ms timeout), the Keeper read ended at 30034 ms on both helper clients, every other client V1 request at 30034 to 30045 ms and the V1 ping at 2082 ms"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-04-T2-OK (E-11 ranges)"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA records every case: one new Connector-source row, the StubClickHouseServer row extended in place, Known costs item 14 with the bounded cases, the V1 pipe write as not reached up to 256 MiB, three cases not bounded by decision with E-10 and E-11 numbers, the corrected ping statement and the rollout consequence; only docs/FORK-DELTA.md changed in the docs commit, testUnit 2152 green"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-04-T3-OK (row deltas 1 and 0, citations, seven measured numbers verbatim, removed clauses absent)"
        status: pass
      - kind: other
        ref: "E-12 testunit_tests 2152, failures 0, errors 0, skipped 2"
        status: pass
    human_judgment: false
  - id: D6
    description: "The wording of the recorded limits is a judgement: that each claim stays inside its measurement and that the rollout paragraph reads as the operator needs it"
    requirement: TMO-03
    verification: []
    human_judgment: true
    rationale: "No test asserts prose; the plan routes this to the orchestrator's independent gsd-code-reviewer pass on FORK-DELTA after the phase (T-46-14)"
---

# Phase 46 Plan 04: Live Measurements and FORK-DELTA Records Summary

**The insert-timeout phase is now measured against a real ClickHouse 26.10.3.7 and written into FORK-DELTA: the V2 progress-header insert escapes a 3000 ms bound at 8299 ms only with `async_insert=0` and many blocks, the client V1 bound turns the 8141 ms progress insert into a retriable failure at 3034 ms, the 64 MiB body never reached the write-phase deadline live, and the non-insert requests and the V2 ping timeout are recorded as not bounded by decision with their 20 s and 30 s numbers.**

## Performance

- **Duration:** about 25 min by the session clock (10:59 to 11:12 UTC for the work, plus the review of the plan and the tail gate)
- **Tasks:** 3
- **Files:** 2 changed (`docs/FORK-DELTA.md` and the evidence file); no source, build, CHANGELOG or VERSION file changed

## Names later plans use

- Commits: `6d4f5081` (E-10), `ea65cdc8` (E-11), `d4977444` (FORK-DELTA, E-12 `fork_delta_commit`), `b844c366` (E-12).
- ClickHouse `latest`: 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (unchanged since 46-01).
- Counts: testUnit **2152** (unchanged from E-09), anchors **212** rows (unchanged), `connector_rows_delta` 1, `harness_rows_delta` 0, item 14 uses "not bounded by decision" 4 times.

## Accomplishments

- **Live TMO-02 (E-10).** Through the real `ClickHouseWriter` on B0 and on the fixed tree: the small-body V2 progress insert (4000 rows behind a `sleepEachRow(0.002)` view, `async_insert=0`, 500-row blocks) ran 8315 ms on B0 and 8299 ms fixed against a 3000 ms bound (control 8059 ms), so the write-phase deadline leaves a response phase alone; under the server's default `async_insert=1` the same insert failed at 3060 ms on B0 and 3050 ms fixed, a `DataTransferException` over a `SocketTimeoutException`, retriable by the connector's classification; the V1 progress insert ran 8141 ms on B0 and ended at 3034 ms as a `RetriableException` over a `TimeoutException` fixed.
- **Large body and wire (B2b).** The 64 MiB V2 body (4000 rows of 16 KiB behind the slow view) ended `ok` at 8324 ms on B0 and 8380 ms fixed: the write-phase deadline never fired. The raw wire shows why: the server sent its first progress header 64 ms after the first byte (before the body was fully sent), but the client handed the whole 65610890-byte body to the socket in 2305 ms, inside the bound, over the colima port forward. The live run therefore does not exercise the progress-while-sending shape; that shape rests on `progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound`.
- **Rows left by timed-out inserts (rollout).** V1 into MergeTree: 2500 rows at once, 4000 after 15 s (B0 control 4000). V1 into Memory: 0, then 4000. Default-`async_insert` V2 into MergeTree: 4000 and 4000. A timed-out insert is not rolled back.
- **Non-insert requests (E-11, U-4, U-5).** Against `StubClickHouseServer.stalling()`: every client V2 request except the Keeper read was still blocked at the 20 s cap (20004 to 20005 ms), the V2 ping included, with its 2000 ms timeout; the Keeper read, always a client V1 read, ended at 30034 ms on the V2 helper client and the V1 helper client; client V1 version, table list, `DESCRIBE`, `ALTER` and the Keeper write ended at 30034 to 30045 ms; the V1 ping at 2082 ms.
- **FORK-DELTA (E-12).** One Connector-source row for the V2 write-phase deadline and the V1 bounded wait; the `StubClickHouseServer` row extended in place; Known costs item 14 rewritten with the bounded cases, the three cases not bounded by decision, the corrected ping statement and the rollout consequence. The help-text sentence of item 14 is carried over verbatim for plan 46-05.

## Findings the records carry (as the user asked)

- **V1 body write: not reached up to 256 MiB, not "bounded".** Carried from plan 46-03: at 16, 64 and 256 MiB the SinkTask thread was in the response wait and never in the pipe write, so the bound was not shown to end a stall inside the body write. Item 14 says "not reached", gives the pipe capacity (4194304 bytes, read from the code), the 30000 ms write timeout (read from the code) and the 1530 ms fixed-tree time of the 256 MiB insert, and claims neither "bounded" nor impossibility.
- **V2 rollout consequence from plan 46-02.** A V2 insert whose body (serialization included) is still being sent at `clickhouseClientInsertTimeoutMs` now fails and is retried even when the upload progresses and even when the server streams progress headers; the server may keep the rows it received, so with `exactlyOnce=false` a retry can duplicate. Item 14's rollout paragraph says this, with the V1 consequence (any insert still waiting for its response at the bound, including a progress-streaming one) and the rows E-10 counted.

## Rollout paragraph for Phase 49 (the gg8 operator note)

A bound added in Phase 46 fails an insert that legitimately runs longer than `clickhouseClientInsertTimeoutMs`. On client V2, a batch whose body is still being sent at the bound (serialization counted) fails and is redelivered, even while the upload progresses and even while the server already streams progress headers; a quiet response already failed at the bound before. On client V1, the default client, any insert still waiting for its response at the bound fails, including one whose server keeps sending progress headers, which used to run to its end. A timed-out insert is not rolled back: a client V1 timeout into a MergeTree table behind a slow view left 2500 of 4000 rows at once and 4000 after 15 s, so with `exactlyOnce=false` the redelivery duplicates what landed (with `exactlyOnce=true` the retry reuses the deduplication token, read from the code). Remedies: a larger `clickhouseClientInsertTimeoutMs`, or an operator `socket_timeout` in `jdbcConnectionProperties`, which removes both connector bounds. Not bounded by decision: a V2 insert whose body has been sent and whose server streams progress headers (8299 ms against a 3000 ms bound, only with `async_insert=0` and many blocks), the requests that are not inserts, and the V2 ping timeout.

## Task Commits

1. **Task 1 (tracer): live re-measure** - `6d4f5081` (E-10)
2. **Task 2: non-insert and ping inventory** - `ea65cdc8` (E-11)
3. **Task 3: FORK-DELTA** - `d4977444` (docs), `b844c366` (E-12)

**Plan metadata:** this SUMMARY is committed alone; STATE.md, ROADMAP.md and REQUIREMENTS.md are left to the orchestrator, per the dispatch instructions.

`actuals.commits: 4` is the measured `git rev-list --count plan_head_before..plan_head_after`; `actuals.tokens` is chars/4 over the added lines of `docs/` and the evidence file.

## Files Created/Modified

- `docs/FORK-DELTA.md` - the new Connector-source row, the extended `StubClickHouseServer` row, Known costs item 14
- `.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` - E-10..E-12

## Decisions Made

See `key-decisions` above. Nothing belonging to the user came up: U-1, U-4, U-5 and U-6 were applied as recorded in CONTEXT.md.

## Deviations from Plan

None - plan executed exactly as written.

### Observations (not rule triggers)

- **Probe parameters the plan left open.** Task 2 does not name a retry count; the probe used `setRetry(1)`, as the research probe did and as the plan's ranges (V1 ping 1.5 to 6 s, V1 requests 25 to 40 s) imply. The connector default is 3, and `pingV1` and `queryV1` loop over it (read from the code, not measured); the E-11 prose and item 14 say so.
- **A first Task 2 run was a probe error, not a finding.** The probe gave the V2 helper client's Keeper read the 20 s V2 cap, so it recorded 20014 ms still running, although that request always runs through a client V1 read and the plan expects 25 to 40 s. The cap was changed to the V1 cap for the Keeper read on both helper clients and the probe re-run once; E-11 records both runs and its numbers come from the second.
- **The row-count instrument.** The plan names `countRows`, which issues `OPTIMIZE TABLE` before its `SELECT count()`, so the "at once" figures are to within that call; E-10 and item 14 say so.
- **The live large body did not reach the deadline.** `v2_large_body_cut_by_deadline` is `no` (a range the plan allows, "recorded as found"); the record states that the shape rests on the stub pin.
- The probes ran with a scratch `maxHeapSize = "3g"` edit of `build.gradle.kts` in the removed worktrees only (the 64 MiB body held as 4000 records); no probe was staged or committed (`git log --all` prints nothing for either probe name), and the worktrees were removed.
- The E-10 timestamp line reads a few minutes later than the wall clock at the time it was written (11:05 against about 11:03); the evidence was not edited, per the rule.
- Standing tail gate on the final plan tip `b844c366`: `invariants: provenance_paths=11 anchor_commits=71 anchor_rows=212 marker_commits=89 anchor_exempt=0 result=PASS`, exit 0, unedited script, explicit range end (the plan does not schedule this run; it is recorded here, not in the evidence file).

## Issues Encountered

None. No JVM crash occurred and no crash file was left in the repository root.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. No source file changed.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema was added; the plan changed documentation and evidence only, and the probes inserted generated rows into a throwaway test container database (T-46-16).

## Next Phase Readiness

- Ready for plan 46-05 (the help text and its pin, the phase gate): item 14 still carries the old help-text sentence verbatim for 46-05 to rewrite together with `ClickHouseSinkConfig`; the measured bounds and limits the help text must name are in item 14: what the setting bounds on V2 and V1, what it does not bound (the V2 progress insert after its body is sent, the non-insert requests, the V2 ping), and that a timed-out insert may be committed.
- Expected counts: testUnit starts at 2152; the anchors mirror reads `rows=212 bad=0` until plan 46-05 adds its two rows (214).
- Phase 49 carries the rollout paragraph above into the gg8 operator note.

## Self-Check: PASSED

- Files: `docs/FORK-DELTA.md` and `46-COMMAND-EVIDENCE.md` present; this SUMMARY present.
- Commits `6d4f5081`, `ea65cdc8`, `d4977444` and `b844c366` are ancestors of HEAD.
- Verify blocks P46-04-T1-OK, P46-04-T2-OK and P46-04-T3-OK printed on the committed tip; anchors mirror `rows=212 bad=0`; no worktree is registered; no probe was committed.
