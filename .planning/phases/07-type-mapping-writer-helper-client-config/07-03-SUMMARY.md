---
phase: 07-type-mapping-writer-helper-client-config
plan: 03
subsystem: database
tags: [clickhouse, kafka-connect, config, test-harness, merge-reexpression, rowbinary]

# Dependency graph
requires:
  - phase: 07-type-mapping-writer-helper-client-config
    provides: >-
      Plan 07-01's Type.DYNAMIC/RowBinary fail-fast and plan 07-02's
      targetTableFilter/shouldSkipDescribeRow(Map<String, Column>) signature,
      both re-expressed onto upstream v1.6.0's rewritten classes.
provides:
  - "ClickHouseSinkConfig.JSON_INSERT_COLUMNS key, jsonInsertColumns/jsonInsertFormatExplicit fields and their pre-merge parsing contracts (read by nothing in src/main yet — Phase 9's one-phase gap by design, D-13)"
  - "No async_insert setting forced on a default connector configuration (CFG-02)"
  - "compileTestJava green for every file this plan and its Rule-3 carve-out touched — the fork test tree compiles against upstream's rewritten Table/ClickHouseWriter/ClickHouseHelperClient APIs for the first time since the merge, EXCEPT two named files still blocked by Phase 8 and Phase 10 work (see Known Stubs / Next Phase Readiness — this is NOT full compileTestJava success)"
  - "ClickHouseBase carries @Tag(\"clickhouse\") and uniqueConnectorName(TestInfo) + its own counter — nothing else restored (D-06)"
  - "RowBinaryUnsupportedTypeGateTest asserts upstream's RuntimeException/false-return for JSON and the fork's DataException for DYNAMIC/UNKNOWN only (WRT-02/WRT-03)"
  - "Four JSON-path test classes compile and are @Disabled(\"Pending JSONP-01 keep/drop decision — Phase 9\")"
affects: [07-04-checklist-and-evidence-brief, 08-orchestration-jmx, 09-json-path-decision, 10-harness-carveout]

# Actuals (#2632)
actuals:
  tokens: 23630
  tasks: 2
  commits: 3
  plan_head_before: 5a89cfd7

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Redirect a dead pre-merge call site to the post-merge equivalent that already exists (getBaseProps() for createProps(), ClickHouseTestHelpers.createClient(Map) for the bare instance method, a local queryV2-based helper for the missing ClickHouseTestHelpers.createTable) rather than restoring the dead method itself — keeps every locked planning boundary (D-06, D-09) intact while still reaching the stated compile gate"
    - "Reflective private-field accessor for a writer needing a configured (but not started) ClickHouseSinkConfig in a Docker-free test, modelled on ClickHouseWriterTest's existing helper-client accessor (WRT-02/WRT-03, D-11)"
    - "catch (Exception e) { throw new RuntimeException(e); } around a try-with-resources Records query, matching ClickHouseTestHelpers.dropTable/createDatabase's own idiom — Records.close() throws checked Exception, so a bare try-with-resources inside a method with no throws clause fails to compile despite looking identical to already-working code elsewhere in the same file"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertFormatPerformanceTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java

