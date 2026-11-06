---
phase: "47"
slug: task-stop-retry-classification
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-06"
---

# Phase 47 — Security

> Threat register verified by gsd-security-auditor against HEAD `fcd8686f` (26 rows: T-47-01 to T-47-22 plus four per-plan T-47-SC rows; plans 47-01 to 47-04 each carried a `<threat_model>`). Plan 47-05 had no PLAN.md (the orchestrator's brief was the plan): its wording corrections fall under T-47-09, T-47-14, T-47-15, T-47-17 and T-47-22, and its test-only pin cut under T-47-08, T-47-10, T-47-12 and T-47-19. Live re-checks in this audit: `ClickHouseSinkTaskStopUnitTest` 6 of 6, `RetriableRedeliveryLimitPinUnitTest` 108 of 108 and `ProcessingTest` 14 of 14 green under `testUnit`; the evidence helper read `secrets` 0 token shapes, `index` 15 rows pass, `rows` E-02..E-06, E-10 and E-12..E-15 pass, `anchors` `rows=218 bad=0`, and its md5 `7123d354...` equals Phase 46's copy. Not re-run: the full suite on ClickHouse `latest`, the tail gate and the budget-of-5 mutation (recorded in E-15 and E-13).

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka Connect worker -> `ClickHouseSinkTask.stop()` | the worker calls `stop()` once and only logs a throw (`log.warn("Could not stop task", t)`), so cleanup skipped here stays allocated in the worker JVM | task lifecycle, no data |
| `ClickHouseSinkTask` (hot path) -> every deployment | a change beyond the `try`/`finally` alters task lifecycle in production | one method body |
| Kafka Connect runtime -> connector `put` | Connect owns redelivery; the connector only chooses to throw `RetriableException` or not | exception shape (record values must never cross) |
| CHANGELOG and FORK-DELTA -> operators | operators decide upgrades and incident responses from these texts | documentation claims |
| published tag copies (gg7 CHANGELOG section, gg6/gg7 rollout notes) -> release history | rewriting them changes what a published release claimed | released text |
| `ClickHouseSinkConfig` (high-risk file) -> every deployment | a validator would reject running connectors' configurations | one config class |
| test JVM platform MBean server -> later tests | a red run leaves the task MBean registered | MBean names |
| Docker containers, downloaded sources, mutation worktree -> host and repository | throwaway images, jars and a mutated `ProxySinkTask` must never reach the repository or a classpath | probe data, scratch files |
| gate results and evidence -> verifier and Phase 49 | counts, deltas and invariants decide whether the phase is done | test counts, hashes |
| local repository -> origin | must not be crossed | refs |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-47-01 | Denial of service | `ClickHouseSinkTask.stop()` when the delivery strategy's `stop()` throws | high | mitigate | proxy stop in a `finally` (`ClickHouseSinkTask.java:185-193`; `finally` covers any `Throwable`); `ProxySinkTask.stop()` (unchanged since B0, `:76-99`) runs writer, MBean and state-provider steps each under `catch (RuntimeException \| Error)` and rethrows after all three; `strategyStopFailureStillStopsTheProxyTask` asserts writer stop 1, provider close 1, MBean unregistered, green live (6 of 6); E-02 red 2 of 6 on the unchanged tree, E-03 green; anchor rows `} finally {` and `this.proxySinkTask.stop();` (`fork-anchors.txt:261-262`) hold, `rows=218 bad=0` | closed |
| T-47-02 | Tampering | production scope on a hot-path class | high | mitigate | `git diff 427f30b4 HEAD -- src/main` is one file, one hunk inside `stop()`, 8 added and 5 removed, the four comment lines are context; empty diff for `Processing.java`, `state/` (incl. `KeeperStateProvider`), `ProxySinkTask`, `Utils`, `ClickHouseSinkConfig`, `ClickHouseWriter`; `src/main` unchanged since the E-10 tip `a9669f7b`; E-15 `stop_body_check: try/finally only`; `ProcessingTest` 14 of 14 green live | closed |
| T-47-03 | Repudiation | red-first pin red for the wrong reason | medium | mitigate | E-02: red set exactly `strategyStopFailureStillStopsTheProxyTask` and `bothStopsRunWhenTheStrategyAndTheProxyTaskThrow`, type `AssertionFailedError`, message `expected: <1> but was: <0>`, four cases green; the test file SHA-256 `465dfd49...` equals the frozen red hash at HEAD; red record `a7258507` is an ancestor of fix `796c75fa` | closed |
| T-47-04 | Repudiation | strategy's exception swallowed by the fix | medium | mitigate | no `catch` in `stop()` (`:185-193`); `assertSame(strategy.failure, thrown)` (`ClickHouseSinkTaskStopUnitTest.java:220`) green live | closed |
| T-47-05 | Repudiation | both-throw outcome claimed beyond what is measured | medium | mitigate | E-04 probe at the fix commit: proxy exception thrown, `suppressed 0`, strategy's dropped; stated as "measured and not pinned" (FORK-DELTA item 13, `docs/FORK-DELTA.md:242`) and "a one-off probe, not a pinned test" (`CHANGELOG.md` Known Issues) | closed |
| T-47-06 | Denial of service | MBean left registered, clashing in later tests | low | mitigate | unique connector name per case (`:47-52`, `SHIPPED_CONNECTOR + "-" + shipped` `:249`); `Harness.close()` calls `statistics.unregisterMBean()` (`:171`) under try-with-resources; live 6 of 6 green | closed |
| T-47-07 | Information disclosure | evidence or test text carrying a secret or record value | low | mitigate | scripted messages only (`MESSAGE`, `STRATEGY_STOP_FAILURE`); live XML scan of both pin reports: 0 hits for `SinkRecord{`, `Struct{`, `value=`, `password`, `secret`, `Bearer`; log lines carry topic, partition, offsets and a query UUID only; added lines of CHANGELOG, FORK-DELTA and tests: 0 secret-shaped matches; helper `secrets` 0 | closed |
| T-47-08 | Repudiation | an "unbounded" pin that a small budget passes | high | mitigate | **changed after plan time:** planned 500 redeliveries, now `REDELIVERIES = 50` (`RetriableRedeliveryLimitPinUnitTest.java:70`) per review WR-04; E-13 re-ran the budget-of-5 mutation: 64 redelivery rows red, each at redelivery 6, 44 first-put rows green; method renamed `retriableShapeIsStillRedeliveredAfterManyAttempts` (`:316`); Javadoc and item 17 say "a finite count: no budget below 50 exists, not that none exists above it"; 50 is above the largest setting (`retryCount` 3 to 10); residual: a budget of 50 to 499 would pass; 108 of 108 green live | closed |
| T-47-09 | Repudiation | FORK-DELTA item 17 claiming more than measured | high | mitigate | item 17 (`docs/FORK-DELTA.md:250`) numbers from E-05/E-06 (506, 60042, 120055 ms; 449, 60012, 120013 ms; 7004, 7004, 7006 ms); cadence named as the worker's `offset.flush.interval.ms`, scoped to the Kafka 3.9 line; `exactlyOnce=true` stated as not pinned; Phase 46 interaction labelled "derived"; 47-05 qualified WR-03 and WR-05 (E-14); review ran (`47-REVIEW.md`, 0 critical, 5 warnings fixed) | closed |
| T-47-10 | Tampering | existing classification rows | high | mitigate | no `src/main` change in 47-02; E-15 deltas against E-01: testUnit `removed=0 added=114` (exact), suite `removed=0 added=114` (by class); `classification_pins_equal_base: yes`; `git diff --stat 427f30b4 HEAD -- src/test` is 2 new files, 609 insertions, 0 deletions | closed |
| T-47-11 | Denial of service (operational) | permanent error classified retriable blocks its partition | medium | accept | AR-47-01 | closed (accepted) |
| T-47-12 | Tampering | mutation leaking into the main tree | medium | mitigate | E-05 and E-13 `mutation_main_tree_untouched: yes`, `mutation_worktree_removed: yes`; `ProxySinkTask.java` empty diff against B0; `git worktree list` shows only the main tree; no untracked file | closed |
| T-47-13 | Tampering | downloaded `connect-runtime` sources and container images | low | mitigate | E-06: sources read with `python3 -I` in their own directories, images recorded by digest (`cp-kafka-connect-base@sha256:93b9afd3...`, `cp-kafka@sha256:07c3d603...`), `containers_left: none`, `networks_left: none`; no `gsd47` container or network found by this audit; no jar or class file added (only the pre-existing `gradle-wrapper.jar`) | closed |
| T-47-14 | Repudiation | gg8 CHANGELOG `stop()` bullets claiming more than the pin proves | high | mitigate | each claim maps to a pin or probe: exception from the strategy reaches the caller (`assertSame`, `:220`), `stop()` returns normally for the three shipped strategies (`:248`), both-throw (E-04); Connect "only logs a WARN" labelled "read in its sources"; 47-05 fixed WR-02 and IN-03 (E-12); the committed text now diverges from `47-doc-edits.json` by design (`plan_time_text_diverges: yes`, E-12) | closed |
| T-47-15 | Tampering | published gg7 CHANGELOG section and gg6/gg7 rollout notes | high | mitigate | CHANGELOG diff 26 insertions, 0 deletions; the rest of the file is byte-identical to B0 (`cmp`); `ROLLOUT-v1.7.0_gg6.md` and `ROLLOUT-v1.7.0_gg7.md` empty diff against B0; gg7 note empty diff against tag `v1.7.0_gg7`; `VERSION` unchanged (`v1.7.0_gg7`) | closed |
| T-47-16 | Tampering | validator or budget in `ClickHouseSinkConfig`, `Utils`, `ProxySinkTask` | high | mitigate | the three files have an empty diff against B0; `warnOnUnvettedRetriableCodes` intact (`ClickHouseSinkTask.java:58`, `:136`) and its two WARN tests plus `AdditionalRetriableErrorCodesPinUnitTest` not modified (the test diff has no deletions or modified files); `src/main` changed only in the LIF-01 file | closed |
| T-47-17 | Repudiation | FUT-05 closed without the user's words | medium | mitigate | `47-CONTEXT.md:18-19` records U-1 and U-2 (AskUserQuestion answers 2026-11-05); items 8 and 24 (`docs/FORK-DELTA.md:232`, `:264`) say "closed by the user's decision of 2026-11-05" with the reason; `FUT-05 stays open` count in FORK-DELTA is 0 | closed |
| T-47-18 | Denial of service (operational) | connector listing a permanent code keeps redelivering | medium | accept | AR-47-01 | closed (accepted) |
| T-47-19 | Repudiation | phase gate results | medium | mitigate | E-10 and E-15 rows pass; E-15 gate on `f3612284`: testUnit 2295 / 0 / 0 / 2 skipped, suite 2898 / 0 / 0 / 18 skipped on ClickHouse 26.10.3.7 x V2, deltas `removed=0 added=114`; helper `index 15 --done` OK; the 47-05 pin cut and the stop pin re-ran green live here | closed |
| T-47-20 | Tampering | exactly-once `BEFORE`/`AFTER` flag sequence | high | mitigate | `Processing.java` and `state/` (incl. `KeeperStateProvider.java`) empty diff against B0, as are `ProxySinkTask`, `Utils`, `ClickHouseSinkConfig`, `ChunkFlusher` and the three strategies; the only production change is the `stop()` body; `ProcessingTest` 14 of 14 green live and in E-15 | closed |
| T-47-21 | Tampering | origin branches and tags | high | mitigate | `git ls-remote origin` returns nothing for `refs/tags/pre-v1.7.0_gg8`, `refs/tags/v1.7.0_gg8` and `refs/heads/sync/v1.7.0`; `git branch -r --contains HEAD` is empty (the rollback tag `pre-v1.7.0_gg8` exists locally only) | closed |
| T-47-22 | Tampering | published tag copies edited instead of handed off | medium | mitigate | gg7 note equals its tag, gg6 note equals B0, gg7 CHANGELOG section byte-identical inside the gg8-prefixed file (see T-47-15); the hand-off is recorded in E-11 | closed |
| T-47-SC (01) | Tampering | package installs | low | accept | no dependency change (`build.gradle.kts`, `settings.gradle.kts`, `gradle`, `.github` empty diff) | closed (accepted) |
| T-47-SC (02) | Tampering | package installs | low | accept | same; no image or jar in the change set | closed (accepted) |
| T-47-SC (03) | Tampering | package installs | low | accept | same | closed (accepted) |
| T-47-SC (04) | Tampering | package installs | low | accept | same; build files verified unchanged since B0 | closed (accepted) |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-47-01 | T-47-11, T-47-18 | Unbounded redelivery of a `RetriableException` from `put` is a recorded limit, not a vulnerability. A writer failure the connector classifies as retriable, including a code the operator lists in `additionalRetriableErrorCodes`, is redelivered by Kafka Connect with no count and no deadline, so a permanent error can block its partition. Basis: (1) the user decided to record the limit and add no retry budget (U-1) and no validator (U-2), 2026-11-05, `47-CONTEXT.md:18-19`; (2) measured on a Connect worker of the Kafka 3.9 line (E-06) and read in the `WorkerSinkTask` sources of 3.9.1, 4.1.1 and 4.4.0: Connect pauses the partitions and redelivers the same batch, `errors.retry.timeout`, `errors.tolerance` and the DLQ do not apply, the DLQ stayed empty, the task stayed `RUNNING`, one ERROR line per redelivery, cadence the worker's `offset.flush.interval.ms` (measured for the 3.9 line only); (3) every budget would need a new connector key, because `errors.retry.timeout` belongs to Connect and `retryCount` is the client's own; (4) the start-up WARN for unvetted codes remains (`ClickHouseSinkTask.java:58`, `:136`) and names the code, and the remedy exists (change the setting); (5) pinned by `RetriableRedeliveryLimitPinUnitTest` (64 redelivery rows at 50 attempts, both `errors.tolerance` values, `ProxySinkTask.put` and `ChunkFlusher.putDirect`). Stated limits of the basis: the pin is a finite count, `exactlyOnce=true` is not pinned, first-put outcomes of the non-retriable and conversion shapes through `putDirect` are not pinned, and Connect behaviour outside the 3.9 line is read, not measured. Derived consequence: with `exactlyOnce=false` a stalled insert cut by `clickhouseClientInsertTimeoutMs` is attempted again at every redelivery and each attempt can commit rows (see AR-46-01). Owner and re-open trigger are in FORK-DELTA items 17 and 24. | user decisions U-1 and U-2 (`47-CONTEXT.md`); plan disposition (47-02, 47-03) | 2026-11-05 |
| AR-47-02 | T-47-SC (01-04) | nothing installed, no dependency change | auditor | 2026-11-06 |

## Checks Asked For Beyond The Register

- **`stop()` cleanup on any Throwable, no leaked state provider or MBean.** Closed. `ClickHouseSinkTask.stop()` runs the proxy stop in a `finally` (`:185-193`), which runs for an `Exception` or an `Error` from the strategy. `ProxySinkTask.stop()` (unchanged since B0) runs the writer, the MBean and the state provider each in its own `catch (RuntimeException | Error)` and rethrows only after all three ran, so one failing step cannot skip another. Pinned for a throwing strategy (writer stop 1, provider close 1, MBean unregistered), for both stops throwing, for no strategy, and for the three shipped strategies, all green live. Recorded limits, not leaks: with both throwing, the proxy's exception replaces the strategy's, not suppressed and not logged (E-04, item 13, CHANGELOG Known Issues). Documented pre-existing residuals outside this change: a `stop()` with no `proxySinkTask` after a failed Keeper constructor, `close(partitions)` skipping `deliveryStrategy.close` if `onPartitionRemoved` throws, and `ClickHouseWriter.stop()` swallowing an interrupt (review IN-04).
- **No record data or secrets in test output, CHANGELOG, FORK-DELTA.** Closed. The strategies' `stop()` logs only a buffered-record count. The new tests use fixed scripted messages and synthetic records; both pin reports have 0 hits for record or credential patterns; the added lines of CHANGELOG, FORK-DELTA and tests have 0 secret-shaped matches; helper `secrets` reads 0 token shapes.
- **Unbounded redelivery is an accepted risk, not a vulnerability.** See AR-47-01, with its basis and limits.
- **Nothing pushed.** Closed under T-47-21.

## Unregistered Flags

None. All five SUMMARY `## Threat Flags` sections read "None". Open review items IN-01, IN-02 and IN-04 are advisory (test brittleness and a pre-existing interrupt swallow), not unmapped attack surface.

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-06 | 26 | 26 | 0 | gsd-security-auditor |

## Sign-Off

- [x] All threats have a disposition
- [x] Accepted risks documented
- [x] `threats_open: 0` confirmed
- [x] `status: verified`

**Approval:** verified 2026-11-06
