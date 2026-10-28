# Phase 33: Command evidence

- **Base commit:** `local_head` = `7f27114991a0e6971900dde0ce6c070e41e1a704` (HEAD when plan 33-01 started, branch `sync/v1.7.0`). "src/main is unchanged" means `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main`.
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` explicitly.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-28.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. No entry carries a token or credential.
- **Rule:** a fail or pending verdict is fixed by re-measuring, never by editing the evidence.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | SWP-04 | 33-01 | pass |
| E-02 | SWP-01 | 33-01 | pass |
| E-03 | SWP-01, SWP-04 | 33-01 | pass |
| E-04 | SWP-01 | 33-01 | pass |
| E-05 | SWP-01 | 33-01 | pass |
| E-06 | SWP-04 | 33-01 | pass |
| E-07 | SWP-02 | 33-02 | pass |
| E-08 | SWP-02 | 33-02 | pass |
| E-09 | SWP-02 | 33-02 | pass |
| E-10 | SWP-02 | 33-02 | pass |
| E-11 | SWP-01, SWP-02 | 33-02 | pass |
| E-12 | SWP-03 | 33-03 | pass |
| E-13 | SWP-03 | 33-03 | pass |
| E-14 | SWP-01, SWP-02, SWP-03, SWP-04 | 33-03 | pending (plan 33-03) |

### E-01: Phase start, origin re-read, open Dependabot PRs, rollback tag and environment

Backs: SWP-04, ROADMAP Phase 33 criterion 1 (origin re-read and the local rollback tag before the first change).

```sh
colima list
git ls-remote origin refs/heads/custom_build 'refs/pull/*/head'
git ls-remote --heads origin
git fetch origin refs/heads/custom_build:refs/remotes/origin/custom_build
git rev-parse origin/custom_build
curl -sS --fail "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=100"
git tag pre-v1.7.0_gg6 3a3a15a9ae064ffe09c696b8bb87bce23419d463
git rev-parse refs/tags/pre-v1.7.0_gg6
git cat-file -t refs/tags/pre-v1.7.0_gg6
git ls-remote --exit-code --tags origin pre-v1.7.0_gg6
git rev-parse HEAD
git rev-list --left-right --count origin/custom_build...HEAD
git rev-parse '3a3a15a9^{tree}' '41e7d546^{tree}'
git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain --version
git ls-files --eol gradlew.bat
file gradlew.bat
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
origin_custom_build_expected: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
origin_heads_non_dependabot: custom_build 3a3a15a9ae064ffe09c696b8bb87bce23419d463; main bec167d85918e77c11621063000acc6e031ae2fa
pr_heads: #10 011bc42bfe8d8230c430ec8001d481885daec5fa; #16 cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f; #17 6a53525b738d90ee4c98a3761ebebe11421cf1f0; #18 cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd; #19 2a8060389924edcabcac26c3c22e397f7939c098; #20 8786723bca5a0a2d0a014d15c15981386da8345d
open_prs: #20 dependabot[bot] 8786723bca5a0a2d0a014d15c15981386da8345d chore(deps): bump gradle-wrapper from 9.8.1 to 9.8.2; #19 dependabot[bot] 2a8060389924edcabcac26c3c22e397f7939c098 chore(deps): bump at.yawk.lz4:lz4-java from 1.12.0 to 1.12.1; #18 dependabot[bot] cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd chore(deps): bump com.squareup.okhttp3:okhttp from 5.5.0 to 5.5.1; #17 dependabot[bot] 6a53525b738d90ee4c98a3761ebebe11421cf1f0 chore(deps): bump clickHouseDriver from 0.9.6 to 0.11.0; #16 dependabot[bot] cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f chore(deps): bump actions/setup-java from 6.0.1 to 6.1.0 in the github-actions group; #10 dependabot[bot] 011bc42bfe8d8230c430ec8001d481885daec5fa chore(deps): bump com.google.protobuf:protobuf-java from 3.25.9 to 4.36.2
local_head: 7f27114991a0e6971900dde0ce6c070e41e1a704
divergence_left_right: 2 29
release_merge_trees_equal: yes
pre_tag_target: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pre_tag_object_type: commit
pre_tag_on_origin: absent
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
gradle_version: 9.8.1
gradlew_bat_eol: i/lf w/crlf attr/text eol=crlf gradlew.bat
gradlew_bat_file: gradlew.bat: DOS batch file text, ASCII text, with CRLF line terminators
src_main_diff_vs_pre_tag: 0
anchors_mirror: rows=131 bad=0
```

`git ls-remote --exit-code --tags origin pre-v1.7.0_gg6` exited 2 (tag absent on origin). The tree both release merges share is `abb6c93eed3a82638ac8252b21ee9f228cb19c05`. The dirty tree held only `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json`.

Timestamp (UTC): 2026-10-28T18:40:55Z to 2026-10-28T18:41:19Z

Verdict: pass

### E-02: Baseline full suite on the untouched tip (latest x V2)

Backs: SWP-01, ROADMAP Phase 33 criterion 2 (a green baseline before any bump, so a later red is attributable to one change).

```sh
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd33/exec/suite-baseline.log 2>&1
python3 -I /tmp/gsd33/exec/tally.py
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
baseline_head: 7f27114991a0e6971900dde0ce6c070e41e1a704
suite_exit: 0
suite_tests: 1961
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_duration: BUILD SUCCESSFUL in 2m 48s
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
```

The counts are summed from the 107 JUnit XML roots under `build/test-results/test`. The 18 skips are the 7 `@Disabled` methods plus 11 `exactlyOnceStateMismatchTest` cases.

Timestamp (UTC): 2026-10-28T18:41:19Z to 2026-10-28T18:44:07Z

Verdict: pass

### E-03: PR #19 fetched by ref and merged as its own commit (lz4-java 1.12.1)

Backs: SWP-01 and SWP-04, ROADMAP Phase 33 criteria 1 and 2 (the lz4-java security release first, in its own commit).

```sh
git fetch origin refs/pull/19/head
git rev-parse FETCH_HEAD
git log -1 --format=%an 2a8060389924edcabcac26c3c22e397f7939c098
git rev-list --parents -n 1 2a8060389924edcabcac26c3c22e397f7939c098
git diff --name-only 2a8060389924edcabcac26c3c22e397f7939c098^ 2a8060389924edcabcac26c3c22e397f7939c098
git diff -U0 2a8060389924edcabcac26c3c22e397f7939c098^ 2a8060389924edcabcac26c3c22e397f7939c098
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/19
git ls-remote origin refs/heads/custom_build
git merge-tree --write-tree --name-only HEAD 2a8060389924edcabcac26c3c22e397f7939c098
git merge --no-ff -F /tmp/gsd33/exec/msg-19.txt 2a8060389924edcabcac26c3c22e397f7939c098
git rev-parse HEAD HEAD^1 HEAD^2
git diff --name-only HEAD^1 HEAD
git merge-base --is-ancestor 21c51598b06b6f5af58430765d57b1e5f6747c5a HEAD
git merge-base --is-ancestor 3a3a15a9ae064ffe09c696b8bb87bce23419d463 HEAD
git status --porcelain -- gradlew.bat
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
pr19_head: 2a8060389924edcabcac26c3c22e397f7939c098
pr19_head_research: 2a8060389924edcabcac26c3c22e397f7939c098
rebased_since_research: no
pr19_author: dependabot[bot]
pr19_parent: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pr19_files: gradle/libs.versions.toml
pr19_change: lz4Java 1.12.0 -> 1.12.1
pr19_rest_head: 2a8060389924edcabcac26c3c22e397f7939c098
pr19_body_ghsa: GHSA-48mj-8f7c-7qfm
origin_reread_before_merge: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
merge_tree: clean
merge_commit: 6bf1c43be60835b1dcc051b046e9727d5da97fb4
merge_second_parent: 2a8060389924edcabcac26c3c22e397f7939c098
first_parent_diff_files: gradle/libs.versions.toml
absorbed_ancestors: 21c51598b06b6f5af58430765d57b1e5f6747c5a 3a3a15a9ae064ffe09c696b8bb87bce23419d463
gradlew_bat_status: clean
anchors_mirror: rows=131 bad=0
```

The merge also makes the scratch branch contain the published tip `3a3a15a9`, which closes the gg4/gg5 divergence that E-01's `divergence_left_right` showed. The head has exactly one parent, and the merge-tree dry run printed a single tree-OID line (`bce731ca2fefd75d6b113a2daf73dd2f3741583a`).

Timestamp (UTC): 2026-10-28T18:44:39Z (head fetched; merge made within the following 30 seconds)

Verdict: pass

### E-04: lz4-java 1.12.1 on runtimeClasspath, clickhouseDependencies and in the shipped archive

Backs: SWP-01, ROADMAP Phase 33 criterion 2 (dependencyInsight and the built archive carry 1.12.1).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency at.yawk.lz4:lz4-java --configuration runtimeClasspath
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency at.yawk.lz4:lz4-java --configuration clickhouseDependencies
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd33/exec/archive-19.log 2>&1
rm -rf /tmp/gsd33/arch-19 && mkdir -p /tmp/gsd33/arch-19 && unzip -q -o build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip -d /tmp/gsd33/arch-19
find /tmp/gsd33/arch-19 -name '*-confluent.jar'
unzip -p <that jar> META-INF/maven/at.yawk.lz4/lz4-java/pom.properties
```

