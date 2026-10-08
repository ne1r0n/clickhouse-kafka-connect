# Requirements: v1.7.0_gg1-sync — Upstream v1.7.0 Sync & Follow-up Hardening

**Defined:** 2026-10-07
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Milestone goal:** Merge upstream `v1.7.0` into `custom_build`, close all 17 open follow-ups (including R-01's silent data loss), and publish `v1.7.0_gg1` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Locked decisions this milestone is built on** (settled during questioning — plans do not re-open these):

| Decision | Choice |
|---|---|
| Sync strategy | `git merge v1.7.0` in the `v1.6.0_gg1-sync` shape: scratch branch, rollback tag, one mechanical merge commit, re-apply by intent, non-force `--no-ff` into `custom_build` after human approval |
| Upstream ref | Tag `v1.7.0` (`379b8d01`). Never bare `upstream/main` — a stale `refs/upstream/main` makes it ambiguous |
| Conflict policy | Prefer upstream wherever its implementation passes the fork's tests (unchanged from `v1.6.0_gg1-sync`) |
| D-17 `lz4` pin | Retired in favour of upstream's `setCompressionMethod` (incl. its unconditional `compress=false`). Client stays `0.9.6`; `0.10.0` is out |
| R-01 | Fixed by **routing** Struct records to the JSON writer in metadata-free mode — not by a fail-fast throw. Wrong logical-type values on that route are **fixed**, not just documented |
| CR-02 pin tests | May be **re-pinned to correct bytes** where they assert the pre-#820 early-null behavior: nullable case kept, non-nullable case takes #820's empty-map semantics. Recorded as a user-approved tightening, not a weakening (2026-10-07) |
| Dependency bumps | User-approved 2026-10-07: jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0, and lz4-java **beyond** upstream's 1.10.4 to an advisory-free release (≥ 1.11.2, target 1.12.0) |
| Publication | In scope: push, tags, green CI, publish the post-`c61d90ce` `.planning/` history, operator rollout checklist |
| Ordering | Sync lands first; hardening goes on top of the merged tree |

---

## v1 Requirements

### Upstream Sync (SYNC)

- [x] **SYNC-07**: Upstream `v1.7.0` lands on a scratch branch as one mechanical merge commit, with a rollback tag `pre-v1.7.0-sync` on the pre-sync `custom_build` SHA. The merge-invariant script is parameterised (`PRE_TAG`, `UPSTREAM_REF`); the old "conflicted paths byte-identical to upstream" invariant is replaced by a line-provenance check plus a grep that every `docs/FORK-DELTA.md` anchor construct survives
- [x] **SYNC-08**: A v1.7.0 merge checklist records every path upstream changed, its predicted and actual conflict class, and a reviewed verdict for the **auto-merged** hunks inside fork-touched files (`ClickHouseWriter` MAP branch, `build.gradle.kts` LZ4 rule, `ClickHouseClientBuilder` setter sites, test helpers)
- [x] **SYNC-09**: Every fork feature in `docs/FORK-DELTA.md` survives the merge — `Type.DYNAMIC` fail-fast, JMX `connector=<name>`, `targetTableFilter`, describe sub-column skip, metadata-free JSON path, Kafka Connect API `4.1.1`, `CLICKHOUSE_VERSION_DEFAULT = latest` — each proven by its existing pin test passing
- [x] **SYNC-10**: Upstream's new and changed tests from `v1.7.0` (`nullMapTypeTest`, `testSetCompressionMethod_*`, #800 Avro-union, #801 buffer, cluster `buildFromClause`) are accepted **unmodified** and pass on `latest` × V2, or skip for a named reason recorded in `FORK-DELTA.md`

### Compression (CMP)

- [x] **CMP-01**: The fork's D-17 constants and pin lines are gone; upstream's `setCompressionMethod` (default `true`) is the single mechanism, and every client the connector builds — writer V1/V2, the writer's temporary client, `KeeperStateProvider` — receives the flag
- [x] **CMP-02**: Docker-free tests pin `compress=false` / `compressServerResponse(false)` and the `network_compression_method=lz4` setting on V1 options, the V1 node and the V2 config; with `setCompressionMethod=false` the lz4 setting is absent and response compression is still off
- [x] **CMP-03**: A live test on `latest` inserts and reads back successfully with `setCompressionMethod=true` and with `=false`; which control is load-bearing against the ZSTD default (`compress=false` vs `lz4`) is recorded in `FORK-DELTA.md` from that observation

### Writer Null-Correctness (WRT)

- [x] **WRT-06**: A null optional `Map` written into a non-nullable `Map` column produces upstream #820's bytes (empty map, `00` / `00 00` with defaults); the fork's MAP null guard is scoped to nullable columns, upstream's `nullMapTypeTest` passes unmodified, and a Docker-free byte-level test pins it
- [ ] **WRT-07**: A `null` entry inside a `Map(K, Nullable(V))` column round-trips as `NULL` end-to-end, asserted by typed SQL read-back (WR-01)
- [ ] **WRT-08**: A `null` landing in a **nested** non-nullable element slot (Array element, Map value, Tuple field) throws a `DataException` naming the column, instead of silently desyncing the RowBinary stream; top-level non-nullable Map/Array nulls keep upstream's coercion (WR-02)
- [x] **WRT-09**: Fork CR-02 pin tests that asserted the pre-#820 early-null bytes are re-pinned to the correct bytes — nullable case unchanged, non-nullable case per #820 — with the rationale recorded next to each changed assertion

> **Narrowing (Phase 13, 2026-10-08, D-09).** A nested null `Array` or `Map` in a non-nullable slot is coerced to an empty container, mirroring #820's top-level rule, so WRT-08's `DataException` covers nested non-nullable scalar and `Tuple` slots only. A `Tuple` slot follows `Column`'s reported nullability (D-16), so `Array(Nullable(Tuple(a Int32)))` null elements now throw. Through `ClickHouseSinkTask.put` the `DataException` is the root cause of a `RuntimeException`, because `Processing.doInsert` wraps every failure. The requirement text above is left as originally written.

### Metadata-free JSON Path (JSONP)

- [ ] **JSONP-05**: With `insertFormat=json`, a `Struct` record and `bypassRowBinary` at its default `false`, rows land through the JSON writer (routing on `shouldSkipTableMetadata()`, never on the raw format string). The R-01 pin test flips from "0 rows, no throw" to typed read-back of every record (R-01)
- [ ] **JSONP-06**: On that route, Kafka logical types — `Timestamp`, `Date`, `Time`, `Decimal`, `bytes` — land in matching ClickHouse columns (`DateTime`, `DateTime64`, `Date`/`Date32`, `Decimal`, `String`) with correct values, proven by a typed read-back matrix; conversion is fixed wherever Jackson's defaults would store a wrong value
- [ ] **JSONP-07**: Null-field semantics on that route are defined and pinned (Nullable column → `NULL`, non-nullable → column `DEFAULT`), as is the interaction with `jsonInsertColumns` and `bypassFieldCleanup`
- [ ] **JSONP-08**: `cleanupExtraFields`' guard evaluates `shouldSkipTableMetadata()` before dereferencing `csc`, so the documented null-safety holds at every call site (P9-IN-01)

> **Correction (Phase 13, 2026-10-08, D-13).** The operand swap makes metadata-free mode skip the `isBypassFieldCleanup()` read. It does not deliver null-safety: a null `csc` still throws `NullPointerException`, and no null-config short-circuit is added. FORK-DELTA's P9-IN-01 wording is corrected in Phase 15. The requirement text above is left as originally written.

### Exactly-once State (EOS)

- [x] **EOS-01**: Upstream's `KeeperStateProvider` state-read change (counting streamed rows instead of the response summary) is covered by a direct restart-read test — state written, new provider instance, same min/max/state read back — that is shown to fail against the old summary-count code

### Metadata, Types & Config (DESC / TYPE / COLS / CFG)

- [ ] **DESC-05**: `shouldSkipDescribeRow` skips DESCRIBE rows at any depth below a `JSON`/`Dynamic` column, not only the immediate child (P3-WR-01)
- [ ] **TYPE-08**: `Int256`/`UInt256` columns fail fast through `validateDataSchema` with the actionable message, while `Int128`/`UInt128` keep being written (P3-WR-03, narrowed)
- [ ] **COLS-07**: `Column.extractColumn`'s Array recursion passes `hasDefaultValue` and `isSubColumn` in the right order, pinned by a test
- [ ] **CFG-04**: `ClickHouseSinkConfig`'s `ConfigDef` reports `"default"` as the `USERNAME` default

### Test Debt (DEBT)

- [ ] **DEBT-01**: `HeaderToValue`'s behavior when `fields` names a pre-existing non-STRING column is pinned by a test (WR-03)
- [ ] **DEBT-02**: `JsonColumnTest` reads the payload back and asserts values, not row counts
- [ ] **DEBT-03**: `ClickHouseSinkTaskJsonInsertFormatTest`'s two tests use the `try/finally` task-lifecycle pattern (P4-WR-01)
- [ ] **DEBT-04**: A database-drop failure in `ClickHouseBase.tearDown()` is surfaced so it is attributable to the class that caused it, instead of being logged and swallowed (Phase 10 WR-01)
- [ ] **DEBT-05**: Stale Javadocs are corrected — `ClickHouseVersionDefaultUnitTest` (Phase 11 IN-01), `AsyncInsertLiveTest` (P4-IN-01), and the `NetworkCompressionPin*` rationale after CMP-01

### Build & Dependencies (BLD)

- [x] **BLD-09**: The fork's LZ4 capability rule is removed in favour of upstream's; `dependencyInsight` shows exactly one `at.yawk.lz4:lz4-java` on `runtimeClasspath`; the stale "1.10.2" wording is fixed in `FORK-DELTA.md`, `MERGE-CHECKLIST.md` and `.claude/CLAUDE.md`
- [ ] **BLD-10**: `lz4-java` is raised past upstream's `1.10.4` to an advisory-free release (≥ 1.11.2, target 1.12.0), after showing client `0.9.6` works with it (compile, `testUnit`, live LZ4 round-trip)
- [ ] **BLD-11**: Jackson is `2.22.3` (runtime, ships in the Confluent archive), in its own commit, with the JSON-route tests green
- [ ] **BLD-12**: okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0` are applied as version edits (not dependabot cherry-picks), one commit each, each gated by `compileIntegrationTestJava` and Docker-free `testUnit`
- [x] **BLD-13**: The catalog drops `clickhouse-jdbc` and the obsolete `# FORK OVERRIDE (upstream 0.9.5)` comment while keeping `kafka = "4.1.1"`

### CI (CI)

- [ ] **CI-08**: A published SHA gets exactly one completed `tests.yaml` run — the tag/branch trigger and concurrency group are fixed **before** any tag is pushed
- [ ] **CI-09**: CI's test fork count is read from a job log or summary (e.g. `nproc` / `maxParallelForks`), or explicitly recorded as unmeasured
- [ ] **CI-10**: CI compiles the integration-test source set (`compileIntegrationTestJava`), so integration-only dependencies like okhttp are checked on every push

### Suite Gate (SUITE)

- [ ] **SUITE-04**: The full suite on ClickHouse `latest` × client V2 passes twice (second run with `--rerun`), with 0 failures, 0 errors and a skip set equal to the named skips in `FORK-DELTA.md`; `testUnit` passes with Docker stopped

### Publication (PUB)

- [ ] **PUB-01**: `VERSION` is `v1.7.0_gg1` (built after `clean`, so `Version.java` is not stale) and `CHANGELOG.md` has a `1.7.0_gg1` section listing the fork's own changes — R-01 behavior change, WR-02 strictness, compression, lz4
- [ ] **PUB-02**: `docs/FORK-DELTA.md` reflects the new state — D-17 retired, R-01 removed from known costs, LZ4 rule and version, MAP guard scoping, any new kept patches or costs
- [ ] **PUB-03**: `custom_build` receives a non-force `--no-ff` merge after human approval, tags `v1.7.0_gg1` and `pre-v1.7.0-sync` are on `origin`, and CI is green on the published SHA
- [ ] **PUB-04**: The `.planning/` history after `c61d90ce` is published on `origin` via merge (not fast-forward), without `.planning/.gsd-ws-arg` or local-only tags

### Rollout (ROLL)

- [ ] **ROLL-01**: An operator rollout checklist exists covering: JMX MBean name before/after (`connector=` and the per-release `version=` segment); compression semantics, the `compress=true`-in-`jdbcConnectionProperties` caveat and the server-profile check; R-01's behavior change and the exactly-once backfill procedure (offset reset + KeeperMap state clear); JSON-route type notes; the new `DataException` (DLQ-able) for nested nulls; the rollback tag

## Future Requirements

Deferred — tracked, not in this roadmap.

- **ClickHouse client `0.10.0`** — upstream dependabot branch, unmerged. Revisit once upstream adopts it; it may make the lz4 setting unnecessary
- **Production rollout execution** — this milestone delivers the checklist, not the rollout itself

## Out of Scope

| Feature | Reason |
|---------|--------|
| Adopting upstream's Avro record-union (#800) or `DebeziumRecordConvertor` paths | New capability, not fork behavior; merged as upstream code, not adopted (as in `v1.6.0_gg1-sync`) |
| Changing the metadata-free JSON path's KEEP decision | Settled in Phase 9 (D-01); R-01 is fixed inside it, not by dropping it |
| Client V1 / older ClickHouse test matrix, ClickHouse Cloud | Unchanged project constraint — production is `latest` × V2 |
| Upstream hardening of code the fork never touched (SQL interpolation in `KeeperStateProvider`, reload4j) | Not fork additions |

## Traceability

Which phases cover which requirements. Filled during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SYNC-07 | Phase 12 | Complete |
| SYNC-08 | Phase 12 | Complete |
| SYNC-09 | Phase 12 | Complete |
| SYNC-10 | Phase 12 | Complete |
| CMP-01 | Phase 12 | Complete |
| CMP-02 | Phase 12 | Complete |
| CMP-03 | Phase 12 | Complete |
| WRT-06 | Phase 12 | Complete |
| WRT-07 | Phase 13 | Pending |
| WRT-08 | Phase 13 | Pending |
| WRT-09 | Phase 12 | Complete |
| JSONP-05 | Phase 13 | Pending |
| JSONP-06 | Phase 13 | Pending |
| JSONP-07 | Phase 13 | Pending |
| JSONP-08 | Phase 13 | Pending |
| EOS-01 | Phase 12 | Complete |
| DESC-05 | Phase 13 | Pending |
| TYPE-08 | Phase 13 | Pending |
| COLS-07 | Phase 13 | Pending |
| CFG-04 | Phase 13 | Pending |
| DEBT-01 | Phase 13 | Pending |
| DEBT-02 | Phase 13 | Pending |
| DEBT-03 | Phase 13 | Pending |
| DEBT-04 | Phase 13 | Pending |
| DEBT-05 | Phase 13 | Pending |
| BLD-09 | Phase 12 | Complete |
| BLD-10 | Phase 14 | Pending |
| BLD-11 | Phase 14 | Pending |
| BLD-12 | Phase 14 | Pending |
| BLD-13 | Phase 12 | Complete |
| CI-08 | Phase 14 | Pending |
| CI-09 | Phase 14 | Pending |
| CI-10 | Phase 14 | Pending |
| SUITE-04 | Phase 15 | Pending |
| PUB-01 | Phase 15 | Pending |
| PUB-02 | Phase 15 | Pending |
| PUB-03 | Phase 15 | Pending |
| PUB-04 | Phase 15 | Pending |
| ROLL-01 | Phase 15 | Pending |

**Coverage:**
- v1 requirements: 39 total
- Mapped to phases: 39
- Unmapped: 0 ✓
- By phase: Phase 12 → 12, Phase 13 → 15, Phase 14 → 6, Phase 15 → 6

---
*Requirements defined: 2026-10-07*
*Last updated: 2026-10-07 after roadmap creation (Phases 12-15, traceability filled)*
