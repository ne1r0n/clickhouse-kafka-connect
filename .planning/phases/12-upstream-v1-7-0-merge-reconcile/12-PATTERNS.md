# Phase 12: Upstream v1.7.0 Merge & Reconcile - Pattern Map

**Mapped:** 2026-10-07
**Files analyzed:** 18 new/modified (excluding the 5 mechanically merged conflict files, whose "pattern" is the hunk recipe in 12-RESEARCH.md)
**Analogs found:** 17 / 18

All analog paths below were verified with `git ls-files` (tracked source; no gitignored mirrors).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `.planning/scripts/verify-merge-invariants.sh` (modify: parameterise, provenance, anchors) | script | batch | itself (`.planning/scripts/verify-merge-invariants.sh`) | exact (extend in place) |
| `.planning/scripts/conflicted-paths-v1.7.0.txt` (new) | config/data | batch | `.planning/scripts/conflicted-paths.txt` | exact |
| `.planning/scripts/fork-anchors.txt` (new) | config/data | batch | `.planning/scripts/conflicted-paths.txt` (format mirror) | role-match |
| `.planning/MERGE-CHECKLIST-v1.7.0.md` (new) | doc | batch | `.planning/MERGE-CHECKLIST.md` | exact |
| `docs/FORK-DELTA.md` (modify rows 36, 49, 51, ~146-167) | doc | batch | itself | exact |
| `.planning/MERGE-CHECKLIST.md`, `.claude/CLAUDE.md`, `.planning/codebase/STACK.md` (wording "1.10.2") | doc | - | n/a, one-line edit | n/a |
| `src/main/.../sink/db/ClickHouseWriter.java` (WRT: scoped MAP guard) | service | transform (RowBinary) | itself, `case MAP:` (~line 612) | exact |
| `src/test/.../sink/db/NullOptionalMapBytesTest.java` (new, Docker-free) | test | transform | `src/test/.../sink/db/NestedNullableCompositeWriteTest.java` | exact |
| `src/test/.../sink/db/NestedNullableCompositeWriteTest.java` (add cases only) | test | transform | itself | exact |
| `src/main/.../sink/db/helper/ClickHouseHelperClient.java` (CMP: delete 2 dead constants + comment) | service | request-response | itself (lines 107-110) | exact |
| `src/test/.../sink/db/helper/NetworkCompressionPinUnitTest.java` (extend in place) | test | request-response (config assert) | itself | exact |
| `src/test/.../sink/db/helper/NetworkCompressionPinLiveTest.java` (extend in place) | test | request-response | itself | exact |
| `src/test/.../sink/NewUpstreamConfigDefaultsUnitTest.java` (add 14th-option method) | test | config | itself | exact |
| `src/test/.../sink/CompressionFlagLiveTest.java` (new, CMP-03) | test | CRUD (insert + read back) | `src/test/.../sink/AsyncInsertLiveTest.java` | exact |
| `src/test/.../sink/state/provider/KeeperStateProviderRestartLiveTest.java` (new, EOS-01) | test | CRUD (state write/read) | `AsyncInsertLiveTest.java` (harness) + RESEARCH skeleton | role-match (no existing KeeperStateProvider test) |
| `build.gradle.kts` (BLD: delete fork LZ4 block) | config | - | upstream group-based rule already merged in the same file | exact |
| `gradle/libs.versions.toml` (merge resolution) | config | - | n/a, hunk recipe | n/a |
| D-12 probe / EOS mutation (throwaway, never committed) | test | - | `NetworkCompressionPinLiveTest` | role-match |

## Pattern Assignments

### `.planning/scripts/verify-merge-invariants.sh` (script, batch)

**Analog:** itself. Keep the interface `<merge-sha> [range-end]`, convert hard-coded vars to env overrides, replace invariant 1 with provenance + anchors, keep invariant 2 unchanged.

**Variables to parameterise** (lines 11-14 today):
```bash
PRE_TAG="pre-v1.6.0-sync"
UPSTREAM_REF="v1.6.0"
CONFLICTED_PATHS_FILE="${ROOT_DIR}/.planning/scripts/conflicted-paths.txt"
```
Becomes `PRE_TAG="${PRE_TAG:-pre-v1.7.0-sync}"` etc., plus `ANCHORS_FILE`; skip anchor step when `ANCHORS_FILE` is empty (v1.6.0 re-check).

**Invariant 2 to keep verbatim** (runtime-assembled marker; fail-closed grep):
```bash
MARKER="$(printf '%.0s<' 1 2 3 4 5 6 7)"
SOURCE_PATHSPEC=(src build.gradle.kts settings.gradle.kts gradle .github VERSION .gitignore)
...
    grep_err="$(git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}" 2>&1)"
    grep_rc=$?
    case "${grep_rc}" in
      0) violation="${MARKER} marker found in commit ${c}" ;;
      1) continue ;;
      *) violation="search failed with exit status ${grep_rc} on commit ${c}: ${grep_err}" ;;
    esac
```

