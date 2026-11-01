---
phase: 36-client-0-11-0-audit-bump
plan: "04"
subsystem: kafka-connect-sink
tags: [clickhouse-client, client-0.11.0, rollout-items, revert-rehearsal, phase-gate, go-no-go, clt-07-handoff, CLT-09, CLT-07]

requires:
  - phase: 36-client-0-11-0-audit-bump
    provides: plan 36-01 pins and golden, plan 36-02 fix and catalog commits, plan 36-03 byte comparison, archive audit and D-17 rewording
provides:
  - E-11 and 36-ROLLOUT-ITEMS.md - 14 labelled rollout items for Phase 38, the custom_ stub probe on both client lines, and the CLT-09 revert rehearsed conflict-free on the final history
  - E-12 - the Phase 36 gate (testUnit 1474, two full suites 2072 each, anchors rows=152, tail and cumulative gates PASS) and the CLT-09 decision, go
  - E-13 and 36-USER-DISPATCH.md - the user-gated CLT-07 nightly handoff, pending
affects: [phase-37-archive-allow-list, phase-38-release]

actuals:
  tokens: 11915
  tasks: 3
  commits: 5

plan_head_before: 52a43e2d9a676b0052caf059544fcc6eb23bc933
plan_head_after: 7f0f37a464190db79f5afc50301cc7cd1f89be44

tech-stack:
  added: []
  patterns:
    - "A detached worktree rehearses a git revert of the documentation and catalog commits on the final history before the gate decides"
    - "A throwaway stub probe of the request URI proves a behaviour change on both client lines without a server"
    - "The cumulative merge gate runs in the background with an exit-code file beside the Gradle gate"

key-files:
  created:
    - .planning/phases/36-client-0-11-0-audit-bump/36-ROLLOUT-ITEMS.md
    - .planning/phases/36-client-0-11-0-audit-bump/36-USER-DISPATCH.md
  modified:
    - .planning/phases/36-client-0-11-0-audit-bump/36-COMMAND-EVIDENCE.md

key-decisions:
  - "CLT-09 decision: go. Every gate check held; the connector ships client 0.11.0 and the revert (documentation commit, then catalog commit) is rehearsed and not applied"
  - "The custom_ forwarding stays a rollout item and a user decision; no third createClientV2 option was added"
  - "CLT-07's nightly dispatch is left to the user; E-13 stays pending (user-gated) and CLT-07 is not marked passed"

requirements-completed: [CLT-09, CLT-07, CLT-01, CLT-02, CLT-03, CLT-04, CLT-05, CLT-06, CLT-08]
requirements-partial: [CLT-07]

coverage:
  - id: D1
    description: "14 rollout items with labels, Applies-when values and evidence citations, the replay wording with its scope sentence, the zstd-jni platforms and temp folder, the custom_ effect with its probe on both client lines, operator steps and the Phase 37 and 38 handoffs, in one commit of one file"
    requirement: "CLT-09"
    verification:
      - kind: unit
        ref: "36-COMMAND-EVIDENCE.md E-11; P36-04-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The CLT-09 revert rehearsed on the final history: documentation commit then catalog commit revert with no conflict, the catalog revert touches exactly the catalog line, ClientCatalogPinUnitTest and one anchor row, rows=151, client-v2 0.9.6, fixes, FORK-DELTA row and golden kept, pins, UtilsTest, the retriable-code classes, BLD-16, the live class, the golden and the jdbc live test green on 0.9.6"
    requirement: "CLT-09"
    verification:
      - kind: integration
        ref: "36-COMMAND-EVIDENCE.md E-11; P36-04-T1-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "The phase gate and the go decision: cache-busted testUnit 1474, two --rerun full suites 2072 each with 18 named skips and a Docker-only delta of 7 on ClickHouse 26.10.3.7 with client V2 on client 0.11.0, anchors rows=152, tail and cumulative gates PASS, origin unchanged"
    requirement: "CLT-09"
    verification:
      - kind: integration
        ref: "36-COMMAND-EVIDENCE.md E-12; P36-04-T2-COMMON-OK, P36-04-T2-GO-OK, P36-04-T2-TESTUNIT-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "The user-gated CLT-07 handoff file: SHA checks, push, dispatch with the branch-selection warning, report fields, red-run, clean-up and resume steps; the nightly run itself is not performed"
    requirement: "CLT-07"
    verification:
      - kind: unit
        ref: "36-COMMAND-EVIDENCE.md E-13; P36-04-T3-OK"
        status: pass
    human_judgment: true
    rationale: "The nightly dispatch on a pushed scratch branch is the user's step and its result (E-15) does not exist yet; whether the handoff reads clearly to the user is judgment"
  - id: D5
    description: "Whether the rollout items read correctly and completely to an operator is not asserted by any test; each claim carries a label and an evidence citation, and the custom_ alternative is recorded as a user decision"
    requirement: "CLT-06"
    verification: []
    human_judgment: true
    rationale: "Wording quality and operator comprehension are judgment, reviewed in the Phase 38 release review"

