---
phase: 12-upstream-v1-7-0-merge-reconcile
plan: "01"
subsystem: infra
tags: [git-merge, upstream-sync, invariant-script, line-provenance, fork-anchors, gradle]

requires:
  - phase: 11
    provides: "custom_build c61d90ce (v1.6.0_gg1 published); pre-v1.6.0 invariant script and Recorded Facts"
provides:
  - "Lightweight rollback tag pre-v1.7.0-sync on c61d90ce and local branch sync/v1.7.0"
  - "One mechanical merge commit M (54e63648) of refs/tags/v1.7.0 with zero authored lines, green on the D-03 gate"
  - "Parameterised verify-merge-invariants.sh (shape, line provenance, conflicted-paths cross-check, marker scan, per-commit anchors) and a 4-case fail-first self-test"
  - "conflicted-paths-v1.7.0.txt (5 paths) and fork-anchors.txt (39 rows) for every later Phase 12-15 gate"
affects: [12-02, 12-03, 12-04, 12-05, phase-13, phase-14, phase-15]

plan_head_before: 3b6736e3294781bacadc2513d820a2c52eb609b2
plan_head_after: 605ddf549e21dcc7b43db7c07071a1e26d26ffa3

actuals:
  tokens: 5400
  tasks: 2
  commits: 27

tech-stack:
  added: []
  patterns:
    - "Merge-commit line provenance by set membership against both parents (fd 3 read loop, whole-file oid shortcut for binaries)"
    - "Per-commit anchor inventory: a file in the tree is evaluated at every commit in PRE_TAG..range-end that carries it"
    - "Fail-first self-test built from dangling commit-tree objects only"

key-files:
  created:
    - .planning/scripts/verify-merge-invariants-selftest.sh
    - .planning/scripts/conflicted-paths-v1.7.0.txt
    - .planning/scripts/fork-anchors.txt
  modified:
    - .planning/scripts/verify-merge-invariants.sh
    - VERSION
    - gradle/libs.versions.toml
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java

key-decisions:
  - "Invariant 0 accepts a rollback tag that is itself a merge commit when every one of its parents is an ancestor of MERGE^1 (c61d90ce is the --no-ff merge of sync/v1.6.0, so it is not an ancestor of a sync/v1.6.0-derived branch)"
  - "Provenance skips a path whose blob oid equals the same path in either parent; a binary path that matches neither parent is a violation (BSD grep -f truncates patterns at NUL)"
  - "Fork anchors are measured at M and authored after it, so the D-17 and LZ4 anchors that the merge legitimately moved never fail at M (Pitfall 8)"

patterns-established:
  - "zsh gotchas: write \"${M}:path\" with braces, and never assign to a variable named path (it is tied to PATH)"
  - "Gradle always as env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ..."

requirements-completed: [SYNC-07, BLD-13]

coverage:
  - id: D1
    description: "Tag pre-v1.7.0-sync on c61d90ce, branch sync/v1.7.0, exactly one first-parent merge commit M whose second parent is refs/tags/v1.7.0 (379b8d01); custom_build and origin/custom_build untouched"
    requirement: SYNC-07
    verification:
      - kind: other
        ref: "git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD | wc -l == 1; git rev-parse M^2 == 379b8d018c090ba5279c2a9f86dbfe4934a19329; git rev-parse custom_build origin/custom_build == c61d90ce"
        status: pass
    human_judgment: false
  - id: D2
    description: "Parameterised verify-merge-invariants.sh passes on the v1.6.0 merge (v1.6.0 values) and on M (defaults), and the self-test proves provenance, shape and anchor checks can fail"
    requirement: SYNC-07
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants-selftest.sh (PASS ST1..ST4)"
        status: pass
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh M HEAD (result=PASS)"
        status: pass
    human_judgment: false
  - id: D3
    description: "M compiles in all four source sets and Docker-free testUnit reports 0 failures and 0 errors (D-03)"
    requirement: SYNC-07
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava testUnit (BUILD SUCCESSFUL; 31 classes, 333 tests, 0 failed, 2 skipped)"
        status: pass
    human_judgment: false
  - id: D4
    description: "BLD-13 catalog state at M: bare clickHouseDriver = 0.9.6, kafka = 4.1.1 override kept, no clickhouse-jdbc in the catalog or build.gradle.kts, no 0.9.5 override comment"
    requirement: BLD-13
    verification:
      - kind: other
        ref: "git show M:gradle/libs.versions.toml | grep -c clickhouse-jdbc == 0; git show M:build.gradle.kts | grep -c clickhouse.jdbc == 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "Anchor inventory (39 rows) and conflicted-paths record committed after M; every FORK-DELTA-backed anchor has the same count at pre-v1.7.0-sync and at M"
    requirement: SYNC-07
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh M HEAD (anchor_rows=39, result=PASS)"
        status: pass
    human_judgment: false

