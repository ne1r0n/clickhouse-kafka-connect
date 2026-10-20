# Project Research Summary

**Project:** ClickHouse Kafka Connect — `custom_build` fork, v1.7.0_gg2-hardening milestone
**Domain:** Kafka Connect sink connector hardening and CI hygiene for production use
**Researched:** 2026-10-10
**Confidence:** HIGH (live-probed against ClickHouse 26.9.14.10, GitHub API verified, git-sourced)

---

## Executive Summary

This milestone hardens a production ClickHouse Kafka Connect fork across two dimensions: **data correctness** (Nullable type handling, empty-CDC skip, error messaging, log masking) and **CI/build hygiene** (GitHub Actions node24 majors, runner pin, Gradle lz4 duplicate handling, Lombok Jackson warning).

The challenge is **sequencing**: eight features interact through shared code (`ClickHouseWriter` has four changes, `Column.extractColumn` gates two, the WARN block touches five review items) and exactly-once-semantics invariants (skip placement, byte-determinism). The recommended approach is a **five-phase sequence** with early docs publication to remove a published data-loss hazard, then parallel CI hardening and test-debt work, then the correctness writer fixes (Enum before Tuple), then release. Publication mechanics require careful branch/tag ordering due to protected `custom_build` and token downgrade on forks.

**Key risks and mitigation:**

1. **Tuple null-marker stream desync (CRITICAL):** Naive fix propagates DESCRIBE-reported nullability which has extra markers; breaks currently-working single-field tuples. **Mitigate:** parse declared element nullability from the parent type string, not DESCRIBE rows; live matrix with twin tables; seen-red mutation.

2. **Stored value change for Nullable(Enum)** breaks historical consistency and flips a validation gate; four test pins become stale. **Mitigate:** flip (don't delete) tests with dated rationale; narrow the WARN; update FORK-DELTA, ledger, and rollout text.

3. **Publication mechanics:** `custom_build` is protected; relayed approvals refused; tag/branch cancellations; unpublished tail. **Mitigate:** orchestrator-inline approval gates; separate docs-only merge before code; tags after CI green.

4. **Skip layer placement:** Filtering records before `Processing` breaks offset ranges; must stay inside the writer. **Mitigate:** one private helper; validate exactly-once invariants; pin all-skipped batch.

---

## Key Findings

### Recommended Stack (from STACK.md)

**CI actions (GitHub Actions):** SHA pinned to node24 majors with commit-not-tag SHAs and trailing version comments: checkout v7.0.1, setup-java v6.0.1, gradle/actions v6.4.0 (annotated tag trap: peeled commit SHA, not tag object), upload-artifact v7.0.2. **Runner:** ubuntu-24.04 pin (nine days before 26.04 drift starts). **Permissions:** contents:read only. **Gradle/Lombok:** scoped lz4 exclusion on testRuntimeClasspath, Lombok Jackson 2 config line, printTestForks task for CI reporting.

### Expected Features (from FEATURES.md)

**Launch-critical:** F8a (early docs publication to remove TRUNCATE hazard) precedes all code. F1a Enum fix (one-line, gates F7 IN-01..05). F1b Tuple fix (gated after spike; fallback defined). F2 empty-CDC skip (hoisted guard). F3 DataException naming column. F4 null-message guard. F6 DateTime log masking (both arguments). F5 response-leg LZ4 assertion. F7 IN-01..IN-10 disposition. F8b release notes and rollout. **Defer:** RowBinary empty-CDC route (upstream hot path); Utils IOException null-message (different threat).

### Architecture (from ARCHITECTURE.md)

Data flow touches: Column.extractColumn (F1 nullability), ClickHouseWriter.doInsertBatch (route selection) → doWriteCol (F3, F1 markers) / writeRecordsJson (F2, F6), catch block (F4), doWriteDates (F6). File risk: ClickHouseWriter.java high-risk; serialize four fixes before F1b Tuple. **Exactly-once invariant:** Skip deterministic function of record+metadata; token-based dedup; offset ranges unchanged.

### Critical Pitfalls (from PITFALLS.md)

1. **Tuple desync:** DESCRIBE reports extra element markers; naive fix breaks single-field case. Prevent: parse from type string; twin-table live matrix; mutation test.
2. **Enum value change:** Pins, WARN, FORK-DELTA, rollout document defect; all become stale. Prevent: flip tests with rationale; decide WARN fate; move anchors; update ledger.
3. **Cross-version bytes:** Retry sends different bytes post-upgrade. Prevent: drain procedure in rollout; state token-based dedup.
4. **Skip at wrong layer:** Filtering before Processing breaks ranges. Prevent: hoist inside writer; all-skipped batch test; invariant validation.

---

## Implications for Roadmap

**Five-phase structure:**

- **Phase 16 (Early Publication):** Merge sync/v1.7.0 into custom_build (--no-ff); docs-only (ROLLOUT items 1/4/8, FORK-DELTA, IN-07..10). Must precede code. Approval-gated.
- **Phase 17 (CI & Build):** tests.yaml (Node24, ubuntu-24.04, SHA pins, permissions), lombok.config, build.gradle.kts (lz4 exclusion, fork-count task), F5 response-leg assertion, IN-06 test. Parallel with Phase 18.
- **Phase 18 (Writer Fixes I):** WR-03 (column name), WR-04 (null guard), T-13-19 (log masking), F2 (empty-CDC helper). Four separate commits; ground work before F1.
- **Phase 19 (Nullable Types):** Sub-phase 4a: spike + F1a Enum fix (flip pins, narrow WARN, IN-01..05). Sub-phase 4b: F1b decision (Tuple or defer to Known cost).
- **Phase 20 (Release):** VERSION v1.7.0_gg2, CHANGELOG 1.7.0_gg2, new ROLLOUT-v1.7.0_gg2.md (reviewed before gate), FORK-DELTA refresh, tag v1.7.0_gg2. Approval-gated.

**Sequencing:** 16 first/alone → 17 & 18 parallel → 18 before 19 → 19 before 20 → 20 last. Phase 16 uses unchanged workflow (proof that CI changes are clean). Phase 19 includes 30-min spike on Tuple parsing; F1b is go/no-go at spike end.

**Research flags:** F1b Tuple parsing (design decision), F5 response-leg via client-v2 (assertion design), F2 RowBinary route (skip inclusion decision), IN-07..10 timing (Phase 16 or 20?). All others use standard patterns.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| **Stack** | HIGH | GitHub API 2026-10-10; action.yml verified; Lombok warning reproduced; Gradle measured |
| **Features** | HIGH | Live ClickHouse 26.9.14.10 probes; code flow traced; all items sourced |
| **Architecture** | HIGH | sync/v1.7.0 HEAD; call paths, routing, invariants verified |
| **Pitfalls** | HIGH | Live probes, code flow, Phase 11/15 lessons in project memory |
| **Publication** | MEDIUM-HIGH | Phase 11/15 precedent; protected branch confirmed; tag timing from CI-08 |

**Gaps:** F1b parsing design (spike needed); DESCRIBE shape stability across server configs (Docker tests); response-leg assertion at scale (spike); exact-once on schema change (document risk); F2 RowBinary scope (decision in plan).

---

## Sources

STACK.md: GitHub API, git ls-remote, action.yml, local Lombok testing, Gradle dependencyInsight. FEATURES.md: Repository source, live ClickHouse 26.9.14.10, code flow analysis. ARCHITECTURE.md: Same + file locations + integration test framework. PITFALLS.md: Phase 11/15 history, project memory, live measurements, GitHub API.

---

*Research completed: 2026-10-10*
*Ready for roadmap: YES*
