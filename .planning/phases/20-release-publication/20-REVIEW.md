---
phase: 20-release-publication
reviewed: 2026-10-14T13:45:00Z
depth: standard
diff_base: d73848807c58c4ba535da05e7515ecbbf5025bdf
files_reviewed: 6
files_reviewed_list:
  - CHANGELOG.md
  - VERSION
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg2.md
  - src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java
  - .planning/scripts/fork-anchors.txt
findings:
  critical: 0
  warning: 2
  info: 4
  total: 6
status: issues_found
---

# Phase 20: Code Review Report

**Reviewed:** 2026-10-14
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found
**Diff base:** `d73848807c58c4ba535da05e7515ecbbf5025bdf` (HEAD `b180edc7`)
**How it ran:** inline, following `~/.claude/gsd-core/agents/gsd-code-reviewer.md` (the executor has no sub-agent tool; see "Skill adaptation" in 20-05-SUMMARY.md)

## Summary

Scope is exactly the Phase 20 change: `git diff d73848807c58 HEAD` over the six files above. `git diff --name-only d73848807c58 HEAD -- . ':(exclude).planning'` lists CHANGELOG.md, VERSION, docs/FORK-DELTA.md, docs/ROLLOUT-v1.7.0_gg2.md and the new test class, and nothing else. Surrounding text was read for context only. Phase 17 to 19 code was not re-reviewed (D-07); where a claim rests on it, the claim was read against the source at HEAD.

The rollout note is sound and well evidenced: every `sql` and `sh` block equals an evidence block, the 999.1 queries were run on rows that the `be6b8861` code wrote, the dedup outcomes in item 1 match `DedupTokenRetryLiveTest` and E-01 and E-03, and the rollback in item 6 is the `read-tree` form proven by E-31. No finding concerns the rollback, the TRUNCATE rule, secrets or the published gg1 texts.

Two Warnings are factual disagreements between the release documents, both found by reading the CHANGELOG and FORK-DELTA against the source and the rollout note (confirmation 7). The four Info findings are wording that overstates or blurs a measured fact. All six are inside the six reviewed files and inside D-01 to D-16.

## Warnings

### WR-01: CHANGELOG calls `lombok.config` new and the artifact byte-identical; neither is what was measured

**File:** `CHANGELOG.md:62-63`
**Issue:** The bullet reads "A new `lombok.config` selects Jackson 2 for `@Jacksonized`, ... The built artifact is byte-identical." `lombok.config` already exists in `v1.7.0_gg1` (`git show be6b8861:lombok.config` prints the two lines `config.stopBubbling = true` and `lombok.addLombokGeneratedAnnotation = true`); Phase 17 added the one line `lombok.jacksonized.jacksonVersion += 2`. FORK-DELTA's row says this correctly (three settings). The byte-identity measured in Phase 17 (17-02-SUMMARY) is a normalized `javap -v -p` comparison of `ClickHouseFieldDescriptor` and its builder, not of the built artifact, which changes anyway with every release. An operator who reads the CHANGELOG would conclude the whole jar is unchanged by this line.
**Fix:** Say what is true and measured: "`lombok.config` gains a line that selects Jackson 2 for `@Jacksonized`, which removes a compile-time warning. The generated code of the one class that uses it is unchanged."

### WR-02: FORK-DELTA says a serialization change never duplicates rows on a retry; item 1 of the rollout note says that holds only with a deduplication window

**File:** `docs/FORK-DELTA.md:198`
**Issue:** The new paragraph ends "`DedupTokenRetryLiveTest` pins that the server drops a retry carrying the same deduplication token even when its bytes differ, so a serialization change in a sync does not duplicate rows on a retry but keeps the earlier attempt's content." The test pins that only for tables that deduplicate (`ReplicatedMergeTree`, and a `MergeTree` with `non_replicated_deduplication_window` above 0). On a plain `MergeTree` the same test pins the opposite: a re-delivered range is stored again, identical or changed. Rollout item 1 says so ("A table without a deduplication window stores a second copy of a retried batch in any release"). The sentence is read by whoever runs the next sync, who would assume a serialization change is safe on every table.
**Fix:** Qualify it: "... so on a table with a deduplication window a serialization change in a sync does not duplicate rows on a retry but keeps the earlier attempt's content; a table without a window stores a retried batch again whatever the encoding."

## Info

### IN-01: The sentence about the hardening ledgers now points at the wrong range

**File:** `docs/FORK-DELTA.md:198`
**Issue:** Phase 20 inserted the gg2 paragraph in front of the sentence "The behaviour changes of the hardening phases are individually listed, with the test that pins each, in the Phase 13 behaviour ledger (`L-01` to `L-22`)." It now closes the paragraph that describes `v1.7.0_gg2`, so it reads as if the gg2 hardening were in `L-01` to `L-22`. The gg2 entries are `L-23` to `L-30` (Phase 18 ledger) and `L-31` to `L-41` (Phase 19 ledger), as the same file says near the top.
**Fix:** Move the sentence back before the gg2 paragraph, or end the gg2 paragraph with "its behaviour changes are `L-23` to `L-41` in the Phase 18 and Phase 19 ledgers".

### IN-02: The new Tuple WARN is logged once per column, not once per table

**File:** `docs/ROLLOUT-v1.7.0_gg2.md:120` and `CHANGELOG.md:21`
**Issue:** Both say the line is logged "once per describe of that table" or "of a table". `DeclaredNullability.apply` logs it per root column, so a table with two such columns logs two lines per describe, and the connector describes tables again on every mapping refresh. The line names the column, so the difference matters to someone counting or rate-limiting the alert.
**Fix:** "once per affected column each time the connector describes the table".

### IN-03: The Known costs preamble contradicts its own list