duration: 11min
completed: 2026-10-07
status: complete
---

# Phase 12 Plan 01: Upstream v1.7.0 Merge and Invariant Script Summary

**Upstream v1.7.0 merged onto local sync/v1.7.0 as one mechanical merge commit (54e63648, zero authored lines, 31 classes / 333 tests green), behind a parameterised provenance-and-anchor invariant script whose every check is proven able to fail.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-10-07T19:24:13Z
- **Completed:** 2026-10-07T19:35:18Z
- **Tasks:** 2
- **Files modified:** 4 `.planning/scripts` files authored plus the 5 conflicted source paths (the merge also brought 17 upstream-only paths)

## Accomplishments

- Lightweight tag `pre-v1.7.0-sync` on `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`; branch `sync/v1.7.0` cut from the `sync/v1.6.0` HEAD (`3b6736e3`). Nothing was pushed; `custom_build` and `origin/custom_build` both still resolve to `c61d90ce`.
- **M = `54e6364858cd728e3427ce2058fc00380895c689`**, parents `b1d26fd2` (P0) and `379b8d018c090ba5279c2a9f86dbfe4934a19329` (`refs/tags/v1.7.0`). The invariant script reports `provenance_paths=21`, zero authored lines.
- The D-03 gate holds on M and BLD-13 is proven by the mechanical merge alone.
- `verify-merge-invariants.sh` takes `PRE_TAG`, `UPSTREAM_REF`, `ANCHORS_FILE`, `CONFLICTED_PATHS_FILE` as `${VAR-default}` overrides and re-checks the v1.6.0 merge `673d29c3` with the v1.6.0 values.

## Actual conflicted list and resolution

`git diff --name-only --diff-filter=U` at merge time was exactly the dry-run prediction:

| Path | Resolution |
|------|------------|
| `VERSION` | Fork side, `v1.6.0_gg1` (`git checkout --ours`, byte-identical to `pre-v1.7.0-sync`) (D-05) |
| `gradle/libs.versions.toml` | Upstream `clickHouseDriver = "0.9.6"` plus the fork `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)` line; fork's `0.9.5` override line and upstream's `kafka = "2.7.0"` dropped |
| `ClickHouseSinkConfig.java` | Union in all 4 hunks (constants, fields, constructor assignments, both `configDef.define` blocks), fork lines first |
| `ClickHouseWriter.java` | Union in `start()`: `.setTargetTableFilter(...)`, `.setCompressionMethod(...)`, `.build();` |
| `ClickHouseHelperClient.java` | Union for fields, constructor, builder field and both setters (each with its own `return this; }`); upstream side for `getDefaultClientOptions` and the `createClientV2` chain (`.compressServerResponse(false)` + `.setDefaultDatabase(...)`) |

Markers were removed by deleting only marker lines (or by choosing a side with `Edit`); no new line was authored. `git diff --cached --check` was clean.

## D-03 gate totals at M

Command: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava testUnit` produced `BUILD SUCCESSFUL`.

| classes | tests | skipped | failures | errors |
|---------|-------|---------|----------|--------|
| 31 | 333 | 2 | 0 | 0 |

The planning baseline was 30 classes / 331 tests / 2 skipped; the delta is upstream's new `StructToJsonMapTest` (2 tests). Spot checks: `StructToJsonMapTest` 2/0/0, `NetworkCompressionPinUnitTest` 4/0/0, `NestedNullableCompositeWriteTest` 3/0/0, `NewUpstreamConfigDefaultsUnitTest` 3/0/0, `ClickHouseVersionDefaultUnitTest` 1/0/0 (tests/failures/errors).

## Script and self-test output

```
# v1.6.0 merge, v1.6.0 values, range-end pre-v1.7.0-sync
invariants: provenance_paths=214 anchor_commits=0 anchor_rows=0 marker_commits=726 result=PASS

# M, anchors and conflicted-paths checks off (pre-A)
invariants: provenance_paths=21 anchor_commits=0 anchor_rows=0 marker_commits=46 result=PASS

