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
  warning: 21
  info: 11
  total: 32
status: issues_found
rounds: 5
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

## Round 2

reviewer_agent_id: a09cc0035374691f7

All 15 round-1 fixes were checked one by one against `src/main`, git and the gg7 tag. Thirteen are correct as applied: the CHANGELOG slash bullet now says the behaviour did not change for `1.7.0_gg7`; `e83a0bbb` is the merge of pull request #839 and is part of the tail `83808df9`; `ClickHouseSinkTaskStopUnitTest` has 7 cases (4 plain, 3 from the enum source); the seven refactor files, the added `classCastFailureTextIsPinnedExactly` and the 14 + 8 + 6 partition re-derive by script (143, 115, 28, 8 tail-identical, 20 others); the gg7 statements quoted in the new closes rows match `git show v1.7.0_gg7:docs/ROLLOUT-v1.7.0_gg7.md` and `git show v1.7.0_gg7:CHANGELOG.md` word for word (T11, the T13 clause, the T15 sentence, Before-you-upgrade 2, What-to-do 3, the two Known Issues bullets); the 16 shapes of item 17 regroup as 9 + 1 + 2 + 3 + 1 and match the 16 retriable `Shape` values of `RetriableRedeliveryLimitPinUnitTest`. Two fixes introduced new wrong or too strong claims: the WR-05 fix (an incomplete list of TRACE lines, WR-12) and the IN-05 fix (two claims about what the exception carries, WR-10 and WR-11). Round 1 also missed one label (WR-13). Two wording findings follow.

### WR-10: "The exception text itself is unchanged since `v1.7.0_gg7`" is false for a chain that holds a Jackson link

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:36` (Before you upgrade, step 4) and `docs/ROLLOUT-v1.7.0_gg8.md:52` (section A, What to do)
**Issue:** The sentence was added by the IN-05 fix. In `v1.7.0_gg7`, `doWriteColValue` rethrew a pass-through chain as it was (`git show v1.7.0_gg7:src/main/.../ClickHouseWriter.java`, the `passesThroughUnchanged` branch, `throw e`), and `writeRecordsJson` had no serialization catch (the constant `JSON_ROW_FAILURE_FORMAT` was defined and not used). Since then `throwRebuiltJacksonChain` (`ClickHouseWriter.java:782`, called at 1096 and 1687) replaces every Jackson link, and the top text, with a value-free substitute whenever the chain holds one, on the RowBinary route and on the JSON route. So the message and the cause chain of such an exception differ from `v1.7.0_gg7`. Row U2 of the same note says "a Jackson link in its cause chain is rebuilt without any record value", the rollback list says "the value-free rebuild of a Jackson chain" is gone after a rollback, and FORK-DELTA's rows on `doWriteColValue` and on the JSON route and the CHANGELOG bullet say the same. The unqualified sentence contradicts them. Only the class and the retry classification are unchanged.
**Fix:** In both places replace "The exception text itself is unchanged since `v1.7.0_gg7`." by "The exception's class and its retry classification are unchanged since `v1.7.0_gg7`, and so is its text, except that a cause chain that holds a Jackson link is now rebuilt with a value-free text (row U2)."

### WR-11: "this fork does not put the record's offset or the column in the exception" is wrong for the column

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:36`, `:49` (row U3) and `:52`
**Issue:** After the IN-05 fix, step 4 and the section A advice say the record's offset or the column is not in the exception, and row U3 says "the record's own offset and the column are in the ERROR line of row U2 only". The writer's own `DataException` for a value that could not be written carries the column in its message: ``Failed to write column `<name>`: Kafka value of type ... could not be written as ClickHouse type ...`` (`WRITE_VALUE_CONVERSION_FAILURE_FORMAT` and `WRITE_TYPE_MISMATCH_FAILURE_FORMAT`, `ClickHouseWriter.java:630-633`, built by `buildValueConversionFailureMessage`; both formats exist at `v1.7.0_gg7`). FORK-DELTA's `doWriteColValue` row says "the column, type and class are in `exception.stacktrace`" (D-15). So a reader who follows the note stops looking in the exception for the column, which is there for those shapes. Only the record's own offset is in no exception (the `Processing` wrap carries the batch's topic, partition and offset range). A label of `read from the code` makes the wrong claim a stronger one.
**Fix:** Step 4 and section A: "this fork does not put the record's own offset in the exception: read it from that ERROR line, which also names the column for every failure shape". Row U3: "the record's own offset is in the ERROR line of row U2 only; the column is also in the message of the writer's `DataException` for a value that could not be written, as in `v1.7.0_gg7`".

### WR-12: The list of TRACE lines in row U2 leaves out the two that print a whole record

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2, How it is known)
**Issue:** The WR-05 fix says "The TRACE lines are the `LOGGER.trace` calls of `ClickHouseWriter` that write a date, a fixed string, a primitive and the column value." `ClickHouseWriter.java` has more TRACE lines that print record data: `writeJsonRow` (line 1708, `"topic {} partition {} offset {} payload {}"` with the whole serialized row) and `writeRecordsString` (line 1856, `"data: %s"` with the whole String value), and the date lines 555 to 564 print the parsed epoch value. The row claims to cover the JSON route ("on the RowBinary route and on the JSON route") and the String route is one of the three insert routes, so the very lines that expose a record on the JSON route are missing from a list given as the list. The label is `read from the code`, so the omission makes the label stronger than the reading.
**Fix:** "The TRACE lines are the `LOGGER.trace` calls of `ClickHouseWriter` that write a date, a fixed string, a primitive, the column value, the serialized JSON row (`writeJsonRow`) or the String value (`writeRecordsString`)." Or drop the enumeration and keep "the writer's TRACE lines still print record values".

### WR-13: Row T3 is labelled `measured` as a whole but its default-value claims are derived

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:62` (row T3); the same claim without a label in the CHANGELOG (Important Changes, second bullet), Before you upgrade step 2 and the closes table
**Issue:** T3 says "At the default of 240000 ms a quiet client V1 insert still ends at the library's own 30 s limit, and a value above 30000 ms does not extend it". `InsertV1WaitBoundPinUnitTest` has no case at the default or above 30000 ms: its quiet case uses 300 ms. The 30 s figure is the gg7 measurement on the unchanged tree (gg7 row T12, 30140 ms), and FORK-DELTA item 14 words it as an inference ("so at the default value of 240000 ms ..."). The step from that run to the default of this release, and the clause about a value above 30000 ms, are derived. The note's own rule says that a row of mixed strength labels each claim, and round 1 fixed T5, S3 and U1 for the same reason.
**Fix:** Label cell: "bound ends a write stall, a quiet stall and a progress insert: measured; the 30 s end at the default and no extension above 30000 ms: derived (measured on the unchanged tree, with a connector bound above the library limit)".

### IN-07: gg7 row T6 is narrowed by row T6 of this note and is not named in the closes table

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:157-177`
**Issue:** gg7 row T6 states, without a phase, that an insert during which the server streams progress headers "escapes the bound: it is not ended at `clickhouseClientInsertTimeoutMs` and runs to its natural end". Row T6 of this note says that while the body is still being sent the deadline applies even when progress arrives. The gg7 CHANGELOG bullet with the same statement is covered in the table, the gg7 note row is not (it is reached only through the "What to do" step 3 row, which lists gg8 T6 among the rows).
**Fix:** Add a row: "Row T6 of section B: an insert during which the server streams progress headers escapes the bound." / "Narrowed: while the body is still being sent the deadline applies (stub-server evidence); the escape after the send stays, measured." / T2, T6.

