---
phase: 32-release-publication
reviewed: 2026-10-26T18:00:00Z
depth: standard
diff_base: 8dcbbfc1ced091b49abc6d444b562d591ffc28f2
files_reviewed: 3
files_reviewed_list:
  - docs/ROLLOUT-v1.7.0_gg5.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 5
  info: 8
  total: 13
status: issues_found
rounds: 2
---

# Phase 32: Code Review Report

**Reviewed:** 2026-10-26
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Round 1

Scope: the three documents against `32-COMMAND-EVIDENCE.md` (E-01..E-05, E-10, E-11, E-20..E-22), `32-AMENDMENT-01.md`, the Phase 30 and 31 evidence, and the live repository. I re-ran `git diff --quiet v1.7.0_gg4 HEAD -- src/main` (exit 0), the six `git diff --shortstat` rows of E-22 (identical to the documents), the nightly `grep` (identical to the note), the `CHANGELOG.md` diff (additions only, no removed line, no line over 120 characters in the new section, no planning ID) and `git rev-parse v1.7.0_gg4 pre-v1.7.0_gg5` (both `21c51598...`). Numbers, run ids, SHAs and the four-row archive wording agree with the evidence. The defects below are one false dated statement, one overclaim about Dependabot, an overclaim about what the note "shows", and a rollback step that cannot be carried out as written.

### WR-01: FORK-DELTA dates the Dependabot `exclude-paths` reduction to `v1.7.0_gg4`, but it happened after gg4

**File:** `docs/FORK-DELTA.md:83` (Dependabot row, last sentence of the "Why upstream does not provide it" cell)
**Issue:** The row ends "Since `v1.7.0_gg4` the list holds only `integrationTests.yaml`". That is false. `git show v1.7.0_gg4:.github/dependabot.yml` lists four `exclude-paths` entries (`increment-version.yml`, `integrationTests.yaml`, `pre-release.yaml`, `release.yaml`). The reduction to one entry is in commit `496de32a` (2026-10-24, Phase 30), after the gg4 release of 2026-10-23, and `git diff v1.7.0_gg4 HEAD -- .github/dependabot.yml` shows the three removals. So the reduction is a gg5 change. `CHANGELOG.md` says "now excludes only", and the rollback bullet of the note ("on the branch, the `v1.7.0_gg4` ... `dependabot.yml`") already assumes it is a gg5 change, so the three documents disagree. The same wrong claim sits in the E-21 source table ("list holds only `integrationTests.yaml` since `v1.7.0_gg4`"), so the evidence was not independent of the text.
**Fix:** Replace the sentence with "In this release (`v1.7.0_gg5`) the list holds only `integrationTests.yaml`; `v1.7.0_gg4` still listed `increment-version.yml`, `pre-release.yaml` and `release.yaml`, which lets Dependabot keep the release-path pins current", and correct the E-21 row the same way.

### WR-02: FORK-DELTA says the Dependabot `github-actions` entry "is live", which E-05 does not show

**File:** `docs/FORK-DELTA.md:83`
**Issue:** "Dependabot reads this file from the default branch, `custom_build`, and version updates are on, so the entry is live: `32-COMMAND-EVIDENCE.md` E-05 lists the open Dependabot pull requests against `custom_build`." E-05 supports the default branch (`custom_build`) and two open pull requests, `#10 dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2` and `#4 dependabot/gradle/clickHouseDriver-0.10.0`. Both belong to the `gradle` ecosystem. No `github-actions` pull request was observed, the entry is monthly, and the same cell still says `exclude-paths` support "was confirmed from Dependabot's source, not from an observed run". "Live" is therefore an inference from a different ecosystem, and the cell contradicts itself. Pointing the reader at a planning-evidence file for the proof also leaves the shipped document unable to prove it.
**Fix:** "Dependabot reads this file from the default branch, `custom_build`, and version updates are on for the `gradle` ecosystem (open pull requests `#4` and `#10`); no `github-actions` pull request has been observed yet, so the `github-actions` entry and its `exclude-paths` are untested in a run".