# M to HEAD, all defaults (after commit A)
invariants: provenance_paths=21 anchor_commits=1 anchor_rows=39 marker_commits=48 result=PASS

# self-test (final, after the bash 3.2 fix)
PASS ST1
PASS ST2
PASS ST3
PASS ST4
selftest: no failures
```

Before commit A the self-test printed `PASS ST1 / ST2 / ST3` and `SKIP ST4`, as the plan specified.

## Anchor survival table (SYNC-09 tripwire)

Count = lines matching `grep -cF -e <pattern>`. Only rows backed by BLD-09, BLD-13, CMP-01 or CMP-02 differ; every FORK-DELTA-backed row is equal at `pre-v1.7.0-sync` and at M.

| Path (under `src/main/java/com/clickhouse/kafka/connect/` unless rooted) | Pattern | pre-v1.7.0-sync | M | Backs |
|------|---------|-----|---|-------|
| `sink/ClickHouseSinkConfig.java` | `TARGET_TABLE_FILTER` | 3 | 3 | CFG-01 |
| `sink/ClickHouseSinkConfig.java` | `jsonInsertFormatExplicit` | 2 | 2 | CFG-01 |
| `sink/ClickHouseSinkConfig.java` | `//this.addClickHouseSetting("async_insert", "0", false);` | 1 | 1 | CFG-02 |
| `sink/ClickHouseSinkTask.java` | `public void onPartitionsRevoked(Collection<TopicPartition> partitions) {` | 1 | 1 | RF-8 |
| `util/jmx/SinkTaskStatistics.java` | `connector=%s` | 1 | 1 | ORCH-02 |
| `sink/db/ClickHouseWriter.java` | `shouldSkipTableMetadata()` | 6 | 6 | JSONP-01 |
| `sink/db/ClickHouseWriter.java` | `csc.getJsonInsertColumns()` | 1 | 1 | JSONP-03 |
| `sink/db/ClickHouseWriter.java` | `case "DYNAMIC":` | 1 | 1 | WRT-01 |
| `sink/db/ClickHouseWriter.java` | `buildRowBinaryUnsupportedTypeMessage` | 5 | 5 | WRT-01 |
| `sink/db/ClickHouseWriter.java` | `.setTargetTableFilter(csc.getTargetTableFilter())` | 1 | 1 | HLP-01 |
| `sink/db/ClickHouseWriter.java` | `new ClickHouseHelperClient.ClickHouseClientBuilder(` | 2 | 2 | CMP-01 |
| `sink/db/ClickHouseWriter.java` | `.setCompressionMethod(csc.isSetCompressionMethod())` | 0 | 2 | CMP-01 (moved) |
| `sink/db/helper/ClickHouseHelperClient.java` | `buildTableFilterClause` | 4 | 4 | HLP-01 |
| `sink/db/helper/ClickHouseHelperClient.java` | `shouldSkipDescribeRow` | 3 | 3 | HLP-02 |
| `sink/db/helper/ClickHouseHelperClient.java` | `StandardCharsets.UTF_8` | 1 | 1 | HLP-04 |
| `sink/db/helper/ClickHouseHelperClient.java` | `InputStreamReader` | 2 | 2 | HLP-04 |
| `sink/db/helper/ClickHouseHelperClient.java` | `.compressServerResponse(false)` | 0 | 1 | CMP-02 (moved) |
| `sink/db/helper/ClickHouseHelperClient.java` | `options.put(ClickHouseClientOption.COMPRESS, false);` | 0 | 1 | CMP-02 (moved) |
| `sink/db/helper/ClickHouseHelperClient.java` | `NETWORK_COMPRESSION_METHOD_` | 4 | 2 | CMP-01 (moved; 12-03 sets 0) |
| `sink/db/mapping/Type.java` | `DYNAMIC` | 1 | 1 | MAP-01 |
| `sink/db/mapping/Column.java` | `Dynamic` | 1 | 1 | MAP-01 |
| `sink/state/provider/KeeperStateProvider.java` | `new ClickHouseHelperClient.ClickHouseClientBuilder(` | 1 | 1 | CMP-01 |
| `sink/state/provider/KeeperStateProvider.java` | `.setCompressionMethod(csc.isSetCompressionMethod())` | 0 | 1 | CMP-01 (moved) |
| `src/main/resources/META-INF/services/org.apache.kafka.connect.transforms.Transformation` | `com.clickhouse.kafka.connect.transforms` | 7 | 7 | SUITE-02 |
| `.github/workflows/tests.yaml` | `timeout-minutes: 45` | 1 | 1 | CI-05 |
| `VERSION` | `v1.6.0_gg1` | 1 | 1 | BLD-07 |
| `build.gradle.kts` | `windows-1252` | 2 | 2 | BLD-05 |
| `build.gradle.kts` | `slf4j-reload4j:2.0.17` | 1 | 1 | BLD-08 |
| `build.gradle.kts` | `jacocoForkReport` | 3 | 3 | BLD-03 |
| `build.gradle.kts` | `register<Test>("testUnit")` | 1 | 1 | BLD-04 |
| `build.gradle.kts` | `withCapability("org.lz4:lz4-java")` | 1 | 2 | BLD-09 (moved; 12-02 sets 1) |
| `build.gradle.kts` | `clickhouse.jdbc` | 1 | 0 | BLD-13 (moved) |
| `gradle/libs.versions.toml` | `kafka = "4.1.1"` | 1 | 1 | BLD-08 |
| `gradle/libs.versions.toml` | `clickhouse-jdbc` | 1 | 0 | BLD-13 (moved) |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` | `@Tag("clickhouse")` | 1 | 1 | HARN-01 |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` | `sharedContainerStarts()` | 2 | 2 | HARN-03 |
| `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` | `CLICKHOUSE_VERSION_LATEST` | 2 | 2 | RF-15 |
| `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` | `StandardCharsets.UTF_8` | 3 | 3 | HLP-04 |
| `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` | `.setCompressionMethod(csc.isSetCompressionMethod())` | 0 | 1 | CMP-01 (moved) |

