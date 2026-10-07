# Phase 8: Orchestration & JMX Re-plumbing - Context

**Gathered:** 2026-09-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Restore the JMX `connector=<name>` segment in its new home. Upstream `v1.6.0` deleted the fork's carrier (`ProxySinkTask.buildMBeanName`) and moved all MBean naming into `util.jmx.SinkTaskStatistics`, which renders no `connector=` segment at all — so this is a deliberate design re-plumb, not a patch re-apply. The milestone's "prefer upstream" policy explicitly does **not** apply here: upstream's version fails JMX-01/JMX-02.

Delivers: `connectorName` threaded `ClickHouseSinkTask.start()` → `ProxySinkTask` → `SinkTaskStatistics`; `connector=<name>` in both the sink-task and the per-topic ObjectName, with the null-version and colon sanitization retained; `MBeanNameUnitTest` re-targeted and un-excluded from the test compile; `ORCH-01` (upstream's `DeliveryStrategy`/`ChunkFlusher` layering above, 5-arg `Processing` below) and `ORCH-04` (`Utils.java` / `Processing.java` explicitly read, `UtilsTest`/`ProcessingTest` on upstream's versions) discharged by verification and record; `FREE-01` closed in writing.

Gate: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` green (Docker-free), **plus** a full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` run whose pass condition is re-baselined — see D-12.

Out of this phase: the metadata-free `insertFormat=json` keep/drop decision and `jsonInsertColumns`' new consumer (Phase 9); the harness contract — `@AfterEach` MBean leak sweep extension, shared-container strategy, Testcontainers `2.0.5` verification, `ClickHouseTestHelpers` canonicalisation, the six conflicted sink-task test files (Phase 10); publication and the SUITE-03 record (Phase 11).

**Measured starting state** (`git diff v1.6.0 -- <path>`, taken at discussion time — do not re-derive, but do re-confirm before planning):

| Path | Diff vs `v1.6.0` | Consequence |
|---|---|---|
| `sink/ProxySinkTask.java` | **0 lines** | 5-arg `Processing` already constructed at `:54`; no `connectorName` field; 2-arg ctor |
| `sink/processing/Processing.java` | **0 lines** | ORCH-01 has **zero** stale 4-arg call sites anywhere in the tree |
| `util/jmx/SinkTaskStatistics.java` | **0 lines** | statics `getMBeanName(int)` / `getTopicMBeanName(int, String)`; version from `Version.ARTIFACT_VERSION`; **no `connector=`** |
| `sink/processing/ProcessingTest.java` | **0 lines** | already upstream's version — ORCH-04's test half is satisfied |
| `sink/util/UtilsTest.java` | **0 lines** | already upstream's version |
| `util/Utils.java` | 29 lines, **Spotless import reorder only** | `isServerError()` already covers `com.clickhouse.client.api.ServerException` → **FREE-01 is already true in the tree**, record-only |
| `sink/ClickHouseSinkTask.java` | 16 lines | Phase 7's `@Override` removal only; `props.get("name")` is **gone** |

So the only production behaviour this phase must *write* is the JMX segment. ORCH-01, ORCH-04 and FREE-01 are discharged by reading, verifying and recording — not by code.

</domain>

<decisions>
## Implementation Decisions

### JMX Name Carrier

- **D-01:** Connector-aware naming lives in **`SinkTaskStatistics`**, per ROADMAP criterion 1. It takes `connectorName` as a **constructor argument** and holds it as a field alongside the existing `taskId`; `getMBeanName()` and `getTopicMBeanName(topic)` become **instance** methods. A static **`buildMBeanName(String connectorName, int taskId, String version)`** is the pure function that holds the actual format string, the `null → "unknown"` version default and the `: → _` sanitization — this is what `MBeanNameUnitTest` re-targets at.
  - Rejected: keeping everything static with `connectorName` as a leading parameter — `registerMBean()` / `unregisterMBean()` / `createTopicStatistics()` all need the value, so it becomes a field either way, just implicitly.
  - Rejected: re-adding the carrier to `ProxySinkTask` and passing a finished name string down. Smallest test diff, but it splits naming across two classes (topic MBeans are still named internally by `SinkTaskStatistics`) and contradicts ROADMAP criterion 1.

