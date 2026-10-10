# Phase 14: Dependency Bumps & CI - Pattern Map

**Mapped:** 2026-10-09
**Files analyzed:** 4 modified/created source files, plus 1 evidence-only file group
**Analogs found:** 4 / 4 (one partial for the Docker-free option)

All analog paths below were confirmed git-tracked (`git ls-files`). No gitignored mirror paths are used.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `gradle/libs.versions.toml` (5 one-line edits, 5 commits) | config | version catalog | itself (existing lines 5, 10, 16, 19, 25) | exact (edit in place) |
| `.github/workflows/tests.yaml` (trigger, 2 new steps) | config | CI event-driven | itself (existing steps, lines 8-32) | exact (edit in place) |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java` (new; name is discretionary) | test | request-response (live, compressed insert + read-back) | `.../db/helper/NetworkCompressionPinLiveTest.java` | role-match + same domain |
| Docker-free option: pin of lz4 origin without Docker (optional) | test | transform | `.../db/helper/NetworkCompressionPinUnitTest.java` | partial (pattern for tagging only) |
| `.planning/scripts/fork-anchors.txt` | config | n/a | n/a | no change expected |

## Pattern Assignments

### `gradle/libs.versions.toml` (config)

**Analog:** itself. Plain one-line edits, one commit per version change. Current lines:

```toml
jackson = "2.22.2"            # line 5  -> 2.22.3
jacksonAnnotations = "2.22"   # line 6  -> unchanged (BOM pairs 2.22 with 2.22.3)
lz4Java = "1.10.4"            # line 10 -> 1.12.0 (fallback 1.11.4)
mockito = "5.23.0"            # line 16 -> 5.24.0
okhttp = "5.4.0"              # line 19 -> 5.5.0
orgJson = "20260522"          # line 25 -> 20260814
```
Alias lines (do not touch): `lz4-java` line 48, `jackson-*` 50-52, `mockito-junit-jupiter` 60, `okhttp` 68, `json` 69.

**Anchor note:** `fork-anchors.txt` anchors `gradle/libs.versions.toml	kafka = "4.1.1"	1`. None of the five bump lines is anchored, so no anchor edit is needed. Keep `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)` byte-identical.

---

### `.github/workflows/tests.yaml` (config, CI event-driven)

**Analog:** itself. Current relevant region (lines 8-32):

```yaml
on: [push]

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    ...
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Run tests
        run: ./gradlew test

      - name: Generate coverage summary
        if: always()
        run: python3 .github/scripts/jacoco_summary.py
```

**Trigger edit (D-10):** replace `on: [push]` with
```yaml
on:
  push:
    branches: ['**']
```
Leave the `concurrency` block as is. `'**'` is needed so `sync/v1.7.0` matches (RESEARCH assumption A2).

**New steps (D-12), inserted after "Set up Gradle" and before "Run tests"**, from RESEARCH Pattern 2 (shell body is the project's own derivation of `build.gradle.kts:200-205`):
```yaml
      - name: Report test fork count
        run: |
          cpus="$(nproc)"
          if [ -n "${CLICKHOUSE_CLUSTER_NAME:-}" ]; then forks=1; else forks=$((cpus / 2)); fi
          if [ "$forks" -lt 1 ]; then forks=1; fi
          msg="runner nproc=${cpus}, derived maxParallelForks=${forks} (build.gradle.kts: availableProcessors/2, minimum 1)"
          echo "$msg"
          echo "### Test fork count" >> "$GITHUB_STEP_SUMMARY"
          echo "$msg" >> "$GITHUB_STEP_SUMMARY"
          echo "::notice title=Test fork count::${msg}"

      - name: Compile integration-test sources
        run: ./gradlew compileIntegrationTestJava
```

**Step-summary convention analog:** `.github/scripts/jacoco_summary.py` writes to `$GITHUB_STEP_SUMMARY`; follow its convention.

**Constraints:**
- Anchor `.github/workflows/tests.yaml	timeout-minutes: 45	1` is counted with `grep -cF` over the whole file. Do not add a second `timeout-minutes: 45` and do not remove the existing one at line 17.
- Keep the FORK MODIFICATION header comment (lines 1-5). Spaces, not tabs.
- Use no untrusted `${{ }}` interpolation inside `run:` scripts.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java` (test, live request-response)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java` (211 lines).

**Package, class shape and base class** (lines 1, 47-48):
```java
package com.clickhouse.kafka.connect.sink.db.helper;
...
class NetworkCompressionPinLiveTest extends ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCompressionPinLiveTest.class);
```
Extending `ClickHouseBase` supplies `@Tag("clickhouse")` (ClickHouseBase.java:32), `@TestInstance(PER_CLASS)`, the shared container and the per-class database. Do not add the tag again.

**Imports to reuse** (lines 3-5, 7, 11-13, 15-18, 26-27, 31-32):
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```
New imports: `com.clickhouse.client.api.Client`, `java.io.ByteArrayInputStream`, `java.nio.charset.StandardCharsets`.

**Config from base props** (lines 59-72): take host, credentials and database from `new ClickHouseSinkConfig(getBaseProps())`:
```java
ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
... csc.getHostname(), csc.getPort(), csc.getDatabase(), csc.getUsername(), csc.getPassword(), csc.isSslEnabled()
```

