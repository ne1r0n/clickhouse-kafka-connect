---
phase: 37-ci-hardening-advisory-debt
plan: "02"
subsystem: ci
tags: [python, junit, archive-compare, allow-list, release-tags, github-rest]

requires:
  - phase: 37-ci-hardening-advisory-debt
    provides: "plan 37-01: the evidence file, the helpers, the script-from-JUnit house style, fork-anchors at rows=157"
provides:
  - "compare_confluent_archives.py under .github/scripts (git mv), usage and baseline wording updated, a glob of only stars refused with exit 2"
  - "CompareConfluentArchivesScriptUnitTest: 27 Docker-free executions on synthetic archives (exit 0 x6, exit 1 x8, exit 2 x13)"
  - "baseline_tag.py and BaselineTagScriptUnitTest (19 executions): the greatest vX.Y.Z or vX.Y.Z_ggN tag strictly below a VERSION"
  - "the allow-lists of the two published releases renamed by VERSION (-v1.7.0_gg4, -v1.7.0_gg5), FORK-DELTA naming them and the compare"
  - "E-07: the compare proven in both polarities on real archives built without Docker"
  - "E-08: the real v1.7.0_gg5 release from read-only REST readings; no workflow run created it"
affects: [37-03, 37-05, 38-release]

plan_head_before: 343fd0f0ee45f2924294ee759d4b4bac93b4a388
plan_head_after: 5a7ab43b2c7c34b79c314ae2d8e18cb1a872225e

actuals:
  tokens: 21710
  tasks: 3
  commits: 7

tech-stack:
  added: []
  patterns:
    - "git mv in its own commit with a 100 percent similarity check, then a separate edit commit, so the rename stays a pure rename"
    - "red first on the moved but unedited script: the two star rows failed before the edit and passed after it"

key-files:
  created:
    - .github/scripts/baseline_tag.py
    - src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java
  modified:
    - .github/scripts/compare_confluent_archives.py
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
    - docs/FORK-DELTA.md
    - .planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md

key-decisions:
  - "Assets copied unchanged and proven with cmp; the compare script and baseline script behave as the plan says"
  - "E-08 states the ADV-03 facts as measured and leaves the publish-path caveat open until Phase 38, deviating from the ROADMAP wording of criterion 5 (O-6)"
  - "Evidence keys that the plan spelled with a capital letter (mutA_*, runA_*) are written lowercase, because the unmodified evidence helper only reads [a-z0-9_]+"

patterns-established:
  - "A Docker-free JUnit class that runs a Python script on synthetic zips built in a TempDir and asserts exit codes and report lines"

requirements-completed: [CIH-05, CIH-04, ADV-03]

coverage:
  - id: D1
    description: "The archive compare lives under .github/scripts after a pure rename, refuses a catch-all glob with exit 2, and its self-test pins UNALLOWED (changed, only-B, only-A), STALE-ROW and the exit codes 0, 1 and 2 on synthetic archives"
    requirement: "CIH-05"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java (27 executions green; two star rows red before the edit; STALE-ROW mutation fails 1, UNALLOWED mutation fails 5)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The baseline of a release is chosen by a tested rule: v1.7.0_gg5 gives v1.7.0_gg4 and v1.7.0_gg6 gives v1.7.0_gg5 on the real tags, and rollback or branch-like tags are ignored"
    requirement: "CIH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java (19 executions green, all red before the script existed, counter mutation fails 7)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The allow-lists of the published releases are renamed byte-identically by VERSION, and on real archives the moved script passes gg4 against gg5, fails gg4 against the current tip with the three zstd natives UNALLOWED, and fails an extra row as STALE-ROW"
    requirement: "CIH-04"
    verification:
      - kind: other
        ref: "37-COMMAND-EVIDENCE.md E-07 (runa_*, runb_*, runc_* keys)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The real v1.7.0_gg5 release is on the record from read-only REST readings: no workflow run created it, the publish path of release.yaml has not been exercised, and the caveat stays open until Phase 38"
    requirement: "ADV-03"
    verification:
      - kind: other
        ref: "37-COMMAND-EVIDENCE.md E-08"
        status: pass
    human_judgment: true
    rationale: "The facts contradict the ROADMAP wording of criterion 5 (which asks for the run that created the release and for the caveat to be closed); accepting the deviation is the user's call"
  - id: D5
    description: "Neither script has run on a GitHub runner: nothing is pushed or dispatched in this phase"
    requirement: "CIH-05"
    verification: []
    human_judgment: true
    rationale: "A CI run needs the workflow wiring of plan 37-03 on origin; Phase 37 pushes nothing, so the first CI exercise is a later, user-gated step"

