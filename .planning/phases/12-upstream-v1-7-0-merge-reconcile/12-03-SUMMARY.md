---
phase: 12-upstream-v1-7-0-merge-reconcile
plan: "03"
subsystem: testing
tags: [compression, lz4, zstd, setCompressionMethod, d-17, cmp, clickhouse-26.9, client-v1, client-v2]

requires:
  - phase: 12-02
    provides: "single LZ4 rule, scoped MAP guard, fork-anchors.txt with 40 rows"
provides:
  - "D-17's two dead constants and comment removed; upstream setCompressionMethod is the single mechanism (CMP-01)"
  - "Docker-free pins of compress=false and the lz4 setting, with and without the flag, on V1 options, V1 node and V2 config (CMP-02)"
  - "14th upstream option setCompressionMethod pinned Docker-free (CFG-03 / CMP-01)"
  - "Live sink-task insert and read-back on latest for the flag true/false on V2 and V1 (CMP-03)"
  - "D-12 probe observation for 12-05's FORK-DELTA sentence"
affects: [12-04, 12-05, phase-13, phase-15]

plan_head_before: c9d15b62aec36b99a65926bb88b1f8fad4628d49
plan_head_after: 9a4f79785f9097b263ca34771eebe4ead25904a3

actuals:
  tokens: 3294
  tasks: 3
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Docker-free config pins via try-with-resources ClickHouseHelperClient against localhost:1 (construction never contacts the server)"
    - "Read-back client built before property overrides so the read path never depends on the variable under test"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "A2/A3 literals held on the first run: V2 config and V1 node store 'compress' as the string 'false', V1 default options store Boolean.FALSE; no expected literal needed correcting"
  - "Task 1 made no commit; Tasks 1+2 land as the single CMP commit 9a4f7978 (D-04)"
  - "The D-12 probe class was deleted before the task ended and never staged"

patterns-established:
  - "Probe output lines carry a cause chain; use a separator that is not '<' so an XML-greppable extract is not truncated"

requirements-completed: [CMP-01, CMP-02, CMP-03]

coverage:
  - id: D1
    description: "D-17 leftovers removed from ClickHouseHelperClient (exactly 4 deleted lines, no NETWORK_COMPRESSION_METHOD_ reference left in src); every one of the 3 ClickHouseClientBuilder sites carries .setCompressionMethod(csc.isSetCompressionMethod())"
    requirement: CMP-01
    verification:
      - kind: other
        ref: "git grep -c NETWORK_COMPRESSION_METHOD_ -- src == 0; numstat 0/4; builder sites 3 == flag sites 3; verify-merge-invariants.sh M HEAD result=PASS"
        status: pass
    human_judgment: false
  - id: D2
    description: "compress=false and the lz4 setting pinned Docker-free, with and without setCompressionMethod, on V1 options, V1 node and V2 config; operator compress=true caveat pinned"
    requirement: CMP-02
    verification:
      - kind: unit
        ref: "NetworkCompressionPinUnitTest (9 tests, 0 failures); full testUnit 342 tests, 0 failed, 2 skipped"
        status: pass
    human_judgment: false
  - id: D3
    description: "14th upstream option setCompressionMethod defaults true; canonical key setCompressionMethod and alias set_compression_method both work; ConfigDef default is true"
    requirement: CMP-01
    verification:
      - kind: unit
        ref: "NewUpstreamConfigDefaultsUnitTest#setCompressionMethodFourteenthOptionDefaultsTrueWithCanonicalKeyAndAlias (4 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Full ClickHouseSinkTask insert and read-back on ClickHouse 26.9.12.8 succeeds with setCompressionMethod true and false on client V2 and client V1"
    requirement: CMP-03
    verification:
      - kind: integration
        ref: "NetworkCompressionPinLiveTest#sinkTaskInsertAndReadBackWithSetCompressionMethod x4 (9 tests, 0 skipped, 0 failures)"
        status: pass
    human_judgment: false
  - id: D5
    description: "D-12 observation: which control is load-bearing against the ZSTD default"
    requirement: CMP-03
    verification: []
    human_judgment: true
    rationale: "One-off probe against a single drifting image tag; the FORK-DELTA sentence is an interpretation 12-05 transcribes and the user should confirm, including the V1 'no observation' finding."

duration: 11min
completed: 2026-10-07
status: complete
---

# Phase 12 Plan 03: D-17 retired onto upstream setCompressionMethod Summary

**D-17's dead constants are gone and `compress=false` plus the lz4 setting are pinned on all three client surfaces with and without the flag; a live sink-task round trip passes on ClickHouse 26.9.12.8 for the flag true/false on V2 and V1, and the D-12 probe shows `compress=false` alone suffices on both clients.**

