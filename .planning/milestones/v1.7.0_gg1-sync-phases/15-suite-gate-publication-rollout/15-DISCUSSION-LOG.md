# Phase 15: Suite Gate, Publication & Rollout - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-10
**Phase:** 15-suite-gate-publication-rollout
**Areas discussed:** Open findings vs. publish, Rollout checklist shape, Pre-approval audits, Publish mechanics

---

## Open findings vs. publish

**13-REVIEW WR-02:** Debezium Timestamp milliseconds are stored as seconds on the metadata-free JSON route.

| Option | Description | Selected |
|--------|-------------|----------|
| Fix it now | Add `io.debezium.time.Timestamp`/`NanoTimestamp` to `writeLogicalValue`, flip the pass-through pins to typed read-back, mutation check; lands before the gate | ✓ |
| Document + runtime WARN | One-time WARN, documented in FORK-DELTA and the rollout doc; the data is still wrong | |
| Document only | No code change (Phase 11 D-08 shape) | |

**13-REVIEW WR-05:** nested nulls in `Nullable(Enum)`/`Nullable(Tuple)` slots now fail the batch.

| Option | Description | Selected |
|--------|-------------|----------|
| Document + startup WARN | WARN at table-mapping time naming affected columns; root cause stays in 999.1 | ✓ |
| Fix root cause now (pull 999.1 in) | `extractColumn` nullability plus writer marker handling, with live Enum/Tuple tests | |
| Document only | Rollout doc asks operators to check their schemas | |

**Other open findings:** 13 WR-01/03/04, 14 WR-01/02, and the info items.

| Option | Description | Selected |
|--------|-------------|----------|
| Publish open, set dispositions | Mark `deferred` with reasons; operator-visible ones go in FORK-DELTA Known costs | ✓ |
| Also fix the test/CI-only ones | Fix 14 WR-01/WR-02 too | |
| Fix WR-03 and WR-04 too | Edit upstream lines in `ClickHouseWriter` | |

## Rollout checklist shape

**Location:**

| Option | Description | Selected |
|--------|-------------|----------|
| `docs/ROLLOUT-v1.7.0_gg1.md` | Its own per-release tracked file, linked from CHANGELOG and FORK-DELTA | ✓ |
| Section in FORK-DELTA.md | One document, two audiences | |
| `.planning/` only | Kept out of the public tree | |

**Audience:**

| Option | Description | Selected |
|--------|-------------|----------|
| Generic + self-check steps | Concrete checks per item, no internal names | ✓ |
| Generic doc + private run-sheet | Plus a deployment-specific `.planning` run-sheet (still published with `.planning` history) | |
| Written for my deployment | Names real connectors and versions in `docs/` | |

**Exactly-once upgrade step:**

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, add a pre-upgrade check | Drain `BEFORE_PROCESSING` state before swapping jars; research which encodings changed | ✓ |
| Only the R-01 backfill | Keep to ROLL-01's literal list | |

**CHANGELOG shape:**

| Option | Description | Selected |
|--------|-------------|----------|
| `1.7.0_gg1` only, upstream style | New top section, fork changes since `v1.6.0_gg1`, links | ✓ |
| Also backfill `1.6.0_gg1` | Add a section for the previous release | |
| Fork-prefixed headings | "Fork changes" headings | |

## Pre-approval audits

**Security audits:**

| Option | Description | Selected |
|--------|-------------|----------|
| 13 and 14 | `/gsd-secure-phase` for both; an open high-severity threat becomes a blocking checkpoint | ✓ |
| 13 only | Production-code phase only | |
| Neither | Record the omission | |

**Ordering:**

| Option | Description | Selected |
|--------|-------------|----------|
| Code → review → docs/VERSION → gate | The gate runs on the exact published tree | ✓ |
| Gate before docs | Faster, but the gated tree is not the published tree | |

## Publish mechanics

**Rehearsal:**

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, push scratch and require green | Phase 11 D-02 shape; one extra CI run | ✓ |
| No, published SHA only | First CI exposure of the new code comes after a one-way push | |

**Planning tail after publication:**

| Option | Description | Selected |
|--------|-------------|----------|
| Leave local, publish next milestone | Same as after `v1.6.0_gg1`; STATE note | ✓ |
| Second planning-only merge after close | Second push, CI run and approval | |
| Keep scratch branch on origin | Contradicts 11 D-04 | |

**Tag push order:**

| Option | Description | Selected |
|--------|-------------|----------|
| Push both, record the extra run | `custom_build` first, wait for its run, then both tags by name; record the `c61d90ce` run | ✓ |
| Push rollback tag first | A tag reaches origin before the approval gate | |

## Claude's Discretion

- Handling `latest` drift between the two runs.
- D-02's WARN wording, dedup mechanism and test style.
- D-01 test placement.
- Commit granularity.
- README/AGENTS.md links.
- Deleting local `sync/v1.6.0`.
- Not triggering the release workflows.

## Deferred Ideas

- Enum/Tuple nullability root cause (backlog 999.1).
- The remaining open review findings (published as `deferred`).
- `ZonedTimestamp` handling on the metadata-free route (documentation only).
- Publishing the post-publication planning tail with the next milestone.
- Backlog 999.2.
- Client `0.10.0`.
