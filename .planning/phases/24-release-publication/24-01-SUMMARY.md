---
phase: 24-release-publication
plan: "01"
subsystem: release
tags: [release, version-bump, jmx, jackson, json-as-string, createConfluentArchive, lombok, evidence]

requires:
  - phase: 23-declared-nullability-hardening-review-closure
    provides: the final src/main tree whose shipped artifact is measured here
provides:
  - "VERSION v1.7.0_gg3 with its fork-anchors VERSION row, in one commit (PUB-09)"
  - "JsonAsStringKeyResidualLiveTest: committed live pin of the Jackson Map-key residual (D-03), D-04 branch 1"
  - "24-COMMAND-EVIDENCE.md E-01..E-05: bump, MBean and client name, Map-key residual, shipped-archive diff, BLD-14 re-check"
affects: [24-02, 24-03, 24-04, 24-05, 24-06, 24-07]

actuals:
  tokens: 9900
  tasks: 3
  commits: 4
plan_head_before: 4ca26f1bd466c47c40a9decfe8f3a54a2ab14e8b
plan_head_after: a60ceec5f393c51b26c7c63c6aa30d9af59c6ed6
commits: 4

tech-stack:
  added: []
  patterns:
    - "Live test of a failure route asserts the route's own exception class in the cause chain (non-vacuity) and carries a control without the setting"
    - "Evidence entries carry sentinels only, never record data"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "D-04 branch 1: only a Map key reaches the task status chain, the DLQ stack-trace header and the Error inserting records throwable; no value, no List content; the residual stays accepted with no code change"
  - "The shipped jar differs between the pre-merge tip and the gg3 tip by the Phase 22-23 classes, one added class and four version-bearing entries (Version, MANIFEST, ClickHouseHelperClient, SinkTaskStatistics); the bumps change no jar entry"

requirements-completed: [PUB-09, ROLL-03]