- **D-02:** The connector-blind static **`getMBeanName(int taskId)` is deleted**, and all four upstream call sites are updated to pass `null` explicitly:
  - `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java:313` (sink-task name), `:334` (**topic** name, via `getTopicMBeanName`), `:392` (sink-task name)
  - `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java:75` (sink-task name)

  Neither file sets a `name` prop, so `null` is exactly what those tasks register. The rejected alternative — keeping a 1-arg overload that delegates with `null` — is correct only by that coincidence: the day someone adds `props.put("name", …)` to one of those tests it silently looks up an MBean that was never registered and fails as a bare `InstanceNotFoundException`.
  - Both files are currently **byte-identical to `v1.6.0`**. `ClickHouseSinkTaskTest.java` is a **Phase-10-owned** `survives-as-is` checklist row and `FailureTest.java` is not in the conflict inventory at all — so Phase 8 **appends a note** to the `ClickHouseSinkTaskTest.java` row recording the four-site edit and its commit, and does **not** flip it. Same bookkeeping pattern as Phase 7's D-03/D-07.
  - **Reversibility:** reversible — four call sites, one static.

- **D-03:** `connectorName` reaches `ProxySinkTask` by **restoring the fork's pre-merge 3-arg constructor**: `ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, String connectorName)`, called from `ClickHouseSinkTask.start()` with a raw `props.get("name")`. There is exactly one call site. The raw `get` is deliberate and already pinned: an absent `name` yields the four-character string `null` in the rendered name (JMX-02), an empty one yields `connector=` followed immediately by `name=`.
  - Rejected: retaining `connectorName` on `ClickHouseSinkConfig`. It would grow the fork's delta on the file with the worst rebase surface (fork +59 / upstream +227) to hold a value that is a Connect-framework key rather than a `ConfigDef` option, with exactly one consumer.
  - Rejected: a post-construction `setConnectorName()` — works only because `registerMBean()` happens late, i.e. temporal coupling.

- **D-04:** `MBeanNameUnitTest` is re-targeted by a **mechanical carrier swap only**: `ProxySinkTask.buildMBeanName` → `SinkTaskStatistics.buildMBeanName` in the five call sites and the Javadoc `@link`. **Every assertion string stays byte-identical**, including the `0.9.6` version literals. Version stays an explicit parameter, so the test keeps pinning the `null → unknown` and colon branches regardless of what production feeds in, and the diff proves the naming contract did not move — only its home did.
  - Rejected: replacing the literals with `Version.ARTIFACT_VERSION`. A test that computes its expectation from the same source it is checking pins less than it appears to, and a `VERSION` bump would then edit the expected strings.

- **D-05:** `build.gradle.kts:448`'s `exclude("**/MBeanNameUnitTest.java")` is **deleted by this phase** — it was a user-approved temporary carve-out from Phase 7 (commit `b25a71f1`), and leaving it silently drops a canary from the suite. `exclude("**/ClickHouseTestHarnessTest.java")` at `:450` stays; it is Phase 10's to remove. Verify only one exclude remains afterwards.

### Version Segment

