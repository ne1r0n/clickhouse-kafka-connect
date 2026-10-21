---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "07"
subsystem: upstream-sync
tags: [deps-02, sync-11, merge-2, tail-gate, d-15, suite-gate]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: merge 1 (50f56289), the Shadow repair, BLD-16, the CI-20 archive guard and DEBT-14 (plans 25-03 to 25-06)
provides:
  - "E-19: the step-4 full suite on the merge-1 tree, 1303 tests, 0 failures, 0 errors, the 18 named skips, one server version"
  - "Merge commit M2 38265d26ce00e078b9e8c7d1198ade333a34f3ac: upstream's tagless tail a6f1df83 behind the unedited tail gate"
  - "pre-v1.7.0-tail (local rollback tag), .planning/MERGE-CHECKLIST-v1.7.0-tail.md and .planning/scripts/conflicted-paths-v1.7.0-tail.txt in the record commit after M2"
  - "The D-15 standing gate command, in the checklist and in STATE.md, for Phases 26-29"
affects: [25-08, 26, 27, 28, 29]

actuals:
  tokens: 6000
  tasks: 3
  commits: 18
plan_head_before: cff854864fac5c2f19f2a692dcd78fa05b71b1cd
plan_head_after: 054131be5ed1bd619d0cdc7ba505b6920da6a34f
commits: 18

tech-stack:
  added: []
  patterns:
    - "Rehearse-then-reproduce: the merge is resolved once in a throwaway worktree and the real merge checks out the rehearsal's blobs, so the real tree is byte-equal to the rehearsed one"
    - "Record commit after the merge (X-3): a new file inside the merge commit fails the provenance invariant, so the checklist and conflicted-paths file land in the next commit"

key-files:
  created:
    - .planning/MERGE-CHECKLIST-v1.7.0-tail.md
    - .planning/scripts/conflicted-paths-v1.7.0-tail.txt
  modified:
    - .gitignore
    - gradle/libs.versions.toml
    - build.gradle.kts
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "SYNC-11: the catalog keeps the fork's value for every shared key, adds guava = 33.7.2-jre and drops httpclientTest; .gitignore is the union of both sides with upstream's typo kept (resolution table in the checklist, Section 1a)"

requirements-completed: [DEPS-02, SYNC-11]

coverage:
  - id: D1
    description: "Full ./gradlew test --rerun on latest x V2 after every step-4 change: 92 XML files, 1303 tests, 0 failures, 0 errors, exactly the 18 named skips, server 26.9.19.9 only, fresh XML"
    requirement: DEPS-02
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-19; plan 25-07 Task 1 verify STEP4-SUITE-GREEN"
        status: pass
    human_judgment: false
  - id: D2
    description: "compileIntegrationTestJava and createConfluentArchive succeed with the guard line 'verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class'; spotless numstat of HEAD equals v1.7.0_gg3 (46 lines each)"
    requirement: DEPS-02
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-19"
        status: pass
    human_judgment: false
  - id: D3
    description: "ConnectApiReferencesUnitTest (3 tests) and the pin guard (39 tests) ran inside the suite with 0 failures"
    requirement: DEPS-02
    verification:
      - kind: unit
        ref: "TEST-com.clickhouse.kafka.connect.ci.ConnectApiReferencesUnitTest.xml, ...TestsWorkflowPinGuardUnitTest.xml (E-19)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The rehearsal in /tmp/gsd25/wt-m2: conflicts exactly .gitignore and gradle/libs.versions.toml, resolution compiles main and integration-test sources, the tail gate ends provenance_paths=3 result=PASS on empty stderr"
    requirement: SYNC-11
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-20; plan 25-07 Task 2 verify M2-REHEARSED"
        status: pass
    human_judgment: false
  - id: D5
    description: "Real M2: parents pre-v1.7.0-tail (bb88f23e) and a6f1df83, changes exactly the three upstream paths, adds no file, tree equal to the rehearsal; the next commit is exactly the two record files; tail gate PASS provenance_paths=3, self-test 'selftest: no failures'; gate script and self-test unchanged against pre-v1.7.0-deps"
    requirement: SYNC-11
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-21; plan 25-07 Task 3 verify M2-MERGED-GATED"
        status: pass
    human_judgment: false
  - id: D6
    description: "The standing tail-gate command with M2's full SHA is in MERGE-CHECKLIST-v1.7.0-tail.md and in STATE.md (D-15)"
    requirement: SYNC-11
    verification:
      - kind: other
        ref: "grep -F in .planning/MERGE-CHECKLIST-v1.7.0-tail.md and .planning/STATE.md (E-21)"
        status: pass
    human_judgment: false

duration: 22min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 07: Step-4 suite gate and merge 2 (upstream tail) Summary

**One green full suite closes step 4 (1303 tests, the same 18 named skips), then upstream's untagged tail a6f1df83 is taken as one mechanical merge M2 (`38265d26`) with the fork's values kept in the catalog, guava 33.7.2-jre added and `httpclientTest` gone, passing the unedited tail gate and the self-test.**

## Performance

- **Duration:** about 22 min wall clock (suite 2m 39s, spotless recipe about 1 min, self-test about 13 min)
- **Completed:** 2026-10-21
- **Tasks:** 3 of 3
- **Files modified:** 6 (2 created), plus the merge's three upstream paths

## Accomplishments

