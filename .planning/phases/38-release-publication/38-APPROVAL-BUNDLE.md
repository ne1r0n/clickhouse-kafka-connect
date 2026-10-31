# Phase 38: Approval bundle for the user-run publication of v1.7.0_gg6

The orchestrator presents this bundle verbatim and waits for the user's own direct answer. Nothing below was pushed, tagged or dispatched: origin `custom_build` reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, and every figure was measured by command at the tip `b0ab8c7b1b47185b58a22561742c00ec95b24f73` (evidence entries E-11 to E-14 in `38-COMMAND-EVIDENCE.md`). The figures grow with every later commit under `.planning`; the pre-checks of the handoff files print the live ones. An approval relayed by another agent does not count.

(1) What the first push (U1) publishes. The tip is `b0ab8c7b1b47185b58a22561742c00ec95b24f73`.
- `ahead_commits`: 331 commits since `3a3a15a9` (`git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463..HEAD`); `unpublished_commits`: 168 of them are on no origin ref yet (`git rev-list --count HEAD --not --remotes=origin`); the other 163 are on the user's two `scratch/clt-07-*` branches.
- Outside `.planning` (`git diff --shortstat`): 88 files changed, 8499 insertions, 619 deletions against the published tip. Inside `.planning`: 237 files changed, 49912 insertions, 507 deletions.
- `custom_build` left count (`git rev-list --count sync/v1.7.0..origin/custom_build`): 0, so a `--no-ff` merge adds the whole tail and nothing is lost.
- The tip equals the gated head `ea613ec35ebade7a2182cac4e6ecd5d86d099def` outside `.planning` and under `.planning/scripts`; the last commit outside `.planning` is `927ac07c30274c46b2330d597d417f9f1eda8f8b` and the allow-list commit is `cc928221b7f2a19ccad3fa2f160f970b35c671e9`.
- Changed files outside `.planning` (`git diff --name-status 3a3a15a9ae064ffe09c696b8bb87bce23419d463 HEAD`, paths in C order; 7 are `src/main`, 52 `src/test`, 4 `src/testFixtures`, 4 `src/integrationTest`, 5 workflows, 4 scripts under `.github/scripts`):

```text
M .gitattributes
M .github/dependabot.yml
A .github/scripts/baseline_tag.py
A .github/scripts/bump_version.py
A .github/scripts/compare_confluent_archives.py
M .github/scripts/failed_tests_summary.py
M .github/workflows/increment-version.yml
M .github/workflows/nightly-integration-tests.yaml
M .github/workflows/pre-release.yaml
M .github/workflows/release.yaml
M .github/workflows/tests.yaml
M .gitignore
M CHANGELOG.md
M VERSION
M docs/FORK-DELTA.md
M docs/ROLLOUT-v1.7.0_gg1.md
M docs/ROLLOUT-v1.7.0_gg5.md
A docs/ROLLOUT-v1.7.0_gg6.md
M gradle/libs.versions.toml
M gradle/wrapper/gradle-wrapper.properties
M gradlew.bat
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseCloudTest.java
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/ExactlyOnceTest.java
M src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
M src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java
M src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java
M src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
M src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java
M src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
M src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
M src/main/java/com/clickhouse/kafka/connect/util/Utils.java
A src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
M src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfigTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkJdbcPropertiesTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskBufferTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStringTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/ProtobufIngestTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryClientParityLiveTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapePinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapes.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientInsertWirePinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientTimeoutPrefixPinUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
A src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java
M src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
M src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
M src/test/resources/connect-api-references.txt
A src/test/resources/rowbinary-golden-sha256.txt
A src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTableEngine.java
M src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/CreateTableStatement.java
A src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java
A src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java
```

