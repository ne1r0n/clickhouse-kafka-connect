---
phase: 01-test-infrastructure-green-baseline
plan: 03
subsystem: testing
tags: [jacoco, coverage, gradle, lombok, testing-infrastructure, junit-tags]

# Dependency graph
requires:
  - phase: 01-02
    provides: "ClickHouseBase container-per-fork singleton, single inherited @Tag(\"clickhouse\"), full suite green at 194 tests / 0 failed / 7 skipped"
provides:
  - "jacoco plugin (toolVersion 0.8.15) wired to the test task only, producing build/reports/jacoco/test/{jacocoTestReport.xml,html/}"
  - "jacocoForkReport: a second JacocoReport task filtered to the 15 D-13 fork-changed classes, writing build/reports/jacoco/fork/{jacocoForkReport.xml,html/}"
  - "Root lombok.config (stopBubbling + addLombokGeneratedAnnotation), proven via javap and the report excluding Column's Lombok-generated methods"
  - "testUnit Gradle task: excludeTags(\"clickhouse\"), no Docker required, JaCoCo agent disabled, kept out of check via an if (name != \"testUnit\") guard in the existing tasks.withType<Test> loop"
  - "Full ./gradlew test green with the JaCoCo agent attached and neither CLICKHOUSE_VERSION nor CLIENT_VERSION set: 194 tests, 187 succeeded, 0 failed, 7 skipped"
affects: [02, 03, 04, 05]

# Actuals (#2632)
actuals:
  tokens: 1484
  tasks: 2
  commits: 2
  plan_head_before: d79ac2f6b711b52640f2877aa02482d845c5f45c

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JaCoCo hung off test only via finalizedBy, never off check or integrationTest (D-16); no jacocoTestCoverageVerification rule added — coverage is report-only"
    - "Fork-changed coverage view: a named, exact-class-name include list (forkChangedClasses) feeding a second JacocoReport task that reads the same test.exec, rather than a package glob or a git-diff-derived list"
    - "Lombok-generated members excluded from coverage via lombok.config + JaCoCo's built-in @lombok.Generated filtering, verified by inspecting the report rather than trusting the file's presence"
    - "Docker-free test selection via JUnit tag exclusion (testUnit excludeTags(\"clickhouse\")) plus a one-line guard in the pre-existing tasks.withType<Test> check-loop, instead of a separate source set"

key-files:
  created:
    - lombok.config
  modified:
    - build.gradle.kts

key-decisions:
  - "forkChangedClasses lists both <path>/X.class and <path>/X\\$*.class per D-13 class (30 entries for 15 classes) instead of a bare prefix glob, so HeaderToValue* cannot also match HeaderToValueConfig.class — verified: the fork report's class set equals exactly the 15 D-13 classes, no more, no fewer."
  - "jacocoForkReport is registered (not just configured) since it did not exist before this plan; jacocoTestReport is configured via tasks.named since Gradle's jacoco plugin already registers it."
  - "The check-loop guard (if (name != \"testUnit\")) is scoped to exactly the task name D-16 names, leaving test's and integrationTest's existing check dependency untouched, per research Pitfall 5."
  - "No JaCoCo-agent-related test fix was needed this run (plan step 5's contingency) — the full suite was green on the first covered run with no test-code changes required."

requirements-completed: [COV-01, COV-02, INFRA-04]

