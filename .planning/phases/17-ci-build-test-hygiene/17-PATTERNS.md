# Phase 17: CI, Build & Test Hygiene - Pattern Map

**Mapped:** 2026-10-11
**Files analyzed:** 15 (4 new, 10 modified, 1 deleted)
**Analogs found:** 14 / 15 (the exception is the CI-17 push, which is a process step and not a file)

All paths below were checked with `git ls-files` and are tracked. The new files do not exist yet, so they have no `git ls-files` entry. No analog is a gitignored mirror. `src/test/java/com/clickhouse/kafka/connect/ci/` does not exist yet and will be created.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.github/workflows/tests.yaml` (modify) | config (CI workflow) | event-driven (push) | itself (current content) + RESEARCH Pattern 1 prototype | exact (self) |
| `.github/workflows/test-head.yaml` (delete) | config | event-driven | none (deletion) | n/a |
| `.github/dependabot.yml` (modify) | config | batch (monthly) | its own `gradle` entry | exact |
| `build.gradle.kts` (modify: `reportTestForks` task) | config (build task) | request-response (read, print) | `tasks.register<Test>("testUnit")` block, L240-251 | role-match |
| `build.gradle.kts` (modify: scoped exclude) | config | transform | capability-rule block, L60-72 | role-match (placement, comment style) |
| `lombok.config` (modify) | config | n/a | itself (2 lines today) | exact (self) |
| `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` (new) | test (Docker-free) | file-I/O (line scan) | `ClickHouseVersionDefaultUnitTest.java` | role-match |
| `src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java` (new) | test (Docker-free) | transform (classloader) | `NetworkCompressionPinUnitTest.java` + `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork` | role-match |
| `.../sink/db/helper/Lz4RoundTripLiveTest.java` (modify) | test (live, Docker) | request-response | itself, `insertAndReadBackOverLz4OnBothLegs` | exact (self) |
| `.../sink/db/MetadataFreeJsonWriterTest.java` (modify) | test (parameterized) | transform | the D-01 `@CsvSource` block, L321-335 | exact (self) |
| `docs/FORK-DELTA.md` (modify) | docs | n/a | its CI row (L51) and Known costs items 5-7 (L140-146) | exact (self) |
| `.planning/MERGE-CHECKLIST-v1.7.0.md` (modify) | docs | n/a | existing Section 2 rows and Recorded Facts format | role-match |
| `.planning/scripts/fork-anchors.txt` | config | n/a | UNCHANGED, read-only | n/a |
| Phase evidence file (new, `.planning/phases/17-.../17-EVIDENCE.md`) | docs | n/a | Recorded Facts 35-40 style in `MERGE-CHECKLIST-v1.7.0.md` | role-match |
| CI-17 push of `sync/v1.7.0` | process | n/a | Recorded Facts 37 and 39 (Phase 14/15 pushes) | precedent |

No test in `src/test` reads `fork-anchors.txt`. The anchor check is only `.planning/scripts/verify-merge-invariants.sh`, plus its selftest script.

## Pattern Assignments

### `.github/workflows/tests.yaml` (config, event-driven)

**Analog:** the current file, with the RESEARCH Pattern 1 prototype as the target shape.

**Keep as is** (the existing structure, L14-27):
```yaml
name: ClickHouse Kafka Connect Tests CI

