---
phase: 11-full-suite-ci-publication
verified: 2026-10-07T13:05:00Z
status: passed
score: 7/12 must-haves verified
covered_files:
  - ".github/workflows/tests.yaml"
  - ".planning/phases/11-full-suite-ci-publication/11-01-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-01-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-02-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-02-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-03-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-03-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-04-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-04-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-05-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-05-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-06-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-06-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-07-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-07-SUMMARY.md"
  - ".planning/phases/11-full-suite-ci-publication/11-08-PLAN.md"
  - ".planning/phases/11-full-suite-ci-publication/11-08-SUMMARY.md"
  - ".planning/scripts/verify-merge-invariants.sh"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java"
  - "src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java"
covered_digest: "v3:sha256:24e76b5708850241a3ac693cd6b53529d441b467df857db2bfda4ed13287cb29"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "CI-06 locator judgment: is `head_sha` + `path` + `conclusion != cancelled` -> exactly one run an acceptable reading of 'asserting exactly one match'?"
    expected: "Either accept the narrowed locator (recording it as an override in this file's frontmatter), or require the contract to be restated. The plain locator `head_sha` + `path` returns TWO runs on c61d90ce (37619430314 cancelled, 37619434848 success). Exactly one run is green and no run is red or in progress, but the literal 'exactly one match' assertion did not hold and was reached by adding a filter."
    why_human: "Interpretive. The narrowing was disclosed and was not 'take the newest', but it changes the criterion's wording after the fact, and the verifier must not self-certify that."
  - test: "CI-07 approval record: confirm you gave the approval ('Fix F-1, then publish (Recommended)') to the orchestrator on 2026-10-07 before the `custom_build` push."
    expected: "You confirm the approval happened and covered the merge commit, the branch push and both tag pushes."
    why_human: "The only record is a statement in 11-06-SUMMARY.md written by the orchestrator. The dispatched executor refused to act on a relayed approval. No artifact in the repo or on origin can show that a human approved."
  - test: "SUITE-03 'smaller delta' judgment: is the delta smaller after this milestone than before it?"
    expected: "Decide whether the behaviour ledger plus the disclosed raw numbers satisfies 'shows the fork's delta over upstream is smaller'."
    why_human: "The raw numbers are mixed. Files fell 93->84 (wide scope) and 32->30 (src/main). src/main deletions fell 139->98. Insertions ROSE 8401->9476 (wide) and 1308->1368 (src/main). Wide-scope deletions ROSE 423->451. FORK-DELTA.md states this and rests the claim on the behaviour ledger. The ledger lists retired patches and surviving ones but gives no before-count of fork behaviours, and this phase ADDED one (the `network_compression_method=lz4` pin)."
  - test: "Backstop (SUITE-01/concurrency): CI executes at the same single-fork topology as the local gate."
    expected: "Confirm, or accept that the CI-green result is at an unmeasured topology. The local gate ran 6 forks (12 cores). The CI fork count was inferred from a 2-core assumption and never read from a log (unauthenticated log access returns 403). Public-repo `ubuntu-latest` runners are generally 4 vCPU, which would resolve `maxParallelForks` to 2, so the inference may be wrong."
    why_human: "Non-inferable truth with no explicit evidence. Abstains to human_needed by design."
  - test: "Backstop (SUITE-03/ordering): a future syncer diffing FORK-DELTA.md sees content changes, not reordering noise."
    expected: "Confirm, or accept as untestable from one revision. Rows are grouped by disposition under stable headings in this revision."
    why_human: "Property of future revisions. Abstains to human_needed by design."
