# Phase 21 Evidence

- Phase: 21 (Dependabot Integration & CI Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 21 plans. Nothing here was pushed.
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured ({reason})`. Long output stays in `/tmp/gsd21/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | OPS-01, OPS-02, OPS-03 (facts at phase start) | unmeasured (D-11 window opens 2026-10-17T12:00:00Z; the closing OPS-01 check is E-13 in plan 21-05) |
| E-02 | OPS-01, OPS-02, OPS-03 (operator answers) | pending (plan 21-01) |
| E-03 | DEPS-01 (merge) | pending (plan 21-02) |
| E-04 | DEPS-01, D-03 (gate before the exemption) | pending (plan 21-02) |
| E-05 | DEPS-01 (dependencyInsight) | pending (plan 21-02) |
| E-06 | DEPS-01 (testUnit and Lombok warning) | pending (plan 21-02) |
| E-07 | DEPS-01, D-14 (spotless baseline-equal) | pending (plan 21-02) |
| E-08 | DEPS-01, D-01, D-02 (gate with and without the exemption) | pending (plan 21-03) |
| E-09 | D-04 (self-test) | pending (plan 21-03) |
| E-10 | CI-18, CI-19, D-07 (pin-guard red run) | pending (plan 21-04) |
| E-11 | CI-18, CI-19, D-09, D-14 (pin-guard green run) | pending (plan 21-04) |
| E-12 | DEPS-01 (phase close gate) | pending (plan 21-05) |
| E-13 | OPS-01, D-11 (closing observation) | pending (plan 21-05) |

### E-01: OPS facts at phase start (REST, GitHub clock, Keychain)

Commands as run (repo root, unauthenticated, `R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`, every curl `-sS --fail`, output in `/tmp/gsd21/exec/`):

1. `curl -sS --fail -D ops-wf.headers -o ops-wf.json "$R/actions/workflows/integrationTests.yaml"` (id, state, `updated_at` U, `date:` header)
2. `curl -sS --fail -o ops-runs-after.json "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E{U}&per_page=100"`
3. `curl -sS --fail -o ops-repo-runs-after.json "$R/actions/runs?event=schedule&created=%3E{U}&per_page=100"`
4. `curl -sS --fail -o ops-runs-control.json "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E2026-10-01T00:00:00Z&per_page=100"`
5. `curl -sS --fail -o ops-dependabot-runs.json "$R/actions/workflows/384810012/runs?per_page=20"` (newest run named `gradle in /`)
6. `security find-generic-password -s gsd17-gh-actions-read >/dev/null 2>&1; echo $?` (exit status only)
7. `git diff --quiet v1.7.0 HEAD -- .github/workflows/integrationTests.yaml` and `git diff --quiet v1.7.0 origin/custom_build -- .github/workflows/integrationTests.yaml` (both succeeded)

```text
workflow_id: 384641581
workflow_state: disabled_manually
workflow_updated_at: 2026-10-16T09:38:23Z
github_date: Sat, 17 Oct 2026 07:34:27 GMT
schedule_runs_after_disable: 0
repo_schedule_runs_after_disable: 0
control_schedule_runs_since_2026-10-01: 2
dependabot_gradle_job: gradle in /. - Update #1627525145 2026-10-14T21:05:35Z completed success
keychain_lookup_exit: 44
integration_tests_yaml_identical_to_v1.7.0: yes
```

Timestamp: 2026-10-17T07:34:34Z

Verdict: unmeasured (D-11 window opens 2026-10-17T12:00:00Z; the closing OPS-01 check is E-13 in plan 21-05)
