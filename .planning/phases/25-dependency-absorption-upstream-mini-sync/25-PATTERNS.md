# Phase 25: Dependency Absorption & Upstream Mini-Sync - Pattern Map

**Mapped:** 2026-10-21
**Files analyzed:** 17 new/modified
**Analogs found:** 16 / 17 (all analog paths verified git-tracked)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java` (DEBT-13 `ktsCode`, DEBT-14, CI-20 rules) | test | transform (line-level text rules) | same file: `ktsCode`, `forkCountStepProblem`, `forkTaskProblem`, `accepts`/`rejects` | exact |
| `src/test/java/.../ci/ConnectApiReferencesUnitTest.java` (D-13, new) | test | file-I/O (read class files) | `src/test/java/.../ci/TestRuntimeClasspathLz4UnitTest.java` | role-match |
| `src/test/resources/connect-api-refs.txt` or `.planning/scripts/connect-api-refs.txt` (D-13 list) | config (blessed data) | batch | `.planning/scripts/fork-anchors.txt` (TSV list with header comments) | partial |
| `build.gradle.kts` (`confluentJar`, `shadow {}`, `verifyConfluentArchive`, imports) | config | batch | same file: `confluentJar` (~362), `createConfluentArchive` (~392), `reportTestForks` (255-262) | exact |
| `.github/workflows/tests.yaml` (new archive step) | config | request-response (CI step) | same file: `Report test fork count` / `Compile integration-test sources` steps | exact |
| `.planning/scripts/fork-anchors.txt` (rows moved/added) | config | batch | same file existing rows | exact |
| `.planning/scripts/fork-anchor-exemptions.txt` (18 SHA-pinned rows) | config | batch | same file, `6473e24f...` row | exact |
| `.planning/scripts/conflicted-paths-v1.7.0-tail.txt` | config | batch | `.planning/scripts/conflicted-paths-v1.7.0.txt` | exact |
| `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` | doc | batch | `.planning/MERGE-CHECKLIST-v1.7.0.md` | exact |
| `.planning/scripts/compare-confluent-archives.py` (new) | utility | batch/file-I/O | `.github/scripts/jacoco_summary.py` (style only) | partial |
| `.planning/scripts/connect-api-refs.py` (new) | utility | transform | `.github/scripts/jacoco_summary.py` (style only) | partial |
| `.planning/scripts/confluent-archive-allowlist-25.txt` | config | batch | `fork-anchor-exemptions.txt` (TSV + comment header) | partial |
| `.planning/scripts/verify-merge-invariants-selftest.sh` | test | batch | not edited (X-5); only run | n/a |
| `docs/FORK-DELTA.md` (rows: Shadow, guava, httpclient5, kafka, slf4j, Connect API sentence) | doc | batch | same file, "What the fork still patches" table | exact |
| `gradle/libs.versions.toml` (merge 2 resolution; DEBT-15 library row) | config | batch | same file, `slf4j-simple` row (line 70) | exact |
| `src/main/.../ClickHouseSinkTask.java` comment + 3 DLQ Javadocs (D-14) | comment-only | n/a | n/a | n/a |
| `.gitignore` (merge 2 union) | config | batch | no analog; rule in RESEARCH Pitfall 5 | none |

## Pattern Assignments

### `TestsWorkflowPinGuardUnitTest.java` (test, transform)

**Analog:** itself. Rules are static package-private `Optional<String> xxxProblem(List<String>)` (empty = OK, message names the offender); tests wrap them with `assertNoProblem(...)`; fixtures use `accepts`/`rejects` Executables inside `assertAll`.

**Reader (DEBT-13)** current code to replace (line ~305):
```java
static List<String> ktsCode(String text) {
    return text.replaceAll("(?s)/\\*.*?\\*/", "").lines()
            .filter(line -> !line.stripLeading().startsWith("//"))
            .collect(Collectors.toList());
}
```
Replace body with the one-pass scanner (RESEARCH Pattern 6: states raw `"""`, string/char with `\`, `//`, nested `/* */` keeping newlines). Keep signature. Read-from-disk callers use `BUILD_SCRIPT` and `ktsAccepts`/`ktsRejects(String text)` helpers (lines ~386-391) built on `forkTaskProblem(ktsCode(text))`; add fixtures as Java text blocks the same way.

**YAML reader** (line 85) — UTF-8 explicit, `#` comment filter; DEBT-14 trailing-comment handling goes in rules, not here:
```java
private static List<String> nonCommentLines(Path file) throws IOException {
    return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
            .filter(line -> !line.stripLeading().startsWith("#"))
            .collect(Collectors.toList());
}
```

**Step-by-name rule (CI-20 model)**: `forkCountStepProblem` (~line 219) with `FORK_STEP` / `GRADLEW_REPORT` patterns (lines 53-56):
```java
private static final Pattern FORK_STEP =
        Pattern.compile("^(\\s*)-\\s+name:\\s*Report test fork count\\s*(?:\\s#.*)?$");
private static final Pattern GRADLEW_REPORT =
        Pattern.compile("^(?!\\s*(?:echo|printf)\\b).*\\./gradlew\\s.*\\breportTestForks\\b.*$");
...
int stepIndent = step.group(1).length();
for (int j = i + 1; j < lines.size(); j++) {
    String line = lines.get(j);
    if (!line.isBlank() && indent(line) <= stepIndent) break;
    if (GRADLEW_REPORT.matcher(line).matches()) return nprocProblem(lines);
}
return Optional.of("Step 'Report test fork count' in " + WORKFLOW + " must call './gradlew ... reportTestForks' in its run block");
```
CI-20: generalise to a helper `(lines, stepName, taskName)` (DRY, D-09); do not copy the regexes.

**Kts-side rule model**: `forkTaskProblem` with `REGISTER_FORKS = ^tasks\.register\("reportTestForks"\)\s*\{\s*$`, scanning until a line equal to `}`; for `verifyConfluentArchive` register + `finalizedBy("verifyConfluentArchive")` inside the `createConfluentArchive` block, copy that loop. Message format ends `... not found in " + BUILD_SCRIPT + " outside comments"`.

**Test/fixture helpers** (lines 355-391): `accepts(rule, String...)`, `rejects(rule, String...)`, `ktsAccepts/ktsRejects`. Red-first: commit fixtures, run `./gradlew testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'` (Pitfall 1: otherwise UP-TO-DATE); proof of real run is `Tests summary: 26 tests`.

**Runner pin** (line 45): `RUNS_ON = ^\s*runs-on:\s*(.*?)\s*(?:\s#.*)?$` — already comment-tolerant; DEBT-14 trailing-comment cases need fixtures plus checks against it.

---

### `ConnectApiReferencesUnitTest.java` (test, file-I/O)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java`

Conventions: package `com.clickhouse.kafka.connect.ci`; extends nothing (extending `ClickHouseBase` drops the `clickhouse` tag exclusion and the class out of `testUnit`); `@DisplayName("<REQ-ID>: ...")`; message-bearing assertions.
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Docker-free pin for ... It extends nothing, because extending {@code ClickHouseBase} would
 * inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}. */
class TestRuntimeClasspathLz4UnitTest {
    @Test
    @DisplayName("DEBT-07: LZ4Factory is on the test runtime classpath exactly once, from at.yawk.lz4")
    void ... throws IOException {
        List<URL> locations = Collections.list(getClass().getClassLoader().getResources(RESOURCE));
        assertEquals(1, locations.size(), "... but was found at " + locations);
```
File reads: read as UTF-8 explicitly (`Files.readAllLines(path, StandardCharsets.UTF_8)`, test JVM runs `file.encoding=windows-1252`), paths relative to project root like `Path.of(".github","workflows","tests.yaml")` in the pin guard. If the list lives in `src/test/resources` (existing: only `clickhouse_keeper_map.xml`, `log4j.properties`), read via classloader; if under `.planning/scripts/`, read via `Path.of`. Main classes: `build/classes/java/main` or `getResources("com/clickhouse/...")`. Pure-Java constant-pool reader (`DataInputStream`), no ASM (RESEARCH).

---

### `build.gradle.kts` (config, batch)

**Analog:** itself.

Existing `confluentJar` (~362) to repair:
```kotlin
tasks.register<ShadowJar>("confluentJar") {
    archiveClassifier.set("confluent")
    from(clickhouseDependencies, sourceSets.main.get().output)
}
```
Target shape: RESEARCH Pattern 3 (`configurations.add(clickhouseDependencies)`, `from(sourceSets.main.get().output)`, `addMultiReleaseAttribute.set(false)`, top-level `shadow { addShadowJarToAssembleLifecycle.set(false) }`). Do not repeat the anchor text in a comment (anchor counts lines).

`createConfluentArchive` (~392) to add `finalizedBy("verifyConfluentArchive")`:
```kotlin
tasks.register<Zip>("createConfluentArchive") {
    group = "Confluent"
    description = "Creates the Confluent Archive zipfile to be uploaded to the Confluent Hub"
    dependsOn("prepareConfluentArchive")
    from(layout.buildDirectory.dir("confluentArchive"))
    ...
    destinationDirectory.set(layout.buildDirectory.dir("confluent"))
}
```
`verifyConfluentArchive`: full prototype in RESEARCH Pattern 4. Shape to copy for lazy config-time providers and `doLast`-only reads is `reportTestForks` (255-262):
```kotlin
tasks.register("reportTestForks") {
    group = "verification"
    description = "Prints the effective maxParallelForks of the test task, one line (read by CI)"
    val forks = tasks.named<Test>("test").map { it.maxParallelForks }
    doLast { println(forks.get()) }
}
```
Needs `import java.util.zip.ZipFile` / `ZipInputStream` at the top (X-6). Rule strings must not contain `/*` (old `ktsCode` hazard; DEBT-13 lands first).

DEBT-15 (line 106): replace
```kotlin
// FORK OVERRIDE (D-16): no upstream catalog entry for this coordinate.
implementation("org.slf4j:slf4j-reload4j:2.0.20")
```
with `implementation(libs.slf4j.reload4j)`; keep the literal out of any comment (anchor row text moves to catalog line in the same commit).

---

### `.github/workflows/tests.yaml` (config)

**Analog:** same file, steps at lines 51-70 (2-space `- name:` list, `run:` one-liner shape):
```yaml
      - name: Compile integration-test sources
        run: ./gradlew compileIntegrationTestJava

      - name: Run tests
        run: ./gradlew test
```
Insert between them: `- name: <archive step name>` / `run: ./gradlew createConfluentArchive`. Actions stay SHA-pinned with `# vX.Y.Z`; no new `uses:`; token stays read-only. Step name must equal what the new pin-guard pattern matches.

---

### `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `conflicted-paths-v1.7.0-tail.txt`

**fork-anchors.txt** row: `path<TAB>fixed-string<TAB>count<TAB>backs-ID`, count = `grep -cF` matching LINES. Examples:
```
build.gradle.kts	slf4j-reload4j:2.0.20	1	BLD-08
gradle/libs.versions.toml	kafka = "4.1.1"	1	BLD-08
```
Edits: merge 1 changes ONLY `kafka = "4.1.1"` to `kafka = "4.4.0"` (X-2); Shadow commit adds `build.gradle.kts<TAB>configurations.add(clickhouseDependencies)<TAB>1<TAB>BLD-15` and `build.gradle.kts<TAB>from(clickhouseDependencies, <TAB>0<TAB>BLD-15` (trailing space in pattern); DEBT-15 moves the `slf4j-reload4j:2.0.20` row from `build.gradle.kts` to `gradle/libs.versions.toml` (`org.slf4j:slf4j-reload4j` line). Anchor row moves in the same commit as the code.

**fork-anchor-exemptions.txt** row (existing, line ~10):
```
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	build.gradle.kts	slf4j-reload4j:2.0.17	Dependabot #3 on origin/custom_build bumped ...
```
Fields: full 40-hex SHA, path, pattern spelled as that commit's own copy, reason (name PR number). Generate from the gate's stderr (`ANCHOR VIOLATION: <short> <path> '<pattern>' expected N got M`), `git rev-parse` the short SHA. 18 rows expected; do not hand-type.

**conflicted-paths-v1.7.0-tail.txt**: plain path-per-line (model `conflicted-paths-v1.7.0.txt`), contents `.gitignore` and `gradle/libs.versions.toml`, generated from `git merge-tree --write-tree --name-only <P1> <P2>`. Must be in the commit AFTER merge 2, not inside it (X-3).

---

### `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` (doc)

**Analog:** `.planning/MERGE-CHECKLIST-v1.7.0.md`. Copy: title line `# Merge Resolution Checklist - ...`; intro naming merge commit, rollback tag, rules list; `## Section 1 — Conflicted` table with columns `path | predicted class | actual class | hunks | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status`; `## Section 2 — Every path upstream changed` generated from `git diff --name-only`; Recorded Facts numbering continues (the v1.7.0 file's start at 17; continue after its last). Add the resolution table from RESEARCH Pitfall 5 and both gate commands with env overrides (D-15: tail gate `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt`; cumulative `verify-merge-invariants.sh 54e63648 HEAD`).

---

### `compare-confluent-archives.py` and `connect-api-refs.py` (utility)

**Analog (style only):** `.github/scripts/jacoco_summary.py`. Copy: shebang `#!/usr/bin/env python3`; module docstring stating purpose, requirement IDs, inputs and "Standard library only"; module-level path/constant names in CAPS; small `def`s; `main()` plus `if __name__ == "__main__":` (lines 117-129). Invoke as `python3 -I`. Use `zipfile`, `hashlib`, `subprocess` (javap) only. Compare script: args `archive-a.zip archive-b.zip allowlist.txt`; output only-in-a/only-in-b/changed/nested-jar count with SHA-256; treat `config/archive/manifest.json` line-wise ignoring `release_date`/`version` (Pitfall 6); allow-list rows `glob<TAB>reason`, every row must be used (model: exemptions file format). Connect-api script: `javap -v` for Methodref/InterfaceMethodref/Fieldref/Class under `org.apache.kafka.`, `javap -p -s` resolution walking superclasses/interfaces.

---

### `docs/FORK-DELTA.md` (doc)

**Analog:** the table "What the fork still patches" (line 23+); rows are 4 columns `what | where | why | backing IDs`. Rows to edit: line 65 (slf4j-reload4j literal -> catalog row, DEBT-15), line 68 (catalog pins: `kafka` 4.4.0 etc.), line 34 (`onPartitionsRevoked` row, re-verify note), plus the "production Connect API version" sentence (D-14, after evidence). New rows: Shadow repair (BLD-15; D-02 multi-release reasoning, D-03 `assemble` line), guava override (say "overrides client-v2's transitive guava", X-1; label advisory by source, X-7), httpclient5. Line 219 sentence about "the one row today" in exemptions needs updating when 18 rows are added. Raw numbers wait for Phase 29.

---

### `gradle/libs.versions.toml` (config)

**Analog:** same file. Library row model (line 70):
```
slf4j-simple = { module = "org.slf4j:slf4j-simple", version.ref = "slf4j" }
```
DEBT-15 adds `slf4j-reload4j = { module = "org.slf4j:slf4j-reload4j", version.ref = "slf4j" }` (no new `[versions]` key). Merge 2 conflict resolution (fork values; add `guava = "33.7.2-jre"`; delete `httpclientTest`; `httpclient5-test` library row at line 45 is removed by upstream) per RESEARCH Pitfall 5; the `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` comment form is preserved.

## Shared Patterns

### Tracked-source and gate discipline
**Apply to:** every commit. `verify-merge-invariants.sh` is never edited. Stage explicit paths only; never `git add -A`/`commit -a` after merge 1 (`gradlew.bat` stays "modified", RESEARCH Pattern 1). Run every Gradle call as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; pin-guard/`testUnit` gates use `--rerun`.

### Docker-free test conventions
**Source:** `TestRuntimeClasspathLz4UnitTest.java`, `TestsWorkflowPinGuardUnitTest.java`
**Apply to:** the pin-guard edits and `ConnectApiReferencesUnitTest`. Untagged, extends nothing, UTF-8 explicit reads, assertion messages name the offending line/entry.

### Anchor-moves-with-code
**Source:** `.planning/scripts/fork-anchors.txt` header comments.
**Apply to:** merge 1 (kafka row), Shadow commit (2 rows), DEBT-15 commit (slf4j row).

### Wording-after-evidence
**Apply to:** D-14 edits (FORK-DELTA Connect API sentence, `ClickHouseSinkTask` comment, three DLQ Javadocs located by `grep -rn 4.1.1 src`): separate commit after the javap evidence exists.

### Merge commit contents
**Apply to:** merge 1 (anchor edit + 18 exemption rows only) and merge 2 (resolved `.gitignore` + catalog only). Record files (`conflicted-paths-*-tail.txt`, tail checklist) go in the next commit (X-3); no `.planning/` commit interleaved with in-progress conflict resolution.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.gitignore` merge-2 union | config | batch | no analog; union of both sides, upstream's `# ecliose` typo kept because provenance needs every line in a parent |
| `verifyConfluentArchive` (zip inspection in Gradle) | task | file-I/O | no existing zip-reading task; use RESEARCH Pattern 4 prototype with `reportTestForks` shape for providers |
| `.github/scripts` / `.planning/scripts` Python archive tooling | utility | batch | only style analog (`jacoco_summary.py`); logic from RESEARCH |
| `.planning/scripts/verify-merge-invariants-selftest.sh` | test | n/a | not modified (X-5); if a new case is ever needed, use helpers `expect_failure`, `report`, `synth_tree` (lines ~45-68) |

## Metadata

**Analog search scope:** `src/test/java/.../ci`, `build.gradle.kts`, `.github/`, `.planning/scripts`, `docs/FORK-DELTA.md`, `gradle/libs.versions.toml`, `.planning/MERGE-CHECKLIST-v1.7.0.md`
**Files scanned:** ~14
**Pattern extraction date:** 2026-10-21
**Note:** RESEARCH.md was read only to line 385 of 629; the remaining part (verification tables, bytecode details) was not re-read, so line numbers in `build.gradle.kts` marked `~` are approximate (found via sed ranges 340-420).
