# ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## What This Is

The `custom_build` branch of `ne1r0n/clickhouse-kafka-connect` (version `v1.7.0_gg4`) is a fork of the official ClickHouse Kafka Connect sink, carrying custom SMTs, table filtering, `Dynamic` column support, a metadata-free JSON insert path, and JMX naming changes used in CDC (Debezium) pipelines into ClickHouse. The first milestone (`v1.2.9_gg4-tests`) audited the existing tests for everything the fork added on top of upstream, wrote the missing unit and integration tests, and fixed the bugs those tests uncovered — delivering a fully green suite, a coverage report, and CI running it on every push.

The second milestone (`v1.6.0_gg1-sync`) cashed that in: 581 commits of upstream (`v1.2.9` → `v1.6.0`) merged into `custom_build` with the suite as the acceptance gate, every fork feature either re-applied onto upstream's rewritten code or consciously retired in favour of upstream, and the result published as `v1.6.0_gg1`. What the fork still patches, and what it gave up, is recorded in `docs/FORK-DELTA.md`.

The third milestone (`v1.7.0_gg1-sync`) repeated the sync for upstream `v1.7.0` and then paid down the fork's open follow-ups on the merged tree. R-01's silent data loss is fixed by routing, nested nulls in RowBinary either round-trip or fail loudly, and D-17 is retired onto upstream's `setCompressionMethod`. Dependencies were bumped one commit at a time and CI now runs once per published SHA. The result is published as `v1.7.0_gg1`, with an operator rollout checklist in `docs/ROLLOUT-v1.7.0_gg1.md`.

The fourth milestone (`v1.7.0_gg2-hardening`) closed the eleven follow-ups `v1.7.0_gg1-sync` left open: `Nullable(Enum)`/`Nullable(Tuple)` values are stored as written, empty Debezium records are skipped on every write route, error messages name the column without carrying record data, and CI is hardened (pinned `ubuntu-24.04` runner, SHA-pinned actions, read-only token, Dependabot config). The result is published as `v1.7.0_gg2` (merge `a246ca01`, 2026-10-14), with its operator note in `docs/ROLLOUT-v1.7.0_gg2.md`.

The fifth milestone (`v1.7.0_gg3-hardening`) closed the last known record-data exposure: a writer value-conversion failure now names the column, both types and the exception class without carrying the record value (backlog 999.3), and retry classification is pinned unchanged. It also hardened `DeclaredNullability` (null-safe, quote-aware, a WARN for an unpaired element), cleared every 17-REVIEW and 19-REVIEW advisory, absorbed the Dependabot bumps and moved the writer tests off Mockito inline mocking. The result is published as `v1.7.0_gg3` (merge `3376a18f`, 2026-10-20), with its operator note in `docs/ROLLOUT-v1.7.0_gg3.md`. Three more review rounds corrected the note after publication, and it was republished as `6a2a87a5` the same day.

The sixth milestone (`v1.7.0_gg4-hardening`) absorbed nine Dependabot merges and upstream's untagged tail, repaired and guarded the Confluent archive under Shadow 9 (it had nested 25 jars into an unusable archive behind a green build), and fixed the two discovery defects `v1.7.0_gg3` disclosed. A Tuple element named `size`, `size0` or `size<digits>` is no longer dropped, and column, element and Enum-label names are matched under the raw identifier written in `CREATE TABLE` on both clients. The writer failure path no longer names a Map key, an unknown Enum label fails with a column-named message, and the carried 21–24 review debt is cleared. It shipped as `v1.7.0_gg4`.

## Core Value

Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## Current Milestone: v1.7.0_gg5-ci CI & Release Workflow Modernization

**Goal:** Bring the fork's workflows current and get a trustworthy integration-test signal, without touching connector code or ClickHouse client `0.10.0`.

**Target features:**
- `pre-release.yaml`: replace the pinned Gradle `7.4.2` (DEPS-F4) with the project wrapper / `setup-gradle`
- `release.yaml`, `pre-release.yaml`, `increment-version.yml`: SHA-pin every action (checkout, setup-java, setup-node, setup-gradle); replace deprecated `gradle-build-action` and archived `create-release` / `upload-release-asset` with `gh release`
- Nightly: leave upstream `integrationTests.yaml` byte-identical and disabled; add a fork-owned nightly running `integrationTest` on ClickHouse `latest` × client V2 in local Docker, with no cloud job
- Verify with a dry run of the release path and green CI on the published SHA

**Out of scope:** ClickHouse client `0.10.0`, connector source changes, editing upstream `integrationTests.yaml`.

## Current State

**Shipped:** `v1.7.0_gg4-hardening` (published 2026-10-23, closed 2026-10-24). It covered 5 phases (25–29), 32 plans, 81 tasks and 36/36 requirements. The milestone audit is `tech_debt` with no gaps: phases 5/5, integration 5/5, flows 5/5. It closed as an `override_closeout`: `init.manager` read Phases 25–28 as stale (later phases edited covered files) and Phase 27 success criterion 2 was narrowed by the user.

`v1.7.0_gg4` is on `origin/custom_build` as the `--no-ff` merge `21c51598` (parents `69cd680b` and `3d847009`), tagged `v1.7.0_gg4`. The rollback tag `pre-v1.7.0_gg4` points at `69cd680b`, and the merge has one green `tests.yaml` run, 40017036598. `sync/v1.7.0` is 6 docs and test commits ahead of `custom_build`.

