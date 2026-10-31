---
phase: 38
reviewed: 2026-10-31T14:35:00Z
depth: standard
diff_base: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
files_reviewed: 5
files_reviewed_list:
  - docs/ROLLOUT-v1.7.0_gg6.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
  - docs/ROLLOUT-v1.7.0_gg5.md
findings:
  critical: 0
  warning: 12
  info: 8
  total: 20
status: issues_found
rounds: 2
---

# Phase 38: Code Review Report

**Reviewed:** 2026-10-31
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Five release documents were read against the tree at HEAD `83026266` (branch `sync/v1.7.0`), the evidence entries E-01 to E-07, the three handoff files, the public `client-v2` 0.11.0 sources and the two archives built in E-05 (still on disk, re-compared by me). Numbers were re-derived rather than trusted. These hold: the `src/main` shortstat (7 files, 112 insertions, 31 deletions), every FORK-DELTA raw-number row and the since-`v1.7.0_gg5` directory split (79 = 7 + 52 + 4 + 4 + 10 + 2 files, 7939 and 478 sum correctly), the archive figures of row D12 (7801 and 7933 entries, 7399 and 7513 files, 4 only in the old archive, 118 only in the new, 120 changed, 12453556 and 13558634 bytes, guava `33.7.2-jre` and httpclient5 `5.6.5` on both sides, three zstd natives), the upstream states of row B7 (re-read: issue 777 `open`, pull request 786 `closed`, not merged), the `v1.7.0_gg5` asset (size 12453556, digest `4c69b6fb...`, published 2026-10-28), tag `pre-v1.7.0_gg6` and `v1.7.0_gg5` both on `3a3a15a9`, and the CHANGELOG (every released section byte-identical to the file at `3a3a15a9`; the `Unreleased` entry is verbatim under Improvements). I also ran the rollback block, the forward path and the revert on the release tip in a throwaway clone under `/tmp/gsd38/review/` (no push to the real origin, clone removed afterwards): every claim of the Rollback section holds.

The defects are of three kinds. First, the headline upgrade-breaking item (`custom_` keys) is stated for every connector, but the code path behind it exists only for client V2, and this tree's default client, when `client_version` is not set, is V1 (WR-01, WR-02); the failure mode the note describes for `errors.tolerance=all` also does not match the start-up path (WR-03). Second, the rollback section omits two consequences an operator needs (WR-05, WR-06), and one prerequisite of the new fix is not stated as a version (WR-07). Third, two older documents were touched in a way that leaves a contradiction or an overclaim (WR-04, WR-08), and one FORK-DELTA bookkeeping rule is not met (WR-09).

## Warnings

### WR-01: The `custom_` upgrade-breaking item is stated for every connector, but it holds only on client V2, and the V1 client uses `custom_settings` on purpose

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:28`, `docs/ROLLOUT-v1.7.0_gg6.md:122`, `docs/ROLLOUT-v1.7.0_gg6.md:148`, `CHANGELOG.md:26`
**Issue:** Step 1 of "Before you upgrade" says "A `custom_` key makes the task fail after the upgrade", the bold sentence of row D7 says "remove or rename such keys before moving", and the CHANGELOG bullet says the keys "are now forwarded to the server, which rejects them, so the task fails". The forwarding is the V2 client's `ClientConfigProperties` mapping (`custom_settings_prefix` default `custom_`, 0.11.0 source lines 446-457), applied by `createClientV2` only. Client V1 handles `custom_settings` itself: `NetworkCompressionPinLiveTest.operatorOverrideReachesServerOnV1` pins `?custom_settings=network_compression_method%3D...,max_threads%3D3` as a working V1 option. The connector's default is V1: `ClickHouseSinkConfig.java:380` reads `props.getOrDefault(CLIENT_VERSION, "V1")`, and Connect hands the raw property map to `start()`, so a connector that does not set `client_version` runs V1 (`ClickHouseWriter.java:85`). The word "V2" appears in the section text and in the first sentence of D7, but step 1, the bold sentence, the "What to search for" paragraph and the CHANGELOG do not carry it, so an operator on V1 who sets `custom_settings` as intended is told to delete a working setting.
**Fix:** Scope every statement: in step 1, the D7 bold sentence, the section paragraph (line 148) and the CHANGELOG bullet add "on client V2 (`client_version=V2`)" and one sentence "Client V1 reads `custom_settings` itself and is not affected; keep it". Add the V1 pin to the How cell, labelled `measured` (green on the release tip in the full suite), and say that the search recipe also finds V1 connectors, which need no change.

### WR-02: The note never says which of its D rows apply to client V1 and which to client V2, and "either client" in the `Applies when` column means the library line, not the API

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:22`, `docs/ROLLOUT-v1.7.0_gg6.md:112`, `docs/ROLLOUT-v1.7.0_gg6.md:116-129`
**Issue:** The column definition (line 22) reads `either client` as "client `0.9.6` and `0.11.0`". The same word "client" is used in the repository for V1 and V2. Rows D1 (V2 endpoint and options), D5 (`clickhouse-java-v2/...` user agent), D10 (V2 socket buffers) and `createClientV2`-based rows are V2-only; D13 mixes both; D2 to D4 and D6 were pinned through the V2 client. The string `client_version` appears nowhere in the note, although a connector that does not set it runs V1 (WR-01). What the move from `0.9.6` to `0.11.0` changes for a V1 connector (the V1 artifacts `clickhouse-client`, `clickhouse-http-client` and `clickhouse-data` move with the same catalog key) is neither listed nor declared out of scope, except D13 and D14. An operator on the default client would read rows D1 to D10 as theirs, or conclude that the client bump does not touch them.
**Fix:** In the intro of section D add: "Rows D1 to D6 and D10 describe client V2 (`client_version=V2`). A connector that does not set `client_version` runs client V1; for it only rows D13 and D14 apply, and the other effects of the V1 artifacts on `0.11.0` were not audited here" (or list them if they were). Rename the `Applies when` value `either client` to `either client library (0.9.6 or 0.11.0)` so it cannot be read as V1 or V2.

