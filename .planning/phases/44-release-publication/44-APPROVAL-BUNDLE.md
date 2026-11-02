# Phase 44: Approval bundle for the user-run publication of v1.7.0_gg7

The orchestrator presents this bundle verbatim and waits for the user's own direct answer. An approval relayed by another agent, a task notification or a summary does not count.

## Status

- Local-complete. Nothing below was pushed, merged, tagged or dispatched. Origin `custom_build` reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` (the `v1.7.0_gg6` release commit); no `sync/v1.7.0` head and no `v1.7.0_gg7` tag exist on origin; the local tag `pre-v1.7.0_gg7` exists and points at `72e8ccff`.
- Executor boundary: no executor runs any step after your answer; the orchestrator and you do, and only on your own direct word.

Valid only for gated head b265f5a45a0752f314bb95315e3cdfbeaa2b78cd.

Freeze rule: any change outside `.planning` (and under `.planning/scripts`, other than the allow-list) after that head, including a fix from a later review round and a CHANGELOG date edit, voids this bundle and every approval given for it, until plans 44-03, 44-04 Tasks 1 to 3 and 44-05 Task 1 are re-run on the new head.

The figures were measured by command at the tip `45cbc9db5bba2e5f367eff4bb7043251587e44e2` (evidence entries E-10 to E-14 and E-20 in `44-COMMAND-EVIDENCE.md`). The tip equals the gated head outside `.planning` and under `.planning/scripts`. Counts of commits grow with every later commit under `.planning`; the pre-checks of the handoff files print the live ones.

## Gates and what each approval authorises

| Gate | Who | What it is | What it authorises |
|------|-----|------------|--------------------|
| G0 | the executor, plan 44-01 | pre-flight: origin, pull requests and upstream re-read, helpers checked (E-01) | starting the phase; passed |
| G1 | you, through the orchestrator | the review-round decision after rounds 3 and 4 (E-09): "Fix both, then one more round", then "Fix both, accept without round 5" | ending the review loop; given |
| G2 | the orchestrator | the gate check after plan 44-04: both suites green, invariant script `result=PASS`, E-13 `gate_decision: go` | showing you this bundle; passed |
| G3 | you: your direct "go" | the approval of questions (1) to (8) | U1 (push the tip to the scratch branch `sync/v1.7.0`) and U2 (the release dry run from it) only |
| G4 | you: your own "publish" message after a green U2 | the go to build the merge commit M | the orchestrator's build of M (detached, never on the local `custom_build`) and its push U3 only |
| G5 | you: after M's single green `tests.yaml` run | the go to publish | U4 (the publish dispatch, always yours) and then U5 (rollback tag, scratch branch deletion) |

## Questions for the user

(1) Who runs the origin writes? Default, as for gg6: you run every push, the dispatches, the pre-tag push and the scratch-branch delete from the four handoff files; the orchestrator only reads GitHub (GET calls). Alternative: the orchestrator runs the pushes on your direct word for each step; the publish dispatch U4 is yours either way.

(2) Go for U1 and U2? What U1 publishes: the tip is `45cbc9db5bba2e5f367eff4bb7043251587e44e2`; `ahead_of_gg6` 261 commits, of which `unpublished_commits` 259 are on no origin ref yet (the other 2 are on your `scratch/clt-07-*` branches); outside `.planning` 40 files changed, 3167 insertions, 146 deletions against `72e8ccff`; inside `.planning` 301 files changed, 30107 insertions, 1427 deletions (the `.planning` history is published too); left count `git rev-list --count sync/v1.7.0..origin/custom_build` is 0, so a `--no-ff` merge adds the whole tail and nothing is lost; the tip equals the gated head outside `.planning` and under `.planning/scripts` (last commit outside `.planning` `a43ff32f39647dfc3afb9849f6729b251f2619d2`, allow-list commit `402b6e2ea99a65e80c42f710e2b93277c6f78c64`). Deleting the scratch branch later does not unpublish the commits. The changed files outside `.planning` (`git diff --name-status 72e8ccff HEAD -- . ':(exclude).planning'`; 8 `src/main`, 18 `src/test`, 2 `src/integrationTest`, 5 workflows, 1 gradle catalog, 6 documents):

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

(3) The JVM record of the two full suites (E-13 `suites_jdk`): both ran on the default test JDK line (`defaultJdkVersion` 17, no `-PjavaVersion`; the installed JDK 17 entries are IBM Semeru 17.0.9), with no JVM crash and no crash file left (`suite1_jvm_crash: none`, `suite2_jvm_crash: none`, `crash_files_left: none`). `testUnit` with Docker unreachable also ran on JDK 17 with no crash. Nothing needs your judgment here; say so if a JDK 17 record is not enough for you.

(4) The review's end state: four rounds, each by a fresh reviewer (14, 2, 2 and 2 findings; 20 in all, 6 warnings, 14 info, 0 critical; `44-REVIEW.md`, `44-REVIEW-DISPOSITION.md`). 19 are fixed, 1 is deferred, 0 rejected, 0 open. The last round was not clean: after rounds 3 and 4 you chose "Fix both, then one more round" and then "Fix both, accept without round 5" (`round3_decision`, E-09), so the last fixes were not read by a fresh reviewer. Deferred: IN-03, the planning identifier "Phase 39" that Phase 43 added to the published gg6 note (owner: you as fork maintainer; re-open trigger: the next edit of the published gg6 note). The findings:
- WR-01: Row T5 attributes 4000 committed rows to one failed insert; the evidence counts both runs (fixed)
- WR-02: Rows R4 and R1 are labelled `measured` although part of each claim is read from the code or derived (fixed)
- WR-03: R1, section C and the CHANGELOG omit the configuration where the R1 effect does not occur (fixed)
- WR-04: The headline "every helper client ... is now closed" is false by the document's own residual row (fixed)
- WR-05: FORK-DELTA writer row still says the writer "releases each one" of "every helper client" (fixed)
- WR-06: FORK-DELTA writer row says two residuals stay open, but a third sits on the writer's own factory (fixed)
- IN-01 to IN-02, IN-04 to IN-14: wording, label and scope fixes of the note, the CHANGELOG and FORK-DELTA (fixed)
- IN-03: the deferred item above

(5) The CHANGELOG heading date. The heading reads `# 1.7.0_gg7, 2026-11-02` and must carry the merge date in UTC. At the measurement (2026-11-02T23:41Z) UTC today was `2026-11-02`, equal, but the UTC date flips at 2026-11-03T00:00Z, minutes after that measurement, so by the time you read this the dates differ, and no merge can now happen on 2026-11-02 UTC. Nothing reads the heading: no workflow, script or test refers to `CHANGELOG.md` (searched in `.github` and `src/test`); the tag and the release carry their own timestamps. The exact consequence of each choice:
- Keep: the heading date `2026-11-02` stays, read as the date the release documents were frozen (E-10, 2026-11-02). No edit, no re-run; the release record states it as a deviation. The U3 pre-check on the date then prints a difference that you have accepted. This is the recommendation: the date is documentary, and an edit would re-open everything below and could go stale again if the merge slips past the day it is set for.
- Date-fix: edit the heading to the UTC day of the merge. That is a change of `CHANGELOG.md` after the gates, so the Freeze rule applies: plan 44-03 re-opens (a fresh reviewer round with its own agent id, a disposition, a new frozen-documents entry); plan 44-04 Tasks 1 to 3 re-run on the new head (allow-list regenerated last, `testUnit` with Docker unreachable, both full suites, the invariant script on the new explicit SHA); plan 44-05 Task 1 re-runs (rehearsal on the new tip, this bundle rewritten with the new gated head). About 2.5 hours, and every approval given for the older head is void and asked again.
- Merging before the flip: no longer possible.
Which do you choose? It is asked again before U3 if you hold.

