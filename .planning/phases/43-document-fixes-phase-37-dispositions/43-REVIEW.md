---
phase: 43-document-fixes-phase-37-dispositions
reviewed: 2026-11-02T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - .github/workflows/increment-version.yml
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
  - docs/ROLLOUT-v1.7.0_gg6.md
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
  - .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md
findings:
  critical: 0
  warning: 3
  info: 2
  total: 5
status: issues_found
---

# Phase 43: Code Review Report

**Reviewed:** 2026-11-02
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Scope was `git diff cb1ccb66 HEAD`. `src/main` is unchanged (`git diff --stat cb1ccb66 HEAD -- src/main` is empty), so no production code moved. No existing assertion was weakened or deleted: the three test diffs are additive, except one one-token condition change in `ConfluentArchiveCheck.java` (`!= 1` to `> 1`).

I re-ran the four affected Docker-free classes (`ConfluentArchiveCheckUnitTest`, `ReleaseWorkflowGuardUnitTest`, `IncrementVersionWorkflowGuardUnitTest`, `CompareConfluentArchivesScriptUnitTest`) with `./gradlew testUnit --offline`: 121 of 121 passed.

The following checks held:
- **Tests:** The CHAIN addition matches the step order in both `release.yaml` and `pre-release.yaml` (lines 146 to 177). The new CIH-04 fixture discriminates, because the swap and the rename both fail the chain rule only now that `TAG_GATE_STEP` is in `CHAIN`. The CI-20 zero-jar test would have failed on the old `!= 1` condition. It asserts the exact stderr line, the absence of the hint prefix and the absence of the hint suffix.
- **Ledger pointers:** All four 40-hex pointers resolve to commits that carry the stated change. The ledger checker reports `18 rows, 0 problems` and `carried: 12 rows, 0 problems`.
- **Ledger row counts:** The 18 rows and 17 base IDs agree.
- **Rollout notes:** The gg6 correction notice is accurate, because only rows C2 and D7 differ from tag `v1.7.0_gg6`. The test that D7 names exists. The C2 claims check out against the code: `retryCount` is used only inside `ClickHouseHelperClient` pings and queries, `errors.retry.timeout` is read only at `ClickHouseSinkConfig` lines 413-417 to log a WARN, and the built-in set holds 15 codes.
- **Published facts, read from the GitHub API:**
  - Run `41298074815`, release `430511927`, asset size `13558692`, and the single `tests.yaml` run `41297502026` at the merged SHA all match.
  - PR #10 and PR #17 were closed unmerged by `dependabot[bot]` on 2026-11-01.
- **Raw numbers:** The repointed gg6 `git diff --shortstat` commands reproduce the stated figures against the tags.

The defects are in the published prose, not in the tests.

## Warnings

