---
phase: 01-test-infrastructure-green-baseline
plan: 01
subsystem: testing
tags: [testcontainers, clickhouse, junit, baseline, testing-infrastructure]

# Dependency graph
requires: []
provides:
  - "ClickHouse `latest` default test image (CLICKHOUSE_VERSION_DEFAULT = latest), CLICKHOUSE_VERSION env override preserved"
  - "ClickHouseServerVersionSmokeTest: versionMatchesRequestedTag() predicate + live container/version assertion"
  - "Confirmed green ./gradlew test baseline against ClickHouse latest x client V2 (189 tests, 0 failed, 7 skipped)"
  - "Documented environment gotcha: TESTCONTAINERS_HOST_OVERRIDE must not be set to a literal string when running Testcontainers-backed gradlew test"
affects: [01-02, 01-03, 02, 03, 04, 05]

# Actuals (#2632)
actuals:
  tokens: 1650
  tasks: 2
  commits: 1
  plan_head_before: 878682fabd9b8fd7abe55093e4e26f06f52baf3e

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Server-version smoke test asserting container image reference + well-formed version string when no image-level version label exists (D-06)"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java

key-decisions:
  - "The untouched suite is genuinely green against ClickHouse latest x client V2 (187 tests, 0 failed, 7 skipped) once a sandbox environment artifact (TESTCONTAINERS_HOST_OVERRIDE=null) is unset — no D-01/D-02/D-04 fixes were needed."
  - "D-06's 'equal the image label' requirement is unsatisfiable for `latest` (no ClickHouse image carries a usable version label — ClickHouse issue #54224); the smoke test instead asserts the container image reference equals CLICKHOUSE_DOCKER_IMAGE and the reported version is well-formed, paired with the RepoDigest in this SUMMARY."

requirements-completed: [INFRA-01, INFRA-02]

coverage:
  - id: D1
    description: "Raw untouched-suite baseline captured against ClickHouse latest x client V2 before any harness change (D-01)"
    requirement: "INFRA-01"
    verification:
      - kind: integration
        ref: "build/baseline/test-latest-v2.log (Tests summary: 187 tests, 180 succeeded, 0 failed, 7 skipped)"
        status: pass
    human_judgment: false
  - id: D2
    description: "CLICKHOUSE_VERSION_DEFAULT flipped to latest; CLICKHOUSE_VERSION env override and CLICKHOUSE_PROXY_VERSION_DEFAULT (23.8) unchanged (D-05)"
    requirement: "INFRA-02"
    verification:
      - kind: unit
        ref: "grep -c 'CLICKHOUSE_VERSION_DEFAULT = \"latest\"' ClickHouseTestHelpers.java == 1"
        status: pass
    human_judgment: false
  - id: D3
    description: "ClickHouseServerVersionSmokeTest: predicate pins numeric-tag and latest edge cases, and a live test asserts container image + logged server version against both latest and 25.3 (D-06)"
    requirement: "INFRA-02"
    verification:
      - kind: integration
        ref: "ClickHouseServerVersionSmokeTest#serverVersionMatchesRequestedImage (latest: 26.8.5.13; 25.3: 25.3.13.19), ClickHouseServerVersionSmokeTest#versionMatchPredicatePinsEdgeCases"
        status: pass
    human_judgment: false
  - id: D4
    description: "Full suite green with only the defaults (no CLICKHOUSE_VERSION/CLIENT_VERSION overrides), @Disabled count unchanged at 7"
    requirement: "INFRA-01"
    verification:
      - kind: integration
        ref: "build/phase1-01-full-test.log (Test result: SUCCESS, Tests summary: 189 tests, 182 succeeded, 0 failed, 7 skipped)"
        status: pass
    human_judgment: false

duration: 40min
completed: 2026-09-16
status: complete
---

# Phase 1 Plan 1: Green Baseline on ClickHouse `latest` x Client V2 Summary

**The untouched `custom_build` suite was already green against ClickHouse `latest` x client V2 (0 failures) once a sandbox environment artifact was neutralized; the default test image now points at `latest`, and a new server-version smoke test pins the container-image and version-string contract for future phases.**

## Performance

