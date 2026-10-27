---
phase: 32-release-publication
verified: 2026-10-27T09:40:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".planning/phases/32-release-publication/32-01-PLAN.md"
  - ".planning/phases/32-release-publication/32-01-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-02-PLAN.md"
  - ".planning/phases/32-release-publication/32-02-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-03-PLAN.md"
  - ".planning/phases/32-release-publication/32-03-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-04-PLAN.md"
  - ".planning/phases/32-release-publication/32-04-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-05-PLAN.md"
  - ".planning/phases/32-release-publication/32-05-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-06-PLAN.md"
  - ".planning/phases/32-release-publication/32-06-SUMMARY.md"
  - ".planning/phases/32-release-publication/32-07-PLAN.md"
  - ".planning/phases/32-release-publication/32-07-SUMMARY.md"
  - ".planning/scripts/confluent-archive-allowlist-32.txt"
  - ".planning/scripts/fork-anchors.txt"
  - "CHANGELOG.md"
  - "VERSION"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg5.md"
covered_digest: "v3:sha256:21b22d8657158e4587c2fe789265b097db8df0a81c18da16d5db40ad2c502286"
behavior_unverified: 0
overrides_applied: 1
overrides:
  - must_have: "Rounds repeated until one found nothing, at most three, and every finding's disposition is recorded"
    reason: "Round 3 still reported two Info findings (IN-09, IN-10); the three-round cap was reached. The user chose 'Fix, accept unreviewed' and both findings were fixed in 904efdb7 and 8b57ca7a without a fourth round (32-REVIEW-DISPOSITION.md round3_decision). Both fixes touch only docs/ROLLOUT-v1.7.0_gg5.md rollback wording."
    accepted_by: "user (direct answer to orchestrator AskUserQuestion, 32-REVIEW-DISPOSITION.md)"
    accepted_at: "2026-10-26T23:59:00Z"
human_verification:
  - test: "Resolve the five judgment-tier prohibitions (unverified-prohibition, non-authoritative LLM verdict recorded in 'Prohibitions' below): 32-02 and 32-03 'no claim the evidence does not show'; 32-04 'no inline executor review counted as a round'; 32-06 'no push without the user's own direct answer'; 32-07 'no setting change, dispatch or relayed answer by an agent'"
    expected: "You confirm that each held. Evidence found supports all five: every document number re-derived or matched to an E-nn entry; each review round is recorded as a separately spawned gsd-code-reviewer; E-71 records the direct 'publish' answer before the first push; E-81/E-82 record the dispatch and the variable as the operator's own acts"
    why_human: "Judgment-tier prohibitions cannot be proven absent by grep; the reviewer-spawn provenance and 'who clicked' facts exist only as orchestrator and operator reports"
  - test: "Confirm that the repository variable NIGHTLY_ENABLED was set after (not before) the green run 40474535605 was recorded, and that its value is what you intend"
    expected: "Set at 2026-10-27T09:15:07Z per your report, after E-81 was committed at 09:14:18Z; the only scheduled run so far (40477788530, 09:02:27Z) is skipped"
    why_human: "The unauthenticated REST API cannot read repository variables, so E-82 rests on the operator's report (also noted in 32-SECURITY.md note 1)"
---

