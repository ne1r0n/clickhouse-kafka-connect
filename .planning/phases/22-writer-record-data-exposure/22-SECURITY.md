---
phase: "22"
slug: "writer-record-data-exposure"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-20"
---

# Phase 22 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Test JVM, instrumentation | Inline mocking instruments `ClickHouseHelperClient` bytecode via `retransformClasses` inside the shared full-suite JVM | Test bytecode |
| Test code, assertions | A refactor of a pinning test can silently drop or loosen what it pins | Test assertions |
| Writer failure, retry decision | What the writer throws decides whether Connect retries (RetriableException), fails the task or sends the batch to the DLQ | Exception classes |
| Pin, later change | A pin that can be edited alongside the change it guards proves nothing | Test source |
| Kafka record value, writer exception text | Untrusted record content is parsed in `doWriteColValue`; JDK and client exceptions embed it in their messages | Record values (sensitive) |
| Writer exception, Connect status / DLQ topic / logs | The REST-readable task status, a DLQ topic other consumers read, and log aggregation all render the thrown chain | Exception text (may carry record values) |
| Writer exception text, control flow | `doInsertRawBinary` decides on a mapping refresh by matching the failure message | Exception text |
| Connector, DLQ topic | Other consumers read the DLQ record headers built from the reported exception | DLQ headers |
| Connector, Connect REST status | The task status trace is readable over the Connect REST API | Stack trace text |
| Real client V2, writer exception | The client wraps the writer's exception before `doInsertRawBinary` sees it | Exception chain |
| Local gate results, verifier and release | Phase 24 publishes on the strength of these results | Gate logs and exit codes |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-22-01 | Tampering | WRT-11 pins in ClickHouseWriterNullMessageUnitTest | medium | mitigate | E-01 `harn08_before`/`harn08_after`: tests=5 failures=0. At fefd8648 and HEAD the class has 8 test annotations, 6 `assertSame`, 6 `hookCalls`, 3 `RETRY_TRIGGER_MESSAGE`; display names and method signatures diff-identical. | closed |
| T-22-02 | Denial of Service | OpenJ9 test JVM (retransformClasses GPF) | medium | mitigate | 0 `org.mockito` lines in `ClickHouseWriterNullMessageUnitTest` and `CountingHelperClient` (E-01 `mockito_lines=0`, rechecked at HEAD). 0 added `org.mockito` lines in `src/test` since fefd8648. | closed |
| T-22-03 | Information Disclosure | CountingHelperClient constructing a real client | low | accept | See R-22-01. | closed |
| T-22-04 | Tampering | classification evidence (the pin) | high | mitigate | `WriterFailureClassificationUnitTest` has 0 `getMessage(`/`getCause(`/`getRootCause(` reads and asserts reach markers (`hookCalls()` exactly once, line 330). E-02: 184 cases through the real ProxySinkTask, Processing, ClickHouseWriter and ChunkFlusher. | closed |
| T-22-05 | Repudiation | green-on-unchanged-writer claim | medium | mitigate | E-02 `writer_unchanged: git diff --quiet fefd8648 HEAD -- src/main (exit 0)`, `pin_testcases: 184`, `pin_commit: 0590d20c`. E-03 `pin_unchanged_since` and `pin_testcases_after: 184`. | closed |
| T-22-06 | Tampering | ScriptedWriteFailureWriter reused by 22-03 | medium | mitigate | `git diff --stat 0590d20c HEAD` on the pin class, `ScriptedWriteFailureWriter` and `CountingHelperClient` is empty (byte-identical). | closed |
| T-22-07 | Information Disclosure | doWriteColValue generic catch (T-18-07) | high | mitigate | `ClickHouseWriter.java:909` throws `DataException(buildValueConversionFailureMessage(...), WithheldMessageException.of(e))`; the log line at :904 carries field type, column, ClickHouse type and class only. E-03 `tracer_red` 4/4 and `expansion_red` 13 red on the unchanged writer, `TRACER-GREEN EXPANSION-GREEN` after. | closed |
| T-22-08 | Information Disclosure | suppressed exceptions and cause messages | high | mitigate | `WithheldMessageException.java:43` `super(WITHHELD_NOTE, sanitizedCause, false, true)` disables suppression. `ThrowableTexts.allTexts` walks causes and `getSuppressed()` (line 52); covered by `WithheldMessageExceptionUnitTest`. | closed |
| T-22-09 | Information Disclosure | exact RuntimeException pass-through (F1) | medium | mitigate | `Column.java:529` now throws a fixed `"No content in enum"` text (D-14 comment at :528). E-03 `d03_result`: the only reachable value-carrying site; D-14 case 1/1 red before, green after. | closed |
| T-22-10 | Tampering | record content steering the refresh (D-04) | medium | mitigate | `WriterValueFreeFailureUnitTest` D-04 V1 (line 479) and D-04 V2 (line 498) assert `hookCalls` 1, `pinCalls` 0, `updateMappingCalls` 0. E-03: D-04 V1 2/2 red before, D-04 V2 2/2 green on both writers. | closed |
| T-22-11 | Denial of Service | WithheldMessageException.of on cyclic or deep chains | low | mitigate | `WithheldMessageException.java:38` `MAX_CAUSE_DEPTH = 32`; :54-56 identity set (`newSetFromMap(new IdentityHashMap<>())`) bounds the walk. | closed |
| T-22-12 | Tampering | retry classification | high | mitigate | `passesThroughUnchanged(e)` rethrows IO chains (`ClickHouseWriter.java:906`). The WRT-14 pin and fixtures are byte-identical to 0590d20c and pass 184/184 (E-05 `pin_unchanged`). | closed |
| T-22-13 | Information Disclosure | IOException chains passed through unchanged, including Jackson JsonProcessingException on the JSON-as-string route | low | accept | See R-22-02. | closed |
| T-22-14 | Repudiation | doc claims vs pins | medium | mitigate | `c713a837` changes 12 files in one commit (writer, Column, WithheldMessageException, FORK-DELTA, ledger, ROADMAP, anchors, 5 tests). `docs/FORK-DELTA.md:42` states the D-01 IO pass-through and the D-15 DLQ message reading. | closed |
| T-22-15 | Information Disclosure | live V2 path (real client wrap, real server) | high | mitigate | E-04 `live_red`: every `WRT-13 live` case red at 6ecfae89 (4/4 and 1/1), `ValueFreeDlqHeadersUnitTest` 2/2 red; `live_green`: all green on latest (26.9.19.9) x V2. | closed |
| T-22-16 | Information Disclosure | loggers outside LogCapture's scope (client-v2 internals, the Connect runtime) | low | accept | See R-22-03. | closed |
| T-22-17 | Repudiation | DLQ message header claim (D-15) | medium | mitigate | `ValueFreeDlqHeadersUnitTest:39` full-matches the `QueryIdentifier` text pattern and :41 checks `Failed to write column ` is absent from the message header. Stated in L-48, E-04 and FORK-DELTA item 6. | closed |
| T-22-18 | Repudiation | phase gate results | medium | mitigate | E-05 `testunit_rerun` and `full_suite` with `--rerun`; `/tmp/gsd22/exec/` holds 65 files, `full-suite.rc` 0 and `gate-final.rc` 0. | closed |
| T-22-19 | Tampering | release scope | medium | mitigate | E-05 `SCOPE-ORDER-OK`. Rechecked: `src/main` diff since fefd8648 is exactly ClickHouseWriter, Column and WithheldMessageException; VERSION, CHANGELOG.md, build.gradle.kts, gradle/ and .github/ untouched; HEAD on no remote ref. | closed |
| T-22-SC | Tampering | npm/pip/cargo installs | low | accept | See R-22-04. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-22-01 | T-22-03 | The `CountingHelperClient` constructor opens no connection and owns no thread (22-RESEARCH, measured). It targets 127.0.0.1 with no credentials and is closed per test with try-with-resources. | Plan threat model (22-01, accept at plan time) | 2026-10-19 |
| R-22-02 | T-22-13 | Locked D-01 requires IO chains to pass through unchanged so retry classification cannot move. A Jackson `JsonProcessingException` reference chain on the opt-in JSON-as-string route can name a Map key (verifier measurement, review WR-01). Disclosed in FORK-DELTA item 6. | Plan threat model (22-03); user accepted in 22-UAT test 3 | 2026-10-20 |
| R-22-03 | T-22-16 | Client-v2 internals and the Connect runtime log outside this phase's control. The client logs a throwable only on retry, which no measured run hit; Utils, ProxySinkTask and ChunkFlusher log only the top wrapper's queryId text. | Plan threat model (22-04, accept at plan time) | 2026-10-19 |
| R-22-04 | T-22-SC (22-01 to 22-05) | No package, dependency or build change in Phase 22 (22-RESEARCH Package Legitimacy Audit: none). Rechecked: build.gradle.kts and gradle/ untouched since fefd8648. | Plan threat models (accept at plan time) | 2026-10-19 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-20 | 20 | 20 | 0 | gsd-secure-phase (L1 short-circuit: register authored at plan time, all mitigations evidenced) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-20