```text
dependencyinsight_runtimeclasspath: at.yawk.lz4:lz4-java:1.12.1
dependencyinsight_clickhousedependencies: at.yawk.lz4:lz4-java:1.12.1
archive_exit: 0
verify_archive_line: verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
archive_file: build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip
archive_confluent_jars: 1
archive_pom_version: 1.12.1
```

The archive holds exactly one `lib/*-confluent.jar`, and its `META-INF/maven/at.yawk.lz4/lz4-java/pom.properties` reads `groupId=at.yawk.lz4`, `artifactId=lz4-java`, `version=1.12.1`. `verifyConfluentArchive` ran as the `createConfluentArchive` finalizer and passed.

Timestamp (UTC): 2026-10-28T18:45:11Z

Verdict: pass

### E-05: Full suite on the lz4 tip (latest x V2), Lz4RoundTripLiveTest and the anchors mirror

Backs: SWP-01, ROADMAP Phase 33 criterion 2 (the full suite on latest x V2 is green with the 18 named skips after the lz4-java bump).

```sh
git rev-parse HEAD
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd33/exec/suite-lz4.log 2>&1
python3 -I /tmp/gsd33/exec/tally.py
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
# Lz4RoundTripLiveTest XML root: build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.db.helper.Lz4RoundTripLiveTest.xml
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
gated_head: eca4aed33aa7aafffc490b6c6ea6b66f0d73fc33
suite_exit: 0
suite_tests: 1961
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_duration: BUILD SUCCESSFUL in 2m 44s
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_e02: yes
lz4roundtrip_xml: tests=2 failures=0 errors=0 skipped=0
anchors_mirror: rows=131 bad=0
jvm_crash: none
```

