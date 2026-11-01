# Phase 31: Fork-Owned Nightly Integration Tests - Pattern Map

**Mapped:** 2026-10-26
**Files analyzed:** 9 new/modified (plus planning docs)
**Analogs found:** 8 / 9 (all analog paths verified git-tracked)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.github/workflows/nightly-integration-tests.yaml` | config (workflow) | batch (scheduled CI) | `.github/workflows/tests.yaml` | exact |
| `.github/scripts/failed_tests_summary.py` | utility | transform (XML to markdown) | `.github/scripts/jacoco_summary.py` | exact |
| `src/test/java/.../ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java` | test (guard) | file-I/O | `TestsWorkflowPinGuardUnitTest.java` (style) | role-match |
| `src/test/java/.../ci/NightlyWorkflowGuardUnitTest.java` | test (guard) | file-I/O, transform | `TestsWorkflowPinGuardUnitTest.java` | exact |
| `src/test/java/.../ci/IntegrationSkipListGuardUnitTest.java` | test (guard) | file-I/O | `TestsWorkflowPinGuardUnitTest.java` (+ `ConnectApiReferencesUnitTest` for reading a data file) | role-match |
| `gradle/integration-test-skip-list.tsv` | config (data) | batch | `.planning/scripts/fork-anchors.txt` (TAB rows, `#` header) | role-match |
| `build.gradle.kts` (edit `tasks.named<Test>("integrationTest")`, ~L388) | config | batch | same block, existing | exact |
| `.planning/scripts/fork-anchors.txt` (new rows) | config | batch | existing rows | exact |
| `31-COMMAND-EVIDENCE.md` (measurement record) | doc | n/a | Phase 30 evidence file precedent | no code analog |

All test paths are `src/test/java/com/clickhouse/kafka/connect/ci/`.

## Pattern Assignments

### `.github/workflows/nightly-integration-tests.yaml` (workflow, batch)

**Analog:** `.github/workflows/tests.yaml`

**Header, permissions, runner, pins** (tests.yaml lines 22-52). Copy the four `uses:` lines byte for byte (Phase 30 guard regex needs 40-hex SHA plus `# vX.Y.Z`):
```yaml
permissions:
  contents: read
...
jobs:
  build:
    runs-on: ubuntu-24.04
    timeout-minutes: 45
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with:
          persist-credentials: false
      - name: Set up JDK 17
        uses: actions/setup-java@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0
```

**Summary + notice pattern** (lines 63-67): write to `$GITHUB_STEP_SUMMARY` and a `::notice`/`::error` annotation, since job logs are unreadable. Use for the capacity probe (`df -h /`, `free -m`, `docker version`).

**Always-run uploads** (lines 82-96):
```yaml
      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
        with:
          name: gradle-test-reports
          path: build/reports/tests/
          retention-days: 14
```
Nightly uploads both `build/test-results/integrationTest/` and `build/reports/tests/integrationTest/`.

**Differences from the analog:** trigger is `schedule` + `workflow_dispatch` (tests.yaml is `push` only); job gets `if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`; Gradle step has `env: CLICKHOUSE_VERSION: latest / CLIENT_VERSION: V2`, `run: ./gradlew integrationTest`, and a step `timeout-minutes` smaller than the job's; use a distinct `name:` and a fixed concurrency group (`cancel-in-progress: false`). No `secrets.`, no other `vars.`. Skeleton is in RESEARCH Code Example 1.

---

### `.github/scripts/failed_tests_summary.py` (utility, transform)

**Analog:** `.github/scripts/jacoco_summary.py`

Copy: shebang, module docstring stating "Standard library only", `import os, sys, xml.etree.ElementTree as ET`, path constants at the top, write to stdout and `$GITHUB_STEP_SUMMARY` if set, print a clear line and exit 0 when no reports exist. Input is `build/test-results/integrationTest/TEST-*.xml`; list `classname#name` for testcases holding `<failure>` or `<error>`. Escape `%`, `\r`, `\n` as `%25`, `%0D`, `%0A` for `::error`; escape backticks and pipes for Markdown; cap annotation length. Run it from tests via `python3 -I` (precedent: `ConnectApiReferencesUnitTest.java` line 56 runs `python3 -I .planning/scripts/connect-api-refs.py`).

