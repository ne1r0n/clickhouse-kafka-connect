---
phase: 37-ci-hardening-advisory-debt
verified: 2026-10-31T11:50:00Z
status: passed
score: 4/5 roadmap success criteria verified (0 failed, 1 behavior-unverified and accepted by the user); 8/8 requirement ids satisfied, CIH-04 statically only
covered_files:
  - ".gitattributes"
  - ".github/scripts/baseline_tag.py"
  - ".github/scripts/bump_version.py"
  - ".github/scripts/compare_confluent_archives.py"
  - ".github/scripts/failed_tests_summary.py"
  - ".github/workflows/increment-version.yml"
  - ".github/workflows/pre-release.yaml"
  - ".github/workflows/release.yaml"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-01-PLAN.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-01-SUMMARY.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-02-PLAN.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-02-SUMMARY.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-03-PLAN.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-03-SUMMARY.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-04-PLAN.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-04-SUMMARY.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-05-PLAN.md"
  - ".planning/phases/37-ci-hardening-advisory-debt/37-05-SUMMARY.md"
  - ".planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt"
  - ".planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java"
covered_digest: "v4:sha256:1bd6f9e948704424c7a158d253da76ea431bff819f3141a0f1debc7cb0ca0436"
behavior_unverified: 1
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 3/5 roadmap success criteria verified (criterion 3 behavior-unverified, criterion 5 routed to human judgement)
  gaps_closed: []
  gaps_remaining: []
  regressions: []
behavior_unverified_items:
  - truth: "Criterion 3 (CIH-04): release.yaml and pre-release.yaml run the compare against the VERSION-keyed allow-list, rebuild the baseline from the previous tag in a separate worktree and publish the report as an artifact"
    test: "Dispatch release.yaml (or pre-release.yaml) as a dry run from a scratch branch (the user-gated handoff 37-USER-DISPATCH-DRYRUN.md, evidence entry E-12), or read Phase 38's release dry run"
    expected: "The run fails at the step 'Compare the archive with the previous release' with about 238 UNALLOWED lines (three of them libzstd-jni natives) and uploads the artifact archive-compare-report whose last line reads RESULT: FAIL. A failure earlier than the compare step, in the full-history checkout, the git worktree, the baseline build or the upload, is a CI-only defect"
    why_human: "The chain has never run on GitHub (increment-version.yml has 0 runs, the newest release.yaml run predates the chain). It was verified by reading, by guard tests, by mutation runs and by replaying the run blocks locally with the runner's shell options. None of that exercises the runner-only parts. Phase 38 may not edit .github/, so a defect that only CI shows has no phase left to fix it"
    user_disposition: "Accepted by the user on 2026-10-31: close the phase with this open; the optional dry run E-12 stays pending with the user (see the Human-judgment disposition section)"
---

# Phase 37: CI Hardening & Advisory Debt Verification Report

**Phase Goal:** The fork's release tooling stops carrying known defects. `increment-version.yml` can bump a `_ggN` version without writing `vnull`, because its logic is a script with a test. The release workflows compare the built Confluent archive entry by entry against the previous release, under an allow-list keyed by `VERSION`, so an archive change such as the zstd-jni native jar can no longer ship unseen. Every review advisory carried from Phases 25-31 is fixed, or dispositioned with an owner and a re-open trigger. The real `v1.7.0_gg5` release is on the record. No connector behaviour changes.
**Verified:** 2026-10-31
**Status:** passed
**Re-verification:** Yes. The first report (tree `e5d85761`) went stale because covered files changed afterwards: the UF-37-1 fix `f523cf3c`, the comment fixes `e1e05604`, the added test coverage `5ba9b84f`, the dry-run handoff and the ROADMAP/REQUIREMENTS ADV-03 rewording. Every truth was re-checked against the real tree, not against the earlier report.
**Verified tree:** `sync/v1.7.0` at `a165f8f6`. `.planning/config.json` and `.planning/state.json` are modified and unstaged; they were not read as evidence. The scratch tree was a `git archive` export of `a165f8f6` under `/tmp/gsd37/verify2/`; nothing was written to the repository except this report.

No truth failed, no artifact is missing or a stub, no key link is unwired and no blocker was found. The code half of the goal is achieved and re-run on the tip. The status is `passed` because the one thing no test can show, the behaviour of the compare chain on a GitHub runner, was put to the user, who accepted it as open (see "Human-judgment disposition"). It stays labelled behaviour-unverified here and is not counted in the score.

