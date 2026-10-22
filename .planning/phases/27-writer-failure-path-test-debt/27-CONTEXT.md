# Phase 27: Writer Failure Path & Test Debt - Context

**Gathered:** 2026-10-22
**Status:** Ready for planning

<domain>
## Phase Boundary

On the opt-in JSON-as-string route (`clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true`), a Jackson failure no longer carries a Map key into the task status, the DLQ stack-trace header or the `Error inserting records` throwable. A timeout beneath that failure stays retriable. An unknown Enum label names its column instead of surfacing as a `NullPointerException`, and the `No content in enum` text names the column. The remaining 22-REVIEW and 24-REVIEW advisories and the carried test-debt items close, and no existing assertion is deleted or weakened. This removes the one stated exception in FORK-DELTA Known costs item 6.

Requirements: WRT-16, WRT-17, WRT-18, WRT-19, DEBT-16.

**Locked by ROADMAP, REQUIREMENTS and STATE, not re-opened here:**

*Order inside the phase*
1. The WRT-16 rows, committed green on the unchanged writer; the existing 184 `WriterFailureClassificationUnitTest` cases stay byte-identical.
2. WRT-17, with the Known costs item 6 edit in the same commit, then the deliberate live-test flip.
3. WRT-18, then WRT-19.
4. DEBT-16.

