# Phase 28: Raw Identifier Names in Discovery - Pattern Map

**Mapped:** 2026-10-23
**Files analyzed:** 17 (6 production, 3 build/doc/anchor, 8 test)
**Analogs found:** 17 / 17 (all analogs are git-tracked; the files edited in place are their own best analog)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/.../sink/db/helper/ClickHouseFieldDescriptor.java` (mod: `fromJsonRow`) | utility/model | transform | itself (lines 47-49) plus `DescribeTableBuilder.addRow` caller | exact |
| `src/main/java/.../sink/db/helper/ClickHouseHelperClient.java` (mod: `describeTableV1`) | service | streaming (line read) | `describeTableV2` same file, lines 519-547 | exact |
| `src/main/java/.../sink/db/mapping/DeclaredNullability.java` (mod: `normalise` rewrite) | utility | transform | itself, lines 53-117 | exact (rewrite) |
| `src/main/java/.../sink/db/mapping/Column.java` (mod: `extractEnumValues`) | model | transform | itself, lines 376-385 | exact (rewrite) |
| `build.gradle.kts` (mod: `forkChangedClasses`, DEBT-17) | config | n/a | existing list, lines 310-340 | exact |
| `.github/scripts/jacoco_summary.py` (mod: `FORK_CLASSES_IN_ORDER`) | config | n/a | itself | exact |
| `.planning/scripts/fork-anchors.txt` (mod rows) | config | n/a | rows 30-31 (HLP-04), COLS-13, DESC-08, WRT-18 rows | exact |
| `docs/FORK-DELTA.md`, `CHANGELOG.md`, `.planning/ROADMAP.md` (mod) | doc | n/a | existing rows 49, 65, Known costs item 11 | exact |
| New live test: both-form name pins (`ClickHouseBase`), e.g. `RawIdentifierNameLiveTest` | test | CRUD (write + typed read-back) | `TupleSizeNamedElementRoundTripLiveTest` + `NullableRoundTrip` helpers | role-match |
| New Docker-free test: `fromJsonRow` with `"` in name or type | test | transform | `DescribeTableBuilderUnitTest`, `DescribeFixturesUnitTest` | role-match |
| Live V1 describe pin (optional, Open Question 3) | test | request-response | `TableTest.extractCommentV1` (lines 57-73) | exact |
| `DeclaredNullabilityTest` (changed pins + NAME-01 pin) | test | transform | itself: `normaliseIsTotalAndKeepsNonAsciiNames` (line 325), `typesWithoutABackslashInsideQuotesNormaliseAsBefore` | exact |
| `DeclaredNullabilityFixtures` (`BACKSLASH_N_ELEMENT`, `REAL_NEWLINE_ELEMENT`) | test fixture | n/a | itself | exact |
| `NullableTupleMatrixLiveTest` (lines ~826-842, ~902 raw names) | test | CRUD | itself (pairing oracle) | exact |
| Enum label live pins (NAME-06) | test | CRUD | `NullableEnumRoundTripLiveTest` + Phase 27 lookup-miss pins | role-match |
| Evidence file (NAME-02 matrix) | doc | n/a | `23-EVIDENCE.md` R-02 (milestones dir) | role-match |

## Pattern Assignments

### `ClickHouseFieldDescriptor.java` (utility, transform)

**Analog:** itself. Keep class annotations (`@Data @Builder @Jacksonized @JsonNaming(SnakeCase)`) and the checked `JsonProcessingException` signature: callers `DescribeTableBuilder.addRow` and `describeTableV1` catch it.

**Current decode site** (lines 47-49):
```java
public static ClickHouseFieldDescriptor fromJsonRow(String json) throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(json.replace("\\", "\\\\").replace("\n", "\\n"), ClickHouseFieldDescriptor.class);
}
```
**Target shape** (RESEARCH Pattern 1, prototype): streaming `JsonParser`; field `name` via `p.getText()` (decoded); every other string field as the literal inner text `json.substring(start + 1, end - 1)` using `currentTokenLocation()/currentLocation()` char offsets; `is_subcolumn` boolean from 0/1. Throw only parser-generated exceptions (never embed the row).

### `ClickHouseHelperClient.java` (service, streaming)

