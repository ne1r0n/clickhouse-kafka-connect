# Phase 28: Raw Identifier Names in Discovery - Research

**Researched:** 2026-10-23
**Domain:** ClickHouse `DESCRIBE TABLE` row decoding (JSON escaping layers), declared-type parsing with `ClickHouseColumn.of`, Enum label parsing, JaCoCo fork-class scope, merge-invariant anchors
**Confidence:** HIGH for the measured escaping layers and the decode design (prototyped end to end against a live server); MEDIUM for the shape of the final code (prototype is not production code); open questions listed at the end.

All live measurements below ran on ClickHouse server `26.9.19.9` (`clickhouse/clickhouse-server:latest`, image id `bdb5c911a23d`, already local) with the repo's own client code (client V2 and client V1, `client-v2 0.9.6` / `clickhouse-client 0.9.6`). They were taken in a throwaway container (`gsd28-ch`, since removed) and a scratch directory outside the repo (`/tmp/gsd28`). The repo's tracked files were not touched. This research does not replace the NAME-02 spike (a phase task); it removes the guesswork the ROADMAP research flag worried about, and its raw rows can seed the spike.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Locked by ROADMAP, REQUIREMENTS and the 2026-10-21 user decisions, not re-opened here:**

*Order inside the phase*
1. NAME-01 pin on the current code, seen red under a mutation that always closes the quote.
2. NAME-02 live name-matrix spike.
3. NAME-03 and NAME-04 in one commit. Never change `fromJsonRow` without the pairing key.
4. NAME-05 live pins.
5. NAME-06.
6. NAME-07 document edits, in the fix commit.
7. DEBT-17, last (the milestone's last `build.gradle.kts` edit).

*Rules that hold throughout*
- Canonical form is the raw identifier as written in `CREATE TABLE`. No dual-form alias, no metadata-only WARN for a doubled-form name (NAME-F1 deferred). Operators rename.
- Decode names only. The DESCRIBE type text is not decoded (it changes every multi-line Tuple type).
- `normalise` in `DeclaredNullability` is rewritten, not extended: after the decode the type text holds one escape layer fewer, and a naive decode turns the COLS-13 twins red for LF, tab and CR.
- The fix changes the bytes of a retried batch. Record for Phase 29 the clean stop in both directions, with the rename as its precondition.
- No edit reaches `Processing`, `KeeperStateProvider` or `ClickHouseSinkConfig`. `ClickHouseWriter.java` is high risk.
- Existing tests: add rows, never delete or weaken. Red-first for every changed pin.
- Every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`; Docker is colima profile `vz`; a two-run gate uses `--rerun`; typed SQL read-back; no Mockito inline-mock of `ClickHouseHelperClient`; Spotless baseline-equal; anchors invariant passes at every commit.
- Nothing is pushed. `origin/custom_build` stays frozen at `69cd680b`.
- Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

**Enum labels (NAME-06)**
- **D-01:** Enum labels are **fixed**, not documented. `Column.extractEnumValues` currently reads labels from the still-escaped type text, so a label with an escaped character is keyed in escaped form and a raw-form record value misses (the Phase 27 `IllegalArgumentException` lookup miss). The fix unescapes the label inside `Column.extractEnumValues` only, so labels follow the same raw-identifier rule as names. — **Reversibility:** costly — it changes the bytes of retried batches for enum columns with escaped labels, so Phase 29's rollout note must cover it.
- **D-02:** Scope guard: if the fix would reach beyond `Column`'s enum-label parsing, planning stops for a user decision (ROADMAP research flag). The Phase 27 lookup-miss pins stay and must remain green. Each enum label that round-trips gets a live pin with typed read-back. If the spike shows a label shape that cannot be fixed inside `extractEnumValues`, that shape's remaining behaviour goes into FORK-DELTA Known costs and the Phase 29 record.

**Double-quote names**
- **D-03:** A double quote in a name is a **hard requirement**: the table must map (today `describeTable` returns null for it, FORK-DELTA Known costs item 11) and a record using the raw name must land with typed read-back equal. If the cause of the unmapped table lies outside `fromJsonRow` and the V1 raw-line read, the plan stops and asks the user before widening the fix. — **Reversibility:** reversible.

**Spike scope and evidence (NAME-02)**
- **D-04:** The matrix covers the full shapes: root column, Tuple element, Array(Tuple) element, Nullable(Tuple) element, Map-value Tuple element, and enum label, for every character in the goal (backslash, literal backslash-n, LF, tab, CR, slash, double quote, backtick, non-ASCII). Each cell records the current form and the decoded form. Run live on `latest` × V2 in the suite, plus the one manual `CLIENT_VERSION=V1` run the ROADMAP requires for the V1 raw-line UTF-8 read. The record is kept as a phase evidence file and is the source for Phase 29's per-character old/new name table.

**Doubled-form pins and handoff (NAME-05)**
- **D-05:** One **new `ClickHouseBase` live test** pins both forms per spiked character: raw name lands with typed read-back equal to the record; doubled form behaves as today (root column ignored and `DEFAULT` stored; Tuple element fails loudly). Pinned, not changed. `NullableTupleMatrixLiveTest` stays the pairing oracle and is not mixed with write-path pins; its COLS-13 twins must stay green, and (per Phase 27 DEBT-16) a premise lookup by the old name must fail loudly after the decode.
- **D-06:** Phase 28 hands Phase 29 a record, not the rollout note itself: the measured per-character old/new name table, the root-silent versus element-loud behaviour, the enum-label outcome, and the clean stop in both directions with the rename as its precondition. `docs/ROLLOUT-v1.7.0_gg4.md` is written in Phase 29 only.

### Claude's Discretion
- The mechanism of the names-only decode in `ClickHouseFieldDescriptor.fromJsonRow` (which fields are decoded and how the type text is kept unchanged) and the shape of the rewritten `normalise`, within the rules above.
- The exact form of the NAME-01 mutation proof and the red-first evidence for each pin.
- The pairing-key helper shape, reusing Phase 26's single element-key helper in `DeclaredNullability`.
- Which classes `jacocoForkReport` lists for DEBT-17 beyond `DeclaredNullability`, `WithheldMessageException` and the classes Phases 25–28 added (the list is derived from the diff, edited once, with its FORK-DELTA row in the same commit).
- The plan split and commit order inside the fixed seven-step order; the evidence file layout.

### Deferred Ideas (OUT OF SCOPE)
- NAME-F1: a one-release metadata-only WARN for a Kafka field matching only the doubled form (recommended against; reconsider if operators report missed renames).
- COV-F1: client V1 coverage in CI for the describe path (one manual run per change remains the evidence).
- Decoding the DESCRIBE type text in `fromJsonRow` (out of scope, changes every multi-line Tuple type).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| NAME-01 | Escaped-quote branch of `normalise` pinned on current code, red under an always-close mutation | Section "NAME-01 pin": the 23-REVIEW WR-01 literal was run against the current `normalise` and against the mutation; outputs differ (verified) |
| NAME-02 | Live name-matrix spike on `latest` for root columns, Tuple elements, enum labels | Section "Measured escaping layers" gives the raw rows, the current forms and the decoded forms for all nine characters, V1 and V2; the DDL is reusable |
| NAME-03 | `fromJsonRow` yields the raw identifier for names (type text unchanged); V1 reads raw lines in UTF-8; `TableTest.extractCommentV1` green | "Pattern 1" streaming decode, byte-identical on all 272 committed fixture rows; "Pattern 3" V1 raw-line read |
| NAME-04 | `DeclaredNullability` pairs on the raw-name key; `normalise` rewritten; COLS-13 twins green; IN-01, IN-02 closed | "Pattern 2": the rewritten `normalise` makes `ClickHouseColumn.of` return raw element names for all nine characters; pairing then needs no change to `elementKey` |
| NAME-05 | Live both-form pins | End-to-end prototype run: raw-name record lands at root, in `Tuple` and in `Nullable(Tuple)` for all nine characters; doubled-form Tuple record fails loudly |
| NAME-06 | Enum labels measured; fix or document | Measured: labels are keyed in doubly escaped form today; an apostrophe label crashes discovery; fix design in "Pattern 4" |
| NAME-07 | FORK-DELTA row, CHANGELOG line, Known costs item 11 retired, 999.5 delivered, same commit as the fix | "Document and anchor edits" lists every row and the anchor counts that move |
| DEBT-17 | `jacocoForkReport` lists `DeclaredNullability`, `WithheldMessageException` and every fork class Phases 25–28 added | "DEBT-17" lists the diff-derived class set and a second file (`.github/scripts/jacoco_summary.py`) that duplicates the list |
</phase_requirements>

## Summary

The whole defect is one pair of escaping layers that `fromJsonRow` flattens inconsistently. The server's `DESCRIBE ... FORMAT JSONEachRow` row is JSON; inside the `type` string the identifiers are additionally SQL-escaped (backtick or single-quote form). `fromJsonRow` doubles every backslash before Jackson parses, so every JSON string comes out as the literal inner text of the JSON string: the name `a\b` comes out as `a\\b`, a slash as `a\/`, a line feed as the two characters `\n`. That is the "doubled form". A double quote in any string makes the doubled text invalid JSON, which is why the table is unmapped. Client V1 is a second, different case: `response.records()` hands over the row after a TabSeparated-style unescape, so V1 and V2 disagree today for the same table (details below). The measured fix has three parts that must land together: a names-only decode in `fromJsonRow` (the other string fields keep the literal inner text, so `getType()` stays byte-identical), a rewritten `normalise` that turns the layered type text into text `ClickHouseColumn.of` parses into raw element names, and a V1 describe path that reads the raw HTTP stream in UTF-8 like V2 does.

A prototype of exactly that design, compiled outside the repo against the repo's classes, produced: every one of the nine characters mapped as a raw name at root level, inside `Tuple`, inside `Nullable(Tuple)` (elements non-nullable, as declared), with no declared-nullability fallback WARN, and a Kafka record using the raw names landed with the expected values at all three levels. The same record with doubled names fails loudly on the Tuple. The prototype also shows that `ClickHouseColumn.of` unescapes a quoted name only by dropping the backslash (`\n` becomes `n`), which is the real reason a naive decode breaks LF, tab and CR pairing: the rewritten `normalise` must convert `\n`, `\t`, `\r` inside quotes into the actual characters before parsing.

Two findings are outside what CONTEXT anticipated and need planner attention: (1) an Enum label containing an apostrophe that is followed by another label today makes `extractEnumValues` throw `NumberFormatException`, which unmaps the whole table (baseline, independent of this phase's decode); the NAME-06 fix therefore has to be a small escape-aware scanner inside `Column`, not just an unescape of the existing split result; (2) the CI coverage summary script `.github/scripts/jacoco_summary.py` carries its own copy of the 15-class list, so DEBT-17 must edit it in the same commit as `build.gradle.kts`, or the generated summary will silently omit the new classes.

**Primary recommendation:** Implement the decode as a streaming Jackson read in `fromJsonRow` (name decoded, every other string field sliced as its literal JSON inner text), rewrite `normalise` as "JSON-unescape the layer, then a quote-aware pass that turns `\n`, `\t`, `\r` inside quotes into the characters and the line break outside quotes into a space", give V1 the same raw-line reader as V2, write Enum label parsing as an escape-aware scanner inside `Column`, and land NAME-03 plus NAME-04 plus the V1 reader (and the anchors they move) in one commit.

## Project Constraints (from CLAUDE.md)

Directives from `./CLAUDE.md` (imports `AGENTS.md`) and `.claude/CLAUDE.md` that bear on this phase:

- Java 17, Gradle Kotlin DSL, Lombok for boilerplate, SLF4J (TRACE per record, DEBUG per batch, INFO lifecycle), JUnit 5 plus Mockito for feature tests.
- High-risk files: `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `Processing.java`, `KeeperStateProvider.java`, `RangeContainer.java`. This phase must not edit the last four and should not need to edit `ClickHouseWriter.java` at all (the prototype wrote correctly with no writer change).
- Do not log secrets or data in plaintext; error messages must be clear and actionable.
- Run `./gradlew spotlessApply` after changes (project carries 46 pre-existing Spotless violations per memory; the gate is "baseline-equal", not clean).
- New functionality needs feature-test coverage; tests that write to ClickHouse must read back and `assertEquals` against the original; DRY; read `ClickHouseTestHelpers.java` before writing tests.
- Existing tests: only add, never delete or weaken. Do not use `InMemoryState` in production paths.
- Only write code comments that convey non-obvious invariants.
- Do not upgrade dependencies without asking. This phase needs no dependency change.
- Do not commit without being asked (the GSD workflow commits for the executor; research does not commit).
- `.claude/CLAUDE.md`: tests target ClickHouse `latest` + client V2 only; changes additive and rebasable; code changes limited to genuine bug fixes.
- Memory-recorded environment rules: Docker is colima profile `vz` (`DOCKER_HOST` already exported); run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`; an intermittent OpenJ9 Mockito crash (exit 255) is handled by deleting the core dump and re-running once.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Decode of `DESCRIBE` row names | Connector discovery (`ClickHouseFieldDescriptor.fromJsonRow`) | — | Single decode site shared by V1 and V2 through `DescribeTableBuilder` |
| Raw-line transport of the describe response (V1) | Connector discovery (`ClickHouseHelperClient.describeTableV1`) | ClickHouse client V1 stream API | V1 `records()` re-decodes the row; only the raw stream carries the server's JSON |
| Declared-type parse and pairing key | Connector mapping (`DeclaredNullability`, `ParentShape`) | `ClickHouseColumn.of` (client library) | `ClickHouseColumn.of` owns the SQL-name unescape, which is lossy for `\n \t \r`; `normalise` must pre-shape the text for it |
| Enum label keys | Connector mapping (`Column.extractEnumValues`) | — | D-02 keeps the fix inside this method |
| Name match on write | Connector writer (`ClickHouseWriter`, unchanged) | Kafka `Struct` field names | Looks fields up by `Column.getName()`; correct once discovery yields raw names |
| Name match on schemaless JSON | ClickHouse server (JSONEachRow key match) | `cleanupExtraFields` root filter | Root keys are compared to `Column.getName()`; nested keys are matched by the server |
| Fork coverage scope | Build (`build.gradle.kts`) | CI summary script (`.github/scripts/jacoco_summary.py`) | Two copies of the class list |
| Merge-invariant anchors | Planning scripts (`fork-anchors.txt`) | — | Rows must move in the same commit as the code they pin |

## Measured escaping layers (the NAME-02 seed)

All rows below are the server's own bytes (`curl` of `DESCRIBE TABLE ... FORMAT JSONEachRow SETTINGS describe_include_subcolumns=1`), then what `ClickHouseFieldDescriptor.fromJsonRow` returns today. `[VERIFIED: live run on 26.9.19.9, this session]`.

DDL used (SQL text, so `\\` is one backslash; LF/tab/CR are SQL escapes):
`` `bs\\x` `` (backslash), `` `lit\\nb` `` (literal backslash then n), `` `lf\nb` ``, `` `tab\tb` ``, `` `cr\rb` ``, `` `sl/ash` ``, `` `dq"b` ``, `` `bt\`b` `` (backtick), `` `é日本` ``.

| Character | Server JSON `name` (root and sub-column) | Server JSON text inside `type` (backtick name) | Name from today's `fromJsonRow` (V2 lines) |
|-----------|-------------------------------------------|-------------------------------------------------|---------------------------------------------|
| backslash | `bs\\x` | `` `bs\\\\x` `` | `bs\\x` (two backslashes) |
| literal backslash-n | `lit\\nb` | `` `lit\\\\nb` `` | `lit\\nb` (two backslashes, n) |
| LF | `lf\nb` | `` `lf\\nb` `` | `lf\nb` (backslash, n: two characters) |
| tab | `tab\tb` | `` `tab\\tb` `` | `tab\tb` (backslash, t) |
| CR | `cr\rb` | `` `cr\\rb` `` | `cr\rb` (backslash, r) |
| slash | `sl\/ash` (server escapes `/`) | `` `sl\/ash` `` | `sl\/ash` (backslash kept) |
| double quote | `dq\"b` | `` `dq\"b` `` | **JSON parse failure, `describeTable` returns null** |
| backtick | `` bt`b `` | `` `bt\\`b` `` | `` bt`b `` (unaffected) |
| non-ASCII | raw UTF-8 `é日本` | raw UTF-8 | `é日本` (unaffected on V2) |

Observations that drive the design:

1. The `type` text carries two layers: the server SQL-escapes the identifier, then JSON-escapes the whole string. Today's `getType()` keeps both (the doubling returns the JSON literal's inner text). `[VERIFIED: Cap harness output, this session]`
2. `output_format_json_escape_forward_slashes` is on by default on this server, hence `\/`. `[VERIFIED: live row `{"name":"sl\/ash"...}`]`
3. `ClickHouseColumn.of` unescapes a quoted name by dropping the backslash and keeping the next character. With the proper SQL text `` `lf\nb` `` it returns the name `lfnb`, for tab `tabtb`, for CR `crrb` (measured: elements `lfnb`, `tabtb`, `crrb` do not match the sub-column names). With `` \\ `` it returns one backslash, with `` \` `` a backtick, with a raw LF/TAB/CR character inside the backticks it returns that character. `[VERIFIED: Cap2 harness, this session]`
4. Today the escaped-backtick name makes the declared type unparsable (`ClickHouseColumn.of` throws `IllegalArgumentException` on `` `bt\\`b` ``), so a Tuple with a backtick element name always takes the declared-nullability fallback WARN. After the rewrite it pairs. `[VERIFIED: bt.jsonl run, this session]`

