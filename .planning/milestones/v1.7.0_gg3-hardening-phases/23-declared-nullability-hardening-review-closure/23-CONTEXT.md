# Phase 23: Declared-Nullability Hardening & Review Closure - Context

**Gathered:** 2026-10-20
**Status:** Ready for planning

<domain>
## Phase Boundary

`DeclaredNullability` becomes total for any input. Its `normalise` keeps a literal backslash-n inside backtick-quoted Tuple element names and enum labels. It gets a recorded rule for a declared Tuple element that has no matching Column. Its tests can actually fail. Every 17-REVIEW and 19-REVIEW finding ends as fixed or closed with a written reason, and each archived disposition row points to where it was closed.

Requirements: COLS-12, COLS-13, COLS-14, DEBT-10, DEBT-11, DEBT-12.

The only `src/main` file this phase changes is `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java`, which is fork-only. `ClickHouseHelperClient` is not touched (D-03, D-05).

</domain>

<decisions>
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

### Decided at plan-phase (2026-10-20, the user's own answers to 23-RESEARCH.md Open Questions)
- **D-19:** **The three tests D-01 turns red are flipped (Option B).** They are `ClickHouseHelperClientForkUnitTest.otherTypesDiscoverWithoutWarningAndUnchangedRootNullability`, cases `Tuple(a Nullable(Int32))` and `Tuple(a Nullable(Enum8('q' = 1)))`, and `repeatedDiscoveryNeverWarns`. Each feeds a Tuple root row with no element rows and asserts zero WARNs.
  - They are flipped in the Phase 19 D-11 format: the element rows are added so each Tuple pairs fully, and their no-WARN and root-nullability assertions stay identical.
  - The root-only input moves to a new sibling test that asserts exactly one unpaired-element WARN (D-01/D-02, with D-16 and D-17 applying).
  - These land in the COLS-14 commit. Option A, exempting "no element rows at all" from the WARN, was rejected because it would silence the single-element `Tuple(size UInt32)` shape behind 999.4.
