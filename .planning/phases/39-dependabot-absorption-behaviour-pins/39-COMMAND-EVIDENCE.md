# Phase 39: Command evidence

- **Base commit:** `phase_start_sha` = `8d9d1a17cb729efba591f7470776a68a6e9edf5a` (HEAD when plan 39-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so both runs of a pair share one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-11-01.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `39-evidence-check.py` in this directory is a byte copy of Phase 38's `38-evidence-check.py` (get, expect, index, rows, secrets, junit, anchors, skips, failed) and is never edited.
- **No push happens in Phase 39.** Reads use `git ls-remote`, `git fetch` of named refs and unauthenticated GitHub REST GETs only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | DEP-01, DEP-02 | 39-01 | pass |
| E-02 | DEP-01 | 39-01 | pass |
| E-03 | DEP-01 | 39-01 | pass |
| E-04 | DEP-02 | 39-02 | pass |
| E-05 | DEP-02 | 39-02 | pass |
| E-06 | PIN-02 | 39-03 | pending (plan 39-03) |
| E-07 | PIN-03 | 39-03 | pending (plan 39-03) |
| E-08 | PIN-04 | 39-04 | pending (plan 39-04) |
| E-09 | PIN-04 | 39-04 | pending (plan 39-04) |
| E-10 | DEP-01, DEP-02, PIN-02, PIN-03, PIN-04 | 39-04 | pending (plan 39-04) |
| E-11 | DEP-01, DEP-02, PIN-02, PIN-03, PIN-04 | 39-04 | pending (plan 39-04) |

### E-01: Execution-start re-read, absorption mode and environment

Backs: DEP-01, DEP-02, orchestrator decision 3 (origin and both Dependabot PR heads are re-read at execution start, before any merge, and the absorption mode is chosen from what is found).

```sh
mkdir -p /tmp/gsd39/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git fetch origin refs/heads/custom_build:refs/remotes/origin/custom_build
git rev-parse origin/custom_build
git ls-remote origin refs/heads/custom_build refs/pull/21/head refs/pull/22/head
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/21
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/22
curl -sS --fail 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=100'
git rev-list --left-right --count origin/custom_build...HEAD
git diff --quiet 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 HEAD -- . ':!.planning'
git diff --quiet origin/custom_build HEAD -- src/main
colima list
ls -d /opt/homebrew/Cellar/docker/*/bin/docker
/usr/libexec/java_home -v 21
grep distributionUrl gradle/wrapper/gradle-wrapper.properties
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-evidence-check.py .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py
cmp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-evidence-check.py
```

```text
phase_start_sha: 8d9d1a17cb729efba591f7470776a68a6e9edf5a
branch: sync/v1.7.0
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
pr21_head: 14be67ce7b4e97682bdf54cda89b73d45978a2b1
pr22_head: b5cccbd324635d2fb19fc225e09bb59f7fe81590
pr21_state: open
pr22_state: open
pr21_merged_at: null
pr22_merged_at: null
open_prs: #22 dependabot[bot] b5cccbd324635d2fb19fc225e09bb59f7fe81590 build(deps): Bump com.google.code.gson:gson from 2.14.0 to 2.15.0; #21 dependabot[bot] 14be67ce7b4e97682bdf54cda89b73d45978a2b1 build(deps): Bump gradle/actions/setup-gradle from 6.4.0 to 6.4.1 in the github-actions group
absorption_mode: pr-heads
gson_source: refs/pull/22/head
setup_gradle_source: refs/pull/21/head
divergence_left_right: 1 18
nonplanning_tree_equals_published: yes
src_main_diff_vs_origin: 0
dirty_paths: M .planning/STATE.md; M .planning/config.json; M .planning/state.json
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
gradle_wrapper_version: 9.8.2
testcontainers_host_override: set
anchors_mirror: rows=172 bad=0
helper_copy: cmp-equal
```

The re-read matches the research: `origin/custom_build` still reads the expected tip `72e8ccff`, `refs/heads/custom_build` on origin equals it, both Dependabot PRs are open with `merged_at` null and are the only open PRs, and their heads are the ones the research recorded, so the mode is `pr-heads` and nothing on origin was merged in the meantime. `.planning/STATE.md` is dirty in addition to the two files the plan expected; all three paths sit under `.planning/` and are orchestrator-managed, so the dirty-path rule holds. The divergence reads `1 18` against the research's `1 16` and the planning's `1 17`, the growth being local planning commits; the left `1` is the published gg6 merge `72e8ccff`, which is not yet an ancestor of HEAD and enters this branch's history with the first merge. `testcontainers_host_override` is recorded as set only: this shell sets it to a literal that breaks Ryuk, which is why every Gradle run unsets it.

2026-11-01T17:52:01Z

Verdict: pass

### E-02: gson source fetched and merged as its own commit with the FORK-DELTA version-catalog row (DEP-01)

Backs: DEP-01, ROADMAP Phase 39 criterion 1 (gson 2.15.0 arrives from the fetched Dependabot PR #22 head as its own `--no-ff` merge commit, whose first-parent diff is the catalog line plus the gson value of the FORK-DELTA version-catalog row, and `dependencyInsight` resolves 2.15.0 on the runtime classpath).

```sh
git ls-remote origin refs/heads/custom_build
git fetch origin refs/pull/22/head
git rev-parse FETCH_HEAD
git log -1 --format=%an b5cccbd324635d2fb19fc225e09bb59f7fe81590
git rev-parse b5cccbd324635d2fb19fc225e09bb59f7fe81590^1
git diff --name-only b5cccbd324635d2fb19fc225e09bb59f7fe81590^1 b5cccbd324635d2fb19fc225e09bb59f7fe81590
git diff -U0 b5cccbd324635d2fb19fc225e09bb59f7fe81590^1 b5cccbd324635d2fb19fc225e09bb59f7fe81590
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/22
git merge-tree --write-tree --name-only HEAD b5cccbd324635d2fb19fc225e09bb59f7fe81590
git merge --no-ff --no-commit b5cccbd324635d2fb19fc225e09bb59f7fe81590
git add docs/FORK-DELTA.md
git commit -F /tmp/gsd39/exec/msg-22.txt
git rev-parse HEAD^1 HEAD^2
git diff --name-only HEAD^1 HEAD
git rev-list --left-right --count origin/custom_build...HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency com.google.code.gson:gson --configuration runtimeClasspath
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency com.google.code.gson:gson --configuration clickhouseDependencies
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
```

```text
gson_head: b5cccbd324635d2fb19fc225e09bb59f7fe81590
gson_head_research: b5cccbd324635d2fb19fc225e09bb59f7fe81590
rebased_since_research: no
gson_author: dependabot[bot]
gson_parent: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
gson_files: gradle/libs.versions.toml
gson_change: gson 2.14.0 -> 2.15.0
origin_reread_before_merge: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
merge_tree: clean
merge_commit: ac216401a1efef629dd543fdabe49f3144b9af39
merge_first_parent: 4c463a872385d1776bd37bcbc2f497f2768a4159
first_parent_diff_files: docs/FORK-DELTA.md gradle/libs.versions.toml
dependencyinsight_runtimeclasspath: com.google.code.gson:gson:2.15.0
dependencyinsight_clickhousedependencies: com.google.code.gson:gson:2.15.0
anchors_mirror: rows=172 bad=0
divergence_after_merge: 0 21
```

The source was fetched at 2026-11-01T17:52:47Z. It has one parent equal to the re-read origin tip `72e8ccff`, is authored by `dependabot[bot]`, changes only `gradle/libs.versions.toml`, and the changed lines are exactly the removal of `gson = "2.14.0"` and the addition of `gson = "2.15.0"`; the REST `head.sha` of PR #22 equals the fetched head. The research recorded the same head `b5cccbd3`, so it was not rebased. The dry-run merge printed one tree OID line (`7a0d9d6a4f98617ae9ab19726d571a08205793a1`) with exit 0. Only `gradle/libs.versions.toml` was staged after `git merge --no-ff --no-commit`; the FORK-DELTA edit is one removed and one added line (the gson value `2.14.0` to `2.15.0`, the Phase 39 absorption sentence, and `DEP-01` in the last cell of the version-catalog row). The merge's second parent equals `gson_head`, and `72e8ccff` is now an ancestor of HEAD, hence the divergence `0 21`. Both resolved classpaths carry one gson version, 2.15.0.

2026-11-01T17:53:28Z

Verdict: pass

### E-03: gson gate: cumulative merge-invariant gate, testUnit and the full suite twice each on the gson tip (DEP-01)

Backs: DEP-01, ROADMAP Phase 39 criterion 1 (on the gson commit, `testUnit` with Docker unreachable and the full suite on ClickHouse `latest` x client V2 each pass twice, the second run with `--rerun`, with the 18 named skips; the cumulative merge-invariant gate passes on the merge commit; no new test exists yet, so a red here would be attributable to gson).

```sh
git rev-parse HEAD
git diff --quiet ac216401a1efef629dd543fdabe49f3144b9af39 HEAD -- . ':!.planning'
git ls-remote origin refs/heads/custom_build
bash .planning/scripts/verify-merge-invariants.sh 54e63648 ac216401a1efef629dd543fdabe49f3144b9af39 > /tmp/gsd39/exec/invariants-39-01.log 2>&1
"$(/usr/libexec/java_home -v 21)/bin/java" -version
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 > /tmp/gsd39/exec/gson-testunit-1.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit tests>=1
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd39/exec/gson-testunit-2.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit tests>=1
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
/opt/homebrew/Cellar/docker/29.8.2/bin/docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 > /tmp/gsd39/exec/gson-full-1.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/test tests>=1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py skips build/test-results/test
./gradlew --stop
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd39/exec/gson-full-2.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/test tests>=1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py skips build/test-results/test
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
```

```text
gated_head: f2d541752f7ec36611731a549dc7174c6b136a38
gated_tree_equals_merge: yes
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1)
docker_unreachable: unix:///nonexistent/docker.sock
testunit_run1_exit: 0
testunit_run1_tests: 1672
testunit_run1_failures: 0
testunit_run1_errors: 0
testunit_run1_skipped: 2
testunit_run2_exit: 0
testunit_run2_tests: 1672
testunit_run2_failures: 0
testunit_run2_errors: 0
testunit_run2_skipped: 2
testunit_run2_rerun: yes
full_run1_exit: 0
full_run1_tests: 2270
full_run1_failures: 0
full_run1_errors: 0
full_run1_skipped: 18
full_run1_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
full_run2_exit: 0
full_run2_tests: 2270
full_run2_failures: 0
full_run2_errors: 0
full_run2_skipped: 18
full_run2_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
full_run2_rerun: yes
clickhouse_version_before: 26.10.3.7
clickhouse_version_after: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
matches_research_totals: yes
docker_only_delta: 598
jvm_crash: none
anchors_mirror: rows=172 bad=0
origin_reread: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
invariants_head: ac216401a1efef629dd543fdabe49f3144b9af39
invariants_start_utc: 2026-11-01T17:53:59Z
invariants_end_utc: 2026-11-01T18:31:16Z
invariants_line: invariants: provenance_paths=21 anchor_commits=1302 anchor_rows=172 marker_commits=1368 anchor_exempt=19 result=PASS
invariants_exit: 0
```

The gated head is the evidence commit on top of the gson merge `ac216401`, and its non-planning tree equals the merge's tree. The cumulative gate was started in the background on the merge commit's explicit SHA, right after the E-02 evidence commit and before the first test run, and no commit was made until it ended. Both testUnit runs and both full-suite runs exited 0 on one HotSpot 21 JVM line with no crash file in the repository root. Totals equal the research's (testUnit 1672 tests, 2 skipped; full 2270 tests, 18 skipped; Docker-only delta 2270 - 1672 = 598), and the ClickHouse image digest equals the research run's `sha256:739e099abd11...30b1ce`, so a later difference cannot come from a moved `latest` tag. The second run of each pair used `--rerun` after the results directory was cleared, and the totals were tallied from the JUnit XML. The gate took about 37 minutes against the 17 to 23 the research expected, because it overlapped four Gradle runs on a swap-pressured host; it ended `result=PASS` with exit 0 and no `VIOLATION` line, so the range `pre-v1.7.0-sync..HEAD` with a Dependabot merge and the gg6 publication merge `72e8ccff` is shape-clean.

2026-11-01T18:31:30Z

Verdict: pass

### E-04: setup-gradle source merged as its own commit with the FORK-DELTA tests.yaml row; pins, peeled commit and guard classes (DEP-02)

Backs: DEP-02, ROADMAP Phase 39 criterion 2 (setup-gradle 6.4.1 arrives from the fetched Dependabot PR #21 head as its own `--no-ff` merge commit, pinned in the four fork workflows by the peeled commit of `refs/tags/v6.4.1`, with `integrationTests.yaml` untouched, the release/pre-release mutual diff unchanged, the FORK-DELTA `tests.yaml` row changed in the same commit, and the pin-guard classes green).

```sh
git ls-remote origin refs/heads/custom_build refs/pull/21/head
grep -n 'gradle/actions/setup-gradle@' .github/workflows/tests.yaml .github/workflows/nightly-integration-tests.yaml .github/workflows/release.yaml .github/workflows/pre-release.yaml
git fetch origin refs/pull/21/head
git rev-parse FETCH_HEAD
git log -1 --format='author=%an parents=%P' 14be67ce7b4e97682bdf54cda89b73d45978a2b1
git diff --name-only 14be67ce7b4e97682bdf54cda89b73d45978a2b1^1 14be67ce7b4e97682bdf54cda89b73d45978a2b1
git diff -U0 14be67ce7b4e97682bdf54cda89b73d45978a2b1^1 14be67ce7b4e97682bdf54cda89b73d45978a2b1
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/21
git ls-remote https://github.com/gradle/actions.git refs/tags/v6.4.1 'refs/tags/v6.4.1^{}'
git merge-tree --write-tree --name-only HEAD 14be67ce7b4e97682bdf54cda89b73d45978a2b1
git merge --no-ff --no-commit 14be67ce7b4e97682bdf54cda89b73d45978a2b1
git add docs/FORK-DELTA.md
git commit -F /tmp/gsd39/exec/msg-21.txt
git diff --name-only 82202f7da7e8c842cc97543c6cc02c89de620caf^1 82202f7da7e8c842cc97543c6cc02c89de620caf
git diff --numstat 82202f7da7e8c842cc97543c6cc02c89de620caf^1 82202f7da7e8c842cc97543c6cc02c89de620caf
grep -c 'gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1' <four workflows>
grep -rl 3f5f9adaf7d9fecd50b5935e54106014257a94e6 .github
shasum -a 256 .github/workflows/integrationTests.yaml
diff .github/workflows/release.yaml .github/workflows/pre-release.yaml | shasum -a 256
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 --tests '*ReleaseWorkflowGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*DependabotProtobufIgnoreGuardUnitTest' --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' > /tmp/gsd39/exec/guards.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit
curl -sS --fail 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=14be67ce7b4e97682bdf54cda89b73d45978a2b1&per_page=20'
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
```

```text
origin_reread_before_merge: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
setup_gradle_head: 14be67ce7b4e97682bdf54cda89b73d45978a2b1
setup_gradle_head_research: 14be67ce7b4e97682bdf54cda89b73d45978a2b1
rebased_since_research: no
setup_gradle_author: dependabot[bot]
setup_gradle_parent: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
setup_gradle_files: .github/workflows/nightly-integration-tests.yaml .github/workflows/pre-release.yaml .github/workflows/release.yaml .github/workflows/tests.yaml
setup_gradle_change: setup-gradle 3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0 -> 8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1, one line per file, same indentation
action_tag_object: 7296c772c13d12d9871528814b51fb2143c138ef
action_peeled_commit: 8da46b539cd8fe6ab27ca75128919a18d757604c
pinned_is_peeled_commit: yes
merge_tree: clean
merge_commit: 82202f7da7e8c842cc97543c6cc02c89de620caf
first_parent_diff_files: .github/workflows/nightly-integration-tests.yaml .github/workflows/pre-release.yaml .github/workflows/release.yaml .github/workflows/tests.yaml docs/FORK-DELTA.md
pin_counts: tests=1 nightly=1 release=1 pre-release=1
old_pin_files: none
integration_tests_yaml_sha256: 07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109
release_prerelease_diff_sha256: ac7dce11e09bbe30ca41a0f0c65ebaeb6c8a69c5dec6fa0f1bf5cb1147ae8d44
release_prerelease_pin_line_only: yes
guard_classes_exit: 0
guard_classes_files: 6
guard_classes_tests: 184
guard_classes_failures: 0
guard_classes_errors: 0
guard_classes_skipped: 0
github_tests_run_on_head: 41297596686 .github/workflows/tests.yaml success
anchors_mirror: rows=172 bad=0
```

The source was fetched at 2026-11-01T18:33:37Z. It has one parent equal to the re-read origin tip `72e8ccff`, is authored by `dependabot[bot]`, changes only the four fork workflows, and in each the changed lines are exactly the removal of the v6.4.0 `uses:` line and the addition of the same line with the v6.4.1 peeled commit and a `# v6.4.1` comment at identical indentation; the REST `head.sha` of PR #21 equals the fetched head, and the PR was still open and unmerged. The research recorded the same head `14be67ce`, so it was not rebased. The live `ls-remote` shows the plain tag line (the tag object `7296c772...`) and the `^{}` line (the commit `8da46b53...`); the pinned 40-hex is the peeled commit and differs from the tag object. The dry-run merge printed one tree OID line (`68e39ca263fbd5783469e641a2f658b4accd43d8`) with exit 0. Only the four workflows were staged after `git merge --no-ff --no-commit`; the FORK-DELTA edit is one removed and one added line (setup-gradle `v6.4.0` to `v6.4.1` in the `tests.yaml` row, and `DEP-02` after `SWP-02` in its last cell). The merge's second parent equals `setup_gradle_head`. Each workflow changes by one added and one removed line, `integrationTests.yaml` has its planning-time SHA-256, and the release/pre-release diff signature is unchanged, which is the measured form of the reworded criterion 2. The six guard classes ran under `testUnit --rerun` with Docker unreachable on one HotSpot 21 JVM line: 6 XML files, 184 tests, no failure, error or skip, and no test file was edited. The GitHub `tests.yaml` run on the PR head was read only for information. No text in the merge message or the FORK-DELTA row claims a closed vulnerability.

2026-11-01T18:36:00Z

Verdict: pass

### E-05: setup-gradle gate: testUnit with --rerun, anchors mirror and the cumulative merge-invariant gate across both Dependabot merges (DEP-02)

Backs: DEP-02, research Pitfall 9 / A7 (the full `testUnit` with Docker unreachable passes with `--rerun` on the setup-gradle tip, and the cumulative merge-invariant gate passes on an explicit SHA that holds both Dependabot merges, so a merge-shape failure of the setup-gradle merge surfaces before any pin-guard or behaviour test lands).

```sh
git rev-parse HEAD
git merge-base --is-ancestor ac216401a1efef629dd543fdabe49f3144b9af39 686cc49725de8fdbd9d62d1273d07d2442ca9213
git merge-base --is-ancestor 82202f7da7e8c842cc97543c6cc02c89de620caf 686cc49725de8fdbd9d62d1273d07d2442ca9213
git ls-remote origin refs/heads/custom_build
bash .planning/scripts/verify-merge-invariants.sh 54e63648 686cc49725de8fdbd9d62d1273d07d2442ca9213 > /tmp/gsd39/exec/invariants-39-02.log 2>&1
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 > /tmp/gsd39/exec/setup-gradle-testunit.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
git ls-remote origin refs/heads/custom_build
```

```text
gated_head: 686cc49725de8fdbd9d62d1273d07d2442ca9213
testunit_exit: 0
testunit_tests: 1672
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_rerun: yes
invariants_head: 686cc49725de8fdbd9d62d1273d07d2442ca9213
invariants_start_utc: 2026-11-01T18:34:51Z
invariants_end_utc: 2026-11-01T19:12:51Z
invariants_line: invariants: provenance_paths=21 anchor_commits=1309 anchor_rows=172 marker_commits=1375 anchor_exempt=19 result=PASS
invariants_exit: 0
anchors_mirror: rows=172 bad=0
origin_reread: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
scratch_push: none (the first GitHub run of the combined tree and of the dispatch-only workflows nightly-integration-tests.yaml, release.yaml and pre-release.yaml is the Phase 44 rehearsal push)
```

The gated head is the E-04 evidence commit on top of the setup-gradle merge `82202f7d`; the gson merge `ac216401` and the setup-gradle merge are both its ancestors. The cumulative gate was started in the background on that explicit SHA before the test run, with the default environment (`PRE_TAG=pre-v1.7.0-sync`, `UPSTREAM_REF=refs/tags/v1.7.0`), and no commit was made until it ended. The full `testUnit` ran with Docker unreachable and `--rerun` after the results directory was cleared: 1672 tests in 80 XML files, 0 failures, 0 errors, 2 skipped, the same total as E-03's second testUnit run, since no test changed. The gate took 38 minutes and ended `result=PASS` with exit 0 and no `VIOLATION` line, so the range `pre-v1.7.0-sync..HEAD` holding both Dependabot merges and the gg6 publication merge is shape-clean. No JVM crash file appeared in the repository root. Origin still reads `72e8ccff`. Nothing was pushed.

2026-11-01T19:14:00Z

Verdict: pass
