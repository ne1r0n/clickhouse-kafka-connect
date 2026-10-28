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
| E-05 | SWP-01 | 33-01 | pending (plan 33-01) |
| E-06 | SWP-04 | 33-01 | pending (plan 33-01) |
| E-07 | SWP-02 | 33-02 | pending (plan 33-02) |
| E-08 | SWP-02 | 33-02 | pending (plan 33-02) |
| E-09 | SWP-02 | 33-02 | pending (plan 33-02) |
| E-10 | SWP-02 | 33-02 | pending (plan 33-02) |
| E-11 | SWP-01, SWP-02 | 33-02 | pending (plan 33-02) |
| E-12 | SWP-03 | 33-03 | pending (plan 33-03) |
| E-13 | SWP-03 | 33-03 | pending (plan 33-03) |
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
