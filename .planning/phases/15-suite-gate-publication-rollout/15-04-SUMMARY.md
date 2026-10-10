---
phase: 15-suite-gate-publication-rollout
plan: 04
subsystem: security
tags: [security-audit, secure-phase, asvs-l1, osv, d-09, metadata-free-json, github-actions, dependency-bumps]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: plan 15-02 (D-10 code review), so the src/main tree the audits read is settled
provides:
  - 13-SECURITY.md, a retroactive threat verification of every Phase 13 plan threat model against the tree being published
  - 14-SECURITY.md, the same for Phase 14, with OSV re-queried on the audit day
  - Two audit verdicts for the plan 15-07 approval bundle (D-12 item 5), threats_open 0 in both
  - Two open below-threshold threats (T-13-19, T-14-16) for the bundle's knowingly-open list
affects: [15-05, 15-07, 15-08]

plan_head_before: 7117ccda45a3044d33a7118553e8e913b63bfb30
plan_head_after: 37be6a069e11622e6c5376bd34427a222f73bfac

actuals:
  tokens: 9200
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Inline retroactive security audit against the gsd-security-auditor procedure when the executor has no sub-agent tool (Phase 11 precedent), written in the shape of 09-SECURITY.md"
    - "Measured evidence per threat: current source with line numbers, named pinning test, and an audit-day measurement (adversarial probe, OSV with positive controls, dependencyInsight, REST API re-read)"

key-files:
  created:
    - .planning/phases/13-fork-hardening-on-the-merged-tree/13-SECURITY.md
    - .planning/phases/14-dependency-bumps-ci/14-SECURITY.md
  modified: []

key-decisions:
  - "The two new threats are left open below the high block threshold, not accepted: accepting an upstream-inherited data log (T-13-19) or a missing permissions block (T-14-16) would be a user call, so the audits carry them to the approval bundle instead of closing them"
  - "Audits ran inline, not through the gsd-secure-phase skill: the executor has no sub-agent tool to spawn gsd-security-auditor"

requirements-completed: [PUB-03]

coverage:
  - id: D1
    description: "13-SECURITY.md exists, is committed, covers every threat ID in 13-01..08-PLAN.md plus one new threat, and reports threats_open 0 with MetadataFreeJsonWriter's escaping, raw-byte and data-free-message handling verified against the current tree"
    requirement: PUB-03
    verification:
      - kind: other
        ref: "Task 1 automated check printed AUDIT13-CLEAR; 19 of 19 plan threat IDs present in the file"
        status: pass
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun over seven Phase 13 unit classes (128 tests, 0 failures)"
        status: pass
      - kind: integration
        ref: "./gradlew test --rerun --tests '*NestedNullRowBinaryLiveTest' --tests '*MetadataFreeJsonTypeParityLiveTest' --tests '*ClickHouseSinkTaskJsonInsertFormatTest' (21 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "14-SECURITY.md exists, is committed, covers every threat ID in 14-01..04-PLAN.md plus one new threat, reports threats_open 0, and records an OSV re-query of all five bumped coordinates with the query time"
    requirement: PUB-03
    verification:
      - kind: other
        ref: "Task 2 automated checks printed AUDIT14-CLEAR and OSV-CLEAN (OSV queried 2026-10-10T10:51Z and 10:57Z)"
        status: pass
      - kind: integration
        ref: "./gradlew test --rerun over Lz4RoundTripLiveTest, NetworkCompressionPinLiveTest, WideIntegerTypeGateLiveTest and four more classes (163 tests, 150 passed, 13 named skips, 0 failures)"
        status: pass
    human_judgment: false
  - id: D3
    description: "No src/main file changed and no file outside .planning changed in this plan"
    requirement: PUB-03
    verification:
      - kind: other
        ref: "git diff --name-only e4755e38 HEAD -- src/main lists only MetadataFreeJsonWriter.java and ClickHouseHelperClient.java (the plan 15-01 files); git diff --name-only 7117ccda HEAD outside .planning is empty"
        status: pass
    human_judgment: false
  - id: D4
    description: "Severity and disposition calls on the two new threats (both left open below the high threshold, neither accepted) are right, and the user wants no change before publication"
    verification: []
    human_judgment: true
    rationale: "Whether an upstream-inherited ERROR log of a record value (T-13-19, medium) or a missing permissions block (T-14-16, low) should be fixed, accepted or left is a risk call for the user at the approval bundle"

duration: 35min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 04: Retroactive security audits of Phases 13 and 14 Summary

**Both retroactive audits (D-09) came back with zero open high threats: 37 plan-time threats re-verified against the tree being published, escaping of untrusted record bytes confirmed by an adversarial probe, and all five bumped dependency versions re-queried clean against OSV on the audit day. Two new below-threshold threats (an upstream-inherited ERROR log of a record value, a missing workflow permissions block) are carried to the approval bundle.**

## Performance

- **Duration:** 35 min
- **Started:** 2026-10-10T10:22:00Z
- **Completed:** 2026-10-10T10:58:00Z
- **Tasks:** 2
- **Files modified:** 2 (both new, both under `.planning/`)

## Accomplishments

