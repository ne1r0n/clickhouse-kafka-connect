---
phase: 07-type-mapping-writer-helper-client-config
plan: 04
subsystem: database
tags: [clickhouse, kafka-connect, config, testUnit, merge-checklist, merge-reexpression]

# Dependency graph
requires:
  - phase: 07-type-mapping-writer-helper-client-config
    provides: >-
      Plans 07-01 through 07-03's re-expressed type mapping, RowBinary
      fail-fast, targetTableFilter/describe-skip, config keys, and the
      Phase 10 test carve-out that made src/test compile against upstream's
      rewritten Table/ClickHouseWriter/ClickHouseHelperClient APIs.
provides:
  - "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit green with Docker stopped - the phase's acceptance gate - 300 tests, 0 failures, 6 skipped (4 in the Docker-free @Disabled JSONP-01 class, 2 pre-existing unrelated performance-test skips)"
  - "NewUpstreamConfigDefaultsUnitTest.java pinning all thirteen new upstream ClickHouseSinkConfig options at their upstream defaults, extends nothing, stays in testUnit"
  - "MERGE-CHECKLIST.md's twelve Phase-7-owned Section 1 rows flipped to done; the two foreign rows (ClickHouseSinkTask.java/Phase 8, ClickHouseBase.java/Phase 10) annotated but left pending"
  - "Three new MERGE-CHECKLIST.md Recorded Facts: the connect-api 4.1.1 onPartitionsRevoked gap (D-04), the map null-marker free win with WR-01 stated open (FREE-02/WRT-04), the describe-table database free win (FREE-03)"
  - "A user-approved build.gradle.kts test-compile exclusion for two future-phase test files (MBeanNameUnitTest.java/Phase 8, ClickHouseTestHarnessTest.java/Phase 10), both left byte-for-byte unchanged on disk"
affects: [08-orchestration-jmx, 09-json-path-decision, 10-harness-carveout, 11-live-verification]

# Actuals (#2632)
actuals:
  tokens: 6578
  tasks: 2
  commits: 3
  plan_head_before: f6a2d0f4

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Naming a Docker-free config-pin test's target key constants inline as containsKey(...) assertions doubles as both the 'key cannot be silently omitted' requirement and the 'edge case: key absent from props map' requirement in one line per option"
    - "A build.gradle.kts sourceSets[\"test\"].java.exclude(...) line, scoped to the test *compile* source set only, is the mechanical shape for carrying a not-yet-landed cross-phase API dependency without touching or weakening the test file that needs it"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
    - build.gradle.kts
    - .planning/MERGE-CHECKLIST.md

key-decisions:
  - "User-approved scope addition: excluded MBeanNameUnitTest.java (needs Phase 8's ProxySinkTask.buildMBeanName) and ClickHouseTestHarnessTest.java (needs Phase 10's shared-container contract) from the test *compile* source set in build.gradle.kts, so compileTestJava - and therefore testUnit - could succeed without weakening either file. Both stay on disk byte-for-byte unchanged; each owning phase removes its own exclude line when it lands the missing API."
  - "[Rule 1 - Bug] Fixed a test-construction bug in RowBinaryUnsupportedTypeGateTest's JSON validateDataSchema case: it built a Record with an empty fields list while populating jsonMap directly, so ClickHouseWriter.validateDataSchema's default: branch NPE'd calling Schema#name() on the resulting null lookup. The sibling DYNAMIC/UNKNOWN test never hit this line because its switch case returns earlier. Fixed by adding the matching Field entry the jsonMap key represents, mirroring how Record.convert() would populate it - no src/main change, no assertion weakened."
  - "PD-02 honored as specified: errors.retry.timeout has no getter and no configDef.define entry, so the defaults test covers it via its key-constant string plus a no-throw construction assertion, not a fabricated getter."
  - "Stopped colima's running 'vz' profile before running testUnit (the acceptance gate requires Docker stopped, but colima vz was actually running under the environment's default docker context despite `colima status` on the default profile reporting 'not running'), then restarted it afterward to leave the environment as found."

