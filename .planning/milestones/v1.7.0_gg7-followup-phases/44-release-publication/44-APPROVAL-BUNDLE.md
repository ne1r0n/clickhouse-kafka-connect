# Phase 44: Approval bundle for the user-run publication of v1.7.0_gg7

The orchestrator presents this bundle verbatim and waits for the user's own direct answer. An approval relayed by another agent, a task notification or a summary does not count.

## Status

- Local-complete. Nothing below was pushed, merged, tagged or dispatched. Origin `custom_build` reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` (the `v1.7.0_gg6` release commit); no `sync/v1.7.0` head and no `v1.7.0_gg7` tag exist on origin; the local tag `pre-v1.7.0_gg7` exists and points at `72e8ccff`.
- Executor boundary: no executor runs any step after your answer; the orchestrator and you do, and only on your own direct word.
- This bundle was rewritten after the re-open you asked for at the first approval (measure the F-2 claim, re-run the integration test). It replaces the bundle for the older gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`; the go you gave for that head is void under the Freeze rule and is asked again in question (2).

Valid only for gated head 06a9980a7f7271efa1bae4c8b88ad149e51f50ee.

Freeze rule: any change outside `.planning` (and under `.planning/scripts`, other than the allow-list) after that head, including a fix from a later review round and a CHANGELOG date edit, voids this bundle and every approval given for it, until plans 44-03, 44-04 Tasks 1 to 3 and 44-05 Task 1 are re-run on the new head.

The figures were measured by command at the tip `16ffde588434c36085d80c124e6036a9cf478043` (evidence entries E-21 to E-27 in `44-COMMAND-EVIDENCE.md`). The tip equals the gated head outside `.planning` and under `.planning/scripts`; the last commit outside `.planning` is `5b25c18ce6137892383899f9f10663a22f498fe5`. Counts of commits grow with every later commit under `.planning`; the pre-checks of the handoff files print the live ones.

What changed since the bundle you answered (the old gated head to this one): three documents, `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg7.md`, which now state the F-2 progress-header claim as measured (E-21), and one more review round (round 5) over that change. No Java, build, workflow or script line changed, and the allow-list rows did not change (E-23).

## Your recorded answers (G3, given to the orchestrator through its question tool, verbatim)

