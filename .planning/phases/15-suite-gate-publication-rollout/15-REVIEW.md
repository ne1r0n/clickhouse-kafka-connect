---
phase: 15-suite-gate-publication-rollout
reviewed: 2026-10-10T10:40:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
findings:
  critical: 0
  warning: 3
  info: 10
  total: 13
status: issues_found
incremental_reviews:
  - id: release-documents
    date: 2026-10-10
    diff_base: 55ff8e7429086dabec27305b16fcb36e254cac0b
    depth: standard
    scope: operator-facing release documents of v1.7.0_gg1
    files_reviewed: 4
    files_reviewed_list:
      - CHANGELOG.md
      - VERSION
      - docs/FORK-DELTA.md
      - docs/ROLLOUT-v1.7.0_gg1.md
    findings:
      critical: 0
      warning: 3
      info: 4
      total: 7
    finding_ids:
      - WR-01
      - WR-02
      - WR-03
      - IN-07
      - IN-08
      - IN-09
      - IN-10
---

# Phase 15: Code Review Report

**Reviewed:** 2026-10-10
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

I reviewed the diff of the two 15-01 code commits (`981a89d6` fix D-01, `4a91b92c` feat D-02/D-15), range `672663c2..HEAD`, restricted to the six source and test files above. `.planning/scripts/fork-anchors.txt` is outside the review scope by design. I read each changed file in context (the full `MetadataFreeJsonWriter` serializer chain and both `describeTable` loops) and checked the points the plan named explicitly. I found no Critical and no Warning findings. I did not run the Gradle build; the 15-01 SUMMARY records the targeted runs, and plan 15-02 Task 2 re-runs them.

Explicit checks from the plan:

- **Non-Number values and overflow-free scaling.** Both new cases guard with `value instanceof Number` and fall through to the default rules otherwise (pinned by `nonNumberDebeziumTimestampFallsThrough`). `BigDecimal.valueOf(long, scale)` carries the unscaled long exactly, so no value can overflow or lose digits. I confirmed the scientific rendering of small `NanoTimestamp` values with a mantissa fraction against the server (`clickhouse-local`, `latest`): `1.23E-7`, `9.99E-7`, `-9.99E-7`, `-1.5E-8` read back as 123, 999, -999 and -15 ns. The committed tests pin `0E-9`, `1E-9` and `-1E-9` only; the mantissa-with-fraction form is covered by this check, not by a test (see IN-06 on test coverage of the form).
- **Nested values.** `writeValue` recurses through `ARRAY`, `MAP` and nested `STRUCT` and calls `writeLogicalValue` per element, so a Debezium `Timestamp` inside an array or struct is converted too.
- **Static warned-key set.** `ConcurrentHashMap.newKeySet().add(...)` is atomic, so two tasks racing on the same column log once. See IN-03 for growth and key shape.
- **WARN never runs per record and carries no data.** The call sits in the DESCRIBE loops only, with `database`, `table`, `column` and raw type as the four `{}` arguments. The `isSubcolumn()` check runs before the regex and the key build, so unflagged and sub-column rows pay for neither.
- **Hook placement.** In both `describeTableV1` and `describeTableV2` the hook is after the alias/materialized/ephemeral skip, after the `shouldSkipDescribeRow` structural skip, and before `fieldDescriptor.hasDefault()` and `Column.extractColumn`.
- **WARN wording against D-15.** It states that values may be stored as NULL or shifted with no error, that a null entry fails the batch with `DataException`, and points at FORK-DELTA Known costs and backlog 999.1. Plan 15-06 owes the Known costs entry it points at.
- **Fork-anchor strings.** `"io.debezium.time.Timestamp"`, `"io.debezium.time.NanoTimestamp"` and `static boolean hasNullableEnumOrTuple(` each occur exactly once in their file; no new comment repeats an anchored string.
- **Renamed unit test.** `cdcDebeziumTimestampsAreDecimalSecondsAndZonedTimestampPassesThrough` still asserts MicroTimestamp (`micro`) and ZonedTimestamp (`zoned`) and gains `millis` and `nanos` with corrected bytes. The old assertion was tightened, not deleted or weakened, and the comment says why.

