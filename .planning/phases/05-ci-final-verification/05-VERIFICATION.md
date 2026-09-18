---
phase: 05-ci-final-verification
verified: 2026-09-18T00:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files: [".github/scripts/jacoco_summary.py", ".github/workflows/tests.yaml", ".planning/REQUIREMENTS.md", ".planning/phases/05-ci-final-verification/05-01-PLAN.md", ".planning/phases/05-ci-final-verification/05-01-SUMMARY.md", ".planning/phases/05-ci-final-verification/05-02-PLAN.md", ".planning/phases/05-ci-final-verification/05-02-SUMMARY.md", ".planning/phases/05-ci-final-verification/05-REVIEW.md"]
covered_digest: "v1:sha256:5e04cfce4a0e9d4b1683cffda4db4eca2f12422e71a0e7e613de38e3db636660"
behavior_unverified: 0
overrides_applied: 0
human_verification:

  - test: "Open https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594 and look at the run's Summary tab (landing page of the run, below the job list)."
    expected: "A rendered 'Coverage Summary' heading appears as formatted markdown (not raw pipe-delimited text), followed by a table with a 'Full suite' row and a 'Fork-changed classes' row (each with Instruction/Line/Branch percentages), followed by a 'Fork-changed classes' heading and a 15-row per-class table (a few cells legitimately reading 'n/a')."
    why_human: "GitHub exposes no unauthenticated REST endpoint for a rendered job-summary page (confirmed during planning: /logs requires admin rights, /zip returns 401). The step's SUCCESS conclusion and the script's CONTENT are both machine-proven (see evidence below); only GitHub's actual rendering of that markdown in the Summary tab has never been seen by a human. Roadmap success criterion 3 explicitly requires the workflow 'writes a coverage summary to the job summary' — the write and its content are proven, but rendering is not."
  - test: "Confirm judgment-tier prohibitions declared in 05-01-PLAN.md and 05-02-PLAN.md were not violated: (1) CI execution was not narrowed to get a green badge, (2) the published coverage summary came from the same run's real test execution, (3) no existing test was disabled/deleted/narrowed to pass CI, (4) the CI run report is not attributed via 'most recent run on branch'."
    expected: "Agree/disagree with the verifier's judgment-tier findings below (all four found un-violated on available evidence)."
    why_human: "These are judgment-tier prohibitions (verification: judgment in plan frontmatter) — the verifier's assessment is a non-authoritative LLM-judge verdict, not an authoritative pass. See 'Prohibitions' section below for the evidence each judgment rests on."
---

# Phase 5: CI & Final Verification Verification Report

**Phase Goal:** The fork's CI runs the full test suite on every push against ClickHouse `latest` × client V2 using current actions and publishes coverage, and the whole suite is confirmed green both locally and in CI
**Verified:** 2026-09-18
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `.github/workflows/tests.yaml` uses `actions/checkout@v4`, `actions/setup-java@v6` (temurin 17), `gradle/actions/setup-gradle@v6`, with no deprecated actions left | ✓ VERIFIED | Read file directly: lines 20, 22-26, 28-29 show all three tags with `java-version: '17'`, `distribution: 'temurin'`. Diffed against the pre-Phase-5 file (`git show f68bf1d15be82290d3409e165ec868aee1aa00eb^:...`): old file used `@v3` actions, `distribution: 'adopt'`, and `gradle/gradle-build-action@v2` — all now gone. Banned-token scan for `@v3`/`adopt`/`gradle-build-action` over the current file returns zero matches (independently re-run). |
| 2 | A push to `custom_build` triggers exactly one test job, using `CLICKHOUSE_VERSION=latest` and client V2, with no V1, 23.7, 24.3, or Cloud legs | ✓ VERIFIED | File has no `strategy`/`matrix` key, one job (`build`), `Run tests` step sets `CLICKHOUSE_VERSION: latest` / `CLIENT_VERSION: V2` literally. Independently queried `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594/jobs` — `num_jobs=1`, job name `ClickHouse latest Client version V2 tests`, conclusion `success`. No cloud secrets, no `matrix.` interpolation present (grep confirms zero occurrences comment-stripped). |
| 3 | That CI run is green, uploads the JaCoCo report as an artifact, and writes a coverage summary to the job summary. No coverage threshold can fail the build | ⚠️ Partially machine-verified — see below | Independently queried the live GitHub API (not trusting SUMMARY.md): run `35317272594` `status=completed conclusion=success head_sha=a295af8aacef6c5c3ad0b49abe89a3a85ca4efa2` (matches `origin/custom_build` tip, confirmed via `git ls-remote`), `path=.github/workflows/tests.yaml`. Jobs listing: 1 job, `success`; step `Generate coverage summary`=`success`, step `Run tests`=`success`. Artifacts listing: `jacoco-coverage-reports` 618613 bytes `expired=false`, `gradle-test-reports` 1912272 bytes `expired=false`. Local content proof independently re-run: `GITHUB_STEP_SUMMARY`-pointed run of `jacoco_summary.py` renders the two headline rows and all 15 fork-class rows in both destinations. `grep -in "coverageVerification\|minimum\|threshold" build.gradle.kts` returns nothing — no coverage gate anywhere. **The one sub-claim no unauthenticated API can prove is whether GitHub actually rendered that markdown in the run's Summary tab** — routed to human verification below, per the plan's own DEC-D and `workflow.human_verify_mode=end-of-phase`. |
| 4 | With colima running, `./gradlew test` passes in full locally (fork and upstream tests) against `latest` × V2, and the final report shows coverage for the fork-changed classes | ✓ VERIFIED | Independently aggregated `build/test-results/test/TEST-*.xml` (43 classes): `tests=371 failures=0 errors=0 skipped=7` — matches Phase 4's 371-test baseline exactly, ruling out a partial run. Independently parsed `build/reports/jacoco/fork/jacocoForkReport.xml`: covered LINE counts `TableOperationFilter=57`, `HeadersToValue=74`, `HeaderToValue=77`, `ConvertToMapOrJson=108` — all non-zero, the signature of a genuine full run (the stale pre-plan report read 0.0% on these four). |

