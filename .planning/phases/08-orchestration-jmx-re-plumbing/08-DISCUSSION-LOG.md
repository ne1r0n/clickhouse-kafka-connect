# Phase 8: Orchestration & JMX Re-plumbing - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-19
**Phase:** 8-orchestration-jmx-re-plumbing
**Areas discussed:** Name-carrier shape & upstream's 4 call sites, Version segment source, Per-topic MBean & how ORCH-03 is proven, Acceptance gate & the 7 carry-in failures

**Gray areas offered, all four selected.** Prior context loaded: `PROJECT.md`, `REQUIREMENTS.md`, `STATE.md`, `ROADMAP.md`, `MERGE-CHECKLIST.md`, `07-CONTEXT.md`, `06-CONTEXT.md`. Codebase scouted directly (`git diff v1.6.0` per file, call-site greps) rather than via `.planning/codebase/` maps — the phase turns on the *current merged state* of six specific files, which the maps predate.

---

## Name-carrier shape & upstream's 4 call sites

### Q1 — Where should the connector-aware MBean naming live, and what shape should the API take?

| Option | Description | Selected |
|--------|-------------|----------|
| Instance field + static builder | `SinkTaskStatistics` takes `connectorName` in its constructor and owns instance `getMBeanName()`/`getTopicMBeanName(topic)`; a static `buildMBeanName(connectorName, taskId, version)` stays as the pure, testable function `MBeanNameUnitTest` re-targets at | ✓ |
| All-static, connectorName as parameter | Keep upstream's static shape, add `connectorName` as a leading parameter to both statics. Shown to collapse back into a field anyway, since `registerMBean`/`unregisterMBean`/`createTopicStatistics` all need the value | |
| Keep carrier in ProxySinkTask | Re-add `buildMBeanName` to `ProxySinkTask` (the fork's pre-merge home, so the unit test compiles untouched) and pass a finished name string down. Contradicts ROADMAP criterion 1 and splits naming across two classes | |

**User's choice:** Instance field + static builder (recommended option).
**Notes:** Selected with the code preview shown — the constructor signature, the static builder with its `null → unknown` and `: → _` handling, and the `ProxySinkTask` / `ClickHouseSinkTask` call sites.

### Q2 — How should upstream's 4 call sites be handled once the rendered name grows a `connector=` segment?

Grounding presented: `ClickHouseSinkTaskTest.java` (×3, including the topic-name site at `:334`) is a **Phase-10-owned** `survives-as-is` checklist row; `FailureTest.java` (×1) is not in the conflict inventory at all. Both are byte-identical to `v1.6.0` and neither sets a `name` prop, so both register `connector=null`.

| Option | Description | Selected |
|--------|-------------|----------|
| Delete 1-arg, update all 4 explicitly | Drop the connector-blind static; the 4 sites pass `null` explicitly. Costs a cross-phase edit that gets annotated on the checklist, buys a tree with no helper that can render a name no task registered | ✓ |
| Keep 1-arg as a null-connector delegate | Zero edits to either upstream file; correct only by the coincidence that those tests never set a name. Adding `props.put("name", …)` later would fail as a bare `InstanceNotFoundException` | |
| Keep 1-arg, but make it assert its own contract | Same delegate, renamed (e.g. `getUnnamedConnectorMBeanName`) so the `null` is stated rather than implied | |

**User's choice:** Delete 1-arg, update all 4 explicitly (recommended option).

### Q3 — How should `connectorName` travel from `ClickHouseSinkTask.start()` down to `SinkTaskStatistics`?

| Option | Description | Selected |
|--------|-------------|----------|
| Restore the fork's 3-arg ProxySinkTask ctor | `ProxySinkTask(config, errorReporter, connectorName)` — byte-for-byte the pre-merge shape, one call site, three-line diff against `v1.6.0` | ✓ |
| Add connectorName to ClickHouseSinkConfig | Retain `props.get("name")` as a config field. Rejected: grows the fork delta on the worst rebase surface in the tree to hold a non-`ConfigDef` value with one consumer | |
| Post-construction setter on ProxySinkTask | Keeps upstream's 2-arg ctor; introduces a temporal coupling that works only because `registerMBean()` is late | |

**User's choice:** Restore the fork's 3-arg ProxySinkTask ctor (recommended option).

### Q4 — How should `MBeanNameUnitTest` be re-targeted?

| Option | Description | Selected |
|--------|-------------|----------|
| Mechanical carrier swap only | Change the carrier and the Javadoc `@link`; every assertion string including the `0.9.6` literals stays byte-identical, version stays an explicit parameter | ✓ |
| Swap carrier and switch literals to `Version.ARTIFACT_VERSION` | Reads like production, but a test computing its expectation from the source it checks pins less than it appears to | |
| Swap carrier and add per-topic cases | Flagged at ask time as pre-answering the per-topic area | |

**User's choice:** Mechanical carrier swap only (recommended option).
**Notes:** Declining the third option kept the per-topic evidence question genuinely open for the third area, and set up D-09's two-commit split.

---

## Version segment source

### Q1 — Which value should the `version=` segment carry after the sync?

Grounding presented: fork production renders `version=0.9.6` from the ClickHouse *client jar's* manifest; upstream renders `Version.ARTIFACT_VERSION` = `v1.6.0_gg1`, a build-generated compile-time literal that can never be null and contains no colon — which is exactly why JMX-03's two branches exist.

| Option | Description | Selected |
|--------|-------------|----------|
| `Version.ARTIFACT_VERSION` — upstream's source | Semantically right, holds the line byte-identical to upstream, shrinks the fork delta. Costs an operator-visible change and makes JMX-03's branches unreachable from production | ✓ |
| Client jar manifest — the fork's current value | Byte-for-byte what production scrapes today and keeps JMX-03's branches live; re-adds a fork delta on a line upstream owns | |
| `ARTIFACT_VERSION`, and say so to operators | Same as the first, bundled with writing the change up | |

**User's choice:** `Version.ARTIFACT_VERSION` (recommended option), without the bundled write-up — which was then asked separately.

### Q2 — Should Phase 8 write the operator-visible name change down anywhere?

| Option | Description | Selected |
|--------|-------------|----------|
| Named fact in `MERGE-CHECKLIST.md § Recorded Facts` | One row, old string → new string, in the single source Phase 11's SUITE-03 record reads from | ✓ |
| No record — it rides with the release | The JMX name is not a documented contract and already moves on every `VERSION` bump | |
| Record it and pin it with a test | Adds a regression guard, at the cost of coupling a test to the build-generated constant — the weakness rejected in Q4 above | |

**User's choice:** Named fact in Recorded Facts (recommended option).

---

## Per-topic MBean & how ORCH-03 is proven

### Q1 — How should the per-topic MBean name be derived?

Grounding presented: `ObjectName` comparison is canonical, so key order affects literal assertions and human readers only, never lookup. `SinkTaskStatistics.unregisterMBean()` already walks its `topicMBeans` deque, so cleanup needs nothing new.

| Option | Description | Selected |
|--------|-------------|----------|
| Compose from `buildMBeanName` + `",topic="` | One code path, shared literal prefix, key order `type,connector,name,version,topic` falls out rather than being chosen; the two names structurally cannot drift | ✓ |
| Separate `buildTopicMBeanName` static | Symmetric and greppable, but two format strings holding one contract — an edit to one silently leaves the other behind | |
| Append `connector=` to upstream's existing format | Smallest diff vs `v1.6.0`, but the two names disagree on key order and neither reads as a prefix of the other | |

**User's choice:** Compose from `buildMBeanName` + `",topic="` (recommended option), with the side-by-side rendered-name preview shown.
**Notes:** Explicitly framed as the same anti-drift reasoning Phase 7's WRT-01 used to make both RowBinary gates share one message builder.

### Q2 — What evidence carries ORCH-03?

| Option | Description | Selected |
|--------|-------------|----------|
| Docker-free cases in a second commit on `MBeanNameUnitTest` | Carrier swap lands first as its own reviewable diff; topic cases follow in a second commit to the same file. `testUnit` is the gate criterion 3 names, so a Docker-free pin is the only evidence that gate can see | ✓ |
| New Docker-free file for topic naming | Cleaner provenance per requirement, at the cost of a second file testing one static method | |
| Live-only, in `MBeanRegistrationLiveTest` | Strongest evidence (proves registration, not formatting) but invisible to `testUnit`, so criterion 3 would go green without it | |
| Both — Docker-free pin plus a live assertion | Covers the gap Phase 7's gap-closure exposed, at the cost of a second test surface | |

**User's choice:** Docker-free cases in a second commit on `MBeanNameUnitTest` (recommended option).
**Notes:** A live assertion was judged unnecessary because the full-suite gate agreed in the next area already exercises topic-MBean registration through `ClickHouseSinkTaskTest`'s existing case.

---

## Acceptance gate & the 7 carry-in failures

### Q1 — What is Phase 8's acceptance gate?

Grounding presented: Phase 7's gap-closure ran the first live suite since the 581-commit merge, found 12 failures, and discovered `targetTableFilter` was *entirely* broken — appended after `group by`, throwing `ILLEGAL_TYPE_OF_ARGUMENT` on every non-empty filter — behind a green `testUnit`. `STATE.md` records the lesson as "worth repeating at the end of Phases 8 and 10".

| Option | Description | Selected |
|--------|-------------|----------|
| `testUnit` + full live suite, re-baselined | Pass condition: Phase 8's 2 JMX-04 failures gone, zero new failures, remaining carry-ins named with owning phase | ✓ |
| `testUnit` + the JMX live tests only | Much faster; a regression elsewhere goes unseen until Phase 11 | |
| `testUnit` only, as ROADMAP criterion 3 words it | Literal reading; JMX-04 is a live test, so the phase's headline requirement would close on no evidence | |

**User's choice:** `testUnit` + full live suite, re-baselined (recommended option).

### Q2 — What should Phase 8 do with the 5 carry-in failures that are not its own?

| Option | Description | Selected |
|--------|-------------|----------|
| Take the UTF-8 3-liner now, triage `TableTest`, fix neither beyond that | Clears 4 diagnosed mojibake failures cheaply so the new full-suite gate is a clean signal; names a cause and owner for the ownerless failure | (Claude's call) |
| Leave all 5 — fix only Phase 8's two | Strict phase boundaries; runs the suite against 5 known reds for two more phases | |
| Take the UTF-8 fix and triage-and-fix `TableTest` too | Hands Phase 9 a suite whose only reds are Phase 10's; `TableTest` is unscoped work against a file byte-identical to upstream | |

**User's choice:** *"реши сам"* — delegated to Claude.
**Notes:** Call made and recorded as D-13 with its reasoning: take the UTF-8 fix (the gate just agreed in Q1 is undermined by four known reds that could mask a fifth; the fix is three diagnosed lines in `src/testFixtures`), triage `TableTest.extractNullables` without fixing it (`Table.java` is byte-identical to upstream, so it is either a stale fork expectation or a real upstream behaviour change — the latter deserves its own decision). Both cross phase boundaries, so both annotate `MERGE-CHECKLIST.md` rows rather than flipping them. A planner may override either half with a stated reason.

---

## Final gate

| Option | Description | Selected |
|--------|-------------|----------|
| I'm ready for context | Write CONTEXT.md and hand off to `/gsd-plan-phase 8` | ✓ |
| Explore more gray areas | Offered: ORCH-01/ORCH-04 evidence shape, plan/commit decomposition, and whether ObjectName-hostile connector names should stop being deferred | |

**User's choice:** I'm ready for context.
**Notes:** The three unexplored areas were resolved at Claude's discretion in CONTEXT.md (D-10, D-11, D-14, and the Deferred section respectively), each marked overridable.

---

## Claude's Discretion

- **The 5 non-Phase-8 carry-in failures** (explicitly delegated with "реши сам") — recorded as D-13.
- **Wave shape** — the JMX re-plumb is one connected edit with a single call site per level and does not decompose; the fixture fix is independent and could run in parallel; the full-suite run is necessarily last.
- **ORCH-01 / ORCH-04 evidence shape** — discharged by grep and by reading + recording, not by new code or new tests (D-10, D-11). Offered as an explorable area and declined.
- **`MBeanServerUtils` is not touched** — its `InstanceAlreadyExistsException` rethrow and `MalformedObjectNameException` swallow are both load-bearing for existing tests and neither is in this phase's requirements.
- **No new metric is added** — this phase changes only how the existing MBeans are named.
- **Spotless is run per-touched-file, not tree-wide** — an unrelated 52-file reformat has been pending since the merge and would destroy the one-behaviour-per-commit diff property (Phase 7 plan 07-01 precedent).

## Deferred Ideas

- **ObjectName-hostile connector names** (comma, `=`, quote) — `registerMBean` swallows `MalformedObjectNameException`, so such a connector runs while publishing no JMX metrics. Deliberately uncovered since Phase 4's D-05; quoting would change names operators already scrape.
- **A standing test that production uses `ARTIFACT_VERSION`** — rejected under D-07 for the same reason the literals stayed in `MBeanNameUnitTest`.
- **`TableTest.extractNullables()`** — triaged by D-13, fix deferred to whichever phase owns `Table`/mapping coverage.
- **Extending `ClickHouseBase`'s `@AfterEach` MBean leak sweep to per-topic MBeans** — Phase 10 / HARN-02. Phase 8 only changes the names it will have to match.
- **`ClickHouseSinkTask.version()` returns a hard-coded `"0.0.1"`** — noticed while reading the file, unrelated to the MBean `version=` segment, out of scope.
- **CR-01** — `.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error as "no match". Carried from Phase 6's security audit; must be fixed before Phase 11, which gates publication on that script running unattended.
