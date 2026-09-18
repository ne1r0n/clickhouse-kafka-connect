# Project Research Summary

**Project:** ClickHouse Kafka Connect `custom_build` Fork — Test Coverage
**Domain:** Test coverage for a production CDC sink connector (Java, Gradle, Testcontainers)
**Researched:** 2026-09-15
**Confidence:** HIGH (stack verified against official sources; features/architecture/pitfalls grounded directly in fork source code)

## Executive Summary

This is a test-coverage audit and implementation project for a production ClickHouse Kafka Connect fork (`custom_build` branch of `ne1r0n/clickhouse-kafka-connect`) that carries 11 significant additions on top of the upstream connector: 4 custom SMTs (transforms), table regex filtering, JSON/Dynamic column type support, an explicit metadata-free JSON insert path, column-subset inserts, custom JMX naming, and intentional async-insert non-forcing. The fork runs in production pipelines, but its additions are not yet covered by automated tests — a critical gap for confidence during rebases and maintenance.

The recommended approach is to add comprehensive unit and integration tests using the existing JUnit Jupiter + Testcontainers infrastructure, organized into 4 parallel test tracks (transforms, table metadata & types, JSON insert path, connector plumbing), backed by a modernized CI job (single-job, ClickHouse `latest` + client V2 only) running JaCoCo coverage reports. The research has identified 10 critical pitfalls (e.g., CLICKHOUSE_VERSION silently defaulting to 25.3 instead of `latest`, JSON/Dynamic column RowBinary serialization gap, MBean collision leak risk on test failures), and the suggested phase structure prioritizes infrastructure setup first (Docker baseline, container-singleton pattern, JaCoCo wiring, CI modernization) before expanding test coverage in parallel to reduce suite wall-clock time and catch integration issues early.

## Key Findings

### Recommended Stack

**Technology additions (no major new dependencies):**

- **JaCoCo Gradle plugin (`0.8.15`)** — Line/branch coverage instrumentation + HTML/XML reports. It's a core Gradle plugin bundled with Gradle 8.14 (already pinned); override default `toolVersion` from 0.8.13 to 0.8.15. Configure explicitly to produce both XML (CI tooling) and HTML (human review).

- **GitHub Actions updates** — Modernize from deprecated actions to current: `gradle/actions/setup-gradle@v6` (supersedes `gradle-build-action@v2`), `actions/setup-java@v6` with `distribution: temurin` (supersedes `v3` with `adopt`), `actions/checkout@v4`.

- **No new test libraries** — Existing JUnit Jupiter, Mockito, Hamcrest, Testcontainers 1.21.4 are sufficient; reuse `ClickHouseBase` and `ClickHouseTestHelpers`.

**Keep as-is:** Gradle 8.14 wrapper, Testcontainers 1.21.4 (2.x GA but zero functional benefit for this scope).

### Expected Features & Test Gaps

The fork's 11 additions require **80+ specific test cases**:

**Must-have (table stakes):**
- Transform SMTs: `TableOperationFilter` (truncate op, multi-table, invalid values); `HeadersToValue` (null headers, collision); `HeaderToValue` (multi-valued, missing); `ConvertToMapOrJson` (null preservation, nested arrays/maps)
- Table metadata: `targetTableFilter` (empty filter, quote escaping); `describeTable` subcolumns (Nullable/.null, Array/.size0, JSON/.object_shared_data)
- JSON/Dynamic: Type mapping + real-insert content assertions (current tests only check row counts)
- Explicit JSON path: `insertFormat=json` bypass verification + `jsonInsertColumns` default behavior
- Connector plumbing: MBean naming (present/absent/null version); `async_insert` non-forcing (negative assertion + override)
- Config validation: ConfigException/DataException tests for all 4 SMTs (currently zero exist)

**Test inventory status:** 29 existing tests (mostly SMT happy-path); **80+ gaps** concentrated in jsonInsertColumns (0 tests), JSON/Dynamic content (0), describeTable subcolumns (0), MBean (0), async_insert (0).

### Architecture Approach

Test organization mirrors existing pattern: unit tests (fast, no Docker) and ClickHouse-backed tests (extending `ClickHouseBase`, Testcontainers) in same `src/test/java`. Four key patterns:

1. **Container-per-fork singleton** — Guard shared `db` static field with `if (db == null)` check; saves 10–20s per new test class as suite grows.
2. **JUnit 5 tags for unit-only runs** — `@Tag("clickhouse")` on CH-backed classes; new `testUnit` task excludes them for fast local iteration without Docker.
3. **Minimal testability seams** — Bump `ProxySinkTask.getMBeanName()`, `ClickHouseWriter.shouldSkipTableMetadata()` to package-private; extract `ClickHouseHelperClient.buildTableFilterClause()` as static utility. Applied in same phase as tests.
4. **JaCoCo to `test` only** — Don't hook to `check` (pulls out-of-scope `integrationTest`). Use `test.finalizedBy(jacocoTestReport)` + CI calls `./gradlew test jacocoTestReport` directly.

