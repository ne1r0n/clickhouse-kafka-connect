---
phase: 09-metadata-free-json-path-keep-or-drop
plan: 01
subsystem: database
tags: [clickhouse, kafka-connect, insertFormat, json, metadata-free, client-v2]

# Dependency graph
requires:
  - phase: 07
    provides: jsonInsertColumns config key, jsonInsertFormatExplicit flag, Table's immutable five-argument constructor
provides:
  - ClickHouseWriter.shouldSkipTableMetadata() — the one shared predicate behind all four metadata-free guard sites
  - Re-applied metadata-free insertFormat=json bypass (start/updateMapping/getTable/cleanupExtraFields guards)
  - jsonInsertColumns re-attached server-side in doInsertJsonV2 via the surviving five-argument client.insert overload
  - MetadataFreeJsonInsertUnitTest as a Docker-free testUnit canary (11 tests) for the bypass
affects: [09-02 (live proof against real ClickHouse), 09-03 (MERGE-CHECKLIST.md/REQUIREMENTS.md bookkeeping for the KEEP disposition)]

# Actuals (#2632)
actuals:
  tokens: 2356
  tasks: 4
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Single shared boolean predicate (shouldSkipTableMetadata) behind every early-return guard, rather than inlining the condition at each of the four call sites"
    - "Synthetic value object (Table) built through the same immutable constructor as every DESCRIBE-backed call site, with (false, List.of(), 0) as the explicit knowingly-empty arguments"
    - "Branch-on-emptiness overload selection for a server API with two sibling overloads, instead of an unconditional call to the richer one"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java

key-decisions:
  - "Re-confirmed D-01 (KEEP) at Task 1's checkpoint: developer answer was \"Confirm KEEP (expected).\" — proceeded to production edits on that basis, no drop variant implemented."
  - "updateMapping() under the bypass returns false, not true: upstream's contract for the boolean is \"did I refresh the mapping,\" and getTable's own guard (Step D) returns before it ever consults updateMapping, so the alternative (returning true to force a caller to re-read an empty mapping and throw loudly) was considered and rejected as redundant with getTable's own short-circuit."
  - "D-08's optional live probe (\"prove unconditional five-argument overload use is safe for an empty column list\") was deliberately not attempted — RESEARCH.md's recommendation was to keep the emptiness-branch default rather than spend a live probe proving the unconditional alternative safe, and Task 4 followed that recommendation exactly."
  - "D-13 deviation: Task 2's commit merges the four-guard/predicate/synthetic-Table production edit with MetadataFreeJsonInsertUnitTest's un-parking (removing @Disabled, rewriting its javadoc) into one commit, rather than D-13's strict six-step split — the tracer task needs a real Docker-free end-to-end gate, and the only such gate available is the already-written, previously-disabled canary class. Claude's discretion, as the plan explicitly allowed."

patterns-established:
  - "Metadata-free guard predicate: is-prefixed-adjacent shouldSkipTableMetadata(), matching the codebase's is*/should* boolean-method convention (AGENTS.md Naming Patterns)"

requirements-completed: [JSONP-01, JSONP-02, JSONP-03]

coverage:
  - id: D1
    description: "Explicit insertFormat=json connector reaches an insert without ever fetching table metadata — one shared predicate, four guards (start, updateMapping, getTable, cleanupExtraFields), one synthetic Table"
    requirement: JSONP-01
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java (11 tests, testUnit)"
        status: pass
      - kind: other
        ref: "PLAN.md Task 2 verify block: guard-shape grep counts, guard-ordering anchor check, upstream-body-preserved grep counts (all run this session, all passed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "R-01's silent data loss on the Struct/RowBinary route stays pinned, not fixed or widened — the empty column list is reproduced knowingly per D-01's accepted cost"
    requirement: JSONP-02
    verification: []
    human_judgment: true
    rationale: "The pin itself (ExplicitJsonInsertPathLiveTest's explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch) is a live ClickHouse test re-enabled in plan 09-02, not this plan — this plan only reproduces the mechanism (empty getRootColumnsList() on the synthetic Table) that the pin will assert against. No Docker-free test in this plan proves the loss stays the same shape; that requires plan 09-02's live run."
  - id: D3
    description: "jsonInsertColumns has a production consumer again, server-side, branching on list emptiness in doInsertJsonV2"
    requirement: JSONP-03
    verification:
      - kind: unit
        ref: "PLAN.md Task 4 verify block: exact-count grep for csc.getJsonInsertColumns(), both client.insert overload call shapes, and ordering anchors (doInsertJsonV1 < doInsertJsonV2 < column-list read) — run this session, passed"
        status: pass
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit — 28 classes, 324 tests, 0 failures, 0 errors, JsonInsertColumnsUnitTest among the classes that ran"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-09-20
status: complete
---