### WR-03: The note's opening promise that each claim "shows the command that measured it and its output" is not true for several blocks

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:7` (promise), `:27-37` (section 2), `:50-63` (section 3)
**Issue:** The note says "Each of the five claims below shows the command that measured it and its output". (a) Section 3: the only command shown is the `system.query_log` SQL, whose result is the single user-agent line. The two MBean lines in the same `text` block come from a scratch JVM probe (`Phase32RolloutProbe`, E-02) that is neither shown nor reproducible by an operator, and no command lists MBeans. The block reads as the SQL's output. It also shows `Mac OS X; jvm:17.0.9` as if from the operator's server. (b) Section 2: the `text` block is three hand-picked lines (`grep` of `build-tip.log`, E-03) presented as the output of `./gradlew clean createConfluentArchive`, not marked as an excerpt. The `BUILD SUCCESSFUL in 8s` line comes from a run with `--console=plain` that the command line does not show. (c) Section 3 says "Measured on ClickHouse 26.10.3.7", but the operator-facing text of "wildcard forms keep working" is unmeasured. D-03 asks for measured command and output, so an operator cannot tell which lines they can reproduce.
**Fix:** Reword the intro to "Each claim names how it was checked; where the output is an excerpt or comes from a test probe, the text says so." In section 3, split the block: one `text` block for the SQL result, and a sentence "the MBean names were listed from a running sink task by a test probe (JMX), for both releases". In section 2, label the block "excerpt of the build log" and drop the unshown timing, or show the exact `grep`.

### WR-04: Rollback step 3 cannot be carried out as written: it names an archive that no release provides and gives no build command

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:105`
**Issue:** "Redeploy the archive built at tag `v1.7.0_gg4`, the same commit." E-05 shows 0 published GitHub releases and section 2 says no GitHub release exists for `v1.7.0_gg5`; the gg4 note said "No release asset is published for any tag, so the archive is always your own build" and gave the command. The gg5 note drops both, so an operator who did not keep the gg4 archive has nothing to redeploy and no instruction to build it. Section 2's "What to do" gives the command only for the gg5 tag. The step also gives no instruction to keep the deployed archive before the rollback commit (the gg4 note did), although the rollback removes this document from the branch.
**Fix:** "3. Redeploy the `v1.7.0_gg4` archive. If you kept the one you deployed, use it. Otherwise check out tag `v1.7.0_gg4` in a clean worktree and run `./gradlew createConfluentArchive`; no release asset exists for any tag, so the archive is always your own build. It passes the archive check of section 2 and differs from the `v1.7.0_gg5` archive only in the release identity."

### IN-01: "The shipped archive" implies a distributed artifact, while no GitHub release or asset exists

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:21`, `CHANGELOG.md:18-19` (third Important Changes bullet), `docs/FORK-DELTA.md:246`
**Issue:** D-01 and section 2 say no `GitHub release` is published and the release is its two tags. All three documents still say "the shipped archive differs from the `v1.7.0_gg4` one". Nothing is shipped; the operator builds it. The compared archives are local builds (E-03: archive A and B built in worktrees), and E-03 itself declines whole-zip equality (SHA-256 differs), so "the `v1.7.0_gg4` archive" is also ambiguous.
**Fix:** "The archive built from tag `v1.7.0_gg5` differs from the one built from tag `v1.7.0_gg4`, entry by entry, only in the release identity: ...".

### IN-02: Rollback step 1 drops the gg4 note's warning that the reset overwrites untracked files

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:92`
**Issue:** The gg4 note ended the warning with "and it overwrites untracked files at paths that the tag contains". The gg5 note keeps only "discards uncommitted changes to tracked files". An operator who has an untracked file at a path the tag holds is not told, and `git stash -u` is offered only as one option among three.
**Fix:** Restore the clause: "...discards uncommitted and staged changes to tracked files and overwrites untracked files at paths that the tag contains".

### IN-03: FORK-DELTA says the gg5 figures were measured "at the commit that carries this document"

**File:** `docs/FORK-DELTA.md:131` (raw numbers paragraph)
**Issue:** E-22 states the figures were measured at `39465506`, the commit before the one that carries the final text (`a92aae5f`). The figures are unchanged (re-run now: 287/31774/731, 37/2877/241, and the per-directory rows 6/616/92, 5/1486/25, 1/23/1), so this is wording only, but the phrase is copied from the gg4 sentence and was not true as written. Also, "the last two commands beneath the table reproduce the figures on the `v1.7.0_gg5` tag once it exists" depends on the publication step; the commands fail until then.
**Fix:** "measured on `sync/v1.7.0` at the commit before the one that carries this document; no file in the measured scopes changed in between".

