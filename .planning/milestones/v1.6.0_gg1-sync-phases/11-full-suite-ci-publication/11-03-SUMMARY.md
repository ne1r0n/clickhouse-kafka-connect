---
phase: 11-full-suite-ci-publication
plan: 03
subsystem: docs
tags: [fork-delta, upstream-sync, documentation, r-01, skip-inventory]

requires:
  - phase: 06-merge-groundwork-build-migration
    provides: MERGE-CHECKLIST.md Section 1 classification and Recorded Facts 1-16, the ledger's index
  - phase: 09-metadata-free-json-path-keep-or-drop
    provides: the KEEP decision (Recorded Fact 14) whose accepted cost R-01 is published here
  - phase: 10-test-harness-reconciliation
    provides: the 18-skip named inventory and the shared-container cost (Recorded Fact 16)
provides:
  - docs/FORK-DELTA.md, a tracked behaviour ledger of what the fork still patches and what it retired in favour of upstream
  - raw file/insertion/deletion counts at both scopes and both endpoints, measured by command
  - a published statement of R-01 silent data loss, WR-01 (harness), the four open review findings and the skip inventory
  - one discoverability link line each in README.md and AGENTS.md
affects: [11-04, 11-05, 11-06, upstream-sync]

actuals:
  tokens: 6900
  tasks: 2
  commits: 2
plan_head_before: 09cae191785e24cbbf0a66a36f866908d94ecb66
plan_head_after: 623b8829879701c20d0dac7068975d798a7b53b2

tech-stack:
  added: []
  patterns:
    - "Every ledger row is derived from MERGE-CHECKLIST.md then confirmed by a grep or git diff against the merged tree, never transcribed"
    - "Publish all twelve scope-by-endpoint numbers in one table so no figure reads smaller by switching scope"

key-files:
  created:
    - docs/FORK-DELTA.md
  modified:
    - README.md
    - AGENTS.md

key-decisions:
  - "Published the measured wider-scope figures 9255 insertions and 450 deletions, not the plan-time 9256 and 449, and updated the verify gate rather than the document"
  - "Labelled the harness item 'WR-01 (test harness)' and kept the Future Requirement WR-01 (map null) as a separate item, because the identifier is used for two different things"
  - "Stated the tracking status of the four open review findings and the harness WR-01 as 'carried in STATE.md', because REQUIREMENTS.md's Future Requirements list does not name them"
  - "Did not recommend bypassRowBinary=true as a fix for R-01: no test pins that combination in metadata-free mode"

patterns-established:
  - "Line numbers are omitted from the published ledger because they rot; rows cite constructs and file names"

requirements-completed: [SUITE-03]

coverage:
  - id: D1
    description: "docs/FORK-DELTA.md exists as a tracked file beside DESIGN.md with a behaviour ledger of still-patched and retired rows, each grep-confirmed against the merged tree"
    requirement: "SUITE-03"
    verification:
      - kind: other
        ref: "bash structure gate: headings, 80+ lines, seven ## headings (104 lines after task 1, 164 after task 2)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Raw numbers published at both scopes and both endpoints (twelve numbers), re-measured by the four git diff --shortstat commands, with the growth in insertions stated plainly"
    requirement: "SUITE-03"
    verification:
      - kind: other
        ref: "git diff --shortstat x4 re-measure; twelve-number presence gate run with measured values"
        status: pass
    human_judgment: false
  - id: D3
    description: "Known costs and open items leads on R-01 with the trigger, the mechanism, the pin test, the operator fact and the explicit caveat that it does not transfer to other deployments"
    requirement: "SUITE-03"
    verification:
      - kind: other
        ref: "bash gate: required headings and open-item symbols present"
        status: pass
    human_judgment: true
    rationale: "Whether the R-01 statement reads as sufficiently plain and unsoftened to a stranger building from the public fork is an interpretive judgement no command settles"
  - id: D4
    description: "Named skip inventory states 18 skips, with the arithmetic from measured annotation counts (7 @Disabled + 11 parameterized cases behind 1 assumption site) and that none is fork coverage"
    requirement: "SUITE-03"
    verification:
      - kind: other
        ref: "grep -rc '@Disabled' = 7 and grep -rc 'Assumptions.assumeTrue' = 1 under src/test/java"
        status: pass
    human_judgment: false
  - id: D5
    description: "README.md and AGENTS.md each link docs/FORK-DELTA.md with one added line and zero deleted lines between them"
    requirement: "SUITE-03"
    verification:
      - kind: other
        ref: "git diff --numstat on the task 2 commit over README.md AGENTS.md = 2/0"
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 03: FORK-DELTA Behaviour Ledger Summary