- **D-06:** The `version=` segment is sourced from **`Version.ARTIFACT_VERSION`** — upstream's source, currently `v1.6.0_gg1` (Phase 6 Recorded Fact #4: `VERSION` file → `generateVersionFile` task → the constant). The fork's pre-merge read of `ClickHouseClientOption.class.getPackage().getImplementationVersion()` (`0.9.6`, the *client jar's* manifest) is **dropped**. Semantically it is the connector's MBean, not the client's; keeping upstream's source holds that line byte-identical and shrinks the fork delta, which is the milestone's stated goal.
  - **Consequence to carry, not to fix:** `ARTIFACT_VERSION` is a compile-time literal — it can never be `null` and contains no colon. JMX-03's two branches therefore become **unreachable from production** while remaining a pinned contract of `buildMBeanName`. They stay (ROADMAP criterion 1 says colon sanitization is retained) and `MBeanNameUnitTest` keeps exercising them through the explicit parameter.
  - **Reversibility:** reversible in code, but see D-07 — the rendered name is what operators scrape.

- **D-07:** The operator-visible name change is recorded as a **named fact in `.planning/MERGE-CHECKLIST.md` § Recorded Facts** — old string → new string, one row. That file is the single source Phase 11's SUITE-03 dropped-changes record reads from, and it is where Phase 6's SYNC-04/SYNC-05 and Phase 7's FREE-02/FREE-03 facts already live. Rejected: pinning it with a test, which would couple a test to the build-generated constant — the weakness D-04 rejects.

### Per-topic MBean (ORCH-03)

- **D-08:** `getTopicMBeanName(topic)` is **composed**, not duplicated: `buildMBeanName(connectorName, taskId, version) + ",topic=" + topic`. The two names then share a literal prefix and structurally cannot drift — same null-version default, same colon handling, same key order. The key order `type,connector,name,version,topic` **falls out** rather than being chosen.

  ```
  com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=v1.6.0_gg1
  com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=v1.6.0_gg1,topic=orders
  ```

  Rejected: a symmetric `buildTopicMBeanName(connectorName, taskId, version, topic)` static — two format strings holding one contract, so a future edit to one (e.g. quoting hostile names) silently leaves the other behind. This is the same anti-drift reasoning Phase 7's WRT-01 used to make both RowBinary gates share one message builder.
  Rejected: appending `,connector=…` to upstream's existing topic format — the sink-task and topic names would disagree on where `connector` sits, so neither reads as a prefix of the other. (`ObjectName` comparison is canonical, so lookup works either way; only literal assertions and human readers care.)

- **D-09:** ORCH-03's evidence is **Docker-free whole-string cases added to `MBeanNameUnitTest`**, landed in a **second commit** after D-04's mechanical carrier swap. Two reviewable diffs in one file: "the contract moved home" then "the contract grew topic coverage". ROADMAP criterion 3 gates on `testUnit`, so a Docker-free pin is the only evidence that gate can see. A live assertion is not added — D-12's full-suite run already exercises registration through `ClickHouseSinkTaskTest`'s existing topic-MBean case.

### Orchestration, Utils & Processing (ORCH-01, ORCH-04, FREE-01)

- **D-10:** ORCH-01 is discharged by **verification, not code**. `ProxySinkTask.java` and `Processing.java` are byte-identical to `v1.6.0`, `DeliveryStrategy` / `ChunkFlusher` / `DirectDeliveryStrategy` / `AtLeastOnceBufferStrategy` / `ExactlyOnceBufferStrategy` are new upstream files with no fork delta, and a tree-wide grep for `new Processing(` finds only 5-arg call sites (1 in `src/main`, 15 in `ProcessingTest`). The plan should assert this by grep at execution time rather than assume it from this document — D-03's constructor change is the only edit either file receives.

- **D-11:** ORCH-04 is discharged by **reading both files and writing down what was read**. `Processing.java` needs no edit. `Utils.java`'s entire 29-line delta against upstream is a Spotless import reorder — confirm and state that, so the next sync's reviewer does not re-investigate. `UtilsTest.java` and `ProcessingTest.java` are already on upstream's versions; confirm by diff and flip both Section 1 rows, plus the two Section 2 rows for the production files. **FREE-01 is already satisfied in the merged tree** — `Utils.isServerError()` is `t instanceof ClickHouseException || t instanceof ServerException` and `handleException` reads the code off whichever matched — so the fork's "error classification is dead code on client V2" follow-up is closed in `MERGE-CHECKLIST.md § Recorded Facts` citing upstream issue **#818**, alongside D-07's fact. No new test.

### Acceptance Gate

- **D-12:** The phase closes on **`testUnit` green *plus* a full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` run**, with a re-baselined pass condition rather than "zero failures":
  1. Phase 8's own two failures (`MBeanRegistrationLiveTest` JMX-04 ×2, currently NPE because `connector=` does not exist) are **gone**.
  2. **Zero new failures** relative to the 7-failure baseline recorded at the end of Phase 7.
  3. Every remaining failure is **named with its owning phase** in the phase SUMMARY, so Phase 9 inherits a list rather than a surprise.

  This is Phase 7's lesson applied: its gap-closure run was the first execution of anything since the 581-commit merge and found `targetTableFilter` **entirely broken** — appended after `group by`, throwing `ILLEGAL_TYPE_OF_ARGUMENT` on every non-empty filter — behind a green `testUnit`. A Docker-free gate structurally cannot see that class of defect.

- **D-13 (Claude's discretion — user said "реши сам"):** Of the 5 carry-in failures that are not Phase 8's:
  - **The 4 UTF-8 mojibake failures are fixed here.** Three unguarded `new InputStreamReader(stream)` calls in `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (≈`:140` `getAllRowsAsJson`, ≈`:158` `getAllRowsAsJsonCloud`, ≈`:318`) decode UTF-8 bytes under the test JVM's deliberate `file.encoding=windows-1252`. Fully diagnosed, three lines, and it clears `JsonDynamicRoundTripTest` TYPE-07 ×2 plus `SmtSinkLandingTest` HDRS-09/CONV-08. **Rationale:** D-12 just made a full live suite the gate, and a gate running against four known reds is a gate that can hide a fifth. This is Phase 10's HARN-04 territory, so **annotate** the relevant checklist row with the early landing and its commit — do not flip it.
  - **`TableTest.extractNullables()` (expected 3, got 2) is triaged, not fixed.** Investigate far enough to name a cause and an owning phase, and write that down. `Table.java` is byte-identical to upstream and was out of Phase 7's scope, so this is either a stale fork expectation or a genuine upstream behaviour change — the second is unscoped work that deserves its own decision.
  - A planner may override either half with a stated reason; both are reversible.

### Commit Shape

- **D-14:** One fork behaviour per commit, per Phase 7's D-19 — each an isolated, reviewable diff against `v1.6.0`, which is exactly the record Phase 11's SUITE-03 must produce. The natural sequence: JMX re-plumb (`SinkTaskStatistics` + `ProxySinkTask` + `ClickHouseSinkTask` + the 4 call sites + the `build.gradle.kts` exclude removal + D-04's mechanical test swap) → D-09's topic-name test cases → D-13's UTF-8 fixture fix → the checklist flips and Recorded Facts. Not one commit per file, not one per plan.

- **D-15:** Run `./gradlew spotlessApply` before each commit per `AGENTS.md`, but **only over the files the commit touches** — a tree-wide `spotlessApply` was already declined in Phase 7 (plan 07-01) because an unrelated 52-file reformat has been pending since the merge and would destroy the single-behaviour diff property D-14 exists to preserve. Verify touched files pass `spotlessCheck` individually instead.

### Claude's Discretion

The user did not constrain the following; these are the calls made, all reversible and open to a planner overriding them with a reason:

- **Carry-in split for D-13** (above) — explicitly delegated by the user.
- **Wave shape.** The JMX re-plumb is one connected edit across four production/test files with a single call site each, so it does not decompose into parallel work. D-09's topic cases depend on D-08's composition existing; D-13's fixture fix is independent of everything and could run in parallel if the planner wants a second wave. D-12's full-suite run is necessarily last.
- **`MBeanServerUtils` is not touched.** Its `registerMBean` still rethrows `InstanceAlreadyExistsException` as a `RuntimeException` and still swallows `MalformedObjectNameException` with a WARN. Both behaviours are load-bearing for existing tests (the leak sweep, and Phase 4's D-05 reasoning about hostile names) and neither is in this phase's requirements.
- **No new metric is added.** `AGENTS.md` asks that relevant metrics land in `SinkTaskStatistics` / `TopicStatistics`; this phase changes only how those MBeans are *named*.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 8: Orchestration & JMX Re-plumbing" — the four success criteria this phase is judged against, including the explicit statement that "prefer upstream" does not apply to JMX naming
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — scratch-branch rule, the no-`.planning`-churn-during-conflict-resolution commit cadence, the mandatory `env -u TESTCONTAINERS_HOST_OVERRIDE` Gradle invocation, and `testUnit` as the cheap gate
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — the 14 decisions plans must not re-open; in particular "Conflict policy" (and its JMX carve-out) and "Kafka Connect API 4.1.1"
- `.planning/REQUIREMENTS.md` § "Orchestration & JMX (ORCH)" — ORCH-01..04, this phase's four scoped requirements
- `.planning/REQUIREMENTS.md` § "Free Wins — Verify and Record (FREE)" — FREE-01's exact wording: *confirm and record, never fix*
- `.planning/REQUIREMENTS.md` § "Out of Scope" and § "Future Requirements" — what must not be attempted
- `.planning/MERGE-CHECKLIST.md` § "Section 1 — Conflicted" — the 6 rows owned by Phase 8 (`ClickHouseSinkTask.java`, `ProxySinkTask.java`, `Processing.java`, `ProcessingTest.java`, `UtilsTest.java`, `RangeContainerTest.java`) and the Phase-10 rows Phase 8 annotates but does not flip (D-02, D-13)
- `.planning/MERGE-CHECKLIST.md` § "Section 2" — the `Utils.java` and `Processing.java` clean-merge rows that ORCH-04 makes a hard read requirement
- `.planning/MERGE-CHECKLIST.md` § "Recorded Facts" — item 4 forwards the exact `Version.ARTIFACT_VERSION` string to this phase; D-07 and D-11 add their facts here

### Prior phase context
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § "Commit Shape" (D-19) — the one-behaviour-per-commit rule D-14 inherits
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § "The Compile Blocker" (D-02/D-03) — the annotate-don't-flip bookkeeping pattern D-02 and D-13 reuse for cross-phase landings
- `.planning/phases/06-merge-groundwork-build-migration/06-CONTEXT.md` § "Merge Commit Shape" — the byte-identity invariant that makes every Phase 8 re-application a reviewable diff against upstream

### Re-expression sources
- `.planning/research/ARCHITECTURE.md` — per-collision-file resolution shape for `ProxySinkTask` / `ClickHouseSinkTask` / `Utils`
- `.planning/research/FEATURES.md` — the JMX feature's disposition
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase
- `.planning/research/SUMMARY.md` § "Must-not-silently-lose list" — JMX `connector=<name>` is one of the five behaviours that vanish with no conflict marker and no compile error; its canary is `MBeanRegistrationLiveTest`

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard and colima profile `vz`; the `[Phase 07 → 8]` row (JMX-04 NPE, this phase's canary firing as designed); the `[Phase 07 → 8/10]` row (the two `build.gradle.kts` excludes, one per phase to remove); the `[Phase 07 → 10]` row (the UTF-8 fixture regression D-13 takes on); the `[Phase 07 → untriaged]` row (`TableTest.extractNullables`); the GSD worktree-isolation quirk requiring `--force-isolation none` immediately before each dispatch
- `.planning/STATE.md` § "Accumulated Context → Decisions", last entry — Phase 7's gap-closure record, the evidence behind D-12
- `.planning/config.json` — `build_command` / `test_command` carry the `env -u` prefix; `granularity: coarse`, `mode: yolo`, `test_gate_timeout: 2400`
- `AGENTS.md` § "Coding Conventions", § "Correctness & Safety First", § "Observability & Diagnostics", § "Security" — Google Java Style via Spotless, JMX metrics belong in `SinkTaskStatistics` / `TopicStatistics`, no secrets or record data logged in plaintext
- `.planning/codebase/CONVENTIONS.md` — SLF4J level conventions

### Files this phase touches directly
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` — `connectorName` field + ctor arg (D-01), static `buildMBeanName` (D-01/D-06), instance `getMBeanName()` / `getTopicMBeanName(topic)` composed per D-08, `registerMBean` / `unregisterMBean` / `createTopicStatistics` updated to the instance methods
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:43,52` — 3-arg constructor (D-03) and `new SinkTaskStatistics(connectorName, id)`
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:47` — `props.get("name")` restored and passed through (D-03)
- `build.gradle.kts:448` — delete `exclude("**/MBeanNameUnitTest.java")`; leave `:450` alone (D-05)
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` — mechanical carrier swap (D-04), then topic cases in a second commit (D-09)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java:313,334,392` and `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java:75` — pass `null` explicitly (D-02)
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` ≈`:140`, ≈`:158`, ≈`:318` — `StandardCharsets.UTF_8` on three `InputStreamReader`s (D-13)
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java`, `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` — **read only** (D-11), no edit expected
- `.planning/MERGE-CHECKLIST.md` — row flips and the D-07 / D-11 Recorded Facts

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **`MBeanNameUnitTest` survived the merge byte-for-byte** and is still on disk — it is excluded from compilation, not deleted. Its six assertions already encode the full fork contract (key order `type,connector,name,version`, `connector=null` for absent, `connector=` for empty, `version=unknown` for null, every colon → underscore). D-04 is a find-and-replace, not a rewrite.
- **`MBeanRegistrationLiveTest` needs no adaptation** beyond the fix that makes it pass. It filters by `ObjectName.getKeyProperty("connector")` over the pattern `com.clickhouse:type=ClickHouseKafkaConnector,*`, so it works the moment the segment exists, and it deliberately never asserts an absolute `SinkTask<N>` (the `NEXT_ID` counter does not reset inside a JVM fork).
- **`SinkTaskStatistics.unregisterMBean()` already walks a `topicMBeans` deque** and unregisters each entry, so the per-topic rename needs no cleanup work — the names it stores are whatever `createTopicStatistics` produced.
- **`Version.ARTIFACT_VERSION` is already imported and used** by `SinkTaskStatistics` (`:163`, `:167`) and `ClickHouseHelperClient:105`. D-06 changes nothing about where the value comes from.

### Established Patterns

- **`ObjectName` equality is canonical, not textual.** Key order in the rendered string affects literal assertions and human readability only — `new ObjectName(...)`, `isRegistered`, `getMBeanInfo` and `queryNames` are all order-insensitive. This is why D-08 can choose the order on readability grounds without a lookup risk.
- **Upstream's tests reach MBeans exclusively through the `SinkTaskStatistics` statics**, never by hand-writing an ObjectName string. That is what makes D-02 a four-line change instead of a search for string literals.
- **Phase 7's anti-drift precedent:** both RowBinary gates were made to share one message builder so their wording could not diverge. D-08 applies the same reasoning to the two MBean names.
- **`registerMBean` rethrows `InstanceAlreadyExistsException` as a `RuntimeException`** but swallows `MalformedObjectNameException` with a WARN — so a connector whose name contains a comma, `=` or a quote runs perfectly while silently publishing no JMX metrics. Unchanged from Phase 4's D-05 analysis; see Deferred.

### Integration Points

- **`ClickHouseSinkTask.start()` → `new ProxySinkTask(cfg, errorReporter, props.get("name"))` → `new SinkTaskStatistics(connectorName, id)` → `statistics.registerMBean()`** is the whole chain. It has exactly one link at each level, and breaking any of them silently publishes MBeans without the `connector=` segment — no compile error, no test failure outside the JMX tests. This is one of the five "vanish with no conflict marker" behaviours.
- **`ProxySinkTask` constructs both `ClickHouseWriter` and (indirectly) the helper client**, which is why ROADMAP made Phase 8 depend on Phase 7. That dependency is now satisfied — `compileJava` and `testUnit` were both green at the end of Phase 7.
- **⚠ Counting hazard for the planner:** once topic MBeans carry `connector=`, `MBeanRegistrationLiveTest.registeredFor()` — which filters the entire `type=ClickHouseKafkaConnector,*` space by the `connector` key — will return task **and** topic MBeans for any task that has called `put()`. Its current JMX-04 cases never do, so `newlyAdded()`'s "expected exactly one newly registered MBean" assertion stays correct, but that is now load-bearing on a property nobody wrote down. Any new live JMX case that inserts records must filter on the `topic` key or count differently.
- **`ClickHouseBase`'s `@AfterEach` MBean leak sweep is not yet restored** (Phase 10 / HARN-02), and HARN-02 explicitly requires extending it to cover the per-topic MBeans this phase renames. Phase 8 changes the names that sweep will have to match — worth a note in the SUMMARY so Phase 10 does not write the sweep against upstream's format.

</code_context>

<specifics>
## Specific Ideas

- **The organising fact of this phase is that almost nothing needs writing.** Four of the five files involved are byte-identical to upstream `v1.6.0`, `Utils.java`'s only delta is an import reorder, and ORCH-01's "no stale 4-arg `Processing` call site" is already true across the whole tree. The phase's real content is one connected JMX edit plus a disciplined round of read-verify-record. Planners should resist inventing work for ORCH-01 and ORCH-04 — the requirement is evidence, and grep is the evidence.
- **The gate was deliberately raised above what ROADMAP criterion 3 asks for.** Criterion 3 says `testUnit`; D-12 adds a full live run because Phase 7's gap-closure proved a Docker-free gate can be green over a behaviour that is entirely broken. The pass condition is a *re-baselined* one, not "zero failures" — a phase that cannot close until other phases' work is done is a phase that will be closed dishonestly.
- **JMX-03's branches are now vestigial in production, and that is accepted, not overlooked.** `Version.ARTIFACT_VERSION` can never be null and never contains a colon. The branches stay because ROADMAP criterion 1 requires the sanitization retained and because `buildMBeanName` is a general formatter — but nobody should later "discover" this as dead code and delete it. It is recorded here so the next reviewer does not re-litigate it.
- **The version string changing from `0.9.6` to `v1.6.0_gg1` is the one user-visible consequence of this phase**, and it moves again on every `VERSION` bump. That is why D-07 puts it in Recorded Facts rather than leaving it to be found by a blank Grafana panel.

</specifics>

<deferred>
## Deferred Ideas

- **ObjectName-hostile connector names.** A connector whose `name` contains a comma, an `=` or a quote produces a `MalformedObjectNameException` that `MBeanServerUtils.registerMBean` swallows with a WARN — so the connector runs perfectly and publishes no JMX metrics at all. Deliberately uncovered since Phase 4's D-05: quoting the name would change ObjectNames operators already scrape, so it needs its own decision. Still true, still deferred.
- **A standing test that the production MBean name uses `ARTIFACT_VERSION`**, so a future sync silently reverting to the client-jar read fails a test rather than a dashboard. Considered under D-07 and rejected — it couples a test to a build-generated constant, the same weakness D-04 rejects. Worth revisiting if the version source ever changes again.
- **`TableTest.extractNullables()`** — triaged by D-13 but not fixed. Whichever phase owns `Table`/mapping coverage inherits it with a named cause.
- **Extending `ClickHouseBase`'s `@AfterEach` MBean leak sweep to the per-topic MBeans** — Phase 10 / HARN-02, not Phase 8. Phase 8 only changes the names the sweep will need to match.
- **`ClickHouseSinkTask.version()` returns a hard-coded `"0.0.1"`** (upstream's value, unrelated to `Version.ARTIFACT_VERSION` and to the MBean `version=` segment). Noticed while reading the file; out of this phase's requirements and untouched.
- **Fixing CR-01** (`.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error as "no match", so Invariant 2 can report clean on a broken pathspec). Carried from Phase 6's security audit; **must be fixed before Phase 11**, which gates publication on that script running unattended. Not Phase 8 work.

### Reviewed Todos (not folded)

`gsd_run query todo.match-phase 8` returned zero matches — no pending todos intersect this phase.

</deferred>

---

*Phase: 8-Orchestration & JMX Re-plumbing*
*Context gathered: 2026-09-19*
