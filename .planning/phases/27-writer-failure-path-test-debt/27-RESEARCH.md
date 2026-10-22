# Phase 27: Writer Failure Path & Test Debt - Research

**Researched:** 2026-10-22
**Domain:** Exception classification and value-free failure text in `ClickHouseWriter` (Kafka Connect sink, Java 17, JUnit 5, Gradle), plus test-debt closure
**Confidence:** HIGH (every finding below was read from the repo or measured this session; the few `[ASSUMED]` items are listed in the Assumptions Log)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Locked by ROADMAP, REQUIREMENTS and STATE, not re-opened here**

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

**Implementation Decisions**

*Jackson exclusion on the IOException pass-through (WRT-17)*
- **D-01:** `passesThroughUnchanged` excludes **`com.fasterxml.jackson.core.JacksonException`** link by link along the cause chain, not the narrower `JsonProcessingException`. The base class (Jackson 2.12+, project on 2.21) covers `JsonMappingException`, `InvalidDefinitionException` and `StreamWriteException`, so no Jackson-originated message with a reference chain slips through. A Jackson link never counts as the retry-relevant `IOException`; the loop keeps walking to its cause. — **Reversibility:** reversible — one `instanceof` in one method.
- **D-02:** When the chain is a Jackson exception over a `SocketTimeoutException` or a timeout-text `IOException`, the original exception **passes through unchanged and stays retriable**. The pass-through does not wrap or sanitise the Jackson link in that case: a timeout beneath it is a transport failure, and the roadmap wording is "stays retriable". A Jackson exception alone, with no non-Jackson `IOException` beneath it, falls to the generic path (`DataException` plus `WithheldMessageException`). The WRT-16 rows pin all four shapes (Jackson alone, over a socket timeout, over a timeout-text `IOException`, a plain `IOException`) green on the unchanged writer first. — **Reversibility:** reversible — a wrapping variant can be added later without changing the classifier rows.
- **D-03:** **User sign-off to flip `JsonAsStringKeyResidualLiveTest`** (given 2026-10-22). The key-leak assertions flip to key-absent for the task status, the DLQ stack-trace header and the `Error inserting records` throwable, with the red-on-the-unfixed-writer run recorded as evidence before the writer fix lands. The assertion on the Jackson class stays, so a green run cannot come from a failure that never reached Jackson, and the "no row landed" check stays. This is the one sanctioned flip in the phase. The exception sentence in FORK-DELTA Known costs item 6 and the matching sentence in the `doWriteColValue` FORK-DELTA row are removed in the WRT-17 commit.
- **D-04:** The `ConnectException` fully qualified name in `passesThroughUnchanged` becomes an import in the same commit, since it is the method WRT-17 edits.

*Enum lookup miss (WRT-18, 22-REVIEW IN-01, IN-02)*
- **D-05:** The lookup miss is detected **inside `Column.convertEnumValues`**: when `enumValues.get(value)` returns null, it throws, so every Enum8/Enum16 call site (`byteValue()`/`intValue()`) is covered and none can reach a null. The message names the column and says the label was not found; it never contains the label. Phase 28 reads this same `Column` code for enum-label measurement. — **Reversibility:** reversible.
- **D-06:** The thrown type is **`IllegalArgumentException`** (the 22-REVIEW fix). It is not exact-class `RuntimeException`, so it goes through the generic catch and surfaces as ``Failed to write column `<name>`: … ClickHouse type Enum8: java.lang.IllegalArgumentException`` with a `WithheldMessageException` cause. Classification stays NON_RETRIABLE, and the `Failed to write column ` prefix is byte-identical. `UNKNOWN_ENUM_LABEL` expectations in `WriterValueFreeFailureUnitTest` and `WriterFailureClassificationUnitTest` change from the NPE class to `IllegalArgumentException`, each seen red first; those are expectation updates on the class name, not weakened assertions, and they are listed in the diff check.
- **D-07:** The `No content in enum` branch (hand-built `Column` with no `enumValues`) stays an **exact-class `RuntimeException`**, with the text becoming `No content in enum column <name>`. It still passes through unchanged. `NO_ENUM_CONTENT_MESSAGE` in `WriterValueFreeFailureUnitTest` changes in the same commit, seen red first. Both messages stay value-free.

*Failure-metadata derivation (WRT-19, 22-REVIEW IN-03)*
- **D-08:** Two small private static helpers in `ClickHouseWriter`, `sourceTypeName(Data)` and `valueClassName(Data)`, replace the two copies of the `sourceType`/`valueClass` derivation (`buildValueConversionFailureMessage` and the `ClassCastException` branch). No holder object.
- **D-09:** The `ClassCastException` message format is built from `WRITE_FAILURE_COLUMN_PREFIX` so the two failure texts cannot drift, with every other character of that text **unchanged byte for byte**. A unit row asserts the exact `ClassCastException` message string before and after the change. The two message builders are not merged. Merging would change wording and need a ledger row and a rollout line.

*Planning identifiers (WRT-19, 22-REVIEW IN-05, 24-REVIEW IN-03)*
- **D-10:** The scrub covers **all of `src/main`**: the ~24 planning-ID hits across `ClickHouseWriter`, `Column`, `WithheldMessageException`, `ParentShape`, `DeclaredNullability`, `DescribeTableBuilder`, `ClickHouseHelperClient`, `SinkTaskStatistics` and `HeaderToValue`. Keep each comment's invariant sentence and drop only the tag (`D-xx`, `WRT-xx`, `COLS-xx`, `DESC-xx`, `TYPE-xx`, `HLP-xx`, review IDs). The very long single-line comment above `WRITE_FAILURE_COLUMN_PREFIX` is wrapped in the same pass. Comment-only commits are separate from behaviour commits, and the anchors invariant plus a full compile run at each. A standing guard that fails when a planning ID appears in `src/main` is added (form is Claude's discretion, D-14 below).
- **D-11:** In the test tree, **only `JsonAsStringKeyResidualLiveTest`** is scrubbed: its class Javadoc (`D-03`, `D-14`, the `22-RESEARCH Pitfall 8` reference) and every `@DisplayName` prefix. It goes in the same commit as the D-03 flip, so the file's live run is re-opened once. The other ~70 test files that carry the same tags stay as they are and are recorded as deferred.
- **D-12:** `CountingHelperClient.useClientV2Calls` is **removed** (the field, its increment and its getter), not asserted. Nothing asserts it today, so no assertion is deleted or weakened.

*DEBT-16 remainder*
- **D-13:** `schemaWithFixedStringMismatchTest` becomes `RuntimeException e = assertThrows(RuntimeException.class, () -> chst.put(sr));` with the two existing assertions on `Utils.getRootCause(e)` kept, and is seen red against a `put` that does not throw (22-REVIEW IN-04). `NullableTupleMatrixLiveTest`'s premise loops follow 23-REVIEW IN-03 as written: collect the matching `describeRows` row into a variable and `assertNotNull` it with a message before comparing, and wrap the `columns.get(...)` and `getAllColumnsMap().get(...)` lookups in `assertNotNull(..., key)`. Each is seen red against a mutated premise before it is accepted.

### Claude's Discretion
- **D-14:** The form of the standing "no planning IDs in `src/main`" guard (a Docker-free unit test over the source tree, in the style of `TestsWorkflowPinGuardUnitTest`, versus an addition to the invariants script) and the exact ID patterns it matches.
- The exact wording of the new messages, within the value-free rules above.
- Whether the `Column` lookup-miss text shares a constant with the writer's metadata strings.
- The plan split and commit order inside the fixed four-step order, and the evidence file layout (one phase evidence file with the red-first runs and the diff check).
- The shape of the red-first proof for the premise-loop changes in `NullableTupleMatrixLiveTest`.

