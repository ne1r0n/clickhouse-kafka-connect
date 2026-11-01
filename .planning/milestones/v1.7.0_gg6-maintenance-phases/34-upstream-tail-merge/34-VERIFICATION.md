---
phase: 34-upstream-tail-merge
verified: 2026-10-29T08:25:00Z
status: passed
score: 6/6 must-haves verified
covered_files:
  - .planning/phases/34-upstream-tail-merge/34-01-PLAN.md
  - .planning/phases/34-upstream-tail-merge/34-01-SUMMARY.md
  - .planning/phases/34-upstream-tail-merge/34-02-PLAN.md
  - .planning/phases/34-upstream-tail-merge/34-02-SUMMARY.md
  - .planning/phases/34-upstream-tail-merge/34-03-PLAN.md
  - .planning/phases/34-upstream-tail-merge/34-03-SUMMARY.md
  - .planning/scripts/fork-anchors.txt
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java
covered_digest: "v4:sha256:54b3fe97c7006ccc658623c1b03cc4da95d7f7388826e22a78940723387f9940"
behavior_unverified: 0
overrides_applied: 0
open_items:
  - item: "DataException wrapped-case decision (TAL-04 wording vs pinned interpretation)"
    owner: "user decision"
  - item: "10 three-argument ServerException sites for the Phase 36 client bump"
    owner: "Phase 36"
  - item: "FUT-05 rollout item (added retriable code is redelivered with no retry budget, outside errors.tolerance and the DLQ)"
    owner: "Phase 38"
---

# Phase 34: Upstream Tail Merge Verification Report

**Phase Goal:** The working tree holds upstream's untagged tail `9bed6f79`, merged in the fork's sync shape while the client is still 0.9.6. `additionalRetriableErrorCodes` is a supported, pinned setting whose unset default changes nothing. In `ProxySinkTask`, the fork's `connectorName` plumbing and upstream's `DBWriter` test seam coexist without an ambiguous constructor, and new anchor rows protect both.
**Verified:** 2026-10-29
**Status:** passed
**Re-verification:** No, initial verification

