---
phase: 26-size-named-tuple-elements-in-discovery
verified: 2026-10-22T00:00:00Z
status: passed
score: 5/5 must-haves verified
covered_files: [".planning/phases/26-size-named-tuple-elements-in-discovery/26-01-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-01-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-02-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-02-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-03-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-03-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-04-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-04-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-05-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-05-SUMMARY.md", ".planning/scripts/fork-anchors.txt", "CHANGELOG.md", "docs/FORK-DELTA.md", "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java", "src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java"]
covered_digest: "v3:sha256:1ebd755c97d8b86de4380cc572c86f82f352a4928af8f91be58af8811325e366"
behavior_unverified: 0
overrides_applied: 0
coincidental_reliance_items:
  - truth: "Array(Array(Tuple(size1 ...))) shapes (n01, s4_a10, x05) round-trip with the declared element winning over the structural twin"
    reason: incidental-ordering
    harden: "DescribeTableBuilder.columnsByName.put and Table.registerValidColumn let the last same-named row win; the declared row wins only because this server prints the structural row first. Make the winner order-independent (26-REVIEW IN-01); the drift premise (DescribeFixtureDriftLiveTest) is the only guard today."
---

# Phase 26: Size-Named Tuple Elements in Discovery Verification Report

**Phase Goal:** A table whose Tuple declares an element named `size`, `size0` or `size<digits>` accepts inserts, and the typed read-back equals the records. Discovery keeps the declared element at both drop sites and still drops every structural DESCRIBE row. The pins drive the production DESCRIBE loop, which clients V1 and V2 now share. This closes backlog 999.4 and FORK-DELTA Known costs item 10.
**Verified:** 2026-10-22
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria, the contract)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Spike record before any `src/main` change: DESCRIBE rows for the E-15 matrix and the new shapes, double row confirmed or denied, classifier rule stated | VERIFIED | Commit `40ffda7e` (26-01) touches no `src/main` file (0 matches); first `src/main` change is `9c186046`. 47 fixtures committed under `src/test/resources/describe-fixtures/` (jsonl, columns.txt, SERVER_VERSION). Evidence E-02 records `double_row: CONFIRMED` (n02 `c.size0` printed twice, structural `UInt64` first, declared second), the 9-step classifier rule, and measured affected set = predicted (25 keys). The rule in E-02 matches `DescribeTableBuilder.decide` line for line. |
| 2 | V1 and V2 share one fork-owned builder; extraction is its own no-behaviour-change commit, green first; 999.4 pin drives the builder; one manual `CLIENT_VERSION=V1` run recorded | VERIFIED | `ClickHouseHelperClient.describeTableV1` (line ~530) and `describeTableV2` (line ~557) both construct `DescribeTableBuilder` and call `addRow`; the duplicated loops are gone from the diff. Extraction is separate commit `9c186046`, before fix `7c00953e`. `ClickHouseHelperClientForkUnitTest` line 315 drives `new DescribeTableBuilder(...)`. E-04 records testUnit, snapshots and full suite green on the extraction; E-10 records V1 run: 113 tests, 0 failed, `DescribeFixtureDriftLiveTest client=V1`, plus an extra V1 round-trip 46/46. |
| 3 | Docker-free unit table of captured real DESCRIBE rows pins the rule at both drop sites; declared element becomes Column; structural rows (offsets, null map, Map keys/values, nested `.size1`, DESC-05 JSON/Dynamic walk) still dropped; structural twin never enters skip set | VERIFIED | `DescribeTableBuilderUnitTest` (120 tests, 0 fail in JUnit XML): per-fixture "declared elements" and "no WARN" rows over all 47 keys, cascade guards (declared element children are Columns; structural row below declared still dropped), null-map/keys rows, nested `size1` registered once and in no Tuple, DESC-05 "no Column below a JSON root", rule table, `Table.updateParent` flagged vs unflagged child. Code: `RowDecision.entersSkipSet()` excludes `SKIP_STRUCTURAL_SIZE`; `Table.updateParent` ARRAY branch reads `!child.isDeclaredElement()`. E-05 shows the new tests red on the pre-fix tree with the failing set equal to the prediction (52 + 3), E-06 shows green after the fix. |
| 4 | Live: every E-15 affected shape inserts and typed read-back equals originals; DESC-05 rows green; D-04 pin flipped (no unpaired-element WARN) | VERIFIED | `TupleSizeNamedElementRoundTripLiveTest` JUnit XML: 46 tests, 0 failures (class asserts typed read-back via `NullableRoundTrip.selectAndAssert` and zero WARN on the helper and DeclaredNullability loggers). E-08: same class on the extraction tree fails exactly the 25 affected keys (23 CANNOT_READ_ALL_DATA, 1 ATTEMPT_TO_READ_AFTER_EOF, r01 stores wrong value 1667391751). E-09: 46/46 on the fix tree, r01 stores size 7. D-04 pin renamed to `sizeNamedTupleElementIsKeptByDiscoveryWithoutWarning` (line 611) and green. Full suite `build/test-results`: test 1709 tests, 0 failures, 0 errors, 18 skipped; testUnit 1126, 0 failures. |
| 5 | Same commit as the fix: FORK-DELTA rows (`ClickHouseHelperClient`, `Table.java`), `fork-anchors.txt` rows, CHANGELOG line, Known costs item 10 retired, backlog 999.4 delivered; anchors invariant passes | VERIFIED | `git show --stat 7c00953e` contains ROADMAP.md (+1 `**Delivered:**` line on Phase 999.4), fork-anchors.txt, CHANGELOG.md (+9, "1.7.0_gg4, unreleased" Bug Fixes), docs/FORK-DELTA.md (14 changed lines: two new rows plus item 10 rewritten as "Backlog 999.4 is closed"), plus the code. Anchor rows for `new DescribeTableBuilder(`, `elementKey(`, `!child.isDeclaredElement()`, count-0 row for upstream's old `Table` condition, and `declaredElement`. Anchors invariant recorded green at every commit in E-12 (tail gate and selftest). |

