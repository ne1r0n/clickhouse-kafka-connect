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
| E-02 | OPS-04 (operator answer) | pass |
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

### E-02: OPS-04 operator answer (verbatim)

Presented inline by the orchestrator in this session (plan 25-01 Task 2, `blocking-human`): the E-01 facts, the four web-UI steps for creating the `dependencies` label, and the request to answer in the user's own words. The answer below is the user's typed reply, copied unedited.

OPS-04 answer (verbatim, given directly to the orchestrator, 2026-10-21T15:11:05Z):
> just created the label

orchestrator annotation (not the user's words): no screenshot was given. The answer states no clock time; "just" is read as the instant the answer was received, which is the `ops04_created_at_user` value below. The API bounds the creation instant to the window after E-01's `404` at 2026-10-21T14:07:46Z and at or before the re-read below.

Command re-run by the orchestrator after the answer (unauthenticated, `R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`, GitHub `date:` header `Wed, 21 Oct 2026 15:11:05 GMT`, `x-ratelimit-remaining: 59`):

```bash
curl -sS -D /tmp/ops04-headers.txt -o /tmp/ops04-body.json -w '%{http_code}' "$R/labels/dependencies"
```

```text
ops04_created_at_user: 2026-10-21T15:11:05Z
ops04_created_window_api: after 2026-10-21T14:07:46Z (E-01 404), at or before 2026-10-21T15:11:05Z (200)
label_dependencies_http_after: 200
label_name_after: dependencies
label_description_after: Dependency updates (Dependabot)
ops04_user_and_api_agree: yes
OPS-04 label created: pass
```

The user says the label exists, the status is 200 and the JSON `name` is exactly `dependencies`, so the agree line is `yes`. No label, PR or `dependabot.yml` change was made by an agent; no authenticated call was made. The closing observation (a Dependabot PR without the "labels could not be found" comment, or pending the next monthly run) is E-25 in plan 25-08, not this entry.

Timestamp: 2026-10-21T15:11:05Z

Verdict: pass
