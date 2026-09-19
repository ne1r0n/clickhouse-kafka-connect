# Phase 9: Metadata-free JSON Path — Keep or Drop - Context

**Gathered:** 2026-09-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Settle JSONP-01's keep-or-drop call for the fork's metadata-free `insertFormat=json` bypass **on evidence from the merged code**, then make `src/main` and the tests say the same thing, and give `jsonInsertColumns` a working consumer again.

**The call is made: KEEP.** See D-01. It was made in this discussion, on the measured state below plus operator facts the repository cannot supply, and *before* any dependent implementation work — which is what ROADMAP criterion 1 requires.

Delivers: the bypass re-applied as minimal early-return guards at four sites in `ClickHouseWriter`; `jsonInsertColumns` re-attached via the surviving client-`0.9.6` columns overload; four `@Disabled` test classes (17 methods) re-enabled unchanged; two new Docker-free regression pins; JSONP-04 discharged by reading the resolved diff; the `TableTest.extractNullables()` carry-in adopted so the gate is a clean binary.

Gate: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` green **plus** a full `./gradlew test` run — see D-12.

Out of this phase: R-01's fail-fast fix (Future Requirement — the loss stays pinned under KEEP per JSONP-02); the harness contract — `@Tag`, MBean leak sweep, shared-container strategy, Testcontainers `2.0.5`, `ClickHouseTestHelpers` canonicalisation, the `build.gradle.kts` `ClickHouseTestHarnessTest` exclude (Phase 10); publication, CI and the SUITE-03 record (Phase 11); CR-01's fail-open invariant script (before Phase 11, no owner yet).

### Measured starting state

Re-derived at discussion time against the merged tree — **re-confirm by command before planning, do not re-derive by reading this table.**

| Fact | Evidence |
|---|---|
| The bypass is **entirely absent** from merged `src/main` | `git diff v1.6.0 -- ClickHouseWriter.java` = +80/−8, all of it Phase 7's work. No `shouldSkipTableMetadata`, no synthetic `Table` in `getTable`, no `start()`/`updateMapping()` short-circuit, no `cleanupExtraFields` bypass |
| `shouldSkipTableMetadata()` exists nowhere in `src/` | grep: only the `ClickHouseSinkConfig` field and the `@Disabled` tests reference the concept |
| `jsonInsertFormatExplicit` and `jsonInsertColumns` have **zero production consumers** | `ClickHouseSinkConfig.java:138,278` and `:44,137,279-290`; the only readers are test classes. Phase 7 D-13's deliberate one-phase gap |
| The old `jsonInsertColumns` call site is gone | `ClickHouseWriter.java:1379` is now `client.insert(table.getName(), dataWriter, JSONEachRow, insertSettings)` |
| **But the columns overload survives** in client `0.9.6` | `javap` on `client-v2-0.9.6.jar`: `insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)`. JSONP-03's stated rationale is wrong — see D-07 |
| R-01's mechanism is the **synthetic column-less `Table`**, not `insertFormat` | `doInsertBatch:222` never reads `insertFormat`; `SCHEMA` → RowBinary unless `bypassRowBinary`. With no bypass, `getTable` returns a described table and the loss cannot occur. Re-applying the bypass re-introduces it |
| Upstream gained three partial substitutes | `suppressTableExistenceException`, `bypassFieldCleanup`, `bypassSchemaValidation`, the `enableDbTopicSplit` empty-mapping branch at `:126-135`, and a per-table `describeTable` fallback at `:1596-1601`. **None covers a missing `DESCRIBE` grant** |
| Upstream's `Table` is immutable, one 5-arg constructor | `Table.java:48` — `Table(String, String, boolean, List<Column>, int)`. The fork's `new Table(database, tableName)` is gone |
| 17 test methods parked | `ExplicitJsonInsertPathLiveTest` (6, incl. R-01's pin), `JsonInsertColumnsLiveTest` (5), `MetadataFreeJsonInsertUnitTest` (4), `ClickHouseSinkTaskJsonInsertFormatTest` (2), all `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")`. `JsonInsertColumnsUnitTest` (6, COLS-01 parsing) is **not** disabled and green |

