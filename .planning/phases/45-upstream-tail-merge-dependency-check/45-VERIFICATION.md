---
phase: 45-upstream-tail-merge-dependency-check
verified: 2026-11-05T00:30:00Z
status: passed
score: 7/7 must-haves verified
covered_files:
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-01-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-01-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-02-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-02-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-03-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-03-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-04-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-04-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-05-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-05-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-06-PLAN.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-06-SUMMARY.md
  - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonRouteValueFreeDlqUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterRecordContextDlqUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/JsonRouteValueFreeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/SlashColumnRoundTripTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterRecordContextUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/FieldDescriptorLiteralFieldsUnitTest.java
covered_digest: "v4:sha256:e85b24bfdffd68680aebbcfc604912319aab3977b33d120983d881f2612f37b2"
behavior_unverified: 0
overrides_applied: 0
resolutions:
  - item: "WR-02 suppressed exceptions on the kept IOException suffix, fail-open idiom"
    decision: "accepted as a residual, no production change; FORK-DELTA wording qualified (02baac3a)"
    decided_by: user (relayed by the coordinator)
  - item: "WR-01 write-timeout prefix and Map-lambda shape unpinned"
    decision: "fixed by test-only pins (876a9bd1), evidence E-19 (a7b67d36)"
    decided_by: user (relayed by the coordinator)
  - item: "DEP-02 record wording (in spirit, not verbatim)"
    decision: "acceptable as is"
    decided_by: user (relayed by the coordinator)
---

# Phase 45: Upstream Tail Merge and Dependency Check Verification Report