**Score:** 5/5 truths verified, 0 present-but-behavior-unverified.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/db/helper/DescribeTableBuilder.java` | Shared DESCRIBE row loop plus classifier | VERIFIED | 290 lines, `decide` is pure; wired from both describe paths; data flows from real `DESCRIBE` rows to `build()` |
| `sink/db/mapping/ParentShape.java` | Parent type parse (array depth, Tuple element keys, offsets-row test) | VERIFIED | Used by `decide`; text-built comparisons, no integer parse |
| `sink/db/mapping/Table.java` | Second drop site honors declared flag | VERIFIED | One-line change; unflagged `size<digits>` still ignored |
| `sink/db/mapping/Column.java` | `declaredElement` carrier | VERIFIED | Set only by builder (`KEEP_DECLARED`) |
| `DeclaredNullability.elementKey` | Single Tuple-element key definition | VERIFIED | Used by `collectTuple` and `ParentShape.declaresElement` |
| `src/test/resources/describe-fixtures/` | 47 captured fixtures | VERIFIED | Drift live test 94/94, unit 95/95, snapshot 47/47 |
| `captureDescribeFixtures` in `build.gradle.kts` | Manual JavaExec, wired to nothing else | VERIFIED | Present, group verification |

### Key Link Verification

| From | To | Via | Status |
|------|----|----|--------|
| `describeTableV1` / `describeTableV2` | `DescribeTableBuilder.addRow/build` | direct calls | WIRED |
| `DescribeTableBuilder.decide` | `ParentShape.parse` / `DeclaredNullability.elementKey` | direct calls | WIRED |
| `DescribeTableBuilder` | `Column.setDeclaredElement` -> `Table.updateParent` | flag read at ARRAY branch | WIRED |
| `shouldSkipDescribeRow` | `DescribeTableBuilder.decide` | delegation (kept for tests, see advisory IN-02) | WIRED |

### Data-Flow Trace (Level 4)

Raw DESCRIBE JSON line -> `ClickHouseFieldDescriptor.fromJsonRow` -> `decide` (rawTypes of kept parents) -> `Column` list -> `buildDescribedTable` -> `Table` tuple fields -> RowBinary writer -> typed SELECT read-back equals records (46 live cases). FLOWING, no static fallbacks.

### Behavioral Spot-Checks

Not re-run live (Docker). Read the JUnit XML of the final tree instead: `build/test-results/test` 1709/0 failed/18 skipped; `testUnit` 1126/0 failed/2 skipped; per-class counts match E-12 (RoundTrip 46, Builder 120, FixturesUnit 95, Snapshot 47, Drift 94, HelperClientForkUnit 51, DeclaredNullability 66). Pre-fix red / post-fix green was independently recorded (E-05/E-06, E-08/E-09).

### Probe Execution

No phase-declared probes (`probe-*.sh`). Skipped.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DESC-06 | 26-01 | Live DESCRIBE spike, classifier rule stated before code | SATISFIED | Truth 1 |
| DESC-07 | 26-02 | Shared builder extraction, behavior-preserving, green first, V1 manual run | SATISFIED | Truth 2 |
| DESC-08 | 26-03 | Both drop sites keep declared element; structural rows dropped; unit table | SATISFIED | Truth 3 |
| DESC-09 | 26-04 | Live round-trip of every affected shape; D-04 flip | SATISFIED | Truth 4 |
| DESC-10 | 26-03 | FORK-DELTA, anchors, CHANGELOG, item 10, 999.4 in the fix commit | SATISFIED | Truth 5 |

All five IDs are marked `[x]` and "Phase 26 / Complete" in REQUIREMENTS.md; no requirement mapped to Phase 26 is unclaimed by a plan (no orphans).

### Anti-Patterns Found

Debt-marker scan (TBD/FIXME/XXX/TODO/HACK) over every added line in the phase's src/main and src/test files: none. No stubs, no empty implementations.

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `ClickHouseHelperClient.shouldSkipDescribeRow` (both overloads) | No `src/main` caller; two-arg form uses `Map.of()` (weaker rule than production) | Info | 26-REVIEW IN-02, open; not a goal blocker |

### Advisory / Open Review Items (not blocking)

26-REVIEW has 2 warnings and 8 info items, all dispositioned `open`; no blockers, and 26-SECURITY is `verified` with `threats_open: 0`.

- **WR-01 (warning):** the unreadable-parent path (type the pinned client cannot parse, or no raw type for the parent) keeps the pre-phase behavior, so a declared `size`/`size0` element is still dropped there with one metadata-only WARN. This is the locked decision D-03/D-04 in 26-CONTEXT (rejected alternative: refusing the table), and FORK-DELTA states it honestly. The test name and a `DescribeFixturesUnitTest` comment say "fails closed", and the CHANGELOG line says "at any depth" without the caveat. Wording issue, not a goal failure: every one of the 47 captured parents parses (E-02 A4), so no known shape takes this path.
- **WR-02 (warning):** the drift live test compares raw DESCRIBE bytes against a floating `latest` image; a server formatting change would turn the suite red for non-connector reasons (known `latest` drift, see project memory).
- **Coincidental reliance (incidental ordering):** see frontmatter. Depth-2 Array shapes are correct because this server prints the structural row before the declared one; nothing in the code enforces that order. The drift test guards it. Advisory only; it does not change the score or status.

### Human Verification Required

None. No `<verify><human-check>` blocks exist in any of the five PLAN files, and every behavior-dependent truth (declared element survives and round-trips; structural rows still dropped; cascade guard) has a passing behavioral test, red on the pre-fix tree and green on the fix tree.

### Gaps Summary

No gaps. The goal is achieved in the codebase: both drop sites keep declared size-named Tuple elements and still drop structural rows, V1 and V2 share the production `DescribeTableBuilder` that the pins drive, and all 46 live round-trip cases (25 formerly failing) store and read back equal. Environment notes: the working tree carries the expected `gradlew.bat` CRLF diff and `.planning/state.json`; the OpenJ9 `Mockito.spy` crash recorded in E-12 is a known environment fault, not a test failure. `VERSION` still reads `v1.7.0_gg3`; the CHANGELOG section is `1.7.0_gg4, unreleased`, and the bump belongs to the publication phase, not this one.

---

_Verified: 2026-10-22_
_Verifier: Claude (gsd-verifier)_
