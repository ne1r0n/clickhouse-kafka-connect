---
phase: 04-json-insert-path-connector-plumbing
plan: 02
subsystem: testing
tags: [junit5, testcontainers, clickhouse-latest, client-v2, kafka-connect-json-insert-columns]

# Dependency graph
requires:
  - phase: 03-table-metadata-json-dynamic-types
    provides: "Typed SQL read-back convention (allRows/GenericRecord aliasing helper), jacocoForkReport coverage of ClickHouseWriter/ClickHouseSinkConfig/ProxySinkTask/ClickHouseSinkTask, and the green empty-jsonInsertColumns evidence (JsonDynamicRoundTripTest) that D-11 relies on"
  - phase: 04-json-insert-path-connector-plumbing (plan 01)
    provides: "ExplicitJsonInsertPathLiveTest's allRows/messageChainContains helper conventions and try/finally/table-before-start() lifecycle idiom, reused verbatim in this plan's JsonInsertColumnsLiveTest"
provides:
  - "JsonInsertColumnsLiveTest — ClickHouse-backed pins for COLS-02 (default empty list), COLS-03 (explicit subset against non-trivial DEFAULTs), COLS-04 (extra payload keys outside the list), COLS-05 (nonexistent column, exception classification), and COLS-06 (non-explicit path x cleanupExtraFields interaction)"
  - "JsonInsertColumnsUnitTest — Docker-free pins for COLS-01's jsonInsertColumns parsing contract (blank/unset -> empty non-null list, padding trim, adjacency drop, order preservation)"
  - "COLS-05 classification recorded: root cause is com.clickhouse.client.api.ServerException (client-V2 API exception type), ClickHouse error code 16 (NO_SUCH_COLUMN_IN_TABLE), non-retriable per Utils.handleException's fallback path"
affects: [04-03-jmx-async-plan, 05-ci-and-final-verification]

# Actuals (#2632)
actuals:
  tokens: 5167
  tasks: 3
  commits: 3
plan_head_before: a8fa576083a43f24e440da38d64ae38ea48b39ef

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Schemaless-first live testing (D-06, carried from 04-01): every SinkRecord in JsonInsertColumnsLiveTest is a Map value, never a Struct, because doInsertJsonV2 is the only route that passes csc.getJsonInsertColumns() to client.insert(...) — the String route (doInsertStringV2) uses the 4-arg overload with no column list."
    - "Non-trivial DEFAULT fixture columns (D-12): extra Int32 DEFAULT 42 / tag String DEFAULT 'unset' make 'unlisted column received its DEFAULT' distinguishable from 'column was never written' — a plain Int32/String column would read back 0/'' for both cases."
    - "Docker-free ClickHouseSinkConfig unit test constructing the POJO directly from a minimal Map<String,String> (HOSTNAME/PORT/DATABASE/USERNAME/PASSWORD) and asserting on getJsonInsertColumns() — extends nothing, stays in testUnit (D-16, no prior dedicated ClickHouseSinkConfig test class existed)."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java
  modified: []

key-decisions:
  - "COLS-05's exception classification captured via a temporary debug print (removed before commit): the root cause is com.clickhouse.client.api.ServerException — NOT com.clickhouse.client.ClickHouseException — carrying ClickHouse error code 16 (NO_SUCH_COLUMN_IN_TABLE). Utils.handleException's `rootCause instanceof ClickHouseException` switch therefore never matches under client V2, so a bad jsonInsertColumns entry always falls through to the final non-retriable RuntimeException wrap regardless of the switch's case list — the D-10 classification (not retriable) holds, but for a different structural reason than the switch's exhaustiveness (04-RESEARCH.md's Open Question #1 assumed ClickHouseException would be the type checked; it is not, under client V2)."
  - "COLS-02 written as a plain confirmatory test per D-11: Phase 3's JsonDynamicRoundTripTest already drove doInsertJsonV2 with the default empty jsonInsertColumns list and was green, so this plan does not re-open it as a research question — it only adds an explicit, requirement-ID-labelled pin."
  - "Task 1 (COLS-03) executed as the tracer slice; its automated <verify> was re-run and passed before Task 2 expansion began (HUMAN_VERIFY_MODE=end-of-phase, fully automated verify, no checkpoint needed per the tracer feedback gate)."

requirements-completed: [COLS-01, COLS-02, COLS-03, COLS-04, COLS-05, COLS-06]