The gated head is the evidence commit on top of the PR #19 merge `6bf1c43b`, which is its first parent. The totals equal the baseline in E-02 (1961 tests, 18 skipped) on the same image digest, so the bump changed no test outcome. The run was concurrent with the E-06 gate, which reads only git trees.

Timestamp (UTC): 2026-10-28T18:46:31Z to 2026-10-28T18:49:15Z

Verdict: pass

### E-06: Merge-invariant gate after the lz4 merge (range now holds 21c51598 and 3a3a15a9)

Backs: SWP-04, ROADMAP Phase 33 phase gate (the cumulative merge-invariant gate passes on the range the first merge widened).

```sh
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > /tmp/gsd33/exec/gate-33-01.log 2>&1
```

```text
gate_head: eca4aed33aa7aafffc490b6c6ea6b66f0d73fc33
gate_start_utc: 2026-10-28T18:46:27Z
gate_end_utc: 2026-10-28T19:06:29Z
invariants_line: invariants: provenance_paths=21 anchor_commits=981 anchor_rows=131 marker_commits=1040 anchor_exempt=19 result=PASS
invariants_exit: 0
violation_lines: 0
```

The gate walks every commit in `pre-v1.7.0-sync..HEAD`, which now includes the two release merges and the Dependabot head, and each is checked against its own copy of `fork-anchors.txt`.

Timestamp (UTC): 2026-10-28T19:06:29Z

Verdict: pass

### E-07: PR #18 fetched by ref, merged as its own commit and gated (okhttp 5.5.1)

Backs: SWP-02, ROADMAP Phase 33 criterion 3 (okhttp 5.5.1 in its own commit, with its own gate).

