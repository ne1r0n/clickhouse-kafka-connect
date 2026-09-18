---
phase: "05"
slug: "ci-final-verification"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-09-18"
---

# Phase 05 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time (`<threat_model>` blocks in `05-01-PLAN.md` and
`05-02-PLAN.md`). Verification mode: verify declared mitigations exist — not a fresh threat scan.
ASVS L1 (grep depth), `security_block_on: high`.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| GitHub Actions runner ← Marketplace action code | Four third-party actions execute with the workspace checked out | Repo source, build outputs |
| Public repo ← uploaded artifacts | Coverage and test reports become downloadable from a public repository's run page | Line-coverage counts, class names, test names |
| Runner ← workflow secrets | The upstream job read two cloud credentials; the rewritten job reads none | None (secret references deleted) |
| Public internet ← local git history | 106 local commits, including 79 tracked `.planning/` files, crossed into permanent public visibility | Source, planning docs, commit metadata |
| Local shell ← unauthenticated GitHub API responses | Untrusted JSON from the public internet parsed and asserted on locally | Run/job/artifact metadata |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-05-01 | Tampering | Mutable major-version action tags repointed by a publisher | medium | accept | Verified: `tests.yaml` pins `actions/checkout@v4`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v4` — all first-party (`actions`, `gradle`), matching the fork's pre-existing convention and CI-01's wording. All four re-resolved live to real commits at execution. SHA pinning recorded as a rejected stricter alternative, not an oversight. | closed |
| T-05-02 | Information Disclosure | `jacoco-coverage-reports` / `gradle-test-reports` artifacts on a public repo | low | accept | Verified: the rewritten `tests.yaml` job contains zero `secrets.` references, so nothing secret can reach a report. Artifact contents are line-coverage counts, class names and test names. 14-day retention. | closed |
| T-05-03 | Elevation of Privilege | Untrusted-fork code execution with repository secrets | low | mitigate | Verified in `.github/workflows/tests.yaml`: trigger is bare `on: [push]` (D-13) — no `pull_request`, no `pull_request_target`. No `permissions:` escalation block. Zero `secrets.` references; the only `env:` values are `CLICKHOUSE_VERSION: latest` and `CLIENT_VERSION: V2`. | closed |
| T-05-04 | Tampering | Supply chain via a coverage-reporting marketplace action | low | mitigate | Verified: no coverage marketplace action present (`madrapps/jacoco-report` rejected at D-06). The summary is rendered by repo-local `.github/scripts/jacoco_summary.py`, whose only imports are `os`, `sys`, `xml.etree.ElementTree` — standard library. No `eval`, `exec`, `subprocess`, `os.system`, `shell=True`, or network call in the script. | closed |
| T-05-06 | Information Disclosure | `git push` of the full `custom_build` history to a public fork (D-09) | high | mitigate | Verified in `05-02-SUMMARY.md`: gated behind a Task 2 `checkpoint:decision` carrying `gate="blocking-human"` (never auto-approved in any mode), with the one-way public-visibility consequence named explicitly. A read-only pre-push scan of the 106-commit delta surfaced 4 credential-shaped candidates, all confirmed false positives before the go/no-go. Push was fast-forward (`22f055c..a295af8`), no force, no history rewrite. | closed |
| T-05-07 | Tampering | Unauthenticated GitHub API JSON parsed locally and used to decide phase success | low | mitigate | Verified: responses parsed with `json.load` only — never evaluated. Assertions read specific named fields (`conclusion`, `name`, `size_in_bytes`, `path`). Nothing from the payload is executed, interpolated into a shell command, or written to disk. No `/logs` or artifact `/zip` endpoint called; no token minted; `gh` CLI not installed (D-11). | closed |
| T-05-08 | Spoofing | Attributing an unrelated run's green result to this work | medium | mitigate | Verified: the run was located by filtering `actions/runs?head_sha=<pushed SHA>` to `path == .github/workflows/tests.yaml`, asserting exactly one match (DEC-F) — run `35317272594`. A plan-level prohibition additionally forbids reporting on a run found by recency; UAT test 2 confirms it held. | closed |
| T-05-09 | Information Disclosure | Public CI artifacts (`jacoco-coverage-reports`, `gradle-test-reports`) | low | accept | Verified: same control as T-05-02 — the job reads no secrets, so no secret can reach a report. | closed |
| T-05-SC | Tampering | npm/pip/cargo installs | low | accept | Verified: neither plan invokes a package manager and no dependency was added. `jacoco_summary.py` is standard-library-only; `gh` was deliberately not installed. RESEARCH.md's Package Legitimacy Audit records the Marketplace Actions as first-party/Approved — no `[ASSUMED]`/`[SUS]`/`[SLOP]` package exists in this phase. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-05-01 | T-05-01 | Mutable major tags are what CI-01 specifies and what the fork already used; all four publishers are first-party. SHA pinning is stricter but outside CI-01's wording. | Phase 05 plan (05-01) | 2026-09-18 |
| R-05-02 | T-05-02, T-05-09 | Public coverage/test artifacts expose only line-coverage counts, class names and test names from an already-public fork; the producing job holds no secrets. | Phase 05 plan (05-01, 05-02) | 2026-09-18 |
| R-05-03 | T-05-SC | No package manager runs in this phase; nothing is installed, so there is no install-time supply-chain surface to control. | Phase 05 plan (05-01, 05-02) | 2026-09-18 |

*Accepted risks do not resurface in future audit runs.*

---

## Out-of-Scope Observations

Recorded for the audit trail; **not** phase-05 threats and not counted in `threats_open`. Phase 05's
change surface was `tests.yaml`, `jacoco_summary.py`, and `build.gradle.kts` only.

- `.github/workflows/integrationTests.yaml` and `.github/workflows/increment-version.yml` carry
  `secrets.` references inherited from upstream. Checked their triggers while verifying T-05-03:
  `integrationTests.yaml` is `on: [workflow_dispatch]` (manual only, not fork-triggerable), so the
  untrusted-fork-with-secrets path does not exist there either.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-18 | 9 | 9 | 0 | /gsd-secure-phase (verify:post hook, ASVS L1) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-09-18