*Rules that hold throughout*
- `ClickHouseWriter.java` is a high-risk file (AGENTS.md). No edit reaches `Processing`, `KeeperStateProvider` or `ClickHouseSinkConfig`.
- Keep `Failed to write column ` and `have no matching DESCRIBE sub-column` byte-identical. Any intended text change is a ledger row and a rollout line, recorded for Phase 29.
- A new writer message must not embed a record value, a Map key, a list element or a value length.
- Flipping any existing assertion needs the user's sign-off and seen-red evidence (Phase 22 L-47, Phase 23 D-19). Sign-off for the one flip in this phase is given below (D-03).
- The anchors invariant passes at every commit, including the comment-only scrub commits.
- Existing tests: add rows, never delete or weaken. A diff check at the end shows no existing assertion removed or loosened.
- Every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`; Docker is colima profile `vz`; a two-run gate uses `--rerun`. Type coverage asserts typed SQL read-back. No Mockito inline-mock of `ClickHouseHelperClient`. Spotless is baseline-equal.
- Nothing is pushed. `origin/custom_build` stays frozen at `69cd680b`.

*Inputs to Phase 28*
- DEBT-16's premise-loop fix in `NullableTupleMatrixLiveTest` lands before Phase 28 uses that file's COLS-13 twins as its oracle. After a name decode, a premise lookup by the old name must fail loudly, not skip silently.
- WRT-18's lookup-miss pins are in place before Phase 28 measures enum labels (NAME-06) in the same `Column` code.

</domain>

<decisions>
## Implementation Decisions

### Jackson exclusion on the IOException pass-through (WRT-17)
- **D-01:** `passesThroughUnchanged` excludes **`com.fasterxml.jackson.core.JacksonException`** link by link along the cause chain, not the narrower `JsonProcessingException`. The base class (Jackson 2.12+, project on 2.21) covers `JsonMappingException`, `InvalidDefinitionException` and `StreamWriteException`, so no Jackson-originated message with a reference chain slips through. A Jackson link never counts as the retry-relevant `IOException`; the loop keeps walking to its cause. — **Reversibility:** reversible — one `instanceof` in one method.
- **D-02:** When the chain is a Jackson exception over a `SocketTimeoutException` or a timeout-text `IOException`, the original exception **passes through unchanged and stays retriable**. The pass-through does not wrap or sanitise the Jackson link in that case: a timeout beneath it is a transport failure, and the roadmap wording is "stays retriable". A Jackson exception alone, with no non-Jackson `IOException` beneath it, falls to the generic path (`DataException` plus `WithheldMessageException`). The WRT-16 rows pin all four shapes (Jackson alone, over a socket timeout, over a timeout-text `IOException`, a plain `IOException`) green on the unchanged writer first. — **Reversibility:** reversible — a wrapping variant can be added later without changing the classifier rows.
- **D-03:** **User sign-off to flip `JsonAsStringKeyResidualLiveTest`** (given 2026-10-22). The key-leak assertions flip to key-absent for the task status, the DLQ stack-trace header and the `Error inserting records` throwable, with the red-on-the-unfixed-writer run recorded as evidence before the writer fix lands. The assertion on the Jackson class stays, so a green run cannot come from a failure that never reached Jackson, and the "no row landed" check stays. This is the one sanctioned flip in the phase. The exception sentence in FORK-DELTA Known costs item 6 and the matching sentence in the `doWriteColValue` FORK-DELTA row are removed in the WRT-17 commit.
- **D-04:** The `ConnectException` fully qualified name in `passesThroughUnchanged` becomes an import in the same commit, since it is the method WRT-17 edits.

### Enum lookup miss (WRT-18, 22-REVIEW IN-01, IN-02)
- **D-05:** The lookup miss is detected **inside `Column.convertEnumValues`**: when `enumValues.get(value)` returns null, it throws, so every Enum8/Enum16 call site (`byteValue()`/`intValue()`) is covered and none can reach a null. The message names the column and says the label was not found; it never contains the label. Phase 28 reads this same `Column` code for enum-label measurement. — **Reversibility:** reversible.
- **D-06:** The thrown type is **`IllegalArgumentException`** (the 22-REVIEW fix). It is not exact-class `RuntimeException`, so it goes through the generic catch and surfaces as ``Failed to write column `<name>`: … ClickHouse type Enum8: java.lang.IllegalArgumentException`` with a `WithheldMessageException` cause. Classification stays NON_RETRIABLE, and the `Failed to write column ` prefix is byte-identical. `UNKNOWN_ENUM_LABEL` expectations in `WriterValueFreeFailureUnitTest` and `WriterFailureClassificationUnitTest` change from the NPE class to `IllegalArgumentException`, each seen red first; those are expectation updates on the class name, not weakened assertions, and they are listed in the diff check.
- **D-07:** The `No content in enum` branch (hand-built `Column` with no `enumValues`) stays an **exact-class `RuntimeException`**, with the text becoming `No content in enum column <name>`. It still passes through unchanged. `NO_ENUM_CONTENT_MESSAGE` in `WriterValueFreeFailureUnitTest` changes in the same commit, seen red first. Both messages stay value-free.

### Failure-metadata derivation (WRT-19, 22-REVIEW IN-03)
- **D-08:** Two small private static helpers in `ClickHouseWriter`, `sourceTypeName(Data)` and `valueClassName(Data)`, replace the two copies of the `sourceType`/`valueClass` derivation (`buildValueConversionFailureMessage` and the `ClassCastException` branch). No holder object.
- **D-09:** The `ClassCastException` message format is built from `WRITE_FAILURE_COLUMN_PREFIX` so the two failure texts cannot drift, with every other character of that text **unchanged byte for byte**. A unit row asserts the exact `ClassCastException` message string before and after the change. The two message builders are not merged. Merging would change wording and need a ledger row and a rollout line.

### Planning identifiers (WRT-19, 22-REVIEW IN-05, 24-REVIEW IN-03)
- **D-10:** The scrub covers **all of `src/main`**: the ~24 planning-ID hits across `ClickHouseWriter`, `Column`, `WithheldMessageException`, `ParentShape`, `DeclaredNullability`, `DescribeTableBuilder`, `ClickHouseHelperClient`, `SinkTaskStatistics` and `HeaderToValue`. Keep each comment's invariant sentence and drop only the tag (`D-xx`, `WRT-xx`, `COLS-xx`, `DESC-xx`, `TYPE-xx`, `HLP-xx`, review IDs). The very long single-line comment above `WRITE_FAILURE_COLUMN_PREFIX` is wrapped in the same pass. Comment-only commits are separate from behaviour commits, and the anchors invariant plus a full compile run at each. A standing guard that fails when a planning ID appears in `src/main` is added (form is Claude's discretion, D-14 below).
- **D-11:** In the test tree, **only `JsonAsStringKeyResidualLiveTest`** is scrubbed: its class Javadoc (`D-03`, `D-14`, the `22-RESEARCH Pitfall 8` reference) and every `@DisplayName` prefix. It goes in the same commit as the D-03 flip, so the file's live run is re-opened once. The other ~70 test files that carry the same tags stay as they are and are recorded as deferred.
- **D-12:** `CountingHelperClient.useClientV2Calls` is **removed** (the field, its increment and its getter), not asserted. Nothing asserts it today, so no assertion is deleted or weakened.

### DEBT-16 remainder
- **D-13:** `schemaWithFixedStringMismatchTest` becomes `RuntimeException e = assertThrows(RuntimeException.class, () -> chst.put(sr));` with the two existing assertions on `Utils.getRootCause(e)` kept, and is seen red against a `put` that does not throw (22-REVIEW IN-04). `NullableTupleMatrixLiveTest`'s premise loops follow 23-REVIEW IN-03 as written: collect the matching `describeRows` row into a variable and `assertNotNull` it with a message before comparing, and wrap the `columns.get(...)` and `getAllColumnsMap().get(...)` lookups in `assertNotNull(..., key)`. Each is seen red against a mutated premise before it is accepted.

### Claude's Discretion
- **D-14:** The form of the standing "no planning IDs in `src/main`" guard (a Docker-free unit test over the source tree, in the style of `TestsWorkflowPinGuardUnitTest`, versus an addition to the invariants script) and the exact ID patterns it matches.
- The exact wording of the new messages, within the value-free rules above.
- Whether the `Column` lookup-miss text shares a constant with the writer's metadata strings.
- The plan split and commit order inside the fixed four-step order, and the evidence file layout (one phase evidence file with the red-first runs and the diff check).
- The shape of the red-first proof for the premise-loop changes in `NullableTupleMatrixLiveTest`.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` § "Phase 27: Writer Failure Path & Test Debt" — goal, success criteria 1–4, the order note and the two Phase 28 inputs.
- `.planning/REQUIREMENTS.md` — WRT-16, WRT-17, WRT-18, WRT-19, DEBT-16.
- `.planning/STATE.md` — the carried rules (`env -u TESTCONTAINERS_HOST_OVERRIDE`, `--rerun`, typed read-back, no inline-mock of the helper client, the tail gate and baseline tag).
- `.planning/PROJECT.md` — milestone framing and Key Decisions.

