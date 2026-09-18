# Phase 1: Test Infrastructure & Green Baseline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-15
**Phase:** 01-test-infrastructure-green-baseline
**Areas discussed:** Baseline failure policy, `latest` image & proxy tests, Lifecycle retrofit scope, Fork-changed coverage view

---

## Baseline failure policy

### Existing upstream test fails on `latest` × V2 because its expectation is outdated (the connector is correct)

| Option | Description | Selected |
|--------|-------------|----------|
| Update test to latest (Recommended) | Change the assertion to match `latest`; test-only diff; record each change in the phase summary | ✓ |
| @Disabled with reason | Skip with a comment naming the ClickHouse change; behavior no longer covered | |
| Decide per test | Stop and ask about each failure | |

**User's choice:** Update test to latest

### Baseline failure caused by a real bug in upstream (non-fork) main code

| Option | Description | Selected |
|--------|-------------|----------|
| Fix if small, else ask (Recommended) | Targeted few-line rebase-friendly fix (mirror the upstream fix if one exists); bigger fixes stop for a decision | ✓ |
| Disable and defer | @Disabled + deferred entry; no upstream main-code changes | |
| Always fix in main code | Fix whatever the size | |

**User's choice:** Fix if small, else ask

### What the baseline does with existing @Disabled tests

| Option | Description | Selected |
|--------|-------------|----------|
| Leave all as-is (Recommended) | Baseline covers the suite as it is today; Variant tests are upstream; JsonColumnTest case belongs to Phase 3; perf tests stay off | ✓ |
| Try re-enabling upstream Variant | Remove @Disabled from the 2 Variant tests if they pass on `latest` | |
| Re-enable all that pass | Try every @Disabled test except perf; pulls in Phase 3 JsonColumnTest work | |

**User's choice:** Leave all as-is

### Flaky baseline tests

| Option | Description | Selected |
|--------|-------------|----------|
| Fix root cause, no retries (Recommended) | Rerun to confirm, then fix the cause; no retry plugin | ✓ |
| Add Gradle test-retry plugin | Automatic retries; a second new dependency; can hide flakiness | |
| @Disabled flaky tests | Turn off with a reason and defer | |

**User's choice:** Fix root cause, no retries
**Notes:** Scouting found the colima `vz` VM also runs a local Airflow stack, so resource pressure is a realistic cause of flakes.

---

## `latest` image & proxy tests

### How ClickHouse-backed tests get the moving `latest` image (not cached locally)

| Option | Description | Selected |
|--------|-------------|----------|
| Always pull (Recommended) | `PullPolicy.alwaysPull()`; local `latest` matches CI | ✓ |
| Age-based pull | `PullPolicy.ageBased(...)`; local can lag CI | |
| Cached, pull manually | Testcontainers default + manual `docker pull` | |

**User's choice:** Always pull

### What the version smoke test checks for `latest`

| Option | Description | Selected |
|--------|-------------|----------|
| Match image's version label (Recommended) | `version()` must equal the pulled image's version metadata; logs the version; research picks a fallback if there's no label | ✓ |
| Minimum version floor | `version()` ≥ a floor above 25.3; floor ages, misses a stale cached image | |
| Log only + assert tag | Assert the image resolves to `:latest`, log `version()` | |

**User's choice:** Match image's version label

### Toxiproxy classes pinned to CH 23.8 (overwrite the shared `db`)

| Option | Description | Selected |
|--------|-------------|----------|
| Keep 23.8, isolate (Recommended) | Documented exception; own container field/lifecycle; never touch the shared `latest` container | ✓ |
| Move to `latest` | Accept #58828 breakage; rework or @Disable cases | |
| Separate tag, out of `test` | Exclude from default `test`/CI; runnable on demand | |

**User's choice:** Keep 23.8, isolate
**Notes:** Checked ClickHouse #58828 before asking: "does not support URI with absolute-form" (Toxiproxy); still open.

### Class isolation on the shared container

| Option | Description | Selected |
|--------|-------------|----------|
| Fresh database per class (Recommended) | Unique database per class in @BeforeAll, dropped in @AfterAll | ✓ |
| Shared database, fix on failure | Keep the static shared database; fix collisions only when a test breaks | |
| Rename fixed names | Convert duplicated fixed table names to `createTopicName` | |

**User's choice:** Fresh database per class
**Notes:** Scouting found 65 fixed table names vs 66 `createTopicName` uses, with duplicates across classes that will share a container (`special-char-table-test` ×3 classes, `mapping_table_test`/`table_mapping_test`, `array_string_table_test`).

---

## Lifecycle retrofit scope

### How far the leak-safety work goes (~115 start() calls in 11 files, no try/finally)

