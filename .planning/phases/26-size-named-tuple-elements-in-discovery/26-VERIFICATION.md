---
phase: 26-size-named-tuple-elements-in-discovery
verified: 2026-10-22T14:30:00Z
status: passed
score: 5/5 must-haves verified
covered_files: [".planning/phases/26-size-named-tuple-elements-in-discovery/26-01-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-01-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-02-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-02-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-03-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-03-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-04-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-04-SUMMARY.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-05-PLAN.md", ".planning/phases/26-size-named-tuple-elements-in-discovery/26-05-SUMMARY.md", ".planning/scripts/fork-anchors.txt", "CHANGELOG.md", "build.gradle.kts", "docs/FORK-DELTA.md", "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java", "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java", "src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java", "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java"]
covered_digest: "v3:sha256:3921c870a10a96b9a5c340113ce470de7b383011d62f731260786f3adc41078b"
behavior_unverified: 0
overrides_applied: 0
coincidental_reliance_items:
  - truth: "Array(Array(Tuple(size1 ...))) shapes (n01, s4_a10, x05) round-trip with the declared element winning over the structural twin"
    reason: incidental-ordering
    harden: "Narrowed since the last report. The Table map is now order independent (IN-01 fix a58c66da, merge in registerValidColumn, pinned by theDeclaredElementWinsTheSharedNameInEitherRowOrder). The residual is DescribeTableBuilder.columnsByName.put, still last-wins (26-REVIEW IN-12): it only feeds the JSON/Dynamic ancestor walk, so it matters only for a declared JSON or Dynamic element named size<digits> at Array depth 2 or more, printed before its structural twin. Not reproduced and no known server shape reaches it. Apply the same declared-wins merge in the builder and add a JSON-element case to the order test."
---

# Phase 26: Size-Named Tuple Elements in Discovery Verification Report

**Phase Goal:** A table whose Tuple declares an element named `size`, `size0` or `size<digits>` accepts inserts, and the typed read-back equals the records. Discovery keeps the declared element at both drop sites and still drops every structural DESCRIBE row. The pins drive the production DESCRIBE loop, which clients V1 and V2 now share. This closes backlog 999.4 and FORK-DELTA Known costs item 10.
**Verified:** 2026-10-22
**Status:** passed
**Re-verification:** Yes, regeneration after the code-review `--fix` pass (commits `a58c66da..0a1c79f8`) and the incremental review `4271be66`. The prior report was `passed` 5/5 with no `gaps:` section, so every truth was re-checked against the current tree, not carried over.

## What changed since the prior report

Diff `80275a4a..HEAD` outside `.planning` touches 12 files, 132 insertions and 38 deletions:

- **Production code:** `Table.registerValidColumn` now uses `allColumnsMap.merge` (a registered declared element survives a later structural twin); `DescribeTableBuilder` and `DeclaredNullability` get comment and Javadoc edits only.
- **Docs and build:** `CHANGELOG.md` names both WARNs of the unreadable-parent path; `docs/FORK-DELTA.md` item 10 says the drift premise runs on demand only and the Table row describes the merge; `build.gradle.kts` adds `inputs.file(build.gradle.kts)` to every `Test` task.
- **Tests:** `theDeclaredElementWinsTheSharedNameInEitherRowOrder` (builder unit), `theDriftTagInTheBuildEqualsTheTagOfTheDriftTest` (fixtures unit), snapshot test renamed and described as regression-only, round-trip test closes its client on every path, fixture capture writes atomically in effect.
- No `src` change since `0a1c79f8` (the commits after it are docs only), so the JUnit XML of the full run is current.

## Goal Achievement