duration: 11min
completed: 2026-10-31
status: complete
---

# Phase 37 Plan 02: Compare script, baseline tag rule and allow-list naming Summary

**The archive compare moved under `.github/scripts/` and is pinned by 27 synthetic-archive executions, the baseline release tag is chosen by a 19-execution table-tested script, the published releases' allow-lists are named by `VERSION`, the compare was shown passing, failing (three zstd natives UNALLOWED) and going stale on real archives built without Docker, and the real `v1.7.0_gg5` release is on the record as created by no workflow run.**

## Performance

- **Duration:** about 11 min
- **Started:** 2026-10-31T09:04:41Z
- **Completed:** 2026-10-31T09:16Z
- **Tasks:** 3 (all auto)
- **Files modified:** 8 in the diff (3 created, 2 renamed lists, the moved script, FORK-DELTA, the evidence file)

## Accomplishments

- E-05: `git mv` as a pure 100 percent rename, then the plan asset over it. The 27-execution class ran against the moved but unedited script first: exactly the two star-row executions failed (`tests=27 failures=2`), and all 27 passed after the edit with the script mode still 100755. Neutralising the STALE-ROW statement failed exactly 1 execution; dropping the UNALLOWED append failed 5 (changed, only-B, only-A, manifest.json, mixed pair). FORK-DELTA names the new path 3 times and the old path no more.
- E-06: `BaselineTagScriptUnitTest` (10 printed baselines, 3 no-baseline, 4 invalid versions, 2 usage errors) was red 19 of 19 before `baseline_tag.py` existed and green after. On the 78 release-looking tags reachable from HEAD, `v1.7.0_gg5` gives `v1.7.0_gg4`, `v1.7.0_gg6` gives `v1.7.0_gg5`, the current `VERSION` gives `v1.7.0_gg4`; 20 names (for example `v1.7.0_gg4-hardening`, `v1.7.0_gg5-ci`, the `-alfa` and `-beta` tags) are ignored. Treating every counter as zero failed 7 executions.
- E-07: the lists `-29` and `-32` were renamed with `git mv` (2 of 2 at 100 percent), `-25` stays, FORK-DELTA gained the compare row. Three archives were built locally and Docker-free (gg4 and gg5 from their tags with Gradle 9.8.1, the tip with 9.8.2). Run A (gg4 against gg5 under the gg5 list) PASS with `changed 4 same 7395`; run B (gg4 against the tip) FAIL with 238 UNALLOWED (116 CHANGED, 118 ONLY-B, 4 ONLY-A), three `libzstd-jni-1.5.7-23` natives among them, no STALE-ROW, equal to the research numbers exactly; run C (extra row) FAIL with `STALE-ROW zz/never`. The `zstd-jni` jar has no VERSION table line (no `pom.properties`) and no row was added for it.
- E-08: the real `v1.7.0_gg5` release, tag, three `release.yaml` runs and the asset were read with plain GETs. The asset uploader is the user's account; the only publish-mode run was cancelled during `Build and verify Confluent archive` with `Publish release` skipped; the later success was a dry run. The downloaded asset matches the API size and digest; a rebuild from the tag compares PASS against it with `changed 0 same 7399` under a comment-only list.
- Plan gate: cache-busted `testUnit` 1585 tests (1539 plus 27 plus 19), 0 failures, 0 errors, 2 skipped; the pin guards keep 39 and 21; anchors mirror `rows=157 bad=0`; no `src/main`, `VERSION`, `CHANGELOG.md`, gradle, workflow or existing assertion changed.

## Task Commits

1. **Task 1: move, edit, pin the compare**
   - `4eee1466` ci: the pure rename (move_commit)
   - `10d90f61` test: the script edit, `CompareConfluentArchivesScriptUnitTest`, FORK-DELTA path updates (edit_commit)
   - `3ad8d5f6` docs: E-05
2. **Task 2: baseline tag rule**
   - `b6cf6d30` ci: `baseline_tag.py` and `BaselineTagScriptUnitTest` (baseline_commit)
   - `09ab3a9d` docs: E-06
