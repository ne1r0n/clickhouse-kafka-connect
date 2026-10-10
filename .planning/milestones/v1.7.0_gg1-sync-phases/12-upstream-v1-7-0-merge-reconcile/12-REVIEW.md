---
phase: 12-upstream-v1-7-0-merge-reconcile
reviewed: 2026-10-07T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - .claude/CLAUDE.md
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java
findings:
  critical: 0
  warning: 3
  info: 5
  total: 8
status: issues_found
---

# Phase 12: Code Review Report

**Reviewed:** 2026-10-07
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Scope was the Phase 12 delta on top of merge commit `54e63648` (`git diff 54e63648 HEAD`). The production change is two small edits:
- The `ClickHouseWriter` MAP null guard now fires only for `Nullable` columns.
- The dead D-17 constants were removed from `ClickHouseHelperClient`.

The rest is tests, the build file, and docs.

The new byte-level pins agree with what the code does. The non-nullable null-Map case does produce `00` and `00 00`. I found no blocker. The weaknesses are:
- The scoped MAP guard is effectively dead for real ClickHouse schemas.
- The same class of stray-marker bug survives in the TUPLE case.
- The MAP child-null path writes no bytes at all when defaults are off.
- The compression live tests no longer exercise the failure mode they are named after.

No security issues were found in the diff.

## Warnings

### WR-01: A null non-nullable Map child (e.g. `Array(Map(...))` element) writes zero bytes when defaults are off, desyncing RowBinary

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:614-637`
**Issue:** After the WRT-06 scoping, a null `Data` reaching the `MAP` case with a non-nullable column falls through to `if (mapTmp == null) { if (defaultsSupport) writeVarInt(0); break; }`. The comment says the size byte was "already written by `doWriteCol`". That is true only for a root-level column.

The ARRAY recursion calls `doWriteColValue(col.getArrayType(), ...)` directly, and so does the MAP-value recursion. Neither goes through `doWriteCol`. For `Array(Map(K,V))` with a null element and `defaultsSupport == false`, nothing is written for that element. The element's size varint is missing, so every following byte is misparsed. Before the sync, the unconditional guard wrote a stray `0x01`, which was also wrong, so this is not a regression against upstream. The fork now documents the case as handled ("#820's empty map"), but only the root-level case is. No test covers a null element inside `Array(Map)` or a Map value.
**Fix:** Write the empty-map varint whenever the null did not come through `doWriteCol`. For example, have the ARRAY and MAP-value recursion normalise a null non-nullable Map child to an empty map before the call, and keep the `defaultsSupport` gate only for the root call. Add a unit test over `doWriteColValue` for `Array(Map(String,String))` with `[null]` in both RowBinary and RowBinaryWithDefaults, and assert the exact bytes.

### WR-02: The TUPLE null guard is still unconditional, so a non-nullable Tuple child gets a stray null marker (same defect class as WRT-06)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:662-669`
**Issue:** WRT-06 scoped the MAP guard to `col.isNullable()` because an unconditional marker desynchronises the stream. The TUPLE guard has the same shape and was left unconditional. A null Tuple that reaches `doWriteColValue` as an `Array(Tuple(...))` or Map-value child has a non-nullable column. A Tuple has no null marker on the wire, so the `writeNull` byte (`0x01`) is read as the first element's bytes. The fork's own scoping rationale for MAP applies unchanged here, and the two cases are now inconsistent. FORK-DELTA row 36 describes "TUPLE and VARIANT" as carrying the same guard without noting the divergence.
**Fix:** Mirror the MAP treatment: write the null marker only when `col.isNullable()`. For a non-nullable null Tuple, throw `DataException` naming the column, or write default-valued elements. Pin it with a byte-level test next to `NestedNullableCompositeWriteTest`.

### WR-03: The compression live and unit tests no longer reproduce the failure they pin, and the documented operator hazard has only a tautological pin

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java:115-119`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java:159-203`
**Issue:** With `compress=false` now unconditional, the existing live tests (`v2PingAndQueryRoundTrip` etc.) never request a compressed response. They pass even if LZ4 handling were broken, because no compressed block is sent. The one case that matters for the retained `setCompressionMethod=true` default is not live-tested. That case is an operator `compress=true` in `jdbcConnectionProperties`, which re-enables response compression, and the flag value that makes it work against a 26.9+ server. FORK-DELTA calls this "the hazard that remains", yet the only automated pin is `operatorCompressTrueOverridesV2ResponseCompression`. That test asserts the client configuration map contains the string the test itself supplied, so it proves nothing about the wire. CMP-03 (`sinkTaskInsertAndReadBackWithSetCompressionMethod`) also never sets `compress=true`. The observations in Recorded Facts 25 and 26 are manual and bound to a specific `latest` build (`26.9.12.8`). A future `latest` change, or a client bump, would not fail any test.
**Fix:** Add a live case that builds a V2 client with `?compress=true`:
- With `setCompressionMethod=true`, assert `queryV2(TYPED_QUERY)` succeeds and `getSetting('network_compression_method')` returns `lz4`.
- Optionally pin the `false` case as an expected failure, documented as a known hazard.