### Deferred Ideas (OUT OF SCOPE)
- Scrubbing planning IDs (`D-xx`, `COLS-xx`, …) from the other ~70 test files and their `@DisplayName` strings: not requested for this phase, and the names may be cited by evidence files. Candidate for a later hygiene phase.
- Merging the two failure-message builders into one format: would change wording (ledger row, rollout line); not needed to close IN-03.
- Wrapping a retriable Jackson-over-timeout exception with a sanitised cause: not needed, since no Map key was measured on that path.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| WRT-16 | `WriterFailureClassificationUnitTest` gains rows for a `JsonProcessingException` alone, over a `SocketTimeoutException`, over a timeout-text `IOException`, and a plain `IOException`, green on the unchanged writer first; the existing 184 cases are byte-identical. | Baseline measured: 184 testcases, 0 failures (`build/test-results/testUnit/TEST-...WriterFailureClassificationUnitTest.xml`, this session). Jackson constructors usable from tests are listed under Code Examples. `IO_PLAIN` already pins a plain `IOException` (see Finding F-5). Insert new enum constants mid-list so the diff is additions only (Pitfall 3). |
| WRT-17 | `JacksonException` is excluded per link of the cause chain from the `IOException` pass-through, so a sentinel Map key reaches none of the task status, the DLQ stack-trace header and the `Error inserting records` throwable, while a timeout beneath a Jackson exception stays retriable; `JsonAsStringKeyResidualLiveTest` is flipped deliberately; Known costs item 6 exception sentence removed. | One-line change in `passesThroughUnchanged` (Code Examples). The live test's `assertJacksonRouteReached` MUST be adapted or it goes red for the wrong reason (Finding F-1). A Docker-free reproduction with the real Jackson failure is feasible and recommended (Finding F-2). Doc rows to edit are listed in the FORK-DELTA table below. |
| WRT-18 | Unknown Enum label reported with the column name and a lookup-miss cause instead of NPE (IN-01); `No content in enum` names the column (IN-02); both value-free; `Failed to write column ` prefix byte-identical. | Fix is in `Column.convertEnumValues` only. No existing test asserts the NPE class, so the pins are new rows (Finding F-3). The IAE message is withheld end to end, so "names the column" must be proven at `Column` level (Finding F-4). Anchor row for the old `No content in enum` line must move in the same commit. |
| WRT-19 | Metadata-derivation duplication removed (IN-03); planning-only identifiers leave `src/main` comments (IN-05 src half); `JsonAsStringKeyResidualLiveTest` Javadoc and display names carry no planning IDs (24-REVIEW IN-03). | Two helper methods; exact `ClassCastException` text pinned green first (no exact pin exists today, Finding F-6). Full hit list of planning IDs, including two hits an ID regex misses (Finding F-7). Guard design under Architecture Patterns. |
| DEBT-16 | `schemaWithFixedStringMismatchTest` fails when `put` does not throw (IN-04); `CountingHelperClient.useClientV2Calls` removed or asserted (IN-05 test half); `NullableTupleMatrixLiveTest` premise loops fail with a message (23-REVIEW IN-03); no existing assertion deleted or weakened. | Three exact edit sites in `NullableTupleMatrixLiveTest` and one in `ClickHouseSinkTaskWithSchemaTest` (Findings F-8, F-9). Removing the counter also changes a FORK-DELTA row (Finding F-10). |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` (which imports `AGENTS.md`) and `./.claude/CLAUDE.md`. The planner must treat these like locked decisions.

- Java 17, Gradle Kotlin DSL, Lombok, SLF4J (TRACE per-record, DEBUG batch, INFO lifecycle), JUnit 5 + Mockito. [VERIFIED: AGENTS.md, Coding Conventions]
- `ClickHouseWriter.java` is high-risk; type-mapping and serialization hot-path changes need extensive test coverage. [VERIFIED: AGENTS.md, Key Files]
- Do not log secrets or record data in plaintext. Error messages must be clear, actionable, and carry context. [VERIFIED: AGENTS.md, Security / Observability]
- Comments: only important context or invariants, never a summary of the logic. This is the rule behind the planning-ID scrub. [VERIFIED: AGENTS.md, Configuration & Settings]
- Static string constants are extracted into top-level static strings. [VERIFIED: AGENTS.md, Code Style]
- Existing tests: only add new tests, never delete or weaken. Test names must be descriptive. Read `ClickHouseTestHelpers.java` before writing tests. DRY. [VERIFIED: AGENTS.md, Testing Discipline]
- Feature tests that need ClickHouse extend `ClickHouseBase`; Docker-free tests extend nothing and carry no tag. Always read data back and `assertEquals`. [VERIFIED: AGENTS.md, Writing Tests]
- Do not commit or change PR text without being asked. In this milestone the user has authorised phase commits; nothing is pushed. [VERIFIED: AGENTS.md Review rules; 27-CONTEXT.md]
- Do not upgrade dependencies without asking. This phase adds no dependency. [VERIFIED: AGENTS.md, Security]
- Tests pin current fork behaviour; code changes are limited to genuine bug fixes; changes stay additive for rebasability. [VERIFIED: .claude/CLAUDE.md, Constraints]
- Formatting: Spotless applies `importOrder()` and `removeUnusedImports()` only (`googleJavaFormat()` is commented out). [VERIFIED: build.gradle.kts:552-558, quoted: `importOrder()`, `removeUnusedImports()`, `// googleJavaFormat()`]
- GSD edit-gate text in CLAUDE.md is ignored for this task (research writes only RESEARCH.md).

## Summary

The phase is five small, well-bounded edits on one failure path, plus test debt. Everything the planner needs to be careful about sits in the interactions: the Jackson fix changes which exception object the live test sees (its Jackson-class assertion would fail for the wrong reason), the enum fix throws a message that the writer then deliberately withholds, and the comment scrub touches lines whose exact text is counted by the fork-anchors gate.

The core mechanism, read this session: `passesThroughUnchanged` rethrows any exception whose cause chain holds an `IOException`. Jackson's checked exceptions are `IOException` subclasses (`JacksonException extends java.io.IOException`, confirmed with `javap` on `jackson-core-2.22.4.jar`), so on the opt-in JSON-as-string route a Jackson failure is rethrown untouched with its `(through reference chain: ...)` text. The fix is a one-condition change inside the existing per-link loop. The WRT-16 rows must be written so that they pass on the unchanged writer and keep passing afterwards: all four shapes have the same classification outcome before and after (Jackson alone is non-retriable both times; Jackson over a timeout is retriable both times), so they guard against the naive "exclude the whole chain" implementation rather than detect the key fix itself. The key itself is detected by the live flip and, recommended here, by a Docker-free unit row that drives the real Jackson failure through `doWriteColValue`.

