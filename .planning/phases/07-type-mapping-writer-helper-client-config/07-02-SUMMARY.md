---
phase: 07-type-mapping-writer-helper-client-config
plan: 02
subsystem: database
tags: [clickhouse, kafka-connect, table-filtering, describe, utf-8, merge-reexpression]

# Dependency graph
requires:
  - phase: 07-type-mapping-writer-helper-client-config
    provides: >-
      Plan 07-01's compileJava-green tree and Type.DYNAMIC enum constant,
      which shouldSkipDescribeRow reads via Column.getType() == Type.DYNAMIC.
provides:
  - "targetTableFilter config key (constant, field, parse, configDef.define) reaching ClickHouseWriter.start() and both system.columns discovery queries"
  - "ClickHouseHelperClient.buildTableFilterClause(String) - static, quote-doubling, injection-safe match() clause builder"
  - "ClickHouseHelperClient.shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>) - static, re-expressed over a loop-local column map instead of the now-immutable Table"
  - "Both describeTableV1/V2 loops skip structural DESCRIBE sub-column rows (.null/.size0/.size suffixes, JSON/Dynamic sub-columns) while numColumns still counts every top-level row first"
  - "describeTableV2's InputStreamReader decodes as UTF-8 explicitly, whole-file grep-provable"
  - "Query-retry message and both unmappable-column log statements routed through the logger (D-16)"
affects: [07-03-config-and-harness-carveout, 07-04-checklist-and-evidence-brief, 08-orchestration-jmx, 11-live-verification]

# Actuals (#2632)
actuals:
  tokens: 3717
  tasks: 2
  commits: 2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Loop-local Map<String, Column> maintained alongside List<Column> during DESCRIBE iteration, giving O(1) parent lookup without constructing a throwaway Table per row - the immutable-object construction pattern this phase establishes for any future mid-construction lookup need"
    - "Config key landed as a four-part unit in one commit: key constant, private final field, constructor parse, configDef.define block"
    - "One fork behaviour per commit against a byte-identical upstream baseline (D-19), continuing plan 07-01's pattern"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java

key-decisions:
  - "Extracted describeTableV2's InputStreamReader construction to name its InputStream in a separate try-with-resources variable (`InputStream describeStream = queryResponse.getInputStream();`) rather than nesting `queryResponse.getInputStream()` directly inside `new InputStreamReader(...)`. Functionally identical (charset explicit, same resource lifecycle, redundant close() on an already-closed InputStream is a no-op) - done because the plan's own UTF-8 verify grep (`new InputStreamReader([^)]*StandardCharsets\\.UTF_8`) cannot match past the first `)` inside a nested method call, so the plan's PATTERNS.md example code and its own acceptance-check regex were mutually incompatible for any nested-call form. This is a Rule 3 blocking-issue auto-fix on the plan's own verify tooling, not a design change - HLP-04's substance (explicit UTF-8, whole-file grep provable) is unchanged."
  - "Did not run the tree-wide `./gradlew spotlessApply`, continuing plan 07-01's documented decision - the tree has not been spotless-cleaned since the Phase 6 merge, so a first run reformats 50+ unrelated files. Verified instead via `./gradlew spotlessCheck` that neither touched file (`ClickHouseSinkConfig.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`) is flagged - all three are independently style-compliant without a fix."
  - "targetTableFilter's configDef.define landed in the 'Connection' group (sharing the file's primary `orderInGroup` counter, alongside `enableReplicaPinning`) rather than the newer 'DDL' group, matching the fork's pre-merge placement and the setting's connection-level scope (it affects table discovery, not schema evolution)."
  - "Plan commit ledger (protocol 0c) was not recorded before the first commit this session either (same gap as plan 07-01). Reconstructed retroactively: the plan's base is `20f4c691` (07-01's final metadata commit, HEAD at session start), confirmed via `git rev-list --count 20f4c691..HEAD` = 2, matching the 2 task commits made. Ledger file written retroactively at `.git/gsd-plan-head-before-07-02` for downstream reference."

patterns-established:
  - "Pattern: when a plan's own automated verify grep cannot match the exact code shape its own PATTERNS.md prescribes (nested method-call parens defeating a `[^)]*` negated-class regex), extract the inner expression to a named local/resource variable rather than reformatting the regex - the regex is the acceptance instrument and is not something the executor is positioned to silently loosen; changing the code to satisfy it, documented as a deviation, preserves the check's authority while keeping runtime behavior byte-identical."

requirements-completed: [CFG-01, HLP-01, HLP-02, HLP-03, HLP-04, WRT-05]

