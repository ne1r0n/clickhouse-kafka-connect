# Phase 5: CI & Final Verification - Pattern Map

**Mapped:** 2026-09-18
**Files analyzed:** 2 (1 certain modification, 1 conditional new file — see note)
**Analogs found:** 1 exact (self-analog + in-repo convention) / 1 no-analog (documented, verified alternative source provided)

## Scope Note (read first)

This phase is CI-config-only. CONTEXT.md and RESEARCH.md agree the phase touches **exactly one file**:
`.github/workflows/tests.yaml`. There is no `src/`, no `build.gradle.kts`, no new test file. Unlike a normal
feature phase, there is no controller/service/model to classify — the "file classification" table below
covers a workflow config file and one **conditionally new** script file that RESEARCH.md's Code Examples
section materializes as `.github/scripts/jacoco_summary.py` but CONTEXT.md's framing ("the only file this
phase modifies") suggests could instead be inlined as a heredoc inside the workflow's `run:` step. Both
options are documented below; the planner must pick one and can lift the exact code either way from
RESEARCH.md's verified Code Examples section, since no in-repo analog exists for the script content itself.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `.github/workflows/tests.yaml` | config (CI workflow) | event-driven (triggered on `push`) | `.github/workflows/tests.yaml` (its own pre-Phase-5 content, for structure) + `.github/workflows/test-head.yaml` (for the `concurrency` block) | exact (self) / role-match (concurrency block) |
| `.github/scripts/jacoco_summary.py` (conditional — only if planner chooses a separate file over an inline heredoc) | utility / transform script | transform (XML → markdown) | none in this repo | no analog — use RESEARCH.md's verified Code Examples script instead |

**Tracked-source gate:** both `.github/workflows/tests.yaml` and `.github/workflows/test-head.yaml` are
git-tracked (`git ls-files` confirms both). No mirrored/gitignored paths are involved anywhere in this
phase.

## Pattern Assignments

### `.github/workflows/tests.yaml` (config, event-driven)

**Analog:** its own current content (git-tracked, read in full this session) plus
`.github/workflows/test-head.yaml` for the one new structural element (`concurrency`).

**Current structure to remove** (`.github/workflows/tests.yaml:1-43`, full file):
```yaml
name: ClickHouse Kafka Connect Tests CI

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        client: ["V1", "V2"]
        clickhouse: ["23.7", "24.3", "latest", "cloud"]
    name: ClickHouse ${{ matrix.clickhouse }} Client version ${{ matrix.client }} tests
    steps:
      - name: Check for Cloud Credentials
        id: check-cloud-credentials
        run: |
          if [[ "${{ matrix.clickhouse }}" == "cloud" && (-z "${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT }}" || -z "${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_PASSWORD_SMT }}") ]]; then
            echo "SKIP_STEP=true" >> $GITHUB_ENV
          else
            echo "SKIP_STEP=false" >> $GITHUB_ENV
          fi
        shell: bash

      - uses: actions/checkout@v3
        if: env.SKIP_STEP != 'true'
      - name: Set up JDK 17
        if: env.SKIP_STEP != 'true'
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'adopt'
          architecture: x64
      - name: Setup and execute Gradle 'test' task
        if: env.SKIP_STEP != 'true'
        uses: gradle/gradle-build-action@v2
        env:
          CLICKHOUSE_VERSION: ${{ matrix.clickhouse }}
          CLICKHOUSE_CLOUD_HOST: ${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT }}
          CLICKHOUSE_CLOUD_PASSWORD: ${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_PASSWORD_SMT }}
          CLIENT_VERSION: ${{ matrix.client }}
        with:
          arguments: test
```
Every `if:`, the cloud-guard step, the matrix, and both `secrets.*` refs are deleted wholesale per D-01/D-02.
`on: [push]` (line 3) is the one line kept verbatim (D-13).

**Concurrency pattern to copy** (`.github/workflows/test-head.yaml:8-10`, tracked, the only other workflow in
this repo using this pattern):
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true
```
Copy verbatim — this is the repo's one established convention for this concern (D-14). The `pull_request`
branch of the ternary is dead code here (`tests.yaml` has no `pull_request` trigger) but copying it as-is
keeps the two workflows textually consistent; `${{ github.workflow }}-${{ github.sha }}` is an equally valid
simplification if the planner prefers to drop the dead branch.

**Env-var consumption this workflow must match** (downstream contract, not itself modified —
`src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:42,55-62`):
```java
public static final String CLICKHOUSE_VERSION_DEFAULT = "latest";
...
String clickHouseVersion = System.getenv("CLICKHOUSE_VERSION");
if (clickHouseVersion == null) {
    clickHouseVersion = CLICKHOUSE_VERSION_DEFAULT;
}
...
String version = System.getenv("CLICKHOUSE_VERSION");
```
Confirms the exact env var names (`CLICKHOUSE_VERSION`, `CLIENT_VERSION`) D-03 pins explicitly in the job
`env:` block — same names the old matrix used (`${{ matrix.clickhouse }}` / `${{ matrix.client }}`), just now
literal `latest` / `V2` instead of matrix interpolation.

**Target shape** (RESEARCH.md "Complete rewritten `tests.yaml` skeleton", all three named action tags
live-verified against the GitHub API this session — `actions/checkout@v4`, `actions/setup-java@v6`
temurin, `gradle/actions/setup-gradle@v6`):
```yaml
name: ClickHouse Kafka Connect Tests CI

