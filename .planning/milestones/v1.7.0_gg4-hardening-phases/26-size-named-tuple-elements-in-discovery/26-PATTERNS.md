# Phase 26: Size-Named Tuple Elements in Discovery - Pattern Map

**Mapped:** 2026-10-21
**Files analyzed:** 17 new/modified (production 5, test 7, resources/build/docs 5)
**Analogs found:** 17 / 17 (all analogs verified git-tracked via `git ls-files`; none is a gitignored mirror)

Line numbers are from the tree at `37d2ef11`. The extraction commit moves them, so re-grep before editing.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/.../sink/db/helper/DescribeTableBuilder.java` (new) | service/utility (fork-owned) | transform (JSONL line to Column list) | `sink/db/mapping/DeclaredNullability.java` (shape) + the V2 loop in `ClickHouseHelperClient.describeTableV2` (body) | exact (shape) / exact (logic source) |
| `src/main/.../sink/db/mapping/ParentShape` (new, package-private, beside `DeclaredNullability`) | utility | transform | `DeclaredNullability.collectTuple` + `unwrapSimpleAggregate` | role-match |
| `src/main/.../sink/db/helper/ClickHouseHelperClient.java` (modify) | service | request-response | itself (`describeTableV1`/`V2` loops, `shouldSkipDescribeRow`) | exact |
| `src/main/.../sink/db/mapping/DeclaredNullability.java` (modify: `elementKey`) | utility | transform | itself (`collectTuple` lines 228-229) | exact |
| `src/main/.../sink/db/mapping/Column.java` (modify: `declaredElement`) | model | n/a | itself (`@Setter private Column arrayType;` line 66) | exact |
| `src/main/.../sink/db/mapping/Table.java` (modify, one line) | model | transform | itself (`updateParent` ARRAY branch, line 120) | exact |
| `src/test/.../sink/db/helper/DescribeFixtureCapture.java` (new, `main()`) | utility (harness) | batch / file-I/O | `src/test/.../ci/ConfluentArchiveCheck.java` + `ClickHouseBase.setupContainer` | role-match |
| `build.gradle.kts` (`captureDescribeFixtures` JavaExec) | config | batch | `verifyConfluentArchive` task (line 421) | exact |
| `src/test/.../sink/db/helper/DescribeFixtureCatalog.java` (new) | test fixture | n/a | `sink/db/mapping/DeclaredNullabilityFixtures` (static DDL/RAW constants) | role-match |
| `src/test/.../sink/db/helper/DescribeFixtureDriftLiveTest.java` (new) | test (live) | request-response | `NullableTupleMatrixLiveTest` COLS-13 `*_RAW` premise (line ~795-815) | exact |
| `src/test/.../sink/db/helper/DescribeTableBuilderUnitTest.java` (new) | test (Docker-free) | transform | `ClickHouseHelperClientForkUnitTest` (`discoverAsDescribeTableV2`, D-04 pin) | exact |
| `ClickHouseHelperClientForkUnitTest.java` (modify: rename and invert D-04 pin) | test | transform | itself (lines 624-660) | exact |
| `src/test/.../sink/TupleSizeNamedElementRoundTripLiveTest.java` (new) | test (live) | CRUD (put + typed read-back) | `NullableTupleMatrixLiveTest` + `NullableRoundTrip` | exact |
| `TableSizeNamedElementTest` (new, Docker-free, hand-built Columns) | test | transform | `ClickHouseHelperClientForkUnitTest` (no `ClickHouseBase`) | role-match |
| `src/test/resources/describe-fixtures/*.jsonl` + `SERVER_VERSION.txt` | resource | file-I/O | `src/test/resources/connect-api-references.txt` | role-match |
| `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `CHANGELOG.md`, `ROADMAP.md` backlog 999.4 | docs/config | n/a | existing rows in each (see Shared Patterns) | exact |
| `26-COMMAND-EVIDENCE.md` | docs | n/a | Phase 25 `25-COMMAND-EVIDENCE.md` shape | exact |

## Pattern Assignments

### `DescribeTableBuilder.java` (fork-owned service, transform)

**Analog A (shape):** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java`

Final class, private constructor, SLF4J logger, `static final` message constants, metadata-only WARN (lines 1-60):

```java
public final class DeclaredNullability {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeclaredNullability.class);

    static final String FALLBACK_WARNING = "Could not take element nullability for column {} of table {}.{} from its "
            + "declared type {} ({}); keeping the nullability DESCRIBE reported for it";

    static final String UNPAIRED_ELEMENT_WARNING = "Declared Tuple element(s) {} of column {} of table {}.{} "
            + "(declared type {}) have no matching DESCRIBE sub-column; applying the declared nullability to "
            + "the elements that paired";

    private static final String NAME_SEPARATOR = ".";
    ...
    private DeclaredNullability() {}
```

Copy for the D-04 WARN: a `static final String` constant (for example `UNPARSEABLE_PARENT_WARNING`) with `{}` placeholders for database, table, parent column, raw type (and exception class name only, never the message or any value). The builder is an instance class (state: `columns`, `columnsByName`, `declaredTypes`, `rawTypes`, `skippedCols`, `numColumns`, `hasDefaults`), so give it a non-private constructor `(String database, String tableName)`.

**Analog B (loop body to move verbatim):** `ClickHouseHelperClient.java` V2 loop, lines 612-656 (V1's identical copy is 541-583). Quote of the part the builder takes over:

```java
ClickHouseFieldDescriptor fieldDescriptor = ClickHouseFieldDescriptor.fromJsonRow(line);
// Count what system.columns counts: top-level columns, including the ones skipped below
if (!fieldDescriptor.isSubcolumn()) {
    numColumns++;
}

if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
    LOGGER.debug("Skipping column {} as it is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
    skippedCols.add(fieldDescriptor.getName());
    continue;
}
if (fieldDescriptor.isSubcolumn()) {
    String parentName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().lastIndexOf("."));
    if (skippedCols.contains(parentName)) {
        LOGGER.debug("Skipping subcolumn {} as its parent column is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
        skippedCols.add(fieldDescriptor.getName());
        continue;
    }
}

if (shouldSkipDescribeRow(fieldDescriptor, columnsByName)) {
    LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
    skippedCols.add(fieldDescriptor.getName());
    continue;
}

if (!fieldDescriptor.isSubcolumn()) {
    declaredTypes.put(fieldDescriptor.getName(), fieldDescriptor.getType());
}

if (fieldDescriptor.hasDefault()) {
    hasDefaults = true;
}

Column column = Column.extractColumn(fieldDescriptor);
//If we run into a rare column we can't handle, just ignore the table and warn the user
if (column == null) {
    LOGGER.error("Unable to handle column: {}", fieldDescriptor.getName());
    return null;
}
columns.add(column);
columnsByName.put(column.getName(), column);
```

Extraction rules (byte-for-byte behaviour in commit 26-02): keep `numColumns++` before any skip; keep message texts; `addRow` declares `throws JsonProcessingException` and does not wrap it; a `RuntimeException` from `Column.extractColumn` must propagate; the `.inner` early return stays in the two methods before any I/O; `build()` ends with the existing `ClickHouseHelperClient.buildDescribedTable(database, tableName, hasDefaults, columns, numColumns, declaredTypes)`. In 26-03 only the `shouldSkipDescribeRow` step becomes the six-way decision; `skippedCols.add` is skipped for the `size0`/bare `size` structural twin (cascade guard), and `rawTypes.put` runs for kept rows other than `KEEP_STRUCTURAL`/`KEEP_UNDECIDABLE`.

**Call-site shape after extraction** (in `describeTableV2`, keep the try/catch and settings text unchanged):

```java
DescribeTableBuilder builder = new DescribeTableBuilder(database, tableName);
...
while ((line = br.readLine()) != null) {
    if (!builder.addRow(line)) {
        return null;
    }
}
...
return builder.build();
```

V1: `builder.addRow(v.asString())` inside `for (ClickHouseRecord r : response.records())`; keep `catch (ClickHouseException | JsonProcessingException e)` (current line ~585) and V2's `catch (Exception e)` + `LOGGER.error("describeTableV2 failed", e)`.

---

### `ClickHouseHelperClient.shouldSkipDescribeRow` (modify; keep 2-arg, add 3-arg overload)

**Current text** (lines 473-488), to keep the 20+ existing 2-arg unit call sites compiling:

```java
static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar) {
    if (!fieldDescriptor.isSubcolumn()) {
        return false;
    }
    String name = fieldDescriptor.getName();
    if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
        return true;
    }
    for (int dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.', dot - 1)) {
        Column ancestor = columnsSoFar.get(name.substring(0, dot));
        if (ancestor != null && (ancestor.getType() == Type.JSON || ancestor.getType() == Type.DYNAMIC)) {
            return true;
        }
    }
    return false;
}
```

Move the ancestor loop verbatim ahead of the size rule (Pitfall 5: `j.a.size` must stay dropped). The 2-arg form delegates with an empty raw-type map, so a missing raw type is "undecidable", which skips `.size0`/`.size` exactly as before. Rule table is RESEARCH.md "Classifier rule" steps 1-9; compare offsets rows by building `"size"+k` and `Array(`^k `UInt64` strings (no `Integer.parseInt`).

---

### `DeclaredNullability.elementKey` (modify)

**Current definition to extract** (`collectTuple`, lines 228-229):

```java
for (int i = 0; i < elements.size(); i++) {
    String name = elements.get(i).getColumnName();
    byKey.put(name == null || name.isEmpty() ? String.valueOf(i + 1) : name, elements.get(i));
}
```

Target (the only new public API on this class); `collectTuple` must call it:

```java
public static String elementKey(ClickHouseColumn element, int index) {
    String name = element.getColumnName();
    return name == null || name.isEmpty() ? String.valueOf(index + 1) : name;
}
```

Do not touch `normalise` (line 77) or `fromJsonRow`. Anchor: `unwrapSimpleAggregate(` is private (count 2 in `fork-anchors.txt` line 76); to share it, widen to package-private and put `ParentShape` in `sink.db.mapping` (RESEARCH preference), which keeps `elementKey` the only public addition.

---

### `Column.java` (modify) and `Table.java` (modify, one line)

**Column analog** (line 66, 73-74): public Lombok setter style `@Setter private Column arrayType;`. Add `@Setter private boolean declaredElement;` (public, because the builder is in `sink.db.helper`; the existing `@Setter(AccessLevel.PACKAGE) private boolean isNullable;` at line 56 is the package-private counterpart and is not the model here). `@Builder` defaults it to false.

**Table.updateParent ARRAY branch** (lines 118-123) current text:

```java
case ARRAY:
    final String childName = child.getName();
    if (SIZE_FIELD_MATCHER.test(childName) || childName.endsWith(".null"))
        return;
```

Target:

```java
if ((SIZE_FIELD_MATCHER.test(childName) && !child.isDeclaredElement()) || childName.endsWith(".null"))
    return;
```

Leave the MAP branch (`.keys` or `SIZE_FIELD_MATCHER`, line 144) and the TUPLE branch (`parent.getTupleFields().add(child);`, line 194) unchanged. Anchor rows: `Table.java` `!child.isDeclaredElement()` count 1; a negative row for the old text count 0; `Column.java` `private boolean declaredElement;` count 1.

---

### `DescribeFixtureCapture.java` + `captureDescribeFixtures` task (batch harness)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java` (final class in the test source set, private constructor, `static final String` usage/exit constants, `main()` with `USAGE_EXIT = 2`, `VIOLATION_EXIT = 1`, JDK-only) and the Gradle wiring at `build.gradle.kts:421-429`:

```kotlin
tasks.register<JavaExec>("verifyConfluentArchive") {
    group = "verification"
    description = "Fails if the Confluent archive nests jars or bundles a Kafka API class"
    val archive = tasks.named<Zip>("createConfluentArchive").flatMap { it.archiveFile }
    inputs.file(archive)
    classpath = sourceSets["test"].output.classesDirs
    mainClass.set("com.clickhouse.kafka.connect.ci.ConfluentArchiveCheck")
    argumentProviders.add(CommandLineArgumentProvider { listOf(archive.get().asFile.absolutePath) })
}
```

Differences to apply: the harness needs Testcontainers and the ClickHouse client, so use `classpath = sourceSets["test"].runtimeClasspath` (not `classesDirs`), pass the output directory as `args(...)`, and group it `verification`. After editing `build.gradle.kts` run `TestsWorkflowPinGuardUnitTest` (it scans the file).

**Container start without JUnit** (`ClickHouseBase.setupContainer`, lines 108-132):

```java
ClickHouseContainer container = new ClickHouseContainer(clickhouseDockerImage)
        .withNetwork(network)
        .withNetworkAliases(ClickHouseTestHelpers.CLICKHOUSE_DB_NETWORK_ALIAS)
        .withPassword("test_password")
        .withExposedPorts(8123)
        .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
        ...
if (ClickHouseTestHelpers.CLICKHOUSE_VERSION_LATEST.equals(ClickHouseTestHelpers.getClickhouseVersion())) {
    container = container.withImagePullPolicy(PullPolicy.alwaysPull());
}
db.start();
```

**Raw-line DESCRIBE reader** (copy from `NullableRoundTrip.describeRows`, lines 170-190; add a `describeRawLines` variant that adds `line` instead of `ClickHouseFieldDescriptor.fromJsonRow(line)`; keep it in a public helper in `sink.db.helper` so the capture, the drift test and `NullableRoundTrip` share it):

```java
QuerySettings settings = new QuerySettings().setFormat(ClickHouseFormat.JSONEachRow);
settings.serverSetting(DESCRIBE_INCLUDE_SUBCOLUMNS, "1");
settings.setDatabase(chc.getDatabase());
...
try (QueryResponse response = chc.getClient()
                .query(String.format(DESCRIBE_QUERY_FORMAT, chc.getDatabase(), tableName), settings)
                .get();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))) {
    String line;
    while ((line = reader.readLine()) != null) { ... }
}
```

The test JVM defaults to `windows-1252`, so every fixture read/write names UTF-8 explicitly. `serverVersion(chc)` (line ~193, `SELECT version() AS v` via `RowSnapshots.select`) supplies the `SERVER_VERSION.txt` content.

---

### `src/test/resources/describe-fixtures/*.jsonl`

**Analog:** `src/test/resources/connect-api-references.txt`, a committed, regenerable reference list with a provenance header (`# image: ...` line, "Derived from ... by ..." line). Copy the idea: a sidecar `SERVER_VERSION.txt` carries version and image id; JSONL lines are byte-for-byte what the server printed. Docker-free tests load them from the classpath with `StandardCharsets.UTF_8`.

---

### `DescribeFixtureDriftLiveTest.java` (live, request-response)

**Analog:** `NullableTupleMatrixLiveTest` COLS-13 premise (lines ~795-815): create table, read DESCRIBE rows, assert the raw type equals the expected constant with `PREMISE_MESSAGE`:

```java
ClickHouseTestHelpers.dropTable(chc, tableName);
NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + NULLABLE_BACKSLASH_N_NAME_DDL));
...
for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
    if (CELL_COLUMN.equals(row.getName())) {
        assertEquals(NULLABLE_BACKSLASH_N_NAME_RAW, row.getType(), PREMISE_MESSAGE);
    }
}
```

Adapt: per catalog key, `assertEquals(committedLines, describeRawLines(...), key)` so the failure message names the drifted table. Extends only `ClickHouseBase`; runs in the full suite, not `testUnit`. If the class lives in `sink.db.helper`, make the shared reader public (the package-private `NullableRoundTrip` is in `sink`).

---

### `DescribeTableBuilderUnitTest.java` and the D-04 pin flip (Docker-free)

**Analog:** `ClickHouseHelperClientForkUnitTest.java`. Do not extend `ClickHouseBase`; leave untagged so it runs in `testUnit`.

The WR-02 copy being retired (`discoverAsDescribeTableV2`, lines 316-342) re-implements the loop with `shouldSkipDescribeRow(descriptor, columnsByName)`; the replacement feeds JSON lines to `new DescribeTableBuilder(DATABASE, TABLE)` and calls `build()`. `discover(...)` (lines 287-310) stays for pre-skipped-row cases.

**D-04 pin to rename and invert (lines 624-660):** keep the R-01 `rows`, the `LogCapture` attach pattern and the `apply() == true` assertion; flip the middle:

```java
try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
        LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
    Column root = discoverAsDescribeTableV2(rows).getRootColumnsMap().get(ROOT_SENTINEL);
    Column tuple = arrayWrapped ? root.getArrayType() : root;
    List<String> fieldNames = tuple.getTupleFields().stream().map(Column::getName).toList();
    assertFalse(fieldNames.contains(unpaired), label + ": the dropped element is not a Column");   // flips to assertTrue
    assertEquals(0, warnCount(helperLog), label);
    List<LogCapture.CapturedEvent> warnings = nullabilityLog.events(WARN_LEVEL);
    assertEquals(1, warnings.size(), label);                                                        // flips to 0
    ...
    assertTrue(DeclaredNullability.apply(root, rawType, DATABASE, TABLE), label);                   // kept
}
```

Rename (backslash-free name, for example `sizeNamedTupleElementIsKeptByDiscoveryAndRaisesNoWarning`), update `@DisplayName` and `@ParameterizedTest(name = ...)`, drop the "Known defect 999.4" comment (lines 624-627), add the builder `LogCapture` too. Seen red on the pre-fix tree before the fix commit (E-04). `LogCapture` API: `LogCapture.attach(Class)`, try-with-resources, `events("WARN")`, `CapturedEvent.message()/hasThrowable()`.

**Fixture-driven assertions** per RESEARCH "Unit table": DDL-derived tupleFields keys vs `elementKey`, no `*.null` Column, `.size1` rows present in `getAllColumnsList()` but absent from every `tupleFields`, no Column below a JSON root, cascade rows (`e.size0.a` kept, `e.size0.size` dropped), the fail-closed row (truncated `Tuple(` parent: one builder WARN with db/table/column/raw type, no exception text, plus the COLS-14 WARN still firing), zero WARNs across all fixtures.

---

### `TupleSizeNamedElementRoundTripLiveTest.java` (live, CRUD)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` (extends `ClickHouseBase`, parameterised, package `com.clickhouse.kafka.connect.sink` so the package-private `NullableRoundTrip` is reachable).

Skeleton from the COLS-13 live test (lines ~799-830):

```java
@ParameterizedTest(name = "...{0}")
@EnumSource(Twin.class)
void ...(Twin twin, TestInfo testInfo) throws Exception {
    Map<String, String> props = getBaseProps();
    ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
    String tableName = createTopicName(TABLE_PREFIX + "bsn_" + twin);
    ClickHouseTestHelpers.dropTable(chc, tableName);
    NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + ...));
    props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
    try {
        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            Table table = chc.describeTable(chc.getDatabase(), tableName);
            assertEquals(List.of(), log.events("WARN"), "...");
            ...
        }
        // build Struct records, NullableRoundTrip.putAndStop(props, records), NullableRoundTrip.selectAndAssert(chc, tableName, id, cells)
    } finally {
        ClickHouseTestHelpers.dropTable(chc, tableName);
    }
}
```

Helpers to reuse (all `static`, package-private in `NullableRoundTrip`): `createTable(chc, tableName, ddlFormat)` (line 81), `putAndStop(props, records)` (90), `selectAndAssert(chc, tableName, id, cells)` (101), `assertPutFails(...)` (134), `describeRows` (174), `serverVersion` (~193); plus `RowSnapshots.select(chc, sql, aliases...)` from `src/testFixtures/.../helper/RowSnapshots.java`. Create tables before `start()`; typed read-back with `assertEquals`, never `countRows`. Use `@MethodSource` over the shared `DescribeFixtureCatalog` (33 E-15 tables plus new shapes) so a failure names the table. Attach `LogCapture` to `DeclaredNullability.class` and the builder class for the zero-WARN assertion. D-09 record: `"abc" + '\u0003' + "xyz"` on the `s4_a01` shape `Tuple(size UInt32, v String)`; expect `hex(tupleElement(c,'v')) = 6162630378797A`.

**Manual V1 run** (`ClickHouseHelperClientForkLiveTest` builds its helper via `getBaseProps()` + `ClickHouseTestHelpers.createClient(props)` which honours `CLIENT_VERSION`):

```bash
CLIENT_VERSION=V1 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*ClickHouseHelperClientForkLiveTest' --tests '*TableTest' --rerun
```

---

## Shared Patterns

### Fork logic outside upstream method bodies, one anchored call site
**Source:** `DeclaredNullability` (Phase 19) and `.planning/scripts/fork-anchors.txt` rows 74-88, for example:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	DeclaredNullability.apply(	1	COLS-10
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	buildDescribedTable(	3	COLS-10
src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java	UNPAIRED_ELEMENT_WARNING	2	COLS-14
```
Format: `path TAB pattern TAB count TAB ID` (TAB-separated, `grep -cF` line counts, so patterns cannot contain a tab). **Apply to:** the extraction commit (counts for `shouldSkipDescribeRow` 3 to 1 and `buildDescribedTable(` 3 to 1 move in that same commit, plus `new DescribeTableBuilder(` 2) and the fix commit (`Table.java`, `Column.java`, `elementKey(` rows; a negative count-0 row for the old `Table` text). Re-measure counts at execution. The anchors invariant is evaluated at every commit.

### Metadata-only WARN
**Source:** `DeclaredNullability.FALLBACK_WARNING` (above). **Apply to:** the new D-04 classifier WARN (database, table, parent column, raw type, exception class name only; never record data or exception message). Silent in the pure 2-arg `shouldSkipDescribeRow`.

### Log capture in tests
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java` (`attach(Class)`, `AutoCloseable`, sets DEBUG, restores on close; `events(level)`, `allTexts()`). **Apply to:** unit table, D-04 flip, live round-trip.

### Docker-free vs live test split
Docker-free tests: untagged, no `ClickHouseBase`, run in `testUnit` (`ClickHouseHelperClientForkUnitTest`). Live tests: extend `ClickHouseBase`, run in the full suite (`ClickHouseHelperClientForkLiveTest`, `NullableTupleMatrixLiveTest`). `TableTest` extends `ClickHouseBase`, so new Docker-free `Table` rows go in a new `TableSizeNamedElementTest`, not in upstream's `TableTest`.

### Gradle and gating conventions
Every Gradle call: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; two-run gates use `--rerun`. Spotless baseline has 46 pre-existing violating files, so check each new file by copying into a scratch worktree and running `spotlessApply` there plus `cmp` (the tree-wide `spotlessApply` is not run). Exclude `gradlew.bat` from verify strings. Standing gate: `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD` plus the self-test.

### Document edits (DESC-10, in the fix commit)
Per RESEARCH "Document edits": `docs/FORK-DELTA.md` (line 47 `shouldSkipDescribeRow` row, line 51 `DeclaredNullability` row, line 106 remove `Table.java` from "Adopted wholesale" and add a Connector-source row, Test-harness row for `captureDescribeFixtures`, item 10 at line 177 rewritten in the closed form of items 1-3 and 7, ~line 215 sync sentence), `CHANGELOG.md` (new top `# 1.7.0_gg4` section, one Bug Fixes line), `.planning/ROADMAP.md` backlog 999.4 marked delivered. Do not edit the `jacocoForkReport` list (DEBT-17, Phase 28).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `ParentShape` classifier (arrayDepth + Tuple element declaration check) | utility | transform | No existing code computes Array depth or "declares element key" for a parsed `ClickHouseColumn`; build from `DeclaredNullability.unwrapSimpleAggregate` and `collectTuple` plus RESEARCH "Pattern 2". |
| Capture harness that boots Testcontainers outside JUnit | utility | batch | `ConfluentArchiveCheck` is JDK-only; the container recipe comes from `ClickHouseBase.setupContainer` (instance method on a JUnit base class), so the harness copies its builder chain rather than calling it. |

## Metadata

**Analog search scope:** `src/main/java/.../sink/db/{helper,mapping}`, `src/test/java/.../sink` and `.../sink/db/helper`, `.../ci`, `src/testFixtures`, `src/test/resources`, `build.gradle.kts`, `.planning/scripts/fork-anchors.txt`
**Files scanned:** about 14 read (partially), tracked status checked with `git ls-files` for each named analog
**Pattern extraction date:** 2026-10-21