- **Phase 13 register: 20 threats, 19 closed, 1 open below `high`, 0 open at or above `high`.** Every ID in `13-01..08-PLAN.md` (T-13-01..18 and T-13-SC) is in `13-SECURITY.md` with a status and file, line and test evidence. T-13-19 is new. The five `high` threats (T-13-01, -03, -05, -07, -09) are all closed on measured evidence: the unit classes (128 tests) and the live classes (21 tests, ClickHouse `latest`, client V2) all pass.
- **MetadataFreeJsonWriter's handling of untrusted bytes was measured, not read.** A probe compiled from the current source into `/tmp` (not committed) fed field names, map keys, bytes values and string values built to break out of a JSON string. The output parsed back to exactly the expected field count, every injected name and value round-tripped, and no raw byte below `0x20` survived. Verbatim bytes at or above `0x80` cannot terminate a string.
- **Phase 14 register: 17 threats, 16 closed, 1 open below `high`, 0 open at or above `high`.** Every ID in `14-01..04-PLAN.md` (T-14-01..15 and T-14-SC) is in `14-SECURITY.md`. T-14-16 is new. The five `high` threats (T-14-05, -08, -11, -12, -13) are closed.
- **OSV re-queried on the audit day, unauthenticated, with positive controls.**

  | Coordinate | Version | Advisories (2026-10-10T10:51Z) |
  |------------|---------|-------------------------------|
  | `at.yawk.lz4:lz4-java` | 1.12.0 | none |
  | `com.fasterxml.jackson.core:jackson-databind` | 2.22.3 | none |
  | `com.squareup.okhttp3:okhttp` | 5.5.0 | none |
  | `org.json:json` | 20260814 | none |
  | `org.mockito:mockito-junit-jupiter` | 5.24.0 | none |

  Controls: `lz4-java` 1.10.4 returned 6 advisories, `jackson-databind` 2.22.2 returned 2, `org.lz4:lz4-java` 1.8.0 returned 8. The plan's own loop printed `OSV-CLEAN` at 10:57Z.
- `dependencyInsight` confirms a single `at.yawk.lz4:lz4-java:1.12.0` headline on `runtimeClasspath` and `clickhouseDependencies`, `jackson-databind:2.22.3` on `runtimeClasspath`, and that `testRuntimeClasspath` still holds `org.lz4:lz4-java:1.8.0` (backlog 999.2, accepted).
- The publication evidence behind T-14-11..14 was re-read live: origin `custom_build` is still `c61d90ce`, origin has no 1.7.0 tag, `push.followTags` is unset, `.planning/.gsd-ws-arg` is untracked, and the REST API returns exactly one green `tests.yaml` run for `54b5ce2f`.

## New threats the audits found

| ID | Phase | Severity | What | Status |
|----|-------|----------|------|--------|
| T-13-19 | 13 | medium | `ClickHouseWriter.java:461` and `:515` log `Error parsing date time string: {}` with the record's raw field value at ERROR when a String cannot be parsed into a DateTime column. Both lines are upstream `v1.7.0` (lines 432 and 486 there), not a Phase 13 change. Contradicts AGENTS.md's no-data-in-logs rule. | open, below `high`, not accepted |
| T-14-16 | 14 | low | `tests.yaml` has no `permissions:` block (upstream `v1.7.0` has none either) and pins actions by mutable tag. Pre-existing; Phase 14 added no `uses:` line. | open, below `high`, not accepted |

Neither counts toward `threats_open`. Neither was downgraded or accepted to reach zero.

## Task Commits

Each task was committed atomically:

1. **Task 1: Retroactive security audit of Phase 13 (D-09)** - `b2c132ad` (docs)
2. **Task 2: Retroactive security audit of Phase 14 (D-09)** - `37be6a06` (docs)

**Plan metadata:** recorded in the following `docs(15-04)` summary and state commits.

## Files Created/Modified

- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-SECURITY.md` - Phase 13 threat register, accepted-risks log (AR-01..06), audit trail, carried-forward notes
- `.planning/phases/14-dependency-bumps-ci/14-SECURITY.md` - Phase 14 threat register, accepted-risks log (AR-01..04), OSV table and positive controls, audit trail

## Decisions Made

- **Skill adaptation.** `/gsd-secure-phase` would spawn `gsd-security-auditor`; this executor has no sub-agent tool, so both audits ran inline per `secure-phase.md` and the Phase 11 precedent. The files are in the shape of `09-SECURITY.md`; this plan's commits replace the skill's commit step.
- **Open below the block threshold, not accepted.** T-13-19 and T-14-16 stay `open`. Accepting them would be the user's risk call, and the plan forbids downgrading a disposition to reach zero. They go into the approval bundle's knowingly-open list.
- **No user decision was taken at a checkpoint**, because no threat at or above `high` was open, so neither blocking-human checkpoint fired.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- A first probe run reported a Jackson parse error on a `0xFF` byte; that is the documented verbatim-bytes behaviour (JSONP-06) tripping Jackson's own UTF-8 reader, not a writer fault. The round-trip assertions were re-run without `0xFF` and passed.
- `timeout(1)` is not on this macOS host, and `zsh` does not word-split `$spec`; both were worked around in the commands, with no effect on results.

## Carried to the approval bundle (15-07)

- T-13-19 and T-14-16 (above), for a user decision if the user wants either changed before publication. Any fix is a `src/main` or CI change, which D-04 reserves for an explicit decision.
- `MetadataFreeJsonWriterTest` pins escaping for byte and string values but not for a field name or map key containing a quote or backslash. The behaviour is correct (measured); a pin would be a test-only addition.
- The OSV verdict is a point-in-time reading (2026-10-10T10:51Z); the bundle should quote the date.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 15-05 (release documents) can cite `threats_open: 0` for both phases; plan 15-07's bundle item 5 has its two measured verdicts.
- No blocker. `src/main` is unchanged by this plan, so the gate's tree is unaffected.

## Self-Check: PASSED

- `13-SECURITY.md` and `14-SECURITY.md` exist on disk and are tracked by git.
- Commits `b2c132ad` and `37be6a06` are ancestors of HEAD; `git rev-list --count 7117ccda..HEAD` was 2 at write time.
- Task verify commands printed `AUDIT13-CLEAR`, `AUDIT14-CLEAR` and `OSV-CLEAN`; no file outside `.planning/` changed.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
