---
phase: 28-raw-identifier-names-in-discovery
verified: 2026-10-23T18:30:00Z
status: passed
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
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/UnescapeJsonLayerTest.java"
covered_digest: "v3:sha256:4228dc573140eaf0ae52964bca7d97e23df88c1e76f928f199c97c037f20e0da"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 5/5
  gaps_closed: []
  gaps_remaining: []
  regressions: []
---

# Phase 28: Raw Identifier Names in Discovery Verification Report

**Phase Goal:** A column or Tuple element whose name contains a backslash, a literal backslash-n, a line feed, a tab, a carriage return, a slash, a double quote, a backtick or a non-ASCII character is matched under its raw identifier, as written in `CREATE TABLE`. A Kafka record using that name lands, and the typed read-back equals it. Declared nullability still pairs. The doubled form's behaviour is pinned as the rollout note will state it, and there is no dual-form alias. This closes backlog 999.5 and FORK-DELTA Known costs item 11, and the fork coverage report lists every fork class.
**Verified:** 2026-10-23T18:30:00Z
**Status:** passed
**Re-verification:** Yes. The earlier report (status `human_needed`, no `gaps:`) is stale: review-fix commits a8f50d87, ae97c496, 16950de4, 2575092a and 54447ab4 changed covered files after it. This run re-read the code at HEAD (393e226f) and re-ran the affected tests. The two human items of the earlier report are signed off in 28-UAT.md (2/2 pass).

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | The escaped-quote branch of `normalise` is pinned on the current code before any decode change; the pin was red under a mutation that always closes the quote (NAME-01) | VERIFIED | `DeclaredNullabilityTest` passes at HEAD (69 tests, 0 failures, my run). The pin commit 1f984bef precedes every `src/main` change of the phase (`git log 1f984bef~1..HEAD -- src/main` lists only d589234f, d7361cbb, a8f50d87 and ae97c496). The red-run output under the mutation is recorded in 28-COMMAND-EVIDENCE.md E-02; I did not re-execute the mutation. |
| 2 | A live name-matrix spike on `latest` records the current and decoded form of every spiked character for root columns, Tuple elements and enum labels (NAME-02) | VERIFIED | `RawNameMatrixLiveTest` 25/25 and `RawNameMatrixFixturesUnitTest` pass at HEAD against Docker; 24 committed `raw-name-matrix/*.jsonl` fixtures plus `SERVER_VERSION.txt` exist; the spike table is E-04 of the evidence file. |
| 3 | The decode is names-only and pairing follows it (NAME-03, NAME-04): `fromJsonRow` yields the raw identifier for names and leaves type text as the server's JSON text; V1 reads raw UTF-8 lines; `DeclaredNullability` pairs on the raw-name key with decode and key in the same commit; COLS-13 twins green; 23-REVIEW IN-01 and IN-02 closed | VERIFIED | `ClickHouseFieldDescriptor.fromJsonRow` (code read): only the `name` field goes through `parser.getText()`; every other string is sliced from the source text. `ClickHouseHelperClient.readDescribeRows` reads raw lines as UTF-8 and is shared by `describeTableV1` and `describeTableV2`. `DeclaredNullability.normalise` decodes the JSON layer via `unescapeJsonLayer`, then does a quote-aware pass (n, t, r escapes become characters inside quotes; an escape pair is consumed together so an escaped quote cannot close the quote). Decode and pairing landed together in d589234f. `NullableTupleMatrixLiveTest` 33/33 and `TableTest` 7/7 (including `extractCommentV1`) pass. IN-01: `apply` Javadoc no longer says "all or none". IN-02: the null-name check sits above the `try` and logs `<unnamed>`. |
| 4 | Every spiked character lands under the raw name with typed read-back equal to the record; the doubled form is pinned (root column ignored and DEFAULT stored, Tuple element fails loudly); enum labels round-trip under the raw form or the remaining behaviour is in FORK-DELTA Known costs (NAME-05, NAME-06) | VERIFIED | `RawIdentifierNameLiveTest` 73/73, 0 skipped, against live ClickHouse at HEAD. Manual `CLIENT_VERSION=V1` run of 105 tests recorded in E-10 (the V1 write path frames are named there; not re-run by me). `Column.extractEnumValues` is an escape-aware scan; after the review fix `unescapedLabelChar` handles n, t, r, b, f, 0, backslash and apostrophe and refuses any other escape with `Enum type could not be read`. FORK-DELTA Known costs item 11 states the remaining unmeasured cases (control characters outside the nine, a `.` in a name). |
| 5 | FORK-DELTA row, CHANGELOG line, Known costs item 11 retired and 999.5 marked delivered land with the fix; `jacocoForkReport` lists `DeclaredNullability`, `WithheldMessageException` and every fork class, edited once with its FORK-DELTA row; anchors invariant passes (NAME-07, DEBT-17) | VERIFIED | d7361cbb (fix(28-05)) carries `Column.java`, FORK-DELTA, CHANGELOG and ROADMAP; ROADMAP "Phase 999.5" now has a `**Delivered:**` line (line 537); FORK-DELTA item 11 reads "Backlog 999.5 is closed."; the CHANGELOG 1.7.0_gg4 section names backlog 999.5. DEBT-17 is the single commit 898a20b2 (`build.gradle.kts`, `jacoco_summary.py`, `ForkCoverageScopeUnitTest`, FORK-DELTA, `fork-anchors.txt`). `build.gradle.kts` lines 345-352 and `jacoco_summary.py` lines 44-47 list both classes. My `test` run regenerated `build/reports/jacoco/fork/jacocoForkReport.xml` with 21 distinct classes, including `DeclaredNullability`, `WithheldMessageException`, `DescribeTableBuilder`, `ParentShape`, `MetadataFreeJsonWriter` and all five transform classes; every class added to `src/main` since `v1.7.0` is in that set. `ForkCoverageScopeUnitTest` 3/3 pass. `verify-merge-invariants.sh` on HEAD (pre-v1.7.0-tail, merge 38265d26): `anchor_rows=94 ... anchor_exempt=0 result=PASS`. |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/db/helper/ClickHouseFieldDescriptor.java` | names-only decode in `fromJsonRow` | VERIFIED | substantive, called by `DescribeTableBuilder.addRow` |
| `sink/db/helper/ClickHouseHelperClient.java` | shared UTF-8 raw-line reader for V1 and V2; V1 skips an unreadable table like V2 | VERIFIED | `readDescribeRows` used by both paths; `describeTableV1` catches `ClickHouseException \| IOException \| RuntimeException` (ae97c496) |
| `sink/db/mapping/DeclaredNullability.java` | rewritten `normalise`, `unescapeJsonLayer`, `elementKey` | VERIFIED | `elementKey` is used by `collectTuple` and `ParentShape`; pairs on the raw key from `ClickHouseColumn.of` |
| `sink/db/mapping/Column.java` | escape-aware enum label scan | VERIFIED | `extractEnumValues` reads through `DeclaredNullability.unescapeJsonLayer` |
| `RawIdentifierNameLiveTest`, `RawNameMatrix*`, `ForkCoverageScopeUnitTest`, `UnescapeJsonLayerTest`, `DescribeUnreadableEnumMockServerTest` | tests | VERIFIED | all present and passing at HEAD |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `ClickHouseHelperClient.describe*` | `DescribeTableBuilder.addRow` | `readDescribeRows` raw lines | WIRED | both V1 and V2 paths call it |
| `DescribeTableBuilder` | `ClickHouseFieldDescriptor.fromJsonRow` | per row | WIRED | name decoded, type text kept raw |
| `Column.extractEnumValues` | `DeclaredNullability.unescapeJsonLayer` | same JSON-layer decode | WIRED | package-private call; user-approved choice (UAT item 2b) |
| `DeclaredNullability.apply` | `normalise` then `ClickHouseColumn.of` then `collectTuple` | element key | WIRED | `elementKey` shared with `ParentShape` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| Discovered `Column` names | `name` | live `DESCRIBE TABLE` JSONEachRow on ClickHouse `latest` | Yes, `RawIdentifierNameLiveTest` writes records and reads typed values back | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase test set at HEAD | `./gradlew test --tests *RawIdentifierNameLiveTest *RawNameMatrixLiveTest *NullableTupleMatrixLiveTest *DeclaredNullabilityTest *ColumnTest *UnescapeJsonLayerTest *DescribeUnreadableEnumMockServerTest *ForkCoverageScopeUnitTest *TableTest` | 285 tests, 284 succeeded, 0 failed, 1 skipped (the skip is `JsonColumnTest.compareAllConfigurations`, matched by the `*ColumnTest` glob and one of the suite's known skips) | PASS |
| Per-class XML | RawIdentifierNameLiveTest 73, RawNameMatrixLiveTest 25, NullableTupleMatrixLiveTest 33, DeclaredNullabilityTest 69, ColumnTest 50, UnescapeJsonLayerTest 16, TableTest 7, DescribeUnreadableEnumMockServerTest 2, ForkCoverageScopeUnitTest 3 | all 0 failures, 0 errors, 0 skipped | PASS |
| Full-suite regression gate (given by the orchestrator, not re-run) | `./gradlew test` on HEAD | 1865 tests, 0 failures, 0 errors, 18 skipped | PASS (reported) |
| Anchors invariant | `verify-merge-invariants.sh 38265d26 HEAD` with `PRE_TAG=pre-v1.7.0-tail` | `result=PASS` | PASS |

### Probe Execution

Step 7c: SKIPPED. No `scripts/*/tests/probe-*.sh` exists and no plan declares a probe.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| NAME-01 | 28-01 | escaped-quote pin, red under mutation | SATISFIED | truth 1 |
| NAME-02 | 28-02 | live name-matrix spike | SATISFIED | truth 2 |
| NAME-03 | 28-03 | `fromJsonRow` raw names, V1 UTF-8 lines, `extractCommentV1` green | SATISFIED | truth 3 |
| NAME-04 | 28-03 | pairing on raw key, COLS-13 twins, IN-01/IN-02 | SATISFIED | truth 3 |
| NAME-05 | 28-04 | both-form live pins | SATISFIED | truth 4 |
| NAME-06 | 28-05 | enum labels measured and pinned | SATISFIED | truth 4 |
| NAME-07 | 28-05 | FORK-DELTA, CHANGELOG, item 11, 999.5 in the fix commit | SATISFIED | truth 5 |
| DEBT-17 | 28-06 | complete fork coverage list, edited once | SATISFIED | truth 5 |

All eight requirement IDs appear in the plan frontmatter (28-01 NAME-01, 28-02 NAME-02, 28-03 NAME-03 and NAME-04, 28-04 NAME-05, 28-05 NAME-06 and NAME-07, 28-06 DEBT-17) and are checked `[x]` and "Complete" in REQUIREMENTS.md. No orphaned requirement is mapped to Phase 28.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | | No `TBD`, `FIXME`, `XXX`, `TODO`, `HACK` or `PLACEHOLDER` in any line added by the phase to `src`, `build.gradle.kts`, `.github`, `docs` or `CHANGELOG.md` | | |

28-REVIEW.md (re-review after fixes): 0 critical, 0 warning, 5 info. The infos are test-quality notes and do not contradict a must-have. 28-SECURITY.md: 19/19 threats closed.

### Human Verification Required

None open. The two end-of-phase human items (the named exemption list sign-off, and the two choices made on the user's behalf) are recorded in 28-UAT.md as `pass`, 2/2, 0 issues.

### Gaps Summary

No gaps. Residual, documented limits (not gaps): a name or label holding a control character outside the nine spiked characters, and a `.` inside a name, are not measured; FORK-DELTA Known costs item 11 states this, and the rollout note for Phase 29 carries it. The roadmap checkbox for Phase 28 at ROADMAP line 100 is still `[ ]`; ticking it belongs to the orchestrator's phase-complete step.

---

_Verified: 2026-10-23T18:30:00Z_
_Verifier: Claude (gsd-verifier)_
