# Phase 17: CI, Build & Test Hygiene - Research

**Researched:** 2026-10-11
**Domain:** GitHub Actions hardening (SHA pins, token scope, runner pin), Gradle 9.6.1 build hygiene (Lombok config, test-classpath exclude, lazy task output), JUnit pin-guards, ClickHouse `query_log` wire-size proof
**Confidence:** HIGH (every mechanism below was reproduced in a scratch copy of the tree at `/tmp/gsd17/wt`, built with the repo's own wrapper; the repo itself was not modified)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Carried forward (locked earlier; not re-opened)**
- Pin every action to a full commit SHA with a `# vX.Y.Z` comment. Re-resolve each SHA at execution time. If `object.type == "tag"`, peel it to the commit; `gradle/actions` is an annotated tag. Check each SHA against its tag, not just that it resolves (the impostor-commit risk). Research values as of 2026-10-10: checkout v7.0.1, upload-artifact v7.0.2, setup-java v6.0.1, gradle/actions v6.4.0.
- `fork-anchors.txt` row `.github/workflows/tests.yaml  timeout-minutes: 45  1` must survive. Any commit that changes an anchor updates `fork-anchors.txt` in the same commit.
- `build.gradle.kts`:
  - Never repeat the literal `withCapability("org.lz4:lz4-java")`; it is anchored at count 1.
  - Never use `configurations.all { exclude }`.
  - `runtimeClasspath` and `integrationTestRuntimeClasspath` must resolve exactly as before.
- Lombok: append `lombok.jacksonized.jacksonVersion += 2` to the root `lombok.config`. Use `+=` (the key is a list) and `2` (the runtime mapper is Jackson 2). Prove it with a `javap` diff of `ClickHouseFieldDescriptor`'s generated code. `lombok.addLombokGeneratedAnnotation` must stay in effect.
- Process rules:
  - The CI-17 push needs the user's own explicit answer at an orchestrator-inline gate. A relayed approval is refused.
  - A red run is diagnosed and the work stops (Phase 14 D-14, Phase 16 D-11): no blind re-run, attribute the failure first, and a second run only with the user's OK.
  - CI is read through the unauthenticated GitHub REST API; `gh` is not installed.
- Existing tests are never deleted or weakened. Gradle always runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. `spotlessApply` runs only on touched files. A two-run gate uses `--rerun`.

**test-head.yaml (CI-16)**
- D-01: **Delete `.github/workflows/test-head.yaml`** from the fork. Both of its jobs call `tests.yaml` as a reusable workflow, with inputs (`clickhouse_version: head`, `clickhouse_java: main`, `run_cloud_tests`) that the fork's latest-only `tests.yaml` does not declare. Testing ClickHouse head and clickhouse-java main is outside the `latest` x V2 target (CI-01..04). Do not add `workflow_call` to `tests.yaml`. CI-16 is proven statically in this phase (the file is absent, and nothing else references it). The effect on the nightly schedule becomes observable only after Phase 20 publishes the deletion to `custom_build`. Reversibility: costly (modify/delete conflict on future upstream syncs; D-02 covers that).
- D-02: Guard against a sync quietly restoring the file: a `docs/FORK-DELTA.md` row records the deletion and its reason; `.planning/MERGE-CHECKLIST-v1.7.0.md` gets one line: "keep `test-head.yaml` deleted on modify/delete"; no new invariant-script logic.

**Dependabot and pin maintenance (CI-15, CI-11)**
- D-03: Add a `package-ecosystem: "github-actions"` entry to `.github/dependabot.yml`, scoped to `tests.yaml` only by using `exclude-paths` on the four upstream workflows. Research must confirm that `exclude-paths` is supported for the `github-actions` ecosystem. If it is not, fall back to a plain `directory: "/"` entry covering all workflows, and record that fallback (and that bump PRs for upstream workflows are to be closed, not merged) in FORK-DELTA.
- D-04: Cadence matches the existing gradle entry: `interval: "monthly"`, label `dependencies`, `open-pull-requests-limit: 5`. A `groups` block bundles all action bumps into one PR. The gradle entry is left untouched.
- D-05: CI-15 is met by the config file alone. Research checks, via the API or the docs, whether Dependabot version updates actually run on this fork, or are switched off in its settings. If enabling them is a settings toggle, add it to the Phase 20 operator steps as a manual check for the user. Make no settings change from here.
- D-06: Add a **Docker-free JUnit guard in `testUnit`** that reads `.github/workflows/tests.yaml` and fails unless: every `uses:` line is pinned to `@[0-9a-f]{40}` followed by a `# vX.Y.Z` comment; `runs-on` is `ubuntu-24.04` and no `ubuntu-latest` is left; `permissions` has `contents: read` and checkout has `persist-credentials: false`. Use a plain regex or line scan, with no new YAML dependency. Additive: a new test class.

**LZ4 response-leg proof (DEBT-06)**
- D-07: The primary assertion is `ProfileEvents['NetworkSendBytes']` against `result_bytes` from `system.query_log`, for **client-v2's own read query id** in `Lz4RoundTripLiveTest`. "Compressed" means LZ4 here because the session asserts `network_compression_method=lz4` (existing assertion around L128-130). The short spike confirms the read path works through client-v2. Fallback 1: raw `java.net.http` LZ4 method-byte (`0x82`) check from ARCHITECTURE.md. Fallback 2: narrow the test's `@DisplayName` and Javadoc to "request leg verified, response leg by read-back". Never leave the "both legs" claim unbacked.
- D-08: Use a **wide ratio bound**, e.g. `NetworkSendBytes < result_bytes / 10`. Log the measured ratio on every run. Do not try to tell LZ4 from ZSTD by magnitude.
- D-09: Prove "seen red" with a **one-off local flip**: temporarily set `compressServerResponse(false)` and run the test. Record the command, the assertion message and the measured bytes in the phase evidence file. Revert without committing. No committed negative-control test.

**CI-17 push**
- D-10: Push **once, at the end of the phase**: after all 11 requirements are implemented and the local gates are green (`testUnit`, a full live suite on `latest` x V2, `verify-merge-invariants.sh` anchors).
- D-11: Push **`sync/v1.7.0`** (Phase 14 D-13 precedent): explicit refspec, branch only, no tag. `custom_build` is untouched. The SHA must differ from `origin/custom_build`'s. Delete the branch from `origin` once its run is green. `origin/sync/v1.7.0` does not exist right now. The push also publishes the local Phase 16 bookkeeping tail and the Phase 17 commits; that is accepted. Before pushing, confirm `.planning/.gsd-ws-arg` is not tracked and no local-only tag rides along. Reversibility: one-way.
- D-12: **No stop-and-ask at 2026-10-19.** `tests.yaml` is pinned to `ubuntu-24.04`, so the `ubuntu-latest` switch does not affect this run.
- D-13: Acceptance for the run (criterion 3): exactly one `tests.yaml` run on the SHA (by `head_sha` plus workflow path) with no cancelled sibling; the `ubuntu-24.04` image in the "Set up job" runner details; only `Contents: read` / `Metadata: read` in the GITHUB_TOKEN permissions; the job summary and the fork-count `::notice`; both artifacts (`jacoco-coverage-reports`, `gradle-test-reports`). Also check that the Node 20 deprecation annotation for `checkout`/`upload-artifact` is gone.

### Claude's Discretion
- **DEBT-07 mechanism.** STACK prefers a scoped `exclude` on `testRuntimeClasspath`; ARCHITECTURE prefers a test-scoped `dependencySubstitution` built from the catalog. Recommendation: the scoped exclude, placed next to the capability-rule block with a comment. Pin it with a **new Docker-free test class** (not inside `Lz4RoundTripLiveTest`): `ClassLoader.getResources("net/jpountz/lz4/LZ4Factory.class")` yields exactly one URL, from `at.yawk.lz4`. Verify with `dependencyInsight` on `testRuntimeClasspath`, `runtimeClasspath`, `integrationTestRuntimeClasspath` and `clickhouseDependencies`.
- **CI-14 task.** Task name (`printTestForks` in STACK, `reportTestForks` in ARCHITECTURE) and exact output. Read lazily via `tasks.named<Test>("test").map { it.maxParallelForks }`. Step uses `./gradlew -q --console=plain <task> | tail -n 1`; `nproc` appears in the message as context only. Keep the summary heading and the `::notice` (CI-09). No copy of the formula stays in shell.
- **DEBT-08 row value.** One `@CsvSource` row in the existing D-01 NanoTimestamp block of `MetadataFreeJsonWriterTest` whose expected rendering is mantissa-with-fraction scientific notation; confirm against `BigDecimal.valueOf(v, 9).toString()`.
- **`tests.yaml` header comment.** Rewrite to describe the new state (fork-count source, pins, permissions).
- **FORK-DELTA rows.** Rewrite the CI row (line ~51). Close Known costs item 7 (999.2). Add a test-head deletion row, a Dependabot row, and the T-14-16 closure. Add a line that the other upstream workflows are intentionally untouched. Edit only Phase 17's rows; raw numbers are refreshed in Phase 20.
- **Plan split, commit order and evidence layout.** By precedent, use a measured approval bundle and a phase evidence file for: the SHA resolution table, the `javap` diff, the `dependencyInsight` outputs, the red flip and the run URL.

### Deferred Ideas (OUT OF SCOPE)
- Dependabot settings enablement on the fork: a manual check for the user, listed in Phase 20's operator steps (D-05). Not a Phase 17 action.
- Upgrading and pinning the other upstream workflows (`release`, `pre-release`, `increment-version`, `integrationTests`). Already a Future Requirement.
- An invariant-script check that `test-head.yaml` stays absent. Declined in favour of the checklist line (D-02).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CI-11 | Node 24 majors + every action pinned by full SHA with `# vX.Y.Z` (peeled commit for annotated tags) | SHA table below (re-resolved 2026-10-11, identical to 2026-10-10); `runs.using: node24` read from each pinned commit; target `tests.yaml` prototype passes the pin guard |
| CI-12 | `runs-on: ubuntu-24.04` | Label still valid in runner-images README; prototype; `/jobs` API shows the `labels` array |
| CI-13 | `permissions: contents: read` + `persist-credentials: false` | Prototype; checkout `action.yml` at the pinned SHA defaults `persist-credentials` to `true`, so the explicit `false` is required |
| CI-14 | Fork count read from a Gradle task | `reportTestForks` prototype printed `6` (12 CPUs), `1` with `CLICKHOUSE_CLUSTER_NAME=x`, and works under `--configuration-cache` |
| CI-15 | Dependabot `github-actions` ecosystem | `exclude-paths` confirmed for the ecosystem by reading dependabot-core source; **default-branch caveat below** |
| CI-16 | `test-head.yaml` fixed or disabled | D-01 delete; **the default branch is `main`, not `custom_build` (finding F1)** |
| CI-17 | One green run on a pushed SHA | Precedent runs 38037397969 / 38048114845 / 38050860929; what the unauthenticated API can and cannot show (finding F4) |
| BLD-14 | No Lombok Jackson2/3 warning; `javap` unchanged | Reproduced the warning, the fix, and a byte-identical normalized `javap -v -p` diff for both generated classes |
| DEBT-06 | LZ4 response leg asserted and seen red | Spike succeeded through client-v2; measured green and red values below |
| DEBT-07 | `testRuntimeClasspath` resolves only `at.yawk.lz4` | Scoped exclude reproduced: only the `testRuntimeClasspath` line changes; Docker-free single-URL test passes, fails without the exclude |
| DEBT-08 | Pin the mantissa-with-fraction NanoTimestamp form | Rows `123 -> 1.23E-7` and `-123 -> -1.23E-7` run green against the real writer |
</phase_requirements>

## Summary

All eleven requirements are implementable with small, additive edits, and every mechanism was reproduced against the real tree. The prior milestone research (STACK, ARCHITECTURE, PITFALLS) holds: the four action SHAs are unchanged after re-resolution on 2026-10-11, the scoped `exclude` removes exactly the `org.lz4:lz4-java:1.8.0` line from `testRuntimeClasspath` and nothing else, and `lombok.jacksonized.jacksonVersion += 2` removes the warning with a byte-identical `javap` of `ClickHouseFieldDescriptor` and its builder. The DEBT-06 spike succeeded on the primary method (`NetworkSendBytes` against `result_bytes` for the read's own query id through client-v2), so neither fallback is needed.

**Four findings change or qualify the plan inputs** (details in Key Findings): (F1) the fork's GitHub **default branch is `main`** (a stale 2025 upstream mirror), not `custom_build`; `test-head.yaml` is already `disabled_inactivity` there with zero runs, so CI-16 and Dependabot (CI-15) have no observable effect from `custom_build` at all, before or after Phase 20, unless the user changes the default branch. (F2) `exclude-paths` is supported for `github-actions`, matched against repo-root-relative paths. (F3) the live ClickHouse `latest` is now `26.9.16.11`, not `26.9.14.10`; the measured ratio moved from 0.0076 to 0.0129, still ~77x below the D-08 bound. (F4) job logs return 403 unauthenticated, so criterion 3's runner image, token permissions and job summary need a human-pasted "Set up job" section and summary from the web UI; the API gives labels, annotations and artifact names.

**Primary recommendation:** Implement in this order: build and test edits first (`lombok.config`, `reportTestForks`, test-scoped exclude, DEBT-06/07/08 tests), then `tests.yaml` + `dependabot.yml` + `test-head.yaml` deletion + the pin-guard test, then docs, then one gated push of `sync/v1.7.0`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Runner image, token scope, action pins | CI workflow file (`tests.yaml`) | JUnit pin-guard in `testUnit` | The workflow is the single source; the guard stops silent regressions between runs |
| Effective fork count | Gradle build (`build.gradle.kts` task) | CI step formats the message | The `Test` task owns `maxParallelForks`; shell must only read it |
| Lombok generated-code behaviour | Build config (`lombok.config`) | `javap` evidence | Config-only, zero bytecode change |
| Test-classpath de-duplication | Gradle dependency resolution (scoped exclude) | Docker-free classloader test | Resolution is the fix; the classloader URL count is the standing pin |
| LZ4 response-leg proof | ClickHouse `system.query_log` (server-side measurement) | client-v2 under test issues the read | Measures the client under test, not a separate raw-HTTP client |
| Pin maintenance | `.github/dependabot.yml` | Default-branch settings (user) | Dependabot reads config only from the default branch (F1) |

## Standard Stack

### Core (no new dependencies)

| Item | Version | Purpose | Source |
|------|---------|---------|--------|
| `actions/checkout` | v7.0.1 -> `3d3c42e5aac5ba805825da76410c181273ba90b1` (lightweight tag) | Checkout, node24 | [VERIFIED: `git ls-remote https://github.com/actions/checkout refs/tags/v7.0.1` printed `3d3c42e5aac5ba805825da76410c181273ba90b1  refs/tags/v7.0.1`; `action.yml` at that SHA: `using: node24`] |
| `actions/upload-artifact` | v7.0.2 -> `cf430e030ddbb5b0abf93d22962f4752f3646cd9` (lightweight) | Artifact upload, node24 | [VERIFIED: `git ls-remote --tags .../upload-artifact 'v7*'` printed `cf430e030ddbb5b0abf93d22962f4752f3646cd9  refs/tags/v7.0.2`; `action.yml`: `using: 'node24'`] |
| `actions/setup-java` | v6.0.1 -> `de7274f081f381c8f8158605e0321c36c376e2e6` (lightweight) | JDK 17 temurin | [VERIFIED: `git ls-remote --tags .../setup-java 'v6*'`; `action.yml`: `using: 'node24'`] |
| `gradle/actions/setup-gradle` | v6.4.0 -> **commit** `3f5f9adaf7d9fecd50b5935e54106014257a94e6` (annotated tag; tag object `b9bee63ef481cc545e4517ec730544a4d8b1dfa0`) | Gradle cache/summary, node24 | [VERIFIED: `git ls-remote https://github.com/gradle/actions refs/tags/v6.4.0 'refs/tags/v6.4.0^{}'` printed `b9bee63e...  refs/tags/v6.4.0` and `3f5f9ada...  refs/tags/v6.4.0^{}`; REST `git/ref/tags/v6.4.0` returned `object.type == tag`, `git/tags/b9bee63e...` returned commit `3f5f9ada...`; `setup-gradle/action.yml`: `using: 'node24'`] |
| `ubuntu-24.04` runner | image `20261004.327.1` | Pinned runner | [VERIFIED: runner-images README row "Ubuntu 24.04 ... `ubuntu-latest` or `ubuntu-24.04`"; Ubuntu2404-Readme: `Image Version: 20261004.327.1`, `Docker Client 28.0.4`] |
| Lombok | 1.18.46 (catalog `lombok = "1.18.46"`) | Key `lombok.jacksonized.jacksonVersion` | [VERIFIED: gradle/libs.versions.toml:9; warning reproduced and removed, see BLD-14] |
| Gradle wrapper | 9.6.1 | `tasks.named<Test>().map`, `configurations.named().exclude` | [VERIFIED: gradle/wrapper/gradle-wrapper.properties `distributionUrl=...gradle-9.6.1-bin.zip`] |

Re-resolution date: **2026-10-11**; `releases/latest` for the four repos returned v7.0.1, v7.0.2, v6.0.1, v6.4.0, matching 2026-10-10. Plans MUST re-resolve at execution time (commands in Validation Architecture).

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Scoped `exclude` on `testRuntimeClasspath` | test-scoped `dependencySubstitution` from the catalog | Substitution keeps a visible redirect in `dependencyInsight` but is more code and must read `libs.lz4.java`; the exclude was reproduced and is the smaller edit (CONTEXT recommendation) |
| Bare-integer output + `tail -n 1` + integer guard | Sentinel line (`maxParallelForks=6`) | Not needed: with `-q` the wrapper prints nothing before the task output (verified with a fresh `GRADLE_USER_HOME`: stdout held only Gradle's own output, stderr empty) |

**Installation:** none. The only build-file additions are a `configurations.named("testRuntimeClasspath") { exclude(...) }` block and a `tasks.register("reportTestForks")`.

## Package Legitimacy Audit

No external packages (npm/PyPI/crates/Maven) are added in this phase; JaCoCo, Lombok, Mockito and JUnit are already in the tree. GitHub Actions are not registry packages, so `package-legitimacy check` does not apply; their provenance was verified directly:

| Action | Source | Verdict | Disposition |
|--------|--------|---------|-------------|
| actions/checkout, actions/upload-artifact, actions/setup-java | GitHub-owned org `actions`, tags read with `git ls-remote` from github.com | OK (first-party) | Approved |
| gradle/actions | Gradle Inc. org, tag peeled to commit via REST and `git ls-remote ^{}`, two methods agree | OK | Approved |

**Packages removed due to [SLOP] verdict:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### Data flow (CI-17 run, left to right)

```
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0   (branch only, no tag)
   |
   v
GitHub: push event ('**') -> tests.yaml (permissions: contents: read; ubuntu-24.04)
   |
   v
checkout@SHA (persist-credentials:false) -> setup-java@SHA (17) -> setup-gradle@SHA
   |
   v
"Report test fork count": ./gradlew -q reportTestForks | tail -n 1 -> integer guard
   |--> echo + $GITHUB_STEP_SUMMARY + ::notice            (CI-09, CI-14)
   v
compileIntegrationTestJava -> ./gradlew test  (includes TestsWorkflowPinGuardUnitTest,
   |                                          TestRuntimeClasspathLz4UnitTest, Lz4RoundTripLiveTest)
   v
jacoco_summary.py -> upload-artifact@SHA x2 (jacoco-coverage-reports, gradle-test-reports)
   |
   v
Evidence: REST /actions/runs?head_sha, /jobs (labels), /check-runs/{job}/annotations,
          /runs/{id}/artifacts; web-UI paste for "Set up job" + summary (F4)
```

### Recommended file touch list

```
.github/workflows/tests.yaml          pins, runner, permissions, persist-credentials, fork-count step, header comment
.github/workflows/test-head.yaml      DELETED (D-01)
.github/dependabot.yml                + github-actions entry (D-03, D-04)
build.gradle.kts                      + scoped exclude (next to capability rule), + reportTestForks task
lombok.config                         + lombok.jacksonized.jacksonVersion += 2
src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java            NEW, Docker-free (D-06)
src/test/java/.../ci/TestRuntimeClasspathLz4UnitTest.java          NEW, Docker-free (DEBT-07 pin)
src/test/java/.../sink/db/helper/Lz4RoundTripLiveTest.java         + read query id + response-leg assertion; refresh stale Javadoc
src/test/java/.../sink/db/MetadataFreeJsonWriterTest.java          + @CsvSource row(s)
docs/FORK-DELTA.md                    CI row, new rows, items 5/6/7
.planning/MERGE-CHECKLIST-v1.7.0.md   + one line (D-02)
.planning/scripts/fork-anchors.txt    UNCHANGED (all anchors verified intact in the prototype)
```

Test class naming follows the repo convention: Docker-free classes are `*UnitTest` and extend nothing (`ClickHouseVersionDefaultUnitTest` Javadoc: "deliberately extends nothing - not ClickHouseBase, whose inherited @Tag("clickhouse") would drop it out of testUnit") [VERIFIED: read src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java:1-22].

### Pattern 1: Target `tests.yaml` (prototype passes the guard and parses with Ruby YAML)

```yaml
name: ClickHouse Kafka Connect Tests CI

permissions:
  contents: read

on:
  push:
    branches: ['**']

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-24.04
    timeout-minutes: 45
    name: ClickHouse latest Client version V2 tests
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

      - name: Report test fork count
        run: |
          forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)"
          case "$forks" in ''|*[!0-9]*) echo "::error::reportTestForks did not print an integer: '${forks}'"; exit 1;; esac
          msg="runner nproc=$(nproc), Gradle maxParallelForks=${forks} (read from the test task in build.gradle.kts)"
          echo "$msg"
          echo "### Test fork count" >> "$GITHUB_STEP_SUMMARY"
          echo "$msg" >> "$GITHUB_STEP_SUMMARY"
          echo "::notice title=Test fork count::${msg}"
      # ... Compile integration-test sources / Run tests / coverage summary unchanged;
      # both upload steps: uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
```

The integer guard is required: GitHub's default `run` shell is `bash -e {0}` without `pipefail`, so a failing `./gradlew` piped into `tail` would otherwise leave `forks` empty and the step green [ASSUMED: GitHub default-shell semantics from training; the guard is correct either way]. The header comment must be rewritten, must not contain the literal `timeout-minutes: 45` (anchor count stays 1; verified `grep -cF` = 1 on the prototype), and may mention `ubuntu-latest` because the guard strips comment lines.

### Pattern 2: `reportTestForks` (place after the `tasks.withType<Test>` block, before `testUnit`)

```kotlin
// CI-14: CI reads the fork count from here instead of recomputing the formula in shell.
tasks.register("reportTestForks") {
    group = "verification"
    description = "Prints the effective maxParallelForks of the test task, one line (read by CI)"
    val forks = tasks.named<Test>("test").map { it.maxParallelForks }
    doLast { println(forks.get()) }
}
```

Measured on this machine (12 CPUs, Gradle 9.6.1): `-q` prints `6\n` only; `CLICKHOUSE_CLUSTER_NAME=x` prints `1`; `--configuration-cache` prints `6` twice (store + reuse) [VERIFIED: scratch run]. It must not contain `register<Test>("testUnit")` (anchor count 1) and does not.

### Pattern 3: DEBT-07 scoped exclude (place directly after the capability-rule block, before `dependencies {`)

```kotlin
// 999.2: the capability rule above does not fire on testRuntimeClasspath (the Confluent 7.9.1
// test stack's POM modules do not declare the capability), so org.lz4:lz4-java:1.8.0 rides along
// beside at.yawk.lz4. at.yawk.lz4 is a drop-in (same net.jpountz.lz4 package) and is already on
// this classpath through client-v2, so the original is dropped for tests only.
configurations.named("testRuntimeClasspath") {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

Measured with `/tmp/gsd17/lz4cp.sh` (`dependencies --configuration X | grep lz4`), before vs after: the only diff is the removal of `org.lz4:lz4-java:1.8.0` under `testRuntimeClasspath`. `runtimeClasspath` and `integrationTestRuntimeClasspath` still print `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0`; `clickhouseDependencies` unchanged. `grep -cF 'withCapability("org.lz4:lz4-java")' build.gradle.kts` stays 1; `windows-1252` 2, `jacocoForkReport` 3, `register<Test>("testUnit")` 1 [VERIFIED: scratch run]. The comment above deliberately avoids the anchored literal. Root cause of the uncaught capability conflict is unconfirmed upstream-of-Gradle; the fix does not depend on it.

### Pattern 4: DEBT-06 response-leg assertion (spike code, proven green and red)

```java
String readQueryId = UUID.randomUUID().toString();
List<GenericRecord> rows = client.queryAll(
        String.format("SELECT id, payload, amount, ts FROM `%s` ORDER BY id", table),
        new QuerySettings().setQueryId(readQueryId));          // import com.clickhouse.client.api.query.QuerySettings
// ... existing read-back loop and `SYSTEM FLUSH LOGS` (one flush covers the insert and the read) ...
List<GenericRecord> readLog = client.queryAll(String.format(
        "SELECT toInt64(ProfileEvents['NetworkSendBytes']) AS wire, toInt64(result_bytes) AS result, "
                + "Settings['network_compression_method'] AS method "
                + "FROM system.query_log WHERE query_id = '%s' AND type = 'QueryFinish'", readQueryId));
assertEquals(1, readLog.size());
long wire = readLog.get(0).getLong("wire");
long result = readLog.get(0).getLong("result");
LOGGER.info("Response leg: NetworkSendBytes={} result_bytes={} ratio={}", wire, result, (double) wire / result);
assertTrue(result > 0 && wire < result / RESPONSE_COMPRESSION_MIN_RATIO,
        "response not compressed: NetworkSendBytes=" + wire + " result_bytes=" + result);
```

Extract `10` and the SQL fragments into static constants (AGENTS.md "Static string constants"). The extra `Settings['network_compression_method']` read returned `lz4` for the read query id in the spike, so it ties the ratio to LZ4 without a magnitude argument; adding `assertEquals(LZ4, method)` is optional and consistent with D-08 (it does not infer the codec from size) [VERIFIED: spike log `m=lz4`]. `client.queryAll(String, QuerySettings)` and `QuerySettings.setQueryId(String)` exist in client-v2 0.9.6 [VERIFIED: `javap` on client-v2-0.9.6.jar].

### Pattern 5: Docker-free guards (prototypes at `/tmp/gsd17/wt/src/test/java/com/clickhouse/kafka/connect/ci/`)

- `TestsWorkflowPinGuardUnitTest`: line scan with `java.nio.file.Files`, relative path `.github/workflows/tests.yaml` (Gradle's test working dir is the project root; verified). Four tests: `uses:` pinned `@[0-9a-f]{40}` + `# vX.Y.Z` trailing comment (and at least 5 uses found, so an empty scan cannot pass); every `runs-on` equals `ubuntu-24.04`; top-level `permissions:` + `contents: read` and no `: write`; each step using `actions/checkout@` contains `persist-credentials: false`; plus `dependabot.yml` contains `package-ecosystem: "github-actions"`. Comment-only lines are stripped before scanning.
- `TestRuntimeClasspathLz4UnitTest`: `Collections.list(getClass().getClassLoader().getResources("net/jpountz/lz4/LZ4Factory.class"))` has size 1 and the URL contains `at.yawk.lz4`.
- Negative controls run: pre-change `tests.yaml` fails three of the four guard tests (`expected: <ubuntu-24.04> but was: <ubuntu-latest>`, `not pinned to a 40-hex commit SHA: - uses: actions/checkout@v4`, `top-level 'permissions: contents: read' missing`); removing the exclude fails the lz4 test with `expected: <1> but was: <2>` listing both the `at.yawk.lz4/lz4-java/1.12.0` and `org.lz4/lz4-java/1.8.0` jar URLs [VERIFIED: scratch runs].
- Run both under `testUnit` and `test`; neither extends `ClickHouseBase`. Both must go through `spotlessApply` (the Spotless block is `importOrder()` + `removeUnusedImports()` only).

### Pattern 6: `dependabot.yml` github-actions entry (parses with Ruby YAML)

```yaml
  # FORK: keeps the SHA pins in tests.yaml current. The four upstream workflows are not pinned
  # here, so they are excluded; a bump PR that still appears for one of them is closed, not merged.
  - package-ecosystem: "github-actions"
    directory: "/"
    exclude-paths:
      - ".github/workflows/increment-version.yml"
      - ".github/workflows/integrationTests.yaml"
      - ".github/workflows/pre-release.yaml"
      - ".github/workflows/release.yaml"
    schedule:
      interval: "monthly"
    labels:
      - "dependencies"
    open-pull-requests-limit: 5
    groups:
      github-actions:
        patterns:
          - "*"
```

`directory: "/"` for `github-actions` makes Dependabot scan `/.github/workflows` and the root `action.yml` [CITED: docs.github.com dependabot-options-reference, "For GitHub Actions, use the value `/`. Dependabot will search the `/.github/workflows` directory, as well as the `action.yml/action.yaml` file from the root directory."]. The composite action `.github/actions/use-clickhouse-java/action.yaml` (which uses `actions/download-artifact@v4`) is not reached by `directory: "/"`, so it needs no exclude. The gradle entry is left byte-identical.

### Anti-Patterns to Avoid
- Pinning `gradle/actions` to the tag object `b9bee63e...` (fails at job start).
- `configurations.all { exclude(...) }` or touching the `withCapability` block (forbidden by CONTEXT and the anchor).
- Setting `lombok.jacksonized.jacksonVersion += 3`: it does not even compile here (the scratch build with `+= 3` printed zero "Ambiguous" lines but `javap` then reported `class not found: ...ClickHouseFieldDescriptor`, because Jackson 3 is not on the classpath) [VERIFIED: scratch run].
- Rewriting `lombok.config` instead of appending (drops `lombok.addLombokGeneratedAnnotation = true` and `config.stopBubbling = true`).
- Asserting the response leg with `CompressedWriteBufferBlocks`: the spike showed `cwb=0` on the read, so it would be vacuous (PITFALLS Pitfall 7).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Fork count in CI | `nproc / 2` shell formula | `reportTestForks` reading `Test.maxParallelForks` | The shell copy drifted from `build.gradle.kts` (14-REVIEW WR-02) |
| YAML parsing in the guard | A YAML library dependency | Line scan with `java.util.regex` | D-06: no new dependency; the file is small and flat |
| Peeling annotated tags | Eyeballing SHAs | `git ls-remote <repo> refs/tags/<tag> 'refs/tags/<tag>^{}'` | One command shows both the tag object and the commit |
| Detecting duplicate jars | Parsing `dependencies` output in a test | `ClassLoader.getResources(...)` count | Tests the thing that actually matters at runtime |
| Compressed-response proof | A raw-HTTP client | `query_log` `NetworkSendBytes` vs `result_bytes` on the client under test | Spike succeeded; raw HTTP would test the server, not client-v2 |

## Runtime State Inventory

This is not a rename/migration phase in the data sense, but file deletion and a runner pin touch state outside git. Answered explicitly:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None: no datastore keys depend on these names | None |
| Live service config | GitHub workflow registry: `test-head.yaml` is registered as workflow id 92415970, state `disabled_inactivity` (updated 2024-06-25), and `/actions/workflows/92415970/runs` returns `total_count: 0`. Registered workflows on the fork: `pre-release.yaml` (active), `test-head.yaml` (disabled_inactivity), `tests.yaml` (active) [VERIFIED: REST `actions/workflows`, 2026-10-11] | Deleting the file on `custom_build` changes nothing there; the entry lives on `main` (F1) |
| OS-registered state | None | None |
| Secrets/env vars | `tests.yaml` references no secrets (the old `secrets: inherit` lived only in `test-head.yaml`) | None |
| Build artifacts | Local `build/` classes carry the old generated Lombok output; irrelevant after `clean` | Capture the `javap` baseline BEFORE editing `lombok.config` (see BLD-14) |

## Common Pitfalls

### Pitfall 1: `javap` baseline captured after the edit
**What goes wrong:** the before/after diff compares two post-change builds and proves nothing. **Avoid:** build `HEAD` first (a scratch `git archive HEAD | tar -x` copy as used here, or `git stash`), save `javap -v -p` of `ClickHouseFieldDescriptor` and `ClickHouseFieldDescriptor$ClickHouseFieldDescriptorBuilder`, then edit. Normalize with `grep -v -E '^(Classfile|  Last modified|  SHA-256 checksum)'` since the `-v` header carries a path, timestamp and checksum. Both classes were identical in the prototype; the `+= 3` run showed the diff is not vacuous (compile fails).

### Pitfall 2: Fork-count step goes green with an empty value
**What goes wrong:** no `pipefail` plus `| tail -n 1` hides a Gradle failure. **Avoid:** the `case` integer guard in Pattern 1 (verified: fires on empty, passes on `6`).

### Pitfall 3: Evidence the unauthenticated API cannot give (F4)
`GET /actions/jobs/{id}/logs` returned HTTP 403 on a Phase 15 job. Available without a token: run list by `head_sha`, `/jobs` (`labels` was `["ubuntu-latest"]` on the old run, so it will read `["ubuntu-24.04"]`), `check-runs/{job}/annotations`, `/runs/{id}/artifacts` (names `gradle-test-reports`, `jacoco-coverage-reports` listed with `expired: false` on run 38050860929). NOT available: "Set up job" runner image and GITHUB_TOKEN permissions, and the job summary. The plan needs an orchestrator-inline step where the user pastes the "Set up job" section and the summary from the web UI (Phase 14 Recorded Fact 37 precedent).

### Pitfall 4: Unauthenticated rate limit
60 requests/hour per IP; 25 were used during this research. The CI-17 polling loop (90 s interval, precedent in Recorded Fact 39) must budget calls. Check `GET /rate_limit` before and after.

### Pitfall 5: Stale Javadoc after DEBT-07
`Lz4RoundTripLiveTest`'s class Javadoc says "`testRuntimeClasspath` also carries the old `org.lz4` artifact" [VERIFIED: Lz4RoundTripLiveTest.java:37-39]. After the exclude that sentence is false. Update the prose; keep `lz4FactoryLoadsFromTheMaintainedFork` (never delete tests) and keep the `@DisplayName` "BLD-10: client-v2 inserts and reads back over LZ4 on both legs" (it becomes true).

### Pitfall 6: FORK-DELTA has more stale mentions than the CI row
Known costs item 5 lists IN-06 and 14-REVIEW WR-01/WR-02 as open; item 6 states T-14-16 (`tests.yaml` declares no `permissions:` block, pins by tag); item 7 is 999.2 [VERIFIED: docs/FORK-DELTA.md:116-148]. All three are closed by this phase and must be edited in the same plan as their fix.

### Pitfall 7: `latest` drift in the LZ4 test
ClickHouse `latest` moved `26.9.14.10` -> `26.9.16.11`. If the class goes red on unchanged code, check the image label and `SELECT version()` first (memory note `gsd-phase11-orchestration-gotchas`). The D-08 bound has a 77x margin.

### Pitfall 8: Annotated-tag pin comment drift
A SHA whose `# vX.Y.Z` comment does not match the tag is a silent impostor risk. The CONTEXT-mandated check ("check each SHA against its tag") is the `git ls-remote` comparison in Validation Architecture; the in-repo guard can only check shape.

## Code Examples

### DEBT-08 rows (run against the real writer, all PASSED)

```java
"io.debezium.time.NanoTimestamp, -1, -1E-9",
"io.debezium.time.NanoTimestamp, 123, 1.23E-7",
"io.debezium.time.NanoTimestamp, -123, -1.23E-7",
```

`BigDecimal.valueOf(v, 9).toString()` for the candidate values [VERIFIED: `java BD.java`]: `12 -> 1.2E-8`, `100 -> 1.00E-7` (trailing zeros kept), `123 -> 1.23E-7`, `-123 -> -1.23E-7`, `999 -> 9.99E-7`, `1000 -> 0.000001000` (plain: scientific form applies only when the adjusted exponent is below -6, i.e. 2 or 3 digit magnitudes). 15-REVIEW also verified the server reads `1.23E-7`, `9.99E-7`, `-9.99E-7`, `-1.5E-8` back as 123, 999, -999, -15 ns [VERIFIED: 15-REVIEW.md:59]. Minimum for DEBT-08: the `123` row. Cheap extras that pin the form's edges: `999 -> 9.99E-7` and `1000 -> 0.000001000` (the first plain-form value). Extend the test's `@DisplayName` only additively.

### Lombok / BLD-14

Before (HEAD): `ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'. See https://projectlombok.org/features/experimental/Jacksonized` [VERIFIED: scratch `compileJava --rerun-tasks`]. After appending `lombok.jacksonized.jacksonVersion += 2`: zero "Ambiguous" lines across `clean compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava`, and `diff` of the normalized `javap -v -p` output is empty for both generated classes (1196 and 423 lines before). The `lombok.config` after the edit is three lines: `config.stopBubbling = true`, `lombok.addLombokGeneratedAnnotation = true`, `lombok.jacksonized.jacksonVersion += 2`.

## Key Findings Qualifying the Plan Inputs

### F1. The fork's default branch is `main`, not `custom_build` (CI-15, CI-16, D-05)
`git ls-remote --symref https://github.com/ne1r0n/clickhouse-kafka-connect HEAD` printed `ref: refs/heads/main	HEAD`; REST `default_branch` is `main`, `fork: true`; heads are `custom_build` = `be6b8861c25f3d7611194e7150b18362c5c75e32` and `main` = `bec167d85918e77c11621063000acc6e031ae2fa` (a 2025-03-25 upstream merge). `main`'s `.github/dependabot.yml` is upstream's gradle-only file, and `main`'s `tests.yaml` is the old upstream matrix workflow (`on: [push]`, no `workflow_call`).
Consequences, none of which change the plan's code edits but all of which change what can be claimed:
1. Scheduled workflows run from the default branch. `test-head.yaml` there is `disabled_inactivity` with zero runs, so it never fails nightly and never will from `custom_build`. ROADMAP/STATE/CONTEXT statements "runs nightly from `custom_build`" and "observable after Phase 20" are wrong. CI-16 is provable only statically (file absent, no references, no `workflow_call`), which D-01 already accepts; the Phase 20 "check the nightly no longer fails" step (ROADMAP.md:256) has nothing to observe.
2. Dependabot reads `dependabot.yml` from the default branch [CITED: docs.github.com configuring-dependabot-version-updates: "when this file is checked in, Dependabot checks the manifest files on the **default branch**"], and version updates on a fork are off by default until enabled in the fork's settings [CITED: same page, "Version updates are not automatically enabled on forks when a dependabot.yml configuration file is present"]. So the CI-15 entry on `custom_build` is inert until the user both enables Dependabot and either makes `custom_build` the default branch or lands the file on `main`. D-05 already routes the settings question to Phase 20; the planner should widen that operator item to "default branch is `main`" and record it in the FORK-DELTA Dependabot row. Do not change any setting from this phase.
3. Recommend the planner surface F1 to the user at the next gate (it contradicts a stated premise) rather than silently rewording requirements.

### F2. `exclude-paths` works for `github-actions` (D-03)
The docs describe `exclude-paths` generically ("a list of glob patterns for files or directories to ignore", relative to `directory`) without an ecosystem list [CITED: docs.github.com dependabot-options-reference]. dependabot-core at commit `4725d253a379379dc85d291268d9607977b3c1af` applies it in `FileFetchers::Base#filter_excluded`, which `repo_contents` uses, and the GitHub Actions fetcher lists workflows through `repo_contents(dir: ".github/workflows")` [VERIFIED: read common/lib/dependabot/file_fetchers/base.rb:339-360, 495-515, 599-616 and github_actions/lib/dependabot/github_actions/file_fetcher.rb:94-110]. Entry paths are repo-root-relative, and `FileFiltering.exclude_path?` matches exact paths, directory prefixes and globs, so `.github/workflows/<file>` patterns are correct. The SchemaStore `dependabot-2.0.json` schema also contains `exclude-paths`. Caveat: this is source reading, not an observed Dependabot run; mark MEDIUM and keep the D-03 fallback note in FORK-DELTA.

### F3. Spike result for DEBT-06 (research flag resolved: primary method works)
Run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --offline test --tests '*Lz4RoundTripLiveTest'` in the scratch tree against `clickhouse/clickhouse-server:latest`, server `26.9.16.11`, colima profile `vz`:
- Green: `network_compression_method=lz4`; read query: `NetworkSendBytes=61865 result_bytes=4790219 ratio=0.012914858381213886`, `Settings['network_compression_method']=lz4`, `CompressedWriteBufferBlocks=0` (confirms the `Compressed*` event is vacuous for responses); exactly one `QueryFinish` row; `BUILD SUCCESSFUL`.
- Red (one-off flip `.compressServerResponse(false)`): `NetworkSendBytes=4778633 result_bytes=4790219 ratio=0.9975813214385396`; `Lz4RoundTripLiveTest > BLD-10: client-v2 inserts and reads back over LZ4 on both legs FAILED`, `AssertionFailedError: response not compressed: NetworkSendBytes=4778633 result_bytes=4790219 ==> expected: <true> but was: <false>`; `BUILD FAILED`. The flip was reverted in the scratch copy; the real tree is untouched. D-09 requires the executor to repeat this flip on the real tree and record it in the evidence file.
The ratio differs from PITFALLS' 0.0076 (older server build), both far below 0.1. The raw-HTTP fallback is not needed.

### F4. Evidence limits for CI-17 — see Pitfall 3.

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| `checkout@v4`, `upload-artifact@v4` (node20) | v7.0.1 / v7.0.2 (node24) | Node 20 deprecation annotation disappears |
| floating `@v6` tags | full-SHA pins with version comment | Dependabot bumps SHA and comment together |
| `ubuntu-latest` | `ubuntu-24.04` | `ubuntu-latest` begins moving to 26.04 on 2026-10-19 [CITED: runner-images issue #14748 per STACK.md; README row confirms 24.04 is still addressable] |
| Lombok unset Jackson variant | `lombok.jacksonized.jacksonVersion += 2` | warning gone since Lombok 1.18.44 |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | GitHub `run` default shell is `bash -e {0}` without `pipefail` | Pattern 1 | Low: the integer guard is correct under either shell |
| A2 | With `permissions: contents: read` the "Set up job" log lists only `Contents: read` and `Metadata: read` | Success criterion 3 | Medium: only a real run can prove it; the human-pasted log is the proof, so a mismatch is caught at CI-17 |
| A3 | Dependabot honours `exclude-paths` for `github-actions` in production (source-read only, no observed PR) | F2 | Low: fallback in D-03 (close unwanted PRs) is already specified |
| A4 | Dependabot reads `dependabot.yml` from the default branch only | F1 | Low-Medium: documented quote supports it; `target-branch` semantics not read in full |
| A5 | `ubuntu-24.04` still ships Python 3 for `jacoco_summary.py` | Pattern 1 | Low: identical image to today's `ubuntu-latest`, which ran it green in runs 38048114845 and 38050860929 |
| A6 | setup-gradle v6.4.0 does not need token scopes beyond `contents: read` with its defaults | Pattern 1 | Low-Medium: STACK.md cites the action's docs; first run proves it |

## Open Questions

1. **Default branch is `main` (F1).**
   - What we know: verified through `ls-remote --symref` and REST; `test-head.yaml` is disabled_inactivity with 0 runs; `main` carries upstream's old files.
   - What's unclear: whether the user intends `custom_build` to become the default branch (it would also make Dependabot and the schedule meaningful).
   - Recommendation: do not change settings here; correct the "nightly from `custom_build`" wording in FORK-DELTA/STATE when editing them; add "default branch is `main`" to the Phase 20 operator check; ask the user at the CI-17 gate.
2. **Optional `Settings['network_compression_method']` assertion in DEBT-06.** Verified to return `lz4`; adds an LZ4-vs-ZSTD tie that D-08 declines to infer from magnitude. Recommendation: include it as a plain equality assert (it is not a magnitude inference), planner's call.
3. **Extra DEBT-08 rows.** One row satisfies the requirement; `999` and `1000` pin the edges of the scientific range. Recommendation: add `123`, `-123` and `999`; `1000` is optional.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | all Gradle work | yes | toolchain 17 (class major version 61 in `javap`) | none needed |
| Gradle wrapper | build | yes | 9.6.1 | none needed |
| Docker via colima profile `vz` | DEBT-06 live test, full suite | yes (`colima status -p vz`: running, aarch64, docker runtime) | server image `clickhouse/clickhouse-server:latest` = `26.9.16.11` | the Docker-free subset (`testUnit`) |
| `gh` CLI | none | no (not installed; STATE.md says so) | n/a | `curl` against api.github.com and `git ls-remote` (both used here) |
| `curl`, `python3 -I`, `ruby` | evidence scripts, YAML parse check | yes | system | n/a |
| `actionlint`, `yamllint` | extra workflow lint | no | n/a | Ruby `YAML.load_file` parse + the pin-guard test (used here) |
| GitHub REST (unauthenticated) | SHA resolution, CI-17 reads | yes | 60 req/hour; ~35 left at time of writing | `git ls-remote` for tag resolution needs no API quota |

**Missing dependencies with no fallback:** none. **zsh gotcha for scripts:** a variable named `path` clobbers `PATH` in zsh (hit once during research); use a different name in evidence scripts.

## Validation Architecture

> `workflow.nyquist_validation` is `false` in `.planning/config.json`; this section is included because the invocation asked for per-requirement proofs.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) 5.11.x via BOM, Gradle 9.6.1 |
| Config file | `build.gradle.kts` (`testUnit` excludes tag `clickhouse`; `test` runs everything) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` |
| Full suite command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` (needs colima `vz`) |

### Phase Requirements -> Proof Map

| Req ID | Behavior | Test Type | Command | File Exists? |
|--------|----------|-----------|---------|-------------|
| CI-11 | every `uses:` is a 40-hex SHA with `# vX.Y.Z`; pins match their tags | unit + script | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'`; plus per pin: `git ls-remote https://github.com/<owner>/<repo> "refs/tags/<tag>" "refs/tags/<tag>^{}"` and compare the `^{}` line if present, else the tag line, with the pinned SHA | Wave 0 |
| CI-12 | `runs-on: ubuntu-24.04`, no `ubuntu-latest` | unit + run | same guard; after the run `GET /actions/runs/{id}/jobs` -> `labels == ["ubuntu-24.04"]`; web-UI paste of "Set up job" | Wave 0 |
| CI-13 | `permissions: contents: read`, `persist-credentials: false` | unit + run | same guard; web-UI paste shows only `Contents: read` / `Metadata: read` | Wave 0 |
| CI-14 | fork count from Gradle; no shell formula | script | `./gradlew -q --console=plain reportTestForks` prints one integer; `CLICKHOUSE_CLUSTER_NAME=x ./gradlew -q --console=plain reportTestForks` prints `1`; `grep -nE 'nproc ?/|/ ?2|availableProcessors|CLICKHOUSE_CLUSTER_NAME' .github/workflows/tests.yaml` prints nothing | Wave 0 (task) |
| CI-15 | Dependabot has `github-actions` | unit + parse | guard's `dependabotCoversGithubActions`; `ruby -ryaml -e 'p YAML.load_file(".github/dependabot.yml")["updates"].map{\|u\| u["package-ecosystem"]}'` prints `["gradle", "github-actions"]` | Wave 0 |
| CI-16 | `test-head.yaml` gone, nothing references it, no `workflow_call` added | static | `test ! -e .github/workflows/test-head.yaml && ! grep -rIn 'test-head' .github && ! grep -n workflow_call .github/workflows/tests.yaml` | n/a |
| CI-17 | one green run, artifacts, notice, summary | API + paste | `GET https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<sha>` filtered on `path == ".github/workflows/tests.yaml"` (exactly one, conclusion success, none cancelled); `.../actions/runs/{id}/artifacts` lists both names; `.../check-runs/{job_id}/annotations` has the `Test fork count` notice and no Node 20 warning; web-UI paste for image, token permissions, summary | n/a |
| BLD-14 | no Lombok warning; `javap` unchanged | build + diff | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava 2>&1 \| grep -c Ambiguous` prints `0`; `diff` of normalized `javap -v -p` (baseline taken before the edit) empty for both classes | Wave 0 (baseline) |
| DEBT-07 | `testRuntimeClasspath` has only `at.yawk.lz4` | script + unit | `/tmp/gsd17/lz4cp.sh`-style `dependencies --configuration X \| grep lz4` for the four configurations (only the `testRuntimeClasspath` line `org.lz4:lz4-java:1.8.0` disappears); `./gradlew testUnit --tests '*TestRuntimeClasspathLz4UnitTest'` | Wave 0 |
| DEBT-06 | LZ4 response leg asserted and seen red | live + one-off flip | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*Lz4RoundTripLiveTest'` green; flip `compressServerResponse(false)`, rerun, record assertion text and bytes, revert (`git diff --quiet -- src/test/.../Lz4RoundTripLiveTest.java` afterwards matches the committed version) | existing file, edited |
| DEBT-08 | mantissa-with-fraction NanoTimestamp pinned | unit | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*MetadataFreeJsonWriterTest'` | existing file, edited |
| Anchors | no fork anchor moved | script | `bash .planning/scripts/verify-merge-invariants.sh $(git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD \| tail -1) HEAD` -> `result=PASS` (baseline today: `anchor_rows=43 ... result=PASS`) | exists |

### Sampling Rate
- **Per task commit:** the single targeted `--tests` command above, plus `testUnit` after any Gradle or workflow edit.
- **Per wave merge:** `testUnit --rerun` then the anchors script.
- **Phase gate (D-10):** full `test --rerun` on `latest` x V2, anchors PASS, `spotlessCheck` clean on touched files, then the single approved push.

### Wave 0 Gaps
- [ ] `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - CI-11/12/13/15
- [ ] `src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java` - DEBT-07
- [ ] `javap` baseline of `ClickHouseFieldDescriptor` + builder from unmodified `HEAD` (capture before touching `lombok.config`)
- [ ] `lz4cp`-style baseline of `dependencies` for the four configurations
- Framework install: none.

## Security Domain

`security_enforcement` is enabled (ASVS level 1).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / supply chain | yes | Full-SHA action pins, Dependabot grouped updates, pin-guard test |
| V2 Authentication | no | no auth surface |
| V4 Access Control | yes | Workflow-level `permissions: contents: read`; `persist-credentials: false` (no token left in `.git/config`) |
| V5 Input Validation | partial | Integer guard on the Gradle-sourced `forks` value before it reaches `$GITHUB_STEP_SUMMARY` and a `::notice` command |
| V6 Cryptography | no | none |
| V14 Configuration | yes | `ubuntu-24.04` pin removes silent runner-image drift |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mutable action tag retargeted (T-14-16) | Tampering | Pin to a commit SHA; verify SHA against the tag (impostor-commit risk) |
| Annotated-tag object pinned instead of commit | Spoofing/DoS | Peel with `^{}`; guard checks shape, `ls-remote` check proves provenance |
| Over-broad GITHUB_TOKEN | Elevation of privilege | `permissions: contents: read`; nothing in the job needs write |
| Credentials persisted in the checkout | Information disclosure | `persist-credentials: false` |
| Workflow-command injection through computed text | Tampering | Only an all-digit value from Gradle plus `nproc` is interpolated into `::notice`; guard rejects anything else |
| Dependabot PRs for unpinned upstream workflows | Tampering (review fatigue) | `exclude-paths`; close any stray PR |
| Logging secrets/data | Information disclosure | New tests log byte counts and ratios only (AGENTS.md rule) |

## Project Constraints (from CLAUDE.md / AGENTS.md / .claude/CLAUDE.md)

- Start work through a GSD command; no direct repo edits outside a GSD workflow. This research wrote only the RESEARCH.md; all prototypes lived in `/tmp/gsd17/` (a `git archive` copy), the repo tree was not modified.
- Java 17, Gradle Kotlin DSL; Google Java Style via Spotless: `./gradlew spotlessApply` on touched files; Spotless here is `importOrder()` + `removeUnusedImports()`.
- Never delete or weaken existing tests; new functionality needs feature-test coverage; Docker-free tests belong in `testUnit` (no `@Tag("clickhouse")`).
- Test against ClickHouse `latest` + client V2 only for this fork; read `ClickHouseTestHelpers` before writing live tests; descriptive test names; assert read-back values.
- Static string constants extracted to top-level constants; comments only for non-obvious context; do not log secrets or record data.
- `VERSION` bump and `CHANGELOG.md` are upstream PR-rule items; this phase changes no `src/main`, and Phase 20 owns the release version (`VERSION` anchor is `v1.7.0_gg1`, count 1; do not touch).
- Do not commit without explicit instruction in the workflow, do not change PR text; do not upgrade dependencies (Gradle 9.8.0, Lombok 1.18.48 exist and are not approved: Future Requirements).
- Keep changes additive and rebasable: new test files, small targeted edits.

## Sources

### Primary (HIGH confidence)
- `git ls-remote` against github.com/{actions/checkout, actions/upload-artifact, actions/setup-java, gradle/actions}, 2026-10-11 (tags, peeled commits)
- GitHub REST (unauthenticated), 2026-10-11: `releases/latest`, `git/ref/tags`, `git/tags/{sha}`, `commits/{sha}`, `actions/workflows`, `actions/workflows/92415970(/runs)`, `actions/jobs/{id}` (+ `/logs` -> 403), `actions/runs/{id}/artifacts`, `repos/ne1r0n/clickhouse-kafka-connect` (`default_branch`, `fork`)
- raw `action.yml` at each pinned commit (`runs.using`, `persist-credentials` default)
- runner-images README and Ubuntu2404-Readme (labels, image version, Docker client)
- Local scratch reproduction in `/tmp/gsd17/wt` (git archive of HEAD `e0017d69`): Lombok warning and `javap` diffs, `dependencies` before/after, `reportTestForks`, guard and lz4 tests with negative controls, NanoTimestamp rows, live DEBT-06 spike green and red against ClickHouse `26.9.16.11`
- Repo files read this session: tests.yaml, test-head.yaml, dependabot.yml, build.gradle.kts, lombok.config, fork-anchors.txt, verify-merge-invariants.sh, Lz4RoundTripLiveTest.java, MetadataFreeJsonWriterTest.java (L300-345), MetadataFreeJsonWriter.java (L30-60), FORK-DELTA.md (L45-56, L116-148), research/STACK.md, ARCHITECTURE.md (L144-176), PITFALLS.md (Pitfalls 7, 9, 10, 11), 15-REVIEW.md (IN-06)
- dependabot-core @ `4725d253a379379dc85d291268d9607977b3c1af`: `file_fetchers/base.rb`, `file_filtering.rb`, `github_actions/file_fetcher.rb`, `config/file.rb`

### Secondary (MEDIUM confidence)
- https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference (exclude-paths, groups, github-actions directory)
- https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/configuring-dependabot-version-updates (default branch, forks)

### Tertiary (LOW confidence)
- GitHub Actions default-shell/`pipefail` behaviour (A1), token-permission log wording (A2): training knowledge, to be confirmed by the CI-17 run

## Metadata

**Confidence breakdown:**
- Standard stack (SHAs, runner): HIGH, re-resolved by two methods
- Build/test mechanisms (Lombok, exclude, task, tests): HIGH, reproduced with negative controls
- DEBT-06: HIGH, live green and red
- Dependabot behaviour on the fork: MEDIUM (source and docs, no observed PR); default-branch finding F1: HIGH (direct API/ls-remote reads)
- CI-17 evidence plan: MEDIUM (log access limits are factual; token-permission wording is assumed)

**Research date:** 2026-10-11
**Valid until:** SHAs and runner facts 7 days (re-resolve at execution regardless); build/test mechanisms 30 days; `latest` ClickHouse drift can change the LZ4 ratio at any time (bound has ~77x margin)