The suite runs 1,865 tests on ClickHouse `latest` (26.9.19.9) × client V2 and passed twice, with 0 failures and 18 named skips. `testUnit` runs 1,278 tests with Docker stopped. Client `0.9.6` is unchanged. Connect API is `4.4.0` at compile time only: the production worker supplies it at runtime (176 references proven present in the worker's 7.9.7 jars). Shadow is 9.6.1, and the `createConfluentArchive` task is finalized by `verifyConfluentArchive`.

Code: `src/main` is 11,059 LOC and the tests are 38,839 LOC. Since the milestone start `src/main` changed in 12 files (+792/−256), including upstream's tail. The new classes are `DescribeTableBuilder` and `ParentShape`. The fork delta against upstream grew from 130 to 277 files at the wide scope and from 33 to 37 in `src/main`, mostly tests.

**Behaviour changes operators will see** (each in `docs/ROLLOUT-v1.7.0_gg4.md`):
- A Tuple element named `size`, `size0` or `size<digits>` is kept at discovery, so inserts into the 25 affected catalog shapes land instead of failing with code 32 or 33 (or storing a shifted row).
- Column, element and Enum-label names are matched under the raw `CREATE TABLE` identifier. A record that used the old doubled backslash form no longer matches, and a doubled-form Tuple element fails the put loudly.
- A Jackson failure on the JSON-as-string route no longer names a Map key. An unknown or null Enum label fails with a value-free message naming the column.
- The MBean `version=` segment and the client name read `v1.7.0_gg4`. The rollback tree lists `gradlew.bat` as modified on a fresh checkout.

**Knowingly open in the published `v1.7.0_gg4`:** the 16 unfixed review findings in `29-MILESTONE-CLOSE-RECORD.md`, the PUB-12 review-round deviation, and the manual archive compare. See the milestone-close note under Requirements → Active.

<details>
<summary>Previous state — <code>v1.7.0_gg3-hardening</code> (shipped 2026-10-20)</summary>

**Shipped:** `v1.7.0_gg3-hardening` (published 2026-10-20, closed 2026-10-21). It covered 4 phases (21–24), 23 plans, 60 tasks and 20/20 requirements, plus quick task `261020-vcr`. The milestone audit first came back `tech_debt`: seven review findings on the published rollout note were untriaged. It passed on re-audit after the quick task fixed them, with requirements 20/20, phases 4/4, integration 12/12 and flows 5/5. It closed as an override closeout, because all four phase verification reports read as stale: later phases and the quick task edited files those reports covered (`docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt` and the release documents). Each report passed when it was run, and every UAT passed (7/7).

`v1.7.0_gg3` is on `origin` as the `--no-ff` merge `3376a18f` (parents `6473e24f` and X = `8b1c318d`), tagged `v1.7.0_gg3`. The rollback tag `pre-v1.7.0_gg3` points at `6473e24f`, and the merge has one green `tests.yaml` run, 39472696952. `custom_build` on `origin` is now `6a2a87a5`, a docs-only `--no-ff` merge on top that carries the corrected rollout note. It has its own green run, 39496482074, and the tags did not move. Post-publication bookkeeping, including this close, lives on local `sync/v1.7.0`.

The suite runs 1281 tests on ClickHouse `latest` (26.9.19.9) × client V2 and passed twice, with 0 failures, 0 errors and 18 named skips. `testUnit` runs 838 tests with Docker stopped. Client `0.9.6` and Connect API `4.1.1` are unchanged. Lombok 1.18.48, Spotless 8.10.4 and slf4j-reload4j 2.0.20 came in through Dependabot.

Code: `src/main` is 10,523 LOC and the tests are 38,242 LOC (`src/test`, `src/testFixtures` and `src/integrationTest`). This milestone changed `src/main` by +227/−26 lines across 4 files, adding one new file, `WithheldMessageException`. The tests grew by +3,953/−178 lines across 20 files.

**Behaviour changes operators will see** (each in `docs/ROLLOUT-v1.7.0_gg3.md`):
- A writer value-conversion failure now names the column, the Kafka and ClickHouse types and the exception class, without the record value. This applies to the task status, the DLQ stack-trace header and the `Error inserting records` logs; grep for ``Failed to write column ``. There is one measured exception: on the opt-in JSON-as-string route, a Jackson reference chain can name a Map key.
- A `Nullable(Tuple)` whose element name holds a backslash-n or a line break now pairs and stores the row. Such tables encode differently than on `v1.7.0_gg2`, so a rollback needs a clean stop (item 4 of the note).
- A new WARN names a declared Tuple element that has no DESCRIBE sub-column. It marks the pre-existing `size`/`size0` discovery defect (backlog 999.4), fixed on the working branch in Phase 26 and unpublished until `v1.7.0_gg4`.
- The MBean `version=` segment and the client name read `v1.7.0_gg3`.

**Knowingly open in the published `v1.7.0_gg3`:**
- **Backlog 999.4:** a Tuple element named `size` or `size0` is dropped at discovery. The inserts that were run failed with code 32 or 33. Phase 26 measured the pre-fix tree on `latest`: 24 of 25 affected shapes are rejected (23 `CANNOT_READ_ALL_DATA`, one `ATTEMPT_TO_READ_AFTER_EOF`) and the R-01 shape stores a wrong value. Fixed on the working branch (Phase 26), unpublished until `v1.7.0_gg4`.
- **Backlog 999.5:** `fromJsonRow` doubles escaped name characters.

Both are in FORK-DELTA Known costs items 10 and 11 and in the rollout note.

</details>

<details>
<summary>Previous state — <code>v1.7.0_gg2-hardening</code> (shipped 2026-10-14)</summary>

**Shipped:** `v1.7.0_gg2-hardening` (published 2026-10-14, closed 2026-10-16). It covered 5 phases (16–20), 29 plans, 59 tasks and 26/26 requirements. The milestone audit passed: requirements 26/26, integration 4/4, flows 3/3. It still closed as an override closeout, for two reasons. First, the Phase 16–19 verification reports read as stale because Phase 20 later edited files they covered. Each passed when run, and Phase 20 verification (8/8) and UAT (35/35) are current. Second, one deferred item was acknowledged: the Mockito inline mock in `ClickHouseWriterNullMessageUnitTest`, with its intermittent OpenJ9 crash risk.

`v1.7.0_gg2` is on `origin/custom_build` as the two-parent `--no-ff` merge `a246ca01` (parents `be6b8861` and the rehearsed tip `3879b883`). It is tagged `v1.7.0_gg2`, and the rollback tag `pre-v1.7.0_gg2` points at `be6b8861`. The published SHA has exactly one `tests.yaml` run, 38587062864, and it is green. The scratch branch `sync/v1.7.0` is deleted from `origin`. Post-merge bookkeeping, including this close, lives on local `sync/v1.7.0`. After publication the user switched the fork's GitHub default branch to `custom_build` and enabled Dependabot version updates.

The suite runs 988 tests on ClickHouse `latest` (26.9.18.11) × client V2 and passed twice: 0 failures, 0 errors, and the same 18 named skips. `testUnit` runs 556 tests with Docker stopped. Client `0.9.6`, Connect API `4.1.1` and the version catalog are unchanged; this milestone bumped no library.

Code: `src/main` 10,322 LOC; tests 34,467 LOC (`src/test` 28,360, `src/testFixtures` 4,329, `src/integrationTest` 1,778). The fork delta against upstream grew from 95 to 119 files at the wide scope and from 31 to 32 in `src/main`. Most of the growth is tests, and `src/main` gained one file, `DeclaredNullability` (`docs/FORK-DELTA.md`).

**Behaviour changes operators will see** (each in `docs/ROLLOUT-v1.7.0_gg2.md`):
- `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` values are now stored as written. Rows stored wrongly by earlier releases are not repaired; the note gives five detection queries.
- A single-field `Nullable(Tuple(a T))` with a null field now fails with a `DataException` (ledger L-38).
- Empty Debezium records (`op=t`, or null `before` and `after`) insert no row on any write route.
- A batch left `BEFORE_PROCESSING` by the previous release retries with the same deduplication token. On deduplicating engines it is dropped even if its bytes changed, so old rows persist when the earlier attempt landed.
- Writer errors and DateTime parse logs name the column without the raw value. A rethrown message can still carry one (backlog 999.3).
- The MBean `version=` segment and the client name change with the version.

**Knowingly open in the published `v1.7.0_gg2`:** backlog 999.3, record values in rethrown write-failure messages. No known wrong-value defect remains in the writer.

</details>

<details>
<summary>Previous state — <code>v1.7.0_gg1-sync</code> (shipped 2026-10-10)</summary>

**Shipped:** `v1.7.0_gg1-sync` (2026-10-10). It covered 4 phases (12–15), 25 plans, 55 tasks and 39/39 requirements. It closed as an override closeout for the same two reasons as the previous milestone. First, no milestone audit was run. Second, the Phase 12 and 13 verification reports read as stale because Phases 13–15 later edited the files they covered. Both verifications passed when run. Phases 14 and 15 verification passed on the final tree, and Phase 15 UAT was 1/1.

The release is the two-parent `--no-ff` merge `f328fd21` (parents `c61d90ce`, `ae8520ed`), tagged `v1.7.0_gg1`. The rollback tag `pre-v1.7.0-sync` points at `c61d90ce`. Phase 16 published two docs-only `--no-ff` merges on top of it, with no tag. `537beb1f` (2026-10-10) carries the corrected rollout doc (`505e4981`, IN-07..IN-10). `be6b8861` (2026-10-11) carries the corrected item 8 rollback steps. `custom_build` on `origin` is `be6b8861`, and its own `tests.yaml` run, 38156040416, is green.

The suite runs 830 tests on ClickHouse `latest` (26.9.14.10) × client V2 and passed twice: 0 failures, 0 errors, and the same 18 named skips. `testUnit` runs 454 tests with Docker stopped. CI run `38050860929` on the published SHA is green in 389 s and is the only run on that SHA. Client `0.9.6` and Connect API `4.1.1` are unchanged. lz4-java is `1.12.0`, Jackson `2.22.3`, okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0`.

Code: `src/main` 10,119 LOC; tests 29,523 LOC (`src/test` 23,416, `src/testFixtures` 4,329, `src/integrationTest` 1,778). The fork delta against upstream grew from 84 to 95 files at the wide scope and from 30 to 31 in `src/main`. The growth is the hardening, tests and CI added on top of the merge, not unretired debt (`docs/FORK-DELTA.md`).

**Behaviour changes operators will see:**
- `Struct`/CDC records under `insertFormat=json` now land instead of being dropped, so exactly-once pipelines need a backfill.
- A nested null in a non-nullable scalar or Tuple slot now throws a `DataException`, which can be routed to the DLQ.
- `Int256`/`UInt256` columns fail fast.
- Compression follows upstream's flag.
- The MBean `version=` segment changes.

Each is covered in the rollout checklist. **Knowingly open in the published `v1.7.0_gg1`:** `Nullable(Enum8|Enum16|Tuple)` columns are mis-mapped (backlog 999.1, warned at discovery), and the empty-CDC skip covers only the explicit JSON route. Both fixes — the empty-CDC skip on all four routes (Phase 18, JSONP-09) and 999.1 closed with `Nullable(Enum)`/`Nullable(Tuple)` round-tripping and the discovery WARN deleted (Phase 19, COLS-08/COLS-10) — are now published in `v1.7.0_gg2` (Phase 20, merge `a246ca01`).

</details>

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

## Next Milestone Goals

Not defined yet: `/gsd-new-milestone` starts with questioning. Candidates:

- **The next upstream sync,** once upstream tags past `v1.7.0`.
- **The carried review advisories** in `milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md`: 25-REVIEW WR-01..WR-03 and IN-01..IN-04 (pin guard, `ConfluentArchiveCheck`, `ConnectApiReferencesUnitTest`) and the 26–28 findings. Each has an owner and a re-open trigger.
- **A CI consumer for the archive compare.** `confluent-archive-allowlist-29.txt` is used only by a manual release-time step today.
- **A test for a size-named Tuple element whose name also holds an escape character.**
- **Still Future Requirements:** ClickHouse client `0.10.0` (PR #4), Gradle `7.4.2` in `pre-release.yaml` (DEPS-F4), and the upstream workflows still on `@v3`-era actions.

<details>
<summary>Milestone scope as planned — <code>v1.7.0_gg4-hardening</code> Dependency Absorption, Discovery Defects &amp; Review Debt (shipped 2026-10-23)</summary>


**Goal:** Absorb the Dependabot majors and minors now on `origin/custom_build` plus upstream's untagged dependency tail, fix the two known discovery defects (backlogs 999.4 and 999.5), clear every review advisory carried from `v1.7.0_gg3-hardening`, and publish `v1.7.0_gg4` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Target features:**

- **Dependency absorption (first).** A DEPS-01-shaped merge of `origin/custom_build` (`d20bae63`, 15 commits past the published `6a2a87a5`) into the working branch, plus PR #10 (protobuf-java 3.25.9 → 4.36.2, testFixtures only) once the user merges it. The suite gates the two majors that reach production code: Kafka `connect-api` 4.1.1 → 4.4.0 (#9, a fork-only pin) and the Shadow plugin 8.3.5 → 9.6.1 (#11). The rest are Gradle wrapper 9.8.1 (#8), commons-lang3 3.21.0 (#6), ben-manes versions 0.66.0 (#7), gson 2.14.0 (#13) and httpclient5 5.6.5 (#12). FORK-DELTA's pin rows and the fork-anchors invariant follow. The client stays `0.9.6`, and PR #4 stays open and unignored.
- **Tagless upstream mini-sync.** `upstream/main` is 12 commits past `v1.7.0` with no new tag: an explicit guava dependency that closes a CVE (`30521f74`), an lz4-java bump, httpclient5 5.6.5, and the four bumps the fork already took in Phase 14. Merge it in the usual sync shape (scratch branch, rollback tag, `verify-merge-invariants.sh`, two full runs), so the next tagged sync starts small and the guava CVE is closed on the record.
- **Backlog 999.4.** `shouldSkipDescribeRow` keeps a declared Tuple element named `size` or `size0` while still dropping the structural rows (`c.name.size`, `d.null`, `e.size0` under `Array(Tuple)`), with a live round-trip pin, the DESC-05 regression check, a FORK-DELTA row, an anchors row, and the D-04 WARN pin flipped. 23-REVIEW WR-02 lands with it: the pin stops exercising a copy of the DESCRIBE loop.
- **Backlog 999.5.** Decide the intended name form for a backslash in a column or element name, fix `ClickHouseFieldDescriptor.fromJsonRow`, pin both forms live, and write the rollout item, since an operator's Kafka field names may depend on the doubled form. 23-REVIEW WR-01 (the escaped-quote `normalise` pin) lands with it.
- **Carried review advisories, all 17** (`milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md`): 21-REVIEW WR-01..WR-03 and IN-01, IN-02, IN-04 (pin-guard robustness, slf4j binding drift); 22-REVIEW WR-01 (exclude `JsonProcessingException` from the `IOException` pass-through, with a sentinel Map-key test) and IN-01..IN-05; 23-REVIEW IN-01..IN-03. Plus the two other carried items: the `jacocoForkReport` class list (add `DeclaredNullability` and `WithheldMessageException`) and 24-REVIEW IN-03 (planning identifiers in a live test).
- **Operator and docs items.** The user creates the `dependencies` label in the GitHub UI (D-11); the milestone verifies the Dependabot "labels could not be found" comment stops. The published `docs/ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` get the corrected `enableDbTopicSplit=true` key rule and state-table query. The stale local ref `refs/upstream/main` (at the v1.6.0 merge) is deleted, so `upstream/main` stops being ambiguous.
- **Release.** `VERSION=v1.7.0_gg4`, CHANGELOG, FORK-DELTA, a rollout note (the 999.4 and 999.5 behaviour changes, the `connect-api` major, the guava addition), an independent `gsd-code-reviewer` pass on the operator documents before the publication gate, a `--no-ff` merge into `custom_build` after human approval, CI green on the published SHA, and the tag pushed after that run is green.

**Baseline at milestone start (2026-10-21):**
- `origin/custom_build` = `d20bae63`: the published `6a2a87a5` plus seven Dependabot merges the user made on 2026-10-20 and 2026-10-21 (#8, #9, #6, #7, #13, #12, #11). CI is green on the first four; the runs for `55845c3a`, `4b9ddf96` and `d20bae63` were in progress at 06:26 UTC.
- Local `sync/v1.7.0` carries the `v1.7.0_gg3-hardening` close (4 commits past `6a2a87a5`) and none of the Dependabot commits, so the two have diverged (14 and 4 by first parent before the #11 merge).
- `upstream/main` = `a6f1df83`, 12 commits past `v1.7.0` (`379b8d01`), no new tag.
- Dependabot PRs #4 (client 0.10.0) and #10 (protobuf-java 4.36.2) are open. #5 (JUnit 6.1.3) was closed unmerged on 2026-10-16.
- The nightly `integrationTests.yaml` is still disabled in settings. No seeds, no pending todos.

**Out of this milestone:**
- ClickHouse client `0.10.0`: PR #4 is left open, unmerged and not ignored, and the client stays `0.9.6` (the user's call, 2026-10-21).
- A tagged upstream sync: there is no tag past `v1.7.0`.
- New connector features, and running the upgrade on production connectors.

</details>

<details>
<summary>Milestone scope as planned — <code>v1.7.0_gg3-hardening</code> Record-Data Exposure & Review Debt (shipped 2026-10-20)</summary>

**Goal:** Close the last known record-data exposure (backlog 999.3), clear every review advisory carried from `v1.7.0_gg2-hardening`, and absorb the Dependabot bumps already on `origin/custom_build`. Then publish `v1.7.0_gg3` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Target features:**

- **999.3, record values in rethrown write failures:**
  - A `doWriteColValue` failure stops carrying the record value into the Connect task status, the DLQ headers and the two `Error inserting records` ERROR logs (T-18-07).
  - Retriable/non-retriable classification stays exactly as it is, pinned both ways.
  - This closes FORK-DELTA Known costs item 6.
- **19-REVIEW advisories** (carried under D-08):
  - WR-01: `DeclaredNullability.apply`/`containsTuple` become null-safe.
  - WR-02: the vacuous WARN-content assertions use a column name that cannot match the fixed text.
  - IN-01: the backslash-n literal is shared, not copied.
  - IN-02: brittle and indirect pins are fixed.
  - IN-03: `normalise` no longer rewrites backslash-n inside names and enum labels.
  - IN-04: a Tuple Column that lacks a declared element no longer "applies".
- **17-REVIEW advisories:**
  - IN-01: the pin guard covers the workflow's use of `reportTestForks`.
  - IN-02: the `RUNS_ON` and `WRITE_SCOPE` regexes are fixed.
  - IN-03: the `Lz4RoundTripLiveTest` jar-origin duplicate stops growing. The live test is not deleted or weakened.
- **Test-harness risk.** `ClickHouseWriterNullMessageUnitTest` drops the Mockito inline mock of `ClickHouseHelperClient` and overrides one level up instead (the `CapturingClickHouseWriter` pattern). This removes the intermittent OpenJ9 `retransformClasses` crash risk.
- **Dependabot integration.** The three Dependabot merges on `origin/custom_build` are merged into the working branch, and the suite stays green on them: lombok 1.18.48 (#1), spotless 8.10.4 (#2) and slf4j-reload4j 2.0.20 (#3), tip `6473e24f`.
- **Operator checks (user):**
  - Disable the nightly `integrationTests.yaml` in the repository's Actions settings. The file stays byte-identical to upstream; the milestone records the change and verifies that no scheduled run follows.
  - Confirm that the Dependabot `gradle` entry shows a recent "Last checked". This closes the residue of 17-REVIEW WR-01.
  - Revoke the read-only GitHub token and delete the Keychain item `gsd17-gh-actions-read`.
- **Release:**
  - `VERSION=v1.7.0_gg3`, CHANGELOG, FORK-DELTA and a tag.
  - A rollout note for the changed error and DLQ message text.
  - A `--no-ff` merge into `custom_build` after human approval, with CI green on the published SHA.

**Baseline at milestone start (2026-10-16):**
- `origin/custom_build` = `6473e24f`, which is `a246ca01` (`v1.7.0_gg2`) plus three Dependabot merges the user made on 2026-10-16. CI is green on each.
- Local `sync/v1.7.0` carries the post-publication bookkeeping and the gg2 close past `3879b883`. It does not contain the Dependabot commits, so the two have diverged.
- `upstream/main` = `v1.7.0` (`379b8d01`) with 0 commits past the tag, so there is no sync this milestone.
- Dependabot PR #4 (`clickHouseDriver` 0.9.6 → 0.10.0) is open.
- The nightly `integrationTests.yaml` failed all 31 jobs on 2026-10-16 (run 38881153720), including the standalone `latest` × V2 jobs.
- No seeds, no pending todos.

**Out of this milestone:**
- The next upstream sync: nothing past `v1.7.0` to sync.
- ClickHouse client `0.10.0`: PR #4 is left open, unmerged and not ignored, and the client stays `0.9.6`.
- Making the nightly `integrationTests.yaml` pass, or editing or deleting it: the user disables it in settings instead.
- New connector features.
- Running the upgrade on production connectors.

</details>

<details>
<summary>Milestone scope as planned — <code>v1.7.0_gg2-hardening</code> Follow-up Hardening & CI Hygiene (shipped 2026-10-14)</summary>

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

**Outcome:** all 26 requirements shipped, and all 11 carried follow-ups are closed. The Tuple spike answered `go`, so the `Nullable(Tuple)` half of 999.1 shipped with the Enum half instead of becoming a Known cost. Review gap closure added the `SimpleAggregateFunction(f, Nullable(T))` case (19-07..19-10). The rollout work corrected the gg1 note's deduplication-token claim from a live pin (`DedupTokenRetryLiveTest`). One new fork-side defect surfaced and was deferred rather than fixed: backlog 999.3. The five 19-REVIEW advisories were deferred under D-08, so no code changed between Phase 19's green gate and the release.

</details>

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
- ✓ The 15-REVIEW docs findings are fixed and reviewed before the gate. Rollout item 2's recovery steps are qualified by `exactlyOnce`, with no `TRUNCATE` of the shared state table. Item 7 gives regex-safe `version=` matchers. FORK-DELTA's grep commands print totals. CHANGELOG and ROLLOUT entries are in plain language. — Phase 16 (PUB-05)
- ✓ The corrected rollout doc is on `origin/custom_build`, through docs-only non-force `--no-ff` merges approved by the user. `537beb1f` has one green run, 38074050935. `be6b8861` has one green run, 38156040416, and carries the item 8 rollback that works on the published tip: `git read-tree -u --reset pre-v1.7.0-sync` from a clean, fast-forwarded checkout, because `git revert -m 1` hits 97 conflicts once docs merges sit on top of the release. — Phase 16 (PUB-06, 16-REVIEW WR-01/WR-02)
- ✓ Operator rollout checklist `docs/ROLLOUT-v1.7.0_gg1.md`: 8 items, each with what changed, an "Am I affected?" self-check and an action — MBean `version=` rename, compression and `compress=true` caveat with the server-profile check, R-01 behaviour change with the exactly-once backfill, JSON-route type notes (incl. L-08 `input_format_null_as_default`), nested-null `DataException`, rollback tag `pre-v1.7.0-sync` — Phase 15 (ROLL-01)
- ✓ `tests.yaml` hardened: all four actions pinned to 40-hex commits with version comments (Node 24 majors; `gradle/actions` on the peeled commit), runner pinned to `ubuntu-24.04`, a top-level `permissions: contents: read` with `persist-credentials: false`, and the fork-count step reads Gradle's own value from a new `reportTestForks` task; `TestsWorkflowPinGuardUnitTest` pins it Docker-free; a grouped monthly `github-actions` Dependabot entry; `test-head.yaml` deleted — Phase 17 (CI-11..CI-16)
- ✓ Phase 14 build/test debt closed: duplicate `org.lz4:lz4-java:1.8.0` excluded from `testRuntimeClasspath` only (other classpaths byte-identical), `Lz4RoundTripLiveTest` asserts LZ4 on the response leg (seen red with response compression off), `lombok.config` selects Jackson 2 for `@Jacksonized` (warning gone, bytecode identical), NanoTimestamp mantissa-with-fraction rows pinned — Phase 17 (DEBT-06, DEBT-07, BLD-14, DEBT-08)
- ✓ CI-17 proven by one approved scratch push of `0c90d331`: run `38172811430` green on `ubuntu-24.04` (one run, attempt 1), fork-count notice `nproc=4, Gradle maxParallelForks=2`, no Node 20 / Lombok / `ubuntu-latest` annotation, token `Contents: read` + `Metadata: read`, all four action SHAs downloaded as pinned; scratch branch deleted after green; local gate 840 tests / 0 failures / 18 named skips on `latest` 26.9.16.11 — Phase 17 (CI-17)
- ✓ Empty Debezium records (`op=t`, or null `before` and `after`) insert no row on all four write routes: explicit JSON, `bypassRowBinary=true` JSON, and RowBinary with and without defaults. One shared `skipEmptyCdcRecord` in the writer serialization loops decides it, so offsets, exactly-once state and the dedup token still cover the whole batch. Pinned live on clients V1 and V2 plus 24 Docker-free byte/token/state pins, mutation-checked — Phase 18 (JSONP-09)
- ✓ Writer error path. A missing non-nullable value, including inside `Array(Tuple)`, throws a data-free `DataException` naming the column and its type. A message-less failure is rethrown as itself, and a null `IOException` message no longer classifies as a timeout; retry classification is pinned both ways. DateTime/DateTime64 parse-failure ERROR logs carry column, types, exception class and value length only, pinned by `WriterLogMaskingUnitTest` (T-13-19) — Phase 18 (WRT-10, WRT-11, WRT-12)
- ✓ Backlog 999.1 closed. `Nullable(Enum8|Enum16)` round-trips top-level and nested through `.isNullable(isNull)` in `Column.extractColumn`. After a live twin-table spike answered `go` (`19-SPIKE.md`), `Nullable(Tuple(...))` and `Array(Nullable(Tuple(...)))` round-trip with element nullability taken from the declared type string. `DeclaredNullability` does this, is total, fails open per root and unwraps `SimpleAggregateFunction`. The discovery WARN, its anchor row and its FORK-DELTA rows were removed in the same commit. 15-REVIEW IN-01..IN-05 were closed against the fix — Phase 19 (COLS-08..COLS-11, DEBT-09)
- ✓ `docs/ROLLOUT-v1.7.0_gg2.md` adds six delta items over the gg1 note:
  - in-flight batches and the deduplication token
  - `Nullable` Enum/Tuple values, with five detection queries
  - empty Debezium records
  - messages and log lines
  - MBean and client name
  - rollback to `pre-v1.7.0_gg2`

  Every quoted query was run live. The dedup-token consequence is pinned by `DedupTokenRetryLiveTest` (8 cases on 26.9.18.11) — Phase 20 (ROLL-02)
- ✓ `VERSION` = `v1.7.0_gg2`, with a plain-language `1.7.0_gg2` CHANGELOG section. FORK-DELTA is refreshed with git-derived raw numbers and has a row for every new fork-side patch — Phase 20 (PUB-07)
- ✓ The frozen tree passed the full suite twice on `latest` 26.9.18.11 × V2: 988 tests, 0 failures, 0 errors, and 18 named skips equal to FORK-DELTA's inventory. `testUnit` ran 556 tests with Docker stopped, and the anchors invariant passed — Phase 20 (SUITE-05)
- ✓ `v1.7.0_gg2` published. Rehearsal run 38578966373 was green on the exact tip. `custom_build` on `origin` is the non-force `--no-ff` merge `a246ca01`, approved verbatim "publish". The rollback tag `pre-v1.7.0_gg2` (`be6b8861`, which already carries the branch-only trigger) was pushed first. The published SHA had exactly one green `tests.yaml` run (38587062864) before the `v1.7.0_gg2` tag went up, and the scratch branch was deleted last — Phase 20 (PUB-08)
- ✓ The three Dependabot merges on `origin/custom_build` (`6473e24f`: lombok 1.18.48, spotless 8.10.4, slf4j-reload4j 2.0.20) are in `sync/v1.7.0` through the first-parent merge `82524e12`, with no rebase, force or push. `testUnit` is green, there is no Lombok Ambiguous warning, and spotless is baseline-equal (46 files, no tree-wide reformat, D-14). The one anchor violation that #3 introduced is forgiven by a single full-SHA, path- and pattern-pinned row in `fork-anchor-exemptions.txt`, which only invariant 3's count-mismatch branch consults. Self-test ST5-ST9 fail on the old gate and on a SHA-only mutant — Phase 21 (DEPS-01)
- ✓ `TestsWorkflowPinGuardUnitTest` goes red if the "Report test fork count" step stops calling `./gradlew ... reportTestForks` or the task registration disappears. It accepts `runs-on: ubuntu-24.04 # comment` and no longer reads step `name:`/`run:` text containing `: write` as a write scope. Each case was seen red on a fixture first (26 tests); the narrowed accept set is tabled in E-11 (D-10) — Phase 21 (CI-18, CI-19)
- ✓ Operator checks done by the user in the web UI: the nightly `integrationTests.yaml` is disabled (2026-10-16T09:38:23Z, file still upstream-identical, 0 scheduled runs after it); Dependabot's `gradle` entry runs with no configuration error (job 1636257648 on 2026-10-19 opened PRs #6-#9; the tab no longer shows a "Last checked" field); the read-only token is revoked and the Keychain item is gone — Phase 21 (OPS-01, OPS-02, OPS-03)
- ✓ A `doWriteColValue` value-conversion failure no longer carries the record value. The generic catch throws a `DataException` ``Failed to write column `<name>` ...`` naming the column, the Kafka and ClickHouse types and the exception class, with a `WithheldMessageException` cause (frames kept, messages withheld, no suppressed). IO chains, `ConnectException` and exact `RuntimeException` pass through (D-01); the one value-carrying exact site, `Column.convertEnumValues`, now throws fixed text (D-14). A record value can no longer trigger the V1 mapping refresh (D-04). The DLQ `exception.message` header stays the queryId text and the column, type and class are in `exception.stacktrace` (D-15). Sentinel tests were seen red before the single twelve-file `fix(22-03)` commit `c713a837` and are green after, Docker-free on V1/V2 and live on `latest` 26.9.19.9 × V2 — Phase 22 (WRT-13, WRT-15)
- ✓ Retry classification is unchanged: the 184-case `WriterFailureClassificationUnitTest` (21 failure kinds through the real writer, `ProxySinkTask` and `ChunkFlusher`) was green on the unchanged writer and stays byte-identical and green after the fix — Phase 22 (WRT-14)
- ✓ `ClickHouseWriterNullMessageUnitTest` no longer uses Mockito: a hand-written `CountingHelperClient` subclass replaces the inline mock, all five WRT-11 cases and their assertions are kept, and the Phase 18 OpenJ9 `retransformClasses` item is closed (F7 reading accepted at 22-UAT) — Phase 22 (HARN-08)
- ✓ `DeclaredNullability.apply` and `containsTuple` are total: a null root, or a Column with a null type at any depth, gives one metadata-only `(structure mismatch)` WARN and leaves the root unchanged instead of throwing. The D-08 flip keeps its method and null-type input under a scoped AGENTS.md exemption. Pins were red on the unchanged code (11/38) and green after the `fix(23-02)` commit `4bbc9627` — Phase 23 (COLS-12)
- ✓ `normalise` is one quote-aware pass, so a literal backslash-n inside a backtick-quoted Tuple element name or an enum label pairs instead of falling back; only the server's line break becomes a space. A live premise compares every captured `*_RAW` literal with `latest` DESCRIBE text (D-13 not triggered on 26.9.19.9). Red before and green after `fix(23-04)` `f55165ee`, Docker-free and live — Phase 23 (COLS-13)
- ✓ An unpaired declared Tuple element logs one metadata-only `UNPAIRED_ELEMENT_WARNING`, at every DESCRIBE, and the declared nullability still applies to the elements that paired (D-01, D-02). The three colliding tests were flipped under D-19 (Option B) with names and assertions kept; the 999.4 `size`/`size0` drop is pinned through the real `shouldSkipDescribeRow` (D-04). `fix(23-05)` `f83ac147` — Phase 23 (COLS-14)
- ✓ The fallback-WARN content assertions name the column through a sentinel root and the delimited `column <name> of table` segment, so a dropped column argument or placeholder fails them: 14/14 red under both D-17 mutations, then reverted — Phase 23 (DEBT-10)
- ✓ `DeclaredNullability.ESCAPED_NEWLINE` is the only backslash-n literal under `src`; the three test copies, the `NullableRoundTrip` pass-through and the mid-class test constants are gone — Phase 23 (DEBT-11)
- ✓ Every 17-REVIEW and 19-REVIEW finding is `fixed` or closed with a written reason and a commit pointer (17 IN-03 `skipped` with the roadmap's reason); the DEBT-12 checker reports `errors: []` and both archived dispositions read `open: 0` — Phase 23 (DEBT-12)
- ✓ `docs/ROLLOUT-v1.7.0_gg3.md` gives operators seven items, each with What changed, Am I affected? and What to do, and every quoted query and command comes from a Phase 24 transcript on `latest` 26.9.19.9 × V2. It covers the value-free error text with the measured Jackson Map-key exception (D-04 branch 1, pinned by `JsonAsStringKeyResidualLiveTest`), the `size`/`size0` defect (codes 32 or 33, no workaround), the backslash-n and line-break shapes that encode differently (14 of 44 fixture tables, D-09), and the clean stop a rollback needs for that one shape (D-15, KeeperMap in-flight query). Two pre-gate review rounds ran before the suite gate. After publication, three more independent rounds found 14 findings. Quick task 261020-vcr fixed them and republished the note as `6a2a87a5` (run 39496482074, green) — Phase 24 (ROLL-03), quick 261020-vcr
- ✓ `VERSION` is `v1.7.0_gg3`, committed with its anchor row, and the MBean `version=` segment and client name read it on a live task. `CHANGELOG.md` has a plain-language `1.7.0_gg3` section, and `docs/FORK-DELTA.md` has a row for every Phase 21–23 patch (no uncovered IDs) plus Known costs 6, 10 and 11 — Phase 24 (PUB-09)
- ✓ The frozen tree is green twice on `latest` 26.9.19.9 × V2, the second run with `--rerun`: 0 failures, 0 errors, 18 named skips. `testUnit` is green with Docker stopped, and the anchors invariant passes — Phase 24 (SUITE-06)
- ✓ `v1.7.0_gg3` is published. `origin/custom_build` = `3376a18f`, a non-force `--no-ff` merge of `sync/v1.7.0` (X = `8b1c318d`, rehearsed at X0 = `3ab55e1d`) onto `6473e24f`, made after the user's own direct `push` and `publish` answers (confirmed at 24-UAT test 2). The SHA's one `tests.yaml` run, 39472696952, is green. `pre-v1.7.0_gg3` (`6473e24f`) went out first, and `v1.7.0_gg3` only after the run was green — Phase 24 (PUB-10)
- ✓ Backlog 999.4 is fixed on the working branch (published in `v1.7.0_gg4`): a declared Tuple element named `size`, `size0` or `size<digits>` is kept at both discovery drop sites (`DescribeTableBuilder.decide`, `Table.updateParent`) while every structural DESCRIBE row is still dropped; the V1 and V2 DESCRIBE loops share one `DescribeTableBuilder`; 47 committed DESCRIBE fixtures plus a live drift premise pin the server's rows; the live round-trip of 46 shapes is green with zero WARN on `latest` x V2 and fails exactly the 25 measured keys on the pre-fix tree; FORK-DELTA Known costs item 10 is retired — Phase 26 (DESC-06..DESC-10)
- ✓ 23-REVIEW WR-02 is closed: `discoverAsDescribeTableV2` is deleted and the 999.4 pin drives the production `DescribeTableBuilder` with raw JSON lines — Phase 26 (DESC-07)
- ✓ The pin guard reads `build.gradle.kts` with a one-pass scanner, and the guard's other robustness items are fixed (DEBT-13, DEBT-14) — Phase 25
- ✓ Nine Dependabot merges and upstream's untagged tail are absorbed through two merges behind the unedited anchors gate; `kafka = "4.4.0"` is compile-time only and proven against the production worker's jars (DEPS-02, DEPS-03, SYNC-11, SYNC-12) — Phase 25
- ✓ The Confluent archive is repaired under Shadow 9 and finalized by `verifyConfluentArchive`, which fails the build on a nested jar or a bundled Kafka API class; CI builds the archive before the tests (BLD-15, BLD-16, CI-20) — Phase 25
- ✓ `slf4j-reload4j` lives in the catalog with its anchor, guava and httpclient5 are recorded with advisories, and the `dependencies` label exists (DEBT-15, OPS-04) — Phase 25
- ✓ A Jackson failure on the JSON-as-string route is withheld link by link, while a timeout beneath it stays retriable (WRT-17) — Phase 27
- ✓ An unknown or null Enum label throws a value-free `IllegalArgumentException` naming the column, and the write-failure texts are built from one prefix constant (WRT-18, WRT-19) — Phase 27
- ✓ The Phase 22–24 test debt is cleared: `schemaWithFixedStringMismatchTest` fails when `put` does not throw, the dead counter is gone, and the `NullableTupleMatrixLiveTest` premises fail with a message (DEBT-16) — Phase 27
- ✓ Backlog 999.5 is fixed: column, element and Enum-label names are matched under the raw `CREATE TABLE` identifier on both clients, with `normalise` rewritten and the `normalise` pins landed first (NAME-01..NAME-07) — Phase 28
- ✓ `jacocoForkReport` lists all 21 fork classes, and a Docker-free guard keeps its two copies equal (DEBT-17) — Phase 28
- ✓ `v1.7.0_gg4` is published: `custom_build` = `21c51598`, tags `v1.7.0_gg4` and `pre-v1.7.0_gg4` on `origin`, one green CI run, and `docs/ROLLOUT-v1.7.0_gg4.md` reviewed (four rounds; the fourth was user-authorised) — Phase 29

### Active

<!-- Milestone `v1.7.0_gg4-hardening` is active (started 2026-10-21). Every open `[ ]` item below is in its scope except client 0.10.0; its REQ-IDs are in `REQUIREMENTS.md`. Struck-through items are history. -->

**v1.6.0_gg1-sync:** all 53 requirements shipped and moved to Validated.

**v1.7.0_gg1-sync:** all 39 requirements shipped and moved to Validated (archive: `milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`). All 17 follow-ups carried in are closed. The open items below emerged during this milestone.

**v1.7.0_gg2-hardening:** all 26 requirements shipped and moved to Validated (archive: `milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md`). All 11 follow-ups carried in are closed. Backlog 999.3 and the milestone-close follow-ups below are open.

**v1.7.0_gg3-hardening:** all 20 requirements shipped and moved to Validated (archive: `milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md`). All ten follow-ups carried in are closed. The open items below emerged during this milestone.

**v1.7.0_gg4-hardening:** all 36 requirements shipped and moved to Validated (archive: `milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md`). The ten follow-ups carried in are closed except the review advisories listed in the close note below.

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
- ~~The empty-CDC-record skip (op=`t`, null before/after) applies only to the explicit `insertFormat=json` route; the upstream `bypassRowBinary=true` JSON route still writes a `{}` all-defaults row (13-REVIEW WR-01, ledger L-03)~~ **Closed in Phase 18 (JSONP-09)** — one shared `skipEmptyCdcRecord` in both writer serialization loops covers all four routes (explicit-json, bypass-json, rowbinary-defaults, rowbinary-plain), pinned live on clients V1 and V2 plus 24 Docker-free byte/exactly-once pins
- ~~`Column.extractColumn` drops `Nullable` for `Enum8`/`Enum16` and `Tuple`, so values are mis-stored and the new nested-null guards throw on legal `Nullable(Enum)`/`Nullable(Tuple)` slots — ROADMAP backlog 999.1 (13-REVIEW WR-05, ledger L-13)~~ **Closed in Phase 19 (COLS-08, COLS-10)** — `Nullable(Enum8/16)` keeps its flag via `.isNullable(isNull)` in `Column.extractColumn`; Tuple element nullability comes from the declared type string via `DeclaredNullability` (incl. `SimpleAggregateFunction` unwrap, 19-07); the discovery WARN is deleted; go decision recorded in `19-SPIKE.md`, L-13 reversed by L-33
- ~~`doWriteCol` throws a message-less `RuntimeException` for a missing non-nullable value, now reachable for `Array(Tuple)` after COLS-07, so Connect status and the DLQ show no column name (13-REVIEW WR-03)~~ **Closed in Phase 18 (WRT-10)** — now a column-naming, data-free `DataException`; sentinel asserted absent from every cause-chain message
- ~~`e.getMessage().contains(code)` runs before its null check in `ClickHouseWriter`'s error path; pre-existing, but new exception types flow through it (13-REVIEW WR-04)~~ **Closed in Phase 18 (WRT-11, D-05)** — a message-less failure is rethrown as itself and a null `IOException` message no longer classifies as a timeout; retry classification pinned both ways
- ~~Non-nullable null outcomes on the metadata-free JSON route rely on the server default `input_format_null_as_default=1` — belongs in the rollout checklist (ledger L-08)~~ **Closed in Phase 15 (ROLL-01)** — rollout item 3 states the dependency and gives the `system.settings` self-check

**Phase 14 review & CI follow-ups (emerged 2026-10-10, not yet scheduled — open in `14-REVIEW-DISPOSITION.md` / ROADMAP backlog):**

- ~~`Lz4RoundTripLiveTest` asserts LZ4 on the request leg only (`CompressedReadBufferBlocks > 0`); the response leg was proven once by an uncommitted zstd probe, not by a standing assertion (14-REVIEW WR-01)~~ **Closed in Phase 17 (DEBT-06)** — standing response-leg assertion from the read's own `system.query_log` row, seen red with `compressServerResponse(false)`
- ~~The "Test fork count" step recomputes `maxParallelForks` in shell from `nproc` rather than reading Gradle's container-aware value, duplicating the `build.gradle.kts` formula (14-REVIEW WR-02)~~ **Closed in Phase 17 (CI-14)** — the step reads the new `reportTestForks` Gradle task
- ~~`testRuntimeClasspath` carries `org.lz4:lz4-java:1.8.0` beside `at.yawk.lz4:lz4-java` via the Confluent 7.9.1 test stack — test-only, not shipped, needs a `build.gradle.kts` change — ROADMAP backlog 999.2~~ **Closed in Phase 17 (DEBT-07)** — excluded from `testRuntimeClasspath` only, pinned by `TestRuntimeClasspathLz4UnitTest`
- ~~CI annotations on run `38037397969`: Node.js 20 deprecation for `actions/checkout@v4`/`upload-artifact@v4`; `ubuntu-latest` moves to Ubuntu 26 from 2026-10-19; Lombok "Ambiguous: Jackson2 and Jackson3 exist" warning under Jackson 2.22.3~~ **Closed in Phase 17 (CI-11, CI-12, BLD-14)** — none of the three annotations appears on run `38172811430`

**Phase 15 publication follow-ups (emerged 2026-10-10, not yet scheduled):**

- [x] ~~⚠ The published `docs/ROLLOUT-v1.7.0_gg1.md` (at `f328fd21`) still has the pre-review text: item 2 offers `TRUNCATE TABLE` on the KeeperMap state table, which every `exactlyOnce` connector with the same `zkDatabase`/`zkPath` shares, so following it erases other connectors' state (15-REVIEW WR-01 / T-15-14); item 7's `version=*` advice is wrong for regex matchers (WR-03); FORK-DELTA L-05 states `best_effort` as the default without a version (WR-02). Fixed locally by docs-only `505e4981` on `sync/v1.7.0`; at Phase 15 UAT the user chose to leave it unpublished for now, so it reaches `origin` with the next `--no-ff` merge unless pushed earlier. Operators reading the published copy should be pointed at the corrected text~~ **Closed in Phase 16 (PUB-05, PUB-06)**: published in `537beb1f` (run 38074050935, green), with item 8 corrected in `be6b8861` (run 38156040416, green)
- ~~Audit threat T-13-19 (upstream ERROR log of an unparseable DateTime string) open below the `high` block threshold, listed in FORK-DELTA Known costs~~ **Closed in Phase 18 (WRT-12)** — DateTime/DateTime64 parse-failure ERROR logs carry column, types, exception class and value length only; sentinel masking pinned by `WriterLogMaskingUnitTest`. T-14-16 (`tests.yaml` has no `permissions` block; tag-pinned actions) **closed in Phase 17 (CI-11, CI-13)**
- ~~15-02 code review of the D-01/D-02 commits: 6 Info findings deferred (`15-REVIEW-DISPOSITION.md`)~~ **Closed in Phases 17 and 19 (DEBT-08, DEBT-09)**. IN-01..IN-05 were fixed (superseded) by the Nullable(Tuple) fix `2a94e045`, and IN-06's NanoTimestamp mantissa-with-fraction row is pinned in `MetadataFreeJsonWriterTest`

**Phase 17 follow-ups (emerged 2026-10-12, carried to Phase 20's operator checks):**

- ~~F1: the fork's GitHub default branch is `main` (a 2025 upstream mirror), not `custom_build`, so Dependabot (CI-15) and scheduled workflows read `main`; Dependabot version updates are also off on the fork until enabled. The user's default-branch intent was not stated at the CI-17 gate — undecided (17-RESEARCH F1, E-10)~~ **Closed in Phase 20 (20-08, D-11)** — after publication the user enabled Dependabot version updates and changed the default branch to `custom_build` themselves (recorded verbatim in `20-MILESTONE-CLOSE-RECORD.md`), so Dependabot and scheduled workflows now read the fork's files
- ~~The `github-actions` Dependabot entry's `exclude-paths` key is confirmed only from Dependabot's source, never observed; if Dependabot rejects it the whole file could be flagged invalid and stall the `gradle` entry. Check for no Dependabot config error once Dependabot reads the file; optionally assert the four exclude entries in the pin guard (17-REVIEW WR-01; IN-01..IN-03 open in `17-REVIEW-DISPOSITION.md`)~~ **Largely closed in Phase 20 (20-08)** — with `custom_build` the default branch the Dependabot tab showed no configuration error and a version-update job running. Residue carried in the close record: confirm the `gradle` entry shows a recent "Last checked" once the first jobs finish; IN-01..IN-03 remain open in `17-REVIEW-DISPOSITION.md` **— residue closed in Phase 23 (DEBT-12):** WR-01, IN-01 and IN-02 `fixed` with pointers to the Phase 21 commits, IN-03 `skipped` with the roadmap's reason; `open: 0`

**Phase 18 follow-ups (emerged 2026-10-12):**

- ~~The generic `doWriteColValue` catch rethrows the original exception, whose message can embed record data (reaches Connect status, DLQ headers and the `Error inserting records` ERROR log). Kept by D-09 so retry classification cannot change; pinned by a test and recorded as FORK-DELTA Known costs item 6 — ROADMAP backlog 999.3 (T-18-07)~~ **Closed in Phase 22 (WRT-13..WRT-15)** — value-free `DataException` with a `WithheldMessageException` cause; classification pinned unchanged by 184 cases; residual: Jackson reference-chain text on the opt-in JSON-as-string route (22-REVIEW WR-01, accepted at 22-UAT)
- ~~Phase 20's rollout note (ROLL-02) must state the upgrade carry-over for a batch left `BEFORE_PROCESSING` by the previous release: the batch retries with the same `insert_deduplication_token`, and whether earlier phantom rows remain depends on whether the first attempt landed; ledger L-30 carries the detection query (UAT test 2)~~ **Closed in Phase 20 (ROLL-02)** — `docs/ROLLOUT-v1.7.0_gg2.md` item 1, with the retry consequence pinned live by `DedupTokenRetryLiveTest` (8 cases on 26.9.18.11): a same-token retry is dropped on deduplicating engines even if the bytes changed, so old rows persist when the earlier attempt landed — the gg1 note's "can write rows twice" reasoning was wrong and is corrected

**Milestone-close follow-ups (emerged in `v1.7.0_gg2-hardening`, carried to the next milestone; owners and re-open triggers in `milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md`):**

- ~~19-REVIEW WR-01: `DeclaredNullability.apply`/`containsTuple` can throw an NPE outside the `try` for a null root or a null-type Column. Unreachable today. Re-open on a new caller or the next change to the pass~~ **Closed in Phase 23 (COLS-12)** — total for a null root and null types at any depth; `fix(23-02)` `4bbc9627`
- ~~19-REVIEW WR-02: four WARN-content assertions use column name `c`, which also matches the fixed text, so they cannot fail on a dropped column argument. Fix at the next edit of those test classes~~ **Closed in Phase 23 (DEBT-10)** — sentinel root plus delimited segment, red under both D-17 mutations; `test(23-03)` `075e6466`
- ~~19-REVIEW IN-01..IN-03: the `ESCAPED_NEWLINE` literal is copied into three tests; some test pins are brittle or indirect; `normalise` rewrites backslash-n inside element names and enum labels, which fails open with the metadata-only WARN~~ **Closed in Phase 23 (DEBT-11, COLS-12, COLS-13)** — one shared literal (`refactor(23-01)`), the D-08 flip, and the quote-aware `normalise` (`fix(23-04)` `f55165ee`)
- ~~19-REVIEW IN-04: a Tuple Column that lacks a declared element still "applies". Owned by the next milestone. Re-open on a clickhouse-data version change or a DESCRIBE shape that drops a declared element. Pinned by `missingDeclaredElementDoesNotShiftPairing`~~ **Closed in Phase 23 (COLS-14)** — the D-01 rule: one metadata-only WARN that names the unpaired element and still applies; `missingDeclaredElementDoesNotShiftPairing` byte-identical and green; the discovery drop it exposed is backlog 999.4
- ~~Phase 18 deferred item: `ClickHouseWriterNullMessageUnitTest` inline-mocks `ClickHouseHelperClient` with Mockito, with an intermittent OpenJ9 `retransformClasses` crash risk on this machine. Acknowledged at close~~ **Closed in Phase 22 (HARN-08)** — replaced by the Mockito-free `CountingHelperClient` subclass
- ~~The nightly `integrationTests.yaml` is upstream-identical. Its cron fires now that `custom_build` is the default branch, and it is red by inheritance: upstream's nightly fails daily, and the cloud job has no fork secrets. Disable it in Actions settings, or make it pass~~ **Closed in Phase 21 (OPS-01)** — the user disabled it on 2026-10-16; no scheduled run since, and the file is still byte-identical to upstream
- ~~Housekeeping (user): confirm that the Dependabot `gradle` entry shows a recent "Last checked". Revoke the read-only GitHub token and delete the Keychain item `gsd17-gh-actions-read`~~ **Closed in Phase 21 (OPS-02, OPS-03)** — confirmed by the user in their own words; token revoked, Keychain lookup exits 44

**Milestone-start items (emerged 2026-10-16, at `v1.7.0_gg3-hardening` start):**

- ~~The user merged three Dependabot PRs straight onto `origin/custom_build` on 2026-10-16: lombok 1.18.48 (#1), spotless 8.10.4 (#2) and slf4j-reload4j 2.0.20 (#3), tip `6473e24f`, CI green on each. Local `sync/v1.7.0` lacks them. They must be merged into the working branch, and the suite must pass on them before the next `--no-ff` publish merge~~ **Closed in Phase 21 (DEPS-01)** — merged as `82524e12`, suite green on them

**Phase 21 follow-ups (emerged 2026-10-19, at Phase 21 UAT):**

- ~~The repository has no `dependencies` label, but both `dependabot.yml` entries request it (the `gradle` entry from upstream v1.7.0, the `github-actions` entry from `475ce7b9`). Every Dependabot PR gets a "labels could not be found ... remove invalid values from `dependabot.yml`" comment and arrives unlabeled. Updates still run. Fix: the user creates the label in the web UI (D-11), or the fork drops the request~~ **Closed in Phase 25 (OPS-04): the user created the label in the web UI on 2026-10-21 (E-02, API 200); the closing observation E-25 is pending the next Dependabot PR (Phase 29 re-checks)**
- ~~Dependabot proposes bumps of the fork-only pins: PR #9 `org.apache.kafka:connect-api` 4.1.1 → 4.4.0 and PR #4 `clickHouseDriver` 0.9.6 → 0.10.0. `dependabot.yml` has no `ignore` for them. PRs #6-#8 (commons-lang3 3.21.0, ben-manes versions 0.66.0, Gradle wrapper 9.8.1) are open too, so 5 of 5 open-PR slots are used. Any merge on `origin/custom_build` diverges `sync/v1.7.0` again and needs a DEPS-01-shaped merge before the release~~ **Closed in Phase 25 (DEPS-02, DEPS-03): the nine user-merged PRs (#6-#9, #11-#15) are in merge M1 `50f56289` behind the unedited anchors gate; `connect-api` 4.4.0 is now the compile pin (BLD-16); PRs #4 and #10 stay open and untouched**

**Phase 22 follow-ups (emerged 2026-10-19/20, at Phase 22 review and UAT; findings in `22-REVIEW.md`; since Phase 24 each is `deferred` in `22-REVIEW-DISPOSITION.md` (`open: 0`), carried with reason, owner and trigger in `24-MILESTONE-CLOSE-RECORD.md`):**

- ~~22-REVIEW WR-01, accepted as a residual at 22-UAT test 3: the IOException pass-through (D-01) also passes Jackson's `JsonMappingException`, whose "through reference chain" text can name a Map key, on the opt-in `binaryFormatWrtiteJsonAsString` route. Disclosed in FORK-DELTA item 6. Fix if wanted: exclude `JsonProcessingException` from the pass-through and add a sentinel-map-key test~~ **Closed in Phase 27 (WRT-17)**
- ~~22-REVIEW IN-01..IN-05 (Info): an unknown Enum label is reported as `NullPointerException`; the fixed `No content in enum` text drops the column name; the metadata derivation is duplicated between the `ClassCastException` branch and the new helper; `schemaWithFixedStringMismatchTest` passes vacuously if `put` does not throw; a dead counter and planning-ID noise in tests~~ **Closed in Phase 27 (WRT-18, WRT-19, DEBT-16)**

**Phase 23 follow-ups (emerged 2026-10-20, at Phase 23 discussion, research and review; findings in `23-REVIEW.md`; since Phase 24 each review finding is `deferred` in `23-REVIEW-DISPOSITION.md` (`open: 0`) and carried in `24-MILESTONE-CLOSE-RECORD.md`, except IN-04, fulfilled):**

- ~~Backlog 999.5: `ClickHouseFieldDescriptor.fromJsonRow` doubles every backslash in a column or element name, so a Kafka field must use the doubled form to match. Pre-existing; needs a decision on the intended name form and a rollout note (D-20)~~ **Closed in Phase 28 (NAME-01..NAME-07)**
- ~~23-REVIEW WR-01: no test pins the `normalise` branch where an even backslash run before a backtick or single quote keeps the quote open; a mutation that always closes the quote passes the suite. The verifier checked the branch by hand and production is correct — a test gap, not a defect~~ **Closed in Phase 28 (NAME-01)**
- ~~23-REVIEW IN-01..IN-03 (Info): the `apply` Javadoc still says "all or none"; the null-name early return sits inside the `try` and logs `null` in the column slot; the new live tests can pass vacuously or NPE on a missing DESCRIBE row~~ **Closed in Phases 27 and 28 (NAME-04, DEBT-16)**
- ~~23-REVIEW IN-04 (Info): the unpaired-element WARN does not say inserts will fail with `CANNOT_READ_ALL_DATA`~~ **Fulfilled in Phase 24 (`9d4cb9b2`)**: rollout item 2 says what the WARN marks, that it repeats at every DESCRIBE, and what to do. The WARN text is unchanged

**Phase 24 follow-ups (emerged 2026-10-20, at the Phase 24 pre-gate review and 24-UAT):**

- ~~24-REVIEW round 3, accepted at 24-UAT test 1 as a docs-only follow-up after the milestone. Three were statements or steps an operator could act on wrongly (WR-03, WR-05, WR-06, listed below).~~ **Closed by quick 261020-vcr (2026-10-20).** All seven round 3 findings are fixed. Review rounds 4–6 found and fixed seven more (WR-08..WR-11, IN-06..IN-08), among them a false "safe" for `enableDbTopicSplit=true` connectors. The note was republished as `6a2a87a5` (run 39496482074, green), and `24-REVIEW-DISPOSITION.md` has 19 rows, `open: 0`:
  - ~~WR-03: rollout item 3 says "every root that is not `Nullable`" is identical, but 4 of the 14 differing roots are not `Nullable` at the top (`Array(Nullable(Tuple))`, `Map(String, Nullable(Tuple))` and two plain `Tuple` roots).~~
  - ~~WR-05: item 4 excludes `exactlyOnce=false` connectors from the rollback hazard, but E-12 measured that hazard on `exactlyOnce=false`.~~
  - ~~WR-06: item 7 commits the branch rollback, which removes the note, before the in-flight check that item 4 says to run first.~~
- ~~24-REVIEW round 3, the rest: WR-04 ("every insert fails" is stated for all `size` shapes, but 4 of 13 were inserted, and the same text allows a shorter row to parse); WR-07 (the state-table query has two topic slots, so a connector with three or more topics can read a false "safe", and the query names no database); IN-04 (the known-limitation sentence leaves out the line break); IN-05 (the CHANGELOG says "the ClickHouse client" for `ClickHouseHelperClient`)~~ **Closed by quick 261020-vcr**
- ~~Carried in `24-MILESTONE-CLOSE-RECORD.md`: `jacocoForkReport` omits `DeclaredNullability` and `WithheldMessageException`, so the coverage report understates fork coverage; 24-REVIEW IN-03, planning identifiers in `JsonAsStringKeyResidualLiveTest`'s Javadoc and display names~~ **Closed in Phases 27 and 28 (WRT-19, DEBT-17)**

**Quick 261020-vcr follow-ups (emerged 2026-10-20, at review rounds 4–6):**

- ~~`docs/ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` are protected and already published. Both keep a shorter `enableDbTopicSplit=true` key rule ("list `'<db><dbTopicSplitChar><topic>'` instead of `'<topic>'`") and a state-table query with two slots, and both can miss a key: per `RecordConvertor.convert`, a topic that the split character divides into exactly two parts is keyed under its own name. The gg3 note tells the reader to follow its own item 4 step 2. Correct the older notes only if an operator still uses them~~ **Closed in Phase 29: the gg1 and gg2 notes now carry the key rule**

**Milestone-start items (emerged 2026-10-20/21, at `v1.7.0_gg4-hardening` start):**

- ~~The user merged seven Dependabot PRs straight onto `origin/custom_build` on 2026-10-20 and 2026-10-21: Gradle wrapper 9.8.1 (#8), `connect-api` 4.4.0 (#9, fork-only pin), commons-lang3 3.21.0 (#6), ben-manes versions 0.66.0 (#7), gson 2.14.0 (#13), httpclient5 5.6.5 (#12) and Shadow 9.6.1 (#11), tip `d20bae63`. Local `sync/v1.7.0` lacks them. They must be merged into the working branch and the suite must pass on them before any release. PR #10 (protobuf-java 4.36.2, testFixtures only) is to be merged the same way~~ **Closed in Phase 25 (DEPS-02): absorbed as merge M1 `50f56289` (second parent `69cd680b`); the suite passed on the merged tree (E-19, E-24); PR #10 remains open, not merged**
- ~~`upstream/main` is 12 untagged commits past `v1.7.0`, among them an explicit guava dependency for a CVE (`30521f74`). A tagless mini-sync in the usual shape takes them~~ **Closed in Phase 25 (SYNC-11, SYNC-12): merge M2 `38265d26` behind the tail gate; guava resolves to 33.7.2-jre (E-22); baseline tag `baseline-v1.7.0_gg4` = `aa450709`**
- [ ] The stale local ref `refs/upstream/main` (`789a57c2`, the v1.6.0 merge) makes `upstream/main` ambiguous in git commands; delete it

**Quality gates:** all three met — moved to Validated at the Phase 5 transition.

**Milestone-close note (v1.2.9_gg4-tests):** every item above is a *review finding*, not an unmet v1 requirement — all 75 v1 requirements shipped. None is a regression; each is either pre-existing fork behavior now documented, or a hardening opportunity the coverage work made visible for the first time.

**Milestone-close note (v1.6.0_gg1-sync):** 17 items open. Ten items came in from `v1.2.9_gg4-tests`: two closed for free (FREE-01, FREE-03) and eight carry forward. Nine are new, from Phase 7, 9, 10 and 11 review and verification. None is an unmet requirement; R-01 is still the only one that loses data.

**Milestone-close note (v1.7.0_gg1-sync):** all 17 carried items are closed. Eleven new items are open: four from Phase 13 review, four from Phase 14 review and CI, and three from Phase 15 publication. Info-level review findings are tracked in each phase's `REVIEW-DISPOSITION.md`. None is an unmet requirement. None loses data silently. 999.1 stores wrong values for `Nullable(Enum)`, but it is a pre-existing defect that is now warned about at discovery. The most urgent item is publishing `505e4981`, because the published rollout doc can erase shared exactly-once state.

**Milestone-close note (v1.7.0_gg2-hardening):** all 11 carried items are closed, including the 15-02 Info findings (DEBT-08, DEBT-09). Ten items are open: backlog 999.3, the five 19-REVIEW advisories and IN-04, the Phase 18 Mockito/OpenJ9 risk, the inherited-red nightly `integrationTests.yaml`, and user housekeeping. The 17-REVIEW WR-01 residue and IN-01..IN-03 stay recorded under the Phase 17 follow-ups above. None is an unmet requirement. None stores a wrong value. 999.3 is the only one that can expose record data.

**Milestone-close note (v1.7.0_gg3-hardening):** all ten carried items are closed (999.3, the 19-REVIEW advisories and IN-04, the Mockito/OpenJ9 risk, the nightly workflow and the user housekeeping), and so is the 17-REVIEW residue. These items are open:
- the Phase 21 Dependabot items (the label, PRs #4–#9);
- the 22-REVIEW and 23-REVIEW advisories, with owners and triggers in the close record;
- backlogs 999.4 and 999.5;
- the `jacocoForkReport` gap;
- the older notes' key rule.

None is an unmet requirement. 999.4 is the one item that can fail inserts for an affected table, and possibly store a wrong value. It is pre-existing, named by a WARN, and disclosed; Phase 26 fixed it on the working branch, and the fix ships with `v1.7.0_gg4`.

**Milestone-close note (v1.7.0_gg4-hardening):** backlogs 999.4 and 999.5, the Jackson Map-key residual, the Enum-label NPE, the `jacocoForkReport` gap and the older notes' key rule are closed. These items are open, each with an owner and a re-open trigger in `milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md`:
- [ ] The 16 unfixed review findings from 25-REVIEW (WR-01..WR-03, IN-01..IN-04) and 26–28-REVIEW. Phase 29 was a release phase and edited no `src/`, test or build file to close them.
- [ ] PUB-12 / SC3: the Phase 29 review ran four rounds (the roadmap allowed at most three) and round four found IN-09. The user authorised round four and accepted the deviation at 29-UAT. The IN-09 fix (CHANGELOG wording only) was never re-reviewed.
- [ ] `confluent-archive-allowlist-29.txt` has no CI consumer. The entry-by-entry archive compare against gg3 is a manual release-time step; CI runs only `verifyConfluentArchive`.
- [ ] No single test drives a size-named Tuple element whose name also contains an escape character.
- [ ] `docs/FORK-DELTA.md:224` still reads, as history, that gg3 shipped items 10 and 11 as open. Line 226 states they are closed.

None is an unmet requirement and none loses data. The two discovery defects that could fail inserts or store a shifted row are fixed and shipped.


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

**Out-of-Scope audit at v1.7.0_gg2-hardening close.** Every exclusion still holds, with three notes:

- **"New connector features" held.** `DeclaredNullability` restores the values the declared column type already promised and adds no capability or config option. The empty-CDC skip extends an existing rule to the remaining routes.
- **The `ClickHouseWriter` refactoring exclusion held.** Phase 18 touched four targeted regions. The Tuple nullability pass lives in its own file, outside `Table`, the writer and the state classes, so retried batches stay byte-identical.
- **Upstream-identical code was patched on the record.** The DateTime ERROR-log masking (T-13-19) and the null-message guard in `Utils.handleException` change upstream lines, and each has its FORK-DELTA row. Upstream's nightly `integrationTests.yaml` stays byte-identical even though it now runs red on the fork.

**Out-of-Scope audit at v1.7.0_gg3-hardening close.** Every exclusion still holds, with three notes:

- **"New connector features" held.** `WithheldMessageException` and the value-free `DataException` change error text only, and the unpaired-element WARN is a log line. No config option was added.
- **The `ClickHouseWriter` refactoring exclusion held.** Phase 22 changed one catch and one helper. Retry classification is pinned byte-identical by 184 cases.
- **The upstream sync stayed out because there was nothing to sync** (`upstream/main` = `v1.7.0`). ClickHouse client `0.10.0` stayed out, with PR #4 left open. The nightly `integrationTests.yaml` was disabled in settings rather than edited.

## Context

- **Repository:** fork `git@github.com:ne1r0n/clickhouse-kafka-connect.git` (`origin`), branch `custom_build` at `21c51598` (the `v1.7.0_gg4` merge) = upstream `v1.7.0` + the fork delta in `docs/FORK-DELTA.md`. Since 2026-10-14 `custom_build` is the GitHub default branch. `upstream` remote → `ClickHouse/clickhouse-kafka-connect`. Tags on `origin`: `v1.7.0_gg3`, `v1.7.0_gg2`, `v1.7.0_gg1` and `v1.6.0_gg1` (published); `pre-v1.7.0_gg3`, `pre-v1.7.0_gg2`, `pre-v1.7.0-sync` and `pre-v1.6.0-sync` (rollback). The corrected-note merge publishes the planning history through `a403cc1d`. Later bookkeeping, including the `v1.7.0_gg3-hardening` close, lives on local `sync/v1.7.0`. Milestone tags (`v1.7.0_gg1-sync` etc.) are local-only. `custom_build` is protected; the scratch branch was deleted from `origin` once CI went green. Codebase map in `.planning/codebase/` predates the merge (mapped at `22f055c`) and is stale for `ClickHouseWriter`, `ClickHouseHelperClient` and the build.
- **Stack:** Java 17 toolchain, Gradle 9.6.1 with upstream's version catalog (`gradle/libs.versions.toml`) and `testFixtures`, Kafka Connect API 4.4.0 (fork-pinned, compile-time only), ClickHouse Java client 0.9.6 (fork-pinned; V1 and V2 paths), Jackson + Gson, Lombok. Tests: JUnit Jupiter 5.14.4, Mockito, Hamcrest, Testcontainers 2.0.5 (ClickHouse, Kafka, Toxiproxy). Shared test helpers (`ClickHouseTestHelpers`, `SchemaTestData`, `SchemalessTestData`) live in `src/testFixtures`.
- **Test layout:**
  - `src/test/java` — unit tests plus ClickHouse-backed tests extending `ClickHouseBase` (starts a Testcontainers ClickHouse per class); run by `./gradlew test`.
  - `src/test/java/transforms/` — SMT tests (outside the `com.clickhouse...` package).
  - `src/integrationTest/java` — Confluent-platform / Cloud tests, `integrationTest` task, only `workflow_dispatch` in CI.
  - ClickHouse image chosen by `CLICKHOUSE_VERSION` env (default `latest` since Phase 1, was `25.3`, `ClickHouseTestHelpers`); client by `CLIENT_VERSION` env (default V2). **The default silently reverted to `25.8.18.1` in the v1.6.0 merge and stayed wrong from Phase 6 to Phase 10** — Phases 8 and 9 therefore closed green against a server this milestone does not claim to test. Restored and pinned Docker-free by `ClickHouseVersionDefaultUnitTest` (Phase 10; MERGE-CHECKLIST Recorded Fact 15). Tests run with `file.encoding=windows-1252` — deliberately, so non-ASCII handling is exercised rather than masked by a UTF-8 default.
  - `ClickHouseBase` gives one shared container per test JVM fork, a fresh empty database per test class, an inherited `@Tag("clickhouse")`, an `@AfterEach` MBean leak sweep, and opt-in `uniqueConnectorName(TestInfo)`. New sink-task tests must create their tables before `start()` — `ClickHouseWriter.start()` fails against an empty database.
  - `./gradlew testUnit` runs everything not tagged `clickhouse`, needs no Docker, and is deliberately not part of `check`.
  - JaCoCo is report-only on `test`: `jacocoTestReport` plus `jacocoForkReport` over 21 explicitly listed fork-changed classes. No coverage threshold can fail the build.
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
- **CI after v1.7.0_gg2-hardening:** `tests.yaml` runs on `ubuntu-24.04` with all four actions pinned to commit SHAs (Node 24 majors), plus `permissions: contents: read` and `persist-credentials: false`. The fork-count notice reads Gradle's `reportTestForks` (`nproc=4, Gradle maxParallelForks=2`). `TestsWorkflowPinGuardUnitTest` pins all of it Docker-free. `test-head.yaml` is deleted. Dependabot has `gradle` and grouped monthly `github-actions` entries, and version updates are enabled. With `custom_build` the default branch, upstream's nightly `integrationTests.yaml` now fires on schedule and is red by inheritance (Active follow-up). Last verified run: `38587062864` on `a246ca01`, 408 s, the only run on that SHA.
- **Research:** each milestone's research is archived with it: `milestones/v1.2.9_gg4-tests-research/` (per-feature test inventories, 10 pitfalls), `milestones/v1.6.0_gg1-sync-research/` (merge strategy, conflict map, upgrade pitfalls) `milestones/v1.7.0_gg1-sync-research/` (v1.7.0 collision map, compression and lz4 advisories, R-01 routing) and `milestones/v1.7.0_gg2-hardening-research/` (follow-up hardening and CI hygiene). No milestone-level research was run for `v1.7.0_gg3-hardening`; each phase's research is in its archived phase directory.
- **CI after v1.7.0_gg3-hardening:** same shape. The pin guard also pins the fork-count step's `reportTestForks` call and Dependabot's `exclude-paths`. Last verified run: `39496482074` on `6a2a87a5`, the only run on that SHA. The nightly `integrationTests.yaml` has been disabled in settings since 2026-10-16.
- **CI after v1.7.0_gg4-hardening:** same shape, plus the archive step: CI builds `createConfluentArchive` (finalized by `verifyConfluentArchive`) before the tests, and the pin guard keeps the step and the task from being dropped. Last verified run: `40017036598` on `21c51598`.
- **Open follow-ups:** the open items under Requirements → Active (see Next Milestone Goals). The published `v1.7.0_gg4` has no known wrong-value or record-data defect; the open items are review advisories and the manual archive compare (FORK-DELTA Known costs).

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

**Constraints check at v1.7.0_gg2-hardening close.** All five still apply.

- **Behavior preservation** was crossed again, deliberately and on the record. `Nullable(Enum)`/`Nullable(Tuple)` values change, empty CDC records stop writing rows, and a single-field `Nullable(Tuple)` with a null field now throws. Each is a bug fix with a rollout item, a CHANGELOG line and a FORK-DELTA row.
- **Tooling.** No library was bumped and no build dependency was added. `build.gradle.kts` gained 18 lines: the test-only lz4 exclude and the `reportTestForks` task.
- **Rebasability.** The fork delta grew (95→119 files wide, 31→32 in `src/main`), mostly with tests. The one new `src/main` file keeps the Tuple fix outside upstream's `Table`, which keeps the next sync's conflict surface small. The anchors invariant passed over every commit.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fix the round 3 findings before closing the milestone instead of carrying them, and republish as a docs-only `--no-ff` merge on top of the release | The audit came back `tech_debt` only because of them, and three of them (WR-05..WR-07) were rollback steps an operator could follow wrongly | ✓ Quick 261020-vcr: `6a2a87a5`, run 39496482074 green, tags unmoved; re-audit passed |
| Run independent `gsd-code-reviewer` rounds on operator-doc fixes until a round finds nothing | Each round of fixes surfaced a smaller follow-up (7 → 5 → 2 → 0), as in Phases 15 and 16 | ✓ Rounds 4–6 (quick 261020-vcr) |
| WR-10: list both `enableDbTopicSplit=true` key forms in the state-table check, against the reviewer's and the gg1 note's single prefixed form | `RecordConvertor.convert` keys a two-part topic under its own name, so the prefixed form alone misses it. An extra entry in the `IN` list is harmless | ✓ Checked with `String.split` in round 5 and confirmed in round 6; read from code, not measured |
| Phase 24 D-04 branch 1: keep the Jackson Map-key residual on the opt-in JSON-as-string route with no code change, and state it as the one exception to "no record value" in the rollout note, CHANGELOG and FORK-DELTA item 6 | Measured on 26.9.19.9: only a Map key reaches the task status, the DLQ stack-trace header and the `Error inserting records` throwable (no value, no List content). Phase 24 makes no `src/main` edit (D-06) | ✓ Phase 24 (24-01) — pinned by `JsonAsStringKeyResidualLiveTest`; R-22-02 aligned |
| Phase 24 D-15: an upgrade needs no clean stop; a rollback needs one for a table with a Nullable(Tuple) element whose name has a backslash-n or a line break | A gg2 retry of a range that gg3 landed fails with code 33 instead of being dropped, because the server reads the bytes before the token (E-12); every other shape is byte-identical (D-09, 30 of 44 tables) | ✓ Phase 24 (24-02, 24-03) — shape query E-13 and KeeperMap in-flight query E-14 in rollout item 4 |
| Phase 24: accept the round 3 review warnings (WR-03..WR-07, IN-04, IN-05) as a docs-only follow-up after publication instead of reopening the release | The tag and merge are already published; the fixes are wording and step order in operator documents, with no code or test change | ✓ The user's answer at 24-UAT test 1 (2026-10-20); done in quick 261020-vcr before the milestone close (`6a2a87a5`) |
| Phase 23 D-01/D-02: an unpaired declared Tuple element logs one metadata-only WARN at every DESCRIBE and the pass still applies to the elements that paired, instead of falling back for the whole column | A Tuple Column never legitimately lacks a declared element (discovery dropped it), and a fallback would re-introduce the double flag on the paired elements; ROADMAP SC3 was amended to match | ✓ Phase 23 (23-05) — 16 unpaired-WARN testcases red before, green after; `missingDeclaredElementDoesNotShiftPairing` byte-identical |
| Phase 23 D-19 (Option B): flip the three tests the D-01 rule collides with by adding element rows, keeping names and assertions, and move the root-only inputs to sibling tests that expect the WARN | Keeps every existing assertion and testcase name (AGENTS.md: never weaken a test) while the new rule holds; each flip carries a scoped exemption comment | ✓ User's own answer at plan-phase; forced-red run of exactly the three names recorded in E-05 |
| Phase 23 D-03: leave the `size`/`size0` discovery drop (999.4) to its own phase and pin it as a known defect now | The fix touches upstream-region `ClickHouseHelperClient` and needs its own FORK-DELTA row, anchors row and live round trip; the unpaired-element WARN already names the dropped element | ✓ Phase 23 — D-04 pin through the real skip; backlog 999.4 |
| Phase 23 D-08: flip `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning` from asserting `NullPointerException` to `(structure mismatch)` | COLS-12 makes the null-type input a structure mismatch, so the old expectation is impossible; method and input are kept with a scoped AGENTS.md exemption naming D-08 and ROADMAP SC1 | ✓ Phase 23 (23-02) — red on the unchanged code, recorded in E-02 |
| Phase 22 D-01/D-05: pass IO chains, `ConnectException` and exact `RuntimeException` through unchanged; replace everything else with a value-free `DataException` whose cause is a `WithheldMessageException` copy of the chain | Retry classification reads the root cause, so IO chains must stay intact (WRT-14); a sanitized copy keeps the frames and the original class name for debugging without the message text | ✓ Phase 22 (22-03) — 184-case pin byte-identical and green; sentinel cases red before, green after |
| Phase 22 L-47: flip `schemaWithFixedStringMismatchTest` to assert a `WithheldMessageException` root cause plus `getOriginalClassName()` instead of deleting it | D-05 makes the old instance-of assertion impossible; the new pair discriminates the same class, so the test is not weakened | ✓ Approved by the user at 22-UAT test 1 (2026-10-20) |
| Phase 22 WR-01: accept the Jackson reference-chain residual on the opt-in JSON-as-string route instead of excluding `JsonProcessingException` from the IO pass-through | Opt-in and narrowly reachable; already disclosed in FORK-DELTA item 6; the fix is one line plus a test if wanted later | ✓ Accepted by the user at 22-UAT test 3 (2026-10-20); recorded as R-22-02 in `22-SECURITY.md` |
| Phase 21 D-01/D-02: forgive the single pre-existing anchor violation at `6473e24f` with a full-SHA, path- and pattern-pinned row in `fork-anchor-exemptions.txt`, consulted only by invariant 3's count-mismatch branch; `PRE_TAG` stays `pre-v1.7.0-sync` | The violation is in published history (Dependabot #3 bumped `slf4j-reload4j` without the anchors file) and cannot be rewritten. A narrow data row keeps the gate command for Phases 22-24 unchanged and cannot match a future commit | ✓ Phase 21 (21-03) — `anchor_exempt=1`, self-test 16/16, the old gate fails 7 cases and a SHA-only mutant fails 2 |
| Phase 21 D-14: hold spotless at baseline-equal (the same 46 import-order files as 8.10.1) instead of a tree-wide reformat | A reformat commit would touch 46 files with no behaviour change and conflict with every future upstream sync | ✓ Phase 21 — the diff md5 equals the 8.10.1 baseline; no file Phase 21 edited is in it |
| Phase 21 OPS-02: accept the user's own statement ("dependabot работает как и ожидается, новые PR создаются") plus the Dependabot job trace instead of a "Last checked" reading | The Dependabot tab no longer shows a "Last checked" field (a Recent jobs list replaced it), and the REST API does not expose one. The user's words were recorded verbatim; the screenshot-only reply was not counted (D-13) | ✓ Phase 21 UAT (2026-10-19) — E-02; job 1636257648 opened PRs #6-#9 |
| Close `v1.7.0_gg2-hardening` as an override closeout after a passed milestone audit, accepting the stale Phase 16–19 verification digests and acknowledging the Phase 18 Mockito/OpenJ9 item | The digests drifted only because Phase 20 edited the docs and anchors they covered, so re-running four verifiers would change nothing on the tree. The audit, two suite runs and the published-SHA CI run cover the final tree | ✓ Milestone close (2026-10-16; user chose Acknowledge and Override) |
| Phase 20 D-08: carry the five 19-REVIEW advisories and IN-04 with re-open triggers instead of fixing them before the release | Any fix would be a production or test change after Phase 19's last green gate. None is reachable today or changes shipped behaviour | — Pending (next milestone; triggers in `20-MILESTONE-CLOSE-RECORD.md`) |
| Phase 20 PUB-08: publish `v1.7.0_gg2` as a rehearsed two-step — push `sync/v1.7.0` once for a green CI rehearsal on the exact tip X0, then the `--no-ff` merge of X into `be6b8861` with the rollback tag pushed first, the release tag only after the merge's own run is green, and the scratch branch deleted last | One branch per SHA keeps "exactly one run per published SHA" provable; tagging after green means the tag can never point at an unproven tree; the pre-pushed rollback tag makes the escape hatch live before the change it escapes | ✓ Phase 20 (user answered verbatim "push", then "publish", both directly) — merge `a246ca01`, run 38587062864 green, tags `v1.7.0_gg2` + `pre-v1.7.0_gg2` on origin, verification 8/8 |
| Phase 20 ROLL-02: pin the dedup-token retry consequence with a live test before writing the rollout note, instead of reasoning from the gg1 note | The gg1 note's "can write rows twice" claim was unverified; `DedupTokenRetryLiveTest` showed the opposite — a same-token retry is dropped on deduplicating engines even with changed bytes, so the real risk is old rows persisting when the earlier attempt landed | ✓ Phase 20 (20-01, 8/8 green on 26.9.18.11) — rollout item 1 quotes only measured behaviour; detection queries flag gg1 rows and no gg2 rows in the live probe matrix |
| Phase 19 COLS-10/COLS-08: Tuple element nullability comes from the declared type string (`DeclaredNullability`, fail-open total pass run once per DESCRIBE), and the Enum fix is a single `.isNullable(isNull)` in `Column.extractColumn`; the `Nullable(Tuple)` discovery WARN is deleted | Upstream drops Tuple nullability and DESCRIBE marks every element of a `Nullable(Tuple)` as `Nullable`, so the declared string is the only correct source; keeping the fix outside `Table`/writer/state classes keeps retried batches byte-identical | ✓ Phase 19 — go recorded in `19-SPIKE.md`; gap closure (19-07..19-10) added the `SimpleAggregateFunction` unwrap, made the pass total with a `db.t`-naming fallback WARN, and pinned 7 wrapper edges; suite gate 2× green (980 tests), verification passed 6/6 |
| Phase 18 JSONP-09: one shared `skipEmptyCdcRecord` in the writer serialization loops, no short-circuit for an all-skipped batch (empty insert body sent) | Offsets, exactly-once state and the dedup token must cover the whole batch, so the skip may only live where bytes are produced; both clients accepted a zero-row body on 26.9.16.11 | ✓ Phase 18 (18-03/18-04, D-01..D-04) — four routes pinned live on V1 and V2; byte-identity, full-range token and identical retry body pinned Docker-free; mutation check seen red. UAT confirmed all-empty batches on V1 and compressed requests |
| Phase 18 WRT-12/D-09: date-time parse-failure ERROR logs masked to column, types, exception class and value length; the generic catch logs with no throwable but still rethrows the original exception unchanged | Changing the rethrown type or message could change retry classification (D-09); the residual record-data exposure in the rethrown message is accepted and tracked (backlog 999.3, FORK-DELTA item 6, T-18-07) | ✓ Phase 18 — `WriterLogMaskingUnitTest` pins sentinel absence from every log text and self-checks the raw exception embeds it; zero-count anchor row keeps the old throwable-bearing line out |
| Phase 18 WRT-10/WRT-11/D-05: missing non-nullable value throws a column-naming, data-free `DataException`; a message-less writer failure is rethrown as itself; a null `IOException` message is not a timeout | Connect status and the DLQ previously showed a message-less `RuntimeException` with no column name; null messages crashed or mis-classified the retry decision | ✓ Phase 18 (18-01, three self-contained fix commits each carrying its own fork-anchors and FORK-DELTA rows per D-13) — retry classification pinned both ways at put() level |
| Phase 17 D-11: push scratch `sync/v1.7.0` once for the CI-17 proof (branch only, explicit refspec), delete it after green | One run proves the pinned, read-only, `ubuntu-24.04` workflow and every Phase 17 change before the release merge depends on it; one-way publication of the local tail accepted | ✓ Phase 17 (user answered verbatim "push" to the orchestrator, 2026-10-11) — run `38172811430` green, branch deleted |
| Phase 17: read the CI-17 job log through the API with a user-created read-only fine-grained token instead of the D-13 web-UI paste; strip only `##[group]` markers for the checker (A9) | Verbatim log beats a manual copy; the token never entered the chat or a file (macOS Keychain); the rendered job summary has no REST endpoint, so it was reconstructed from the same log | ✓ Phase 17 (user-requested 2026-10-12; A9 strip "approved") — E-11 pass, verifier judged D4 satisfied |
| D-14: publish `v1.7.0_gg1` as a non-force `--no-ff` merge of the rehearsed tree X (`ae8520ed`) into `custom_build`; Phase 15 commits after X stay on local `sync/v1.7.0` until the next milestone's merge | One revertible commit (`git revert -m 1 f328fd21`) for the whole sync; a rehearsal run on X proved CI before the protected branch moved; publishing the bookkeeping tail separately would cost a second CI run on `custom_build` | ✓ Phase 15 (user approved verbatim "publish") — `f328fd21`, run `38050860929` green, the only run on that SHA |
| Leave docs-only `505e4981` (15-REVIEW WR-01..03 fixes to ROLLOUT and FORK-DELTA) unpublished after release | Publishing needs a new push to protected `custom_build` and another CI run; the user accepted the published copy keeping the old text for now | ✓ Superseded by Phase 16. The fix was published in `537beb1f` on 2026-10-10 |
| Fix 16-REVIEW WR-01 (item 8 rollback conflicts on the published tip) and republish, not defer | The published rollback step stopped with 97 conflicts once docs merges sat on top of the release. A wrong rollback step is worst in the middle of an incident | ✓ Phase 16 UAT (2026-10-11, "fix everything", then "Publish now"). The fix is `read-tree -u --reset pre-v1.7.0-sync` with an explicit clean, fast-forwarded precondition (WR-02, IN-02..IN-04), proved on the real merge before push (E-15..E-17), and published in `be6b8861` with run 38156040416 green |
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
| Phase 25: absorb the user-merged Dependabot PRs as one merge (M1) behind the unedited anchors gate, with exemption rows generated from the gate's own stderr, and take upstream's untagged tail as a second mechanical merge (M2) with its own rollback tag | Two merges keep the Dependabot absorption and the upstream sync separately revertible (`pre-v1.7.0-deps`, `pre-v1.7.0-tail`); generated rows cannot forgive more than the gate reported | ✓ M1 `50f56289` (`anchor_exempt=19 result=PASS`), M2 `38265d26` (`provenance_paths=3 result=PASS`); nothing pushed (Phase 25) |
| Phase 25: the Confluent archive rules live once in `ConfluentArchiveCheck` (test source set, JDK only), run by the `verifyConfluentArchive` finalizer of `createConfluentArchive` and in CI before the tests, and the pin guard takes the task's `mainClass` from the class itself | One implementation for the build, the unit test and CI, so the guard cannot drift from what the test exercises | ✓ 7,798 inner entries, 0 nested jars, 0 Kafka classes; both negative controls fire; 25-REVIEW WR-01 notes the step pin still accepts `-x`/`--dry-run` shapes (open) |
| Phase 26: fix 999.4 behind a committed DESCRIBE spike — a 47-table DDL catalog, a hand-run capture harness, byte-exact fixtures with a live drift premise, and the measured affected set from the connector's own WARN — before any `src/main` change | The server's real rows, not memory, decide the classifier rule (the structural-then-declared double row was confirmed on `latest`); the same fixtures prove the V1/V2 extraction behaviour-preserving and give the fix an exact red set | ✓ E-02 confirmed the double row; 25 of 47 fixtures affected; snapshots went red on exactly those 25 (E-06) |
| Phase 26: the classifier keeps a size-named row only when the parsed parent Tuple declares that key and the row is not an offsets row by type and depth, and fails closed (old behaviour plus one metadata-only WARN) when the parent type cannot be parsed (D-03/D-04) | A wrong Column list writes wrong RowBinary bytes for every row of an affected table, so an unparseable parent must not guess; 26-REVIEW WR-01 notes the name "fail closed" understates that the declared element is then still dropped | ✓ 120-row unit table, 46/46 live round-trip with zero WARN, pre-fix tree fails exactly the 25 measured keys |
| Phase 26: the phase gate's full suite counts a HotSpot 21 run (`-PjavaVersion=21`) after three consecutive OpenJ9 `Mockito.spy` crashes on the default Semeru 17 under host memory pressure; the orchestrator's own Semeru gate passed on re-run | The crash is an environment fault at a known frame, not a test failure, and the tree was already green twice on Semeru 17 on the same code; all four runs are recorded in E-12 | ✓ 1709 tests, 0 failed, 18 skipped on `1f2f1561` (Semeru twice, HotSpot once) |
| Phase 25: `connect-api` 4.4.0 is proven compile-time only by an offline bytecode check of the connector's 176 Kafka references against the `cp-kafka-connect-base:7.9.7` worker jars, pinned by `ConnectApiReferencesUnitTest`; wording states only what E-12 measured (the Kafka 3.9 worker still declares `onPartitionsRevoked`) | The production worker supplies the Connect API at runtime; an operator-facing claim needs a transcript behind it (D-14) | ✓ `REFERENCE worker classes-missing 0 members-missing 0`; 25-REVIEW WR-02/WR-03 note the unit test is a weaker pin than the bless run (open) |

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
*Last updated: 2026-10-24 after starting milestone v1.7.0_gg5-ci*