# Phase 44: Command evidence

- **Base commit:** `local_head` = `9a5209c17e5380ec1f675cd8b3225250d3ff41fe` (HEAD when plan 44-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; a JVM crash is handled by the crash rule of the plans (delete the crash files by name, re-run once, then `-PjavaVersion=21`).
- **Docker:** colima profile `vz`; the ClickHouse image is the local `clickhouse/clickhouse-server:latest`, never pulled.
- **Date (UTC):** 2026-11-02.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new entry, never by editing the evidence.
- **Helpers:** `44-evidence-check.py` is a byte copy of Phase 43's `43-evidence-check.py`, `44-doc-check.py` a byte copy of Phase 43's `43-doc-check.py`, `44-allowlist-check.py` and `44-hygiene-check.py` byte copies of Phase 38's helpers, and `44-review-check.py` is Phase 38's `38-review-check.py` with `38-` replaced by `44-`, `RLS-04` replaced by `RLS-03` and one marked block added (the review-attestation `agents` subcommand); none is ever edited.
- **Evidence ids (O-11):** plans make E-01 to E-19; E-15 to E-18 are the pending records of the four user handoffs and stay pending; the publication tail is recorded by the orchestrator from E-20 on.
- **No push happens in Phase 44 by an agent.** Reads use `git ls-remote`, `gh api` GET and `curl -s` GET only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RLS-01 | 44-01 | pass |
| E-02 | RLS-01 | 44-01 | pending (plan 44-01) |
| E-03 | RLS-01 | 44-01 | pending (plan 44-01) |
| E-04 | RLS-01 | 44-01 | pending (plan 44-01) |
| E-05 | RLS-03 | 44-02 | pending (plan 44-02) |
| E-06 | RLS-03 | 44-02 | pending (plan 44-02) |
| E-07 | RLS-03 | 44-02 | pending (plan 44-02) |
| E-08 | RLS-03 | 44-03 | pending (plan 44-03) |
| E-09 | RLS-03 | 44-03 | pending (plan 44-03) |
| E-10 | RLS-03 | 44-03 | pending (plan 44-03) |
| E-11 | RLS-02 | 44-04 | pending (plan 44-04) |
| E-12 | RLS-04 | 44-04 | pending (plan 44-04) |
| E-13 | RLS-04 | 44-04 | pending (plan 44-04) |
| E-14 | RLS-04 | 44-05 | pending (plan 44-05) |
| E-15 | RLS-04 | 44-05 | pending (plan 44-05) |
| E-16 | RLS-04 | 44-05 | pending (plan 44-05) |
| E-17 | RLS-04 | 44-05 | pending (plan 44-05) |
| E-18 | RLS-04 | 44-05 | pending (plan 44-05) |
| E-19 | RLS-01 to RLS-04 | 44-05 | pending (plan 44-05) |

### E-01: Pre-flight: origin, pull requests and upstream re-read, helpers checked, premise confirmed on the untouched tree

Backs: RLS-01 (execution-start facts: branch and base commit, the origin and pull-request re-read, the first upstream recheck, the tree values, the Docker environment and the five helper copies, all on the tree before any tracked file changed).

```sh
mkdir -p /tmp/gsd44/exec /tmp/gsd44/w
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
git rev-parse origin/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git merge-base --is-ancestor 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 HEAD
git rev-list --count HEAD --not --remotes=origin
git ls-remote --heads origin
git ls-remote --heads origin sync/v1.7.0
git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'
git tag -l 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'
git rev-parse 'v1.7.0_gg6^{commit}'
gh api "repos/ne1r0n/clickhouse-kafka-connect/releases" --jq '[.[] | "\(.id) \(.tag_name) \(.draft)"] | join("; ")'
gh api "repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50" --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git merge-base --is-ancestor <pull request head> HEAD
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
sysctl vm.swapusage
cp .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py .planning/phases/44-release-publication/44-evidence-check.py
cp .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py .planning/phases/44-release-publication/44-doc-check.py
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-allowlist-check.py .planning/phases/44-release-publication/44-allowlist-check.py
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-hygiene-check.py .planning/phases/44-release-publication/44-hygiene-check.py
python3 -I /tmp/gsd44/exec/make-rv.py <38-review-check.py> .planning/phases/44-release-publication/44-review-check.py
cmp <source> <copy>
bash .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-assets/hygiene-selftest.sh.txt .planning/phases/44-release-publication/44-hygiene-check.py /tmp/gsd44/w/hyg-selftest
python3 -I -X pycache_prefix=/tmp/gsd44/w/pyc -m py_compile <the five helpers>
```

```text
branch: sync/v1.7.0
local_head: 9a5209c17e5380ec1f675cd8b3225250d3ff41fe
dirty_paths: M .planning/config.json; M .planning/state.json; ?? .planning/phases/44-release-publication/44-allowlist-check.py; ?? .planning/phases/44-release-publication/44-doc-check.py; ?? .planning/phases/44-release-publication/44-evidence-check.py; ?? .planning/phases/44-release-publication/44-hygiene-check.py; ?? .planning/phases/44-release-publication/44-review-check.py
phase43_complete: yes
colima_vz: Running
python_version: 3.14.8
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236	refs/heads/custom_build
origin_unchanged: yes
origin_tracking_equal: yes
left_right: 0 201
left_count: 0
gg6_ancestor: yes
unpublished_commits: 199
origin_heads: 14be67ce refs/heads/dependabot/github_actions/github-actions-3bc6d10805; 72e8ccff refs/heads/custom_build; 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; b5cccbd3 refs/heads/dependabot/gradle/com.google.code.gson-gson-2.15.0; bec167d8 refs/heads/main; de8c685b refs/heads/scratch/clt-07-client-0.11.0
origin_sync_ref: none
origin_gg7_tags: none
local_gg7_tags: none
tag_gg6: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
releases_on_origin: 430511927 v1.7.0_gg6 false; 426690002 v1.7.0_gg5 false
pr_heads: 21 14be67ce; 22 b5cccbd3
pr_heads_unchanged: yes
pr_heads_ancestors: yes
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_main_unchanged: yes
upstream_newest_tag: v1.7.0
upstream_tag_past_v170: no
upstream_pr_861: open False 3d4cfdf2
upstream_pr_855: open False ddabf28b
upstream_pr_857: open False 11d88bc0
upstream_prs_unchanged: yes
version_file: v1.7.0_gg6
anchors_version_row: VERSION|v1.7.0_gg6|1|BLD-07
anchors_mirror: rows=190 bad=0
gson_catalog: 2.15.0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_matches_research: yes
swap_used: used = 27152.88M of 27648.00M total (encrypted), 495.12M free
helper_evidence_identical: yes
helper_doc_identical: yes
helper_allowlist_identical: yes
helper_hygiene_identical: yes
review_source_counts: 38-=5 RLS-04=3
helper_review_derived: yes
helper_review_agents_selftest: 5 of 5 as expected
hygiene_selftest: selftest_default_hits=7 selftest_strict_hits=9
helper_syntax: yes
testunit_baseline: not run here: plan 44-04 gates the tip; expected 1826 tests and 2 skipped, Phase 43 verifier and research run
```

The premise holds on the untouched tree: origin `custom_build` still reads the gg6 release commit, the left count is 0 (the right count of 201 is the unpublished `sync/v1.7.0` history, which already contains the gg6 merge), no gg7 or pre-gg7 tag exists locally or on origin, and `sync/v1.7.0` has no head on origin. The two open pull requests on origin (21 and 22) point at commits that are ancestors of `HEAD`. The first upstream recheck found nothing new: upstream `main` is the planning value, the newest tag is `v1.7.0`, and upstream pull requests 861, 855 and 857 are open and unmerged with their planning heads. The swap figure is high, which is the condition under which the OpenJ9 inline-mock crash was seen before; the crash rule applies to every Gradle run of this plan. The helper `44-review-check.py` was verified against the stated derivation (file minus the marked block equals the replaced source, block directly after the `COMMANDS` line) and its `agents` subcommand against five synthetic cases.

2026-11-02T21:01:17Z

Verdict: pass
