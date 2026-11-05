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
| E-03 | TMO-02, TMO-03 | 46-01 | pass |
| E-04 | TMO-03, TMO-02 | 46-02 | pass |
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

### E-03: Raw responders complete, existing stub users green, A1 measured on Linux and macOS

Backs: TMO-02, TMO-03, 46-RESEARCH.md assumption A1 (Linux loopback buffers swallow no more than a fraction of the 64 MiB write-stall body) and Pitfalls 1 and 2 (a stub that streams progress headers, and a body that cannot fit in loopback buffers).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*StubClickHouseServerRawModeUnitTest*' --tests '*InsertStallBoundPinUnitTest*' --tests '*ClientErrorShapePinUnitTest*' --tests '*ClientEndpointAndOptionsPinUnitTest*' --tests '*WriterHelperClientClosingUnitTest*' --tests '*ClientInsertWirePinUnitTest*' > /tmp/gsd46/exec/raw-full.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class StubClickHouseServerRawModeUnitTest
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit failures=0 errors=0
python3 -I /tmp/gsd46/exec/a1_probe.py
colima ssh --profile vz -- python3 -I - < /tmp/gsd46/exec/a1_probe.py
git add src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
git commit -F /tmp/gsd46/exec/msg-full.txt
git rev-parse HEAD
git diff --name-only 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 9bd0944ebd066852ef059e7b2bca2a6ff2a54137 -- src/main | wc -l
```

```text
raw_full_exit: 0
raw_tests: 6
raw_total_tests: 58 across the six classes run, 0 failures, 0 errors, 0 skipped
http_mode_users_failed: 0
a1_macos_absorbed_bytes: 662096
a1_macos_kernel: Darwin 25.5.0
a1_macos_rcvbuf_effective: 326568
a1_linux_absorbed_bytes: 1245184
a1_linux_kernel: 6.8.0-50-generic
a1_linux_rcvbuf_effective: 8192
a1_linux_tcp_rmem: 4096 131072 6291456
a1_linux_tcp_wmem: 4096 16384 4194304
a1_tool: colima-python3
a1_runs: 4 runs on each OS (one plus three repeats), identical absorbed byte counts on every run
a1_pin_body_bytes: 67108864
a1_margin: 53.9
harness_tip: 9bd0944ebd066852ef059e7b2bca2a6ff2a54137
harness_main_diff: 0
fmt_stub_2: identical
fmt_raw_test_2: identical
```

Meaning of the A1 numbers for the 64 MiB body: with a 4096-byte receive buffer set before `listen` and a peer that reads only the request head, a writer pushed 662096 bytes (0.63 MiB) on macOS and 1245184 bytes (1.19 MiB) on the colima VM's Linux 6.8 kernel before its send timed out, so the 64 MiB pin body is 53.9 times the larger value and cannot be swallowed by loopback buffers on either OS. Measurement notes: the probe is a Python client and server (`/tmp/gsd46/exec/a1_probe.py`, scratch, never committed), not the Java stub; a send of 64 KiB pieces with a 2 s send timeout ends the run; on macOS the accepted socket reports a 326568-byte receive buffer, so the 4096 request is not honoured there and the Java premise test (`notReadingBodyBlocksA64MebibyteBodyWrite`, 2.016 s) is the macOS evidence; the Linux accepted socket reports 8192 (the kernel doubles 4096) and its send buffer autotuned to 2626560 bytes. The Java stub itself runs only on the machine that runs testUnit; the Linux figure shows that the same shape blocks long before 64 MiB on a Linux loopback. The per-file formatting check for the second commit reused the Spotless worktree run of the same file contents at B0.

Recorded 2026-11-05T09:58:00Z.

Verdict: pass

### E-04: V2 pins red on the unchanged production tree

Backs: TMO-03, TMO-02, ROADMAP Phase 46 criterion 2 (a red-first pin for the client V2 write-side stall) and criterion 3 (retriable under both tolerances, no DLQ record).

```sh
git worktree add --detach /tmp/gsd46/red 9bd0944ebd066852ef059e7b2bca2a6ff2a54137
git diff --quiet 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 9bd0944ebd066852ef059e7b2bca2a6ff2a54137 -- src/main
cp src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java /tmp/gsd46/red/src/test/java/com/clickhouse/kafka/connect/sink/
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/red --console=plain testUnit --rerun --tests '*InsertWriteStallBoundPinUnitTest*' > /tmp/gsd46/exec/red-v2.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit /tmp/gsd46/red/build/test-results/testUnit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed /tmp/gsd46/red/build/test-results/testUnit
git worktree remove --force /tmp/gsd46/red && git worktree prune
```

```text
v2_red_tree: 9bd0944ebd066852ef059e7b2bca2a6ff2a54137
v2_red_main_identical_to_base: yes
v2_red_exit: 1
v2_red_tests: 19
v2_red_failed_count: 14
v2_red_failed_methods: insertWatchdogIsADaemonThreadEndedByStop; progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound; slowV2UploadStillSendingAtTheBoundFailsAsRetriable; slowV2UploadThroughPutIsRetriableWithNoDlqRecord; writeStalledV2InsertEndsWithinTheBoundAsRetriable; writeStalledV2InsertThroughPutIsRetriableWithNoDlqRecord
v2_red_green_methods: lateFireAfterASuccessfulV2InsertIsHarmless; operatorShorterSocketTimeoutEndsAV2QuietStallThroughPut; operatorSocketTimeoutLeavesAWriteStallToTheOperator; progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent
v2_red_by_guard: yes
v2_red_watchdog_by_assertion: yes
v2_red_progress_case_seconds: 6.088
v2_red_operator_long_case_seconds: 3.011
worktree_removed: yes
```

The worktree's `src/main` is byte-identical to the base (`git diff --quiet` exit 0), so every red here is the unchanged production behaviour. The 13 guard cases (write stall x3 routes, write stall through put x6, slow upload x1, slow upload through put x2, progress while sending x1) each failed with `a client V2 insert must end within the bound ==> execution timed out after 10000 ms`, the `assertTimeoutPreemptively` guard: the insert was still blocked after 10 s with a 1000 ms insert timeout and a 64 MiB body. The watchdog case failed on `one armed insert creates exactly one watchdog thread ==> expected: <1> but was: <0>` (no watchdog exists). The five recorded or operator-owned cases were green: the progress-after-sent case ran 6.088 s past its 1500 ms bound (the recorded limit, body already sent), the operator's longer `socket_timeout` left the write stall running for the 3 s observation with no connector bound in the chain, the operator's shorter `socket_timeout` ended a quiet stall through put under both tolerances, and the late-fire pair of inserts passed.

Recorded 2026-11-05T10:07:00Z.

Verdict: pass