3. **Task 3: allow-list names, real-archive compare, ADV-03 record**
   - `163c0f33` ci: the two renames and the FORK-DELTA row (rename_commit)
   - `5a7ab43b` docs: E-07 and E-08

## Commits

- `move_commit`: `4eee1466f6e61db89cb64463c4d2c0f2b4407b17`
- `edit_commit`: `10d90f61fcf10212a87bcc1cdf30d71949fd1932`
- `baseline_commit`: `b6cf6d30885dee5b43a61e7176c2db3d4733b8d4`
- `rename_commit`: `163c0f33d09cb61df00a27c23c2c660587c61501`

## Files Created/Modified

- `.github/scripts/compare_confluent_archives.py` - the moved compare (plan asset, unchanged), mode 755
- `.github/scripts/baseline_tag.py` - the baseline rule (plan asset, unchanged), mode 755
- `src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java` - 27 executions
- `src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java` - 19 executions
- `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt` and `-v1.7.0_gg5.txt` - renamed lists, content byte-identical
- `docs/FORK-DELTA.md` - three script path references, two list path references, one new compare row
- `.planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md` - entries E-05 to E-08, index rows flipped

## Decisions Made

- Both scripts were copied from the assets without edits (proven with `cmp`); no asset behaved differently from its plan.
- Both new test classes have no superclass and no tag; their Javadoc says "has no superclass" instead of the words "extends nothing", following plan 37-01's carry-over.
- STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/state.json` and `.planning/config.json` were not touched by this run, per the orchestrator's staging rules; requirements `CIH-04`, `CIH-05`, `ADV-03` are listed in `requirements-completed` for the orchestrator to mark after verification (CIH-04 is completed by plan 37-03).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Evidence keys written with a capital letter cannot be read by the helper**
- **Found during:** Task 1 (E-05).
- **Issue:** the plan's keys `mutA_*`, `mutB_*` and (Task 3) `runA_*`, `runB_*`, `runC_*` contain a capital letter, but the unmodified evidence helper's fact grammar is `[a-z0-9_]+`, so neither an entry nor an `expect` expression could carry them.
- **Fix:** lowercase keys `muta_*`, `mutb_*`, `runa_*`, `runb_*`, `runc_*`, with the plan's `expect` expressions adjusted accordingly. Recorded in the free text of E-05 and E-07.
- **Files modified:** `37-COMMAND-EVIDENCE.md` (own entries only).
- **Verification:** `expect` for E-05 (12 checks), E-07 (20), E-08 (29) read 0 failed.
- **Committed in:** `3ad8d5f6`, `5a7ab43b`.

**2. [Rule 3 - Blocking] The Task 1 verify compares an unsorted file list against a sorted one**
- **Found during:** Task 1 verify (first `<automated>`).
- **Issue:** the check compares `LC_ALL=C sort` output of the edit commit's files against `.github/... ${T}/...Test.java docs/FORK-DELTA.md`, which is not in C order (`docs/` sorts before `src/`), so the check fails on a correct commit.
- **Fix:** the expected string in the order `.github/scripts/compare_confluent_archives.py docs/FORK-DELTA.md src/test/.../CompareConfluentArchivesScriptUnitTest.java`. The three paths are the plan's; only the order changed. `edit_commit_files` in E-05 is recorded in the same C order.
- **Verification:** the structural part of the verify prints `T1-STRUCT-OK`; every other clause passed unchanged.
- **Committed in:** not applicable (verify command only).

**3. [Plan wording] The download counter did not rise**
- **Found during:** Task 3 (E-08).
- **Issue:** the plan expects the asset's `download_count` to rise by one after the API download; it read 0 before and after.
- **Fix:** recorded as measured (`download_count_before: 0`, `download_count_after: 0`) with a sentence in E-08. No gate depended on it.
- **Committed in:** `5a7ab43b`.

**4. [Rule 3 - Blocking] The plan's Gradle verify commands carry an unquoted `tests>=N`**
- **Found during:** Task 1 and Task 2 verify (second `<automated>` of each), the same trap plan 37-01 recorded.
- **Fix:** the expression was quoted (`'tests>=27'`, `'tests>=19'`); no stray `=N` file was created (checked with `git status` and `ls`).
- **Verification:** `P37-02-T1-TESTS-OK` and `P37-02-T2-TESTS-OK` print.

---

**Total deviations:** 4 (3 blocking verify or key-naming issues, 1 premise reading). **Impact on plan:** none on scope or results; no src/main, dependency, workflow, assertion or origin change. The ADV-03 outcome itself deviates from the ROADMAP wording of criterion 5 by design (O-6), recorded in E-08 and in the Handoffs.

## Issues Encountered

- No OpenJ9 crash, core dump or javacore appeared in any run.
- The three archive builds took 11, 7 and 7 seconds with a warm Gradle cache.

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or file access: the scripts read zip entries in memory with `python3 -I`, the downloaded asset sat in its own new directory and was never extracted or executed, and every GitHub call was a plain GET.

## User Setup Required

None - no external service configuration required. Neither script has run on GitHub (nothing is pushed in this phase).

## Handoffs

- **Plan 37-03 (release workflows):**
  - Paths: the compare is `.github/scripts/compare_confluent_archives.py` (usage `A.zip B.zip ALLOWLIST`, exit 0 PASS, 1 FAIL, 2 unusable input, a catch-all row refused), the baseline picker is `.github/scripts/baseline_tag.py` (`VERSION` as the one argument, the tags from `git tag --merged HEAD --list 'v*'` on stdin; exit 1 when no tag is below, 2 on a bad `VERSION`).
  - Allow-lists: `.planning/scripts/confluent-archive-allowlist-<VERSION>.txt`; present are `-25`, `-v1.7.0_gg4` and `-v1.7.0_gg5`; the gg6 list does not exist and is Phase 38's (RLS-03), no provisional list was written.
  - Scratch archives for the local replay of the workflow steps: `/tmp/gsd37/w/archives/base_gg4/archive.zip`, `/tmp/gsd37/w/archives/cand_gg5/archive.zip`, `/tmp/gsd37/w/archives/cand_tip/archive.zip` (each in its own directory; sizes 12453558, 12453556 and 13558637 bytes).
  - Run B numbers (gg4 against the current tip under the gg5 list): 238 UNALLOWED (116 CHANGED, 118 ONLY-B, 4 ONLY-A), 0 STALE-ROW, 3 zstd natives, 31 report lines naming `com/clickhouse/kafka/connect`; totals `files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275`; they matched the research exactly, because `src/main`, the build files, the gradle directory and `VERSION` had not moved since the research HEAD.
  - The anchors mirror stays `rows=157 bad=0`; plan 37-03 raises it to 171 with the workflow rows.
- **Plan 37-05 (release record and ledger):** the E-08 finding is that no workflow run created `v1.7.0_gg5`: the only publish-mode run (`40474419526`) was cancelled before `Publish release`, the dry run (`40693904004`) deleted its own draft, and the asset (`685055288`, `12453556` bytes, digest `4c69b6fb...6767`) was uploaded by the user's account `ne1r0n`. The caveat "no run exercised the publish path" stays open until Phase 38; the ledger and the release record must say so and note that this deviates from the ROADMAP wording of criterion 5.
- **User decision pending:** accept the criterion 5 deviation above, or ask for a different closure of the caveat.
- Scratch rules stay as in plan 37-01: logs under `/tmp/gsd37/exec/`, worktrees under `/tmp/gsd37/w/<name>` removed after use (`git worktree list` shows one entry), never stage `.planning/config.json`, `.planning/state.json` or `.planning/STATE.md`, origin and GitHub read-only.

## Next Phase Readiness

Plan 37-03 can start: the tree is clean apart from the orchestrator-owned state files, the evidence index reads pass for E-01..E-08 and pending for E-09..E-18, `testUnit` is green at 1585 tests, and the scripts and lists plan 37-03 wires into the release workflows exist and are tested.

## Self-Check: PASSED

- Files: `.github/scripts/compare_confluent_archives.py`, `.github/scripts/baseline_tag.py`, both test classes, both renamed lists and the evidence file exist; the old compare path and the `-29` and `-32` list paths are gone; both scripts equal their assets (`cmp`).
- Commits `4eee1466`, `10d90f61`, `3ad8d5f6`, `b6cf6d30`, `09ab3a9d`, `163c0f33`, `5a7ab43b` are ancestors of `HEAD`.
- All six plan `<automated>` checks pass (`P37-02-T1-TESTS-OK`, `P37-02-T2-TESTS-OK`; the structural and evidence clauses of the T1, T2 and T3 checks with the two verify-command fixes above).

---
*Phase: 37-ci-hardening-advisory-debt*
*Completed: 2026-10-31*
