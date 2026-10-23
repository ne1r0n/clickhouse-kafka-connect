# Phase 28: Raw Identifier Names in Discovery - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-23
**Phase:** 28-raw-identifier-names-in-discovery
**Areas discussed:** Enum label fix-or-document, Double-quote names, Spike scope & evidence, Doubled-form pins & rollout

---

## Enum label fix-or-document

| Option | Description | Selected |
|--------|-------------|----------|
| Fix in extractEnumValues | Unescape labels inside Column.extractEnumValues only; stop for decision if wider | ✓ |
| Document only | Leave escaped form, record in Known costs and rollout note | |
| Decide after the spike | Measure first, then fix or document | |

**User's choice:** Fix in extractEnumValues (Recommended)

---

## Double-quote names

| Option | Description | Selected |
|--------|-------------|----------|
| Hard requirement | Must map and round-trip; stop and ask if cause is outside the decode | ✓ |
| Fix if cheap, else document | | |
| Pin as unmapped | | |

**User's choice:** Hard requirement (Recommended)

---

## Spike scope & evidence

| Option | Description | Selected |
|--------|-------------|----------|
| Full shapes, V2 live + one V1 run | Root, Tuple, Array(Tuple), Nullable(Tuple), Map-value Tuple, enum label | ✓ |
| Minimal: root, Tuple element, enum | | |

**User's choice:** Full shapes (Recommended)

---

## Doubled-form pins & rollout

| Option | Description | Selected |
|--------|-------------|----------|
| New live test + evidence table | New ClickHouseBase test; record handed to Phase 29 | ✓ |
| Extend NullableTupleMatrixLiveTest | | |

**User's choice:** New live test + evidence table (Recommended)

---

## Claude's Discretion

Decode mechanism, `normalise` shape, mutation-proof form, jacoco class list, plan split.

## Deferred Ideas

NAME-F1 doubled-form WARN; COV-F1 V1 CI coverage; type-text decode.
