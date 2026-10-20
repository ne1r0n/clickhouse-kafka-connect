# Phase 24: Release & Publication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-20
**Phase:** 24-release-publication
**Areas discussed:** Known-defect disclosure, Open review findings, Rollback target wording, Dependabot release window

Carried forward without re-asking: the Phase 20 publication recipe and merge checks, the rollback tag on the pre-merge `custom_build` tip, direct approvals given orchestrator-inline, the delta-note shape (20-D-01), the writing disciplines (20-D-03), the review shape (20-D-07), the CHANGELOG shape (20-D-10) and the milestone audit before close (20-D-12). State checked during the discussion: `origin/custom_build` = `6473e24f` (left count 0); `upstream/main` 8 commits past `v1.7.0`, no new tag.

---

## Known-defect disclosure

### 999.4 coverage

| Option | Description | Selected |
|--------|-------------|----------|
| Rollout item + query | Rollout item with a live-proven system.columns query, tied to the new WARN; CHANGELOG Known-issue line; FORK-DELTA Known-costs item | ✓ |
| Inside the WARN item only | The L-51 item explains the WARN marks a 999.4 table; no query, no CHANGELOG line; FORK-DELTA item still added | |
| FORK-DELTA only | Pre-existing and unchanged by gg3, so Known costs only; the note mentions only the WARN text | |

**User's choice:** Rollout item + query

### The 22-WR-01 residual and the headline claim

| Option | Description | Selected |
|--------|-------------|----------|
| Measure, then state | Live probe on latest × V2 of whether Jackson text names a Map key; word the exception on the result; FORK-DELTA item 6 drops "not measured" | ✓ |
| State it as unmeasured | Explicit exception for the opt-in route, worded like FORK-DELTA item 6, plus a config self-check; no probe | |
| No caveat in the note | Claim without the exception; residual left to FORK-DELTA; overclaims for that route | |

**User's choice:** Measure, then state

### Probe form and contingency

| Option | Description | Selected |
|--------|-------------|----------|
| Committed test + stop-ask | Committed live JUnit test; a Map key is stated and stays accepted; anything beyond a key stops and asks | ✓ |
| Evidence transcript + stop-ask | One-off live run recorded in command evidence, no new test; same stop-and-ask | |
| Committed test, no stop | Committed test; the note states whatever is measured; residual stays accepted regardless | |

**User's choice:** Committed test + stop-ask

### 999.5 coverage

| Option | Description | Selected |
|--------|-------------|----------|
| FORK-DELTA + short note | Known-costs item plus a brief known-limitation paragraph beside the COLS-13 item, with the doubled-form workaround and a live-proven query | ✓ |
| FORK-DELTA only | Known-costs item only | |
| Full item like 999.4 | Own rollout item with a query, plus a CHANGELOG Known-issue line | |

**User's choice:** FORK-DELTA + short note

### 999.4 "What to do"

| Option | Description | Selected |
|--------|-------------|----------|
| No workaround, say so | No fix in this release, inserts fail with code 33 regardless, the fix is a tracked backlog item; no untested schema advice | ✓ |
| Live-proven schema workaround | Research and prove a schema change (new column, backfill) and give it as the action | |
| Workaround as an idea only | Mention renaming the element as an untested suggestion, clearly marked | |

**User's choice:** No workaround, say so

---

## Open review findings

| Option | Description | Selected |
|--------|-------------|----------|
| Docs only, gg2 rule | Fix 21-IN-03 in FORK-DELTA, close 23-IN-04 via the 999.4 item, carry the other 17 to the close record (20-D-08) | ✓ |
| Docs + test warnings | Also fix 23-WR-01, 23-WR-02 and 21-WR-01..03 before the suite gate; infos carried | |
| Docs + all test-only | Fix all 10 test-only findings before the gate; jar unchanged | |
| All except 22-WR-01 | Also src/main and build changes, with new ledger rows, anchors and rollout text | |

**User's choice:** Docs only, gg2 rule
**Notes:** Carried findings each get an owner and a re-open trigger in the close record, as in gg2 (stated in the follow-up prompt, user moved on).

---

## Rollback target wording

### Artifact to redeploy

| Option | Description | Selected |
|--------|-------------|----------|
| Published v1.7.0_gg2 | Redeploy the artifact built from tag v1.7.0_gg2 (a246ca01); the note says pre-v1.7.0_gg3 differs only by the three Dependabot bumps | ✓ |
| Build of pre-v1.7.0_gg3 | Redeploy a jar built from 6473e24f, matching the branch rollback exactly | |
| Either, named both ways | Name both, say they differ only in the bundled slf4j-reload4j and Lombok output | |

**User's choice:** Published v1.7.0_gg2

### Runtime effect of the bumps

| Option | Description | Selected |
|--------|-------------|----------|
| Measured artifact diff | Build the artifact at v1.7.0_gg2 and at the gg3 tip and diff it; name changed bundled jars and any Lombok class-file change; re-establishes BLD-14 (21-IN-03) | ✓ |
| Stated from build config | Cite compileOnly / build-only / implementation from the Gradle configuration | |
| Bundled-jar list only | Compare the bundled dependency list only | |

**User's choice:** Measured artifact diff

### In-flight batches

| Option | Description | Selected |
|--------|-------------|----------|
| Research decides, state it | Research checks whether any landed batch encodes differently between releases; the note states the result, and reuses gg2 item 1 only for a shape that differs | ✓ |
| Always clean stop | Recommend gg2 item 1's clean stop and self-check for every upgrade and rollback | |
| Omit the topic | Mention only if research finds a byte difference | |

**User's choice:** Research decides, state it

---

## Dependabot release window

### Merges on origin during the phase

| Option | Description | Selected |
|--------|-------------|----------|
| Freeze until tag | No Dependabot merge on origin until the tag is pushed; the pre-gate fetch still checks 6473e24f | ✓ |
| Absorb as they come | Each merge gets a DEPS-01-style merge and gate re-runs | |
| Ship some in gg3 | Merge chosen PRs (e.g. #6-#8) before the gate so gg3 ships them | |

**User's choice:** Freeze until tag

### Post-publication manual steps

| Option | Description | Selected |
|--------|-------------|----------|
| Just a reminder | The close record lists the open user items with exact steps; nothing waits on answers | ✓ |
| Present and record | As in gg2: present the steps, wait for answers in the user's own words, record them | |
| Nothing | No post-publication steps; items stay in PROJECT.md Active | |

**User's choice:** Just a reminder

---

## Claude's Discretion

- The plan split, with the publication plan orchestrator-inline.
- Item order, titles and TOC of the gg3 note; how the error-text example is captured.
- Exact SQL of the 999.4 and 999.5 detection queries and their look-alike fixtures.
- The D-03 test's name, placement and sentinel design.
- Placement of the CHANGELOG Known-issue line.
- The merge commit message, the approval-bundle contents, the close record's name and layout, and the scripted merge checks and artifact diff.

## Deferred Ideas

- The 17 carried review findings (close record, future milestone).
- The 999.4 fix and the 999.5 name-form decision (backlog).
- Dependabot `ignore` entries for the fork-only pins, the `dependencies` label (user actions, reminded only).
- Shipping Dependabot PRs #6-#8 (user decision after the freeze).
- The next upstream sync once upstream tags past `v1.7.0`.