### Critical Pitfalls

1. **`CLICKHOUSE_VERSION` defaults to 25.3, not `latest`** — Hides JSON/Dynamic behavior differences. **Prevention:** Hard-code `CLICKHOUSE_VERSION: latest` in CI; add fail-fast assertion.

2. **Whole-table drop on unrecognized subcolumn** — `describeTable` returns `null` for entire table if one column's subcolumn is unrecognized. **Prevention:** Test against real `latest` output; upgrade log to ERROR.

3. **RE2 vs Java regex mismatch** — `targetTableFilter` compiles in Java but fails in ClickHouse's `match()` with backslashes. **Prevention:** Test regex against live container, including backslash patterns.

4. **`jsonInsertColumns` empty-list default untested** — Unknown if client-V2 treats empty list like "no columns" or generates malformed INSERT. **Prevention:** Test default case + verify column values, not just counts.

5. **MBean collision leak on test failure** — Missing `stop()` leaves MBean in registry; next test with same name crashes. **Prevention:** try/finally + unique connector name per test.

6. **`Type.DYNAMIC` has no RowBinary serializer** — Dynamic column via default insert path silently drops data. **Prevention:** Test first; if crash/silent-drop, fix to explicit error or document.

7. **`windows-1252` charset mismatch** — Tests run with non-UTF-8 default; JSON/Dynamic assertions fail on non-ASCII. **Prevention:** Explicit `StandardCharsets.UTF_8` for stream readers; test non-ASCII round-trip.

8. **`skipped.operations` silently ignores invalid values** — No ConfigException for typos. **Prevention:** Test pins current warn-and-ignore or adds validation.

9. **JaCoCo Lombok-generated code dilution** — Builder/getter boilerplate inflates coverage %. **Prevention:** Manually verify generated report for `Column.java`, config classes.

10. **Deprecated CI actions / fork Actions possibly disabled** — Workflow exists but may never run on fork. **Prevention:** Verify Actions enabled via `gh api`; confirm run history.

## Implications for Roadmap

**Suggested 5-phase structure:**

### Phase 1: Baseline & Infrastructure (gates all others)
**Rationale:** Establishes known-green suite, working CI, performance foundations.
**Delivers:**
- Confirm `./gradlew test` green against ClickHouse `latest` × client V2
- Add JaCoCo plugin, wire to `test` task; verify sample report
- Apply `ClickHouseBase` container-singleton guard (essential for Phase 2 performance)
- Modernize CI: `setup-gradle@v6`, `setup-java@v6`, single-job `latest`×V2 only
- Hard-code `CLICKHOUSE_VERSION: latest` in CI job `env:`
- Verify fork Actions enabled via `gh api` (Pitfall 10)
**Avoids:** Pitfalls 1, 10; establishes try/finally pattern early (Pitfall 5)

### Phase 2A: Transform/SMT Unit Tests (parallelizable with 2B/2C/2D)
**Rationale:** Pure unit tests, no Docker, fastest feedback.
**Delivers:** 26 new unit test cases across 4 SMTs (TableOperationFilter 7, HeadersToValue 6, HeaderToValue 5, ConvertToMapOrJson 8)
**Features:** Truncate op, multi-table, invalid/empty values, null handling, collision, config parsing, null preservation, nested arrays
**Avoids:** Pitfall 8 (config validation silent failures)

### Phase 2B: Table Metadata & Types (parallelizable, shares container with 2C)
**Rationale:** Largest new CH-backed cluster; describeTable and JSON/Dynamic tests share fixtures.
**Delivers:**
- `targetTableFilter` string-level + live-container tests; quote/backslash escaping (4 new)
- `describeTable` subcolumns: Nullable/.null, Array/.size0, JSON/.object_shared_data (3 new)
- `JSON(...)`/`Dynamic` type mapping + real-insert content (4 new CH-backed)
**Uses:** Container-singleton pattern; version-gating extension
**Avoids:** Pitfalls 2, 3, 6
**Research flags:** Pre-phase spike on ClickHouse `latest` DESCRIBE output for complete subcolumn-suffix list; Dynamic RowBinary ground-truth test outcome determines Phase 3 fix

### Phase 2C: Explicit JSON Insert Path & `jsonInsertColumns` (parallelizable)
**Rationale:** These features share the same `doInsertJson` code path.
**Delivers:**
- `insertFormat=json` bypass: startup without table, `getTable` bypass, `cleanupExtraFields` bypass (2 new)
- `jsonInsertColumns` parsing: empty, whitespace (1 new unit)
- `jsonInsertColumns` insert: empty-default, column-subset, defaults, nonexistent column (3 new)
**Implements:** Shares `ClickHouseBase` from Phase 1; uses Phase 2B fixtures
**Avoids:** Pitfall 4 (empty-list default untested)
**Research flags:** Client-V2 0.9.6 behavior with empty column list

