---
phase: 14
review: 14-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "LZ4 test never proves the response leg was compressed"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "Fork-count step reports a recomputed value, not the one Gradle uses"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "Concurrency group still dead-codes the pull_request branch and stays SHA-keyed"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "Test duplicates constants already exported by production code"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "Jar-origin check is a substring match on a filesystem path"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "Payload length is asserted inside the data-building loop"
open: 0
total: 6
recorded: 2026-10-10T08:30:54.000Z
---

# Phase 14: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | test-only: the LZ4 round-trip test asserts the request leg but not the response leg; no production code affected |
| WR-02 | warning | deferred | CI-only reporting: the fork-count step recomputes maxParallelForks in shell; Gradle behaviour and test results unaffected |
| IN-01 | info | deferred | CI-only and cosmetic: concurrency group keeps a dead pull_request branch and stays SHA-keyed |
| IN-02 | info | deferred | test-only: constants duplicated from production code; the independent copy keeps the test correct if production renames the key |
| IN-03 | info | deferred | test-only: jar-origin check is a substring match on a path; works for the Gradle cache layout the suite uses |
| IN-04 | info | deferred | test-only: payload length asserted inside the data-building loop; the later body-length assertion is the meaningful one |