coverage:
  - id: D1
    description: "A configured targetTableFilter reaches both system.columns discovery queries as a quote-safe match() clause through the unbroken four-link chain: config parse -> ClickHouseWriter.start() -> ClickHouseClientBuilder.setTargetTableFilter -> ClickHouseHelperClient field -> showTablesV1/showTablesV2 query"
    requirement: "CFG-01, HLP-01, WRT-05"
    verification:
      - kind: other
        ref: "07-02-PLAN.md Task 1's four verify blocks (BUILD SUCCESSFUL, CONFIG_KEY_OK, FILTER_CHAIN_OK, NO_STDOUT_OK) - all re-run this session and passing"
        status: pass
    human_judgment: true
    rationale: "buildTableFilterClause's empty/non-empty/quote-doubling behavior and the live match() semantics are the behavioral canaries (ClickHouseHelperClientForkUnitTest, ClickHouseHelperClientForkLiveTest / FILT-01..04), but neither test file compiles until plan 07-03 rewrites their Table call sites. This plan's verification is grep/compile-based only (the chain is present, the signatures are upstream-shaped, no stdout write survives); the actual filtering behavior is proven in plan 07-04's testUnit run and Phase 11's live audit."
  - id: D2
    description: "shouldSkipDescribeRow is re-expressed over a loop-local Map<String, Column> (D-08's locked signature) and called from both describeTableV1 and describeTableV2 at the correct insertion point - after the alias/materialized/ephemeral skip, before Column.extractColumn - with numColumns still incremented for every non-subcolumn row before any skip decision"
    requirement: "HLP-02, HLP-03"
    verification:
      - kind: other
        ref: "07-02-PLAN.md Task 2's SKIP_HELPER_OK and COUNT_ORDER_AND_LOGLEVEL_OK verify blocks (bash-invoked; word-splitting requires bash, not zsh - both re-run and confirmed passing this session)"
        status: pass
    human_judgment: true
    rationale: "The twelve shouldSkipDescribeRow expectations in ClickHouseHelperClientForkUnitTest (suffix cases, JSON/Dynamic parent cases, never-added-parent case, map/tuple non-skip cases) are the behavioral canary, but the test file cannot compile until plan 07-03 rewrites its eleven call sites to the map-based signature. This plan's verification is static/structural (signature shape, call-site count, no throwaway Table construction, increment-before-skip line ordering); the actual skip behavior is proven in plan 07-04's testUnit run."
  - id: D3
    description: "Every InputStreamReader in the resolved ClickHouseHelperClient decodes as UTF-8, proven by a whole-file grep (reader count equals UTF-8-guarded reader count, both at least one); describeTableV2's DESCRIBE TABLE query line remains byte-identical to v1.6.0 (FREE-03)"
    requirement: "HLP-04"
    verification:
      - kind: other
        ref: "07-02-PLAN.md Task 2's UTF8_WHOLE_FILE_OK and DESCRIBE_QUERY_UNTOUCHED verify blocks, both re-run and passing (readers=1 guarded=1; git diff v1.6.0 HEAD shows zero removed 'DESCRIBE TABLE' lines)"
        status: pass
    human_judgment: false
---

# Phase 7 Plan 2: Table Filtering and DESCRIBE Sub-Column Skip Summary