key-decisions:
  - "Discovered a measurement gap in the phase's planning: D-05's blast-radius count (uniqueConnectorName, @Tag, one stale import, dead Table API in 4 files) did not include ClickHouseBase's pre-merge createProps()/createClient(Map) instance methods or ClickHouseTestHelpers' pre-merge createTable(...) static method, both of which the mechanical merge dropped and which twelve additional fork test files call. Verified via git show pre-v1.6.0-sync that createTable existed in the fork's ClickHouseTestHelpers.java pre-merge (returning OperationMetrics) and was lost when the merge resolved that file's modify/delete conflict to upstream's testFixtures copy (the same file STATE.md's Blockers section already flagged for HARN-04/Phase 10, but for a different reason — file de-duplication, not content completeness)."
  - "Resolved the gap via Rule 3 (blocking issue, auto-fix) rather than restoring the dead methods: every call site was redirected to a functionally-equivalent method that already exists post-merge — getBaseProps() (upstream's own rewritten equivalent of createProps(), already returning this class's per-class test database via the `database` instance field, not the fork's old hardcoded \"default\") and ClickHouseTestHelpers.createClient(Map) (already present, confirmed identical construction logic to the fork's dropped instance method). For createTable, no equivalent existed anywhere post-merge, so a local per-file `createTable(chc, tableName, ddlTemplate)` helper wrapping `chc.queryV2(String.format(ddlTemplate, tableName))` was added to each of the twelve files needing it — zero new public API surface on ClickHouseBase or ClickHouseTestHelpers, so D-06 (\"restore exactly two members and nothing else\") and D-09 (\"do not touch ClickHouseTestHelpers.java\") both hold exactly as locked."
  - "Two files were deliberately left uncompiled: MBeanNameUnitTest.java (needs ProxySinkTask.buildMBeanName, explicitly Phase 8's JMX re-plumb) and ClickHouseTestHarnessTest.java (needs ClickHouseBase's shared-container strategy plus a ClickHouseTestHelpers.query helper, explicitly Phase 10's harness work and the exact file that pins the Phase-10-owned harness contract by its own class javadoc). Fixing either would mean writing Phase 8 or Phase 10 production/harness code inside this plan, directly violating D-06's and the phase's own domain-boundary paragraph. This is a CRITICAL flag for plan 07-04, whose own must_haves.truths assumes a green `testUnit` run — see Next Phase Readiness."
  - "Did not run the tree-wide ./gradlew spotlessApply, continuing plans 07-01/07-02's documented decision (the tree has not been spotless-cleaned since the Phase 6 merge). Verified via spotlessJavaCheck that all seventeen touched files are independently style-compliant."
  - "Plan commit ledger (protocol 0c) was, again, not recorded before the first commit this session (same recurring gap noted in 07-01 and 07-02's summaries). Reconstructed retroactively: base is 5a89cfd7 (07-02's final metadata commit, HEAD at session start), confirmed via `git rev-list --count 5a89cfd7..HEAD` = 3, matching the 3 task commits made. Ledger file written retroactively at `.git/gsd-plan-head-before-07-03`."

patterns-established:
  - "Pattern: when a fork test file's pre-merge instance-method dependency (createProps/createClient) has an upstream-equivalent already present under a different name (getBaseProps) or a different qualifier (ClickHouseTestHelpers.createClient), redirect the call site rather than restoring the dead method — this is a special case of 'prefer upstream wherever it already does the job' that applies even to test-harness scaffolding, not just src/main."
  - "Pattern: any new try-with-resources block over `Records` (client-v2's query result type, whose close() throws checked Exception) inside a method with no `throws` clause needs an explicit catch-and-wrap, matching ClickHouseTestHelpers' own established idiom — do not assume a bare try-with-resources compiles just because visually-identical code compiles elsewhere; Gradle's incremental compiler does not always re-diagnose every affected file in the same pass, so this class of error can appear one file at a time across successive `compileTestJava` runs even when the underlying bug is present everywhere from the start."

requirements-completed: [CFG-01, CFG-02]

