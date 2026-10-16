# Phase 20: Release & Publication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-14
**Phase:** 20-release-publication
**Areas discussed:** gg2 rollout note shape, Dedup-retry evidence, Review gate & open advisories, CHANGELOG & close bookkeeping

---

## gg2 rollout note shape

| Option | Description | Selected |
|--------|-------------|----------|
| Delta note (Recommended) | Covers only gg1 → gg2 changes; opens with a pointer sending pre-gg1 operators through ROLLOUT-v1.7.0_gg1.md first; no duplicated content | ✓ |
| Full standalone | Self-contained like gg1's note; repeats still-relevant gg1 items; duplicated text must stay consistent | |
| Delta + merged summary table | Delta note plus one table of every behaviour change since v1.6.0_gg1 across both releases | |

**User's choice:** Delta note

| Option | Description | Selected |
|--------|-------------|----------|
| Ready SQL per shape (Recommended) | A runnable detection query per broken 999.1 shape, each proven live on `latest` before publication | ✓ |
| Symptoms only | Describe what wrongly-stored rows look like; operators write their own queries | |
| Symptoms + one worked example | One example query operators adapt | |

**User's choice:** Ready SQL per shape
**Notes:** Phase 16 writing disciplines (plain language, every quoted command/SQL run live with transcripts) stated as carried; user did not object.

---

## Dedup-retry evidence

| Option | Description | Selected |
|--------|-------------|----------|
| Fresh live probe (Recommended) | Live demonstration on `latest`: retry the same offset range with changed bytes, observe dedup outcome; SELECT version() recorded | ✓ |
| Cite committed pins | Point at 18-04's byte-identity pins and the 19 twin matrix; nothing demonstrates the cross-upgrade miss directly | |
| Both | Cite pins + fresh probe | |

**User's choice:** Fresh live probe

| Option | Description | Selected |
|--------|-------------|----------|
| Procedure + self-check (Recommended) | Stop connectors cleanly (no BEFORE-state ranges), plus a KeeperMap state-table self-check query before restarting on gg2 | ✓ |
| Consequence + stop advice only | No state-table query | |
| Consequence only | Mitigation left to the operator | |

**User's choice:** Procedure + self-check

| Option | Description | Selected |
|--------|-------------|----------|
| Committed live test (Recommended) | Live JUnit test beside the exactly-once tests, pinning both halves permanently (Phase 19 spike D-05 precedent) | ✓ |
| Scripted one-off session | Reproducible script/SQL session in evidence only, not in the suite | |

**User's choice:** Committed live test

---

## Review gate & open advisories

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 20 commits (Recommended) | gsd-code-reviewer, incremental, over everything Phase 20 commits (docs + VERSION + new live test); Phases 17–19 already reviewed per-phase | ✓ |
| Docs only | Operator-facing docs only; the live test rides the suite gate alone | |
| Full release sweep | Re-review unpublished Phase 17–19 code as one diff against be6b8861 | |

**User's choice:** Phase 20 commits

| Option | Description | Selected |
|--------|-------------|----------|
| Fix docs, carry code (Recommended) | Fix the FORK-DELTA per-twin wording finding in Phase 20; carry code-side warnings and infos to the close record with written reasons | ✓ |
| Carry all six | All six to the close record untouched | |
| Triage and fix all warnings | Fix the two code-side warnings too, re-opening reviewed Phase 19 code | |

**User's choice:** Fix docs, carry code

| Option | Description | Selected |
|--------|-------------|----------|
| Before the merge gate (Recommended) | /gsd-secure-phase 19 as a Phase 20 prerequisite; findings weighable at the approval | ✓ |
| Before planning Phase 20 | Run immediately, strictly sequential bookkeeping | |
| Skip with written reason | Record why no threat check ran for Phase 19 | |

**User's choice:** Before the merge gate

---

## CHANGELOG & close bookkeeping

| Option | Description | Selected |
|--------|-------------|----------|
| Behaviour + CI/build note (Recommended) | gg1's structure from the Phase 18/19 ledgers, plus a short Build/CI subsection (SHA-pinned actions, ubuntu-24.04, read-only token, test-head.yaml deleted, lombok.config) | ✓ |
| Operator-visible only | Behaviour and bug fixes only; CI/build stays in FORK-DELTA and history | |

**User's choice:** Behaviour + CI/build note

| Option | Description | Selected |
|--------|-------------|----------|
| Prompt + record (Recommended) | After the tag, present the three manual checks with steps, wait for answers in-session, record outcomes in the close record | ✓ |
| Hand off to close record | Write them as open operator items; phase completes without waiting | |

**User's choice:** Prompt + record

| Option | Description | Selected |
|--------|-------------|----------|
| Run audit (Recommended) | /gsd-audit-milestone after Phase 20 verification, replacing the stale start-of-milestone snapshot | ✓ |
| Override again | Third override closeout citing per-phase verifications | |
| Decide at close | Note the stale audit file; raise at the close prompt | |

**User's choice:** Run audit

---

## Claude's Discretion

- Plan split; item ordering and titles in the gg2 note; exact detection/self-check SQL (proven live); the dedup test's name/placement/parameterisation; merge commit message and approval-bundle contents; scripted merge checks and FORK-DELTA raw-numbers commands.

## Deferred Ideas

- Five carried 19-REVIEW advisories (two code warnings, three infos) → milestone close record.
- Upgrading the remaining upstream workflows off `@v3`-era actions → REQUIREMENTS Future.
- Repairing rows already stored wrongly by 999.1 → operator data, detection only (Out of Scope).
