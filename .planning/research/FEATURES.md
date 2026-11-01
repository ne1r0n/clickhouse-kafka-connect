# Feature Research

**Domain:** Maintenance milestone `v1.7.0_gg7` on a fork of the ClickHouse Kafka Connect sink (follow-up debt, not new connector features)
**Researched:** 2026-11-01
**Confidence:** HIGH for current behaviour and what the pins must assert (read from the repo and the Phase 34-37 artifacts, `upstream/main` diffed directly). MEDIUM for the F-2 mechanism choice and the Connect-runtime claim in R-1 (reasoned, not measured). LOW is not used anywhere.

Scope: only the NEW work of this milestone. Existing features live in `.planning/PROJECT.md`. "Feature" here means "a follow-up, as a requirement-sized unit with its expected post-fix behaviour".

## Identifier warning (read first)

The ID `IN-04` means three different things in the source artifacts. The roadmap must not mix them.

| Where | What it is | In this milestone? |
|-------|-----------|--------------------|
| `MILESTONES.md` Known gaps for gg6, and `38-05-SUMMARY` ("the optional `custom_` forwarding pin of IN-04 was not added") | No Docker-free pin for the `custom_*` forwarding of client 0.11.0 | **Yes, this is the milestone's IN-04** (category E) |
| `36-REVIEW.md` IN-04 | `ClientErrorShapes` has an inverted assertion (`assertFalse(!m.find())`) and a two-value `Line` assumption in `clientVersion()` | Optional test hygiene, fold into E |
| `35-REVIEW.md` IN-04 | `ClickHouseSinkTask.close` passes original-key partitions to `onPartitionRemoved`, which holds transformed keys | Resolved by OI-35-1 (category D) |

## Feature Landscape

### Table Stakes (the milestone is incomplete without these)