### WR-03: Row D7 and the section say a `custom_` key under `errors.tolerance=all` silently drops rows, but the same key fails the start-up ping, so the task never starts

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:146`, `docs/ROLLOUT-v1.7.0_gg6.md:122`
**Issue:** Line 146 says that with `errors.tolerance=all` "the failed records are handed to the dead letter queue reporter ... instead of failing the task, so the symptom is rows that never reach the table, which is easy to miss (read from the code: `ProxySinkTask.put` and `Utils.handleException`)". The client turns a `custom_` key into a client-level server setting (0.11.0 `ClientConfigProperties.java:454-455`, `serverSetting(key)`), and `HttpAPIClientHelper.addRequestParams` (line 564, lines 1050-1056) adds every such setting to every request, including the `SELECT 1` of `Client.ping` (`Client.java:1496`). `ClickHouseWriter.start` calls `chc.ping()` (line 103) and returns false on failure, and `ProxySinkTask.start` then throws `RuntimeException("Connection to ClickHouse is not active.")` (lines 69-72). So the task fails at start under every `errors.tolerance`, before any `put`; the code 115 text is logged only at DEBUG by the client (`Client.java:1501`), and the connector logs "Unable to ping ClickHouse instance." The label `read from the code` for the task failure rests on `handleException` alone and misses the earlier path. I read the sources; I did not run a connector with such a key.
**Fix:** Replace the sentence with the start-up path: "The key is sent with every request, including the connector's start-up ping, so the task fails at start with `Connection to ClickHouse is not active.` under any `errors.tolerance`; the log shows `Unable to ping ClickHouse instance.` and shows code 115 only at DEBUG." Keep the label `read from the code` and cite `ClickHouseWriter.start`, `ProxySinkTask.start` and the client's `ping`. Confirm it once with a V2 client against a throwaway container before the text is final.

### WR-04: `docs/ROLLOUT-v1.7.0_gg5.md` line 39 still says no GitHub release exists for `v1.7.0_gg5`, and the corrected line 111 will itself go stale when `v1.7.0_gg6` is published

**File:** `docs/ROLLOUT-v1.7.0_gg5.md:39`, `docs/ROLLOUT-v1.7.0_gg5.md:111`
**Issue:** Line 39 reads "No GitHub release is published for `v1.7.0_gg5`: the release is its two tags." A release `v1.7.0_gg5` exists (id 426690002, one asset, `published_at` 2026-10-28T13:16:09Z; E-01 `releases_on_origin`). Line 111 now says the opposite ("the only release asset published so far belongs to `v1.7.0_gg5`"), so one note holds two contradictory statements, and the gg6 note (rollback step 3) agrees with line 111. The plan allowed one changed line, but the contradiction is in a note that operators read when rolling back to `v1.7.0_gg5`. Separately, "so far" in line 111 becomes false when the release workflow attaches a `v1.7.0_gg6` archive, and the note is part of the published tree, so it cannot be corrected after publication without another release.
**Fix:** Change line 39 to "A GitHub release for `v1.7.0_gg5` was published by hand on 2026-10-28, after this note was written; before that the release was its two tags." Change line 111 to "the `v1.7.0_gg4` tag has no release asset (the `v1.7.0_gg5` release has one, published 2026-10-28)", with no time-relative wording.

### WR-05: The "Rolling back brings back" list omits that the `lz4-java` security release is reverted

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:231-237`, `docs/ROLLOUT-v1.7.0_gg6.md:186`
**Issue:** Row E1 says `lz4-java` moves from `1.12.0` to `1.12.1`, "a security release (`GHSA-48mj-8f7c-7qfm`)". Row E4 says rolling back "reverses row B1 and rows D1 to D10", and the bullets that follow list the MBean name, offsets, client behaviour, the new setting and the documents. A rollback to the `v1.7.0_gg5` archive also restores `lz4-java 1.12.0` (step 3 even states that the archive "carries ... `lz4-java` `1.12.0`"), that is, the version the note itself calls the one fixed by a security release, and it drops `zstd-jni`. An operator deciding whether to roll back is not told that the rollback has a security cost. This is an understated claim that hides a fact the note already holds.
**Fix:** Add a bullet: "row E1 reversed: `lz4-java` goes back to `1.12.0`, the version affected by `GHSA-48mj-8f7c-7qfm`; prefer rolling forward when the reason for the rollback is not the client or the offset change", and change E4 to "reverses rows B1, D1 to D10 and E1".