### Operator facts established in this discussion

These are the inputs the repository cannot supply. They are the load-bearing evidence for D-01 and belong in `MERGE-CHECKLIST.md § Recorded Facts` as `operator-confirmed`, alongside SYNC-04/SYNC-05's.

1. **At least one live connector sets `insertFormat=json`.** The bypass has a production consumer; "drop" is not operationally free.
2. **Those pipelines produce schemaless JSON (`Map`) values** — `SCHEMA_LESS` records. R-01's loss needs a `Struct` record, so **no live pipeline is currently losing data to R-01.**
3. **The strictest constraint set applies**: tables may not exist at `start()`, the connector user may not be able to `DESCRIBE`/`SHOW` them, and `DESCRIBE` traffic is unwanted.

</domain>

<decisions>
## Implementation Decisions

### The Keep-or-Drop Call (JSONP-01)

- **D-01: KEEP the metadata-free `insertFormat=json` bypass.** Operator fact 3 forces it: of the three things the bypass buys, the missing `DESCRIBE`/`SHOW` grant is the one **no combination of upstream configuration reaches**. `suppressTableExistenceException` suppresses the *exception* but `getTable` still calls `describeTable` and returns `null`; `enableDbTopicSplit` lets `start()` survive an empty mapping but still runs `updateMapping`; `bypassFieldCleanup` covers only the third effect. Only skipping metadata acquisition entirely works.
  - This **overrides Phase 7 D-10's inherited default of "dropped"**, which was explicitly labelled reversible and Phase 9's to decide.
  - **Known cost, accepted:** KEEP re-introduces R-01's silent data loss on the `Struct` route. JSONP-02 requires the loss stay pinned rather than fixed; operator fact 2 says no live pipeline takes that route today. See Deferred.
  - This is a **deliberate departure from the milestone's "prefer upstream" conflict policy**, on the same footing as the JMX carve-out: upstream's implementation cannot pass the fork's tests here. Record it as such in `MERGE-CHECKLIST.md`, because SUITE-03 must show the fork delta and explain each surviving piece.
  - **Reversibility:** costly — undoing it later means a production migration for the affected connectors (grants, or table pre-creation), not just a code revert.

- **D-02: The decision is recorded here, not re-litigated at execution.** ROADMAP criterion 1 asks for a recorded keep-or-drop with rationale, made at a blocking-human checkpoint on evidence, **before** the dependent work. This discussion was that checkpoint: the evidence table above is from the merged code, the three operator facts came from the human, and no implementation exists yet. A planner may still place a cheap `checkpoint:decision` before the first code task — but it re-confirms D-01, it does not re-open it.

- **D-03: Re-apply as minimal early-return guards, not restored method bodies.** A guard at the top of each of the four sites; **every upstream line below stays untouched**, including the `describeTable` fallback (`:1596-1601`), `updateMapping`'s `compareAndSet` (`:142`), and the `enableDbTopicSplit` empty-mapping branch (`:126-135`). The sites, at current line numbers:

  | Site | Current line | Guard behaviour |
  |---|---|---|
  | `start(ClickHouseSinkConfig)` | `:78` | after the ping and version check, before `updateMapping`: log at INFO and `return true` |
  | `updateMapping(String)` | `:142` | return early. **This also kills the background DESCRIBE traffic** — `TableMappingRefresher` calls this method, so `startBackgroundTableSync` (`:1623`) needs no separate guard |
  | `getTable(String, String)` | `:1592` | return the synthetic table resolved from `csc.getTopicToTableMap().getOrDefault(topic, topic)` |
  | `cleanupExtraFields(Map, Table)` | `:1387` | `return m` untouched, OR'd with the existing `csc.isBypassFieldCleanup()` check |

  Rejected: restoring the pre-merge method bodies wholesale — it discards three upstream improvements and grows the fork delta on a file the next sync must re-merge.
  - **Reversibility:** reversible — four guards, one predicate.

