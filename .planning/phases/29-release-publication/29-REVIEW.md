---
phase: 29-release-publication
reviewed: 2026-10-23T18:57:00Z
depth: standard
diff_base: c1dc6e5983fe93745526cecd89d3b7e7cff63c7d
files_reviewed: 10
files_reviewed_list:
  - docs/ROLLOUT-v1.7.0_gg4.md
  - docs/ROLLOUT-v1.7.0_gg1.md
  - docs/ROLLOUT-v1.7.0_gg2.md
  - docs/ROLLOUT-v1.7.0_gg3.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - VERSION
  - .planning/scripts/fork-anchors.txt
  - .planning/scripts/confluent-archive-allowlist-29.txt
  - .planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md
findings:
  critical: 1
  warning: 8
  info: 4
  total: 13
status: issues_found
rounds: 1
---

# Phase 29: Code Review Report

**Reviewed:** 2026-10-23
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Scope: `git diff c1dc6e5983fe93745526cecd89d3b7e7cff63c7d HEAD` over `docs`, `CHANGELOG.md`, `VERSION`, `fork-anchors.txt` and the Phase 29 allow-list, plus the close record. `src/main` was read where a document makes a claim about connector behaviour (`RecordConvertor`, `Processing`, `DescribeTableBuilder`, `ClickHouseWriter.passesThroughUnchanged`, `Column.convertEnumValues`, `ConnectApiReferencesUnitTest`, the seven LIST keys, the Kafka 4.4.0 `ConfigDef.parseType`).

What holds: every `sql` and `sh` block of the gg4 note is byte-present in `29-COMMAND-EVIDENCE.md`; the anchors mirror re-run on this HEAD gives 94 rows, 0 bad; the numbers of the gg4 note (14 and 4 table counts, 25 of 47, 27 suspects, 22 differing files, 25 nested jars, 135 against 7376 entries, nine Dependabot merges, 21 coverage classes, 37 / 14 / 23 file split) agree with E-03, E-04, E-10, E-13, E-22, E-43 and the tree; the enableDbTopicSplit key rule (two-part topic keyed by its own name, any other topic keyed `<db><char><topic>`) agrees with `RecordConvertor.convert` and `Processing.doLogic`; the Kafka 4.4.0 `ConfigDef.parseType` has no list-duplicate or null handling, so the KIP-1161 reading (validator based) is supported; the gg1/gg2/gg3 diffs change no query and no `DELETE`.

The defects are in operator procedure and in claims that go past their evidence. One Critical: the rollback instruction for the name change has the operator rename the producers back before the connector is stopped and drained, which makes the still-running gg4 connector store column DEFAULTs silently. Eight Warnings: a drain check that exists only for rollback but is called from the upgrade path, two places where the size-element measurements are generalised, a unit-test overclaim that the close record itself contradicts, an incomplete dependency list stated as exhaustive, a residual description that differs between three documents, a false "safe" in the gg1 note for connectors with more than two topics, and an upgrade order with an open window. Four Info items.

## Round 1

