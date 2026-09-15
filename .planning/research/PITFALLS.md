# Pitfalls Research

**Domain:** Adding unit + Testcontainers integration tests, JaCoCo coverage, and GitHub Actions CI to a production Kafka Connect sink connector fork (ClickHouse Kafka Connect, Java/Gradle)
**Researched:** 2026-09-15
**Confidence:** HIGH for pitfalls grounded directly in this repo's source (cited with file:line); MEDIUM for general ClickHouse/Testcontainers/JaCoCo ecosystem claims verified against current docs/issues; LOW/flagged inline where a claim could not be independently verified and should be confirmed by a spike test.

Every pitfall below was checked against the actual fork source (`src/main/java/com/clickhouse/kafka/connect/...`) or current upstream documentation/issue trackers — not recalled from general knowledge. Line numbers refer to the `custom_build` branch at commit range ending `22f055c`/`3107d71`.

## Critical Pitfalls

### Pitfall 1: `CLICKHOUSE_VERSION` silently defaults to `25.3`, not `latest`

**What goes wrong:**
`ClickHouseTestHelpers.CLICKHOUSE_VERSION_DEFAULT = "25.3"` (`src/test/java/.../helper/ClickHouseTestHelpers.java:41`) and `getClickhouseVersion()` falls back to that constant whenever the `CLICKHOUSE_VERSION` env var is unset. The project's own constraint says "ClickHouse `latest` + client V2 only — matches production." Any test run — locally (`./gradlew test` with no env var) or in a CI job that forgets to set `CLICKHOUSE_VERSION=latest` — silently runs against 25.3 instead of `latest`, and "all green" gives false confidence about `latest` behavior.

**Why it happens:**
The default was presumably chosen as a stable baseline for the *existing* upstream test suite, before the fork's `latest`-only constraint was decided. Nobody updated the fallback.

**How to avoid:**
Either flip the default constant to a marker that forces explicit selection (fail fast if unset in the fork's own CI job), or add an assertion in `ClickHouseBase` that logs/fails loudly when running fork-specific JSON/Dynamic tests against anything other than `latest`. At minimum, the fork's new CI job must hard-code `CLICKHOUSE_VERSION: latest` and a smoke test should assert the running container's actual `VERSION()` string is what was requested.

**Warning signs:**
`./gradlew test` "passes" locally without ever setting `CLICKHOUSE_VERSION`; CI job YAML doesn't show `CLICKHOUSE_VERSION=latest` in every fork-specific step's `env:` block.

**Phase to address:**
Baseline/infra phase (establish the `./gradlew test` baseline) and the CI phase — both must pin `latest` explicitly, not rely on the default.

---

### Pitfall 2: One unrecognized `DESCRIBE` subcolumn silently drops the entire table from mapping

**What goes wrong:**
`ClickHouseHelperClient.describeTableV1/V2` (`.../db/helper/ClickHouseHelperClient.java:388-393`, `435-440`) calls `Column.extractColumn(fieldDescriptor)`; if it returns `null` for *any single column*, the method logs a warning and returns `null` for the **whole table** — the table is then absent from `mapping`, and inserts to it fail with "table not found" rather than a clear per-column error. `Column.extractColumn` already returns `null` for `Nested` types (by design) and for anything `dispatchPrimitive` can't classify (falls through to `Type.UNKNOWN`, but only `Nested` explicitly returns null — other unknowns pass through with `Type.UNKNOWN`, which downstream write logic treats as unsupported). With `describe_include_subcolumns` on, `latest`'s JSON/Dynamic types under active development (e.g. new internal subcolumn suffixes beyond the three patterns the fork strips: `.null`, `.size0`, `.object_shared_data.`) can introduce a subcolumn name the fork's skip-list and `Column.extractColumn` don't yet recognize — silently disabling the table.

**Why it happens:**
The skip-list of "system" subcolumn suffixes was hand-written against one ClickHouse version's subcolumn naming; ClickHouse's JSON/Dynamic internal representation (shared-data columns, per-path typed subcolumns) is still evolving release to release, so `latest` can introduce new suffixes the fork hasn't seen.

**How to avoid:**
Add tests that `DESCRIBE` a table with `JSON`, `Dynamic`, `Nullable`, and `Array` columns against the actual `latest` image and assert (a) the subcolumn skip produces the exact expected column set, and (b) no `Column.extractColumn` call returns `null` for any row of `DESCRIBE ... describe_include_subcolumns`. Log at ERROR (not WARN) when a whole table is dropped due to one bad column — WARN-level silent table drop is exactly the failure mode to catch.

**Warning signs:**
A table with JSON/Dynamic columns is present in ClickHouse but never appears in connector logs as "found N tables"; inserts fail with "table not found in mapping" despite the table existing.

**Phase to address:**
Table metadata & types phase (`describeTable` sub-column skipping, `JSON(...)`/`Dynamic` mapping).

---

### Pitfall 3: `targetTableFilter` regex is Java-authored but evaluated as RE2 server-side

**What goes wrong:**
`ClickHouseHelperClient.showTables` (line 297) builds `match(table, '<regex>')` where `<regex>` is the raw `targetTableFilter` config value with only single quotes doubled for SQL-string-literal escaping (`replace("'", "''")`). ClickHouse's `match()` uses **RE2**, not Java's `java.util.regex`. Two concrete gaps: (1) backslashes are not doubled for the SQL string literal, so a Java-idiomatic regex like `\d+` becomes the ClickHouse string literal `\d+`, which the SQL parser may interpret differently than intended (ClickHouse string literals require `\\d` to produce the regex token `\d`); (2) RE2 does not support some Java regex constructs (lookahead/lookbehind, backreferences) — a filter that "compiles" fine as a Java `Pattern` in a unit test can be rejected or silently misbehave as RE2 server-side. Also, RE2's `.` matches line breaks by default (ClickHouse-specific behavior, unlike Java's default), which matters if table names could ever contain newlines (unlikely, but the matcher's semantics genuinely differ from Java's).

