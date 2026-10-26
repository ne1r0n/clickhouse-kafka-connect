# Phase 31: Fork-Owned Nightly Integration Tests - Research

**Researched:** 2026-10-26
**Domain:** GitHub Actions nightly for a Testcontainers-backed Gradle `integrationTest` task, plus Docker-free byte/shape guards and a name-only skip list in the Gradle build
**Confidence:** MEDIUM-HIGH. The dispatch rule, the Actions API state, the workflow/guard/build shapes and every in-repo value are verified. The root cause of the upstream red cannot be established from public data (job logs return HTTP 403), so it is a measurement task, with ranked hypotheses below.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** The nightly has `workflow_dispatch` and a `schedule` trigger. The schedule is gated by a repository variable: `if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`. Manual dispatch always works; only cron runs are gated. The gate is off at the Phase 32 merge, and the user sets the variable after the dispatch run. Phase 32 edits no `.github/` file, so enabling is an operator setting.
- **D-02:** NIT-03 (the dispatch run) moves to Phase 32 if research confirms GitHub dispatches only workflow files present on the default branch (`custom_build`). The user chose this over a temporary scratch-branch `push` trigger or early publication. Phase 31 then ends with measurement, workflow, guard and a record. REQUIREMENTS.md and ROADMAP.md must be updated for the move, with the user's confirmation at that point, since NIT-03 is mapped to Phase 31 today.
- **D-03:** Research must confirm the dispatch behaviour first; D-02 applies only if confirmed.
- **D-04:** The nightly runs the whole `integrationTest` task (all three classes). `ClickHouseCloudTest` self-skips without cloud properties, and the measurement confirms it skips rather than fails. Upstream's standalone filter is not used.
- **D-05:** The job timeout is set from the local measurement with headroom (about 1.5-2x), recorded with the measurement. No test is dropped for time alone, and the job is not split per class.
- **D-06:** Testcontainers pulls the Confluent and ClickHouse `latest` images at run time. The measurement records the image list and tags, and `latest` drift is accepted and recorded.
- **D-07:** If any skip list is needed, it is a fork-owned exclude in the build (`build.gradle.kts` or a small included file) applied to `integrationTest`, so local and CI runs behave identically. It is not in workflow Gradle filters and not `@Disabled` in test source.
- **D-08:** Each entry is a fully-qualified test name (class#method) with a cause category (environment, harness, cloud-only, connector defect), a one-line reason, and a re-open trigger. Names only, no patterns. A guard may check that entries are names. No existing test is deleted or weakened.
- **D-09:** A connector-defect failure (needs `src/main` change) stops for a user decision. The expected outcome is a skip-list entry with category `connector defect` and a deferred-fix record. No `src/main` edit in this milestone. A harness fix under `src/integrationTest` or `src/testFixtures` is allowed with its reason recorded; an existing assertion is never weakened.
- **D-10:** On failure the nightly uploads test reports and writes the failing test names to `GITHUB_STEP_SUMMARY` plus an annotation, since job logs are not readable without admin rights. No issue automation, so `permissions: contents: read` stays.

### Claude's Discretion

- Cron time (must not fire before the gate is enabled; the gate guarantees that), concurrency group, artifact names and retention.
- Exact shape of the `integrationTests.yaml` byte guard (e.g. expected-bytes hash or embedded copy), provided it was seen red on a one-byte edit and covers NIT-02's "disabled" state through the Actions API check recorded in the phase.
- How the measurement record is laid out, within criterion 1's required contents.

### Deferred Ideas (OUT OF SCOPE)

- Issue automation on nightly failure (needs `issues: write`) - not chosen; revisit if failed-run emails prove insufficient.
- Pinning the ClickHouse image digest - contradicts the `latest` target; not chosen.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| NIT-01 | A new fork-owned nightly workflow runs `integrationTest` on ClickHouse `latest` x client V2 in local Docker, with no cloud job, and is SHA-pinned | Workflow skeleton and pins (Code Examples 1), the shape rules for a Docker-free nightly guard (Pattern 3), what the task needs on a runner (Summary, Environment Availability), timeout method (Pitfall 4) |
| NIT-02 | Upstream `integrationTests.yaml` stays byte-identical and disabled, and a guard check pins that | Byte guard shape with the measured SHA-256 and length (Code Examples 2); Actions API state `disabled_manually` re-checkable with one unauthenticated `curl` (Verified Facts V5) |
| NIT-03 | The nightly passes, or each failure is classified in a named skip list, on a manual dispatch before the schedule is enabled | D-03 settled: dispatch needs the file on `custom_build`, so D-02 applies (Verified Facts V1); gate behaviour and its skipped-run wrinkle (Pitfall 1, Open Question 1); name-only skip list in the build (Code Examples 3); measurement plan (Pattern 1) |
</phase_requirements>

## Summary

**D-03 is settled: D-02 applies.** GitHub's docs say `workflow_dispatch` "will only trigger a workflow run if the workflow file exists on the default branch", and the "Run workflow" button is present only then. The new nightly file reaches `custom_build` only at the Phase 32 publication, which the user chose not to move earlier. So the real dispatch run (NIT-03) cannot happen in Phase 31. Phase 31 ends with the measurement, the workflow file, the guards and the record. The gate in D-01 holds at the merge. REQUIREMENTS.md (traceability row and the NIT-03 note) and ROADMAP.md (Phase 31 criterion 4 and Phase 32's requirements and criteria) must be edited, after the user confirms.

**What `integrationTest` needs on a runner.** Docker, JDK 17 and the Gradle wrapper. The task itself depends on `prepareConfluentArchive`, which builds the shaded connector jar and the `build/confluentArchive` directory that the tests mount into the Connect container, so the nightly needs no archive step of its own. Testcontainers pulls about ten images at run time: nine are pinned tags (Confluent Platform 7.9.6 for zookeeper, server, schema registry, kafka-rest, control centre and ksqlDB; the `cnfldemos` datagen Connect image `0.6.4-7.6.0`; Toxiproxy `2.7.0`; Ryuk) and only `clickhouse/clickhouse-server:latest` floats. On a standalone `latest` run only `ClickHouseSinkConnectorIntegrationTest` really executes: `ExactlyOnceTest` aborts in `@BeforeAll` through an assumption unless the run is cluster or cloud, and `ClickHouseCloudTest` aborts through an assumption unless the cloud system properties exist. So running "the whole task" and running upstream's standalone filter do the same work on this target, which makes D-04 cheap. The client version caveat matters for the record: `CLIENT_VERSION=V2` reaches only the test-side helper client and the Avro/Protobuf connector configs, because the connector's own default is `V1` and four of the six connector JSON files set no `client_version`.

