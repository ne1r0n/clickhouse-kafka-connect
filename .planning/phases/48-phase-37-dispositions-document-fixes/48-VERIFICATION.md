---
phase: 48-phase-37-dispositions-document-fixes
verified: 2026-11-06T13:40:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-01-PLAN.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-01-SUMMARY.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-02-PLAN.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-02-SUMMARY.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-03-PLAN.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-03-SUMMARY.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-04-PLAN.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-04-SUMMARY.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-05-PLAN.md"
  - ".planning/phases/48-phase-37-dispositions-document-fixes/48-05-SUMMARY.md"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
  - "docs/ROLLOUT-v1.7.0_gg6.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java"
covered_digest: "v4:sha256:8fcc2cf441a98a5eae6c4c09df12982f8564b70a7bc694fa9520d7fe7be4f0b5"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 48: Phase 37 Dispositions & Document Fixes Verification Report

**Phase Goal:** The release phase starts with no carried review row left to the user by default. Each `src/main` advisory row from Phases 26-28, the null assertion, the 28 IN-05(b) items, the broad allow-list glob rows and the review Infos ends fixed (tests first, one commit each) or closed with a written reason. The published documents stop pointing at planning identifiers and at paths that moved when gg7 was archived.
**Verified:** 2026-11-06 (HEAD `fb78675e`, branch `sync/v1.7.0`)
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | The four `src/main` rows and the null assertion are each fixed in their own commit with a test, or closed with a written reason in the new ledger (P37-01) | VERIFIED | `28-IN-01` fixed in `c4ec0cbf`. `git diff 92570854 HEAD -- src/main` is exactly 3 deleted lines of `DeclaredNullability.java` (the Javadoc line, the `ESCAPED_NEWLINE` constant and a blank line), with no added file. `grep ESCAPED_NEWLINE` finds no reader outside `src/test`; the only other hit is the `COLS-13` zero-count anchor row, which stays true. `28-IN-05(b)(i)` fixed in `4de705a6`: `DescribeUnreadableEnumNullBindingUnitTest` (4 rows, both clients) was added and `DescribeUnreadableEnumMockServerTest` is byte-identical to the base. `26-IN-02`, `27-WR-01` and `27-IN-01` are closed in the ledger with the user's decisions U-1, U-2 and U-3. I checked their facts: `shouldSkipDescribeRow` has no `src/main` caller (lines 508 and 516 of `ClickHouseHelperClient`); `Column.convertEnumValues` throws at line 630; `passesThroughUnchanged` is at line 710; the anchor row `shouldSkipDescribeRow 3 HLP-02` exists. Pointers `f13d8378`, `0c8be1da` and `876a9bd1` are cited, and `f13d8378` resolves as an ancestor commit. |
| 2 | Each 28 IN-05(b) item and each review Info has a fix with a commit pointer or a named disposition with reason; no row is "owner: user" alone (P37-02, P37-04) | VERIFIED | `python3 -I 48-ledger-check.py check` prints `rows=57 required=57 problems=0`, and `counts` prints `fixed=10 closed=47 pointers=14`. The two 28 IN-05(b) items are split into (i) fixed and (ii) closed with a stated reason and trigger. `grep -i "owner: user\|(the user)"` finds 0 hits. Every one of the 14 pointers resolves (`git cat-file -t` is `commit`, an ancestor of HEAD), and each commit touches the file its row claims (all test-only or docs-only, except `c4ec0cbf`). The four extra pins (40 IN-03, 42 IN-03(b), 42 IN-04, 47 IN-01) were approved as U-13 and each has its own commit. They add 75, 26, 34 and 12 lines to existing files with 0 deleted lines (`git diff --numstat`). `git ls-files '*48-DISPOSITION-LEDGER.md'` prints exactly one path. |
| 3 | Each broad allow-list glob row (UF-37-3) is narrowed or justified, and the dry run reads `0 unallowed, 0 stale rows` (P37-03) | VERIFIED (under the user's reading U-12, see Note 1) | `AllowListWildcardRuleUnitTest` (`07e65662`, hardened by `72928578`) reads every shipped list and refuses a wildcard row with fewer than two real literal segments. It refuses `outer/com/*`, `outer/META-INF/*` and `a//*`. The 49 shipped wildcard rows pass, so none is broad. The test ran green here with Docker unreachable. `.github/`, `.planning/scripts/` and the gg6 and gg7 lists are unedited; both lists are `cmp`-identical to their tags. I re-ran the compare script myself on the archived zips from E-15, whose SHA-256 values match: the published gg6 against gg7 pair with the gg7 list gives `RESULT: PASS` (`totals ... changed 119 same 7393`), and the tip against gg7 with the 14-exact-row draft (no wildcard rows) gives `RESULT: PASS` (`changed 11 same 7501`, `only-b 3`). |
| 4 | The gg6 note names no planning identifier; every FORK-DELTA evidence path resolves on the tip; the test path carries its package; item 20 names its ledger by a surviving path (DOC-01, DOC-02) | VERIFIED | `48-id-scan.py HEAD docs/ROLLOUT-v1.7.0_gg6.md` prints `ids: hits=0`, and the tag copy also reads 0 (the scan was 3 hits on the base per E-02). `48-path-check.py HEAD docs/FORK-DELTA.md` prints `candidates=98 resolved=70 abbreviated=20 ... by_design=6 unresolved=0`. The `KeyToValueTest` row keeps the removed path and names `src/test/java/com/clickhouse/kafka/connect/transforms/KeyToValueTest.java` beside it. Item 20 names `48-DISPOSITION-LEDGER.md` plus the lookup `git ls-files '*48-DISPOSITION-LEDGER.md'`. The gg6 diff against its tag copy is the notice and rows C2 and D7 only. Row C2 points at FORK-DELTA items 17 and 24, which exist and match. D7 says `since v1.7.0_gg7`, and commit `59c5975e` is an ancestor of that tag. |

**Score:** 4/4 truths verified (0 present-but-behavior-unverified)

### Phase-level invariants checked

| Check | Result |
|-------|--------|
| `src/main` diff since base `92570854` | Only `DeclaredNullability.java`, 0 added, 3 deleted; the hot files (`Processing`, `Utils`, `ClickHouseWriter`, `Column`, `ClickHouseHelperClient`) are untouched |
| Existing assertions weakened | None. The only deleted test lines are the 8 `DeclaredNullability.ESCAPED_NEWLINE` references that switch to the moved constant and the one Javadoc sentence replaced in `DeclaredNullabilityFixtures` (the constant text is unchanged). The 4 extended files lost 0 lines. |
| `build.gradle.kts`, `.github`, `VERSION`, `CHANGELOG.md`, `.planning/scripts` | `git diff --quiet` against the base is clean |
| Anchors mirror | `48-evidence-check.py anchors ... --rows 218` prints `rows=218 bad=0` |
| Tail gate on an explicit SHA (I ran it, ~25 min) | `invariants: provenance_paths=11 anchor_commits=173 anchor_rows=218 marker_commits=191 anchor_exempt=0 result=PASS`, exit 0 |
| New tests Docker-free | `./gradlew testUnit` with `DOCKER_HOST=unix:///nonexistent.sock` on the 7 touched or new classes: 168 tests, 168 succeeded, 0 failed. This includes both new classes and `DeclaredNullabilityTest`. |
| Merges since base, pushes | 0 merges; nothing pushed by this verification |
| Working tree | Only `.planning/config.json` and `.planning/state.json` are modified, and they predate this run |

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| P37-01 | SATISFIED | Truth 1 |
| P37-02 | SATISFIED | Truth 2 |
| P37-03 | SATISFIED | Truth 3 (user reading U-12) |
| P37-04 | SATISFIED | Truth 2 |
| DOC-01 | SATISFIED | Truth 4 |
| DOC-02 | SATISFIED | Truth 4 |

No orphaned requirements: all six IDs are claimed by the phase's plans. The REQUIREMENTS.md checkboxes and traceability rows are still `Pending`. Ledger hand-off item 8 assigns that edit to the orchestrator, so it is not a gap.

### Anti-Patterns Found

None blocking. No TBD, FIXME or XXX marker was added in the phase diff, and the new tests use no Mockito, `ClickHouseBase` or Testcontainers.

### Notes and Advisories (non-blocking)

1. **Criterion 3 is satisfied under the user's own reading (U-12/D-1), not the literal one.** The tip against the gg7 list fails by design (14 unallowed), because the gg8 list does not exist until Phase 49. The two dry runs the user accepted both read 0 unallowed and 0 stale, and I reproduced them. E-14 is a deliberate recorded fail, superseded by E-15 (14 exact rows, not 12). Phase 49 must write 14 exact rows.
2. **Full suite not re-run after the 5 review-fix cases.** The derived total is 2928, measured at E-17 as 2923. `testUnit` is measured at 2325. The 5 added cases are Docker-free and green in my run, so I accept this as recorded. Phase 49's raw-numbers refresh should use 2325 for `testUnit` and mark 2928 as derived.
3. **Ledger wording is slightly stale after the WR-02 fix.** Rows `45-IN-04` and `46-WR-05` and hand-off items 3 and 4 say "two ERROR lines". FORK-DELTA item 14 now says the first line is logged for any cut, and the second only when the cut lands in the value write. Phase 49's CHANGELOG and rollout sentences should follow the item 14 text, not the ledger paraphrase.
4. **Open review Infos recorded by decision.** 48-REVIEW `IN-02` (a duplicated MBean test) stays open because the project rule forbids removing a test. `IN-03` (the `.planning/phases/45..47` evidence paths in FORK-DELTA move at the milestone close) is ledger hand-off item 6, after which `48-path-check.py` must be re-run. Item 20 still names findings by review-row number, in FORK-DELTA's existing style. DOC-01 covers only the gg6 note, which is clean (the gg1 note still reads 26 scan hits, which is out of scope).
5. The ledger says `28-IN-01` is fixed while declining the finding's second ask, reflowing the Javadoc. It records the reason (D-3: production diff limited to the move), so this is a written disposition and not a silent drop.
6. The published gg6 and gg1 note edits and the FORK-DELTA changes go into Phase 49's independent review (U-6, ledger hand-off item 2).

### Human Verification Required

None.

### Gaps Summary

No gaps. Every success criterion holds against the code, git and the documents. The scripts, tests, compare dry runs, anchors and tail gate were re-run here and agree with the recorded evidence.

---

_Verified: 2026-11-06_
_Verifier: Claude (gsd-verifier)_