---

### `UpstreamIntegrationWorkflowBytesGuardUnitTest.java` (test, file-I/O)

**Analog (style):** `TestsWorkflowPinGuardUnitTest.java`

**Class shape** (lines 27-45): extends nothing (keeps it in `testUnit`), Javadoc stating why, UTF-8 explicit, package-private static `Optional<String>` problem functions, `assertNoProblem` helper (line 117-119), fixture tests (`accepts`/`rejects` helpers, lines 588-617) proving each rule red before the real file is checked.
```java
private static final Path WORKFLOW = Path.of(".github", "workflows", "tests.yaml");
private static void assertNoProblem(Optional<String> problem) {
    problem.ifPresent(message -> fail(message));
}
```
**Core pattern:** RESEARCH Code Example 2 (`Files.readAllBytes` + SHA-256 + length; values `07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109`, 4812 bytes, blob `3e9b9be0...`). Do NOT use `nonCommentLines`/`readAllLines` (hides line-ending changes). Fixtures: byte flip, append, missing final newline, CRLF, empty.

---

### `NightlyWorkflowGuardUnitTest.java` (test, file-I/O)

**Analog:** `TestsWorkflowPinGuardUnitTest.java` (same package, reuse its package-private statics)

**Reuse directly** (package-private statics): `nonCommentLines(Path)` (line 107), `runsOnProblem` (143), `writeScopeProblem` (170), `permissionsBlockProblem` (179), `checkoutCredentialsProblem` (202), `stepBlock` (253), `gradlewStepProblem` (274; command-position regex `gradlewCall`, line 243, is private but reached through `gradlewStepProblem`), `SHA` (line 627). Prefix assertion text with the nightly file name since their messages say `tests.yaml`.
```java
assertNoProblem(runsOnProblem(lines));
assertNoProblem(writeScopeProblem(lines));
assertNoProblem(permissionsBlockProblem(lines));
assertNoProblem(checkoutCredentialsProblem(lines));
assertNoProblem(gradlewStepProblem(lines, "Run integration tests", "integrationTest"));
```
**Do not reuse** `pinnedUsesProblem` (requires 5 uses via `MIN_PINNED_ACTIONS`; Phase 30 guard already covers pins). `continue-on-error` handling: copy `CONTINUE_ON_ERROR` regex idea (line 63, `archiveStepProblem` lines 297-310). New rules (each seen red on fixtures): triggers exactly schedule + workflow_dispatch; job `if:` byte-equal to the D-01 expression; job and step `timeout-minutes` with step < job; explicit `CLICKHOUSE_VERSION: latest` and `CLIENT_VERSION: V2`; no `--tests`; no `secrets.`, only `vars.NIGHTLY_ENABLED`, no `CLICKHOUSE_CLOUD` / `-Dclickhouse.`; report and upload steps `if: always()`.

**Fixture helper style** (lines 588-617): `accepts(rule, String... lines)` / `rejects(rule, ...)` inside `assertAll`.

**Phase 30 coupling:** `WorkflowActionPinGuardUnitTest.java` line 49 has `FORK_WORKFLOWS = List.of("increment-version.yml","pre-release.yaml","release.yaml","tests.yaml")` and line 174 uses `found.containsAll(FORK_WORKFLOWS)`, so adding the nightly does not break it (superset is allowed). Also `TestsWorkflowPinGuardUnitTest.EXISTING_WORKFLOWS` (line 694) is fixture-only; no edit needed. Dependabot `exclude-paths` must not list the nightly.

---

### `IntegrationSkipListGuardUnitTest.java` (test, file-I/O)

