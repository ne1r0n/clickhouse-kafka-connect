# Phase 10: Test Harness Reconciliation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-20
**Phase:** 10-Test Harness Reconciliation
**Areas discussed:** Shared-container strategy, ClickHouse image default (`latest` → `25.8.18.1` reversion), `ClickHouseTestHarnessTest` un-exclude, MBean leak sweep shape, Acceptance gate

---

## Area selection

| Option | Description | Selected |
|--------|-------------|----------|
| Shared-container strategy | HARN-03: restore per-JVM-fork sharing over upstream's `PER_CLASS` shape, or record the fallback | ✓ |
| The `latest` → `25.8.18.1` reversion | Restore the fork's default (+ `alwaysPull`), or accept upstream's LTS pin | ✓ |
| `ClickHouseTestHarnessTest` un-exclude | Restore `createProps()`/`createClient()` on `ClickHouseBase`, or adapt the test | ✓ |
| MBean leak sweep shape | Restore verbatim (WARN + unregister), or make leaks fail the test | ✓ |

**User's choice:** all four.
**Notes:** Presented alongside a measured starting state showing `HARN-04`, `HARN-05` and `HARN-06` already satisfied by the merge and `HARN-07` de facto proven, plus one regression found during the scout that no requirement names (the image default).

---

## Shared-container strategy

### Q1 — restore sharing, measure first, or take the fallback?

| Option | Description | Selected |
|--------|-------------|----------|
| Restore per-JVM sharing | Static container + lazy start guard; `tearDown` drops the class database instead of stopping the container. Keeps the fork contract and protects CI's single-fork runner from 27 sequential container starts | ✓ |
| Measure both, then decide in-phase | Take HARN-03's wording literally: time the full suite both ways, then pick on the number | |
| Keep upstream's per-class container | Record the fallback HARN-03 explicitly permits. Smallest delta, strongest isolation, already green at 645 tests | |

**User's choice:** Restore per-JVM sharing.
**Notes:** Decisive context put on the table before the question — `maxParallelForks = availableProcessors() / 2` resolves to **1** on a 2-core GitHub runner, so per-class containers mean 27 *sequential* container starts on exactly the machine Phase 11 measures against `timeout-minutes: 45`. The locally-measured 3m01s is not representative of CI.

### Q2 — the two Toxiproxy classes

| Option | Description | Selected |
|--------|-------------|----------|
| Shared by default, explicit opt-out | Overridable hook on `ClickHouseBase`; the two proxy classes override it and get their own container + `Network`. Collision impossible by construction | ✓ |
| Share for all, re-home the networks | Attach the shared container to each proxy class's own `Network` at class setup | |
| Share for all, prove it by running | No opt-out; let the full-suite gate decide | |

**User's choice:** Shared by default, explicit opt-out.
**Notes:** Both `ClickHouseSinkTaskSchemalessProxyTest:58-60` and `ClickHouseSinkTaskWithSchemaProxyTest:81-83` call `.withNetwork(db.getNetwork())` **and** claim the same literal alias `"toxiproxy"` — so under one shared container the collision fires whenever Gradle assigns them to the same fork, which varies run to run. Also surfaced: `ClickHouseHelperClientTest:105-118` runs `CREATE USER` / `GRANT … ON *.*`; names are timestamped so they don't collide, but they now accumulate on a server that outlives the class.

### Q3 — upstream's server-log extraction

| Option | Description | Selected |
|--------|-------------|----------|
| Keep it, key by class name | Copy while the container is still alive into `server_<SimpleClassName>.log`. No JVM shutdown hook — Phase 1 ruled one out | ✓ |
| Drop it | Restores the pre-merge `tearDown` exactly; loses server-side evidence on a failing live run | |
| Keep it only for opt-out classes | Upstream's shape untouched for the two own-container classes; the 25 shared classes lose the diagnostic | |

**User's choice:** Keep it, key by class name.
**Notes:** Flagged as non-negotiable alongside this question — `tearDown()` must now drop the class database, because `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` asserts zero *other* `kafka_connect_test_%` databases exist on the server. Upstream got that for free by stopping the container.

**Continue?** → Next area.

---

