---
phase: "41"
slug: "bounded-insert-timeout"
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-02"
---

# Phase 41 — Security

> Per-phase security contract. Classified at grep depth by the orchestrator against the plan-time registers (no auditor spawned; all high threats mitigated, ASVS 1).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Connector task thread → ClickHouse HTTP | Insert request with per-insert socket timeout | record batches (unchanged) |
| Operator config → client options | `jdbcConnectionProperties` `socket_timeout` | config string |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-41-01 | Repudiation | recorded red runs | medium | mitigate | E-02/E-03 red runs recorded, failing via preemptive timeout | closed |
| T-41-02 | Tampering | red-tree patch | medium | mitigate | `41-red-pins.patch` SHA-256 recorded (10c738b2…) | closed |
| T-41-03 | Tampering | src/main during red phase | high | mitigate | src equals HEAD after plan 41-01 | closed |
| T-41-04 | Denial of Service | test-suite stability | medium | mitigate | stub releases latch before stop(0); handler pool daemon threads | closed |
| T-41-05 | Information Disclosure | assertion messages / evidence | low | mitigate | loopback stub, synthetic data only | closed |
| T-41-06 | Denial of Service | stalled V2 insert pins task thread | high | mitigate | per-insert socket_timeout at 3 V2 sites; pins green (15 cases); WR-02 guards non-positive | closed |
| T-41-07 | Tampering | timed-out insert still committed (duplicate risk) | medium | accept | recorded in Known cost 14 and rollout item (measured: 4000 rows committed) | closed |
| T-41-08 | Tampering | operator socket_timeout overridden | high | mitigate | once-computed operator flag, mutation run failed exactly 10 operator cases (E-07); WR-03 edge cases | closed |
| T-41-09 | Information Disclosure | jdbcConnectionProperties parsing | low | mitigate | parser reads key names only; no logging of values | closed |
| T-41-10 | Tampering | change scope (config, classification, async_insert) | high | mitigate | ClickHouseSinkConfig byte-identical vs phase start; CFG-02 anchor count 1; anchors rows=184 bad=0 | closed |
| T-41-11 | Repudiation | gate totals | medium | mitigate | live JUnit totals vs recorded baseline (E-08) | closed |
| T-41-12 | Repudiation | rollout claims | medium | mitigate | 13 labelled claims; progress-header claim labelled inferred | closed |
| T-41-13 | Tampering | fork anchors across commits | medium | mitigate | anchors check at each commit; invariants script PASS (E-10) | closed |
| T-41-SC | Tampering | package installs | low | accept | none installed | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-41-01 | T-41-07 | a timed-out insert may still commit; duplicate risk with exactlyOnce=false documented for operators | plan-time register | 2026-11-02 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-02 | 14 | 14 | 0 | orchestrator (grep-depth classification) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-02