```sh
git ls-remote origin refs/heads/custom_build
git fetch origin refs/pull/18/head
git rev-parse FETCH_HEAD
git log -1 --format=%an cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git rev-list --parents -n 1 cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git diff --name-only cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd^ cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git diff -U0 cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd^ cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git merge-tree --write-tree --name-only HEAD cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git merge --no-ff -F /tmp/gsd33/exec/msg-18.txt cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
git rev-parse HEAD HEAD^1 HEAD^2
git diff --name-only HEAD^1 HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileIntegrationTestJava > /tmp/gsd33/exec/compile-it-18.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --dependency com.squareup.okhttp3:okhttp --configuration integrationTestCompileClasspath
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
pr18_head: cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
pr18_head_research: cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
rebased_since_research: no
pr18_author: dependabot[bot]
pr18_parent: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pr18_files: gradle/libs.versions.toml
pr18_change: okhttp 5.5.0 -> 5.5.1
origin_reread_before_merge: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
merge_tree: clean
merge_commit: e075b2ccec663a6089cddd59bbc2c37a4208e4e3
merge_second_parent: cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd
first_parent_diff_files: gradle/libs.versions.toml
compile_integration_test_exit: 0
dependencyinsight_integrationtestcompileclasspath: com.squareup.okhttp3:okhttp:5.5.1
anchors_mirror: rows=131 bad=0
```

The diff of the head is the one `okhttp = "5.5.0"` to `okhttp = "5.5.1"` version line (line 19); the `okhttp = { module = ... }` library row is unchanged. The merge-tree dry run printed a single tree-OID line (`f765dda9833fe7b879c49dc78d44faadd8338dc7`). The head has exactly one parent, the origin tip.

Timestamp (UTC): 2026-10-28T19:08:50Z (head fetched; merge, compile and insight within the following 30 seconds)

Verdict: pass

### E-08: PR #20 fetched by ref, merged as its own commit and gated (Gradle wrapper 9.8.2, gradlew.bat CRLF intact)

Backs: SWP-02, ROADMAP Phase 33 criterion 3 (the Gradle wrapper bump changes only the properties file, `gradlew.bat` keeps CRLF, and the release-path archive task runs on 9.8.2).

```sh
git ls-files --eol gradlew.bat
file gradlew.bat
git ls-remote origin refs/heads/custom_build
git fetch origin refs/pull/20/head
git rev-parse FETCH_HEAD
git log -1 --format=%an 8786723bca5a0a2d0a014d15c15981386da8345d
git rev-list --parents -n 1 8786723bca5a0a2d0a014d15c15981386da8345d
git diff --name-only 8786723bca5a0a2d0a014d15c15981386da8345d^ 8786723bca5a0a2d0a014d15c15981386da8345d
git diff -U0 8786723bca5a0a2d0a014d15c15981386da8345d^ 8786723bca5a0a2d0a014d15c15981386da8345d
git merge-tree --write-tree --name-only HEAD 8786723bca5a0a2d0a014d15c15981386da8345d
git merge --no-ff -F /tmp/gsd33/exec/msg-20.txt 8786723bca5a0a2d0a014d15c15981386da8345d
git rev-parse HEAD HEAD^1 HEAD^2
git diff --name-only HEAD^1 HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain --version > /tmp/gsd33/exec/gradle-version-20.log 2>&1
curl -sS --fail -L -o /tmp/gsd33/exec/gradle-9.8.2-bin.zip https://services.gradle.org/distributions/gradle-9.8.2-bin.zip
shasum -a 256 /tmp/gsd33/exec/gradle-9.8.2-bin.zip
curl -sS --fail -L https://services.gradle.org/distributions/gradle-9.8.2-bin.zip.sha256
rm -f /tmp/gsd33/exec/gradle-9.8.2-bin.zip
git ls-files --eol gradlew.bat
file gradlew.bat
git status --porcelain -- gradlew gradlew.bat gradle/wrapper
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd33/exec/archive-20.log 2>&1
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
gradlew_bat_eol_before: i/lf w/crlf attr/text eol=crlf gradlew.bat
gradlew_bat_file_before: gradlew.bat: DOS batch file text, ASCII text, with CRLF line terminators
pr20_head: 8786723bca5a0a2d0a014d15c15981386da8345d
pr20_head_research: 8786723bca5a0a2d0a014d15c15981386da8345d
rebased_since_research: no
pr20_author: dependabot[bot]
pr20_parent: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pr20_files: gradle/wrapper/gradle-wrapper.properties
pr20_change: gradle-9.8.1-bin.zip -> gradle-9.8.2-bin.zip
origin_reread_before_merge: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
merge_tree: clean
merge_commit: e4f49dca67bb6d04641eedbb6daf2690a996852c
merge_second_parent: 8786723bca5a0a2d0a014d15c15981386da8345d
first_parent_diff_files: gradle/wrapper/gradle-wrapper.properties
gradle_version_after: 9.8.2
gradle_dist_sha256: 21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607
gradle_dist_sha256_published: 21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607
gradle_dist_sha256_match: yes
gradlew_bat_eol_after: i/lf w/crlf attr/text eol=crlf gradlew.bat
gradlew_bat_file_after: gradlew.bat: DOS batch file text, ASCII text, with CRLF line terminators
wrapper_files_status: clean
archive_exit: 0
verify_archive_line: verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
anchors_mirror: rows=131 bad=0
```