### IN-04: Skip-inventory text omits the `NIGHTLY_ENABLED` condition and mixes two skip counts

**File:** `docs/FORK-DELTA.md:232` (Named skip inventory)
**Issue:** "Since `v1.7.0_gg5` the fork-owned nightly ... runs the whole `integrationTest` task on CI when it is dispatched or scheduled." A scheduled run happens only while `NIGHTLY_ENABLED` is `true`, and the nightly is not on `custom_build` until the merge (E-05: 404). The next sentence gives "55 tests, 54 passed, 1 skipped" and then says `ExactlyOnceTest` and `ClickHouseCloudTest` are both skips of that run; 31's E-18 shows the console counts 1 skipped (the cloud class) and the XML lists 9 (8 `ExactlyOnceTest` methods plus 1), so the two statements read inconsistent.
**Fix:** "...when it is dispatched, or scheduled while `NIGHTLY_ENABLED` is `true`", and "the console summary counts 1 skipped (`ClickHouseCloudTest`); the JUnit XML also lists the 8 `ExactlyOnceTest` methods as skipped".

### IN-05: The rollback never says to publish the rollback commit

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:93-103`
**Issue:** Step 2 ends with a local commit and the `ok` check. Nothing says the commit must reach `origin/custom_build` for the release workflows, the pin guard and `NIGHTLY_ENABLED` handling to roll back on GitHub, and section 5 later says the branch "has `VERSION` `v1.7.0_gg4` and no nightly workflow file", which is true locally only. The gg4 note had the same gap, but gg5 adds a workflow whose scheduled runs only stop after the push.
**Fix:** Add "4. Push `custom_build` (a normal push, or the pull request your branch rules require) so GitHub stops running the nightly".

### IN-06: Nightly text does not mention that GitHub can disable a schedule on its own

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:69-72`
**Issue:** The job-condition text matches the workflow (`github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`, line 25), and the note states exactly that. It is silent that a scheduled workflow in a public repository can be disabled by GitHub after a period without repository activity, which `32-COMMAND-EVIDENCE.md` E-05 already shows happened to upstream's `integrationTests.yaml` (`disabled_manually`, last schedule 2026-10-16) and the earlier FORK-DELTA text said for `test-head.yaml`. A green first manual run and `NIGHTLY_ENABLED=true` therefore do not guarantee a daily signal. This is not measured here.
**Fix:** One sentence: "GitHub may disable scheduled runs of a repository without recent activity; if the daily run is missing, check the workflow's state in the Actions tab and re-enable it".

round: 1 findings: 10

## Mandated confirmations (CIG-02)

