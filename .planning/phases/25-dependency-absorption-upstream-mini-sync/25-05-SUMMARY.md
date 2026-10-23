---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "05"
subsystem: connect-api-compatibility
tags: [bld-16, connect-api, javap, worker-jars, testunit-guard, fork-delta]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: merge 1 with connect-api 4.4.0 (plan 25-03); the repaired Confluent archive (plan 25-04)
provides:
  - "connect-api-refs.py: javap constant-pool derivation of the 176 org/apache/kafka references (39 C, 137 M) of the compiled connector, checked against the worker jars, 4.1.1 and 4.4.0"
  - "src/test/resources/connect-api-references.txt: the blessed, sorted list with a provenance header (image digest, jar SHA-256s, 0/0 results)"
  - "ConnectApiReferencesUnitTest: Docker-free testUnit guard that fails, naming the entry and the re-bless command, when a new Kafka reference appears"
  - "FORK-DELTA, the ClickHouseSinkTask comment and three DLQ-emulation Javadocs say the worker supplies the Connect API at runtime, citing E-12 and E-13"
affects: [25-06, 25-07, 25-08, 29]

actuals:
  tokens: 17000
  tasks: 3
  commits: 3
plan_head_before: b567458505224b0a4fdd5f9a336f2d5ff89fbd7c
plan_head_after: 8a598ba3f904771a167676ddb132dcac657f0512
commits: 3

tech-stack:
  added: []
  patterns:
    - "Bytecode reference set: parse the constant pool structurally (#class.#nameAndType), resolve connector-owned owners through superclasses then interfaces, bless only when every reference reports 0 missing, replace the list atomically"
    - "Pure-Java twin of the script in testUnit, equal to it line for line, reading the connector from a directory or a jar code source through java.nio.file.Path"

key-files:
  created:
    - .planning/scripts/connect-api-refs.py
    - src/test/resources/connect-api-references.txt
    - src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
  modified:
    - docs/FORK-DELTA.md
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/util/ThrowableTexts.java
    - src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "D-10: the reference is the jars copied out of confluentinc/cp-kafka-connect-base:7.9.7 (digest sha256:93b9afd3...4055); amd64 and native jars are byte-identical"
  - "D-13: the list lives in src/test/resources and the test reads it from the classpath as UTF-8"
  - "D-14: wording follows what E-12 measured, not what the plan expected: the Kafka 3.9 worker's SinkTask still declares onPartitionsRevoked"

requirements-completed: [BLD-16]

coverage:
  - id: D1
    description: "All 176 org/apache/kafka references (39 classes, 137 members) of the compiled connector exist in the worker's connect-api-7.9.7-ccs.jar and kafka-clients-7.9.7-ccs.jar, in 4.1.1 and in 4.4.0"
    requirement: BLD-16
    verification:
      - kind: other
        ref: "plan 25-05 Task 1 bless run (E-12): three REFERENCE lines at classes-missing 0 members-missing 0; verify REFS-BLESSED"
        status: pass
    human_judgment: false
  - id: D2
    description: "The checker can fail: connect-api-7.9.7-ccs.jar alone reports 17 missing classes and 37 missing members and exits 1; ConnectPlugin is absent from the worker jars and present in 4.4.0"
    requirement: BLD-16
    verification:
      - kind: other
        ref: "/tmp/gsd25/exec/bless-neg.txt (E-12)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The Confluent archive bundles no org/apache/kafka entry (inner jar 7,798 entries incl. directories, 0 under org/apache/kafka, 0 nested jars)"
    requirement: BLD-16
    verification:
      - kind: other
        ref: "archive-kafka-count.txt over archive-m1-repaired.zip (E-12)"
        status: pass
    human_judgment: false
  - id: D4
    description: "ConnectApiReferencesUnitTest: 3 tests green; red with AbstractConfig.getBoolean removed from the list (names the entry and the re-bless command); derived set equals the script's 176 entries (diff empty); full testUnit 844 tests, 842 succeeded, 0 failed, 2 skipped"
    requirement: BLD-16
    verification:
      - kind: unit
        ref: "ConnectApiReferencesUnitTest (testUnit --rerun); refs-red.xml (E-13); verify REFS-TEST-GREEN"
        status: pass
    human_judgment: false
  - id: D5
    description: "The D-14 commit changes only comment and Javadoc lines in the four Java files; each of the five files contains 'the worker supplies the Connect API at runtime'; FORK-DELTA cites E-12 and no longer calls 4.1.1 the production API version; the onPartitionsRevoked signature stays on one line"
    requirement: BLD-16
    verification:
      - kind: other
        ref: "plan 25-05 Task 3 verify D14-WORDING; git diff pre-v1.7.0-deps HEAD -- src/main shows 4 comment lines changed"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 05: Connect API proof against the production worker Summary

**The connector's 176 Kafka references (39 classes, 137 members) are proven present in the production worker's `connect-api-7.9.7-ccs.jar` and `kafka-clients-7.9.7-ccs.jar`, in 4.1.1 and in 4.4.0; the blessed list is a standing `testUnit` guard, and the Connect API wording now says the worker supplies the API at runtime.**

## Performance

- **Duration:** about 25 min (two image pulls and copies, one 30 s script run, five short Gradle runs)
- **Completed:** 2026-10-21
- **Tasks:** 3 of 3
- **Files modified:** 9 (3 created)

## Accomplishments