**Re-threaded server-side `targetTableFilter` table discovery filtering and the DESCRIBE sub-column skip onto upstream `v1.6.0`'s rewritten `showTables`/describe-loop shape, re-expressing the fork's `shouldSkipDescribeRow` over a loop-local column map since upstream's `Table` is now immutable — two isolated commits, each independently bisectable against `v1.6.0`.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-09-19 (session start, continuing from 07-01)
- **Completed:** 2026-09-19
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- `ClickHouseSinkConfig` gained the `targetTableFilter` config key as a complete four-part unit: `TARGET_TABLE_FILTER` constant, `private final String targetTableFilter` field (Lombok-generated `getTargetTableFilter()`, no hand-written accessor), the trim-once parse in the constructor, and a `configDef.define` block in the "Connection" group.
- `ClickHouseHelperClient` gained `buildTableFilterClause(String)` — a pure static function returning the empty string for an empty filter, otherwise a space-prefixed ` AND match(table, '<filter>')` clause with every embedded single quote doubled. Wired inline into both `showTablesV1` and `showTablesV2`'s own `String.format(...)` query construction, keeping upstream's `(String database)` signature and `List<Table.TableDesc>` return type on all three `showTables*` methods — the fork's pre-merge query-string-parameter shape was not restored.
- `ClickHouseWriter.start()`'s fluent builder chain gained `.setTargetTableFilter(csc.getTargetTableFilter())`, completing the four-link chain from config through to the discovery query. The second, unrelated builder chain inside `getMutationRequest` (near the file's tail) was confirmed to belong to a different method and left untouched.
- `ClickHouseHelperClient` gained `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)` — re-signed per D-08 from the fork's `Table`-parameter form, since upstream's describe loops build a bare `List<Column>` and construct the immutable `Table` only once, after the loop. Both `describeTableV1` and `describeTableV2` now maintain a sibling `Map<String, Column> columnsByName` alongside `columns`, call the skip immediately after the existing alias/materialized/ephemeral `continue` block and before `Column.extractColumn`, and record the skip in the loop's existing `skippedCols` set on a hit.
- `numColumns` ordering (HLP-03) required no code change: the increment for non-subcolumn rows already ran ahead of both the alias skip and the new structural skip in upstream's loop shape; only insertion-point discipline was needed.
- `describeTableV2`'s `InputStreamReader` now decodes explicitly as `StandardCharsets.UTF_8` (previously platform-default, silently mangling non-ASCII column identifiers under the suite's `windows-1252` test JVM). `describeTableV1` was left untouched — it constructs no reader at all, decoding via the record iterator instead.
- Two D-16 non-behavioural tweaks landed alongside their neighbouring behaviours: the query-retry message moved from an unconditional `System.out.println` to `LOGGER.debug`, and both "Unable to handle column" log statements moved from WARN to ERROR (an unmappable column drops the whole table from the mapping — a louder outcome than WARN implies).

## Task Commits

Each task was committed atomically, split per D-19 into one commit per behaviour:

1. **Task 1: targetTableFilter chain** - `7d167152` (feat) — `ClickHouseSinkConfig.java` + `ClickHouseHelperClient.java` + `ClickHouseWriter.java`: config key, `buildTableFilterClause`, builder wiring, D-16 retry-log tweak.
2. **Task 2: DESCRIBE sub-column skip + UTF-8** - `5a4a12cd` (feat) — `ClickHouseHelperClient.java`: `shouldSkipDescribeRow`, both loop insertions, UTF-8 reader, D-16 log-level tweaks.

**Plan metadata commit** (this SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md) follows this write per the executor's `git_commit_metadata` step.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` - Added `TARGET_TABLE_FILTER` constant, field, parse, and `configDef.define` block (CFG-01, filter half).
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - Added `.setTargetTableFilter(csc.getTargetTableFilter())` to `start()`'s builder chain (WRT-05).
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - Added `buildTableFilterClause`, `targetTableFilter` field + builder setter, wired into both `showTables*` queries; added `shouldSkipDescribeRow` and wired into both describe loops with a loop-local `Map<String, Column>`; pinned the V2 describe reader to UTF-8; routed the retry message and both unmappable-column logs through the logger at appropriate levels (HLP-01..04, WRT-05 call site, D-16).

## Decisions Made