### IN-08: The reversal lists omit row T4

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:142` and `:151` (row D3)
**Issue:** After the IN-03 fix both lists name T2, T3, T5, T9 and T10. Row T4 states that client V1 inserts now have a connector bound and supersedes gg7 rows T1 and T12; it reverses with row T3, and the first bullet already says the client V1 wait has no bound again.
**Fix:** Add T4 to both lists ("rows T2, T3, T4, T5, T9 and T10").

---

round: 2 findings: 6

## Round 3

reviewer_agent_id: a33561b94d0bde104

All 21 earlier findings were re-checked against `src/main`, `git show v1.7.0_gg7:...` and the files as they are. The round-2 fixes are correct as applied: the step 4 and section A sentences now say that the retry classification is unchanged and that the text is unchanged except for a Jackson-holding chain; the offset-only denial matches `WRITE_VALUE_CONVERSION_FAILURE_FORMAT` and `WRITE_TYPE_MISMATCH_FAILURE_FORMAT`; the TRACE list names `writeJsonRow` and `writeRecordsString`; row T3 splits its label; the closes table now names gg7 row T6; the reversal lists carry T4; the released CHANGELOG sections equal `git show v1.7.0_gg7:CHANGELOG.md` (71626 bytes, script compare); no planning identifier is in the note or the CHANGELOG section. The ERROR line formats (`RECORD_CONTEXT_FAILURE_LOG_FORMAT`, `WRITE_VALUE_FAILURE_LOG_FORMAT`), the catch nesting that orders the two T10 lines, the `RetriableException` pass-through of `writeColumnWithRecordContext`, the `insertBoundMs` gate and the `awaitInsertResponse` and `awaitInsertResponseV1` waits were re-read and match. The new findings are one scope error that survived two rounds (T6 and its three echoes), one privacy claim in the CHANGELOG that is stronger than the note's own qualified sentence, one imprecise description of the Jackson rebuild that the last orchestrator follow-up produced, and one unlabelled-claims point on row U2.

### WR-14: Row T6 and its echoes say a client V2 insert that streams progress headers after the send is not bounded, which is false for `async=true` and omits the gg7 conditions

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:65` (row T6, Effect and Applies when), `:75` (What to do, step 3), `:185` (known limits, second bullet); `CHANGELOG.md:82-84` (Known Issues, the bounded-insert bullet)
**Issue:** Row T6 has Applies when `client V2` and states that the insert "is not bounded, by decision". Row T5 of the same note says that the response wait of a client V2 insert with `async=true` "still ends at `clickhouseClientInsertTimeoutMs`", and the code agrees: `insertWithWriteDeadline` calls `awaitInsertResponse`, which does `future.get(timeoutMs)` on the insert future with no gate and no reset on progress headers (`ClickHouseWriter.java:1981-1991`, `:2063-2072`). For `async=true` that future completes when the response arrives, so a progress-streaming insert is cut at the bound whether or not `socket_timeout` is named. The escape of T6 exists only where `awaitInsertResponse` receives an already completed future, which is `async=false`, the default, and it was measured only with `async_insert=0`. The gg7 row T6 that this row narrows says so in its Applies when ("client V2, async=false, server `async_insert=0`") and also says that the connector adds `send_progress_in_http_headers=1` as a default and that a value in `clickhouseSettings` replaces it, so that "without that setting the same insert is bounded" (3007 ms in the gg7 changelog, `CHANGELOG.md:165`). Row T6 keeps `async_insert=0` in its Effect only, drops `async=false` and the progress-header setting, and has `client V2` as the whole Applies when. FORK-DELTA item 14, case (a), carries `send_progress_in_http_headers=1` and `async_insert=0` as the conditions of the measured run. The effect is that a reader with a client V2 connector that sets `async=true`, or that replaces `send_progress_in_http_headers` in `clickhouseSettings`, is told that nothing bounds its progress-streaming inserts. The step 3 sentence, the second known-limits bullet and the CHANGELOG bullet repeat the unscoped statement.
**Fix:** Row T6, Applies when: `client V2, async=false (the default)`. Effect: add "With `async=true` the same insert is cut at `clickhouseClientInsertTimeoutMs` (read from the code: `awaitInsertResponse`, row T5). The escape needs the progress headers that the connector requests by default with `send_progress_in_http_headers=1`; a value for that key in `clickhouseSettings` replaces it, and gg7 measured the same insert without it failing at 3007 ms." Label: add "the `async=true` cut and the setting: read from the code". In step 3, the known-limits bullet and the CHANGELOG bullet write "a client V2 insert with `async=false` whose body has been sent while the server streams progress headers".

### WR-15: The CHANGELOG states the value-free claim without the scope that row U2 gives it, and it is wider than the tests for a server-side parse failure

