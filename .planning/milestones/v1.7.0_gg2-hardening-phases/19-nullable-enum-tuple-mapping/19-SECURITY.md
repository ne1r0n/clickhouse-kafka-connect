---
phase: "19"
slug: "nullable-enum-tuple-mapping"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-14"
---

# Phase 19 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Audited inline in Phase 20 (plan 20-05, D-09) before the v1.7.0_gg2 merge gate, at ASVS level 1 with `security_block_on: high`.
> The register is the union of the `<threat_model>` blocks of plans 19-01 to 19-10, authored at plan time.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| DESCRIBE metadata -> Column tree -> RowBinary bytes | The nullability flag of every Enum, Tuple, Tuple element and nested slot decides whether the writer emits a null marker; a wrong flag shifts every following byte | Table metadata (non-sensitive), serialized batch bytes |
| Declared type string -> `ClickHouseColumn.of` parser | The pass parses the server's declared column type; an exotic or malformed type must not stop table discovery | Table metadata (non-sensitive) |
| Fail-open WARN -> connector logs | The WARN reaches log aggregation read by people without data access | Column, database, table and raw type names, exception class name |
| Describe-time pass -> exactly-once retry | The pass runs once per DESCRIBE, before the Table is published, so a retried batch sees the same Table | Offsets, dedup token, batch bytes |
| Test harness -> test containers and working tree | Live matrix and round-trip tests create tables with synthetic data; the mutation run edits `src/main` temporarily | Synthetic ASCII records |
| Fork code -> next upstream sync | Patched upstream lines can silently revert on a merge | Fork anchor rows |
| Review/disposition record -> release decision | The user decides on these records | Findings and dispositions |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-19-01 | Tampering | 19-SPIKE.md matrix versus the measured run | medium | mitigate | `19-SPIKE.md` carries both twin matrices, the control, the DESCRIBE rows, the recommendation and the refreshed gate; `NullableTupleMatrixLiveTest` stays at exactly 30 cases (19-10 gate) as the permanent record | closed |
| T-19-02 | Information Disclosure | matrix logs and 19-SPIKE.md | low | mitigate | Synthetic ASCII literals only; logs carry table keys, column names, raw types and the server version | closed |
| T-19-03 | Denial of Service | tables left in the test container | low | mitigate | `finally` blocks drop the tables in `NullableTupleMatrixLiveTest` (5 present); unique table prefix plus timestamp | closed |
| T-19-04 | Tampering | `Column.extractColumn` Enum branch -> stored values | high | mitigate | `.isNullable(isNull)` on the Enum return at `Column.java:212`, anchored (3 rows, COLS-10); live twin round trips and Docker-free byte pins; 19-09 mutation | closed |
| T-19-05 | Information Disclosure | the narrowed discovery WARN text | low | mitigate | The Tuple discovery WARN no longer exists (removed by 19-04, COLS-11); no WARN in `Column`, `Table` or the describe helper carries a record value | closed |
| T-19-06 | Tampering | fork-anchors.txt versus the patched lines | medium | mitigate | All 59 anchor rows match their counts at HEAD; `verify-merge-invariants.sh 54e63648 HEAD` prints `result=PASS` | closed |
| T-19-07 | Tampering | retry byte identity (exactly-once), Enum change | medium | mitigate | `Processing`, `ProxySinkTask`, `state`, `kafka` and `Table` are byte-identical to the Phase 19 base `0f35a4c0` (git diff clean); the Enum change is a DESCRIBE-time flag | closed |
| T-19-08 | Elevation of Privilege | the Tuple go/no-go | high | mitigate | 19-03 SUMMARY: the user answered `go` directly to the orchestrator; `19-SPIKE.md` `## Decision` carries the answer; 19-04 ran only after it | closed |
| T-19-09 | Repudiation | the bundle the user decided on | medium | mitigate | `19-SPIKE.md` `## Gate refresh` records the matrix re-run on the day's `latest` (26.9.17.8) after the Enum fix | closed |
| T-19-10 | Tampering | both alternatives running, or neither | medium | mitigate | `19-05-SUMMARY.md` reads `status: superseded`, `superseded_by: "19-04"`; 19-04 delivered the fix | closed |
| T-19-11 | Tampering | declared-nullability pass -> stored values | high | mitigate | `DeclaredNullability.apply` is called from `buildDescribedTable` in both describe paths (anchored); typed read-back on both twins, Docker-free byte and pairing pins, mutation run (19-09); see the cross-check note on IN-03 and IN-04 | closed |
| T-19-12 | Denial of Service | `ClickHouseColumn.of` on an exotic or malformed type | medium | mitigate | The parse sits inside the `RuntimeException` catch of `apply` (a rejected type fails open); BUT the claim that a non-pairing tree is detected before anything is applied is limited by IN-04 (a declared element with no Column is not detected) and the same catch is limited by WR-01 | open — below high threshold (non-blocking) |
| T-19-13 | Information Disclosure | fail-open WARN text | low | mitigate | `DeclaredNullability.java` contains no `getMessage` (count 0); the WARN logs the exception class name only | closed |
| T-19-14 | Tampering | retry byte identity (exactly-once), Tuple pass | high | mitigate | The pass runs in `buildDescribedTable` before the Table is returned, with no per-record state; state-machine classes and `Table.java` are unchanged since `0f35a4c0` | closed |
| T-19-15 | Tampering | fork-anchors.txt versus the patched lines | medium | mitigate | Anchors rows 71-75 (COLS-10) match at HEAD; invariants script `result=PASS` | closed |
| T-19-16 | Repudiation | WARN text and Known cost versus the matrix (alternative branch) | medium | mitigate | Component not shipped: 19-05 was superseded, and the Tuple WARN it would have pinned was removed by 19-04 | closed |
| T-19-17 | Information Disclosure | WARN text (alternative branch) | low | mitigate | Component not shipped (as T-19-16); no Tuple discovery WARN remains in `src/main/java` | closed |
| T-19-18 | Tampering | fork-anchors.txt and the WARN predicate (alternative branch) | low | mitigate | Component not shipped (as T-19-16); anchors rows match at HEAD | closed |
| T-19-19 | Repudiation | 15-REVIEW IN-01..IN-05 dispositions | medium | mitigate | 19-06 SUMMARY: IN-01..IN-05 set to `fixed` (superseded) citing `2a94e045` in the archived Phase 15 disposition file | closed |
| T-19-20 | Repudiation | phase-gate verdict from stale or partial results | medium | mitigate | 19-06 SUMMARY records the cache-busted testUnit and two full runs plus anchors PASS | closed |
| T-19-21 | Tampering | `DeclaredNullability.collect` leaf rule for `SimpleAggregateFunction(f, Nullable(T))` (CR-01) | high | mitigate | `unwrapSimpleAggregate` is called at the entry of `collect` (`DeclaredNullability.java:119`); anchored (2 rows); `DeclaredNullabilityTest` CR-01 pins, `SimpleAggregateTupleRoundTripLiveTest` on both twins; 19-REVIEW CR-01 is `fixed` (`4e133d4e`) | closed |
| T-19-22 | Denial of Service | unwrap loop on an unexpected parse shape | low | mitigate | The loop advances only while the node is a `SimpleAggregateFunction` with a non-empty nested list (`DeclaredNullability.java:111-112`) | closed |
| T-19-23 | Tampering | fork-anchors.txt versus the patched lines | medium | mitigate | Anchor row for `unwrapSimpleAggregate(` (count 2) matches at HEAD | closed |
| T-19-24 | Information Disclosure | live-test log lines | low | mitigate | `SimpleAggregateTupleRoundTripLiveTest` logs DESCRIBE metadata with synthetic integer records | closed |
| T-19-25 | Tampering | retry byte identity (exactly-once), SimpleAggregateFunction change | high | mitigate | State-machine classes and `Table.java` unchanged since the phase base; `ClickHouseWriter.java` differs from the base only by one comment edit made in 19-04 (`2a94e045`, no code change) | closed |
| T-19-26 | Denial of Service | `DeclaredNullability.apply` -> `describeTableV1` (`extractTablesMapping` loop) | medium | mitigate | One `try` spans parse, pairing and assignment with one `RuntimeException` catch, pinned by the null-type case; BUT 19-REVIEW WR-01 shows the guarantee is not total: the catch handler dereferences `root.getName()` (a null root throws a second NPE) and `containsTuple(root)` in `buildDescribedTable` sits outside any guard. Both paths are unreachable today (`extractColumn` always sets a type and a root) | open — below high threshold (non-blocking) |
| T-19-27 | Information Disclosure | fallback WARN text | low | mitigate | The WARN adds database and table names (catalog metadata); the reason is the exception class name; no `getMessage` | closed |
| T-19-28 | Repudiation | 19-REVIEW-DISPOSITION.md closure record | medium | mitigate | The disposition file records all eight findings with `open: 0`, each deferred row with a written reason, each fixed row with a commit | closed |
| T-19-29 | Tampering | test refactor weakening assertions | medium | mitigate | 19-10 gate: matrix exactly 30 cases, CR-01 live class exactly 2, the same 18 named skips and nothing else | closed |
| T-19-30 | Tampering | gate verdict from stale results | medium | mitigate | 19-10 SUMMARY: result directories deleted first, each XML newer than the last code commit, three runs | closed |
| T-19-31 | Tampering | mutated `DeclaredNullability.java` reaching a commit | high | mitigate | `git log` of `DeclaredNullability.java` shows only the three reviewed fix commits (`2a94e045`, `4e133d4e`, `88e4e854`) and `git status` of `src` is clean; the file at HEAD has the unwrap and no mutation; anchors PASS | closed |
| T-19-32 | Tampering | flags of `LowCardinality(Nullable(...))` and wrapper-adjacent shapes -> RowBinary bytes | high | mitigate | 19-09 commit `01e932e3` pins LowCardinality and SimpleAggregateFunction wrapper edges on the Tuple-element, Array-element and Map-value positions | closed |
| T-19-33 | Repudiation | deferred findings lost between phases (L-38, IN-04) | medium | mitigate | The ROADMAP Phase 20 entry carries the WR-02 (L-38) and IN-04 bullets; Phase 20 plan 20-04 fulfilled L-38 in the rollout note and CHANGELOG and copied IN-04 to the close record | closed |
| T-19-34 | Repudiation | closure record ahead of its evidence | medium | mitigate | 19-10 SUMMARY orders gate, anchors, record, and states the gate logs are older than the docs commit and newer than the last code commit | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Cross-check against the Phase 19 review (D-09)

