---
phase: 44-release-publication
reviewed: 2026-11-03T01:00:00Z
depth: standard
diff_base: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
files_reviewed: 5
files_reviewed_list:
  - docs/ROLLOUT-v1.7.0_gg7.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg6.md
  - docs/ROLLOUT-v1.7.0_gg1.md
findings:
  critical: 0
  warning: 6
  info: 14
  total: 20
status: issues_found
rounds: 4
---

## Round 1

reviewer_agent_id: a37f170dc8cc7a801

### WR-01: Row T5 attributes 4000 committed rows to one failed insert; the evidence counts both runs

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:66`
**Issue:** T5 says "A second run with a connector-shaped insert and a 3000 ms value failed at 3030 ms, and 4000 rows were committed." The source of the second run (`41-COMMAND-EVIDENCE.md` E-06, `progress_probe_rows_committed: 4000`) states the count was taken "after both runs": the 3000 ms insert that failed and the 8000 ms control that succeeded (`written_rows=2000`). The failed insert alone therefore left about 2000 rows (4000 minus the control's 2000, an inference), not 4000. The row is labelled `measured`, and the figure as written is twice what the failed insert committed. The same wording reached the E-09 claim; FORK-DELTA item 14 words it safely ("the timed-out rows were committed").
**Fix:** Replace the sentence with: "A second run with a connector-shaped insert and a 3000 ms value failed at 3030 ms; after it and an 8000 ms control that wrote 2000 rows, the table held 4000 rows, so the failed insert's rows were committed too." Keep the label `measured`.

### WR-02: Rows R4 and R1 are labelled `measured` although part of each claim is read from the code or derived

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:107` and `docs/ROLLOUT-v1.7.0_gg7.md:90`
**Issue:** The note's own rule (line 22) says a row with claims of different strength labels each claim. R2, R3, R7 and F1 to F3 do this; R1 and R4 do not.
R4: the tests (`RetriableExceptionPassThroughUnitTest`, `HandleExceptionCauseCycleUnitTest`) assert that `Utils.handleException` throws a `RetriableException`. They do not run Kafka Connect, so "Under `all`, a batch that went to the dead letter queue is now redelivered with no retry budget" is a statement about the Connect runtime, not measured. The gg6 row C2 labels the same redelivery `read from the code`. The list of writer sites (`awaitInsertResponse`, `refreshTableAfterDDL`) is also read from the code.
R1: "Only the in-memory inserted-offset map reads it" (`StateRecord.getOriginalTopic` read only in `BaseStateProviderImpl.onStateUpdate`) is read from the code; the row's own How-known cell says so. I confirmed the claim is true (`grep getOriginalTopic src/main`), but it is not measured.
**Fix:** R4 label: `rule and its ordering, the 30 of 38 red cases: measured; redelivery by Kafka Connect with no retry budget, writer sites: read from the code`. R1 label: `one-line change, the four red cases, reporting and clearing: measured; only the in-memory map reads the topic: read from the code`.