## Goal Achievement

### Observable Truths (ROADMAP Phase 37 success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | *[Docker-free]* The version logic is a script with a table-driven test that runs with Docker stopped (CIH-01, CIH-02): validation against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`, `gg` bumps the counter and turns `vX.Y.Z` into `vX.Y.Z_gg1`, `patch`/`minor`/`major` reset to `_gg1`, invalid input fails and writes nothing; no `vnull`; `increment-version.yml` calls the script, installs no npm `semver`, keeps SHA pins and permissions, passes the pin guard; the "known and left unfixed" note is gone; FORK-DELTA no longer calls the defect open | VERIFIED | My own run on a throwaway copy (`/tmp/gsd37/verify2/bump`): `gg` turned `v1.7.0_gg5` into `v1.7.0_gg6` in `VERSION` and the anchors row; `patch`, `minor`, `major` then gave `v1.7.1_gg1`, `v1.8.0_gg1`, `v2.0.0_gg1` with the row following each time; a `VERSION` of `vnull` was refused (`ERROR: current version 'vnull' does not match ...`, exit 1) with `VERSION` and the anchors file byte-identical (`cmp`); a `VERSION` that differs from the anchors row was refused with an ERROR naming the row; the argument `gg; rm -rf /` was refused by argparse (exit 2) and nothing changed. `increment-version.yml` has no `setup-node`, `npm` or `semver` (grep, 0 hits); `grep -n "left unfixed\|vnull"` over it and `docs/FORK-DELTA.md` finds nothing. Docker-free suite on the tip with Docker unreachable: see the spot-check table, 1672 tests, 0 failures. The guard classes are in that run: `BumpVersionScriptUnitTest`, `IncrementVersionWorkflowGuardUnitTest`, `WorkflowActionPinGuardUnitTest` |
| 2 | *[Docker-free]* The bump also edits the `VERSION` row of `fork-anchors.txt`, or the script fails when that row does not match; seen red on a fixture (CIH-03) | VERIFIED | In the throwaway run the row `VERSION	v1.7.0_gg6	1	BLD-07` followed the bump and ended at `v2.0.0_gg1` after the major bump; the mismatch case failed with files unchanged (see truth 1). `37-evidence-check.py anchors .planning/scripts/fork-anchors.txt` on the tip: `rows=172 bad=0`. The refusal for a missing, duplicated or mismatched row and the restore after a failing second rename (review WR-04, `27f9ff89`) are covered by `BumpVersionScriptUnitTest`, green in the run |
| 3 | *[Docker-free]* The compare script lives under `.github/scripts/`; its synthetic-archive self-test covers UNALLOWED and STALE-ROW and exit codes 0, 1, 2 (CIH-05). `release.yaml` and `pre-release.yaml` run it against an allow-list keyed by `VERSION` and publish the report as an artifact; the baseline is rebuilt from the previous tag in a separate worktree; the `REL-xx` anchor counts and `TestsWorkflowPinGuardUnitTest` are updated and both pass (CIH-04) | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED (accepted by the user) | Script half VERIFIED. I ran `compare_confluent_archives.py` with `python3 -I` from an empty directory on the three real archives in `/tmp/gsd37/w/archives` under the committed gg5 allow-list: `base_gg4` against `cand_gg5` exit 0, `RESULT: PASS`; `base_gg4` against `cand_tip` exit 1, `RESULT: FAIL`, 238 `UNALLOWED` lines, six lines naming `libzstd`; the gg5 list plus the row `zz/Nothing.class` exit 1 with `STALE-ROW zz/Nothing.class`; a list of only `*` exit 2 (`a glob of only stars matches every difference and is refused`); a missing list exit 2. `CompareConfluentArchivesScriptUnitTest` now has 58 tests, all green. Workflow half present and wired (read in `release.yaml`): `Select the baseline tag`, `Build the baseline archive from the previous tag` (`git worktree add --detach` at the tag), `Compare the archive with the previous release` (no `if:`, no `continue-on-error`, `exit "$status"` on non-zero), `Upload the archive compare report` (`if: always()`, artifact `archive-compare-report`), then the tests gate and publish; `fetch-depth: 0` on the checkout. `release.yaml` and `pre-release.yaml` differ in 16 diff lines (8 line pairs: names, prerelease flags and their expectation); `ReleaseWorkflowGuardUnitTest` and `TestsWorkflowPinGuardUnitTest` are green in the run. **What no run has shown:** the runner side (full-history checkout, `git worktree add` of a tag, the baseline build under `setup-java` and `setup-gradle`, the artifact upload, the report as a downloaded artifact). The chain was replayed locally with the runner's shell options (E-11); the dry run E-12 is pending with the user. Not counted as verified; the user accepted it as open |
| 4 | *[record]* The Phase 30 rows (WR-01..WR-03, IN-01..IN-04) and Phase 31 rows (WR-01..WR-03, IN-01..IN-03) are each closed: fixed with a commit pointer, or dispositioned with an owner and a re-open trigger (ADV-01); the same for the 16 unfixed findings of `29-MILESTONE-CLOSE-RECORD.md` (ADV-02); a diff check shows no existing assertion deleted or weakened, and an assertion changes only with the user's sign-off | VERIFIED | `37-ledger-check.py check`: `OK ledger: 36 rows, 0 problems`; `counts`: `rows=36 bases=29 fixed=15 closed=3 dispositioned=18`; `pointers` exits 0 (every pointer resolves in git). I read the ledger: every dispositioned row carries an owner and a re-open trigger. `git diff -U0 ff873086..HEAD` over `src/test`, `src/testFixtures`, `src/integrationTest`: two removed lines match `assert`, `assertNull(chc.describeTable(DATABASE, UNREADABLE_TABLE))` and `assertNotNull(chc.describeTable(DATABASE, READABLE_TABLE))`, the same two lines that the first verification read as re-indented unchanged inside a `try (chc)` block (28 IN-05(a)); 0 added `@Disabled`, `assumeTrue` or `Assumptions`. `src/main`, `build.gradle.kts`, `gradle/`, `VERSION`, `tests.yaml`, `integrationTests.yaml`: 0 changed paths since `ff873086`. The five rows left as dispositions were put to the user and kept (see the disposition section) |
| 5 | *[record, CI]* The real `v1.7.0_gg5` GitHub release of 2026-10-28 is recorded as evidence, read from the REST API: the release, its tag commit, the attached archive's name, size and SHA-256, and what the REST API shows about how the release was created. Measured: no workflow run created it, so the "no run exercised the publish path" caveat stays open until the Phase 38 publication (ADV-03, as reworded by the user's decision of 2026-10-31) | VERIFIED (as reworded) | `ROADMAP.md` criterion 5 and `REQUIREMENTS.md` ADV-03 (line 48) carry the reworded text; I read both. `37-RELEASE-RECORD.md` gives release id `426690002`, tag `v1.7.0_gg5`, tag commit `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (equal to the origin `custom_build` tip, re-read with `git ls-remote origin refs/heads/custom_build`), asset `clickhouse-kafka-connect-v1.7.0_gg5.zip`, 12453556 bytes, SHA-256 `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`, the three `release.yaml` runs, and the finding `created_by_workflow_run: no`, `publish_path_exercised: no`. It says the caveat stays open until Phase 38. The record matches the reworded criterion; it claims no creating run and no closure. I did not re-read the GitHub REST API; the record's values are the plan 37-02 readings (E-08) |

**Score:** 4/5 truths verified (criteria 1, 2, 4, 5); 1 present, behaviour-unverified and accepted by the user (criterion 3).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/scripts/bump_version.py` | table-tested version script writing `VERSION` and its anchors row | ✓ VERIFIED | Run by me on a copy, truths 1 and 2 |
| `.github/workflows/increment-version.yml` | calls the script, no npm, no expression in `run:` | ✓ VERIFIED | grep for npm and left-unfixed empty; guard green |
| `.github/scripts/compare_confluent_archives.py` | in-memory zip compare, UNALLOWED, STALE-ROW, exit 0/1/2, log-safe report | ✓ VERIFIED | Run by me, five outcomes plus the hostile-name case below |
| `.github/scripts/baseline_tag.py` | baseline tag below `VERSION` | ✓ VERIFIED | `BaselineTagScriptUnitTest` green |
| `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt`, `-v1.7.0_gg5.txt` | allow-lists keyed by `VERSION` | ✓ VERIFIED | The gg5 list loads and gives PASS for gg4 to gg5 |
| `release.yaml`, `pre-release.yaml` compare chain | select, baseline build, compare, upload before the tests gate and publish | ⚠️ present and wired, never run on GitHub | Truth 3 |
| `ReleaseWorkflowGuardUnitTest`, `IncrementVersionWorkflowGuardUnitTest`, `CompareConfluentArchivesScriptUnitTest` (58), `BumpVersionScriptUnitTest`, `BaselineTagScriptUnitTest` | Docker-free tests | ✓ VERIFIED | All in the green 1672-test run |
| `37-ADVISORY-LEDGER.md`, `37-RELEASE-RECORD.md`, `37-USER-DISPATCH-DRYRUN.md` | records and handoff | ✓ VERIFIED | Read; ledger checker green |
| `fork-anchors.txt` rows for the new files | anchors in the same commits as the code | ✓ VERIFIED | `rows=172 bad=0` |
| `37-assets/*.txt` | scratch copies of scripts | ⚠️ stale | Older than the committed scripts (UF-37-5); not part of the shipped tree |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `increment-version.yml` `Increment version` | `bump_version.py` | `python3 -I .github/scripts/bump_version.py "$BUMP"`, `BUMP` from `env:` | WIRED (hostile argument refused in my run) |
| `bump_version.py` | `fork-anchors.txt` VERSION row | same-step rewrite, refusal on mismatch | WIRED (my run) |
| `release.yaml` / `pre-release.yaml` select step | `baseline_tag.py` and the allow-list named by `VERSION` | `git tag --merged HEAD --list 'v*'`, fail closed | WIRED by reading and local replay; not run on GitHub |
| compare step | publish and tag steps | no `if:`, no `continue-on-error`, `exit "$status"` | WIRED by reading (re-read this run) and by `ReleaseWorkflowGuardUnitTest`; not run on GitHub |
| compare step | `archive-compare-report` artifact | `upload-artifact` with `if: always()` | PRESENT; never exercised on a runner |
| `cat "$report"` and `grep -c '^UNALLOWED '` in the compare step | report text | the script escapes every entry-name line (`safe_line`, UF-37-1 fix `f523cf3c`) | WIRED: the line counts read real lines and a name can no longer forge one (see the hostile-name check) |
| ledger rows | commit pointers | `37-ledger-check.py pointers` | WIRED (exit 0) |

