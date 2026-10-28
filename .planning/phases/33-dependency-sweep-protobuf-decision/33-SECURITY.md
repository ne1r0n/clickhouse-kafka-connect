---
phase: "33"
slug: dependency-sweep-protobuf-decision
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-28"
---

# Phase 33 — Security

> Plan-time threat registers (33-01..33-03) classified at evidence depth against `33-COMMAND-EVIDENCE.md` (E-01..E-14), the three SUMMARYs and the committed tree. No auditor spawned: all threats mitigated, ASVS 1.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Dependabot PR heads -> local scratch branch | Third-party-authored diffs merged locally | Version strings, workflow action SHA pins, wrapper properties |
| Local branch -> origin | No push, no PR write in this phase | None |
| Shipped Confluent archive | lz4-java runtime jar | Binary dependency |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-33-01 | Tampering | PR #19 head | high | mitigate | Head fetched pre-merge, author/parent/diff checked, SHA in merge message (E-03) | closed |
| T-33-02 | Tampering | origin tip/tags | high | mitigate | No push; tag local; `ls-remote --exit-code` = 2; origin tip unchanged `3a3a15a9` (E-01, E-14) | closed |
| T-33-03 | Info Disclosure | lz4 in shipped archive | medium | mitigate | lz4-java 1.12.1 in archive pom.properties; `verifyConfluentArchive` passed (E-04) | closed |
| T-33-04 | Repudiation | suite gate evidence | medium | mitigate | Counts from JUnit XML; `--rerun` (E-02, E-05, E-10, E-14) | closed |
| T-33-05 | Tampering | setup-java SHA pin | high | mitigate | SHA matches upstream `v6.1.0`; pin-guard classes green (E-09) | closed |
| T-33-06 | Tampering | Gradle 9.8.2 distribution | medium | mitigate | SHA-256 matched published checksum (E-07/E-08) | closed |
| T-33-07 | Tampering | PR heads #18/#20/#16 | high | mitigate | Per-head author/parent/files checks, second-parent = recorded head (E-07, E-09) | closed |
| T-33-08 | Tampering | upstream-identical integrationTests.yaml | medium | mitigate | Blob `3e9b9be0` unchanged; byte guard green (E-09) | closed |
| T-33-09 | Repudiation | FORK-DELTA wording | low | mitigate | Values from evidence; docs commit touches only FORK-DELTA (E-11) | closed |
| T-33-10 | Tampering | dependabot.yml ignore scope | medium | mitigate | `DependabotProtobufIgnoreGuardUnitTest` (red then green, E-12); review WR-01/WR-02 fixed | closed |
| T-33-11 | Spoofing | PR #10 disposition | medium | mitigate | `closing-at-publication`, no agent touched PR #10 (E-13) | closed |
| T-33-13 | DoS | protobuf 4.x advisories not proposed | low | accept | protobuf-java is test-fixtures only; archive holds no `com/google/protobuf/` entries; FUT-03 re-opens | closed |
| T-33-14 | Repudiation | E-14 gate record | medium | mitigate | `gated_head`, XML counts, first-parent merge order re-derived (E-14) | closed |
| T-33-15 | Tampering | origin during merges | high | mitigate | Origin re-read before each merge, never moved (E-07, E-09) | closed |
| T-33-16 | Tampering | origin at phase gate | high | mitigate | Gate re-read of tip, tag, branch, PR #10 `merged_at` (E-14) | closed |
| T-33-SC | Tampering | package installs | low | accept | No npm/pip/cargo install; only existing Maven coordinate/action/wrapper bumps | closed |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-33-01 | T-33-13 | Test-fixtures-only dependency; re-open trigger FUT-03 | Plan 33-03 (ROADMAP) | 2026-10-28 |
| AR-33-02 | T-33-SC | No package-manager installs in phase | Plan 33-01..03 | 2026-10-28 |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-28 | 16 | 16 | 0 | orchestrator (evidence-depth short-circuit) |

## Sign-Off

- [x] All threats have a disposition
- [x] Accepted risks documented
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set

**Approval:** verified 2026-10-28
