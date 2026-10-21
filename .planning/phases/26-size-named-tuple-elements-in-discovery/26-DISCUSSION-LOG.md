# Phase 26: Size-Named Tuple Elements in Discovery - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-21
**Phase:** 26-size-named-tuple-elements-in-discovery
**Areas discussed:** Builder shape and home (DESC-07), Classifier fail mode, Fixture provenance for the unit table, Live round-trip depth (DESC-09)

---

## Builder shape and home (DESC-07)

### Where should the shared DESCRIBE-row builder live?

| Option | Description | Selected |
|--------|-------------|----------|
| New fork-owned file (Recommended) | A new class in `sink.db.helper` that `describeTableV1`, `describeTableV2` and the unit test all call; same shape as `DeclaredNullability`; the next sync conflicts only on the two call sites; one FORK-DELTA row, one anchor on the call | ✓ |
| Package-private method in `ClickHouseHelperClient` | Extract `describeRows(...)` inside the existing class as 23-REVIEW WR-02 sketched; fewer files, but the loop stays inside an upstream-region file | |

**User's choice:** New fork-owned file

### What does the builder consume per row?

| Option | Description | Selected |
|--------|-------------|----------|
| One JSON line per row (Recommended) | `addRow(String jsonLine)` parses with `fromJsonRow` inside; V1 passes `v.asString()`, V2 passes `br.readLine()`; the unit table feeds captured JSONL unchanged; Phase 28 rewrites `fromJsonRow` against this one entry | ✓ |
| One `ClickHouseFieldDescriptor` per row | Each describe method parses the line itself; the builder is parser-independent but V1 and V2 keep one duplicated line each and the unit table bypasses `fromJsonRow` | |

**User's choice:** One JSON line per row
**Notes:** Unmappable-column `null` path, `.inner` early return and which unit-test helpers the builder replaces were left to Claude's discretion; the user moved on after two questions.

---

## Classifier fail mode

### When the parent's declared type cannot be parsed, what happens to a size-named row?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep today's drop + one WARN (Recommended) | Fail closed to the status quo: the row is dropped as structural and one metadata-only WARN names database, table, column and raw type; an affected table keeps failing as on gg3, nothing new can leak | ✓ |
| Keep the row | Fail open: the row becomes a Column; a declared element survives a parse failure but a leaked structural row adds bytes to every write | |
| Refuse the table (return null table) | ERROR log and `describeTable` returns null; loudest, but a parse edge case becomes a whole-table outage | |

**User's choice:** Keep today's drop + one WARN

### Should the classifier add its own log line on that path?

| Option | Description | Selected |
|--------|-------------|----------|
| One distinct classifier WARN (Recommended) | A new metadata-only WARN at the drop site says the declared type could not be parsed; the operator sees cause and symptom; new rollout-note material | ✓ |
| Reuse the unpaired-element WARN only | No new log text; the COLS-14 WARN is the single signal; a parse failure is indistinguishable from any other drop | |
| Classifier logs at DEBUG only | Drop reason visible with DEBUG logging; WARN stays single | |

**User's choice:** One distinct classifier WARN
**Notes:** How the verdict reaches `Table.updateParent` and per-pass parse caching were left to Claude's discretion.

---

## Fixture provenance for the unit table

### Where do the captured real DESCRIBE rows come from?

| Option | Description | Selected |
|--------|-------------|----------|
| Committed JSONL resources from the spike (Recommended) | One JSONL file per table under `src/test/resources`, raw DESCRIBE lines with the server version recorded; the unit table feeds each line to the builder | ✓ |
| Inline Java strings copied from the spike | Today's R-01 style; grep-able but retyped by hand, and `\n` escaping has needed care before (COLS-13) | |
| Both: JSONL resources plus a few inline rows | Resources for the matrix, inline only for small hand-built negative cases | |

**User's choice:** Committed JSONL resources from the spike

### How are the resources produced and re-captured?

| Option | Description | Selected |
|--------|-------------|----------|
| Committed capture harness in the test tree (Recommended) | JavaExec task or `main()` class (ConfluentArchiveCheck precedent) that starts Testcontainers ClickHouse, creates every fixture from a committed DDL list, runs DESCRIBE and overwrites the files; one documented Gradle command | ✓ |
| Throwaway worktree probe, files copied by hand | Phase 24 style; nothing new in the build, but re-capture means rebuilding the probe | |