**File:** `docs/FORK-DELTA.md:135`
**Issue:** The paragraph, edited in Phase 20 for the archived paths, still says "This release is published with the items below knowingly open ... and none is fixed by the sync or by the hardening that followed it." Items 1, 2, 3, 5 and 7 below it are headed "is closed" and say what fixed them.
**Fix:** "The items below are either closed, with what closed them, or knowingly open."

### IN-04: Item 1 says "measured" for a gg1 attempt that the test simulates

**File:** `docs/ROLLOUT-v1.7.0_gg2.md:34-36`
**Issue:** "Measured on ClickHouse 26.9.18.11 ... If the `v1.7.0_gg1` attempt landed its rows, those rows stay exactly as they are, including phantom rows ..." In `DedupTokenRetryLiveTest` the earlier attempt is not written by the gg1 jar: the test inserts the rows the pre-fix connector wrote (two real rows plus two all-defaults rows) with a plain JSON insert under the token the connector sends (E-03 says so). The rule it supports is the server's, and the token is the same in both releases (`QueryIdentifier` is unchanged), so the conclusion stands; but the reader is told the whole sentence was measured. The sentence also lists "wrongly stored Enum and Tuple values", which the test does not exercise.
**Fix:** Add one clause: "(the earlier attempt was simulated by inserting the rows the `v1.7.0_gg1` connector wrote under the token the connector sends; the Enum and Tuple cases follow from the same rule and were not run through a retry)".

---

## Mandated confirmations (D-07)

1. **The dedup claims match the test and E-01/E-03: confirmed, with IN-04 and WR-02.** Every claim of item 1 about the server (same token dropped with changed bytes, different token with identical data stored, window 100 drops, window 0 stores, the `exactlyOnce=false` outcomes on `ReplicatedMergeTree` and plain `MergeTree`, the version 26.9.18.11 and the window values 10000 and 3600) equals a `DedupTokenRetryLiveTest` case and its E-01/E-03 output (8 tests, 0 failures). The 3600 seconds time limit is stated as read from the setting and not waited out, which is accurate. Two wordings over-reach (IN-04 in the note, WR-02 in FORK-DELTA).
2. **Every quoted query and command has matching evidence, and the 999.1 queries were proven on gg1-written rows: confirmed.** All `sql` and `sh` blocks of the note are byte-equal to evidence blocks (E-11 to E-19, E-30; the check is re-run in the disposition task). The tuple, Enum, Array, Map, single-field and phantom-row queries were run on rows written by `be6b8861` (E-10 names the tree and server) and flag them, and flag nothing on the `v1.7.0_gg2` rows of the same records. The Enum16 case is stated as having nothing to find because gg1 refused the batch (E-12, E-20).
3. **No TRUNCATE or unscoped DELETE instruction for the shared KeeperMap table: confirmed.** The note, CHANGELOG, FORK-DELTA, README and docs contain no `TRUNCATE TABLE` and no delete statement; the only occurrences of the word are the Debezium truncate event, and the gg1 note's own "Do not `TRUNCATE`". The note says it gives no delete statement. The KeeperMap query is a read, scoped by topic (E-17).
4. **The rollback item uses read-tree with pre-v1.7.0_gg2, and E-31 proves it: confirmed.** Item 6 uses `git read-tree -u --reset pre-v1.7.0_gg2` plus a commit and says why `git revert -m 1` is not used. E-31 runs every quoted command on a dry-run merge of the release tip into `be6b8861` (rollback commit, `ok`, the document gone from the branch, `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg2.md`, forward revert restoring the tree). The tag is local until plan 20-07 pushes it before the merge; the note's "published together with this release" is loose but not wrong. What the rollback brings back, including the pre-hardening `tests.yaml` (`ubuntu-latest`, tag-pinned actions), matches `git show be6b8861:.github/workflows/tests.yaml`.
5. **The released gg1 CHANGELOG section and gg1 note are unchanged: confirmed.** `CHANGELOG.md` from the `# 1.7.0_gg1, 2026-10-10` heading onward equals the same text at `d73848807c58`, and `git diff --quiet d73848807c58 HEAD -- docs/ROLLOUT-v1.7.0_gg1.md` exits 0.
6. **No secret, hostname, record value or production identifier: confirmed.** The added lines carry no URL outside the repository's own relative links, no address, credential or host; every environment-specific value is an angle-bracket placeholder; the test uses synthetic names and numbers and logs the server version, the window settings, case keys and row counts only. The only local-style paths are inside the evidence file, which is not published.
7. **The note, CHANGELOG and FORK-DELTA agree with each other and with the quoted source texts: not fully, see WR-01, WR-02, IN-01, IN-02 and IN-03.** The statements about values, the single-field `DataException`, the empty-CDC skip, the message and log changes (L-23 to L-29), the MBean and client name, the VERSION row and the anchor row (`v1.7.0_gg2`, count 1, anchors invariant PASS) agree across the three documents and with ledgers L-23 to L-41. The quoted texts (the old and new WARN, the `DataException` texts, the DateTime line) equal the source strings.
8. **The new test follows the AGENTS.md test rules: confirmed.** It extends `ClickHouseBase`, so it runs against a real ClickHouse and is excluded from `testUnit`; every assertion is a typed read-back compared with `assertEquals` (the grouped read-back makes a row stored twice visible as `n = 2`); the exactly-once cases use the real `ClickHouseSinkTask` and `KeeperStateProvider`, not `InMemoryState` and not a mocked writer; names are descriptive; strings are constants; the helpers are shared with the other live tests; and no existing test was edited or weakened. E-04 shows it goes red without the connector's token.

---

_Reviewed: 2026-10-14_
_Reviewer: Claude (gsd-code-reviewer procedure, inline)_
_Depth: standard_
