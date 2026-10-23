---
phase: "27"
slug: "writer-failure-path-test-debt"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-23"
---

# Phase 27 — Security

> Per-phase security contract. The register was authored at plan time (27-01..27-05 threat models); this audit is a grep-depth check of each mitigation against the tree at HEAD.

## Threat Register

| Threat ID | Category | Severity | Disposition | Evidence | Status |
|-----------|----------|----------|-------------|----------|--------|
| T-27-01 | DoS | medium | mitigate | `JACKSON_OVER_SOCKET_TIMEOUT` / `JACKSON_OVER_TIMEOUT_TEXT` rows in `WriterFailureClassificationUnitTest`, green in the 1680-test gate (E-13) | closed |
| T-27-02 | Repudiation | low | mitigate | 184 base case names all present; E-02 | closed |
| T-27-03 | Info disclosure | high | mitigate | `ClickHouseWriter.java:659` `!(t instanceof JacksonException)`; anchor row WRT-17; live pins flipped to key-absent (E-04 sign-off) | closed |
| T-27-04 | DoS | medium | mitigate | `jacksonOverSocketTimeoutPassesThrough` and WRT-16 rows green after the fix | closed |
| T-27-05 | Info disclosure | low | accept | user sign-off 2026-10-22 ("Accept residual, proceed."); FORK-DELTA Known costs item 6 | closed |
| T-27-06 | Tampering | medium | mitigate | fork-anchors row `!(t instanceof JacksonException)` = 1; tail gate `anchor_rows=83 result=PASS` | closed |
| T-27-07 | Info disclosure | low | accept | recorded as backlog candidate in the behaviour ledger | closed |
| T-27-08 | Repudiation | medium | mitigate | E-03 `live_red` / `predicate_red`; E-04 verbatim user answer 2026-10-23T00:06:49Z | closed |
| T-27-09 | Info disclosure | high | mitigate | `Column.java:44-45` formats take only the column name; ColumnTest asserts label absence | closed |
| T-27-10 | DoS | medium | mitigate | UNKNOWN_ENUM_LABEL classification row green in full suite | closed |
| T-27-11 | Tampering | medium | mitigate | anchor rows for both enum constants; tail gate PASS | closed |
| T-27-12 | Info disclosure | medium | mitigate | `classCastFailureTextIsPinnedExactly` committed before the refactor; suite green on the refactor tree | closed |
| T-27-13 | Repudiation | low | mitigate | comment-only checks and compile per scrub commit; reviewer re-verified the diffs are comment-only | closed |
| T-27-14 | Tampering | low | mitigate | `SourcePlanningIdGuardUnitTest` seen red, plant test red | closed |
| T-27-15 | Tampering | low | mitigate | anchor row `WRITE_TYPE_MISMATCH_FAILURE_FORMAT` = 2 | closed |
| T-27-16 | Repudiation | medium | mitigate | mutation proofs E-11, E-12 | closed |
| T-27-17 | Repudiation | medium | mitigate | `ASSERTION-DIFF-CLEAN`; human check of E-13 table still open (phase-completion item) | closed |
| T-27-18 | Repudiation | medium | mitigate | `--rerun` with cleared result dir; E-13 | closed |
| T-27-19 | Tampering | high | mitigate | `git ls-remote origin custom_build` = `69cd680b` (equals E-01); no `sync/*` branch on origin | closed |
| T-27-20 | Tampering | low | mitigate | `git diff --quiet HEAD -- src/main` at Task 1 end (E-03) | closed |
| T-27-SC | Tampering | low | accept | no build/dependency files changed since the phase base | closed |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-23 | 21 | 21 | 0 | execute-phase orchestrator (L1 grep-depth short-circuit: threats_open 0, plan-time register, ASVS 1) |

**Note:** code-review WR-01 (opaque unknown-enum-label failure text) is an operability finding, not an open threat; it is tracked in `27-REVIEW-DISPOSITION.md`.

**Approval:** verified 2026-10-23
