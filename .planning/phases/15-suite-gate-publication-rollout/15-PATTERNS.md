# Phase 15: Suite Gate, Publication & Rollout - Pattern Map

**Mapped:** 2026-10-10
**Files analyzed:** 17 (2 main, 3 test, 8 docs/config/planning edits, 4 publication/gate procedures)
**Analogs found:** 17 / 17 (all analog paths verified git-tracked; no `.gsd/capabilities` mirrors involved)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java` (D-01) | utility (serializer) | transform | same file, `DEBEZIUM_MICRO_TIMESTAMP` case (lines 47-48, 186-226) | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (D-02) | service (metadata discovery) | request-response + once-per-key logging | same file, `shouldSkipDescribeRow` (472) plus the describe loops (489-555, 560-624) | exact (in-file) |
| `src/test/.../sink/db/MetadataFreeJsonWriterTest.java` (D-01 flip) | test (unit) | transform | same file, line 305-314 | exact |
| `src/test/.../sink/MetadataFreeJsonTypeParityLiveTest.java` (D-01 new cell) | test (live, Docker) | CRUD + read-back | same file, `debeziumDateAndMicroTimestampReadBackAsLiterals` (561-598) | exact |
| `src/test/.../sink/db/helper/ClickHouseHelperClientForkUnitTest.java` (D-02 predicate test) | test (Docker-free unit) | transform | same file, `shouldSkipDescribeRow` tests (~100-125) | exact |
| `VERSION` | config | n/a | current file (`v1.6.0_gg1`, no extra content) | exact |
| `.planning/scripts/fork-anchors.txt` | config (gate data) | n/a | row `VERSION<TAB>v1.6.0_gg1<TAB>1<TAB>BLD-07` | exact |
| `CHANGELOG.md` (new top section) | docs | n/a | upstream `# 1.7.0, 2026-09-30` section (lines 1-21) | exact |
| `docs/FORK-DELTA.md` | docs | n/a | itself (stale-spot list in RESEARCH) | exact |
| `docs/ROLLOUT-v1.7.0_gg1.md` (new) | docs (operator checklist) | n/a | `13-BEHAVIOUR-LEDGER.md` prose; FORK-DELTA table style | role-match |
| `13-BEHAVIOUR-LEDGER.md` (add L-20, L-21, L-22) | docs (ledger) | n/a | rows L-17..L-19 | exact |
| `13-REVIEW-DISPOSITION.md`, `14-REVIEW-DISPOSITION.md` | docs (state) | n/a | themselves (YAML frontmatter plus table) | exact |
| `.planning/MERGE-CHECKLIST-v1.7.0.md` (flip 2 `pending` rows) | docs | n/a | other closed rows in same file | exact |
| Gate procedure: Run 1 / Run 2 / testUnit-with-Docker-down | procedure | batch | `14-03-PLAN.md` lines ~101-102 (XML sums); 11-04-SUMMARY lines ~175-178 (colima) | exact |
| Rehearsal push plus CI locator | procedure | request-response (REST) | Phase 11 plan 11-05; 14-04 | exact |
| Blocking-human approval checkpoint | plan task | n/a | `11-06-PLAN.md` task 1 (lines 71-110) | exact |
| Orchestrator-inline publication (merge, push, tags) | plan task | n/a | `11-06-PLAN.md` task 2 (lines 113-160) | exact |

## Pattern Assignments

### `MetadataFreeJsonWriter.java` (utility, transform) - D-01

**Analog:** same file, lines 47-48 (constants), 53-55 (scales), 186-226 (`writeLogicalValue`).

**Constants pattern** (lines 45-55):
```java
private static final String DEBEZIUM_DATE = "io.debezium.time.Date";
private static final String DEBEZIUM_MICRO_TIMESTAMP = "io.debezium.time.MicroTimestamp";

private static final long MILLIS_PER_DAY = 86_400_000L;
private static final int MILLIS_SCALE = 3;
private static final int MICROS_SCALE = 6;
```
Add `DEBEZIUM_TIMESTAMP = "io.debezium.time.Timestamp"`, `DEBEZIUM_NANO_TIMESTAMP = "io.debezium.time.NanoTimestamp"`, `NANOS_SCALE = 9` next to these (static string constants rule).

**Core pattern to copy** (lines 215-220):
```java
case DEBEZIUM_MICRO_TIMESTAMP:
    if (value instanceof Number micros) {
        gen.writeNumber(BigDecimal.valueOf(micros.longValue(), MICROS_SCALE));
        return true;
    }
    return false;
```
New cases are the same shape with `MILLIS_SCALE` and `NANOS_SCALE`. Server evidence: scientific notation (`0E-9`) is accepted.