**Analog:** `describeTableV2`, lines 525-546 (the raw-line UTF-8 loop to reuse in V1):
```java
DescribeTableBuilder builder = new DescribeTableBuilder(database, tableName);
try (QueryResponse queryResponse = client.query(describeQuery, settings).get();
     InputStream describeStream = queryResponse.getInputStream();
     BufferedReader br = new BufferedReader(new InputStreamReader(describeStream, StandardCharsets.UTF_8))) {
    String line = null;
    while ((line = br.readLine()) != null) {
        if (!builder.addRow(line)) {
            return null;
        }
    }
}
return builder.build();
```
**To replace** in `describeTableV1` (lines 504-510):
```java
for (ClickHouseRecord r : response.records()) {
    ClickHouseValue v = r.getValue(0);
    if (!builder.addRow(v.asString())) { return null; }
}
```
Use `response.getInputStream()` with explicit UTF-8 (tests run with `file.encoding=windows-1252`). Prefer one shared private helper feeding a `DescribeTableBuilder` from an `InputStream` so anchors `StandardCharsets.UTF_8`=1 and `InputStreamReader`=2 (HLP-04, `fork-anchors.txt` lines 30-31) stay unchanged. Remove `ClickHouseRecord`/`ClickHouseValue` imports only if unused elsewhere.

### `DeclaredNullability.java` (utility, transform; `normalise` rewrite)

**Analog:** itself. Constants to reuse (lines 53-58): `SPACE`, `BACKSLASH`, `BACKTICK`, `SINGLE_QUOTE`, `NO_QUOTE`. Current loop (lines 78-117) encodes the double layer through `run / 2`; rewrite, do not extend. Line 102 is the NAME-01 mutation target:
```java
if (escaped == quote && (run / 2) % 2 == 0) {
    quote = NO_QUOTE;
}
```
**Target** (RESEARCH Pattern 2): (1) lenient total JSON-unescape (`\\ \" \/ \n \t \r \b \f \uXXXX`, unknown kept); (2) quote-aware pass: outside quotes LF becomes space; inside quotes `\n \t \r` become the characters, other backslash pairs copied, a backslash consumes the next char so `` \` `` and `\'` never close. No exceptions; unterminated quote leaves remainder unchanged. Call site stays `ClickHouseColumn.of(columnName, normalise(declaredType))` (line 154); `elementKey(` (line 227, 2 occurrences) needs no change. Anchors: `private static final char BACKTICK` (1) and `declaredType.replace(ESCAPED_NEWLINE, SPACE)` (0) must hold. `ParentShape.parse` and `DescribeTableBuilder` need no edit.

### `Column.java` (model, transform; `extractEnumValues`)

**Analog:** itself, lines 376-385:
```java
private static Map<String, Integer> extractEnumValues(String valueType) {
    Map <String, Integer> data = new HashMap<>();
    List<String> values = Utils.splitIgnoringQuotes(valueType.substring(valueType.indexOf("(") + 1, valueType.indexOf(")")), ',');
    for (String value : values) {
        String[] val = value.split("=", 2);
        String key = val[0].trim();
        data.put(key.substring(1, key.length() - 1), Integer.parseInt(val[1].trim()));
    }
    return data;
}
```
Replace with an escape-aware left-to-right scanner inside `Column` (JSON-layer unescape, then SQL unescape with `\'`, `\\`, `\n/\t/\r`; stops at the closing `)` outside quotes). Fixes the apostrophe-label `NumberFormatException` that unmaps the table. Do not touch `Utils.splitIgnoringQuotes` or the `ENUM_LABEL_NOT_FOUND_FORMAT` / `NO_ENUM_CONTENT_FORMAT` constants (WRT-18 anchors stay 2 each; `return enumValues.get(value);` stays 0). Shared unescape: package-private static on `DeclaredNullability` or a short duplicate (Open Question 1).

### `build.gradle.kts` + `.github/scripts/jacoco_summary.py` (config)

**Analog:** the existing list (lines 310-340), two entries per class:
```kotlin
"com/clickhouse/kafka/connect/sink/db/mapping/Table.class",
"com/clickhouse/kafka/connect/sink/db/mapping/Table\$*.class",
```
Add the same pair for `DeclaredNullability`, `ParentShape`, `DescribeTableBuilder`, `WithheldMessageException`, `ClickHouseFieldDescriptor`, `MetadataFreeJsonWriter` (Open Question 4), plus any helper class added. Edit `FORK_CLASSES_IN_ORDER` (and its "15" comment) in the same commit. Update the comment saying `ClickHouseFieldDescriptor` "can never be pulled in". Keep `jacocoForkReport` count (3, BLD-03). This is the milestone's last `build.gradle.kts` edit.

### `fork-anchors.txt` (config)

**Format:** `path<TAB>fixed-string<TAB>count<TAB>ID`. Example rows to keep consistent (lines 30-31):
```
src/main/java/.../ClickHouseHelperClient.java	StandardCharsets.UTF_8	1	HLP-04
src/main/java/.../ClickHouseHelperClient.java	InputStreamReader	2	HLP-04
```
Move rows in the same commit as the code; add a 0-count row that `.replace("\\", "\\\\")` is gone from `ClickHouseFieldDescriptor.java`. Gate: `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD`.