**Score:** 4/4 truths verified (0 present, behavior-unverified); 1 truth (#3) carries a residual human-verification item for the GitHub-rendering sub-claim that no unauthenticated API can prove — this does not fail the truth (content and execution are both machine-proven), it routes the one unprovable-by-API sub-claim to a human check per Step 8/9.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/scripts/jacoco_summary.py` | JaCoCo XML → job-summary markdown renderer, stdlib only, 15-row fork-class table, ≥45 lines | ✓ VERIFIED | 131 lines, ASCII-only (independently re-verified byte scan), contains `FORK_CLASSES_IN_ORDER` (15 entries, matches `build.gradle.kts` `forkChangedClasses` base names), `pct()` handles `None`→`n/a` and zero-total→`0.0%`, `render_markdown()` exits non-zero naming the missing path when either report is absent, writes stdout + `GITHUB_STEP_SUMMARY` unconditionally (DEC-B — no pipe). |
| `.github/workflows/tests.yaml` | Single-job `latest` × V2 workflow on current actions, publishing coverage, no gate | ✓ VERIFIED | 56 lines. One job, `timeout-minutes: 45`, header comment naming fork/cloud/REQUIREMENTS.md (D-04), `concurrency` block with `cancel-in-progress: true` (D-14), steps in the exact order the plan specifies, `Generate coverage summary` step invokes the script with no pipe. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `.github/workflows/tests.yaml` | `.github/scripts/jacoco_summary.py` | `Generate coverage summary` step invokes by path | ✓ WIRED | `run: python3 .github/scripts/jacoco_summary.py` present verbatim (line 39). |
| `.github/scripts/jacoco_summary.py` | `build/reports/jacoco/fork/jacocoForkReport.xml` | `FORK_REPORT_PATH` constant, parsed via ElementTree | ✓ WIRED | Constant matches Phase 1's `jacocoForkReport` task output path exactly; confirmed the file exists and was parsed with non-zero data. |
| `.github/workflows/tests.yaml` | `ClickHouseTestHelpers.java` / `ClickHouseBase.java` | `CLICKHOUSE_VERSION` / `CLIENT_VERSION` env vars | ✓ WIRED | `ClickHouseTestHelpers.java:55` reads `System.getenv("CLICKHOUSE_VERSION")`; `ClickHouseBase.java:109` reads `System.getenv("CLIENT_VERSION")` — both names match the workflow's `env:` block exactly. |

### Behavioral Spot-Checks / Probe Execution

| Check | Command | Result | Status |
|-------|---------|--------|--------|
| Coverage script runs end-to-end against real reports | `GITHUB_STEP_SUMMARY=<tmp> python3 .github/scripts/jacoco_summary.py` (re-run independently, not trusting SUMMARY) | Exit 0; headline rows + 15/15 fork-class rows present in both stdout and the temp file | ✓ PASS |
| Local full-suite aggregation | Parsed `build/test-results/test/TEST-*.xml` directly (not re-running the 6.5-minute suite; used the orchestrator's freshly-produced results, cross-checked independently) | 371 tests, 0 failures, 0 errors, 7 skipped, 43 classes | ✓ PASS |
| Fork coverage non-zero on the 4 signature classes | Parsed `jacocoForkReport.xml` directly | `TableOperationFilter=57 HeadersToValue=74 HeaderToValue=77 ConvertToMapOrJson=108` | ✓ PASS |
| Live CI run status (independent API call, not trusting SUMMARY.md) | `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594` | `status=completed conclusion=success head_sha` matches pushed SHA and `origin/custom_build` tip | ✓ PASS |
| Live CI job/step conclusions (independent API call) | `GET .../runs/35317272594/jobs` | 1 job `success`; steps `Run tests`=`success`, `Generate coverage summary`=`success` | ✓ PASS |
| Live CI artifacts (independent API call) | `GET .../runs/35317272594/artifacts` | `jacoco-coverage-reports` 618613B, `gradle-test-reports` 1912272B, both `expired=false` | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| CI-01 | 05-01 | Current actions (`checkout@v4`, `setup-java@v6` temurin 17, `setup-gradle@v6`) | ✓ SATISFIED | Truth #1 above; independently diffed against pre-Phase-5 workflow. |
| CI-02 | 05-01 | Single job on push, `latest` × V2, no other legs | ✓ SATISFIED | Truth #2 above; independently confirmed via live API job count = 1. |
| CI-03 | 05-01, 05-02 | JaCoCo artifact + job-summary coverage, no threshold | ✓ SATISFIED (content/execution); ⚠️ rendering not independently observable | Truth #3 above; one sub-claim routed to human verification. |
| CI-04 | 05-02 | `./gradlew test` passes in full locally and in CI on `latest` × V2 | ✓ SATISFIED | Truth #4 above plus independently-confirmed CI job success. |

All four requirement IDs declared in the phase's PLAN frontmatter (`CI-01, CI-02, CI-03` in 05-01; `CI-03, CI-04` in 05-02) are accounted for; no orphaned requirements found for Phase 5 in `REQUIREMENTS.md`'s traceability table.

### Prohibitions (judgment tier — non-authoritative LLM-judge verdict, human review recommended)

| # | Prohibition | Verifier judgment | Evidence |
|---|-------------|--------------------|----------|
| 1 | MUST NOT narrow what CI executes to obtain a green badge | Not violated (judgment) | `Run tests` step is exactly `./gradlew test` — the full suite task, not `testUnit` or a filtered subset; no `--tests` filter, no `continue-on-error` anywhere in the file (grep confirms). |
| 2 | MUST NOT publish a coverage summary not from the same run's real execution | Not violated (judgment) | `Generate coverage summary` runs in the same job, after `Run tests`, with `if: always()`; `jacocoTestReport`/`jacocoForkReport` are `finalizedBy(test)` per Phase 1, so the XML consumed is this run's own output, not a cached/copied file. |
| 3 | MUST NOT disable/delete/narrow an existing test to pass CI | Not violated (judgment) | Phase 5's commits (`666f585`, `73b2c71`, and the push delta) touch only `.github/workflows/tests.yaml` and `.github/scripts/jacoco_summary.py` — independently confirmed via `git show --stat` and `git diff --stat`; zero `src/` or test-file changes. Local test count (371) matches the Phase 4 baseline exactly, no reduction. |
| 4 | MUST NOT report on a run located by "most recent on branch" rather than the pushed SHA | Not violated (judgment) | Independently re-queried `/actions/runs/35317272594` and confirmed `head_sha` equals the pushed commit and `origin/custom_build`'s current tip via `git ls-remote`. |

These are judgment-tier assessments per plan frontmatter (`verification: judgment`) — recorded as this verifier's non-authoritative reading of the evidence; final resolution belongs to the human at the end-of-phase checkpoint (see `human_verification` above).

### Anti-Patterns Found

None blocking. Code review (`05-REVIEW.md`, committed `40737eb`) found 0 critical, 3 warnings, 2 info — all advisory, none of which affect goal achievement:

- **WR-01** (warning): the new `concurrency:` group key references `github.event.number` guarded by `github.event_name == 'pull_request'`, which can never be true under `on: [push]` — the ternary always resolves to `github.sha`, so `cancel-in-progress` never actually cancels a superseded run on the same branch (every push has a unique SHA), and in the unlikely case two branches/tags share a SHA it risks cross-branch cancellation. Does not affect the phase's success criteria (CI still runs and completes as required) — recorded as a real defect worth a follow-up fix (`group: ${{ github.workflow }}-${{ github.ref }}`).
- **WR-02** (warning): `jacoco_summary.py` renders an all-`n/a` table and still exits 0 if the hand-duplicated `FORK_CLASSES_IN_ORDER` list ever drifts from `build.gradle.kts`'s `forkChangedClasses` (e.g., a future class rename) — a real future silent-failure risk, not a defect in the current, verified-matching state.
- **WR-03**, **IN-01**, **IN-02**: minor/defensive-code observations, no functional impact today.

No debt markers (`TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER`) found in either phase-touched file (independently re-grepped).

### Human Verification Required

See `human_verification` frontmatter above for the two items:

1. Visual confirmation that GitHub rendered the Coverage Summary markdown on the run's Summary tab (the one CI-03 sub-claim no unauthenticated API can return).
2. Human sign-off on the four judgment-tier prohibition findings above.

### Gaps Summary

No gaps. All four roadmap success criteria have strong machine evidence — independently re-derived in this verification pass, not taken from SUMMARY.md claims: the workflow file was read and diffed against its pre-Phase-5 predecessor; the pushed SHA, run, jobs, steps, and artifacts were re-queried live against `api.github.com` in this session (not merely re-stated from 05-02-SUMMARY.md); the local test-results XML and JaCoCo fork report were re-parsed directly from disk. The only item that cannot be closed by any automated check — GitHub's actual rendering of the job-summary markdown — is exactly the item the plan itself (DEC-D) identified as requiring one scoped human glance, and it is correctly routed here rather than silently assumed passed.

---

_Verified: 2026-09-18_
_Verifier: Claude (gsd-verifier)_
