---
phase: 33-dependency-sweep-protobuf-decision
plan: "01"
subsystem: build-dependencies
tags: [lz4-java, dependabot, gradle, rollback-tag, merge-invariants, evidence]

requires:
  - phase: 32-release-v1.7.0_gg5-published
    provides: published custom_build tip 3a3a15a9 (v1.7.0_gg5) as the sweep base
provides:
  - local lightweight rollback tag pre-v1.7.0_gg6 on origin custom_build 3a3a15a9 (not pushed)
  - lz4-java 1.12.1 (GHSA-48mj-8f7c-7qfm security release) absorbed from PR #19 as its own --no-ff merge commit 6bf1c43b
  - green baseline and green lz4-tip full suite on latest x V2 (1961 tests, 18 skipped)
  - Phase 33 evidence file with E-01..E-06 filled and E-07..E-14 pending
affects: [33-02, 33-03, 38-publish]

actuals:
  tokens: 14500
  tasks: 2
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Dependabot head fetched by refs/pull/N/head, checked (author, one parent, files, version), merged --no-ff as its own commit with the head SHA in the message"
    - "Baseline full suite on the untouched tip so a later red is attributable to one change"

key-files:
  created:
    - .planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md
  modified:
    - gradle/libs.versions.toml

key-decisions:
  - "PR #19 absorbed unchanged from its fetched head 2a806038 (not rebased since research); the merge also brings the published release merges 21c51598 and 3a3a15a9 into the branch history, tree-neutral"
  - "pre-v1.7.0_gg6 tag created locally on the re-read origin tip and never pushed; Phase 38 pushes it after the published SHA's run is green"

requirements-completed: [SWP-01, SWP-04]

coverage:
  - id: D1
    description: "Origin custom_build re-read before the first change and a local lightweight pre-v1.7.0_gg6 tag on that SHA, absent from origin"
    requirement: SWP-04
    verification:
      - kind: other
        ref: "33-COMMAND-EVIDENCE.md E-01 (P33-01-T1-OK check script)"
        status: pass
    human_judgment: false
  - id: D2
    description: "PR #19 head fetched by ref, validated, merged as its own commit; first-parent diff is exactly lz4Java 1.12.0 to 1.12.1"
    requirement: SWP-01
    verification:
      - kind: other
        ref: "33-COMMAND-EVIDENCE.md E-03 (P33-01-T1-OK check script)"
        status: pass
    human_judgment: false
  - id: D3
    description: "lz4-java 1.12.1 on runtimeClasspath and clickhouseDependencies and in the pom.properties of the inner jar of the verified Confluent archive"
    requirement: SWP-01
    verification:
      - kind: other
        ref: "./gradlew dependencyInsight --dependency at.yawk.lz4:lz4-java; createConfluentArchive (verifyConfluentArchive finalizer); E-04"
        status: pass
    human_judgment: false
  - id: D4
    description: "Full suite green on latest x V2 before (E-02) and after (E-05) the bump: 1961 tests, 0 failures, 0 errors, 18 skipped; Lz4RoundTripLiveTest ran green"
    requirement: SWP-01
    verification:
      - kind: integration
        ref: "CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun; Lz4RoundTripLiveTest XML tests=2 failures=0"
        status: pass
    human_judgment: false
  - id: D5
    description: "Cumulative merge-invariant gate verify-merge-invariants.sh 54e63648 HEAD passes over the range holding the absorbed release merges"
    requirement: SWP-04
    verification:
      - kind: other
        ref: ".planning/scripts/verify-merge-invariants.sh 54e63648 HEAD -> result=PASS exit 0 (E-06)"
        status: pass
    human_judgment: false

duration: 27min
completed: 2026-10-28
status: complete
plan_head_before: 7f27114991a0e6971900dde0ce6c070e41e1a704
plan_head_after: 9e5508ad9bdb18d9f0774813fe3f9c19e3c7f958
---

# Phase 33 Plan 01: lz4-java 1.12.1 sweep tracer Summary

**lz4-java 1.12.1 (GHSA-48mj-8f7c-7qfm) travelled from a fetched Dependabot head into the verified Confluent archive as its own merge commit, behind a local `pre-v1.7.0_gg6` rollback tag and a green baseline suite, with the full suite and the cumulative anchors gate green on top.**