- **Duration:** ~40 min
- **Started:** 2026-09-15T21:00:00Z (approx.)
- **Completed:** 2026-09-15T21:40:16Z
- **Tasks:** 2
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments
- Captured the raw baseline of the untouched suite against ClickHouse `latest` x client V2 under `build/baseline/` (gitignored): 187 tests, 180 succeeded, 0 failed, 7 skipped — genuinely green, no D-01/D-02/D-04 fixes required.
- Flipped `CLICKHOUSE_VERSION_DEFAULT` to `"latest"` in `ClickHouseTestHelpers`, keeping the `CLICKHOUSE_VERSION` env override and the `23.8` proxy pin (`CLICKHOUSE_PROXY_VERSION_DEFAULT`) untouched.
- Added `ClickHouseServerVersionSmokeTest` with a pinned `versionMatchesRequestedTag()` predicate (numeric-tag prefix match; `latest` requires a well-formed `MAJOR.MINOR.PATCH.BUILD` string) and a live test asserting the container's image reference equals `ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE` and the logged server version matches the requested tag.
- Ran the full suite with only the defaults (no `CLICKHOUSE_VERSION`/`CLIENT_VERSION` overrides): 189 tests, 182 succeeded, 0 failed, 7 skipped, `Test result: SUCCESS`. The `@Disabled` count is unchanged at 7.

## Task Commits

1. **Task 1: Tracer — capture the raw latest×V2 baseline, flip the default image to latest, prove it end-to-end with the server-version smoke test** - `632688a` (feat)
2. **Task 2: Make the full suite green on latest×V2 under the D-01..D-04 failure policy and record every change** - no code commit required; the baseline was already green, so Task 2 consisted of running the final gate and recording evidence (see Deviations)

**Plan metadata:** committed separately (docs commit, see below)

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java` - new server-version smoke test (D-06, INFRA-02)
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` - `CLICKHOUSE_VERSION_DEFAULT` changed from `"25.3"` to `"latest"` (D-05)

## Baseline

### Raw untouched-suite result (D-01, before any harness change)

- **Command:** `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun` (environment note below applies)
- **Result:** `Test result: SUCCESS` — `Tests summary: 187 tests, 180 succeeded, 0 failed, 7 skipped`
- **Failing tests:** none
- **ClickHouse `latest` server version:** `26.8.5.13`
- **`clickhouse/clickhouse-server:latest` RepoDigest:** `sha256:5a55fb7517cbba97a99b8676a748fd13e4709049a4d1f5d5be5c20695da4c91b`
- **Containers started for `clickhouse/clickhouse-server:latest`:** 14 (across the 14 `ClickHouseBase` subclasses); 2 for `clickhouse/clickhouse-server:23.8` (the two proxy classes, D-07), 2 for `ghcr.io/shopify/toxiproxy:2.7.0`, 6 for `testcontainers/ryuk:0.12.0` (one per fork)
- **Wall-clock duration:** `BUILD SUCCESSFUL in 4m 21s`
- **Raw evidence:** `build/baseline/test-latest-v2.log`, `build/baseline/test-results/` (26 XML files; gitignored)

### D-01 assertion updates

None — the untouched suite is green; no assertion needed updating for `latest`.

### D-02 main-code fixes

None. `git log --format= --name-only --grep='(01-01)' -- src/main > build/01-01-main-changes.txt` produced empty output, confirming no `src/main` files were touched in this plan.

### D-04 flake evidence

None (see Deviations below for the one apparent flake that turned out to be an execution-environment artifact, not a real flake). No `maxParallelForks` change was made; `build.gradle.kts` is untouched by this plan.

### D-06 deviation note