### WR-01: gg1 item 6 still overclaims that client V1 fails on the connector's `compress=true` plus `setCompressionMethod=false` combination, and contradicts FORK-DELTA

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:242`
**Issue:** The Phase 43 rewrite narrowed "client V1 of any line" to "client V1 on both client lines checked (`0.9.6` and `0.11.0`)". The sentence is still about the operator combination `compress=true` in `jdbcConnectionProperties` with `setCompressionMethod=false`, and says it "still fails with `Invalid LZ4 magic byte` or `Magic is not correct`". That combination was not measured through the connector on V1.

- The only V1 observation on `0.11.0` is `Client011BehaviourPinLiveTest.v1StillCannotDecodeZstd` (line 127). It builds a raw V1 `ClickHouseNode` with `compress=true` set directly in the node options, bypassing `ClickHouseHelperClient.createClientV1`.
- The connector's V1 node sets `compress=false` unconditionally in its options map (`ClickHouseHelperClient.java:156`).
- `docs/FORK-DELTA.md:131` says the opposite about the connector: "on V1 an operator's `?compress=true` did not reach the node options and the V1 stripped-both case was not observed (Recorded Fact 26)".
- The new gg1 text therefore overstates what was observed and contradicts FORK-DELTA in a published operator note. The gg6 row D13 is worded correctly ("client V1 cannot decode ZSTD on either client line") because it is about the client library, not the connector's combination.

**Fix:** Say what was measured, in the same labelled style as the rest of the item:
```
...; client V1 cannot decode a ZSTD response on either client line checked (`0.9.6` and `0.11.0`; measured on a raw V1 client with `compress=true`, row D13 of ROLLOUT-v1.7.0_gg6.md). Whether a V1 connector with `compress=true` in `jdbcConnectionProperties` reaches that path was not observed: the connector's V1 node forces `compress=false`.
```
Apply the same narrowing to `docs/FORK-DELTA.md:299`, which still says the combination "fails reads ... with `Magic is not correct` on client V1 on both client lines checked".

### WR-02: FORK-DELTA's closing sentence for the gg6 paragraph, and the status labels of items 12 and 19, disagree with the items Phase 43 closed

**File:** `docs/FORK-DELTA.md:295` (also lines 229 and 243)
**Issue:** Line 295 ends "The release published the open items 12 to 23 of Known costs; items 22 and 23 have closed since."

- Phase 43 also closed item 19 (retitled "(closed)" at line 243, commit `566f92e4`). The sentence omits it.
- PR #10 of item 12 is now closed, and the item's text says so. Its title still reads "(published knowingly open)".
- Items 19, 22 and 23 carry a status in their title. Item 12's title does not follow that convention, so a reader scanning the headings sees three different labelling schemes.
- The sentence is the explicit, enumerated "what is still open" statement of the document, and it is now false by one item.

**Fix:** Change the sentence to "items 19, 22 and 23 have closed since, and the pull request of item 12 was closed unmerged on 2026-11-01". Retitle item 12 to say what remains open (the 3.x pin and FUT-03) separately from the PR state, for example "protobuf-java stays on 3.x, PR #10 closed unmerged (the pin stays open)".

### WR-03: The repointing in FORK-DELTA is partial, so two published reproduction commands still give different numbers than the document states

**File:** `docs/FORK-DELTA.md:184` and `docs/FORK-DELTA.md:188`
**Issue:** Phase 43 repointed only the gg6 commands and the gg5-to-gg6 paragraph to tags. The following `HEAD`-relative commands remain, and they no longer reproduce:

- Line 184 gives `git diff --shortstat v1.7.0_gg4 HEAD` as the source of ".github at 6 files, 616 insertions and 92 deletions". Run today it reports 10 files, 1267 insertions and 108 deletions over `.github`. The correct command is `v1.7.0_gg4 v1.7.0_gg5`, which gives 6, 616 and 92.
- Line 188 says `git diff --quiet v1.7.0 HEAD -- <file>` "exits 0 for the 33 byte-identical files". I confirmed that `ClickHouseWriter.java` now exits 1 against `HEAD`, so at least that file no longer qualifies. The command is wrong at `HEAD` and right only against a tag.

The ledger (Handoff item 4) defers both to Phase 44's raw-numbers refresh, so this is a tracked deferral rather than an oversight. As shipped by Phase 43, though, the document holds two correct and two incorrect reproduction commands within a few lines of each other.

**Fix:** Either repoint these two to `v1.7.0_gg5` and `v1.7.0_gg6` now (a one-token change each; the figures are unchanged), or leave a sentence in the paragraph stating that the `HEAD`-relative commands were true only at the gg5 tip. Whichever is chosen, Phase 44's verifier should check that no `HEAD`-relative command is left in a measured-figures paragraph.

## Info

### IN-01: FORK-DELTA row 91 does not mention the pin that IN-04 added

**File:** `docs/FORK-DELTA.md:91`
**Issue:** The row describes `ReleaseWorkflowGuardUnitTest` as pinning "the compare chain in order before the tag gate and the publish step". Since commit `33d079a9`, `CHAIN` is the compare chain, then the tests gate, then the tag gate, then the publish step, and the guard fails if the tag gate moves or is renamed. The row's wording ("before the tag gate") suggests the tag gate is outside the pinned order. The row is now understated, not wrong. The `CHAIN` Javadoc and test display names still say "compare chain" for a chain that now includes both gates.
**Fix:** Reword to "the compare chain, the tests gate, the tag gate and the publish step in that order". Optionally rename the CIH-04 display name `workflowRunsTheCompareChainInOrder` to say "release chain".

### IN-02: gg1 notice sentence names "the release" without saying which tree the measurements are from

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:3`
**Issue:** The added clause says the item labels "which of its facts were measured on the release and which only in the maintainers' research". In a `v1.7.0_gg1` note, "the release" reads as `v1.7.0_gg1`, which shipped client `0.9.6`. The `0.11.0` facts labelled "measured" in item 6 come from the `v1.7.0_gg6` tip (`Client011BehaviourPinLiveTest`, rows D9 and D13 of the gg6 note). A reader holding only gg1 cannot tell that.
**Fix:** Say "measured on the `v1.7.0_gg6` tree" in the notice and in the item's "(measured)" label.

---

_Reviewed: 2026-11-02_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
