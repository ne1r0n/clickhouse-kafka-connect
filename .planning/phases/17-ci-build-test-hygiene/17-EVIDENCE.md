# Phase 17 Evidence

- Phase: 17 (CI, Build & Test Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 17 plans. Nothing here was pushed.
- Entry format: one `### E-NN: <title>` section per entry with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured (<reason>)`. Long Gradle output stays in `/tmp/gsd17/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | CI-14 | pass |
| E-02 | CI-11 | pass |
| E-03 | CI-11, CI-12, CI-13, CI-15, D-06 | pass |
| E-04 | CI-16, D-02 | pass |
| E-05 | DEBT-07 | pass |
| E-06 | DEBT-06 | pass |
| E-07 | BLD-14 | pass |
| E-08 | DEBT-08 | pass |
| E-09 | CI-17 (D-10 local gate) | pass |

### E-01: CI-14 tracer, reportTestForks read by the fork-count step

Commands as run (repo root, zsh, macOS; `nproc` supplied by a shim directory first on `PATH` because macOS has none):

1. Extract the `Report test fork count` step's `run` text from `tests.yaml` with Ruby into `/tmp/gsd17/exec/tracer/step.sh`.
2. `env -u TESTCONTAINERS_HOST_OVERRIDE PATH="/tmp/gsd17/exec/bin:$PATH" GITHUB_STEP_SUMMARY=/tmp/gsd17/exec/tracer/summary.md bash -e /tmp/gsd17/exec/tracer/step.sh`
3. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain reportTestForks | tail -n 1`

Step output and Gradle result:

```text
runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
::notice title=Test fork count::runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
gradle '6'
```

Step summary file content:

```text
### Test fork count
runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
```

Exactly one `::notice title=Test fork count::` line; its `maxParallelForks` equals Gradle's `6`. Result: `TRACER-FORK-COUNT-OK`.

Edges:

```text
$ CLICKHOUSE_CLUSTER_NAME=x ./gradlew -q --console=plain reportTestForks | tail -n 1
1
$ (cd fakegw && bash -e step.sh)   # fake gradlew exits 1 with no output; step exit status non-zero
::error::reportTestForks did not print an integer: ''
formula-left [] anchor-lines [1, 2, 3, 1, 1] timeout-anchor 1
```

`anchor-lines` order: `register<Test>("testUnit")` 1, `windows-1252` 2, `jacocoForkReport` 3, `withCapability("org.lz4:lz4-java")` 1, `tasks.register("reportTestForks")` 1. `fork-anchors.txt` and `src/main` are unchanged. Result: `TRACER-EDGES-OK`.

Timestamp: 2026-10-11T20:15Z (approximate; run between plan start 20:14:40Z and the E-01 commit)

Verdict: pass

### E-02: CI-11 action SHA resolution

Command as run, per action: `git ls-remote https://github.com/<repo> refs/tags/<tag> 'refs/tags/<tag>^{}'`, then `runs.using` from `https://raw.githubusercontent.com/<repo>/<sha>/action.yml` (`setup-gradle/action.yml` for gradle/actions), then the newest same-major tag from `git ls-remote --tags`.

```text
== actions/checkout v7.0.1
3d3c42e5aac5ba805825da76410c181273ba90b1	refs/tags/v7.0.1
== actions/upload-artifact v7.0.2
cf430e030ddbb5b0abf93d22962f4752f3646cd9	refs/tags/v7.0.2
== actions/setup-java v6.0.1
de7274f081f381c8f8158605e0321c36c376e2e6	refs/tags/v6.0.1
== gradle/actions v6.4.0
b9bee63ef481cc545e4517ec730544a4d8b1dfa0	refs/tags/v6.4.0
3f5f9adaf7d9fecd50b5935e54106014257a94e6	refs/tags/v6.4.0^{}
```

| Action | Tag | Tag type | Tag-object SHA | Pinned commit | runs.using | Newest same-major tag |
|--------|-----|----------|----------------|---------------|------------|-----------------------|
| actions/checkout | v7.0.1 | lightweight | (same as commit) | 3d3c42e5aac5ba805825da76410c181273ba90b1 | node24 | v7.0.1 |
| actions/upload-artifact | v7.0.2 | lightweight | (same as commit) | cf430e030ddbb5b0abf93d22962f4752f3646cd9 | node24 | v7.0.2 |
| actions/setup-java | v6.0.1 | lightweight | (same as commit) | de7274f081f381c8f8158605e0321c36c376e2e6 | node24 | v6.0.1 |
| gradle/actions (setup-gradle) | v6.4.0 | annotated | b9bee63ef481cc545e4517ec730544a4d8b1dfa0 | 3f5f9adaf7d9fecd50b5935e54106014257a94e6 (peeled `^{}`) | node24 | v6.4.0 (a `v6.4.0-rc.1` pre-release also exists) |

All four resolved commits equal the planning-time values, so no impostor-commit stop was raised. No newer same-major release exists, so nothing is left for Dependabot to bump yet.

Timestamp: 2026-10-11T20:16Z

Verdict: pass

### E-03: D-06 pin guard red then green, workflow and Dependabot YAML parse

Red run (before the edits), `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'`, `5 tests completed, 5 failed`:

```text
every action in tests.yaml is pinned to a full commit SHA with a version comment FAILED
    AssertionFailedError: Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '- uses: actions/checkout@v4'
checkout does not persist the token in .git/config FAILED
    AssertionFailedError: Checkout step must set 'persist-credentials: false': '- uses: actions/checkout@v4'
dependabot.yml updates the github-actions ecosystem next to gradle FAILED
    AssertionFailedError: Missing '- package-ecosystem: "github-actions"' in .github/dependabot.yml
every job in tests.yaml runs on the pinned ubuntu-24.04 runner FAILED
    AssertionFailedError: runs-on must be the pinned runner, not 'ubuntu-latest' ==> expected: <ubuntu-24.04> but was: <ubuntu-latest>
the workflow token is read-only: one top-level permissions block, contents read FAILED
    AssertionFailedError: Exactly one 'permissions:' line is allowed, at the top level, so every job inherits it ==> expected: <1> but was: <0>
```

Green run (after the edits): test-results XML `tests,failures,errors,skipped ['5', '0', '0', '0']`; spotless IDE hook on the test file printed `IS CLEAN`.

Pin check against live tags and `action.yml`:

```text
actions/checkout v7.0.1 3d3c42e5aac5ba805825da76410c181273ba90b1 lightweight
actions/setup-java v6.0.1 de7274f081f381c8f8158605e0321c36c376e2e6 lightweight
gradle/actions/setup-gradle v6.4.0 3f5f9adaf7d9fecd50b5935e54106014257a94e6 annotated
actions/upload-artifact v7.0.2 cf430e030ddbb5b0abf93d22962f4752f3646cd9 lightweight
actions/upload-artifact v7.0.2 cf430e030ddbb5b0abf93d22962f4752f3646cd9 lightweight
uses 5 pinned 5 mismatch [] not-node24 [] wrong-major [] upload-pins [('cf430e030ddbb5b0abf93d22962f4752f3646cd9', 'v7.0.2')]
```

Ruby parse and byte checks:

```text
YAML-OK
gradle-prefix-identical True header-lines 18 timeout-anchor-lines 1
```

The parse confirms: trigger unchanged, `permissions` is exactly `contents: read` with no job-level block, runner `ubuntu-24.04`, `timeout-minutes: 45`, checkout `with: persist-credentials: false`, and Dependabot ecosystems `["gradle", "github-actions"]` with the D-03/D-04 fields. Results: `PIN-GUARD-GREEN`, `PINS-MATCH-TAGS-NODE24`, `WORKFLOW-AND-DEPENDABOT-OK`.

Timestamp: 2026-10-11T20:17Z

Verdict: pass

### E-04: CI-16 static proof and the Phase 17 CI docs rows

CI-16 can be proven statically only: the file is gone from the tree and the index, nothing under `.github` mentions it, `tests.yaml` has no reusable-workflow trigger, and the other four workflows, `.github/actions` and `.github/scripts` are byte-identical to the phase start `e0017d69`.

```text
$ test ! -e .github/workflows/test-head.yaml && test -z "$(git ls-files .github/workflows/test-head.yaml)" && test -z "$(git grep -n -e test-head -- .github)" && test -z "$(git grep -n -e workflow_call -- .github/workflows/tests.yaml)" && [workflow list is exactly the five expected files] && git diff --quiet e0017d69 HEAD -- <four workflows> .github/actions .github/scripts
CI-16-STATIC-OK
$ git show --first-parent --diff-filter=D --name-only --pretty=format: c3171c97
.github/workflows/test-head.yaml
```

Registered-workflow note (research F1): the fork's default branch is `main`, a 2025 upstream mirror on which `test-head.yaml` is registered as disabled for inactivity with no runs. GitHub runs schedules from the default branch only, so deleting the file on `custom_build` has no observable effect on any schedule until `custom_build` becomes the default branch. No repository setting was changed (D-05).

FORK-DELTA region check (only the Build/CI section and Known costs items 5 to 7 may differ from `e0017d69`):

```text
outside-identical True ci True test-head True dependabot True untouched-line True item6 True item5 True
FORK-DELTA-17-01-OK
appended 1   (MERGE-CHECKLIST-v1.7.0.md gained exactly Recorded Fact 41, with "git rm")
CHECKLIST-LINE-OK
SRC-MAIN-UNCHANGED, ANCHORS-FILE-UNCHANGED
anchor mismatches: []   (every row of fork-anchors.txt re-counted on the tip)
```

Timestamp: 2026-10-11T20:18Z

Verdict: pass

### E-05: DEBT-07 test classpath de-duplicated

Commands as run (repo root, `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`):

1. Before the exclude, for each of `testRuntimeClasspath`, `runtimeClasspath`, `integrationTestRuntimeClasspath`, `clickhouseDependencies`: `./gradlew -q dependencies --configuration <c>` into `/tmp/gsd17/exec/deps-before/<c>.txt` and `./gradlew -q dependencyInsight --dependency lz4-java --configuration <c>` into `/tmp/gsd17/exec/insight-before/<c>.txt`.
2. RED: `./gradlew testUnit --tests '*TestRuntimeClasspathLz4UnitTest'` (commit `7993bea2`, before any build change).
3. GREEN: the same command after the `configurations.named("testRuntimeClasspath")` exclude (commit `e50712a0`).
4. After the exclude, the same `dependencies` and `dependencyInsight` runs into the `-after` directories.

RED message (test failed, BUILD FAILED):

```text
LZ4Factory must be on the test classpath once, but was found at
[jar:file:/Users/alexandr.chashnikov/.gradle/caches/modules-2/files-2.1/at.yawk.lz4/lz4-java/1.12.0/4aeb33effcf8f9e29f4a2e206c224afebd2621d/lz4-java-1.12.0.jar!/net/jpountz/lz4/LZ4Factory.class,
 jar:file:/Users/alexandr.chashnikov/.gradle/caches/modules-2/files-2.1/org.lz4/lz4-java/1.8.0/4b986a99445e49ea5fbf5d149c4b63f6ed6c6780/lz4-java-1.8.0.jar!/net/jpountz/lz4/LZ4Factory.class]
==> expected: <1> but was: <2>
```

The lz4 lines of each configuration (`grep lz4 deps-<when>/<c>.txt`):

```text
testRuntimeClasspath before:  |         +--- org.lz4:lz4-java:1.8.0
                              |    +--- at.yawk.lz4:lz4-java:1.10.2 -> 1.12.0
                              +--- at.yawk.lz4:lz4-java:1.12.0
testRuntimeClasspath after:   |    +--- at.yawk.lz4:lz4-java:1.10.2 -> 1.12.0
                              +--- at.yawk.lz4:lz4-java:1.12.0
runtimeClasspath (both):      |         +--- org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0   (plus the two at.yawk.lz4 lines)
integrationTestRuntimeClasspath (both): same redirected line plus the two at.yawk.lz4 lines
clickhouseDependencies (both): the two at.yawk.lz4 lines only
```

Byte-identity of the shipped configurations (`cmp before after`): `identical runtimeClasspath`, `identical integrationTestRuntimeClasspath`, `identical clickhouseDependencies`. `diff` of `testRuntimeClasspath` is exactly one removed line: `12d11 < |         +--- org.lz4:lz4-java:1.8.0`.

`dependencyInsight --dependency lz4-java`, two-line summaries:

```text
before/testRuntimeClasspath: org.lz4:lz4-java:1.8.0 resolved plain (no selection reason), beside at.yawk.lz4:lz4-java:1.12.0
after/testRuntimeClasspath:  only at.yawk.lz4:lz4-java:1.12.0 is reported
before/runtimeClasspath and integrationTestRuntimeClasspath, unchanged after:
                             "By conflict resolution: On capability org.lz4:lz4-java ..." org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0
before and after clickhouseDependencies: only at.yawk.lz4:lz4-java is reported
```

GREEN run: `TestRuntimeClasspathLz4UnitTest` 1 test, 0 failures (BUILD SUCCESSFUL).

Timestamp: 2026-10-11T20:22Z

Verdict: pass

### E-06: DEBT-06 response leg, green and seen red

D-07 path used: primary (the read's own `system.query_log` QueryFinish row under a client-chosen query id). No fallback was needed. Server: ClickHouse `26.9.16.11` (`clickhouse/clickhouse-server:latest`), client-v2 0.9.6.

Commands as run:

1. Green: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*Lz4RoundTripLiveTest' --rerun > /tmp/gsd17/exec/debt06-green.log 2>&1` (commit `4bbe32f9`). BUILD SUCCESSFUL, 2 tests, 0 failures.
2. Red (D-09, local only, never committed): `sed -i '' 's/\.compressServerResponse(true)/.compressServerResponse(false)/'` on `Lz4RoundTripLiveTest.java`, then the same Gradle command into `/tmp/gsd17/exec/debt06-red.log`. BUILD FAILED, 2 tests, 1 failure.
3. Revert: the inverse `sed`, then `git diff --quiet -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java`.

Green run log lines:

```text
network_compression_method=lz4, server version 26.9.16.11
CompressedReadBufferBlocks for the insert: 75
Response leg: NetworkSendBytes=61864 result_bytes=4790219 ratio=0.012914649622491163 method=lz4
```

Red run (`compressServerResponse(false)`):

```text
response not compressed: NetworkSendBytes=4778633 result_bytes=4790219 ==> expected: <true> but was: <false>
Response leg: NetworkSendBytes=4778633 result_bytes=4790219 ratio=0.9975813214385396 method=lz4
```

Post-revert `git diff --quiet -- <file>`: exit 0 (REVERT-CLEAN). The bound is a tenth of `result_bytes`; green sat at 0.0129, red at 0.9976, so the margin on both sides is wide. The log line carries byte counts, a ratio and a setting name only.

Timestamp: 2026-10-11T20:22Z

Verdict: pass

### E-07: BLD-14 Lombok warning gone, javap unchanged

Commands as run (repo root, `javap` from `/opt/homebrew/bin`, the same binary for both captures):

1. Before any edit to `lombok.config`: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava > /tmp/gsd17/exec/bld14-before.log 2>&1`, then `javap -v -p -cp build/classes/java/main <class> | grep -v -E '^(Classfile|  Last modified|  SHA-256 checksum)'` for `ClickHouseFieldDescriptor` and `ClickHouseFieldDescriptor$ClickHouseFieldDescriptorBuilder` into `/tmp/gsd17/exec/javap/before-*.txt`.
2. Append `lombok.jacksonized.jacksonVersion += 2` to `lombok.config` (commit `ac349085`, one added line, none removed), repeat the compile into `bld14-after.log` and the two captures into `after-*.txt`.

```text
Ambiguous: Jackson2 warning lines   before: 1   after: 0
  before: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'. See https://projectlombok.org/features/experimental/Jacksonized
all other warning lines: identical set (the [deprecation] ClickHouseClient line at ClickHouseHelperClient.java:687 is reported once or twice per run by javac and is unrelated)
javap descriptor: 1193 lines before, 1193 after, cmp identical
javap builder:     420 lines before,  420 after, cmp identical
lombok.Generated occurrences in the builder javap: 21 before, 21 after
lombok.config: config.stopBubbling = true / lombok.addLombokGeneratedAnnotation = true / lombok.jacksonized.jacksonVersion += 2
```

`+= 3` was not tried (it does not compile here), and no Lombok or Gradle version was changed.

Timestamp: 2026-10-11T20:23Z

Verdict: pass

### E-08: DEBT-08 NanoTimestamp scientific-notation rows

Four rows added to `MetadataFreeJsonWriterTest`'s D-01 `@CsvSource` after `-1, -1E-9` (commit `6379fb9d`), each checked against `BigDecimal.valueOf(v, 9).toString()`; the `@DisplayName` was extended additively (15-REVIEW IN-06).

Command: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*MetadataFreeJsonWriterTest'` (BUILD SUCCESSFUL, class 45 tests, 0 failures, 0 errors). The seven NanoTimestamp cases, as JUnit reported them:

```text
D-01: io.debezium.time.NanoTimestamp value 0 is written as 0E-9
D-01: io.debezium.time.NanoTimestamp value 1 is written as 1E-9
D-01: io.debezium.time.NanoTimestamp value -1 is written as -1E-9
D-01: io.debezium.time.NanoTimestamp value 123 is written as 1.23E-7
D-01: io.debezium.time.NanoTimestamp value -123 is written as -1.23E-7
D-01: io.debezium.time.NanoTimestamp value 999 is written as 9.99E-7
D-01: io.debezium.time.NanoTimestamp value 1000 is written as 0.000001000
```

Timestamp: 2026-10-11T20:23Z

Verdict: pass

### E-09: D-10 local gate

Final Phase 17 code, last non-`.planning` commit `2be1db6cda6ef983a693414ac4f0a6461bd32171` (2026-10-11T23:24:02+03:00). Commands as run (repo root, branch `sync/v1.7.0`, ClickHouse `latest` x client V2 defaults):

1. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun > /tmp/gsd17/exec/gate-testunit.log 2>&1` (rc 0, BUILD SUCCESSFUL in 4s; Docker-free).
2. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun > /tmp/gsd17/exec/gate-test.log 2>&1` in the background, rc file `/tmp/gsd17/exec/gate-test.rc` = 0 (BUILD SUCCESSFUL in 2m 42s).
3. `/opt/homebrew/opt/docker/bin/docker image inspect clickhouse/clickhouse-server:latest --format '{{index .Config.Labels "com.clickhouse.build.version"}} {{.Created}}'`.

```text
testUnit  classes 37  tests 464  failures 0  errors 0  skipped 2
test      classes 71  tests 840  failures 0  errors 0  skipped 18 (7 named methods + 11 bracketed exactlyOnceStateMismatchTest cases)
stale result files: 0 in both; Phase 17 classes present in both: TestsWorkflowPinGuardUnitTest, TestRuntimeClasspathLz4UnitTest, MetadataFreeJsonWriterTest; in test also Lz4RoundTripLiveTest
server image: 26.9.16.11  created 2026-10-11T16:49:40.783693619Z
LOCAL-GATE-GREEN
```

Anchors invariant (`/tmp/gsd17/exec/anchors.log`, zero `INVARIANT` lines):

```text
invariants: provenance_paths=21 anchor_commits=212 anchor_rows=43 marker_commits=259 result=PASS
```

Spotless IDE hook on the four touched Java files: `TestsWorkflowPinGuardUnitTest`, `TestRuntimeClasspathLz4UnitTest`, `Lz4RoundTripLiveTest` and `MetadataFreeJsonWriterTest` each answered `IS CLEAN`.

Phase scope outside `.planning` (`git diff --name-only e0017d69 HEAD`), exactly ten paths, no extra and none missing: `.github/dependabot.yml`, `.github/workflows/test-head.yaml` (deleted), `.github/workflows/tests.yaml`, `build.gradle.kts`, `docs/FORK-DELTA.md`, `lombok.config`, and the four test files above. `git diff --quiet e0017d69 HEAD -- src/main .planning/scripts/fork-anchors.txt` exit 0. `ANCHORS-SPOTLESS-SCOPE-OK`.

Timestamp: 2026-10-11T20:34Z

Verdict: pass
