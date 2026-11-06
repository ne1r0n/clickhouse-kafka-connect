# Phase 49: Approval bundle for the user-gated publication of v1.7.0_gg8

The orchestrator presents this bundle verbatim and waits for the user's own direct answer. An approval relayed by another agent, a task notification or a summary does not count.

## Status

- Local-complete. Nothing below was pushed, merged, tagged, dispatched or deleted. Origin `custom_build` reads `9483476322e0d331e81c5b92ac266a18f0f01450` (the `v1.7.0_gg7` release merge); no `sync/v1.7.0` head and no `v1.7.0_gg8` tag exist on origin; the local tag `pre-v1.7.0_gg8` exists and points at `94834763` (the retag of your answer U-1; it is not on origin).
- Executor boundary: no executor runs any step after your answer; the orchestrator does, and only on your own direct word for that exact step, or you do from the handoff files.

Valid only for gated head d9b10b9bf670b8236119c725e3639c53ad3e570b.

Freeze rule: any change outside `.planning` (and under `.planning/scripts`) after that head, including a fix from a later review round, voids this bundle and every approval given for it until plans 49-03, 49-04 Tasks 1 to 3 and 49-05 Task 1 are re-run on the new head; the single exception is the one-line date commit of U-3 (line 1 of `CHANGELOG.md` only, made by the orchestrator on the push day and checked narrowly).

The figures were measured by command at the tip `3699a9cd617cdb01950bff86603e60eb8ae75725` (evidence entries E-11 to E-14 in `49-COMMAND-EVIDENCE.md`). The tip equals the gated head outside `.planning` and under `.planning/scripts`; the last commit outside `.planning` is `315f5de1c85fb61bd2a1e887d2be085bc62571ce`. Counts of commits grow with every later commit under `.planning`; the pre-checks of the handoff files print the live ones.

## Gates and what each word authorises

| Gate | Who | What it is | What it authorises |
|------|-----|------------|--------------------|
| G0 | the executor, plan 49-01 | pre-flight: origin, pull requests, upstream and scratch tips read, helpers checked (E-01) | starting the phase; passed |
| G1 | you, through the orchestrator | the review-round decisions: after rounds 3, 4 and 5 you chose "authorise round 4", "authorise round 5" and "accept as recorded" (E-09) | ending the review loop; passed |
| G2 | the orchestrator | the gate check after plan 49-04: both suites green, tail gate `result=PASS`, E-13 `gate_decision: go` | showing you this bundle; passed |
| G3 | you: your direct "go" | the approval of questions (1) to (10) for THIS head | U1 only: the push of `X0` to the throwaway branch `sync/v1.7.0` |
| G3b | you: your own word for the dry run | the `release.yaml` dry run dispatched from `sync/v1.7.0` | U2 only: the dispatch of `release.yaml` from `sync/v1.7.0` with `dry_run` ticked |
| G4 | you: your own "publish" after a green U2 | the go to build the merge commit `M'` | the orchestrator's build of `M'` (detached, never on the local `custom_build`) and its push U3 only |
| G5 | you: after `M'`'s single green `tests.yaml` run | the go to publish | U4 only: the publish dispatch of `release.yaml` from `custom_build` |
| G6a | you: your own word after the publish run was read back | the rollback tag | U5a only: the push of the tag `pre-v1.7.0_gg8` by name |
| G6b | you: your own word, answer to question (11) | the deletion of the throwaway branch `sync/v1.7.0` on origin | U5b only: `git push origin --delete refs/heads/sync/v1.7.0` |
| G7 | you: the separate RLS-06 confirmation, in your own words, after the publish | the deletion of the two `scratch/clt-07-*` branches | U6 only: one push deleting both by name |

One origin step per word (U-4): a word authorises the step in its row and nothing after it, and you may run any step yourself from its handoff file instead. The date commit of U-3 is not a gate: you settled it, and answer (2) only names the push day.

## Questions for the user

Eleven questions need an answer from you. (1) and (2) are about who and when, (3) and (4) are the two words that start the CI side, (5) to (10) are judgment items that you may overrule, and (11) is the deletion of the throwaway branch.

(1) Who runs the origin writes? The arrangement of U-4: the orchestrator runs each origin write only after your direct word for that exact step (G3 U1, G3b U2, G4 U3, G5 U4, G6a U5a, G6b U5b, G7 U6), or you name the steps you run yourself from the handoff files. A word for one step is not a word for the next.

