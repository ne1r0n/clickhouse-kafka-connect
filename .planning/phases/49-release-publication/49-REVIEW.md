---
phase: 49
reviewed: 2026-11-06T16:00:00Z
depth: standard
diff_base: 9483476322e0d331e81c5b92ac266a18f0f01450
files_reviewed: 5
files_reviewed_list:
  - docs/ROLLOUT-v1.7.0_gg8.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg6.md
  - docs/ROLLOUT-v1.7.0_gg1.md
findings:
  critical: 0
  warning: 9
  info: 6
  total: 15
status: issues_found
rounds: 1
---

# Phase 49: Code Review Report

**Reviewed:** 2026-11-06T16:00:00Z
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Round 1

reviewer_agent_id: aa6175e0dcb6fb58e

No finding is a data-loss or failed-rollback hazard. The rollback block, its order, the target `pre-v1.7.0_gg8` on `94834763` and the asset facts check out against git and the evidence. The findings are overstated or mislabelled claims and cross-document inconsistencies.

### WR-01: CHANGELOG presents the slash-in-column-name item as a behaviour change a v1.7.0_gg7 user will see

**File:** `CHANGELOG.md:40`
**Issue:** The Bug Fixes bullet says a field name containing `/` "now round-trips through the `DESCRIBE` reader on client V1 and client V2, so a table whose column or Tuple element name holds a slash is discovered under its raw name". That is false for a `v1.7.0_gg7` deployment. `git diff --quiet v1.7.0_gg7 HEAD -- .../ClickHouseFieldDescriptor.java` exits 0, and `ClickHouseHelperClient` has only the `insertBoundMs` addition since gg7 in `src/main`. Plan 49-02 (E-06) also ran `SlashColumnRoundTripTest` green on an export of the gg7 tag. This release adds tests that pin the behaviour and changes none. The gg8 note's row U1 says so ("so an upgrade changes nothing here"); the CHANGELOG, which operators read first, says the opposite under "Bug Fixes". FORK-DELTA's raw-identifier row is accurate; only the CHANGELOG bullet is wrong.
**Fix:** Move the bullet out of Bug Fixes (to Improvements or Build and CI) and reword it: "From upstream's tail (issue #838, fixed upstream by pull request #839): a field name that contains `/` in a `DESCRIBE` response round-trips on client V1 and client V2. This is not a change of behaviour for `1.7.0_gg7`: the fork's reader already decoded only the `name` of a row; this release adds upstream's regression tests (`SlashColumnRoundTripTest`) and the fork's `FieldDescriptorLiteralFieldsUnitTest`." Also change the intro sentence "Two items of upstream's tail are new here" so that it does not read as two behaviour changes.

### WR-02: The same two upstream items carry different numbers in the CHANGELOG, in FORK-DELTA's last paragraph, in FORK-DELTA's rows and in the note

**File:** `CHANGELOG.md:40,44`; `docs/FORK-DELTA.md:318` against `docs/FORK-DELTA.md:48,55` and `docs/ROLLOUT-v1.7.0_gg8.md:43,49`
**Issue:** I checked upstream (GitHub API, read only). #838 and #640 are issues; #839 and #857 are the pull requests that close them ("Fixes #838", "Closes #640"). The CHANGELOG and the FORK-DELTA gg8 paragraph write bare `#838` and `#640`; the FORK-DELTA rows and the note write `#839`/`#857` and "pull request 839/857". Each number is correct as what it is, but nothing says so, so a reader who greps the CHANGELOG for `857` finds nothing, and FORK-DELTA contradicts itself on one page (rows `#839`/`#857`, paragraph `#838`/`#640`). The CHANGELOG's own convention for upstream items is spelled out ("upstream issue 777", "PR 848").
**Fix:** Name the object kind and give both numbers, the same way everywhere: "upstream issue #838, fixed by pull request #839" and "upstream issue #640, fixed by pull request #857". Apply to `CHANGELOG.md:40,44`, `docs/FORK-DELTA.md:318` and, for the note, to the A-section prose (`docs/ROLLOUT-v1.7.0_gg8.md:43`).

### WR-03: FORK-DELTA test-tree row attributes the 14 non-tail differences to fork adaptation only; seven of them also carry upstream's table-statement refactor, and one adds a test

