---
phase: 09-metadata-free-json-path-keep-or-drop
verified: 2026-09-20T00:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - .planning/MERGE-CHECKLIST.md
  - .planning/REQUIREMENTS.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-01-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-01-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-02-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-02-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-03-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-03-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW-FIX.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-UAT.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java
covered_digest: "v1:sha256:8cb88eb7bb1d6c7f8aafd7f0d1f06a4072805fbb101ad36503353acbd51f53ac"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 4/4
  gaps_closed:
    - "CR-01: ClickHouseWriter.doInsert's auto.evolve branch reached describeTable/ALTER TABLE unconditionally under the metadata-free bypass, defeating the KEEP decision's operator-fact-3 rationale — now guarded by shouldSkipTableMetadata() (commit 51baea62), pinned by two new Docker-free tests."
    - "WR-01: stale 'Disabled pending the JSONP-01 keep/drop decision' Javadoc left on three re-enabled live test classes — replaced with 'Re-enabled per the JSONP-01 KEEP decision' wording (commit 99908525)."
  gaps_remaining: []
  regressions: []
gaps: []
---

# Phase 9: Metadata-free JSON Path — Keep or Drop Verification Report

**Phase Goal:** Settle the one genuinely open design question in this milestone — whether the fork's metadata-free `insertFormat=json` bypass (and with it R-01's silent data loss) is kept or retired in favour of upstream's `writeRecordsJson` — at a blocking-human checkpoint on evidence read from the merged code, then make the code and the tests say the same thing.
**Verified:** 2026-09-20
**Status:** passed
**Re-verification:** Yes — after gap closure (prior report was `human_needed`; the one outstanding human-decision item, CR-01's disposition, has since been resolved)

## Re-verification Summary

The prior `09-VERIFICATION.md` (status `human_needed`, score 4/4) found all four ROADMAP truths verified but escalated one item to a human checkpoint: CR-01, a code-review Critical finding that `ClickHouseWriter.doInsert`'s `auto.evolve` branch called `evolveTableSchema` with no `shouldSkipTableMetadata()` guard, which could drive `ALTER TABLE`/`describeTable` traffic (or a hard failure) on every batch under `auto.evolve=true` + explicit `insertFormat=json` — undermining the KEEP decision's own operator-fact-3 rationale.

`09-UAT.md` (`status: complete`, 1/1 passed) records the resolved decision: "Fix now — accepted as already applied." Two commits landed the fix:
- `51baea62` — guards the branch with `if (csc.isAutoEvolve() && !shouldSkipTableMetadata())`, adds a warning log on the skipped-evolution path, and pins both halves (guard fires under explicit JSON; does not over-fire otherwise) with two new tests in `MetadataFreeJsonInsertUnitTest`.
- `99908525` — replaces the stale "Disabled pending..." Javadoc on the three re-enabled live test classes (WR-01) with accurate "Re-enabled per the JSONP-01 KEEP decision" wording.

Because these commits touched a file (`ClickHouseWriter.java`) covered by the prior digest, this report re-verifies from scratch against the current tree rather than trusting the resolution narrative.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|---|---|---|
| 1 | A recorded keep-or-drop decision with rationale exists, made at a blocking-human checkpoint on evidence from the merged code, before the dependent implementation work | ✓ VERIFIED | Unchanged from prior verification. `09-01-PLAN.md` Task 1 is `type="checkpoint:decision"`; `09-01-SUMMARY.md` records "Confirm KEEP (expected)"; `09-CONTEXT.md` D-01/D-02 and `MERGE-CHECKLIST.md` Recorded Fact 14 confirm the checkpoint and its rationale on the current tree. |
| 2 | The JSON-path tests reflect the KEEP decision: the bypass works and R-01's labelled loss test still passes | ✓ VERIFIED | Regression run supplied by orchestrator: `./gradlew test` BUILD SUCCESSFUL, 645 tests / 0 failures / 0 errors / 18 skipped (2 more than the prior verification's 643 — the two new CR-01 guard pins). Independently confirmed `MetadataFreeJsonInsertUnitTest`'s own result XML: `tests="13" skipped="0" failures="0" errors="0"` (11 pre-existing + 2 new). R-01's pin test remains present in `ExplicitJsonInsertPathLiveTest` (unchanged file content besides Javadoc). |
| 3 | `jsonInsertColumns` works again as a production consumer, redesigned per the corrected premise (D-06/D-07/D-08: server-side branch on the surviving 5-arg `client.insert` overload, not a client-side field filter) | ✓ VERIFIED | Unchanged code path; re-read `ClickHouseWriter.java` — the `insertColumns` ternary selecting the 5-arg vs 4-arg `client.insert` overload is intact and untouched by the CR-01/WR-01 fix commits (`git show 51baea62 --stat` touches only the `auto.evolve` guard hunk plus the new test file). `REQUIREMENTS.md` JSONP-03 correction blockquote confirmed present. |
| 4 | Reading the resolved diff (not merely a green test) shows no fork-only JSON-path code left standing against a `Table` shape upstream no longer produces | ✓ VERIFIED | Unchanged; `grep -rn "new Table(" src/main/java` still returns exactly 3 five-arg call sites. Not touched by the fix commits. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### CR-01 Fix Verification (the resolved human checkpoint)