### Observable Truths (ROADMAP success criteria, the contract)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Spike record before any `src/main` change: DESCRIBE rows for the E-15 matrix and the new shapes, double row confirmed or denied, classifier rule stated | VERIFIED | Unchanged by the fix pass. Commit `40ffda7e` (26-01) touches no `src/main` file; the first `src/main` change is `9c186046`. 47 fixtures stay under `src/test/resources/describe-fixtures/`. E-02 records `double_row: CONFIRMED` and the classifier rule, and the current `DescribeTableBuilder.decide` still implements it step for step (read in full: null suffix and JSON/Dynamic ancestor, size-name test, parent raw type, `contains` shortcut, `ParentShape.parse`, `declaresElement`, `isOffsetsRow`). |
| 2 | V1 and V2 share one fork-owned builder; extraction is its own no-behaviour-change commit, green first; 999.4 pin drives the builder; one manual `CLIENT_VERSION=V1` run recorded | VERIFIED | `ClickHouseHelperClient` lines 530 and 557 construct `new DescribeTableBuilder(database, tableName)` in `describeTableV1` and `describeTableV2`; the anchor row `new DescribeTableBuilder(` count 2 holds on the current file. `shouldSkipDescribeRow` delegates to `DescribeTableBuilder.decide` (line 485). Extraction was commit `9c186046`, before the fix `7c00953e`. V1 run recorded in E-10 and re-confirmed by UAT check 19 (`client=V1`, all 47 snapshots through `describeTableV1`). |
| 3 | Docker-free unit table of captured real DESCRIBE rows pins the rule at both drop sites; declared element becomes Column; structural rows still dropped; structural twin never enters skip set | VERIFIED | Re-ran now: `./gradlew testUnit -PjavaVersion=21 --tests *DescribeTableBuilderUnitTest *DescribeFixturesUnitTest *DescribeFixtureSnapshotUnitTest *ClickHouseHelperClientForkUnitTest *TableTest` gave 315 tests, 315 succeeded, 0 failed. Code: `RowDecision.entersSkipSet()` excludes `SKIP_STRUCTURAL_SIZE`; `Table.updateParent` ARRAY branch reads `(SIZE_FIELD_MATCHER.test(childName) && !child.isDeclaredElement()) \|\| childName.endsWith(".null")`; `.null` rows and anything below a JSON or Dynamic ancestor are `SKIP_STRUCTURAL`. The new order test builds both row orders and asserts the declared `c.size1` Column, its Tuple membership and its children are identical. |
| 4 | Live: every E-15 affected shape inserts and typed read-back equals originals; DESC-05 rows green; D-04 pin flipped | VERIFIED | `build/test-results/test` XML (timestamp 2026-10-22T11:53Z, after the last `src` commit at 08:19Z): `TupleSizeNamedElementRoundTripLiveTest` 46 tests, 0 failures, 0 errors. Pre-fix red / post-fix green stays recorded in E-08 / E-09 (25 affected keys failed before, 46/46 after). UAT `26-UAT.md` 24 passed, 0 issues. Whole-tree XML total: 1617 tests, 0 failed, 0 errors, 18 skipped. The count is lower than the prior 1709 because `DescribeFixtureDriftLiveTest` (94) is now tag-gated to `describeFixtureDrift` (WR-02 / IN-09 fix); `build.gradle.kts:370` excludes the tag from `test`, and the new `theDriftTagInTheBuildEqualsTheTagOfTheDriftTest` guards the tag string. |
| 5 | Same commit as the fix: FORK-DELTA rows, `fork-anchors.txt` rows, CHANGELOG line, Known costs item 10 retired, backlog 999.4 delivered; anchors invariant passes | VERIFIED | Unchanged in `7c00953e` (ROADMAP `**Delivered:**` line, anchors, CHANGELOG, FORK-DELTA). The fix pass only refined wording. Anchor rows checked against the current files by direct count: `new DescribeTableBuilder(` 2/2, `elementKey(` 2/2, `!child.isDeclaredElement()` 1/1, upstream's old `Table` condition 0/0, `private boolean declaredElement;` 1/1, `shouldSkipDescribeRow` 3/3, `DeclaredNullability.apply(` 1/1, `buildDescribedTable(` 1/1. I did not run `verify-merge-invariants.sh` (it walks every commit of a range); the per-row counts above are its anchor check at HEAD. |