**File:** `docs/FORK-DELTA.md:145`
**Issue:** The counts re-derive correctly (143 files, 115 byte-identical, 28 differing, 8 byte-identical to `83808df9`, 6 changed after gg1; the 14 + 8 + 6 partition sums to 28). The explanation of the 14 is incomplete. `git diff v1.7.0 HEAD` shows `CreateTableStatement.create()` replacing `new CreateTableStatement()` in `ClickHouseSinkTaskMappingTest`, `TableTest`, `ClickHouseSinkTaskTest`, `FailureTest`, `ClickHouseSinkTaskSchemalessProxyTest`, `ClickHouseSinkTaskWithSchemaProxyTest` and `ClickHouseWriterTest`. `v1.7.0` has no `create()` at all and `83808df9` has it, so this is upstream's tail refactor (`TableTest` at the tail already has both `create()` sites), not fork adaptation. The row says `TableTest` "carries one corrected assertion" (it also has the refactor, 7 insertions and 5 deletions) and describes `ClickHouseWriterColumnContextTest` only by the null connector name (it also adds `classCastFailureTextIsPinnedExactly`). Eight of the 14 were changed after gg1 as well, so "the last six were changed after `v1.7.0_gg1`" reads as exclusive and is not.
**Fix:** After the 14-file list add: "seven of them (`ClickHouseSinkTaskMappingTest`, `TableTest`, `ClickHouseSinkTaskTest`, `FailureTest`, `ClickHouseSinkTaskSchemalessProxyTest`, `ClickHouseSinkTaskWithSchemaProxyTest`, `ClickHouseWriterTest`) also take upstream's `CreateTableStatement.create()` refactor from the tail, and `ClickHouseWriterColumnContextTest` adds the fork's `classCastFailureTextIsPinnedExactly`." Change "The last six were changed after `v1.7.0_gg1`" to "The last six differ for reasons beyond the above:".

### WR-04: FORK-DELTA item 13 still says `ClickHouseSinkTaskStopUnitTest` has six cases; it has seven

**File:** `docs/FORK-DELTA.md:250`
**Issue:** Item 13 says "two of its six cases (the ones that use a throwing strategy) red on the unchanged tree and all six green after". The test class has seven cases (the JUnit result of the base run holds `tests="7"`; the methods are `strategyStopFailureStillStopsTheProxyTask`, `bothStopsRunWhenTheStrategyAndTheProxyTaskThrow`, `stopWithoutADeliveryStrategyStillStopsTheProxyTask`, `theTaskMBeanIsRegisteredBeforeStopAndGoneAfter`, and three shipped-strategy cases). Commit `ae31c4a9` (Phase 48) added the seventh after the Phase 47 measurement of six. Plan 49-02 saw this (E-06) and recorded it as unfixed. The note states no count, so the note and FORK-DELTA do not collide, but FORK-DELTA is wrong about the file it names.
**Fix:** "`ClickHouseSinkTaskStopUnitTest` (seven cases; six when Phase 47 measured it, and in that run two of them, the ones that use a throwing strategy, were red on the unchanged tree and all six green after, E-02 and E-03)".

### WR-05: "No record value reaches ... a log" is stated without a level, but the writer logs record values at TRACE