patterns-established: []

requirements-completed: [CFG-03, WRT-04, FREE-02, FREE-03]

coverage:
  - id: D1
    description: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit is green with Docker genuinely stopped (colima vz stopped for the run) - 300 tests, 294 succeeded, 0 failed, 6 skipped; every fork canary (JsonDynamicTypeMappingTest, RowBinaryUnsupportedTypeGateTest, ClickHouseHelperClientForkUnitTest, JsonInsertColumnsUnitTest, AsyncInsertSettingsUnitTest, NewUpstreamConfigDefaultsUnitTest) executed with zero failures"
    requirement: "CFG-03"
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit (BUILD SUCCESSFUL); per-class XML result files in build/test-results/testUnit/ for all six named canaries, each failures=\"0\""
        status: pass
    human_judgment: false
  - id: D2
    description: "All twelve getter-backed new upstream config options (auto.evolve family, bufferCount, bufferFlushTime, clickhouseClientInsertTimeoutMs, clusterName, debeziumCDCEnabled, enableReplicaPinning, reportInsertedOffsets, retryOnSocketException, ssl_socket_sni) resolve to upstream's default when unset; errors.retry.timeout (no getter, no configDef entry, PD-02) is covered by its key-constant string and a no-throw construction"
    requirement: "CFG-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java (3 tests, 0 failures in testUnit)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Upstream's Map(K, Nullable(V)) RowBinary null-marker fix is confirmed present in the merged tree with its extra getMapValueType() != null guard the fork's own patch lacked; the fork's duplicate patch is confirmed dropped by the mechanical merge; WR-01's null-entry end-to-end coverage status is stated explicitly as still open, not closed by this merge"
    requirement: "WRT-04, FREE-02"
    verification:
      - kind: other
        ref: "MERGE-CHECKLIST.md Recorded Fact #9 (verbatim source quote of ClickHouseWriter.java:582, plus a read of SmtSinkLandingTest.java's HDRS-09 and transforms/HeadersToValueTest.java's HDRS-03 establishing the null-entry path is exercised only at the SMT/transform layer, never through ClickHouseWriter's RowBinary write)"
        status: pass
    human_judgment: true
    rationale: "This is a recorded fact from reading the merged source and existing test coverage, not a new automated test - the underlying claim (WR-01 stays open) is a negative (absence of coverage), which a human should spot-check against the cited test files rather than trust a pass/fail signal for."
  - id: D4
    description: "Whether upstream's describeTableV2 database-parameter fix subsumes the fork's dormant P3-WR-02 follow-up is checked against the merged source and recorded: it does, and nothing needed fixing"
    requirement: "FREE-03"
    verification:
      - kind: other
        ref: "MERGE-CHECKLIST.md Recorded Fact #10 (verbatim source quote of ClickHouseHelperClient.java:510, confirming the query builds from the method's own database parameter, not this.database)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Twelve MERGE-CHECKLIST.md Section 1 rows owned by Phase 7 flipped to done; the two foreign rows (ClickHouseSinkTask.java/Phase 8, ClickHouseBase.java/Phase 10) annotated with the commit that landed their early sliver, left pending and unowned by Phase 7; no row owned by Phase 8/9/10/11 reads done"
    requirement: "CFG-03"
    verification:
      - kind: other
        ref: "07-04-PLAN.md Task 2's four automated verify blocks (PHASE7_ROWS_FLIPPED, FOREIGN_ROWS_INTACT, STRUCTURE_AND_FACTS_OK, FACTS_EVIDENCED_AND_SCOPE_CLEAN), all re-run and passing against commit 89b646ec"
        status: pass
    human_judgment: false

duration: 22min
completed: 2026-09-19
status: complete
---

# Phase 7 Plan 4: Config Defaults Pin, testUnit Acceptance Gate, and Checklist Close-Out Summary

