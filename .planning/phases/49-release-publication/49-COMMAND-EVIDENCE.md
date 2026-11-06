# Phase 49: Command evidence

- **Base commit:** `local_head` = `816d3b554045c5e1368b5e5090d8674695596cb4` (HEAD when plan 49-01 started, branch `sync/v1.7.0`); this is L0.
- **The U-2 merge:** `merge_commit` = `77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f` (B1), the local tree-neutral merge of `94834763` into `sync/v1.7.0`; its tree equals L0's tree.
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-06.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence. Every expected number is re-measured here; a measured value that differs from the expectation is recorded and used, never forced.
- **Helpers:** `49-evidence-check.py` (`7123d354f51d45e019837f43b3a30fbc`), `49-testcase-status.py` (`41f14e07ae7b402ad2e29bfc381a2aff`), `49-allowlist-check.py` (`886006e1302ff7f9b0c2ac023639a38d`), `49-hygiene-check.py` (`8d18a641065b320e28e571a19679ab98`), `49-doc-check.py` (`110fd44a2495ebd4b9845675a0419ad7`), `49-path-check.py` (`641402547d365dc5b663d6c88d6e1604`), `49-id-scan.py` (`0922b7a9ab304cb8a77f181798827ed6`), `49-review-check.py` (`4fce996a3b9e8676bf7b5a4f7100a613`) and `49-note-check.py` (`4773618affcaf355632262823a1723a9`) are read-only plan-time files; they are never edited.
- **Evidence ids:** plans make E-01 to E-20; E-15 to E-19 are the pending records of the five user handoffs and stay pending; E-20 is the local close; the tail from E-21 on is recorded by the orchestrator.
- **No push happens in plans 49-01 to 49-05.** Reads use `git ls-remote`, `gh api` GET and `curl -s` GET only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RLS-01 | 49-01 | pass |
| E-02 | RLS-01 | 49-01 | pending (plan 49-01) |
| E-03 | RLS-01 | 49-01 | pending (plan 49-01) |
| E-04 | RLS-01, RLS-04 | 49-01 | pending (plan 49-01) |
| E-05 | RLS-02, RLS-03 | 49-02 | pending (plan 49-02) |
| E-06 | RLS-03 | 49-02 | pending (plan 49-02) |
| E-07 | RLS-03 | 49-02 | pending (plan 49-02) |
| E-08 | RLS-03 | 49-03 | pending (plan 49-03) |
| E-09 | RLS-03 | 49-03 | pending (plan 49-03) |
| E-10 | RLS-03 | 49-03 | pending (plan 49-03) |
| E-11 | RLS-02 | 49-04 | pending (plan 49-04) |
| E-12 | RLS-04 | 49-04 | pending (plan 49-04) |
| E-13 | RLS-04 | 49-04 | pending (plan 49-04) |
| E-14 | RLS-04, RLS-05 | 49-05 | pending (plan 49-05) |
| E-15 | RLS-02, RLS-05 | 49-05 | pending (plan 49-05) |
| E-16 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-17 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-18 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-19 | RLS-06 | 49-05 | pending (plan 49-05) |
| E-20 | RLS-01 to RLS-06 | 49-05 | pending (plan 49-05) |

### E-01: Pre-flight, the tree-neutral merge of the gg7 release merge, the retag of pre-v1.7.0_gg8 and the tail gate on the merge

Backs: RLS-01, U-1 (the local retag of `pre-v1.7.0_gg8`), U-2 (the local tree-neutral merge of the published gg7 merge), D-8 (upstream and divergence rechecked before any change), PL-8 (the tail gate on the merge commit with the retagged `PRE_TAG`).