### Data-Flow Trace (Level 4)

The compare reads two zips by path and one allow-list file and writes only to stdout; the verdict depended on the archive bytes and list rows in every run above (PASS, FAIL with 238 UNALLOWED, STALE-ROW on an unused row). Classification uses the raw entry names and `safe_line` is applied once on output (re-review `37-REVIEW-UF.md`; the same four `UNALLOWED` lines before and after the fix). The bump script reads `VERSION` and the anchors row and writes both; the printed version is built from integers. No value in either flow ends in a static return.

### Behavioral Spot-Checks and Probe Execution

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite on the tip, Docker unreachable | `env -u TESTCONTAINERS_HOST_OVERRIDE DOCKER_HOST=unix:///nonexistent/docker.sock ./gradlew testUnit --rerun --offline` in a `git archive` export of `a165f8f6` | `BUILD SUCCESSFUL`, `37-evidence-check.py junit`: 80 files, 1672 tests, 0 failures, 0 errors, 2 skipped; `CompareConfluentArchivesScriptUnitTest` 58 | PASS |
| Bump rules on a throwaway copy | `python3 -I .github/scripts/bump_version.py gg\|patch\|minor\|major`, a `vnull` input, a mismatched anchors row, a hostile argument | `v1.7.0_gg6`; `v1.7.1_gg1`, `v1.8.0_gg1`, `v2.0.0_gg1`; `vnull` refused exit 1, files unchanged; mismatch refused; hostile argument refused exit 2 | PASS |
| Compare polarities on real archives | `compare_confluent_archives.py` on `/tmp/gsd37/w/archives/{base_gg4,cand_gg5,cand_tip}/archive.zip` under the gg5 list | exit 0 PASS; exit 1 FAIL with 238 UNALLOWED and 6 `libzstd` lines; stale row exit 1; star-only list exit 2; missing list exit 2 | PASS |
| Hostile-name report escaping | a synthetic archive pair built in `/tmp/gsd37/verify2/hostile` whose new inner entry is named `p\n::error::forged\r\n##[error]x/q.class` and whose `MANIFEST.MF` holds `::error::ctx`, `::add-mask::x` and an ESC line; run by `python3 -I` from an empty directory | exit 1; stdout shows the entry as one escaped line (`ONLY-B p\n::error::forged\r\n##[error]x/q.class`), the manifest context line as `\ ::error::ctx`, the ESC line as `+\x1b[31m red`; 0 lines start (after whitespace) with `::` or `##[`; 0 raw ESC bytes. A zip path holding a newline and `::error::x` produced one `ERROR:` line with the newline escaped | PASS |
| Anchors mirror | `37-evidence-check.py anchors .planning/scripts/fork-anchors.txt` | `rows=172 bad=0` | PASS |
| Ledger | `37-ledger-check.py check`, `counts`, `pointers` | 36 rows, 0 problems; 29 bases; pointers exit 0 | PASS |
| `src/main`, build, `gradle/`, `VERSION`, `tests.yaml`, `integrationTests.yaml` diff since phase start | `git diff --name-only ff873086..HEAD` over those paths | 0 paths | PASS |
| Debt markers in added code and workflow lines | scan of added lines for TBD, FIXME, XXX, TODO, HACK in `*.java`, `*.py`, `*.yml`, `*.yaml` | 0 matches | PASS |
| Origin untouched | `git ls-remote origin refs/heads/custom_build` | `3a3a15a9ae064ffe09c696b8bb87bce23419d463` | PASS |
| Full Docker-backed suite, merge gate | not re-run here | E-18 records 2231 tests and 18 named skips on `latest` and `result=PASS` at the gated head `b2b2d265`. Since then only `.github` files and Docker-free test classes changed (the two UF fixes, one comment line in each release workflow, the 58-test compare class). Not re-run since E-18 | SKIPPED (long, recorded); labelled honestly |
| CI dry run of the release workflow (E-12) | not run, user-gated | pending with the user by design; not a pass of anything | SKIPPED |
| Probe scripts | none declared by the plans | none | SKIPPED |