coverage:
  - id: D1
    description: "JaCoCo 0.8.15 wired to test only, producing full HTML+XML coverage report at build/reports/jacoco/test/ (COV-01, D-16)"
    requirement: "COV-01"
    verification:
      - kind: other
        ref: "rm -rf build/reports/jacoco build/jacoco && ./gradlew test --rerun --tests ColumnTest && test -f build/jacoco/test.exec && test -f build/reports/jacoco/test/jacocoTestReport.xml && test -f build/reports/jacoco/test/html/index.html"
        status: pass
    human_judgment: false
  - id: D2
    description: "jacocoForkReport filters the same exec data to exactly the 15 D-13 fork-changed classes and their inner classes, writing its own HTML+XML under build/reports/jacoco/fork/ (COV-02, D-13, D-14)"
    requirement: "COV-02"
    verification:
      - kind: other
        ref: "python3 xml.etree check of jacocoForkReport.xml: got == want (15 D-13 classes, split on '$' to fold inner classes) — run against both the filtered-test run and the full covered run"
        status: pass
    human_judgment: false
  - id: D3
    description: "Column's Lombok-generated members (getName, getType, builder) are excluded from coverage while hand-written methods (extractColumn, etc.) are counted (COV-02, D-15)"
    requirement: "COV-02"
    verification:
      - kind: other
        ref: "python3 xml.etree check of jacocoTestReport.xml Column class method set (extractColumn present, getName/getType/builder absent); javap -v -p Column.class | grep -c 'Llombok/Generated;' == 1"
        status: pass
    human_judgment: false
  - id: D4
    description: "testUnit runs every untagged unit test with Docker unreachable, runs no class extending ClickHouseBase, and is excluded from check and from JaCoCo instrumentation (INFRA-04, D-12, D-16)"
    requirement: "INFRA-04"
    verification:
      - kind: other
        ref: "DOCKER_HOST=unix:///tmp/gsd-no-docker.sock docker info fails; testUnit run: 69 tests, 67 succeeded, 0 failed, 2 skipped, tagged_ran=[], no missing required unit classes, no build/jacoco/testUnit.exec; ./gradlew check --dry-run shows :test/:integrationTest SKIPPED, no :testUnit line, no coverage-verification task"
        status: pass
    human_judgment: false
  - id: D5
    description: "Full ./gradlew test is green on the 01-02 harness with the JaCoCo agent attached and neither CLICKHOUSE_VERSION nor CLIENT_VERSION set, both reports regenerated from that run, and no leftover Testcontainers containers"
    requirement: "COV-01"
    verification:
      - kind: integration
        ref: "build/phase1-03-full-test.log: Test result: SUCCESS, Tests summary: 194 tests, 187 succeeded, 0 failed, 7 skipped; docker ps --filter label=org.testcontainers=true returns 0 after a 20s wait; D1-D3 python checks re-run and passed against this run's reports"
        status: pass
    human_judgment: false
  - id: D6
    description: "The generated fork-changed HTML report is human-reviewable: it lists only the 15 D-13 classes across 7 packages, and Column's page shows hand-written methods with no Lombok getter/setter/builder rows"
    verification: []
    human_judgment: true
    rationale: "D-15/ROADMAP criterion 5 explicitly ask for human review of the generated HTML report's readability and Lombok exclusion, not just the XML's machine-checkable class set. An automated sanity check (grep on the generated HTML) was additionally run this session and corroborates D1-D3, but the plan's own <human-check> block defers final sign-off to end-of-phase UAT per workflow.human_verify_mode=end-of-phase."

# Metrics
duration: 20min
completed: 2026-09-16
status: complete
---

# Phase 1 Plan 3: JaCoCo Coverage Wiring, Lombok Exclusion, and Docker-Free testUnit Summary

**JaCoCo 0.8.15 wired to `test` only with a full report and a 15-class fork-changed report (Lombok-generated methods excluded via `lombok.config`), plus a Docker-free `testUnit` task that stays out of `check` — full suite still green at 194 tests, 0 failed, 7 skipped with the agent attached.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-09-15T22:05:22Z (approx., right after the 01-02 plan-metadata commit)
- **Completed:** 2026-09-15T22:25:00Z (approx.)
- **Tasks:** 2
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments

- `build.gradle.kts`: `jacoco` plugin added, `toolVersion = "0.8.15"`; `jacocoTestReport` configured for XML+HTML under `build/reports/jacoco/test/`; new `jacocoForkReport` task filters the same `test.exec` to a named, exact-class-name `forkChangedClasses` list (the 15 D-13 classes plus inner classes), writing `build/reports/jacoco/fork/{jacocoForkReport.xml,html/}`. Both reports hang off `test` via `finalizedBy` only — never `check`, no coverage-verification rule. The JaCoCo agent is explicitly disabled on `integrationTest`.
- New root `lombok.config` (`config.stopBubbling = true`, `lombok.addLombokGeneratedAnnotation = true`): proven by `javap` showing `@lombok.Generated` on `Column`, and the coverage report listing `extractColumn` (hand-written) while omitting `getName`/`getType`/`builder` (Lombok-generated).
- New Docker-free `testUnit` Gradle task: `excludeTags("clickhouse")`, JaCoCo agent disabled, inherits `maxParallelForks`/`windows-1252` encoding/logging/Java 17 launcher from the existing `tasks.withType<Test>` block. Kept out of `check` via a one-line `if (name != "testUnit")` guard added to that same block's `check.dependsOn(this)` line (research Pitfall 5) — `test` and `integrationTest` keep their existing `check` dependency unchanged.
- Full `./gradlew test` rerun with neither `CLICKHOUSE_VERSION` nor `CLIENT_VERSION` set, JaCoCo agent attached: 194 tests, 187 succeeded, 0 failed, 7 skipped — identical to the 01-02 baseline. Both reports regenerated from that run and re-verified against the same Column/fork-set checks used in Task 1. No leftover Testcontainers containers after a 20s post-build wait.

## Task Commits

1. **Task 1: Tracer — JaCoCo on test, producing the full and fork-changed reports with Lombok excluded, proven on one Docker-free unit test class** - `9e7cde8` (feat)
2. **Task 2: Docker-free testUnit kept out of check, then the full covered run on the new harness** - `0a2d7c0` (feat)

**Plan metadata:** committed separately (docs commit, see below)

## Files Created/Modified

