---
phase: "42"
slug: "isolated-high-risk-edits"
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-02"
---

# Phase 42 — Security

> Per-phase security contract. Classified at grep depth by the orchestrator against the plan-time registers (no auditor spawned; all high threats mitigated, ASVS 1).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Exactly-once state path (Processing → StateProvider → KeeperMap) | RSK-01 edit adjacent to state calls | offsets, topic names (state key unchanged) |
| Error classification (Utils.handleException) | RSK-02 chain-form pass-through | exceptions |
| Start-up log | RSK-03 WARN | integer error codes only |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-42-01 | Tampering | state key and KeeperMap rows | high | mitigate | `KeeperStateProvider`, `sink/state`, `sink/kafka` diffs empty; BEFORE/AFTER sequence untouched; `ProcessingTest` 14 unchanged; review RSK-01 clean | closed |
| T-42-02 | Tampering | consumer offsets under many-to-one rename / partition-changing SMT with reportInsertedOffsets | medium | mitigate | limits pinned (measured) and recorded in Known cost 16 | closed |
| T-42-03 | Repudiation | RSK-01 red run | medium | mitigate | E-02 red run, test SHA identical after edit | closed |
| T-42-04 | Repudiation | baseline and gate totals | medium | mitigate | live JUnit totals vs recorded baseline, per-testcase status deltas | closed |
| T-42-05 | Information Disclosure | assertion messages | low | mitigate | constants only | closed |
| T-42-06 | Denial of Service | RetriableException under tolerance=all | medium | mitigate | chain-form pass-through; behaviour changes documented in Known cost 17 | closed |
| T-42-07 | Tampering | existing classification rows | high | mitigate | expected-added list committed before edit; actual diff equals list (E-06/E-07); 0 existing rows changed | closed |
| T-42-08 | Denial of Service | cyclic cause chain | low | mitigate | initially accepted; review WR-01 found a hang, fixed with an identity-set guard (`1c0d9d00`) and a preemptive-timeout test | closed |
| T-42-09 | Repudiation | red run vs committed test | medium | mitigate | SHA-256 identical | closed |
| T-42-10 | Denial of Service | operator lists a permanent code in additionalRetriableErrorCodes | medium | mitigate | WARN at start-up (RSK-03); no validation by design; documented in Known cost 24 | closed |
| T-42-11 | Information Disclosure | WARN text | low | mitigate | integer codes only, no record data | closed |
| T-42-12 | Denial of Service | start-up regression | medium | mitigate | config parsed inside existing try; helper cannot throw (review clean); live start test | closed |
| T-42-13 | Repudiation | red runs RSK-03 | medium | mitigate | E-08 red runs, SHA-256 equal | closed |
| T-42-14 | Repudiation | each edit reverts alone | medium | mitigate | E-11 reverts in throwaway worktree, each green; RSK-02 revert unit is now c8574890 + 1c0d9d00 (documented) | closed |
| T-42-15 | Tampering | main working tree during revert check | high | mitigate | worktree only; main tree status unchanged | closed |
| T-42-16 | Tampering | fork anchors across commits | medium | mitigate | invariants script PASS (E-12), anchors rows=190 bad=0 after fix | closed |
| T-42-17 | Repudiation | scope of the phase | medium | mitigate | src/main differs in exactly 3 files; the review fix stays inside the RSK-02 loop | closed |
| T-42-SC | Tampering | package installs | low | accept | none installed | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-42-01 | T-42-SC | no package installs | plan-time register | 2026-11-02 |

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
