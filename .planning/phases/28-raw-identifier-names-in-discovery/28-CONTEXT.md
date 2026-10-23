# Phase 28: Raw Identifier Names in Discovery - Context

**Gathered:** 2026-10-23
**Status:** Ready for planning

<domain>
## Phase Boundary

A column or Tuple element whose name contains a backslash, a literal backslash-n, a line feed, a tab, a carriage return, a slash, a double quote, a backtick or a non-ASCII character is matched under its raw identifier, as written in `CREATE TABLE`. A Kafka record using that name lands, and the typed read-back equals it. Declared nullability still pairs. The doubled form's behaviour is pinned as the rollout note will state it, with no dual-form alias. This closes backlog 999.5 and FORK-DELTA Known costs item 11, and the fork coverage report lists every fork class.

Requirements: NAME-01, NAME-02, NAME-03, NAME-04, NAME-05, NAME-06, NAME-07, DEBT-17.

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

</domain>

<decisions>
## Implementation Decisions

### Enum labels (NAME-06)
- **D-01:** Enum labels are **fixed**, not documented. `Column.extractEnumValues` currently reads labels from the still-escaped type text, so a label with an escaped character is keyed in escaped form and a raw-form record value misses (the Phase 27 `IllegalArgumentException` lookup miss). The fix unescapes the label inside `Column.extractEnumValues` only, so labels follow the same raw-identifier rule as names. — **Reversibility:** costly — it changes the bytes of retried batches for enum columns with escaped labels, so Phase 29's rollout note must cover it.
- **D-02:** Scope guard: if the fix would reach beyond `Column`'s enum-label parsing, planning stops for a user decision (ROADMAP research flag). The Phase 27 lookup-miss pins stay and must remain green. Each enum label that round-trips gets a live pin with typed read-back. If the spike shows a label shape that cannot be fixed inside `extractEnumValues`, that shape's remaining behaviour goes into FORK-DELTA Known costs and the Phase 29 record.

### Double-quote names
- **D-03:** A double quote in a name is a **hard requirement**: the table must map (today `describeTable` returns null for it, FORK-DELTA Known costs item 11) and a record using the raw name must land with typed read-back equal. If the cause of the unmapped table lies outside `fromJsonRow` and the V1 raw-line read, the plan stops and asks the user before widening the fix. — **Reversibility:** reversible.

### Spike scope and evidence (NAME-02)
- **D-04:** The matrix covers the full shapes: root column, Tuple element, Array(Tuple) element, Nullable(Tuple) element, Map-value Tuple element, and enum label, for every character in the goal (backslash, literal backslash-n, LF, tab, CR, slash, double quote, backtick, non-ASCII). Each cell records the current form and the decoded form. Run live on `latest` × V2 in the suite, plus the one manual `CLIENT_VERSION=V1` run the ROADMAP requires for the V1 raw-line UTF-8 read. The record is kept as a phase evidence file and is the source for Phase 29's per-character old/new name table.

### Doubled-form pins and handoff (NAME-05)
- **D-05:** One **new `ClickHouseBase` live test** pins both forms per spiked character: raw name lands with typed read-back equal to the record; doubled form behaves as today (root column ignored and `DEFAULT` stored; Tuple element fails loudly). Pinned, not changed. `NullableTupleMatrixLiveTest` stays the pairing oracle and is not mixed with write-path pins; its COLS-13 twins must stay green, and (per Phase 27 DEBT-16) a premise lookup by the old name must fail loudly after the decode.
- **D-06:** Phase 28 hands Phase 29 a record, not the rollout note itself: the measured per-character old/new name table, the root-silent versus element-loud behaviour, the enum-label outcome, and the clean stop in both directions with the rename as its precondition. `docs/ROLLOUT-v1.7.0_gg4.md` is written in Phase 29 only.