## Performance

- **Duration:** about 11 min (start time was not captured at launch; estimated from the first command)
- **Completed:** 2026-10-07T19:51Z
- **Tasks:** 3 (one commit; Task 1 and Task 3 commit nothing by design)
- **Files modified:** 5

## Accomplishments

- Removed the two-line 26.9/ZSTD comment and the two package-private `NETWORK_COMPRESSION_METHOD_*` constants (0 insertions, 4 deletions). Upstream's public `NETWORK_COMPRESSION_METHOD` / `LZ4` and the `if (setCompressionMethod)` blocks are the only mechanism now.
- Added five Docker-free pins to `NetworkCompressionPinUnitTest` (plus `COMPRESS_KEY` and the `buildClient(String, boolean)` overload) and the 14th-option pin to `NewUpstreamConfigDefaultsUnitTest`. All existing methods are untouched; the three test files show 0 deletions against the merge commit.
- Added the parameterised live case to `NetworkCompressionPinLiveTest`: V2/V1 x true/false, each doing a ClickHouseSinkTask start/put/stop of five schemaless records and a read-back with `assertEquals` on every id and str.
- `fork-anchors.txt`: `NETWORK_COMPRESSION_METHOD_` 2 -> 0 in the same commit; `verify-merge-invariants.sh` passes (`provenance_paths=21 anchor_commits=10 anchor_rows=40 marker_commits=57 result=PASS`).

## CMP-03 live results

Server: ClickHouse **26.9.12.8** (`clickhouse/clickhouse-server:latest`, image Created 2026-10-06T18:12:57Z, a ZSTD-default build). Client: **0.9.6**.

| Client | setCompressionMethod | Result |
|--------|----------------------|--------|
| V2 | true | pass (5 rows read back and compared) |
| V2 | false | pass |
| V1 | true | pass |
| V1 | false | pass |

The D-13 stop rule did not trigger. The class reports tests=9 skipped=0 failures=0 errors=0.

The V1 rows exercise `getMutationRequest`'s temporary client (`chcTmp`, ClickHouseWriter.java:1590), which V2 never constructs. Builder threading itself is held by the anchors: 3 `new ClickHouseHelperClient.ClickHouseClientBuilder(` sites and 3 `.setCompressionMethod(csc.isSetCompressionMethod())` sites in `src/main`.

## D-12 probe (throwaway, never committed)

Query: `SELECT toUInt8(1) AS n, version() AS v, getSetting('network_compression_method') AS m`. Controls were read from the built client before each query. No credential appears in any recorded text.

| Client | Config | flag | jdbcConnectionProperties | Controls observed | Outcome |
|--------|--------|------|--------------------------|-------------------|---------|
| V2 | S0 | true | (none) | compress=false, lz4 setting=lz4 | OK v=26.9.12.8 m=lz4 |
| V2 | S1 | false | (none) | compress=false, lz4 setting absent | OK v=26.9.12.8 m=ZSTD |
| V2 | S2 | true | ?compress=true | compress=true, lz4 setting=lz4 | OK v=26.9.12.8 m=lz4 |
| V2 | S3 | false | ?compress=true | compress=true, lz4 setting absent | FAIL ClientException: Failed to get query response, caused by ClientException: Invalid LZ4 magic byte: '-112' |
| V1 | S0 | true | (none) | node compress=false, opts COMPRESS=false, node lz4=lz4, opts CUSTOM_SETTINGS=network_compression_method=lz4 | OK v=26.9.12.8 m=lz4 |
| V1 | S1 | false | (none) | node compress=false, opts COMPRESS=false, lz4 absent in node and opts | OK v=26.9.12.8 m=ZSTD |
| V1 | S2 | true | ?compress=true | node compress=false (strip did NOT take effect), opts COMPRESS=false, lz4 present | OK v=26.9.12.8 m=lz4 |
| V1 | S3 | false | ?compress=true | node compress=false (strip did NOT take effect), opts COMPRESS=false, lz4 absent | OK v=26.9.12.8 m=ZSTD |

The V2 S3 failure is the LZ4 magic-byte error (-112 is 0x90, a ZSTD frame header) that D-17 was written against. The task text expected it to surface as an inactive connection; here it surfaces as a `ClientException: Failed to get query response`.

### Drafted FORK-DELTA sentence (for 12-05 to transcribe)