### WR-06: The rollback section does not say what happens to the consumer group's offsets once `v1.7.0_gg6` has committed them

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:234`, `docs/ROLLOUT-v1.7.0_gg6.md:186`
**Issue:** The bullet reads "row B1 reversed: ... offsets are again keyed by the renamed topic". The note spends a whole subsection on the first start after the upgrade (rows B3 and B4) but never states the mirror case. After `v1.7.0_gg6` has flushed and committed offsets under the original topic, the `v1.7.0_gg5` archive commits nothing for those partitions (row B2), so the group's committed offset freezes at the last `v1.7.0_gg6` commit, and the next restart or rebalance on `v1.7.0_gg5` resumes from that frozen offset and rereads everything written since. For a table without deduplication that is a duplicate load of the whole rollback period, and B4 (offset retention) applies again. This follows from the same Kafka semantics as B3 and needs the same label.
**Fix:** Add under the rollback bullets: "After a rollback, committed offsets stop advancing again. The group keeps the offsets that `v1.7.0_gg6` committed, so a later restart on `v1.7.0_gg5` resumes there and rereads from that point (derived from Kafka consumer-group semantics (not measured)); run the describe command of section B before and after the rollback and decide whether to reset the offsets, with the try-first caution of step 2."

### WR-07: "Only runtimes below the supported floor are affected" names no floor, and the measured versions of the Connect API are dropped

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:46`, `docs/ROLLOUT-v1.7.0_gg6.md:30`, `docs/ROLLOUT-v1.7.0_gg6.md:81-88`
**Issue:** B6 and the "Minimum Connect runtime" subsection tell the operator to check that the runtime has three `SinkRecord` accessors, and end with "Only runtimes below the supported floor are affected". The floor is never given as a version. FORK-DELTA names it ("the text of #786 says Kafka Connect 3.6 and later", first Connector source row), and the handoff of Phase 35 adds the measured facts that all three accessors exist in `connect-api` 4.1.1 and 4.4.0 as well as in the 7.9.7 worker jar; the note keeps only the worker image. An operator on a self-managed Connect cluster cannot tell from the note which Apache Kafka release is enough, and the sentence "below the supported floor" refers to something the reader was not shown. Step 3 of "Before you upgrade" reduces to "check the accessors" with no way to check them.
**Fix:** State the version and its label: "The accessors were added in Apache Kafka Connect 3.6 (the text of upstream pull request 786; not re-verified here). They are present in the `connect-api` jar of the `confluentinc/cp-kafka-connect-base:7.9.7` worker image (Kafka 3.9) and in `connect-api` 4.1.1 and 4.4.0 (measured by `javap`; guarded by `ConnectApiReferencesUnitTest`)", and give the check: `javap -cp <connect-api jar> org.apache.kafka.connect.sink.SinkRecord | grep original`.

### WR-08: The `ROLLOUT-v1.7.0_gg1.md` correction and one FORK-DELTA sentence state more than rows D9 and D13 support

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:242`, `docs/FORK-DELTA.md:290`
**Issue:** The added gg1 sentence says that on V2 of client `0.11.0` the combination `compress=true` plus `setCompressionMethod=false` "decodes ZSTD and loads the `zstd-jni` native, which needs an executable temp folder ... (measured in the public confluentinc/cp-kafka-connect-base:7.9.7 image)". In the gg6 note the ZSTD decode is `measured` (D13) but the native loading and the executable-folder requirement are `measured in research (not re-run)` (D9), and the gg6 note defines that label so the reader knows no run was repeated. The gg1 sentence uses the bare word "measured" for both. The same sentence says client V1 "of any line" still fails with `Invalid LZ4 magic byte` or `Magic is not correct`; FORK-DELTA line 290 repeats "client V1 of any line". Only two lines were observed (`0.9.6` and `0.11.0`; D13 and the D-17 row say "either client line" and "both client lines"), so "any line" is wider than the evidence. The brief's rule for the five documents is that no claim is stronger than its source.
**Fix:** In gg1 item 6 write "on V2 of client `0.11.0` that combination decodes ZSTD (measured) and loads the `zstd-jni` native, which needs an executable temp folder (measured in research, not re-run, in the public confluentinc/cp-kafka-connect-base:7.9.7 image)"; replace "of any line" by "on both client lines checked (`0.9.6` and `0.11.0`)" in the gg1 sentence and in FORK-DELTA line 290.

### WR-09: FORK-DELTA open item 12 has no owner, and the syncer paragraph publishes items "13 to 23" although item 12 is new and published knowingly open too

**File:** `docs/FORK-DELTA.md:222`, `docs/FORK-DELTA.md:286`
**Issue:** The brief requires each open item to carry an owner and a re-open trigger. Items 13 to 23 do; item 12 (protobuf-java stays on 3.x, titled "published knowingly open") has a re-open trigger (FUT-03) but no "Owner:" clause. Item 12 is new in this release (the old file ended at item 11), so the paragraph "For the next syncer" should read "open items 12 to 23", not "13 to 23" (line 286); the evidence record E-04 counts 11 added items, but the diff adds 12 (items 12 to 23). A reader following the paragraph skips the one item that concerns an open Dependabot pull request.
**Fix:** Append to item 12: "Owner: fork maintainer (the user)." and change line 286 to "The release publishes the open items 12 to 23 of Known costs."

## Info

### IN-01: Row B1 is labelled `measured` for a "before" claim that its How cell does not cover

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:41`
**Issue:** The effect says "Before, `preCommit` returned offsets keyed by the renamed topic". The How cell names only the post-fix tests (`BufferedOffsetCommitUnitTest`, `BufferedOffsetCommitLiveTest`: each returned value equals the end offset of its original partition). The "before" half was measured by the red runs of the same tests on the unfixed tree, which the cell does not say. The claim is true, but the cell cannot back it as written.
**Fix:** Add to the How cell: "the same tests fail on the tree before the fix, where the returned keys name the renamed topic".

