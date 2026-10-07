---
phase: 11-full-suite-ci-publication
plan: 05
subsystem: ci
tags: [github-actions, ci-rehearsal, scratch-branch, rest-api, evidence-bundle, publication-gate]

requires:
  - phase: 11-full-suite-ci-publication
    provides: "11-01 debt closure, 11-02 CI pin removal, 11-03 FORK-DELTA, 11-04 local gate, 11-08 LZ4 network-compression fix (D-17) - the tree this plan rehearses"
provides:
  - "sync/v1.6.0 on origin at 64289171, pushed without force, rehearsing the exact tree 11-04 gated"
  - "CI-06 locator evidence: head_sha plus workflow path returned exactly one run (id 37617508949), conclusion success"
  - "CI-05 evidence: wall-clock 350 s (5.8 min) against a 1350 s threshold derived from timeout-minutes 45; jacoco-coverage-reports artifact 816160 bytes"
  - "D-03's six-item evidence bundle plus two additional facts, every item re-derived by command, for plan 11-06's gate"
  - "Three findings for the gate: stale FORK-DELTA raw numbers, an incomplete knowingly-open list across sources, and a scratch-tip-versus-local-HEAD relationship"
affects: [11-06, 11-07]

actuals:
  tokens: 0
  tasks: 2
  commits: 0

plan_head_before: 64289171eae5f21c2b5af6e8b94449f968d152b5
plan_head_after: 64289171eae5f21c2b5af6e8b94449f968d152b5
commits: 0

tech-stack:
  added: []
  patterns:
    - "CI run located by head_sha AND workflow path with an asserted cardinality of exactly one; a transient zero is polled, never read as a result"
    - "Wall-clock measured two ways (run-level and job-level) against a threshold derived from the workflow file at execution time"

key-files:
  created: []
  modified: []

key-decisions:
  - "No file changed and no commit made by Tasks 1 and 2: the only mutation is the new remote ref refs/heads/sync/v1.6.0, so commits: 0 is the measured and legitimate count."
  - "The local sync/v1.6.0 will be ahead of origin/sync/v1.6.0 by this plan's own docs commits (.planning only); they are deliberately NOT pushed, because a second push would start a second run on a SHA the plan never located and would blur the one-run-one-SHA evidence."
  - "The CI server version could not be read (job logs and artifact downloads need authentication); it is inferred from the image digest instead and labelled as an inference."

requirements-completed: [CI-05, CI-06]

coverage:
  - id: D1
    description: "sync/v1.6.0 pushed to origin without force; ls-remote SHA equals the local ref"
    requirement: CI-06
    verification:
      - kind: other
        ref: "git push origin sync/v1.6.0 (new branch); verify command printed SCRATCH_PUSH_CONFIRMED with LOCAL=REMOTE=64289171eae5f21c2b5af6e8b94449f968d152b5"
        status: pass
    human_judgment: false
  - id: D2
    description: "Run located by head_sha plus path .github/workflows/tests.yaml with exactly one match; conclusion success"
    requirement: CI-06
    verification:
      - kind: other
        ref: "GitHub REST actions/runs?head_sha=...: MATCHING_RUNS=1 (total_count=1), id 37617508949, SCRATCH_CI_GREEN"
        status: pass
    human_judgment: false
  - id: D3
    description: "CI wall-clock 350 s (5.8 min) run-level and 345 s job-level, under the 1350 s (22.5 min) threshold derived from timeout-minutes 45"
    requirement: CI-05
    verification:
      - kind: other
        ref: "CI_WALLCLOCK_UNDER_THRESHOLD; run-level run_started_at->updated_at and job-level started_at->completed_at both read from the REST API"
        status: pass
    human_judgment: false
  - id: D4
    description: "jacoco-coverage-reports artifact exists on the run with non-zero size (816160 bytes)"
    requirement: CI-05
    verification:
      - kind: other
        ref: "actions/runs/37617508949/artifacts: COVERAGE_PUBLISHED"
        status: pass
    human_judgment: false
  - id: D5
    description: "D-03 six-item evidence bundle plus two additional facts assembled, each item re-derived by command at execution time"
    verification:
      - kind: other
        ref: "bash /tmp/p11-05-bundle.sh -> BUNDLE_DERIVED; bash /tmp/p11-05-item6.sh -> BUNDLE_ITEM_6_CONSISTENT"
        status: pass
    human_judgment: false
  - id: D6
    description: "The first single-fork execution of this tree is green on CI"
    verification:
      - kind: other
        ref: "run 37617508949 conclusion success on a 2-core ubuntu-latest runner"
        status: pass
    human_judgment: true
    rationale: "That the runner resolved maxParallelForks to 1 is inferred from its core count, not read from a log (job logs need authentication); the green result is measured, the topology is not"