Every measured value matched the research prediction. The builder-site invariant holds at M: 3 `new ...ClickHouseClientBuilder(` lines and 3 `.setCompressionMethod(csc...)` lines in `src/main`. `KeeperStateProvider.java` is byte-identical to `v1.7.0`.

## Task Commits

1. **Task 1 (tracer): merge slice end-to-end** - `b1d26fd2` (P0, feat: parameterised script + self-test), `54e63648` (M, merge), `efecaffb` (fix: invariant 0 ancestry, deviation 2)
2. **Task 2: conflicted-paths record and anchor inventory** - `7216fb2c` (A, chore), `605ddf54` (fix: self-test on bash 3.2, deviation 4)

**Plan metadata:** committed separately (docs: complete plan).

`commits: 27` in the frontmatter is the measured `git rev-list --count` from `plan_head_before`; it includes upstream's 22 commits that arrive through M's second parent. The plan's own first-parent commits number 5.

## Files Created/Modified

- `.planning/scripts/verify-merge-invariants.sh` - parameterised gate: shape, line provenance, conflicted-paths cross-check, marker scan, per-commit anchors
- `.planning/scripts/verify-merge-invariants-selftest.sh` - ST1..ST4 over dangling synthetic commits
- `.planning/scripts/conflicted-paths-v1.7.0.txt` - the 5 conflicted paths, generated by `git merge-tree`
- `.planning/scripts/fork-anchors.txt` - 39 anchor rows
- The 5 conflicted source paths listed above, resolved inside M

## Decisions Made

- Invariant 0 ancestry is relaxed for a merge-commit rollback tag (see deviation 2). Intent preserved: M^1 must contain the entire pre-sync history.
- Provenance short-circuits on blob-oid equality with either parent, and treats a binary path that matches neither parent as a violation.
- Anchors authored after M from measured values, not predicted ones.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Precondition "tracked tree clean" read as "no source changes"**
- **Found during:** Task 1 precondition check
- **Issue:** `git status --porcelain --untracked-files=no` listed `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json` as modified. These are the orchestrator's own "Phase 12 execution started" bookkeeping, written after the plan was authored.
- **Fix:** Proceeded after confirming by read-only checks that no tracked source file was dirty and that the merge touches nothing under `.planning/` (fork-only paths cannot conflict). Every commit staged explicit paths, so those three files were never swept in. The D-03 gate ran with the same three files dirty.
- **Verification:** `git status --short | grep -v '^??'` shows only those three files before and after M. Flagged here so a reviewer can disagree: the literal wording of the precondition was not met.
- **Files modified:** none