**docs/FORK-DELTA.md: a grep-confirmed ledger of what the fork still patches and retired in favour of upstream v1.6.0, all twelve raw numbers (insertions up, stated plainly), R-01 silent data loss published first, and the 18-skip inventory, linked from README and AGENTS**

## Performance

- **Duration:** 7 min
- **Started:** 2026-10-07T10:43:07Z
- **Completed:** 2026-10-07T10:49:47Z
- **Tasks:** 2
- **Files modified:** 3 (1 created, 2 one-line edits); no file under `src/` changed (`git diff $plan_head_before HEAD -- src` is empty)

## Accomplishments

- `docs/FORK-DELTA.md` (164 lines, seven `##` headings, manual `## Contents` list, tone matched to `docs/DESIGN.md`) lists 26 still-patched rows and 16 retired or adopted rows, grouped by disposition and ordered by file path.
- The raw numbers section publishes files, insertions and deletions for both scopes at both endpoints, then says directly that insertions grew at both scopes and that wider-scope deletions rose (423 to 450). It adds one disclosure the plan did not require: 16 of the 30 `src/main` files that differ from upstream differ in import order only, a standing cost of keeping Spotless `importOrder()`.
- `## Known costs and open items` leads on R-01 (trigger, mechanism, pinning test name, operator fact, explicit "unverifiable for anyone else" caveat), then WR-01 (harness), the four open findings by symbol, other tracked follow-ups, and unadopted upstream capability.
- `## Named skip inventory` reaches 18 from measured counts, so plan 11-04's live-run derivation has a structural basis to agree with.

## Re-measured raw numbers

| Scope | Endpoint | Files | Insertions | Deletions |
|---|---|---|---|---|
| `src build.gradle.kts gradle .github` | `v1.2.9` to `pre-v1.6.0-sync` | 93 | 8401 | 423 |
| `src build.gradle.kts gradle .github` | `v1.6.0` to `sync/v1.6.0` | 82 | **9255** | **450** |
| `src/main` | `v1.2.9` to `pre-v1.6.0-sync` | 32 | 1308 | 139 |
| `src/main` | `v1.6.0` to `sync/v1.6.0` | 30 | 1358 | 97 |

Measured at `09cae191`. Two values differ from the plan-time figures (9256 insertions, 449 deletions at the wider scope after): plans 11-01 and 11-02 landed in the same wave and changed `.github/workflows/tests.yaml` (3 deletions) and a Javadoc in `ClickHouseTestHelpers.java` (7 insertions, 6 deletions). The plan instructs publishing measured values and updating the gate, so the document carries 9255 and 450 and the twelve-number gate was run with those. The direction of every claim is unchanged: insertions grew, wider-scope deletions rose, `src/main` deletions fell.

Skip structure measured: `@Disabled` sites = 7, `Assumptions.assumeTrue` sites = 1 (11 parameterized cases behind it), 7 + 11 = 18. This matches the plan's expectation exactly; nothing to flag.

## Ledger rows and the confirming check

All checks were run against the merged tree at `09cae191` (`HEAD` tree equals `sync/v1.6.0` for `src`, `build.gradle.kts`, `gradle` and `.github`).

### Still patched (every check found the construct present)

