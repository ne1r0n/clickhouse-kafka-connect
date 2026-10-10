---
phase: 11-full-suite-ci-publication
plan: 08
subsystem: testing
tags: [clickhouse, network_compression_method, lz4, zstd, client-v1, client-v2, gap-closure, fork-delta]

requires:
  - phase: 11-full-suite-ci-publication
    provides: wave-1 post-merge gate that exposed the ClickHouse 26.9 ZSTD incompatibility (D-17)
provides:
  - connector forces network_compression_method=lz4 on every V1 and V2 client it builds
  - Docker-free unit pin and live round-trip pin for the setting
  - FORK-DELTA ledger row, next-syncer note and removal condition
affects: [11-04, 11-05, 11-06, 11-07]

actuals:
  tokens: 3358
  tasks: 3
  commits: 4

plan_head_before: c1480f8ffa7c3fed5b21f14a0455abedf4f14df9
plan_head_after: 3ebe4e0abb23b63c3abcc6cbd462ddbc972d8c2c
commits: 4

tech-stack:
  added: []
  patterns:
    - "Connector default applied before operator options so an explicit operator value wins (V2 serverSetting before the jdbcConnectionProperties loop; V1 CUSTOM_SETTINGS merged by the client)"
    - "lz4hc as a decodable override value for proving operator precedence on the wire"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - docs/FORK-DELTA.md

key-decisions:
  - "No V1 merge code: probing showed the V1 client already merges a node-level custom_settings with the client default and the operator's value wins for the same key"
  - "Operator-override live tests use lz4hc (same LZ4 method byte, decodable by client 0.9.6) instead of zstd, so precedence is proven on the wire without breaking decoding"

requirements-completed: [SUITE-01]

coverage:
  - id: D1
    description: "Every V2 client the connector builds sends network_compression_method=lz4 as a server setting"
    requirement: SUITE-01
    verification:
      - kind: unit
        ref: "NetworkCompressionPinUnitTest#v2ClientCarriesLz4"
        status: pass
      - kind: integration
        ref: "NetworkCompressionPinLiveTest#v2PingAndQueryRoundTrip"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every V1 request path sends network_compression_method=lz4 through the shared default client options"
    requirement: SUITE-01
    verification:
      - kind: unit
        ref: "NetworkCompressionPinUnitTest#v1DefaultOptionsCarryLz4"
        status: pass
      - kind: integration
        ref: "NetworkCompressionPinLiveTest#v1PingAndQueryRoundTrip"
        status: pass
    human_judgment: false
  - id: D3
    description: "An operator's explicit value for the same setting wins on V2 and V1, and V1 merges unrelated custom_settings keys"
    requirement: SUITE-01
    verification:
      - kind: unit
        ref: "NetworkCompressionPinUnitTest#operatorValueWinsOnV2"
        status: pass
      - kind: integration
        ref: "NetworkCompressionPinLiveTest#operatorOverrideReachesServerOnV2"
        status: pass
      - kind: integration
        ref: "NetworkCompressionPinLiveTest#operatorOverrideReachesServerOnV1"
        status: pass
    human_judgment: false
  - id: D4
    description: "The classes red in the wave-1 gate pass against ClickHouse 26.9.12.8 (latest)"
    requirement: SUITE-01
    verification:
      - kind: integration
        ref: "./gradlew test --tests '*ClickHouseServerVersionSmokeTest*' '*ClickHouseHelperClientTest*' '*TableTest*' '*ClickHouseWriterTest*' '*AsyncInsertLiveTest*' '*ClickHouseSinkTaskSchemalessTest*' '*MBeanRegistrationLiveTest*' -> 55 tests, 55 succeeded, 0 failed, 0 skipped"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA names the patch, its pins and its removal condition"
    verification: []
    human_judgment: true
    rationale: "Documentation wording adequacy is a reader judgment; only the presence of the setting name is machine-checked"

duration: 25min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 08: Force LZ4 Network Compression for ClickHouse 26.9+ Summary

**Connector now sends `network_compression_method=lz4` on every V1 and V2 client (V2 `serverSetting` before operator options, V1 `CUSTOM_SETTINGS` in the shared default options), so client 0.9.6 decodes ClickHouse 26.9's responses again; pinned Docker-free and live.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-10-07T11:34Z
- **Tasks:** 3
- **Files modified:** 4 (1 in src/main, 2 new tests, 1 doc)

## Accomplishments

- The single production change is in `ClickHouseHelperClient`: two top-level constants (with the removal-condition comment), a `serverSetting` call on the V2 builder ahead of the `jdbcConnectionProperties` loop, and one `CUSTOM_SETTINGS` entry in `getDefaultClientOptions()`.
- All V1 request paths already route through `getDefaultClientOptions()`: the helper's ping/version/query/showTables/describe/alter sites, `KeeperStateProvider.getStateRecord`, and `ClickHouseWriter.getClient()`. No out-of-file edit was needed.
- Live proof on 26.9.12.8: a `getSetting('network_compression_method')` query returns `lz4` on both clients, and operator overrides (`lz4hc`) reach the server on both clients.
- The formerly red classes are green against `latest` 26.9.12.8: 55 tests, 55 succeeded, 0 failed, 0 skipped. `testUnit`: 331 tests, 329 succeeded, 0 failed, 2 skipped (327 before, plus the 4 new unit methods; the 2 skips are pre-existing).

## RED evidence (observed on the unfixed tree, commit 6b584980)

`./gradlew test --tests '*NetworkCompressionPin*' --rerun` -> 7 tests, 6 failed, 1 passed.