on: [push]

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    name: ClickHouse latest Client version V2 tests
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v6
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Run tests
        env:
          CLICKHOUSE_VERSION: latest
          CLIENT_VERSION: V2
        run: ./gradlew test

      - name: Generate coverage summary
        if: always()
        run: python3 .github/scripts/jacoco_summary.py | tee -a "$GITHUB_STEP_SUMMARY"

      - name: Upload JaCoCo coverage reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-coverage-reports
          path: build/reports/jacoco/
          retention-days: 14

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: gradle-test-reports
          path: build/reports/tests/
          retention-days: 14
```
**Before finalizing:** `actions/upload-artifact@v4` was not one of CI-01's three named tags — RESEARCH.md
flags it as needing the same live tag-existence check the three named actions already got
(Assumptions Log A3, Open Question #2). Verify it the same way (`GET
api.github.com/repos/actions/upload-artifact/git/ref/tags/v4`) before locking it into the plan.

**Do not carry over** (`build/reports/jacoco/fork/jacocoForkReport.xml` pitfall confirmed live this
session): the local shell's `env -u TESTCONTAINERS_HOST_OVERRIDE` prefix from
`.planning/config.json`'s `workflow.test_command`. The `run:` step is exactly `./gradlew test`, D-15.

**Header comment pattern (D-04)** — no in-repo precedent for a "fork-modified" marker comment exists yet;
write one at the top of the file, e.g.:
```yaml
# FORK MODIFICATION: matrix removed — this fork only tests ClickHouse `latest` x client V2
# (the V1 / 23.7 / 24.3 / Cloud legs upstream ran are out of scope; see .planning/REQUIREMENTS.md CI-01..04)
```
(from RESEARCH.md's synthesized skeleton — exact wording is Claude's Discretion per CONTEXT.md).

---

### `.github/scripts/jacoco_summary.py` (conditional new file — utility/transform)

**No in-repo analog.** This repo has zero existing Python files and no `.github/scripts/` directory
(`find . -iname "*.py"` returns nothing outside `.git`/`.gsd`). Do not force-fit an unrelated Java/Kotlin
pattern here. RESEARCH.md's Code Examples section is the load-bearing source instead — it was written and
**test-run against this repo's own real JaCoCo XML output this session**, including a genuine edge case
(classes with missing `LINE`/`BRANCH` counters) that a guessed implementation would miss.

**Decision point for the planner:** CONTEXT.md/RESEARCH.md both frame this phase as touching "exactly one
file" (`.github/workflows/tests.yaml`), which argues for inlining the parser as a `python3 - <<'EOF' ... EOF`
heredoc inside the workflow's `run:` step rather than adding a second checked-in file. RESEARCH.md's own
Code Example nonetheless materializes it as a separate `.github/scripts/jacoco_summary.py` invoked via
`python3 .github/scripts/jacoco_summary.py`. Either is valid D-06 compliance (no marketplace action, no
`build.gradle.kts` change); the script content is identical either way. Record the choice explicitly in the
plan.

**Verified XML shape to parse** (`build/reports/jacoco/fork/jacocoForkReport.xml`, read live this session —
report-level totals are the trailing `<counter>` elements under `<report>`; class-level totals are the
trailing `<counter>` elements inside each `<class>`, after all `<method>` children):
```xml
<class name="com/clickhouse/kafka/connect/ClickHouseSinkConnector" ...>
  <method .../>
  <!-- class-level TOTALS come after all <method> children: -->
  <counter type="INSTRUCTION" missed="92" covered="0"/>
  <counter type="BRANCH" missed="2" covered="0"/>
  <counter type="LINE" missed="27" covered="0"/>
  ...
</class>
```
**Verified edge case, must guard:** some classes (e.g. `Column$ColumnBuilder`, `Table$1`) have **no**
`LINE`/`BRANCH` counter element at all — a parser that does `int(counter.get('missed'))` without a `None`
check will crash (`AttributeError`).

**Working parser + markdown emitter** (RESEARCH.md Code Examples, verified against real repo XML this
session — produces correct headline percentages, e.g. full-suite BRANCH 168/852 = 19.7%, and handles
missing counters without crashing):
```python
import xml.etree.ElementTree as ET

FORK_CLASSES_IN_ORDER = [
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter",
    # ... all 15 classes from build.gradle.kts's forkChangedClasses list (D-13, build.gradle.kts:245-276)
]

def pct(counter):
    if counter is None:
        return "n/a"
    missed, covered = int(counter.get("missed")), int(counter.get("covered"))
    total = missed + covered
    return f"{100 * covered / total:.1f}%" if total else "0.0%"

def report_totals(xml_path):
    root = ET.parse(xml_path).getroot()
    return {c.get("type"): c for c in root.findall("counter")}