### IN-02: Row B8 states the Direct-path limit without its condition, and the note omits the partition-changing-SMT limit that FORK-DELTA item 16 carries

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:48`, `docs/ROLLOUT-v1.7.0_gg6.md:245`, `docs/FORK-DELTA.md:230`
**Issue:** `DirectDeliveryStrategy.preCommit` returns Connect's own offsets first when `exactlyOnce=false` and `ignorePartitionsWhenBatching=true` (default `false`), and only then reaches the `reportInsertedOffsets` branch that drops every key. So "the Direct path with `reportInsertedOffsets=true` under a rename commits nothing" is true only without that option. FORK-DELTA item 16 also records that a partition-changing SMT under exactly-once buffering was read from the code and not probed; the operator note and its carried-limits bullet do not mention it, so the two documents list different limits for the same fix. The scratch probe behind the Direct-path claim stays honestly flagged as not in the repository.
**Fix:** Add "(with `ignorePartitionsWhenBatching=false`, the default)" to the Direct-path limit, and add a fifth limit: "a partition-changing SMT under exactly-once buffering was read from the code and not probed".

### IN-03: The first two carried limits say "observed" outside the four defined labels, with no command or test named

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:243-244`
**Issue:** The note promises that every claim carries one of four labels. The bullets about `ClickHouseWriter.stop()` and the four-minute insert timeout use "observed on both client lines" plus a source site. `ClickHouseWriter.stop` does not close the helper client (confirmed by reading it), but "observed" names no measurement. The insert-timeout claim (an already completed future, `socket_timeout` defaulting to 0) is read from code plus client options and cannot be told apart from a measurement.
**Fix:** Replace "observed" by `read from the code` (or `measured in research (not re-run)` if that is what happened) in both bullets.

### IN-04: Row D7's forwarding evidence is a maintainers' probe that is not in the repository, and no pin guards the upgrade-breaking behaviour

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:122`, `docs/ROLLOUT-v1.7.0_gg6.md:133`
**Issue:** B8 says openly that its probe "is not in the repository and not something you run"; row D7, the item the note calls upgrade-breaking, gives "a stub-server probe of the maintainers" with the label `measured` and no such statement. Nothing under `src/test` asserts that a `custom_` key is forwarded on 0.11.0 (the only `custom_settings` test is the V1 one), so a later client bump could change it unnoticed, and the note's `measured` rests on a run the reader cannot repeat.
**Fix:** Say in the D7 How cell that the probe is not in the repository; consider a Docker-free pin in the style of `ClientEndpointAndOptionsPinUnitTest` that builds a V2 client with `?custom_myparam=7` and asserts the request line.

### IN-05: The offset-reset advice does not say what the chosen position costs

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:78`
**Issue:** Step 2 correctly flags the reset as live state to try first, but "set the group's offsets to the position you want" gives no guide. A position later than the last record in the table skips records permanently; an earlier one rereads them (a duplicate load without deduplication). The command also needs the group to be inactive, which the step covers only by "stop the connector".
**Fix:** Add one sentence: "Choose a position at or before the last record already in the table: a later position skips records for good, an earlier one reloads them."

### IN-06: The CHANGELOG `zstd-jni` bullet omits that the default configuration never loads the native

**File:** `CHANGELOG.md:28`
**Issue:** "`zstd-jni` natives ship for linux/amd64, linux/aarch64 and darwin/aarch64 only" reads as a warning for every other platform. The note (D9, section "zstd-jni") says the default configuration never reaches the ZSTD path, so only connectors with `compress=true` plus `setCompressionMethod=false`, or `decompress=true`, are affected.
**Fix:** Append "; only a connector that selects the ZSTD path is affected, and the default configuration does not".

