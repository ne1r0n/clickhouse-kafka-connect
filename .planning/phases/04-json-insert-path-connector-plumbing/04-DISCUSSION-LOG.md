# Phase 4: JSON Insert Path & Connector Plumbing - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-17
**Phase:** 04-json-insert-path-connector-plumbing
**Areas discussed:** JMX test seam, JSON route coverage, jsonInsertColumns, async_insert proof

---

## JMX test seam

### Q1 — How to make JMX-03 (null version → `unknown`, `:` → `_`) testable

| Option | Description | Selected |
|--------|-------------|----------|
| Extract a static helper | Package-private `static String buildMBeanName(connectorName, id, version)` in `ProxySinkTask`; `getMBeanName()` delegates. Docker-free unit test covers JMX-01/02/03. Cost: a main-code change in a production fork. | ✓ |
| Live-only, JMX-03 partial | No main-code change; JMX-01/02/04 live, JMX-03 left uncovered because the real client jar always reports a plain version. | |
| Reflection on the private method | Invoke the private `getMBeanName()` reflectively — still cannot inject a null or colon-bearing version, so JMX-03 stays uncoverable. | |

**User's choice:** Extract a static helper
**Notes:** Became the single pre-approved main-code change of the phase (CONTEXT D-01, D-02).

### Q2 — How to prove JMX-04 (distinct MBeans, `stop()` unregisters only its own)

| Option | Description | Selected |
|--------|-------------|----------|
| Live registration test | Two tasks under different `name` props plus two under the same name; query the platform MBean server filtered to this test's connector names; stop one, assert only that one disappears. | ✓ |
| Live test + helper unit test | The live test plus a Docker-free distinctness test on `buildMBeanName`. | |
| Helper unit test only | Distinct strings only — never exercises `registerMBean`/`unregisterMBean`. | |

**User's choice:** Live registration test
**Notes:** `ProxySinkTask.id` comes from a static `NEXT_ID` counter that never resets in a test JVM, so absolute names like `SinkTask0` are unassertable.

### Q3 — Whether to pin the ObjectName-hostile connector-name degradation

| Option | Description | Selected |
|--------|-------------|----------|
| Plain names only | JMX-01 asserts `connector=<name>`, JMX-02 asserts literal `connector=null`. Nothing about `,`/`=` in names. | ✓ |
| Also pin the silent degradation | One live test: odd name → task runs, no MBean registered, no exception. | |
| Pin it and flag as a bug | As above, plus a recorded follow-up candidate (`ObjectName.quote`). | |

**User's choice:** Plain names only
**Notes:** The degradation itself was preserved as a deferred idea rather than dropped.

### Q4 — Exact shape of the pre-approved `ProxySinkTask` change

| Option | Description | Selected |
|--------|-------------|----------|
| Extraction only | Helper holds today's logic and format string; `getMBeanName()` is a one-line delegate; output identical byte-for-byte; nothing else in `ProxySinkTask`/`MBeanServerUtils` changes. | ✓ |
| Extraction + version seam | Plus a `getMBeanName(String version)` overload for instance-path testing. | |
| Extraction + test accessors | Plus package-private accessors for `id`/`statistics`. | |

**User's choice:** Extraction only

---

## JSON route coverage

### Q1 — Which record route(s) JSON-01..04 are pinned on

| Option | Description | Selected |
|--------|-------------|----------|
| Schemaless-first | Schemaless `Map` records — the only route where metadata-free mode changes behavior (`getTable`, `cleanupExtraFields`, `jsonInsertColumns`). JSON-05 upgrades the existing String-record tests to value assertions. | ✓ |
| Both routes | Schemaless *and* String records, covering both converter setups; roughly doubles plan 04-01's live tests. | |
| String-record-first | Stay on the route the existing tests use; leaves JSON-02/03's actual subject untested. | |

**User's choice:** Schemaless-first
**Notes:** Driven by the finding that `doInsertStringV2` never passes `jsonInsertColumns` and never calls `cleanupExtraFields`.

### Q2 — The `insertFormat=json` + Struct + no-`bypassRowBinary` combination

| Option | Description | Selected |
|--------|-------------|----------|
| Probe, then decide | Research establishes ground truth; a fail-fast fix escalates only if silent data loss is proven. | ✓ |
| Pin whatever happens | Assert today's behavior as-is, no main-code change under any outcome. | |
| Out of scope | No test; deferred idea only. | |

**User's choice:** Probe, then decide
**Notes:** Became research flag R-01. The fix itself stays *unapproved* (D-01) — it would change behavior for a config combination that may run in production.

### Q3 — How to prove JSON-03's `cleanupExtraFields` bypass

| Option | Description | Selected |
|--------|-------------|----------|
| Unit pin + live values | Docker-free unit test via the existing `protected setSinkConfig(...)` seam, plus a live test that known fields land with correct values. | ✓ |
| Live differential | `input_format_skip_unknown_fields=0` contrast: explicit mode fails, default mode succeeds. | |
| Both | Unit pin, live values, and the differential. | |

**User's choice:** Unit pin + live values
**Notes:** `input_format_skip_unknown_fields=1` is added unconditionally by the connector, which is exactly what makes a plain live test blind here.

### Q4 — Which batch shapes JSON-04 covers

| Option | Description | Selected |
|--------|-------------|----------|
| Mixed + all-tombstone | Mixed batch plus an all-tombstone batch, where an empty JSONEachRow stream still reaches `client.insert`. | ✓ |
| Mixed batch only | Exactly what the requirement asks; all-tombstone deferred. | |
| Mixed + tombstone-first | Mixed batch plus a batch whose first record is a tombstone (routing quirk). | |