### Claude's Discretion
- The mechanism of the names-only decode in `ClickHouseFieldDescriptor.fromJsonRow` (which fields are decoded and how the type text is kept unchanged) and the shape of the rewritten `normalise`, within the rules above.
- The exact form of the NAME-01 mutation proof and the red-first evidence for each pin.
- The pairing-key helper shape, reusing Phase 26's single element-key helper in `DeclaredNullability`.
- Which classes `jacocoForkReport` lists for DEBT-17 beyond `DeclaredNullability`, `WithheldMessageException` and the classes Phases 25–28 added (the list is derived from the diff, edited once, with its FORK-DELTA row in the same commit).
- The plan split and commit order inside the fixed seven-step order; the evidence file layout.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` § "Phase 28: Raw Identifier Names in Discovery" — goal, success criteria 1–5, order note, constraints; § "Phase 999.5" backlog entry.
- `.planning/REQUIREMENTS.md` — NAME-01..NAME-07, DEBT-17; Out of Scope rows for dual-form lookup and type-text decode; NAME-F1.
- `.planning/STATE.md` — carried rules (`env -u TESTCONTAINERS_HOST_OVERRIDE`, `--rerun`, typed read-back, tail gate).
- `.planning/PROJECT.md` — milestone framing and Key Decisions.
- `AGENTS.md` — high-risk files, test rules, comment rules.

### Prior phase decisions
- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-CONTEXT.md` — the extracted `DescribeTableBuilder` and the single public element-key helper in `DeclaredNullability` this phase rewrites against.
- `.planning/phases/27-writer-failure-path-test-debt/27-CONTEXT.md` — D-05..D-07 (enum lookup miss in `Column.convertEnumValues`), D-13 (premise loops), Inputs to Phase 28.

### Findings and evidence
- `.planning/milestones/v1.7.0_gg3-hardening-phases/23-declared-nullability-hardening-review-closure/23-REVIEW.md` § WR-01, IN-01, IN-02.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` § R-02 — doubled-form measurement.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-COMMAND-EVIDENCE.md` § E-16 — identical behaviour on gg2/gg3.
- `docs/FORK-DELTA.md` — Known costs item 11 (retired by this phase), the `ClickHouseHelperClient`/`Table.java` rows, the jacoco scope row.

### Code this phase edits
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java` — `fromJsonRow` (`json.replace("\\", "\\\\").replace("\n", "\\n")`).
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` — V1 describe path (`describeTableV1` raw line reading) and V2 path.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java`, `sink/db/mapping/DeclaredNullability.java`, `ParentShape.java` — pairing key and `normalise`.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` — `extractEnumValues`, `convertEnumValues`.
- `build.gradle.kts` — `forkChangedClasses` / `jacocoForkReport` (DEBT-17, last edit).
- `.planning/scripts/fork-anchors.txt` — anchors move in the same commit as the code they pin.
- `CHANGELOG.md` — one line in the `1.7.0_gg4` section.

### Tests
- `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` (COLS-13 twins, pairing oracle), `db/mapping/DeclaredNullabilityTest.java`, `db/mapping/DeclaredNullabilityFixtures.java`, `db/helper/DescribeFixtureCatalog.java`, `DescribeTableBuilderUnitTest.java`, `ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `TableTest.extractCommentV1`.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DescribeTableBuilder` (Phase 26): one fork-owned builder for V1 and V2 describe loops, so the decode lands once.
- Element-key helper in `DeclaredNullability` (Phase 26): the single definition of a Tuple element's name; the pairing key rewrite reuses it.
- `DescribeFixtureCatalog` / `DescribeFixtureCapture`: captured real DESCRIBE rows for Docker-free tables.
- `Column.convertEnumValues` and its Phase 27 lookup-miss pins: the enum measurement baseline.
- `NullableTupleMatrixLiveTest` COLS-13 twins: live oracle for pairing after the decode.

### Established Patterns
- Red-first: a changed pin is seen red on the unchanged tree before it goes green.
- Anchor rows move in the same commit as the code they pin; document edits that cite a measurement land after the measurement.
- Docker-free tests are untagged and run in `testUnit`; live tests extend `ClickHouseBase`.
- Value-free failure text: column metadata and class names only, never record values or keys.

### Integration Points
- `fromJsonRow` doubles backslashes and LF before Jackson parses, so every escaped name character comes out in escaped form; that is the single decode site for names.
- `extractEnumValues` parses labels from the escaped type text; the type text stays escaped, so labels need their own unescape.
- `describeTable` returns null for a double-quote name today (unmapped table).

</code_context>

<specifics>
## Specific Ideas

- The user chose the raw identifier as the canonical form on 2026-10-21; operators rename. All four discussed areas took the recommended option.

</specifics>

<deferred>
## Deferred Ideas

- NAME-F1: a one-release metadata-only WARN for a Kafka field matching only the doubled form (recommended against; reconsider if operators report missed renames).
- COV-F1: client V1 coverage in CI for the describe path (one manual run per change remains the evidence).
- Decoding the DESCRIBE type text in `fromJsonRow` (out of scope, changes every multi-line Tuple type).

</deferred>

---

*Phase: 28-Raw Identifier Names in Discovery*
*Context gathered: 2026-10-23*