### Phase 2D: Connector Plumbing (parallelizable, smallest)
**Rationale:** MBean and async_insert are independent; pure unit or lightweight CH-backed.
**Delivers:**
- MBean `connector=`: name present/absent, null version, multi-task collision (3 unit)
- `async_insert` not forced: default unset, user override honored (2 unit + 1 CH-backed)
**Implements:** `ProxySinkTask.getMBeanName()` visibility seam
**Avoids:** Pitfall 5 (MBean collision) — establishes try/finally enforcement

### Phase 3: Bug-Fix-in-Place (interleaved with Phase 2A–2D)
**Rationale:** Fix bugs found by tests immediately, same phase.
**Delivers:** Targeted bug fixes scoped to the feature being tested (no large refactors per PROJECT.md Out-of-Scope)

### Phase 4: Final Verification (depends on all Phase 2)
**Rationale:** Cross-track stability and coverage quality.
**Delivers:**
- Full `./gradlew test` green across all new tests
- JaCoCo report manual inspection for `Column.java`, SMT coverage quality (Pitfall 9 prevention)
- CI end-to-end green; coverage artifact published

### Phase Ordering Rationale

- **Phase 1 mandatory first:** Every later phase depends on baseline, performance foundations, CI trust.
- **Phases 2A–2D in parallel:** Independent by component touched; 2A has zero Docker overhead; 2B/2C share container.
- **Phase 3 interleaved:** Fixes stay with tests (no "known failing" commits).
- **Phase 4 last:** Stability verification after integration.

### Research Flags

Phases needing deeper research:
- **Phase 1:** Verify fork Actions enabled (Pitfall 10 spike)
- **Phase 2B:** Spike on `DESCRIBE TABLE ... describe_include_subcolumns=1` against `latest` (Pitfall 2 prevention); Dynamic RowBinary ground-truth
- **Phase 2C:** Confirm client-V2 0.9.6 empty-column-list behavior (Pitfall 4 spike)

Phases with standard patterns (skip research-phase):
- **Phase 2A:** SMT unit testing follows Apache Kafka Connect convention (established)
- **Phase 2D:** JMX/connector-config naming is standard pattern (established)

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| **Stack** | HIGH | Verified against GitHub Releases API, Gradle docs, official Actions migration |
| **Features** | HIGH | Read directly from fork source (commit 22f055c), test inventory by grep |
| **Architecture** | HIGH | Grounded in fork `build.gradle.kts`, current tests, official Testcontainers/Gradle docs |
| **Pitfalls** | HIGH (repo-specific) / MEDIUM (ecosystem) | Source code lines cited; ecosystem patterns cross-referenced |

**Overall:** HIGH — Roadmap can proceed with confidence. Identified spikes are pre-planning (Phase 1) or phase-entry validations (Phases 2B/2C), not blocker unknowns.

### Gaps to Address

- `CLICKHOUSE_VERSION` handling at init: confirm default + CI override during Phase 1
- ClickHouse `latest` subcolumn naming evolution: Phase 2B DESCRIBE audit is mandatory pre-execution
- Client-V2 empty-column-list behavior: Phase 2C integration test outcome determines design
- Dynamic RowBinary gap resolution: Phase 2B test result feeds Phase 3 bug fix or documentation
- JMX leak frequency in practice: enforce try/finally suite-wide during Phase 2; raise severity if flakes occur

## Sources

**Primary (HIGH confidence):**
- Fork source code: `src/main/java/com/clickhouse/kafka/connect/`, `src/test/java/` (commit 22f055c)
- Gradle 8.14 JacocoPlugin.java (GitHub, tag v8.14.0) — confirmed DEFAULT_JACOCO_VERSION
- GitHub Releases API — verified JaCoCo 0.8.15, Actions versions, Testcontainers 2.0.5
- Gradle Actions official migration guide — gradle-build-action deprecation
- PROJECT.md and CONCERNS.md — constraints, known risks

**Secondary (MEDIUM confidence):**
- Docker/Testcontainers official guidance — container patterns
- Apache Kafka Connect SMT testing conventions
- ClickHouse docs — JSON/Dynamic type history
- GitHub community discussions — fork-disabled-by-default, Testcontainers performance

**Tertiary (LOW confidence, needs validation):**
- Testcontainers colima environment-variable patterns (community-sourced)
- ClickHouse RE2 string-literal escaping edge cases (inferred from code)

---

*Research completed: 2026-09-15*
*Synthesized by: gsd-research-synthesizer*
*Ready for roadmap: yes*
