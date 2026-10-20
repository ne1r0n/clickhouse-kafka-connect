---
phase: "23"
slug: "declared-nullability-hardening-review-closure"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-20"
---

# Phase 23 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Test code, assertions | Replacing a constant or converting an assertion can silently change what a premise or a WARN pin checks | Test assertions |
| Production class members | `DeclaredNullability.ESCAPED_NEWLINE` becomes visible outside its package | Immutable String constant |
| DESCRIBE metadata, discovery pass | Column trees and type strings come from the server and are trusted for shape only; a null or unexpected shape must not escape the pass | Table metadata |
| Server DESCRIBE text, normalise | The declared type string is server-supplied text with JSON escapes kept verbatim; it is untrusted for structure | Type strings |
| DESCRIBE rows, discovery skip, Column tree | A structural-row skip can drop a real Tuple element before the pass runs (999.4) | Table metadata |
| Declared nullability, writer byte layout | A wrong pairing or a fallback changes which slots get a null marker, which corrupts or rejects RowBinary inserts | Insert bytes (data integrity) |
| Pass, operator logs | The fallback and unpaired-element WARN texts are operator-visible and grepped and alerted on | Column, table and type names (metadata only) |
| Working tree during mutation runs | A mutated production file is in the tree between a run and its revert | Production source |
| Archived review records, future readers | A disposition row is the audit trail for why a finding is closed | Review dispositions |
| Local gate results, release decision | Phase 24 publishes on the strength of this gate | Gate logs and exit codes |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-23-01 | Tampering | Premise assertions in DeclaredNullabilityTest, NullableTupleMatrixLiveTest, SimpleAggregateTupleRoundTripLiveTest | medium | mitigate | `grep -rnF '= "\\n";' src` finds 1 definition (`DeclaredNullability.java:43`). E-01 `d14_commit` 4478016a, `live_d14`: SimpleAggregateTupleRoundTripLiveTest 2/0, declaredTupleTypesParseWithDdlNullability 1/0. | closed |
| T-23-02 | Elevation of Privilege | `DeclaredNullability.ESCAPED_NEWLINE` made public | low | accept | See R-23-01. | closed |
| T-23-03 | Denial of Service | `containsTuple` / `apply` called from `buildDescribedTable` | medium | mitigate | Null guards at `DeclaredNullability.java:120` (containsTuple), :144 and :149 (apply), :192, :220, :233, :236 and :241 (collect/collectTuple). E-02 `cols12_tracer_red` 3, `cols12_expansion_red` d07_failed=8, `cols12_green` 38/0. | closed |
| T-23-04 | Information Disclosure | Fallback WARN text | medium | mitigate | Reason is `STRUCTURE_MISMATCH` (`:58`) or `e.getClass().getSimpleName()` (`:171`), never a message or throwable. `hasThrowable` asserted false 8 times in DeclaredNullabilityTest and 2 in the fork unit test; one D-09 pin. | closed |
| T-23-05 | Repudiation | D-08 flip of an existing pin | low | mitigate | Scoped AGENTS.md exemption naming D-08 and ROADMAP SC1 at `DeclaredNullabilityTest.java:641`. E-02 `d08_flip`: method and null-type input kept; commit 4bbc9627 holds 6 files. | closed |
| T-23-06 | Tampering | Fallback-WARN content assertions | medium | mitigate | `assertWarnNamesColumn` used 17 times in DeclaredNullabilityTest and 5 in the fork unit test. E-03 `d17_fallback_m1` and `d17_fallback_m2`: rc=1, marked=14, missing=none. | closed |
| T-23-07 | Tampering | Mutated DeclaredNullability.java during the D-17 runs | medium | mitigate | `git diff --quiet 4bbc9627 075e6466 -- src/main` is empty. Commit 075e6466 holds only the two test files. E-03 `d17_reverted: yes`. | closed |
| T-23-08 | Tampering | `normalise` and pairing (data integrity of inserts) | high | mitigate | Unchanged-shapes pin `COLS-13 (D-10) unchanged` at `DeclaredNullabilityTest.java:334`, 6 cases green before and after. E-04: twin round trip red before, green after. `live_six_23_04` and `live_six_23_05`: six live classes 81/0. | closed |
| T-23-09 | Denial of Service | Scan over a pathological type string | low | accept | See R-23-02. | closed |
| T-23-10 | Tampering | Drift between the Docker-free literals and `latest` | medium | mitigate | NullableTupleMatrixLiveTest compares the `*_RAW` constants with server DESCRIBE text (12 `_RAW` uses, D-12). E-04 `d13_verdict`: not triggered; the live premise matched every captured literal on 26.9.19.9. | closed |
| T-23-11 | Tampering | Short Tuple write after discovery drops a `size`/`size0` element (999.4) | high | mitigate | Detective control: `sizeNamedTupleElementDroppedByDiscoveryIsNamedInOneWarning` in ClickHouseHelperClientForkUnitTest runs the R-01 rows through the real `shouldSkipDescribeRow` and asserts one WARN naming the dropped element. The corrective fix is backlog 999.4 in ROADMAP (user decision D-03). | closed |
| T-23-12 | Information Disclosure | UNPAIRED_ELEMENT_WARNING content | medium | mitigate | `DeclaredNullability.java:164` logs element names, column, database, table and raw type only, with no throwable. The 16 `COLS-14 unpaired-element WARN` testcases assert `hasThrowable()` false and one event. E-05 `d17_unpaired_m1`/`m2`: marked=16, missing=none. | closed |
| T-23-13 | Tampering | Log forging through control characters in element names | low | accept | See R-23-03. | closed |
| T-23-14 | Repudiation | D-19 flips of three existing tests | medium | mitigate | Scoped AGENTS.md exemptions naming D-19 (Option B) at `ClickHouseHelperClientForkUnitTest.java:421` and :489. E-05 `d19_forced_red`: rc=1, failed=3 (the three D-19 names). | closed |
| T-23-15 | Tampering | Re-introducing the double flag by falling back | high | mitigate | The D-04 pin asserts every Tuple field non-nullable as declared, including `other` that DESCRIBE reported `Nullable(String)`, and `DeclaredNullability.apply(...)` returns true (ClickHouseHelperClientForkUnitTest, `discoveryDropsSizeNamedElements`). | closed |
| T-23-16 | Repudiation | 17/19-REVIEW-DISPOSITION.md pointers | medium | mitigate | E-06: the DEBT-12 checker printed `errors: []`; every Source SHA is an ancestor of HEAD; IDs, titles, order, `total:`, `recorded:` and the footer are unchanged against 16e0b2d6. | closed |
| T-23-17 | Tampering | Gate results | medium | mitigate | E-07 `testunit_rerun` 838/0 and `full_suite` 1277/0 with `--rerun`, `full_suite_runs: 1` (no exit-255). `gate_final`: anchor_rows=69, anchor_exempt=1, result=PASS. Orchestrator post-merge gate re-checked: 89 JUnit XML files, 0 failures, 0 errors. | closed |
| T-23-18 | Elevation of Privilege | Publishing from the working branch | high | mitigate | Rechecked: no `origin` ref points at HEAD (`git ls-remote` match count 0), branch `sync/v1.7.0`, one worktree, and VERSION and CHANGELOG.md are unchanged since 16e0b2d6. E-07 `no_push: yes`. | closed |
| T-23-SC | Tampering | npm/pip/cargo installs | low | accept | See R-23-04. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-23-01 | T-23-02 | `ESCAPED_NEWLINE` is an immutable `public static final String` (`DeclaredNullability.java:43`), so widening its visibility exposes no state and no behaviour. | Plan threat model (23-01, accept at plan time) | 2026-10-20 |
| R-23-02 | T-23-09 | `normalise` is a single linear pass with no regex or backtracking (0 `Pattern`/`replaceAll`/`matches(` in `DeclaredNullability.java`). It is total for an unterminated quote (edge pins) and runs once per root per DESCRIBE, never per record. | Plan threat model (23-04, accept at plan time) | 2026-10-20 |
| R-23-03 | T-23-13 | Element names come from the table owner's DDL. `fromJsonRow` keeps escapes as text, so a real newline appears as backslash-n (R-02). The fallback WARN already prints the raw type, so the unpaired-element WARN adds no new class of exposure. | Plan threat model (23-05, accept at plan time) | 2026-10-20 |
| R-23-04 | T-23-SC (23-01 to 23-06) | No package, dependency or build change in Phase 23 (23-RESEARCH Package Legitimacy Audit: none). Rechecked: build.gradle.kts, gradle/ and .github/ untouched since 16e0b2d6. | Plan threat models (accept at plan time) | 2026-10-20 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-20 | 19 | 19 | 0 | gsd-secure-phase (L1 short-circuit: register authored at plan time, all mitigations evidenced) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-20
