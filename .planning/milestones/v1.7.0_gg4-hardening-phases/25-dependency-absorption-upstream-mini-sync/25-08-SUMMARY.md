---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "08"
subsystem: dependency-baseline
tags: [sync-12, debt-15, bld-15, ops-04, baseline, guava, httpclient5, d-16, d-17]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: merge 1 and the repaired Shadow archive (25-03, 25-04), the pin guards (25-05, 25-06), merge 2 M2 38265d26 and the tail gate (25-07)
provides:
  - "E-22: the merge-2 archive compare against v1.7.0_gg3 ends RESULT: PASS (allow-list extended with version-bump rows only), dependencyInsight proves guava 33.7.2-jre and httpclient5 5.6.5, advisory facts per source"
  - "E-23: slf4j-reload4j resolves from the catalog on the shared slf4j key; its anchor row moved in the same commit"
  - "E-24: two --rerun full suite runs green (1303 tests, 18 named skips), spotless baseline-equal, local tag baseline-v1.7.0_gg4 = aa45070953137f612842e730c465937ac5c6ada5"
  - "E-25: OPS-04 closing observation recorded as pending: no Dependabot PR opened after the label"
affects: [26, 27, 28, 29]

actuals:
  tokens: 11660
  tasks: 3
  commits: 4
plan_head_before: d143a9839587afc08fe205e75bbf686386f80ad3
plan_head_after: 31ca99adbba0887f5afe480d7cb890fe57275ab0
commits: 4

tech-stack:
  added: []
  patterns:
    - "Fork-only catalog row on a shared version key: slf4j-reload4j rides the slf4j key so one Dependabot PR moves all three slf4j artifacts"
    - "Advisory facts are labelled by the source queried (NVD, OSV, release notes) and carry no exposure claim"

key-files:
  created: []
  modified:
    - .planning/scripts/confluent-archive-allowlist-25.txt
    - docs/FORK-DELTA.md
    - gradle/libs.versions.toml
    - build.gradle.kts
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "DEBT-15 (D-16): the BLD-08 anchor moved to the catalog line and a build-side row for implementation(libs.slf4j.reload4j) was added, so an upstream sync cannot silently drop the dependency"

requirements-completed: [SYNC-12, DEBT-15, BLD-15, OPS-04]

coverage:
  - id: C1
    description: "Archive built on the merge-2 tip: 0 nested jars, 0 org/apache/kafka entries, MANIFEST.MF identical, RESULT: PASS against v1.7.0_gg3 with only version-bump allow-list rows (guava 33.4.6-jre -> 33.7.2-jre, j2objc 3.0.0 -> 3.1, jspecify 1.0.0 -> 1.0.1, error_prone 2.38.0 -> 2.50.0); the final archive is byte-identical to the merge-2 one"
    requirement: BLD-15
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-22 and E-24; plan 25-08 Task 1 verify M2-TREE-MEASURED"
        status: pass
    human_judgment: false
  - id: C2
    description: "dependencyInsight on runtimeClasspath and clickhouseDependencies selects com.google.guava:guava:33.7.2-jre by conflict resolution over 33.4.6-jre, and httpclient5 5.4.4 -> 5.6.5"
    requirement: SYNC-12
    verification:
      - kind: other
        ref: "/tmp/gsd25/exec/insight-guava-runtime.txt, insight-guava-chdeps.txt, insight-httpclient5.txt (E-22)"
        status: pass
    human_judgment: false
  - id: C3
    description: "FORK-DELTA holds the guava override and the httpclient5 pin with CVE-2026-102554 / GHSA-xxph-c9ww-hj94 and CVE-2026-64607 / GHSA-hjcp-jmpx-g3qm, each fact labelled NVD, OSV or the Guava release notes; no exposure claim; 'every scanner' appears 0 times"
    requirement: SYNC-12
    verification:
      - kind: other
        ref: "docs/FORK-DELTA.md; E-22 advisory facts; Task 1 verify greps"
        status: pass
    human_judgment: true
  - id: C4
    description: "slf4j-reload4j comes from the catalog (one library row on version.ref slf4j, no new [versions] key), build.gradle.kts holds no literal coordinate, every fork-anchors.txt row matches its file count (72 rows), dependencyInsight resolves 2.0.20, testUnit SUCCESS"
    requirement: DEBT-15
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-23; plan 25-08 Task 2 verify DEBT15-MOVED"
        status: pass
    human_judgment: false
  - id: C5
    description: "Two full test --rerun runs on the tip: 92 XML files, 1303 tests, 0 failures, 0 errors, exactly the 18 named skips, server 26.9.19.9 only, run 2 started after run 1 ended; testUnit SUCCESS (860 tests); spotless numstat equal to v1.7.0_gg3 (46 lines each)"
    requirement: SYNC-12
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-24; plan 25-08 Task 3 verify BASELINE-SET"
        status: pass
    human_judgment: false
  - id: C6
    description: "Local lightweight tag baseline-v1.7.0_gg4 names aa45070953137f612842e730c465937ac5c6ada5 (the tested tip), is not on origin, and STATE.md records its full SHA; origin/custom_build still 69cd680b"
    requirement: SYNC-12
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-24; git ls-remote --tags origin baseline-v1.7.0_gg4 prints nothing"
        status: pass
    human_judgment: false
  - id: C7
    description: "OPS-04 closing observation: label answers 200, 15 pull requests listed, none created after 2026-10-21T15:11:05Z, verdict pending (next monthly Dependabot run; Phase 29 re-checks)"
    requirement: OPS-04
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-25; plan 25-08 Task 3 verify OPS04-CLOSED-OR-PENDING"
        status: pass
    human_judgment: true