## Info

### IN-01: The WARN fires on routes where the defect cannot occur

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:490-527`
**Issue:** Table discovery has no knowledge of the insert route. `ClickHouseWriter.doInsertBatch` writes `SCHEMA_LESS` and `STRING_SCHEMA` records through JSON, and `SCHEMA`/`DEBEZIUM_CDC` records through JSON when `bypassRowBinary=true`. Only the RowBinary route omits the null marker. A deployment on those routes with a `Nullable(Enum8)` column still gets the unqualified sentence "The connector writes such slots without their null marker". This follows D-15 as locked (the WARN fires for any such column and cannot see the route), so it is a precision gap in the text, not a deviation.
**Fix:** Deferred. Qualifying the text ("on the RowBinary insert route") is a wording decision against D-15; plumbing the route into `ClickHouseHelperClient` needs edits in `ClickHouseWriter`, outside the D-04 carve-out. FORK-DELTA Known costs (plan 15-06) can state the route restriction.

### IN-02: The V1 describe hook is not exercised by the V2-only target run

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java:395-434`
**Issue:** The live wiring pin goes through `chc.describeTable`, which dispatches on `useClientV2`. With the project's V2 target only `describeTableV2` is run; the 15-01 mutation check also removed the hook from the V2 loop only. A deletion of the hook from `describeTableV1` would pass the default run and fail only under `CLIENT_VERSION=V1`.
**Fix:** Deferred. The suite target is client V2 only (project constraint), and the V1 test run is outside this milestone.

### IN-03: The warned-key set never evicts and its key is delimiter-ambiguous

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:497-518`
**Issue:** `WARNED_NULLABLE_ENUM_OR_TUPLE` lives for the process and only grows. Growth is bounded by distinct flagged (database, table, column, raw type) tuples, so it is a few hundred bytes per flagged column in practice. The key joins its parts with `.` and `:`, so `("a.b","c")` and `("a","b.c")` collide; the consequence is one suppressed WARN, in a case that needs both names to contain dots.
**Fix:** Deferred. No behavioural risk worth a change on a fork that rebases on upstream; a key record or `\u0000` separator would remove the collision if it ever matters.

### IN-04: The WARN wording is not pinned by a test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java:264-324`
**Issue:** The tests pin the predicate, the once-per-key return value and the live registration. None asserts any text of `NULLABLE_ENUM_OR_TUPLE_WARNING`, so the D-15 wording (NULL or shifted, `DataException`, 999.1) can change silently. The 15-01 SUMMARY already marks the wording as a human-judgement deliverable (D3).
**Fix:** Deferred. A text pin would need either a log appender or widening the constant's visibility; the wording is a reviewed-by-reading deliverable.

### IN-05: New constants sit in the middle of the class

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:490-499`
**Issue:** The pattern, the message and the dedup set are declared between `shouldSkipDescribeRow` and `describeTableV1`, while the class keeps its other constants (for example `NETWORK_COMPRESSION_METHOD`, `LZ4`) near the top. Style only.
**Fix:** Deferred. Moving them adds diff churn against the fork's insertion point for no behaviour change, against the rebasability constraint.

### IN-06: Edge-value pins cover the mantissa-less scientific form only

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java:325-336`
**Issue:** `debeziumTimestampEdgeValuesAreExactDecimalSeconds` pins `0E-9`, `1E-9` and `-1E-9`. Values with magnitude 2 to 999 ns render as `1.23E-7`-style numbers, which the unit tests do not pin and no live cell reads back. A server-side change in how `DateTime64` parses exponent numbers would be caught only for the mantissa-less form. The server accepts the forms today (verified in the Summary above). `value.longValue()` on a non-`Long` `Number` would also truncate, as the existing `Timestamp` and `MicroTimestamp` cases do; the Connect `INT64` contract makes a `Long` the only reachable type.
**Fix:** Deferred. An additional `@CsvSource` row (for example `io.debezium.time.NanoTimestamp, 123, 1.23E-7`) is possible later and would only add coverage.

