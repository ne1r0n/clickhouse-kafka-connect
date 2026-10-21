# Phase 26: Size-Named Tuple Elements in Discovery - Research

**Researched:** 2026-10-21
**Domain:** ClickHouse `DESCRIBE TABLE ... describe_include_subcolumns=1` row classification inside a Kafka Connect sink (Java 17, Gradle 9.8.1, JUnit 5, Testcontainers); fork-owned discovery code beside upstream-identical `Table.java`
**Confidence:** HIGH for the classifier rule and the code sites (rows measured live this session, source read this session); MEDIUM for the harness wiring (precedent read, not run) and for the carrier choice (design, not yet built)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Locked by ROADMAP, REQUIREMENTS, STATE and the user's 2026-10-21 scope decisions, not re-opened here:**

*Scope and order*
- Fix both drop sites, including `size<digits>` under an Array parent in `Table.java` (user, 2026-10-21).
- Order inside the phase: (1) the live DESCRIBE spike on `latest`; (2) the behaviour-preserving extraction of the describe loop, its own commit, green on `testUnit` and the full suite before the fix; (3) the classifier at both sites, with the cascade guard and every DESC-10 document edit, in one commit; (4) the live round-trip; (5) the phase gate.
- The spike confirms or denies the structural-then-declared double row for `Array(Tuple(size0 ...))` and states the classifier rule before any `src/main` change. The plan for the classifier follows the spike.
- `Table.java` is adopted wholesale from upstream; narrow its change to "the child is not a declared element" and record the fork delta.

*Classifier rule*
- Classify on the parent's raw declared type, its array depth and the row's own `type`. Never on occurrence order or a closed list of names. Never on "the parent is a Tuple" alone, which is wrong for `Array(Tuple)`, where `e.size0` is the offsets row.
- A structural twin never enters the skip set (`skippedCols`), so a declared element's own children (`e.size0.a`) survive.
- The one public element-key helper this phase adds to `DeclaredNullability` is the single definition of a Tuple element's name (`collectTuple`'s key: `getColumnName()`, or `String.valueOf(i + 1)` for an unnamed element). Phase 28 reuses it; this phase does not change `normalise` or `fromJsonRow`.
- New pin names are backslash-free, so they survive Phase 28.

*Records and gates*
- DESC-10 lands in the fix commit: FORK-DELTA rows for `ClickHouseHelperClient` and `Table.java`, `fork-anchors.txt` rows, one CHANGELOG line, Known costs item 10 retired, backlog 999.4 marked delivered. The anchors invariant passes at every commit.
- One manual `CLIENT_VERSION=V1` run of the helper live tests is recorded.
- The D-04 pin flips: no unpaired-element WARN fires for the affected tables, live and Docker-free.
- Rollout facts for Phase 29 are recorded as they land: an upgrade needs no clean stop (failing inserts stored nothing); a rollback to gg3 needs a clean stop first; short writes that happened to parse are a data tail that nothing repairs. The DESCRIBE Column list is the RowBinary wire format, so the fix changes the bytes of a retried batch for an affected table.
- Phase gate: cache-busted `testUnit`, a full suite on `latest` x V2, and the anchors invariant. Standing merge gate: `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD`, with the self-test. Baseline tag `baseline-v1.7.0_gg4` = `aa450709`.
- Every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`; a two-run gate uses `--rerun`. Type coverage asserts typed SQL read-back, never `countRows`. Snapshot client-v2 `GenericRecord` rows into a `Map` during iteration. Sink-task tests create their tables before `start()`. No Mockito inline-mock of `ClickHouseHelperClient`. Spotless is baseline-equal.
- Existing tests: add rows, never delete or weaken; a flip of an existing assertion needs seen-red evidence (the D-04 flip is the one sanctioned flip, see D-10).
- Nothing is pushed. `origin/custom_build` stays frozen at `69cd680b`. Dependabot PRs #4 and #10 stay open and unmerged.

*Builder shape and home (DESC-07)*
- **D-01:** The shared DESCRIBE-row builder is a **new fork-owned class in `sink.db.helper`** (working name `DescribeTableBuilder`; the research calls it `DescribedTableBuilder`, either name is fine), the same shape as `DeclaredNullability`: the fork logic lives outside upstream's method bodies, and `describeTableV1`, `describeTableV2` and the unit test all call it. The next upstream sync conflicts only on the two call sites. One FORK-DELTA row and one anchor row on the call. Reversibility: reversible - inlining the class back into the two methods restores the pre-extraction text.
- **D-02:** The builder consumes **one raw JSON line per row** (`addRow(String jsonLine)`), parsing with `ClickHouseFieldDescriptor.fromJsonRow` inside. V1 passes `v.asString()`, V2 passes `br.readLine()`; both methods shrink to query, stream, feed, `build()`. The unit table feeds captured JSONL rows unchanged, so the Docker-free pin exercises `fromJsonRow` too, and Phase 28 rewrites `fromJsonRow` against this one entry point. The alias/materialized/ephemeral branch, the parent-skipped branch, `numColumns` counting, `declaredTypes` capture, the unmappable-column `null` result and the `.inner` early return keep their current behaviour (the extraction is behaviour-preserving by requirement).

*Classifier fail mode*
- **D-03:** When a size-named sub-column row arrives and the parent's declared type cannot be parsed into an element list (`ClickHouseColumn.of` throws, or no raw type is known for that parent), the classifier **fails closed to the status quo**: the row is treated as structural and dropped. An affected table keeps failing exactly as on gg3, and no structural row can leak into the Column list. Rejected: keep the row; refuse the table.
- **D-04:** On that fail-closed path the classifier logs **one distinct metadata-only WARN** at the drop site, naming database, table, column (parent) and raw type and saying the declared type could not be parsed. `DeclaredNullability`'s COLS-14 unpaired-element WARN still fires for the same table, so the operator sees cause and symptom. No record data, no element values. The WARN text is new rollout-note material for Phase 29. Rejected: reuse the unpaired WARN only; DEBUG only.

*Fixture provenance for the unit table (DESC-06, DESC-08)*
- **D-05:** The "captured real DESCRIBE rows" are **committed JSONL resources under `src/test/resources`**, one file per fixture table, holding the raw `DESCRIBE TABLE ... FORMAT JSONEachRow` lines with `describe_include_subcolumns=1`, with the server version recorded (header comment or sidecar, planner's call). The DESC-08 unit table loads them and feeds each line to the builder (D-02). Inline Java strings are not the primary fixture form any more; today's R-01 string arrays stay where they are (the D-10 flip keeps them).
- **D-06:** The resources are produced by a **committed capture harness in the test tree** (a JavaExec task or a `main()` class, `ConfluentArchiveCheck`/`verifyConfluentArchive` precedent): it starts the Testcontainers ClickHouse, creates every fixture table from a **committed DDL list** (the E-15 13 affected, 19 look-alikes, `s4_o01`, and the five new shapes), runs the DESCRIBE and overwrites the JSONL files, printing the server version. The DESC-06 spike record is its transcript plus the committed files; anyone re-captures with one documented Gradle command. The DDL list is the single source for the capture, the drift premise (D-07) and the live round-trip (D-08).
- **D-07:** A **live drift premise** guards the resources: one Docker-backed test creates each fixture from the committed DDL, runs DESCRIBE and asserts the rows equal the committed JSONL line for line (the COLS-13 `*_RAW` premise precedent). When a newer `latest` image changes a DESCRIBE shape, the suite names the table that drifted instead of the unit pin silently testing stale rows. It runs in the full suite, not in `testUnit`.

*Live round-trip depth (DESC-09)*
- **D-08:** The live round-trip inserts into and reads back **every E-15 table: the 13 affected shapes, the 19 look-alikes and `s4_o01`**. Affected tables must now store and match; look-alikes and `s4_o01` must still store and match, so the classifier is pinned both ways (no over-drop, no new leak). The five new spike shapes join the matrix when the spike shows they insert.
- **D-09:** The round-trip **pins the R-01 crafted value**: one extra record on the `s4_a01` shape (`Tuple(size UInt32, v String)`, the R-01 table was `Tuple(size UInt32, name String)`) whose String is `"abc" + U+0003 + "xyz"`, the bytes that stored `(1667391751,'xyz')` on gg3. Typed read-back must equal the record. This is the measurement behind the Phase 29 "unrepaired data tail" rollout fact.
- **D-10:** The D-04 pin `sizeNamedTupleElementDroppedByDiscoveryIsNamedInOneWarning` is **renamed and inverted in the fix commit**: the method name and `@DisplayName` state the new truth (the element becomes a Column, zero WARNs), the R-01 rows and the `apply() == true` assertion are kept, the "known defect 999.4" comment goes, and the renamed test is seen red on the pre-fix tree first so the flip is proven to come from the fix. Nothing is deleted or weakened; it asserts more. The test now drives the builder (D-01), not `discoverAsDescribeTableV2`.
- **D-11:** The live round-trip is a **new LiveTest class beside `NullableTupleMatrixLiveTest`** (the research's name `TupleSizeNamedElementRoundTripLiveTest` is fine), extending `ClickHouseBase`, creating the E-15 tables before `start()`, driving `put()` through `ClickHouseSinkTask` on the default RowBinary route with client V2, reading back with typed SQL, and asserting **zero unpaired-element WARNs** for the affected tables (the live half of the D-04 flip, via `LogCapture`). `ClickHouseHelperClientForkLiveTest` keeps its describe pins and stays the class the manual V1 run covers.

### Claude's Discretion
- The builder's internal API beyond `addRow(String)`/`build()`: how the unmappable-column `null` result and the `.inner` early return are expressed; how it tracks the raw type of every kept row (not only roots) so the classifier knows a nested parent's declared type; per-pass caching of parsed parent types.
- How the classifier's verdict reaches `Table.updateParent` so that `Table.java`'s change stays "the child is not a declared element" (for example a flag the builder sets on the child `Column`), with the anchor and FORK-DELTA rows that pin it.
- The exact signature of the public element-key helper on `DeclaredNullability`, provided `collectTuple` uses it too.
- Which unit-test helpers (`discover`, `discoverAsDescribeTableV2`) are replaced by the builder and which stay for pre-skipped-row cases.
- The fixture file naming, the DDL list's home (test resource vs Java constant), the server-version header format, the Gradle task name for the capture, and the WARN wording (metadata-only, column-naming, consistent with `FALLBACK_WARNING`).
- The plan split and commit order inside the fixed five-step order; the evidence file layout (Phase 23/24 precedent: one phase evidence file with the spike transcript, the red-first runs, the V1 manual run, the gate output); whether to rehearse in a throwaway worktree.
- DEBUG-level logging when the classifier keeps a size-named declared element.

### Deferred Ideas (OUT OF SCOPE)
- Client V1 coverage of the describe path in CI (a V1/V2 Table-parity live test, or a V1 run in `tests.yaml`): already listed as future requirement COV-F1; this phase records one manual V1 run only.
- A metadata-only WARN when a declared element is kept that an earlier release would have dropped (an upgrade hint): not requested; the CHANGELOG line and the rollout note carry that information.
- The `Table.java` upstream contribution (offering the "declared element" rule upstream): outside this fork's phases.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DESC-06 | Live DESCRIBE spike on `latest`: E-15 matrix + five new shapes, confirm/deny the double row, state the classifier rule before any code change | All five shapes and 18 more were measured this session on `26.9.19.9` (see "Measured DESCRIBE rows"); the double row is **confirmed**; the rule and a decision table are in "Classifier rule". The committed harness (section "Capture harness") makes the spike a repeatable record. |
| DESC-07 | One fork-owned builder shared by V1/V2, behaviour-preserving, green first; 999.4 pin drives it; one manual V1 run | Exact current loop text and line ranges, the builder API, anchor-count moves, and the existing V1 coverage (`TableTest.extractCommentV1`, `ClickHouseWriterTest` V1 rows) are in "Code sites" and "Extraction". |
| DESC-08 | Declared `size`/`size0`/`size<digits>` becomes a Column at both drop sites; structural rows still dropped; structural twin never enters the skip set; Docker-free unit table of captured rows | Decision table, the Column-flag carrier for `Table.updateParent`, the cascade guard, and the fixture/unit-table design. |
| DESC-09 | Live round-trip for every E-15 affected shape; DESC-05 regression stays green; D-04 pin flips | Reusable helpers (`NullableRoundTrip`, `RowSnapshots`, `LogCapture`) and the package placement that lets the new class reuse them. |
| DESC-10 | FORK-DELTA rows, anchors, CHANGELOG line, Known costs item 10 retired, backlog 999.4 delivered, same commit as the fix | Exact rows/lines to edit, anchor counts, and conventions in "Document edits (DESC-10)". |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` (imports `AGENTS.md`) and `./.claude/CLAUDE.md`; treated as locked-decision authority.

