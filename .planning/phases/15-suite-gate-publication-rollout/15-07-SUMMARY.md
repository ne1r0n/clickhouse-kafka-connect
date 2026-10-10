---
phase: 15-suite-gate-publication-rollout
plan: "07"
subsystem: testing
tags: [suite-gate, ci-rehearsal, approval-bundle, github-rest-api, colima, testcontainers]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: "plans 15-01 to 15-06 (D-01/D-02 code, review, VERSION, CHANGELOG, FORK-DELTA, rollout doc, security audits, dispositions)"
provides:
  - "Recorded Fact 38 (Phase 15 local gate) and Fact 39 (rehearsal CI run) in MERGE-CHECKLIST-v1.7.0.md"
  - "origin refs/heads/sync/v1.7.0 at ae8520ed (X), the rehearsed tree plan 15-08 merges by hash"
  - "One green tests.yaml run on X, 391 s against a 1350 s threshold"
  - "The measured D-12 approval bundle (section below), presented verbatim by plan 15-08"
affects: [15-08]

plan_head_before: 6e25bb1beae64831cbe38f01501d51430c9ecc34
plan_head_after: bbf08ff32ca7eb6d2cd826cfba436f0833bf5865

actuals:
  tokens: 1810
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Gate the exact tree to be published, then rehearse that same SHA on CI before any merge"
    - "Copy Run 1 results aside before Run 2 --rerun overwrites them"

key-files:
  created:
    - .planning/phases/15-suite-gate-publication-rollout/15-07-SUMMARY.md
  modified:
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "No CHANGELOG date commit: the 1.7.0_gg1 heading already read 2026-10-10, today's UTC date"
  - "Gate evidence for Fact 38 is committed before the push so it travels with X; Fact 39 and this SUMMARY land after X and stay local (D-14)"

requirements-completed: [SUITE-04, PUB-01, PUB-03, PUB-04]