coverage:
  - id: D1
    description: "ClickHouseSinkConfig gains JSON_INSERT_COLUMNS/jsonInsertColumns/jsonInsertFormatExplicit with the exact pre-merge parsing contract (explicit flag reads the raw INSERT_FORMAT property, stays false when unset even though the enum resolves to JSON; column list splits on comma, trims, drops empty elements, preserves order)"
    requirement: "CFG-01"
    verification:
      - kind: other
        ref: "07-03-PLAN.md Task 1's two verify blocks (BUILD SUCCESSFUL, CFG01_REMAINDER_OK) re-run this session and passing"
        status: pass
    human_judgment: true
    rationale: "JsonInsertColumnsUnitTest and MetadataFreeJsonInsertUnitTest's JSON-01 cases are the behavioral canaries, but MetadataFreeJsonInsertUnitTest is now @Disabled (D-11) so its assertions do not execute in plan 07-04's testUnit run. JsonInsertColumnsUnitTest is not disabled and should execute; its six parse-contract cases prove this requirement behaviorally. This plan's own verification is grep/compile-based only."
  - id: D2
    description: "A default connector configuration sends no async_insert setting, while the two neighbouring forced settings (input_format_skip_unknown_fields, wait_end_of_query) are unchanged; an operator-supplied clickhouseSettings value still round-trips"
    requirement: "CFG-02"
    verification:
      - kind: other
        ref: "07-03-PLAN.md Task 1's ASYNC_INSERT_UNFORCED_OK verify block, re-run this session and passing"
        status: pass
    human_judgment: true
    rationale: "AsyncInsertSettingsUnitTest is the behavioral canary and is not disabled, so it should execute in plan 07-04's testUnit run. This plan's own verification is grep-based only (occurrence count, comment-prefix check)."
  - id: D3
    description: "The Phase 10 carve-out lands exactly as measured plus the Rule-3 mechanical fix: ClickHouseBase's two-member sliver, JsonColumnTest's import fix, ClickHouseHelperClientForkUnitTest's map-based rewrite, RowBinaryUnsupportedTypeGateTest's writer/Table/JSON-split rewrite, MetadataFreeJsonInsertUnitTest's compile-then-disable, three more JSON-path classes disabled, ClickHouseHelperClientForkLiveTest fixed per the compiler's own report, and twelve additional files' dead createProps/createClient/createTable call sites redirected to already-existing equivalents"
    requirement: "MAP-03, WRT-02, WRT-03"
    verification:
      - kind: other
        ref: "All five of 07-03-PLAN.md Task 2's automated verify blocks (BASE_SLIVER_AND_IMPORTS_OK, DEAD_API_CLEARED, TEST_REWRITES_OK, DISABLES_OK) re-run this session and passing; the sixth (BUILD SUCCESSFUL for the whole test source set) does NOT pass — see Known Stubs"
        status: fail
    human_judgment: true
    rationale: "compileTestJava for the WHOLE test source set does not succeed — MBeanNameUnitTest.java (Phase 8) and ClickHouseTestHarnessTest.java (Phase 10) remain uncompiled by design, per locked decisions this plan must not violate. Every file this plan's own declared scope and Rule-3 carve-out touched compiles cleanly and independently verified; the residual failure belongs to two named symbols this plan is explicitly prohibited from supplying. Plan 07-04's testUnit run will hit this same wall — flagged prominently below for a human/orchestrator decision this plan cannot make unilaterally."

duration: ~55min
completed: 2026-09-19
status: complete
---

# Phase 7 Plan 3: Config Layer and RowBinary+JSON Split, Plus a Larger-Than-Measured Test Carve-Out Summary

**Landed the remaining fork config settings (`jsonInsertColumns` key + explicit-format flag, `async_insert` un-forcing) and the planned four-item Phase 10 test carve-out, then discovered and mechanically fixed a much larger compile gap — twelve additional fork test files depending on pre-merge `ClickHouseBase`/`ClickHouseTestHelpers` methods the mechanical merge silently dropped — while deliberately leaving two files unfixed because completing them would require writing Phase 8's or Phase 10's own work inside this plan.**

## Performance

- **Duration:** ~55 min
- **Started:** 2026-09-19 (session start, continuing from 07-02)
- **Completed:** 2026-09-19
- **Tasks:** 2
- **Files modified:** 18 (1 src/main, 17 src/test)

## Accomplishments

