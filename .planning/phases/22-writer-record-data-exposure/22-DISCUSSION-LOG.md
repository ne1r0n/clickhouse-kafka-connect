# Phase 22: Writer Record-Data Exposure - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-19
**Phase:** 22-writer-record-data-exposure
**Areas discussed:** What gets replaced, Cause chain & frames, Message wording, Test evidence

---

## What gets replaced

Context given: the Map/Tuple lambdas wrap a nested stream `IOException` as `new RuntimeException(e)` (~L713, ~L785), so the pass-through test must look at the whole cause chain, not only the top-level type.

**Q1: Which failures should the generic catch replace with a value-free exception, and which should pass through unchanged?**

| Option | Description | Selected |
|--------|-------------|----------|
| Pass IO-chain + Connect (Recommended) | Rethrow unchanged if any exception in the cause chain is an IOException, or if it is a ConnectException/DataException; replace everything else. Unknown future types are covered by default | ✓ |
| Allowlist JDK conversion types | Replace only IAE (incl. NFE), ArithmeticException, DateTimeException; everything else rethrown as today. Other value-carrying types keep leaking | |
| Pass IO-chain only | Replace everything except IOException chains, including the fork's own DataExceptions; WRT-08/WRT-10 texts get re-wrapped | |

**Q2: What type should the replacement exception be?**

| Option | Description | Selected |
|--------|-------------|----------|
| DataException (Recommended) | Matches the ClassCastException branch; never re-wrapped by an outer Tuple/Map catch, so the innermost column name survives | ✓ |
| Plain RuntimeException | Closest to today's IAE/NFE; an outer catch would re-wrap it and name the outer column | |

**Q3: What should happen to exact `RuntimeException`s with safe text (JSON hint ~L849, ~L845, nested L-12 ~L1073/L1088)?**

| Option | Description | Selected |
|--------|-------------|----------|
| Pass exact RuntimeException (Recommended) | Also rethrow unchanged when `e.getClass() == RuntimeException.class`; no throw sites edited; research confirms no client/JDK code in reach throws an exact RuntimeException carrying a value | ✓ |
| Convert sites to DataException | Edit the throw sites so they pass through; edits upstream-identical lines and changes L-12's top-level root type | |
| Accept generic text | Let them be replaced; operators lose the JSON setting hint and the nested-null reason | |

**Q4: A record value containing `ClickHouseException: Code: 33` will no longer trigger a refresh. How should this be handled?**

| Option | Description | Selected |
|--------|-------------|----------|
| Accept and record it (Recommended) | Treat it as an intended loss (record content steering control flow); record it in CONTEXT, the ledger and FORK-DELTA; pin it with one test | ✓ |
| Preserve it | Carry a flag or marker over from the original message to keep the refresh | |

---

## Cause chain & frames

**Q1: What should the replacement DataException carry as its cause?**

| Option | Description | Selected |
|--------|-------------|----------|
| Sanitized copy (Recommended) | A fork class with the original's class name and stack frames but no message; its own causes sanitized recursively; suppressed dropped | ✓ |
| No cause | Simplest; the failing JDK/client frames are lost | |
| Keep the original as cause | Like the CCE branch; fails the sentinel requirement (listed for completeness) | |

**Q2: How should the sanitized cause render in a stack trace?**

| Option | Description | Selected |
|--------|-------------|----------|
| Original class + withheld note (Recommended) | `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)` | ✓ |
| Fork class + original class | Default `Throwable.toString()`; real class second, no hint why the message is missing | |
| Original class only | Looks like a message-less JDK exception; could mislead | |

---

## Message wording

**Q1: What base shape should the new DataException message have?**

| Option | Description | Selected |
|--------|-------------|----------|
| Follow the CCE precedent (Recommended) | `Failed to write column `x`: Kafka value of type … (value class: …) could not be written as ClickHouse type …: <exception class>`; shares the CCE prefix | ✓ |
| Mirror the log line | Reuse the `Error writing value of …` text; the CCE branch would then differ | |
| Phase 18 DataException style | `column 'x' (UUID): value could not be written: …`; departs from the roadmap's named precedent | |

**Q2: Should the message include the value's length?**

| Option | Description | Selected |
|--------|-------------|----------|
| No length (Recommended) | Metadata only; this message reaches Connect status and a DLQ topic other consumers read | ✓ |
| Length for String values | Consistent with the DateTime logs; adds a small piece of record information to the DLQ headers | |

**Q3: How should the ClickHouse type be rendered?**

| Option | Description | Selected |
|--------|-------------|----------|
| Type enum, like CCE (Recommended) | `UUID`, `INT32`; the same token as the CCE branch and the log line | ✓ |
| renderColumnType | `Nullable(UUID)`, `Array(Tuple(...))`; more informative, but differs from the CCE branch and the log line | |

---

## Test evidence

**Q1: How detailed should the WRT-14 classification pin be?**

| Option | Description | Selected |
|--------|-------------|----------|
| Full outcome (Recommended) | Exact outcome per failure kind × tolerance (× retryOnSocketException): RetriableException / DataException rethrown / RuntimeException wrap / returns normally | ✓ |
| Retriable vs not | Only whether a RetriableException comes out; passes even if the non-retriable outcome changes shape | |

**Q2: Where should the three surfaces be proven with the sentinel?**

| Option | Description | Selected |
|--------|-------------|----------|
| Docker-free + live V2 (Recommended) | Docker-free writer-level tests over V1/V2 via hooks; live `put()` on `latest` × V2 for tolerance none/all and the logs; UUID and number cases | ✓ |
| Live on V1 and V2 | Adds a live V1 run; V1 is outside the project's test target | |
| Docker-free only | Never sees how the real V2 client wraps the writer's exception | |

**Q3: How should the phase treat the `ServerException`-branch `Error inserting records` log?**

| Option | Description | Selected |
|--------|-------------|----------|
| Map it, stop if reachable (Recommended) | Research traces the branch per client; if unreachable, a written trace; if reachable, stop and ask before touching it | ✓ |
| Pin both regardless | A Docker-free ServerException test; pins a server message that never carried a writer value | |

**Q4: How should HARN-08 drop the Mockito mock and keep its pin/unpin and `isUseClientV2` assertions?**

| Option | Description | Selected |
|--------|-------------|----------|
| Hand-written helper fake (Recommended) | A test-only `ClickHouseHelperClient` subclass counting calls, injected by reflection; no production change | ✓ |
| Writer seams | Protected delegators in `ClickHouseWriter`; edits upstream-identical lines in a high-risk file for a test | |
| Writer-level counts | Replace the pin/unpin verifies with `updateMapping` counts; drops an original assertion | |

---

## Claude's Discretion

- The names and locations of the sanitized-cause class and the helper-client fake.
- Whether the replacement logic is a private helper in `ClickHouseWriter`, and how the cause-chain scan is written.
- The wording polish of the D-07 message within its fields, and whether the sanitized cause's `getMessage()` is null or the fixed note.
- The plan split within the fixed order HARN-08 → WRT-14 pin → WRT-13 + WRT-15.
- The evidence file layout.

## Deferred Ideas

None.
