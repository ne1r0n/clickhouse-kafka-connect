---
phase: 01-test-infrastructure-green-baseline
verified: 2026-09-16T02:00:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".planning/REQUIREMENTS.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-01-PLAN.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-01-SUMMARY.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-02-PLAN.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-02-SUMMARY.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-03-PLAN.md"
  - ".planning/phases/01-test-infrastructure-green-baseline/01-03-SUMMARY.md"
  - "build.gradle.kts"
  - "lombok.config"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java"
covered_digest: "v1:sha256:a3ff15b54e11e1dadbae7b156c59c72b60f213a8bbe89d77be99430ad98180b2"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 1: Test Infrastructure & Green Baseline Verification Report

**Phase Goal:** The existing suite passes against ClickHouse `latest` with client V2 on a harness that is fast (one container per JVM fork), can run unit tests without Docker, cannot leak MBeans between tests, decodes responses as UTF-8, and already produces a JaCoCo report so every later test shows up in coverage
**Verified:** 2026-09-16T02:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Roadmap Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | With colima running and no `CLICKHOUSE_VERSION` override, `./gradlew test` runs against ClickHouse `latest` with client V2 and passes in full; the baseline result (incl. pre-existing failures and fixes) is recorded | ✓ VERIFIED | `build/phase1-03-full-test.log`: `Test result: SUCCESS`, `Tests summary: 194 tests, 187 succeeded, 0 failed, 7 skipped`. `01-01-SUMMARY.md` records the raw untouched baseline (187 tests, 0 failed, 7 skipped) and documents the one deviation found (a `TESTCONTAINERS_HOST_OVERRIDE=null` sandbox artifact, not a code defect — no D-01/D-02/D-04 fixes were needed because the suite was genuinely green) |
| 2 | A smoke test fails if the running ClickHouse server version does not match the requested image; a full ClickHouse-backed run starts one container per test JVM fork, not one per test class | ✓ VERIFIED | `ClickHouseServerVersionSmokeTest` re-run live in this verification: `Tests summary: 2 tests, 2 succeeded, 0 failed`, log line `ClickHouse server version: 26.8.5.13 (requested tag: latest)`, asserting `db.getDockerImageName()` equals the requested image and `versionMatchesRequestedTag()`. Container-per-fork: `ClickHouseBase.setup()` guards container creation with `if (db == null)` (verified in source) and `ClickHouseTestHarnessTest#sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage` (re-run live: PASSED) asserts `sharedContainerStarts() == 1` from inside the running JVM fork — a self-verifying executable assertion, not just log presence |
| 3 | With Docker stopped, `./gradlew testUnit` passes and runs every test not tagged as ClickHouse-backed, including the existing SMT tests | ✓ VERIFIED | Re-ran live in this verification with `DOCKER_HOST` pointed at a nonexistent socket (confirmed `docker info` fails first, exit 1): `./gradlew testUnit --rerun` → `Tests summary: 69 tests, 67 succeeded, 0 failed, 2 skipped`, no `build/jacoco/testUnit.exec` produced. Python cross-check: 16 `@Tag`-inheriting classes discovered, 0 of them ran, all 11 required untagged unit classes (including all 5 `transforms.*` SMT tests) ran |
| 4 | Sink-task tests stop their task in `finally`/`@AfterEach` and use unique connector names, so a deliberately failing test does not cause an MBean-already-registered failure later; test helpers decode ClickHouse responses as explicit UTF-8 | ✓ VERIFIED | Re-ran `ClickHouseTestHarnessTest` live: all 5 tests PASSED, including `testFailingBetweenStartAndStopLeaksAnMBean` (throws mid-test) followed by `leakedMBeanWasSweptAndUniqueNamesAreSafe` (asserts 0 leaked MBeans remain) — the `@AfterEach cleanupLeakedMBeans()` sweep logged `Unregistering leaked MBean left by a test: ...connector=harness-mbean-leak-probe...` exactly once, proving the safety net actually fires. `uniqueConnectorName(TestInfo)` verified present and exercised. `helpersDecodeResponsesAsUtf8()` PASSED, and `StandardCharsets.UTF_8` is present at both `InputStreamReader` call sites in `ClickHouseTestHelpers.java` (grep count = 2) |
| 5 | After `./gradlew test`, JaCoCo HTML and XML reports exist, Lombok-generated methods of `Column` are excluded, fork-changed classes can be reviewed on their own; `integrationTest` and `check` do not run JaCoCo | ✓ VERIFIED | Re-ran `ColumnTest` live with JaCoCo attached: `build/jacoco/test.exec`, `build/reports/jacoco/test/{jacocoTestReport.xml,html/index.html}`, `build/reports/jacoco/fork/{jacocoForkReport.xml,html/index.html}` all produced. XML parse of `jacocoTestReport.xml` shows `Column` with `extractColumn` present and `getName`/`getType`/`builder` absent. `javap` on `Column.class` shows exactly 1 `@lombok.Generated` annotation reference. `jacocoForkReport.xml` class set equals exactly the 15 D-13 classes (verified by exact-match Python assertion). `./gradlew check --dry-run` shows `:test SKIPPED` / `:integrationTest SKIPPED`, no `:testUnit` line, no coverage-verification task. `tasks.named<Test>("integrationTest") { extensions.configure<JacocoTaskExtension> { isEnabled = false } }` present in `build.gradle.kts` |

