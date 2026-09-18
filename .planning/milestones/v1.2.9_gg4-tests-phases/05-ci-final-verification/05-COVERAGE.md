# 05 — API Coverage Matrix

**Scope:** the GitHub REST API surface touched by Phase 5's *verification tooling*. The connector
itself (a Kafka Connect sink writing to ClickHouse) makes no GitHub API calls at runtime — this
surface exists only in the one-shot scripts that confirmed the pushed `custom_build` CI run
(run `35317272594`) was green and that the pinned action tags resolve to real commits.

**Auth posture:** unauthenticated, read-only, within the 60 req/hr anonymous budget (DEC-D). No
token, PAT, or `gh` CLI credential is introduced into the repo or into CI by this phase.

| capability | decision | reason |
|---|---|---|
| `GET /repos/{o}/{r}/actions/runs` (filtered by `head_sha` + `path`) | INTEGRATE | locates the CI run for the pushed SHA — exactly one match, never "latest run on branch" |
| `GET /repos/{o}/{r}/actions/runs/{id}/jobs` | INTEGRATE | confirms the single job's conclusion and both key steps' status |
| `GET /repos/{o}/{r}/actions/runs/{id}/artifacts` | INTEGRATE | confirms both coverage artifacts were uploaded by that run |
| `GET /repos/{o}/{r}/git/ref/tags/*` (action tag resolution) | INTEGRATE | re-confirms the 4 pinned action tags resolve to real commits at execution time |
| `GET /repos/{o}/{r}/actions/runs/{id}/logs` | OPT-OUT | requires admin rights; 401 unauthenticated. Rendered job summary is verified by a human instead (UAT test 1) |
| `GET /repos/{o}/{r}/actions/artifacts/{id}/zip` | OPT-OUT | 401 unauthenticated. Artifact existence is the claim under test; artifact contents are machine-proven at build time |
| job-summary rendering endpoint | OPT-OUT | no unauthenticated endpoint returns a rendered job summary (05-RESEARCH.md). Covered by a scoped human glance, harvested into UAT |
| mutating run endpoints (re-run, cancel, dispatch, delete artifact) | OPT-OUT | verification is read-only by design — no CI state is mutated from this workflow |
| authenticated endpoints (PAT, `gh` CLI, `GITHUB_TOKEN` reads) | OPT-OUT | DEC-D chose unauthenticated read-only; adding a credential for a one-shot check is not worth the secret-handling surface |
| Checks API, Statuses API, webhooks, GitHub App | OPT-OUT | no bot and no external status reporting — Actions posts its own run status natively |
| connector runtime GitHub surface | OPT-OUT | the shipped sink talks to ClickHouse and Kafka only; it never calls GitHub |

**Completeness:** every capability the phase's verification scripts could have reached is listed
above with a decision. The four INTEGRATE rows are the entire live surface actually called.
