---
phase: 20-release-publication
plan: "01"
subsystem: testing
tags: [clickhouse, deduplication, insert_deduplication_token, exactly-once, junit5, testcontainers]

requires:
  - phase: 18-writer-correctness
    provides: "empty-CDC skip (ledger L-30) whose gg1 phantom rows the retry cases simulate"
provides:
  - "DedupTokenRetryLiveTest: committed live pins of the dedup-token retry outcome on ClickHouse latest x client V2 (D-04, D-05, D-13, D-14)"
  - "20-COMMAND-EVIDENCE.md entries E-01..E-04 (server facts, tracer outcome, outcome table, seen-red run) for plan 20-03 to quote"
affects: [20-03 rollout note dedup item, 20-02 changelog, 20-05 suite gate]

actuals:
  tokens: 8950
  tasks: 3
  commits: 5

plan_head_before: 2fa6cbb4f20e5f3883aaa0f2d83b4349a405b36e
plan_head_after: dc51e3f14a5433a686275af563a3c758ccb7df62

tech-stack:
  added: []
  patterns:
    - "Tokens in live tests are built through QueryIdentifier.getDeduplicationToken(), never a copied format string"
    - "Grouped typed read-back (id, name, count) makes a row stored twice visible"
    - "Two-mutation seen-red: one edit to the shared token method, one isolating the connector"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md
  modified: []

key-decisions:
  - "Pinned the measured outcome: a retry under the same token is dropped on every deduplicating engine even when its bytes changed; the research premise held on latest 26.9.18.11, so no checkpoint was needed"
  - "exactlyOnce=false re-delivery of a landed range on ReplicatedMergeTree is dropped by the token as well (D-14); on a plain MergeTree it lands again"
  - "The two bypassRowBinary connector cases read back id, amount, name, _version, is_deleted and leave ts out, as EmptyCdcSkipRouteLiveTest does for that route"
  - "Seen-red was run twice (QueryIdentifier edit as planned, plus an isolating Processing edit) because the planned edit also changes the test's own token builder"

patterns-established:
  - "Evidence entries that back not-yet-written doc text carry the verdict pending (20-03)"

requirements-completed: [ROLL-02]

coverage:
  - id: D1
    description: "Server version and the three deduplication window settings are logged by a committed test, replicated window asserted above zero"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "DedupTokenRetryLiveTest#serverVersionAndWindowsAreLogged (D-04)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Same-token identical and changed-bytes retries are dropped, a different token lands, on ReplicatedMergeTree and on MergeTree with a window; a plain MergeTree stores every insert"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "DedupTokenRetryLiveTest#sameTokenAndDifferentTokenInserts (three D-13 invocations)"
        status: pass
    human_judgment: false
  - id: D3
    description: "exactlyOnce=true retry of a BEFORE_PROCESSING range: dropped and earlier rows (phantom rows included) kept when the earlier attempt landed, only the real rows stored when it did not, state ends AFTER_PROCESSING"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "DedupTokenRetryLiveTest#exactlyOnceRetryOfLandedRangeKeepsEarlierRows and #exactlyOnceRetryOfUnlandedRangeStoresOnlyRealRows"
        status: pass
    human_judgment: false
  - id: D4
    description: "exactlyOnce=false re-delivery: dropped on ReplicatedMergeTree whether identical or changed, stored again on a plain MergeTree"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "DedupTokenRetryLiveTest#redeliveredRangeOnReplicatedTableIsDroppedWithoutExactlyOnce and #redeliveredRangeOnPlainTableIsStoredTwice (D-14)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The connector-path pins fail without the connector's token and src/main is unchanged since P20_BASE"
    requirement: ROLL-02
    verification:
      - kind: other
        ref: "20-COMMAND-EVIDENCE.md E-04 plus the plan Task 3 automated verify (DEDUP-SEEN-RED-RECORDED)"
        status: pass
    human_judgment: false

duration: 47min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 01: Dedup-token retry pins and command evidence Summary

**`DedupTokenRetryLiveTest` pins on ClickHouse latest (26.9.18.11, client V2) that a retried range under the connector's `<topic>-<partition>-<min>-<max>` token is dropped even when its bytes changed, through the server directly and through the real task in both exactly-once modes, with E-01..E-04 recorded for the rollout note.**