(6) Do not merge the pull requests #21 and #22 on origin before M lands. GitHub normally marks both merged once M lands because their heads (`14be67ce`, `b5cccbd3`) are ancestors of the tip (information; an assumption until read after U5). No agent comments on or merges any of them.

(7) `integrationTest` was not re-run (E-12 `integration_test`): it needs a Connect stack, and the recorded run is the one of the Phase 39 harness fix. `integration_sources_after_run` (E-12): `51200928 (fix(39): WR-01, the sink task status log line kept purely diagnostic in the integration test)`, a diagnostic log-line fix in the integration test after that run. Overrule? An overrule re-opens plan 44-04 with nothing pushed.

(8) The F-2 progress-header claim (row T6 of the note: whether an insert during which the server keeps sending progress headers can reset the per-insert timer and escape the bound; two probes on `26.10.3.7` showed the mechanism, no connector insert was observed escaping it) is carried as `derived (mechanism, not measured)`. Do you want it measured before the release?

## The evidence behind the questions

- Allow-list (E-11): 18 rows derived from the 120 differences of two archives rebuilt on the final tree (102 gson entries, 3 gson metadata entries, 14 connector classes, the manifest, nothing else); `OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0`; the compare against it exits 0, `RESULT: PASS`, summary `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`; three mutations turned it red (a stale row: exit 1; a deleted gson row: exit 1 with 102 `UNALLOWED` lines; a leading-wildcard row: exit 2). Archive sizes 13558634 (baseline `v1.7.0_gg6`) and 13564145 bytes (tip, `final_tip_bytes`), 7933 and 7932 inner entries. The list is valid for exactly this tree. A size note: row D4 of the note quotes 13558633 and 13564144 bytes (the build of 2026-11-02); a rebuild made on a later date is one byte larger because the archive's manifest holds the build date (E-12 `archive_size_delta_cause`); the entry counts match, and a runner build differs by some bytes anyway; the compare decides.
- Docker-free gate (E-12 and E-20): `testUnit` with Docker unreachable: 1826 tests, 0 failures, 0 errors, 2 skipped, no JVM crash; anchors mirror `rows=190 bad=0`. E-12 keeps its verdict `fail` by design (one hygiene scan hit three words in the review file); E-20 re-ran the scans after the orchestrator's neutral rewording `4c1f600b` (0 hits) and carries `docker_free_decision: go`.
- Two full suites (E-13): 2425 tests each, 0 failures, 0 errors, 18 skipped (`named=7 state_mismatch_cases=11 unexpected=0`), 2m 53s and 2m 45s, client V2, `SELECT version()` 26.10.3.7 on the image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`; today's read in this bundle's rehearsal (E-14): the same version and the same digest. `gate_decision: go`.
- Invariant script, once on `b265f5a4`: `invariants: provenance_paths=21 anchor_commits=1534 anchor_rows=190 marker_commits=1600 anchor_exempt=19 result=PASS`, exit 0.
- The dry-run merge D (E-14), built in a throwaway detached worktree on `72e8ccff` with the real merge message file and never pushed: parents `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and `45cbc9db5bba2e5f367eff4bb7043251587e44e2`, the tip's tree, subject `Merge sync/v1.7.0: release v1.7.0_gg7`, no `.gsd-ws-arg`, `VERSION` `v1.7.0_gg7`, the allow-list present, the baseline selection on D names `v1.7.0_gg6`, no `v1.7.0_gg7` tag merged. The merge runs with git's attributes switched off (`git --attr-source=<empty tree>`) and so does the build of M; the line-ending trap on `gradlew.bat` did not occur on this base (`d_gradlew_bat_trap: no`, equal to E-05).
- Operator documents: no `TRUNCATE`, `DROP TABLE`, `DROP DATABASE` or `DELETE FROM` in the note, the CHANGELOG section or the added FORK-DELTA lines, and no instruction to empty a table.
- The rollback block of the note, run unchanged on D with `bash -e`: exit 0, prints `ok`, `VERSION` `v1.7.0_gg6`, the note absent, the tree equal to tag `pre-v1.7.0_gg7`; a revert restores D's tree.
- Upstream recheck 2 (end of the phase) against the first one (E-01): the newest upstream tag is still `v1.7.0`, no tag past it; pull requests #861 (`3d4cfdf2`), #855 (`ddabf28b`) and #857 (`11d88bc0`) are still open and unchanged. Upstream `main` moved from `9bed6f79` to `d92a1830` since E-01: six commits, merged between 2026-11-01 and 2026-11-02T23:29Z, touching only `.github/workflows/tests.yaml` (cloud-test secrets) and `gradle/libs.versions.toml` (spotless 8.10.4, commons-lang3 3.21.0). That is information for the next upstream sync; this release is built on `9bed6f79` as its CHANGELOG states. If you want it absorbed first, say so: that is a new sync phase and nothing here is pushed.
- What each step makes permanent: U1 publishes every commit since `72e8ccff`, the `.planning` history included; deleting the branch later does not unpublish them. U3 puts the whole `.planning` tree on the default branch. U4 creates a GitHub release and a tag that only you can delete. `pre-v1.7.0_gg7` is a tag; the rollback path is the note's: forward only, a new commit that restores the tree of `pre-v1.7.0_gg7`, never a force-push.
- What the release contains and does not: it contains the eight `src/main` files above (194 insertions and 22 deletions against `v1.7.0_gg6`), the workflow, test and document changes outside `.planning` listed in question (2), and the allow-list and anchors under `.planning/scripts`. It does not change upstream's `integrationTests.yaml`; the scratch dry run is the first runner exercise of the `setup-gradle` 6.4.1 pin and of the compare chain with the gg7 list, and a defect that only the runner shows returns to its phase as a gap, because this phase may not edit `.github/`.
- The uncommitted `.planning/config.json` and `.planning/state.json` lines do not ride in any push.

