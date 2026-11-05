---
phase: 47-task-stop-retry-classification
reviewed: 2026-11-05T00:00:00Z
depth: deep
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java
  - docs/FORK-DELTA.md
  - CHANGELOG.md
  - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md
findings:
  critical: 0
  warning: 5
  info: 4
  total: 9
  necessity: 0
status: issues_found
---

# Phase 47: Code Review Report

**Reviewed:** 2026-11-05
**Depth:** deep
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Reviewed the diff `427f30b4..HEAD` for `src`, `CHANGELOG.md` and `docs/FORK-DELTA.md`: the `stop()` try/finally,
the two new test classes, FORK-DELTA items 8, 13, 17 and 24, and the new `1.7.0_gg8` CHANGELOG section. I read the
call chain below `stop()` (`ProxySinkTask.stop`, `ClickHouseWriter.stop`, the three strategies' `stop`,
`ChunkFlusher.putDirect`, `Utils.handleException`) and ran both new test classes once
(`testUnit`, Docker unreachable): 114 tests, 0 failures, class time 21.9 s.

Results by the requested checks:

1. **`stop()` change.** `git diff` for `src/main` touches one file and only the body of `stop()` (+8/-5). `close()`,
   `ProxySinkTask`, the strategies and the writer are untouched. `try/finally` covers `Throwable`, so an `Error` from the
   strategy also reaches the proxy stop, and a throw from the proxy stop replaces the strategy's exception (recorded
   limit). I found no behavioural defect in the code change. The interrupt flag is not handled in `stop()` and was not
   before; the swallow is one level down and pre-existing (IN-04).
2. **Pin non-vacuity.** `strategyStopFailureStillStopsTheProxyTask` and `bothStopsRunWhenTheStrategyAndTheProxyTaskThrow`
   were red on the unchanged tree (E-02); order-swap and swallow mutations of the production code would fail
   `writerStopsSeen` and `assertSame`. The RTY pin has a measured mutation (E-05). Two assertions were never seen red
   (IN-01). No Mockito in either class; no record value reaches the output (checked the 36 MB XML report for
   `SinkRecord{`, `Struct{`, `value=`: none; log lines carry topic, partition and offsets only).
3. **Claims.** The numbers in item 17 match E-05 and E-06 (22 shapes, 16 retriable shapes, 500, 64/44, the instants and
   gaps). The CHANGELOG gg8 section claims no more than E-02..E-04 show, with the one wording problem in WR-02, and the
   gg7 section is byte-identical (the diff is a pure 22-line prepend). Five findings below: three on claims, one on
   test cost, one stale cross-reference.

No blocker found.

## Warnings

### WR-01: FORK-DELTA patch-table row for `ClickHouseWriter` still names the task's `stop()` as an open residual

**File:** `docs/FORK-DELTA.md:39`
**Issue:** The row says three residuals stay open "among the five that Known cost 13 lists (the other two belong to the
Keeper state provider and to the task's `stop()`)". This phase closed the task's `stop()` residual in item 13, and the E-11
hand-off inventory does not list this row. After the edit item 13 lists the V1 Keeper constructor, the
helper-client constructor, the second `start()`, the 5 s wait and the new `close(partitions)` residual; the "other
two" are now the Keeper constructor and `close(partitions)`, and the `stop()` part now contradicts item 13 and the CHANGELOG. The phase gate's `fork_delta_final_checks` (E-10) only greps for FUT-05 strings, so it did not catch this.
**Fix:** Replace the parenthesis with "(the other two belong to the Keeper state provider and to the task's
`close(partitions)`)" and re-run a grep for `task's .stop()` over `docs/FORK-DELTA.md`.

### WR-02: CHANGELOG says the strategy's exception "reaches Kafka Connect" without saying Connect only logs it

**File:** `CHANGELOG.md:5-8, 19-21`
**Issue:** The pin proves the exception reaches the caller of `ClickHouseSinkTask.stop()` (`assertSame`, line 220 of the
test). What Connect does with it is not pinned and not measured; the research read it in the 4.x sources: `WorkerSinkTask.close()`
does `try { task.stop(); } catch (Throwable t) { log.warn("Could not stop task", t); }`
(47-RESEARCH.md line 125). So an operator reading "its exception is the one that reaches Kafka Connect" can take it as a task
failure; in fact it is one WARN line, and in the both-throw case the strategy's exception is in no log at all (the Known
Issues bullet states the drop but not that nothing logs it). The first bullet therefore claims a visible outcome the pins and
evidence do not show, and the second understates its cost.
**Fix:** In the Bug Fixes bullet write "its exception is the one `stop()` throws; Kafka Connect logs it as a WARN and does
not fail the task". In the Known Issues bullet add "the dropped exception is not logged anywhere". Optionally add a
`LOGGER.warn` of the strategy failure in a catch before the finally, which removes the loss without changing the throw.