Scratch work lived under `/tmp/gsd37/verify2/` and was not committed.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CIH-01 | 37-01 | `gg` step, validation, suffix reset, never `vnull` | SATISFIED | Truth 1, my run |
| CIH-02 | 37-01 | script with table test, no npm, pins and permissions kept, note removed | SATISFIED | Truth 1 |
| CIH-03 | 37-01 | bump also moves the `VERSION` anchors row, or the script fails | SATISFIED | Truth 2 |
| CIH-04 | 37-02, 37-03 | compare in both release workflows, baseline from a worktree, artifact, anchors and pin guard updated | SATISFIED statically; behaviour on a runner unverified and accepted by the user | Truth 3 |
| CIH-05 | 37-02 | compare script under `.github/scripts/` with a synthetic-archive self-test | SATISFIED | Truth 3 |
| ADV-01 | 37-03, 37-04, 37-05 | Phase 30 and 31 rows fixed or dispositioned | SATISFIED | Truth 4 |
| ADV-02 | 37-04, 37-05 | 16 unfixed findings fixed or dispositioned | SATISFIED; five rows stay dispositions by the user's decision | Truth 4 |
| ADV-03 | 37-02, 37-05 | real `v1.7.0_gg5` release recorded; no creating run; caveat open until Phase 38 | SATISFIED as reworded | Truth 5 |

