---
phase: 20-release-publication
plan: "05"
subsystem: release
tags: [security-audit, code-review, review-disposition, release-docs, threat-model]

requires:
  - phase: 20-release-publication
    provides: "20-03 rollout note and 20-04 CHANGELOG and FORK-DELTA, the text under review"
  - phase: 19-nullable-enum-tuple-mapping
    provides: "34-threat register and the 19-REVIEW findings and dispositions the audit cross-checks"
provides:
  - "19-SECURITY.md: Phase 19 threat verification, threats_open 0 (D-09)"
  - "20-REVIEW.md: pre-gate review of the Phase 20 changes with the eight mandated confirmations (D-07)"
  - "20-REVIEW-DISPOSITION.md: six findings, all fixed, open 0"
  - "Six prose fixes to CHANGELOG, FORK-DELTA and the rollout note, with E-50 recording the re-run checks"
affects: [20-06 suite gate and rehearsal, 20-07 publication, milestone close]

actuals:
  tokens: 9800
  tasks: 3
  commits: 10

plan_head_before: b2bfad1ecd02b8097b705c624ac25c2b913eb087
plan_head_after: 99e826dc4f62b77da7f9f3e8526da9babf82825a

tech-stack:
  added: []
  patterns:
    - "A threat whose mitigation a review finding limits is recorded open, below the block threshold, not closed"
    - "Re-review rounds put new findings before the confirmations section so the section stays last and heading-free"

key-files:
  created:
    - .planning/phases/19-nullable-enum-tuple-mapping/19-SECURITY.md
    - .planning/phases/20-release-publication/20-REVIEW.md
    - .planning/phases/20-release-publication/20-REVIEW-DISPOSITION.md
    - .planning/phases/20-release-publication/20-05-SUMMARY.md
  modified:
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - docs/ROLLOUT-v1.7.0_gg2.md
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md

key-decisions:
  - "The Phase 19 audit ran inline at ASVS level 1 and verified mitigations against the source, not against the plan text; 32 of 34 threats closed, T-19-12 and T-19-26 recorded open (medium, below the high block threshold), so threats_open is 0"
  - "All six review findings were fixed rather than deferred, because every one is wording inside the six reviewed files"

requirements-completed: [ROLL-02, PUB-07]

coverage:
  - id: D1
    description: "19-SECURITY.md exists, is committed and reads threats_open: 0, with every closed threat cross-checked against the 19-REVIEW findings"
    requirement: ROLL-02
    verification:
      - kind: other
        ref: "Task 1 verify command (SECURITY-19-OK)"
        status: pass
    human_judgment: true
    rationale: "Closing T-19-11 (high) in the face of 19-REVIEW IN-03 and IN-04 is an auditor judgment, written out in the SECURITY file for the user to weigh at the approval"
  - id: D2
    description: "20-REVIEW.md reviews exactly the Phase 20 changes with the eight mandated confirmations and findings only under ### ID headings"
    requirement: ROLL-02
    verification:
      - kind: other
        ref: "Task 2 verify command (REVIEW-20-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "20-REVIEW-DISPOSITION.md has one row per finding, none open, every fixed row starting with a resolvable commit"
    requirement: ROLL-02
    verification:
      - kind: other
        ref: "Task 3 verify command 1 (DISPOSITION-20-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Release documents frozen after the fixes: six items, quoted blocks in the evidence, gg1 tail equals P20_BASE, raw numbers equal git, no TRUNCATE instruction, gg1 note and protected paths unchanged"
    requirement: PUB-07
    verification:
      - kind: other
        ref: "Task 3 verify command 2 (DOCS-FROZEN-OK); ROLLOUT-T3-OK, ITEM1-CONTENT-OK, ITEMS2-5-CONTENT-OK, ROLLOUT-FINAL-OK, CHANGELOG-GG2-OK, FORK-DELTA-GG2-OK (E-50)"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 05: Pre-gate security audit and review Summary

