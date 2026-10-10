---
phase: "07"
slug: "type-mapping-writer-helper-client-config"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-07"
---

# Phase 07 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time — all four of `07-01-PLAN.md` through `07-04-PLAN.md` carry a
parseable `<threat_model>` block. This is a **retroactive** audit run from plan 11-01 (D-07): it
verifies each declared mitigation against the code as it stands today (`sync/v1.6.0`), and adds the
three threats the live suite and code review surfaced after the plans were written (T-07-20..22).
**No file under `src/main/` was modified by this audit.**

The same threat ID `T-07-SC` appears in all four plans (identical text); it is recorded once.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka record payload → connector serialization | `doWriteColValue` and the messages/log statements built near it receive untrusted, operator-supplied record values | record values |
| Connector → ClickHouse RowBinary stream | A partially-written column desynchronises every following column; the writer is the last place to stop it | binary block |
| Connector → worker log sink / DLQ | Anything named in an exception message or log statement leaves the process | column names, query text |
| Connector configuration → ClickHouse SQL | `targetTableFilter` is embedded into a string literal in the `system.columns` discovery query; `database` is embedded into the same query | operator-supplied strings |
| ClickHouse HTTP response → connector decoding | `DESCRIBE` output, including non-ASCII column identifiers, is decoded by a reader whose charset the connector chooses | column identifiers |
| Connector configuration → connector behaviour | Two operator settings gain parsing here; one previously-forced ClickHouse setting stops being sent | config values |
| Test suite → phase verdict | A disabled or vanished test is indistinguishable from a passing one unless the executed set is inspected | test results |
| Checklist document → publication gate | Phase 11 blocks publication on zero pending rows; a row flipped without evidence removes a gate | bookkeeping claims |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-07-01 | Information Disclosure | `ClickHouseWriter.buildRowBinaryUnsupportedTypeMessage` and call sites | high | mitigate | Verified: the helper's signature is `(String columnName, Type columnType)` (`ClickHouseWriter.java:543`) — it cannot receive a `Data`/`Record`/value. Call sites at `:333` and `:754` pass only `colName`/`col.getName()` and the type. Pinned by `RowBinaryUnsupportedTypeGateTest` (`validateDataSchema…NeverLeaksTheValue`, `assertFalse(ex.getMessage().contains(secretFieldValue))`). | closed |
| T-07-02 | Information Disclosure | `ClickHouseWriter` per-batch payload log statements | medium | mitigate | Verified: every statement that emits record content is `TRACE` — `:416`, `:777`, `:798`, `:948`, `:1298` (guarded by `isTraceEnabled()`), `:1443`. The `DEBUG` statements (`:233`, `:303`, `:347`, `:1195`) name only counts, type names and column names. | closed |
| T-07-03 | Denial of Service | `doWriteColValue` `default:` branch now throwing | low | accept | Accepted — see Accepted Risks Log AR-01. | closed |
| T-07-04 | Tampering | `ClickHouseSinkTask.onPartitionsRevoked` losing `@Override` | low | accept | Accepted — see Accepted Risks Log AR-02. | closed |
| T-07-05 | Tampering | `ClickHouseHelperClient.buildTableFilterClause` embedding the operator filter in the `system.columns` query | high | mitigate | Verified, and **strengthened after the plan**: the plan-time mitigation (double every quote) was insufficient — Phase 7's gap-closure found a `x\' OR 1=1-- ` backslash-before-quote breakout (`07-REVIEW.md` CR-01). Fixed in `2c6a0f56`: `ClickHouseHelperClient.java:346` (`BACKSLASH_RUN_BEFORE_QUOTE_OR_END`) pads any backslash run before a quote, or before the closing quote, to an even length *before* the quote is doubled (`:365-377`). Clause emitted only when the filter is non-empty. Pinned Docker-free by `ClickHouseHelperClientForkUnitTest` (`singleQuoteFilterIsEscapedExactly`, `multipleSingleQuotesAreEachEscaped`, `backslashImmediatelyBeforeQuoteIsNeutralizedBeforeDoubling`, `trailingBackslashIsNeutralizedAgainstTheClosingQuote`, `evenBackslashRunBeforeQuoteIsUntouched`). Reaches a read-only `SELECT` on `system.columns` only. | closed |
| T-07-06 | Information Disclosure | the client's query-retry message | medium | mitigate | Verified: `grep -rn 'System\.out\|System\.err\|printStackTrace' src/main` returns nothing. Retry path logs through SLF4J — query text at `DEBUG` (`ClickHouseHelperClient.java:322`), retry count at `WARN`. | closed |
| T-07-07 | Tampering | `describeTableV2`'s `DESCRIBE TABLE` (and `showTables`' `database = '%s'` literal) built by interpolating database and table names | medium | accept | Accepted — see Accepted Risks Log AR-03. Extended by this audit to also name the `showTablesV1/V2` `database` literal (`:397`, `:416`), which has the same provenance. | closed |
| T-07-08 | Tampering | platform-default charset decoding of `DESCRIBE` output | medium | mitigate | Verified: the only `InputStreamReader` in `src/main` is `ClickHouseHelperClient.java:558`, constructed with `StandardCharsets.UTF_8`. | closed |
| T-07-09 | Denial of Service | per-row `Table` construction inside a describe loop | low | mitigate | Verified: `describeTableV2` accumulates into a loop-local `Map<String, Column> columnsByName` and constructs `Table` once after the loop (O(1) parent lookup in `shouldSkipDescribeRow`). | closed |
| T-07-10 | Repudiation | the four `@Disabled` JSON-path test classes | medium | mitigate | Verified and **superseded**: the four pending-decision disables were re-enabled by Phase 9 plan 09-02 (delete-only). `grep -rn 'Disabled pending\|Pending JSONP-01' src/test` returns nothing; the seven remaining `@Disabled` are upstream/pre-existing and carry their own reasons. No silent disable survives. | closed |
| T-07-11 | Information Disclosure | rewritten JSON coverage in `RowBinaryUnsupportedTypeGateTest` | high | mitigate | Verified: the distinctive-secret absence assertion is present on the JSON case (`:83-89`) and on every unsupported type (`:144-173`, `assertFalse(ex.getMessage().contains(secretFieldValue))`). | closed |
| T-07-12 | Tampering | a default connector no longer sending `async_insert` | medium | accept | Accepted — see Accepted Risks Log AR-04. | closed |
| T-07-13 | Elevation of Privilege | reflective write to the writer's private sink-config field in a test | low | accept | Accepted — see Accepted Risks Log AR-05. | closed |
| T-07-14 | Spoofing | `ClickHouseBase.uniqueConnectorName` | low | accept | Accepted — see Accepted Risks Log AR-06. | closed |
| T-07-15 | Repudiation | flipping `MERGE-CHECKLIST.md` rows | high | mitigate | Verified by re-count: Section 1 holds exactly 12 rows with owning phase 7, all `done`; the Section 1 total is still 40 (5 + 12 + 6 + 17 across owners 6/7/8/10). No other phase's rows were flipped by Phase 7. | closed |
| T-07-16 | Spoofing | a test silently disappearing from the gate while the run stays green | high | mitigate | Verified from `07-04-SUMMARY.md`: the gate was asserted against result files, with the six fork canaries (`JsonDynamicTypeMappingTest`, `RowBinaryUnsupportedTypeGateTest`, `ClickHouseHelperClientForkUnitTest`, `JsonInsertColumnsUnitTest`, `AsyncInsertSettingsUnitTest`, `NewUpstreamConfigDefaultsUnitTest`) named as executed with zero failures. Independently reinforced later: Phase 10's two full-suite runs had byte-identical 57-class executed sets. | closed |
| T-07-17 | Information Disclosure | the defaults test's props map | low | accept | Accepted — see Accepted Risks Log AR-07. | closed |
| T-07-18 | Tampering | the three new Recorded Facts | medium | mitigate | Verified: the Recorded Facts block carries numbered entries each labelled with provenance (`repo-derived` / `operator-confirmed`), and Phase 7's three entries (8, 9 and 10 — the Connect API gap, the map null-marker win, the describe-table database win) are present. | closed |
| T-07-19 | Repudiation | deciding the metadata-free JSON path by omission | high | mitigate | Verified: the checklist's closing deferral paragraph survives and now reads `Superseded (Phase 9, 2026-09-20). Recorded Fact 14 above now supplies that disposition` — the question was settled at Phase 9's blocking-human checkpoint, not by a checklist edit. | closed |
| T-07-SC | Tampering | package-manager installs (supply chain) | high | accept | Accepted — see Accepted Risks Log AR-08. Re-measured: `gradle/libs.versions.toml` was last changed by Phase 6 (`b582a44b`); no Phase 7 commit touches it. | closed |
| T-07-20 | Information Disclosure | `ClickHouseSinkConfig` debug logging of `clickhouseSettings` and `topicToTableMap` (`:351-354`) | low | accept | Accepted — see Accepted Risks Log AR-09. The `PASSWORD` config is `ConfigDef.Type.PASSWORD` (masked by Connect) and is not among the logged values. | closed |
| T-07-21 | Denial of Service | `targetTableFilter` evaluated as a regex by `match(table, '…')` | low | accept | Accepted — see Accepted Risks Log AR-10. | closed |
| T-07-22 | Tampering / Denial of Service (integrity) | `doWriteColValue` MAP/TUPLE/VARIANT cases and `validateDataSchema` default branch dereferencing a null nested value mid-RowBinary stream | medium | mitigate | Surfaced by the live suite after the plans (`07-REVIEW.md` CR-02, WR-03), not in the plan-time register. Verified fixed: `ClickHouseWriter.java:612-` null-guards each case with `BinaryStreamUtils.writeNull(stream)` (`e411353c`, pinned by `NestedNullableCompositeWriteTest`), and `validateDataSchema`'s default branch null-checks `objSchema` (`95e49964`). | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Totals: 23 threats (T-07-01..22 plus T-07-SC) — 23 closed, **0 open**.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-07-03 | Failing the batch on a serializer-less column type is the requirement (WRT-01); the alternative is a silently desynced RowBinary block corrupting every following column. Connect's `errors.tolerance` and DLQ path own the failure. | plan 07-01 (plan-time disposition), confirmed by audit 11-01 | 2026-10-07 |
| AR-02 | T-07-04 | Method body and signature unchanged; `connect-api` is declared `api(...)` so a pre-3.0 worker still binds it. The annotation carried no runtime behaviour. Rationale recorded in-file at `ClickHouseSinkTask.java:150-153`. | plan 07-01, confirmed by audit 11-01 | 2026-10-07 |
| AR-03 | T-07-07 | `DESCRIBE TABLE` and the `showTables` `database` literal interpolate names that originate from the connector's own topic-to-table mapping, the configured database, and the server's own `system.columns` output — never record payloads. Pre-existing upstream construction; changing it reaches outside this phase's scope. Residual: a Connect administrator who can set `database` can already point the connector at any database with any credential. | plan 07-02, confirmed by audit 11-01 | 2026-10-07 |
| AR-04 | T-07-12 | Restores the fork's pre-merge production behaviour (CFG-02). `async_insert` is not a security control. `AsyncInsertSettingsUnitTest` pins that an explicit operator value is honoured. | plan 07-03, confirmed by audit 11-01 | 2026-10-07 |
| AR-05 | T-07-13 | Test-source only; mirrors an accessor pattern already in `ClickHouseWriterTest`; no production code path gains reflective access. | plan 07-03, confirmed by audit 11-01 | 2026-10-07 |
| AR-06 | T-07-14 | Test isolation aid for MBean names, not an authentication or authorization control. | plan 07-03, confirmed by audit 11-01 | 2026-10-07 |
| AR-07 | T-07-17 | The defaults test carries a localhost host, default database and empty password; no connection is opened. | plan 07-04, confirmed by audit 11-01 | 2026-10-07 |
| AR-08 | T-07-SC | No package-manager install task exists in any Phase 7 plan, and `gradle/libs.versions.toml` is unchanged since Phase 6. `build.gradle.kts` was touched by `b25a71f1` for a test-compile exclusion only (no dependency), later removed in Phase 10. | plans 07-01..04, confirmed by audit 11-01 | 2026-10-07 |
| AR-09 | T-07-20 | `clickhouseSettings` holds ClickHouse server settings (not credentials by convention) and is logged at `DEBUG`, an operator-chosen level. The password is neither in that map nor logged. Residual: an operator who places a secret inside `clickhouseSettings` and enables `DEBUG` will see it logged. | audit 11-01 (new) | 2026-10-07 |
| AR-10 | T-07-21 | `match()` runs server-side under ClickHouse's RE2 engine (linear-time, no backtracking), the value is operator-supplied connector config, and the query reads only `system.columns`. | audit 11-01 (new) | 2026-10-07 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-07 | 23 | 23 | 0 | /gsd-secure-phase 07 (retroactive audit, plan 11-01 task 3; ASVS L1; mitigations verified against code at `sync/v1.6.0`) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-07

