---
phase: "40"
slug: "helper-client-closing"
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-02"
---

# Phase 40 — Security

> Per-phase security contract. Classified at grep depth by the orchestrator against the plan-time registers (no auditor spawned; all high threats mitigated, ASVS 1).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Task lifecycle → ClickHouse HTTP pools | Helper clients opened by writer, V1 batch path and Keeper provider | connections only |
| Exactly-once state path | Keeper state read/write | offsets (unchanged) |
| Logs | close-failure WARN lines | exception messages |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-40-01 | Repudiation | recorded red runs | medium | mitigate | E-02..E-04 with failing assertion reasons | closed |
| T-40-02 | Tampering | red-tree patches | medium | mitigate | SHA-256 recorded and re-checked (E-08 equals E-04) | closed |
| T-40-03 | Tampering | src/main during red phase | high | mitigate | src equal to HEAD after plan 40-01 (E-04) | closed |
| T-40-04 | Denial of Service | test-suite stability | low | mitigate | no mocking framework in new tests | closed |
| T-40-05 | Information Disclosure | assertion messages | low | mitigate | constants only | closed |
| T-40-06 | Denial of Service | writer helper clients | medium | mitigate | writer commit `de3faf77` + WR-01 `e7c4958d`; 22+ cases | closed |
| T-40-07 | Information Disclosure | close-failure WARN | low | mitigate | logs exception class/message through shared closeQuietly; no record data or config | closed |
| T-40-08 | Tampering | scope of writer half | high | mitigate | five-file commit; src/main delta limited to four declared files | closed |
| T-40-09 | Repudiation | gate A | medium | mitigate | E-07 pass after authorized test-only seed (`fae1fcb1`), failed attempt retained | closed |
| T-40-10 | Denial of Service | stop() vs refresh race | low | accept | documented limit in FORK-DELTA Known cost 13 | closed |
| T-40-11 | Tampering | exactly-once state read/write | high | mitigate | getStateRecord..EOF, injected ctor, init() byte-identical (E-09); KeeperStateProvider untouched by review fixes | closed |
| T-40-12 | Denial of Service | exactly-once client leak | medium | mitigate | `KeeperStateProvider.close()` closes owned client only; WR-02 `53c0f06b` | closed |
| T-40-13 | Denial of Service | Keeper ctor failure leak (V1) | low | accept | residual in Known cost 13 | closed |
| T-40-14 | Tampering | ProxySinkTask shape | medium | mitigate | constructor pin passes; anchors rows=180 bad=0 | closed |
| T-40-15 | Repudiation | gate B / phase gate | medium | mitigate | E-10, E-11 PASS; post-review full suite 2317/18 skips | closed |
| T-40-16 | Denial of Service | stop() step masking | low | accept | now first failure wins, rest suppressed (WR-02) | closed |
| T-40-17 | Denial of Service | ClickHouseSinkTask.stop() order | low | accept | residual, no strategy throws today | closed |
| T-40-SC | Tampering | package installs | low | accept | none installed | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-40-01 | T-40-10 | 5 s awaitTermination timeout race, logged limit | plan-time register | 2026-11-02 |
| AR-40-02 | T-40-13 | Keeper constructor failure leak, out of declared scope | plan-time register / OQ-1 | 2026-11-02 |
| AR-40-03 | T-40-17 | deliveryStrategy.stop() ordering, no strategy throws | plan-time register | 2026-11-02 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-02 | 18 | 18 | 0 | orchestrator (grep-depth classification) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-02