## What happens after each answer

- Your "go" (G3): U1 and U2 from `44-USER-DISPATCH-1-SCRATCH-DRYRUN.md`: you push the tip to the scratch branch `sync/v1.7.0` and wait for its one green `tests.yaml` run, then dispatch `release.yaml` from that branch as a dry run (the run is a dry run because the ref is not `custom_build`), and report.
- After a green dry run you send your own direct "publish" message (G4). The orchestrator re-checks origin, the date and upstream, builds M detached on `72e8ccff` with `44-MERGE-MESSAGE.txt`, and tells you `X0` (the SHA you pushed) and `M`. U3 (`44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`): you push M to `custom_build` by SHA, without force, and wait for its single green `tests.yaml` run.
- G5, then U4 (`44-USER-DISPATCH-3-PUBLISH.md`): you dispatch `release.yaml` from `custom_build` with `dry_run` not ticked; the run creates the release and the tag `v1.7.0_gg7`; you never push that tag. Then U5 (`44-USER-DISPATCH-4-PRETAG-CLEANUP.md`): you push `pre-v1.7.0_gg7` by name and delete the scratch branch.
- Each step has a stop condition, and after any stop the orchestrator asks you again: a red or not-exactly-one `tests.yaml` run, a moved `custom_build` (contingency C-1), a changed UTC date against the CHANGELOG heading (C-7; already answered by question (5) if you chose to keep), an existing `v1.7.0_gg7` tag (C-8), a moved upstream (a tag past `v1.7.0` is put to you: publish gg7 first, or sync first).

Ready for the user's direct approval.
