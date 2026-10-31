---
phase: 37-ci-hardening-advisory-debt
verified: 2026-10-31T13:30:00Z
status: human_needed
score: 3/5 roadmap success criteria verified (0 failed, 1 behavior-unverified, 1 not meetable as worded and routed to human judgement); 8/8 requirement ids have implementation evidence, ADV-03 only in a reduced form
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
covered_digest: "v4:sha256:d80327d52be5566b7a9a374d407cfaef5da4dd3ca0dba4f4b43915093e4dd3d5"
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "Criterion 3 (CIH-04): release.yaml and pre-release.yaml run the compare against the VERSION-keyed allow-list, rebuild the baseline from the previous tag in a separate worktree and publish the report as an artifact"
    test: "Dispatch release.yaml (or pre-release.yaml) as a dry run from a scratch branch (the user-gated handoff 37-USER-DISPATCH-DRYRUN.md, evidence entry E-12), or read Phase 38's release dry run"
    expected: "The run fails at the step 'Compare the archive with the previous release' with about 238 UNALLOWED lines (three of them libzstd-jni natives) and uploads the artifact archive-compare-report whose last line reads RESULT: FAIL. A failure earlier than the compare step, in the full-history checkout, the git worktree, the baseline build or the upload, is a CI-only defect"
    why_human: "The chain has never run on GitHub (increment-version.yml has 0 runs, the newest release.yaml run predates the chain). It was verified by reading, by guard tests, by mutation runs and by replaying the run blocks locally with the runner's shell options. None of that exercises the runner-only parts. Phase 38 may not edit .github/, so a defect that only CI shows has no phase left to fix it"
human_verification:
  - test: "Decide what ADV-03 and ROADMAP criterion 5 mean. The criterion says the record gives 'the workflow run that created it' and 'closes the no run exercised the publish path caveat'"
    expected: "Either accept 37-RELEASE-RECORD.md as the ADV-03 deliverable in its reduced form (release, tag commit, asset name, size and SHA-256 recorded from the REST API; no creating run exists; the caveat stays open until Phase 38 publishes gg6 through release.yaml from custom_build), and reword ROADMAP criterion 5 and REQUIREMENTS ADV-03 to match, or reject that reduction and say what else would satisfy it"
    why_human: "The wording cannot be met. Entry E-08 measured created_by_workflow_run: no and publish_path_exercised: no: the release and its asset were created by the user's own account outside any run, and the only publish-mode run ended cancelled before its publish step. A record of those facts cannot close the caveat. Whether a recorded impossibility counts as the criterion being met is a decision, not a test result"
  - test: "Decide the five src/main and assertion rows that the ledger left dispositioned: 26 IN-02 (unused shouldSkipDescribeRow overloads), 27 WR-01 (withheld unknown-enum-label message), 27 IN-01 (passesThroughUnchanged order), 28 IN-01 (public ESCAPED_NEWLINE constant), 28 IN-05(b) (null assertion not tightened)"
    expected: "For each, either keep the disposition with the owner and re-open trigger already recorded, or give explicit approval for the src/main edit or the assertion change"
    why_human: "ROADMAP says a src/main change in this phase needs the user's approval, and changing an existing assertion needs sign-off. Neither was given, so all five are dispositions, as the criterion allows. Whether that is the outcome the user wanted is the user's call"
  - test: "Decide whether to run the optional CI dry run (E-12) before Phase 38, and whether to close the log-forging gap UF-37-1 in this phase"
    expected: "A recorded choice. If the dry run is skipped, Phase 38's release dry run is the first CI exercise of the chain. UF-37-1 (release.yaml:126 and the twin: 'cat \"$report\"' prints archive entry names verbatim, so an entry name holding a newline can forge a workflow command line in the job log) is low and non-blocking; fixing it edits both release workflows and a guard row, which only a Phase 37 gap plan may do"
    why_human: "Both are risk-acceptance decisions that cost a push to the public origin (dry run) or a gap plan (UF-37-1). The security audit rated UF-37-1 low and reachable only if the connector's own dependency or code ships such an entry name"
  - test: "Decide who corrects two inaccurate statements about pipefail and the wrong class name in a header comment"
    expected: "Accept leaving them until the next edit of those files, or open a small gap. (a) The comment at release.yaml:108 and the 'E-11 could not have shown this defect' note in 37-REVIEW-FIX.md say that defaults.run.shell: bash runs without pipefail. A runner log of run 40693904004 shows '--noprofile --norc -e -o pipefail' for the same setting, so review WR-01's premise was false and the added 'set -o pipefail' is redundant but harmless (UF-37-2). (b) The header of increment-version.yml names ReleaseWorkflowGuardUnitTest; the class that pins the file is IncrementVersionWorkflowGuardUnitTest (UF-37-8). Owner for both is the user, because Phase 38 may not edit .github/"
    why_human: "Documentation accuracy with no behavioural effect. Both sit in files this phase's own rules forbid later phases from touching"
  - test: "When writing the gg6 allow-list in Phase 38, respect the rule added by review fix WR-02: a row whose first character is '*', '?' or '[' is refused with exit 2, and so is a glob of only stars"
    expected: "The gg6 list names dependency namespaces and exact connector class names, like the gg5 list. A row such as '*.class' or '[!x]*' will not load. Short literal prefixes such as 'c*' or 'com/*' do load and can excuse every connector class, so they need a reviewer's eye (UF-37-3)"
    why_human: "A hand-written list for the next release is written last and judged by a person. The script only rejects the shapes it can see"
