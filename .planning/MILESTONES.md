# Milestones

## v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening (Shipped: 2026-10-10)

**Phases completed:** 4 phases (12–15), 25 plans, 55 tasks

**Delivered:** Upstream `v1.7.0` was merged into `custom_build` and published as `v1.7.0_gg1`, with all 17 follow-ups carried from earlier milestones closed on the merged tree. The biggest change is that R-01's silent data loss is fixed: `Struct` and CDC records under `insertFormat=json` now land through a schema-aware JSON writer, and a live twin-table matrix proves parity with RowBinary. Nested nulls in RowBinary either round-trip or fail loudly. D-17 is retired onto upstream's `setCompressionMethod`, and five dependencies were bumped one commit at a time. CI now runs exactly once per published SHA. Operators get a rollout checklist (`docs/ROLLOUT-v1.7.0_gg1.md`) covering every behaviour change.

**Stats:**

- Git range: `94d98615` (milestone start) → `cb6918f9`. That is 153 first-parent commits, 175 including the merged upstream history.
- Timeline: 2026-10-07 → 2026-10-10 (4 days)
- Files changed (incl. the upstream merge): 161 total (+26,882 / −369). Of those, 49 are in `src/`, build, `gradle/` and `.github/` (+4,927 / −221).
- Codebase: 10,119 LOC main Java; 29,523 LOC test Java (`src/test` 23,416, `src/testFixtures` 4,329, `src/integrationTest` 1,778)
- Fork delta over upstream: files 84→95 (wide) and 30→31 (`src/main`). Insertions 9,476→13,587 (wide) and 1,368→1,826 (`src/main`). The growth is hardening and tests on top of the merge, not unretired debt.
- Suite: 830 tests, 0 failures, 0 errors, 18 named skips on ClickHouse `latest` (26.9.14.10), passed twice (`clean test`, then `test --rerun`). `testUnit` runs 454 tests with Docker stopped.
- Requirements: 39/39 complete
- Published: `custom_build` = `--no-ff` merge `f328fd21` (of the rehearsed tree `ae8520ed`), with tags `v1.7.0_gg1` and `pre-v1.7.0-sync` (rollback → `c61d90ce`) on `origin`
- CI: run [`38050860929`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38050860929) on `f328fd21` is green in 389 s. It is the only run on that SHA, with no cancelled sibling, which proves CI-08 end to end.

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clear in every category. The override is for two other reasons, both accepted by the user at close:

1. **Phases 12–13 verification reads stale.** Both `VERIFICATION.md` files have `status: passed`. Their covered-file digests no longer match because Phases 13–15 later edited the files they covered (`ClickHouseWriter`, `docs/FORK-DELTA.md` and others). That is ordinary sequential drift, not a failed or reversed check. Phases 14 and 15 verification are current and `passed`, and Phase 15 UAT was 1/1. Phase 15's two full-suite runs and the CI run on the published SHA cover the final tree.
2. **No milestone audit was run.** There is no `v1.7.0_gg1-sync-MILESTONE-AUDIT.md`. Accepted because 39/39 requirements are checked and mapped, and Phase 15 verified the end-to-end result directly.

**Known gaps:** none against requirements. TYPE-08 (`Int256`/`UInt256` only) and JSONP-08 (evaluation order only) were narrowed on the record. Eleven follow-ups are open under `PROJECT.md` → Requirements → Active:

- **Most urgent:** the corrected rollout doc `505e4981` is not yet on `origin`. The published copy still offers `TRUNCATE` on the KeeperMap state table that exactly-once connectors share.
- Backlog 999.1: `Nullable(Enum8|Enum16|Tuple)` is mis-mapped by `Column.extractColumn`. It is warned about at discovery.
- Backlog 999.2: the test classpath carries a second lz4.

**Key accomplishments:**