**Javadoc to correct in the same commit** (lines 181-185): currently says "including {@code io.debezium.time.Timestamp} and ZonedTimestamp, returns false". Reduce to ZonedTimestamp only, and update the class Javadoc list of encodings. Do not put anchor strings from `fork-anchors.txt` into new comments.

---

### `ClickHouseHelperClient.java` (service, discovery) - D-02

**Analog:** same file. Logger is already declared (line 50): `private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseHelperClient.class);`

**Pure-predicate placement**: package-private `static` next to `shouldSkipDescribeRow` (line 472), same style (static, no instance state), so it is unit-testable Docker-free.

**Hook site** (both loops, identical shape; V1 lines 533-537, V2 602-606):
```java
if (shouldSkipDescribeRow(fieldDescriptor, columnsByName)) {
    LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
    skippedCols.add(fieldDescriptor.getName());
    continue;
}
// <-- insert one shared private call here, before Column.extractColumn (line ~544):
// warnOnceIfNullableEnumOrTuple(database, tableName, fieldDescriptor);   // root rows only
Column column = Column.extractColumn(fieldDescriptor);
```
Root-row filter reuses the idiom already in the loop (line 515): `if (!fieldDescriptor.isSubcolumn())`. Use SLF4J `{}` parameters, never string concatenation (existing line 624 style `LOGGER.error("describeTableV2 failed", e)`). The loops use `LOGGER.debug("... {}", ...)`. WARN is a one-shot at table level, never per record.

**Dedup mechanism (per RESEARCH A8)**: process-wide `ConcurrentHashMap.newKeySet()` keyed `database.table.column:rawType`, with a package-private reset for tests. Wording per D-15: values in the slot may be stored as NULL or shifted, null entries fail the batch with `DataException`, see backlog 999.1.

**Anchors at risk** (fixed-string counts in `fork-anchors.txt`): `buildTableFilterClause` 4, `shouldSkipDescribeRow` 3, `StandardCharsets.UTF_8` 1, `InputStreamReader` 2, `.compressServerResponse(false)` 1. A new call or comment that mentions `shouldSkipDescribeRow` changes its count; update the anchors file in the same commit.

---

### `ClickHouseHelperClientForkUnitTest.java` (test, Docker-free) - D-02

**Analog:** same file. Imports (lines 3-16):
```java
import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
...
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```
Class extends nothing (extending `ClickHouseBase` would add `@Tag("clickhouse")` and drop it from `testUnit`, D-16 in the class Javadoc).

**Test shape** (lines 111-121):
```java
@ParameterizedTest(name = "DESC-02: {0} subcolumn of an Array parent is skipped")
@ValueSource(strings = {".size0", ".size"})
@DisplayName("DESC-02: array-length subcolumn suffixes ... are skipped")
void arrayLengthSuffixesAreSkipped(String suffix) {
    ...
    boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
            newDescriptor("array_col" + suffix, "UInt64"), columns);
```
Use `@ParameterizedTest` + `@ValueSource` for the true set (`Array(Nullable(Enum8('x' = 1)))`, `Map(String, Nullable(Enum16('y' = 1)))`, `Nullable(Tuple(a Int32))`, `Nullable(Enum8('z' = 1))`) and the false set (`Array(Nullable(Int32))`, `Enum8('a' = 1)`, `Tuple(a Nullable(Int32))`), plus a null input. Add a test for the dedup (second call for the same key does not re-warn) via the reset hook. Use `DisplayName` prefix convention (e.g. `D-02:`), and `newDescriptor(name, type)` for descriptors.

---

### `MetadataFreeJsonWriterTest.java` (test, unit) - D-01 flip

