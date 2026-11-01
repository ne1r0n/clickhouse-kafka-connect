---
phase: 39-dependabot-absorption-behaviour-pins
fixed_at: 2026-11-01T00:00:00Z
review_path: .planning/phases/39-dependabot-absorption-behaviour-pins/39-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 39: Code Review Fix Report

**Fixed at:** 2026-11-01T00:00:00Z
**Source review:** .planning/phases/39-dependabot-absorption-behaviour-pins/39-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2
- Fixed: 2
- Skipped: 0

Verification ran in an isolated git worktree (`.claude/worktrees/rf-39-56999-*`). The worktree was fast-forwarded into `sync/v1.7.0` and then removed. `compileIntegrationTestJava` ran in that worktree, not in the main checkout. `src/main` is untouched. `.planning/config.json` and `.planning/state.json` were left uncommitted.

## Fixed Issues

### WR-01: Diagnostic log lines in two integration tests now fail the test on a non-200 status response

**Files modified:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java`
**Commit:** 51200928
**Applied fix:** Added a private `sinkTaskStatusForLog()` helper that catches `IOException` and returns `"unavailable: <message>"`. Both `stockGenMultiTaskTopicTest` and `stockGenMultiTaskSchemalessTest` now log through it. Only the edited hunks changed and the import lines are byte-identical. `./gradlew compileIntegrationTestJava` passed with no errors (warnings only, all pre-existing).

### WR-02: `ROLLOUT-v1.7.0_gg6.md` row D7 now states the opposite of the repository

**Files modified:** `docs/ROLLOUT-v1.7.0_gg6.md`
**Commit:** 12379bf1
**Applied fix:** Replaced "no test in the repository asserts the forwarding, so a later client bump could change it unnoticed" with a snapshot-preserving sentence. It says that at this release no test asserted the forwarding, and that since Phase 39 `ClientEndpointAndOptionsPinUnitTest.customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` pins it. No test or workflow reads the old text.

---

_Fixed: 2026-11-01T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