**File:** `CHANGELOG.md:44-50`; `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2); `docs/FORK-DELTA.md:45` ("never logs the value")
**Issue:** The CHANGELOG says "No record value reaches the message, the cause chain, a stack trace, the dead letter queue headers or a log, on the RowBinary route and on the JSON route". The note's U2 says the same ("a log") with the qualifier "in the cases the tests drive". `ClickHouseWriter` still logs values on the RowBinary path: `LOGGER.trace("Writing date type: {}, value: {}, ...", ..., value.getObject(), ...)` (line 472), `LOGGER.trace("Writing fixed string type: {}, value: {}", ..., value)` (1104), `LOGGER.trace("Writing primitive type: {}, value: {}", ..., value)` (1125) and `LOGGER.trace("Column value is {}", value)` (1275). AGENTS.md says TRACE is the per-record level, so an operator at TRACE sees values of exactly the records that fail. The claim is true at INFO, WARN, ERROR and DEBUG only. The CHANGELOG sentence has no qualifier at all, and it is a privacy claim.
**Fix:** In the CHANGELOG bullet and in U2 add the level: "...or a log at DEBUG or above; the writer's TRACE lines still print record values". Add the same qualifier to the FORK-DELTA `doWriteDates` row ("never logs the value" holds above TRACE). Alternatively state that the value-free claim is about the failure path's ERROR and WARN output.

### WR-06: Row T5 is labelled `measured` as a whole but its async=true exception is read from the code

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:64`
**Issue:** T5 has three claims: both connector bounds are removed when the operator names `socket_timeout`; `socket_timeout=0` keeps inserts unbounded; and the response wait of a client V2 insert with `async=true` "still ends at `clickhouseClientInsertTimeoutMs`; the cut future is cancelled while its request may keep running". The first two are pinned by `InsertSocketTimeoutOptionUnitTest` and `InsertBoundGateUnitTest`. The third is pinned by no test: the only place in `src/test` that mentions `async=true` is the help-text pin, which checks the phrase, not the behaviour. It rests on `awaitInsertResponse` being ungated (FORK-DELTA item 14 words it "the unchanged `awaitInsertResponse` still applies this key to that future"). The note's own rule says a row of mixed strength must label each claim.
**Fix:** Label: "gate on both clients and `socket_timeout=0`: measured; the `async=true` response wait and the cancelled-future remark: read from the code (`ClickHouseWriter.awaitInsertResponse`, which `insertBoundMs` does not gate)".

### WR-07: Row S3's closing consequence has no label and is a derived claim

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:86`
**Issue:** The last sentence of S3's Effect ("An insert that `clickhouseClientInsertTimeoutMs` cuts is such a failure, so a permanently stalled insert is attempted again at every redelivery, and with `exactlyOnce=false` each attempt can commit rows") is a conclusion from item 14 and item 17 (item 17 itself says "Consequence, derived from item 14 and the interval above"). The Label cell lists measured, "measured (Kafka 3.9 line only)" and read-from-the-code claims, but no `derived`. Nothing in the evidence measured a redelivered cut insert committing rows twice.
**Fix:** Add "consequence for a cut insert: derived" to the Label cell, in the `claim: label; claim: label` form, and remove the claim's apparent coverage by the 3.9-line measurement.

### WR-08: The closes table omits gg7 statements that the gg8 rows supersede

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:157-170`
**Issue:** The table names F5, R8, T1, T12, T14, R6, the end of section F, three known-limits bullets and three gg7 CHANGELOG statements. These gg7 statements are also contradicted by gg8 rows and are not named:
- gg7 row T11, "A write-side stall ... is not bounded, because `socket_timeout` bounds reads only" (client V2, labelled measured). Row T2 of this note reverses it. Only the known-limits bullet that lists T11 is named, not T11.
- gg7 row T13, "the ping has its own timeout" (client V2). Row T7 of this note says the client V2 `ping(timeout)` does not fire (still blocked at 20005 ms with 2000 ms), and FORK-DELTA item 14 (c) says it "corrects the earlier claim about the ping's timeout".
- gg7 row T15, "The client V1 half of that statement stands", the statement that the four-minute insert timeout has no effect while `async=false`. On client V1 it no longer stands (rows T3, T4).
- gg7 "Before you upgrade" item 2 ("A connector that runs client V1 ... has no connector-side insert bound in this release") and "What to do" item 3 of its section B ("Do not read the bound as ... covering ... a client V1 insert").
- The gg7 CHANGELOG Known Issues bullets "The closing has residuals ... a task-level `stop()` that has no `try`/`finally`" and "The bounded insert has limits. A write-side stall ... is not bounded. Client V1 inserts have no connector timeout ..."; the table names only two other gg7 CHANGELOG statements.

The table's own heading promises "the statements named in the first column" and the intro says the gg7 note "is not edited", so a gg7 reader has only this table to find what no longer holds.
**Fix:** Add rows: (1) gg7 T11 -> closed on client V2 (T2); (2) gg7 T13, the ping clause -> corrected for client V2 (T7); (3) gg7 T15, client V1 half, and gg7 Before-you-upgrade 2 and section-B What-to-do 3 -> superseded (T3, T4); (4) the two gg7 CHANGELOG Known Issues bullets above -> superseded by the gg8 Known Issues (S1, S2, T2, T3, T6, T7, T9).