**Test-owned V2 client (no analog in `src/test`; the only `new Client.Builder` is main code, ClickHouseHelperClient.java:187-193).** Copy the builder shape and invert compression:
```java
Client.Builder clientBuilder = new Client.Builder()
        .addEndpoint(url)
        .setUsername(this.username)
        .setPassword(this.password)
        .setClientName(CONNECT_CLIENT_NAME)
        .compressServerResponse(false)      // test flips to true; add .compressClientRequest(true)
        .setDefaultDatabase(this.database);
if (setCompressionMethod) {
    clientBuilder.serverSetting(NETWORK_COMPRESSION_METHOD, LZ4);   // test: serverSetting("network_compression_method", "lz4"), mandatory on 26.9
}
```
Per RESEARCH Pattern 3 also set `.useHttpCompression(false)` (assumption A1), and build the endpoint URL the way `ClickHouseHelperClient` does (protocol, host, port; read lines ~175-184 when writing the test).

**Typed read-back analog** (lines 79-88): V2 `Records` / `GenericRecord` loop with `assertEquals` on typed getters. In the new test use `client.queryAll("SELECT ... ORDER BY id")` and compare every value against the source rows. Never use `countRows`.

**Table lifecycle** (lines 171-177, 206-209):
```java
String tableName = createTopicName("cmp03_" + ...);
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
try {
    ClickHouseTestHelpers.executeQueryIgnoreResult(chc,
        String.format("CREATE TABLE `%s` (id UInt32, str String) ENGINE = MergeTree ORDER BY id", tableName));
    ...
} finally {
    ClickHouseTestHelpers.dropTable(chc, tableName);
    chc.close();
}
```
Close the test-owned `Client` too (try-with-resources, per AGENTS.md).

**Setting assertion analog** (lines 49-50, 85): `getSetting('network_compression_method')` returned and `assertEquals("lz4", ...)`. Reuse the query shape on the test client.

**Required additions beyond the analog (RESEARCH Pitfalls 1, 2 and A1):**
1. Assert `net.jpountz.lz4.LZ4Factory.class.getProtectionDomain().getCodeSource().getLocation()` contains `at.yawk.lz4`. Log the path. Do not hard-code a lz4 version (D-02: the test must be green before and after the pure catalog edit).
2. Insert compressible rows spanning several LZ4 blocks via `insert(table, InputStream, ClickHouseFormat.JSONEachRow)`.
3. Typed columns, for example `UInt32`, `String`, `Array(UInt8)` or `DateTime64`.

**Style:** the repo's Spotless Google Java Format uses 4-space indentation in these files. Run `spotlessApply` only on the new file. Use `@DisplayName` like the analog.

---

### Docker-free analog (reference only): `NetworkCompressionPinUnitTest.java`

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java`. The D-01 round-trip is live by definition and needs no new Docker-free test. If the planner adds a classpath-origin pin that must run in `testUnit`, copy this file's shape:

```java
class NetworkCompressionPinUnitTest {          // extends nothing on purpose
```
Javadoc lines 11-21 explain: "Deliberately extends nothing - extending ClickHouseBase would inherit its clickhouse tag and exclude this class from testUnit." Imports (lines 3-9) are only `assertEquals/assertFalse/assertNull`, `DisplayName`, `Test`. Clients are built against an unreachable endpoint (`new ClickHouseHelperClient.ClickHouseClientBuilder("localhost", 1, null, null, -1)`, lines 29-34), so no Docker is needed.

`testUnit` definition (build.gradle.kts:249-258): `excludeTags("clickhouse")`, JaCoCo disabled. Any test extending `ClickHouseBase` is excluded from it automatically.

## Shared Patterns

### Gate invocation
**Source:** CONTEXT "Carried forward".
**Apply to:** every task that runs Gradle.
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --rerun ... testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileIntegrationTestJava
```
Use `--rerun` on gate runs (an `up-to-date` result is not evidence). Docker comes from colima profile `vz` (`colima start -p vz`, never bare `colima start`).

### Fork-anchor check
**Source:** `.planning/scripts/fork-anchors.txt` (line 40 `.github/workflows/tests.yaml	timeout-minutes: 45	1`; lines 46-49 build/catalog anchors) and `.planning/scripts/verify-merge-invariants.sh`.
**Apply to:** the tests.yaml commit and the phase-end gate. Any commit that changes an anchor updates the file in the same commit. Planned edits change no anchor.

### Max parallel forks formula
**Source:** `build.gradle.kts:200-205`.
**Apply to:** the CI-09 step, which must mirror this exactly:
```kotlin
if (System.getenv("CLICKHOUSE_CLUSTER_NAME") != null) { maxParallelForks = 1 }
else { maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1 }
```

### Evidence commands
**Source:** RESEARCH "Code Examples".
**Apply to:** the lz4 bump task and the CI evidence tasks (`dependencyInsight` on `runtimeClasspath` and `clickhouseDependencies`; OSV query; REST `actions/runs?head_sha=` filtered by `path == ".github/workflows/tests.yaml"`; `check-runs/{job}/annotations` for the fork count).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| Test-owned `Client.Builder` with LZ4 request and response compression | test helper | request-response | No test constructs `Client.Builder` directly. The only sample is main code (`ClickHouseHelperClient.java:187-215`), used here as the builder-shape reference. Use RESEARCH Pattern 3 for the compression flags. |
| `::notice::` fork-count annotation | CI step | event-driven | No existing step emits annotations. Use RESEARCH Pattern 2. |

## Metadata

**Analog search scope:** `.github/workflows/`, `gradle/`, `build.gradle.kts`, `src/test/java/.../sink/db/helper/`, `src/test/java/.../sink/ClickHouseBase.java`, `src/testFixtures/.../ClickHouseTestHelpers.java`, `src/main/.../ClickHouseHelperClient.java`, `.planning/scripts/`.
**Pattern extraction date:** 2026-10-09