- E-12: `cp-kafka-connect-base:7.9.7` digest `sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055`, Temurin 17.0.19 on both platforms; the three jars hashed on amd64 and native copies and byte-identical (research A3 resolved). Bless run: `derived classes 39 members 137 total 176`; `REFERENCE worker`, `REFERENCE 4.1.1` and `REFERENCE 4.4.0` all `classes-missing 0 members-missing 0`. The derived set equals the research prototype's 176 lines. Negative controls: `ccs-api-only` reports 17 classes and 37 members missing (exit 1); `ConnectPlugin` is absent from the worker jars and present in 4.4.0.
- E-13: `ConnectApiReferencesUnitTest` red with one list line removed (names the entry and the re-bless command), green with the full list, derived set equal to the script's (diff empty), full `testUnit` green.
- D-14: wording commit after the evidence; the four-line comment above `onPartitionsRevoked` is the only `src/main` change.

## Task Commits

1. **Task 1: worker jars, script, bless run, reference list (E-12)** - `00f76ef2` (build)
2. **Task 2: ConnectApiReferencesUnitTest (E-13)** - `db2521c7` (test)
3. **Task 3: D-14 wording** - `8a598ba3` (docs)

**Plan metadata:** the `docs(25-05)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `.planning/scripts/connect-api-refs.py` - new: `derive_references`, `check_reference`, `write_list`, `main` (adds an optional `--image-note` for the list header)
- `src/test/resources/connect-api-references.txt` - new: provenance header and 176 sorted entries
- `src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java` - new: 3 tests
- `docs/FORK-DELTA.md` - the `onPartitionsRevoked` row, the dependency-pins row and the test-doubles row
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` - comment above `onPartitionsRevoked` only
- `ValueFreeDlqHeadersUnitTest.java`, `ThrowableTexts.java`, `InMemoryDLQ.java` - Javadoc only
- `25-COMMAND-EVIDENCE.md` - E-12 and E-13, index rows flipped to pass

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The plan's expected `onPartitionsRevoked` fact is contradicted by the worker's jar**
- **Found during:** Task 1 step 6
- **Issue:** the plan expected `javap` to show no `onPartitionsRevoked` in `SinkTask` for the worker, 4.1.1 and 4.4.0, and Task 3 asked for "SinkTask has had no such method since Kafka 3.0". Measured: `connect-api-7.9.7-ccs.jar` (Kafka 3.9) declares it (`close(Collection)` calls it by default); 4.1.1 and 4.4.0 do not.
- **Fix:** the FORK-DELTA row and the `ClickHouseSinkTask` comment state only what E-12 shows (absent from the compile-time 4.4.0 and 4.1.1, so `@Override` cannot compile; the worker's `SinkTask` may still declare it, so the method stays). The unmeasured "pre-3.0 worker" claims were dropped. This honours the plan prohibition on claims no transcript backs.
- **Files modified:** `docs/FORK-DELTA.md`, `ClickHouseSinkTask.java`
- **Commit:** `8a598ba3`

**2. [Rule 3 - Blocking] The test classpath holds the connector as a jar, not a classes directory**
- **Found during:** Task 2, first run
- **Issue:** the plan's "code source must be a directory" assertion failed: the code source is `build/libs/clickhouse-kafka-connect-v1.7.0_gg3.jar` (test-fixtures dependency on the main project).
- **Fix:** `derivedFromConnectorCodeSource()` uses the directory when it is one, otherwise opens the jar as a zip file system and passes its root to the same `derivedReferences(Path)`; it fails naming the location if neither. Recorded in E-13.
- **Files modified:** `ConnectApiReferencesUnitTest.java`
- **Commit:** `db2521c7`

### Notes

- The negative control counted 37 missing members (research: 36); the 17 classes match. The live number is recorded.
- The plan's verify strings exclude only `gradlew.bat`; the clean-tree checks here also excluded `.planning/state.json`, the pre-existing dirty item named in the dispatch. `state.json`, `gradlew.bat` and `milestone.lock` were never staged.
- `spotlessApply` is import-order and unused-import only in this build (`googleJavaFormat()` is commented out), so the new class and the four edited files were byte-identical after the per-file recipe.
- Commit trailers use `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` as the dispatch specifies.

## Issues Encountered

None blocking. The OpenJ9 Mockito crash did not occur and no `core.*` dump was present. Containers `gsd25-ccs-amd64` and `gsd25-ccs` were removed after `docker cp`; `docker ps -a --filter name=gsd25` lists nothing; the formatting worktree was removed and pruned.

## Known Stubs

None.

## Threat Flags

None. The image jars were copied into new empty directories, hashed and only read by `javap` and `zipfile` (T-25-14); the connector cannot silently reach for a member the worker lacks (T-25-15); the wording commit follows the evidence and cites E-12 (T-25-16); no container remains (T-25-17).

## Coverage

| Requirement | Evidence |
|-------------|----------|
| BLD-16 | E-12 (worker jars, bless run 0/0 for worker, 4.1.1, 4.4.0, negative controls, archive has 0 org/apache/kafka entries), E-13 (red/green, script equality), D-14 commit with `D14-WORDING` |

## Next Plan Readiness

Plan 25-06 can proceed. Plan 25-08 re-runs the bless script after merge 2 only if `src/main` changes there; `ConnectApiReferencesUnitTest` will fail `testUnit` if a new reference appears. Nothing is pushed; `custom_build` is untouched.

## Self-Check: PASSED

- FOUND: .planning/scripts/connect-api-refs.py
- FOUND: src/test/resources/connect-api-references.txt
- FOUND: src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
- FOUND: commits 00f76ef2, db2521c7, 8a598ba3
