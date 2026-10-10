# Phase 13: Fork Hardening on the Merged Tree - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-08
**Phase:** 13-fork-hardening-on-the-merged-tree
**Areas discussed:** Logical-type encoding, Conversion scope, Nested-null containers, Small-fix depth

---

## Logical-type encoding

**Q: What should define a 'correct' value in the JSON-route type matrix?**

| Option | Description | Selected |
|--------|-------------|----------|
| RowBinary parity | Same stored value as RowBinary for the same Struct and column; objective, no per-cell judgement | ✓ |
| Per-type expectation | Hand-written expected value per cell | |
| Parity, with listed exceptions | Parity default, named FORK-DELTA exceptions | |

**Q: May the JSON route add its own insert setting (e.g. date_time_input_format=best_effort)?**

| Option | Description | Selected |
|--------|-------------|----------|
| No forced settings | Encoding must work under operator settings; tz-independent numeric form picked by the spike | ✓ |
| Route-scoped setting allowed | Send ISO-8601 UTC + best_effort on this route's inserts | |
| Spike decides | Prefer none, allow if no numeric form reaches parity | |

**Q: Kafka `bytes` into a String column?**

| Option | Description | Selected |
|--------|-------------|----------|
| Raw parity, else fail | Raw bytes with minimal escaping if the spike proves verbatim storage; else DataException naming the field | ✓ |
| Raw parity, else base64 | Fall back to today's base64 as a named exception | |
| Keep base64 | Pin base64 as documented behaviour | |

**Q: How far should the parity matrix reach?**

| Option | Description | Selected |
|--------|-------------|----------|
| Supported pairs only | Only pairs RowBinary writes; rejected pairs out of scope | ✓ |
| Also pin rejections | Assert JSON route also rejects | |
| Supported + observe others | Record rejected-pair behaviour as Recorded Facts | |

---

## Conversion scope

**Q: Which JSON writes get the logical-type conversion?**

| Option | Description | Selected |
|--------|-------------|----------|
| Metadata-free only | Gate on shouldSkipTableMetadata(); upstream bypassRowBinary=true untouched | ✓ |
| All Struct JSON writes | Also changes upstream's bypassRowBinary=true route | |
| Struct + Debezium JSON | Widest, all routes | |

**Q: DEBEZIUM_CDC shares the switch case — cover it too?**

| Option | Description | Selected |
|--------|-------------|----------|
| Route + convert both | Same conversion for CDC on this route; ≥1 CDC row in the matrix | ✓ |
| Route both, convert SCHEMA | CDC lands with unconverted logical types | |
| Route SCHEMA only | CDC keeps silent loss | |

**Q: Convert logical types nested in containers?**

| Option | Description | Selected |
|--------|-------------|----------|
| Recurse, pin a few | Every depth; a handful of nested cells pinned | ✓ |
| Top-level fields only | Nested keep Jackson defaults | |
| Recurse, full matrix | Every type at every depth pinned | |

**Q: Routing unconditional or behind a flag?**

| Option | Description | Selected |
|--------|-------------|----------|
| Unconditional | Matches locked decision; record for CHANGELOG/FORK-DELTA/rollout incl. KeeperMap backfill | ✓ |
| Opt-out flag | Config to restore old behaviour | |
| Opt-in flag | Keep silent loss by default | |

---

## Nested-null containers

**Q: Nested null Array/Map in a non-nullable slot?**

| Option | Description | Selected |
|--------|-------------|----------|
| Coerce to empty, always | Mirror #820 regardless of table defaults; WRT-08 throw covers scalars + Tuples (requirement note) | ✓ |
| Throw, as WRT-08 is worded | DataException for containers too | |
| Keep today's split | Coerce with defaults, throw without | |

**Q: Fate of the CR-02 TUPLE early-null branch?**

| Option | Description | Selected |
|--------|-------------|----------|
| Scope like MAP | isNullable → null marker, else DataException; re-pin under 2026-10-07 approval | ✓ |
| Always throw | Replace branch with DataException | |
| Coerce to default tuple | Write per-field defaults | |

**Q: Unify the top-level RuntimeException on DataException?**

| Option | Description | Selected |
|--------|-------------|----------|
| Leave top level as is | Only nested gets DataException; upstream lines untouched | ✓ |
| Unify on DataException | Edit upstream top-level throws | |

**Q: What should the DataException message contain?**

| Option | Description | Selected |
|--------|-------------|----------|
| Column path, no values | Column name, type, slot kind/path; no keys/values | ✓ |
| Column name only | Minimal | |
| Path incl. index/key | Map keys are record data | |

---

## Small-fix depth

**Q: P9-IN-01 — how far should the fix go?**

| Option | Description | Selected |
|--------|-------------|----------|
| Swap + honest docs | Swap, Mockito order pin, correct the null-safety claim, JSONP-08 note | ✓ |
| Make it truly null-safe | Add csc == null short-circuit | |
| Swap only | One-line swap, docs unchanged | |

**Q: DEBT-04 — tearDown() drop failure?**

| Option | Description | Selected |
|--------|-------------|----------|
| Rethrow, class goes red | Wrap with class + database name, rethrow from @AfterAll | ✓ |
| Log with attribution only | Non-fatal ERROR log | |
| Defer to next class | Fail next class's setup naming the leaker | |

**Q: DEBT-01 — HeaderToValue with a non-STRING existing field?**

| Option | Description | Selected |
|--------|-------------|----------|
| Pin current behaviour | Assert DataException (schema path) and String overwrite (schemaless) | ✓ |
| Fix: skip mismatched header | Keep original value, WARN | |
| Fix: fail at configure() | Not feasible — types known per record | |

---

## Claude's Discretion

- Spike shape and placement (research task vs. first plan's RED step)
- Where the route-scoped conversion lives, without changing `DataJson.OBJECT_MAPPER` for other routes
- Decimal encoding mechanics and DateTime64 precisions other than 3 (governed by parity)
- JSONP-07 null-semantics pins and the `jsonInsertColumns`/`bypassFieldCleanup` interaction pins
- R-01 test flip naming; Docker-free routing unit test
- Plan/wave breakdown and commit sequence
- Where behaviour-change notes accumulate before Phase 15
- Test shapes for TYPE-08, DESC-05, COLS-07, CFG-04

## Deferred Ideas

- Logical-type fix on upstream's `bypassRowBinary=true` route (possible upstream PR)
- Unifying top-level null failures on DataException (upstream change)
- Per-record isolation of nested-null failures (forbidden by exactly-once batch invariant)
- HeaderToValue fix for non-STRING target fields
