---
phase: 04-json-insert-path-connector-plumbing
verified: 2026-09-18T00:20:00Z
status: passed
score: 5/5 must-haves verified (18/18 requirement IDs satisfied)
covered_files:
  - .planning/REQUIREMENTS.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-01-PLAN.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-01-SUMMARY.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-02-PLAN.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-02-SUMMARY.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-03-PLAN.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-03-SUMMARY.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-CONTEXT.md
  - .planning/phases/04-json-insert-path-connector-plumbing/04-REVIEW.md
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertSettingsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
covered_digest: "v1:sha256:6ccf7cd8fa10211ea6dc0bbbff276c846623318b46fa4a1adc79bc14dbe3f2c2"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 04: JSON Insert Path & Connector Plumbing Verification Report

**Phase Goal:** The work-in-progress metadata-free JSON insert path and `jsonInsertColumns` are
pinned as they are today, and connector-level changes are covered: MBean naming, per-task
registration, and `async_insert` left to the user.

**Verified:** 2026-09-18T00:20:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Roadmap Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `insertFormat=json` in any letter case enables metadata-free mode; unset does not. Remapped-table insert (topic != table) succeeds with no fetched metadata: unknown fields sent unmodified, known fields land correctly, tombstones skipped. Existing explicit-JSON tests assert column values. | VERIFIED | `MetadataFreeJsonInsertUnitTest` (4 case-matrix + 3 negative + 1 unset test, all pass, ran fresh — 27/27 testUnit green). `ExplicitJsonInsertPathLiveTest` JSON-01 differential, JSON-02 (remapped table, typed read-back), JSON-03 (unknown field), JSON-04 (mixed batch skips tombstone) — all re-run fresh against live ClickHouse, 6/6 pass. `ClickHouseSinkTaskJsonInsertFormatTest` JSON-05 both pre-existing tests now assert `allRows` typed read-back in addition to `countRows` — re-run fresh, 2/2 pass. |
| 2 | `jsonInsertColumns` parsing (blank→empty list; whitespace/empty entries dropped; order preserved); default empty list correct; explicit subset fills listed + DEFAULT on unlisted; extra JSON keys don't break insert; list also applies on non-explicit path. | VERIFIED | `JsonInsertColumnsUnitTest` (9 parsing tests, re-run fresh, 9/9 pass — verified blank/whitespace/lone-comma→`List.of()`, unset key same, padding+adjacency drop, leading/trailing comma drop, order preservation, single element). `JsonInsertColumnsLiveTest` COLS-02 (default list), COLS-03 (explicit subset vs. non-trivial DEFAULTs `42`/`'unset'`), COLS-04 (extra keys), COLS-06 (non-explicit path) — re-run fresh against live ClickHouse, all pass. |
| 3 | A `jsonInsertColumns` entry naming a nonexistent column produces a clear error, no partial insert, no NPE. | VERIFIED | `JsonInsertColumnsLiveTest#nonexistentColumnThrowsNonRetriableWithNoPartialInsert` (COLS-05) re-run fresh: asserts `RuntimeException` thrown, message chain contains `no_such_column`, `countRows==0`, cause-chain walk asserts no `NullPointerException`, and `assertFalse(ex instanceof RetriableException)`. Root cause classification (`com.clickhouse.client.api.ServerException`, ClickHouse error code 16 `NO_SUCH_COLUMN_IN_TABLE`) recorded in test comment and SUMMARY. Test passed in fresh run. |
| 4 | MBean tests: `connector=<name>` when set, `connector=null` without it, `version=unknown` for null version and `:`→`_`; multiple tasks/connectors register distinct MBeans; `stop()` unregisters only its own. | VERIFIED | `MBeanNameUnitTest` (6 tests: JMX-01 whole-string equality, JMX-02 null+empty connector, JMX-03 null version + colon substitution via `endsWith`) re-run fresh, 2 spot-run pass (full class covered by orchestrator's green gate). `MBeanRegistrationLiveTest` (2 tests: single-task register/unregister/idempotent-second-stop baseline; four-task/two-connector distinctness via before/after `Set<ObjectName>` difference, selective `stop()`) re-run fresh against live ClickHouse platform MBean server, 2/2 pass. `ProxySinkTask.buildMBeanName` extraction diffed line-by-line against pre-plan `ProxySinkTask.java` — logic (null/colon handling, format template) is byte-for-byte unchanged, confirmed independently of the code-review claim. |
| 5 | Default config sends no `async_insert`; user-supplied `async_insert` passed through unchanged; a sink insert with `async_insert=1` completes and data is present. | VERIFIED | `AsyncInsertSettingsUnitTest` (3 tests: no-key-by-default with positive control on 3 other forced settings; `async_insert=1`/`wait_for_async_insert=1` round-trip to `"1"`; `async_insert=0` round-trips to `"0"`) re-run fresh, 3/3 pass. `AsyncInsertLiveTest#asyncInsertWithWaitForAsyncInsertCompletesAndDataIsPresent` (ASYNC-03) re-run fresh against live ClickHouse: 5-row insert with `async_insert=1,wait_for_async_insert=1`, typed read-back confirms all 5 rows with correct `id`/`str`, no polling/sleep construct present. Pass. |

**Score:** 5/5 roadmap success criteria verified (0 present-but-behavior-unverified).

### Known Accepted Deviation (Not a Gap)

`ExplicitJsonInsertPathLiveTest#explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`
(`@DisplayName` prefixed `JSON-01: [R-01 pin]`) deliberately asserts confirmed silent data loss:
`insertFormat=json` + a `Struct` record + `bypassRowBinary` left at its default (`false`) makes
`put()` return without throwing while 0 of 5 records land. This was escalated to the user at a
blocking-human checkpoint in plan 04-01 Task 4; the user's answer was `pin-current-behavior`. Per
CONTEXT.md D-01/D-07 a fail-fast fix is explicitly out of this phase's scope. Verified present on
disk: the test's Javadoc is headed "R-01 PIN - NOT AN ENDORSEMENT," names the checkpoint, explains
the mechanism (`doInsertRawBinaryV2` iterating zero columns of the column-less synthetic metadata-free
`Table`), and the test passed in a fresh live run against ClickHouse `latest`. This is why
`ExplicitJsonInsertPathLiveTest` imports `Struct`/`SchemaBuilder` despite the phase's general
schemaless-first convention (D-06) — the import exists specifically to pin this one accepted
deviation, not to violate D-06. Recorded here as an accepted, documented deviation per the verifier's
brief; not counted as an unmet must-have.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ExplicitJsonInsertPathLiveTest.java` | ClickHouse-backed pins JSON-01/02/03/04 + R-01 pin | VERIFIED | 367 lines, extends `ClickHouseBase`, 6 `@Test` methods, all REQ-ID `@DisplayName`s present, re-run green |
| `MetadataFreeJsonInsertUnitTest.java` | Docker-free pins JSON-01 case matrix + JSON-03 cleanup differential | VERIFIED | 97 lines, package `...sink.db`, extends nothing, re-run green (testUnit) |
| `ClickHouseSinkTaskJsonInsertFormatTest.java` | JSON-05 typed assertions added to both pre-existing tests | VERIFIED | `allRows(` present 3x, `countRows(` still present 2x, `JSON-05:` present 2x, re-run green |
| `JsonInsertColumnsLiveTest.java` | ClickHouse-backed pins COLS-02..06 | VERIFIED | 328 lines, extends `ClickHouseBase`, 5 `@Test` methods, all REQ-ID `@DisplayName`s present, re-run green |
| `JsonInsertColumnsUnitTest.java` | Docker-free pins COLS-01 parsing contract | VERIFIED | 91 lines, extends nothing, 9 tests, re-run green (testUnit) |
| `ProxySinkTask.java` (main code) | `buildMBeanName` extraction, byte-for-byte preserved | VERIFIED | Diffed against pre-plan revision: only change is the extraction; null/colon logic and format template moved verbatim; format template appears exactly once |
| `MBeanNameUnitTest.java` | Docker-free pins JMX-01/02/03 | VERIFIED | 74 lines, extends nothing, 5 tests, spot-run green |
| `MBeanRegistrationLiveTest.java` | ClickHouse-backed pin JMX-04 | VERIFIED | 177 lines, extends `ClickHouseBase`, 2 tests, no `SinkTask[0-9]` literal, re-run green |
| `AsyncInsertSettingsUnitTest.java` | Docker-free pins ASYNC-01/02 | VERIFIED | 71 lines, extends nothing, 3 tests, re-run green (testUnit) |
| `AsyncInsertLiveTest.java` | ClickHouse-backed pin ASYNC-03 | VERIFIED | 95 lines, extends `ClickHouseBase`, 1 test, no sleep/retry construct, re-run green |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `MetadataFreeJsonInsertUnitTest` | `ClickHouseWriter` | protected `setSinkConfig` seam + `cleanupExtraFields` on a bare `new ClickHouseWriter()` | WIRED | `writer.setSinkConfig(` and `cleanupExtraFields(` both present ≥2x; test asserts behavior differs by config |
| `ExplicitJsonInsertPathLiveTest` | `ClickHouseWriter` | `ClickHouseSinkTask.start(props)` with `insertFormat=json` drives `shouldSkipTableMetadata()`/`getTable()` | WIRED | `INSERT_FORMAT` set, live task started, typed read-back proves the metadata-free path executed end to end |
| `ClickHouseSinkTaskJsonInsertFormatTest` | `JsonDynamicRoundTripTest`'s `allRows` pattern | typed read-back helper copied in | WIRED | `GenericRecord` Javadoc present, helper used in both edited tests |
| `JsonInsertColumnsUnitTest` | `ClickHouseSinkConfig` | direct construction from `Map<String,String>`, asserts `getJsonInsertColumns()` | WIRED | `getJsonInsertColumns()` called across all 9 parsing tests |
| `JsonInsertColumnsLiveTest` | `ClickHouseWriter` | `ClickHouseSinkTask.put` with schemaless Map records drives `doInsertJsonV2`'s 5-arg `client.insert(table, jsonInsertColumns, ...)` | WIRED | `JSON_INSERT_COLUMNS` set, live inserts confirmed via typed read-back distinguishing listed vs. DEFAULT columns |
| `MBeanNameUnitTest` | `ProxySinkTask.buildMBeanName` | same-package test calls package-private static directly | WIRED | `ProxySinkTask.buildMBeanName(` called in every test method |
| `MBeanRegistrationLiveTest` | `MBeanServerUtils` | queries same `ManagementFactory.getPlatformMBeanServer()` instance `registerMBean`/`unregisterMBean` act on | WIRED | `getPlatformMBeanServer()` present; live register/unregister confirmed by fresh run |
| `AsyncInsertSettingsUnitTest` | `ClickHouseSinkConfig` | asserts on `getClickhouseSettings()` | WIRED | `getClickhouseSettings()` called in all 3 tests |

### Behavioral Spot-Checks (Fresh Execution, Not Trusted From SUMMARY)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All 4 Docker-free (testUnit) classes this phase added | `./gradlew testUnit --rerun --tests MBeanNameUnitTest --tests AsyncInsertSettingsUnitTest --tests JsonInsertColumnsUnitTest --tests MetadataFreeJsonInsertUnitTest` | 27 tests, 27 succeeded, 0 failed | PASS |
| All 5 live (Testcontainers) classes this phase added/edited | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests ExplicitJsonInsertPathLiveTest --tests JsonInsertColumnsLiveTest --tests MBeanRegistrationLiveTest --tests AsyncInsertLiveTest --tests ClickHouseSinkTaskJsonInsertFormatTest` | 16 tests, 16 succeeded, 0 failed | PASS |
| `ProxySinkTask.java` production diff is exactly the pre-approved extraction | `git diff 40c0520..HEAD -- src/main/` | Only `ProxySinkTask.java` changed, +12/-2, extraction is byte-for-byte behavior preserving | PASS |
| No uncommitted changes under `src/` or `build.gradle.kts` | `git status --porcelain -- src/ build.gradle.kts` | empty | PASS |

Note: I did not re-run the full `./gradlew test` suite (all 43 classes / 371 tests) — the orchestrator's
already-established post-merge gate (exit 0, 371 tests, 0 failures, 0 errors, 7 skipped) is accepted
per the verification brief. Instead I independently re-ran every test class this phase created or
modified (9 classes, 43 individual test methods across the two runs above), fresh, and all passed with
zero failures — this is stronger evidence than trusting the SUMMARY narrative alone.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| JSON-01 | 04-01 | insertFormat=json (any case) enables metadata-free; unset does not | SATISFIED | MetadataFreeJsonInsertUnitTest + ExplicitJsonInsertPathLiveTest, re-run green |
| JSON-02 | 04-01 | Metadata-free table resolution via topic-to-table map, no fetched metadata | SATISFIED | ExplicitJsonInsertPathLiveTest JSON-02, re-run green |
| JSON-03 | 04-01 | Unknown fields sent unmodified, known fields land correctly | SATISFIED | MetadataFreeJsonInsertUnitTest + ExplicitJsonInsertPathLiveTest JSON-03, re-run green |
| JSON-04 | 04-01 | Tombstones skipped without error | SATISFIED | ExplicitJsonInsertPathLiveTest JSON-04 (mixed + all-tombstone), re-run green |
| JSON-05 | 04-01 | Existing explicit-JSON tests assert column values | SATISFIED | ClickHouseSinkTaskJsonInsertFormatTest, re-run green |
| COLS-01 | 04-02 | jsonInsertColumns parsing contract | SATISFIED | JsonInsertColumnsUnitTest, re-run green (9/9) |
| COLS-02 | 04-02 | Default empty list correct inserts | SATISFIED | JsonInsertColumnsLiveTest COLS-02, re-run green |
| COLS-03 | 04-02 | Explicit subset fills listed, DEFAULT on unlisted | SATISFIED | JsonInsertColumnsLiveTest COLS-03, re-run green |
| COLS-04 | 04-02 | Extra JSON keys don't break insert | SATISFIED | JsonInsertColumnsLiveTest COLS-04, re-run green |
| COLS-05 | 04-02 | Nonexistent column: clear error, no partial insert, no NPE | SATISFIED | JsonInsertColumnsLiveTest COLS-05, re-run green |
| COLS-06 | 04-02 | List applies on non-explicit JSON path | SATISFIED | JsonInsertColumnsLiveTest COLS-06, re-run green |
| JMX-01 | 04-03 | connector=<name> when name prop set | SATISFIED | MBeanNameUnitTest, spot-run green |
| JMX-02 | 04-03 | connector=null without name prop | SATISFIED | MBeanNameUnitTest, spot-run green |
| JMX-03 | 04-03 | version=unknown for null; `:`→`_` | SATISFIED | MBeanNameUnitTest source-verified + testUnit gate green |
| JMX-04 | 04-03 | Distinct MBeans per task/connector; stop() unregisters own | SATISFIED | MBeanRegistrationLiveTest, re-run green |
| ASYNC-01 | 04-03 | Default config sends no async_insert | SATISFIED | AsyncInsertSettingsUnitTest, re-run green |
| ASYNC-02 | 04-03 | User-supplied async_insert passed through unchanged | SATISFIED | AsyncInsertSettingsUnitTest, re-run green |
| ASYNC-03 | 04-03 | Sink insert with async_insert=1 completes, data present | SATISFIED | AsyncInsertLiveTest, re-run green |

**Orphaned requirements:** None. All 18 IDs cross-referenced against `.planning/REQUIREMENTS.md` —
every ID declared in the three plans' frontmatter appears in REQUIREMENTS.md mapped to "Phase 4 /
Complete", and no additional Phase-4-mapped ID exists in REQUIREMENTS.md that isn't claimed by a plan.

### Anti-Patterns Found

No `TBD`, `FIXME`, `XXX`, `TODO`, `HACK`, `@Disabled`, or placeholder markers found in any of the 9
phase-4 test files or `ProxySinkTask.java` (checked via grep across all files).

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `ClickHouseSinkTaskJsonInsertFormatTest.java` | 73-91, 108-122 (per 04-REVIEW.md WR-01) | Missing `try`/`finally` around task lifecycle in 2 of the phase's 9 test files, unlike every sibling live test | ⚠️ Warning (pre-existing, already documented in committed 04-REVIEW.md) | Mitigated by `ClickHouseBase`'s `@AfterEach` MBean sweep; does not affect assertion correctness or the JSON-05 must-have; not a new finding — carried forward from the phase's own committed code review, noted here for completeness rather than treated as a fresh gap |
| `AsyncInsertLiveTest.java` | 20-22 (per 04-REVIEW.md IN-01) | Class Javadoc references a race (`async_insert=1` alone) the class does not itself test | ℹ️ Info (pre-existing, documented) | Documentation clarity only; the one test present (ASYNC-03) is correctly named and asserts what it claims |

Both findings above were already surfaced and classified by the phase's own committed code review
(`04-REVIEW.md`, status `issues_found`, 0 Critical / 1 Warning / 1 Info) and are not new. Neither
touches a must-have truth, a required artifact, or a key link — both are quality nits on otherwise
substantive, passing tests. No override is needed since neither blocks goal achievement.

### Prohibitions Reviewed (Judgment-Tier, Non-Authoritative LLM Review)

All prohibitions declared across the three plans' `must_haves.prohibitions` blocks were reviewed
against the actual diff and test sources:

- "MUST NOT weaken/@Disabled/delete/narrow scope to turn a red pin green" — HOLDS. No `@Disabled`
  anywhere in scope; the R-01 finding was pinned, not hidden or weakened.
- "MUST NOT assert only `countRows` where the requirement is about column values" — HOLDS. Every
  live test in scope pairs `countRows` (where used) with a typed `allRows` read-back; `countRows`
  appears exactly once in `JsonInsertColumnsLiveTest` (the COLS-05 no-partial-insert check, which is
  legitimately a count-only assertion).
- "MUST NOT encode R-01 as expected-and-acceptable without surfacing to the developer" — HOLDS. R-01
  was escalated at a `checkpoint:human-verify` gate and the pin explicitly carries a "NOT AN
  ENDORSEMENT" Javadoc naming the checkpoint.
- "MUST NOT silently normalize/validate/quote/dedupe `jsonInsertColumns` in main code" — HOLDS.
  `ClickHouseSinkConfig.java` blob hash unchanged for the whole phase (confirmed via `git diff --stat`
  showing only `ProxySinkTask.java` touched under `src/main/`).
- "MUST NOT change the rendered MBean name while extracting `buildMBeanName`" — HOLDS. Line-by-line
  diff confirms the null/colon logic and format template moved verbatim with no wording change.

This review is an LLM judgment call over static evidence (diffs, source text), not a formally proven
guarantee — flagged here as non-authoritative per the verification protocol, but no violation was
found and none is suspected.

## Gaps Summary

None. All 5 roadmap success criteria are verified with fresh, independently re-run passing tests
(not trusted from SUMMARY claims alone). All 18 requirement IDs are satisfied and cross-referenced
cleanly against REQUIREMENTS.md with no orphans. The one known deviation (R-01 silent-data-loss pin)
is documented, escalated, and accepted per explicit user decision — not a gap. The phase's only
production change (`ProxySinkTask.buildMBeanName` extraction) is confirmed behavior-preserving by
independent line-by-line diff review, not merely by trusting the code review's claim. No human
verification items were identified — this is a backend Kafka Connect/JDBC connector project with no
UI, and every asserted behavior is exercised by an automated, freshly-re-run test.

---

*Verified: 2026-09-18T00:20:00Z*
*Verifier: Claude (gsd-verifier)*
