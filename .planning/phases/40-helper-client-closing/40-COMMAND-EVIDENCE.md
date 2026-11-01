# Phase 40: Command evidence

- **Base commit:** `phase_start_sha` = `52ce1cd4c1501f1c4a332f42c64295be43f0f53c` (HEAD when plan 40-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so every run shares one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-11-01.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `40-evidence-check.py` in this directory is a byte copy of Phase 39's `39-evidence-check.py` (get, expect, index, rows, secrets, junit, anchors, skips, failed) and is never edited.
- **Red-reason rule:** a red counts only when every failing testcase is an `org.opentest4j.AssertionFailedError` whose message contains `close count`; a compile error, an unexpected exception or any other assertion is not a recorded red.
- **No push happens in Phase 40.** Reads use `git ls-remote` and unauthenticated GitHub REST GETs only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | CLS-04 | 40-01 | pass |
| E-02 | CLS-01, CLS-04 | 40-01 | pass |
| E-03 | CLS-01, CLS-02, CLS-04 | 40-01 | pending (plan 40-01) |
| E-04 | CLS-03, CLS-04 | 40-01 | pending (plan 40-01) |
| E-05 | CLS-01, CLS-04 | 40-02 | pending (plan 40-02) |
| E-06 | CLS-01, CLS-02, CLS-04 | 40-02 | pending (plan 40-02) |
| E-07 | CLS-01, CLS-02 | 40-02 | pending (plan 40-02) |
| E-08 | CLS-03, CLS-04 | 40-03 | pending (plan 40-03) |
| E-09 | CLS-03, CLS-04 | 40-03 | pending (plan 40-03) |
| E-10 | CLS-03 | 40-03 | pending (plan 40-03) |
| E-11 | CLS-01, CLS-02, CLS-03, CLS-04 | 40-03 | pending (plan 40-03) |

### E-01: Execution-start facts, baselines and environment

Backs: CLS-04 (execution-start facts: base commit, origin and upstream re-read, the Phase 39 baselines that Phase 40's totals build on, the evidence helper copy, the environment and the Spotless baselines of the five files this phase touches).

```sh
mkdir -p /tmp/gsd40/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/861
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/855
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/857
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py get .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md E-10 testunit_tests
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py get .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md E-10 full_tests
git diff --quiet bcf4276365c6a12017be27e8a613d545ebf796eb HEAD -- src/main src/test src/testFixtures build.gradle.kts gradle
cp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/40-helper-client-closing/40-evidence-check.py
cmp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/40-helper-client-closing/40-evidence-check.py
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
colima list
ls -d /opt/homebrew/Cellar/docker/*/bin/docker
/usr/libexec/java_home -v 21
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd40/exec/sc-base-<name>.out 2> /tmp/gsd40/exec/sc-base-<name>.err
```

```text
phase_start_sha: 52ce1cd4c1501f1c4a332f42c64295be43f0f53c
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
upstream_pr_861: open merged_at=None head=3d4cfdf27181
upstream_pr_855: open merged_at=None head=ddabf28b9827
upstream_pr_857: open merged_at=None head=11d88bc0a552
baseline_source: 39-COMMAND-EVIDENCE.md E-10
baseline_testunit_tests: 1687
baseline_full_tests: 2285
baseline_inputs_unchanged: 0
anchors_mirror: rows=172 bad=0
helper_copy: cmp-equal
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
testcontainers_host_override: set
spotless_baseline_clickhouse_writer: IS DIRTY
spotless_baseline_keeper_state_provider: IS DIRTY
spotless_baseline_state_provider: IS DIRTY
spotless_baseline_proxy_sink_task: IS CLEAN
spotless_baseline_counting_helper: IS CLEAN
```

Both dirty paths sit under `.planning/` and are orchestrator-managed, so the dirty-path rule holds. `origin/custom_build` equals the expected tip and all three upstream PRs (#861, #855, #857) are still open and unmerged, as at planning. `git diff --quiet` from Phase 39's `gate_head` `bcf42763` to HEAD over `src/main`, `src/test`, `src/testFixtures`, `build.gradle.kts` and `gradle` exits 0, so the Phase 39 totals 1687 (`testUnit`) and 2285 (full suite) are this phase's baseline. The Spotless verdicts equal the research measurement: the three production files are import-only dirty and are never rewritten by the hook, the other two are clean. `testcontainers_host_override` is recorded as set only; this shell sets it to a literal that breaks Ryuk, which is why every Gradle run unsets it.

2026-11-01T21:59:36Z

Verdict: pass

### E-02: Tracer: counting seam and writer factory carry one writer case, red on V1 and V2

Backs: CLS-04 (the Mockito-free counting seam, the writer's behaviour-neutral `createHelperClient` factory and one writer case run end to end with Docker unreachable; the first red of the matrix, recorded before any fix exists) and CLS-01 (the writer's helper client is never closed on the unchanged lifecycle).

```sh
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
git diff -U0 -- src/main/java | grep '^[-+]import'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java" -PspotlessIdeHookUseStdOut
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' --tests '*WriterFailureClassificationUnitTest' --tests '*ValueFreeDlqHeadersUnitTest' --tests '*ClickHouseWriterNullMessageUnitTest' --tests '*WriterValueFreeFailureUnitTest' > /tmp/gsd40/exec/tracer.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-tracer
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-tracer --class WriterHelperClientClosingUnitTest tests=2 failures=2
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-tracer --expect 'WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce'
```

```text
seam_removed_lines: 6
seam_import_lines_changed: 0
spotless_counting_helper: IS CLEAN
spotless_test_class: IS CLEAN
spotless_clickhouse_writer_diff_import_only: yes
tracer_run_exit: 1
tracer_class_tests: 2
tracer_class_failures: 2
red_failed_set: WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce
red_reason_close_count: yes
counting_users_green: yes
compile_exit: 0
```

The run exited non-zero (`254 tests, 252 succeeded, 2 failed`) and produced the class XML, which proves the sources compiled. The two failures are the tracer case on client V1 and on client V2, both `org.opentest4j.AssertionFailedError` with a message beginning `close count of the writer's helper client`; the other 252 tests, which include the four existing `CountingHelperClient` users (`WriterFailureClassificationUnitTest`, `ValueFreeDlqHeadersUnitTest`, `ClickHouseWriterNullMessageUnitTest`, `WriterValueFreeFailureUnitTest`), pass. The `ClickHouseWriter.java` seam removes exactly six lines (the two builder starts, the two compression lines and the two `.build();` lines) and changes no import line; the hook's stdout differs from the file in the same 21 import and blank lines as at the baseline, with equal import sets.

2026-11-01T22:00:05Z

Verdict: pass