### WR-03: Item 17 states the redelivery interval as a general fact measured on one Connect line

**File:** `docs/FORK-DELTA.md:250`
**Issue:** "The interval between redeliveries is the worker's `offset.flush.interval.ms` (60 s by default; 7004, 7004 and
7006 ms apart with 7000)" follows the sentence that the sources of 3.9.1, 4.1.1 and 4.4.0 were read. What E-06 compared
across the three versions is only the body of `deliverMessages()` (`deliver_messages_identical: yes`). The cadence comes from
`iteration()` and `poll()` (`nextCommit`, the paused poll), which E-06 did not compare for 4.1.1 or 4.4.0; the measurements are
on `7.9.7-ccs` (Kafka 3.9) only. The connector compiles against connect-api 4.4.0 (`ClickHouseSinkTask.java:171`). "No count
and no deadline" is likewise measured for three calls over 120 s and otherwise rests on the source reading. The wording
should not run past that.
**Fix:** State "measured on a Connect worker of the Kafka 3.9 line; the `deliverMessages` body is identical in 4.1.1 and
4.4.0; the cadence code (`iteration`, `poll`) was not compared in the later two". Or compare `iteration()` in the three
source trees and record it as an E-entry.

### WR-04: The RTY-01 pin class takes 22 s and writes a 36 MB report, because it logs about 180,000 lines

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java:68, 312-342`
**Issue:** The 64 redelivery rows each make 500 calls. `src/test/resources/log4j.properties` sets
`com.clickhouse.kafka=DEBUG` with a `%C` (caller class, a stack walk per event) console pattern. My run of the class gave
`TEST-...RetriableRedeliveryLimitPinUnitTest.xml` of 36,151,467 bytes with 179,491 lines (about 32,000 `doLogic` lines, 32,000
`BEFORE_PROCESSING` lines, 18,000 `ClickHouse server error code` lines, 6,000 `Error code ... wasn't in the acceptable list`
lines, plus the `Deciding how to handle exception` and `... wrapping exception` WARNs), and 21.9 s of class time. E-05 recorded
"the cause of the longer total was not investigated"; this is the cause. It adds about 22 s to the testUnit run and about half of the 73 MB `build/test-results/testUnit` directory to every CI run that uploads reports. The constant 500
is arbitrary: the largest legitimate budget (`retryCount` is bounded to 3..10) is caught by far fewer than 500 calls. The
test name `retriableShapeIsRedeliveredWithoutBound` also says more than a finite 500 shows (the comment on line 67 is the
accurate statement).
**Fix:** Lower `REDELIVERIES` to about 50 (a budget of 5 still fails at redelivery 6; E-05's mutation stays valid) and/or raise
the level of `com.clickhouse.kafka.connect.sink.processing` and `util.Utils` for the class. Rename the method to
`retriableShapeIsStillRedeliveredAfterManyAttempts` or keep the name and cite the constant in its Javadoc. Re-run the E-05
mutation after changing the constant.

### WR-05: The first-put outcomes are pinned only through `ProxySinkTask.put`, and the item 17 wording reads as the connector's end-to-end behaviour

**File:** `docs/FORK-DELTA.md:250`; `src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java:240-287`
**Issue:** The connector's real entry point is `ClickHouseSinkTask.put` -> `DirectDeliveryStrategy.put` ->
`ChunkFlusher.putDirect` (`ChunkFlusher.java:63-82`), which catches every exception from `ProxySinkTask.put` and runs
`Utils.handleException` a second time, then sends the whole batch to the DLQ when `errors.tolerance=all`. Reading the code, a
conversion `DataException` (shape `CONVERSION_DATA_EXCEPTION_CONTROL`, pinned as `ESCAPES_RAW` under `all`) does not "escape
put with no DLQ record" through the Direct path: under `all` `putDirect` ignores it and writes the batch to the DLQ, and under `none`
it rethrows the raw `DataException`. The phase's new sentence "Any other exception from `put`, a `DataException` included, fails
the task even under `errors.tolerance=all`, with no DLQ record (measured)" is true of a task that throws from `put` (the c2 probe, which
is not this connector) and is placed in the paragraph about the connector. The first-put matrix covers only `PROXY_PUT`; only the
retriable rows run both paths. The scoping words "Through `ProxySinkTask.put`" exist earlier in the item but not next to the new
claim, and a reader of the table in E-05 can take `ESCAPES_RAW` and `FAILS_TASK` as what Connect sees.
**Fix:** Qualify the sentence: "A task that throws anything else from `put` fails; the connector's own Direct path
(`ChunkFlusher.putDirect`) throws only a `RetriableException` under `all`, and sends everything else to the DLQ". Add the
first-put matrix through `ChunkFlusher.putDirect` (the non-retriable shapes are the interesting rows), or state in the item
that those rows are not pinned.

