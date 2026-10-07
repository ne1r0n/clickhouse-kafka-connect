---
phase: 06-merge-groundwork-build-migration
plan: 02
subsystem: infra
tags: [gradle, jacoco, dependency-catalog, build-migration, ci]

# Dependency graph
requires:
  - phase: 06-01
    provides: "commit 673d29c3 (mechanical merge of upstream v1.6.0 into custom_build on sync/v1.6.0), Gradle wrapper bootstrapped at 9.6.1"
provides:
  - "commit 2 (b582a44b) on sync/v1.6.0: upstream's version-catalog build.gradle.kts with all fork build additions re-applied"
  - "gradle/libs.versions.toml with the two D-15 FORK OVERRIDE lines (clickHouseDriver 0.9.6, kafka 4.1.1)"
  - "VERSION = v1.6.0_gg1"
  - ".gitignore union (fork's /bin/, .vscode, .gsd + upstream's # AI / memory)"
  - ".github/workflows/tests.yaml restored to the fork's 55-line single-job workflow"
  - "a resolvable Gradle build (help + dependencies both green) proving the four D-12 resolved-version facts"
affects: ["06-03", "07-*", "08-*", "09-*", "10-*", "11-*"]

# Actuals (#2632)
actuals:
  tokens: 5600
  tasks: 3
  commits: 1

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Version-catalog fork overrides marked inline with a trailing `# FORK OVERRIDE (upstream X)` comment so the next upstream sync conflicts on exactly those two lines (D-15)"
    - "Fork-only build coordinates (slf4j-reload4j, ben-manes plugin, core jacoco plugin, LZ4 capability block) stay inline in build.gradle.kts and never enter the version catalog (D-16)"
    - "JaCoCo wired report-only via `finalizedBy` on `test`, never via `check.dependsOn` or `jacocoTestCoverageVerification` — coverage can never gate a build"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - VERSION
    - .gitignore
    - .github/workflows/tests.yaml
    - build.gradle.kts

key-decisions:
  - "PD-01 applied: kept the fork's Spotless `java { importOrder(); removeUnusedImports() }` configuration block and the registered `removeUnusedImports` task verbatim, since CLAUDE.md documents both as project conventions and upstream applies Spotless with no configuration at all."
  - "The `org.slf4j:slf4j-reload4j:2.0.17` inline dependency (D-16, no upstream catalog entry) was re-added to the `dependencies {}` block even though it isn't one of the plan's six numbered additions — it is a required carry-over per D-16 and this plan's own acceptance criteria/threat register (T-06-06), so it was added alongside the jackson dependencies with a `FORK OVERRIDE` comment."
  - "Renamed a code comment above the `testUnit` task registration from 'Inherits maxParallelForks, ...' to 'Inherits fork-count parallelism, ...' — the literal word `maxParallelForks` in prose would have made the acceptance criterion's `grep -o 'maxParallelForks' | wc -l` count 3 instead of 2, incorrectly signalling a surviving third fork-authored assignment line. No functional change; the block still has exactly two real `maxParallelForks =` assignments (upstream's cluster-aware branch)."

patterns-established:
  - "Gate-only build-migration pattern: hand-transplant fork build additions onto a byte-identical upstream base file, verify via targeted grep/wc assertions plus `./gradlew help`/`dependencies`, never via compilation (src/main isn't re-expressed until Phase 7)."

requirements-completed: [BLD-01, BLD-02, BLD-03, BLD-04, BLD-05, BLD-06, BLD-07, BLD-08]