**File:** `CHANGELOG.md:44-47` (Bug Fixes, the #640 bullet); `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2) for the contrast
**Issue:** The CHANGELOG says "No record value reaches the message, the cause chain, a stack trace, the dead letter queue headers or a log at DEBUG or above, on the RowBinary route and on the JSON route". Row U2 says the same only "in the cases the tests drive", and labels it `measured` for those cases. Round 1 (WR-05) found the missing level and the fix added "at DEBUG or above" and the TRACE remark, but the CHANGELOG sentence still has no scope at all, and it is the document operators read first and a privacy claim. The claim is also wider than what the code and the tests cover: it is about failures that the writer's own serialization raises, and a rejection by the server is another path that neither the rebuild nor the record-context line touches. A server that cannot parse an inserted row normally answers with a `ClickHouseException` whose message quotes text near the failure (ClickHouse behaviour; no test of this repository measures it, so this part is stated as a risk, not as a measurement). `Utils.handleException` rethrows it as the cause of `RuntimeException("Number of records: ...")`, `doInsertRawBinary` logs `LOGGER.error("Error inserting records", e)` with the throwable on the RowBinary route (`ClickHouseWriter.java:1413`, `:1430`), and on both routes the cause chain is what Connect puts in the task status and in the dead letter queue stack trace header. A reader who takes the bullet at its word about the JSON route and the dead letter queue headers is told more than the tests show.
**Fix:** In the CHANGELOG bullet: "In the cases the tests drive (a failure raised by the writer's own serialization, not a rejection by the server, whose message can quote the rejected row), no record value reaches ...". Keep the TRACE remark. Row U2 may carry the same parenthesis so that both documents say one thing.

### WR-16: The rebuild is described as "a substitute exception in place of the Jackson one", which misdescribes what the code does to the other links

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2), `:36` and `:52` (step 4 and section A, "rebuilt with a value-free text"); `CHANGELOG.md:43-45`
**Issue:** The last orchestrator follow-up replaced "so its class ... do not change" by "so such a chain carries a substitute exception in place of the Jackson one". `withheldJacksonChain` (`ClickHouseWriter.java:734-780`) does more and something different: the top is always a new exception carrying the value-free top text; a Jackson root becomes an `IOException` substitute; every Jackson link above the root is dropped, so the chain can be shorter, and when a non-Jackson `IOException` run sits at the bottom the Jackson link is dropped with no substitute at all; and every non-Jackson link above a Jackson link is replaced by a substitute of the same kind with its message replaced by `<class> (message withheld: may contain record data)`, which is `IOException` or `RuntimeException` for a class outside the list in `sameKindStandIn`. FORK-DELTA's row on `doWriteColValue` states all of this ("every Jackson link above the root is dropped ... every other link above becomes a substitute of the same kind with a withheld text"). The note and the CHANGELOG say only that the Jackson link is replaced, so an operator reading a cause chain cannot tell that the messages of its non-Jackson links above the Jackson one are gone too, nor that a link may be missing. The retry-classification statement stays true (the links that classification reads keep their kind and code, which `JacksonOverIoValueFreeUnitTest` pins), and the sentence about the top text is right; the cause-chain half is incomplete.
**Fix:** Row U2 and the CHANGELOG: "a Jackson link in its cause chain is rebuilt without any record value: the top carries a value-free text, Jackson links are replaced or dropped, and each other link above a Jackson link keeps its kind and its error code but carries a withheld message; links below stay as they were (`FORK-DELTA.md`, the row on `doWriteColValue`)". Step 4 and section A may keep their sentence, which already points to row U2.

### WR-17: Row U2's label cell labels two of its claims and leaves the line, the rethrow, the retry classification, the rebuild and the `RetriableException` pass-through unlabelled

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2, Label)
**Issue:** The note's rule is that a row of mixed strength says which label each claim has. U2's label cell reads "no value in the cases the tests drive: measured; the TRACE lines still print values: read from the code". The Effect also claims that one ERROR line with a stated format is logged, that the writer's own exception is rethrown, that its retry classification does not change, that a Jackson chain is rebuilt, and that a `RetriableException` passes without the line. Round 1 and round 2 fixed the same shape in rows T5, S3, U1 and T3. These claims are measured by the named tests (`WriterRecordContextUnitTest` asserts the line text, the rethrow and the `RetriableException` pass-through, and calls `Utils.handleException` on the thrown exception; `JacksonOverIoValueFreeUnitTest` pins the rebuild and its classification), so the fix is a label, not a retraction. The unlabelled claims otherwise read as `derived` or as default.
**Fix:** Label cell: "the line, the rethrow, the retry classification, the rebuild and the `RetriableException` pass-through: measured; no value in the cases the tests drive: measured; the TRACE lines still print values: read from the code".

### IN-09: The rollback list says the value-free rebuild is gone but not what that brings back

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:141` (rolling back brings back, rows U2 and U3)
**Issue:** The bullet says that "the value-free rebuild of a Jackson chain" is gone after a rollback. The sibling bullets give the operator consequence (no connector bound again, the old help text, the old ERROR lines). This one does not, and its consequence concerns data: on `v1.7.0_gg7` a pass-through chain that holds a Jackson link, and a Jackson failure on the JSON route, reach the task status, the dead letter queue headers and the stack trace with their messages, which can quote record keys and values. The `1.7.0_gg7` CHANGELOG states this as an accepted residual (its lines on a Jackson exception whose chain also holds a non-Jackson `IOException`, and on the `insertFormat=json` route), so the fact is not new; it is missing at the one place where the operator decides to go back.
**Fix:** Add to the bullet: ", so on `v1.7.0_gg7` a Jackson failure can again carry a record key or value in the task status, the dead letter queue headers and the logged cause chain (the `1.7.0_gg7` Known Issues state this residual)".

---

round: 3 findings: 5

## Round 4

reviewer_agent_id: a32c2a44ee49eac0e

All 26 earlier findings were re-checked and the round-3 fixes (WR-14 to WR-17, IN-09) were read word by word against `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (`withheldJacksonChain`, `sameKindStandIn`, `withheldJacksonRoot`, `passesThroughUnchanged`, `awaitInsertResponse`, `awaitInsertResponseV1`, `insertWithWriteDeadline`, `writeColumnWithRecordContext`), `ClickHouseSinkConfig` (`addClickHouseSetting("send_progress_in_http_headers", "1", false)`, the help text), `ClickHouseHelperClient.insertBoundMs`, `git show v1.7.0_gg7:...` and the named tests. The WR-14 scope is correct as applied: the escape needs `async=false` and the default `send_progress_in_http_headers=1`, the 3007 ms figure is in the gg7 `CHANGELOG`, the stub pin and the escape pin exist under the names the row gives, and rows T5, T6, the closes table, the known-limits bullet, the `CHANGELOG` bullet and FORK-DELTA item 14 now say the same. The WR-16 description matches the code (top rebuilt with the value-free text, Jackson links above the root dropped, a Jackson root turned into a plain `IOException`, the bottom run of non-Jackson `IOException`s kept as the same objects, every other link replaced by a substitute of its kind with a withheld text); the WR-17 labels are pinned by `WriterRecordContextUnitTest` and `JacksonOverIoValueFreeUnitTest`; the WR-15 parenthesis is true (the live tests that look at a server error assert only that a client-side conversion failure is not a `ServerException`, none checks the text of a server rejection). The released `CHANGELOG` sections still equal `git show v1.7.0_gg7:CHANGELOG.md` (71626 bytes). The rollback block is byte-identical at `b4615700` and at `HEAD`. `git diff --shortstat v1.7.0_gg7 HEAD -- src/main` is 6 files, 428 insertions, 72 deletions. Four new findings remain: the IN-09 fix states as unmeasured a fact that FORK-DELTA records as measured, the shipped help text is unscoped where row T6 is now scoped, and two wording points.

