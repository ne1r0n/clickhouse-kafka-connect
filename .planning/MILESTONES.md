# Milestones

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
