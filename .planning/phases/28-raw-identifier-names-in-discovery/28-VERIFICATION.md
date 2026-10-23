---
phase: 28-raw-identifier-names-in-discovery
verified: 2026-10-23T12:20:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".github/scripts/jacoco_summary.py"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-01-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-01-SUMMARY.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-02-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-02-SUMMARY.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-03-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-03-SUMMARY.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-04-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-04-SUMMARY.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-05-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-05-SUMMARY.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-06-PLAN.md"
  - ".planning/phases/28-raw-identifier-names-in-discovery/28-06-SUMMARY.md"
  - "CHANGELOG.md"
  - "build.gradle.kts"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java"
covered_digest: "v3:sha256:4a3e9dad1388ce43d87fc78b1f9fcbdf506c9763ad85a359504d67660a608f5a"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Sign off the named exemption list (plan 28-06 human-check). Read 28-COMMAND-EVIDENCE.md E-06 `#### Named exemption list` (X-01..X-12 pins, S-01..S-04 support rows) and its `exemption_signoff:` line, then E-13's `assertion diff check` and `predicate changes to existing assertions` tables."
    expected: "Every removed src/test line is covered by a list row; each of X-01..X-12 is a change of the expected form under the 2026-10-21 decision (raw identifier canonical, no dual-form alias) with the same strictness; no other existing test member changed; S-01..S-04 change no assertion."
    why_human: "The plan declares this sign-off as an end-of-phase human item. S-04 (the private constant RESOURCE_PATH_FORMAT) was added to the list at the gate through a relayed user choice, and the summary treats that relay as the sign-off of the 16-row list; a relayed choice is not the plan's own end-of-phase confirmation, so it stays open."
  - test: "Confirm the two choices the phase made on the user's behalf (plan 28-06 human-check)."
    expected: "(a) The NAME-07 documents (FORK-DELTA, CHANGELOG, Known costs item 11, ROADMAP 999.5) landed in fix(28-05) rather than fix(28-03). (b) Column's enum-label scan calls the package-private DeclaredNullability.unescapeJsonLayer instead of carrying its own copy (research Open Question 1)."
    why_human: "Both are design decisions the plan routes to the user; they are not mechanically verifiable."
---

# Phase 28: Raw Identifier Names in Discovery Verification Report

**Phase Goal:** A column or Tuple element whose name contains a backslash, a literal backslash-n, a line feed, a tab, a carriage return, a slash, a double quote, a backtick or a non-ASCII character is matched under its raw identifier, as written in `CREATE TABLE`. A Kafka record using that name lands, and the typed read-back equals it. Declared nullability still pairs. The doubled form's behaviour is pinned as the rollout note will state it, and there is no dual-form alias. This closes backlog 999.5 and FORK-DELTA Known costs item 11, and the fork coverage report lists every fork class.
**Verified:** 2026-10-23T12:20:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