duration: 11min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 05: Scratch-Branch CI Rehearsal and D-03 Evidence Bundle Summary

**`sync/v1.6.0` was pushed to origin and its single `tests.yaml` run (id 37617508949) finished `success` in 350 s (5.8 min) against a 22.5-minute threshold, with a 816160-byte `jacoco-coverage-reports` artifact; the six-item D-03 bundle is re-derived below and surfaces three findings for plan 11-06's gate, none of them a blocker.**

## Performance

- **Duration:** about 11 min wall-clock, dominated by the 5.8-minute CI run
- **Started:** about 2026-10-07T11:55Z
- **Completed:** 2026-10-07T12:07Z
- **Tasks:** 2
- **Files modified:** 0 (one remote ref created)

## This run's firsts (stated explicitly, per the plan)

- **First CI run to exercise the code defaults rather than job-environment pins.** Plan 11-02 removed `CLICKHOUSE_VERSION` and `CLIENT_VERSION` from the job environment; this run resolved both from the code defaults (`latest`, V2). Confirmed by the workflow file at the pushed SHA: the `Run tests` step is `run: ./gradlew test` with no `env:` block.
- **First CI run over the merged tree.** `origin` had never seen `sync/v1.6.0`; the run covers the 581-commit upstream merge plus Phases 7 to 11 on top of it.
- **First CI run at what should be single-fork topology.** Plan 11-04 found the local gate ran 6 forks (12 cores), so it did not measure the single-fork shape. A 2-core `ubuntu-latest` runner resolves `maxParallelForks = availableProcessors() / 2` to 1. The run is green. The core count is inferred, not read from a log.

## Task 1: End-to-end rehearsal (tracer)

### Secret scan, run before the push (T-11-01)

Scope: every one of the **506** tracked files at HEAD `64289171`, with `.planning/` and `docs/FORK-DELTA.md` included, plus the added-lines history of the commits about to be published.

| Check | Result |
|---|---|
| Private-key headers, AWS `AKIA`, GitHub `ghp_`/`github_pat_`, Slack `xox*`, `sk-` keys, Google `AIza`, JWT-shaped tokens (`git grep -E` over HEAD) | 0 matches |
| `password`/`secret`/`token`/`api_key` assignments with a literal value, outside `src/main` | 1 match: `MaskTest.java:14` `"DATBwWKXvYQnce"`. It is the masking-test fixture and is **present byte-for-byte in upstream `v1.6.0`** (`git grep -c DATBwWKXvYQnce v1.6.0 -- src/test` = 1), so it is already public upstream and not a fork credential |
| Same assignment pattern in `src/main` | only constant names (`PASSWORD = "password"`) and `getDeduplicationToken()` calls |
| Tracked key/keystore/env/credentials files (`*.pem`, `*.key`, `*.p12`, `*.jks`, `.env`, `.npmrc`, `id_rsa`) | 0 files |
| Connection strings, internal-looking hosts, private IP ranges, email addresses | only `localhost` health URLs, `example.com` fixtures, upstream's `security@clickhouse.com` and `ai@clickhouse.com`, and `git@github.com` remote spellings in `.planning/` |
| `git log origin/custom_build..HEAD -G<credential regex>` | 0 commits |
| `docs/FORK-DELTA.md` and `.planning/PROJECT.md`/`STATE.md` for non-GitHub/ClickHouse/Apache/Confluent URLs | none |

Nothing credential-shaped was found; the push proceeded.

### Push and SHA confirmation

- `git push origin sync/v1.6.0` printed `* [new branch] sync/v1.6.0 -> sync/v1.6.0`. No force flag. No other ref was pushed: `git ls-remote --heads origin` afterwards lists `custom_build @ a295af8a` (unchanged), `main @ bec167d8` (unchanged) and the new `sync/v1.6.0`; `git ls-remote --tags origin` is empty.
- Local `git rev-parse sync/v1.6.0` = `ls-remote` value = **`64289171eae5f21c2b5af6e8b94449f968d152b5`**. `SCRATCH_PUSH_CONFIRMED` printed.
- The precondition held: no non-`.planning` file differs between 11-04's recorded `382c9e71` and this push (`git diff --stat 382c9e71 HEAD -- . ':!.planning'` is empty); the two commits since are `docs(11-04)` records.