**Analog:** same file, lines 304-314 (the pin to flip):
```java
@Test
@DisplayName("D-18: MicroTimestamp is decimal seconds with six fractional digits; Timestamp and ZonedTimestamp pass through")
void cdcMicroTimestampConvertsAndOtherDebeziumTimesPassThrough() throws Exception {
    Map<String, Data> values = new LinkedHashMap<>();
    values.put("micro", new Data(SchemaBuilder.int64().name(DEBEZIUM_MICRO_TIMESTAMP).build(), 1_704_067_200_123_456L));
    values.put("millis", new Data(SchemaBuilder.int64().name(DEBEZIUM_TIMESTAMP).build(), 1_704_067_200_123L));
    values.put("zoned", new Data(SchemaBuilder.string().name(DEBEZIUM_ZONED_TIMESTAMP).build(), "2024-01-01T00:00:00Z"));
    assertEquals(
            "{\"micro\":1704067200.123456,\"millis\":1704067200123,\"zoned\":\"2024-01-01T00:00:00Z\"}",
            latin1(MetadataFreeJsonWriter.cdcRow(values)));
}
```
Constants `DEBEZIUM_MICRO_TIMESTAMP` (line 45) and `DEBEZIUM_TIMESTAMP` exist in the test; add `DEBEZIUM_NANO_TIMESTAMP`. Flip `"millis":1704067200123` to `1704067200.123`, add a nano cell (`...123456789L` to `1704067200.123456789`), keep zoned pass-through. Split/rename the method (name no longer says "OtherDebeziumTimesPassThrough"; keep `ZonedTimestamp` pass-through pinned). Put the tightening rationale in a comment next to each changed assertion (WRT-09 precedent). Also check the pre-1970 and zero-value forms (`0E-9` per RESEARCH A7) with a unit byte pin. Seen-RED mutation: revert the new case, confirm the test fails, record in the SUMMARY.

---

### `MetadataFreeJsonTypeParityLiveTest.java` (test, live) - D-01 new cell

**Analog:** same file, `debeziumDateAndMicroTimestampReadBackAsLiterals` (lines 561-598).

**Structure to copy** (lines 561-598): topic name via `createTopicName("...")`, `ClickHouseTestHelpers.createClient(getBaseProps())`, `dropTable` before and in `finally`, `createTable(chc, topic, "CREATE TABLE %s (... ) ENGINE = MergeTree ORDER BY id")`, build `rowSchema` with `SchemaBuilder.int64().name("io.debezium.time.MicroTimestamp").build()`, `cdcEnvelopeSchema(rowSchema)`, `cdcCreateEnvelope(...)`, then:
```java
writeThroughJsonRoute(
        testInfo, JSON_SUFFIX, topic, topic,
        sinkRecords(topic, envelopeSchema, List.of(cdcCreateEnvelope(envelopeSchema, after, 100L))),
        Map.of(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true"));
List<Map<String, String>> rows = selectRows(chc, new Projection().strings("d", "mts"), topic);
assertEquals("2024-01-01 00:00:00.123456", rows.get(0).get("mts_s"));
```
For D-01 use `DateTime64(3,'UTC')` for `io.debezium.time.Timestamp` and `DateTime64(9,'UTC')` for `NanoTimestamp`, with typed read-back. If RowBinary parity is wanted, use `writeThroughBothRoutes`/`assertRoutesAgree` (lines 226, 291) but only for matching precision cells (RESEARCH: RowBinary is correct only for ms into `DateTime64(3)` and ns into `DateTime64(9)`). Existing tests in this class are `@Test` + `@DisplayName` with `TestInfo testInfo`.

---

### `VERSION` and `fork-anchors.txt` (config)

`VERSION` contains exactly `v1.6.0_gg1` with a trailing newline; keep the newline. The anchors file row is tab-separated: `VERSION	v1.6.0_gg1	1	BLD-07`. Change both in the same commit (the invariant script evaluates the anchors at each commit in `pre-v1.7.0-sync..HEAD`). `Version.java` is generated by `generateVersionFile` (`build.gradle.kts` ~417-437); `clean` is mandatory before asserting `ARTIFACT_VERSION = "v1.7.0_gg1"`.

---

### `CHANGELOG.md` (docs)

**Analog:** top of the file (lines 1-21), upstream style:
```markdown
# 1.7.0, 2026-09-30

## Important Changes

* Fixed issue with server response compression when using ClickHouse 26.9+. ...
  (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/841)

## Bug Fixes

* Fixed NPE on null optional Map fields; ... (https://github.com/.../issues/820)

## Confluent Archive
...
## Dependencies

* Bumped `com.clickhouse:*` from `0.9.5` to `0.9.6`.
```
New section `# 1.7.0_gg1, <publish date>` goes above it with `## Important Changes` / `## Bug Fixes` / `## Dependencies`, bullets with `*` and two-space continuation lines. Content source: `13-BEHAVIOUR-LEDGER.md` lines 83-110 (CHANGELOG list: L-01, L-02, L-05, L-06, L-09, L-10, L-15, L-16, L-17, L-18) plus L-20, L-21, L-22. Link `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md`. Name 13-REVIEW WR-02 in full to avoid the naming collision with L-10 ("WR-02 strictness"). Leave upstream sections untouched.