### Client V1 differs from V2 today (new finding)

`describeTableV1` reads `response.records()` and passes `v.asString()` to the builder. Measured on the same table with the repo's own V1 code path: V1 hands over the row after a TabSeparated-style unescape, not the server's JSON. `[VERIFIED: V1Raw harness, this session]`

| Character | V1 `records()` line fragment | Consequence today on V1 |
|-----------|------------------------------|--------------------------|
| backslash | `"name":"bs\x"` | after the doubling the name is the raw `bs\x`; the type text is one layer thinner than V2's |
| literal backslash-n | `"name":"lit\nb"` | raw name `lit\nb` |
| LF | a real line feed inside the line | the second `replace("\n","\\n")` turns it back into an escape; raw LF name |
| tab | a real tab inside the string | Jackson fails (`Illegal unquoted character (CTRL-CHAR, code 9)`), table unmapped; CR is the same class |
| slash | `"name":"sl�ash"` | U+FFFD replaces the slash (lossy) |
| double quote | unescaped `"` inside the string | invalid JSON, table unmapped |
| non-ASCII | correct under UTF-8 and under `-Dfile.encoding=windows-1252` | no mojibake today |

So `fromJsonRow`'s two `replace` calls exist to serve V1's already-unescaped lines as much as V2's, and the NAME-03 requirement "the V1 describe path reads raw lines in UTF-8" is what makes both clients feed the same text to the decode. The raw stream form (`response.getInputStream()` wrapped in an explicit `InputStreamReader(..., StandardCharsets.UTF_8)`) returns byte-for-byte the V2 lines `[VERIFIED: V1Raw "raw getInputStream() view" matches the server rows, this session]`. The explicit charset is not cosmetic: the test JVM runs with `file.encoding=windows-1252` (`build.gradle.kts:225`: `systemProperty("file.encoding", "windows-1252") // run tests with different encoding`), so the default charset would mojibake non-ASCII names.