Directly re-read `ClickHouseWriter.java:206-224` on the current tree:

```java
if (csc.isAutoEvolve() && !shouldSkipTableMetadata()) {
    // Check the last record's schema for new fields.
    // Limitation: if a batch contains multiple schema versions, only the last one is checked.
    // A full scan across all records can be implemented later if needed.
    Record last = records.get(records.size() - 1);
    Map<String, Schema> lastFields = new LinkedHashMap<>();
    if (last.getFields() != null) {
        for (Field f : last.getFields()) {
            lastFields.put(f.name(), f.schema());
        }
    }
    table = evolveTableSchema(table, lastFields);
} else if (csc.isAutoEvolve()) {
    LOGGER.warn(
            "auto.evolve is enabled but insertFormat is explicitly set to JSON (metadata-free mode); "
                    + "schema evolution is skipped because it would require table metadata the bypass exists to avoid.");
}
```

Confirmed: the `csc.isAutoEvolve()` branch is now guarded by `!shouldSkipTableMetadata()`, matching the reviewer's suggested fix in `09-REVIEW.md` CR-01 verbatim in shape. `git log --oneline -- ClickHouseWriter.java` shows this is the tip commit (`51baea62`) on top of the two Phase 9 feature commits (`9d8f3d95`, `3f9ecd82`). The two new pin tests (`autoEvolveIsSkippedUnderExplicitJsonMetadataFreeMode`, `autoEvolveStillRunsUnderNonExplicitInsertFormat`) are present in `MetadataFreeJsonInsertUnitTest.java` at lines 207 and 230, and both are included in the class's 13/13-passing result XML. This is a genuine fix, not a narrative claim: the guard predicate reused is the same `shouldSkipTableMetadata()` used at the other four sites (D-05's single-predicate design is preserved — CR-01 is now a fifth call site of the same predicate, not a bespoke check).

WR-01 (the accompanying doc-only Warning) is also confirmed fixed: `grep -n "Disabled pending\|Re-enabled per"` across the three live test classes shows all three now carry the "Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-10)" wording; no "Disabled pending" text remains.

**No new human decision is required.** The single outstanding human-verification item from the prior report has been resolved with a genuine code fix, confirmed independently against the current tree (not taken on the UAT/REVIEW-FIX narrative alone).

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `src/main/java/.../sink/db/ClickHouseWriter.java` | `shouldSkipTableMetadata()` predicate + 4 original guard sites + synthetic `Table` + `doInsertJsonV2` overload branch + CR-01's 5th guard site on the `auto.evolve` branch | ✓ VERIFIED | All 4 original guard sites unchanged (`:125` `start`, `:147` `updateMapping`, `:1400`-range `cleanupExtraFields`, `getTable`). New 5th guard site confirmed at the `auto.evolve` branch in `doInsert` (line ~210), reusing the same predicate. |
| `src/test/java/.../db/MetadataFreeJsonInsertUnitTest.java` | Docker-free canary, un-parked, 2 new CR-01 pins added on top of the original 2 pins | ✓ VERIFIED | 13 tests total (was 11); result XML: 0 failures/errors/skipped. New method names `autoEvolveIsSkippedUnderExplicitJsonMetadataFreeMode` and `autoEvolveStillRunsUnderNonExplicitInsertFormat` confirmed present and passing. |
| `src/test/java/.../ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java` | `@Disabled` removed, contract unchanged, Javadoc accurate (WR-01 fixed) | ✓ VERIFIED | No `@Disabled` remains; stale "Disabled pending..." Javadoc replaced with "Re-enabled per the JSONP-01 KEEP decision" in all three. No assertion/annotation/import/method-body changes (doc-only diff, confirmed via commit `99908525`'s stat). |
| `src/test/java/.../db/mapping/TableTest.java` | `extractNullables()`'s all-columns assertion `3` → `2` (D-14) | ✓ VERIFIED | Unchanged since prior verification; not touched by the fix commits. |
| `.planning/REQUIREMENTS.md` | JSONP-03 dated correction blockquote; JSONP-01..04 closed | ✓ VERIFIED | Unchanged; all four `[x]` and `Complete` in the traceability table. |
| `.planning/MERGE-CHECKLIST.md` | Recorded Fact 14 (KEEP disposition) | ✓ VERIFIED | Unchanged since prior verification. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ClickHouseSinkConfig.isJsonInsertFormatExplicit()` | `ClickHouseWriter.shouldSkipTableMetadata()` | single predicate, no inlined duplicates | ✓ WIRED | Still exactly one call site of `isJsonInsertFormatExplicit()`, inside the predicate; predicate now called at 5 sites (4 original + CR-01's `auto.evolve` guard), all reusing the same method — no duplicated inline check introduced by the fix. |
| CR-01 fix | `MetadataFreeJsonInsertUnitTest`'s two new pins | `RecordingAutoEvolveWriter` subclass overriding `getTable`/`evolveTableSchema` | ✓ WIRED | Both new tests present, both passing, both exercise the guard's positive and negative halves per `09-REVIEW-FIX.md`'s description — independently confirmed via the class's 13/13 result XML rather than taken on the fix report's word. |
| Plan 09-01's overload branch | `JsonInsertColumnsLiveTest` COLS-02/COLS-06 | unchanged | ✓ WIRED | Not touched by the fix commits; carried forward from prior verification. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Full-suite regression after gap closure (evidence supplied by orchestrator, not re-run here) | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` | BUILD SUCCESSFUL in 3m 13s, exit 0; aggregated `build/test-results/test/*.xml`: tests=645, failures=0, errors=0, skipped=18 | ✓ PASS |
| `MetadataFreeJsonInsertUnitTest` individually green, count increased by exactly 2 (the CR-01 pins) | `grep -o 'tests="..." skipped="..." failures="..." errors="..."' build/test-results/test/TEST-*MetadataFreeJsonInsertUnitTest*.xml` | `tests="13" skipped="0" failures="0" errors="0"` | ✓ PASS |
| CR-01 guard present in source, matching reviewer's suggested fix in shape | Direct read of `ClickHouseWriter.java` doInsert | `if (csc.isAutoEvolve() && !shouldSkipTableMetadata())` + `else if` warn branch present | ✓ PASS |
| WR-01 stale Javadoc replaced in all three live test classes | `grep -n "Disabled pending\|Re-enabled per"` across the three files | All three show "Re-enabled per the JSONP-01 KEEP decision"; zero "Disabled pending" hits | ✓ PASS |
| No debt markers in the 6 phase-touched files (including the 2 fix-commit-modified files) | `grep -n "TBD\|FIXME\|XXX"` across all 6 covered source/test files | no matches | ✓ PASS |
| `git status --porcelain -- src/` clean | direct check | clean (only unrelated `.planning/state.json`, `.planning/milestone.lock`) | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| JSONP-01 | 09-01, 09-03 | Keep-or-drop decision at blocking-human checkpoint, on evidence, recorded with rationale | ✓ SATISFIED | Checkpoint task + developer answer + `MERGE-CHECKLIST.md` Fact 14, all confirmed above; unaffected by the CR-01/WR-01 fix. |
| JSONP-02 | 09-01, 09-02 | Tests reflect the decision; bypass works; R-01 pin still passes | ✓ SATISFIED | Live suite green (645/0/0/18); R-01 pin unchanged and passing; CR-01's fifth gap in "the bypass works" claim is now closed by the guard fix, strengthening rather than weakening this requirement's evidence. |
| JSONP-03 | 09-01, 09-02, 09-03 | `jsonInsertColumns` works again; premise correction recorded | ✓ SATISFIED | Overload branch confirmed unchanged and correct; correction blockquote confirmed in `REQUIREMENTS.md`. |
| JSONP-04 | 09-03 | No fork-only JSON-path code against old `Table` shape; verified by diff read | ✓ SATISFIED | Direct 3-site/5-arg census confirmed unchanged. |

No orphaned requirements: `REQUIREMENTS.md`'s Phase 9 traceability rows (JSONP-01..04) match exactly the requirement IDs declared across the three plans' frontmatter.

### Anti-Patterns Found

None outstanding. Both findings carried in the prior verification's Anti-Patterns table are resolved:

| File | Prior Finding | Resolution | Status |
|---|---|---|---|
| `ClickHouseWriter.java:210` (approx.) | CR-01: `csc.isAutoEvolve()` branch unguarded by `shouldSkipTableMetadata()` | Fixed in commit `51baea62`: guard added, warning log added, 2 new pin tests added | ✓ RESOLVED |
| `ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java` | WR-01: stale "Disabled pending..." class Javadoc | Fixed in commit `99908525`: Javadoc replaced with accurate "Re-enabled per..." wording, doc-only | ✓ RESOLVED |

IN-01 (`cleanupExtraFields`'s evaluation-order dependency, Info-tier) remains explicitly out of scope per the orchestrator's fix-run instructions (`09-REVIEW-FIX.md` "Out of Scope") and was never a must-have or a blocker; it is not re-raised here.

## Deferred Items

None.

## Human Verification Required

None. The single item from the prior verification (CR-01's disposition) was a decision request, not an open-ended UAT check — it has been answered ("fix now") and the fix independently re-verified against the current tree in this report. No new human-verification-worthy findings were surfaced during re-verification.

## Gaps Summary

No gaps. All four ROADMAP success criteria remain independently verified true against the current tree. The one item that kept the prior verification at `human_needed` — CR-01's disposition — has been resolved with a genuine, independently-confirmed code fix (not a narrative claim): the `auto.evolve` branch in `ClickHouseWriter.doInsert` is now guarded by the same `shouldSkipTableMetadata()` predicate used at the phase's other four guard sites, pinned by two new Docker-free tests, and the accompanying doc-only Warning (WR-01) is also resolved. The regression suite is green at 645/0/0/18 (two more tests than the prior verification's 643, accounting for exactly the two new CR-01 pins). Status is `passed`.

---

_Verified: 2026-09-20_
_Verifier: Claude (gsd-verifier)_
