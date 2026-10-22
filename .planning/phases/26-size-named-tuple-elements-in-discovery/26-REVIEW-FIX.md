---
phase: 26-size-named-tuple-elements-in-discovery
fixed_at: 2026-10-22T09:50:00+03:00
review_path: .planning/phases/26-size-named-tuple-elements-in-discovery/26-REVIEW.md
iteration: 1
fix_scope: critical_warning
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 26: Code Review Fix Report

**Fixed at:** 2026-10-22
**Source review:** `.planning/phases/26-size-named-tuple-elements-in-discovery/26-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (WR-01, WR-02)
- Fixed: 2
- Skipped: 0

**Isolation:** no worktree was created. `workflow.use_worktrees` is absent from `.planning/config.json` (default true),
but the dispatching instruction fixed the repo root as the working directory and asked for explicit-path commits on
`sync/v1.7.0`, so I edited and committed there directly (deviation from the default worktree setup, noted here). No temp
branch or recovery sentinel exists, so there was nothing to clean up. All verification ran in the main checkout and is
reproducible from it.

## Fixed Issues

### WR-01: Unreadable-parent path is fail-open (silently re-creates the short tuple) but is documented and tested as "fails closed"

**Status:** fixed (documentation and test-name correction; production behaviour deliberately unchanged)
**Commit:** `595d0dc7`
**Files changed:**
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java`
- `CHANGELOG.md`
- `docs/FORK-DELTA.md`

**Route taken:** keep the behaviour, make every claim accurate (the reviewer's second option).

**Why not fail closed (`addRow` returns false):**
- The behaviour-preservation constraint in `.claude/CLAUDE.md` limits code changes to genuine bug fixes because the fork
  runs in production. The undecidable path is the pre-phase behaviour for this case (earlier releases dropped every bare
  `size`/`size0` row), so it is not a regression introduced by Phase 26.
- Decision D-03 in `26-CONTEXT.md` weighed exactly this and rejected "refuse the table" because it "turns a parse edge
  case into a whole-table outage for shapes that store fine today". The code, `DescribeTableBuilder` Javadoc
  ("as earlier releases did"), the unreadable-parent WARN text and the FORK-DELTA row all already implement and describe
  that decision. Only the word "closed" in the test name and one comment, plus a missing CHANGELOG caveat, were wrong.
- Reachability looks lower than the review suggests: the parent root column must itself be parsed by
  `Column.extractColumn`, so a type the client cannot parse generally fails at the root, not at a sub-column. I did not
  prove this against a server, so it is stated here as an assessment, not a measurement.

**Reviewer claims checked against the code:**
- Line references `DescribeTableBuilder.java:200-219`, `DescribeTableBuilderUnitTest.java:254-285` and
  `DescribeFixturesUnitTest.java:33` were correct.
- The test pins `assertEquals(List.of("c.x.v"), fieldNamesOf(unreadable))`, a short Tuple with the table served. Confirmed.
- FORK-DELTA rows already said "keeps the earlier behaviour"; confirmed accurate, but I made the consequence explicit.
- CHANGELOG said "at any depth ... no longer dropped" with no caveat. Confirmed; caveat added.
- The suggested snippet was not applied; it would also have made the table unservable for a case D-03 chose to keep serving.

**Applied changes:**
- Renamed `anUnreadableParentFailsClosedWithOneMetadataOnlyWarning` to
  `anUnreadableParentKeepsTheEarlierBehaviourWithOneMetadataOnlyWarning`, rewrote its `@DisplayName`, and added a comment
  above the unchanged assertion saying the short Tuple is intended (D-03). No assertion was changed, weakened or removed.
- `DescribeFixturesUnitTest` class Javadoc: "fail-closed path" replaced by the undecidable path that keeps the earlier
  behaviour.
- `CHANGELOG.md` (1.7.0_gg4 entry): added the caveat that an unreadable parent type still drops a `size`/`size0` element,
  logs one WARN per parent, and still serves the table.
- `docs/FORK-DELTA.md` (Known costs item 10): states that the short tuple is still written for that case, by D-03.

**Verification commands:**
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*DescribeTableBuilderUnitTest" --tests "*DescribeFixturesUnitTest"`
  -> BUILD SUCCESSFUL, 215 tests, 215 succeeded. The renamed test is present in the XML result with the new display name.
- `./gradlew spotlessCheck` -> fails only on two pre-existing files that this fix did not touch
  (`src/integrationTest/.../ClickHouseCloudTest.java`, `ClickHouseSinkConnectorIntegrationTest.java`); none of the
  changed files is listed.

### WR-02: Byte-for-byte drift tests against a floating `latest` image can turn the whole suite red for reasons unrelated to the connector

**Status:** fixed
**Commit:** `eb4e6821`
**Files changed:**
- `build.gradle.kts`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java`
- `docs/FORK-DELTA.md`

**Applied fix (tag plus Gradle exclusion, the least invasive option):**
- `DescribeFixtureDriftLiveTest` gets `@Tag("describe-drift")`. It still inherits `@Tag("clickhouse")` from
  `ClickHouseBase`, so it was already outside `testUnit`; that is unchanged.
- `build.gradle.kts`: the `test` task now uses `useJUnitPlatform { excludeTags("describe-drift") }`, and a new
  `describeFixtureDrift` `Test` task (same test classes and classpath, `includeTags("describe-drift")`, never up to date,
  JaCoCo off) runs it on demand. Because the shared `withType<Test>` block wires every test task into `check`, that
  wiring now skips `describeFixtureDrift` too (as it already did for `testUnit`); `check --dry-run` confirms `check`
  runs `test` and not the drift task. The existing `reportTestForks`, JaCoCo and matrix tasks use `test` and are
  unaffected. The CI workflow runs `./gradlew test`, so the drift check no longer gates CI.
- The failure message now names the live server version and the committed `SERVER_VERSION.txt` version. It is built
  lazily through a `Supplier`, so the extra server round trip for `version()` happens only on a failing case.
- Javadoc on the test class documents the tag, why it is excluded, and the run command
  (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew describeFixtureDrift`). The FORK-DELTA fixtures row documents the same.
