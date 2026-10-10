---
phase: 14-dependency-bumps-ci
verified: 2026-10-10T09:30:00Z
status: passed
score: 4/4 roadmap success criteria verified (plus all plan-level must-haves)
covered_files:
  - ".github/workflows/tests.yaml"
  - ".planning/phases/14-dependency-bumps-ci/14-01-PLAN.md"
  - ".planning/phases/14-dependency-bumps-ci/14-01-SUMMARY.md"
  - ".planning/phases/14-dependency-bumps-ci/14-02-PLAN.md"
  - ".planning/phases/14-dependency-bumps-ci/14-02-SUMMARY.md"
  - ".planning/phases/14-dependency-bumps-ci/14-03-PLAN.md"
  - ".planning/phases/14-dependency-bumps-ci/14-03-SUMMARY.md"
  - ".planning/phases/14-dependency-bumps-ci/14-04-PLAN.md"
  - ".planning/phases/14-dependency-bumps-ci/14-04-SUMMARY.md"
  - "gradle/libs.versions.toml"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java"
covered_digest: "v3:sha256:c3413c2514d33a74e85018b2502389eca8fc2abd9c124f00f5a978d1f5754685"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 14: Dependency Bumps & CI Verification Report

**Phase Goal:** The approved dependency bumps land on the hardened tree one attributable commit at a time, so a regression points at exactly one version change. CI checks what a local run checks: integration sources compile, and the fork count is measured. A published SHA gets exactly one run.
**Verified:** 2026-10-10
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | lz4-java on `runtimeClasspath` is advisory-free and past 1.10.4; client 0.9.6 works with it (compile, testUnit, live LZ4 round-trip on latest) | VERIFIED | `gradle/libs.versions.toml` has `lz4Java = "1.12.0"`. I ran `dependencyInsight` on `runtimeClasspath`. It shows a single headline `at.yawk.lz4:lz4-java:1.12.0`. `client-v2:0.9.6` (asks for 1.10.2) and kafka-clients (asks for `org.lz4:lz4-java:1.8.0`) both resolve to it. `Lz4RoundTripLiveTest` exists (179 lines). It builds its own client with `compressClientRequest(true)`, asserts the `LZ4Factory` code source, reads rows back through typed getters, and asserts `CompressedReadBufferBlocks > 0` from `system.query_log`. Its XML in `build/test-results/test` reports tests=2, skipped=0, failures=0, errors=0. The 1.11.4 floor correction is in REQUIREMENTS.md line 84, and 1.12.0 is above it. |
| 2 | Jackson 2.22.3 is in its own commit with JSON-route tests green. okhttp 5.5.0, org.json 20260814 and mockito 5.24.0 are one-line version-edit commits, each gated by `compileIntegrationTestJava` and `testUnit` | VERIFIED | The catalog holds jackson 2.22.3, okhttp 5.5.0, orgJson 20260814 and mockito 5.24.0. `git show` of `cf448fcb` (lz4), `60006d49` (okhttp), `7c85dc73` (mockito), `32e91452` (jackson) and `7bdf8328` (org.json) shows each touches only `gradle/libs.versions.toml` with a 1-line diff. Each diff changes exactly the intended version. `git diff 57473598..HEAD` outside `.planning` touches only `tests.yaml`, the catalog and the new `Lz4RoundTripLiveTest`. Nothing under `src/main` and no `build.gradle.kts` change. MERGE-CHECKLIST Facts 33-36 record the gates: Jackson full run 805 tests, 0 failures, 0 errors, 18 skipped, equal to FORK-DELTA's named skips. JSON-route classes are all above 0 tests, including `MetadataFreeJsonTypeParityLiveTest` 8/0/0/0. The org.json run is identical, with 152 testcases in the D-06 before/after comparison and an empty diff. Server version is recorded. |
| 3 | `tests.yaml` compiles integration sources on every push. The fork count is read from a log, summary or annotation of a run on this tree | VERIFIED | `tests.yaml` has a "Compile integration-test sources" step (`./gradlew compileIntegrationTestJava`) before "Run tests". The "Report test fork count" step writes the log, `$GITHUB_STEP_SUMMARY` and a `::notice title=Test fork count`. I ran the step body against stub `nproc` values 1, 3, 4 and 8, with `CLICKHOUSE_CLUSTER_NAME` unset and set to empty. Results: nproc 1, 3 -> 1; 4 -> 2; 8 -> 4; empty cluster name -> 1. This matches the spec. I re-queried the GitHub REST API unauthenticated. Run 38037397969, job 114170598071: step 5 "Report test fork count", step 6 "Compile integration-test sources" and step 7 "Run tests" all concluded success. The check-run annotation reads `runner nproc=4, derived maxParallelForks=2`. |
| 4 | A branch push and a tag push on the same SHA yield exactly one run. The fix is committed before any tag reaches origin | VERIFIED | `tests.yaml` now has `on: push: branches: ['**']` with no `tags` key, so a tag-only push starts no run. The SHA-keyed concurrency group is unchanged. The fix is in `80f4d6b7`, an ancestor of HEAD. `git ls-remote origin` shows tags only `pre-v1.6.0-sync` and `v1.6.0_gg1`, and `custom_build` still at `c61d90ce`. No tag was pushed. `actions/workflows/tests.yaml/runs?head_sha=54b5ce2f...` returns total_count=1: run 38037397969, event push, conclusion success, so no cancelled sibling. The tag-plus-branch-on-one-SHA case is not run end to end here. By the roadmap, Phase 15 proves it on the published SHA. |