The head changes only the `distributionUrl` line (line 3). The merge-tree dry run printed a single tree-OID line (`d5ae47c785d34a023424ce5bd253448bd81c8413`). The first `./gradlew --version` after the merge downloaded the 9.8.2 distribution; `createConfluentArchive` ran on it and finished `BUILD SUCCESSFUL in 30s`. The scratch zip was deleted by name after the checksum comparison; the `wrapper` task was not run.

Timestamp (UTC): 2026-10-28T19:09:19Z to 2026-10-28T19:16:14Z

Verdict: pass

### E-09: PR #16 fetched by ref, merged as its own commit and pin-guarded (setup-java 6.1.0)

Backs: SWP-02, ROADMAP Phase 33 criterion 3 (every setup-java pin is a full SHA with `# v6.1.0`, the pin guards pass under `testUnit`, and the upstream-identical `integrationTests.yaml` is untouched).

```sh
git ls-remote origin refs/heads/custom_build
git fetch origin refs/pull/16/head
git rev-parse FETCH_HEAD
git log -1 --format=%an cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git rev-list --parents -n 1 cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git diff --name-only cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f^ cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git diff -U0 cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f^ cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git ls-remote --heads origin 'dependabot/github_actions/*'
git ls-remote https://github.com/actions/setup-java refs/tags/v6.1.0 'refs/tags/v6.1.0^{}'
git merge-tree --write-tree --name-only HEAD cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git merge --no-ff -F /tmp/gsd33/exec/msg-16.txt cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
git rev-parse HEAD HEAD^1 HEAD^2
git diff --name-only HEAD^1 HEAD
grep -cF 'actions/setup-java@b843ebf9b913146b38da2eaa7aeddf76074ae1ad # v6.1.0' .github/workflows/{tests,release,pre-release,nightly-integration-tests}.yaml
grep -cF 'actions/setup-java@' .github/workflows/{tests,release,pre-release,nightly-integration-tests}.yaml
git rev-parse HEAD:.github/workflows/integrationTests.yaml
grep -c 'actions/setup-java@v3' .github/workflows/integrationTests.yaml
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' > /tmp/gsd33/exec/pin-guards-16.log 2>&1
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
pr16_head: cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
pr16_head_research: cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
rebased_since_research: no
pr16_author: dependabot[bot]
pr16_parent: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pr16_files: .github/workflows/nightly-integration-tests.yaml,.github/workflows/pre-release.yaml,.github/workflows/release.yaml,.github/workflows/tests.yaml
pr16_change: setup-java v6.0.1 -> v6.1.0 (b843ebf9b913146b38da2eaa7aeddf76074ae1ad), 4 lines
pr16_branch: dependabot/github_actions/github-actions-4430c5f07e
setup_java_tag_commit: b843ebf9b913146b38da2eaa7aeddf76074ae1ad
setup_java_tag_kind: lightweight
origin_reread_before_merge: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
merge_tree: clean
merge_commit: 79f56a5ec6f3cedad4d9ada4c5860344c262d285
merge_second_parent: cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f
first_parent_diff_files: .github/workflows/nightly-integration-tests.yaml,.github/workflows/pre-release.yaml,.github/workflows/release.yaml,.github/workflows/tests.yaml
setup_java_pins: tests.yaml 1/1; release.yaml 1/1; pre-release.yaml 1/1; nightly-integration-tests.yaml 1/1 (pinned v6.1.0 line count / any setup-java line count)
integration_tests_yaml_blob: 3e9b9be050fa05805227b2eee0d1fe8c72df56c0
integration_tests_setup_java_refs: 3
pin_guards_exit: 0
pin_guards_xml: WorkflowActionPinGuardUnitTest 21/0/0/0; TestsWorkflowPinGuardUnitTest 39/0/0/0; NightlyWorkflowGuardUnitTest 58/0/0/0; UpstreamIntegrationWorkflowBytesGuardUnitTest 9/0/0/0
anchors_mirror: rows=131 bad=0
```