- "Go for U1 and U2": given for the OLD gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`. It is void and is asked again in question (2).
- Who runs the origin writes: "You run them (Recommended)". Kept; it is an arrangement, not a verdict on a head. Say so if you want to change it (question (1)).
- CHANGELOG heading date: "Keep the date (Recommended)". Kept (question (5)).
- Extras before the release: "Re-run integrationTest first": done, 55 passed (E-22; question (7)). "Measure the T6 progress-header claim first": done (E-21; question (8)).

## Gates and what each approval authorises

| Gate | Who | What it is | What it authorises |
|------|-----|------------|--------------------|
| G0 | the executor, plan 44-01 | pre-flight: origin, pull requests and upstream re-read, helpers checked (E-01) | starting the phase; passed |
| G1 | you, through the orchestrator | the review-round decisions: after rounds 3 and 4 (E-09) "Fix both, then one more round", then "Fix both, accept without round 5"; round 5 (the F-2 change) was run at your request and its five fixes are not re-reviewed: question (4) | ending the review loop; given for rounds 1 to 4, open for round 5 |
| G2 | the orchestrator | the gate check after the plan 44-04 re-run on the new head: both suites green, invariant script `result=PASS`, E-26 `docker_free_decision: go` and `suites_decision: go` | showing you this bundle; passed |
| G3 | you: your direct "go" | the approval of questions (1) to (8) for THIS head | U1 (push the tip to the scratch branch `sync/v1.7.0`) and U2 (the release dry run from it) only |
| G4 | you: your own "publish" message after a green U2 | the go to build the merge commit M | the orchestrator's build of M (detached, never on the local `custom_build`) and its push U3 only |
| G5 | you: after M's single green `tests.yaml` run | the go to publish | U4 (the publish dispatch, always yours) and then U5 (rollback tag, scratch branch deletion) |

## Questions for the user

Two questions need a new answer from you: (2), the go for this head, and (4), whether round 5's fixes are accepted without a round 6 (yes or no). The others restate an answer you gave, or a measurement you asked for, and need a word only if you disagree.

(1) Who runs the origin writes? You answered "You run them (Recommended)": you run every push, the dispatches, the pre-tag push and the scratch-branch delete from the four handoff files; the orchestrator only reads GitHub (GET calls). Alternative, if you change your mind: the orchestrator runs the pushes on your direct word for each step; the publish dispatch U4 is yours either way.

(2) Go for U1 and U2, for gated head `06a9980a7f7271efa1bae4c8b88ad149e51f50ee`? (Your earlier go was for the older head and is void.) What U1 publishes: the tip is `16ffde588434c36085d80c124e6036a9cf478043`; `ahead_of_gg6` 278 commits, of which `unpublished_commits` 276 are on no origin ref yet (the other 2 are on your `scratch/clt-07-*` branches); outside `.planning` 40 files changed, 3169 insertions, 146 deletions against `72e8ccff`; inside `.planning` 311 files changed, 31735 insertions, 1428 deletions (the `.planning` history is published too); left count `git rev-list --count sync/v1.7.0..origin/custom_build` is 0, so a `--no-ff` merge adds the whole tail and nothing is lost; the tip equals the gated head outside `.planning` and under `.planning/scripts` (last commit outside `.planning` `5b25c18ce6137892383899f9f10663a22f498fe5`, allow-list commit `402b6e2ea99a65e80c42f710e2b93277c6f78c64`). Deleting the scratch branch later does not unpublish the commits. The changed files outside `.planning` are the same 40 paths as in the bundle you first saw (`git diff --name-status 72e8ccff HEAD -- . ':(exclude).planning'`; 8 `src/main`, 18 `src/test`, 2 `src/integrationTest`, 5 workflows, 1 gradle catalog, 6 documents):

```text
M .github/workflows/increment-version.yml
M .github/workflows/nightly-integration-tests.yaml
M .github/workflows/pre-release.yaml
M .github/workflows/release.yaml
M .github/workflows/tests.yaml
M CHANGELOG.md
M VERSION
M docs/FORK-DELTA.md
M docs/ROLLOUT-v1.7.0_gg1.md
M docs/ROLLOUT-v1.7.0_gg6.md
A docs/ROLLOUT-v1.7.0_gg7.md
M gradle/libs.versions.toml
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
M src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
M src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
M src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
M src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
M src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
M src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java
M src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
M src/main/java/com/clickhouse/kafka/connect/util/Utils.java
M src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
M src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/HandleExceptionCauseCycleUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java
```

(3) The JVM record of the two full suites (E-25 `suites_jdk`): they ran with `-PjavaVersion=21` (a JDK 21 test JVM), NOT on the default JDK 17 line that the first approval's suites used. The cause, recorded in E-22 and E-25: the default OpenJ9 17 test JVM could not load class version 65 on this machine in the re-run (the first attempt of the integration test failed that way in 16 s). No JVM crash and no crash file (`suite1_jvm_crash: none`, `suite2_jvm_crash: none`, `crash_files_left: none`). `testUnit` with Docker unreachable also ran with `-PjavaVersion=21`. The flag selects the test JVM only; the archive is built by the same toolchain as before (E-23). Is a JDK 21 test run enough for you, or do you want a JDK 17 record as well? Say so if you want it; it is a local re-run of the suites and nothing is pushed.

(4) The review's end state: five rounds, each by a fresh reviewer (14, 2, 2, 2 and 5 findings; 25 in all, 8 warnings, 17 info, 0 critical; `44-REVIEW.md`, `44-REVIEW-DISPOSITION.md`). 24 are fixed, 1 is deferred, 0 rejected, 0 open. Rounds 3 and 4 ended on your answers "Fix both, then one more round" and "Fix both, accept without round 5" (`round3_decision`, E-09), so the fixes of round 4 were not read by a fresh reviewer, and you accepted that. Round 5 (agent `a87ce913aac02a00b`) reviewed the one commit that restated the F-2 claim as measured and found WR-07, WR-08, IN-15, IN-16 and IN-17; all five are fixed in `5b25c18c` and NOT re-read by a fresh reviewer. A round 6 needs your direct word. Do you accept the round 5 fixes without a round 6, yes or no? An answer of no re-opens plan 44-03 for a round 6 and every later gate on the head it produces, with nothing pushed. Deferred: IN-03, the planning identifier "Phase 39" that Phase 43 added to the published gg6 note (owner: you as fork maintainer; re-open trigger: the next edit of the published gg6 note). The findings:
- WR-01: Row T5 attributes 4000 committed rows to one failed insert; the evidence counts both runs (fixed)
- WR-02: Rows R4 and R1 are labelled `measured` although part of each claim is read from the code or derived (fixed)
- WR-03: R1, section C and the CHANGELOG omit the configuration where the R1 effect does not occur (fixed)
- WR-04: The headline "every helper client ... is now closed" is false by the document's own residual row (fixed)
- WR-05: FORK-DELTA writer row still says the writer "releases each one" of "every helper client" (fixed)
- WR-06: FORK-DELTA writer row says two residuals stay open, but a third sits on the writer's own factory (fixed)
- WR-07 (round 5): Row T6 and Known cost 14 state the escape for "the default settings", but the probe ran with settings that differ from what the connector sends (fixed, not re-reviewed)
- WR-08 (round 5): Row T6 has six cells in a five-column table, so a sentence is dropped when the note is rendered (fixed, not re-reviewed)
- IN-15 to IN-17 (round 5): the checker and the records still required the old label; Known cost 14's title left out the escape; three probe details were missing from the note (all fixed, not re-reviewed)
- IN-01 to IN-02, IN-04 to IN-14: wording, label and scope fixes of the note, the CHANGELOG and FORK-DELTA (fixed)
- IN-03: the deferred item above

(5) The CHANGELOG heading date. The heading reads `# 1.7.0_gg7, 2026-11-02`; UTC today is `2026-11-03` (read at 2026-11-03T12:43Z in the rehearsal, E-27), so the dates differ. You answered "Keep the date (Recommended)": the heading date stays, read as the date the release documents were frozen (E-10, 2026-11-02); no edit, no re-run; the release record states it as a deviation, and the U3 pre-check on the date prints a difference that you have accepted. Nothing reads the heading: no workflow, script or test refers to `CHANGELOG.md`. The alternative, a date-fix, edits `CHANGELOG.md` after the gates: the Freeze rule applies in full (plan 44-03 re-opens with a fresh reviewer round, plan 44-04 Tasks 1 to 3 re-run, this plan's Task 1 re-runs, about 2.5 hours, every approval for the older head void) and the date can go stale again if the merge slips. It needs a word from you only if you want to change your answer.

