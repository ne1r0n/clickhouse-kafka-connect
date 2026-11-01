# Merge Resolution Checklist — upstream tail 9bed6f79 (tagless mini-sync 2)

This document is the checked-in resolution record for merge 3 of the fork's sync lineage on branch `sync/v1.7.0`: the merge commit M3 = `adaf8c4e76375e5099c076f2ca3bace892709765` takes upstream's untagged tail `9bed6f7970670a55c66be8efda246889e1f95bd0` (upstream PRs #860 and #848's merges, 7 commits past `a6f1df839c079180bc7c343b5823f205e204dafc`, no tag) onto the Phase 33 sweep tip. The merge base is `a6f1df839c079180bc7c343b5823f205e204dafc` (the previous tail). The rollback tag is the local, never-pushed `pre-v1.7.0-tail-2` = `68f1b0cbfcb217fea1de769b48bc985dacdc40f3` (M3's first parent); `pre-v1.7.0_gg6` stays reserved for the release. `UPSTREAM_REF` is the full SHA above, never `upstream/main`. Per research correction X-3 the record lands in the commit after M3, because a new file inside the merge commit would fail the tail gate's provenance invariant. It sits beside `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` (Recorded Facts 42-43).

Three rules govern it:

1. Mechanical merge, per-hunk resolution: Section 1 is generated from `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` (the committed output of `git merge-tree --write-tree --name-only` over M3's two parents) and Section 2 from `git diff --name-status a6f1df839c079180bc7c343b5823f205e204dafc 9bed6f7970670a55c66be8efda246889e1f95bd0`. Neither is hand-transcribed.
2. No dependency bump rides inside M3 beyond what upstream's tail carries (it carries none), and no `.planning` commit happens between `git merge --no-commit` and M3.
3. Recorded Facts continue the global numbering at 44, because the tail-1 checklist ends at 43, so a citation "Recorded Fact N" is never ambiguous.

## Section 1 — Conflicted

| path | predicted class | actual class | hunks | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CHANGELOG.md | CONFLICT content | conflict | 1 | union of both sides: the fork's `1.7.0_gg5` ... `1.7.0_gg1` sections first, one blank line, then upstream's 10-line `# Unreleased` block; the `Unreleased` text is not rewritten (Phase 38 RLS-01 absorbs it) | 38 (RLS-01 absorbs the Unreleased text) | the fork's release notes and the gg section stack | tail gate invariants 1 and 2 (no authored line, no marker) | done |
| src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java | CONFLICT content | conflict | 2 | the fork (HEAD) side in both hunks: hunk 1 keeps the public `(ClickHouseSinkConfig, ErrorReporter, String connectorName)` constructor line, hunk 2 keeps the `new SinkTaskStatistics(connectorName, id)`, `new ClickHouseWriter(this.statistics)` and `new Processing(...)` lines; upstream's auto-merged five-argument `Utils.handleException(...)` call stays. The DBWriter seam is NOT added here; plan 34-02 re-expresses it | 34 (plan 34-02, TAL-02) | ORCH-02 `connector=<name>` JMX plumbing (`new SinkTaskStatistics(connectorName, id)`) | the ProxySinkTask anchor rows and `ProxySinkTaskConstructorUnitTest` (plan 34-02); tail gate invariants 1-3 | done |
| src/main/java/com/clickhouse/kafka/connect/util/Utils.java | CONFLICT content | conflict | 1 | imports only: upstream's `import java.util.Set;` first, then the five fork `org.*` import lines; the `DEFAULT_RETRIABLE_ERROR_CODES` set, the five-argument signature and lookup and the fork's null-message guard auto-merged outside the hunk | 34 (plan 34-02 adds the lookup anchor row) | the WRT-11 null-message guard in `handleException` | the existing `msg != null && (msg.indexOf(` anchor row, `UtilsNullMessageClassificationUnitTest`, tail gate invariants 1-3 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java | CONFLICT content | conflict | 1 | imports only: upstream's `import java.util.function.Supplier;`, then the two fork lines (`SinkRecord`, `Test`); upstream's blank line and duplicate static `assertEquals` import dropped (the fork already imports it at the top) | 34 | the fork-added `tableFilterTest` (raw SQL, not `CreateTableStatement`) | tail gate invariants 1 and 2; `compileTestJava` | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java | CONFLICT content | conflict | 1 | imports only; the fork side is empty: only upstream's `import java.util.function.Supplier;` plus its trailing blank line is kept, the other upstream lines duplicate imports the fork already has in its top block. The `Supplier` import cannot reach its sorted position from inside the hunk (it sits after the `org.slf4j` imports); a within-hunk position, recorded here | 34 | the fork's WRT-* writer tests in the same class | tail gate invariants 1 and 2; `compileTestJava` | done |

## Section 2 — Every path upstream changed

| path | upstream change | predicted class | actual class | fork-touched | review | owning phase | status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CHANGELOG.md | M | CONFLICT content | conflict | yes | resolved in Section 1 | 34 | done |
| src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseCloudTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/integrationTest/java/com/clickhouse/kafka/connect/sink/ExactlyOnceTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java | M | auto-merged | auto-merged | no | two upstream hunks (the `Collections` and `Set` imports, and the config's `additionalRetriableErrorCodes` passed to the five-argument `Utils.handleException`), taken as merged; Phase 35's BUF fix edits this file next | 34 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java | M | auto-merged | auto-merged | yes | high-risk file; upstream's `additionalRetriableErrorCodes` key, `parseErrorCodes` and `ErrorCodeListValidator` taken unedited as merged | 34 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java | M | CONFLICT content | conflict | yes | resolved in Section 1 | 34 | done |
| src/main/java/com/clickhouse/kafka/connect/util/Utils.java | M | CONFLICT content | conflict | yes | resolved in Section 1 | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java | A | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfigTest.java | A | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkJdbcPropertiesTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskBufferTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java | M | CONFLICT content | conflict | yes | resolved in Section 1 | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStringTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/ProtobufIngestTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java | M | CONFLICT content | conflict | yes | resolved in Section 1 | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java | M | auto-merged | auto-merged | yes | upstream-owned test, taken as merged | 34 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java | M | auto-merged | auto-merged | no | upstream-owned test, taken as merged | 34 | done |
| src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTableEngine.java | A | auto-merged | auto-merged | no | upstream-owned test fixture (`CreateTableStatement.create()` and the engine enum; old constructors kept as `@Deprecated`), taken as merged | 34 | done |
| src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/CreateTableStatement.java | M | auto-merged | auto-merged | no | upstream-owned test fixture (`CreateTableStatement.create()` and the engine enum; old constructors kept as `@Deprecated`), taken as merged | 34 | done |

## Section 3 — Breaks that merge without a marker

The research predicted three breaks that carry no conflict marker. Evidence entry E-04 measures them on the bare merge before any repair; each has its repair plan:

1. `AdditionalRetriableErrorCodesTest` needs the DBWriter seam constructor: 3 javac errors on `new ProxySinkTask(config, null, writer)` (plan 34-02, TAL-02).
2. `UtilsNullMessageClassificationUnitTest` calls the old four-argument `handleException`: 2 javac errors on one line (plan 34-02, TAL-03).
3. BLD-16 `ConnectApiReferencesUnitTest` needs `ConfigException(String,Object,String)` blessed in `src/test/resources/connect-api-references.txt`: one `testUnit` failure that no compile gate sees (plan 34-02, TAL-03).

`CreateTableStatement` needs no fork edit: upstream kept its old constructors as `@Deprecated`, and no fork file calls a `CreateTableStatement` constructor (E-04).

## Gate commands

Standing tail gate for Phases 35-37 (the unedited script; run it with each gate):

```bash
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 <range end>
```

Cumulative gate (default environment):

```bash
bash .planning/scripts/verify-merge-invariants.sh 54e63648 <range end>
```

Self-test (expected last line `selftest: no failures`):

```bash
bash .planning/scripts/verify-merge-invariants-selftest.sh
```

## Recorded Facts

44. **Merge-3 conflict list — `observed`, 2026-10-28, plan 34-01.** `git merge-tree --write-tree --name-only HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0` exited 1 and printed exactly the five paths of Section 1 (`CHANGELOG.md`, `ProxySinkTask.java`, `Utils.java`, `ClickHouseSinkTaskMappingTest.java`, `ClickHouseWriterTest.java`); `ClickHouseSinkConfig.java` and `ChunkFlusher.java` auto-merged. `git ls-remote upstream main` printed `9bed6f7970670a55c66be8efda246889e1f95bd0` at the pre-flight read and again immediately before the merge, and `git ls-remote --tags --refs upstream 'v*'` has no tag newer than `v1.7.0`.

45. **Merge-3 resolution — `observed`, 2026-10-28, plan 34-01.** Resolved as Section 1 records. M3 changes exactly the 27 upstream-changed paths and adds no `.planning` file; the unedited tail gate result is evidence entry E-03.