See `key-decisions` in frontmatter — most notably: extracted the V2 describe stream into its own try-with-resources `InputStream` variable so the plan's own UTF-8 verify regex (which cannot match past a nested method call's closing paren) can match the reader construction, without changing the reader's actual charset behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Plan's own UTF-8 verify regex could not match the code shape its own PATTERNS.md prescribed**
- **Found during:** Task 2, running the `UTF8_WHOLE_FILE_OK` verify block
- **Issue:** The plan's verify command greps for `new InputStreamReader([^)]*StandardCharsets\.UTF_8`. `[^)]*` is a negated character class that excludes `)`, so it cannot match past the first `)` in `new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8)` — the nested `getInputStream()` call's closing paren breaks the match before `StandardCharsets.UTF_8` is reached. The plan's own PATTERNS.md file prescribed exactly this nested form (`new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8)`), so the plan's illustrative code and its own acceptance check were mutually incompatible for that shape.
- **Fix:** Extracted the `InputStream` to its own named try-with-resources variable — `InputStream describeStream = queryResponse.getInputStream();` — then `new InputStreamReader(describeStream, StandardCharsets.UTF_8)`, which has no intervening parens and satisfies the regex. Added the `java.io.InputStream` import. Runtime behavior is unchanged: same resource, same explicit UTF-8 charset, same try-with-resources close ordering (the extra explicit close on an already-closed `InputStream` is a documented no-op).
- **Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`
- **Verification:** `readers=1 guarded=1` printed by the re-run verify block; `UTF8_WHOLE_FILE_OK` printed; `compileJava` green.
- **Committed in:** `5a4a12cd` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking — plan verify-tooling mismatch)
**Impact on plan:** No behavioral change; the fix exists solely to make the plan's own automated acceptance check pass against code that was already correct per HLP-04's substance. No scope creep.

## Issues Encountered

- The `COUNT_ORDER_AND_LOGLEVEL_OK` verify block's `set -- $INC` word-splitting failed when first invoked under this session's interactive `zsh` (unquoted parameter expansion does not word-split by default in zsh, unlike bash). Re-run explicitly via `bash -c '...'` and confirmed passing (`increments=[463 532 ] aliasSkips=[466 535 ]`, `COUNT_ORDER_AND_LOGLEVEL_OK` printed). Not a deviation — the plan's verify blocks are written for `bash` semantics, which the actual CI/gradle environment uses; this was an artifact of the interactive shell, not the code.

## User Setup Required

None - no external service configuration required.

## Evidence for Downstream Plans

**For plan 07-03 (test file rewrites) and Phase 11's SUITE-03 dropped-changes record:**

- **Two commit SHAs, one behaviour each:**
  - `7d167152` - `targetTableFilter` chain: config key, `buildTableFilterClause`, builder wiring, D-16 retry-log tweak (CFG-01/HLP-01/WRT-05).
  - `5a4a12cd` - DESCRIBE sub-column skip + UTF-8 reader + D-16 log-level tweaks (HLP-02/HLP-03/HLP-04).

- **Final signature of `shouldSkipDescribeRow`** (plan 07-03 rewrites eleven `ClickHouseHelperClientForkUnitTest` call sites against this):
  ```java
  static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar)
  ```
  Package-private static, second parameter is `Map<String, Column>` (not `Table`). The "parent intentionally never added" test case should build an empty `Map<String, Column>`, not a placeholder entry.

- **Final name and visibility of `buildTableFilterClause`:**
  ```java
  static String buildTableFilterClause(String targetTableFilter)
  ```
  Package-private static, unchanged signature from the fork's pre-merge shape — transplanted verbatim.

- **UTF-8 reader confirmation:** whole-file grep of `ClickHouseHelperClient.java` finds exactly 1 `new InputStreamReader(` construction and exactly 1 occurrence naming `StandardCharsets.UTF_8` on that construction (`readers=1 guarded=1`). No other reader exists in the file.

- **FREE-03 confirmation:** `describeTableV2`'s `DESCRIBE TABLE` query line (`String describeQuery = String.format("DESCRIBE TABLE \`%s\`.\`%s\`", database, tableName);`) is byte-identical to `v1.6.0` at both the V1 (line 439) and V2 (line 510) call sites — confirmed via `git show v1.6.0:...` diffed against the current file this session. No edit was made to either line.

## Next Phase Readiness

- `compileJava` is green after both of this plan's commits.
- `shouldSkipDescribeRow`'s final signature and `buildTableFilterClause`'s name/visibility are fixed and documented here for plan 07-03's eleven-site `ClickHouseHelperClientForkUnitTest` rewrite.
- No test task was run in this plan (`compileTestJava` cannot succeed until plan 07-03 lands, per the plan's explicit prohibition) — the behavioral canaries (`ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`) remain unverified end-to-end until plan 07-04's `testUnit` run. This is expected, not a gap.
- The Task 2 UTF-8 verify-block deviation (InputStream extraction) is a code-shape change worth noting if plan 07-03 or 07-04 diffs this file against PATTERNS.md's illustrative snippet — the running code differs from that snippet in exactly this one respect, deliberately, for the reason documented above.

## Self-Check: PASSED

Both task commits (`7d167152`, `5a4a12cd`) found in `git log`. All three modified files exist on disk. `git rev-list --count 20f4c691..HEAD` = 2, matching the 2 task commits. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava` re-run clean at self-check time: `BUILD SUCCESSFUL`. All eight of the plan's automated verify blocks re-run and passing (`BUILD SUCCESSFUL` x2, `CONFIG_KEY_OK`, `FILTER_CHAIN_OK`, `NO_STDOUT_OK`, `SKIP_HELPER_OK`, `UTF8_WHOLE_FILE_OK`, `COUNT_ORDER_AND_LOGLEVEL_OK`, `DESCRIBE_QUERY_UNTOUCHED`). No conflict markers in either commit's added lines. No test task was run, per plan prohibition.

---
*Phase: 07-type-mapping-writer-helper-client-config*
*Completed: 2026-09-19*
