---
phase: 34-upstream-tail-merge
fixed_at: 2026-10-29T00:00:00Z
review_path: .planning/phases/34-upstream-tail-merge/34-REVIEW.md
iteration: 1
findings_in_scope: 1
fixed: 1
skipped: 0
status: all_fixed
---

# Phase 34: Code Review Fix Report

**Fixed at:** 2026-10-29
**Source review:** .planning/phases/34-upstream-tail-merge/34-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 1
- Fixed: 1
- Skipped: 0

Scope was Critical + Warning only. Info findings IN-01, IN-02 and IN-03 were left open by instruction.

Verification ran in the main checkout (no worktree). `testUnit` for the two classes: 23 tests, 23 passed (4 + 19, unchanged). Both files are Spotless-clean: `spotlessApply` produced no diff on them. The other 48 or so pre-existing Spotless violations in the repo were left untouched.

## Fixed Issues

### WR-01: Seam-built tasks that call `put()` leak topic MBeans into the platform MBean server

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java`, `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java`
**Commit:** ad38ab27
**Applied fix:** Wrapped the `put()` and assertions of `dbWriterSeamConstructorWiresTheSuppliedWriterIntoProcessingAndRoutesInsertsToIt`, and of the strict and tolerant tasks in `writerDataExceptionFailsTheTaskNonRetriablyThroughProxySinkTaskWithCodesConfigured`, in `try { ... } finally { task.stop(); }`. `stop()` unregisters the task and topic MBeans. No assertion was changed or removed.

---

_Fixed: 2026-10-29_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