## ClickHouse image default

### Q1 — restore `latest`?

| Option | Description | Selected |
|--------|-------------|----------|
| Restore `latest` + Docker-free canary | Set the default back and add a `testUnit`-visible test pinning it, so the next sync cannot drop it silently either | ✓ |
| Restore `latest`, no new canary | One-line fix; rely on Phase 11's CI verification | |
| Keep upstream's `25.8.18.1` pin | Smallest fork delta; lean on CI's explicit env | |

**User's choice:** Restore `latest` + Docker-free canary.
**Notes:** Framed as the seventh entry for STATE.md's "vanishes with no conflict marker and no compile error" list — and the only one found *after* the phase that would normally have caught it. It hid because `.github/workflows/tests.yaml:33` pins `CLICKHOUSE_VERSION: latest` in the job env, so CI was always right; only local runs moved, and local runs are where every GSD gate since the merge took its evidence.

### Q2 — `alwaysPull()`

| Option | Description | Selected |
|--------|-------------|----------|
| Restore, but only for floating tags | `alwaysPull()` for `latest`, cached pull for a pinned `CLICKHOUSE_VERSION` | ✓ |
| Restore unconditionally | Exactly the pre-merge shape and a recorded Phase 1 decision; pointless re-pull work for pinned versions, breaks offline runs | |
| Leave it off | Fastest, works offline — but `latest` silently means "whatever I pulled last" | |

**User's choice:** Restore, but only for floating tags.

### Q3 — ownership if the `latest` gate goes red

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 10, with a two-server comparison run | Gate on `latest`; if red, re-run that class on `25.8.18.1` to separate "harness broke it" from "server changed", then decide at a checkpoint | ✓ |
| Phase 10 owns everything `latest` surfaces | Straight application of the milestone's locked rule; risk is a 26.x DESCRIBE change ballooning a harness phase | |
| Harness failures only; hand the rest to Phase 11 | Phase 8 D-12's re-baselined gate shape | |

**User's choice:** Phase 10, with a two-server comparison run.
**Notes:** Phase 9's green 645/0/0/18 was measured on `25.8.18.1`. `latest` is now 26.x — the band where `DESCRIBE` output for `JSON`/`Dynamic` sub-columns is most likely to have moved, which is exactly what `DESC-01..04` and `shouldSkipDescribeRow` pin.

**Continue?** → Next area.

---

## `ClickHouseTestHarnessTest` un-exclude

### Q1 — how the canary gets its helpers back