### WR-09: Row U1's "column or Tuple element" round trip is labelled measured but the cited tests cover a root column only

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:47`
**Issue:** U1 says "A column or Tuple element whose name contains `/` round-trips" with label "round trip on both clients: measured". The cited `SlashColumnRoundTripTest` creates root columns `a/b`, `a__b` and `café/温度` only (no Tuple), and `FieldDescriptorLiteralFieldsUnitTest` feeds name strings to the row reader. A slash in a Tuple element is covered, if at all, by the `sl` entry of `RawNameMatrix` (`RawNameMatrixLiveTest`), which the row does not cite and which this release did not add.
**Fix:** Either cite `RawNameMatrixLiveTest` and its `sl` entry for the Tuple-element half, or narrow the measured claim to "a root column name" and label the Tuple-element half `read from the code` (the same `name` decode serves both).

### IN-01: FORK-DELTA states the gg8 publication as an accomplished fact before it happens

**File:** `docs/FORK-DELTA.md:5,318`
**Issue:** Line 5 says the tree is "published ... as `v1.7.0_gg8`" and line 318 says "The release after that is `v1.7.0_gg8`, published from `sync/v1.7.0` by a non-force merge into `custom_build`", plus "The git tag and the GitHub release are created at the merged SHA by the publish run". No `v1.7.0_gg8` tag or release exists (checked: `git tag -l`, E-01). The CHANGELOG heading correctly says `Unreleased`; these sentences are the one place that asserts the release object. The same wording was flipped to past tense after publication for earlier releases.
**Fix:** "is to be published", "will be created", or add "(not yet published when this was written; the heading of the `1.7.0_gg8` changelog section reads `Unreleased` until then)". Flip it in the publish commit.

### IN-02: FORK-DELTA row for raw identifiers ties #839 to the commit of #857

**File:** `docs/FORK-DELTA.md:55`
**Issue:** "Upstream #839 (`83808df9`) then replaced the backslash doubling". `83808df9` is the merge of #857 and the tail's end; the merge of #839 is `e83a0bbb`. The code state at `83808df9` does include #839, so the parenthetical is meant as "as of the tail", but a reader who runs `git show 83808df9` for #839 lands on the wrong merge.
**Fix:** "Upstream #839 (merged as `e83a0bbb`, part of the tail `83808df9`)".

### IN-03: Rollback "reversed" lists name U1 (which changes nothing) and omit T10

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:141,151`
**Issue:** "rows U1 to U3 reversed ... the `DESCRIBE` reader is the one of `v1.7.0_gg7`" and D3's "reverses rows U1 to U3, T2, T3, T5 and T9, S1, P1 and D1". The reader is byte-identical in gg7 and gg8, so nothing of U1 reverses (the row itself says an upgrade changes nothing). T10 (the two ERROR lines on a deadline cut) does reverse, because its cause rows T2 and U2 do, and is not listed.
**Fix:** "rows U2 and U3 reversed ... (row U1 changes nothing: the `DESCRIBE` reader is the same in both)"; add T10 to the reversed list in both places.

### IN-04: Row P1 says the diff shows "three removed lines (the Javadoc and the declaration)"

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:95`
**Issue:** `git diff v1.7.0_gg7 HEAD` of `DeclaredNullability.java` removes three lines: the one-line Javadoc, the declaration and a blank line. The parenthetical names two.
**Fix:** "three removed lines (the Javadoc, the declaration and a blank line)".

### IN-05: Before-you-upgrade step 4 and the section A advice read as a regression from gg7 that did not happen

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:36,52`
**Issue:** "A parser or an alert that reads the topic, the partition, the offset or the column out of an exception message needs the ERROR line ... instead". In `v1.7.0_gg7` and in this release the writer's exception message does not carry those coordinates (the fork rethrows its own exception), so no gg7 parser can have relied on them. Only someone coming from upstream would have. As written, the step suggests the exception text changed.
**Fix:** "If you come from upstream, or expect upstream's wrapped message: this fork does not put the coordinates in the exception; read them from the ERROR line `Record serialization failed`. The exception text itself is unchanged since `v1.7.0_gg7`." The new ERROR line (log volume per failing record) is the actual operator-visible change; say that.

### IN-06: FORK-DELTA item 17 lists the 16 retriable shapes with two repeats