Also relevant for scope: the connector's own default is client V1: `ClickHouseSinkConfig.java:331` reads `this.clientVersion = props.getOrDefault(CLIENT_VERSION, "V1");`. `[VERIFIED: Read this session]` So a deployment that does not set `client_version=V2` runs the V1 describe path; the suite itself runs V2 only. One manual V1 run is the locked evidence (D-04); a single small V1 live pin in the style of `TableTest.extractCommentV1` (which already runs V1 in the suite) would be additive and is a planner option (see Open Question 3).

## Standard Stack

No new dependencies. Everything below is already on the classpath. `[VERIFIED: classpath printed from Gradle, this session]`

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.fasterxml.jackson.core:jackson-core` (`JsonFactory`, `JsonParser`) | as resolved by the repo's Gradle catalog (2.22.x seen on the test classpath) | Streaming read of the DESCRIBE row: decode `name`, slice other strings | Already used (`ObjectMapper` in `ClickHouseFieldDescriptor`); `currentTokenLocation()`/`currentLocation()` char offsets are exact for a `String` source |
| `com.clickhouse:clickhouse-data` (`ClickHouseColumn.of`) | 0.9.6 | Declared-type parse for nullability pairing | Existing dependency; its quoted-name unescape is lossy for `\n \t \r` (measured), so the input text is shaped by `normalise` |
| `com.clickhouse:clickhouse-client` (V1 `ClickHouseResponse.getInputStream()`) | 0.9.6 | Raw describe stream for V1 | Same client already used by `describeTableV1` |
| JaCoCo | 0.8.15 (`build.gradle.kts`, `jacoco { toolVersion }`) | Fork report | Unchanged |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Streaming decode with raw slices for non-name strings | Full Jackson decode of all fields, `normalise` accepts decoded text | Changes `getType()` for every multi-line Tuple (real LF instead of `\n`), which CONTEXT forbids and which would break pins such as `assertCapturedShapePairs`'s "discovery keeps the captured text verbatim" |
| Streaming decode | Re-escaping the decoded `type` with `JsonStringEncoder` | Not byte-identical: the server writes `\/`, Jackson's encoder does not; slice is exact |
| Rewriting `normalise` | Un-escaping parsed element names after `ClickHouseColumn.of` (key-side unescape) | Impossible for LF, tab, CR (the parser already dropped the backslash and returned `lfnb`), and the escaped-backtick type does not parse at all |

**Installation:** none.

## Package Legitimacy Audit

No external package is added or changed by this phase (Jackson streaming API, the ClickHouse client and JaCoCo are all existing dependencies; the Standard Stack table lists only those). `gsd-tools query package-legitimacy check` was therefore not run; there is nothing to verify and no `checkpoint:human-verify` is needed for an install.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
ClickHouse server
  DESCRIBE TABLE ... FORMAT JSONEachRow  (one JSON object per line; names JSON-escaped,
                                          type text = SQL-escaped identifiers, then JSON-escaped)
        |
        +-- client V2: QueryResponse.getInputStream() -> BufferedReader(UTF-8) -> lines
        +-- client V1: today response.records() (TSV-style re-decode, LOSSY)
        |              NAME-03: response.getInputStream() -> BufferedReader(UTF-8) -> lines  (same as V2)
        v
DescribeTableBuilder.addRow(line)            <- single entry for both clients (Phase 26)
        |
        v
ClickHouseFieldDescriptor.fromJsonRow(line)  <- THE decode site
        |   name                -> Jackson-decoded (RAW identifier)
        |   type, comment, ...  -> literal JSON inner text, unchanged (layered)
        v
descriptor.getName() == raw  ----------------------------------------------+
descriptor.getType() == layered text (unchanged)                           |
        |                                                                   |
        +--> Column.extractColumn(desc)                                     |
        |       Enum branch: extractEnumValues(layered text)  <- NAME-06    |
        |           escape-aware scan + JSON unescape + SQL unescape        |
        |           => keys are RAW labels                                  |
        |       (Tuple/Map/Array branches keep using the name only)         |
        |                                                                   |
        +--> declaredTypes[rootName] = layered type text                    |
        v                                                                   |
Table(...)  (upstream-identical; pairs sub-columns by name prefix)  <-------+
        v
DeclaredNullability.apply(root, layeredType)
        |  normalise(layered)  <- NAME-04 rewrite
        |     1. JSON-unescape the layer (\\ \" \/ \n \t \r \b \f \uXXXX)
        |     2. quote-aware pass: outside quotes LF -> space;
        |        inside quotes \n \t \r -> the characters; \\ \` \' left for the parser
        v
ClickHouseColumn.of(rootName, normalised)  => nested column names == RAW element names
        v
collectTuple: key = DeclaredNullability.elementKey(parsedElement, i)   (unchanged)
              field.getName() = "<root>.<raw>" ; pairing succeeds, no fallback WARN
        v
Kafka Struct / Map field named with the RAW identifier
  -> ClickHouseWriter looks up by Column.getName() (unchanged) -> RowBinary / JSON insert
```

