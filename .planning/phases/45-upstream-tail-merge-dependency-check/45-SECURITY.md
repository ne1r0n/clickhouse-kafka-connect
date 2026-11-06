---
phase: "45"
slug: upstream-tail-merge-dependency-check
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-05"
---

# Phase 45 — Security

> Threat register verified by gsd-security-auditor against HEAD `edbe4e49` (32 rows, 26 unique IDs plus six per-plan T-45-SC rows). Unedited tail gate re-run: `anchor_rows=200 result=PASS`.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| upstream repo → fork tree | one mechanical merge of upstream `83808df9` | upstream source, catalog coordinates |
| record value → failure surfaces | writer exceptions, ERROR log, DLQ headers | Kafka record values (must never cross) |
| Maven coordinates → shipped jar | dependency catalog | third-party code |
| fork → origin | local tag and branch, nothing pushed | refs |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-45-01 | Tampering | merge commit M | high | mitigate | M changes exactly 6 paths, no `.planning`; fork-side paths byte-identical to M^1; tail gate PASS | closed |
| T-45-02 | Spoofing | upstream commit | high | mitigate | M^2 = `83808df9` = upstream/main, re-read before merge (E-02) | closed |
| T-45-03 | Tampering | origin refs | high | mitigate | no `pre-v1.7.0_gg8` tag on origin (gh api and ls-remote) | closed |
| T-45-04 | Repudiation | evidence E-01..E-04 | medium | mitigate | helper checks `rows`, git facts re-verified | closed |
| T-45-05 | Information disclosure | evidence and scratch logs | medium | mitigate | `secrets` check 0 token shapes; no sentinel | closed |
| T-45-06 | Information disclosure | #857 text, ERROR log, DLQ | high | mitigate | #857 `serializationFailure` layer removed; E-06 15 shapes | closed |
| T-45-07 | Information disclosure | record-context log line | high | mitigate | log carries topic/partition/offset/column/class only, no throwable | closed |
| T-45-08 | Information disclosure | #857 probe, sentinel | medium | mitigate | probe never committed; one worktree | closed |
| T-45-09 | Tampering | upstream-owned tests | medium | mitigate | two edits, each bounded vs upstream; no `@Disabled` | closed |
| T-45-10 | DoS / Tampering | retry classification | high | mitigate | `Utils` change is visibility only; 216-case matrix unchanged; V1/V2 retry pins | closed |
| T-45-11 | Information disclosure | Jackson-over-IO pass-through | high | mitigate | `withheldJacksonChain` rebuild; value checks on all surfaces | closed |
| T-45-12 | Tampering | worktree mutations | low | mitigate | mutation worktrees removed; src/main diff limited to two files | closed |
| T-45-13 | Tampering | `fromJsonRow` decoding | high | mitigate | `FieldDescriptorLiteralFieldsUnitTest` 31 cases, 30 red under mutation | closed |
| T-45-14 | Information disclosure | malformed DESCRIBE row message | medium | mitigate | no-echo pin unchanged | closed |
| T-45-15 | Tampering | connect-json in shipped set | medium | mitigate | `testImplementation` only; absent from shipped set | closed |
| T-45-16 | Repudiation | FORK-DELTA claims | medium | mitigate | rows match code; over-statements corrected | closed |
| T-45-17 | EoP / Tampering | vulnerable shipped dependency | high | mitigate | 27 shipped coordinates, 0 OSV and 0 GitHub advisories (control 74/74) | closed |
| T-45-18 | Tampering | unapproved version change | high | mitigate | build files unchanged since M | closed |
| T-45-19 | Information disclosure | gh token | medium | mitigate | no token shape in records | closed |
| T-45-20 | Repudiation | gate results | medium | mitigate | E-18 two equal full suites | closed |
| T-45-21 | Tampering | origin refs | high | mitigate | origin `custom_build` unchanged `94834763`; no tag | closed |
| T-45-22 | Information disclosure | residual Jackson chains | high | mitigate | 21-shape pin incl. server-error links | closed |
| T-45-23 | Information disclosure | `writeRecordsJson` failures | high | mitigate | try/catch guard, `JSON_ROW_FAILURE_FORMAT` | closed |
| T-45-24 | DoS / Tampering | JSON-route retry classification | high | mitigate | shared rebuild; Jackson-free control unchanged | closed |
| T-45-25 | Tampering | JSON success path | medium | mitigate | `writeJsonRow` outside the try; byte tests | closed |
| T-45-26 | Information disclosure | JSON probe, sentinel | medium | mitigate | probe never committed | closed |
| T-45-SC (01) | Tampering | connect-json 4.4.0 | high | mitigate | jar SHA-1 equals Maven Central | closed |
| T-45-SC (02-05) | Tampering | package installs | low | accept | nothing installed | closed (accepted) |
| T-45-SC (06) | Tampering | Maven coordinates | high | mitigate | no bump; P1-P16 declined | closed |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-45-01 | T-45-11, T-45-22, T-45-23 | WR-02: suppressed exceptions on the kept IOException suffix are not scanned; the writer path builds none; FORK-DELTA names the residual | user (dialog answer) | 2026-11-05 |
| AR-45-02 | T-45-17 | DEP-02: 16 test-scope advisories (P1-P16) declined, record only; none ships. Also unmapped Confluent `7.9.1-ccs` versions noted, not chased | user (dialog answer) | 2026-11-05 |
| AR-45-03 | T-45-SC (02-05) | nothing installed | auditor | 2026-11-05 |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-05 | 32 | 32 | 0 | gsd-security-auditor |

## Sign-Off

- [x] All threats have a disposition
- [x] Accepted risks documented
- [x] `threats_open: 0` confirmed
- [x] `status: verified`

**Approval:** verified 2026-11-05