---

# Phase 37: CI Hardening & Advisory Debt Verification Report

**Phase Goal:** The fork's release tooling stops carrying known defects. `increment-version.yml` can bump a `_ggN` version without writing `vnull`, because its logic is a script with a test. The release workflows compare the built Confluent archive entry by entry against the previous release, under an allow-list keyed by `VERSION`, so an archive change such as the zstd-jni native jar can no longer ship unseen. Every review advisory carried from Phases 25-31 is fixed, or dispositioned with an owner and a re-open trigger. The real `v1.7.0_gg5` release is on the record. No connector behaviour changes.
**Verified:** 2026-10-31
**Status:** human_needed
**Re-verification:** No, initial verification
**Verified tree:** `sync/v1.7.0` at `e5d85761`. `.planning/config.json` and `.planning/state.json` are modified and unstaged; they were not read as evidence.

The code half of the goal is achieved and re-run on the tip: the version script, the archive compare, the baseline-tag rule, the guards and the advisory fixes all exist, are wired and pass. No truth FAILED and no blocker was found. The status is `human_needed` for three reasons that no test settles. First, the CI wiring of the compare chain has never run on GitHub. Second, ROADMAP criterion 5 and ADV-03 are worded in a way the facts cannot meet. Third, a group of src/main and assertion rows is dispositioned pending the user's approval.

## Goal Achievement