Reword the unit test so it asserts behaviour (the connector default plus operator override precedence) rather than echoing its input.

## Info

### IN-01: The MAP null guard is unreachable for real ClickHouse schemas, and the docs describe an impossible scenario

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:614`, `docs/FORK-DELTA.md:36`
**Issue:** `Column.java:386` states that ClickHouse forbids Nullable wrapping for Array, Map and Variant. `Nullable(Tuple)` is likewise not a valid type. A `Column` with `type=MAP` and `isNullable=true` therefore cannot come from `DESCRIBE`, so the guard `value.getObject() == null && col.isNullable()` can only fire in the synthetic unit test. The FORK-DELTA row ("null `Nullable(Tuple(...))` or `Nullable(Variant(...))` entry reached as a child") describes types the server rejects. The pin test `nullableMapNullThroughDoWriteColKeepsNullMarkerInRowBinary` pins a state no schema can produce. This is harmless but misleading to the next syncer.
**Fix:** Say plainly in the code comment and the FORK-DELTA row that the nullable branch is defensive and cannot occur with a server-reported schema. Alternatively, drop the branch and rely on #820's handling, once WR-01 is fixed.

### IN-02: Stale and misleading wording in test javadocs and the FORK-DELTA lz4 rows

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java:11-19`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java:31-36`, `docs/FORK-DELTA.md:78`, `docs/FORK-DELTA.md:5`
**Issue:**
- Both class javadocs still say every client "sends `network_compression_method=lz4`" and that the connector "forces" it. That is now conditional on `setCompressionMethod` and secondary to `compress=false`.
- The FORK-DELTA retired-capability row says the old rule "resolved the capability to a fixed older `at.yawk.lz4` release", then says "both resolved to the same version". The checklist (Fact 24) shows 1.10.4 before and after. The "older release" wording contradicts the redundancy claim.
- The FORK-DELTA header still says every row was checked against `sync/v1.6.0` commit `09cae191`. The `VERSION` row still reads `v1.6.0_gg1` on the v1.7.0 sync branch.

**Fix:** Update the javadocs to describe the flag and `compress=false`. Remove "fixed older" from the lz4 row. Refresh the header and the `VERSION` row, or note that they are updated in a later phase.

### IN-03: Planning-process references in code comments

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java:81-82`, `:98-99`, `:113-115`
**Issue:** Comments such as "2026-10-07 user-approved tightening of the CR-02 pins (REQUIREMENTS.md ...)" record planning history rather than a code invariant. AGENTS.md asks for comments that convey invariants. The same sentence is repeated three times (DRY).
**Fix:** Keep one comment on the invariant: a non-nullable Map takes #820's empty map, and only a nullable column takes the null marker. Leave the history to the git log or FORK-DELTA.

### IN-04: `KeeperStateProviderRestartLiveTest` never closes the providers' clients

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java:50-52`, `:79-100`
**Issue:** `newProvider(props)` builds a `KeeperStateProvider`, which constructs a `ClickHouseHelperClient` (AutoCloseable, V1 plus V2 clients). The test creates four to five of these per invocation and never closes any. `KeeperStateProvider` exposes no `close()`, so the test cannot release them. The leak is bounded to the test JVM, but it conflicts with AGENTS.md's "avoid resource leaks".

The `freshKeyReadsBackNone` case also uses a brand-new table, so `NEVER_WRITTEN_PARTITION = 1` adds nothing. Any key reads back empty. The test would be stronger if it wrote partition 0 and then read partition 1 from a fresh provider.
**Fix:** Construct providers through `new KeeperStateProvider(ClickHouseHelperClient)` with a client the test closes in `finally`. For the second point, write partition 0 first, then assert partition 1 is `NONE`.

### IN-05: Double blank line left where the constants were removed

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:111-112`
**Issue:** Deleting the D-17 constants left two consecutive blank lines after `CONNECT_CLIENT_NAME`. Spotless here does not run `googleJavaFormat`, so nothing will clean it up.
**Fix:** Remove one blank line.

---

_Reviewed: 2026-10-07_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
