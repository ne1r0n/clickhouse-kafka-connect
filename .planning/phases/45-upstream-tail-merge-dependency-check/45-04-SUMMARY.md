---
phase: 45-upstream-tail-merge-dependency-check
plan: "04"
subsystem: writer-value-free-failures
tags: [ups-02, value-free, json-route, jackson-rebuild, retry-classification, mutation-testing, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: plan 45-03's withheldJacksonChain rebuild, its pins and anchors (195 rows), E-09 testUnit total 1995
provides:
  - commit 0c8be1da, writeRecordsJson rethrows a serialization failure through the shared throwRebuiltJacksonChain on the failure path only; doWriteColValue uses the same entry point
  - JsonRouteValueFreeUnitTest (8 shapes x 5 checks + 2 success-path byte tests) and JsonRouteValueFreeDlqUnitTest (2 shapes x 2 tolerances), both Docker-free
  - two UPS-02 anchor rows (anchors mirror rows=197 bad=0)
  - E-10, E-11, E-12 (measurement, red-first and green, three mutations, testUnit 2041, live JSON run, tail gate with anchor_rows=197)
affects: [45-05, 45-06]

actuals:
  tokens: 21500
  tasks: 3
  commits: 5
plan_head_before: bff9662b92e9ed8eac8f1555eccb59cb20776c63
plan_head_after: e365763bb5b649454949e9c01d44f914eff2ef8d

tech-stack:
  added: []
  patterns: [one shared value-free rebuild entry point for the RowBinary and JSON routes, scripted JsonSerializable failures driven through the real writeRecordsJson, oracle exception from the serializer itself, detached-worktree mutation runs]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/JsonRouteValueFreeUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonRouteValueFreeDlqUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-04-PLAN.md
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md

key-decisions:
  - "The iteration-3 answer executed as locked: the JSON insert route gets a failure-path-only guard, the success path allocates nothing and sends the same bytes, and the rebuild keeps what retry classification reads"
  - "The shape table was amended after the first measurement (plan-assumption errors, confirmed by the coordinator): the IO-timeout control became a Jackson-free NullPointerException and the metadata-free shapes carry the value in the scripted exception's message"

requirements-completed: [UPS-02]

coverage:
  - id: D1
    description: "writeRecordsJson's serialization statements sit in one try whose catch calls throwRebuiltJacksonChain(e, value-free text) and rethrows e; a Jackson-holding chain leaves value-free with its classified links kept, a chain without a Jackson link is rethrown unchanged; writeJsonRow stays outside the try; doWriteColValue uses the same entry point so withheldJacksonChain( occurs twice"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-04-T2-OK; JsonRouteValueFreeUnitTest 42 + JsonRouteValueFreeDlqUnitTest 4 (18 red on the unguarded tree, all green after); E-11"
        status: pass
    human_judgment: false
  - id: D2
    description: "The new checks bite: J1 (guard off) turns 18 red, J2 (message in the top text) 14 red, J3 (kept suffix replaced by a withheld copy) 3 red on JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT including keepsTheRetryOutcome"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-04-T3-OK; E-12 j1/j2/j3 facts"
        status: pass
    human_judgment: false
  - id: D3
    description: "testUnit is green at 2041 tests with 2 skips, the targeted live JSON run (5 classes, 31 tests, which read back what they insert) is green on ClickHouse 26.10.3.7 x client V2, anchors read rows=197 bad=0, only ClickHouseWriter.java and Utils.java changed in src/main since the merge, and the unedited tail gate ends anchor_rows=197 result=PASS"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-04-T3-OK; E-12"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 04: JSON insert route value-free guard Summary

**A serialization failure that leaves the JSON insert route (schemaless, bypassRowBinary and both metadata-free routes) no longer carries a record value in any message, stack trace, DLQ header or log: `writeRecordsJson` rebuilds a Jackson-holding chain through the same `throwRebuiltJacksonChain` entry point the RowBinary route uses, on the failure path only, with the retriable Jackson-over-socket-timeout classification and the bytes sent unchanged.**

## Performance

- **Duration:** about 25 min of active work, including the blocker report and the amendment round trip
- **Tasks:** 3 (plus one plan-amendment commit)
- **Files:** 2 test classes created, 1 main file, the anchors file, the plan and the evidence modified

## Accomplishments

- **Task 1 (E-10, commit `7ffccd63`).** A throwaway worktree probe drove eight shapes through the real writer. Seven carry the sentinel in the thrown message, the rendered trace and the DLQ headers; none reaches a log; the only retriable shape is Jackson over a socket timeout; V1 and V2 outcomes are equal. The IO-timeout fact: `ObjectMapper.writeValueAsBytes` wraps every non-Jackson IOException into a cause-less `JsonMappingException`.
- **Task 2 (commit `0c8be1da`, E-11 `79eb0b95`).** The guard, `throwRebuiltJacksonChain`, `JSON_ROW_FAILURE_FORMAT`, the two pin classes and two anchor rows. Red-first on the unguarded tree: 46 tests, 18 red (the value-free checks of the seven value-carrying shapes plus the four DLQ cases); the control, the byte tests and the non-change checks were green. After the change all 46 pass and the 529 neighbouring tests (216-case matrix included) pass.
- **Task 3 (E-12, commit `e365763b`).** J1 18 red, J2 14 red, J3 3 red; testUnit 2041 tests (1995 + 46) with 2 skips; live JSON run 31 tests green; anchors `rows=197 bad=0`; tail gate `provenance_paths=11 anchor_commits=21 anchor_rows=197 marker_commits=39 anchor_exempt=0 result=PASS`.

## Task Commits

1. **Plan amendment** - `fb5fa4cb` (shape table corrected after the first measurement)
2. **Task 1: JSON-route tracer** - `7ffccd63` (E-10)
3. **Task 2: guard and pins** - `0c8be1da` (code, tests, anchors), `79eb0b95` (E-11)
4. **Task 3: bite and gate** - `e365763b` (E-12)

`actuals.commits: 5` is the measured `git rev-list --count plan_head_before..plan_head_after`; the docs(45-04) completion commit follows this file (SUMMARY only; STATE.md, config.json, state.json, ROADMAP and requirements are not staged).

## Facts for plan 45-05's FORK-DELTA row

- **Behaviour change of the fork's JSON route:** a serialization failure whose chain holds a Jackson link no longer reaches Processing as the original `JsonMappingException`. The top is an `IOException` with the text ``Failed to serialize a record as JSON for table `<table>`: <exception class>`` (clean table name, no value, no message), every link above the kept suffix is a withheld stand-in with the original's stack trace, a non-Jackson root becomes a same-kind stand-in, a Jackson root becomes a plain IOException that keeps the client timeout prefix, and a trusted non-Jackson IOException suffix (a socket timeout) stays the same objects, so a Jackson-over-socket-timeout failure is still retriable. A chain with no Jackson link (for example the `NullPointerException` of a null `Data` in a CDC record) is rethrown unchanged.
- **Routes covered:** SCHEMA_LESS, SCHEMA with `bypassRowBinary`, SCHEMA and DEBEZIUM_CDC with `insertFormat=json` (`MetadataFreeJsonWriter.structRow` and `cdcRow`). `writeJsonRow` (the stream write) stays outside the guard.
- **Jackson fact for the record:** a plain IOException thrown by a serializer never leaves this route as an IOException; `writeValueAsBytes` turns it into a cause-less `JsonMappingException` carrying the IOException text, so the guard does not and cannot restore an unchanged retriable IOException. Not changed.
- **Metadata-free key fact:** the metadata-free serializers write map keys themselves, so a key is not quoted in a reference chain there (only the scripted exception's own message can carry a value).
- **Reuse:** `doWriteColValue` now calls `throwRebuiltJacksonChain(e, ...)` then `throw e;`; the observable behaviour of the RowBinary route is unchanged (the 216-case matrix and the 45-03 pins are green).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking, plan assumption false] The IO-timeout control cannot exist; the metadata-free key shapes carry no value**
- **Found during:** Task 1 (first probe run)
- **Issue:** the plan expected a plain IOException from the scripted serializer to pass unchanged and retriable, and expected a Map key to reach the reference chain on the metadata-free routes. Measured: `ObjectMapper.writeValueAsBytes` wraps every non-Jackson IOException into a cause-less `JsonMappingException` on all four routes (outcome not retriable), and the metadata-free serializers write keys themselves, so their key shapes yield the message `boom` and no value.
- **Fix:** stopped and reported the blocker; the coordinator confirmed the amendment as a plan-assumption error (no user decision). The control became JSON_NO_JACKSON_NULL_DATA_CONTROL (DEBEZIUM_CDC, `bypassRowBinary=true`, a null `Data` entry: the writer's own `NullPointerException`, no Jackson link, no value); the metadata-free shapes became JSON_METADATA_FREE_STRUCT_VALUE and JSON_METADATA_FREE_CDC_VALUE (a scripted `RuntimeException(<SENTINEL>)` at a map value); JSON_SCRIPTED_IO_TIMEOUT stayed as a ninth, measured-fact line (`json_io_timeout_wrapped=yes`). `json_probe_shapes` stays 8, the pin count stays 5 x 8 + 6 = 46.
- **Files modified:** `45-04-PLAN.md` (shape table, Task 1 facts and gates, Task 2 control wording, threat row)
- **Commit:** `fb5fa4cb` (its own docs(45-04) commit, before Task 1's evidence)

**2. [Rule 1 - cosmetic bug in the new message] Table name printed in doubled backticks**
- **Found during:** Task 2, first green run
- **Issue:** the format already wraps the name in backticks and `table.getName()` is escaped (`` `json-topic` ``), which printed ``` ``json-topic`` ```.
- **Fix:** the catch uses `table.getCleanName()` (the plain table name); the pin computes the same text. The plan text said `table.getName()`; the J1 and J2 targets in E-12 record the exact replaced text.
- **Files modified:** `ClickHouseWriter.java`, `JsonRouteValueFreeUnitTest.java` (inside the Task 2 commit)
- **Commit:** `0c8be1da`

### Other observations (not rule triggers)

- The pin for a lone Jackson exception (JSON_SCRIPTED_JACKSON_ALONE) asserts the single value-free top, because the rebuild produces no separate root when the chain is one link; the first draft assumed a separate root and was corrected after the first green run. The red-first set was unaffected (that shape was red on the Jackson-link check before the change).
- `JsonRouteValueFreeUnitTest` exposes five constants, `Route`, `config` and `table` as `public` so `JsonRouteValueFreeDlqUnitTest` (another package) reuses the scripted serializer and fixtures instead of duplicating them, keeping the commit to the four planned paths.
- Spotless: `ClickHouseWriter.java` still carries only the pre-existing import-order differences (`fmt_writer_json: preexisting`); both new test files are `identical`.
- No JVM crash and no core dump occurred; every worktree was removed (`git worktree list` has one line); nothing was pushed.

**Total deviations:** 2 auto-fixed (1 blocking plan-assumption error, 1 cosmetic). **Impact:** none on the locked intent; the gate counts (8 shapes, 46 pins, 197 anchor rows, testUnit 2041) match the amended plan.

## Issues Encountered

None beyond deviation 1.

## Known Stubs

None.

## Threat Flags

None. The new test classes add no network endpoint, auth path or file access; T-45-23 to T-45-26 are mitigated as the register states (mutations J1 to J3 prove the checks bite; the probe lived only in removed worktrees and the evidence carries no sentinel).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 45-05: `src/main` differs from the merge commit only in `ClickHouseWriter.java` and `Utils.java`; the anchors mirror reads `rows=197 bad=0`; testUnit is 2041 tests with 2 skips; the full-suite expectation is 2598 + 46 = 2644 tests (the new classes are untagged) with the 18 named skips.

## Self-Check: PASSED

- Files: both new test classes, the amended plan, `45-04-SUMMARY.md` and the E-10..E-12 entries are present.
- Commits `fb5fa4cb`, `7ffccd63`, `0c8be1da`, `79eb0b95`, `e365763b` are ancestors of HEAD.
- Verify blocks `P45-04-T1-OK`, `P45-04-T2-OK` and `P45-04-T3-OK` printed on the committed tip; `git worktree list` has one line; no `ProbeJsonRouteMeasurementTest` file is tracked or in any ref's history.