### Observable Truths (ROADMAP Phase 37 success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | *[Docker-free]* The version logic is a script with a table-driven test that runs with Docker stopped (CIH-01, CIH-02): four rules (validation against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`, `gg` bumps the counter and turns `vX.Y.Z` into `vX.Y.Z_gg1`, `patch`/`minor`/`major` reset the suffix to `_gg1`, invalid input fails and writes nothing); no case writes `vnull`; `increment-version.yml` calls the script, installs no npm `semver`, keeps its SHA pins and permissions and passes the pin guard; the "known and left unfixed" note is gone; FORK-DELTA no longer calls the defect open | VERIFIED | `.github/scripts/bump_version.py` exists and `increment-version.yml` reads: `NEW_VERSION="$(python3 -I .github/scripts/bump_version.py "$BUMP")"` with `BUMP: ${{ inputs.version }}` taken from `env:`, no `setup-node`, no `npm`, no expression inside a `run:` block, `actions/checkout` pinned to a 40-hex SHA with `# v7.0.1`, top-level `contents: read` and job-level `contents: write` plus `pull-requests: write`. `grep` for "left unfixed" finds nothing in `.github` or `docs/FORK-DELTA.md`; the FORK-DELTA row (line 82) states the old defect as history and the new behaviour. My own run of the script on a throwaway copy in `/tmp/gsd37/verify/bump`: `gg` turned `v1.7.0_gg5` into `v1.7.0_gg6` in `VERSION` and the anchors row; a `VERSION` of `vnull` was refused with exit 1 and both files unchanged (`cmp` equal); `patch`, `minor` and `major` from `v1.7.0_gg6` gave `v1.7.1_gg1`, `v1.8.0_gg1` and `v2.0.0_gg1`, each with the anchors row moved. Docker-free test: `testUnit --rerun --offline` on a `git archive` export of `e5d85761` with `DOCKER_HOST` pointing at a socket that does not exist: 80 test classes, 1658 tests, 0 failures, 0 errors, 2 skipped (the number in `37-REVIEW-FIX.md`). `BumpVersionScriptUnitTest` ran 52, `IncrementVersionWorkflowGuardUnitTest` 17, `WorkflowActionPinGuardUnitTest` 21. The mutation red runs for the workflow guard are recorded in E-03, E-04 and the audit's M3 and M4 |
| 2 | *[Docker-free]* The version bump also edits the `VERSION` row of `fork-anchors.txt`, or the script fails when that row does not match; seen red on a fixture (CIH-03) | VERIFIED | `fork-anchors.txt:41` is `VERSION	v1.7.0_gg5	1	BLD-07` and `bump_version.py` writes it in the same step. In my throwaway run the row moved to `v1.7.0_gg6` and, after the major bump, to `v2.0.0_gg1`. The refusal for a mismatched, missing or duplicated row is covered by the table test (52 rows, green on the tip) and by the audit's A-05 and A-13, which also show the old workflow's effect (a `VERSION`-only bump) failing the anchors mirror with `bad=1`. The second-rename failure restores the anchors file (review WR-04, fixed in `27f9ff89`). `37-evidence-check.py anchors .planning/scripts/fork-anchors.txt` on the tip: `rows=172 bad=0` |
| 3 | *[Docker-free]* The compare script lives under `.github/scripts/`; its synthetic-archive self-test covers UNALLOWED and STALE-ROW and exit codes 0, 1, 2 (CIH-05). `release.yaml` and `pre-release.yaml` run it against an allow-list keyed by `VERSION` and publish the report as an artifact; the baseline is rebuilt from the previous tag in a separate worktree; the `REL-xx` anchor counts and `TestsWorkflowPinGuardUnitTest` are updated and both pass (CIH-04) | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | Script half VERIFIED: `.github/scripts/compare_confluent_archives.py` exists and `CompareConfluentArchivesScriptUnitTest` (44) is green in the 1658-test run. I ran the script from an empty directory with `python3 -I` on the three real archives left in `/tmp/gsd37/w/archives` under the committed gg5 allow-list: `base_gg4` against `cand_gg5` exit 0, `RESULT: PASS`; `base_gg4` against `cand_tip` exit 1, `RESULT: FAIL`, 238 UNALLOWED lines, six lines naming `libzstd` (the three natives and their related entries); the gg5 list plus one extra row naming nothing: exit 1 with `STALE-ROW zz/Nothing.class`; a list holding only `*`: exit 2 (`a glob of only stars matches every difference and is refused`); a missing allow-list: exit 2. These are the same verdicts E-07 and the audit's A-14 recorded. Workflow half present and wired: `release.yaml` has, in order, `Select the baseline tag` (fails closed on a missing baseline tag or allow-list; the list name is `confluent-archive-allowlist-${RELEASE_VERSION}.txt`), `Build the baseline archive from the previous tag` (`git worktree add --detach` at the tag, the tag's own wrapper), `Compare the archive with the previous release` (no `if:`, no `continue-on-error`, `exit "$status"` on non-zero), `Upload the archive compare report` (`if: always()`, artifact `archive-compare-report`), then the tests gate and publish. `pre-release.yaml` differs from `release.yaml` in eight line pairs only (names, the prerelease flags and their expectation), checked by my own `diff`. The allow-lists were renamed by `VERSION` with rename history kept (`R100`). `ReleaseWorkflowGuardUnitTest` (37) and `TestsWorkflowPinGuardUnitTest` (44) are green. What no run has shown is the runner side: the full-history checkout, `git worktree add` of a tag on the runner, the baseline build under `setup-java` and `setup-gradle`, the artifact upload and the shape of the report as a downloaded artifact. The chain was replayed locally with the runner's shell options (E-11) and the dry run (E-12) is pending with the user. By the rule in Step 3 this truth is present and wired but not behaviourally proven in CI, so it is not counted as verified and is routed to human verification |
| 4 | *[record]* The Phase 30 rows (WR-01..WR-03, IN-01..IN-04) and Phase 31 rows (WR-01..WR-03, IN-01..IN-03) are each closed: fixed with a commit pointer, or dispositioned with an owner and a re-open trigger (ADV-01); the same for the 16 unfixed findings of `29-MILESTONE-CLOSE-RECORD.md` (ADV-02); a diff check shows no existing assertion deleted or weakened, and an assertion changes only with the user's sign-off | VERIFIED (with a judgement item) | `37-ledger-check.py check` on `37-ADVISORY-LEDGER.md`: `OK ledger: 36 rows, 0 problems`; `counts`: 29 bases (7 + 6 + 16), 15 fixed, 3 closed, 18 dispositioned. I counted the 16 against the close record: 25 (WR-01..03, IN-01..04 = 7), 26 IN-02, 27 (WR-01, IN-01..03 = 4), 28 (IN-01, IN-02, IN-04, IN-05 = 4). The ten distinct commit pointers resolve and, apart from the Phase 30 commit `496de32a` that the ledger declares, lie in `ff873086..HEAD`. Every dispositioned row carries an owner and a re-open trigger (read in the file). Assertion diff: `git diff -U0 ff873086..HEAD` over `src/test`, `src/testFixtures` and `src/integrationTest` shows only two removed lines that match `assert`, and both are `assertNull` and `assertNotNull` in `DescribeUnreadableEnumMockServerTest`, re-indented unchanged inside a `try (chc)` block (28 IN-05(a)); I read the result. No added `@Disabled`, `Assumptions` or `assumeTrue`. The null assertion of 28 IN-05(b) is deliberately left. `src/main`, `build.gradle.kts`, `gradle/`, `VERSION`, `tests.yaml` and the nightly workflow have an empty diff against `ff873086` (0 paths). The judgement item is the five rows that need the user's approval (human item 2) |
| 5 | *[record, CI]* The real `v1.7.0_gg5` GitHub release of 2026-10-28 is recorded as evidence, read from the REST API: the release, **the workflow run that created it**, its tag commit, and the attached archive's name, size and SHA-256. **This closes the "no run exercised the publish path" caveat** (ADV-03) | ? UNCERTAIN (human judgement) | The record exists and is complete for what exists: `37-RELEASE-RECORD.md` gives release id `426690002`, tag `v1.7.0_gg5`, tag commit `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (equal to the origin `custom_build` tip, which I re-read with `git ls-remote`), asset `clickhouse-kafka-connect-v1.7.0_gg5.zip`, 12453556 bytes, SHA-256 `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767` (matching size and digest from the API and from a downloaded copy), and the three `release.yaml` runs. It cannot give a creating run, because E-08 measured `created_by_workflow_run: no` and `publish_path_exercised: no`: author and asset uploader are the user's own account, the asset was uploaded about four and a half minutes after a successful dry run, and the only publish-mode run (40474419526) was cancelled before `Publish release`. The record says so under the heading "Deviation from the ROADMAP wording" and says "The caveat stays open until Phase 38"; I found no closing phrase in it. I did not mark the criterion passed. Part of it (the record and the archive facts) is met; two parts (a creating run, and closing the caveat) are not met and cannot be. ROADMAP.md:347 and REQUIREMENTS.md:48 still carry the old wording and ADV-03 is still "Pending" there (UF-37-7). **This looks intentional in the sense of unavoidable; it is not a defect of implementation.** The decision is human item 1 |

