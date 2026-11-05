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
| E-08 | RTY-02 | 47-03 | pending (plan 47-03) |
| E-09 | LIF-02 | 47-03 | pending (plan 47-03) |
| E-10 | LIF-01, LIF-02, RTY-01, RTY-02 | 47-04 | pending (plan 47-04) |
| E-11 | LIF-02, RTY-02 | 47-04 | pending (plan 47-04) |

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