**Score:** 5/5 truths verified, 0 present-but-behavior-unverified.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/db/helper/DescribeTableBuilder.java` | Shared DESCRIBE row loop plus classifier | VERIFIED | Read in full; `decide` is static and pure; both describe paths call it through the builder; data flows from `fromJsonRow` rows to `build()` |
| `sink/db/mapping/ParentShape.java` | Parent type parse | VERIFIED | Called by `decide`; unchanged by the fix pass |
| `sink/db/mapping/Table.java` | Second drop site honours declared flag; order-independent registration | VERIFIED | `registerValidColumn` merge keeps a declared element against a structural twin; `updateParent` ARRAY check unchanged |
| `sink/db/mapping/Column.java` | `declaredElement` carrier | VERIFIED | `@Setter private boolean declaredElement` (line 59), set only by the builder for `KEEP_DECLARED` |
| `DeclaredNullability.elementKey` | Single Tuple-element key definition | VERIFIED | Anchor count 2 |
| `src/test/resources/describe-fixtures/` | 47 captured fixtures | VERIFIED | Snapshot unit test 47/47, fixtures unit test 96/96 |
| `captureDescribeFixtures`, `describeFixtureDrift` in `build.gradle.kts` | Manual tasks, not in `test`/`check` | VERIFIED | Tag declared once (line 213), excluded from `test` (line 370), included by the on-demand task |

### Key Link Verification

| From | To | Via | Status |
|------|----|----|--------|
| `describeTableV1` / `describeTableV2` | `DescribeTableBuilder.addRow/build` | direct calls | WIRED |
| `DescribeTableBuilder.decide` | `ParentShape.parse`, `ParentShape.declaresElement/isOffsetsRow` | direct calls | WIRED |
| `DescribeTableBuilder.addRow` | `Column.setDeclaredElement` -> `Table.updateParent` | flag read at ARRAY branch | WIRED |
| `DescribeTableBuilder.build` | `ClickHouseHelperClient.buildDescribedTable` -> `DeclaredNullability.apply` | package-private static call | WIRED |
| `shouldSkipDescribeRow` | `DescribeTableBuilder.decide` | delegation, test-only callers (IN-02, skipped by decision) | WIRED |

### Data-Flow Trace (Level 4)

Raw DESCRIBE JSON line -> `ClickHouseFieldDescriptor.fromJsonRow` -> `decide` (kept parents' raw types) -> `Column` list -> `buildDescribedTable` -> `Table` tuple fields -> RowBinary writer -> typed SELECT read-back equals records (46 live cases in the current XML). FLOWING, no static fallback.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Rule, both drop sites, order independence, drift-tag guard, helper pins | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit -PjavaVersion=21 --tests '*DescribeTableBuilderUnitTest' '*DescribeFixturesUnitTest' '*DescribeFixtureSnapshotUnitTest' '*ClickHouseHelperClientForkUnitTest' '*TableTest'` | 315 tests, 315 succeeded, 0 failed | PASS |
| Live round-trip, whole suite on the post-fix tree | JUnit XML in `build/test-results/test` (run on this tree, not re-run by me) | RoundTrip 46/0; builder 121, fixtures 96, snapshot 47, helper fork unit 51, all 0 failed; total 1617 tests, 0 failed, 0 errors, 18 skipped | PASS |

The full suite was not launched by me, as instructed. The four `src` files the fix pass changed in behaviour terms reduce to one: `Table.registerValidColumn`, covered by the order test and by the live XML produced after it.

### Probe Execution

No phase-declared probes (`probe-*.sh`). Skipped.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DESC-06 | 26-01 | Live DESCRIBE spike, classifier rule stated before code | SATISFIED | Truth 1 |
| DESC-07 | 26-02 | Shared builder extraction, behaviour-preserving, green first, V1 manual run | SATISFIED | Truth 2 |
| DESC-08 | 26-03 | Both drop sites keep declared element; structural rows dropped; unit table | SATISFIED | Truth 3 |
| DESC-09 | 26-04 | Live round-trip of every affected shape; D-04 flip | SATISFIED | Truth 4 |
| DESC-10 | 26-03 | FORK-DELTA, anchors, CHANGELOG, item 10, 999.4 in the fix commit | SATISFIED | Truth 5 |

All five IDs appear in plan frontmatter, are `[x]` in REQUIREMENTS.md (lines 55-59) and "Phase 26 / Complete" in its traceability table (lines 143-147). No requirement mapped to Phase 26 is unclaimed (no orphans).