**Why upstream's standalone `latest` x V2 job fails (run 38881153720).** Not determinable from public data. The run is the fork's own scheduled run #2 of the upstream-identical workflow (SHA `a246ca01`, created 2026-10-16T09:32:28Z); all 43 jobs failed; the V2 job failed in the Gradle step after 29.6 minutes with only the annotation "Gradle build failed: see console output for details"; its log returns 403 without a token. What the Actions API does show: upstream's own run history was fully green on 2026-06-21 (run 164, 43 of 43 jobs, standalone `latest` x V2 in about 20 minutes) and has failed in every one of the 127 runs since, starting after the 2026-06-22 merge of upstream's new Protobuf integration tests (`0b3cab9e`, merged in `a16b5e55`). One failing test in `ClickHouseSinkConnectorIntegrationTest` fails every standalone and cluster job, which explains "43 of 43" without 43 separate causes. The local measurement must reproduce and name the failing test(s); ranked hypotheses are in Pitfall 7. Phase 31 can also read the log if the user creates a short-lived read-only token (the Phase 17 route).

**Primary recommendation:** Build in this order, each step seen red before green: (1) the measured local `integrationTest` run on `latest` x V2 with a cold image cache and a pulled `latest`; (2) the Docker-free byte guard for `integrationTests.yaml`; (3) the nightly workflow with a Docker-free shape guard; (4) the name-only skip list and its reader in `build.gradle.kts`, filled only from step 1; (5) the record and the user-confirmed NIT-03 move to Phase 32. Set the job timeout from the cold local wall time, and use a shorter step-level timeout on the Gradle step so the report uploads still run.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Schedule and manual trigger, gate | GitHub Actions workflow file | Repository variable `NIGHTLY_ENABLED` (operator setting) | The gate is a job-level `if` reading the `vars` context; enabling is a UI setting, so no `.github/` edit is needed later |
| Image pulls, container lifecycle | Runner Docker via Testcontainers (test JVM) | none | The tests start every container themselves; the workflow adds no service containers |
| Connector under test | `build/confluentArchive` built by Gradle (`prepareConfluentArchive`) | Connect worker container | The task depends on the archive build, so the workflow needs no separate build step |
| Skip list | `build.gradle.kts` (+ one small data file read by it) | Docker-free guard in `testUnit` | D-07: the same list applies locally and in CI; the guard keeps entries names, not patterns |
| Failing-test report (D-10) | Workflow step reading Gradle's JUnit XML | `GITHUB_STEP_SUMMARY`, annotation, artifact | Job logs are unreadable without admin rights, so the signal has to be in the summary |
| Upstream workflow byte pin (NIT-02) | Docker-free JUnit guard (`testUnit`) | Actions API check recorded in the phase | The bytes are checkable offline; the disabled state exists only on GitHub |
| Measurement record | Phase evidence file (`31-COMMAND-EVIDENCE.md`, by the Phase 30 precedent) | none | Criterion 1 is a record, not code |

## Verified Facts That Settle the Research Flags

| # | Question | Answer | Source |
|---|----------|--------|--------|
| V1 | Does `workflow_dispatch` need the file on the default branch? (D-03) | Yes. "This event will only trigger a workflow run if the workflow file exists on the default branch." The UI button needs the same. | [CITED: docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows]; [CITED: docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/manually-running-a-workflow] "To trigger the `workflow_dispatch` event, your workflow must be in the default branch." |
| V2 | Can it then run on another ref? | Yes, once the file is on the default branch, and "Once a workflow has run at least once, you can dispatch it against any branch or tag via the GitHub API or GitHub CLI". This does not help before publication. | same page; sentence quoted verbatim |
| V3 | Does `schedule` use the default branch? | Yes: "Scheduled workflows will only run on the default branch", `GITHUB_SHA` is the "Last commit on default branch". So the cron goes live at the Phase 32 merge unless gated. | [CITED: same events page] |
| V4 | Can a job-level `if` read a repository variable? | Yes. Row `jobs.<job_id>.if`: contexts `github, needs, vars, inputs`. An unset variable is an empty string, so `vars.NIGHTLY_ENABLED == 'true'` is false until the user sets it. | [CITED: docs.github.com/en/actions/reference/workflows-and-actions/contexts] |
| V5 | State of upstream's workflow on the fork | `Integration Tests`, path `.github/workflows/integrationTests.yaml`, id 384641581, state `disabled_manually`, `updated_at` 2026-10-16T09:38:23Z. Two runs total (#1 2026-10-15T09:39:18Z, #2 2026-10-16T09:32:28Z, both `schedule`, `custom_build`, `a246ca01`, failure). Zero runs created after 2026-10-16T10:00:00Z. The repository is public, a fork, default branch `custom_build`. Everything reads without a token. | [VERIFIED: api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml and .../runs, run this session] |
| V6 | Is `integrationTests.yaml` identical to upstream? | Git blob `3e9b9be050fa05805227b2eee0d1fe8c72df56c0` at the worktree, `HEAD`, `v1.7.0`, `a6f1df83` and `upstream/main`. SHA-256 `07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109`, 4812 bytes, ASCII, LF line endings, ends with a newline. | [VERIFIED: `git rev-parse <ref>:.github/workflows/integrationTests.yaml`, `shasum -a 256`, `wc -c`, run this session] |
| V7 | Runner size | `ubuntu-24.04` for a public repository: 4 vCPU, 16 GB RAM, 14 GB SSD. Image `20261023.334.3` has Docker 28.0.4, Compose 2.38.2 and Java 17.0.20 as the default. | [CITED: docs.github.com/en/actions/reference/runners/github-hosted-runners]; [CITED: github.com/actions/runner-images Ubuntu2404-Readme.md] |
| V8 | Healthy runtime of the standalone job on a hosted runner | Upstream run 164 (2026-06-21, all 43 jobs green): standalone `latest` x V2 started 06:36:20, ended 06:56:38, so about 20 minutes including checkout, Gradle and pulls. Red runs take 30 to 52 minutes, because each connector that never reaches RUNNING waits 5 minutes. | [VERIFIED: api.github.com/repos/ClickHouse/clickhouse-kafka-connect/actions/runs/27896130598/jobs and .../28006728044/jobs]; wait value `private static final long CONNECTOR_RUNNING_TIMEOUT_MINS = 5L;` [VERIFIED: ConfluentPlatform.java:67] |
| V9 | Upstream's history of this workflow | 274 runs listed; last success is run 164 (2026-06-21, SHA `54342d64`); all 127 later runs failed, latest verified #291 on 2026-10-26. First widely red run #166 on `a16b5e55`, the merge of `0b3cab9e` "Added protobuf tests and schemas". | [VERIFIED: api.github.com/repos/ClickHouse/clickhouse-kafka-connect/actions/workflows/78218174/runs, three pages; `git log` for the SHAs] |
| V10 | Cron delay in practice | The fork's own `cron: "00 05 * * *"` runs were created at 09:39 and 09:32 UTC, about 4.5 hours late. | [VERIFIED: the two run `created_at` values above] |

## Standard Stack

