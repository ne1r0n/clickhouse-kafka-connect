# ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## What This Is

The `custom_build` branch of `ne1r0n/clickhouse-kafka-connect` (version `v1.7.0_gg1`) is a fork of the official ClickHouse Kafka Connect sink, carrying custom SMTs, table filtering, `Dynamic` column support, a metadata-free JSON insert path, and JMX naming changes used in CDC (Debezium) pipelines into ClickHouse. The first milestone (`v1.2.9_gg4-tests`) audited the existing tests for everything the fork added on top of upstream, wrote the missing unit and integration tests, and fixed the bugs those tests uncovered — delivering a fully green suite, a coverage report, and CI running it on every push.

The second milestone (`v1.6.0_gg1-sync`) cashed that in: 581 commits of upstream (`v1.2.9` → `v1.6.0`) merged into `custom_build` with the suite as the acceptance gate, every fork feature either re-applied onto upstream's rewritten code or consciously retired in favour of upstream, and the result published as `v1.6.0_gg1`. What the fork still patches, and what it gave up, is recorded in `docs/FORK-DELTA.md`.

The third milestone (`v1.7.0_gg1-sync`) repeated the sync for upstream `v1.7.0` and then paid down the fork's open follow-ups on the merged tree. R-01's silent data loss is fixed by routing, nested nulls in RowBinary either round-trip or fail loudly, and D-17 is retired onto upstream's `setCompressionMethod`. Dependencies were bumped one commit at a time and CI now runs once per published SHA. The result is published as `v1.7.0_gg1`, with an operator rollout checklist in `docs/ROLLOUT-v1.7.0_gg1.md`.

## Core Value

Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## Current State

**Shipped:** `v1.7.0_gg1-sync` (2026-10-10). It covered 4 phases (12–15), 25 plans, 55 tasks and 39/39 requirements. It closed as an override closeout for the same two reasons as the previous milestone. First, no milestone audit was run. Second, the Phase 12 and 13 verification reports read as stale because Phases 13–15 later edited the files they covered. Both verifications passed when run. Phases 14 and 15 verification passed on the final tree, and Phase 15 UAT was 1/1.

`custom_build` on `origin` is the two-parent `--no-ff` merge `f328fd21` (parents `c61d90ce`, `ae8520ed`), tagged `v1.7.0_gg1`. The rollback tag `pre-v1.7.0-sync` points at `c61d90ce`. Seventeen planning and docs commits after `f328fd21` live only on local `sync/v1.7.0`. They include `505e4981`, the corrected rollout doc, which is not yet published.