- Commands, numbers and run ids against the evidence: PASS. Every figure in the three files equals its evidence entry: runs `40296330498` and `40296481765` (E-04), `7801 inner entries` and `BUILD SUCCESSFUL in 8s` (E-03 `build-tip.log`), `26.10.3.7` and the client and MBean names (E-02), `21c51598` for both `pre-v1.7.0_gg5` and `v1.7.0_gg4` (E-05, re-run), 55 tests/54/1 (31 E-18), the four-row archive wording of `32-AMENDMENT-01.md` (`changed 4`). Section-level presentation problems are in WR-03.
- Overclaims: FAIL. See WR-03 (the "shows the command that measured it and its output" promise and unmarked excerpts), WR-02 ("the entry is live" without a `github-actions` pull request) and WR-01 (a wrong "since `v1.7.0_gg4`"); IN-01 (the "shipped archive" wording) is the milder case. "Only in the release identity" is supported at entry-by-entry content level (E-03), not at whole-zip level, and the documents say "entry by entry" in the CHANGELOG.
- Rollback steps and the `pre-v1.7.0_gg5` target: FAIL on completeness, PASS on correctness. The tag `pre-v1.7.0_gg5` names `21c51598`, equal to `v1.7.0_gg4`; the `gradlew.bat` exception, the ff-only start, `git read-tree -u --reset pre-v1.7.0_gg5`, the empty `git diff --quiet` and the forward path (`git revert <rollback-commit>`, note readable at `<rollback-commit>^`) match E-11 as rehearsed. Step 3 cannot be performed as written (WR-04); see also IN-02 and IN-05.
- Nightly on/off text against `NIGHTLY_ENABLED`: PASS. The note, the CHANGELOG bullet and the FORK-DELTA row agree with `.github/workflows/nightly-integration-tests.yaml:25`: a manual dispatch always runs, the schedule (03:17 UTC) runs only while `NIGHTLY_ENABLED` is `true`, otherwise the job is skipped. The only deviation is the shortened FORK-DELTA skip-inventory phrase (IN-04) and the unmentioned schedule auto-disable (IN-06).
- No promise of a GitHub release: PASS. The note says "No GitHub release is published for `v1.7.0_gg5`: the release is its two tags"; the CHANGELOG and FORK-DELTA describe the workflows' behaviour without promising a release or asset for this version. The word "shipped" is loose (IN-01), and WR-04 notes the missing build instruction that follows from there being no asset.
- No host or secret: PASS. No host name, credential, token, connector or cluster name in the three files; only angle-bracket stand-ins. The nightly is stated to use no repository secret and the workflow file has no `secrets.` or `GITHUB_TOKEN` reference.
- Released CHANGELOG sections untouched and no planning IDs: PASS. `git diff 8dcbbfc1 HEAD -- CHANGELOG.md` has additions only (the first 40 lines); everything from the `# 1.7.0_gg4, 2026-10-23` heading down is unchanged; the new section holds no requirement, decision, evidence or phase ID and no line over 120 characters.
- FORK-DELTA's `DEPS-F4` paragraph replaced, its rows and the raw numbers: PARTIAL. The old "left exactly as upstream ships them" paragraph with the `DEPS-F4` claim is gone; `DEPS-F4` now appears only in the release row as upstream's reason; `main` is no longer called the default branch (the `test-head.yaml` and Dependabot rows name `custom_build`); the Contents anchors match the new headings. The raw numbers are exact: the two after-gg5 rows (287/31774/731 and 37/2877/241), the two gg5 commands and the growth paragraph (277 to 287, 29679 to 31774, 643 to 731; 6/616/92, 5/1486/25, 1/23/1) re-run to the same values (wording only, IN-03). The rows are not clean: the Dependabot row has WR-01 and WR-02.
- The empty `src/main` diff stated consistently in all three documents: PASS. The note (section 1, with the command and exit 0), the CHANGELOG first Important Changes bullet ("`src/main` is identical to `1.7.0_gg4`") and the FORK-DELTA syncer paragraph ("`git diff v1.7.0_gg4 -- src/main` is empty") agree with each other and with the live `git diff --quiet v1.7.0_gg4 HEAD -- src/main` (exit 0); the identity `v1.7.0_gg5`, the rollback target `21c51598` and the change set (workflows, nightly, guards, harness, `dependabot.yml`, `VERSION`, documents) agree across the three.
- The note free of review text: PASS. `docs/ROLLOUT-v1.7.0_gg5.md` contains no reviewer finding, round or disposition text (D-04); the only planning-style content is operator instructions.

## Round 2

Scope: the fix commits `d5b2d0b3` to `cde8fd7a`, `b23f4247`, `c4efe113`, `2cf2b569` and the evidence commit `99a5087d` (`git diff 57232664 HEAD -- docs CHANGELOG.md`: 5 changed lines in `CHANGELOG.md`, 8 in `docs/FORK-DELTA.md`, 26 in `docs/ROLLOUT-v1.7.0_gg5.md`). I re-read each hunk against the files and against `32-COMMAND-EVIDENCE.md` E-02, E-30, E-31, E-32 and `31-COMMAND-EVIDENCE.md` E-18, and re-ran `git diff --quiet v1.7.0_gg4 HEAD -- src/main` (exit 0), `git diff --quiet 39465506 HEAD -- src build.gradle.kts gradle .github` (exit 0), `git show v1.7.0_gg4:build.gradle.kts` (holds `verifyConfluentArchive`), the CHANGELOG line-length and removed-line checks (the new section has no line over 120 characters, no removed line against `8dcbbfc1`, no planning ID) and a search for `shipped` in the note, the new CHANGELOG section and the gg5 FORK-DELTA paragraph (none left). Round 1 fixes WR-01, WR-02, WR-04, IN-01, IN-02, IN-03 and IN-04 are correct and introduced no new number or cross-document disagreement: the Dependabot row now agrees with `git show v1.7.0_gg4:.github/dependabot.yml` (four entries) and with `496de32a` (2026-10-24, not an ancestor of the tag); the skip counts agree with `31-COMMAND-EVIDENCE.md` E-18 (55 = 54 + 1 in the console, 63 testcases and 9 skipped in the XML); the section 3 MBean and user-agent lines equal the E-02 `PROBE-` lines of the gg5 run, and the "(Mac OS X; jvm:17.0.9)" caveat is true. IN-06 was rejected by the orchestrator and I do not re-raise it. The rollback is now four steps and E-30 rehearses steps 1 and 2 verbatim (its text says so); steps 3 and 4 are not exercised, and the findings below are about step 4 and about two leftovers of the WR-03 fix.