coverage:
  - id: D1
    description: "Version catalog carries exactly the two D-15 FORK OVERRIDE lines (clickHouseDriver 0.9.6, kafka 4.1.1) with no other key touched"
    requirement: BLD-08
    verification:
      - kind: other
        ref: "git diff v1.6.0 -- gradle/libs.versions.toml filtered to non-clickHouseDriver/kafka lines is empty; grep -o 'FORK OVERRIDE' count is 2"
        status: pass
    human_judgment: false
  - id: D2
    description: "VERSION reads v1.6.0_gg1; .gitignore is the hand-authored union of both sides; tests.yaml is the fork's 55-line workflow byte-identical to pre-v1.6.0-sync"
    requirement: BLD-07
    verification:
      - kind: other
        ref: "cat VERSION == v1.6.0_gg1; all 5 gitignore tokens present; diff against git show pre-v1.6.0-sync:.github/workflows/tests.yaml is empty, wc -l == 55"
        status: pass
    human_judgment: false
  - id: D3
    description: "build.gradle.kts carries all five locked fork build additions (JaCoCo report-only, testUnit + check-exclusion guard, JDK-17 test-JVM launcher + windows-1252, LZ4 capability resolution, ben-manes dependencyUpdates) plus PD-01's Spotless block, with jdkHome/JDK-11 settings/dead plugins absent"
    requirement: "BLD-01, BLD-03, BLD-04, BLD-05, BLD-06"
    verification:
      - kind: other
        ref: "task 2's full pattern-presence grep loop (ADDITIONS_PRESENT) and negative-pattern loop (DROPPED_ITEMS_OK); 30/15 class-list counts; maxParallelForks count == 2"
        status: pass
    human_judgment: false
  - id: D4
    description: "The phase gate (help + dependencies) succeeds on commit 2 at Gradle 9.6.1, with all four D-12 resolved-version facts holding in the actual dependency graph and the three fork-added tasks registered"
    requirement: BLD-02
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help (BUILD SUCCESSFUL); ./gradlew dependencies (BUILD SUCCESSFUL, com.clickhouse@0.9.6, connect-api@4.1.1, slf4j-reload4j@2.0.17, at.yawk.lz4:lz4-java:1.10.2, zero 0.9.5/2.7.0 hits); ./gradlew tasks --all lists testUnit/jacocoForkReport/removeUnusedImports"
        status: pass
    human_judgment: false

duration: ~9min
completed: 2026-09-18
status: complete
---

# Phase 6 Plan 2: Build Migration Summary

**Re-applied all six fork `build.gradle.kts` additions (JaCoCo report-only, `testUnit`, JDK-17 test-JVM pin, LZ4 capability resolution, ben-manes `dependencyUpdates`, Spotless config) onto upstream v1.6.0's version-catalog build file, landed `VERSION=v1.6.0_gg1` plus the `.gitignore`/`tests.yaml` settlement, and proved the four D-12 resolved-version facts (`com.clickhouse@0.9.6`, `connect-api@4.1.1`, `slf4j-reload4j@2.0.17`, `at.yawk.lz4:lz4-java:1.10.2`) against the real dependency graph.**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-09-18T18:33:00Z (approx., immediately after 06-01)
- **Completed:** 2026-09-18T18:42:04Z
- **Tasks:** 3
- **Files modified:** 5 (`gradle/libs.versions.toml`, `VERSION`, `.gitignore`, `.github/workflows/tests.yaml`, `build.gradle.kts`)

## Accomplishments