# Phase 32: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg5` is published on `custom_build`. Operators get a short, independently reviewed rollout note that says what changed in the workflows and that no connector code changed. FORK-DELTA records the newly fork-owned workflows and the nightly. The suite is green twice on `latest` x V2, and the published SHA has exactly one green `tests.yaml` run before the tags are pushed.
**Verified:** 2026-10-27
**Status:** human_needed (all five roadmap success criteria hold in the repo and on origin; the remaining items are the flagged judgment-tier prohibitions and one operator-attested fact)
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth (roadmap success criterion) | Status | Evidence |
|---|---|---|---|
| 1 | SC1 (CIG-02): `VERSION` reads `v1.7.0_gg5`, its anchor row changes in the same commit, CHANGELOG has a complete `1.7.0_gg5` section, FORK-DELTA no longer says the release workflows are left as upstream ships them (DEPS-F4) and has the new rows, raw numbers refreshed once | VERIFIED | `git show v1.7.0_gg5:VERSION` = `v1.7.0_gg5`; commit `a6bfd78f` changes `VERSION` and `fork-anchors.txt` together; anchors diff since `8dcbbfc1` is exactly the one VERSION row (`-...gg4 +...gg5`). `CHANGELOG.md`: `# 1.7.0_gg5, 2026-10-26` with Important Changes, Known Issues and Build and CI; `git diff 8dcbbfc1 HEAD -- CHANGELOG.md` = 41 added, 0 removed lines. `docs/FORK-DELTA.md`: new `### CI workflows` group (line 72) with rows for release.yaml/pre-release.yaml with `gh release create`, increment-version, nightly, `WorkflowActionPinGuardUnitTest` (CIG-01), `UpstreamIntegrationWorkflowBytesGuardUnitTest` (NIT-02), Dependabot `github-actions`; "left as upstream ships them" no longer occurs; `DEPS-F4` survives once, as upstream's reason in the release row. Raw numbers re-derived live: `v1.7.0..v1.7.0_gg5` wider scope 287 files/31774/731, `src/main` 37/2877/241, gg4 277/29679/643, equal to the table. |
| 2 | SC2 (CIG-02): short rollout note with five measured claims; independently reviewed, max three rounds, every disposition recorded | VERIFIED (override: stop rule) | `docs/ROLLOUT-v1.7.0_gg5.md` is 122 lines, five sections: (1) empty `src/main` diff, re-run live: `git diff --quiet v1.7.0_gg4 v1.7.0_gg5 -- src/main` exit 0; (2) wrapper-built archive with `verifyConfluentArchive` (E-03, E-31: `7801 inner entries`), runs `40296330498`/`40296481765` exist and are green; (3) MBean `version=` and client name, E-02 `PROBE-MBEAN`/`PROBE-AGENT` lines for gg4 and gg5 equal the note; (4) nightly on/off, the three grep lines match `.github/workflows/nightly-integration-tests.yaml:16,17,25`; (5) rollback, rehearsed in E-11/E-30/E-33 (steps 3 and 4 stated as not rehearsed). `32-REVIEW.md` has rounds 3 with 10, 3, 2 findings; `32-REVIEW-DISPOSITION.md` lists 15 findings (14 fixed, 1 rejected), `open: 0`, each fix commit hash present in git (IN-09 `904efdb7`, IN-10 `8b57ca7a` checked). Deviation: round 3 did not find nothing; the cap was reached and the user accepted the two Info fixes unreviewed. Carried as override. |
| 3 | SC3 (CIG-03): two green `--rerun` suite runs on `latest` x V2 with the same 18 named skips; Docker-stopped `testUnit`; anchors gate; re-fetch; user approval; non-force `--no-ff` merge with tree equal to rehearsed tip; exactly one green `tests.yaml` run on the published SHA before tags; scratch branch deleted | VERIFIED | E-60: run 1 and run 2 both 1961 tests, 0 failed, 18 skipped, equal per-class digest, server 26.10.3.7; `testUnit` 1374 tests, 0 failed with Docker stopped. E-61: `anchor_rows=131 result=PASS`, selftest no failures; gate scripts, exemptions and every `.java`/`.kts` file unchanged since `8dcbbfc1` (re-checked). E-71: direct "publish" answer precedes the first push. Live: `git cat-file -p 3a3a15a9` has parents `21c51598` and `41e7d546` (non-fast-forward `--no-ff` merge); tree `abb6c93e` equals X0 `41e7d546`'s tree; `21c51598` is an ancestor (no force). GitHub REST (read now): `tests.yaml` runs for `3a3a15a9` = 1, `40416215635` push success, updated 2026-10-26T23:02:21Z; for X0 = 1, `40414210750` success. Tags pushed ~23:04:2xZ, after completion. `git ls-remote origin`: `v1.7.0_gg5` -> `3a3a15a9`, `pre-v1.7.0_gg5` -> `21c51598` (= tag `v1.7.0_gg4`); no `sync/v1.7.0` on origin; `main` unchanged. Minor deviation: the late single-run re-check ran 117 s after completion (plan asked 120 s); a second check at 198 s showed the same single run (E-72). |
| 4 | SC4 (CIG-02/CIG-03): after publication `integrationTests.yaml` still disabled, nightly in recorded state, no workflow run created an unintended release or tag | VERIFIED | GitHub REST read now: `integrationTests.yaml` state `disabled_manually`, 2 schedule runs, newest 2026-10-16 (none after the marker); `nightly-integration-tests.yaml` state `active`; `releases` = 0; `git ls-remote --tags` shows only the intended two new tags; `integrationTests.yaml` SHA-256 equals upstream `v1.7.0`'s. Unintended event: `release.yaml` run `40474419526` (workflow_dispatch, 2026-10-27T08:34:11Z) is `cancelled`, listed by the API; the user states it was a mis-click they cancelled, it ended during the archive build step before any publish step, and it created no release and no tag (releases 0, tags unchanged). |
| 5 | SC5 (NIT-03): the user dispatches the nightly after the merge; run green or failures named in a skip list; `NIGHTLY_ENABLED` set only after the run is recorded; no scheduled run with a conclusion other than `skipped` precedes it; no edit to build/gradle/.github | VERIFIED (operator-attested part flagged) | REST read now for the nightly workflow: run `40474535605` event `workflow_dispatch`, head `3a3a15a9` (= M), conclusion `success` (63 tests, 0 failures, 9 skipped per E-81 annotations); run `40477788530` event `schedule`, created 09:02:27Z (after the dispatch run), conclusion `skipped`. No run precedes the dispatch. `git diff --name-only 0791a8a7 HEAD` outside `.planning/` is exactly CHANGELOG.md, VERSION, docs/FORK-DELTA.md, docs/ROLLOUT-v1.7.0_gg5.md; zero files under `src`, `build.gradle.kts`, `gradle`, `.github`. Green run so no skip-list change was needed. Ordering of the variable: E-82 records "set by operator 2026-10-27T09:15:07Z" after E-81 was committed (`ad9a2dcb`, 09:14:18Z); the value of a repository variable cannot be read unauthenticated, so this part rests on the operator's report (human item 2). |