No new libraries. Everything below already exists in the repository.

### Core
| Item | Version / pin | Purpose | Why Standard |
|------|---------------|---------|--------------|
| `actions/checkout` | `3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` | checkout, with `persist-credentials: false` | Phase 30 pin, quoted from `tests.yaml:41-43` [VERIFIED: tests.yaml:41] |
| `actions/setup-java` | `de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1`, `java-version: '17'`, `distribution: 'temurin'` | JDK 17 | `tests.yaml:46-49` [VERIFIED] |
| `gradle/actions/setup-gradle` | `3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0` | Gradle cache and setup; the wrapper is Gradle 9.8.1 | `tests.yaml:52` [VERIFIED]; `distributionUrl=...gradle-9.8.1-bin.zip` [VERIFIED: gradle-wrapper.properties] |
| `actions/upload-artifact` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2` | reports on failure | `tests.yaml:84` [VERIFIED] |
| Runner | `ubuntu-24.04` | Docker 28.0.4 preinstalled | V7 |
| Testcontainers | 2.0.5 (`testcontainers`, `-clickhouse`, `-kafka`, `-toxiproxy`) | Containers for the tests | [VERIFIED: gradle/libs.versions.toml:24] |
| JUnit / Gradle | JUnit Jupiter 5.14.4; Gradle `TestFilter.excludeTest(className, methodName)` | Guards; name-only skip-list exclude | [VERIFIED: libs.versions.toml junitJupiter]; [CITED: docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/TestFilter.html] |
| Python 3 stdlib | `xml.etree.ElementTree`, no third-party code | Optional failing-test summary script | Precedent `.github/scripts/jacoco_summary.py`, "Standard library only" [VERIFIED: jacoco_summary.py header] |

### Images pulled at run time (record these in the measurement)
| Image | Tag | Source line | Floats? |
|-------|-----|-------------|---------|
| `confluentinc/cp-zookeeper`, `cp-server`, `cp-schema-registry`, `cp-enterprise-control-center`, `cp-ksqldb-server`, `cp-kafka-rest` | `7.9.6` | `private static final String CONFLUENT_VERSION = "7.9.6";` [VERIFIED: ConfluentPlatform.java:32] | No |
| `cnfldemos/cp-server-connect-datagen` | `0.6.4-7.6.0` | `private static final String DATAGEN_VERSION = "0.6.4-7.6.0";` [VERIFIED: ConfluentPlatform.java:33] | No |
| `clickhouse/clickhouse-server` | `latest` | `CLICKHOUSE_VERSION_DEFAULT = CLICKHOUSE_VERSION_LATEST` [VERIFIED: ClickHouseTestHelpers.java:41-43] | **Yes** (D-06) |
| `ghcr.io/shopify/toxiproxy` | `2.7.0` | `TOXIPROXY_DOCKER_IMAGE_NAME = "ghcr.io/shopify/toxiproxy:2.7.0"` [VERIFIED: ClickHouseTestHelpers.java:44] | No |
| `testcontainers/ryuk` | Testcontainers' own default | not in repo | No |

Correction to D-06's wording: the Confluent images are pinned tags; only the ClickHouse image floats. All are multi-arch (amd64 and arm64), so the Apple Silicon colima VM pulls native images [CITED: hub.docker.com tag API, checked for each tag]. Docker Hub reports roughly 10 GB of compressed layers across the set (2.05 GB schema-registry, 2.05 GB datagen, 1.87 GB cp-server, 1.87 GB kafka-rest, 1.40 GB ksqlDB, 1.10 GB control-centre, 0.54 GB zookeeper, 0.31 GB ClickHouse) [CITED: hub.docker.com `full_size`; may be summed over platforms, so treat as an upper bound]. The local Docker VM has none of the Confluent images except an unrelated `cp-kafka-connect-base:7.9.7`, and its `clickhouse/clickhouse-server:latest` is 11 days old [VERIFIED: `docker images` through the vz socket]. The first local run is therefore a cold pull of several GB.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Variable-gated job `if` (D-01, locked) | `gh workflow disable` until the dispatch | A disabled workflow cannot be dispatched from the UI without re-enabling it, which also re-arms the cron [ASSUMED]. The variable gate keeps dispatch always possible. |
| Python summary script | `awk`/`grep` over the XML inside `run:` | Shell parsing of XML is fragile; the repo already has a stdlib Python precedent. |
| Hash of the bytes | Embedded full copy of the 4812-byte file | A copy gives a readable diff; a hash is one constant. Recommended: hash plus length, with the failure message printing both actual values. |

**Installation:** none. **Package legitimacy:** this phase installs no external package. The four actions are already SHA-pinned in `tests.yaml` and are covered by the Phase 30 guard; the Python script is standard library only. A `## Package Legitimacy Audit` table is not needed.

## Architecture Patterns

### System Architecture Diagram

```
 cron tick (default branch)            "Run workflow" (operator, custom_build)
        |                                         |
        +------------------+----------------------+
                           v
        job-level gate:  github.event_name == 'workflow_dispatch'
                         || vars.NIGHTLY_ENABLED == 'true'
              |                                   |
          false: run shows as skipped         true: run starts
                                                  v
   checkout (no persisted token) -> setup-java 17 -> setup-gradle
                                                  v
   capacity probe (df, free, docker version)  -> summary
                                                  v
   ./gradlew integrationTest   [env CLICKHOUSE_VERSION=latest, CLIENT_VERSION=V2; step timeout < job timeout]
        |  dependsOn prepareConfluentArchive -> confluentJar -> build/confluentArchive
        |  test JVM (Testcontainers) -> docker pulls -> starts network:
        |      zookeeper, cp-server, schema-registry, datagen Connect (mounts build/confluentArchive),
        |      control-center, ksqldb, rest-proxy, ClickHouse latest, toxiproxy
        |  skip list (build.gradle.kts) -> excludeTest(class, method) per entry
        v
   build/test-results/integrationTest/*.xml , build/reports/tests/integrationTest/
        v
   if: always()  summarise failures -> GITHUB_STEP_SUMMARY + ::error annotation
   if: always()  upload-artifact (XML + HTML reports)
```

### Recommended Project Structure
```
.github/workflows/nightly-integration-tests.yaml   # new; name differs from upstream's "Integration Tests"
.github/scripts/failed_tests_summary.py            # optional; stdlib only
gradle/integration-test-skip-list.tsv              # new; zero data rows if the run is green
build.gradle.kts                                   # extends the existing tasks.named<Test>("integrationTest") block (line 388)
src/test/java/com/clickhouse/kafka/connect/ci/
    UpstreamIntegrationWorkflowBytesGuardUnitTest.java   # NIT-02
    NightlyWorkflowGuardUnitTest.java                    # NIT-01 shape
    IntegrationSkipListGuardUnitTest.java                # D-08
.planning/scripts/fork-anchors.txt                  # anchor rows land in the same commit as each edit
.planning/phases/31-.../31-COMMAND-EVIDENCE.md      # measurement record
```
Name the workflow file so it cannot collide with upstream's files (`nightly.yaml`, `integrationTests.yaml`, `test-head.yaml` exist upstream). Give it a `name:` different from "Integration Tests", otherwise the Actions UI shows two workflows with one title [ASSUMED: UI groups by name, with different paths].

