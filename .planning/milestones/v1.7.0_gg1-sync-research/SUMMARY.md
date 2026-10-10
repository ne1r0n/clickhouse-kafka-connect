# Project Research Summary

**Project:** ClickHouse Kafka Connect `custom_build` fork — milestone `v1.7.0_gg1-sync`  
**Domain:** Brownfield fork maintenance and upstream sync (Kafka Connect sink to ClickHouse, Java 17)  
**Researched:** 2026-10-07  
**Confidence:** HIGH for merge mechanics and code-level findings; MEDIUM for dependency release-note claims and compression wire semantics  

## Executive Summary

The `custom_build` fork of ClickHouse Kafka Connect is merging upstream `v1.7.0` (22 commits, 102 insertions on top of `v1.6.0`) into a production-running connector. The merge is **smaller and lower-risk than v1.6.0** (5 conflicted files vs 40), but contains **three critical silent hazards**: (1) upstream's #820 null-Map fix is shadowed by the fork's own MAP-branch guard, desyncing RowBinary streams silently; (2) upstream's D-17 retirement via `setCompressionMethod` flag **also disables server response compression**, changing wire behaviour beyond the lz4 setting; (3) R-01's routing fix changes where Struct bytes go, exposing a new population to JSON-path type-fidelity caveats (Timestamp/Date as epoch ms, null fields omitted). The fork's build is clean (lz4-java pins converge, jackson 2.22.3 is runtime) and the test gate is established. **Recommended approach:** merge mechanically with scoped conflict resolution (MAP guard, D-17 flag threading), retire D-17 onto upstream's broader fix, route R-01 via JSON writer with typed read-back, and harden exactly-once via a direct KeeperStateProvider restart test. The sync lands first; hardening goes on top of the merged tree.

The milestone scope is aggressive: T1 sync, T2–T4 writer correctness and R-01 routing, T5–T6 nine small fixes and test debt, T7–T8 CI and dependency bumps, T9–T11 publication. **Phase ordering suggested by research:** (12) sync+reconcile, (13) writer hardening+R-01, (14) deps+CI, (15) publication. The suite gate is 663 tests on ClickHouse `latest` × client V2. No new architectural patterns; the fork keeps its four behaviour families (SMTs, table filtering, metadata-free JSON, Dynamic types) and adds the R-01 and WR-02 hardening on top.

## Key Findings

### Recommended Stack

The fork's stack is inherited from `v1.6.0_gg1` (Gradle 9.6.1 with upstream catalog, JUnit 5.14.4, Testcontainers 2.0.5, JDK 17) and carries three permanent fork pins: **Kafka Connect API 4.1.1** (upstream is 2.7.0; the fork needs `onPartitionsRevoked` unflagged), **ClickHouse client 0.9.6** (client 0.10.0 is out of scope), **slf4j-reload4j 2.0.17** (intentional literal, not catalogued).