**2. [Rule 1 - Bug] Invariant 0 ancestry assertion was impossible for the real topology**
- **Found during:** Task 1, running the script on M
- **Issue:** `pre-v1.7.0-sync` is `c61d90ce`, the `--no-ff` merge of `sync/v1.6.0` into `custom_build` (parents `6ac2d20c` and `586af775`). `sync/v1.6.0` HEAD descends from `586af775`, so `c61d90ce` is not its ancestor. The plan and D-01 both assumed "`c61d90ce` plus 18 planning commits"; the code is identical (`git diff c61d90ce 3b6736e3 -- . ':!.planning'` is empty) but the topology is not. `git merge-base --is-ancestor PRE_TAG MERGE^1` therefore failed and gated all provenance checks off.
- **Fix:** Invariant 0 now accepts a tag that is an ancestor of MERGE^1, or a merge-commit tag whose every parent is. Both parents of `c61d90ce` are ancestors of P0.
- **Files modified:** `.planning/scripts/verify-merge-invariants.sh`
- **Verification:** script PASS on M and HEAD; ST3 still fails on a wrong second parent.
- **Committed in:** `efecaffb`
- **Consequence for later phases:** `pre-v1.7.0-sync..HEAD` holds 48 commits at this point (the 22 planning commits, the upstream history, and this plan's own), not the 22 the plan assumed. Harmless for the marker scan. Phase 15's non-force `--no-ff` merge into `custom_build` is unaffected because `586af775` is an ancestor of `sync/v1.7.0`.

**3. [Rule 1 - Bug] Binary files broke line provenance**
- **Found during:** Task 1, first run of the script on `673d29c3`
- **Issue:** `gradle/wrapper/gradle-wrapper.jar` reported 77 authored lines. macOS BSD `grep -f` truncates patterns at NUL, so a byte-identical binary failed the comparison.
- **Fix:** A path whose blob equals the same path in either parent is skipped; a binary path matching neither parent is reported as a violation.
- **Files modified:** `.planning/scripts/verify-merge-invariants.sh`
- **Verification:** the v1.6.0 merge passes with `provenance_paths=214`; ST2 still fails on an authored text line.
- **Committed in:** `b1d26fd2` (before the merge)

**4. [Rule 1 - Bug] Self-test aborted under `set -u` on bash 3.2**
- **Found during:** Task 2, first full self-test run
- **Issue:** ST4 calls `expect_failure` with no env overrides; expanding the empty `env_args` array is an error on macOS bash 3.2.
- **Fix:** `${env_args[@]+"${env_args[@]}"}` idiom.
- **Files modified:** `.planning/scripts/verify-merge-invariants-selftest.sh`
- **Verification:** PASS ST1..ST4.
- **Committed in:** `605ddf54`

---

**Total deviations:** 4 auto-fixed (3 bug, 1 blocking)
**Impact on plan:** All fixes were to the planning scripts or to reading a precondition; M itself is the mechanical result the plan specified. No scope creep. Deviations 2-4 are extra commits beyond the P0 / M / A shape.

## Issues Encountered

- A zsh pitfall cost two tool calls: `$M:path` expands as a modifier (the plan warned about it), and assigning to a variable named `path` replaces `PATH`. Both were re-run with braces and a different variable name; no repository state was affected.
- The first full run of the script on the v1.6.0 merge takes about 25 s (726 `git grep` marker scans); the self-test takes about 80 s.

## Known Stubs

None.

## Threat Flags

None. The scripts create dangling objects only; the self-test left no ref (`git for-each-ref` count unchanged at 140).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 12-02 onward run on `sync/v1.7.0` (the checkout is on it). Each anchor-changing commit must update `fork-anchors.txt` in the same commit: `withCapability("org.lz4:lz4-java")` 2 -> 1 (BLD-09) and `NETWORK_COMPRESSION_METHOD_` 2 -> 0 (CMP-01).
- The MAP guard shadow, the dead D-17 constants and the fork LZ4 rule survive inside M by design and are fixed in the WRT, CMP and BLD commits.
- `.planning/STATE.md`, `config.json` and `state.json` were dirty at start (orchestrator bookkeeping) and are committed with this plan's metadata where the plan touches them.

## Self-Check: PASSED

- FOUND: `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/verify-merge-invariants-selftest.sh`, `.planning/scripts/conflicted-paths-v1.7.0.txt`, `.planning/scripts/fork-anchors.txt`
- FOUND commits: `b1d26fd2`, `54e63648`, `efecaffb`, `7216fb2c`, `605ddf54` (all ancestors of HEAD)
- Post-commit deletion check: the only deletion in M is upstream's own rename of `src/testFixtures/avro/schemas/incompatible/union_two_records.json`.

---
*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Completed: 2026-10-07*
