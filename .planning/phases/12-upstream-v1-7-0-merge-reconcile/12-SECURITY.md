---
phase: "12"
slug: "upstream-v1-7-0-merge-reconcile"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-07"
---

# Phase 12 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| upstream tag → fork tree | Upstream code enters the production fork through the merge commit | source code (integrity-critical) |
| local repo → origin | A push publishes refs and fires CI; `custom_build` is production | git refs |
| planning script → git object store | The scripts create dangling objects only; refs must not change | git objects |
| Gradle resolution → shipped jar | The capability rule decides which lz4-java ships in the Confluent archive | dependency coordinates |
| Kafka record → RowBinary stream | A null optional Map from an untrusted record becomes bytes the server parses | record data |
| connector → ClickHouse server (HTTP) | Response encoding is negotiated by `compress` and `network_compression_method` | wire protocol |
| operator config → client options | `jdbcConnectionProperties` can override connector defaults | configuration |
| connector → KeeperMap state table | Exactly-once state decides whether a batch is re-sent or dropped | offset state (consistency-critical) |
| test working tree → commit | A temporary production mutation must never cross into history | source code |
| probe/test output → planning docs | Recorded Facts transcribe run output that may embed connection details | potentially credentials |
| phase evidence → published fork ledger | FORK-DELTA tells operators and future syncers what the fork guarantees | documentation |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-12-01 | Tampering | Merge commit M (`54e63648`) | high | mitigate | Line-provenance invariant: `verify-merge-invariants.sh 54e63648 HEAD` → `result=PASS` (21 provenance paths); self-test ST2 PASS | closed |
| T-12-02 | Spoofing | Upstream ref selection | high | mitigate | `54e63648^2` = `379b8d01` = `refs/tags/v1.7.0`; self-test ST3 PASS | closed |
| T-12-03 | Repudiation | Fork anchors drifting silently | medium | mitigate | Invariant 3 over 16 anchor commits, 40 anchor rows → PASS; self-test ST4 PASS | closed |
| T-12-04 | Elevation of Privilege | origin / custom_build | high | mitigate | `custom_build` = `origin/custom_build` = `c61d90ce`; no push | closed |
| T-12-05 | Tampering | ClickHouseWriter `case MAP:` (RowBinary desync) | high | mitigate | `ClickHouseWriter.java:614` guard is `value.getObject() == null && col.isNullable()`; `{0x00}` / `{0x00, 0x00}` byte pins in `NestedNullableCompositeWriteTest`; upstream `nullMapTypeTest` green | closed |
| T-12-06 | Tampering | CR-02 pins weakened to fit new code | medium | mitigate | `git diff 54e63648 HEAD -- NestedNullableCompositeWriteTest.java` has 0 removed lines | closed |
| T-12-07 | Tampering | Duplicate LZ4 rule masking the resolved version | medium | mitigate | `withCapability` count in `build.gradle.kts` = 1; `dependencyInsight` shows one `at.yawk.lz4:lz4-java:1.10.4` (12-02 SUMMARY) | closed |
| T-12-08 | Denial of Service | Builder site missing `setCompressionMethod` | high | mitigate | `setCompressionMethod(` present in `ClickHouseHelperClient`, `ClickHouseWriter`, `KeeperStateProvider`; live V1/V2 flag-false round trips green (`NetworkCompressionPinLiveTest`) | closed |
| T-12-09 | Denial of Service | Operator `compress=true` in `jdbcConnectionProperties` vs 26.9+ server | medium | accept | Upstream behaviour; pinned by `operatorCompressTrueOverridesV2ResponseCompression`; carried to ROLL-01 (Phase 15) | closed |
| T-12-10 | Information Disclosure | Probe/test error text in SUMMARY and Recorded Facts | medium | mitigate | Credential grep on all 12-0x SUMMARYs, `FORK-DELTA.md`, `MERGE-CHECKLIST-v1.7.0.md` → 0 real hits (the single 12-05 hit is the grep pattern itself) | closed |
| T-12-11 | Tampering | Probe class leaking into the committed suite | low | mitigate | No `probe` path in `git ls-files` nor in `pre-v1.7.0-sync..HEAD` history | closed |
| T-12-12 | Tampering | KeeperStateProvider temporary mutation | high | mitigate | `git diff refs/tags/v1.7.0 -- KeeperStateProvider.java` empty; 0 commits touch it after M | closed |
| T-12-13 | Tampering | SQL string interpolation in KeeperStateProvider | low | accept | Upstream code, out of scope; test uses generated identifiers only | closed |
| T-12-14 | Information Disclosure | KeeperStateProvider constructor log line | low | accept | Upstream masks via `Mask.passwordMask` (`KeeperStateProvider.java:48`) | closed |
| T-12-15 | Repudiation | FORK-DELTA load-bearing-control sentence | medium | mitigate | `FORK-DELTA.md:77` cites Recorded Facts 25 and 26 and states the V1 stripped-both case was not observed; human confirmation of wording remains an open review item (non-blocking) | closed |
| T-12-16 | Information Disclosure | Recorded Facts / FORK-DELTA transcribing run output | medium | mitigate | Same credential grep as T-12-10 → 0 | closed |
| T-12-17 | Tampering | Checklist rows closed without evidence | low | mitigate | Remaining `pending` rows in `MERGE-CHECKLIST-v1.7.0.md` are owned by phases 13/14 only | closed |
| T-12-18 | Elevation of Privilege | origin / custom_build at end of phase | high | mitigate | `custom_build` = `origin/custom_build` = `c61d90ce` after 12-05 | closed |
| T-12-SC | Tampering | Dependency resolution (Gradle/Maven) | high | mitigate | No install task; catalog diff `54e63648..HEAD` empty; only lz4-java is upstream's 1.10.4 from immutable tag `379b8d01` | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-12-01 | T-12-09 | Upstream behaviour, not a fork patch; not on a trust boundary the fork controls; pinned by a unit test and recorded for ROLL-01 (Phase 15) | plan-time disposition (12-03-PLAN) | 2026-10-07 |
| AR-12-02 | T-12-13 | Upstream KeeperStateProvider code, out of scope per REQUIREMENTS; file kept byte-identical to v1.7.0 | plan-time disposition (12-04-PLAN) | 2026-10-07 |
| AR-12-03 | T-12-14 | Upstream already masks the password; the new test logs nothing of its own | plan-time disposition (12-04-PLAN) | 2026-10-07 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-07 | 19 | 19 | 0 | gsd-secure-phase orchestrator (ASVS L1 grep-depth, plan-time register; auditor short-circuited) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-07