**Score:** 4/4 roadmap criteria verified, 0 behavior-unverified.

### Plan-level must-haves (spot checks)

| Must-have | Status | Evidence |
|-----------|--------|----------|
| 14-01: fork anchor `timeout-minutes: 45` counts exactly 1 | VERIFIED | `grep -c` gives 1 |
| 14-01: no Phase 14 commit touches `src/main` | VERIFIED | `git diff --stat 57473598 HEAD -- src/main build.gradle.kts` is empty |
| 14-02: `clickHouseDriver` 0.9.6 and kafka 4.1.1 unchanged | VERIFIED | The only catalog lines changed since the phase started are the five bumps (per-commit diffs above) |
| 14-02: test-classpath duplicate recorded as a ROADMAP backlog item | VERIFIED | ROADMAP Phase 999.2 (BACKLOG) |
| 14-02: okhttp is compile-checked only (backstop residual) | VERIFIED (recorded) | MERGE-CHECKLIST Fact 34 states "runtime behaviour is unexercised" |
| 14-03: jackson-databind 2.22.3 on runtimeClasspath | VERIFIED | `dependencyInsight` printed `jackson-databind:2.22.3 (by constraint)` |
| 14-03: Jackson commit and org.json commit adjacent, with only the catalog changed in between | VERIFIED | Recorded Fact 36's `git diff --quiet` check. Commit history shows only a docs commit between them |
| 14-04: Phase 12 D-01 amendment present | VERIFIED | 12-CONTEXT.md line 36, "Amended by Phase 14 D-13" |
| 14-04: push was branch-only, with the amendment included | VERIFIED | origin `sync/v1.7.0` = `54b5ce2f`, and that SHA is the D-13 amendment commit and an ancestor of HEAD. The tag list on origin has only the two pre-existing tags. |
| 14-04: CI-08 branch-ordering, CI-09 precision and `pre-v1.7.0-sync` residuals are recorded | VERIFIED (recorded) | Recorded Fact 37 |
| Merge-invariant script passes | VERIFIED | `verify-merge-invariants.sh 54e63648 HEAD` -> `result=PASS` |

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `.github/workflows/tests.yaml` | VERIFIED | Substantive, and exercised by a real runner |
| `gradle/libs.versions.toml` | VERIFIED | Five bumps; `lz4-java` alias feeds the runtime classpath (shown by `dependencyInsight`) |
| `src/test/.../Lz4RoundTripLiveTest.java` | VERIFIED | Real assertions, not a stub; passing XML is present |
| `.planning/MERGE-CHECKLIST-v1.7.0.md` Facts 33-37 | VERIFIED | Present and consistent with the code and with the API re-check |
| `.planning/REQUIREMENTS.md` correction | VERIFIED | Dated Phase 14 correction at line 84 |

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| Fork-count step | `build.gradle.kts` `maxParallelForks` formula | WIRED | Boundary behavior reproduced locally |
| `lz4Java` catalog entry | runtimeClasspath | WIRED | Resolved to 1.12.0 via the capability rule |
| `tests.yaml` branch push | one run per SHA | WIRED | The API shows 1 run for the pushed SHA |
| Fork-count notice | check-run annotation | WIRED | Annotation read unauthenticated |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Fork-count formula | Step body with a stub `nproc` | 1/1/2/4, and 1 with an empty cluster name | PASS |
| lz4 resolution | `./gradlew --offline dependencyInsight --dependency lz4 --configuration runtimeClasspath` | single `at.yawk.lz4:lz4-java:1.12.0` | PASS |
| Remote CI run | GitHub REST API | 1 run, success, with expected steps and annotation | PASS |
| Merge invariants | `verify-merge-invariants.sh 54e63648 HEAD` | PASS | PASS |