coverage:
  - id: D1
    description: "jsonInsertColumns parsing: unset/empty/whitespace-only/lone-comma all parse to an empty, never-null List; padding is trimmed; adjacent/leading/trailing empty entries are dropped; source order is preserved with no sort or dedup"
    requirement: "COLS-01"
    verification:
      - kind: unit
        ref: "JsonInsertColumnsUnitTest#blankOrWhitespaceOnlyValueParsesToEmptyList, #unsetKeyParsesToEmptyList, #paddingStrippedAndAdjacentEmptyEntryDropped, #leadingDoubledAndTrailingCommasAreDropped, #sourceOrderIsPreservedExactly, #singleElementValueParsesToSingleElementList"
        status: pass
    human_judgment: false
  - id: D2
    description: "With the default empty jsonInsertColumns list, every column of the fixture table receives its payload value by typed read-back (confirmatory pin, D-11)"
    requirement: "COLS-02"
    verification:
      - kind: integration
        ref: "JsonInsertColumnsLiveTest#defaultEmptyListFillsEveryColumnFromPayload"
        status: pass
    human_judgment: false
  - id: D3
    description: "An explicit jsonInsertColumns subset (with deliberate padding) fills the listed columns and leaves every unlisted column at its non-trivial DEFAULT, proving both the column clause and the parser's trim reach the server"
    requirement: "COLS-03"
    verification:
      - kind: integration
        ref: "JsonInsertColumnsLiveTest#explicitSubsetFillsListedColumnsAndLeavesUnlistedAtDefault"
        status: pass
    human_judgment: false
  - id: D4
    description: "A payload key that is a real column outside the configured list, and a payload key that is not a column at all, both leave the insert successful; the real-but-unlisted column reads back its DEFAULT, not its payload value"
    requirement: "COLS-04"
    verification:
      - kind: integration
        ref: "JsonInsertColumnsLiveTest#extraPayloadKeysOutsideListDoNotBreakInsertAndStayAtDefault"
        status: pass
    human_judgment: false
  - id: D5
    description: "A jsonInsertColumns entry naming a nonexistent column makes put() throw a non-retriable RuntimeException, no partial insert (countRows==0), no NullPointerException anywhere in the cause chain; the observed exception class and ClickHouse error code are recorded"
    requirement: "COLS-05"
    verification:
      - kind: integration
        ref: "JsonInsertColumnsLiveTest#nonexistentColumnThrowsNonRetriableWithNoPartialInsert"
        status: pass
    human_judgment: false
  - id: D6
    description: "On the non-explicit JSON path, jsonInsertColumns still applies: listed columns are filled, a real-but-unlisted column keeps its DEFAULT even though cleanupExtraFields kept its payload key in the JSONEachRow body, and a non-column key is stripped"
    requirement: "COLS-06"
    verification:
      - kind: integration
        ref: "JsonInsertColumnsLiveTest#nonExplicitPathHonoursColumnListAndCleanupExtraFieldsInteraction"
        status: pass
    human_judgment: false

duration: ~55 min (single session)
completed: 2026-09-17
status: complete
---

# Phase 4 Plan 2: jsonInsertColumns Summary

**Pinned every insert-time consequence of `jsonInsertColumns` (default list, explicit subset against non-trivial DEFAULTs, extra payload keys, a nonexistent column's non-retriable classification, and the non-explicit path's interaction with `cleanupExtraFields`) plus its Docker-free parsing contract, in two new fork-only test classes.**

## Performance

- **Duration:** ~55 min (single session)
- **Tasks:** 3 / 3 complete
- **Files modified:** 2 (both new)

## Accomplishments

- `JsonInsertColumnsLiveTest` (new, `com.clickhouse.kafka.connect.sink`, extends `ClickHouseBase`): 5 live tests covering COLS-02 through COLS-06, every insert built from schemaless `Map` records (D-06) since `jsonInsertColumns` is only reachable through `doInsertJsonV2`'s 5-arg `client.insert(...)` overload.
- `JsonInsertColumnsUnitTest` (new, `com.clickhouse.kafka.connect.sink`, Docker-free, extends nothing): 9 tests pinning COLS-01's parsing contract — unset and blank both yield an empty, never-null `List`; padding trimmed; empty/leading/trailing comma entries dropped; source order preserved with no sort or dedup.
- COLS-05's classification captured empirically and recorded in this SUMMARY (see Decisions Made): root cause `com.clickhouse.client.api.ServerException`, ClickHouse error code 16 (`NO_SUCH_COLUMN_IN_TABLE`), non-retriable — with the structural nuance that `Utils.handleException`'s `ClickHouseException`-typed switch never matches this client-V2 exception type at all, so the non-retriable outcome comes from the unconditional fallback path, not from the switch excluding the error code.
- `git status --porcelain -- src/main/ build.gradle.kts` is empty for the whole plan; `Utils.java`, `ClickHouseWriter.java`, and `ClickHouseSinkConfig.java` blob hashes are unchanged (verified via `git hash-object` after every task).
- Full `./gradlew testUnit --rerun` is green (201 tests, 199 passed, 2 skipped — pre-existing skips, unrelated to this plan).

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end column-list slice (COLS-03 tracer)** - `43b759b` (test)
2. **Task 2: Expand live class — COLS-02, COLS-04, COLS-05 classification, COLS-06** - `5fdbb2a` (test)
3. **Task 3: Docker-free parsing pins (COLS-01)** - `a5ff5b6` (test)

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` - New ClickHouse-backed class; 5 live tests for COLS-02..06
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java` - New Docker-free class; 9 tests for COLS-01's parsing contract