### Findings this phase closes
- `.planning/milestones/v1.7.0_gg3-hardening-phases/22-writer-record-data-exposure/22-REVIEW.md` § WR-01, IN-01, IN-02, IN-03, IN-04, IN-05 — the issue text and fix sketches.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-REVIEW.md` § IN-03 — planning IDs in `JsonAsStringKeyResidualLiveTest`.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/23-declared-nullability-hardening-review-closure/23-REVIEW.md` § IN-03 — the premise loops in `NullableTupleMatrixLiveTest`.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-COMMAND-EVIDENCE.md` § E-03 — the measured Map-key residual that the flip removes.

### Code and fork records this phase edits
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — `passesThroughUnchanged`, `doWriteColValue`'s generic and `ClassCastException` catches, `buildValueConversionFailureMessage`, `WRITE_FAILURE_COLUMN_PREFIX`.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` — `convertEnumValues`.
- `src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java` — the cause copy; planning IDs in its Javadoc.
- `docs/FORK-DELTA.md` — the `doWriteColValue` row (the Jackson sentence), Known costs item 6 (the exception sentence), the release-history sentence naming `JsonAsStringKeyResidualLiveTest`.
- `.planning/scripts/fork-anchors.txt` — anchors on the files above; moved or kept in the same commit as the code they pin.
- `CHANGELOG.md` — one line per behaviour change in the `1.7.0_gg4` section.
- `AGENTS.md` — high-risk files, test rules, comment rules ("convey invariants, do not summarise logic").

### Tests this phase touches
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterFailureClassificationUnitTest.java` (WRT-16 rows), `WriterValueFreeFailureUnitTest` (`UNKNOWN_ENUM_LABEL`, `NO_ENUM_CONTENT_MESSAGE`), `JsonAsStringKeyResidualLiveTest`, `ClickHouseSinkTaskWithSchemaTest` (`schemaWithFixedStringMismatchTest`), `CountingHelperClient`, `NullableTupleMatrixLiveTest`.

### Prior decisions carried
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-CONTEXT.md` and `.planning/phases/26-size-named-tuple-elements-in-discovery/26-CONTEXT.md` — red-first discipline, Docker-free test conventions, anchors-per-commit, metadata-only logging.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WithheldMessageException.of(e)`: already copies the chain's frames and withholds messages; the Jackson-alone case reuses it unchanged.
- `WRITE_FAILURE_COLUMN_PREFIX` and `buildValueConversionFailureMessage`: the shared grep prefix that the `ClassCastException` text moves onto.
- `JsonAsStringKeyResidualLiveTest`: already asserts the Jackson class and that no row landed, so the flip changes only the key assertions.
- `WriterFailureClassificationUnitTest` (184 cases): the table the four WRT-16 rows join.
- `LogCapture`, `ClickHouseBase`, `TestsWorkflowPinGuardUnitTest`: precedents for log assertions, live tests and a Docker-free source-tree guard.

### Established Patterns
- Value-free failure text: column metadata, class names and Kafka types only; never a value, key, element or length.
- Red-first: a changed pin is seen red on the unchanged tree before it goes green.
- Anchor rows move in the same commit as the code they pin; document edits that cite a measurement land after the measurement.
- Docker-free tests are untagged and run in `testUnit`; live tests extend `ClickHouseBase`.

### Integration Points
- `passesThroughUnchanged` feeds `doWriteColValue`'s generic catch, which feeds `Utils.handleException` retry classification and the DLQ headers.
- `Column.convertEnumValues` is called from the Enum8/Enum16 cases in `doWritePrimitive`; Phase 28 measures enum labels in the same code.
- `NullableTupleMatrixLiveTest`'s COLS-13 twins become Phase 28's oracle.

</code_context>

<specifics>
## Specific Ideas

- The user accepted the recommended option at every fork: the base class `JacksonException`, a plain pass-through for the timeout case, `IllegalArgumentException` thrown from `Column`, prefix-constant-only dedup, the whole of `src/main` scrubbed but one test class, and the dead counter removed.
- The scrub is broader in `src/main` than 22-REVIEW IN-05 asked, to make roadmap criterion 4 true as written, and narrower in tests than 24-REVIEW IN-03 hinted.

</specifics>

<deferred>
## Deferred Ideas

- Scrubbing planning IDs (`D-xx`, `COLS-xx`, …) from the other ~70 test files and their `@DisplayName` strings: not requested for this phase, and the names may be cited by evidence files. Candidate for a later hygiene phase.
- Merging the two failure-message builders into one format: would change wording (ledger row, rollout line); not needed to close IN-03.
- Wrapping a retriable Jackson-over-timeout exception with a sanitised cause: not needed, since no Map key was measured on that path.

</deferred>

---

*Phase: 27-writer-failure-path-test-debt*
*Context gathered: 2026-10-22*