---

### `docs/FORK-DELTA.md` (docs)

**Analog:** itself. Table row style (line 33-38): 4 columns `| behaviour | file | rationale | requirement IDs; test classes |`. Use the RESEARCH "FORK-DELTA stale spots" table (lines 308-326) as the work list: intro, line 31 (`v1.6.0_gg1`), 33 (metadata-free row; "five guard sites", R-01 mention; anchor `shouldSkipTableMetadata()` is 8), 35 (add INT256/UINT256), 38 ("any depth"), 47 (CI), 48 (VERSION), 51 (deps), 77-78 retired rows, 84-110 raw numbers (re-measure), 112-137 Known costs, 139-163 skip inventory, 165-173 next syncer. Every claim grep-confirmed against the tree. Known costs gets the D-15 wording for Nullable Enum/Tuple (values may be stored as NULL or shifted; null entries throw) pointing at 999.1.

---

### `docs/ROLLOUT-v1.7.0_gg1.md` (docs, new)

**Analog:** no rollout doc exists. Prose sources: `13-BEHAVIOUR-LEDGER.md` "R-01 backfill procedure" (lines 35-46, copy as written), "Nested-null semantics by route" (48), "TYPE-08 message change" (69), "Known JSON-route gaps" (73), "Phase 15 inputs by destination" (83-). Per item structure: what changed, how to tell if affected (a concrete self-check), what to do. Self-check SQL is in RESEARCH Code Examples 3 (KeeperMap state: `SELECT key, minOffset, maxOffset, state FROM connect_state WHERE state != 'AFTER_PROCESSING'`) and 4 (`system.columns` with `LIKE '%Nullable(Enum%' OR ... '%Nullable(Tuple%'`; `system.settings_profile_elements` for `network_compression_method`). Required vs precautionary exactly-once check: use RESEARCH route matrix (required only for explicit `insertFormat=json` + `bypassRowBinary=true` + Struct/CDC, per D-16). No hostnames, connector names or prod versions. Run a secret/hostname scan over `docs/` and the diff before the rehearsal push.

---

### Ledger, dispositions, checklist (docs)

