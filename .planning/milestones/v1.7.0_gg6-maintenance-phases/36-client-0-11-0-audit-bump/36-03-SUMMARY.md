---
phase: 36-client-0-11-0-audit-bump
plan: "03"
subsystem: kafka-connect-sink
tags: [clickhouse-client, client-0.11.0, rowbinary-golden, confluent-archive, zstd-jni, guava, d-17, compression, CLT-05, CLT-06, CLT-08, CLT-02]

requires:
  - phase: 36-client-0-11-0-audit-bump
    provides: plan 36-01 golden blessed on 0.9.6 and the pins, plan 36-02 fix commit, catalog commit and the 0.11.0 green results
provides:
  - E-08, the RowBinary byte comparison - the 0.9.6 golden (96 lines) is green on 0.11.0 under four zones, the wire pin shows the same body, token and no Content-Encoding, no clean stop is required for the compared matrix
  - E-09 and five record files - the Confluent archive before and after the bump (7802 and 7933 inner entries) compared entry by entry with an empty allow-list, the zstd-jni cloud addition with three natives, the guava override shown redundant, not lost
  - E-10 and one documentation commit - the D-17 rationale reworded to the 0.11.0 measurements in two Javadocs, FORK-DELTA and the gg1 rollout item 6, plus the client-override, guava and httpclient5 wording
affects: [36-04, phase-37-archive-allow-list, phase-38-release]

actuals:
  tokens: 276392
  tasks: 3
  commits: 5

plan_head_before: 18f229d18680217f2033c84a5dafddb810a05b8c
plan_head_after: 4edde77702d176a9fa17efc6c36eb2eae2c64526

tech-stack:
  added: []
  patterns:
    - "Archives compared from detached worktrees at the commit before and at the catalog commit with an empty allow-list, so every difference is listed and none is explained away"
    - "A throwaway worktree with the override lines deleted proves a pin redundant without touching the main tree"
    - "Documentation edits applied from a block asset by a script that asserts one match per block"

key-files:
  created:
    - .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-DIFF.txt
    - .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-OUTER-BEFORE.txt
    - .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-OUTER-AFTER.txt
    - .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-INNER-BEFORE.txt
    - .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-INNER-AFTER.txt
  modified:
    - docs/FORK-DELTA.md
    - docs/ROLLOUT-v1.7.0_gg1.md
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
    - .planning/phases/36-client-0-11-0-audit-bump/36-COMMAND-EVIDENCE.md

key-decisions:
  - "No src/main, build, catalog, VERSION, CHANGELOG, workflow or gate-script file changed; the D-17 default itself is untouched and only Javadoc comment lines of the two compression pin tests changed"
  - "No clean stop is documented: a BEFORE_PROCESSING batch replayed across the upgrade carries the same bytes and the same deduplication token for the 48-case matrix"
  - "The guava pin and its stale comment stay in build.gradle.kts for a follow-up; this plan only records that the override is redundant"

requirements-completed: [CLT-05, CLT-06, CLT-08, CLT-02]