No orphaned requirements: REQUIREMENTS.md maps CIH-01..05 and ADV-01..03 to Phase 37 and the five plans claim all eight. Bookkeeping left to the orchestrator: the REQUIREMENTS.md traceability column still reads `Pending` for all eight (ADV-03 at line 127), and ROADMAP's Phase 37 line still shows `Plans: TBD`.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `release.yaml`, `pre-release.yaml` | 126 (was) | `cat "$report"` printed archive entry names verbatim (UF-37-1) | Resolved | The script now escapes control characters, the backslash and a leading `::` or `##[` on every output line; shown by my hostile-name run and pinned by 58 tests, including the manifest-diff and `ERROR:` paths (`5ba9b84f`, WR-01 of `37-REVIEW-UF.md`) |
| `release.yaml`, `pre-release.yaml` | 108 | comment on `set -o pipefail` | Resolved | Now states that the runner's bash already runs with `-o pipefail` (UF-37-2, `e1e05604`); the line is 173 columns and cites "a runner log" without a pointer (`37-REVIEW-UF.md` IN-03, Info) |
| `compare_confluent_archives.py` | 57, 112, 127 | NBSP, zero-width and bidi characters are not escaped; an uncaught zip error prints a traceback and exits 1, not 2 | Info | `37-REVIEW-UF.md` IN-01 and IN-02: cosmetic or defence in depth, no command forgery, left open out of scope |
| `increment-version.yml` | 5 | header comment names `ReleaseWorkflowGuardUnitTest` where `IncrementVersionWorkflowGuardUnitTest` pins the file | Info | UF-37-8, no behavioural effect; owner the user, trigger the next edit of that file |
| review IN-01, IN-03, IN-04, IN-05, IN-06 (`37-REVIEW.md`) | various | workflows accept `_gg0` and leading zeros that the scripts refuse; a corrupt archive can leave as a traceback; the guard's `CHAIN` omits the tag gate; two allow-list headers name the removed script path; a hint text | Info | Each fails closed or is cosmetic; none weakens a gate; open by scope (UF-37-4) |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` in any added line of code or workflow. All four review warnings of `37-REVIEW.md` (WR-01..WR-04) are fixed (`b6fd9a0a`, `2bec20d8`, `058eca48`, `27f9ff89`) and the one warning of `37-REVIEW-UF.md` is fixed (`5ba9b84f`). `37-SECURITY.md`: `status: secured`, 20 threats, 0 open.