The auditor checks that a mitigation is present; the review records where one is wrong or limited. Each closed threat was compared with the 19-REVIEW findings and dispositions (CR-01, WR-01..WR-03, IN-01..IN-04):

| Finding | Threats compared | Outcome |
|---------|------------------|---------|
| CR-01 (SimpleAggregateFunction element flipped to non-nullable) | T-19-11, T-19-21, T-19-22, T-19-31, T-19-32 | Fixed in `4e133d4e`; the unwrap is in the source and anchored; T-19-21 closed on that evidence |
| WR-01 (totality of `apply` not met on the handler and the caller) | T-19-26, T-19-12 | Contradicts T-19-26's totality claim; T-19-26 recorded open (medium, non-blocking). T-19-12 shares the same catch and is recorded open with it |
| WR-02 (vacuous WARN-name assertions) | T-19-13, T-19-27 | The assertions are weak, the WARN content itself is correct (class name only, no `getMessage`); both stay closed on the source evidence, and the test weakness is a deferred finding |
| WR-03 (FORK-DELTA per-twin codes unpinned) | none | Documentation wording; fixed in `3e39867d`; no threat depends on it |
| IN-01, IN-02 (duplicated constant, brittle pins) | T-19-29 | No effect on a mitigation; T-19-29 stays closed |
| IN-03 (`normalise` rewrites every backslash-n pair) | T-19-11, T-19-12 | A Tuple element name or enum label containing a literal backslash-n fails open to DESCRIBE's nullability with a WARN. This is the designed failure policy, so T-19-11 stays closed, but note for the approval: for a `Nullable(Tuple(...))` column of that rare shape the old DESCRIBE flag shift would persist, with a WARN naming the column |
| IN-04 (a Tuple Column lacking a declared element still applies) | T-19-11, T-19-12 | No DESCRIBE shape that triggers it has been observed; pinned by `missingDeclaredElementDoesNotShiftPairing`. T-19-12's "non-pairing tree is detected" claim is one-directional, so T-19-12 is recorded open |

Neither open threat is at or above `high`, so `threats_open` stays 0. Both are carried by the deferred findings in `19-REVIEW-DISPOSITION.md` and the Phase 20 milestone close record, each with a reason and a re-open trigger. Any change to close them touches `src/main`, which is outside Phase 20 (D-08).

Unregistered flags: none. The `## Threat Flags` sections of the 19-01, 19-02, 19-06, 19-07 and 19-08 SUMMARYs say none; the 19-03, 19-04, 19-09 and 19-10 SUMMARYs carry no such section.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| none | - | No threat in the Phase 19 register has disposition `accept`; the two open non-blocking threats are tracked as deferred findings, not accepted | - | 2026-10-14 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-14 | 34 | 32 | 2 (non-blocking, below high) | inline audit in Phase 20 plan 20-05 (gsd-secure-phase procedure, `gsd-security-auditor.md` followed inline: sub-agent spawning unavailable to the executor) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log (none)
- [x] `threats_open: 0` confirmed (no open threat at or above `high`)
- [x] `status: verified` set in frontmatter
- [x] Every closed threat cross-checked against the 19-REVIEW findings

**Approval:** verified 2026-10-14 (audit result; the release approval is the user's own, in plan 20-06 and 20-07)