The suite runs 830 tests on ClickHouse `latest` (26.9.14.10) × client V2 and passed twice: 0 failures, 0 errors, and the same 18 named skips. `testUnit` runs 454 tests with Docker stopped. CI run `38050860929` on the published SHA is green in 389 s and is the only run on that SHA. Client `0.9.6` and Connect API `4.1.1` are unchanged. lz4-java is `1.12.0`, Jackson `2.22.3`, okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0`.

Code: `src/main` 10,119 LOC; tests 29,523 LOC (`src/test` 23,416, `src/testFixtures` 4,329, `src/integrationTest` 1,778). The fork delta against upstream grew from 84 to 95 files at the wide scope and from 30 to 31 in `src/main`. The growth is the hardening, tests and CI added on top of the merge, not unretired debt (`docs/FORK-DELTA.md`).

**Behaviour changes operators will see:**
- `Struct`/CDC records under `insertFormat=json` now land instead of being dropped, so exactly-once pipelines need a backfill.
- A nested null in a non-nullable scalar or Tuple slot now throws a `DataException`, which can be routed to the DLQ.
- `Int256`/`UInt256` columns fail fast.
- Compression follows upstream's flag.
- The MBean `version=` segment changes.

Each is covered in the rollout checklist. **Knowingly open:** `Nullable(Enum8|Enum16|Tuple)` columns are mis-mapped (backlog 999.1, warned at discovery). The empty-CDC skip covers only the explicit JSON route.

<details>
<summary>Previous state — <code>v1.6.0_gg1-sync</code> (shipped 2026-10-07)</summary>

**Shipped:** `v1.6.0_gg1-sync` (2026-10-07) — 6 phases (6–11), 24 plans, 63 tasks, 53/53 requirements. Closed as an override closeout: no milestone audit was run, and Phases 6–10 verification reports read as stale because later phases legitimately touched the files they covered (each passed when run; Phase 11 verification passed and UAT 5/5).

`custom_build` on `origin` is the two-parent `--no-ff` merge `c61d90ce` (parents `6ac2d20c`, `586af775`), tagged `v1.6.0_gg1`, with the rollback tag `pre-v1.6.0-sync` → `6ac2d20c`. `upstream` is a permanent named remote.

The suite runs 663 tests on ClickHouse `latest` (26.9.12.8) × client V2: 0 failures, 0 errors, 18 skips — every skip named in `docs/FORK-DELTA.md`. `testUnit` runs 331 tests with Docker stopped. CI run `37619434848` on the published SHA is green in 386 s and publishes coverage. The build is upstream's version catalog (Gradle 9.6.1, Testcontainers 2.0.5, JUnit 5.14.4) with the fork's JaCoCo, `testUnit`, JDK 17 pin, LZ4 resolution and `dependencyUpdates` re-applied; the fork keeps ClickHouse client `0.9.6` and Kafka Connect API `4.1.1`.

Code: `src/main` 9,622 LOC; tests 25,473 LOC across `src/test`, `src/testFixtures` and `src/integrationTest`.

**Two silently-vanishing behaviours caught by the gate rather than by production:** `CLICKHOUSE_VERSION_DEFAULT` reverted `latest` → `25.8.18.1` in the merge with no conflict marker, so Phases 6–9 tested the wrong server until Phase 10 found it; and ClickHouse 26.9's ZSTD default for `network_compression_method` broke client `0.9.6`, fixed before publication by forcing `lz4` (D-17).

**Fork features retired in favour of upstream:** the native `Type.JSON` path, the duplicate `Map(K, Nullable(V))` null-marker patch, and the client-V2 error-classification gap (closed by upstream's `Utils.handleException`). **Kept against the "prefer upstream" policy:** `Type.DYNAMIC` with its RowBinary fail-fast, JMX `connector=<name>`, `targetTableFilter`, the describe sub-column skip, and the metadata-free `insertFormat=json` path (KEEP, D-01) — R-01's silent data loss with it, knowingly.

</details>

<details>
<summary>Previous state — <code>v1.2.9_gg4-tests</code> (shipped 2026-09-18)</summary>

**Shipped:** `v1.2.9_gg4-tests` (2026-09-18) — 5 phases, 16 plans, 75/75 requirements.

The suite runs 371 tests against ClickHouse `latest` × client V2: 0 failures, 0 errors, 7 pre-existing skips. Verified green both locally (`./gradlew test`, colima) and in CI — run [`35317272594`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594) on `a295af8`, one job, both key steps `success`, both artifacts present. `custom_build` is pushed to the public fork.

Test code grew from the upstream baseline to 15,179 LOC across `src/test` and `src/integrationTest`; `src/main` is 6,850 LOC and was touched only by targeted bug fixes.

**Seven production bugs found and fixed**, each in the phase whose tests exposed it:

| Bug | Fix | Phase |
|---|---|---|
| `TableOperationFilter.configure()` unioned skipped ops across repeated calls | Clear before parse (D-03) | 2 |
| `HeaderToValue` served a stale cached `valueSchema` after a source-schema change | `sourceSchema` reference-comparison guard (D-02) | 2 |
| `HeadersToValue` — same stale-schema-cache bug | Same guard shape | 2 |
| `ConvertToMapOrJson.convertFieldValue()` did not recurse into List/Map values | Added both recursion branches (D-01) | 2 |
| `ClickHouseWriter` MAP branch omitted the RowBinary null marker, making `Map(K, Nullable(V))` columns unwritable | 3-line fix mirroring the ARRAY branch (approved out-of-scope) | 2 |
| `describeTable` readers used the platform default charset, mangling non-ASCII column names and `Enum` members under the `windows-1252` test JVM | Pinned both readers to UTF-8 (D-15) | 3 |
| `JSON`/`Dynamic` through RowBinary silently corrupted the stream | Fail-fast `DataException` naming `insertFormat=json` / `bypassRowBinary=true` (TYPE-06) | 3 |

**One bug found and deliberately not fixed:** R-01 — `insertFormat=json` + a `Struct` record + default `bypassRowBinary` writes zero rows and throws nothing. Escalated at a blocking-human checkpoint; you chose `pin-current-behavior`, because a fail-fast throw would break, loudly, any pipeline already running explicit JSON behind a Struct converter. A labelled test now asserts the loss, so a rebase cannot change it unnoticed. The fix needs its own plan.

</details>

## Current Milestone: `v1.7.0_gg2-hardening` Follow-up Hardening & CI Hygiene

**Goal:** Close all 11 follow-ups left open by `v1.7.0_gg1-sync`, getting the corrected rollout doc onto `origin` first, and publish `v1.7.0_gg2` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Target features:**

- **Early publication (first phase)** — docs-only `--no-ff` merge of `sync/v1.7.0` (17+ planning/docs commits, including `505e4981`) into `custom_build` and CI green on that SHA, before any code work. Removes the published `TRUNCATE TABLE` advice on the shared KeeperMap state table (15-REVIEW WR-01/WR-02/WR-03)
- **Writer correctness** — 999.1 `Column.extractColumn` keeps `Nullable` for `Enum8`/`Enum16`/`Tuple` (values round-trip, nested-null guards accept legal slots); WR-03 `doWriteCol` names the column in its exception; WR-04 null check before `getMessage().contains(code)`; 13-REVIEW WR-01 empty-CDC skip also on the `bypassRowBinary=true` JSON route
- **Test & build debt** — `Lz4RoundTripLiveTest` gains a standing response-leg assertion (14 WR-01); 999.2 drops the duplicate `org.lz4:lz4-java:1.8.0` from `testRuntimeClasspath`
- **CI hygiene (each change user-approved 2026-10-10)** — Node 24 majors for `actions/checkout`/`actions/upload-artifact`; runner pinned to `ubuntu-24.04` ahead of the 2026-10-19 `ubuntu-latest` → Ubuntu 26 switch; every action SHA-pinned plus a least-privilege `permissions:` block (T-14-16); the fork-count step reads Gradle's value instead of recomputing it in shell (14 WR-02); the Lombok "Ambiguous: Jackson2 and Jackson3" warning silenced
- **Upstream-identical code fixed in the fork, each patch recorded in `docs/FORK-DELTA.md`** — T-13-19: mask the raw DateTime value in upstream's two ERROR logs; 15-02 review IN-01..IN-10: each fixed or closed with a written reason
- **Release** — `VERSION=v1.7.0_gg2`, CHANGELOG, tag; rollout note for the 999.1 value change on `Nullable(Enum)`/`Nullable(Tuple)` columns; second `--no-ff` merge into `custom_build` with CI green on the published SHA

**Baseline at milestone start (2026-10-10):** `origin/custom_build` = `f328fd21` (`v1.7.0_gg1`); local `sync/v1.7.0` is 20 commits ahead. `upstream/main` = `v1.7.0` (`379b8d01`) — no newer upstream tag, so no sync this milestone. No seeds, no pending todos.

**Out of this milestone:** the next upstream sync (nothing to sync); ClickHouse client `0.10.0`; adopting upstream's Avro-union or Debezium paths; running the upgrade on production connectors.

<details>
<summary>Milestone scope as planned — <code>v1.7.0_gg1-sync</code> Upstream v1.7.0 Sync & Follow-up Hardening (shipped 2026-10-10)</summary>

**Goal:** Merge upstream `v1.7.0` into `custom_build`, close all 17 open follow-ups (including R-01's silent data loss), and publish `v1.7.0_gg1` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Target features:**

- **Upstream `v1.7.0` sync** in the `v1.6.0_gg1-sync` shape: scratch branch, rollback tag, one mechanical merge commit, fork features re-applied by intent, non-force `--no-ff` merge into `custom_build` after human approval
- **Retire D-17 in favour of upstream's `setCompressionMethod`** (upstream #841, default `true`, also disables server response compression). Client stays `0.9.6`; `docs/FORK-DELTA.md` updated
- **R-01 fixed by routing**, not by throwing: in metadata-free `insertFormat=json` mode, `Struct` records go through the JSON writer so the rows actually land. The R-01 pin test flips to typed read-back. This is a deliberate fork behavior change — recorded in `FORK-DELTA.md` and CHANGELOG
- **Writer null-correctness** — WR-01 (end-to-end null inside `Map(K, Nullable(V))`) and WR-02 (MAP/ARRAY fail loudly on a null in a non-nullable slot), reconciled with upstream's #820 null-Map fix
- **Metadata/config correctness** — `shouldSkipDescribeRow` at any depth (P3-WR-01), fail-fast message for `INT128`/`INT256`/`UINT128`/`UINT256` (P3-WR-03), `Column.extractColumn` argument swap, `USERNAME` ConfigDef default, `cleanupExtraFields` operand order (P9-IN-01)
- **Test debt** — `HeaderToValue` collision contract (WR-03), `JsonColumnTest` read-back, `ClickHouseSinkTaskJsonInsertFormatTest` try/finally (P4-WR-01), `ClickHouseBase.tearDown()` drop-failure handling (Phase 10 WR-01), stale Javadocs (Phase 11 IN-01, P4-IN-01)
- **CI** — exactly one `tests.yaml` run per published SHA (concurrency group), and the CI fork count read from a log or recorded as unmeasured
- **Dependency bumps (user-approved 2026-10-07)** — jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0 (upstream's open dependabot branches), lz4-java 1.10.4 (from upstream `v1.7.0`)
- **Publication & rollout** — `VERSION=v1.7.0_gg1`, CHANGELOG, push and green CI; publish the post-`c61d90ce` `.planning/` history; an operator rollout checklist (MBean rename D-07, compression setting, R-01 behavior change)

**Baseline measured at milestone start (2026-10-07):** upstream `v1.7.0` = `379b8d01` (2026-09-30), 22 commits past `v1.6.0` (12 non-merge); `upstream/main` = `v1.7.0`. Delta: 8 files in `src/main` + build (+102 / −12), 12 test files (+580 / −42).

| Upstream change | Collision with the fork |
|---|---|
| #841 `setCompressionMethod` in `ClickHouseHelperClient`, `ClickHouseSinkConfig`, `ClickHouseWriter`, `KeeperStateProvider` | Same lines as the fork's D-17 pin — resolve to upstream |
| #820 null optional Map: `StructToJsonMap`, `ClickHouseWriter` MAP branch + nullable marker + `cleanupExtraFields` | Overlaps WR-01/WR-02 and P9-IN-01 |
| `KeeperStateProvider` state read counts streamed rows instead of `getSummary().getResultRows()` | No fork edits in this file — high-risk (exactly-once), verify separately |
| #801 buffered-sink duplicate after retriable failure (`AtLeastOnceBufferStrategy`) | None — fork does not use buffering |
| #800 Avro record unions → JSON | None — take with the merge, do not adopt (as with `DebeziumRecordConvertor`) |
| Client `0.9.5`→`0.9.6`, `lz4-java 1.10.4`, `clickhouse-jdbc` removed from the catalog | Fork's `0.9.6` FORK OVERRIDE becomes upstream's own value |

**Ordering constraint:** the sync lands first — `v1.7.0` rewrites the same code that WR-01/WR-02, P9-IN-01 and D-17 touch, so hardening goes on top of the merged tree.

**Out of this milestone:** ClickHouse client `0.10.0` (upstream dependabot, unmerged) — not adopted; the `lz4` setting stays on via upstream's flag. Adopting upstream's Avro-union or Debezium paths.

**Outcome:** all 39 requirements shipped. All 17 carried follow-ups were closed, except where a requirement was narrowed on the record: TYPE-08 covers `Int256`/`UInt256` only, and JSONP-08 delivers evaluation order only. lz4-java went past the planned `1.10.4` to an advisory-free `1.12.0`. Phase 15 also fixed the Debezium `Timestamp`/`NanoTimestamp` gap that Phase 13 review had found. Two fork-side defects surfaced and were deferred rather than fixed: the `Column` nullability defect (999.1) and the duplicate test-classpath lz4 (999.2).

</details>

<details>
<summary>Milestone scope as planned — <code>v1.6.0_gg1-sync</code> Upstream v1.6.0 Sync (shipped 2026-10-07)</summary>

**Goal:** Merge upstream `v1.6.0` into `custom_build`, preserving every fork feature, with the fork's test suite green against ClickHouse `latest` × client V2 and CI green on the pushed SHA.

**Target features:**

- Upstream inventory and rollback point — upstream wired as a permanent remote, scratch refs, a map of every file both sides touched, and a per-fork-feature verdict on whether upstream now covers it natively
- Big-bang merge of `v1.6.0` into `custom_build` with per-file conflict resolution; `build.gradle.kts` taken from upstream wholesale (version catalog + `testFixtures`), with JaCoCo reports, `testUnit`, and the JDK 17 / encoding settings re-applied on top
- Fork suite restored to green — 371 fork tests adapted to upstream's rewritten test infrastructure (`ClickHouseBase`, `ClickHouseTestHelpers`, `SchemaTestData`); upstream's 56 test files accepted whole and run on `latest` × V2, cloud-dependent ones left skipped
- CI and publication — `tests.yaml` updated for the new build, a green run verified on the pushed SHA, `VERSION` → `v1.6.0_gg1`, `custom_build` pushed to the public fork

**Baseline measured at milestone start (2026-09-18):**

Local `main` is exactly upstream `v1.2.9` (`bec167d`). Upstream `v1.6.0` is `789a57c` — **581 commits** ahead.

| Side | `src/main` + build delta |
|---|---|
| Fork over v1.2.9 | 36 files, +1,643 / −203 (of which ~880 lines are 4 net-new SMT files — no conflict surface) |
| Upstream v1.2.9 → v1.6.0 | 35 files, +3,603 / −865, plus 56 test files at +9,726 / −4,609 |

**Collision points — files both sides changed:**

| File | Fork | Upstream v1.6.0 |
|---|---|---|
| `ClickHouseWriter.java` | +85 (JSON/Dynamic fail-fast, MAP null marker) | ±1,289 — effectively rewritten |
| `ClickHouseHelperClient.java` | +133 (`targetTableFilter`, sub-column skip, UTF-8) | +359 |
| `ClickHouseSinkConfig.java` | +59 (`jsonInsertColumns`, `targetTableFilter`) | +227 |
| `Column.java` / `Table.java` / `Type.java` | `JSON(...)` / `Dynamic` mapping | +188 / +88 / +1 |
| `ProxySinkTask.java` | +35 (JMX `connector=`) | +105 |
| `Utils.java` | +11 | +135 |
| `ClickHouseSinkTask.java` | +9 | +139 |
| `build.gradle.kts` | +195 (JaCoCo, `testUnit`) | ±368 — moved to a version catalog (`libs.*`) + `testFixtures` |

**Conflict policy:** prefer upstream wherever its implementation passes the fork's tests — shrinking the fork delta is what makes the *next* sync cheap. The previous milestone's suite is the acceptance gate here; it was written for exactly this moment.

**Candidates for removal from the fork** (to be verified, not pre-decided): upstream's native `Type.JSON`, its new `DebeziumRecordConvertor` (+289), its rewritten `KeyToValue` (+90), and its new `Utils.handleException` (+135). Upstream still has no `Type.DYNAMIC`, so that part of the fork stays.

**Named risk:** upstream effectively rewrote `ClickHouseWriter.java`. The fork's edits there must be carried over by intent, not by diff.

**Out of this milestone:** the nine follow-ups in Active below, and production rollout. If the sync closes one of them for free, record the fact — do not fix it here.

**Outcome:** of the four removal candidates, upstream's native `Type.JSON` and `Utils.handleException` were adopted and `KeyToValue` is byte-identical to upstream; `DebeziumRecordConvertor` came in with the merge but was deliberately not adopted (`docs/FORK-DELTA.md` §5). `ClickHouseWriter.java` was carried over by intent as planned. Two carried follow-ups closed for free and were recorded, not fixed: client-V2 error classification (FREE-01) and `describeTableV2`'s database parameter, P3-WR-02 (FREE-03). FREE-02 confirmed upstream's map null-marker fix but did **not** close WR-01.

</details>

## Requirements

### Validated

<!-- Existing capabilities, inferred from the codebase map and `git diff main...custom_build`. -->

**Upstream connector (inherited):**

- ✓ Sink Kafka records into ClickHouse tables from schema (Struct), schemaless (Map), and String JSON records — existing
- ✓ At-least-once (`InMemoryState`) and exactly-once (`KeeperStateProvider`) offset-range deduplication — existing
- ✓ Insert formats (RowBinary, JSON, CSV/TSV) with type mapping including Map/Array/Tuple/Variant/Decimal/DateTime64 — existing
- ✓ Topic-to-table mapping, error tolerance with DLQ routing — existing
- ✓ Upstream SMTs `KeyToValue`, `ExtractTopic` — existing

**v1.6.0_gg1-sync milestone (shipped 2026-10-07, 53/53 requirements):**

- ✓ Upstream wired as the permanent `upstream` remote, rollback tag `pre-v1.6.0-sync`, the merge landed on scratch branch `sync/v1.6.0` as one 100%-mechanical commit (`673d29c3`, 0 of 40 conflicted paths differing from `v1.6.0`), with a generated per-path `MERGE-CHECKLIST.md` and a self-testing invariant script — Phase 6 (SYNC-01..06) — v1.6.0_gg1-sync
- ✓ `Type.DYNAMIC` re-applied beside upstream's native `Type.JSON`; RowBinary fails fast with a `DataException` for `DYNAMIC`/`UNKNOWN`, and `JSON` takes upstream's flag-gated write-as-string path — Phase 7 (MAP-01..03, WRT-01..05) — v1.6.0_gg1-sync
- ✓ `targetTableFilter` and the structural describe sub-column skip re-threaded onto upstream's rewritten `showTables`/describe loop and immutable `Table`; UTF-8 on every `InputStreamReader` in the helper client — Phase 7 (HLP-01..04) — v1.6.0_gg1-sync
- ✓ `targetTableFilter`/`jsonInsertColumns` config keys kept, `async_insert` still not forced, all thirteen new upstream options pinned at their defaults by a Docker-free test — Phase 7 (CFG-01..03) — v1.6.0_gg1-sync
- ✓ Metadata-free `insertFormat=json` path **kept** at a blocking-human checkpoint and re-applied as four guards behind one `shouldSkipTableMetadata()` predicate; `jsonInsertColumns` re-attached server-side through the surviving 5-arg `client.insert` overload; the `auto.evolve` gap (CR-01) guarded too — Phase 9 (JSONP-01..04) — v1.6.0_gg1-sync
- ✓ `build.gradle.kts` on upstream's version-catalog structure with the fork's JaCoCo / `testUnit` / JDK 17 additions re-applied — Phase 6 (all six fork additions re-applied; `help` + `dependencies` both green; all four D-12 resolved-version facts proved against the live graph)
- ✓ JMX `connector=<name>` re-plumbed onto upstream's new naming owner: `ClickHouseSinkTask.start()`'s `name` prop threaded through `ProxySinkTask` into `SinkTaskStatistics`, with the per-topic MBean composing from the task-level name rather than duplicating it — Phase 8 (ORCH-02, ORCH-03; `MBeanNameUnitTest` un-excluded and green, `MBeanRegistrationLiveTest` proving both levels live)
- ✓ `ProxySinkTask`/`ClickHouseSinkTask` resolved against upstream's `DeliveryStrategy`/`ChunkFlusher` layering with zero stale 4-arg `Processing` call sites (16 sites, all 5-arg; the six delivery/processing files are 0-line diffs against v1.6.0) — Phase 8 (ORCH-01)
- ✓ `Utils.java` and `Processing.java` — the two clean-merge files both sides touched — explicitly reviewed and recorded; `UtilsTest`/`RangeContainerTest` are 0-line diffs, `ProcessingTest` differs only by one forced `SinkTaskStatistics` constructor-arg adaptation — Phase 8 (ORCH-04)
- ✓ `Utils.handleException` confirmed to classify client V2's `ServerException` alongside V1's `ClickHouseException`, closing the fork's "error classification is dead code on V2" follow-up via upstream issue #818 — Phase 8 (FREE-01, closed by upstream's rewrite, no new test)
- ✓ UTF-8 restored to the three charset-less `InputStreamReader`s in the shared `ClickHouseTestHelpers`, undoing the regression the v1.6.0 merge introduced under the deliberate `windows-1252` test JVM — Phase 8
- ✓ `ClickHouseBase`'s shared-container-per-fork contract re-derived over upstream's per-class `@TestInstance(PER_CLASS)` rewrite: a lazily started static container behind one counted start site (`sharedContainerStarts()`), a fresh per-class database as the only isolation mechanism, and a per-class opt-out (`usesSharedContainer()`) for the two Toxiproxy classes — Phase 10 (HARN-03)
- ✓ The `@AfterEach` MBean leak sweep restored with its pattern verbatim (`com.clickhouse:type=ClickHouseKafkaConnector,*`), every swept name logged at WARN, and per-topic coverage proven rather than assumed; `ClickHouseTestHarnessTest` un-excluded from `compileTestJava` and green — Phase 10 (HARN-02)
- ✓ **Seventh silently-vanishing fork behaviour found and fixed:** `CLICKHOUSE_VERSION_DEFAULT` had reverted `latest` → `25.8.18.1` in the merge with no conflict marker and no compile error, so every local gate since Phase 6 — including the green closes of Phases 8 and 9 — silently ran against the wrong server. Pinned Docker-free by `ClickHouseVersionDefaultUnitTest` — Phase 10 (MERGE-CHECKLIST Recorded Fact 15)
- ✓ The milestone's first full-suite evidence taken on the server it actually claims to test: two consecutive clean runs on ClickHouse `latest` (654 tests each, 0 failures, 0 errors, byte-identical 57-class executed sets, skips non-growing at 18), plus HARN-01's Docker-free `testUnit` proof with the colima daemon genuinely stopped (327 tests, 0 failures) — Phase 10 (HARN-01)
- ✓ All 17 Phase-10-owned `MERGE-CHECKLIST.md` rows flipped to done, each citing a command re-run at execution time rather than a planning document; zero Phase-10-owned rows remain pending — Phase 10 (HARN-04..07)
- ✓ Upstream `v1.6.0` merged into `custom_build` with every fork feature preserved or consciously retired: published as the two-parent `--no-ff` merge `c61d90ce` (parents `6ac2d20c`, `586af775`), pushed without force after a recorded human approval, tagged `v1.6.0_gg1`, with the rollback tag `pre-v1.6.0-sync` → `6ac2d20c` on `origin` — Phase 11 (CI-07)
- ✓ `./gradlew test` green on ClickHouse `latest` (26.9.12.8) × client V2, fork tests plus upstream's: 663 tests, 0 failed, 0 errors, 18 skipped, with the skip set identical by (class, method) to Phase 10's named 18 and the Docker-free `testUnit` contract re-proved (331 tests) with the daemon down — Phase 11 (SUITE-01, SUITE-02)
- ✓ `tests.yaml` green on the published SHA: run `37619434848` on `c61d90ce`, 386 s against a 1350 s threshold, coverage artifact published; `VERSION` = `v1.6.0_gg1`; CI's job-environment version pins dropped so `ClickHouseVersionDefaultUnitTest` is the single source of truth — Phase 11 (CI-05, CI-06)
- ✓ `docs/FORK-DELTA.md` published as the written record of what the fork still patches and what it retired in favour of upstream, each row grep-confirmed against the merged tree, with R-01 leading its Known costs section. The delta is smaller by the behaviour ledger and by files (93→84 wide, 32→30 `src/main`), while insertions grew; the mixed raw numbers are published in full and were accepted at UAT — Phase 11 (SUITE-03)
- ✓ **Server-default break found and fixed before publication (D-17):** ClickHouse 26.9 defaults `network_compression_method` to ZSTD, which client `0.9.6` cannot decode. The connector now forces `lz4` on every V1 and V2 client, applied before operator options so an explicit operator value wins. Pinned by `NetworkCompressionPinUnitTest` and `NetworkCompressionPinLiveTest` — Phase 11 (plan 11-08)

**Fork additions (the subject of this project):**

- ✓ `TableOperationFilter` SMT: drops chosen Debezium operations (`c`/`u`/`d`/`t`) for listed `database.table`s — existing (fork)
- ✓ `HeadersToValue` SMT: copies all Kafka headers into a single map field of the value (schema and schemaless) — existing (fork)
- ✓ `HeaderToValue` SMT (+ `HeaderToValueConfig`): copies selected headers into value fields, including multi-valued headers — existing (fork)
- ✓ `ConvertToMapOrJson` SMT: converts Struct values to Map or JSON string (Jackson/Gson, bytes → base64 options) — existing (fork)
- ✓ `targetTableFilter`: regex restricting table metadata discovery, applied server-side via `match(table, '<regex>')` with quote escaping in `ClickHouseHelperClient` — existing (fork), covered Phase 3
- ✓ `JSON(...)` parameterized columns and `Dynamic` columns recognized in `Column`/`Type` mapping — existing (fork), covered Phase 3
- ✓ `describeTable` streams rows and skips system sub-columns (`.null`, `.size0`, `.size`, and children of `JSON`/`Dynamic` columns) — existing (fork), covered Phase 3

**Validated by this project's tests:**

- ✓ `targetTableFilter` covered: empty filter adds no clause, no-match yields empty mapping, quote escaping, RE2 `\d`/anchors live — Phase 3 (FILT-01..04)
- ✓ `describeTable` sub-column skipping covered for `Nullable`, `Array`, `JSON` and `Dynamic`; unmappable column still drops the table, now logged at ERROR — Phase 3 (DESC-01..04)
- ✓ `JSON`, `JSON(...)` and `Dynamic` columns covered: type-string mapping plus typed read-back round-trips on both surviving insert routes — Phase 3 (TYPE-01..05)
- ✓ RowBinary fails fast with an actionable `insertFormat=json` / `bypassRowBinary=true` message before any byte reaches ClickHouse, instead of silently desyncing the stream — Phase 3 (TYPE-06)
- ✓ Cyrillic/CJK/non-BMP values and identifiers round-trip through String/`JSON`/`Dynamic` columns and SMT headers under the `windows-1252` test JVM — Phase 3 (TYPE-07)
- ✓ Explicit `insertFormat=json` skips table discovery, validation, and field cleanup; inserts `JSONEachRow` directly using topic-to-table mapping — existing (fork, WIP)
- ✓ `jsonInsertColumns`: comma-separated explicit column list passed to JSON inserts — existing (fork, WIP)
- ✓ JMX MBean name includes `connector=<name>` (from connector `name` prop), with version sanitized (`:` → `_`, `null` → `unknown`) — existing (fork)
- ✓ Connector no longer forces `async_insert=0`; async insert is controlled by server profile / `clickhouseSettings` — existing (fork, intentional)

**Delivered by this project:**

- ✓ Green baseline: `./gradlew test` passes in full against ClickHouse `latest` × client V2 (194 tests, 0 failed, 7 skipped) on a harness with one ClickHouse container per test JVM fork, per-class databases, an MBean leak sweep, UTF-8 test helpers, a server-version smoke test, and a Docker-free `testUnit` task — Phase 1
- ✓ JaCoCo coverage report generated for the test run (HTML + XML), with a focused report on the fork-changed classes and Lombok-generated code excluded — Phase 1
- ✓ `TableOperationFilter` gaps covered: truncate (`t`) operation, multiple tables, invalid/empty `skipped.operations`, whitespace handling — Phase 2
- ✓ `HeadersToValue` covered: null value, no headers, null header values, field name collision with existing value field, missing/blank `field` config, schema and schemaless paths — Phase 2
- ✓ `HeaderToValue` covered: multi-valued headers, missing headers, config parsing edge cases — Phase 2
- ✓ `ConvertToMapOrJson` existing tests reviewed; remaining gaps (null/optional fields, nested arrays/maps) covered — Phase 2
- ✓ At least one sink-level test per fork SMT showing transformed records land correctly in ClickHouse — Phase 2
- ✓ Three stale-state SMT bugs fixed and pinned: `TableOperationFilter.configure()` unioning across repeated calls (D-03), `HeaderToValue`/`HeadersToValue` serving a stale cached `valueSchema` after a source-schema change (D-02), `ConvertToMapOrJson.convertFieldValue()` not recursing into List/Map values (D-01) — Phase 2
- ✓ `ClickHouseWriter` MAP-branch RowBinary null-marker bug fixed: `Map(K, Nullable(V))` columns were silently unwritable on the client-V2 path — Phase 2 (approved out-of-scope fix)
- ✓ Metadata-free JSON insert path pinned: the `insertFormat=json` gate in any letter case (including the upstream `getOrDefault(INSERT_FORMAT, "node")` typo), table resolution via `topic2TableMap` without fetched metadata, the `cleanupExtraFields` bypass, and tombstone handling — Phase 4 (JSON-01..04)
- ✓ Both pre-existing explicit-JSON tests upgraded from row counts to typed column-value read-back — Phase 4 (JSON-05)
- ✓ `jsonInsertColumns` pinned end to end: parsing contract (blank ⇒ empty list, whitespace and empty entries dropped, order preserved), the default empty list, an explicit subset against non-trivial `DEFAULT`s, extra payload keys, a nonexistent column with its error classification, and the non-explicit path's `cleanupExtraFields` interaction — Phase 4 (COLS-01..06)
- ✓ JMX MBean naming and per-task registration pinned: `connector=<name>` present/absent, `version=unknown` for a null version, `:` → `_`, and four tasks across two connectors registering distinct MBeans with `stop()` unregistering only its own — Phase 4 (JMX-01..04)
- ✓ `ProxySinkTask.buildMBeanName(String, int, String)` extracted, behavior-preserving, so the null-version and colon-in-version branches are reachable from a test at all — Phase 4 (the phase's only production change, pre-approved by D-02)
- ✓ `async_insert` left to the user pinned: default config sends no `async_insert` setting, a user-supplied value in `clickhouseSettings` passes through unchanged, and a live insert with `async_insert=1` lands its data — Phase 4 (ASYNC-01..03)
- ✓ Fork CI rewritten as a single `ClickHouse latest × client V2` job on current actions (`checkout@v4`, `setup-java@v6`, `gradle/actions/setup-gradle@v6`, `upload-artifact@v4`), dropping upstream's V1/23.7/24.3/cloud matrix and both cloud-credential secret references — Phase 5 (CI-01, CI-02)
- ✓ Coverage published to the Actions job summary by a checked-in standard-library-only renderer (`.github/scripts/jacoco_summary.py`): two headline rows plus a 15-row fork-changed-class table, with JaCoCo and Gradle test reports uploaded as 14-day artifacts — Phase 5 (CI-03)
- ✓ `custom_build` pushed to the public fork and the resulting CI run machine-verified green on the pushed SHA — run `35317272594`, one job, both key steps `success`, both artifacts present — Phase 5 (CI-04)
- ✓ Quality gate — bugs surfaced by the new tests were fixed in the phase that found them: 4 SMT bugs plus one approved writer fix (Phase 2), the UTF-8 describe-reader fix (Phase 3); R-01 escalated and pinned by user decision rather than silently carried (Phase 4)
- ✓ Quality gate — `./gradlew test` green in full on ClickHouse `latest` × client V2 (371 tests, 0 failures, 0 errors, 7 pre-existing skips), confirmed both locally and in CI — Phase 5
- ✓ Quality gate — fork CI runs the suite on push against `latest` × V2 and publishes the coverage report — Phase 5

**Delivered in `v1.7.0_gg1-sync` (shipped 2026-10-10, 39/39 requirements):**

- ✓ Upstream `v1.7.0` merged on scratch branch `sync/v1.7.0` as one mechanical merge commit (`54e63648`, zero authored lines), rollback tag `pre-v1.7.0-sync`, parameterised merge-invariant script with line provenance plus a 39-row FORK-DELTA anchor grep, and a v1.7.0 merge checklist with reviewed verdicts for auto-merged hunks in fork-touched files — Phase 12 (SYNC-07, SYNC-08)
- ✓ Every fork feature in `docs/FORK-DELTA.md` survives the merge, each proven by its existing pin test; upstream's `v1.7.0` tests accepted unmodified, with the #800 Avro-union and cluster `buildFromClause` tests compile-only and named in the FORK-DELTA skip inventory — Phase 12 (SYNC-09, SYNC-10)
- ✓ D-17 retired onto upstream's `setCompressionMethod` for every client the connector builds, pinned Docker-free and proven live on `latest` with the flag on and off; FORK-DELTA records `compress=false` alone as sufficient on both clients — Phase 12 (CMP-01..03)
- ✓ #820's null-Map bytes reachable for non-nullable `Map` columns (fork MAP null guard scoped to `Nullable`), pinned Docker-free; CR-02 handled by adding non-nullable pins beside the unchanged nullable ones, each with its rationale — Phase 12 (WRT-06, WRT-09)
- ✓ `KeeperStateProvider`'s streamed-row state read covered by a restart-read test, seen red (4 of 6) against the old summary-count read — Phase 12 (EOS-01)
- ✓ Fork LZ4 capability rule removed in favour of upstream's (one `lz4-java` on `runtimeClasspath`), catalog drops `clickhouse-jdbc` and keeps `kafka = "4.1.1"`; full suite green on `latest` 26.9.12.8 × V2 (691 tests, 18 named skips) — Phase 12 (BLD-09, BLD-13)

- ✓ R-01's silent data loss fixed by routing: in metadata-free `insertFormat=json` mode `Struct` and `DEBEZIUM_CDC` records go through a new route-scoped, schema-aware `MetadataFreeJsonWriter` (Timestamp/Date/Time/Decimal/bytes encoded so ClickHouse stores what RowBinary would; empty CDC records skipped); parity with RowBinary proven by a live twin-table matrix with no hand-written expectations; null semantics, `jsonInsertColumns` and `bypassFieldCleanup` interactions pinned; `cleanupExtraFields` evaluates the metadata-free predicate first — Phase 13 (JSONP-05..08)
- ✓ RowBinary nested nulls either round-trip or fail loudly: `Map(K, Nullable(V))`/`Array(Nullable(V))` nulls pinned live as `NULL`; a nested null in a non-nullable scalar/Tuple slot throws a data-free `DataException` naming the column (no row written, DLQ-routable); a nested null `Array`/`Map` is written empty — Phase 13 (WRT-07, WRT-08)
- ✓ Metadata/config findings fixed and pinned: DESCRIBE skip at any depth below JSON/Dynamic, `Int256`/`UInt256` fail fast, `Column.extractColumn` Array-recursion argument order, `USERNAME` ConfigDef default — Phase 13 (DESC-05, TYPE-08, COLS-07, CFG-04)
- ✓ Test debt closed: `HeaderToValue` non-STRING target pinned, `JsonColumnTest` typed read-back, try/finally lifecycle, attributable `ClickHouseBase` teardown failures, four stale Javadocs; full suite green on `latest` 26.9.13.15 × V2 (cache-busted, 803 tests, 18 named skips); behaviour ledger L-01..L-19 in `13-BEHAVIOUR-LEDGER.md` for Phase 15 — Phase 13 (DEBT-01..05)
- ✓ Dependency bumps landed one attributable one-line catalog commit each, gated in order: lz4-java `1.10.4` → `1.12.0` (advisory-free; floor is 1.11.4; live `Lz4RoundTripLiveTest` proves client `0.9.6` exercises it), okhttp `5.5.0`, mockito `5.24.0`, Jackson `2.22.3` (closes CVE-2026-91776/91777) and org.json `20260814`; Jackson and org.json each gated by a full live suite on `latest` 26.9.14.10 × V2 (805 tests, 18 named skips, 152 wide-integer/Decimal256 outcomes unchanged) — Phase 14 (BLD-10, BLD-11, BLD-12)
- ✓ `tests.yaml` checks what a local run checks: branch-push-only trigger (a tag push starts no run), a "Compile integration-test sources" step before "Run tests", and a "Test fork count" step; observed on CI as run `38037397969` on the pushed `sync/v1.7.0` HEAD `54b5ce2f` — exactly one run, green, `nproc=4, derived maxParallelForks=2` — Phase 14 (CI-08, CI-09, CI-10)
- ✓ Gated tree passed the full suite twice on `latest` 26.9.14.10 × V2 (`clean test`, then `test --rerun`): 830 tests, 0 failures, 0 errors, 18 skips identical across runs and equal to FORK-DELTA's named skips; `testUnit` green with Docker stopped — Phase 15 (SUITE-04)
- ✓ `VERSION` = `v1.7.0_gg1` with a `1.7.0_gg1` CHANGELOG section (R-01 routing, WR-02 strictness, compression, lz4-java 1.12.0); FORK-DELTA rewritten for the v1.7.0 tree (D-17 retired, R-01 closed, single LZ4 rule, Nullable-only MAP guard, L-20..L-22, Nullable Enum/Tuple WARN) — Phase 15 (PUB-01, PUB-02)
- ✓ `v1.7.0_gg1` published: `custom_build` on `origin` = two-parent `--no-ff` merge `f328fd21` (approved verbatim "publish", no force), tags `v1.7.0_gg1` and `pre-v1.7.0-sync` (`c61d90ce`) on `origin`; exactly one `tests.yaml` run on the published SHA (`38050860929`, green, none cancelled), proving CI-08 end to end; the post-`c61d90ce` `.planning/` history published through the merge without `.gsd-ws-arg` or local-only tags — Phase 15 (PUB-03, PUB-04)
- ✓ Operator rollout checklist `docs/ROLLOUT-v1.7.0_gg1.md`: 8 items, each with what changed, an "Am I affected?" self-check and an action — MBean `version=` rename, compression and `compress=true` caveat with the server-profile check, R-01 behaviour change with the exactly-once backfill, JSON-route type notes (incl. L-08 `input_format_null_as_default`), nested-null `DataException`, rollback tag `pre-v1.7.0-sync` — Phase 15 (ROLL-01)

### Active

<!-- Milestone `v1.7.0_gg2-hardening` is active: every open `[ ]` item below is in its scope (see REQUIREMENTS.md). Struck-through items are history. -->

**v1.6.0_gg1-sync:** all 53 requirements shipped and moved to Validated.

**v1.7.0_gg1-sync:** all 39 requirements shipped and moved to Validated (archive: `milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`). All 17 follow-ups carried in are closed. The open items below emerged during this milestone.

**Publication & client follow-ups (emerged in Phase 11, not yet scheduled):**

- ~~`ClickHouseVersionDefaultUnitTest`'s class Javadoc still says "CI pins `CLICKHOUSE_VERSION: latest`" in the published tree. D-09 corrected the same sentence only in `ClickHouseTestHelpers`. Comment-only (Phase 11 review IN-01)~~ **Closed in Phase 13 (DEBT-05)** — Javadoc rewritten to describe the code as it is
- ~~The CI fork count was never read from a log. The local gate ran 6 forks; CI's was inferred from a 2-core runner assumption, and public `ubuntu-latest` runners are usually 4 vCPU. Read it from an authenticated log, or accept the topology as unmeasured (Phase 11 verification backstop)~~ **Closed in Phase 14 (CI-09)** — a "Test fork count" notice annotation, readable without a token, reported `nproc=4, derived maxParallelForks=2` on run `38037397969`

- ~~Pushing a tag onto the same SHA as a branch push starts a second `tests.yaml` run in the `github.sha` concurrency group, which cancels the branch run. CI-06 had to narrow its locator to `conclusion != cancelled`. Either key the concurrency group on `github.ref` too, or push tags separately, so a published SHA has exactly one run again~~ **Closed in Phase 14 (CI-08)** — `tests.yaml` now triggers on branch pushes only, so a tag push starts no run. Residual: two *branches* pushed at one SHA still cancel each other, and the pre-fix tag `pre-v1.7.0-sync` (c61d90ce) still runs the old workflow when pushed — Phase 15 pushes the published SHA on one branch only
- ~~The forced `network_compression_method=lz4` (D-17) is a workaround for client `0.9.6`. Remove it once the client decodes ZSTD, under the removal condition recorded in `docs/FORK-DELTA.md`~~ **Closed in Phase 12 (CMP-01..03)** — D-17 retired onto upstream's `setCompressionMethod`. The pre-rollout check that no server profile constrains the compression setting (T-11-38) moves to the rollout checklist (ROLL-01)

**Transform follow-ups (emerged in Phase 2 code review, not yet scheduled):**

- ~~End-to-end coverage for an actual `null` entry inside a `Map(K, Nullable(V))` column — Phase 2's fix is asserted for non-null values only; the null path is code-read, never exercised through RowBinary (WR-01)~~ **Closed in Phase 13 (WRT-07)** — needed no writer change; pinned live for 12 `Map(String, Nullable(V))` and 4 `Array(Nullable(V))` types, with a seen-red mutation
- ~~`ClickHouseWriter` MAP/ARRAY branches fail loudly, as `doWriteCol()` does, when a `null` lands in a non-nullable element slot instead of silently desyncing the RowBinary stream (WR-02)~~ **Closed in Phase 13 (WRT-08)** — nested null in a non-nullable scalar/Tuple slot throws a data-free `DataException` naming the column; a nested null `Array`/`Map` is written as an empty container
- ~~Pin `HeaderToValue`'s behavior when `fields` names a pre-existing non-STRING column — currently throws `DataException` on every record, unlike `HeadersToValue`'s tested collision contract (WR-03)~~ **Closed in Phase 13 (DEBT-01)** — current behaviour pinned (schema path throws, schemaless overwrites with the header String); no main change

**Table metadata & type follow-ups (emerged in Phase 3 code review, not yet scheduled):**

- ~~`shouldSkipDescribeRow` walks only the immediate parent via `lastIndexOf('.')`, so a DESCRIBE row two or more levels below a `JSON`/`Dynamic` column leaks through as an orphaned `Column` plus an ERROR log. No data loss; ClickHouse `latest` was not observed emitting that shape in the Phase 3 live audit (P3-WR-01)~~ **Closed in Phase 13 (DESC-05)** — ancestor-prefix walk skips rows at any depth
- ~~`validateDataSchema`'s fail-fast gate enumerates only `JSON`/`DYNAMIC`/`UNKNOWN`, while `doWriteColValue`'s throw is a true `default:` catch-all — so `INT128`/`INT256`/`UINT128`/`UINT256` still surface the old bare "Data schema validation failed" message. Unchanged from pre-Phase-3 behavior, so not a regression (P3-WR-03)~~ **Closed in Phase 13 (TYPE-08, narrowed)** — `Int256`/`UInt256` now fail fast with the actionable message; `Int128`/`UInt128` keep being written
- ~~`describeTableV2` builds its DESCRIBE query from `this.database` instead of the passed-in `database` parameter (P3-WR-02)~~ **Closed by the v1.6.0 merge (FREE-03)** — upstream's `describeTableV2` builds from the method's own `database` parameter. Recorded in MERGE-CHECKLIST Recorded Fact 10; nothing needed fixing

**JSON insert path & connector follow-ups (emerged in Phase 4, not yet scheduled):**

- ~~**R-01 — silent data loss.** `insertFormat=json` + a `Struct` (schema'd) record + `bypassRowBinary` at its default `false` writes **zero rows and throws nothing**: metadata-free mode builds a column-less synthetic `Table`, so `validateDataSchema`'s loop is vacuous and `doInsertRawBinaryV2`'s per-column write loop emits no bytes. Confirmed live against server `26.8.6.5` (`Written Rows: [0]`, 0 of 5 records landed). Pinned by a labelled test in Phase 4 at the user's direction; the fail-fast fix is **not** pre-approved (D-01/D-07) and needs its own plan — it would break any pipeline running explicit JSON behind a Struct converter. **Re-affirmed in Phase 9 (D-01):** the keep-or-drop call went KEEP, so the loss is re-introduced knowingly as an accepted cost, with the pin carried forward byte-identical. Still open, still unfixed~~ **Closed in Phase 13 (JSONP-05..07)** — fixed by routing: `Struct` and `DEBEZIUM_CDC` records in metadata-free mode go through a new schema-aware `MetadataFreeJsonWriter`, parity with RowBinary proven by a live twin-table matrix; the R-01 pin flipped to typed read-back of all rows
- ~~**Error classification is dead code on the V2 path.**~~ **Closed in Phase 8 (FREE-01)** — upstream's rewritten `Utils.handleException` now classifies `com.clickhouse.client.api.ServerException` (client V2) alongside `ClickHouseException` (client V1), so the retriable/non-retriable error-code table is reachable again. Closed citing upstream issue #818 as the mechanism; recorded rather than pinned with a new test, per the requirement's own wording
- ~~`ClickHouseSinkTaskJsonInsertFormatTest`'s two tests skip the `try/finally` task-lifecycle pattern every sibling live test uses; an unexpected `put()` failure leaves an MBean registered until the class's `@AfterEach` sweep catches it — Phase 4 code review (P4-WR-01, mitigated)~~ **Closed in Phase 13 (DEBT-03)**
- ~~`AsyncInsertLiveTest`'s class Javadoc reads as if it pins the bare `async_insert=1` race, but no test exercises that combination — Phase 4 code review (P4-IN-01, doc clarity)~~ **Closed in Phase 13 (DEBT-05)**

**Merge-era review follow-ups (Phases 7 and 10, accepted rather than fixed, not yet scheduled):**

- ~~`Column.extractColumn`'s Array recursion swaps the `hasDefaultValue`/`isSubColumn` positional arguments. Dead code today, so a landmine for later (Phase 7 review warning)~~ **Closed in Phase 13 (COLS-07)** — argument order fixed, pinned by `ColumnArrayRecursionTest`
- ~~`ClickHouseSinkConfig`'s `ConfigDef` default for `USERNAME` uses `passwordDefault` (`""`) instead of `usernameDefault` (`"default"`). Connect's config UI and docs report the wrong default; runtime is unaffected (Phase 7 review warning)~~ **Closed in Phase 13 (CFG-04)**
- ~~`JsonColumnTest` asserts row counts only and never reads the payload back (Phase 7 review warning; 3 Info findings also open in the archived `07-REVIEW.md`)~~ **Closed in Phase 13 (DEBT-02)** — all 10000 rows read back per configuration via `RowSnapshots`
- ~~`ClickHouseBase.tearDown()` logs and swallows a database-drop failure. Under the shared container that drop is the only isolation between classes in a JVM fork, so a transient failure can make an unrelated later class fail with no attributable cause. Accepted as a cost in MERGE-CHECKLIST Recorded Fact 16 (Phase 10 WR-01)~~ **Closed in Phase 13 (DEBT-04)** — a shared-container drop failure now fails the leaking class with an attributable `IllegalStateException`

**Metadata-free JSON path follow-up (emerged in Phase 9 code review, not yet scheduled):**

- ~~`cleanupExtraFields`'s guard reads `csc.isBypassFieldCleanup() || shouldSkipTableMetadata()`, so it dereferences `csc` before reaching the predicate whose whole point is a `csc != null` short-circuit. Unreachable today — the method is only called from `writeRecordsJson`, itself only reachable through a started writer — but it means the null-safety guarantee documented on `shouldSkipTableMetadata()` does not actually hold at every call site. Fix is to swap the operands — Phase 9 code review (P9-IN-01, informational, deliberately out of the fix scope)~~ **Closed in Phase 13 (JSONP-08)** — operands swapped; delivers evaluation order only (dated D-13 correction in `REQUIREMENTS.md`)

**Phase 13 review & research follow-ups (emerged 2026-10-08, not yet scheduled — all open in `13-REVIEW-DISPOSITION.md` / ROADMAP backlog):**

- ~~⚠ `io.debezium.time.Timestamp` (epoch-millis long, Debezium's default MySQL `datetime` mapping) goes through the metadata-free JSON route unconverted, so ClickHouse reads milliseconds as seconds — a silent wrong value. Known gap in `13-BEHAVIOUR-LEDGER.md` (L-05); RowBinary stores it correctly (13-REVIEW WR-02)~~ **Closed in Phase 15 (15-01, `981a89d6`)** — Debezium `Timestamp`/`NanoTimestamp` written as decimal seconds on the metadata-free JSON route
- [ ] The empty-CDC-record skip (op=`t`, null before/after) applies only to the explicit `insertFormat=json` route; the upstream `bypassRowBinary=true` JSON route still writes a `{}` all-defaults row (13-REVIEW WR-01, ledger L-03)
- [ ] `Column.extractColumn` drops `Nullable` for `Enum8`/`Enum16` and `Tuple`, so values are mis-stored and the new nested-null guards throw on legal `Nullable(Enum)`/`Nullable(Tuple)` slots — ROADMAP backlog 999.1 (13-REVIEW WR-05, ledger L-13)
- [ ] `doWriteCol` throws a message-less `RuntimeException` for a missing non-nullable value, now reachable for `Array(Tuple)` after COLS-07, so Connect status and the DLQ show no column name (13-REVIEW WR-03)
- [ ] `e.getMessage().contains(code)` runs before its null check in `ClickHouseWriter`'s error path; pre-existing, but new exception types flow through it (13-REVIEW WR-04)
- ~~Non-nullable null outcomes on the metadata-free JSON route rely on the server default `input_format_null_as_default=1` — belongs in the rollout checklist (ledger L-08)~~ **Closed in Phase 15 (ROLL-01)** — rollout item 3 states the dependency and gives the `system.settings` self-check

**Phase 14 review & CI follow-ups (emerged 2026-10-10, not yet scheduled — open in `14-REVIEW-DISPOSITION.md` / ROADMAP backlog):**

- [ ] `Lz4RoundTripLiveTest` asserts LZ4 on the request leg only (`CompressedReadBufferBlocks > 0`); the response leg was proven once by an uncommitted zstd probe, not by a standing assertion (14-REVIEW WR-01)
- [ ] The "Test fork count" step recomputes `maxParallelForks` in shell from `nproc` rather than reading Gradle's container-aware value, duplicating the `build.gradle.kts` formula (14-REVIEW WR-02)
- [ ] `testRuntimeClasspath` carries `org.lz4:lz4-java:1.8.0` beside `at.yawk.lz4:lz4-java` via the Confluent 7.9.1 test stack — test-only, not shipped, needs a `build.gradle.kts` change — ROADMAP backlog 999.2
- [ ] CI annotations on run `38037397969`: Node.js 20 deprecation for `actions/checkout@v4`/`upload-artifact@v4`; `ubuntu-latest` moves to Ubuntu 26 from 2026-10-19; Lombok "Ambiguous: Jackson2 and Jackson3 exist" warning under Jackson 2.22.3

**Phase 15 publication follow-ups (emerged 2026-10-10, not yet scheduled):**

- [ ] ⚠ The published `docs/ROLLOUT-v1.7.0_gg1.md` (at `f328fd21`) still has the pre-review text: item 2 offers `TRUNCATE TABLE` on the KeeperMap state table, which every `exactlyOnce` connector with the same `zkDatabase`/`zkPath` shares, so following it erases other connectors' state (15-REVIEW WR-01 / T-15-14); item 7's `version=*` advice is wrong for regex matchers (WR-03); FORK-DELTA L-05 states `best_effort` as the default without a version (WR-02). Fixed locally by docs-only `505e4981` on `sync/v1.7.0`; at Phase 15 UAT the user chose to leave it unpublished for now, so it reaches `origin` with the next `--no-ff` merge unless pushed earlier. Operators reading the published copy should be pointed at the corrected text
- [ ] Audit threats T-13-19 (upstream ERROR log of an unparseable DateTime string) and T-14-16 (`tests.yaml` has no `permissions` block; tag-pinned actions) open below the `high` block threshold, listed in FORK-DELTA Known costs
- [ ] 15-02 code review of the D-01/D-02 commits: 6 Info findings deferred (`15-REVIEW-DISPOSITION.md`)

**Quality gates:** all three met — moved to Validated at the Phase 5 transition.

**Milestone-close note (v1.2.9_gg4-tests):** every item above is a *review finding*, not an unmet v1 requirement — all 75 v1 requirements shipped. None is a regression; each is either pre-existing fork behavior now documented, or a hardening opportunity the coverage work made visible for the first time.

**Milestone-close note (v1.6.0_gg1-sync):** 17 items open. Ten items came in from `v1.2.9_gg4-tests`: two closed for free (FREE-01, FREE-03) and eight carry forward. Nine are new, from Phase 7, 9, 10 and 11 review and verification. None is an unmet requirement; R-01 is still the only one that loses data.

**Milestone-close note (v1.7.0_gg1-sync):** all 17 carried items are closed. Eleven new items are open: four from Phase 13 review, four from Phase 14 review and CI, and three from Phase 15 publication. Info-level review findings are tracked in each phase's `REVIEW-DISPOSITION.md`. None is an unmet requirement. None loses data silently. 999.1 stores wrong values for `Nullable(Enum)`, but it is a pre-existing defect that is now warned about at discovery. The most urgent item is publishing `505e4981`, because the published rollout doc can erase shared exactly-once state.

### Out of Scope

- New connector features — the goal is coverage of existing fork behavior, not new capability
- Completing WIP features beyond bug fixes (e.g., validating `jsonInsertColumns` against the table schema, new config semantics) — user chose to test WIP behavior as-is
- Refactoring `ClickHouseWriter`, `Processing`, or `ClickHouseSinkConfig` — not required for coverage; high churn on a fork that must stay rebasable
- ~~Syncing `custom_build` with upstream `ClickHouse/clickhouse-kafka-connect`~~ — **no longer excluded.** This is the whole of `v1.6.0_gg1-sync`; the test suite shipped by `v1.2.9_gg4-tests` was built to make exactly this safe
- Test matrix for ClickHouse 23.7 / 24.3 and client V1 — production connectors use V2; only `latest` matters (older versions also lack JSON/Dynamic)
- ClickHouse Cloud test matrix — fork has no cloud credentials and does not use Cloud
- Upstream's disabled `testDBTopicSplit` — upstream feature, not a fork addition
- Upstream hardening items from `.planning/codebase/CONCERNS.md` (SQL interpolation in `KeeperStateProvider`, reload4j replacement) — not fork additions

**Out-of-Scope audit at v1.2.9_gg4-tests close** — every exclusion above still holds, with two adjustments:

- **`error categorization` removed from the upstream-hardening exclusion.** It was excluded as an upstream concern, but Phase 4 found the fork's own client-V2 migration is what makes `Utils.handleException`'s error-code table unreachable. That is a fork-facing defect, so it moved to Active rather than staying excluded.
- **"Completing WIP features beyond bug fixes" narrowed in practice.** The exclusion held — the WIP JSON path and `jsonInsertColumns` were pinned as-is, `src/main/` untouched by plans 04-01 and 04-02. R-01's fix stays out of scope for the same reason the exclusion exists, but is now tracked in Active with a known cost rather than merely excluded.

**Out-of-Scope audit at v1.6.0_gg1-sync close.** Every exclusion still holds, with four notes:

- **Upstream sync is now a recurring activity, not an exclusion.** `upstream` is a permanent remote and `docs/FORK-DELTA.md` is the record the next sync starts from.
- **"New connector features" held under pressure twice.** The merge made upstream's `DebeziumRecordConvertor`, `auto.evolve`, buffered delivery and the retry options available. None was adopted (`FORK-DELTA.md` §5). D-17's `lz4` pin is a compatibility fix for a server-default change, not a feature.
- **Client V1 is still out of the test matrix**, but D-17 had to be applied to V1 too, because the shared default options feed both clients.
- **Production rollout** of `v1.6.0_gg1` stayed out of scope, as planned. The operator-visible MBean name change (D-07) and the `lz4` pin (T-11-38) are the two things to check at rollout.

**Out-of-Scope audit at v1.7.0_gg1-sync close.** Every exclusion still holds, with three notes:

- **"New connector features" held again.** The R-01 fix routes records to a new route-scoped `MetadataFreeJsonWriter`. This restores rows the fork was already supposed to write; it adds no capability. Upstream's #800 Avro unions and `DebeziumRecordConvertor` were merged but not adopted.
- **The `ClickHouseWriter` refactoring exclusion was stretched, not broken.** The R-01 routing, nested-null rules and Int256 fail-fast are targeted edits. The new writer lives in its own file, so the upstream JSON paths stay byte-identical.
- **Rollout guidance is now in scope; rollout itself is not.** The operator checklist shipped (ROLL-01). Running the upgrade on production connectors stays with the operators.

## Context

- **Repository:** fork `git@github.com:ne1r0n/clickhouse-kafka-connect.git` (`origin`), branch `custom_build` at merge `f328fd21` = upstream `v1.7.0` + the fork delta in `docs/FORK-DELTA.md`. `upstream` remote → `ClickHouse/clickhouse-kafka-connect`. Tags on `origin`: `v1.7.0_gg1` and `v1.6.0_gg1` (published), `pre-v1.7.0-sync` and `pre-v1.6.0-sync` (rollback). The planning history after publication lives on local `sync/v1.7.0` (17 commits past `f328fd21`, including the unpublished `505e4981`). Milestone tags (`v1.7.0_gg1-sync` etc.) are local-only. `custom_build` is protected; the scratch branch was deleted from `origin` once CI went green. Codebase map in `.planning/codebase/` predates the merge (mapped at `22f055c`) and is stale for `ClickHouseWriter`, `ClickHouseHelperClient` and the build.
- **Stack:** Java 17 toolchain, Gradle 9.6.1 with upstream's version catalog (`gradle/libs.versions.toml`) and `testFixtures`, Kafka Connect API 4.1.1 (fork-pinned), ClickHouse Java client 0.9.6 (fork-pinned; V1 and V2 paths), Jackson + Gson, Lombok. Tests: JUnit Jupiter 5.14.4, Mockito, Hamcrest, Testcontainers 2.0.5 (ClickHouse, Kafka, Toxiproxy). Shared test helpers (`ClickHouseTestHelpers`, `SchemaTestData`, `SchemalessTestData`) live in `src/testFixtures`.
- **Test layout:**
  - `src/test/java` — unit tests plus ClickHouse-backed tests extending `ClickHouseBase` (starts a Testcontainers ClickHouse per class); run by `./gradlew test`.
  - `src/test/java/transforms/` — SMT tests (outside the `com.clickhouse...` package).
  - `src/integrationTest/java` — Confluent-platform / Cloud tests, `integrationTest` task, only `workflow_dispatch` in CI.
  - ClickHouse image chosen by `CLICKHOUSE_VERSION` env (default `latest` since Phase 1, was `25.3`, `ClickHouseTestHelpers`); client by `CLIENT_VERSION` env (default V2). **The default silently reverted to `25.8.18.1` in the v1.6.0 merge and stayed wrong from Phase 6 to Phase 10** — Phases 8 and 9 therefore closed green against a server this milestone does not claim to test. Restored and pinned Docker-free by `ClickHouseVersionDefaultUnitTest` (Phase 10; MERGE-CHECKLIST Recorded Fact 15). Tests run with `file.encoding=windows-1252` — deliberately, so non-ASCII handling is exercised rather than masked by a UTF-8 default.
  - `ClickHouseBase` gives one shared container per test JVM fork, a fresh empty database per test class, an inherited `@Tag("clickhouse")`, an `@AfterEach` MBean leak sweep, and opt-in `uniqueConnectorName(TestInfo)`. New sink-task tests must create their tables before `start()` — `ClickHouseWriter.start()` fails against an empty database.
  - `./gradlew testUnit` runs everything not tagged `clickhouse`, needs no Docker, and is deliberately not part of `check`.
  - JaCoCo is report-only on `test`: `jacocoTestReport` plus `jacocoForkReport` over 15 explicitly listed fork-changed classes. No coverage threshold can fail the build.
  - Version gating helper exists: `src/test/java/.../extension/FromVersionConditionExtension.java`.
- **Fork test inventory (2026-09-15):**

  | Fork addition | Existing tests |
  |---|---|
  | `TableOperationFilter` | 26 unit (`TableOperationFilterTest`) + 1 sink-level — Phase 2 (was 9) |
  | `ConvertToMapOrJson` | 37 unit + perf test + 1 sink-level — Phase 2 (was 13) |
  | `HeadersToValue` | 25 unit + 1 sink-level — Phase 2 (was 2) |
  | `HeaderToValue` | 24 unit + 1 sink-level — Phase 2 (was 2) |
  | `targetTableFilter` | 1 ClickHouse-backed (`ClickHouseSinkTaskMappingTest.tableFilterTest`) + 1 in JSON insert test |
  | Explicit JSON insert path | 2 ClickHouse-backed (`ClickHouseSinkTaskJsonInsertFormatTest`, now typed read-back) + `ExplicitJsonInsertPathLiveTest` + `MetadataFreeJsonInsertUnitTest` — Phase 4 |
  | `jsonInsertColumns` | `JsonInsertColumnsUnitTest` (9 Docker-free) + `JsonInsertColumnsLiveTest` (5 live) — Phase 4 (was none) |
  | `JSON(...)` / `Dynamic` types | `JsonDynamicRoundTripTest` + type-string mapping and RowBinary fail-fast coverage — Phase 3 (TYPE-01..06; was none) |
  | `describeTable` sub-column skip | `Nullable`/`Array`/`JSON`/`Dynamic` sub-column coverage — Phase 3 (DESC-01..04; was none) |
  | MBean `connector=` name | `MBeanNameUnitTest` (Docker-free) + `MBeanRegistrationLiveTest` — Phase 4 (was none) |
  | `async_insert` not forced | `AsyncInsertSettingsUnitTest` (Docker-free) + `AsyncInsertLiveTest` — Phase 4 (was none) |

- **CI today (as of Phase 5):** `.github/workflows/tests.yaml` runs one job — `ClickHouse latest Client version V2 tests` — on every push: `./gradlew test`, then `python3 .github/scripts/jacoco_summary.py` to render coverage into the job summary, then two `if: always()` artifact uploads (`jacoco-coverage-reports`, `gradle-test-reports`, 14-day retention). Trigger is bare `on: [push]`; the job reads no secrets. Upstream's V1/23.7/24.3/cloud matrix and the deprecated `gradle-build-action@v2`/`setup-java@v3` actions are gone. Last verified run: `35317272594` on `a295af8`, green. `gh` CLI is still not installed locally — CI is checked via the unauthenticated GitHub REST API or the web UI.
- **Local environment:** Docker via colima must be running for every `ClickHouseBase` test. JDK 21 on PATH; Gradle toolchain targets 17.
- **Known risk — resolved.** `.planning/codebase/CONCERNS.md` flagged that `Type.DYNAMIC` has no RowBinary serialization in `ClickHouseWriter`. Phase 3 confirmed it and closed it the way the constraint required: `doWriteColValue` now throws an actionable `DataException` naming the column, its type, and the `insertFormat=json` / `bypassRowBinary=true` escape hatches, before any byte is written (TYPE-06). Explicit JSON insert continues to work — ClickHouse parses `JSONEachRow` server-side.
- **Baseline — established.** Phase 1 found the untouched suite already green on `latest` × V2 once a sandbox environment artifact was neutralized: 187 tests / 0 failed / 7 skipped, rising to 194 / 0 / 7 after the harness work. The final milestone run is 371 / 0 failures / 0 errors / 7 skipped.
- **CI after v1.6.0_gg1-sync:** same single `latest` × V2 job, now with no `CLICKHOUSE_VERSION`/`CLIENT_VERSION` in the job environment, so CI runs the code defaults that `ClickHouseVersionDefaultUnitTest` pins. Last verified run: `37619434848` on `c61d90ce`, 386 s. The concurrency group is keyed on `github.sha` with `cancel-in-progress`, so pushing a branch and a tag at one SHA produces a cancelled run beside the green one (Active follow-up).
- **CI after v1.7.0_gg1-sync:** `tests.yaml` triggers on branch pushes only, so a tag push starts no run. Before "Run tests" it compiles the integration-test sources and reports the test fork count (`nproc=4`, `maxParallelForks=2` on public runners). Last verified run: `38050860929` on `f328fd21`, 389 s, the only run on that SHA.
- **Research:** each milestone's research is archived with it: `milestones/v1.2.9_gg4-tests-research/` (per-feature test inventories, 10 pitfalls), `milestones/v1.6.0_gg1-sync-research/` (merge strategy, conflict map, upgrade pitfalls) and `milestones/v1.7.0_gg1-sync-research/` (v1.7.0 collision map, compression and lz4 advisories, R-01 routing).
- **Open follow-ups:** eleven items under Requirements → Active. The most urgent is the unpublished `505e4981` fix for the published rollout doc's `TRUNCATE` example. R-01 and the Debezium `Timestamp` gap are both fixed, and no known silent wrong value remains on the metadata-free JSON route. On RowBinary, `Nullable(Enum)` mis-storage (999.1) is the remaining known wrong-value defect.

## Constraints

- **Test target**: ClickHouse `latest` + client V2 only — matches production connectors; older versions lack JSON/Dynamic.
- **Tooling**: Reuse existing JUnit/Mockito/Testcontainers setup, `ClickHouseBase`, and test helpers; only new build dependency expected is the JaCoCo Gradle plugin — keeps the fork close to upstream conventions.
- **Behavior preservation**: Tests pin current fork behavior (including WIP JSON path and not forcing `async_insert`); code changes are limited to genuine bug fixes — the fork runs in production.
- **Environment**: Docker (colima) must be running for ClickHouse-backed tests locally and available on CI runners.
- **Rebasability**: Keep changes additive (new test files/cases, small targeted fixes) to minimize conflicts with future upstream syncs.

**Constraints check at v1.2.9_gg4-tests close** — all five held and all five still apply. Evidence: the JaCoCo plugin was the only build dependency added; `src/main/` changes across the whole milestone total 38 files/+5,742/−117 including tests, with production edits confined to seven targeted bug fixes plus one behavior-preserving `buildMBeanName` extraction; CI now runs `latest` × V2 exclusively. The rebasability constraint is the one to watch — `.planning/` is tracked in this repo (`commit_docs: true`), so an upstream sync will carry 80+ planning files through the merge.

**Constraints check at v1.6.0_gg1-sync close.** All five still apply. Two were stretched on purpose, and the stretch is on the record:

- **Tooling.** The build is upstream's now, not the fork's. Testcontainers 1.21.4 → 2.0.5 and Gradle → 9.6.1 came in with the merge, by recorded decision. The fork added no new build dependency of its own.
- **Behavior preservation.** `src/main` changed beyond bug fixes in exactly one place: D-17's `lz4` default, which keeps the connector working against `latest` at all. Every other `src/main` edit re-applies existing fork behaviour onto upstream's code.
- **Rebasability** paid off and is measurable. Files in the fork delta fell 93→84 wide and 32→30 in `src/main`. Insertions rose 8,401→9,476, mostly tests. The `.planning/` concern from the last close did not bite: SYNC-06's invariant kept planning churn out of the merge commit.

**Constraints check at v1.7.0_gg1-sync close.** All five still apply.

- **Behavior preservation** was the one deliberately crossed. R-01 routing, the nested-null `DataException` and the `Int256` fail-fast change what operators see. Each was decided in advance and recorded in FORK-DELTA and the CHANGELOG. Each is covered by the rollout checklist.
- **Tooling.** No new build dependency was added. Five version bumps were user-approved and each landed as its own catalog commit.
- **Rebasability.** The fork delta grew (84→95 files wide) because hardening and tests sit on top of the merge. The mechanical-merge invariant held again (`54e63648`, zero authored lines), so the next sync starts from the same footing.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| D-14: publish `v1.7.0_gg1` as a non-force `--no-ff` merge of the rehearsed tree X (`ae8520ed`) into `custom_build`; Phase 15 commits after X stay on local `sync/v1.7.0` until the next milestone's merge | One revertible commit (`git revert -m 1 f328fd21`) for the whole sync; a rehearsal run on X proved CI before the protected branch moved; publishing the bookkeeping tail separately would cost a second CI run on `custom_build` | ✓ Phase 15 (user approved verbatim "publish") — `f328fd21`, run `38050860929` green, the only run on that SHA |
| Leave docs-only `505e4981` (15-REVIEW WR-01..03 fixes to ROLLOUT and FORK-DELTA) unpublished after release | Publishing needs a new push to protected `custom_build` and another CI run; the user accepted the published copy keeping the old text for now | ⚠ Phase 15 UAT (2026-10-10, test 1 passed) — the published rollout doc keeps the `TRUNCATE` example until the fix is pushed; tracked under Active |
| D-13: push the scratch branch `sync/v1.7.0` to `origin` once (branch only, explicit refspec, no tags) before Phase 15, amending Phase 12 D-01 | CI-09 needs a measurement on CI hardware and D-11(b) needs to see the new trigger produce exactly one run before publication relies on it; a CI failure is cheapest here | ✓ Phase 14 (user answered "push" 2026-10-10) — `54b5ce2f` pushed, run `38037397969` green, `custom_build` and origin tags untouched; later planning commits stay local until PUB-04 |
| R-01 fixed by **routing** on `shouldSkipTableMetadata()`, unconditional, no opt-out flag; logical types encoded by a private route-scoped `MetadataFreeJsonWriter` (shared `DataJson.OBJECT_MAPPER` untouched) | Throwing would break any pipeline already on explicit JSON behind a Struct converter; a private mapper keeps the upstream JSON paths byte-identical | ✓ Phase 13 (JSONP-05..07, D-05/D-06) — R-01 pin flipped to typed read-back; parity matrix seen red under a Timestamp-encoding mutation. Debezium `io.debezium.time.Timestamp` left as a known gap (follow-up) |
| Nested nulls in non-nullable RowBinary slots: scalar/Tuple **throw**, Array/Map **coerce to empty** | A throw on an empty-able container would break pipelines that legitimately send null lists; a scalar null has no faithful encoding | ✓ Phase 13 (WRT-08, D-09/D-10/D-16) — `Nullable(Enum)`/`Nullable(Tuple)` slots now throw because of the pre-existing `Column` nullability defect (backlog 999.1), accepted as a rollout cost |
| WRT-09's "re-pin" delivered **additively**: three new pins beside the unchanged nullable CR-02 pins, no existing assertion edited | All three CR-02 pins sit on nullable columns and stay correct after #820; the 12-02 plan forbade editing existing assertions, so the literal "re-pin" reading conflicted with the plan | ✓ Phase 12 — accepted at UAT test 5 |
| #800 Avro-union and cluster `buildFromClause` tests accepted as **compile-only** for SYNC-10 | They live in `integrationTest`, are byte-identical to `v1.7.0`, and `compileIntegrationTestJava` passes; running them needs cloud/cluster infrastructure | ✓ Phase 12 — named in the FORK-DELTA skip inventory; accepted at UAT test 7 |
| D-17 retired onto upstream's `setCompressionMethod` (default `true`) instead of keeping the fork's lz4 pin | Upstream #841 covers the same ZSTD break; keeping both re-creates a conflict on every sync. The live probe showed `compress=false` alone suffices on V2 and V1 against 26.9.12.8 | ✓ Phase 12 (CMP-01..03) — FORK-DELTA's load-bearing sentence derived only from Recorded Fact 26; accepted at UAT test 6 |
| ZSTD-default break on ClickHouse 26.9 fixed **in the publishing phase** (D-17) by forcing `network_compression_method=lz4` as a connector default, not by bumping the client | Without it the connector cannot read any response from `latest`, so publishing without the fix would ship a broken connector. Bumping the client is a dependency change outside this milestone; a connector default applied before operator options is the smallest fix and leaves the operator in control | ✓ Phase 11 (plan 11-08, `87fa1e78`) — the only Phase 11 `src/main` change; unit and live pins; removal condition recorded in `FORK-DELTA.md`. **Superseded in Phase 12:** retired onto upstream's `setCompressionMethod` (CMP-01..03) |
| Publish as a `--no-ff` merge into `custom_build`, with the stale FORK-DELTA raw numbers (F-1) fixed in a docs-only commit **before** the merge | A single two-parent merge gives one revertible commit (`git revert -m 1`) for the whole bring-forward. Publishing with numbers the document itself says are pre-D-17 would contradict the published-SHA run | ✓ Phase 11 — user chose "Fix F-1, then publish (Recommended)" at the blocking-human gate; `c61d90ce` pushed without force; the published-SHA CI run covers the final tree |
| CI-06's "exactly one run" locator narrowed to exclude cancelled runs instead of re-running CI | The tag push cancelled the branch run on the same SHA, so the plain locator returned two runs. Re-running CI to manufacture one match would be the run-laundering the plan forbade | ✓ Phase 11 — run `37619434848` is the only non-cancelled match; accepted at UAT test 1. Root cause tracked as an Active follow-up |
| Both `CLICKHOUSE_VERSION` and `CLIENT_VERSION` dropped from the CI job environment | The CI pin is what let Phases 8–9 report green on a server no local run used. Removing it makes `ClickHouseVersionDefaultUnitTest` the single source of truth in both places | ✓ Phase 11 (D-09, CI-05) |
| The shared-container contract **re-derived** over upstream's per-class `@TestInstance(PER_CLASS)` rewrite, rather than taking HARN-03's permitted fallback ("measure the cost, record the fallback") | Upstream rewrote the lifecycle the fork's contract sat on; re-deriving keeps the harness on upstream's shape instead of restoring a pre-merge file that would re-conflict on every future sync. The cost is that 25 classes became order-dependent within a fork for the first time | ✓ Phase 10 (D-01/D-02) — lazily started static container behind `sharedContainerStarts()`, per-class database drop as the only isolation mechanism, `usesSharedContainer()` opt-out for the two Toxiproxy classes |
| The acceptance gate is **two** consecutive clean full-suite runs, not one, with run 2 forced via `--rerun` | Plan 10-01 made 25 classes order-dependent inside a fork and Gradle's fork assignment varies run to run, so one green run proves less here than it did in Phase 9. Without `--rerun`, Gradle's `UP-TO-DATE` cache would have echoed run 1's cached results as a fake second run — defeating the gate's entire purpose | ✓ Phase 10 (D-09) — 654 tests each run, 0 failures, byte-identical 57-class executed sets, skips 18 → 18. Anyone re-running this gate (Phase 11's CI measurement included) must bust the cache explicitly |
| `HARN-04`/`05`/`06`/`07` closed on **re-measured evidence** rather than on new code, and the fact disclosed in `REQUIREMENTS.md` rather than left to read as work performed | All four were already satisfied before the phase began — by the Phase 6 merge and Phase 7's early sliver. Closing them on `10-CONTEXT.md`'s measured-state table would have cited numbers taken before five source files changed and two were added, so every command was re-run against the tree as plan 10-01 left it | ✓ Phase 10 (HARN-04..07) — the `REQUIREMENTS.md:104-110` correction blockquote names the evidence per requirement; confirmed by a human stranger-read at UAT |
| `HARN-07`'s *sequencing* clause ("verified before any of the 371 fork tests are adapted") recorded as **moot on arrival** rather than as formally unmet | The state half (TC `2.0.5` starts a container through `ClickHouseBase`) is robustly true and live-verified; the process half cannot be retroactively performed once the adaptation has happened. The suite had been running green on `2.0.5` since Phase 9, so there was no ordering left to establish | ✓ Phase 10 — flagged by the verifier as defensible but genuinely debatable, and accepted at UAT test 2. Disclosed plainly in the ROADMAP note, the REQUIREMENTS blockquote and the MERGE-CHECKLIST fact rather than hidden |
| **KEEP** the fork's metadata-free `insertFormat=json` bypass rather than retiring it for upstream's `writeRecordsJson` — overriding Phase 7 D-10's inherited "dropped" default | Of the three things the bypass buys, the missing `DESCRIBE`/`SHOW` grant is the one **no combination of upstream configuration reaches**: `suppressTableExistenceException` suppresses the exception but `getTable` still calls `describeTable`; `enableDbTopicSplit` still runs `updateMapping`; `bypassFieldCleanup` covers only the third effect. A deliberate departure from the milestone's "prefer upstream" policy, on the same footing as the JMX carve-out | ✓ Phase 9 (D-01) — decided at the blocking-human checkpoint in discussion, on evidence read from the merged code, before any dependent work; re-applied as four early-return guards behind one `shouldSkipTableMetadata()` predicate. Reversing it later costs a production migration (grants or table pre-creation), not a code revert |
| R-01's silent data loss re-introduced **knowingly** as KEEP's accepted cost, still pinned rather than fixed | Operator fact 2: no live pipeline takes the `Struct` + explicit-JSON route today. A fail-fast throw would break, loudly, any pipeline that does — and it was not pre-approved in Phase 4 either | ✓ Phase 9 (D-01, JSONP-02) — the `PIN - NOT AN ENDORSEMENT` Javadoc kept byte-identical; the fix stays an Active follow-up needing its own plan. **Superseded in Phase 13:** fixed by routing (JSONP-05..07) |
| `jsonInsertColumns` re-attached **server-side** via the surviving 5-arg `client.insert(table, columns, …)` overload, not as a client-side field filter in `writeRecordsJson` | A client-side filter would silently swallow COLS-05's contract (a non-column name throws today, would produce nothing under a filter), and under KEEP's bypass `cleanupExtraFields` returns the payload untouched — so a filter folded in there is a no-op in exactly the mode the feature is most used with | ✓ Phase 9 (D-06/D-07) — `javap` confirmed the overload in `client-v2-0.9.6.jar`; JSONP-03's rationale annotated in REQUIREMENTS.md rather than rewritten |
| The `auto.evolve` guard gap found by code review (CR-01) fixed **now**, not scheduled as a follow-up | Left unguarded, `auto.evolve=true` + explicit `insertFormat=json` reads the synthetic `Table`'s empty column list, treats every field as missing, issues `ALTER TABLE ... ADD COLUMN`, then falls through to a direct `describeTable` — the exact metadata query the bypass exists to avoid, undermining KEEP's own rationale | ✓ Phase 9 — escalated to UAT as the phase's one human-verification item; user chose fix-now. Commit `51baea62` guards the branch with the same shared predicate; 2 new pin tests |
| `TableTest.extractNullables()` carry-in adopted and fixed here, outside `JSONP-01..04` | It turns the acceptance gate from a re-baselined "one known red, zero new" into a clean binary — a re-baselined gate is exactly what lets a second failure hide, the risk Phase 8's D-12 named and had to live with | ✓ Phase 9 (D-14) — `3` → `2` citing DESC-01/HLP-02; taken at the user's explicit decision per STATE.md's out-of-scope rule. MERGE-CHECKLIST Recorded Fact 13 flipped to resolved |
| Land the v1.6.0 merge as two commits — one purely mechanical (every conflicted path resolves to upstream's exact file), one re-applying the fork's build additions | A mechanical commit 1 carries a script-verifiable byte-identity invariant, so each fork behaviour Phases 7–10 re-apply shows up as its own reviewable diff against upstream instead of a judgement call buried in a 500-file merge | ✓ Phase 6 (D-01/D-02) — merge `673d29c3`; 0 of 40 conflicted paths differ from `v1.6.0`; 592-commit range marker-free |
| JMX naming re-plumbed into `SinkTaskStatistics` (upstream's new naming owner) rather than restoring the fork's old `ProxySinkTask.buildMBeanName` home | Upstream moved ObjectName ownership; re-applying the fork's old shape would re-create the conflict on every future sync. Threading the `name` prop to upstream's owner shrinks the fork delta while keeping `JMX-01..04` green | ✓ Phase 8 (ORCH-02/ORCH-03) — task-level `connector=%s` plus a per-topic name composed from it, proven live by `MBeanRegistrationLiveTest` |
| `ProcessingTest.java`'s one forced constructor-arg line accepted as satisfying ORCH-04's "takes upstream's version" rather than treated as a failed byte-identity bar | The single changed line (`new SinkTaskStatistics(null, 0)`) is forced by Phase 8's own constructor change; every assertion and scenario is upstream's. Holding the literal byte-identity reading would block on a self-inflicted, intended edit | ✓ Phase 8 — accepted at UAT test 1; `MERGE-CHECKLIST.md`'s "takes upstream's version wholesale" row wording corrected to name the adaptation |
| Phase 8's acceptance gate re-baselined from "zero failures" to "≤1 failure, 0 errors, every remaining failure and skip named with its owning phase" | The one remaining failure (`TableTest.extractNullables`) is a pre-existing carry-in outside Phase 8's scope; demanding zero would either block the phase or invite an out-of-scope fix to `Table.java` | ✓ Phase 8 — 636 tests, 1 failure, 0 errors, 35 explained skips; per-class greenness additionally asserted for the four classes this phase changed |
| `TableTest.extractNullables()` triaged, not fixed, in Phase 8 | `Table.java` and `TableTest.java` are byte-identical to upstream and out of Phase 8's scope; a read-only triage keeps the ownership honest | ✓ Phase 8 — cause named: `ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally strips Nullable `.null` subcolumns, a fork behaviour already pinned by DESC-01 |
| Unit + integration tests (Testcontainers ClickHouse) | Unit tests alone can't verify real inserts for JSON/Dynamic, `jsonInsertColumns`, server-side `targetTableFilter` | ✓ Phases 1–4 — both tiers shipped; live ClickHouse-backed tests carry the claims unit tests cannot |
| Fix bugs found by tests in the same phase | User wants working features, not just documented failures | ✓ Phase 2 fixed 4 bugs inline (D-01, D-02 ×2, D-03) plus one approved out-of-scope writer fix |
| Test WIP JSON-path features as-is | Pin current behavior; no feature expansion in this project | ✓ Phase 4 — JSON-01..05 and COLS-01..06 pinned as-is; `src/main/` untouched by plans 04-01 and 04-02 |
| `async_insert=0` intentionally not forced | Async insert is controlled via server profile / `clickhouseSettings` | ✓ Phase 4 (ASYNC-01..03) — default config sends no setting, user value passes through, live `async_insert=1` insert lands |
| Test matrix: ClickHouse `latest` × client V2 only | Production uses V2; only latest ClickHouse matters | ✓ Phase 5 — `tests.yaml` rewritten to the single `latest` × V2 job; upstream's V1/23.7/24.3/cloud legs dropped |
| Done = per-feature tests + fully green `./gradlew test` + JaCoCo report + CI | User's definition of done | ✓ Phase 5 — all four met and CI-observed |
| All research differentiators included in v1 (sink-level SMT tests, SMT contracts, RE2 live tests, non-ASCII, container singleton, `testUnit` task, etc.) | User selected every optional test group during scoping | ✓ Phases 1–4 — every selected group shipped |
| Coverage summary rendered by a checked-in stdlib-only script, not a marketplace action | `madrapps/jacoco-report` would add third-party code to the supply chain for markdown formatting the repo can produce itself; the script writes `GITHUB_STEP_SUMMARY` and stdout directly, avoiding a `tee` pipeline that hides failures under `bash -e` without `pipefail` | ✓ Phase 5 (D-06) — `.github/scripts/jacoco_summary.py`, imports `os`/`sys`/`xml.etree.ElementTree` only |
| Pushing `custom_build` to the public fork gated behind a blocking-human checkpoint with a credential scan | Publishing 106 commits including 79 tracked `.planning/` files is one-way; the go/no-go had to rest on a scan of the actual delta, not on the assumption that planning docs hold no secrets | ✓ Phase 5 (D-09) — 4 candidate strings surfaced, all confirmed false positives, then approved |
| CI run located by the pushed commit's own `head_sha` + workflow `path`, asserting exactly one match | "Most recent run on the branch" can attribute an unrelated push's green result to this work | ✓ Phase 5 (DEC-F) — run `35317272594`, exactly one match, verified unauthenticated (no token, no `gh`) |
| `JSON`/`Dynamic` via RowBinary, if broken → explicit clear error (not serialization, not auto-routing) | Smallest rebase-friendly fix; serialization would be new capability | ✓ Phase 3 (TYPE-06) — `doWriteColValue` throws a non-retriable `DataException` naming `insertFormat=json` / `bypassRowBinary=true` before any byte is written |
| The `.object_shared_data.` literal skip clause replaced by a structural "child of a `JSON`/`DYNAMIC` column" rule | The literal matched one ClickHouse-internal name; the structural rule covers every JSON/Dynamic sub-column shape and does not rot with server versions | ✓ Phase 3 (D-07), live-audited against `latest` |
| `describeTable` readers pinned to UTF-8 explicitly rather than relying on the platform default | The test JVM runs `windows-1252` on purpose; the unguarded `InputStreamReader`s mangled non-ASCII column names and `Enum` members — the deferred Phase 1 D-11 bug, reproduced by a test before the fix | ✓ Phase 3 (TYPE-07, D-15) |
| Type coverage asserts typed SQL read-back, never `countRows` | A row-count assertion passes while a `JSON` column holds the wrong nested value or a `Dynamic` column holds `42` as a `String` | ✓ Phase 3 (D-14) |
| `TableOperationFilter` invalid `skipped.operations` (incl. `r`) → pin warn-and-ignore | Avoid breaking existing production connector configs | ✓ Phase 2 (TOF-03, pinned behaviourally with no log assertions) |
| A main-code fix reaching outside `transforms/` stops for a user decision rather than being taken autonomously | Fork runs in production; the phase's declared fix scope is `transforms/`-only | ✓ Phase 2 — plan 02-05 halted on a `ClickHouseWriter` MAP-branch bug; user approved the 3-line fix mirroring the existing ARRAY branch |
| `ClickHouseWriter` MAP branch writes the RowBinary null marker for `Nullable` map values; an actual `null` map value stays unimplemented | Smallest fix that unblocks `Map(K, Nullable(V))` columns; a `writeNull`/`defaultsSupport` path is a larger change deferred out of Phase 2 | ✓ Phase 2 (commit `3929ce6`), null-entry path tracked as an Active follow-up |
| Coverage: report only, no CI threshold | Visibility wanted, not a gate | ✓ JaCoCo is report-only on `test`, no coverage-verification rule (Phase 1) |
| Server-version smoke test asserts the container image reference plus a well-formed reported version, not an image version label | ClickHouse `latest` images carry no usable version label (ClickHouse issue #54224), so label equality is unsatisfiable | ✓ Phase 1 |
| One shared ClickHouse container per test JVM fork (`alwaysPull`), a fresh database per test class, cleanup left to Ryuk (no JVM shutdown hook), default `maxParallelForks` kept | Fast runs with class isolation and no change to test semantics; 0 flakes observed at 6 forks | ✓ Phase 1 |
| Fork coverage view is an explicit list of the 15 fork-changed classes (`X.class` + `X$*.class`) in a separate `jacocoForkReport` | Prefix globs over-match (e.g. `HeaderToValue*` would pull in `HeaderToValueConfig`) | ✓ Phase 1 |
| `testUnit` selects tests via `excludeTags("clickhouse")` and stays out of `check` | Docker-free unit loop without a new source set or changing `check` | ✓ Phase 1 |
| R-01's silent data loss is **pinned, not fixed** — a labelled test asserts the current outcome (no throw, 0 rows) | A fail-fast throw is rated costly and not pre-approved (D-01/D-07): it would break, loudly, any pipeline running `insertFormat=json` behind a Struct converter. Pinning makes the loss suite-detectable so a future rebase cannot change it unnoticed, without touching production behavior | ✓ Phase 4 — escalated at a `blocking-human` checkpoint in plan 04-01; user chose `pin-current-behavior`. Fix tracked as an Active follow-up needing its own plan. **Superseded in Phase 13:** fixed by routing (JSONP-05..07) |
| `buildMBeanName` extracted from `ProxySinkTask.getMBeanName()` as the phase's only production change | `getMBeanName()` is private, reads the client jar's implementation version inline, and is reachable only through a constructor requiring a live ClickHouse — the real jar reports a plain `0.9.6`, so the null-version and colon-version branches could never execute in a test | ✓ Phase 4 (D-02) — +12/−2, 8 of them Javadoc; rendered name byte-for-byte unchanged, independently diff-verified by both the code reviewer and the verifier |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-10-10 after starting v1.7.0_gg2-hardening milestone*