I did not re-run the full suite. The orchestrator reports 805 tests, 0 failures, 0 errors and 18 skipped on HEAD. The XML kept in `build/test-results` agrees with the Lz4 test result.

### Probe Execution

None declared. Step 7c does not apply.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| BLD-10 | 14-01, 14-02 | lz4-java past 1.10.4 and advisory-free, shown working with client 0.9.6 | SATISFIED | 1.12.0 resolved; live round-trip green; floor corrected to 1.11.4 |
| BLD-11 | 14-03 | Jackson 2.22.3 in its own commit, JSON-route tests green | SATISFIED | `32e91452`; Fact 35 |
| BLD-12 | 14-02, 14-03 | okhttp, org.json and mockito applied as version edits, one commit each, gated | SATISFIED | `60006d49`, `7bdf8328`, `7c85dc73`; Facts 34, 36 |
| CI-08 | 14-01, 14-04 | One completed run per published SHA; fix before any tag | SATISFIED | Branch-only trigger; 1 run for the pushed SHA; no tag on origin. The tag path itself gets its end-to-end proof in Phase 15 (PUB-04). |
| CI-09 | 14-01, 14-04 | Fork count read from CI or recorded as unmeasured | SATISFIED | Annotation reads nproc=4, forks=2 |
| CI-10 | 14-01, 14-04 | CI compiles integration sources | SATISFIED | Step 6 success on a real runner, ordered before the tests |

No orphaned requirements: REQUIREMENTS.md maps exactly BLD-10, BLD-11, BLD-12, CI-08, CI-09 and CI-10 to Phase 14. All six appear in plan frontmatter.

### Anti-Patterns Found

No TBD, FIXME, XXX or TODO markers in the modified files. Three notes, none blocking:

| Item | Severity | Impact |
|------|----------|--------|
| ROADMAP.md line for Phase 14 in the phase list still shows `[ ]` although the plans are all `[x]` and REQUIREMENTS are Complete | Info | Bookkeeping; the orchestrator's phase-complete step should flip it |
| CI-09 value derives from `nproc`, not Gradle's actual `availableProcessors()` | Info | Recorded as a residual; it does not affect the criterion ("read from a log or annotation") |
| Open advisory items in 14-REVIEW.md (0 critical, 2 warnings) | Info | Advisory only; dispositions are tracked in 14-REVIEW-DISPOSITION.md |

### Human Verification Required

None. The push approval that needed a human decision was given at the D-13 checkpoint, and I confirmed the resulting evidence independently.

### Gaps Summary

No gaps. Every roadmap success criterion is backed by code, resolved dependency state, or remote CI evidence that I re-derived. I did not rely on the SUMMARY claims. The bump commits are atomic one-line catalog edits, and nothing outside the catalog, workflow and one new test file changed. The only criterion-4 element not proven here is the tag-plus-branch end-to-end case. The plan and roadmap assign that to Phase 15, and the trigger configuration that makes it work is in place and committed before any tag reached origin.

---

_Verified: 2026-10-10_
_Verifier: Claude (gsd-verifier)_