No ClickHouse Docker image carries a usable version label to compare `SELECT VERSION()` against for the `latest` tag (`Dockerfile.ubuntu` has no `LABEL` instructions; ClickHouse issue #54224 confirms `org.opencontainers.image.version`, where present at all, reflects the Ubuntu base OS version, not the ClickHouse semantic version). `ClickHouseServerVersionSmokeTest` therefore asserts, for `latest`: (1) the container's image reference equals `ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE`, and (2) the reported version string is well-formed (`^\d+\.\d+\.\d+\.\d+$`). The actual version (`26.8.5.13`) is recorded above next to the image RepoDigest, making the version-to-digest pairing auditable.

### Final totals (only defaults: no `CLICKHOUSE_VERSION`/`CLIENT_VERSION` in the environment)

- **Command:** `./gradlew test --rerun` (environment note below applies)
- **Result:** `Test result: SUCCESS` — `Tests summary: 189 tests, 182 succeeded, 0 failed, 7 skipped`
- **`@Disabled` count:** 7 (unchanged; matches skipped count)
- **Wall-clock duration:** `BUILD SUCCESSFUL in 4m 21s`
- **Log:** `build/phase1-01-full-test.log` (gitignored)

The final run has 2 more tests than the raw baseline (189 vs 187) because it includes the 2 new `ClickHouseServerVersionSmokeTest` methods.

## Decisions Made

- The suite did not need any D-01 assertion updates, D-02 upstream fixes, or D-04 flake fixes — the untouched code is already correct against ClickHouse `latest` x client V2. This plan's real work was capturing that evidence, flipping the default image, and adding the smoke test.
- D-06's original "equal the image label" requirement is impossible for `latest` given no such label exists; the smoke test uses the closest observable equivalent (image reference + well-formed version), exactly as D-06 itself anticipated when no label exists.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Neutralized a `TESTCONTAINERS_HOST_OVERRIDE=null` execution-environment artifact that caused every ClickHouse-backed test class to fail identically**
- **Found during:** Task 1, Step A (raw baseline capture)
- **Issue:** The very first `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun` run reported `Tests summary: 83 tests, 67 succeeded, 14 failed, 2 skipped`, with all 14 failures being an identical `initializationError`: `java.lang.IllegalStateException: Could not connect to Ryuk at null`. Investigation traced this to the shell environment (this agent's sandbox, inherited by every Bash invocation) carrying `TESTCONTAINERS_HOST_OVERRIDE` set to the **literal string `"null"`** (not unset) — Testcontainers' `RyukResourceReaper` reads this override and tries to connect to a host literally named `null`, which fails for every fork. No `clickhouse/clickhouse-server` containers were ever created in that run (only the 6 `testcontainers/ryuk:0.12.0` containers, one per fork, got as far as being created before the connection-back step failed) — this was never a test-logic, upstream, or resource-contention issue.
- **Fix:** Re-ran all Testcontainers-backed `./gradlew test` invocations in this plan with `TESTCONTAINERS_HOST_OVERRIDE` explicitly unset (`env -u TESTCONTAINERS_HOST_OVERRIDE ...`). To capture an honest D-01 raw baseline against the truly untouched code, the two Step B/C edits (`CLICKHOUSE_VERSION_DEFAULT` and the new smoke-test file) were temporarily reverted/moved aside, the corrected baseline was captured, and the edits were then re-applied identically. No repository file, build config, or test code was changed to work around this — it is purely an artifact of this execution's shell environment, not the project.
- **Files modified:** none (environment-only; no code or config changed to address this)
- **Verification:** With the override unset, the untouched suite reported `Tests summary: 187 tests, 180 succeeded, 0 failed, 7 skipped` and `Test result: SUCCESS` — matching the `@Disabled` count (7) and showing zero genuine test failures.
- **Note for future runs:** if `./gradlew test` reports a mass of identical `Could not connect to Ryuk at <host>` failures across every ClickHouse-backed class, check whether `TESTCONTAINERS_HOST_OVERRIDE` is set to an invalid value in the shell before assuming a code or infrastructure regression. This is not expected to affect CI or a normal developer machine — no evidence was found that this variable is set anywhere in this repository's config, CI workflow, or shell rc files; it appears specific to this execution's sandbox.

---

**Total deviations:** 1 auto-fixed (1 blocking, environment-only — no code changed)
**Impact on plan:** No code, test, or build-config changes were made in response to this deviation. It only affected how the executor invoked `./gradlew test` during this session. The baseline evidence recorded above reflects the corrected (environment-fixed) runs, which is the honest D-01 result for the actual project code.

## Issues Encountered

- See Deviations above — the `TESTCONTAINERS_HOST_OVERRIDE=null` sandbox artifact was the only real issue, and it was resolved by adjusting how test commands were invoked, not by changing the repository.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The baseline for Phases 2-4 is established: `./gradlew test` is green against ClickHouse `latest` x client V2 using only the defaults, with 7 `@Disabled` tests unchanged.
- `ClickHouseServerVersionSmokeTest` is in place and can be reused by later phases as the version-contract check for the shared container once 01-02 introduces the container singleton.
- Plans 01-02 (container singleton, per-class database, `@Tag("clickhouse")`, MBean sweep, UTF-8 helpers) and 01-03 (JaCoCo, `testUnit`) build on this green baseline; no blockers were left behind.
- **Environment note for whoever runs `./gradlew test` next in an equivalent sandbox:** if ClickHouse-backed tests fail en masse with `Could not connect to Ryuk at null`/`<host>`, check `TESTCONTAINERS_HOST_OVERRIDE` in the shell before assuming a regression (see Deviations).

## Self-Check: PASSED

- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java`
- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`
- FOUND: `build/baseline/test-latest-v2.log`
- FOUND: `build/phase1-01-full-test.log`
- FOUND commit `632688a` in `git log --oneline --all`
- Re-ran acceptance criteria: `CLICKHOUSE_VERSION_DEFAULT = "latest"` count = 1; `CLICKHOUSE_PROXY_VERSION_DEFAULT = "23.8"` count = 1; `@Disabled` count = 7; final gate log shows `Test result: SUCCESS` / `Tests summary: 189 tests, 182 succeeded, 0 failed, 7 skipped`

---
*Phase: 01-test-infrastructure-green-baseline*
*Completed: 2026-09-16*