def class_counters(xml_path):
    root = ET.parse(xml_path).getroot()
    result = {}
    for cls in root.iter("class"):
        result[cls.get("name")] = {c.get("type"): c for c in cls.findall("counter")}
    return result

full = report_totals("build/reports/jacoco/test/jacocoTestReport.xml")
fork = report_totals("build/reports/jacoco/fork/jacocoForkReport.xml")
fork_classes = class_counters("build/reports/jacoco/fork/jacocoForkReport.xml")

lines = ["## Coverage Summary", "", "| Scope | Instruction | Line | Branch |", "|---|---|---|---|"]
lines.append(f"| Full suite | {pct(full.get('INSTRUCTION'))} | {pct(full.get('LINE'))} | {pct(full.get('BRANCH'))} |")
lines.append(f"| Fork-changed classes | {pct(fork.get('INSTRUCTION'))} | {pct(fork.get('LINE'))} | {pct(fork.get('BRANCH'))} |")
lines += ["", "### Fork-changed classes", "", "| Class | Line | Branch |", "|---|---|---|"]
for name in FORK_CLASSES_IN_ORDER:
    counters = fork_classes.get(name, {})
    short = name.rsplit("/", 1)[-1]
    lines.append(f"| {short} | {pct(counters.get('LINE'))} | {pct(counters.get('BRANCH'))} |")

print("\n".join(lines))
```
**Source of the 15-class ordering:** copy `forkChangedClasses` names (without the `.class`/`\$*.class`
suffixes) from `build.gradle.kts:245-276` — this is the authoritative list, already deduped to base class
names only (the `$*` wildcard entries are JaCoCo include-glob siblings, not separate classes to list).

**Error handling pattern:** none needed beyond the `None`-guard above — this is a short-lived CI script, not
a service; a crash here should fail loudly (non-zero exit) rather than swallow the exception, since D-08's
`if: always()` means it runs on both green and red test outcomes and its own success/failure is visible in
the job's step list either way.

---

## Shared Patterns

### CI action versions (cross-cutting: applies to every `uses:` step touched)
**Source:** RESEARCH.md Standard Stack table, all three tags live-verified against `api.github.com` this
session (2026-09-18).
| Old (current `tests.yaml`) | New | Verified |
|---|---|---|
| `actions/checkout@v3` | `actions/checkout@v4` | resolves to commit `11d5960a326750d5838078e36cf38b85af677262` |
| `actions/setup-java@v3` (`distribution: adopt`, `architecture: x64`) | `actions/setup-java@v6` (`distribution: temurin`, `architecture:` omitted) | resolves to commit `de7274f081f381c8f8158605e0321c36c376e2e6`; `temurin` confirmed supported; omitting `architecture` is safe since `ubuntu-latest` is x64 by default |
| `gradle/gradle-build-action@v2` (`with: arguments: test`) | `gradle/actions/setup-gradle@v6` + separate `run: ./gradlew test` | tag confirmed present in `gradle/actions` tag list; `arguments:` input was dropped in v4+, per the action's own docs |

**Apply to:** the single `.github/workflows/tests.yaml` job only (D-02: the other four workflows are
explicitly untouched this phase, even though they carry the same deprecated tags).

### Concurrency + timeout hardening
**Source:** `.github/workflows/test-head.yaml:8-10` (tracked, in-repo, the only other workflow using this
pattern).
**Apply to:** the `tests.yaml` job (new addition, D-14). Not applicable to any other file this phase touches.

### No secrets, no cloud guard
**Source:** deletion, not addition — `.github/workflows/tests.yaml`'s current `Check for Cloud Credentials`
step and its two `secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_*` references are removed wholesale (CI-02). No
replacement pattern is needed; the new job has zero secrets in scope by design.
**Apply to:** `tests.yaml` only.

### Coverage is report-only, never a gate
**Source:** `build.gradle.kts:278-305` — `jacocoTestReport`/`jacocoForkReport` are wired via `finalizedBy`
on `test` only; there is no `jacocoTestCoverageVerification` task anywhere in the build.
**Apply to:** the new "Generate coverage summary" and both "Upload ... reports" steps — none may introduce
a pass/fail threshold (CI-03 explicitly forbids one; this is a hard constraint, not a style choice).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.github/scripts/jacoco_summary.py` (if written as a separate file rather than inlined) | utility/transform | transform (XML → markdown) | Repo has zero existing Python files or `.github/scripts/` directory; use the RESEARCH.md-verified script under Pattern Assignments above instead of searching for an in-repo precedent that does not exist |

## Metadata

**Analog search scope:** `.github/workflows/` (all 6 files read), `build.gradle.kts` (jacoco/test task
region, lines 220-309), `src/test/java/.../ClickHouseTestHelpers.java` (env-var contract), full-repo
`find -iname "*.py"` (confirmed empty) and `find .github -type d` (confirmed no `scripts/` dir yet).
**Files scanned:** 8 (6 workflow YAMLs, 1 Gradle build file region, 1 Java helper for env-var names).
**Pattern extraction date:** 2026-09-18