### WR-18: The rollback bullet says the text of a Jackson failure on the JSON route "was not measured", but FORK-DELTA records that it was, and what it found

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:141` (rolling back brings back, rows U2 and U3)
**Issue:** The IN-09 fix ends the bullet with "whether the text of a Jackson failure on the `insertFormat=json` route carries a key into the task status, the dead letter queue headers or the logs was not measured (the `1.7.0_gg7` Known Issues state both residuals)". That repeats `git show v1.7.0_gg7:CHANGELOG.md` ("Whether its text carries a `Map` key into the task status, the dead letter queue or the logs was not measured"), a sentence that predates the measurement. FORK-DELTA's row on the JSON route (`docs/FORK-DELTA.md:49`) says the opposite for this release: "Measured over 8 shapes ... for seven of them, every Jackson-wrapped value-carrying failure on the four routes, the record value reaches the thrown message, the rendered stack trace and the Connect DLQ headers (and so the task status); no measured logger line carried it", and its "Upstream" cell says the route had no serialization catch. The probe tree (`fb5fa4cb`) has the `ObjectMapper.writeValueAsBytes` statements of the JSON route unchanged from `v1.7.0_gg7` (`git diff v1.7.0_gg7 fb5fa4cb -- .../ClickHouseWriter.java` has no line of them), and one of the seven shapes puts the sentinel in the Map key of the reference chain, which is the case the gg7 sentence asked about. So the one sentence of the rollback list that exists to tell the operator what a rollback gives back understates it: after the rollback a Jackson failure on a JSON insert route carries the record key or value in the task status and the dead letter queue headers, a fact the maintainers measured, and the note calls it unmeasured. This is also a disagreement between the note and FORK-DELTA on one fact (confirmations (d) and (m)). The first half of the bullet (a Jackson exception whose chain also holds a non-Jackson `IOException` passes through with its own message) is right.
**Fix:** Replace the closing clause by: ", and a Jackson failure on a JSON insert route carries the record's key or value in the exception message and so in the task status, the dead letter queue headers and the rendered stack trace, but not in the logs (measured on a probe tree whose JSON route is that of `v1.7.0_gg7`, in the failure shapes the probe drove; `FORK-DELTA.md`, the row on the JSON route; the `1.7.0_gg7` Known Issues record the same residual as not measured)". Keep the note free of the probe's planning identifiers.

### WR-19: The shipped help text of `clickhouseClientInsertTimeoutMs` states the client V2 progress-after-send limit without the `async=false` scope that row T6 now carries, and row T9 does not say so

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:68` (row T9) against `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:858` and `src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java:80`
**Issue:** After the WR-14 fix, rows T5 and T6, the closes table, the known-limits bullet, the `CHANGELOG` and FORK-DELTA item 14 (a) all say that a client V2 insert whose body has been sent and whose server streams progress headers is unbounded only with `async=false`, and is cut at the bound with `async=true`. The help text that the connector prints for the setting says, under "What it does not bound": "On client V2, an insert whose batch has been sent and whose server keeps sending progress headers runs to its natural end." The sentence has no `async` scope and none for the progress setting, and the text is pinned verbatim by the test, so it ships that way. The only `async=true` remark of the help text is in the `socket_timeout` sentence ("on client V2 with async=true the wait for the response still ends at this setting"), which a reader of the earlier sentence does not connect to it. Row T9 says the help text "says what the setting bounds on each client and what it does not" and is `read from the code (pinned by a test)`, so a reader takes the help text as the statement of the same limit that row T6 scopes. A client V2 operator who sets `async=true` and reads the Connect configuration documentation is told that nothing bounds a progress-streaming insert, while row T5 and the code (`awaitInsertResponse`) cut it. FORK-DELTA item 14 describes the help text unscoped without remarking on the difference.
**Fix:** Add one sentence to row T9: "Its sentence on a client V2 insert that streams progress headers after the send does not carry the `async=false` condition of row T6; with `async=true` that insert is cut at the bound (rows T5 and T6)." If the help text is changed instead, the pin phrase at line 80 of the test must change with it, which is a source change and not wanted at the gate; the note sentence is the lighter fix.

### IN-10: Row U2 and the `CHANGELOG` bullet read as if a `RetriableException`, `DataException` or `ConnectException` link becomes an `IOException` or a `RuntimeException`

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:48` (row U2); `CHANGELOG.md:43-47`
**Issue:** Both say that every other link "is replaced by one of the same kind (with the same error code for a ClickHouse server error, and otherwise an `IOException` or `RuntimeException`)". In `sameKindStandIn` a `RetriableException`, a `DataException` and a `ConnectException` are replaced by a new one of that class, a client V1 `ClickHouseException` and a client V2 `ServerException` keep the error code, and only a link of any other class becomes an `IOException` (if it was one) or a `RuntimeException`. FORK-DELTA's row on `doWriteColValue` lists the three kinds. In the note and the `CHANGELOG` the "otherwise" attaches to the error-code clause, so the three kinds that retry classification reads are not named, and the sentence beside it ("its retry classification does not change") cannot be followed from the list given. No code is wrong; the sentence is incomplete.
**Fix:** "every other link is replaced by one of the same kind with a withheld message: a `RetriableException`, a `DataException` or a `ConnectException` stays one, a ClickHouse server error keeps its error code, and any other link becomes an `IOException` or a `RuntimeException`". Apply to row U2 and to the `CHANGELOG` bullet.

### IN-11: Two statements about the ERROR lines of a deadline cut do not say `client V2`, which the note's own scoping rule requires

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:36` (Before you upgrade, step 4: "the same ERROR lines also appear on a deadline cut, and neither means the record is bad (section B)") and `:76` (section B, What to do, step 4: "expect them on a deadline cut too")
**Issue:** The note says "Every statement that holds only on client V2 says `client V2`". The write-phase deadline exists on client V2 only (`insertWriteDeadline`, `InsertWriteDeadline`; on client V1 the cut is the response wait of `awaitInsertResponseV1`, which comes after the body is in the pipe and raises no serialization failure), and row T10 is scoped `client V2 (RowBinary route)`. The two sentences point at row T10 but state the fact unscoped, so a client V1 operator reads a warning that does not apply to the connector.
**Fix:** Step 4: "the same ERROR lines also appear on a client V2 deadline cut"; section B step 4: "on client V2 expect them on a deadline cut too".

---

round: 4 findings: 4

## Round 5

reviewer_agent_id: a2456cb7a52ff4907

