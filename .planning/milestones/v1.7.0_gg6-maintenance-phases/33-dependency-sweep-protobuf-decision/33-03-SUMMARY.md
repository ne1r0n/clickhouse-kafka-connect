---
phase: 33-dependency-sweep-protobuf-decision
plan: "03"
subsystem: build-dependencies
tags: [protobuf, dependabot, ignore, pin-guard, fork-delta, phase-gate, evidence]

requires:
  - phase: 33-dependency-sweep-protobuf-decision
    provides: plans 33-01 and 33-02, the sweep tip 79f56a5e (four bump merges #19, #18, #20, #16) and evidence E-01..E-11
provides:
  - gradle entry of .github/dependabot.yml ignores semver-major updates of com.google.protobuf:protobuf-java and com.google.protobuf:protoc by exact name (6ef8e14e)
  - DependabotProtobufIgnoreGuardUnitTest, a Docker-free guard with 2 real-file tests and 9 fixtures (6ef8e14e)
  - FORK-DELTA protobuf row, open item 12 and a Dependabot-row clause with the measured VerifyError cause and the FUT-03 trigger (093bab99)
  - PR #10 recorded as declined and closing at publication, not merged
  - Phase 33 gate green (E-14) with origin untouched and nothing pushed
affects: [34-tail-merge, 38-publish, FUT-03]

actuals:
  tokens: 9600
  tasks: 3
  commits: 5

plan_head_before: e5b7b43e0e5332dcd918ba086f1ca92b2fd583da
plan_head_after: d06f8e5c07d079bcd7c5d6ca5dbbd095acd7db51

tech-stack:
  added: []
  patterns:
    - "Dependabot ignore pinned by a line-level Docker-free guard that reads the real file and fixtures through the same comment-stripping reader the pin guards use"
    - "Tracer: guard written first and seen red on the unchanged config (one failure, the predicted real-file test), then green after an insert-only config edit"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java
    - .planning/phases/33-dependency-sweep-protobuf-decision/33-03-SUMMARY.md
  modified:
    - .github/dependabot.yml
    - docs/FORK-DELTA.md
    - .planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md

key-decisions:
  - "Both coordinates that read the libprotobuf ref are ignored for semver-major by exact name (protobuf-java and protoc); no wildcard, so the protobuf Gradle plugin stays updatable"
  - "PR #10 disposition defaults to closing-at-publication (ROADMAP criterion 4); pr10_user_words is none received and no agent touched the PR"
  - "The FORK-DELTA row states the archive holds no com/google/protobuf/ entry because the measured count is 0 (E-13)"

requirements-completed: [SWP-03, SWP-04, SWP-01, SWP-02]

coverage:
  - id: D1
    description: "gradle entry of dependabot.yml holds exactly the two exact-name semver-major ignore items (protobuf-java, then protoc); github-actions entry has no ignore and its exclude-paths are unchanged"
    requirement: SWP-03
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java#gradleEntryIgnoresProtobufJavaAndProtocMajorVersions"
        status: pass
      - kind: other
        ref: "ruby -ryaml semantics check printing YAML-OK (33-COMMAND-EVIDENCE.md E-12)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Guard class pins both items against dropping, widening, a versions range, a wildcard, a commented-out ignore and a move into the github-actions entry"
    requirement: SWP-03
    verification:
      - kind: unit
        ref: "DependabotProtobufIgnoreGuardUnitTest, 11 tests; E-12 red 11/1/0/0 then green 11/0/0/0"
        status: pass
    human_judgment: false
  - id: D3
    description: "FORK-DELTA records the protobuf decision with the measured cause (ProtobufIngestTest VerifyError against Confluent 7.9.1, run 39501799049) and the FUT-03 re-open trigger"
    requirement: SWP-03
    verification:
      - kind: other
        ref: "P33-03-T2 check script over docs/FORK-DELTA.md and E-13 (14 checks True)"
        status: pass
    human_judgment: false
  - id: D4
    description: "PR #10 is declined and recorded as closing once the ignore reaches custom_build at publication"
    requirement: SWP-03
    verification:
      - kind: other
        ref: "E-13 pr10_merged_at null, pr10_disposition closing-at-publication; E-14 pr10_merged_at_at_gate null"
        status: pass
    human_judgment: true
    rationale: "Whether Dependabot closes PR #10 by itself after publication is undocumented, and the user may prefer to close it now; only the user can do that, and Phase 38 re-reads it"
  - id: D5
    description: "Phase 33 gate: origin untouched, merge order #19 #18 #20 #16, testUnit and full suite green on latest x V2, anchors mirror bad=0, invariant gate PASS and self-test clean, empty src/main diff"
    requirement: SWP-04
    verification:
      - kind: integration
        ref: "33-COMMAND-EVIDENCE.md E-14 (22 checks True); suite 1972/0/0/18 on ClickHouse 26.10.3.7"
        status: pass
    human_judgment: false

duration: 52min
completed: 2026-10-28
status: complete
---

# Phase 33 Plan 03: Protobuf decision, guard and phase gate Summary

**protobuf-java and protoc pinned to 3.x by exact-name semver-major Dependabot ignores in the gradle entry, guarded Docker-free, with the measured VerifyError cause and FUT-03 trigger in FORK-DELTA and the Phase 33 gate green**

## Performance

- **Duration:** 52 min (the cumulative invariant gate and its self-test account for most of it)
- **Started:** 2026-10-28T19:23Z
- **Completed:** 2026-10-28T20:15Z
- **Tasks:** 3
- **Files modified:** 5 (1 created, 4 modified, counting the evidence file and this SUMMARY)

## Accomplishments

- `DependabotProtobufIgnoreGuardUnitTest` (11 tests, extends nothing, so it stays in `testUnit`) was seen red on the unchanged file with exactly one failure, the predicted real-file test, and green after the config edit. The red XML is kept at `/tmp/gsd33/exec/guard-red.xml`: tests 11, failures 1, errors 0, and the failure message names the missing `ignore:` list.
- `.github/dependabot.yml` gained seven inserted lines in the `gradle` entry: a `# FORK:` comment and two exact-name items, `com.google.protobuf:protobuf-java` then `com.google.protobuf:protoc`, each with `update-types: ["version-update:semver-major"]`. The `github-actions` entry is byte-unchanged, and the Ruby YAML check printed `YAML-OK`.
- FORK-DELTA has a new Build-and-dependencies row, open item 12 and a Dependabot-row clause. The cause was re-read from PR #10's own run `39501799049` with `gh run view --log-failed`: 27 `ProtobufIngestTest` cases and 29 failures in all (the other two are protobuf cases of `ClickHouseSinkTaskWithSchemaTest`), 29 `java.lang.VerifyError` lines, each in `ProtobufSchema.toEnum`.
- The Confluent archive holds 0 entries containing `com/google/protobuf/` (7801 listed, `unzip` exit 0), so the row states that fact. Upstream `9bed6f79` reads `libprotobuf = "3.25.9"` and has no `ignore` in its `dependabot.yml`, recorded as E-13 keys.
- The Phase 33 gate (E-14) passed: origin `custom_build` still `3a3a15a9`, tag and scratch branch absent on origin, merges #19, #18, #20, #16 in order with their recorded heads, `testUnit` 1385/0/0/2, full suite 1972/0/0/18 on ClickHouse 26.10.3.7, anchors mirror `rows=131 bad=0`, invariants `result=PASS`, self-test `no failures`, and an empty `src/main` diff against `pre-v1.7.0_gg6`.

## Task Commits

1. **Task 1: protobuf ignore guard and gradle-entry ignores (SWP-03)** - `6ef8e14e` (ci), evidence `e44a66f8`
2. **Task 2: PR #10, cause, archive, FORK-DELTA decision (SWP-03)** - `093bab99` (docs), evidence `c9443756`
3. **Task 3: Phase 33 gate (SWP-04, SWP-01, SWP-02, SWP-03)** - evidence `d06f8e5c` (docs)

**Plan metadata:** recorded in the `docs(33-03): complete ...` commit that follows this file.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java` - the guard; reads through `TestsWorkflowPinGuardUnitTest.nonCommentLines`, two real-file tests and nine fixtures
- `.github/dependabot.yml` - the two ignore items in the `gradle` entry, insert only
- `docs/FORK-DELTA.md` - protobuf row, open item 12, Dependabot-row clause and Pinned-by additions
- `.planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md` - E-12, E-13, E-14, all 14 index rows `pass`

## Decisions Made

- Both `libprotobuf` readers are ignored by exact name, not `com.google.protobuf:*`, so the protobuf Gradle plugin (`protobufPlugin`) stays updatable. Whether Dependabot detects `protoc` through its interpolated coordinate is unverified, and FORK-DELTA says so.
- PR #10 is recorded as `closing-at-publication`. The user has given no words on it (`pr10_user_words: none received`). It stays open on GitHub, and no agent closed it, commented on it or posted a `@dependabot` command.

## Deviations from Plan

None - plan executed exactly as written.

Two process notes, neither a deviation. The Edit tool trimmed the whitespace-only line 15 of `dependabot.yml` on its first write, which made the diff one deletion; it was restored byte-exact with a scoped script and `git diff --numstat` then read 7 insertions and 0 deletions, and the green run was repeated after the restore. The `gsd_run check tdd-red-evidence` classifier was not run, because the plan's own E-12 criteria (one failure, the predicted test, the failure message) define the red evidence for this tracer; the red set equals the prediction.

**Total deviations:** 0
**Impact on plan:** none.

## Requirement wording flag

REQUIREMENTS.md SWP-03 reads that PR #10 "is closed with a Dependabot `ignore`". ROADMAP Phase 33 criterion 4, which governs and which this plan delivered, accepts PR #10 "recorded as closing once the `ignore` reaches `custom_build` at publication". The requirement text is therefore slightly stricter than what shipped: PR #10 is still open on GitHub and will be closed after publication, either by Dependabot's refresh or by the user. The user may want to align SWP-03's wording. No agent edited SWP-03's text in REQUIREMENTS.md. See E-13's `swp03_wording_note`.

## Issues Encountered

None.

## Authentication Gates

None. `gh auth status` exited 0 and `gh` was used once, read-only, for the failed-run log.

## Known Stubs

None.

## Threat Flags

None. The only new surface is the Dependabot ignore, which is the planned T-33-10 mitigation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 33 is gated and its scratch branch `sync/v1.7.0` is the attributable base for Phase 34's tail merge.
- For the user: PR #10 (protobuf-java 4.36.2) is declined. If you would rather close it now, close it on GitHub yourself and tell me in your own words, and Phase 38 records that. Please keep not merging Dependabot PRs on `origin` until publication; PR #17 stays open for Phase 36.
- Nothing was pushed, and `.planning/config.json` and `.planning/state.json` remain dirty and unstaged.

---
*Phase: 33-dependency-sweep-protobuf-decision*
*Completed: 2026-10-28*

## Self-Check: PASSED

All five key files exist and the five task and evidence commits (`6ef8e14e`, `e44a66f8`, `093bab99`, `c9443756`, `d06f8e5c`) are ancestors of HEAD.
