---
phase: 15-suite-gate-publication-rollout
verified: 2026-10-10T17:00:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - CHANGELOG.md
  - VERSION
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
covered_digest: "v3:sha256:36004d37ffaa2b62aa23cc9230d84d68bbddac27522c00154a0e0c8c24bca0bf"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide whether to publish commit 505e4981 (docs-only fix of 15-REVIEW WR-01..WR-03) to origin"
    expected: "Either the two corrected docs reach origin/custom_build, or the published copy is knowingly left with the old text and a TRUNCATE example"
    why_human: "Publishing to a protected branch needs the user's direct approval (D-12/D-14). The published ROLLOUT item 2 still tells operators they may TRUNCATE a KeeperMap table that every exactlyOnce connector on the same zkDatabase and zkPath shares. Following that example erases other connectors' in-flight state."
---

# Phase 15: Suite Gate, Publication & Rollout Verification Report

**Phase Goal:** `v1.7.0_gg1` is published. The merged and hardened tree passes the full suite twice on ClickHouse `latest` x client V2. `custom_build` receives it by a non-force `--no-ff` merge after human approval, with CI green on the published SHA. The post-`c61d90ce` planning history is on `origin`, and operators have a written checklist for every behaviour change they will see.
**Verified:** 2026-10-10
**Status:** human_needed
**Re-verification:** No, initial verification

## Published versus local state

| Item | Published (`origin/custom_build` = `f328fd21`, tag `v1.7.0_gg1`) | Local `sync/v1.7.0` (HEAD `49fdb102`) |
|---|---|---|
| Source, build, VERSION, CHANGELOG | Final | Identical. `git diff f328fd21 HEAD -- src build.gradle.kts gradle VERSION CHANGELOG.md` is empty. |
| `docs/ROLLOUT-v1.7.0_gg1.md` | Old text: WR-01 (shared KeeperMap table treated as per-connector, with a `TRUNCATE TABLE` example), WR-03 (`version=*` advice wrong for regex matchers) | Fixed by `505e4981` |
| `docs/FORK-DELTA.md` L-05 line | Old text: WR-02 (`best_effort` stated as the default without a version) | Fixed by `505e4981` |
| Facts 39 and 40, 15-07 and 15-08 SUMMARYs, review, security and disposition files | Not on origin (D-14) | Present |