### CR-01: Item 4 tells the operator to rename the producers back before the connector is stopped and drained, so gg4 (or gg3 after redeploy) reads records under the wrong name rule

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:244` (also `:337`, `:349`; compare `:112`)
**Issue:** "What to do" of item 4 reads "Before a rollback, rename the producer fields back, and for each connector with `exactlyOnce=true` ... 1. Stop or pause the connector". Read in order, the producers go back to the earlier (escaped) names while the gg4 connector is still consuming. gg4 then handles every new record under the earlier names as item 1 says it does: a root field is ignored and the column `DEFAULT` is stored with no error and no WARN, a Tuple element fails the put. Item 7 step 1 sends the reader through item 4 and so inherits the order. The same gap exists for the backlog: records already in the topic that were produced under the new names, and not yet consumed when gg4 is stopped, are read by the redeployed gg3 under its old rule, with the same silent DEFAULT. Item 1 step 2 gives the operator a drain-then-switch order for the upgrade direction (marked inferred for a backlog); the rollback direction has none, and item 7's closing bullet ("producers must be renamed back before the old release reads their records") states only the final condition. This is silent wrong data in a production rollback, so it is Critical.
**Fix:** Give the rollback its own order in item 4 (and point item 7 step 1 at it): keep the producers on the new names until gg4 has consumed everything produced under them (no lag), stop the connector cleanly, run the in-flight check, then switch the producers back, then redeploy `v1.7.0_gg3`. For example, replace the sentence at line 244 with:
```text
Before a rollback, do not rename the producers yet. Let `v1.7.0_gg4` consume everything that was produced under the new names (no lag on those topics), stop the connector cleanly, and for each connector with `exactlyOnce=true` that writes a table of either shape run steps 1 to 3 below. Only then switch the producers back to the earlier names and redeploy `v1.7.0_gg3`. Records produced under the new names that are still in a topic when `v1.7.0_gg3` starts are read under the old rule (a root field stores the column's DEFAULT; this order is inferred, to be pinned live).
```

### WR-01: The in-flight check procedure of item 4 is written for rollback only, yet items 1 and 2 and the upgrade bullets depend on "stop the connector cleanly (item 4)"

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:112`, `:231-232`, `:244-258`
**Issue:** For the upgrade, item 1 step 2 says "stop the connector cleanly (item 4)" and item 4 bullets 1 and 2 say "a clean stop is what leaves no range in flight" and "Stop cleanly first for a table of that shape". But the only procedure in item 4 is headed "Before a rollback": the query result is judged "safe to redeploy `v1.7.0_gg3`", and step 3 says "start the `v1.7.0_gg4` jar again" and "continue with step 2 of item 7". An operator going from gg3 to gg4 has no way to confirm that no range is in flight (a stopped task that was killed, or a worker restart, leaves `BEFORE_PROCESSING`), and the sentences they would follow name the wrong release. The gg3 note avoided this because its upgrade needed no clean stop; gg4 introduces a clean-stop requirement for every connector that writes a rename-affected table, with no usable check for it. For `exactlyOnce=false` the note says only "stop its tasks cleanly", with no way to tell that the offsets were committed.
**Fix:** Add an upgrade-direction paragraph to item 4 that reuses the same query (it is release independent) and says "empty result: safe to start `v1.7.0_gg4`; non-empty: start the `v1.7.0_gg3` jar again, let the listed ranges reach `AFTER_PROCESSING`, stop it cleanly, run the query again". Keep the rollback wording as it is. For `exactlyOnce=false`, say how a clean stop is recognised (the consumer group has no lag and the task stopped through the Connect REST pause or delete).

### WR-02: The rollback bullet states that gg3's short bytes are "rejected before the token", which is false for the shape that gg3 stored shifted

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:233` (and `:240`, `:350`)
**Issue:** The bullet says "The bytes of `v1.7.0_gg3` are short, and the server rejects them before it looks at the token", then "Measured for one affected shape" (E-12 `rb_a01`, `Tuple(size UInt16, v String)`). Item 2 of the same note (line 129) says that for `Tuple(size UInt32, v String)` the gg3 bytes do parse: the server stored a shifted row. For that shape the rollback retry is not rejected. By the same mechanism as the measured upgrade case (E-12: dropped on a table with a window, stored as a second row on a table without one), a gg3 retry of a range gg4 already stored is dropped or stored again as a shifted row; this direction was not run. `:240` ("expected to fail with code 32 or 33") and `:350` ("the stored shifted row of one shape") repeat the failing-only picture for every size-named shape. The instruction (stop cleanly first) is the safe direction, so no operator action is wrong, but the stated mechanism is wider than what was measured and contradicts line 129.
**Fix:** In the rollback bullet scope the mechanism: "For a shape whose short bytes cannot be parsed, the server rejects them before it looks at the token (measured for one such shape). For a shape whose short bytes do parse, such as `Tuple(size UInt32, v String)`, the retry is dropped by the token on a table with a deduplication window and stored as an extra shifted row on a table without one; this direction was not run (inferred from the upgrade measurement, to be pinned live)."

### WR-03: The note never says which of the 25 differing tables were inserted, and the upgrade rule "for the shapes that failed" cannot be applied by an operator

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:129`, `:232`; `CHANGELOG.md:20-24`
**Issue:** Item 2 says "Measured with one record per table" and then lists seven shapes plus three look-alikes. E-11 inserted ten tables, seven of them among the 25 tables whose description differs (E-10); the other 18 of the 25 were detected by the description and the WARN only. The note does not say so. Item 4 then reasons "For the shapes that failed on `v1.7.0_gg3` no clean stop is needed" and "The shape that stored a shifted row is different ... for a table of that shape", and the CHANGELOG repeats "a table whose `Tuple` element named `size` stored a shifted row on `v1.7.0_gg3`". Item 2 itself says a shorter write that happens to parse "was measured for that one shape only". An operator cannot know, for any of the 18 uninserted shapes (several of them have integer-typed neighbours, like the shifted one), whether gg3 failed or stored a shifted row, so the "no clean stop needed" branch is not decidable and the safe branch is not the default. This is the quantifier class that earlier reviewers (24-REVIEW WR-04, WR-08, WR-09) raised against the gg3 note.
**Fix:** In item 2 state the count ("7 of the 25 were inserted; the other 18 were detected only"). In item 4 and in the CHANGELOG make the safe branch the default: "Stop cleanly first for every table the item 2 query lists. For the shapes that were inserted and failed on `v1.7.0_gg3` (listed above) nothing is in flight; for the shape that stored a shifted row, and for every one of the 18 shapes that were not inserted, a range in flight can meet a shifted row (inferred, to be pinned live)."

### WR-04: "A unit test repeats the check on every build" overclaims what `ConnectApiReferencesUnitTest` does, and the close record says the note claims less

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:293`; `CHANGELOG.md:97-98`; `docs/FORK-DELTA.md:72`; `.planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md:20`
**Issue:** The test (read in full) compares the references derived from the compiled classes with a committed list, `connect-api-references.txt`. It does not open any worker jar. The list was checked against the 7.9.7, 4.1.1 and 4.4.0 jars once, by a script outside the build (E-06, E-13 of Phase 25). So the test does not "repeat the check" against the worker; it detects a new reference that is not on the list. The note's wording is the opposite of the close record's carry of 25-REVIEW WR-02: "the current reference list was checked statically ... and the rollout note states only that", and WR-03 of the same review calls "every referenced class and member" stronger than the test guarantees (descriptor-only and unresolved inherited members are not covered). The CHANGELOG sentence "lists every Kafka class and member the compiled connector references" and FORK-DELTA's "every one of the 176 references" carry the same word without the limit. An operator would read the note as "the build keeps proving the worker compatibility".
**Fix:** Note (line 293): "A unit test fails the build when the compiled connector references a Kafka class or member that is not on the committed list; the list was checked once against the worker jars named above, and the test does not read any worker jar." CHANGELOG: "fails when a new reference is not on the reference list (the list was checked once against the worker jars)". Keep the close record as it is.

### WR-05: The dependency table and the CHANGELOG list are presented as complete and omit jspecify (and, in the CHANGELOG, j2objc and error_prone)

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:295-306`; `CHANGELOG.md:31-36`, `:99-102`
**Issue:** Item 6 says "every other difference is a dependency version change from the table below or one of 21 classes" and the table is headed "Bundled versions that changed (from the same compare)". The Phase 29 allow-list itself names one more change: row `META-INF/versions/9/OSGI-INF/MANIFEST.MF`, "jspecify 1.0.0 -> 1.0.1 (guava 33.7.2-jre's dependency ... jspecify has no pom.properties so no VERSION line)", and FORK-DELTA row 114 states the same move. The compare prints `VERSION` lines only from `pom.properties`, so jspecify is invisible there; the table copied the VERSION lines. The sentence "every other difference is ... from the table" is therefore false. The CHANGELOG is shorter still: its Important Changes bullet lists guava, httpclient5, httpcore5, Jackson and gson, and its Build and CI bullet says the archive "differs ... only in the bundled versions above"; j2objc `3.1` and error_prone `2.50.0` (in the note's table) and jspecify `1.0.1` are not "above". A scanner report on the archive will show libraries that the documents say did not change.
**Fix:** Add a table row `jspecify | 1.0.0 | 1.0.1` with the source "from the allow-list row and the dependency resolution; it has no `pom.properties`, so the compare prints no VERSION line", change the sentence to "every other difference is one of the version changes in the table below or one of 21 classes", and extend the CHANGELOG list with `j2objc-annotations` `3.1`, `error_prone_annotations` `2.50.0` and `jspecify` `1.0.1` (or write "the bundled versions listed in the rollout note").

### WR-06: The unreadable-declared-type residual is described three different ways (which names, how often, which WARNs)

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:131`, `:166`, `:172`; `CHANGELOG.md:46-48` against `:60-67`; `docs/FORK-DELTA.md` (row of `DescribeTableBuilder`, Known costs item 10)
**Issue:** Read against `DescribeTableBuilder.decide` and `warnUnreadableParent`: (1) names: when the parent type is unreadable, `size` and `size0` rows are dropped (`SKIP_UNDECIDABLE`), while `size<digits>` rows are kept unmarked for `Table` to ignore, which `Table` does only under an `ARRAY` or `MAP` parent. The note and the CHANGELOG Known Issues bullet say "`size`, `size0` or `size` followed by digits" are dropped, the CHANGELOG Bug Fixes bullet says "`size` or `size0`". (2) frequency: the code warns once per unreadable parent path per DESCRIBE pass (`warnedParents`, keyed by the parent, which can be a nested path); the note and the Known Issues bullet say "once per column and table description". (3) WARNs: the CHANGELOG Bug Fixes bullet says the fallback also emits the `have no matching DESCRIBE sub-column` WARN (26-REVIEW IN-10 was fixed to say so), the note says the residual "has its own line" and that a hit of the first grep on a gg4 log is "a different cause than the names of this item". An operator who finds the `have no matching` line on a gg4 log is told it is not this item, when the CHANGELOG says it can be. FORK-DELTA says "one metadata-only WARN per parent per DESCRIBE".
**Fix:** Pick the code's wording and use it in all three: "for each parent whose declared type cannot be read (or that is absent from the described rows), once per table description, the connector logs `Could not read the declared type ...`, drops that parent's sub-columns named `size` or `size0`, and keeps `size<digits>` sub-columns for the existing Array/Map rule; the `have no matching DESCRIBE sub-column` WARN can accompany it". Then change line 166 to "on a gg4 log it can also come from this residual; check for the `Could not read the declared type` line of the same table".

### WR-07: The gg1 note still gives a false "safe to upgrade" for a connector with more than two topics

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:54`, `:62`, `:64`, `:66`
**Issue:** The gg1 note was corrected for the key rule, but its step 2 still says only "Scope the query to the topics this connector consumes" over a query with two `'<topic>'` slots and no instruction about the number of entries. The result sentence is unscoped: "**Empty result:** no batch is in flight and nothing is waiting for a retry, so it is safe to upgrade." A connector with three topics (a list or a regex) whose operator fills two slots gets an empty result for the listed topics and the note says nothing is in flight. This is the false "safe" for multi-topic connectors that 24-REVIEW WR-07 removed from the gg3 and gg2 notes (their text is "one entry for each topic the connector consumes" and "no batch of the listed topics"); the gg1 note was edited in this change set and left behind.
**Fix:** In the `enableDbTopicSplit` paragraph of item 1 (line 64) add the same first sentence as the gg2 note ("Replace the `'<topic>'` entries with one entry for each topic the connector consumes, adding an entry per further topic and writing a single topic in both entries") and change line 66 to "**Empty result:** no batch of the listed topics is in flight, so it is safe to upgrade."

### WR-08: The upgrade order for the producer rename leaves an open window with no instruction to stop the producers

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:112`; `CHANGELOG.md:16-17`; `.planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md:157`
**Issue:** Step 2 reads: let the old release consume everything produced under the earlier names "until the connector has no lag", stop the connector cleanly, switch the producers, then upgrade. A live producer keeps writing under the earlier names for as long as it has not been switched, so records produced after the lag was observed and before the switch sit in the topic under the earlier names; the upgraded connector reads them under the new rule (root: DEFAULT with no log line; Tuple element: failed put). Switching the producers before the drain is not possible either, because the old release would then see the new names. The sequence is only correct if the producers are paused between "no lag" and the switch, and the note does not say so. The close record carries this order as "inferred for a backlog", but the window does not depend on the backlog; it is a property of the order as written.
**Fix:** Write the order as: pause the producers of the affected topics (or accept that records produced from this point under the earlier names are lost to the new rule), wait for no lag, stop the connector cleanly, switch the producers, upgrade and start the connector, resume the producers. Mirror the sentence in the CHANGELOG bullet.

### IN-01: "Rolling back brings back" ends with a bullet about the gg4 documents, which a rollback removes

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:356`
**Issue:** The list header is "Rolling back brings back:" and the last bullet is "on the branch, the gg4 documents themselves: this note, the CHANGELOG section and the FORK-DELTA rows". The paragraph above (line 345) says the rollback "removes this document from the branch", and `git read-tree` of the `pre-v1.7.0_gg4` tree removes the note, the CHANGELOG section and the FORK-DELTA rows. The bullet states the opposite of what happens.
**Fix:** "- on the branch, the gg4 documents are gone: this note, the CHANGELOG section and the FORK-DELTA rows (read the note with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg4.md`)."

### IN-02: The gg1 `DELETE` example comment omits the trailing-split-character form that item 1 of the same note lists

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:105-110`
**Issue:** Item 1 (line 64) says a topic that ends with `dbTopicSplitChar` "can also be keyed without those trailing characters". The first `DELETE` comment gives only two forms (the topic name itself, and `<db><char><topic>`), and the second comment says "also list ... as in item 1", which does cover it. A reader who copies the first statement misses that key. The failure direction is a state row that stays, so the replayed range is dropped as already stored, which is the data-recovery failure that step 2 exists to prevent.
**Fix:** Add to the first comment: "(a topic that ends with the split character can also be keyed without the trailing characters, as in item 1)".

### IN-03: The CHANGELOG states "No other change in this release needs one" without the label the note gives the dependency claim

**File:** `CHANGELOG.md:22-24` (against `docs/ROLLOUT-v1.7.0_gg4.md:236`)
**Issue:** The note says no clean stop is needed for the bundled dependency bumps and labels it "inferred, to be pinned live: it is not measured". The CHANGELOG sentence has no label, and the close record lists the claim under "Inferred claims of the note". D-09 asks for labels on the note; the CHANGELOG is the document operators read first and drops it.
**Fix:** "No other change in this release is expected to need one (the dependency bumps are inferred, not measured; see the rollout checklist)."

### IN-04: The rename banner and the CHANGELOG bullet do not say that most of the escaped forms were client V2 only

**File:** `docs/ROLLOUT-v1.7.0_gg4.md:3`; `CHANGELOG.md:11-19`
**Issue:** The banner and the first Important Changes bullet describe the earlier form as a doubled backslash, a backslash and a letter, a backslash before a slash. The item 1 table shows that this was client V2; on client V1 (the connector's default in `ClickHouseSinkConfig`, `props.getOrDefault(CLIENT_VERSION, "V1")`) a backslash and a line feed already matched the raw name, so for them nothing changes, while the V1 changes are different (tab, carriage return, slash, double quote). A V1 operator reads "must be renamed before you upgrade" and either does needless work or cannot tell whether the notice is about them.
**Fix:** Add one clause to both: "(client V2; on client V1 only the tab, carriage return, slash and double quote cases and the Enum labels change, see the table in item 1)".

round: 1 findings: 13

## Mandated confirmations (PUB-12)

(a) No TRUNCATE advice that would wipe other connectors' state remains: holds. The only `TRUNCATE` in the three operator notes is gg1 line 115, "Do not `TRUNCATE` the table", with the shared-table reason; the gg2, gg3 and gg4 notes contain no `TRUNCATE`, `DROP`, `ALTER` or unscoped `DELETE`, and the two `DELETE` examples in gg1 are key-scoped and were run (E-25).
(b) Rollback uses tag `pre-v1.7.0_gg4` and states the nested-jar hazard correctly: holds. Item 7 uses `git read-tree -u --reset pre-v1.7.0_gg4` (E-30), redeploys the archive built at tag `v1.7.0_gg3`, and states 25 nested jars, 135 against 7376 entries and the release workflow behaviour exactly as E-04 reads them (the `pre-release.yaml` Gradle 7.4.2 pin is correctly left out of the note and is in FORK-DELTA); the ordering of the name rename inside the rollback is not correct, see CR-01, and WR-02 and IN-01 touch item 7.
(c) The CREATE TABLE-name rename notice is present and correct: holds, with a scope caveat. It is the opening paragraph of the note and the first Important Changes bullet, names `CREATE TABLE`, the forms (E-20), the DEFAULT with no log line, and the drain-then-switch order; IN-04 (client scope) and WR-08 (producer window) are the points.
(d) exactlyOnce=false connectors are handled correctly and no false "safe" remains in the gg4 note: holds. Item 4 says the state table does not exist for them, that the failure does, and tells the operator to stop the tasks cleanly before redeploying; the multi-topic query text of the gg4 note is correct. The gg1 note still has the multi-topic false "safe" (WR-07), and the clean-stop check for an upgrade is missing (WR-01).
(e) The static checks and greps the note quotes are accurate: holds. All nine `sql` and `sh` blocks of the gg4 note are present verbatim in E-13, E-14, E-15, E-22, E-23 and E-25; the two WARN greps match the constants in `DescribeTableBuilder` and the real gg3 line; the alert-rule grep has its literal-text limit stated; the anchors mirror re-run on this HEAD prints 94 rows, 0 bad. The interpretation of a hit of the first grep on a gg4 log is the one point that disagrees with the CHANGELOG (WR-06).

---

_Reviewed: 2026-10-23T18:57:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
