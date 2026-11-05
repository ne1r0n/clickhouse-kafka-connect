# Phase 46: Command evidence

- **Base commit:** `local_head` = `3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5` (HEAD when plan 46-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-05.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `46-evidence-check.py` is the read-only gate helper written at plan time (byte-identical to the Phase 34-45 copies); it is never edited.
- **No push happens in Phase 46.** Reads use `git ls-remote` and `gh api` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | TMO-01, TMO-02, TMO-03 | 46-01 | pass |
| E-02 | TMO-03 | 46-01 | pass |
| E-03 | TMO-02, TMO-03 | 46-01 | pending (plan 46-01) |
| E-04 | TMO-03, TMO-02 | 46-02 | pending (plan 46-02) |
| E-05 | TMO-03, TMO-02 | 46-02 | pending (plan 46-02) |
| E-06 | TMO-03, TMO-02 | 46-02 | pending (plan 46-02) |
| E-07 | TMO-03 | 46-03 | pending (plan 46-03) |
| E-08 | TMO-03 | 46-03 | pending (plan 46-03) |
| E-09 | TMO-03 | 46-03 | pending (plan 46-03) |
| E-10 | TMO-02 | 46-04 | pending (plan 46-04) |
| E-11 | TMO-03 | 46-04 | pending (plan 46-04) |
| E-12 | TMO-02, TMO-03 | 46-04 | pending (plan 46-04) |
| E-13 | TMO-01 | 46-05 | pending (plan 46-05) |
| E-14 | TMO-01, TMO-02, TMO-03 | 46-05 | pending (plan 46-05) |

### E-01: Pre-flight and baseline on the untouched base

Backs: TMO-01, TMO-02, TMO-03, ROADMAP Phase 46 phase gate (a green baseline on the untouched tip, so every later red is attributable to this phase).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
colima list
md5 -q .planning/phases/46-insert-timeout-limits/46-evidence-check.py
git ls-files --error-unmatch .planning/phases/46-insert-timeout-limits/46-evidence-check.py
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py anchors .planning/scripts/fork-anchors.txt
grep -rn 'async_insert' src/main | wc -l
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd46/exec/selftest.log 2>&1
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 > /tmp/gsd46/exec/tail-gate-base.log 2>&1
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd46/exec/testunit-base.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class InsertStallBoundPinUnitTest
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class InsertSocketTimeoutOptionUnitTest
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class RetriableExceptionPassThroughUnitTest
rm -rf /tmp/gsd46/exec/xml-base-testunit && cp -R build/test-results/testUnit /tmp/gsd46/exec/xml-base-testunit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd46/exec/suite-base.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed build/test-results/test
rm -rf /tmp/gsd46/exec/xml-base-suite && cp -R build/test-results/test /tmp/gsd46/exec/xml-base-suite
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
helper_md5: 7123d354f51d45e019837f43b3a30fbc
helper_tracked: yes
anchors_mirror: rows=200 bad=0
async_insert_main_lines: 2
selftest_last_line: selftest: no failures
selftest_duration: about 63 min (its ST4 step runs the cumulative gate)
base_tail_gate_line: invariants: provenance_paths=11 anchor_commits=51 anchor_rows=200 marker_commits=69 anchor_exempt=0 result=PASS
base_tail_gate_exit: 0
base_tail_gate_duration: 3 min 4 s
baseline_testunit_exit: 0
baseline_testunit_tests: 2086
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_testunit_files: 98
gg7_pins_green: InsertStallBoundPinUnitTest 15, InsertSocketTimeoutOptionUnitTest 23, RetriableExceptionPassThroughUnitTest 38, no failure
baseline_suite_exit: 0
baseline_suite_tests: 2689
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_files: 147
baseline_suite_duration: 172s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
baseline_java: default toolchain (semeru-17.0.9, OpenJ9)
jvm_crash: yes, first full-suite run only
baseline_suite_first_run_exit: 255
baseline_suite_first_run_tests: 2587 tests in 137 files before Gradle Test Executor 7 died with exit value 255
baseline_suite_first_run_cleanup: core.20261105.110341.98215.0001.dmp, javacore.20261105.110341.98215.0002.txt, Snap.20261105.110341.98215.0003.trc and jitdump.20261105.110341.98215.0004.dmp deleted by name
baseline_suite_rerun: the re-run above, same command, exit 0, no crash file
```

The baseline totals later plans compute expected counts from are testUnit 2086 tests (Phase 45 E-19 recorded the same) and full suite 2689 tests (Phase 45 E-18's 2675 plus E-19's 14 test-only cases). The first full-suite attempt ended in the known intermittent OpenJ9 crash (exit 255, 2587 of 2689 tests finished, no failure among them); the single re-run is the recorded baseline. The self-test ran concurrently with the testUnit and full-suite baselines; it reads git objects only. Every file under `src` equalled B0 while the testUnit and full-suite baselines ran; the tail gate and the self-test read commit objects only.

Recorded 2026-11-05T09:55:13Z.

Verdict: pass

### E-02: Raw-socket stub first slice: notReadingBody and the 64 MiB write-stall premise

Backs: TMO-03, ROADMAP Phase 46 note (extend `StubClickHouseServer` with the responders the pins need): the com.sun `HttpServer` reads the whole body before a responder runs, so a client body write can never block against it; the raw mode reads the request head only.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*StubClickHouseServerRawModeUnitTest*' --tests '*InsertStallBoundPinUnitTest*' --tests '*ClientErrorShapePinUnitTest*' --tests '*ClientEndpointAndOptionsPinUnitTest*' --tests '*WriterHelperClientClosingUnitTest*' --tests '*ClientInsertWirePinUnitTest*' > /tmp/gsd46/exec/raw-slice.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class StubClickHouseServerRawModeUnitTest
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit failures=0 errors=0
git worktree add --detach /tmp/gsd46/fmt 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/fmt --console=plain spotlessApply > /tmp/gsd46/exec/fmt-slice.log 2>&1
cmp /tmp/gsd46/fmt/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
cmp /tmp/gsd46/fmt/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
git worktree remove --force /tmp/gsd46/fmt && git worktree prune
git add src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
git commit -F /tmp/gsd46/exec/msg-slice.txt
git diff --name-only 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 HEAD -- src/main | wc -l
```

```text
raw_slice_exit: 0
raw_slice_tests: 2
raw_slice_total_tests: 54 across the six classes run, 0 failures, 0 errors, 0 skipped
http_mode_users_failed: 0
http_mode_users_run: InsertStallBoundPinUnitTest 15, ClientErrorShapePinUnitTest 7, ClientEndpointAndOptionsPinUnitTest 5, WriterHelperClientClosingUnitTest 23, ClientInsertWirePinUnitTest 2 (ClientErrorShapes is driven by ClientErrorShapePinUnitTest)
premise_blocked_write_bytes: 67108864
premise_observation_ms: 2000
premise_body_bytes_read: 0
premise_requests_recorded: 1
premise_release_bound_ms: 5000
premise_test_seconds: 2.005
stub_receive_buffer_bytes: 4096
fmt_stub_1: identical
fmt_raw_test_1: identical
raw_slice_commit: d1cbdff294b53aaccabff25d85f86718a1441643
raw_slice_main_diff: 0
```

`notReadingBodyBlocksA64MebibyteBodyWrite` shows on the OS that runs testUnit (macOS, Darwin 25.5.0) that a plain socket client writing a 64 MiB chunked body against `notReadingBody()` is still blocked after 2 s, with 0 body bytes read and exactly 1 recorded request, and that the blocked writer ends with an `IOException` within 5 s of `close()`. The raw slice run was first made on the same file contents before the E-01 commit and repeated after it; the figures above are the repeat. The per-file formatting check ran on a worktree at B0; Spotless reformatted other, pre-existing files there but neither new file changed.

Recorded 2026-11-05T09:57:00Z.

Verdict: pass