| Option | Description | Selected |
|--------|-------------|----------|
| Base cleanup + new-test rule (Recommended) | @AfterEach in ClickHouseBase unregisters and logs leftover connector MBeans; new/touched tests use try/finally + unique name; deliberate-failure proof test | ✓ |
| Retrofit all existing tests | Wrap every existing start/put/stop; biggest upstream diff | |
| New/touched tests only | No base safety net | |

**User's choice:** Base cleanup + new-test rule
**Notes:** MBean ids come from a static `AtomicInteger`, so leaks linger rather than crash today.

### How new tests get a unique connector name (JMX-02 pins `connector=null`)

| Option | Description | Selected |
|--------|-------------|----------|
| Opt-in helper (Recommended) | `createProps()` unchanged; ClickHouseBase helper adds a unique `name` | ✓ |
| Default in createProps | Always add a unique name; JMX-02 tests must remove it | |
| Inline per test | Each test sets `name` by hand | |

**User's choice:** Opt-in helper

### When main-code `describeTable` readers get explicit UTF-8

| Option | Description | Selected |
|--------|-------------|----------|
| Helpers now, main when proven (Recommended) | Phase 1 fixes `ClickHouseTestHelpers.java:158/:252`; `ClickHouseHelperClient.java:370/:417` fixed in Phase 3 when non-ASCII tests show the problem | ✓ |
| All four now | Also change the two main-code readers in Phase 1 | |
| Helpers only, main out of scope | Never touch production decoding in this project | |

**User's choice:** Helpers now, main when proven

### Where the ClickHouse JUnit tag goes

| Option | Description | Selected |
|--------|-------------|----------|
| On ClickHouseBase (Recommended) | One inherited `@Tag("clickhouse")` covers all 14 subclasses | ✓ |
| On each of the 14 classes | Explicit per file; edits 14 upstream files | |
| Base + guard test | Plus a check that no other class starts a Testcontainer | |

**User's choice:** On ClickHouseBase

---

## Fork-changed coverage view

### How the fork-changed class list is defined

| Option | Description | Selected |
|--------|-------------|----------|
| Hand-picked list (Recommended) | Named include list for the 11 fork additions | ✓ |
| Derived from git diff | `git diff --name-only main...custom_build` at build time; import-only noise; breaks on shallow CI checkouts | |
| Package-level globs | Whole packages; pulls in upstream-only classes | |

**User's choice:** Hand-picked list

### How the fork-focused view is delivered

| Option | Description | Selected |
|--------|-------------|----------|
| 2nd report task after `test` (Recommended) | Filtered JacocoReport, own HTML+XML, runs automatically after `test` | ✓ |
| On-demand task only | Same task, run explicitly | |
| Single report + class list doc | Full report only; manual review | |

**User's choice:** 2nd report task after `test`

### Lombok-generated code exclusion

| Option | Description | Selected |
|--------|-------------|----------|
| Add root lombok.config (Recommended) | `config.stopBubbling = true` + `lombok.addLombokGeneratedAnnotation = true`; JaCoCo filters `@lombok.Generated` | ✓ |
| JaCoCo class excludes only | Builder class patterns; getters/setters in Column/Table remain | |
| Verify default first | Build the report, inspect Column, add lombok.config only if needed | |

**User's choice:** Add root lombok.config
**Notes:** No `lombok.config` exists here or on upstream `main`.

### Which tasks are instrumented, and `testUnit` vs `check`

| Option | Description | Selected |
|--------|-------------|----------|
| `test` only; testUnit off check (Recommended) | Agent off on `testUnit`/`integrationTest`; `testUnit` excluded from `check`; reports wired to `test` only; existing `check`→`integrationTest` untouched | ✓ |
| Instrument testUnit too | Separate partial testUnit report | |
| Plugin defaults | Agent on all Test tasks; `testUnit` in `check` | |

**User's choice:** `test` only; testUnit off check

---

## Claude's Discretion

- Shared-container cleanup mechanism (Ryuk vs JVM shutdown hook)
- Names: connector-name helper, fork report task, per-class database scheme, report directory
- Where the version smoke test lives
- How the proxy classes get their own container field (separate static field vs small base hook)
- `testUnit` classpath/tag-exclusion wiring and how it's kept out of `check` in the Kotlin DSL
- Whether to lower `maxParallelForks` locally if colima resource pressure causes flakes

## Deferred Ideas

- Explicit UTF-8 in main-code `describeTable` readers → Phase 3
- Re-enabling upstream Variant @Disabled tests → not a fork addition; possible future upstream-sync item
- Pre-existing `check` → `integrationTest` dependency → upstream quirk, untouched
- Moving Toxiproxy tests to `latest` → blocked by ClickHouse #58828
