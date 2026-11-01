---
phase: 36-client-0-11-0-audit-bump
verified: 2026-10-31T02:30:00Z
status: passed
score: 5/5 roadmap success criteria verified (0 failed, 0 behavior-unverified); 4 judgment items routed to human review
covered_files:
  - ".planning/phases/36-client-0-11-0-audit-bump/36-01-PLAN.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-01-SUMMARY.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-02-PLAN.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-02-SUMMARY.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-03-PLAN.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-03-SUMMARY.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-04-PLAN.md"
  - ".planning/phases/36-client-0-11-0-audit-bump/36-04-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
  - "gradle/libs.versions.toml"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryClientParityLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java"
  - "src/test/resources/rowbinary-golden-sha256.txt"
covered_digest: "v4:sha256:f64ac734cb4bb3692aa4bc8d2fc3af61f079eb5a33529b7dd03907bd30363c6e"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide whether one green nightly (E-17, run 41152289179 at scratch SHA de8c685b) is enough for CLT-07, or whether a second green nightly on the final tree is wanted before Phase 38"
    expected: "Either accept E-17 as closing CLT-07, or dispatch the nightly once more on the publication SHA and record it"
    why_human: "The two earlier reds (E-15, run 40971181887) were attributed to a Datagen harness race at about 85 percent. That is a hypothesis, not a proof. CI kept no Connect-worker logs and the race rate on the runner was never measured. A single green run after the test-only fix is also what a lucky draw of a racy harness would give. Whether that residual risk is acceptable is a judgment call."
  - test: "Confirm the CLT-05 reading: the D-17 rationale is reworded in the FORK-DELTA D-17 row and eighth-behaviour paragraph, in the two pin-class Javadocs and in gg1 item 6, while FORK-DELTA has no separate CMP rows and Recorded Fact 26 itself is untouched"
    expected: "The user agrees the D-17 row (which carries the CMP-01..03 ids and cites Fact 26) is the 'CMP rows' place, and that the facts live in the milestone archive and stay as the 0.9.6 record, with the 0.11.0 measurement added beside them"
    why_human: "The roadmap wording names 'Recorded Fact 26 and the CMP rows'. The codebase has no such rows under that name, and no main-source Javadoc carries the D-17 rationale. The executor chose a reading. Whether it matches intent is a wording judgment that no test can make."
  - test: "Read 36-ROLLOUT-ITEMS.md (14 labelled items, replay section, zstd-jni section, custom_ section) as an operator would"
    expected: "Each claim is clear, correctly labelled measured / read from the code / measured in research (not re-run), and the replay statement is scoped to the compared 48-case matrix"
    why_human: "Operator comprehension and wording quality are judgment. The independent review of the release note belongs to Phase 38."
  - test: "Decide whether the full suites need one more run on the final tip, and on one JVM line, before the phase is considered closed"
    expected: "Either accept E-12 (two suites at a8b61f15, plus the JDK 17 re-run record) plus the targeted re-runs below, or run the full suite once on 43cabc3f"
    why_human: "The last full-suite gate is at a8b61f15. Later commits (harness fix 0bb894a4, WR-01 7e717d2d, WR-02 bdd2e592, pin fix 195fe556) are test-only and were not re-gated by a full suite. The recorded suite 2 first went green on JDK 21 and was re-run on JDK 17 in 36-JDK17-RERUN.md; that record says JDK 17 by build default, and no test XML records the JVM at runtime. Phase 38 already requires two green suites on a pinned latest build, so this is non-blocking."
---

# Phase 36: Client 0.11.0 Audit and Bump Verification Report

**Phase Goal:** The connector runs on ClickHouse Java client 0.11.0, and every behaviour change is recorded. Each classification, endpoint, config-key and compression behaviour that operators rely on is pinned first on 0.9.6 and then on 0.11.0. The two required `createClientV2` fixes land. An exactly-once retry across the upgrade either sends identical bytes or needs a documented clean stop. The archive's change is recorded. The bump is one catalog commit, so if the audit fails, gg6 ships on 0.9.6 by reverting it (CLT-09).
**Verified:** 2026-10-31
**Status:** human_needed
**Re-verification:** No, initial verification
**Verified tree:** `sync/v1.7.0` at `43cabc3f`. `.planning/config.json` and `.planning/state.json` are modified and unstaged; they were not read as evidence.