### Pattern 1: Measurement before workflow (criterion 1)
**What:** One local run, with the evidence the record needs. **Schedule these as a measurement task, not as research:**
1. `docker pull clickhouse/clickhouse-server:latest` first and record its digest. Testcontainers does not re-pull an image that is already present [ASSUMED], and the local `latest` is 11 days old, while CI always pulls fresh.
2. Run cold for the wall time that sets the timeout (D-05): the Confluent images are absent locally today, which is the CI condition. Command shape: `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew integrationTest`. `config.json` already runs Gradle with `env -u TESTCONTAINERS_HOST_OVERRIDE`, and STATE records that this Claude Code shell sets the variable to the literal `"null"`, which breaks Ryuk. Docker is colima profile `vz` (running, aarch64, 8 CPU, 16 GiB, `DOCKER_HOST` already points at `~/.colima/vz/docker.sock`); never run a bare `colima start`. The `docker` CLI is at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
3. Capture: `SELECT version()` (from the pulled image, e.g. `clickhouse-local`, or the test log), the image list with tags and digests (`docker image inspect`), every test class as ran or skipped with counts, the Gradle summary line, and the wall time. Do not trust the summary alone for the skipped classes: how Gradle reports a class aborted in `@BeforeAll` (as skipped, or absent from the XML) is not known [ASSUMED]; read `build/test-results/integrationTest/`.
4. On any failure, capture before teardown: the connector status JSON (`GET /connectors/<name>/status`) and the Connect container log (`docker logs`), `java -version` inside the datagen image, and the worker's `connect-api` jar version. The test classes only log through SLF4J, so the container logs are not in the test report. A harness addition that attaches a log consumer is allowed by D-09 if needed, with its reason recorded.
5. Record `docker stats --no-stream` mid-run, to judge the 16 GB runner.
6. The OpenJ9 caveat from memory applies to the unit suite, not to `integrationTest` (JaCoCo is off there, [VERIFIED: build.gradle.kts:389]), but if the test JVM exits 255, read `javacore.*.txt` before blaming the change, and delete the dumps one file at a time.
7. Say in the record that `CLIENT_VERSION=V2` governs the test-side helper client and the Avro and Protobuf connector configs only: the default is `this.clientVersion = props.getOrDefault(CLIENT_VERSION, "V1");` [VERIFIED: ClickHouseSinkConfig.java:331], and `clickhouse_sink.json` and the schemaless, JDBC-property and no-proxy configs set no `client_version`, while `clickhouse_sink_avro.json`, `clickhouse_sink_avro_json.json` and `clickhouse_sink_protobuf.json` set `"client_version": "V2"` [VERIFIED: grep of src/integrationTest/resources]. Do not change the JSON files to make the label true; that alters what is tested.

### Pattern 2: The nightly must pass Phase 30's guard unchanged
`WorkflowActionPinGuardUnitTest` lists `.github/workflows/`, skips only `integrationTests.yaml`, requires at least one `uses:` line, and checks each against `PINNED_USES`: `^\s*(?:-\s+)?uses:\s+[\w.-]+/[\w./-]+@[0-9a-f]{40}\s+#\s+v\d+\.\d+\.\d+\s*$` [VERIFIED: WorkflowActionPinGuardUnitTest.java, TestsWorkflowPinGuardUnitTest.java:44-46]. Copy the four `uses:` lines from `tests.yaml` exactly. No `./` or `docker://` actions. Dependabot's `exclude-paths` lists only the upstream file [VERIFIED: dependabot.yml:22-23], so the nightly's pins stay current with no edit.

### Pattern 3: A Docker-free shape guard for the nightly
Extend nothing (so it stays in `testUnit`), read as UTF-8, return `Optional<String>` problems, and prove each rule on fixtures before pointing it at the real file. Reuse the package-private statics of `TestsWorkflowPinGuardUnitTest` where their messages are generic: `runsOnProblem` (every runner exactly `ubuntu-24.04`, no floating label outside comments), `writeScopeProblem`, `permissionsBlockProblem` (exactly one column-0 `permissions:` followed by `  contents: read`), `checkoutCredentialsProblem`. Prefix the caller's assertion text with the file name, because those messages name `tests.yaml`. **Do not reuse `pinnedUsesProblem`**: it requires at least five `uses:` lines (`MIN_PINNED_ACTIONS = 5`) and the nightly has four or five depending on the number of uploads; pin coverage is already provided by Phase 30's guard.
Rules to add for the nightly (each seen red on a fixture):
- Triggers are exactly `schedule` and `workflow_dispatch`: no `push`, `pull_request*`, `workflow_run`, `repository_dispatch`.
- The job `if:` line equals the D-01 expression byte for byte.
- A job-level `timeout-minutes:` integer, and a step-level `timeout-minutes:` on the Gradle step that is smaller.
- The Gradle step calls `./gradlew` with `integrationTest` in command position, sets `CLICKHOUSE_VERSION: latest` and `CLIENT_VERSION: V2` explicitly, and carries no `--tests` filter (D-04).
- No cloud reference: no `secrets.` at all; `vars.` only as `vars.NIGHTLY_ENABLED` (criterion 2 says "no cloud secret or variable", and the gate itself reads a variable, so a blanket "no `vars.`" rule would be red on the real file); no `CLICKHOUSE_CLOUD`, no `-Dclickhouse.`, no `CLICKHOUSE_VERSION: cloud`.
- The failure-report and upload steps carry `if: always()` (this covers failure, and a step-level timeout).
- No `continue-on-error: true` on the Gradle step.
Explicit env values are a deliberate choice: the defaults already resolve to `latest` and `V2` [VERIFIED: ClickHouseTestHelpers.java:41-43, 91-98], and `tests.yaml` sets none on purpose, but the nightly's guard can only pin what the file states, and an explicit value protects the nightly if the default ever moves.

### Pattern 4: Name-only skip list in the build (D-07, D-08)
`TestFilter.excludeTest(className, methodName)` is an exact class-and-method exclusion, not a pattern ([CITED: Gradle TestFilter Javadoc]: "Excludes a test method specified by test class name and method name"). Keep the data in a small file the build parses, so the Docker-free guard can read the same file. Entry fields: `class#method`, category (`environment | harness | cloud-only | connector-defect`), one-line reason, re-open trigger. The build fails with a clear message on a malformed line. The guard checks: file may have zero entries; each class exists under `src/integrationTest/java`; each method name occurs in that class; no `*`, `?` or `[` in a name; categories belong to the set; no duplicates; reason and trigger non-empty. Empty list is the expected outcome if the measurement is green.
**Granularity limit to decide before it bites:** `avroSchemaTest` and `protoSchemaTest` are parameterized over every fixture file (25 Avro and 22 Protobuf fixtures today [VERIFIED: `ls` of `src/testFixtures/{avro,proto}/schemas/compatible`]), and the fixture list comes from `File.listFiles()` with no sort [VERIFIED: ClickHouseSinkConnectorIntegrationTest.java:286-290, 445-449], so invocation indices are not stable. A `class#method` entry would drop all invocations of that method. See Open Question 3.

