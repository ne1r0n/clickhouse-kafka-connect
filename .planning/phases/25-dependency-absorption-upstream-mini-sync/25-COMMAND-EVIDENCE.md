# Phase 25: Command evidence

- Phase: 25 (Dependency Absorption & Upstream Mini-Sync)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 25 plans. Nothing here was pushed.
- Shell: macOS zsh; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. A red-run entry's verdict is `pass` when the red set equals the prediction. Long output stays in `/tmp/gsd25/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | OPS-04 (facts at phase start) | pass |
| E-02 | OPS-04 (operator answer) | pending (plan 25-01) |
| E-03 | DEBT-13 (red run) | pending (plan 25-02) |
| E-04 | DEBT-13 (green run, spotless, testUnit) | pending (plan 25-02) |
| E-05 | DEPS-02 (pre-flight) | pending (plan 25-03) |
| E-06 | DEPS-03 (merge 1 rehearsal and exemption rows) | pending (plan 25-03) |
| E-07 | DEPS-02, DEPS-03 (merge 1, cumulative gate, self-test) | pending (plan 25-03) |
| E-08 | DEPS-02 (merged-tree Docker-free checks) | pending (plan 25-03) |
| E-09 | BLD-15 (unrepaired archive) | pending (plan 25-04) |
| E-10 | BLD-15 (repair) | pending (plan 25-04) |
| E-11 | BLD-15 (compare against v1.7.0_gg3) | pending (plan 25-04) |
| E-12 | BLD-16 (worker jars and bless run) | pending (plan 25-05) |
| E-13 | BLD-16 (reference test) | pending (plan 25-05) |
| E-14 | CI-20 (verifyConfluentArchive controls) | pending (plan 25-06) |
| E-15 | CI-20 (pin-guard red run) | pending (plan 25-06) |
| E-16 | CI-20 (pin-guard green run) | pending (plan 25-06) |
| E-17 | DEBT-14 (pin-guard red run) | pending (plan 25-06) |
| E-18 | DEBT-14 (pin-guard green run, accept-set changes) | pending (plan 25-06) |
| E-19 | DEPS-02 (step-4 suite gate) | pending (plan 25-07) |
| E-20 | SYNC-11 (merge 2 rehearsal) | pending (plan 25-07) |
| E-21 | SYNC-11 (merge 2, tail gate, self-test) | pending (plan 25-07) |
| E-22 | SYNC-12, BLD-15 (post-merge archive, dependencyInsight, advisories) | pending (plan 25-08) |
| E-23 | DEBT-15 | pending (plan 25-08) |
| E-24 | SYNC-12 (two --rerun runs, baseline tag) | pending (plan 25-08) |
| E-25 | OPS-04 (closing observation) | pending (plan 25-08) |

### E-01: OPS-04 facts at phase start (REST, GitHub clock)

Commands as run (R = `https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`; unauthenticated, no `gh`, no token):

```bash
curl -sS "$R/labels/dependencies" -o /tmp/gsd25/exec/ops04-label.json -D /tmp/gsd25/exec/ops04-label.headers -w '%{http_code}'
curl -sS --fail "$R/labels?per_page=100" -o /tmp/gsd25/exec/ops04-labels.json
curl -sS --fail "$R/pulls?state=open&per_page=100" -o /tmp/gsd25/exec/ops04-pulls.json
curl -sS --fail "$R/issues/{4,10}/comments?per_page=100" -o /tmp/gsd25/exec/ops04-comments-{N}.json
```

```text
github_date: Wed, 21 Oct 2026 14:07:41 GMT
label_dependencies_http: 404
labels_count: 9
labels_names: bug, documentation, duplicate, enhancement, good first issue, help wanted, invalid, question, wontfix
open_prs: #10 dependabot[bot] 2026-10-20T21:37:00Z chore(deps): bump com.google.protobuf:protobuf-java from 3.25.9 to 4.36.2; #4 dependabot[bot] 2026-10-14T21:59:26Z chore(deps): bump clickHouseDriver from 0.9.6 to 0.10.0
open_dependabot_prs: 4, 10
dependabot_prs_with_labels_comment: #4: yes, #10: yes
```

Timestamp: 2026-10-21T14:07:46Z

The `dependencies` label is absent before the checkpoint, as expected; both open Dependabot PRs carry the "labels could not be found" comment.

Verdict: pass