on:
  push:
    branches: ['**']

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true
```
Insert a top-level `permissions:\n  contents: read` between `name:` and `on:`.

**Anchor constraint:** the literal `timeout-minutes: 45` must stay exactly once (`fork-anchors.txt` L40). Do not write it in the rewritten header comment.

**Changes by line:**
- `runs-on: ubuntu-latest` becomes `runs-on: ubuntu-24.04`.
- `actions/checkout@v4` becomes `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` and gets `with:\n  persist-credentials: false`.
- `setup-java@v6` becomes `@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1`.
- `gradle/actions/setup-gradle@v6` becomes `@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0`. This is the peeled commit, not the tag object.
- Both `upload-artifact@v4` become `@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2`.
- Re-resolve every SHA at execution time with `git ls-remote <repo> refs/tags/<tag> 'refs/tags/<tag>^{}'`.

**Fork-count step** replaces the current shell formula (L36-45). The current copy to remove:
```yaml
cpus="$(nproc)"
if [ -n "${CLICKHOUSE_CLUSTER_NAME+set}" ]; then forks=1; else forks=$((cpus / 2)); fi
```
Target (RESEARCH Pattern 1):
```yaml
      - name: Report test fork count
        run: |
          forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)"
          case "$forks" in ''|*[!0-9]*) echo "::error::reportTestForks did not print an integer: '${forks}'"; exit 1;; esac
          msg="runner nproc=$(nproc), Gradle maxParallelForks=${forks} (read from the test task in build.gradle.kts)"
          echo "$msg"
          echo "### Test fork count" >> "$GITHUB_STEP_SUMMARY"
          echo "$msg" >> "$GITHUB_STEP_SUMMARY"
          echo "::notice title=Test fork count::${msg}"
```
After the edit, `grep -nE 'nproc ?/|/ ?2|availableProcessors|CLICKHOUSE_CLUSTER_NAME' tests.yaml` must print nothing. Keep the `### Test fork count` heading and the `::notice` (CI-09).

**Header comment:** rewrite L1-12. Say the fork-count source is the Gradle task, mention the pins and the permissions, and drop "mirrors the maxParallelForks formula".

---

### `.github/workflows/test-head.yaml` (delete)

No analog. Delete with `git rm`. Static proof for CI-16:
```
test ! -e .github/workflows/test-head.yaml && ! grep -rIn 'test-head' .github && ! grep -n workflow_call .github/workflows/tests.yaml
```
Do not add `workflow_call` to `tests.yaml`.

---

### `.github/dependabot.yml` (config, batch)

**Analog:** the existing gradle entry (the file is 13 lines). Leave it byte-identical.
```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/" 
    schedule:
      interval: "monthly"
    labels:
      - "dependencies"
    open-pull-requests-limit: 5
```
Append the entry as a sibling item (RESEARCH Pattern 6): same `schedule`, `labels` and `open-pull-requests-limit`, plus `exclude-paths` listing `.github/workflows/increment-version.yml`, `integrationTests.yaml`, `pre-release.yaml` and `release.yaml`, and `groups: github-actions: patterns: ["*"]`. Note the extension of `increment-version.yml` is `.yml`, the others `.yaml`.

---

### `build.gradle.kts` (two edits)

**(a) `reportTestForks`**

**Analog:** `tasks.register<Test>("testUnit")` (L240-251) for `group` and `description` style.
```kotlin
tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Runs every test not tagged clickhouse; no Docker required"
```
Place the new task after the `tasks.withType<Test>` block (ends L236) and before the `testUnit` comment. The target code is in RESEARCH Pattern 2:
```kotlin
tasks.register("reportTestForks") {
    group = "verification"
    description = "Prints the effective maxParallelForks of the test task, one line (read by CI)"
    val forks = tasks.named<Test>("test").map { it.maxParallelForks }
    doLast { println(forks.get()) }
}
```
Anchors: do not write `register<Test>("testUnit")` a second time. `windows-1252` (count 2) and `jacocoForkReport` (count 3) must not change.

**(b) scoped lz4 exclude**

**Analog:** the capability-rule block (L60-72), same comment-above-block style.
```kotlin
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") {
```
Place the new block right after that block (L72) and before `dependencies {`. Target code is in RESEARCH Pattern 3:
```kotlin
configurations.named("testRuntimeClasspath") {
    exclude(group = "org.lz4", module = "lz4-java")
}
```
Constraints:
- The comment must not contain the literal `withCapability("org.lz4:lz4-java")`, which is anchored at count 1 (`fork-anchors.txt` L46).
- Never use `configurations.all { exclude }`.
- `runtimeClasspath`, `integrationTestRuntimeClasspath` and `clickhouseDependencies` must resolve exactly as before. Prove it with before/after `dependencies --configuration X | grep lz4`.

---

### `lombok.config` (config)