**Primary recommendation:** Land the work as five plans in the CONTEXT order. Add the WRT-16 rows by inserting enum constants mid-list (additions only), pin the exact `ClassCastException` text green before refactoring, write every WRT-17 and WRT-18 pin as a new red-first row (there is no existing NPE or Jackson-class expectation to flip except the live test's key assertions), move the two anchor rows and four FORK-DELTA rows in the same commits as the code, and add the planning-ID guard as a Docker-free unit test with a wider pattern than the `D-xx`-style IDs (two real hits are not ID-shaped).

## Findings that refine CONTEXT.md (read before planning)

These are facts the CONTEXT did not have. None reopens a locked decision; they change how the decisions are executed.

| # | Finding | Evidence | Consequence for the plan |
|---|---------|----------|--------------------------|
| F-1 | After WRT-17, a Jackson-alone failure reaches the live test as a `WithheldMessageException` chain. `assertJacksonRouteReached` compares `t.getClass().getName()` with `JACKSON_FAILURE_CLASS`, so it would fail even though the route was reached. The real class survives only in `WithheldMessageException.getOriginalClassName()` and in `toString()`. | `JsonAsStringKeyResidualLiveTest.java:187-195` (`.anyMatch(t -> JACKSON_FAILURE_CLASS.equals(t.getClass().getName()))`); `WithheldMessageException.java:63` (`new WithheldMessageException(node.getClass().getName(), copy)`) and `:71` (`getOriginalClassName()`) | The helper must also accept `t instanceof WithheldMessageException w && JACKSON_FAILURE_CLASS.equals(w.getOriginalClassName())`. This keeps D-03's "assertion on the Jackson class stays" true and is not a weakening: the class identity is still required. The adapted helper must be seen green on the unfixed writer too (direct class match), so the red run is caused only by the key assertions. |
| F-2 | The real Jackson failure can be reproduced Docker-free. `doWriteColValue` for a `Type.JSON` column with `input_format_binary_read_json_as_string=1` calls `OBJECT_MAPPER.writeValueAsBytes(value)`; the existing unit test `unsupportedJsonFieldTypePassesThrough` already builds such a config without Docker. | `ClickHouseWriter.java:871-874`; `WriterValueFreeFailureUnitTest.java:412-422` | Recommended: add a Docker-free row to `WriterValueFreeFailureUnitTest` (Struct holding a Map with a sentinel key mapped to `new Object()`), red on the unchanged writer (sentinel present), green after. The live flip remains the end-to-end proof for the three destinations. Feasibility of the exact value construction is `[ASSUMED]` (A1); confirm with a scratch run in the first WRT-17 task. |
| F-3 | No existing test asserts the NPE class for an unknown Enum label, so D-06's "expectations change from NPE to IllegalArgumentException" has nothing to change. `WriterFailureClassificationUnitTest.UNKNOWN_ENUM_LABEL` pins only the outcome (`RetryClass.NON_RETRIABLE`); `WriterValueFreeFailureUnitTest` has no unknown-label row; `NO_ENUM_CONTENT_MESSAGE` is the only enum constant there. | `grep -rn -i "UNKNOWN_ENUM_LABEL\|No content in enum" src/test` returned only `WriterFailureClassificationUnitTest.java:91`, `WriterValueFreeFailureUnitTest.java:74` and `:517`; no `NullPointerException` assertion in any enum test | WRT-18's unknown-label pins are **new rows**, red first on the unchanged writer (class `NullPointerException`), green after. The only existing assertion that changes text is `NO_ENUM_CONTENT_MESSAGE` (D-07). The diff-check list for "changed expectations" is therefore shorter than CONTEXT states. |
| F-4 | The text of the lookup-miss `IllegalArgumentException` never reaches a task status, DLQ header or log: the generic catch wraps every non-pass-through exception with `WithheldMessageException.of(e)`, which keeps the class name and frames and withholds the message. What an operator sees is `Failed to write column `<name>`: … ClickHouse type Enum8: java.lang.IllegalArgumentException` plus `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)`. The column was already named by the `DataException` prefix even with the NPE. | `ClickHouseWriter.java:909` (`throw new DataException(buildValueConversionFailureMessage(col, columnType, value, e), WithheldMessageException.of(e));`); `WithheldMessageException.java:35` (`WITHHELD_NOTE = "(message withheld: may contain record data)"`) | The observable end-to-end change is NPE class to IAE class. Success criterion 3 ("a message that names the column and a lookup-miss cause") can only be proven on `Column.convertEnumValues` directly (new `Column` unit row: `assertThrows(IllegalArgumentException.class, ...)`, message contains the column name, does not contain the label). The writer-level row asserts the exact `DataException` text with `IllegalArgumentException.class` and `getOriginalClassName()`. Recorded as Open Question 1 in case the user wants an operator-visible miss text. |
| F-5 | A plain `IOException` row already exists: `IO_PLAIN(RetryClass.NON_RETRIABLE, () -> uuidStream(new IOException("stream closed by test")))`. The fourth WRT-16 shape is therefore a control that duplicates an existing kind. | `WriterFailureClassificationUnitTest.java:116` | Add it anyway as a named control inside the Jackson group (8 cheap cases) so the roadmap criterion is literally true, or add a more informative fourth shape (a plain `IOException` whose cause is a Jackson exception: still passes through because the non-Jackson link counts). Planner's choice; both are green on the unchanged writer. |
| F-6 | No test pins the exact `ClassCastException` message. `ClickHouseWriterColumnContextTest` only uses `contains(...)`. D-09's "unit row asserting the exact message before and after" is a new row that must be committed green before the refactor. | `ClickHouseWriterColumnContextTest.java:49-57` (`message.contains("effective_at")`, `contains("UINT64")`, `contains("STRING")`); only other occurrence of the text is the source line `ClickHouseWriter.java:899` and the published `docs/ROLLOUT-v1.7.0_gg3.md:55` | Exact text to pin, derived from the format at `ClickHouseWriter.java:898-900`: ``Failed to write column `effective_at`: cannot convert Kafka value of type STRING (value class: java.lang.String) to ClickHouse type UINT64`` for `Column.extractColumn("effective_at", "UInt64", ...)` with a `Data(Schema.STRING_SCHEMA, ...)` value. Confirm by running before relying on it (A2). |
| F-7 | Two planning references in `src/main` are not ID-shaped: `Phase 28's` (`DeclaredNullability.java:221`) and the bare word `RESEARCH` (`SinkTaskStatistics.java:170`, `RESEARCH` then `Pitfall 4)` on the next line). An ID-only regex misses them. | Output of `grep -rnE "Phase [0-9]+\|RESEARCH\|Pitfall"` over `src/main` this session | The scrub list and the guard pattern must include `Phase \d+`, `Pitfall \d+` and `RESEARCH`. Full hit list below. |
| F-8 | Project Jackson is **2.22.4**, not 2.21. | `gradle/libs.versions.toml:5` (`jackson = "2.22.4"`) | D-01's rationale ("Jackson 2.12+") holds; correct the version in the evidence text. |
| F-9 | `fork-anchors.txt` currently yields `anchor_rows=79` at HEAD, and the tail gate passes. Earlier Phase 26 plans quote 77. | Gate run this session: `invariants: provenance_paths=3 anchor_commits=72 anchor_rows=79 marker_commits=84 anchor_exempt=0 result=PASS` | Use the number the gate prints at the start of the phase as the baseline; add rows only with their code commit and re-state the expected count in each plan's verify step. |
| F-10 | `docs/FORK-DELTA.md` row 82 describes `CountingHelperClient` as counting `isUseClientV2`, `pinReplica` and `unpinReplica`. Removing the counter (D-12) makes that row wrong. | `docs/FORK-DELTA.md:82` (``a real `ClickHouseHelperClient` subclass that counts `isUseClientV2`, `pinReplica` and `unpinReplica` and opens no connection``) and the class Javadoc `CountingHelperClient.java:7-8` | Edit both in the D-12 commit. |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Decide which write failures keep their original exception (retry classification) | Connector sink task: `ClickHouseWriter.passesThroughUnchanged` | `Utils.handleException` (consumes the decision) | The writer owns the catch; `Utils` only reads the resulting cause chain. |
| Keep record text out of status, DLQ and logs | Connector sink task: `doWriteColValue` generic catch + `WithheldMessageException` | `Processing` (wraps in `RuntimeException(queryId)`; untouched) | The sanitised copy is built at the catch; nothing downstream may re-read the original. |
| Name the failing column and classify a missing enum label | Connector type mapping: `Column.convertEnumValues` | `ClickHouseWriter.doWritePrimitive` (call sites) | Detecting the miss at the lookup covers both `Enum8` and `Enum16` call sites. |
| Surface the DLQ headers and task status | Kafka Connect runtime (external) | Test helpers `ThrowableTexts`, `InMemoryDLQ.connectHeaders` approximate it | The worker prints `printStackTrace` of the chain; tests render the same. |
| Gate fork anchors and docs | Repo scripts and docs (`.planning/scripts`, `docs/FORK-DELTA.md`) | Phase evidence files | Anchors move in the same commit as the pinned line. |

## Standard Stack

### Core (no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jackson core + databind | 2.22.4 [VERIFIED: gradle/libs.versions.toml:5, quoted `jackson = "2.22.4"`] | Source of the exceptions this phase classifies (`JacksonException`, `JsonMappingException`, `InvalidDefinitionException`) | Already `implementation` dependencies of the connector (`build.gradle.kts:102-103`), so the test classpath sees them. |
| JUnit Jupiter | 5.11.3 (per `.claude/CLAUDE.md`) | `@ParameterizedTest`, `assertThrows`, `assertInstanceOf` | Existing convention. |
| Gradle wrapper | 9.8.1 [VERIFIED: gradle log "docs.gradle.org/9.8.1"] | Builds and runs `testUnit` | Existing. |

### Supporting

| Component | Purpose | When to Use |
|-----------|---------|-------------|
| `LogCapture.attach(Class)` | Capture log events of one logger as texts and throwables | Every new row that asserts "no sentinel in any log text". It attaches **one** class; pass `ClickHouseWriter.class` as the existing tests do. |
| `ThrowableTexts` (`causeChain`, `allTexts`, `printedStackTrace`) | Render the operator-visible texts of a chain | Sentinel checks, task-status approximation. |
| `ScriptedWriteFailureWriter.FailingOutputStream(IOException, bytesBeforeFailure)` | Make the real `doWriteColValue` fail with a chosen `IOException` | WRT-16 rows and any Docker-free Jackson-over-timeout pass-through row. |
| `CountingHelperClient` | Real `ClickHouseHelperClient` subclass, no Mockito | WRT-16 harness (already used). |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Exclude `JacksonException` (D-01, locked) | `JsonProcessingException` (22-REVIEW's sketch) | Narrower: misses `StreamWriteException` subclasses only if they are not `JsonProcessingException`; `StreamWriteException extends JsonProcessingException` [VERIFIED: javap], so both work today. The base class is the locked, more future-proof choice. |
| Planning-ID guard as a unit test (recommended for D-14) | Add an invariant to `verify-merge-invariants.sh` | The script and its self-test are protected in this milestone (Phase 26 plan: "MUST NOT ... edit verify-merge-invariants.sh or its self-test"), and the guard is unrelated to merge provenance. Unit test wins. |

**Installation:** none. **Package Legitimacy:** no new external packages are introduced; the Package Legitimacy Gate does not apply.

## Package Legitimacy Audit

No external package is added or changed by this phase (Jackson is an existing pinned dependency, version not touched).

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none) | - | - | - | - | - | - |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram (the failure path this phase edits)

```
Kafka SinkRecord (JSON-as-string route: clickhouseSettings has input_format_binary_read_json_as_string=1|true)
   |
   v
ClickHouseSinkTask.put -> ProxySinkTask.put -> Processing.doLogic
   |                                              (catch Exception e -> throw new RuntimeException(queryId.toString(), e))   [untouched]
   v
ClickHouseWriter.doInsertRawBinary -> doInsertRawBinaryV1/V2 -> doWriteColValue(col, stream, value, defaultsSupport)
   |
   |  case JSON, flag on, STRUCT:  OBJECT_MAPPER.writeValueAsBytes(value)  ---- throws InvalidDefinitionException
   |                                                                              (JacksonException -> java.io.IOException)
   |  case Enum8/Enum16:           col.convertEnumValues((String) value)   ---- WRT-18 detects the miss HERE (Column)
   v
catch (ClassCastException)  -> DataException "Failed to write column `n`: cannot convert Kafka value ..."   (WRT-19: helpers + prefix constant)
catch (Exception e)
   |-- passesThroughUnchanged(e)? ---- WRT-17 changes this predicate
   |       true : throw e                                    (stream/timeout IOException, ConnectException, exact RuntimeException)
   |       false: DataException(buildValueConversionFailureMessage(...), WithheldMessageException.of(e))   (value-free)
   v
doInsertRawBinary catch -> LOGGER.error("Error inserting records", e); throw e
   v
Utils.handleException: root cause SocketTimeoutException / UnknownHost / timeout-text IOException => RetriableException
                       else errors.tolerance=none => RuntimeException("Number of records: n", e); all => DLQ
   v
Destinations an operator reads: task status (printStackTrace of chain), DLQ exception.stacktrace header,
                                "Error inserting records" throwable, "Error writing value of" line
```

### Recommended edit set and plan split (discretion item; one recommendation)

```
27-01  WRT-16   Docker-free rows only. Commit green on the unchanged writer. Evidence: 184 -> 184 + N names, old names unchanged.
27-02  WRT-17   (a) scratch red run of flipped live test + new Docker-free row on UNCHANGED writer (record output, not committed)
                (b) commit: passesThroughUnchanged + ConnectException import + anchor row + FORK-DELTA rows (row 42, item 6, line 217 sentence) + CHANGELOG line + new Docker-free rows
                (c) commit: JsonAsStringKeyResidualLiveTest flip + helper adaptation (F-1) + Javadoc/DisplayName scrub (D-11); live run green
27-03  WRT-18   new red rows -> Column.convertEnumValues change -> anchor row move + FORK-DELTA row 53 + CHANGELOG + ledger
27-04  WRT-19   (a) exact ClassCastException pin green first (b) helpers + prefix constant (behaviour commit)
                (c) comment-only scrub commits (anchors + compile at each) (d) planning-ID guard
27-05  DEBT-16  IN-04 test, CountingHelperClient removal (+ FORK-DELTA row 82), NullableTupleMatrixLiveTest premises; then evidence, diff check, ledger
```

### Pattern 1: per-link Jackson exclusion (WRT-17)

**What:** The existing loop already walks the chain one link at a time with an identity-guarded set. Keep walking past a Jackson link; only a non-Jackson `IOException` makes the chain pass through.
**When to use:** only inside `passesThroughUnchanged`.

```java
// Source: derived from ClickHouseWriter.java:643-654 (read this session) and D-01/D-04
private static boolean passesThroughUnchanged(Exception e) {
    if (e instanceof ConnectException || e.getClass() == RuntimeException.class) {
        return true;
    }
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
        if (t instanceof IOException && !(t instanceof JacksonException)) {
            return true;
        }
    }
    return false;
}
```

Imports (Spotless `importOrder`): `com.fasterxml.jackson.core.JacksonException` joins the `com.*` block; `org.apache.kafka.connect.errors.ConnectException` goes between `org.apache.kafka.connect.data.Struct` and `org.apache.kafka.connect.errors.DataException` (current block: lines 28-32). [VERIFIED: ClickHouseWriter.java import listing, `import org.apache.kafka.connect.errors.DataException;` at line 31]

The Javadoc of this method also needs rewording: "any chain holding an IOException" becomes "any chain holding an IOException that is not a Jackson exception", and its `(D-01, D-03)` tags are dropped in the WRT-19 scrub (or here, if the planner prefers to touch the comment once).

### Pattern 2: lookup-miss at the lookup (WRT-18)

```java
// Source: derived from Column.java:526-532 (read this session) and D-05/D-06/D-07
public Integer convertEnumValues(String value) {
    if (this.enumValues != null) {
        Integer code = enumValues.get(value);
        if (code == null) {
            throw new IllegalArgumentException(String.format(ENUM_LABEL_NOT_FOUND_FORMAT, name));
        }
        return code;
    }
    // The label is record data and this exact RuntimeException reaches Connect status unchanged, so the text carries the column only.
    throw new RuntimeException(String.format(NO_ENUM_CONTENT_FORMAT, name));
}
```

Constants (AGENTS.md: extract strings), wording is discretion, for example `"label not found in enum of column %s"` and `"No content in enum column %s"`. Keep `No content in enum column ` free of the string `No content in enum %s`, which is a negative anchor (count 0) [VERIFIED: fork-anchors.txt:83]. `name` is a Lombok-generated field of `Column`, never a record value.

### Pattern 3: shared metadata helpers (WRT-19, D-08/D-09)

```java
// Source: derived from ClickHouseWriter.java:657-658 and :896-900 (read this session)
private static String sourceTypeName(Data value) {
    return value == null ? "null" : String.valueOf(value.getFieldType());
}

private static String valueClassName(Data value) {
    return (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
}
```

The `ClassCastException` branch then becomes `String.format(WRITE_FAILURE_COLUMN_PREFIX + "%s`: cannot convert Kafka value of type %s (value class: %s) to ClickHouse type %s", ...)` held in a new constant. Careful: `WRITE_FAILURE_COLUMN_PREFIX` already ends with the backtick that opens the column name (``"Failed to write column `"``), and the existing format string reads ``"Failed to write column `%s`: cannot convert ..."``. The new constant must produce the same bytes (concatenation `PREFIX + "%s`: cannot ..."`), as `WRITE_VALUE_CONVERSION_FAILURE_FORMAT` already does at `ClickHouseWriter.java:571-572`. The exact-text pin from F-6 is what proves it.

### Pattern 4: standing planning-ID guard (D-14 recommendation)

A Docker-free class in `src/test/java/com/clickhouse/kafka/connect/ci/` (same package and style as `TestsWorkflowPinGuardUnitTest`: extends nothing, relative `Path.of`, explicit UTF-8, per-line assertion messages naming file and line, a non-vacuity floor on the number of files scanned). Walk `src/main`, read each `.java` file, fail on any line matching:

```
\b(?:D|L|R|F|A|IN|WR|CR)-\d{1,3}\b
\b(?:WRT|COLS|DESC|TYPE|HLP|DEBT|NAME|JSONP|CMP|CFG|ORCH|MAP|BLD|HARN|CI|DEPS|OPS|ROLL|SEC)-\d{1,3}\b
\bPhase \d+\b   \bPitfall \d+\b   \bRESEARCH\b   \.planning/   \b\d{2}-(?:RESEARCH|CONTEXT|REVIEW|PLAN|UAT|SECURITY)\b
```

Add a pattern self-test inside the class (positive samples: ``(D-01)``, ``WRT-13``, ``Phase 28's``, ``22-RESEARCH Pitfall 8``; negative samples: ``UTF-8``, ``UTF-16``, ``upstream #820``, ``SHA-256``) so a regex typo cannot make the guard vacuous. False positives measured this session: a generic `\b[A-Z]{1,6}-[0-9]{1,3}\b` scan of `src/main` found only `UTF-8` (2) and `UTF-16` (1) beyond real planning IDs [VERIFIED: grep output this session], so the explicit prefix list above does not match them.

### Anti-Patterns to Avoid

- **Excluding the whole chain when its head is Jackson.** It would wrap a genuine `SocketTimeoutException` beneath Jackson into a non-retriable `DataException` (root cause becomes `WithheldMessageException`). The WRT-16 Jackson-over-timeout rows exist to catch this. [CITED: .planning/research/PITFALLS.md Pitfall 13]
- **Reflowing `WithheldMessageException` Javadoc lines 23-24.** The anchor `(message withheld: may contain record data)` has count 1 today because the Javadoc example is split across two lines (`... (message withheld: may contain` / `record data)}`). Re-wrapping those lines into one would make the count 2 and fail the gate. [VERIFIED: WithheldMessageException.java:23-24 and fork-anchors.txt:81]
- **Committing the unfixed-writer red run.** Record it as evidence, do not commit a red test.
- **Putting the lookup-miss text into a pass-through type to make it visible** without the user's decision (see Open Question 1).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Sanitised cause chain | A new exception wrapper for the Jackson case | `WithheldMessageException.of(e)` (exists, covers cycles and depth 32) | D-02 and the generic path already use it. |
| Rendering what Connect prints | A new stack-trace renderer | `ThrowableTexts.printedStackTrace` / `InMemoryDLQ.connectHeaders` | They reproduce the worker's behaviour with explicit UTF-8 (JVM runs windows-1252). |
| Log capture | A custom appender | `LogCapture.attach(Class)` | Existing precedent for log assertions. |
| Test double for the helper client | Mockito inline mock | `CountingHelperClient` | Inline-mocking `ClickHouseHelperClient` crashes the OpenJ9 test JVM (class Javadoc, HARN-08). |
| Per-case "byte-identical" proof for the 184 cases | Eyeballing | Compare testcase name lists from the two `TEST-...xml` files plus a diff showing only `+` lines for the test source | Phase 22 evidence used `pin_testcases` and `pin_unchanged` lines. |

**Key insight:** every piece of machinery this phase needs already exists in the repo; the risk is in the interactions (what each helper sees after the fix), not in missing tools.

## Common Pitfalls

### Pitfall 1: The live test goes red for the wrong reason after the fix (F-1)
**What goes wrong:** `assertJacksonRouteReached` stops finding `InvalidDefinitionException` because the chain now holds `WithheldMessageException` nodes.
**How to avoid:** adapt the helper to accept the original class name carried by `WithheldMessageException`; run the adapted test on the unfixed writer first to show the red comes only from the key assertions.
**Warning signs:** failure message begins "the Jackson route was not reached; the cause chain is: [...WithheldMessageException...]".

### Pitfall 2: Intermediate red commit between the writer fix and the live flip
**What goes wrong:** CONTEXT orders "writer + FORK-DELTA commit, then flip commit". Between them the unflipped `JsonAsStringKeyResidualLiveTest` asserts the key IS present and fails on the fixed writer. Any suite gate or bisect landing there is red.
**How to avoid:** either put the writer change and the flip in the same commit, or run no gate between the two and keep them adjacent. Record the unfixed-writer red run as scratch evidence either way. This follows Phase 22's forced-red precedent (L-47: `f2-forced-red` run recorded, test flip committed with the fix). [CITED: 22-BEHAVIOUR-LEDGER.md L-47]
**Warning signs:** `./gradlew test` red at the writer commit.

### Pitfall 3: Touching existing lines in `WriterFailureClassificationUnitTest`
**What goes wrong:** the last enum constant ends with `ARRAY_BYTES_BEFORE_ELEMENT));` and `;`. Appending constants after it rewrites that line (`;` to `,`), so the diff shows a removed line and the "byte-identical" check needs an explanation.
**How to avoid:** insert the new constants in the middle of the list (for example after `SOCKET_TIMEOUT`), so the diff is `+` lines only. Test display names use `{0}` (the enum name), so existing case names do not change. [VERIFIED: WriterFailureClassificationUnitTest.java:125-147, 359]
**Expected count:** 184 + 8 per new kind (2 clientV2 x 2 tolerance x 2 entries); the SOCKET kind adds extra only for `RetryClass.SOCKET`, so Jackson kinds add 8 each: 3 kinds = 208, 4 kinds = 216.

### Pitfall 4: A comment edit changes an anchor count
**What goes wrong:** anchors count matching **lines** (`grep -cF`). Reflowing or rewording a comment can add or remove a line containing an anchored token. Anchored tokens in the files being scrubbed include `WithheldMessageException.of(` (1), `WRITE_VALUE_CONVERSION_FAILURE_FORMAT` (2), `WRITE_VALUE_FAILURE_LOG_FORMAT` (2), `buildRowBinaryUnsupportedTypeMessage` (5), `shouldSkipTableMetadata()` (8), `MISSING_NON_NULLABLE_MESSAGE_FORMAT` (2), `logDateTimeParseFailure(` (3), `DATE_CONVERSION_UNSUPPORTED_MESSAGE_FORMAT` (2). [VERIFIED: fork-anchors.txt:20-70, 78-83]
**How to avoid:** before each scrub commit, run the gate; and compute `grep -cF` for every anchored token of the touched file on the old and new blob. Several of those tokens occur in Javadoc `{@link ...}` lines (for example `buildRowBinaryUnsupportedTypeMessage` at `ClickHouseWriter.java:631`), so do not reword those lines.

### Pitfall 5: Anchor rows for the enum lines must move with the code
**What goes wrong:** `fork-anchors.txt:82` pins `throw new RuntimeException("No content in enum");` with count 1. The D-07 text change makes that count 0 and fails invariant 3 at that commit.
**How to avoid:** in the same commit, replace the row with the new throw line (or a stable fragment such as `NO_ENUM_CONTENT_FORMAT`), keep the negative row `No content in enum %s` (0), and add rows for the new `IllegalArgumentException` throw and for `JacksonException` in `ClickHouseWriter.java` (so an upstream sync cannot silently revert either fix). Stating the new `anchor_rows` total in the plan's verify step prevents silent drift. Tail gate command is in Environment Availability.

### Pitfall 6: `Map.of(...)` enum maps and a null label
**What goes wrong:** the existing classification fixture builds `Column ... .enumValues(Map.of("a", 1))`; `Map.of(...).get(null)` throws `NullPointerException`, unlike the production `HashMap` from `Column.extractEnumValues` [VERIFIED: Column.java:373-382, `Map <String, Integer> data = new HashMap<>();`]. A test that passes a null label through a `Map.of` fixture would see an NPE that is not the bug being fixed.
**How to avoid:** use a non-null unknown label (`"zz"`, as the existing fixture does) and build enum fixtures for new rows with `HashMap` when they probe null.

### Pitfall 7: Display names and method names cited elsewhere
**What goes wrong:** the flipped live test's method names (`mapKeyReachesTaskStatusAndInsertErrorUnderToleranceNone`, ...) describe the old behaviour and must be renamed. Only archived planning documents cite them; no code, workflow or doc outside `.planning` does (grep this session found `JsonAsStringKeyResidual` in `docs/FORK-DELTA.md` and `.planning/*` only, and `mapKeyReaches` only in `.planning` archives).
**How to avoid:** rename freely, keep the class name (FORK-DELTA rows 42, 82 and line 217 cite it). Published docs (`docs/ROLLOUT-v1.7.0_gg3.md`, CHANGELOG sections below `1.7.0_gg4`) are frozen history and must not be edited. [CITED: 24-REVIEW.md "Released CHANGELOG sections and the gg1 and gg2 notes are unchanged"]

### Pitfall 8: Red-first for premise loops needs a mutated premise, not a code revert
**What goes wrong:** the old loop is vacuous when no row matches, so "run the new test on the old code" cannot show red.
**How to avoid:** show the **old** loop stays green when the premise key is mutated (for example look up `CELL_COLUMN + "_x"`), and the **new** code fails with the message. Record both outputs. Revert the mutation with `git checkout --`. [CITED: PITFALLS.md Pitfall 12 template]

## Code Examples

### WRT-16 row construction (Docker-free, green on the unchanged writer)

Jackson constructors available to tests (public, from `javap` on `jackson-databind-2.22.4.jar`): `JsonMappingException(String)`, `JsonMappingException(String, Throwable)`. `JsonMappingException extends DatabindException extends ... JsonProcessingException extends JacksonException extends IOException` [VERIFIED: javap output this session]. `JacksonException`'s own constructors are `protected`, so use `JsonMappingException`.

```java
// Source: pattern of WriterFailureClassificationUnitTest.java:116-125 (uuidStream) + javap above
JACKSON_ALONE(
        RetryClass.NON_RETRIABLE,
        () -> uuidStream(new JsonMappingException("scripted jackson failure"))),
JACKSON_OVER_SOCKET_TIMEOUT(
        RetryClass.RETRIABLE,
        () -> uuidStream(new JsonMappingException("scripted jackson failure", new SocketTimeoutException("timed out")))),
JACKSON_OVER_TIMEOUT_TEXT(
        RetryClass.RETRIABLE,
        () -> uuidStream(new JsonMappingException("scripted jackson failure", new IOException("Read timed out after 30000 ms")))),
```

Why these outcomes are identical before and after WRT-17: on the unchanged writer the whole chain passes through and `Utils.handleException` reads the root cause (timeout classes or the "Read timed out after" prefix at message start) [VERIFIED: Utils.java handleException, `msg.indexOf(CLICKHOUSE_CLIENT_ERROR_READ_TIMEOUT_MSG) == 0`]; after WRT-17 the same two shapes still pass through because the non-Jackson link beneath counts, and Jackson-alone moves from "passes through, non-retriable" to "DataException, non-retriable", which `Processing` wraps in the same `RuntimeException` either way, so the test's outcome enum is unchanged (the existing `INVALID_UUID` kind is a `DataException` after WRT-13 and expects `RUNTIME_WRAP`). [VERIFIED: WriterFailureClassificationUnitTest.java:268-273 `expected(...)`, Processing.java:70-72 `throw new RuntimeException(queryId.toString(), e)`]

### Docker-free Jackson key test (recommended, red then green)

```java
// Source: pattern of WriterValueFreeFailureUnitTest.unsupportedJsonFieldTypePassesThrough (lines 412-422) and
//         the Struct+Map construction in JsonAsStringKeyResidualLiveTest.mapBatch (lines 110-117)
Map<String, String> props = minimalProps();
props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, JSON_AS_STRING_SETTING);
// Build Struct{m: Map<String,String>} validly, then add KEY_SENTINEL -> new Object() after Struct.put (Connect validates on put).
Captured captured = writeColValue(new ClickHouseSinkConfig(props), column(JSON_COLUMN, Type.JSON), structData);
DataException thrown = assertInstanceOf(DataException.class, captured.thrown);
assertNoSentinelAnywhere(thrown, captured, KEY_SENTINEL);
// and the original class is still visible:
assertTrue(ThrowableTexts.printedStackTrace(thrown).contains("InvalidDefinitionException (message withheld"));
```

On the unchanged writer `captured.thrown` is the `InvalidDefinitionException` itself and carries the key, so the row is red there. Also add the pass-through counterpart: `JsonMappingException("scripted", new SocketTimeoutException(...))` through `FailingOutputStream` on a UUID column must come back as the **same instance** (`assertSame`), the pattern of `streamIoExceptionPassesThrough` (lines 440-450).

### Premise-loop fix (DEBT-16)

```java
// Source: NullableTupleMatrixLiveTest.java:810-814 (current) and 23-REVIEW IN-03
ClickHouseFieldDescriptor cellRow = null;
for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
    if (CELL_COLUMN.equals(row.getName())) {
        cellRow = row;
    }
}
assertNotNull(cellRow, PREMISE_MESSAGE + ": DESCRIBE returned no row named " + CELL_COLUMN);
assertEquals(NULLABLE_BACKSLASH_N_NAME_RAW, cellRow.getType(), PREMISE_MESSAGE);
// lookups: Column cell = columns.get(CELL_COLUMN); assertNotNull(cell, CELL_COLUMN); ... likewise for the element key
```

Exact sites (line numbers from this session's read): premise loop `:810-814`; `columns.get(CELL_COLUMN).isNullable()` `:820`; `columns.get(CELL_COLUMN + PATH_SEPARATOR + BACKSLASH_N_ELEMENT).isNullable()` `:821-823`; `table.getAllColumnsMap().get("n.p\\\\nq.t").isNullable()` `:896`. The loops at `:568` (log only) and `:743`, `:885` (results compared by `assertEquals` on whole maps at `:759` and `:890`) are not vacuous and need no change. `assertNotNull` is already imported in this file (used at `:580`).

### `schemaWithFixedStringMismatchTest` (DEBT-16 / 22-REVIEW IN-04)

```java
// Source: ClickHouseSinkTaskWithSchemaTest.java:906-917 (current) and 22-REVIEW IN-04
ClickHouseSinkTask chst = new ClickHouseSinkTask();
chst.start(props);
try {
    RuntimeException e = assertThrows(RuntimeException.class, () -> chst.put(sr));
    WithheldMessageException withheld = assertInstanceOf(WithheldMessageException.class, Utils.getRootCause(e), "Could not detect size mismatch for FixedString");
    assertEquals(IllegalArgumentException.class.getName(), withheld.getOriginalClassName(), "Could not detect size mismatch for FixedString");
} finally {
    chst.stop();
}
```

`chst.stop()` currently runs after the catch; with `assertThrows` it must sit in a `finally` so a failing assertion does not leak the task. Red proof: temporarily make the table's `FixedString(n)` wide enough (`fss` instead of `fss - 1` at `:901`) so `put` does not throw; the old test stays green (vacuous), the new one fails. `assertThrows` and `assertInstanceOf` imports must exist in that file (A3, check on first edit).

## Planning-ID hit list for the scrub (`src/main`, measured this session)

Keep the invariant sentence; drop only the tag. `Pxx` = hit not shaped like an ID.

| File | Lines | Tags |
|------|-------|------|
| `sink/db/ClickHouseWriter.java` | 346, 569, 582, 612, 620, 636-637, 718, 782-783, 1595, 1809, 1815 | `WR-03`, `D-07, D-08` (and the long line), `WRT-08`, `D-09`, `TYPE-06`, `D-01, D-03`, `WRT-06`, `D-10`, `COLS-10`, `JSONP-08`, `D-05`, `JSONP-08` |
| `util/WithheldMessageException.java` | 15 | `WRT-13, D-05, D-06` (do not touch lines 23-24, Pitfall 4 and the anti-patterns) |
| `util/jmx/SinkTaskStatistics.java` | 165, 170-171, 185 | `D-01/D-06`, `D-06, RESEARCH` / `Pitfall 4` (**P: no ID shape**), `D-08` |
| `sink/db/mapping/DeclaredNullability.java` | 15, 221 | `COLS-10`, `Phase 28's` (**P: no ID shape**) |
| `sink/db/helper/DescribeTableBuilder.java` | 19, 50, 187, 302 | `DESC-07`, `DESC-08`, `DESC-05`, `COLS-10` |
| `sink/db/mapping/Column.java` | 530 | `D-14` (this comment is rewritten anyway by WRT-18) |
| `sink/db/helper/ClickHouseHelperClient.java` | 362, 372 | `CR-01`, `CR-01, D-19` |
| `sink/db/mapping/ParentShape.java` | 7 | `DESC-08` |
| `transforms/HeaderToValue.java` | 142 | `D-02` |

`upstream #820` at `ClickHouseWriter.java:612` is a legitimate upstream reference and stays. The count (about 28 lines) is slightly above CONTEXT's "~24" because multi-line tags and the two non-ID hits are included. [VERIFIED: grep over src/main, this session]

## FORK-DELTA, CHANGELOG, anchors and ledger edits per requirement

| Requirement | `docs/FORK-DELTA.md` | `fork-anchors.txt` | `CHANGELOG.md` (`1.7.0_gg4, unreleased` only) | Ledger / rollout (for Phase 29) |
|-------------|----------------------|--------------------|-----------------------------------------------|---------------------------------|
| WRT-17 | Row at line 42: remove ``On the opt-in JSON-as-string route ... that residual is measured and accepted (Known costs item 6)``; change "only when its cause chain holds an `IOException`" to say a Jackson exception does not count. Known costs item 6 (line 171): remove from ``One such route was measured in Phase 24 ...`` to ``... (`24-COMMAND-EVIDENCE.md` E-03).`` and keep the earlier sentence that IOException chains pass through, amended for Jackson. Line 217 sentence ``JsonAsStringKeyResidualLiveTest pins the one route on which a Jackson failure can still name a Map key.`` describes the published `gg3` and needs a "closed in `v1.7.0_gg4`" amendment (planner decision, Open Question 2). | add rows for `JacksonException` in `ClickHouseWriter.java` and the changed import | one bullet: a Jackson failure on the JSON-as-string route no longer shows a Map key in task status, DLQ stack-trace header or the `Error inserting records` log | new ledger row: before/after text, how an operator notices (the `Caused by: com.fasterxml... (message withheld...)` line replaces the Jackson message) |
| WRT-18 | Row at line 53: new text `No content in enum column <name>`; add the lookup-miss class (`IllegalArgumentException`) to the row for unknown Enum labels (row 42 lists "an unknown `Enum` label") | replace `throw new RuntimeException("No content in enum");` (1); keep `No content in enum %s` (0); add a row for the new `IllegalArgumentException` throw | one bullet: an unknown Enum label now reports `java.lang.IllegalArgumentException` instead of `java.lang.NullPointerException`; the `No content in enum` text names the column | ledger row (class name change is operator-visible in `Failed to write column ...: java.lang.IllegalArgumentException`); rollout line (alert rules matching `NullPointerException` for Enum) |
| WRT-19 | none expected (behaviour identical); check the row that says the prefix is shared | none; verify counts (Pitfall 4) | none (no behaviour change) | none |
| DEBT-16 | Row at line 82: drop `isUseClientV2` from the list of counted calls | none | none | none |

## Runtime State Inventory

Not applicable: this is not a rename, rebrand or migration phase. Removing planning IDs from source comments and the `useClientV2Calls` field leaves no stored data, service config, OS registration, secret or build artifact keyed on those strings. Verified by search: `useClientV2Calls` occurs only in `CountingHelperClient.java` (4 lines) [VERIFIED: `grep -rn "useClientV2Calls" src`].

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Jackson 2.x checked exceptions under `JsonProcessingException` | `JacksonException` base class (still `extends IOException` in 2.x) | Jackson 2.12 | Excluding the base class covers every 2.x Jackson checked exception. Jackson 3 (`tools.jackson`) makes `JacksonException` unchecked, which is irrelevant to this classpath. [VERIFIED: javap on 2.22.4; Jackson 3 claim is `[ASSUMED]`, not needed for the plan] |

**Deprecated/outdated:** none relevant.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A Docker-free row can reproduce the real `InvalidDefinitionException` by calling `doWriteColValue` on a `Type.JSON` column with a Struct whose Map holds a sentinel key mapped to `new Object()` | Findings F-2, Code Examples | Low: the live test already does exactly this through `put`; if the unit variant cannot reproduce it, fall back to the scripted `JsonMappingException` row plus the live flip. |
| A2 | The exact `ClassCastException` text for `effective_at`/UInt64/STRING is ``Failed to write column `effective_at`: cannot convert Kafka value of type STRING (value class: java.lang.String) to ClickHouse type UINT64`` | Finding F-6 | Low: derived from the format at `ClickHouseWriter.java:898-900` and the `contains` checks in `ClickHouseWriterColumnContextTest`; confirm by running the unchanged writer before pinning. |
| A3 | `ClickHouseSinkTaskWithSchemaTest` already imports `assertThrows` and `assertInstanceOf` | Code Examples | Trivial compile fix; `assertInstanceOf` is already used at `:913`, `assertThrows` unchecked. |
| A4 | New behaviour-ledger IDs continue at `L-52` in a new `27-BEHAVIOUR-LEDGER.md` (no `L-52`+ id appears anywhere under `.planning`; Phase 26 wrote no ledger) | FORK-DELTA/ledger table | Low: naming only. |
| A5 | The `Array(JSON)`/`Tuple(JSON)` nested route is safe after the fix because the child's own `doWriteColValue` catch converts the Jackson failure to a `DataException` before the parent lambda wraps (`catch (IOException)` wrappers at `ClickHouseWriter.java:747, 775, 819` only see IOExceptions that passed through) | Pitfalls | Medium-low: if a Jackson `IOException` could escape a child unconverted it would be wrapped in an exact `RuntimeException` and pass through again. A single nested-JSON unit row would confirm; recommended but optional. |
| A6 | The `insertFormat=json` route (`writeRecordsJson`, `OBJECT_MAPPER.writeValueAsBytes(cleaned)` at `ClickHouseWriter.java:1473`) can also raise a Jackson failure whose text names a Map key, and is not covered by this phase | Open Questions | Out of scope by the phase goal ("on the opt-in JSON-as-string route"); recorded so the user can add it to the backlog. |

## Open Questions

1. **Should the lookup-miss text be operator-visible?**
   - What we know: with D-06 the thrown `IllegalArgumentException` is wrapped by `WithheldMessageException.of(e)`, so its message (column name, "label not found") appears nowhere an operator reads. The operator sees the class `IllegalArgumentException` and the already-present column in the `DataException` prefix.
   - What's unclear: whether roadmap criterion 3 ("a message that names the column and a lookup-miss cause") is satisfied by the `Column`-level message plus the class change, or whether the user wants the text surfaced.
   - Recommendation: implement D-05/D-06 as locked; prove the message at `Column` level and the class at writer level; state this limit in the evidence and the ledger row. Surfacing the text would need a pass-through type (a `DataException` thrown from `Column`), which is a different decision from D-06.

2. **Does the FORK-DELTA line 217 sentence (release history of `v1.7.0_gg3`) get an amendment?**
   - What we know: CONTEXT's canonical refs list "the release-history sentence naming `JsonAsStringKeyResidualLiveTest`" among the edited docs, but D-03 names only item 6 and the `doWriteColValue` row.
   - Recommendation: append "(closed in `v1.7.0_gg4`)" so the sentence stays true; do not touch `docs/ROLLOUT-v1.7.0_gg3.md`.

3. **A fourth WRT-16 shape.** See F-5; planner picks the duplicate control or the Jackson-under-plain-`IOException` variant.

4. **Jackson failure on the `insertFormat=json` route** (A6). Not in scope; consider a backlog item.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | Live tests (`JsonAsStringKeyResidualLiveTest`, `NullableTupleMatrixLiveTest`, `ClickHouseSinkTaskWithSchemaTest`) | yes | colima `vz` Running, 8 CPU, 16 GiB (`colima list`) | none for live tests; Docker-free rows run in `testUnit` |
| JDK | Gradle toolchain | yes | OpenJDK 21.0.12.1 on PATH (project toolchain is 17; `-PjavaVersion=21` is the HotSpot escape for the known OpenJ9 crash) | `-PjavaVersion=21` |
| Gradle wrapper | All builds | yes | 9.8.1 (run: `BUILD SUCCESSFUL in 10s`) | none |
| `testUnit` task | Docker-free rows | yes | baseline run: `WriterFailureClassificationUnitTest` 184 tests, 0 failures, 0 errors | none |
| Tail merge-invariants gate | Anchors at every commit | yes | `result=PASS`, `anchor_rows=79` | none |
| `javap` | Jackson hierarchy checks | yes | JDK | none |

Commands (all verified to run this session):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WriterFailureClassificationUnitTest'
# result XML: build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.sink.WriterFailureClassificationUnitTest.xml
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc \
  CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt \
  .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
```

Tooling gotchas observed this session: `timeout` is not installed on this macOS host (use Gradle's own timeouts); the shell is zsh, so quote `--include='*.java'` globs; the working tree carries unstaged `gradlew.bat` and `.planning/state.json` changes that plan verify strings must exclude (STATE.md: "plan verify strings must exclude it").

**Missing dependencies with no fallback:** none. **Missing with fallback:** none.

## Evidence and Gate Protocol (replaces the Validation Architecture section; `workflow.nyquist_validation` is `false`)

| Step | Proof | Command or artifact |
|------|-------|---------------------|
| WRT-16 green first | New rows pass on the unchanged writer; old 184 names and results unchanged | two `TEST-...xml` testcase-name lists (before and after the rows), `git diff` of the test file shows only `+` lines |
| WRT-17 red first | Flipped live test and the new Docker-free key row fail on the unchanged writer only on key assertions | scratch run output saved to the phase evidence file; not committed |
| WRT-17 green | Same tests pass after the fix; live run through `put` on `latest` + V2 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*JsonAsStringKeyResidualLiveTest' --rerun` (the E-03 command shape) |
| WRT-18 red first | New unknown-label rows fail on the unchanged writer with class `NullPointerException`; `NO_ENUM_CONTENT_MESSAGE` edit fails on the unchanged writer | scratch run |
| WRT-19 | Exact `ClassCastException` text pinned green before the refactor; guard fails when a planning ID is planted in `src/main` (mutation) and passes on the scrubbed tree | scratch mutation, reverted with `git checkout --` |
| DEBT-16 | Mutated-premise runs (Pitfall 8) | scratch runs |
| Every commit | Tail gate PASS with the stated `anchor_rows`; `compileJava compileTestJava` | commands above |
| Phase gate | cache-busted `testUnit`; full suite on `latest` x V2, two `--rerun` runs; self-test of the invariants script; Spotless baseline-equal | per STATE standing rules |
| Final diff check | No removed or loosened assertion | `git diff baseline-v1.7.0_gg4..HEAD -- src/test \| grep '^-' \| grep -v '^---'`: the expected output is exactly the flipped live-test lines, the `useClientV2Calls` lines, `NO_ENUM_CONTENT_MESSAGE`, the `schemaWithFixedStringMismatchTest` catch block and the replaced premise loop lines; each is listed and justified |

Run the OpenJ9 caution: a Mockito GPF (`exit 255`) is an environment crash, not a failure; delete the core dump and re-run once. [CITED: project memory, openj9-mockito-test-jvm-crash]

## Security Domain

`security_enforcement` is enabled (ASVS level 1, block on high). The phase is about information disclosure through error and log channels.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | - |
| V3 Session Management | no | - |
| V4 Access Control | no | - |
| V5 Input Validation | partly | Labels, Map keys and list elements from records are untrusted input: new messages must carry column metadata only |
| V6 Cryptography | no | - |
| V7 Error Handling and Logging | yes | Errors and logs must not contain record data: `WithheldMessageException`, class-name-only log lines, sentinel-based tests (ASVS 7.1.x, CWE-209/CWE-532) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Record data (a Map key) leaking through a third-party exception message into Connect status, DLQ headers and logs | Information disclosure | Do not pass third-party exceptions that format object paths; wrap with `WithheldMessageException` (this phase closes the Jackson gap) |
| New diagnostic message embedding the unknown enum label | Information disclosure | Message built from `Column.name` only; sentinel test asserts label absent in thrown texts, rendering and logs |
| Planning-doc or review text in shipped code | Low (hygiene) | Standing guard test |
| Retry loop on a non-retriable failure or a missed retry on a timeout | Availability | WRT-16 pins retry classification before the change |

A `13-SECURITY`/`22-SECURITY`-style threat record for this phase will cite R-22-02 (the accepted Jackson residual) as closed. [CITED: docs/FORK-DELTA.md item 6 text, "`22-SECURITY.md` R-22-02"]

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `ClickHouseWriter.java` (lines 555-680, 850-930, 1030-1060, 1455-1495, import block), `Column.java` (55-70, 373-382, 515-570), `WithheldMessageException.java`, `Utils.java` (handleException), `WriterFailureClassificationUnitTest.java`, `WriterValueFreeFailureUnitTest.java`, `JsonAsStringKeyResidualLiveTest.java`, `NullableTupleMatrixLiveTest.java` (545-604, 725-912), `ClickHouseSinkTaskWithSchemaTest.java` (888-917), `CountingHelperClient.java`, `ThrowableTexts.java`, `ClickHouseWriterColumnContextTest.java`, `TestsWorkflowPinGuardUnitTest.java`, `fork-anchors.txt`, `verify-merge-invariants.sh` (head), `docs/FORK-DELTA.md` rows 42, 53, 82, item 6, line 217, `CHANGELOG.md` head, `build.gradle.kts` (spotless, test tasks), `gradle/libs.versions.toml`.
- `javap` against `jackson-core-2.22.4.jar` and `jackson-databind-2.22.4.jar` (Gradle cache): `public abstract class com.fasterxml.jackson.core.JacksonException extends java.io.IOException`; `public class com.fasterxml.jackson.core.JsonProcessingException extends com.fasterxml.jackson.core.JacksonException`; `public class com.fasterxml.jackson.databind.exc.InvalidDefinitionException extends com.fasterxml.jackson.databind.JsonMappingException`; `public abstract class com.fasterxml.jackson.core.exc.StreamWriteException extends com.fasterxml.jackson.core.JsonProcessingException`; public constructors `JsonMappingException(java.lang.String)` and `JsonMappingException(java.lang.String, java.lang.Throwable)`.
- Measured: baseline `testUnit` run (184 tests, 0 failures); tail gate run (`anchor_rows=79`, PASS); `grep` scans of `src/main` and `src/test`.

### Secondary (MEDIUM confidence)
- `.planning/research/PITFALLS.md` Pitfalls 12, 13, 20; `.planning/research/SUMMARY.md` Phase 27 notes; Phase 22 REVIEW/LEDGER/EVIDENCE; Phase 24 REVIEW and COMMAND-EVIDENCE E-03; Phase 23 REVIEW IN-03.

### Tertiary (LOW confidence)
- None used for decisions. Web search was not needed: every question was answered from the repo and the local jars.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new packages; versions read from the catalog.
- Architecture (edit sites, flow): HIGH - every edit site was read at line level.
- Pitfalls: HIGH for F-1, F-3, F-4, F-7, anchors; MEDIUM for A1/A5 (needs one scratch run).

**Research date:** 2026-10-22
**Valid until:** until `ClickHouseWriter.java`, `Column.java` or `fork-anchors.txt` change on `sync/v1.7.0` (re-read line numbers), otherwise 30 days.