### IN-07: FORK-DELTA states the gg6 allow-list as a fact although the file does not exist at HEAD

**File:** `docs/FORK-DELTA.md:84`, `docs/FORK-DELTA.md:286`
**Issue:** Line 286 says the archive compare for gg6 runs "against `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`, written last from the final tree", and row 84 says a `VERSION` with no list file stops the release run. At HEAD the file is absent (E-02 `allowlist_gg6_present: no`), so run on this tree the release workflow would stop at the baseline step. The wording "written last" signals a pending step, but states no condition.
**Fix:** Say "(written in the release phase before the merge; absent until then)", or drop the sentence about the file from the paragraph and keep only the compare command.

---

round: 1 findings: 16

## Mandated confirmations (RLS-04)

- (a) completeness: confirmed with exceptions that are findings. All 29 rows (B1 to B8, C1 to C3, D1 to D14, E1 to E4) exist; I mapped each row of the three handoff files to the note: Phase 35 item to B1 to B8 and the sections "First start", "What to do" and "Minimum Connect runtime"; the 14 items of Phase 36 to D1 to D14 one to one, with the labels equal or weaker (D5 tail, D6 text, D7 rejection and D8 default method moved from research to read-from-code or measured on E-05 evidence; D6 and D7 gained a split that only weakens); the FUT-05 item to C2 and the section "The bypass of errors.tolerance". Softened or dropped details: the Connect API 4.1.1 and 4.4.0 measurements and the supported version (WR-07), the Direct-path condition and the partition-changing limit (IN-02). Nothing is made stronger than its source except as listed in (b).
- (b) labels: not confirmed, see WR-08 (gg1 sentence and FORK-DELTA line 290 say "measured" and "any line" where D9 and D13 say less), WR-03 (the `read from the code` label of the D7 task failure misses the start-up path), IN-01 (B1), IN-03 (carried limits) and IN-04 (D7 probe). Every research-only claim keeps `measured in research (not re-run)`; the four labels are defined before the first section; I read the "never", "only", "exactly", "all" and "identical" wording of each row against its How cell: the uses in D1, D3, D9, D11 and D13 are covered by the row's evidence or by the research label, and I found no further overclaim beyond the findings above.
- (c) numbers, commands, versions and names: confirmed. Re-derived: `src/main` 7 files, 112 insertions, 31 deletions; FORK-DELTA wider 331, 39665, 1161 and narrow 40, 2989, 272, and the 79, 7939, 478 split; D12 archive figures from the two archives of E-05 (re-ran the compare, 18 version lines, only `lz4-java` differs); the B7 upstream states (re-read today); the asset digest and size of `v1.7.0_gg5`; MBean and client names against `SinkTaskStatistics.buildMBeanName` and `CONNECT_CLIENT_NAME`; all 14 test classes named in the rows exist.
- (d) rollback: confirmed for the steps, not for the list of consequences. On the release tip in a throwaway clone: step 1 printed nothing, the block printed `ok`, `git status` then lists ` M gradlew.bat`, a plain `git switch` and `git revert` abort, the `--attr-source` forms (git 2.50.1 here) succeed, `git pull --ff-only` works, the revert restores the tree, `VERSION` reads `v1.7.0_gg5` after the rollback commit, and the note is readable with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg6.md`. Tag `pre-v1.7.0_gg6` and `v1.7.0_gg5` are both `3a3a15a9`. The gg5 release asset sentence is true (size, digest, date). The list "Rolling back brings back" is incomplete: WR-05, WR-06. The gg5 note still contradicts itself on a GitHub release: WR-04.
- (e) release object: confirmed. The note and the CHANGELOG state no digest, tag commit or URL of the `v1.7.0_gg6` release object; the sentence "The release workflow creates the release and its tag at the merged commit and attaches the archive" describes the workflow, and "The tag is published together with this release" concerns `pre-v1.7.0_gg6` only.
- (f) no data destruction: confirmed. No advice empties a table or deletes state; the only SQL is a read-only `system.query_log` query. The offset reset is flagged as live state to try first on an unused group; its cost is incomplete (IN-05).
- (g) the `custom_` item: not confirmed, see WR-01, WR-03 and IN-04. The search recipe (`custom_` in every `jdbcConnectionProperties`) finds the keys and the remedy (remove or rename) is stated, but both are unscoped for client V1 (WR-01). The sentence "The connector has no switch that restores the old behaviour in this release" is true of the tree: no config key or code path in `src/main` touches `custom_settings_prefix` or the prefix; the client has such a key, untested through the connector, so the sentence should stay worded as "the connector".
- (h) `additionalRetriableErrorCodes`: confirmed. Parsing (`ClickHouseSinkConfig.parseErrorCodes`: trim, skip empty, set, reject non-integer and values at or below zero), the 15 built-in codes with 159 (`Utils.DEFAULT_RETRIABLE_ERROR_CODES`), and the bypass of `errors.tolerance` and the DLQ (the retriable check runs before the tolerance branch in `Utils.handleException`; Connect redelivers with no retry budget) all match the note. C3's two cases are retriable as stated, because the server-error check precedes the `DataException` rethrow.
- (i) cross-document agreement: confirmed. Version `v1.7.0_gg6`, rollback target `3a3a15a9` / `v1.7.0_gg5`, the 7 changed `src/main` files and the 40 differing files, client `0.11.0`, lz4 `1.12.1` and the open-items list agree across the note, the CHANGELOG, FORK-DELTA and the gg1 and gg5 edits, apart from WR-04 and WR-09 (item range).
- (j) CHANGELOG: confirmed. The released sections from `1.7.0_gg5` down are byte-identical to the file at `3a3a15a9`; no `Unreleased` heading remains; the Improvements entry equals upstream's `Unreleased` text at `9bed6f79` line for line. The `1.7.0_gg6` section holds no planning identifier; one line exceeds 120 characters, the verbatim upstream entry.
- (k) FORK-DELTA: not confirmed, see WR-09 and IN-07. Stale forward references are replaced by outcomes or conditions (the remaining "Phase 36/37" hits name finished work), the recounted sentences hold (33 + 14 + 16 = 63 files at `v1.7.0`, plus 10 new = 40; the gg5 recount 36 + 14 + 13 = 63), the shortstat rows and the directory split match `git diff --shortstat`, and open items 13 to 23 each carry an owner and a re-open trigger; item 12 has no owner.
- (l) no host, connector, cluster or system name, and no planning, requirement or review identifier, in the note or the CHANGELOG section: confirmed by scanning both for identifiers, paths, host names and user names. FORK-DELTA, outside this requirement, keeps its planning identifiers by design.

Rows by label in the note, counted by its own check: 29 rows, with 20 measured, 15 read from the code, 3 derived and 4 `measured in research (not re-run)` (split rows count once per label). Nothing was re-run for the research-only rows beyond the source reads above: D2 cadence, D8 client-side failure, D9 worker behaviour, D6 HTTP error text and the zstd platform list remain `measured in research (not re-run)`.

---

_Reviewed: 2026-10-31T14:35:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

---

## Round 2

**Reviewed:** 2026-10-31
**Scope:** the fix commits only (`git diff cdbdbbd7 HEAD -- . ':(exclude).planning'`, 16 fix commits and the evidence commit, HEAD `21d70b07`): `docs/ROLLOUT-v1.7.0_gg6.md`, `docs/ROLLOUT-v1.7.0_gg5.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md`.
**Status:** issues_found

All 16 round-1 findings are resolved by their fix commits, apart from the gg1 half of WR-08, which was deferred by the orchestrator. I re-checked, against the sources and the tree and not from the fixer's log: the client selection (`ClickHouseSinkConfig.java:380` default `V1`; `ClickHouseWriter.start` and `KeeperStateProvider` use client V2 for any other value, an empty value included), the `custom_` start-up failure path (`ClickHouseHelperClient.pingV2`, `ClickHouseWriter.start`, and the E-11 probe record of the forwarding), the `javap` command on the two cached `connect-api` jars (4.1.1 and 4.4.0 both print the three accessors), the rollback block (run unchanged in a throwaway worktree under `/tmp/gsd38/review/`, removed afterwards with its branch: it printed `ok`, `VERSION` read `v1.7.0_gg5`, the note was absent, ` M gradlew.bat` showed, `lz4Java` read `1.12.0` and `clickHouseDriver` `0.9.6` after it, and `git show HEAD^:docs/ROLLOUT-v1.7.0_gg6.md` read the note), the `lz4Java` values at `3a3a15a9` and at HEAD (`1.12.0` and `1.12.1`), the released CHANGELOG sections (byte-identical to `3a3a15a9`, checked by `diff`), the open-item owners and triggers (items 12 to 23, each has both), and `38-note-check.py all` (29 rows, 20 measured, 15 read from the code, 3 derived, 4 research; no forbidden identifier). The new text introduces no wrong command and no identifier. Four defects remain, none about the `custom_` item itself.

### WR-10: The CHANGELOG and FORK-DELTA item 16 state the Direct-path limit without the condition that the note now carries

**File:** `CHANGELOG.md:60`, `docs/FORK-DELTA.md:230`, against `docs/ROLLOUT-v1.7.0_gg6.md:48` and `docs/ROLLOUT-v1.7.0_gg6.md:249`
**Issue:** The fix of IN-02 added to row B8 and to the carried limits that the Direct path with `reportInsertedOffsets=true` commits nothing under a rename "except when `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`" (read from `DirectDeliveryStrategy.preCommit`, lines 37-44: the first branch returns Connect's own offsets). The CHANGELOG Known Issues bullet ("the Direct path with `reportInsertedOffsets=true` under a topic-changing SMT commits nothing") and FORK-DELTA item 16 ("still commits nothing") kept the unconditional wording, and the FORK-DELTA item is the text the next syncer reads. The three documents now list different limits for the same fix, which is the inconsistency IN-02 was about, moved to the other two documents. The CHANGELOG bullet also overstates the limit for a connector that sets `ignorePartitionsWhenBatching=true` without exactly-once.
**Fix:** Add "(unless `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned)" to the CHANGELOG bullet and to item 16, wording them as the note does. The CHANGELOG edit stays inside the new `1.7.0_gg6` section.