Only two files differ between published and local outside `.planning/`: the two docs above (`git diff --stat f328fd21 HEAD -- . ':!.planning'`). No code differs.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | After `clean`, VERSION and generated `Version.java` read `v1.7.0_gg1`. CHANGELOG has a `1.7.0_gg1` section naming R-01, WR-02 strictness, compression and lz4. FORK-DELTA shows D-17 retired, R-01 out of Known costs, the LZ4 rule and version, the MAP guard scoping, and the new patches and costs | VERIFIED | `git show f328fd21:VERSION` = `v1.7.0_gg1`. The generated `build/generated/sources/version/java/main/.../Version.java` has `ARTIFACT_VERSION = "v1.7.0_gg1"`. The CHANGELOG head section (read from `f328fd21`) names R-01, "WR-02 strictness", Compression and lz4-java 1.10.2 to 1.12.0. In the published FORK-DELTA, line 81 retires D-17, line 82 documents the single LZ4 rule and `lz4Java = "1.12.0"`, line 38 documents the MAP guard as Nullable-only, line 120 moves R-01 to "Closed since", and L-20 to L-22 plus the Nullable Enum/Tuple WARN row (line 43) are present. In source, ClickHouseWriter.java:660 is `value.getObject() == null && col.isNullable()`, `libs.versions.toml` has `lz4Java = "1.12.0"`, and `grep` finds no D-17 constants in `src/main` (the one `NETWORK_COMPRESSION_METHOD` constant is the setting name). |
| 2 | The full suite on `latest` x V2 passes twice (second with `--rerun`) with 0 failures, 0 errors and a skip set equal to FORK-DELTA's named skips. `testUnit` passes with Docker stopped | VERIFIED | `/tmp/gsd15/run1.log` (`clean test`) and `run2.log` (`test --rerun`): both BUILD SUCCESSFUL, 812 PASSED lines, 18 SKIPPED, 0 FAILED. Run 2 re-executed tests (812 PASSED lines, "3 executed"). Skip lists `run1-skips.txt` and `run2-skips.txt` are identical and match the FORK-DELTA table (1 + 2 + 11 + 1 + 1 + 2 = 18). Server 26.9.14.10 for both runs. I recomputed from the current `build/test-results/test/*.xml` (the post-publication `--rerun`): 69 classes, 830 tests, 18 skipped, 0 failures, 0 errors. `testUnit` with Docker down: `dockerinfo-down.rc` = `docker info rc=1`, `testunit.log` BUILD SUCCESSFUL. I re-ran `testUnit --rerun` myself (Docker up): 35 classes, 454 tests, 2 skipped, 0 failures, 0 errors. The Docker-stopped claim rests on the recorded artifact; I did not stop Docker. |
| 3 | The operator rollout checklist covers JMX MBean before/after, compression semantics with the `compress=true` caveat and server-profile check, R-01 with the exactly-once backfill (offset reset plus KeeperMap clear), JSON-route type notes, the nested-null `DataException` (DLQ-able) and the rollback tag | VERIFIED on content; the published copy carries a hazardous example, see Gaps and Human Verification | The published `docs/ROLLOUT-v1.7.0_gg1.md` has 8 items, each with what changed, an "Am I affected?" self-check and an action. Item 7 gives MBean `version=v1.6.0_gg1` to `v1.7.0_gg1`, with `connector=` stated. Item 6 gives the `compress=true` caveat and the `system.settings_profile_elements` check. Item 2 has the two-step backfill (offset reset, then state-row clear). Item 3 covers JSON-route type notes. Item 4 covers the `DataException` with DLQ routing under `errors.tolerance=all`. Item 8 names rollback tag `pre-v1.7.0-sync` (`c61d90ce`). Defect in the published copy: item 2's example offers `TRUNCATE TABLE <zkDatabase>`. `KeeperStateProvider.java:78-80` creates one KeeperMap table per `zkDatabase` keyed only by `<topic>-<partition>`, so the table is shared and a truncate wipes other connectors' state. Local `505e4981` replaces it with a topic-scoped `DELETE` and a "do not TRUNCATE" warning. I ran the new key-parsing expression in `clickhouse-local`: `orders-0` matches `orders`, `orders-v2-0` does not. |
| 4 | After recorded human approval, `custom_build` on origin is a non-force `--no-ff` merge. Tags `v1.7.0_gg1` and `pre-v1.7.0-sync` are on origin. The published SHA has exactly one completed `tests.yaml` run, green, no cancelled sibling | VERIFIED | `git ls-remote origin`: `custom_build` = `f328fd21`, `v1.7.0_gg1` = `f328fd21`, `pre-v1.7.0-sync` = `c61d90ce`. `f328fd21` has two parents, `c61d90ce` then `ae8520ed`, and `c61d90ce` is an ancestor, so the branch moved by fast-forward of the ref (no force) onto a real merge commit. Tree `9d9a7fbd` is identical to X (`ae8520ed`). The GitHub REST API `actions/runs?head_sha=f328fd21...` returns `total_count` 1: run 38050860929, `.github/workflows/tests.yaml`, event push, status completed, conclusion success. The approval is recorded verbatim (`publish`) in Fact 40 and 15-08-SUMMARY. |
| 5 | The `.planning/` history after `c61d90ce` is on origin through a merge, without `.planning/.gsd-ws-arg` or local-only tags | VERIFIED | 173 commits in `c61d90ce..f328fd21`, 120 of them touching `.planning`. The tree of `f328fd21` has 295 `.planning/` files and 0 `gsd-ws-arg`. `git log c61d90ce..f328fd21 -- .planning/.gsd-ws-arg` is empty (the two commits that touched it predate `c61d90ce`). Origin tags are exactly the four expected (`pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `v1.6.0_gg1`, `v1.7.0_gg1`). Local-only tags such as `v1.2.9_gg4-tests` are not on origin. |

**Score:** 5/5 truths verified (0 present-but-behavior-unverified, 0 overrides)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `VERSION` | `v1.7.0_gg1` | VERIFIED | Same on published and local. |
| `CHANGELOG.md` | `1.7.0_gg1` section | VERIFIED | Substantive, with the sections Important Changes, Bug Fixes and Dependencies. |
| `docs/FORK-DELTA.md` | Rewritten for the v1.7.0 tree | VERIFIED | Retired, kept and adopted rows, Known costs, named skip inventory (18). |
| `docs/ROLLOUT-v1.7.0_gg1.md` | 8-item operator checklist | VERIFIED (published copy has a defect) | See truth 3. |
| `src/main/.../ClickHouseHelperClient.java` | Nullable Enum/Tuple WARN (D-02) | VERIFIED | `hasNullableEnumOrTuple` and `warnOnceIfNullableEnumOrTuple` present, with unit and live tests. |
| `MetadataFreeJsonWriter` | Debezium Timestamp and NanoTimestamp as decimal seconds (WR-02) | VERIFIED | `MetadataFreeJsonWriterTest` D-01 cases PASSED in the gate log. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `f328fd21` | `c61d90ce` and `ae8520ed` | merge parents | WIRED | Confirmed with `git rev-list --parents`. |
| `v1.7.0_gg1` tag | `f328fd21` on origin | `ls-remote` | WIRED | |
| `custom_build` | CI run 38050860929 | `head_sha` | WIRED | |
| CHANGELOG and FORK-DELTA | rollout doc | links | WIRED | Both cite the doc. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free unit suite | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` | 35 classes, 454 tests, 0 failures, 0 errors | PASS |
| Full-suite totals | XML summed over `build/test-results/test` | 69 classes, 830 tests, 18 skipped, 0 failures, 0 errors | PASS |
| Operator SQL key parsing (local fix) | `clickhouse-local` on `orders-0`, `orders-v2-0`, `db_orders-12` | match, no match, no match for `'orders'` | PASS |
| Published CI | REST API by `head_sha` | 1 run, success | PASS |

### Probe Execution

Step 7c: SKIPPED. The phase declares no probe scripts.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SUITE-04 | 15-07 | Full suite twice, 0 failures and errors, named skips, `testUnit` Docker-free | SATISFIED | Truth 2. |
| PUB-01 | 15-03, 15-05, 15-07 | VERSION and CHANGELOG | SATISFIED | Truth 1. |
| PUB-02 | 15-01, 15-02, 15-03, 15-06 | FORK-DELTA reflects the new state | SATISFIED. The published L-05 line has the WR-02 wording defect; local `505e4981` fixes it. | Truth 1. |
| PUB-03 | 15-03, 15-04, 15-07, 15-08 | Non-force `--no-ff` merge after approval, tags, green CI | SATISFIED | Truth 4. |
| PUB-04 | 15-07, 15-08 | `.planning/` history after `c61d90ce` on origin via merge | SATISFIED | Truth 5. The unpublished local tail is intended (D-14). |
| ROLL-01 | 15-01, 15-05 | Operator rollout checklist | SATISFIED on content. The published copy contains the `TRUNCATE` hazard (see Human Verification). | Truth 3. |

All six requirement IDs appear in PLAN frontmatter (15-01 to 15-08) and in REQUIREMENTS.md. No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `docs/ROLLOUT-v1.7.0_gg1.md` (published) | item 2 | `TRUNCATE TABLE` of a shared KeeperMap table | Warning, pending a human decision | An operator following the example can erase other connectors' exactly-once state. |
| `docs/ROLLOUT-v1.7.0_gg1.md` (published) | item 7 | `version=*` given as the fix for regex matchers | Warning | Silently stops matching in regex-based rules. |
| `docs/FORK-DELTA.md` and rollout (published) | L-05 and item 3 | `best_effort` named as the default without a version | Info | Wrong on older servers. |
| `src/main/.../Column.java` | 168 | `FIXME` | Info | Verbatim upstream `v1.7.0` line in a file Phase 15 did not touch. Not a blocker. |

Open information-level review items IN-07 to IN-10 (15-REVIEW-DISPOSITION.md) are recorded as open and do not affect any truth.

### Human Verification Required

#### 1. Publish or defer the docs fix (`505e4981`)

**Test:** Decide whether the local docs-only commit `505e4981` goes to origin, for example as a follow-up commit on `custom_build`.
**Expected:** `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/FORK-DELTA.md` on `custom_build` carry the WR-01 to WR-03 fixes. The tag `v1.7.0_gg1` stays on `f328fd21`, so a documentation-only follow-up can be published without moving the tag.
**Why human:** Pushing to the protected `custom_build` branch needs the user's direct approval. This verifier may not push. Until it is done, the operator-facing checklist on origin includes a data-affecting `TRUNCATE` example. 15-SECURITY.md already notes that T-15-14 was reopened and then closed by the local fix only.

### Gaps Summary

No must-have truth failed. Every roadmap success criterion is met by evidence in the published tree and on origin, and the gate results were independently recomputed from the saved logs and test XMLs.

The only open matter is the published/local split. The rollout checklist on origin (`f328fd21`) still contains the hazardous `TRUNCATE` example and two wrong-advice defects found after publication. The fix exists locally. Because the phase goal says "operators have a written checklist", and the defect is in operator-facing text that could damage exactly-once state if followed literally, this is flagged for a human decision rather than counted as a blocker. All requirement elements of ROLL-01 are present in both copies.

---

_Verified: 2026-10-10_
_Verifier: Claude (gsd-verifier)_