**Score:** 5/5 truths verified (1 via accepted override), 0 present-but-behavior-unverified.

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `VERSION` | `v1.7.0_gg5` | VERIFIED | on HEAD and on tag |
| `.planning/scripts/fork-anchors.txt` | VERSION row moved only | VERIFIED | 131 anchor rows; one-row diff |
| `.planning/scripts/confluent-archive-allowlist-32.txt` | four exact rows per 32-AMENDMENT-01 | VERIFIED | Version.class, ClickHouseHelperClient.class, SinkTaskStatistics.class, META-INF/MANIFEST.MF; no wildcard; E-03: `only-a 0 only-b 0 changed 4`, `RESULT: PASS`, javap proof present |
| `CHANGELOG.md` | complete 1.7.0_gg5 section | VERIFIED | additions only |
| `docs/FORK-DELTA.md` | CI group, DEPS-F4 sentence replaced, numbers refreshed | VERIFIED | see SC1 |
| `docs/ROLLOUT-v1.7.0_gg5.md` | one page, five claims | VERIFIED | 122 lines; wording consistent with the amendment ("four" release-identity entries) |
| `32-REVIEW.md`, `32-REVIEW-DISPOSITION.md` | three rounds, dispositions, open 0 | VERIFIED | 15 findings, open 0 |
| `32-COMMAND-EVIDENCE.md` | E-01..E-82 | VERIFIED | all cited entries exist; ids and SHAs cross-checked against git and REST |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| tag `v1.7.0_gg5` | merge `3a3a15a9` | lightweight tag on origin | WIRED | `git ls-remote origin` |
| tag `pre-v1.7.0_gg5` | `21c51598` (= `v1.7.0_gg4`) | origin tag | WIRED | rollback target as the note states |
| rollout note section 4 | nightly workflow job condition | `vars.NIGHTLY_ENABLED == 'true'` | WIRED | line 25 |
| `VERSION` | anchors row | single commit `a6bfd78f` | WIRED | |
| FORK-DELTA numbers | git | recomputed | WIRED | equal |

