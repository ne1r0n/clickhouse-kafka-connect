---
phase: 18-writer-correctness
reviewed: 2026-10-12T00:00:00Z
depth: standard
files_reviewed: 15
files_reviewed_list:
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - src/test/java/com/clickhouse/kafka/connect/sink/CdcTestFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/EmptyCdcSkipUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MissingNonNullableValueWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/processing/EmptyCdcExactlyOnceStateTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
findings:
  critical: 0
  warning: 4
  info: 5
  total: 9
status: issues_found
---

# Phase 18: Code Review Report

**Reviewed:** 2026-10-12
**Depth:** standard
**Files Reviewed:** 15
**Status:** issues_found

## Summary

The production changes are small and sound. I traced the exactly-once invariants and found no break:

- `skipEmptyCdcRecord` is a pure, static function of the `Record`. It reads no table metadata, so a retry that re-reads the table mapping (the `doInsertRawBinary` schema-refresh retry) skips the same records.
- The skip runs inside the stream-writer loops, so offsets, the `Processing` state and the dedup token still cover the whole batch.
- The `Utils` and `doInsertRawBinary` null-message guards are correct.
- The DateTime masking logs metadata only, and the new `DataException` messages are built from `Column` metadata only. `String.format` and `LOGGER.error(msg)` treat column names as arguments, not as format strings, so there is no format injection.
- The tests are largely honest. Where it matters they read back with `assertEquals`, compare exact bytes and tokens, and run a sentinel-absence check next to an exact-line presence check, so the masking assertions are not vacuous.

Nothing reaches BLOCKER level. The four warnings are:

1. The T-13-19 "closed" claim is stronger than the code supports.
2. A test re-introduces a Mockito pattern the same phase found to crash the JVM.
3. A test is sensitive to the source encoding.
4. A documentation claim about client V1 has no test behind it.

## Warnings

### WR-01: T-13-19 is documented as "closed" while the rethrown exception still carries record values into logs

**File:** `docs/FORK-DELTA.md` (Known costs item 6), `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:867-872`, `src/main/java/com/clickhouse/kafka/connect/util/Utils.java:78`
**Issue:**

- The generic `catch (Exception e)` in `doWriteColValue` now logs metadata only, but it still does `throw e`.
- For an invalid UUID or a non-numeric number, `e.getMessage()` embeds the record value. Two WARN/ERROR sinks then write it verbatim:
  - `doInsertRawBinary` calls `LOGGER.error("Error inserting records", e)` with the full throwable.
  - `Utils.handleException` calls `LOGGER.warn("Deciding how to handle exception: {}", e.getLocalizedMessage())`.
- The test `uuidWriteFailureRethrowsOriginalWithValueInMessage` pins this leak as expected behaviour.
- Known costs item 6 does disclose the residual and tracks it as backlog 999.3. But it opens with "T-13-19 is closed", and AGENTS.md says "Do not log secrets or data in plaintext".
- The mask therefore only covers the log lines that sat next to the exception, not the channels the value actually leaves through.
- The generic catch also drops the stack trace for every exception type, including internal bugs such as an NPE. The only remaining trace is the downstream "Error inserting records" log.

**Fix:** The structural fix is backlog 999.3:

- Catch the known value-embedding types (`IllegalArgumentException`, `NumberFormatException`, `DateTimeException`) at the `doWriteColValue` boundary.
- Rethrow a `DataException` built from the column and type metadata, with the cause attached via a masked wrapper.
- Keep retry classification unchanged.

Until then, reword item 6 to "T-13-19 is partly mitigated: the dedicated ERROR lines are masked, the rethrown message is not", so the security ledger does not overstate the closure. Consider logging the masked stack trace at DEBUG in the generic catch for internal-bug diagnosability.

### WR-02: `ClickHouseWriterNullMessageUnitTest` inline-mocks `ClickHouseHelperClient`, the pattern this phase found to crash the test JVM

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java:118`
**Issue:**

- `CapturingClickHouseWriter`'s javadoc says inline-mocking `ClickHouseHelperClient` crashed the Semeru/OpenJ9 test JVM natively in `retransformClasses`, twice in the full suite.
- The same phase's `deferred-items.md` records that this test carries the same risk and leaves it unfixed.
- The result is an intermittent hard worker death (exit 255) that takes down every test in that fork. It is not a clean test failure.
- The new class is the only new test that still uses the pattern.

**Fix:** Avoid inline-mocking a concrete class. Either add a protected seam in the test-only subclass, or build a real `ClickHouseHelperClient` through its builder without calling the network. Then make `pinReplica` and `unpinReplica` observable through a counting subclass:

```java
// e.g. a tiny recording subclass instead of mock(...)
final class RecordingHelperClient extends ClickHouseHelperClient {
    int pins; int unpins; final boolean v2;
    @Override public void pinReplica() { pins++; }
    @Override public void unpinReplica() { unpins++; }
    @Override public boolean isUseClientV2() { return v2; }
}
```

If the constructor makes this impractical, move the retry assertions behind a protected `shouldRetryOnSchemaChange(Exception)` seam, or pin the test to a HotSpot-only tag.

### WR-03: The emoji literal makes `valueLengthCountsUtf16CodeUnits` depend on the compiler's source encoding

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java:56`
**Issue:**

- `GRIN_EMOJI = "😀"` is a raw non-ASCII literal.
- `build.gradle.kts` sets no `options.encoding`, and it deliberately runs tests with `file.encoding=windows-1252`.
- Under JDK 17, javac decodes the source with the Gradle JVM's platform charset. On a non-UTF-8 default (a C/POSIX locale on a Linux runner, or Windows) the four UTF-8 bytes become four chars, or four U+FFFD.
- The test asserts `value.length() == 18` and `valueLength == 18`, so it fails (or passes for the wrong reason) there.
- The assertion is the one that pins "UTF-16 code units", so it must not depend on the host locale.