### WR-05: Rollback step 4 does not work when the branch rules require a pull request, because steps 1 and 2 commit on `custom_build` itself

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:97-98, 111`
**Issue:** Step 1 checks out `custom_build` and step 2 makes `<rollback-commit>` on it. Step 4 then says "a normal push, or the pull request your branch rules require". A pull request cannot have `custom_build` as both head and base, and a direct push of that commit is exactly what branch rules that require pull requests refuse (the planning documents of this phase call `custom_build` a protected base branch, and the release itself is a merge). An operator in that situation has a local `custom_build` that is ahead of `origin/custom_build`, no branch to open the pull request from, and no instruction for the next minute of the incident. E-30 states that step 4 is not executed by the rehearsal, so nothing shows the step works. The forward path also depends on the SHA of `<rollback-commit>`, which a squash or rebase merge of such a pull request changes; the note does not say which merge kind keeps it.
**Fix:** In step 2 start with `git switch -c rollback-v1.7.0_gg5` (the commands that follow are unchanged), and make step 4: "Publish the rollback commit. Where `custom_build` accepts pushes, run `git push origin HEAD:custom_build`. Where it requires a pull request, run `git push origin rollback-v1.7.0_gg5`, open the pull request into `custom_build` and merge it with a merge commit, so the rollback commit keeps its SHA for the forward path below. Until the rollback commit is on `origin/custom_build`, GitHub keeps running the `v1.7.0_gg5` workflows there, the scheduled nightly included."

### IN-07: The CHANGELOG still says "wildcard matchers keep working", the claim the note retracted in the WR-03 fix

**File:** `CHANGELOG.md:15`
**Issue:** The WR-03 fix replaced "wildcard forms keep working" in the note (section 3, "What to do") by "matchers that do not spell out the version are unaffected", because no wildcard matcher was run against either release (E-02 lists MBean names and a user agent, nothing that matches them). The CHANGELOG bullet in the same release section keeps the unmeasured wording, so the two documents say different things about the same operator action, and only one of them is limited to what was observed.
**Fix:** Say the same in both: "Update dashboard, alert and exporter matchers that spell out the `v1.7.0_gg4` value; matchers that do not spell out the version are unaffected."

### IN-08: The reworded opening promise "Each claim below names how it was checked" is still broader than the text

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:7`, `:21` (the list of what the release changes), `:95` and `:115-121` (section 5)
**Issue:** After the WR-03 fix sections 2 and 3 do name their method and mark the excerpt and the probe. The sentence at line 7 still covers every claim, and several name no method: the list "What the release does change" in section 1, "Nothing in the data format changes" and the "Rolling back brings back" list in section 5. They are true as far as the diff shows (`git diff v1.7.0_gg4 v1.7.0_gg5 --stat` lists exactly those areas), but the note says it names how each was checked. This is the same kind of promise that WR-03 removed, one level smaller.
**Fix:** "Where a claim rests on a command or a test, the text names it and says whether the output is an excerpt or comes from a test probe; the lists of what changes and what a rollback brings back come from the diff between the two tags."

round: 2 findings: 3

## Mandated confirmations (CIG-02)