---

_Reviewed: 2026-10-10_
_Reviewer: Claude (gsd-code-reviewer, run inline by the plan executor)_
_Depth: standard_

---

## Incremental review: release documents (since 55ff8e74)

**Reviewed:** 2026-10-10
**Depth:** standard
**Diff base:** `55ff8e7429086dabec27305b16fcb36e254cac0b`
**Files Reviewed:** 4 (`CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md`)
**Status:** issues_found (0 critical, 3 warning, 4 info; IDs continue after the findings above, so WR-01..WR-03 and IN-07..IN-10)

### Summary

These four files are the operator-facing documents of the published `v1.7.0_gg1`. I checked each concrete claim against the tree and ran the operator SQL against a real server.

Verified correct, with no finding:

- **Versions and coordinates.** `VERSION` is `v1.7.0_gg1`. The catalog values in `gradle/libs.versions.toml` match CHANGELOG and FORK-DELTA (`lz4Java` 1.12.0, Jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0, client 0.9.6, Connect API 4.1.1). The "from" values (`lz4` 1.10.2 via the fork's capability rule, Jackson 2.22.2, okhttp 5.4.0, org.json 20260522, mockito 5.23.0) match `v1.6.0_gg1`. Upstream's `lz4Java = 1.10.4` and `kafka = 2.7.0` match `v1.7.0`.
- **Git facts.** Tag `v1.7.0_gg1` is the merge commit `f328fd21`, whose first parent is `c61d90ce`. Tags `pre-v1.7.0-sync` and `v1.6.0_gg1` both resolve to `c61d90ce`. `git revert -m 1 <merge>` is therefore the correct command for item 8.
- **Source facts.** Config keys `insertFormat`, `bypassRowBinary`, `exactlyOnce`, `zkDatabase` (default `connect_state`), `enableDbTopicSplit`, `dbTopicSplitChar`, `targetTableFilter`, `setCompressionMethod` and its `set_compression_method` alias, and `jdbcConnectionProperties` all exist as written. The state table DDL, the `<topic>-<partition>` key, the db-split prefix and `BEFORE_PROCESSING`/`AFTER_PROCESSING` match `KeeperStateProvider` and `Processing`. The Int256 message, the nested-null message, the `Column {} is not nullable and no value is provided` log line, the WARN text, the MBean name format and the client name `clickhouse-kafka-connect/v1.7.0_gg1` match the source. Explicit `insertFormat=json` is matched case-insensitively after trim. The compression behaviour matches `ClickHouseHelperClient` (lz4 setting gated on `setCompressionMethod`; `compress=false` unconditional on V1 node and V2; operator options applied after on V2).
- **Counts.** `shouldSkipTableMetadata` has 8 occurrences in `ClickHouseWriter.java` (definition plus 7 reads). The raw-numbers table reproduces exactly with the four `git diff --shortstat` commands (84/9476/451, 95/13587/484, 30/1368/98, 31/1826/130). 38 of 63 upstream `src/main` files are byte-identical; 25 differ; 6 are added. `Table`, `Processing` and `KeyToValue` are byte-identical to `v1.7.0`. The `jacocoForkReport` class list has 15 classes. There are 7 `@Disabled` sites. The ledger holds L-01..L-22. Every cited planning file exists. Internal markdown anchors resolve.
- **SQL.** I ran the item 1 state query, the item 2 `DELETE FROM ... WHERE key IN (...)` and `TRUNCATE TABLE`, and the item 4, 5, 6 and 7 queries on ClickHouse 26.9.14.10 with `keeper_map_path_prefix` set. All parse and behave as the text says (state query returns only non-`AFTER_PROCESSING` rows; the lightweight DELETE and TRUNCATE work on KeeperMap). `system.settings_profile_elements` has every column item 6 selects.
- **Secrets and identity.** No secrets, hostnames, addresses or production identifiers appear in the four files; every environment value is a placeholder.

### Warnings

#### WR-01: Item 1 and item 2 treat a shared KeeperMap table as if it were per-connector

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:52-70` and `:98-106`
**Issue:** The state table is named by `zkDatabase` (default `connect_state`) and backed by `zkPath` (default `/kafka-connect`). Its key is only `<topic>-<partition>` (optionally db-prefixed); it carries no connector identity (`KeeperStateProvider.init`, lines 78-81 and 99). With defaults, every `exactlyOnce` connector that uses the same ClickHouse database shares one table.

Three consequences for the commands as written:

1. The item 1 query lists the rows of every connector on that table, not of the one being upgraded. The text tells the operator to stop only "the connector". Any other connector that is still running keeps producing transient `BEFORE_PROCESSING` rows, so the result may never be empty and the instruction "Repeat until it is empty" cannot terminate. A stale row for a deleted topic or a partition nobody consumes any more has the same effect, and the text offers no way out of that loop.
2. The item 2 example comment says "clear every state row of this connector's table", and the example is `TRUNCATE TABLE \`<zkDatabase>\``. On a shared table this deletes the state of unrelated connectors. Their next batch is then treated as new, which defeats the exactly-once state machine for any of them that had a batch in `BEFORE_PROCESSING`: that batch can be re-sent as a different block, so deduplication does not apply.
3. The `DELETE` example uses `'<topic>-<partition>'` keys only. With `enableDbTopicSplit=true` the stored key is `<db><dbTopicSplitChar><topic>-<partition>` (item 1 says so, item 2 does not), so the example deletes nothing for those connectors, after which the replayed ranges are silently dropped as `AFTER_PROCESSING`.

**Fix:** Scope the item 1 query to the connector's topics and tell the operator what to do with leftovers:

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND startsWith(`key`, '<topic>-')   -- repeat per topic; add the '<db><dbTopicSplitChar>' prefix when enableDbTopicSplit=true
ORDER BY `key`;
```

State that the table is shared by every connector using the same `zkDatabase` and `zkPath` unless they are set apart, that rows for topics nobody consumes any more can be ignored after checking them, and drop the `TRUNCATE` example, or mark it as safe only when this connector is the sole user of the table. Use the prefixed key in the item 2 `DELETE` example.

#### WR-02: The `date_time_input_format` default is stated as `best_effort` without a version

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:121` and `:143-149`; `docs/FORK-DELTA.md:134`
**Issue:** Item 3 says Debezium `ZonedTimestamp` strings "parse when the server's `date_time_input_format` is `best_effort` (the default)", and the self-check reads two rows with `changed = 0` as "the server defaults (`best_effort` and `1`) apply". I reproduced `best_effort`, `changed = 0` on 26.9.14.10, which is the only version this was probed on. The connector supports servers back to 22.5, and ClickHouse's documented default for this setting on older releases is `basic`. On such a server an operator sees `basic` with `changed = 0`, is told the defaults are fine, and is not told that `ZonedTimestamp` strings will be rejected. The check keys on `changed` when the decision depends on `value`.
**Fix:** Decide on `value`, not on `changed`, and qualify the default by version:

> `ZonedTimestamp` strings parse only when `date_time_input_format` is `best_effort` (the default on ClickHouse 26.9, the version probed; older servers default to `basic`). If the query returns `basic`, whether changed or not, those strings are rejected.

Apply the same qualification to FORK-DELTA Known costs, item 4, L-05 (line 134).

#### WR-03: The `version=*` wildcard advice is wrong for the regex-based consumers the same sentence names

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:279` and `:292`
**Issue:** Item 7 tells the operator to "match `version=` by wildcard (for example `version=*`) in dashboards, alerts and exporter rules". `version=*` is valid only as a JMX `ObjectName` property-value pattern. JMX exporter rules, Prometheus relabel rules and most dashboard matchers use regular expressions, where `version=*` means "version followed by zero or more equals signs". A rule copied from the example silently stops matching the bean, so a dashboard goes blank or an alert stops firing, with no error. The text also does not say that the other properties (`connector=`, `name=`, `topic=`) must be present or covered by `,*` for an `ObjectName` pattern to match.
**Fix:** Give one example per matcher type and name the type:

> JMX `ObjectName` pattern: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask*,version=*,*`. Regex-based rule: `version=.*`.

### Info

#### IN-07: Item 2's recovery steps are not qualified by `exactlyOnce`, and the closing sentence is logically loose

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:93-108`
**Issue:** "Both steps are needed" is unconditional, but step 2 (clearing KeeperMap) applies only to connectors running with `exactlyOnce=true`; CHANGELOG says so, the rollout item does not. For a connector without `exactlyOnce` there is no state table and the `DELETE` fails with `UNKNOWN_TABLE`. The sentence "Skipping this step makes the connector see the replayed ranges as already stored ..." appears twice (lines 96 and 108). The last sentence, "If you use neither exactly-once nor an explicit `insertFormat=json` with schema'd records, there is nothing to recover", reads as if exactly-once alone were a reason to recover; the only condition that matters is an explicit `insertFormat=json` with schema'd records, and exactly-once decides only whether step 2 is needed.
**Fix:** Open with "Step 1 always; step 2 only for connectors with `exactlyOnce=true`", delete the duplicate sentence, and end with "If no connector combines an explicit `insertFormat=json` with schema'd records, there is nothing to recover."

#### IN-08: Item 8 omits the re-merge consequence of `git revert -m 1`

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:306-310`
**Issue:** Reverting a merge commit on `custom_build` records that the merged history is "undone". A later merge of `sync/v1.7.0` (or of upstream `v1.7.0` changes already contained in it) will not bring back the reverted content until the revert itself is reverted. Anyone who rolls back and later wants to roll forward again will find the release silently missing.
**Fix:** Add one line: "To go forward again, revert the revert commit first; merging `sync/v1.7.0` again without that brings nothing back."

#### IN-09: FORK-DELTA still speaks of the `v1.7.0_gg1` tag as future, and the skip-count command does not print the totals it claims

**File:** `docs/FORK-DELTA.md:94` and `:153-156`
**Issue:** Line 94 says the after-figures "can be refreshed ... once the `v1.7.0_gg1` tag exists". The tag exists (it is the merge `f328fd21`), and the four commands now reproduce the table exactly, so the sentence is stale. Lines 154-155 show `grep -rc '@Disabled' src/test/java -> 7 annotation sites`; `grep -rc` prints one count per file, not a total, so the operator-visible output is not what the arrow shows (the total is 7, summed over five files).
**Fix:** Replace "once the tag exists" with "the commands below reproduce them against the tag". Use `grep -rho '@Disabled' src/test/java | wc -l` and `grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l`.

#### IN-10: Planning-internal identifiers appear in operator-facing text without a path, and the item 6 self-check pattern is broader than its explanation

**File:** `CHANGELOG.md:1-48` (for example lines 17, 34), `docs/ROLLOUT-v1.7.0_gg1.md:13` and `:241`
**Issue:** CHANGELOG uses `R-01`, `WR-02 strictness` and `13-REVIEW WR-02` with no explanation. A reader of a published release note, who may not have the planning directory, cannot resolve them. The rollout note at line 13 says ledger entries live "in this repository's planning directory" but gives no path (`.planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`). The item 6 `grep` includes the bare alternative `set_compression_method`, which matches the alias with any value including `true`, while the text after it treats a non-empty result as risk only for `compress=true` together with `setCompressionMethod=false`. CHANGELOG also lists the JMX `version=` change and the new client name nowhere, although item 7 treats them as an upgrade item, and it states the lz4 bump without the reason (FORK-DELTA records that `1.11.2` and `1.11.3` are affected by three advisories).
**Fix:** Replace the bare IDs in CHANGELOG with plain descriptions, or add the ledger path once in the preface; narrow the item 6 pattern to `set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false`; add one CHANGELOG line each for the MBean version segment and client name, and for the advisory reason of the lz4 bump.

---

_Incremental review: 2026-10-10_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
