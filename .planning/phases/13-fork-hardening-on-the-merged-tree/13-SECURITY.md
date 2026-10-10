---
phase: "13"
slug: "fork-hardening-on-the-merged-tree"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-10"
---

# Phase 13 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time. `13-01-PLAN.md` through `13-08-PLAN.md` each carry a parseable
`<threat_model>` block. This is a **retroactive** audit run from plan 15-04 (D-09). It verifies each
declared mitigation against the code as it stands today (`sync/v1.7.0`, HEAD `7117ccda`), not against
the plan's intent, and adds the one threat the audit surfaced after the plans were written (T-13-19).
**No file under `src/main/` was modified by this audit.**

The same threat ID `T-13-SC` appears in all eight plans (identical text, disposition `accept`); it is
recorded once.

Audit method: the `gsd-security-auditor` sub-agent could not be spawned (the executor has no sub-agent
tool), so the audit ran inline, following `secure-phase.md` and the Phase 11 precedent. Three kinds of
evidence back every verdict below: a read of the current source with line numbers, the named pinning
test, and measurements taken on the audit day (2026-10-10).

Measurements taken on the audit day:

- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` over `MetadataFreeJsonWriterTest`,
  `NestedNullableCompositeWriteTest`, `RowBinaryUnsupportedTypeGateTest`,
  `ClickHouseHelperClientForkUnitTest`, `ColumnArrayRecursionTest`,
  `ClickHouseBaseTearDownAttributionUnitTest` and `ClickHouseSinkConfigUsernameDefaultUnitTest`:
  128 tests, 128 succeeded, 0 failed, 0 skipped.
- `./gradlew test --rerun` over `NestedNullRowBinaryLiveTest`, `MetadataFreeJsonTypeParityLiveTest` and
  `ClickHouseSinkTaskJsonInsertFormatTest` against `clickhouse/clickhouse-server:latest` with client V2:
  21 tests, 21 succeeded, 0 failed, 0 skipped.
- An adversarial probe of `MetadataFreeJsonWriter` (compiled from the current source into a scratch
  directory outside the repository; nothing was committed). Input: a field name, a map key and bytes and
  string values all containing `"`, `\`, a newline and `0x01`, shaped to break out of the JSON string
  (`a":1,"x\`, `"},{"injected":"1\`). Result for `structRow`: output
  `{"a\":1,\"x\\\n\u0001":"\"},{\"injected\":\"1\\","m":{...},"str":"..."}`, which Jackson 2.22.3 parses
  back to exactly 3 top-level fields; the field name, the map key and the value each round-trip equal to
  the input; no raw byte below `0x20` in the output. For `cdcRow`: 1 field, key and value round-trip
  equal. Byte `0xFF` is emitted verbatim by design (JSONP-06).
- `git diff --name-only e4755e386b66aa51d0890249d7800c6efd7a8038 HEAD -- src/main` lists exactly
  `MetadataFreeJsonWriter.java` and `ClickHouseHelperClient.java` (the Phase 15 D-01 and D-02 changes,
  outside this audit's scope and covered by Phase 15's own threat models).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka record → connector JSON writer | Struct field values (arbitrary bytes and strings), field names and map keys are untrusted and are serialised into a JSONEachRow body | record fields |
| Connector → ClickHouse HTTP insert | The JSON body is parsed by the server; malformed escaping would let record data restructure the row | row bytes |
| Kafka record → RowBinary stream | Untrusted nulls in nested slots used to desync the binary stream | record fields |
| Connector exception → logs, Connect status, DLQ headers | Exception text crosses into operator-visible and third-party-visible stores | message text |
| ClickHouse DESCRIBE output → connector table model | Server-described column names, including user-chosen JSON path names with dots, shape the Column tree | column metadata |
| Connector config → Connect REST validation | ConfigDef defaults are displayed to operators | config defaults |
| Test code → shared ClickHouse container | Test lifecycles must release MBeans and tables; the per-class drop is the only isolation between classes | test tables |
| Planning artifacts → published history (Phase 15) | The ledger is published with `.planning` history | bookkeeping text |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-13-01 | Tampering | `MetadataFreeJsonWriter` bytes/string encoding | high | mitigate | Verified. Bytes go through `JsonGenerator.writeUTF8String` (`MetadataFreeJsonWriter.java:258` for `byte[]`, `:263` and `:267` for `ByteBuffer`), which escapes quote, backslash and control bytes on the UTF-8 byte generator that `writeValueAsBytes` (`:84`, `:89`) provides. Field names (`:102`, `:118`, `:182`) and map keys (`:159`) go through `writeFieldName`, which escapes the same set. No string concatenation builds JSON and no raw-write variant is called. Pinned by `MetadataFreeJsonWriterTest#quoteAndBackslashAreEscaped`, `#controlBytesNeverAppearRaw`, `#highBytesAppearVerbatim`, `#invalidUtf8SequencesAppearVerbatim`. Measured on the audit day: the adversarial probe above returned the expected field count (3 and 1) with every injected name and value round-tripping. Bytes `>= 0x80` can never equal quote (`0x22`) or backslash (`0x5C`), so verbatim high bytes cannot terminate a string. | closed |
| T-13-02 | Information Disclosure | `writeRecordsJson` logging, D-19 skip log | medium | mitigate | Verified. The payload is logged only inside `writeJsonRow` behind `LOGGER.isTraceEnabled()` (`ClickHouseWriter.java:1417`). The D-19 skip log is `LOGGER.debug` and interpolates topic, partition and offset only (`:1369-1372`). `MetadataFreeJsonWriter` has no logger. | closed |
| T-13-03 | Tampering (integrity) | logical-type encodings | high | mitigate | Verified. Timestamp, Date, Time, Decimal and the Debezium Date/MicroTimestamp encodings are numeric and time-zone free (`MetadataFreeJsonWriter.java:194-242`); `java.sql.Date` goes through `toLocalDate().toEpochDay()` (`:221`). Pinned by `MetadataFreeJsonWriterTest#timestampIsUnquotedDecimalSecondsWithThreeFractionDigits`, `#dateIsUnquotedIntegerEpochDays`, `#cdcDebeziumDateIsEpochDayUnderAnyTimeZone` (parameterised over zones), `#debeziumTimestampEdgeValuesAreExactDecimalSeconds`, and by RowBinary parity in `MetadataFreeJsonTypeParityLiveTest` (live, passed on the audit day). The file also carries the Phase 15 D-01 Timestamp/NanoTimestamp cases (`:225-242`); they follow the same pattern and are covered by Phase 15's threat models. | closed |
| T-13-04 | Repudiation (exactly-once) | per-record serialisation | medium | mitigate | Verified. `MAPPER` is a private final copy (`:62`); every other static field is a final constant. Pinned by `MetadataFreeJsonWriterTest#structRowIsDeterministic`, `#cdcRowIsDeterministic`, `#writerHasOnlyFinalStaticFields` and `#sharedMapperIsNeverMutated`. | closed |
| T-13-05 | Tampering (integrity) | parity comparison | high | mitigate | Verified. `MetadataFreeJsonTypeParityLiveTest` asserts the RowBinary reference row count before comparing (`:418`, `:467`, `:549`, `:608`, `:838`) and the candidate against the reference (`:302`); literal anchors sit on the reference side only and byte cells compare through `hex(...)` (`:251-254`). Passed on the audit day. | closed |
| T-13-06 | Tampering | test SQL construction | low | accept | Accepted — see AR-01. Re-measured: `MetadataFreeJsonTypeParityLiveTest` builds table names through `createTopicName` (9 uses) and the DDL strings are static `%s` templates; record values never enter SQL text. | closed |
| T-13-07 | Information Disclosure | `buildNestedNullMessage` / DataException text | high | mitigate | Verified. `ClickHouseWriter.java:554-566` builds the message from the static `NESTED_NULL_MESSAGE_FORMAT`, `container.getName()` and `renderColumnType(container)` (column metadata only). The four throw sites (`:688`, `:716`, `:736`, `:763-764`) pass only a slot kind and, for a tuple field, the field name from the schema. Pinned by `NestedNullableCompositeWriteTest` (`SECRET_ELEMENT` at `:131`, `SECRET_MAP_KEY` at `:132`; `assertFalse(ex.getMessage().contains(...))` at `:197` and `:231`) and by the live `NestedNullRowBinaryLiveTest`, both passed on the audit day. | closed |
| T-13-08 | Denial of Service | poison batch with a nested null | medium | accept | Accepted — see AR-02. Re-verified: the behaviour is recorded as ledger L-10, and pinned by `NestedNullRowBinaryLiveTest#nestedNullInLastRecordFailsTheWholeBatch` and `#nestedNullUnderToleranceAllSendsWholeBatchToDlq`. Phase 15 adds the D-02 startup WARN and the rollout-doc self-check for the slots where the check cannot distinguish a null from a non-null value. | closed |
| T-13-09 | Tampering (integrity) | nested null written as a stray marker byte | high | mitigate | Verified. The container loops check before writing: MAP value (`:685-692`), ARRAY element (`:714-720`) and TUPLE slot and field (`:731-737`, `:753-765`) either throw `DataException` or write an empty container, and the marker byte is written only for a genuinely nullable slot (`:693-695`, `:721-723`). Byte-level pins in `NestedNullableCompositeWriteTest` and live read-back in `NestedNullRowBinaryLiveTest#nullContainersAreCoercedToEmptyWithoutDefaults` / `#...WithDefaults`, passed on the audit day. | closed |
| T-13-10 | Information Disclosure | `validateDataSchema` INT256/UINT256 message | medium | mitigate | Verified. `ClickHouseWriter.java:333-338` reuses `buildRowBinaryUnsupportedTypeMessage(colName, type)` (`:546-552`), which formats column name and type only. Pinned by `RowBinaryUnsupportedTypeGateTest` (`secretFieldValue` at `:87` and `:151`, `assertFalse(ex.getMessage().contains(secretFieldValue))` at `:173`), passed on the audit day. | closed |
| T-13-11 | Elevation of Privilege | reflective config injection in tests | low | accept | Accepted — see AR-03. Re-measured: `grep -rn 'setAccessible\|getDeclaredField\|getDeclaredMethod' src/main/java` returns nothing. | closed |
| T-13-12 | Tampering (integrity) | `shouldSkipDescribeRow` ancestor walk | medium | mitigate | Verified. `ClickHouseHelperClient.java:473-487` skips a sub-column only when the row is a structural `.null`/`.size0`/`.size` suffix or an ancestor in `columnsSoFar` is `Type.JSON` or `Type.DYNAMIC`. Pinned by the DESC-05 must-not-widen cases in `ClickHouseHelperClientForkUnitTest` (Tuple, Map, `Array(JSON)` rows kept) — passed on the audit day — and by the live `nt.a.b` assertion. | closed |
| T-13-13 | Tampering (integrity) | `Column.extractColumn` argument order | medium | mitigate | Verified. The Array recursion call is `extractColumn(name, <inner type>, false, hasDefaultValue, isSubColumn, arrayDepth + 1)` (`Column.java:213`), matching the signature `(name, valueType, isNull, hasDefaultValue, isSubColumn, arrayDepth)` at `:193`. Pinned for all four flag combinations by `ColumnArrayRecursionTest` (`flagCombinations`), passed on the audit day. | closed |
| T-13-14 | Information Disclosure | USERNAME ConfigDef default | low | accept | Accepted — see AR-04. Re-measured: `ClickHouseSinkConfig.java:83` `usernameDefault = "default"`, the same value the runtime already used (`:224`); `ClickHouseSinkConfigUsernameDefaultUnitTest` passed. | closed |
| T-13-15 | Denial of Service (test isolation) | `ClickHouseSinkTaskJsonInsertFormatTest` lifecycles | low | mitigate | Verified. Both tests run `chst.stop()` and `dropTable` in a `finally` block (`ClickHouseSinkTaskJsonInsertFormatTest.java`, the two `finally` blocks at `:103` and `:139`); passed on the audit day. | closed |
| T-13-16 | Repudiation | `ClickHouseBase.tearDown` drop failure | medium | mitigate | Verified. `ClickHouseBase.dropSharedDatabaseOrAttribute` (`ClickHouseBase.java:170-178`) catches the failure and rethrows `IllegalStateException` with the class and database names and the cause. Pinned Docker-free by `ClickHouseBaseTearDownAttributionUnitTest`, passed on the audit day. | closed |
| T-13-17 | Information Disclosure | attribution message | low | accept | Accepted — see AR-05. Re-measured: `DROP_FAILURE_MESSAGE_FORMAT` (`ClickHouseBase.java:37-39`) carries a test class name and a generated database name only. | closed |
| T-13-18 | Information Disclosure | `13-BEHAVIOUR-LEDGER.md` | low | mitigate | Verified. The ledger states that it holds test names, decision IDs and message formats only. A case-insensitive scan for `password`, `passwd`, `secret`, `hostname`, `jdbc:`, `api key`, `token` and URL hosts returns only the L-18 row's mention of the `PASSWORD` ConfigDef default being the empty string (a config key name, not a value). No credential, connection string or record data. | closed |
| T-13-19 | Information Disclosure | `ClickHouseWriter.doWriteDate` DateTime-string parse failure logs (new, found by this audit) | medium | mitigate | Found by this audit; not in any plan. `ClickHouseWriter.java:461` and `:515` log `LOGGER.error("Error parsing date time string: {}", value.getObject())`, which writes the record's raw field value at ERROR when a String-typed value cannot be parsed into a DateTime column. This is **not a Phase 13 change**: both lines are present unchanged in upstream `v1.7.0` (`git grep 'Error parsing date time string' v1.7.0 -- src/main`, lines 432 and 486), reached only on the RowBinary DateTime path and only when parsing fails. It contradicts the repository's rule against logging record data (AGENTS.md, Security). Below the `high` block threshold, so it does not count toward `threats_open`. Not accepted here: whether to fix it (a `src/main` change, which D-04 reserves for a user decision), accept it, or leave it for the next upstream sync is the user's call, carried to the approval bundle. | open — below high threshold (non-blocking) |
| T-13-SC | Tampering | package-manager installs (supply chain) | low | accept | Accepted — see AR-06. Re-measured: no Phase 13 plan installs a package; the five dependency version edits are Phase 14. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Totals: 20 threats (T-13-01..19 plus T-13-SC) — 19 closed, 1 open below the block threshold
(T-13-19, medium), **0 open at or above `high`**.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-13-06 | Test-only SQL. Identifiers come from `createTopicName`; record values never enter SQL text, only Kafka records. | plan 13-02 (plan-time disposition), re-measured by audit 15-04 | 2026-10-10 |
| AR-02 | T-13-08 | Fail-the-batch on a nested null is the intended design (D-10, D-12 note): `errors.tolerance=none` stops the task on the first offending batch, `errors.tolerance=all` sends the whole batch to the DLQ. No new retry loop. Recorded in ledger L-10 for the rollout checklist. | plan 13-03 (plan-time disposition), re-verified by audit 15-04 | 2026-10-10 |
| AR-03 | T-13-11 | Reflection is confined to `src/test`; no production code path uses reflection (grep over `src/main/java` returns nothing). | plan 13-04, re-measured by audit 15-04 | 2026-10-10 |
| AR-04 | T-13-14 | `"default"` is ClickHouse's public default user name and already the runtime default; the ConfigDef default carries no secret. | plan 13-05, re-measured by audit 15-04 | 2026-10-10 |
| AR-05 | T-13-17 | The attribution message carries a test class name and a generated test database name only, in test output. | plan 13-07, re-measured by audit 15-04 | 2026-10-10 |
| AR-06 | T-13-SC | No package-manager install task exists in any Phase 13 plan. | plans 13-01..08, re-measured by audit 15-04 | 2026-10-10 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-10 | 20 | 19 | 1 (T-13-19, medium, below the `high` block threshold) | /gsd-secure-phase 13 (retroactive audit, plan 15-04 task 1, run inline; ASVS L1; mitigations verified against code at `sync/v1.7.0` HEAD `7117ccda`) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed (no open threat at or above `high`)
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-10

---

## Carried Forward (non-blocking)

Recorded so this register, the review dispositions and the Phase 15 approval bundle do not drift apart.

- **T-13-19** (open, medium): upstream's two ERROR logs of an unparseable DateTime string, described in the
  register. Needs a user decision only if the user wants it changed before publication; no `src/main`
  change was made.
- **Field-name escaping has no dedicated unit pin.** `MetadataFreeJsonWriterTest` pins escaping for
  byte and string values but not for a field name or map key containing a quote or backslash. The
  behaviour is correct (measured above) and Jackson's `writeFieldName` is the standard path, so this is
  a test-coverage gap, not a threat. A pin would be a test-only addition.
- **13-REVIEW WR-01..WR-05 and IN-01..IN-04** keep their `13-REVIEW-DISPOSITION.md` dispositions
  (WR-02 fixed by 15-01; the rest deferred). None is a security threat: WR-03 is a message-less
  exception (no data disclosed), WR-04 a null-deref that can mask a failure.
- **Phase 15 `src/main` changes** (D-01 `MetadataFreeJsonWriter` Timestamp/NanoTimestamp, D-02
  `ClickHouseHelperClient` WARN) are outside this audit. The D-02 WARN logs database, table, column and
  raw type only — catalog metadata, no record data — and is covered by Phase 15's own threat models.