- Commands, numbers and run ids against the evidence: PASS. The round 2 hunks add no new number except the fixed ones, and each equals its evidence: the build excerpt (`7801 inner entries`, `BUILD SUCCESSFUL in 8s` from E-03, with the `grep` and `--console=plain` of E-31, which read `10s` and the note says the time varies), the MBean and user-agent lines (E-02 `PROBE-` lines of the gg5 run, `26.10.3.7`, `clickhouse-java-v2/0.9.6`), the four `exclude-paths` entries of `v1.7.0_gg4` and `496de32a` of 2026-10-24 (E-32, re-run), the skip counts 55/54/1 and 9 in the XML (`31-COMMAND-EVIDENCE.md` E-18), the Dependabot pull requests `#4` and `#10` (E-05) and the unchanged `pre-v1.7.0_gg5` commit `21c51598`.
- Overclaims: PASS WITH FINDINGS. "Shipped" is gone from the three documents' gg5 text (IN-01 fixed in all three places), "the entry is live" is replaced by what E-05 shows (WR-02 fixed; "no `github-actions` pull request has been observed" rests on the open-pull-request list of E-05 and is worded as an observation, which holds), and "since `v1.7.0_gg4`" is corrected (WR-01 fixed). What remains is smaller: the opening sentence of the note (IN-08) and the unmeasured "wildcard matchers keep working" in the CHANGELOG (IN-07).
- Rollback steps and the `pre-v1.7.0_gg5` target: PASS on the target, FAIL on completeness of step 4 (WR-05). Tag `pre-v1.7.0_gg5` names `21c51598`, equal to `v1.7.0_gg4`; the `gradlew.bat` exception, the ff-only start, `git read-tree -u --reset pre-v1.7.0_gg5`, the empty `git diff --quiet` and the forward path match E-30 as rehearsed on `2cf2b569` (steps 1 and 2 only, as E-30 says). Step 1 again warns about untracked files (IN-02 fixed), step 3 now says how to get the `v1.7.0_gg4` archive and the tag does build with `verifyConfluentArchive` (WR-04 fixed), and step 4 names the push (IN-05 fixed) but not what to do when `custom_build` takes no direct push.
- Nightly on/off text against `NIGHTLY_ENABLED`: PASS. Note section 4, the CHANGELOG bullet and the FORK-DELTA row agree with `.github/workflows/nightly-integration-tests.yaml:25`: a manual dispatch always runs, the schedule (03:17 UTC) runs only while `NIGHTLY_ENABLED` is `true`; the skip-inventory sentence now says "scheduled while `NIGHTLY_ENABLED` is `true`" (IN-04 fixed).
- No promise of a GitHub release: PASS. The note still says "No GitHub release is published for `v1.7.0_gg5`: the release is its two tags" and step 3 says "no release asset exists for any tag"; the new sentences promise no release or asset, and the archive is described as built from the tag.
- No host or secret: PASS. The round 2 hunks add only angle-bracket stand-ins, a test-machine OS and JVM string marked as the test machine's, and no host, credential, connector or cluster name.
- Released CHANGELOG sections untouched and no planning IDs: PASS. `git diff 8dcbbfc1 HEAD -- CHANGELOG.md` has no removed line; the round 2 change is inside the `1.7.0_gg5` section only, whose longest line is within 120 characters and which holds no requirement, decision, evidence or phase ID.
- FORK-DELTA's `DEPS-F4` paragraph replaced, its rows and the raw numbers: PASS. The `DEPS-F4` paragraph stays replaced (the term appears only as upstream's reason in the release row), `main` is still not called the default branch, and the Dependabot row now dates the `exclude-paths` reduction to `v1.7.0_gg5`. The raw numbers are unchanged by the fixes and still equal E-22: `git diff --quiet 39465506 HEAD -- src build.gradle.kts gradle .github` exits 0, so "no file in the measured scopes changed in between" holds (IN-03 fixed).
- The empty `src/main` diff stated consistently in all three documents: PASS. The note (section 1, command and exit 0), the CHANGELOG first Important Changes bullet and the FORK-DELTA syncer paragraph (`git diff v1.7.0_gg4 -- src/main` is empty) agree, and the live `git diff --quiet v1.7.0_gg4 HEAD -- src/main` exits 0; the archive sentence is now identical in meaning in all three (built from the tag, entry by entry, only the release identity).
- The note free of review text: PASS. The note holds operator instructions only; the new sentences describe the probe, the excerpt and the rollback, and name no finding, round or disposition.

---

_Reviewed: 2026-10-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
