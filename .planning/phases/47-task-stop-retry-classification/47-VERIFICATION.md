---
phase: 47-task-stop-retry-classification
verified: 2026-11-06T00:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/47-task-stop-retry-classification/47-01-PLAN.md"
  - ".planning/phases/47-task-stop-retry-classification/47-01-SUMMARY.md"
  - ".planning/phases/47-task-stop-retry-classification/47-02-PLAN.md"
  - ".planning/phases/47-task-stop-retry-classification/47-02-SUMMARY.md"
  - ".planning/phases/47-task-stop-retry-classification/47-03-PLAN.md"
  - ".planning/phases/47-task-stop-retry-classification/47-03-SUMMARY.md"
  - ".planning/phases/47-task-stop-retry-classification/47-04-PLAN.md"
  - ".planning/phases/47-task-stop-retry-classification/47-04-SUMMARY.md"
  - ".planning/phases/47-task-stop-retry-classification/47-05-SUMMARY.md"
  - "CHANGELOG.md"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java"
covered_digest: "v4:sha256:ed39aeaa1f47e865b385248aea187d9b8837e8f8386e3982912a70ff0fab7c36"
behavior_unverified: 0
overrides_applied: 0
coincidental_reliance_items:
  - truth: "ClickHouseSinkTask.stop() stops the proxy task (writer, MBean, state provider) when the strategy's stop() throws"
    reason: fixture-only
    harden: "ClickHouseSinkTaskStopUnitTest asserts MBEAN_GONE against a second SinkTaskStatistics the test builds itself, with no assertTrue(mBeanRegistered()) before stop(). It holds because both objects compute the same MBean name today. The writer-stop and state-close assertions do not share this weakness and were seen red on the base. This is open review item IN-01 in 47-REVIEW-DISPOSITION.md. Add the precondition, or expose the proxy's own statistics name."
---

# Phase 47: Task Stop & Retry Classification Verification Report

**Phase Goal:** A stopping task reaches every cleanup step even when one delivery strategy throws, and the published CHANGELOG stops claiming more than the code does. An operator knows what happens to a retriable `DataException`: either Connect stops redelivering it after a bounded budget, or the endless redelivery is pinned and documented as a limit. FUT-05 ends with a recorded decision instead of staying open.
**Verified:** 2026-11-06
**Status:** passed
**Re-verification:** No, initial verification
**Tip verified:** `7183ed81` on `sync/v1.7.0`. The working tree has only the unstaged `.planning/config.json` and `.planning/state.json`, which this verification did not touch.

The SUMMARY and EVIDENCE claims were treated as unproven. Everything below was read from the source or re-run. The Docker-backed full suite was not re-run. Its figures are taken from E-10 and E-15 and marked as recorded.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP criterion) | Status | Evidence |
|---|---------------------------|--------|----------|
| 1 | `ClickHouseSinkTask.stop()` stops the delivery strategy and still reaches the proxy-task cleanup when the strategy's `stop()` throws (`try`/`finally`). The pin uses no Mockito, runs with Docker unreachable, and was seen red on the unchanged tree before the fix. | VERIFIED | See below. |
| 2 | The CHANGELOG `stop()` Bug Fixes and Known Issues bullets describe the code's behaviour and claim no more than the LIF-01 pin proves. | VERIFIED | See below. |
| 3 | Through `ProxySinkTask.put` under both `errors.tolerance` values, each retriable shape either has a bounded budget or its unbounded redelivery is pinned and documented as a limit in FORK-DELTA. | VERIFIED (record branch) | See below. |
| 4 | FUT-05 is closed, the start-up WARN stays, no validator was added without the user's words, and the decision and its reason are recorded. FORK-DELTA no longer lists FUT-05 as open. | VERIFIED | See below. |

**Score:** 4/4 truths verified, 0 present but behavior-unverified.

#### Truth 1: LIF-01, `stop()` try/finally