### The run

| Field | Value |
|---|---|
| Repository | `ne1r0n/clickhouse-kafka-connect` |
| Run `id` | **37617508949** |
| `html_url` | https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37617508949 |
| `run_number` / `run_attempt` / `event` | 33 / 1 / `push` |
| `head_sha` | `64289171eae5f21c2b5af6e8b94449f968d152b5` |
| Locator | `actions/runs?head_sha=<SHA>`, filtered to `path == .github/workflows/tests.yaml` |
| **Matching runs (cardinality)** | **1** (`total_count` 1, matching 1) |
| `status` / **`conclusion`** | `completed` / **`success`** |

**Locator note.** One poll (12:00:14Z) returned a match count of 0 while the run was in progress, between two polls that returned 1. A zero was treated as not-yet-registered/API-transient, polled again, and never read as a result. The final count, asserted by three separate calls (poll, verify and a fresh re-read at SUMMARY time), is exactly 1.

### CI-05 measurement

| Measure | Seconds | Minutes |
|---|---|---|
| Run-level: `run_started_at` 11:56:33Z to `updated_at` 12:02:23Z | **350** | **5.8** |
| Job-level: `started_at` 11:56:37Z to `completed_at` 12:02:22Z | **345** | **5.75** |
| Discrepancy | 5 | 0.08 (run-level includes ~4 s of queueing before the job starts and a ~1 s tail) |
| `Run tests` step alone | 326 | 5.4 |
| `timeout-minutes` read from `.github/workflows/tests.yaml` at execution time | 45 | |
| Threshold (half of it) | **1350** | **22.5** |

**Verdict: UNDER the threshold** (350 s is 25.9 percent of 1350 s; 12.9 percent of the 45-minute budget). `CI_WALLCLOCK_UNDER_THRESHOLD` printed. The criterion could have failed and did not. Per-step: `Set up job` 4 s, `checkout` 2 s, `Set up JDK 17` 0 s, `Set up Gradle` 2 s, `Run tests` 326 s, `Generate coverage summary` 0 s, `Upload JaCoCo` 2 s, `Upload test reports` 4 s. For comparison, plan 11-04's local 6-fork run took 187 s, so the single-fork 2-core runner took 1.8 times as long, not the order-of-magnitude slowdown the plan feared.

### Coverage artifact

`actions/runs/37617508949/artifacts`:

| Artifact | Bytes | Expired |
|---|---|---|
| **`jacoco-coverage-reports`** | **816160** | false |
| `gradle-test-reports` | 23131778 | false |

`COVERAGE_PUBLISHED` printed (name matches, size greater than 0, count exactly 1). Contents were not downloaded: artifact download needs authentication (HTTP 401 unauthenticated), so "non-zero size" is the measurable claim, not "contains both XML reports".

### What the unauthenticated API could not show

- **Job logs return HTTP 403** and artifact zips HTTP 401. CI's own `Tests summary:` line, per-class counts and ClickHouse server version are therefore **not readable** here. The only fact that the run executed the suite to completion is `Run tests` step `conclusion: success` after 326 s of work.
- **ClickHouse server version, inferred not read.** Docker Hub's `clickhouse/clickhouse-server:latest` was last updated `2026-10-06T18:14:29Z` with digest `sha256:5d6b622bbb109f40d12333187e44ae80864cf72aa71ae9a4704a53496615e941`. The image on this machine, which plan 11-04 recorded as ClickHouse 26.9.12.8, has the **same** repo digest. The CI run started `2026-10-07T11:56Z`, after that update, so the runner's floating `latest` pull should have resolved to the same image, hence 26.9.12.8, and therefore exercised the 11-08 LZ4 pin on the same server generation. This is an inference from a digest, not a log line.
- One check-run annotation: `Node.js 20 is deprecated... actions/checkout@v4, actions/upload-artifact@v4 are being forced to run on Node.js 24`. A warning, not a failure; the workflow still passes. Worth knowing for a future workflow edit, out of scope here.

### Task 1 verify commands, as written

All four printed their markers: `SCRATCH_PUSH_CONFIRMED`, `SCRATCH_CI_GREEN` (preceded by `MATCHING_RUNS=1`), `CI_WALLCLOCK_UNDER_THRESHOLD` (`CI_WALLCLOCK_SECONDS=350 MINUTES=5.8 THRESHOLD_SECONDS=1350`), `COVERAGE_PUBLISHED`.

