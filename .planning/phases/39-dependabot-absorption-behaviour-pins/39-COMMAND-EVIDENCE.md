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
| E-02 | DEP-01 | 39-01 | pending (plan 39-01) |
| E-03 | DEP-01 | 39-01 | pending (plan 39-01) |
| E-04 | DEP-02 | 39-02 | pending (plan 39-02) |
| E-05 | DEP-02 | 39-02 | pending (plan 39-02) |
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