coverage:
  - id: D1
    description: "The RowBinary golden blessed on client 0.9.6 (96 lines, byte-identical to its commit, data hash unchanged) is green on client 0.11.0 under the workstation zone, UTC, America/Los_Angeles and Pacific/Kiritimati; the wire pin (2) and endpoint pins (3) are green; no clean stop is required for the compared matrix"
    requirement: "CLT-06"
    verification:
      - kind: integration
        ref: "RowBinaryClientParityLiveTest x4 zones, ClientInsertWirePinUnitTest, ClientEndpointAndOptionsPinUnitTest; 36-COMMAND-EVIDENCE.md E-08; P36-03-T1-OK, P36-03-T1-TESTS-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The Confluent archive before (7802 inner entries) and after (7933) the catalog commit is listed in five record files and compared with an empty allow-list: 4 only-before, 117 only-after, 94 changed, 7302 identical, manifest identical, no nested jar, no Kafka API class, no connector or guava class changed; zstd-jni 1.5.7-23 cloud adds 37 classes, 17 multi-release classes and three natives; the guava override is redundant, not lost"
    requirement: "CLT-08"
    verification:
      - kind: unit
        ref: "36-ARCHIVE-DIFF.txt; 36-COMMAND-EVIDENCE.md E-09; P36-03-T2-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "The D-17 default stays and its rationale is reworded to the 0.11.0 measurements in the two Javadocs (comment lines only), the FORK-DELTA D-17 row and eighth-behaviour paragraph and the gg1 item 6, with the client-override, guava and httpclient5 wording, in one four-path commit"
    requirement: "CLT-05"
    verification:
      - kind: unit
        ref: "NetworkCompressionPinUnitTest (9), SourcePlanningIdGuardUnitTest (18), ClientCatalogPinUnitTest (2); 36-COMMAND-EVIDENCE.md E-10; P36-03-T3-TESTS-OK; P36-03-T3-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the reworded operator-facing text reads correctly to a human operator is not asserted by any test; the claims it makes are pinned by Client011BehaviourPinLiveTest or labelled as measured in the public confluentinc/cp-kafka-connect-base:7.9.7 image"
    requirement: "CLT-05"
    verification: []
    human_judgment: true
    rationale: "Wording quality and operator comprehension are judgment, reviewed with the rollout items in plan 36-04 and the Phase 38 release review"

duration: 8min
completed: 2026-10-29
status: complete
---

# Phase 36 Plan 03: Byte Comparison, Archive Audit and the D-17 Rewording Summary

**The 0.9.6 RowBinary golden holds byte for byte on client 0.11.0 under four time zones with the same wire body and deduplication token, the Confluent archive grows by 131 inner entries (zstd-jni with three natives, no connector or guava class changed, guava override redundant, not lost), and the D-17 rationale is reworded to the 0.11.0 measurements in one revertable documentation commit.**

## Performance

- **Duration:** about 8 min (start 2026-10-29T13:37:02Z, end about 13:45Z)
- **Tasks:** 3
- **Files:** 10 (5 new record files, 2 documents, 2 test Javadocs, 1 evidence file)

## Accomplishments