The starting hypothesis was that the tasks were done and the goal missed. Every claim below was re-checked against the tree and live git state. SUMMARY text was not used as evidence.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP Phase 34 success criteria are governing) | Status | Evidence |
|---|---------------------------------------------------------|--------|----------|
| 1 | SC1 (TAL-01): one mechanical merge of full SHA `9bed6f79`, tag `pre-v1.7.0-tail-2`, tail gate passing unedited, checklist listing every conflicted path | VERIFIED | `git rev-list --parents -n 1 adaf8c4e` returns parents `68f1b0cb` and `9bed6f7970670a55c66be8efda246889e1f95bd0`. The second parent equals `git rev-parse 9bed6f79`. Tag `pre-v1.7.0-tail-2` resolves to `68f1b0cb`, which is the first parent, and is a local commit tag. The merge changes exactly 27 paths, equal to the 27 paths upstream changed since merge-base `a6f1df83`, and 0 are `.planning` paths. Live `git ls-remote upstream main` is still `9bed6f79`. The newest upstream release tag is still `v1.7.0`. I re-ran the tail gate against the current tip: `PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e... HEAD` printed `provenance_paths=27 anchor_commits=23 anchor_rows=141 marker_commits=30 anchor_exempt=0 result=PASS` (exit 0). `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` holds the 5 roadmap paths (CHANGELOG.md, ProxySinkTask.java, Utils.java, ClickHouseSinkTaskMappingTest.java, ClickHouseWriterTest.java). `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` has a Section 1 row for each, all `done`, plus a Section 2 row per upstream-changed path. The gate scripts are byte-identical to commit `c9443756`, where the Phase 33 self-test last passed (`git diff --quiet` exit 0 against HEAD). |
| 2 | SC2 (TAL-02, TAL-03): `ProxySinkTask` has two constructors on one 4-argument package-private core with no ambiguous `null`. Every `handleException` caller is on the 5-argument form. The three compile tasks pass. | VERIFIED | `ProxySinkTask.java` lines 42-52: public `(config, errorReporter, String connectorName)` calls `this(..., connectorName, null)`. Package-private `(config, errorReporter, DBWriter)` calls `this(..., null, dbWriter)`. The only core is package-private `(config, errorReporter, String, DBWriter)`. The 4-argument call sites pass a typed `String` or `null` in the connectorName position, which cannot be ambiguous because one constructor has four parameters. No in-tree call passes a literal `null` third argument (grep of `src` for `new ProxySinkTask(`). The only production caller, `ClickHouseSinkTask.java:48`, passes `connectorName`. The statistics plumbing is kept: `new SinkTaskStatistics(connectorName, id)` at line 61, and `Processing` receives `this.dbWriter`. `grep handleException(` over `src` finds every caller on 5 arguments: `ProxySinkTask.java:110`, `ChunkFlusher.java:75`, `UtilsTest`, `UtilsNullMessageClassificationUnitTest:34` and the new pin class. `compileJava`, `compileTestJava` and `compileIntegrationTestJava` re-run on the current tree: all UP-TO-DATE, BUILD SUCCESSFUL, so they compiled this exact input. The separate commits are `608d70a1`, `4c9b3ed8` and `8bd35e59`. M3 itself took the fork side of `ProxySinkTask` (checklist row). |
| 3 | SC3 (TAL-04): tests pin parse/validate on both paths, the 15-code default including 159, the unset-key no-op, and DataException handling | VERIFIED (narrow DataException reading, see open items) | `AdditionalRetriableErrorCodesPinUnitTest` has 19 tests, 0 failures in the JUnit XML. Its `@DisplayName`s cover: the 15-code unmodifiable default set, the 0..1100 sweep on V2 and V1 with the key unset, the unset-key constructor and ConfigDef no-op, the validator registration, trim, de-duplicate and skip empty items, rejection of non-integer, non-positive and out-of-range values naming the key, and three DataException cases (plain, non-ClickHouse cause, writer through `ProxySinkTask`). Production code matches the pins. `Utils.DEFAULT_RETRIABLE_ERROR_CODES` is `Set.of(3,107,159,164,202,203,209,210,241,242,252,285,319,425,999)`, which is 15 codes and equals the pre-merge `switch` read from `git show pre-v1.7.0-tail-2:.../Utils.java`. `ClickHouseSinkConfig.parseErrorCodes` trims, uses a `TreeSet`, skips blanks, rejects `NumberFormatException` and values <= 0, and returns an unmodifiable set. It defaults to `""`, and `ErrorCodeListValidator` is registered at `ClickHouseSinkConfig.java:612`. `Utils.handleException` line 99 consults `additionalRetriableErrorCodes` only after the default set. The mutation run (dropping 159) is recorded in E-09. I did not repeat it. |
| 4 | SC4 (TAL-05): new anchor rows for the `ProxySinkTask` statistics and `connectorName` plumbing and for the `DEFAULT_RETRIABLE_ERROR_CODES` lookup. The anchors gate passes on the phase tip. Two full `--rerun` suites on `latest` x V2 are green with 18 named skips. | VERIFIED | `fork-anchors.txt` lines 157-167 hold 8 `ProxySinkTask.java` rows (ORCH-02, TAL-02, TAL-04) and the `Utils.java` row `DEFAULT_RETRIABLE_ERROR_CODES.contains(errorCode)` (TAL-05). The file has 141 rows, 10 more than the 131 at E-01. `34-evidence-check.py anchors` printed `rows=141 bad=0`. The tail gate PASS above covers `anchor_rows=141` across 23 commits. I re-ran the cumulative gate (`verify-merge-invariants.sh 54e63648 HEAD`): `provenance_paths=21 anchor_commits=1034 anchor_rows=141 marker_commits=1100 anchor_exempt=19 result=PASS` (exit 0). `build/test-results/test` holds a full-suite XML set. `34-evidence-check.py junit` printed `tests=2028 failures=0 errors=0 skipped=18 files=112`, and `skips` printed `OK skips: named=7 state_mismatch_cases=11 unexpected=0`. That set was written 07:18-07:21Z, which matches E-11's second run on the gated tip `7342319a`. E-11 records suite1 and suite2 both at 2028/0/0/18 on image `sha256:739e099a...`, ClickHouse 26.10.3.7. Only one XML set exists on disk, so suite 1's numbers rest on E-11 and the recorded logs, not on a surviving artifact. |
| 5 | Plan 34-03 handoffs: Phase 36 `ServerException` sites and the Phase 38 FUT-05 item are on record | VERIFIED | `grep -rn "new ServerException(" src` finds 10 sites: `UtilsTest` 8, `AdditionalRetriableErrorCodesTest` 1, the pin class 1, which matches E-10 and the 34-03 SUMMARY. FUT-05 is in `REQUIREMENTS.md:75` and `ROADMAP.md:216`. The SUMMARY and E-10 carry the rollout wording. |
| 6 | Plan 34-03 prohibitions (judgment-tier): nothing pushed, no edit of upstream-owned tests, anchors, gate scripts, release files, or `Utils`/`ClickHouseSinkConfig` to make a pin pass | VERIFIED | Evidence is deterministic git state, not only judgment. Origin `custom_build` is still `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. `git ls-remote --tags origin pre-v1.7.0-tail-2` and `git ls-remote --heads origin sync/v1.7.0` print nothing. `git branch -r` shows no `sync/v1.7.0` or tail tag, and the local branch has no upstream. `git diff --stat 8e7e4481 HEAD` (end of plan 34-02) over `src/main`, `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `.planning/scripts`, `UtilsTest`, `AdditionalRetriableErrorCodesTest`, `ClickHouseSinkConfigTest` and `34-evidence-check.py` is empty. Since M3, `src/main` changed only in `ProxySinkTask.java`, and `CHANGELOG.md`, `VERSION` and `FORK-DELTA.md` are untouched. |