### Recommended Project Structure

No new directory. Touch list (production):

```
src/main/java/com/clickhouse/kafka/connect/sink/db/
├── helper/ClickHouseFieldDescriptor.java   # fromJsonRow: names-only decode (NAME-03)
├── helper/ClickHouseHelperClient.java      # describeTableV1: raw-line UTF-8 read (NAME-03)
└── mapping/
    ├── DeclaredNullability.java            # normalise rewrite, Javadoc IN-01, null-name IN-02 (NAME-04)
    └── Column.java                         # extractEnumValues scanner (NAME-06)
build.gradle.kts                            # forkChangedClasses (DEBT-17, last edit)
.github/scripts/jacoco_summary.py           # duplicate class list (DEBT-17)
.planning/scripts/fork-anchors.txt          # rows move with the code
docs/FORK-DELTA.md, CHANGELOG.md, .planning/ROADMAP.md   # NAME-07, DEBT-17
```

`ParentShape.java` and `DescribeTableBuilder.java` need no edit: `ParentShape.parse` calls `DeclaredNullability.normalise`, and `declaresElement` compares `elementKey` to the sub-column segment, both of which become raw on both sides together. `Table.java` needs no edit (it pairs by `getName()` and `lastIndexOf(".")`).

### Pattern 1: Names-only decode in `fromJsonRow` (NAME-03)

**What:** Parse the row with `JsonFactory`/`JsonParser`. For field `name`, take the decoded text. For every other string field, take the literal inner text of the JSON string (slice `[tokenStart+1, tokenEnd-1)` of the input), which is exactly what the doubling-then-Jackson trick produced for inputs without `\"`. `is_subcolumn` stays a boolean (the server sends `0`/`1`).

**When to use:** The one decode site; V1 and V2 both reach it through `DescribeTableBuilder.addRow`.

**Verified properties of the prototype** `[VERIFIED: Proto2 harness, this session]`:
- Over all 272 committed fixture rows (`src/test/resources/describe-fixtures/*.jsonl`, 95 files incl. snapshots): zero differences against today's `fromJsonRow` for name, type, comment, default type, default expression and `isSubcolumn`.
- The `dq"b` Tuple row now parses (it throws today), and its `getType()` equals the server's JSON inner text.
- Lombok caveat for the executor: `ClickHouseFieldDescriptor` is `@Data @Builder @Jacksonized @JsonNaming(SnakeCaseStrategy)`; the repo's `lombok.config` has `lombok.jacksonized.jacksonVersion += 2`. Keep the class annotations (other code builds descriptors with `ClickHouseFieldDescriptor.builder()`), and keep the checked `JsonProcessingException` in the `fromJsonRow` signature, because `DescribeTableBuilder.addRow` and `describeTableV1` catch it.

**Example (prototype, not production code):**
```java
// Source: /tmp/gsd28/src/Proto2.java (prototype run in this session; ephemeral)
try (JsonParser p = FACTORY.createParser(json)) {
    p.nextToken();                                    // START_OBJECT
    while (p.nextToken() == JsonToken.FIELD_NAME) {
        String field = p.currentName();
        JsonToken token = p.nextToken();
        if (token == JsonToken.VALUE_STRING) {
            String value;
            if ("name".equals(field)) {
                value = p.getText();                  // decoded: the raw identifier
            } else {
                int start = (int) p.currentTokenLocation().getCharOffset();
                p.getText();                          // forces the token to be read to its end
                int end = (int) p.currentLocation().getCharOffset();
                value = json.substring(start + 1, end - 1);   // literal inner text, as before
            }
            // switch (field) -> builder.name / type / defaultType / ...
        } else if ("is_subcolumn".equals(field)) { /* 0/1 or true/false */ }
        else p.skipChildren();
    }
}
```

### Pattern 2: Rewritten `normalise` (NAME-04)

**What:** Input stays the layered type text exactly as `getType()` returns it. Output is text whose `ClickHouseColumn.of` parse yields raw element names.