| # | Requirement | Expected behaviour once fixed | Complexity | Notes |
|---|-------------|-------------------------------|------------|-------|
| A | **F-1: writer closes its helper client** | `ClickHouseWriter.stop()` closes `chc` (`ClickHouseHelperClient.close()` already exists and calls `client.close()`), after the scheduled mapping executor has terminated. It is null-safe and idempotent (`stop()` before `start()`, or twice). One V2 client with its connection pool is released per task stop, so workers that restart or rebalance often stop growing. Also close `chc` on the `start()` paths that return `false` after `chc` was built (ping failed, version unreadable, no tables). | LOW to MEDIUM | `ClickHouseWriter.java` is a HIGH-RISK file but the change is not in the insert path. **Upstream has the same bug** (`upstream/main` `stop()` only shuts the executor), so this becomes fork delta in a file that is otherwise upstream-shaped; add it to FORK-DELTA and the anchors file. |
| B | **F-2: the insert timeout is enforced on the call, not on a future that is already done** | With `async=false`, `client.insert(...)` returns an already completed future, so `future.get(clickhouseClientInsertTimeoutMs)` never fires and `socket_timeout` defaults to 0 (unbounded). After the fix a stalled server or connection makes the insert fail after about `clickhouseClientInsertTimeoutMs` with a `RetriableException`. Recommended mechanism: when the operator has not set `socket_timeout` in `jdbcConnectionProperties`, `createClientV2` applies `setSocketTimeout(clickhouseClientInsertTimeoutMs)`; an operator-set `socket_timeout` still wins (the property loop runs later). Keep `awaitInsertResponse` as is (it still bounds the async=true case). | MEDIUM | See "Decision needed" under B. Retry classification already exists: client 0.11.0 read-timeout text is pinned by `ClientTimeoutPrefixPinUnitTest` and `Utils.handleException` maps `SocketTimeoutException` and the `Read timed out after` prefix to `RetriableException`. In exactly-once mode the retry rebuilds the identical block, so dedup holds; in at-least-once mode a timed-out insert that actually landed is re-sent (already the documented behaviour of the existing timeout). |
| C1 | **Wrapped `DataException` in `Utils.handleException` and `Processing`: characterise, do not change** | Pin today's behaviour with Docker-free tests: (1) a writer `DataException` raised inside `Processing.doInsert` reaches `handleException` wrapped in `RuntimeException(queryId, e)`, so `e instanceof DataException` is false and it surfaces as `RuntimeException("Number of records: N", e)`, not retriable, with the `DataException` in the cause chain; (2) a `DataException` whose cause chain holds a server error with a retriable code (default or added) is retriable; (3) a `DataException` whose root cause is `SocketTimeoutException` or `UnknownHostException` is retriable under `errors.tolerance=all`, and under `none` the direct-`DataException` branch rethrows it unchanged. | LOW | The ROADMAP wording "A DataException is never reclassified as retriable" was interpreted narrowly in Phase 34 and the wrapped cases are retriable by upstream's root-cause-first rule. Retriable-for-transient-cause is arguably correct. Tests only, no `src/main` change unless the user decides otherwise (see anti-features). `Utils.java` already differs from upstream (import order, null-safe `msg` guard), so a pin must not assume byte-equality with upstream. |
| C2 | **FUT-05: only transient codes belong in `additionalRetriableErrorCodes`** | The gg7 rollout note and FORK-DELTA name FUT-05 by ID and state the consequence: a listed code raises `RetriableException`, which Connect redelivers with no retry budget and outside `errors.tolerance` and the DLQ, so a permanent error code stalls the task forever. Upstream's ConfigDef description and its Unreleased CHANGELOG entry already say the same. | LOW | The behaviour itself is pinned by the 19-execution `AdditionalRetriableErrorCodesPinUnitTest` (the 15 default codes including 159, every code 0..1100 on V1 and V2, with the key unset). Nothing in that class needs to change. |
| D1 | **OI-35-1: Direct path with `reportInsertedOffsets=true` under a renaming SMT commits offsets** | `Processing.doLogic` reads `originalTopic` from `record.getSinkRecord().originalTopic()` instead of `topic()`. `lastInsertedOffsets` is then keyed by Connect's own key (measured: `bufT-dev-0=10` instead of none), `preCommit` returns it, and the group commits. `onPartitionRemoved` (35 IN-04) works under a renaming SMT with no further change, because `close()` already passes original keys. Without an SMT, or with the default `reportInsertedOffsets=false`, nothing changes (measured P7). | LOW code, HIGH care | One line in `Processing.java`, a HIGH-RISK file. Measured in Phase 35 (E-06 P6b): all 14 `ProcessingTest` cases stay green. The state key (`topic` local), the KeeperMap row schema and the `BEFORE`/`AFTER` sequence are untouched, and `originalTopic` is not stored by `KeeperStateProvider` (it is restored from the record at line 164), so no KeeperMap row is orphaned. **Upstream is unfixed**: `Processing.java` is currently byte-identical to `upstream/main`, so this makes `Processing.java` a fork-delta file for the first time. It needs a FORK-DELTA row and an anchors row, and it raises rebase cost on a file upstream edits. The user decided in Phase 35 that `Processing.java` is not edited without a decision; this milestone's scope statement is that decision to confirm. |
| D2 | **OI-35-2 and OI-35-3: record as documented limits with a trigger; do not change the state key** | Exactly-once and the in-memory state machine require an injective topic rename and a partition-preserving SMT. OI-35-2 (two sources renamed to one table share one state key; skewed ranges lose or reject the lower-offset source, measured P2-P4) and OI-35-3 (exactly-once buffer groups by original partition while `Processing` keys by transformed topic and partition; not probed) are closed by a documented limit in the rollout note and FORK-DELTA Known costs, plus an optional probe for OI-35-3. | LOW (docs), MEDIUM (probe) | Changing the state key orphans every stored KeeperMap row (first batch after upgrade reads `NONE` and inserts as new, rows have no TTL). That is the stop-for-user-decision case. Also unprobed (35-REVIEW): value-routed fan-out from one source partition (the fork's `ExtractTopic`). |
| E1 | **IN-04: Docker-free pin for the `custom_*` forwarding on client 0.11.0** | Turn the throwaway probe `ZzCustomProbeTest` (asset `36-assets/ZzCustomProbeTest.java.txt`) into a permanent `testUnit` pin on `StubClickHouseServer`: with `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` the V2 request URI holds `custom_myparam=7` and `custom_settings=max_threads%253D3` (the operator's percent-encoding doubled) on 0.11.0. The pin documents the effect; it does not change it (the gg6 note already says `custom_*` keys fail the start-up ping on V2). | LOW | Reuses `StubClickHouseServer` and the existing `ClientEndpointAndOptionsPinUnitTest` style. The pin bites on a client bump, which is its point. |
| E2 | **F-3: harness counts only its own connector** | Any `ConfluentPlatform` helper that counts across `getConnectors()` output is scoped to one connector's task status, as `generateData` already is (E-16 fix). `createConnectorAndWaitUntilRunning` checks task 0 only and first polls after 5 s; it is correct for the sink connector, so leave it or note it. Cleanup of a leftover connector runs in `finally`. | LOW | Test code only (`src/integrationTest`). Needs Docker, so it is verified by one `integrationTest` run (or the nightly), not by `testUnit`. `NIGHTLY_ENABLED` stays with the user. |
| F | **Documents** | `docs/ROLLOUT-v1.7.0_gg1.md` item 6: change "measured" to the gg6 note's label for the `zstd-jni` native and temp folder, and "client V1 of any line" to the two lines observed (the gg1 half of WR-08). FORK-DELTA lines 6, 139, 286: past tense for the publish run. FUT-05 named by ID (C2). | LOW | `docs/ROLLOUT-v1.7.0_gg1.md` was outside gg6's editable set, which is why it was deferred. Operator documents get an independent `gsd-code-reviewer` pass before the publication gate (project rule; recurred in Phases 15 and 24). |
| G | **Phase 37 dispositions** | Each row is closed `fixed` or re-dispositioned with owner and trigger. The `increment-version.yml` header comment names the correct guard class (comment only). | LOW each | See "Phase 37 dispositions" below. Rows touching `src/main` need the user's approval; the milestone's scope statement can serve as it but should name the rows. |
| H | **Dependabot and release** | PR #22 gson 2.14.0 to 2.15.0 and PR #21 `setup-gradle` 6.4.0 to 6.4.1 (SHA re-pin behind the pin guard), each with its own suite gate. `VERSION=v1.7.0_gg7`, CHANGELOG, FORK-DELTA, rollout note with independent review, `--no-ff` merge after human approval, CI green on the published SHA. | MEDIUM (process) | Same shape as the gg6 release. Gson reaches production code; setup-gradle reaches only the workflows. |

### Differentiators (valuable, optional, each needs a user decision)

| Feature | Value | Complexity | Notes |
|---------|-------|------------|-------|
| FUT-05 start-up WARN | One WARN at task start when `additionalRetriableErrorCodes` is non-empty, naming the codes and the no-retry-budget consequence. Operators see the risk without reading the note. | LOW | Touches `ClickHouseSinkConfig` or `ProxySinkTask` (HIGH-RISK file for the config). Adds fork delta against upstream. Do not reject or filter codes (see anti-features). |
| F-2 as a connector setting | Reuse `clickhouseClientInsertTimeoutMs` as the socket timeout (recommended above) instead of adding a new key. | LOW | No new config key means no `ClickHouseSinkConfig` change and no new operator surface. |
| F-1 observable | A DEBUG line, or a JMX counter, for the helper-client close. | LOW | Only if the test seam needs it; AGENTS.md asks for metrics on new behaviour but a close is not a metric-worthy event. |
| OI-35-3 probe | A Docker-free `Processing` probe with a partition-changing SMT shape (several original partitions into one state key) that either shows the hazard or removes the caveat. | MEDIUM | Converts an unprobed claim into a measured one. Pairs with D2. |
| `ClientErrorShapes` hygiene (36 IN-04) | `assertTrue(m.find(), ...)` and the version string on the `Line` enum. | LOW | Test code only, no behaviour. Cheap while E1 is open in the same package. |

### Anti-Features (look good, create problems)

| Anti-Feature | Why requested | Why problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Change `Utils.handleException` so a wrapped `DataException` is never retriable | The ROADMAP wording "never reclassified as retriable" read literally | Upstream code, merged unedited. A `DataException` wrapping server code 241 or a socket timeout is a transient failure; making it fatal turns a retry into a stuck or failed task. The fork's `Utils.java` already differs from upstream. | Pin today's behaviour (C1); reword the criterion. |
| Reject or filter non-transient codes in `additionalRetriableErrorCodes` | FUT-05 reads like "validate the list" | Needs a curated list of "transient" codes that upstream does not have, and an operator may knowingly list one (the feature exists for #847, code 216). Diverges the config from upstream. | Document (C2), optional WARN. |
| A retry budget for listed codes | Fixes the infinite redelivery | Changes Connect redelivery semantics for a `RetriableException` thrown from `put()`; no upstream basis. | Operator guidance: list only transient codes. |
| Change the exactly-once state key to include the original topic (OI-35-2) | Makes many-to-one renames safe | Orphans every stored KeeperMap row; first batch after upgrade inserts as new, rows have no TTL; HIGH-RISK file. | Documented limit: injective rename only. |
| `useAsyncRequests(true)` to make `future.get(timeout)` work (F-2) | Smallest-looking change | Changes the threading and failure model of every insert; a cancelled future may not abort the in-flight HTTP request. | Bound the call itself with the socket timeout. |
| `ClickHouseHelperClient` close via a finalizer or shutdown hook (F-1) | Avoids touching `stop()` | Hides the lifecycle, leaks until GC. | Explicit close in `stop()`. |
| Add a third `createClientV2` option to restore 0.9.6 `custom_*` behaviour (E1) | Fixes the upgrade-breaking item | Out of scope ("client upgrades beyond 0.11.0", no new connector features). The gg6 note records it as a user decision. | Pin the effect (E1); keep the rollout item. |
| Mockito inline mocks for the F-1 test | Quick way to count `close()` | The OpenJ9 inline-mocking GPF crashes the test JVM; gg3 moved the writer tests off it. | Real `StubClickHouseServer`, or a small test seam. |

## Feature Dependencies

```
A (F-1 close)      -- independent of B; same file as B's call sites, so land in separate commits
B (F-2 timeout)    -- needs a stall responder in StubClickHouseServer (new test fixture code)
                   -- uses the existing "Read timed out after" classification (ClientTimeoutPrefixPinUnitTest)
C1 (wrapped DataException pin) -- independent, tests only
C2 (FUT-05 text)   -- feeds F (rollout note), H (release)
D1 (OI-35-1 line)  -- requires user confirmation (Processing.java), then FORK-DELTA + anchors row
D1 --resolves--> 35 IN-04 (onPartitionRemoved)
D2 (limits)        -- feeds F (rollout note); a state-key change would conflict with the no-orphan rule
E1 (custom_ pin)   -- independent; bites on any client bump
E2 (F-3)           -- needs Docker (integrationTest), no src/main dependency
F, G               -- documents; must precede the independent review of operator docs
H                  -- last: needs A..G merged and two equal green suites
```

### Dependency Notes

- **Tests first (the milestone's stated order):** A and B each need a red test before the fix. B's test must use `assertTimeoutPreemptively` so the unfixed code fails the test instead of hanging the suite.
- **A before B in the same file:** both touch `ClickHouseWriter` or `ClickHouseHelperClient`. Separate commits keep the revert surface small (the fork runs in production).
- **D1 changes a high-risk file:** it needs the `BEFORE`/`AFTER` crash-scenario check from AGENTS.md. The evidence already exists in `35-BUF03-RECORD.md`; re-measure it on the new tip rather than quoting it.
- **Anchors and FORK-DELTA:** A (if `stop()` is the only fork delta in that method), B (`createClientV2`) and D1 each add or move a fork-anchors row. `verify-merge-invariants.sh` and the anchors counts are the gate.
- **Gson and setup-gradle (H)** are independent of A to G and can run first or last; each gets its own suite gate.

## What each code requirement must pin

| Req | Red-first test (Docker-free unless noted) | Assertion that must not regress |
|-----|------------------------------------------|--------------------------------|
| A | Start then stop a writer against `StubClickHouseServer`, then show the client is closed (keep-alive socket closed on the stub side, or `chc` use after stop throws). Cases: `stop()` without `start()`, twice, start that returns `false`. | Mapping executor still terminates; `ClickHouseWriterNullMessageUnitTest` and the writer tests stay green. |
| B | Stub that accepts the insert and never answers, `clickhouseClientInsertTimeoutMs` of about 1 s, `async=false` on V2: the put fails with `RetriableException` within a bound. A second case: an operator `socket_timeout` in `jdbcConnectionProperties` wins. | `awaitInsertResponse` async path; a normal insert; `max.poll.interval.ms` validation at `ClickHouseSinkConfig` line 428. |
| C1 | The three cases listed in the table, through `Utils.handleException` and through `ProxySinkTask` with `InMemoryDBWriter`-style failure injection. | The 19-execution `AdditionalRetriableErrorCodesPinUnitTest`. |
| C2 | A docs guard (grep-style, like the existing Docker-free doc guards) that the rollout note names FUT-05. | None. |
| D1 | Flip E-06 P5/P6/P7 into `Processing`/`DirectDeliveryStrategy` pins: key is original topic under a renaming SMT; default config returns Connect's map. Add an `onPartitionRemoved` pin. | `ProcessingRenamedTopicStateKeyUnitTest` S1 and S2 (state key stays the transformed topic) and all 14 `ProcessingTest` cases. |
| E1 | Probe URI contains both `custom_` keys with doubled encoding on 0.11.0. | `ClientEndpointAndOptionsPinUnitTest`. |
| E2 | One `integrationTest` run (or nightly) after the change. | The 54 connector tests. |

## Phase 37 dispositions (recommended handling)

| Row | Recommended | Why |
|-----|-------------|-----|
| `increment-version.yml` header comment (wrong guard class) | Fix: comment only | No behaviour; pin guard unaffected. |
| 26 IN-02 (`shouldSkipDescribeRow` overloads have no `src/main` caller, but are an anchor row) | Re-disposition with the existing trigger, or remove with an anchors edit if the user approves | Dead code but anchored; removal edits `src/main`. |
| 27 WR-01 (`Column` unknown-enum failure withholds its text) | Re-disposition | Showing it needs a pass-through exception type, a design decision that conflicts with the no-record-data rule. |
| 27 IN-01 (`passesThroughUnchanged` short-circuits before the Jackson check) | Re-disposition, or add a test that pins the call sites | A property of the call sites, not a defect. |
| 28 IN-01 (`DeclaredNullability.ESCAPED_NEWLINE` public) | Fix only if tests in two packages are moved; else re-disposition | Making it package-private breaks two packages' tests. |
| 28 IN-05(b) (tighten a null assertion) | Re-disposition unless the user signs off | It changes an existing assertion, and AGENTS.md forbids weakening or deleting tests. |
| UF-37-3 (broad allow-list glob rows) | Fix: narrow the globs if the archive compare stays green, else document | CI-only. |
| Review Infos | Close each by `fixed` or `dispositioned` with an owner and trigger | Same ledger discipline as Phase 37 (`37-ledger-check.py`). |

## MVP Definition

### Launch With (this milestone)

- [ ] A, B, E1 with red-first tests, then the fixes (A, B); these are the only production-behaviour changes besides D1.
- [ ] C1 pins and C2 wording; they are the cheapest closures of the carried items.
- [ ] D1 if the user confirms the `Processing.java` edit; otherwise D2 as a documented limit only.
- [ ] F, G, E2 and H as the document, harness and release work.

### Add After Validation (next milestone)

- [ ] OI-35-3 probe (differentiator), re-open trigger: an exactly-once connector with a partition-changing SMT.
- [ ] FUT-05 start-up WARN, trigger: an operator report of a stuck task with a listed code.

### Future Consideration

- [ ] Next tagged upstream sync (carries the F-1, B and D1 delta against upstream).
- [ ] Upstream workflows still on `@v3`-era actions.

## Feature Prioritization Matrix

| Requirement | Operator value | Cost | Priority |
|-------------|----------------|------|----------|
| B (F-2) | HIGH (an insert can block indefinitely) | MEDIUM | P1 |
| A (F-1) | MEDIUM (leak per task start) | LOW | P1 |
| D1 (OI-35-1) | MEDIUM (only `reportInsertedOffsets=true` + renaming SMT) | LOW code, HIGH care | P1 if confirmed |
| C1, C2, E1 | MEDIUM (pins and wording) | LOW | P1 |
| F, G, H | MEDIUM (release hygiene) | LOW to MEDIUM | P1 |
| D2 | LOW (documented limit) | LOW | P2 |
| E2 (F-3) | LOW (harness only) | LOW | P2 |
| Differentiators | LOW | LOW to MEDIUM | P3 |

## Operator-visible effects to list in the gg7 rollout note

1. **B:** an insert that stalls longer than `clickhouseClientInsertTimeoutMs` (default 4 minutes) now fails with a retriable error where it used to block. Exactly-once is unchanged (identical block, dedup). With at-least-once a timed-out insert that landed is re-sent. Operators who set `socket_timeout` themselves keep their value.
2. **A:** no behaviour change. Worker memory and connection count stop growing across task restarts.
3. **D1:** only for `bufferCount` or the Direct path with `reportInsertedOffsets=true` under a topic-renaming SMT: offsets now commit for Connect's partitions. The first start after the upgrade follows the same "derived, not measured" wording as gg6 item for BUF.
4. **C1, E1:** none (tests only).
5. **C2, D2:** documented limits: FUT-05 and the injective-rename requirement.
6. The MBean `version=` segment and client name read `v1.7.0_gg7`.

## Sources

- `.planning/milestones/v1.7.0_gg6-maintenance-phases/36-client-0-11-0-audit-bump/36-FOLLOWUPS.md` (F-1, F-2, F-3; observed on both client lines) - HIGH
- `.../36-client-0-11-0-audit-bump/36-REVIEW.md` (IN-04 of Phase 36) and `36-assets/ZzCustomProbeTest.java.txt`, `36-04-SUMMARY.md` (E-11 `custom_` probe URIs on both lines) - HIGH
- `.../35-buffered-offset-commit-under-topic-renaming-smts/35-BUF03-RECORD.md`, `35-REVIEW.md` (IN-04), `35-VERIFICATION.md` (OI-35-1..3, measured P1-P7) - HIGH
- `.../34-upstream-tail-merge/34-VERIFICATION.md` (open items 1 and 3), `34-COMMAND-EVIDENCE.md` E-09 and E-10 (wrapped `DataException`, FUT-05 wording) - HIGH
- `.../37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md`, `38-release-publication/38-03-SUMMARY.md` (WR-08 half), `.planning/MILESTONES.md` Known gaps for gg6 - HIGH
- Current source: `ClickHouseWriter.stop()` and `awaitInsertResponse`, `ClickHouseHelperClient.close()` and `createClientV2`, `Utils.handleException`, `Processing.doLogic` (line 137), `BaseStateProviderImpl`, `DirectDeliveryStrategy`, `ClickHouseSinkTask.close` - HIGH
- Upstream: `git show upstream/main` for `ClickHouseWriter.stop()` (no client close), `Processing.java` (identical to the fork) and `ChunkFlusher` (still `record.topic()`); `gh search issues` on `ClickHouse/clickhouse-kafka-connect` (#587 retriable exceptions, #847 code 216, #701 unclosed test resources; no upstream issue found for F-1, F-2 or OI-35) - HIGH for the diffs, MEDIUM for "no upstream issue" (search is not exhaustive)
- Client 0.11.0 API surface from `javap` on `client-v2-0.11.0.jar`: `Client.Builder.setSocketTimeout`, `setExecutionTimeout`, `useAsyncRequests`, `ClientConfigProperties.SOCKET_OPERATION_TIMEOUT` - HIGH for existence, MEDIUM for the F-2 mechanism choice (not run)
- Not verified: that Kafka Connect does not call `SinkTask.stop()` when `start()` throws (basis for closing `chc` on the failed-start paths in A); confirm against the Connect worker source in the phase's own research.

---
*Feature research for: v1.7.0_gg7 follow-up debt (ClickHouse Kafka Connect fork)*
*Researched: 2026-11-01*