- Merged upstream `v1.7.0` as one mechanical commit (`54e63648`, zero authored lines) behind a parameterised invariant script, every check of which was proven able to fail. A 33-verdict per-hunk checklist covers the auto-merged hunks in fork-touched files.
- Retired D-17 onto upstream's `setCompressionMethod` for every client the connector builds, pinned Docker-free and proven live with the flag on and off. Made upstream #820's null-Map bytes reachable by scoping the fork's MAP guard to `Nullable`. Proved `KeeperStateProvider`'s new streamed-row read with a restart test that was seen to fail against the old code.
- Fixed R-01 by routing. `Struct` and Debezium CDC records in metadata-free JSON mode go through a new `MetadataFreeJsonWriter`, which writes `Timestamp`, `Date`, `Time`, `Decimal`, `bytes` and Debezium `Timestamp`/`NanoTimestamp` as RowBinary would. A live twin-table matrix proves it.
- Nested nulls in RowBinary now round-trip in `Nullable` slots, coerce to empty for `Array`/`Map`, or fail the batch with a data-free `DataException` naming the column. Metadata, config and test-debt findings were closed: DESCRIBE skip at any depth, `Int256` fail-fast, `extractColumn` argument order, the `USERNAME` default, typed read-back and attributable teardown.
- Bumped lz4-java (1.10.4 → 1.12.0, advisory-free), okhttp, mockito, Jackson (two CVEs closed) and org.json, one catalog commit each, each gated by its own tests. `tests.yaml` now triggers on branch pushes only, compiles integration sources and reports its fork count.
- Gated the exact tree twice (830 tests), rehearsed it on CI, and published it as `v1.7.0_gg1` after a recorded human approval. It shipped with a CHANGELOG section, a rewritten `FORK-DELTA.md` and an eight-item operator rollout checklist.

---

## v1.6.0_gg1-sync Upstream v1.6.0 Sync (Shipped: 2026-10-07)

**Phases completed:** 6 phases, 24 plans, 63 tasks

**Delivered:** Upstream `v1.6.0` (581 commits past `v1.2.9`) merged into `custom_build` and published as `v1.6.0_gg1`. Every fork feature was either re-applied onto upstream's rewritten code or consciously retired in favour of upstream. The fork's suite plus upstream's 56 test files run green on ClickHouse `latest` × client V2, and CI is green on the published SHA. `docs/FORK-DELTA.md` records what the fork still patches and what it gave up. Two behaviours that vanished silently during the merge were caught by the gate before publication: the `latest` test-image default, and client `0.9.6` failing against ClickHouse 26.9's ZSTD default.

**Stats:**

- Git range: `214571b2` (milestone start) → `01b7880d` — 170 first-parent commits (751 including the merged upstream history)
- Timeline: 2026-09-18 → 2026-10-07 (20 days)
- Files changed (incl. the upstream merge): 360 total (+48,383 / −5,486); 216 in `src/`, build, `gradle/` and `.github/` (+19,778 / −4,153)
- Codebase: 9,622 LOC main Java; 25,473 LOC test Java (`src/test` 19,571, `src/testFixtures` 4,215, `src/integrationTest` 1,687)
- Fork delta over upstream: files 93→84 (wide) / 32→30 (`src/main`); insertions 8,401→9,476 (wide) / 1,308→1,368 (`src/main`)
- Suite: 663 tests, 0 failures, 0 errors, 18 named skips on ClickHouse `latest` (26.9.12.8); `testUnit` 331 tests with Docker stopped
- Requirements: 53/53 complete
- Published: `custom_build` = merge `c61d90ce`, tags `v1.6.0_gg1` and `pre-v1.6.0-sync` (rollback) on `origin`
- CI: run [`37619434848`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37619434848) on `c61d90ce` — green, 386 s

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clear in every category. The override is for two other reasons, both accepted by the user at close:

