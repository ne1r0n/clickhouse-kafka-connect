---
phase: "46"
slug: insert-timeout-limits
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-05"
---

# Phase 46 — Security

> Threat register verified by gsd-security-auditor against HEAD `54c814fb` (25 rows: T-46-01 to T-46-20 plus five per-plan T-46-SC rows; plans 46-01 to 46-05 each carried a `<threat_model>`). Plan 46-06 had no PLAN.md (the dispatch prompt was the plan); its changes fall under T-46-04, T-46-05, T-46-06, T-46-08 and T-46-17. Live re-checks in this audit: the five phase pin classes ran 95 of 95 green (`InsertWriteStallBoundPinUnitTest` 20, `InsertV1WaitBoundPinUnitTest` 21, `InsertTimeoutHelpTextPinUnitTest` 27, `InsertBoundGateUnitTest` 20, `StubClickHouseServerRawModeUnitTest` 7), `SourcePlanningIdGuardUnitTest` 18 of 18, the unedited standing tail gate on HEAD read `anchor_commits=87 anchor_rows=216 result=PASS`, the evidence helper read `secrets` 0 token shapes, `index` 19 rows pass and `anchors` `rows=216 bad=0`, and the helper's md5 equals Phase 45's copy.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| ClickHouse server (or a network black hole) -> SinkTask thread | a peer that stops reading or answering can hold the task thread past `max.poll.interval.ms` (V2 write phase, V1 `future.get()`) | request body, response headers |
| watchdog thread -> client V2 transport | the watchdog closes another thread's in-flight request by query id | query id (random UUID) |
| timeout exception -> Processing wrap -> `Utils.handleException` -> Connect | the shape decides retry, DLQ or task failure | exception message and cause chain (record values must never cross) |
| operator `jdbcConnectionProperties` -> connector bounds | an operator-named `socket_timeout` keeps ownership of the transport limits | one config key |
| help text and FORK-DELTA -> operators, Phase 49 note | an operator sizes the setting from the wording | documentation claims |
| loopback stub, probes, scratch worktrees -> repository | raw stub sockets, A1/live probes, mutation worktrees | test-only data, generated rows |
| local repository -> origin | must not be crossed | refs |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-46-01 | Denial of service | raw stub threads, blocked test clients | medium | mitigate | `startRawThread` sets daemon (`StubClickHouseServer.java:374-375`); `closeRaw()` closes the server socket and every accepted socket with SO_LINGER 0 and interrupts the threads (`:345-358`); premise pins assert the blocked writer ends with an `IOException` within `RELEASE_BOUND` 5 s; pins wrap inserts in `assertTimeoutPreemptively`; 7 of 7 green live | closed |
| T-46-02 | Repudiation | vacuous write-stall pin (A1) | high | mitigate | `RAW_RECEIVE_BUFFER_BYTES = 4096` set before bind (`:58`, `:155`); premise pin asserts the 64 MiB write is still blocked with `bodyBytesRead() == 0`; E-03 margin 53.9 (macOS 662096 and Linux 1245184 absorbed bytes); the V2 pins assert the deadline-specific `RetriableException` shape via `assertDeadlineFailure` | closed |
| T-46-03 | Repudiation / Tampering | evidence E-01..E-19 and helper | medium | mitigate | helper md5 `7123d354...` equals Phase 45's copy; `index` 19 rows pass; `rows` E-14 and E-19 pass; E-19 counts re-read from the gate XML snapshots (2181 and 2784 tests, 0 failures); `src/main` change set is 3 files | closed |
| T-46-04 | Denial of service | V2 write-side stall | high | mitigate | `insertWriteDeadline` (`ClickHouseWriter.java:2029-2035`) arms before `client.insert` (`:2065`) and fires `client::cancelTransportRequest(queryId)`; wired at all three V2 sites (`:1494`, `:1793`, `:1943`); `writeStalledV2InsertEndsWithinTheBoundAsRetriable` over JSON, STRING and ROW_BINARY plus the `put` variants for both tolerances green live; E-04 red, E-05 green, mutation D2 turned 14 cases red | closed |
| T-46-05 | Denial of service | watchdog executor | medium | mitigate | one thread, daemon, `setRemoveOnCancelPolicy(true)` (`:2206-2214`); created lazily under `insertWatchdogLock` (`:2020-2027`); `shutdownNow()` in `stop()` (`:214-219`); `insertWatchdogIsADaemonThreadEndedByStop` green live; E-06 queue 0 after 100 inserts, terminated after `stop()` | closed |
| T-46-06 | Tampering | stale fire cancelling a later insert | medium | mitigate | query id is `UUID.randomUUID()` per `doInsert` (`Processing.java:64`, `:88`), so a fire can only name its own insert; `disarm()` in `insertWithWriteDeadline`'s `finally` (`:2072`) and in `onOutput` (`:2053`); `secondInsertAfterTheFirstInsertsBoundHasElapsedSucceeds` green (it pins a later insert succeeding, not a late fire; the bound on the id is by construction) | closed |
| T-46-07 | Tampering (data duplication) | timed-out V2 insert partly committed, redelivered | medium | accept | consequence stated in the help text (`ClickHouseSinkConfig.java` "with exactlyOnce=false the retry can duplicate rows") and FORK-DELTA item 14; `Processing.java`, `state/` and `Utils.java` unchanged over the phase; dedup token set at all three V2 sites (`:1482`, `:1782`, `:1930`); AR-46-01 | closed (accepted) |
| T-46-08 | Information disclosure | V2 `RetriableException` message | low | mitigate | message is a fixed format of client version, setting name, bound and query id (`:2195-2196`; `:1988-1989`); `assertNoSentinel` scans every message in the chain and the full stack trace on all three routes; sentinel `INSERT-BOUND-SENTINEL-46` appears in no `src/main` line and no evidence line; `SourcePlanningIdGuardUnitTest` green; no planning ids in added `src/main` lines | closed |
| T-46-09 | Denial of service | V1 write stall, quiet stall, progress insert at the three `future.get()` sites | high | mitigate | `awaitInsertResponseV1` (`:2006-2018`) bounds the wait and is the only V1 wait (`:1620`, `:1751`, `:1906`; the bare `future.get()` at `:2010` is the `bound <= 0` operator branch); 21 of 21 green live; E-07 red, E-08 green, mutation V1a turned 18 cases red | closed |
| T-46-10 | Denial of service | V1 body write blocked on the full pipe | medium | accept | not bounded by decision, measured: E-07 and E-09 (task thread in the response wait at 16, 64 and 256 MiB, never in the pipe write); help text and FORK-DELTA item 14 say it is not bounded; AR-46-02 | closed (accepted) |
| T-46-11 | Denial of service | worker or connection leak after a V1 timeout | medium | mitigate | no thread added; try-with-resources closes the piped stream on the exception path at all three V1 sites (`:1612`, `:1743`, `:1898`); `timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite` green live | closed |
| T-46-12 | Tampering (data duplication) | timed-out V1 insert partly committed, redelivered | medium | accept | same documentation and unchanged state machine as T-46-07; E-10 rows (2500 at once, 4000 after 15 s) recorded; AR-46-01 | closed (accepted) |
| T-46-13 | Information disclosure | V1 `RetriableException` message and probe output | low | mitigate | fixed `V1_INSERT_TIMEOUT_FORMAT` (`:63-64`, `:2016`); `assertNoSentinel` in the V1 pins (`InsertV1WaitBoundPinUnitTest.java:158`, `:254`) across messages and trace; probe prints library text only (E-09) | closed |
| T-46-14 | Repudiation | FORK-DELTA claims about bounds and limits | high | mitigate | item 14 (`docs/FORK-DELTA.md:244`) cites E-03..E-11 and E-15..E-17 with measured values; "not bounded by decision" wording (U-6); the earlier false ping claim is corrected (V2 ping 20005 ms against 2000 ms); orchestrator's `gsd-code-reviewer` pass ran (`46-REVIEW.md`, five warnings, four fixed in 46-06, WR-05 open and non-security) | closed |
| T-46-15 | Denial of service (operator surprise) | rollout consequence of the new bounds | medium | mitigate | item 14 "Rollout consequence" paragraph and the help text sentence "An insert that legitimately runs longer than a limit above fails and is retried" (pin phrase H10), with measured rows | closed |
| T-46-16 | Information disclosure / Tampering | probes and live data | low | mitigate | `git worktree list` shows only the main tree; no probe, scratch or `/tmp` path in the commit set `b2b96020^..HEAD`; outside `src`, `docs/FORK-DELTA.md`, `fork-anchors.txt` and this phase directory only `.planning/ROADMAP.md` changed; evidence `secrets` 0 token shapes | closed |
| T-46-17 | Repudiation | help-text claims | high | mitigate | `InsertTimeoutHelpTextPinUnitTest` 27 of 27 green live (required phrases H0..H17, kept phrases, five retired claims); 13 cases were red on the pre-46-06 text (E-17); two anchor rows keep a sync from restoring the old claim | closed |
| T-46-18 | Tampering | `ClickHouseSinkConfig` beyond the text | high | mitigate | diff of the file over the phase has only string-literal lines; `.define(` count 48 at B0 and at HEAD; `async_insert` lines 2 at B0 and at HEAD; the pin asserts type, default, validator, importance, display name and width | closed |
| T-46-19 | Repudiation | phase gate results | medium | mitigate | E-14 and E-19 rows pass; gate XML snapshots re-read (`testUnit` 2181 / 0 / 0 / 2 skipped; suite 2784 / 0 / 0 / 18 skipped, ClickHouse 26.10.3.7); tail gate re-run live on HEAD `result=PASS`; index reads pass for 19 entries | closed |
| T-46-20 | Tampering | origin branches and tags | high | mitigate | `git ls-remote origin` returns nothing for tag `pre-v1.7.0_gg8` and for `sync/v1.7.0`; `git branch -r --contains HEAD` is empty | closed |
| T-46-SC (01) | Tampering | container images | low | accept | no image pulled; no build, workflow, VERSION or CHANGELOG change over the phase | closed (accepted) |
| T-46-SC (02) | Tampering | package installs | low | accept | no dependency change (build files unchanged) | closed (accepted) |
| T-46-SC (03) | Tampering | sources jars | low | accept | no jar in the change set | closed (accepted) |
| T-46-SC (04) | Tampering | container images | low | accept | no image or dependency change | closed (accepted) |
| T-46-SC (05) | Tampering | package installs | low | accept | build files unchanged since B0 | closed (accepted) |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-46-01 | T-46-07, T-46-12 | A timed-out insert is not rolled back and the server may keep part or all of it; the redelivery duplicates rows with `exactlyOnce=false`, and with `exactlyOnce=true` reuses `insert_deduplication_token`. Stated in the help text and FORK-DELTA item 14. This is a recorded limit of bounding an insert, not a defect. | plan disposition (46-02, 46-03); user answers U-2 and U-7 (46-CONTEXT.md). No separate sign-off on the duplicate consequence is recorded. | 2026-11-05 |
| AR-46-02 | T-46-10 | V1 pipe-write stall is not bounded by decision; measured not reached up to 256 MiB; the library's own "Write timed out" after `socket_timeout` ends it. Stated in the help text and FORK-DELTA item 14. | plan disposition (plan-check B2a, 46-03) | 2026-11-05 |
| AR-46-03 | T-46-SC (01-05) | nothing installed, no dependency change | auditor | 2026-11-05 |