- **Code.** `git diff 427f30b4 HEAD -- src/main` touches one file, `ClickHouseSinkTask.java`, with +8/-5. The only change is the body of `stop()`. `deliveryStrategy.stop()` now sits in a `try`, and `proxySinkTask.stop()` in the `finally`. There is no catch and no new method. `ProxySinkTask`, `Processing`, `KeeperStateProvider`, `Utils`, `ClickHouseSinkConfig` and the strategies are not in the diff.
- **Behavior-dependent, so I ran a behavioral test.** The truth is a cleanup-ordering invariant.
  - `ClickHouseSinkTaskStopUnitTest` has 6 cases. It sets the private `proxySinkTask` and `deliveryStrategy` fields by reflection and uses stub strategies. There are 0 Mockito references.
  - It asserts that the writer stopped once, the state provider closed once, the MBean was unregistered, the strategy stopped first (`writerStopsSeen == 0`), and `assertSame` that the strategy's own exception reaches the caller.
  - Run on `HEAD` with `DOCKER_HOST=unix:///nonexistent.sock` and `env -u TESTCONTAINERS_HOST_OVERRIDE`: 6/6 pass. Related classes also pass: `ProxySinkTaskStopUnitTest` 4/4, `UnvettedRetriableCodesWarnUnitTest` 21/21, `AdditionalRetriableErrorCodesPinUnitTest` 19/19, `WrappedDataExceptionClassificationPinUnitTest` 14/14, `RetriableRedeliveryLimitPinUnitTest` 108/108.
- **Red on the unchanged tree, re-confirmed independently.**
  - I made a detached worktree at `a7258507`, the evidence commit that precedes the fix. Its `src/main` equals base `427f30b4` (`git diff --quiet` was clean).
  - The pin was not committed at that SHA, which is by design: it was committed together with the fix. I copied the HEAD pin into the worktree.
  - Result: 6 tests, 2 failed, both with `AssertionFailedError: the writer must have been stopped once ==> expected: <1> but was: <0>`. The failures are `strategyStopFailureStillStopsTheProxyTask` and `bothStopsRunWhenTheStrategyAndTheProxyTaskThrow`. The other 4 cases passed on the unchanged tree. This matches E-02.
  - The worktree was removed and pruned. `git worktree list` shows only the main tree.
- **Order of recorded evidence.** The red record and the expected-added list (`a7258507`, E-01 and E-02) precede the fix commit `796c75fa` (E-03) in history.
- **Advisory (not a gap).** The MBean-gone assertion has no precondition and was never evaluated red, because the writer assertion fails first. See `coincidental_reliance_items` and review item IN-01, which is open and an Info.

#### Truth 2: LIF-02, CHANGELOG claims against the pin

I read the `# 1.7.0_gg8, Unreleased` section (lines 1-25) sentence by sentence.

| CHANGELOG / FORK-DELTA item 13 claim | Backed by | Exceeds the pin? |
|---|---|---|
| `stop()` stops the proxy task when the strategy's `stop()` throws | red-then-green pin cases 1 and 2 | No |
| When only the strategy throws, its exception is the one `stop()` throws | `assertSame(strategy.failure, thrown)` | No |
| With the three shipped strategies, `stop()` returns normally and stops the proxy task, as before | parameterized case, green on the base and after the fix | No |
| gg7's sentence held for `ProxySinkTask.stop()`, not for `ClickHouseSinkTask.stop()` | the red run on the base | No |
| "Kafka Connect only logs a throw from `stop()` as a WARN and does not fail the task, read in its sources" | Labelled as read from sources, not as pinned. I confirmed it in the Kafka 4.1.1 `WorkerSinkTask.java` copy under `/tmp/gsd47/x411` (lines 183-187: `task.stop()` in `try`, `catch (Throwable t) { log.warn("Could not stop task", t); }`). | It is not something the pin proves, but it is honestly scoped and true. It is a statement about Connect, not about the connector. |
| Both-throw case: both stops run and an exception reaches the caller (pinned). The proxy's exception arrives and the strategy's is dropped, "a one-off probe, not a pinned test" | the pin covers "both ran, something thrown"; the rest is labelled a probe (E-04) | The probe claim is labelled "not a pinned test". It also follows from Java `try`/`finally` semantics plus `ProxySinkTask.stop()` rethrowing the first failure, which I read. |
| `close(partitions)` runs the proxy removal before the strategy's `close` with no `finally` | `ClickHouseSinkTask.java:161-169` read directly: confirmed | No. It is a stated residual, not a guarantee. |