**Upstream v1.7.0 stack changes (all adoptable):**
- ClickHouse client stays 0.9.6 (fork already had it; now upstream's own value — drop FORK OVERRIDE comment)
- `lz4-java: 1.10.2` → `1.10.4` (no functional difference; resolves CVE-driven bump; single capability rule to keep)
- Four approved dependency bumps: Jackson 2.22.3 (runtime, hardening), OkHttp 5.5.0 (test-only), org.json 20260814 (test-only, has new number-length limit), Mockito 5.24.0 (test-only)
- `clickhouse-jdbc` removed from catalog and integrationTest (fork never used it; safe to remove)
- Build gains `lz4Java` catalog entry and upstream's group-based LZ4 capability rule; fork's duplicate rule must be deleted to avoid masking the 1.10.4 bump

**Critical build mechanics:**
- Fork carries two LZ4 capability rules (auto-merged side-by-side); remove fork's "Temporary fix" block (BLD-06), keep upstream's. Proof: `./gradlew dependencyInsight --dependency lz4-java --configuration runtimeClasspath` must show `at.yawk.lz4:lz4-java:1.10.4` only.
- `generateVersionFile` staleness trap: after `VERSION=v1.7.0_gg1` edit, run `./gradlew clean` locally before test runs, or `Version.ARTIFACT_VERSION` feeds a stale merge-conflict string to JMX and the client name.

### Expected Features

**Table Stakes (must ship):**
- T1: Mechanical merge of `v1.7.0` with conflict resolution per hunk table (fork guards re-applied)
- T2: Retire D-17 in favour of upstream `setCompressionMethod` — takes flag, constants, and **unconditional `compress=false`** (not just lz4)
- T3: R-01 routing fix — one condition in `doInsertBatch`: `csc.isBypassRowBinary() || shouldSkipTableMetadata()`
- T4: WR-01/WR-02 null-correctness — scoped MAP guard (`&& col.isNullable()`), fail-loud on nested non-nullable slots
- T5: Metadata/config fixes — P3-WR-01 depth walk, P3-WR-03 narrowed to INT256/UINT256, `extractColumn` swap, `USERNAME` default, P9-IN-01 operand swap
- T6: Test debt — WR-03 collision contract, JsonColumnTest read-back, try/finally guards, tearDown drop handling, Javadocs

**R-01 routing special case** (T3, decisive for data loss fix):
- **Today:** `insertFormat=json` + Struct + default `bypassRowBinary=false` writes **zero rows, no error**. Synthetic table has zero columns; RowBinary loop emits no bytes.
- **After:** routes to JSON writer; rows land with Jackson serialization (Timestamp/Date as epoch ms, nulls omitted, bytes as base64, nested Struct handled).
- **Test flip required:** `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` → typed read-back (Int32/String minimum), plus live matrix covering Timestamp, Date, Decimal, nested Struct, null Map (ties to #820), and `jsonInsertColumns` interaction.
- **Exactly-once implication:** dedup token is `topic-partition-offset-range`, not content-hash; routing does not break dedup for a retry straddling the upgrade. Old `AFTER_PROCESSING` state stays "done". Backfilling needs consumer-group offset reset + KeeperMap state cleared for those partitions.

**D-17 retirement scope** (T2):
- Fork pins `network_compression_method=lz4` unconditionally on every V1/V2 client. **Upstream does the same** via a new flag (default `true`).
- **But upstream also** unconditionally sets `COMPRESS=false` (V1 options, V1 node options, V2 config). Server responses are now **uncompressed regardless**.
- **Behaviour change recorded:** responses grow slightly for `DESCRIBE`/`SHOW`/state (small metadata, one state row); inserts unaffected (request compression already off). Do not measure; accept as a cost.
- **Rollout caveat:** `setCompressionMethod=false` removes both mitigations. If an operator has `compress=true` in `jdbcConnectionProperties` (applied after `compressServerResponse(false)` on V2, so it wins), they re-expose the ZSTD break. Document in checklist.

**Open decisions for the user:**
1. **lz4-java beyond 1.10.4:** versions 1.11.x and 1.12.0 fix CVE-related advisories (LZ4BlockInputStream, XXHash JVM crash, temp-dir race). Taking 1.10.4 is approved. A follow-up to 1.11.2+ would need explicit user approval and client compatibility check on 0.9.6.
2. **CR-02 pin tests re-scoping vs "never weaken tests":** WR-02 narrows the fork's TUPLE/VARIANT null branches to nested non-nullable slots only, not top-level. CR-02 pins must be re-checked; explicitly record any test changes as approvals, not refactorings.
3. **CHANGELOG format for `_gg1` entries:** previous milestone did not add fork-specific CHANGELOG lines. Policy decision: what format / level of detail for v1.7.0_gg1 entries?
4. **org.json number-limit risk:** 20260719 added a new configurable max length for BigInteger/Decimal. Default unknown. If a test builds Int256/Decimal256 fixtures through org.json, behaviour may differ. First full `test` run is the check; bisect by reverting `orgJson` alone if failures appear.

### Architecture Approach

The fork's architecture is layered: Kafka Connect API → ClickHouseSinkTask/ProxySinkTask → Processing (state machine) → ClickHouseWriter (type routing, RowBinary, JSON paths). The critical point for v1.7.0 is **ClickHouseWriter** because four upstream changes touch it: #820 (null Map), #841 (setCompressionMethod), D-17 retirement hunks, and R-01 routing. **No new components**; routing lives in one condition check at `doInsertBatch`, type handling stays in `doWriteColValue`.

**Architecture hazards found in research:**
1. **Silent MAP conflict (Pitfall 1):** Fork's `if (value.getObject() == null) writeNull` auto-merges **before** upstream's `if (mapTmp == null)...` block. Fork's branch runs first, makes upstream's unreachable. For a non-nullable Map: doWriteCol writes `0x00` (empty-map size marker), fork writes `0x01` (stray byte), stream desyncs. **Probe confirmed:** scoped guard `&& col.isNullable()` fixes bytes (`00` / `00 00`). Upstream's `nullMapTypeTest` must not be edited; run unmodified as the acceptance gate.
2. **Duplicate LZ4 rules (Pitfall 2):** Fork's rule and upstream's both select the `at.yawk.lz4` candidate; fork's hard-codes 1.10.2, masking the 1.10.4 bump. Delete fork's rule; upstream's version-agnostic rule covers it.
3. **D-17 pin meaning change (Pitfall 3):** The load-bearing protection is `compress=false`, not lz4. lz4 is defence-in-depth. `NetworkCompressionPinLiveTest` was written to reproduce a ZSTD failure; with `compress=false` the failure never occurs even without lz4. **Needs explicit re-test and Javadoc rewrite.**
4. **Exactly-once consistency (Pitfall 7):** `KeeperStateProvider.getStateRecord` changed from `response.getSummary().getResultRows()` to `response.stream()` count. Summary header may not populate with `compress=false`. **Needs a direct restart-read test**, not just relying on dedup-window shadowing.

**Data flow risk:** the R-01 JSON writer calls `struct.get(field)` + Jackson serialization. Nested Struct is handled; logical types are **not** converted (Timestamp/Date as java.util.Date → epoch millis, Decimal as BigDecimal, bytes as base64). This is pre-existing `bypassRowBinary=true` behaviour, now reachable by a new population of operators. **The test matrix must pin observed outcome, not assume parity with RowBinary.**

### Critical Pitfalls

1. **Pitfall 1: MAP null branch shadows #820, desyncs RowBinary** — Fork's `writeNull` guard for a Nullable child sits above upstream's lenient top-level null-Map handler. Stream desync. **Avoid:** scoped guard `value.getObject() == null && col.isNullable()`. Run upstream's `nullMapTypeTest` unmodified as gate; add Docker-free byte-level test.

2. **Pitfall 2: Duplicate LZ4 rules, fork's masks 1.10.4 bump** — Two `withCapability("org.lz4:lz4-java")` blocks (fork selects 1.10.2, upstream selects any at.yawk). **Avoid:** delete fork's "Temporary fix" block. Prove with `dependencyInsight --dependency lz4-java --configuration runtimeClasspath` → `at.yawk.lz4:lz4-java:1.10.4` only.

3. **Pitfall 3: D-17 pin is vestigial; load-bearing protection is `compress=false`** — ClickHouse 26.9 defaults `network_compression_method` to ZSTD; client 0.9.6 cannot decode. Upstream #841 fixes by setting `COMPRESS=false` on V1/V2, **not** by lz4. lz4 is now defence-in-depth. **Avoid:** add Docker-free pins for `compress=false` (V1 options, V1 node, V2 config). Add a live test with `setCompressionMethod=false` on latest; it must pass (because `compress=false`). Record the real load-bearing control in FORK-DELTA.

4. **Pitfall 5: R-01 JSON route has type-fidelity caveats** — Jackson + `Timestamp`/`Date`/`Decimal`/bytes have non-RowBinary behaviours. Operators upgrading from silent-loss to actual rows will see new data and new error modes (server parse errors, Type errors). **Avoid:** flip the test to typed read-back, document caveats (no `DateTime` columns; use `DateTime64(3)`; null fields become defaults; bytes are base64). Add rollout step: per-connector re-confirmation that the pipeline is not `insertFormat=json` + Struct + `bypassRowBinary=false`.

5. **Pitfall 9: Green build after merge is not proof fork survived** — Seven fork behaviours vanished in v1.6.0 merge silently. This diff is smaller but the same classes are touched: auto-merged hunks inside conflicted files (MAP shadow), duplicate rules (LZ4), builder defaults (setCompressionMethod), test helpers (version revert). **Avoid:** (1) before merging, list every fork-touched file upstream touches; review non-conflicting hunks. (2) After merge: run `testUnit` with Docker down; `compileIntegrationTestJava` green; `ClickHouseVersionDefaultUnitTest`, `NewUpstreamConfigDefaultsUnitTest`, `NetworkCompressionPin*` green; compare fork test helper symbol set before/after. (3) Full suite: two consecutive runs with `--rerun`; executed set identical to 18 named skips.

## Implications for Roadmap

Research suggests a **4-phase structure, coarse-grained**: sync→reconcile, writer hardening, dependencies+CI, publication. Phases can be further broken into granular sub-plans during roadmap work, but this ordering respects code dependencies and minimizes hidden conflicts.

### Phase 12: Upstream v1.7.0 sync + reconcile
**Rationale:** Merge base is v1.6.0; every fork behaviour that touches the sync must be re-applied on top. D-17 retirement is a merge-line decision, not a later fix.
**Delivers:** 
- One mechanical merge commit with 5 textual conflicts resolved (VERSION, libs.versions.toml, ClickHouseSinkConfig x4, ClickHouseWriter x1, ClickHouseHelperClient x6–7).
- One by-intent reconcile commit: scoped MAP guard, setCompressionMethod added to option-defaults pin, D-17 constants deleted, lz4 capability rule deduplicated, kafka/clickHouseDriver catalog state restored.
**Addresses (from FEATURES T1, T2, implicit):**
- Upstream merge with fork behaviour preserved (finding 3: silent MAP conflict fixed via scoped guard).
- D-17 retired onto upstream's broader flag + `compress=false` logic (finding 6, section D-17 retirement).
**Avoids:**
- Pitfall 1 (MAP shadow): scoped guard `&& col.isNullable()`.
- Pitfall 2 (duplicate LZ4 rules): delete fork block.
- Pitfall 8 (build pins reverted): re-apply kafka 4.1.1, extend invariant script with line-provenance checks.
- Pitfall 9 (green build is not proof): pre-merge review of non-conflicting hunks in touched files, post-merge verification checklist.

### Phase 13: Fork hardening — R-01, WR-01/WR-02, metadata/config fixes
**Rationale:** Writer hunks collide; done on merged tree to avoid re-merge. R-01 touches `doInsertBatch` only. WR-01/WR-02 share the same MAP/ARRAY null branches from #820. P9-IN-01 is a one-line swap adjacent to #820's hunk.
**Delivers:**
- R-01 routing: one condition (`shouldSkipTableMetadata()`) reroutes Struct records to JSON writer (FEATURES T3).
- WR-01 live test: end-to-end null inside Map(K, Nullable(V)) round-trips as NULL (not data loss or default).
- WR-02 loud failure: nested non-nullable slot gets null, throws DataException (not stream desync).
- P3-WR-01 (depth walk): `shouldSkipDescribeRow` walks all dot-ancestors, not just `lastIndexOf`.
- P3-WR-03 (narrowed): fail-fast gate for INT256/UINT256 only (INT128/UINT128 confirmed writeable).

### Phase 14: Dependency bumps and CI
**Rationale:** Independent of code changes; sequenced after hardening so attribution is clear. Jackson bump sits under the JSON writer R-01 path.
**Delivers:**
- jackson 2.22.3: security hardening in core/databind (as runtime dependency).
- okhttp 5.5.0, org.json 20260814, mockito 5.24.0: test-only bumps, one commit per group.
- CI workflow: concurrency group keyed on `github.ref` (not just `github.sha`), so tag push does not cancel branch run.

### Phase 15: Publication & rollout
**Rationale:** Final gate; publishes planning history on custom_build (protected, --no-ff merge required).
**Delivers:**
- `VERSION=v1.7.0_gg1` (changes JMX `version=` segment, D-07).
- CHANGELOG with fork entries (R-01 behaviour change, WR-02 strictness, lz4 note, compression behaviour).
- FORK-DELTA.md updated: D-17 row rewritten, lz4 row updated, R-01 Known Costs removed, metadata-free row amended.
- Operator rollout checklist: MBean name before/after strings (D-07), compression setting semantics (D-17, caveat on `compress=true`), R-01 behaviour change + KeeperMap backfill note.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | Build delta verified by simulating merge in throwaway clone; real Gradle resolution runs. Dependabot branches are one-liners (verified by `git diff`). lz4 resolution confirmed by `dependencyInsight` simulation. |
| Features | **HIGH** | Code read at both v1.6.0 and v1.7.0 tags; live test coverage for #820 null Map and R-01 routing exists (though R-01 test is not yet flipped). Feature dependencies traced through file collisions. |
| Architecture | **HIGH** | Merge conflict map generated by `git merge-tree` and verified by real merge in throwaway tree. Silent MAP conflict confirmed by byte-level probe (RowBinary output diffed before/after scoped guard). |
| Pitfalls | **HIGH** | All critical pitfalls either code-read (MAP shadow, duplicate rules, setCompressionMethod threading) or inferred from test findings (R-01 type-fidelity caveats, KeeperStateProvider stream-count change). |

**Overall confidence:** **HIGH** — Research conducted via read-only operations on the actual repository and merge simulation; no assertions rely on unverified web sources.

### Gaps to Address

1. **Jackson 2.22.3 release notes:** Summariser report only; CVE IDs not cross-checked against NVD. **Mitigation:** first full `test` run after bump is the check.

2. **org.json 20260814 number-limit default:** Unknown default for new configurable max length. **Mitigation:** if org.json builds Int256/Decimal256 fixtures, watch for roundtrip changes in first test run after bump.

3. **R-01 JSON-route type-fidelity:** Timestamp/Date/Decimal behaviour through Jackson are inferred from code, not observed. **Mitigation:** the R-01 test matrix in Phase 13 must include these types and assert observed values, not assume parity with RowBinary.

## Sources

### Primary (HIGH confidence)
- Local repository: `git merge-tree`, `git diff v1.6.0 v1.7.0`, real `git merge --no-commit v1.7.0` in throwaway worktree with Gradle compilation
- Codebase reads: `git grep`, `javap` of client 0.9.6 jars for option defaults
- Test expectations: upstream's nullMapTypeTest, testSetCompressionMethod_Disabled unmodified and expected to pass

### Secondary (MEDIUM confidence)
- Jackson 2.22.3 release notes, OkHttp CHANGELOG, org.json releases, Mockito 5.24.0 release notes (via GitHub releases)

### Tertiary (LOW confidence)
- Upstream #820, #841, #800, #801 as cited in v1.7.0 CHANGELOG.md only (issue pages not accessed)

---

*Research completed: 2026-10-07*  
*Synthesized from: STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md, PROJECT.md*  
*Ready for roadmap: yes*