### Human Verification Required

None open. Every item that needed a human decision was answered by the user (below). The one thing that stays unverified, truth 3 on a GitHub runner, is listed in `behavior_unverified_items` with the user's disposition.

## Gaps Summary

No gaps: no failed truth, no missing or stub artifact, no unwired key link, no blocker anti-pattern.

What the reader should weigh:

- **The compare chain has never run on GitHub.** Criterion 3 is counted behaviour-unverified and is not in the score. The user accepted this. If the optional dry run E-12 is skipped, Phase 38's release dry run is the first runner exercise, and a defect it shows has no phase that may edit `.github/` and would return to Phase 37 as a gap.
- **The full Docker-backed suite and the merge gate were not re-run since E-18.** The Docker-free suite was re-run in this verification (1672 tests, 0 failures, 2 skipped); the diff since the gated head touches only `.github` files and Docker-free test classes, and the anchors mirror is `bad=0`.
- **The caveat "no run exercised the publish path" stays open until Phase 38** publishes through `release.yaml` from `custom_build`. This is what criterion 5 now says; it is not a defect.
- **E-12 is pending by design** and is not a pass of anything.

---

_Verified: 2026-10-31T11:50:00Z_
_Verifier: Claude (gsd-verifier)_

## Human-judgment disposition (2026-10-31)

The first verifier run returned `human_needed`. The user answered (AskUserQuestion, 2026-10-31) as follows. This re-run records the same answers; `passed` rests on them and on a re-run of the checks above, with no check result altered to fit.

1. Close the phase accepting the open items: the CI wiring has never run on GitHub (criterion 3 stays behavior-unverified; the optional dry-run E-12 stays pending with the user); the five dispositioned rows stay dispositions with owners (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 in `src/main`, and 28 IN-05(b)); `src/main` is not changed.
2. ADV-03 and ROADMAP criterion 5 were reworded to say that no workflow run created `v1.7.0_gg5` and that the caveat stays open until Phase 38. The record is complete for what exists.
3. UF-37-1 (log forging via archive entry names) was to be fixed now. It is fixed in `f523cf3c` and its coverage was completed by `5ba9b84f` (`37-REVIEW-UF.md`, `37-REVIEW-UF-FIX.md`). UF-37-2 (false premise of review WR-01) was corrected in `e1e05604`. These fixes post-date the first verifier tree `e5d85761`; the Docker-free suite was re-run after them (1672 tests, 0 failures, 2 skipped), again in this verification; the Docker-backed suite was not re-run since E-18.
4. Open for Phase 38 (information, not a decision item): the gg6 allow-list must respect the WR-02 glob rule (a row starting with `*`, `?` or `[` is refused; short literal prefixes such as `c*` load and need a reviewer's eye); the `increment-version.yml` header comment names the wrong guard class (owner the user); review Info items and the three Info findings of `37-REVIEW-UF.md` remain open.