- The gg7 section (line 27 onward) is byte-identical to the base `CHANGELOG.md` and to `CHANGELOG.md` at tag `v1.7.0_gg7` (`cmp` clean on both). The numstat since base is 26 insertions and 0 deletions, so the change is a pure prepend.
- `VERSION` and `docs/ROLLOUT-*` are unchanged since base (neither is in the changed-file list).
- FORK-DELTA item 13 follows the same pattern: fixed behaviour, then a "Limit, measured and not pinned" label for the both-throw case, then the `close(partitions)` residual "read from the code and not fixed". It says two of six cases were red, which matches the red run. No claim in it outruns what the pin or its labelled evidence shows.
- Verdict: the claims do not exceed the pin. Where they go beyond it (Connect's log-and-continue, the both-throw outcome), they are labelled by evidence class.

#### Truth 3: RTY-01, unbounded redelivery pinned and documented (user decision U-1)

- `RetriableRedeliveryLimitPinUnitTest` has 108 cases and 0 Mockito references.
  - It has 44 first-put rows: 22 shapes x 2 `errors.tolerance` values, through `ProxySinkTask.put`.
  - It has 64 redelivery rows: 16 retriable shapes x 2 paths (`ProxySinkTask.put` and `ChunkFlusher.putDirect`) x 2 values. Each row makes 50 consecutive calls and asserts a `RetriableException`, `writer.calls == 50` and an empty DLQ.
  - It runs with `errors.retry.timeout=60` and `retryCount=3` set.
- The PIN-02 shapes (plain, retriable-server-code, socket-timeout, non-retriable-code, unknown-host, runtime-over-socket-timeout and conversion) are all present in the new matrix. I compared `WrappedDataExceptionClassificationPinUnitTest` lines 69-131 against the new shape list.
  - The RSK-02 shapes are present: the bare `RetriableException`, the one over a socket timeout, the one over a non-retriable code, the bare retriable server code, and the additional-code shape.
  - The first-put outcomes match the base classification pins. E-05 and E-10 record `classification_pins_equal_base: yes`.
- The pin's strength comes from the budget mutation. E-05 and E-13 record a budget of 5 added in a throwaway worktree. It turned exactly the 64 redelivery rows red and left the 44 first-put rows green. E-13 re-ran it after the cut to 50 redeliveries (review item WR-04). I did not re-run the mutation myself.
- FORK-DELTA item 17 is rewritten as "recorded limit, by decision" and claims only what the evidence labels support.
  - The Connect cadence is stated as measured on the Kafka 3.9 worker (`7.9.7-ccs`) only. The `iteration` and `poll` code is said not to have been compared in 4.1.1 or 4.4.0.
  - The `deliverMessages` body is stated as identical in the three source versions.
  - "A task that throws anything other than a `RetriableException` fails ... measured with a probe task (not this connector)" is separated from "the connector's own path ... read from the code and not measured".
  - The 44-versus-64 split, the 50 redeliveries as "a finite count", and "`exactlyOnce=true` not pinned" are all stated.
  - These scopings address review items WR-03 and WR-05.
- The recorded limits (unbounded `RetriableException` redelivery, no new key) are user decision U-1 and are not counted as gaps.
- `src/main` contains no budget, counter or sleep. The only production diff in the phase is the `stop()` body.

#### Truth 4: RTY-02, FUT-05 closed

- `grep` for FUT-05 in `docs/FORK-DELTA.md` returns 3 hits on 2 lines.
  - Item 8 (line 232) reads "FUT-05 ... is closed by decision (items 17 and 24)". It is removed from the open list, which now reads FUT-01..FUT-04.
  - Item 24 (line 264) says "FUT-05 is closed by decision" and "closed by the user's decision of 2026-11-05". It states the three non-actions (no retry budget, no removal setting, no validator) and the reason.
  - Item 24's reason is that transient is the connector's judgment, so a validator would reject legitimate settings. It also says a rejecting check would break running connectors at the next configuration write or the next task start.
  - No "stays open" or "is not closed" wording remains in FORK-DELTA.
- The WARN is unchanged.
  - `git diff 427f30b4 HEAD --stat` over `ClickHouseSinkConfig.java`, `UnvettedRetriableCodesWarn*.java` and `AdditionalRetriableErrorCodes*.java` is empty.
  - The WARN constant and method in `ClickHouseSinkTask.java` (lines 22-28 and the method) are outside the `stop()` hunk. The hunk is at lines 182+ only.
  - `UnvettedRetriableCodesWarnUnitTest` 21/21 and `AdditionalRetriableErrorCodesPinUnitTest` 19/19 pass on `HEAD`.
- The decision is attributed to the user (`47-CONTEXT.md` U-2, 2026-11-05). No validator or config change was added, which is correct because the user declined one.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/.../ClickHouseSinkTask.java` | `stop()` try/finally only | VERIFIED | +8/-5, only the `stop()` body |
| `src/test/.../ClickHouseSinkTaskStopUnitTest.java` | 6-case Docker-free, no-Mockito pin | VERIFIED | 263 lines, 6/6 green on `HEAD`, 2/6 red on base |
| `src/test/.../RetriableRedeliveryLimitPinUnitTest.java` | 108-row limit pin | VERIFIED | 346 lines, 108/108 green |
| `docs/FORK-DELTA.md` items 8, 13, 17, 24 and the writer-row parenthesis | corrected and closed | VERIFIED | read directly |
| `CHANGELOG.md` gg8 section | new section, gg7 untouched | VERIFIED | 26 insertions, 0 deletions, `cmp`-identical gg7 |
| `47-COMMAND-EVIDENCE.md` E-01..E-15 | all pass | VERIFIED | `index ... 15 --done`: OK. `secrets`: 0 token shapes. |
| `.planning/scripts/fork-anchors.txt` | 218 rows | VERIFIED | `anchors --rows 218`: `rows=218 bad=0` |

`verify.artifacts` flags one item. The `47-02-PLAN.md` artifact pattern `REDELIVERIES = 500` no longer matches because the constant is now 50. This is expected: plan 47-05 cut it deliberately (review item WR-04), E-13 records the re-run mutation, and the pattern in the PLAN is plan-time text. It is not a gap.

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseSinkTask.stop()` | `deliveryStrategy.stop()` then `proxySinkTask.stop()` | `try`/`finally` | WIRED. The stub strategy saw 0 writer stops when it ran, and the writer was stopped once afterwards. |
| `ProxySinkTask.stop()` | writer, MBean, state provider | existing `keepFirst` isolation | WIRED, unchanged. I read lines 76-99. |
| `RetriableRedeliveryLimitPinUnitTest` | `ProxySinkTask.put` and `ChunkFlusher.putDirect` | direct calls with a counting failing writer | WIRED |
| `ClickHouseSinkTask.start` | `UNVETTED_RETRIABLE_CODES_WARNING` | existing | WIRED, unchanged |

### Data-Flow Trace (Level 4)

Not applicable. Nothing in this phase renders dynamic data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| LIF-01 pin red on base | detached worktree at `a7258507`, HEAD pin copied in, `testUnit --tests '*ClickHouseSinkTaskStopUnitTest*'` with Docker unreachable | 6 tests, 2 failed with `expected: <1> but was: <0>`, the 2 named | PASS (red as required) |
| LIF-01 pin green on `HEAD` | `testUnit --rerun` for the six classes above | 0 failures in all six | PASS |
| Failing-testcase set | `47-evidence-check.py failed build/test-results/testUnit` | 0 failing, 0 expected | PASS |
| Anchors mirror | `47-evidence-check.py anchors ... --rows 218` | `rows=218 bad=0` | PASS |
| Evidence index | `47-evidence-check.py index ... 15 --done` | `OK index: 15 rows` | PASS |
| gg7 CHANGELOG section | `cmp` against base and `v1.7.0_gg7` | identical | PASS |
| Connect `stop()` handling | read `WorkerSinkTask.java` (4.1.1) lines 183-187 | log-and-continue confirmed | PASS |

### Probe Execution

SKIPPED. The phase declares no `probe-*.sh` scripts.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| LIF-01 | 47-01 | `stop()` reaches later cleanup when a strategy throws; red-first, Docker-free pin | SATISFIED | `try`/`finally` at `ClickHouseSinkTask.stop()`. Pin 6/6 green on `HEAD` and 2/6 red on base. No Mockito, Docker unreachable. |
| LIF-02 | 47-01, 47-03, 47-05 | CHANGELOG `stop()` and Known Issues bullets match the fixed behaviour | SATISFIED | gg8 section in `CHANGELOG.md`. Claims are pinned or labelled as probe or source read. gg7 section is byte-identical to the tag. |
| RTY-01 | 47-02, 47-05 | Retriable `DataException` budget, or the limit pinned and documented under both tolerance values | SATISFIED (limit branch, per U-1) | 108-case pin through `ProxySinkTask.put` and `putDirect`. FORK-DELTA item 17 rewritten. Budget-of-5 mutation recorded (E-05, E-13). |
| RTY-02 | 47-03 | FUT-05 closed, WARN kept, validator only if the user approves | SATISFIED | FORK-DELTA items 8 and 24. WARN, its pins and `ClickHouseSinkConfig` unchanged. Decision attributed to U-2. |

**Orphaned requirements:** none. `REQUIREMENTS.md` maps exactly LIF-01, LIF-02, RTY-01 and RTY-02 to Phase 47, and all four are claimed by plans.

**Bookkeeping, not a code gap.** `REQUIREMENTS.md` (lines 26, 27, 37, 38 and traceability lines 87, 88, 92, 93) and the Phase 47 ROADMAP checkboxes still read unchecked and Pending. The ROADMAP plan list says "4 plans" and omits `47-05`. E-11 hands the REQUIREMENTS update to the orchestrator and the transition step.

### Prohibitions (judgment tier, from plan frontmatter)

All were checked against the repo and are held, not just claimed.

| Prohibition | Result |
|---|---|
| No `src/main` change outside the `stop()` body, no catch or new method | Held. The diff is `try`/`finally` only. |
| No Mockito, test tag or Docker base class in the pins; no existing test file edited | Held. Zero Mockito references. The changed-file list shows only two added test files. |
| No push; gate scripts and plan-time helpers not edited | Held. `git log` over the helpers and `verify-merge-invariants.sh` since base is empty. `git ls-remote origin` returns no `sync/v1.7.0` branch and no `pre-v1.7.0_gg8` tag. |
| No validator, budget or removal setting; `ClickHouseSinkConfig`, `Utils` and `ProxySinkTask` untouched; WARN text and tests unchanged | Held. |
| gg7 CHANGELOG, `ROLLOUT-*`, `VERSION` not edited | Held. |
| No FORK-DELTA number or cadence that E-05/E-06 did not record; `exactlyOnce=true` not claimed as pinned | Held. Item 17 says "with `exactlyOnce=true` the redelivery is not pinned". |

### Anti-Patterns Found

I scanned the three changed code files for `TBD`, `FIXME`, `XXX`, `TODO`, `HACK` and placeholder markers. There are none in the phase's added lines. The only `src/main` diff is the 13-line `try`/`finally`, and the new tests are not stubs.

### Review Dispositions

`47-REVIEW.md` has 0 critical, 5 warnings and 4 info. `47-REVIEW-DISPOSITION.md` records WR-01..WR-05 fixed or fixed by qualification, and IN-03 fixed. I confirmed each fix in the tree:

- **WR-01.** The writer row now names `close(partitions)`.
- **WR-02.** The CHANGELOG states Connect logs a WARN and that the dropped exception is not logged.
- **WR-03.** Item 17 scopes the cadence to the Kafka 3.9 worker.
- **WR-04.** `REDELIVERIES = 50`.
- **WR-05.** Item 17 scopes the claim and says the `putDirect` first-put outcomes are not pinned.

Open Infos IN-01, IN-02 and IN-04 are recorded as `open` with reasons. IN-04 is pre-existing and outside the diff. None affects a must-have. IN-01 is carried above as the advisory `coincidental_reliance_items` entry.

### Deferred Items

None. The items below are explicit hand-offs to Phase 49, recorded in E-11 as 14 items. They are outside Phase 47's success criteria and are not gaps in this phase:

- the gg7 rollout rows F5 and R8, and the `FUT-05 stays open.` line (D-2)
- completing and dating the gg8 CHANGELOG section (U-3)
- the `VERSION` bump
- the FORK-DELTA raw-numbers refresh

Phase 48 also owns the gg6 note row C2.

Two stale-text risks for the later phases are worth keeping in view. FORK-DELTA item 8 still points at the "Future Requirements" list in `REQUIREMENTS.md`, which no longer carries FUT-0x identifiers (E-11 row 10). The gg6 rollout row C2 still says "FUT-05, open in FORK-DELTA" (E-11 row 5).

### Human Verification Required

None. The remaining unpinned behaviours are all stated as limits in the documents, so they are recorded and not hidden.

- **Connect redelivery cadence on the 4.x line.** The cadence is measured on the Kafka 3.9 worker only. The documents say so.
- **`exactlyOnce=true` redelivery.** It is not pinned, and item 17 says so.
- **Connect's WARN-and-continue on a throw from `stop()`.** It is read from the sources, which I re-read for 4.1.1.

### Gaps Summary

No gaps. All four roadmap criteria hold against the code and the tests, which I re-ran for the Docker-free classes. The LIF-01 pin is red on the unchanged `src/main` and green on the fixed tree. The one production change is the `try`/`finally` in `stop()`. The gg8 CHANGELOG and FORK-DELTA claims about `stop()` do not exceed what the pin proves, or are labelled as probe or source-read. The gg7 section is byte-identical to the tag. RTY-01 is satisfied by the recorded-limit branch, which is user decision U-1. FUT-05 is closed in FORK-DELTA with the WARN, its pins and the config untouched.

The recorded Docker-backed totals (E-10 and E-15: testUnit 2295 and full suite 2898 on `latest` x V2, with 18 named skips, 0 failures, tail gate PASS and anchors 218) were not re-run here. Only the Docker-free classes above were re-run.

One advisory remains and does not change the status: the MBean-gone sub-assertion in the stop pin has no precondition (IN-01).

---

_Verified: 2026-11-06_
_Verifier: Claude (gsd-verifier)_
