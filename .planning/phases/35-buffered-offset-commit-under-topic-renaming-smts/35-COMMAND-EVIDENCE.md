# Phase 35: Command evidence

- **Base commit:** `local_head` = `6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e` (HEAD when plan 35-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` explicitly.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-29.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. No entry carries a token or credential.
- **Rule:** a fail or pending verdict is fixed by re-measuring, never by editing the evidence.
- **Helper:** `35-evidence-check.py` in this directory is the read-only gate helper (a byte-identical copy of Phase 34's `34-evidence-check.py`; get, expect, index, rows, secrets, junit, anchors, skips, failed); it is never edited.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | BUF-01, BUF-04 | 35-01 | pass |
| E-02 | BUF-01, BUF-02, BUF-04 | 35-01 | pass |
| E-03 | BUF-01, BUF-02, BUF-07 | 35-01 | pass |
| E-04 | BUF-01, BUF-02, BUF-04 | 35-01 | pass |
| E-05 | BUF-02, BUF-05 | 35-02 | pass |
| E-06 | BUF-03 | 35-02 | pass |
| E-07 | BUF-03 | 35-02 | pass |
| E-08 | BUF-07 | 35-03 | pass |
| E-09 | BUF-01, BUF-02, BUF-03, BUF-04, BUF-05, BUF-07 | 35-03 | pass |

### E-01: Pre-flight: upstream and origin re-read, Phase 34 gated, baselines on the untouched tree

Backs: BUF-01, BUF-04, ROADMAP Phase 35 criterion 1 (the premise re-read before any code lands, and a green baseline so every later red or green is attributable to this phase).

```sh
colima list
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py index .planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md 11 --done
git rev-parse refs/tags/pre-v1.7.0-tail-2
git ls-remote --exit-code --tags origin pre-v1.7.0-tail-2
git ls-remote --exit-code --heads origin sync/v1.7.0
git ls-remote origin refs/heads/custom_build
git ls-remote upstream main
gh issue view 777 -R ClickHouse/clickhouse-kafka-connect --json state --jq .state
gh pr view 786 -R ClickHouse/clickhouse-kafka-connect --json state,mergedAt
git diff --quiet c9443756eb85e601552cb8241f0e7b52ed6209da HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py anchors .planning/scripts/fork-anchors.txt
cmp .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py .planning/phases/34-upstream-tail-merge/34-evidence-check.py
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd35/exec/testunit-baseline.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd35/exec/suite-baseline.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/test --class ClickHouseSinkTaskBufferTest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
branch: sync/v1.7.0
local_head: 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e
dirty_paths: M .planning/STATE.md; M .planning/config.json; M .planning/state.json
colima_vz: Running
phase34_complete: yes
pre_tail2_tag_target: 68f1b0cbfcb217fea1de769b48bc985dacdc40f3
pre_tail2_tag_on_origin: absent
sync_branch_on_origin: absent
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463 refs/heads/custom_build
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved: no
upstream_777_state: OPEN
upstream_786_state: CLOSED unmerged
gate_scripts_vs_33_e14: identical
anchors_mirror: rows=141 bad=0
helper_identical_to_phase34: yes
baseline_testunit_exit: 0
baseline_testunit_tests: 1441
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_suite_exit: 0
baseline_suite_tests: 2028
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_duration: BUILD SUCCESSFUL in 2m 44s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
baseline_docker_only_tests: 587
baseline_buffer_class_tests: 35
```

`git ls-remote --exit-code` exited 2 for both the tag and the branch (absent on origin). The pull request state was `{"mergedAt":null,"state":"CLOSED"}`, recorded as `CLOSED unmerged`. The testUnit baseline is 1441 tests over 68 XML files; the full suite is 2028 tests over 112 XML files. The dirty tree held only `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json`. No `javacore.*`, `core.*.dmp`, `Snap.*.trc` or `jitdump.*` file appeared. The `ClickHouseSinkTaskBufferTest` class ran 35 tests green; plan 35-02 must find it unchanged.

Timestamp (UTC): 2026-10-29T09:21:41Z to 2026-10-29T09:24:29Z

Verdict: pass

### E-02: Red pins on the unchanged tree: BufferedOffsetCommitUnitTest fails exactly the BUF set

Backs: BUF-01, BUF-02, BUF-04, ROADMAP Phase 35 criterion 1 (Docker-free unit tests built through newRecord fail on the unchanged code and their controls pass).

```sh
git diff --quiet 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e HEAD -- src/main
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BufferedOffsetCommitUnitTest' > /tmp/gsd35/exec/red-pins.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/testUnit --class BufferedOffsetCommitUnitTest
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py failed build/test-results/testUnit --expect <the 8 red names>
git worktree add --detach /tmp/gsd35/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/fmt --console=plain spotlessApply > /tmp/gsd35/exec/fmt-1.log 2>&1
cmp <each new Java file> /tmp/gsd35/fmt/<same path>
git worktree remove --force /tmp/gsd35/fmt
git commit -F /tmp/gsd35/exec/msg-red-pins.txt
git diff --name-only <red_pins_commit>^ <red_pins_commit>
git show <red_pins_commit>:<main file> | grep -cF <transformed-topic accessor call>
```

```text
red_run_exit: 1
red_failed: BufferedOffsetCommitUnitTest#BUF-01: preCommit keys the flushed offsets by the original topic and partition at the original offset plus one [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit keys the flushed offsets by the original topic and partition at the original offset plus one [EXACTLY_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit follows the original partition and offset when the SMT changes them as well [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit follows the original partition and offset when the SMT changes them as well [EXACTLY_ONCE]; BufferedOffsetCommitUnitTest#BUF-02: at-least-once close drops the buffered records of the revoked original partition and keeps the others; BufferedOffsetCommitUnitTest#BUF-02: exactly-once buffer groups by the original partition so a revoked partition leaves no stale records in a later chunk; BufferedOffsetCommitUnitTest#BUF-01: close forgets the flushed offsets of the revoked original partition only [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: close forgets the flushed offsets of the revoked original partition only [EXACTLY_ONCE]
red_class_xml: OK junit build/test-results/testUnit class *BufferedOffsetCommitUnitTest: tests=13 failures=8 errors=0 skipped=0 files=1
red_prediction_met: yes
spotless_worktree_cmp: identical
spotless_worktree_cmp_fixtures: identical
red_pins_commit: e8573752bad7040e7de17e83ca35681910e0f685
red_pins_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java; src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java; src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java
main_unchanged_at_red_commit: yes
unfixed_zero_row_counts: ChunkFlusher.java=1; AtLeastOnceBufferStrategy.java=1; ExactlyOnceBufferStrategy.java=1
```

The 8 failures are assertion failures, not errors: the preCommit pins report `expected {buf35-dev-1=110, buf35-dev-0=50} but was {buf35-0=50, buf35-1=110}` (the renamed topic as the key), the at-least-once pin writes the revoked partition's records, the exactly-once pin flushes stale records of the revoked partition in a later chunk, and the close pin keeps the renamed-topic keys after the revocation. The 5 controls (records that were not renamed in both strategies, the direct strategy, and table routing under the renamed topic in both strategies) pass. The red-pins commit is red by design and local. The three `unfixed_zero_row_counts` values are the baselines of the three zero anchor rows the fix commit adds: each is 1 here, so those rows fail on the unfixed code.

Timestamp (UTC): 2026-10-29T09:26:17Z

Verdict: pass

### E-03: The fix commit: three offset sites, the BLD-16 re-bless, 7 anchor rows, the FORK-DELTA row, pins green

Backs: BUF-01, BUF-02, BUF-07, ROADMAP Phase 35 criterion 1 (the pins pass after the fix, the three accessors are blessed and shown present in the production worker's jar) and the first half of criterion 5 (the fix commit carries the FORK-DELTA row and the anchors).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava > /tmp/gsd35/exec/compilejava-fix.log 2>&1
git diff --numstat -- src/main
git diff -U0 -- src/main | grep -cE '^[-+]import '
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BufferedOffsetCommitUnitTest' > /tmp/gsd35/exec/pins-green.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd35/exec/testunit-bld16-red.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py failed build/test-results/testUnit --expect 'ConnectApiReferencesUnitTest#BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list'
docker image inspect --format '{{.Id}}' confluentinc/cp-kafka-connect-base:7.9.7
docker create confluentinc/cp-kafka-connect-base:7.9.7
docker cp <container>:/usr/share/java/kafka/connect-api-7.9.7-ccs.jar /tmp/gsd35/worker-jars/
docker cp <container>:/usr/share/java/kafka/kafka-clients-7.9.7-ccs.jar /tmp/gsd35/worker-jars/
docker rm <container>
shasum -a 256 /tmp/gsd35/worker-jars/*.jar <the 4.1.1 and 4.4.0 connect-api and kafka-clients jars of the Gradle cache>
javap -cp /tmp/gsd35/worker-jars/connect-api-7.9.7-ccs.jar org.apache.kafka.connect.sink.SinkRecord
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava > /tmp/gsd35/exec/compilejava-fresh.log 2>&1
python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main --compile-classpath <4.4.0 jars> --reference worker=<worker jars> --reference 4.1.1=<4.1.1 jars> --reference 4.4.0=<4.4.0 jars> --image-note "image: confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055 (tag 7.9.7; linux/amd64 and linux/arm64 jars byte-identical; Temurin 17.0.19)" --write src/test/resources/connect-api-references.txt > /tmp/gsd35/exec/rebless.log 2>&1
git diff -U0 -- src/test/resources/connect-api-references.txt
grep -cF -e <each of the 7 anchor patterns> <its file>
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py anchors .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd35/exec/testunit-green.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-hygiene-check.py /tmp/gsd35/exec/added-lines-fix.txt
git commit -F /tmp/gsd35/exec/msg-fix.txt
git diff --name-only <fix_commit>^ <fix_commit>
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 <fix_commit> > /tmp/gsd35/exec/tail-gate-fix.log 2>&1
```

```text
compilejava_exit: 0
diff_numstat: AtLeastOnceBufferStrategy.java:1/1; ChunkFlusher.java:3/2; ExactlyOnceBufferStrategy.java:1/1
import_lines_changed: 0
object_creations_equal: yes
unit_pins_exit: 0
unit_pins_xml: OK junit build/test-results/testUnit class *BufferedOffsetCommitUnitTest: tests=13 failures=0 errors=0 skipped=0 files=1
bld16_red_exit: 1
bld16_red_failed: ConnectApiReferencesUnitTest#BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list
bld16_red_prediction_met: yes
worker_image_id: sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055
worker_connect_api_sha256: b57546e9160d5435238fded6f38756d621fac62bd9f64737c3f68e094ae01de4
worker_kafka_clients_sha256: 28423086fbbf8f91b2f1d335eeeca6675916e24adf2f813b3f9c47f87b49f962
worker_jar_hashes_match_header: yes
reference_jar_hashes_match_header: yes
worker_accessors_present: originalTopic; originalKafkaPartition; originalKafkaOffset
rebless_exit: 0
rebless_summary: REFERENCE worker classes-missing 0 members-missing 0; REFERENCE 4.1.1 classes-missing 0 members-missing 0; REFERENCE 4.4.0 classes-missing 0 members-missing 0
rebless_diff: the # date: line replaced, and three M lines added (SinkRecord.originalKafkaOffset, SinkRecord.originalKafkaPartition, SinkRecord.originalTopic); derived members 138 to 141, total 177 to 180
rebless_image_line_unchanged: yes
anchor_counts_measured: 1; 1; 0; 1; 0; 1; 0
new_anchor_rows: 7
anchors_mirror: rows=148 bad=0
testunit_exit: 0
testunit_tests: 1454
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 13
hygiene_scan: clean
fix_commit: 6f3533c20098f048aa0d3fa9ee59638cbdb11ab5
fix_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java; src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java; src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java; src/test/resources/connect-api-references.txt
tail_gate_exit: 0
tail_gate_line: invariants: provenance_paths=27 anchor_commits=33 anchor_rows=148 marker_commits=40 anchor_exempt=0 result=PASS
```

The three accessors are the lines `public java.lang.String originalTopic();`, `public java.lang.Integer originalKafkaPartition();` and `public long originalKafkaOffset();` that `javap` prints for `SinkRecord` in the worker's `connect-api-7.9.7-ccs.jar`. The worker jar hashes equal the `# reference worker:` header lines of the blessed file and the 4.1.1 and 4.4.0 jar hashes equal their header lines. The BLD-16 red run (the edit in the tree, the list not yet re-blessed) failed exactly that one test out of 1454. The fix commit holds six paths by design: BLD-16 goes red the instant the main edit lands without the re-bless, and an anchor may not precede or lag the code it pins. The exactly-once state key, `Processing.java`, `KeeperStateProvider.java` and table routing are untouched; the main diff changes no import line and adds no object creation.

Timestamp (UTC): 2026-10-29T09:30:24Z

Verdict: pass

### E-04: Mutation proof: each edited file restored from the red commit turns exactly its pins and anchor rows red

Backs: BUF-01, BUF-02, BUF-04, ROADMAP Phase 35 criterion 1 (every pin and anchor row of the fix can fail, and each of the three edits is individually guarded).

```sh
git worktree add --detach /tmp/gsd35/mut <fix_commit>
git -C /tmp/gsd35/mut checkout <red_pins_commit> -- <one edited main file>
rm -rf /tmp/gsd35/mut/build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/mut --console=plain testUnit --rerun --tests '*BufferedOffsetCommitUnitTest' > /tmp/gsd35/exec/mut-<a|b|c>.log 2>&1
python3 -I <abs path>/35-evidence-check.py failed /tmp/gsd35/mut/build/test-results/testUnit --expect <the expected names>
cd /tmp/gsd35/mut && python3 -I <abs path>/35-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git -C /tmp/gsd35/mut checkout <fix_commit> -- <the same file>
git worktree remove --force /tmp/gsd35/mut
git worktree prune
git worktree list
git diff --quiet <fix_commit> HEAD -- src
```

```text
mutation_a_file: src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java
mutation_a_exit: 1
mutation_a_failed: BufferedOffsetCommitUnitTest#BUF-01: preCommit keys the flushed offsets by the original topic and partition at the original offset plus one [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit keys the flushed offsets by the original topic and partition at the original offset plus one [EXACTLY_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit follows the original partition and offset when the SMT changes them as well [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: preCommit follows the original partition and offset when the SMT changes them as well [EXACTLY_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: close forgets the flushed offsets of the revoked original partition only [AT_LEAST_ONCE]; BufferedOffsetCommitUnitTest#BUF-01: close forgets the flushed offsets of the revoked original partition only [EXACTLY_ONCE]
mutation_a_prediction_met: yes
mutation_a_anchor_bad: 3
mutation_b_file: src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java
mutation_b_exit: 1
mutation_b_failed: BufferedOffsetCommitUnitTest#BUF-02: at-least-once close drops the buffered records of the revoked original partition and keeps the others
mutation_b_prediction_met: yes
mutation_b_anchor_bad: 2
mutation_c_file: src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java
mutation_c_exit: 1
mutation_c_failed: BufferedOffsetCommitUnitTest#BUF-02: exactly-once buffer groups by the original partition so a revoked partition leaves no stale records in a later chunk
mutation_c_prediction_met: yes
mutation_c_anchor_bad: 2
worktrees_removed: yes
main_tree_src_unchanged: yes
```

Each mutation restored one edited main file to its content at the red-pins commit (no text replacement) and put the fix back before the next one, so the three runs were independent. Restoring `ChunkFlusher.java` turned the six offset pins red (the preCommit pins and the close pin, both strategies) and the three `ChunkFlusher` anchor rows; restoring `AtLeastOnceBufferStrategy.java` turned only the at-least-once close pin and its positive and zero anchor rows red; restoring `ExactlyOnceBufferStrategy.java` turned only the exactly-once grouping pin and its positive and zero anchor rows red. In every run the failing set equalled the expected set, which also shows the five controls (the not-renamed case, the direct strategy and table routing under the renamed topic) stay green under each mutation. No red set differed from the prediction, so no follow-up test commit and no second proof run was needed. The mutation worktree is removed and the main tree's `src` equals the fix commit's.

Timestamp (UTC): 2026-10-29T09:33:00Z

Verdict: pass

### E-05: Live pin: red on the unchanged tree in a worktree, green on the fixed tip, mutation proof and the harness decision

Backs: BUF-02, BUF-05, ROADMAP Phase 35 criterion 3 (a ClickHouse-backed pin shows the committed offsets equal to the end offsets under a renaming SMT, the direct path and the buffer contract are unchanged, and the harness choice is recorded with reasons).

```sh
grep -l integrationTest .github/workflows/*.yaml
sed -n 474,492p src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
git worktree add --detach /tmp/gsd35/red <red_pins_commit>
git -C /tmp/gsd35/red diff --quiet <local_head> HEAD -- src/main
git worktree add --detach /tmp/gsd35/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/fmt --console=plain spotlessApply > /tmp/gsd35/exec/fmt-live.log 2>&1
cmp /tmp/gsd35/red/src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java /tmp/gsd35/fmt/src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
git worktree remove --force /tmp/gsd35/fmt
rm -rf /tmp/gsd35/red/build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/red --console=plain test --rerun --tests '*BufferedOffsetCommitLiveTest' > /tmp/gsd35/exec/live-red.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py failed /tmp/gsd35/red/build/test-results/test --expect <the 2 red names>
cp /tmp/gsd35/red/src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
cmp /tmp/gsd35/red/src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
git worktree remove --force /tmp/gsd35/red
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*BufferedOffsetCommitLiveTest' --tests '*ClickHouseSinkTaskBufferTest' --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskTest.preCommit*' --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskTest.closeRemoves*' --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskTest.onPartitionsRevoked*' > /tmp/gsd35/exec/live-green.log 2>&1
git diff --quiet <local_head> HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskBufferTest.java src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
git worktree add --detach /tmp/gsd35/mut <fix_commit>
git -C /tmp/gsd35/mut checkout <red_pins_commit> -- <AtLeastOnceBufferStrategy.java, then ChunkFlusher.java; restored from <fix_commit> after each run>
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/mut --console=plain test --rerun --tests '*BufferedOffsetCommitLiveTest' > /tmp/gsd35/exec/live-mut-<a|b>.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py failed /tmp/gsd35/mut/build/test-results/test --expect <the expected names>
git worktree remove --force /tmp/gsd35/mut
git worktree prune
git worktree list
git commit -F /tmp/gsd35/exec/msg-live-pin.txt
git diff --name-only <live_pin_commit>^ <live_pin_commit>
git diff --quiet <fix_commit> <live_pin_commit> -- src/main src/testFixtures
```

```text
integration_test_workflows: nightly-integration-tests.yaml; integrationTests.yaml
confluent_platform_offset_helper: ConfluentPlatform.getOffset reads the endpoint topics/<topic>/partitions/<n>/offsets and returns obj.getLong("end_offset"), a topic's end offset and not a group's committed offset
red_worktree_main_unchanged: yes
spotless_worktree_cmp: identical
live_red_exit: 1
live_red_failed: BufferedOffsetCommitLiveTest#BUF-02: buffered sink under a topic-renaming SMT drops the revoked partition and commits only the partitions it still owns; BufferedOffsetCommitLiveTest#BUF-05: buffered sink under a topic-renaming SMT commits the end offset of every assigned original partition and inserts every row
live_red_class_xml: OK junit /tmp/gsd35/red/build/test-results/test class *BufferedOffsetCommitLiveTest: tests=4 failures=2 errors=0 skipped=0 files=1
live_red_prediction_met: yes
live_class_bytes_equal_red_worktree: yes
live_green_exit: 0
live_class_xml: OK junit build/test-results/test class *BufferedOffsetCommitLiveTest: tests=4 failures=0 errors=0 skipped=0 files=1
buffer_class_xml: OK junit build/test-results/test class *ClickHouseSinkTaskBufferTest: tests=35 failures=0 errors=0 skipped=0 files=1
direct_offset_tests_xml: OK junit build/test-results/test class *ClickHouseSinkTaskTest: tests=5 failures=0 errors=0 skipped=0 files=1
existing_tests_unedited: yes
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_same_as_baseline: yes
jvm_crash: none
live_mutation_a_file: src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java
live_mutation_a_failed: BufferedOffsetCommitLiveTest#BUF-02: buffered sink under a topic-renaming SMT drops the revoked partition and commits only the partitions it still owns
live_mutation_a_prediction_met: yes
live_mutation_b_file: src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java
live_mutation_b_failed: BufferedOffsetCommitLiveTest#BUF-02: buffered sink under a topic-renaming SMT drops the revoked partition and commits only the partitions it still owns; BufferedOffsetCommitLiveTest#BUF-05: buffered sink under a topic-renaming SMT commits the end offset of every assigned original partition and inserts every row
live_mutation_b_prediction_met: yes
worktrees_removed: yes
live_pin_harness: test suite, ClickHouseSinkTask against ClickHouse latest with client V2
live_pin_observes: the offsets preCommit returns, which Connect commits exactly after dropping keys outside its assignment; the pin applies that drop through RenamedTopicRecords.acceptedByConnect, asserts it removes nothing, and asserts each value equals the end offset of its original partition
broker_level_class: not added
broker_level_reasons: no phase gate (testUnit, the full test --rerun, the anchors invariant) runs integrationTest, which runs only in the workflows named in integration_test_workflows, so a class there could not carry the guarantee through the suite gates of Phases 36 and 38; a broker-level class needs a connector archive built from the unfixed tree for its red run, a class start-up of about 95 seconds (RESEARCH, measured at 29.5 minutes for the whole integration task) and a new ConfluentPlatform helper, because the existing offset helper reads a topic's end offset (confluent_platform_offset_helper), not a group's committed offset; it would observe the same keys and values the test-suite pin observes, plus Connect's own filter, which the pin applies from the documented contract
live_pin_commit: 2e99eadd98a370e5e9d1b17e3f0297a8c628f987
live_pin_commit_files: docs/FORK-DELTA.md; src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
main_and_fixtures_unchanged: yes
hygiene_scan: clean
```

On the unchanged tree both failing tests are assertion failures at the first offset assertion, not errors: the returned keys are the renamed topic (`<table>-0`, `<table>-1`, `<table>-2` and `<table>-0`, `<table>-2`) where the pin expects `<table>-dev-<p>`. The two controls (the not-renamed buffered case and the non-buffered direct case) pass on both trees. Restoring `AtLeastOnceBufferStrategy.java` turned only the revocation test red; restoring `ChunkFlusher.java` turned the commit and the revocation test red, as predicted, and each file was put back from the fix commit before the next run.

A broker-level observation of a group's committed offsets, for example `kafka-consumer-groups --describe --group <group>`, remains a manual operator check that no gate depends on.

Timestamp (UTC): 2026-10-29T09:34:00Z to 2026-10-29T09:40:00Z (approximate start; the plan did not capture it)

Verdict: pass

### E-06: BUF-03 probes: state key, many-to-one, the originalTopic local and the Direct path with reportInsertedOffsets, measured on the tip

Backs: BUF-03, ROADMAP Phase 35 criterion 2 (Processing.doLogic was checked on the current tree and the outputs are recorded as printed; nothing in the repository changed).

```sh
git rev-parse HEAD
git worktree add --detach /tmp/gsd35/probe <probe_base_commit>
# throwaway class, worktree only: src/test/java/com/clickhouse/kafka/connect/sink/Buf03ProbeUnitTest.java (7 methods, one BUF03-PROBE line each, no assertion on a measured value)
rm -rf /tmp/gsd35/probe/build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/probe --console=plain testUnit --rerun --tests '*Buf03ProbeUnitTest' > /tmp/gsd35/exec/probe.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit /tmp/gsd35/probe/build/test-results/testUnit --class Buf03ProbeUnitTest
grep -ho 'BUF03-PROBE [A-Za-z0-9_=.,:+ -]*' /tmp/gsd35/probe/build/test-results/testUnit/TEST-*Buf03ProbeUnitTest.xml | sort -u
python3 -I -c '<assert the old originalTopic line occurs once, then replace record.getSinkRecord().topic() by record.getSinkRecord().originalTopic() in the worktree Processing.java>'
rm -rf /tmp/gsd35/probe/build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/probe --console=plain testUnit --rerun --tests '*Buf03ProbeUnitTest' --tests '*ProcessingTest' > /tmp/gsd35/exec/probe-p6b.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit /tmp/gsd35/probe/build/test-results/testUnit --class ProcessingTest
git worktree remove --force /tmp/gsd35/probe
git worktree prune
git worktree list
git diff --quiet <local_head> HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java src/main/java/com/clickhouse/kafka/connect/sink/state src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
```

```text
probe_base_commit: db4b7d0dbfab20180fb4540950a0074d58a08bf1
probe_run_exit: 0
probe_class_xml: OK junit /tmp/gsd35/probe/build/test-results/testUnit class *Buf03ProbeUnitTest: tests=7 failures=0 errors=0 skipped=0 files=1
probe_p1: offered=400 written=400 last_inserted=t-0=200
probe_p2: offered_a=200 offered_b=100 written_a=200 written_b=0
probe_p3: exception=RuntimeException message_prefix=AFTER_PROCESSING: State MISMATCH as batch already processed
probe_p4: offered_b=100 written_b=0 exception=none
probe_p5: last_inserted_keys=bufT-0=10
probe_p6: preCommit_keys=none control_keys=bufT-0=10
probe_p7: preCommit_keys=bufT-dev-0=10 same_instance=true
probe_prediction_met: yes
probe_p6b: preCommit_keys=bufT-dev-0=10 control_keys=none
processing_test_after_one_line_xml: OK junit /tmp/gsd35/probe/build/test-results/testUnit class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
worktrees_removed: yes
probe_class_committed: no
processing_untouched_in_main_tree: yes
```

The seven lines equal the planning-time observations of RESEARCH one for one (P1 400 written under the one key `t-0`; P2 `b` written 0 of 100; P3 the `AFTER_PROCESSING: State MISMATCH` exception; P4 `b` written 0; P5 key `bufT-0`; P6 no keys with the transformed key as the control; P7 `bufT-dev-0=10` and the same map instance), so there is no difference to report. In the P6b run the same probe class printed P1 `last_inserted=a-dev-0=200` and P5 `bufT-dev-0=10`: with the one-line change the offsets map is keyed by the original topic, and P2 to P4 and P7 print as before.

P1 (equal offset ranges lose nothing): two sources renamed to one target, with equal offset ranges in interleaved batches, wrote all 400 offered records under one shared state record `t-0`; the state key is the transformed topic and the partition, and nothing is lost while the offset spaces coincide.

P2 to P4 (skewed offsets lose or reject records of the lower-offset source): with a state of `a-dev` at offsets 5000 to 5099, the lower-offset source `b-dev` had 0 of 100 records written when it arrived with offsets 100 to 199 in the next batch (P2), the batch with offsets 10 to 109 raised a `RuntimeException` starting `AFTER_PROCESSING: State MISMATCH as batch already processed` (P3), and with `tolerateStateMismatch=true` it was skipped silently, 0 of 100 written (P4). This runs through `Processing` directly, so it is independent of buffering.

P5 and P6 (`lastInsertedOffsets` is keyed by the transformed topic and the Direct path with `reportInsertedOffsets=true` returns no offsets under a renaming SMT): the offsets map holds `bufT-0`, the transformed topic, while Connect's own positions are keyed `bufT-dev-0`, so `preCommit` returns none (P6) and returns `bufT-0=10` only when the caller's keys are the wrong ones (the control). The default configuration is unaffected: P7 hands Connect its own map back, the same instance.

P6b (the one-line counterfactual restores the keys and is not applied): changing the one line that assigns the `originalTopic` local in `Processing.doLogic`, in the removed worktree only, makes P6 return `bufT-dev-0=10` and leaves all 14 `ProcessingTest` cases green; the change is not applied in the repository, by the recorded decision that `Processing.java` is not edited in this phase.

Timestamp (UTC): 2026-10-29T09:41:00Z to 2026-10-29T09:42:15Z

Verdict: pass

### E-07: BUF-03 tripwire pinned, mutation-proved, and the record written

Backs: BUF-03, ROADMAP Phase 35 criterion 2 (the part that must not move, the exactly-once state key, has a Docker-free tripwire that bites under a state-key mutation, and the outcome, open items, KeeperMap statement and stop rule are recorded).

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ProcessingRenamedTopicStateKeyUnitTest' --tests '*ProcessingTest' > /tmp/gsd35/exec/tripwire-green.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/testUnit --class ProcessingRenamedTopicStateKeyUnitTest
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/testUnit --class ProcessingTest
git worktree add --detach /tmp/gsd35/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/fmt --console=plain spotlessApply > /tmp/gsd35/exec/fmt-tripwire.log 2>&1
cmp src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java /tmp/gsd35/fmt/src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
git worktree remove --force /tmp/gsd35/fmt
git commit -F /tmp/gsd35/exec/msg-tripwire.txt
git diff --name-only <tripwire_commit>^ <tripwire_commit>
git worktree add --detach /tmp/gsd35/mut <tripwire_commit>
python3 -I -c '<assert the line "String topic = record.getRecordOffsetContainer().getTopic();" followed by the originalTopic line occurs once in the worktree Processing.java, then read record.getSinkRecord().originalTopic() instead>'
rm -rf /tmp/gsd35/mut/build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd35/mut --console=plain testUnit --rerun --tests '*ProcessingRenamedTopicStateKeyUnitTest' > /tmp/gsd35/exec/tripwire-mut.log 2>&1
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py failed /tmp/gsd35/mut/build/test-results/testUnit --expect <the 2 pinned names>
git worktree remove --force /tmp/gsd35/mut
git worktree prune
git worktree list
git diff --quiet <local_head> HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java src/main/java/com/clickhouse/kafka/connect/sink/state src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-hygiene-check.py <the record, the evidence and the test class>
```

```text
tripwire_exit: 0
tripwire_class_xml: OK junit build/test-results/testUnit class *ProcessingRenamedTopicStateKeyUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
processing_test_xml: OK junit build/test-results/testUnit class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
spotless_worktree_cmp: identical
tripwire_commit: 3e4248e9d16b49cca8993e6ddb219f63a9fac28f
tripwire_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
mutation_state_key_exit: 1
mutation_state_key_failed: ProcessingRenamedTopicStateKeyUnitTest#BUF-03: Processing keeps the exactly-once state of renamed records under the transformed topic and partition; ProcessingRenamedTopicStateKeyUnitTest#BUF-03: two renamed sources with equal offset ranges are written in full under one shared state record
mutation_state_key_prediction_met: yes
mutation_state_key_class_xml: OK junit /tmp/gsd35/mut/build/test-results/testUnit class *ProcessingRenamedTopicStateKeyUnitTest: tests=2 failures=2 errors=0 skipped=0 files=1
worktrees_removed: yes
processing_untouched_in_main_tree: yes
record_file: 35-BUF03-RECORD.md
open_items: OI-35-1; OI-35-2
```

The tripwire pins unchanged behaviour, so it passed on its first run; the red half is the mutation. With the `topic` local read from `originalTopic()` in the worktree, both executions fail on their first state assertion (`expected: <AFTER_PROCESSING> but was: <NONE>` under the transformed topic), because the state is then stored under the original topic. The mutation edited `Processing.java` in the removed worktree only. The record also carries a third open item, OI-35-3 (exactly-once buffering under a partition-changing SMT, read from the code and not probed), beyond the two named in the `open_items` key.

Timestamp (UTC): 2026-10-29T09:38:00Z to 2026-10-29T09:45:00Z (approximate start; the plan did not capture it)

Verdict: pass

### E-08: Rollout item for Phase 38 and the first-start decision

Backs: BUF-07, ROADMAP Phase 35 criterion 5, second half (the rollout item with its evidence label, which RLS-02 checks), and the research flag on the first start after the upgrade.

```sh
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*'
gh issue view 777 -R ClickHouse/clickhouse-kafka-connect --json state --jq .state
gh pr view 786 -R ClickHouse/clickhouse-kafka-connect --json state,mergedAt
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-hygiene-check.py .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-ROLLOUT-ITEM.md .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md
test ! -e docs/ROLLOUT-v1.7.0_gg6.md
git add .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-ROLLOUT-ITEM.md
git commit -F /tmp/gsd35/exec/msg-rollout-item.txt
git diff --name-only <rollout_item_commit>^ <rollout_item_commit>
```

```text
upstream_main_now: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved_since_preflight: no
upstream_newest_release_tag: v1.7.0
upstream_777_state_now: OPEN
upstream_786_state_now: CLOSED unmerged
first_start_measurement: not measured
first_start_measurement_reasons: the behaviour is Kafka consumer-group semantics (committed offsets, auto.offset.reset, offset retention) that need a broker and a running Connect worker with a real consumer group, which is the integration harness with its roughly 95 second class start-up and a connector archive built from the unfixed tree for the before half (E-05 broker_level_reasons); a test in the test suite would measure a mock consumer, not Kafka; the rollout item therefore words the claim as derived and tells the reader how to check their own group read-only
rollout_item_file: 35-ROLLOUT-ITEM.md
rollout_item_commit: c2c2be3157d9877814c123e3a6584e0f6505f1d9
rollout_item_sections: ## What changes; ## Who is affected; ## First start after the upgrade; ## What to do; ## Minimum Connect runtime; ## Evidence labels; ## Handoffs
rollout_item_labels: measured; read from the code; derived from Kafka consumer-group semantics (not measured)
gg6_rollout_note_absent: yes
hygiene_scan: clean
```

The upstream state was re-read before the item was written: issue #777 is still open and pull request #786 is still closed without a merge, so the fork-only statement of FORK-DELTA and the wording of the item holds. Upstream main equals the pre-flight value of E-01 and the newest release tag is `v1.7.0`. The rollout-item commit holds exactly the item file; `docs/ROLLOUT-v1.7.0_gg6.md` was neither created nor edited.

Timestamp (UTC): 2026-10-29T09:49:05Z

Verdict: pass

### E-09: Phase 35 gate

Backs: BUF-01, BUF-02, BUF-03, BUF-04, BUF-05, BUF-07, ROADMAP Phase 35 gate (origin and upstream re-read, cache-busted testUnit, one full `--rerun` suite on latest with client V2 with the 18 named skips, the anchors mirror) plus the tail and cumulative merge gates of Phase 34 E-11.

```sh
git rev-parse HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
git ls-remote --exit-code --tags origin pre-v1.7.0-tail-2
git ls-remote --exit-code --heads origin sync/v1.7.0
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*'
gh issue view 777 -R ClickHouse/clickhouse-kafka-connect --json state --jq .state
gh pr view 786 -R ClickHouse/clickhouse-kafka-connect --json state,mergedAt
bash .planning/scripts/verify-merge-invariants.sh 54e63648 3e574320ba889671a95c43767893100e28f866cf > /tmp/gsd35/exec/cumulative-gate-gate.log 2>&1   # background, exit code in a file
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd35/exec/testunit-gate.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd35/exec/suite-gate.log 2>&1   # background, exit code in a file
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py junit build/test-results/test --class <BufferedOffsetCommitUnitTest | BufferedOffsetCommitLiveTest | ClickHouseSinkTaskBufferTest | ProcessingRenamedTopicStateKeyUnitTest>
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py anchors .planning/scripts/fork-anchors.txt
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 3e574320ba889671a95c43767893100e28f866cf > /tmp/gsd35/exec/tail-gate-gate.log 2>&1
git diff --quiet c9443756eb85e601552cb8241f0e7b52ed6209da 3e574320ba889671a95c43767893100e28f866cf -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
git diff --name-only 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e 3e574320ba889671a95c43767893100e28f866cf -- src/main
git diff --quiet 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e 3e574320ba889671a95c43767893100e28f866cf -- <the high-risk sink paths>
git diff --quiet 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e 3e574320ba889671a95c43767893100e28f866cf -- build.gradle.kts settings.gradle.kts gradle VERSION CHANGELOG.md .github
git diff --name-status 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e 3e574320ba889671a95c43767893100e28f866cf -- src/test src/testFixtures src/integrationTest
git log --format=%h -- <35-evidence-check.py | 35-hygiene-check.py>
cmp .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py .planning/phases/34-upstream-tail-merge/34-evidence-check.py
git rev-list --first-parent --merges pre-v1.7.0-tail-2..3e574320ba889671a95c43767893100e28f866cf
git log --first-parent --format='%h %s' 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e..3e574320ba889671a95c43767893100e28f866cf
```

```text
gated_head: 3e574320ba889671a95c43767893100e28f866cf
utc_start: 2026-10-29T09:49:30Z
dirty_paths_at_gate: M .planning/STATE.md; M .planning/config.json; M .planning/state.json
origin_custom_build_at_gate: 3a3a15a9ae064ffe09c696b8bb87bce23419d463 refs/heads/custom_build
origin_unchanged: yes
pre_tail2_tag_on_origin: absent
sync_branch_on_origin: absent
upstream_main_at_gate: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved_during_phase: no
upstream_newest_release_tag_at_gate: v1.7.0
upstream_777_state_at_gate: OPEN
upstream_786_state_at_gate: CLOSED unmerged
testunit_exit: 0
testunit_tests: 1456
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_expected_tests: 1456
testunit_count_matches: yes
suite_exit: 0
suite_tests: 2047
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite_duration: BUILD SUCCESSFUL in 2m 48s
suite_jvm_crash: none
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_baseline: yes
unit_class_xml: OK junit build/test-results/test class *BufferedOffsetCommitUnitTest: tests=13 failures=0 errors=0 skipped=0 files=1
live_class_xml: OK junit build/test-results/test class *BufferedOffsetCommitLiveTest: tests=4 failures=0 errors=0 skipped=0 files=1
buffer_class_xml: OK junit build/test-results/test class *ClickHouseSinkTaskBufferTest: tests=35 failures=0 errors=0 skipped=0 files=1
tripwire_class_xml: OK junit build/test-results/test class *ProcessingRenamedTopicStateKeyUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
docker_only_tests: 591
docker_only_delta: 4
docker_only_delta_matches: yes
anchors_mirror: rows=148 bad=0
tail_gate_exit: 0
tail_gate_line: invariants: provenance_paths=27 anchor_commits=44 anchor_rows=148 marker_commits=51 anchor_exempt=0 result=PASS
gate_scripts_vs_33_e14: identical
src_main_changed: src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java; src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java; src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java
high_risk_untouched: yes
build_and_release_files_untouched: yes
test_side_not_added: M src/test/resources/connect-api-references.txt
test_side_added: src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java; src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java; src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java
no_gg6_rollout_note: yes
helper_commits: 1
hygiene_helper_commits: 1
helper_identical_to_phase34: yes
merges_since_tag: adaf8c4e76375e5099c076f2ca3bace892709765
phase_commits: 3e574320 docs(35-03): record the rollout item and the first-start decision (E-08); c2c2be31 docs(35-03): record the buffered offset-commit rollout item for Phase 38 (BUF-07); eb06d0bd docs(35-02): complete the live pin and BUF-03 record plan; b7379a63 docs(35-02): record the Processing.doLogic check, its open items and the tripwire (E-07); 3e4248e9 test(35-02): pin the exactly-once state key of renamed records as a tripwire (BUF-03); a7b3138c docs(35-02): record the BUF-03 probes on the tip (E-06); db4b7d0d docs(35-02): record the live pin and the harness decision (E-05); 2e99eadd test(35-02): pin the committed offsets of a buffered sink under a topic-renaming SMT on ClickHouse (BUF-02, BUF-05); b4fb0ca8 docs(35-01): complete the buffered offset commit fix plan; 4db8087d docs(35-01): record the mutation proof of the fix (E-04); 76ed1511 docs(35-01): record the fix commit and its gates (E-03); 6f3533c2 fix(35-01): commit buffered offsets under the original topic and partition (BUF-01, BUF-02, BUF-07); ad978fcd docs(35-01): record the red pins (E-02); e8573752 test(35-01): pin offsets, revocation and buffering of renamed records, red on the unchanged tree (BUF-01, BUF-02, BUF-04); e23247b1 docs(35-01): open the Phase 35 evidence: pre-flight and baselines on the untouched tree
cumulative_gate_exit: 0
cumulative_gate_line: invariants: provenance_paths=21 anchor_commits=1055 anchor_rows=148 marker_commits=1121 anchor_exempt=19 result=PASS
utc_end: 2026-10-29T10:13:56Z
```

The cumulative gate ran in the background from 09:49:44Z to 10:13:56Z (about 24 minutes) while testUnit and the full suite ran in the foreground; it reads commit trees through git only. Its line differs from Phase 34's in `anchor_rows=148` and in the commit counts. The tail gate over the merge commit to the tip ends `anchor_exempt=0 result=PASS`. The suite total is 2047 tests: the baseline 2028 plus 19 (13 unit pins, 4 live executions and 2 tripwire executions); of those, 15 are Docker-free (testUnit 1441 to 1456) and 4 are the live class, so the Docker-only count moved from 587 to 591. No `javacore.*`, `core.*.dmp`, `Snap.*.trc` or `jitdump.*` file appeared and the suite log has no `non-zero exit value 255`.

`.planning/STATE.md` was modified in the tree at the gate, besides `.planning/config.json` and `.planning/state.json`. It is orchestrator-managed state, was already modified at the phase start (E-01 `dirty_paths`), is not part of any committed tree the gates read, and is never staged by this phase; it is recorded here, not treated as a stop.

What Phase 35 delivered: the three-site fix of buffered offset commit under topic-renaming SMTs (`ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java`, `ExactlyOnceBufferStrategy.java`) with 13 Docker-free pins that were red on the unchanged tree and are mutation-proved per file; the BLD-16 re-bless for the three `SinkRecord` accessors, shown present in the production worker image's jar; 7 anchor rows (141 to 148) and the FORK-DELTA row; the live pin on ClickHouse `latest` with client V2 (4 executions); the BUF-03 record with probes P1 to P7, the state-key tripwire and the open items; and the rollout item for Phase 38 with its evidence labels (E-08). Open items for the milestone close: OI-35-1 (the Direct path with `reportInsertedOffsets=true` under a renaming SMT commits nothing; the one-line fix is measured and not applied by decision), OI-35-2 (the many-to-one state-key hazard; any fix changes the state key and stops for a user decision) and OI-35-3 (exactly-once buffering under a partition-changing SMT, read from the code). Nothing is pushed: origin `custom_build` is unchanged, and the tag `pre-v1.7.0-tail-2` and the branch `sync/v1.7.0` exist only locally. This tip is the baseline for Phase 36, the client bump.

Timestamp (UTC): 2026-10-29T09:49:30Z to 2026-10-29T10:14:38Z

Verdict: pass
