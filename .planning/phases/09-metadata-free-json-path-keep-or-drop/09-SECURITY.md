---
phase: "09"
slug: "metadata-free-json-path-keep-or-drop"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-07"
---

# Phase 09 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time — `09-01-PLAN.md`, `09-02-PLAN.md` and `09-03-PLAN.md` each
carry a parseable `<threat_model>` block. This is a **retroactive** audit run from plan 11-01 (D-07):
it verifies each declared mitigation against the code as it stands today (`sync/v1.6.0`) and adds the
two threats the code review and this audit surfaced after the plans were written (T-09-15, T-09-16).
**No file under `src/main/` was modified by this audit.**

The same threat ID `T-09-SC` appears in all three plans (identical text); it is recorded once.

**R-01 is not minted as a new threat.** The silent-data-loss behaviour on the `Struct` + explicit
`insertFormat=json` route is the plan-time threat **T-09-03**, already dispositioned `accept` by an
explicit developer decision (D-01, KEEP) taken at a blocking-human checkpoint. It is recorded below with
that disposition and cross-referenced to `MERGE-CHECKLIST.md` Recorded Fact 14.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka record payload → connector | Under the bypass, untrusted field names and values are no longer filtered against real table columns | record fields |
| Connector config → ClickHouse SQL | `topic2TableMap` values become the synthetic `Table`'s name; `jsonInsertColumns` values become the INSERT column clause | operator-supplied identifiers |
| Connector → ClickHouse (privilege) | The bypass exists because the connector user may lack `DESCRIBE`/`SHOW` on the target tables | none added or removed |
| Test suite → disposable ClickHouse containers | Live tests create and drop real tables; no production data or credential crosses | test rows |
| Acceptance gate → phase closure | The only signal that Phase 9's production change is correct | test results |
| Planning record → future reader | `MERGE-CHECKLIST.md` and `REQUIREMENTS.md` are what Phase 11's gate and any future sync reader consult | bookkeeping claims |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-09-01 | Tampering | `ClickHouseWriter.getTable` synthetic `Table` name from `topic2TableMap` | medium | mitigate | Verified: `ClickHouseWriter.java:1622` builds the synthetic `Table` from `Utils.getMappedOrTopicTableName(...)`; `Table.getName()` (`Table.java:69-70`) returns `Utils.escapeName(name)`, which strips every backtick before quoting (`Utils.java:25-28`), so no identifier is built by concatenation and a backtick breakout is not possible. | closed |
| T-09-02 | Information Disclosure | the two new guard log lines in `start` and `updateMapping` | low | mitigate | Verified: `ClickHouseWriter.java:126` and `:148` are static strings — no record payload, table data or credential interpolated. | closed |
| T-09-03 | Repudiation / Integrity (CWE-390) | `doInsertRawBinaryV2` / `writeRecordsRowBinary` reading the synthetic `Table`'s empty column list — **this is R-01** | high | accept | Accepted by explicit developer decision D-01 (KEEP), taken at a blocking-human checkpoint and re-confirmed before the first production edit — see Accepted Risks Log AR-01 and `MERGE-CHECKLIST.md` Recorded Fact 14. Detection contract verified present and unchanged: `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` (`:305`, Javadoc `R-01 PIN - NOT AN ENDORSEMENT` at `:279`), executed in Phase 9's 645-test and Phase 10's 654-test green runs. | closed |
| T-09-04 | Elevation of Privilege / Access Control | the guards skipping metadata acquisition | low | accept | Accepted — see Accepted Risks Log AR-02. | closed |
| T-09-05 | Tampering / Injection | `jsonInsertColumns` reaching the INSERT column clause | low | mitigate | Verified as planned: `ClickHouseWriter.java:1391-1394` passes the typed `List<String>` straight to `client.insert(table.getName(), insertColumns, …)`; connector code builds no SQL string, and an unknown column stays a loud server-side `NO_SUCH_COLUMN_IN_TABLE`. **The planned premise is only half true** — the client does not escape the names (see T-09-16). | closed |
| T-09-06 | Denial of Service | `isUpdateMappingRunning` in `ClickHouseWriter.updateMapping` | high | mitigate | Verified: the guard `return false` at `ClickHouseWriter.java:147-150` precedes `compareAndSet(false, true)` at `:153`, so the flag is never set-and-abandoned; the `finally` at `:170` clears it on the normal path. Pinned by `MetadataFreeJsonInsertUnitTest.updateMappingUnderExplicitJsonShortCircuitsAndLeavesTheInProgressFlagClear`. | closed |
| T-09-07 | Repudiation | the D-12 acceptance gate | high | mitigate | Verified from `09-VERIFICATION.md` (`status: passed`): `./gradlew test` 645 tests / 0 failures / 0 errors / 18 skipped, with per-class result XML read independently. Reinforced by Phase 10's two consecutive clean full runs (654 tests each). | closed |
| T-09-08 | Repudiation / Integrity (CWE-390) | R-01's detection contract | high | mitigate | Verified: the named pin test exists and is not `@Disabled` (`grep` over `src/test` shows no `Disabled` on `ExplicitJsonInsertPathLiveTest`); `09-VERIFICATION.md` confirms it remains present and executed. | closed |
| T-09-09 | Information Disclosure | test output and the tee'd suite log | low | mitigate | Verified: `grep -ciE 'password\|hostname\|secret'` over `09-01/02/03-SUMMARY.md` returns 0 each; the summaries carry counts and class names only. | closed |
| T-09-10 | Tampering | `TableTest` assertion change | low | mitigate | Verified: commit `b84b179e` changes exactly one file (`TableTest.java`, 3 insertions / 1 deletion), zero production files. | closed |
| T-09-11 | Repudiation | the recorded KEEP disposition | high | mitigate | Verified: Recorded Fact 14 names R-01, the missing `DESCRIBE`/`SHOW` grant, the conflict-policy departure and the costly reversibility ("a production migration for the affected connectors, not a code revert"). | closed |
| T-09-12 | Tampering | existing checklist and requirement prose | medium | mitigate | Verified: Recorded Fact 13 is still present (now marked resolved), and the checklist's closing deferral paragraph is preserved with an appended `Superseded (Phase 9, 2026-09-20)` note — annotated, not deleted. | closed |
| T-09-13 | Spoofing | requirement closure claims | medium | mitigate | Verified by re-read of `REQUIREMENTS.md`: JSONP-01..04 are `Complete`; SUITE-01..03 and CI-05..07 (later-phase rows) are still `Pending`. | closed |
| T-09-14 | Information Disclosure | planning documents committed to the repository | low | mitigate | Verified: Recorded Fact 14 describes configuration shape and grant posture in general terms — no hostname, credential, table name, topic name or customer identifier. | closed |
| T-09-SC | Tampering | package-manager installs (supply chain) | low | accept | Accepted — see Accepted Risks Log AR-03. Re-measured: no Phase 9 commit touches `gradle/libs.versions.toml`, `build.gradle.kts` or `settings.gradle.kts`; the catalog was last changed in Phase 6 (`b582a44b`). | closed |
| T-09-15 | Tampering | `ClickHouseWriter.doInsert`'s `auto.evolve` branch under the metadata-free bypass | medium | mitigate | Surfaced by code review after the plans (`09-REVIEW.md` CR-01), not in the plan-time register: the one guard site plan 09-01 missed would `ALTER TABLE` off the synthetic Table's empty column list and then fall through to `describeTable`. Verified fixed: `ClickHouseWriter.java:210` reads `csc.isAutoEvolve() && !shouldSkipTableMetadata()` and `:222-226` logs a warning on the skipped path (`51baea62`). Pinned by `MetadataFreeJsonInsertUnitTest.autoEvolveIsSkippedUnderExplicitJsonMetadataFreeMode` and `autoEvolveStillRunsUnderNonExplicitInsertFormat`. | closed |
| T-09-16 | Tampering / Injection | the client splicing `jsonInsertColumns` into the INSERT statement without quoting | low | accept | Surfaced by this audit's bytecode read: `com.clickhouse.client.api.Client.insert(String, List<String>, …)` (client-v2 `0.9.6`) builds `INSERT INTO <table> (<c1>,<c2>…)` with `StringBuilder.append` and no escaping. See Accepted Risks Log AR-04. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