**Analog:** itself. Today it holds `config.stopBubbling = true` and `lombok.addLombokGeneratedAnnotation = true`. Append a third line, and do not rewrite the file:
```
lombok.jacksonized.jacksonVersion += 2
```
Capture the `javap -v -p` baseline of `ClickHouseFieldDescriptor` and `ClickHouseFieldDescriptor$ClickHouseFieldDescriptorBuilder` from unmodified HEAD before the edit. Normalize with `grep -v -E '^(Classfile|  Last modified|  SHA-256 checksum)'`.

---

### `TestsWorkflowPinGuardUnitTest.java` (test, file-I/O)

**Path:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` (new package `com.clickhouse.kafka.connect.ci`).

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java`.

**Pattern to copy** (extends nothing, so it stays in `testUnit`; JUnit 5; Javadoc says why):
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
/** ... This class deliberately extends nothing - not {@link ClickHouseBase}, whose inherited
 * {@code @Tag("clickhouse")} would drop it out of {@code testUnit}. ... */
public class ClickHouseVersionDefaultUnitTest {
    @Test
    void clickhouseVersionDefaultIsTheFloatingLatestTag() {
        assertEquals("latest", ..., "A pinned server version here silently moves ... ");
```
Naming: `*UnitTest`, with descriptive method names and an explanatory failure message in each assertion. A second style reference with `@DisplayName` is `NetworkCompressionPinUnitTest` (its imports are `assertEquals`, `assertFalse`, `assertNull`, `DisplayName` and `Test`).

**Core logic** (RESEARCH Pattern 5): read `.github/workflows/tests.yaml` with `java.nio.file.Files` at a project-root-relative path; strip comment-only lines first.
- Every `uses:` matches `@[0-9a-f]{40}` followed by `# v\d+\.\d+\.\d+`, and at least 5 `uses:` lines are found, so an empty scan cannot pass.
- Every `runs-on` equals `ubuntu-24.04`.
- A top-level `permissions:` with `contents: read` exists and no `: write` appears.
- The checkout step contains `persist-credentials: false`.
- `.github/dependabot.yml` contains `package-ecosystem: "github-actions"`.

No YAML dependency. Spotless here is `importOrder()` + `removeUnusedImports()` only, so run `spotlessApply` on this file.

---

### `TestRuntimeClasspathLz4UnitTest.java` (test, transform)

**Path:** `src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java` (new).

**Analog:** the class-loading assertion in `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork` (L99-110), moved into a Docker-free class:
```java
    @Test
    @DisplayName("BLD-10: net.jpountz.lz4 loads from the maintained at.yawk.lz4 jar")
    void lz4FactoryLoadsFromTheMaintainedFork() {
        URL location = net.jpountz.lz4.LZ4Factory.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        ...
        assertTrue(
                location.toString().contains(MAINTAINED_LZ4_JAR_MARKER),
                "LZ4Factory must come from the at.yawk.lz4 jar, but was loaded from " + location);
    }
```
Constant to reuse (same name, redeclared): `MAINTAINED_LZ4_JAR_MARKER = "at.yawk.lz4"`.

New assertion: `Collections.list(getClass().getClassLoader().getResources("net/jpountz/lz4/LZ4Factory.class"))` has size 1 and the URL contains `at.yawk.lz4`. The failure message lists all URLs. Extends nothing. Do not edit `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork`: never delete or weaken tests.

---

### `Lz4RoundTripLiveTest.java` (test, request-response)

**Analog:** itself, `insertAndReadBackOverLz4OnBothLegs` (L112-178). The existing insert-leg proof is the template:
```java
client.execute("SYSTEM FLUSH LOGS").get().close();
List<GenericRecord> logRows = client.queryAll(String.format(
        "SELECT toInt64(ProfileEvents['CompressedReadBufferBlocks']) AS blocks "
                + "FROM system.query_log WHERE query_id = '%s' AND type = 'QueryFinish'",
        queryId));
assertEquals(1, logRows.size());
long blocks = logRows.get(0).getLong("blocks");
LOGGER.info("CompressedReadBufferBlocks for the insert: {}", blocks);
assertTrue(blocks > 0, "the insert request body was not read as natively compressed blocks");
```
**Edits:**
- The read-back query at L145-146 becomes `client.queryAll(sql, new QuerySettings().setQueryId(readQueryId))`. Add `import com.clickhouse.client.api.query.QuerySettings;` (and keep the `UUID` import already present).
- After the existing `SYSTEM FLUSH LOGS` (one flush covers both queries), add the response-leg query from RESEARCH Pattern 4. Compare `ProfileEvents['NetworkSendBytes']` to `result_bytes` for `readQueryId`, and assert `wire < result / RESPONSE_COMPRESSION_MIN_RATIO` with the constant `= 10`. Log the ratio each run. Optionally `assertEquals(LZ4, Settings['network_compression_method'])`.
- Do NOT use `CompressedWriteBufferBlocks`: it is 0 on the read path, so the assertion would be vacuous.
- Extract the SQL fragments into `private static final String` constants. The file already has this convention, for example `NETWORK_COMPRESSION_METHOD`, `LZ4`, `MIN_BODY_BYTES`.
- Update the class Javadoc sentence at L37-39, "`testRuntimeClasspath` also carries the old `org.lz4` artifact", which is false after DEBT-07. Keep the `@DisplayName` text "BLD-10: client-v2 inserts and reads back over LZ4 on both legs".
- Red-proof (D-09): a one-off, uncommitted flip of `.compressServerResponse(true)` (L56) to `false`. Record the command, the assertion message and the bytes in the evidence file, then revert.

---

### `MetadataFreeJsonWriterTest.java` (test, transform)

**Analog:** itself, the D-01 block at L321-335:
```java
    @ParameterizedTest(name = "D-01: {0} value {1} is written as {2}")
    @CsvSource({
        "io.debezium.time.Timestamp, 0, 0.000",
        ...
        "io.debezium.time.NanoTimestamp, 0, 0E-9",
        "io.debezium.time.NanoTimestamp, 1, 1E-9",
        "io.debezium.time.NanoTimestamp, -1, -1E-9",
    })
```
Append rows after the `-1, -1E-9` row. Values verified against `BigDecimal.valueOf(v, 9).toString()`:
```java
        "io.debezium.time.NanoTimestamp, 123, 1.23E-7",
        "io.debezium.time.NanoTimestamp, -123, -1.23E-7",
        "io.debezium.time.NanoTimestamp, 999, 9.99E-7",
```
`1000 -> 0.000001000` is optional. Only extend the `@DisplayName` additively. Keep the file's `@CsvSource` formatting, with 8-space indent and a trailing comma on the last row.

---

### `docs/FORK-DELTA.md` (docs)

**Analog:** the CI row at L51, a single table line with columns `what | file | why | requirement refs`:
```
| A single-job CI workflow: ClickHouse `latest` ... with a "Report test fork count" step that prints the runner's `nproc` and the derived `maxParallelForks` ... | `.github/workflows/tests.yaml` | Upstream runs ... | CI-05, CI-08, CI-09, CI-10; Recorded Facts 15, 31 and 37 |
```
**Edits (only Phase 17 rows):**
- Rewrite the CI row: SHA-pinned actions, `permissions: contents: read`, `persist-credentials: false`, `ubuntu-24.04`, and the fork count read from `reportTestForks`.
- Add new rows in the same table format for:
  - the `test-head.yaml` deletion and its reason (D-02);
  - the Dependabot `github-actions` entry. Note that the default branch is `main` (finding F1) and that Dependabot on forks is off by default. State the fallback: close stray bump PRs for upstream workflows.
  - the other upstream workflows (`release`, `pre-release`, `increment-version`, `integrationTests`) being intentionally untouched.
- Known costs (L136-146):
  - Item 5 lists IN-06 and WR-01/WR-02 as open; update them.
  - Item 6 (T-14-16, "declares no `permissions:` block and pins its actions by tag") is closed by this phase.
  - Item 7 (backlog 999.2) is closed by the scoped exclude.
- Do not refresh raw numbers; Phase 20 does that.

---

### `.planning/MERGE-CHECKLIST-v1.7.0.md` (docs)

**Analog:** the numbered Recorded Facts list (L88 on, items 35-40) and the Section 1/2 row tables. Add exactly one line, "keep `test-head.yaml` deleted on modify/delete". Put it where a future sync reader will see it, for example in Section 1 or 2 next to the other workflow rows. Edit by slicing the target section first (the `c1480f8f` precedent).

---

### `.planning/scripts/fork-anchors.txt` (UNCHANGED)

Rows touched by this phase and verified intact in the research prototype:
```
.github/workflows/tests.yaml	timeout-minutes: 45	1	CI-05     (L40)
build.gradle.kts	register<Test>("testUnit")	1	BLD-04       (L45)
build.gradle.kts	withCapability("org.lz4:lz4-java")	1	BLD-09   (L46)
```
Gate: `bash .planning/scripts/verify-merge-invariants.sh $(git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD | tail -1) HEAD` must print `result=PASS`.

---

### CI-17 push (process)

**Analog:** the Recorded Facts 37 and 39 procedure (`MERGE-CHECKLIST-v1.7.0.md`). Copy the exact steps:
- Push: `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0`. Branch only, no tag. Before pushing confirm `push.followTags` is unset and `.planning/.gsd-ws-arg` is untracked.
- Compare `git ls-remote --tags origin` before and after with `cmp`.
- Read the run through `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<sha>`. Filter on `path == ".github/workflows/tests.yaml"`, and require exactly one run and no cancelled sibling.
- Poll about every 90 s and budget the 60 requests/hour limit. Check `GET /rate_limit`.
- Job logs return 403 unauthenticated. The runner image, the token permissions and the job summary must be pasted by the user from the web UI at an orchestrator-inline gate.
- Delete the remote branch once green: `git push origin --delete sync/v1.7.0`.

---

## Shared Patterns

### Docker-free test convention
**Source:** `ClickHouseVersionDefaultUnitTest.java` (L1-22) and `NetworkCompressionPinUnitTest.java` (L12-24).
**Apply to:** both new `ci/*UnitTest` classes.
- Name ends in `UnitTest`.
- Extends nothing. `ClickHouseBase` carries `@Tag("clickhouse")`, which would exclude the class from `testUnit` (`useJUnitPlatform { excludeTags("clickhouse") }`, `build.gradle.kts` L246-248).
- The Javadoc explains the pinned regression.
- Every assertion carries an explanatory message.

### Anchor discipline
**Source:** `.planning/scripts/fork-anchors.txt`.
**Apply to:** `tests.yaml`, `build.gradle.kts` and any comment text in them. Never repeat an anchored literal (`timeout-minutes: 45`, `withCapability("org.lz4:lz4-java")`, `register<Test>("testUnit")`). If an anchor must move, update `fork-anchors.txt` in the same commit.

### Gradle invocation and formatting
**Apply to:** all tasks.
- Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.
- Run `spotlessApply` only on touched Java files.
- Use `--rerun` for two-run gates.
- Static string constants are extracted to top-level constants (AGENTS.md).

### Evidence recording
**Source:** Recorded Facts 35-40 in `.planning/MERGE-CHECKLIST-v1.7.0.md`.
**Apply to:** the phase evidence file. It holds:
- the SHA resolution table (`git ls-remote` output with the peeled commit);
- the `javap` diff;
- the `dependencyInsight` / `dependencies` outputs before and after;
- the red-flip output (command, assertion message, bytes);
- the run URL and API reads.

Use the same style: observed commands, exact outputs, UTC timestamps.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `.github/workflows/test-head.yaml` | config | n/a | Deleted, not created |
| `src/test/java/com/clickhouse/kafka/connect/ci/` (package) | test | n/a | The directory is new. The class conventions come from the `*UnitTest` analogs above; no existing test reads workflow files or counts classpath resources. For those two behaviours use the RESEARCH Pattern 5 prototypes (scratch copy at `/tmp/gsd17/wt/src/test/java/com/clickhouse/kafka/connect/ci/`, which may be gone) |

## Metadata

**Analog search scope:** `.github/`, `build.gradle.kts`, `lombok.config`, `src/test/java/**/sink/**`, `docs/FORK-DELTA.md`, `.planning/MERGE-CHECKLIST-v1.7.0.md`, `.planning/scripts/`
**Files scanned:** about 25
**Pattern extraction date:** 2026-10-11
**Verification:** `git ls-files` confirmed every modified-file path and every analog path.