**New provenance block** (from 12-RESEARCH.md Pattern 1, validated against `673d29c3`). Gotcha: loop must read on fd 3 or `git show` eats stdin:
```bash
while IFS= read -r p <&3; do
  git cat-file -e "$M:$p" 2>/dev/null || continue
  { for r in "$P1" "$P2"; do git cat-file -e "$r:$p" 2>/dev/null && { git show "$r:$p"; echo; }; done; } > "$TMP/u"
  git show "$M:$p" > "$TMP/m"
  extra=$(grep -vxFf "$TMP/u" "$TMP/m" | wc -l | tr -d ' ')
  [ "$extra" != 0 ] && { echo "PROVENANCE VIOLATION: $extra authored line(s) in $p" >&2; viol=1; }
done 3< <( { git merge-tree --write-tree --name-only "$P1" "$P2" | awk 'NR==1{next} /^$/{exit} {print}'; \
             git diff --name-only "$P1" "$M"; } | sort -u )
```
**Anchors:** TAB-separated `path<TAB>fixed-string<TAB>expected-line-count<TAB>FORK-DELTA row`, counted with `grep -cF -e "$pattern" "$path"` (lines, not occurrences). Author after the merge commit, never before (Pitfall 8).

---

### `.planning/scripts/conflicted-paths-v1.7.0.txt` / `fork-anchors.txt` (data, batch)

**Analog:** `.planning/scripts/conflicted-paths.txt`: one repo-relative path per line, no header, generated from `git merge-tree --write-tree --name-only` (not hand-typed, Recorded Fact 5). Do not overwrite the v1.6.0 file. Initial anchor rows and measured counts: 12-RESEARCH.md "Pattern 2" table.

---

### `.planning/MERGE-CHECKLIST-v1.7.0.md` (doc)

**Analog:** `.planning/MERGE-CHECKLIST.md`

**Layout** (lines 1-9, 60s):
```markdown
## Section 1 — Conflicted
| path | conflict type | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status |
## Section 2 — Merged clean, READ ANYWAY
| path | why it must be read | owning phase | status |
## Recorded Facts
1. **Resolved ClickHouse client version (SYNC-04) — `repo-derived`.** ...
```
Rules to copy: status column `pending`/`done`; owner flips only own rows; each Recorded Fact states source (`repo-derived`, `operator-confirmed`, observed run). **Numbering continues at 17** (v1.6.0 ends at 16). Section 1 = 5 rows (VERSION, libs.versions.toml, ClickHouseSinkConfig, ClickHouseWriter, ClickHouseHelperClient); Section 2 = 22 upstream paths with verdicts from RESEARCH "Auto-merged hunks" table. Rows Phase 13 re-edits (writer hunks) stay `pending`.

---

### `src/main/.../sink/db/ClickHouseWriter.java` (WRT-06, service, transform)

**Analog:** itself, `case MAP:` (fork guard at ~612-622 pre-merge; upstream `mapTmp == null` block lands below it at merge). Change only the condition (new code, not in merge commit):
```java
case MAP:
    if (value.getObject() == null && col.isNullable()) {
        // existing CR-02 comment ... plus: scoped to Nullable; non-nullable null Map is #820's empty map
        BinaryStreamUtils.writeNull(stream);
        break;
    }
    Map<?, ?> mapTmp = (Map<?, ?>) value.getObject();
    if (mapTmp == null) {            // upstream #820 block, now reachable
        if (defaultsSupport) { BinaryStreamUtils.writeVarInt(stream, 0); }
        break;
    }
```
Also resolved `start()` chain (union): `.setTargetTableFilter(csc.getTargetTableFilter())` then `.setCompressionMethod(csc.isSetCompressionMethod())` then `.build()`.
Constraint: do not touch the `cleanupExtraFields` fork guard (P9-IN-01, Phase 13).

---

### `NullOptionalMapBytesTest.java` (new, test, transform)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java`

**Imports / class shape** (lines 1-26): same package `com.clickhouse.kafka.connect.sink.db` (so protected `doWriteCol` is callable), extends nothing (so it stays in `testUnit`; extending `ClickHouseBase` adds `@Tag("clickhouse")`).
```java
package com.clickhouse.kafka.connect.sink.db;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import org.apache.kafka.connect.data.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
```
**Core pattern** (lines 31-42):
```java
@Test
@DisplayName("CR-02: a null MAP value writes the RowBinary null marker ...")
void nullMapValueWritesNullMarker() throws Exception {
    ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
    Column column = Column.builder().name("nested_map").type(Type.MAP).isNullable(true).build();
    Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));
    assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
}
```
New cases swap to `Column...isNullable(false)` and call `writer.doWriteCol(nullMap, true, col, out, defaults)`; expect `{0x00}` (RowBinary) and `{0x00, 0x00}` (WithDefaults). Run RED on the merge SHA first (expect `00 01`), GREEN after the guard. Existing CR-02 pins (all `isNullable(true)`) must remain unmodified; WRT-09 = evidence that 0 assertions changed; each new case carries a comment citing the 2026-10-07 approval.