- E-19: `./gradlew test --rerun` on `clickhouse-server:latest` (image `bdb5c911a23d`, server 26.9.19.9) x client V2: 92 XML files, 1303 tests, 0 failures, 0 errors, 18 skipped (the 7 named methods plus the 11 `exactlyOnceStateMismatchTest` cases), every XML newer than the last non-`.planning` commit. `compileIntegrationTestJava` and `createConfluentArchive` pass with the guard line; the spotless numstat of HEAD equals `v1.7.0_gg3` (46 lines each).
- E-20: the rehearsal in a throwaway worktree: `git merge-tree` lists exactly `.gitignore` and `gradle/libs.versions.toml`; the resolved tree compiles main and integration-test sources; its tail gate ends `provenance_paths=3 ... result=PASS`.
- E-21: the real merge. Local tag `pre-v1.7.0-tail` = `bb88f23e3adbb49417e73c27e561e50d6bf17a5f`; M2 = `38265d26ce00e078b9e8c7d1198ade333a34f3ac` with parents that tag and `a6f1df839c079180bc7c343b5823f205e204dafc`; the record commit `76da1b3c` after it; tail gate `invariants: provenance_paths=3 anchor_commits=2 anchor_rows=71 marker_commits=14 anchor_exempt=0 result=PASS`; self-test `selftest: no failures`.

## Conflict resolution

| Path | Resolution |
|------|------------|
| `.gitignore` | union: the fork's `/bin/`, `.vscode`, `.gsd` then upstream's `# ecliose` (typo kept), `.classpath`, `.project`, `.settings/`; each line once |
| `gradle/libs.versions.toml` | `[versions]`: fork values for `kafka` (4.4.0 override comment kept), `gson`, `jackson`, `lombok`, `lz4Java`; `httpclient` 5.6.5 (identical); `guava = "33.7.2-jre"` added; `httpclientTest` dropped; `[libraries]` hunk auto-merged (`httpclient5-test` row replaced by `guava`) |
| `build.gradle.kts` | auto-merged, no marker: `implementation(libs.guava)`, `clickhouseDependencies(libs.guava)`, `libs.httpclient5.test` became `libs.httpclient5` |

## Task Commits

1. **Task 1: step-4 suite gate (E-19)** - `89900043` (docs)
2. **Task 2: merge-2 rehearsal (E-20)** - `bb88f23e` (docs)
3. **Task 3: merge M2** - `38265d26ce00e078b9e8c7d1198ade333a34f3ac` (merge)
4. **Task 3: record commit R2** - `76da1b3c` (docs)
5. **Task 3: E-21** - `07ca9382` (docs)
6. **Evidence timestamps corrected to the commit clock** - `054131be` (docs)

**Plan metadata:** the `docs(25-07)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `.gitignore`, `gradle/libs.versions.toml`, `build.gradle.kts` - M2's resolved tree
- `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` - resolution table, conflicted paths, every upstream-changed path, D-15 gate commands, Recorded Facts 42-43
- `.planning/scripts/conflicted-paths-v1.7.0-tail.txt` - the two conflicted paths, no comments
- `25-COMMAND-EVIDENCE.md` - E-19 to E-21, index rows flipped to pass

## Decisions Made

The D-15 standing-gate decision was added to STATE.md through `state.add-decision` in Task 3 and is not repeated here. See `key-decisions` above for the catalog resolution.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written.

### Notes

- `commits:` is the measured `git rev-list --count` over `plan_head_before..plan_head_after` and includes the 12 upstream commits that M2 brought in; first-parent it is 6. The plan's own commits are the six listed under Task Commits.
- The first versions of the E-19..E-21 timestamps were written ahead of the clock; `054131be` replaced them with the commit times (UTC).
- Commits 1 to 5 of this plan carry the trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` that the dispatch specified; the harness attribution for this session names `Claude Sonnet 5.5`, which the later commits use. Rewriting would have changed M2's SHA after the gate had passed on it, so the earlier trailers were left.
- The plan's clean-tree checks excluded only `gradlew.bat`; the checks here also excluded `.planning/state.json` (and the untracked `.planning/milestone.lock`), as plans 25-03 to 25-06 did. None of the three was staged. The rehearsal worktree also showed ` M gradlew.bat`, left unstaged.

## Issues Encountered

None blocking. The OpenJ9 Mockito crash did not occur. No gradle test JVM exited 255.

## Known Stubs

None.

## Threat Flags

None. M2 adds no network endpoint, auth path or file-access pattern; the only new dependency is guava 33.7.2-jre through upstream's catalog (T-25-SC accepted), and `UPSTREAM_REF` was the full SHA confirmed by `git ls-remote upstream main` immediately before the merge (T-25-24).

## Coverage

| Requirement | Evidence |
|-------------|----------|
| DEPS-02 | E-19 (full suite, 18 named skips, archive guard, spotless baseline-equal) |
| SYNC-11 | E-20 (rehearsal), E-21 (M2, record commit, tail gate, self-test, D-15 in STATE.md) |

## Next Plan Readiness

Plan 25-08 can proceed: `dependencyInsight` for guava, DEBT-15, the FORK-DELTA advisory rows and the two `--rerun` runs are its work. The Phase 26-29 gates run the standing tail-gate command recorded in the checklist and STATE.md. Nothing is pushed (`git ls-remote --tags origin pre-v1.7.0-tail` prints nothing); `custom_build` is untouched.

## Self-Check: PASSED

- FOUND: .planning/MERGE-CHECKLIST-v1.7.0-tail.md, .planning/scripts/conflicted-paths-v1.7.0-tail.txt
- FOUND: commits 89900043, bb88f23e, 38265d26, 76da1b3c, 07ca9382, 054131be (ancestors of HEAD)
- FOUND: tag pre-v1.7.0-tail
