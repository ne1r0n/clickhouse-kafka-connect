# Phase 19: Nullable Enum & Tuple Mapping - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-12
**Phase:** 19-nullable-enum-tuple-mapping
**Areas discussed:** Go/no-go gate shape, Spike artifact & reuse, WARN lifecycle, IN-01..IN-05 appetite

---

## Go/no-go gate shape

| Option | Description | Selected |
|--------|-------------|----------|
| Blocking-human gate | Spike presents matrix results + recommendation; user answers go/no-go directly (milestone rule: no relayed approvals) | ✓ |
| Claude decides by criteria | Written go criteria locked now; executor applies and records the verdict | |
| Hybrid: auto-go, human no-go | All-green cells auto-go; any red or ambiguity escalates | |

**User's choice:** Blocking-human gate (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Binary only | Go = ROADMAP criterion 3 in full (Nullable(Tuple) AND Array(Nullable(Tuple)) round-trip); anything less is no-go | ✓ |
| Partial go allowed | Approve fixing only the safe shape, WARN narrowed to remaining broken shapes | |

**User's choice:** Binary only (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Between plans | Spike is its own plan; orchestrator asks at the plan boundary, then dispatches Tuple-fix or Known-cost plan | ✓ |
| Checkpoint inside one plan | One plan with a blocking checkpoint mid-plan | |

**User's choice:** Between plans (recommended) — Phase 16 precedent: executors refuse relayed approvals, so human gates run orchestrator-inline.

| Option | Description | Selected |
|--------|-------------|----------|
| Matrix + approach + cost | Per-cell results on both twins, SELECT version(), parsing approach, estimated diff shape, one-line recommendation | ✓ |
| ROADMAP minimum | Exactly what criterion 1 requires | |
| Extended risk bundle | Plus dedup-token/retry analysis and draft rollout wording for both outcomes | |

**User's choice:** Matrix + approach + cost (recommended)

---

## Spike artifact & reuse

| Option | Description | Selected |
|--------|-------------|----------|
| Committed live test | JUnit matrix committed with the spike plan; pre-fix assertions = seen-red; flips to COLS-10 coverage on go; stays as pinned failure record on no-go | ✓ |
| Throwaway script + SPIKE.md | Scratch script, results in planning docs only | |
| clickhouse-local probe first | SQL wire probing before Java, then a committed test | |

**User's choice:** Committed live test (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| 19-SPIKE.md in phase dir | One committed bundle document, Phase 16/17 approval-bundle shape | ✓ |
| Split: results in test, verdict in STATE | Matrix in assertions only; verdict in STATE.md/ROADMAP | |

**User's choice:** 19-SPIKE.md in phase dir (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| ROADMAP cells + informational extras | Required cells decide; single- vs multi-field tuple, Map(K, Nullable(Tuple)), Tuple-in-Tuple measured but cannot veto | ✓ |
| ROADMAP cells only | Exactly the required matrix | |
| Extras can veto too | Extended matrix all counts toward go/no-go | |

**User's choice:** ROADMAP cells + informational extras (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Both sides per cell | (a) current DESCRIBE-derived broken behaviour and (b) hand-built correctly-nullable Column round-trips — (b) is the go evidence | ✓ |
| Broken side only | Documents today's failure; fix feasibility judged from docs/code reading | |

**User's choice:** Both sides per cell (recommended)

---

## WARN lifecycle

| Option | Description | Selected |
|--------|-------------|----------|
| Narrow to Tuple-only at the Enum-fix commit | Predicate, text, anchors row and FORK-DELTA rewritten together; WARN never stale at any commit | ✓ |
| Narrow only at the Tuple outcome | One rewrite later, but interim commits ship a WARN that lies about Enum | |

**User's choice:** Narrow to Tuple-only then (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Delete it all on go | Predicate, dedup set, constants, both describe hooks removed in the Tuple-fix commit with anchors/FORK-DELTA rows | ✓ |
| Keep as defensive tripwire | Keep machinery for hypothetical future shapes | |

**User's choice:** Delete it all (recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Flip to negative pins | Tests assert no WARN fires and Column carries correct nullability, via the Phase 18 log-capture helper; written rationale per flip | ✓ |
| Delete with written rationale | Tests go with the machinery — first deleted-test precedent | |
| Keep predicate tests as unit-only | Dead code kept so no test dies | |

**User's choice:** Flip to negative pins (recommended)

---

## IN-01..IN-05 appetite

| Option | Description | Selected |
|--------|-------------|----------|
| Fix what the phase touches | On go: all five moot/superseded. On no-go: IN-01 route wording + IN-04 text pin + IN-03 delimiter fixed in the lines being rewritten; IN-02/IN-05 closed with reasons | ✓ |
| Close all five with reasons | No extra code either way | |
| Fix everything incl. IN-02 | Also V1-path hook coverage the project target excludes | |

**User's choice:** Fix what the phase touches (recommended)

---

## Claude's Discretion

- Spike test class name, placement, and cell parameterisation
- Narrowed predicate/constant naming (anchors row moves in the same commit)
- Exact element-nullability parsing approach (proposed at the gate from spike evidence)
- Plan split beyond the fixed phase order (gate must fall between plans)
- Wording of the narrowed WARN and flipped-pin rationales, within metadata-only rules

## Deferred Ideas

- IN-02 V1 describe-hook coverage — belongs to a milestone targeting client V1
- Repairing rows already stored wrongly by 999.1 — operator data, rollout note only (REQUIREMENTS Out of Scope)
- Fixing Map(K, Nullable(Tuple)) / Tuple-in-Tuple beyond criterion 3 — future phase if spike evidence warrants