---

### `ClickHouseHelperClient.java` (CMP, D-17 leftovers)

**Analog:** itself. After the merge, lines ~107-110 (the comment `// ClickHouse 26.9 defaults network_compression_method to ZSTD ...` and constants `NETWORK_COMPRESSION_METHOD_SETTING` / `NETWORK_COMPRESSION_METHOD_LZ4`) are unused; delete them. Upstream's public replacements are `NETWORK_COMPRESSION_METHOD` and `LZ4`. Update `fork-anchors.txt` (`NETWORK_COMPRESSION_METHOD_SETTING` 2 -> 0) in the same commit.

Merge hunk recipe (builder setters, union, each with own return):
```java
public ClickHouseClientBuilder setTargetTableFilter(String targetTableFilter) {
    this.targetTableFilter = targetTableFilter;
    return this;
}
public ClickHouseClientBuilder setCompressionMethod(boolean setCompressionMethod) {
    this.setCompressionMethod = setCompressionMethod;
    return this;
}
```
V1 options and V2 chain hunks resolve to upstream (`if (setCompressionMethod) {...}` + `options.put(ClickHouseClientOption.COMPRESS, false);` and `.compressServerResponse(false).setDefaultDatabase(this.database);`).

---

### `NetworkCompressionPinUnitTest.java` (extend in place, CMP-02)

**Analog:** itself (`src/test/.../sink/db/helper/NetworkCompressionPinUnitTest.java`). Keep all 4 cases and `buildClient`; add beside them.

**Helper and constants** (lines 20-29):
```java
private static final String SETTING = "network_compression_method";
private static final String V2_SETTING_KEY = "clickhouse_setting_" + SETTING;
private static ClickHouseHelperClient buildClient(String jdbcConnectionProperties) {
    return new ClickHouseHelperClient.ClickHouseClientBuilder("localhost", 1, null, null, -1)
            .setJdbcConnectionProperties(jdbcConnectionProperties)
            .useClientV2(true)
            .build();
}
```
**Assertion style** (lines 31-37, try-with-resources on the client):
```java
try (ClickHouseHelperClient chc = buildClient("")) {
    assertEquals("lz4", chc.getClient().getConfiguration().get(V2_SETTING_KEY));
}
```
New assertions: V1 `chc.getDefaultClientOptions().get(ClickHouseClientOption.COMPRESS)` equals `false`; V1 node `chc.getServer().getOptions().get("compress")` equals `"false"`; V2 `getConfiguration().get("compress")` equals `"false"` (value string is assumed A2, confirm on first run); with `.setCompressionMethod(false)` the lz4 key is absent (V1 `CUSTOM_SETTINGS`, V1 node, V2 `V2_SETTING_KEY`) while `compress` stays false. Optional: `?compress=true` operator override pin. Class Javadoc rewrite is Phase 13 (DEBT-05); do not touch.

---

### `NewUpstreamConfigDefaultsUnitTest.java` (add method)

**Analog:** itself (imports lines 3-11: `assertEquals/assertFalse/assertNotNull/assertDoesNotThrow`, `HashMap`, `Map`, `@DisplayName`, `@Test`). Add a separate method; do not edit the "thirteen" test or the existing class Javadoc. Assert: default `isSetCompressionMethod()` true == `ClickHouseSinkConfig.setCompressionMethodDefault`, canonical key `"setCompressionMethod"`, alias `"set_compression_method"`, `ConfigDef` default true. Docker-free, extends nothing.

---

### `CompressionFlagLiveTest.java` (new, CMP-03)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java`