## Decisions Made

- **COLS-05's exact classification captured live** (Task 2). A temporary debug print (added, run once, then removed before the commit — never left in the committed source) walked the exception cause chain for the nonexistent-column scenario. Observed: the outer exception is `java.lang.RuntimeException` (two layers: `Utils.handleException`'s wrap with message `"Number of records: 1"`, whose cause is another `RuntimeException` with message `"Topic: [...] (QueryId: [...])"`); the root cause is `com.clickhouse.client.api.ServerException` (the client-V2 API exception type) carrying ClickHouse error code 16 (`NO_SUCH_COLUMN_IN_TABLE`), message `"Code: 16. DB::Exception: No such column no_such_column in table <db>.<table> (...). (NO_SUCH_COLUMN_IN_TABLE) (version 26.8.6.5 (official build))"`. This resolves 04-RESEARCH.md's Open Question #1 and Assumption A2: `Utils.handleException`'s retriable-error switch is typed against `com.clickhouse.client.ClickHouseException`, not `com.clickhouse.client.api.ServerException`, so under client V2 that switch's `instanceof` check never matches for this error at all — the non-retriable classification is correct (D-10 pin holds), but it is reached via the unconditional final `else` fallback, not via the switch excluding error code 16 from its retriable list. No production code was changed; this is a pinned observation only.
- **COLS-02 written as a confirmatory test, not a discovery test, per D-11.** Phase 3's `JsonDynamicRoundTripTest` already drove `doInsertJsonV2` with the default empty `jsonInsertColumns` list and was green, so the roadmap's empty-list research flag was withdrawn before this plan started. This test exists to give COLS-02 its own explicit, requirement-ID-labelled pin rather than relying on an incidental side effect of a Phase 3 test.
- **Task 1's tracer feedback gate resolved without a checkpoint.** `HUMAN_VERIFY_MODE=end-of-phase` (config default) plus a fully automated `<verify>` for Task 1 (COLS-03) meant the gate re-ran the automated verify, it passed, and execution continued straight into Task 2's expansion with a log-only note rather than a `checkpoint:human-verify`.

## Deviations from Plan

None - plan executed exactly as written. No `Rule 1-4` deviations were triggered: no bugs found in main code, no missing critical functionality, no blocking issues, no architectural changes needed. The one adaptation beyond the plan's literal task text was executor-level: Task 1 and Task 2's target file (`JsonInsertColumnsLiveTest.java`) was built incrementally across the two tasks' own commit boundaries (COLS-03 alone in Task 1's commit, the remaining four methods added in Task 2's commit) to preserve per-task commit atomicity, exactly matching the plan's stated task/file split.

## Issues Encountered

None. Docker (colima `vz` profile) was reachable throughout (`docker info` succeeded, `colima list` showed `vz` Running); all live and Docker-free verification commands passed on their first real run. The one operational step worth noting: capturing COLS-05's exact exception classification required a throwaway debug-print run (`System.out.println` walking the cause chain plus a ClickHouse-error-code print) that was executed once via `--tests ...nonexistentColumnThrowsNonRetriableWithNoPartialInsert -i`, then fully removed from the source before the Task 2 commit — the committed test method contains only the plan-specified assertions plus the resulting inline comment recording the observed facts.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- COLS-01 through COLS-06 are fully pinned; `jsonInsertColumns`'s parsing contract and every insert-time consequence (default list, explicit subset, extra keys, nonexistent-column classification, non-explicit-path interaction with `cleanupExtraFields`) now have automated regression coverage against ClickHouse `latest` / client V2.
- The String-record route's silent disregard of `jsonInsertColumns` (04-CONTEXT.md Deferred Ideas) remains unpinned and unfixed, exactly as scoped — no test in this plan claims coverage of that route.
- No blockers for 04-03 (JMX + `async_insert`) — independent test track per ROADMAP.md, shares no files with this plan.

## Self-Check: PASSED

All created files (`JsonInsertColumnsLiveTest.java`, `JsonInsertColumnsUnitTest.java`, this SUMMARY.md) confirmed present on disk, and all three task commits (`43b759b`, `5fdbb2a`, `a5ff5b6`) confirmed present in `git log --oneline --all`.

---
*Phase: 04-json-insert-path-connector-plumbing*
*Completed: 2026-09-17*
