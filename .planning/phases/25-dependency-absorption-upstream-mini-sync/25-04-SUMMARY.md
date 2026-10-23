---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "04"
subsystem: build-confluent-archive
tags: [bld-15, shadow-9, confluent-archive, fork-anchors, archive-compare]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: merge 1 (M1 50f56289) with Shadow 9.6.1 and Gradle 9.8.1 (plan 25-03); the string- and comment-aware pin guard (plan 25-02)
provides:
  - "confluentJar repaired for Shadow 9.6.1: configurations.add(clickhouseDependencies) beside from(sourceSets.main.get().output), addMultiReleaseAttribute false, and a top-level shadow block that keeps assemble from building a *-all.jar"
  - "Two BLD-15 rows in fork-anchors.txt (positive count 1, negative count 0) committed with the repair"
  - "Committed compare-confluent-archives.py and a 17-row version-bump allow-list; PASS against the v1.7.0_gg3 archive rebuilt from the tag"
  - "FORK-DELTA row for the repair; evidence E-09..E-11"
affects: [25-05, 25-06, 25-07, 25-08, 29]

actuals:
  tokens: 7700
  tasks: 2
  commits: 3
plan_head_before: 6d59b920516a80206809c93fef77fa0aaca8555e
plan_head_after: 8b0a4caed1b336dd3a7e95bac617d96c927c5a1c
commits: 3

tech-stack:
  added: []
  patterns:
    - "Shadow 9 unpacks only configurations; jars given to from(...) are copied as-is, so dependencies enter a ShadowJar through configurations.add(...)"
    - "Archive compare: in-memory zipfile, per-entry SHA-256, glob allow-list whose rows must all be used, exit 2 on unusable input"

key-files:
  created:
    - .planning/scripts/compare-confluent-archives.py
    - .planning/scripts/confluent-archive-allowlist-25.txt
  modified:
    - build.gradle.kts
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "D-01 primary form shipped (Shadow configurations.add); the zipTree fallback was not needed because every difference against gg3 is a version bump"
  - "D-02: addMultiReleaseAttribute false, so META-INF/versions classes stay inert on the worker JVM as in gg3"
  - "D-03: shadow { addShadowJarToAssembleLifecycle false } so assemble builds no *-all.jar of the runtime classpath"
  - "Outer entries are compared too (prefix outer/, manifest.json ignoring release_date and version lines); none differ"

requirements-completed: [BLD-15]

coverage:
  - id: D1
    description: "On the merge-1 tree the unrepaired Shadow 9.6.1 build is BUILD SUCCESSFUL and its inner jar nests 25 *.jar entries (135 file entries)"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "plan 25-04 Task 1 step 1 (E-09); /tmp/gsd25/exec/archive-m1-unrepaired.zip"
        status: pass
    human_judgment: false
  - id: D2
    description: "Repaired confluentJar yields one flat inner jar: 7,395 file entries, 0 nested jars, 0 org/apache/kafka entries, ClickHouseSinkConnector.class present, 3-line manifest without Multi-Release; assemble --dry-run schedules no shadowJar"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "plan 25-04 Task 1 verify SHADOW-REPAIRED (E-10)"
        status: pass
    human_judgment: false
  - id: D3
    description: "configurations.add(clickhouseDependencies) is on exactly 1 line and the two-argument from call on 0 lines of build.gradle.kts; both rows pinned in fork-anchors.txt in the repair commit bddb838d; TestsWorkflowPinGuardUnitTest 29 tests green"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "grep -cF counts; testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest' (E-10)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Compare of the gg3 archive (inner sha256 c2514401...b36e, 7,376 files) against the repaired archive ends RESULT: PASS with nested jars 0, kafka classes 0, MANIFEST.MF identical, 441 differences all matched by version-bump rows, no stale row; two runs byte-identical; four negative controls exit 1, 1, 1, 2"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "plan 25-04 Task 2 verify ARCHIVE-COMPARED (E-11); /tmp/gsd25/exec/cmp-m1.txt"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA carries the repair row with the D-02 and D-03 reasons and BLD-15 in Pinned by"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "grep -q BLD-15 docs/FORK-DELTA.md"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 04: Shadow 9 repair of confluentJar and archive compare Summary

**`confluentJar` now unpacks its dependencies through Shadow 9's `configurations.add(...)` instead of nesting 25 jars (E-09: green build, unusable archive), the repair is pinned by a positive and a 0-count anchor, and the new committed compare script proves the archive intact against the gg3 archive rebuilt from the tag: 441 differences, every one a version bump, `RESULT: PASS`.**

## Performance

- **Duration:** about 15 min wall time (four Gradle archive builds of 3 to 7 s each, one pin-guard run, the rest authoring)
- **Completed:** 2026-10-21
- **Tasks:** 2 of 2
- **Files modified:** 6 (2 created)

## Accomplishments