### Anti-Patterns Found

Debt-marker scan (TBD, FIXME, XXX, TODO, HACK) over every added line of `git diff 80275a4a..HEAD -- src build.gradle.kts`: none. No stubs or empty implementations.

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `ClickHouseHelperClient.shouldSkipDescribeRow` (both overloads) | No `src/main` caller; two-arg form passes `Map.of()` | Info | 26-REVIEW IN-02, skipped by decision (anchor HLP-02 pins it); not a goal blocker |
| `DescribeTableBuilder` <-> `ClickHouseHelperClient` | Two-class cycle via `buildDescribedTable` | Info | IN-06, skipped by decision (anchor COLS-10) |
| `DescribeTableBuilder.columnsByName.put` | Last-wins, unlike `Table` | Info | IN-12, open, not reproduced (see below) |

### Advisory / Open Review Items (not blocking)

26-REVIEW (incremental, `4271be66`): 0 critical, 0 warning, 4 info. Disposition ledger: WR-01, WR-02, IN-01, IN-03, IN-04, IN-05, IN-07..IN-11 fixed; IN-02 and IN-06 skipped by decision; IN-12 and IN-13 open. 26-SECURITY remains `verified`.

- **IN-12 (open, info):** the builder's own name map is still last-wins, so it can disagree with `Table`. It is read only by the JSON/Dynamic ancestor walk (DESC-05). It would matter only for a declared JSON or Dynamic element named `size<digits>` at Array depth 2 or more, printed before a structural twin. The server prints the structural row first today, and the review did not verify that any server accepts such a shape. No affected fixture or live case exists, so no must-have is falsified. Recorded as the narrowed `coincidental_reliance_items` entry.
- **IN-13 (open, info):** the new `allColumnsMap.merge(` in upstream-owned `Table.registerValidColumn` has no `fork-anchors.txt` row. I confirmed it: rows 95-96 cover only the `updateParent` check. A future upstream sync that restores `allColumnsMap.put(...)` keeps the anchors green; only `theDeclaredElementWinsTheSharedNameInEitherRowOrder` (declared-first order) would fail. The merge gate does not protect this line. This is a rebasability gap in the fork's own safety net, not a failure of the phase goal, and the goal truths hold today. Suggested fix: add `Table.java<TAB>allColumnsMap.merge(<TAB>1<TAB>DESC-08` and a count-0 row for `allColumnsMap.put(column.getName(), column)`.
- **Drift premise is on demand only (known, documented):** `DescribeFixtureDriftLiveTest` no longer runs in `test`, so committed fixtures can go stale against `latest` unnoticed until `describeFixtureDrift` is run. FORK-DELTA item 10 states this.
- **Unreadable-parent path (D-03/D-04, documented):** a bare `size`/`size0` element whose parent type cannot be parsed or is absent is still dropped with metadata-only WARNs. CHANGELOG and FORK-DELTA now say so without the earlier "fails closed" wording. No known shape takes this path (every one of the 47 captured parents parses).

### Human Verification Required

None. No `<verify><human-check>` blocks exist in the five PLAN files. Every behavior-dependent truth has a passing behavioral test: red on the pre-fix tree and green on the fix tree (E-05/E-06, E-08/E-09), and green again in the XML of the post-fix-pass tree. UAT recorded 24 of 24 passed with 0 issues.

### Gaps Summary

No gaps. The goal is achieved on the current tree: both drop sites keep declared size-named Tuple elements and still drop structural rows, V1 and V2 share the production `DescribeTableBuilder` that the pins drive, and all 46 live round-trip cases store and read back equal. The fix pass strengthened the one order dependency the prior report flagged (the `Table` map) and left a narrower, unreproduced residual in the builder (IN-12) plus an unanchored upstream-owned line (IN-13). Both are advisory and neither falsifies a must-have. Environment notes: the working tree carries only the expected `gradlew.bat` CRLF diff and `.planning/state.json`; `VERSION` still reads `v1.7.0_gg3` and its bump belongs to the publication phase.

---

_Verified: 2026-10-22_
_Verifier: Claude (gsd-verifier)_