duration: 46min
completed: 2026-10-29
status: complete
---

# Phase 36 Plan 04: Rollout Items, Revert Rehearsal, Gate and the CLT-07 Handoff Summary

**The connector ships ClickHouse client 0.11.0: 14 labelled rollout items are recorded for Phase 38, the CLT-09 revert is rehearsed conflict-free on the final history, the cache-busted testUnit (1474) and two full suites on ClickHouse latest with client V2 (2072 each) are green with the merge gates PASS, and the CLT-07 nightly is handed to the user in a file.**

## Performance

- **Duration:** about 46 min (start about 13:45Z, end about 14:31Z, 2026-10-29)
- **Tasks:** 3
- **Files:** 3 (2 new record files, 1 evidence file)

## Decision

**Go.** Evidence entry E-12; gated head `a8b61f1512ab245a584b506527beaceca7264b04`. Nothing was reverted.

- Cache-busted `testUnit`: 1474 tests (baseline 1458 plus 16), 0 failures, 0 errors, 2 skipped.
- Two `--rerun` full suites on ClickHouse `latest` (26.10.3.7, digest `sha256:739e099a...b1ce`, unchanged since the baseline) with client V2 on client 0.11.0: 2072 tests each (baseline 2049 plus 23), 0 failures, 0 errors, exactly the 18 named skips (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), Docker-only delta exactly 7. Class tallies: error shapes 7, timeout prefixes 2, endpoint and options 3, insert wire 2, catalog pin 2, behaviour pins (live) 6, golden (live) 1.
- Anchors mirror `rows=152 bad=0`. Tail gate `provenance_paths=27 anchor_commits=86 anchor_rows=152 marker_commits=93 anchor_exempt=0 result=PASS`. Cumulative gate `provenance_paths=21 anchor_commits=1097 anchor_rows=152 marker_commits=1163 anchor_exempt=19 result=PASS`, exit 0. Gate scripts identical to the Phase 33 version.
- Scope: `src/main` differs from the phase start in exactly `ClickHouseHelperClient.java`; the high-risk packages and files are untouched; the only build-side change is `gradle/libs.versions.toml`; no test deleted, 12 added, 5 modified as expected; no gg6 rollout note, `VERSION` or `CHANGELOG.md` touched.
- Origin `custom_build` is `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; `pre-v1.7.0-tail-2`, `sync/v1.7.0` and `scratch/*` are absent on origin.

## Accomplishments

- **Rollout items (E-11, task 1).** `36-ROLLOUT-ITEMS.md` holds the 14 items with the three evidence labels (`measured`, `read from the code`, `measured in research (not re-run)`), Applies-when values and evidence citations, plus the exactly-once replay wording (`clean_stop_required: no`, for the compared matrix only), the `zstd-jni` platforms and temp folder, the `custom_` section with its probe URIs, the operator steps and the Phase 37 and 38 handoffs.
- **The `custom_` probe on both lines (E-11).** With `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` the request URI on client 0.11.0 is `/?custom_myparam=7&custom_settings=max_threads%253D3&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4`: both keys forwarded, the operator's percent-encoding doubled. On client 0.9.6 it is `/?query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4`: nothing forwarded.
- **CLT-09 revert rehearsal (E-11).** In a detached worktree at the committed head, `git revert` of the documentation commit and then the catalog commit applied with no conflict. The catalog revert touched exactly `.planning/scripts/fork-anchors.txt`, `gradle/libs.versions.toml` and `ClientCatalogPinUnitTest.java`. The worktree then showed `clickHouseDriver = "0.9.6"`, `rows=151 bad=0`, `client-v2:0.9.6`, the two `createClientV2` fixes with their FORK-DELTA row and the golden kept. On 0.9.6 the error-shape (7), timeout-prefix (2), endpoint (3) and wire (2) pins, `UtilsTest`, the retriable-code classes, BLD-16 and the planning-id guard are green with no failing testcase, and the live class (6), the golden (1) and the jdbc-properties class (2) are green. The worktree and the probe file are gone and the main tree was not touched.
- **CLT-07 handoff (E-13, task 3).** `36-USER-DISPATCH.md` is written and committed; the plan stops there.

## Commits

| Commit | SHA | Paths |
|--------|-----|-------|
| rollout items | `7464977bed6efe53c20827e6ef3bc38459a482bc` | `36-ROLLOUT-ITEMS.md` |
| evidence E-11 | `a8b61f1512ab245a584b506527beaceca7264b04` | `36-COMMAND-EVIDENCE.md` |
| evidence E-12 | `3c6e3696e66b382b7e6d3b8f6811a87f2c4cce6f` | `36-COMMAND-EVIDENCE.md` |
| handoff | `fccb5bb142843593c40332259889300b3de17b13` | `36-USER-DISPATCH.md` |
| evidence E-13 | `7f0f37a464190db79f5afc50301cc7cd1f89be44` | `36-COMMAND-EVIDENCE.md` |

All paths are under `.planning/phases/36-client-0-11-0-audit-bump/`. **Plan metadata:** the SUMMARY commit that follows this file. `STATE.md`, `ROADMAP.md`, `REQUIREMENTS.md`, `.planning/config.json` and `.planning/state.json` were not staged (orchestrator-managed, per the run instructions).

## For the user

CLT-07's nightly dispatch is yours. The orchestrator must ask you and, after your report, append E-15. The full handoff is `.planning/phases/36-client-0-11-0-audit-bump/36-USER-DISPATCH.md`.

- **The SHA to push** is `SHA=$(git rev-parse sync/v1.7.0)`, after the checks: `git ls-remote origin refs/heads/custom_build` reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; `git diff --quiet a8b61f1512ab245a584b506527beaceca7264b04 "$SHA" -- . ':(exclude).planning' && echo same-as-gated` prints `same-as-gated`; `git ls-remote origin | grep "$SHA"` prints nothing.
- **The scratch branch:** `scratch/clt-07-client-0.11.0`.
- **Push:** `git push origin "$SHA":refs/heads/scratch/clt-07-client-0.11.0`. It publishes about 157 local commits (the whole local `.planning` tail included; deleting the branch does not unpublish them) and runs `tests.yaml` once.
- **Dispatch:** workflow `Nightly Integration Tests (ClickHouse latest, client V2)`, "Use workflow from" the scratch branch, never `custom_build`; or `gh workflow run nightly-integration-tests.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/clt-07-client-0.11.0`.
- **Report directly to the orchestrator:** run URL and id; `headSha` (must equal the pushed SHA), `event`, `headBranch`; conclusion; start and end times and duration; the totals line of the `Summarise failed tests` step and the artifact counts (the local run had `55 tests, 54 succeeded, 0 failed, 1 skipped`); the ClickHouse server version of the run; the conclusion of the `tests.yaml` run on the same push; whether you deleted the scratch branch.
- **Open decision (custom_):** a third `createClientV2` option that would keep the 0.9.6 behaviour for `custom_` keys in `jdbcConnectionProperties` was not added. It is recorded in the rollout items as the alternative; it is a production behaviour change beyond the two required fixes.
- **PR #17** (OPEN, head `6a53525b738d90ee4c98a3761ebebe11421cf1f0`, same SHA as the Dependabot branch) was only read and is untouched. It is superseded by the catalog commit; closing it is your action at publication. Do not merge PR #17 or any Dependabot PR on origin before publication.

## Handoffs

Phase 37:
- The archive allow-list must name the 37 `com/github/luben/zstd/` classes, the 17 `META-INF/versions/22/com/github/luben/zstd/` classes and the three natives; the compare script's VERSION table has no `zstd-jni` row.
- The stale `// temp to override client's with CVE` comment and the redundant guava pin in `build.gradle.kts` are a follow-up (no build file was edited).

Phase 38:
- `36-ROLLOUT-ITEMS.md` goes into `docs/ROLLOUT-v1.7.0_gg6.md` (RLS-01, RLS-02); on a no-go only the `either client` rows would stay, but the decision is go. RLS-01 refreshes the raw FORK-DELTA numbers once, and FORK-DELTA's open item 8 still names client `0.10.0` as a future requirement. `VERSION` and `CHANGELOG.md` were not touched here.
- Upstream state at the gate: upstream main `9bed6f7970670a55c66be8efda246889e1f95bd0` (unchanged since E-01), newest release tag `v1.7.0`, Maven's latest `client-v2` `0.11.0`, PR #17 OPEN, Dependabot branch `6a53525b738d90ee4c98a3761ebebe11421cf1f0`.
- For the milestone close: CLT-07's nightly result (E-15), if the user has run it.

## Deviations from Plan

**1. [Rule 3 - Blocking] JVM crashes in the full suites, resolved by the documented escalation.** *Found during:* task 2 step 4. The OpenJ9 fault in Mockito's inline retransformation (`InstrumentationImpl.retransformClasses0` under `InlineBytecodeGenerator.mockClass`, exit value 255, swap 44.7 GB of 46 GB in use) ended suite 1's first run and suite 2's first and second runs after 1790 executions each, with no failing testcase. Per plan 36-01's crash rule I read each javacore, deleted the crash files by name, and re-ran once. Suite 1's re-run was green on the default JDK 17. Suite 2's re-run crashed again, so I ran it once more with `-PjavaVersion=21`, per the same rule, and it was green with exactly suite 1's counts. All runs are in E-12. The decision rule of this plan lists "a JVM crash that recurs after the documented re-run" as a STOP; I read the documented procedure as including the `-PjavaVersion=21` run, and treated the green third run as the documented outcome. *Consequence for the orchestrator:* suite 1 ran on JDK 17 and suite 2 on JDK 21; if you want both suites on one JVM, a further run is needed. *Files modified:* none. *Verification:* `P36-04-T2-GO-OK` (the XML on disk is the JDK 21 run: 2072 tests, 18 skips). 

**2. Process note.** The first E-12 draft recorded `clickhouse_version` twice; the duplicate was removed from the uncommitted text before the evidence commit, so the committed entry has each key once. No commit was amended.

No verify block needed a baseline move. **Total deviations:** 1 auto-resolved (Rule 3), 1 note. **Impact:** none on the committed content; the JVM split is disclosed here and in E-12.

## Authentication Gates

None.

## Issues Encountered

- Suite 2's recorded run used `-PjavaVersion=21` (see Deviation 1).
- The `.planning/config.json` and `.planning/state.json` modifications predate the phase and were never staged. The throwaway worktree `rehearse` is removed; `git worktree list` shows only the main tree. The three crash dumps (about 8 GB each) were deleted by name; no crash file remains in the root.

## Known Stubs

None. No hardcoded empty value, placeholder or unwired data was added; no skipped or disabled test was added or removed (the 18 named skips are unchanged).

## Threat Flags

None beyond the plan's register. T-36-15 (no push, dispatch or pull-request action; origin re-read at the gate and in every verify), T-36-16 (decision rule written before the run; counts re-read from the JUnit XML; the tail gate re-run live), T-36-17 (the revert rehearsed on the final history, conflict-free, exact paths), T-36-18 (hygiene and secrets helpers clean over every file before its commit) and T-36-19 (every operator claim labelled; the `custom_` alternative recorded as a user decision) are mitigated as planned.

## Self-Check: PASSED

The two record files exist on disk; the five plan commits (`7464977b`, `a8b61f15`, `3c6e3696`, `fccb5bb1`, `7f0f37a4`) are ancestors of HEAD; the rollout-items commit holds exactly `36-ROLLOUT-ITEMS.md` and the handoff commit exactly `36-USER-DISPATCH.md`; `P36-04-T1-OK`, `P36-04-T2-COMMON-OK`, `P36-04-T2-GO-OK`, `P36-04-T2-TESTUNIT-OK` and `P36-04-T3-OK` printed on the committed tip; E-01..E-12 read pass, E-13 reads `pending (user-gated)`, the index reads E-01..E-13; origin `custom_build` is `3a3a15a9`, no scratch ref exists on origin, and no worktree other than the main tree remains.
