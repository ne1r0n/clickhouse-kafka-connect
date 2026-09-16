# Phase 2: Fork SMT Coverage - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-16
**Phase:** 02-fork-smt-coverage
**Areas discussed:** Bug-vs-pin policy, Sink-level test design, Unit test layout, Config-error contracts

All four offered gray areas were selected for discussion.

---

## Bug-vs-pin policy

### CONV-02 — what shape should the ConvertToMapOrJson fix take?

| Option | Description | Selected |
|--------|-------------|----------|
| Recurse into collections | Walk List/Collection elements and Map values, applying Struct→Map **and** bytes→base64 recursively | ✓ |
| Structs only, no bytes | Recurse for nested Structs but leave byte[]/ByteBuffer inside collections alone | |
| Pin current behavior | Test what happens today (nested Struct leaks through), change no main code | |

**User's choice:** Recurse into collections
**Notes:** Rejected "Structs only" because `bytes.to.base64` would then behave differently depending on nesting depth. Rejected pinning because CONV-02 is explicitly "fixed if broken".

### Stale valueSchema cache in HeadersToValue / HeaderToValue

| Option | Description | Selected |
|--------|-------------|----------|
| Fix with source-schema guard | Store the schema the cache was built from; rebuild on change; close() nulls it too | ✓ |
| Pin it as-is | Document the stale-cache behavior in a test, change no main code | |
| Leave it out of Phase 2 | Not one of the 30 requirements — defer entirely | |

**User's choice:** Fix with source-schema guard
**Notes:** Not a listed requirement, but a real data-loss path for the Debezium pipelines this fork serves. ~3 lines per transform.

### TableOperationFilter.configure() accumulates config state

| Option | Description | Selected |
|--------|-------------|----------|
| Fix: clear at configure() start | `targetTables.clear()` / `skippedOperations.clear()` at the top of configure() | ✓ |
| Pin: fresh instance per test | Rely on the existing `@BeforeEach setUp()` constructing a new instance | |

**User's choice:** Fix: clear at configure() start
**Notes:** Connect only calls configure() once per instance, so production behavior is unchanged; the fix removes a trap for reconfigure-style tests.

### Escalation rule for unanticipated bugs

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 1's D-02 rule | Fix inline if small (few lines, one method, rebase-friendly); larger stops for a user decision; never disable a test | ✓ |
| Free rein inside transforms/ | Fix anything wholly inside the four SMT classes regardless of size | |
| Pin everything else | The three fixes above are the only main-code changes; pin anything else | |

**User's choice:** Phase 1's D-02 rule
**Notes:** Consistency with Phase 1 — executors already ran on this rule.

---

## Sink-level test design

### How should sink tests apply an SMT before task.put()?

| Option | Description | Selected |
|--------|-------------|----------|
| Shared helper in the test class | Private helper mirroring the worker: apply per record, drop nulls | ✓ |
| Inline per test | `records.stream().map(xform::apply).filter(Objects::nonNull).toList()` in each test | |
| Real TransformationChain | Add `connect-runtime` as testImplementation and drive the actual worker chain | |

**User's choice:** Shared helper in the test class
**Notes:** `connect-runtime` was ruled out on inspection — only `connect-api` is a dependency (`build.gradle.kts:86`), and a second new build dependency conflicts with the Tooling constraint.

### TOF-06 — how does a Debezium envelope land in a ClickHouse table?

| Option | Description | Selected |
|--------|-------------|----------|
| Tuple column for source | Keep the real envelope; table gets `source Tuple(db String, \`table\` String)` | ✓ |
| Flat table + bypassSchemaValidation | Table holds only id/op; tolerate the unmapped source field | |
| Flatten source into columns | Flat `source_db` / `source_table` fields with plain String columns | |

**User's choice:** Tuple column for source
**Notes:** TOF-04 pins schemaless envelopes as pass-through, so TOF-06 is forced onto the Struct path. Flagged as R-01: confirm Struct→Tuple mapping works on `latest` before the plan locks it; flattening is the named fallback.

### CONV-08 — what does "lands in a String/JSON column" mean?

| Option | Description | Selected |
|--------|-------------|----------|
| Keys to typed columns, nested to JSON column | Assert the real end-to-end String path: scalars in typed columns, nested struct in a JSON column | ✓ |
| Whole document in one String column | Wrap the payload so the entire JSON doc lands in one String column | |
| Map output instead of JSON | Run with `output.json.enabled=false` and exercise the schemaless path | |

**User's choice:** Keys to typed columns, nested to JSON column
**Notes:** The connector's `StringRecordConvertor` path parses the JSON and maps keys to columns, so a single-column store would need an artificial wrapper nothing in production produces. Flagged as R-02: if the String path turns out to use RowBinary rather than JSONEachRow, use a String column here and leave JSON to Phase 3.

### Which record path for HDRS-09 / HDR-07?

| Option | Description | Selected |
|--------|-------------|----------|
| Schema/Struct path only | Struct-valued records so the SMT's generated map schema drives the insert | ✓ |
| Both paths for HDRS-09 | Add a fifth sink test on the schemaless path | |
| Schemaless path only | Map-valued records through JSONEachRow | |

**User's choice:** Schema/Struct path only
**Notes:** The map schema `HeadersToValue.configure()` builds is the fork addition worth pinning at sink level; the schemaless path is already covered by HDRS-02 / HDR-03 unit tests.

---

## Unit test layout

### Where should the new unit tests live?

| Option | Description | Selected |
|--------|-------------|----------|
| Grow the four existing files | Add cases to the existing per-SMT test classes; files reach ~350-600 lines | ✓ |
| New sibling classes per SMT | e.g. `TableOperationFilterEdgeCasesTest` alongside the existing file | |
| Grow files, group with @Nested | Same files, cases organized into @Nested inner classes by concern | |

