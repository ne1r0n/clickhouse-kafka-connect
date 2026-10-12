# Phase 18: Writer Correctness - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-12
**Phase:** 18-writer-correctness
**Areas discussed:** Batch of only empty records, Null-message fix scope, DateTime log content, Message text & change log

---

## Batch of only empty records

**All-skipped batch handling**

| Option | Description | Selected |
|--------|-------------|----------|
| Empty insert, fallback | Keep the empty-body insert; the spike proves V1+V2 accept it; stop and ask before any short-circuit | ✓ |
| Always short-circuit | Return before opening the HTTP request; new branch in the four insert paths | |

**Skip log**

| Option | Description | Selected |
|--------|-------------|----------|
| Per record, DEBUG | Move the existing metadata-free line into the shared helper | ✓ |
| One count per batch | INFO count per batch; changes the existing log | |
| Both | Per-record DEBUG plus a batch count | |

**Test depth**

| Option | Description | Selected |
|--------|-------------|----------|
| Live core + Docker-free | Live mixed + all-empty per route; Docker-free byte identity, all-unknown schemaless row, exactly-once state | ✓ |
| All five pins live | Pitfall 4 (a)-(e) live on every route | |
| Minimum | One live mixed-batch test per route | |

**JMX metric**

| Option | Description | Selected |
|--------|-------------|----------|
| No metric | DEBUG log is enough | ✓ |
| Add a counter | New TopicStatistics attribute | |

---

## Null-message fix scope

**Utils.handleException IOException NPE**

| Option | Description | Selected |
|--------|-------------|----------|
| Fix, separate commit | Same plan as WRT-10/11; null message = not a timeout | ✓ |
| Defer, record why | Keep WRT-11 to ClickHouseWriter; record the gap | |

**Test depth**

| Option | Description | Selected |
|--------|-------------|----------|
| Unit + put()-level | Docker-free writer tests plus ClickHouseSinkTask.put() pins for both tolerance modes and a null-message timeout | ✓ |
| Docker-free unit only | Writer-level tests only | |

---

## DateTime log content

**Nearby leaks (generic catch :854, UUID/NumberFormat messages)**

| Option | Description | Selected |
|--------|-------------|----------|
| Record, don't fix | Security note + backlog entry | |
| Fix :854 too | Change the generic catch log as well | ✓ |
| Just mention it | One FORK-DELTA line | |

**:854 scope (follow-up)**

| Option | Description | Selected |
|--------|-------------|----------|
| Log only, keep throw e | Mask the log; rethrow unchanged so classification can't change; residual recorded | ✓ |
| Also wrap the rethrow | Data-free DataException; changes classification for IOException | |

**DateTime DataException column name**

| Option | Description | Selected |
|--------|-------------|----------|
| Add column name | Add column + ClickHouse type, no value | ✓ |
| Leave unchanged | Only the two log lines change | |

**Log capture**

| Option | Description | Selected |
|--------|-------------|----------|
| Reusable helper | reload4j appender test utility in src/test | ✓ |
| Inline in the test | Appender inside the test class | |
| You decide | Planner picks | |

**Notes:** `doWriteDates` doesn't receive the record, so topic/partition/offset stay out of the log (no upstream signature change). The user didn't object.

---

## Message text & change log

**WRT-10 wording**

| Option | Description | Selected |
|--------|-------------|----------|
| Sibling style + hint | `column '<name>' (<type>): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT` | ✓ |
| Sibling style, no hint | Same without the hint | |
| Keep the old log text | `Column <name> (<type>) is not nullable and no value is provided` | |

**Duplicate LOGGER.error**

| Option | Description | Selected |
|--------|-------------|----------|
| Drop it | The exception carries it; callers already log | ✓ |
| Log the new message | One ERROR line with the same text | |

**Change record location**

| Option | Description | Selected |
|--------|-------------|----------|
| Phase ledger file | `18-BEHAVIOUR-LEDGER.md`, rows added with each fix | ✓ |
| CHANGELOG now | Unreleased 1.7.0_gg2 section | |
| Both | Ledger + CHANGELOG line | |

---

## Claude's Discretion

- Helper names and locations; exact masked log wording within the content rules; RowBinary live-test construction; plan split and order (WRT-10/11 + Utils commit stay one plan); whether `:854` logs a message-free stack.

## Deferred Ideas

- The `:854` rethrown exception message can still carry the value into Connect status/DLQ: backlog entry 999.x + FORK-DELTA Known cost.
- Short-circuit for all-skipped batches: only if the spike fails, with the user's approval.
- A JMX counter for skipped empty CDC records: rejected for now.