- `lombok.config` - `config.stopBubbling = true`, `lombok.addLombokGeneratedAnnotation = true` (D-15)
- `build.gradle.kts` - `jacoco` plugin/config, `forkChangedClasses`, `jacocoTestReport`/`jacocoForkReport` wiring, `testUnit` task, `if (name != "testUnit")` check-loop guard, JaCoCo agent disabled on `integrationTest`/`testUnit`

## Test / Coverage Metrics

| Run | Tests | Succeeded | Failed | Skipped | Duration |
|---|---|---|---|---|---|
| `testUnit` (Docker unreachable) | 69 | 67 | 0 | 2 | ~3s |
| Full `test` (JaCoCo attached, no env overrides) | 194 | 187 | 0 | 7 | 4m 13s |

`testUnit` discovered 16 `@Tag("clickhouse")` classes and ran none of them; all 11 required untagged unit classes ran (`transforms.{ConvertToMapOrJsonTest,HeaderToValueTest,HeadersToValueTest,KeyToValueTest,TableOperationFilterTest}` and `sink.{kafa.RangeContainerTest,processing.ProcessingTest,provider.LocalProviderTest,util.MaskTest,util.UtilsTest,db.mapping.ColumnTest}`); `ColumnTest` alone contributes several parameterized cases, hence 69 total vs. 12 discovered test classes.

**Report-level LINE coverage** (from the full covered run):

| Report | Covered | Missed | Total | % |
|---|---|---|---|---|
| `jacocoTestReport` (all `src/main` classes) | 2122 | 739 | 2861 | 74.2% |
| `jacocoForkReport` (15 D-13 fork-changed classes) | 1645 | 397 | 2042 | 80.6% |

Phase 5 CI can consume `build/reports/jacoco/fork/jacocoForkReport.xml` directly for the fork-changed-class view.

## Decisions Made

- `forkChangedClasses` lists both `<path>/X.class` and `<path>/X$*.class` per D-13 class (30 entries for 15 classes), never a bare prefix glob — this specifically avoids `HeaderToValue*` also matching `HeaderToValueConfig.class`, a risk STACK.md flagged in research. Verified: the generated fork report's class set is exactly the 15 D-13 classes, confirmed twice (filtered run and full run).
- `jacocoForkReport` uses `tasks.register` (new task); `jacocoTestReport` uses `tasks.named` (already registered by the core `jacoco` plugin).
- No JaCoCo-agent-related test fix was needed (the plan's step 5 contingency for a broken test under instrumentation) — the full suite was green on the first attempt with the agent attached, no test-code changes required.
- The check-loop guard is scoped to exactly `testUnit` by name, per D-16 and research Pitfall 5, leaving `test`'s and `integrationTest`'s pre-existing `check` dependency untouched.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The full covered run was green on the first attempt with the JaCoCo agent attached; no flakes, no leaked containers, no test-code changes required.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 1 success criteria 1 ("JaCoCo report shows coverage of fork-changed classes") and 5 (a reviewable fork-changed-class view) are both satisfied: `build/reports/jacoco/test/` and `build/reports/jacoco/fork/` each contain XML+HTML after `./gradlew test`.
- `./gradlew testUnit` is available for Docker-free iteration on SMT/unit work in Phases 2-4; it excludes every `ClickHouseBase` subclass automatically via the inherited `@Tag("clickhouse")` from 01-02, with no additional per-class configuration needed.
- The full suite remains green at 194 tests / 0 failed / 7 skipped with JaCoCo now permanently attached to `test` — every test Phases 2-4 add will show up in coverage from day one.
- `build/reports/jacoco/fork/jacocoForkReport.xml` is ready for Phase 5 CI to publish or summarize directly.
- No blockers. Phase 1 is complete pending the final human review of the generated fork-changed HTML report (D6 above), which is deferred to end-of-phase UAT per `workflow.human_verify_mode=end-of-phase`.

## Self-Check: PASSED

- FOUND: `lombok.config`
- FOUND: `build.gradle.kts` (modified)
- FOUND: `build/phase1-03-full-test.log` (Test result: SUCCESS, 194 tests, 187 succeeded, 0 failed, 7 skipped)
- FOUND: `build/check-dry-run.txt` (`:test SKIPPED`, `:integrationTest SKIPPED`, no `:testUnit` line, no coverage-verification task)
- FOUND commit `9e7cde8`, `0a2d7c0` in `git log --oneline --all`
- Re-ran acceptance criteria: `toolVersion = "0.8.15"` count = 1; `register<JacocoReport>("jacocoForkReport")` count = 1; `finalizedBy(` count = 1; `JacocoTaskExtension` count = 2; `jacocoTestCoverageVerification|violationRules` count (excluding comments) = 0; `register<Test>("testUnit")` count = 1; `excludeTags("clickhouse")` count = 1; `if (name != "testUnit")` count = 1
- Re-ran all automated `<verify>` commands from both tasks against the current `build/` output: all passed

---
*Phase: 01-test-infrastructure-green-baseline*
*Completed: 2026-09-16*