**User's choice:** Grow the four existing files
**Notes:** Upstream has none of these SMTs, so the files are fork-only and carry no rebase conflict. @Nested would be a new convention — no test in the repo uses it.

### Should tests carry their requirement ID?

| Option | Description | Selected |
|--------|-------------|----------|
| @DisplayName with the ID | `@DisplayName("TOF-03: ...")`, method names stay in the `test*` style | ✓ |
| ID in the method name | `testTof03InvalidSkippedOperationsIgnored()` | |
| No IDs in tests | Record the requirement→test mapping once in SUMMARY.md | |

**User's choice:** @DisplayName with the ID
**Notes:** Gives Phase 5 a greppable mapping that can't drift from the tests. @DisplayName is already used in 5 test classes.

### Plain @Test or @ParameterizedTest for matrix-shaped requirements?

| Option | Description | Selected |
|--------|-------------|----------|
| Parameterized for matrices only | @ParameterizedTest for TOF-03 tokens, CONV-01 libraries, schema-vs-schemaless; plain @Test elsewhere | ✓ |
| Plain @Test everywhere | One @Test per case, matching all existing tests in the repo | |
| Parameterize aggressively | Reach for @ParameterizedTest wherever two or more cases share a body | |

**User's choice:** Parameterized for matrices only
**Notes:** Confirmed no build change is needed — the `junit-jupiter` aggregate (BOM 6.0.2) already brings `junit-jupiter-params`, though nothing in the repo uses it yet.

### Shared fixtures for the transform tests?

| Option | Description | Selected |
|--------|-------------|----------|
| Small shared helper class | Record builders + a single `assertMetadataPreserved(in, out)` in `src/test/java/transforms/` | ✓ |
| Keep helpers private per class | Each test class grows its own `generateSampleRecord` and assertions | |
| Shared assertion only | Share `assertMetadataPreserved`, keep builders private | |

**User's choice:** Small shared helper class
**Notes:** HDRS-06, HDR-05 and CONV-04 are literally the same assertion across three SMTs; the sink-level class can reuse the builders too.

---

## Config-error contracts

### CONV-05 — case-sensitivity and the unreachable DataException branch

| Option | Description | Selected |
|--------|-------------|----------|
| Pin both, change nothing | Assert ConfigException for an unknown library and for "JACKSON"; leave the dead branch | ✓ |
| Pin, and drop the dead branch | Same assertions plus deleting the unreachable try/catch | |
| Make it case-insensitive | Widen ValidString so JACKSON/Gson are accepted | |

**User's choice:** Pin both, change nothing
**Notes:** `ConfigDef.ValidString.in("jackson","gson")` validates inside `new AbstractConfig(...)`, so the `DataException` at lines 100-104 is unreachable and `JACKSON` is rejected despite the `.toLowerCase()` call. Widening it would be a config-semantics change the milestone rules out.

### HDRS-04 — requirement text doesn't match the code

| Option | Description | Selected |
|--------|-------------|----------|
| Pin reality, fix the requirement text | Absent → ConfigException, blank → DataException; correct HDRS-04 in REQUIREMENTS.md; check HDR-01 too | ✓ |
| Assert the common supertype | Assert KafkaException for the absent case so no doc edit is needed | |
| Make absent throw DataException too | Give the config a `""` default so absent falls through to the explicit check | |

**User's choice:** Pin reality, fix the requirement text
**Notes:** `FIELD_CONFIG` is `NO_DEFAULT_VALUE`, so an absent key throws `ConfigException` at `HeadersToValue.java:46` before the explicit check at line 50 ever runs.

### TOF-03 — how to observe warn-and-ignore

| Option | Description | Selected |
|--------|-------------|----------|
| Behavioral assertion only | configure() doesn't throw; records with that op pass through untouched | ✓ |
| Add a log appender | Attach a reload4j appender and assert the WARN line | |
| Behavioral now, appender only if reused | Defer the appender to Phase 3's DESC-04 if that needs it anyway | |

**User's choice:** Behavioral assertion only
**Notes:** An appender would couple tests to log wording and touch shared logger state under `maxParallelForks`.

### How deep should negative tests assert on wrapped exceptions?

| Option | Description | Selected |
|--------|-------------|----------|
| Type, plus cause where it's the contract | Assert the type always; substring-match the inner cause message where that message is the contract | ✓ |
| Exception type only | `assertThrows(DataException.class, ...)` and stop | |
| Full message matching | Exact message text at every level of the chain | |

**User's choice:** Type, plus cause where it's the contract
**Notes:** All three header/filter transforms wrap `apply()` failures in a generic `DataException`, so the real diagnostic lives on the cause.

---

## Claude's Discretion

- Name and package of the new ClickHouse-backed sink test class
- Name of the shared unit-test helper class and its exact set of builders
- Whether each sink-level test gets its own table (likely, since the column shapes differ)
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix
- Which specific cases become `@ParameterizedTest` within the named matrices
- Record counts per sink test and the read-back assertion helper

## Deferred Ideas

- `HeadersToValue.close()` nulls `headersSchema`, so close-then-apply would fail — not a legal Connect lifecycle, no test this phase
- Removing the unreachable `DataException` branch in `ConvertToMapOrJson.configure()` — behavior-neutral cleanup, declined
- A fifth sink test covering HDRS-09 on the schemaless path — declined to stay within "one per SMT"
- Case-insensitive `output.json.library` — declined as a config-semantics change
- `HeaderToValueConfig` dead-code question — already tracked as v2 requirement CLEAN-01
