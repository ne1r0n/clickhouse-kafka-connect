# Phase 23: Declared-Nullability Hardening & Review Closure - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-20
**Phase:** 23-declared-nullability-hardening-review-closure
**Areas discussed:** Unpaired element (IN-04), Null-safety outcome, Backslash-n fix (COLS-13), Shared literal & sentinel

Finding raised before the area selection (from code scouting): `ClickHouseHelperClient.shouldSkipDescribeRow` drops every DESCRIBE sub-column row whose name ends in `.size`, `.size0` or `.null`. So a Tuple element *named* `size`, `size0` or `null` probably never becomes a Column. This was read in the code and not confirmed live. If it holds, it already meets IN-04's re-open trigger.

---

## Unpaired element (IN-04)

| Option | Description | Selected |
|--------|-------------|----------|
| WARN, still apply (Recommended) | One metadata-only WARN naming the root, the table and the unpaired element names. The paired elements still get their flags and `apply` returns true. `missingDeclaredElementDoesNotShiftPairing` stays green unchanged. Needs a new WARN constant | ✓ |
| Keep today's behaviour | Silent, with the written reason that pairing completeness belongs to discovery and Table | |
| WARN and fall back | Return false and keep DESCRIBE's nullability. The pinned test goes red and needs a flip, which amends SC3 | |
| Checkpoint after research | Research confirms the `.size` shape live first, then the user decides at a blocking checkpoint | |

**User's choice:** WARN, still apply

| Option | Description | Selected |
|--------|-------------|----------|
| Confirm live, then backlog | Research confirms. Today's behaviour is pinned as a known defect and the new WARN fires for it. Backlog 999.4 records the fix, and `ClickHouseHelperClient` is not touched | ✓ |
| Confirm live, fix in 23 | Narrow the skip in this phase, with FORK-DELTA, anchors, a live round-trip test and a DESC-05 regression check | |
| Confirm, then checkpoint | Research sizes the fix, and the user decides at a blocking checkpoint | |

**User's choice:** Confirm live, then backlog

| Option | Description | Selected |
|--------|-------------|----------|
| Every DESCRIBE (Recommended) | Same as `FALLBACK_WARNING`: no dedup, no state | ✓ |
| Once per column+type | A static dedup set, as Phase 19 deleted | |

**User's choice:** Every DESCRIBE

| Option | Description | Selected |
|--------|-------------|----------|
| Docker-free + evidence (Rec.) | Docker-free test over the live-captured rows through the skip and `buildDescribedTable`. The insert outcome goes into evidence and the 999.4 entry | ✓ |
| Also a live insert pin | A permanent live test that pins today's insert outcome as the known defect | |

**User's choice:** Docker-free + evidence

---

## Null-safety outcome

| Option | Description | Selected |
|--------|-------------|----------|
| Mismatch WARN, false (Rec.) | A null-type Column gets one `structure mismatch` WARN and false. The flipped test asserts that reason instead of NPE | ✓ |
| Silent false | No WARN. The flipped test asserts zero log events | |

**User's choice:** Mismatch WARN, false

| Option | Description | Selected |
|--------|-------------|----------|
| Silent false (Recommended) | A null root returns false before the try, with no WARN, matching `declaredType == null` | ✓ |
| WARN naming the table | One WARN with column `null` | |

**User's choice:** Silent false

| Option | Description | Selected |
|--------|-------------|----------|
| Every null in the tree (Rec.) | Null type, null tupleFields entry and null names all count as a structure mismatch, so the catch is left for genuinely unexpected failures | ✓ |
| Null type only | Only what WR-01 named | |

**User's choice:** Every null in the tree

| Option | Description | Selected |
|--------|-------------|----------|
| Unparsable type, class derived (Recommended) | A new test learns the class `ClickHouseColumn.of` throws and asserts the WARN ends with it | ✓ |
| Existing test is enough | Rely on `unparsableDeclaredTypeFailsOpenWithOneMetadataWarning` as it is | |

**User's choice:** Unparsable type, class derived

---

## Backslash-n fix (COLS-13)

| Option | Description | Selected |
|--------|-------------|----------|
| Quote-aware scan (Rec.) | Replace only outside backtick and single-quoted spans | ✓ |
| Position rule (review's) | Replace only after `(`/`,` or before `)` | |

**User's choice:** Quote-aware scan

| Option | Description | Selected |
|--------|-------------|----------|
| Permanent live premise (Rec.) | A live case asserts the raw type equals the Docker-free literal and round-trips one row | ✓ |
| One-off capture in evidence | Captured once, copied into the Docker-free pin | |

**User's choice:** Permanent live premise

| Option | Description | Selected |
|--------|-------------|----------|
| Stop and ask (Rec.) | A blocking checkpoint with the captured bytes | ✓ |
| Fall back to a pin | Keep the blind replace and document the fallback | |

**User's choice:** Stop and ask

---

## Shared literal & sentinel

| Option | Description | Selected |
|--------|-------------|----------|
| Production, made public (Rec.) | `DeclaredNullability.ESCAPED_NEWLINE` public and the only definition. The live premise is the independent oracle | ✓ |
| Test-side public constant | A test helper constant, independent of production | |

**User's choice:** Production, made public

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated WARN root (Rec.) | A sentinel root used only by WARN-text tests, asserted in delimited form | ✓ |
| Rename ROOT everywhere | Rewrite every `c` row | |

**User's choice:** Dedicated WARN root

| Option | Description | Selected |
|--------|-------------|----------|
| Both mutations (Rec.) | Argument dropped, and placeholder dropped, each seen red | ✓ |
| Argument dropped only | The roadmap's literal reading | |

**User's choice:** Both mutations

---

## Claude's Discretion

- The new WARN's constant name and wording; the scanner's structure; the sentinel name; a sibling test vs added assertions for the unpaired WARN (preference: sibling); where the live premise lands; the D-04 helper shape; the plan split; the evidence layout; the wording of disposition pointers.
- DEBT-12 mechanics were proposed in the closing summary (17 WR-01/IN-01/IN-02 `fixed` from Phase 21, IN-03 `skipped` with the roadmap's reason, the 19 rows `fixed` from this phase). The user chose "I'm ready for context" without discussing them further.

## Deferred Ideas

- Backlog 999.4: fix the `.size`/`.size0`/`.null` suffix skip for Tuple elements of those names (only if D-03 confirms it).
- 21-REVIEW and 22-REVIEW open findings: outside DEBT-12's scope, so for Phase 24 or the milestone close.
