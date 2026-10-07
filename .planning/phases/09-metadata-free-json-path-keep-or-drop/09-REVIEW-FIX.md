---
phase: 09-metadata-free-json-path-keep-or-drop
fixed_at: 2026-09-20T00:00:00Z
review_path: .planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 9: Code Review Fix Report

**Fixed at:** 2026-09-20T00:00:00Z
**Source review:** .planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (CR-01 critical, WR-01 warning; IN-01 info was explicitly out of scope per orchestrator instructions and is not counted here)
- Fixed: 2
- Skipped: 0

**Verification environment:** all edits, compiles, and test runs were performed inside an isolated git worktree (`.claude/worktrees/rf-09-91976-1789862544`, branch `gsd-reviewfix/09-91976`) created per the review-fixer's worktree isolation protocol, then fast-forwarded onto `sync/v1.6.0` and torn down. Not reproducible from the main checkout after teardown; re-run `./gradlew test --tests "*MetadataFreeJsonInsertUnitTest*"` on `sync/v1.6.0` after this merge to reproduce.

## Fixed Issues

### CR-01: `auto.evolve` bypasses the metadata-free guard and reaches `describeTable`/`ALTER TABLE` on every batch

**Files modified:**
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java`

**Commit:** `51baea62` — `fix(09): guard auto.evolve branch with shouldSkipTableMetadata (CR-01)`

**Applied fix:** Applied the reviewer's suggested fix verbatim in shape. In `ClickHouseWriter.doInsert` (around line 210), changed `if (csc.isAutoEvolve())` to `if (csc.isAutoEvolve() && !shouldSkipTableMetadata())`, and added an `else if (csc.isAutoEvolve())` branch that logs a single `LOGGER.warn(...)` explaining that schema evolution is skipped because `insertFormat` is explicitly JSON (metadata-free mode). The existing body of the `auto.evolve` branch, including its two pre-existing comment lines about the last-record-only limitation, was left byte-identical; only the condition and the new `else if` are new. No other lines in this high-risk file were touched — the diff is exactly this hunk (confirmed via `git diff`).

Added two new Docker-free test cases to `MetadataFreeJsonInsertUnitTest` pinning both halves of the guard so the positive assertion cannot pass vacuously:
- `autoEvolveIsSkippedUnderExplicitJsonMetadataFreeMode`: with `auto.evolve=true` and an explicit `insertFormat=json`, asserts `evolveTableSchema` is never invoked (via a test-only `RecordingAutoEvolveWriter` subclass that overrides `getTable`/`evolveTableSchema` to isolate the guard from table-resolution/DDL machinery, since a full `doInsert` call still needs the never-built `chc` for the actual insert and throws `NullPointerException` from `doInsertBatch` regardless of which branch fires — that NPE is expected/incidental in both tests, and the `evolveTableSchemaCalled` flag is what actually proves the guard's behavior).
- `autoEvolveStillRunsUnderNonExplicitInsertFormat`: with `auto.evolve=true` and a non-explicit insert format, asserts `evolveTableSchema` IS invoked, proving the new guard does not over-fire.

Only new test cases were added; no existing test was deleted or weakened.

**Verification:**
- Tier 1: re-read the modified section of `ClickHouseWriter.java` after editing — fix text present, surrounding code (including the two existing limitation comments) intact and byte-identical.
- Tier 2: `./gradlew compileJava compileTestJava` — BUILD SUCCESSFUL (only pre-existing deprecation warnings unrelated to the touched files).
- `./gradlew test --tests "*MetadataFreeJsonInsertUnitTest*"` — 13 tests, 13 succeeded, 0 failed, 0 skipped (includes the two new CR-01 pins and all 11 pre-existing tests in the class, none weakened).
- `./gradlew spotlessApply` was run. Because `ClickHouseWriter.java` is a designated high-risk file with an explicit "make the minimal change described above and nothing else in it" instruction, and keeping changes additive/rebasable is a repeated project constraint (D-10, D-16), the unrelated import-reordering spotless produced in that file was manually reverted, keeping only the functional CR-01 diff. `MetadataFreeJsonInsertUnitTest.java`'s spotless output was already fully attributable to the new code (correctly sorted new imports, no reordering of pre-existing ones) and was kept as-is. Every other file spotless touched was reverted via `git checkout --` and is not part of this commit or any other in this fix run.
- **Correction (orchestrator, verified post-merge on `sync/v1.6.0`):** this report originally claimed spotless drift across 46 files "including `ClickHouseWriter.java`". That is wrong. `./gradlew spotlessCheck` on the current tip fails on exactly **two** files, both untouched by this fix and both last modified by unrelated older commits: `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseCloudTest.java` (`57592cfb`) and `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java` (`0b3cab9e`). All four files this fix run modified — `ClickHouseWriter.java` included — pass `spotlessCheck`. The narrow blast radius is expected: `build.gradle.kts` enables only `importOrder()` and `removeUnusedImports()`; `googleJavaFormat()` is commented out, so Spotless does not reformat bodies in this repo. The two pre-existing violations are genuine but out of this task's scope; flagged for visibility rather than silently fixed.

### WR-01: Stale "Disabled pending..." class Javadoc left on three re-enabled live test classes

**Files modified:**
- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java`
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java`
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java`

**Commit:** `99908525` — `docs(09): refresh stale disabled-pending Javadoc on re-enabled live tests (WR-01)`

**Applied fix:** Replaced the stale "Disabled pending the JSONP-01 keep/drop decision (Phase 9, D-11): ..." class-level Javadoc paragraph in all three files with a "Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-10): ..." paragraph, following the voice and pattern already established in `MetadataFreeJsonInsertUnitTest`'s own re-enabled-state Javadoc. Doc-only — confirmed via `git diff` that no assertion, annotation, import, or method body was touched in any of the three classes; only the Javadoc paragraph text changed.

**Verification:**
- Tier 1: re-read each modified file — stale paragraph replaced, all surrounding Javadoc (class-purpose descriptions, D-16/D-06 notes) and code untouched.
- Tier 2: `./gradlew compileTestJava` — BUILD SUCCESSFUL.
- Tier 3 fallback not needed since Tier 2 (compile check) is applicable to `.java` files and passed.

## Skipped Issues

None — both in-scope findings (CR-01, WR-01) were fixed.

## Out of Scope (not attempted, per explicit orchestrator instruction)

- **IN-01** (`cleanupExtraFields`'s disjunction ordering relying on evaluation order for null-safety) — Info-tier finding, explicitly excluded from this fix run's scope by the orchestrator. Not attempted; not counted as skipped-because-failed.
- The reviewer's secondary suggestion in CR-01's Fix section (reject `auto.evolve=true` + explicit `insertFormat=json` at config/`start()` time) — explicitly excluded by the orchestrator ("the user explicitly chose NOT to do this"). `ClickHouseSinkConfig.java` was not modified.

---

_Fixed: 2026-09-20T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