### Anti-Patterns to Avoid
- **Filtering in the workflow** (`--tests`, `-PexcludeTests`): D-07 forbids it; local and CI must behave identically.
- **`@Disabled` in test source** to skip: D-07.
- **Gating with `gh workflow disable`:** it blocks dispatch.
- **A job timeout as the only timeout:** on expiry the job is cancelled and the `if: always()` uploads may not get to run [ASSUMED]; give the Gradle step its own shorter timeout.
- **Blanket "no `vars.`" or "no `secrets.`-like" text scans** that trip on the D-01 gate or on comments: scan non-comment lines only, as the existing guards do.
- **Changing `src/main`:** the milestone excludes it. `git diff v1.7.0_gg4 -- src/main` is empty today (0 lines) [VERIFIED: run this session], and the phase gate re-checks it.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Test exclusion by name | Filesystem or reflection filtering of test classes, JUnit `ExecutionCondition` | `filter.excludeTest(class, method)` in the `integrationTest` task | Exact names, applies to local and CI runs, no test-source edit |
| Byte comparison of the upstream file | Line-based diff or `readAllLines` compare | `Files.readAllBytes` + `MessageDigest` SHA-256 + length | `readAllLines` normalises line endings and drops the final newline; a one-byte edit there could pass |
| Pin checking of the nightly | A second pin regex | Phase 30's `WorkflowActionPinGuardUnitTest`, which auto-discovers the file | The task says it must pass with no guard edit |
| Failing-test names | Parsing Gradle console output | JUnit XML in `build/test-results/integrationTest/*.xml` | Stable schema; console text is not |
| Image availability on the runner | Pre-pull or `docker pull` steps | Testcontainers' own pulls | D-06; keeps local and CI identical |
| Disabling the nightly | Workflow edits | The `NIGHTLY_ENABLED` variable | Phase 32 edits no `.github/` file |

**Key insight:** this phase adds almost no executable logic. The risk is in the claims (what is skipped, what is measured, what "no scheduled run" means), so each claim should rest on a guard seen red or a command output pasted into the record.

## Common Pitfalls

### Pitfall 1: "No scheduled run precedes NIT-03" versus a gated cron
**What goes wrong:** With the D-01 gate the cron still fires after the Phase 32 merge; the job is skipped, but GitHub still creates a run for the tick [ASSUMED: a run whose only job is skipped shows in the list with conclusion `skipped`]. ROADMAP criterion 4 and criterion 2 both say a schedule that "cannot fire" / "no scheduled run precedes it".
**How to avoid:** State the acceptance wording in the record and in the Phase 32 criteria: no scheduled run that executed any job precedes the dispatch run; skipped runs from the gated cron are expected and are listed. The check is the public Actions API (`.../workflows/<file>/runs?event=schedule`), reading `conclusion`. Ask the user to confirm the wording (Open Question 1).
**Warning signs:** a scheduled run with a conclusion other than `skipped` before the dispatch run was recorded.

### Pitfall 2: The first dispatch can fail for CI-only reasons, after publication
**What goes wrong:** V1 means the first real run happens only after the Phase 32 merge. A failure that exists only on the runner (memory, disk, Docker version, Docker Hub pull limits, x86 versus the arm64 measurement VM) then needs a build or skip-list change on `custom_build` after the release tip, which disturbs Phase 32's "exactly one green `tests.yaml` run on the published SHA before the tags".
**How to avoid:** Put runner evidence into the workflow (capacity probe step, failing-test summary, uploaded XML), so one failed dispatch yields a complete diagnosis. In the record, list the CI-only risks as a checklist, and flag the ordering problem for Phase 32's planner (Open Question 2). Measure the local run cold.
**Warning signs:** the run is cancelled at the timeout, the disk fills during pulls, or the connector never reaches RUNNING.

### Pitfall 3: `ClickHouseCloudTest` and `ExactlyOnceTest` look like failures but are aborted by assumptions
**What goes wrong:** Both abort in `@BeforeAll` on a standalone `latest` run: `ExactlyOnceTest` through `Assumptions.assumeTrue(isCluster || isCloud, ...)` [VERIFIED: ExactlyOnceTest.java:84], `ClickHouseCloudTest` through `Assumptions.assumeFalse(isCluster, ...)` and then `logAndThrowIfCloudPropNotExists`, which wraps `Assumptions.assumeTrue(properties.get(property) != null)` and rethrows the `TestAbortedException` [VERIFIED: ClickHouseCloudTest.java:64-65, ClickHouseTestHelpers.java:402-410]. A `TestAbortedException` is a skip, not a failure, so D-04's premise holds in source. The measurement must still confirm it in the XML.
**How to avoid:** Never pass `-Dclickhouse.host`, `-Dclickhouse.port` or `-Dclickhouse.password` (the build copies every Gradle JVM system property into the test JVM, [VERIFIED: build.gradle.kts:197-198]); never set `CLICKHOUSE_CLUSTER_NAME` or `CLICKHOUSE_VERSION=cloud` in the nightly. The nightly guard pins the absence.

### Pitfall 4: Timeout design and lost reports
**What goes wrong:** A single job-level timeout cancels the job; the reports and summary may be lost [ASSUMED]. A failing run takes 30 to 52 minutes upstream because of the 5-minute connector waits, so a timeout set from a green local run can be hit by a red run, which is when the reports matter most.
**How to avoid:** Job `timeout-minutes` = step timeout + about 10. Step timeout on the Gradle step set from the cold local wall time with the D-05 headroom of 1.5 to 2x. Upload both `build/test-results/integrationTest/` (XML is written class by class) and `build/reports/tests/integrationTest/` (the HTML report may not exist after a killed Gradle). The healthy CI reference is about 20 minutes (V8); a local arm64 run will not equal it, so record both and say which one the timeout follows.

### Pitfall 5: Runner capacity
**What goes wrong:** Nine long-lived containers, three of them heavy Java processes (control-centre, ksqlDB, cp-server), plus the Gradle test JVM, on 16 GB RAM and a 14 GB SSD [CITED: V7], with about 10 GB of compressed images to pull [CITED: Docker Hub sizes, upper bound]. Upstream passed with the same shape in June, so it fits when healthy, but it is the first suspect for a CI-only failure.
**How to avoid:** Add a cheap probe step (`df -h /`, `free -m`, `docker version`) that writes to the step summary, and record `docker stats` in the local measurement.