> Against ClickHouse 26.9.12.8 with client 0.9.6, `compress=false` alone is sufficient on both clients (V2 S1 and V1 S1 return `network_compression_method=ZSTD` from the server and the response still decodes), and the lz4 setting alone is sufficient when an operator re-enables response compression on V2 (S2); stripping both on V2 with `compress=true` fails with `Invalid LZ4 magic byte: '-112'` (S3), so on V2 at least one of the two controls is required. On V1 an operator's `?compress=true` did not reach the node options, so the V1 stripped-both case was not observed.

Every clause maps to a table row: S1 (V2, V1), S2 (V2), S3 (V2), V1 S2/S3 controls.

## Task Commits

1. **Task 1: remove D-17 leftovers, add CMP-02 and 14th-option pins** - no commit by design (D-04)
2. **Task 2: CMP-03 live round trip, anchor 2 -> 0, single CMP commit** - `9a4f7978` (feat)
3. **Task 3: D-12 probe** - no commit by design (D-12)

**Plan metadata:** committed separately after this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - four dead D-17 lines deleted
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java` - five CMP-02 cases, `COMPRESS_KEY`, `buildClient(String, boolean)`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java` - parameterised CMP-03 sink-task round trip
- `src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java` - 14th-option pin
- `.planning/scripts/fork-anchors.txt` - `NETWORK_COMPRESSION_METHOD_` anchor 2 -> 0

## Decisions Made

- Followed the plan as specified. The A2/A3 expected literals (`"false"` strings in the V2 config and V1 node options, `Boolean.FALSE` in the V1 options) were correct on the first run, so no literal was corrected and no production code changed to fit a test.

## Deviations from Plan

None - plan executed exactly as written.

One note on `tdd="true"` for Task 1: these tests pin behaviour that already exists in the merged tree (the lz4 and `compress=false` code is upstream's), so there is no meaningful RED phase. They were added in a single CMP commit as D-04 requires, with no `test(...)` commit.

**Total deviations:** 0
**Impact on plan:** none.

## Flagged findings

1. **V1 `?compress=true` is a no observation (RESEARCH A3 resolved).** On client V1 the operator's `jdbcConnectionProperties=?compress=true` did not change the node's `compress` option (it stayed `false` in S2 and S3). So V1 S3 succeeding does NOT show "both controls unnecessary on V1"; the strip never happened. What V1 does show: with the flag off and no operator override (S1), the server answers ZSTD and the response still decodes, because `compress=false` is set on the node. This is a V1 fact and cannot be generalised to "lz4 is redundant on V1 when compression is requested".
2. **Operator caveat (T-12-09, for ROLL-01).** On V2 an operator `?compress=true` wins over the connector's `compress=false` (pinned by `operatorCompressTrueOverridesV2ResponseCompression`). Combined with `setCompressionMethod=false` against a 26.9+ server this breaks reads (S3). The default (`setCompressionMethod=true`) is safe even with `compress=true` (S2).
3. **Premise not refuted.** S3 on V2 failed as the model predicted; there was no unexpected S3 success on V2.
4. **Image drift (A7).** `latest` resolved to 26.9.12.8 at probe time; the sentence above is bound to that build.

## Issues Encountered

- The first extract of the probe lines was truncated at the `<-` separator I used between cause-chain entries, because the XML-greppable pattern stops at `<`. The full V2 S3 text was recovered from the XML after un-escaping `&lt;`. Nothing else.

## Threat Flags

None. No new network endpoint, auth path or schema change. T-12-08 is mitigated by the 3 = 3 builder-site invariant and the live V2 and V1 flag-false round trips; T-12-10 by scrubbing (the table holds no credential text); T-12-11 by the probe check below.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 12-04 and 12-05 can use the invariant script as before (anchors now 40 rows, `NETWORK_COMPRESSION_METHOD_` = 0).
- 12-05 transcribes the drafted sentence and Recorded Facts. Finding 1 (V1 no observation) should be stated as such.
- ROLL-01 (Phase 15) must tell operators not to put `compress=true` in `jdbcConnectionProperties` together with `setCompressionMethod=false` on 26.9+ servers.
- `spotlessCheck` is red repo-wide and was not used as a gate; the new code follows the surrounding file style.

## Self-Check: PASSED

- Commit `9a4f7978` is an ancestor of HEAD; subject starts `feat(12-03):`; it touches exactly the five planned files.
- `git diff --numstat M HEAD` on the three test files shows 0 deletions.
- `D12CompressionProbeTest.java` is absent from the tree, from `git status` and from every commit since `pre-v1.7.0-sync` (`PROBE-ABSENT`).
- Credential grep acceptance: the credential-substring grep over this file prints 0.

---
*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Completed: 2026-10-07*
