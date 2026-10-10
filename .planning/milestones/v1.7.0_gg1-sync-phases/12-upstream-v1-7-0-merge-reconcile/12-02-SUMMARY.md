---
phase: 12-upstream-v1-7-0-merge-reconcile
plan: "02"
subsystem: infra
tags: [merge-checklist, rowbinary, map-null, upstream-820, lz4, gradle-capability, byte-pins]

requires:
  - phase: 12-01
    provides: "merge commit M (54e63648) on sync/v1.7.0, verify-merge-invariants.sh, conflicted-paths-v1.7.0.txt, fork-anchors.txt"
provides:
  - "MERGE-CHECKLIST-v1.7.0.md: SYNC-08 living gate (5 conflicted rows, 22 upstream paths, 33 per-hunk verdicts, Recorded Facts 17-21)"
  - "Scoped MAP null guard in ClickHouseWriter.doWriteColValue so upstream #820's empty-map bytes are reachable (WRT-06)"
  - "Three Docker-free byte pins next to the CR-02 pins, zero existing assertions changed (WRT-09)"
  - "Single LZ4 capability rule in build.gradle.kts (upstream's), stale 1.10.2 wording removed (BLD-09)"
affects: [12-03, 12-04, 12-05, phase-13, phase-14, phase-15]

plan_head_before: 156f9adbb9a364b4e4001a4652717b3a1243954f
plan_head_after: a75bd4647a6f4eb2eea4bc6fced2fdbfaada1ed8

actuals:
  tokens: 6800
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Guard scoped by column nullability so a fork early-return no longer shadows an upstream branch"
    - "Byte pins that build their own writer and stream, calling the protected doWriteCol from the same package"

key-files:
  created:
    - .planning/MERGE-CHECKLIST-v1.7.0.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
    - .planning/scripts/fork-anchors.txt
    - build.gradle.kts
    - .claude/CLAUDE.md
    - .planning/codebase/STACK.md
    - .planning/MERGE-CHECKLIST.md

key-decisions:
  - "WRT-09 is delivered as evidence plus additions: the only MAP CR-02 pin uses a nullable column and keeps {0x01}, so no existing assertion is edited; the non-nullable #820 pins are new, each with its 2026-10-07 rationale"
  - "Rows that later commits close stay pending with an owning phase in the checklist (writer, helper, build.gradle.kts rule, chcTmp site, D-17 constants, cleanupExtraFields, the #820 MAP block, the lz4Java entry)"
  - "The checklist table separator rows begin with '| ' so the plan's own row-count awk (which drops two '| ' lines) counts data rows correctly"

patterns-established:
  - "bash, not zsh, for plan verification snippets that rely on word splitting of command substitutions"

requirements-completed: [SYNC-08, WRT-06, WRT-09, BLD-09]

coverage:
  - id: D1
    description: "SYNC-08 checklist: 22 upstream paths with predicted and actual class, 5 conflicted rows, one verdict row per upstream hunk (33) in the 8 fork-touched files, Recorded Facts from 17"
    requirement: SYNC-08
    verification:
      - kind: other
        ref: "bash row-count check over MERGE-CHECKLIST-v1.7.0.md (Section 2 count 22 x 1, Section 1 count 5 x 1, hunks=33 rows=33)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A null optional Map into a non-nullable Map column writes upstream #820's bytes ({0x00} RowBinary, {0x00,0x00} RowBinaryWithDefaults); nullable case keeps {0x01}; existing CR-02 pins unchanged"
    requirement: WRT-06
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java (6 tests, 0 failures)"
        status: pass
      - kind: integration
        ref: "ClickHouseSinkTaskWithSchemaTest.nullMapTypeTest, nullMapTypeJsonInsertTest, ClickHouseSinkTaskSchemalessTest.nullMapTypesTest on latest x V2"
        status: pass
    human_judgment: true
    rationale: "WRT-09 interpretation (12-RESEARCH Open Question 1): zero existing assertions changed; the user may have expected a visibly re-pinned existing assertion. Needs the user's confirmation."
  - id: D3
    description: "One LZ4 capability rule; dependencyInsight shows exactly one at.yawk.lz4:lz4-java headline (1.10.4) and no unreplaced org.lz4 headline on runtimeClasspath and clickhouseDependencies"
    requirement: BLD-09
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q dependencyInsight --dependency lz4-java --configuration runtimeClasspath|clickhouseDependencies"
        status: pass
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava testUnit --rerun (336 tests, 0 failed, 2 skipped)"
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-10-07
status: complete
---

# Phase 12 Plan 02: Merge Checklist, Scoped MAP Guard and Single LZ4 Rule Summary

**SYNC-08 checklist with 33 per-hunk verdicts, the fork's MAP null guard scoped to Nullable columns so upstream #820's `00` / `00 00` empty-map bytes are reachable and pinned Docker-free, and the fork's duplicate LZ4 capability rule deleted with lz4-java still resolving to upstream's 1.10.4.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-10-07T19:38:04Z
- **Completed:** 2026-10-07T19:45:14Z
- **Tasks:** 3
- **Files modified:** 8 (1 created, 7 modified)