coverage:
  - id: D1
    description: "VERSION reads v1.7.0_gg3 and the generated ARTIFACT_VERSION agrees after a clean build; VERSION and its anchor row changed in one commit; anchors mirror rows=69 bad=0"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-01 Task 1 verify commands VERSION-GG3-OK and ANCHORS-MIRROR-OK (E-01)"
        status: pass
    human_judgment: false
  - id: D2
    description: "MBean version= segment (per-task and per-topic bean) and the client name read v1.7.0_gg3 on a live task; the same probe on tag v1.7.0_gg2 shows v1.7.0_gg2; server 26.9.19.9"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-01 Task 1 verify command MBEAN-PROBE-OK (E-02)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Committed live test pins the Jackson Map-key residual on latest x V2; D-04 branch 1 recorded once in E-03"
    requirement: ROLL-03
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java (4 tests, 0 skipped, 0 failures)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Shipped-archive diff across tag v1.7.0_gg2, the pre-merge tip and the gg3 tip, and the BLD-14 re-check with a negative control, recorded as E-04 and E-05"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "E-04 cmp.py outputs and /tmp/gsd24-scripts/artifact-diff-check.py (passes only with the two version-inlining classes added to the plan's allowed set); E-05 BLD14-OK"
        status: pass
    human_judgment: true
    rationale: "The plan's ARTIFACT-DIFF-OK allowed set was too narrow and was widened by two measured classes; a reviewer should accept that reading before the rollout note quotes it"

duration: 10min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 01: Release identity and measured inputs Summary

**VERSION v1.7.0_gg3 with its anchor row in one commit, a committed live test that pins the Jackson Map-key residual (D-04 branch 1), and measured MBean, client-name, shipped-archive and BLD-14 evidence for the release documents**

## Performance

- **Duration:** 10 min
- **Started:** 2026-10-20T16:09:50Z
- **Completed:** 2026-10-20T16:20:20Z
- **Tasks:** 3
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments

- `VERSION` and the `VERSION` row of `.planning/scripts/fork-anchors.txt` changed in one commit that touches nothing else (`3c62abe2`). After `clean compileJava` the generated `ARTIFACT_VERSION` is `v1.7.0_gg3`, and the anchors mirror reads `rows=69 bad=0`.
- A live task built from the gg3 tip registers MBeans with `version=v1.7.0_gg3` (per-task and per-topic bean) and sends the client name `clickhouse-kafka-connect/v1.7.0_gg3`. The same probe on tag `v1.7.0_gg2` shows `v1.7.0_gg2` for both. Server `26.9.19.9`, client V2 (E-02).
- `JsonAsStringKeyResidualLiveTest` (4 tests) shows, through `ClickHouseSinkTask.put()` on `latest` x V2, that with `input_format_binary_read_json_as_string=1` a Map key (and only the key) reaches three destinations. D-04 branch 1 is recorded in E-03.
- The shipped `createConfluentArchive` jar is identical per entry at tag `v1.7.0_gg2` and at the pre-merge tip (7375 entries, 0 changed, same whole-zip SHA-256). `slf4j-reload4j` and `reload4j` are not in the archive. BLD-14 is re-established for Lombok 1.18.48 with a negative control (E-04, E-05).

### D-04 destination table (E-03, server 26.9.19.9, client V2)

| Destination | Map key | Any value |
|-------------|---------|-----------|
| Task status cause chain | yes | no |
| DLQ message header | no | no |
| DLQ stack-trace header | yes | no |
| `Error inserting records` throwable | yes | no |
| `Error writing value of` line | no | no |

The List case names an index (`java.util.ArrayList[1]`), so no record text reaches any destination. No D-04 checkpoint was raised: branch 3 did not occur.

### Archive and BLD-14 results

- `a246ca01` (`v1.7.0_gg2`) versus `6473e24f` (pre-merge tip): `git diff --stat` lists exactly `build.gradle.kts` and `gradle/libs.versions.toml`. Whole-zip SHA-256 `2e3583a9a0bd...` and inner jar `95256b62c112...` are identical; 0 only-in-a, 0 only-in-b, 0 changed.
- Pre-merge tip versus gg3 tip: one added class (`util/WithheldMessageException.class`) and 11 changed entries: the seven Phase 22-23 classes plus `Version.class`, `META-INF/MANIFEST.MF`, `ClickHouseHelperClient.class` and `SinkTaskStatistics.class`. `manifest.json` differs only in `version`; `release_date` is the build date. No path containing `reload4j` exists in any extracted tree.
- BLD-14: with `-Xmaxwarns 100000` the gg2 and gg3 trees print 273 warnings each and none says `Ambiguous: Jackson2 and Jackson3`. The control without `lombok.jacksonized.jacksonVersion += 2` prints 274 and names `ClickHouseFieldDescriptor.java`.

## Task Commits

1. **Task 1: v1.7.0_gg3 end to end (tracer)** - `3c62abe2` (chore: VERSION and anchor row), `1994cb42` (docs: E-01, E-02). Tracer verify re-run end to end after the commits: `VERSION-GG3-OK`, `ANCHORS-MIRROR-OK`, `MBEAN-PROBE-OK`; expansion followed.
2. **Task 2: Jackson Map-key residual live pin** - `98b9e04d` (test, with E-03)
3. **Task 3: shipped-archive diff and BLD-14** - `a60ceec5` (docs, E-04 and E-05)

**Plan metadata:** the commit of this SUMMARY (docs: complete plan)

## Files Created/Modified

- `VERSION` - release identity `v1.7.0_gg3`
- `.planning/scripts/fork-anchors.txt` - the `VERSION` row moved to `v1.7.0_gg3`
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java` - new live pin (D-03), extends `ClickHouseBase`
- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` - E-01..E-05

## Decisions Made

- D-04 branch 1 applies (see the table above): no code change; the residual stays accepted (22-UAT test 3, 22-SECURITY R-22-02).
- The test logs the server version from a `@BeforeAll` method, because the plan fixes the class at exactly four `@Test` methods and asks for one version line.
- The artifact check was widened by exactly the two classes that carry the inlined `Version.ARTIFACT_VERSION` constant, after a control build proved it (see Deviations).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The control case expected a text that `put()` never surfaces**
- **Found during:** Task 2, first live run (3 of 4 tests green, the control red)
- **Issue:** The plan (and research R2.4) expected the setting-absent control to show `Writing JSON in binary is not supported yet`. That `RuntimeException` sits in the writer's JSON branch, but schema validation refuses the JSON column first, so `put()` surfaces `Column [j] has unsupported type [JSON] for RowBinary serialization. Use insertFormat=json or bypassRowBinary=true instead.`
- **Fix:** The control asserts the measured validation text, still asserts no Jackson class and no sentinel. The original string stays only in the class comment, which also explains why. This is recorded in E-03.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java`
- **Verification:** `D03-LIVE-GREEN` (4 tests, 0 skipped, 0 failures, 0 errors), `D03-STATIC-OK`, `SPOTLESS-BASELINE-EQUAL`
- **Committed in:** `98b9e04d`

### Plan defect reported, not waved through

**2. [Research assumption A3 - surprise] The artifact-diff allowed set missed two classes**
- **Found during:** Task 3, `cmp.py` of the pre-merge jar against the gg3 jar
- **Issue:** The plan's ARTIFACT-DIFF-OK allowed set names nine changed entries; the measurement shows eleven. The extra two, `ClickHouseHelperClient.class` and `SinkTaskStatistics.class`, read the `static final String` literal `Version.ARTIFACT_VERSION`, which javac inlines, so they change with the version bump. Run with the plan's set the check exits 1.
- **Evidence it is the version bump and nothing else:** both classes hold `v1.7.0_gg2` in the pre-merge jar and `v1.7.0_gg3` in the gg3 jar; a control worktree at the same commit with only `VERSION` set back to `v1.7.0_gg2` compared against the pre-merge jar shows exactly the seven Phase 22-23 classes and the one added class (E-04).
- **Action:** the check was run with those two classes added (`/tmp/gsd24-scripts/artifact-diff-check.py`) and the plan's verify text for Task 3 command 1 is therefore not met literally. Plans 24-03 and 24-04 should word the jar delta from E-04 (eleven changed entries, four of them version-bearing), not from the plan's nine-entry list.
- **Files modified:** none (evidence only)
- **Committed in:** `a60ceec5`

---

**Total deviations:** 1 auto-fixed (Rule 1), 1 plan defect reported with a proven explanation.
**Impact on plan:** Both are corrections of an assumed text or an assumed list to the measured one. No scope creep, no `src/main` or existing-test change.

### Worktree stand-ins

- `wt-gg2` (tag `v1.7.0_gg2`) and `wt-tip` carry the scratch probe `Phase24RolloutProbe.java` as an untracked file. `wt-tip` was moved to `98b9e04d` for the builds and is not at the final plan-01 commit, which only adds evidence under `.planning/`.
- The gg2 archive was built in `wt-gg2` without `clean` (the operator form the rollout note will quote); `wt-pre` and `wt-tip` were built with `clean`. All three on 2026-10-20 UTC.
- `wt-gg2` and `wt-tip` had their `build/` removed by the `clean compileJava` runs of the BLD-14 step; the extracted trees `/tmp/gsd24/x-gg2`, `x-pre`, `x-tip` (and `x-ver`, the control) remain for plan 24-02.
- Extra throwaway worktree `wt-ver` (control for the two version-inlining classes) was created and removed. `wt-pre`, `wt-neg` and `wt-ver` are removed; `git worktree list` shows the main tree, `wt-gg2` and `wt-tip`.

## Issues Encountered

- The shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal `null`; every Gradle call used `env -u TESTCONTAINERS_HOST_OVERRIDE`. No OpenJ9 crash occurred in any run.
- The background-task completion notice fired once for the wrapper shell before the inner Gradle runs finished; the executor waited on the flag files instead.

## Known Stubs

None.

## Threat Flags

None. The only new file under `src/` is a test; no endpoint, auth path or schema changed. T-24-01: the test, probe and evidence hold the four sentinels and constants only. T-24-02: one commit for both files, checked by VERSION-GG3-OK. T-24-04: archives were extracted into new empty directories and the scripts live in `/tmp/gsd24-scripts`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 24-02 can reuse `/tmp/gsd24/wt-gg2`, `/tmp/gsd24/wt-tip` (move it to the current HEAD first), `/tmp/gsd24/probe` and the `x-*` trees.
- 24-03 and 24-04 can quote E-02 (MBean and client name), E-03 (D-04 branch 1 and the destination table), E-04 (jar delta) and E-05 (BLD-14). The control text in E-03 and the eleven-entry jar delta in E-04 differ from the plan text, as noted above.
- No blocker. D-04 branch 3 did not fire, so nothing waits on the user.

## Self-Check: PASSED

- Files: `VERSION`, `.planning/scripts/fork-anchors.txt`, `JsonAsStringKeyResidualLiveTest.java`, `24-COMMAND-EVIDENCE.md` all exist.
- Commits `3c62abe2`, `1994cb42`, `98b9e04d`, `a60ceec5` are ancestors of HEAD; `git rev-list --count 4ca26f1b..HEAD` = 4 before this SUMMARY.
- Acceptance: `VERSION-GG3-OK`, `ANCHORS-MIRROR-OK`, `MBEAN-PROBE-OK`, `D03-LIVE-GREEN`, `D03-STATIC-OK`, `SPOTLESS-BASELINE-EQUAL`, `BLD14-OK` printed; `ARTIFACT-DIFF-OK` only with the two-class widening (Deviation 2).

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