gaps: []
deferred: []
advisory:
  - finding: "ClickHouseVersionDefaultUnitTest Javadoc still asserts 'CI pins CLICKHOUSE_VERSION: latest' (line 12) in the published tree. D-09 corrected the same sentence only in ClickHouseTestHelpers."
    category: other
    reason: "Review finding IN-01, open. Comment-only, no behavioural effect. D-09's mandatory companion edit was applied to one of two places."
    evidence_status: "git show c61d90ce:src/test/.../ClickHouseVersionDefaultUnitTest.java | grep 'CI pins'"
  - finding: "The CI concurrency group keyed on github.sha with cancel-in-progress is what produced the second run on the published SHA (review finding WR-01, open)."
    category: other
    reason: "It is the root cause of the CI-06 'exactly one match' ambiguity. It remains unfixed in the published workflow, so the next branch+tag publication at one SHA will repeat it."
    evidence_status: "REST API: runs 37619430314 (cancelled) and 37619434848 (success) on c61d90ce"
---

# Phase 11: Full Suite, CI & Publication Verification Report

**Phase Goal:** The merged connector is accepted by the suite the previous milestone was built to provide: green locally on ClickHouse `latest` x client V2, green in CI on the pushed SHA, published to the public fork by a non-force merge, with the shrunken fork delta written down.
**Verified:** 2026-10-07
**Status:** human_needed
**Re-verification:** No, initial verification

## Goal Achievement