### WR-11: Row B1 carries the label `measured` for a half of its claim that its own How cell says was not repeated; D7's forwarding and B8's probe are the same case

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:41`, `docs/ROLLOUT-v1.7.0_gg6.md:124`, `docs/ROLLOUT-v1.7.0_gg6.md:48`
**Issue:** The note defines `measured` as "a command or a test ran, and the row names it" and `measured in research (not re-run)` as measured by the maintainers while preparing the release and "did not repeat it on the release tip". The IN-01 fix added to B1 that the unit test "also failed 8 of its tests on the tree before the fix, in a run recorded when the fix was written and not repeated for this note". That is the second definition, word for word, but the Label cell of B1 still reads the bare `measured`, so the "Before, `preCommit` returned offsets keyed by the renamed topic" half is labelled stronger than the definition allows (the recorded run exists, `tests=13 failures=8`, and I confirmed it; only the label is wrong). The same applies to the forwarding half of D7 (a stub-server probe recorded on the tree of the client bump and not repeated on the release tip; the sibling rows D6 and D8, which rest on the same kind of stub probe, carry the research label) and to the Direct-path and many-to-one half of B8 (a scratch probe the row itself calls not in the repository). A reader who relies on the four labels reads those halves as measured on this release.
**Fix:** Split the Label cells: B1 `after: measured; before: measured in research (not re-run)`, D7 `forwarding: measured in research (not re-run)` (keeping rejection, start-up failure and the V1 `custom_settings` claim as measured, which were run on the release tip), and B8 `Direct path and many-to-one rename: measured in research (not re-run)`. The check script accepts claim-qualified label cells, so `38-note-check.py rows` still passes; the counts it prints change and the FORK-DELTA or evidence text that quotes them, if any, changes with them.

### WR-12: FORK-DELTA item 22 says "The one release that exists" and goes stale when `v1.7.0_gg6` is published; the same file states the release process as a fact

**File:** `docs/FORK-DELTA.md:242`, `docs/FORK-DELTA.md:286`, `docs/ROLLOUT-v1.7.0_gg6.md:210`
**Issue:** The fix of WR-04 took the time-relative wording out of the gg5 note. Item 22 still reads "The one release that exists, `v1.7.0_gg5`", in a file that is part of the published tree: the moment the `v1.7.0_gg6` release object exists the sentence is false, and the file cannot be corrected afterwards without another release. Item 22 also records that the only release was made outside any workflow and that no publish-mode run has completed its publish step, yet the release-object sentences (`FORK-DELTA.md:286` "are created by the release workflow at the merged SHA", note line 210 "The release workflow creates the release and its tag") are written as what will happen. They describe the workflow's design, as round 1 accepted, but they read as fact and become wrong if the release is made by hand as `v1.7.0_gg5` was, which item 22 lists as a re-open trigger.
**Fix:** Item 22: "The one release that existed when this item was written, `v1.7.0_gg5`, was created outside any workflow run ...". Line 286 and the note's line 210: "is meant to be created by the release workflow ... (the first publish-mode run is item 22's open condition)", with no word about the outcome.

### IN-08: The new client scoping lists row D14 as also holding on client V1, but the piped streams behind it are used only on the V1 insert path

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:114`, `docs/ROLLOUT-v1.7.0_gg6.md:131`
**Issue:** The intro of section D (WR-02 fix) says rows D1 to D8, D10 and D11 describe client V2 and that "Rows D13 and D14 also hold on client V1", which reads as holding on both. D14's evidence, `ClientTimeoutPrefixPinUnitTest`, builds a `ClickHousePipedOutputStream` directly and names no connector client. In the connector the piped stream is created only in the three insert methods that start with `try (ClickHouseClient client = getClient())` (`ClickHouseWriter.java:1404-1416`, `1536-1541`, `1691-1696`, the client V1 path); the three client V2 insert methods (`1298`, `1574`, `1722`) call `client.insert(...)` and create none. So the row's prefixes are produced by the V1 path, and a connector on V2 is told, by the word "also", that the row is its own.
**Fix:** Say "Row D14 concerns the client V1 insert path (read from the code: `ClickHouseWriter` creates the piped streams only there); row D13 holds on both." If a V2 source of the same prefixes is known, name it instead.