## Checks Asked For Beyond The Register

- **No record data or secrets in timeout messages or query ids.** Closed. The query id is a random UUID (`Processing.java:64`, `:88`); the Processing wrapper message is `queryId.toString()` (topic, partition, offsets, UUID). The three timeout messages are fixed formats with the bound and the UUID. The pins scan every message in the cause chain and the stack trace for a 1 MiB sentinel payload on all three routes and both clients. The RowBinary and JSON failure logs a deadline cancel passes through carry coordinates, column and exception class only (Phase 45 policy, `:626`, `:1092-1100`, `:1574-1580`); the review's WR-05 is a misleading-diagnosis finding, not a disclosure.
- **Watchdog resource exhaustion.** Closed under T-46-05: at most one daemon thread per writer, one pending task per task thread, queue empty after 100 inserts, thread ended by `stop()`. Not blocking, recorded for the reader: a writer abandoned without `stop()` would keep one idle daemon thread (Kafka Connect always calls `stop()`), and `stop()` during an insert can surface `RejectedExecutionException` from `arm()` (review IN-06, shutdown only).
- **Retry-after-timeout duplicate risk.** A recorded limit, accepted as AR-46-01, not a vulnerability. The exactly-once path is intact: `Processing`, `state/` and `Utils` are byte-unchanged over the phase and the writer throws before any state change. One gap in the pin: the help-text pin asserts "may still be committed" (H11) but not the verbatim "retry can duplicate rows" clause.

## Unregistered Flags

None. All five SUMMARY `## Threat Flags` sections read "None" or "None beyond the plan's register". Open review items WR-05, IN-02, IN-05 and IN-06 are advisory code-review findings (diagnostics and shutdown edge cases), not unmapped attack surface.

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-05 | 25 | 25 | 0 | gsd-security-auditor |

## Sign-Off

- [x] All threats have a disposition
- [x] Accepted risks documented
- [x] `threats_open: 0` confirmed
- [x] `status: verified`

**Approval:** verified 2026-11-05