# Phase 9 Plan 1: Metadata-free JSON Path Re-Application Summary

**Re-applied the fork's four metadata-free `insertFormat=json` guard sites behind one shared predicate, rebuilt the synthetic `Table` against upstream's immutable constructor, re-attached `jsonInsertColumns` to the surviving `client.insert` overload, and un-parked the Docker-free canary test that now pins all of it inside `testUnit`.**

## Performance

- **Duration:** ~25 min (continuation run; Task 1 was resolved by a prior executor run at its checkpoint)
- **Started:** 2026-09-20 (this continuation)
- **Completed:** 2026-09-20T02:10:00Z (approx.)
- **Tasks:** 4 (Task 1 resolved-by-checkpoint in a prior run; Tasks 2, 3, 4 executed this run)
- **Files modified:** 2 (`ClickHouseWriter.java`, `MetadataFreeJsonInsertUnitTest.java`)

## Accomplishments

- Re-applied the metadata-free `insertFormat=json` bypass at all four guard sites (`start`, `updateMapping`, `getTable`, `cleanupExtraFields`) behind one shared `shouldSkipTableMetadata()` predicate
- The only hand-built `Table` in `ClickHouseWriter.java` now uses upstream's five-argument constructor with `(database, mappedName, false, List.of(), 0)`
- `updateMapping()`'s guard sits above `compareAndSet(false, true)`, so the in-progress flag is never stranded true (T-09-06)
- Un-parked `MetadataFreeJsonInsertUnitTest` (removed `@Disabled`, rewrote its javadoc) and added two new Docker-free regression pins for `getTable` and `updateMapping` under the bypass
- Re-attached `jsonInsertColumns` server-side in `doInsertJsonV2`, branching on list emptiness between the surviving five-argument and the unchanged four-argument `client.insert` overloads
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` green throughout: 28 classes, 324 tests, 0 failures, 0 errors, 2 pre-existing skips

## Task 1 (Checkpoint — resolved in prior run)

**Task 1: Re-confirm D-01 (KEEP) before the first production edit.**

Resolved by a prior executor run at its `checkpoint:decision` gate. The developer's verbatim answer: **"Confirm KEEP (expected)."**

Meaning: KEEP the metadata-free `insertFormat=json` bypass (D-01), accepting that it re-introduces R-01's silent data loss on the `Struct`/RowBinary route (pinned, not fixed) and that reversing it later costs a production migration (grants, or table pre-creation) rather than a code revert. This is a second deliberate departure from the milestone's prefer-upstream conflict policy, on the same footing as the JMX carve-out; plan 09-03 records the disposition and the conflict-policy departure in `MERGE-CHECKLIST.md`.

No code was touched for Task 1 — it is a pure re-confirmation gate. `git status --porcelain -- src/` was verified clean both at the moment the question was asked (prior run) and at the start of this continuation run (`git log --oneline -5` and `git status --porcelain -- src/` both re-verified per the resume instructions, confirming HEAD = `db17bd24...` and a clean `src/` tree before any edit in this session).

## Task Commits

Each task was committed atomically:

1. **Task 1: Re-confirm D-01 (KEEP)** — no commit (checkpoint task, no code changes); resolved in a prior executor run
2. **Task 2: End-to-end explicit-JSON connector reaches an insert without ever fetching table metadata** — `9d8f3d95` (feat)
3. **Task 3: Two Docker-free regression pins for getTable and updateMapping** — `4ee2950b` (test)
4. **Task 4: Re-attach jsonInsertColumns via the surviving column-taking client overload** — `3f9ecd82` (feat)

_Note: Task 2's commit deliberately merges the production-code guard/predicate/synthetic-Table edit with `MetadataFreeJsonInsertUnitTest`'s un-parking, a documented D-13 deviation (see below) — the tracer task needed a real end-to-end Docker-free gate, and the only Docker-free proof of a guard already existed as that previously-disabled test class._

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — added `shouldSkipTableMetadata()`, four early-return guards, the synthetic `Table` construction, and `doInsertJsonV2`'s overload branch
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` — un-parked (removed `@Disabled` and its now-unused import, rewrote the class javadoc), added two new Docker-free regression pins

## Decisions Made