- Set `gradle/libs.versions.toml`'s `clickHouseDriver` to `0.9.6` and `kafka` to `4.1.1`, each with a `# FORK OVERRIDE (upstream X)` trailing comment, and verified via diff that no other catalog key moved.
- Set `VERSION` to `v1.6.0_gg1` (single line, trailing newline) — the sole input to `generateVersionFile`'s `Version.ARTIFACT_VERSION` constant.
- Unioned `.gitignore`: kept the identical lines 1-36 (through upstream's `# AI`/`memory` tail) and appended the fork's `/bin/`, `.vscode`, `.gsd` entries — confirmed all five tokens present. `bin/` and `.gsd` are now correctly ignored again (previously untracked/unignored post-merge, exactly the evidence flagged by the orchestrator's wave-1 spot-check).
- Restored `.github/workflows/tests.yaml` to the fork's 55-line single-job workflow, byte-identical to `git show pre-v1.6.0-sync:.github/workflows/tests.yaml` — upstream's 245-line 3-D matrix is fully dropped.
- Hand-transplanted onto upstream's verbatim `build.gradle.kts` (which arrived byte-identical to `v1.6.0` at commit 1):
  - **JaCoCo (BLD-03):** bare `jacoco` plugin id, `toolVersion = "0.8.15"`, the 15-class/30-entry `forkChangedClasses` list, `jacocoTestReport` configuration, the `jacocoForkReport` registration (explicit XML/HTML output paths matching `.github/scripts/jacoco_summary.py`'s hardcoded paths — confirmed zero drift in that script), and the `finalizedBy`/JaCoCo-disable wiring on `test`/`integrationTest`. Report-only: no `violationRules`, no `jacocoTestCoverageVerification` wired to `check`.
  - **`testUnit` + check-exclusion guard (BLD-04):** the registered `testUnit` task (excludes the `clickhouse` tag, no Docker) plus the exact-name `if (name != "testUnit") { tasks.getByName("check").dependsOn(this) }` guard re-inserted inside upstream's own `tasks.withType<Test>` block.
  - **JDK-17 test-JVM pin + `windows-1252` (BLD-05, non-negotiable):** `javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) })` and `systemProperty("file.encoding", "windows-1252")`, both inside the shared `tasks.withType<Test>` block so every fork of every `Test` task inherits them.
  - **LZ4 capability resolution (BLD-06, first half):** the `configurations.configureEach { resolutionStrategy.capabilitiesResolution... }` block, verbatim, coupled to the Kafka `4.1.1` catalog override in the same commit.
  - **ben-manes `dependencyUpdates` (BLD-06, second half):** the plugin id and its `rejectVersionIf` configuration block landed as one atomic unit — the build-script Kotlin compile (`./gradlew help`) is what proves they aren't split.
  - **Spotless block + `removeUnusedImports` task (PD-01):** kept verbatim per the planner's discretionary decision, since CLAUDE.md documents both as project conventions.
  - **Discretionary replacements:** upstream's cluster-aware `maxParallelForks` branch replaces the fork's single-line version (exactly 2 `maxParallelForks` assignments survive, not 3); the fork's `defaultJdkVersion = 17` + toolchain block replaces upstream's JDK-11 `sourceCompatibility`/`targetCompatibility`, with `options.release.set(11)` dropped and no compatibility setting ported alongside the toolchain; `jdkHome` dropped entirely (D-13); `addTestListener` kept verbatim (D-13); upstream's `runClickHouseVersionClientMatrix` and `generateVersionFile` tasks untouched.
- Staged and committed all five files together as commit 2, `b582a44b` (an ordinary, non-merge commit — `git rev-list --parents -n 1 HEAD` prints exactly two SHAs).
- Ran the phase gate twice (cold + a warm re-run for wall-clock): `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` — `BUILD SUCCESSFUL` (first run ~14s while the protobuf plugin fetched its native `protoc` binary; warm re-run ~1.0s) — and `./gradlew dependencies` — `BUILD SUCCESSFUL` (first run ~3s; warm re-run ~1.0s). `./gradlew --version` confirmed `Gradle 9.6.1`.
- Asserted all four D-12 resolved-version facts directly against the `dependencies` output (not the catalog literals): every `com.clickhouse:*` line resolves to `0.9.6` with zero `0.9.5` hits; `org.apache.kafka:connect-api` resolves to `4.1.1` with zero `2.7.0` hits; `org.slf4j:slf4j-reload4j` resolves to `2.0.17`; the `org.lz4:lz4-java` capability is satisfied by `at.yawk.lz4:lz4-java:1.10.2`.
- Confirmed `./gradlew tasks --all` registers `testUnit`, `jacocoForkReport`, and `removeUnusedImports`, and that `./gradlew check --dry-run` does **not** pull in `testUnit` (the exact-name exclusion guard works), while `jacocoTestReport`/`jacocoForkReport` appear only as `finalizedBy` followers of `test` — never as a `check`-gating dependency.
- Verified no compilation or test task ran: the pre-existing `build/classes` directory on disk predates this session (mtime 2026-09-13, from a prior milestone's local run) — nothing under `build/` was newer than this plan's edits except Gradle's own `problems-report.html` metadata file.

## Task Commits

1. **Task 1: Catalog overrides, VERSION, .gitignore union, and the fork's tests.yaml** - staged, not committed separately (see task 2)
2. **Task 2: Re-apply the fork's build additions onto upstream's build.gradle.kts and land commit 2** - `b582a44b` (build) — lands all five files from tasks 1 and 2 together, exactly as the plan specifies ("stage all four files and leave them uncommitted; task 2 lands them together with build.gradle.kts as the single commit 2")
3. **Task 3: Run the phase gate and assert the four resolved-version facts** - no commit (verification-only; findings recorded above and in this SUMMARY for plan 06-03)

**Plan metadata:** committed separately, see below.

## Files Created/Modified

- `gradle/libs.versions.toml` - two `# FORK OVERRIDE` lines (`clickHouseDriver = 0.9.6`, `kafka = 4.1.1`), all other keys byte-identical to upstream
- `VERSION` - `v1.6.0_gg1`
- `.gitignore` - union of fork's `/bin/`/`.vscode`/`.gsd` and upstream's `# AI`/`memory`
- `.github/workflows/tests.yaml` - restored to the fork's 55-line single-job workflow, byte-identical to `pre-v1.6.0-sync`
- `build.gradle.kts` - upstream's version-catalog file with all six fork build additions re-applied and all four discretionary replacements landed

## Decisions Made

- PD-01 (Spotless block, decided in the plan's own preamble): kept, verbatim.
- The `org.slf4j:slf4j-reload4j:2.0.17` inline dependency line was re-added to the `dependencies {}` block — required by D-16/T-06-06 and this plan's own acceptance criteria, even though it wasn't itemized among the plan's six numbered "Additions." Added with a `FORK OVERRIDE` comment, verified present in both the build script text and the resolved dependency graph.
- Reworded a comment near the `testUnit` registration to avoid the literal string `maxParallelForks` in prose, so the acceptance criterion's blunt `grep -o` count stayed at the intended 2 (the two real `maxParallelForks =` assignment lines) rather than 3.

## Deviations from Plan

None - plan executed exactly as written. The two items above (the inline `slf4j-reload4j` re-add and the comment wording) are both explicitly required by the plan's own acceptance criteria and threat register, not scope additions — they are documented here for traceability, not as Rule 1-4 deviations.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `sync/v1.6.0` now sits at `b582a44b`, two ordinary commits past the mechanical merge (`673d29c3` → `4f3ff2a9` → `b582a44b`), with a fully resolvable build (`help`/`dependencies` both green at Gradle 9.6.1).
- The four D-12 resolved-version facts are proven and recorded above for plan 06-03 to file into its checklist: `com.clickhouse:client-v2@0.9.6` (and all sibling `com.clickhouse:*` artifacts), `org.apache.kafka:connect-api@4.1.1`, `org.slf4j:slf4j-reload4j@2.0.17`, LZ4 capability satisfied by `at.yawk.lz4:lz4-java:1.10.2`.
- `generateVersionFile` will interpolate `ARTIFACT_VERSION = "v1.6.0_gg1"` from the `VERSION` file the next time `compileJava` runs (Phase 7) — Phase 8's JMX re-plumb should inherit this string rather than rediscovering it.
- No push occurred to `origin`, and `sync/v1.6.0` still has no tracking remote — both prohibitions honored.
- No compilation or test task was run at any point in this plan (`help`, `dependencies`, `--version`, and `tasks --all` only, plus one read-only `check --dry-run` probe used solely to confirm task-graph wiring, itself run with no actual test execution).
- `REQUIREMENTS.md`: BLD-01 and BLD-02 are shared with plan 06-01 (declared there too) — flip to `Complete` automatically once the shared-ID gate sees both declaring plans have a SUMMARY. BLD-03 through BLD-08 are declared only by this plan and mark complete immediately.

## Self-Check: PASSED

- `gradle/libs.versions.toml` — FOUND on disk, diff confirms exactly 2 changed keys
- `VERSION` — FOUND on disk, reads `v1.6.0_gg1`
- `.gitignore` — FOUND on disk, all 5 union tokens present
- `.github/workflows/tests.yaml` — FOUND on disk, byte-identical to `pre-v1.6.0-sync`, 55 lines
- `build.gradle.kts` — FOUND on disk, all `ADDITIONS_PRESENT` and `DROPPED_ITEMS_OK` grep assertions re-verified passing
- Commit `b582a44b` — FOUND in git log, non-merge (2 parent SHAs), subject starts `build(06):`
- All task `<acceptance_criteria>` re-verified passing (task 1's three verify blocks, task 2's three verify blocks, task 3's four verify blocks)
- Plan-level `<verification>` re-run: `help`/`dependencies` exit 0, `--version` reports `Gradle 9.6.1`, `tasks --all` lists all three fork tasks, all four D-12 facts hold, commit exists on `sync/v1.6.0` with nothing pushed, no compilation/test task ran

---
*Phase: 06-merge-groundwork-build-migration*
*Completed: 2026-09-18*