- **D-04: The synthetic `Table` is `new Table(database, tableName, false, List.of(), 0)`.** This reproduces the pre-merge 2-arg constructor's semantics exactly (empty column lists, `numColumns` 0), expressed against upstream's immutable 5-arg shape. `hasDefaults=false` matters: `doInsertBatch:222` passes `table.hasDefaults()` into `doInsertRawBinary`. The empty `rootColumnsList` **is** R-01's mechanism — it is reproduced knowingly, not overlooked.
  - This is the whole of JSONP-04's "no fork-only JSON-path code left standing against a `Table` shape upstream no longer produces": the one place the fork built a `Table` by hand is here, and it must use the new constructor.

- **D-05 (Claude's discretion):** The guard predicate is a single private `shouldSkipTableMetadata()` returning `csc != null && csc.isJsonInsertFormatExplicit()`, matching the pre-merge shape and `AGENTS.md`'s DRY rule, rather than four inlined `csc.isJsonInsertFormatExplicit()` calls — a gate spread across four call sites can drift. The user chose "minimal early-returns" as the *diff shape*; whether the condition is named or inlined was not constrained. A planner may inline it with a stated reason.

### `jsonInsertColumns` (JSONP-03)

- **D-06: Keep the column list server-side.** In `doInsertJsonV2`, call `client.insert(table.getName(), columns, dataWriter, JSONEachRow, insertSettings)` — the overload `javap` confirms exists in `client-v2-0.9.6.jar`. Rejected: a client-side field filter inside `writeRecordsJson`, because (a) it silently swallows COLS-05's contract — a name that is not a table column produces a non-retriable throw today and would produce nothing at all under a filter — and (b) under D-01's bypass `cleanupExtraFields` returns the payload untouched, so a filter folded in there would be a no-op in exactly the mode `jsonInsertColumns` is most used with.
  - **Reversibility:** reversible — one conditional call site.

- **D-07: JSONP-03's rationale is corrected, not its goal.** The requirement reads *"the `client.insert(table, columns, data, …)` overload it attached to no longer exists at that call site"* — true of the call site, false of the API. D-06 therefore deviates from the requirement's prescribed *means* while satisfying its end ("`jsonInsertColumns` works again … with `COLS-01..06` green"). Append a dated correction note to `REQUIREMENTS.md` § JSONP-03 citing the `javap` output; **do not rewrite the requirement text** — same annotate-don't-flip bookkeeping Phase 7 D-03/D-07 and Phase 8 D-02 used.

- **D-08 (planner constraint): branch on non-empty unless COLS-02 re-proves otherwise.** Pre-merge passed the list *unconditionally* (`ClickHouseWriter.java:1031-1032` at `pre-v1.6.0-sync`) and COLS-02 proved an empty list still means "no column clause" — but that was the `InputStream` overload. A mis-rendered `INSERT INTO t ()` would break **every default-config JSON insert in production**, so call the 4-arg overload when the list is empty and the 5-arg one when it is not, unless a live COLS-02 run proves unconditional is safe on the `DataStreamWriter` overload.

- **D-09 (Claude's discretion): `doInsertJsonV1` stays untouched.** The pre-merge fork never wired `jsonInsertColumns` into the V1 path, and the milestone tests client V2 only (`REQUIREMENTS.md` § Locked decisions, § Out of Scope).

### Test Disposition (JSONP-02)

- **D-10: Re-enable all four classes unchanged — delete the `@Disabled` line and nothing else.** Phase 7 D-11 parked them precisely so this branch would be a four-line diff, and that diff is itself the proof that the contract did not move. Phase 7's compile fixes (the reflective `setSinkConfig` accessor, 5-arg `Table` construction, the local `createTable` helpers) **stay** — they are required by upstream's immutable `Table` regardless of D-01.
  - R-01's `PIN - NOT AN ENDORSEMENT` Javadoc stays **byte-identical**. The user declined refreshing it; operator fact 2 is recorded here and in Deferred instead.
  - Rejected: re-enabling one class per commit. D-13's sequencing already lands the behaviour before the un-disabling.

- **D-11: Add two Docker-free regression pins to `MetadataFreeJsonInsertUnitTest`** — `getTable()` returns a synthetic `Table` resolved from `topic2TableMap` without touching the helper client, and `updateMapping()` short-circuits. Both are reachable through the reflective `csc` accessor that class already carries. `start()` stays live-only: it pings a server before reaching its guard.
  - **Why:** the bypass is exactly the class of fork behaviour that *"vanishes with no conflict marker and no compile error"* — it just did, in this merge, and the existing Docker-free coverage (JSON-01's letter-case matrix, JSON-03's `cleanupExtraFields`) would not have caught it. This makes the bypass a **sixth** entry in `STATE.md`'s must-not-silently-lose list, with a `testUnit`-visible canary rather than a Docker-only one.

### Acceptance Gate (JSONP-04)

- **D-12: `testUnit` green PLUS a full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test`, with a clean zero-failure pass condition.** This raises the bar above ROADMAP criterion 3, which asks only for the Docker-free `JsonInsertColumnsUnitTest` here and defers `COLS-01..06`'s live half to Phase 11. The reason it is raised: this phase re-enables **13 live test methods that have not executed once since the 581-commit merge**, and Phase 7's gap-closure proved a green `testUnit` can sit on top of a behaviour that is entirely broken (`targetTableFilter` appended after `group by`). Same reasoning as Phase 8's D-12.
  - Pass condition: all 13 newly-enabled live methods green, **zero failures total** (not re-baselined — see D-14), every skip explained.

- **D-13 (Claude's discretion): commit sequence, per Phase 8's D-14 one-behaviour-per-commit rule.** (1) the four bypass guards + the synthetic `Table`; (2) `jsonInsertColumns`' server-side re-attachment; (3) the four `@Disabled` deletions; (4) D-11's two Docker-free pins; (5) the `TableTest` assertion fix; (6) checklist flips and Recorded Facts. Not one commit per file, not one per plan. Run `./gradlew spotlessApply` over **only the files each commit touches** (Phase 8 D-15 — a tree-wide reformat has been pending since the merge and would destroy the single-behaviour diff property).

- **D-14: Adopt the `TableTest.extractNullables()` carry-in and fix it here.** `TableTest.java:51` asserts `getAllColumnsList().size() == 3` for a table with `off16 Int16` + `date_number Nullable(Date)`; the fork's `shouldSkipDescribeRow` drops the structural `date_number.null` subcolumn, so 2 is correct. Change `3` → `2` with a comment citing `DESC-01`/`HLP-02`. Phase 8 named the cause; `MERGE-CHECKLIST.md` Recorded Fact 13 says it is unowned and must resolve before Phase 11.
  - **Why here:** it turns D-12's gate from a re-baselined "one known red, zero new" into a clean binary. A re-baselined gate is exactly what lets a second failure hide — the risk Phase 8's D-12 named and had to live with.
  - This is a **test-only** change outside `JSONP-01..04`, taken at the user's explicit decision per `STATE.md`'s "a change reaching outside a phase's declared scope stops for a user decision".
  - Flip Recorded Fact 13's disposition from "owning phase: none" to resolved-in-Phase-9, with the commit SHA.

### Claude's Discretion

The user did not constrain the following. All reversible; a planner may override with a stated reason.

- **D-05** (named predicate vs. inlined condition), **D-09** (V1 untouched), **D-13** (commit sequence).
- **Wave shape.** The four guards are one connected edit with one call site each and do not decompose. `jsonInsertColumns` (D-06) touches only `doInsertJsonV2` and is independent of the guards — it could run in a parallel wave. The `@Disabled` deletions depend on both. D-12's live run is necessarily last.
- **JSONP-04's evidence act.** Discharged by reading `git diff v1.6.0 -- src/main/java/.../ClickHouseWriter.java` after the re-application and stating in the SUMMARY that (a) the only hand-built `Table` uses the 5-arg constructor, (b) no JSON-path code assumes the old mutable shape, (c) the guards are the only fork additions on that path. A grep is the evidence; a green test is not sufficient, per the requirement's own wording.
- **No new metric.** `AGENTS.md` asks that metrics land in `SinkTaskStatistics`/`TopicStatistics`; this phase adds no insert path, only re-routes an existing one.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 9: Metadata-free JSON Path — Keep or Drop" — the four success criteria this phase is judged against; note criterion 3's deferral of `COLS-01..06`'s live half to Phase 11, which D-12 deliberately overrides
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — scratch-branch rule, the no-`.planning`-churn-during-conflict-resolution cadence, the mandatory `env -u TESTCONTAINERS_HOST_OVERRIDE` prefix, `testUnit` as the cheap gate
- `.planning/REQUIREMENTS.md` § "Metadata-free JSON Path (JSONP)" — JSONP-01..04, this phase's four requirements. **JSONP-03's rationale is factually wrong — see D-07 before acting on it**
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — in particular "Conflict policy" (D-01 is a second carve-out from it, after JMX) and "R-01 — decided at a blocking-human checkpoint inside the JSON-path phase, on evidence"
- `.planning/REQUIREMENTS.md` § "Future Requirements" — R-01 fail-fast and P4-WR-01 are both listed here; neither is this phase's work
- `.planning/REQUIREMENTS.md` § "Out of Scope" — client-V1 and the beyond-`latest`×V2 matrix
- `.planning/MERGE-CHECKLIST.md` § "Recorded Facts" — item 13 is the `TableTest.extractNullables()` triage D-14 resolves; the line below the table states no keep-or-drop disposition is recorded anywhere, which D-01 now supplies
- `.planning/PROJECT.md` § Active follow-ups — R-01's full write-up and the Phase 4 decision trail behind "pin, don't fix"

### Prior phase context
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-10 — the inherited "dropped" default D-01 overrides, and its explicit reversibility note
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-11 — why the four classes were parked rather than deleted; D-10 here is the payoff
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-12/D-13 — the evidence brief for this checkpoint, and the deliberate one-phase consumer gap for `jsonInsertColumns`/`jsonInsertFormatExplicit`
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-19 — one-behaviour-per-commit, inherited via Phase 8 D-14
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-12 — the re-baselined full-suite gate precedent and the Phase 7 gap-closure lesson behind it
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-13 — the annotate-don't-flip pattern for cross-phase landings, reused by D-14
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-15 — scoped `spotlessApply` only

### Re-expression sources
- `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — the fork's pre-merge bypass: `shouldSkipTableMetadata()` at `:1268`, the `start()` guard at `:137`, the `updateMapping()` guard at `:156`, the synthetic `getTable` at `:1241-1245`, the `cleanupExtraFields` guard at `:1039`, and `jsonInsertColumns`' old call site at `:1031-1032`
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase
- `.planning/research/SUMMARY.md` § "Must-not-silently-lose list" — D-11 adds the bypass as a sixth entry with a `testUnit`-visible canary

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard and colima profile `vz`; the `[Prod]` R-01 row this phase resolves the disposition of; the `[Phase 10]` row for the remaining `build.gradle.kts` exclude; the `[Phase 06]` CR-01 row; the GSD worktree-isolation quirk needing `--force-isolation none` immediately before each dispatch
- `.planning/config.json` — `build_command`/`test_command` carry the `env -u` prefix; `granularity: coarse`, `mode: yolo`, `test_gate_timeout: 2400`
- `AGENTS.md` § "Correctness & Safety First", § "Coding Conventions", § "Testing Discipline", § "Security" — Google Java Style via Spotless, DRY, no record data logged in plaintext, only-add-never-weaken tests
- `.planning/codebase/CONVENTIONS.md` — SLF4J level conventions (the `start()`/`updateMapping()` guards log at INFO/DEBUG respectively, matching pre-merge)

### Files this phase touches directly
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — guards at `:78` (`start`), `:142` (`updateMapping`), `:1592` (`getTable`), `:1387` (`cleanupExtraFields`); the synthetic `Table` (D-04); `shouldSkipTableMetadata()` (D-05); the `jsonInsertColumns` overload switch at `:1379` (D-06/D-08)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java:36` — delete `@Disabled`; add D-11's two pins
- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java:44` — delete `@Disabled` only
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java:41` — delete `@Disabled` only
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java:31` — delete `@Disabled` only
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java:51` — `3` → `2` with a `DESC-01`/`HLP-02` comment (D-14)
- `.planning/REQUIREMENTS.md` § JSONP-03 — appended correction note (D-07)
- `.planning/MERGE-CHECKLIST.md` — the keep-or-drop disposition, the three operator-confirmed facts, Recorded Fact 13's resolution
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` — **read only**; `jsonInsertColumns`/`jsonInsertFormatExplicit` already parse correctly (Phase 7 D-13), they only needed consumers

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **All four parked test classes survived the merge intact and compile today.** Phase 7 made them build against upstream's immutable `Table` and then `@Disabled` them rather than deleting or weakening anything, so KEEP costs four deleted lines. `MetadataFreeJsonInsertUnitTest` already carries the reflective `setSinkConfig` accessor D-11's new pins need.
- **`JsonInsertColumnsUnitTest` was never disabled and is green** — COLS-01's six parsing cases already pass inside `testUnit`, so D-06 only has to restore the *consumer*, not the contract.
- **The pre-merge bypass is recoverable verbatim** from `pre-v1.6.0-sync`; every site is a handful of lines. D-03 re-expresses rather than restores, but the original is the reference for exact semantics (especially the log levels and the `getOrDefault(topic, topic)` fallback).
- **`csc.isJsonInsertFormatExplicit()` and `csc.getJsonInsertColumns()` already exist and parse correctly** (Phase 7 D-13, `@Getter` on the class at `ClickHouseSinkConfig.java:21`). No config work in this phase.

### Established Patterns

- **`TableMappingRefresher` calls `updateMapping`**, so guarding that one method also stops the background DESCRIBE traffic. `startBackgroundTableSync` (`:1623`) is additionally already gated on `tableRefreshInterval > 0` — no separate guard, and no `stop()` change.
- **`insertFormat` does not route inserts.** `doInsertBatch:222` switches on `SchemaType` and the RowBinary-vs-JSON choice is `bypassRowBinary`. `insertFormat=json` only ever meant "skip metadata" in this fork — which is why R-01 exists and why the name misleads.
- **Annotate-don't-flip** for cross-phase bookkeeping (Phase 7 D-03/D-07, Phase 8 D-02/D-13): when this phase touches a row another phase owns, append a note with the commit SHA rather than flipping its status. D-07 and D-14 both use it.
- **Anti-drift by shared construction** (Phase 7's shared RowBinary message builder, Phase 8's D-08 composed MBean names): D-05's single predicate is the same reasoning applied to a four-site gate.

### Integration Points

- **`ClickHouseWriter.start()` → `updateMapping()` → `getTable()` → `cleanupExtraFields()` is one chain, and every link is on the path of *every* connector**, not just JSON ones. The guards are early-returns keyed on a config flag, so a non-JSON connector's behaviour is unchanged — but a bug in the predicate would be blast-radius-wide. This is why D-12 rejected a targeted live run of only the JSON classes.
- **⚠ `getTable` returns a table whose `getRootColumnsList()` is empty**, and three downstream consumers read it: `validateDataSchema` (loop becomes vacuous), `doInsertRawBinaryV2`'s write loop (emits zero bytes — R-01), and `cleanupExtraFields` (guarded, so never reached under the bypass). A planner adding any new `getRootColumnsList()` consumer must handle the empty case.
- **`ExplicitJsonInsertPathLiveTest` JSON-02 creates its table *after* `sinkTask.start(props)`** — it is the live proof of the `start()` guard specifically, and it cannot pass without D-03's first site. It is also the only live coverage of that site, since D-11's Docker-free pins stop short of `start()`.
- **Phase 10 inherits changed names/behaviour**: these classes now *run*, so Phase 10's MBean leak sweep and shared-container work meets 13 more live tests than Phase 8's baseline did. Worth a SUMMARY note.

</code_context>

<specifics>
## Specific Ideas

- **The organising fact of this phase is that the evidence pointed one way and the operator pointed the other.** Read from the merged code alone, "drop" wins on every axis: R-01 closes for free, the fork delta shrinks, upstream has three partial substitutes, and Phase 7 had already made "dropped" the default. What flips it is a single fact no repository can hold — the connector user may not be able to `DESCRIBE`. That is precisely why JSONP-01 was written as a *blocking-human* checkpoint rather than a research task, and the plan should say so.
- **The `javap` finding is the kind of thing a locked requirement can be wrong about.** JSONP-03 asserted the columns overload was gone; it is gone *from the call site* but present in the API. Following the requirement literally would have silently downgraded COLS-05 from a loud failure to no failure at all. Downstream agents should treat a requirement's stated *rationale* as checkable, while treating its stated *goal* as binding.
- **R-01 is re-introduced with eyes open, and the reason it is survivable is written down.** No live pipeline takes the `Struct` route (operator fact 2). That is a materially different risk picture from Phase 4's, where the fail-fast fix was rated prohibitive because it "would break, loudly, any pipeline running explicit JSON behind a Struct converter" — there is no such pipeline. The fix is still out of scope, but it should be rescheduled on this evidence rather than left at its old cost estimate.
- **D-14 buys a binary gate.** Phase 8 had to close on "one known red, zero new", and its own CONTEXT named the weakness: a phase that cannot close until other phases finish is a phase that closes dishonestly. One test line removes that compromise for Phase 9 and for Phase 11's publication gate.

</specifics>

<deferred>
## Deferred Ideas

- **R-01 fail-fast — reschedule on new evidence.** Still out of scope (`REQUIREMENTS.md` § Future Requirements; JSONP-02 requires the loss stay *pinned* under KEEP). But operator fact 2 says no live pipeline uses the `Struct` route, so the migration story Phase 4 rated prohibitive may now be empty. Worth raising at milestone close with this fact attached. Candidate shapes, none endorsed here: a fail-fast throw in `doInsertBatch` when the table has no columns; or routing `SCHEMA` records to `doInsertJson` under the bypass.
- **P4-WR-01 — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally` task-lifecycle pattern.** D-10 re-enables the class unchanged, so the defect comes back with it (an unexpected `put()` failure leaves an MBean registered until the `@AfterEach` sweep catches it). Future Requirement; Phase 10's HARN-02 sweep restoration mitigates it.
- **CR-01 — `.planning/scripts/verify-merge-invariants.sh:44` fails open** on a `git grep` error. Carried from Phase 6's security audit, still unowned, **must be fixed before Phase 11**, which gates publication on that script running unattended. Not Phase 9 work.
- **Naming `insertFormat` for what it does.** The flag's only effect in this fork is "skip table metadata"; it does not select the insert format (`bypassRowBinary` does). A clearer key with a deprecation alias would prevent the next reader making R-01's mistake — but it is an operator-visible config change and needs its own decision.
- **P3-WR-01** — `shouldSkipDescribeRow` walking more than one level up for deeply nested `JSON`/`Dynamic` children. Adjacent to D-14's root cause, still a Future Requirement.

### Reviewed Todos (not folded)

`gsd_run query todo.match-phase 9` returned zero matches — no pending todos intersect this phase.

</deferred>

---

*Phase: 9-Metadata-free JSON Path — Keep or Drop*
*Context gathered: 2026-09-19*