- **Behaviour preservation:** tests pin current fork behaviour; code changes limited to genuine bug fixes; the fork runs in production. Keep changes additive (new files/cases, small targeted fixes) so upstream syncs stay cheap.
- **Test target:** ClickHouse `latest` + client V2 only; reuse JUnit/Mockito/Testcontainers, `ClickHouseBase` and test helpers; no new build dependency (only JaCoCo exists as a fork addition).
- **High-risk files (AGENTS.md):** `ClickHouseWriter.java`, `Processing.java`, `KeeperStateProvider.java` are untouched by this phase. `ClickHouseSinkConfig` untouched. Do not change batch composition after `BEFORE` is set: this phase changes the Column list, hence the bytes of a retried batch for an affected table (a rollout fact, section "Rollout facts").
- **Logging:** SLF4J; TRACE per-record detail, DEBUG batch-level, INFO lifecycle. Never log secrets or record data; the new WARN is metadata only (database, table, column, raw type, exception class).
- **Style:** Java 17, Lombok for data classes (`@Getter`, `@Setter`), Google Java Style, DRY, static string constants extracted, comments only for non-obvious invariants, try-with-resources for `AutoCloseable`. Spotless runs `importOrder()` + `removeUnusedImports()` only (googleJavaFormat is commented out in `build.gradle.kts`); the tree has a 46-file pre-existing Spotless baseline, so a new file is checked as "byte-identical after spotlessApply" in a scratch worktree, not with `spotlessCheck`.
- **Tests:** never delete or weaken existing tests; typed SQL read-back with `assertEquals` against the original records; descriptive test names; Docker-free tests are untagged and do not extend `ClickHouseBase` (else they drop out of `testUnit`).
- **Process:** `.claude/CLAUDE.md` requires edits to go through a GSD workflow; AGENTS.md says no commits to the branch without asking; inside `/gsd-execute-phase` the workflow's commit verbs are the sanctioned path.
- **Env:** `TESTCONTAINERS_HOST_OVERRIDE` is set to the literal `"null"` in this shell; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. The test JVM runs `file.encoding=windows-1252` on purpose, so any new reader or writer of ClickHouse output or fixture files specifies UTF-8 explicitly.
- **Verify strings:** exclude the unstaged CRLF diff on `gradlew.bat` (`git status --porcelain --untracked-files=no -- . ':(exclude)gradlew.bat' ':(exclude).planning/state.json'`).

## Summary

The defect has one root cause and two drop sites. `DESCRIBE ... describe_include_subcolumns=1` has no field that separates a structural sub-column (Array offsets, String length, Nullable null map) from a declared Tuple element: both are `is_subcolumn=1`, and an element named `size`, `size0` or `size<digits>` shares its name with a structural row. `ClickHouseHelperClient.shouldSkipDescribeRow` drops `.null`, `.size0` and bare `.size` by suffix, and `Table.updateParent` (upstream-identical) drops `.size[0-9]+` children of an ARRAY or MAP parent. The writer iterates `getTupleFields()`, so the Tuple is written short. The fix classifies each size-named row from three inputs that DESCRIBE does give: the parent row's raw type (its Tuple element list and array depth) and the row's own `type`. This was **measured live this session** (image `bdb5c911a23d`, ClickHouse `26.9.19.9`, `clickhouse-local` in a throwaway `docker run --rm -i`, stdin only, nothing written to the repo): the structural-then-declared **double row is confirmed** for `Array(Tuple(size0 ...))`, the rule classified every row of the 33 E-15 tables, the five spike shapes and eight extra shapes consistently with hand labels, and one trap in `FEATURES.md` was made sharper (`Array(Tuple(size1 UInt64, v String))`: a declared `size1` row has the same type text as a structural `size1` row, so the `N < arrayDepth` guard is load-bearing, not decoration).

The phase is five ordered steps and the plan should be built around the commit boundaries, not around files: (1) a committed capture harness + DDL catalog + JSONL fixtures + live drift premise, before any `src/main` change; (2) a behaviour-preserving `DescribeTableBuilder` extraction, with the 999.4 pin re-pointed at the builder **while still asserting the defect**, green on `testUnit` and the full suite; (3) one fix commit holding the classifier, the `Column` carrier flag, the one-line `Table.java` change, the `DeclaredNullability` key helper, the unit table, the renamed/inverted D-04 pin (run red first), and every DESC-10 document edit; (4) the live round-trip class; (5) the gate. No new dependency, no `ClickHouseWriter`/`Processing`/state-class change.