The head changes exactly one line in each of the four workflows: the `actions/setup-java@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1` pin becomes `actions/setup-java@b843ebf9b913146b38da2eaa7aeddf76074ae1ad # v6.1.0`. The tag lookup returned a single line and no peeled `^{}` line, so the tag is lightweight and the commit is the tag line's SHA. The merge-tree dry run printed a single tree-OID line (`fbe045c1cda875f20d7ef5b9ad2e1be75a57c737`). `pin_guards_xml` is `Class tests/failures/errors/skipped`. The `FIVE_PINNED` fixture of `TestsWorkflowPinGuardUnitTest` was not edited.

Timestamp (UTC): 2026-10-28T19:16:57Z to 2026-10-28T19:17:18Z

Verdict: pass

### E-10: Full suite on the sweep tip (the PR #16 merge commit, latest x V2)

Backs: SWP-02, ROADMAP Phase 33 criterion 3 (no anchor row moved and the full suite is green on the sweep tip that holds all four bumps).

```sh
git rev-parse HEAD
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd33/exec/suite-sweep.log 2>&1
python3 -I /tmp/gsd33/exec/tally.py
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main
/bin/bash /tmp/gsd33/exec/anchors.sh
```

```text
gated_head: 79f56a5ec6f3cedad4d9ada4c5860344c262d285
suite_exit: 0
suite_tests: 1961
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_duration: BUILD SUCCESSFUL in 2m 43s
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_e05: yes
src_main_diff_vs_pre_tag: 0
anchors_mirror: rows=131 bad=0
jvm_crash: none
```

The gated head is the PR #16 merge commit itself, before any evidence commit on top of it. The counts are summed from the 107 JUnit XML roots under `build/test-results/test` and equal E-02 and E-05 (1961 tests, 18 skipped) on the same image digest, so the three build-input bumps and the action bump changed no test outcome. The run used Gradle 9.8.2 and okhttp 5.5.1.

Timestamp (UTC): 2026-10-28T19:17:23Z to 2026-10-28T19:20:07Z

Verdict: pass

### E-11: FORK-DELTA refreshed for the four absorbed bumps