(2) The gate, measured on one head, `ea613ec35ebade7a2182cac4e6ecd5d86d099def` (E-11, E-12, E-13).
- Allow-list (E-11): 31 rows derived from the 242 differences of two archives rebuilt on the final tree; `OK differences=242 covered=242 uncovered=0 overlapping=0 stale_rows=0`; the compare against it exits 0 with `RESULT: PASS`, 0 unallowed, 0 stale rows; three mutations turned it red (a stale row: exit 1 `STALE-ROW`; a deleted native row: exit 1 `UNALLOWED`; a leading-wildcard row: exit 2). Archive sizes 12453556 (baseline `v1.7.0_gg5`) and 13558634 bytes (tip). The list is valid for exactly this tree: a later change of `src`, `build.gradle.kts`, `gradle` or `README.md` makes a row stale or a difference unallowed.
- `testUnit` with Docker unreachable (E-12): 1672 tests, 0 failures, 0 errors, 2 skipped, on the default JDK 17; the anchors mirror reads `rows=172 bad=0`; the tail gate ends `result=PASS` (`anchor_rows=172`) and the cumulative gate ends `result=PASS` (`anchor_rows=172`).
- Two full suites (E-13): 2270 tests, 0 failures, 0 errors, 18 skipped each (`named=7 state_mismatch_cases=11 unexpected=0`), 2m 44s and 2m 43s, against ClickHouse `SELECT version()` 26.10.3.7 on the image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (read before the first run, after it and after the second, the same each time; also the Phase 37 image).
- JVM line: both full suites ran on JDK 21 (`-PjavaVersion=21`), because the default JDK 17 on this host (an OpenJ9 build) crashed twice inside the Mockito inline mock maker (a JVM fault, not a test result); only `testUnit` ran on the default JDK 17. This is a decision for you, not a passed gate: say whether a JDK 21 record satisfies you for the two full suites.
- The `integration_test` text of E-12, verbatim: `not re-run: it needs a Connect stack and the nightly run 41152289179 on scratch de8c685b is the record that the user accepted; the gated tree has the same src/main, build and gradle files as that run (git diff de8c685b HEAD over them is empty, VERSION aside)`. You may overrule it; an overrule re-opens plan 38-04 with nothing pushed.

