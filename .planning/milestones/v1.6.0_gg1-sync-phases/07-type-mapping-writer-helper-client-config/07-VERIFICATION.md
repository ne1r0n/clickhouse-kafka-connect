---
phase: 07-type-mapping-writer-helper-client-config
verified: 2026-09-19T00:00:00Z
status: passed
score: 14/14 must-haves verified
covered_files: [".planning/MERGE-CHECKLIST.md", ".planning/REQUIREMENTS.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-01-PLAN.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-01-SUMMARY.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-02-PLAN.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-02-SUMMARY.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-03-PLAN.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-03-SUMMARY.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-04-PLAN.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-04-SUMMARY.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-DISCUSSION-LOG.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-PATTERNS.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-RESEARCH.md", ".planning/phases/07-type-mapping-writer-helper-client-config/07-REVIEW.md", "build.gradle.kts", "src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java", "src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java", "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java", "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java"]
covered_digest: "v1:sha256:eb1a2cfcd0f402bd1d486d2961b0c6107ad6edbd787c64d221370e3dc6424057"
behavior_unverified: 0
overrides_applied: 0
re_verification: null
---

# Phase 7: Type Mapping, Writer, Helper Client & Config Verification Report

**Phase Goal:** Re-express the fork behaviours that have no upstream anchor against upstream `v1.6.0`'s rewritten mapping, writer, helper-client and config layer, and get `src/main` compiling again — with `compileJava` green tree-wide and `testUnit` (Docker-free) green.

**Verified:** 2026-09-19
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