### New live both-form pin test (test, CRUD)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java` (extends `ClickHouseBase`; `@ParameterizedTest` + `@MethodSource`; `LogCapture.attach(...)` for WARN checks; typed read-back; metadata-only log line, never record values). Imports to copy (lines 3-27): `NullableRoundTrip.Cell`, `ClickHouseHelperClient`, `ClickHouseTestHelpers`, `RowSnapshots`, `LogCapture`, Kafka `Schema/SchemaBuilder/Struct/SinkRecord`, JUnit params.

**Helpers** in `NullableRoundTrip.java` (package `...sink`): `createTable(chc, tableName, ddlFormat)` (l.75), `putAndStop(props, records)` (l.84), `selectAndAssert(chc, tableName, id, List<Cell>)` (l.95), `assertPutFails(...)` (l.128, for the loud doubled-form Tuple), `describeRows(chc, tableName)` (l.170). Write non-ASCII as Java `é` escapes (test JVM is windows-1252). Doubled form: root column ignored with `DEFAULT` stored; Tuple element fails loudly.

### V1 live describe pin (optional)

**Analog:** `src/test/java/.../db/mapping/TableTest.java` `extractCommentV1` lines 57-73:
```java
Map<String, String> props = getBaseProps();
props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V1");
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
...
Table table = chc.describeTable(chc.getDatabase(), tableName);
assertNotNull(table);
```
Use a tab and a double-quote name (both fail on V1 today). The manual run uses env `CLIENT_VERSION=V1`.

### `DeclaredNullabilityTest` / `Fixtures` / `NullableTupleMatrixLiveTest` (changed pins)

**Analog:** `DeclaredNullabilityTest.normaliseIsTotalAndKeepsNonAsciiNames` (line 325) uses `DeclaredNullability.normalise(...)` and Java `é` escapes; its expected strings change by one escape layer (red-first). Keep `typesWithoutABackslashInsideQuotesNormaliseAsBefore` green unchanged. NAME-01 literal (RESEARCH): input `"Tuple(\\n    `a\\\\`\\\\\\\\nb` Int32,\\n    d String)"`, pinned on the current code and seen red under the always-close mutation at line 102. Add a behavioural pin: `apply` pairs a backtick-named element with no fallback WARN (red today). Add rows only; never delete or weaken.

## Shared Patterns

### Red-first and anchors
Every changed pin is seen red on the unchanged tree first. Anchor rows move in the same commit as their code. Never change `fromJsonRow` without the `normalise` rewrite and V1 raw read (single NAME-03/04 commit).

### Value-free errors and logging
Column metadata and class names only; no record values, row text, or labels in exceptions (applies to `fromJsonRow`, enum scanner, and the lookup-miss text). New log lines at DEBUG only.

### Live test conventions
Extend `ClickHouseBase`; typed SQL read-back with `assertEquals`; `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; `--rerun` for two-run gates; docker-free tests untagged (run in `testUnit`); no Mockito inline-mock of `ClickHouseHelperClient`.

### Document edits (same commit as fix, NAME-07)
`docs/FORK-DELTA.md` (new row; row 49 HLP-04 now covers V1; retire Known costs item 11 and the cross-reference near line 218; jacoco row near line 65), `CHANGELOG.md` one line under `1.7.0_gg4` Bug Fixes, `.planning/ROADMAP.md` 999.5 delivered. No `docs/ROLLOUT-v1.7.0_gg4.md` (Phase 29).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| Streaming `JsonParser` name-decode / literal slice in `fromJsonRow` | utility | transform | No streaming-Jackson row reader exists; use RESEARCH Pattern 1 prototype |
| Escape-aware Enum scanner | utility | transform | No scanner exists; `Utils.splitIgnoringQuotes` is the thing being replaced; RESEARCH Pattern 4 |
| Committed name-matrix `.jsonl` fixture (optional) | fixture | n/a | Existing `src/test/resources/describe-fixtures/*.jsonl` rows are backslash-free by construction; follow `DescribeFixtureCatalog` format |

## Metadata

**Analog search scope:** `src/main/java/.../sink/db/{helper,mapping}`, `src/test/java/.../sink`, `build.gradle.kts`, `.planning/scripts`, `.github/scripts`
**Tracked-source check:** `ClickHouseFieldDescriptor.java` and `.github/scripts/jacoco_summary.py` confirmed via `git ls-files`; all other paths are standard tracked source or tests.
**Pattern extraction date:** 2026-10-23