### The five metadata-free bypass guard sites (re-verified)

All five read the single predicate `shouldSkipTableMetadata()` (`ClickHouseWriter.java:1616-1618`), which
is keyed on `csc.isJsonInsertFormatExplicit()` and nothing else, so an unset `insertFormat` (which resolves
to the JSON enum constant through upstream's default-branch typo) does not widen the bypass.

| # | Site | Location | Pin |
|---|------|----------|-----|
| 1 | `start()` — skip mapping and validation | `:125` | `MetadataFreeJsonInsertUnitTest` |
| 2 | `updateMapping()` — short-circuit above the in-progress flag | `:147` | `updateMappingUnderExplicitJsonShortCircuits…` |
| 3 | `getTable()` — synthetic five-argument `Table` | `:1621` | `getTableUnderExplicitJsonReturnsSyntheticRemappedTable…` |
| 4 | `cleanupExtraFields()` — skip field cleanup | `:1404` | `cleanupExtraFieldsBypassDiffersByInsertFormat` |
| 5 | `doInsert()` `auto.evolve` — added by CR-01's fix (T-09-15) | `:210` / `:222` | `autoEvolveIsSkippedUnderExplicitJson…`, `autoEvolveStillRunsUnderNonExplicitInsertFormat` |

Totals: 17 threats (T-09-01..16 plus T-09-SC) — 17 closed, **0 open**.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-09-03 (R-01) | KEEP re-introduces R-01's silent data loss on the `Struct` + explicit-JSON + default `bypassRowBinary` route. Accepted because operator fact 2 — no live pipeline takes that route today (live pipelines produce schemaless `Map` values) — and because a missing `DESCRIBE`/`SHOW` grant is reached by no combination of `suppressTableExistenceException`, `enableDbTopicSplit` or `bypassFieldCleanup`. The loss stays pinned, not fixed (JSONP-02); a fail-fast repair is a recorded Future Requirement worth rescheduling on this evidence. Reversibility is costly (a production migration). **Stops being acceptable the moment a live pipeline takes the `Struct` + explicit-JSON route.** | developer, decision D-01 at Phase 9's blocking-human checkpoint (re-confirmed in plan 09-01 task 1: "Confirm KEEP (expected)."); recorded as `MERGE-CHECKLIST.md` Recorded Fact 14 | 2026-09-20 |
| AR-02 | T-09-04 | Least-privilege accommodation, not escalation: no grant, credential or permission check is added or removed; the guards only change whether the connector attempts queries it may not be entitled to run. | plan 09-01 (plan-time disposition), confirmed by audit 11-01 | 2026-10-07 |
| AR-03 | T-09-SC | No package-manager install task exists in any Phase 9 plan; no Gradle dependency, catalog or lockfile is touched. | plans 09-01..03, confirmed by audit 11-01 | 2026-10-07 |
| AR-04 | T-09-16 | `jsonInsertColumns` is operator-supplied Connect configuration — the same trust level as `database`, `topic2TableMap` and the credentials themselves; a Connect administrator who can set it can already point the connector at any database with any credential. It never carries record data. A name needing quoting must be pre-quoted by the operator. No connector-side pre-validation is added, because that would downgrade the loud server-side `NO_SUCH_COLUMN_IN_TABLE` failure (COLS-05) to silence. Mitigating this properly means quoting inside the client, which is a dependency change and out of this milestone's scope. | audit 11-01 (new) | 2026-10-07 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-07 | 17 | 17 | 0 | /gsd-secure-phase 09 (retroactive audit, plan 11-01 task 3; ASVS L1; mitigations verified against code at `sync/v1.6.0`) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-07

---

## Carried Forward (non-blocking)

Recorded with their existing dispositions so this register, `09-REVIEW.md` and `STATE.md` do not drift
apart. Neither is a security threat; neither is minted as a new open threat.

- **`09-REVIEW.md` IN-01 (P9-IN-01)** — `cleanupExtraFields`'s guard reads `csc.isBypassFieldCleanup() || shouldSkipTableMetadata()` (`ClickHouseWriter.java:1404`), dereferencing `csc` before the predicate whose stated purpose is a `csc != null` short-circuit, so the null-safety documented on `shouldSkipTableMetadata()` does not hold at that call site. Re-confirmed still present. Unreachable today: the method is only reached through a started writer. Fix is to swap the operands.
- **R-01** — see T-09-03 / AR-01 above and `STATE.md` `[Prod]`.

No audit mitigation requiring a `src/main/` change was proposed. The one residual that would need one
(T-09-16, quoting inside the client) is a dependency-level change and is recorded as an accepted risk.