- `ClickHouseSinkConfig` gained `JSON_INSERT_COLUMNS`, `jsonInsertColumns`/`jsonInsertFormatExplicit` fields (Lombok-generated accessors only), their pre-merge parsing contract, and a `configDef.define` block placed immediately after `INSERT_FORMAT`'s. The explicit-format flag reads the **raw** `INSERT_FORMAT` property (not the defaulted lowercase value already computed for the enum), so it stays `false` when the key is absent even though the enum itself resolves to `JSON` via upstream's `default:` branch typo.
- The forced `async_insert=0` ClickHouse setting is commented out, restoring the fork's pre-merge default of leaving `async_insert` to the server profile or an operator-supplied `clickhouseSettings` value. The two neighbouring forced settings are untouched.
- `ClickHouseBase` gained exactly the two members D-06 authorizes: `@Tag("clickhouse")` on the class declaration and `uniqueConnectorName(TestInfo)` backed by its own `AtomicLong` counter, ported verbatim from the fork's pre-merge shape. No MBean sweep, shared-container strategy, or database-name helper was restored.
- `JsonColumnTest`'s two stale `FromVersionConditionExtension`/`SinceClickHouseVersion` imports were retargeted from the pre-merge `com.clickhouse.kafka.connect.sink.junit.extension` path to the actual `com.clickhouse.kafka.connect.test.junit.extension` package under `src/testFixtures`.
- `ClickHouseHelperClientForkUnitTest`'s ten two-argument `Table` constructions (plus their `addColumn` calls) were rewritten to build `Map<String, Column>` locals directly, matching `shouldSkipDescribeRow`'s D-08 signature from plan 07-02. The "parent intentionally never added" case builds a genuinely empty map, not a placeholder entry.
- `RowBinaryUnsupportedTypeGateTest` was rewritten in full: all three `ClickHouseWriter` constructions now take a `SinkTaskStatistics`, the one `Table` construction uses the 5-argument immutable form, both parametrized `@EnumSource` lists dropped `JSON` (now `{"DYNAMIC", "UNKNOWN"}` only), and the single-case JSON test was replaced with two rewritten tests asserting upstream's behaviour under a default (flag-off) configuration: `doWriteColValue` throws a `RuntimeException` naming `input_format_binary_read_json_as_string`, and `validateDataSchema` returns `false` without throwing — both via a new reflective accessor for the writer's private `csc` field, modelled on `ClickHouseWriterTest`'s existing helper-client accessor. The distinctive-secret-value absence assertion was carried onto the rewritten JSON case, as the plan required.
- `MetadataFreeJsonInsertUnitTest` was made to compile (one-arg writer construction, 5-arg `Table`, a local reflective `setSinkConfig` accessor replacing the non-existent public setter it originally called) and annotated `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")`.
- `ClickHouseSinkTaskJsonInsertFormatTest`, `ExplicitJsonInsertPathLiveTest`, and `JsonInsertColumnsLiveTest` were made to compile and carry the same `@Disabled` annotation and reason string. `ExplicitJsonInsertPathLiveTest`'s R-01 labelled loss test is covered by the class-level annotation.
- `ClickHouseHelperClientForkLiveTest` was verified against the compiler per the plan's own "let compileTestJava decide" instruction; it needed the same `createProps`/`createClient`/`createTable` redirect as the other files below, plus two `List<Table>` declarations corrected to `List<Table.TableDesc>` and two `Table::getCleanName` method references corrected to `Table.TableDesc::getCleanName` (upstream's `showTables` now returns `List<Table.TableDesc>`, not `List<Table>`).
- **Discovered and fixed a measurement gap beyond the plan's declared scope (Rule 3):** twelve fork test files — `JsonColumnTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ClickHouseHelperClientForkLiveTest`, `AsyncInsertLiveTest`, `ClickHouseServerVersionSmokeTest`, `ClickHouseSinkTaskMappingTest`, `InsertFormatPerformanceTest`, `JsonDynamicRoundTripTest`, `MBeanRegistrationLiveTest`, `RowBinaryFailFastLiveTest`, `SmtSinkLandingTest` — called `ClickHouseBase`'s pre-merge `createProps()`/`createClient(Map)` instance methods or a `ClickHouseTestHelpers.createTable(ClickHouseHelperClient, String, String)` static method, none of which exist in the merged tree. Every call site was redirected to an already-existing equivalent: `getBaseProps()` for `createProps()`, `ClickHouseTestHelpers.createClient(Map)` for the bare instance call, and a new local `createTable(...)` helper (wrapping `chc.queryV2(String.format(ddl, tableName))`, catch-and-wrap per `ClickHouseTestHelpers`' own idiom) in each file that needed table creation. No new method was added to `ClickHouseBase` or `ClickHouseTestHelpers` — D-06 and D-09 both hold exactly as locked.

## Task Commits

Each task was committed atomically:

1. **Task 1: JSON insert-columns key and explicit-format flag** - `4d0ec809` (feat) — `ClickHouseSinkConfig.java`.
2. **Task 1: async_insert un-forcing** - `b4bae8cc` (fix) — `ClickHouseSinkConfig.java`.
3. **Task 2: test carve-out + Rule-3 mechanical fixes** - `e1a90309` (test) — 17 test files (the 8 the plan declared, plus the 9 additional files the Rule-3 fix required — `ClickHouseHelperClientForkLiveTest` was in the declared 8's verify-only scope; the other 8 — `AsyncInsertLiveTest`, `ClickHouseServerVersionSmokeTest`, `ClickHouseSinkTaskMappingTest`, `InsertFormatPerformanceTest`, `JsonDynamicRoundTripTest`, `MBeanRegistrationLiveTest`, `RowBinaryFailFastLiveTest`, `SmtSinkLandingTest` — were not in the plan's declared scope at all).

**Plan metadata commit** (this SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md) follows this write per the executor's `git_commit_metadata` step.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` - `JSON_INSERT_COLUMNS` key/fields/parse/configDef.define; `async_insert` forcing commented out (CFG-01, CFG-02).
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` - `@Tag("clickhouse")` + `uniqueConnectorName(TestInfo)` + counter (D-06).
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` - stale extension imports retargeted; `createProps()`/`createClient()`/`ClickHouseTestHelpers.createTable()` calls redirected (D-05, Rule 3).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` - writer/`Table` construction rewrites, JSON split, rewritten JSON test pair (MAP-03, WRT-02, WRT-03).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` - compile fixes + `@Disabled` (D-11).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - ten `Table`→`Map<String, Column>` rewrites (D-08).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` - `createProps`/`createClient`/`createTable` redirect, `List<Table>`→`List<Table.TableDesc>`, `Table::getCleanName`→`Table.TableDesc::getCleanName`.
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java`, `ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java` - compile fixes + `@Disabled` (D-11).
- `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java`, `ClickHouseServerVersionSmokeTest.java`, `ClickHouseSinkTaskMappingTest.java`, `InsertFormatPerformanceTest.java`, `JsonDynamicRoundTripTest.java`, `MBeanRegistrationLiveTest.java`, `RowBinaryFailFastLiveTest.java`, `SmtSinkLandingTest.java` - Rule-3 `createProps`/`createClient`/`createTable` redirects (not in the plan's declared scope; needed to reach the compile gate).

## Decisions Made

See `key-decisions` in frontmatter. Most notably: the D-05 blast-radius measurement from planning undercounted by twelve files, all fixed via Rule 3 without touching `ClickHouseBase` or `ClickHouseTestHelpers` beyond what was already locked; two files (`MBeanNameUnitTest.java`, `ClickHouseTestHarnessTest.java`) were deliberately left broken because fixing them requires Phase 8's or Phase 10's own work.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Twelve additional fork test files called dead `ClickHouseBase`/`ClickHouseTestHelpers` methods, blocking `compileTestJava` beyond the plan's measured four-item scope**
- **Found during:** Task 2, after landing the plan's declared four items and running `compileTestJava` to check the gate
- **Issue:** `ClickHouseBase.createProps()`, `ClickHouseBase.createClient(Map)`, and `ClickHouseTestHelpers.createTable(ClickHouseHelperClient, String, String)` were all present in the fork's pre-merge harness (confirmed via `git show pre-v1.6.0-sync`) but none survived the mechanical v1.6.0 merge. Twelve fork test files still called them, producing ~100+ "cannot find symbol" compile errors across the test source set — none of these files were named in `07-CONTEXT.md`'s D-05 blast-radius measurement or this plan's `files_modified`.
- **Fix:** Redirected every call site to an already-existing, functionally-equivalent method rather than restoring the dead ones: `createProps()` → `getBaseProps()` (upstream's own rewritten equivalent — confirmed to return the correct per-class test database via the `database` instance field, actually more correct than the fork's version, which one file's own comment noted returned the hardcoded `"default"` database and relied on `createClient`'s now-nonexistent mutation side effect); `createClient(props)` → `ClickHouseTestHelpers.createClient(props)` (already present, construction logic confirmed identical to the fork's dropped method); `ClickHouseTestHelpers.createTable(chc, tableName, ddl)` → a new local `createTable(...)` helper per file, wrapping `chc.queryV2(String.format(ddl, tableName))` in a `try (Records ignored = ...) { } catch (Exception e) { throw new RuntimeException(e); }` block matching `ClickHouseTestHelpers.dropTable`/`createDatabase`'s own established idiom (`Records.close()` throws checked `Exception`, so a bare try-with-resources inside a method with no `throws` clause does not compile).
- **Files modified:** `JsonColumnTest.java`, `ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `AsyncInsertLiveTest.java`, `ClickHouseServerVersionSmokeTest.java`, `ClickHouseSinkTaskMappingTest.java`, `InsertFormatPerformanceTest.java`, `JsonDynamicRoundTripTest.java`, `MBeanRegistrationLiveTest.java`, `RowBinaryFailFastLiveTest.java`, `SmtSinkLandingTest.java`.
- **Verification:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileTestJava` reduced from ~100+ errors (capped) to exactly 16 errors in exactly 2 files, both confirmed to need only Phase 8/10-owned symbols (see Known Stubs below). No new public method was added to `ClickHouseBase` or `ClickHouseTestHelpers`; grep confirms zero occurrences of `createProps(` or bare `createClient(` remaining anywhere in `src/test` outside `ClickHouseBase.java` itself, and zero occurrences of `ClickHouseTestHelpers.createTable(` anywhere in `src/test`.
- **Committed in:** `e1a90309` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking, spanning 13 files beyond the plan's declared 8)
**Impact on plan:** No behavioural change to any test's assertions — every redirect targets a method that already existed and does the same thing the dead method did (in `getBaseProps()`'s case, more correctly). No scope creep into Phase 8 or Phase 10's actual production/harness work: the two files that need that work were left unfixed. This is the largest deviation this phase has needed so far and is flagged prominently in Next Phase Readiness because it affects plan 07-04's own acceptance gate.

## Known Stubs

Not stubs in the traditional sense (no placeholder data or empty renders), but two test files remain uncompiled by design, which will surface as `compileTestJava`/`testUnit` failures until their owning phase lands:

- **`src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java`** — five `ProxySinkTask.buildMBeanName(String, int, String)` call sites (lines 27, 38, 46, 54, 62) reference a method that does not exist in the merged `ProxySinkTask.java`. `buildMBeanName` was fork-added functionality (previous milestone's Phase 4) that the mechanical v1.6.0 merge dropped along with the rest of `ProxySinkTask.java`'s JMX naming rewrite; re-adding it is explicitly Phase 8's scope ("the JMX `connector=<name>` re-plumb and everything else in `ProxySinkTask`/`SinkTaskStatistics`", 07-CONTEXT.md's Phase Boundary paragraph). This file deliberately does **not** extend `ClickHouseBase` (per its own class comment, to stay in the Docker-free `testUnit` gate), so it is not excluded by the `@Tag("clickhouse")` filter — its compile failure blocks `testUnit`, not just full `test`.
- **`src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java`** — needs `sharedContainerStarts()` (an instance/static counter proving the shared-container-per-JVM-fork invariant) and `ClickHouseTestHelpers.query(ClickHouseHelperClient, String)`, neither of which exists post-merge. This file's own class javadoc states it "Proves the `ClickHouseBase` test harness contract (INFRA-03..06, D-05, D-08, D-09, D-10, D-11)" — it is explicitly Phase 10's harness-contract test, and D-06 explicitly forbids this plan from restoring the shared-container strategy ("not the shared-container-per-JVM-fork strategy... All of those are Phase 10's"). This file **is** excluded from `testUnit` by the inherited `@Tag("clickhouse")`, but its compile failure still blocks `compileTestJava` for the whole source set, which every Gradle test task (including `testUnit`) depends on.

Neither file is in this plan's `files_modified`, and fixing either requires writing Phase 8's or Phase 10's own production/harness code — explicitly prohibited by D-06 and the phase's domain boundary. **Recorded here rather than fixed.**

## Threat Flags

None beyond what the plan's own threat model already covers (T-07-10 through T-07-14, T-07-SC) — no new network endpoint, auth path, file-access pattern, or schema change at a trust boundary was introduced. The reflective private-field accessors added to `RowBinaryUnsupportedTypeGateTest` and `MetadataFreeJsonInsertUnitTest` are confined to test sources and mirror `ClickHouseWriterTest`'s existing pattern (T-03-21/T-07-13 disposition already covers this shape).

## Issues Encountered

- **Gradle's incremental `compileTestJava` surfaced the `Records.close()` checked-exception issue one file at a time across successive runs**, even though the same bug (a bare try-with-resources over `chc.queryV2(...)` inside a method with no `throws` clause) was present in five files simultaneously from the moment they were edited. Fixing the first-reported file and recompiling revealed the identical bug in a different file, not a new bug — confirmed by running `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileTestJava` (forcing a full, non-incremental recompile) after applying the catch-and-wrap fix everywhere proactively, which produced a stable, complete error list. Not a deviation in the code sense — a note for future sessions that a single incremental `compileTestJava` run cannot be trusted to report every error in one pass when Gradle's incremental compiler is involved; use `clean compileTestJava` (or otherwise force a full recompile) before concluding a gate has "no more errors."
- See Known Stubs above for the two files that remain genuinely blocked by other phases' scope.

## User Setup Required

None - no external service configuration required.

## Evidence for Downstream Plans

**For plan 07-04, Phase 8, Phase 9, and Phase 10:**

- **Three commit SHAs, this plan's units of work:**
  - `4d0ec809` - JSON insert-columns key + explicit-format flag (CFG-01 remainder).
  - `b4bae8cc` - async_insert un-forcing (CFG-02).
  - `e1a90309` - Phase 10 test carve-out (the plan's declared four items) + the Rule-3 mechanical fix across thirteen files total.

- **`ClickHouseBase`'s restored members, verbatim enough to append as a `MERGE-CHECKLIST.md` note (D-07 — Phase 10 still owns flipping that row):** `@Tag("clickhouse")` on the class declaration, and:
  ```java
  private static final AtomicLong CONNECTOR_NAME_COUNTER = new AtomicLong();
  // ...
  protected String uniqueConnectorName(TestInfo testInfo) {
      String methodName = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
      return getClass().getSimpleName() + "-" + methodName + "-" + CONNECTOR_NAME_COUNTER.incrementAndGet();
  }
  ```
  Both landed in commit `e1a90309`.

- **The four classes now `@Disabled` and the exact reason string** (Phase 9 can find them by `grep -rl '@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")' src/test`):
  `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java`, `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java`, `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java`, `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java`.

- **Old and new expectations of the rewritten JSON case in `RowBinaryUnsupportedTypeGateTest`, for Phase 11's dropped-changes record:**
  - **Old (fork, removed):** a single-case `@Test` asserting a `DataException` containing `insertFormat=json`, `bypassRowBinary=true`, and the column name, with zero bytes written to the stream — the fork's own RowBinary-JSON fail-fast, sharing the same message shape as the DYNAMIC/UNKNOWN gate.
  - **New (upstream, adopted per WRT-02's locked discretion):** two tests. `doWriteColValueThrowsUpstreamRuntimeExceptionForJsonColumnUnderDefaultConfig` asserts a `RuntimeException` containing `"Writing JSON in binary is not supported yet"` and `"input_format_binary_read_json_as_string"`. `validateDataSchemaReturnsFalseForJsonColumnUnderDefaultConfigAndNeverLeaksTheValue` asserts `validateDataSchema` returns `false` (no throw) and that the message-absence assertion pattern (though there is no exception message here, the returned boolean is the only observable) still proves no exception fires. Both use a `JSON` column under a default (flag-off) `ClickHouseSinkConfig` built via `new ClickHouseSinkConfig(new HashMap<>())` and set onto the writer's private `csc` field reflectively.
  - `Type.DYNAMIC` and `Type.UNKNOWN` retain the fork's original `DataException` gate, unchanged in message shape, in both `doWriteColValueThrowsForEveryUnsupportedType` and `validateDataSchemaThrowsForEveryUnsupportedTypeAndNeverLeaksTheValue`.

- **`ClickHouseHelperClientForkLiveTest` needed edits** — yes, contrary to the plan's "verify, do not rewrite (planning-time grep found no dead call site)" expectation. The compiler named: `createProps()`/`createClient(...)` (9 call sites), `ClickHouseTestHelpers.createTable(...)` (11 call sites), two `List<Table>` declarations needing `List<Table.TableDesc>`, and two `Table::getCleanName` method references needing `Table.TableDesc::getCleanName`. All fixed per the same Rule-3 redirect pattern as the other twelve files.

- **CRITICAL — flagged for plan 07-04:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileTestJava` still fails with 16 errors in exactly two files: `MBeanNameUnitTest.java` (needs `ProxySinkTask.buildMBeanName`, Phase 8) and `ClickHouseTestHarnessTest.java` (needs `ClickHouseBase.sharedContainerStarts()` and `ClickHouseTestHelpers.query(...)`, Phase 10). **`MBeanNameUnitTest.java` does not extend `ClickHouseBase`, so it is not excluded from `testUnit` by the ClickHouse tag** — its compile failure will block plan 07-04's stated acceptance gate (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` green) exactly as written, since `testUnit` depends on `compileTestJava` succeeding for the whole source set before any tag-based filtering happens. This plan could not resolve it without writing Phase 8's or Phase 10's own work, which D-06 and the phase's domain boundary explicitly forbid. Plan 07-04 (or the phase orchestrator) needs to make an explicit call: bring forward a minimal slice of Phase 8's `buildMBeanName` and/or Phase 10's shared-container counter, or apply the same disable-with-named-reason pattern used for the JSONP-01 files to these two, or accept a `testUnit` run scoped to exclude them by some other mechanism. This is a genuinely new decision this plan is not authorized to make unilaterally.

## Next Phase Readiness

- `compileJava` (main) is green — unaffected by anything in this plan beyond the two `ClickHouseSinkConfig.java` commits, both verified independently.
- Every file this plan's own declared scope (`files_modified`) and its Rule-3 carve-out touched compiles cleanly, confirmed via a forced `clean compileTestJava` (not just an incremental run — see Issues Encountered).
- **`compileTestJava` for the whole test source set does NOT yet succeed.** Two files remain blocked by explicitly-named Phase 8 and Phase 10 symbols. This is the single most important fact for plan 07-04 to act on before attempting its own `testUnit` gate — see the CRITICAL flag above.
- The rewritten JSON case's exact expectations, the four disabled classes' reason string, and `ClickHouseBase`'s restored members are all documented above for Phase 9, Phase 10, and Phase 11's respective consumers.
- No test task was run in this plan (`compileJava`/`compileTestJava`/`spotlessJavaCheck` only), per the plan's explicit prohibition — the behavioral canaries (`JsonInsertColumnsUnitTest`, `AsyncInsertSettingsUnitTest`, `RowBinaryUnsupportedTypeGateTest`, `ClickHouseHelperClientForkUnitTest`, and every other Docker-free fork test) remain unverified end-to-end until plan 07-04's `testUnit` run — which cannot itself proceed until the CRITICAL flag above is resolved.

## Self-Check: PASSED

All eighteen modified files found on disk. All three task commits (`4d0ec809`, `b4bae8cc`, `e1a90309`) found in `git log`. `git rev-list --count 5a89cfd7..HEAD` = 3, matching. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava` re-run clean at self-check time: `BUILD SUCCESSFUL`. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileTestJava` re-run at self-check time: `BUILD FAILED`, 16 errors, confined to exactly `MBeanNameUnitTest.java` and `ClickHouseTestHarnessTest.java` (documented above, not silently passed over). All five of Task 2's own automated verify sub-blocks (`BASE_SLIVER_AND_IMPORTS_OK`, `DEAD_API_CLEARED`, `TEST_REWRITES_OK`, `DISABLES_OK`, plus Task 1's `CFG01_REMAINDER_OK`/`ASYNC_INSERT_UNFORCED_OK`) re-run and passing. No conflict markers in either commit's added lines (`git diff --cached` grep clean before commit). `git diff pre-v1.6.0-sync HEAD` over the eight originally-declared files shows zero removed `@Test`/`@ParameterizedTest` lines (before=42, after=43 across those eight files); the four removed assertion lines in `RowBinaryUnsupportedTypeGateTest.java` are exactly the deliberate JSON-rewrite's old fork-specific assertions, matching the plan's own allowance. No test task (`test`/`testUnit`) was run, per plan prohibition.

---
*Phase: 07-type-mapping-writer-helper-client-config*
*Completed: 2026-09-19*
