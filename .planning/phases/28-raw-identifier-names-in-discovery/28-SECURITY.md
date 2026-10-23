---
phase: "28"
slug: raw-identifier-names-in-discovery
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-23"
---

# Phase 28 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| DESCRIBE row (server JSON) -> fromJsonRow / normalise / Column enum scanner | Admin-defined identifiers, labels, comments and default expressions reach hand-written parsers | DDL metadata |
| Kafka record field names -> ClickHouseWriter column lookup | A record field that names no column is silently ignored at the root | Record field names |
| Record value (Enum label) -> Column.convertEnumValues | Untrusted record data; lookup miss must stay value-free | Record values |
| Live server DESCRIBE -> committed fixtures | Server bytes become test oracles | Test fixtures |
| build.gradle.kts / jacoco_summary.py -> CI summary | Two copies of one fork-class list | Build metadata |
| Local branch -> origin | Nothing may leave the machine in this phase | Git refs |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-28-01 | Tampering | normalise quote closing | medium | mitigate | Backslash pair read as one unit (DeclaredNullability.java:110); WR-01 pins in DeclaredNullabilityTest | closed |
| T-28-02 | Repudiation | "pinned before decode change" claim | low | mitigate | Commit 1f984bef touches only the test file, no src/main diff | closed |
| T-28-03 | Tampering | committed matrix rows | medium | mitigate | 25 files byte-equal to run 2; RawNameMatrixDriftLiveTest | closed |
| T-28-04 | Repudiation | old/new name table | medium | mitigate | Forms from production describeTable on explicit clients; c1/c2 independent escapers | closed |
| T-28-05 | Information disclosure | fromJsonRow exceptions | medium | mitigate | Fixed ROW_READ_FAILURE_MESSAGE; sentinel-comment pin in RawNameMatrixFixturesUnitTest | closed |
| T-28-06 | Denial of service | normalise / unescapeJsonLayer | medium | mitigate | Single forward passes, no exception path; totality rows | closed |
| T-28-07 | Tampering | data under wrong column | high | mitigate | Raw-name + nullability assertions on both clients; live write/read-back pins | closed |
| T-28-08 | Tampering | batch retried across upgrade | high | transfer | No diff in Processing/state/kafka/writer; clean stop recorded, handed to Phase 29 (ROLL-04) | closed |
| T-28-09 | Tampering | upstream sync restoring doubling | medium | mitigate | 11 anchor rows in fork-anchors.txt recounted on HEAD | closed |
| T-28-10 | Tampering | root column DEFAULT for unrenamed producer | high | transfer | doubledRootNameStoresTheDefault pin; rename precondition in ledger, CHANGELOG, FORK-DELTA | closed |
| T-28-11 | Repudiation | doubled element fails loudly | medium | mitigate | 32 cases with container-path substring check | closed |
| T-28-12 | Information disclosure | enum scanner failure text | medium | mitigate | Fixed MALFORMED_ENUM_TYPE_MESSAGE; lookup miss names column only | closed |
| T-28-13 | Denial of service | enum scanner looping | medium | mitigate | One forward scan, backslash consumes next char, malformed fails with fixed exception | closed |
| T-28-14 | Tampering | enum written as wrong code | high | mitigate | Typed read-back of code and label for all 12 entries; legacy form fails loudly | closed |
| T-28-15 | Repudiation | claims without measurement | medium | mitigate | Suite + V1 run on committed tree; reasoned claims labelled | closed |
| T-28-16 | Repudiation | coverage lists drifting | low | mitigate | ForkCoverageScopeUnitTest in testUnit; DEBT-17 anchors | closed |
| T-28-17 | Tampering | accidental push / moved origin | medium | mitigate | origin/custom_build equals E-01 tip; no sync/v1.7.0 on origin | closed |
| T-28-18 | Repudiation | existing test weakened | medium | mitigate | ASSERTION-DIFF-CLEAN against signed-off exemption list; user sign-off in 28-UAT.md | closed |
| T-28-SC | Tampering | package installs | low | accept | No dependency changed | closed |

*Status: open · closed · open — below high threshold (non-blocking)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-28-01 | T-28-SC | No package installed or changed in this phase | plan threat models | 2026-10-23 |
| AR-28-02 | Log lines (28-03) | Raw names holding LF/CR can appear in existing DEBUG lines of DescribeTableBuilder and the DeclaredNullability WARN column slot; names are admin-chosen DDL metadata, no new log line prints a name; below the high blocking threshold at ASVS L1 | plan 28-03 threat model | 2026-10-23 |

---

## Informational Notes (non-blocking)

- Review-fix commits a8f50d87 and ae97c496 changed src/main after the E-10/E-13 full-suite runs; only targeted classes re-ran. Run one full `test` on HEAD before Phase 29 cites E-13 figures.
- The T-28-04 escaper check and T-28-18 assertion-diff script live only in /tmp/gsd28/exec/.
- ClickHouseFieldDescriptor non-object-row fallback may quote the row in a databind message; such rows carry no comment or default expression, so T-28-05 is not reached.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-23 | 19 | 19 | 0 | gsd-security-auditor |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-23