1. JSON-unescape the layer leniently (total function, never throws): `\\ \" \/ \n \t \r \b \f \uXXXX`; an unknown or truncated escape is copied as is.
2. Quote-aware pass over backtick and single-quote quotes: outside quotes a line feed becomes a space (the server's multi-line Tuple form); inside quotes `\n`, `\t`, `\r` become the actual characters; any other backslash pair (`\\`, `` \` ``, `\'`) is copied for the parser, whose drop-the-backslash rule already yields the right character; a quote closes on its first unescaped occurrence.

**Verified:** Over the nine characters in `Tuple`, `Nullable(Tuple)`, `Array(Tuple)`, nested `Tuple(Tuple)` and a `Tuple` holding an Enum with all the labels, the parsed element names equal the sub-column names the server reports in every case (the `Map(String, Tuple)` comparison in the harness compared against the wrong path segment, the real `Map` pairing is exercised by the existing `w`/`m` wrapper cells and by the `n`/`t` write run). `[VERIFIED: Proto harness output `parsedElems=9 notInDescribeSubcols=[]` for t1/t2, 5/0 for t3, 2/0 for t5, 4/0 for t6, this session]`

**Consequences for existing pins (all changed pins, red-first):**
- The expected-string literals in `DeclaredNullabilityTest` (lines 248 to 346 region: the `assertCapturedShapePairs` cases and `normaliseIsTotalAndKeepsNonAsciiNames`) assert today's output, in which backslash runs inside quotes are kept doubled. They change by design: one layer fewer.
- `typesWithoutABackslashInsideQuotesNormaliseAsBefore` (six types without a backslash inside quotes) must stay green unchanged; it is the compatibility guard that the rewrite equals the plain replace for backslash-free types.
- `DeclaredNullabilityFixtures.BACKSLASH_N_ELEMENT = "a\\\\nb"` (two backslashes, the doubled name) and `REAL_NEWLINE_ELEMENT = "a\\nb"` (backslash, n) are the sub-column names fed to `row(...)`; after the decode they become the raw forms (one backslash and `n`; a real LF). The `*_RAW` type constants stay (the type text is unchanged), so they remain valid premises.
- `NullableTupleMatrixLiveTest` lines 826 to 842 and 902 use the doubled names as the Kafka field name and as the `getAllColumnsMap()` key (`"n.p\\\\nq.t"`); they change to the raw names in the same commit. Per DEBT-16 the premise lookup by the old name must fail loudly (`assertNotNull(element, elementKey)` already does).

### Pattern 3: V1 raw-line read (NAME-03)

**What:** `describeTableV1` replaces `for (ClickHouseRecord r : response.records())` / `v.asString()` with a `BufferedReader` over `response.getInputStream()` and `StandardCharsets.UTF_8`, the same loop as `describeTableV2`. Prefer one shared private helper that feeds a `DescribeTableBuilder` from an `InputStream`, so the loop is defined once (project rule: DRY) and the anchor counts below stay predictable.

**Anchors this touches (all in `fork-anchors.txt`, same commit):** `ClickHouseHelperClient.java` `StandardCharsets.UTF_8` expected `1` (HLP-04) and `InputStreamReader` expected `2` (HLP-04). With a shared helper both stay at 1 and 2 (the helper holds the only reader line); without one, UTF_8 becomes 2 and `InputStreamReader` 3. The `ClickHouseRecord` and `ClickHouseValue` imports go unused in V1 after the change (Spotless `removeUnusedImports`), so check they are not used elsewhere in the file before deleting them.

### Pattern 4: Enum label scanner (NAME-06, D-01)

**What:** `Column.extractEnumValues` today does `Utils.splitIgnoringQuotes(valueType.substring(indexOf("(")+1, indexOf(")")), ',')`, then `split("=", 2)` and `key.substring(1, key.length()-1)`. `[VERIFIED: Column.java:376-385, read this session]`

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

Measured on the unchanged tree `[VERIFIED: Desc harness on nm2/nm_apos, this session]`:
- Labels with an escaped character are keyed in the doubly escaped form: for the DDL labels `bs\x`, `lit\nb`, LF, tab, slash the keys are `bs\\\\x`, `lit\\\\nb`, `lf\\nb`, `tab\\tb`, `sl\/ash` (as printed by the harness, i.e. the JSON-layer text). A record carrying the raw label misses and `convertEnumValues` throws the Phase 27 `IllegalArgumentException`.
- A label with an apostrophe (`'a\'b' = 1, 'c' = 2`) makes `Utils.splitIgnoringQuotes` toggle its quote state on the escaped apostrophe, the split mis-groups, `Integer.parseInt("1, 'c' = 2")` throws `NumberFormatException`, and `describeTableV2` returns null: **the whole table is unmapped today**. `Utils.splitIgnoringQuotes` is used only by `Column.extractEnumValues` (grep), so the scanner can stay entirely inside `Column`.

**Recommended:** replace the split-and-substring with a single left-to-right scan inside `Column` that (1) applies the same JSON-layer unescape as `normalise` step 1, (2) reads quoted labels honouring backslash escapes (`\\`, `\'`, `\n`, `\t`, `\r` decoded to the characters), (3) reads `= <int>` after the closing quote, (4) stops at the closing `)` outside quotes. This also removes the latent `)`, `,` and `=` inside-a-label failures of the current code (not in the spiked list; pin one or two cheaply or record them in Known costs). The JSON-layer unescape and the quote-aware SQL unescape are shared with `normalise`: put them in one package-private place (see Open Question 2) so the two parsers cannot disagree.

**Verified end to end:** with the prototype decode but the unchanged `Column`, an Enum `e` with a CR/LF/tab/slash/dq label still keyed the layered text (so the raw-label record threw the lookup miss); the NAME-06 fix is what closes it. `[VERIFIED: Write harness raw run failed with `Failed to write column `e` ... IllegalArgumentException`, this session]`

### Anti-Patterns to Avoid

- **Decoding the type text.** Forbidden by CONTEXT and it changes `getType()` for every multi-line Tuple. Keep the slice.
- **Un-escaping parsed names (key-side only).** Cannot work for LF, tab, CR (the parser has already dropped the backslash) and cannot work for the escaped backtick (the layered text does not parse).
- **Changing `fromJsonRow` in a commit that does not also carry the `normalise` rewrite.** Between the two, every Tuple with a backslash-class name falls into the declared-nullability fallback and the double-flag defect returns.
- **Extending the existing `normalise` loop instead of rewriting it.** Its `run / 2` logic encodes the double layer; the new text has one layer.
- **Touching `Utils.splitIgnoringQuotes`.** It is shared fork-modified territory; the scanner belongs in `Column`.
- **Adding a dual-form alias or a doubled-form WARN.** Locked out (NAME-F1).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON string decoding of `name` | A manual `\uXXXX`/surrogate decoder for the name field | `JsonParser.getText()` | Correct for surrogate pairs and every standard escape |
| Locating a string token's source span | Regex over the line | `currentTokenLocation()` / `currentLocation()` char offsets | Correct for `\\"` sequences that defeat any regex; verified on all fixtures |
| Parsing the declared type | A custom Tuple/Enum grammar for nullability | `ClickHouseColumn.of` after `normalise` | Already the single source for nullability; only its input is shaped |
| Reading the V1 stream | A second bespoke loop | The shared describe-stream helper (or the V2 loop copied once) | One definition of the line loop; one anchor row |

**Key insight:** the only genuinely hand-written parts are `normalise` (a total, linear, quote-aware pass) and the Enum scanner; both are small, both must be total (never throw on odd input: `normalise` is on the high-risk nullability path, and a throw would unmap a table instead of falling back with a WARN).

## Runtime State Inventory

This is a behaviour fix in discovery, not a rename of stored data, but it changes the keys under which existing deployments match names, so the five categories are answered explicitly:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | Rows already stored by earlier releases for tables whose names contain the nine characters (a root column written `DEFAULT` because the raw-name field was ignored; Tuples written from doubled-form fields) | None in this phase; rows are not repaired. Phase 29's rollout note states it. |
| Live service config | Kafka producers/Debezium topics/SMT configs that emit the doubled field names (e.g. `a\\b` in a Struct) | Operators rename fields to the raw identifier before upgrading; recorded for Phase 29 (D-06). No in-repo action. |
| OS-registered state | None: verified, no scheduler/launchd/systemd registration embeds a column name | None |
| Secrets/env vars | None: no secret key or env var is keyed by a column name | None |
| Build artifacts | `.planning/scripts/fork-anchors.txt` rows (counts), the committed describe fixtures (backslash-free by construction, `DescribeFixtureCatalog` Javadoc: "Keys and column names are backslash-free by construction"), `src/test/resources/describe-fixtures/*` snapshots | Anchors move in the same commit as the code (below). Fixtures are unaffected because no committed fixture row has a backslash in a name or type; re-run `DescribeFixturesUnitTest` to confirm. |

**Exactly-once retry bytes (D-06):** after the upgrade, a batch retried across the version change is built from a different Table: a root column that was `DEFAULT`-filled is now written from the record, and enum labels are keyed differently. The BEFORE/AFTER invariant therefore needs a clean stop in both directions with the rename as its precondition. Phase 28 records this for Phase 29; it does not edit `Processing` or `KeeperStateProvider`.

## Common Pitfalls

### Pitfall 1: Quote-state tracking after the JSON-unescape
**What goes wrong:** After unescaping the JSON layer, a `\"` has become `"`, and a `\\`+backtick has become backslash+backtick. A second pass that toggles on any backtick will close the quote early on the escaped backtick.
**How to avoid:** In the SQL pass, inside a quote a backslash always consumes the next character (so `` \` `` and `\'` never close). Pin with the WR-01 case (below) plus an escaped single quote inside an Enum label.
**Warning signs:** A Tuple with a backtick or apostrophe name takes the fallback WARN.

### Pitfall 2: `ClickHouseColumn.of` drops the backslash of `\n`
**What goes wrong:** Feeding it the proper SQL text pairs everything except LF, tab and CR (`lfnb`).
**How to avoid:** Convert `\n \t \r` inside quotes to the characters before the call (the parser keeps a raw control character inside backticks, verified). Nothing else needs conversion.
**Warning signs:** COLS-13 twins red only for LF/tab/CR.

### Pitfall 3: `fromJsonRow` decode without the V1 raw read
**What goes wrong:** V1 hands over unescaped lines (real tab, unescaped quote, U+FFFD for the slash). A decode that assumes JSON-escaped input then fails or loses data on V1.
**How to avoid:** Land the V1 raw-line read in the same commit as the decode (it is part of NAME-03) and run V1 manually once.
**Warning signs:** `describeTableV1` returns null for a tab/CR/quote/slash table.

### Pitfall 4: Jackson stream constraints and exception text
**What goes wrong:** The streaming parser must not leak the row into an exception message (a row carries a column default expression and a comment). Jackson 2.16+ redacts the source (`[Source: REDACTED ...]` was seen in the harness), but a hand-built `JsonParseException(p, "...")` must not embed the line.
**How to avoid:** Throw/propagate only parser-generated exceptions or a fixed message. Keep the declared `JsonProcessingException` so callers that catch it keep working.

### Pitfall 5: Mutating anchor counts silently
**What goes wrong:** `fork-anchors.txt` is evaluated at every commit in range; a count that drifts fails the gate for that commit.
**How to avoid:** See the anchor table; edit `fork-anchors.txt` in the same commit as the code row by row.

### Pitfall 6: The `jacoco_summary.py` duplicate
**What goes wrong:** Editing only `build.gradle.kts` leaves the CI step-summary table unchanged because the script iterates its own `FORK_CLASSES_IN_ORDER` list (15 entries, comment "The 15 fork-changed base class names"). The XML report would list the classes, the summary would not.
**How to avoid:** Edit both files in the DEBT-17 commit and update the "15" comments.

### Pitfall 7: Test source encoding
**What goes wrong:** Non-ASCII literals in test sources depend on the compiler's source encoding while the test JVM default is windows-1252.
**How to avoid:** Write non-ASCII characters as `é`-style Java escapes, as `DeclaredNullabilityTest.normaliseIsTotalAndKeepsNonAsciiNames` already does; compare read-back values after a typed SQL read.

## Code Examples

### NAME-01 pin: the WR-01 literal, verified red under the mutation
```java
// Source: 23-REVIEW.md WR-01 fix text, run against the CURRENT normalise in this session
// (java -cp ... N1, /tmp/gsd28/src/N1.java)
String raw = "Tuple(\\n    `a\\\\`\\\\\\\\nb` Int32,\\n    d String)";
// runtime text: Tuple(\n    `a\\`\\\\nb` Int32,\n    d String)
assertEquals("Tuple(     `a\\\\`\\\\\\\\nb` Int32,     d String)", DeclaredNullability.normalise(raw));
// current code:   Tuple(     `a\\`\\\\nb` Int32,     d String)      (PASSES)
// always-close mutation (`if (escaped == quote)` without the half-run test):
//                 Tuple(     `a\\`\\\\nb` Int32,\n    d String)       (the second \n is left; RED)
```
`[VERIFIED: both outputs printed by the N1 harness, this session]`. The mutation targets `DeclaredNullability.java:102`: `if (escaped == quote && (run / 2) % 2 == 0) {`. WR-01 also asks for the outside-quotes `run == 1` guard (two or more backslashes then `n` outside quotes) and a captured real server literal in the evidence file. Because NAME-04 rewrites `normalise`, make the NAME-01 pin behavioural as well as literal: assert that `apply` pairs a Tuple whose element is named with an escaped backtick **without** a fallback WARN. That behaviour is red today (the type does not parse) and becomes green with the rewrite, so it is a red-first pin for NAME-04, while the string-literal pin is the NAME-01 pin on the current code and is then a changed pin.

### Prototype `normalise` (shape only)
```java
// Source: /tmp/gsd28/src/Proto.java, run against nm3 in this session (ephemeral prototype)
static String normalise(String layered) {
    String s = jsonUnescapeLenient(layered);          // \\ \" \/ \n \t \r \b \f \uXXXX; unknown kept
    StringBuilder out = new StringBuilder(s.length());
    char quote = 0;
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (quote != 0) {
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'n') out.append('\n'); else if (n == 't') out.append('\t');
                else if (n == 'r') out.append('\r'); else out.append('\\').append(n);
            } else { out.append(c); if (c == quote) quote = 0; }
        } else if (c == '`' || c == '\'') { quote = c; out.append(c); }
        else out.append(c == '\n' ? ' ' : c);
    }
    return out.toString();
}
```
Totality: no exception path; an unterminated quote leaves the remaining text as is (matches the existing unterminated-quote pin).

### End-to-end write evidence (prototype)
`[VERIFIED: Write harness, this session]` Table in a scratch database with root columns `DEFAULT 99`, a `Tuple` and a `Nullable(Tuple)` named with the nine characters (apostrophe omitted), `put` of one `Struct` record using the raw names through `ClickHouseSinkTask`, client V2. Typed read-back via `FORMAT JSONEachRow`: root values 10..18, `Tuple` 100..108, `Nullable(Tuple)` 200..204, no root `DEFAULT 99` left. A second run with doubled-form field names failed in `put` (the Tuple element is missing: loud). No `ClickHouseWriter` change was needed.

## Document and anchor edits (NAME-07, DEBT-17)

### `fork-anchors.txt` rows the code changes will move
`[VERIFIED: file read this session, lines 28-31, 85-87, 93]`

| Row | Current count | Moves when |
|-----|---------------|------------|
| `ClickHouseHelperClient.java` `StandardCharsets.UTF_8` (HLP-04) | 1 | V1 gains an explicit-UTF-8 reader without a shared helper (becomes 2) |
| `ClickHouseHelperClient.java` `InputStreamReader` (HLP-04) | 2 | same (becomes 3) |
| `DeclaredNullability.java` `private static final char BACKTICK` (COLS-13) | 1 | the rewrite renames or removes the constant |
| `DeclaredNullability.java` `declaredType.replace(ESCAPED_NEWLINE, SPACE)` (COLS-13) | 0 | stays 0; keep it 0 |
| `DeclaredNullability.java` `elementKey(` (DESC-08) | 2 | only if the helper is edited or gains a call site |
| `Column.java` `ENUM_LABEL_NOT_FOUND_FORMAT` / `NO_ENUM_CONTENT_FORMAT` (WRT-18) | 2 / 2 | must stay 2 each; do not touch those constants |
| `Column.java` `return enumValues.get(value);` (WRT-18) | 0 | stays 0 |
| `build.gradle.kts` `jacocoForkReport` (BLD-03) | 3 | does not move when only list entries are added; do not rename the task or its description |

Add rows for the new code in the same commit (for example a 0-count row pinning that `.replace("\\", "\\\\")` stays gone from `ClickHouseFieldDescriptor.java`, and a count row for the V1 raw reader). The gate: `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD` (from the Phase 27 plans; it takes about 25 seconds). `[CITED: .planning/phases/27-writer-failure-path-test-debt/27-*-PLAN.md]` The script and its self-test are read-only.

### DEBT-17 class list
`[VERIFIED: git diff --name-status v1.7.0 HEAD -- src/main, run this session]` Added (A) classes: `MetadataFreeJsonWriter`, `DescribeTableBuilder`, `DeclaredNullability`, `ParentShape`, `WithheldMessageException`, plus the five transforms (already listed). Since the tail baseline `38265d26`, Phases 25 to 27 added `DescribeTableBuilder` and `ParentShape` only. `ClickHouseFieldDescriptor` is upstream-identical today (not in the diff) and becomes a fork-modified class with this phase. `build.gradle.kts` currently names none of the five and carries the comment that `ClickHouseFieldDescriptor` "can never be pulled in" (the comment must change if it is added). `DeclaredNullability` has a nested `Assignment` record and `DescribeTableBuilder` a nested `RowDecision` enum, so each needs its `\$*.class` glob entry like the existing entries.

Recommended list additions (diff-derived, per CONTEXT discretion): `DeclaredNullability`, `ParentShape`, `DescribeTableBuilder`, `WithheldMessageException`, `ClickHouseFieldDescriptor`, and any helper class Phase 28 adds. `MetadataFreeJsonWriter` is an older fork class missing from the list; adding it satisfies the goal text "lists every fork class"; this is Open Question 4.

Also edit in the same commit: `.github/scripts/jacoco_summary.py` (`FORK_CLASSES_IN_ORDER`, comment "The 15 ..."), and the FORK-DELTA row at line 65 (it says "the 15-class `jacocoForkReport`; `DeclaredNullability` and `WithheldMessageException` are not in its 15-class list, a gap that is recorded here and not fixed in `v1.7.0_gg3`"). `DescribeFixturesUnitTest` reads `build.gradle.kts` (task guard), so `build.gradle.kts` edits re-run it by Gradle input declaration (`inputs.file(layout.projectDirectory.file("build.gradle.kts"))`, line 227). Verify the fork report shows the classes: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` finalizes `jacocoForkReport`; read `build/reports/jacoco/fork/jacocoForkReport.xml` for the class names.

### NAME-07 document list (same commit as the fix)
- `docs/FORK-DELTA.md`: new row for the names-only decode, rewritten `normalise`, V1 raw read and Enum scanner; the existing row 49 ("The describe-table reader decodes the response with an explicit `StandardCharsets.UTF_8`", HLP-04) now covers V1; retire Known costs item 11 (line 182) and fix the cross-reference sentence at line 218 that names Known costs items 10 and 11 as knowingly open; jacoco row 65.
- `CHANGELOG.md`: one line in the `# 1.7.0_gg4, unreleased` section under `## Bug Fixes`.
- `.planning/ROADMAP.md`: mark backlog Phase 999.5 delivered (line 512 entry).
- Do not write `docs/ROLLOUT-v1.7.0_gg4.md` (Phase 29 only, D-06). `VERSION` stays `v1.7.0_gg3` (anchor `VERSION v1.7.0_gg3 1 BLD-07`).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Double every backslash, then Jackson-parse the whole row | Streaming parse: decode `name`; slice other strings | This phase | Names raw; `"` no longer unmaps the table; type text byte-identical |
| V1 reads `response.records()` | V1 reads the raw stream in UTF-8 | This phase | V1 and V2 feed the same lines |
| `normalise` keeps backslash runs doubled and tracks them with `run / 2` | JSON-unescape then quote-aware SQL pass | This phase | Parser returns raw element names, backtick names pair |
| Enum labels split and trimmed from the layered text | Escape-aware scan with JSON and SQL unescape | This phase | Raw-label records match; apostrophe labels no longer unmap the table |

**Deprecated/outdated:** `ClickHouseFieldDescriptor.fromJsonRow`'s two `replace` calls (the second served V1's real line feeds).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | all live tests, NAME-02 spike | ✓ | colima `vz` running (`colima status -p vz`) | — |
| ClickHouse `latest` image | live tests | ✓ (cached locally) | server 26.9.19.9, image id `bdb5c911a23d` | `docker pull` |
| JDK | Gradle toolchain (17) | ✓ (shell java reported 21; Gradle resolves its own) | — | `-PjavaVersion=21` escape if OpenJ9 crashes under swap |
| Gradle wrapper | build and tests | ✓ (`--offline` run succeeded for the classpath print) | project wrapper | — |
| `yq` | CI-matrix discovery only | not needed here | — | — |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none.

