---
phase: "43"
slug: "document-fixes-phase-37-dispositions"
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-02"
---

# Phase 43 — Security

> Per-phase security contract. Classified at grep depth by the orchestrator against the plan-time registers (no auditor spawned; all high threats mitigated, ASVS 1).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Published rollout notes / FORK-DELTA | Documents shipped with release tags | factual claims, public GitHub facts |
| User-owned gates | src/main rows, existing-assertion changes | approvals |
| CI workflow header comment / allow-lists | comment-only edits | none |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-43-01 | Tampering | increment-version.yml | medium | mitigate | header comment only; anchored strings not on changed lines; six workflow guard classes green | closed |
| T-43-02 | Elevation of privilege | gate "changing an existing assertion" (37-IN-04) | high | mitigate | user answer quoted verbatim ("Approve 37 IN-04, defer 28 IN-05(b)"); single CHAIN line added; no assertion deleted/weakened | closed |
| T-43-03 | Repudiation | "tests first" claim | medium | mitigate | red runs recorded (E-03/E-04), frozen-file SHA checks | closed |
| T-43-04 | Tampering | shipped allow-lists | low | mitigate | only gg4/gg5 touched; 25 and gg6 lists unchanged; gg6 list byte-identical to tag copy | closed |
| T-43-05 | Repudiation | published-note claims | medium | mitigate | claims re-verified; labels honest; review WR-01 overclaim corrected (`d77da0f9`) | closed |
| T-43-06 | Information Disclosure | GitHub REST facts | low | mitigate | unauthenticated public GETs; no tokens recorded | closed |
| T-43-07 | Repudiation | publication facts in FORK-DELTA | medium | mitigate | 30 facts re-read, all matched (E-06); review WR-02 fixed (`5c6c40e1`) | closed |
| T-43-08 | Tampering | unintended edits of published docs | medium | mitigate | exact 10-line FORK-DELTA delta; raw numbers table unchanged; gg6 diff vs tag limited to rows C2/D7 + notice | closed |
| T-43-09 | Repudiation | ledger completeness | medium | mitigate | checker: 18 rows, 0 problems; 12 carried rows match Phase 37 | closed |
| T-43-10 | Elevation of privilege | user-owned gate: src/main rows | high | mitigate | all four rows deferred per user answer; src/main diff empty | closed |
| T-43-11 | Tampering | fork anchors | medium | mitigate | rows=190 bad=0 after every change | closed |
| T-43-12 | Repudiation | phase scope | medium | mitigate | non-planning commits limited to the planned set | closed |
| T-43-SC | Tampering | package installs | low | accept | none installed | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-43-01 | T-43-SC | no package installs | plan-time register | 2026-11-02 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-02 | 13 | 13 | 0 | orchestrator (grep-depth classification) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-02