## Task 2: D-03 evidence bundle

<!-- BUNDLE-BEGIN: plan 11-06's checkpoint quotes this section verbatim -->

### D-03 EVIDENCE BUNDLE (every item re-derived by command at 2026-10-07T12:0xZ; none cited from a planning document)

**Tree under evidence:** `sync/v1.6.0` @ `64289171eae5f21c2b5af6e8b94449f968d152b5`, which equals `origin/sync/v1.6.0`.

**Item 1 - local run totals and skip count** (re-read from `build/test-results/test/TEST-*.xml`, 59 files, still on disk, written 2026-10-07 14:51 local = 11:51Z by plan 11-04's `--rerun`):

```
$ awk over TEST-*.xml <testsuite> attributes
TOTALS tests=663 failures=0 errors=0 skipped=18 succeeded=645
$ python3 -I ... testcase elements carrying <skipped>
18 skipped: ClickHouseSinkTaskTest 1, ClickHouseSinkTaskWithSchemaTest 13,
            InsertFormatPerformanceTest 1, JsonColumnTest 1, ConvertToMapOrJsonPerformanceTestJson 2
```

Skip-diff against Phase 10's named 18, in the form 11-04 recorded it (sorted `(class, method)` pairs): the 18 pairs re-derived now are the same 18 pairs 11-04 stored in `/tmp/p11-skips-now.txt` (`clientNameTest()`; `schemaWithNestedTupleMapArrayAndVariant()`, `schemaWithTupleOfMapsWithVariantTest()`; eleven `exactlyOnceStateMismatchTest` cases `[1]` to `[11]`; `compareInsertPerformance()`; `compareAllConfigurations()`; `testWideStructPerformance()`, `testDeepNestedStructPerformance()`): **identical set, no new skip**. Docker-free `testUnit` (11-04, daemon confirmed down): 331 tests, 0 failed, 2 skipped. The 663 against Phase 10's 654 is the nine `NetworkCompressionPin*` methods from plan 11-08.

**Item 2 - the scratch CI run** (fresh API read at SUMMARY time):

```
FRESH count_all=1 matching=1 id=37617508949 conclusion=success wall_s=350
URL  https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37617508949
conclusion success | 5.8 min (run-level), 5.75 min (job-level) | threshold 22.5 min | verdict UNDER
```

**Item 3 - `MERGE-CHECKLIST.md` at zero open rows:**

```
OPEN_ROWS=0
DONE_ROWS=48
```

**Item 4 - invariant script, unattended:**

```
$ bash .planning/scripts/verify-merge-invariants.sh 673d29c3d5dc24b97de3b022acdfe473e1a5493c
INVARIANT_EXIT=0
```

**Item 5 - fork-delta figures** (four `git diff --shortstat` commands, twelve numbers, measured now):

| Scope | Endpoints | Files | Insertions | Deletions |
|---|---|---|---|---|
| `src build.gradle.kts gradle .github` | `v1.2.9` to `pre-v1.6.0-sync` (before) | 93 | 8401 | 423 |
| `src build.gradle.kts gradle .github` | `v1.6.0` to `sync/v1.6.0` (after) | **84** | **9476** | **451** |
| `src/main` | `v1.2.9` to `pre-v1.6.0-sync` (before) | 32 | 1308 | 139 |
| `src/main` | `v1.6.0` to `sync/v1.6.0` (after) | 30 | **1368** | **98** |

**FINDING F-1 (stale published figures).** `docs/FORK-DELTA.md` lines 92 to 95 publish the "after" rows as `82 / 9255 / 450` and `30 / 1358 / 97`. Today's measurement is `84 / 9476 / 451` and `30 / 1368 / 98`. The "before" rows still match exactly. The drift is plan 11-08, which landed after plan 11-03 measured: two new test files (`+2` files, `+221` insertions at the wider scope), one `src/main` fix (`+10` insertions, `+1` deletion in `ClickHouseHelperClient.java`, confirmed by `git diff --shortstat 87fa1e78^ 87fa1e78 -- src/main` = `1 file changed, 10 insertions(+), 1 deletion(-)`), and one deletion elsewhere. 11-08 added an eighth-behaviour paragraph to FORK-DELTA but did not refresh the raw-numbers table. The narrative direction ("insertions grew, file count fell") is unchanged and still true, but the document states numbers the tree no longer produces. No file was edited here; the correction is a one-table docs change for the gate to order.

**Item 6 - knowingly-open items being published (D-08):**

```
FORK_DELTA_HAS: WR-01 / extractColumn / USERNAME / JsonColumnTest / bypassRowBinary   (all five)
SECURITY_DOC_PRESENT: 07-SECURITY.md (129 lines), 09-SECURITY.md (128 lines)
BUNDLE_ITEM_6_CONSISTENT
```

The list as published: **R-01** (silent data loss, `insertFormat=json` plus `Struct` plus default `bypassRowBinary`, pinned by `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`); **WR-01 (test harness)** `ClickHouseBase.tearDown()` swallowing a drop failure; the four open review findings `Column.extractColumn` (swapped positional args), `ClickHouseSinkConfig` `USERNAME` default reading `passwordDefault`, `JsonColumnTest` row-count-only assertions, and `cleanupExtraFields` P9-IN-01 operand order.

**FINDING F-2 (three sources do not fully agree).** Compared across `docs/FORK-DELTA.md` Known costs, `REQUIREMENTS.md` Future Requirements and the sources they cite:

1. **`REQUIREMENTS.md` Future Requirements names none of the four review findings and not the harness WR-01.** It lists R-01 fail-fast, WR-01 (the *Map null* item, a different thing under the same label), WR-02, WR-03, P3-WR-01..03, P4-WR-01 and P4-IN-01. FORK-DELTA is open about this: it says the four findings and the harness WR-01 are "carried as an open item in `.planning/STATE.md`", not in Future Requirements. So the claim "stays a Future Requirement" in CONTEXT D-08 is not literally true for five of the seven items; they live in `STATE.md`.
2. **`07-REVIEW.md` has more open items than D-08's four.** Its register is WR-01 (`extractColumn`), WR-02 (`USERNAME`), WR-03 (`validateDataSchema` NPE), IN-01 (commented-out `async_insert` at `ClickHouseSinkConfig.java:246`, still present), IN-02 (`versionV2()` comment at `ClickHouseHelperClient.java:271`) and IN-03 (`JsonColumnTest`). WR-03 is fixed in the current code (a `objSchema == null` guard at `ClickHouseWriter.java:338-345` carries the label `WR-03`). **IN-01 and IN-02 are open and appear in neither FORK-DELTA nor REQUIREMENTS** (the string `IN-01` in FORK-DELTA is `P9-IN-01`; `IN-02` is absent). Both are comment-only and low severity. `STATE.md`'s own headline says "3 Warning + 3 Info" but names only three items and files `JsonColumnTest` under Warning although the review labels it IN-03.
3. **Net:** the publication list is correct for everything with a behavioural consequence; it omits two cosmetic comment-level Infos. A gate decision, not a blocker.

**Additional fact A - open-threat counts** (`grep`/`awk` over each register, re-derived now):

| Document | Threat rows | Closed | Open | `threats_open` (at or above `high`) | Dispositions |
|---|---|---|---|---|---|
| `07-SECURITY.md` | 23 | 23 | **0** | **0** | 13 mitigate, 10 accept |
| `09-SECURITY.md` | 17 | 17 | **0** | **0** | 13 mitigate, 4 accept |

Two `high` rows are `accept`: `T-07-SC` and `T-09-03` (R-01). Both carry a recorded prior disposition; plan 11-01 recorded them as closed accepted risks rather than escalating, and flagged that as an interpretive call (`human_judgment: true` in its coverage block).

**Additional fact B - commits the publication push will carry:**

```
$ git fetch origin custom_build --quiet
$ git rev-list --count origin/custom_build..sync/v1.6.0
COMMITS_TO_BE_PUBLISHED=750
```

**750 is not the 697 the ROADMAP names, and not the 699 the plan recorded at plan time.** Three components: (a) 697 at discussion time and 699 at plan time (`git rev-list --left-right --count custom_build...sync/v1.6.0` returned `0 697` then `0 699`); (b) that figure is now **722** (`0 722`) because Phase 11's own plans 11-01 through 11-04 and 11-08 added commits on `sync/v1.6.0` after those readings; (c) **28** more because local `custom_build` was already 28 commits ahead of `origin/custom_build` (`git rev-list --count origin/custom_build..custom_build` = 28, `origin/custom_build` = `a295af8a`, local `custom_build` = `6ac2d20c`), and 722 + 28 = 750. Plan 11-06's own docs commits will add more before the merge, so the count at the push will be higher still and must be re-derived there.

<!-- BUNDLE-END -->

## Local-versus-origin SHA relationship

- `origin/sync/v1.6.0` = `64289171eae5f21c2b5af6e8b94449f968d152b5` = local `sync/v1.6.0` at the moment of the push and at every verify.
- After this plan's SUMMARY commit, local `sync/v1.6.0` is **ahead of origin by this plan's docs commit(s)** and `origin/sync/v1.6.0` stays at `64289171`. Those commits touch only `.planning/` (`git diff --stat 64289171 HEAD -- . ':!.planning'` was empty at SUMMARY time and must stay empty).
- They are **not pushed**, by choice. The run located above is the run for `64289171`; pushing the SUMMARY would start a second run on a SHA the plan did not locate and would break the one-run-one-SHA argument. The cost is that plan 11-06's merge commit will have a tree differing from the CI-proven scratch tip in `.planning/` only. The plan's key link says "byte-identical", which is true for everything outside `.planning/` and not literally true of the whole tree; plan 11-06 should assert the tree-equality outside `.planning/` rather than the whole tree.

## Task Commits

None for Tasks 1 and 2. This plan pushed one ref and read an API; no file was modified. `git rev-list --count 64289171..HEAD` returned 0 at SUMMARY-write time. The plan's own SUMMARY commit follows this file.

## Files Created/Modified

None. Scratch evidence outside the tree: `/tmp/p11-05-run.json`, `/tmp/p11-05-jobs.json`, `/tmp/p11-05-scratch.txt`, `/tmp/p11-05-bundle.sh`, `/tmp/p11-05-item6.sh`.

## Decisions Made

- Evidence plan: nothing committed by the tasks, nothing re-baselined, no `src/main` edit, only `sync/v1.6.0` pushed, no force flag.
- Evidence commits stay local (rationale in the section above).
- The threshold verdict, the three findings and the inference about the server version are reported as stated, not softened.

## Deviations from Plan

None - plan executed exactly as written. Two small environment notes, neither a deviation: the shell is zsh, so an unquoted `echo ======` separator in a multi-command verify block failed on the first attempt and the block was re-run in a clean form (no state was changed by the failed attempt, and the push had already been confirmed); and the plan's step 3 `Poll to completion` was satisfied in 6 minutes rather than the 45-minute bound.

## Authentication Gates

None. All requests were unauthenticated and within the rate limit (about 20 requests used of 60 per hour). Log and artifact-content access returning 403/401 is the documented consequence of D-11's no-`gh` decision, not a gate.

## Issues Encountered

- **A transient zero in the locator** at 12:00:14Z (one of five polls) while the run was in progress; handled as the plan prescribes (poll, do not conclude).
- **CI's own test totals and server version are not readable** without authentication; reported as inference where inferred.
- **F-1 and F-2** above are findings for plan 11-06's gate, not blockers: no criterion of this plan fails on them.

## Known Stubs

None.

## Threat Flags

None. The only new surface is the public scratch branch, which is the plan's own subject (T-11-01 mitigated by the pre-push scan above).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 11-06 can quote the delimited bundle verbatim. Open items for its gate: **F-1** (FORK-DELTA raw numbers stale by 11-08; a one-table docs correction), **F-2** (IN-01/IN-02 and the Future-Requirements/STATE.md split), the 750-commit count, and the tree-equality wording (assert outside `.planning/`).
- Reversal of this plan is one command: `git push origin --delete sync/v1.6.0`. Do not run it before plan 11-06 has merged; it is part of 11-06's own cleanup.
- The 22.5-minute threshold passed with a factor-of-four margin (5.8 min), so the `maxParallelForks` and `timeout-minutes` deferrals stand.

## Self-Check: PASSED

- `git ls-remote --heads origin sync/v1.6.0` = `64289171eae5f21c2b5af6e8b94449f968d152b5` (FOUND); `git merge-base --is-ancestor 64289171 HEAD` holds (HEAD is that commit).
- `git rev-list --count 64289171..HEAD` = 0, consistent with `commits: 0` and no code changes; `git diff --stat 64289171 HEAD -- . ':!.planning'` is empty.
- Run 37617508949 re-read fresh at SUMMARY time: `matching=1`, `conclusion=success`, `wall_s=350`.
- Task 1's four verify commands and Task 2's two verify commands each printed their pass marker.
- No ref other than `sync/v1.6.0` was pushed; `custom_build` and `main` on origin are unchanged and `origin` still has zero tags.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
