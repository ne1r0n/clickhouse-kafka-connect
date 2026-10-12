---
status: testing
phase: 18-writer-correctness
source: [18-VERIFICATION.md]
started: 2026-10-12T18:20:00Z
updated: 2026-10-12T18:20:00Z
---

## Current Test

number: 1
name: All-empty CDC batch on client V1 and with request compression
expected: |
  put() succeeds and 0 rows land; if either errors, short-circuit the network
  insert in the writer when every record is skipped, or soften the
  FORK-DELTA/L-30 wording to "client V2, uncompressed, verified"
awaiting: user response

## Tests

### 1. All-empty CDC batch on client V1 and with request compression
expected: put() succeeds and 0 rows land; if either errors, short-circuit the network insert in the writer when every record is skipped, or soften the FORK-DELTA/L-30 wording to "client V2, uncompressed, verified"
result: [pending]

### 2. Upgrade carry-over for a batch left BEFORE_PROCESSING by the previous release (backstop truth, ledger L-30)
expected: The batch is retried with the same insert_deduplication_token; whether its earlier phantom rows remain depends on whether the first attempt landed. Confirm this is stated in Phase 20's rollout note
result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