**Imports** (lines 3-16) and class (line 27 `extends ClickHouseBase`):
```java
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import org.apache.kafka.connect.sink.SinkRecord;
```
**Core pattern** (lines 37-79):
```java
String tableName = createTopicName("async03_insert");
Map<String, String> props = getBaseProps();
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
createTable(chc, tableName, "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");
props.put("name", uniqueConnectorName(testInfo));
try {
    ...records.add(new SinkRecord(tableName, 0, null, null, null, value, i));
    ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
    sinkTask.start(props);
    try { sinkTask.put(records); } finally { sinkTask.stop(); }
    assertEquals(5, ClickHouseTestHelpers.countRows(chc, tableName));
    // read back rows ORDER BY id; assertEquals per row value (use the file's allRows helper:
    // snapshot values inside the iterator, GenericRecord is a reused view, lines 81-88)
} finally {
    ClickHouseTestHelpers.dropTable(chc, tableName);
}
```
Parameterise on `setCompressionMethod` (true/false) via `props.put("setCompressionMethod", ...)` and on `client_version` (V2 required; add V1 because the writer's `chcTmp` builder is only reached on V1 insert paths, RESEARCH Pitfall 1). If the `false` case fails: blocking checkpoint (D-13), do not record and continue.

---

### `KeeperStateProviderRestartLiveTest.java` (new, EOS-01)

**Analog:** no existing KeeperStateProvider test in `src/test`; use `ClickHouseBase` harness (as `AsyncInsertLiveTest`: `getBaseProps()`, shared container with `clickhouse_keeper_map.xml`) plus the skeleton in 12-RESEARCH.md "EOS-01 restart-read test skeleton".

**Source API** (`KeeperStateProvider.java` lines 33-60): constructor `KeeperStateProvider(ClickHouseSinkConfig csc)` builds its own client from config and calls `init()`; `stateMap` is per instance, so a second provider can only learn state from ClickHouse.
```java
props.put(ClickHouseSinkConfig.ZK_PATH, "/" + id);      // must start with "/"
props.put(ClickHouseSinkConfig.ZK_DATABASE, id);        // KeeperMap table name; unique per test
props.put("setCompressionMethod", String.valueOf(flag));
writer.setStateRecord(new StateRecord(topic, 0, 19, 10, State.AFTER_PROCESSING, topic)); // max BEFORE min
StateRecord read = new KeeperStateProvider(new ClickHouseSinkConfig(props)).getStateRecord(topic, 0);
```
Assert exact `(minOffset, maxOffset, state)`; fresh key (`partition 1`) -> `State.NONE`. Mutation check (D-15) is a temporary working-tree revert of `getStateRecord` to `response.getSummary().getResultRows()`, never committed; afterwards `git diff --quiet v1.7.0 HEAD -- src/main/.../KeeperStateProvider.java` must pass. If it stays green: D-16 time-boxed hunt, else blocking checkpoint.

---

### `build.gradle.kts` (BLD, config)

**Analog:** upstream's own group-based rule in the same merged file (`configurations.all { ... withCapability("org.lz4:lz4-java") ... select(fork) }`, ~lines 64-74). Delete the fork's `// Temporary fix` `configurations.configureEach { ... select("at.yawk.lz4:lz4-java:1.10.2") }` block (pre-merge `:134-138`, ~`:153-159` post-merge). Proof: `grep -c 'withCapability("org.lz4:lz4-java")' build.gradle.kts` = 1 and `dependencyInsight --dependency lz4-java` on `runtimeClasspath` and `clickhouseDependencies` yields one distinct `at.yawk.lz4:lz4-java` version (expect 1.10.4); see RESEARCH Pitfall 5 for the grep shape. Update the matching anchor row in the same commit. Do not run repo-wide `spotlessApply` (baseline red on 44 files).

---

## Shared Patterns

### Docker-free vs live test split
**Source:** `NestedNullableCompositeWriteTest.java:15-26`, `NetworkCompressionPinUnitTest.java:9-19`
**Apply to:** all new/extended tests. Docker-free: extend nothing (so no `@Tag("clickhouse")`), include in `testUnit`. Live: `extends ClickHouseBase`, use `getBaseProps()`, `uniqueConnectorName(testInfo)`, `createTopicName(...)`, read back and `assertEquals` against original values. Never delete or weaken existing cases.

### Commit-per-concern with anchors
**Source:** CONTEXT D-04/D-08. Any commit that changes an anchor edits `.planning/scripts/fork-anchors.txt` in the same commit. WRT, CMP, BLD run sequentially (shared anchors file); EOS touches only a new file and may parallelise. No `.planning/` commit interleaved with conflict resolution.

### Gradle invocation
`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Merge-commit gate: `compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` plus `testUnit`.

### Mechanical merge rule
Merge commit contains only lines from `pre-v1.7.0-sync` or `v1.7.0`; `VERSION` takes the fork side `v1.6.0_gg1`; D-17 hunks (V1 options, V2 chain) take upstream. Merge with the full ref `refs/tags/v1.7.0` (stale `refs/upstream/*` exist).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `KeeperStateProviderRestartLiveTest.java` | test | CRUD | No existing direct `KeeperStateProvider` test; compose from `ClickHouseBase` harness + RESEARCH skeleton |

## Metadata

**Analog search scope:** `.planning/scripts`, `.planning/MERGE-CHECKLIST.md`, `src/test/java/.../sink`, `.../sink/db`, `.../sink/db/helper`, `KeeperStateProvider.java`
**Files read:** 8 (plus CONTEXT.md and RESEARCH.md in full)
**Pattern extraction date:** 2026-10-07
