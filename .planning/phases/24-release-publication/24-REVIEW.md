---
phase: 24-release-publication
reviewed: 2026-10-20T18:40:34Z
depth: standard
diff_base: 07bf1e7695cc32fa7239b0b9ee0feed06c079f63
files_reviewed: 6
files_reviewed_list:
  - CHANGELOG.md
  - VERSION
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg3.md
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java
  - .planning/scripts/fork-anchors.txt
findings:
  critical: 0
  warning: 10
  info: 7
  total: 17
status: issues_found
---

# Phase 24: Code Review Report

**Reviewed:** 2026-10-20
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Scope: `git diff 07bf1e7695cc32fa7239b0b9ee0feed06c079f63 HEAD` over the six files above. The only non-`.planning` paths Phase 24 changed are `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg3.md` and the one new live test; `src/main`, `src/testFixtures`, the build, `.github` and the gg1 and gg2 notes are untouched. This round was done inline from `gsd-code-reviewer.md` (see the SUMMARY's Skill adaptation).

The operator command text was read closest. Every `sql` and `sh` block of the note is also in `24-COMMAND-EVIDENCE.md` (the check `DOCS-FROZEN-OK` compares them as text), and the detection queries were run against fixture tables and look-alikes there (E-13, E-15, E-16, E-21, E-23). I re-read the narrowed backslash predicate of the item 3 shape query (backslash followed by `n`, E-13) against the broad variant: the two releases differ only where the declared type text holds that pair, and the broad variant would list four tables whose bytes are identical, so the narrow query is the correct one. The rollback in item 7 is the command sequence of E-22, and the artifact wording matches E-04.

No Critical finding. Two Warnings are overclaims where the three documents say more than the measurements show, in the way the review brief asked to be checked ("measured", "never", "every"). Three Info findings are wording or convention points.

## Warnings

### WR-01: The `size` defect is stated without the route that was measured, and the note contradicts its own qualifier

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:151`, `CHANGELOG.md:38-40`, `docs/FORK-DELTA.md:173`
**Issue:** E-15 measured the inserts with client V2 on the default route only. The note's "What to do" says "Inserts into such a table fail whatever the connector settings", and in the same sentence says the explicit `insertFormat=json` route, which runs no table description, "was not measured". The two halves contradict each other, and "whatever the connector settings" is the stronger claim. The CHANGELOG Known Issues bullet says "Every insert into such a table fails" and FORK-DELTA Known costs item 10 says "every insert into the table fails", with no route in either (item 10 names client V2 only for the measurement). Client V1 was not measured either. An operator who runs the explicit JSON route would read these documents as saying that their table fails.
**Fix:** Say what was measured and what was not, in all three places, with the codes kept:
```text
Inserts into such a table fail on the route that was measured, the default route with client V2. Client V1 and the route of an explicit `insertFormat=json`, which runs no table description and so logs no WARN, were not measured.
```
CHANGELOG: "On the default route with client V2, every insert into such a table fails with the server error code 32 or 33 and stores no row, on `v1.7.0_gg2` and on this release alike." FORK-DELTA item 10: add the route and a sentence that client V1 and the explicit JSON route have not been measured.

### WR-02: Item 4 says `v1.7.0_gg2` "never stored a batch of that shape" for all 14 shapes, and E-11 inserted four

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:234`
**Issue:** The upgrade bullet starts "`v1.7.0_gg2` never stored a batch of that shape" and then says "A range of such a table that `v1.7.0_gg2` left in flight therefore holds no row". E-11 inserted four of the 14 differing shapes (r02 in two styles, r05, r07) and says the other four root shapes (r03, r09, r10, r12) "were not inserted in this plan; their insert outcome is by analogy, not measured", and that a gg2 batch whose doubled flags happened to parse was not measured. Item 3 of the same note hedges correctly ("likely, not measured"), so the note disagrees with itself and with its evidence. The conclusion "no clean stop is needed" still holds, because a retry under the same token either lands or is dropped, but the stated reason is stronger than the measurement.
**Fix:** Limit the sentence to what was measured and point at item 3 for the rest:
```text
In the measurement, the four such shapes that were inserted failed on `v1.7.0_gg2` with code 33 and stored 0 rows, so a range of such a table that `v1.7.0_gg2` left in flight holds no row and the retry by `v1.7.0_gg3` lands once, with the declared bytes. The other differing shapes were not inserted (item 3), and the same outcome is likely for them, not measured.
```

## Info

### IN-01: The CHANGELOG Bug Fixes bullet states the insert outcome for every `Nullable(Tuple)` with such a name

**File:** `CHANGELOG.md:45-47`
**Issue:** "now pairs with its declared type, so its inserts store the row instead of failing with code 33" holds for the four shapes inserted in E-11. The pairing itself was measured for all 14 (E-10), but item 3 of the note hedges the insert outcome for the rest. The same wording issue as WR-02, in the CHANGELOG.
**Fix:** "...now pairs with its declared type, so an insert into it stores the row instead of failing with code 33 (measured for a `Nullable(Tuple)`, an `Array` of it and a `Map` of it), and the fallback WARN for such a table stops."

### IN-02: FORK-DELTA describes the release as published and `pre-v1.7.0_gg3` as existing before plan 24-07 makes it so

**File:** `docs/FORK-DELTA.md:5`, `docs/FORK-DELTA.md:211`
**Issue:** The intro sentence ("is published ... as `v1.7.0_gg3`") and the gg3 paragraph of "For the next syncer" are written in the tense of publication, and the tag `pre-v1.7.0_gg3` is local until plan 24-07. This is the same convention the gg2 text used before its tag existed, and the document is part of the tree that the tag names, so it is true from the moment the tag exists. The line 110 text is hedged ("once it exists"). The only risk is a reader of an unpublished branch.
**Fix:** None in the document. Plan 24-06's approval bundle states that these lines become true at plan 24-07.

### IN-03: Planning identifiers in the new live test's Javadoc and display names

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java:34`, `:239`, `:230`, `:255`, `:276`, `:291`
**Issue:** The class Javadoc cites `D-03` and `D-14`, a comment cites `22-RESEARCH Pitfall 8`, and each `@DisplayName` starts with `D-03:`. These identifiers resolve only in `.planning`, and AGENTS.md asks for comments that carry invariants. The same convention runs through the Phase 21 to 23 tests (for example `COLS-12 (D-05)`), and 22-REVIEW IN-05 already carries the same point for `src/main`.
**Fix:** Carried with 22-REVIEW IN-05: drop the identifiers in the next edit of the file. Editing the test now would re-open its live run and the FORK-DELTA raw numbers for a naming point.

## Mandated confirmations (20-D-07)

1. **The headline and its Map-key exception match E-03, and no nonexistent connector key is told. Holds.** The CHANGELOG headline and the note's exception section state the Map key on the `input_format_binary_read_json_as_string` route and name the same three places (task status, DLQ stack-trace header, `Error inserting records` throwable) and the same two non-places. The connector key is `clickhouseSettings` (`ClickHouseSinkConfig.CLICKHOUSE_SETTINGS`), and `ClickHouseSinkConfig` reads the setting as `true` or `1`. `JsonAsStringKeyResidualLiveTest` is the cited pin, and E-03 records `D-04-BRANCH=1`.
2. **Every quoted query and command has matching evidence, and the detection queries were proven against look-alikes. Holds.** `DOCS-FROZEN-OK` finds no `sql` or `sh` block of the note that is missing from the evidence and no `pending (` verdict. The queries were run on fixture tables and on a look-alike-only database (E-13, E-15, E-16), the greps on scratch files (E-21, E-23) and the state-table query on a table created by the gg3 code (E-14).
3. **No TRUNCATE or unscoped DELETE of the KeeperMap table. Holds.** The note tells the operator not to empty or edit the state table, and contains no `TRUNCATE`, `DELETE`, `DROP TABLE` or `ALTER TABLE` statement. The CHANGELOG's only `truncate` is the Debezium event in a released section.
4. **The rollback uses `read-tree` with `pre-v1.7.0_gg3`, E-22 proves it, and the artifact wording matches E-04. Holds.** Item 7 is the command sequence E-22 ran (fetch, fast-forward, clean check, `git read-tree -u --reset pre-v1.7.0_gg3`, commit, the equality check, the revert and the `show`). `git diff --name-only v1.7.0_gg2 6473e24f` prints `build.gradle.kts` and `gradle/libs.versions.toml` only, and E-04 shows the same shipped jar (7375 entries, none added, removed or changed).
5. **Released CHANGELOG sections and the gg1 and gg2 notes are unchanged. Holds.** The CHANGELOG from `# 1.7.0_gg2, 2026-10-14` down equals `P24_BASE`, and `git diff --quiet` over the two notes and every protected path is clean.
6. **No secret, host, record value or production identifier. Holds.** The only literals are the four test sentinels in the test and in sentinel-only evidence excerpts, a server version, commit hashes and placeholders. A search for key, token, host and address patterns finds nothing.
7. **The three documents agree with each other and with the evidence. Does not hold in two places.** WR-01 (the route of the `size` defect) and WR-02 (item 4, "never stored") are findings. Everything else agrees: the codes 32 and 33, the 14 and 30 table counts, the three dependency versions, the MBean and client name, the raw numbers (`RAW-NUMBERS-GG3-OK`) and the Known costs numbering.
8. **The new test follows the AGENTS.md test rules. Holds.** It extends `ClickHouseBase`, uses the real writer and no Mockito, `@Disabled` or assumption, has four descriptive negative and positive cases, extracts every string constant, shares helpers, asserts the Jackson class so a green run cannot come from a failure that never reached Jackson, and checks that no row landed. It adds a file and edits no existing test. IN-03 is the only point.
9. **The size-element item offers no workaround, and the escaped-name limitation has no CHANGELOG line. Holds.** Item 2 says "This note offers no workaround and no schema change", FORK-DELTA item 10 says "no fix and no workaround", and the CHANGELOG has the Known Issues bullet for `size` and no line for the escaped-name limitation, as FORK-DELTA item 11 says.

## Round 2: incremental review of the fix commits (2026-10-20)

Scope: `0c08e663` (WR-01), `cd40803c` (WR-02) and `6ee4a22a` (IN-01), nothing else. No new finding, so the counts in the frontmatter stay at 2 warnings, 3 info and a total of 5, and no heading is added.

- **WR-01 fix.** The note, the CHANGELOG bullet and FORK-DELTA item 10 now give the same scope: the default route with client V2 was measured (E-15), client V1 and the explicit `insertFormat=json` route were not. The note keeps the decision D-02 phrase "whatever the connector settings", now attached to the measured route, so it no longer contradicts the qualifier after it. The codes 32 and 33 are in every unit that mentions `size0`, and `consistency.py` is clean. The fix suggested in round 1 dropped the D-02 phrase; the committed text keeps it, because D-02 is a locked decision and the scoping alone removes the contradiction.
- **WR-02 fix.** Item 4's upgrade bullet now says what E-11 measured (four inserted shapes, code 33, 0 rows) and sends the other differing shapes to item 3 as "likely, not measured". It no longer says "never stored". The conclusion "no clean stop is needed" is unchanged and still rests on the shared token.
- **IN-01 fix.** The CHANGELOG bullet names the three shapes that were inserted (a `Nullable(Tuple)`, an `Array` of it and a `Map` of it), which is what E-11 measured.
- Re-run on the committed tip: `ROLLOUT-FRAME-OK`, `ITEMS-1-7-OK`, `ITEMS-2-3-OK`, `ROLLOUT-T3-OK`, `ROLLOUT-FINAL-OK`, `CHANGELOG-GG3-OK`, `FORK-DELTA-ROWS-OK`, `FORK-DELTA-PATHS-OK` and `RAW-NUMBERS-GG3-OK` (E-40).

**Confirmations, round 2.** Items 1 to 6, 8 and 9 above are unchanged by the fixes: the headline and the Map-key exception of `input_format_binary_read_json_as_string` still match E-03 and `JsonAsStringKeyResidualLiveTest`; every quoted query and command is still in the evidence; there is still no TRUNCATE or DELETE of the KeeperMap table; the `pre-v1.7.0_gg3` rollback is untouched (E-22, E-04); the released CHANGELOG sections are byte-equal to `P24_BASE`; the new test and the size-element no-workaround wording are untouched. Item 7 now holds: the three documents agree with each other and with E-11 and E-15 after WR-01 and WR-02. Round 2 is the last round needed; no Critical or Warning remains.

## Round 3: independent gsd-code-reviewer pass (2026-10-20)

Scope: `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg3.md` as published in merge `3376a18f` (unchanged since `cc266815`), read in full against `24-COMMAND-EVIDENCE.md` (E-02 to E-05, E-10 to E-16, E-20 to E-23, E-33, E-40, E-60 to E-72) and against `src/main` where a document quotes code (`DeclaredNullability` WARN text, `WithheldMessageException`, `shouldSkipDescribeRow`, `SIZE_FIELD_MATCHER`, the catalog versions, the `jacocoForkReport` list). This pass did not repeat rounds 1 and 2. The round-2 fixes for WR-01 and WR-02 hold. No Critical finding: no command or query does anything other than what its text says, every `sql` and `sh` block matches its evidence entry, all intra-document anchors and the gg1 and gg2 note anchors resolve, the version strings, SHAs, table counts (44, 30, 14, 13, 19) and raw numbers agree with the evidence, and the item 7 command sequence is the one E-22 ran. The findings are five Warnings (three statements that go beyond what E-10, E-12 and E-15 measured, one procedure whose two halves disagree, one query that cannot cover a connector with more than two topics) and two Info items. These documents are already published, so each is for a later docs fix.

## Warnings (round 3)

### WR-03: Item 3 says "every root that is not `Nullable`" is identical on both releases, and four of the 14 differing roots are not `Nullable`

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:167`
**Issue:** The sentence reads "The other 30, among them every table with a plain name, every root that is not `Nullable`, and the `Enum` label shapes, are identical on both releases." E-10 lists the 14 differing tables as r02, r03, r05, r07, r09, r10 and r12 in two name styles each. Of those roots, `r05` is `Array(Nullable(Tuple(...)))`, `r07` is `Map(String, Nullable(Tuple(...)))`, and `r10` (`Tuple(x Nullable(Tuple(n Int32)))`) and `r12` (`Tuple(n Int32, y Nullable(Tuple(c Int32)))`) are plain `Tuple` roots. None of the four is a `Nullable` root, and all differ (E-10 table: `d09_r05_*`, `d09_r07_*`, `d09_r10_*`, `d09_r12_*`). E-10's own wording is "every non-`Nullable` root ... (r01, r04, r06, r08, r11)", which means roots that contain no `Nullable(Tuple)` at all. The note shortened it into a statement that is false as written, and it contradicts the first paragraph of the same item ("the Tuple is the column itself, an `Array` element, a `Map` value or a field of another Tuple"). An operator who reads "root that is not `Nullable`" as the top-level type could dismiss an `Array(Nullable(Tuple(...)))` or `Tuple(x Nullable(Tuple(...)))` table, which is exactly the shape that needs the item 4 clean stop on a rollback.
**Fix:**
```text
The other 30, among them every table with a plain name, every table whose type holds no `Nullable(Tuple(...))` at any depth, and the `Enum` label shapes, are identical on both releases.
```

### WR-04: "Every insert into such a table fails" is stated for all `size` shapes, and E-15 inserted four of 13 and the same documents say a shorter row might parse

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:115` and `:121`, `CHANGELOG.md:38-42`, `docs/FORK-DELTA.md:173`
**Issue:** Round 2 scoped the claim to the default route with client V2, but the quantifier over shapes is untouched. Item 2 says "every insert into that table fails with the server error code 32 ... or code 33 ... and stores no row" for every shape in the first paragraph (a `Nullable(Tuple)`, a `Map` value, a nested `Tuple`, `Array(Array(Tuple))`, `size12`, and so on). E-15 inserted four shapes only (`s4_a01`, `s4_a04`, `s4_a08`, `s4_a13`; the other nine affected tables were detected by the WARN oracle and not inserted). The same item then says, five lines later, "A wrongly stored value is possible in principle when the shorter row happens to parse; that case was not measured". Those two statements cannot both hold: if every insert fails there is no shorter row that parses. The CHANGELOG bullet ("every insert into such a table fails with the server error code 32 or 33 and stores no row") and FORK-DELTA item 10 ("every insert into the table fails ... A wrongly stored value is possible in principle") carry the same contradiction. The operator-relevant risk is the unmeasured silent-wrong-value case, and the universal wording hides it.
**Fix:** In all three places, say what was inserted and keep the hedge as the live possibility:
```text
Inserts that were run into such tables (four shapes: `Tuple(size ...)`, `Array(Tuple(size ...))`, `Array(Tuple(size1 ...))` and `Array(Tuple(v String, size1 ...))`) failed with the server error code 32 or 33 and stored no row. The other affected shapes were detected by the WARN and were not inserted, and a shorter row that happens to parse could store a wrong value instead of failing; neither was measured.
```

### WR-05: Item 4 sends `exactlyOnce=false` connectors to the token for a rollback, and E-12 measured that the token does not save them for the differing shape

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:239` (and the scope sentence at `:243`)
**Issue:** The "Am I affected?" paragraph says a connector with `exactlyOnce=false` "keeps no offset state, so the procedure below does not apply to it; a range that Kafka delivers again is handled by the token as described in item 1 of the gg2 note". Item 1 of the gg2 note says the server "decides by the token and ignores the bytes". For the rollback direction of the differing shape that is false, and E-12 shows it: the probe ran `v1.7.0_gg2` with `exactlyOnce=false` ("V2, `exactlyOnce=false`, which still sends the token"), the range had landed under its token, and the retry "failed code=33 rows=1" while the control was "dropped". So a `exactlyOnce=false` connector that is rolled back, with a differing-shape table and a range Kafka delivers again (an unclean stop, a rebalance, or offsets not yet committed), fails or dead-letters even though the rows landed. The note's own rollback bullet in the same item explains the mechanism ("the server reads the bytes before it looks at the token") and then excludes the one mode in which it was measured. The note gives that operator no instruction at all.
**Fix:** Replace the exclusion with a scoped one and give the `exactlyOnce=false` instruction:
```text
A connector with `exactlyOnce=false` has no state table to query, so the check below does not exist for it, but the failure does: a range that Kafka delivers again to `v1.7.0_gg2` for a table of the item 3 shape fails with code 33 even when `v1.7.0_gg3` stored its rows (E-12 ran with `exactlyOnce=false`). Stop its tasks cleanly, so that the offsets are committed, before you redeploy.
```

### WR-06: Item 7 makes the branch rollback step 1 and the in-flight check step 2, while item 4 says "do not roll back yet" and has the operator restart the `v1.7.0_gg3` jar

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:256-257` and `:315-323`
**Issue:** Item 4 says "do not roll back yet" on a non-empty result and "Repeat until it is empty, then roll back". Item 7 step 1 is `git read-tree -u --reset pre-v1.7.0_gg3` plus a commit on `custom_build`, and only step 2 says "Before you redeploy, run the check of item 4". The two use "rollback" for different acts (the branch commit versus the redeploy), and a reader who follows item 7 in order commits the rollback first. That commit removes `docs/ROLLOUT-v1.7.0_gg3.md` from the branch (item 7 says so itself), so the document that holds the check and the item 3 shape query is gone from the working tree at the moment the reader needs it, and item 4 step 3 ("start the `v1.7.0_gg3` jar again") now has no tree to build it from unless the reader kept the archive or checks out tag `v1.7.0_gg3`. Item 7 step 3 covers the same question for the `v1.7.0_gg2` archive and says nothing for `v1.7.0_gg3`. E-22 proves the git commands, not the order.
**Fix:** Move the check before the branch commit and keep the artifact:
```text
1. Before you change the branch, run the check of item 4 for each connector that needs it, and keep the `v1.7.0_gg3` archive you deployed (or rebuild it from tag `v1.7.0_gg3`), because the check may need you to start it again.
2. Back out the whole release in one commit on `custom_build` ... (the current step 1)
3. Redeploy the artifact built at tag `v1.7.0_gg2` ... (the current step 3)
```
and in item 4 write "do not redeploy `v1.7.0_gg2` yet" in place of "do not roll back yet".

### WR-07: The state-table query has room for two topics only, and its "Empty result" sentence says it is safe to roll back

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:246-256`
**Issue:** The query ends in `IN ('<topic>', '<topic>')` and the text says "Replace each `<topic>` with one of your topics, writing the same topic twice if you have only one". A connector that consumes three or more topics (a topic list or a `topics.regex`) has no instruction to add entries. If the operator fills the two slots, a batch in flight on the third topic does not appear and the note then says "Empty result: no batch of those topics is in flight, so it is safe to roll back", which is the unsafe outcome the check exists to prevent. E-14 proves the query for one topic and a look-alike prefix topic only. The query also names the state table without a database, while E-14 notes that it "runs in the database the state table lives in (the probe's connection database)"; the connector creates it in the database of its connection, so an operator connected to another default database gets an unknown-table error (harmless, read-only, but not obvious from the text). The text is inherited from item 1 of the gg2 note, where the same gap exists.
**Fix:** Say "add one `'<topic>'` entry for each topic the connector consumes" and "run it in the database the connector connects to, or write `` `<db>`.`<zkDatabase>` ``". Phrase the empty-result sentence as "no batch of the listed topics is in flight".

## Info (round 3)

### IN-04: The known-limitation sentence lists a backslash, a slash and a tab, and the table below it also requires the escaped form for a line break

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:188` (against `:194` and `:205-207`)
**Issue:** "A backslash, a slash or a tab in a column or element name therefore means the Kafka field must use the connector's escaped form as its name." The table two lines later has a line-break row (`a\nb`), E-16 measured it (`e5_e03`, root name stores the DEFAULT with the natural field), and the detection query lists `position(name, '\n')`. Only the lead sentence omits it.
**Fix:** "A backslash, a line break, a slash or a tab ...".

### IN-05: The CHANGELOG Build and CI bullet says the tests no longer mock "the ClickHouse client"

**File:** `CHANGELOG.md:59`
**Issue:** "The writer error-path tests no longer mock the ClickHouse client inline." FORK-DELTA's Test harness row (HARN-08) says what changed: the tests stopped inline-mocking `ClickHouseHelperClient`, the connector's own class, in favour of a subclass (`CountingHelperClient`). "The ClickHouse client" reads as the ClickHouse Java client library, which the same release does not touch. It is a test-only line in an operator-facing file.
**Fix:** "The writer error-path tests no longer inline-mock the connector's `ClickHouseHelperClient`; they use a subclass."

## Confirmations, round 3

- The headline Map-key exception, the five-row destination table and the three places match E-03 (`D-04-BRANCH=1`) and the `WithheldMessageException` code (`toString()` is `<class> (message withheld: may contain record data)`, suppression off).
- The `sql` and `sh` blocks were compared with E-13, E-14, E-15, E-16, E-20, E-21 and E-23 line by line; there is no difference. The `Declared Tuple element(s) ... have no matching DESCRIBE sub-column` and `Could not take element nullability ...` templates match `DeclaredNullability.UNPAIRED_ELEMENT_WARNING` and `FALLBACK_WARNING`. `shouldSkipDescribeRow` drops `.null`, `.size0` and `.size`, and `Table.SIZE_FIELD_MATCHER` is `.+\.size[0-9]+$` with the `ARRAY` and `MAP` parent cases, which agrees with the three descriptions of the defect.
- The version-catalog row of FORK-DELTA (Jackson `2.22.3`, `lz4Java 1.12.0`, okhttp `5.5.0`, org.json `20260814`, mockito `5.24.0`, lombok `1.18.48`, spotless `8.10.4`, upstream values from `v1.7.0`) and `build.gradle.kts` (`slf4j-reload4j:2.0.20`) match. Raw numbers 130/22783/534 and 33/2245/145, the 38/14/11 split and the 63/25/8/33 arithmetic match E-33 and E-72; the `src/main` split (14 import-only, 11 modified, 8 new) adds up.
- Release facts match E-70 to E-72: `pre-v1.7.0_gg3` at `6473e24f` on origin before the merge, `v1.7.0_gg3` at `3376a18f`, the CHANGELOG date `2026-10-20`, and the gg1 and gg2 note anchors the new note links to (`#item-1-in-flight-batches-and-the-deduplication-token`, `#item-7-jmx-mbean-name-and-client-name`) exist. Item 6 of the gg2 note does describe the older rollback to `v1.7.0_gg1`.
- No TRUNCATE, DELETE, DROP or ALTER anywhere in the three documents; the state-table query is a `SELECT`.

## Round 4: incremental review of the round 3 fixes (2026-10-20)

Scope: the eight `docs(quick-261020-vcr)` commits `40699434` (WR-06), `7987a093` (WR-03), `6df28936` (IN-04), `12e9790d` (WR-04), `83cbaf94` (WR-05), `72773c7c` (WR-07), `1ba3fe96` (IN-05) and `bbfb0a86` (the revision notice), read as `git diff 3376a18f bbfb0a86 -- docs/ROLLOUT-v1.7.0_gg3.md CHANGELOG.md docs/FORK-DELTA.md` and in full context in the current files (the notice and items 2, 3, 4 and 7 of the note, the 1.7.0_gg3 section of the CHANGELOG, Known costs item 10 of FORK-DELTA), against `24-COMMAND-EVIDENCE.md` E-10 to E-16 and E-22, and against `Processing.java` where the state-table key is built. The fixes resolve what round 3 raised, but three of them stop one step short of their own reach, and a sweep for the same defect classes finds five new items: three Warnings (WR-08 to WR-10) and two Info items (IN-06, IN-07). The frontmatter counts are updated to match.

- **WR-03 holds.** Item 3 now says the other 30 tables include "every table whose type holds no `Nullable(Tuple(...))` at any depth". That is E-10's own reading: the 30 identical trees are the plain-name tables, the roots r01, r04, r06, r08 and r11 in both styles, the four enum-label shapes and the four `size` shapes, and none of them holds a `Nullable(Tuple(`. The four differing roots that are not `Nullable` at the top (r05, r07, r10, r12) are no longer excluded by the wording.
- **WR-04 holds for the four places it named, with residuals.** The note (item 2), the CHANGELOG Known Issues bullet and FORK-DELTA item 10 now agree with each other and with E-15: four inserted shapes (`s4_a01`, `s4_a04`, `s4_a08`, `s4_a13`) failed with code 32 or 33 and stored no row, the other nine affected shapes were detected by the WARN only, and the wrong-value case was not measured. "Nine" is right (13 affected minus 4 inserted). The hedge is now a live possibility instead of a contradiction. Residuals the fix left: the closing sentence of item 2 (WR-08), the same universal "every insert failed" pattern for the item 3 shape (WR-09), and two FORK-DELTA rows (IN-06). The `shouldSkipDescribeRow` row and the declared-nullability row do overclaim, mildly: see IN-06.
- **WR-05 holds.** Item 4 no longer excludes `exactlyOnce=false`; it says the failure exists without the state table, ties the measurement to `exactlyOnce=false` in prose, and gives the instruction (stop cleanly so the offsets are committed). Turning the evidence ID into prose loses nothing: the sentence names the mode, the token and the rollback bullet it rests on. The clause "for a table of the item 3 shape fails with code 33" reaches wider than E-12 measured; that is folded into WR-09.
- **WR-06 holds.** Item 7 step 1 sends the reader through item 4 before the branch changes and tells them to keep or rebuild the `v1.7.0_gg3` archive; item 4 says "do not redeploy `v1.7.0_gg2` yet", ends at "continue with step 2 of item 7", and item 7 step 2 is the old step 1 with its git text unchanged (checked line by line against `3376a18f`; E-22's command sequence is still the one in the note). The `exactlyOnce=false` paragraph fits the flow, because step 1 already says "with either `exactlyOnce` setting". The order is consistent.
- **WR-07 holds for the topic count and the database.** The prose now says one entry per consumed topic, and names the connection database or `` `<db>`.`<zkDatabase>` ``; the empty-result sentence says "the listed topics". The SQL block is byte-equal to E-14, so the visible template still has two slots, but the sentence directly above tells the reader to add entries, which is enough. The fix does not cover `enableDbTopicSplit=true`, where the entries must carry the database prefix (WR-10).
- **IN-04 holds.** The lead sentence now lists a line break, and agrees with the table row, the detection query and E-16.
- **IN-05 holds.** The CHANGELOG bullet names `ClickHouseHelperClient` and the subclass, which is what the Test harness row of FORK-DELTA says (`CountingHelperClient`).

## Warnings (round 4)

### WR-08: Item 2 still closes with "it only tells you which table fails", after the same item says a shorter row might store a wrong value instead of failing

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:153` (and `:123`)
**Issue:** The "What to do" paragraph of item 2 ends with "The WARN changes nothing about what the connector does; it only tells you which table fails and why." Line 117 now says that a shorter row that happens to parse could store a wrong value instead of failing, and that only four shapes were seen to fail. A table the WARN names for one of the other nine shapes may not fail at all; it may store a wrong value without an error, which is the worse outcome, and the last sentence an operator reads about the item says the WARN identifies a failing table. Line 123 has the same shape: "`v1.7.0_gg2` fails the same way", with no limit to the four shapes that were measured on both releases. This is the quantifier round 3 asked to remove from item 2, left in its closing lines.
**Fix:**
```text
The WARN changes nothing about what the connector does; it only tells you which table has the defect. For the four shapes that were inserted the defect showed as a failed insert, and for the other shapes it was not measured.
```
and at line 123: "`v1.7.0_gg2` behaves the same way for the four shapes that were inserted on both releases, and logs no WARN, so a table that the new WARN names may already have been affected before you upgrade."

### WR-09: The item 3 shape is described as failing "every insert" and "fails with code 33" for all 14 tables, and E-11 and E-12 measured four inserts and one retry

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:163`, `:241` (and `:332`)
**Issue:** The same defect class as WR-04, in the nullability item that round 2 only fixed inside item 4's upgrade bullet. Line 163 says that on `v1.7.0_gg2` "every insert into the table failed with the server error code 33 ... and stored no row". Line 169, four lines below, says four tables were inserted, and line 184 says that for the other listed shapes "no insert was run, so read their outcome as likely, not measured". E-11 states the same limit ("their insert outcome is by analogy, not measured"). The new text of item 4 (line 241) says a re-delivered range "for a table of the item 3 shape fails with code 33 even when `v1.7.0_gg3` stored its rows", and E-12 ran the retry for one table (`d15_r02_bsn`, the `Nullable(Tuple)` root with a literal backslash-n name); the 14-table shape is wider. Line 332 (rolling back brings back "the failure of inserts into a table with a `Nullable(Tuple(...))` whose element name holds a backslash and an `n`, or a line break, with the server error code 33") carries the same universal. The instruction in each place (check, stop cleanly) is the safe direction; the claims are not what was measured, and they contradict the sentences next to them.
**Fix:** Line 163: "...kept the nullability that the server reported, and the inserts that were run into such tables (four shapes, listed below) failed with the server error code 33 (`CANNOT_READ_ALL_DATA`) and stored no row:". Line 241: "...a range that Kafka delivers again to `v1.7.0_gg2` for a table of the item 3 shape is expected to fail with code 33 even when `v1.7.0_gg3` stored its rows (measured for a `Nullable(Tuple)` with a literal backslash-n name; the other shapes were not run). The measurement in the rollback bullet above ran with `exactlyOnce=false`, which still sends the token." Line 332: "the failure of inserts into such a table (measured for the four shapes of item 3, with the server error code 33), and the `Could not take element nullability` WARN at every table description".

### WR-10: Step 2 of item 4 says nothing about `enableDbTopicSplit=true`, and the link it relies on does not carry the rule, so the empty result can be false-safe

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:248`, `:258` and `:263`
**Issue:** With `enableDbTopicSplit=true` the state key is `<db><dbTopicSplitChar><topic>-<partition>` (`Processing.java:139-141` prefixes the topic with the database and the split character; the gg1 note states it at lines 62 and 104). Step 2 tells the reader to write "one entry for each topic the connector consumes", and the query is exact-match on the key without the partition: a reader who writes the plain topic gets an empty result, and the sentence under it says "no batch of the listed topics is in flight, so it is safe to redeploy `v1.7.0_gg2`", with batches in flight. That is the unsafe outcome WR-07 was raised to remove. The note defers the rule to line 263: "Item 1 of the gg2 note ... explains why the state table is shared and how the key looks with `enableDbTopicSplit=true`". The target does not explain it. Item 1 of the gg2 note ends its step 2 with "The gg1 note explains why the table is shared and how the key looks with `enableDbTopicSplit=true`; those explanations are not repeated here". So the rule is two hops away, and the reader who follows the gg3 link lands on the gg2 text that has the two-topic query and the "safe to upgrade" sentence. The gg3 note therefore sends the operator to the gg2 query text at line 263. The gg2 note needs no edit for this, because the gg3 note can carry the rule itself. The text of line 263 and the gap are inherited from the tagged copy; the WR-07 and WR-06 rewrites of step 2 and step 3 make the gap matter, because they now present step 2 as complete.
**Fix:** Add to step 2, after the sentence about the entries: "With `enableDbTopicSplit=true` each key is `<db><dbTopicSplitChar><topic>-<partition>`, so write each entry as `'<db><dbTopicSplitChar><topic>'`." Retarget the closing link to the page that holds the explanation, `ROLLOUT-v1.7.0_gg1.md#item-1-exactly-once-pre-upgrade-check`, and word it "explains why the state table is shared" (the key form is then stated here). The anchor exists (the gg2 note links to it).

## Info (round 4)

### IN-06: Two FORK-DELTA rows still say that inserts into a `size` table "fail with code 32 or 33"

**File:** `docs/FORK-DELTA.md:47` and `:51`
**Issue:** Row 47 (`shouldSkipDescribeRow`) says "inserts into such a table fail with code 32 or 33", and row 51 (declared nullability) says "an element named `size` or `size0`, whose inserts fail with code 32 or 33". Both cite Known costs item 10, which after the WR-04 fix says only four shapes were seen to fail, the other nine were not inserted, and a wrong value is possible and unmeasured. The two parentheticals are the universal that item 10 no longer makes, so the file disagrees with itself. They are short and point at item 10, so a reader is not misled for long; they still overclaim.
**Fix:** Row 47: "(the inserts that were run into such a table failed with code 32 or 33; Known costs item 10)". Row 51: "an element named `size` or `size0`, whose inserts failed with code 32 or 33 where they were run, Known costs item 10".

### IN-07: The revision notice says "No query and no command changed", and item 7 step 1 now names a build command for the `v1.7.0_gg3` tag

**File:** `docs/ROLLOUT-v1.7.0_gg3.md:3`
**Issue:** The notice links exactly the items whose text differs from the tagged copy (items 2, 3, 4 and 7; the diff has no hunk in items 1, 5 and 6; all four anchors resolve to their headings). Its second sentence is slightly off: the SQL blocks and the git commands are byte-equal to `3376a18f`, but item 7 step 1 now tells the reader to run `./gradlew createConfluentArchive` on the `v1.7.0_gg3` tag, and item 4 adds an instruction (stop `exactlyOnce=false` tasks cleanly) that did not exist. A holder of the tagged copy who reads only the notice learns neither. It carries no date and no planning identifier.
**Fix:**
```text
This copy corrects the text of the copy in tag `v1.7.0_gg3` in four items: ... No query and no git command changed. Item 4 now also covers connectors with `exactlyOnce=false`, and item 7 has you work through item 4 before you change the branch. If you hold the tagged copy, read those items here.
```

**Confirmations, round 4.** No Critical finding: every `sql` and `sh` block in items 2, 3 and 4 and every git command in item 7 is unchanged from `3376a18f`, so no command or query does anything other than its text says, and nothing destroys state. The lines added by the eight commits carry no WR-, IN-, E- or D- identifier, no phase number, no quick ID and no date; the `L-` ledger tags and the ledger file names in the note are in unchanged lines, and they were accepted in earlier rounds. The notice's four anchors (`#item-2-the-unpaired-element-warn-and-tuple-elements-named-size`, `#item-3-backslashes-and-line-breaks-in-names`, `#item-4-in-flight-batches`, `#item-7-rollback`) match the GitHub slugs of their headings, the `` `<db>`.`<zkDatabase>` `` code span renders, and the new paragraph after the item 4 list ends the list cleanly. In the 1.7.0_gg3 section of the CHANGELOG no line passes 120 characters (the first long line is in the 1.7.0_gg2 section). The CHANGELOG Important Changes bullet on the rollback still agrees with item 4 and item 7. The three deliberate narrowings hold: WR-05's prose in place of an evidence ID, WR-07's SQL kept byte-equal to E-14 with the instruction in the prose above it, and FORK-DELTA's "has not been measured" form (item 10 says "neither has been measured" and still lists client V1 and the `insertFormat=json` route as unmeasured). The gg2 note was not edited and needs no edit, but the gg3 note sends the operator to the gg2 item 1 text at line 263 (WR-10), so the gg3 note leaves the operator safe only after WR-10 is applied. The earlier rounds are untouched.