The phase goal is achieved in the codebase. No truth failed, no artifact is a stub, and no blocker was found. The status is `human_needed` only because four items are judgment calls that no test settles (see the frontmatter and the section below). None of them blocks Phase 37.

## Goal Achievement

### Observable Truths (ROADMAP Phase 36 success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Before the bump, pins on 0.9.6 are committed green: 159 is a `RetriableException`; 502/503 and unexpected-status `ClientException` are classified as today; the timeout-message prefixes are produced; the endpoint pin and `jdbcConnectionProperties` pin are green on 0.9.6 and were red on a bare 0.11.0 (CLT-01) | VERIFIED | Commit order read from `git log`: factory `c6c7edb4`, shape pin `9dd36588`, remaining pins `765476af`, golden `83d0b42a` all precede `a1cc3382` (fix) and `5ab3410e` (catalog), and all are ancestors of HEAD. Test names read: `ClientErrorShapePinUnitTest` (502, 503, unexpected status with and without `retryOnSocketException`, 159, 252, 60, bare `ClientException`), `ClientTimeoutPrefixPinUnitTest` (read and write prefixes). No `@Disabled` or assumption in any phase-36 test file (grep). Independent bare-bump check: in a scratch clone of HEAD with only `ClickHouseHelperClient.java` restored to its phase-start version, `ClientEndpointAndOptionsPinUnitTest` ran 4 tests and 3 failed (request path, `ssl`/`sslmode`, V1-only keys), matching E-03's predicted red set. Green-on-0.9.6 is carried by E-02/E-03 and, for the revert state, by my own rehearsal below. |
| 2 | `clickHouseDriver` is one catalog change marked `# FORK OVERRIDE (upstream 0.9.6)`, with an anchor row and a Docker-free catalog pin; `createClientV2` passes only `protocol://host:port` to `addEndpoint` and sets `ignore_unknown_config_key=true` before the property loop; both pins green on 0.11.0 (CLT-02, CLT-03) | VERIFIED | `gradle/libs.versions.toml:2`: `clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)`; the four client modules share `version.ref = "clickHouseDriver"` (lines 38-41). `git show --stat 5ab3410e` is exactly 3 paths (anchors, catalog, `ClientCatalogPinUnitTest`). `fork-anchors.txt:50` holds the mid-file anchor row (`rows=152 bad=0` from `36-evidence-check.py anchors`). `ClickHouseHelperClient.java:195-212` read: local `endpoint` of `protocol://host:port` goes to `addEndpoint`, `setDefaultDatabase(database)`, `setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true")` at line 203 before the loop at 207, so an operator's own value wins. `dependencyInsight` resolves `com.clickhouse:client-v2:0.11.0`. The src/main diff since phase start is that one file (+9/-1 per the fix commit). Re-run on the tip: `testUnit --rerun` = 1475 tests, 0 failures, 0 errors, 2 skipped (1474 recorded at E-12 plus the WR-02 pin). Live on `latest` x V2 (server 26.10.3.7, digest `739e099a...b1ce`, unchanged): `Client011BehaviourPinLiveTest` 6, `RowBinaryClientParityLiveTest` 1, `ClickHouseSinkJdbcPropertiesTest` 2, all green (9 tests, 0 failures). |
| 3 | `Client011BehaviourPinLiveTest` pins 159 as `RetriableException`, the 502/503 shapes with and without `retryOnSocketException`, `ClientException` as non-retriable, and ZSTD decoding on V2 with `setCompressionMethod=false` and `compress=true`; date/datetime live pins pass on a non-UTC JVM; the Connect-worker `integrationTest` and one manual nightly pass on 0.11.0 (CLT-04, CLT-07) | VERIFIED (with a judgment item) | The six `CLT-04`/`CLT-05` display names are present in the class and all six ran green in my live run on 0.11.0. Non-UTC date pins: E-07 records 15 executions green under four zones with a zone probe and a red negative control (carried by the record; I did not re-run the zone matrix). Local Connect-worker `integrationTest`: E-07, 55 tests, 54 succeeded, 0 failed, 1 skipped, HotSpot Temurin 17 (carried by the record). Nightly: E-15 was red (run 40971181887, 5 then 2 failures, all `stockGen*` with the Datagen pattern); the harness fix `0bb894a4` is test-only (`ConfluentPlatform.java` plus a FORK-DELTA row; `src/main` unchanged between E-12's head and it); E-17 records run 41152289179 at `de8c685b`, `workflow_dispatch`, attempt 1, conclusion `success`, 63 tests / 0 failures / 9 cloud skips, with the 7 `stockGen*` tests present and passing, user-reported and re-read with read-only `gh`. The literal criterion (one nightly passes) is met. The attribution of the earlier reds is a hypothesis; see human item 1. |
| 4 | Exactly-once safety recorded: a RowBinary byte comparison of 0.9.6 against 0.11.0 covers the type matrices including nested Tuple and Map nullable markers; a `BEFORE_PROCESSING` batch replayed across the upgrade has identical bytes or the rollout item states a clean stop (CLT-06) | VERIFIED | `src/test/resources/rowbinary-golden-sha256.txt` is the golden blessed on client-v2 0.9.6 (header line read; bless mode rewrites and then fails on purpose; a one-digit mutation turned the test red in E-04). I ran `RowBinaryClientParityLiveTest` on 0.11.0 against that golden: green (1 test, 0 failures). The run includes the review-fix commit `7e717d2d` (unicode escapes), so the result no longer depends on the compile-time encoding. E-08 adds the same-wire statement (same body, same `insert_deduplication_token`, no `Content-Encoding`), and `QueryIdentifier`, `ClickHouseWriter`, `sink/processing` and `sink/state` are unchanged since phase start (the src/main diff is one file). `36-ROLLOUT-ITEMS.md` item 11 and the replay section state `clean_stop_required: no`, scoped to the compared 48-case matrix, with a not-compared sentence. This is a behavioral check (a passing named test), not presence. |
| 5 | The D-17 `network_compression_method=lz4` default stays; its rationale is reworded to what was measured on 0.11.0 in the Javadoc, FORK-DELTA and the gg1 warning (CLT-05); Confluent archive listings before and after, with `zstd-jni` and the lost guava override recorded (CLT-08); a go/no-go is recorded (CLT-09): on go, two full `--rerun` suites green on 0.11.0; on no-go, the catalog commit reverted | VERIFIED (with judgment items) | D-17 default code is untouched: `ClickHouseHelperClient.java:116,149,198` still set lz4. FORK-DELTA line 120 (D-17 row) and line 256 (eighth-behaviour paragraph) carry the 0.11.0 measurement; `docs/ROLLOUT-v1.7.0_gg1.md:242` keeps the original sentence and appends `Correction for client 0.11.0`; the two pin-class Javadocs were reworded (comment-only diff, read). The five archive record files exist in the phase directory (7802 and 7933 inner entries, empty allow-list diff) and E-09 records the `zstd-jni` 1.5.7-23 cloud addition with three natives and the guava override as redundant, not lost. Decision: E-12 `go` (re-taken as `go` in E-16 after the red nightly, the user's decision). Two full suites: E-12 records 2072 tests each, 0 failures, 18 named skips, the first on JDK 17 and the second on JDK 21 after two JDK 17 crashes (OpenJ9 Mockito fault, no failing testcase); `36-JDK17-RERUN.md` records the second suite re-run green on JDK 17 (2072 / 0 / 0 / 18). Not re-run by me (full suites take far longer than a spot-check; see human item 4). The no-go branch was not taken; its mechanics are verified by the revert rehearsal below. The wording reading (no CMP rows, Fact 26 untouched, Javadoc in test classes) is human item 2. |

**Score:** 5/5 roadmap success criteria verified; 0 behavior-unverified; 4 human items (none is a FAILED truth).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | one marked override line | VERIFIED | Line 2; 4 modules on the ref |
| `ClickHouseHelperClient.java` `createClientV2` | endpoint without path, ignore flag before loop | VERIFIED | Lines 195-212 |
| `ClientCatalogPinUnitTest` | Docker-free catalog pin | VERIFIED | 2 tests in `testUnit` (1475 total) |
| `ClientErrorShapePinUnitTest`, `ClientTimeoutPrefixPinUnitTest`, `ClientEndpointAndOptionsPinUnitTest`, `ClientInsertWirePinUnitTest` | Docker-free pins | VERIFIED | 7, 2, 4, 2 green on 0.11.0 inside the green `testUnit`; all but the catalog pin green on 0.9.6 in my rehearsal |
| `Client011BehaviourPinLiveTest` | 6 live pins | VERIFIED | 6 green on `latest` x V2 |
| `RowBinaryClientParityLiveTest` + `rowbinary-golden-sha256.txt` | byte parity | VERIFIED | green on 0.11.0 |
| `ServerExceptions` factory | 10 sites compile on both lines | VERIFIED | compiles and runs on 0.11.0 (HEAD) and on 0.9.6 (rehearsal) |
| `fork-anchors.txt` rows, FORK-DELTA row | anchors for the fixes and the catalog line | VERIFIED | `rows=152 bad=0` |
| `36-ARCHIVE-*.txt` (5 files), `36-ROLLOUT-ITEMS.md`, `36-COMMAND-EVIDENCE.md` | records | VERIFIED | Files present; content read |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseHelperClient.createClientV2` | `Client.Builder.addEndpoint` | `endpoint` local, no path | WIRED (3 endpoint pins fail when the old code is restored) |
| `createClientV2` | `jdbcConnectionProperties` loop | `setOption(IGNORE_UNKNOWN_CONFIG_KEY)` before the loop | WIRED (`operatorIgnoreFlagWins` pins the order; A-05 mutation in `36-SECURITY.md` turned exactly it red) |
| catalog line | all four client modules | `version.ref = "clickHouseDriver"` | WIRED (`dependencyInsight` and `ClientCatalogPinUnitTest`) |
| golden file | `RowBinaryClientParityLiveTest` | resource read plus bless switch | WIRED (test green on 0.11.0) |
| Phase 36 pins | `Utils.handleException` | real V2 client against a loopback stub | WIRED (shape rows classified through the connector's own handler) |

### Data-Flow Trace (Level 4)

Error shapes: stub socket response, V2 client, thrown exception, `Utils.handleException`, assertion on type. Wire pin: insert body and `insert_deduplication_token` captured at the stub. Golden: writer output bytes hashed per case against a blessed list. Each flow ends in a real client or writer call, not a static value.

### Behavioral Spot-Checks and Probe Execution

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite on the tip | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` (build results cleared first) | 1475 tests, 0 failures, 0 errors, 2 skipped | PASS |
| Live pins on `latest` x V2 | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun --tests '*Client011BehaviourPinLiveTest' --tests '*RowBinaryClientParityLiveTest' --tests '*ClickHouseSinkJdbcPropertiesTest'` | 9 tests, 0 failures, 0 skipped; server 26.10.3.7, digest `sha256:739e099a...b1ce` | PASS |
| Bare-bump red set | scratch clone of HEAD, `ClickHouseHelperClient.java` restored to the phase-start version, `*ClientEndpointAndOptionsPin*` | 4 tests, 3 failed (the predicted three) | PASS |
| CLT-09 revert (documentation commit `375c93be`, then catalog commit `5ab3410e`) on the tip in a scratch clone | `git revert` twice, then the Docker-free pin classes, `UtilsTest`, `AdditionalRetriableErrorCodes*` | both reverts clean (catalog revert = 3 paths), catalog reads `0.9.6`, anchors `rows=151 bad=0`, 71 tests, 0 failures | PASS |
| Anchors mirror | `36-evidence-check.py anchors .planning/scripts/fork-anchors.txt` | `rows=152 bad=0` | PASS |
| Tail merge gate | `PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f79... verify-merge-invariants.sh adaf8c4e HEAD` | `anchor_rows=152 anchor_exempt=0 result=PASS`, exit 0 | PASS |
| Origin untouched | `git ls-remote origin refs/heads/custom_build` | `3a3a15a9ae064ffe09c696b8bb87bce23419d463` | PASS |
| Debt markers in phase-changed files | grep of added lines for TBD, FIXME, XXX, TODO, HACK | 0 matches | PASS |
| Two full suites, the nightly, the zone matrix, the Connect-worker `integrationTest` | not re-run here | carried by E-07, E-12, `36-JDK17-RERUN.md`, E-17 | SKIPPED (long, user-gated or already recorded) |
| Cumulative merge gate | not re-run here | E-12 records `result=PASS`, E-16 records it again | SKIPPED (long) |

The scratch clones lived under `/tmp/gsd36/verify/` and were not committed.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CLT-01 | 36-01, 36-04 | pins on 0.9.6 for 159, 502/503, unexpected status, timeout prefixes | SATISFIED | Truth 1 |
| CLT-02 | 36-02, 36-03, 36-04 | one marked catalog change, anchor row, catalog pin | SATISFIED | Truth 2 |
| CLT-03 | 36-01, 36-02, 36-04 | endpoint and ignore-flag fixes, both pinned live | SATISFIED | Truth 2 |
| CLT-04 | 36-01, 36-02, 36-04 | `Client011BehaviourPinLiveTest` | SATISFIED | Truth 3 |
| CLT-05 | 36-03, 36-04 | D-17 stays, rationale reworded in three places | SATISFIED, wording reading flagged | Truth 5, human item 2 |
| CLT-06 | 36-01, 36-03, 36-04 | RowBinary byte comparison and replay statement | SATISFIED | Truth 4 |
| CLT-07 | 36-02, 36-04 | non-UTC pins, Connect-worker `integrationTest`, one manual nightly | SATISFIED, attribution flagged | Truth 3, human item 1 (REQUIREMENTS.md already marks it complete) |
| CLT-08 | 36-03, 36-04 | archive listings, `zstd-jni`, guava recorded | SATISFIED | Truth 5 |
| CLT-09 | 36-04 | go/no-go, revert path | SATISFIED (go; revert path rehearsed twice and re-run by me) | Truth 5 |

No orphaned requirements: REQUIREMENTS.md maps CLT-01..CLT-09 to Phase 36 and the four plans claim all nine.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ClickHouseHelperClient.java` | 170-183 | `createClientV2` still logs a "ClickHouse URL" the client no longer receives, and writes `jdbcConnectionProperties` verbatim at INFO (review IN-01) | Info | The log line predates the phase (inherited from upstream) and was not added by it; the verbatim property string is a residual accepted as AR-36-01 in `36-SECURITY.md`. Not a blocker. |
| phase test files | various | review IN-02..IN-06 (weak `ssl=true` assertion, stale comment, two-line `clientVersion()` assumption, hardcoded `http://`, strict-inequality wait loop) | Info | Test hygiene only; all open, none maps to a goal truth. |

No TBD, FIXME or XXX in any file the phase changed outside `.planning`. Review WR-01 and WR-02 are fixed (`7e717d2d`, `bdd2e592`), and the threat found by the security audit (T-36-17) is closed by `195fe556` with E-18; I re-ran the underlying claim (the revert state is green on 0.9.6) and it holds.

### Human Verification Required

1. **CLT-07 nightly stability.** Test: decide whether E-17 (one green run) is enough, or dispatch one more nightly on the publication SHA. Expected: a recorded decision. Why human: the earlier two reds were attributed to a Datagen race at about 85 percent (a hypothesis); a second green run would raise confidence and one red would reopen the question of attribution to client 0.11.0.
2. **CLT-05 reading.** Test: confirm the D-17 row and eighth-behaviour paragraph satisfy "Recorded Fact 26 and the CMP rows", and that no main-source Javadoc was expected to carry the rationale. Expected: user agreement or a request for an extra edit. Why human: wording interpretation of the roadmap text.
3. **Rollout items wording.** Test: read `36-ROLLOUT-ITEMS.md` as an operator. Expected: clear, correctly labelled claims. Why human: comprehension and tone; the independent release review is Phase 38's.
4. **Full-suite coverage of the tip.** Test: accept E-12 plus the targeted re-runs here, or run one full suite on `43cabc3f`. Expected: a recorded choice. Why human: the last full-suite gate predates four test-only commits and the two suites were split across JDK 17 and JDK 21 before the JDK 17 re-run record; it is non-blocking because Phase 38 gates two suites on a pinned `latest`.

## Gaps Summary

No gaps. Every must-have truth is backed by code and by a re-run on the current tip: the two `createClientV2` fixes are in the source, the catalog bump is one marked three-path commit, the pins are green on 0.11.0 and were red on a bare 0.11.0 where the plan said they would be, the 0.9.6 RowBinary golden holds on 0.11.0, and the CLT-09 revert path applies cleanly and leaves a green 0.9.6 pin set.

Placeholders and open items the reader should weigh, none of which fails a truth:
- E-13 reads `pending (user-gated)` and E-14 reads `unmeasured`. E-13 is the record of the first handoff and is resolved by E-17; E-14 exists only to keep the evidence index contiguous because the no-go branch was not taken. Both follow from the evidence rule that entries are never edited.
- CLT-07 is green only at scratch SHA `de8c685b`; the final tip differs by later test-only and documentation commits.
- The review Info findings IN-01..IN-06 are open by scope decision; `36-FOLLOWUPS.md` F-1 (`ClickHouseWriter.stop()` does not close its helper client), F-2 (the 4-minute insert timeout does not bound a V2 insert with `async=false`) and F-3 (Datagen completion counting outside `generateData`) are open and observed on both client lines, not 0.11.0 regressions.
- ROADMAP.md still shows the four Phase 36 plans unchecked and REQUIREMENTS.md still shows CLT-01..06, 08 and 09 as Pending. That is bookkeeping for the orchestrator after this verification, not a code gap.
- Two scratch branches (`scratch/clt-07-client-0.11.0` at `de8c685b`, `scratch/clt-07-prebump-0.9.6` at `939e8992`) exist on origin from the user's own CLT-07 pushes; origin `custom_build` is unchanged. Publication and scratch-branch deletion are user-gated Phase 38 items.
- The CLT-09 revert is two commits in practice (documentation commit `375c93be`, then catalog commit `5ab3410e`), because the plan kept the D-17 rewording in its own commit; the catalog commit itself is one three-path change as the roadmap requires. Both reverts were applied cleanly in my rehearsal.

---

_Verified: 2026-10-31_
_Verifier: Claude (gsd-verifier)_

## Human-judgment disposition (2026-10-31)

The verifier returned `human_needed` for four judgment items. The user accepted them as listed and asked to close the phase (AskUserQuestion, option "Принять и закрыть"). The frontmatter status was changed from `human_needed` to `passed` on that basis only; no check result was altered.

1. CLT-07: accepted on the single green nightly at `de8c685b` (E-17). Phase 38 gates two full suites on a pinned `latest` at the publication SHA.
2. CLT-05: accepted as read (D-17 default unchanged; rationale reworded in the FORK-DELTA D-17 row and paragraph, the two pin-class Javadocs and gg1 item 6; FORK-DELTA has no CMP rows; Recorded Fact 26 untouched).
3. Rollout-items wording: the operator-comprehension read is carried to Phase 38.
4. Full-suite coverage of the tip: later test-only commits (`0bb894a4`, `7e717d2d`, `bdd2e592`, `195fe556`) were not re-gated by a full suite; carried to the Phase 38 gate.