Backs: SWP-01, SWP-02 (the fork's re-litigation guard states lz4Java 1.12.1, okhttp 5.5.1 past upstream and setup-java v6.1.0 from values measured in this phase).

```sh
git ls-remote upstream refs/heads/main
git cat-file -e 9bed6f7970670a55c66be8efda246889e1f95bd0^{commit}
git show 9bed6f7970670a55c66be8efda246889e1f95bd0:gradle/libs.versions.toml
git show a6f1df83:gradle/libs.versions.toml
grep -nE '^\s*-?\s*uses:' .github/workflows/tests.yaml
grep -cF 'lz4Java = "1.12.1"' docs/FORK-DELTA.md   # and the other four count gates
git commit docs/FORK-DELTA.md
```

```text
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_main_lz4java: 1.11.4
upstream_main_okhttp: 5.5.0
upstream_a6f1df83_lz4java: 1.11.4
upstream_a6f1df83_okhttp: 5.5.0
tests_yaml_pins: checkout v7.0.1; setup-java v6.1.0; setup-gradle v6.4.0; upload-artifact v7.0.2
baseline_counts: lz4Java 1.12.1 = 0; lz4Java 1.12.0 = 2; setup-java v6.1.0 = 0; setup-java v6.0.1 = 1; open pull requests #4 and #10 phrase = 1; no github-actions pull request observed yet phrase = 1
after_counts: lz4Java 1.12.1 = 2; lz4Java 1.12.0 = 0; setup-java v6.1.0 = 1; setup-java v6.0.1 = 0; open pull requests #4 and #10 phrase = 0; no github-actions pull request observed yet phrase = 0
forkdelta_commit: cefdcb3964865c59c66ffd7446a16b1c253b823c
forkdelta_rows: row 70 (version-catalog pins); row 76 (single-job CI workflow); row 83 (Dependabot); row 117 (retired lz4 capability rule)
```

`git ls-remote upstream` already had its commit locally, so no fetch was needed. No `uses:` version in row 76's parenthesis differed from `tests_yaml_pins` apart from setup-java, so no other action version was corrected. The row 70 GHSA clause is written because E-03's `pr19_body_ghsa` is `GHSA-48mj-8f7c-7qfm`. The docs commit changes 4 lines (the four rows) and touches only `docs/FORK-DELTA.md`.

Timestamp (UTC): 2026-10-28T19:21:39Z

Verdict: pass

### E-12: Protobuf ignore guard red on the unchanged dependabot.yml, then green with the gradle-entry protobuf-java and protoc ignores

Backs: SWP-03, ROADMAP Phase 33 criterion 4 (the gradle entry ignores major versions 4 and above for protobuf-java and protoc, the pin guards still pass).

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*DependabotProtobufIgnoreGuardUnitTest' > /tmp/gsd33/exec/guard-red.log 2>&1
cp build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.ci.DependabotProtobufIgnoreGuardUnitTest.xml /tmp/gsd33/exec/guard-red.xml
# edit .github/dependabot.yml (gradle entry, insert only), then
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*DependabotProtobufIgnoreGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' > /tmp/gsd33/exec/guard-green.log 2>&1
ruby -ryaml -e '<gradle ignore equals the two items, github-actions has no ignore, exclude-paths unchanged>; puts "YAML-OK"'
git worktree add --detach /tmp/gsd33/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd33/fmt --console=plain spotlessApply > /tmp/gsd33/exec/fmt-33-03.log 2>&1
cmp /tmp/gsd33/fmt/src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java
git worktree remove --force /tmp/gsd33/fmt && git worktree prune
git commit -F /tmp/gsd33/exec/msg-33-03-t1.txt
```

```text
red_exit: 1
red_xml: tests/failures/errors/skipped = 11/1/0/0 (failures=1 errors=0), copy at /tmp/gsd33/exec/guard-red.xml
red_failed_tests: dependabot.yml's gradle entry ignores com.google.protobuf:protobuf-java and com.google.protobuf:protoc major versions (SWP-03) (method gradleEntryIgnoresProtobufJavaAndProtocMajorVersions, from the stack frame of the failure)
red_failure_message: .github/dependabot.yml: the gradle entry has no `ignore:` list; it must ignore version-update:semver-major updates of com.google.protobuf:protobuf-java and com.google.protobuf:protoc
red_prediction: gradleEntryIgnoresProtobufJavaAndProtocMajorVersions only
red_prediction_met: yes
green_exit: 0
green_xml: DependabotProtobufIgnoreGuardUnitTest 11/0/0/0; WorkflowActionPinGuardUnitTest 21/0/0/0; TestsWorkflowPinGuardUnitTest 39/0/0/0 (Class tests/failures/errors/skipped)
yaml_semantics: YAML-OK
dependabot_diff: 7 insertions, 0 deletions (git diff --numstat .github/dependabot.yml)
spotless_worktree_cmp: identical
guard_commit: 6ef8e14e10a786cf186fe1ad70aed1bd121c2760
guard_commit_files: .github/dependabot.yml,src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java
fmt_worktree_removed: yes
```

The red run failed on the planned assertion only: the unchanged file has no `ignore:` in the gradle entry. The other ten tests, including the nine fixtures and the real-file github-actions test, passed before the edit. The Edit tool trimmed the whitespace-only line 15 on its first write; it was restored byte-exact before the green run, so the diff is insertions only. The first green run was also executed before that restore and was repeated after it with the same result.

Timestamp (UTC): 2026-10-28T19:24:59Z to 2026-10-28T19:25:44Z (guard commit)

Verdict: pass

### E-13: PR #10 and its failed run re-read, the cause, protobuf in the archive, and the decision recorded

Backs: SWP-03, ROADMAP Phase 33 criterion 4 (PR #10 not merged, the decision recorded in FORK-DELTA with its measured cause and its FUT-03 re-open trigger).

```sh
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls/10
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/39501799049
curl -sS --fail https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/39501799049/jobs
gh auth status -h github.com
gh run view 39501799049 -R ne1r0n/clickhouse-kafka-connect --log-failed > /tmp/gsd33/exec/run-39501799049-failed.log 2>&1
git show 9bed6f7970670a55c66be8efda246889e1f95bd0:gradle/libs.versions.toml
git show 9bed6f7970670a55c66be8efda246889e1f95bd0:.github/dependabot.yml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd33/exec/archive-pb.log 2>&1
unzip -q -o build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip -d /tmp/gsd33/arch-pb
unzip -l /tmp/gsd33/arch-pb/clickhouse-kafka-connect-v1.7.0_gg5/lib/clickhouse-kafka-connect-v1.7.0_gg5-confluent.jar > /tmp/gsd33/exec/arch-pb-listing.txt
grep -c 'com/google/protobuf/' /tmp/gsd33/exec/arch-pb-listing.txt
python3 -I /tmp/gsd33/scripts/edit_forkdelta.py docs/FORK-DELTA.md
git commit -F /tmp/gsd33/exec/msg-33-03-t2.txt
```

```text
pr10_state: open
pr10_merged_at: null
pr10_closed_at: null
pr10_head: 011bc42bfe8d8230c430ec8001d481885daec5fa
pr10_head_ref: dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2
pr10_base_sha: 71ac96600829dd3503447b1ece42f6a074dcb446
run_conclusion: failure
run_head_sha: 011bc42bfe8d8230c430ec8001d481885daec5fa
run_head_equals_pr10_head: yes
run_created_at: 2026-10-20T21:37:01Z
run_failed_steps: job "ClickHouse latest Client version V2 tests": Run tests (failure), Generate coverage summary (failure)
cause_source: gh run view --log-failed, re-read in Phase 33
verifyerror_lines: 29
protobuf_ingest_failed_cases: 27
run_total_failures: 29 (1281 tests completed, 29 failed, 18 skipped); the two others are ClickHouseSinkTaskWithSchemaTest testInt32ToInt64ConversionWithProtobuf and testWritingProtoMessageWithRowBinary
verifyerror_excerpt: java.lang.VerifyError: Bad type on operand stack; Location io/confluent/kafka/schemaregistry/protobuf/ProtobufSchema.toEnum(Lcom/google/protobuf/DescriptorProtos$EnumDescriptorProto;)...; Type 'com/google/protobuf/DescriptorProtos$EnumValueOptions' is not assignable to 'com/google/protobuf/GeneratedMessageV3$ExtendableMessage'
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_libprotobuf: 3.25.9
upstream_kafka_platform_protobuf: 7.9.1
upstream_dependabot_ignore_count: 0
archive_exit: 0
archive_confluent_jars: 1
archive_listing_entries: 7801
archive_protobuf_entries: 0
forkdelta_commit: 093bab9940f36403d1c47383463d110f07e57932
forkdelta_rows: Build and dependencies protobuf row (after the version-catalog pins row); open item 12; Dependabot row clause and Pinned-by cell
pr10_disposition: closing-at-publication
pr10_user_words: none received
swp03_wording_note: ROADMAP criterion 4 governs (PR #10 recorded as closing at publication); REQUIREMENTS.md SWP-03 "is closed with a Dependabot ignore" is slightly stricter; flagged in 33-03-SUMMARY.md for the user to align; SWP-03 text not edited
```

The log was read to key lines only and not pasted. `archive_listing_entries` is the file count on the last line of `unzip -l` (7801), equal to the 7801 inner entries `verifyConfluentArchive` reports; the listing was captured to a file first and counted, with `unzip` exiting 0, so a failed `unzip` cannot read as 0. `createConfluentArchive` reused its up-to-date outputs from the PR #20 run (inputs unchanged since), and `verifyConfluentArchive` ran as its finalizer. No pull request was touched: PR #10 is read only, nothing was closed, labelled or commented, and no `@dependabot` command was posted.

Timestamp (UTC): 2026-10-28T19:26:03Z to 2026-10-28T19:27:24Z (FORK-DELTA commit)

Verdict: pass