coverage:
  - id: D1
    description: "The tree to be published passes the full suite twice on ClickHouse latest x client V2 (clean test, then test --rerun) with 0 failures, 0 errors and exactly FORK-DELTA's 18 named skips, and the generated Version.java reads v1.7.0_gg1"
    requirement: SUITE-04
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean test; then ./gradlew test --rerun (BOTH-RUNS-GREEN-NAMED-SKIPS, set equality True, not-newer 0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "testUnit passes with the colima vz profile stopped and docker info failing, and the merge invariants hold on the gated tree"
    requirement: SUITE-04
    verification:
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun with docker info rc=1 (35 classes, 454 tests, 0 failures, 0 errors)"
        status: pass
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD -> result=PASS, exit 0"
        status: pass
    human_judgment: false
  - id: D3
    description: "sync/v1.7.0 at X is published alone with the explicit refspec, custom_build and the tag list are untouched, and exactly one tests.yaml run on X is green under half of timeout-minutes"
    requirement: PUB-03
    verification:
      - kind: other
        ref: "REHEARSED-TREE-OK, REFS-UNCHANGED-OK, REHEARSAL-RUN-OK (run 38048114845, 391 s < 1350 s)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The approval bundle is complete and decidable without opening a planning file"
    verification: []
    human_judgment: true
    rationale: "Whether the bundle, in particular the knowingly-open list, justifies the one-way custom_build push is the user's decision at the 15-08 gate"

duration: 21min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 07: Suite gate, rehearsal push and approval bundle Summary

**The exact tree to be published (X = ae8520ed) passed the full suite twice on ClickHouse 26.9.14.10 x client V2 (830 tests, 0 failures, 0 errors, the 18 named skips), passed testUnit with Docker genuinely stopped, passed the merge invariants, and ran green once on CI in 6m 31s after a branch-only push, so plan 15-08 can decide from measured evidence.**

## Performance

- **Duration:** 21 min
- **Started:** 2026-10-10T11:11:22Z
- **Completed:** 2026-10-10T11:32Z
- **Tasks:** 3
- **Files modified:** 1 tracked file (`.planning/MERGE-CHECKLIST-v1.7.0.md`, two appended Recorded Facts) plus this SUMMARY

## Accomplishments

- Run 1 (`clean test`) and Run 2 (`test --rerun`) both green, identical skip sets equal to FORK-DELTA's 18 named skips, Run 2 provably re-executed.
- `testUnit` green with the vz profile stopped; the profile was restarted and Docker is running again.
- `sync/v1.7.0` pushed alone at X; origin `custom_build` and the tag list unchanged; one green CI run under the threshold.
- Approval bundle assembled below, every item measured or read by command.

## Task Commits

1. **Task 1: Two full-suite runs, version, skip-set and server-version checks** - no commit (measurement only; evidence under `/tmp/gsd15/` and untracked `build/phase15-run1-results/`)
2. **Task 2: testUnit with Docker down, invariant gate, Recorded Fact 38** - `ae8520ed` (docs). This is X, the commit that was pushed.
3. **Task 3: Readiness, branch-only push, one green CI run, Recorded Fact 39** - `bbf08ff3` (docs, local tail, not pushed)

**Plan metadata:** the docs commit that carries this SUMMARY, STATE and ROADMAP (local tail, not pushed).

T0 (the gated commit, HEAD when Run 1 started) = `6e25bb1beae64831cbe38f01501d51430c9ecc34`. X (pushed) = `ae8520ed2ae848cb131e22a729ca670571782d55`. Between T0 and X only `.planning/MERGE-CHECKLIST-v1.7.0.md` changed.

The push command, exactly as run at 2026-10-10 11:21:20Z:

`git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0`

Rehearsal run: id `38048114845`, https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38048114845

## Approval bundle

Every item below was measured in this plan or read from a committed file by command. Plan 15-08 presents it verbatim.

### 1. Run 1 and Run 2

Both on `clickhouse/clickhouse-server:latest` and client V2 (`CLIENT_VERSION`, `CLICKHOUSE_VERSION`, `CLICKHOUSE_CLUSTER_NAME` unset), colima profile `vz`, commit T0.

| | Command | Window (UTC) | Gradle | Classes | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|---|---|---|---|
| Run 1 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean test` | 11:11:37 to 11:14:23 | BUILD SUCCESSFUL in 2m 45s, exit 0 | 69 | 830 | 0 | 0 | 18 |
| Run 2 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` | 11:14:30 to 11:17:14 | BUILD SUCCESSFUL in 2m 41s, exit 0 | 69 | 830 | 0 | 0 | 18 |

- **Skip-set equality:** the sorted skipped `class#method` lists of Run 1 and Run 2 are identical (`diff` empty), and equal FORK-DELTA's Named skip inventory as sets of names: the seven named methods (`ClickHouseSinkTaskTest#clientNameTest()`, `ClickHouseSinkTaskWithSchemaTest#schemaWithNestedTupleMapArrayAndVariant()`, `ClickHouseSinkTaskWithSchemaTest#schemaWithTupleOfMapsWithVariantTest()`, `ConvertToMapOrJsonPerformanceTestJson#testDeepNestedStructPerformance()`, `ConvertToMapOrJsonPerformanceTestJson#testWideStructPerformance()`, `InsertFormatPerformanceTest#compareInsertPerformance()`, `JsonColumnTest#compareAllConfigurations()`) plus exactly eleven `ClickHouseSinkTaskWithSchemaTest#[1]` to `[11]` cases. None extra, none missing.
- **Run 2 re-executed:** per-class XML timestamps span 11:11:45.495Z to 11:14:20.322Z in Run 1 and 11:14:35.079Z to 11:17:11.368Z in Run 2; zero classes had a Run 2 timestamp that was not newer. Run 1's results were copied to `build/phase15-run1-results/` before Run 2.
- **Generated version:** after Run 1's `clean`, `Version.java` contains `ARTIFACT_VERSION = "v1.7.0_gg1"`.
- **Server version and image date:** `com.clickhouse.build.version` and `Created` read before Run 1, after Run 1 and after Run 2: `26.9.14.10 2026-10-09T16:14:51.196309763Z` every time. **Drift note:** `latest` did not move between the runs. (Phase 14's run, Fact 36, was 805 tests; this run is 830.)

### 2. testUnit with Docker down

- `colima stop --profile vz`, then `/opt/homebrew/opt/docker/bin/docker info` exited **1**: `failed to connect to the docker API at unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock; check if the path is correct and if the daemon is running: dial unix /Users/alexandr.chashnikov/.colima/vz/docker.sock: connect: no such file or directory`.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun`: BUILD SUCCESSFUL in 4s; XML totals classes 35, tests 454, failures 0, errors 0, skipped 2. `docker info` still exited 1 after the run.
- `colima start --profile vz`: `colima list` shows `vz Running`, `docker info` succeeds. The profile is left running.

### 3. Merge invariants

`bash .planning/scripts/verify-merge-invariants.sh 54e6364858cd728e3427ce2058fc00380895c689 HEAD` printed `invariants: provenance_paths=21 anchor_commits=124 anchor_rows=43 marker_commits=171 result=PASS`, exit **0**. The checklist has zero `pending` rows for owning phases 12 to 15.

### 4. Rehearsal run

- Run id `38048114845`, https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38048114845, workflow `.github/workflows/tests.yaml`, event `push`, head_branch `sync/v1.7.0`, head_sha X, run_attempt 1, status `completed`, conclusion **success**. Exactly one run exists for that SHA and path; no cancelled sibling.
- Run duration `run_started_at` 11:21:25Z to `updated_at` 11:27:56Z = **391 s (6m 31s)**; job `114201643510` 11:21:29Z to 11:27:55Z = 386 s (6m 26s). `timeout-minutes` is 45, so the threshold is 22.5 min (1350 s): **under**, by 959 s.
- Annotation "Test fork count": `runner nproc=4, derived maxParallelForks=2 (build.gradle.kts: availableProcessors/2, minimum 1)`. All steps `success`, including "Compile integration-test sources" before "Run tests".

### 5. Audit verdicts

| Audit | File | `threats_open` | Register |
|---|---|---|---|
| Phase 13 | `13-SECURITY.md` | **0** | 20 threats: 19 closed, 1 open below the high threshold |
| Phase 14 | `14-SECURITY.md` | **0** | 17 threats: 16 closed, 1 open below the high threshold |

The two open items are non-blocking and are left to the user's decision:

- **T-13-19** (medium): `ClickHouseWriter.java:461` and `:515` log the raw field value at ERROR (`Error parsing date time string: {}`) when a String value cannot be parsed into a DateTime column. Present unchanged in upstream `v1.7.0`; contradicts AGENTS.md's no-record-data-in-logs rule. Fixing it is a `src/main` change that D-04 reserves for a user decision.
- **T-14-16** (low): `tests.yaml` has no top-level `permissions:` block and its actions are pinned by tag, not commit SHA. Pre-existing and also true of upstream's file; the workflow reads no secret. Adding `permissions: contents: read` is a CI change outside this phase.

### 6. Knowingly-open list (D-03)

Published as open, each with a one-line reason recorded in the REVIEW-DISPOSITION files.

**13-REVIEW (`13-REVIEW-DISPOSITION.md`), `open: 0`, 1 fixed and 8 deferred:**

| ID | Disposition | Title |
|---|---|---|
| WR-02 | fixed (`981a89d6`, ledger L-20) | `io.debezium.time.Timestamp` millisecond longs pass through unconverted and are silently misread as seconds |
| WR-01 | deferred | Empty-CDC-record skip guard exists only on the metadata-free route; the `bypassRowBinary=true` JSON route still emits all-defaults rows |
| WR-03 | deferred | The "not nullable and no value" failure throws a message-less `RuntimeException`, and this phase makes it reachable for `Array(Tuple)` |
| WR-04 | deferred | Null-deref on `e.getMessage()` in the `doInsertRawBinary` catch block can mask the real failure (pre-existing, in a high-risk file) |
| WR-05 | deferred ("WARN added (D-02); root cause backlog 999.1") | Nested nulls in `Array(Nullable(Enum))`, `Map(K, Nullable(Enum))` and `Array(Nullable(Tuple))` now fail the whole batch, including arrays that are entirely null |
| IN-01 | deferred | Nested-null check logic is duplicated three times (ARRAY, MAP, TUPLE) |
| IN-02 | deferred | Expected data-quality failures are logged at ERROR with a full stack trace, once per recursion level |
| IN-03 | deferred | `NESTED_NULL_*` constants and helpers are declared mid-class, between unrelated methods |
| IN-04 | deferred | `writeRecordsJson` now has three nested serialization strategies in one loop body |

**14-REVIEW (`14-REVIEW-DISPOSITION.md`), `open: 0`, 6 deferred:**

| ID | Title |
|---|---|
| WR-01 | LZ4 test never proves the response leg was compressed |
| WR-02 | Fork-count step reports a recomputed value, not the one Gradle uses |
| IN-01 | Concurrency group still dead-codes the pull_request branch and stays SHA-keyed |
| IN-02 | Test duplicates constants already exported by production code |
| IN-03 | Jar-origin check is a substring match on a filesystem path |
| IN-04 | Payload length is asserted inside the data-building loop |

**15-02 review of the D-01/D-02 commits (`15-REVIEW.md`), 0 Critical, 0 Warning, 6 Info, all deferred (the outcome table has zero rows):**

| ID | Title |
|---|---|
| IN-01 | The WARN fires on routes where the defect cannot occur |
| IN-02 | The V1 describe hook is not exercised by the V2-only target run |
| IN-03 | The warned-key set never evicts and its key is delimiter-ambiguous |
| IN-04 | The WARN wording is not pinned by a test |
| IN-05 | New constants sit in the middle of the class |
| IN-06 | Edge-value pins cover the mantissa-less scientific form only |

**Backlog:** 999.1 "Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple" (the root cause behind 13-REVIEW WR-05; values in such slots can be stored as NULL or shifted with no error) and 999.2 "Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4" (test-only, not shipped).

**Operator-visible open items** (in FORK-DELTA "Known costs and open items"): 13-REVIEW WR-01's all-defaults rows on the `bypassRowBinary=true` route and WR-03's message-less exception, plus the Nullable Enum/Tuple limitation.

### 7. Release documents

`git diff --stat c61d90ce5dfb32ea5876dffacc8bb54d8846739a ae8520ed -- CHANGELOG.md docs/`:

```
 CHANGELOG.md               |  85 ++++++++++++
 docs/FORK-DELTA.md         | 118 ++++++++++-------
 docs/ROLLOUT-v1.7.0_gg1.md | 312 +++++++++++++++++++++++++++++++++++++++++++++
 3 files changed, 466 insertions(+), 49 deletions(-)
```

- **CHANGELOG** adds a `# 1.7.0_gg1, 2026-10-10` section above upstream's `1.7.0`, listing only the fork's changes since `v1.6.0_gg1`: R-01 (explicit `insertFormat=json` now lands Struct/CDC rows), the move of explicit-json plus `bypassRowBinary=true` onto the metadata-free writer, WR-02 strictness (nested null throws `DataException`), the `Int256`/`UInt256` message, the new Nullable Enum/Tuple startup WARN, compression retired onto `setCompressionMethod`, the 13-REVIEW WR-02 Debezium Timestamp/NanoTimestamp fix, and the lz4 `1.12.0`, Jackson `2.22.3`, okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0` bumps.
- **FORK-DELTA** is rewritten for the v1.7.0 tree: `VERSION` is `v1.7.0_gg1`, D-17 and R-01 are retired from the open list, the LZ4 rule and `1.12.0` are stated, the Nullable-Enum/Tuple WARN and nested-null split rows are added, Known costs and open items carries the operator-visible deferred findings, and the Named skip inventory (18 names) is the one the gate runs just matched.
- **Rollout doc** `docs/ROLLOUT-v1.7.0_gg1.md` has eight items, each with What changed, Am I affected and What to do: the exactly-once pre-upgrade check, the R-01 backfill, JSON-route type notes, nested-null and the Enum/Tuple limitation, TYPE-08 and `Array(Tuple)`, compression, JMX MBean and client name, and rollback. It names no internal host, connector or production version.

### 8. Publication safety

- `.planning/.gsd-ws-arg` is untracked, absent from X's tree (`git ls-tree -r`), and no commit in `c61d90ce..X` touches it. `sync/v1.6.0` is an ancestor of X.
- Origin's tags are exactly `pre-v1.6.0-sync` and `v1.6.0_gg1` (byte-identical before and after the push). The merge step will push only `v1.7.0_gg1` (to be created on the merge commit) and `pre-v1.7.0-sync` (local tag, resolves to `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`).
- Local-only tags that will **not** be pushed: 67 (`git tag -l` minus origin's list minus `pre-v1.7.0-sync`): 65 upstream-style `v0.0.x`, `v1.x` tags (including `v1.6.0` and `v1.7.0`), plus `v1.2.9_gg4-tests` and `v1.6.0_gg1-sync`.
- `git config --get push.followTags` is unset. `origin/custom_build` is still `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`; `origin/main` is still `bec167d85918e77c11621063000acc6e031ae2fa`.
- Pre-push secret scan over the 44 files changed in `54b5ce2f..ae8520ed`: no key header or token pattern, no URL with credentials, and two assignment hits that are variable-to-variable code (`ClickHouseHelperClient.java` lines 91 and 863), not secrets.

### 9. Shape of the publication

- X = `ae8520ed2ae848cb131e22a729ca670571782d55`, on `origin/sync/v1.7.0`.
- `git rev-list --count c61d90ce..X` = **172** commits, of which **120** touch `.planning/` (and 49 touch a path outside `.planning/`).
- The merge will be a non-force `--no-ff` merge of X by hash into `custom_build`, done in a throwaway worktree so the dirty `.planning/state.json` and the main tree stay untouched. The local planning-tail commits after X (Fact 39, this SUMMARY, STATE) stay unpushed (D-14).

### 10. Environment

- Run date: 2026-10-10. `ubuntu-latest` migrates to Ubuntu 26 from 2026-10-19 (CI annotation), so this rehearsal ran on the current image and a later publication run may run on a different one.
- Docker for local runs is colima profile `vz`, running again at the end of this plan. GitHub was read through the unauthenticated REST API (49 of 60 requests left at the end of the plan).

## Files Created/Modified

- `.planning/MERGE-CHECKLIST-v1.7.0.md` - Recorded Fact 38 (local gate, in X) and Recorded Fact 39 (rehearsal CI, local tail)
- `.planning/phases/15-suite-gate-publication-rollout/15-07-SUMMARY.md` - this summary and the approval bundle

## Decisions Made

- No CHANGELOG date commit was needed; the heading already read today's UTC date.
- Hits of the credential-shaped assignment scan were classified, not suppressed: both are variable assignments in `ClickHouseHelperClient`.

## Deviations from Plan

None - plan executed exactly as written.

Two tooling notes, neither a deviation: the Bash tool's default shell is zsh, which does not word-split an unquoted variable, so the secret scan and the Task 3 verify block were run as `bash` scripts (macOS bash 3.2 has no `mapfile`; a `while read` loop was used instead). The first scan attempt fell back to the whole tree because of that and was discarded; the recorded scan is the scoped 44-file one.

## Issues Encountered

None.

## Known Stubs

None. This plan changes no code.

## Threat Flags

None. The plan introduced no new network endpoint, auth path or file access surface.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 15-08 can present the approval bundle above and, on approval, merge X by hash. Before merging it should re-assert `git ls-remote --heads origin sync/v1.7.0` equals `ae8520ed2ae848cb131e22a729ca670571782d55`.
- Nothing blocks the gate. The two below-threshold audit items (T-13-19, T-14-16) and the deferred findings are for the user to weigh.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*

## Self-Check: PASSED

Verified: commits ae8520ed, bbf08ff3 and T0 6e25bb1b are on the branch; Recorded Facts 38 and 39 exist; commits since T0 = 2; origin sync/v1.7.0 = X; bundle items 1 to 10 present; Docker running.