### Data-Flow Trace (Level 4)

Not applicable: the phase ships documents, a version string and refs; no dynamic rendering.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| src/main identical gg4 -> gg5 | `git diff --quiet v1.7.0_gg4 v1.7.0_gg5 -- src/main` | exit 0 | PASS |
| Merge shape | `git cat-file -p 3a3a15a9` | parents `21c51598`, `41e7d546`; tree equal to X0 | PASS |
| Origin refs | `git ls-remote origin` | as above | PASS |
| One green tests.yaml run on M and X0 | REST `tests.yaml/runs?head_sha=` | 1 and 1, both success | PASS |
| Raw numbers | `git diff --shortstat v1.7.0 v1.7.0_gg5 ...` | equal to FORK-DELTA | PASS |
| Anchors invariants on current HEAD | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | `invariants: provenance_paths=21 anchor_commits=955 anchor_rows=131 marker_commits=1014 anchor_exempt=19 result=PASS` (ran about 17 minutes) | PASS |
| Suite and `testUnit` re-run | not executed here (the verifier did not start Docker or the 3-minute suites); E-60 evidence consulted | n/a | SKIPPED |

Note: the suite results (two 1961-test runs, `testUnit`) rest on the recorded E-60 output; I confirmed that no `src`, test, `.kts`, `gradle/` or `.github` file changed after the gated head, so the recorded result applies to the published tree.

### Probe Execution

No probe scripts are declared or conventional for this phase (`scripts/*/tests/probe-*.sh` absent). Skipped.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| CIG-02 | 32-01, 32-02, 32-03, 32-04 | FORK-DELTA, CHANGELOG, `VERSION=v1.7.0_gg5` and a short rollout note updated | SATISFIED | SC1, SC2 |
| CIG-03 | 32-05, 32-06, 32-07 | CI green on the published SHA | SATISFIED | SC3, SC4; run `40416215635` success on `3a3a15a9` |
| NIT-03 | 32-07 | Nightly passes on a manual dispatch before the schedule is enabled | SATISFIED | SC5; run `40474535605` success |

All three IDs claimed by plan frontmatter are accounted for, and REQUIREMENTS.md maps no additional ID to Phase 32 (no orphans).

Bookkeeping drift (WARNING, not a goal gap): `.planning/REQUIREMENTS.md` still shows `[ ]` and `Pending` for CIG-02, CIG-03 and NIT-03 (lines 21, 26, 27, 57, 59, 60). The phase-completion step should flip them.

### Prohibitions (must_haves.prohibitions)

Test-tier prohibitions (all in plans 32-01 to 32-07 with `verification: test`) were checked against state, not against plan narration:

| Prohibition | Result |
|---|---|
| No edit under `src/`, `build.gradle.kts`, `gradle/`, `.github/`, invariant scripts, or earlier allow-lists/notes | HELD: zero files changed in those paths since the 32-01 base `0791a8a7`; gate scripts unchanged since `8dcbbfc1` |
| Only intended refs pushed, no force, no `--tags` | HELD: origin gained exactly the two tags; `custom_build` moved by a two-parent merge whose first parent is the old tip |
| No released CHANGELOG section edited | HELD: 0 removed lines |
| No GitHub release created | HELD: REST `releases` = 0 |
| No fix to a red nightly | HELD: nightly was green; no `.github`/`src` change |