### Pitfall 6: Local-versus-CI environment differences in the measurement
**What goes wrong:** Local Docker is arm64 colima 29.x with `TESTCONTAINERS_HOST_OVERRIDE` set in the shell (the literal `"null"` in the Claude Code shell per STATE; `192.168.64.4` in the shell this research ran in), the runner is x86 Docker 28.0.4 without it. A local pass is evidence, not proof.
**How to avoid:** Run with `env -u TESTCONTAINERS_HOST_OVERRIDE`, record the exact command and the Docker and architecture lines, and say in the record which differences remain.

### Pitfall 7: Why the upstream standalone job is red (hypotheses for the measurement to test, ranked)
Not verified; the log is not public.
1. **A failing test added with upstream's Protobuf integration tests** (`protoSchemaTest`, `ProtobufIngestTest`, `clickhouse_sink_protobuf.json`, merged 2026-06-22, first widely red run the next day). A single failing test fails every standalone and cluster job, which fits "43 of 43". Memory records the death site as `ConfluentPlatform.createConnectorAndWaitUntilRunning` (connector never RUNNING, or the POST non-2xx), observed in an earlier session and not re-read now [ASSUMED]. That method asserts a 2xx POST, then waits up to 5 minutes for the first task to be RUNNING [VERIFIED: ConfluentPlatform.java:344-370].
2. **Binary skew between the connector and the Connect worker (fork-specific, and a production concern).** The fork compiles against `connect-api` 4.4.0 (`kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` [VERIFIED: libs.versions.toml:3]) while the datagen worker image is CP 7.6.0, which is Apache Kafka 3.6 [ASSUMED], and the archive must not bundle the Kafka API (`verifyConfluentArchive`). A missing method or a class-file version above the worker JVM would stop the connector from starting. If this is the cause, the fix is a version-catalog choice, not a `src/main` edit, so D-09's stop-and-ask still applies.
3. **Runner capacity** (Pitfall 5).
4. **A `latest` ClickHouse change.** Upstream's `latest` jobs were green on 2026-06-21 on the same SHA that failed `head` the next day, so some server drift is plausible, but the `latest` jobs only turned red with the Protobuf commit.
The measurement settles this by naming the failing test and the container-side error. If it needs the log of run 38881153720 itself, the user can create the short-lived read-only token described in memory (Phase 17); this is optional and the local run should reproduce the failure first.

### Pitfall 8: Skip-list entries for parameterized tests
**What goes wrong:** Covered in Pattern 4: a method-level entry silently drops 25 or 22 fixtures, which is a weakening unless the user accepts it.
**How to avoid:** Open Question 3.

### Pitfall 9: Windows-style line endings or editors on the upstream file
**What goes wrong:** `.gitattributes` has only `*.bat text eol=crlf`; there is no `text=auto`, so CI on Linux reads exact bytes, but a Windows checkout with `core.autocrlf=true` would rewrite the file and turn the byte guard red. [VERIFIED: .gitattributes]
**How to avoid:** CI is Linux; accept it, or add `.github/workflows/integrationTests.yaml -text` to `.gitattributes` as optional hardening (a one-line addition, outside the file itself).

### Pitfall 10: The byte guard will fire on a legitimate upstream sync
**What goes wrong:** A future sync that takes a changed upstream `integrationTests.yaml` turns the guard red by design.
**How to avoid:** The failure message prints the actual SHA-256 and length and the update procedure: verify the new file against upstream, then replace the constants in the same commit. Record the git blob id beside the SHA-256 so `git rev-parse upstream/main:.github/workflows/integrationTests.yaml` can confirm it.

### Pitfall 11: Failing-test names in workflow commands
**What goes wrong:** Test display names can contain `%`, CR or LF; a raw value in `::error::` breaks the annotation or injects a command.
**How to avoid:** Escape `%`, `\r`, `\n` as `%25`, `%0D`, `%0A` when emitting the annotation; the summary file takes Markdown, so escape backticks and pipes there. Cap the annotation text (the summary holds the full list).

## Code Examples

### 1. Nightly workflow skeleton (pins copied from `tests.yaml`)
```yaml
name: Nightly Integration Tests (ClickHouse latest, client V2)

permissions:
  contents: read

on:
  schedule:
    - cron: '17 3 * * *'   # odd minute: the hour boundary is the busiest time; runs still arrive late (V10)
  workflow_dispatch:

concurrency:
  group: nightly-integration-tests
  cancel-in-progress: false

jobs:
  integration:
    if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'
    runs-on: ubuntu-24.04
    timeout-minutes: <step timeout + 10, from the measurement>
    name: Integration tests, ClickHouse latest, client V2
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

      - name: Run integration tests
        timeout-minutes: <from the cold local wall time, 1.5-2x>
        env:
          CLICKHOUSE_VERSION: latest
          CLIENT_VERSION: V2
        run: ./gradlew integrationTest

      # failing-test summary (if: always()), artifact uploads (if: always()), retention-days: 14
```
Source for the pins and layout: `.github/workflows/tests.yaml:24-25, 37-52, 82-96` [VERIFIED]. The run command is bare `./gradlew integrationTest`: the task depends on `prepareConfluentArchive` [VERIFIED: build.gradle.kts:194] and ignores up-to-date state [VERIFIED: build.gradle.kts:195].

### 2. Byte guard skeleton (NIT-02)
```java
// Extends nothing, so it stays in testUnit. Bytes, not lines: readAllLines would hide a changed
// line ending or a missing final newline.
private static final Path UPSTREAM_WORKFLOW = Path.of(".github", "workflows", "integrationTests.yaml");
private static final String EXPECTED_SHA256 =
        "07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109";
private static final int EXPECTED_LENGTH = 4812;
// git blob 3e9b9be050fa05805227b2eee0d1fe8c72df56c0 at v1.7.0, a6f1df83 and upstream/main

static Optional<String> byteIdentityProblem(byte[] actual) throws NoSuchAlgorithmException {
    String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(actual));
    if (actual.length == EXPECTED_LENGTH && EXPECTED_SHA256.equals(sha)) {
        return Optional.empty();
    }
    return Optional.of("integrationTests.yaml must stay byte-identical to upstream: expected "
            + EXPECTED_LENGTH + " bytes / " + EXPECTED_SHA256 + ", found " + actual.length + " / " + sha);
}
```
The values are the verbatim measurements in V6. Fixtures (each must reject): one byte flipped, one byte appended, a trailing newline removed, LF replaced by CRLF, empty array. Then the real file must accept. For "seen red on a one-byte edit" at file level, edit the real file by one byte, run the guard, record the red output, restore with `git checkout -- <file>`, and record the green run. The Actions API check is separate and network-based, so it belongs in the record, not in `testUnit`:
`curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml` shows `"state": "disabled_manually"` today [VERIFIED: V5]; `.../runs?created=%3E2026-10-16T10%3A00%3A00Z` returns `total_count` 0.

