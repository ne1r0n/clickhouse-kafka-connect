# Phase 11 — API Coverage Decision

This phase integrates one external API, read-only: the **GitHub REST API** for `ne1r0n/clickhouse-kafka-connect`, called unauthenticated with `curl` + `jq` (the `gh` CLI is deliberately not installed, see the `[Tooling]` row in `STATE.md`; 60 requests/hour budget, threat T-11-24). It is used only to read CI evidence for CI-05 and CI-06. No connector, test, or build code calls it. One adjacent read of Docker Hub tag metadata is listed for completeness.

## Why the detector fired

Three signals: `connect`/`api` and `rest`. The `rest` signal and the GitHub reads in plans 11-05 to 11-07 are a real integration, enumerated below. The other `api` hits are not an integration surface: `connect-api` / `libs.kafka.connect.api` is the Kafka Connect **library dependency** pin (`4.1.1`) recorded in `docs/FORK-DELTA.md` (11-03), and "docker API" is a quoted `docker info` error from 11-04's daemon-down check.

## Coverage matrix — GitHub REST API (Actions)

| Capability | Decision | Reason |
|---|---|---|
| GET actions/runs?head_sha= (list runs for a commit) | INTEGRATE | CI-06 locator: filter path == tests.yaml, assert one match, read conclusion. Used in 11-05, 11-06, 11-07; see 11-VERIFICATION.md row 4 |
| GET actions/runs/{id} (single run) | INTEGRATE | CI-05 run-level wall-clock run_started_at to updated_at vs half of timeout-minutes: 350 s (11-05) and 386 s (11-07) vs 1350 s |
| GET actions/runs/{id}/jobs (run jobs and steps) | INTEGRATE | CI-05 job-level wall-clock and Run tests step conclusion: 345 s / 326 s (11-05) and 380 s / 363 s (11-07) |
| GET actions/runs/{id}/artifacts (artifact list) | INTEGRATE | CI-05 coverage publication: exactly one jacoco-coverage-reports with size > 0 (816157 B on run 37619434848) |
| GET check-runs and check-run annotations | INTEGRATE | Cross-check of the run list (one success, one cancelled) and proof annotations are deprecation warnings only (11-VERIFICATION.md rows 3-4) |
| GET actions/jobs/{id}/logs (job log download) | OPT-OUT | HTTP 403 unauthenticated. CI test totals, server version and fork count stay unread and are disclosed (UAT test 4); a token is out of scope for a read-only phase |
| GET actions/artifacts/{id}/zip (artifact download) | OPT-OUT | HTTP 401 unauthenticated. The claim is downgraded to artifact exists with non-zero size, not contains both XML reports (11-05 SUMMARY) |
| POST rerun / cancel / workflow_dispatch | OPT-OUT | Runs are triggered only by git push (on: [push]) so evidence matches a real push; no write access is held or wanted |
| Workflow, secret and variable management endpoints | OPT-OUT | tests.yaml is read from the git tree, not the API; the phase manages no secrets or variables |
| Issues, PRs, releases, repo admin endpoints | OPT-OUT | Not a phase concern; tags are published with git push, not the Releases API |

## Coverage matrix — Docker Hub (adjacent read)

| Capability | Decision | Reason |
|---|---|---|
| Docker Hub tag metadata for clickhouse-server:latest | INTEGRATE | 11-05 matched the Hub digest to the local image repo digest to infer the CI server version (26.9.12.8), labelled as an inference |
| Docker Hub pulls, auth and other repositories | OPT-OUT | Testcontainers pulls the image itself; no other Hub use |

## Residual limits

All INTEGRATE rows are unauthenticated reads of public data from a public fork, so their results are only as trustworthy as an unauthenticated third-party endpoint (trust boundary in the 11-05 plan's threat model). The two OPT-OUT download rows are why UAT test 4 (CI fork topology) is a human judgment and not a measurement.