- **CLT-06 byte comparison (E-08).** The golden file is byte-identical to its 0.9.6 commit (`83d0b42a`), its header line reads `# blessed on client-v2 0.9.6 (revision: 5777f5a)`, its 96 data lines hash to `716b0f7e...acb6e` as E-04 recorded, and `RowBinaryClientParityLiveTest` is green (1 execution) on client-v2 0.11.0 under the workstation zone, `UTC`, `America/Los_Angeles` and `Pacific/Kiritimati`. The wire pin (2) and endpoint pins (3) are green on 0.11.0: the same body, the same `insert_deduplication_token` and no `Content-Encoding`. The token is built from the batch (`topic-partition-minOffset-maxOffset`), and `QueryIdentifier`, `ClickHouseWriter`, `processing` and `state` are unchanged since the phase start. `clean_stop_required: no`, for 48 type cases x 2 variants including the nested Tuple and Map nullable markers; a column type outside the matrix, a time-zoned DateTime and a Variant fed plain text were not compared.
- **CLT-08 archive audit (E-09).** Both archives built (exit 0) from detached worktrees at `catalog_commit^` and `catalog_commit`: `verifyConfluentArchive` printed 7802 and 7933 inner entries, no nested jar, no Kafka API class; zips 12,456,907 and 13,558,637 bytes. The compare script with an empty allow-list printed `totals files-a 7400 files-b 7513 only-a 4 only-b 117 changed 94 same 7302`, `MANIFEST.MF: identical`, `nested jars in b: 0`, `kafka classes in b: 0` (exit 1 by design). The four only-before entries are `client-v2-version.properties` and the three LZ4 stream and entity classes of client V2. The 131 more inner entries are 113 more files and 18 more directories. No connector class and no guava class differs.
- **zstd-jni (E-09).** `zstd-jni` 1.5.7-23 arrives only through client-v2 0.11.0's own POM, as a compile dependency with the `cloud` classifier (jar SHA-256 `37589ad5...e7b3`, license BSD 2-Clause): 37 classes, 17 multi-release classes under `META-INF/versions/22/` and exactly three natives (`darwin/aarch64` dylib, `linux/aarch64` so, `linux/amd64` so) against 18 in the plain jar. The consequence for other platforms is read from the library's loader in the research, not re-measured.
- **guava, httpclient5, lz4 (E-09).** Resolutions before and after: guava `33.4.6-jre -> 33.7.2-jre` then plain `33.7.2-jre` (requested by `com.clickhouse:client-v2:0.11.0`, no arrow); httpclient5 `5.4.4 -> 5.6.5` then `5.6.4 -> 5.6.5`; lz4 `1.10.2 -> 1.12.1` then `1.11.4 -> 1.12.1`. In a throwaway worktree with both `libs.guava` lines deleted (numstat `0/2`) guava `33.7.2-jre` still resolves on `runtimeClasspath` and `clickhouseDependencies`: the override is redundant, not lost. `build.gradle.kts` was not edited in the main tree.
- **CLT-05 rewording (E-10).** One commit of four paths: FORK-DELTA 5/5 lines (catalog-pins row with the override marker and `ClientCatalogPinUnitTest`, D-17 row, guava row, httpclient5 row, eighth-behaviour paragraph), ROLLOUT gg1 2/2 lines (header notice and item 6 with `Correction for client 0.11.0`, original sentence kept), and Javadoc comment lines only in the two compression pin tests (5/1 and 6/3). The tree compiles, `NetworkCompressionPinUnitTest` (9), `SourcePlanningIdGuardUnitTest` (18) and `ClientCatalogPinUnitTest` (2) are green, both test files are byte-identical after Spotless, and Recorded Fact 26 is untouched.
- Every evidence entry records ClickHouse 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`. No JVM crash occurred. The 18 named skips were not touched.

## Commits

| Commit | SHA | Paths |
|--------|-----|-------|
| evidence E-08 | `999453e6` | `36-COMMAND-EVIDENCE.md` |
| listings (CLT-08) | `4b4d488133189f637baa1f2fc75fa327eccbcba6` | `36-ARCHIVE-DIFF.txt`; `36-ARCHIVE-INNER-AFTER.txt`; `36-ARCHIVE-INNER-BEFORE.txt`; `36-ARCHIVE-OUTER-AFTER.txt`; `36-ARCHIVE-OUTER-BEFORE.txt` (all under `.planning/phases/36-client-0-11-0-audit-bump/`) |
| evidence E-09 | `04ed4630` | `36-COMMAND-EVIDENCE.md` |
| documentation (CLT-05, CLT-02, CLT-08) | `375c93be91d2a32eafd4e0364f905e51f8dfd280` | `docs/FORK-DELTA.md`; `docs/ROLLOUT-v1.7.0_gg1.md`; `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java`; `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java` |
| evidence E-10 | `4edde777` | `36-COMMAND-EVIDENCE.md` |

**Plan metadata:** the SUMMARY commit that follows this file. `STATE.md`, `ROADMAP.md`, `.planning/config.json` and `.planning/state.json` were not staged (orchestrator-managed, per the run instructions).

## Handoffs

For plan 36-04:
- `docs_commit` = `375c93be91d2a32eafd4e0364f905e51f8dfd280` joins the revert set beside `catalog_commit` `5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8` (it reverts cleanly: nothing builds on it). The createClientV2 `fix_commit` `a1cc33825a07d960bc336e0e6c7474c6698ab40b` stays on a revert.
- E-08 for the rollout items: the replay statement (no clean stop for the compared matrix; User-Agent is the only request header that differs), the not-compared sentence and the labels.
- E-09 for the rollout items: totals `7400 -> 7513` files, `4 / 117 / 94 / 7302`, three natives of the cloud classifier against 18 in the plain jar (worker platforms outside darwin/aarch64, linux/aarch64 and linux/amd64 cannot load the native when a ZSTD path runs; research, not re-measured), the zstd native needs an executable temp folder (research, public image), guava redundant, httpclient5 and lz4 pins still win, resolved versions as listed above.
- E-10: the reworded D-17 text; the gg1 item 6 carries a dated correction and keeps the original sentence.
- Anchors count is `152`; the anchors mirror printed `rows=152 bad=0` on the committed tip.

For Phase 37:
- The archive allow-list must name the 37 `com/github/luben/zstd/` classes, the 17 `META-INF/versions/22/com/github/luben/zstd/` classes and the three natives; the VERSION table of the compare script has no `zstd-jni` row.
- The stale `// temp to override client's with CVE` comment and the redundant guava pin in `build.gradle.kts` are a follow-up.