- No test was deleted or weakened; the Docker-free fixture tests (`DescribeFixturesUnitTest`,
  `DescribeFixtureSnapshotUnitTest`, `DescribeTableBuilderUnitTest`) remain in the main suite.

**Trade-off to note:** the drift check no longer runs in `./gradlew test` or in CI. Someone must run
`describeFixtureDrift` by hand, or a separate non-gating CI job must be added (not done here: it would change the CI
workflow, which is outside this fix's scope and rebase surface).

**Verification commands:**
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew describeFixtureDrift` -> BUILD SUCCESSFUL, 94 tests, 94 succeeded
  (against `latest`, 26.9.19.9, client V2, Docker via colima `vz`).
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*DescribeTableBuilderUnitTest" --tests "*DescribeFixturesUnitTest" --tests "*DescribeFixtureDriftLiveTest" --tests "*DescribeFixtureSnapshotUnitTest"`
  -> BUILD SUCCESSFUL, 262 tests; the 94 drift cases were not run by `test` (215 + 47 only), proving the exclusion.
- `./gradlew check --dry-run` -> lists `:test`, no `describeFixtureDrift`.
- Seen-red check of the new message: prefixed one line of `describe-fixtures/n01.jsonl` with a space, ran
  `describeFixtureDrift` -> 93 passed, 1 failed with `fixture n01: the live DESCRIBE rows differs from the committed
  file (live server 26.9.19.9, committed 26.9.19.9); re-capture with ...`. The fixture was reverted with
  `git checkout -- src/test/resources/describe-fixtures/n01.jsonl`; `git status` shows no fixture change.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew describeFixtureDrift testUnit` -> BUILD SUCCESSFUL; drift 94/94;
  testUnit 1126 tests, 1124 succeeded, 0 failed, 2 skipped.
- `./gradlew spotlessCheck` -> only the same two pre-existing, untouched integrationTest files are flagged. I did not run
  `spotlessApply`, because it would have reformatted those two unrelated files.

## Out of Scope (fix_scope = critical_warning)

Left untouched, as instructed: IN-01 (structural/declared `size<digits>` registration order), IN-02 (dead
`shouldSkipDescribeRow` overloads, `ParentShape.arrayDepth()`), IN-03 (stale `DeclaredNullability` Javadoc), IN-04
(snapshot test described as pre-extraction), IN-05 (Tuple element named `null`), IN-06 (builder/client dependency
cycle), IN-07 (non-atomic `DescribeFixtureCapture`), IN-08 (client leak in `TupleSizeNamedElementRoundTripLiveTest`).

Note for IN-03 and IN-02: WR-01 kept the undecidable path, so the `DeclaredNullability` 999.4 wording and the two-argument
`shouldSkipDescribeRow` rule (which documents "as earlier releases did") remain accurate for that path.

## Environment notes

- The working tree still has the unrelated uncommitted modifications to `.planning/state.json` and `gradlew.bat`; they
  were not staged, reverted or touched.
- No OpenJ9 Mockito crash or core dump occurred in any run.
- `26-REVIEW-FIX.md` is not committed; the orchestrator handles that.

---

_Fixed: 2026-10-22_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