## Performance

- **Duration:** 27 min
- **Started:** 2026-10-28T18:40:55Z
- **Completed:** 2026-10-28T19:07:08Z
- **Tasks:** 2
- **Files modified:** 2 (`gradle/libs.versions.toml` via the merge, and the new evidence file)

## Accomplishments

- Re-read `origin/custom_build` at `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (matches the expected `v1.7.0_gg5` tip) and tagged `pre-v1.7.0_gg6` locally on it; the tag is lightweight (`commit`) and absent from origin (`ls-remote --exit-code` exit 2).
- Baseline full suite on the untouched tip (E-02): 1961 tests, 0 failures, 0 errors, 18 skipped on ClickHouse 26.10.3.7, digest `sha256:739e099a...`; no JVM crash.
- PR #19 head `2a806038` (dependabot[bot], one parent equal to the origin tip, only `gradle/libs.versions.toml`, REST head matched, body names GHSA-48mj-8f7c-7qfm) merged `--no-ff` as `6bf1c43b`; first-parent diff is the single `lz4Java` line (E-03).
- `dependencyInsight` shows `at.yawk.lz4:lz4-java:1.12.1` on runtimeClasspath and clickhouseDependencies; `createConfluentArchive` exited 0 with `verifyConfluentArchive` passing and the archive's inner jar `pom.properties` reads `version=1.12.1` (E-04).
- Full suite on the lz4 tip (E-05): identical totals to the baseline on the same image digest, `Lz4RoundTripLiveTest` 2 tests green, anchors mirror `rows=131 bad=0`.
- Cumulative merge-invariant gate (E-06): `invariants: provenance_paths=21 anchor_commits=981 anchor_rows=131 marker_commits=1040 anchor_exempt=19 result=PASS`, exit 0, 20 minutes.

## Task Commits

1. **Task 1: tracer, origin re-read, tag, baseline, PR #19 merged and proven** - `6bf1c43b` (PR #19 merge, the only source-changing commit) and `eca4aed3` (evidence E-01..E-04)
2. **Task 2: live gate on the lz4 tip** - `9e5508ad` (evidence E-05, E-06)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan).

`commits: 6` is measured with `git rev-list --count 7f271149..HEAD`. Three of the six are the plan's own first-parent commits above; the other three are the absorbed ancestors that PR #19's parent brought in (the Dependabot head `2a806038` and the published release merges `21c51598` and `3a3a15a9`).

## Files Created/Modified

- `gradle/libs.versions.toml` - `lz4Java` 1.12.0 to 1.12.1, arriving through the PR #19 merge commit
- `.planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md` - header, index E-01..E-14, entries E-01..E-06

## Decisions Made

- Merged the PR #19 head exactly as fetched; it had not been rebased since research (`rebased_since_research: no`).
- Left `VERSION` and `src/main` untouched; `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` exits 0.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. No JVM crash occurred in either full-suite run, and the origin tip never moved (re-read at start, before the merge, and after the gate: always `3a3a15a9`).

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or trust-boundary file was introduced; the only dependency change is a patch version of an existing Maven coordinate from an audited Dependabot head.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 33-02 (PRs #18, #20, #16) from a gated, attributable tip. Evidence reuse keys: `origin_custom_build`, `origin_heads_non_dependabot`, `pr_heads`, `open_prs` in E-01.
- The tag `pre-v1.7.0_gg6` and all four-merge work remain local; nothing was pushed and no PR action was taken on origin.

## Self-Check: PASSED

- FOUND: `.planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md`
- FOUND commits: `6bf1c43b`, `eca4aed3`, `9e5508ad` (ancestors of HEAD)
- Task 1 and Task 2 automated verifies printed `P33-01-T1-OK`, `P33-01-T1-COMMITTED`, `P33-01-T2-OK`.
- Plan verification: `git rev-list --first-parent --merges pre-v1.7.0_gg6..HEAD` prints exactly `6bf1c43b`; `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` exits 0.

---
*Phase: 33-dependency-sweep-protobuf-decision*
*Completed: 2026-10-28*