All code and documentation checks pass. The only open items are the human sign-offs the plan itself declares (exemption list, two design choices). Four advisory code-review warnings are recorded below; none contradicts a must-have.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | NAME-01: escaped-quote branch of `normalise` pinned on the current code before any decode change; red under the always-close mutation | VERIFIED | `DeclaredNullabilityTest.escapedBacktickInsideANameKeepsTheQuoteOpen` (line 340) and `twoBackslashesBeforeNOutsideQuotesAreNotALineBreak` (line 348) exist and pass in my run (69 tests, 0 failures). Commit 1f984bef (`test(28-01)`) precedes every src/main change. I traced the old `normalise` (f3b1c487) by hand: with `if (escaped == quote)` the quote closes at the escaped backtick, the `\n` pair at the end is then left unreplaced, so the expectation fails. Red-run output is recorded in E-02; I did not re-execute the mutation (no source edits allowed). |
| 2 | NAME-02: live name-matrix spike on `latest` records current and decoded forms for root, Tuple, enum label | VERIFIED | E-04 holds a 12-entry table (ctl, bs, bsn, lf, tab, cr, sl, dq, bt, na, ap, btbsn) with columns server JSON text, current V2, current V1, decoded, for root, tuple, array_tuple, nullable_tuple, map_tuple and enum_label. Server 26.9.19.9 on `latest`. `RawNameMatrixLiveTest` (25) and `RawNameMatrixFixturesUnitTest` (50) pass in my run. |
| 3 | NAME-03/04: names-only decode, V1 raw UTF-8 lines, pairing on raw-name key, COLS-13 twins green, 23-REVIEW IN-01/IN-02 closed | VERIFIED | `ClickHouseFieldDescriptor.fromJsonRow` decodes only the `name` field via `parser.getText()` and slices every other string from the source text, so type text keeps its JSON layer (code read). `ClickHouseHelperClient.readDescribeRows` reads raw lines as UTF-8 for both V1 (`response.getInputStream()`) and V2. `DeclaredNullability.normalise` is rewritten (decode JSON layer via `unescapeJsonLayer`, then quote-aware pass). Decode and pairing key landed together in d589234f. `NullableTupleMatrixLiveTest` 33/33 and `TableTest` (incl. `extractCommentV1`) pass in my run. IN-01: `apply` Javadoc no longer says "all or none". IN-02: null-name check sits above the `try` and logs `<unnamed>`. |
| 4 | NAME-05/06: raw-name record lands with typed read-back; doubled root stores DEFAULT; doubled element fails loudly; enum labels round-trip under raw form | VERIFIED | `RawIdentifierNameLiveTest`: 73 cases, 0 skipped, 0 failures in my run, covering `rawNamesLandAndReadBackTyped` (reads back and compares), `doubledRootNameStoresTheDefault`, `doubledElementNameFailsLoudly` (with a cause substring, per container shape) and `rawEnumLabelLandsAndReadsBackTyped`. `Column.extractEnumValues` is an escape-aware scan. A manual `CLIENT_VERSION=V1` run (105 tests, 0 failed) is recorded in E-10. |
| 5 | NAME-07/DEBT-17: FORK-DELTA row, CHANGELOG line, Known costs item 11 retired, 999.5 delivered, in the fix commit; 21-class `jacocoForkReport` edited once with FORK-DELTA row; anchors invariant passes | VERIFIED | fix(28-05) d7361cbb touches `docs/FORK-DELTA.md`, `CHANGELOG.md`, `.planning/ROADMAP.md` (999.5 "Delivered:" line present) and `Column.java`. FORK-DELTA Known costs item 11 reads "Backlog 999.5 is closed." `build.gradle.kts` was edited once in the phase (898a20b2, together with `jacoco_summary.py`, `ForkCoverageScopeUnitTest`, FORK-DELTA and the anchors file). `forkChangedClasses` and `FORK_CLASSES_IN_ORDER` both list 21 classes in the same order; my `test` run regenerated `build/reports/jacoco/fork/jacocoForkReport.xml` with 21 distinct classes including `DeclaredNullability` and `WithheldMessageException`. Every class added to `src/main` since `v1.7.0` (MetadataFreeJsonWriter, DescribeTableBuilder, DeclaredNullability, ParentShape, ConvertToMapOrJson, HeaderToValue, HeaderToValueConfig, HeadersToValue, TableOperationFilter, WithheldMessageException) is in the list. I re-ran `verify-merge-invariants.sh` on HEAD: `anchor_rows=94 ... result=PASS`. |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseFieldDescriptor.java` | names-only decode | VERIFIED | `fromJsonRow` token-offset slicing; used by `DescribeTableBuilder.addRow` via both describe paths |
| `ClickHouseHelperClient.java` | shared UTF-8 raw-line reader | VERIFIED | `readDescribeRows` called from `describeTableV1` and `describeTableV2` |
| `DeclaredNullability.java` | rewritten `normalise`, raw-key pairing | VERIFIED | `unescapeJsonLayer` is also used by `Column` |
| `Column.java` | escape-aware enum scan | VERIFIED | `extractEnumValues`, `malformedEnumType` |
| `RawIdentifierNameLiveTest.java` | NAME-05/06 live pins | VERIFIED | 73 pass |
| `ForkCoverageScopeUnitTest.java` | list-equality guard | VERIFIED | 3 pass |
| `build.gradle.kts`, `jacoco_summary.py` | 21-class list | VERIFIED | equal lists |
| `docs/FORK-DELTA.md`, `CHANGELOG.md`, `ROADMAP.md` | NAME-07 documents | VERIFIED | see truth 5 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `ClickHouseHelperClient.describeTableV1/V2` | `DescribeTableBuilder.addRow` | `readDescribeRows` | WIRED | same reader for both clients |
| `DescribeTableBuilder` | `ClickHouseFieldDescriptor.fromJsonRow` | per-line parse | WIRED | names decoded once |
| `Table`/writer | `DeclaredNullability.apply` | root + declared type | WIRED | live twins pass on raw names |
| `Column.extractEnumValues` | `DeclaredNullability.unescapeJsonLayer` | package-private call | WIRED | one JSON-layer decoder |
| `build.gradle.kts` list | `jacoco_summary.py` list | `ForkCoverageScopeUnitTest` | WIRED | equality enforced |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| Column names in `Table` mapping | `ClickHouseFieldDescriptor.name` | live `DESCRIBE TABLE` JSONEachRow lines | Yes (live tests insert and read back equal) | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase test classes on ClickHouse `latest` (26.9.19.9), client V2 | `./gradlew test --tests '*DeclaredNullabilityTest' '*ColumnTest' '*ForkCoverageScopeUnitTest' '*RawNameMatrixFixturesUnitTest' '*TableTest' '*DescribeTableBuilderUnitTest' '*RawIdentifierNameLiveTest' '*RawNameMatrixLiveTest' '*NullableTupleMatrixLiveTest'` | 426 tests, 425 succeeded, 0 failed, 1 skipped (BUILD SUCCESSFUL) | PASS |
| Full-suite regression evidence | recorded in 28-COMMAND-EVIDENCE.md E-12/E-13 (not re-run) | 1835 tests, 0 failures, 0 errors, 18 named skips, 26.9.19.9 | PASS (recorded) |
| Anchors invariant on HEAD | `verify-merge-invariants.sh 38265d26... HEAD` with the tail env | `anchor_rows=94 result=PASS` | PASS |

### Probe Execution

Step 7c: SKIPPED. No `probe-*.sh` is declared by the phase plans.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| NAME-01 | 28-01 | escaped-quote pin | SATISFIED | truth 1 |
| NAME-02 | 28-02 | name-matrix spike | SATISFIED | truth 2 |
| NAME-03 | 28-03 | raw name in `fromJsonRow`, V1 UTF-8 | SATISFIED | truth 3 |
| NAME-04 | 28-03 | raw-key pairing, `normalise`, IN-01/02 | SATISFIED | truth 3 |
| NAME-05 | 28-04 | both-form live pins | SATISFIED | truth 4 |
| NAME-06 | 28-05 | enum labels | SATISFIED | truth 4 |
| NAME-07 | 28-05 | FORK-DELTA/CHANGELOG/item 11/999.5 | SATISFIED | truth 5 |
| DEBT-17 | 28-06 | 21-class fork report | SATISFIED | truth 5 |

All eight IDs appear in a plan's `requirements:` frontmatter and in its SUMMARY's `requirements-completed`, and all eight are marked Complete in REQUIREMENTS.md. No orphaned Phase 28 requirements.

### Anti-Patterns Found

TBD/FIXME/XXX scan of the four modified src/main files and the new test files: one match, an upstream marker. No stubs; no hollow data paths.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `Column.java` | 181 | `FIXME: Map keys doesn't support Decimal` | Info | Present unchanged in upstream `v1.7.0` (line 168) and not touched by this phase's diff; not a phase-introduced debt marker |

