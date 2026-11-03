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
| E-06 | PIN-02 | 39-03 | pass |
| E-07 | PIN-03 | 39-03 | pass |
| E-08 | PIN-04 | 39-04 | pass |
| E-09 | PIN-04 | 39-04 | pass |
| E-10 | DEP-01, DEP-02, PIN-02, PIN-03, PIN-04 | 39-04 | pass |
| E-11 | DEP-01, DEP-02, PIN-02, PIN-03, PIN-04 | 39-04 | pass |

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

### E-06: PIN-02 wrapped-DataException matrix through ProxySinkTask.put (14 cases, Docker unreachable)

Backs: PIN-02, ROADMAP Phase 39 criterion 3 (tests drive the real `ProxySinkTask.put` under both `errors.tolerance` values and assert, per named shape, whether `put` throws a `RetriableException`, fails the task or routes the records to the DLQ, passing on unchanged code; the conversion-time row is pinned as a control), orchestrator decision 2.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd39/exec/sc-pin02.out 2> /tmp/gsd39/exec/sc-pin02.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 --tests '*WrappedDataExceptionClassificationPinUnitTest' > /tmp/gsd39/exec/pin02.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit --class WrappedDataExceptionClassificationPinUnitTest
git add src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java docs/FORK-DELTA.md
git commit -F /tmp/gsd39/exec/msg-pin02.txt
git diff --quiet 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- src/main
git diff --quiet 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
```

```text
pin02_commit: 6f067dbae6e88e8c19f2adc3c5801f9be8240906
pin02_commit_files: docs/FORK-DELTA.md src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java
test_class: com.clickhouse.kafka.connect.sink.WrappedDataExceptionClassificationPinUnitTest
cases: 14
junit_class_line: OK junit build/test-results/testUnit class *WrappedDataExceptionClassificationPinUnitTest: tests=14 failures=0 errors=0 skipped=0 files=1
docker_unreachable: yes (DOCKER_HOST=unix:///nonexistent/docker.sock)
src_main_diff_vs_phase_start: 0
additional_retriable_pin_unchanged: yes
fork_delta_known_cost_17: updated
spotless_scoped_check: IS CLEAN
```

All 14 cases passed on the first run against unchanged `src/main`; no expectation was edited. The class has one parameterized test, extends nothing, carries no tag and uses no mocking framework (a `FailingDBWriter` subclass of `InMemoryDBWriter` injects the failure, `InMemoryDLQ` captures the reports). Outcome table, as measured (research F6) and as asserted literally:

| Shape | errors.tolerance=none | errors.tolerance=all |
|-------|-----------------------|----------------------|
| WRAPPED_DATA_EXCEPTION (plain writer DataException) | FAILS_TASK (`Number of records: 3`, cause chain holds the injected DataException under the exact-RuntimeException queryId wrapper, DLQ empty) | DLQ (all 3 records reported, chains hold the injected DataException) |
| RETRIABLE_SERVER_CODE_IN_CHAIN (DataException caused by server code 241) | RETRIABLE (DLQ empty) | RETRIABLE (DLQ empty) |
| SOCKET_TIMEOUT_ROOT (DataException caused by SocketTimeoutException) | RETRIABLE | RETRIABLE |
| NON_RETRIABLE_SERVER_CODE_CONTROL (server code 60) | FAILS_TASK | DLQ |
| UNKNOWN_HOST_ROOT_CONTROL (UnknownHostException root) | RETRIABLE | RETRIABLE |
| RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL (RuntimeException with SocketTimeoutException root) | RETRIABLE | RETRIABLE |
| CONVERSION_DATA_EXCEPTION_CONTROL (Integer value rejected by `Record.convert` before Processing) | ESCAPES_RAW (raw DataException, writer never called, DLQ empty) | ESCAPES_RAW |

`FORK-DELTA` Known cost 17 now states these put-layer outcomes and names the class in the same commit; the clause that said no test asserts the cases is gone. The sibling `AdditionalRetriableErrorCodesPinUnitTest` is byte-unchanged since the phase start.

2026-11-01T19:21:00Z

Verdict: pass

### E-07: PIN-03 client-version tripwire and the full testUnit with both pins (Docker unreachable)

Backs: PIN-03, ROADMAP Phase 39 criterion 4 (a Docker-free test shows the doubled-encoding `custom_*` forwarding of client 0.11.0 and its name and failure message call it a client-version tripwire), and the testUnit baseline with both pins.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd39/exec/sc-pin03.out 2> /tmp/gsd39/exec/sc-pin03.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 --tests '*ClientEndpointAndOptionsPinUnitTest' > /tmp/gsd39/exec/pin03.log 2>&1
git add src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
git commit -F /tmp/gsd39/exec/msg-pin03.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency com.clickhouse:client-v2 --configuration testRuntimeClasspath
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 > /tmp/gsd39/exec/pins-testunit.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit --class ClientEndpointAndOptionsPinUnitTest
git diff --quiet 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- src/main
```

```text
pin03_commit: 59c5975e30d36d2221d8c0aece0e36dd07da59be
pin03_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
client_v2_version: com.clickhouse:client-v2:0.11.0
client_line: V0_11_0
tripwire_class_line: OK junit build/test-results/testUnit class *ClientEndpointAndOptionsPinUnitTest: tests=5 failures=0 errors=0 skipped=0 files=1
testunit_exit: 0
testunit_tests: 1687
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_e05: 15
src_main_diff_vs_phase_start: 0
spotless_scoped_check: IS CLEAN
```

The test is additive: the diff of `ClientEndpointAndOptionsPinUnitTest.java` against the phase start has no removed line (three imports, four constants, `rawQueryMap` and the new test were added). On client 0.11.0 it asserts from parsed raw query values, never the raw URI: `custom_myparam` is `7`, `custom_settings` is `max_threads%253D3`, `custom_a` is `b%2520c`, the keys request has no `ssl` key and has a `query` key; the 0.9.6 branch (measured in Phase 36 E-11, not re-measured) asserts that no `custom_` key is forwarded. The client line is read inside `assertDoesNotThrow`, so an unknown client fails with the `client-version tripwire` text that names `docs/ROLLOUT-v1.7.0_gg6.md` row D7. All 5 tests of the class passed on the first run, with no expectation edited.

The full `testUnit` with `--rerun` and Docker unreachable ran 1687 tests in 81 XML files: 0 failures, 0 errors, 2 skipped. That is E-05's 1672 plus 15 (14 PIN-02 cases and 1 PIN-03 test). No JVM crash file appeared in the repository root. Nothing was pushed.

Correction note: E-06's closing timestamp first read `2026-11-01T19:25:00Z`, an estimate that is later than its own commit `a30e7c9a` (19:21:35Z); it was corrected to the `date -u` reading `19:21:00Z` in this commit. No result, key or verdict of E-06 changed.

Amend note: the PIN-03 commit was first made as `aaa1bacc` with `assertDoesNotThrow(` and its first argument on separate lines; the plan gate needs `assertDoesNotThrow(ClientErrorShapes::line` on one line, so the unpushed commit was amended (whitespace only, scoped Spotless check re-run: IS CLEAN) to `59c5975e`, and the class test and the full testUnit were re-run on it. The evidence above is from those re-runs.

2026-11-01T19:24:00Z

Verdict: pass

### E-08: F-3 harness fix (src/integrationTest only), the leftover-connector pin seen red against a cross-connector count, then green live (PIN-04)

Backs: PIN-04, ROADMAP Phase 39 criterion 5 (the harness counts Datagen completion only on the connector it started; the fix touches only `src/integrationTest`), orchestrator decision 1 (at HEAD `generateData` already counted by connector name, so the deliverable is the leftover-connector pin, the removal of the all-connectors accessor and the Phase 36 review IN-06 hygiene), user decisions on formatting and red-first.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileIntegrationTestJava -PjavaVersion=21
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java"
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java" -PspotlessIdeHookUseStdOut > /tmp/gsd39/exec/sc-cp.out 2> /tmp/gsd39/exec/sc-cp.err
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd39/exec/sc-it.out 2> /tmp/gsd39/exec/sc-it.err
grep -r getConnectors src/integrationTest
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
git add src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java
git commit -F /tmp/gsd39/exec/msg-pin04.txt
git diff -- src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java > /tmp/gsd39/exec/pin04-red.patch
shasum -a 256 /tmp/gsd39/exec/pin04-red.patch
./gradlew --stop
rm -rf build/test-results/integrationTest
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest -PjavaVersion=21 --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkConnectorIntegrationTest.datagenCompletionCountIgnoresLeftoverConnectorTest' > /tmp/gsd39/exec/integration-pin-red.log 2>&1
git checkout -- src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
git diff --quiet HEAD -- src/integrationTest
git status --porcelain -- src
./gradlew --stop
rm -rf build/test-results/integrationTest
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest -PjavaVersion=21 --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkConnectorIntegrationTest.datagenCompletionCountIgnoresLeftoverConnectorTest' > /tmp/gsd39/exec/integration-pin.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/integrationTest --class ClickHouseSinkConnectorIntegrationTest
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
pin04_commit: 4bbc2bac16db1f9138829c236de06a3af66bffc3
pin04_commit_files: src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
compile_integration_exit: 0
accessor_removed: yes
spotless_scoped_check_cp: IS CLEAN
cp_import_set_unchanged: yes
it_imports_byte_identical: yes
it_nonimport_lines_formatter_clean: yes
spotless_waiver_it: pre-existing import-order violation, also at phase start; import lines left byte-identical by user decision; non-import lines formatter-clean
anchors_mirror: rows=172 bad=0
red_variant_patch_sha256: 9008fd397871a34018b18c08430e5153aadc69098767f32cca806690abb3d154
red_run_exit: 1
red_testcase_failed: yes
red_run_failure: org.opentest4j.AssertionFailedError: A scoped count must not include the other finished connector's tasks; with both connectors present, a count across connectors would read 30 for each ==> expected: <10> but was: <30>
red_variant_reverted: yes
single_run_exit: 0
single_run_xml: OK junit build/test-results/integrationTest class *ClickHouseSinkConnectorIntegrationTest: tests=1 failures=0 errors=0 skipped=0 files=1
single_run_info_lines: 1 (datagenAfterLeftoverTopic 10/10, the pin's own generateData; the two kept connectors are started with startDatagenConnector, which logs no completion line)
clickhouse_version: 26.10.3.7
```

What the fix changes, in `ConfluentPlatform.java`. `countFinishedDatagenTasks(connectorName)` reads only `/connectors/{name}/status` of the connector it is given and counts each task once whose `trace` holds the `DATAGEN_FINISHED_MARKER` match, so a trace that repeats the marker cannot inflate the count. `generateData` starts the connector with `startDatagenConnector` and waits in `awaitDatagenCompletion`, which loops while the count is less than the task total and fewer than `DATAGEN_MAX_POLLS` polls have run, logs `Datagen connector {}: {} of {} tasks finished, counted from its own status endpoint only`, and throws `Data generation failed` below the total. Its `finally` deletes the connector inside its own `try/catch (IOException)` that logs `Could not delete Datagen connector {}`, so a delete failure cannot replace a propagating generation failure. The only `Pattern.compile` left is the constant. `awaitAllTasksFailed` waits on task state only (every task `FAILED`, the expected task count present), so the pin's completion wait is independent of the count under test. The all-connectors accessor `getConnectors()` is deleted; it read the status-and-info endpoint, whose `info` part carries every connector's config, and its two call sites in `ClickHouseSinkConnectorIntegrationTest` now log `Sink connector task status: {}` for the sink connector only (T-39-13). `getFirstTaskFailureOpt` uses the new `TASK_STATE_FAILED` and `TASK_TRACE_KEY` constants instead of repeating the literals.

The scoped Spotless hook in write mode on `ConfluentPlatform.java` only reordered its imports (the sorted `import` lines equal those at the phase start); the check form then printed `IS CLEAN`. `ClickHouseSinkConnectorIntegrationTest.java` was never given the write form: its check form prints `IS DIRTY` for the import order that was already wrong at the phase start, its import lines are byte-identical to the phase start, and its non-import, non-blank lines equal those of the formatted output. Waiver: the pre-existing import-order violation of that file is left, by user decision. The fix commit touches only the two files under `src/integrationTest/`; `src/main` and `src/test` are unchanged.

Red first. With the fix committed and `git status --porcelain -- src` empty, the body of `countFinishedDatagenTasks` was replaced, uncommitted, by a GET of `/connectors?expand=status` that returns the number of `DATAGEN_FINISHED_MARKER` matches in the whole response body, which is the counting that spans all connectors (F-3). The single pin was run against it on ClickHouse `latest` with client V2 (19:33:40Z to 19:35:19Z, exit 1) and failed at its first count assertion: both kept connectors had finished, so the leftover connector's count read 30 (10 plus 20) where its own task total is 10. The scratch patch is the committed fix's diff against the working tree, saved before the run (SHA-256 above):

```diff
diff --git a/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java b/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
index a624816f..748e1fe8 100644
--- a/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
+++ b/src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
@@ -503,15 +503,14 @@ public class ConfluentPlatform {
      * status endpoint (/connectors/{name}/status). A task counts once however often its trace repeats the marker.
      */
     public long countFinishedDatagenTasks(String connectorName) throws IOException {
-        long tasksWithMarker = 0;
-        for (Object task : getConnectorTasksStatus(connectorName)) {
-            JSONObject jsonTaskObj = (JSONObject) task;
-            if (jsonTaskObj.has(TASK_TRACE_KEY)
-                    && DATAGEN_FINISHED_MARKER.matcher(String.valueOf(jsonTaskObj.get(TASK_TRACE_KEY))).find()) {
-                tasksWithMarker++;
-            }
+        OkHttpClient client = new OkHttpClient();
+        Request request = new Request.Builder()
+                .url(getConnectRestEndPoint() + "/connectors?expand=status")
+                .get()
+                .build();
+        try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
+            return DATAGEN_FINISHED_MARKER.matcher(responseBody.string()).results().count();
         }
-        return tasksWithMarker;
     }
 
     public long awaitDatagenCompletion(String connectorName, int totalWorkers) throws IOException, InterruptedException {
```

The scratch change was reverted with `git checkout --` on that one file: `git diff --quiet HEAD -- src/integrationTest` exited 0 and `git status --porcelain -- src` was empty. The green single run that follows was made on that reverted tree (19:35:25Z to 19:37:12Z, `BUILD SUCCESSFUL in 1m 46s`, exit 0): the pin's testcase passed, the XML holds 1 test with 0 failures, 0 errors and 0 skipped, and the pin's own `generateData` logged `10 of 10` for its connector while both kept connectors were still present. `SELECT version()` on the `latest` image read 26.10.3.7 with digest `sha256:739e099abd11...30b1ce`, the same as E-03. No JVM crash file appeared in the repository root.

Amend note: the fix was first committed as `16e69d54`, and the plan's gate (`grep -E 'finished != totalWorkers|long finished = 0|loopCount < 120'`, meant for the dead initialiser) also matched the legitimate accumulator `long finished = 0` in `countFinishedDatagenTasks`. The unpushed commit was amended to `4bbc2bac` with that local variable renamed `tasksWithMarker` (no behaviour change), the scoped Spotless check was re-run (`IS CLEAN`), and the red run (first run: 19:27:36Z to 19:29:44Z, same `expected: <10> but was: <30>`) and the green run (first run: 19:30:03Z to 19:31:56Z) were repeated on `4bbc2bac`. The patch, its SHA-256, the times and the commit above are from those repeats.

Gate note: the plan's check `git diff --quiet "$S" HEAD -- src/main src/test` cannot hold, because plan 39-03 added `WrappedDataExceptionClassificationPinUnitTest` and edited `ClientEndpointAndOptionsPinUnitTest` under `src/test` after the phase start. The check that matches the plan's prohibition (this plan changes no file under `src/main` or `src/test`) was run instead: `git diff --quiet "$S" HEAD -- src/main` exits 0, and `git diff --quiet 46459dc54f2cc93706ec7bc3753cf8547cf8f688 HEAD -- src/main src/test` exits 0, where `46459dc5` is HEAD when plan 39-04 started.

2026-11-01T19:37:33Z

Verdict: pass

### E-09: Recorded integrationTest run of ClickHouseSinkConnectorIntegrationTest on latest x V2 (PIN-04)

Backs: PIN-04, ROADMAP Phase 39 criterion 5 (one recorded `integrationTest` run shows the harness counting only the connector it started), orchestrator decision 1 (the run is the `ClickHouseSinkConnectorIntegrationTest` class, not the whole `integrationTest` task).

```sh
git rev-parse HEAD
grep -rln 'generateData\|ConfluentPlatform' src/integrationTest
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
/opt/homebrew/Cellar/docker/29.8.2/bin/docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/integrationTest
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest -PjavaVersion=21 --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkConnectorIntegrationTest' > /tmp/gsd39/exec/integration-class.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/integrationTest --class ClickHouseSinkConnectorIntegrationTest
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
run_head: 31b1721427915576bb631e59a6c75812725fd8b0
run_command: CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest -PjavaVersion=21 --tests 'com.clickhouse.kafka.connect.sink.ClickHouseSinkConnectorIntegrationTest'
run_exit: 0
run_duration: BUILD SUCCESSFUL in 29m 49s
class_tests: 55
class_failures: 0
class_errors: 0
class_skipped: 0
pin_test_result: passed
datagen_info_lines: 8
datagen_info_all_equal: yes
datagen_info_counts: datagenAfterLeftoverTopic 10/10; stockGenMultiTaskInterruptTest 30/30; stockGenMultiTaskSchemalessTest 30/30; stockGenMultiTaskTopicTest 30/30; stockGenSingleTaskInterruptTest 10/10; stockGenSingleTaskSchemalessTest 10/10; stockGenSingleTaskTest 10/10; stockGenWithJdbcPropSingleTaskTest 10/10
scope_decision: class run of ClickHouseSinkConnectorIntegrationTest; every executing caller of the changed helpers is in it: ExactlyOnceTest calls generateData but its @BeforeAll assumeTrue(isCluster || isCloud) skips it on standalone latest; ClickHouseCloudTest (@BeforeAll assumeFalse(isCluster)) runs on standalone but never calls generateData or the removed accessor; no whole-task fallback
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
fork_delta_known_cost_15: updated
```

Scope reading, as the plan states it: `grep -rln 'generateData\|ConfluentPlatform' src/integrationTest` names only `ClickHouseSinkConnectorIntegrationTest.java`, `ExactlyOnceTest.java` and `ConfluentPlatform.java` itself; `ExactlyOnceTest`'s `@BeforeAll` holds `Assumptions.assumeTrue(isCluster || isCloud, ...)`, `ClickHouseCloudTest`'s holds `Assumptions.assumeFalse(isCluster, ...)`, and `ClickHouseCloudTest` contains no `generateData`, `ConfluentPlatform`, `countFinishedDatagenTasks` or `getConnectors` reference. The reading held, so the run was not widened.

The class ran 55 tests (the 7 stockGen tests, the leftover-connector pin, and the schema and ingest tests of the class) in `1779.9` s of test time, from 19:38:22Z to 20:08:12Z: 0 failures, 0 errors, 0 skipped, the pin's testcase without a `failure`, `error` or `skipped` child. The XML holds 8 distinct `Datagen connector ...: N of N tasks finished, counted from its own status endpoint only` lines, one per `generateData` call (the 7 stockGen tests and the pin's own call), each with equal numbers: 10 of 10 for a one-partition topic, 30 of 30 for a three-partition one. The two connectors the pin keeps are started with `startDatagenConnector` and asserted through `countFinishedDatagenTasks`, so they log no completion line. Connector names carry a UUID that is dropped from the counts above. `SELECT version()` on the `latest` image read 26.10.3.7 before and after the run, with the digest of E-03, so the image did not move. No JVM crash file appeared in the repository root. Nothing was pushed.

`docs/FORK-DELTA.md` Known cost 15 is rewritten in the same commit as this entry: titled `The nightly's Datagen completion counting is scoped to its own connector (closed)`, it states the scoped counting, names `datagenCompletionCountIgnoresLeftoverConnectorTest`, cites E-08 for the red run and this entry for the green one, and keeps the owner and a re-open trigger.

2026-11-01T20:08:40Z

Verdict: pass

### E-10: Phase gate suites: testUnit with Docker unreachable and the full suite on latest x V2

Backs: ROADMAP Phase 39 phase gate (cache-busted `testUnit` with Docker unreachable, and a full suite on ClickHouse `latest` x client V2 with `SELECT version()` recorded, on the final tip), and all five requirements.

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
"$(/usr/libexec/java_home -v 21)/bin/java" -version
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 > /tmp/gsd39/exec/gate-testunit.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/testUnit
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
/opt/homebrew/Cellar/docker/29.8.2/bin/docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd39/exec/gate-full.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py skips build/test-results/test
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
gate_head: bcf4276365c6a12017be27e8a613d545ebf796eb
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1)
docker_unreachable: unix:///nonexistent/docker.sock
testunit_exit: 0
testunit_tests: 1687
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_equals_e07: yes
full_exit: 0
full_tests: 2285
full_failures: 0
full_errors: 0
full_skipped: 18
full_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
full_delta_vs_e03: 15
docker_only_delta: 598
clickhouse_version_before: 26.10.3.7
clickhouse_version_after: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
```

Both runs were made on `gate_head`, the tip after the plan 39-04 evidence commit `bcf42763`, with the results directories cleared first and `--rerun` on the task. The `testUnit` run (Docker unreachable, 20:09:06Z, 81 XML files) ran 1687 tests with 0 failures, 0 errors and 2 skipped, which is E-07's total: plan 39-04 adds no unit test. The full suite (20:09:31Z to 20:12:27Z, `BUILD SUCCESSFUL in 2m 55s`, 128 XML files) ran 2285 tests with 0 failures, 0 errors and 18 skipped, and the skips helper reports exactly the 7 named `@Disabled` methods and the 11 `exactlyOnceStateMismatchTest` cases. 2285 is E-03's second full run (2270) plus the 15 tests that plans 39-03 added (14 PIN-02 cases and 1 PIN-03 test), and 2285 minus 1687 is 598, the Docker-only delta of E-03. The `latest` image read 26.10.3.7 before and after with the digest of E-03, so a difference from E-03 cannot come from a moved tag. No JVM crash file appeared in the repository root. The `integrationTest` task is not part of this gate; its class run is E-09.

2026-11-01T20:13:09Z

Verdict: pass

### E-11: Phase gate: cumulative merge-invariant gate, anchors mirror, src/main and origin

Backs: ROADMAP Phase 39 phase gate (the anchors invariant as mirror and as cumulative gate, an empty `src/main` diff against the phase start and against `origin/custom_build`, origin unmoved), and all five requirements.

```sh
git ls-remote origin refs/heads/custom_build
bash .planning/scripts/verify-merge-invariants.sh 54e63648 bcf4276365c6a12017be27e8a613d545ebf796eb > /tmp/gsd39/exec/invariants-39-04.log 2>&1
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
git diff --quiet 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- src/main
git fetch origin refs/heads/custom_build:refs/remotes/origin/custom_build
git diff --quiet origin/custom_build HEAD -- src/main
git diff --numstat 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- gradle/libs.versions.toml
git diff --numstat 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- .github
git diff --name-only 8d9d1a17cb729efba591f7470776a68a6e9edf5a HEAD -- .github/workflows/integrationTests.yaml .github/workflows/increment-version.yml build.gradle.kts gradle/wrapper src/main
```

```text
invariants_head: bcf4276365c6a12017be27e8a613d545ebf796eb
invariants_start_utc: 2026-11-01T20:09:01Z
invariants_end_utc: 2026-11-01T20:48:31Z
invariants_line: invariants: provenance_paths=21 anchor_commits=1321 anchor_rows=172 marker_commits=1387 anchor_exempt=19 result=PASS
invariants_exit: 0
anchors_mirror: rows=172 bad=0
src_main_diff_vs_phase_start: 0
src_main_diff_vs_origin: 0
origin_custom_build_at_gate: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
catalog_diff_vs_phase_start: gson only
workflow_diff_vs_phase_start: setup-gradle only
pushes: none
```

The cumulative gate ran in its default environment on the explicit SHA `bcf42763...`, which equals E-10's `gate_head`, in the background while the E-10 suites ran, and no commit was made until it ended; it exited 0 with `result=PASS` and no `VIOLATION` line. Origin was read with `git ls-remote` before the gate started and again after it ended: `refs/heads/custom_build` stayed at `72e8ccff...`, the value of E-01, so origin did not move. `src/main` is byte-identical to the phase start and to `origin/custom_build` (both `git diff --quiet` exit 0). Against the phase start, `gradle/libs.versions.toml` changes one line removed and one added, `gson = "2.14.0"` to `gson = "2.15.0"`; `.github` changes only the four fork workflows (`nightly-integration-tests.yaml`, `pre-release.yaml`, `release.yaml`, `tests.yaml`), one line each, the `gradle/actions/setup-gradle` pin from `v6.4.0` (`3f5f9ada...`) to `v6.4.1` (`8da46b53...`); `integrationTests.yaml`, `increment-version.yml`, `build.gradle.kts`, the wrapper and `src/main` have no changed file. Nothing was pushed in Phase 39: every read of origin was `git ls-remote` or a `git fetch` of one named ref. The gate ran for about 39 minutes, against the 17 to 23 minutes the research expected, because the range now holds 1321 anchor commits.

2026-11-01T20:49:09Z

Verdict: pass