(2) The push day. Which UTC date should the merge carry? On that day, before U1, the orchestrator makes the one-line date commit of U-3: line 1 of `CHANGELOG.md` changes from `# 1.7.0_gg8, Unreleased` to `# 1.7.0_gg8, <that date>` and nothing else (checked: numstat `1 1 CHANGELOG.md`, the rest of the file byte-identical, no planning identifier). UTC today reads `2026-11-06` (read at the rehearsal, E-14); if U3 later slips to another UTC day, a second date commit and a new scratch push are needed (contingency C-7).

(3) Go for U1, for gated head `d9b10b9bf670b8236119c725e3639c53ad3e570b`? What U1 publishes: the pushed tip `X0` is the tip after the date commit; at the rehearsal tip `3699a9cd617cdb01950bff86603e60eb8ae75725` the count `unpublished_commits` is 292 (commits on no origin ref; equal to the commits since `94834763`, because both `scratch/clt-07-*` tips are ancestors of it); outside `.planning` 42 files changed, 5948 insertions, 130 deletions against `94834763` (6 `src/main` files with 428 insertions and 72 deletions, 28 `src/test`, 4 documents, `VERSION`, `CHANGELOG.md`, `build.gradle.kts`, `gradle/libs.versions.toml`); inside `.planning` 253 files changed, 28568 insertions, 311 deletions (the `.planning` history is published too, the tree-neutral merge of `94834763` into the branch included: `77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f`). The left count `git rev-list --left-right --count origin/custom_build...HEAD` reads `0 292`, so a `--no-ff` merge adds the whole tail and loses nothing. Deleting the branch later does not unpublish any commit. The `.planning/config.json` and `.planning/state.json` edits are uncommitted and ride in no push. The changed paths outside `.planning` (`git diff --name-status 94834763 HEAD -- . ':(exclude).planning'`):

```text
M	CHANGELOG.md
M	VERSION
M	build.gradle.kts
M	docs/FORK-DELTA.md
M	docs/ROLLOUT-v1.7.0_gg1.md
M	docs/ROLLOUT-v1.7.0_gg6.md
A	docs/ROLLOUT-v1.7.0_gg8.md
M	gradle/libs.versions.toml
M	src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
M	src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
M	src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
M	src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
M	src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
M	src/main/java/com/clickhouse/kafka/connect/util/Utils.java
(28 files under src/test: 19 added, 9 modified; `git diff --name-status` prints them)
```

(4) The release dry run U2, its own word G3b. It dispatches `release.yaml` from the branch `sync/v1.7.0` as a dry run (the run is a dry run whatever the input says, because the ref is not `custom_build`; its name must read `Release sync/v1.7.0 (dry run)`). It is kept by U-2. It builds the archive twice, compares it with the one rebuilt from `v1.7.0_gg7` under the gg8 allow-list, and creates and deletes a throwaway draft release; it creates no tag. Do you want it, and do you give the word separately after U1's `tests.yaml` run is green? A defect that shows before the compare step on the runner is a CI-only finding that returns to its phase as a gap, because this phase edits no `.github/` file.

(5) The JVM record of the two full suites (E-13 `suites_java`): both ran on the default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1); `-PjavaVersion=21` was not needed; no JVM crash and no crash file (`suite1_jvm_crash: none`, `suite2_jvm_crash: none`, `crash_files_left: none`). Is that enough, or do you want another JVM record? A re-run is local and nothing is pushed.

(6) The review's end state: five rounds, each by a fresh `gsd-code-reviewer` spawned by the orchestrator, with 15, 6, 5, 4 and 2 findings (32 in all: 21 warnings, 11 info, 0 critical; `49-REVIEW.md`, `49-REVIEW-DISPOSITION.md`). All 32 are fixed, 0 are deferred, 0 rejected, 0 open. The cap of three rounds was extended by your answers (`round3_decision` in E-09 and `round4_decision` and `round5_decision` in `49-REVIEW-DISPOSITION.md`: authorise round 4, authorise round 5, accept as recorded). The two fixes of round 5 (the findings WR-20 and WR-21, commit `315f5de1`) were never read by a fresh reviewer, because you declined a sixth round; every earlier fix was reviewed by a later round. Do you stay with that? An answer of no re-opens plan 49-03 for a sixth round and every later gate on the head it produces, with nothing pushed.