**Phase 19 is threat-verified (34 threats, `threats_open: 0`, two medium threats recorded open and non-blocking after a cross-check against 19-REVIEW), and everything Phase 20 committed has been reviewed: six findings (two Warning, four Info), all fixed in prose, `open: 0`, documents frozen.**

## Performance
- **Duration:** about 20 min (start time not recorded; estimated from the session clock)
- **Tasks:** 3 of 3
- **Commits:** 10 (measured from the ledger, `b2bfad1e..99e826dc`)

## Skill adaptation
The executor has no sub-agent tool, so neither skill could spawn its agent. `gsd-secure-phase 19` was invoked and its workflow read; the auditor step was done inline following `~/.claude/gsd-core/agents/gsd-security-auditor.md`. The review was done inline following `~/.claude/gsd-core/agents/gsd-code-reviewer.md` (the skill's file-list validation was not tried; the plan names this fallback, Phase 15/16 precedent). The adversarial stance of the auditor was kept: each mitigation was checked in the source or in a command, not accepted from the plan or a summary.

## Task 1: Phase 19 threat verification (D-09)
`19-SECURITY.md` (commit `b180edc7`): 34 threats from the 19-01 to 19-10 registers, ASVS level 1, `security_block_on: high`, `threats_open: 0`.

Checks run against the tree: `DeclaredNullability.java` read in full (unwrap at the entry of `collect`, no `getMessage` anywhere, WARN carries the class name only); `Processing`, `ProxySinkTask`, `state`, `kafka` and `Table` byte-identical to the Phase 19 base `0f35a4c0`; `ClickHouseWriter.java` differs from that base by one comment edit (19-04); the old Tuple discovery WARN is gone from `src/main/java`; all 59 anchor rows match their counts at HEAD; `verify-merge-invariants.sh 54e63648 HEAD` prints `result=PASS`; the 19-05 SUMMARY reads `superseded`.

**Cross-check against 19-REVIEW (findings compared with the threats they touch):**

| Finding | Threats compared | Outcome |
|---|---|---|
| CR-01 (SimpleAggregateFunction flipped) | T-19-11, T-19-21, T-19-22, T-19-31, T-19-32 | fixed in `4e133d4e`; unwrap present and anchored; closed |
| WR-01 (totality not met on handler and caller) | T-19-26, T-19-12 | contradicts T-19-26's totality claim: recorded open (medium, non-blocking); T-19-12 shares the catch: recorded open |
| WR-02 (vacuous WARN-name assertions) | T-19-13, T-19-27 | WARN content itself correct in source; closed |
| WR-03 (per-twin codes unpinned) | none | doc wording, fixed `3e39867d` |
| IN-01, IN-02 | T-19-29 | no effect on a mitigation; closed |
| IN-03 (normalise rewrites every backslash-n pair) | T-19-11, T-19-12 | designed fail-open with a WARN; T-19-11 stays closed, noted in the file |
| IN-04 (a Tuple Column lacking a declared element still applies) | T-19-11, T-19-12 | pairing check is one-directional; T-19-12 recorded open |

No threat at or above `high` is open. **For the approval bundle:** T-19-12 and T-19-26 are open at medium (both tied to deferred findings WR-01 and IN-04, with reasons and re-open triggers in the milestone close record), and T-19-11 (high) was closed with IN-03 and IN-04 noted as a rare fail-open case; the user can weigh that judgment. A fix would touch `src/main`, outside this phase (D-08).

## Task 2: Review of the Phase 20 changes (D-07)
`20-REVIEW.md` (commit `be5e44b1`): six files, `diff_base d73848807c58`, scope check showed exactly the five expected non-`.planning` paths. Findings:

| ID | Severity | Finding | Fix commit |
|---|---|---|---|
| WR-01 | warning | CHANGELOG called `lombok.config` new (it existed at gg1 with two lines) and the artifact byte-identical (only one class's generated code was compared) | `54474bf5` |
| WR-02 | warning | FORK-DELTA said a serialization change never duplicates rows on a retry; only true for tables with a deduplication window | `32a06bcf` |
| IN-01 | info | the `L-01` to `L-22` sentence pointed at the wrong range once the gg2 paragraph sat in front of it | `e6682d9a` |
| IN-02 | info | the Tuple WARN is logged per affected column, not once per table (CHANGELOG and item 2) | `a15c3887` |
| IN-03 | info | the Known costs preamble said "knowingly open" above items headed "is closed" | `aef8d39d` |
| IN-04 | info | item 1 said "measured" for a gg1 attempt that the test simulates | `02adff03` |

**Mandated confirmations (all eight confirmed after the fixes):** (1) dedup claims match the test and E-01/E-03; (2) every quoted query and command has evidence and the 999.1 queries ran on gg1-written rows; (3) no TRUNCATE or unscoped DELETE instruction; (4) rollback uses `read-tree` with `pre-v1.7.0_gg2` and E-31 proves it; (5) the gg1 CHANGELOG section and gg1 note are unchanged; (6) no secret, hostname, record value or production identifier; (7) note, CHANGELOG and FORK-DELTA agree (this one failed first and produced WR-01, WR-02); (8) the new test follows the AGENTS.md test rules. The four flagged wording points of 20-03 were checked: the 3600 seconds caveat is stated as read, not waited out; the in-flight check scope reads correctly; the rollback mirror image is stated as following from the token rule, not measured with gg1 as the retrying side; "published together with this release" is loose (the tag is pushed first by plan 20-07) but not wrong.

## Task 3: Fixes, dispositions, frozen documents (D-07)
Six prose fixes, one commit each (table above). `E-50` (commit `cf68dd40`) records the re-run of ROLLOUT-T3-OK, ITEM1-CONTENT-OK, ITEMS2-5-CONTENT-OK, ROLLOUT-FINAL-OK, CHANGELOG-GG2-OK and FORK-DELTA-GG2-OK with what each changed sentence rests on. A Critical or Warning fix landed, so round 2 re-reviewed the fix commits only and found nothing new (counts stay 6). `20-REVIEW-DISPOSITION.md` (commit `99e826dc`): six rows, all `fixed`, `open: 0`, `total: 6`. DISPOSITION-20-OK and DOCS-FROZEN-OK both print. No fix touched `src/main`, `build.gradle.kts`, `gradle/`, `.github/`, `VERSION`, the gg1 note or the released CHANGELOG sections; no test changed, so `DedupTokenRetryLiveTest` was not re-run and FORK-DELTA's raw numbers stay 119/18802/529 and 32/2042/143.

## Deviations from Plan

None - plan executed exactly as written. (The `gsd-secure-phase` and `gsd-code-review` skills could not spawn their agents; the plan's inline fallback was used and is recorded above.)

## Issues Encountered
- The first anchor re-check used shell quoting that mangled rows containing quotes and parentheses (false mismatches); a Python re-check of all 59 rows reported 0 bad, and the invariants script printed PASS.
- `verify-merge-invariants.sh 54e63648 HEAD` takes about two minutes (it walks every commit); it ran in the background.

## Next Phase Readiness
- Plan 20-06 can cite `threats_open: 0` (19-SECURITY.md) and `open: 0` (20-REVIEW-DISPOSITION.md) measured from committed files, and should carry the T-19-12, T-19-26 and T-19-11 judgment note into the approval bundle.
- The release documents are frozen; any later change to a non-`.planning` path must happen before the rehearsal push.

## Known Stubs
None.

## Threat Flags
None. The plan changed documents and planning records only.

## Self-Check: PASSED
- `19-SECURITY.md`, `20-REVIEW.md` and `20-REVIEW-DISPOSITION.md` exist and are tracked.
- `git rev-list --count b2bfad1e..HEAD` was 10 before this summary; `gsd check evaluation-scope --plan 20-05 --commits-only` resolved the plan commits.
- SECURITY-19-OK, REVIEW-20-OK, DISPOSITION-20-OK and DOCS-FROZEN-OK all print on the final tree.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*