**Analog:** `TestsWorkflowPinGuardUnitTest.java` (structure), `SourcePlanningIdGuardUnitTest.java` / `ConnectApiReferencesUnitTest.java` (reading a repo data file; not read in detail, planner should glance at them).

Rules (D-08): zero rows allowed; each class exists under `src/integrationTest/java`; method name occurs in that class; no `*`, `?`, `[`; category in `{environment, harness, cloud-only, connector-defect}`; no duplicates; reason and trigger non-empty. Fixtures red first. Skip `#` comment lines like the anchors file.

---

### `gradle/integration-test-skip-list.tsv` (data)

**Analog:** `.planning/scripts/fork-anchors.txt` lines 1-12: `#` header comments documenting format ("four TAB-separated fields per row"), then TAB rows. Fields: `class#method <TAB> category <TAB> reason <TAB> re-open trigger`. Header only if the measurement is green. Note: `gradle/` currently holds only `libs.versions.toml` and wrapper (tracked), so this is a new file there.

---

### `build.gradle.kts` (config edit)

**Analog:** existing block (~L388):
```kotlin
// D-16: JaCoCo agent runs on test only.
tasks.named<Test>("integrationTest") {
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}
```
Keep the JaCoCo line; add `inputs.file(...)`, parse the TSV (fail the build on a malformed row with a clear message), and `filter { excludeTest(className, methodName) }` per entry (only when non-empty). Related existing config at L170-200: `integrationTest by registering(JvmTestSuite::class)`, `dependsOn("prepareConfluentArchive")`, `outputs.upToDateWhen { false }`, and `systemProperties(System.getProperties()...)` copying every Gradle `-D` prop (so the nightly must never pass `-Dclickhouse.*`). Note `TestsWorkflowPinGuardUnitTest` parses `build.gradle.kts` with `ktsCode` (comment-stripping, line 455) and a quote nested in `${...}` templates is unsupported: avoid that in new Kotlin. Prove `failOnNoMatchingTests` is not tripped with a fixture entry.

---

### `.planning/scripts/fork-anchors.txt` (new rows)

**Analog:** existing rows, format `path <TAB> fixed-string <TAB> expected count <TAB> backs-ID` (header lines 1-12). Rule: rows land in the same commit as the edit they pin. Candidate anchors: `excludeTest(` in `build.gradle.kts` (backs NIT-0x), the D-01 gate line in the nightly. Counts are `grep -cF` line counts.

## Shared Patterns

### Docker-free guard style
**Source:** `TestsWorkflowPinGuardUnitTest.java` lines 27-45, 117-119, 588-617
**Apply to:** all three new guard classes. Extend nothing; UTF-8 explicit (`file.encoding=windows-1252` in the test JVM); `Optional<String>` problem functions; every rule seen red on a fixture; messages name the file and offending line.

### Least-privilege workflow
**Source:** `tests.yaml` lines 24-25, 41-43
**Apply to:** the nightly. Top-level `permissions: contents: read` (exactly one `permissions:` line), `persist-credentials: false`, `ubuntu-24.04`, full-SHA pins with `# vX.Y.Z`.

### Always-run reporting
**Source:** `tests.yaml` lines 78-96 (`if: always()` steps, `retention-days: 14`)
**Apply to:** summary and upload steps of the nightly.

### Anchor rows with the edit
**Source:** `.planning/scripts/fork-anchors.txt` header rule
**Apply to:** every commit that adds a guard-pinned line.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `31-COMMAND-EVIDENCE.md` | record | n/a | Use RESEARCH Pattern 1 as the checklist; follow the Phase 30 evidence file layout under `.planning/phases/30-*` |

## Metadata

**Analog search scope:** `.github/`, `src/test/java/.../ci/`, `.planning/scripts/`, `build.gradle.kts`, `gradle/`
**Tracked-source check:** `git ls-files` confirmed tracked: tests.yaml, jacoco_summary.py, fork-anchors.txt, ci/*.java, gradle/libs.versions.toml
**Pattern extraction date:** 2026-10-26