### 3. Skip-list reader in the build (shape only; verify the Kotlin DSL when writing it)
```kotlin
// gradle/integration-test-skip-list.tsv: class#method <TAB> category <TAB> reason <TAB> re-open trigger
tasks.named<Test>("integrationTest") {
    extensions.configure<JacocoTaskExtension> { isEnabled = false }   // existing line, keep
    inputs.file(skipListFile)                                           // editing the list must re-run the task
    val entries = readSkipList(skipListFile)                            // fails the build on a malformed line
    if (entries.isNotEmpty()) {
        filter {
            entries.forEach { excludeTest(it.className, it.methodName) }
        }
    }
}
```
`failOnNoMatchingTests` defaults to true [CITED: TestFilter Javadoc]; an exclude-only filter still leaves tests, so it should not trip, but the executor must prove it with an entry fixture. The Docker-free `IntegrationSkipListGuardUnitTest` reads the same file.

### 4. Failing-test summary step (D-10), shape only
```yaml
      - name: Summarise failed tests
        if: always()
        run: python3 .github/scripts/failed_tests_summary.py build/test-results/integrationTest
```
The script uses `xml.etree.ElementTree` over `TEST-*.xml`, lists `classname#name` for each `<testcase>` holding a `<failure>` or `<error>`, appends them to `$GITHUB_STEP_SUMMARY`, and emits one escaped `::error title=Integration tests failed::...` line. It prints a clear line and exits 0 when there is nothing to report, as `jacoco_summary.py` does for missing reports. A Docker-free test with fixture XML directories proves it (one failure, one error, one skip, none, escaped characters); it can run the script through `ProcessBuilder` with `python3 -I`, or the same logic can be a small Gradle task if the planner prefers no Python.

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| Upstream's nightly: 43-job matrix, V1 and V2, cloud job, `@v3` actions, `ubuntu-latest`, `adopt` JDK | One job on `ubuntu-24.04`, SHA pins, `temurin` 17, read-only token, no cloud | A single signal the fork owns; `ubuntu-latest` is about to move to Ubuntu 26 (run annotation, 2026-10-19), another reason the nightly pins `ubuntu-24.04` [VERIFIED: annotation text in upstream run 40272864693 job 121409273779] |
| Upstream's setup action `gradle/gradle-build-action@v2` | `gradle/actions/setup-gradle` (already the fork's choice) | The old action's cache saves failed in the 2026-10-16 fork run with "Our services aren't available right now"; transient, but the old action is deprecated [VERIFIED: annotations of job 116907928136] |