**Score:** 6/6 truths verified (0 present-but-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` | Dual constructors on a 4-argument core, with `connectorName` plumbing | VERIFIED | Substantive and wired. `ClickHouseSinkTask:48` uses the public constructor. The seam constructor is used by `AdditionalRetriableErrorCodesTest`, `ProxySinkTaskConstructorUnitTest` and the pin class. |
| `src/test/java/.../sink/ProxySinkTaskConstructorUnitTest.java` | TAL-02 pins | VERIFIED | 4 tests, 0 failures in the XML. |
| `src/test/java/.../sink/AdditionalRetriableErrorCodesPinUnitTest.java` | TAL-04 pins | VERIFIED | 19 tests, 0 failures in the XML. |
| `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` | Every conflicted path and its resolution | VERIFIED | 5 conflicted rows, all `done`. |
| `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` | Conflicted-path record read by the tail gate | VERIFIED | Matches the roadmap's measured set. |
| `.planning/scripts/fork-anchors.txt` | 10 new rows | VERIFIED | 141 rows, `bad=0`. |
| `.planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md` | E-01..E-11, all `pass` | VERIFIED | The index shows 11 rows, all `pass`. E-11's recorded gate facts agree with my live re-reads. |
| `src/test/resources/connect-api-references.txt` | BLD-16 list re-blessed | VERIFIED | Changed in the phase. `ConnectApiReferencesUnitTest` is in the green `testUnit` run. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `ClickHouseSinkTask` | `ProxySinkTask` public constructor | `new ProxySinkTask(config, errorReporter, connectorName)` | WIRED | `ClickHouseSinkTask.java:48` |
| Public and seam constructors | 4-argument core | `this(...)` delegation | WIRED | `ProxySinkTask.java:43,47` |
| `ProxySinkTask` | `SinkTaskStatistics(connectorName, id)` | core constructor | WIRED | line 61, anchor row (1 hit) |
| `ProxySinkTask.put` and `ChunkFlusher` | `Utils.handleException` (5 arguments) with `getAdditionalRetriableErrorCodes()` | direct call | WIRED | `ProxySinkTask.java:110`, `ChunkFlusher.java:75` |
| `ClickHouseSinkConfig.CONFIG` | `ErrorCodeListValidator` and `parseErrorCodes` | ConfigDef key and constructor | WIRED | lines 285 and 612 |
| `Utils.handleException` | `DEFAULT_RETRIABLE_ERROR_CODES` and the added codes | `contains` lookup | WIRED | `Utils.java:99` |

### Data-Flow Trace (Level 4)

Not applicable. This phase moves control and classification flow, not rendered data. The config-to-classification path was traced in the table above, from `props` through `parseErrorCodes` to `handleException`.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite on the post-review-fix tree | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit`, which executed (not UP-TO-DATE) | `tests=1441 failures=0 errors=0 skipped=2`, equal to E-11's `testunit_expected_tests: 1441` | PASS |
| Compile gates | `./gradlew compileJava compileTestJava compileIntegrationTestJava` | UP-TO-DATE, BUILD SUCCESSFUL | PASS |
| Full-suite XML | `34-evidence-check.py junit` and `skips` on `build/test-results/test` | 2028 tests, 0 failures, 0 errors, 18 skipped, skips OK | PASS |
| Pin classes in the full-suite XML | read `TEST-...PinUnitTest.xml` and `TEST-...ConstructorUnitTest.xml` | 19 and 4 tests, 0 failures | PASS |

No new Docker full suite was started, as instructed.

### Probe Execution

The phase declares gate scripts, not `probe-*.sh` probes. Both gates were run read-only in my own process.

| Probe | Command | Result | Status |
|-------|---------|--------|--------|
| Tail gate | `verify-merge-invariants.sh adaf8c4e... HEAD` with the tail-2 environment | `result=PASS`, exit 0 | PASS |
| Cumulative gate | `verify-merge-invariants.sh 54e63648 HEAD` | `result=PASS`, exit 0 | PASS |
| Anchors mirror | `34-evidence-check.py anchors .planning/scripts/fork-anchors.txt` | `rows=141 bad=0` | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| TAL-01 | 34-01, 34-03 | Upstream tail merged mechanically, gate passes, checklist | SATISFIED | Truth 1 |
| TAL-02 | 34-02 | Dual `ProxySinkTask` constructors, no ambiguous `null` | SATISFIED | Truth 2 |
| TAL-03 | 34-01, 34-02 | 5-argument `handleException` everywhere, compile tasks pass | SATISFIED | Truth 2 |
| TAL-04 | 34-03 | `additionalRetriableErrorCodes` pinned | SATISFIED (pinned DataException interpretation) | Truth 3 |
| TAL-05 | 34-02, 34-03 | Anchor rows, anchors gate passes | SATISFIED | Truth 4 |

No orphaned requirements: `REQUIREMENTS.md` maps TAL-01..TAL-05 to Phase 34, and each appears in a plan.

### Anti-Patterns Found

I scanned the fork-authored delta, `ProxySinkTask.java` and the two new test classes, for `TBD`, `FIXME` and `XXX`. The tail gate's marker check also covers 30 commits and passed. Nothing blocking was found.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ProxySinkTask.java` | 62 | A null `dbWriter` silently selects the production `ClickHouseWriter` (review IN-01) | Info | Not reachable from production. It could surprise an in-package test author. Left open in `34-REVIEW-DISPOSITION.md`. |
| `ProxySinkTaskConstructorUnitTest.java` | 79-97 | Constructor-count pin is brittle and has a redundant assertion (IN-02) | Info | Left open. |
| `AdditionalRetriableErrorCodesPinUnitTest.java` | 157-178 | The unset-key sweep covers tolerance=all on V2 only (IN-03) | Info | The server-code branch is shared, so V1 under tolerance=all is not independently pinned. Left open. |

The review's one Warning (WR-01, MBean leak from seam-built tasks that call `put()`) was fixed in `ad38ab27`. I read the diff. It wraps the assertions in `try`/`finally { task.stop(); }`, changes no assertion, and touches only the two test classes. `testUnit` is green on the fixed tree.

### Human Verification Required

None. No truth is behavior-dependent without a test. The open items below are decisions and handoffs, not unverified must-haves.

### Open Items (reported, not gaps)

1. **DataException wrapped-case decision (user decision).** ROADMAP SC3 says "A `DataException` is never reclassified as retriable." Plan 34-03 pinned the narrower statement: adding codes never makes a `DataException` that has no ClickHouse server error and no network root cause retriable. Under the literal wording, two cases are not satisfied, and neither is a Phase 34 regression. `Utils.handleException` classifies the root cause first, in upstream code that was merged unedited:
   - A `DataException` whose cause chain holds a ClickHouse server error with a retriable code (default or added) is retriable.
   - A `DataException` whose root cause is a `SocketTimeoutException` or `UnknownHostException` is retriable under `errors.tolerance=all`.

   Changing either is a behaviour change to upstream code, so it needs the user's decision. The roadmap criterion was verified under the narrow reading, and the SUMMARY flags it. If the user wants the literal reading, that is a new plan.
2. **Ten 3-argument `ServerException` sites for Phase 36.** `UtilsTest` 8, `AdditionalRetriableErrorCodesTest` 1, the pin class 1. They compile on 0.9.6. Client-v2 0.11.0 has only the 4-argument constructor, so Phase 36 must change all ten at once. The ROADMAP note says "all eight", which is now stale.
3. **FUT-05 for Phase 38.** An added code raises `RetriableException`, which Connect redelivers outside `errors.tolerance` and the DLQ and with no retry budget. Only transient codes belong in the setting. This belongs in the rollout note.
4. **Post-gate tip drift (informational).** The two full `--rerun` suites ran on `7342319a`. The current tip adds `ad38ab27`, a test-only `try`/`finally` change in two Docker-free classes, plus documentation commits. I re-ran the full `testUnit` set on the current tree (1441/0/0/2), which covers both changed classes. I did not re-run the Docker full suite, per instruction. Phase 35 should treat the first full suite on its own tip as the next baseline confirmation.

### Gaps Summary

No gaps. Every roadmap success criterion for Phase 34 is supported by live repository state. The merge shape, tag, origin isolation, constructors, call sites, anchors, both gates and the recorded two-run full suite all check out. The open items above are decisions or handoffs for later phases, and none blocks Phase 35.

---

_Verified: 2026-10-29_
_Verifier: Claude (gsd-verifier)_
