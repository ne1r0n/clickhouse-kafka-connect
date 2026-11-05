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
| E-05 | RTY-01 | 47-02 | pending (plan 47-02) |
| E-06 | RTY-01 | 47-02 | pending (plan 47-02) |
| E-07 | RTY-01 | 47-02 | pending (plan 47-02) |
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