**User's choice:** Committed capture harness in the test tree

### Should a live test guard the committed JSONL against drift on `latest`?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, a live premise per fixture (Recommended) | One Docker-backed test creates each fixture from the same DDL and asserts the DESCRIBE rows equal the committed JSONL line for line (COLS-13 precedent); full suite, not `testUnit` | ✓ |
| No, re-capture is manual | The committed files are the record; drift is noticed only on a manual re-run | |

**User's choice:** Yes, a live premise per fixture
**Notes:** File naming, the DDL list's home and the server-version header format were left to Claude's discretion.

---

## Live round-trip depth (DESC-09)

### Which tables does the live round-trip insert into and read back?

| Option | Description | Selected |
|--------|-------------|----------|
| 13 affected + 19 look-alikes + s4_o01 (Recommended) | Every E-15 table; affected must now store and match, look-alikes and `s4_o01` must still store and match; same DDL list as the fixtures | ✓ |
| 13 affected shapes only | Pins the fix; look-alike regressions rest on the unit table and the DESC-05 live rows | |
| 13 affected + 5 new spike shapes only | Affected plus the five new shapes; look-alikes covered Docker-free only | |

**User's choice:** 13 affected + 19 look-alikes + s4_o01

### Should the live round-trip pin the R-01 crafted value?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, pin the crafted value (Recommended) | One extra record on the `s4_a01` shape with `"abc" + U+0003 + "xyz"`, typed read-back equal to the record; the measurement behind the Phase 29 data-tail fact | ✓ |
| No, constants only | Plain constant records; the crafted case stays a research finding | |

**User's choice:** Yes, pin the crafted value

### How is the D-04 pin flipped?

| Option | Description | Selected |
|--------|-------------|----------|
| Rename and invert in the fix commit (Recommended) | Method and DisplayName renamed to the new truth, R-01 rows and the `apply()==true` assertion kept, known-defect comment removed, seen red on the pre-fix tree first; nothing deleted or weakened | ✓ |
| Keep the name, invert the assertions only | Phase 23 D-19 style; smaller diff, but the name then contradicts the assertions | |
| Add a new test, leave the old one deleted | Simplest text, but deletes an existing test, which the repo rules forbid | |

**User's choice:** Rename and invert in the fix commit

### Where does the live round-trip live and on which write path?

| Option | Description | Selected |
|--------|-------------|----------|
| New LiveTest class through the sink task (Recommended) | New class beside `NullableTupleMatrixLiveTest`, extends `ClickHouseBase`, tables before `start()`, `put()` through `ClickHouseSinkTask` on the default RowBinary route with V2, typed read-back, zero unpaired WARNs for affected tables; the five new spike shapes join when the spike shows they insert | ✓ |
| Extend `ClickHouseHelperClientForkLiveTest` | Keeps describe pins together, but that class tests `describeTable` and is the manual V1 class | |

**User's choice:** New LiveTest class through the sink task

---

## Closing check

Asked "Which gray areas remain unclear?" with the options "I'm ready for context" / "Explore more gray areas" (candidates offered: how the verdict reaches `Table.updateParent`, the element-key helper's contract, the spike's denied-double-row branch, rollout-fact wording). **User's choice:** I'm ready for context.

## Claude's Discretion

- Builder internals beyond `addRow(String)`/`build()`: unmappable-column `null`, `.inner` early return, raw-type tracking for nested parents, per-pass parse caching.
- The carrier of the classifier's verdict into `Table.updateParent`; the element-key helper's exact signature.
- Which unit-test helpers the builder replaces.
- Fixture file naming, DDL list home, server-version header, Gradle task name, WARN wording.
- Plan split inside the fixed five-step order, evidence layout, worktree rehearsal, DEBUG logging of kept elements.

## Deferred Ideas

- Client V1 coverage of the describe path in CI (future requirement COV-F1).
- An upgrade-hint WARN for a kept element that an earlier release dropped.
- Offering the `Table.java` "declared element" rule upstream.