**Why it happens:**
`match()`/RE2 "looks like" standard regex and passes for simple patterns (`^prefix_.*`), so the Java/RE2 gap only surfaces on patterns exercising escapes, anchors, or unsupported constructs.

**How to avoid:**
Test `targetTableFilter` **against the real ClickHouse container**, not just as a Java `Pattern` unit test — a pattern that compiles in Java proves nothing about RE2 behavior. Include a case with a backslash-containing regex (e.g. filtering `table_\d+`) and assert it matches/doesn't match the intended tables via an actual `SHOW TABLES`/`system.columns` query, not a mocked string comparison. Document in the connector's config description that the filter is RE2, not Java regex.

**Warning signs:**
A `targetTableFilter` unit test only asserts against a Java `Pattern.matches()` call rather than a live `match()` query result.

**Phase to address:**
Table metadata & types phase (`targetTableFilter` regex tests).

---

### Pitfall 4: `jsonInsertColumns` defaults to an **empty list**, not `null` — untested interaction with client-v2's `insert(table, columns, ...)` overload

**What goes wrong:**
`ClickHouseSinkConfig` builds `jsonInsertColumns` as `new ArrayList<>()` when the `jsonInsertColumns` prop is unset or blank (`ClickHouseSinkConfig.java:242-252`), and `ClickHouseWriter.doInsertJsonV2` unconditionally calls `client.insert(table.getName(), jsonInsertColumns, data, ClickHouseFormat.JSONEachRow, insertSettings)` (line 999) with that possibly-empty list. Whether client-v2 0.9.6's `insert(String, List<String>, InputStream, ...)` overload treats an **empty but non-null** column list the same as "no explicit columns" (defer entirely to the JSONEachRow payload) or generates a malformed/empty `INSERT INTO table ()` column clause is unverified — this is exactly the untested boundary the project's own inventory flags ("`jsonInsertColumns`: none" existing tests). This is the single highest-risk untested path in the fork because it's the default behavior for every connector that sets `insertFormat=json` without also setting `jsonInsertColumns`.

**Why it happens:**
The WIP commit added the config plumbing before writing a test that exercises the empty-list default against a real server; `List<String>` "empty" vs `null` is an easy category to overlook when the immediate goal was "does it compile."

**How to avoid:**
Write a Testcontainers test for the *default* (`jsonInsertColumns` unset) explicit-JSON-insert path first — before testing the explicit column-subset case — and assert rows land correctly with correct column values (not just row count, which the existing `ClickHouseSinkTaskJsonInsertFormatTest` mostly checks). Then test explicit column subsets separately, including a column list that omits columns with defaults (verify defaults apply) and one that includes a nonexistent column name (verify the failure is a clear ClickHouse error, not a client-side NPE).

**Warning signs:**
Existing `ClickHouseSinkTaskJsonInsertFormatTest` tests pass but only assert `countRows`, not column-level content; no test sets `insertFormat=json` while leaving `jsonInsertColumns` unset and checks the *values* landed.

**Phase to address:**
Explicit JSON insert path phase.

---

### Pitfall 5: `MBeanServerUtils.registerMBean` throws hard on collision, and existing test style doesn't use try/finally around `stop()`