---

## Carried Forward (non-blocking)

Recorded here with their existing dispositions so this register and `07-REVIEW.md` / `STATE.md` do not
drift apart. None is a security threat and none is minted as a new open threat; all were published
open by decision D-08.

- **`07-REVIEW.md` WR-01** — `Column.extractColumn`'s `Array` recursion swaps the `hasDefaultValue`/`isSubColumn` positional arguments. Currently dead code; a future landmine, not a present exposure.
- **`07-REVIEW.md` WR-02** — the `ConfigDef` default for `USERNAME` is `passwordDefault` (`""`) instead of `usernameDefault` (`"default"`) (`ClickHouseSinkConfig.java:433-435`). Misreports through Connect's config UI/docs only; runtime reads `props.getOrDefault(USERNAME, usernameDefault)` (`:218`) and is unaffected.
- **`07-REVIEW.md` IN-03** — `JsonColumnTest` asserts row counts only, never the JSON payload content.

Observation outside this phase's surface (not a finding of this audit): `KeeperStateProvider.java:140`
builds its state-table `INSERT` with `String.format` over topic/partition-derived keys. The file is
byte-identical to upstream `v1.6.0` (re-measured in plan 11-01 task 2) and Kafka topic names are
restricted to `[a-zA-Z0-9._-]`; it is upstream's construction and out of Phase 7's scope.

No audit mitigation requiring a `src/main/` change was proposed.