## Performance
- **Duration:** about 47 min (start approximated at 12:08 UTC; the start timestamp was not captured at plan start)
- **Started:** 2026-10-14T12:08:00Z (approximate)
- **Completed:** 2026-10-14T12:55:00Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 2 created, 0 modified, nothing under `src/main`, `build.gradle.kts`, `gradle/` or `.github/`

## Accomplishments
- Server facts (E-01): `SELECT version()` = `26.9.18.11`; `replicated_deduplication_window=10000`, `replicated_deduplication_window_seconds=3600`, `non_replicated_deduplication_window=0`. Research measured `26.9.17.8` with the same values.
- Outcome table (E-03), all measured, none assumed:

| Case | Rows after each step | Outcome |
|---|---|---|
| server REPLICATED (A/T1, A/T1, changed/T1, A/T2) | 2, 2, 2, 4 | same token dropped, identical or changed; other token lands |
| server NON_REPLICATED_WINDOW (window 100) | 2, 2, 2, 4 | same |
| server PLAIN (window 0) | 2, 4, 5, 7 | every insert lands |
| exactlyOnce=true, earlier attempt landed (2 real + 2 phantom rows) | 4, then 5 | retry dropped, old rows kept, state AFTER_PROCESSING 1..4, next range adds one real row |
| exactlyOnce=true, earlier attempt never landed | 2, then 3 | only the real rows, no phantom row |
| exactlyOnce=false, ReplicatedMergeTree (orig, changed, orig) | 3, 3, 3 | re-delivery dropped |
| exactlyOnce=false, plain MergeTree | 3, 6, 9 | re-delivery stored again |

- Seen-red (E-04): with `QueryIdentifier.getDeduplicationToken()` returning null, 4 cases failed (the REPLICATED and NON_REPLICATED_WINDOW server-matrix invocations, `D-13` landed range, `D-14` replicated) and 4 passed. With only the connector's `Processing.doInsert` made token-less, exactly the two connector-path cases failed (`D-13` landed range, `D-14` replicated) and the server matrix stayed green. Both edits were reverted before any commit; the restored class is 8/8 green and `git log P20_BASE..HEAD -- src/main` is empty.
- The research premise (D-13) holds, so the plan never stopped at the blocking-human decision.

## Task Commits
1. **Task 1 (tracer): re-delivered range on ReplicatedMergeTree, server facts, evidence file** - `e1e21e12` (test), `7f3414ef` (docs, E-01/E-02)
2. **Task 2: dedup matrix, exactlyOnce upgrade shapes, plain-table re-delivery** - `7ae6af45` (test), `f97ac75b` (docs, E-03)
3. **Task 3: seen-red and restore** - `dc51e3f1` (docs, E-04)

