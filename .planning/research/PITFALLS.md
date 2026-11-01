# Pitfalls Research

**Domain:** Maintenance milestone `v1.7.0_gg7` on a fork of the ClickHouse Kafka Connect sink (exactly-once state machine, Debezium CDC into ClickHouse). Work in scope: F-1 (close the writer's helper client), F-2 (insert timeout with `async=false`), F-3 (harness Datagen counting), IN-04 (Docker-free `custom_*` pin), the wrapped-`DataException` case in `Processing.doLogic`, FUT-05 (transient-only `additionalRetriableErrorCodes`), OI-35-1..3, Phase 37 dispositions, Dependabot PRs #21 (`setup-gradle` SHA re-pin) and #22 (gson 2.15.0), `bump_version.py` use, and the `v1.7.0_gg7` release.
**Researched:** 2026-11-01
**Confidence:** HIGH for everything read from this repository (code, Phase 34-38 review files, RETROSPECTIVE, memory notes); MEDIUM for the gson 2.15.0 release-note summary (one fetched page) and for ClickHouse deduplication defaults (stated from ClickHouse docs knowledge, not re-verified in this session).

Evidence labels used below: **[code]** read from the current tree in this session, **[review]** stated in a Phase 34-38 review, verification or follow-up file, **[retro]** RETROSPECTIVE.md or the memory notes, **[inferred]** my reasoning from the above, not run.

Provisional phase labels (the roadmapper numbers them from 39): **P-DEP** Dependabot absorption, **P-CODE** code follow-ups (tests first), **P-DOC** documents and Phase 37 dispositions, **P-REL** release. The order P-DEP, P-CODE, P-DOC, P-REL follows the dependency-first pattern that worked in gg3, gg4 and gg6 [retro], and it leaves the archive allow-list for last (Pitfall 9).

## Critical Pitfalls

### Pitfall 1: F-1 is bigger than "close `chc` in `stop()`" — there are three leak sites, and the V1 one leaks per insert

**What goes wrong:**
The Phase 36 follow-up describes F-1 as one leaked V2 client per task start. The code shows more [code]:
- `ClickHouseHelperClient`'s constructor *always* calls `createClientV2()` and `createClientV1()` ("We are creating two clients"), whatever `useClientV2` says. So a V2 `Client` with its HTTP pool exists for every helper, including on `clientVersion=V1`.
- `ClickHouseWriter.getMutationRequest` (the V1 insert path, 3 call sites) builds a throwaway `chcTmp` helper on **every insert batch**, reads `getServer()` and drops it. The builder default is `useClientV2 = true`, and the constructor builds a V2 client regardless. That is one unclosed V2 client per batch on V1, not per task start.
- `KeeperStateProvider` (exactly-once) builds its own `ClickHouseHelperClient` and never closes it. `ProxySinkTask.stop()` calls only `dbWriter.stop()`, and `StateProvider` has no stop hook.

Fixing only `ClickHouseWriter.stop()` closes the smallest of the three and the test "counts open clients over a start/stop cycle" goes green while the other two keep leaking.

**Why it happens:**
The follow-up was written from the observed per-task-start symptom on the default (V2, non-exactly-once) path. The V1 per-insert and Keeper paths are only visible by reading the constructor and `getMutationRequest`.

**How to avoid:**
1. Decide scope explicitly in the plan: writer `chc` (required), `chcTmp` (close right after `getServer()`, `ClickHouseNode` does not depend on the V2 client, or use try-with-resources), `KeeperStateProvider` (needs a lifecycle hook on `StateProvider`/`ProxySinkTask.stop()`; it is a consistency-critical file, so treat it as its own decision and its own test, or record it as an explicit non-goal with an owner and trigger).
2. Keep the diff additive for rebasability: `ClickHouseHelperClient` already implements `AutoCloseable`, so the fix is calls to the existing `close()`, not a new abstraction.
3. Measure before claiming: run N start/stop cycles and count live client instances or threads, on V2 and on V1, before and after. Label the result `measured`, not inferred.

**Warning signs:**
A test named for F-1 that only starts and stops a `ClickHouseWriter`; a SUMMARY that says "leak closed" without naming `chcTmp` or `KeeperStateProvider`; heap or thread growth on a V1 or exactly-once canary after the fix.

**Phase to address:** P-CODE (F-1 plan; scope decision recorded before the first edit).

---

### Pitfall 2: Closing the helper client too early, twice, or on a half-started writer

**What goes wrong:**
`stop()` is entered in states the happy path never shows:
- `chc` is `null` (`stop()` before `start()`, or `start()` threw while building the client).
- `start()` returned `false` after `chc` was built (ping failed, server older than 23.3, no tables). `ProxySinkTask.start()` then throws, and `chc` is live but unreferenced. Closing only in `stop()` misses this exit; the failed-start path needs its own close.
- The `clickhouse-table-sync` refresher may be mid-`extractTablesMapping` on `chc` when `stop()` runs. `stop()` calls `shutdownNow()` then waits 5 s and only logs an error if the thread did not end. If `chc.close()` comes first, or the shutdown block throws, the refresher fails on a closed client; if close is skipped on a shutdown exception it leaks again.
- Tests swap the private `chc` field by reflection (`ClickHouseWriterTest` with a Mockito spy, `ClickHouseWriterNullMessageUnitTest`, `ScriptedWriteFailureWriter`; field name `"chc"`), and `CountingHelperClient` documents "callers close it with try-with-resources". A `stop()` that closes whatever is in the field makes those tests double-close, and a swapped-in helper means the original is never closed.

**Why it happens:**
`stop()` currently only handles the executor, so the natural edit appends `chc.close()` at the end.

**How to avoid:**
- Close in a `finally` after the executor shutdown block, null-guarded, and null the field so a second `stop()` is a no-op. The `AutoCloseable` contract wants idempotence; do not assume the V2 `Client.close()` is idempotent, pin it.
- Also close on every `start()` exit that returns `false`.
- Do **not** rename, wrap or make `final` the `chc` field: three test classes reach it by name. If a rename is wanted, change those tests in the same commit and say so.
- Write the test with the existing reflection-swap idiom plus a `CountingHelperClient`-style subclass that counts `close()` (no Mockito, Pitfall 7). That needs no production seam, so `ClickHouseWriter`'s constructor and `start()` signature stay upstream-shaped.

**Warning signs:**
`NullPointerException` from `stop()` in a start-failure test; "Failed to describe table" ERROR logs right after a stop; `ClickHouseWriterTest` failing only when run after another test that stops the writer.

**Phase to address:** P-CODE (F-1).

---

### Pitfall 3: F-2 as written ("make the 4-minute timeout effective") can turn a hang into duplicated rows or a dropped batch

**What goes wrong:**
Three separate hazards [code, inferred]:
1. **The existing timeout path is not retriable end to end.** `awaitInsertResponse` throws `RetriableException(…, TimeoutException)`. `Processing.doInsert` then wraps every failure as `new RuntimeException(queryId.toString(), e)`. `Utils.handleException` has no pass-through for `RetriableException`, and it classifies on the deepest cause, here `java.util.concurrent.TimeoutException`, which is not a `SocketTimeoutException`, not an `IOException`, not a server error. Result: with `errors.tolerance=none` the task fails with `RuntimeException("Number of records: N")`; with `errors.tolerance=all` the batch goes to the DLQ and offsets advance. No test exercises `awaitInsertResponse`'s timeout (grep of `src/test`). Making the timeout fire in production would first expose this.
2. **A shorter wait creates the "insert landed but we gave up" case.** A timeout on a still-running server-side insert is the crash-after-insert window the state machine was built for, but only exactly-once mode has a dedup token. In non-exactly-once mode the retry re-inserts the same rows (duplicates). In exactly-once mode dedup works only if the target engine deduplicates: `replicated_deduplication_window` for Replicated/Shared engines; plain MergeTree needs `non_replicated_deduplication_window` (default off, per ClickHouse docs; verify). More timeouts means more exposure to a table that does not deduplicate.
3. **The wrong knob or scope.** `csc.getTimeout()` (30 s default, used for ping) is not the insert timeout; reusing it would kill legitimate large inserts. A client-wide socket timeout set in `createClientV2()` also applies to the state-table queries of `KeeperStateProvider`, DESCRIBE and `system.tables` refreshes, and the start-up ping. A bounded-execution wrapper (run the insert on another thread, `future.get(timeout)`) cannot abort a blocked socket read: `future.cancel(true)` leaves the insert running while Connect redelivers the batch, so two inserts of one batch can overlap.

**Why it happens:**
The follow-up defines the problem as "the timeout never fires" and the cheapest fix looks like "make it fire".

**How to avoid:**
- Fix the classification first, as its own test-first step: a Docker-free test through `ProxySinkTask.put` with a writer that throws the timeout `RetriableException`, asserting it reaches Connect as retriable under both tolerance settings. Use the existing `InMemoryDBWriter`/`DataExceptionDBWriter` seam style (`AdditionalRetriableErrorCodesPinUnitTest`).
- Prefer a client-level read/socket timeout on the insert call over thread abandonment, and set it so an operator's own `socket_timeout` in `jdbcConnectionProperties` still wins. That ordering contract (default applied before the operator loop) is exactly the one Phase 36 review WR-02 found unpinned; pin the order for any new default.
- Scope the timeout to inserts if client 0.11.0 allows a per-operation option (verify in the plan's research step); otherwise document that it also covers the other queries and test start-up against a slow stub.
- Do not "fix" F-2 by forcing `async_insert` or `wait_for_async_insert`: the connector deliberately does not set `async_insert` (anchor `//this.addClickHouseSetting("async_insert", "0", false);`, CFG-02), and doing so changes dedup semantics.
- Treat the new effective timeout as an operator-visible behaviour change: default it so a connector that works today does not start failing, validate against `max.poll.interval.ms` (the existing check at `ClickHouseSinkConfig:428`), and give it a rollout line with the duplicate-risk wording.

**Warning signs:**
A stalled-stub test that passes only because the wrapper exception happens to be retriable; DLQ records that also exist in the table; `Number of records:` task failures after a slow merge period.

**Phase to address:** P-CODE (F-2; classification pin first, then the timeout), P-DOC (rollout wording).

---

### Pitfall 4: Changing retry classification silently changes DLQ routing and the BEFORE/AFTER outcomes

**What goes wrong:**
Retry classification is the only thing deciding whether a failure is retried by Connect (task stays up, partition blocked, state stays `BEFORE`), fails the task, or goes to the DLQ and advances offsets. In this system:
- A `RetriableException` thrown from `handleException` bypasses `errors.tolerance` and the DLQ, and has no retry budget. A permanent error code listed in `additionalRetriableErrorCodes` is a poison pill: the same batch is redelivered forever, the partition never advances, and in exactly-once the state row is stuck at `BEFORE`.
- The wrapped `DataException` case (Phase 34 verification open item 1, ROLLOUT rows C3): the writer's `DataException` always reaches `handleException` wrapped in `RuntimeException(queryId, …)` from `Processing.doInsert` [code]. So the `e instanceof DataException && !errorsTolerance` branch (rethrow the `DataException` itself) does not fire on the production path; the task fails with the `RuntimeException("Number of records")` wrapper. "Fixing" it by unwrapping changes the exception class and message of task failures and DLQ stack-trace headers, loses the `queryId` that the wrapper exists to carry (`// This way the queryId will propagate`), and can break `ValueFreeDlqHeadersUnitTest`. Separately, a `DataException` whose cause chain holds a retriable server code, or a socket timeout under `tolerance=all`, is retriable today by the root-cause-first rule in upstream code; making it non-retriable is a behaviour change to upstream code that was merged unedited.
- The 184-case `WriterFailureClassificationUnitTest` matrix and the `AdditionalRetriableErrorCodesPinUnitTest` sweeps are the measured statement of "classification unchanged". AGENTS.md forbids deleting or weakening tests, but a deliberate behaviour change has to flip rows.

**Why it happens:**
The user decision on the wrapped case and FUT-05 was deferred twice; the temptation is a one-line "never retry a DataException" or a validator that rejects codes.

**How to avoid:**
- Get the user's decision recorded first (the roadmap should make it a blocking-human checkpoint: it changes upstream-owned code). Options to put to the user: pin and document only (cheapest, zero rebase cost), or change `handleException`/`doLogic` with a named row flip list.
- If code changes: land the new expected outcomes as *added* rows or a new matrix version in a commit that goes red first, then flip the implementation; never edit an existing expectation in place without naming it in the commit message and the rollout note. Run the matrix before and after and diff the per-row verdicts; the diff must be exactly the intended rows.
- FUT-05 as "list only transient codes": prefer documentation plus an optional start-up WARN naming a listed code that is known permanent over a validator that throws `ConfigException`. A rejecting validator makes an operator's existing connector fail to start after the upgrade, and it edits `ClickHouseSinkConfig`, a high-risk and upstream-owned file. A retry budget is a new feature and is out of scope for this milestone.
- Check every message path for the 38-REVIEW class of defect: operator text claiming "never retriable" or "safe" must match the pinned rows.

**Warning signs:**
A diff touching `Utils.handleException` or `Processing.doInsert`'s `catch`; any matrix expectation edited rather than added; a rollout sentence with an unqualified "never".

**Phase to address:** P-CODE (decision checkpoint, then pins), P-DOC (rollout rows C2/C3 named by ID, including FUT-05).

---

### Pitfall 5: Touching `Processing.doLogic` for OI-35-1 can orphan state or only half-fix the key

**What goes wrong:**
OI-35-1 is a one-line change: `String originalTopic = record.getSinkRecord().topic();` should read `originalTopic()`. Facts [code, review]:
- `originalTopic` is not persisted to Keeper: `KeeperStateProvider.setStateRecord` writes only key (`topic-partition`), min, max and state. The state key is built from the **transformed** `topic` and `partition` and must not move; changing it orphans every stored KeeperMap row (first batch reads `NONE`, inserts as new, old rows have no TTL). Phase 35 pinned the key with `ProcessingRenamedTopicStateKeyUnitTest` and the stop rule says a key change stops for a user decision.
- The `lastInsertedOffsets` key is `new TopicPartition(originalTopic, stateRecord.getPartition())`, so the one-liner fixes the topic half only. `partition` and the stored max offset come from the transformed record. Under a partition- or offset-changing SMT the key is still wrong, and review WR-01 of Phase 35 showed that no test catches a partition-only mistake today.
- `ClickHouseSinkTask.close` → `onPartitionRemoved` has the same keying mismatch (35 review IN-04) and is a no-op under a renaming SMT; fixing only the `doLogic` line leaves stale entries unless `onPartitionRemoved` is made to match.
- `Processing.java` is the highest-risk file: the BEFORE/AFTER sequence must survive crash-before-insert, crash-after-insert-before-AFTER, and crash-during-flag-write.

**How to avoid:**
- Keep the change to the `originalTopic` local and its consumers; assert in the same plan that `git diff` of `StateRecord` construction arguments `topic`/`partition` and of `KeeperStateProvider` is empty (reuse the E-06/E-07 "processing untouched except line N" measurement style).
- Add the missing pins first, red on the unchanged tree: a revoked-partition removal under a renaming SMT, and a partition-moving SMT (`RenamedTopicRecords.withTransformedPosition` already exists) through `Processing` with `reportInsertedOffsets=true`.
- Decide and record the scope for partition/offset (original vs transformed) explicitly; if it is out of scope, narrow the FORK-DELTA and rollout wording to the topic-rename case (OI-35-3 stays open).
- Re-run all 14 `ProcessingTest` cases and the state-key tripwire; they must stay green and unmodified.

**Warning signs:**
`StateRecord(topic, …)` arguments edited; a test that exercises `InMemoryState` only being cited as proof about `KeeperStateProvider` (35 review IN-05); `reportInsertedOffsets=false` (default) used in the new test, which proves nothing.

**Phase to address:** P-CODE (OI-35-1 plan, after the user decision recorded in `35-BUF03-RECORD.md`).

---

### Pitfall 6: A fork edit in `ClickHouseWriter`/`ClickHouseHelperClient` collides with upstream's in-flight client refactor and breaks the anchors invariant

**What goes wrong:**
- Upstream has open PR #861 "Isolated client init by version" touching `ClickHouseWriter` (+5/−28), `ClickHouseHelperClient` (+73/−18) and `KeeperStateProvider` (+1/−14), and #855 (apply `jdbcConnectionProperties` on V2). They overlap F-1, F-2 and the V2 client options the fork already patched for 0.11.0 [code; `gh pr view` on upstream, state OPEN as of 2026-10-26]. Whichever side merges second pays for a conflict in exactly the lines F-1/F-2 edit, and #861 may make F-1's sites disappear.
- `fork-anchors.txt` counts lines: `ClickHouseWriter.java` has `new ClickHouseHelperClient.ClickHouseClientBuilder(` = 2 and `.setCompressionMethod(csc.isSetCompressionMethod())` = 2 (CMP-01), `.setTargetTableFilter(...)` = 1; `KeeperStateProvider.java` has the builder = 1. Extracting a shared builder helper to de-duplicate `start()` and `getMutationRequest` changes those counts, and invariant 3 of `verify-merge-invariants.sh` fails for any range containing the commit unless `fork-anchors.txt` is updated in the same commit.
- `spotlessApply` on whole files, or reformatting while editing, creates conflict noise across upstream-owned lines; 46 Spotless violations are already pre-existing [retro].

**How to avoid:**
- Smallest possible additive diffs: calls to the existing `close()`, `try`/`finally` around existing code, no extraction, no renames, no reordering.
- If a count must move, change the anchors row in the same commit and say why in the message; add a FORK-DELTA row for every F-1/F-2 patch with a "superseded if upstream #861 lands" trigger.
- Run `spotlessApply` only on files you changed and check `git diff --stat` shows no unrelated file; do not run it repo-wide.
- Before starting P-CODE and again before P-REL: `git fetch upstream` and check whether #861/#855 merged or whether `upstream/main` gained a tag; if so the milestone's "no upstream sync" premise changes.

**Warning signs:**
`verify-merge-invariants.sh` invariant 3 failing on a code commit; `git diff --stat` listing files nobody meant to touch; new fork-only helper methods in upstream-owned classes.

**Phase to address:** P-CODE (every `src/main` plan), P-REL (re-check upstream; anchors gate).

---

### Pitfall 7: Mockito inline mocking and the OpenJ9 JDK 17 test-JVM crash

**What goes wrong:**
The local default JDK is Semeru/OpenJ9 17. In the full suite a test JVM can die natively inside Mockito's inline bytecode generator (`retransformClasses`), Gradle reports only "exit value 255", the JUnit XML shows 0 failures, and an ~8 GB core dump lands in the repo root. It hit gg2, gg4 (five times under swap pressure) and gg6 (both full suites needed `-PjavaVersion=21`) [retro, memory]. New F-1/F-2 tests are the likeliest place to reintroduce it: the natural way to "verify `close()` was called" or "stall the client" is `Mockito.spy`/`mock` on `ClickHouseHelperClient`, `ClickHouseWriter` or the V2 `Client`. One inline `spy` remains (`ClickHouseWriterTest` line 444, `staleDescribeTableIsNotCachedAsCurrentSchema`, the known crash frame).

**How to avoid:**
- No Mockito in new tests. Follow `CountingHelperClient`: a real subclass with a counter, closed by try-with-resources, no static state. For stalls use `StubClickHouseServer` (a real socket) with millisecond-scale timeouts, not `Thread.sleep` against a real server.
- Do not use `mockStatic` or `mockConstruction`.
- Gates: on exit 255 read the `javacore.*.txt` before blaming the change, delete `core.*.dmp`, `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one (never `git clean`), re-run once, and record both runs. Run the two gate suites with `-PjavaVersion=21` from the start if swap is high, and record that choice honestly in the evidence (gg6 did).
- Every `ProxySinkTask` that runs `put()` in a Docker-free test must call `stop()` in `finally`, or its topic MBeans leak into the JVM-global MBean server (Phase 34 WR-01).

**Warning signs:**
"Gradle Test Executor N finished with non-zero exit value 255"; a suite tally lower than the previous run's with no failure listed; a fresh `core.*.dmp`.

**Phase to address:** P-CODE (test style rule in every plan), P-REL (two-run gate).

---

### Pitfall 8: SHA re-pinning behind the pin guard — four files, one fixture, one doc row, and two Dependabot behaviours

**What goes wrong:**
PR #21 moves `gradle/actions/setup-gradle` 6.4.0 to 6.4.1. The same SHA appears in four fork-owned workflows (`tests.yaml`, `nightly-integration-tests.yaml`, `release.yaml`, `pre-release.yaml`). `release.yaml` and `pre-release.yaml` are pinned equal by `ReleaseWorkflowGuardUnitTest`, so an edit to one must be byte-identical in the other. Failure modes [code, review, retro]:
- Pinning the annotated tag object SHA instead of the commit it points to. Check `git ls-remote <repo> refs/tags/vX.Y.Z refs/tags/vX.Y.Z^{}` (gg6 did this for setup-java); take the `^{}` line when present. The comment must be `# vX.Y.Z` (the pin guard rejects any other shape).
- Updating three of four files. `WorkflowActionPinGuardUnitTest` enumerates the workflows, but `nightly-integration-tests.yaml` is fork-owned and easy to forget.
- Touching `integrationTests.yaml`. It is upstream's, byte-identical by SHA-256 and unpinned on purpose (`@v3`); a stray bump of it must be closed, not merged.
- Guard tests that pin the live value. 37-REVIEW WR-03 (checkout SHA in `IncrementVersionWorkflowGuardUnitTest`) was fixed to match by shape; re-check `TestsWorkflowPinGuardUnitTest`'s `FIVE_PINNED` (`v6.4.0` over a fake SHA) and `WorkflowActionPinGuardUnitTest` line 233: they read as fixtures, but the bump must prove it by running all four guard classes under `testUnit --rerun`, not by reading them.
- Doc drift: `docs/FORK-DELTA.md` rows 76/79 name `setup-gradle v6.4.0`, and no test ties the doc to the workflow. Gg6 refreshed these from measured values; do the same.
- Dependabot specifics: it keeps PR branches rebased on `custom_build`, groups github-actions into one PR (so PR #21 may gain more actions at the next monthly run, today is the 1st), and the user merges Dependabot PRs directly on `origin` (outside GSD) [memory]. A merge on `origin` after the milestone started diverges the working branch and can break the fork-anchors invariant if an anchored line moves.

**How to avoid:**
- Absorb each Dependabot PR as its own commit by merging the fetched PR head (gg6 pattern), with its own gate: the four guard classes, the SHA-to-tag lookup, `integrationTests.yaml` blob unchanged, then a full suite on the tip.
- At milestone start and before every push or publish gate: `git fetch origin`, compare `origin/custom_build` with the working branch, merge (never rebase), and check whether any bumped line is anchored.
- After the merge, re-run `verify-merge-invariants.sh` with the exemption file only if a row-exact exemption is needed; never move `PRE_TAG`.
- Do not hand-edit a pin that Dependabot already produced; verify it instead.

**Warning signs:**
`TestsWorkflowPinGuardUnitTest`/`WorkflowActionPinGuardUnitTest` red on the Dependabot PR; `git ls-remote` returning two lines for the tag and the pin matching the first; `origin/custom_build` ahead of the working branch with a commit nobody expected.

**Phase to address:** P-DEP.

---

### Pitfall 9: The archive allow-list is valid for exactly one tree, and the compare fails only at publish time

**What goes wrong:**
`release.yaml`/`pre-release.yaml` rebuild the previous tag's archive (`v1.7.0_gg6`) and compare it with the new archive under `.planning/scripts/confluent-archive-allowlist-${VERSION}.txt`. The file for `v1.7.0_gg7` must exist, and its header states the list "is valid for exactly one tree: any later change of src, `build.gradle.kts`, gradle or `README.md` moves the archive and turns a row into STALE-ROW or a difference into UNALLOWED" [code]. Specific traps for this milestone:
- gg7 changes `src/main` (F-1/F-2/Processing/Utils/ClickHouseSinkConfig as decided), the gson jar (`com/google/gson/*`, `META-INF/maven/com.google.code.gson/gson/*`, and gson's `META-INF/versions/9/module-info.class`; the gg4 list has all three rows), and `Version.class` plus `SinkTaskStatistics.class` (release identity). Inner and anonymous classes of every changed class differ too (`ClickHouseWriter$1`, lambdas), and the gg6 list shows they need their own rows.
- Copying the gg6 list forward makes its rows `STALE-ROW` (an unused row fails) and the new differences `UNALLOWED`.
- The compare runs only in the release workflows, so a tree edit after the list was generated is invisible to `tests.yaml` and the local suite and fails at the dry run or the publish run.
- 37-REVIEW WR-02: the loader's catch-all refusal only rejects star-only globs. `*?`, `**/*`, `*/*`, `[!x]*` load and match everything. Do not take the shortcut of a broad row to make a red compare green; derive rows from the report.
- The compare is wired to a baseline picked by `baseline_tag.py` from tags reachable from HEAD; a missing or mis-ordered tag fails closed.

**How to avoid:**
- Generate the allow-list last, from the report of the final tree's rebuilt archive (the gg6 order: documents frozen, then allow-list), and treat any later `src`/`build.gradle.kts`/`gradle`/`README.md` edit as a reason to regenerate it.
- Run the `pre-release.yaml` dry run from a scratch branch before the publish (gg6 lesson: the dry run is the only proof of the compare chain's CI wiring) and read the report artifact.
- Keep rows anchored literal-prefix globs, one per library, exact paths for connector classes.

**Warning signs:**
`UNALLOWED`/`STALE-ROW` counts in the compare step summary; a `README.md` or CHANGELOG-adjacent edit after the list was written (README is in the archive; CHANGELOG is not).

**Phase to address:** P-REL (list generation and dry run); P-DEP and P-CODE must not edit the tree after P-REL starts without regenerating it.

---

### Pitfall 10: Release gates that fire late — date, tag, CI cancellation, version grammar, and the human-approval boundary

**What goes wrong:** (all [retro], [review], [code])
- **CHANGELOG date gate.** gg6's heading carried the planning date and the merge landed a day later; the date gate fired at publication. Write the date at merge time or make it a checked precondition one step earlier.
- **Tag push cancels CI.** `tests.yaml` concurrency is keyed on `github.sha` with `cancel-in-progress`; a tag push on the same SHA cancels the branch run. Gg6's design avoids hand-pushing tags (the publish run creates the tag; a hand-pushed tag is refused by the tag-absent gate). Keep it that way and push by SHA to an explicit ref, never by branch name.
- **Exactly one CI run per published SHA**, and the publish gate needs a successful `tests.yaml` run on that SHA.
- **Version grammar mismatch.** The workflow regex accepts `_gg[0-9]+`; the scripts require `_gg[1-9][0-9]*` (37-REVIEW IN-01). `bump_version.py` writes `VERSION` and the `VERSION` row of `fork-anchors.txt` together; run it, do not hand-edit either, and expect the anchors row to move in the same commit. `_ggN` suffixes were the cause of the old `vnull` bug.
- **Operator note and independent review.** The rollout note needs an orchestrator-spawned fresh `gsd-code-reviewer` *before* the push gate, repeated until a round is clean; an executor's inline review missed five warnings in gg3 and Phase 24 recurred [retro, memory]. Gg6 ran 12 rounds against a cap of 3. Budget by severity (stop on no Warning), not by round count, and record the cap decision in the plan.
- **Rollback commands** in the note must be run on the post-publication tip and on a dry-run merge (gg1 lesson); every behavioural claim is labelled `measured`, `read from the code`, or `derived`, and the quantifiers match the measurement.
- **Approval boundary.** Relayed approvals are refused by executors at `blocking-human` gates; decide in planning who runs each irreversible step and write it into the handoff (gg6 lesson 2). The `--no-ff` merge needs the user's direct approval.

**How to avoid:**
Make the release plan a checklist of the gates above with a named owner per step, including who creates the CHANGELOG date and when.

**Warning signs:**
A CHANGELOG heading dated before the merge day; a tag present on `origin` before the publish run; two CI runs on one SHA.

**Phase to address:** P-REL.

---

## Moderate Pitfalls

### Pitfall 11: A new Kafka API reference in `src/main` breaks the Connect API guard

**What goes wrong:**
`ConnectApiReferencesUnitTest` derives every `org/apache/kafka` class and member the compiled connector references and fails when one is missing from `src/test/resources/connect-api-references.txt` (196 lines, blessed against the production worker's 7.9.7 jars; the connector compiles against connect-api 4.4.0 but the worker supplies the runtime). F-2 (timeouts), FUT-05 (config validation) or F-1 (a new exception type) can add a reference such as `ConnectException`, a `Time` or `ConfigException` member. Re-blessing needs the worker image jars and `.planning/scripts/connect-api-refs.py`, not a hand edit; a member absent from the worker is a runtime `NoSuchMethodError`. Phase 35 review WR-02 shows the failure shape: on at-least-once the insert lands before the missing accessor throws, then the chunk is redelivered.

**Prevention:**
Check `connect-api-references.txt` impact in each `src/main` plan; prefer already-referenced classes; re-bless only with the documented command and keep the "result worker: missing 0" lines.

**Phase to address:** P-CODE.

### Pitfall 12: Testing a stalled or timed-out insert is slow, flaky, or tests the wrong layer

**What goes wrong:**
A real stalled server needs a real socket; second-scale timeouts bloat a 2,270-test suite; a test that asserts only "RetriableException thrown by the writer" misses the Pitfall 3 classification gap.

**Prevention:**
Use `StubClickHouseServer` with a millisecond timeout and assert through `ProxySinkTask.put` and `Utils.handleException`, under both `errors.tolerance` values. Docker-free `testUnit` first, one live pin at most. A pin on client behaviour (IN-04: `custom_*` forwarding fails the V2 start-up ping) is a client-version tripwire; label it so, and expect it to flip on the next client bump rather than weakening it.

**Phase to address:** P-CODE.

### Pitfall 13: F-3 harness fix shaped like F-1 (wrong scope, or a hygiene regression)

**What goes wrong:**
F-3's remaining sites are other helpers counting across `getConnectors()` output and `createConnectorAndWaitUntilRunning` (checks task 0 only). The 36 review IN-06 list for `generateData`: strict `==` exit condition (use `<`), `Pattern` recompiled per poll, magic numbers, a `finally`-time `deleteConnectors` `IOException` masking the real failure. Copy those into the new code.

**Prevention:**
Fix the helpers that count across connectors in one change with the same per-connector-status pattern; keep it test-code only (no `src/main`); there is no nightly red to rely on, so run `integrationTest` locally once and record the result.

**Phase to address:** P-CODE.

### Pitfall 14: gson 2.15.0 changes more than a number

**What goes wrong:**
Per the fetched release notes (MEDIUM): untyped maps now default to `LinkedHashMap`; stricter integer parsing and unpaired-surrogate rejection in strict JSON; `InetAddress` and BOM handling changed. The fork uses gson only for serialization (`DataJson.GSON`, `ConvertToMapOrJson` with a registered `Date`/`Timestamp` adapter), so the likely impact is small, but `DataJson.GSON` feeds the metadata-free JSON insert bytes and the exactly-once rule is that a retry batch is byte-identical. A batch left `BEFORE_PROCESSING` by gg6 and retried by gg7 is covered by the deduplication token (gg2 rollout), not by the bytes.

**Prevention:**
Run the full suite twice on the bump commit (the gate is the guard); read `DataJson`/`ConvertToMapOrJson` outputs for key-order-sensitive tests; the archive compare needs the three gson allow-list rows (Pitfall 9). Do not read the release notes as proof: the suite is.

**Phase to address:** P-DEP.

### Pitfall 15: Verification goes stale, and the milestone close becomes an override again

**What goes wrong:**
Six of seven prior milestones closed as `override_closeout` because later phases edited covered files [retro]. This milestone edits `docs/FORK-DELTA.md`, `fork-anchors.txt`, the rollout docs and `.github/` across all phases, so earlier verifications will read stale again.

**Prevention:**
Plan the override or re-verify at close; keep `requirements-completed` frontmatter in step with each plan; run the milestone audit before `/gsd-complete-milestone` (the pre-flight gate caught a skipped audit in gg3).

**Phase to address:** P-REL / close.

### Pitfall 16: Phase 37 dispositions that look like clean-up but edit pinned or approved files

**What goes wrong:**
Candidates: the `increment-version.yml` header comment (the real pin test is `IncrementVersionWorkflowGuardUnitTest`, not `ReleaseWorkflowGuardUnitTest`); rows 26 IN-02 (`shouldSkipDescribeRow` has no caller but is an anchor row, count 3 under HLP-02), 27 WR-01, 27 IN-01, 28 IN-01 (`ESCAPED_NEWLINE` public constant read by tests in two packages), 28 IN-05(b) (tightening an existing assertion); and the broad allow-list glob rows (UF-37-3). Each of the four `src/main` rows was held back because Phase 37 had no approval to edit `src/main`. Removing `shouldSkipDescribeRow` or changing a constant's visibility changes anchor counts or breaks tests in other packages; tightening 28 IN-05(b) is exactly the "weaken or change an existing assertion" case.

**Prevention:**
One decision per row with the user's recorded approval for any `src/main` row; update the anchors row in the same commit when a removal moves a count; changing an existing assertion needs explicit sign-off and a stronger, not weaker, replacement. Editing `increment-version.yml`, even a comment word, moves the pin-guard's line-by-line pin (`IncrementVersionWorkflowGuardUnitTest` pins the file line by line); run that class after the edit. Broad allow-list glob rows are removed, never widened (Pitfall 9).

**Phase to address:** P-DOC (and P-CODE for the `src/main` rows).

---

## Minor Pitfalls

### Pitfall 17: `bump_version.py` atomicity is described more strongly than it is

37-REVIEW WR-04 flagged an overclaimed atomicity; the docstring was since rewritten to "a failure while staging or replacing leaves the files as they were, only a process kill between the two renames can leave the anchors row ahead of VERSION; the next run then refuses". If it refuses, fix the anchors row by hand first; do not edit `VERSION` to match. **Prevention:** run it from a clean tree, commit both files together, and check `git status` shows no `.bump-*` files.

### Pitfall 18: GSD orchestration traps recorded for this repo

Short list from the memory notes, relevant to this milestone's tooling: ROADMAP `**Wave N**` headings repeat across phases (slice to the target `### Phase N:` section before scripted replaces); `custom_build` is the protected base branch, so bookkeeping commits live on local `sync/v1.7.0`; `git rev-list --left-right --count custom_build...sync/vX` shows 2 on the left, not 0; zsh mangles `$M:refs/...` (write `${M}:refs/...`) and there is no `timeout` binary; GSD verbs use the space form; `gsd-code-reviewer --fix` skips the reviewer on its first run, so carry standing findings forward with verbatim `### ID: title` headings.

### Pitfall 19: Spotless and imports

Spotless here checks import order and unused imports only (Google Java Format is commented out), so long lines are not violations, but unused imports left by a refactor are. Run `spotlessApply` on changed files and review the diff.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Close only `ClickHouseWriter.chc` for F-1 | Smallest diff, one test | `chcTmp` (V1 per insert) and `KeeperStateProvider` keep leaking, and the follow-up is marked closed | Only if the other two sites are recorded as open with an owner and trigger |
| Make `awaitInsertResponse` fire without fixing classification | F-2 looks done | Hung inserts become task failures or DLQ'd batches that also landed | Never |
| `Mockito.spy` for the F-1/F-2 tests | Faster to write | OpenJ9 crash risk returns, 8 GB dumps | Never in new tests; one inline spy already exists |
| Broad glob row to turn the archive compare green | Green dry run | The compare stops meaning anything (WR-02) | Never |
| Copy the gg6 allow-list forward | Saves derivation time | STALE-ROW and UNALLOWED at publish time | Never |
| Validator that rejects "non-transient" codes (FUT-05) | Enforces the rule in code | Existing connector fails to start after upgrade; edits an upstream-owned high-risk file | Only after a user decision and a rollout line |
| Unwrap `RuntimeException(queryId, e)` to fix the wrapped `DataException` | Correct exception class | Loses `queryId`, changes DLQ headers and the 184-row matrix | Only with a named row-flip list and user approval |
| Tighten a guard test by editing the existing assertion | Stricter pin | Violates "never delete or weaken existing tests" if done loosely | Only with sign-off and a stronger replacement |
| Cap review rounds at 3 and accept untested fixes | Faster close | A post-gate Info/Warning ships unreviewed (gg5, gg6) | Acceptable only with a recorded user decision |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| ClickHouse V2 client (0.11.0) | Assuming `close()` is idempotent, or that the helper only owns one client | It owns a V2 client even on `clientVersion=V1`; guard and pin double-close |
| ClickHouse V2 client timeouts | Reusing `csc.getTimeout()` (30 s, ping) or setting a client-wide socket timeout | Scope to inserts if the client allows; apply connector defaults before the operator's `jdbcConnectionProperties` loop and pin the order |
| ClickHouse dedup (exactly-once) | Assuming a retry after a timeout is always dropped | Needs `replicated_deduplication_window` (Replicated/Shared) or `non_replicated_deduplication_window` (plain MergeTree); verify per table |
| Kafka Connect retry contract | Expecting `RetriableException` inside a wrapped `RuntimeException` to be retried | `handleException` classifies on the deepest cause; test through `ProxySinkTask.put` under both tolerance values |
| Kafka Connect runtime | Referencing a Kafka member the 7.9.x worker lacks | Keep `connect-api-references.txt` blessed against the worker jars |
| GitHub Actions pins | Pinning the annotated-tag object SHA, or editing only some of the four fork workflows | Use the `^{}` commit, keep the twin workflows byte-equal, leave `integrationTests.yaml` untouched |
| Dependabot | Merging on `origin` between milestone gates, or hand-editing a pin it already produced | Fetch and merge `origin/custom_build` at each gate; verify rather than re-edit |
| GitHub release path | Hand-pushing the tag, or pushing it while the branch CI run is in flight | Let the publish run create the tag; push by SHA to an explicit ref after the one green run |
| Upstream (ClickHouse/clickhouse-kafka-connect) | Assuming "no upstream sync" means no upstream collision | Open upstream PRs #861/#855/#857 touch the same files; recheck before P-CODE and P-REL |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Per-insert helper construction on V1 (`chcTmp`) | Heap/thread growth, GC pressure on V1 connectors | Close after `getServer()` or reuse the writer's helper | Every insert batch on V1; visible within hours at high batch rates |
| Client leak per task start | Slow growth on workers that rebalance often | Close in `stop()` and on failed `start()` | Frequent rebalances or crash-looping tasks |
| Effective insert timeout below a legitimate batch time | Retried (duplicated) or failed batches during merges, `Too many parts` back-pressure | Default high, validate against `max.poll.interval.ms`, measure p99 insert time | Large `bufferCount` batches, busy or throttled clusters |
| Poison-pill retriable code | Partition stalled, state stuck at `BEFORE` | Document transient-only, optional WARN | The first batch that hits the listed code |
| Test noise from sweeps | ~3,300 WARN/ERROR lines per run from the code sweep (34 review IN-03) | Narrow the sweep to neighbours plus sentinels | Already present; affects failure triage |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Logging `jdbcConnectionProperties` verbatim (`createClientV2` INFO "ClickHouse URL") | Operator-supplied `user`/`password` keys reach the log (36 review IN-01) | If `createClientV2` is edited for F-2, log endpoint and database only; drop the property string |
| Record data in new error messages | Reopens the record-data-exposure class closed in gg2-gg4 (backlog 999.3) | New timeout/classification messages carry queryId, topic/partition/offset range and class names only |
| Workflow command injection from archive entry names | A hostile entry name starting a runner command | Keep `safe_line` on every output path; the manifest-diff and `ERROR:` paths were pinned by 37-REVIEW-UF WR-01 (fix `5ba9b84f`), so a new report line needs the same treatment and a test; that review's IN-01..IN-03 (bidi and zero-width characters, traceback exit code) remain open |
| Unpinned or tag-pinned third-party action in a release workflow | Supply-chain risk on the publish path | Full 40-hex SHA plus `# vX.Y.Z`; the pin guard enforces it for fork-owned files |
| Treating a Dependabot PR as trusted | A compromised action tag | Check the tag-to-commit lookup, not only CI status |

## UX Pitfalls

Operator-facing, since this is a connector:

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Silent timeout behaviour change | A connector that works on gg6 starts retrying or failing | Rollout line with the effect, the default, and the duplicate-risk wording |
| Rollout note claims stronger than the evidence | A false "safe" (gg1, gg3 incidents) | Label every claim; quantifiers match the measurement; independent review before the gate |
| FUT-05 only in a rollout row, not named by ID | Operator cannot find the rule | Name FUT-05 in the note, as the milestone scope says |
| Rollback text untested on the new tip | Rollback fails with 97 conflicts (gg1) | Run the rollback commands on a dry-run merge and the real merge |

## "Looks Done But Isn't" Checklist

- [ ] **F-1:** the test counts closes on the writer, `chcTmp` and the Keeper provider paths, on V2 and V1, including the failed-`start()` exit and a double `stop()`; the other sites are fixed or recorded with an owner.
- [ ] **F-2:** a stalled-stub test asserts retriable classification *through `ProxySinkTask.put`* under `errors.tolerance=none` and `all`; the operator's own `socket_timeout` still wins (order pinned); `async_insert` still unset (CFG-02 anchor unchanged).
- [ ] **Wrapped `DataException` / FUT-05:** the user decision is recorded; changed matrix rows are named; the other 180-odd rows are byte-identical before and after.
- [ ] **OI-35-1:** state key, KeeperMap row schema and BEFORE/AFTER sequence untouched (`git diff` evidence); the partition/offset scope is stated; `onPartitionRemoved` is covered or recorded.
- [ ] **IN-04:** the Docker-free `custom_*` pin runs with Docker stopped and is labelled a client-version tripwire.
- [ ] **PR #21:** four workflows carry the same commit SHA with `# v6.4.1`; `integrationTests.yaml` blob unchanged; FORK-DELTA rows 76/79 updated; all four guard classes green.
- [ ] **PR #22:** full suite twice on the bump commit; gson allow-list rows planned.
- [ ] **Anchors:** `fork-anchors.txt` counts re-measured after every `src/main` edit; `verify-merge-invariants.sh` passes at the tip.
- [ ] **Allow-list:** generated last from the final tree; dry run from a scratch branch read, `0 unallowed, 0 stale rows`.
- [ ] **Connect API guard:** `connect-api-references.txt` unchanged or re-blessed with the documented command.
- [ ] **Release:** CHANGELOG date set at merge time; rollout note reviewed by an orchestrator-spawned reviewer until a round finds no Warning; rollback commands run on the post-publication tip; exactly one green `tests.yaml` run on the published SHA; tag created by the publish run.
- [ ] **Suite:** two full runs on ClickHouse `latest` x client V2 with the JDK actually used recorded; `testUnit` with Docker unreachable.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| F-1 closed too early (premature close, NPE) | LOW | Revert the single commit; the helper is `AutoCloseable`, the fix is additive |
| F-2 timeout fires and DLQs landed batches | HIGH (data semantics) | Roll back to the gg6 tag via the tested rollback; reconcile DLQ records against table content; raise the timeout |
| Classification change breaks DLQ routing | MEDIUM | Revert the commit; the matrix pins show the exact rows that moved |
| Poison-pill retriable code | LOW | Remove the code from `additionalRetriableErrorCodes` and restart the task; state is still `BEFORE`, the retry sends the identical batch |
| State key moved (OI-35-1 done wrong) | HIGH | Revert; rows under the old key are orphaned until restored; stop for a user decision before any key change |
| Allow-list stale at publish | LOW | Regenerate from the dry-run report on the final tree, rerun the dry run |
| Hand-pushed or wrongly pinned tag | MEDIUM | Delete only a tag nobody consumed; recheck the tag-absent gate; never move a published tag |
| OpenJ9 crash in a gate | LOW | Delete the dumps one by one, re-run once, record both; use `-PjavaVersion=21` |
| Dependabot merge on `origin` diverged the branch | LOW | `git fetch`, merge `origin/custom_build` (never rebase), re-run anchors with an exemption row only if needed |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 F-1 three leak sites | P-CODE | Close-count tests on writer, `chcTmp`, Keeper (or recorded non-goal); measured V1/V2 cycles |
| 2 Premature/double close | P-CODE | Null, double-stop and failed-start tests; reflection-swap tests still green |
| 3 F-2 timeout and classification | P-CODE, P-DOC | Classification pin through `ProxySinkTask.put`; stalled-stub test; option-order pin; rollout line |
| 4 Retry classification and DLQ routing | P-CODE (after user decision), P-DOC | Matrix row-diff equals the intended set; rows C2/C3 and FUT-05 in the note |
| 5 `doLogic` / OI-35-1 | P-CODE | Empty diff on state key and Keeper; red-first partition and revocation pins; `ProcessingTest` 14/14 |
| 6 Upstream collision and anchors | P-CODE, P-REL | `verify-merge-invariants.sh` at tip; `git diff --stat` limited to intended files; upstream recheck |
| 7 Mockito / OpenJ9 | P-CODE, P-REL | No Mockito in new tests; two-run gate with JDK recorded |
| 8 SHA re-pin and Dependabot | P-DEP | Four guard classes, tag lookup, `integrationTests.yaml` blob, FORK-DELTA rows |
| 9 One-tree allow-list | P-REL | Dry-run report `0 unallowed, 0 stale rows` on the final tree |
| 10 Release gates | P-REL | Gate checklist with owners; one green run on the published SHA; tag from the publish run |
| 11 Connect API guard | P-CODE | `ConnectApiReferencesUnitTest` green without a hand edit |
| 12 Stall test design | P-CODE | Docker-free, millisecond timeouts, both tolerance settings |
| 13 F-3 harness | P-CODE | Per-connector counting everywhere; local `integrationTest` result recorded |
| 14 gson 2.15.0 | P-DEP | Two full suites on the bump commit; allow-list rows |
| 15 Stale verification | P-REL / close | Audit run before close; override planned or avoided |
| 16 Phase 37 dispositions | P-DOC, P-CODE | One recorded decision per row; anchors updated with any removal |
| 17-19 Minor | P-REL, P-CODE | `git status` clean of `.bump-*`; changed-files-only Spotless |

## Sources

- `.planning/milestones/v1.7.0_gg6-maintenance-phases/36-client-0-11-0-audit-bump/36-FOLLOWUPS.md` (F-1, F-2, F-3) and `36-REVIEW.md` (WR-01/02, IN-01..06) [review]
- `.../34-upstream-tail-merge/34-REVIEW.md`, `34-VERIFICATION.md` (open items: wrapped `DataException`, FUT-05, ten `ServerException` sites), `34-RESEARCH.md` [review]
- `.../35-buffered-offset-commit-under-topic-renaming-smts/35-REVIEW.md` and `35-BUF03-RECORD.md` (OI-35-1..3, state-key stop rule) [review]
- `.../37-ci-hardening-advisory-debt/37-REVIEW.md`, `37-REVIEW-UF.md`, `37-ADVISORY-LEDGER.md`, `37-RELEASE-RECORD.md` [review]
- `.../38-release-publication/38-REVIEW-DISPOSITION.md` (43 findings, 12 rounds; WR-08 carried) and `.../33-dependency-sweep-protobuf-decision/33-02-SUMMARY.md`, `33-RESEARCH.md` (pin-absorption pattern) [review]
- `.planning/RETROSPECTIVE.md` (gg3 to gg6 lessons, cross-milestone trends) and the memory notes: OpenJ9 Mockito crash, Dependabot merges on `custom_build`, Phase 11/16/26 orchestration gotchas [retro]
- Source read in this session: `ClickHouseWriter` (`start`, `stop`, `awaitInsertResponse`, `getMutationRequest`), `ClickHouseHelperClient` (constructor, `createClientV2`, `close`, builder defaults), `KeeperStateProvider`, `BaseStateProviderImpl`, `Processing.doLogic`/`doInsert`, `ProxySinkTask`, `ClickHouseSinkTask`, `Utils.handleException`, `.github/workflows/release.yaml`, `.github/dependabot.yml`, `.github/scripts/bump_version.py`, `.planning/scripts/fork-anchors.txt`, `confluent-archive-allowlist-v1.7.0_gg6.txt`, `connect-api-references.txt`, `CountingHelperClient` [code]
- `gh pr list` on `ne1r0n/clickhouse-kafka-connect` (open PRs #21, #22) and on the upstream repo (open PRs #861, #855, #857, state as of 2026-10-26) [MEDIUM: live state, will drift]
- Gson release notes page (https://github.com/google/gson/releases), summarised by a fetch tool [MEDIUM]
- ClickHouse deduplication settings (`replicated_deduplication_window`, `non_replicated_deduplication_window`) from ClickHouse documentation knowledge, not re-verified here [MEDIUM]
- Not run: no suite, no stall test and no leak measurement was executed for this research; every "[inferred]" statement is a hypothesis for the owning plan to confirm.

---
*Pitfalls research for: `v1.7.0_gg7` maintenance milestone on the ClickHouse Kafka Connect `custom_build` fork*
*Researched: 2026-11-01*