**Ledger row shape** (13-BEHAVIOUR-LEDGER.md lines 31-33, columns: ID | plan | description | decision | pinning tests | observable? | destination):
```
| L-19 | 13-07 | **DEBT-04, DEBT-05.** ... | D-14 | `ClickHouseBaseTearDownAttributionUnitTest#...` | no (test harness) | FORK-DELTA (...) |
```
Add L-20 (Timestamp/NanoTimestamp), L-21 (Nullable Enum/Tuple WARN, D-15 wording), L-22 (explicit json + bypassRowBinary route change; state honestly whether a value pin exists, `MetadataFreeJsonInsertUnitTest` pins routing only). Extend the "Phase 15 inputs by destination" lists.

**Disposition files**: YAML frontmatter entries (`- id: WR-01` / `severity` / `disposition: open` / `title`) plus the `open:`/`total:` counts plus the Markdown table; edit frontmatter and table together, set deferred reason in the Source cell. 13 WR-02 becomes `fixed`; WR-05 `deferred` with "WARN added (D-02); root cause backlog 999.1".

**MERGE-CHECKLIST-v1.7.0.md**: flip the two `pending` rows (lines 72, 75: ClickHouseWriter hunks `@@ -556,0 +558,6 @@` and `@@ -1323 +1330,6 @@`) with evidence; Rule 2 requires zero pending before publication.

---

### Gate runs (procedure)

**Sums** (`14-03-PLAN.md` ~101-102, reproduced in RESEARCH Code Examples 5):
```bash
python3 -I -c "import glob,xml.etree.ElementTree as E;R=[E.parse(p).getroot() for p in glob.glob('build/test-results/test/TEST-*.xml')];S=lambda k:sum(int(r.get(k)) for r in R);print('classes',len(R),'tests',S('tests'),'failures',S('failures'),'errors',S('errors'),'skipped',S('skipped'));raise SystemExit(0 if R and S('failures')==0 and S('errors')==0 and S('skipped')==18 else 1)"
python3 -I -c "import glob,xml.etree.ElementTree as E;print('\n'.join(sorted(c.get('classname').rsplit('.',1)[-1]+'#'+c.get('name') for p in glob.glob('build/test-results/test/TEST-*.xml') for c in E.parse(p).getroot().iter('testcase') if c.find('skipped') is not None)))"
```
Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean test`, then copy `build/test-results/test` aside before `./gradlew test --rerun`. Record the server build label after each run. Docker-down: `colima stop --profile vz`, `docker info` must fail, `testUnit --rerun`, `colima start --profile vz` (docker CLI at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`). Finish with `.planning/scripts/verify-merge-invariants.sh 54e6364858cd728e3427ce2058fc00380895c689 HEAD`, record exit code. Assert tree equivalence `git diff --name-only <gated-HEAD> X -- . ':!.planning'` empty.

### Approval checkpoint and publication (plan shape)

**Analog:** `.planning/milestones/v1.6.0_gg1-sync-phases/11-full-suite-ci-publication/11-06-PLAN.md`.
- Task 1 (lines 71-110): `<task type="checkpoint:decision" gate="blocking-human">` with `<decision>`, `<context>` (numbered evidence bundle, "none of it cited from a planning document"), `<options>` (`publish` / `hold` / `publish-with-...`), `<resume-signal>`.
- Task 2 (lines 113-160): `type="auto"` with `<precondition>` requiring recorded approval, `<reversibility rating="one-way">`, `<files>`, `<read_first>`, `<action>`: confirm ground truth (clean `git status`, `cat VERSION`), merge `--no-ff`, pointer-style message, verify two parents and `git diff --quiet <merge> <rehearsed-sha>`, lightweight tag, push with no force. Task 3 (lines 161-192): CI wait and attribution on red.
- Differences for Phase 15 (from RESEARCH): merge the rehearsed SHA by hash, handle dirty `.planning/state.json` (worktree or stash), push branch alone then tags by explicit `refs/tags/<name>` refspecs, re-run the locator after the tag push, expect one extra run on `c61d90ce` for `pre-v1.7.0-sync`, delete `origin/sync/v1.7.0`, local tail stays unpushed (D-14). The orchestrator runs post-gate tasks inline because `gsd-executor` refuses relayed approvals.

**CI locator** (RESEARCH Code Examples 6): `curl .../actions/runs?head_sha=$M | jq -e --arg p ".github/workflows/tests.yaml" '[.workflow_runs[] | select(.path==$p)] ...'`. Check `/rate_limit` first, and treat a missing `workflow_runs` array as an error, not zero.

## Shared Patterns

### One behaviour per commit, anchors in the same commit
**Source:** `.planning/scripts/fork-anchors.txt` and `verify-merge-invariants.sh`
**Apply to:** every commit in the phase (D-01, D-02, VERSION, CHANGELOG, FORK-DELTA, rollout, dispositions).

### Seen-RED mutation and tightening rationale
**Source:** 13-02 precedent, WRT-09
**Apply to:** the D-01 test flip and the D-02 test: record the mutation evidence in SUMMARY, rationale comment next to changed assertions, never delete or weaken a pin.

### Logging and error text
**Source:** `ClickHouseHelperClient.java` lines 50, 520-529
**Apply to:** D-02 WARN: SLF4J `{}` parameters, metadata only (database, table, column, raw type), never record data.

### Docker-free vs live tests
**Source:** `ClickHouseHelperClientForkUnitTest` (extends nothing, stays in `testUnit`) vs `MetadataFreeJsonTypeParityLiveTest` (live, typed read-back with `selectRows`/`Projection`)
**Apply to:** D-02 uses the unit style; D-01 gets one unit pin plus one live cell.

### Gradle invocation and formatting
**Apply to:** all runs: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; `spotlessApply` only on touched files.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `docs/ROLLOUT-v1.7.0_gg1.md` | operator checklist | n/a | No prior per-release rollout doc; build from ledger prose and FORK-DELTA table style |
| Process-wide dedup set for the D-02 WARN | utility | once-per-key logging | No existing static warn-once registry in `src/main`; use `ConcurrentHashMap.newKeySet()` per RESEARCH (with a test reset hook) |
| Post-tag CI re-check (zero tag-triggered runs) | procedure | request-response | New under the branch-only `tests.yaml` trigger; Phase 11 only observed the old behaviour |

## Metadata

**Analog search scope:** `src/main/.../sink/db/`, `src/main/.../sink/db/helper/`, `src/test/.../sink/`, `src/test/.../sink/db/`, `.planning/scripts/`, `.planning/phases/13-*`, `.planning/phases/14-*`, `.planning/milestones/v1.6.0_gg1-sync-phases/11-*`, `docs/`, `CHANGELOG.md`
**Files scanned:** about 20 read or excerpted; tracked status confirmed via `git ls-files` for the main/test analogs and planning/doc analogs
**Pattern extraction date:** 2026-10-10
