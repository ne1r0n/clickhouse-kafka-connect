---
phase: 24-release-publication
reviewed: 2026-10-20T17:12:00Z
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
  warning: 2
  info: 3
  total: 5
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