- **D-20:** **New backlog entry 999.5.** It records that a server column or element name containing a backslash comes out of `fromJsonRow` with the backslash doubled (`c.a\\nb`), so a Kafka field must use the doubled form for the writer to match it.
  - The evidence is 23-EVIDENCE.md R-02. The behaviour is pre-existing, and Phase 23 changes no code or test for it.
  - It is appended to `.planning/ROADMAP.md` `## Backlog` after 999.4 (or after 999.3 if D-03's 999.4 is not created), sliced to the backlog section before any edit (23-RESEARCH.md Pitfall 10).

### Claude's Discretion
- The new WARN constant's name and exact wording, within D-01's fields. It must not contain the sentinel text, and must not say "keeping the nullability DESCRIBE reported".
- How the quote-aware scanner is structured (a char loop or a small state machine). It runs once per DESCRIBE per root, not on the hot path.
- The sentinel root's exact name.
- Whether the unpaired-element WARN gets its own sibling test or `LogCapture` assertions added beside `missingDeclaredElementDoesNotShiftPairing`. The preference is a sibling, so the pinned test's body stays byte-identical.
- Where the live premise lands (D-12), and whether the D-04 skip-aware discovery helper is a new helper or a parameter on `discover(...)`.
- The plan split, within the fixed order.
- The layout of `23-EVIDENCE.md`. Precedent: the Phase 21/22 evidence files with red-before/green-after runs and gate output.
- How each disposition's Source cell words the pointer, and whether it keeps a "previously deferred (20-MILESTONE-CLOSE-RECORD.md)" note.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` §"Phase 23: Declared-Nullability Hardening & Review Closure": goal, SC1–SC5, the fixed order, and the fork/anchors/gate notes
- `.planning/REQUIREMENTS.md`: COLS-12, COLS-13, COLS-14, DEBT-10, DEBT-11, DEBT-12
- `AGENTS.md`: high-risk files, no deleting or weakening of tests, coding conventions

### The findings being closed
- `.planning/milestones/v1.7.0_gg2-hardening-phases/19-nullable-enum-tuple-mapping/19-REVIEW.md`: WR-01, WR-02, WR-03, IN-01, IN-02, IN-03 with their suggested fixes
- `.planning/milestones/v1.7.0_gg2-hardening-phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md`: the rows DEBT-12 updates, including CR-01 and IN-04 (IN-04 is recorded only here and in the close record), and the file's preservation rules
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW.md`: WR-01, IN-01, IN-02, IN-03
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW-DISPOSITION.md`: all four rows still `open`
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md` §"Carried review advisories" and §"19-REVIEW IN-04": reasons and re-open triggers (read-only; not edited)

### Prior decisions this phase builds on
- `.planning/milestones/v1.7.0_gg2-hardening-phases/19-nullable-enum-tuple-mapping/19-CONTEXT.md`: D-10 (no dedup state), the D-11 flip format, the twin-table rule
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-CONTEXT.md`: D-01 (anchors exemption), D-07 (red in evidence), D-08..D-10 (the 17-REVIEW closures), D-14 (spotless baseline)
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-04-SUMMARY.md`, `21-VERIFICATION.md`, `21-UAT.md`: SHAs and the OPS-02 pass for the DEBT-12 pointers
- `.planning/phases/22-writer-record-data-exposure/22-CONTEXT.md`: process rules, the D-16 spotless reading, the D-03/D-13 stop-and-ask pattern
- `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md`: ledger format for `23-BEHAVIOUR-LEDGER.md`

### Fork bookkeeping
- `docs/FORK-DELTA.md:51`: the COLS-10 row, which gains the D-01/D-07/D-10 clauses
- `.planning/scripts/fork-anchors.txt` rows 71–75: the COLS-10 anchors (`unwrapSimpleAggregate( 2` for `DeclaredNullability.java`)
- `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/fork-anchor-exemptions.txt`: the anchors gate

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java`: `LogCapture.attach(DeclaredNullability.class)`, `events("WARN")`, `hasThrowable()`. Every WARN assertion uses it.
- `DeclaredNullabilityTest`'s `row(name, type)` / `describe(...)` / `root(table)` helpers build Columns from DESCRIBE JSON lines through `fromJsonRow` → `extractColumn` → `Table`, exactly as discovery does. D-16 needs a `root(table, name)` variant.
- `ClickHouseHelperClientForkUnitTest.discover(...)` runs `buildDescribedTable` without `shouldSkipDescribeRow`. Its existing `shouldSkipDescribeRow` cases (~L113–153) show how to call the skip directly for D-04.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnNullabilityPaths.java`: `byPath(...)` flattens nullability by dotted path for the assertions.
- `src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java`: `createTable`, `describeRows`, and the DDL twins for the live premise (D-12).
- `NullableTupleMatrixLiveTest` ~L720–750: the existing multi-line premise case, the natural neighbour for D-12.

### Established Patterns
- `DeclaredNullability.apply` is all-or-none: assignments are collected first and applied only after `collect` succeeds. D-01's "still apply" keeps that shape; the WARN is logged on top of a successful collect.
- `FALLBACK_WARNING` is one SLF4J format with five arguments `(column, database, table, declaredType, reason)`. The reason is `structure mismatch` or the exception's simple class name.
- `collectTuple` pairs by name, with a positional key `i+1` for unnamed elements. It iterates the Column's fields, so a missing Column field is never noticed today. D-01 changes that by comparing against the declared elements.

### Integration Points
- `ClickHouseHelperClient.buildDescribedTable` (~L499–513) is the only caller: `containsTuple(root)` guards `apply(...)`. Both `describeTableV1` (catches only `ClickHouseException | JsonProcessingException`) and `describeTableV2` reach it. It is not edited (D-05).
- `ClickHouseHelperClient.shouldSkipDescribeRow` (~L473–488): the `.null`/`.size0`/`.size` suffix skip behind D-03. Read only.
- `ClickHouseWriter` ~L803: the Tuple write iterates `col.getTupleFields()`, which is why a missing element shortens the bytes.

</code_context>

<specifics>
## Specific Ideas

- The new WARN must make sense to an operator when it fires for the `.size` shape. It names the unpaired declared element(s), so the 999.4 defect is visible in logs before it is fixed.
- The mutation in D-17 (2), dropping the placeholder but keeping the argument, is the case that shows *why* the delimited form matters. Record that reasoning in the evidence next to the red run.

</specifics>

<deferred>
## Deferred Ideas

- **Backlog 999.4 (created in this phase if D-03 confirms the shape):** `shouldSkipDescribeRow`'s `.size`/`.size0`/`.null` suffix skip drops a Tuple element with that name, so the writer writes a short tuple.
  - Fix sketch: narrow the skip so it keeps a row whose parent is a Tuple, while still skipping the structural `.null`/`.size0` rows of `Nullable`/`Array` columns (DESC-05 regression check).
  - The fix carries a FORK-DELTA row, an anchors row and a live round-trip test, and flips the D-04 pin.
  - It touches `ClickHouseHelperClient`, an upstream-region file, so it gets its own phase.
- **21-REVIEW (7 findings, all `open`) and 22-REVIEW (WR-01 and IN-01..IN-05, `open`):** these fall outside DEBT-12's 17/19-REVIEW scope. They go to Phase 24 or the milestone close. PROJECT.md → Active already tracks the Phase 21 and Phase 22 follow-ups.

</deferred>

---

*Phase: 23-declared-nullability-hardening-review-closure*
*Context gathered: 2026-10-20*
