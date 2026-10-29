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
| E-05 | BUF-02, BUF-05 | 35-02 | pending (plan 35-02) |
| E-06 | BUF-03 | 35-02 | pending (plan 35-02) |
| E-07 | BUF-03 | 35-02 | pending (plan 35-02) |
| E-08 | BUF-07 | 35-03 | pending (plan 35-03) |
| E-09 | BUF-01, BUF-02, BUF-03, BUF-04, BUF-05, BUF-07 | 35-03 | pending (plan 35-03) |

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
