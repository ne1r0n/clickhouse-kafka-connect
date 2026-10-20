# Phase 16: Early Docs Publication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-10
**Phase:** 16-early-docs-publication
**Areas discussed:** CHANGELOG correction, Planning IDs in operator text, Review & command proof, Red CI / deadline

---

## CHANGELOG correction

| Option | Description | Selected |
|--------|-------------|----------|
| Edit in place | Rewrite the bullets in the `1.7.0_gg1` section and add the two missing lines; git history keeps the original | ✓ |
| In place + a note | Same edits, plus a dated "wording corrected" note under the heading | |
| Forward only (gg2) | Leave `1.7.0_gg1` byte-identical and correct it in the `1.7.0_gg2` section (PITFALLS 13) | |

| Option | Description | Selected |
|--------|-------------|----------|
| Important Changes | The MBean/client-name line goes here, pointing at rollout item 7 | ✓ |
| Bug Fixes | Less prominent; a poor fit for an intentional change | |
| You decide | Planner places it | |

| Option | Description | Selected |
|--------|-------------|----------|
| Versions + pointer | Name the affected versions and point at FORK-DELTA | ✓ |
| Name the advisories | List the advisory IDs in the CHANGELOG | |
| You decide | Planner picks the wording | |

**User's choice:** Edit in place; MBean line under Important Changes; lz4 reason as versions + pointer.

---

## Planning IDs in operator text

| Option | Description | Selected |
|--------|-------------|----------|
| Plain text + path | Rename the item 2/5 headings, drop bare IDs, keep L-xx as "(ledger L-xx)", give the ledger path once | ✓ |
| Strip all IDs | Remove every ID, L-xx included | |
| Path only | Keep all IDs and add the ledger path to the preface | |

| Option | Description | Selected |
|--------|-------------|----------|
| No, maintainer doc | FORK-DELTA keeps its IDs; only IN-09 changes | ✓ |
| Yes, same rule | Rewrite FORK-DELTA prose too | |

**User's choice:** Plain text + path for the rollout doc; FORK-DELTA exempt.
**Notes:** The scout found that the ledger has moved to `.planning/milestones/v1.7.0_gg1-sync-phases/13-…/13-BEHAVIOUR-LEDGER.md`; IN-10's suggested path is stale.

---

## Review & command proof

| Option | Description | Selected |
|--------|-------------|----------|
| Live SQL if touched | Greps and git commands locally; changed SQL on a live `latest` ClickHouse with KeeperMap | ✓ |
| Re-run all SQL | Re-run every SQL block, changed or not | |
| Shell only | Verify only grep and git commands | |

| Option | Description | Selected |
|--------|-------------|----------|
| Phase evidence file | `16-COMMAND-EVIDENCE.md` in the phase directory | ✓ |
| Plan SUMMARY only | Inline in the plan's SUMMARY.md | |

| Option | Description | Selected |
|--------|-------------|----------|
| Both places | Flip IN-07..IN-10 in the archived 15 disposition, plus a new 16-REVIEW-DISPOSITION | ✓ |
| Phase 16 only | Leave the archive frozen | |

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 16 diff only | Incremental review of the Phase 16 commits, with checks on the WR-01..03 fixes and TRUNCATE | ✓ |
| Full diff vs f328fd21 | Also formally review `505e4981` | |
| Full docs re-review | Review all three documents end to end | |

**User's choice:** All recommended options.

---

## Red CI / deadline

| Option | Description | Selected |
|--------|-------------|----------|
| Attribute, then stop | No blind re-run; attribute, report, re-run only with explicit OK | ✓ |
| Re-run once if flaky | Re-run once without asking for environmental failures | |

| Option | Description | Selected |
|--------|-------------|----------|
| Stop and ask | Don't push past 2026-10-19 without a decision on Ubuntu 26 vs pulling CI-11 forward | ✓ |
| Push anyway | Publish on whatever `ubuntu-latest` is | |

**User's choice:** Attribute, then stop; Stop and ask.

---

## Claude's Discretion

- The exact plain-language titles and CHANGELOG wording
- The scripted form of the allow-list, tree-equality and outside-tree assertions
- The merge commit message and the approval bundle contents (Phase 15 D-12 precedent)
- The plan split

## Deferred Ideas

- A plain-language rewrite of FORK-DELTA (not scheduled)
- A full re-review of all three documents (out of scope)