### WR-03: R1, section C and the CHANGELOG omit the configuration where the R1 effect does not occur (`exactlyOnce=false` with `ignorePartitionsWhenBatching=true`)

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:90` (Applies-when), `docs/ROLLOUT-v1.7.0_gg7.md:86`, `CHANGELOG.md:33-36`
**Issue:** `DirectDeliveryStrategy.preCommit` returns Connect's own `currentOffsets` when `!exactlyOnce && ignorePartitionsWhenBatching`, before it looks at `reportInsertedOffsets`, and `Processing.doLogic` takes its `doInsert(records)` branch there without touching the state provider. In that configuration the inserted-offset map is never filled, so the one-line change has no effect, whatever the SMT. The published gg6 note names this exception ("unless `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`" in its row B8 and its third known limit). The gg7 Applies-when `reportInsertedOffsets=true, bufferCount 0, injective topic-renaming SMT` and the CHANGELOG bullet state the effect without it, so an operator with that combination is told to expect offsets under the consumer's topic and will see none. The supersession-table row for the third gg6 bullet also drops the parenthesis.
**Fix:** Append to the R1 Applies-when: `, not exactlyOnce=false with ignorePartitionsWhenBatching=true (that path returns Connect's own offsets and is unchanged)`; add the same parenthesis to the C section What-to-do and to the CHANGELOG bullet ("... `bufferCount` 0, unless `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned, now reports ...").

### WR-04: The headline "every helper client ... is now closed" is false by the document's own residual row

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:5`, `CHANGELOG.md:4` (intro), `docs/FORK-DELTA.md` item 13 heading ("A stopping task closes every helper client it created (closed)")
**Issue:** Overview point 1 of the note says "Every helper client the connector opens is now closed when a task stops". Row F5 lists clients that stay open: a `KeeperStateProvider` constructor that fails on client V1 (verified: `init()` calls `chc.queryV1(...)` with no `try`, and `ClickHouseSinkTask.start` assigns `proxySinkTask` only after the constructor returns, so `stop()` has nothing to stop), a `ClickHouseHelperClient` constructor that fails after its client V2 instance exists, a second `start()` on one writer, and a table refresh still running after the five-second wait. The CHANGELOG intro ("the closing of every helper client a task creates") and the FORK-DELTA item 13 title carry the same universal. An operator who reads only the summary concludes no leak path remains.
**Fix:** Say "the helper clients that a started task holds are closed on stop and on a failed start; row F5 lists the paths that still leave one open". Use the same wording in the CHANGELOG intro and rename FORK-DELTA item 13 to "A stopping task closes the helper clients it holds (closed, residuals open)".

### IN-01: "nothing validates the codes" reads as a contradiction of gg6 row C1

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:125`, `CHANGELOG.md:46`, `docs/FORK-DELTA.md` item 24 ("Nothing validates, rejects or budgets the codes")
**Issue:** The published gg6 row C1 says parsing "rejects a value that is not an integer or is zero or below", and `ClickHouseSinkConfig` parses and validates the list (`parseErrorCodes`). The new text means "nothing checks a code against what the server can return", but an operator comparing the two notes reads a contradiction.
**Fix:** Write "the connector does not judge whether a code is transient beyond the vetted set, and does not budget retries" (the format check of row C1 still applies).

### IN-02: "Start-up still succeeds" next to "also when ClickHouse is down" can be read as start-up succeeding without a server

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:125`, `CHANGELOG.md:46`
**Issue:** The WARN is logged before the connection step, but with ClickHouse down `ProxySinkTask.start` still throws "Connection to ClickHouse is not active." Only the WARN itself never fails start-up.
**Fix:** "The WARN never fails the start-up; with ClickHouse down the task still fails at the connection step, after the WARN."

### IN-03: The Phase 43 edit of the published gg6 note introduces the planning identifier "Phase 39" into an operator document

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:148` (row D7, How known)
**Issue:** `git show 72e8ccff:docs/ROLLOUT-v1.7.0_gg6.md | grep -c "Phase [0-9]"` is 0; the edit adds "since Phase 39". An operator cannot resolve it, and the gg7 note and CHANGELOG are held free of planning identifiers.
**Fix:** "since `v1.7.0_gg7`, `ClientEndpointAndOptionsPinUnitTest.customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` pins it" (the test is in the gg7 tree and not in the gg6 tag).

### IN-04: The gg7 note keeps `HEAD`-relative commands where FORK-DELTA moved to tag pairs

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:3`, `:50` (F3), `:91` (R2), `:190` (D5)
**Issue:** `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` and the `git diff v1.7.0_gg6 HEAD` checks give the quoted result only on the release tip; on any later `custom_build` commit they do not reproduce. Phase 44 removed the same pattern from FORK-DELTA measured-figure paragraphs. The note scopes line 3 with "on the release tip" but F3, R2 and D5 do not. The gg6 note used the same style, so this is a consistency point, not a regression.
**Fix:** Name `v1.7.0_gg7` in the commands, as in FORK-DELTA ("measured with `HEAD` in place of the tag on the release tip, before the release workflow creates the tag"), or add that the figures hold on the release tip only.

### IN-05: The note uses `async=false` and `async=true` without saying where an operator sets it

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:35`, `:58`, rows T2, T3, T5, T14
**Issue:** Nothing in `src/main` sets a client `async` option; the only route is an operator key in `jdbcConnectionProperties`, which `createClientV2` forwards to the client. The note says only that `async=false` "is the client's default" and never names the route, and `async` is easily confused with the ClickHouse `async_insert` setting (not touched by this release).
**Fix:** Add one sentence to the section B lead: "`async` is a client V2 option that only an operator-supplied key in `jdbcConnectionProperties` can set; the connector sets none, and it is not the ClickHouse `async_insert` setting."

### IN-06: The rollback list says an insert can "wait for ever"; the evidence supports "no connector-side bound"

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:181`
**Issue:** Row T2 records the old behaviour as "blocked" with the probe still blocked at an 8-second guard. "Wait for ever" follows from the default `socket_timeout` of 0 (read from the code), not from any observation.
**Fix:** "an insert has again no bound from the connector on a server that stops answering (the client's default `socket_timeout` is 0)".

### IN-07: CHANGELOG says "client V1 inserts are unchanged" while the per-batch helper client of client V1 inserts is now closed

**File:** `CHANGELOG.md:14-15`
**Issue:** The sentence is about the new timeout, but `ClickHouseWriter.getMutationRequest` (a client V1 insert path) changed in this release (the helper client is closed right after the node is read), and the Bug Fixes bullet says so.
**Fix:** "no timeout is added to client V1 inserts".

### IN-08: The CHANGELOG and FORK-DELTA state the gg7 allow-list in the present tense, and the file does not exist yet

**File:** `CHANGELOG.md:68`, `docs/FORK-DELTA.md` (gg7 paragraph, "compare for gg7 is ... `confluent-archive-allowlist-v1.7.0_gg7.txt`, written last")
**Issue:** `ls .planning/scripts` at this commit lists allow-lists up to `v1.7.0_gg6`. `release.yaml` stops at the baseline step when `.planning/scripts/confluent-archive-allowlist-${RELEASE_VERSION}.txt` is absent. The workflow itself was not changed by this release, so the "Build and CI" bullet restates a gg6 behaviour as a gg7 change.
**Fix:** Drop the bullet or word it as unchanged ("the release workflows keep comparing ..."); before the push gate, confirm the file exists and its first line names `.github/scripts/compare_confluent_archives.py` (handoff item 5 of the Phase 43 ledger).

### IN-09: Row T6 says "the client sends `send_progress_in_http_headers=1`"; it is a connector default that an operator can change

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:67`
**Issue:** `ClickHouseSinkConfig` adds `send_progress_in_http_headers=1` with `addClickHouseSetting(..., false)` (line 303), and `ClickHouseWriter` sends every entry of `getClickhouseSettings()` on each insert; a value in `clickhouseSettings` wins. The row attributes the setting to the client and omits that the operator controls the exposure the row describes. FORK-DELTA item 14 repeats "a default client setting".
**Fix:** "The connector adds `send_progress_in_http_headers=1` as a default ClickHouse setting on its insert requests (`ClickHouseSinkConfig`); a value in `clickhouseSettings` replaces it." Keep the label `derived (mechanism, not measured)`.

### IN-10: The Rollback intro promises the release and the pre-tag, which do not exist yet

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:154`
**Issue:** "The release workflow creates the release and its tag at the merged commit and attaches the archive" and "The tag is published together with this release" are future-tense promises. Neither the `v1.7.0_gg7` release nor `pre-v1.7.0_gg7` exists on origin (`git ls-remote --tags origin 'pre-v1.7.0_gg7*' 'v1.7.0_gg7*'` prints nothing; the pre-tag exists locally only). Step 1 and step 2 fail until the user pushes that tag. The statements are true only if the publish handoff completes exactly as planned.
**Fix:** Keep the sentences but state the dependency: "Step 2 needs the tag `pre-v1.7.0_gg7` on origin; if `git fetch --tags origin` does not bring it, stop and ask the maintainer to publish it."

round: 1 findings: 14

## Mandated confirmations (RLS-03)

- (a) 33 rows: counted F1 to F5, T1 to T15, R1 to R8 and D1 to D5 = 33 labelled rows. The 13 E-09 claims stand as T1 to T13 in E-09's order, with the same or weaker label (T7 and T8 split between `derived` and `read from the code`; T12 and T13 split as the evidence did; T6 is `derived (mechanism, not measured)` by design). Rows stronger than their evidence: T5 (figure of 4000 rows, WR-01), R4 and R1 (mixed claims under a single `measured`, WR-02). No other row is stronger than its source.
- (b) Verified: 8 files, 194 insertions, 22 deletions of `git diff --shortstat v1.7.0_gg6 HEAD -- src/main`; the archive figures against E-05 (7933 and 7932 entries, 13558633 and 13564144 bytes, 120 differences = 101 + 1 + 3 + 14 + 1, 18 libraries, `gson` 2.14.0 to 2.15.0, 59 bytes and six classes against the asset); FORK-DELTA raw numbers recomputed with `git diff --shortstat` (345/42439/1221, 43/3183/294, 34/2826/112, per-directory 18/2470/28, 2/155/55, 5/6/6, 1/1/1, and the directory sums equal the 34/2826/112 total); the import-only recount reproduced with a script on the file's method (v1.7.0 to HEAD: 63, 30, 14, 19, 10 new; at v1.7.0_gg6: 33, 14, 16, 10 new); T2 timings (304 to 432 ms), T9 (1506 ms), T13 (407 and 403 ms), T1 and T12 (30140 ms) against `41-RESEARCH.md`; R4's 30 of 38 and R1's 4 of 6 against the Phase 42 evidence; release 430511927, asset size and SHA-256, run 41298074815, PRs 10 and 17 closed unmerged on 2026-11-01 by the GitHub REST API. One count does not hold as written (T5, WR-01).
- (c) Applies-when: the client V2 rows (T2 to T11, T13, T14) match the three `setInsertSocketTimeoutV2` call sites and the V1 `future.get()` sites; F2 is client V1 (`getMutationRequest`); F3 and R2 are `exactlyOnce=true`; R7 and R8 hold for any connector that sets `additionalRetriableErrorCodes`. T7 is right that `exactlyOnce=false` sends the token on the default path (`Processing.doInsert(records, range)`) and not under `ignorePartitionsWhenBatching=true`. R1 is wrong for `exactlyOnce=false` with `ignorePartitionsWhenBatching=true` (WR-03). `bufferCount 0` for R1 is right: the buffered strategies keep their own offsets.
- (d) Rollback: the steps run in order (clean tip, branch, `read-tree -u --reset`, commit, equality check, redeploy, publish) and match the E-05 rehearsal. Target `pre-v1.7.0_gg7` is a local lightweight tag on `72e8ccff`, the same commit as `v1.7.0_gg6`, and absent on origin (IN-10). The `gradlew.bat` omission is correct as measured: the blob at `72e8ccff` has no CRLF lines (it had 112 at the gg5 commit, which is why the gg6 note needs that sentence), so the note carries none and its "git 2.50.1 only" caveat stands. The gg6 asset facts (13558692 bytes, SHA-256, 59 bytes, six classes, two examined) match E-05 and the API. "Wait for ever" is stronger than the evidence (IN-06).
- (e) No fact about the `v1.7.0_gg7` release object appears: no asset name followed by a size, no digest other than the gg6 asset's, no release URL. Promises of the release and its tag appear as future-tense sentences in the Rollback intro, the CHANGELOG intro and the FORK-DELTA gg7 paragraph (IN-10, IN-08); I treat them as handoff-dependent statements, not as facts about an existing release object.
- (f) No advice that empties or destroys data: no TRUNCATE, DELETE, DROP, force-push or `reset --hard`. The destructive effect of `git read-tree -u --reset` on uncommitted work is stated before the block, and the note says "Never force-push".
- (g) `FUT-05 stays open.` stands in section F (line 130), and FUT-05 appears nowhere else in the gg7 note (`grep -n "FUT-0" docs/ROLLOUT-v1.7.0_gg7.md` prints only that line). Its presence in gg6 row C2 and in the gg6 correction notice is deliberate and not reported.
- (h) The WARN text in section F equals `UNVETTED_RETRIABLE_CODES_WARNING` of `ClickHouseSinkTask.java`, joined from its four literals, character for character. `{}` is filled with a `TreeSet`, so the list is ascending and prints in square brackets. The vetted set is the 15 codes of `Utils.DEFAULT_RETRIABLE_ERROR_CODES` plus 216; the check runs in `start` before the proxy task is built.
- (i) FORK-DELTA: the `Processing.java` row (one-line difference at `doLogic` line 137, confirmed by the diff) and the `KeyToValue.java` row (byte-identical, split into its own row) are correct, and the recounts 30/33/14/19/29 and 33/14/26 at gg6 are correct. The tag-pair commands are well formed; the remaining `HEAD` mentions in the measured-figure paragraphs are historical descriptions with a tag command beside them, and I do not report them. The gg7 paragraphs, items 12 (PR 10 closed 2026-11-01), 19, 22 (run 41298074815, release 430511927, asset 13558692) and 23 (PR 17 closed 2026-11-01) match the GitHub REST API and the code. The one headline overstatement is item 13 (WR-04). Items 14, 16, 17 and 24 match the code, apart from the IN-01 wording and the IN-09 attribution.
- (j) CHANGELOG `1.7.0_gg7`: every bullet matches a code or behaviour change in `git diff 72e8ccff HEAD -- src/main`, apart from the omitted configuration in the reporting bullet (WR-03), the "unchanged" wording (IN-07) and the headline universal (WR-04). The heading `# 1.7.0_gg7, 2026-11-02` has the same date format as `# 1.7.0_gg6, 2026-10-31`; E-03 records that the orchestrator compares the date with the UTC date at the merge, and that check stays open. The "Build and CI" allow-list bullet describes a gg6 behaviour and a file that does not exist yet (IN-08). No sentence promises a release beyond the intro pointer to the rollout note.
- The F-2 operator override, `socket_timeout`: rows T9, T10 and T13 confirmed against the code and the Phase 41 evidence. `ClickHouseHelperClient.operatorNamesSocketTimeout` matches the exact key `socket_timeout` (`ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey()`) in the pairs parsed from `jdbcConnectionProperties`, with or without a leading `?`, and any value, `0` included. When the key is named, `setInsertSocketTimeoutV2` returns without setting the per-insert value, so the operator's client-level value bounds every request and an explicit `socket_timeout=0` keeps inserts unbounded. Otherwise it sets the per-insert value (clamped to the int range, ignored when not positive) at the three client V2 insert sites of `ClickHouseWriter`. The measurements match `41-RESEARCH.md`: 407 ms and 403 ms with and without the `?` (T13), 1506 ms for `socket_timeout=1500` with an insert timeout of 300 ms (T9), and the option is absent from the request query string (T4). T10's minimum 100 is `ConfigDef.Range.atLeast(100)`. One gap remains: the exact-key match is case-sensitive and the note does not say so (`SOCKET_TIMEOUT=5` is ignored per Probe 2), a point inside T9 and not a falsehood.

---

_Reviewed: 2026-11-02T22:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

## Round 2

reviewer_agent_id: a752798ab528d2b1f

Scope: `git diff e02adb0c7ab638b171e6768a525da537dc3a38fd HEAD -- docs CHANGELOG.md` (CHANGELOG, FORK-DELTA and the gg7 note changed; the gg6 and gg1 notes did not). Each round-1 fix was re-read against `src/main` and the Phase 41 and 44 evidence. Fixes confirmed correct: WR-01 (T5 split; the 4000 count and the 2000 control figure match E-06), WR-02 (R1 and R4 labels), WR-03 (exclusion verified in `DirectDeliveryStrategy.preCommit` and `Processing.doLogic`), IN-01 to IN-02, IN-04 to IN-10. WR-04 is fixed in the note, the CHANGELOG intro and the FORK-DELTA item 13 title, but one FORK-DELTA row still carries the universal (WR-05). IN-03 stays deferred to the user (published gg6 note) and is not re-reported.

### WR-05: FORK-DELTA writer row still says the writer "releases each one" of "every helper client"

**File:** `docs/FORK-DELTA.md:39` (the `ClickHouseWriter` row of the table)
**Issue:** The row opens "`ClickHouseWriter` builds every helper client through the protected factory `createHelperClient(builder)` and releases each one". The round-1 fix for WR-04 retitled item 13 and removed the universal from the note and the CHANGELOG, but this row keeps it. It contradicts item 13 of the same file: a second `start()` on one writer assigns a new `chc` without closing the earlier one (`ClickHouseWriter.startInternal` does `chc = createHelperClient(...)` with no prior close), and after the five-second wait `stop()` closes `chc` while a table refresh may still be running on it. A reader of the table sees no leak path.
**Fix:** Write "builds every helper client through the protected factory `createHelperClient(builder)` and releases the ones named below (the residuals are in Known cost 13)", or end the first clause with "and releases each one on the paths below; a second `start()` on one writer is not one of them (Known cost 13)".

### IN-11: Section C "What to do" negates the exclusion in a way that can be read as two separate exclusions

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:93`
**Issue:** "nothing, unless you enabled `reportInsertedOffsets=true` under a topic-renaming SMT and do not run `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned and nothing changes". "do not run A and B" can be read as "run neither", so an operator with `exactlyOnce=false` alone (the default `ignorePartitionsWhenBatching` keeps the inserted-offset map filled) may think this section does not concern them. It also omits `bufferCount` 0, which the section lead and the Applies-when of R1 carry. The code excludes only the combination (`!isExactlyOnce() && isIgnorePartitionsWhenBatching()` in `DirectDeliveryStrategy.preCommit` and `Processing.doLogic`).
**Fix:** "nothing, unless you enabled `reportInsertedOffsets=true` with `bufferCount` 0 under a topic-renaming SMT. The one combination where nothing changes is `exactlyOnce=false` together with `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned; in every other case expect offsets under the consumer's topic, and read section D if the rename or the SMT is of another kind."

round: 2 findings: 2

## Mandated confirmations (RLS-03)

- (a) 33 rows: still F1 to F5, T1 to T15, R1 to R8 and D1 to D5 = 33 labelled rows. The 13 E-09 claims remain T1 to T13 in order. The T5 label split (`first run, the failure and the commit: measured; second run, the rows the failed insert committed: derived`) is not stronger than E-06: the table held 4000 rows after the failed 3000 ms run and an 8000 ms control run that wrote 2000 (the difference, 2000, is correctly marked derived; that the table began empty is an assumption the evidence does not state, but the label already carries it). Row T6 stays `derived (mechanism, not measured)` by design and its new wording (a connector default in `ClickHouseSinkConfig.addClickHouseSetting(..., false)` that a `clickhouseSettings` value replaces) matches the code. R1 and R4 now label each claim and no row is stronger than its source.
- (b) Verified again: `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` reads 8 files, 194 insertions, 22 deletions; FORK-DELTA raw numbers recomputed (`v1.7.0` to `HEAD`: 345/42439/1221 for the wide scope and 43/3183/294 for `src/main`), unchanged because no `src`, `gradle`, `build` or `.github` file changed since round 1 (`git diff --stat e02adb0c HEAD` lists only docs and the evidence file). The archive figures were not touched by the fixes. The commands that name `v1.7.0_gg7` in the note (line 3, F3, R2, D5) give the quoted results when run with `HEAD` in place of the tag: the `Processing.java` diff is the one `originalTopic()` line at 137 and the `KeeperStateProvider.java` diff leaves `getStateRecord` and `setStateRecord` untouched. The line-3 sentence that every such command was measured that way is accurate.
- (c) Applies-when: the `exactlyOnce=false` plus `ignorePartitionsWhenBatching=true` exclusion in R1, section C, the CHANGELOG bullet, the label legend (line 28) and the gg6-supersession row is right against the code (`preCommit` returns Connect's offsets before it reads `reportInsertedOffsets`, `doLogic` calls `doInsert(records)` and keeps no state). `exactlyOnce=true` with `ignorePartitionsWhenBatching=true` is correctly not excluded. The client V2 rows still match the three `setInsertSocketTimeoutV2` sites; the new `async` sentence is right (no `src/main` code sets `async`, `createClientV2` forwards the operator's keys, and the `wait_for_async_insert` in `KeeperStateProvider` is a different ClickHouse setting). The only wording defect is IN-11.
- (d) Rollback: steps 1 to 4 run in order and `git read-tree -u --reset` does overwrite an untracked file at a path the tag contains (reproduced in a scratch repository), so the added warning is correct. Target `pre-v1.7.0_gg7` is a local tag on `72e8ccff` (same commit as `v1.7.0_gg6`); the new intro sentence states that step 2 needs it on origin and what to do if `git fetch --tags origin` does not bring it. `pre-v1.7.0_gg7` is absent on origin at this commit, which the sentence now covers. No `gradlew.bat` sentence is carried, as measured (`gradlew_bat_trap: no`). The gg6 asset facts (13558692 bytes, SHA-256 `e745e7d5...c2af`, 59 bytes, six classes) match E-05. "Wait for ever" is gone; the replacement (client default `socket_timeout` of 0) matches Probe 2. One unchanged phrase in step 1, "the merge of the release tip printed nothing", is loosely worded (E-05 records `git status --porcelain` clean after the merge, not the output of `git merge`); not reported as a finding.
- (e) No fact about the `release object` of `v1.7.0_gg7`: the note, the CHANGELOG and FORK-DELTA state no release URL, asset, size or digest for it. The sentences that the tag or the release "is created" or "is published" are conditional process statements, as in round 1, and the CHANGELOG makes no promise beyond the pointer to the rollout note.
- (f) No advice that empties or destroys data: no TRUNCATE, DELETE, DROP, force-push or `reset --hard`; the discard effect of `read-tree -u --reset` is stated before the block and "Never force-push" stands.
- (g) `FUT-05 stays open.` appears once, in section F (line 130); `grep -n "FUT-0" docs/ROLLOUT-v1.7.0_gg7.md` prints only that line.
- (h) The WARN text in section F equals `UNVETTED_RETRIABLE_CODES_WARNING` in `ClickHouseSinkTask.java` joined from its four literals, character for character; the check runs in `start` after config parsing and before the proxy task is built, the vetted set is the 15 codes of `Utils.DEFAULT_RETRIABLE_ERROR_CODES` plus 216, and the format check is `ClickHouseSinkConfig.parseErrorCodes` (positive integers), so the new "format check still applies" wording in R7, the CHANGELOG and FORK-DELTA item 24 is accurate.
- (i) FORK-DELTA: the `Processing.java` row (one line at 137) and the `KeyToValue.java` row (byte-identical) are unchanged and correct; the recounts and the tag-pair commands are unchanged since round 1; the two remaining `HEAD` descriptions in the measured-figure paragraph (line 147) describe how the figures were once measured and sit beside `v1.7.0_gg7` commands, so they are history. Items 12, 19, 22, 23 are unchanged. The rewritten item 13 matches the code in each residual (checked `KeeperStateProvider.init`, `ClickHouseSinkTask.start` and `stop`, the strategies' `stop()` bodies, `ClickHouseWriter.startInternal`), item 14's T5 wording matches E-06, item 24's format-check clause matches `parseErrorCodes`, and the gg7 paragraph's allow-list sentence ("generated in the release process") no longer states the file exists. One false present-tense universal remains in the writer table row (WR-05).
- (j) CHANGELOG `1.7.0_gg7`: each bullet matches the code or behaviour change in `git diff v1.7.0_gg6 HEAD -- src/main .github`; the intro and the Bug Fixes bullet now carry the residuals and the exclusion; "no timeout is added to client V1 inserts" is accurate; the Build and CI statement that the release workflows change only by the `setup-gradle` pin holds for `release.yaml` and `pre-release.yaml` (the only other `.github` change is a comment in `increment-version.yml`). The heading `# 1.7.0_gg7, 2026-11-02` has the same format as `# 1.7.0_gg6, 2026-10-31`; the date-against-UTC-at-merge check stays with the orchestrator. No promise of a release beyond the pointer to the rollout note.
- Specific tokens: `socket_timeout` rows T9, T10 and T13 and the CHANGELOG sentence are unchanged and still match `ClickHouseHelperClient`; every `client V1` insert claim is still scoped to client V1 (default `client_version` is V1); the T5 and R1 label splits, the `exactlyOnce=false` plus `ignorePartitionsWhenBatching=true` exclusion and the `pre-v1.7.0_gg7` target on `72e8ccff` are consistent between the note, the CHANGELOG and FORK-DELTA.

---

_Reviewed: 2026-11-02T23:30:00Z_
_Reviewer: Claude (gsd-code-reviewer), round 2_
_Depth: standard_

## Round 3

reviewer_agent_id: a401c814e0b118957

Scope: `git diff 8e226d41 HEAD -- docs CHANGELOG.md`, three commits that touch two files (`docs/FORK-DELTA.md` one row, `docs/ROLLOUT-v1.7.0_gg7.md` section C "What to do" and rollback step 1; CHANGELOG, the gg6 note and the gg1 note did not change; `git diff --stat 8e226d41 HEAD -- src` lists nothing). Each change was read against `DirectDeliveryStrategy.preCommit`, `Processing.doLogic`, `ClickHouseWriter.start/startInternal/stop/closeHelperClient/getMutationRequest`, `ClickHouseHelperClient`'s constructor, FORK-DELTA Known cost 13, note row F5 and E-05 (with `/tmp/gsd44/exec/rehearse.sh`). Confirmed correct: IN-11 (the exclusion is the combination only; `exactlyOnce=true` with `ignorePartitionsWhenBatching=true`, and `exactlyOnce=false` with the default `ignorePartitionsWhenBatching=false`, both run the state machine and both reach the inserted-offset snapshot; `bufferCount` 0 is now stated); rollback step 1 (the dry run did merge the committed tip into a detached checkout of `72e8ccff` and did not run `git fetch` or `--ff-only`; `d0_status_before_block: clean` is the measured figure; reproduced the plain merge, `read-tree`, commit and equality check in a scratch clone and `git status --porcelain` printed nothing after each). IN-03 stays deferred to the user and is not re-reported.

### WR-06: FORK-DELTA writer row says "two residuals stay open (Known cost 13)", but Known cost 13 and note row F5 list a third that sits on the writer's own factory

**File:** `docs/FORK-DELTA.md:39` (the `ClickHouseWriter` row of the table)
**Issue:** The WR-05 fix reads "closes the ones a started task holds, on the paths named here; two residuals stay open (Known cost 13): a second `start()` on one writer ... and the 5 s wait of `stop()` ...". Known cost 13 lists five residuals and the note's row F5 lists the same five. One of the three that Known cost 13 does not attribute to the keeper or the task is on this row's own code: `ClickHouseHelperClient`'s constructor assigns `this.client = createClientV2()` (line 110) before `this.server = createClientV1()` (line 111), so a throw from the second leaves a V2 client open with no object to close; `ClickHouseWriter` reaches it at every `createHelperClient(builder)` call (`startInternal` and the per-batch call in `getMutationRequest`). The row says it "builds every helper client through the protected factory", so a reader who counts residuals from the row finds two, from item 13 five, and cannot tell which of the other three touch the writer. "Two" is also read by `grep` as the total for the file.
**Fix:** Say "two residuals of the writer's own paths stay open ... and a helper client whose constructor throws after it built its V2 client is never held, so nothing closes it; the keeper and task residuals are in Known cost 13". Shortest safe form: replace "two residuals stay open (Known cost 13): a second ... and the 5 s wait ..." with "the residuals of Known cost 13 stay open, among them a second `start()` on one writer, which does not close the client the first one built, the 5 s wait of `stop()`, which can end with a table refresh still running on a closed client, and a `ClickHouseHelperClient` constructor that throws after it built its V2 client".

### IN-12: The note calls the rehearsed commit "the release tip"; E-05 merged an intermediate note commit

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:154` (rollback step 1), also the paragraph after step 4 and row D5 (`dry-run merge of the release tip`)
**Issue:** The reworded step 1 now says "on the dry run below, the merge of the release tip left a clean tree ... (measured; the dry run merges the tip into a detached checkout of the `v1.7.0_gg6` commit ...)". E-05 records `rehearsal_tip: 7ef33b4feae0759c17a77e171e4d53e36b924b3a`, `note_commit_1` of plan 44-02, which is an early commit of the note. The release tip (the merged `sync/v1.7.0` head) has more commits and does not exist yet, and no later evidence block repeats the rehearsal. The clean-tree and block results do not depend on the later content (checked: a plain merge of the current `71fe00e9` onto `72e8ccff` also leaves `git status --porcelain` empty, and the block prints `ok`), so this is a wording point, but "release tip" asserts a rehearsal of the thing that will be published. The same wording was accepted in rounds 1 and 2 for the paragraph after step 4; the new step 1 sentence adds a third use.
**Fix:** Write "a dry-run merge of the committed tip of the branch (an earlier commit than the release tip)" in step 1, the paragraph after step 4 and row D5, or re-run `rehearse.sh` on the final tip at the publication step and record it as a new E-entry.

round: 3 findings: 2

## Mandated confirmations (RLS-03)

- (a) 33 rows: unchanged since round 2 in count and labels, F1 to F5, T1 to T15, R1 to R8 and D1 to D5 = 33 labelled rows (`grep -cE '^\| T[0-9]+ '` prints 15). R1's text was not edited by the three fixes; only its "What to do" paragraph was. The 13 E-09 claims remain T1 to T13 in order, T6 stays `derived (mechanism, not measured)` by design, and no row is stronger than its source.
- (b) The commit set touches no `src`, `gradle`, `build` or archive figure. `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` still reads 8 files, 194 insertions, 22 deletions (E-05 `src_main_shortstat`); the archive figures (7933 and 7932 entries, 13558633 and 13564144 bytes, 120 differences, 59 bytes and six classes against the asset, 13558692 bytes) still match E-05. The new FORK-DELTA clause "the 5 s wait" matches `TIMEOUT_FOR_SHUTDOWN = 5` seconds; the new "a second `start()` on one writer does not close the client the first one built" matches `startInternal`, which assigns `chc` with no prior close. The row's count "two residuals" is WR-06.
- (c) Applies-when: section C "What to do" now matches `DirectDeliveryStrategy.preCommit` (the first branch returns Connect's offsets only for `!isExactlyOnce() && isIgnorePartitionsWhenBatching()`) and `Processing.doLogic` (the same condition, `doInsert(records)` with no state), the default of `ignorePartitionsWhenBatching` is `false`, and the bullet "either of the two alone does not exclude you" holds for both singles. `bufferCount` 0 and `reportInsertedOffsets=true` are carried. Every `client V1` insert claim stays scoped to client V1 (the default `client_version` is V1; the F-2 fix is client V2 only).
- (d) Rollback: steps 1 to 4 run in order and the step 1 parenthetical matches `rehearse.sh` (`git merge --no-ff` into a detached worktree at `72e8ccff`, no fetch, no fast-forward merge; the tree before the block printed `clean`). Target `pre-v1.7.0_gg7` is a local tag on `72e8ccff`, equal to `v1.7.0_gg6` (`git rev-parse` prints the same SHA), and the intro still tells the operator what to do if origin lacks it. No `gradlew.bat` sentence is carried, as measured (`gradlew_bat_trap: no`, and a plain merge, `read-tree` and commit in a scratch clone leave no ` M gradlew.bat`). The gg6 asset facts match E-05. "Release tip" wording: IN-12.
- (e) No fact about the `release object` of `v1.7.0_gg7` was added by the three commits; the note, the CHANGELOG and FORK-DELTA still carry no release URL, asset, size or digest for it, and the conditional process sentences are unchanged.
- (f) No advice that empties or destroys data was added; the step 1 text keeps the warning that `git read-tree -u --reset` discards uncommitted changes, and no TRUNCATE, DELETE, DROP or force-push appears.
- (g) `FUT-05 stays open.` is the only `FUT-05` line of the gg7 note (line 130; `grep -c FUT-05` prints 1); the changed lines add none.
- (h) The WARN text was not touched; it still equals `UNVETTED_RETRIABLE_CODES_WARNING` in `ClickHouseSinkTask.java` as confirmed in rounds 1 and 2 (no `src` change since).
- (i) FORK-DELTA: the one changed row is checked above (WR-06); the `Processing.java` row (one line at `doLogic` line 137, `String originalTopic = record.getSinkRecord().originalTopic();`) and the `KeyToValue.java` row, the recounts, the tag-pair commands, the gg7 paragraphs and items 12, 19, 22 and 23 are unchanged by the three commits and stand as confirmed in round 2. The new clause "closes the ones a started task holds" is consistent with item 13's title and with row F5.
- (j) CHANGELOG `1.7.0_gg7`: not changed since round 2; each bullet still matches the code or behaviour change and the heading date format is unchanged; no promise of a release beyond the rollout-note pointer.
- Specific tokens: `socket_timeout` rows T9, T10 and T13 were not changed and still match `ClickHouseHelperClient`; the `exactlyOnce=false` plus `ignorePartitionsWhenBatching=true` text in row R1, section C and the gg6-supersession row is consistent after the IN-11 rewrite; `FUT-05 stays open` stands; `pre-v1.7.0_gg7` and `72e8ccff` are consistent in the intro, step 2 and row D5.

---

_Reviewed: 2026-11-03T00:30:00Z_
_Reviewer: Claude (gsd-code-reviewer), round 3_
_Depth: standard_

## Round 4

reviewer_agent_id: a32f740ad20019e8a

Scope: `git diff 156fd283 HEAD -- docs CHANGELOG.md`, two commits that touch two files (`docs/FORK-DELTA.md` one row, `docs/ROLLOUT-v1.7.0_gg7.md` rollback step 1, the paragraph after step 4 and row D5). The `ClickHouseWriter` row now says three residuals of the five in Known cost 13: I checked it against `ClickHouseHelperClient` (the constructor assigns `client = createClientV2()` and then `server = createClientV1()`, and `ClickHouseNode.of(url, options)` can throw after the client V2 instance exists), `ClickHouseWriter.startInternal` (assigns `chc` with no earlier close) and `stop()` (`TIMEOUT_FOR_SHUTDOWN` 5 s); the count is right (Known cost 13 lists five, three on the writer, one Keeper provider, one task `stop()`). I re-ran the rollback rehearsal myself in a scratch clone under `/tmp`: dry-run merge of `f95f9e9c` onto `72e8ccff`, `git status --porcelain` empty, the block of step 2 printed `ok`, status empty after it, `VERSION` `v1.7.0_gg6`, no copy of the note, and a revert restored the merged tree. The claims of the reworded text therefore hold. Two wording and evidence points remain.

### IN-13: The rehearsal the note now names (commit `f95f9e9c`) is recorded in no evidence entry; E-05 still records `7ef33b4f`

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:156`, `:175` and `:190` (row D5, label `rollback block on a dry-run merge: measured`); `.planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md` E-05
**Issue:** The three sentences were changed to say the block ran on a merge of `f95f9e9c`. The only evidence entry for the rehearsal, E-05, records `rehearsal_tip: 7ef33b4feae0759c17a77e171e4d53e36b924b3a` and `rehearsal_parents: 72e8ccff... 7ef33b4f...`. The re-run on `f95f9e9c` left its output only in `/tmp/gsd44/exec` (`rb-*.log`, overwritten by any later run), so a label `measured` rests on a record that is neither in the repository nor the one the evidence map points to. I reproduced the result (see the scope paragraph), so the claim is true; the gap is the record. A later round or an audit that follows the evidence map finds a different commit from the one the note names.
**Fix:** Add an E-entry to `44-COMMAND-EVIDENCE.md` (for example E-05b under plan 44-03) with `rehearsal_tip: f95f9e9cb5859f7a77cc7cfc32d72d67686ec64c`, `rehearsal_parents`, `d0_status_before_block`, `rollback_output`, `rollback_status_after`, `rollback_version`, `rollback_note_absent`, `rollback_tree_equals_tag` and `revert_restores_tree` as printed by `rehearse.sh`, and state that E-05's `7ef33b4f` rehearsal is superseded for the note's wording. At the publication step, re-run on the release tip (the merge commit) if it is to be quoted as such.

### IN-14: "commit `f95f9e9c` of this branch" never names the branch, in a note that operators read after the branch is merged

**File:** `docs/ROLLOUT-v1.7.0_gg7.md:156`, `:175`, `:190`
**Issue:** The note names no branch anywhere (`grep -n 'sync/v1.7.0'` finds nothing in it). "This branch" means `sync/v1.7.0` only to someone who knows the release process; an operator reading the note from `custom_build` has no way to tell which branch is meant, and the abbreviated SHA is the only handle. The commit does stay reachable after the release (the release is a merge commit of `sync/v1.7.0`, as `3a3a15a9` and its predecessors show), so the pointer is valid but cryptic.
**Fix:** Write "commit `f95f9e9c` of the release branch `sync/v1.7.0` (reachable from `custom_build` through the release merge)" in the first use (step 1) and "that commit" in the other two.

round: 4 findings: 2

## Mandated confirmations (RLS-03)

- (a) 33 rows: `44-note-check.py all` prints `OK rows=33 measured=23 code=20 derived=4 mechanism=2`; the two commits touch no row text except the D5 How-known cell, whose label and strength are unchanged. T1 to T13 still carry the 13 E-09 claims in order, and T6 stays `derived (mechanism, not measured)` by design. No row is stronger than its evidence; the only evidence gap added is IN-13 (the record, not the fact).
- (b) Numbers and commands: `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` is unchanged in substance (8 files, 194 insertions, 22 deletions; no commit since round 3 touches `src`, `gradle`, `build.gradle.kts` or `.github`); the archive figures (7933 and 7932 entries, 13558633 and 13564144 bytes, 120 differences, 59 bytes and six classes against the asset, 13558692 bytes) match E-05. Rows D1 and D4 say "release tip" for the archive rebuilds: accurate in effect, because E-05 exported `HEAD` at `27859a8d` and no file that goes into the archive (`src`, `gradle`, `build.gradle.kts`, `VERSION`, `.github`) changed between `27859a8d` and the current head (only `docs`, `CHANGELOG.md` and `.planning` did, none of which `createConfluentArchive` packs; the archive holds only `doc/LICENSE` and `doc/README.md`). Not reported.
- (c) Applies-when: unchanged by the two commits; every client V1 insert claim stays scoped to `client V1` (default `client_version` V1; the F-2 fix is client V2 only), and the `exactlyOnce=false` plus `ignorePartitionsWhenBatching=true` exclusion stands in R1, section C and the supersession row.
- (d) Rollback: steps 1 to 4 run in order; target `pre-v1.7.0_gg7` is a local tag on `72e8ccff`, equal to `v1.7.0_gg6`; the step 2 block and its `ok` output are unchanged since `f95f9e9c` (the diff of the two commits shows only prose lines 156, 175 and 190 changed), and I reproduced the full rehearsal on `f95f9e9c` in a scratch clone (merge clean, block `ok`, tree equal to the tag, revert restores the tree). No `gradlew.bat` sentence, as measured. The gg6 asset facts match E-05. Findings: IN-13, IN-14.
- (e) No fact about the `release object` of `v1.7.0_gg7` and no promise of one in the two commits; "the release tip, which does not exist until the merge" is about the merge commit, not the release object, and is true.
- (f) No advice that empties or destroys data; step 1 keeps the warning about `git read-tree -u --reset`, no TRUNCATE, DELETE, DROP or force-push.
- (g) `FUT-05 stays open.` is the only line with FUT-05 in the gg7 note (`grep -c FUT-05` prints 1; the checker's `fut` check prints OK), in section F.
- (h) The WARN text: no `src` change since round 3; it still equals the constant in `ClickHouseSinkTask.java` (E-05 `warn_text`).
- (i) FORK-DELTA: the one changed row checked against `ClickHouseHelperClient`, `ClickHouseWriter.startInternal`, `stop()` and `getMutationRequest`: three residuals on the writer's own paths among the five of Known cost 13 is correct and consistent with row F5 and item 13. The `Processing.java` row (line 137, `String originalTopic = record.getSinkRecord().originalTopic();`), the `KeyToValue.java` row, the recounts, the tag-pair commands, the gg7 paragraphs and items 12, 19, 22 and 23 are unchanged by the two commits and stand as confirmed in rounds 2 and 3; the `HEAD`-relative phrases left are the `measured with HEAD in place of the tag` forms that the note states openly.
- (j) CHANGELOG: not touched by the two commits; each bullet of `1.7.0_gg7` still matches the code or behaviour change, the heading date format is unchanged, and it carries no promise of a release beyond the rollout-note pointer.
- Specific tokens: `socket_timeout` rows T9, T10 and T13 unchanged and consistent with `ClickHouseHelperClient`; `pre-v1.7.0_gg7` and `72e8ccff` consistent in the intro, step 2 and row D5; `FUT-05 stays open` stands.

---

_Reviewed: 2026-11-03T01:00:00Z_
_Reviewer: Claude (gsd-code-reviewer), round 4_
_Depth: standard_