**File:** `docs/FORK-DELTA.md:258`
**Issue:** The parenthetical lists "a `RetriableException`" and later "a bare `RetriableException`", and "a `RetriableException` over a non-retriable code" and later "one over a non-retriable server code". The pin has 16 distinct retriable shapes (22 shapes less six non-retriable and conversion controls); the prose lists 16 items but only 14 are distinct. The note's S3 counts (16 shapes, 108 cases) are right.
**Fix:** Replace the enumeration with the shape names of `RetriableRedeliveryLimitPinUnitTest.Shape`, or drop the enumeration and keep the counts.

---

round: 1 findings: 15

## Mandated confirmations (RLS-03)

(a) Every one of the 22 rows (U1 to U4, T1 to T10, S1 to S4, P1, D1 to D3 = 22 rows) was read against its source and label. Not clean: U1's Tuple-element half is not covered by the cited tests (WR-09), T5 labels the `async=true` exception as measured though no test runs it (WR-06), and S3's closing consequence has no label (WR-07). The qualifiers on T6 (stub only for the cut during the send), S1 (Connect's handling read, not measured), S3 (Kafka 3.9 line only), D2 and D3 are honest. FAILED, see WR-06, WR-07, WR-09.

(b) Commands, numbers and versions. Verified: archive figures against E-05 (7932 and 7935 inner entries, 13564144 and 13572233 bytes, 17 differing entries = 3 only-b + 13 changed + manifest, `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`, 18 libraries unchanged, asset 13564238 bytes and 94 bytes larger than the rebuild, six differing classes); the T-row durations against FORK-DELTA item 14; case counts against the JUnit XML (SlashColumnRoundTripTest 4, FieldDescriptorLiteralFieldsUnitTest 31, WriterRecordContext 59 and 4, JacksonOverIoValueFree 109, JsonRouteValueFree 42 and 4, InsertWriteStallBoundPin 20, InsertV1WaitBoundPin 21, InsertSocketTimeoutOption 23, InsertBoundGate 20, InsertTimeoutHelpTextPin 27, RetriableRedeliveryLimitPin 108); FORK-DELTA raw numbers against `git diff --shortstat` (364/47983/1260, 43/3578/333, 36/5602/97, 6/428/72, per-file figures, per-directory figures); the totals sentence against E-04 (2325 and 2, 2928 and 18, 218 rows); the test-tree recount re-derived by script (143, 115, 28, 8, 6) and the `src/main` recount (30, 14, 19, 10). Two defects: the test-tree row's reasons for the 14 files are incomplete (WR-03) and item 13 says six cases where there are seven (WR-04).

(c) Applies-when against the code path. Client V2 only for T2 and T10 (the write-phase deadline and the RowBinary log lines; `writeRecordsRowBinary` is also reached by client V1, but the deadline cut exists on V2 only); client V1 only for T3; the `insertBoundMs` gate and `socket_timeout` for T5 hold on both clients, with the `async=true` exception on client V2 (see WR-06); `exactlyOnce` scoping on T8 and S3 matches `QueryIdentifier` and the pin that runs with `exactlyOnce=false`; the default of `client_version` is `V1` in `ClickHouseSinkConfig` and any other value, including an empty one, selects client V2 in `ClickHouseWriter.startInternal`. No wrong Applies-when found.

(d) Rollback. Order is correct (fetch, checkout and fast-forward, clean-status check, branch and `git read-tree -u --reset pre-v1.7.0_gg8`, commit, tree check, redeploy, publish); the local tag `pre-v1.7.0_gg8` and tag `v1.7.0_gg7` both resolve to `94834763` (`9483476322e0d331e81c5b92ac266a18f0f01450`); the gg7 asset size and SHA-256 match E-05; the rehearsal statement (commit `b4615700`, `rehearsal: PASS` in both modes, `git 2.50.1`, no other version) matches E-05. The release workflow is `workflow_dispatch` only, so the rollback push publishes nothing. Passes. Only a wording point remains in IN-03.

(e) No fact about the `release object` and no promise of one in the note: it says only that the tag `v1.7.0_gg8` does not exist before publication and that the rollback tag is pushed when the release is published. Passes for the note. FORK-DELTA does state the publication as done (IN-01).

(f) No advice empties or destroys data. The one destructive command, `git read-tree -u --reset`, is warned about, preceded by a clean-status gate with `git stash -u` and branch isolation, and acts on a new branch. No TRUNCATE, DELETE or state-table advice; the exactly-once KeeperMap state is untouched and the note says the rollback changes no state format. Passes.

(g) ERROR line texts. ``Record serialization failed: topic [..] partition [..] offset [..] column `..`: exception <class>`` matches `RECORD_CONTEXT_FAILURE_LOG_FORMAT` (line 626) up to placeholder names; `Error writing value of ...` matches the prefix of `WRITE_VALUE_FAILURE_LOG_FORMAT` (line 623); the order in T10 (value-write line first, record line after) matches the catch nesting. The exceptions rethrown and the `RetriableException` pass-through match `writeColumnWithRecordContext`. Passes, apart from the TRACE-level overstatement in WR-05.

(h) The closes table names F5, R8, T1, T12, T14, R6, the last statement of section F and the residual and limit bullets (first, second, fourth). The note carries no planning identifier (only 8-hex commit abbreviations, branch names and its own row ids). FAILED for completeness: gg7 T11, the ping clause of T13, T15's client V1 half, Before-you-upgrade 2, What-to-do 3 and two gg7 CHANGELOG Known Issues bullets are superseded and not named (WR-08).

(i) FORK-DELTA. The VERSION row and the `currently v1.7.0_gg8` parenthetical read `v1.7.0_gg8`; the tag-pair commands for gg8 are present; the after-gg8 rows and the gg7-to-gg8 paragraph match the shortstats; the totals sentence matches E-04; the gg8 intro, the pointer to the note and the next-syncer paragraph agree with the note on version, rollback target `94834763`, the tree-neutral merge and the baseline choice; items 14, 17 and 20 (the removed public constant `ESCAPED_NEWLINE`) and item 24 agree with the note and the CHANGELOG, and item 8 agrees (FUT-05 closed by decision: item 17 records the limit, item 24 closes the follow-up); the changed evidence paths exist. FAILED on items 13 (WR-04) and the test-tree row (WR-03), the #838/#640 against #839/#857 mix in the next-syncer paragraph (WR-02), the record-value-in-a-log wording (WR-05) and the pre-publication wording (IN-01).

(j) Released CHANGELOG sections are byte-identical to `v1.7.0_gg7`'s: the file from the `# 1.7.0_gg7, 2026-11-02` heading down equals `git show v1.7.0_gg7:CHANGELOG.md` (checked with `cmp`; only the missing trailing newline of the tag copy differs from an awk extract); the Phase 47 bullets are unchanged by the last CHANGELOG commit (`d2240a70` has no removed line); the heading reads `Unreleased` by U-3 and the note's `Unreleased` reference is fine. Passes.

(k) No host, connector or planning identifier in the note or the CHANGELOG section (scanned for phase numbers, requirement ids, evidence ids, review ids, host names); the references to `FORK-DELTA item 14, 17, 24` are document pointers, as PL-4 allows. Passes.

(l) The Phase 48 edits of the gg6 and gg1 notes are correct as published text. gg6 row D7: the tripwire `customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` is present in the `v1.7.0_gg7` tag and absent from `v1.7.0_gg6`, so "since `v1.7.0_gg7`" is right. gg6 row C2 and its intro: items 17 and 24 say what the row now says (no budget, no removal setting, by decision). gg1 item 6 and its notice: the ZSTD decode on client `0.11.0` was measured by the gg6 tests (`NetworkCompressionPinLiveTest`, `Client011BehaviourPinLiveTest`, gg6 row D13), so "measured on the `v1.7.0_gg6` tree" is accurate. Passes.

(m) Cross-document agreement. Version (`v1.7.0_gg8`, `VERSION` file, CHANGELOG heading `Unreleased`), the rollback target (`pre-v1.7.0_gg8` on `94834763`, FORK-DELTA and note), the change set (six `src/main` files, 428 insertions and 72 deletions in the note and FORK-DELTA), and the client scope (every V1 claim says `client V1`, every V2 claim says `client V2`) agree. Disagreements: the upstream item numbers (WR-02), the CHANGELOG framing of the slash item against the note (WR-01), and the closes table against FORK-DELTA item 14 on the ping (WR-08).

---

_Reviewed: 2026-11-06T16:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
