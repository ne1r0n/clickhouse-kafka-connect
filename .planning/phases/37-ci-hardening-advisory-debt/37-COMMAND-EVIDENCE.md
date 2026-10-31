# Phase 37: Command evidence

- **Base commit:** `local_head` = `ff8730860ba8560171fcda44bc1cdcd6dba1ba2a` (HEAD when plan 37-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-31.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `37-evidence-check.py` in this directory is the read-only gate helper (a byte-identical copy of Phase 36's; get, expect, index, rows, secrets, junit, anchors, skips, failed) and `37-ledger-check.py` is the read-only ledger helper of this phase; neither is ever edited.
- **Server version:** E-01 and E-18 record `clickhouse_version`, the `SELECT version()` of the ClickHouse `latest` image at the time of the entry, even when no server ran in the entry.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | CIH-01, CIH-02, CIH-03 | 37-01 | pass |
| E-02 | CIH-01, CIH-02, CIH-03 | 37-01 | pending (plan 37-01) |
| E-03 | CIH-01, CIH-03 | 37-01 | pending (plan 37-01) |
| E-04 | CIH-02 | 37-01 | pending (plan 37-01) |
| E-05 | CIH-05 | 37-02 | pending (plan 37-02) |
| E-06 | CIH-04 | 37-02 | pending (plan 37-02) |
| E-07 | CIH-04, CIH-05 | 37-02 | pending (plan 37-02) |
| E-08 | ADV-03 | 37-02 | pending (plan 37-02) |
| E-09 | CIH-04, ADV-01 | 37-03 | pending (plan 37-03) |
| E-10 | CIH-04, ADV-01, ADV-02 | 37-03 | pending (plan 37-03) |
| E-11 | CIH-04 | 37-03 | pending (plan 37-03) |
| E-12 | CIH-04 | 37-03 | pending (plan 37-03) |
| E-13 | ADV-01 | 37-04 | pending (plan 37-04) |
| E-14 | ADV-02 | 37-04 | pending (plan 37-04) |
| E-15 | ADV-02 | 37-04 | pending (plan 37-04) |
| E-16 | ADV-01, ADV-02 | 37-05 | pending (plan 37-05) |
| E-17 | ADV-02, ADV-03 | 37-05 | pending (plan 37-05) |
| E-18 | CIH-01 to CIH-05, ADV-01 to ADV-03 | 37-05 | pending (plan 37-05) |

### E-01: Pre-flight: origin re-read, Phase 36 complete, baselines on the untouched tree

Backs: CIH-01, CIH-02, CIH-03, ROADMAP Phase 37 criterion 1 (the premise re-read before any code lands, and a green baseline re-measured on the untouched tree so every later red or green is attributable to this phase).

```sh
colima list
python3 --version
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
git log --format=%s | grep -qF 'docs(36): complete phase 36'
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'scratch/*'
git ls-remote --heads origin sync/v1.7.0
git rev-parse 'v1.7.0_gg4^{commit}'
git rev-parse 'v1.7.0_gg5^{commit}'
gh auth status
cat VERSION
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt
cmp .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py
cmp .planning/phases/37-ci-hardening-advisory-debt/37-hygiene-check.py .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py
git ls-files src/main | wc -l
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd37/exec/testunit-baseline.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class <ClassName>
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed build/test-results/testUnit
```

```text
branch: sync/v1.7.0
local_head: ff8730860ba8560171fcda44bc1cdcd6dba1ba2a
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
phase36_complete: yes
python_version: Python 3.14.8
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
origin_unchanged: yes
origin_scratch_refs: de8c685b80d40b0a6e19338271e07b08ede84f3b refs/heads/scratch/clt-07-client-0.11.0; 939e899298c2f12dec2cfe1e69b1a6b53b4f511e refs/heads/scratch/clt-07-prebump-0.9.6
origin_sync_ref: none
tag_gg4: 21c51598b06b6f5af58430765d57b1e5f6747c5a
tag_gg5: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
gh_auth: github.com
version_file: v1.7.0_gg5
anchors_version_row: VERSION|v1.7.0_gg5|1|BLD-07
anchors_mirror: rows=152 bad=0
helper_identical_to_phase36: yes
hygiene_helper_identical_to_phase36: yes
assets_present: 16
src_main_files: 73
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
baseline_testunit_exit: 0
baseline_testunit_tests: 1475
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_pinguard_tests: 39
baseline_actionpin_tests: 21
baseline_failedsummary_tests: 8
baseline_bytesguard_tests: 9
baseline_archivecheck_tests: 6
baseline_connectapi_tests: 3
baseline_columntest_tests: 50
baseline_describeunreadable_tests: 2
baseline_jvm_crash: none
```

The planning-time research found no `scratch/*` ref on origin; two now exist (`scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6`, the Phase 36 CLT-07 scratch refs). They are recorded, not a stop condition: this phase merges nothing and pushes nothing. `origin_custom_build` still equals the `v1.7.0_gg5` tag. The phase helper files and the 16 assets were already tracked at `local_head` (committed with the plan), so this evidence commit stages only the evidence file. The untouched testUnit run reports 2 skipped tests; the 18 skips of the full suite belong to the `test` task, not to `testUnit`.

2026-10-31T08:52:00Z

Verdict: pass
