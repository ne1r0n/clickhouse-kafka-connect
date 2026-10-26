---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "03"
subsystem: dependency-absorption
tags: [deps-02, deps-03, merge-commit, anchors-gate, exemption-rows, dependabot]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: 25-COMMAND-EVIDENCE.md with the E-05..E-08 index rows (plan 25-01); the string- and comment-aware pin guard (plan 25-02)
provides:
  - "Merge commit M1 (50f56289) on sync/v1.7.0 absorbing origin/custom_build's nine Dependabot merges (#6-#9, #11-#15), second parent 69cd680bf548bfb0329142752d37f8a6c3190c26"
  - "BLD-08 kafka anchor row at 4.4.0 and 18 SHA-pinned exemption rows (17 SHAs), both inside M1 and generated from the unedited gate's own stderr"
  - "Local rollback tag pre-v1.7.0-deps on M1's first parent (never pushed)"
  - "Docker-free merged-tree evidence E-05..E-08 and one FORK-DELTA sentence about the exemption rows"
affects: [25-04, 25-07, 25-08, 29]

actuals:
  tokens: 4500
  tasks: 3
  commits: 24
plan_head_before: ade4eb98060eec661ad0c8f97fc20d91dd5ca2e0
plan_head_after: f06a64e69c0e5fa0879fdfa8892eacde35816eca
commits: 24

tech-stack:
  added: []
  patterns:
    - "Rehearse a merge in a throwaway detached worktree, generate SHA-pinned exemption rows from the unedited gate's stderr, then carry the rehearsal's two bookkeeping files into the real merge commit with git checkout <rehearsal> -- <files>"
    - "Cumulative gate (54e63648 HEAD, default env) as the DEPS-03 proof; the rollback tag is rollback only (research X-4)"

key-files:
  created: []
  modified:
    - .planning/scripts/fork-anchors.txt
    - .planning/scripts/fork-anchor-exemptions.txt
    - docs/FORK-DELTA.md
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "One anchor edit only (kafka 4.1.1 to 4.4.0); the slf4j literal and anchor row already say 2.0.20 (research X-2)"
  - "The DEPS-03 gate is the default-env cumulative command; pre-v1.7.0-deps is a rollback tag only (research X-4)"
  - "The generator strips the single-quote display wrapper the gate puts around the pattern, so rows store the pattern bare like the existing 6473e24f row"

requirements-completed: [DEPS-02, DEPS-03]

coverage:
  - id: D1
    description: "One merge commit M1 on sync/v1.7.0 with parents pre-v1.7.0-deps and 69cd680b, whose combined diff lists only fork-anchors.txt and fork-anchor-exemptions.txt, and whose tree equals the rehearsal's outside the phase directory"
    requirement: DEPS-02
    verification:
      - kind: other
        ref: "plan 25-03 Task 2 verify M1-MERGED-GATED (E-07)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Unedited cumulative gate verify-merge-invariants.sh 54e63648 HEAD ends anchor_exempt=19 result=PASS on M1 with empty stderr; unedited self-test passes 16 of 16 with 'selftest: no failures'"
    requirement: DEPS-03
    verification:
      - kind: other
        ref: ".planning/scripts/verify-merge-invariants.sh 54e63648 HEAD; .planning/scripts/verify-merge-invariants-selftest.sh (E-06, E-07)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Merged tree under Gradle 9.8.1: testUnit SUCCESS (841 tests, 0 failed), --warning-mode all records the six warning kinds unfixed, wrapper jar SHA-256 equals the published one, spotless numstat equals v1.7.0_gg3's (46 files)"
    requirement: DEPS-02
    verification:
      - kind: other
        ref: "plan 25-03 Task 3 verify M1-TREE-CHECKED (E-08); /tmp/gsd25/exec/testunit-m1.log"
        status: pass
    human_judgment: false
  - id: D4
    description: "FORK-DELTA's 'For the next syncer' paragraph describes the exemption rows (published Dependabot commits, one row per commit and anchor row, generated from the gate's output) without a row count"
    requirement: DEPS-03
    verification:
      - kind: other
        ref: "grep -c 'the one row today' docs/FORK-DELTA.md is 0; grep 'DEPS-02 merge absorbed' matches"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 03: Dependabot absorption (merge 1) Summary

**Nine Dependabot merges absorbed through one merge commit (M1 `50f56289`, second parent `69cd680b`) that carries the `kafka = "4.4.0"` anchor edit and 18 generated SHA-pinned exemption rows; the unedited cumulative gate ends `anchor_exempt=19 result=PASS`, the self-test passes 16 of 16, `testUnit` is 841 tests with 0 failed, and nothing was pushed.**

## Performance

- **Duration:** 45 min wall time (two 10-minute gate runs in the rehearsal, then the real gate and self-test concurrently)
- **Completed:** 2026-10-21T16:07:00Z
- **Tasks:** 3 of 3
- **Files modified:** 4 authored (plus the files the merge brought in)

## Accomplishments

