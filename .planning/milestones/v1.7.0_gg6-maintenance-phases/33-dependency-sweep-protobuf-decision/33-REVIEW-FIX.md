---
phase: 33-dependency-sweep-protobuf-decision
fixed_at: 2026-10-28T00:00:00Z
review_path: .planning/phases/33-dependency-sweep-protobuf-decision/33-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 33: Code Review Fix Report

**Fixed at:** 2026-10-28
**Source review:** .planning/phases/33-dependency-sweep-protobuf-decision/33-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2
- Fixed: 2
- Skipped: 0

Info findings IN-01, IN-02 and IN-03 were out of scope (`fix_scope: critical_warning`).

## Fixed Issues

### WR-01: Guard reports "no `ignore:` list" for a valid `ignore:` key that carries a trailing comment

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java`
**Commit:** f5cac9c5
**Applied fix:** Added `IGNORE_KEY_LINE`, a regex that tolerates indentation and a trailing ` # comment`, and used it in both `ignoreItems` (gradle entry) and `githubActionsIgnoreProblem`. The reviewer's suggested pattern `^\s*ignore:\s*(?:#.*)?$` was tightened to `^\s*ignore:(?:\s+#.*)?\s*$`. The suggested form would also match `ignore:#x`, where YAML reads `#x` as a value rather than a comment. The `IGNORE_KEY` constant stays, because the fixture builder still emits it. Added `fixtureIgnoreKeyWithTrailingCommentIsRecognised`, which covers the commented key in both entries: accepted for gradle, rejected for github-actions, and reported as no list for a gradle entry that lacks one. The pre-fix code would have false-passed the github-actions case.

### WR-02: Guard checks only the first `gradle` entry and the first item per name, so a later duplicate can weaken or bypass the ignore unseen

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java`
**Commit:** 8ee51d44
**Applied fix:** `gradleProtobufIgnoreProblem` now counts gradle ecosystem entries and fails with "must have exactly one gradle package-ecosystem entry" when there is more than one. The zero-entry case keeps its existing "has no gradle package-ecosystem entry" message, so the existing tests are unaffected. The guard fails loudly on a layout change instead of passing unseen. The reviewer's `findFirst()` per-name point was left alone: extra items only add ignores, which the reviewer called harmless. Added `fixtureSecondGradleEntryIsRejected`, covering a second gradle entry with and without its own ignore. No existing test was changed or removed.

## Verification

- Ran in an isolated worktree under `.claude/worktrees/`, which was fast-forwarded into `sync/v1.7.0` and removed. The commands used `--offline`, so the numbers are reproducible from the main checkout.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*DependabotProtobufIgnoreGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --offline`: 73 tests, 73 passed. The Dependabot guard class went from 11 to 13 tests.
- `./gradlew spotlessCheck --offline` fails only on files outside this change (`src/integrationTest/.../ClickHouseCloudTest.java` and `ClickHouseSinkConnectorIntegrationTest.java`, pre-existing). The edited guard file is not listed. `spotlessApply` was not run.
- Both fixes change test code only, so nothing was flagged for human verification. WR-02 is a deliberate strictness choice: a legitimate future second gradle entry now needs the guard extended.
- `.planning/config.json` and `.planning/state.json` were not staged.

---

_Fixed: 2026-10-28_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
