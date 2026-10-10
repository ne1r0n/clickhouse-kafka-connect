---
phase: 12-upstream-v1-7-0-merge-reconcile
verified: 2026-10-07T20:30:00Z
status: passed
score: 7/11 must-haves verified
covered_files:
  - .planning/MERGE-CHECKLIST-v1.7.0.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-01-PLAN.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-01-SUMMARY.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-02-PLAN.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-02-SUMMARY.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-03-PLAN.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-03-SUMMARY.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-04-PLAN.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-04-SUMMARY.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-05-PLAN.md
  - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-05-SUMMARY.md
  - .planning/scripts/fork-anchors.txt
  - .planning/scripts/verify-merge-invariants.sh
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java
covered_digest: "v3:sha256:1e3d16daa62642867e7286374b41fffffb9432e9c944ce7d34052e9b0e6f7961"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Backstop truth (12-01): line provenance is set-membership and ignores order and multiplicity; any mis-ordered or duplicated resolution in a conflicted file is caught by the D-03 compile + testUnit gate at M"
    expected: "Reviewer accepts that the set-membership provenance check plus the recorded D-03 gate at M is adequate for ordering and duplication errors in the 5 conflicted files, or asks for an ordered-hunk review"
    why_human: "Non-inferable design claim. I confirmed set-membership holds (independent re-check: 0 authored lines across 21 changed paths) and that the tree compiles and tests green at HEAD, but nothing in the repo exercises a mis-ordered or duplicated resolution to show the gate would catch it."
  - test: "Backstop truth (12-02): each byte pin builds its own writer and stream, so the pins are independent of test execution order"
    expected: "No order dependence between the Docker-free MAP/TUPLE/VARIANT pins in NestedNullableCompositeWriteTest"
    why_human: "I read all six test methods and each builds its own ClickHouseWriter and ByteArrayOutputStream, but no randomized-order run exists to confirm it."
  - test: "Backstop truth (12-02): with one capability rule left, LZ4 selection no longer depends on the registration order of configurations.all versus configurations.configureEach"
    expected: "Reviewer accepts the single remaining rule (build.gradle.kts:65) as order-independent"
    why_human: "Absence of a second rule is verified (one withCapability line, dependencyInsight shows one at.yawk.lz4:lz4-java 1.10.4 on runtimeClasspath and clickhouseDependencies). Order independence is a Gradle-semantics claim with no test."
  - test: "Backstop truth (12-05): pin outcomes do not depend on test execution order (full suite ran once)"
    expected: "Reviewer accepts one green full-suite run as sufficient, or schedules a second ordering"
    why_human: "Only one full-suite ordering was observed (691 tests, 0 failures); Phase 15 requires two clean runs."
  - test: "WRT-09 interpretation (judgment): REQUIREMENTS says CR-02 pin tests that asserted pre-#820 early-null bytes are re-pinned. In the tree, 0 existing assertions changed (git diff 54e63648 HEAD on NestedNullableCompositeWriteTest has zero removed lines): the three CR-02 pins sit on nullable columns and stay correct, and 3 new pins were added (non-nullable RowBinary {0x00}, non-nullable WithDefaults {0x00,0x00}, nullable through doWriteCol {0x01}), each with a 2026-10-07 rationale comment."
    expected: "Reviewer confirms 'nullable unchanged, non-nullable per #820, rationale next to each' is satisfied by adding pins rather than editing any, or asks for a different shape"
    why_human: "Literal reading of 're-pinned' (edit existing) differs from the delivered shape (additive). The 12-02 plan prohibition forbids editing existing assertions, so the two readings conflict and the call belongs to the user."
  - test: "D-17 FORK-DELTA wording (judgment prohibition): docs/FORK-DELTA.md line 77 says compress=false alone is sufficient on both clients, the lz4 setting alone is sufficient on V2 when an operator re-enables compression, and at least one control is required on V2; V1 stripped-both was not observed. Compare against Recorded Fact 26 in .planning/MERGE-CHECKLIST-v1.7.0.md."
    expected: "Every clause is derivable from the Fact 26 table and no control is claimed load-bearing beyond what was observed"
    why_human: "I compared the sentence to the table and found each clause supported, but the plan marks this prohibition 'judgment' and the probe class was throwaway, so the observation cannot be re-run here."
  - test: "SYNC-10 compile-only reading (judgment): the #800 Avro-union and cluster buildFromClause tests live in the integrationTest source set. They are byte-identical to refs/tags/v1.7.0 (git diff --quiet passes for all 11 unmodified upstream test-tree paths) and compileIntegrationTestJava passes, but they were never executed."
    expected: "Reviewer accepts 'compile-only, named in FORK-DELTA skip inventory' as the 'skip for a named reason' SYNC-10 allows"
    why_human: "REQUIREMENTS lets these skip for a named reason; whether compile-only plus byte identity counts is a user call (12-RESEARCH Open Question 3)."
  - test: "EOS-01 mutation evidence (could not reproduce): the restart-read test was recorded failing 4 of 6 against the pre-v1.7.0 summary-count read (Recorded Fact 28, 12-04-SUMMARY)."
    expected: "Optional: with Docker/colima running, temporarily revert getStateRecord in a throwaway worktree to the v1.6.0 read and confirm KeeperStateProviderRestartLiveTest goes red"
    why_human: "colima is not running in this environment so I could not re-run the mutation. I did verify the restart test is non-vacuous by reading it (fresh provider per read, distinct min 10 / max 19, beyond-int offsets, swapped-constructor-order detectable), the v1.6.0 to v1.7.0 diff of KeeperStateProvider shows exactly the summary-count to streamed-count change, and the live test passed in the 691-test XML."