**Phase Goal:** The code-gap phases start from a tree that already holds upstream's tail, so a later red points at the fork's own change and not at the merge; the tail is merged in the fork's sync shape; upstream #857's record context is kept only where it carries no record value (column, types and exception class visible, never the value, retry classification unmoved); slash column names (#839) round-trip and `fromJsonRow` agrees with the fork's raw-identifier names; the shipped dependencies have been checked for open CVEs and every bump is the user's decision.
**Verified:** 2026-11-04
**Status:** passed
**Re-verification:** Yes, after the three user decisions (WR-01 pins, WR-02 residual accepted, DEP-02 record accepted); initial status was human_needed

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | UPS-01 / SC1: rollback tag, scratch branch, one mechanical merge, generated per-path checklist, tail gate PASS | VERIFIED | `git rev-parse pre-v1.7.0_gg8` = `baa404cd`, equal to M's first parent. M = `12e2cf17`, parents `baa404cd` and `83808df9` (= `upstream/main`, re-read by `git rev-parse`). `git diff M^1 M --stat` is exactly 6 paths (+298/-5), no `.planning` file. The six conflicted paths are byte-identical to the first parent except the one added catalog line (`gradle/libs.versions.toml` 1+). `MERGE-CHECKLIST-v1.7.0-tail-3.md` has Sections 1, 2, 2a, 3, one row per conflicted path (6), every upstream-changed path (11) and the auto-merged hunks. I ran the unedited gate `PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9... verify-merge-invariants.sh 12e2cf17 HEAD` on HEAD: `invariants: provenance_paths=11 anchor_commits=39 anchor_rows=200 marker_commits=57 anchor_exempt=0 result=PASS`. Tag and branch absent on origin; `origin/custom_build` unmoved (`94834763`). |
| 2 | UPS-02 / SC2: no error message, DLQ header or log line of a serialization failure carries a record value; #857's context kept only where value-free | VERIFIED (accepted residual WR-02) | #857's `serializationFailure` (message from `cause.getMessage()`, ERROR log with throwable) is gone (anchor row count 0). `writeColumnWithRecordContext` rethrows the writer's own exception and logs `RECORD_CONTEXT_FAILURE_LOG_FORMAT` with five placeholders (topic, partition, offset, column, exception class name), no throwable. `withheldJacksonChain` rebuilds every Jackson-bearing pass-through chain on the RowBinary route (`doWriteColValue` line ~1083) and the JSON route (`writeRecordsJson`, failure path only, `JSON_ROW_FAILURE_FORMAT` has table and class only). Ran: `WriterRecordContextUnitTest`, `WriterRecordContextDlqUnitTest`, `JacksonOverIoValueFreeUnitTest`, `JsonRouteValueFreeUnitTest`, `JsonRouteValueFreeDlqUnitTest`, `ClickHouseWriterRecordContextTest` all pass (491 tests, 0 failures in the combined run below). Measurements precede the repair: E-06 (15 shapes) and E-10 (9 lines), probes in removed worktrees, never committed (`git log --all` for the probe classes is empty, one worktree). Suppressed-exception residual on the kept IOException suffix: accepted by the user, FORK-DELTA qualified (see Resolution of the human items). |
| 3 | UPS-02 / SC2: retry classification does not move; gg3 pins and the 216-case matrix pass unchanged | VERIFIED | `WriterFailureClassificationUnitTest`: 216 PASSED lines in my run. `git diff pre-v1.7.0_gg8 HEAD -- src/test` touches only new files, the two named upstream-test edits (`ClickHouseWriterRecordContextTest` two constructor lines + three coordinate assertions, `FieldDescriptorEscapeTest` two assertions) and `WriterValueFreeFailureUnitTest` (three renamed methods, user-approved). Every other gg3 pin file is unchanged. `Utils.java` diff is two `private` to `public` keywords on the timeout-prefix constants (user-approved). Live `WriterErrorPathLiveTest` WRT-10 PASSED against ClickHouse `latest` x V2 in my run. |
| 4 | Retry classification kept on a Jackson root with the client write-timeout prefix (sub-branch of 3) | VERIFIED | Re-checked after 876a9bd1: `JacksonOverIoValueFreeUnitTest` gains `PLAIN_IO_OVER_WRITE_TIMEOUT_PREFIXED_JACKSON_ROOT` and `RUNTIME_OVER_JACKSON_OVER_READ_TIMEOUT_SOCKET` (both driven by the five matrix checks) plus `staysRetriableAndValueFree`, `writeTimeoutPrefixOfAJacksonRootSurvivesOnTheRebuiltRoot` and `mapLambdaShapeDropsTheJacksonLinkAndKeepsTheTimeoutAsTheDirectCause`; 109 cases. I ran the class with `WriterFailureClassificationUnitTest` and `WriterValueFreeFailureUnitTest`: 354 passed, 0 failed. Mutation done by me in a throwaway detached worktree (removed afterwards, repo tree untouched): deleting the `Write timed out after` `else if` in `withheldJacksonRoot` turns 5 tests red (`keepsTheRetryOutcome`, `staysRetriableAndValueFree`, `keepsTheClassifiedLinks`, `rebuildIsValueFreeAndKeepsTheSuffix` on the new shape, and `writeTimeoutPrefixOfAJacksonRootSurvivesOnTheRebuiltRoot`). E-19 records the same plus a second mutation (RuntimeException top rebuilt with a null cause: 9 red). `git diff 1b076b1d HEAD -- src` shows only the test file; no production change. |
| 5 | UPS-03, UPS-04 / SC3: slash column round-trips on `latest` x V2; `fromJsonRow` reconciled, NAME-01..07 unchanged, differences pinned and named | VERIFIED | `SlashColumnRoundTripTest` is byte-identical to upstream's (`git diff 83808df9 HEAD` empty) and passes live in my run, V1 and V2 (DESCRIBE keeps `a/b`, `café/温度`; insert stores 1.5, 2.5, 3.5, read back typed with `getDouble`). `RawNameMatrixLiveTest` also green (39 tests, 0 failures, 0 skipped in the live run). NAME-pin files unchanged since the tag. The `fromJsonRow` difference (names-only decode, other fields literal JSON, malformed-row message never echoes the row) is pinned by the new `FieldDescriptorLiteralFieldsUnitTest` (all cases PASSED) and by the edited upstream `FieldDescriptorEscapeTest`, and named in the FORK-DELTA Discovery row and the "Tests changed by a user decision" row. |
| 6 | UPS-05 / SC4: bumps resolve; invariant script PASS on explicit SHAs; anchor count moves with its rows; two full green suites | VERIFIED | `dependencyInsight`: `slf4j-simple:2.0.20` (integrationTestRuntimeClasspath), `commons-lang3:3.21.0` (testRuntimeClasspath), `connect-json:4.4.0` on test only and absent from `clickhouseDependencies`; spotless `8.10.4` in the catalog and the build resolves it. Gate PASS (see 1); `fork-anchors.txt` gains 10 rows (190 to 200), and the gate (anchor-count invariant) passes. Two full suites: E-18 records 2675 tests, 0 failures, 0 errors, 18 skipped, twice, on ClickHouse 26.10.3.7 (digest recorded), plus `testUnit` 2072 with `DOCKER_HOST=unix:///nonexistent.sock`. I did not re-run the full suites (3 min each); I re-ran the 491 Docker-free named tests and the 39 live tests above. Note: both recorded full runs used `-PjavaVersion=21` because the OpenJ9 17 toolchain crashed twice with the known GPF (memory/swap pressure), recorded and disclosed in E-18. |
| 7 | DEP-01, DEP-02 / SC5: dependency and CVE check recorded, bumps are the user's decision | VERIFIED (record wording accepted by the user) | `45-DEPENDENCY-CHECK.md`: 27 shipped coordinates (each with resolved version, latest stable, OSV and GitHub counts, archive cross-check), test, integration-test, plugins, wrapper, actions and images sections; 16 test-scope advisories P1-P16 named with CVE ids. Spot-check I ran against OSV.dev: lz4-java 1.12.1, guava 33.7.2-jre, commons-compress 1.28.0, jackson-databind 2.22.4, httpclient5 5.6.5 all return `{}`; control `jackson-databind@2.9.0` returns 74. All 16 declined (user confirmed in the request; `45-CONTEXT.md` line 34); `git diff 12e2cf17 HEAD --stat -- .github build.gradle.kts gradle` shows only the earlier catalog/anchor changes, no bump commit. "None approved, nothing moves" holds. Caveat: the record says the decision was relayed and is "in spirit, not verbatim". |

**Score:** 7/7 truths verified (0 behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` | Per-path checklist | VERIFIED | 6 conflicted rows, 11 upstream-changed rows, Section 2a hunks, Recorded Facts 46-47 |
| `.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt` | Gate input | VERIFIED | six paths |
| `ClickHouseWriter.java` (`withheldJacksonChain`, `writeColumnWithRecordContext`, `writeRecordsJson` guard) | Value-free rebuild | VERIFIED | substantive, called at 3 sites, anchors 2/2/2/3 |
| `Utils.java` | Constants public only | VERIFIED | diff is visibility only |
| `docs/FORK-DELTA.md` | Names every difference | VERIFIED (IN-01 corrected in 02baac3a) | rows for RowBinary seam, JSON route, `fromJsonRow`, edited tests; see IN-01 |
| New Docker-free pins (5 classes) | Red first, then green | VERIFIED | all pass; red-first recorded in E-07, E-08, E-11, E-13 |
| `45-DEPENDENCY-CHECK.md` | DEP-01 record | VERIFIED | sections 1-7 |
| `fork-anchors.txt` | rows moved with their commits | VERIFIED | 200 rows, gate PASS |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `doWriteColValue` pass-through catch | `withheldJacksonChain` | `throwRebuiltJacksonChain(e, ...)` | WIRED | line ~1083, before `throw e` |
| `writeRecordsJson` serialization statements | same rebuild | `throwRebuiltJacksonChain` in `catch (IOException \| RuntimeException)` | WIRED | stream write `writeJsonRow` stays outside |
| `withheldJacksonRoot` | `Utils.handleException` prefix check | `Utils.CLICKHOUSE_CLIENT_ERROR_*_TIMEOUT_MSG` | WIRED | constants public |
| `writeRecordsRowBinary` per record | `writeColumnWithRecordContext` | direct call | WIRED | logs five fields, no throwable |
| `fromJsonRow` | `readDescribeRows` (both clients) | `ClickHouseHelperClient` | WIRED | dead #839 reader dropped, 0-count anchor row |

### Data-Flow Trace (Level 4)

Not applicable: no rendered dynamic data. The relevant flow is failure text to Connect status, DLQ headers and logs; traced above and pinned by `WriterRecordContextDlqUnitTest` and `JsonRouteValueFreeDlqUnitTest` (sentinel absent from message, headers and captured logs).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| 10 Phase 45 and gg3 classes, Docker-free | `./gradlew --offline testUnit --tests` (10 classes) | 491 run, 491 succeeded, 0 failed; 216 `WriterFailureClassificationUnitTest` cases | PASS |
| Live slash, WRT-10 and raw-name matrix on `latest` x V2 | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew --offline test --tests '*SlashColumnRoundTripTest' '*WriterErrorPathLiveTest' '*RawNameMatrixLiveTest'` | 39 run, 39 succeeded | PASS |
| Tail gate on explicit SHAs | `verify-merge-invariants.sh 12e2cf17 HEAD` | `result=PASS`, 200 anchor rows | PASS |
| `dependencyInsight` slf4j-simple, commons-lang3, connect-json | `./gradlew -q --offline dependencyInsight` | 2.0.20, 3.21.0, 4.4.0 (test only) | PASS |
| OSV.dev, shipped coordinates (5 sampled) | `curl api.osv.dev/v1/query` | `{}` each; control 74 | PASS |

### Probe Execution

No `probe-*.sh` declared by the phase (SKIPPED). The measurement probes (E-06, E-10) were deliberately worktree-only and are removed.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| UPS-01 | 45-01, 45-06 | tag, scratch branch, one mechanical merge, checklist | SATISFIED | Truth 1 |
| UPS-02 | 45-02, 45-03, 45-04, 45-05 | #857 merged value-free, pins unchanged | SATISFIED (accepted residual WR-02) | Truths 2-4 |
| UPS-03 | 45-05 | slash column round trip live | SATISFIED | Truth 5 |
| UPS-04 | 45-02, 45-05 | `fromJsonRow` reconciled, differences pinned | SATISFIED | Truth 5 |
| UPS-05 | 45-05, 45-06 | three bumps taken, gate PASS, anchor count | SATISFIED | Truth 6 |
| DEP-01 | 45-06 | manual dependency and CVE check recorded | SATISFIED | Truth 7 |
| DEP-02 | 45-06 | approved bumps in own commits | SATISFIED (vacuous: none approved, 16 declined) | Truth 7 |

All seven IDs appear in the PLAN frontmatter (`requirements:` of plans 01-06) and in REQUIREMENTS.md; no orphaned requirement. REQUIREMENTS.md still shows them as `Pending` in the traceability table; the orchestrator updates that at phase completion.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (src and docs diff since M) | n/a | `TBD`, `FIXME`, `XXX` grep over added lines | none | no debt markers |
| `ClickHouseWriter.java` | 723-731 etc. | `throwRebuiltJacksonChain` returns normally then caller `throw e` (fail-open idiom) | Warning (WR-02a, accepted) | unreachable today: every `withheldJacksonChain` branch returns `IOException` or `RuntimeException`; a future edit could silently reopen the leak |
| `ClickHouseWriter.java` | 1082-1084 | top text formatted eagerly on the failure path (IN-02) | Info | failure path only |
| tests | n/a | three copies of helper block (WR-04), no tie between `handleException` and rebuild (WR-03) | Warning (open, advisory) | maintainability, no behavior defect |

### Human Verification Required

None open. The three items raised by the initial verification were decided by the user (see below).

### Resolution of the human items (user decisions, relayed by the coordinator)

1. **WR-02: accepted as a residual.** No production change. I re-checked the FORK-DELTA wording in `02baac3a`: the row now says Jackson links above the root are dropped so the rebuilt chain can be shorter, and that no Jackson link remains in the thrown cause chain, with the accepted residual named (the writer path builds no suppressed Jackson exception, and suppressed exceptions on the kept `IOException` suffix are not scanned). The two over-statements ("no Jackson residual remains", "no Jackson chain is left alone") no longer occur in the file (grep count 0). IN-01 is fixed by the same commit.
2. **WR-01: fixed by test-only pins** (`876a9bd1`, evidence E-19 in `a7b67d36`). Verified as truth 4 above. E-19 records `testUnit` 2086 tests, 0 failures, 2 skipped with Docker unreachable; I did not re-run the full `testUnit`, I re-ran the three relevant classes (354 green). Tail gate on final HEAD: I ran it, `provenance_paths=11 anchor_commits=42 anchor_rows=200 marker_commits=60 anchor_exempt=0 result=PASS`.
3. **DEP-02 record wording: acceptable as is** (declines recorded in spirit, not verbatim).

Still open in `45-REVIEW-DISPOSITION.md` and not must-haves: WR-03 (classification knowledge duplicated, no tying test), WR-04 (test helper duplication), IN-02, IN-03, IN-04. These are advisory maintainability items, none affects a roadmap success criterion, and no criterion depends on them.

### Gaps Summary

**WR-02: residual, not a gap against "never the value" (accepted by the user).** Reasoning from the code:

- The fail-open idiom (a) is unreachable today. `withheldJacksonChain` returns `null`, an `IOException` or a `RuntimeException` on every path, including a checked non-IO top (it falls to `new RuntimeException`). It is a maintainability hazard, not a leak.
- The suppressed-exception hole (b) needs a plain, non-Jackson `IOException` at the bottom of a pass-through chain that carries a Jackson exception in `getSuppressed()`. Nothing in the connector's writer path calls `addSuppressed` (the only call is `ProxySinkTask:106` at batch level). On the JSON route, `ObjectMapper.writeValueAsBytes` turns a serializer's plain `IOException` into a Jackson `JsonMappingException` (measured fact in E-10), which the rebuild drops with its suppressed list, so the kept-suffix case cannot arise there. On the RowBinary route the Jackson close-failure suppression (`closeOnFailAndThrowAsIOE`) is not on the stream path. The pre-merge gg3 policy already passed non-Jackson IO chains through whole, suppressed included, so Phase 45 narrowed the exposure and did not add it.
- It is not provably zero: a custom client stream or serializer that attaches a Jackson exception as suppressed onto an `IOException` would print its message in `exception.stacktrace`. FORK-DELTA's earlier "no Jackson residual remains" over-stated this; corrected in `02baac3a`.

So the criterion "no error message, DLQ header or log line of a serialization failure carries a record value" is met for every shape that was measured and every chain the code can build, with this named, low-likelihood residual. I classified it WARNING, not BLOCKER; the user accepted it as a residual and the FORK-DELTA wording is now qualified. If the guarantee is ever to be absolute, the fix is the `getSuppressed()` scan plus a helper that returns what to throw.

**Plan 45-04 amendment (`fb5fa4cb`)** is honored: the JSON-route control is the Jackson-free `NullPointerException` and the IO-timeout is recorded as a measured fact (Jackson wraps it); the guard, pins and FORK-DELTA row match the amended table.

**Other notes (no action required for status):**
- ROADMAP SC4 asks for two full suites green on `latest` x V2. They are recorded green, but on Java 21 because the default OpenJ9 17 toolchain crashed twice (known GPF). `testUnit` ran on the default toolchain. Disclosed in E-18; I re-ran the targeted Docker-free and live classes on the default toolchain and they pass.
- `STATE.md`, `config.json` and `state.json` were not staged or modified by this verification; nothing was pushed and no source was modified. The mutation check ran in a detached worktree under `/tmp` that was removed (`git worktree list` shows one entry).

---

_Verified: 2026-11-05 (re-verification)_
_Verifier: Claude (gsd-verifier)_