**Fix:** Use the escape form so the constant is encoding-independent:

```java
private static final String GRIN_EMOJI = "😀"; // U+1F600, 2 UTF-16 code units
```

### WR-04: The "clients V1 and V2 accept an empty insert body" claim has no test, and the all-empty case is not tried with request compression

**File:** `docs/FORK-DELTA.md` (empty-CDC row), `src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java:91-97`, `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1440-1475` (`doInsertJsonV1`, `doInsertRawBinaryV1`)
**Issue:**

- The doc states that an all-empty batch "sends an empty insert body, which clients V1 and V2 accept as a zero-row insert".
- The live tests run only against client V2 (the project constraint is ClickHouse `latest` and V2 only) with default properties.
- The Docker-free tests replace `doInsertRawBinary` and `doInsertJson`, so the V1 `ClickHousePipedOutputStream` and `doInsertJsonV1` paths never see a zero-byte body.
- Nothing exercises an empty body together with request compression (LZ4 or gzip), where a zero-byte compressed stream can be handled differently.
- If either unverified combination errors, the whole all-empty batch (for example a Debezium truncate-only batch) fails with `errors.tolerance=none` and stalls the partition. Before this phase it did not, on the metadata-free route.

**Fix:** Either of these closes the gap:

- Add a live all-empty case with `compressClientRequest`/LZ4 enabled (reuse the `Lz4RoundTripLiveTest` props), plus a V1 variant guarded by `CLIENT_VERSION`.
- Short-circuit in the writer when every record is skipped (skip the network insert but still return normally). That is safe for exactly-once because state and offsets live in `Processing`, not in the insert. It removes the whole class of risk and makes the "empty body" claim unnecessary.

If neither is done, soften the doc to "client V2, uncompressed, verified".

## Info

### IN-01: The retry test reuses the same `Record` instances, so it cannot catch non-determinism in conversion

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/processing/EmptyCdcExactlyOnceStateTest.java:131-148`
**Issue:**

- `retriedBatchRebuildsTheIdenticalBodyAndToken` converts the batch once and feeds the same `List<Record>` to both writers.
- A real retry re-reads `SinkRecord`s from Kafka and re-runs `Record.convert`. That is where non-determinism (map ordering, injected `_version`) could creep in.
- The skip is pure, so the test passes, but it proves less than its title claims.

**Fix:** Build the retry batch with a second `records(mixedBatchEnvelopes())` call, so conversion runs twice from fresh `Struct`s.

### IN-02: Only the DateTime branch of the unsupported-conversion message is pinned

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java:176-186`, `docs/FORK-DELTA.md` (doWriteDates row)
**Issue:**

- The doc says `for column '<name>'` is added for "Date, Date32, DateTime and DateTime64".
- `conversionDataExceptionNamesColumnWithoutValue` covers only the DateTime string-parse path.
- The non-string `unsupported = true` branches of Date, Date32 and DateTime64 (for example a STRING field written to `Date`) are unpinned.

**Fix:** Add a small parameterised test, `Type.Date`/`Date32`/`DateTime64` with a STRING `Data`, asserting the exact message and the sentinel absence.

### IN-03: The bypass-route live test skips the `ts` read-back without saying why

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java:62-66, 103, 123-129`
**Issue:**

- `assertTs=false` on the bypass route hides the stored timestamp.
- The reason is the documented L-03 behaviour: Jackson writes epoch milliseconds that ClickHouse reads as seconds.
- The test file never says so. A reader could take the omission for an oversight, and the "read back and `assertEquals` all columns" rule in AGENTS.md is silently relaxed.

**Fix:** Add a one-line comment on the parameter or the test that points at L-03, so a future fix of L-03 knows to turn the assertion on.

### IN-04: Duplicated test constants and placeholder-looking display names

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/MissingNonNullableValueWriteTest.java:36-38`, `WriterLogMaskingUnitTest.java:41`, `WriterErrorPathLiveTest.java:60-62`; `src/test/java/com/clickhouse/kafka/connect/sink/db/EmptyCdcSkipUnitTest.java:176, 188`
**Issue:**

- `SENTINEL` is declared three times and `MISSING_VALUE_HINT` twice. AGENTS.md says DRY, and a drift in one copy would weaken that copy's absence check.
- Two `@DisplayName`s end in a literal `{}` ("writes the defaults-only row {}", "writes {}"). `@DisplayName` is not formatted, so it reads like a leftover placeholder.

**Fix:** Move `SENTINEL` and `MISSING_VALUE_HINT` into a shared test constants holder, for example next to `LogCapture` or `CdcTestFixtures`. Reword the two display names to say `writes the defaults-only row {}` as JSON text, or drop the braces.

### IN-05: Skipped empty CDC records are still counted as inserted rows in JMX statistics

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:69` (consumer), `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1314-1325`
**Issue:**

- `statistics.recordTopicStats(records.size(), ...)` runs after a successful `doInsert`, and `records.size()` includes the skipped records.
- This was already true for the metadata-free route. The phase extends it to all routes, so the "records inserted" JMX counter now overstates the stored rows for every Debezium user, by the number of truncate or null-struct events.
- It is a metrics inaccuracy only. Offsets and state are correct by design.

**Fix:** Either leave it as is and say so in `FORK-DELTA.md` ("counters are record-based, not row-based"), or have the writer report the skipped count so `recordTopicStats` can subtract it.

---

_Reviewed: 2026-10-12_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
