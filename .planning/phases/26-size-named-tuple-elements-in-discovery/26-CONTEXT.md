# Phase 26: Size-Named Tuple Elements in Discovery - Context

**Gathered:** 2026-10-21
**Status:** Ready for planning

<domain>
## Phase Boundary

A table whose Tuple declares an element named `size`, `size0` or `size<digits>` accepts inserts, and the typed read-back equals the records. Discovery keeps the declared element at both drop sites (`ClickHouseHelperClient.shouldSkipDescribeRow` and `Table.updateParent`'s `SIZE_FIELD_MATCHER`) and still drops every structural DESCRIBE row (Array offsets, the Nullable null map, Map `keys`/`values`, nested `.size1`, the DESC-05 ancestor walk under JSON/Dynamic). The V1 and V2 describe loops share one fork-owned builder, and the 999.4 pin drives that builder instead of a copy of the loop. This closes backlog 999.4, FORK-DELTA Known costs item 10 and 23-REVIEW WR-02.

Requirements: DESC-06, DESC-07, DESC-08, DESC-09, DESC-10.

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
- Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant. Standing merge gate: `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD`, with the self-test. Baseline tag `baseline-v1.7.0_gg4` = `aa450709`.
- Every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`; Docker is colima profile `vz`; a two-run gate uses `--rerun`. Type coverage asserts typed SQL read-back, never `countRows`. Snapshot client-v2 `GenericRecord` rows into a `Map` during iteration. Sink-task tests create their tables before `start()`. No Mockito inline-mock of `ClickHouseHelperClient`. Spotless is baseline-equal.
- Existing tests: add rows, never delete or weaken; a flip of an existing assertion needs seen-red evidence (the D-04 flip is the one sanctioned flip, see D-10).
- Nothing is pushed. `origin/custom_build` stays frozen at `69cd680b`. Dependabot PRs #4 and #10 stay open and unmerged.

</domain>

<decisions>
## Implementation Decisions

### Builder shape and home (DESC-07)
- **D-01:** The shared DESCRIBE-row builder is a **new fork-owned class in `sink.db.helper`** (working name `DescribeTableBuilder`; the research calls it `DescribedTableBuilder`, either name is fine), the same shape as `DeclaredNullability`: the fork logic lives outside upstream's method bodies, and `describeTableV1`, `describeTableV2` and the unit test all call it. The next upstream sync conflicts only on the two call sites. One FORK-DELTA row and one anchor row on the call. — **Reversibility:** reversible — inlining the class back into the two methods restores the pre-extraction text.
- **D-02:** The builder consumes **one raw JSON line per row** (`addRow(String jsonLine)`), parsing with `ClickHouseFieldDescriptor.fromJsonRow` inside. V1 passes `v.asString()`, V2 passes `br.readLine()`; both methods shrink to query, stream, feed, `build()`. The unit table feeds captured JSONL rows unchanged, so the Docker-free pin exercises `fromJsonRow` too, and Phase 28 rewrites `fromJsonRow` against this one entry point. The alias/materialized/ephemeral branch, the parent-skipped branch, `numColumns` counting, `declaredTypes` capture, the unmappable-column `null` result and the `.inner` early return keep their current behaviour (the extraction is behaviour-preserving by requirement).

### Classifier fail mode
- **D-03:** When a size-named sub-column row arrives and the parent's declared type cannot be parsed into an element list (`ClickHouseColumn.of` throws, or no raw type is known for that parent), the classifier **fails closed to the status quo**: the row is treated as structural and dropped. An affected table keeps failing exactly as on gg3, and no structural row can leak into the Column list. Rejected: keep the row (a leaked structural row adds bytes to every write, turning a failed write into a wrong write); refuse the table (turns a parse edge case into a whole-table outage for shapes that store fine today).
- **D-04:** On that fail-closed path the classifier logs **one distinct metadata-only WARN** at the drop site, naming database, table, column (parent) and raw type and saying the declared type could not be parsed. `DeclaredNullability`'s COLS-14 unpaired-element WARN still fires for the same table, so the operator sees cause and symptom. No record data, no element values. The WARN text is new rollout-note material for Phase 29. Rejected: reuse the unpaired WARN only (cannot tell a parse failure from any other drop); DEBUG only (diagnosis needs a log-level change).

### Fixture provenance for the unit table (DESC-06, DESC-08)
- **D-05:** The "captured real DESCRIBE rows" are **committed JSONL resources under `src/test/resources`**, one file per fixture table, holding the raw `DESCRIBE TABLE … FORMAT JSONEachRow` lines with `describe_include_subcolumns=1`, with the server version recorded (header comment or sidecar, planner's call). The DESC-08 unit table loads them and feeds each line to the builder (D-02). Inline Java strings are not the primary fixture form any more; today's R-01 string arrays stay where they are (the D-10 flip keeps them).
- **D-06:** The resources are produced by a **committed capture harness in the test tree** (a JavaExec task or a `main()` class, `ConfluentArchiveCheck`/`verifyConfluentArchive` precedent): it starts the Testcontainers ClickHouse, creates every fixture table from a **committed DDL list** (the E-15 13 affected, 19 look-alikes, `s4_o01`, and the five new shapes), runs the DESCRIBE and overwrites the JSONL files, printing the server version. The DESC-06 spike record is its transcript plus the committed files; anyone re-captures with one documented Gradle command. The DDL list is the single source for the capture, the drift premise (D-07) and the live round-trip (D-08).
- **D-07:** A **live drift premise** guards the resources: one Docker-backed test creates each fixture from the committed DDL, runs DESCRIBE and asserts the rows equal the committed JSONL line for line (the COLS-13 `*_RAW` premise precedent). When a newer `latest` image changes a DESCRIBE shape, the suite names the table that drifted instead of the unit pin silently testing stale rows. It runs in the full suite, not in `testUnit`.

### Live round-trip depth (DESC-09)
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` § "Phase 26: Size-Named Tuple Elements in Discovery" — goal, success criteria 1–5, the research flag, the order note, the classifier inputs, the `Table.java` and rollout-fact notes, the phase gate; § "Phase 999.4" backlog entry (the defect sketch, scheduled 2026-10-21); § "Structure Notes" (files shared across phases: the describe builder, `Table.java`, `DeclaredNullability`, FORK-DELTA rows, anchors, CHANGELOG, backlog entries).
- `.planning/REQUIREMENTS.md` — DESC-06..DESC-10; the scope decisions of 2026-10-21 ("fix both drop sites"); COV-F1 (V1 CI coverage is a future requirement, not this phase).
- `.planning/STATE.md` § Accumulated Context → Decisions and § Blockers/Concerns — the carried rules (`env -u TESTCONTAINERS_HOST_OVERRIDE`, `--rerun`, typed read-back, no inline-mock of the helper client, the Phase 25 tail gate D-15 and baseline tag D-17, the `gradlew.bat` CRLF diff to exclude from verify strings).
- `.planning/PROJECT.md` — milestone framing (`v1.7.0_gg4-hardening`), Key Decisions (Phase 23 D-01..D-04, Phase 19 COLS-10, Phase 24 D-15).

### Measurements this phase builds on
- `.planning/milestones/v1.7.0_gg3-hardening-phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` § R-01 — the raw DESCRIBE rows for `Tuple(size …)`, `Nullable(Tuple(size0 …))`, `Array(Tuple(size …))`, the dropped-row list, the code-33 inserts and the crafted value `"abc" + U+0003 + "xyz"` that stored `(1667391751,'xyz')` (D-09).
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-COMMAND-EVIDENCE.md` § E-15 — the fixture DDL for the 13 affected tables (`s4_a01`..`s4_a13`), the 19 look-alikes (`s4_u01`..`s4_u19`) and `s4_o01`; the WARN oracle; the suspects query; the code 32/33 insert outcomes (D-06, D-08).
- `.planning/milestones/v1.7.0_gg3-hardening-phases/23-declared-nullability-hardening-review-closure/23-REVIEW.md` § WR-02 — the finding this phase closes (the unit test copies the production loop).

### Research for this phase
- `.planning/research/SUMMARY.md` — § Discovery defects (999.4 and 999.5): two drop sites, the structural-twin cascade, spike-extract-fix order, backslash-free names, the retried-batch bytes and clean-stop facts, the note content for Phase 29; § Phase 26.
- `.planning/research/ARCHITECTURE.md` § 2 "Backlog 999.4" and § 2.5 "WR-02: extract the DESCRIBE loop" — the classifier design, the element-key observation at `collectTuple`, the key-helper row in the component table.
- `.planning/research/FEATURES.md` § 1.1 "What DESCRIBE gives a rule to work with" and § 2.1 "Backlog 999.4" — the DESCRIBE row shapes and the rule.
- `.planning/research/PITFALLS.md` — Pitfall 5 (the rule is wider than `shouldSkipDescribeRow`, fails in both directions), Pitfall 19 (`.size1` and friends: preserve existing quirks).

### Fork records this phase edits
- `docs/FORK-DELTA.md` — the `shouldSkipDescribeRow` row (HLP-02/HLP-03/DESC-05), the `DeclaredNullability` row (COLS-10..14), Known costs item 10 (retired by DESC-10), the "sync should re-read" sentence in the release history.
- `.planning/scripts/fork-anchors.txt` — rows that move or are added: `ClickHouseHelperClient.java shouldSkipDescribeRow 3 HLP-02`, `buildDescribedTable( 3 COLS-10`, `DeclaredNullability.apply( 1 COLS-10`; new rows for the builder call, the `Table.java` change and the element-key helper.
- `CHANGELOG.md` — one line in a new `1.7.0_gg4` section (Phase 29 completes the section).
- `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` and `.planning/scripts/conflicted-paths-v1.7.0-tail.txt` — the standing tail gate's inputs. `.planning/scripts/verify-merge-invariants.sh` and `-selftest.sh` are read, never edited.

### Prior decisions carried
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-CONTEXT.md` — D-15 (tail gate command), D-17 (baseline tag), the Docker-free test conventions, the "measured, not cited" discipline and the preference for standing guards over one-off evidence.
- `AGENTS.md` — high-risk files (`ClickHouseWriter`, state classes untouched by this phase), test rules (typed read-back, DRY, never delete or weaken), the pre-PR obligations (`spotlessApply`, `VERSION`/CHANGELOG).

</canonical_refs>

<code_context>
## Existing Code Insights

### Current state (scouted 2026-10-21 in this discussion)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`: `shouldSkipDescribeRow` (`:473-488`) drops `.null`, `.size0`, bare `.size` and any row below a JSON/Dynamic ancestor; `buildDescribedTable` (`:500-514`) runs `DeclaredNullability.apply` per Tuple-containing root; `describeTableV1` (`:515-589`) and `describeTableV2` (`:591-663`) each hold the same 40-line loop (`.inner` early return, `numColumns`, alias/materialized/ephemeral skip, parent-skipped skip, structural skip, `declaredTypes` for roots, `hasDefaults`, `Column.extractColumn`, `null` on an unmappable column). V1 feeds `r.getValue(0).asString()`, V2 feeds `br.readLine()` in UTF-8; both go through `ClickHouseFieldDescriptor.fromJsonRow`.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java`: `SIZE_FIELD_MATCHER = .+\.size[0-9]+$` (`:24`); the ARRAY branch of `updateParent` returns for a matching child or `.null` (`:120`), the MAP branch for `.keys` or a match (`:144`); the TUPLE branch adds the child to `tupleFields` (`:194`). Upstream-identical today; no anchor row exists for it yet.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java`: `collectTuple` (`:218-250`) derives the element key (`getColumnName()` or `String.valueOf(i + 1)`), pairs on `tuple.getName() + "." + key`, and reports `unpaired` for the `UNPAIRED_ELEMENT_WARNING`; `normalise` (`:77`) is the quote-aware pass Phase 28 rewrites. `Column.extractColumn` creates a Tuple with empty `tupleFields`; elements arrive only through `Table.updateParent`.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java`: `discover(...)` (`:287`) and `discoverAsDescribeTableV2(...)` (`:316-342`, the WR-02 copy); the R-01 rows as string arrays (`:582-620`); the D-04 pin (`:624-660`) asserting the dropped element, one WARN and `apply() == true`.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java`: `bareSizeSubcolumnsDoNotLeakIntoRootColumns` (`:285`), `describeTableSkipsRowsAtAnyDepthBelowJson` (`:308`, DESC-05), `describeTableNoLongerWarnsAndCarriesDeclaredNullability` (`:402`); builds the helper with `.useClientV2("V2".equals(csc.getClientVersion()))`, so `CLIENT_VERSION=V1` is the manual V1 run.
- `src/test/resources/` holds `connect-api-references.txt` (Phase 25's committed reference list), `clickhouse_keeper_map.xml`, `log4j.properties`; no JSONL fixtures yet.
- The Phase 24 probe (`Phase24RolloutProbe`) lived in `/tmp` worktrees and is not in the tree; only its DDL and outputs survive in E-15.

### Reusable Assets
- `DeclaredNullability`: the fail-open-per-root WARN shape (`FALLBACK_WARNING`, metadata only, class name not message) is the model for D-04's classifier WARN; `ClickHouseColumn.of(name, normalise(type))` is the parser the classifier reuses for the parent's declared element list.
- `ConfluentArchiveCheck` + `verifyConfluentArchive` (JavaExec, test source set, JDK only) and `connect-api-references.txt`: the precedent for D-06's committed capture harness and committed resource files.
- `LogCapture` (`src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java`): WARN counting for the D-10 flip and the D-11 zero-WARN assertion.
- `NullableTupleMatrixLiveTest`: the `RowCase` matrix shape, table creation before `start()`, `put()` through the sink task and typed read-back, plus the COLS-13 `*_RAW` live premise that D-07 copies.
- `ClickHouseBase` and the test helpers: database per class, `latest` image, the 18 named skips.
- E-15's 33 fixture DDLs and the suspects query, ready to become the committed DDL list.

### Established Patterns
- Fork logic in a new `src/main` file outside upstream's method bodies, with one anchor on the call site (`DeclaredNullability`, Phase 19); Phase 26 repeats it for the builder.
- Metadata-only logging: database, table, column, raw type and exception class; never a record value.
- Red-first: a changed or renamed pin is seen red on the pre-fix tree before it goes green (Phase 21 D-05, Phase 23 fixes).
- Docker-free tests are untagged, do not extend `ClickHouseBase`, run in `testUnit`; live tests extend `ClickHouseBase` and run in the full suite.
- Anchor rows move in the same commit as the code they pin; document edits that cite a measurement land after the measurement.

### Integration Points
- The builder is called from `describeTableV1`, `describeTableV2` and the unit test; Phase 28 adds the V1 raw-line UTF-8 read on top of it and does not re-extract.
- The element-key helper is used by `collectTuple`, by the classifier and, in Phase 28, by the raw-name pairing.
- `Table.updateParent` receives the classifier's verdict (Claude's discretion on the carrier) and keeps its structural drops for everything that is not a declared element.
- The committed DDL list feeds the capture harness (D-06), the drift premise (D-07) and the live round-trip (D-08, D-11).
- FORK-DELTA rows, the CHANGELOG line and the rollout facts feed Phase 29's rollout note; the tail gate and the baseline tag come from Phase 25.

</code_context>

<specifics>
## Specific Ideas

- The user again chose the durable, measured mechanism at every fork: a committed capture harness over a throwaway probe, committed JSONL over retyped strings, a live drift premise over manual re-capture, the full E-15 matrix over the affected subset, and a renamed red-first pin over a quietly inverted one.
- Fail closed where the classifier is unsure: an affected table may keep failing as on gg3, but a structural row must never leak into the wire format. Say why in a WARN of its own.
- The crafted R-01 value (`"abc" + U+0003 + "xyz"`) is the one record that turns the "silently wrong stored value" finding into a green pin and backs the Phase 29 data-tail fact.
- The builder is where Phase 28 will plug in; keep its entry point the raw JSON line.

</specifics>

<deferred>
## Deferred Ideas

- Client V1 coverage of the describe path in CI (a V1/V2 Table-parity live test, or a V1 run in `tests.yaml`): already listed as future requirement COV-F1; this phase records one manual V1 run only.
- A metadata-only WARN when a declared element is kept that an earlier release would have dropped (an upgrade hint): not requested; the CHANGELOG line and the rollout note carry that information.
- The `Table.java` upstream contribution (offering the "declared element" rule upstream): outside this fork's phases.

</deferred>

---

*Phase: 26-size-named-tuple-elements-in-discovery*
*Context gathered: 2026-10-21*