(3) The reviews of the release documents (12 rounds, a fresh reviewer each; 43 findings, 28 warnings and 15 info, 0 critical; `38-REVIEW.md` and `38-REVIEW-DISPOSITION.md`). Every finding is fixed except one deferred.
- Not proven: the last two fixes (IN-14, IN-15) were made after round 12 at your decision and no fresh reviewer read them; the loop ended on that decision and not on a clean round. The optional Docker-free pin of the `custom_` forwarding (the second half of IN-04) was not added, so a later client bump could change that forwarding without a test noticing. The `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08 is deferred (outside this phase's editable set; owner: you as fork maintainer; re-open trigger: the next edit of that file or the milestone close).
- WR-01: The `custom_` upgrade-breaking item is stated for every connector, but it holds only on client V2, and the V1 client uses `custom_settings` on purpose (fixed)
- WR-02: The note never says which of its D rows apply to client V1 and which to client V2, and "either client" in the `Applies when` column means the library line, not the API (fixed)
- WR-03: Row D7 and the section say a `custom_` key under `errors.tolerance=all` silently drops rows, but the same key fails the start-up ping, so the task never starts (fixed)
- WR-04: `docs/ROLLOUT-v1.7.0_gg5.md` line 39 still says no GitHub release exists for `v1.7.0_gg5`, and the corrected line 111 will itself go stale when `v1.7.0_gg6` is published (fixed)
- WR-05: The "Rolling back brings back" list omits that the `lz4-java` security release is reverted (fixed)
- WR-06: The rollback section does not say what happens to the consumer group's offsets once `v1.7.0_gg6` has committed them (fixed)
- WR-07: "Only runtimes below the supported floor are affected" names no floor, and the measured versions of the Connect API are dropped (fixed)
- WR-08: The `ROLLOUT-v1.7.0_gg1.md` correction and one FORK-DELTA sentence state more than rows D9 and D13 support (the FORK-DELTA half fixed, the docs/ROLLOUT-v1.7.0_gg1.md half deferred and carried)
- WR-09: FORK-DELTA open item 12 has no owner, and the syncer paragraph publishes items "13 to 23" although item 12 is new and published knowingly open too (fixed)
- IN-01: Row B1 is labelled `measured` for a "before" claim that its How cell does not cover (fixed)
- IN-02: Row B8 states the Direct-path limit without its condition, and the note omits the partition-changing-SMT limit that FORK-DELTA item 16 carries (fixed)
- IN-03: The first two carried limits say "observed" outside the four defined labels, with no command or test named (fixed)
- IN-04: Row D7's forwarding evidence is a maintainers' probe that is not in the repository, and no pin guards the upgrade-breaking behaviour (the wording fixed, the optional Docker-free pin not added)
- IN-05: The offset-reset advice does not say what the chosen position costs (fixed)
- IN-06: The CHANGELOG `zstd-jni` bullet omits that the default configuration never loads the native (fixed)
- IN-07: FORK-DELTA states the gg6 allow-list as a fact although the file does not exist at HEAD (fixed)
- WR-10: The CHANGELOG and FORK-DELTA item 16 state the Direct-path limit without the condition that the note now carries (fixed)
- WR-11: Row B1 carries the label `measured` for a half of its claim that its own How cell says was not repeated; D7's forwarding and B8's probe are the same case (fixed)
- WR-12: FORK-DELTA item 22 says "The one release that exists" and goes stale when `v1.7.0_gg6` is published; the same file states the release process as a fact (fixed)
- IN-08: The new client scoping lists row D14 as also holding on client V1, but the piped streams behind it are used only on the V1 insert path (fixed)
- WR-13: Statements that hold only for client V2 are repeated outside the scoped rows without their scope, among them the clean-stop advice (fixed)
- IN-09: The scoping sentence omits that every connector builds a V2 client at construction, so row D1's effects reach client V1 connectors too (fixed)
- WR-14: "Every connector builds a client V2 object at start" omits that a client V1 connector builds another one with every insert, and the V2-client-per-start limit is stated for V2 only (fixed)
- WR-15: The insert-timeout limit is stated for every connector, but the mechanism exists only on the client V2 insert path (fixed)
- WR-16: The note, the CHANGELOG and FORK-DELTA item 13 present the per-insert `ssl`/`sslmode` WARN on client V1 as a consequence of this release, but client `0.9.6` already logged the same warning at the same site (fixed)
- WR-17: "A connector reaches the ZSTD path only with `compress=true` together with `setCompressionMethod=false`, or with `decompress=true`" is stated for every connector, but the client V1 artifacts reach `zstd-jni` through their own algorithm options (fixed)
- WR-18: The text change of the unmapped-key WARN is stated only in a section paragraph, while the log-alert guidance (step 5, row D6, client step 3) is scoped to client V2 and lists no such text (fixed)
- IN-10: The scoping paragraph of section D is one block of about 3,300 characters that mixes the rule an operator needs with log history and tool provenance (fixed)
- IN-11: The `Applies when` column is defined with three values, but row D14 uses a fourth, `client V1 insert path` (fixed)
- WR-19: Row B6 and its subsection call the worker-image `javap` check `measured`, but by the note's own definitions that half is `measured in research (not re-run)` (fixed)
- WR-20: The note and the CHANGELOG never say that the "Direct path" is the unbuffered path, and they list its limit as a limit "of buffered connectors" next to a "Not affected: unbuffered connectors" statement (fixed)
- WR-21: "Not changed by this release: ... buffered connectors whose topics are not renamed" contradicts row B6, which says every buffered connector needs the three new `SinkRecord` accessors (fixed)
- WR-22: The note says the unbuffered path is not changed because "its files are unchanged since `v1.7.0_gg5`", but its inserts go through two files that this release changed (fixed)
- IN-12: "commits nothing" for the unbuffered path under a rename is stated for the whole connector, but the code drops only the keys that carry the renamed topic (fixed)
- WR-23: A rename onto a topic name that another input topic already has is not "commits nothing" or "a topic the SMT leaves alone still commits as before": the target's partitions receive offsets derived from the other topic (fixed)
- WR-24: The fix also changes what a revoked partition does to its buffered records under a rename, but the note and the CHANGELOG describe only the offsets, and row B5 lists "a revoked partition drops its buffered records" among the sentences about what stays as it was (fixed)
- WR-25: The first-start reread is described as "a full duplicate load for a table without deduplication" for every buffered connector, but with `exactlyOnce=true` it meets the stored state and usually fails the task (fixed)
- WR-26: "The offsets returned are unchanged for ... buffered connectors whose topics are not renamed" and "affected: an SMT that changes the topic" are false for an SMT that changes only the partition or the offset (fixed)
- IN-13: "The state-removal call for a revoked partition ... removes nothing for a renamed topic" never says what it removes, and the note's own definition of "state record" makes it read as if the state were removed for the other topics (fixed)
- WR-27: The revocation change is stated for every SMT of the widened group, but an SMT that changes only the offset leaves revocation as it was, and one that changes only the partition did not "drop nothing" (fixed)
- WR-28: The first-start subsection says a chunk below the stored range that starts at offset 0 "is inserted again", but that reset makes every following reread chunk new, so the whole retained topic is loaded again, silently (fixed)
- IN-14: The exactly-once reload is bounded "up to the old stored range" although the chunk of the stored range itself is inserted again, and step 2 offers two outcomes as the whole choice and ties them to where the retained log starts, not where the first start resumes (fixed, not reviewed again)
- IN-15: "Skipped silently under `tolerateStateMismatch=true`" and "skipped the batch silently" omit that the code logs a WARN for every skipped chunk (fixed, not reviewed again)
- Carried to the milestone close with an owner and a re-open trigger, listed in `38-LOCAL-CLOSE-RECORD.md`: WR-08 (the gg1 half), the IN-04 pin gap, the 14 items of the carried-items table (the limits of the buffered fix, the writer's client that `stop()` never closes, the four-minute insert timeout, the Datagen counting, the `scratch/clt-07-*` branches, the missing dependency checksum file, the Phase 36 review notes, the retriable `DataException` cases, the `increment-version.yml` header, the Phase 37 ledger rows, the `tests.yaml` gate that accepts a green run of any branch, the publish-path caveat, the states of the pull requests #10 and #17, and the nightly's one green run).

(4) The dry-run merge D (E-14), built in a throwaway detached worktree on `3a3a15a9ae064ffe09c696b8bb87bce23419d463` with the real merge message file and never pushed.
- Parents `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and `b0ab8c7b1b47185b58a22561742c00ec95b24f73`, the tree of the tip, the subject `Merge sync/v1.7.0: release v1.7.0_gg6`, no `.gsd-ws-arg`, `VERSION` `v1.7.0_gg6`, the allow-list present; the baseline selection on D names `v1.7.0_gg5` and no `v1.7.0_gg6` tag is merged. A plain merge in a fresh worktree at `3a3a15a9` stops on a line-ending difference in `gradlew.bat`, so the merge runs with git's attributes switched off (`git --attr-source=<empty tree>`), and the build of M does the same.
- Operator documents: no `TRUNCATE`, `DROP TABLE`, `DROP DATABASE` or `DELETE FROM` in the note, the CHANGELOG section or the added FORK-DELTA lines.
- The rollback block of the note, run unchanged on D: exit 0, prints `ok`, `VERSION` `v1.7.0_gg5`, the note absent, the tree equal to tag `pre-v1.7.0_gg6`; a revert restores D's tree.
- `SELECT version()` 26.10.3.7 on the same image digest as the two suites.