(6) Do not merge the pull requests #21 and #22 on origin before M lands. GitHub normally marks both merged once M lands because their heads (`14be67ce`, `b5cccbd3`) are ancestors of the tip (information; an assumption until read after U5). They are unchanged at this rehearsal. No agent comments on or merges any of them.

(7) `integrationTest`, re-run as you asked (E-22): `ClickHouseSinkConnectorIntegrationTest`, 55 passed, 0 failed, 0 skipped, 0 errors, `BUILD SUCCESSFUL in 30m 52s`, on head `14f4567e` (a docs-only change since the last `src` change; `src` has not changed since). It ran with `-PjavaVersion=21` (the first attempt with the default test JVM failed in 16 s: environment, not a test result). It is the same scope as the Phase 39 run of record: the whole `integrationTest` task (cloud jobs, other classes) was not run, and the upstream nightly workflow stays red by inheritance and is unrelated. Is that enough? An overrule re-opens plan 44-04 with nothing pushed.

(8) The F-2 progress-header claim (row T6 of the note), measured as you asked (E-21), on `26.10.3.7` (image digest `sha256:739e099a…`) with the connector's client V2 helper client: a body insert of 4000 rows into a table with a slow materialized view, `wait_end_of_query=1`, `async_insert=0`. With a 3000 ms `socket_timeout` and `send_progress_in_http_headers=1` (the connector's default insert setting) the insert ran 8141 ms and succeeded: it escapes the bound. The same insert without the progress setting failed at 3007 ms with a socket timeout; the control without a bound took 8106 ms; curl counted 8 progress headers over about 8 s. The note's row T6, Known cost 14 and the CHANGELOG now say so, labelled `measured` (the mechanism, a read timeout that restarts when a byte arrives, is read from the code); the case with the server's default `async_insert` was rejected by the server in the probe and is stated as not measured. No connector code was changed: the release documents the limit and does not fix it. One loose end for you: the merge message for M (`44-MERGE-MESSAGE.txt`; at your answer in E-28 one sentence about the escape was added, commit `43f6c9da7c2d4c9f8cf5d4fac44c112594fd9c7c`) says an insert "is bounded" and that "a quiet insert that runs longer fails"; that is accurate as worded but does not mention the escape, and the message becomes permanent on `custom_build`. A sentence about it can be added without the Freeze rule (the file is under `.planning`) if you want it; say so, or leave it.