## Accomplishments

- `.planning/MERGE-CHECKLIST-v1.7.0.md` records all 22 upstream-changed paths (5 conflict, 3 auto-merged, 14 upstream-only; predicted equals actual), the 5 conflicted rows, and 33 per-hunk verdicts inside the 8 fork-touched files (13 hunks in conflict regions, 20 auto-merged), plus Recorded Facts 17-21.
- Two verdicts are "wrong by intent or surviving": the MAP #820 block shadowed by the fork guard (fixed here for the top level) and the fork's LZ4 rule surviving beside upstream's (fixed here).
- `doWriteColValue` `case MAP:` now fires its null-marker branch only for `col.isNullable()`; a non-nullable Map takes upstream's `mapTmp == null` path.
- Three new Docker-free pins sit beside the CR-02 pins in `NestedNullableCompositeWriteTest`, each with the 2026-10-07 rationale.
- `build.gradle.kts` has one `withCapability("org.lz4:lz4-java")` rule; `lz4-java` is unchanged at 1.10.4 on both configurations.

## Task Commits

1. **Task 1: SYNC-08 checklist** - `0874d601` (docs)
2. **Task 2: WRT, scoped MAP null guard plus #820 byte pins** - `79664ce3` (fix)
3. **Task 3: BLD, drop the duplicate LZ4 rule and fix 1.10.2 wording** - `a75bd464` (build)

**Plan metadata:** committed separately (docs: complete plan).

Task 2 is `tdd="true"` but the plan prescribes one commit, so RED and GREEN share it; the RED evidence below was observed on the merged writer before the guard edit and classified `RED_EVIDENCE_OK` by `gsd_run check tdd-red-evidence` (JUnit format, target test matched, `reason: target_test_failed`).

## RED and GREEN (Task 2)