1. **Phases 6–10 verification reads stale.** Every phase `VERIFICATION.md` has `status: passed`. Phases 6–10 fall outside their freshness digest because later phases edited files those verifications covered: Phase 11 touched `ClickHouseHelperClient`, `ClickHouseTestHelpers` and `tests.yaml`. That is ordinary sequential drift, not a failed or reversed check. The milestone claim rests on Phase 11, whose verification is current and `passed`, with UAT 5/5. It re-ran the full suite locally and verified CI on the published SHA.
2. **No milestone audit was run.** There is no `v1.6.0_gg1-sync-MILESTONE-AUDIT.md`. Accepted because 53/53 requirements are checked and mapped, and Phase 11 verified the end-to-end result directly.

**Known gaps:** none against requirements. Phase 11 UAT resolved its five human-verification items, including CI-06's narrowed "exactly one run" locator and SUITE-03's mixed raw delta numbers. `PROJECT.md` → Requirements → Active carries 17 follow-ups. R-01's silent data loss is still pinned, not fixed. The `lz4` pin is a client-`0.9.6` workaround with a recorded removal condition.

**Key accomplishments:**
- Landed the 581-commit upstream v1.6.0 merge onto scratch branch `sync/v1.6.0` as one 100%-mechanical commit (`673d29c3`) with every one of the 40 conflicted paths verified byte-identical to upstream, backed by a committed, self-testing invariant script.
- Re-applied all six fork `build.gradle.kts` additions (JaCoCo report-only, `testUnit`, JDK-17 test-JVM pin, LZ4 capability resolution, ben-manes `dependencyUpdates`, Spotless config) onto upstream v1.6.0's version-catalog build file, landed `VERSION=v1.6.0_gg1` plus the `.gitignore`/`tests.yaml` settlement, and proved the four D-12 resolved-version facts (`com.clickhouse@0.9.6`, `connect-api@4.1.1`, `slf4j-reload4j@2.0.17`, `at.yawk.lz4:lz4-java:1.10.2`) against the real dependency graph.
- Generated `.planning/MERGE-CHECKLIST.md` — a 40-row, generated (not transcribed) per-path resolution gate for Phases 7-11, an 8-row "read anyway" list correcting a research-prose error about `Processing.java`, and a `## Recorded Facts` section closing SYNC-04/SYNC-05 — then re-proved both merge invariants across the full 588-commit phase range.
- Unblocked `compileJava` with a one-line `@Override` removal, then re-applied `Type.DYNAMIC` dispatch and a shared RowBinary fail-fast (`DataException`) for `DYNAMIC`/`UNKNOWN` columns onto upstream `v1.6.0`'s rewritten mapping and writer classes — three isolated commits, each independently bisectable.
- Re-threaded server-side `targetTableFilter` table discovery filtering and the DESCRIBE sub-column skip onto upstream `v1.6.0`'s rewritten `showTables`/describe-loop shape, re-expressing the fork's `shouldSkipDescribeRow` over a loop-local column map since upstream's `Table` is now immutable — two isolated commits, each independently bisectable against `v1.6.0`.
- Landed the remaining fork config settings (`jsonInsertColumns` key + explicit-format flag, `async_insert` un-forcing) and the planned four-item Phase 10 test carve-out, then discovered and mechanically fixed a much larger compile gap — twelve additional fork test files depending on pre-merge `ClickHouseBase`/`ClickHouseTestHelpers` methods the mechanical merge silently dropped — while deliberately leaving two files unfixed because completing them would require writing Phase 8's or Phase 10's own work inside this plan.
- Pinned all thirteen new upstream config options at their defaults with a new Docker-free test, fixed a test-construction NPE the acceptance-gate run surfaced, landed a user-approved build.gradle.kts test-compile carve-out for two future-phase files, and ran `testUnit` green with Docker genuinely stopped - then flipped Phase 7's twelve merge-checklist rows and recorded the two free wins.
- Restored the JMX `connector=<name>` ObjectName segment (deleted by the v1.6.0 merge) by threading `connectorName` from `ClickHouseSinkTask.start()` through `ProxySinkTask` into a new `SinkTaskStatistics(connectorName, taskId)` constructor, with the per-topic name composed from the task-level name rather than a second format string, and put the `MBeanNameUnitTest` canary back into the compiled test suite.
- Closed Phase 8 on live evidence: testUnit green (317 tests, 0 failures), the full live suite green modulo exactly the one pre-known carry-in (636 tests, 1 failure, 0 errors, 35 explained skips), and `TableTest.extractNullables()` traced to `ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally stripping Nullable `.null` subcolumns — a fork behavior already pinned by `DESC-01`, not a Table.java defect.
- Discharged ORCH-01, ORCH-04 and FREE-01 by grep/diff evidence and a written record (no `src/` line touched), flipped Phase 8's eight `MERGE-CHECKLIST.md` rows to done, annotated the two Phase-10-owned rows Phase 8 landed early work against, and appended three Recorded Facts — while catching and reporting that the plan's own "`ProcessingTest.java` is byte-identical" premise had gone stale due to plan 08-01's own necessary compile fix.
- Re-applied the fork's four metadata-free `insertFormat=json` guard sites behind one shared predicate, rebuilt the synthetic `Table` against upstream's immutable constructor, re-attached `jsonInsertColumns` to the surviving `client.insert` overload, and un-parked the Docker-free canary test that now pins all of it inside `testUnit`.
- Re-enabled all 13 parked live JSON-path test methods (unchanged), adopted the TableTest.extractNullables() carry-in fix, and ran the full suite against real ClickHouse latex x client V2 clean: 643 tests, 0 failures, 0 errors, 18 skips (down from Phase 8's 35).
- Read the resolved `ClickHouseWriter.java` diff against `v1.6.0` to discharge JSONP-04 (no code, no test), then put Phase 9's decisions on the permanent record: JSONP-03's corrected rationale, all four JSONP requirements closed, `MERGE-CHECKLIST.md` row 26 annotated, Recorded Fact 13 resolved, and a new operator-confirmed Recorded Fact 14 stating the KEEP disposition with its accepted cost.
- Re-derived `ClickHouseBase`'s shared-container-per-fork contract over upstream's per-class rewrite, restored the `@AfterEach` MBean sweep with a proven per-topic extension, and fixed a silent `latest`→pinned-LTS version regression that had every local gate since the v1.6.0 merge testing the wrong ClickHouse server.
- Proved HARN-01's Docker-free claim with the daemon genuinely stopped, ran two back-to-back full suites on ClickHouse `latest` with byte-identical executed-class sets and zero failures, and settled D-06 with nothing to attribute — the milestone's first full-suite evidence on the server it actually claims to test.
- Closed Phase 10's books by re-running every HARN-04/05/06/07 verifying command live against the current tree, flipping all 17 Phase-10-owned MERGE-CHECKLIST.md rows to done with two new Recorded Facts, and closing HARN-01 through HARN-07 in REQUIREMENTS.md with a correction blockquote naming which four requirements closed on evidence rather than new code.
- The merge-invariant script now fails closed when `git grep` itself errors (proved by a rejected pathspec that the old script passed), the last six MERGE-CHECKLIST rows are closed on diffs measured now, and Phases 07 and 09 have 0-open security audits, with no production code touched.
- CI no longer pins the ClickHouse server or client version in its job environment, so it exercises the same code defaults as a local run, with the Docker-free `ClickHouseVersionDefaultUnitTest` as the detector, and the Javadoc that asserted the pin is corrected in the same commit.
- docs/FORK-DELTA.md: a grep-confirmed ledger of what the fork still patches and retired in favour of upstream v1.6.0, all twelve raw numbers (insertions up, stated plainly), R-01 silent data loss published first, and the 18-skip inventory, linked from README and AGENTS
- One `--rerun` full suite on ClickHouse `latest` (26.9.12.8) x client V2 passes at 663 tests, 0 failed, 0 errors, 18 skipped with the skip set identical to Phase 10's named 18, all five SMT classes clean and unskipped, and `testUnit` green at 331 tests with the Docker daemon confirmed down; the run was a 6-fork run, not the single-fork shape the plan assumed.
- `sync/v1.6.0` was pushed to origin and its single `tests.yaml` run (id 37617508949) finished `success` in 350 s (5.8 min) against a 22.5-minute threshold, with a 816160-byte `jacoco-coverage-reports` artifact; the six-item D-03 bundle is re-derived below and surfaces three findings for plan 11-06's gate, none of them a blocker.
- SUITE-01..03 and CI-05..07 are closed in `REQUIREMENTS.md` on commands re-run now (a fresh `--rerun` of 663 tests with 0 failures and a skip set identical to Phase 10's, plus the REST API, `git ls-remote` and four shortstat commands), with a blockquote that says plainly that this phase's real risk was CI and the one-way push, not the suite; nothing was pushed.
- Connector now sends `network_compression_method=lz4` on every V1 and V2 client (V2 `serverSetting` before operator options, V1 `CUSTOM_SETTINGS` in the shared default options), so client 0.9.6 decodes ClickHouse 26.9's responses again; pinned Docker-free and live.

---

## v1.2.9_gg4-tests Fork Test Coverage (Shipped: 2026-09-18)

**Phases completed:** 5 phases, 16 plans, 45 tasks

**Delivered:** Every behavior the `custom_build` fork adds on top of upstream ClickHouse Kafka Connect is now pinned by a named automated test running against ClickHouse `latest` × client V2 — the four fork SMTs, `targetTableFilter`, `describeTable` sub-column skipping, `JSON`/`JSON(...)`/`Dynamic` column mapping and round-trips, the metadata-free JSON insert path, `jsonInsertColumns`, JMX MBean naming, and `async_insert` pass-through. Seven production bugs surfaced by those tests were fixed in the phase that found them; an eighth (R-01 silent data loss) was pinned rather than fixed by explicit user decision. CI runs the suite on every push and publishes coverage.

**Stats:**

- Git range: `f8e4271` (roadmap created) → `a351121` — 110 commits
- Timeline: 2026-09-15 → 2026-09-18 (4 days)
- Files changed: 115 total (+26,583 / −249); 38 files in `src/`, `.github/`, and build config (+5,742 / −117)
- Codebase: 15,179 LOC test Java, 6,850 LOC main Java
- Suite: 371 tests, 0 failures, 0 errors, 7 pre-existing skips
- Requirements: 75/75 v1 requirements complete, 0 unmapped
- CI: run [`35317272594`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594) on `a295af8` — green

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clean across every category — no open debug sessions, threads, seeds, quick tasks, UAT gaps, verification gaps, context questions, or deferred items. The override is recorded for two other reasons, both accepted by the user at close:

1. **Phases 1–4 verification reads stale.** All five phase `VERIFICATION.md` files carry `status: passed`, but phases 1–4 fall outside their freshness window because later phases legitimately edited files those earlier verifications covered — e.g. Phase 1 verified `ClickHouseBase.java`, which Phase 3 then modified. This is ordinary sequential-phase drift, not a failed or reversed check. The milestone-level claim rests on Phase 5, which is freshly `passed` and independently re-confirmed the full suite green both locally and in CI.
2. **No milestone audit was run.** `/gsd-audit-milestone` was never invoked, so no `v1.2.9_gg4-tests-MILESTONE-AUDIT.md` exists in the archive. Accepted on the basis that all 75/75 requirements are checked off and mapped, and Phase 5 verified the end-to-end result directly.

**Known gaps:** none against v1 requirements. Nine review-surfaced follow-ups are carried into `PROJECT.md` → Requirements → Active; two affect production behavior (R-01 fail-fast, client-V2 error classification is dead code).

**Key accomplishments:**

- The untouched `custom_build` suite was already green against ClickHouse `latest` x client V2 (0 failures) once a sandbox environment artifact was neutralized; the default test image now points at `latest`, and a new server-version smoke test pins the container-image and version-string contract for future phases.
- One shared ClickHouse `latest` container per test JVM fork (down from one per class), a fresh isolated database per test class, isolated 23.8 proxy containers, an `@AfterEach` MBean leak sweep proven by a deliberate mid-test failure, and UTF-8 decoding in test helpers proven RED-then-GREEN — full suite green at 194 tests, 0 failed, 7 skipped.
- JaCoCo 0.8.15 wired to `test` only with a full report and a 15-class fork-changed report (Lombok-generated methods excluded via `lombok.config`), plus a Docker-free `testUnit` task that stays out of `check` — full suite still green at 194 tests, 0 failed, 7 skipped with the agent attached.
- All five TableOperationFilter unit-level requirements (TOF-01..05) pinned by 17 new named tests, plus the shared `TransformTestHelpers` builder class and a two-line `configure()` reconfigure fix (D-03).
- All six HeaderToValue unit-level requirements (HDR-01..06) pinned by 22 new named tests, plus a D-02 stale-schema-cache guard that closes a real mid-stream Debezium data-loss path.
- All eight HeadersToValue unit-level requirements (HDRS-01..08) pinned by 23 new named tests (25 total), plus the D-02 stale-schema-cache guard and a corrected HDRS-04 requirement wording.
- Fixed the D-01 nested-Struct-in-collection bug in `ConvertToMapOrJson.convertFieldValue()` and pinned all seven unit-level requirements (CONV-01..CONV-07) with 24 new named tests, taking the file from 13 to 37 tests.
- A new `SmtSinkLandingTest` class proves all four fork SMTs against ClickHouse `latest`/client V2 through the real insert path, and fixes a production RowBinary null-marker bug in `ClickHouseWriter`'s MAP branch that the HDRS-09 test tripped.
- Extracted `shouldSkipDescribeRow`/`buildTableFilterClause` from `ClickHouseHelperClient`, pinned FILT-01..04 and DESC-01..04 with 25 new Docker-free/ClickHouse-backed tests, and fixed the deferred Phase 1 non-ASCII DESCRIBE-reader bug.
- Pinned TYPE-01/02 type-string dispatch and TYPE-03/04/05/07 JSON+Dynamic round-trips across both fork insert routes (schemaless JSONEachRow, bypassRowBinary Struct records) with typed SQL read-back instead of row counts, plus non-ASCII values under the windows-1252 test JVM.
- RowBinary path now throws an actionable DataException (naming the column, its type, and the insertFormat=json/bypassRowBinary=true escape hatches) instead of silently corrupting the stream, pinned Docker-free at both internal gates and live through a full ClickHouseSinkTask.put() failure that leaves zero rows.
- Pinned the metadata-free `insertFormat=json` insert path (gate, table resolution, field cleanup, tombstone handling) with two new fork-only test classes, upgraded both pre-existing explicit-JSON tests to typed assertions, and resolved the R-01 silent-data-loss finding via a blocking-human checkpoint answered `pin-current-behavior`.
- Pinned every insert-time consequence of `jsonInsertColumns` (default list, explicit subset against non-trivial DEFAULTs, extra payload keys, a nonexistent column's non-retriable classification, and the non-explicit path's interaction with `cleanupExtraFields`) plus its Docker-free parsing contract, in two new fork-only test classes.
- Extracted `ProxySinkTask.buildMBeanName` byte-for-byte (the phase's one pre-approved main-code change) and pinned every JMX MBean naming/registration branch (JMX-01..04) plus the fork's `async_insert` pass-through (ASYNC-01..03) in four new fork-only test classes, closing out Phase 4's third and final plan.
- Rewrote the fork's push-triggered `tests.yaml` into a single ClickHouse `latest` x client V2 job on current GitHub Actions tags, backed by a new stdlib-only `jacoco_summary.py` that renders JaCoCo coverage into both the job summary and stdout with no coverage threshold.
- Pushed `custom_build` to the public fork after human approval and machine-verified a fully green `tests.yaml` run (run 35317272594) on the pushed SHA via the unauthenticated GitHub REST API — exactly one job, both key steps and both artifacts confirmed.

---
