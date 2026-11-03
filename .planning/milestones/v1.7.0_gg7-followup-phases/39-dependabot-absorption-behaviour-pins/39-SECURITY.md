---
phase: "39"
slug: "dependabot-absorption-behaviour-pins"
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-01"
---

# Phase 39 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail. Classified at grep depth by the orchestrator against the plan-time registers (no auditor spawned; all threats mitigated, ASVS 1).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Dependabot PR heads → sync/v1.7.0 | Two third-party-authored dependency bumps merged locally | Maven version string, GitHub Action SHA pin |
| Local tree → origin | Nothing pushed in this phase | none |
| Integration harness → Connect REST | Test logging of connector status | connector names, task counts |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-39-01 | Tampering | gson source merge | high | mitigate | E-02 records author, parent, one-line diff; merge `ac216401` first-parent diff is only `docs/FORK-DELTA.md` and `gradle/libs.versions.toml` | closed |
| T-39-02 | Tampering | origin / PRs / tags | high | mitigate | `git ls-remote origin custom_build` still `72e8ccff`; no push performed | closed |
| T-39-03 | Repudiation | gate evidence | medium | mitigate | counts read from JUnit XML via helper; second runs used `--rerun` | closed |
| T-39-04 | Information Disclosure | evidence file | low | mitigate | helper `secrets` scan run before each commit | closed |
| T-39-05 | Tampering | setup-gradle pin | high | mitigate | all four workflows pin `8da46b53…` (peeled commit, full 40-hex), checked against live `refs/tags/v6.4.1^{}` | closed |
| T-39-06 | Tampering | PR #21 merge | high | mitigate | merge `82202f7d` first-parent diff is the four workflows + FORK-DELTA only | closed |
| T-39-07 | Tampering | integrationTests.yaml / release pair | medium | mitigate | SHA-256 and diff signature unchanged; guard classes pass | closed |
| T-39-08 | Repudiation | bump claims | low | mitigate | no vulnerability claim written | closed |
| T-39-09 | Repudiation | PIN expectations | medium | mitigate | `src/main` diff since phase start empty | closed |
| T-39-10 | Information Disclosure | test messages | low | mitigate | constants and synthetic keys only | closed |
| T-39-11 | Denial of Service | OpenJ9 crash | low | mitigate | no mocking framework in new tests | closed |
| T-39-12 | Tampering | existing pins | medium | mitigate | `AdditionalRetriableErrorCodesPinUnitTest` unchanged | closed |
| T-39-13 | Information Disclosure | all-connectors log lines | medium | mitigate | `getConnectors` removed (0 references in ConfluentPlatform.java) | closed |
| T-39-14 | Denial of Service | leftover Datagen connectors | low | mitigate | `finally` deletes | closed |
| T-39-15 | Tampering | F-3 fix scope | medium | mitigate | fix commit limited to `src/integrationTest` | closed |
| T-39-16 | Repudiation | recorded run | medium | mitigate | totals from JUnit XML, E-08/E-09 | closed |
| T-39-SC | Tampering | package installs | low | accept | only an existing Maven coordinate version changed (Dependabot) | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-39-01 | T-39-SC | Dependabot gson/setup-gradle bumps verified against Maven Central / gradle/actions remote | plan-time register | 2026-11-01 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-01 | 17 | 17 | 0 | orchestrator (grep-depth classification) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-01