- `NetworkCompressionPinUnitTest` (setting absent):
  - `v1DefaultOptionsCarryLz4`: `org.opentest4j.AssertionFailedError: expected: <network_compression_method=lz4> but was: <null>`
  - `v2ClientCarriesLz4`: `org.opentest4j.AssertionFailedError: expected: <lz4> but was: <null>`
  - `unrelatedOperatorOptionsKeepLz4`: `org.opentest4j.AssertionFailedError: expected: <lz4> but was: <null>`
  - `operatorValueWinsOnV2` passed on the unfixed tree, as expected: nothing sets the setting yet, so the operator's value is trivially the only one. It becomes a real guard only with the fix, because it pins the ordering.
- `NetworkCompressionPinLiveTest` (all three methods FAILED; the class `@BeforeAll` in `ClickHouseBase.setup()` fails first, creating the per-class database through the same helper client):
  - `com.clickhouse.client.api.ClientException: Invalid LZ4 magic byte: '-112'` (thrown from `ClickHouseLZ4InputStream.refill`, wrapped as `ClientException: Failed to get query response` and `RuntimeException` from `queryV2`).
  - This matches D-17's diagnosis exactly (byte `0x90` = -112 = ZSTD).

## Task Commits

1. **Task 1: RED pin tests (unit + live)** - `6b584980` (test)
2. **Task 2: GREEN force LZ4 on every connector-built client** - `87fa1e78` (fix) - the only commit touching `src/main`
3. **Task 3: FORK-DELTA ledger row, raw-number note, next-syncer paragraph** - `3ebe4e0a` (docs)

Additional commit not in the plan: `e8e352a8` (test) - adds two live tests proving operator override on the wire for V1 and V2. Kept out of the fix commit so that commit contains only the `src/main` file, as the plan requires.

**Plan metadata:** docs(11-08) SUMMARY commit follows this file.

### Commits that touched src/main (later plans carve these out of their no-src/main assertions)

| SHA (full) | Short | File |
|---|---|---|
| `87fa1e7851599360518736c07c78742720e8789c` | `87fa1e78` | `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` |

No other commit in this plan touches `src/main` (verified with `git log c1480f8f..HEAD -- src/main`).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - constants, V2 `serverSetting`, V1 `CUSTOM_SETTINGS` default
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java` - Docker-free pin (no `clickhouse` tag), 4 methods, built against an unreachable endpoint
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java` - live pin on `ClickHouseBase`, 5 methods
- `docs/FORK-DELTA.md` - one ledger row under Connector source, one sentence on the raw-number table, one paragraph under "For the next syncer"

## Decisions Made

- No V1 merge code. A live probe showed the V1 client merges a node-level `custom_settings` (from `jdbcConnectionProperties`) with the client default: with `custom_settings=network_compression_method%3Dlz4hc,max_threads%3D3` the server saw `lz4hc` and `3`. The operator wins for the same key and unrelated keys are kept, so the plan's "merge rather than clobber" requirement is already met by the client.
- Operator-override live tests use `lz4hc` rather than `zstd`: it frames with the same LZ4 method byte, so client 0.9.6 can still decode it, which lets precedence be asserted on the wire.
- Constants are package-private (not public) to keep the helper's public surface unchanged.

## Deviations from Plan

### Auto-fixed Issues

None - no bugs or blockers were found beyond the planned fix.

### Additions (not deviations from a prohibition)

- **Extra test commit `e8e352a8`.** Two live override tests were added after the fix, in a separate test commit. The plan's Task 1 only required a unit-level override assertion (V2); the live tests strengthen T-11-37's mitigation by proving the operator value reaches the server on both clients. No existing test was deleted or weakened.
- **No out-of-file `src/main` edit.** The plan allowed one if a V1 builder bypassed `getDefaultClientOptions()`; none did (8 `ClickHouseClient.builder()` sites in `src/main`, all passing it).

**Total deviations:** 0 auto-fixed. **Impact on plan:** none; scope stayed within the prohibitions (no dependency change, no `build.gradle.kts` change, no compression on/off switch, no image pin, no tree-wide spotlessApply).

## Issues Encountered

- `spotlessCheck` reports 44 pre-existing import-order violations unrelated to this plan. None of the three Java files this plan touches appears in the violation list, so no formatting was applied and no unrelated file was reformatted.
- The staged `.planning/milestone.lock` and the modified `.planning/STATE.md`/`state.json` pre-existed; every commit used `git commit --only <paths>`, so none were swept in.

## Residual risk (T-11-38, accepted)

A server profile that forbids changing `network_compression_method` (a settings constraint or `readonly` mode) would reject every request the connector sends. `readonly=1` is impossible for a user that must write, and constraints on this setting are non-standard, but before a production rollout check the connector user's profile for a constraint on `network_compression_method`.

## Known Stubs

None.

## Threat Flags

None - no new endpoint, auth path or schema change; the only change is one server setting on outbound requests.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 11-04's cache-busted full run is the suite-scale acceptance gate for this fix; this plan verified only the targeted classes and `testUnit`.
- Plans 11-04 to 11-07 must exclude commit `87fa1e78` (and only it) from their "no src/main change in Phase 11" assertions.
- The `docs/FORK-DELTA.md` raw-number table was not recomputed; it now carries a sentence saying a refresh includes this plan's lines.

## Self-Check: PASSED

- Created files exist: both `NetworkCompressionPin*Test.java` files and this summary (committed `6b584980`, `e8e352a8`).
- Commits exist on HEAD: `6b584980`, `87fa1e78`, `e8e352a8`, `3ebe4e0a`.
- `grep network_compression_method` present in `ClickHouseHelperClient.java` and `docs/FORK-DELTA.md`.
- `NetworkCompressionPin*` run: 9 tests, 9 passed; targeted classes: 55/55; `testUnit`: 331 tests, 0 failed.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
