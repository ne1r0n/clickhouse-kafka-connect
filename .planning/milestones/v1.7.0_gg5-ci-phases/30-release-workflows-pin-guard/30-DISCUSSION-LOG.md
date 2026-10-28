# Phase 30: Release Workflows & Pin Guard - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-24
**Phase:** 30-Release Workflows & Pin Guard
**Areas discussed:** Dry-run mechanism, Who creates the tag, increment-version.yml defect, Dependabot + guard scope

---

## Dry-run mechanism

| Option | Description | Selected |
|--------|-------------|----------|
| dry_run input → draft + delete | Draft release with asset, deleted in always() step | ✓ |
| dry_run input → stop before publish | Skip `gh release create`; attach not exercised | |
| Separate dry-run job | Duplicates steps | |

**User's choice:** draft + delete. `dry_run` default false (alternative: true). Real publish only on `custom_build` (alternative: any ref).

---

## Who creates the tag

| Option | Description | Selected |
|--------|-------------|----------|
| gh release create | Delete git tag/push step | ✓ |
| Explicit git tag + push | Keep persisted credentials | |

**User's choice:** gh release create. Workflow enforces green `tests.yaml` on the SHA before a real publish (alternative: operator rule only). Per-job least privilege (alternative: keep top-level contents: write).

---

## increment-version.yml defect

| Option | Description | Selected |
|--------|-------------|----------|
| Pin only, record defect | | ✓ |
| Fix it here | | |
| Retire the workflow | | |

**User's choice:** Pin only, record the defect. Node: current LTS via pinned setup-node (alternative: keep Node 16).

---

## Dependabot + guard scope

| Option | Description | Selected |
|--------|-------------|----------|
| Sign off exclude-paths edit | | ✓ |
| Leave exclude-paths | | |

**User's choice:** Sign off. Guard: new class under ci/ (alternative: extend existing). Composites: not read (alternative: include).

---

## Claude's Discretion

Exact action versions, Actions API query shape, guard fixture layout.

## Deferred Ideas

Fixing/retiring `increment-version.yml`.
