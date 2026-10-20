---
phase: "18"
slug: "writer-correctness"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-12"
---

# Phase 18 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka record data -> exception text | Exception messages reach Connect task status, DLQ headers and JMX, read by people without data access | Record values (sensitive) |
| Kafka record data -> connector logs | ERROR logs are shipped to log aggregation that operators and vendors read without data access | Record values (sensitive) |
| Writer failure -> retry classifier | `Utils.handleException` decides retry, DLQ or task failure from the exception chain | Exception chain |
| Kafka offsets -> exactly-once state | `Processing` turns the batch's offset range into BEFORE/AFTER state and the dedup token | Offsets, dedup token |
| Writer serialization -> ClickHouse insert | What the writer drops never reaches the server; what it keeps must be identical on retry | Serialized batch bytes |
| Column names -> log lines | Column names come from the server's DESCRIBE metadata, not from records | Table metadata (non-sensitive) |
| Test harness -> production object graph | Tests reach private writer fields/methods by reflection; the mutation check edits the working tree temporarily | Test-only access |
| Fork code -> next upstream sync | Patched upstream lines can silently revert on a merge | Fork anchor rows |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-18-01 | Information Disclosure | WRT-10 DataException text in `doWriteCol` | medium | mitigate | Text built only from `col.getName()` and `renderColumnType(col)`; `MissingNonNullableValueWriteTest` + live WRT-10 case assert SENTINEL absent from every cause-chain message | closed |
| T-18-02 | Denial of Service | retry classification in `doInsertRawBinary` / `Utils.handleException` | high | mitigate | Code 33 retry-once twin and timeout-prefix pins pass; put()-level pins cover tolerance none, tolerance all and timeout | closed |
| T-18-03 | Tampering | `fork-anchors.txt` rows vs patched upstream lines | medium | mitigate | Anchor rows (incl. zero-count rows) checked at HEAD per task; `verify-merge-invariants.sh` run per plan | closed |
| T-18-04 | Elevation of Privilege | reflective swap of `Processing.dbWriter` in `WriterErrorPathLiveTest` | low | accept | Test-only code under `src/test`; no production class touched; container password never logged | closed |
| T-18-05 | Information Disclosure | `doWriteDates` DateTime/DateTime64 ERROR logs (T-13-19) | high | mitigate | `logDateTimeParseFailure` reduces exception to class name and value to length; `WriterLogMaskingUnitTest` asserts sentinel absent from message, throwable string and every cause | closed |
| T-18-06 | Information Disclosure | `doWriteColValue` generic-catch ERROR log | high | mitigate | Log carries no throwable argument; test asserts `hasThrowable()` false and no sentinel; zero-count anchor row pins the old line out | closed |
| T-18-07 | Information Disclosure | message of exception rethrown by generic catch | medium | accept | Kept by D-09 so classification cannot change; pinned by test, FORK-DELTA item 6, backlog 999.3 | closed |
| T-18-08 | Tampering | log forging through column names | low | accept | Names come from DESCRIBE metadata or connector config, not record data | closed |
| T-18-09 | Tampering | exactly-once state and dedup token for batches with empty records | high | mitigate | Skip sits only in writer serialization loops; `EmptyCdcExactlyOnceStateTest` asserts AFTER_PROCESSING with full range, token covering full range, identical body+token on retry; phase scope check proves state-machine classes unchanged | closed |
| T-18-10 | Tampering | skip predicate breadth (real rows dropped) | high | mitigate | Predicate is DEBEZIUM_CDC + null/empty raw map before cleanup; live tests read back every real row with `assertEquals` on every route; bypass pins + mutation check | closed |
| T-18-11 | Denial of Service | zero-byte insert for an all-skipped batch | medium | mitigate | Live all-empty batches on every route on clients V1 and V2; UAT test 1 passed | closed |
| T-18-12 | Repudiation | phantom rows written by earlier releases | low | accept | Not repaired (operator data); ledger L-30 carries detection query and token-based upgrade note for Phase 20 rollout (ROLL-02); UAT test 2 passed | closed |
| T-18-13 | Information Disclosure | skip DEBUG line | low | mitigate | Topic, partition and offset only through an anchored constant; record value never an argument | closed |
| T-18-14 | Tampering | mutation check's temporary removal of the two skip call sites | medium | mitigate | Restored with `git checkout`; `git diff --quiet -- src/main` in verify (re-confirmed clean at audit time); anchors gate pins `skipEmptyCdcRecord(` at 3 (re-confirmed: 3) | closed |
| T-18-15 | Repudiation | phase-gate verdict read from stale or partial results | medium | mitigate | Gate deletes result directories, runs both suites itself, rejects results older than last code commit, requires every Phase 18 class, reads BUILD SUCCESSFUL from its own log | closed |
| T-18-16 | Elevation of Privilege | reflection into `ClickHouseWriter` privates in `CapturingClickHouseWriter` | low | accept | Test-only code under `src/test`; mirrors existing `MetadataFreeJsonInsertUnitTest` seam | closed |
| T-18-SC | Tampering | npm/pip/cargo installs | low | accept | No package installed or changed (RESEARCH Package Legitimacy Audit: none); Mockito and reload4j already on test classpath | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-18-01 | T-18-04 | Reflection is test-only, touches no production class, logs no credential | Phase 18 plan (18-01) | 2026-10-12 |
| R-18-02 | T-18-07 | Rethrown message kept by D-09 so retry classification cannot change; tracked as backlog 999.3 / FORK-DELTA item 6 | Phase 18 plan (18-02) | 2026-10-12 |
| R-18-03 | T-18-08 | Column names derive from DESCRIBE metadata/config, not record data; log injection is OWASP canon | Phase 18 plan (18-02) | 2026-10-12 |
| R-18-04 | T-18-12 | Phantom rows from earlier releases are operator data; detection query and upgrade note recorded in ledger L-30 for Phase 20 rollout | Phase 18 plan (18-03) | 2026-10-12 |
| R-18-05 | T-18-16 | Reflection is test-only and mirrors an existing test seam | Phase 18 plan (18-04) | 2026-10-12 |
| R-18-06 | T-18-SC | No dependency added or changed in this phase | Phase 18 plans (all) | 2026-10-12 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-12 | 17 | 17 | 0 | gsd-secure-phase (L1 short-circuit, register authored at plan time) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-12
