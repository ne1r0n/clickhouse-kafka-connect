# Phase 17: CI, Build & Test Hygiene - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-11
**Phase:** 17-ci-build-test-hygiene
**Areas discussed:** test-head.yaml fate, Dependabot for actions, LZ4 response-leg proof, CI-17 push timing

---

## test-head.yaml fate

| Option | Description | Selected |
|--------|-------------|----------|
| Delete the file | It cannot work: it passes inputs `tests.yaml` lacks, and head/main testing is outside latest × V2. Clean FORK-DELTA row; future syncs see a modify/delete conflict | ✓ |
| Keep it, drop the schedule | Remove the cron, keep `workflow_dispatch`; a manual run still fails, so it stays dead code | |
| Fix: add `workflow_call` to `tests.yaml` | Revives head/main testing, against CI-01..04; scope creep | |

**User's choice:** Delete the file

| Option | Description | Selected |
|--------|-------------|----------|
| FORK-DELTA row + MERGE-CHECKLIST line | Document it, plus one checklist line: keep it deleted on modify/delete | ✓ |
| Also an absence check in verify-merge-invariants | Stronger, but new script logic | |
| FORK-DELTA row only | Just document it | |

**User's choice:** FORK-DELTA row + MERGE-CHECKLIST line

---

## Dependabot for actions

| Option | Description | Selected |
|--------|-------------|----------|
| `tests.yaml` only, via `exclude-paths` | Exclude the 4 upstream workflows; research confirms support, fallback recorded in FORK-DELTA | ✓ |
| All workflows, close what we don't want | Plain `directory: "/"` entry; noisy | |

**User's choice:** `tests.yaml` only, via `exclude-paths`

| Option | Description | Selected |
|--------|-------------|----------|
| Monthly, grouped into one PR | Matches the gradle entry; one PR and one CI run | ✓ |
| Monthly, one PR per action | Up to 4 PRs a month | |
| Weekly, grouped | Faster security pickup, more churn | |

**User's choice:** Monthly, grouped into one PR

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, as a `testUnit` JUnit test | Docker-free check of every `uses:` SHA + comment, runner, permissions | ✓ |
| Yes, as an anchors/invariants rule | Runs only at sync/release gates | |
| No guard | Rely on the CI-17 run | |

**User's choice:** Yes, as a `testUnit` JUnit test

| Option | Description | Selected |
|--------|-------------|----------|
| Config only; note the enablement check | Research checks fork enablement; a manual check goes to the Phase 20 operator steps | ✓ |
| Config + you check settings now | The user confirms or enables it during Phase 17 | |

**User's choice:** Config only; note the enablement check

---

## LZ4 response-leg proof

| Option | Description | Selected |
|--------|-------------|----------|
| ProfileEvents first, raw-HTTP as fallback | `NetworkSendBytes` vs `result_bytes` on client-v2's own read query id | ✓ |
| Both checks | ProfileEvents + raw-HTTP `0x82` method byte | |
| Raw-HTTP method byte first | Deterministic but proves server behaviour, not the client's request | |

**User's choice:** ProfileEvents first, raw-HTTP as fallback

| Option | Description | Selected |
|--------|-------------|----------|
| One-off local flip, recorded in evidence | Flip `compressServerResponse(false)`, record the failure, revert | ✓ |
| Committed negative-control test | Standing control, extra ~24 MB read per run | |
| Both | Recorded flip + standing control | |

**User's choice:** One-off local flip, recorded in evidence

| Option | Description | Selected |
|--------|-------------|----------|
| Wide ratio, e.g. wire < result/10 | Robust; LZ4 ~130×, uncompressed ~1× | ✓ |
| Tight, LZ4-shaped bound | Also rejects ZSTD; fragile | |
| You decide | Planner picks from spike measurements | |

**User's choice:** Wide ratio, e.g. wire < result/10
**Notes:** DEBT-07 mechanism (exclude vs substitution) left to the planner, with the recommendation recorded.

---

## CI-17 push timing

| Option | Description | Selected |
|--------|-------------|----------|
| Once, at the end of the phase | One run covers everything; the pinned runner makes 10-19 non-binding | ✓ |
| Early, right after the CI commits | Before 10-19; later commits first reach CI at Phase 20 | |

**User's choice:** Once, at the end of the phase

| Option | Description | Selected |
|--------|-------------|----------|
| `sync/v1.7.0` | D-13 precedent; same name as the Phase 20 rehearsal | ✓ |
| Dedicated `ci/phase17-gg2` | Throwaway name | |

**User's choice:** `sync/v1.7.0`

| Option | Description | Selected |
|--------|-------------|----------|
| Just push when ready | The pinned runner is unaffected by the switch | ✓ |
| Stop and ask | Keep Phase 16 D-12 | |

**User's choice:** Just push when ready

---

## Claude's Discretion

- DEBT-07 mechanism (scoped exclude recommended) and its Docker-free single-`LZ4Factory`-URL pin in a new class
- CI-14 task name and output format
- DEBT-08 row value (confirm against `BigDecimal.valueOf(v, 9)`)
- `tests.yaml` header comment, FORK-DELTA row wording, plan split, commit order, evidence layout

## Deferred Ideas

- Dependabot fork-settings enablement: a manual check in the Phase 20 operator steps
- Upgrading and pinning the other upstream workflows: already a Future Requirement
- An invariant-script absence check for `test-head.yaml`: declined in favour of the checklist line