**Deprecated/outdated:** `actions/checkout@v3`, `actions/setup-java@v3` (annotated "deprecated and will no longer receive updates") and `gradle/gradle-build-action@v2` in upstream's file. They stay, because the file stays byte-identical and disabled.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A cron tick with every job skipped by `if` still creates a visible run with conclusion `skipped` | Pitfall 1 | If no run is created the record is simpler; if it is created and the user reads "no scheduled run" strictly, criterion wording needs the clarification in Open Question 1 |
| A2 | On a job-level timeout the cancelled job may not run later `if: always()` steps | Pitfall 4, Anti-Patterns | If they do run, the step-level timeout is just belt and braces; no harm |
| A3 | `excludeTest(class, method)` removes every invocation of a parameterized method, and Gradle's XML names for invocations are not stable | Pattern 4 | If invocation-level exclusion works with a stable name, Open Question 3 disappears |
| A4 | How Gradle reports a class aborted in `@BeforeAll` (skipped entry or none) is unknown | Pattern 1 step 3, Pitfall 3 | The record must read the XML, not infer; wording in criterion 1 ("ran or skipped") depends on it |
| A5 | CP 7.6.0 corresponds to Apache Kafka 3.6, and the worker's JVM level is unchecked | Pitfall 7 hypothesis 2 | If wrong, hypothesis 2 is dropped; the measurement reads the jar and `java -version` directly |
| A6 | Memory's recorded death site (`createConnectorAndWaitUntilRunning`) is accurate for run 38881153720 | Summary, Pitfall 7 | The measurement overrides it with the local failure |
| A7 | Docker Hub `full_size` approximates the pull size; about 10 GB is an upper bound | Standard Stack, Pitfall 5 | Over- or under-states disk need; the `df -h` probe shows the truth |
| A8 | Testcontainers does not re-pull an image already present locally, so a stale local `latest` would be used | Pattern 1 step 1 | If it re-pulls, the `docker pull` step is redundant but harmless |
| A9 | A disabled workflow cannot be dispatched until re-enabled | Alternatives Considered | If it can be dispatched, disabling remains a worse gate (it also loses the schedule's gate), no plan change |
| A10 | The Actions UI lists two workflows with the same `name:` separately; the nightly's different `name:` avoids confusion | Recommended Project Structure | Cosmetic |
| A11 | Colima `vz` forwards container ports to the address Testcontainers uses once `TESTCONTAINERS_HOST_OVERRIDE` is unset | Pattern 1 step 2, Pitfall 6 | If not, the measurement needs the override set to the VM address (`192.168.64.4`); record which form worked |

## Open Questions

1. **What does "no scheduled run precedes it" mean with a gated cron?**
   - What we know: the gate skips the job, but the cron tick still happens after the Phase 32 merge (V3), and a skipped run probably still shows (A1).
   - What's unclear: whether the user reads criteria 2 and 4 as "no run at all" or "no run that executes".
   - Recommendation: write "no scheduled run with a conclusion other than `skipped`" into the record and the Phase 32 criteria, and have the user confirm it together with the NIT-03 move. The alternative (no cron until a later edit) breaks D-01.

2. **Where does the NIT-03 dispatch sit in Phase 32's ordering, and what if it is red?**
   - What we know: V1 forces it after the `--no-ff` merge to `custom_build`; Phase 32 requires one green `tests.yaml` run on the published SHA before the tags, with no `.github/` edit.
   - What's unclear: a fix after a red dispatch would be a new commit on the release tip (skip-list or build change), and so a new SHA to re-gate.
   - Recommendation: the Phase 31 record carries the CI-only risk checklist (Pitfall 2); the user decides whether Phase 32 dispatches before or after the tags. This phase only needs to record the question and update the roadmap text.

3. **Granularity for a failing parameterized test.**
   - What we know: D-08 says `class#method`, names only; a failing `avroSchemaTest` or `protoSchemaTest` fixture cannot be named without dropping all invocations, and moving a fixture to the `incompatible` directory would delete a test case (forbidden).
   - What's unclear: whether the user accepts a whole-method entry (with the fixture named in the reason), or wants a fix instead.
   - Recommendation: decide only if the measurement hits it; if it does, stop for a user decision (D-09 style).

4. **Do FORK-DELTA rows land in Phase 31 or Phase 32?**
   - What we know: CONTEXT says "new workflow/guard rows land with the change"; ROADMAP Phase 32 criterion 1 says FORK-DELTA has rows for the nightly and the CIG-01 and NIT-02 guards.
   - Recommendation: `.planning/scripts/fork-anchors.txt` rows (the anchor inventory) land in Phase 31 in the same commit as each edit; FORK-DELTA prose waits for Phase 32 to avoid editing the same rows twice. Confirm with the user.

5. **Is the optional log read worth a token?** The local run should reproduce the red; if it does not (CI-only cause), the user's read-only token route is the next step.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | local measurement | yes | colima `vz` Running, aarch64, 8 CPU, 16 GiB, 256 GiB disk; Docker CLI 29.8.2 at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`; `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` | none needed |
| Gradle wrapper / JDK 17 toolchain | all builds | yes | wrapper 9.8.1; `java` on PATH is Homebrew OpenJDK 21.0.12 (tests use the Gradle 17 toolchain; memory records Semeru 17.0.9 OpenJ9) | `-PjavaVersion=21` is an existing build property |
| Docker Hub, ghcr.io reachability | pulls | yes (Docker Hub API answered) | n/a | none |
| Local images | measurement | partly | only `clickhouse/clickhouse-server:latest` (11 days old), `ghcr.io/shopify/toxiproxy:2.7.0`, `testcontainers/ryuk:0.14.0`; Confluent 7.9.6 and the datagen image absent | the first run pulls them (several GB) |
| `python3` | optional summary script and its test | yes | 3.14.8 | write the summary as a Gradle task |
| `gh` | not required | present at `/opt/homebrew/bin/gh`, authentication not checked | n/a | the public REST API answers unauthenticated for this public repository (60 requests per hour) |
| `yq` | not required | no | n/a | not needed |
| GitHub repository variable `NIGHTLY_ENABLED` | the gate | not set (reading variables needs auth; absence is the intended state) | n/a | unset means off |

**Missing dependencies with no fallback:** none. **Missing with fallback:** the Confluent images (pulled on first run).

## Security Domain

`security_enforcement` is enabled (ASVS level 1, block on high). The surface is a CI workflow and two guards; no application code or runtime data path changes.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication / V3 Session | no | the workflow holds no credentials; no cloud secret or variable is referenced |
| V4 Access Control | yes | top-level `permissions: contents: read`; `persist-credentials: false`; guard pins both |
| V5 Input Validation | yes | no `workflow_dispatch` inputs, so no expression injection from inputs; escape test names in workflow commands (Pitfall 11) |
| V6 Cryptography | yes (integrity) | SHA-256 over bytes via `java.security.MessageDigest`; no custom crypto |
| V14 Configuration / supply chain | yes | every action pinned to a 40-hex SHA with a version comment (Phase 30 guard); runner pinned; Dependabot keeps pins current; third-party images are a known, accepted exposure (D-06) |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mutable action tag swapped upstream | Tampering | full-SHA pins with version comment, enforced by `WorkflowActionPinGuardUnitTest` |
| Over-broad `GITHUB_TOKEN` | Elevation of privilege | read-only top-level permissions, guard rejects any write scope |
| Persisted checkout token read by later steps | Information disclosure | `persist-credentials: false`, guard rejects its absence |
| Workflow-command injection through test names | Tampering | escape `%`, CR, LF; cap annotation text |
| Floating third-party image (`clickhouse-server:latest`) | Tampering / supply chain | accepted by D-06, recorded with digests in the measurement; digest pinning is deferred |
| Silent edit of the disabled upstream workflow (re-armed cron, cloud job) | Tampering | the byte guard and the recorded `disabled_manually` state |
| Secrets in artifacts | Information disclosure | the nightly has no secrets; the uploaded XML and HTML contain test output only |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `31-CONTEXT.md`, `ROADMAP.md` (Phase 31, 32), `REQUIREMENTS.md`, `.github/workflows/tests.yaml`, `integrationTests.yaml`, `.github/dependabot.yml`, `build.gradle.kts` (lines 160-300, 360-470), `gradle/libs.versions.toml`, `ConfluentPlatform.java`, `ClickHouseSinkConnectorIntegrationTest.java`, `ExactlyOnceTest.java`, `ClickHouseCloudTest.java`, `ClickHouseTestHelpers.java`, `ClickHouseSinkConfig.java:329-332`, `TestsWorkflowPinGuardUnitTest.java`, `WorkflowActionPinGuardUnitTest.java`
- GitHub REST API, unauthenticated, run this session: `ne1r0n/clickhouse-kafka-connect` workflows, runs, jobs, annotations, repository metadata; `ClickHouse/clickhouse-kafka-connect` workflow 78218174 runs and jobs
- `git rev-parse` / `shasum` / `wc` of `integrationTests.yaml` at the worktree, `HEAD`, `v1.7.0`, `a6f1df83`, `upstream/main`

### Secondary (MEDIUM confidence, official docs fetched this session)
- https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows (`workflow_dispatch`, `schedule`)
- https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/manually-running-a-workflow
- https://docs.github.com/en/actions/reference/workflows-and-actions/contexts (`vars` in `jobs.<job_id>.if`)
- https://docs.github.com/en/rest/actions/workflows (state values, disable, dispatch endpoints)
- https://docs.github.com/en/actions/reference/runners/github-hosted-runners and https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md
- https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/TestFilter.html and https://docs.gradle.org/current/userguide/java_testing.html
- Docker Hub tag API for each image (architectures, sizes)

### Tertiary (LOW confidence, marked `[ASSUMED]` above)
- Memory notes `nightly-integration-tests-inherited-red`, `colima-vz-profile`, `gsd-phase17-ci-log-access`, `openj9-mockito-test-jvm-crash` (point-in-time, 7 to 17 days old)
- A WebSearch for upstream issues about the red nightly found nothing

## Metadata

**Confidence breakdown:**
- Dispatch behaviour and gate: HIGH on V1 to V4 (docs quoted); MEDIUM on the skipped-run wrinkle (A1).
- Standard stack and workflow shape: HIGH (copied and read from `tests.yaml` and the build).
- Byte guard: HIGH (values measured and the shape follows an existing guard).
- Skip list in the build: MEDIUM (Gradle API documented; the Kotlin reader and parameterized behaviour must be proven by fixtures).
- Cause of the upstream red: LOW (logs not readable; hypotheses only).
- Runtime on the runner: MEDIUM (about 20 minutes healthy, from upstream's June run; local arm64 differs).

**Research date:** 2026-10-26
**Valid until:** 2026-11-02 for the upstream-run observations and the `latest` image facts (fast-moving); 30 days for the GitHub docs and the repository-structure findings.

Validation Architecture: omitted, because `workflow.nyquist_validation` is `false` in `.planning/config.json`. Runtime State Inventory: omitted, not a rename or migration phase.