The five files were read first as they now are, then the round-4 fixes (`git diff 17fa56e9 HEAD -- docs CHANGELOG.md` touches only `CHANGELOG.md` and `docs/ROLLOUT-v1.7.0_gg8.md`; FORK-DELTA, the gg6 note and the gg1 note are unchanged since round 4), then rounds 1 to 4. The four round-4 fixes were checked word by word and are correct as applied. IN-10 (row U2 and the `CHANGELOG` bullet): `sameKindStandIn` replaces a `RetriableException`, a `DataException` and a `ConnectException` by a new one of that class, a client V1 `ClickHouseException` and a client V2 `ServerException` keep their error code, and every other link becomes an `IOException` (if it was one) or a `RuntimeException`; `withheldJacksonChain` keeps the top's `IOException` or `RuntimeException` category, drops every Jackson link above the root, turns a Jackson root into a plain `IOException` (with the client-timeout prefix kept) and leaves the bottom run of non-Jackson `IOException`s as the same objects; `Utils.handleException` reads the root cause (server error code first), any `RetriableException` in the chain, and the timeout prefix of an `IOException` root, all of which the rebuild keeps. WR-19 (row T9): the help text of `clickhouseClientInsertTimeoutMs` says "On client V2, an insert whose batch has been sent and whose server keeps sending progress headers runs to its natural end" with no `async` scope, and `async=true` occurs once in it, in the `socket_timeout` sentence; `awaitInsertResponse` cuts an `async=true` insert at the bound. IN-11: the write-phase deadline exists on client V2 only, so both sentences are now scoped correctly. WR-18: `git diff v1.7.0_gg7 fb5fa4cb -- src/main` touches neither `writeRecordsJson` nor the serializing statements of `MetadataFreeJsonWriter` (not in the diffstat), so the probe tree's JSON route is that of `v1.7.0_gg7`; the probe's `sentinel_in_message`, `sentinel_in_stack` and `sentinel_in_dlq_headers` are `yes` for seven shapes and `sentinel_in_logs` is `no` for all eight, which is what the bullet says. Two new findings remain, one of them in the sentence that WR-18's fix produced.

### WR-20: The rollback bullet says the `v1.7.0_gg7` Known Issues record the JSON-route residual as not measured; the `1.7.0_gg7` section has no such bullet

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:146` (Rolling back brings back, second bullet: "not run on `v1.7.0_gg7`, whose Known Issues record the residual as not measured")
**Issue:** The clause was written from round 4's own Fix text, which said "the `1.7.0_gg7` Known Issues record the same residual as not measured". It is not true of the `v1.7.0_gg7` release's Known Issues. The `# 1.7.0_gg7, 2026-11-02` section of `git show v1.7.0_gg7:CHANGELOG.md` (lines 1 to 77) has five Known Issues bullets (closing residuals, bounded-insert limits, many-to-one rename, retry budget, retriable `DataException`) and none about a Jackson failure on the `insertFormat=json` route. The sentence "On the `insertFormat=json` route a Jackson failure can still occur. Whether its text carries a `Map` key into the task status, the dead letter queue or the logs was not measured." is at line 288 of that file, inside the `# 1.7.0_gg4` section (lines 223 to 345), and is repeated in the `1.7.0_gg5` and `1.7.0_gg6` sections (`was not measured` at line 108 of the gg5 file and line 212 of the gg6 file). The `v1.7.0_gg7` rollout note and the gg7 FORK-DELTA do not state it either. So the bullet names as its source the Known Issues of the release being rolled back to, and an operator who opens that release's Known Issues finds nothing; the statement is true only of three earlier releases' sections that the gg7 changelog still carries as history. The factual core of the bullet (not run on `v1.7.0_gg7`, same statements in the code) is right; the attribution is the defect, in the one sentence that tells the operator what a rollback gives back.
**Fix:** Replace "whose Known Issues record the residual as not measured" with "the `1.7.0_gg4` to `1.7.0_gg6` sections of its changelog record the residual as not measured", or drop the clause and end the parenthesis at "not run on `v1.7.0_gg7`".

### WR-21: Row T9 describes the shipped help text as the statement of the limits, but its exactly-once clause says ClickHouse "drops duplicates" with no condition, against row T8

**File:** `docs/ROLLOUT-v1.7.0_gg8.md:68` (row T9) against `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:843` and `docs/ROLLOUT-v1.7.0_gg8.md:67` (row T8)
**Issue:** After the WR-19 fix row T9 says where the help text is stronger than row T6 (no `async=false` scope), and nowhere else. The same help text says, in its first paragraph, "in exactly-once mode the retry reuses insert_deduplication_token and ClickHouse drops duplicates", unconditionally. Row T8 states the same fact at a different level of confidence: "with `exactlyOnce=true` the retry reuses the deduplication token, and whether the server drops the repeat depends on the table", and its How-it-is-known cell says the outcome "depends on the engine and on `async_insert`, so no general rule is stated"; the gg7 note's row T8 says server deduplication "was not measured". The clause is carried unchanged from `v1.7.0_gg7` (`git show v1.7.0_gg7:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` lines 846 to 847) and `InsertTimeoutHelpTextPinUnitTest` keeps only the phrase `insert_deduplication_token`, so it ships in the text that row T9 says "closes row T14" and that the Connect configuration documentation prints. An operator who reads the help text and row T8 gets two answers to whether an exactly-once retry of a timed-out insert is safe, and the note says which limit of row T6 the help text leaves unscoped but not this one. This is the same kind of gap as WR-19, on the fact that decides whether a timed-out insert can duplicate rows.
**Fix:** Add one sentence to row T9: "Its clause that in exactly-once mode the retry reuses `insert_deduplication_token` and ClickHouse drops duplicates is carried from `v1.7.0_gg7` and states the drop without the condition of row T8: the token is reused (read from the code), and whether the server drops the repeat depends on the table." Changing the help text instead would be a source change.

round: 5 findings: 2

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

Round 2 verdicts (fresh reviewer; the round-1 verdicts above hold unless this part changes them).

(a) 22 rows: U1 to U4, T1 to T10, S1 to S4, P1, D1 to D3 (22 rows) read again against source and label. The WR-06, WR-07 and WR-09 splits are honest (T5: both gates and `socket_timeout=0` measured, the `async=true` wait read from `awaitInsertResponse`, which `insertBoundMs` does not gate; S3: the cut-insert consequence derived; U1: root column measured, Tuple element read from the code, `SlashColumnRoundTripTest` creates `id`, `a/b`, `a__b` and `café/温度` only, `sl` exists in `RawNameMatrix`). FAILED: T3 labels derived default-value claims as measured (WR-13); U2's TRACE list is incomplete (WR-12); U3 and step 4 deny the column in the exception (WR-11).