**Primary recommendation:** classify in one pure static function over `(row name, row type, parent raw type)` that returns a four-way decision (`KEEP`, `KEEP_DECLARED`, `SKIP_STRUCTURAL`, `SKIP_UNDECIDABLE`); have `shouldSkipDescribeRow` (kept, with a 3-argument overload) and the builder both consume it; carry the verdict to `Table` as a boolean `declaredElement` on the child `Column` (set by the builder, read by `Table`'s ARRAY branch only). Rejected carriers are listed in "Carrier for Table.updateParent".

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Reading DESCRIBE rows (V1 `ClickHouseRecord`, V2 `BufferedReader` UTF-8) | Connector discovery I/O (`ClickHouseHelperClient.describeTableV1/V2`) | - | I/O and exception handling stay in the two methods (HLP-04 anchors pin `StandardCharsets.UTF_8` and `InputStreamReader` there). |
| Per-row decision (alias/skipped-parent/structural/declared), `numColumns`, `declaredTypes`, raw-type tracking | Fork-owned `DescribeTableBuilder` (`sink.db.helper`) | `ClickHouseHelperClient.shouldSkipDescribeRow` (pure rule) | One entry point per raw JSON line; Phase 28 plugs in here. |
| Table assembly and sub-column to parent routing | `Table` (`sink.db.mapping`, upstream-identical but for one line) | `Column` (carrier flag) | `Table.updateParent` is the second drop site; it must learn only "this child is a declared element". |
| Declared nullability and the Tuple element key | `DeclaredNullability` (`sink.db.mapping`) | - | The element key (`getColumnName()` or `String.valueOf(i + 1)`) is defined once, publicly, and reused by `collectTuple`, the classifier and (Phase 28) raw-name pairing. |
| RowBinary bytes | `ClickHouseWriter` (untouched) | - | Iterates `getTupleFields()`; the Column list is the wire format. |
| Fixture capture, drift premise, round-trip | Test tree + Gradle `JavaExec` | Docker / Testcontainers | Not shipped: lives in the test source set like `ConfluentArchiveCheck`. |

## Standard Stack

No new library. Everything below is already on the classpath. [VERIFIED: `build.gradle.kts` lines 100-160 read this session; versions from the project CLAUDE.md]

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:clickhouse-data` (`ClickHouseColumn.of`) | 0.9.6 | Parse the parent's raw type into nested columns (`isTuple()`, `isArray()`, `getNestedColumns()`, `getColumnName()`, `isNullable()`) | Already the parser `DeclaredNullability` uses; the classifier reuses it, so both agree on what an element is. |
| Jackson (`ObjectMapper` inside `fromJsonRow`) | 2.22.x (catalog) | Parse one DESCRIBE JSON line | Unchanged; the builder calls `ClickHouseFieldDescriptor.fromJsonRow`. |
| Testcontainers ClickHouse | 1.21.x (catalog) | Start the server for the harness | `ClickHouseBase.setupContainer` is the precedent. |
| JUnit 5 + Mockito | 5.11.3 / 5.21.0 | Tests; no Mockito inline-mock of `ClickHouseHelperClient` | Project rule. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `Column.declaredElement` flag | `Set<String>` of declared names passed to a new `Table` constructor | Changes `Table`'s constructor signature (upstream-identical file), wider diff, breaks `TableTest` call sites. |
| `Column.declaredElement` flag | Re-derive structural-ness inside `Table` from the child's type text | `Table` has no raw parent type and cannot count Array(Map) offsets dimensions; `Array(Tuple(size1 UInt64))` collides (see Pitfall 3). |
| `Column.declaredElement` flag | Builder skips structural `size<digits>` rows itself so `Table` never sees them | Changes `allColumnsList`/`allColumnsMap` contents (Pitfall 19 of the project research says preserve the existing quirk) and leaves `Table.java` untouched, contradicting the user's "fix both drop sites, narrow `Table`" decision. |

**Installation:** none.

**Version verification:** not applicable (no package is added). The gradle wrapper is `gradle-9.8.1-bin.zip` [VERIFIED: `gradle/wrapper/gradle-wrapper.properties`: `distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip`]; the host runs OpenJDK 21 while Gradle's toolchain compiles and runs tests with 17.

## Package Legitimacy Audit

No external package is installed or changed in this phase. Audit table omitted; `gsd-tools query package-legitimacy check` has nothing to check.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Measured DESCRIBE rows (the spike input; confirms and sharpens FEATURES.md 1.3)

[VERIFIED: `docker run --rm -i clickhouse/clickhouse-server:latest clickhouse-local --multiquery < file.sql`, run this session, `SELECT version()` printed `26.9.19.9`, image `bdb5c911a23d`, the same image as the Phase 24 and Phase 25 evidence; scratch SQL and output in `/tmp/gsd26-research/{shapes,e15,extra}.sql`, `/tmp/gsd26-research/e15.out`.] These are `DESCRIBE ... describe_include_subcolumns=1` rows over `Memory` tables; sub-column enumeration does not depend on the engine, and the E-15 rows match what Phase 24 measured through a MergeTree table and the real sink task. The committed harness re-measures them through the production reader in step 1; treat this table as the prediction the spike confirms or denies.

The five required spike shapes (`name : type`; `\n    ` is the JSON `\n` escape plus four spaces that the server puts in multi-line Tuple types):

```text
i  Array(Array(Tuple(size1 UInt8, size0 UInt8)))
  i.size0 : UInt64                      structural (offsets, dim 0)
  i.size1 : Array(UInt64)               structural (offsets, dim 1)
  i.size1 : Array(Array(UInt8))         DECLARED   (same name as the row above)
  i.size0 : Array(Array(UInt8))         DECLARED
e  Array(Tuple(size0 Tuple(a UInt8), v String))
  e.size0   : UInt64                    structural
  e.size0   : Array(Tuple(a UInt8))     DECLARED   (double row confirmed)
  e.size0.a : Array(UInt8)              child of the declared element (cascade victim today)
  e.v : Array(String) ; e.v.size : Array(UInt64)   structural .size under a String element
e  Array(Tuple(size0 String, size String))
  e.size0 : UInt64 (structural) ; e.size0 : Array(String) (DECLARED) ; e.size0.size : Array(UInt64) (structural: parent is a String)
  e.size : Array(String) (DECLARED, bare size, no structural twin) ; e.size.size : Array(UInt64) (structural)
c  Array(Map(String, Tuple(size UInt8, size0 UInt8)))
  c.size0 : UInt64 ; c.size1 : Array(UInt64)            structural (Array offsets, Map offsets)
  c.keys : Array(Array(String)) ; c.keys.size : Array(Array(UInt64))
  c.values : Array(Array(Tuple(size UInt8, size0 UInt8)))
  c.values.size  : Array(Array(UInt8))    DECLARED (no structural twin: Map values carry no offsets row)
  c.values.size0 : Array(Array(UInt8))    DECLARED (gg3 drops it by suffix; so this shape is affected too)
c  Tuple(keys UInt8, `values` String)
  c.keys : UInt8 ; c.values : String ; c.values.size : UInt64   (structural String length)
```

Extra shapes worth adding to the DDL catalog (all measured; names are suggestions, backslash-free):

```text
x1 Tuple(size Tuple(a UInt8, b String))          c.size : Tuple(...), c.size.a, c.size.b, c.size.b.size : UInt64
x2 Array(Tuple(size Tuple(a UInt8), v String))   e.size0 : UInt64, e.size : Array(Tuple(a UInt8)), e.size.a : Array(UInt8)
x3 root column named size: size Tuple(size UInt8, size0 String)
                                                 size.size : UInt8, size.size0 : String, size.size0.size : UInt64
x4 Array(Tuple(size1 UInt64, v String))          e.size0 : UInt64 ; e.size1 : Array(UInt64)  <- DECLARED but identical text to a structural size1 row
x5 Array(Array(Tuple(size1 UInt64, size0 UInt64)))   i.size0 UInt64, i.size1 Array(UInt64), i.size1 Array(Array(UInt64)), i.size0 Array(Array(UInt64))
x6 Array(Nullable(Tuple(size0 UInt8, v String))) e.size0 : UInt64, e.null : Array(UInt8), e.size0 : Array(Nullable(UInt8)), e.v : Array(Nullable(String))
x7 Map(String, Array(Tuple(size UInt8, v String)))   m.values : Array(Array(Tuple(...))), m.values.size1 : Array(UInt64) (structural, parent depth 2), m.values.size : Array(Array(UInt8)) (DECLARED)
x8 j JSON(a Tuple(size UInt8, v String))         j.a, j.a.size : UInt8, j.a.v, j.a.v.size   (DESC-05 ancestor walk must still win)
```

Other E-15 facts read from the same run [VERIFIED: `/tmp/gsd26-research/e15.out`]: `s4_a03 Nullable(Tuple(size UInt8, v String))` emits `c.null : UInt8`, `c.size : Nullable(UInt8)` (reported Nullable although declared plain; `DeclaredNullability` repairs that), `c.v.size : Nullable(UInt64)`; `s4_a05 Map(String, Tuple(size UInt8, v String))` emits `c.size0 : UInt64`, `c.keys`, `c.keys.size`, `c.values : Array(Tuple(...))`, `c.values.size : Array(UInt8)` (declared), `c.values.v.size`; `s4_a07 Tuple(x Array(Tuple(size0 UInt8)))` emits the double row `c.x.size0 : UInt64` then `c.x.size0 : Array(UInt8)`; `s4_a13 Array(Tuple(v String, size1 UInt16))` emits `c.size0, c.v, c.v.size, c.size1 : Array(UInt16)` (declaration order, so `tupleFields` order equals DDL order); `s4_u15 Nested(size UInt8, v String)` emits `n.size` and `n.v` as roots (real `is_subcolumn` 0) and `n.size.size0`, `n.v.size0`, `n.v.size` below them; `s4_u19 Tuple(UInt8, String)` emits `c.1`, `c.2`, `c.2.size : UInt64`; `s4_u16` (Enum8 with labels `size`/`size0`) and `s4_u18` (label text containing `size`) emit no size-named sub-column rows.

Verdict for DESC-06: **structural-then-declared double row: CONFIRMED** (in 4 of the 5 spike shapes and in `s4_a07`, `s4_a10`, `x5`, `x6`). Order observed: structural twin first, declared second, always; the rule does not depend on it (Pitfall 4 explains the one place downstream code does).

## Classifier rule (to be stated in the spike record; tested by the unit table)

Inputs: the row's `name` and `type`, and `rawTypes[parentName]` where `parentName` is `name` up to its last dot. `rawTypes` holds the raw DESCRIBE type of **every kept row** (not only roots, which is what `declaredTypes` holds today). Segment `s` is the text after the last dot. "Size-named" means `s` equals `size` or `size` followed by one or more digits.

| Step | Condition | Result |
|------|-----------|--------|
| 1 | not `is_subcolumn` | `KEEP` (unchanged) |
| 2 | name ends `.null` | `SKIP_STRUCTURAL` (the server rejects an element named `null`, code 36, so there is no collision; keep unconditional) |
| 3 | a `JSON`/`Dynamic` ancestor is in `columnsSoFar` (the DESC-05 walk) | `SKIP_STRUCTURAL`; this runs **before** step 4 so `j.a.size` stays dropped (x8) |
| 4 | `s` is not size-named | `KEEP` |
| 5 | no raw type known for the parent, or `ClickHouseColumn.of(parentName, DeclaredNullability.normalise(parentRaw))` throws | `SKIP_UNDECIDABLE` for `size0`/bare `size` (status quo skip), `KEEP` unflagged for `size<N>`, `N >= 1` (status quo: registered, then dropped by `Table`); in both cases one new metadata-only WARN (D-03, D-04) |
| 6 | read the parsed parent through `Array` (count depth `D`), `Nullable`, `LowCardinality`, `SimpleAggregateFunction`; it ends in a Tuple, and `s` is the key (D-key helper) of one of that Tuple's elements | go to 7. Otherwise (String, scalar array, Map, Variant member, Tuple that does not declare `s`) the row is structural: `s == size` or `size0` -> `SKIP_STRUCTURAL`; `size<N>`, `N >= 1` -> `KEEP` unflagged |
| 7 | `s == size` | `KEEP_DECLARED` (a Tuple has no structural bare-`size` row of its own) |
| 8 | `s == size<N>`, and for some `k < D` the name is `size<k>` and `type` equals `Array`^k wrapped around `UInt64` | structural (`SKIP_STRUCTURAL` for `size0`; `KEEP` unflagged for `N >= 1`) |
| 9 | any other `size<N>` (including `N >= D`) | `KEEP_DECLARED` |

Notes that make the rule safe:
- **Compare by constructing, not parsing:** for `k` in `0..D-1` build `"size" + k` and `"Array(" * k + "UInt64" + ")" * k` and compare strings. No integer parse, so `size99999999999` and `size01` (never a server offsets row) cannot throw or mis-classify; they fall to step 9. [ASSUMED: that a server never prints leading-zero offsets names; consistent with all 75+ measured rows]
- **`N >= 1` structural rows stay `KEEP` unflagged** because gg3 registers them and `Table` ignores them via `SIZE_FIELD_MATCHER`; changing that moves `allColumnsList` (project Pitfall 19). Only `.size0` and bare `.size` structural rows are skipped, as today.
- **A structural twin never enters `skippedCols` and never enters `rawTypes`.** The declared twin's row is the one that registers its raw type, so `e.size0.size` (n3) sees parent type `Array(String)` and classifies structural, and `e.size0.a` (n2) is not swallowed by the parent-skipped branch.
- `.size1`-style rows under a **non-Tuple** parent (`c.size1 : Array(UInt64)` in `Array(Map(...))`, `m.values.size1` in x7) classify structural through step 6 and stay exactly as gg3 handles them.
- Verified against 75 rows by FEATURES.md (prototype outside the repo) and, this session, by hand against every row printed above [ASSUMED for the 8 extra shapes: hand-labelled by the researcher, not by an executable prototype; the step-1 spike should run a throwaway Python `-I` prototype over the committed JSONL against a hand-labelled sidecar before the classifier plan is written, the Phase 23 D-03 / FEATURES precedent].

## Code sites (current state, read this session)

Line numbers are those of the tree at `37d2ef11` (branch `sync/v1.7.0`); the extraction commit moves them, so re-grep before editing.

| Site | Lines | Signature / text that matters |
|------|-------|-------------------------------|
| `ClickHouseHelperClient.describeTable` | 451 | dispatches on `useClientV2` to `describeTableV2` / `describeTableV1` |
| `ClickHouseHelperClient.shouldSkipDescribeRow` | 473-488 | `static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar)`; body quote: `if (name.endsWith(".null") \|\| name.endsWith(".size0") \|\| name.endsWith(".size")) {` then the DESC-05 loop `for (int dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.', dot - 1))` returning true for a `Type.JSON` or `Type.DYNAMIC` ancestor in `columnsSoFar` |
| `ClickHouseHelperClient.buildDescribedTable` | 499-513 | `static Table buildDescribedTable(String database, String tableName, boolean hasDefaults, List<Column> columns, int numColumns, Map<String, String> declaredTypes)`: `new Table(...)`, then `DeclaredNullability.apply(root, declaredTypes.get(root.getName()), database, tableName)` for every root with `DeclaredNullability.containsTuple(root)` |
| `describeTableV1` | 515-589 | `if (tableName.startsWith(".inner")) return null;` then `ClickHouseClient.builder()...client.read(server).set("describe_include_subcolumns", true).format(ClickHouseFormat.JSONEachRow)` with `setReplicaTagHeaderV1`; loop `for (ClickHouseRecord r : response.records())` reading `r.getValue(0)` and `v.asString()`; `catch (ClickHouseException \| JsonProcessingException e)` logs `Exception when running describeTable %s` and returns null |
| `describeTableV2` | 591-663 | same `.inner` check; `QuerySettings` with `ClickHouseFormat.JSONEachRow`, `settings.serverSetting("describe_include_subcolumns", "1")`, `settings.setDatabase(database)`, `setReplicaTagHeaderV2`; `new BufferedReader(new InputStreamReader(describeStream, StandardCharsets.UTF_8))` loop `readLine()`; `catch (Exception e)` logs `describeTableV2 failed` and returns null |
| loop body (identical in V1 and V2) | V1 541-583, V2 615-656 | `fromJsonRow`; `numColumns++` for non-sub-column rows **before** any skip; alias/materialized/ephemeral -> `skippedCols.add(name)`, `continue`; sub-column whose parent is in `skippedCols` -> add + `continue`; `shouldSkipDescribeRow(fieldDescriptor, columnsByName)` -> add + `continue`; `declaredTypes.put` for non-sub-column rows; `hasDefault()` -> `hasDefaults = true`; `Column.extractColumn(fieldDescriptor)`, `null` -> `LOGGER.error("Unable to handle column: {}", ...)` and `return null`; `columns.add`, `columnsByName.put` |
| `Table.SIZE_FIELD_MATCHER` | 24 | `private static final Predicate<String> SIZE_FIELD_MATCHER = Pattern.compile(".+\\.size[0-9]+$").asMatchPredicate();` |
| `Table.updateParent` ARRAY branch | 118-141 | line 120: `if (SIZE_FIELD_MATCHER.test(childName) \|\| childName.endsWith(".null"))` followed by `return;` |
| `Table.updateParent` MAP branch | 142-166 | line 144: `if (child.getName().endsWith(".keys") \|\| SIZE_FIELD_MATCHER.test(child.getName()))`; leave unchanged: a declared element is never a direct child of a Map (its parent is the `m.values` Tuple Column) |
| `Table.updateParent` TUPLE branch | 167-195 | `parent.getTupleFields().add(child);` at 194; no name check, which is why a structural row that leaks through becomes a Tuple field (Pitfall 1) |
| `DeclaredNullability.collectTuple` | 218-252 | element key at 228-229: `String name = elements.get(i).getColumnName();` and `byKey.put(name == null \|\| name.isEmpty() ? String.valueOf(i + 1) : name, elements.get(i));` into a `LinkedHashMap`; pairing prefix `tuple.getName() + NAME_SEPARATOR` |
| `DeclaredNullability` constants | 45-50 | `FALLBACK_WARNING = "Could not take element nullability for column {} of table {}.{} from its declared type {} ({}); keeping the nullability DESCRIBE reported for it"`; `UNPAIRED_ELEMENT_WARNING = "Declared Tuple element(s) {} of column {} of table {}.{} (declared type {}) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired"` |
| `DeclaredNullability.unwrapSimpleAggregate` / `normalise` | 181 / 77 | `private static ClickHouseColumn unwrapSimpleAggregate(ClickHouseColumn parsed)` (private) and `public static String normalise(String declaredType)` |
| `Column` | 51-74 | `@Builder @Getter public class Column`; `@Setter(AccessLevel.PACKAGE) private boolean isNullable;` (56); `@Setter private Column parent;` (73-74); `extractColumn` Tuple branch returns `builder.type(Type.TUPLE).tupleFields(new ArrayList<>()).isNullable(isNull).build()` |
| `ClickHouseFieldDescriptor.fromJsonRow` | 47-49 | `return OBJECT_MAPPER.readValue(json.replace("\\", "\\\\").replace("\n", "\\n"), ClickHouseFieldDescriptor.class);` throws `JsonProcessingException`; unchanged this phase |

[VERIFIED: all quotes from files opened with the file reader this session: `ClickHouseHelperClient.java` 440-663, `Table.java` 1-245, `DeclaredNullability.java` 1-253, `Column.java` 34-300, `ClickHouseFieldDescriptor.java` 1-50.]

Writer contract that makes the Column list the wire format [VERIFIED: `ClickHouseWriter.java:803-806`]: `col.getTupleFields().forEach(column -> { String[] colNameSplit = column.getName().split("\\."); String fieldName = colNameSplit.length > 0 ? colNameSplit[colNameSplit.length - 1] : column.getName();`. A kept element named `size` therefore needs no writer change; the Kafka field is looked up by the last dotted segment.

## Architecture Patterns

### System architecture (data flow of one DESCRIBE pass after the phase)

```text
describeTableV1 / describeTableV2                       (I/O, exception handling, UTF-8 read: unchanged text)
   |  one raw JSONEachRow line at a time (V1: v.asString(); V2: br.readLine())
   v
DescribeTableBuilder.addRow(String jsonLine)            (NEW, sink.db.helper)
   |-- ClickHouseFieldDescriptor.fromJsonRow            (unchanged; throws JsonProcessingException)
   |-- numColumns++ if not sub-column                    (before any skip, as today)
   |-- alias / materialized / ephemeral  -> skippedCols, next row          (unchanged)
   |-- parent in skippedCols             -> skippedCols, next row          (unchanged)
   |-- decide(row, columnsByName, rawTypes)              (NEW pure rule; shouldSkipDescribeRow 3-arg consumes the same)
   |        SKIP_STRUCTURAL  .null / JSON-Dynamic ancestor -> skippedCols, next row (status quo)
   |        SKIP_STRUCTURAL  size0 / bare size twin        -> next row, NOT skippedCols, NOT rawTypes   (cascade guard)
   |        KEEP_STRUCTURAL (size<N>, N>=1, structural)    -> kept unflagged exactly as gg3; Table drops it via SIZE_FIELD_MATCHER
   |        SKIP_UNDECIDABLE / KEEP_UNDECIDABLE            -> one WARN (db, table, parent, raw type), status quo action
   |        KEEP_DECLARED                                  -> Column gets declaredElement = true
   |-- rawTypes.put(name, type) for every kept row except KEEP_STRUCTURAL / KEEP_UNDECIDABLE; declaredTypes for roots (as today)
   |-- Column.extractColumn -> columns / columnsByName     (null -> addRow returns false -> caller returns null)
   v
build() -> ClickHouseHelperClient.buildDescribedTable(...)
   |-- new Table(...)  -> Table.updateParent: ARRAY branch ignores a size<digits> child unless child.isDeclaredElement()
   |-- DeclaredNullability.apply per Tuple-bearing root     (element now pairs: no COLS-14 WARN)
   v
Table -> ClickHouseWriter.getTupleFields() -> RowBinary bytes (full tuple)
```

### Recommended structure

```text
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/
  ClickHouseHelperClient.java      # V1/V2 shrink to query + stream + addRow + build; shouldSkipDescribeRow keeps its 2-arg signature, gains a 3-arg overload
  DescribeTableBuilder.java        # NEW, fork-owned (D-01); owns loop state; holds the WARN constant
  (classifier: static methods + enum inside DescribeTableBuilder, or a small package-private final class beside it)
src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/
  DeclaredNullability.java         # + public static String elementKey(ClickHouseColumn element, int index); collectTuple calls it
  Column.java                      # + @Setter private boolean declaredElement;  (public setter: the builder is in another package)
  Table.java                       # one line in the ARRAY branch
src/test/java/com/clickhouse/kafka/connect/sink/db/helper/
  DescribeFixtureCatalog.java      # NEW, public: the committed DDL list (single source for D-06/D-07/D-08)
  DescribeFixtureCapture.java      # NEW, public main(): the harness
  DescribeFixtureDriftLiveTest.java        # D-07, extends ClickHouseBase
  DescribeTableBuilderUnitTest.java        # D-05/DESC-08, Docker-free, reads the JSONL
src/test/java/com/clickhouse/kafka/connect/sink/
  TupleSizeNamedElementRoundTripLiveTest.java   # D-11, beside NullableTupleMatrixLiveTest (reuses package-private NullableRoundTrip)
src/test/resources/describe-fixtures/
  <key>.jsonl (one per fixture) + SERVER_VERSION.txt
```

### Pattern 1: fork logic outside upstream method bodies, one anchored call site
**What:** `DeclaredNullability` (Phase 19) is the precedent: a new fork-owned file does the work and upstream's file keeps one call. **When:** the builder (D-01) and the classifier. **Example (extraction target):**

```java
// describeTableV2, after the extraction (illustrative; keep the surrounding try/catch and settings text byte-identical)
DescribeTableBuilder builder = new DescribeTableBuilder(database, tableName);
try (QueryResponse queryResponse = client.query(describeQuery, settings).get();
     InputStream describeStream = queryResponse.getInputStream();
     BufferedReader br = new BufferedReader(new InputStreamReader(describeStream, StandardCharsets.UTF_8))) {
    String line;
    while ((line = br.readLine()) != null) {
        if (!builder.addRow(line)) {
            return null; // an unmappable column: the loop logged it, the caller returns null exactly as before
        }
    }
}
return builder.build();
```

`addRow` must declare `throws JsonProcessingException` and not wrap it: V1 catches `ClickHouseException | JsonProcessingException`, V2 catches `Exception`, and both must log the same text as today. A `RuntimeException` out of `Column.extractColumn` (for example `can't parse SimpleAggregateFunction type`) must keep propagating unchanged (V1 does not catch it).

### Pattern 2: the pure decision function
**What:** one function over `(row, columnsSoFar, rawTypes)` returning the six-way decision (`KEEP_STRUCTURAL` is a size-named `size<N>`, `N >= 1` structural row: kept and registered as gg3 does, unflagged, so `Table` drops it, but kept out of `rawTypes`); `shouldSkipDescribeRow(row, columnsSoFar)` becomes `shouldSkipDescribeRow(row, columnsSoFar, Map.of())` so the ~20 existing unit call sites compile and keep passing (a missing raw type is `*_UNDECIDABLE`, which skips `.size0`/`.size` exactly as gg3 did); the 3-arg overload is what the builder and the unit table exercise. **Why not inline in the loop:** the unit table must drive the same code as production (WR-02).

```java
// Illustrative skeleton, not final code. Constants (SIZE_SEGMENT = "size", SIZE_OFFSETS_SEGMENT = "size0", NULL_SUFFIX = ".null") are extracted per the project style.
enum RowDecision { KEEP, KEEP_DECLARED, KEEP_STRUCTURAL, SKIP_STRUCTURAL, SKIP_UNDECIDABLE, KEEP_UNDECIDABLE }

static RowDecision decide(ClickHouseFieldDescriptor row, Map<String, Column> columnsSoFar, Map<String, String> rawTypes) {
    if (!row.isSubcolumn()) return KEEP;
    String name = row.getName();
    if (name.endsWith(NULL_SUFFIX) || hasJsonOrDynamicAncestor(name, columnsSoFar)) return SKIP_STRUCTURAL; // existing DESC-05 loop, moved verbatim, ahead of any size rule
    int dot = name.lastIndexOf('.');
    String segment = name.substring(dot + 1);
    if (!isSizeNamed(segment)) return KEEP;
    boolean skippedToday = segment.equals(SIZE_SEGMENT) || segment.equals(SIZE_OFFSETS_SEGMENT); // the rows gg3 skips by suffix
    ParentShape parent = ParentShape.of(rawTypes.get(name.substring(0, dot))); // null when absent or ClickHouseColumn.of throws
    if (parent == null) return skippedToday ? SKIP_UNDECIDABLE : KEEP_UNDECIDABLE;
    if (!parent.declares(segment)) return skippedToday ? SKIP_STRUCTURAL : KEEP_STRUCTURAL;
    if (segment.equals(SIZE_SEGMENT) || !parent.isOffsetsRow(segment, row.getType())) return KEEP_DECLARED;
    return skippedToday ? SKIP_STRUCTURAL : KEEP_STRUCTURAL;
}
```

`ParentShape` holds `(ClickHouseColumn tuple, int arrayDepth)` after reading the parsed type through `Array` (depth++), `Nullable`, `LowCardinality` and `SimpleAggregateFunction` (same unwrap as `DeclaredNullability.unwrapSimpleAggregate`, which is private: either widen it to package-private and put the classifier in `sink.db.mapping`, or duplicate the 5-line loop; prefer **putting `ParentShape` in `sink.db.mapping` next to `DeclaredNullability`** so the unwrap is shared at package visibility and the only new public API is `elementKey`, as CONTEXT requires). `declares(segment)` compares against `DeclaredNullability.elementKey(element, i)` over `getNestedColumns()`. `isOffsetsRow` builds `"size" + k` and `Array(` k times around `UInt64` for each `k < arrayDepth` and string-compares (no integer parse).

### Pattern 3: the cascade guard
`skippedCols.add(name)` runs only for `SKIP_STRUCTURAL` rows that are not the `size0`/bare `size` twin, and for `SKIP_UNDECIDABLE` (D-03: exactly gg3, including its cascade). `rawTypes.put` runs for every kept row except `KEEP_STRUCTURAL` and `KEEP_UNDECIDABLE`, so the declared twin is the registered raw type. Unit rows: x1 (`c.size.a` survives), n2 (`e.size0.a` survives), n3 (`e.size0.size` and `e.size.size` dropped), x3.

### Carrier for `Table.updateParent` (Claude's discretion; recommended: boolean on the child `Column`)
Add `@Setter private boolean declaredElement;` to `Column` (default false through `@Builder`; Lombok generates `isDeclaredElement()` and a public `setDeclaredElement`; the existing `isNullable` setter is package-private, so this one is deliberately public because the builder lives in `sink.db.helper`). The builder sets it for `KEEP_DECLARED` only. `Table` changes one line:

```java
// Table.java:120 before:  if (SIZE_FIELD_MATCHER.test(childName) || childName.endsWith(".null"))
// after:                  if ((SIZE_FIELD_MATCHER.test(childName) && !child.isDeclaredElement()) || childName.endsWith(".null"))
```

Why this is the narrowest correct form: the flag is evaluated on the **outer** child (the Array-wrapped sub-column Column) before the ARRAY branch unwraps it; the recursive calls into the TUPLE/MAP branches never consult it; hand-built Columns in `TableTest` default to false, so upstream's `TableTest` stays byte-identical and green. Alternatives named and rejected in "Alternatives Considered". New anchors: `Table.java` `!child.isDeclaredElement()` count 1, a **negative** row for the old text `if (SIZE_FIELD_MATCHER.test(childName) || childName.endsWith(".null"))` count 0 (so a sync that takes upstream's `Table.java` wholesale fails the gate instead of silently reverting), `Column.java` `private boolean declaredElement;` count 1. Add a new `TableSizeNamedElementTest` (Docker-free, hand-built Columns) rather than editing upstream's `TableTest`; note `TableTest` extends `ClickHouseBase`, so any rows added there are live, not Docker-free. Careful with the anchor file: fields are TAB-separated, so a pattern cannot contain a tab, and the count is lines under `grep -cF`.

### Public element-key helper
```java
// DeclaredNullability.java (extracted from collectTuple, lines 228-229; collectTuple must call it, so there is one definition)
public static String elementKey(ClickHouseColumn element, int index) {
    String name = element.getColumnName();
    return name == null || name.isEmpty() ? String.valueOf(index + 1) : name;
}
```
Pin it in `DeclaredNullabilityTest`-style rows with unnamed (`Tuple(UInt8, String)` -> `1`, `2`) and backtick-named (`` `my size` `` -> `my size`) elements, using fixtures `s4_u19` and `s4_u11`. Phase 28 reuses it for raw-name pairing.

### Anti-Patterns to Avoid
- **Classifying by occurrence order** ("first of two same-named rows is structural"): works on every measured table and is undocumented server behaviour.
- **A closed list of structural names:** bare `.size` is itself undocumented and was found live; the list would rot.
- **"Parent is a Tuple" alone:** wrong for `Array(Tuple)` (`e.size0` offsets) and for `Map(K, Tuple)`.
- **Putting `.size<N>` structural rows into `skippedCols`/skipping them in the builder:** changes `Table.getAllColumnsList()`; keep gg3's behaviour.
- **Tidying `Table`'s MAP-branch matcher or `.keys`/`.values` handling:** DESC-03 pins depend on it.
- **Inline Mockito mock of `ClickHouseHelperClient`** (OpenJ9 crash, project rule); drive the builder with JSON lines instead.

## Extraction (DESC-07): what must stay byte-for-byte

- `.inner` early return stays at the top of both methods, before any I/O (D-02 lists it as behaviour to keep; leaving it in the methods keeps the diff smallest).
- Message texts keep their wording: `Skipping column {} as it is either an alias, materialized view, or ephemeral`, `Skipping subcolumn {} as its parent column is either an alias, materialized view, or ephemeral`, `Skipping structural sub-column {}`, `Unable to handle column: {}`. Only the logger name changes if the builder uses its own logger; nothing in `src`, `docs` or scripts greps these texts [VERIFIED: grep of `src docs .planning/scripts CHANGELOG.md`, hits only in `ClickHouseHelperClient.java`]. Using `LoggerFactory.getLogger(ClickHouseHelperClient.class)` inside the builder would keep even the logger name; either is acceptable, but then `LogCapture.attach(...)` in the flipped pin must attach to whichever logger the new WARN uses.
- V1's `// TODO: add subcolumn filter here?` comment exists only in the V2 loop; drop it or keep it, it is comment text.
- **Anchors move in the extraction commit** (the invariant evaluates every commit): measured now with `grep -cF` on `ClickHouseHelperClient.java`: `shouldSkipDescribeRow` 3, `buildDescribedTable(` 3, `DeclaredNullability.apply(` 1, `StandardCharsets.UTF_8` 1, `InputStreamReader` 2. After the extraction expect `shouldSkipDescribeRow` 1 (definition) and `buildDescribedTable(` 1 (definition); `DeclaredNullability.apply(` 1, `StandardCharsets.UTF_8` 1 and `InputStreamReader` 2 must not move (the V2 reader stays in the method). Add rows for the builder: `new DescribeTableBuilder(` 2 in `ClickHouseHelperClient.java`, and in the builder file `shouldSkipDescribeRow(` 1 and `buildDescribedTable(` 1 (counts to be re-measured at execution, not trusted from here). Move HLP-02 / COLS-10 rows, do not delete them.
- Re-point the existing 999.4 pin at the builder **while it still asserts the defect** (element absent, one COLS-14 WARN, `apply() == true`): feed it JSON lines rendered from the existing R-01 `String[]` pairs by a small test helper (name, type, `is_subcolumn` = 1 when the name contains a dot, the other five keys empty strings). The R-01 strings contain `\\n` (backslash + n), which is exactly the JSON escape text the server sends, so `fromJsonRow` yields the same type strings the `newDescriptor` path produced. This step closes WR-02 and gives step 3 a real "seen red" baseline.
- Existing tests that already drive `describeTableV1` in the default full suite: `TableTest.extractCommentV1` (`props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V1")`, comment containing backslashes) and the `{"V1", "V2"}` parameterised rows of `ClickHouseWriterTest` (lines 299, 404, 526) [VERIFIED: grep]. FEATURES.md says V1 describe is not exercised by CI; that is too strong. They do not cover size-named rows, so the manual V1 run still matters for the fix commit.

## Capture harness, fixtures and the drift premise (D-05, D-06, D-07)

**Precedent read:** `verifyConfluentArchive` is a `JavaExec` whose `classpath = sourceSets["test"].output.classesDirs` (JDK only) and whose `mainClass` is a `main()` class in the test source set (`ci.ConfluentArchiveCheck`); the pin guard takes the main class name from the class literal [VERIFIED: `build.gradle.kts:421-429`, STATE decision D-09]. The capture harness needs Testcontainers, the ClickHouse client and the test fixtures, so its classpath is **`sourceSets["test"].runtimeClasspath`** (which includes `testFixtures` through the `java-test-fixtures` plugin and the Testcontainers test dependencies) instead of `classesDirs`.

```kotlin
// build.gradle.kts, next to verifyConfluentArchive (illustrative)
tasks.register<JavaExec>("captureDescribeFixtures") {
    group = "verification"
    description = "Re-captures src/test/resources/describe-fixtures from ClickHouse (needs Docker); prints the server version"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCapture")
    args(layout.projectDirectory.dir("src/test/resources/describe-fixtures").asFile.absolutePath)
}
// documented command:  env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures
```

- **Container outside JUnit:** what `ClickHouseBase.setupContainer` does is `new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE).withNetwork(...).withNetworkAliases(...).withPassword("test_password")...withExposedPorts(8123)`, `PullPolicy.alwaysPull()` only when the version is the floating `latest`, then `db.start()` [VERIFIED: `ClickHouseBase.java:108-132`]. The harness needs only `new ClickHouseContainer(image).withPassword("test_password")` in a try-with-resources (project rule: `AutoCloseable`), the same pull policy rule (so the committed rows come from the same image the suite pulls), then the non-cloud branch of `getBaseProps()` (`HOSTNAME`, `PORT` = mapped 8123, `DATABASE`, `USERNAME`, `PASSWORD`, `SSL_ENABLED=false`, `CLIENT_VERSION=V2`) fed to `ClickHouseTestHelpers.createClient(props)`, and `ClickHouseTestHelpers.createDatabase(database, client)` for a scratch database. It should refuse `CLICKHOUSE_VERSION=cloud` and `CLICKHOUSE_CLUSTER_NAME`.
- **Raw lines, same reader as production V2:** `NullableRoundTrip.describeRows` already reads DESCRIBE exactly like `describeTableV2` (JSONEachRow, `describe_include_subcolumns=1`, `QuerySettings.setDatabase`, UTF-8) but returns parsed descriptors and is package-private in `sink`. Add a raw-line variant (`describeRawLines(chc, table)`) to a public test helper in `sink.db.helper` and let `NullableRoundTrip.describeRows` delegate to it, so the harness, the drift premise and the existing helper share one reader (DRY). Write files with `Files.writeString(path, text, StandardCharsets.UTF_8)`: the JavaExec JVM's default charset follows the locale under JDK 17, and the test JVM is `windows-1252` on purpose.
- **Fixture catalog (single source):** one public class holding, per key, the full column DDL fragment (the E-15 tables are `(id Int32, c <type>)` except `u12`..`u15`, whose columns are plain `size UInt8`, `size0 UInt8`, `size Array(UInt8)` and `n Nested(size UInt8, v String)`), the 13 affected, 19 look-alikes, `s4_o01` and the new shapes (the five required plus x1..x8 above, named backslash-free). It also carries what the round-trip needs (Connect schema, sample value, read-back cells); that is why a Java class beats a resource file: the unit test, the harness, the drift test and the round-trip test are all in the test source set.
- **Resources:** `src/test/resources/describe-fixtures/<key>.jsonl` (raw lines, byte-for-byte what the server printed, `\n` as the two characters backslash-n inside the JSON) plus one `SERVER_VERSION.txt` (version string and image id; the drift test compares rows only, so the version file is documentation and the harness prints it too). Docker-free tests read them from the classpath with an explicit UTF-8 reader.
- **Drift premise (live):** per fixture, create the table from the catalog DDL in the class database, `describeRawLines`, `assertEquals(committed lines, live lines, key)` so a failure names the table. It must not extend anything but `ClickHouseBase`, runs in the full suite only, and is expected to go red when a newer `latest` image changes a DESCRIBE shape (that is its job; the fix is `captureDescribeFixtures` plus a reviewed diff). Note the cost: it ties the suite to the floating `latest`, which already has a history of drift (project memory: Phase 11 `latest` drift).
- **Optional characterization snapshots (recommended):** in the extraction step, after the builder is in place, generate a plain-text Column-list snapshot per fixture (names, types, nullability, and the `tupleFields` tree) through the builder and commit it beside the JSONL. In the fix commit the diff of those snapshot files **is** the behaviour change: only the 13 affected tables (plus the x-shapes that are affected) change, and the 19 look-alikes plus `s4_o01` do not. This is the mechanical form of "look-alike Column-list comparison" that project Pitfall 5 asks for, and it gives D-08's "pinned both ways" a Docker-free half.

## Unit table (DESC-08, Docker-free)

New `DescribeTableBuilderUnitTest` (no `ClickHouseBase`, untagged, so it runs in `testUnit`). For every fixture: load the JSONL, `addRow` each line, `build()`, then assert against **the DDL as oracle**, not against a hand-labelled list: for every Tuple Column at every depth (root, `getArrayType()`, `getMapValueType()`, nested `tupleFields`), the `tupleFields` keys (suffix after the Tuple's own name plus `.`), in order, equal `DeclaredNullability.elementKey` over `ClickHouseColumn.of(...).getNestedColumns()` of the declared type. That single assertion covers "a declared element named `size`, `size0` or `size<digits>` becomes a Column" for all 13 affected shapes and every look-alike. Add explicit rows for the "still dropped" side:
- Array offsets, the Nullable null map and Map `keys`/`values` structure: no Column named `*.null`; no structural twin in `tupleFields` (n1, n2, x6, `s4_a05`, `s4_o01`).
- nested `.size1` (`s4_o01 c.size1`, x7 `m.values.size1`, `s4_a10`): present in `getAllColumnsList()` exactly as in the pre-fix snapshot, absent from every `tupleFields`.
- DESC-05 under JSON/Dynamic (x8 `j.a.size`, plus the existing `tj.jj.k` rows): no Column below `j`.
- cascade guard: `e.size0.a` (n2) and `c.size.a` (x1) are Columns; `e.size0.size` (n3) is not.
- the fail-closed rows (D-03/D-04) with a hand-made line whose parent raw type does not parse (for example a truncated `Tuple(`): the row is dropped, one WARN on the builder logger names database, table, parent column and raw type, no exception text, and the unpaired-element WARN from `DeclaredNullability` still fires for the same table.
- zero WARN assertions on all 33+ fixtures with `LogCapture` attached to the builder class, `ClickHouseHelperClient` and `DeclaredNullability` (the live half is in D-11).
Also add rows for the 3-argument `shouldSkipDescribeRow` next to the existing 2-argument rows and for `elementKey` (unnamed and backtick-named elements); keep every existing row unchanged.

## Live round-trip (DESC-09, D-08, D-09, D-11)

New `TupleSizeNamedElementRoundTripLiveTest extends ClickHouseBase` in package `com.clickhouse.kafka.connect.sink` (the package-private `NullableRoundTrip` helpers: `createTable`, `putAndStop`, `selectAndAssert`, `assertPutFails`, `describeRows`, `serverVersion`, plus `RowSnapshots`, `LogCapture` and the `uniqueConnectorName(testInfo)` pattern, are reusable only from there). Per fixture: create the table **before** `start()` (`ClickHouseWriter.start()` fails against an empty database), `props.put("name", uniqueConnectorName(testInfo))`, `putAndStop` one record built from the catalog, then typed read-back cells (`Cell(alias, sqlExpression, expected)`; client-v2 `getString` returns the raw text, so `toString(c)` prints `(7,'nm')`, `[(1,'a'),(2,'b')]`, `{'k':(3,'z')}` [VERIFIED: clickhouse-local run this session]). For a String that holds a control character compare `hex(tupleElement(c, 'v'))`. Assert zero WARN events on `ClickHouseHelperClient`, the builder class and `DeclaredNullability` for every table (affected tables must not warn; look-alikes never did).

- **D-09 record:** an extra row on the `s4_a01` shape whose String is `"abc" + U+0003 + "xyz"`; the measured wrong-store bytes on gg3 were `(1667391751,'xyz')` with `hex(c.name) = 78797A` [VERIFIED: 23-EVIDENCE.md R-01]. Expect `hex(tupleElement(c,'v')) = 6162630378797A` (measured this session for the same bytes inserted by SQL).
- **DESC-05 regression rows:** keep `ClickHouseHelperClientForkLiveTest.describeTableSkipsRowsAtAnyDepthBelowJson` and `bareSizeSubcolumnsDoNotLeakIntoRootColumns` (lines 308 and 285) untouched; add an x8-style JSON table row in the new class's matrix if the planner wants the live half.
- **Repeatability:** inserts into 33+ tables on a shared container are cheap, but each `put` starts and stops a task; follow `NullableTupleMatrixLiveTest`'s parameterised shape rather than one giant method so a failure names the table.

## One manual `CLIENT_VERSION=V1` run (DESC-07)

`ClickHouseTestHelpers.extractClientVersion()` reads the `CLIENT_VERSION` environment variable (`"V1"` or else `"V2"`) and `ClickHouseHelperClientForkLiveTest.setUp` builds its helper through `getBaseProps()` and `createClient`, which sets `useClientV2("V2".equals(csc.getClientVersion()))` [VERIFIED: `ClickHouseTestHelpers.java:91-98` (`extractClientVersion`) and `:432-441` (`createClient` ... `.useClientV2("V2".equals(csc.getClientVersion()))`), `ClickHouseHelperClientForkLiveTest.java:40-42` (`setUp` calls `ClickHouseTestHelpers.createClient(props)`)]. `ClickHouseHelperClient` always builds both clients (`this.client = createClientV2(); this.server = createClientV1();`, line 106-108), so `queryV2` DDL helpers still work under V1. Command to record (E-entry with the XML counts and the server version):

```bash
CLIENT_VERSION=V1 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*ClickHouseHelperClientForkLiveTest' --tests '*TableTest' --tests '*TupleSizeNamedElementRoundTripLiveTest' --rerun
```

Expect the describe pins to run through `describeTableV1`; the round-trip class under `CLIENT_VERSION=V1` additionally switches the **sink task's** client to V1, which D-11 does not require: run it only if the planner wants it, and record it as an extra, not as a pass condition. If a pre-existing live test is known to fail under V1 for unrelated reasons, record that as a fact rather than widening scope.

## Document edits (DESC-10, the fix commit)

| Document | Edit | Source of the current text |
|----------|------|----------------------------|
| `docs/FORK-DELTA.md` line 47 (the `shouldSkipDescribeRow` row, HLP-02/HLP-03/DESC-05) | Rewrite the sentence "A Tuple element that is itself named `size` or `size0` is dropped by the same rule, which is the known defect of Known costs item 10" into the new rule; the row now names `DescribeTableBuilder`, the decision on `(parent raw type, array depth, row type)` and the cascade guard; add requirement IDs DESC-06..DESC-10 and the new test names to "Pinned by" | read this session |
| `docs/FORK-DELTA.md` line 51 (`DeclaredNullability` row, COLS-10..14) | Remove the clause "(discovery dropped it; backlog 999.4: an element named `size` or `size0`, whose inserts failed with code 32 or 33 ... Known costs item 10)" and "`Table.java` is unchanged"; mention `elementKey` | read this session |
| `docs/FORK-DELTA.md` line 106 ("Adopted wholesale") | Drop `Table.java` from "`Table.java`, `Processing.java` and `KeyToValue.java` | Byte-identical ... (`git diff --quiet v1.7.0 HEAD -- <path>` exits 0 for each)" (the claim becomes false) and add a **new row** in "Connector source" for `Table.java` (one-line change: `SIZE_FIELD_MATCHER` ignores a child only when it is not a declared element) and for `Column.declaredElement` | read this session |
| `docs/FORK-DELTA.md` "Test harness" (line 74 section) | One row for `captureDescribeFixtures` / `DescribeFixtureCapture` / the JSONL fixtures / the drift test, and the build row at line 64 if the planner counts the Gradle task as a "fork build addition" (that row says "Five fork build additions"; `verifyConfluentArchive` has its own row at line 66, the precedent) | read this session |
| `docs/FORK-DELTA.md` item 10 (line 177) | Replace with the closed form used by items 1-3 and 7: "**10. Backlog 999.4 is closed.**" plus what closed it, the scope (all 13 shapes, V2 default route; V1 manual run), and the facts that rows written by earlier releases are not repaired. Keep numbering (item 11 stays 11) | convention read at lines 150-160 |
| `docs/FORK-DELTA.md` line ~215 ("a sync that touches `shouldSkipDescribeRow`, `Table.SIZE_FIELD_MATCHER` or `fromJsonRow` should re-read them first") | Release-history sentence about gg3: leave the history, add that item 10 is closed in gg4 and that a sync touching these must re-read the builder and the one `Table.java` line | read this session |
| `.planning/scripts/fork-anchors.txt` | Rows listed in "Extraction" and "Carrier"; plus builder rows (`new DescribeTableBuilder(` 2, `elementKey(` count measured at execution, `declaredElement` rows). Backs-IDs: the anchor file's own rule is "FORK-DELTA row's Pinned-by ID or this milestone's requirement ID", so use `DESC-07` for the extraction rows and `DESC-08` for the rule, flag, `Table` and key-helper rows. Add a `# Phase 26 (v1.7.0_gg4)` comment line above them, the file's convention. Moves land in the same commit as the code | read this session |
| `CHANGELOG.md` | Top section is `# 1.7.0_gg3, 2026-10-20`; there is **no** `1.7.0_gg4` section yet. Add a new top `# 1.7.0_gg4` section (date and intro paragraph completed by Phase 29) with one `## Bug Fixes` line: a Tuple element named `size`, `size0` or `size<digits>` is kept, inserts into such tables now store the row; no row written earlier is repaired. Keep the gg3 Known Issues text untouched (published history) | read this session |
| `.planning/ROADMAP.md` "Phase 999.4" (lines 472-477) | Mark delivered in the repository's backlog convention (header suffix / a `**Delivered:**` line); `REQUIREMENTS.md` traceability rows DESC-06..10 are flipped by the phase-complete tooling, not by hand | read this session |
| `jacocoForkReport` list (`build.gradle.kts:288-319`) | **Do not edit.** DEBT-17 says the list is edited once in the last code phase (28). Record `DescribeTableBuilder` and the classifier class as inputs for Phase 28 in the evidence file | REQUIREMENTS DEBT-17 |

## Gates and evidence conventions

- **Evidence file:** `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md`, the Phase 25 shape: a header (Phase, Branch `sync/v1.7.0`, Purpose, Shell, Entry format), an `## Index` table (Entry, Requirement, Verdict), then `### E-NN: {title}` sections each with "Commands as run", a `text` fence, a UTC `Timestamp:`, and a final `Verdict: pass|fail|pending (...)|unmeasured (...)`. A red run's verdict is `pass` when the red set equals the prediction. Long output goes to `/tmp/gsd26/exec/`. Natural entries: E-01 spike transcript (harness run, rows, double-row verdict, classifier rule), E-02 prototype run over the JSONL, E-03 extraction green (`testUnit` + full suite, anchors), E-04 red run of the renamed pin on the extraction tree, E-05 fix green, E-06 look-alike snapshot diff, E-07 live round-trip, E-08 the V1 manual run, E-09 phase gate, E-10 rollout facts.
- **Cache-busted `testUnit`:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun` (baseline in E-24 of Phase 25: 860 tests, 858 succeeded, 2 skipped).
- **Full suite:** `rm -rf build/test-results/test` then `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` (baseline: 1303 tests, 1285 succeeded, 0 failed, 18 skipped, 92 XML files, server `26.9.19.9`; the 18 skips are 7 named methods plus the 11 `ClickHouseSinkTaskWithSchemaTest#exactlyOnceStateMismatchTest` cases; counts will rise with the new tests). The phase gate wants the full suite on `latest` x V2 (one run; the two-run gate is Phase 29's).
- **Anchors invariant (measured this session, passes at HEAD):** `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD` printed `invariants: provenance_paths=3 anchor_commits=18 anchor_rows=72 marker_commits=30 anchor_exempt=0 result=PASS` in about 23 s. The self-test `.planning/scripts/verify-merge-invariants-selftest.sh` is slow: it finished with `selftest: no failures` at HEAD after 11 min 53 s wall time in this session (run it in the background or with a long timeout; do it once per gate, not per commit). Run the gate after **each** of the phase's commits, because the script evaluates every commit in the range and an anchor can only change in the commit that rewrites `fork-anchors.txt`. `fork-anchors.txt` and the scripts: only the anchors file is edited; the two scripts are read, never edited.
- **Spotless baseline:** 46 files already violate; do not run `spotlessApply` on the tree. For each new or edited file, copy it into a scratch worktree (`git worktree add --detach /tmp/gsd26/wt-fmt HEAD`), run `./gradlew -q spotlessApply` there, and `cmp` (Phase 25 E-04 recipe). The block is `importOrder()` + `removeUnusedImports()` only, so the practical rule is: no unused imports, the project's import order.
- **After editing `build.gradle.kts`:** run `TestsWorkflowPinGuardUnitTest` (in `testUnit`), because it scans the file's Kotlin code.
- **Rollout facts to record as they land (feed Phase 29, `docs/ROLLOUT-v1.7.0_gg4.md`):** (1) an upgrade needs no clean stop for these tables: the failing inserts (code 32/33, measured for four shapes in E-15) stored nothing, so a retried batch after the upgrade is the first time its bytes are the full tuple; (2) a rollback to `v1.7.0_gg3` needs a clean stop first, because in-flight batches written by gg4 for an affected table carry the full tuple and gg3 would write a different (shorter) block for the same offsets, which defeats block deduplication; (3) a short write that happened to parse on gg3 (R-01's `(1667391751,'xyz')`) is a data tail nothing repairs, and D-09 is the measurement behind that sentence; (4) the new fail-closed WARN text (D-04) is new log material for the note; (5) the DESCRIBE Column list is the RowBinary wire format, so the fix changes the bytes of a retried batch for an affected table. [ASSUMED for (2): the dedup interaction is reasoned from `Processing`'s BEFORE/AFTER design in AGENTS.md, not measured; Phase 29 should either measure it or word it as a precaution.]

## Suggested plan split and commit order (inside the fixed five steps)

| Plan | Step | Content | Commit rule | Done when |
|------|------|---------|-------------|-----------|
| 26-01 | 1 spike | `DescribeFixtureCatalog`, `DescribeFixtureCapture` + `captureDescribeFixtures` task, committed JSONL + `SERVER_VERSION.txt`, `describeRawLines` helper, `DescribeFixtureDriftLiveTest`; evidence E-01/E-02 (transcript, double-row verdict, written classifier rule, throwaway prototype over the JSONL) | **No `src/main` change.** Test tree, resources and `build.gradle.kts` only. The anchors gate runs (anchors unchanged). | Harness run twice gives byte-identical files; drift test green; the rule is written in the evidence file |
| 26-02 | 2 extraction | `DescribeTableBuilder`; V1/V2 shrink; anchors move; the 999.4 pin and a Column-list snapshot test drive the builder; pin still asserts the defect | Own commit; **green on `testUnit` and the full suite before 26-03 starts** (E-03); anchors rows move in this commit | Full suite count equals baseline plus the new tests; anchors PASS |
| 26-03 | 3 fix | Tests first: unit table, renamed/inverted pin, `elementKey` rows, `TableSizeNamedElementTest`; **run them red on the 26-02 tree and record the red set (E-04)**; then `RowDecision`/`ParentShape`, builder wiring, `Column.declaredElement`, `Table` line, `elementKey`; all DESC-10 documents | **One commit** for fix + documents (ROADMAP criterion 5, CONTEXT); a document that cites a measurement is written after the measurement | `testUnit` green; snapshot diff shows only affected tables changed (E-06); anchors PASS |
| 26-04 | 4 live | `TupleSizeNamedElementRoundTripLiveTest` (all fixtures, crafted value, zero-WARN); the manual V1 run (E-08) | own commit | live class green on `latest` x V2; V1 run recorded |
| 26-05 | 5 gate | cache-busted `testUnit`, full suite, anchors + self-test, rollout-facts record (E-09/E-10) | evidence-only commit | all green |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Parsing the parent's type into an element list | A regex or paren-counting splitter over `Tuple(...)` | `ClickHouseColumn.of(parentName, DeclaredNullability.normalise(rawType))` | Backtick names, enum labels with commas/parentheses, nested wrappers; `DeclaredNullability` already relies on it and pins those cases. |
| Tuple element names | A second `getColumnName()` / `i + 1` expression | `DeclaredNullability.elementKey` | One definition (CONTEXT); Phase 28 changes pairing against it. |
| Reading DESCRIBE | A new HTTP reader in the harness or drift test | the shared raw-line helper factored from `NullableRoundTrip.describeRows` | Same settings (`describe_include_subcolumns=1`, JSONEachRow, UTF-8) as production V2. |
| WARN capture | A custom appender | `LogCapture.attach(Class)` | Existing helper snapshots level, message and throwable texts. |
| Typed read-back | `countRows` or iterating `GenericRecord` references | `RowSnapshots.select(chc, sql, aliases...)` / `NullableRoundTrip.selectAndAssert` | Row view is reused by the client; snapshots into a `Map` during iteration. |
| Integer parsing of the `sizeN` suffix | `Integer.parseInt` | build `"size" + k` for `k < depth` and compare strings | No overflow, no `size01` ambiguity. |

**Key insight:** the server tells you everything except "this row is structural"; the only reliable evidence is the shape of the **parent's declared type** plus the row's own type. Anything keyed on names or order rots silently with the next ClickHouse release, which is why the drift premise exists.

## Common Pitfalls

### Pitfall 1: a structural row that leaks into a Tuple becomes a wire-format field
**What goes wrong:** `Table.updateParent`'s TUPLE branch ends `parent.getTupleFields().add(child);` with no name check [VERIFIED: `Table.java:194`]. If `d.null` (`UInt8`, parent `Nullable(Tuple)`) or `e.size0` (the offsets row) is kept, the writer emits an extra element for every row.
**Why:** a rule that is too wide. **Avoid:** fail closed (D-03); the unit table asserts the DDL-derived key list equals `tupleFields` keys, so any leak fails by name. **Warning signs:** a look-alike snapshot changes; `Tuple` field count above the declared count.

### Pitfall 2: the cascade swallows the declared element's children
**What goes wrong:** the structural twin `e.size0` enters `skippedCols`; `e.size0.a` hits the parent-skipped branch and is dropped, reproducing the short tuple one level down (n2). Today `c.size` dropped also drops `c.size.a`, `c.size.b` (x1). **Avoid:** Pattern 3.

### Pitfall 3: a declared `size<N>` that has the same type text as a structural row
**What goes wrong:** `Array(Tuple(size1 UInt64, v String))` (x4) emits `e.size1 : Array(UInt64)`, byte-identical to the `.size1` offsets row of an `Array(Array(...))`. A rule on type text alone drops it. **Avoid:** structural only when `k < arrayDepth` (here depth 1, so `size1` is declared). Pin x4 and x5.

### Pitfall 4: duplicate names, `Table`'s last-wins map and row order
**What goes wrong:** `Table.registerValidColumn` does `allColumnsMap.put(column.getName(), column)` for every row [VERIFIED: `Table.java:77-80`], so for a double row the later row is the one `handleNonRoot` finds as the parent of `e.size0.a`. The server prints structural first, declared second in every table measured, so the declared element wins. The classifier does not depend on order, but this one lookup does. **Avoid:** keep structural `size0`/`size` twins out of the Column list entirely (they are skipped), and pin n2 (`e.size0.a` ends up under the Array(Tuple) element) in the unit table. For `size<N>`, `N >= 1`, the structural twin is still registered as in gg3 (kept for compatibility, Pitfall 19 of the project research); a unit row asserts the declared one is the registered `allColumnsMap` entry.

### Pitfall 5: the DESC-05 ancestor walk must run before the size rule
`j.a.size : UInt8` (x8) is a declared Tuple element inside a typed JSON path and must stay dropped. Keep the ancestor walk ahead of step 4 and keep all existing DESC-05 rows unchanged.

### Pitfall 6: WARN noise
The new WARN fires only for size-named rows whose parent raw type is missing or unparseable. It must not fire for rows under a JSON/Dynamic ancestor (caught earlier by the parent-skipped branch or the ancestor step), for look-alikes, or in the 2-argument `shouldSkipDescribeRow` (pure, no database/table to name). Assert zero WARN across the whole catalog.

### Pitfall 7: `latest` moves
The drift premise and the committed rows are pinned to `26.9.19.9` text. A newer image may reformat `type` text or add a structural family. That is the intended tripwire, but the plan must say how to refresh: `captureDescribeFixtures`, review the JSONL diff, re-run the unit table. A change in a structural family that the rule does not know falls out as `KEEP_STRUCTURAL`/`SKIP_STRUCTURAL` only if the parent is not a declaring Tuple, so the rule is conservative by construction.

### Pitfall 8: scope creep into `fromJsonRow`, `normalise`, or the writer
The classifier calls `DeclaredNullability.normalise` as-is; backslash-bearing names stay Phase 28's. New fixtures use backslash-free names so their pins survive Phase 28. `ClickHouseWriter` is not touched (its `split("\\.")` is a different, known adjacent defect: dotted element names).

### Pitfall 9: extraction changes behaviour by accident
The easy slips: moving `numColumns++` after a skip; adding the `.inner` check inside the builder (then V1/V2 query first); wrapping `JsonProcessingException`; changing the V1 `catch` coverage; reordering `hasDefaults`/`declaredTypes` relative to the unmappable-column `return null`. The extraction commit is verified by an unchanged full suite plus the snapshot test, not by reading.

### Pitfall 10: anchors, documents and evidence
Anchor counts are lines under `grep -cF`; re-measure at execution. The invariant script walks every commit, so the anchors file and the code it pins change in the same commit; a rehearsal in a throwaway worktree is cheap. Verify strings must exclude `gradlew.bat`.

## Code Examples

### JSON line renderer for the existing R-01 pairs (test helper, extraction step)
```java
// Renders a (name, type) pair exactly as ClickHouse prints it in JSONEachRow. The R-01 type strings already hold
// backslash-n as two characters, which is the JSON escape the server sends; fromJsonRow keeps it as text.
private static String describeLine(String name, String type) {
    return String.format(
            "{\"name\":\"%s\",\"type\":\"%s\",\"default_type\":\"\",\"default_expression\":\"\",\"comment\":\"\","
                    + "\"codec_expression\":\"\",\"ttl_expression\":\"\",\"is_subcolumn\":%d}",
            name, type, name.contains(".") ? 1 : 0);
}
```
[Fixture line format verified against a real `clickhouse-local` DESCRIBE this session: `{"name":"e.size0","type":"UInt64","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":1}`.] Do not use this for the captured fixtures (they are the server's bytes); it only re-points the old string arrays at the builder.

### Loading a committed fixture (Docker-free)
```java
static List<String> fixtureLines(String key) throws IOException {
    String resource = "/describe-fixtures/" + key + ".jsonl";
    try (InputStream in = DescribeTableBuilderUnitTest.class.getResourceAsStream(resource)) {
        assertNotNull(in, resource);
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
    }
}
```

### Building the Table from a fixture
```java
DescribeTableBuilder builder = new DescribeTableBuilder("db", "t");
for (String line : fixtureLines("s4_a07")) {
    assertTrue(builder.addRow(line));
}
Table table = builder.build();
```

## State of the Art

| Old approach | Current approach | When changed | Impact |
|--------------|------------------|--------------|--------|
| Name-suffix drop of `.size0`, `.size`, `.null` plus `.size[0-9]+` in `Table` | Decision on `(parent raw type, array depth, row type)` | this phase | declared `size`-named elements round-trip |
| Duplicated 40-line loop in V1 and V2, test copies it | `DescribeTableBuilder.addRow(String)` shared by both and by the tests | this phase | closes 23-REVIEW WR-02 |
| Hand-typed R-01 rows in Java strings | Captured JSONL + live drift premise | this phase | pins the server's real output |

**Deprecated/outdated:** FEATURES.md's statement that client V1 describe is not exercised by CI is too strong (see "Extraction"); its section 1.3 table is confirmed and sharpened by x4/x7.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `JavaExec` runs on the Gradle-configured Java 17 toolchain without an explicit `javaLauncher` | Capture harness | The harness JVM would be the host JDK 21; harmless for the harness, but record `java -version` in E-01 |
| A2 | The server never prints an offsets row with a leading-zero index (`size01`) | Classifier rule | A row would classify as declared if it is not `size<k>` for a `k < depth`; only matters for hostile names |
| A3 | The eight extra shapes (x1..x8) were labelled by hand against the DDL, no executable prototype was run this session | Measured rows | A mislabel would surface when the unit table's DDL-oracle assertion runs; the spike's prototype step is there to catch it first |
| A4 | `ClickHouseColumn.of` parses the raw type of every fixture table (including enum labels such as `'x, size1 UInt8'`) | Classifier / unit table | An unparseable parent takes the D-03 fail-closed path and the fixture would keep failing; the spike should parse every raw type before the classifier plan is written |
| A5 | Rolling back from gg4 to gg3 breaks block deduplication of in-flight batches for an affected table | Rollout facts | Phase 29 wording; either measure it or phrase it as a precaution |
| A6 | The V1 client's `response.records()` yields the same JSONEachRow text as V2's reader | Extraction / V1 run | The manual V1 run is exactly the check; a difference would show as a Column-list mismatch |
| A7 | `Table.addColumn` order (structural twin first, declared second) holds on every ClickHouse the fork targets | Pitfall 4 | A reversed order would register the structural twin last for `size<N>`, `N >= 1` twins; the unit row on `allColumnsMap` and the drift premise detect it |

## Open Questions

1. **Where does the classifier live?**
   - Known: CONTEXT puts the builder in `sink.db.helper` and wants exactly one new public API on `DeclaredNullability` (`elementKey`); the unwrap of `SimpleAggregateFunction` is private there.
   - Unclear: builder-package static methods (duplicating a 5-line unwrap) versus a package-private `ParentShape` in `sink.db.mapping` (shared unwrap, one more fork-owned file and anchor row).
   - Recommendation: `ParentShape` in `sink.db.mapping`, `RowDecision` and `decide` beside `shouldSkipDescribeRow` in `sink.db.helper`.
2. **How is the expected read-back text produced for 33+ shapes?**
   - Known: `NullableTupleMatrixLiveTest` uses hand-written `RowCase`/`Cell` entries; the E-15 tables are `(id Int32, c <type>)` with a tiny type vocabulary.
   - Unclear: hand-written entries per fixture versus a small generator from the DDL type.
   - Recommendation: a catalog entry per fixture with explicit Connect schema, value and expected `toString(c)` text; generators cost more than they save for ~40 shapes.
3. **Should the D-04 WARN carry the exception class?**
   - Known: `FALLBACK_WARNING` logs the class name only because exception text can echo the type.
   - Recommendation: have `ParentShape.of` return a small result or reason string (`"parse error: ParseException"` / `"no raw type"`) and log that; no exception text, no values.
4. **Is the harness FORK-DELTA row a "build" row or a "test harness" row?** Recommendation: Test harness section; the Gradle task is wiring for a test-tree class.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | harness, drift test, live round-trip, spike | yes | `colima list`: `vz` Running; `docker ps` answers | none; bare `colima status` hits the wrong default profile (project memory) |
| `clickhouse/clickhouse-server:latest` | all live work | yes, locally | image `bdb5c911a23d` ("7 days old"), server `26.9.19.9` | `ClickHouseBase` re-pulls `latest` (`PullPolicy.alwaysPull()`), so the suite may move to a newer image than the committed rows |
| JDK / Gradle | build and tests | yes | host OpenJDK 21.0.12.1; toolchain 17; wrapper Gradle 9.8.1 | none |
| `python3` (run with `-I`) | spike prototype over the JSONL | yes | system python3 | any scripting; keep scripts out of the data directory |
| `yq`, `gh` | AGENTS.md matrix lookup; PR tooling | no | - | not needed (`gh` absent on purpose, D-11 of Phase 25) |

**Missing dependencies with no fallback:** none. **Missing with fallback:** none that this phase needs.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json` [VERIFIED: file read this session]. The requirement-to-test mapping the planner needs is in the plan table above and in "Unit table" / "Live round-trip".

## Security Domain

`security_enforcement` is enabled in `.planning/config.json`, ASVS level 1, block on high.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2/V3/V4 (authn, session, access control) | no | not touched |
| V5 Input validation | yes | DESCRIBE rows are server-supplied text parsed by Jackson (`fromJsonRow`) and `ClickHouseColumn.of`; a parse failure fails closed (D-03), never keeps a row |
| V6 Cryptography | no | not touched |
| V7 Error handling and logging | yes | the new WARN names database, table, parent column and raw type only; no record value, no element value, no exception text (class name at most) |
| V14 Configuration | yes | the harness takes its output directory from a Gradle argument; fixtures are committed and read-only at test time |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Pathological or hostile type text reaching `ClickHouseColumn.of` | Denial of service | only size-named rows trigger a parse; same parser and same inputs as `DeclaredNullability` already handle per root |
| Table/column names with control characters in a log line | Tampering (log forging) | the same metadata already appears in `FALLBACK_WARNING` and `UNPAIRED_ELEMENT_WARNING`; no new class of exposure |
| A leaked structural row corrupting stored data | Tampering | fail-closed rule, DDL-oracle unit assertion, look-alike snapshots, live typed read-back |
| Container and file handling in the harness | Elevation / tampering | try-with-resources for the container and the client; writes only below the given directory; no secrets (the test password is the existing `test_password`) |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `ClickHouseHelperClient.java` (440-663), `Table.java`, `DeclaredNullability.java`, `Column.java` (34-300), `ClickHouseFieldDescriptor.java`, `ClickHouseWriter.java` (780-810), `ClickHouseBase.java`, `ClickHouseTestHelpers.java` (60-110, 377-441), `NullableRoundTrip.java`, `RowSnapshots.java`, `LogCapture.java`, `NullableTupleMatrixLiveTest.java` (1-640), `ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `ConfluentArchiveCheck.java`, `build.gradle.kts`, `.planning/scripts/fork-anchors.txt`, `fork-anchor-exemptions.txt`, `verify-merge-invariants.sh` (1-80), `docs/FORK-DELTA.md`, `CHANGELOG.md`, `.planning/{ROADMAP,REQUIREMENTS,STATE,config}.json|md`.
- Phase evidence: `23-EVIDENCE.md` R-01, `24-COMMAND-EVIDENCE.md` E-15 (fixture DDLs and outcomes), `25-COMMAND-EVIDENCE.md` E-21, E-24, E-04 (gate and baseline conventions).
- Live measurements this session: `docker run --rm -i clickhouse/clickhouse-server:latest clickhouse-local --multiquery` (version `26.9.19.9`), outputs in `/tmp/gsd26-research/`.
- `.planning/research/{SUMMARY,ARCHITECTURE,FEATURES,PITFALLS}.md` (sections 2.x, 1.1-1.4, Pitfall 5, 19).

### Secondary (MEDIUM confidence)
- ClickHouse DESCRIBE and subcolumn behaviour as recorded in FEATURES.md [D] entries (not re-fetched; the live rows supersede them).

### Tertiary (LOW confidence)
- none beyond the `[ASSUMED]` items in the log.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new is added.
- Architecture / classifier rule: HIGH for the rule on the measured rows; MEDIUM for the carrier and package placement (design choices, not yet compiled).
- Pitfalls: HIGH for 1-5, 9 (derived from read code and measured rows); MEDIUM for 7 (future server behaviour).
- Harness wiring: MEDIUM (precedent read, `runtimeClasspath` and toolchain default not run).

**Research date:** 2026-10-21
**Valid until:** until the `latest` image changes DESCRIBE text (re-run `captureDescribeFixtures`) or Phase 27/28 edit the same files; otherwise about 30 days.