duration: 16min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 08: Post-absorption baseline Summary

**The merged tree is measured, DEBT-15 moves `slf4j-reload4j` into the catalog with its anchor, guava 33.7.2-jre and httpclient5 5.6.5 are recorded with source-labelled advisories, and two green `--rerun` runs tag the local baseline `baseline-v1.7.0_gg4` (`aa450709`) for Phases 26-28.**

## Performance

- **Duration:** about 16 min wall clock (two suite runs of 2m 38s and 2m 39s, spotless recipe about 2 min)
- **Completed:** 2026-10-21
- **Tasks:** 3 of 3
- **Files modified:** 6

## Accomplishments

- E-22: `createConfluentArchive` on the merge-2 tip passes the guard line (`7798 inner entries, no nested jar, no Kafka API class`). The first compare against `v1.7.0_gg3` failed on guava's classes and metadata, j2objc, a root `module-info.class` and an OSGi manifest; after the version-bump rows it ends `RESULT: PASS`, `nested jars in b: 0`, `kafka classes in b: 0`, `MANIFEST.MF: identical`, 0 STALE-ROW. `dependencyInsight` shows guava `33.4.6-jre -> 33.7.2-jre` (conflict resolution, on `runtimeClasspath` and on `clickhouseDependencies`) and httpclient5 `5.4.4 -> 5.6.5`.
- E-22 advisories, re-queried today: NVD, OSV and the Guava `v33.7.2` release notes for `CVE-2026-102554` = `GHSA-xxph-c9ww-hj94`; NVD and OSV for `CVE-2026-64607` = `GHSA-hjcp-jmpx-g3qm`. FORK-DELTA rows carry the labels, make no exposure claim and name no other scanner.
- E-23: `slf4j-reload4j = { module = "org.slf4j:slf4j-reload4j", version.ref = "slf4j" }` after the `slf4j-simple` row, `implementation(libs.slf4j.reload4j)`, anchor row moved plus a build-side row, in one commit (`9c9b480b`). `dependencyInsight` resolves `2.0.20`; `testUnit` SUCCESS; the tail gate still passes (`anchor_rows=72`).
- E-24: freeze check clean, `testUnit` 860 tests green, final compare `RESULT: PASS` (archive byte-identical to the merge-2 one), run 1 and run 2 each 1303 tests, 0 failures, 0 errors, 18 skips, run 2's first XML (17:24:11Z) later than run 1's last (17:23:54Z), spotless numstat equal (46 lines), tag set, D-17 recorded in STATE.md.
- E-25: the label answers 200 but Dependabot has opened no PR since the operator created it (newest is #15, 2026-10-21T06:29:01Z), so the verdict is `pending (no Dependabot PR opened since 2026-10-21T15:11:05Z; next monthly Dependabot run; Phase 29 re-checks)`.

## Task Commits

1. **Task 1: merged-tree archive compare, advisories, FORK-DELTA rows (E-22)** - `0a7a00d9` (docs)
2. **Task 2: DEBT-15 slf4j-reload4j into the catalog** - `9c9b480b` (build)
3. **Task 2: E-23** - `aa450709` (docs; this is the commit the baseline tag names)
4. **Task 3: E-24 and E-25** - `31ca99ad` (docs)

**Plan metadata:** the `docs(25-08)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `.planning/scripts/confluent-archive-allowlist-25.txt` - 11 new version-bump rows, the 2 error_prone rows retitled 2.48.0 -> 2.50.0, header updated
- `docs/FORK-DELTA.md` - guava and httpclient5 rows, refreshed slf4j-reload4j, catalog-pins and lz4 rows compared with upstream's tail
- `gradle/libs.versions.toml`, `build.gradle.kts`, `.planning/scripts/fork-anchors.txt` - DEBT-15
- `25-COMMAND-EVIDENCE.md` - E-22 to E-25, index rows flipped (E-25 to pending)

## Decisions Made

The D-17 baseline decision was added to STATE.md through `state.add-decision` in Task 3 and is not repeated here. DEBT-15's anchor decision is in `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

None - the plan's rules were applied as written. Three points where the live result differed from what the plan or research text implied:

**1. [Note] Two archive differences the research table did not list**
- **Found during:** Task 1 compare
- **Issue:** the root `module-info.class` and `META-INF/versions/9/OSGI-INF/MANIFEST.MF` differed
- **Resolution:** the first was traced by sha256 to jackson-annotations 2.22 (gg3) and guava-33.7.2-jre (now; guava moved its descriptor to the jar root); the second to jspecify 1.0.0, replaced by 1.0.1 through guava 33.7.2 (conflict resolution shown by `dependencyInsight`; 1.0.1 has no such entry). Both are version-bump rows naming the bump, which the plan allows. No catch-all and no `com/clickhouse/` row.
- **Commit:** `0a7a00d9`

**2. [Note] error_prone_annotations now 2.50.0**
- The VERSION line reads `2.38.0 -> 2.50.0`; the two merge-1 rows said 2.48.0, so their reasons were updated as the plan directs.

**3. [Note] OSV answers the guava GHSA id with "not found"**
- `GET /v1/vulns/GHSA-xxph-c9ww-hj94` returns 404 naming the alias `CVE-2026-102554`; the record sits under the CVE id with a GIT range only, and the Maven queries return nothing. E-22 and FORK-DELTA say so and do not turn it into a coverage claim.

### Process notes

- The commit trailer `Co-Authored-By: Claude Fable 5.1` was specified by the dispatch for these commits; this session's attribution reminder names `Claude Sonnet 5.5`. The four plan commits above carry the dispatch's trailer, as in plan 25-07; the completion commit follows the session reminder. Commits were not rewritten because `aa450709` is the tagged, tested tip.
- Clean-tree checks excluded `gradlew.bat`, `.planning/state.json` and `.planning/milestone.lock`, none of which was staged (as in plans 25-03 to 25-07); the plan's own verify commands exclude only `gradlew.bat`, so they were run with the extra excludes.
- E-23's timestamp was corrected to the clock time and the commit amended before any later work (not pushed).
- One `git ls-remote origin custom_build` at tag time failed with "Connection to github.com closed by remote host"; the repeat printed `69cd680b` (the freeze held). The tag was created in the same command block, before the repeat; it is local and the repeat confirmed the stop condition did not fire.
- `git ls-remote --tags origin 'pre-v1.7.0-*'` lists a pre-existing remote tag `pre-v1.7.0-sync` (`c61d90ce`); it is not one of this phase's tags.

## Issues Encountered

None blocking. No test JVM exited 255; no core dumps appeared.

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or file access; the only dependency movement is guava and httpclient5 resolving to the versions upstream's catalog names (T-25-26 mitigated by E-22; T-25-27 by source-labelled wording; T-25-30 by the empty `git ls-remote --tags origin baseline-v1.7.0_gg4`).

## Coverage

| Requirement | Evidence |
|-------------|----------|
| SYNC-12 | E-22 (dependencyInsight, advisories, FORK-DELTA), E-24 (two --rerun runs, tag, STATE.md) |
| DEBT-15 | E-23 |
| BLD-15 | E-22 (merge-2 compare), E-24 (final compare, PASS) |
| OPS-04 | E-25 (pending, the REQUIREMENTS text allows "otherwise the check is recorded as pending the next monthly run") |

## Next Plan Readiness

Phase 25 has no further plan. Phases 26-28 build on the local tag `baseline-v1.7.0_gg4` (`git diff baseline-v1.7.0_gg4` shows what each phase changed) and use the standing tail-gate command in STATE.md. Phase 29 names this baseline, copies the source labels of the two advisories, and re-checks OPS-04 after the next monthly Dependabot run. Nothing is pushed: `git ls-remote --tags origin baseline-v1.7.0_gg4` prints nothing, `custom_build` is untouched at `69cd680b`, Dependabot PRs #4 and #10 and the `dependencies` label were not touched.

## Self-Check: PASSED

- FOUND: docs/FORK-DELTA.md rows for CVE-2026-102554 and CVE-2026-64607; gradle/libs.versions.toml `slf4j-reload4j` row; `implementation(libs.slf4j.reload4j)` in build.gradle.kts
- FOUND: commits 0a7a00d9, 9c9b480b, aa450709, 31ca99ad (ancestors of HEAD)
- FOUND: tag baseline-v1.7.0_gg4 -> aa45070953137f612842e730c465937ac5c6ada5