(b) Commands, numbers and versions: `git diff --shortstat` for `v1.7.0 HEAD` gives 364/47983/1260 and 43/3578/333, for `v1.7.0_gg7 HEAD` 36/5602/97 and 6/428/72 (per file 32/7, 8/5, 376/55, 10/0, 0/3, 2/2); the test-tree recount re-derived by script (143, 115, 28, 8 tail-identical, 20 others = 14 + 6); `ClickHouseSinkTaskStopUnitTest` 7; `git diff --quiet v1.7.0_gg7 HEAD -- ClickHouseFieldDescriptor.java` exits 0; `ClickHouseHelperClient` changed only by `insertBoundMs`; `gradle` and `build.gradle.kts` one added line each; the T-row figures appear in FORK-DELTA item 14. Passes.

(c) Applies-when against the code path: client V1 for T3 (`awaitInsertResponseV1`, three call sites), client V2 for T2 and T10, both clients for T5 with the `async=true` exception on client V2 only, `exactlyOnce=false` and `exactlyOnce=true` scoping on T8 and S3 unchanged. Passes.

(d) Rollback: order, the target `pre-v1.7.0_gg8` on `94834763` (tag `v1.7.0_gg7`), the gg7 asset facts and the rehearsal statement unchanged by the round-1 fixes. Passes; IN-08 is a wording point on which rows reverse.

(e) No fact about the `v1.7.0_gg8` `release object` and no promise of one in the note. FORK-DELTA now says "will be created" and "not yet published when this was written", which is the process, not a fact about an existing object (IN-01 fix correct). Passes.

(f) No advice empties or destroys data; the one destructive command is gated as in round 1. Passes.

(g) The ERROR line texts match `RECORD_CONTEXT_FAILURE_LOG_FORMAT` (line 626) and `WRITE_VALUE_FAILURE_LOG_FORMAT` (line 623) up to placeholder names; `Record serialization failed` is logged in `writeColumnWithRecordContext` and not for a `RetriableException`. Passes.

(h) The closes table now names F5, R8, T1 and T12, T14, R6, T11, the ping clause of T13, T15, Before-you-upgrade 2, What-to-do 3, the last statement of section F, the first, second and fourth known-limits bullets and four gg7 CHANGELOG statements; the new quotations are verbatim. No planning identifier in the note. Passes, with one residual (IN-07, gg7 T6).

(i) FORK-DELTA: the VERSION row (`v1.7.0_gg8`), the current-version parenthetical, the tag-pair commands, the test-tree row, the after-gg8 rows, the gg7-to-gg8 paragraph, the intro, the pointer and the next-syncer paragraph agree with the note and the `CHANGELOG`; items 13, 14, 17, 20 and 24 agree (`ESCAPED_NEWLINE` removed, three removed lines; `item 24` records the closed follow-up). The `DeclaredNullability` diff shows three removed lines. Passes.

(j) Released `CHANGELOG` sections equal `git show v1.7.0_gg7:CHANGELOG.md` byte for byte (script compare, 71626 bytes); the heading reads `Unreleased` by U-3; the three Phase 47 bullets are unchanged by the fixes. Passes.

(k) No host, connector or planning identifier in the note or the `CHANGELOG` section; `FORK-DELTA item 14`, `17` and `24` are document pointers. Passes.

(l) The Phase 48 edits of the gg6 and gg1 notes are unchanged since round 1 and correct (the tripwire test first appears at `v1.7.0_gg7`). Passes.

(m) Cross-document agreement on the change set, the version, the rollback target and the client scope holds: six `src/main` files, 428 and 72, `v1.7.0_gg8`, `pre-v1.7.0_gg8` on `94834763`, every client V1 and client V2 claim scoped. The `CHANGELOG` bullet on #640 is accurate about the Jackson rebuild, which the note's step 4 and section A advice contradict (WR-10). Passes on the four named items.

Round 3 verdicts (fresh reviewer; the earlier verdicts hold unless this part changes them).

(a) Every one of the 22 rows (U1 to U4, T1 to T10, S1 to S4, P1, D1 to D3 = 22 rows) was read once more against its source and label. FAILED: row T6 has an Applies when of `client V2` that is wider than its evidence and contradicts row T5 for `async=true` (WR-14), and row U2 leaves five of its claims unlabelled (WR-17). Rows T3, U1, T5 and S3 now carry honest per-claim labels; T6's `measured (stub only)` qualifier, S1's and S3's qualifiers and D3's label are honest.

(b) Commands, numbers and versions. Passes: the T-row figures (8299, 8380, 3050, 8141, 3034, 1.137 to 1.218 s, 0.313 to 0.325 s, 1.51 s, 1.010 to 1.082 s, 30034 to 30045 ms, 20005 ms, 2082 ms, 2500 and 4000 rows) are in FORK-DELTA item 14; 3007 ms is in the gg7 CHANGELOG section; `git diff --shortstat v1.7.0_gg7 HEAD -- src/main` is 6 files, 428 insertions and 72 deletions with the per-file split in the FORK-DELTA gg7-to-gg8 paragraph; the archive figures hold as in rounds 1 and 2; tags `pre-v1.7.0_gg8` and `v1.7.0_gg7` both resolve to `94834763`.

(c) Applies when against the code path. FAILED for row T6 (WR-14): the escape after the send needs `async=false` and the connector's default `send_progress_in_http_headers=1`; with `async=true` `awaitInsertResponse` cuts at the bound. The `insertBoundMs` gate and the `socket_timeout` scoping of T2, T3 and T5 hold on client V2 and client V1 as written; T10's cut exists only where the deadline is armed, which T2 scopes.

(d) Rollback. Order, target `pre-v1.7.0_gg8` on `94834763` (tag `v1.7.0_gg7`), the gg7 asset facts and the rehearsal statement are unchanged by the round-2 fixes and correct; T4 is now in both reversal lists. Passes; IN-09 asks for the data consequence of the Jackson rebuild's removal to be stated.

(e) No fact about the `v1.7.0_gg8` `release object` and no promise of one in the note or in FORK-DELTA ("not yet published when this was written"; the tag does not exist locally). Passes.

(f) No advice that empties or destroys data; the one destructive command, `git read-tree -u --reset`, is gated and acts on a new branch; no TRUNCATE or state-table advice, and the exactly-once KeeperMap state is untouched. Passes.

(g) ERROR line texts match `RECORD_CONTEXT_FAILURE_LOG_FORMAT` and the prefix of `WRITE_VALUE_FAILURE_LOG_FORMAT`; `Record serialization failed` is not logged for a `RetriableException`; the two-line order of T10 matches the catch nesting. Passes. What the exception, its cause chain and the logs contain was re-read against `withheldJacksonChain`, `sameKindStandIn`, `passesThroughUnchanged` and `doWriteColValue`: the step 4 and section A sentences are now true, the U3 column statement is true, and the remaining defects are the incomplete rebuild description (WR-16) and the unscoped CHANGELOG privacy claim (WR-15).