## Info

### IN-01: Two assertions of `assertProxyStoppedOnce` were never seen red, and the MBean check has no precondition

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java:163-167`
**Issue:** `assertProxyStoppedOnce` checks the writer count first. On the unchanged tree it failed there (E-02,
`expected: <1> but was: <0>`), so `CLOSE_COUNT` and `MBEAN_GONE` were never evaluated red. `MBEAN_GONE` also lacks a
`assertTrue(mBeanRegistered())` before `stop()`: the test builds its own second `SinkTaskStatistics` and relies on the two
objects computing the same name (`buildMBeanName(connectorName, id, version)`; they do today). If the naming ever diverged, the
assertion would pass vacuously.
**Fix:** Add `assertTrue(harness.mBeanRegistered())` in the `Harness` constructor after `proxy.start()` (or in each test before
`stop()`), and use `proxy`'s own statistics name if it can be exposed.

### IN-02: The stop pin reaches into private and `final` fields by reflection

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java:41-43, 146, 175-193`
**Issue:** `ProxySinkTask.stateProvider` is `private final` and is replaced through `Field.set`; the strategies' `buffer` and
`perPartitionBuffer` are read by name. This works on JDK 17 and 21 (the toolchain) but JEP 500 makes final-field mutation warn and
later fail on newer JDKs, and a rename of either buffer field surfaces as an `IllegalStateException` from a helper rather than a
named failure. The same technique already exists in `ProxySinkTaskStopUnitTest:100`, so this is consistent, not new.
**Fix:** If the next JDK bump hits it, give `ProxySinkTask` a package-private constructor taking the `StateProvider` (the
phase decided against a production constructor, so this is a later option). Optionally wrap the buffer reads in a helper that
names the field in its failure message.

### IN-03: Two small statements in the docs run past their evidence

**File:** `docs/FORK-DELTA.md:242`; `CHANGELOG.md:12-21`
**Issue:** (a) Item 13 says `ClickHouseSinkTaskStopUnitTest` was "red on the unchanged tree and green after"; two of its six cases
were red (E-02), the other four were green before and after. (b) FORK-DELTA item 13 records the `close(partitions)` residual (a
throw from `proxySinkTask.onPartitionRemoved` skips `deliveryStrategy.close`), but the gg8 Known Issues bullet that lists the
open residuals of the closing does not, so the two documents give different lists.
**Fix:** (a) "red for the two cases that use a throwing strategy". (b) Add `close(partitions)` to the Known Issues bullet, or write in
the Phase 49 hand-off why it is left out.

### IN-04: `ClickHouseWriter.stop()` swallows an interrupt (pre-existing, in the stop path)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:205-209`
**Issue:** `catch (Exception e)` around `awaitTermination` catches `InterruptedException`, logs it and does not call
`Thread.currentThread().interrupt()`. With the new `finally` the rest of `stop()` still runs, which is right, but the interrupt
is lost for Connect's caller. This predates the phase and is outside the diff; recorded because the review asked about interrupt
handling in the stop path and `ClickHouseSinkTask.stop()` itself has none.
**Fix:** `catch (InterruptedException e) { Thread.currentThread().interrupt(); LOGGER.error(...); } catch (Exception e) {...}`,
as a separate change with its own pin.

---

_Reviewed: 2026-11-05_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