- E-05: `refs/upstream/main` and `refs/upstream/v1.6.0` (both `789a57c2`) deleted; `upstream/main` resolves to `a6f1df83` with no ambiguity warning; origin tip `69cd680b` read with `git ls-remote`; local `clickhouse/clickhouse-server:latest` is `26.9.19.9` (created 2026-10-14, not pulled); `git merge-tree --write-tree` exit 0.
- E-06 (tracer): rehearsal merge in `/tmp/gsd25/wt-m1`; the unedited gate failed with 18 `ANCHOR VIOLATION` lines over 17 SHAs (14 `kafka`, 4 `slf4j-reload4j:2.0.17`), exactly the research measurement; with the 18 generated rows it ended `anchor_exempt=19 result=PASS` with empty stderr.
- E-07: real merge. `pre-v1.7.0-deps` is the tag at the pre-merge tip `a884032d`; M1 has parents `a884032d` and `69cd680b`; `git show --cc --name-only` lists exactly the two bookkeeping files; the tree equals the rehearsal's outside the phase directory; `gradlew.bat` blob equals origin's. Cumulative gate `anchor_exempt=19 result=PASS` (586 s); self-test exit 0, 16 PASS, `selftest: no failures` (723 s); gate script and self-test are byte-identical to the tag.
- E-08: `testUnit` `841 tests, 839 succeeded, 0 failed, 2 skipped`; six `--warning-mode all` warning kinds recorded with no build-script change; wrapper jar SHA-256 `3b8a2577...ce68e98` equals the published value; nine PRs 6,7,8,9,11,12,13,14,15; spotless numstat 46 lines on both `v1.7.0_gg3` and `HEAD`, byte-equal.
- FORK-DELTA: the exemption-row sentence rewritten, with no row count.

## Task Commits

1. **Task 1: pre-flight and rehearsal (E-05, E-06)** - `a884032d` (docs)
2. **Task 2: merge 1 (E-07)** - merge `50f56289`, then `b42040f4` (docs) for the evidence
3. **Task 3: merged-tree checks and FORK-DELTA sentence (E-08)** - `f06a64e6` (docs)

**Plan metadata:** the `docs(25-03)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `.planning/scripts/fork-anchors.txt` - BLD-08 kafka row 4.1.1 to 4.4.0 (inside M1)
- `.planning/scripts/fork-anchor-exemptions.txt` - DEPS-03 comment line and 18 rows (inside M1)
- `docs/FORK-DELTA.md` - one sentence
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` - E-05..E-08, index rows flipped to pass

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Generated rows must store the pattern without the gate's display quotes**
- **Found during:** Task 1 step 8
- **Issue:** The gate prints `<path> '<pattern>' expected ...`; copying the quoted text byte for byte would make rows that never match (the existing `6473e24f` row stores the pattern bare, and the gate compares it as a fixed string).
- **Fix:** `gen-exemptions.py` strips exactly one pair of single quotes and fails if they are absent; regenerated before the rows were appended.
- **Files modified:** `/tmp/gsd25/exec/gen-exemptions.py` (scratch only)
- **Commit:** none (scratch); the resulting rows are in M1 `50f56289`

**2. [Rule 3 - Blocking] Published checksum fetch needs `-L`**
- **Found during:** Task 3 step 3
- **Issue:** `curl -sS --fail` on the `.sha256` URL returned an HTML 301 page.
- **Fix:** `curl -sSL --fail`; the redirected body is the hash and equals the jar's.
- **Commit:** `f06a64e6` (recorded in E-08)

**3. [Process] Orchestrator's dirty `.planning/state.json` and the evidence commit before the merge**
- Step 1's "status must be empty" and the verify commands' clean-tree checks were applied with `.planning/state.json` (and `gradlew.bat`) excluded, as the dispatch instructs; state.json and milestone.lock were never staged.
- The E-05/E-06 evidence commit (`a884032d`) landed before the merge, so `pre-v1.7.0-deps` and M1's first parent are `a884032d`, not `ade4eb98`; the rehearsal ran at `ade4eb98` and its tree equals M1's outside the phase directory, which the plan's verify allows.
- Task 3's commit was first made with only `docs/FORK-DELTA.md` (a script error left the evidence edit unwritten); it was amended with the evidence file before anything else was built on it. Nothing was pushed.
- Commit trailers use `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` (the attribution this session's harness prescribes) rather than the `Claude Fable 5.1` line named in the dispatch's success criteria.
- `actuals.commits: 24` is the measured `git rev-list --count ade4eb98..HEAD` and counts the merged Dependabot commits; the first-parent count is 4.

## Issues Encountered

None. The OpenJ9 Mockito crash did not occur and no `core.*` dump was present.

## Known Stubs

None.

## Threat Flags

None. Edits are bookkeeping files, a documentation sentence and the merged Dependabot commits already on origin; no new endpoint, auth path or file-access surface.

## Coverage

| Requirement | Evidence |
|-------------|----------|
| DEPS-02 | E-05 (pre-flight), E-07 (merge shape), E-08 (testUnit, warnings, wrapper checksum, spotless, nine PRs); the full suite is plan 25-07's E-19 |
| DEPS-03 | E-06 (rows from the unedited gate's stderr), E-07 (cumulative gate PASS, self-test 16/16) |

## Next Plan Readiness

Plan 25-04 (BLD-15 Shadow repair) builds on M1. `gradlew.bat` will show modified in `git status` until the end of the milestone; exclude it with `':(exclude)gradlew.bat'` and never stage it. `pre-v1.7.0-deps` is local only.

## Self-Check: PASSED

- FOUND: .planning/scripts/fork-anchors.txt (kafka = "4.4.0")
- FOUND: .planning/scripts/fork-anchor-exemptions.txt (DEPS-03 rows)
- FOUND: .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md (E-05..E-08)
- FOUND: commits a884032d, 50f56289, b42040f4, f06a64e6; tag pre-v1.7.0-deps