**RED** (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*NestedNullableCompositeWriteTest'` against the unmodified merged writer): 6 tests, 4 succeeded, 2 failed.

- `nonNullableMapNullWritesUpstream820EmptyMapInRowBinary`: `AssertionFailedError: array lengths differ, expected: <1> but was: <2>`
- `nonNullableMapNullWritesUpstream820EmptyMapInRowBinaryWithDefaults`: `AssertionFailedError: array contents differ at index [1], expected: <0> but was: <1>`

The actual bytes are `{0x00, 0x01}` in both cases: `doWriteCol`'s size byte followed by the fork guard's stray null marker, which is the shadow. Semantic assessment: the target executed and failed on the planned `assertArrayEquals`, for the intended reason; the other four tests (three CR-02 pins and the nullable `doWriteCol` pin) were green.

Upstream's live `ClickHouseSinkTaskWithSchemaTest.nullMapTypeTest` was also red at M (`RuntimeException: Number of records: 1000`), while `nullMapTypeJsonInsertTest` and `ClickHouseSinkTaskSchemalessTest.nullMapTypesTest` were green. Evidence only, not a gate.

**GREEN** (after the one-condition change `if (value.getObject() == null && col.isNullable()) {`):

- `NestedNullableCompositeWriteTest`: tests="6" failures="0" errors="0".
- Full `testUnit`: 336 tests, 0 failed, 2 skipped (333 at M plus the 3 new).
- Live on `latest` x V2: `nullMapTypeTest`, `nullMapTypeJsonInsertTest`, `nullMapTypesTest` all PASSED, unmodified.

## WRT-09 evidence: zero existing assertions changed

- `git diff --numstat M HEAD -- NestedNullableCompositeWriteTest.java` prints `48 0`: 48 additions, 0 deletions. The only MAP CR-02 pin builds `isNullable(true)`, so the scoped guard keeps it at `{0x01}`.
- `git diff --stat M HEAD -- src/main` after the WRT commit: only `ClickHouseWriter.java`, 4 insertions and 1 deletion (the condition line), all inside `case MAP:`.
- `grep -rln 'Type.MAP' src/test` prints `ColumnTest.java`, `NestedNullableCompositeWriteTest.java`, `TableTest.java` and `StructToJsonMapTest.java`. The last is new with v1.7.0 and matches `Schema.Type.MAP` (Kafka's schema type, a converter-level assertion), not writer bytes, so no other test pins pre-#820 Map null bytes.
- `grep -c '2026-10-07'` on the test file prints 3, one rationale next to each new assertion.

**Nested-Map observation for Phase 13 (RESEARCH Pitfall 2).** A server-derived MAP column is never Nullable, so after this change `col.isNullable()` is true for a MAP only on a hand-built `Column`. A nested null Map (for example a null inner value of `Map(String, Map(...))`) now falls to upstream's `mapTmp == null` branch: it writes varint 0 with defaults and nothing without defaults. The pre-merge fork wrote a stray `0x01` there, so this is not a regression. It is WR-02's territory (WRT-07, WRT-08) and the guard was deliberately not widened.

## LZ4 resolution before and after the BLD commit (BLD-09)

Headline lines are identical on both configurations; the fork rule was redundant, not masking.

Before (HEAD at M's code, both rules present; Recorded Fact 19):

- `runtimeClasspath`: headline `at.yawk.lz4:lz4-java:1.10.4`; edges `at.yawk.lz4:lz4-java:1.10.2 -> 1.10.4` (client-v2 0.9.6) and `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.10.4` (kafka-clients 4.1.1); reasons "between versions 1.10.4 and 1.10.2" and "On capability org.lz4:lz4-java client-v2 uses the maintained at.yawk.lz4 fork…".
- `clickhouseDependencies`: headline `at.yawk.lz4:lz4-java:1.10.4`; edge `at.yawk.lz4:lz4-java:1.10.2 -> 1.10.4`.

After (fork block deleted, one `withCapability` line left):

- `runtimeClasspath`: the same headline, the same two edges, the same two reasons. `sort -u` of headline lines is exactly one line, `at.yawk.lz4:lz4-java:1.10.4`; no `^org.lz4:lz4-java:[0-9.]+$` line without `->`.
- `clickhouseDependencies`: the same headline and edge, exactly one distinct headline.

`git diff M HEAD -- gradle/libs.versions.toml` is empty; `grep -c 'configurations.configureEach' build.gradle.kts` went from 1 at M to 0. `git diff --numstat M HEAD -- .planning/MERGE-CHECKLIST.md` prints `1 1`.

## Files Created/Modified

- `.planning/MERGE-CHECKLIST-v1.7.0.md` - SYNC-08 gate: Sections 1, 2, 2a and Recorded Facts 17-21
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - MAP guard scoped to Nullable columns, with the reason in the comment
- `src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java` - three new byte pins
- `.planning/scripts/fork-anchors.txt` - WRT-06 anchor added (39 to 40 rows), BLD-09 `withCapability` 2 to 1
- `build.gradle.kts` - fork LZ4 block deleted (8 lines)
- `.claude/CLAUDE.md`, `.planning/codebase/STACK.md`, `.planning/MERGE-CHECKLIST.md` - stale 1.10.2 wording replaced

## Decisions Made

- WRT-09 is met by evidence plus new pins; no passing pin was edited to manufacture a change (see the open `human_judgment` on D2).
- Checklist rows that later commits close are left `pending` with their owning phase: the build.gradle.kts rule hunk (12), the writer and helper rows (12, closed by 12-03 for the helper), the `chcTmp` site and D-17 constants (12), the #820 MAP block and `cleanupExtraFields` (13), and `lz4Java` (14, BLD-10).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Table separator style in the new checklist**
- **Found during:** Task 1 verification
- **Issue:** The plan's own Section 2a check pipes `/^\| /` lines through `tail -n +3`. A GitHub-style separator `|---|---|` does not begin with `| `, so the check dropped the first data row and reported rows=32 against hunks=33.
- **Fix:** Separator rows written as `| --- | --- | ... |` with the right column count per table.
- **Files modified:** `.planning/MERGE-CHECKLIST-v1.7.0.md`
- **Verification:** `hunks=33 rows=33`; the Section 1 and Section 2 per-path counts all equal 1.
- **Committed in:** `0874d601`

---

**Total deviations:** 1 auto-fixed (1 blocking). **Impact on plan:** formatting only; no content or scope change.

## Issues Encountered

- zsh does not word-split `$(...)` in the plan's verify snippets (`for p in $(git diff --name-only ...)`, `git diff -- $(comm ...)`), so a first hunk-listing attempt returned nothing. The checks were re-run under `bash`.
- `nullMapTypeTest` was red at M and green after the guard, which confirms the shadow was observable through upstream's own live test.

## Known Stubs

None.

## Threat Flags

None. The change narrows an existing guard; no new endpoint, auth path or schema surface.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 12-03 (CMP) can delete the dead D-17 constants and set the `NETWORK_COMPRESSION_METHOD_` anchor to 0; the checklist's helper, `chcTmp` and V1/V2 compression hunk rows are pending on it.
- Phase 13 inherits the nested-Map observation above and the two pending rows it owns (the #820 MAP block, `cleanupExtraFields`).
- Open for the user: confirm the WRT-09 interpretation (zero existing assertions changed, three new non-nullable and nullable pins added), or name the assertion expected to change.

## Self-Check: PASSED

- FOUND: `.planning/MERGE-CHECKLIST-v1.7.0.md`, `src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java`, `.planning/scripts/fork-anchors.txt`
- FOUND commits (ancestors of HEAD): `0874d601`, `79664ce3`, `a75bd464`
- `bash .planning/scripts/verify-merge-invariants.sh M HEAD`: `result=PASS` after each commit (anchor_rows=40 at the end)
- `grep -cF 'if (value.getObject() == null && col.isNullable()) {'` on the writer prints 1; `withCapability("org.lz4:lz4-java")` count in build.gradle.kts prints 1

---
*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Completed: 2026-10-07*
