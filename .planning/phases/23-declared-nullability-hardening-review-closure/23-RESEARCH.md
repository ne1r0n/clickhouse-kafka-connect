# Phase 23: Declared-Nullability Hardening & Review Closure - Research

**Researched:** 2026-10-20
**Domain:** fork-only `DeclaredNullability` pass (Java 17, ClickHouse client V2 0.9.6), its Docker-free and live tests, and review-disposition bookkeeping
**Confidence:** HIGH (every claim about behaviour was run live on ClickHouse `latest` 26.9.19.9 x client V2 or against the unchanged tree this session)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- **Order inside the phase (roadmap):**
  1. DEBT-11's shared literal.
  2. COLS-12, with the IN-02 flip in the same commit.
  3. DEBT-10.
  4. COLS-13, which needs the live capture first.
  5. COLS-14.
  6. DEBT-12, last.

  Each step lands as its own commit(s), with `testUnit` green at every commit.
- **Fork rules:**
  - Any behaviour change carries its FORK-DELTA clause (the COLS-10 row, `docs/FORK-DELTA.md:51`) and its `fork-anchors.txt` row in the same commit (Phase 19 D-13 pattern).
  - The anchors invariant `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` passes at every commit, with Phase 21 D-01's SHA-pinned exemption list.
- **Tests:**
  - Never edit upstream-identical tests (`ColumnTest`, `TableTest`). Add fork-only classes beside them.
  - Existing tests are never deleted or weakened.
  - A flipped test keeps its method and carries a written rationale, in the Phase 19 D-11 flip format.
