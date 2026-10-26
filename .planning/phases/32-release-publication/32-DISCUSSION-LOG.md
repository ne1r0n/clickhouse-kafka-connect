# Phase 32: Release & Publication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-26
**Phase:** 32-release-publication
**Areas discussed:** Tag vs GitHub release, Rollout note shape, CHANGELOG & FORK-DELTA scope, Gate order & nightly handoff

---

## Tag vs GitHub release

| Option | Description | Selected |
|--------|-------------|----------|
| Hand-pushed tags only | Phase 29 recipe, no GitHub release | ✓ |
| Tags + real release.yaml dispatch | Publishes a release with the archive | |
| Tags + pre-release.yaml dispatch | Prerelease with the archive | |

**User's choice:** Hand-pushed tags only; `pre-` tag keeps the Phase 29 meaning.

## Rollout note shape

| Option | Description | Selected |
|--------|-------------|----------|
| One page, five claims | Only the required claims with measured output | ✓ |
| Mirror ROLLOUT-v1.7.0_gg4 | Full prior layout, trimmed | |

**User's choice:** One page, five claims; review rounds stay in phase REVIEW files.

## CHANGELOG & FORK-DELTA scope

**User's choice:** Workflow-only CHANGELOG section that states no connector code change; one CI/workflows group in FORK-DELTA.

## Gate order & nightly handoff

**User's choice:** Phase 29 order with one direct approval; a red nightly after tags is a Phase 31 gap and the tags stay.

## Claude's Discretion

Layout of review/evidence records, round count, wording.

## Deferred Ideas

A real release.yaml / pre-release.yaml dispatch producing a GitHub release.