## The evidence behind the questions

- Allow-list (E-23, re-run on the new head; E-11 is the older head's record): archives rebuilt from clean exports of `v1.7.0_gg6` and of the tip read the same figures as before, 120 differences (102 gson entries, 3 gson metadata entries, 14 connector classes, the manifest, nothing else); the 18 derived rows equal the committed list as a set, so the list was not rewritten (last commit `402b6e2e`); `OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0`; the compare against it exits 0, `RESULT: PASS`, summary `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`; three mutations turned it red (a stale row: exit 1; a deleted gson row: exit 1 with 102 `UNALLOWED` lines; a leading-wildcard row: exit 2). Archive sizes 13558634 (baseline) and 13564145 bytes (tip, `final_tip_bytes`), 7933 and 7932 inner entries; the same as E-11. A runner build differs by some bytes anyway; the compare decides.
- Docker-free gate (E-24, hygiene decision E-26): `testUnit` with Docker unreachable: 1826 tests, 0 failures, 0 errors, 2 skipped, no JVM crash, `BUILD SUCCESSFUL in 17s`; anchors mirror `rows=190 bad=0`. E-24 keeps its verdict `fail` by design (one hygiene hit in a handoff line of the orchestrator's re-open note, line 32); E-26 re-ran the scans after the neutral rewording (0 hits in 25 files, default and strict) and carries `docker_free_decision: go`.
- Two full suites (E-25): 2425 tests each, 0 failures, 0 errors, 18 skipped (`named=7 state_mismatch_cases=11 unexpected=0`), 2m 47s and 2m 38s, client V2, `SELECT version()` 26.10.3.7 on the image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (one digest at all three reads); today's read in this bundle's rehearsal (E-27): the same version and the same digest. E-25 reads `gate_decision: no-go` only for the hygiene hit; E-26 supersedes it with `suites_decision: go`.
- Invariant script, once on `06a9980a` (started on the gated head, run alone): `invariants: provenance_paths=21 anchor_commits=1556 anchor_rows=190 marker_commits=1622 anchor_exempt=19 result=PASS`, exit 0 (E-24, E-25).
- The dry-run merge D (E-27), built in a throwaway detached worktree on `72e8ccff` with the real merge message file and never pushed: commit `ff4a84e70281a97951766268afdd6c0830fc0181`, parents `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and `16ffde588434c36085d80c124e6036a9cf478043`, the tip's tree, subject `Merge sync/v1.7.0: release v1.7.0_gg7`, no `.gsd-ws-arg`, `VERSION` `v1.7.0_gg7`, the allow-list present, the baseline selection on D names `v1.7.0_gg6`, no `v1.7.0_gg7` tag merged. The merge runs with git's attributes switched off (`git --attr-source=<empty tree>`) and so does the build of M; the line-ending trap on `gradlew.bat` did not occur on this base (`d_gradlew_bat_trap: no`, equal to E-05).
- Operator documents: no `TRUNCATE`, `DROP TABLE`, `DROP DATABASE` or `DELETE FROM` in the note, the CHANGELOG section or the added FORK-DELTA lines, and no instruction to empty a table.
- The rollback block of the note, extracted from the changed note and run unchanged on D with `bash -e`: exit 0, prints `ok`, `VERSION` `v1.7.0_gg6`, the note absent, the tree equal to tag `pre-v1.7.0_gg7`; a revert restores D's tree; the block is byte-equal to the one of the first rehearsal (E-14).
- Upstream recheck 3 (this rehearsal) against E-14 and E-01: the newest upstream tag is still `v1.7.0`, no tag past it; pull requests #861 (`3d4cfdf2`), #855 (`ddabf28b`) and #857 (`11d88bc0`) are still open and unchanged. Upstream `main` still reads `d92a1830`, unchanged since E-14 (it moved from `9bed6f79` before that: six commits touching only `.github/workflows/tests.yaml` and `gradle/libs.versions.toml`). That is information for the next upstream sync; this release is built on `9bed6f79` as its CHANGELOG states. If you want it absorbed first, say so: that is a new sync phase and nothing here is pushed.
- What each step makes permanent: U1 publishes every commit since `72e8ccff`, the `.planning` history included; deleting the branch later does not unpublish them. U3 puts the whole `.planning` tree on the default branch. U4 creates a GitHub release and a tag that only you can delete. `pre-v1.7.0_gg7` is a tag; the rollback path is the note's: forward only, a new commit that restores the tree of `pre-v1.7.0_gg7`, never a force-push.
- What the release contains and does not: it contains the eight `src/main` files above (194 insertions and 22 deletions against `v1.7.0_gg6`), the workflow, test and document changes outside `.planning` listed in question (2), and the allow-list and anchors under `.planning/scripts`. It does not change upstream's `integrationTests.yaml`; the scratch dry run is the first runner exercise of the `setup-gradle` 6.4.1 pin and of the compare chain with the gg7 list, and a defect that only the runner shows returns to its phase as a gap, because this phase may not edit `.github/`.
- The uncommitted `.planning/config.json` and `.planning/state.json` lines do not ride in any push.

## What happens after each answer

- Your "go" for this head (G3): U1 and U2 from `44-USER-DISPATCH-1-SCRATCH-DRYRUN.md`: you push the tip to the scratch branch `sync/v1.7.0` and wait for its one green `tests.yaml` run, then dispatch `release.yaml` from that branch as a dry run (the run is a dry run because the ref is not `custom_build`), and report.
- After a green dry run you send your own direct "publish" message (G4). The orchestrator re-checks origin, the date and upstream, builds M detached on `72e8ccff` with `44-MERGE-MESSAGE.txt`, and tells you `X0` (the SHA you pushed) and `M`. U3 (`44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`): you push M to `custom_build` by SHA, without force, and wait for its single green `tests.yaml` run.
- G5, then U4 (`44-USER-DISPATCH-3-PUBLISH.md`): you dispatch `release.yaml` from `custom_build` with `dry_run` not ticked; the run creates the release and the tag `v1.7.0_gg7`; you never push that tag. Then U5 (`44-USER-DISPATCH-4-PRETAG-CLEANUP.md`): you push `pre-v1.7.0_gg7` by name and delete the scratch branch.
- An answer of no to question (4), or a request under (3), (7) or (8), re-opens the matching plan and voids this bundle again; nothing is pushed in the meantime.
- Each step has a stop condition, and after any stop the orchestrator asks you again: a red or not-exactly-one `tests.yaml` run, a moved `custom_build` (contingency C-1), a changed UTC date against the CHANGELOG heading (C-7; already answered by question (5)), an existing `v1.7.0_gg7` tag (C-8), a moved upstream (a tag past `v1.7.0` is put to you: publish gg7 first, or sync first).

Ready for the user's direct approval.