- **Parse cost:** the type string is parsed once per root `Column` per DESCRIBE, never per record.
- **COLS-14 doesn't change pairing** for any shape that pairs today. Phase 19's live `Nullable(Tuple)` matrix (`NullableTupleMatrixLiveTest`) and `SimpleAggregateTupleRoundTripLiveTest` stay green.
- **Content rule (Phase 18):** every WARN this phase adds or changes carries metadata only: column, database, table, raw declared type, element names from the DDL, and exception class names. It never carries an exception message or throwable, and never a record value.
- **"Seen red" (Phase 21 D-07):** the red run is recorded in `23-EVIDENCE.md`, then reverted. Nothing red is committed, and the fix lands green.
- **Process rules (Phase 21 D-14, Phase 22 D-16):**
  - Gradle runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`.
  - `spotlessApply` runs on touched files only, with no tree-wide reformat.
  - A touched file already in the 46-file baseline-red set must have no *new* violations, meaning the same `spotlessApply` +/- counts as its baseline. Every other touched or new file must have an empty `spotlessApply` diff. Research checks which of this phase's files are in the baseline set.
- **Behaviour ledger:** create `.planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md` in the `22-BEHAVIOUR-LEDGER.md` / `18-BEHAVIOUR-LEDGER.md` format. Each operator-visible change gets a row in the same commit as the change, for Phase 24's ROLL-03. Expected rows:
  - the new unpaired-element WARN (D-01, D-02), including that a Tuple element named `size`/`size0`/`null` now triggers it if D-03 confirms the shape;
  - a backslash-n inside an element name or enum label now pairs, so the fallback WARN no longer fires for it and the declared nullability applies (D-10).

  `VERSION` and `CHANGELOG.md` stay untouched; Phase 24 writes them.
- **Human gates:** every decision gate needs the user's own explicit answer, and a relayed approval is refused. Checkpoints run orchestrator-inline (milestone rule).
- **Phase gate:** cache-busted `testUnit` (`--rerun`), a full suite on `latest` × V2, and the anchors invariant.

### Unpaired declared element (COLS-14, 19-REVIEW IN-04)
- **D-01:** **Rule:** a Tuple Column never legitimately lacks a declared element. When one is missing, discovery dropped it before this pass ran.
  - The pass logs **one metadata-only WARN**. It names the root column, the database, the table, the raw declared type and the unpaired declared element names.
  - It **still applies** the declared nullability to every element that did pair, and `apply` returns true. This is not a fallback: falling back to DESCRIBE's nullability can't repair the missing element's bytes, and it would bring back the double-flagging this pass exists to remove.
  - `missingDeclaredElementDoesNotShiftPairing` stays green with its body unchanged.
  - The rule is recorded in the `DeclaredNullability` Javadoc, the FORK-DELTA COLS-10 row and the 19-REVIEW IN-04 disposition row.
  - **Reversibility:** costly. Once Phase 24 publishes, operators may grep or alert on the new WARN text.
- **D-02:** The WARN is a **new constant**, separate from `FALLBACK_WARNING`, because that text says "keeping the nullability DESCRIBE reported", which would be false here.
  - It fires **on every DESCRIBE** for each affected root, with no dedup and no static state. This matches `FALLBACK_WARNING`, and Phase 19 D-10 deliberately deleted the old dedup set.
  - Its column assertion follows D-16 and D-17.
- **D-03:** **The `.size` suspicion (found while scouting for this discussion):** `ClickHouseHelperClient.shouldSkipDescribeRow` drops every DESCRIBE sub-column row ending in `.size`, `.size0` or `.null`. A Tuple element *named* `size`, `size0` or `null` (e.g. `Tuple(size UInt32, name String)`) would then never become a Column, and the writer (`ClickHouseWriter` ~L803 iterates `getTupleFields()`) would write a short tuple.
  - **Research confirms or refutes this live on `latest` × V2.** Capture into `23-EVIDENCE.md`:
    - the raw DESCRIBE JSON rows;
    - the Column tree discovery builds;
    - the insert outcome, read back with typed SQL: a server error code, or a silently wrong stored value. The evidence states plainly which one.
  - **No fix in Phase 23.** `ClickHouseHelperClient` is not touched. A new ROADMAP backlog entry **999.4** records the defect, quotes the evidence (severity included) and sketches the fix (see Deferred).
  - If research refutes it (DESCRIBE emits no such row, or the skip keeps it), record that in evidence and add no 999.4. D-01 stands either way.
- **D-04:** **Pin for the `.size` shape:**
  - A Docker-free test (beside `ClickHouseHelperClientForkUnitTest`'s discovery cases) feeds the live-captured DESCRIBE rows through `shouldSkipDescribeRow` and `buildDescribedTable`, as `describeTableV2`'s loop does. The existing `discover(...)` helper doesn't apply the skip, so the test needs a variant that does.
  - It asserts that the Column lacks the element, that the new WARN fires once naming it, and that `apply` returns true with the paired elements applied.
  - It is labelled as known defect 999.4 in its `@DisplayName` and comment, so the 999.4 fix flips it later.
  - There is no live insert pin. The insert outcome lives in evidence and in the 999.4 entry.

### Null-safety (COLS-12, 19-REVIEW WR-01, IN-02 flip)
- **D-05:** The fix is **entirely in `DeclaredNullability.java`**.
  - `containsTuple` returns false for a Column with a null type, at whatever depth it recurses to.
  - `buildDescribedTable` and its anchors rows (`DeclaredNullability.apply( 1`, `buildDescribedTable( 3`) are untouched.
- **D-06:** **Null root:** `apply(null, …)` returns false **before** the `try`, with no WARN and no log event at all, matching the existing `declaredType == null` early return. The column name for any WARN is computed before the `try`, so the handler can never dereference the root. A Docker-free test asserts false and zero log events.
- **D-07:** **Every null in the tree that throws an NPE today is a structure mismatch.** That covers a null type at any depth, a null entry in `tupleFields`, and a null field or tuple name.
  - The result is one WARN with the `structure mismatch` reason, `apply` returns false, and the root is untouched. Each shape gets a Docker-free pin.
  - The `catch (RuntimeException)` is then left for genuinely unexpected failures, such as `ClickHouseColumn.of` rejecting the type.
  - Null handling that already exists is kept as it is: an ARRAY with a null element type is a mismatch, and a MAP with a null value type pairs. Changing either would change a shape's outcome today.
- **D-08:** **The flip of `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning`:**
  - It keeps its null-type Column input.
  - Its NullPointerException class-name assertion becomes an assertion of the `structure mismatch` reason.
  - Its rationale comment states that `collect` gaining a null check is 19-REVIEW IN-02's re-open trigger, and that the contract (fail open with one metadata-only WARN) is unchanged.
  - It adopts the sentinel root (D-16).
- **D-09:** **The catch-path pin:**
  - A new Docker-free test uses an input that is invalid by design: a declared type that `ClickHouseColumn.of` rejects.
  - The test first calls `ClickHouseColumn.of` on that string itself to learn which class is thrown. It then asserts that the WARN ends with that simple class name in delimited form (e.g. `(<SimpleName>)`), that there is no throwable, and that there is exactly one log event.
  - No client-library exception class is hard-coded. The test uses the sentinel root.

### Backslash-n inside names and labels (COLS-13, 19-REVIEW IN-03)
- **D-10:** `normalise` becomes **one quote-aware pass**. It keeps its signature `public static String normalise(String)`.
  - It tracks backtick-quoted identifiers and single-quoted literals (enum labels), with the escape rules the live capture shows.
  - It replaces the escaped newline with a space only outside quotes.
  - Every shape that normalises today normalises to the same string, so the multi-line twin tests and the Phase 19 live matrix stay green.
- **D-11:** **The live capture comes first.**
  - Capture what `latest` × V2 DESCRIBE returns for a backslash-n inside a backtick-quoted Tuple element name, and for one inside an enum label in a Tuple. Record both the raw JSON row and the string after `fromJsonRow` in `23-EVIDENCE.md`.
  - The Docker-free pin's input is that captured string, verbatim.
  - Pairing succeeds, declared nullability applies, and no fallback WARN fires (asserted with `LogCapture`).
- **D-12:** **Permanent live premise** on `latest` × V2. It DESCRIBEs such a table and asserts:
  - the raw type equals the Docker-free pin's input literal, so the two can't drift (IN-03's re-open trigger is a change in how `fromJsonRow` escapes, and `latest` drifts);
  - one row round-trips with typed read-back;
  - no fallback WARN fires.

  Placement: an existing live class if one fits, e.g. beside `NullableTupleMatrixLiveTest`'s multi-line premise case. If it lands as a matrix cell, it follows that class's twin-table convention.
- **D-13:** **Contingency:** if the capture shows a quote-aware scan can't tell the server's line break from a backslash-n inside quotes (identical escaping even inside quotes), execution **stops at a blocking checkpoint**. The checkpoint runs orchestrator-inline, needs the user's own answer, and presents the captured bytes. The user then chooses another rule, or closing IN-03 as a pinned fail-open with a written reason (Phase 22 D-03/D-13 pattern).

### Shared literal and WARN sentinel (DEBT-11, DEBT-10, 19-REVIEW IN-01, IN-02, WR-02)
- **D-14:** **`DeclaredNullability.ESCAPED_NEWLINE` becomes `public`** and is the only definition in the tree.
  - The three test copies are removed: `ESCAPED_NEWLINE` in `NullableTupleMatrixLiveTest` and in `SimpleAggregateTupleRoundTripLiveTest`, and `SERVER_NEWLINE` in `DeclaredNullabilityTest`. Every test references the production constant instead, including the COLS-13 pin and the live premise.
  - Independence from production rests on the live premise (D-12), where the server is the oracle.
  - This lands first (order step 1), before COLS-13 adds another use.
- **D-15:** **IN-02 cleanups:**
  - Drop `NullableRoundTrip.normaliseDeclaredType`. Its one call site (`NullableTupleMatrixLiveTest` ~L730) calls `DeclaredNullability.normalise` directly.
  - In `ClickHouseHelperClientForkUnitTest`, `DISCOVERED_TABLE` moves to the constants block. It is derived from shared database and table constants that `discover(...)` also uses, so the `"db"`/`"t"` literals are no longer duplicated.
- **D-16:** **A dedicated sentinel root** (e.g. `nullability_root`), used **only** by tests that assert WARN text:
  - in `DeclaredNullabilityTest`: `unparsableDeclaredTypeFailsOpenWithOneMetadataWarning`, `structureMismatchFailsOpenWithOneWarning` and the flipped D-08 test;
  - the fixture of `ClickHouseHelperClientForkUnitTest.fallbackWarningNamesDatabaseAndTable`;
  - every new WARN test this phase adds (D-01/D-02, D-04, D-07, D-09).

  Each asserts the delimited form `"column " + ROOT + " of table"`, or the new WARN's equivalent delimiters. The other tests keep their `c` rows.
- **D-17:** **Seen red against both mutations.** Every WARN-content column assertion, existing and new, including the unpaired-element WARN's, is run red against:
  1. the column argument dropped from the `LOGGER.warn` call, with the placeholder kept (every value shifts one slot);
  2. the column placeholder dropped from the format text, with the argument kept. SLF4J then puts the name in the next slot, which only the delimited form catches.

  Each red run is recorded in `23-EVIDENCE.md` and reverted. Nothing red is committed.

### Review closure (DEBT-12). Not discussed in depth; the user accepted this proposal at "ready for context"
- **D-18:** DEBT-12 runs last. It adds closure pointers (commit SHA plus test or evidence name) to the rows of the archived `17-REVIEW-DISPOSITION.md` and `19-REVIEW-DISPOSITION.md` without rewriting any finding. It follows the files' own rules: a disposition set by hand, the reason in the Source cell, and the frontmatter `open` count updated.
  - **17-REVIEW:**
    - WR-01 → `fixed`, by Phase 21 D-09's `exclude-paths` guard (21-04) plus OPS-02's UAT pass on 2026-10-19 (`21-UAT.md`).
    - IN-01 → `fixed`, by CI-18 (21-04).
    - IN-02 → `fixed`, by CI-19 (21-04).
    - IN-03 → `skipped`, with the roadmap's reason: "leave `Lz4RoundTripLiveTest`'s jar-origin check, cross-referenced to `TestRuntimeClasspathLz4UnitTest`, with no new origin assertions". The live test is neither deleted nor weakened.
  - **19-REVIEW:**
    - WR-01 → `fixed` (COLS-12).
    - WR-02 → `fixed` (DEBT-10).
    - IN-01 → `fixed` (DEBT-11).
    - IN-02 → `fixed` (DEBT-11 plus the D-08 flip).
    - IN-03 → `fixed` (COLS-13), or whatever the user decides at D-13.
    - IN-04 → `fixed` (COLS-14, the D-01 rule, with a pointer to 999.4 if D-03 confirms the shape).
    - WR-03 and CR-01 are already `fixed` and are left untouched.
  - The Phase 21 SHAs come from `21-04-SUMMARY.md` and `21-VERIFICATION.md`.
  - The archived `20-MILESTONE-CLOSE-RECORD.md` is not edited.

### Claude's Discretion
- The new WARN constant's name and exact wording, within D-01's fields. It must not contain the sentinel text, and must not say "keeping the nullability DESCRIBE reported".
- How the quote-aware scanner is structured (a char loop or a small state machine). It runs once per DESCRIBE per root, not on the hot path.
- The sentinel root's exact name.
- Whether the unpaired-element WARN gets its own sibling test or `LogCapture` assertions added beside `missingDeclaredElementDoesNotShiftPairing`. The preference is a sibling, so the pinned test's body stays byte-identical.
- Where the live premise lands (D-12), and whether the D-04 skip-aware discovery helper is a new helper or a parameter on `discover(...)`.
- The plan split, within the fixed order.
- The layout of `23-EVIDENCE.md`. Precedent: the Phase 21/22 evidence files with red-before/green-after runs and gate output.
- How each disposition's Source cell words the pointer, and whether it keeps a "previously deferred (20-MILESTONE-CLOSE-RECORD.md)" note.

### Deferred Ideas (OUT OF SCOPE)
- **Backlog 999.4 (created in this phase if D-03 confirms the shape):** `shouldSkipDescribeRow`'s `.size`/`.size0`/`.null` suffix skip drops a Tuple element with that name, so the writer writes a short tuple.
  - Fix sketch: narrow the skip so it keeps a row whose parent is a Tuple, while still skipping the structural `.null`/`.size0` rows of `Nullable`/`Array` columns (DESC-05 regression check).
  - The fix carries a FORK-DELTA row, an anchors row and a live round-trip test, and flips the D-04 pin.
  - It touches `ClickHouseHelperClient`, an upstream-region file, so it gets its own phase.
- **21-REVIEW (7 findings, all `open`) and 22-REVIEW (WR-01 and IN-01..IN-05, `open`):** these fall outside DEBT-12's 17/19-REVIEW scope. They go to Phase 24 or the milestone close. PROJECT.md → Active already tracks the Phase 21 and Phase 22 follow-ups.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COLS-12 | `apply` and `containsTuple` total for a null root and a null-type Column; IN-02 flip | R-03 matrix: today `apply(null)` throws NPE (handler dereferences root) and `containsTuple` throws NPE for a null type at any depth; prototype fix in "Code Examples" passes all pins; flip target confirmed red under the fix (Evidence R-04) |
| COLS-13 | backslash-n inside a quoted element name or enum label survives `normalise` | R-02 raw bytes, escape rules (even backslash runs inside quotes), prototype scanner proven on 9 captured shapes and live end-to-end (red on `main`, green with the fix) |
| COLS-14 | rule for an unpaired declared Tuple element | D-01 locked; R-01 confirms the `size`/`size0` shape live; prototype shows the WARN per shape; **three existing tests collide with D-01 (Open Question 1)** |
| DEBT-10 | WARN-content assertions that can fail; sentinel root | Inventory of the 5 assertion sites; both mutations are mechanically valid (see "WARN mutation mechanics") |
| DEBT-11 | one backslash-n literal | Inventory: 1 production + 4 test definitions/uses to remove; all call sites listed |
| DEBT-12 | closure pointers in both archived disposition files | Current rows/frontmatter quoted verbatim; Phase 21 SHAs resolved to full 40-hex |
</phase_requirements>

## Summary

Phase 23 is a small, well-bounded change to one fork-only file (`DeclaredNullability.java`, 168 lines) plus test and bookkeeping work. Everything the CONTEXT hands to research was run live this session on ClickHouse `latest` (26.9.19.9, image created 2026-10-14) with client V2 against the unchanged tree, and a prototype of the whole production change (D-01, D-06, D-07, D-10, `public` literal) was compiled outside the repo and run against the existing Docker-free tests and the five live classes that exercise the pass. The prototype leaves all 68 live tests green and turns exactly four Docker-free tests red: the D-08 flip target (expected) and three that the CONTEXT did not predict (Open Question 1).

The main findings that change planning: (1) D-03 is **confirmed for `size` and `size0`** and **refuted for `null`** (the server rejects an element named `null`), and the outcome is a server error (`CANNOT_READ_ALL_DATA`, code 33, no row stored) for ordinary data with a silently wrong stored value reachable for crafted bytes; (2) a quote-aware scan **can** distinguish the server line break (a lone backslash before `n`, outside quotes) from a backslash-n in a name or label (always an even backslash run inside quotes), so the D-13 checkpoint is not triggered; (3) a real newline in a quoted name is broken by `normalise` today as well as a literal backslash-n, while the enum-label half never changed a pairing outcome (it is visible only in the `normalise` output string); (4) D-01 makes three existing fixture-only tests red because their input is a Tuple root row with no element rows.

**Primary recommendation:** implement the prototype in "Code Examples" in the fixed order, flip the three collided tests in the Phase 19 D-11 format only after the user confirms Open Question 1, take the Docker-free pins' inputs from `23-EVIDENCE.md` R-02 (already captured), and use the standing live premise (D-12) as the drift oracle instead of a second capture.

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` -> `AGENTS.md` and `.claude/CLAUDE.md` [VERIFIED: files read this session]:
- Never delete or weaken existing tests; only add tests. Test names must be descriptive. Negative and edge tests required.
- Java 17, Lombok, SLF4J (WARN for operator-visible degradations), JUnit 5 + Mockito; Spotless (Google Java Format) must pass; run `spotlessApply` after changes.
- Do not log secrets or record data; error messages must be actionable with context.
- High-risk files: `Processing.java`, `KeeperStateProvider.java`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `RangeContainer.java`. None is touched by this phase.
- Do NOT commit without being asked, do not change PR text. (In GSD execution, commits are made by the executor per the plan; this research commits nothing.)
- Behaviour preservation: the fork runs in production; code changes limited to genuine fixes; keep changes additive for upstream rebases; reuse existing test helpers (`ClickHouseBase`, `ClickHouseTestHelpers`).
- GSD rule: file-changing work goes through a GSD command.
- Stale statements in `.claude/CLAUDE.md` (do not copy into plans): Gradle is **9.6.1** [VERIFIED: gradle/wrapper/gradle-wrapper.properties:3 `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`], JUnit Jupiter is **5.14.4** and the ClickHouse client **0.9.6** [VERIFIED: test runtime classpath resolved via a Gradle init script this session].

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Declared type text normalisation (`normalise`) | Connector discovery layer (`sink.db.mapping`) | — | Pure string transform on the type DESCRIBE returned, once per root per DESCRIBE |
| Pairing declared tree to Column tree, nullability overwrite | Connector discovery layer (`DeclaredNullability`) | `ClickHouseHelperClient.buildDescribedTable` (caller) | Runs after `Table` is built, before it is published; never per record |
| Dropping `.size`/`.size0`/`.null` DESCRIBE rows | Connector discovery layer (`ClickHouseHelperClient.shouldSkipDescribeRow`) | — | Upstream-region file; **not touched** (D-05); defect goes to backlog 999.4 |
| Tuple byte layout on insert | Writer (`ClickHouseWriter` ~L803) | ClickHouse server (parses RowBinary) | The writer iterates `getTupleFields()`; a missing Column shortens the body; the server is the oracle |
| Escaping of the type string | ClickHouse server (JSONEachRow) + `ClickHouseFieldDescriptor.fromJsonRow` | — | `fromJsonRow` keeps JSON escapes verbatim as text; `normalise` must cope, `fromJsonRow` must not change |
| Operator-visible WARN | Connector logging (SLF4J) | Behaviour ledger / FORK-DELTA | Metadata-only content rule |
| Review closure records | Planning docs in the archive | — | Hand-set dispositions, no code |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:clickhouse-data` (`ClickHouseColumn.of`) | 0.9.6 [VERIFIED: classpath jar `clickhouse-data-0.9.6.jar`] | Parses the declared type | Already used by the pass; the pass must not re-implement a type parser |
| SLF4J + reload4j | existing | WARN output; `LogCapture` (test) attaches a reload4j appender | Existing convention |
| JUnit Jupiter | 5.14.4 [VERIFIED: classpath] | Tests | Existing |
| Testcontainers ClickHouse | 2.0.5 [VERIFIED: classpath `testcontainers-clickhouse-2.0.5.jar`] | Live premise | Existing `ClickHouseBase` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `LogCapture` (`src/test/.../sink/util/LogCapture.java`) | in repo | `attach(Class)`, `events()`, `events("WARN")`, `hasThrowable()`, `allTexts()` | Every WARN assertion |
| `ColumnNullabilityPaths.byPath` | in repo | flatten nullability by dotted path | Assertions on Column trees |
| `RowSnapshots.select(chc, sql, aliases...)` | in repo (`src/testFixtures`) | typed SQL read-back | D-12 live premise |
| `NullableRoundTrip` (`createTable`, `describeRows`, `putAndStop`, `selectAndAssert`, `Twin`) | in repo | live helpers | D-12 live premise |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Quote-aware scan (D-10, locked) | A lookbehind that replaces a backslash-n only when it is not preceded by another backslash | Equivalent on every captured shape because the line break is always a lone backslash, but D-10 locks the quote-aware pass; mention only to the reviewer |
| Fixing `fromJsonRow` | none | Out of scope: `ClickHouseHelperClient`/`ClickHouseFieldDescriptor` are upstream-region and D-05 forbids touching them |

**Installation:** none. No new dependency; no `package.json`/Gradle change.

**Version verification:** versions above were read from the resolved test runtime classpath (`/tmp/gsd23/cp.txt`, produced with `./gradlew -I <init script> gsd23PrintCp --offline`), not from training data.

## Package Legitimacy Audit

No external package is installed or added by this phase, so the Package Legitimacy Gate does not apply.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 DESCRIBE TABLE ... FORMAT JSONEachRow  (describe_include_subcolumns=1, ClickHouse server)
        |   one JSON line per column and per sub-column; type text is JSON-escaped
        v
 ClickHouseFieldDescriptor.fromJsonRow(line)      doubles every backslash, Jackson decodes
        |   => getType() holds the JSON text VERBATIM: newline = \n (1 backslash),
        |      SQL backslash inside a name = 4 backslashes + n, real newline in a name = 2 + n
        v
 describeTableV2 loop  --  shouldSkipDescribeRow(row, columnsSoFar)   (drops *.null, *.size0, *.size
        |                                                              and sub-rows below JSON/Dynamic)
        |   declaredTypes.put(root name, raw type)   Column.extractColumn(row)
        v
 buildDescribedTable(db, table, hasDefaults, columns, numColumns, declaredTypes)
        |   new Table(...)  -> root Columns with sub-columns paired by upstream Table
        |   for each root: if DeclaredNullability.containsTuple(root)  [must be total: COLS-12]
        v
 DeclaredNullability.apply(root, rawType, db, table)
        |  null root / null declared type -> return false, no log            (D-06)
        |  normalise(rawType)             -> quote-aware, line break -> space (D-10)
        |  ClickHouseColumn.of(name, text)-> parsed tree; throws -> catch -> FALLBACK WARN (class name only)
        |  collect(root, parsed, assignments, unpaired)
        |       null in tree  -> structure mismatch -> FALLBACK WARN, return false        (D-07)
        |       no match      -> structure mismatch -> FALLBACK WARN, return false
        |  apply assignments (all or none)
        |  unpaired declared elements? -> UNPAIRED WARN, still return true               (D-01)
        v
 Table published to ClickHouseWriter.mapping  --> Tuple write iterates getTupleFields()
```

### Recommended Project Structure
```
src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/
└── DeclaredNullability.java            # the only src/main file this phase changes
src/test/java/com/clickhouse/kafka/connect/sink/
├── NullableRoundTrip.java              # drop normaliseDeclaredType (D-15)
├── NullableTupleMatrixLiveTest.java    # D-12 live premise, uses the shared literal
├── SimpleAggregateTupleRoundTripLiveTest.java   # drop the ESCAPED_NEWLINE copy
└── db/
    ├── helper/ClickHouseHelperClientForkUnitTest.java   # D-04 skip-aware pin, D-15, D-16 fixture
    └── mapping/
        ├── DeclaredNullabilityTest.java                 # fork-only; new pins and the D-08 flip
        └── <new public fixture class>                   # shared captured type strings (see Pitfall 6)
```

### Pattern 1: Quote-aware normalise (D-10)
**What:** one linear pass over the type text with a one-character quote state (`` ` `` or `'`). Outside quotes a backslash run of length exactly 1 followed by `n` becomes a space. Inside quotes a backslash run is copied verbatim together with the next character; that next character closes the quote only when the run length divided by two is even (JSON doubles each server-level backslash, so runs inside quotes are always even in the captured data).
**When to use:** every call of `normalise` (called once per root per DESCRIBE, off the hot path).
**Evidence:** [VERIFIED: live capture + prototype, 23-EVIDENCE.md R-02]. The same scan reproduced the expected text for literal backslash-n names, real-newline names, enum labels, nested names, `Nullable(Tuple(...))` and a name ending in a backslash.
**Example:** see "Code Examples".

### Pattern 2: All-or-none with a trailing diagnostic (D-01)
**What:** `collect` accumulates `Assignment`s and, new, the unpaired declared element paths. Assignments are applied only after `collect` succeeded; the unpaired WARN is logged after the assignments are applied and does not change the return value.
**Why:** keeps the existing all-or-none shape and avoids a half-updated root (existing comment in `apply`).

### Pattern 3: Sentinel root for WARN-content assertions (D-16)
A column name that cannot occur in the fixed WARN text, asserted with its delimiters: `"column " + ROOT + " of table"`. The fixed text of `FALLBACK_WARNING` is `"Could not take element nullability for column {} of table {}.{} from its declared type {} ({}); keeping the nullability DESCRIBE reported for it"` [VERIFIED: DeclaredNullability.java:35-36].

### Anti-Patterns to Avoid
- **Changing `fromJsonRow` or `shouldSkipDescribeRow` in this phase:** D-05; they are upstream-region and go to backlog 999.4.
- **Falling back (returning false) on an unpaired element:** D-01 explicitly rejects it; DESCRIBE's nullability reintroduces the double flag.
- **Logging an exception message or throwable from the pass:** the message can echo the type; only the class name is allowed.
- **Dedup/static state for the unpaired WARN:** D-02.
- **Counting on `HashMap` iteration order for the unpaired names:** `byKey` is a `HashMap` today [VERIFIED: DeclaredNullability.java:149]; use `LinkedHashMap` so the WARN text is stable and the tests can assert a delimited name list.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Parsing the declared type | a second type parser or regex | `ClickHouseColumn.of` | Already the pass's parser; the scanner only fixes the escape before it |
| Capturing log events | a custom appender | `LogCapture` | Existing, restores logger level, snapshots throwables |
| Typed read-back | row counts | `RowSnapshots.select` with positional `tupleElement(c, 1)` | Avoids writing a backslash name in SQL; project rule is typed read-back |
| JSON escape handling | decoding the type text again | leave `fromJsonRow` alone and handle its output | D-05 |

**Key insight:** the only custom code worth writing is the ~40-line scanner; everything else reuses existing parsing and test infrastructure.

## Runtime State Inventory

Not a rename/refactor/migration phase (it adds behaviour to a fork-only pass and edits tests and docs). Explicitly:
- **Stored data:** none. The pass changes in-memory `Column.isNullable` flags per DESCRIBE, nothing persisted.
- **Live service config:** none.
- **OS-registered state:** none.
- **Secrets/env vars:** none.
- **Build artifacts:** none (the build dir is regenerated; the archived disposition files are plain markdown).

## Research Jobs Answered

### Job 1, D-03: Tuple element named `size`, `size0`, `null`  [VERIFIED: live, 23-EVIDENCE.md R-01]
- **`size` and `size0`: CONFIRMED.** Raw rows `{"name":"c.size","type":"UInt32",...,"is_subcolumn":1}` and `{"name":"d.size0","type":"Nullable(Int32)",...}` are emitted and then dropped by the suffix test [VERIFIED: ClickHouseHelperClient.java:478 `if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {`]. The discovered tree for `c Tuple(size UInt32, name String)` has only `c.name`. The same happens for `Nullable(Tuple(size0 ...))` and `Array(Tuple(size ...))`.
- **`null`: REFUTED.** The server refuses the DDL: `Code: 36 ... Tuple element name 'null' is reserved because it would conflict with the subcolumn name used for Nullable null maps ... (BAD_ARGUMENTS)`. So the ledger/999.4 text should say `size` and `size0`, not `null`, on `latest`. (An older server that allows `null` is out of the supported target: ClickHouse `latest` only.)
- **Insert outcome, stated plainly:** ordinary data -> **server error, code 33 `CANNOT_READ_ALL_DATA` ("Bytes read: 3. Bytes expected: 4"), no row stored**, surfaced as a `RuntimeException` chain from `put`. A **silently wrong stored value is also reachable** with crafted bytes: a name `"abc" + U+0003 + "xyz"` was stored as `(1667391751,'xyz')` with `put` returning normally. 999.4 severity: high for the table (every insert fails), with a data-integrity tail.
- **The structural rows that must keep being skipped** (for the 999.4 fix sketch and the regression check): `c.name.size` (String sub-column), `d.null`, `d.other.size`, `e.size0` (Array lengths) and `e.v.size`. Note `e.size0` is structural while `e.size` is the real element, so the 999.4 rule cannot be "keep when the parent is a Tuple" alone for the Array-of-Tuple shape; its parent is `e`, an Array whose element is a Tuple. This is for the 999.4 entry, not for Phase 23.

### Job 2, D-11 / D-13: backslash-n capture  [VERIFIED: live, 23-EVIDENCE.md R-02]
Escape rules the scanner needs (all for the Java string returned by `getType()`, which equals the server's raw JSON text for the type):
- the server line break between Tuple elements: **1 backslash + n, outside quotes**;
- a SQL backslash inside a backtick name or single-quoted label: **2 backslashes** (JSON doubled), so a literal backslash-n is **4 backslashes + n** and a real newline is **2 backslashes + n**;
- an escaped quote inside quotes is **2 backslashes + quote** (run length 2, half = 1, odd, so it does not close); a literal backslash right before the closing quote is **4 backslashes + quote** (half = 2, even, closes).

Raw rows (JSON text; the `type` value as the server sends it) for the pins:
```
{"name":"c","type":"Tuple(\n    `a\\\\nb` Int32,\n    d String)", ... "is_subcolumn":0}
{"name":"c.a\\nb","type":"Int32", ... "is_subcolumn":1}
{"name":"c.d","type":"String", ... "is_subcolumn":1}
{"name":"c","type":"Tuple(\n    e Enum8('x\\\\ny' = 1, 'z' = 2),\n    d String)", ... "is_subcolumn":0}
```
As Java literals for the existing `row(name, type)` helper (which embeds the text with `%s`), and equal to what `fromJsonRow(...).getType()` returns because `fromJsonRow` keeps backslash text verbatim:
```java
"Tuple(\\n    `a\\\\\\\\nb` Int32,\\n    d String)"                 // literal backslash-n in a name
"Tuple(\\n    e Enum8('x\\\\\\\\ny' = 1, 'z' = 2),\\n    d String)"  // literal backslash-n in an enum label
"Nullable(Tuple(`a\\\\\\\\nb` Int32, d String))"                   // the D-12 live table, no line break
"c.a\\\\nb"                                                         // the sub-column row name
```
**D-13 verdict: the contingency does NOT trigger.** A quote-aware scan distinguishes the cases. A one-character-state scanner is sufficient; no parser of the whole type is needed. Counter-example search: an odd backslash run inside quotes occurs only for JSON-level escapes (`\"`, `\t`), which never precede a backtick or single quote, so the parity test is only evaluated on even runs. `ClickHouseColumn.of` itself rejects a backtick escaped inside a backtick name (`IllegalArgumentException: Missing quote`), which is outside this phase and must not be put in a pin.

Facts the plan must not trip over:
1. The enum-label case **already pairs with no WARN on current `main`**. The pin for it must assert the `normalise` output string (equal to the input apart from the line breaks), not only `apply`, or it can never be seen red.
2. A **real newline inside a name** is broken on `main` too (`apply` false, fallback WARN). Add it to the pins; note it in the ledger.
3. Column names that contain a backslash carry the JSON-doubled form (`c.a\\nb`, two backslashes), not the server name, so a Kafka record's field must be named with two backslashes for the writer to find it [VERIFIED: live, 23-EVIDENCE.md R-02]. This predates the phase and is not changed; the D-12 live premise must use that field name.

### Job 3, D-14: spotless baseline  [VERIFIED: gate script run this session]
`bash /tmp/gsd23/spotless-num.sh` (the Phase 22 `SPOTLESS_NUMSTAT` gate, `git archive HEAD` + `spotlessApply` in a throwaway copy) printed `SPOTLESS-BASELINE-EQUAL` at HEAD `16e0b2d6` (46 files, md5 `f2e32c7bc069ecd155830f82d26285d6`). **None of the files this phase touches is in the baseline-red set**: `DeclaredNullability.java`, `DeclaredNullabilityTest.java`, `NullableTupleMatrixLiveTest.java`, `SimpleAggregateTupleRoundTripLiveTest.java`, `NullableRoundTrip.java`, `ClickHouseHelperClientForkUnitTest.java`, `LogCapture.java` are absent from the numstat. So the "same +/- counts" clause applies to no file; every touched or new file needs an **empty** `spotlessApply` diff, which is the same as the whole-tree numstat staying at the 46-file md5. Baseline-red files near the phase that must stay untouched: `ClickHouseHelperClient.java` 3/4 (D-05), `Column.java` 13/14, `ColumnTest.java` 5/7, `TableTest.java` 4/5. The full numstat is saved at `/tmp/gsd23/spotless-numstat.txt` (Phase 22 evidence E-01 lists the same three baseline-red files it touched).

### Job 4, inventory  [VERIFIED: grep + reads this session]
**Backslash-n literal (every definition and use):**
| Where | Line | Text |
|-------|------|------|
| `DeclaredNullability.java` | 33 | `static final String ESCAPED_NEWLINE = "\\n";` (package-private today; make `public`); used in `normalise` line 52 |
| `NullableTupleMatrixLiveTest.java` | 85 | `private static final String ESCAPED_NEWLINE = "\\n";`; used line 745 (`rawTypes.get(multiLine).contains(ESCAPED_NEWLINE)`); the class does not import `DeclaredNullability` yet |
| `SimpleAggregateTupleRoundTripLiveTest.java` | 52 | `private static final String ESCAPED_NEWLINE = "\\n";`; used line 119; already imports `DeclaredNullability` (line 15) |
| `DeclaredNullabilityTest.java` | 33 | `private static final String SERVER_NEWLINE = "\\n";`; used lines 140, 142, 348 (same package, no import needed) |

**`NullableRoundTrip.normaliseDeclaredType`:** definition `NullableRoundTrip.java:195-197` (`static String normaliseDeclaredType(String rawType) { return DeclaredNullability.normalise(rawType); }`); only call site `NullableTupleMatrixLiveTest.java:730`; removing it leaves the import at `NullableRoundTrip.java:16` unused (remove it) and `NullableTupleMatrixLiveTest` needs the new import.

**`DISCOVERED_TABLE`:** `ClickHouseHelperClientForkUnitTest.java:403` `private static final String DISCOVERED_TABLE = "db.t";` sits mid-class. The class has **no constants block at all** (it opens directly with `@Test` methods), and `WARN_LEVEL` is also declared mid-class at ~line 255. D-15 therefore means creating a constants block at the top, with `DATABASE = "db"` and `TABLE = "t"` and `DISCOVERED_TABLE = DATABASE + "." + TABLE`, and passing them to `buildDescribedTable` in `discover(...)` (line 280-290, currently the literals `"db", "t"`).

**WARN-text assertion sites (the D-16 list)** [VERIFIED: file reads]:
| Test | Where | Weak assertion today |
|------|-------|----------------------|
| `unparsableDeclaredTypeFailsOpenWithOneMetadataWarning` | `DeclaredNullabilityTest.java:203`, assertions 216-219 | `message.contains(ROOT)` with `ROOT = "c"` (line 27) |
| `structureMismatchFailsOpenWithOneWarning` | `:227`, assertions 238-241 | same |
| `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning` | `:268`, assertions 278-282 | same, plus `NullPointerException.class.getSimpleName()` at 281 (the D-08 flip) |
| `fallbackWarningNamesDatabaseAndTable` | `ClickHouseHelperClientForkUnitTest.java:407`, assertion 418 | `message.contains("c")` ("Could" contains c) |
| `unmatchedElementAppliesNothing` | `DeclaredNullabilityTest.java:248` | asserts only the table; optional to strengthen |

Other `LogCapture.attach(DeclaredNullability.class)` sites assert **zero** WARN events (`multiLineTupleFormAppliesWithoutFallback`, `rootSimpleAggregateFunctionOverTuplePairsWithoutFlippingFlags`, `simpleAggregateFunctionOverArrayElementPairsWithoutFallback`, `SimpleAggregateTupleRoundTripLiveTest:125`, `ClickHouseHelperClientForkLiveTest:411`, and the `warnCount` cases in the ForkUnitTest). Those are exactly the ones D-01 can break (Open Question 1).

**Fork anchors** [VERIFIED: `.planning/scripts/fork-anchors.txt`, `cat -n`]: line 70 is the Phase 19 comment; rows 71 to 75 are
```
71  Column.java                      .isNullable(isNull)                                        3  COLS-10
72  Column.java                      @Setter(AccessLevel.PACKAGE) private boolean isNullable;   1  COLS-10
73  ClickHouseHelperClient.java      DeclaredNullability.apply(                                 1  COLS-10
74  ClickHouseHelperClient.java      buildDescribedTable(                                       3  COLS-10
75  DeclaredNullability.java         unwrapSimpleAggregate(                                     2  COLS-10
```
The file has 82 lines and **65 data rows** (the Phase 22 mirror also said 65). `unwrapSimpleAggregate(` matches two lines (definition at 109, call at 119); any new Javadoc or comment containing that string would change the count. New rows for this phase are measured with `grep -cF -e <pattern> <file>` at the commit that adds them and the file is rewritten in that same commit (header rule of `fork-anchors.txt`).

**FORK-DELTA COLS-10 row:** `docs/FORK-DELTA.md:51`, one very long table row with four cells: what the fork does | files | why | `COLS-10; <pinned-by test classes>`. Today it states the normalisation as "after turning the backslash-n that `ClickHouseFieldDescriptor.fromJsonRow` leaves in multi-line types into a space", the fallback cases ("a declared type that does not parse, a tree that does not pair, or any unexpected exception in the pass"), and ends "`Table.java` is unchanged, and the discovery WARN this row replaces is gone". Its pinned-by cell lists `NullableTupleMatrixLiveTest`, `DeclaredNullabilityTest`, `NestedNullRowBinaryLiveTest`, `NestedNullableCompositeWriteTest`, `ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`, `SimpleAggregateTupleRoundTripLiveTest`. Add the D-01, D-07 and D-10 clauses there and any new pinning class.

**Archived disposition files (current state):**
- `17-REVIEW-DISPOSITION.md`: frontmatter `open: 4`, `total: 4`, `recorded: 2026-10-12T08:28:47.317Z`; findings WR-01 (warning), IN-01, IN-02, IN-03 (info), all `disposition: open`, table rows `| WR-01 | warning | open | - |` etc. with Source `-`.
- `19-REVIEW-DISPOSITION.md`: frontmatter `open: 0`, `total: 8`, `recorded: 2026-10-14T13:31:33.000Z`; WR-01, WR-02, IN-01, IN-02, IN-03, IN-04 are `deferred` (Source cells say "carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md) ..."), WR-03 and CR-01 are `fixed` with SHAs `3e39867d` and `4e133d4e`.
- Both files carry the rule text "Set `deferred` by hand and put the reason in the Source cell; both are preserved." Each finding exists twice (frontmatter `disposition:` and table row); update **both** copies and `open:`. After DEBT-12 the 17 file is `open: 0` (IN-03 is `skipped`, which is not open); the 19 file stays `open: 0`.
- 19 IN-04's own Source cell names its re-open trigger: "an observed DESCRIBE shape that drops a declared Tuple element". R-01 **is** that observation (`size`/`size0`), so the closure text should say the trigger fired and point to 999.4.

**Phase 21 SHAs for DEBT-12** [VERIFIED: `git rev-parse`, `git log`, `21-04-SUMMARY.md`, `21-UAT.md`]:
| Finding | Closure | SHA (full) | Evidence |
|---------|---------|-----------|----------|
| 17 IN-01 (CI-18), 17 IN-02 (CI-19), 17 WR-01 guard half (D-09) | `test(21-04): pin reportTestForks and dependabot exclude-paths, fix the runs-on and write-scope rules, with fixtures (CI-18, CI-19, D-05..D-10)` | `fee898c635fe7a9fd33e5e00a0485743e14848d2` | `TestsWorkflowPinGuardUnitTest` 26 tests; `21-04-SUMMARY.md`; `21-EVIDENCE.md` E-10, E-11 |
| (preparatory) | `refactor(21-04): extract the pin-guard rules into static line-list methods (D-06)` | `37ef6d40f97bfb8f55361d711efb8fe3daa90f20` | same |
| 17 WR-01 live half (OPS-02) | OPS-02 confirmation recorded in E-02 | `5d616806d47b0b0a1c2e4d3c86b0ffb9232ca5be` | `21-EVIDENCE.md` E-02 ("OPS-02: pass"); `21-UAT.md` test 1 `result: pass`, UAT commit `5010093fa06f39081348782b5ff309ae12246525` (2026-10-19) |
The 17 file's IN-03 `skipped` reason is the roadmap sentence quoted in D-18; `TestRuntimeClasspathLz4UnitTest` is the cross-reference.

**Behaviour ledger numbering:** Phase 22's last ID is `L-48` [VERIFIED: `22-BEHAVIOUR-LEDGER.md`], so Phase 23 rows start at **L-49**. Columns: `ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 24 destination`, destination key CHANGELOG / FORK-DELTA / ROLL-03.

### Job 5, D-08 / D-09  [VERIFIED: run live, 23-EVIDENCE.md R-03]
- Tests touching the null-type Column or NPE path: `DeclaredNullabilityTest.unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning` (line 268, `Column.builder().name(ROOT).build()` with `Tuple(a Int32)`); `containsTupleIsFalseWithoutATupleAnywhere` (line 289) already passes `containsTuple(null)`. No other test builds a type-less Column.
- Shapes that throw an NPE **today**: null root (`apply` throws, because the handler runs `root.getName()`), null type at root, in an Array element or a Map value (`containsTuple` throws too), a null `tupleFields` entry, a field with null name, a field with null type. **Not an NPE today:** a null *root name* (`ClickHouseColumn.of(null, ...)` throws `IllegalArgumentException`, WARN reason `IllegalArgumentException`), a null `tupleFields` list (treated as empty, `apply` true), a null Map value Column (pairs, `apply` true), a null Array element Column (`structure mismatch`).
- A nested tuple whose own name is null does not NPE either: the prefix becomes the text `"null."`, which no field matches, so it ends as a mismatch by accident, and would pair a field literally named `null.x`. Add the explicit null-name check (Pitfall 4).
- **A type string `ClickHouseColumn.of` rejects**, with the thrown class observed (client 0.9.6): `Nullable(Tuple(SENTINEL_ELEMENT_NAME NoSuchType))`, `Tuple(`, `` (empty), `Foo(`, `Array(`, `Map(String)`, `Tuple()` all throw `java.lang.IllegalArgumentException`; `Enum8('a' = x)` throws `java.lang.NumberFormatException`. Current test input `"Nullable(Tuple(" + sentinel + " NoSuchType))"` (DeclaredNullabilityTest.java:204-205) is therefore a valid D-09 input; the new test should still call `ClickHouseColumn.of(...)` first (`assertThrows(RuntimeException.class, ...)`, read `getClass().getSimpleName()`) and assert `"(" + simpleName + ")"` as the message tail. Some strings are accepted by the parser and must not be used: `Tuple(a Int32,, b Int32)`, `Decimal(` and an unbalanced `Nullable(Tuple(a Int32)`.

### Job 6, verify commands  [VERIFIED: run this session on the current tree]
| Command | Result today |
|---------|--------------|
| `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > <log> 2>&1` | `Test result: SUCCESS` / `Tests summary: 795 tests, 793 succeeded, 0 failed, 2 skipped` (HEAD `16e0b2d6`, same as Phase 22's close) |
| anchors mirror (loop over `fork-anchors.txt`, `grep -cF -e "$pat" "$path"`, Phase 22 plan 22-01 line 162) | `rows=65 bad=0` |
| `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | `invariants: provenance_paths=21 anchor_commits=444 anchor_rows=65 marker_commits=491 anchor_exempt=1 result=PASS`, exit 0, **about 6 minutes**: run the mirror per commit and the full script at the phase gate only (and once mid-phase if wanted) |
| SPOTLESS_NUMSTAT gate | `SPOTLESS-BASELINE-EQUAL` (6 s with a warm daemon) |
| Full suite: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` | **not re-run this session**. Phase 22 close (E-05): `1231 tests, 1213 succeeded, 0 failed, 18 skipped`, `latest` 26.9.19.9 |
Docker: colima profile `vz` is `Running` (`colima list`), `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` is exported, image `clickhouse/clickhouse-server:latest` is present locally (id `bdb5c911a23d`, created 2026-10-14), server version `26.9.19.9`. `timeout(1)` does not exist on this macOS; use `until` loops or background jobs. No OpenJ9 crash occurred in the one Gradle `testUnit` run; no core dumps in the repo root.

## Common Pitfalls

### Pitfall 1: D-01 turns three existing tests red
**What goes wrong:** the new unpaired WARN fires for a Tuple root row that has no element rows. `ClickHouseHelperClientForkUnitTest.otherTypesDiscoverWithoutWarningAndUnchangedRootNullability` (cases `Tuple(a Nullable(Int32))` and `Tuple(a Nullable(Enum8('q' = 1)))`, ValueSource at lines ~355-366) and `repeatedDiscoveryNeverWarns` (line 428, three roots) use `discover("c", rawType)` with a root row only and assert zero WARNs. [VERIFIED: prototype run, Evidence R-04: `expected: <0> but was: <1>` and `<3>`].
**How to avoid:** see Open Question 1. Do not "fix" it by making the WARN conditional on shape without a decision.
**Warning signs:** `testUnit` red in the COLS-14 commit only for those names.

### Pitfall 2: the D-08 flip is red the moment COLS-12 lands
**What goes wrong:** the test asserts `NullPointerException` in the WARN. With the null check it is `(structure mismatch)`. This is the intended flip and must be in the same commit (roadmap note 2). [VERIFIED: prototype run].

### Pitfall 3: the sentinel is introduced after it is first needed
**What goes wrong:** order step 2 (COLS-12 + flip) adds WARN-asserting tests and the flipped test must adopt the sentinel (D-08), but the roadmap order puts DEBT-10 (sentinel) at step 3.
**How to avoid:** add the sentinel constant and a `root(table, name)` helper in step 2 (first use), and let step 3 convert the remaining pre-existing assertions (`unparsable...`, `structureMismatch...`, `fallbackWarningNamesDatabaseAndTable`) and record the two red mutations. Do not use `ROOT = "c"` in any new WARN test.

### Pitfall 4: null names are not all NPEs
A null root name gives `IllegalArgumentException`, not an NPE (R-03). D-07 says null names are a structure mismatch, so add explicit checks before `ClickHouseColumn.of` (root name) and in `collectTuple` (tuple name, field name), then pin each. Otherwise the root-name case stays on the catch path with a different reason, and the nested-tuple case relies on the accidental `"null."` prefix.

### Pitfall 5: the sentinel must not be a substring of the fixed text, and must not collide with other tokens
`nullability_root` is not a substring of `FALLBACK_WARNING` (which contains the word "nullability" but not the token) [VERIFIED by reading lines 35-36]. For the new WARN, do not reuse the words "root" or "nullability_root" in its fixed text. Keep the sentinel out of the declared type used in the same assertion unless the assertion is meant to find it there.

### Pitfall 6: where the shared captured strings live
`DeclaredNullabilityTest` is package-private in `sink.db.mapping`; the live classes are in `sink`; `NullableRoundTrip` is package-private in `sink`. Neither can see the other's constants. Put the captured type strings used by both the Docker-free pin and the D-12 live premise in one small **public** test-source class (for example beside `ColumnNullabilityPaths`, which is already used across both packages), and have both compare against it. Because `fromJsonRow` keeps backslash text verbatim, the same Java string is both the input of the `row(name, type)` helper and the expected `getType()` of the live premise, so the two cannot drift.

### Pitfall 7: anchors count lines, not occurrences
`grep -cF` counts matching lines. A Javadoc line that quotes a token such as `unwrapSimpleAggregate(` or `buildDescribedTable(` changes the count. Measure each new row after the edit and rewrite `fork-anchors.txt` in the same commit; the full invariant script re-checks the file at every commit.

### Pitfall 8: SLF4J argument order for the new WARN
The new format has more placeholders than `FALLBACK_WARNING`. A mutation that drops one placeholder shifts every later value by one. Give the new format a distinct delimited segment for the column (as `"column {} of table"`) so D-17 mutation 2 is catchable, and assert the whole delimited segment.

### Pitfall 9: do not paste a `'`-containing or backtick-escaped name into a pin
`ClickHouseColumn.of` rejects some of them (`Missing quote`). Also `Column.extractEnumValues` throws `NumberFormatException` for an enum label with an escaped quote (`Enum8('it\'s' = 1, 'q\\' = 2)`), which is upstream behaviour and out of scope [VERIFIED: live, `Cap2` on `bs_quote`].

### Pitfall 10: `ROADMAP.md` edits by script
Roadmap rule: slice to the target `### Phase N:` section before any find-and-replace (an unanchored replace truncated it once, `c1480f8f`) [CITED: .planning/ROADMAP.md line ~283]. The 999.4 entry is appended after 999.3 under `## Backlog`.

## Code Examples

### Prototype of the whole production change (outside the repo, compiled and run)
All of the following passed `DeclaredNullabilityTest` apart from the flip target, and left the five live classes green (Evidence R-04). It is a starting point, not final code: apply Google Java Format, extract constants per project style, use `LinkedHashMap`, and write the final WARN wording.

```java
// Source: research prototype /tmp/gsd23/proto/src/.../DeclaredNullability.java
public static final String ESCAPED_NEWLINE = "\\n";   // [VERIFIED: current line 33 is package-private]

static final String UNPAIRED_ELEMENT_WARNING = "Declared Tuple element(s) {} of column {} of table {}.{} "
        + "(declared type {}) have no matching column; applying the declared nullability to the elements that paired";

public static String normalise(String declaredType) {
    StringBuilder out = new StringBuilder(declaredType.length());
    int length = declaredType.length();
    char quote = 0;                                   // 0, '`' or '\''
    int i = 0;
    while (i < length) {
        char c = declaredType.charAt(i);
        if (c == '\\') {
            int end = i;
            while (end < length && declaredType.charAt(end) == '\\') end++;
            int run = end - i;
            if (quote == 0 && run == 1 && end < length && declaredType.charAt(end) == 'n') {
                out.append(' ');                      // the server's line break
                i = end + 1;
                continue;
            }
            out.append(declaredType, i, end);
            i = end;
            if (quote != 0 && end < length) {
                char escaped = declaredType.charAt(end);
                out.append(escaped);
                i = end + 1;
                if (escaped == quote && (run / 2) % 2 == 0) quote = 0;   // even half-run: not escaped
            }
            continue;
        }
        if (quote == 0 && (c == '`' || c == '\'')) quote = c;
        else if (quote != 0 && c == quote) quote = 0;
        out.append(c);
        i++;
    }
    return out.toString();
}

public static boolean containsTuple(Column column) {
    if (column == null || column.getType() == null) return false;     // COLS-12
    ...
}

public static boolean apply(Column root, String declaredType, String database, String table) {
    if (root == null || declaredType == null) return false;           // D-06: before the try, no log
    String columnName = root.getName();                               // computed before the try
    try {
        ClickHouseColumn parsed = ClickHouseColumn.of(columnName, normalise(declaredType));
        List<Assignment> assignments = new ArrayList<>();
        List<String> unpaired = new ArrayList<>();
        if (!collect(root, parsed, assignments, unpaired)) {
            LOGGER.warn(FALLBACK_WARNING, columnName, database, table, declaredType, STRUCTURE_MISMATCH);
            return false;
        }
        for (Assignment a : assignments) a.column().setNullable(a.nullable());
        if (!unpaired.isEmpty()) {
            LOGGER.warn(UNPAIRED_ELEMENT_WARNING, unpaired, columnName, database, table, declaredType);
        }
        return true;                                                  // D-01: still applied
    } catch (RuntimeException e) {
        LOGGER.warn(FALLBACK_WARNING, columnName, database, table, declaredType, e.getClass().getSimpleName());
        return false;
    }
}
// collect(): first line  if (column == null || column.getType() == null) return false;   (D-07)
// collectTuple(): tuple.getName()==null -> false; per field: field==null || field.getName()==null -> false;
//   remember the paired keys, then every declared key not paired is added to `unpaired` as prefix + key.
```
Measured output of the prototype on the R-01 rows (one event per affected root, wording not binding): `Declared Tuple element(s) [c.size] of column c of table db.t (declared type Tuple(\n    size UInt32,\n    name String)) have no matching column; applying the declared nullability to the elements that paired`.

### D-04 skip-aware discovery (the loop `describeTableV2` runs)
```java
// Source: ClickHouseHelperClient.java describeTableV2 loop (read this session); skip-aware variant for the unit pin
List<Column> columns = new ArrayList<>();
Map<String, Column> columnsByName = new HashMap<>();
Map<String, String> declaredTypes = new HashMap<>();
int numColumns = 0;
for (String line : CAPTURED_ROWS) {                               // the R-01 rows, verbatim
    ClickHouseFieldDescriptor d = ClickHouseFieldDescriptor.fromJsonRow(line);
    if (!d.isSubcolumn()) numColumns++;
    if (ClickHouseHelperClient.shouldSkipDescribeRow(d, columnsByName)) continue;
    if (!d.isSubcolumn()) declaredTypes.put(d.getName(), d.getType());
    Column c = Column.extractColumn(d);
    columns.add(c);
    columnsByName.put(c.getName(), c);
}
Table t = ClickHouseHelperClient.buildDescribedTable("db", "t", false, columns, numColumns, declaredTypes);
```
Use the sentinel root for the root column of this pin (D-16): rename `c` to the sentinel in the captured rows (`c.size` becomes `<sentinel>.size`, etc.); the pin documents the shape, not the server's column name.

### D-12 live premise sketch (beside `omittedNullableTupleFieldStoresNull`)
```java
@ParameterizedTest(name = "COLS-13 backslash-n in a Tuple element name round-trips {0}")
@EnumSource(Twin.class)
void backslashNInTupleElementNameRoundTripsWithoutFallback(Twin twin, TestInfo testInfo) throws Exception {
    // DDL (Java literal for the SQL text `a\\nb`): "c Nullable(Tuple(`a\\\\nb` Int32, d String))"
    // 1. createTable via twin.ddl(...); 2. NullableRoundTrip.describeRows -> row "c".getType() equals the shared constant
    // 3. LogCapture on DeclaredNullability around chc.describeTable(...) -> events("WARN") is empty
    // 4. put one record whose Struct field for the first element is named with TWO backslashes (the Column name)
    // 5. selectAndAssert with positional tupleElement(c, 1) / tupleElement(c, 2)
}
```
Red/green already measured: on unchanged `main` this table gives one fallback WARN `(structure mismatch)` and `put` fails with `CANNOT_READ_ALL_DATA`; with the fix, no WARN and the row reads back `(7,'nm')` [VERIFIED: live, R-02]. Both twins matter: PLAIN uses RowBinary, DEFAULT uses RowBinaryWithDefaults.

### WARN mutation mechanics (D-17)
With `FALLBACK_WARNING` five placeholders and five arguments `(column, database, table, declaredType, reason)`:
1. column argument dropped, placeholder kept: SLF4J leaves a literal `{}` at the end and every value shifts one slot (`column db of table t.<type> from its declared type <reason> ({})`), so the sentinel is absent and both `contains(ROOT)` (when ROOT is the sentinel) and the delimited form fail.
2. column placeholder dropped, argument kept: the text becomes `... for column  of table {}.{} ...` with the sentinel landing in the next slot (`of table nullability_root.db from its declared type t`). A bare `contains(sentinel)` still passes; only `"column " + sentinel + " of table"` fails. That is why the delimited form is mandatory (CONTEXT "Specifics").
Procedure per mutation: edit the `LOGGER.warn` line or the constant, run the affected classes with `--rerun`, record the failing test names in `23-EVIDENCE.md`, `git checkout -- <file>`, re-run green. Do the same for the new unpaired WARN.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `normalise` = `replace("\\n", " ")` | quote-aware single pass | this phase | names and labels with backslash-n pair |
| Unpaired declared element silently ignored (`missingDeclaredElementDoesNotShiftPairing`) | one metadata-only WARN, nullability still applied | this phase | discovery loss becomes visible in logs |

**Deprecated/outdated:** `.claude/CLAUDE.md` statements about Gradle 7.4.2 and JUnit 5.11.3 (see Project Constraints).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Server behaviour captured on `latest` 26.9.19.9 stays the same until the phase executes (the `latest` tag drifts) | Jobs 1, 2 | The D-12 live premise compares the raw type with the shared literal and would go red, which is its purpose; re-run R-02 only if it does |
| A2 | An older ClickHouse server may allow a Tuple element named `null` | Job 1 | None for this project (target is `latest` only); 999.4 text should still say "on `latest` the server rejects `null`" |
| A3 | The three collided tests may be flipped by extending their fixtures (Open Question 1, option B) without it counting as "weakening" under AGENTS.md | Pitfall 1 | The user decides; the phase's own rule allows flips in the Phase 19 D-11 format |

All other claims in this document were verified in this session (live run, file read with line citation, or command output pasted in `23-EVIDENCE.md`).

## Open Questions

1. **D-01 collides with three existing tests (needs the user's explicit answer before the COLS-14 commit)**
   - What we know: `ClickHouseHelperClientForkUnitTest.otherTypesDiscoverWithoutWarningAndUnchangedRootNullability` (2 of its 10 values: `Tuple(a Nullable(Int32))`, `Tuple(a Nullable(Enum8('q' = 1)))`) and `repeatedDiscoveryNeverWarns` feed a Tuple **root row only** and assert zero WARN events; D-01's WARN fires once per such root. Prototype: 4 reds in the two Docker-free classes, 3 of them these. Live classes: all green.
   - What's unclear: whether to (B, recommended) flip the three in the Phase 19 D-11 format by adding the element rows so the Tuple pairs fully (their no-WARN/root-nullability assertions stay identical; the root-only input moves to a new sibling that asserts exactly one unpaired WARN), or (A) exempt "no element rows at all" from the WARN, which would also silence the real `Tuple(size UInt32)` single-element shape that 999.4 describes.
   - Recommendation: B, surfaced as an orchestrator-inline decision (these are edits to existing tests, which the "human gates" rule treats as needing the user's own answer). Do not pick A silently.
2. **Null root name (D-07 wording)**
   - What we know: a null root name throws `IllegalArgumentException` from `ClickHouseColumn.of`, not an NPE; a null nested tuple name currently mismatches by accident.
   - Recommendation: add explicit null-name checks (root name before the parse, tuple name and field name in `collectTuple`) so all of D-07 yields `structure mismatch`; unreachable in production, so it is a hardening-only change; pin each shape.
3. **SC3 wording vs D-01 (a reading to record, not a question)**
   - ROADMAP SC3 says the pass "either logs and falls back on an unpaired declared element, or keeps today's behaviour with a written reason". D-01 chose a third form: log and still apply. Record the reading in `23-EVIDENCE.md` (as Phase 22 did for F7) so the verifier does not judge SC3 on the literal "falls back".
4. **Observation outside scope: element and column names with backslashes carry the JSON-doubled form**
   - `fromJsonRow` makes `Column.getName()` hold two backslashes for a server name with one, so a Kafka field must be named with two. Pre-existing, not in any requirement. The user may want a backlog entry (not created here).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | live premise, full suite, any live capture | yes | `colima list`: `vz Running aarch64 8 CPUs 16GiB` | none needed |
| ClickHouse image `clickhouse/clickhouse-server:latest` | live tests | yes, local | server 26.9.19.9, image created 2026-10-14 | `docker pull` if absent |
| Gradle wrapper | all gates | yes | 9.6.1 | none |
| JDK | build | toolchain 17 (OpenJ9 per user notes); shell `java` is Homebrew 21 | — | use `./gradlew`, not bare `java`, for repo builds |
| `colima`/`docker` env | — | `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` exported | — | never run a bare `colima start` |
| `python3 -I` | evidence checks | yes | — | — |
| `timeout` | — | **no** (macOS) | — | background job plus an `until` loop |

**Missing dependencies with no fallback:** none. The research container `gsd23-ch` was removed; `git status` is clean apart from the two planning files written in the phase directory.

## Validation Architecture

Skipped: `.planning/config.json` has `workflow.nyquist_validation: false` [VERIFIED: config read this session]. The verification commands the planner should reuse are listed in "Job 6" and the test-to-requirement mapping is in `<phase_requirements>`.

Wave 0 gaps for the plan (not a Nyquist section, planning facts): no new framework is needed. New Docker-free pins go into `DeclaredNullabilityTest` (extends nothing, stays in `testUnit`) and `ClickHouseHelperClientForkUnitTest`; one new public fixture class for the shared captured strings; the live premise goes into `NullableTupleMatrixLiveTest` (tagged `clickhouse`, so it runs only in the full suite).

## Security Domain

`security_enforcement` is true (ASVS level 1, block on high) [VERIFIED: config].

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | The declared type text is server-supplied but treated as untrusted: the scanner is a bounded linear pass with no regex backtracking, total for any string including an unterminated quote; `ClickHouseColumn.of` failures are caught |
| V6 Cryptography | no | — |
| V7 Error Handling and Logging | yes | Metadata-only WARN: column, database, table, raw declared type, DDL element names, exception class names; no exception message, no throwable, no record value (Phase 18 content rule); pinned by `LogCapture` assertions |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Record data or exception text leaking through a WARN | Information disclosure | Log the exception class name only; the unpaired WARN prints names from the DDL, never values; assert `hasThrowable()` is false and one event |
| Log forging via an element name with control characters in the unpaired list | Tampering | Names come from the table owner's DDL and from a client parse that keeps escapes as text (a real newline in a name appears as backslash-n text in the log, observed in R-02); the existing `FALLBACK_WARNING` already prints the raw declared type, so this adds no new class of exposure. Print the list as a bracketed list, not concatenated |
| Pathological type string causing CPU use | Denial of service | Single linear pass, no nested quantifiers; runs once per root per DESCRIBE |
| Silent data corruption from a short Tuple write (the 999.4 defect) | Tampering / integrity | Out of scope to fix; made visible by the new WARN; evidence quotes the outcome |

## Sources

### Primary (HIGH confidence)
- Live ClickHouse `latest` 26.9.19.9 x client V2 0.9.6: DESCRIBE rows, insert outcomes and WARN behaviour captured this session (`23-EVIDENCE.md` R-01..R-04, raw files in `/tmp/gsd23/desc/*.jsonl`).
- Repo files read this session with line citations: `DeclaredNullability.java`, `ClickHouseHelperClient.java` (473-513, describe loops), `ClickHouseFieldDescriptor.java:47-48`, `DeclaredNullabilityTest.java`, `ClickHouseHelperClientForkUnitTest.java`, `NullableRoundTrip.java`, `NullableTupleMatrixLiveTest.java`, `SimpleAggregateTupleRoundTripLiveTest.java`, `LogCapture.java`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `verify-merge-invariants.sh`, `docs/FORK-DELTA.md:51`, `.planning/ROADMAP.md`, both disposition files, `17-REVIEW.md`, `19-REVIEW.md`, `20-MILESTONE-CLOSE-RECORD.md`, `21-04-SUMMARY.md`, `21-UAT.md`, `22-EVIDENCE.md`, `22-BEHAVIOUR-LEDGER.md`.
- Commands run: `testUnit --rerun`, the anchors mirror and the full invariant script, the SPOTLESS_NUMSTAT gate, a JUnit Platform launcher run of the prototype against 70 Docker-free and 68 live tests.

### Secondary (MEDIUM confidence)
- none

### Tertiary (LOW confidence)
- none

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependency, versions read from the resolved classpath.
- Architecture: HIGH, prototype built and run against the real tests and a live server.
- Pitfalls: HIGH, each one reproduced (Pitfall 1 and 4 by run, 9 by live capture); Pitfall 3 and 7 follow from the stated order and the script's `grep -cF` semantics.

**Research date:** 2026-10-20
**Valid until:** 2026-10-27 for the server captures (the `latest` tag moves; D-12 is the standing check), 30 days for the rest.