**Pinned all thirteen new upstream config options at their defaults with a new Docker-free test, fixed a test-construction NPE the acceptance-gate run surfaced, landed a user-approved build.gradle.kts test-compile carve-out for two future-phase files, and ran `testUnit` green with Docker genuinely stopped - then flipped Phase 7's twelve merge-checklist rows and recorded the two free wins.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-09-19T02:52:00Z (approx, first read)
- **Completed:** 2026-09-19T00:13:04Z
- **Tasks:** 2
- **Files modified:** 4 (1 created, 3 modified)

## Accomplishments

- **`NewUpstreamConfigDefaultsUnitTest.java`** created in `com.clickhouse.kafka.connect.sink`, extending nothing (stays in `testUnit`). Three tests: twelve getter-backed defaults asserted against upstream's default constants where they exist (never re-typed literals), `errors.retry.timeout`'s key-constant-and-no-throw coverage per PD-02, and an ordering-edge test confirming construction raises no `ConfigException` (the two `configDef.define` blocks this phase added did not collide with upstream's existing definitions).
- **User-approved scope addition:** `build.gradle.kts` gained a `sourceSets["test"].java.exclude(...)` block removing `MBeanNameUnitTest.java` (Phase 8's `ProxySinkTask.buildMBeanName`) and `ClickHouseTestHarnessTest.java` (Phase 10's shared-container contract) from the test *compile* source set only. Both files are untouched on disk (confirmed via `git status --short`), unblocking `compileTestJava` for the first time since the Phase 6 merge. Committed separately (`b25a71f1`) before the tasks that depend on it, per the user's explicit direction.
- **`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` is green with Docker stopped.** Discovered mid-run that the environment's `colima status` (default profile) reporting "not running" was misleading - a separate `vz` profile was actually running and reachable via `docker info`. Stopped `colima vz` explicitly, confirmed `docker info` unreachable, then ran the gate: `BUILD SUCCESSFUL`, 300 tests total across all XML result files, 0 failures, 6 skipped (4 from the Docker-free `@Disabled` JSONP-01 class, 2 pre-existing unrelated `ConvertToMapOrJsonPerformanceTestJson` skips). Restarted `colima vz` afterward to restore the environment.
- **[Rule 1 - Bug] Fixed a test-construction NPE the gate run surfaced** in `RowBinaryUnsupportedTypeGateTest`'s JSON `validateDataSchema` case (owned by plan 07-03). See Deviations below.
- **`MERGE-CHECKLIST.md`** updated: twelve Phase-7-owned Section 1 rows flipped `pending` → `done`; the two foreign rows (`ClickHouseSinkTask.java`, `ClickHouseBase.java`) got an appended note naming the commit that landed their early sliver, with status and owning phase left untouched; three new numbered `## Recorded Facts` entries added (continuing from 7 to 10).

## Task Commits

Each task was committed atomically, plus one user-approved pre-task commit:

1. **Pre-task: user-approved build.gradle.kts test-compile exclusion** - `b25a71f1` (chore) — `build.gradle.kts` only.
2. **Task 1: defaults test + gate fix** - `272542e5` (test) — `NewUpstreamConfigDefaultsUnitTest.java` (new) + `RowBinaryUnsupportedTypeGateTest.java` (Rule 1 fix).
3. **Task 2: checklist flips + recorded facts** - `89b646ec` (docs) — `.planning/MERGE-CHECKLIST.md` only.

**Plan metadata commit** (this SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md) follows this write per the executor's `git_commit_metadata` step.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java` - New. Pins all thirteen new upstream config options (CFG-03).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` - Added a `Field` entry matching the JSON test's `jsonMap` key, fixing an NPE in `validateDataSchema`'s `default:` branch (Rule 1).
- `build.gradle.kts` - Added a `sourceSets["test"].java.exclude(...)` block for two future-phase test files (user-approved scope addition).
- `.planning/MERGE-CHECKLIST.md` - Twelve Phase-7 rows flipped to done; two foreign rows annotated; three new Recorded Facts.

## Decisions Made

See `key-decisions` in frontmatter. Most notably: the user-approved `build.gradle.kts` exclusion (explicit direction, not an auto-fix), the Rule 1 NPE fix in `RowBinaryUnsupportedTypeGateTest.java`, and stopping/restarting `colima vz` to genuinely prove the Docker-free gate.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `RowBinaryUnsupportedTypeGateTest`'s JSON `validateDataSchema` test threw an NPE instead of exercising the intended behaviour**
- **Found during:** Task 1, first `testUnit` run after landing the defaults test and the build.gradle.kts carve-out
- **Issue:** `validateDataSchemaReturnsFalseForJsonColumnUnderDefaultConfigAndNeverLeaksTheValue` (authored by plan 07-03) built its `Record` with `Collections.emptyList()` for the `fields` parameter while putting data directly into `jsonMap`. `ClickHouseWriter.validateDataSchema`'s `default:` branch (upstream code, unmodified by this phase) calls `objSchema.name()` unconditionally in a debug log statement, where `objSchema` comes from a lookup keyed by `record.getFields()` - with an empty fields list, that lookup is always `null`, producing `NullPointerException: Cannot invoke "org.apache.kafka.connect.data.Schema.name()" because "objSchema" is null` at `ClickHouseWriter.java:327`. The sibling `DYNAMIC`/`UNKNOWN` test in the same file uses the identical empty-fields pattern but never reaches this line, because its switch case returns earlier - so the bug was latent until this specific test's code path (`colTypeName="JSON"` falling to `default:`) was exercised.
- **Fix:** Added a matching `org.apache.kafka.connect.data.Field` entry to the `fields` list (`new Field("json_col", 0, Schema.STRING_SCHEMA)`), mirroring exactly what `Record.convert()` would populate for a real Struct-schema record with that field - the established pattern already used by `ClickHouseWriterColumnContextTest.java`'s passing `validateDataSchema` tests. This is a test-construction fix, not a source fix: the plan's own prohibition ("MUST NOT change any file under `src/main/` in this plan") forecloses fixing the source's unguarded `objSchema.name()` call, and the test's own inconsistency (populating `jsonMap` without a matching `fields` entry) was the actual root cause, not a genuine reachable production bug - `Record.convert()` always builds both from the same source, so `objSchema` being null while `jsonMap` holds the key is not a state a real converted record can be in.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java`
- **Verification:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "com.clickhouse.kafka.connect.sink.db.RowBinaryUnsupportedTypeGateTest"` - 6 tests, 6 succeeded, 0 failed. Full `testUnit` re-run afterward: `BUILD SUCCESSFUL`, 0 failures.
- **Committed in:** `272542e5` (Task 1 commit, alongside the new defaults test)

---

**Total deviations:** 1 auto-fixed (1 bug), plus 1 user-approved scope addition (the `build.gradle.kts` exclusion, directed explicitly by the user before this plan's dispatch, not a deviation this executor decided on its own).
**Impact on plan:** The bug fix is confined to test-file construction, touches no `src/main` file, and does not weaken any assertion - the test now proves what it always intended to prove. No scope creep.

## Known Verify-Tooling Mismatch (not a deviation from behavior)

Task 1's `GATE_COMPOSITION_OK` automated verify block asserts that `build/test-results/testUnit/*.xml` contains the string `Pending JSONP-01 keep/drop decision` in exactly 4 files. This cannot pass as literally written, for two independent, non-code reasons discovered while executing this task:

1. **Gradle's JUnit XML writer does not record `@Disabled`'s reason text.** The `<skipped/>` element Gradle emits for a disabled test carries no `message` attribute at all - confirmed by inspecting `build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.sink.db.MetadataFreeJsonInsertUnitTest.xml` directly (`<skipped/>`, no text). The reason string only ever exists in the Java source (`@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")`), never in the XML report.
2. **Three of the four disabled classes extend `ClickHouseBase`** (`ClickHouseSinkTaskJsonInsertFormatTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`), inheriting its `@Tag("clickhouse")`. `testUnit`'s `excludeTags("clickhouse")` filter excludes these classes from discovery *entirely* - they produce **no XML file at all** under `build/test-results/testUnit/`, not a skipped-with-zero-executed record. This is correct, expected behaviour (these three are genuine live/Docker-backed tests that need a real ClickHouse instance once Phase 9 removes their `@Disabled`), not a silent vanish - and this plan's own `<output>` instructions independently anticipated it ("the fork tests that left the Docker-free gate because the base class regained its ClickHouse tag").

Only `MetadataFreeJsonInsertUnitTest` (the sole Docker-free file among the four - it deliberately extends nothing) appears in `testUnit`'s own report: 4 tests, all skipped, 0 failures - confirmed via its XML (`tests="4" skipped="4" failures="0"`).

**Alternate verification performed in place of the unsatisfiable literal check:**
- `grep -rl '@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")' src/test` returns exactly the four expected files (confirmed this session).
- All four compile cleanly (`compileTestJava` green tree-wide, confirmed via the plan-level verification below).
- The one Docker-free class among them appears in `testUnit`'s report as fully skipped with zero failures.
- The other three are absent from `testUnit`'s report specifically because they inherit `ClickHouseBase`'s `clickhouse` tag - the correct, by-design reason, not an unrelated silent failure.

No code was changed to chase this check; the underlying requirement (four classes correctly disabled with the exact reason, none of them "quietly stopped running" for an unrelated cause) is satisfied and documented here for Phase 11's skip accounting.

## Issues Encountered

- **`colima status` (default profile) reported "colima is not running" while a Docker daemon was actually reachable.** Investigated with `colima list`: the `vz` profile (not `default`) was running, and `docker info` connected to it via `DOCKER_HOST`/context. Stopped `colima vz` explicitly before the gate run (confirmed `docker info` unreachable afterward), ran `testUnit`, then restarted `colima vz` to leave the environment as found. Not a deviation - an environment discovery step needed to satisfy the plan's own "Docker stopped" precondition for the acceptance gate.
- See "Known Verify-Tooling Mismatch" above for the `GATE_COMPOSITION_OK` discrepancy - resolved by substituting an equivalent, achievable verification rather than altering code or test behaviour.

## User Setup Required

None - no external service configuration required.

## CFG-03 Inventory Table

Each of the thirteen new upstream options, its default, and its consumer (the part a getter assertion alone cannot show, per D-15):

| Option key | Constant | Default | Consumer |
|---|---|---|---|
| `auto.evolve` | `AUTO_EVOLVE` | `false` (inline literal) | Not yet consumed anywhere in `src/main` past the constructor parse - reserved for a future auto-evolution feature, out of this milestone's scope |
| `auto.evolve.ddl.refresh.retries` | `AUTO_EVOLVE_DDL_REFRESH_RETRIES` | `3` (inline literal) | Same as above - parsed, not yet consumed |
| `auto.evolve.struct.to.json` | `AUTO_EVOLVE_STRUCT_TO_JSON` | `false` (inline literal) | Same as above - parsed, not yet consumed |
| `bufferCount` | `BUFFER_COUNT` | `bufferCountDefault` = `0` | `ProxySinkTask`'s internal buffering decision (buffering is disabled at `0`); validated against `exactlyOnce`/`ignorePartitionsWhenBatching` at startup per the SYNC-05 production-config audit (Phase 6) |
| `bufferFlushTime` | `BUFFER_FLUSH_TIME` | `bufferFlushTimeDefault` = `0L` | Paired with `bufferCount` for `ProxySinkTask`'s internal buffering flush interval |
| `clickhouseClientInsertTimeoutMs` | `CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS` | `clickhouseClientInsertTimeoutMsDefault` = 4 minutes in ms | `ClickHouseHelperClient`'s client-builder insert timeout, and cross-checked against `consumer.override.max.poll.interval.ms` in the constructor's own warning check |
| `clusterName` | `CLUSTER_NAME` | `""` | `ClickHouseHelperClient`'s cluster clause builder (`ON CLUSTER`), used for cluster-aware DDL/table-mapping operations |
| `debeziumCDCEnabled` | `DEBEZIUM_CDC_ENABLED` | `false` (inline literal) | Reserved for upstream's `DebeziumRecordConvertor` selection - a native upstream feature the fork does not yet route to |
| `enableReplicaPinning` | `ENABLE_REPLICA_PINNING` | `enableReplicaPinningDefault` = `false` | `ClickHouseWriter.start()`'s builder chain (`.enableReplicaPinning(...)`), controlling replica-tag HTTP header behaviour |
| `errors.retry.timeout` | `CONNECTOR_RETRY_TIMEOUT` | literal `60000`, read into a constructor local | **Neither getter-backed nor `configDef`-declared (PD-02).** Its only consumer is the constructor's own inline warning check (`retryTimeout < MINIMAL_RETRY_TIMEOUT_THR_WARN`) - no other code in `src/main` reads it |
| `reportInsertedOffsets` | `REPORT_INSERTED_OFFSETS` | `reportInsertedOffsetsDefault` = `false` | Gates whether `Processing`/`ProxySinkTask` reports inserted-offset metadata (a Debezium-CDC-adjacent feature) |
| `retryOnSocketException` | `RETRY_ON_SOCKET_EXCEPTION` | `false` (inline literal) | `Utils.handleException`'s retry classification for socket-level exceptions on the client-V2 path - deliberately left at its default per `REQUIREMENTS.md`'s Deferred Ideas (enabling it is feature work, out of scope) |
| `ssl_socket_sni` | `SSL_SOCKET_SNI` | `""` | `ClickHouseHelperClient`'s client-builder SNI setter, used only when set to a non-empty value for SSL SNI hostname override |

## Evidence Brief for Phase 9's Blocking Checkpoint (D-12)

- Upstream's JSON record-writing path (`ClickHouseWriter.doInsertJsonV2`) inserts via `client.insert(table.getName(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings)` at **`ClickHouseWriter.java:1329`** (line number has drifted from the plan's `:1307` estimate due to earlier lines this phase added; the call shape is confirmed unchanged) - the 4-argument `(tableName, dataWriter, format, settings)` overload.
- The `(table, columns, data, ...)`-shaped overload the fork's pre-merge `jsonInsertColumns` feature attached to is **not** the overload this call site uses - confirming JSONP-03's premise that `jsonInsertColumns` cannot simply be re-attached to the existing call.
- `jsonInsertColumns` must therefore be redesigned as a **field filter inside `doInsertJsonV2`** (or an equivalent JSON-serialization step upstream of the `client.insert(...)` call), not as an argument to the insert call itself.
- The four classes disabled pending Phase 9's decision, each carrying the exact reason string `"Pending JSONP-01 keep/drop decision — Phase 9"`:
  - `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java`
  - `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java`
  - `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java`
  - `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java`

Phase 9 opens on this evidence rather than a fresh investigation.

## Dropped-Changes List for Phase 11's SUITE-03 Record

Every fork change this phase retired in favour of upstream, with its reason:

| Fork change | Reason retired |
|---|---|
| The fork's JSON entry in the RowBinary fail-fast gate/message | Upstream's own flag-gated `case JSON:` write path and its matching `validateDataSchema` branch now own JSON exclusively, per the milestone's locked "RowBinary + JSON" decision (WRT-02 discretion) |
| The fork's duplicate `Map(K, Nullable(V))` null-marker patch | Already dropped by the mechanical merge (Phase 6); upstream's version carries a strictly better `getMapValueType() != null` guard (FREE-02/WRT-04) |
| The fork's `describeTableV1` UTF-8 `BufferedReader` rewrite | Dropped under D-17 (plan 07-02) - V1 is outside both the test matrix and production; re-expressing it would grow the delta on a path nobody runs |
| The fork's query-string-parameter `showTables`/`showTablesV1`/`showTablesV2` signatures | Re-expressed under D-19 (plan 07-02) to inject the filter clause inline into upstream's `(String database)`-signature methods instead, keeping upstream's method shape |
| The fork's `Table`-typed `shouldSkipDescribeRow` signature | Re-expressed under D-08 (plan 07-02) as `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)`, since upstream's `Table` is now immutable and unavailable mid-loop |

## Skip Accounting

- **Four classes disabled** pending Phase 9's JSONP-01 decision (listed above under the evidence brief), all carrying the identical reason string.
- **Of those four, only one (`MetadataFreeJsonInsertUnitTest`) is Docker-free** and appears inside `testUnit`'s own report as fully skipped (4 tests, 0 failures). The other three inherit `ClickHouseBase`'s `@Tag("clickhouse")` and are excluded from `testUnit`'s discovery entirely - they will appear in the full `./gradlew test` run (Phase 11) once un-disabled or run-and-skipped there.
- **Fork tests that left the Docker-free gate because `ClickHouseBase` regained its `clickhouse` tag** (D-06, plan 07-03): `ClickHouseWriterTest.java`, `ClickHouseHelperClientTest.java`, `TableTest.java` (all three extend `ClickHouseBase`, per `MERGE-CHECKLIST.md`'s Section 1 notes) - each is `done` in this phase's checklist rows (compiles and is byte-identical/re-expressed correctly), with live confirmation explicitly deferred to Phase 11's full `test` run.
- **Two pre-existing, unrelated skips** in `testUnit`'s run: `ConvertToMapOrJsonPerformanceTestJson`'s `testWideStructPerformance()` and `testDeepNestedStructPerformance()` - not part of this phase's scope, not JSONP-01-related.

## Requirement Ledger

All seventeen of Phase 7's requirement IDs, each with the test, source assertion, or recorded fact that discharges it:

| Requirement | Discharged by |
|---|---|
| MAP-01 | `JsonDynamicTypeMappingTest` (5 tests, 0 failures in `testUnit`) - `Type.DYNAMIC` dispatch |
| MAP-02 | `JsonDynamicTypeMappingTest` plus source confirmation that `Column.java`'s `SimpleAggregateFunction`/Avro/Protobuf handling is untouched |
| MAP-03 | `MERGE-CHECKLIST.md`'s `Table.java`/`TableTest.java` rows (done) - compile-verified this run; live confirmation deferred to Phase 11 (`TableTest` extends `ClickHouseBase`, tag-excluded from `testUnit`) |
| WRT-01 | `RowBinaryUnsupportedTypeGateTest` (6 tests, 0 failures in `testUnit`) - `doWriteColValue`'s `DataException` throw |
| WRT-02 | `RowBinaryUnsupportedTypeGateTest` - `validateDataSchema`'s `DYNAMIC`/`UNKNOWN` gate, `JSON` excluded |
| WRT-03 | `RowBinaryUnsupportedTypeGateTest` - upstream's JSON `RuntimeException` path asserted (this plan's Rule 1 fix made this test's JSON case pass for the first time) |
| WRT-04 | `MERGE-CHECKLIST.md` Recorded Fact #9 - map null-marker guard confirmed, WR-01 stated open |
| WRT-05 | `MERGE-CHECKLIST.md`'s `ClickHouseWriter.java` row (done) - `setTargetTableFilter` wiring confirmed by plan 07-02, compiled and gate-green this run |
| HLP-01 | `ClickHouseHelperClientForkUnitTest` (16 tests, 0 failures in `testUnit`) - `targetTableFilter`/`FILT-01..04` |
| HLP-02 | `ClickHouseHelperClientForkUnitTest` - `shouldSkipDescribeRow`/`DESC-01..04` |
| HLP-03 | `ClickHouseHelperClientForkUnitTest` - `numColumns` increment ordering |
| HLP-04 | `MERGE-CHECKLIST.md`'s `ClickHouseHelperClient.java` row (done) - UTF-8 reader confirmed by plan 07-02; live `TYPE-07` confirmation deferred to Phase 11 |
| CFG-01 | `JsonInsertColumnsUnitTest` (9 tests, 0 failures in `testUnit`) - `targetTableFilter`/`jsonInsertColumns` parsing contracts |
| CFG-02 | `AsyncInsertSettingsUnitTest` (3 tests, 0 failures in `testUnit`) - `async_insert` un-forcing |
| CFG-03 | `NewUpstreamConfigDefaultsUnitTest` (3 tests, 0 failures in `testUnit`, this plan) - see CFG-03 Inventory Table above |
| FREE-02 | `MERGE-CHECKLIST.md` Recorded Fact #9 (this plan) |
| FREE-03 | `MERGE-CHECKLIST.md` Recorded Fact #10 (this plan) |

## Next Phase Readiness

- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` is green with Docker stopped - Phase 7's acceptance gate, confirmed at self-check time below.
- All seventeen of Phase 7's requirements are discharged (13 already complete from plans 07-01..03; CFG-03/WRT-04/FREE-02/FREE-03 closed by this plan).
- `MERGE-CHECKLIST.md` correctly reflects Phase 7's twelve owned rows as done; Phase 8, 9, and 10 each have their evidence brief / dropped-changes context / annotated foreign row ready to read.
- **Phase 8** can proceed on `ClickHouseSinkTask.java`/`ProxySinkTask.java`'s JMX re-plumb; the one-line compile-unblock (`b419b778`, plan 07-01) is already landed and noted in the checklist.
- **Phase 9** opens on the evidence brief above (D-12) rather than re-investigating the `client.insert(...)` overload question.
- **Phase 10** can proceed on `ClickHouseBase`'s full harness contract; the `@Tag("clickhouse")`/`uniqueConnectorName` sliver (`e1a90309`, plan 07-03) is already landed and noted in the checklist. **Phase 10 also owns removing this plan's `ClickHouseTestHarnessTest.java` exclude line from `build.gradle.kts`** once it lands the shared-container contract that file needs.
- **Phase 8 also owns removing this plan's `MBeanNameUnitTest.java` exclude line from `build.gradle.kts`** once it lands `ProxySinkTask.buildMBeanName`.
- **Phase 11** has: the dropped-changes list above for its SUITE-03 record, the skip accounting above for its skip inventory, and the two `build.gradle.kts` exclude lines to confirm are both removed before the milestone's final full-suite run (or to flag if either owning phase has not yet landed by then).

## Self-Check: PASSED

- `src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java` - FOUND on disk.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` - FOUND on disk, modified.
- `build.gradle.kts` - FOUND on disk, modified.
- `.planning/MERGE-CHECKLIST.md` - FOUND on disk, modified.
- All three commits found in `git log`: `b25a71f1`, `272542e5`, `89b646ec`.
- `git rev-list --count f6a2d0f4..HEAD` (pre-metadata-commit) = 3, matching the 3 commits made this plan.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava testUnit` re-run at self-check time with Docker stopped: `BUILD SUCCESSFUL` (all tasks `UP-TO-DATE`, confirming no regression since the last full run).
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew spotlessCheck` re-run: fails tree-wide on the same 52+ pre-existing files documented by plans 07-01/07-02/07-03 as out of scope; neither file this plan touched (`NewUpstreamConfigDefaultsUnitTest.java`, `RowBinaryUnsupportedTypeGateTest.java`) appears in the violation list.
- No conflict markers (`<<<<<<<`) found in any of the three commits' added lines.
- `MBeanNameUnitTest.java` and `ClickHouseTestHarnessTest.java` confirmed byte-for-byte unchanged (`git status --short` shows no modification to either).
- Colima's `vz` profile confirmed restarted and running at session end, matching its state at session start.

---
*Phase: 07-type-mapping-writer-helper-client-config*
*Completed: 2026-09-19*