| Row | Check | Result |
|---|---|---|
| `targetTableFilter` / `jsonInsertColumns` / `jsonInsertFormatExplicit` config | `grep TARGET_TABLE_FILTER JSON_INSERT_COLUMNS jsonInsertFormatExplicit` in `ClickHouseSinkConfig.java`; `git diff v1.6.0 sync/v1.6.0` | present; all three are `+` lines against upstream |
| `async_insert` un-forced | `grep async_insert` in `src/main` | `//this.addClickHouseSetting("async_insert", "0", false);` commented out; upstream's diff line is `-` active, ours `+` commented |
| JMX `connector=` segment | `grep connectorName buildMBeanName` in `src/main`; `grep buildMBeanName` in `ProxySinkTask.java` | `props.get("name")` in `ClickHouseSinkTask.start()`, `new SinkTaskStatistics(connectorName, id)` in `ProxySinkTask`, `SinkTaskStatistics.buildMBeanName(String,int,String)` present; absent from `ProxySinkTask` |
| `onPartitionsRevoked` without `@Override` | `grep -B4 -A3 onPartitionsRevoked` in `ClickHouseSinkTask.java` | method present, comment names connect-api 4.1.1, no `@Override` |
| Metadata-free bypass with one predicate and five guard sites | `grep shouldSkipTableMetadata` in `ClickHouseWriter.java` | one definition (`csc != null && csc.isJsonInsertFormatExplicit()`) and exactly five call sites: `start`, `updateMapping`, the `auto.evolve` branch, `cleanupExtraFields`, `getTable` |
| `jsonInsertColumns` reaches the insert | `grep getJsonInsertColumns` in `ClickHouseWriter.java` | `doInsertJson` calls `client.insert(table.getName(), insertColumns, dataWriter, JSONEachRow, ...)` when non-empty |
| RowBinary fail-fast | `grep DYNAMIC UNKNOWN new DataException buildRowBinaryUnsupportedTypeMessage` | `validateDataSchema` `case "DYNAMIC": case "UNKNOWN":` throws `DataException`; `doWriteColValue` `default:` throws `DataException` |
| Server-side table filter | `grep "match(table"`, `buildTableFilterClause`, `setTargetTableFilter` in `ClickHouseHelperClient.java` | `" AND match(table, '%s')"` built by `buildTableFilterClause`, used in two queries; builder setter present |
| `shouldSkipDescribeRow` | `grep shouldSkipDescribeRow` in `src/main` | definition plus two call sites (describeTableV1 and V2 loops) |
| Explicit UTF-8 reader | `grep InputStreamReader` in `src/main`, then `grep -v UTF_8` | exactly one reader, with `StandardCharsets.UTF_8`; zero charset-less constructions |
| `Type.DYNAMIC` and `Column` dispatch | `grep DYNAMIC` in `Type.java`, `Column.java`; `git diff v1.6.0 sync/v1.6.0` | `DYNAMIC` enum constant and `startsWith("Dynamic")` branch; they are the only `+` lines in those two files |
| Four fork SMTs | `ls transforms/`; `git ls-tree v1.6.0` on the same directory; SPI file diff | fork has `ConvertToMapOrJson`, `HeaderToValue(Config)`, `HeadersToValue`, `TableOperationFilter`; `v1.6.0` has only `ExtractTopic(Config)` and `KeyToValue`; four `+` lines in the `Transformation` service file |
| Single-job CI | `grep CLICKHOUSE_VERSION CLIENT_VERSION env:` in `tests.yaml`; read file | no version pin or `env:` block (plan 11-02's result); one job, `timeout-minutes: 45` |
| `VERSION` | `cat VERSION` | `v1.6.0_gg1` |
| Six build additions and PD-01 | `grep jacoco testUnit javaLauncher windows-1252 withCapability benmanes spotless removeUnusedImports` in `build.gradle.kts`; count `forkChangedClasses` | all present; `forkChangedClasses` lists 15 classes |
| `slf4j-reload4j` literal | `grep reload4j` | `implementation("org.slf4j:slf4j-reload4j:2.0.17")`; no catalog entry |
| Dependency pins | `grep clickHouseDriver kafka` in `libs.versions.toml`; `grep connect.api` in `build.gradle.kts` | `clickHouseDriver = "0.9.6"`, `kafka = "4.1.1"`, `api(libs.kafka.connect.api)` |
| `ClickHouseBase` contract | `grep usesSharedContainer sharedContainerStarts @Tag uniqueConnectorName @AfterEach alwaysPull` | all present |
| Version-default canary | `grep CLICKHOUSE_VERSION_LATEST CLICKHOUSE_VERSION_DEFAULT`; `find ClickHouseVersionDefaultUnitTest` | default is `CLICKHOUSE_VERSION_LATEST = "latest"`; canary file present |
| Test-helper UTF-8 readers | `grep InputStreamReader` in `src/testFixtures` | three readers, all `StandardCharsets.UTF_8` |

### Retired or adopted (every check found the fork's version absent, or the file identical to upstream)

| Row | Check | Result |
|---|---|---|
| Duplicate `Map(K, Nullable(V))` null marker | `grep -c 'isNullable() && mapValue != null'` in `ClickHouseWriter.java` | 1 occurrence, carrying the `getMapValueType() != null` guard |
| Client V2 error classification | `grep isServerError ServerException` in `Utils.java`; import-only diff test | `t instanceof ClickHouseException \|\| t instanceof ServerException`; `Utils.java` is in the import-order-only set |
| `describeTableV2` database parameter | `grep "DESCRIBE TABLE"`; `grep this.database` | both describe queries use `database`; `this.database` appears only in the constructor, `.setDefaultDatabase` and the builder |
| `ProxySinkTask.buildMBeanName` | `grep buildMBeanName` in `ProxySinkTask.java` | absent |
| Fork JSON dispatch in `Column` | `grep Type.JSON` in `Column.java`; `git show v1.6.0:Type.java` | one `Type.JSON` dispatch, upstream's; `JSON` exists in upstream's enum |
| JUnit `5.11.3` pin | `grep 5\.11\.3 build.gradle.kts gradle/` | absent (present at `v1.2.9:build.gradle.kts` line 91) |
| `signing` and `buildconfig` plugins | `grep signing buildconfig BuildConfig` in both build files | absent (present at `v1.2.9` lines 33 and 35) |
| Orphans and duplicates deleted | `git ls-files` for `ClickHouseAPI.java`, `SchemaTestData`, `SchemalessTestData`, `ClickHouseTestHelpers`, `KeyToValueTest`; `ls src/test/java/transforms/KeyToValueTest.java` | zero `ClickHouseAPI`; one copy of each helper under `src/testFixtures`; fork's `transforms/KeyToValueTest.java` absent, upstream's repackaged one present |
| `Table`, `Processing`, `KeyToValue` | `git diff --quiet v1.6.0 sync/v1.6.0 -- <path>` | all three byte-identical |
| Upstream `src/main` | per-file `git diff --quiet` over the 63 files at `v1.6.0` | 38 identical, 25 differ (16 import-order only, 9 carry fork logic) |
| Upstream test tree | `git diff --name-status v1.2.9 v1.6.0` over the three test source sets, then per-file `git diff --quiet` | 145 files still present at `v1.6.0`, 129 identical, 16 adapted (breakdown in the document) |

## Contradictions and discrepancies, called out

No row's grep contradicted its expected disposition: nothing expected retired was still present, and nothing expected to survive was gone. Four discrepancies against the plan's and the planning documents' wording were found and handled:

1. **"Upstream's 56 test files adopted wholesale" does not reproduce.** The measurable count is 145 test-tree files that upstream added or changed between `v1.2.9` and `v1.6.0` and that still exist at `v1.6.0` (45 test-tree files exist at `v1.6.0` in total, which is also not 56). 129 are byte-identical here, 16 are adapted. The document publishes the measured figures and does not repeat 56. `11-CONTEXT.md` D-13 and the plan's `<action>` still say 56; they should be corrected before anything else quotes that number.
2. **"Explicit UTF-8 readers" (plural) is one reader in `src/main`.** `ClickHouseHelperClient` has exactly one `InputStreamReader`; the other three explicit readers are in `ClickHouseTestHelpers` under `src/testFixtures`. The document states one and three separately.
3. **Recorded Fact 9's line number is stale.** It cites `ClickHouseWriter.java:582` for the map null-marker guard; the guard now sits much later in the file, and the fact's quoted text still matches the code verbatim. The document cites the construct, not a line. The fact itself was not edited, since this plan does not own `MERGE-CHECKLIST.md`.
4. **The metadata-free guard sites number five, not the four named in the checklist's `ClickHouseWriter.java` row.** The row's wording predates the CR-01 fix that added the `auto.evolve` guard. The plan itself says five and the code agrees.

One tracking discrepancy for the orchestrator: `11-CONTEXT.md` D-08 says each of the four open review findings "stays a Future Requirement", and the plan asks for each to be stated as tracked as one. `REQUIREMENTS.md`'s Future Requirements section lists nine items and **none of the four findings, and not the harness WR-01 either**; they are carried in `.planning/STATE.md` (and `PROJECT.md` for P9-IN-01). The published document states the true location rather than claiming a Future Requirement entry. If they should be Future Requirements, that is an edit to `REQUIREMENTS.md`, which this plan does not modify.

## Task Commits

1. **Task 1: behaviour ledger and raw-numbers table** - `84ae28e5` (docs)
2. **Task 2: known costs, skip inventory, next-syncer section and the two links** - `623b8829` (docs)

**Plan metadata:** committed immediately after this file (docs: complete plan).

## Files Created/Modified

- `docs/FORK-DELTA.md` - new tracked ledger, 164 lines
- `README.md` - one added sentence under `## Design` linking the ledger
- `AGENTS.md` - one added bullet under the design-rationale line linking the ledger

## Decisions Made

- Measured values override plan-time values in the published table; the verify gate was updated, not the document (the plan's own instruction).
- The harness item is named "WR-01 (test harness)" with a pointer to the separate map-null Future Requirement of the same identifier, so a reader cannot conflate them.
- R-01 is stated without a recommended workaround: `bypassRowBinary=true` routes schema'd records through the JSON writer by reading `doInsertBatch`, but no test pins that in metadata-free mode, so the document says to verify rather than endorses it.
- Line numbers are left out of the document because they rot, which is exactly what happened to Recorded Fact 9.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Plan gate carried stale plan-time numbers**
- **Found during:** Task 1 (re-measurement)
- **Issue:** the twelve-number gate expected 9256 insertions and 449 deletions; the tree now measures 9255 and 450 because plans 11-01 and 11-02 changed tracked files in scope.
- **Fix:** published the measured values and ran the gate with them, as the gate's own `fails_when` text requires.
- **Files modified:** docs/FORK-DELTA.md
- **Verification:** all four `git diff --shortstat` commands re-run after writing; every published number matches.
- **Committed in:** 84ae28e5

**2. [Rule 1 - Bug] Unverified rationale claims removed from the first draft**
- **Found during:** Task 1 (self-review against the grep evidence)
- **Issue:** the first draft asserted that production relies on the un-forced `async_insert` setting, that the table filter avoids a "full catalogue read", that charset-less readers corrupt column names, and that upstream has no `JSON`/`Dynamic` child rule. None was verified by the greps; upstream's `Table` does filter `.null` and `.size[0-9]+`.
- **Fix:** rewrote each to state only what the code or Recorded Facts show.
- **Files modified:** docs/FORK-DELTA.md
- **Committed in:** 84ae28e5

**3. [Rule 1 - Bug] Test-tree adaptation row and test attributions corrected**
- **Found during:** Task 1 (diff inspection of the 16 adapted files)
- **Issue:** the first draft said the adapted tests differed by "import order or one-line changes" and attributed `CFG-01` to the wrong test class; seven tests change a constructor call and `ClickHouseSinkTaskMappingTest` adds a whole test method.
- **Fix:** itemised the 16 files accurately and re-pointed the attributions to tests whose source references the construct.
- **Files modified:** docs/FORK-DELTA.md
- **Committed in:** 84ae28e5

---

**Total deviations:** 3 auto-fixed (3 Rule 1, all documentation accuracy)
**Impact on plan:** none on scope. All three kept the document to what the tree demonstrably contains, which is the plan's own acceptance rule.

## Issues Encountered

- The first measurement loop failed under `zsh` because `set --` word splitting differs from `bash`; re-run under `bash -c`, no effect on results.
- A BSD `sed -i` invocation failed on macOS; the edit was redone with a short Python script. No effect on committed content.

## Known Stubs

None. The document contains no placeholders; every section is populated from measured evidence.

## Threat Flags

None. Documentation only; no new endpoint, auth path, file access or schema change. T-11-14 was checked directly: the document contains no host, credential, token or connection string (pattern scan returned only the word "password" inside the `passwordDefault` finding description).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 11-04 can re-derive the skip inventory from a live run and compare it against the document's 7 + 11 = 18 basis and its eight-row table.
- Plan 11-06's pre-push secret scan should include `docs/FORK-DELTA.md`.
- If any later plan in this phase changes files under `src`, `build.gradle.kts`, `gradle` or `.github`, the wider-scope figures in `## The raw numbers` go stale by a few lines; the four commands to refresh them are printed in the document.
- Open for the orchestrator: whether the four review findings and harness WR-01 should be added to `REQUIREMENTS.md`'s Future Requirements, and whether `11-CONTEXT.md`'s "56 test files" should be corrected.

## Self-Check: PASSED

- `docs/FORK-DELTA.md` FOUND; tracked (`git ls-files`); 164 lines; seven `##` headings.
- Commits `84ae28e5` and `623b8829` are ancestors of `HEAD`.
- `git diff 09cae191 HEAD -- src` is empty: no production code changed.
- README/AGENTS churn across the task 2 commit is 2 added, 0 deleted.
- No file deletions in either commit.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