For Phase 38:
- RLS-01 refreshes the raw FORK-DELTA numbers once.

## Deviations from Plan

None that change the repository result. Three process notes:

1. **[Rule 3 - Blocking] Plan verify defect, T3 first block.** The check `git diff --quiet {L0} {docs_commit} -- src/main gradle build.gradle.kts VERSION CHANGELOG.md .github .planning/scripts` compares against the phase start (`99b9d85c`), but plan 36-02's own commits legitimately changed `ClickHouseHelperClient.java`, `gradle/libs.versions.toml` and `.planning/scripts/fork-anchors.txt` after it, so the block exits silently there. I re-ran the whole block with that single baseline moved to the end of plan 36-02 (`18f229d1`): `git diff --quiet` over those paths is clean from `18f229d1` to the documentation commit and the block printed `P36-03-T3-OK`. The other four blocks printed `P36-03-T1-OK`, `P36-03-T1-TESTS-OK`, `P36-03-T2-OK` and `P36-03-T3-TESTS-OK` unmodified.
2. **[Rule 3 - Blocking] E-08 `dedup_token_source`.** The plan asks for the `String.format` line copied verbatim and also expects the value to contain `getDeduplicationToken`; the verbatim line does not. The value is `QueryIdentifier.getDeduplicationToken: return String.format("%s-%s-%s-%s", topic, partition, minOffset, maxOffset);`, corrected before E-08 was committed.
3. **Tooling and timestamps.** The 15 documentation blocks were applied by a script that asserts exactly one match per block and writes the result, instead of 15 Edit-tool calls; the result is the same bytes. The `Measured` timestamp of E-08 reads 13:41:00Z while the clock at that moment was a few minutes earlier (the runs finished before 13:40Z); the committed text is not edited, per the evidence rule.

**Total deviations:** 2 auto-resolved (Rule 3), 1 note.
**Impact:** none on the committed content or the results.

## Authentication Gates

None.

## Issues Encountered

None. No JVM crash occurred (no `javacore`, core dump, `Snap` or `jitdump` file). The local `.planning/config.json` and `.planning/state.json` modifications predate the phase and were never staged. All three throwaway worktrees (`arch-before`, `arch-after`, `guava`) and the `fmt` worktree are removed; `git worktree list` shows only the main tree.

## Known Stubs

None. No hardcoded empty value, placeholder or unwired data was added; no skipped or disabled test was added or removed.

## Threat Flags

None beyond the plan's register. T-36-11 (hygiene scan clean over the five record files, the evidence and the added documentation lines), T-36-12 (every 0.11.0 claim is pinned by `Client011BehaviourPinLiveTest` or labelled as measured in the public image; the gg1 sentence is kept), T-36-13 (both archives from detached worktrees, catalog line of each recorded, no connector class differs) and T-36-14 (the guava experiment ran only in a removed worktree) are mitigated as planned.

## Items for the user

- Nothing was pushed or dispatched; origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. PR #17 was not touched.
- CLT-07's manual nightly dispatch is still waiting for the user (plan 36-04 writes the handoff).

## Self-Check: PASSED

The five record files and the four documentation paths exist on disk, the five plan commits (`999453e6`, `4b4d4881`, `04ed4630`, `375c93be`, `4edde777`) are ancestors of HEAD, the listings commit holds exactly the five record files and the documentation commit exactly the four paths, `P36-03-T1-OK`, `P36-03-T1-TESTS-OK`, `P36-03-T2-OK`, `P36-03-T3-TESTS-OK` and (with the baseline correction above) `P36-03-T3-OK` were printed on the committed tip, E-01..E-10 are `pass` with the index still E-01..E-13, and no worktree other than the main tree remains.
