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
| E-05 | TMO-03, TMO-02 | 46-02 | pass |
| E-06 | TMO-03, TMO-02 | 46-02 | pass |
| E-07 | TMO-03 | 46-03 | pass |
| E-08 | TMO-03 | 46-03 | pass |
| E-09 | TMO-03 | 46-03 | pass |
| E-10 | TMO-02 | 46-04 | pass |
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

### E-05: V2 write-phase deadline green in one commit, operator gate, anchors

Backs: TMO-03, TMO-02 (U-1 recorded side), ROADMAP Phase 46 criteria 2 and 3 for the client V2 write-side stall, and U-3 (the operator's `socket_timeout` wins).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*InsertWriteStallBoundPinUnitTest*' --tests '*InsertBoundGateUnitTest*' > /tmp/gsd46/exec/green-v2.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*InsertStallBoundPinUnitTest*' --tests '*InsertSocketTimeoutOptionUnitTest*' --tests '*RetriableExceptionPassThroughUnitTest*' --tests '*WriterHelperClientClosingUnitTest*' --tests '*ClientInsertWirePinUnitTest*' --tests '*ClientEndpointAndOptionsPinUnitTest*' --tests '*ClientErrorShapePinUnitTest*' --tests '*WriterFailureClassificationUnitTest*' --tests '*SourcePlanningIdGuardUnitTest*' --tests '*StubClickHouseServerRawModeUnitTest*' > /tmp/gsd46/exec/neighbours-v2.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit --class InsertStallBoundPinUnitTest
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 208
grep -cF 'chc.setInsertSocketTimeoutV2(insertSettings, csc.getClickhouseClientInsertTimeoutMs());' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
git show 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5:src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java | grep -cF 'awaitInsertResponse('
grep -cF 'awaitInsertResponse(' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
git worktree add --detach /tmp/gsd46/fmt HEAD && cp <five files> /tmp/gsd46/fmt/... && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/fmt --console=plain spotlessApply && cmp <five files>
git add <six files> && git commit -F /tmp/gsd46/exec/msg-p2-fix.txt
```

```text
v2_green_exit: 0
v2_green_failed_count: 0
v2_pin_tests: 19
v2_gate_unit_tests: 20
v2_new_tests: 39
v2_write_stall_case_seconds_max: 1.082
v2_slow_upload_case_seconds: 1.023
v2_progress_while_sending_case_seconds: 1.013
v2_progress_case_seconds: 6.085
v2_operator_long_case_seconds: 3.026
v2_neighbours_exit: 0
v2_neighbours_tests: 353
v2_neighbours_failed_count: 0
gg7_pins_after: InsertStallBoundPinUnitTest 15, InsertSocketTimeoutOptionUnitTest 23, RetriableExceptionPassThroughUnitTest 38, no failure
anchors_mirror: rows=208 bad=0
insert_socket_timeout_sites: 3
await_insert_response_count_before: 4
await_insert_response_count_after: 2
await_insert_response_body_unchanged: yes
operator_set_socket_timeout_lines: 4
fmt_writer: preexisting
fmt_helper: preexisting
fmt_fixtures: identical
fmt_stall_pin: identical
fmt_gate: identical
v2_fix_commit: b53cd23c7114e9130ceeb4a552f2d87b1c50ca39
v2_fix_commit_files: 6
```

The green runs passed on the first attempt after the code was written. Case times through the real writer and a raw loopback stub, wall clock from the call: the write stall (three routes, direct and through put) ended in 1.01 to 1.08 s against the 1000 ms bound (red on the unchanged tree: still blocked after 10 s); the slow upload cut at 1.023 s with body bytes already read; the progress-while-sending case 1.013 s; the progress-after-sent case ran 6.085 s against a 1500 ms bound (the recorded U-1 limit, body already sent, unchanged); the operator's longer `socket_timeout` left the write stall running for the whole 3 s observation. The fix commit holds exactly `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, the fixtures, the V2 pin class, the gate unit class and `fork-anchors.txt`. The anchor counts measured with `grep -cF` before the rows were written: `insertWriteDeadline(client, queryId)` 3, `insertWithWriteDeadline(` 4, `deadline.disarm();` 4, `client::cancelTransportRequest` 1, `insertWatchdog.shutdownNow()` 1, `setRemoveOnCancelPolicy(true)` 1, `INSERT_BODY_DEADLINE_FORMAT` 2, `insertBoundMs(` declaration 1, and the operator row 3 to 4. The `awaitInsertResponse` method text from its Javadoc to its closing brace equals the base's (python extract of both versions). The per-file formatting check on a worktree at the pre-commit HEAD: the three test files were identical after `spotlessApply`; `ClickHouseWriter.java` and `ClickHouseHelperClient.java` differ only in import ordering, which also differs when the unchanged HEAD copies are formatted, so the lines this change added and edited are not reformatted (`preexisting`).

Recorded 2026-11-05T10:11:00Z.

Verdict: pass

### E-06: V2 bite and gate: four mutations, watchdog queue probe, testUnit, scope, anchors, tail gate

Backs: TMO-03, TMO-02, U-1 and U-3 (each check is shown able to fail), ROADMAP Phase 46 phase gate (cache-busted `testUnit`, anchors invariant, the unedited tail gate).

```sh
git worktree add --detach /tmp/gsd46/mut HEAD   # once per mutation D1..D4, python3 -I replacement asserting the old text occurs once, then removed
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/mut --console=plain testUnit --rerun --tests '*InsertWriteStallBoundPinUnitTest*' --tests '*InsertBoundGateUnitTest*' > /tmp/gsd46/exec/mut-<n>.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed /tmp/gsd46/exec/xml-mut-<n>
git worktree add --detach /tmp/gsd46/probe-queue HEAD && cp /tmp/gsd46/scripts/ProbeInsertWatchdogQueueMeasurementTest.java /tmp/gsd46/probe-queue/src/test/java/com/clickhouse/kafka/connect/sink/
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/probe-queue --console=plain testUnit --rerun --tests '*ProbeInsertWatchdogQueueMeasurementTest*' > /tmp/gsd46/exec/probe-queue.log 2>&1
git worktree remove --force /tmp/gsd46/probe-queue && git worktree prune
git log --all --format=%H -- '*ProbeInsertWatchdogQueueMeasurementTest.java'
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd46/exec/testunit-v2.log 2>&1
rm -rf /tmp/gsd46/exec/xml-02-testunit && cp -R build/test-results/testUnit /tmp/gsd46/exec/xml-02-testunit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit /tmp/gsd46/exec/xml-02-testunit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed /tmp/gsd46/exec/xml-02-testunit
git diff --name-only 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 HEAD -- src/main
grep -rn 'async_insert' src/main | wc -l
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py anchors .planning/scripts/fork-anchors.txt
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 2776ae08c263d4b4a413c3b9101391c0d03c8e72 > /tmp/gsd46/exec/tail-gate-02.log 2>&1
```

```text
d1_target: in InsertWriteDeadline.disarm(), the three body lines `ScheduledFuture<?> task = scheduled; if (task != null) { task.cancel(false); }` removed, leaving the method empty
d1_failed: progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent
d1_failed_count: 1
d1_expected_met: yes
d2_target: in InsertWriteDeadline.arm(), the statement `scheduled = watchdog.schedule(() -> { fired.set(true); try { canceller.accept(queryId); } catch (RuntimeException e) { LOGGER.warn(...); } }, boundMs, TimeUnit.MILLISECONDS);` removed
d2_failed_count: 14
d2_failed: insertWatchdogIsADaemonThreadEndedByStop x1; progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound x1; slowV2UploadStillSendingAtTheBoundFailsAsRetriable x1; slowV2UploadThroughPutIsRetriableWithNoDlqRecord x2; writeStalledV2InsertEndsWithinTheBoundAsRetriable x3; writeStalledV2InsertThroughPutIsRetriableWithNoDlqRecord x6
d2_expected_met: yes
d3_target: in ClickHouseHelperClient.insertBoundMs, `return operatorSetSocketTimeout || timeoutMs <= 0 ? 0L : timeoutMs;` replaced by `return timeoutMs <= 0 ? 0L : timeoutMs;`
d3_failed_count: 9
d3_failed: noConnectorBoundWhenTheOperatorNamedSocketTimeout x8; operatorSocketTimeoutLeavesAWriteStallToTheOperator x1
d3_expected_met: yes
d4_target: in insertWithWriteDeadline, the statement `deadline.throwIfFired(e);` removed
d4_failed_count: 13
d4_failed: progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound x1; slowV2UploadStillSendingAtTheBoundFailsAsRetriable x1; slowV2UploadThroughPutIsRetriableWithNoDlqRecord x2; writeStalledV2InsertEndsWithinTheBoundAsRetriable x3; writeStalledV2InsertThroughPutIsRetriableWithNoDlqRecord x6
d4_expected_met: yes
watchdog_queue_after_100_inserts: 0
watchdog_pool_size: 1
watchdog_terminated_after_stop: true
probe_committed: no
worktrees_removed: yes
testunit_exit: 0
testunit_tests: 2131
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_files: 101
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
async_insert_main_lines: 2
anchors_mirror: rows=208 bad=0
tail_gate_head: 2776ae08c263d4b4a413c3b9101391c0d03c8e72
tail_gate_line: invariants: provenance_paths=11 anchor_commits=60 anchor_rows=208 marker_commits=78 anchor_exempt=0 result=PASS
tail_gate_exit: 0
tail_gate_duration: 3 min 22 s
```

Mutation readings. D1 (the deadline is never disarmed, so it covers the whole insert): the only red is the case that pins the recorded limit, `progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent`, which is cut at its 1500 ms bound; the disarm after the body closes is what keeps U-1's behaviour. D2 (never armed): the 13 guard cases (write stall x9 across direct and put, slow upload x3, progress while sending x1) are red by the preemptive guard, plus the watchdog case, because a thread is only started when the first task is scheduled. D3 (operator gate ignored): the eight gate-unit cases with an operator `socket_timeout` and the long-operator write-stall case are red, so the operator's `socket_timeout` keeps ownership of the transport limits only through `insertBoundMs`. D4 (fired deadline not converted): the 13 guard cases are red, including the six write-stall and two slow-upload put cases, because the cancelled request then surfaces as a non-retriable transport failure instead of a `RetriableException` (read from the XML: under `errors.tolerance=none` the put throws a `RuntimeException`, under `all` nothing is thrown because the batch goes to the DLQ).

Watchdog queue probe (a throwaway test in a detached worktree, never committed: `git log --all` finds no commit touching it): after 100 successful small V2 inserts on one writer against the stub, with a 60000 ms insert timeout, the writer's `insertWatchdog` read by reflection had `queue_size=0` and `pool_size=1`; after `writer.stop()`, `awaitTermination(5 s)` returned true.

The cache-busted `testUnit` ran 2131 tests (2086 baseline + 6 raw-stub premise cases + 39 new) with 0 failures, 0 errors and 2 skipped across 101 files, re-checked against the copied XML `xml-02-testunit`. The unedited tail gate over `M..2776ae08` printed `result=PASS` with `anchor_rows=208`. `src/main` differs from the base only in the writer and the helper client; no new `src/main` file exists; the `async_insert` lines in `src/main` still count 2.

Recorded 2026-11-05T10:25:00Z.

Verdict: pass

### E-07: V1 pipe measured and V1 pins red on the unchanged production tree

Backs: TMO-03, ROADMAP Phase 46 criterion 2 for the client V1 insert sites (a red-first pin on the unchanged tree) and plan-check B2a (the V1 body write is measured before any claim that the bound covers it).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/probe-v1pipe -PjavaVersion=21 --console=plain testUnit --rerun --tests '*ProbeV1PipeMeasurementTest*' > /tmp/gsd46/exec/probe-v1pipe-base.log 2>&1
git worktree remove --force /tmp/gsd46/probe-v1pipe && git worktree prune
git worktree add --detach /tmp/gsd46/red 9bd0944ebd066852ef059e7b2bca2a6ff2a54137
git diff --quiet 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 9bd0944ebd066852ef059e7b2bca2a6ff2a54137 -- src/main
cp src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java /tmp/gsd46/red/src/test/java/com/clickhouse/kafka/connect/sink/
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/red --console=plain testUnit --rerun --tests '*InsertV1WaitBoundPinUnitTest*' > /tmp/gsd46/exec/red-v1.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit /tmp/gsd46/red/build/test-results/testUnit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed /tmp/gsd46/red/build/test-results/testUnit
git worktree remove --force /tmp/gsd46/red && git worktree prune
```

```text
v1_pipe_impl: com.clickhouse.data.stream.NonBlockingPipedOutputStream (deprecated class), chosen by ClickHouseDataStreamFactory.createPipedOutputStream when use_blocking_queue is false (the default) and response buffering is not PERFORMANCE (default RESOURCE_EFFICIENT); BlockingPipedOutputStream is used only when use_blocking_queue is true; read from the clickhouse-data 0.11.0 sources
v1_pipe_capacity_source: max_queued_buffers (default 512) buffers of write_buffer_size bytes (the connector sets 8192 in getMutationRequest), with a linear dynamic capacity policy up to the maximum (buffer_queue_variation default 100); read from the clickhouse-data and clickhouse-client 0.11.0 sources
v1_pipe_capacity_bytes: 4194304
v1_pipe_write_timeout_source: a write that finds the queue full spins on offer and throws IOException "Write timed out after <ms> ms" once the wait reaches the stream's timeout, which the factory sets to config.getReadTimeout(), that is socket_timeout, default 30000 ms (an operator socket_timeout changes it); read from the clickhouse-data 0.11.0 sources
v1_pipe_16mib_blocked_in: future-get
v1_pipe_64mib_blocked_in: future-get
v1_pipe_256mib_blocked_in: future-get
v1_pipe_256mib_elapsed_ms: >60000
v1_pipe_256mib_outcome: still-running
v1_pipe_write_stall_seen: no
v1_pipe_write_stall_size_mib: none
v1_pin_body_mib: 64
v1_pipe_probe_jvm: -PjavaVersion=21, testUnit maxHeapSize 3g in the scratch worktree only
v1_red_tree: 9bd0944ebd066852ef059e7b2bca2a6ff2a54137
v1_red_main_identical_to_base: yes
v1_red_exit: 1
v1_red_tests: 21
v1_red_failed_count: 18
v1_red_failed_methods: boundedV1InsertThroughPutIsRetriableWithNoDlqRecord; progressStreamingV1InsertEndsWithinTheBoundAsRetriable; quietV1InsertEndsWithinTheBoundAsRetriable; timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite; writeStalledV1InsertEndsWithinTheBoundAsRetriable
v1_red_green_methods: operatorShorterSocketTimeoutEndsAV1QuietStallThroughPut; operatorSocketTimeoutLeavesTheV1WaitToTheOperator
v1_red_by_guard: yes
v1_red_guard_split: 15 cases failed at the 10000 ms guard (direct write stall x3, direct quiet stall x3, put x8 of the write-stall and quiet shapes, release x1) and 3 at the 5000 ms guard (direct progress x1, put progress x2)
v1_red_operator_long_case_seconds: 3.047
worktrees_removed: yes
```

```probev1pipe
PROBEV1PIPE tree=base size_mib=16 blocked_in=future-get elapsed_ms=>60000 outcome=still-running message_prefix=
PROBEV1PIPE tree=base size_mib=64 blocked_in=future-get elapsed_ms=>60000 outcome=still-running message_prefix=
PROBEV1PIPE tree=base size_mib=256 blocked_in=future-get elapsed_ms=>60000 outcome=still-running message_prefix=
```

The three `PROBEV1PIPE` lines are the unchanged tree (R0, `src/main` identical to the base) with the real V1 client against `notReadingBody()`, JSON route, insert timeout 1000 (which the unchanged tree ignores on V1): at 16, 64 and 256 MiB the SinkTask thread was in a `CompletableFuture` wait at the 3 s sample, never in the pipe write, and at 60 s the call had still not ended, so the library's 30 s default does not end a V1 write stall at any of the three sizes. By the code the pipe holds at most 512 buffers of 8192 bytes (4 MiB) and a blocked write ends with "Write timed out" after the socket timeout, but no size up to 256 MiB reached that state, so the worker thread evidently drains the pipe as fast as the connector fills it; where those bytes go was not determined here and is not needed by the pin. Consequence: no pipe-write stall was seen up to 256 MiB, the V1 pin body is the 64 MiB the plan names (the 64 MiB body reached the future wait), and plan-check B2a's branch for a stall inside the body write is "not applicable" unless Task 3 sees otherwise on the fixed tree. The 21 V1 cases at R0: the 18 bounded and release cases were red by the `assertTimeoutPreemptively` guard (message `a client V1 insert must end within the bound ==> execution timed out after 10000 ms`, or 5000 ms for the progress shape), and the three operator cases (the longer value left the write stall running for the 3 s observation, 3.047 s; the shorter value ended a quiet stall through put under both tolerances) were green. The probe was a throwaway test in a detached worktree and was never committed.

Recorded 2026-11-05T10:40:00Z.

Verdict: pass

### E-08: V1 wait bound green in one commit, operator gate, release, anchors

Backs: TMO-03, ROADMAP Phase 46 criteria 2 and 3 for the client V1 `future.get()` insert sites, and U-2 and U-3 (the V1 wait is bounded, the operator's `socket_timeout` wins).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*InsertV1WaitBoundPinUnitTest*' > /tmp/gsd46/exec/green-v1.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*InsertWriteStallBoundPinUnitTest*' --tests '*InsertBoundGateUnitTest*' --tests '*InsertStallBoundPinUnitTest*' --tests '*InsertSocketTimeoutOptionUnitTest*' --tests '*RetriableExceptionPassThroughUnitTest*' --tests '*WriterHelperClientClosingUnitTest*' --tests '*ClientInsertWirePinUnitTest*' --tests '*WriterFailureClassificationUnitTest*' --tests '*SourcePlanningIdGuardUnitTest*' > /tmp/gsd46/exec/neighbours-v1.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit build/test-results/testUnit
grep -cF 'try (ClickHouseResponse response = future.get())' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF 'future.get()' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 212
git worktree add --detach /tmp/gsd46/fmt HEAD && cp <the writer and the pin class> /tmp/gsd46/fmt/... && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/fmt --console=plain spotlessApply && cmp <the pin class> && git worktree remove --force /tmp/gsd46/fmt
git add src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java .planning/scripts/fork-anchors.txt && git commit -F /tmp/gsd46/exec/msg-p3-fix.txt
```

```text
v1_green_exit: 0
v1_green_failed_count: 0
v1_pin_tests: 21
v1_write_stall_case_seconds_max: 1.218
v1_progress_case_seconds: 1.51
v1_quiet_case_seconds_max: 0.325
v1_operator_long_case_seconds: 3.024
v1_release_case_seconds: 1.34
v1_neighbours_exit: 0
v1_neighbours_tests: 374
v1_neighbours_failed_count: 0
bare_v1_wait_lines: 0
bare_future_get_count: 1
anchors_mirror: rows=212 bad=0
v1_site_calls: 3
v1_message_format: ClickHouse client V1 insert response not received within clickhouseClientInsertTimeoutMs=%d ms (QueryId: [%s])
fmt_writer_v1: preexisting
fmt_p1: identical
v1_fix_commit: 05b46d18779d0981cf77e234c74df87062829450
v1_fix_commit_files: 3
```

The green run passed on the first attempt after the code was written. Case times, wall clock through the real writer and a raw loopback stub, against the unchanged tree where the same cases were red by the preemptive guard (E-07): the V1 write stall ended in 1.137 to 1.218 s against the 1000 ms bound on three routes, direct and through put under both tolerances; the quiet stall in 0.313 to 0.325 s against 300 ms (the unchanged tree waited for the library's 30 s limit and was cut by the 10 s guard); the progress-streaming insert in 1.51 s against 1500 ms (the unchanged tree ran to about 6 s and was cut by the 5 s guard); the operator's longer `socket_timeout` left the write stall running for the whole 3 s observation (3.024 s) and its chain holds no `RetriableException` once the stub is closed; the operator's shorter value ended a quiet stall through put in 0.22 to 0.73 s as a retriable failure under both tolerances with an empty DLQ. The release case found no live thread in a socket write under a `com.clickhouse.` frame (the stub's own threads aside) within the 1.34 s the case took (the insert itself ended at about 1.2 s, so within about 0.15 s of the failure), while the stub still held the connection. The fix adds `awaitInsertResponseV1` (bounded `get` from the same `insertBoundMs` gate as the V2 deadline; the one remaining unbounded `future.get()` sits in its gated branch) and the three sites call it; the piped stream, `stream.close()`, the client block, the log lines and the statistics call are unchanged. The fix commit holds exactly `ClickHouseWriter.java`, the V1 pin class and `fork-anchors.txt`; `ClickHouseHelperClient.java` is untouched in it. The anchor counts measured with `grep -cF` before the rows were written: the old try line 0 (3 before), `awaitInsertResponseV1(` 4, `chc.insertBoundMs(` 2, `V1_INSERT_TIMEOUT_FORMAT` 2. The per-file formatting check on a worktree at the pre-commit HEAD: the pin class was identical after `spotlessApply`; `ClickHouseWriter.java` differs only in import ordering, which the unchanged HEAD copy shows as well (the text from the class declaration on was compared and is equal), so the lines this change added are not reformatted (`preexisting`).

Recorded 2026-11-05T10:43:00Z.

Verdict: pass

### E-09: V1 pipe-write stall on the fixed tree, bite and gate: three mutations, testUnit, scope, anchors, tail gate

Backs: TMO-03, ROADMAP Phase 46 criterion 2 for client V1 (the checks are shown able to fail; the body write is measured on the fixed tree) and the phase gate (cache-busted `testUnit`, anchors invariant, the unedited tail gate), plan-check B2a.

```sh
git worktree add --detach /tmp/gsd46/probe-v1pipe-fixed 462611cb61d545fd6dfc76872b5d6156aa92d0a5   # scratch heap edit, probe copied in, never committed
PROBE_TREE=fixed PROBE_SIZES=256 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/probe-v1pipe-fixed -PjavaVersion=21 --console=plain testUnit --rerun --tests '*ProbeV1PipeMeasurementTest*' > /tmp/gsd46/exec/probe-v1pipe-fixed.log 2>&1
git worktree remove --force /tmp/gsd46/probe-v1pipe-fixed && git worktree prune
git worktree add --detach /tmp/gsd46/mut 462611cb   # once per mutation V1a..V1c, python3 -I replacement asserting the old text occurs once, then removed (/tmp/gsd46/scripts/run-v1-mutations.sh)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/mut --console=plain testUnit --rerun --tests '*InsertV1WaitBoundPinUnitTest*' > /tmp/gsd46/exec/mut-v1<x>.log 2>&1
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit /tmp/gsd46/exec/xml-mut-v1<x>
git worktree add --detach /tmp/gsd46/probe-detector 462611cb   # throwaway release-detector probe, JDK 21 and the default toolchain, never committed
git log --all --format=%H -- '*ProbeV1PipeMeasurementTest.java' '*ProbeV1DetectorMeasurementTest.java'
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd46/exec/testunit-v1.log 2>&1
rm -rf /tmp/gsd46/exec/xml-03-testunit && cp -R build/test-results/testUnit /tmp/gsd46/exec/xml-03-testunit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py junit /tmp/gsd46/exec/xml-03-testunit
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py failed /tmp/gsd46/exec/xml-03-testunit
git diff --name-only 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5 HEAD -- src/main
grep -rn 'async_insert' src/main | wc -l
python3 -I .planning/phases/46-insert-timeout-limits/46-evidence-check.py anchors .planning/scripts/fork-anchors.txt
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 462611cb61d545fd6dfc76872b5d6156aa92d0a5 > /tmp/gsd46/exec/tail-gate-03.log 2>&1
```

```text
v1_256mib_fixed_elapsed_ms: 1530
v1_256mib_fixed_outcome: org.apache.kafka.connect.errors.RetriableException
v1_256mib_fixed_blocked_in: ended-before-3s
v1_pipe_write_stall_fixed_elapsed_ms: not applicable
v1_pipe_write_stall_fixed_outcome: not applicable
v1_bound_ends_pipe_write_stall: not applicable
v1a_target: in awaitInsertResponseV1, `future.get(bound, TimeUnit.MILLISECONDS)` replaced by `future.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS)`
v1a_failed_count: 18
v1a_failed: boundedV1InsertThroughPutIsRetriableWithNoDlqRecord x10; progressStreamingV1InsertEndsWithinTheBoundAsRetriable x1; quietV1InsertEndsWithinTheBoundAsRetriable x3; timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite x1; writeStalledV1InsertEndsWithinTheBoundAsRetriable x3
v1a_expected_met: yes
v1b_target: in ClickHouseHelperClient.insertBoundMs, `return operatorSetSocketTimeout || timeoutMs <= 0 ? 0L : timeoutMs;` replaced by `return timeoutMs <= 0 ? 0L : timeoutMs;`
v1b_failed_count: 1
v1b_failed: operatorSocketTimeoutLeavesTheV1WaitToTheOperator x1
v1b_expected_met: yes
v1c_target: in awaitInsertResponseV1, `throw new RetriableException(String.format(V1_INSERT_TIMEOUT_FORMAT` replaced by `throw new RuntimeException(String.format(V1_INSERT_TIMEOUT_FORMAT`
v1c_failed_count: 18
v1c_failed: boundedV1InsertThroughPutIsRetriableWithNoDlqRecord x10; progressStreamingV1InsertEndsWithinTheBoundAsRetriable x1; quietV1InsertEndsWithinTheBoundAsRetriable x3; timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite x1; writeStalledV1InsertEndsWithinTheBoundAsRetriable x3
v1c_expected_met: yes
release_detector_blocked_writers_during_stall: 1 (ClickHouseWorker-1), on JDK 21 and on the default toolchain
worktrees_removed: yes
probe_committed: no
testunit_exit: 0
testunit_tests: 2152
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_files: 102
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
async_insert_main_lines: 2
anchors_mirror: rows=212 bad=0
tail_gate_head: 462611cb61d545fd6dfc76872b5d6156aa92d0a5
tail_gate_line: invariants: provenance_paths=11 anchor_commits=65 anchor_rows=212 marker_commits=83 anchor_exempt=0 result=PASS
tail_gate_exit: 0
tail_gate_duration: 3 min 45 s
```

```probev1pipe
PROBEV1PIPE tree=fixed size_mib=256 blocked_in=ended-before-3s elapsed_ms=1530 outcome=org.apache.kafka.connect.errors.RetriableException message_prefix=ClickHouse client V1 insert response not received within cli
```

Pipe-write stall on the fixed tree (plan-check B2a). E-07 saw no pipe-write stall at 16, 64 or 256 MiB, so the stall-size run does not apply and `v1_bound_ends_pipe_write_stall` reads `not applicable`: there is no measured stall inside the V1 body write for the bound to end or fail to end, and no code was added for one. The 256 MiB run on the fixed tree against `notReadingBody()` (insert timeout 1000) ended before the 3 s sample with a `RetriableException` carrying the V1 message, 1530 ms after the call started: about half a second to write 256 MiB into the pipe on the SinkTask thread, then the 1000 ms bound on the response wait. A first run of the same probe printed `elapsed_ms=3007`; that figure included the probe's own 3 s sampling sleep (the thread had already ended), so the probe was changed to read the thread's own end time and re-run; the line above is the re-run. The residual that remains is the one the code reading gives: a body larger than the pipe, with a consumer that stops draining, would block the SinkTask thread in the pipe write until the stream's write timeout (the socket timeout, 30000 ms by default) raises "Write timed out", which the connector already classifies as retriable; it was not reached at any size up to 256 MiB and is recorded here as not measured, not as bounded.

Mutation readings. V1a (the wait never times out): the 18 bounded and release cases are red by the preemptive guard (10000 ms for the write-stall and quiet shapes, 5000 ms for progress), and the three operator cases stay green, so the guard cases bite on the bounded wait. V1b (operator gate ignored): only `operatorSocketTimeoutLeavesTheV1WaitToTheOperator` is red (the connector bound fires inside the operator's longer `socket_timeout`), and the shorter-operator put cases stay green because the library's own read timeout still ends them first, so the V1 operator ownership lives in `insertBoundMs` alone. V1c (the timeout is not converted): 18 red, the 10 put-path cases (under `none` the put throws a non-retriable exception, under `all` nothing retriable surfaces) and the 8 direct and release cases that look for a `RetriableException` in the chain. The release pin's detector was checked against a live stall in a throwaway probe: with the operator's `socket_timeout=60000` (no connector bound) and a 64 MiB write stall, the probe found exactly one client thread blocked in a socket write at 3 s (`ClickHouseWorker-1`), on JDK 21 and on the default toolchain, so the release case is not vacuous on the gate JVM.

The cache-busted `testUnit` ran 2152 tests (2086 baseline + 6 raw-stub premise cases + 39 V2 cases + 21 V1 cases) with 0 failures, 0 errors and 2 skipped across 102 files, re-checked against the copied XML `xml-03-testunit` (the V1 class 21 of 21, the V2 write-stall class 19 of 19). The unedited tail gate over `M..462611cb` printed `result=PASS` with `anchor_rows=212`. `src/main` differs from the base only in the writer and the helper client; no new `src/main` file exists; the `async_insert` lines in `src/main` still count 2; no probe was committed.

Recorded 2026-11-05T10:50:00Z.

Verdict: pass

### E-10: Live TMO-02 re-measure, large body, wire, and timed-out insert rows on ClickHouse latest (base and fixed tree)

Backs: TMO-02 (U-1: the progress-header insert is re-measured against a real server and recorded with its number), TMO-03 (the V1 bound against the real server), ROADMAP Phase 46 criterion 1, and plan-check B2b (the progress-while-sending shape is measured before any claim about it).

```sh
git worktree add --detach /tmp/gsd46/probe-base 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5
git worktree add --detach /tmp/gsd46/probe-fixed cc3c9b55dfdcfc52ab779f6bcdf8578f436b7e7f
# scratch only in both worktrees: maxHeapSize = "3g" added to the test task of build.gradle.kts; the probe file written with the Write tool
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/probe-base --console=plain test --rerun --tests '*ProbeInsertTimeoutLiveMeasurementTest*' > /tmp/gsd46/exec/probe-live-base.log 2>&1
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd46/probe-fixed --console=plain test --rerun --tests '*ProbeInsertTimeoutLiveMeasurementTest*' > /tmp/gsd46/exec/probe-live-fixed.log 2>&1
grep -h '^PROBE46 case=\|^PROBE46WIRE ' <the two TEST-*.xml> > /tmp/gsd46/exec/probe-live-lines.txt
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git worktree remove --force /tmp/gsd46/probe-base && git worktree remove --force /tmp/gsd46/probe-fixed && git worktree prune
git log --all --format=%H -- '*ProbeInsertTimeoutLiveMeasurementTest.java'
```

```text
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
probe_base_tree: 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5
probe_fixed_tree: cc3c9b55dfdcfc52ab779f6bcdf8578f436b7e7f
probe_exit_base: 0
probe_exit_fixed: 0
probe_transport: a Testcontainers port forward through colima profile vz to the container's mapped HTTP port
probe_jvm: default toolchain, maxHeapSize 3g in the scratch worktrees only
probe_row_count_instrument: ClickHouseTestHelpers.countRows, which issues OPTIMIZE TABLE before its SELECT count(), read right after the call returned and again once 15 s had passed since the call ended
probe_retriable_instrument: yes when a RetriableException is in the cause chain, or when Utils.handleException, called with the connector's own tolerance=none, retry-on-socket and additional-code settings over a RuntimeException wrapper like Processing's, throws a RetriableException
v2_progress_escape_ms_base: 8315
v2_progress_escape_ms_fixed: 8299
v2_progress_control_ms: 8059
v2_default_async_insert_bounded_ms: 3050
v2_default_async_insert_retriable: yes
v2_default_async_insert_root: java.net.SocketTimeoutException
v2_default_async_insert_rows_after_15s: 4000
v1_progress_ms_base: 8141
v1_progress_ms_fixed: 3034
v1_progress_fixed_retriable: yes
v1_memory_rows_at_timeout: 0
v1_memory_rows_after_15s: 4000
v1_mergetree_rows_base: 4000
v1_mergetree_rows_at_timeout: 2500
v1_mergetree_rows_after_15s: 4000
v2_large_body_bytes: 65610890
v2_large_body_ms_base: 8324
v2_large_body_outcome_base: ok
v2_large_body_ms_fixed: 8380
v2_large_body_outcome_fixed: ok
wire_body_bytes: 65610890
wire_first_progress_ms: 64
wire_body_sent_ms: 2305
wire_total_ms: 8316
deadline_leaves_progress_alone: yes
v2_large_body_cut_by_deadline: no
wire_progress_while_writing: yes
worktrees_removed: yes
probe_committed: no
```

```probe46
PROBE46 case=l1 tree=base client=V2 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=8315 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l2 tree=base client=V2 body_bytes=105780 insert_timeout_ms=600000 elapsed_ms=8072 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l3 tree=base client=V2 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=3060 outcome=com.clickhouse.client.api.DataTransferException retriable_in_chain=yes root=java.net.SocketTimeoutException rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l4 tree=base client=V1 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=8141 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l5 tree=base client=V1 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=8088 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l6 tree=base client=V2 body_bytes=65610890 insert_timeout_ms=3000 elapsed_ms=8324 outcome=ok retriable_in_chain=no root=none rows_at_end=-1 rows_after_15s=-1
PROBE46 case=l1 tree=fixed client=V2 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=8299 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l2 tree=fixed client=V2 body_bytes=105780 insert_timeout_ms=600000 elapsed_ms=8059 outcome=ok retriable_in_chain=no root=none rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l3 tree=fixed client=V2 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=3050 outcome=com.clickhouse.client.api.DataTransferException retriable_in_chain=yes root=java.net.SocketTimeoutException rows_at_end=4000 rows_after_15s=4000
PROBE46 case=l4 tree=fixed client=V1 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=3034 outcome=org.apache.kafka.connect.errors.RetriableException retriable_in_chain=yes root=java.util.concurrent.TimeoutException rows_at_end=0 rows_after_15s=4000
PROBE46 case=l5 tree=fixed client=V1 body_bytes=105780 insert_timeout_ms=3000 elapsed_ms=3026 outcome=org.apache.kafka.connect.errors.RetriableException retriable_in_chain=yes root=java.util.concurrent.TimeoutException rows_at_end=2500 rows_after_15s=4000
PROBE46 case=l6 tree=fixed client=V2 body_bytes=65610890 insert_timeout_ms=3000 elapsed_ms=8380 outcome=ok retriable_in_chain=no root=none rows_at_end=-1 rows_after_15s=-1
PROBE46WIRE body_bytes=65610890 first_progress_ms=64 body_sent_ms=2305 total_ms=8316 status=HTTP/1.1 200 OK
```

What the small-body progress escape needs: a slow materialized view behind a `Null` sink (`sleepEachRow(0.002)`), `async_insert=0` and the input split into 500-row blocks (the recipe's settings, `send_progress_in_http_headers=1`, `http_headers_progress_interval_ms=50`); with those the V2 insert ran 8315 ms on the base tree and 8299 ms on the fixed tree against a 3000 ms bound (the two differ by 0.2 %), and the control with a 600000 ms bound ran 8059 ms, so the write-phase deadline leaves a response that streams progress after the body is sent alone. The same insert under the server's default `async_insert=1` (the `function_sleep_max_microseconds_per_block` raise needed to get past the server's sleep limit) failed at 3060 ms on the base tree and 3050 ms on the fixed tree, a `DataTransferException` over a `SocketTimeoutException`, retriable by the connector's own classification. The 64 MiB body (4000 rows of 16 KiB behind the same slow view, `async_insert=0`) ended `ok` on both trees, at 8324 ms on the base tree and 8380 ms on the fixed tree, so the write-phase deadline did not cut it: the raw-wire probe of the same shape shows why, the server sent its first progress header 64 ms after the first byte (before the body had been fully sent, so `wire_progress_while_writing` is `yes`), but the client handed the whole 65610890-byte body to the socket in 2305 ms, inside the 3000 ms bound, over this transport, which means no body was still being sent at the bound and this live check never reached the progress-while-sending shape that `progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound` pins on a stub; the claim "the deadline cuts a progress-streaming body that is still being sent" therefore rests on the stub pin and not on a live measurement. What a timed-out insert left: the client V1 insert into a `Memory` table behind the slow view showed 0 rows when the timed-out call returned and 4000 rows 15 s later, the client V1 insert into a MergeTree table showed 2500 rows at once and 4000 rows 15 s later (the base-tree control landed all 4000 rows), and the timed-out default-`async_insert` V2 insert into a MergeTree table showed 4000 rows at once and 4000 rows 15 s later; in all three cases the server finished or kept the insert after the client gave up, so a timed-out insert is not rolled back, and the figures depend on the engine and on `async_insert` (the rows are read by a call that first runs `OPTIMIZE TABLE`, so "at once" is to within that call).

Recorded 2026-11-05T11:05:00Z.

Verdict: pass