**Plan metadata:** recorded in the docs commit that carries this summary.

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java` - 8 testcases (1 `D-04`, 5 `D-13`, 2 `D-14`), nested `Engine` enum (REPLICATED, NON_REPLICATED_WINDOW, PLAIN), log lines `DEDUP_TOKEN_RETRY server version=`, `settings`, `outcome case= step= rows=`.
- `.planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md` - entries E-01..E-04; E-01..E-03 carry `pending (20-03)` until the rollout text exists, E-04 is `n/a (gate check)`. Plans 20-02 onwards append E-10 and later.

## Decisions Made
- Pinned only observed outcomes. The server matrix was written to the research table and passed on the first run; no assertion says a changed-bytes retry writes rows twice.
- The `exactlyOnce=false` plain-table case reuses the three-step re-delivery flow (original, changed, original) instead of the plan's "identical records twice": it is a superset, the original rows read back with n=2 and the changed rows with n=1.
- Kept the connector-path cases on the plan's `bypassRowBinary` route to match the L-30 phantom-row shape.

## Deviations from Plan

**1. [Rule 1 - Bug in plan assumption] Phantom-row amount read back as `0`, not `0.0000`**
- **Found during:** Task 2 (first run of the landed-range case)
- **Issue:** the plan's A3/L-30 note gave the phantom row as amount 0; my first expected value used the padded `0.0000`, but `toString(Decimal(18,4))` of zero reads `0`.
- **Fix:** expected value corrected to `0` (the observed value, matching L-30).
- **Files modified:** `DedupTokenRetryLiveTest.java`
- **Verification:** class green 8/8
- **Committed in:** `7ae6af45`

**2. [Rule 1 - Plan assumption contradicted by the server route] `ts` left out of the read-back on the two exactlyOnce connector cases**
- **Found during:** Task 2 (first run of the never-landed case)
- **Issue:** the plan asked for the `READ_BACK_WITH_TS` shape. On the `bypassRowBinary` route the task stored `ts` as `9999-12-31 23:59:59.000` instead of the source instant `2024-01-01 00:00:00.123`. This is existing behaviour of the metadata-free JSON path; `EmptyCdcSkipRouteLiveTest` already leaves `ts` out of its read-back for that route. It is out of scope for a dedup test and was not changed.
- **Fix:** read-back compares `id`, `amount`, `name`, `_version`, `is_deleted`; the simulated earlier attempt leaves `ts` at its column default. Recorded in E-03.
- **Files modified:** `DedupTokenRetryLiveTest.java`, `20-COMMAND-EVIDENCE.md`
- **Committed in:** `7ae6af45`, `f97ac75b`

**3. [Rule 1 - Plan expectation corrected] Seen-red mutation also turned the server matrix red**
- **Found during:** Task 3
- **Issue:** the plan expected the server matrix to stay green under the `QueryIdentifier` edit because "its tokens are set explicitly". The matrix builds its tokens through that same method (as the plan also requires), so on the deduplicating engines it failed too: 4 red instead of 2.
- **Fix:** kept the planned run A and recorded the real failing set in E-04 (all `D-13`/`D-14`, so the plan's verify holds); added run B, an isolating edit of `Processing.doInsert` only, which fails exactly the two connector-path cases. Both edits reverted, nothing committed.
- **Files modified:** `20-COMMAND-EVIDENCE.md` only (the mutations were never committed)
- **Committed in:** `dc51e3f1`

**Total deviations:** 3 auto-fixed (all Rule 1, plan assumptions corrected by observation).
**Impact on plan:** none on scope or outcome; the pinned behaviour is what the research measured.

## Issues Encountered
- `latest` moved from `26.9.17.8` (research) to `26.9.18.11` between research and this run; the dedup outcomes and window values did not change.
- One foreground Gradle call overran the tool timeout and was moved to the background; it had finished green in 18 s (the timeout was in the shell wrapper, not the build).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 20-03 may quote E-01, E-02 and E-03 (verdicts flip from `pending (20-03)` when the rollout text is written). Wording facts for it: a same-token retry is dropped on a deduplicating table whatever its bytes, so the gg1 to gg2 consequence is "old rows persist if the earlier attempt landed", never "rows written twice"; `exactlyOnce=false` is also deduplicated by the token on a replicated table; a plain table or a window of 0 deduplicates nothing; the 3600 s window limits how late a retry can still be dropped. The gg1 note's "can write rows twice" reasoning is contradicted by these measurements, as research flagged.
- No blockers.

## Known Stubs
None.

## Threat Flags
None - only a test and a planning document were added.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*

## Self-Check: PASSED
- Created files exist: `DedupTokenRetryLiveTest.java`, `20-COMMAND-EVIDENCE.md` (E-01..E-04 present).
- Commits `e1e21e12`, `7f3414ef`, `7ae6af45`, `f97ac75b`, `dc51e3f1` are ancestors of HEAD; `gsd check evaluation-scope` resolves 5 commits.
- Task verifies: `DEDUP-T1-GREEN`, `DEDUP-T1-COMMIT-OK`, `DEDUP-T2-GREEN`, `DEDUP-SEEN-RED-RECORDED` all printed; class re-run 8 tests, 0 failures, 0 errors, 0 skipped; Spotless `IS CLEAN`.
- `git diff --quiet d73848807c58c4ba535da05e7515ecbbf5025bdf HEAD -- src/main build.gradle.kts gradle .github` exits 0.