**User's choice:** Mixed + all-tombstone

---

## jsonInsertColumns

### Q1 — What COLS-05 (nonexistent column) asserts

| Option | Description | Selected |
|--------|-------------|----------|
| Pin the server error | `put()` throws, column name in the cause chain, `countRows == 0`, no NPE, and the exception classification is recorded. | ✓ |
| Pin, escalate if retriable | As above, plus stop for a decision if the error is retriable (would touch upstream-shared `Utils.handleException`). | |
| Pre-validate in main code | Check the list against the table at start — explicitly Out of Scope in PROJECT.md. | |

**User's choice:** Pin the server error
**Notes:** Under D-01 a retriable classification would still escalate rather than be fixed autonomously.

### Q2 — Whether COLS-02's empty-list research flag still needs a probe

| Option | Description | Selected |
|--------|-------------|----------|
| Evidence is sufficient | Phase 3's green round-trips already drove `doInsertJsonV2` with the default empty list; COLS-02 becomes a plain live value test. | ✓ |
| Probe anyway | Have research confirm what client-v2 emits for an empty column list and record raw evidence. | |
| Probe + version pin | As above, plus a regression assertion on the generated INSERT shape. | |

**User's choice:** Evidence is sufficient
**Notes:** The roadmap's COLS-02 research flag is formally withdrawn in CONTEXT D-11.

### Q3 — COLS-03's fixture shape

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit DEFAULT expressions | Non-trivial defaults (`DEFAULT 42`, `DEFAULT 'unset'`) so "received their DEFAULT" is observable. | ✓ |
| Plain columns | Assert `0`/`''`/NULL — indistinguishable from an unwritten column. | |
| Both shapes | One table of each. | |

**User's choice:** Explicit DEFAULT expressions

### Q4 — What COLS-06 asserts in the non-explicit path

| Option | Description | Selected |
|--------|-------------|----------|
| List applies + the interaction | Subset filled, unlisted columns default, *and* a valid-table-column key outside the list does not land — proving the column clause, not `cleanupExtraFields`, excludes it. | ✓ |
| List applies only | Mirror COLS-03 one config level up. | |
| List applies + bypassRowBinary | Also cover the Struct + `bypassRowBinary` route into `doInsertJsonV2`. | |

**User's choice:** List applies + the interaction

---

## async_insert proof

### Q1 — Where ASYNC-01/02 assert

| Option | Description | Selected |
|--------|-------------|----------|
| Config-level unit test | `new ClickHouseSinkConfig(props).getClickhouseSettings()` — no `async_insert` by default, user value round-trips. Docker-free, pins the exact line the fork changed. | ✓ |
| Wire-level live test | `system.query_log` Settings inspection for the insert query. | |
| Both | Config unit tests plus one live query_log check. | |

**User's choice:** Config-level unit test
**Notes:** Both insert paths copy every `clickhouseSettings` entry into their settings in straight-line code, so the config assertion is load-bearing.

### Q2 — How ASYNC-03 stays deterministic

| Option | Description | Selected |
|--------|-------------|----------|
| Set `wait_for_async_insert=1` | Insert returns only after the buffer flushes; assertion is immediate and doubles as a second ASYNC-02 pass-through case. | ✓ |
| Poll until visible | `async_insert=1` alone plus bounded polling — realistic but introduces a timing loop. | |
| Both settings, both shapes | One deterministic test and one realistic polling test. | |

**User's choice:** Set `wait_for_async_insert=1`

### Q3 — Test-class organization across the three wave-1 plans

| Option | Description | Selected |
|--------|-------------|----------|
| One class per concern | A new fork-only Docker-free class per concern, each owned by exactly one plan, plus one live class per plan. No shared files in the wave. | ✓ |
| One shared config class | COLS-01 and ASYNC-01/02 in a single `ClickHouseSinkConfig` test class — written by two plans. | |
| Grow existing fork files | Add cases to existing fork-only classes where they fit. | |

**User's choice:** One class per concern

### Q4 — Phase 4's main-code fix budget

| Option | Description | Selected |
|--------|-------------|----------|
| Extraction only, rest escalates | `buildMBeanName` is the only pre-approved change; every other main-code edit stops for a user decision, including the Struct fail-fast, any `jsonInsertColumns` validation, and anything in `Utils.handleException`. | ✓ |
| Also pre-approve the Struct fail-fast | Pre-authorize a fail-fast throw if R-01 proves silent data loss. | |
| Zero main-code changes | Tests only; JMX-03 becomes uncoverable. | |

**User's choice:** Extraction only, rest escalates

---

## Claude's Discretion

- Names and packages of the new fork-only test classes, and their exact mapping onto 04-01 / 04-02 / 04-03.
- `buildMBeanName`'s signature details beyond "package-private static, byte-for-byte identical output".
- Live fixture table shapes, column types, record counts, and table sharing within a class.
- Which COLS-01 parsing inputs and which JSON-01 letter-case variants get parameterized.
- `@DisplayName` phrasing beyond the mandatory `REQ-ID: ` prefix.

## Deferred Ideas

- `MBeanServerUtils.registerMBean` swallows `MalformedObjectNameException`, so a connector name containing `,` or `=` silently publishes no JMX metrics. A future `ObjectName.quote` fix would change names operators may already scrape.
- The String-record route silently ignores `jsonInsertColumns` — no error, no column clause.
- Metadata-free `getTable` bypasses `Utils.getTableName`, skipping the db-topic-split path in explicit JSON mode.
- Wire-level `system.query_log` verification that no `async_insert` reaches the server.
- COLS-01 parsing edges beyond the requirement: duplicates, backticked/quoted names, names with spaces.
