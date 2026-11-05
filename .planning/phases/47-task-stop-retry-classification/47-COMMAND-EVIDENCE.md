# Phase 47: Command evidence

- **Base commit:** `local_head` = `427f30b4c4dffd56841f5e5eafe13a55ac4caca8` (HEAD when plan 47-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-05.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `47-evidence-check.py`, `47-testcase-status.py` and `47-doc-edits.json` are read-only plan-time files; they are never edited.
- **No push happens in Phase 47.** Reads use `git ls-remote` and `gh api` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | LIF-01, LIF-02, RTY-01, RTY-02 | 47-01 | pass |
| E-02 | LIF-01 | 47-01 | pass |
| E-03 | LIF-01 | 47-01 | pass |
| E-04 | LIF-01, LIF-02 | 47-01 | pass |
| E-05 | RTY-01 | 47-02 | pass |
| E-06 | RTY-01 | 47-02 | pass |
| E-07 | RTY-01 | 47-02 | pass |
| E-08 | RTY-02 | 47-03 | pass |
| E-09 | LIF-02 | 47-03 | pass |
| E-10 | LIF-01, LIF-02, RTY-01, RTY-02 | 47-04 | pass |
| E-11 | LIF-02, RTY-02 | 47-04 | pass |
| E-12 | LIF-02 | 47-05 | pass |
| E-13 | RTY-01 | 47-05 | pass |
| E-14 | RTY-01 | 47-05 | pass |

### E-01: Pre-flight and baseline on the untouched base

Backs: LIF-01, LIF-02, RTY-01, RTY-02, ROADMAP Phase 47 phase gate (a green baseline on the untouched tip, so every later red and every added row is attributable to this phase).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
colima list
md5 -q .planning/phases/47-task-stop-retry-classification/47-evidence-check.py
md5 -q .planning/phases/47-task-stop-retry-classification/47-testcase-status.py
md5 -q .planning/phases/47-task-stop-retry-classification/47-doc-edits.json
git ls-files --error-unmatch .planning/phases/47-task-stop-retry-classification/47-evidence-check.py .planning/phases/47-task-stop-retry-classification/47-testcase-status.py .planning/phases/47-task-stop-retry-classification/47-doc-edits.json
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt
grep -cF -e '} finally {' -e 'deliveryStrategy.stop();' -e 'this.proxySinkTask.stop();' src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java   # run once per pattern
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd47/exec/selftest.log 2>&1
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
git ls-remote origin refs/heads/sync/v1.7.0
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 > /tmp/gsd47/exec/tail-gate-base.log 2>&1
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd47/exec/testunit-base.log 2>&1
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit build/test-results/testUnit --class .WrappedDataExceptionClassificationPinUnitTest   # and the seven other pin classes
rm -rf /tmp/gsd47/exec/xml-base-testunit && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-base-testunit
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-base-testunit /tmp/gsd47/exec/status-base-testunit.tsv
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd47/exec/suite-base.log 2>&1
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py failed build/test-results/test
rm -rf /tmp/gsd47/exec/xml-base-suite && cp -R build/test-results/test /tmp/gsd47/exec/xml-base-suite
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-base-suite /tmp/gsd47/exec/status-base-suite.tsv
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: 427f30b4c4dffd56841f5e5eafe13a55ac4caca8
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
helper_md5: 7123d354f51d45e019837f43b3a30fbc
status_helper_md5: 41f14e07ae7b402ad2e29bfc381a2aff
doc_edits_md5: 3c4b1ce86e50bf8970e744a7eb6dc3a6
helpers_tracked: yes
anchors_mirror: rows=216 bad=0
lif01_finally_count: 0
lif01_strategy_stop_count: 1
lif01_proxy_stop_count: 1
selftest_last_line: selftest: no failures
selftest_duration: about 63 min
origin_pre_tag: absent
origin_sync_branch: absent
base_tail_gate_line: invariants: provenance_paths=11 anchor_commits=91 anchor_rows=216 marker_commits=109 anchor_exempt=0 result=PASS
base_tail_gate_exit: 0
baseline_testunit_docker_host: unix:///nonexistent.sock
baseline_testunit_exit: 0
baseline_testunit_tests: 2181
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
pin02_tests: 14
rsk02_tests: 38
matrix_tests: 216
proxy_stop_tests: 4
warn_tests: 21
codes_pin_tests: 19
cause_cycle_tests: 2
stall_pin_tests: 15
classification_pins_failed: 0
baseline_suite_exit: 0
baseline_suite_tests: 2784
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
baseline_java: default toolchain
jvm_crash: no
```

Pin class names, in the order of the keys: `WrappedDataExceptionClassificationPinUnitTest`, `RetriableExceptionPassThroughUnitTest`, `WriterFailureClassificationUnitTest`, `ProxySinkTaskStopUnitTest`, `UnvettedRetriableCodesWarnUnitTest`, `AdditionalRetriableErrorCodesPinUnitTest`, `HandleExceptionCauseCycleUnitTest`, `InsertStallBoundPinUnitTest`. The baseline totals later plans compute expected counts from are testUnit 2181 tests and full suite 2784 tests. The self-test and the base tail gate ran in the background while the baselines ran; they read git objects only, and `src` equalled B0 throughout. The session was interrupted by an API login expiry after the red run; nothing was committed before it, and the self-test result had already been written to disk (exit 0, last line `selftest: no failures`).

Recorded 2026-11-05T20:22:14Z.

Verdict: pass

### E-02: LIF-01 pin red on the unchanged tree, and the expected added rows written down before the fix

Backs: LIF-01, ROADMAP Phase 47 criterion 1 (the pin was seen red on the unchanged tree for the stated reason before any `src/main` edit).

```sh
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 -- src/main
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClickHouseSinkTaskStopUnitTest*' > /tmp/gsd47/exec/red-lif01.log 2>&1
rm -rf /tmp/gsd47/exec/xml-lif01-red && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-lif01-red
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-lif01-red /tmp/gsd47/exec/status-lif01-red.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit /tmp/gsd47/exec/xml-lif01-red --class .ClickHouseSinkTaskStopUnitTest tests=6 failures=2 errors=0
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py failed /tmp/gsd47/exec/xml-lif01-red --expect ClickHouseSinkTaskStopUnitTest#strategyStopFailureStillStopsTheProxyTask ClickHouseSinkTaskStopUnitTest#bothStopsRunWhenTheStrategyAndTheProxyTaskThrow
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py count /tmp/gsd47/exec/status-lif01-red.tsv .ClickHouseSinkTaskStopUnitTest PASS
shasum -a 256 src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py select .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv PASS /tmp/gsd47/exec/status-lif01-red.tsv .ClickHouseSinkTaskStopUnitTest
shasum -a 256 .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv
```

```text
red_csc_identical_to_base: yes
red_docker_host: unix:///nonexistent.sock
red_exit: 1
red_tests: 6
red_failures: 2
red_errors: 0
red_pass: 4
red_failed_names: strategyStopFailureStillStopsTheProxyTask, bothStopsRunWhenTheStrategyAndTheProxyTaskThrow
red_failure_type: org.opentest4j.AssertionFailedError
red_first_message: org.opentest4j.AssertionFailedError: the writer must have been stopped once ==> expected: <1> but was: <0>
red_test_sha256: 465dfd49b9a9a0612afcc0341040cff3a2ef62ba655f86ddb045cd0f6d7c74db
red_attempts: 1
fmt_st_before_red_record: identical
expected_added_rows: 6
expected_added_sha256: a3a2307945127282f8b6c3050888f4bfa1a384140e053439998312312dc2ef62
```

Both failures are `AssertionFailedError` on the writer stop count (the strategy threw first and the proxy stop was never reached); the other four cases (the no-strategy case and the three shipped strategies) passed. The per-file Spotless check of the pin ran before this record, so the frozen hash is the final one. `47-lif01-expected-added.tsv` holds the 6 PASS rows of the pin and is committed while `ClickHouseSinkTask.java` still equals B0 and before the fix commit.

Recorded 2026-11-05T20:22:14Z.

Verdict: pass

### E-03: LIF-01 fix commit: try/finally, pin green, scope, anchors

Backs: LIF-01, ROADMAP Phase 47 criterion 1 (`ClickHouseSinkTask.stop()` reaches the proxy stop even when the strategy's `stop()` throws; the fix is its own commit), D-1, D-3.

```sh
git diff --numstat -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClickHouseSinkTaskStopUnitTest*' > /tmp/gsd47/exec/green-lif01.log 2>&1
rm -rf /tmp/gsd47/exec/xml-lif01-green && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-lif01-green
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit /tmp/gsd47/exec/xml-lif01-green --class .ClickHouseSinkTaskStopUnitTest tests=6 failures=0 errors=0
shasum -a 256 src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ProxySinkTaskStopUnitTest*' --tests '*UnvettedRetriableCodesWarnUnitTest*' --tests '*SourcePlanningIdGuardUnitTest*' > /tmp/gsd47/exec/neighbours-lif01.log 2>&1
python3 -I -c '<stop() token-list check against B0>'
grep -cF -e '} finally {' -e 'deliveryStrategy.stop();' -e 'this.proxySinkTask.stop();' src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java   # once per pattern
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git worktree add --detach /tmp/gsd47/fmt HEAD   # per-file Spotless on ClickHouseSinkTask.java and the pin, cmp, worktree removed
git add src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd47/exec/msg-fix.txt
```

```text
fix_commit: 796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe
green_tests: 6
green_failures: 0
green_errors: 0
test_sha256_unchanged: yes
neighbours_failed: 0
stop_body_check: try/finally only
csc_numstat: 8 5
lif01_finally_count: 1
lif01_strategy_stop_count: 1
lif01_proxy_stop_count: 1
anchors_mirror: rows=218 bad=0
fmt_csc: identical
fmt_st: identical
```

The neighbour run (`ProxySinkTaskStopUnitTest`, `UnvettedRetriableCodesWarnUnitTest`, `SourcePlanningIdGuardUnitTest`) ran 43 tests with 0 failures; the first two call `stop()` on real tasks. The fix commit holds exactly `ClickHouseSinkTask.java` (8 insertions, 5 deletions, nothing outside `stop()`), the pin (byte-identical to the red run) and `fork-anchors.txt` (3 added lines: one comment and two LIF-01 rows).

Recorded 2026-11-05T20:23:36Z.

Verdict: pass

### E-04: LIF-01 gate on the fix tree, both-throw probe, FORK-DELTA item 13

Backs: LIF-01, LIF-02, ROADMAP Phase 47 criterion 1 (the fix passes its own gate with exactly the planned added rows), D-1, D-2, D-3.

```sh
git diff --quiet 796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe -- src
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd47/exec/lif01-gate-testunit.log 2>&1
rm -rf /tmp/gsd47/exec/xml-lif01-gate-testunit && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-lif01-gate-testunit
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-lif01-gate-testunit /tmp/gsd47/exec/status-lif01-gate-testunit.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py delta /tmp/gsd47/exec/status-base-testunit.tsv /tmp/gsd47/exec/status-lif01-gate-testunit.tsv .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit /tmp/gsd47/exec/xml-lif01-gate-testunit --class .WrappedDataExceptionClassificationPinUnitTest   # and the seven other pin classes
git worktree add --detach /tmp/gsd47/wt-probe 796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe
cp /tmp/gsd47/probes/Lif01ProbeUnitTest.java /tmp/gsd47/wt-probe/src/test/java/com/clickhouse/kafka/connect/sink/
rm -rf /tmp/gsd47/wt-probe/build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd47/wt-probe --console=plain testUnit --rerun --tests '*Lif01ProbeUnitTest*' > /tmp/gsd47/exec/probe-lif01.log 2>&1
cp -R /tmp/gsd47/wt-probe/build/test-results/testUnit /tmp/gsd47/exec/xml-lif01-probe
git worktree remove --force /tmp/gsd47/wt-probe && git worktree prune
python3 -I -c '<apply the single old/new pair of fork_delta_item13 from 47-doc-edits.json to docs/FORK-DELTA.md, old text asserted to occur once>'
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd47/exec/msg-fd.txt
```

```text
testunit_docker_host: unix:///nonexistent.sock
testunit_exit: 0
testunit_tests: 2187
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
status_delta_testunit: delta: removed=0 added=6 expected=6 match=yes
classification_pins_equal_base: yes
gate_tree_equals_fix_commit: yes
probe_source: research copy
probe_tests: 2
probe_failures: 0
both_throw_thrown: IllegalArgumentException:proxy side failure
both_throw_suppressed: 0
probe_worktree_removed: yes
fork_delta_item13: exact
fork_delta_commit: e1b03148940c85683c9b0376aa07b1b38d58eab6
```

The probe at the fix commit shows that when both the strategy's and the proxy task's `stop()` throw, the proxy side's exception is the one thrown and the strategy's is dropped with 0 suppressed (Java discards the `try` exception when the `finally` throws); the pin does not assert which exception surfaces, only that both stops ran and a `RuntimeException` reached the caller, so a later move to the `keepFirst` idiom needs no assertion edit. The same probe confirmed the single-throw case at the fix commit: strategy stop 1, writer stop 1, state close 1, MBean unregistered, the strategy's own exception reaching the caller. Backlog candidates for the SUMMARY, both unchanged in this phase (D-1): a `finally` in `close(partitions)`, which has the same no-`finally` shape, and the in-repo `keepFirst` idiom in `ClickHouseSinkTask.stop()`, which would attach the strategy's exception as suppressed. Nothing of the probe worktree is committed.

Recorded 2026-11-05T20:25:01Z.

Verdict: pass

### E-05: RTY-01 limit pin: 108 rows green, expected added rows, budget-of-5 mutation, testUnit gate

Backs: RTY-01, ROADMAP Phase 47 criterion 3 (record branch: today's unbounded redelivery of every retriable writer-failure shape is pinned through `ProxySinkTask.put` under both `errors.tolerance` values, and the pin is shown to fail against a retry budget), U-1.

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*RetriableRedeliveryLimitPinUnitTest*' > /tmp/gsd47/exec/green-rty01.log 2>&1
rm -rf /tmp/gsd47/exec/xml-rty01-green && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-rty01-green
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-rty01-green /tmp/gsd47/exec/status-rty01-green.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py count /tmp/gsd47/exec/status-rty01-green.tsv .RetriableRedeliveryLimitPinUnitTest PASS --name-contains firstPutOutcomeThroughProxySinkTaskPut   # and retriableShapeIsRedeliveredWithoutBound
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py select .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added.tsv PASS /tmp/gsd47/exec/status-rty01-green.tsv .RetriableRedeliveryLimitPinUnitTest
shasum -a 256 .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added.tsv
git worktree add --detach /tmp/gsd47/fmt HEAD   # per-file Spotless on the pin, cmp, worktree removed
git add src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java && git commit -F /tmp/gsd47/exec/msg-pin.txt
git worktree add --detach /tmp/gsd47/wt-mut 097243fd9b24a4ebf27ff548b3ed0ae450f00c3a
python3 -I -c '<in the worktree ProxySinkTask.java: add field mutationConsecutiveFailures; before "boolean errorTolerance = ..." throw RuntimeException when ++mutationConsecutiveFailures > 5; each old text asserted to occur once>'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd47/wt-mut --console=plain testUnit --rerun --tests '*RetriableRedeliveryLimitPinUnitTest*' > /tmp/gsd47/exec/mut-rty01.log 2>&1
cp -R /tmp/gsd47/wt-mut/build/test-results/testUnit /tmp/gsd47/exec/xml-rty01-mut
git worktree remove --force /tmp/gsd47/wt-mut && git worktree prune
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-rty01-mut /tmp/gsd47/exec/status-rty01-mut.tsv
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 -- src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd47/exec/rty01-gate-testunit.log 2>&1
rm -rf /tmp/gsd47/exec/xml-rty01-gate-testunit && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-rty01-gate-testunit
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-rty01-gate-testunit /tmp/gsd47/exec/status-rty01-gate-testunit.tsv
cat .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added.tsv > /tmp/gsd47/exec/expected-phase.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py delta /tmp/gsd47/exec/status-base-testunit.tsv /tmp/gsd47/exec/status-rty01-gate-testunit.tsv /tmp/gsd47/exec/expected-phase.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit /tmp/gsd47/exec/xml-rty01-gate-testunit --class .WrappedDataExceptionClassificationPinUnitTest   # and the seven other pin classes
git diff --name-only 796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe HEAD -- src/main
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
```

```text
pin_docker_host: unix:///nonexistent.sock
pin_tests: 108
pin_failures: 0
pin_errors: 0
matrix_rows: 44
redelivery_rows: 64
redeliveries: 500
pin_class_seconds: 22.037
expected_added_rows: 108
expected_added_sha256: c4f61823ac961e86ac891cae73559baf975c27fd5508e8492cde4f28e2796c94
fmt_rl: identical
pin_commit: 097243fd9b24a4ebf27ff548b3ed0ae450f00c3a
mutation_tests: 108
mutation_failures: 64
mutation_matrix_pass: 44
mutation_messages_at_redelivery_6: 64
mutation_main_tree_untouched: yes
mutation_worktree_removed: yes
testunit_docker_host: unix:///nonexistent.sock
testunit_exit: 0
testunit_tests: 2295
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
status_delta_testunit: delta: removed=0 added=114 expected=114 match=yes
classification_pins_equal_base: yes
main_diff_since_fix: none
```

The pin is a characterization pin, so it is not red first; the mutation run is its evidence that it can fail. The mutation added a consecutive-failure counter of 5 to `ProxySinkTask.put` in the removed worktree `/tmp/gsd47/wt-mut` (two lines, never in the main tree): exactly the 64 redelivery rows failed, each with `redelivery 6 ==> Unexpected exception type thrown, expected: RetriableException`, and the 44 first-put rows stayed green. The gate run is E-01's 2181 tests plus the 6 LIF-01 rows plus the 108 RTY-01 rows (2295), with 2 skips as at the base; the eight classification and stop pin classes keep E-01's counts. The only `src/main` change since the phase base is `ClickHouseSinkTask.java` (LIF-01), and none since the fix commit. The pin class took 22.037 s (research measured under 0.3 s per shape and tolerance combination for 6 combinations; this pin runs 64 redelivery rows of 500 calls each, and the cause of the longer total was not investigated). No JVM crash occurred and `-PjavaVersion=21` was not used.

First-put outcomes of the 22 shapes (measured through `ProxySinkTask.put`, identical to the plan's table):

| # | Shape | `none` | `all` | In the redelivery rows |
|---|---|---|---|---|
| S01 | `PLAIN_DATA_EXCEPTION` | FAILS_TASK | DLQ | no |
| S02 | `DATA_EXCEPTION_OVER_RETRIABLE_SERVER_CODE` | RETRIABLE | RETRIABLE | yes |
| S03 | `DATA_EXCEPTION_OVER_SOCKET_TIMEOUT` | RETRIABLE | RETRIABLE | yes |
| S04 | `DATA_EXCEPTION_OVER_UNKNOWN_HOST` | RETRIABLE | RETRIABLE | yes |
| S05 | `DATA_EXCEPTION_OVER_RETRIABLE` | RETRIABLE | RETRIABLE | yes |
| S06 | `DATA_EXCEPTION_OVER_NON_RETRIABLE_SERVER_CODE` | FAILS_TASK | DLQ | no |
| S07 | `DATA_EXCEPTION_OVER_ADDITIONAL_RETRIABLE_CODE` | RETRIABLE | RETRIABLE | yes |
| S08 | `DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_ON` | RETRIABLE | RETRIABLE | yes |
| S09 | `DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_OFF` | FAILS_TASK | DLQ | no |
| S10 | `DATA_EXCEPTION_OVER_READ_TIMED_OUT` | RETRIABLE | RETRIABLE | yes |
| S11 | `DATA_EXCEPTION_OVER_PLAIN_IO` | FAILS_TASK | DLQ | no |
| S12 | `DATA_EXCEPTION_OVER_V1_TIMEOUT_EXCEEDED` | RETRIABLE | RETRIABLE | yes |
| S13 | `RETRIABLE_OVER_RUNTIME_EXCEPTION` | RETRIABLE | RETRIABLE | yes |
| S14 | `RETRIABLE_OVER_TIMEOUT_EXCEPTION` | RETRIABLE | RETRIABLE | yes |
| S15 | `RUNTIME_EXCEPTION_OVER_SOCKET_TIMEOUT_CONTROL` | RETRIABLE | RETRIABLE | yes |
| S16 | `RUNTIME_EXCEPTION_OVER_ILLEGAL_STATE_CONTROL` | FAILS_TASK | DLQ | no |
| S17 | `DATA_EXCEPTION_OVER_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE` | RETRIABLE | RETRIABLE | yes |
| S18 | `CONVERSION_DATA_EXCEPTION_CONTROL` | ESCAPES_RAW | ESCAPES_RAW | no |
| S19 | `BARE_RETRIABLE` | RETRIABLE | RETRIABLE | yes |
| S20 | `RETRIABLE_OVER_SOCKET_TIMEOUT` | RETRIABLE | RETRIABLE | yes |
| S21 | `RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE` | RETRIABLE | RETRIABLE | yes |
| S22 | `BARE_RETRIABLE_SERVER_CODE` | RETRIABLE | RETRIABLE | yes |

Recorded 2026-11-05T20:31:17Z.

Verdict: pass

### E-06: Kafka Connect redelivery of a RetriableException from put, measured on a worker and read in the sources

Backs: RTY-01, ROADMAP Phase 47 criterion 3 (what Kafka Connect does with a `RetriableException` from `put`, measured on a real worker of the Kafka 3.9 line and read in the `WorkerSinkTask` sources of 3.9.1, 4.1.1 and 4.4.0), U-1.

```sh
D=$(ls -d /opt/homebrew/Cellar/docker/*/bin/docker | tail -1)
"$D" network create gsd47-net
"$D" run -d --name gsd47-kafka --network gsd47-net -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://gsd47-kafka:9092 -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@gsd47-kafka:9093 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 confluentinc/cp-kafka:7.9.10
"$D" exec gsd47-kafka kafka-topics --bootstrap-server gsd47-kafka:9092 --create --topic probe1 --partitions 1 --replication-factor 1   # probe2, probe3; one record each through kafka-console-producer
"$D" create --name gsd47-build --entrypoint sleep confluentinc/cp-kafka-connect-base:7.9.7 600   # docker cp the source in, javac -cp "/usr/share/java/kafka/*", jar, docker cp the jar out, remove the container
"$D" create --name gsd47-connect --network gsd47-net --entrypoint connect-standalone confluentinc/cp-kafka-connect-base:7.9.7 /conf/worker.properties /conf/c1.properties /conf/c2.properties /conf/c3.properties
"$D" cp /tmp/gsd47/exec/live/conf gsd47-connect:/conf && "$D" cp /tmp/gsd47/exec/live/plugins gsd47-connect:/plugins && "$D" start gsd47-connect   # wait until c1 and c3 printed attempt=3 (126 s)
"$D" exec gsd47-connect curl -s http://localhost:8083/connectors/<name>/status   # c1, c2, c3
"$D" logs gsd47-connect > /tmp/gsd47/exec/live/connect.log 2>&1
"$D" exec gsd47-kafka kafka-get-offsets --bootstrap-server gsd47-kafka:9092 --topic dlq1   # and dlq2
"$D" rm -f gsd47-connect
"$D" create --name gsd47-connect2 --network gsd47-net --entrypoint connect-standalone confluentinc/cp-kafka-connect-base:7.9.7 /conf/worker-flush7000.properties /conf/c3.properties   # run twice, see the process note
"$D" logs gsd47-connect2 > /tmp/gsd47/exec/live/connect-flush7000.log 2>&1
"$D" rm -f gsd47-connect2 gsd47-kafka && "$D" network rm gsd47-net
python3 -I -c '<brace-match the body of private void deliverMessages() in WorkerSinkTask.java of /tmp/gsd47/x39, /tmp/gsd47/x411 and /tmp/gsd47/x44, compare, search it for task.put(, retryWithToleranceOperator, catch (RetriableException e) and pauseAll()>'
grep -rn 'context.timeout(' src/main/java | wc -l
```

```text
probe_source: research copy, connector name added
connect_image: confluentinc/cp-kafka-connect-base:7.9.7
connect_image_digest: confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055
kafka_image: confluentinc/cp-kafka:7.9.10
kafka_image_digest: confluentinc/cp-kafka@sha256:07c3d60362e67c296f7f34b690b1240e5ecdcde4a84f5e73cab320fb4041f58c
connect_runtime_version: 7.9.7-ccs
c1_put_instants_ms: 506,60042,120055
c1_attempts: 3
c1_gap_2_3_ms: 60013
c1_task_state: RUNNING
c1_error_lines: 3
dlq1_end_offsets_sum: 0
c3_put_instants_ms: 449,60012,120013
c3_attempts: 3
c3_gap_2_3_ms: 60001
c3_task_state: RUNNING
c1_c3_attempt3_delta_ms: 42
c2_attempts: 1
c2_task_state: FAILED
c2_trace_unrecoverable: yes
dlq2_end_offsets_sum: 0
flush7000_put_instants_ms: 19856,21065,28069,35073,42079
flush7000_attempts: 5
flush7000_gaps_from_attempt_2_ms: 7004,7004,7006
observe_window_s: 47
sources_location: /tmp/gsd47/x39, /tmp/gsd47/x411, /tmp/gsd47/x44 (connect-runtime-3.9.1, 4.1.1 and 4.4.0 sources jars from the research, in /tmp/gsd47/src39, src411 and src44)
deliver_messages_identical: yes
put_outside_retry_operator: yes
retriable_catch_pauses_all: yes
errors_retry_timeout_default: 0
connector_calls_context_timeout: no
containers_left: none
networks_left: none
```

Connector names are `c1-retriable-tolerance-all-retrytimeout5000` (c1), `c2-data-tolerance-all` (c2) and `c3-retriable-defaults` (c3); each had one task and one record. Instants are milliseconds since the task object was created, read from the `PROBE put` lines of the saved logs. `c1_error_lines` counts the worker-log lines naming c1's task and `RetriableException from SinkTask` (3, equal to its attempts; c3 had 3 as well). The `deliverMessages` body is 2215 characters in each of the three source versions and byte-identical; it calls `task.put(` with no `retryWithToleranceOperator` in the body and handles `RetriableException` by pausing all partitions.

| Connector | Config under test | `put` call instants (ms) | Task state | DLQ end offsets | Outcome |
|---|---|---|---|---|---|
| c1 | always `RetriableException`; `errors.retry.timeout=5000`, `errors.tolerance=all`, DLQ topic `dlq1` | 506, 60042, 120055 | RUNNING | 0 | redelivered about every 60 s, one ERROR line per redelivery |
| c3 | always `RetriableException`; Connect defaults | 449, 60012, 120013 | RUNNING | n/a | same instants within 42 ms at the third attempt |
| c3 on a worker with `offset.flush.interval.ms=7000` | defaults | 19856, 21065, 28069, 35073, 42079 | not read | n/a | gaps from attempt 2: 7004, 7004, 7006 ms |
| c2 | always `DataException`; `errors.tolerance=all`, DLQ topic `dlq2` | 461 | FAILED | 0 | one call, `Task threw an uncaught and unrecoverable exception`, `ConnectException: Exiting WorkerSinkTask due to unrecoverable exception.` |

Process notes. The first run of the flush-7000 worker (`/tmp/gsd47/exec/live/connect-flush7000-run1.log`) reached only 4 attempts inside the plan's 60 s window: its first `put` came at 40639 ms because the consumer group of c3 still held the member of the force-removed first worker, so the group rebalance waited for that member's session to expire (the gaps of that run from attempt 2, 7006 and 7008 ms, agree with the recorded run). The same worker was run again with the same files and the observation capped at 150 s instead of 60 s; the second run is the one recorded above (its first `put` came at 19856 ms for the same reason). The first `put` instants are therefore not a cadence fact; the gaps are. In the default-cadence worker the first call came at about 0.5 s instead of the 3.6 s of the research run; the gaps (60013 and 60001 ms) match the research run's (60001 and 60008 ms between its second and third calls). The research's c1 and c3 third calls were at 120010 and 120051 ms; here at 120055 and 120013 ms.

Recorded 2026-11-05T21:09:27Z.

Verdict: pass

### E-07: FORK-DELTA item 17 rewritten from E-05 and E-06

Backs: RTY-01, ROADMAP Phase 47 criterion 3 (the limit documented in FORK-DELTA with the measured facts only), U-1.

```sh
python3 -I -c '<rebuild only the item-17 line of docs/FORK-DELTA.md: the new title, the existing classification sentences verbatim, the new tail from 47-02-PLAN.md with the E-06 values substituted>'
git diff --numstat docs/FORK-DELTA.md
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd47/exec/msg-fd17.txt
python3 -I -c '<item-17 check at the commit: one line changed, classification sentences verbatim, title, required phrases, runtime version, every E-06 instant and gap, no open FUT-05 reference>'
grep -cF '(FUT-05)' docs/FORK-DELTA.md
```

```text
fork_delta_item17: yes
item17_lines_changed: 1
item17_numbers_from_e06: yes
fork_delta_commit: e1f3947454f15ae51e245e6522ff27c7399e50fe
```

The commit changes exactly one line of `docs/FORK-DELTA.md` (1 insertion, 1 deletion, file line count unchanged). The numbers written into the item are E-06's: runtime `7.9.7-ccs`; c1 calls at 506, 60042 and 120055 ms and c3 calls at 449, 60012 and 120013 ms; flush-7000 gaps 7004, 7004 and 7006 ms; E-05's counts: 22 shapes, 16 retriable shapes, 500 redeliveries, 64 redelivery cases red and 44 first-put cases green under the budget of 5. `(FUT-05)` remains on exactly one line of FORK-DELTA (item 8, closed in plan 47-03). `exactlyOnce=true` is stated as not pinned and the Phase 46 interaction as derived from item 14.

Recorded 2026-11-05T21:10:50Z.

Verdict: pass

### E-08: FUT-05 closed by decision: the record, its reason, FORK-DELTA items 8 and 24, WARN and config unchanged

Backs: RTY-02, ROADMAP Phase 47 criterion 4 (FUT-05 closed with a recorded decision and its reason, the WARN kept, no validator, FORK-DELTA no longer lists it as open), U-1, U-2.

```sh
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java src/main/java/com/clickhouse/kafka/connect/util/Utils.java src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
test "$(git show 427f30b4c4dffd56841f5e5eafe13a55ac4caca8:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java | sed -n '22,28p')" = "$(sed -n '22,28p' src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java)"   # and lines 136,143
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java
python3 -I -c '<apply the three old/new pairs of fork_delta_fut05 from 47-doc-edits.json to docs/FORK-DELTA.md, each old text asserted to occur once>'
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd47/exec/msg-fd-fut05.txt
python3 -I -c '<replay the three pairs on the commit parent and compare with the commit>'
grep -c -e 'FUT-05 stays open' -e 'is not closed by this' -e '(FUT-05)' docs/FORK-DELTA.md
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
```

```text
config_unchanged_since_base: yes
warn_text_unchanged: yes
warn_pins_unchanged: yes
warn_tests_green: yes
decision: FUT-05 closed: no retry budget, no setting that removes a code from the built-in set, no validator; the start-up WARN and its two pins kept
decided_by: user
decided_on: 2026-11-05
decision_source: 47-CONTEXT.md U-1 and U-2, answered via AskUserQuestion
reason: transient is the connector's judgment and ClickHouse has no such classification; a rejecting check breaks running connectors at their next configuration write (ConfigDef validator) or next task start (constructor check)
validator_added: no
budget_added: no
removal_setting_added: no
fork_delta_fut05: exact
fork_delta_open_fut05_lines: 0
fork_delta_commit: 6ff00ea641e31d8f6df119524797b064c86cb665
anchors_mirror: rows=218 bad=0
```

The user decided on 2026-11-05 (47-CONTEXT.md U-2): "NO validator. Keep the start-up WARN and its two pins unchanged; close FUT-05 with the reason; FORK-DELTA items 8, 17 and 24 stop listing it as open." What stays as it was at the phase base: the WARN text (`UNVETTED_RETRIABLE_CODES_WARNING` and its method `warnOnUnvettedRetriableCodes`, lines 22-28 and 136-143 of `ClickHouseSinkTask.java`), `UnvettedRetriableCodesWarnUnitTest`, `UnvettedRetriableCodesWarnLiveTest`, `AdditionalRetriableErrorCodesPinUnitTest`, `AdditionalRetriableErrorCodesTest`, and the setting's description in `ClickHouseSinkConfig` (the file is byte-identical to the phase base, as are `Utils` and `ProxySinkTask`). `warn_tests_green` rests on E-05's testUnit gate (2295 tests, 0 failures), in which the E-01 `warn_tests` count of 21 for `UnvettedRetriableCodesWarnUnitTest` is unchanged. The commit changes exactly two lines of `docs/FORK-DELTA.md` (items 8 and 24, 2 insertions and 2 deletions); the Connector-source WARN row that cites Known cost 24 is not edited, and no line of the file carries the open FUT-05 parenthesis, the stays-open wording or the not-closed sentence. No source or test file changed in this task.

Recorded 2026-11-05T21:13:30Z.

Verdict: pass

### E-09: CHANGELOG 1.7.0_gg8 section started with the corrected stop() bullets; gg7 section untouched

Backs: LIF-02, ROADMAP Phase 47 criterion 2 (the CHANGELOG `stop()` Bug Fixes and Known Issues bullets describe the fixed behaviour and claim no more than the LIF-01 pin and the E-04 probe show), U-3.

```sh
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py expect .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md E-04 'both_throw_thrown^IllegalArgumentException' 'both_throw_suppressed=0'   # and E-03 green_tests=6
python3 -I -c '<read the changelog_gg8 value of 47-doc-edits.json and write it followed by the unchanged CHANGELOG.md content>'
python3 -I -c '<SHA-256 and maximum line length of the changelog_gg8 value; CHANGELOG.md equals the value followed by the base content>'
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 HEAD -- VERSION
git add CHANGELOG.md && git commit -F /tmp/gsd47/exec/msg-cl.txt
python3 -I -c '<replay the prepend on the commit parent and compare with the commit>'
```

```text
e04_both_throw_thrown: IllegalArgumentException:proxy side failure
e04_both_throw_suppressed: 0
e03_green_tests: 6
changelog_first_line: # 1.7.0_gg8, Unreleased
changelog_earlier_content_identical: yes
gg8_sha256: 0fa57f146c9923b6d87881631adaaddedbd88d3d7d87ff2a97f7e1e3ed97b8b8
gg8_max_line_length: 117
gg7_section_edited: no
version_unchanged: yes
gg8_claims_backed: yes
changelog_commit: 15aa445fac0b034ffdd57e64e88042af4e196db5
```

The commit adds 22 lines and removes none; the file equals the `changelog_gg8` text of `47-doc-edits.json` (md5 of the JSON `3c4b1ce86e50bf8970e744a7eb6dc3a6`) followed by the phase base's `CHANGELOG.md` byte for byte, so the published `1.7.0_gg7` section and everything below are unchanged. The section carries only the `stop()` bullets; the rest of the gg8 section (upstream tail, timeout limits, the RTY-01 limit and the FUT-05 closure bullets, the date) is Phase 49's.

Claims map, each sentence against the fact that backs it:

| Sentence | Evidence entry and fact |
|---|---|
| `stop()` stops the proxy task (writer, MBean, state provider) when the strategy's `stop()` throws | E-02: `strategyStopFailureStillStopsTheProxyTask` red on the unchanged tree (`the writer must have been stopped once ==> expected: <1> but was: <0>`); E-03: `green_tests: 6`, `green_failures: 0` after the fix commit |
| when only the strategy throws, its exception is the one that reaches Kafka Connect | E-03: the same case is green and asserts `assertSame(strategy.failure, thrown, ...)` (line 220 of `ClickHouseSinkTaskStopUnitTest`); E-04 narrative: single-throw probe at the fix commit, strategy stop 1, writer stop 1, state close 1, MBean unregistered, the strategy's own exception to the caller; 47-RESEARCH.md: `WorkerSinkTask.close()` only logs a throw from `stop()` |
| with each of the three shipped strategies `stop()` returns normally and stops the proxy task, as before | E-02: the four other cases (no strategy and the three shipped strategies, `stopReachesTheProxyTaskAfterEachShippedStrategy`) green on the unchanged tree (`red_pass: 4`); E-03: all 6 green after |
| the gg7 sentence held for `ProxySinkTask.stop()` and not for `ClickHouseSinkTask.stop()` | E-01: `proxy_stop_tests: 4` (`ProxySinkTaskStopUnitTest`) green on the base and in E-03's neighbour run; E-02: the red run on the unchanged `ClickHouseSinkTask.stop()` |
| the other residuals and the five-second wait | FORK-DELTA item 13 (read from the code, unchanged by this phase): the V1 Keeper constructor, the helper-client constructor, the second `start()`, and `awaitTermination` for at most 5 s |
| both-throw outcome | E-04: `both_throw_thrown: IllegalArgumentException:proxy side failure`, `both_throw_suppressed: 0` (a one-off probe at the fix commit, measured and not pinned) |

Recorded 2026-11-05T21:14:30Z.

Verdict: pass

### E-10: Phase gate: testUnit with Docker unreachable, full suite on latest x V2, deltas, scope, anchors, tail gate, nothing pushed

Backs: LIF-01, LIF-02, RTY-01, RTY-02, ROADMAP Phase 47 phase gate (cache-busted `testUnit`, a full suite on `latest` x V2 and the anchors invariant, measured on the phase tip, closing criteria 1-4).

```sh
git ls-remote upstream main
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
git ls-remote origin refs/heads/sync/v1.7.0
git rev-parse HEAD
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 a9669f7b0d8e54af8a8840e5d1650cd7ae91c58c > /tmp/gsd47/exec/gate-tail.log 2>&1   # background, explicit SHA
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd47/exec/gate-testunit.log 2>&1
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py failed build/test-results/testUnit
rm -rf /tmp/gsd47/exec/xml-gate-testunit && cp -R build/test-results/testUnit /tmp/gsd47/exec/xml-gate-testunit
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-gate-testunit /tmp/gsd47/exec/status-gate-testunit.tsv
cat .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added.tsv > /tmp/gsd47/exec/expected-phase.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py delta /tmp/gsd47/exec/status-base-testunit.tsv /tmp/gsd47/exec/status-gate-testunit.tsv /tmp/gsd47/exec/expected-phase.tsv
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd47/exec/gate-suite.log 2>&1
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py failed build/test-results/test
rm -rf /tmp/gsd47/exec/xml-gate-suite && cp -R build/test-results/test /tmp/gsd47/exec/xml-gate-suite
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/xml-gate-suite /tmp/gsd47/exec/status-gate-suite.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py delta /tmp/gsd47/exec/status-base-suite.tsv /tmp/gsd47/exec/status-gate-suite.tsv /tmp/gsd47/exec/expected-phase.tsv --by-class
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py junit /tmp/gsd47/exec/xml-gate-suite --class .ClickHouseSinkTaskStopUnitTest   # and .RetriableRedeliveryLimitPinUnitTest and the eight E-01 pin classes
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
bash /tmp/gsd47/exec/scope04.sh   # git diff and git show checks against B0 (scope, stop() body, untouched files, tests, build files, CHANGELOG, docs, FORK-DELTA, gate scripts, merges), anchors mirror
```

```text
gate_head: a9669f7b0d8e54af8a8840e5d1650cd7ae91c58c
upstream_main_at_gate: 83808df9a0993cf49b2ceaa4633c782e550c0180
origin_pre_tag_at_gate: absent
origin_sync_branch_at_gate: absent
testunit_docker_host: unix:///nonexistent.sock
testunit_exit: 0
testunit_tests: 2295
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
status_delta_testunit: delta: removed=0 added=114 expected=114 match=yes
suite_exit: 0
suite_tests: 2898
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
status_delta_suite_by_class: delta: removed=0 added=114 expected=114 match=yes
status_delta_suite_by_class_exit: 0
suite_version: 26.10.3.7
suite_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
suite_java: default toolchain
suite_crash: no
phase_classes_in_suite: ClickHouseSinkTaskStopUnitTest 6, RetriableRedeliveryLimitPinUnitTest 108, all green
classification_pins_equal_base: yes
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
main_files_added: none
stop_body_check: try/finally only
untouched_main_files: yes
test_files_added_only: yes
build_files_unchanged: yes
changelog_earlier_content_identical: yes
docs_changed: docs/FORK-DELTA.md
fork_delta_final_checks: yes
gate_scripts_unchanged: yes
merges_since_base: none
anchors_mirror: rows=218 bad=0
tail_gate_line: invariants: provenance_paths=11 anchor_commits=108 anchor_rows=218 marker_commits=126 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

The testUnit gate is E-01's 2181 tests plus 114 (6 LIF-01 and 108 RTY-01 rows) with 2 skips as at the base; the full suite is E-01's 2784 plus 114 with the 18 skips (7 named, 11 state-mismatch cases, none unexpected). Both per-testcase deltas against the E-01 dumps remove nothing and add exactly the two expected lists (exact over testUnit, by class over the suite, where only the two new classes gain rows). The eight E-01 pin classes keep their E-01 counts (14, 38, 216, 4, 21, 19, 2, 15), so `ProcessingTest` and the other unchanged suites are green in both runs. The server is `26.10.3.7` at the same digest as E-01, so the `latest` image did not move during the phase. No JVM crash occurred and `-PjavaVersion=21` was not used; no core dump or javacore file was left in the repository root. `upstream main` is still `83808df9` (the `UPSTREAM_REF` of the gate), `pre-v1.7.0_gg8` is absent on origin and the origin sync branch is absent as in E-01; nothing was pushed in this plan. The tail gate ran in the background over `12e2cf17..a9669f7b` while the testUnit and suite ran (it reads git objects only; the tree was not changed during the run), and its summary line carries `anchor_rows=218` with `anchor_exempt=0`, against `anchor_rows=216` at the phase base (E-01). The `fork_delta_final_checks` fact is the five counts of the plan: the open FUT-05 parenthesis 0, the stays-open wording 0, the not-closed sentence 0, `FUT-05 is closed by decision` 1, `ClickHouseSinkTaskStopUnitTest` 1 and `RetriableRedeliveryLimitPinUnitTest` 1 (at least 1 required). The `BEFORE`/`AFTER` flag sequence lives in `Processing.java` and `KeeperStateProvider.java`, both byte-identical to the phase base, as are `ProxySinkTask`, `Utils`, `ClickHouseSinkConfig`, `ChunkFlusher` and the three strategies.

Recorded 2026-11-05T21:22:37Z.

Verdict: pass

### E-11: Phase 49 hand-off inventory, recorded and not edited

Backs: LIF-02, RTY-02, ROADMAP Phase 47 phase gate (what Phase 49 and the orchestrator inherit, located live on the phase tip and shown unedited), D-2, U-3.

```sh
git diff --quiet v1.7.0_gg7 HEAD -- docs/ROLLOUT-v1.7.0_gg7.md
grep -n '^| F5 |' docs/ROLLOUT-v1.7.0_gg7.md
grep -n '^| R8 |' docs/ROLLOUT-v1.7.0_gg7.md
grep -nxF 'FUT-05 stays open.' docs/ROLLOUT-v1.7.0_gg7.md
grep -n '^- The closing of helper clients has residuals' docs/ROLLOUT-v1.7.0_gg7.md
grep -n '^| C2 |' docs/ROLLOUT-v1.7.0_gg6.md
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 HEAD -- docs/ROLLOUT-v1.7.0_gg6.md
grep -n 'is redelivered with no retry budget' CHANGELOG.md
grep -n 'a decision on changing' CHANGELOG.md
head -1 CHANGELOG.md
python3 -I -c '<the "## The raw numbers" section of docs/FORK-DELTA.md up to the next "## " heading, at the phase base and now, compared>'
grep -n 'Future Requirements' docs/FORK-DELTA.md
grep -n -A6 '^## Future Requirements' .planning/REQUIREMENTS.md
grep -nE 'delivery strategy|deliveryStrategy' .planning/PROJECT.md .planning/MILESTONES.md
grep -n -A8 'public void close(Collection<TopicPartition> partitions)' src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
grep -nE 'LIF-01|LIF-02|RTY-01|RTY-02' .planning/REQUIREMENTS.md
```

```text
rollout_gg7_identical_to_tag: yes
rollout_gg7_f5_line: 52
rollout_gg7_r8_line: 126
rollout_gg7_fut05_line: 130
rollout_gg7_residuals_bullet_line: 208
rollout_gg6_c2_line: 114
rollout_gg6_unchanged_since_base: yes
changelog_gg7_known_issue_budget_line: 85
changelog_gg7_known_issue_open_decision_line: 87
changelog_gg8_unreleased: yes
raw_numbers_section_unchanged: yes
fork_delta_item8_requirements_reference_stale: yes
planning_records_with_stop_claim: .planning/MILESTONES.md:24; .planning/PROJECT.md:48; .planning/PROJECT.md:661
backlog_candidates: close(partitions) runs the proxy's partition removal before the strategy's close with no finally (FORK-DELTA item 13, read from the code); the keepFirst idiom of ProxySinkTask.stop() in ClickHouseSinkTask.stop() would attach the strategy's exception as suppressed (E-04)
handoff_items: 14
```

Located unedited, each line number read live with `grep -n`. In `docs/ROLLOUT-v1.7.0_gg7.md` (identical to tag `v1.7.0_gg7`): row F5 (line 52) still lists "`ClickHouseSinkTask.stop()` without `try`/`finally` around the delivery strategy's `stop()`" as not fixed; row R8 (line 126) still says there is no retry budget and no setting removes a code, and cites FORK-DELTA item 24; line 130 is exactly `FUT-05 stays open.`; the residuals bullet (line 208) still names "a task-level `stop()` without `try`/`finally` around the delivery strategy's `stop()`". In `docs/ROLLOUT-v1.7.0_gg6.md` (unchanged since the phase base) row C2 (line 114) says the redelivery has no retry budget and ends "That follow-up is FUT-05, open in FORK-DELTA.md under Known costs and open items", which FORK-DELTA no longer says. In `CHANGELOG.md` the gg8 section starts at line 1 (`# 1.7.0_gg8, Unreleased`) and the gg7 section at line 23; its Known Issues bullet on the retry budget starts at line 85 and the bullet on writer `DataException` shapes at line 87 (its last words: "a decision on changing them is open"). The `## The raw numbers` section of `docs/FORK-DELTA.md` is identical to the phase base's. Item 8 of FORK-DELTA (line 232) still points the reader to the Future Requirements list of `.planning/REQUIREMENTS.md` ("remains open and is not contradicted by anything here"), while that section holds two prose bullets (upstream PRs #861, #855, #854 and #788; the many-to-one rename and partition-changing SMTs) and no `FUT-0` identifier, so the identifiers FUT-01 to FUT-04 that item 8 lists are not findable there. The `close(partitions)` code (lines 161 to 169 of `ClickHouseSinkTask.java`) calls `proxySinkTask.onPartitionRemoved(partitions)` and then `deliveryStrategy.close(partitions)` with no `finally`, so a throw from the first skips the second. `.planning/REQUIREMENTS.md` still shows LIF-01 and LIF-02 (lines 26 and 27) and RTY-01 and RTY-02 (lines 37 and 38) as unchecked, and the traceability rows for the four (lines 87, 88, 92 and 93) as `Pending`.

| # | Item | Owner | Current location | What to do |
|---|------|-------|------------------|------------|
| 1 | gg7 rollout row F5 | Phase 49 (D-2) | `docs/ROLLOUT-v1.7.0_gg7.md` line 52 (the gg7 note stays a tag copy) | the gg8 rollout note corrects it: the task-level `stop()` residual is closed (FORK-DELTA item 13, E-03); the other residuals (the V1 Keeper constructor, the helper-client constructor, the second `start()`, the five-second wait) stay open |
| 2 | gg7 rollout row R8 | Phase 49 (D-2) | `docs/ROLLOUT-v1.7.0_gg7.md` line 126 | the gg8 note corrects it: the missing budget is a recorded limit by decision (FORK-DELTA item 17, E-05 and E-06); no setting removes a code from the built-in set, by decision (item 24, E-08) |
| 3 | gg7 FUT-05 status line | Phase 49 (D-2) | `docs/ROLLOUT-v1.7.0_gg7.md` line 130, `FUT-05 stays open.` | the gg8 note states that FUT-05 is closed by decision (FORK-DELTA item 24, E-08) |
| 4 | gg7 residuals bullet | Phase 49 (D-2) | `docs/ROLLOUT-v1.7.0_gg7.md` line 208 | the gg8 note corrects the task-level `stop()` part of the bullet (see row 1) |
| 5 | gg6 rollout row C2 | Phase 48 edits the gg6 note; Phase 49 reviews it | `docs/ROLLOUT-v1.7.0_gg6.md` line 114, "That follow-up is FUT-05, open in FORK-DELTA" | FORK-DELTA closes FUT-05 by decision (items 8, 17 and 24), so the clause is stale; correct it with the Phase 48 edits of this note, or record why not |
| 6 | gg8 CHANGELOG section | Phase 49 (U-3) | `CHANGELOG.md` lines 1 to 22, `# 1.7.0_gg8, Unreleased`, only the `stop()` bullets | complete and date it: the upstream tail, the timeout limits, the RTY-01 limit bullet and the FUT-05 closure bullet |
| 7 | superseded gg7 Known Issues bullets | Phase 49 (U-3) | `CHANGELOG.md` line 85 (retry budget) and line 87 (open decision on writer `DataException` shapes), inside the unedited gg7 section | the gg8 section says both are superseded by the recorded decisions (FORK-DELTA items 17 and 24); the gg7 section is not edited |
| 8 | `VERSION` bump | Phase 49 | `VERSION` (unchanged since the phase base) | bump to the gg8 release version when the section is dated |
| 9 | FORK-DELTA raw numbers | Phase 49 | `docs/FORK-DELTA.md` line 149, `## The raw numbers` (identical to the phase base) | refresh with the gate totals of the release (this phase's gate: testUnit 2295, suite 2898, 18 skips, anchors 218) |
| 10 | FORK-DELTA item 8 reference | Phases 48-49 | `docs/FORK-DELTA.md` line 232, the Future Requirements list of `.planning/REQUIREMENTS.md` (no `FUT-0` identifier there) | point item 8 at a list that carries the identifiers, or restore them in REQUIREMENTS.md |
| 11 | planning records with the old `stop()` claim | orchestrator | `.planning/MILESTONES.md:24; .planning/PROJECT.md:48; .planning/PROJECT.md:661` | update them to the fixed behaviour; they are planning records, not published documents |
| 12 | REQUIREMENTS checkboxes | orchestrator | `.planning/REQUIREMENTS.md` lines 26, 27, 37, 38 and traceability lines 87, 88, 92, 93 | set LIF-01, LIF-02, RTY-01 and RTY-02 complete (this plan did not touch REQUIREMENTS.md) |
| 13 | backlog: `close(partitions)` | the user's decision (a production change outside this phase's scope; D-1, D-3) | `ClickHouseSinkTask.java` lines 161 to 169, FORK-DELTA item 13 | a `finally` so the strategy's `close(partitions)` runs when the proxy's partition removal throws |
| 14 | backlog: `keepFirst` in `stop()` | the user's decision (D-1, D-3) | `ClickHouseSinkTask.stop()`, E-04 (both-throw probe: the proxy side's exception is thrown, 0 suppressed) | the `keepFirst` idiom of `ProxySinkTask.stop()` would attach the strategy's exception as suppressed; the LIF-01 pin needs no edit |

The tag copy `docs/ROLLOUT-v1.7.0_gg7.md` and the gg6 note, `CHANGELOG.md`'s gg7 section, `VERSION`, `docs/FORK-DELTA.md`'s raw numbers and every planning record named above are unchanged by this plan; only this evidence file changed.

Recorded 2026-11-05T21:29:17Z.

Verdict: pass

### E-12: Gap closure 47-05, docs: FORK-DELTA stop() residual wording (WR-01, IN-03a) and gg8 CHANGELOG bullets (WR-02, IN-03b)

Backs: LIF-02, 47-REVIEW.md WR-01, WR-02 and IN-03 (a review finding is a correction to the text of plan 47-01/47-03, so the two edits diverge from the plan-time copy `47-doc-edits.json`, which stays unedited), D-3, U-3.

```sh
python3 -I /tmp/gsd47/exec/g05/edit_fd13.py   # three old/new pairs on docs/FORK-DELTA.md, each old text asserted to occur once
git diff --numstat docs/FORK-DELTA.md
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd47/exec/g05/msg1.txt
python3 -I /tmp/gsd47/exec/g05/edit_cl.py   # three old/new pairs on CHANGELOG.md, each old text asserted to occur once
git diff --numstat CHANGELOG.md
git add CHANGELOG.md && git commit -F /tmp/gsd47/exec/g05/msg2.txt
grep -c "task's \`stop()\`" docs/FORK-DELTA.md
grep -c -e 'FUT-05 stays open' -e 'is not closed by this' -e '(FUT-05)' docs/FORK-DELTA.md
git show 427f30b4c4dffd56841f5e5eafe13a55ac4caca8:CHANGELOG.md   # compared with the part of CHANGELOG.md from "# 1.7.0_gg7, " on
git diff --name-only 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 HEAD -- src/main
```

```text
fork_delta_commit: 84f804d07ae1d761e03792510b07237073795716
fork_delta_lines_changed: 2
fork_delta_task_stop_open_mentions: 0
fork_delta_open_fut05_lines: 0
fork_delta_closed_by_decision_count: 1
changelog_commit: 3e0575a77842255f055ffa962f15d2f8d244719b
changelog_numstat_since_base: 26 0
changelog_gg7_section_identical_to_base: yes
changelog_close_partitions_mentions: 1
plan_time_copy_md5_unchanged: 3c4b1ce86e50bf8970e744a7eb6dc3a6
plan_time_text_diverges: yes
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
```

WR-01: the `ClickHouseWriter` patch-table row (line 39) now says the other two of the five Known cost 13 residuals belong to the Keeper state provider and to the task's `close(partitions)`, which is what item 13 lists; no mention of the task's `stop()` as an open residual is left in `docs/FORK-DELTA.md`. IN-03a: item 13 now says two of the six `ClickHouseSinkTaskStopUnitTest` cases (the ones with a throwing strategy) were red on the unchanged tree and all six are green after (E-02: `red_tests: 6`, `red_failures: 2`, `red_pass: 4`). WR-02: the CHANGELOG Bug Fixes bullet says the strategy's exception is the one `stop()` throws and that Kafka Connect only logs a throw from `stop()` as a WARN and does not fail the task (read in the sources, 47-RESEARCH.md; not measured); the both-throw Known Issues bullet says both stops run, an exception reaches the caller of `stop()`, and the strategy's exception is dropped, not attached as suppressed and not logged by `stop()` (E-04: `both_throw_thrown: IllegalArgumentException:proxy side failure`, `both_throw_suppressed: 0`; the code of `stop()` has no catch). IN-03b: the gg8 Known Issues first bullet now carries the `close(partitions)` residual that item 13 records (read from the code, `ClickHouseSinkTask.close`, not fixed). Deviations from the plan-time text of `47-doc-edits.json`: the `changelog_gg8` value and the `fork_delta_item13` pair are no longer the committed text (the CHANGELOG now starts with a text that differs from `changelog_gg8`, and item 13 no longer contains the pair's `new` string); the gg7 section below the gg8 section is byte-identical to the phase base. One edit goes slightly past the review's wording and is recorded as a consistency correction: item 13 carried the same "reaches Kafka Connect" claim as the CHANGELOG, so it received the same WARN qualification and the not-logged remark. `src/main` is unchanged by this task (the only difference from the phase base is still `ClickHouseSinkTask.java`).

Recorded 2026-11-05T21:44:49Z.

Verdict: pass

### E-13: Gap closure 47-05, WR-04: RTY-01 pin cut to 50 redeliveries per row, renamed, mutation re-run

Backs: RTY-01, 47-REVIEW.md WR-04 (cost and name of `RetriableRedeliveryLimitPinUnitTest`), U-1.

```sh
python3 -I /tmp/gsd47/exec/g05/edit_test.py   # REDELIVERIES 500 to 50, method rename, Javadoc and comment, each old text asserted to occur once
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*RetriableRedeliveryLimitPinUnitTest*' > /tmp/gsd47/exec/g05/green-rty01.log 2>&1
rm -rf /tmp/gsd47/exec/g05/xml-rty01-green && cp -R build/test-results/testUnit /tmp/gsd47/exec/g05/xml-rty01-green
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/g05/xml-rty01-green /tmp/gsd47/exec/g05/status-rty01-green.tsv
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py count /tmp/gsd47/exec/g05/status-rty01-green.tsv .RetriableRedeliveryLimitPinUnitTest PASS --name-contains firstPutOutcomeThroughProxySinkTaskPut   # and retriableShapeIsStillRedeliveredAfterManyAttempts
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py select .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added-gap.tsv PASS /tmp/gsd47/exec/g05/status-rty01-green.tsv .RetriableRedeliveryLimitPinUnitTest
git worktree add --detach /tmp/gsd47/fmt HEAD   # per-file Spotless on the pin, cmp, worktree removed
git add src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added-gap.tsv && git commit -F /tmp/gsd47/exec/g05/msg4.txt
git worktree add --detach /tmp/gsd47/wt-mut 0c063acf0068fd284ef21adfbadd5f3223d7bed2
python3 -I -c '<in the worktree ProxySinkTask.java: add field mutationConsecutiveFailures; before "boolean errorTolerance = ..." throw RuntimeException when ++mutationConsecutiveFailures > 5; each old text asserted to occur once>'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd47/wt-mut --console=plain testUnit --rerun --tests '*RetriableRedeliveryLimitPinUnitTest*' > /tmp/gsd47/exec/g05/mut-rty01.log 2>&1
cp -R /tmp/gsd47/wt-mut/build/test-results/testUnit /tmp/gsd47/exec/g05/xml-rty01-mut
git worktree remove --force /tmp/gsd47/wt-mut && git worktree prune
python3 -I .planning/phases/47-task-stop-retry-classification/47-testcase-status.py dump /tmp/gsd47/exec/g05/xml-rty01-mut /tmp/gsd47/exec/g05/status-rty01-mut.tsv
git diff --quiet 427f30b4c4dffd56841f5e5eafe13a55ac4caca8 -- src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
```

```text
pin_docker_host: unix:///nonexistent.sock
pin_tests: 108
pin_failures: 0
pin_errors: 0
matrix_rows: 44
redelivery_rows: 64
redeliveries: 50
pin_class_seconds: 2.909
pin_xml_bytes: 3870005
pin_xml_lines: 19291
before_pin_class_seconds: 22.037
before_pin_xml_bytes: 36151467
before_pin_xml_lines: 179491
first_put_rows_equal_before: yes
expected_added_rows: 108
expected_added_file: 47-rty01-expected-added-gap.tsv
expected_added_sha256: 4da0c7ee5c7e33d6362fd73ac6cc613db9bf7fe71abbaa4b51fa124968e535a7
fmt_rl: identical
without_bound_in_test: 0
pin_commit: 0c063acf0068fd284ef21adfbadd5f3223d7bed2
mutation_budget: 5
mutation_tests: 108
mutation_failures: 64
mutation_matrix_pass: 44
mutation_messages_at_redelivery_6: 64
mutation_failures_all_redelivery_rows: yes
mutation_main_tree_untouched: yes
mutation_worktree_removed: yes
anchors_mirror: rows=218 bad=0
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
```

The redelivery count per row is 50 instead of 500; a budget of 5 still fails at redelivery 6, as before. The class time went from 22.037 s (E-05) to 2.909 s and its XML report from 36,151,467 bytes (179,491 lines, the figures of the review run) to 3,870,005 bytes (19,291 lines), about a tenth, as expected from the 10-fold smaller loop (the log lines scale with the call count). The loggers were not silenced: the cost is gone with the count, and a level change in the test would be global to the test JVM's loggers, where other tests read log output through `LogCapture`, so it was left alone as unnecessary. The method is now `retriableShapeIsStillRedeliveredAfterManyAttempts`, the Javadoc says a finite count shows no budget below it and not that none exists above it, and the test file no longer contains the words "without bound". The testcase names of the 64 redelivery rows changed with the method name, so the 108 expected rows for the gate are written anew in `47-rty01-expected-added-gap.tsv` (the 44 first-put rows are line for line those of `47-rty01-expected-added.tsv`, which stays committed and unedited and is no longer used by the gate). The mutation was re-run on the pin commit in the removed worktree `/tmp/gsd47/wt-mut` (a consecutive-failure counter of 5 in `ProxySinkTask.put`, two lines, never in the main tree): exactly the 64 redelivery rows failed, each with `redelivery 6 ==> Unexpected exception type thrown, expected: <RetriableException> but was: <RuntimeException>`, and the 44 first-put rows stayed green, as in E-05. The full-suite and anchors totals are in the gate entry after the last edit. No JVM crash occurred and `-PjavaVersion=21` was not used.

Recorded 2026-11-05T21:46:57Z.

Verdict: pass

### E-14: Gap closure 47-05, FORK-DELTA item 17 qualified (WR-03, WR-05) and brought to the 50-redelivery pin

Backs: RTY-01, 47-REVIEW.md WR-03 and WR-05, U-1. The edit corrects the text written in plan 47-02 (E-07), so item 17 now diverges from the plan-time wording; `47-doc-edits.json` holds no item-17 text and stays unedited.

```sh
python3 -I /tmp/gsd47/exec/g05/edit_fd17.py   # five old/new pairs on the item-17 line of docs/FORK-DELTA.md, each old text asserted to occur once
git diff --numstat docs/FORK-DELTA.md
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd47/exec/g05/msg6.txt
git show 7b348644^:docs/FORK-DELTA.md   # replay edit_fd17.py on the parent version and cmp with the committed file
python3 -I -c '<item-17 check: the classification sentences up to "A code listed in additionalRetriableErrorCodes" verbatim, every E-06 instant and gap, runtime version, 22 shapes, 16 retriable shapes, 64 and 44, no "500 consecutive", no "fails the task even under", E-13 cited>'
grep -cF 'FUT-05 is closed by decision' docs/FORK-DELTA.md   # and the stays-open, not-closed and (FUT-05) wordings, and the two pin class names
python3 -I .planning/phases/47-task-stop-retry-classification/47-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
```

```text
fork_delta_commit: 7b3486444a64b4fde4fd3f564590973a59812ab2
item17_lines_changed: 1
item17_replay_identical: yes
item17_prefix_verbatim: yes
item17_e06_numbers_kept: yes
item17_pin_count: 50
item17_500_left: no
item17_fails_task_claim_scoped: yes
item17_cadence_scoped_to_kafka_3_9: yes
fork_delta_open_fut05_lines: 0
fork_delta_closed_by_decision_count: 1
anchors_mirror: rows=218 bad=0
```

WR-03: the item now says the body of `deliverMessages` is identical in the sources of Kafka 3.9.1, 4.1.1 and 4.4.0 and that the code around it that sets the cadence (`iteration`, `poll`) was not compared; the 60 s default and the 7004, 7004 and 7006 ms gaps are stated for the Kafka 3.9 worker (`7.9.7-ccs`) only, with the remark that the interval is measured for the 3.9 line only; the absence of a count and a deadline is stated as read from the `deliverMessages` body, next to the three calls over 120 s that were measured (E-06: instants 506, 60042, 120055 and 449, 60012, 120013). WR-05: the sentence "Any other exception from `put` ... fails the task even under `errors.tolerance=all`" now reads as a statement about a task that throws from `put`, measured with the c2 probe task (a `DataException`, not this connector, E-06); the connector's own path is described next, labelled read from the code and not measured: all three delivery strategies call `ChunkFlusher.putDirect` (`DirectDeliveryStrategy.put`, `AtLeastOnceBufferStrategy` and `ExactlyOnceBufferStrategy` through `ChunkFlusher.flush`), which catches every exception from `ProxySinkTask.put`, runs `Utils.handleException` a second time, and under `all` sends the batch to the DLQ, so under `all` a `RetriableException` is the only exception `Utils.handleException` lets out, and a conversion `DataException` reaches the DLQ there while under `none` `putDirect` rethrows it. What the pin covers is stated without overclaiming: the 22-shape first-put matrix (44 rows) runs through `ProxySinkTask.put` only; through `ChunkFlusher.putDirect` only the 16 retriable shapes are pinned, in the 64 redelivery rows (32 of them through `putDirect`); the first-put outcomes of the non-retriable and conversion shapes through `putDirect` are stated as not pinned, which is the review's second option (no new rows were added, so the test and the 108 rows are unchanged by this edit). The pin's count is stated as 50 consecutive redeliveries and as a finite count (no budget below 50 exists, not that none exists above it), with the mutation citation extended to E-13. The classification sentences of the item before "A code listed in `additionalRetriableErrorCodes`" are verbatim as in E-07.

Recorded 2026-11-05T21:48:10Z.

Verdict: pass