**Score:** 5/5 truths verified (0 present-but-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/test/java/.../ClickHouseServerVersionSmokeTest.java` | Server-version smoke test | ✓ VERIFIED | Exists, contains `versionMatchesRequestedTag`, `chc.version()`, `assumeFalse(isCloud`; live run passes on `latest` |
| `src/test/java/.../helper/ClickHouseTestHelpers.java` | `CLICKHOUSE_VERSION_DEFAULT = "latest"`, UTF-8 decoding | ✓ VERIFIED | Line 42: `"latest"`; line 43: `CLICKHOUSE_PROXY_VERSION_DEFAULT = "23.8"` unchanged; `StandardCharsets.UTF_8` at 2 sites (lines 160, 255) |
| `src/test/java/.../ClickHouseBase.java` | Container singleton, per-class DB, `@Tag`, MBean sweep, `uniqueConnectorName` | ✓ VERIFIED | `@Tag("clickhouse")` (only file in repo with `@Tag`), `if (db == null)`, `PullPolicy.alwaysPull()`, `newTestDatabaseName()`, `sharedContainerStarts()`, `@AfterEach cleanupLeakedMBeans()`, `uniqueConnectorName(TestInfo)` all present and exercised |
| `ClickHouseSinkTaskWithSchemaProxyTest.java` / `ClickHouseSinkTaskSchemalessProxyTest.java` | `proxyDb` isolation | ✓ VERIFIED | Both have `private static ClickHouseContainer proxyDb`, own `@AfterAll tearDown()` stopping `proxyDb`/`toxiproxy`, `database = newTestDatabaseName()`; `grep -nE '^\s+db = '` on both returns nothing (never touch the shared field) |
| `ClickHouseTestHarnessTest.java` | Executable proof of singleton/DB/MBean/UTF-8 | ✓ VERIFIED | 5 ordered tests, all PASSED on live re-run: `sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage`, `classGetsFreshIsolatedDatabase`, `testFailingBetweenStartAndStopLeaksAnMBean`, `leakedMBeanWasSweptAndUniqueNamesAreSafe`, `helpersDecodeResponsesAsUtf8` |
| `build.gradle.kts` | JaCoCo wiring, `jacocoForkReport`, `testUnit`, check-loop guard | ✓ VERIFIED | `toolVersion = "0.8.15"` (1), `register<JacocoReport>("jacocoForkReport")` (1), `finalizedBy(...)` on `test` only, `JacocoTaskExtension` disabled on `integrationTest` and `testUnit`, `if (name != "testUnit")` guard (1), `register<Test>("testUnit")` with `excludeTags("clickhouse")` |
| `lombok.config` | Lombok-generated stamping for JaCoCo filtering | ✓ VERIFIED | Contains exactly `config.stopBubbling = true` and `lombok.addLombokGeneratedAnnotation = true` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `ClickHouseServerVersionSmokeTest` | `ClickHouseHelperClient.version()` | `chc.version()` runs `SELECT VERSION()` via client V2 | ✓ WIRED | Live run logged `ClickHouse server version: 26.8.5.13 (requested tag: latest)` |
| `ClickHouseBase.setup()` | `ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE` | container built only when `db == null` | ✓ WIRED | Source confirms guard; harness test asserts `sharedContainerStarts()==1` and image equality live |
| `ClickHouseBase` `@AfterEach` sweep | `ProxySinkTask` MBean name format | `com.clickhouse:type=ClickHouseKafkaConnector,*` query | ✓ WIRED | Live harness run: leak logged and swept exactly once for the deliberately-leaked connector |
| Proxy classes | `proxyDb` container | `getTestProperties()` reads credentials from `proxyDb` | ✓ WIRED | Grep confirms `proxyDb.getUsername()`/`getPassword()` in both files, no assignment to shared `db` |
| `build.gradle.kts` `test` task | `jacocoTestReport` / `jacocoForkReport` | `finalizedBy(...)` | ✓ WIRED | Live `ColumnTest` run produced both reports from one `test` invocation |
| `lombok.config` | `Column.java` | Lombok stamps `@lombok.Generated`, JaCoCo filters it | ✓ WIRED | `javap` shows 1 `@lombok.Generated` reference; report excludes `getName`/`getType`/`builder` |
| `testUnit` task | `ClickHouseBase` `@Tag` | `excludeTags("clickhouse")` | ✓ WIRED | Live run with Docker unreachable: 0 tagged classes ran, all untagged unit classes ran |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Smoke test passes on `latest`, logs version | `./gradlew test --tests ClickHouseServerVersionSmokeTest` | 2 tests, 0 failed; logged `26.8.5.13 (requested tag: latest)` | ✓ PASS |
| Harness proves singleton/DB/MBean-sweep/UTF-8 | `./gradlew test --tests ClickHouseTestHarnessTest` | 5 tests, 0 failed; leak-and-sweep log line present exactly once | ✓ PASS |
| `testUnit` passes with Docker unreachable, excludes tagged classes | `DOCKER_HOST=unix:///nonexistent/docker.sock ./gradlew testUnit --rerun` | 69 tests, 0 failed, 2 skipped; 0 tagged classes ran; no `testUnit.exec` | ✓ PASS |
| `check --dry-run` excludes `testUnit` and coverage verification | `./gradlew check --dry-run` | `:test SKIPPED`, `:integrationTest SKIPPED`, no `:testUnit`, no `jacocoTestCoverageVerification` | ✓ PASS |
| JaCoCo reports + Lombok exclusion | `./gradlew test --tests ColumnTest` + XML/`javap` parse | Both reports produced; `Column` excludes Lombok methods; fork report == exactly 15 D-13 classes | ✓ PASS |
| No leftover Testcontainers containers after builds | `docker ps -q --filter label=org.testcontainers=true` (after 20s wait) | `0` | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| INFRA-01 | 01-01 | Baseline run against `latest`×V2 recorded, pre-existing failures fixed | ✓ SATISFIED | `01-01-SUMMARY.md` Baseline section; `build/phase1-01-full-test.log` |
| INFRA-02 | 01-01 | `latest` default image + version smoke test | ✓ SATISFIED | `CLICKHOUSE_VERSION_DEFAULT="latest"`; smoke test live-verified |
| INFRA-03 | 01-02 | Single container per test JVM fork | ✓ SATISFIED | `if (db == null)` guard; `sharedContainerStarts()==1` assertion live-verified |
| INFRA-04 | 01-02, 01-03 | `@Tag("clickhouse")` + Docker-free `testUnit` | ✓ SATISFIED | Only `ClickHouseBase.java` has `@Tag`; `testUnit` live-verified with Docker unreachable |
| INFRA-05 | 01-02 | MBean leak safety net + unique connector names | ✓ SATISFIED | Live harness run: leak logged and swept; `uniqueConnectorName` present |
| INFRA-06 | 01-02 | UTF-8 decoding in test helpers | ✓ SATISFIED | `StandardCharsets.UTF_8` x2; `helpersDecodeResponsesAsUtf8()` PASSED live, RED evidence documented in `01-02-SUMMARY.md` |
| COV-01 | 01-03 | JaCoCo on `test` only, HTML+XML reports | ✓ SATISFIED | Live `ColumnTest` run produced both reports; `integrationTest`/`testUnit` agent disabled |
| COV-02 | 01-03 | Lombok exclusion + fork-changed-class report | ✓ SATISFIED | `javap`/XML checks live-verified; fork report class set == exactly 15 D-13 classes |

No orphaned requirements: all 8 IDs mapped to Phase 1 in `REQUIREMENTS.md` are claimed by exactly one of the three plans' `requirements` frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `build.gradle.kts` | 105 | `// TODO: need to remove ???` | ℹ️ Info | Pre-existing from 2022 (`git blame`: commit `601c34cb`), not introduced or touched by this phase's diff — not a blocker |

No `TBD`/`FIXME`/`XXX`/`HACK`/`PLACEHOLDER` markers found in any of the 8 files this phase created or modified.

### Human Verification Required

None. Every truth was confirmed either by direct source inspection plus a live, targeted test re-run in this verification session (not merely trusting SUMMARY claims), including the behavior-dependent MBean-leak/cleanup invariant (Order 3→4 of `ClickHouseTestHarnessTest`, re-run live and observed to log-and-sweep the leaked MBean exactly once).

### Gaps Summary

None. All 5 roadmap success criteria and all 8 requirement IDs are verified against the actual codebase, not just against SUMMARY.md narrative. Live re-runs in this verification session (not the executors' cached logs) confirm: the smoke test, the full 5-test harness (including the deliberate-failure MBean-leak proof), `testUnit` under an actually-unreachable Docker daemon, `check --dry-run`'s task graph, and the JaCoCo/Lombok exclusion on a live `ColumnTest` run. No leftover Testcontainers containers remained after any of these runs.

---

*Verified: 2026-09-16T02:00:00Z*
*Verifier: Claude (gsd-verifier)*