Note for the executor: a V1 manual run is `CLIENT_VERSION=V1` (the test helper reads that environment variable), distinct from the connector's own `client_version` property whose default is `V1` (`ClickHouseSinkConfig.java:331`).

## Security Domain

`security_enforcement` is enabled (config absent = enabled; `security_asvs_level: 1`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | The DESCRIBE row is server-originated but carries admin-chosen identifiers and free text (`comment`, default expression); parse with Jackson, no regex over untrusted text, `normalise` and the Enum scanner total and linear |
| V6 Cryptography | no | — |
| V7 Error handling and logging | yes | Metadata-only logs; no row text or record value in exception messages |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Row content echoed into exception text (comment, default expression) | Information disclosure | Rely on Jackson's redacted locations; do not embed the line in any hand-built exception; keep `DeclaredNullability`'s class-name-only fallback WARN |
| Log injection through names containing LF/CR in WARN/DEBUG lines | Tampering / repudiation | Names are DDL-defined metadata, already logged before this phase; the `normalise` output is only logged inside the existing fallback WARN (raw `declaredType`, unchanged). Do not add new log lines that print the decoded name verbatim at INFO; if a new line is needed, log at DEBUG as the existing builder lines do |
| Unterminated quote / adversarial type text causing non-termination | Denial of service | Single linear pass, no recursion, no backtracking; unterminated quote leaves the remainder unchanged (existing pin) |
| Data written under the wrong column silently | Tampering | The change fixes a silent drop (root column `DEFAULT`); the doubled-form pin keeps it visible for Tuple (loud) |
| Unescaped table name in `DESCRIBE TABLE `%s`.`%s`` | Tampering | Pre-existing (`ClickHouseHelperClient.java:491`, `:522`), table names come from `system.tables`; out of this phase's scope, mention only |

No new network surface, no new secret, no new dependency. Existing `13-SECURITY` / `18-SECURITY` value-free rules apply to the Enum lookup miss text (column only, never the label).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The server escapes only `\ " / \n \t \r` and the quote character in identifiers (no `\xHH`, `\0`, `\b`, `\f` handling needed for the nine spiked characters). NUL, backspace and form-feed in names are outside the phase's character list and were not measured. | Pattern 2 | A name with one of those would pair incorrectly; it falls back with the existing WARN, so the failure is not silent, but it is unpinned |
| A2 | An Enum label containing `)`, `,` or `=` is not in the phase's list; the recommended scanner fixes them as a by-product, but they were not measured on the unchanged tree. | Pattern 4 | If the planner keeps a minimal unescape instead of a scanner these stay broken; record in Known costs |
| A3 | Names containing a `.` are out of scope; `Table.handleNonRoot`, `DescribeTableBuilder` and the writer split on the last dot. | Pattern 1/2 | Not affected by this phase; not measured |
| A4 | The Jackson version resolved by Gradle at execution time equals the one on the test classpath printed here (`jackson-core` 2.22.x); `.claude/CLAUDE.md` lists 2.21.0, so the doc is stale or the catalog moved. | Standard Stack | `currentTokenLocation()` has been stable across 2.x; low risk, confirm with `./gradlew dependencies` |
| A5 | A single V1 live pin in the suite is acceptable under the COV-F1 deferral (extractCommentV1 sets the precedent). | Open Question 3 | If the user reads COV-F1 as "no V1 live tests", use only the manual run |

## Open Questions

1. **Does the scope guard D-02 permit a shared helper outside `Column`?**
   - What we know: the Enum scanner and `normalise` both need the same JSON-layer unescape; `Column` and `DeclaredNullability` are in the same package, so a package-private static in one of them or a small new package-private class would serve.
   - What's unclear: CONTEXT says the fix is "inside `Column.extractEnumValues` only" and that a fix reaching beyond `Column`'s enum-label parsing stops for a user decision. A shared helper is an implementation detail but arguably a reach.
   - Recommendation: have `Column` call a package-private static on `DeclaredNullability` (no new file, no behaviour outside label parsing), or duplicate the 15-line JSON unescape in `Column` and accept the DRY cost. Either keeps D-02 satisfied; confirm with the user only if a new class is preferred.

2. **Apostrophe in an Enum label.**
   - What we know: unmaps the table today; the scanner fixes it; the label is an "escaped character" (SQL `\'`), so NAME-06 covers it even though the goal's character list does not name it.
   - What's unclear: whether the user wants it pinned live.
   - Recommendation: pin it (one live row), because it converts a table-unmapped failure into a working mapping.

3. **A live V1 pin in the suite versus the manual run only.**
   - What we know: V1 is the connector's default (`ClickHouseSinkConfig.java:331`); `TableTest.extractCommentV1` already runs V1 live in the suite; D-04 locks one manual V1 run.
   - Recommendation: add one small V1 live describe pin (name matrix subset including tab and double quote, which fail on V1 today) in the `extractCommentV1` style, and keep the manual full-matrix run as the evidence of record.

4. **Is `MetadataFreeJsonWriter` part of "every fork class"?**
   - What we know: it is an added fork class (Phase 13 era) missing from the list; DEBT-17's text names this milestone's classes; the ROADMAP goal says "lists every fork class".
   - Recommendation: include it (one entry plus glob), and say so in the FORK-DELTA row.

5. **`Map(K, Tuple)` pairing cell.**
   - What we know: the existing live cell `m Map(String, Tuple(`a\\nb` ...))` in `describeReturnsTheCapturedBackslashNEscapingAndPairs` covers pairing; the end-to-end write evidence here did not include a Map-value Tuple record.
   - Recommendation: D-04's matrix includes Map-value Tuple; have the spike write one record into it and read back typed, since the writer's Map-of-Tuple route is the only shape not exercised end to end in this research.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`. Test-planning facts the planner still needs:

- Docker-free tests (untagged) run in `testUnit`; live tests extend `ClickHouseBase` (tag `clickhouse`). Quick command: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` (cache-busted gate per CONTEXT). Full suite: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test`. `testUnit` disables JaCoCo.
- Reusable helpers: `NullableRoundTrip` (`createTable`, `putAndStop`, `selectAndAssert` with typed `Cell`s, `describeRows`), `LogCapture.attach(DeclaredNullability.class)` for WARN assertions, `DeclaredNullabilityFixtures` (`row(...)`, `*_RAW` type constants), `DescribeFixtureCatalog` (Docker-free fixtures; committed rows are backslash-free, so name-matrix rows would be a new committed `.jsonl`). The raw rows captured in this research (nm3/nm4) can be committed as a Docker-free fixture by the spike plan.
- Red-first evidence per changed pin: NAME-01 under the always-close mutation (`DeclaredNullability.java:102`); NAME-03/04 under "decode without the normalise rewrite" (COLS-13 twins red for LF, tab, CR); NAME-06 under the unchanged `extractEnumValues`.
- Existing tests that must change in the NAME-03/04 commit (changed pins, not deletions): `DeclaredNullabilityTest` expected-normalised literals (lines ~248-346) and the `BACKSLASH_N_ELEMENT`/`REAL_NEWLINE_ELEMENT` fixture constants; `NullableTupleMatrixLiveTest` (lines ~826-842, ~902); any `DescribeTableBuilderUnitTest`/`ClickHouseHelperClientForkUnitTest` rows that feed a doubled name. Find them by running `testUnit` on the unchanged tree with the decode applied and reading the red set.
- Wave-0 style gap: no Docker-free test exists for "a row with a double quote in a name or type parses"; add one beside the `fromJsonRow` change.

## Sources

### Primary (HIGH confidence)
- Live ClickHouse `26.9.19.9`, repo classes compiled outside the repo, harness sources in `/tmp/gsd28/src` (Cap, Cap2, Desc, V1Raw, Proto, Proto2, N1, Write) — all escaping tables, the V1/V2 comparison, the apostrophe crash, the pairing and the write evidence.
- Repo files read this session: `ClickHouseFieldDescriptor.java`, `DeclaredNullability.java`, `DescribeTableBuilder.java`, `ParentShape.java`, `Column.java` (lines 90-440, 520-550), `ClickHouseHelperClient.java` (440-560), `Table.java` (20-250), `ClickHouseSinkConfig.java:329-332`, `build.gradle.kts:218-227`, `fork-anchors.txt`, `DeclaredNullabilityTest.java:200-360`, `NullableTupleMatrixLiveTest.java:80-140,720-910`, `DeclaredNullabilityFixtures.java:1-100`.
- `23-REVIEW.md` WR-01, IN-01, IN-02; `23-EVIDENCE.md` R-02.
- `docs/FORK-DELTA.md` rows 49, 52, 65 and Known costs item 11; `CHANGELOG.md` top section.

### Secondary (MEDIUM confidence)
- `.github/scripts/jacoco_summary.py` (class list duplicate), `.planning/scripts/verify-merge-invariants.sh` header, Phase 27 plans for the gate invocation.
- `git diff --name-status v1.7.0 HEAD -- src/main` and `38265d26..HEAD` for the fork-class list.

### Tertiary (LOW confidence)
- None used.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependency; classpath printed from Gradle.
- Architecture: HIGH for the three-part decode (prototype landed a record at root, Tuple and Nullable(Tuple)); MEDIUM for exact production code shape and the Map-value Tuple cell.
- Pitfalls: HIGH — each was reproduced (lossy `\n` unescape, unmapped table on apostrophe label and double quote, V1 divergence, duplicate coverage list).

**Research date:** 2026-10-23
**Valid until:** 2026-11-22 for the design; the live escaping tables are tied to ClickHouse `26.9.19.9` and should be re-captured by the NAME-02 spike if `latest` moves.