Judgment-tier prohibitions: `unverified-prohibition - human review recommended`. My verdict below is a NON-AUTHORITATIVE LLM judgment, not a pass.

| Plan | Prohibition | LLM verdict |
|---|---|---|
| 32-02 | MUST NOT state a claim the evidence does not show | Not violated on the claims I re-derived (src/main exit, MBean/agent lines, nightly lines, tag/commit names, run ids); three reviewer rounds also checked this |
| 32-03 | MUST NOT state a workflow or guard fact the file, test or evidence does not show | Not violated on the facts I re-derived; one FORK-DELTA row ("no `github-actions` pull request observed") is out of date after publication (see Gaps/Notes) |
| 32-04 | MUST NOT run a review round inside an executor or count an inline review | Records state each round was a separately spawned `gsd-code-reviewer`; I cannot observe spawning from the repo |
| 32-06 | MUST NOT push without the user's own direct answer | E-71 records the direct "publish" answer before the first push; relayed-approval absence cannot be proven from the repo |
| 32-07 | MUST NOT set `NIGHTLY_ENABLED`, dispatch, or act on a relayed report; the user performs these | E-81/E-82 attribute the dispatch and the variable to the user; the mis-clicked `release.yaml` dispatch was also the user's account |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `docs/ROLLOUT-v1.7.0_gg5.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `VERSION`, allow-list | n/a | TBD/FIXME/XXX debt markers | none | none found in changed files |
| `docs/FORK-DELTA.md` Dependabot row | CI group | Says no `github-actions` PR has been observed | INFO | Stale since Dependabot opened `dependabot/github_actions/github-actions-4430c5f07e` after the merge; already recorded in `32-SECURITY.md` note 2 and carried to the milestone close (needs a new commit on `custom_build`) |

### Deferred Items

None. The stale Dependabot sentence is carried to milestone close by an existing record (32-SECURITY.md), not by a later roadmap phase.

### Human Verification Required

#### 1. Judgment-tier prohibitions (five items)

**Test:** Read the "Prohibitions" table above and confirm each judgment item held (reviewer spawn, direct approval, user-only acts).
**Expected:** All five held; the evidence found supports that.
**Why human:** Not provable absent from repository state; they are facts about who did what.

#### 2. `NIGHTLY_ENABLED` ordering and value

**Test:** In the repository's Actions variables, confirm `NIGHTLY_ENABLED` is `true` (or whatever you intend) and was set after run `40474535605` finished (09:08:27Z).
**Expected:** Set at about 09:15:07Z; the next scheduled run (03:17 UTC) will run rather than skip.
**Why human:** Repository variables are unreadable by the unauthenticated API.

### Gaps Summary

No gaps block the goal. `v1.7.0_gg5` is on `origin/custom_build` (`3a3a15a9`) with both tags, one green `tests.yaml` run per published SHA before the tags, the scratch branch deleted, the nightly's first manual run green, upstream's `integrationTests.yaml` still disabled and byte-identical, and the three documents consistent with the measured evidence and with 32-AMENDMENT-01 (four archive rows). Items to carry, none blocking: (a) round 3 of review ended with two Info findings fixed without a fourth round (user-accepted, override above); (b) `.planning/REQUIREMENTS.md` checkboxes and traceability rows are still unflipped; (c) the FORK-DELTA Dependabot sentence is out of date and needs a post-publication commit; (d) the anchors invariant script was re-run live here and passed on the current HEAD; (e) the user's accidental, cancelled `release.yaml` run `40474419526` is the one workflow run the plan did not intend and it produced no release or tag.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