- E-09 (before): unrepaired archive, `BUILD SUCCESSFUL`, one outer `lib/*.jar`, inner jar with 135 file entries of which 25 are nested `*.jar`.
- E-10 (after): inner jar sha256 `72849d23...e65ca`, 7,395 file entries, 0 nested jars, 0 `org/apache/kafka/` entries, connector class present, manifest `Implementation-Title` and `Implementation-Version: v1.7.0_gg3` only (no `Multi-Release`); `assemble --dry-run` lists `:jar` and `:assemble`, no `:shadowJar`; anchor counts 1 and 0.
- E-11: gg3 rebuilt from the tag in `/tmp/gsd25/wt-gg3` reproduces E-04 (sha256 `c2514401f76729d17f61bab3d405288801c2b162dba10a58eead3d20e264b36e`, 7,376 files); compare totals `files-a 7376 files-b 7395 only-a 51 only-b 70 changed 320 same 7005`, `nested jars in b: 0`, `kafka classes in b: 0`, `MANIFEST.MF: identical`, `RESULT: PASS`; outer entries equal; no difference under `com/clickhouse/`.
- Bumps explaining the differences (from the VERSION table): jackson-core and jackson-databind 2.22.3 to 2.22.4, gson 2.13.1 to 2.14.0, httpclient5 5.6.2 to 5.6.5, httpcore5 and httpcore5-h2 5.4.3 to 5.4.4, error_prone_annotations 2.38.0 to 2.48.0. Guava and its small dependencies read `x -> x` (research X-1).
- Negative controls: unrepaired B exit 1 (`nested jars in b: 25`); extra row exit 1 (`STALE-ROW com/example/never/*`); removed row exit 1 (85 `UNALLOWED`); text-only zip exit 2.

## Task Commits

1. **Task 1: repair and anchors (E-09, E-10)** - `bddb838d` (build), then evidence `7a9f9d26` (docs)
2. **Task 2: compare script, allow-list, FORK-DELTA row (E-11)** - `8b0a4cae` (build)

**Plan metadata:** the `docs(25-04)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `build.gradle.kts` - repaired `confluentJar` block and the `shadow { ... }` block (8 added lines, 1 removed)
- `.planning/scripts/fork-anchors.txt` - a Phase 25 comment line and two BLD-15 rows
- `.planning/scripts/compare-confluent-archives.py` - new, `load_inner`, `compare`, `main`
- `.planning/scripts/confluent-archive-allowlist-25.txt` - new, 17 `glob<TAB>reason` rows
- `docs/FORK-DELTA.md` - one row in "Build, dependencies and CI"
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` - E-09..E-11, index rows flipped to pass

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

None - the plan executed as written.

### Notes

- The research's "158 inner entries" for the unrepaired jar counts directory entries; the measured 135 are file entries (the unit the 7,376 and 7,395 figures use). The 25 nested jars match research.
- `META-INF/versions/9/module-info.class` and `META-INF/DEPENDENCIES` are paths shared by several bundled jars; they were traced to gson 2.14.0 and httpclient5 5.6.5 by hashing the candidate jars in the Gradle cache, so their rows name a bump.
- The compare script exits 1 on a FAIL; the plan's "exit 2" cases (no or several inner jars, empty inner jar, unreadable archive, malformed allow-list) are all `InputError`.
- Pre-existing uncommitted items (`.planning/state.json`, `gradlew.bat`, `.planning/milestone.lock`) were never staged; clean-tree checks excluded them. The plan's verify strings exclude only `gradlew.bat`, so the same command run literally would also list `.planning/state.json`.
- Commit trailers use `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` as the dispatch specifies. `actuals.commits: 3` is measured `git rev-list --count 6d59b920..HEAD` before the completion commit.

## Issues Encountered

None. The OpenJ9 Mockito crash did not occur and no `core.*` dump was present. The baseline worktree was removed and pruned; `git worktree list` shows only the main tree.

## Known Stubs

None.

## Threat Flags

None. The edits are a build-script repair, two tooling files and documentation; archives were read in memory with `python3 -I` and nothing was extracted or executed. T-25-11 and T-25-12 are mitigated as the plan states (measured flat archive, two anchors, entry-by-entry compare; no catch-all or `com/clickhouse/` row, stale rows fail).

## Coverage

| Requirement | Evidence |
|-------------|----------|
| BLD-15 | E-09 (unrepaired control), E-10 (repair, flat archive, anchors, dry-run), E-11 (compare against gg3 PASS, negative controls, determinism) |

## Next Plan Readiness

Plan 25-05 can add the worker-jar baseline on the repaired archive; plan 25-06 adds the build-time guard and reuses the anchors; plan 25-08 repeats this compare at the phase tip after merge 2 and extends the allow-list for the second merge's bumps. No file under `src/main` changed (`git diff --quiet 6d59b920 HEAD -- src/main` exits 0). Nothing is pushed.

## Self-Check: PASSED

- FOUND: build.gradle.kts (configurations.add(clickhouseDependencies) on one line)
- FOUND: .planning/scripts/compare-confluent-archives.py
- FOUND: .planning/scripts/confluent-archive-allowlist-25.txt
- FOUND: commits bddb838d, 7a9f9d26, 8b0a4cae