(5) The exact sequence after your go, each step with its stop condition (a red or not-exactly-one run, a moved `custom_build`, a changed UTC date against the CHANGELOG heading, a tag that exists); after any stop the orchestrator asks you again.
- U1 and U2 (first handoff, `38-USER-DISPATCH-1-SCRATCH-DRYRUN.md`): you push the tip to the scratch branch `sync/v1.7.0`, wait for its one green `tests.yaml` run, then dispatch `release.yaml` from that branch as a dry run and report. You run both, with your own account.
- After a green dry run you send the orchestrator your own direct "publish" message; the orchestrator re-checks origin and the date, builds the merge commit M detached on `3a3a15a9`, and hands you `X0` (the SHA you pushed) and `M`.
- U3 (`38-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`): you push M to `custom_build` by SHA, without force, and wait for its single green `tests.yaml` run.
- U4 (`38-USER-DISPATCH-3-PUBLISH.md`): you dispatch `release.yaml` from `custom_build` with `dry_run` not ticked. The run creates the release and the tag `v1.7.0_gg6`. You never push that tag.
- U5 (`38-USER-DISPATCH-4-PRETAG-CLEANUP.md`): you push `pre-v1.7.0_gg6` by name, delete the scratch branch, and close or leave the pull requests.

(6) What each step makes permanent.
- U1 publishes every commit since `3a3a15a9`, the `.planning` history included; deleting the branch later does not unpublish them.
- U3 puts the whole `.planning` tree on the default branch.
- U4 creates a GitHub release and a tag that only you can delete.
- `pre-v1.7.0_gg6` is a tag. The rollback path is the note's: forward only, a new commit that restores the tree of `pre-v1.7.0_gg6`, never a force-push.

(7) What the release contains and does not.
- It contains the seven `src/main` files above (112 insertions and 31 deletions against `v1.7.0_gg5`), the workflow, guard, allow-list and document changes outside `.planning` listed in item (1), and the allow-list and anchors under `.planning/scripts`.
- It does not change upstream's `integrationTests.yaml`; the nightly stays off (`NIGHTLY_ENABLED` is not set).
- Pull requests: the heads of #16, #18, #19 and #20 become ancestors of `custom_build`, and GitHub normally marks them merged (an assumption, A1); #17 (client `0.11.0`) and #10 (`protobuf-java` 4) stay open until you or Dependabot closes them (A2). No agent comments on or merges any of them.
- The scratch dry run is the first runner exercise of the archive compare chain; a defect that only the runner shows returns to Phase 37 as a gap, because Phase 38 may not edit `.github/`.

(8) Housekeeping and what does not ride.
- The CHANGELOG heading date is `2026-10-31` and today's UTC date when this bundle was measured was `2026-10-31`: equal. The orchestrator compares them again before U3 and asks you directly if they differ.
- The uncommitted `.planning/config.json` and `.planning/state.json` lines, and the ` M gradlew.bat` line of a fresh checkout at `3a3a15a9`, do not ride in any push.
- The orchestrator reads GitHub with read-only calls only; you run every write to origin and every dispatch.