---

round: 2 findings: 4

## Mandated confirmations (RLS-04)

- (1) fixes resolve their findings: confirmed for 15 of 16. WR-01, WR-02, WR-03, WR-04, WR-05, WR-06, WR-07, WR-09 and IN-01 to IN-07 hold as fixed (the V2 scoping, the start-up failure path, the one-release wording, the rollback bullets, the 3.6 attribution and the `javap` command, the item 12 owner, the new label and condition wording). WR-08 is half resolved by design: `FORK-DELTA` line 290 now says "on both client lines checked", but `docs/ROLLOUT-v1.7.0_gg1.md:242` still says "of any line" (deferred, not a new finding), so the two documents disagree until gg1 is edited. New defects of the fixes: WR-10 (the IN-02 condition reached only the note), WR-11 (the IN-01 clause contradicts B1's label) and IN-08 (the WR-02 scoping of D14). No wrong command, broken cross-reference, version, change set or rollback target was introduced: `v1.7.0_gg6`, `3a3a15a9`, 7 changed `src/main` files and client `0.11.0` agree in all four documents.
- (2) labels: not confirmed, see WR-11. The four labels are written verbatim in the note and `38-note-check.py` accepts all 29 rows (20 measured, 15 read from the code, 3 derived, 4 `measured in research (not re-run)`, split rows counted once per label). B1, D7 and B8 carry the bare `measured` for a half that was run earlier and not repeated; every other row is earned by its How cell. The CHANGELOG paraphrases the research label ("measured in the maintainers' research, not re-run") in its `zstd-jni` bullet, which is acceptable for a section that does not define the labels.
- (3) D7 against `src/main` and the evidence: confirmed. A connector runs client V2 for any `client_version` other than `V1` (`ClickHouseWriter.start`, `KeeperStateProvider`), the default is `V1`, `ClickHouseHelperClient.pingV2` logs "Unable to ping ClickHouse instance." and `ClickHouseWriter.start` returns false, so the task fails at start under every `errors.tolerance` before any `put`; V1 and default connectors are unaffected (their start-up ping goes through client V1, and `NetworkCompressionPinLiveTest.operatorOverrideReachesServerOnV1` pins `custom_settings` there). The measured run had `errors.tolerance` set to `none` and `all`, the only two values Connect accepts. The forwarding itself is recorded in the Phase 36 evidence (`custom_forwarded_on_0110: yes`, `custom_forwarded_on_096: no`) and no test in `src/test` asserts it, which the How cell now says. One caveat for operators: `client_version` set to an empty string selects client V2, which the wording "set to a value other than V1" covers.
- (4) rollback bullets: confirmed. The `lz4-java` bullet is accurate (`lz4Java` `1.12.0` at `3a3a15a9` and in the tree after the rollback block, `1.12.1` at HEAD) and does not claim that `1.12.0` is the affected version beyond "before the security release of that row". The frozen-offset bullet is labelled `derived from Kafka consumer-group semantics (not measured)`, cites rows B2 to B4 and claims no run. The rollback block, run unchanged in a throwaway worktree, printed `ok`, left `VERSION` `v1.7.0_gg5` and no copy of the note, and the worktree and the branch it created were removed; nothing was pushed. The rollback to `pre-v1.7.0_gg6` and to tag `v1.7.0_gg5` is one commit, `3a3a15a9`.
- (5) gg5 note: confirmed for lines 39 and 111, with a consequence for another file. The two lines agree with each other and with the release record (one release, `v1.7.0_gg5`, one asset, published 2026-10-28): line 39 dates the release, line 111 uses "as of 2026-10-28", and neither goes stale. The same staleness exists in `FORK-DELTA` item 22 ("The one release that exists"): WR-12.
- (6) `FORK-DELTA` open items 12 to 23: confirmed. Each of the 12 items carries "Owner: fork maintainer (the user)." and a re-open trigger (checked item by item), item 12 included, and the paragraph says "open items 12 to 23", which is the count (12 items). The statement of the allow-list is a plan and a condition (see 8).
- (7) identifiers: confirmed. The note and the new CHANGELOG section hold no host, connector, cluster or system name, no planning, requirement or review identifier and no `round:` text (`38-note-check.py forbidden` and `38-hygiene-check.py --strict` clean; the only `.com` hits are the two upstream issue URLs of the verbatim upstream entry). The ClickHouse server version `26.10.3.7` is a version, not a name.
- (8) `release object` and the allow-list: confirmed with one wording risk. The note and the CHANGELOG state no digest, tag commit or URL of the `v1.7.0_gg6` release object, and the `FORK-DELTA` paragraph now says the `.planning/scripts` allow-list "is written from the final tree before the merge and does not exist before that", a plan with a stated condition (the release workflows stop at the baseline step without it, as `pre-release.yaml` and `release.yaml` lines 97-103 show). The release-process sentences are worded as fact: WR-12.
- (9) CHANGELOG: confirmed. Every section from `# 1.7.0_gg5` down is byte-identical to the file at `3a3a15a9` (`diff` of the two tails prints nothing), and only the new `1.7.0_gg6` section changed in the fix commits (the `custom_` bullet, the `zstd-jni` bullet and the Known Issues bullet). Its `custom_` wording is consistent with the note (client V2 only, fails at start).

Totals across both rounds: 20 findings (12 warnings, 8 info, 0 critical): round 1 16, round 2 4. The checker's `rounds` command ends with FAIL only on the line "the last round still has findings and no round3_decision is recorded", which is its rule for a round with findings; every structural check (numbering, unique IDs, heading level, counts) passes.

_Round 2 reviewed: 2026-10-31_
_Reviewer: Claude (gsd-code-reviewer), fresh context_
