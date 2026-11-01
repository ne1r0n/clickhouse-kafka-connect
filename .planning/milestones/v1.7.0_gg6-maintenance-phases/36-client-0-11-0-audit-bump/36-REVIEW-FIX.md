---
phase: 36-client-0-11-0-audit-bump
fixed_at: 2026-10-31T00:00:00Z
review_path: .planning/phases/36-client-0-11-0-audit-bump/36-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 36: Code Review Fix Report

**Fixed at:** 2026-10-31
**Source review:** `.planning/phases/36-client-0-11-0-audit-bump/36-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (WR-01, WR-02; scope was narrowed to the two warnings, so IN-01 to IN-06 are recorded below as skipped out of scope)
- Fixed: 2
- Skipped: 0 in-scope (6 Info findings out of scope)

**Verification ran in the main checkout** (worktrees were not used), on branch `sync/v1.7.0`, so the numbers are reproducible from the tree.

## Fixed Issues

### WR-01: Golden RowBinary hashes depend on the compile-time source encoding, and the Javadoc says otherwise

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java`
**Commit:** 7e717d2d
**Applied fix:** The three non-ASCII literals on lines 104-105 are now unicode escapes: `"unicodé 中文 😀"` (surrogate pair for the emoji) and `"é"`. The string values are byte-identical to before, so `src/test/resources/rowbinary-golden-sha256.txt` is untouched (`git diff` empty). `grep -nP '[^\x00-\x7F]'` on the file now finds nothing. `RowBinaryClientParityLiveTest` is the only other file that uses the cases or the golden, and it has no non-ASCII characters. `MetadataFreeJsonTypeParityLiveTest` does contain `"Grüße ✓"` raw literals (lines 168, 826), but it does not read the golden file, so it was left alone as unrelated to the finding.
**Verification:** `RowBinaryClientParityLiveTest` passes (1 test, 0 failures, 0 skipped) with the default test task, which runs the test JVM with `file.encoding=windows-1252`.

### WR-02: The documented "operator's own `ignore_unknown_config_key` still wins" is pinned by no test

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java`
**Commit:** bdd2e592
**Applied fix:** New test `operatorIgnoreFlagWins`, using the file's existing `insertOnce` helper and `IGNORE_KEY` constant. It builds a V2 client with `?ignore_unknown_config_key=false&ssl=true` and asserts a `ClientMisconfigurationException` (the type was confirmed against client 0.11.0 with a probe: `Unknown and unmapped config properties: [ssl]`) whose message names `ssl`.
**Mutation check:** Temporarily moving `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true")` below the `jdbcConnectionProperties` loop made exactly this test fail (the connector default overwrote the operator's `false`). The main-source change was reverted with `git checkout --`, and the committed tree has no change to `ClickHouseHelperClient.java`.
**Anchors:** No row change needed. The CLT-03 anchor rows count lines in `ClickHouseHelperClient.java` only, and no main source changed. The anchors mirror reads `rows=152 bad=0`.

## Skipped Issues

All six are Info findings, outside the requested scope (Critical and Warning only). None was attempted.

### IN-01: `createClientV2` still logs a "ClickHouse URL" the client no longer receives

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

### IN-02: `v1OnlyPropertiesAreAccepted` does not check the `ssl=true` it names

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

### IN-03: Stale "changes this constructor (Phase 36)" comment

**File:** see `36-REVIEW.md`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

### IN-04: `ClientErrorShapes` has an inverted assertion and a two-line assumption in `clientVersion()`

**File:** see `36-REVIEW.md`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

### IN-05: `v1StillCannotDecodeZstd` hardcodes `http://` and ignores the base config's SSL setting

**File:** see `36-REVIEW.md`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

### IN-06: `ConfluentPlatform.generateData` wait loop uses strict inequality and leaves small hygiene gaps

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java`
**Reason:** skipped, out of scope (Info finding)
**Original issue:** See `36-REVIEW.md`.

---

_Fixed: 2026-10-31_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
