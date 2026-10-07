# Phase 7: Type Mapping, Writer, Helper Client & Config - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-18
**Phase:** 7-Type Mapping, Writer, Helper Client & Config
**Areas discussed:** Phase 7's real test gate, The one compile blocker, Phase 9's inherited default, Delta-shrink vs re-apply

---

## Phase 7's real test gate

### Q1 — How to draw the Phase 7 / Phase 10 boundary for `./gradlew testUnit`

| Option | Description | Selected |
|--------|-------------|----------|
| Mechanical carve-out | Pull forward only the mechanical harness fixes testUnit needs (`@Tag("clickhouse")` + `uniqueConnectorName`, the 1 stale extension import, re-expressing the 4 fork test files against the immutable `Table`); Phase 10 keeps all judgement work. Gate stays `compileJava` + `testUnit` green. | ✓ |
| Full test-tree compile in P7 | Make `compileTestJava` + `compileTestFixturesJava` pass tree-wide regardless of which HARN requirement each error touches, without changing harness semantics. | |
| Downgrade the gate | Gate becomes `compileJava` only; `testUnit` green moves to Phase 10 and all Phase 7 test evidence lands one phase later. | |

**User's choice:** Mechanical carve-out
**Notes:** Blast radius was measured before the question was asked — `uniqueConnectorName` used by 9 fork test files and absent from the merged `ClickHouseBase`; `@Tag("clickhouse")` absent; 1 stale `FromVersionConditionExtension` import; 4 fork test files on the dead `Table` API.

### Q2 — Which `ClickHouseBase` members Phase 7 restores

| Option | Description | Selected |
|--------|-------------|----------|
| Tag + uniqueConnectorName only | The two that unblock testUnit and nothing else; MBean sweep, container strategy, `newTestDatabaseName` and helpers stay Phase 10. | ✓ |
| Tag + name + MBean sweep | Also restore `@AfterEach cleanupLeakedMBeans` now, since Phase 7's tests register MBeans; Phase 10 still extends it to upstream's per-topic MBeans. | |
| Whatever the compiler demands | Add members only when a test file fails to compile without them. | |

**User's choice:** Tag + uniqueConnectorName only
**Notes:** The fork's base is `static`-based while upstream's is `@TestInstance(PER_CLASS)` with instance methods, so a wholesale restore was never available; both restored members are shape-independent.

### Q3 — Checklist bookkeeping for the Phase-10-owned `ClickHouseBase.java` row

| Option | Description | Selected |
|--------|-------------|----------|
| Leave pending, add a note | Row stays `pending` and owned by Phase 10; Phase 7 appends a note recording that the sliver landed early and in which commit. | ✓ |
| Split the row | Split into a Phase 7 row (flipped `done` here) and a Phase 10 row. | |
| Record it only in the SUMMARY | Touch nothing in the checklist. | |

**User's choice:** Leave pending, add a note
**Notes:** Preserves Phase 11's zero-pending assertion and the checklist's own "do not flip a row you do not own" rule.

### Q4 — Shape of the re-expressed `shouldSkipDescribeRow`

| Option | Description | Selected |
|--------|-------------|----------|
| Pass an incremental name→Column map | `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)`; loop maintains the map alongside `columns`; helper stays a pure static function with O(1) lookup; unit test builds the map directly. | ✓ |
| Pass the accumulating `List<Column>` | Scan by name; one fewer local, but a linear scan per DESCRIBE row on wide tables. | |
| Keep the `Table` signature | Construct an interim `Table` each iteration so the test barely changes; per-row object churn inside the describe loop. | |

**User's choice:** Pass an incremental name→Column map
**Notes:** Forced by upstream building a local `List<Column>` and constructing the immutable `Table` only at the end, so `table.getAllColumnsMap()` is unavailable mid-loop.

---

## The one compile blocker

### Q1 — How to resolve `ClickHouseSinkTask.java:149`

| Option | Description | Selected |
|--------|-------------|----------|
| Drop only the `@Override` | Keep upstream's method and body, delete one annotation line; fork delta over upstream = 1 line; bridge still fires on a pre-3.0 Connect worker. | ✓ |
| Delete the method | Remove it entirely — unreachable under connect-api 4.1.1, `close(partitions)` already does the work; gives up upstream's old-runtime bridge. | |
| Add a shim interface | Keep the method and reach the old contract via an explicitly-documented non-overriding hook. | |

**User's choice:** Drop only the `@Override`
**Notes:** Provenance established before asking — `javap` against the resolved `connect-api-4.1.1.jar` confirms `SinkTask` declares no `onPartitionsRevoked`; upstream added the method in `c36ab418` under its 2.7.0 catalog pin; the pre-merge fork never had it. `connect-api` is declared `api(...)` and Connect serves those classes parent-first, which is why keeping the method has value.