---

# Phase 12: Upstream v1.7.0 Merge & Reconcile Verification Report

**Phase Goal:** Upstream `v1.7.0` is merged onto a disposable scratch branch with every fork feature intact and the three silent hazards in the merge resolved by intent (D-17 retired onto `setCompressionMethod`, #820 null-Map fix reachable, one LZ4 rule). Upstream's exactly-once state-read change is proven across a provider restart. `c61d90ce` stays one tag away.
**Verified:** 2026-10-07
**Status:** human_needed
**Re-verification:** No, initial verification

All five roadmap success criteria are met in the codebase and no blocker was found. The status is `human_needed` rather than `passed` because four plan `verification: backstop` truths cannot be confirmed from repo evidence, and four judgment items were recorded by the executors and need the user's call.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC1 | `pre-v1.7.0-sync` names `c61d90ce`. v1.7.0 sits on a scratch branch as one mechanical merge. Script is parameterised and passes provenance plus anchors. `custom_build`/`origin` untouched. | VERIFIED | `git rev-parse pre-v1.7.0-sync custom_build origin/custom_build` all `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`; tag is lightweight (`cat-file -t` = commit). `git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD` = exactly `54e63648`, parents `b1d26fd2` and `379b8d01` (= `refs/tags/v1.7.0`). `sync/v1.6.0` (`3b6736e3`) is an ancestor; no remote branch contains HEAD. `verify-merge-invariants.sh 54e63648 HEAD` rc=0, `result=PASS provenance_paths=21 anchor_commits=19 anchor_rows=40 marker_commits=66`. Self-test ST1-ST4 PASS. My own independent set-membership check over the 21 changed paths found 0 authored lines. Script takes `PRE_TAG`, `UPSTREAM_REF`, `ANCHORS_FILE`, `CONFLICTED_PATHS_FILE` overrides (lines 24-27). |
| SC2 | Merge checklist lists every upstream-changed path with predicted/actual class, and a verdict per auto-merged hunk in fork-touched files. | VERIFIED | `.planning/MERGE-CHECKLIST-v1.7.0.md`: Section 1 has 5 rows (matches `conflicted-paths-v1.7.0.txt`, order identical). Section 2 has 22 rows (matches `git diff --name-only v1.6.0 v1.7.0`, same order). Section 2a has 33 rows, equal to the sum of `@@` hunks I counted over the 8 fork-touched paths (1+5+4+6+1+6+9+1). Recorded Facts run 17 to 30. Only 3 `pending` rows remain, owned by phases 13 and 14. |
| SC3 | Every FORK-DELTA fork feature passes its pin. Upstream's new tests pass unmodified or skip for a named reason. One LZ4 rule. Catalog has no `clickhouse-jdbc`, keeps `kafka = "4.1.1"`. | VERIFIED (with human judgment on #800 and cluster `buildFromClause`, see Human Verification) | JUnit XML in `build/test-results/test`: 61 classes, 691 tests, 0 failures, 0 errors, 18 skipped, and no `src/` change since `0a0cf6b3`. Pin classes are non-empty and green: `RowBinaryUnsupportedTypeGateTest`, `JsonDynamicTypeMappingTest` 5, `JsonDynamicRoundTripTest` 6, `MBeanNameUnitTest` 11, `MBeanRegistrationLiveTest` 2, `ClickHouseHelperClientFork{Unit,Live}Test`, `MetadataFreeJsonInsertUnitTest` 13, `AsyncInsert*`, `ClickHouseVersionDefaultUnitTest`, the four SMT test classes. Upstream methods `nullMapTypeTest`, `nullMapTypeJsonInsertTest`, `nullMapTypesTest`, `optionalMapField*`, `testSetCompressionMethod_*`, `testSinkConfig_SetCompressionMethod`, `retriableFlushFailureDoesNotDuplicateBufferedRecords` all `ok`, none skipped. 11 of 12 upstream-changed test-tree paths are byte-identical to `v1.7.0`; the twelfth (`ClickHouseTestHelpers.java`) differs only by fork edits (UTF-8 readers, `CLICKHOUSE_VERSION_DEFAULT = latest`) and keeps upstream's change. `build.gradle.kts` has one `withCapability("org.lz4:lz4-java")`, identical to upstream's rule. Re-run `dependencyInsight` on `runtimeClasspath` and `clickhouseDependencies`: headline `at.yawk.lz4:lz4-java:1.10.4`, `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.10.4`. Catalog: `kafka = "4.1.1"`, no `jdbc`, no `FORK OVERRIDE (upstream 0.9.5)`. No `1.10.2` wording remains in `FORK-DELTA.md`, `.planning/MERGE-CHECKLIST.md`, `.claude/CLAUDE.md` or `STACK.md` (remaining hits are dated historical facts in the new checklist and MILESTONES). |
| SC4 | D-17 constants and pins gone. Every client receives `setCompressionMethod`. Docker-free pins on V1 options, V1 node, V2 config. Flag false drops lz4, response compression stays off. Live insert and read-back with true and false. FORK-DELTA records the load-bearing control. | VERIFIED (FORK-DELTA wording is a judgment item) | `git diff 379b8d01 HEAD` on `ClickHouseHelperClient.java` shows no compression delta against upstream, so the fork constants are gone and upstream's `setCompressionMethod` (lines 116, 149, 195) is the only mechanism, with `COMPRESS=false` / `compressServerResponse(false)` unconditional (lines 119, 152, 192). 3 `new ClickHouseClientBuilder(` sites in `src/main` (`ClickHouseWriter:84`, `:1590`, `KeeperStateProvider:50`) equal 3 `.setCompressionMethod(csc.isSetCompressionMethod())` sites (`:97`, `:1601`, `:59`). XML: `NetworkCompressionPinUnitTest` CMP-02 cases (V2 config, V1 node, V1 default options, flag-off, operator `compress=true` caveat) and `NetworkCompressionPinLiveTest` `client=V2/V1 setCompressionMethod=true/false` all `ok`; `NewUpstreamConfigDefaultsUnitTest` pins the 14th option. `FORK-DELTA.md:77` and Recorded Fact 26 record that `compress=false` alone is sufficient and at least one control is required on V2 under `compress=true`. |
| SC5 | Null optional Map into non-nullable Map produces #820 bytes (`00`, `00 00`), pinned Docker-free. CR-02 pins carry rationale. `KeeperStateProvider` restart-read test passes and was seen to fail against old summary-count code. | VERIFIED (WRT-09 shape and EOS-01 mutation reproduction are human items) | `ClickHouseWriter.java:614` guard is `value.getObject() == null && col.isNullable()`; non-nullable falls to `mapTmp == null` (writes varint 0 only with defaults). XML: `NestedNullableCompositeWriteTest` has the RowBinary `{0x00}` and WithDefaults `{0x00,0x00}` non-nullable pins plus the nullable `{0x01}` pin, all `ok`; each carries a rationale comment and builds its own writer and stream. Upstream `nullMapTypeTest` family passes live. `KeeperStateProvider.java` is byte-identical to v1.7.0 (`git diff 379b8d01 HEAD` empty). `KeeperStateProviderRestartLiveTest` 6 invocations `ok`: reads through a NEW provider, min 10 / max 19, offsets above `Integer.MAX_VALUE`, flag true and false, never-written key reads `NONE`. Mutation failure is recorded in Recorded Fact 28 but not reproduced here. |
| B1 | (12-02 backstop) Checklist Section 1 and 2 rows follow git path sort order | VERIFIED | `diff` of row path columns against `git diff --name-only v1.6.0 v1.7.0` and `conflicted-paths-v1.7.0.txt`: identical. |
| B2 | (12-05 backstop) SYNC-09 pins pass under the build's parallel forks, each with its own container | VERIFIED | Full-suite XML from a run using `maxParallelForks = availableProcessors/2` shows every pin class green. Directly observed in the results, not inferred. |
| B3 | (12-01 backstop) Provenance ignores order and multiplicity; D-03 gate catches mis-ordered or duplicated resolutions | INSUFFICIENT_SPEC | See Human Verification 1. |
| B4 | (12-02 backstop) Byte pins independent of execution order | INSUFFICIENT_SPEC | See Human Verification 2. |
| B5 | (12-02 backstop) LZ4 selection no longer depends on rule registration order | INSUFFICIENT_SPEC | See Human Verification 3. |
| B6 | (12-05 backstop) Pin outcomes do not depend on execution order | INSUFFICIENT_SPEC | See Human Verification 4. |

**Score:** 7/11 must-haves verified (5 roadmap criteria plus 2 backstop truths with direct evidence; 4 backstop truths abstained). 0 truths are present-but-behavior-unverified.

### Requirements Coverage

All 12 IDs appear in plan frontmatter and in REQUIREMENTS.md (checked `[x]`, "Complete"). No orphaned Phase 12 requirement.

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| SYNC-07 | 12-01, 12-05 | SATISFIED | SC1 evidence. |
| SYNC-08 | 12-02, 12-05 | SATISFIED | SC2 evidence. |
| SYNC-09 | 12-05 | SATISFIED | SC3 pin matrix, non-vacuous, green. |
| SYNC-10 | 12-05 | SATISFIED, human judgment on compile-only #800 and cluster tests | Byte identity plus 9 upstream methods passing. |
| CMP-01 | 12-03 | SATISFIED | Constants gone, 3 of 3 builder sites carry the flag. |
| CMP-02 | 12-03 | SATISFIED | Docker-free pins pass. |
| CMP-03 | 12-03, 12-05 | SATISFIED | Live V2/V1 x true/false read-back passes; Fact 25 and 26. |
| WRT-06 | 12-02 | SATISFIED | Scoped guard plus byte pins plus upstream tests green. |
| WRT-09 | 12-02 | SATISFIED in effect, shape is a human item | Additive pins; 0 existing assertions changed. |
| BLD-09 | 12-02, 12-05 | SATISFIED | One rule, one resolved version, wording fixed. |
| BLD-13 | 12-01 | SATISFIED | Catalog check above. |
| EOS-01 | 12-04 | SATISFIED, mutation not reproduced here | Restart test green, non-vacuous by inspection. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseSinkConfig.isSetCompressionMethod()` | every `ClickHouseClientBuilder` in `src/main` | `.setCompressionMethod(csc.isSetCompressionMethod())` x3 | WIRED |
| scoped MAP guard | upstream #820 `mapTmp == null` branch | non-nullable column falls through | WIRED (byte pins and `nullMapTypeTest` green) |
| `NetworkCompressionPinLiveTest` | `ClickHouseSinkTask` | start/put/stop with flag, `assertEquals` read-back | WIRED |
| `KeeperStateProviderRestartLiveTest` | `KeeperStateProvider` | two `new KeeperStateProvider(new ClickHouseSinkConfig(props))` | WIRED |
| `fork-anchors.txt` | `build.gradle.kts` `withCapability` | anchor count 1 (BLD-09) | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Merge invariants hold over `pre-v1.7.0-sync..HEAD` | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | rc=0, `result=PASS` | PASS |
| Invariant checks can fail | `bash .planning/scripts/verify-merge-invariants-selftest.sh` | ST1-ST4 PASS | PASS |
| One LZ4 artifact | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q dependencyInsight --dependency lz4-java --configuration runtimeClasspath` and `clickhouseDependencies` | single headline `at.yawk.lz4:lz4-java:1.10.4` | PASS |
| Suite totals | parse `build/test-results/test/*.xml` | 61 classes, 691 tests, 0 F, 0 E, 18 S | PASS |
| Live tests re-run | not run | colima is not running, so Docker-backed tests were not re-executed; results are taken from the existing XML | SKIP |

### Probe Execution

The phase's declared probe is `.planning/scripts/verify-merge-invariants.sh` and its self-test; both were run in my own process (above). No `scripts/*/tests/probe-*.sh` files exist.

### Anti-Patterns Found

None blocking. Scan of lines added after the merge in `src/`, `build.gradle.kts`, `docs/`, `.claude/` for `TBD|FIXME|XXX` found nothing. No existing test lines were removed after the merge (`git diff 54e63648 HEAD -- src/test src/testFixtures src/integrationTest` has zero `-` lines).

Advisory code-review findings (12-REVIEW.md, 0 critical / 3 warning / 5 info, all `open` in the disposition) were read. None invalidates a Phase 12 truth:

- WR-01 and WR-02 (nested null non-nullable Map and Tuple children) fall under Phase 13 success criterion 3 (WRT-07, WRT-08), which requires a `DataException` for those slots. Deferred, not a Phase 12 gap.
- WR-03 (compression live tests no longer reproduce the ZSTD failure mode) does not affect CMP-03 as written; it is a test-strength note.
- IN-01: the scoped MAP guard cannot fire for a server-reported schema because `Nullable(Map)` is invalid in ClickHouse. This strengthens #820 reachability, but the FORK-DELTA row describing it is worth rewording.
- IN-02: `FORK-DELTA.md` line 5 header still cites `sync/v1.6.0` commit `09cae191` and the `VERSION` row still reads `v1.6.0_gg1`. Phase 15 owns the final FORK-DELTA refresh.

### Gaps Summary

No gaps. Every roadmap success criterion has direct codebase evidence, all 12 requirement IDs are accounted for, and no prohibition was violated (no push, `custom_build` unmoved, no existing test edited, `KeeperStateProvider.java` carries zero fork edits, no dependency version beyond upstream's `1.10.4`). The phase cannot be marked `passed` because four non-inferable backstop truths and four executor-flagged judgment items need a human decision.

---

_Verified: 2026-10-07_
_Verifier: Claude (gsd-verifier)_