(h) The closes table names F5, R8, T1 and T12, T6, T11, the ping clause of T13, T15, Before-you-upgrade 2, What-to-do 3, T14, R6, the last statement of section F, the first, second and fourth known-limits bullets and four gg7 CHANGELOG statements; the note carries no planning identifier (scan for phase numbers, review ids, requirement and evidence ids, `.planning`). Passes.

(i) FORK-DELTA is unchanged since round 2 and consistent with the note and the CHANGELOG on the VERSION row, the current-version parenthetical, the tag-pair commands, the test-tree recount, the after-gg8 rows, the gg7-to-gg8 paragraph, the totals sentence, the gg8 intro, pointer and next-syncer paragraphs, and items 13, 14, 17, 20 and `item 24`; the one difference is that FORK-DELTA item 14, case (a), keeps the conditions that row T6 dropped (WR-14), and the `ESCAPED_NEWLINE` removal agrees. Passes apart from that difference.

(j) Released CHANGELOG sections equal `git show v1.7.0_gg7:CHANGELOG.md` byte for byte (script compare, 71626 bytes); the three Phase 47 bullets are unchanged by the round-2 fixes (`git diff ab0dc881 HEAD -- CHANGELOG.md` is one line of the #640 bullet); the heading reads `Unreleased` by U-3. Passes.

(k) No host, connector or planning identifier in the note or the `CHANGELOG` section; `FORK-DELTA` item pointers are document pointers. Passes.

(l) The Phase 48 edits of the gg6 and gg1 notes are unchanged and correct as published text: the tripwire is in `v1.7.0_gg7` and not in `v1.7.0_gg6`; gg6 row C2 points to items 17 and 24 and says what they say; the gg1 notice and item 6 label the `0.11.0` ZSTD decode as measured on the `v1.7.0_gg6` tree. Passes.

(m) Cross-document agreement on the change set (six `src/main` files, 428 and 72), the version (`v1.7.0_gg8`, `VERSION`, the `CHANGELOG` heading `Unreleased`), the rollback target (`pre-v1.7.0_gg8` on `94834763`, FORK-DELTA and note) holds. FAILED on two points: the client scope of the progress-header limit differs between row T5 and row T6 and between the note and FORK-DELTA item 14 (WR-14), and the `CHANGELOG` states the value-free claim without the scope that row U2 and the note give it (WR-15).

Round 4 verdicts (fresh reviewer; the earlier verdicts hold unless this part changes them).

(a) Every one of the 22 rows (U1 to U4, T1 to T10, S1 to S4, P1, D1 to D3 = 22 rows) was read again against its source and label. T6 now matches the code and its evidence: Applies when `client V2, async=false (the default)`, the escape needs the default `send_progress_in_http_headers=1` (`addClickHouseSetting(..., false)` keeps an operator value), `async=true` is cut by the ungated `awaitInsertResponse` and is labelled read from the code, the stub-only qualifier on the cut during the send is honest, and the 8299, 3050 and 8380 ms figures equal the probe lines. U2's label cell now labels every claim and each is pinned by the named tests. No row is stronger than its evidence. Passes; the open points are wording (IN-10) and the help-text difference (WR-19).

(b) Commands, numbers and versions. `git diff --shortstat v1.7.0_gg7 HEAD -- src/main` is 6 files, 428 insertions and 72 deletions; the rollback block is byte-identical at `b4615700` and at `HEAD`; the 3007 ms figure is in the gg7 `CHANGELOG`; the 8141 ms figure of row T3 is the client V1 base-tree probe line and equals, by coincidence, the client V2 figure of gg7 row T6; the released `CHANGELOG` sections equal the gg7 file (71626 bytes). Passes.

(c) Applies-when against the code path. T6 (`async=false`, client V2), T5 (the `async=true` exception, ungated), T3 (client V1, `awaitInsertResponseV1`, three call sites), T2 and T10 (client V2 deadline) hold. One unscoped sentence about the deadline cut in step 4 and in section B step 4 (IN-11). Passes apart from IN-11.

(d) Rollback. Order, the target `pre-v1.7.0_gg8` on `94834763` (tag `v1.7.0_gg7`), the gg7 asset facts and the rehearsal statement are unchanged and correct. FAILED for the list "Rolling back brings back": the rows U2 and U3 bullet calls the effect of the missing Jackson rebuild on the JSON route unmeasured, while FORK-DELTA records it as measured (WR-18).

(e) No fact about the `v1.7.0_gg8` `release object` and no promise of one in the note; FORK-DELTA says the tag and the release are created by the publish run and that the tree is published "once it is published". Passes.

(f) No advice that empties or destroys data; the one destructive command, `git read-tree -u --reset`, is gated by the clean-status step and runs on a new branch; the exactly-once KeeperMap state is untouched. Passes. With `exactlyOnce=false` the duplicate after a timed-out insert is stated as a consequence, not as advice.

(g) The ERROR line texts equal `RECORD_CONTEXT_FAILURE_LOG_FORMAT` and the prefix of `WRITE_VALUE_FAILURE_LOG_FORMAT`; `Record serialization failed` is not logged for a `RetriableException`; what the exception, its cause chain and the log contain was re-read against `withheldJacksonChain`: the statements are true, with one incomplete list of kinds (IN-10). Passes apart from IN-10.

(h) The closes table names F5, R8, T1 and T12, T6, T11, the ping clause of T13, T15, Before-you-upgrade 2, What-to-do 3, T14, R6, the last statement of section F, the first, second and fourth known-limits bullets and four gg7 `CHANGELOG` statements; the row for gg7 T6 now says "stays for `async=false`", which matches row T6; the note carries no planning identifier. Passes.

(i) FORK-DELTA (VERSION row, current-version parenthetical, tag-pair commands, test-tree recount, after-gg8 rows, gg7-to-gg8 paragraph, totals sentence, intro, pointer and next-syncer paragraphs, items 13, 14, 17, 20 and `item 24`) agrees with the note and the `CHANGELOG` on the progress scope (item 14 (a) carries `async=false` and the progress setting), the rebuild description, the client scope and the removal of `ESCAPED_NEWLINE`. FAILED in one place: FORK-DELTA records the JSON-route Jackson text as measured, the note's rollback bullet as not measured (WR-18).

(j) Released `CHANGELOG` sections are byte-identical to `git show v1.7.0_gg7:CHANGELOG.md`; the three Phase 47 bullets are unchanged; the heading reads `Unreleased` by U-3, and no round reviews that one line. Passes.

(k) No host, connector or planning identifier in the note or the `CHANGELOG` section (scanned for phase numbers, requirement and evidence ids, review ids, `.planning`, host names); the references to FORK-DELTA items are document pointers. Passes.

(l) The Phase 48 edits of the gg6 and gg1 notes are correct as published text: the tripwire test is in `v1.7.0_gg7` and not in `v1.7.0_gg6`; gg6 row C2 points to items 17 and 24, which say what the row says; the gg1 item 6 label "measured on the `v1.7.0_gg6` tree" is accurate. Passes.

(m) Cross-document agreement on the change set (six `src/main` files, 428 and 72), the version (`v1.7.0_gg8`, `VERSION`, the `CHANGELOG` heading `Unreleased`), the rollback target (`pre-v1.7.0_gg8` on `94834763`), the progress-header scope and what the exception, cause chain and log contain holds, with `client V1` and `client V2` scoping, `socket_timeout` handling and the `exactlyOnce=false` duplicate consistent. FAILED on two points: the rollback bullet against FORK-DELTA on the measured JSON-route text (WR-18), and the note's T6 scope against the shipped help text that row T9 describes (WR-19).

Round 5 verdicts (fresh reviewer; the verdicts above hold unless this part changes them).

(a) 22 rows: U1 to U4, T1 to T10, S1 to S4, P1, D1 to D3 (22 rows) read again against source and label. The round-4 edits (U2's list of kinds, T9's new sentence) are accurate. FAILED only through WR-21: row T9 labels the help text `read from the code (pinned by a test)` and lists where it is stronger than row T6, but not where its exactly-once clause ("ClickHouse drops duplicates") is stronger than row T8. No row is stronger than its evidence otherwise; qualifiers (`stub only`, `Kafka 3.9 line only`, `one probe, not pinned`) are honest.

(b) Commands, numbers and versions: `git diff --shortstat v1.7.0_gg7 HEAD -- src/main` is 6 files, 428 insertions, 72 deletions; `VERSION` reads `v1.7.0_gg8`; `git rev-parse pre-v1.7.0_gg8 v1.7.0_gg7` gives `9483476322e0d331e81c5b92ac266a18f0f01450` twice (`94834763`); `git diff --quiet v1.7.0_gg7 HEAD -- .../ClickHouseFieldDescriptor.java` exits 0; the closes table's quoted gg7 statements are substrings of the gg7 note and changelog (one quotation carries an ellipsis by design); the T-row durations, case counts and archive figures were not changed by the round-4 fixes and still match FORK-DELTA item 14 and the evidence. Passes; the one wrong source attribution is WR-20.

(c) Applies-when against the code path: T2 and T10 client V2 (the write-phase deadline, `insertWriteDeadline`, exists on client V2 only), T3 client V1 (`awaitInsertResponseV1`), T5 both clients with the `async=true` wait on client V2 (`awaitInsertResponse` is not gated by `insertBoundMs`), T6 client V2 with `async=false`, `client_version` default `V1` and any other value selecting client V2 (`ClickHouseWriter.java:107`), the two sentences changed for IN-11 now say `client V2`. `socket_timeout` handling and `exactlyOnce=false` scoping unchanged. Passes.

(d) Rollback: the order of steps, the target `pre-v1.7.0_gg8` on `94834763` (tag `v1.7.0_gg7`), the gg7 asset facts and the rehearsal statement are unchanged since round 4; the only edit to the rollback section since round 4 is the second reversal bullet (WR-18 fix), which is right in its facts and wrong only in the attribution of WR-20; the third bullet's `client V2` scoping is right. Passes except WR-20.

(e) No fact about the `v1.7.0_gg8` `release object` and no promise of one: the note says only that the tag does not exist before publication and that the rollback tag is pushed when the release is published. Passes.

(f) No advice that empties or destroys data; the one destructive command, `git read-tree -u --reset`, is gated by the clean-status step and runs on a new branch; the exactly-once KeeperMap state is untouched. Passes.

(g) ERROR line texts: `Record serialization failed: topic [..] partition [..] offset [..] column ..: exception <class>` matches `RECORD_CONTEXT_FAILURE_LOG_FORMAT` (line 626); `Error writing value of ...` matches `WRITE_VALUE_FAILURE_LOG_FORMAT` (line 623); neither carries a value, a message or a throwable. Passes.

(h) The closes table names F5, R8, T1 and T12, T6, T11, the ping clause of T13, T15, Before-you-upgrade 2, What-to-do 3, T14, R6, the last statement of section F, three known-limits bullets and five gg7 changelog statements; the gg8 note carries no planning identifier (only 8-hex commit abbreviations, branch and tag names and its own row ids). Passes.

(i) FORK-DELTA is unchanged since round 4 and still agrees with the note and the `CHANGELOG`: the VERSION row, the current-version parenthetical, the tag-pair commands, the test-tree recount, the after-gg8 rows, the totals sentence, the gg8 intro, pointer and next-syncer paragraphs, and items 13, 14, 17, 20 and 24 (`item 24` records the closed follow-up). Its row on `doWriteColValue` lists the same kinds as row U2 now does, and its row on the JSON route matches the rollback bullet. Passes.

(j) Released `CHANGELOG` sections: the file from the `# 1.7.0_gg7, 2026-11-02` heading down equals `git show v1.7.0_gg7:CHANGELOG.md` (`cmp` reports only that the tag copy ends 6 bytes earlier, the missing trailing newline of the copy and of the awk extract); the three Phase 47 bullets are unchanged by the round-4 edit (the only hunk is the IN-10 sentence of the Bug Fixes bullet); the heading reads `Unreleased` by U-3. Passes.

(k) No host, connector or planning identifier in the note or the `CHANGELOG` section; the references to FORK-DELTA items are document pointers. Passes.

(l) The Phase 48 edits of the gg6 and gg1 notes are unchanged since round 4 and correct as published text. Passes.

(m) Cross-document agreement on the change set (six `src/main` files, 428 and 72), the version (`v1.7.0_gg8`, `VERSION`, `CHANGELOG` heading `Unreleased`), the rollback target (`pre-v1.7.0_gg8` on `94834763`), the client scope (`client V1` and `client V2`), the exception contents (the IN-10 wording in row U2, the `CHANGELOG` bullet and FORK-DELTA agree), the `Record serialization failed` and `ESCAPED_NEWLINE` statements, the label strengths and the rollback consequences. FAILED on one point: the help text's exactly-once clause against row T8 (WR-21); the attribution in WR-20 is a single-document error.

---

_Reviewed: 2026-11-06T16:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