(7) `integrationTest` was not re-run in this phase (PL-11): it needs a Connect stack. Measured against `v1.7.0_gg7`: no file under `src/integrationTest` and no file under `.github` changed, six `src/main` files changed (the full suites exercise them), and `build.gradle.kts` and `gradle/libs.versions.toml` changed by 2 insertions in all (a test dependency that arrives with upstream's tail). The last integration run of record belongs to an earlier release. Do you overrule? An overrule re-opens plan 49-04 with nothing pushed, and the run takes about half an hour.

(8) Do not merge any pull request on origin, Dependabot's included, before `M'` lands (contingency C-1). The user merges them on origin directly and each merge moves `custom_build`. `origin_open_prs` reads none at the rehearsal (E-14). No agent comments on or merges any pull request.

(9) The upstream state (upstream recheck 3): the newest upstream tag is `v1.7.0`, no tag past it; upstream `main` reads `83808df9a0993cf49b2ceaa4633c782e550c0180`; pull requests 861, 855 and 854 are open and 788 is closed unmerged, all unchanged since the first read (`upstream_changed_since_e12`: no). If a tag past `v1.7.0` appears before U3, the orchestrator puts it to you: publish gg8 first, or sync first. Anything to add?

(10) The two scratch branches `scratch/clt-07-client-0.11.0` (`de8c685b`) and `scratch/clt-07-prebump-0.9.6` (`939e8992`): both are still on origin at those tips and both are ancestors of the tip and of `94834763` (`scratch_tips_ancestors: yes`), so deleting them loses no commit. This is information now. The deletion is U6 and waits for a separate confirmation in your own words after the publish (RLS-06); nothing in any earlier word covers it.

(11) The throwaway branch `sync/v1.7.0` on origin, its own word G6b. After the publish is read back and the rollback tag is pushed, do you want it deleted (`git push origin --delete refs/heads/sync/v1.7.0`, U5b)? Deleting it unpublishes nothing: its commits are reachable through `custom_build`. You can keep it instead; the ROADMAP recipe deletes it after the publish run, and the orchestrator asks again at that point.

## The evidence behind the questions

- Allow-list (E-11): the gg8 list has 17 exact rows and no wildcard row, generated last from clean rebuilds of `v1.7.0_gg7` and of the tip. The compare with an empty list reads 17 `UNALLOWED` (14 changed entries, 3 only in the tip archive), totals `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`; the compare with the list exits 0, `RESULT: PASS`, `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg7, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`. Three mutations turned it red (a stale row exit 1, a deleted row exit 1, a leading wildcard exit 2). Archive sizes of the local rebuilds: 13564144 and 13572233 bytes (`final_tip_bytes`); a runner build differs by some bytes, the compare decides. The earlier figures in the context notes (14 rows, `changed 11 same 7501`) were the Phase 48 measurement and are superseded.
- Docker-free gate (E-12, gated head `d9b10b9b`): the tail gate ran once, alone, on the explicit head: `invariants: provenance_paths=11 anchor_commits=270 anchor_rows=218 marker_commits=288 anchor_exempt=0 result=PASS`, exit 0. `testUnit` with Docker unreachable: 2325 tests, 0 failures, 0 errors, 2 skipped, per-testcase delta against the earlier base `match=yes`. Anchors mirror `rows=218 bad=0`. `docker_free_decision: go`. One later commit, `89f3e5ce`, rewords ten lines of the review record (a file under `.planning/phases`), so the results stay valid.
- Two full suites (E-13): 2928 tests each, 0 failures, 0 errors, 18 skipped (`named=7 state_mismatch_cases=11 unexpected=0`), 3m 18s and 3m 15s, client V2, equal test by test (`delta: removed=0 added=0 match=yes`), `SELECT version()` `26.10.3.7` on the image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (one digest at all three reads). Today's read at the rehearsal (E-14): the same version and the same digest. `gate_decision: go`, `gate_failures: none`.
- The dry-run merge D (E-14), built in a throwaway detached worktree on `94834763` with the real merge message file and the frozen script `49-rehearse.sh`, in both modes (git attributes off, and plain): never pushed, removed afterwards. Parents `9483476322e0d331e81c5b92ac266a18f0f01450` and `3699a9cd617cdb01950bff86603e60eb8ae75725`, the tip's tree, subject `Merge sync/v1.7.0: release v1.7.0_gg8`, no `.gsd-ws-arg`, `VERSION` `v1.7.0_gg8`, the allow-list present, the baseline selection on D names `v1.7.0_gg7`. The note's rollback block, extracted from `docs/ROLLOUT-v1.7.0_gg8.md` and run unchanged on D with `bash -e`: exit 0, prints `ok`, `VERSION` `v1.7.0_gg7`, the note absent, the tree equal to tag `pre-v1.7.0_gg8`; a revert restores D's tree; block hash `2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991`, equal to the block rehearsed in E-05 and E-10. Both runs ended `rehearsal: PASS`. The line-ending trap on `gradlew.bat` did not occur (`gradlew_bat_trap: no`).
- Operator documents: no `TRUNCATE`, `DROP TABLE`, `DROP DATABASE` or `DELETE FROM` in the note, the CHANGELOG section or the added FORK-DELTA lines, and no instruction to empty a table.
- Upstream recheck 3 (this rehearsal, against E-01 and E-12): newest tag `v1.7.0`; `main` `83808df9`; pull requests 861 (`b62e0ae4`), 855 (`ddabf28b`), 854 (`69d1efc5`) open and 788 (`bb4b4140`) closed unmerged, all unchanged. They touch files this release also changes: information for the next sync.
- What each step makes permanent: U1 publishes every commit since `94834763`, the `.planning` history included; deleting the branch later does not unpublish them. U3 puts the whole `.planning` tree on the default branch. U4 creates a GitHub release and a tag that only you can delete. U5a pushes a tag. U5b and U6 delete branches. The rollback path is the note's: forward only, a new commit that restores the tree of `pre-v1.7.0_gg8`, never a force-push.
- What the release contains and does not: six `src/main` files (428 insertions, 72 deletions against `v1.7.0_gg7`), the tests and documents listed under question (3), the allow-list and the anchors under `.planning/scripts`. It changes no workflow file; the scratch dry run is the first runner exercise of the compare chain with the gg8 list, and a defect that only the runner shows returns to its phase as a gap.

The exact commands, one per word (pre-checks, expected results and reporting are in the handoff files):

```sh
# DATE (orchestrator, no gate; on the push day of answer (2), before U1): edit line 1 of CHANGELOG.md, commit that file alone
# G3 -> U1, from 49-USER-DISPATCH-1-SCRATCH-DRYRUN.md
git push origin "${X0}:refs/heads/sync/v1.7.0"
# G3b -> U2, same file
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true
# G4 -> build M' (orchestrator, detached, local only) and U3, from 49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
git push origin "${M}:refs/heads/custom_build"
# G5 -> U4, from 49-USER-DISPATCH-3-PUBLISH.md (dry_run NOT ticked)
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref custom_build
# G6a -> U5a, from 49-USER-DISPATCH-4-PRETAG-CLEANUP.md
git push origin refs/tags/pre-v1.7.0_gg8
# G6b -> U5b, same file
git push origin --delete refs/heads/sync/v1.7.0
# G7 -> U6, from 49-USER-DISPATCH-5-SCRATCH-BRANCHES.md
git push origin --delete refs/heads/scratch/clt-07-client-0.11.0 refs/heads/scratch/clt-07-prebump-0.9.6
```

No handoff pushes the tag `v1.7.0_gg8` (the publish run creates it) and none uses a force, tags, all or mirror option.

## What happens after each word

- Your answers (G3 and questions (1) to (11)): the orchestrator re-reads origin, upstream and the scratch tips, records your words verbatim (who received them), makes the DATE commit on the push day you name, then runs U1 on your G3 and waits for exactly one green `tests.yaml` run on the pushed SHA. A red run or two runs is a stop.
- G3b: U2, the dry run from `sync/v1.7.0`: green means baseline `v1.7.0_gg7`, `exit 0, 0 unallowed, 0 stale rows`, `RESULT: PASS`, the publish step skipped, and no draft and no tag left. A green dry run does not prove the publish step or the tests gate on `M'`.
- G4, your own "publish" after a green U2: the orchestrator re-checks origin (still `94834763`), the CHANGELOG date against UTC today, upstream and `X0` against the gated tree, builds `M'` detached on `94834763` with `49-MERGE-MESSAGE.txt`, rehearses the rollback on `M'`, tells you `X0` and `M'`, and pushes `M'` to `custom_build` without force. Exactly one green `tests.yaml` run on `M'`, still one after 120 seconds, and no release run, or it stops.
- G5, after that run: U4, the publish dispatch from `custom_build`; the run creates the release and the tag `v1.7.0_gg8`. The orchestrator then reads the release, the tag, the asset and the run back with read-only calls and writes the release record.
- G6a: U5a pushes `pre-v1.7.0_gg8` by name, after `M'`'s `tests.yaml` run finished. G6b: U5b deletes `sync/v1.7.0`. G7, only after your separate confirmation: U6 deletes both `scratch/clt-07-*` branches in one push, and `git ls-remote origin` must list neither.
- Stop conditions before any origin step, after which the orchestrator asks you again: a red or not-exactly-one `tests.yaml` run, a moved `custom_build` (C-1), a changed UTC date against the CHANGELOG heading (C-7), an existing `v1.7.0_gg8` tag (C-8), a moved upstream (a tag past `v1.7.0` is put to you), a moved scratch tip (C-9), any change outside `.planning` after the gated head other than the date line (the Freeze rule).
- An answer of no to question (6), or a request under (5) or (7), re-opens the matching plan and voids this bundle; nothing is pushed in the meantime.

Ready for the user's direct approval.