### Code Review Warnings (advisory, from 28-REVIEW.md, all recorded `open`)

None contradicts a must-have. Each sits outside what the phase claims to have measured.

| ID | Summary | Relation to must-haves |
|----|---------|------------------------|
| WR-01 | Enum label scan reads `\0`, `\b`, `\f` as the letters 0, b, f; a `\b`/`b` collision can silently overwrite a label | Outside the nine spiked characters. FORK-DELTA item 11 already says control characters beyond the nine are "not measured", but its wording "a label is a lookup miss" understates the collision case. Worth a user decision. |
| WR-02 | `describeTableV1` does not catch `RuntimeException`, so a malformed enum type aborts mapping of the whole database on the default client (V2 returns null) | Reachable through the new `malformedEnumType()` throw site. Not claimed by any must-have. |
| WR-03 | No tests for the `\uXXXX` path of `unescapeJsonLayer` or the malformed-enum throw sites | Coverage gap; the main paths are pinned. |
| WR-04 | `legacyEnumLabelKeyFailsLoudly` passes on any failure (no cause substring) | Weaker oracle than its sibling; red-first run E-09 still shows it red on the unchanged `Column`. |
| IN-01..IN-03 | public test-only constant, list guard cannot detect a missing class (`SinkTaskStatistics`), no runtime signal for a missed rename (accepted D-06) | Informational. |

### Human Verification Required

#### 1. Exemption list sign-off (plan 28-06 human-check)

**Test:** Read 28-COMMAND-EVIDENCE.md E-06 `#### Named exemption list` (X-01..X-12, S-01..S-04) and its `exemption_signoff:` line, then E-13's two tables.
**Expected:** Every removed `src/test` line is covered by a row; X-01..X-12 are changes of the expected form under the 2026-10-21 raw-identifier decision with equal strictness; nothing else was loosened.
**Why human:** The plan declares this end-of-phase confirmation. S-04 was added at the gate through a relayed choice; the 28-06 SUMMARY calls that relay the sign-off of the 16-row list, but a relayed choice does not replace the plan's own human-check. My own check agrees with the evidence: DeclaredNullabilityTest has 58 -> 61 tests and 213 -> 216 assert calls, NullableTupleMatrixLiveTest 8 -> 8 and 30 -> 30, and the assertion diff check reports no unlisted member.

#### 2. Two design choices made for the user

**Test:** Confirm (a) the NAME-07 documents landed in fix(28-05), not fix(28-03); (b) `Column` calls the package-private `DeclaredNullability.unescapeJsonLayer` rather than keeping its own copy.
**Expected:** Both acceptable.
**Why human:** Design decisions routed to the user by the plan.

### Gaps Summary

No gaps. The goal is achieved in the code: names are matched under the raw identifier on both clients, the pairing key follows, both name forms are pinned live, documents and the 21-class report landed, and the anchors invariant passes at HEAD. Status is `human_needed` only because plan 28-06 declares end-of-phase human sign-offs that no automated check can discharge. The four review warnings stay open for `/gsd-code-review 28 --fix` or a user decision; WR-01 and WR-02 are the ones most worth deciding before release.

Housekeeping, not a gap: the ROADMAP progress checkbox for Phase 28 (line 100) still reads `[ ]` and should be flipped by the orchestrator on completion.

---

_Verified: 2026-10-23T12:20:00Z_
_Verifier: Claude (gsd-verifier)_
