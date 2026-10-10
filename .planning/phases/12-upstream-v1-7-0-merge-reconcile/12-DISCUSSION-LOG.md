# Phase 12: Upstream v1.7.0 Merge & Reconcile - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-07
**Phase:** 12-upstream-v1-7-0-merge-reconcile
**Areas discussed:** Branch & commit shape, Checklist & invariant script, Compression evidence, EOS-01 restart test

---

## Branch & commit shape

**Branch base**

| Option | Description | Selected |
|--------|-------------|----------|
| sync/v1.6.0 HEAD | Same code as c61d90ce. Carries the 18 planning commits, so the final `--no-ff` merge also publishes the planning history (PUB-04) | ✓ |
| custom_build (c61d90ce) | Clean code-only base. Planning history would need a separate merge, and the planning docs would live on a different branch | |
| You decide | Claude picks | |

**Reconcile granularity**

| Option | Description | Selected |
|--------|-------------|----------|
| One commit per concern | WRT / CMP / BLD / EOS / docs, each reviewable and revertible on its own | ✓ |
| One reconcile commit | Research's shape: merge commit plus one by-intent commit | |
| You decide | Planner decides | |

**VERSION during Phases 12–14**

| Option | Description | Selected |
|--------|-------------|----------|
| Keep fork's v1.6.0_gg1 | PUB-01 bumps it in Phase 15. JMX never claims an unpublished release | ✓ |
| Bump to v1.7.0_gg1 in a reconcile commit | Target version reported from Phase 12 onwards | |

**Merge-commit gate**

| Option | Description | Selected |
|--------|-------------|----------|
| Compiles + testUnit green | D-17 hunks resolved to upstream inside the merge; bisect stays usable | ✓ |
| Compiles only | Unit failures allowed until the reconcile commits | |
| No gate on the merge SHA | Like v1.6.0's commit 1 | |

---

## Checklist & invariant script

**Checklist location**

| Option | Description | Selected |
|--------|-------------|----------|
| New sibling file | `.planning/MERGE-CHECKLIST-v1.7.0.md`. The v1.6.0 file stays for FORK-DELTA's citations. Recorded Facts continue from 17 | ✓ |
| Append to existing file | One registry, but it outgrows its v1.6.0-specific intro | |
| Archive old, new MERGE-CHECKLIST.md | Breaks FORK-DELTA's Recorded Fact and Section 1 links | |

**Anchor list source**

| Option | Description | Selected |
|--------|-------------|----------|
| Committed anchors file | `.planning/scripts/fork-anchors.txt` (path, fixed string, count, FORK-DELTA row) | ✓ |
| Parse FORK-DELTA.md | Breaks on markdown edits; checks names only | |
| Hard-coded in the script | Anchor list hidden in bash | |

**Status gate**

| Option | Description | Selected |
|--------|-------------|----------|
| Yes: status column, Phase 15 asserts zero pending | Same contract as v1.6.0 | ✓ |
| No: Phase 12 closes it fully | One-shot record | |

**Script cadence**

| Option | Description | Selected |
|--------|-------------|----------|
| End of every phase 12–15 | Commits that change an anchor update `fork-anchors.txt` in the same commit | ✓ |
| Phase 12 and Phase 15 only | Phase 13 drift shows up only at publication | |

---

## Compression evidence

**Test class shape**

| Option | Description | Selected |
|--------|-------------|----------|
| Keep names, extend in place | All 9 existing cases stay; new CMP-02/03 cases are added alongside | ✓ |
| New SetCompressionMethod*Test classes | The contract ends up split across four files | |
| Rename D-17 classes | Churns FORK-DELTA, anchor and checklist references | |

**Stripped-config experiment**

| Option | Description | Selected |
|--------|-------------|----------|
| One-off probe, recorded fact | Recorded Fact plus a FORK-DELTA sentence; no brittle committed test | ✓ |
| Committed negative canary | Goes red whenever the server or client drifts | |

**If the flag=false live test fails**

| Option | Description | Selected |
|--------|-------------|----------|
| Stop at a blocking checkpoint | Upstream's fix doesn't cover what was assumed, so the user decides | ✓ |
| Record it as a rollout fact and continue | Quietly changes what CMP-03 means | |

**CMP-03 live path**

| Option | Description | Selected |
|--------|-------------|----------|
| Full sink task on V2 | Exercises the writer's main client and its temporary client | ✓ |
| Helper client, V1 and V2 | Skips the writer's builder sites | |
| Both | More live tests | |

---

## EOS-01 restart test

**Coverage**

| Option | Description | Selected |
|--------|-------------|----------|
| Write→new instance→read, flag × 2, plus NONE | BEFORE→AFTER with distinct offsets, then a fresh key; V2 config | ✓ |
| Minimal: one write, one restart read | Literal wording only | |
| Full matrix incl. client V1 | Read path is V1 anyway | |

**Mutation proof**

| Option | Description | Selected |
|--------|-------------|----------|
| Temporary revert, evidence in SUMMARY | Never committed; KeeperStateProvider stays byte-identical to upstream | ✓ |
| Committed mutation harness | Adds fork code to a high-risk file | |

**If the revert doesn't go red**

| Option | Description | Selected |
|--------|-------------|----------|
| Time-boxed hunt, then checkpoint | Look for a discriminating condition; if none, the user decides on rewording EOS-01 | ✓ |
| Record and accept as regression pin | Mark the mutation clause not reproducible | |
| Stop immediately | No hunt | |

---

## Claude's Discretion

- How the line-provenance check is implemented, the `fork-anchors.txt` format and the initial anchor set
- Script parameter defaults (it must still re-check the v1.6.0 merge)
- Extending `NewUpstreamConfigDefaultsUnitTest` with `setCompressionMethod` (14th option, plus the alias)
- WRT-09 rationale comment wording
- Plan and wave breakdown, and new test class names

## Deferred Ideas

None. Two extra gray areas were offered (the WRT-06/09 re-pin scope, and the line between Phase 12's MAP guard and Phase 13's WR-01/WR-02) and the user moved straight to writing the context.