### Q2 — Ownership of the fix

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 7 fixes, row stays Phase 8 | One-line fix lands here as a named commit with a note appended to the checklist row; row stays `pending` for Phase 8's JMX re-plumb. | ✓ |
| Pull `ClickHouseSinkTask` into P7 | Move the row to Phase 7 and flip it `done`, leaving Phase 8 only `ProxySinkTask` + `SinkTaskStatistics`. | |
| Record as a named milestone fact | Record the API gap in § Recorded Facts. | |

**User's choice:** Phase 7 fixes, row stays Phase 8
**Notes:** Same bookkeeping pattern as the `ClickHouseBase` row. The third option was re-asked separately and also adopted.

### Q3 — Whether to record the Kafka 4.1.1 vs 2.7.0 gap as a named fact

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — Recorded Facts entry | Add to `MERGE-CHECKLIST.md` § Recorded Facts; feeds SUITE-03 and warns the next sync. | ✓ |
| No — a code comment is enough | Keep the checklist focused on per-path resolution. | |

**User's choice:** Yes — Recorded Facts entry

### Q4 — Commit shape for Phase 7

| Option | Description | Selected |
|--------|-------------|----------|
| One commit per fork behaviour | Unblock-compile → `Type.DYNAMIC`+Column → RowBinary fail-fast → `targetTableFilter` → describe skip + UTF-8 → config; each an isolated diff against upstream. | ✓ |
| One commit per file | Maps 1:1 onto checklist rows, but a commit can mix unrelated behaviours. | |
| One commit per plan | Fewest commits, coarsest review granularity on the milestone's two hardest files. | |

**User's choice:** One commit per fork behaviour
**Notes:** Preserves Phase 6 D-02's property — every re-application is reviewable against `v1.6.0` — which is exactly the record SUITE-03 must produce in Phase 11.

---

## Phase 9's inherited default

### Q1 — Which default the JSONP-01 checkpoint inherits

| Option | Description | Selected |
|--------|-------------|----------|
| Leave it out — default is "dropped" | Phase 7 re-applies nothing of the bypass; Phase 9 opens with the code on upstream's `writeRecordsJson` and "keep" is the option that costs work. | ✓ |
| Re-add it — default is "kept" | Re-express the bypass against the 5-arg `Table` so its tests pass here; better-informed checkpoint, possible throwaway code. | |
| Re-add behind a hard gate | Re-apply but throw at startup unless explicitly opted in, so R-01's silent loss cannot fire while the decision is open. | |

**User's choice:** Leave it out — default is "dropped"
**Notes:** The framing was made explicit — this choice sets which side of JSONP-01 is expensive. Chosen on the milestone's prefer-upstream conflict policy.

### Q2 — How Phase 7 leaves the fork's JSON-path tests

| Option | Description | Selected |
|--------|-------------|----------|
| Compile + `@Disabled` with a JSONP-01 reason | All compile against the new `Table`, then disabled with a named reason; nothing deleted or weakened; Phase 11's skip accounting sees why. | ✓ |
| Tag them `clickhouse` to dodge testUnit | No `@Disabled` churn, but hides a red test behind a tag that means something else. | |
| Let them fail, and record it | Most honest signal, but gives up a binary gate for Phases 7–8. | |

**User's choice:** Compile + `@Disabled` with a JSONP-01 reason

### Q3 — What config Phase 7 adds when nothing reads `jsonInsertColumns` yet

| Option | Description | Selected |
|--------|-------------|----------|
| Add both keys and parsing now | `TARGET_TABLE_FILTER` + `JSON_INSERT_COLUMNS` defines, all three fields and their parsing, exactly as CFG-01 states; two briefly-unread fields is a one-phase gap, not a defect. | ✓ |
| `targetTableFilter` now, JSON keys in Phase 9 | Only add what has a live consumer; defers part of CFG-01 out of its phase. | |
| Add keys, drop `jsonInsertFormatExplicit` | Both public keys but not the derived boolean that exists solely to drive the bypass. | |

**User's choice:** Add both keys and parsing now

### Q4 — Whether Phase 7 hands Phase 9 an evidence brief

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — a short evidence section in Phase 7's SUMMARY | Record the `DataStreamWriter` insert overload at `ClickHouseWriter.java:1307`, that `jsonInsertColumns` must become a field filter inside `writeRecordsJson`, and which tests are disabled. | ✓ |
| No — Phase 9's research covers it | Avoids a stale or pre-framing brief. | |

**User's choice:** Yes — a short evidence section in Phase 7's SUMMARY

---

## Delta-shrink vs re-apply

### Q1 — Which non-behavioural fork tweaks come back (multi-select)