No must-have was found FAILED. The suite is green, CI is green on the published SHA, and the publication state is as claimed. Five items cannot be settled by command and route to human review: two are interpretive readings of the criterion wording (CI-06, SUITE-03), one is an approval that exists only as an orchestrator-written record (CI-07), and two are backstop truths with no explicit evidence.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1 / SUITE-01: `./gradlew test` green on `latest` x V2, every skip accounted for, zero `InstanceAlreadyExistsException` | VERIFIED | I read the result XML on disk (`build/test-results/test`, written 12:25Z today by 11-07's `--rerun`): 59 classes, 663 tests, 0 failures, 0 errors, 18 skipped. `grep -c InstanceAlreadyExists` over all XML returns nothing. The 18 skips match the 18 named in Phase 10's `10-02-SUMMARY.md` by class and test: `clientNameTest` 1, 2 `@Disabled` `WithSchema` tests, 11 `exactlyOnceStateMismatchTest` cases (`assumeTrue(isCluster\|\|isCloud)`), `compareInsertPerformance`, `compareAllConfigurations`, 2 performance `@Disabled`. They are also tabled in FORK-DELTA.md. `CLICKHOUSE_VERSION_DEFAULT = CLICKHOUSE_VERSION_LATEST` in `ClickHouseTestHelpers`. `ClickHouseVersionDefaultUnitTest` 1/0/0/0. `git diff --stat c61d90ce HEAD -- . ':!.planning'` is empty, so this run covers the published tree. Caveats: it was a 6-fork run, not single-fork. The "upstream's 56 test files" figure in the criterion does not reproduce (FORK-DELTA measures 145 upstream-touched test files, 129 byte-identical). The intent, that upstream's tests ran in the suite, holds. |
| 2 | SC2 / SUITE-02: four fork SMTs and their sink-landing tests pass untouched | VERIFIED | `git diff --quiet pre-v1.6.0-sync c61d90ce` over `TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `HeaderToValueConfig` and `ConvertToMapOrJson` returns 0 (main). The same over the four SMT test classes returns 0. XML: `TableOperationFilterTest` 26/0/0/0, `HeadersToValueTest` 25/0/0/0, `HeaderToValueTest` 24/0/0/0, `ConvertToMapOrJsonTest` 37/0/0/0, `SmtSinkLandingTest` 5/0/0/0, performance class 2 run with its 2 `@Disabled` skipped. `SmtSinkLandingTest` is the one test not byte-identical: the diff adds a local `createTable` helper and `getBaseProps()` calls and touches no assertion line. 11-07 states this exception openly. |
| 3 | SC3 / CI-05: `tests.yaml` runs the merged suite on push, publishes coverage, runtime measured against `timeout-minutes: 45` | VERIFIED | `tests.yaml` at `c61d90ce`: `on: [push]`, `timeout-minutes: 45`, `./gradlew test`, JaCoCo summary step, `jacoco-coverage-reports` upload. REST API on run 37619434848: job conclusion success; `Run tests` step 12:13:20 to 12:19:23 (363 s); run 12:13:08 to 12:19:34 (386 s) against the 1350 s (22.5 min) threshold derived from the 45 min budget. Artifacts: `jacoco-coverage-reports` 816157 B and `gradle-test-reports` 23711100 B, not expired. Check-run annotations hold deprecation warnings only, no failure. CI's own test totals were not read: job logs return 403 unauthenticated. |
| 4 | SC3 / CI-06: a `tests.yaml` run is machine-verified green on the pushed SHA, located by `head_sha` + workflow `path`, **asserting exactly one match** | ? UNCERTAIN | Run green: confirmed by me. `actions/runs?head_sha=c61d90ce...` returns `total_count: 2`, both with path `.github/workflows/tests.yaml`: 37619430314 (branch `custom_build`, `cancelled`) and 37619434848 (tag `v1.6.0_gg1`, `success`). The check-runs API agrees: one success, one cancelled. The criterion's literal assertion (exactly one match on head_sha + path) did NOT hold. It was reached by adding `conclusion != cancelled`. The cancelled run was killed 4 s after start by the tag push through the `github.sha` concurrency group, so it is not an independent failure signal. My judgment is that the evidence is sound but the criterion was met by narrowing it. Human decision requested, per the verification brief. |
| 5 | SC4 / CI-07 (mechanics): `custom_build` brought forward by a non-force merge and pushed to the public fork | VERIFIED | `git ls-remote origin`: `custom_build` = `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`; tags `pre-v1.6.0-sync` = `6ac2d20c...` and `v1.6.0_gg1` = `c61d90ce...`; no `sync/v1.6.0` head. `git log -1 --format=%P c61d90ce` = `6ac2d20c 586af775`, a two-parent `--no-ff` merge. `git merge-base --is-ancestor a295af8a c61d90ce` is true: the old `origin/custom_build` tip is an ancestor of the published tip, so the push was a pure forward move (754 commits). A force-flag absence cannot be read from history, but the ancestry proves no history was rewritten. `custom_build:VERSION` = `v1.6.0_gg1`. No conflict marker at the start of a line in `src build.gradle.kts gradle .github VERSION` at `c61d90ce` (`git grep` clean). The invariant script run by me, `verify-merge-invariants.sh 673d29c3 c61d90ce`, exits 0. |
| 6 | SC4 / CI-07 (approval): the push followed human approval | ? UNCERTAIN | Sole evidence is `11-06-SUMMARY.md` key-decisions, which says the user answered "Fix F-1, then publish (Recommended)" directly to the orchestrator. The same summary says the dispatched executor refused to act on the relayed approval, and the orchestrator ran the merge and push inline. Nothing on origin or in the repo can show human approval. Human confirmation requested. |
| 7 | SC5 / SUITE-03 (record): a written record lists every fork change dropped in favour of upstream and why | VERIFIED | `docs/FORK-DELTA.md` exists at `c61d90ce` (167 lines), linked once each from `README.md` and `AGENTS.md`. It has "What the fork retired in favour of upstream" (8 retired rows, each with mechanism and pinning test) and "Known costs and open items" leading on R-01. I spot-checked the ledger against the published tree. The `Map(K,Nullable(V))` construct occurs once in `ClickHouseWriter`. `5.11.3`, `signing` and `buildconfig` are absent from `build.gradle.kts` and `gradle/`. No `ClickHouseAPI.java`. The one `InputStreamReader(` in `src/main` is in `ClickHouseHelperClient`. `buildMBeanName` lives in `SinkTaskStatistics`. `shouldSkipTableMetadata` has 1 definition and 5 sites. `async_insert` is commented out in `ClickHouseSinkConfig`. Minor imprecision: the FORK-DELTA row for `jsonInsertColumns` says `doInsertJson` calls the columns overload. In the code the call is in `doInsertJsonV2` (`ClickHouseWriter` line ~1393). The behaviour is right, the method name is not. |
| 8 | SC5 / SUITE-03 (claim): the document shows the fork's delta is smaller after this milestone than before | ? UNCERTAIN | Raw numbers (re-read from the published file): wide scope files 93->84 (down), insertions 8401->9476 (up), deletions 423->451 (up). `src/main`: files 32->30 (down), insertions 1308->1368 (up), deletions 139->98 (down). Three metrics fell, three rose. The document says so ("neither is offered as the headline") and rests the claim on the behaviour ledger. The ledger lists retired and surviving patches but gives no before/after behaviour count. It also records a new fork behaviour added during this phase (the LZ4 pin, D-17). The claim is honestly disclosed but is not demonstrated by a number. Human judgment requested. |
| 9 | Pre-publication debt closed (D-05/06/07/09): checklist at zero pending, invariant script fails closed, security audits for 07 and 09, CI version pins removed | VERIFIED | `MERGE-CHECKLIST.md`: 48 rows `done`, 0 `pending`. `verify-merge-invariants.sh` branches on `git grep` exit 0/1/>=2 and reports a tool failure as a violation (read the code, ran it: exit 0). `07-SECURITY.md` and `09-SECURITY.md` exist, `status: verified`, `threats_open: 0`. `tests.yaml` diff against the phase base shows the `env: CLICKHOUSE_VERSION / CLIENT_VERSION` block removed. The `ClickHouseTestHelpers` Javadoc was corrected in the same change. |
| 10 | D-17 (gap plan 11-08): the connector forces `network_compression_method=lz4` on every client it builds, pinned by unit and live tests | VERIFIED | `ClickHouseHelperClient.java` diff: V1 via `CUSTOM_SETTINGS` in `getDefaultClientOptions()`, V2 via `.serverSetting(...)` before the `jdbcConnectionProperties` loop. The `src/main` diff from phase start (`c48754be`) to `c61d90ce` touches only this file, as D-08/D-17 allow. XML: `NetworkCompressionPinUnitTest` 4/0/0/0, `NetworkCompressionPinLiveTest` 5/0/0/0. No debt markers (TBD/FIXME/XXX/TODO/HACK) in the added lines of any phase-modified file. |
| 11 | Backstop, SUITE-01/concurrency: CI runs at the same single-fork topology as the local gate | ? UNCERTAIN (insufficient_spec) | The local gate was 6 forks (`maxParallelForks = availableProcessors()/2`, 12 cores). CI's fork count was never read from a log (403). The 1-fork figure assumes a 2-core runner, and public-repo `ubuntu-latest` runners are usually 4 vCPU, so the assumption is doubtful. CI is green at an unmeasured topology. This does not weaken CI-06's green result, only the equivalence claim. |
| 12 | Backstop, SUITE-03/ordering: FORK-DELTA rows are stably ordered so a future diff shows content, not reordering | ? UNCERTAIN (insufficient_spec) | The current revision groups rows by disposition under stable headings. Stability across future revisions cannot be shown from one revision. |

**Score:** 7/12 truths verified (0 present-but-behavior-unverified). The 5 non-verified truths are UNCERTAIN, with no FAILED truth.

### Deferred Items

None. Phase 11 is the last phase of the milestone and no later phase exists to carry anything.

### Advisory (New Scope, Unevidenced)

Not a re-verification. Findings that did not change a truth's status are listed in the frontmatter `advisory` block: the stale CI-pin Javadoc in `ClickHouseVersionDefaultUnitTest` (review IN-01), and the `github.sha` concurrency group (review WR-01) that caused the CI-06 ambiguity and remains in the published workflow.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `docs/FORK-DELTA.md` | Behaviour ledger, raw numbers, known costs, skip inventory | VERIFIED | 167 lines in the published tree, linked from `README.md` and `AGENTS.md`, ledger spot-checked against the code |
| `.github/workflows/tests.yaml` | Single job, no version pins, coverage upload, 45 min budget | VERIFIED | As published and as run by CI |
| `.planning/scripts/verify-merge-invariants.sh` | Fail-closed on `git grep` error | VERIFIED | Code read, run, exit 0 |
| `.planning/MERGE-CHECKLIST.md` | Zero pending rows | VERIFIED | 48 done, 0 pending |
| `07-SECURITY.md`, `09-SECURITY.md` | Retroactive audits, 0 open at or above `high` | VERIFIED | Both `status: verified`, `threats_open: 0` |
| `ClickHouseHelperClient.java` LZ4 pin | Pin on V1 and V2 | VERIFIED | Present, tested, wired through the shared builder and default options |
| `NetworkCompressionPinUnitTest`, `NetworkCompressionPinLiveTest` | Unit and live pins | VERIFIED | 4 and 5 tests, all passing in the run on disk |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `ClickHouseTestHelpers.CLICKHOUSE_VERSION_DEFAULT` | the CI container image | the workflow sets no version, so the code default resolves | WIRED | Workflow has no `env`. The default is `latest`. The canary test passed. |
| `getDefaultClientOptions()` | every V1 request in `src/main` | shared options | WIRED | The review (WR-04) found no unpinned production site. The tests do not exercise the call sites (open review finding). |
| `custom_build` on origin | `c61d90ce` | non-force push | WIRED | `ls-remote` plus the ancestry check |
| Tag `v1.6.0_gg1` | `c61d90ce` and `VERSION` | tag push | WIRED | Tag, `VERSION` and the branch tip agree |

### Data-Flow Trace (Level 4)

Not applicable. The phase delivers evidence, a document and one connection-setting pin, with no dynamic rendering.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full-suite totals on disk | parse `build/test-results/test/*.xml` | 59 classes, 663/0/0/18 | PASS |
| Zero `InstanceAlreadyExistsException` | `grep -c` over the XML | no hits | PASS |
| Published refs | `git ls-remote origin` | `custom_build`, 2 tags as claimed | PASS |
| Published-SHA CI runs | REST `actions/runs?head_sha=...` | 2 runs (1 cancelled, 1 success) | PASS (see truth 4) |
| Invariant script | `verify-merge-invariants.sh 673d29c3 c61d90ce` | exit 0 | PASS |
| Docker-free tests | not re-run. Re-running would overwrite the on-disk full-suite XML that I used as evidence, and 11-07 ran `testUnit` earlier. | n/a | SKIP |

### Probe Execution

No `probe-*.sh` scripts are declared by the phase. Step 7c is not applicable.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SUITE-01 | 11-04, 11-07 | Full `test` green on `latest` x V2, every skip accounted for | SATISFIED | Truth 1. The "56 test files" wording does not reproduce, and the intent holds. |
| SUITE-02 | 11-04, 11-07 | Four SMTs and landing tests pass untouched | SATISFIED | Truth 2 |
| SUITE-03 | 11-03, 11-07 | Written record of dropped changes, shows a smaller delta | NEEDS HUMAN | The record exists (truth 7). The "smaller" claim is mixed on the numbers (truth 8). |
| CI-05 | 11-02, 11-05, 11-07 | CI runs the suite, publishes coverage, runtime measured vs 45 min | SATISFIED | Truth 3 |
| CI-06 | 11-05, 11-06, 11-07 | Green run on the pushed SHA, located by `head_sha` + path, exactly one match | NEEDS HUMAN | Green run confirmed. The exactly-one assertion needed a narrowing filter (truth 4). |
| CI-07 | 11-06, 11-07 | Non-force push after human approval | NEEDS HUMAN | Push confirmed (truth 5). The approval is an orchestrator record only (truth 6). |

All six IDs in the phase's PLAN frontmatter and the roadmap map to rows in `REQUIREMENTS.md`, which marks all six `[x]` and Complete in the traceability table. I found no orphaned Phase 11 requirement. `REQUIREMENTS.md` marks all six Complete while three of them remain interpretive or unconfirmed here. That reflects the plan's own closure, not this verification.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (phase-modified files) | n/a | TBD/FIXME/XXX/TODO/HACK in added lines | none found | No debt-marker blocker |
| `src/test/.../ClickHouseVersionDefaultUnitTest.java` | 12 | Javadoc asserts a CI pin that no longer exists | Warning (review IN-01, open) | Comment only. Reintroduces the stale-comment trap D-09 set out to remove. |
| `.github/workflows/tests.yaml` | 8-12 | `github.sha` concurrency with `cancel-in-progress` | Warning (review WR-01, open) | Caused the second published-SHA run. It will recur on a branch plus tag push at one SHA. |
| `docs/FORK-DELTA.md` | `jsonInsertColumns` row | Names `doInsertJson` where the call is in `doInsertJsonV2` | Info | Method name imprecise, behaviour correct |

Nine code-review findings (5 warnings, 4 info, 0 critical) are all recorded `open` in `11-REVIEW-DISPOSITION.md`. The four code-review findings (`Column.extractColumn`, the `USERNAME` default, `JsonColumnTest`, P9-IN-01) were published knowingly open under D-08 and are disclosed in FORK-DELTA.md. I raised no blocker.

### Human Verification Required

#### 1. CI-06 locator judgment

**Test:** Decide whether `head_sha` + `path` + `conclusion != cancelled` -> exactly one run satisfies "asserting exactly one match".
**Expected:** Accept it (recording an override) or require the criterion restated. The plain locator returns two runs on `c61d90ce`, and exactly one of them is green.
**Why human:** Interpretive, and the narrowing was made after the plain locator failed to return one match. My own judgment is that the evidence is sound, and I have not self-certified the criterion.

An override, if accepted, would read:

```yaml
overrides:
  - must_have: "CI-06: a tests.yaml run is machine-verified green on the pushed SHA, located by head_sha plus workflow path, asserting exactly one match"
    reason: "Plain locator returns 2 runs on c61d90ce: the branch run was cancelled by the tag push via the github.sha concurrency group. Narrowed by excluding cancelled; exactly one success remains and no failing run exists."
    accepted_by: "{name}"
    accepted_at: "{ISO timestamp}"
```

#### 2. CI-07 approval

**Test:** Confirm you approved the publication ("Fix F-1, then publish (Recommended)", 2026-10-07).
**Expected:** Confirmation that the approval covered the merge commit, the branch push and both tag pushes.
**Why human:** The only record is orchestrator-written. The executor refused a relayed approval.

#### 3. SUITE-03 "smaller delta"

**Test:** Read the "raw numbers" and ledger sections of `docs/FORK-DELTA.md` and decide whether they show a smaller delta.
**Expected:** A decision on whether the behaviour-ledger framing satisfies the requirement, given that three raw metrics fell and three rose.
**Why human:** Interpretive. No before/after behaviour count exists in the document.

#### 4. Backstop: CI topology (SUITE-01/concurrency)

**Test:** Accept or reject that CI ran at an unmeasured fork topology.
**Expected:** An explicit acceptance. Optionally check the run's job log in the web UI for the resolved fork count. A public-repo `ubuntu-latest` runner is likely 4 vCPU, so 2 forks, not 1.
**Why human:** No explicit evidence is obtainable unauthenticated.

#### 5. Backstop: FORK-DELTA ordering stability (SUITE-03/ordering)

**Test:** Accept or reject the stable-ordering property as untestable from a single revision.
**Expected:** An explicit acceptance.
**Why human:** A property of future revisions.

### Gaps Summary

There are no gaps. Every concrete claim I could test against the code, the remote or the REST API holds: the suite result on disk, the SMT files, the published refs, the merge shape and ancestry, the CI run, the artifacts and wall-clock, the debt closure and the LZ4 fix. The phase ends at `human_needed` because five items are interpretive or unconfirmable, not because anything was found missing. Two post-publication facts do not alter the verdict. The published tree's `.planning/ROADMAP.md` is a truncated 105-line file (local HEAD has the repaired 269-line version, unpushed). That is planning-only and affects no code, test or CI path. The post-publication `.planning/` bookkeeping on local `sync/v1.6.0` is unpushed by design, and `git diff c61d90ce HEAD -- . ':!.planning'` is empty.

---

_Verified: 2026-10-07_
_Verifier: Claude (gsd-verifier)_