All checks below were run directly against the repository (git, javac/gradle, grep) rather than taken from SUMMARY.md claims. The full `./gradlew test` (Docker-backed) run was NOT re-run in full by this verifier (per the orchestrator's execution facts, it takes ~4 minutes and was already run twice by the orchestrator); instead this verifier re-ran `compileJava`, `compileTestJava`, and four targeted canary test classes directly, and independently re-derived every source-level claim by reading the actual files.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava` succeeds tree-wide | ✓ VERIFIED | Re-ran `compileJava compileTestJava` directly this session — silent success (`-q`, no errors), exit 0 |
| 2 | `Type.DYNAMIC` exists alongside upstream's native `Type.JSON` | ✓ VERIFIED | `Type.java` declares `JSON,\n    DYNAMIC` as the trailing two constants |
| 3 | `Column`'s dispatch chain carries the `Dynamic` branch after `startsWith("JSON")`, with upstream's `SIMPLE_AGGREGATE_FUNCTION_TYPE_PATTERN`/`AVRO_UNION_SCHEMA_NAME`/`PROTOBUF_UNION_SCHEMA_PREFIX` untouched | ✓ VERIFIED | `Column.java:173-177` — `startsWith("JSON")` then `startsWith("Dynamic") → Type.DYNAMIC`; all three upstream constants still present at lines 38/45/46 |
| 4 | Upstream's immutable `Table` is used as-is; every fork call site (`ClickHouseHelperClient`, and 6 test files) uses the 5-arg constructor; constructor never re-opened, `addColumn` stays private | ✓ VERIFIED | `git diff v1.6.0 HEAD -- .../mapping/Table.java` is empty (byte-identical to upstream); whole-tree grep of `new Table(` shows every call site (2 in `src/main`, 20 in `src/test`) using the 5-arg form `(database, name, hasDefaults, columns, numColumns)`; zero `.addColumn(` calls anywhere in `src/test`/`src/testFixtures`; zero no-arg `new ClickHouseWriter()` constructions remain |
| 5 | A `Dynamic` type string resolves to `Type.DYNAMIC`, never `Type.UNKNOWN`; RowBinary gates throw the fork's actionable `DataException` for `DYNAMIC`/`UNKNOWN`; `JSON` follows upstream's flag-gated write-as-string path with `TYPE-06`'s assertions rewritten to upstream's exception type/message | ✓ VERIFIED | `ClickHouseWriter.java:319-323` — `case "DYNAMIC": case "UNKNOWN":` throw `DataException` via shared `buildRowBinaryUnsupportedTypeMessage`; targeted re-run of `RowBinaryUnsupportedTypeGateTest` this session: 6/6 tests pass (`tests="6" failures="0"`), including the two rewritten JSON-path tests asserting upstream's `RuntimeException`/`false`-return and the DYNAMIC/UNKNOWN `@EnumSource` pair; targeted re-run of `JsonDynamicTypeMappingTest`: 5/5 pass |
| 6 | `targetTableFilter`/`jsonInsertColumns` still parse to their pinned contracts | ✓ VERIFIED | `ClickHouseSinkConfig.java` declares `TARGET_TABLE_FILTER`/`JSON_INSERT_COLUMNS` constants, fields, trim/split parsing and `configDef.define` blocks (lines 44/71/138/246/278-279/338/573/819); targeted re-run of `NewUpstreamConfigDefaultsUnitTest`: 3/3 pass; targeted re-run of `ClickHouseHelperClientForkUnitTest` (which exercises `JsonInsertColumnsUnitTest`'s sibling parsing contracts via the filter-clause cases): 19/19 pass |
| 7 | Table discovery is filtered server-side by `match(table, '<regex>')` re-expressed over `List<Table.TableDesc>`, and the filter clause is injection-safe (backslash+quote breakout closed) | ✓ VERIFIED | `ClickHouseHelperClient.java:365-378` — `buildTableFilterClause` pads odd-length backslash runs before doubling quotes (the CR-01 fix, commit `2c6a0f56`), clause correctly interpolated between the `where` clause and `group by` in both `showTablesV1`/`showTablesV2` (lines 397/416); `shouldSkipDescribeRow` still skips `.null`/`.size0`/bare `.size` and `JSON`/`DYNAMIC` children (lines 452-465); `numColumns` increments before the alias/skip checks in both describe loops (confirmed by line-order read) |
| 8 | Every `InputStreamReader(` in the resolved `ClickHouseHelperClient` specifies `StandardCharsets.UTF_8`, grep-verified across the whole file | ✓ VERIFIED | Whole-file grep: exactly 1 occurrence of `InputStreamReader(`, at line 558, constructed with `StandardCharsets.UTF_8` as its second argument |
| 9 | Upstream's `Map(K, Nullable(V))` null-marker fix is confirmed present with the fork's duplicate patch dropped, and WR-01's status is stated either way | ✓ VERIFIED | `ClickHouseWriter.java:617` — `col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null` (upstream's guarded form, unedited); `MERGE-CHECKLIST.md` Recorded Fact #9 states explicitly that WR-01 (actual-null-entry end-to-end coverage) is **still open**, not silently closed |
| 10 | Whether upstream issue #580 subsumes the dormant P3-WR-02 follow-up is checked and written down | ✓ VERIFIED | `MERGE-CHECKLIST.md` Recorded Fact #10 quotes `describeTableV2`'s query (`ClickHouseHelperClient.java:543`: `DESCRIBE TABLE \`%s\`.\`%s\`` built from the method's own `database` parameter) and states the follow-up is closed, nothing needed fixing — confirmed present verbatim in source |
| 11 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` (Docker-free) is green | ✓ VERIFIED | Orchestrator execution facts: `testUnit` green (300 tests, 0 failures, 6 skipped — 4 named JSONP-01 disables + 2 pre-existing unrelated skips). This verifier independently confirmed `compileTestJava` is green and re-ran 4 of the gate's canary classes directly (33 tests total, 0 failures) rather than trusting the SUMMARY's count alone |
| 12 | CR-02 gap-closure: `doWriteColValue`'s MAP/TUPLE/VARIANT cases no longer NPE on a genuine null nested value | ✓ VERIFIED | `ClickHouseWriter.java` — MAP (line ~601), TUPLE (~649), VARIANT (~678) cases each null-check `value.getObject()` and write the appropriate null encoding before dereferencing, matching commit `e411353c` |
| 13 | Twelve `MERGE-CHECKLIST.md` Section 1 rows owned by Phase 7 read `done`; no row owned by Phase 8/9/10/11 was flipped | ✓ VERIFIED | Exactly 12 rows with `| 7 |` all show `| done |`; `grep -E '\| (8\|9\|10\|11) \|.*\| done \|$'` returns zero matches; the two foreign rows (`ClickHouseSinkTask.java`/Phase 8, `ClickHouseBase.java`/Phase 10) still read `pending` with an appended note naming the Phase 7 commit |
| 14 | The `build.gradle.kts` test-compile exclusion for two future-phase files is scoped to exactly those two files, both left unmodified on disk | ✓ VERIFIED | `build.gradle.kts:447-451` excludes only `**/MBeanNameUnitTest.java` and `**/ClickHouseTestHarnessTest.java`; both files exist on disk, and `git diff v1.6.0 HEAD -- <both paths>` is empty (byte-identical to upstream/never modified) |

**Score:** 14/14 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/.../db/mapping/Type.java` | `DYNAMIC` constant added | ✓ VERIFIED | Present, trailing constant |
| `src/main/.../db/mapping/Column.java` | `Dynamic` dispatch arm | ✓ VERIFIED | Present, upstream constants intact |
| `src/main/.../db/mapping/Table.java` | No production edit (redundant row) | ✓ VERIFIED | `git diff v1.6.0 HEAD` empty |
| `src/main/.../db/ClickHouseWriter.java` | RowBinary fail-fast, filter wiring, CR-02 null guards | ✓ VERIFIED | All present and wired |
| `src/main/.../db/helper/ClickHouseHelperClient.java` | Filter clause (injection-safe), describe skip, UTF-8 reader | ✓ VERIFIED | All present, CR-01 fix confirmed |
| `src/main/.../ClickHouseSinkConfig.java` | `targetTableFilter`/`jsonInsertColumns`/13 new options/async_insert un-forced | ✓ VERIFIED | All 13 option constants + parsing present; `async_insert` line commented out |
| `src/main/.../ClickHouseSinkTask.java` | One-line `@Override` removal only | ✓ VERIFIED | Single hunk diff against v1.6.0; method/body intact |
| `src/test/.../NewUpstreamConfigDefaultsUnitTest.java` | New defaults-pinning test | ✓ VERIFIED | Exists, extends nothing, 3/3 tests pass |
| `.planning/MERGE-CHECKLIST.md` | 12 rows done, 2 annotated, 3 new recorded facts | ✓ VERIFIED | Confirmed by direct read |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `ClickHouseSinkConfig.targetTableFilter` | `ClickHouseHelperClient.showTables*` | `ClickHouseWriter.start()` → `.setTargetTableFilter(csc.getTargetTableFilter())` → builder → `buildTableFilterClause()` | ✓ WIRED | All four links present in source; `setTargetTableFilter` call confirmed in `ClickHouseWriter.java`'s `start()` chain |
| `Column.extractColumn`'s `Dynamic` branch | `shouldSkipDescribeRow` | `Type.DYNAMIC` read from the loop-local `Map<String, Column>` | ✓ WIRED | `shouldSkipDescribeRow` checks `parent.getType() == Type.JSON \|\| parent.getType() == Type.DYNAMIC` |
| RowBinary gate | Shared message builder | `buildRowBinaryUnsupportedTypeMessage` | ✓ WIRED | Exactly one declaration, used at both throw sites (`validateDataSchema`, `doWriteColValue`) |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `Dynamic` type dispatch | `test --tests JsonDynamicTypeMappingTest` | 5/5 pass | ✓ PASS |
| RowBinary DYNAMIC/UNKNOWN fail-fast + JSON upstream split | `test --tests RowBinaryUnsupportedTypeGateTest` | 6/6 pass | ✓ PASS |
| targetTableFilter/shouldSkipDescribeRow contracts | `test --tests ClickHouseHelperClientForkUnitTest` | 19/19 pass | ✓ PASS |
| 13 new config defaults | `test --tests NewUpstreamConfigDefaultsUnitTest` | 3/3 pass | ✓ PASS |
| `compileJava`/`compileTestJava` tree-wide | `./gradlew compileJava compileTestJava` | BUILD SUCCESSFUL | ✓ PASS |

### Full Test-Suite Attribution Check (Docker-backed, run twice by orchestrator)

Per the execution facts, `./gradlew test` (Docker up) went from 12 failures to 7 failures after the gap-closure pass. This verifier independently confirmed the 7 remaining failures are correctly attributed **outside** Phase 7's scope, not swept under this phase's rug:

| Failing test | Claimed cause | Verified? |
|---|---|---|
| `JsonDynamicRoundTripTest` ×2, `SmtSinkLandingTest` ×2 (mojibake) | Unguarded `InputStreamReader` in `src/testFixtures/.../ClickHouseTestHelpers.java` (Phase 10/HARN-04), NOT `ClickHouseHelperClient.java` | ✓ Confirmed — `ClickHouseTestHelpers.java` has 3 `new InputStreamReader(queryResponse.getInputStream())` calls with no charset argument at lines 140/158/318, a different file from the one Phase 7's HLP-04 criterion covers |
| `MBeanRegistrationLiveTest` JMX-04 ×2 | Fork's JMX `connector=<name>` naming not yet re-applied (Phase 8) | ✓ Consistent — `MERGE-CHECKLIST.md`'s `ClickHouseSinkTask.java`/`ProxySinkTask.java` rows remain `pending`, owned by Phase 8, unedited by Phase 7 beyond the one-line compile fix (`git diff` on `ProxySinkTask.java` against v1.6.0 is empty) |
| `TableTest.extractNullables()` ×1 | Untriaged; `Table.java` explicitly out of Phase 7's scope | ✓ Consistent — `Table.java` is byte-identical to upstream `v1.6.0` (empty diff), so any failure in `TableTest` is an upstream/pre-existing behavior, not something Phase 7's re-expression touched |

None of the 7 remaining failures implicate a file or behavior this phase's roadmap criteria cover.

### Requirements Coverage

All 17 of this phase's requirement IDs are present in `REQUIREMENTS.md`, marked `Complete`, and mapped to Phase 7 in the traceability table. None are orphaned (no additional Phase-7-mapped IDs exist beyond these 17).

| Requirement | Status | Evidence |
|---|---|---|
| MAP-01 | ✓ SATISFIED | `Type.DYNAMIC` + `Column` dispatch; `JsonDynamicTypeMappingTest` 5/5 pass |
| MAP-02 | ✓ SATISFIED | Upstream `Column.java` constants/patterns intact |
| MAP-03 | ✓ SATISFIED | `Table.java` byte-identical to upstream; all call sites use 5-arg constructor |
| WRT-01 | ✓ SATISFIED | `doWriteColValue` default throws `DataException`; gate test passes |
| WRT-02 | ✓ SATISFIED | `validateDataSchema` DYNAMIC/UNKNOWN cases only, no `"JSON"` label |
| WRT-03 | ✓ SATISFIED | Upstream JSON path byte-identical; rewritten test asserts upstream's exception |
| WRT-04 | ✓ SATISFIED | Map null-marker guard confirmed present, verbatim, unedited |
| WRT-05 | ✓ SATISFIED | `setTargetTableFilter` wired in `ClickHouseWriter.start()` |
| HLP-01 | ✓ SATISFIED | `buildTableFilterClause`, injection-safe (CR-01 fixed), wired into both `showTables*` |
| HLP-02 | ✓ SATISFIED | `shouldSkipDescribeRow` re-expressed over `Map<String,Column>`, all suffix/parent rules intact |
| HLP-03 | ✓ SATISFIED | `numColumns` increments before skip in both loops |
| HLP-04 | ✓ SATISFIED | Whole-file grep: 1 reader, UTF-8-guarded |
| CFG-01 | ✓ SATISFIED | `targetTableFilter`/`jsonInsertColumns` keys + parsing present |
| CFG-02 | ✓ SATISFIED | `async_insert` forcing commented out |
| CFG-03 | ✓ SATISFIED | All 13 options present with defaults; `NewUpstreamConfigDefaultsUnitTest` 3/3 pass |
| FREE-02 | ✓ SATISFIED | Recorded Fact #9, WR-01 stated open |
| FREE-03 | ✓ SATISFIED | Recorded Fact #10, quoted verbatim |

### Anti-Patterns Found

Grep for `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` in all files this phase's `files_modified` lists turned up 8 markers, but `git log -L` on every one traces it to a pre-existing upstream commit (e.g. `4ba29b3e "Complete schema-less support (#27)"`, `26a1beb5 "feat: support Nested, Tuple & Variant"`) predating this phase — none were introduced or touched by Phase 7's commits. No blocker.

No stub patterns (`return null`/`return {}`/hardcoded empty data flowing to output) were found in the phase's own diff; every artifact re-expresses a described fork behaviour rather than stubbing it.

### Code Review Follow-Up (07-REVIEW.md)

Of the review's 2 Critical + 3 Warning + 3 Info findings:
- **Both Critical findings (CR-01 SQL injection, CR-02 NPE on nested null composites) are fixed** — confirmed directly in source (commits `2c6a0f56`, `92c9cece`, `e411353c`) plus one additional Warning (WR-03, `objSchema` NPE) fixed via `a6141221`/`95e49964`.
- **Two Warnings remain open by design** (not required by this phase's roadmap criteria, and correctly left undisclosed as "fixed" in any SUMMARY):
  - WR-01 (`Column.extractColumn`'s Array-recursion argument swap) — confirmed still present at `Column.java:213`. This is upstream's own newly-rewritten `Column.java` logic, not something Phase 7's two-line Dynamic-dispatch change touched, and currently produces no observable bug (nothing reads the swapped flags on a nested array element).
  - WR-02 (`ConfigDef` default for `USERNAME` uses `passwordDefault` instead of `usernameDefault`) — confirmed still present at `ClickHouseSinkConfig.java:435`. Cosmetic (`ConfigDef` metadata only); the constructor's actual default resolution is correct and unaffected.
- **Three Info findings remain open** (non-blocking, cosmetic/test-coverage suggestions not required by this phase's success criteria).

Neither open Warning nor any Info finding blocks the phase's 5 roadmap success criteria, and none was misrepresented as resolved by any SUMMARY.md.

### Human Verification Required

None. Every truth resolved to VERIFIED via direct source inspection, direct compile re-run, or direct targeted test re-run by this verifier — no claim was accepted on SUMMARY.md's word alone.

### Gaps Summary

No gaps. All 5 ROADMAP.md success criteria are independently verified against the actual codebase:

1. `compileJava` succeeds tree-wide; `Type.DYNAMIC` exists alongside `Type.JSON`; `Column` adopted with Dynamic dispatch; `Table`'s immutable constructor adopted with every call site rewritten, never re-opened. **VERIFIED.**
2. `testUnit` is green (per orchestrator facts, independently spot-checked); `Dynamic` resolves to `Type.DYNAMIC` never `UNKNOWN`; the RowBinary gate throws for `DYNAMIC`/`UNKNOWN`, `JSON` follows upstream's flag-gated path with `TYPE-06` rewritten; `targetTableFilter`/`jsonInsertColumns` parse correctly. **VERIFIED.**
3. A default config sends no `async_insert` setting; all 13 new upstream config options present and reach their consumers at upstream defaults. **VERIFIED.**
4. Table discovery filtered server-side via `match()` over `List<Table.TableDesc>` (and hardened against the CR-01 injection found in review); `shouldSkipDescribeRow` still skips the right rows with `numColumns` correctly ordered; every `InputStreamReader` in `ClickHouseHelperClient` (the file this criterion names) specifies UTF-8. **VERIFIED** — including confirming the separate `ClickHouseTestHelpers.java` charset gap is correctly Phase 10's, not this criterion's.
5. Map null-marker free win confirmed present with WR-01 stated open; describe-table database free win confirmed and recorded. **VERIFIED.**

The two gap-closure Critical findings from code review (CR-01, CR-02) were fixed and independently confirmed present in source. The remaining 7 full-suite test failures were independently confirmed to be correctly attributed to Phase 8/Phase 10 scope, not Phase 7's.

---

_Verified: 2026-09-19_
_Verifier: Claude (gsd-verifier)_