| Option | Description | Selected |
|--------|-------------|----------|
| `System.out` → `LOGGER.debug` | `ClickHouseHelperClient.java:315` prints to stdout on every query retry in a production Connect worker. | ✓ |
| Payload logging DEBUG → TRACE | `ClickHouseWriter.java:1355` logs the full record value at DEBUG; `AGENTS.md` forbids logging data in plaintext. | ✓ |
| WARN → ERROR on unmappable column | `ClickHouseHelperClient.java:437,497` — the whole table is dropped from the mapping, louder than WARN implies; deliberate Phase 3 decision. | ✓ |
| `schemaType` in the insert log line | `ClickHouseWriter.java:220` — first thing wanted when diagnosing a wrong-format insert. | ✓ |

**User's choice:** all four
**Notes:** `grep` for `ListAppender` / `LogCaptor` / `Appender` across `src/test` and `src/testFixtures` returns nothing, so no test pins any log level — each was judged purely on merit.

### Q2 — HLP-04 scope

| Option | Description | Selected |
|--------|-------------|----------|
| Patch the one reader, grep to prove it | Add `StandardCharsets.UTF_8` at `:466` and satisfy HLP-04 by grepping the whole resolved file; drop the fork's V1 `BufferedReader` rewrite since V1 is outside both the test matrix and production. | ✓ |
| Also re-express the V1 reader rewrite | Keeps both client paths symmetric at the cost of delta on an untested path. | |
| Patch it and add a charset regression guard | Turns the grep into a standing Docker-free gate. | |

**User's choice:** Patch the one reader, grep to prove it

### Q3 — What proves CFG-03

| Option | Description | Selected |
|--------|-------------|----------|
| Docker-free defaults test + written inventory | Parametrized test asserts each of the thirteen getters returns upstream's default; SUMMARY table names each option, default and consumer. | ✓ |
| Written inventory only | Cheapest; consistent with FREE-01..03 being record-only. | |
| Defaults test only | Mechanically rerun, but says nothing about reaching the consumer. | |

**User's choice:** Docker-free defaults test + written inventory
**Notes:** The thirteen were enumerated from the merged file during discussion rather than taken from prose.

### Q4 — Where FREE-02 / FREE-03 findings are recorded

| Option | Description | Selected |
|--------|-------------|----------|
| `MERGE-CHECKLIST.md` § Recorded Facts | Same home as Phase 6's SYNC-04/05 facts and the new Kafka-4.1.1 entry; the one place SUITE-03 reads from. WR-01's status stated either way; `PROJECT.md` → Active updated at the transition. | ✓ |
| Phase 7 SUMMARY only | Keeps the checklist strictly per-path. | |
| Both, with the checklist as canonical | Most discoverable, slight duplication. | |

**User's choice:** `MERGE-CHECKLIST.md` § Recorded Facts
**Notes:** Both free wins were confirmed live in the merged tree during discussion — upstream's `Map(K, Nullable(V))` fix at `ClickHouseWriter.java:562` carries an extra `getMapValueType() != null` guard the fork's patch lacked, and `describeTableV2` now builds its query from the passed-in `database`, closing P3-WR-02.

---

## Claude's Discretion

The user did not constrain these; the calls are recorded in CONTEXT.md § "Claude's Discretion" and any planner may override with a reason:

- WRT-02's gate contents — `DYNAMIC`/`UNKNOWN` only, `JSON` handed to upstream's `case JSON:`, `TYPE-06`'s JSON assertions rewritten to upstream's exception type and message
- WRT-01's shape — re-apply `buildRowBinaryUnsupportedTypeMessage` so both gates cannot drift in wording; throw `DataException`; never name any part of the record payload
- `Column.java` is a two-line change (upstream already dispatches `JSON` at `:173`); `Table.java` needs no production edit at all
- HLP-03 is nearly free — upstream already increments `numColumns` before its skips at `:411-413` and `:471-473`
- The four conflicted Phase 7 *test* files need nothing restored — their fork deltas were pure Spotless import reorders, verified by diff
- Run `./gradlew spotlessApply` before each commit

## Deferred Ideas

- A standing charset-regression test for `InputStreamReader` charsets
- Re-expressing the fork's `describeTableV1` UTF-8 `BufferedReader` rewrite, if client V1 ever re-enters the matrix
- Splitting `MERGE-CHECKLIST.md`'s `ClickHouseBase.java` row instead of annotating it
- A dedicated sweep for further Kafka 2.7.0-vs-4.1.1 API gaps in test sources
- Adopting `retryOnSocketException` (Future Requirements; CFG-03 keeps it at its upstream default)
- Fixing CR-01, the fail-open `verify-merge-invariants.sh` exit-code branching — carried from Phase 6, due before Phase 11