**Score:** 3/5 truths verified (criteria 1, 2, 4); 1 present, behaviour-unverified (criterion 3); 1 uncertain and routed to human judgement (criterion 5).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/scripts/bump_version.py` | table-tested version script writing `VERSION` and its anchors row | ✓ VERIFIED | Run by me on a copy, see truth 1 and 2 |
| `.github/workflows/increment-version.yml` | calls the script, no npm, no expression in `run:` | ✓ VERIFIED | Read in full |
| `.github/scripts/compare_confluent_archives.py` | in-memory zip compare with UNALLOWED, STALE-ROW, exit 0/1/2 | ✓ VERIFIED | Run by me, five outcomes, see truth 3 |
| `.github/scripts/baseline_tag.py` | baseline tag below `VERSION`, whole-text grammar | ✓ VERIFIED | `BaselineTagScriptUnitTest` 19 green; the workflow feeds it `git tag --merged HEAD` |
| `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt`, `-v1.7.0_gg5.txt` | allow-lists keyed by `VERSION` | ✓ VERIFIED | Renamed from the `-29` and `-32` lists with `R100`; the gg5 list loads under the WR-02 rule and gives PASS for gg4 to gg5 |
| `release.yaml`, `pre-release.yaml` compare chain | select, baseline build, compare, upload before the tests gate and publish | ⚠️ ORPHANED from CI evidence | Present, read, guard-tested and replayed locally; never run on GitHub (truth 3) |
| `ReleaseWorkflowGuardUnitTest`, `IncrementVersionWorkflowGuardUnitTest`, `CompareConfluentArchivesScriptUnitTest`, `BumpVersionScriptUnitTest`, `BaselineTagScriptUnitTest` | Docker-free tests | ✓ VERIFIED | 37, 17, 44, 52, 19 tests in the green 1658-test `testUnit` |
| `37-ADVISORY-LEDGER.md`, `37-RELEASE-RECORD.md`, `37-USER-DISPATCH-DRYRUN.md` | records and handoff | ✓ VERIFIED | Read; ledger checker green |
| `fork-anchors.txt` rows for the new files | anchors in the same commits as the code | ✓ VERIFIED | `rows=172 bad=0` |
| `37-assets/*.txt` | scratch copies of scripts | ⚠️ stale | Two assets are older than the committed scripts (UF-37-5); re-applying them with the plans' `cp` recipe would undo review fixes WR-02 and WR-04. Not part of the shipped tree |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `increment-version.yml` `Increment version` | `bump_version.py` | `python3 -I .github/scripts/bump_version.py "$BUMP"` with `BUMP` from `env:` | WIRED (guard pins it; hostile input refused by argparse, A-13) |
| `bump_version.py` | `fork-anchors.txt` VERSION row | same-step rewrite and refusal on mismatch | WIRED (my run) |
| `increment-version.yml` `Commit and push` | the two files | `git add VERSION .planning/scripts/fork-anchors.txt` (anchor row `CIH-03`, `fork-anchors.txt:165`) | WIRED |
| `release.yaml` / `pre-release.yaml` select step | `baseline_tag.py` and the allow-list named by `VERSION` | `git tag --merged HEAD --list 'v*'`, fail closed | WIRED by reading and local replay; not run on GitHub |
| compare step | publish and tag steps | no `if:`, no `continue-on-error`, `exit "$status"`; steps after it have implicit `success()` | WIRED by reading and guard (`ReleaseWorkflowGuardUnitTest`, mutation runs M1/M2); not run on GitHub |
| compare step | `archive-compare-report` artifact | `upload-artifact` with `if: always()` | PRESENT; never exercised on a runner |
| ledger rows | commit pointers | `37-ledger-check.py pointers` resolves in git | WIRED |

### Data-Flow Trace (Level 4)

The compare reads two zips given by path and one allow-list file; it writes only to stdout, and I saw the verdict depend on the archive bytes and the list rows (PASS, FAIL with 238 UNALLOWED, STALE-ROW on an unused row). The bump script reads `VERSION` and the anchors row and writes both; the printed version is built from integers, so no file text reaches `GITHUB_ENV` (audit A-05). No value in either flow ends in a static return.

### Behavioral Spot-Checks and Probe Execution

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite on the tip, Docker unreachable | `env -u TESTCONTAINERS_HOST_OVERRIDE DOCKER_HOST=unix:///nonexistent.sock ./gradlew testUnit --rerun --offline` in a `git archive` export of `e5d85761` under `/tmp/gsd37/verify/tree` | 80 classes, 1658 tests, 0 failures, 0 errors, 2 skipped | PASS |
| Bump rules on a throwaway copy | `python3 -I .github/scripts/bump_version.py gg\|patch\|minor\|major` and a `vnull` input | `v1.7.0_gg6`; `v1.7.1_gg1`, `v1.8.0_gg1`, `v2.0.0_gg1`; `vnull` refused, files unchanged; anchors row follows | PASS |
| Compare polarities on real archives | `compare_confluent_archives.py` on `/tmp/gsd37/w/archives/{base_gg4,cand_gg5,cand_tip}/archive.zip` under the gg5 list | exit 0 PASS; exit 1 FAIL with 238 UNALLOWED; stale row exit 1; star-only list exit 2; missing list exit 2 | PASS |
| Anchors mirror | `37-evidence-check.py anchors .planning/scripts/fork-anchors.txt` | `rows=172 bad=0` | PASS |
| Ledger | `37-ledger-check.py check`, `counts` | 36 rows, 0 problems; 29 bases | PASS |
| `src/main`, build, `gradle/`, `VERSION`, `tests.yaml`, nightly diff since phase start | `git diff --name-only ff873086..HEAD` over those paths | 0 paths | PASS |
| Debt markers in added code and workflow lines | scan of added lines for TBD, FIXME, XXX, TODO, HACK in `*.java`, `*.py`, `*.yml`, `*.yaml` | 0 matches | PASS |
| Origin untouched | `git ls-remote origin refs/heads/custom_build` | `3a3a15a9ae064ffe09c696b8bb87bce23419d463` | PASS |
| Full Docker-backed suite, tail merge gate | not re-run here | E-18 records 2231 tests and 18 named skips on `latest`, and `result=PASS` for the merge gate, at the gated head; since then only `.github` files and Docker-free test classes changed (audit A-02) | SKIPPED (long, recorded) |
| CI dry run of the release workflow (E-12) | not run, user-gated | pending by design | SKIPPED |
| Probe scripts | none declared by the plans | none | SKIPPED |

Scratch work lived under `/tmp/gsd37/verify/` and was not committed.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CIH-01 | 37-01 | `gg` step, validation, suffix reset, never `vnull` | SATISFIED | Truth 1, my run |
| CIH-02 | 37-01 | script with table test, no npm, pins and permissions kept, note removed | SATISFIED | Truth 1 |
| CIH-03 | 37-01 | bump also moves the `VERSION` anchors row, or the script fails | SATISFIED | Truth 2 |
| CIH-04 | 37-02, 37-03 | compare in both release workflows, baseline from a worktree, artifact, anchors and pin guard updated | SATISFIED statically, behaviour unverified in CI | Truth 3, human item 3 and `behavior_unverified_items` |
| CIH-05 | 37-02 | compare script under `.github/scripts/` with a synthetic-archive self-test | SATISFIED | Truth 3 |
| ADV-01 | 37-03, 37-04, 37-05 | Phase 30 and 31 rows fixed or dispositioned | SATISFIED | Truth 4 |
| ADV-02 | 37-04, 37-05 | 16 unfixed findings fixed or dispositioned | SATISFIED, with five rows awaiting the user's approval | Truth 4, human item 2 |
| ADV-03 | 37-02, 37-05 | real `v1.7.0_gg5` release recorded, caveat closed | PARTIAL: record made; creating run and caveat closure unmeetable as worded | Truth 5, human item 1 |

No orphaned requirements: REQUIREMENTS.md maps CIH-01..05 and ADV-01..03 to Phase 37 and the five plans claim all eight. The REQUIREMENTS.md status column still reads Pending for all eight, and ROADMAP's Phase 37 line still shows `Plans: TBD`; both are bookkeeping for the orchestrator.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `release.yaml`, `pre-release.yaml` | 126 | `cat "$report"` prints archive entry names verbatim to the job log (UF-37-1) | Warning | A newline inside a zip entry name can put a forged `::error` or `::stop-commands` line at the start of a log line; log and annotation forging only, the exit status and counts do not depend on it. Reachable only if the connector's own tree or a dependency ships such an entry |
| `release.yaml`, `pre-release.yaml` | 108 | comment says `defaults.run.shell: bash` runs without `pipefail` | Info | Inaccurate: the runner's shell for the same setting is `-e -o pipefail` (audit A-04). The added `set -o pipefail` is redundant and harmless (UF-37-2) |
| `increment-version.yml` | 5 | header comment names `ReleaseWorkflowGuardUnitTest` where `IncrementVersionWorkflowGuardUnitTest` pins the file | Info | Wrong class name, no behavioural effect (UF-37-8); owner the user, trigger the next edit of that file |
| review IN-01, IN-03, IN-04, IN-05, IN-06 | various | workflows accept `_gg0` and leading zeros that the scripts refuse; a corrupt archive can leave as a traceback with exit 1; the guard's `CHAIN` omits the tag gate; two allow-list headers name the removed script path; a hint text | Info | Each fails closed or is cosmetic; none weakens a gate; open by scope (UF-37-4) |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` in any added line of code or workflow. All four review warnings (WR-01..WR-04) are fixed in `b6fd9a0a`, `2bec20d8`, `058eca48` and `27f9ff89`; I re-ran the underlying behaviour for WR-02 (the star-only and prefix rules) and WR-04 (the refusal and the anchors row) and read WR-01 and WR-03 in the tree.

### Human Verification Required

1. **ADV-03 and ROADMAP criterion 5.** Test: decide whether the reduced record meets the criterion and reword it. Expected: the user accepts `37-RELEASE-RECORD.md` as the deliverable, with the caveat open until Phase 38, and ROADMAP.md:347 and REQUIREMENTS.md:48 are reworded (or the user says what else would satisfy it). Why human: the criterion's own words ("the workflow run that created it", "closes the caveat") describe something that did not happen.
2. **Five dispositioned rows.** Test: for 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 (all `src/main`) and 28 IN-05(b) (an existing assertion), approve an edit or keep the disposition. Expected: a recorded choice per row. Why human: both a `src/main` change and an assertion change need the user's own approval.
3. **CI exercise of the compare chain (E-12) and UF-37-1.** Test: run or skip the user-gated dry run; decide whether the log-forging gap is worth a Phase 37 gap plan. Expected: a recorded choice. Why human: a push to the public origin and a gap plan are the user's to authorise; Phase 38 cannot edit `.github/`.
4. **Two inaccurate statements in `release.yaml:108`/`37-REVIEW-FIX.md` and one wrong class name in `increment-version.yml`.** Test: accept as open or request a small gap. Why human: documentation only, but the files are closed to Phase 38.
5. **gg6 allow-list rule.** Test: when the list is written, check no row starts with `*`, `?` or `[` and read any short-prefix row. Why human: the list is hand-written last, per release.

## Gaps Summary

No gaps in the sense of a failed truth, a missing or stub artifact, or an unwired key link. Every truth I could exercise without GitHub passed on the current tip, and the Docker-free suite is green with Docker unreachable.

What the reader should weigh:

- **Criterion 5 and ADV-03 are not met as worded and cannot be.** The measured fact is that no workflow run created `v1.7.0_gg5`. The record states this plainly and does not claim the caveat closed. This is a decision for the user, not a defect for a closure plan. If the user rejects the reduction, the only thing that closes it is Phase 38's gg6 publish through `release.yaml` from `custom_build`.
- **The new CI wiring has never run on GitHub.** Criterion 3 is therefore counted behaviour-unverified. If the user skips the dry run, Phase 38's release dry run is the first runner exercise, and a defect it shows has no phase that may edit `.github/` and would return to Phase 37 as a gap.
- **The security audit's verdict is SECURED, with 20 threats closed and 0 open.** It carries UF-37-1 (low, unregistered, open) and UF-37-2 (its finding that review WR-01's premise was false). I re-ran the compare and bump behaviours it describes and agree with both flags; I did not re-run its hostile-name battery.
- **The full Docker-backed suite and the merge gate were not re-run by me.** E-18 holds them at the gated head. The diff since then touches only `.github` and Docker-free test classes, and the anchors mirror, which the merge gate also reads, is `bad=0` on the tip.
- **E-12 is pending by design** and is not a pass of anything.

---

_Verified: 2026-10-31T13:30:00Z_
_Verifier: Claude (gsd-verifier)_