| Option | Description | Selected |
|--------|-------------|----------|
| Adapt the test to the post-merge API | Redirect to `getBaseProps()` + `ClickHouseTestHelpers.createClient(...)`, as Phase 7 did for 12 sibling files under D-06/D-09 | ✓ |
| Restore the helpers on `ClickHouseBase` | Canary stays byte-identical — its unchanged diff is itself evidence the contract did not move (Phase 9 D-10's argument) | |
| Restore them as thin deprecated delegates | Byte-identical canary, one implementation, but a shim with exactly one caller | |

**User's choice:** Adapt the test to the post-merge API.
**Notes:** The tension was put on the table explicitly — this canary is what *proves* the harness contract, so rewriting it means the proof and the thing proved change in the same commit, inverting Phase 9 D-10's reasoning. Accepted knowingly; CONTEXT.md records it and D-11 splits the API redirection from the contract change across commits.

### Q2 — which new behaviours the canary pins (multi-select)

| Option | Description | Selected |
|--------|-------------|----------|
| The opt-out hook | A class overriding the hook gets its own container and the shared-start counter does not move | ✓ |
| The per-topic MBean sweep | Leak a topic MBean deliberately, assert `…,topic=<t>` is gone after `@AfterEach` | ✓ |
| The class-name-keyed log copy | Assert `server_<SimpleClassName>.log` exists and is non-empty | |

**User's choice:** the opt-out hook and the per-topic MBean sweep; not the log copy.
**Notes:** Without the opt-out pin, `assertEquals(1, sharedContainerStarts())` is only true by luck of fork assignment. The log copy was left unpinned as a brittle assertion on build output for a diagnostic convenience, not a contract.

**Continue?** → Next area.

---

## MBean leak sweep

### Q1 — what the sweep does on finding a leak

| Option | Description | Selected |
|--------|-------------|----------|
| Restore verbatim — WARN + unregister | The sweep stops a leak crossing into the next test; the canary's `@Order(3)`→`@Order(4)` pair supplies the strict proof | ✓ |
| Unregister, then fail the test | Loud and un-ignorable — but immediately reds the two `ClickHouseSinkTaskJsonInsertFormatTest` methods Phase 9 deliberately re-enabled unchanged | |
| Unregister silently at DEBUG | Quietest — but a genuine leak regression becomes invisible | |

**User's choice:** Restore verbatim — WARN + unregister.
**Notes:** Established before the question that HARN-02's second half needs no new machinery — `MBeanServerUtils.java:22-23` rethrows `InstanceAlreadyExistsException` as a `RuntimeException`, so a name collision is already a test failure. A green full suite *is* the evidence. Also noted: the pre-merge sweep pattern `com.clickhouse:type=ClickHouseKafkaConnector,*` already matches upstream's per-topic names (`SinkTaskStatistics:188-189` composes them as the task name `+ ",topic=" + topic`), so "extended to cover per-topic" is likely free — to be confirmed by command, with a widened pattern as the fallback.

---

## Acceptance gate

| Option | Description | Selected |
|--------|-------------|----------|
| `testUnit` (Docker stopped) + full suite on `latest`, **twice** | A shared container makes 27 classes order-dependent within a fork for the first time; fork assignment varies run to run, so one green run proves less here than in Phase 9 | ✓ |
| `testUnit` (Docker stopped) + one full suite on `latest` | The Phase 8/9 precedent applied straight | |
| `testUnit` only, defer the live run to Phase 11 | ROADMAP criterion 1 read literally | |

**User's choice:** `testUnit` (Docker stopped) + two consecutive clean full runs on `latest`.
**Notes:** Phase 1 recorded 0 flakes at 6 forks for the pre-merge shape; the double run re-establishes that claim post-merge, at a cost of roughly 6 minutes.

---

## Closing check

**Question:** Anything still unclear, or ready for context?
**User's choice:** "I'm ready for context." Offered but declined: how `HARN-04`/`05`/`06`/`07`'s evidence acts are discharged and which of the 17 `MERGE-CHECKLIST.md` rows flip here, whether the `ClickHouseCluster` / cloud branches need saying anything about, and the commit sequencing across five behaviours — all recorded as Claude's Discretion in CONTEXT.md instead.

---

## Claude's Discretion

- **D-10** — discharging `HARN-04`/`05`/`06`/`07` as recorded evidence acts rather than work, with each command re-run at execution time rather than cited from CONTEXT.md.
- **D-11** — the six-commit sequence, one behaviour per commit, with `spotlessApply` scoped to each commit's own files.
- The opt-out hook's exact shape (`protected boolean` override vs. `@Tag` vs. annotation) — only D-02's counter rule is binding.
- Wave shape: the sweep and the image default are independent of the container work and of each other; the canary un-exclude depends on all three; the gate is last.
- Cloud and cluster branches left untouched; existing `Assumptions.assumeFalse(isCloud, …)` guards stand.
- No new JMX metric — this phase changes no production insert path.

## Deferred Ideas

- Whether `tests.yaml` should stop setting `CLICKHOUSE_VERSION` explicitly once the default is correct — Phase 11's file.
- **P4-WR-01** — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`; the permissive sweep keeps it an accepted follow-up rather than a Phase 10 blocker.
- **CR-01** — `.planning/scripts/verify-merge-invariants.sh:44` fails open on a `git grep` error; unowned, due before Phase 11's publication gate.
- `/gsd-secure-phase 07` and `/gsd-secure-phase 09` — both lack a `SECURITY.md` while security enforcement is on.
- `maxParallelForks = availableProcessors() / 2` is untuned for the post-merge suite; revisit with a measured number in Phase 11 alongside the `timeout-minutes: 45` check.
- R-01's fail-fast fix — unaffected by this phase.
- The three open Warning findings in `07-REVIEW.md` — not harness work.