**What goes wrong:**
`registerMBean` (`util/jmx/MBeanServerUtils.java:17-29`) rethrows `InstanceAlreadyExistsException` as an unchecked `RuntimeException` (all *other* exceptions are caught and merely logged as a warning) — so a second `ClickHouseSinkTask`/`ProxySinkTask` started in the same JVM with the same effective MBean name (same connector `name`, or both unset/default) crashes hard on `start()`. `ProxySinkTask.stop()` does call `MBeanServerUtils.unregisterMBean(...)` (`ProxySinkTask.java:82`), so the design intent is correct — **but** the existing fork test pattern (e.g. `JsonColumnTest.runJsonTest`, `src/test/java/.../sink/JsonColumnTest.java:70-73`) calls `sinkTask.start(props); sinkTask.put(...); sinkTask.stop();` **without try/finally**. If an assertion or exception occurs between `start()` and `stop()` (e.g. `put()` throws, or a later `assertEquals` fails before `stop()` is reached), the MBean is never unregistered and leaks into the JVM's platform MBeanServer for the rest of that test fork/process, causing the *next* test that starts a task with a colliding name to fail with `InstanceAlreadyExistsException` — a classic "fails only when run as part of the full suite" flake.

**Why it happens:**
The lifecycle looks correct in the happy path; the failure only appears once a test genuinely fails or throws before reaching `stop()`, which is rare during initial development but common once new fork tests start probing edge cases (that's the whole point of this project).

**How to avoid:**
New tests must wrap `start()`/`put()`/`stop()` in try/finally (or a JUnit `@AfterEach` that always calls `stop()`), and each test should use a unique connector `name` (not rely on config defaults) to avoid cross-test MBean name collisions even if cleanup does happen. Consider hardening `registerMBean` to unregister-then-reregister or warn instead of throw — but that's a genuine bug fix, worth doing since it directly serves "tests uncovering bugs get fixed."

**Warning signs:**
A test passes in isolation (`./gradlew test --tests "...JsonColumnTest"`) but fails with `InstanceAlreadyExistsException` only when the full suite runs; flaky failures correlate with `maxParallelForks` grouping, not with the test's own logic.

**Phase to address:**
Connector plumbing phase (MBean tests) — but the try/finally discipline must be established in every phase that starts a `ClickHouseSinkTask`, since it's a suite-wide hazard, not just an MBean-test-local one.

---

### Pitfall 6: `Type.DYNAMIC` has no RowBinary serializer — the natural "fix" is scope creep

**What goes wrong:**
`Column.java` recognizes `Dynamic` in `dispatchPrimitive` (`Type.DYNAMIC`, lines 154-155), so `describeTable` happily maps a `Dynamic` column into the normal (non-explicit-JSON) insert path's table mapping. But `ClickHouseWriter`'s RowBinary type-dispatch switch has no case for `Type.DYNAMIC` (confirmed in `CONCERNS.md`: "Dynamic type added ... but no serialization logic in ClickHouseWriter"). Writing to a table with a `Dynamic` column via the *default* (non-`insertFormat=json`) sink path will hit the writer's fallback — CONCERNS.md notes the type dispatch has "no fallback for unknown types (defaults to logging + skip)" — meaning data for that column is silently dropped rather than erroring.

**Why it happens:**
`Dynamic` support was added to unblock the explicit-JSON insert path (where ClickHouse parses JSONEachRow server-side and RowBinary is irrelevant), but the mapping/type layer is shared with the RowBinary path, so it "leaks" Dynamic-column support into a path that can't actually serialize it.

**How to avoid:**
Once a test proves this (per PROJECT.md's own risk note), resist implementing full Dynamic RowBinary serialization — that's new capability in a 1244-line class explicitly marked out-of-scope for refactor/expansion. The rebasable fix is a fast, explicit, clearly-worded exception (e.g. `UnsupportedOperationException`/`ConfigException`: "Dynamic columns require insertFormat=json") thrown during table mapping or at the point of write, pinned by a test asserting *that specific error*, not new serialization logic.

**Warning signs:**
A test writes a record with a Dynamic-typed field through the default sink path and the row lands with that field silently null/missing instead of erroring — "looks like it worked" but data was dropped.

**Phase to address:**
Table metadata & types phase (write the test that proves the gap) feeding directly into the "fix bugs found by tests" quality gate — scope the fix to an explicit error, not new serialization.

---

### Pitfall 7: `windows-1252` test encoding is global and most stream readers don't specify a charset

**What goes wrong:**
`build.gradle.kts:174` sets `systemProperty("file.encoding", "windows-1252")` for **all** tests, presumably to catch non-UTF-8 handling bugs (there's already a `countRowsWithEmojis` helper acknowledging this risk area). But most of the fork's stream-reading code does not pass an explicit `Charset` — e.g. `new InputStreamReader(queryResponse.getInputStream())` in `ClickHouseHelperClient.describeTableV1/V2` and in `ClickHouseTestHelpers.getAllRowsAsJson`/`validateRows` — so under the JVM-wide `windows-1252` default, any test that inserts or reads back JSON/Dynamic column data, transform output (header values, `ConvertToMapOrJson` JSON strings), or table names containing non-ASCII characters risks the response bytes being decoded with the wrong charset, producing mangled strings that make assertions fail (or, worse, silently pass by accident because both sides of the comparison are wrong the same way).

**Why it happens:**
`file.encoding` is a JVM-wide default that many stream APIs fall back to implicitly; it's easy to write a new test helper that "works" under the default UTF-8 locally and then breaks (or is subtly wrong) once someone runs the actual configured test JVM.

**How to avoid:**
Any new test/helper reading ClickHouse HTTP responses (JSON, JSONEachRow, DESCRIBE output) must construct `InputStreamReader`/`BufferedReader` with an explicit `StandardCharsets.UTF_8`, not the platform default. Add a regression test with genuinely non-ASCII content (not just emoji, e.g. accented Latin or CJK) in a `JSON`/`Dynamic`/`String` column and confirm round-trip equality under the `windows-1252` test JVM setting specifically (don't just run it under a normal UTF-8 shell).

**Warning signs:**
New assertions comparing JSON/Dynamic column string content with non-ASCII characters are flaky or fail only under `./gradlew test` (which sets `windows-1252`) but pass when run directly from an IDE (which may not apply the Gradle system property).

**Phase to address:**
Table metadata & types phase (JSON/Dynamic content assertions) and transforms phase (header/value content with non-ASCII).

---

### Pitfall 8: `skipped.operations` silently rejects invalid values instead of failing config validation — and doesn't recognize Debezium's `r` (snapshot read)

**What goes wrong:**
`TableOperationFilter.configure` (`transforms/TableOperationFilter.java:70-82`) only accepts single-character values from the set `c`, `u`, `d`, `t`; anything else (including a config typo, or `r` for Debezium's snapshot-read operation) is dropped with a `LOGGER.warn(...)` and otherwise ignored — there is no `ConfigException`. An operator who intends to also skip snapshot-read events (`op: "r"`, common during initial Debezium snapshots) by adding `r` to `skipped.operations` will have that silently no-op'd; the connector starts "successfully" with a filter that's weaker than configured. Separately, `apply()` (line 105) calls `value.getString("op")`, which assumes a `Struct` with a schema field named `op` — a schemaless (`Map`-valued) Debezium-envelope record would hit the earlier `!(record.value() instanceof Struct)` guard and pass through unfiltered rather than erroring, which may or may not be the intended behavior for schemaless CDC pipelines.

**Why it happens:**
Validation-by-warning-log is a common shortcut that avoids `ConfigDef` validator complexity, but it means a misconfiguration is discoverable only by reading connector startup logs, not by connector `validate` API/Connect REST config validation.

**How to avoid:**
Add tests asserting: (1) an invalid/unsupported single-char operation in `skipped.operations` is either rejected via `ConfigException` at validate-time (preferred fix) or, if kept as warn-and-ignore, that the test explicitly documents and pins that this is a silent no-op — not an assumption; (2) `r` (Debezium snapshot read) behavior is explicitly tested and documented as "not filterable" if that's the pinned behavior; (3) a schemaless Debezium-envelope Map record either behaves identically to the Struct path or the difference is documented and tested.

**Warning signs:**
An operator reports "table filter isn't working for snapshot reads" — this is expected/current behavior, but only if it's tested and documented, not accidentally correct.

**Phase to address:**
Transforms phase (`TableOperationFilter` gaps — truncate op, multiple tables, invalid/empty `skipped.operations`).

---

### Pitfall 9: Adding JaCoCo without excluding Lombok-generated code skews "fork-changed classes" coverage numbers

**What goes wrong:**
Fork-touched classes (`Column.java`, `Table.java`, config builders) use Lombok's `@Builder`, `@Getter`, `@Setter`, `@Accessors(fluent = true)` extensively (`Column.java:17-20, 24-25, 32-52`). Lombok-generated builder/getter/setter code counts toward JaCoCo's line/branch totals unless JaCoCo is told to exclude it (via `@lombok.Generated` annotation retention, enabled by default in modern Lombok, combined with JaCoCo's automatic `@Generated`-based exclusion — but this must actually be verified working, not assumed). Without exclusion, a coverage target scoped to "fork-changed classes" (per the quality-gate requirement) is diluted by boilerplate that was never meant to be exercised directly, making genuinely under-tested hand-written logic (e.g. `dispatchPrimitive`'s Decimal-parsing regex, `splitUnlessInBrackets`) look better-covered than it is, or making a reasonable coverage threshold unreachable because of noise.

**Why it happens:**
JaCoCo + Lombok interaction "mostly works" by default in recent versions, which makes it easy to add the plugin, see a coverage percentage, and not verify it's measuring the right thing.

**How to avoid:**
After adding JaCoCo, generate a report and manually inspect `Column.java`'s coverage detail to confirm generated builder methods are excluded (not just "low coverage is fine, that's Lombok"); if not auto-excluded, add explicit `xml`/`class` exclusion patterns for Lombok-annotated members in the JaCoCo Gradle config. Treat the fork's own hand-written logic (type dispatch, filters, transforms) as the coverage target, not the aggregate class percentage.

**Warning signs:**
Coverage report shows suspiciously high numbers on `Column.java`/`Table.java` despite `JsonColumnTest`/`describeTable` tests being sparse — a sign builder/getter boilerplate is being counted as "covered" incidentally.

**Phase to address:**
Coverage + CI phase (JaCoCo setup) — verify exclusion behavior before trusting any coverage number reported to stakeholders.

---

### Pitfall 10: `.github/workflows/tests.yaml` uses a deprecated action, and the fork's Actions may never have actually run

**What goes wrong:**
The workflow (`.github/workflows/tests.yaml`) uses `gradle/gradle-build-action@v2`, which is superseded by `gradle/actions/setup-gradle` — v2 is old enough that it predates the rename and is not receiving the same maintenance/runner-compatibility updates. Separately, and more urgently: `ne1r0n/clickhouse-kafka-connect` is a **fork** of `ClickHouse/clickhouse-kafka-connect`; GitHub disables Actions on newly created forks by default until the fork owner explicitly enables them in Settings → Actions. PROJECT.md already flags this as unverified ("Whether GitHub Actions is enabled/green on the fork is unverified"). Building a JaCoCo-reporting CI job on top of a workflow file that has possibly never executed on this specific fork risks discovering — at the end of the project — that the "green CI" deliverable doesn't actually run at all.

**Why it happens:**
A workflow YAML existing in the repo and being syntactically valid says nothing about whether the platform is actually executing it for this specific fork; this is easy to overlook without directly checking fork settings/API.

**How to avoid:**
Before building on top of the existing workflow, verify via `gh api repos/ne1r0n/clickhouse-kafka-connect/actions/permissions` (or the Settings UI) that Actions are enabled for the fork, and confirm at least one workflow run exists/succeeds on `custom_build` before treating the current YAML as a working baseline. Separately, migrate `gradle/gradle-build-action@v2` → `gradle/actions/setup-gradle` while touching this file anyway (low-risk, well-documented migration), and scope the new fork-specific job to `CLICKHOUSE_VERSION=latest` / `CLIENT_VERSION=V2` only rather than piling coverage-instrumented Testcontainers runs onto all 8 existing matrix legs (many of which — 23.7/24.3 — are explicitly out of scope and will fail for JSON/Dynamic-dependent new tests unless properly version-gated with the existing `@SinceClickHouseVersion` pattern).

**Warning signs:**
No workflow run history visible for `custom_build` in the Actions tab; `gh run list` returns nothing for the branch.

**Phase to address:**
CI phase — as the very first step, before writing any new workflow YAML.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|-----------------|------------------|
| Warn-and-ignore invalid `skipped.operations`/config values instead of `ConfigException` | Simpler `configure()` code, connector still starts | Misconfiguration discoverable only via log-reading; operators believe a filter is active when it silently isn't | Never for new config surface; acceptable to *pin as-is* for existing WIP behavior only if explicitly tested and documented per PROJECT.md's "test WIP as-is" decision |
| Catch-and-return-`null` on `describeTable` failure for a whole table | Simple error handling, connector keeps running | One bad JSON/Dynamic subcolumn silently removes an entire table from the mapping with only a WARN log | Only acceptable short-term if paired with a loud ERROR-level log and a test asserting the exact drop condition, so it's a known/tested limitation, not a hidden one |
| No try/finally around `sinkTask.start()/put()/stop()` in test helpers (existing pattern in `JsonColumnTest`) | Slightly less boilerplate per test | MBean leaks across tests in the same JVM fork on any assertion failure, causing suite-order-dependent flakiness | Never — every new fork test should fix this pattern, not copy it |
| Rely on JaCoCo's default Lombok-generated-code exclusion without verifying it | Faster to "turn on JaCoCo and get a number" | Coverage percentage may be meaningless for the classes that matter most (`Column.java`, config builders) | Never for the coverage number used in the quality gate — verify once, then it's fine to trust |
| Test only row counts (`countRows`) instead of column-level values for new insert paths (existing `JsonColumnTest`/`ClickHouseSinkTaskJsonInsertFormatTest` pattern) | Fast to write, catches gross failures | Misses subtle serialization bugs (wrong column mapped, `Dynamic`/`JSON` values malformed, default-column fallback wrong) that a row-count check can't detect | Acceptable only as a first smoke test, must be paired with at least one value-asserting test per new insert path |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|-----------------|-------------------|
| ClickHouse `match()` / `targetTableFilter` | Testing regex logic as a plain Java `Pattern` unit test | Test against a live `match()` query on the real container; RE2 ≠ Java regex (escaping, lookaround, `.` matching newlines) |
| ClickHouse client-v2 `insert(table, columns, stream, format, settings)` (0.9.6) | Assuming an empty `List<String>` columns argument behaves like `null` (no explicit columns) | Write a real-container test with the default (empty list) `jsonInsertColumns` and assert both success and correct column mapping — don't assume based on the method signature |
| `DESCRIBE TABLE ... SETTINGS describe_include_subcolumns` | Assuming the current three-suffix skip-list (`.null`, `.size0`, `.object_shared_data.`) is exhaustive for `latest` | Run `DESCRIBE` against `latest` with real JSON/Dynamic tables and diff the raw subcolumn list against what the code currently filters, before trusting the skip-list is complete |
| Testcontainers ClickHouse + colima on macOS | Assuming `docker` CLI working implies Testcontainers/Ryuk works identically to CI's native Docker | Explicitly set/document `DOCKER_HOST`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`, and verify Ryuk container starts before relying on any `ClickHouseBase`-derived test locally |
| GitHub Actions on a fork | Assuming a workflow YAML in the repo means CI is running | Verify Actions are enabled for the fork and a run history exists before building new jobs on top of it |
| Debezium envelope `op` field | Assuming `op` is always present on a `Struct`-schema record | `TableOperationFilter`/other SMTs already guard with `hasField`; new SMT tests should include records missing `op`, missing `source`, and schemaless (Map) envelopes to confirm pass-through vs. error behavior is intentional |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| One Testcontainers ClickHouse container per new test class (existing `ClickHouseBase` pattern) | CI wall-clock time grows roughly linearly with number of new fork-specific test classes (JSON/Dynamic, jsonInsertColumns, describeTable, MBean, async_insert) | Group related fork-behavior tests into fewer classes sharing one container where test isolation allows (unique table names per method, already the established pattern via `createTopicName`) | Noticeable once 5-10 new container-per-class test classes are added on top of existing ones; compounds with `maxParallelForks` resource contention on constrained CI/local runners |
| JaCoCo instrumentation overhead stacked on Testcontainers-heavy integration tests | Test suite noticeably slower after JaCoCo is enabled, especially in classes with many small assertions inside loops (e.g. `JsonColumnTest`'s 10,000-record insert) | Scope JaCoCo to the `test` task's actual need; don't instrument the disabled `integrationTest` source set unless coverage is specifically wanted there | Most visible once JaCoCo + full matrix (even just `latest` × V2) runs in CI with limited runner resources |
| `countRows`/`optimizeTable` polling pattern with `select_sequential_consistency=1` in `ClickHouseTestHelpers` | New tests copying this helper for JSON/Dynamic assertions inherit its `OPTIMIZE TABLE` + sequential-consistency round trip per check, adding latency per assertion | Fine for correctness-focused tests at current data volumes (10k records); don't scale this pattern to much larger record counts without reconsidering per-assertion `OPTIMIZE` cost | Becomes a real CI time cost once new tests multiply this pattern across many new test methods |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Treating `targetTableFilter` as a trusted-input regex without testing quote/backslash escaping | A regex containing unescaped single quotes or backslashes could, in theory, break out of the SQL string literal if the doubling logic (`replace("'", "''")`) has an edge case (e.g. a filter ending in an odd number of backslashes before the closing quote) | Add a targeted test with a filter value containing quotes and backslashes together, verifying the generated query is well-formed and doesn't error or (worse) match unintended tables |
| Copying `KeeperStateProvider`'s unparameterized `String.format()` SQL pattern into new test helpers or fixes | Already flagged as low-severity in `CONCERNS.md`; explicitly out of scope for this project, but easy to inadvertently reuse the pattern when writing new helper code that formats identifiers into SQL | Don't introduce new `String.format()`-built SQL with untrusted input while adding tests; if a new helper needs dynamic identifiers, escape/backtick-quote consistently with existing patterns (e.g. `` `%s` ``) |
| Logging full `props`/config maps in new test failure output | `ClickHouseSinkConfig` config maps can contain passwords; `Mask.passwordMask()` exists but isn't applied everywhere per `CONCERNS.md` | New tests that print `props` on failure (common debugging habit) should mask credentials or use test containers with non-sensitive throwaway passwords only (already the case — `test_password`) and avoid logging full config maps in shared test infrastructure that could be reused with real credentials later |

## "Looks Done But Isn't" Checklist

- [ ] **`jsonInsertColumns` tests:** Often missing the *default* (empty-list) case — verify a test exists for `insertFormat=json` with `jsonInsertColumns` unset, not just the explicit-column-subset case.
- [ ] **`describeTable` subcolumn skip tests:** Often missing a diff against the *actual raw* `DESCRIBE ... describe_include_subcolumns` output on `latest` — verify the test asserts against real server output, not a hand-written fixture that may not match current ClickHouse internals.
- [ ] **`targetTableFilter` tests:** Often missing RE2-specific edge cases (backslashes, lookaround) — verify at least one test uses a non-trivial regex against the live `match()` query, not just a Java `Pattern` check.
- [ ] **JMX MBean tests:** Often missing the leak/collision case — verify a test proves that an exception between `start()` and `stop()` doesn't leave a dangling MBean (or that the suite doesn't rely on try/finally being present everywhere by accident).
- [ ] **`async_insert` "not forced" test:** Often missing end-to-end behavior under `async_insert=1` — verify a test doesn't just assert the absence of a forced `async_insert=0` setting, but also exercises an actual async insert and confirms exactly-once/at-least-once dedup semantics still hold (or is explicitly documented as untested/unsupported).
- [ ] **JaCoCo coverage report:** Often "configured" but never verified to produce nonzero, sensible numbers for the actual classes that matter — verify by inspecting the generated report for `Column.java`, `TableOperationFilter.java`, etc., not just checking the build succeeded.
- [ ] **CI JSON/Dynamic tests on the existing matrix:** Often missing `@SinceClickHouseVersion` gating — verify every new JSON/Dynamic/Dynamic-adjacent test is annotated so it doesn't break the existing 23.7/24.3 matrix legs in `tests.yaml`.
- [ ] **CI Actions-enabled check:** Often assumed true because the workflow file exists — verify via `gh api .../actions/permissions` or run history that Actions actually execute on this fork before treating "CI passes" as a real deliverable.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|----------------|------------------|
| `CLICKHOUSE_VERSION` default mismatch (Pitfall 1) | LOW | Change the default constant or add an explicit assertion/fail-fast; re-run affected tests against `latest` to confirm behavior didn't silently differ |
| Whole-table drop on unrecognized subcolumn (Pitfall 2) | MEDIUM | Add the missing subcolumn-suffix pattern to the skip-list (or fix `Column.extractColumn` to classify it), add a regression test with the exact `DESCRIBE` output that triggered it, upgrade the log level so future occurrences are loud |
| RE2 vs Java regex mismatch in `targetTableFilter` (Pitfall 3) | LOW–MEDIUM | Add live-container regex tests; if a real gap is found (e.g. backslash escaping), fix the SQL-literal construction in `showTables` and add a regression test with that exact pattern |
| Empty-list `jsonInsertColumns` misbehavior (Pitfall 4) | MEDIUM–HIGH | If client-v2 mishandles the empty list, either pass `null` instead of an empty list when unset, or explicitly build the column list from table metadata; requires a real fix plus regression test, not just documentation |
| MBean leak on test failure (Pitfall 5) | LOW | Wrap existing and new `start/put/stop` sequences in try/finally or `@AfterEach`; re-run the full suite (not just the affected test in isolation) to confirm no more order-dependent flakes |
| Dynamic RowBinary gap (Pitfall 6) | LOW (as explicit error) / HIGH (as full serialization) | Prefer the LOW-cost fix: explicit exception + test; escalate to full serialization only if product requirements later demand Dynamic support outside the JSON insert path (separate project) |
| `windows-1252` charset mismatches in new assertions (Pitfall 7) | LOW | Add explicit `StandardCharsets.UTF_8` to affected `InputStreamReader`/`BufferedReader` construction sites; re-run the specific new non-ASCII test to confirm |
| CI never actually running on the fork (Pitfall 10) | LOW–MEDIUM | Enable Actions in fork settings, push a trivial commit, confirm a run appears and succeeds, then proceed with JaCoCo/coverage-reporting additions |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|-------------------|---------------|
| `CLICKHOUSE_VERSION` defaults to 25.3, not `latest` (P1) | Baseline/infra phase | New/existing tests explicitly set and assert `CLICKHOUSE_VERSION=latest`; CI job env is hard-coded, not relying on the default |
| Whole-table drop on unrecognized subcolumn (P2) | Table metadata & types phase | Test diffs real `DESCRIBE ... describe_include_subcolumns` output on `latest` against the skip-list; asserts no table silently vanishes from mapping |
| RE2 vs Java regex gap in `targetTableFilter` (P3) | Table metadata & types phase | Regex tests run against the live container's `match()`, including a backslash-containing pattern |
| Empty-list `jsonInsertColumns` behavior unverified (P4) | Explicit JSON insert path phase | Test with `jsonInsertColumns` unset (default) asserts both success and correct per-column values, not just row count |
| MBean collision / leak on test failure (P5) | Connector plumbing phase (MBean tests), enforced suite-wide | try/finally (or `@AfterEach`) around every `start/put/stop` in new and touched test code; full-suite run (not isolated test run) is green |
| `Type.DYNAMIC` RowBinary gap (P6) | Table metadata & types phase, feeding "fix bugs found by tests" gate | Test proves the gap; fix is an explicit clear error, not new serialization logic — code review confirms no `ClickHouseWriter` refactor scope creep |
| `windows-1252` charset mismatches (P7) | Table metadata & types phase + transforms phase | Non-ASCII content test passes under the actual `./gradlew test` JVM settings (not just IDE default charset) |
| `skipped.operations` silent invalid-value handling / `r` op (P8) | Transforms phase | Test explicitly pins current warn-and-ignore behavior for invalid ops and confirms `r` is not filterable, or a `ConfigException` fix is made and tested |
| JaCoCo Lombok-generated code dilution (P9) | Coverage + CI phase | Generated report manually inspected for `Column.java`/config builder classes to confirm exclusion is actually working |
| Deprecated Actions action / fork Actions possibly disabled (P10) | CI phase | `gh api .../actions/permissions` (or Settings UI) confirms Actions enabled and a run exists before new workflow changes are trusted |

## Sources

- Fork source, read directly (HIGH confidence, primary grounding for all repo-specific pitfalls): `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `.../sink/db/mapping/Column.java`, `.../util/jmx/MBeanServerUtils.java`, `.../sink/ProxySinkTask.java`, `.../sink/ClickHouseSinkConfig.java`, `.../sink/db/ClickHouseWriter.java`, `.../transforms/TableOperationFilter.java`, `src/test/java/.../sink/JsonColumnTest.java`, `.../sink/helper/ClickHouseTestHelpers.java`, `build.gradle.kts`, `.github/workflows/tests.yaml`
- `.planning/codebase/CONCERNS.md`, `.planning/codebase/TESTING.md`, `.planning/PROJECT.md` (project-specific known risks and constraints)
- [ClickHouse JSON Data Type docs](https://clickhouse.com/docs/reference/data-types/newjson) — JSON type GA in 25.3, `allow_experimental_json_type` history, legacy `Object('json')` removed in 25.11 (MEDIUM-HIGH; cross-referenced against blog/PR summaries, not the changelog directly)
- [ClickHouse `Dynamic` type / `allow_experimental_dynamic_type` discussion, PR #117746 (settings-naming convention)](https://github.com/ClickHouse/ClickHouse/pull/117746) (MEDIUM — setting-name conventions evolving; verify exact current flag name against the specific `latest` build in CI)
- [ClickHouse string-search / `match()` functions docs](https://clickhouse.com/docs/sql-reference/functions/string-search-functions) — RE2 basis, `.` matches newlines by default unlike RE2 default (MEDIUM-HIGH)
- [Testcontainers "General Container runtime requirements"](https://java.testcontainers.org/supported_docker_environment/) and community colima setup guides (MEDIUM — community-sourced env var conventions, not official Testcontainers colima documentation)
- [gradle/actions deprecation-upgrade-guide.md](https://github.com/gradle/actions/blob/main/docs/deprecation-upgrade-guide.md) — `gradle-build-action` → `gradle/actions/setup-gradle` migration (HIGH — official source)
- [GitHub Community Discussions on forked-repo Actions being disabled by default](https://github.com/orgs/community/discussions/29029) (MEDIUM — community discussion, consistent with documented GitHub behavior, but not the primary GitHub Docs page)
- JaCoCo/Java 21 "Unsupported class file major version" reports (jacoco/jacoco#1858, Gradle forums) — resolved in JaCoCo ≥0.8.9/0.8.11 depending on target bytecode version (MEDIUM — community-reported, version specifics should be re-checked against whatever JaCoCo Gradle plugin version is actually selected)

---
*Pitfalls research for: ClickHouse Kafka Connect fork — test coverage, JaCoCo, CI*
*Researched: 2026-09-15*
