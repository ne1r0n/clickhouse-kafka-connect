---
status: complete
phase: 28-raw-identifier-names-in-discovery
source: [28-VERIFICATION.md]
started: 2026-10-23T12:13:39Z
updated: 2026-10-23T13:34:13.551Z
---

## Current Test

[testing complete]

## Tests

### 1. Sign off the named exemption list (plan 28-06 human-check)
expected: Every removed src/test line is covered by a list row; X-01..X-12 are expected-form changes under the 2026-10-21 decision with the same strictness; no other existing test member changed; S-01..S-04 change no assertion. S-04 (RESOURCE_PATH_FORMAT) was added at the gate through a relayed choice, which is not the plan's own end-of-phase confirmation.
result: pass

### 2. Confirm the two choices made on the user's behalf (plan 28-06 human-check)
expected: (a) The NAME-07 documents (FORK-DELTA, CHANGELOG, Known costs item 11, ROADMAP 999.5) landed in fix(28-05) rather than fix(28-03). (b) Column's enum-label scan calls the package-private DeclaredNullability.unescapeJsonLayer instead of carrying its own copy.
result: pass

## Summary

total: 2
passed: 2
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