```sh
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
python3 -I (md5 of the nine plan-time helpers)
bash .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-assets/hygiene-selftest.sh.txt .planning/phases/49-release-publication/49-hygiene-check.py <new dir>
colima list
python3 -I .planning/phases/49-release-publication/49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git diff --quiet a968e4867f374f09177faadd80f1462b9a146233 HEAD -- . ':(exclude).planning'
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git rev-list origin/custom_build --not HEAD
git rev-list --count HEAD --not --remotes=origin
git ls-remote --heads origin
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
git tag -l 'v1.7.0_gg8'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50' --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '[.[] | select(.tag_name=="v1.7.0_gg7") | "\(.id) \(.tag_name) \(.draft)"] | join("; ")'
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>   # n = 861, 855, 854, 788
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
sysctl vm.swapusage
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg8
git merge-tree --write-tree 9483476322e0d331e81c5b92ac266a18f0f01450 HEAD
git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <msg file> 9483476322e0d331e81c5b92ac266a18f0f01450
git tag -f pre-v1.7.0_gg8 9483476322e0d331e81c5b92ac266a18f0f01450
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
```

```text
branch: sync/v1.7.0
local_head: 816d3b554045c5e1368b5e5090d8674695596cb4
dirty_paths: M .planning/config.json; M .planning/state.json
helpers_md5: ok
helpers_tracked: yes
hygiene_selftest: selftest_default_hits=7 selftest_strict_hits=9
colima_vz: Running
python_version: Python 3.14.8
git_version: git version 2.50.1 (Apple Git-155)
version_file: v1.7.0_gg7
anchors_version_row: VERSION|v1.7.0_gg7|1|BLD-07
anchors_mirror: rows=218 bad=0
tree_equals_phase48_gate: yes
changelog_tail_equals_gg7: yes
origin_custom_build: 9483476322e0d331e81c5b92ac266a18f0f01450
origin_tracking_equal: yes
left_right_before: 1 223
left_count_before: 1
left_commit_before: 9483476322e0d331e81c5b92ac266a18f0f01450
unpublished_before: 223
origin_heads: 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; 94834763 refs/heads/custom_build; bec167d8 refs/heads/main; de8c685b refs/heads/scratch/clt-07-client-0.11.0
origin_sync_ref: none
origin_gg8_tags: none
local_release_tag: none
scratch_tips: de8c685b scratch/clt-07-client-0.11.0; 939e8992 scratch/clt-07-prebump-0.9.6
scratch_tips_ancestors: yes
origin_open_prs: none
releases_gg7: 433237218 v1.7.0_gg7 false
upstream_main: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_newest_tag: v1.7.0
upstream_tag_past_v170: no
upstream_pr_861: open False b62e0ae4
upstream_pr_855: open False ddabf28b
upstream_pr_854: open False 69d1efc5
upstream_pr_788: closed False bb4b4140
upstream_prs_unchanged: yes
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_matches_research: yes
swap_used: vm.swapusage: total = 35840.00M  used = 34860.25M  free = 979.75M  (encrypted)
baseline_before_merge: v1.7.0_gg6
merge_tree_probe: yes
merge_commit: 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
merge_parents: 816d3b554045c5e1368b5e5090d8674695596cb4 9483476322e0d331e81c5b92ac266a18f0f01450
merge_tree_equal_first_parent: yes
merge_status_after: M .planning/config.json; M .planning/state.json
left_right_after: 0 224
left_count_after: 0
origin_ahead_none: yes
pre_tag_before: baa404cd5ed95c556db0acff84def1024c3bafe3
pre_tag_after: 9483476322e0d331e81c5b92ac266a18f0f01450
pre_tag_type: commit
pre_tag_on_origin: absent
baseline_after_merge: v1.7.0_gg7
tail_gate_merge_end: 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
tail_gate_merge_line: invariants: provenance_paths=11 anchor_commits=206 anchor_rows=218 marker_commits=224 anchor_exempt=0 result=PASS
tail_gate_merge_exit: 0
```

The planning HEAD read `1 222` (plan-time); it read `1 223` here because the plan commit for this phase sits one commit later. The one left commit is the published gg7 merge. `origin_heads` shows the origin `main` as `bec167d8` (the fork's own `main`, unrelated to upstream `main`). Swap was 97 % used when the pre-flight ran. The tail gate ran alone, with no Gradle or Docker job alive, and its counts (`anchor_commits=206`, `marker_commits=224`) are at or above the plan-time probe's 204 and 222, with `anchor_rows=218` and `result=PASS`. The merge is the first change of the plan; the local tag `pre-v1.7.0_gg8` moved from `baa404cd` to `94834763` only after it; neither was pushed.

2026-11-06T13:07:02Z
Verdict: pass