- **`updateMapping` return-value choice:** returns `false` (not refreshed) under the bypass, matching upstream's existing boolean contract. **Rejected alternative:** returning `true` so a hypothetical caller re-reads an empty mapping and throws loudly — rejected because `getTable`'s own guard already returns before it ever calls `updateMapping`, so that alternative would add complexity defending against a call path that Task 2's own guard ordering makes unreachable. Task 3's second-consecutive-call pin is what defends this ordering going forward.
- **D-08's optional live probe deliberately not attempted.** RESEARCH.md flagged one genuinely open technical question — whether the five-argument `client.insert(..., List<String> columns, ...)` overload renders an empty column list safely (as "no column clause") or malformed. RESEARCH.md's own recommendation was to keep the emptiness-branch default (D-08) rather than spend a live probe proving the unconditional alternative safe. This plan followed that recommendation exactly: `doInsertJsonV2` branches on `insertColumns != null && !insertColumns.isEmpty()`, and no live probe was run. This is stated explicitly per the plan's Step verbiage requirement.
- **D-13 deviation, recorded per plan instruction:** Task 2's commit (`9d8f3d95`) merges what D-13's strict six-step split would have kept as two separate commits — the four-guard/predicate/synthetic-Table production edit, and `MetadataFreeJsonInsertUnitTest`'s un-parking (`@Disabled` removal + javadoc rewrite) — into one commit. Rationale, as stated in the plan: the tracer task (Task 2, `type="tracer"`) needs a real, runnable end-to-end gate to prove the vertical slice, and the only Docker-free proof available for these four guards is the already-written `MetadataFreeJsonInsertUnitTest` class, which was disabled. Un-parking it separately would have left Task 2's own commit unprovable by any Docker-free test until a later commit. This was the plan author's explicit discretion, not a Rule 1-4 deviation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Task 2's first commit accidentally swept in unrelated pre-staged `.planning/` files**
- **Found during:** Task 2 (immediately after the initial `git commit` call)
- **Issue:** Before this continuation session began, two `.planning/` files (`milestone.lock`, `09-DISCUSS-CHECKPOINT.json`) were already staged in the git index from a prior, unrelated session (visible in the git status snapshot at conversation start). The `task_commit_protocol`'s `git add <specific files>` step only adds the intended files to an *already-populated* index — it does not clear pre-existing staged entries. The subsequent `git commit` therefore committed all four files (two intended `src/` files plus the two pre-staged `.planning/` files), violating Task 2's acceptance criterion that the commit touch exactly the two files in `files_modified`.
- **Fix:** `git reset --soft HEAD~1` to undo the commit while preserving the index and working tree exactly as they were; `git restore --staged` on the two unrelated `.planning/` paths to return them to their pre-session state (one reverted to untracked, the other reverted to fully absent from the index, matching the conversation-start snapshot); re-staged only the two intended `src/` files; re-committed with the identical commit message. Verified with `git diff --stat "${SHA}^..${SHA}"` showing exactly 2 files changed.
- **Files modified:** No additional files modified beyond the plan's declared two — the fix only corrected which files were included in the commit, not any file content.
- **Verification:** `SHA=$(git rev-parse HEAD); git diff --stat "${SHA}^..${SHA}"` confirmed exactly `ClickHouseWriter.java` and `MetadataFreeJsonInsertUnitTest.java`, matching the plan's Task 2 acceptance criterion verbatim.
- **Committed in:** `9d8f3d95` (the corrected Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 Rule 1 — git staging bug caused by pre-existing session state, not by any plan or task-code defect)
**Impact on plan:** No production or test code was affected by this deviation or its fix — it was purely a git-staging correction to keep each task's commit scoped to its declared files. All three task commits now carry exactly their intended file sets, verified individually via `git diff --stat` after each commit.

## Issues Encountered

None beyond the staging deviation documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `ClickHouseWriter.java` and `MetadataFreeJsonInsertUnitTest.java` are ready for plan 09-02, which proves the re-applied bypass live against real ClickHouse (including re-enabling `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, and `ClickHouseSinkTaskJsonInsertFormatTest`, none of which this plan touched).
- Plan 09-03 still owes: `MERGE-CHECKLIST.md` row 26 annotation and the new KEEP-disposition Recorded Fact, `REQUIREMENTS.md`'s JSONP-03 correction blockquote, and `TableTest.extractNullables()`'s `3`→`2` assertion fix (D-14) — none of these were in this plan's `files_modified` scope and none were touched here.
- No blockers for 09-02 or 09-03 from this plan's work. `testUnit` is green (28 classes, 324 tests, 0 failures, 0 errors) with Docker stopped, confirming the four guard sites and the `jsonInsertColumns` re-attachment are provably correct without a live ClickHouse instance.

## Self-Check: PASSED

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — FOUND
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` — FOUND
- Commit `9d8f3d95` — FOUND in `git log --oneline --all`
- Commit `4ee2950b` — FOUND in `git log --oneline --all`
- Commit `3f9ecd82` — FOUND in `git log --oneline --all`
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` re-confirmed green after Task 4 (28 classes, 324 tests, 0 failures, 0 errors)
- `git diff --stat "${SHA}^..${SHA}"` re-confirmed exactly the declared file(s) for each of the three commits

---
*Phase: 09-metadata-free-json-path-keep-or-drop*
*Completed: 2026-09-20*
