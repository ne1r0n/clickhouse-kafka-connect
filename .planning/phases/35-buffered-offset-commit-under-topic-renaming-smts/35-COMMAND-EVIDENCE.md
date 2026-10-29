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
| E-03 | BUF-01, BUF-02, BUF-07 | 35-01 | pending (plan 35-01) |
| E-04 | BUF-01, BUF-02, BUF-04 | 35-01 | pending (plan 35-01) |
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
