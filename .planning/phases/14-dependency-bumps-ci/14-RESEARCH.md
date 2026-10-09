# Phase 14: Dependency Bumps & CI - Research

**Researched:** 2026-10-09
**Domain:** Gradle version-catalog bumps (Java/Maven coordinates) on a Kafka Connect sink, plus a GitHub Actions workflow fix
**Confidence:** HIGH on versions, advisories, classpath facts and workflow semantics; MEDIUM on the live LZ4 behaviour (Docker was stopped during research, so no live run was possible)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Carried forward (locked earlier; not re-opened)**
- The bump set and versions were user-approved on 2026-10-07 (REQUIREMENTS locked decisions). They are applied as **plain version edits** in `gradle/libs.versions.toml`, never as dependabot cherry-picks.
- ClickHouse client stays `0.9.6`. `kafka = "4.1.1"` stays. `0.10.0` is out of scope.
- No tag is pushed in Phase 14. Phase 15 proves CI-08 end to end on the published SHA.
- Main-code changes outside a phase's declared scope stop for a user decision. Existing tests are never deleted or weakened. Type coverage asserts typed SQL read-back, never `countRows`.
- Always invoke Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Run `spotlessApply` only on touched files.
- `verify-merge-invariants.sh` runs at the end of the phase (Phase 12 D-09). Any commit that changes a `fork-anchors.txt` anchor updates the file in the same commit (Phase 12 D-08). Note that `tests.yaml	timeout-minutes: 45	1` is an anchor.

**lz4-java (BLD-10)**
- **D-01: The "live LZ4 round-trip" is a committed live test using a test-only client-v2.**
  - The client is built with `compressClientRequest(true)` and `compressServerResponse(true)` (LZ4).
  - It inserts rows into ClickHouse `latest` and reads them back with typed SQL `assertEquals`.
  - Rationale: the connector's own clients never run lz4-java. Since Phase 12 they set `compress=false`/`compressServerResponse(false)`, and `network_compression_method=lz4` is a server setting. Only an explicitly compressing client exercises the jar in both directions.
  - Connector defaults and main code are untouched.
  - The test stays committed so it guards the next lz4 or client bump.
- **D-02: Test first, then bump.**
  - Commit 1 adds the round-trip test, green on the current `1.10.4`.
  - Commit 2 is a pure `lz4Java` version edit.
  - A failure in commit 2 is then attributable to the version alone.
- **D-03: Fallback ladder.** If `1.12.0` fails with client `0.9.6` (compile, Docker-free `testUnit`, or the D-01 live round-trip):
  - Try the newest advisory-free `1.11.x` (≥ 1.11.2).
  - If that also fails, **stop at a blocking checkpoint**.
  - Record which version failed, at which gate, and the error text.
- **D-04: Evidence for "advisory-free" is a recorded lookup.**
  - Research checks GitHub Advisory DB / OSV for `at.yawk.lz4:lz4-java` and `org.lz4:lz4-java`, including the known lz4-java CVEs that motivated "≥ 1.11.2".
  - It records the source, the date and the result in the phase artifacts.
  - `dependencyInsight` must show exactly one `at.yawk.lz4:lz4-java` on `runtimeClasspath`, at the chosen version. Show `clickhouseDependencies` too, as BLD-09 did.

**Bump breakage policy (BLD-11, BLD-12)**
- **D-05: Any bump that turns an existing test red stops at a blocking checkpoint.**
  - The checkpoint shows the failing test, the diagnosed cause and the options: adapt the test, hold the bump back, or change main code. The user picks.
  - No fix-forward, even for test-only causes such as mockito strict stubbing.
  - **Reversibility:** reversible. This is process only.
- **D-06: The org.json bump is gated by the full live suite on `latest` × V2**, not just `testUnit` plus compile.
  - Compare the `Int256`/`UInt256`/`Decimal256` results before and after. org.json 20260719 added a configurable BigInteger/Decimal length limit with an unknown default.
  - If a result changes, bisect by reverting `orgJson` alone (roadmap note), then go to D-05.
- **D-07: The Jackson `2.22.3` commit is gated by the full live suite on `latest` × V2.**
  - Jackson sits under the R-01 metadata-free JSON route, `StructToJsonMap`, `DataJson` and the schemaless path. Only a full run covers every consumer, and it includes Phase 13's R-01 parity matrix.
  - Jackson gets its own commit (BLD-11).
- **D-08: okhttp and mockito use the roadmap's minimum gate:** `compileIntegrationTestJava` plus Docker-free `testUnit`, one commit each.
- **D-09: The phase gate is one full live-suite run on the phase's final commit.**
  - Pass criteria: 0 failures, 0 errors, and a skip set equal to the 18 named skips.
  - Also required: Docker-free `testUnit` and `verify-merge-invariants.sh`.
  - Phase 15 still runs its own two runs (`--rerun`).

**CI-08: trigger and concurrency**
- **D-10: `tests.yaml` triggers on branch pushes only** (`on: push: branches: ['**']`), so a tag push never starts a run.
  - The SHA-keyed concurrency group and `cancel-in-progress: true` stay.
  - Release workflows are already `workflow_dispatch`, so nothing depends on tag-triggered tests.
  - Residual risk, recorded rather than fixed: two *branches* pushed at the same SHA still share a group and cancel each other. Phase 15 must push the published SHA on one branch only.
  - The fix is committed before the D-13 push and before any tag reaches `origin`.
  - **Reversibility:** costly. Phase 15's "exactly one run" proof and every later publication depend on it, and tag pushes no longer run tests.
- **D-11: CI-08 evidence in this phase has two parts:**
  - (a) A written note in the phase artifacts explaining why a tag push cannot create a `tests.yaml` run under the new trigger filter.
  - (b) The fixed workflow observed running exactly once on the pushed scratch-branch SHA (D-13), located by `head_sha` plus workflow path, with no cancelled sibling.
  - Phase 15's published-SHA check remains the end-to-end proof.

**CI-09 / CI-10 step shape**
- **D-12: Use separate, named steps in `tests.yaml`, with no `build.gradle.kts` change.**
  - A step prints `nproc` and the derived `maxParallelForks` (`nproc/2`, minimum 1, mirroring `build.gradle.kts`) to the job log and to `$GITHUB_STEP_SUMMARY`.
  - A dedicated `./gradlew compileIntegrationTestJava` step runs before `Run tests`, so a failure names its step.
  - CI-09 is satisfied by reading the value from that run's log or summary.

**Pushing `sync/v1.7.0` (amends Phase 12 D-01)**
- **D-13: Push `sync/v1.7.0` to `origin` once, behind a blocking human-approval checkpoint.**
  - Preconditions: all bump and CI commits have landed and the D-09 local gates are green.
  - The executor never pushes on its own initiative.
  - Push the branch only: no tag, and `custom_build` untouched.
  - Record this as an amendment to Phase 12 D-01, which kept the branch local through Phases 12–14.
  - Before pushing, confirm that `.planning/.gsd-ws-arg` is not tracked and that no local-only tag rides along.
  - **Reversibility:** one-way. It publishes the Phase 12–14 code and planning commits to the public fork. Deleting the remote branch afterwards does not unpublish them.
- **D-14: If the pushed run fails while local gates were green, diagnose and stop.**
  - Read the job log through the unauthenticated GitHub REST API (`gh` is not installed, by design).
  - Classify the failure as environment/flake or a real defect.
  - Stop at a checkpoint with the diagnosis.
  - Any fix is a new commit plus another push, both approved by the user.

### Claude's Discretion
- The **order of the bump commits**, subject to:
  - D-02, so the lz4 test lands before the lz4 bump.
  - The CI-08 fix being committed before the D-13 push.
  - Live-gated bumps (Jackson, org.json) being placed so each gets its own full run.
  - Possibly putting the CI-10 compile step first, so CI checks okhttp from the start.
- Whether `jacksonAnnotations = "2.22"` needs to change. Jackson annotations use major.minor versioning; research confirms whether `2.22` already pairs with `2.22.3`.
- Whether to drop the dead `pull_request` branch of the concurrency-group expression. It is harmless and keeps the diff close to upstream; leaving it alone is fine.
- The D-01 test's class name, location, column types and row count. It should sit beside `NetworkCompressionPinLiveTest` on the shared container, tagged `clickhouse`.
- Where Phase 14 records its evidence: the advisory lookup, `dependencyInsight` output, before/after Int256/Decimal256 comparison, the CI run URL and the fork count. Phase 15 turns these into CHANGELOG/FORK-DELTA prose (the lz4 version row in FORK-DELTA §BLD-09 and the CHANGELOG lz4 line).

### Deferred Ideas (OUT OF SCOPE)
- Guarding against two *branches* pushed at the same SHA (the residual CI-08 risk under D-10) is not fixed here. It is recorded as an operational rule for Phase 15: push the published SHA on one branch only.
- Client `0.10.0`: unchanged, still a future requirement.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BLD-10 | `lz4-java` raised past `1.10.4` to an advisory-free release, after showing client `0.9.6` works (compile, `testUnit`, live LZ4 round-trip) | The advisory-free floor is **1.11.4**, not 1.11.2 (see Finding F1). 1.12.0 is clean. The client's bytecode uses only `LZ4Compressor`/`LZ4FastDecompressor`, both still present in 1.12.0. The test classpath carries a second, vulnerable `org.lz4:lz4-java:1.8.0` (Finding F2), so the D-01 test must pin which jar it exercises. |
| BLD-11 | Jackson `2.22.3` in its own commit, JSON-route tests green | 2.22.3 exists on Central and fixes CVE-2026-91776 and CVE-2026-91777, both open against 2.22.2. `jacksonAnnotations = "2.22"` already pairs with 2.22.3 (BOM). Jackson ships in the Confluent archive. Full live run is the gate (D-07). |
| BLD-12 | okhttp `5.5.0`, org.json `20260814`, mockito `5.24.0` as plain version edits, one commit each, each gated by `compileIntegrationTestJava` + `testUnit` | All three versions exist on Central, none has an OSV record. okhttp is used only in `src/integrationTest/.../ConfluentPlatform.java`. org.json is used only in tests, testFixtures and integrationTest and is not shipped. Mockito is used by two Docker-free test classes. org.json's new number-length default is 1000 (Finding F6). |
| CI-08 | A published SHA gets exactly one completed `tests.yaml` run | Confirmed incident on `c61d90ce` (one `cancelled` branch run and one `success` tag run on the same SHA). GitHub docs confirm that a `branches`-only `push` filter does not run on tag pushes. |
| CI-09 | CI's test fork count read from a job log or summary, or recorded as unmeasured | `maxParallelForks = availableProcessors/2` (min 1; 1 if `CLICKHOUSE_CLUSTER_NAME` set). The repo is public, so `ubuntu-latest` is 4 vCPU and the expected value is 2. The job log and `$GITHUB_STEP_SUMMARY` are **not readable through the unauthenticated REST API** (Finding F3). Add a `::notice::` annotation, which is. |
| CI-10 | CI compiles the integration-test source set | `./gradlew test` does not compile `integrationTest`. `compileIntegrationTestJava` needs no Docker and no network beyond dependency resolution (ran locally: 3.7 s up to date). |
</phase_requirements>

## Summary

The phase is five one-line catalog edits plus a workflow fix. The risk is in the evidence, not the edits. Research found four facts that change or sharpen the CONTEXT decisions.

First, **"advisory-free ≥ 1.11.2" is stale.** OSV and GitHub advisories published on 2026-10-07 (two days before this research) show `at.yawk.lz4:lz4-java` 1.11.2 and 1.11.3 still affected by three advisories fixed in 1.11.4. The first advisory-free release is **1.11.4**, and 1.12.0 is also clean. The D-03 ladder is still correct as written ("newest advisory-free 1.11.x" is 1.11.4), but the REQUIREMENTS and ROADMAP phrase "≥ 1.11.2" is wrong and must not be copied into evidence prose. Second, **`testRuntimeClasspath` carries both `at.yawk.lz4:lz4-java:1.10.4` and a vulnerable `org.lz4:lz4-java:1.8.0`** (Confluent `kafka-clients 7.9.1-ccs`), both providing `net.jpountz.lz4.*`. `runtimeClasspath`, `clickhouseDependencies` and `integrationTestRuntimeClasspath` are clean. A live round-trip test running on the test classpath could silently exercise the wrong jar unless it asserts where `LZ4Factory` was loaded from. Third, **GitHub job logs are not readable unauthenticated** (HTTP 403 "Must have admin rights"), and the step summary has no API at all. D-12 and D-14 both lean on log reading. The unauthenticated channels that do work are the `/jobs` step list and the check-run **annotations** endpoint. Fourth, **ClickHouse `latest` (26.9) answers `compress=1` with ZSTD** unless `network_compression_method=lz4` is set, and client `0.9.6` only decodes LZ4. The D-01 client must set that server setting or the test fails with the magic-byte error, for a reason unrelated to the lz4-java version.

Everything else is routine. Jackson 2.22.3 closes two CVEs open against 2.22.2. The BOM already pins annotations at `2.22`. org.json's default max number length is 1000 digits against a 78-digit maximum for Int256. okhttp 5.5.0 and mockito 5.24.0 contain nothing that touches the APIs this repo uses.

**Primary recommendation:** Order the commits CI fix → lz4 round-trip test → lz4 `1.12.0` → okhttp → mockito → Jackson (full live run) → org.json (full live run, which doubles as the D-09 phase gate). Make the D-01 test assert the origin jar of `LZ4Factory`, set `network_compression_method=lz4`, and add a `::notice::` fork-count annotation so CI-09 is machine-checkable.

## Architectural Responsibility Map

This phase has no browser, API or database tiers. The tiers that matter are build, CI and test.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version selection for lz4, Jackson, okhttp, org.json, mockito | Build (`gradle/libs.versions.toml`) | — | The catalog is the single source. `build.gradle.kts` only references aliases. |
| Single-lz4 resolution | Build (`build.gradle.kts` capability rule, lines 60-71 region) | — | Group-based rule selects `at.yawk.lz4`. It is correct on three of four classpaths (Finding F2). No change is in scope. |
| Proof that client `0.9.6` works with the new lz4 | Test (live, `src/test`, tag `clickhouse`) | Build (`dependencyInsight`) | Only an explicitly compressing client runs lz4-java. Connector clients set `compress=false`. |
| Compile integration sources | CI (`tests.yaml` step) | Build (`compileIntegrationTestJava`) | `test` does not depend on `integrationTest`, so okhttp is otherwise unchecked in CI. |
| Fork count measurement | CI (`tests.yaml` step) | Build (`maxParallelForks`) | Measured on the runner, derived by the same formula as the build. |
| One run per published SHA | CI (`on:` trigger + `concurrency`) | — | Trigger filter removes the tag event. The SHA-keyed group remains as a second guard. |
| Publishing the branch | Operator (human checkpoint) | Git | One-way. Executor never pushes by itself (D-13). |

## Standard Stack

### Core (the bump set, all user-approved on 2026-10-07)

| Library | Current | Target | Purpose | Verified |
|---------|---------|--------|---------|----------|
| `at.yawk.lz4:lz4-java` (`lz4Java`) | 1.10.4 | **1.12.0** (fallback **1.11.4**) | LZ4 for client-v2 native compression, shipped in the Confluent archive | Central `<release>1.12.0</release>`, lastUpdated 20260925194048 [VERIFIED: repo1.maven.org/maven2/at/yawk/lz4/lz4-java/maven-metadata.xml, fetched 2026-10-09]. Versions listed: 1.10.3, 1.10.4, 1.11.0, 1.11.1, 1.11.2, 1.11.3, 1.11.4, 1.12.0 |
| `com.fasterxml.jackson.core:*` (`jackson`) | 2.22.2 | **2.22.3** | JSON route, `StructToJsonMap`, `DataJson`; shipped in the Confluent archive | Central `<release>2.22.3</release>`, lastUpdated 20260921210639 [VERIFIED: repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/maven-metadata.xml] |
| `com.fasterxml.jackson.core:jackson-annotations` (`jacksonAnnotations`) | 2.22 | **2.22 (no change)** | Annotations use major.minor versioning | The `jackson-bom-2.22.3.pom` declares `<jackson.version>2.22.3</jackson.version>` and `<jackson.version.annotations>2.22</jackson.version.annotations>` [VERIFIED: repo1.maven.org/maven2/com/fasterxml/jackson/jackson-bom/2.22.3/jackson-bom-2.22.3.pom lines 51 and 62] |
| `com.squareup.okhttp3:okhttp` (`okhttp`) | 5.4.0 | **5.5.0** | Integration-test HTTP calls to Connect REST / Schema Registry | Central `<release>5.5.0</release>`, lastUpdated 20260816161727 [VERIFIED: maven-metadata.xml] |
| `org.json:json` (`orgJson`) | 20260522 | **20260814** | Test read-back parsing (`getAllRowsAsJson`) | Central `<release>20260814</release>` [VERIFIED: maven-metadata.xml] |
| `org.mockito:mockito-junit-jupiter` (`mockito`) | 5.23.0 | **5.24.0** | Two Docker-free test classes | Central `<release>5.24.0</release>`, lastUpdated 20260923140811 [VERIFIED: maven-metadata.xml] |

`kafka = "4.1.1"` and `clickHouseDriver = "0.9.6"` stay. Nothing else is bumped. AGENTS.md says not to upgrade dependencies without asking; these five are the user-approved scope, and this research does not propose others.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| lz4 `1.12.0` | `1.11.4` | 1.11.4 is the security release, 1.12.0 adds "input validation tightening that could in theory be breaking". The upstream maintainer says: upgrade to 1.12.0, "1.11.4 is an option if that breaks" [CITED: github.com/yawkat/lz4-java/releases v1.12.0]. This is exactly the D-03 ladder. |
| Branch-only trigger (D-10) | Key the concurrency group on `github.ref` | Rejected by the user. With `github.ref` a branch and a tag on one SHA would both run to completion, which is two runs and breaks "exactly one". |

**Installation:** no install. Each bump is one line in `gradle/libs.versions.toml`:
```toml
lz4Java = "1.12.0"      # was "1.10.4"
jackson = "2.22.3"      # was "2.22.2"
okhttp = "5.5.0"        # was "5.4.0"
orgJson = "20260814"    # was "20260522"
mockito = "5.24.0"      # was "5.23.0"
```

**Version verification:** every target was checked against `maven-metadata.xml` on Maven Central on 2026-10-09 (table above).

## Package Legitimacy Audit

The five artifacts are existing dependencies of this repo being version-bumped, not new packages. The legitimacy seam only accepts `npm|pypi|crates`; given a Maven coordinate it returned `SLOP / does-not-exist` for `at.yawk.lz4:lz4-java`, which is an ecosystem mismatch and not a finding (the artifact is plainly on Central and in the build today). That output is **ignored**, and legitimacy is established by Central metadata, the upstream release pages and OSV.

| Package | Registry | Age | Source Repo | Verdict | Disposition |
|---------|----------|-----|-------------|---------|-------------|
| `at.yawk.lz4:lz4-java` | Maven Central | Fork line since 1.8.1; already resolved by client-v2 `0.9.6` and by upstream v1.7.0 | github.com/yawkat/lz4-java | Established (already in the build) | Approved |
| `com.fasterxml.jackson.core:*` | Maven Central | Years | github.com/FasterXML | Established | Approved |
| `com.squareup.okhttp3:okhttp` | Maven Central | Years | github.com/square/okhttp | Established | Approved |
| `org.json:json` | Maven Central | Years | github.com/stleary/JSON-java | Established | Approved |
| `org.mockito:mockito-junit-jupiter` | Maven Central | Years | github.com/mockito/mockito | Established | Approved |

**Packages removed due to SLOP verdict:** none (the one SLOP output is a seam limitation, documented above).
**Packages flagged as suspicious:** none. No `checkpoint:human-verify` install gate is needed because no new coordinate is introduced.

okhttp 5.5.0 notes "artifacts are now signed with our new signing key" [CITED: github.com/square/okhttp CHANGELOG.md 5.5.0]. There is no `gradle/verification-metadata.xml` in this repo [VERIFIED: `ls gradle` shows only `libs.versions.toml` and `wrapper`], so Gradle does not check signatures and this is not a build risk.

## Advisory Lookup (D-04 evidence, recorded 2026-10-09)

Source: OSV API (`https://api.osv.dev/v1/query`, Maven ecosystem, per-version queries), which aggregates the GitHub Advisory Database. [VERIFIED: api.osv.dev query results, 2026-10-09 ~12:58 UTC]

| Version | Open advisories (id, CVE, fixed in) |
|---------|--------------------------------------|
| `at.yawk.lz4:lz4-java` **1.10.4** (current) | 6: GHSA-343h-94h5-c4wr (CVE-2026-106449, fixed 1.11.4); GHSA-4v53-57pg-c464 (CVE-2026-106452, fixed 1.11.2); GHSA-6cx8-rjf8-pr8g (CVE-2026-106453, fixed 1.11.2); GHSA-gm45-99xc-r7wv (CVE-2026-106450, fixed 1.11.4); GHSA-mcr4-qmvw-px4g (CVE-2026-106451, fixed 1.11.4); GHSA-xx22-p4ch-683r (CVE-2026-59949, fixed 1.11.1) |
| **1.11.2** | 3: GHSA-343h-94h5-c4wr, GHSA-gm45-99xc-r7wv, GHSA-mcr4-qmvw-px4g (all fixed in 1.11.4) |
| **1.11.3** | the same 3 |
| **1.11.4** | none (`{}`) |
| **1.12.0** | none (`{}`) |
| `org.lz4:lz4-java` 1.8.0 (the old coordinate, on the test classpath) | affected by GHSA-343h-94h5-c4wr (and the same family); `last_affected 1.8.1`, no fix exists on that coordinate |

What the three 1.11.4 advisories are: `LZ4BlockInputStream` with `stopOnEmptyBlock=false` recursing per empty block (StackOverflowError), `LZ4FrameInputStream` reallocating block buffers per frame (CPU/memory), and native-library extraction into a shared temp directory (race). All three concern stream classes or native-library loading. The 1.11.4 release is "Security release for CVE-2026-106450, CVE-2026-106449 and CVE-2026-106451" [CITED: github.com/yawkat/lz4-java/releases v1.11.4].

Other bumped artifacts (OSV, same date): `jackson-databind` 2.22.2 has two open advisories, both fixed in 2.22.3: GHSA-cxp5-3px4-pw24 (CVE-2026-91777, quadratic forward-reference completion) and GHSA-wv8q-qhhj-9h54 (CVE-2026-91776, retains every unknown raw type ID) [VERIFIED: api.osv.dev]. 2.22.3 itself, okhttp 5.5.0, org.json 20260814 and 20260719, mockito-core 5.24.0 and lz4 1.12.0 all returned `{}`.

**Consequence for the planner:** the evidence note and the Phase 15 CHANGELOG line must say "advisory-free = 1.11.4 or later", and the REQUIREMENTS wording "(≥ 1.11.2)" should be corrected where it is cited. Success criterion 1 ("at least 1.11.2") is satisfied by 1.12.0 and 1.11.4, but 1.11.2 or 1.11.3 would **fail** the advisory-free requirement.

## Architecture Patterns

### Commit and gate sequence (recommended order, within Claude's discretion)

```
[1] ci: tests.yaml trigger fix (CI-08) + compile step (CI-10) + fork-count step (CI-09)
        gate: YAML parses; compileIntegrationTestJava; testUnit; anchor count of "timeout-minutes: 45" still 1
[2] test: lz4 live round-trip test, green on 1.10.4 (D-02 commit 1)
        gate: that one test class on latest x V2 (Docker), plus testUnit
[3] build: lz4Java 1.12.0 (pure catalog edit)
        gate: compileJava + compileIntegrationTestJava, testUnit, [2]'s test again,
              dependencyInsight on runtimeClasspath and clickhouseDependencies
        failure -> try 1.11.4 -> else blocking checkpoint (D-03)
[4] build: okhttp 5.5.0        gate: compileIntegrationTestJava + testUnit (D-08)
[5] build: mockito 5.24.0      gate: compileIntegrationTestJava + testUnit (D-08)
[6] build: jackson 2.22.3      gate: FULL live suite on latest x V2 (D-07)  -> baseline for [7]
[7] build: orgJson 20260814    gate: FULL live suite (D-06); before = run at [6], after = this run
                               this run on the final src commit also serves as the D-09 phase gate
[8] docs/evidence + verify-merge-invariants.sh
[9] D-13: blocking human checkpoint -> push branch only -> observe the run (D-11b, CI-09)
```

Why this order: [1] first lets every later local gate match what CI will check, and it must precede the D-13 push anyway. [2] before [3] is D-02. okhttp and mockito are cheap and do not interact, so they sit between lz4 and the two live-gated bumps. Jackson goes immediately before org.json so that Jackson's full run is the exact "before" for org.json (no src-affecting commit between them), and org.json being last means its full run is also the D-09 gate on the final code commit. That saves a third full run. If any live-gated step needs a revert or a fix, the order of the two can swap; the invariant to keep is that nothing touches `src/` or the catalog between the "before" run and the org.json commit.

Evidence-only commits after [7] (SUMMARY, Recorded Facts, `.planning/` files) do not invalidate the D-09 run, as in Phase 13 (13-08 recorded its gate as an evidence-only task).

### Pattern 1: tests.yaml after the fix

```yaml
# Source: GitHub Actions workflow syntax docs
# https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
on:
  push:
    branches: ['**']

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true
```

- `'**'` is required so branch names containing `/` (such as `sync/v1.7.0`) match. A bare `*` does not cross `/`. [ASSUMED: standard GitHub glob semantics. The fetched docs page only demonstrates `releases/**` and does not define `*` vs `**`. Confirm with the first pushed run, which is also the D-11(b) observation.]
- "If you define only `tags`/`tags-ignore` or only `branches`/`branches-ignore`, the workflow won't run for events affecting the undefined Git ref." [CITED: docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions]. That sentence is the D-11(a) written note.
- Keep the file's existing `timeout-minutes: 45` exactly once (anchor `tests.yaml	timeout-minutes: 45	1`). Do not give any new step a `timeout-minutes: 45`; use a different value or none. The anchor check counts occurrences with `grep -cF` over the whole file [VERIFIED: `.planning/scripts/verify-merge-invariants.sh:242`, `a_actual="$(LC_ALL=C grep -cF -e "${a_pattern}" "${blob_file}")"`].
- Leave the `pull_request` branch of the group expression alone (harmless, near-upstream).

### Pattern 2: the two new steps (CI-09, CI-10)

```yaml
      - name: Report test fork count
        run: |
          cpus="$(nproc)"
          if [ -n "${CLICKHOUSE_CLUSTER_NAME:-}" ]; then forks=1; else forks=$((cpus / 2)); fi
          if [ "$forks" -lt 1 ]; then forks=1; fi
          msg="runner nproc=${cpus}, derived maxParallelForks=${forks} (build.gradle.kts: availableProcessors/2, minimum 1)"
          echo "$msg"
          echo "### Test fork count" >> "$GITHUB_STEP_SUMMARY"
          echo "$msg" >> "$GITHUB_STEP_SUMMARY"
          echo "::notice title=Test fork count::${msg}"

      - name: Compile integration-test sources
        run: ./gradlew compileIntegrationTestJava

      - name: Run tests
        run: ./gradlew test
```

The formula mirrors `build.gradle.kts` [VERIFIED: build.gradle.kts:200-205]:
```kotlin
tasks.withType<Test> {
    if (System.getenv("CLICKHOUSE_CLUSTER_NAME") != null) {
        maxParallelForks = 1
    } else {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
```
The `::notice::` line is the addition beyond D-12's wording, justified by Finding F3: it makes the value readable from the unauthenticated annotations endpoint (see Code Examples).

### Pattern 3: the D-01 round-trip test (shape, not final code)

Place it in `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/` beside `NetworkCompressionPinLiveTest`, extend `ClickHouseBase` (gives `@Tag("clickhouse")` [VERIFIED: ClickHouseBase.java:32 `@Tag("clickhouse")`] and the shared container), and build its own client so `ClickHouseHelperClient` and main code stay untouched:

```java
// Client-v2 0.9.6 Builder methods confirmed by javap on client-v2-0.9.6.jar:
//   compressServerResponse(boolean), compressClientRequest(boolean),
//   useHttpCompression(boolean), serverSetting(String,String)
// Client methods confirmed: insert(String, InputStream, ClickHouseFormat),
//   queryAll(String) -> List<GenericRecord>, execute(String)
Client client = new Client.Builder()
        .addEndpoint(url)                       // http://host:port/db from getBaseProps() / ClickHouseSinkConfig
        .setUsername(csc.getUsername())
        .setPassword(csc.getPassword())
        .setDefaultDatabase(csc.getDatabase())
        .compressClientRequest(true)
        .compressServerResponse(true)
        .serverSetting("network_compression_method", "lz4")   // required on 26.9 latest, see Pitfall 1
        .build();
```

What the test must do, in order:
1. **Pin the jar.** Assert `net.jpountz.lz4.LZ4Factory.class.getProtectionDomain().getCodeSource().getLocation()` contains `at.yawk.lz4` (Finding F2), and log the path and `Implementation-Version`/`pom.properties` version. Do **not** hard-code `1.10.4` or `1.12.0` in the assertion: D-02 requires the same test to be green before and after the version edit with no test change. The version evidence goes into the log and the Recorded Fact.
2. Create a table with a few typed columns (suggest `UInt32`, `String`, `Array(UInt8)` or `DateTime64`), insert enough compressible rows that the payload spans several LZ4 blocks (repeated long strings), via `insert(table, InputStream, ClickHouseFormat.JSONEachRow)`.
3. Read back with `queryAll("SELECT ... ORDER BY id")` and `assertEquals` every value against the source rows (typed, not `countRows`).
4. Ping/`SELECT getSetting('network_compression_method')` and `assertEquals("lz4", ...)` to show the setting reached the server.

Why a successful round-trip proves LZ4 on both legs: the client bytecode wires `ClickHouseLZ4OutputStream` (uses `LZ4Compressor.compress`) for requests and `ClickHouseLZ4InputStream` (uses `LZ4FastDecompressor.decompress`) for responses, through `LZ4Entity`, which gets `LZ4Factory.fastCompressor()` and `fastDecompressor()` [VERIFIED: `javap -c` on `com.clickhouse.client.api.internal.ClickHouseLZ4OutputStream`, `ClickHouseLZ4InputStream`, `LZ4Entity` from client-v2-0.9.6.jar]. If the server answered `compress=1` with ZSTD, or the request were not LZ4-framed, the call fails. Whether the client really takes the native-LZ4 path (rather than HTTP gzip) under `useHttpCompression=false` was inferred from bytecode, not observed live. That residual is in the Assumptions Log (A1); an optional extra assertion is to check `system.query_log` ProfileEvents for compressed-block counters (names unverified).

### Anti-Patterns to Avoid
- **Asserting a specific lz4 version inside the D-01 test.** It breaks D-02's "pure version edit" property.
- **Changing connector defaults to make the test pass** (for example flipping `compress=false`). D-01 forbids it; the test owns its client.
- **Fix-forward on a red bump** (D-05). Stop at the checkpoint.
- **Reading an `up-to-date` Gradle result as evidence.** Phase 13 saw `testUnit` served up to date in 723 ms; use `--rerun` for gate runs.
- **Pushing with `--follow-tags` or `--all`/`--mirror`** (D-13). Use an explicit refspec (see Pitfall 7).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Finding which lz4 jar is on a classpath | Custom parsing of Gradle output | `./gradlew dependencyInsight --configuration <cfg> --dependency lz4-java` | Already the BLD-09 evidence method (Recorded Facts 19, 24). Cross-check with a classpath-file print for the test classpath (Finding F2). |
| Advisory lookup | Scraping advisory pages | OSV `POST https://api.osv.dev/v1/query` with `{"package":{"name":"at.yawk.lz4:lz4-java","ecosystem":"Maven"},"version":"X"}` | Per-version, machine-readable, includes `fixed` versions. |
| Reading CI results | Parsing the HTML run page | REST: `/actions/runs?head_sha=`, `/actions/runs/{id}/jobs`, `/check-runs/{job_id}/annotations` | The only unauthenticated, scriptable channels (Finding F3). |
| Test summing | Reading the Gradle console summary | JUnit XML under `build/test-results/test/*.xml` | Phase 13 gate convention (13-08-SUMMARY). |
| Jackson annotation version | A manual `2.22.3` annotations pin | Keep `jacksonAnnotations = "2.22"` | The BOM already pairs it. There is no `jackson-annotations:2.22.3` artifact line to chase (annotations list is `2.20, 2.21, 2.22, 3.0-rc*`). |

**Key insight:** every question this phase asks ("which jar", "which run", "what fork count", "did the advisory clear") has a one-command, source-of-truth answer. Evidence should be pasted command output, not recollection.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted. (Closest analogue, checked: `.planning/MERGE-CHECKLIST-v1.7.0.md` row for `gradle/libs.versions.toml @@ -9,0 +10 @@` has `lz4Java = "1.10.4"` marked `Phase 14 | pending` [VERIFIED: MERGE-CHECKLIST-v1.7.0.md line 61]; the plan should flip it to done with the evidence, and add new Recorded Facts continuing from 31, since the highest existing number is 30.)

## Common Pitfalls

### Pitfall 1: ClickHouse `latest` answers `compress=1` with ZSTD
**What goes wrong:** the D-01 client sends `compress=1`, 26.9 replies in ZSTD native blocks, client `0.9.6` only decodes LZ4: `Invalid LZ4 magic byte: '-112'`, surfacing as "Connection to ClickHouse is not active".
**Why it happens:** the server default for `network_compression_method` changed on 26.9 [CITED: `NetworkCompressionPinLiveTest` class javadoc; Recorded Facts 25 and 26].
**How to avoid:** `serverSetting("network_compression_method", "lz4")` on the test client, and assert the setting back from `getSetting(...)`.
**Warning signs:** the failure appears on commit [2] (green-on-1.10.4 step), which would otherwise be misread as an lz4 problem.

### Pitfall 2: the test classpath has two lz4 jars
**What goes wrong:** `testRuntimeClasspath` contains `at.yawk.lz4/lz4-java/1.10.4` **and** `org.lz4/lz4-java/1.8.0` (via `io.confluent:kafka-*:7.9.1-ccs`). Both define `net.jpountz.lz4.*`. If classpath order ever put 1.8.0 first, the "bumped" test would run against the old jar and pass for the wrong reason.
**Evidence:** an init-script print of resolved files on 2026-10-09 gave, in order, `TESTCP .../at.yawk.lz4/lz4-java/1.10.4/.../lz4-java-1.10.4.jar` then `TESTCP .../org.lz4/lz4-java/1.8.0/.../lz4-java-1.8.0.jar` [VERIFIED: gradle init-script output]. `dependencyInsight --configuration testRuntimeClasspath --dependency lz4-java` shows `org.lz4:lz4-java:1.8.0` selected with no `->` redirect [VERIFIED]. `runtimeClasspath` (one jar, 1.10.4), `integrationTestRuntimeClasspath` (1.8.0 redirected `-> at.yawk.lz4:lz4-java:1.10.4`) and `clickhouseDependencies` (one jar) are clean [VERIFIED].
**How to avoid:** assert the `LZ4Factory` code-source origin in the D-01 test (Pattern 3). Record the test-classpath duplicate as a finding. D-04 only requires `runtimeClasspath` and `clickhouseDependencies`, and the shipped archive is built from `clickhouseDependencies` [VERIFIED: build.gradle.kts `confluentJar` is `from(clickhouseDependencies, sourceSets.main.get().output)`], so this does not affect shipped output. Whether to fix the capability resolution for the test classpath is a **user decision**, because it would be a `build.gradle.kts` change outside this phase's declared scope (see Open Questions).

### Pitfall 3: CI logs and step summaries are not readable unauthenticated
**What goes wrong:** D-12 says CI-09 is "satisfied by reading the value from that run's log or summary", and D-14 says to "read the job log through the unauthenticated GitHub REST API". Observed on the public repo for run 37619434848 / job 112785683668:
- `GET /actions/jobs/{id}/logs` -> HTTP 403 `"Must have admin rights to Repository."`
- `GET /actions/runs/{id}/logs` -> HTTP 403
- `GET /actions/artifacts/{id}/zip` -> HTTP 401
- `GET /actions/runs/{id}/jobs` -> 200 (step names and conclusions)
- `GET /check-runs/{job_id}/annotations` -> 200 (returned warnings and notices from that run)
- `$GITHUB_STEP_SUMMARY` has no REST endpoint.
[VERIFIED: curl results, 2026-10-09]
**How to avoid:** emit the fork count also as a `::notice::` annotation (Pattern 2); read it from `check-runs/{job_id}/annotations`. Use `/jobs` to prove the named steps ran and succeeded. For failure diagnosis under D-14, the log itself needs a browser or a token, so D-14 as written cannot be executed by the agent. See Open Questions.

### Pitfall 4: "exactly one run" has more than one way to fail
**What goes wrong:** the incident is real: on `c61d90ce` run 37619430314 (`custom_build`, `cancelled`) and run 37619434848 (`v1.6.0_gg1`, `success`) share a SHA, created 12:13:06Z and 12:13:08Z [VERIFIED: GitHub REST `actions/runs`, 2026-10-09]. The branch-only filter removes the tag event. Residual cases: two branches at one SHA (accepted, deferred), and an old tag whose commit predates the fix (the workflow file used is the one at the tagged commit, so a tag on a pre-fix SHA would still trigger; irrelevant for new tags created after the fix, relevant if someone re-pushes `pre-v1.6.0-sync`, which already exists on `origin`).
**How to avoid:** the D-11(b) check is `GET /actions/runs?head_sha=<sha>`, filter `path == ".github/workflows/tests.yaml"`, expect exactly one run, `status=completed`, `conclusion=success`, none `cancelled`.

### Pitfall 5: `ubuntu-latest` is about to change
**What goes wrong:** the annotations on the existing run say "The ubuntu-latest label will migrate to Ubuntu 26 beginning October 19, 2026" [VERIFIED: check-run annotations, 2026-10-09]. A scratch-branch run before that date and a Phase 15 run after it may differ in image, Docker, JDK and Gradle behaviour.
**How to avoid:** record `runner_name`/image info and the date next to the fork count. Treat a first red run after the migration as environment-suspect first (D-14 classification).

### Pitfall 6: the unauthenticated API is rate-limited to 60 requests per hour
**How to avoid:** fetch `runs?head_sha=` once, then `jobs`, then `annotations`; do not poll in a tight loop. Poll no faster than once per minute, or have the human confirm completion.

### Pitfall 7: an unqualified push can publish more than the branch
**What goes wrong:** local-only refs exist: tags `pre-v1.7.0-sync` and `v1.7.0` are local and not on `origin`; `origin` has `pre-v1.6.0-sync` and `v1.6.0_gg1` [VERIFIED: `git tag -l 'pre-*'`, `git ls-remote --tags origin`]. `push.followTags` is unset [VERIFIED: `git config --get push.followTags` empty], so a plain branch push would not carry them, but a changed config, `--tags`, `--follow-tags`, `--all` or `--mirror` would, and a tag reaching `origin` would, under the old workflow, have started a second run.
**How to avoid:** `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0`. Verify first with `git ls-files .planning/.gsd-ws-arg` (empty today; the file does not exist [VERIFIED]) and re-check `git ls-remote --tags origin` afterwards.

### Pitfall 8: org.json sits behind the test read-back, so a change there can look like a data bug
**What goes wrong:** `ClickHouseTestHelpers.getAllRowsAsJson` does `new JSONObject(line)` on `JSONEachRow` output [VERIFIED: ClickHouseTestHelpers.java:143-163]. A behavioural change in org.json would surface as a failing assertion on the data, not on the parser.
**How to avoid:** keep the D-06 bisect (revert `orgJson` alone) as the first move on any red full run after [7]. See Finding F6 for why a change is unlikely.

### Pitfall 9: anchors and the checklist must move with the edits
`fork-anchors.txt` line 40 anchors `tests.yaml` `timeout-minutes: 45` once [VERIFIED: fork-anchors.txt]. The catalog anchor is `kafka = "4.1.1"` once; none of the five bump lines is anchored. `verify-merge-invariants.sh` therefore needs no anchor edit unless a step is written with a second `timeout-minutes: 45`. Spotless: run `spotlessApply` only on the new test file.

## Code Examples

### Evidence commands (all read-only)

```bash
# D-04: one at.yawk lz4 on the two required configurations, at the chosen version
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight \
  --configuration runtimeClasspath --dependency lz4-java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight \
  --configuration clickhouseDependencies --dependency lz4-java
# also record, as an observation (not a requirement):
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight \
  --configuration testRuntimeClasspath --dependency lz4-java

# Advisory lookup for the chosen version (empty object = no open advisories)
curl -s -X POST https://api.osv.dev/v1/query \
  -d '{"package":{"name":"at.yawk.lz4:lz4-java","ecosystem":"Maven"},"version":"1.12.0"}'

# Jackson pairing check
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight \
  --configuration runtimeClasspath --dependency jackson-annotations   # expect annotations:2.22 (by constraint)
```

Baseline observed before any bump (2026-10-09): `runtimeClasspath` headline `at.yawk.lz4:lz4-java:1.10.4` with edges from `client-v2:0.9.6` (requested 1.10.2) and `kafka-clients:4.1.1` (requested `org.lz4:1.8.0`, redirected by the capability rule); `jackson-databind:2.22.2 (by constraint)` from `jackson-bom:2.22.2`, `jackson-annotations:2.22`; `okhttp:5.4.0` and `okhttp-jvm:5.4.0` on `integrationTestRuntimeClasspath`; `org.json:json:20260522` winning over everit's `20231013`; `mockito-core:5.23.0` via `mockito-junit-jupiter`. [VERIFIED: dependencyInsight output]

### Reading a CI run without a token

```bash
SHA=<pushed sha>
curl -s "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=$SHA" \
 | jq '[.workflow_runs[] | select(.path==".github/workflows/tests.yaml")
        | {id,event,head_branch,status,conclusion,run_started_at,updated_at}]'
# expect exactly one element, status completed, conclusion success, no "cancelled"
JOB=$(curl -s ".../actions/runs/<run id>/jobs" | jq '.jobs[0].id')
curl -s ".../actions/runs/<run id>/jobs" | jq '.jobs[0].steps[] | {name,conclusion}'   # step shape (CI-10)
curl -s ".../check-runs/$JOB/annotations" | jq '.[] | select(.title=="Test fork count") | .message'   # CI-09
```
(`jq` is installed at `/opt/homebrew/bin/jq`; `yq` is not. `python3 -I` also works.)

### Quote-able facts for the Recorded Fact (client 0.9.6 vs lz4-java API)

`javap` on lz4-java `1.12.0` shows `LZ4Factory.fastCompressor()` and `LZ4Factory.fastDecompressor()` returning `LZ4FastDecompressor`, and `LZ4FastDecompressor.decompress(java.nio.ByteBuffer, int, java.nio.ByteBuffer, int, int)` still public abstract [VERIFIED: javap on lz4-java-1.12.0.jar, class major version 51, so Java 7 bytecode]. The jar carries natives for linux amd64/aarch64/riscv64/ppc64le/s390x/i386 and darwin aarch64/x86_64 [VERIFIED: `unzip` listing], so macOS (local) and Linux amd64 (CI) both load native code. The client-v2 `0.9.6` call sites are exactly `LZ4Compressor.compress(ByteBuffer,int,int,ByteBuffer,int,int)` and `LZ4FastDecompressor.decompress(ByteBuffer,int,ByteBuffer,int,int)` [VERIFIED: javap -c]. It does not call `LZ4FrameInputStream`, `LZ4BlockInputStream` or `LZ4DecompressorWithLength`, the classes touched by 1.11.2-1.12.0 changes.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `org.lz4:lz4-java` | `at.yawk.lz4:lz4-java` fork | Adopted by client-v2 and upstream before v1.7.0 | `org.lz4` has no fix for the 2026-10-07 advisories (last affected 1.8.1). The fork is the only fixed line. |
| `on: [push]` | `on: push: branches: ['**']` | This phase | Tags stop starting `tests.yaml`. |
| `./gradlew test` only | plus `compileIntegrationTestJava` | This phase | Catches integration-only dependency breakage on every push. |

**Deprecated/outdated:** the wording "advisory-free (≥ 1.11.2)" in REQUIREMENTS.md BLD-10 and ROADMAP criterion 1. Floor is 1.11.4.

## Findings Detail

**F1 (lz4 floor).** See Advisory Lookup. 1.12.0 release notes: "Note that lz4-java v1.11.4, also released today, contains security fixes. This minor release contains some additional tightening of input validation that could in theory be breaking but should be fine for most if not all users. I recommend you upgrade to 1.12.0, but 1.11.4 is an option if that breaks." [CITED: github.com/yawkat/lz4-java/releases/tag/v1.12.0, published 2026-09-25]. 1.12.0 contents: throw `IOException` for invalid frame descriptors, reject short output in `LZ4DecompressorWithLength` safe paths, sticky stream failures in `LZ4FrameInputStream`/`LZ4BlockInputStream`. 1.11.3 added "Make default decompression limit configurable". None of those classes is called by client-v2 `0.9.6` (bytecode above); the Kafka consumer's own lz4 use is in `kafka-clients`, which the Connect worker supplies, not this plugin [ASSUMED: Connect plugin classloader isolation is the standard model, not re-verified here].

**F2 (test classpath).** Pitfall 2.

**F3 (CI readability).** Pitfall 3.

**F4 (`jacksonAnnotations`).** No change. The `jackson-bom-2.22.3.pom` pins annotations at `2.22`, and Gradle already resolves `jackson-annotations:2.22` "by constraint" from `jackson-bom:2.22.2` [VERIFIED]. 2.22.3 also contains nothing else behavioural for this repo's use: #6185 IPv6 bracket in `InetSocketAddress` serialization, #6203 and #6204 (the two CVEs), #6205 [CITED: jackson-databind `release-notes/VERSION-2.x`, branch 2.22]. Backlog items for 2.22.4 (not released) are out of scope.

**F5 (Jackson blast radius).** On `testRuntimeClasspath` the Confluent 7.9.1 stack requests Jackson 2.12-2.16 and is overridden to 2.22.2 by the BOM constraint [VERIFIED: dependencyInsight]; the patch bump to 2.22.3 therefore touches the Avro, Protobuf and Schema Registry tests too. This is why D-07 requires the full run.

**F6 (org.json number limit).** 20260719 added `DEFAULT_MAX_NUMBER_LENGTH = 1000` (alongside `DEFAULT_MAXIMUM_NESTING_DEPTH = 512`) in `JSONParserConfiguration` [VERIFIED: raw.githubusercontent.com/stleary/JSON-java/20260814/src/main/java/org/json/JSONParserConfiguration.java, lines 24 and 29]. Int256/UInt256 have at most 78 decimal digits and Decimal256 at most 76, so the limit is not reachable by this repo's fixtures. The roadmap's "default is unknown" is now resolved. The bisect rule in D-06 stays as the process, but a change is very unlikely. 20260719 also fixed CVE-2026-59171 (BigDecimal to BigInteger expansion); 20260814 only fixes `XML.unescape` for valid whitespace [CITED: github.com/stleary/JSON-java releases]. Usage is confined to tests: `src/main` has no `org.json` import [VERIFIED: `grep -rn "org\.json\.\(JSON\|Json\)" src/main` returned nothing], and `org.json` is not in `clickhouseDependencies`, so it is not shipped.

**F7 (what the full run compares for D-06).** The test classes that reference Int256/UInt256/Decimal256/Int128 are `WideIntegerTypeGateLiveTest`, `RowBinaryUnsupportedTypeGateTest`, `ColumnTest`, `ClickHouseWriterColumnContextTest`, `ClickHouseSinkTaskWithSchemaTest` [VERIFIED: grep -rliE]. The before/after comparison should be per-testcase outcomes for those classes taken from the JUnit XML of the Jackson-commit run (before) and the org.json-commit run (after), not the console summary. Phase 13's gate numbers to compare against: 68 classes, 803 tests, 0 failures, 0 errors, 18 skipped [CITED: 13-08-SUMMARY.md line 45]; on `latest` 26.9.13.15 at that time. `latest` may have drifted since (see memory note, "ClickHouse latest drifts"), so record the server version (`com.clickhouse.build.version` label on the image) in every gate run.

**F8 (okhttp, mockito blast radius).** okhttp is imported in one file, `src/integrationTest/java/.../helper/ConfluentPlatform.java`, using `OkHttpClient`, `Request`, `Response`, `ResponseBody`, `MediaType.get`, `RequestBody.create(String, MediaType)` [VERIFIED: grep]. 5.5.0 changes are ECH/DNS APIs, MockWebServer, multipart sink close, HTTP/1 flush, QUERY redirects and proxy-selector fallback, plus Okio 3.18.1 [CITED: github.com/square/okhttp CHANGELOG.md], none of which this file uses. The gate for okhttp is therefore compile-only in practice; the integration tests themselves need ClickHouse Cloud credentials and are skipped otherwise. Mockito is used by two Docker-free classes, `MetadataFreeJsonInsertUnitTest` (`mock(ClickHouseSinkConfig.class)`, `verify`, `never`, `times`) and `ClickHouseWriterTest` (`Mockito.spy`) [VERIFIED: grep]. 5.24.0 notes list doc fixes, "Preserve method parameters when clearing inline mocks", and "Print object fields via reflection when toString() is not implemented" [CITED: github.com/mockito/mockito/releases v5.24.0]. Low risk. `ClickHouseWriterTest` is a `ClickHouseBase` subclass in practice (live), so mockito 5.24.0 is only fully exercised by a Docker run, though D-08 sets the gate at `testUnit` plus compile; `MetadataFreeJsonInsertUnitTest` is in `testUnit` [VERIFIED: its javadoc says tagging it `clickhouse` "would drop it out of testUnit"]. Flag in the plan so nobody reads D-08's pass as full mockito coverage; the final D-09 run covers the rest.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | client-v2 `0.9.6` takes the native ClickHouse-LZ4 path (not HTTP gzip) when `compressClientRequest(true)`/`compressServerResponse(true)` and `useHttpCompression` default (false). Inferred from `LZ4Entity`/`HttpAPIClientHelper` bytecode, not observed live (Docker was stopped). | Pattern 3 | If the default were HTTP compression, the test would not exercise lz4-java and the D-01 evidence would be hollow. Mitigation: jar-origin assertion proves only classpath, not use; add an explicit `useHttpCompression(false)` on the test client, and confirm once with a live run (for example check `system.query_log` for compressed-block ProfileEvents, whose exact names are unverified). |
| A2 | GitHub `branches: ['**']` matches `sync/v1.7.0` while `*` would not. | Pattern 1 | Fetched docs only show `releases/**`. If wrong, the pushed branch gets no run and D-11(b) fails visibly on the first push. Cheap to detect, costly (a second push) to fix. |
| A3 | Public-repo `ubuntu-latest` has 4 vCPU, so `nproc=4` and `maxParallelForks=2`. Docs say Linux public repos are 4 vCPU/16 GB [CITED: docs.github.com/en/actions/reference/runners/github-hosted-runners], but the actual value must come from the run. | CI-09 | Only the expectation is assumed. The step measures the real value; record the measured number, not this one. |
| A4 | `nproc` equals the JVM's `Runtime.availableProcessors()` on a GitHub-hosted VM (no cgroup CPU quota). | Pattern 2 | If they differ, the step reports a derived value that is not the actual fork count. Optional cross-check: count distinct `Gradle Test Executor N` ids in the uploaded `gradle-test-reports`. |
| A5 | The Connect worker supplies its own `kafka-clients` and lz4 under plugin isolation, so the plugin archive's lz4 is used only by client-v2. | F1 | Only affects the blast-radius sentence, not the plan. |
| A6 | The 18 named skips in `docs/FORK-DELTA.md` are unchanged by Phases 13-14. | D-09 | Phase 14 adds one live test that must not skip; if a skip is added by accident the D-09 skip-set check fails visibly. |

## Open Questions

1. **D-14 and D-12 assume job logs are readable through the unauthenticated REST API. They are not (403).**
   - What we know: `/jobs` and `/check-runs/{id}/annotations` work unauthenticated; logs, run-log zips and artifact downloads do not.
   - What's unclear: how the agent should diagnose a red pushed run. Options: (a) the human pastes the failing log section from the web UI at the D-14 checkpoint; (b) add a step that emits a short failure summary as `::error::` annotations (a small workflow addition, not in D-12); (c) supply a read-only token, which conflicts with the "no `gh`" choice.
   - Recommendation: do (a) for diagnosis, and adopt the `::notice::` annotation for the fork count now (it costs one line). Ask the user whether (b) is wanted. Do not change D-14's classification and stop-at-checkpoint rule.

2. **Should the duplicate `org.lz4:lz4-java:1.8.0` on `testRuntimeClasspath` be fixed?**
   - What we know: it is test-only, vulnerable, and shadowed by `at.yawk` only by classpath order. The shipped archive is unaffected.
   - What's unclear: whether the user wants a `build.gradle.kts` change; it is outside the declared scope, so by the project rule it needs a decision.
   - Recommendation: do not change it in this phase. Pin the jar origin in the D-01 test, record the duplicate as a Recorded Fact, and offer it as a backlog item.

3. **Wording correction for "≥ 1.11.2".**
   - Recommendation: record the corrected floor (1.11.4) in the phase evidence and in the Phase 15 prose, and note the REQUIREMENTS/ROADMAP text as superseded rather than silently editing locked requirement text.

4. **Does the user want the phase to wait for ClickHouse `latest` consistency?** Every gate run must record the `latest` image version, because a red full suite on unchanged code should first be checked against the image date.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | all Gradle tasks | yes | OpenJDK 21.0.12 on PATH; Gradle toolchain targets 17 [VERIFIED: java -version; build.gradle.kts toolchain `JavaLanguageVersion.of(17)`] | Gradle provisions/finds toolchain 17 (builds already ran) |
| Gradle wrapper | everything | yes | 9.6.1 [VERIFIED: gradle-wrapper.properties] (CLAUDE.md's "7.4.2" is stale) | — |
| Docker via colima profile `vz` | D-01 test, Jackson and org.json full runs, D-09 gate | **no, currently stopped** | `colima list`: `vz` Stopped (8 CPU, 16 GiB); `default` Stopped | Start with `colima start -p vz` (never bare `colima start`, per the saved note). Docker CLI is not on PATH; Testcontainers uses `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` |
| Maven Central / OSV / GitHub API | version, advisory and CI evidence | yes | reachable 2026-10-09 | — |
| `jq` | evidence snippets | yes | `/opt/homebrew/bin/jq` | `python3 -I` |
| `yq` | `.github/workflows/tests.yaml` matrix lookup mentioned in AGENTS.md | no | — | not needed; the workflow no longer has a matrix |
| `gh` | — | no (by design) | — | unauthenticated REST |
| Network push access to `origin` | D-13 only | not tested, not to be tested | — | human performs or approves the push |

**Missing with no fallback:** none. **Missing with fallback:** Docker is stopped, so the plan's first live task must start colima `vz` and confirm with `colima status -p vz`.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json` [VERIFIED: config.json]. The per-bump gates are fully specified by D-02 to D-09 above.

## Security Domain

`security_enforcement` is enabled (`security_asvs_level: 1`, `security_block_on: high`) [VERIFIED: config.json].

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / V14 Config & Dependencies | yes | Advisory lookup per bumped artifact; exact pinned versions in the catalog; no new coordinates |
| V2 Authentication / V3 Session / V4 Access Control | no | Not touched. The D-01 test authenticates to the test container with the harness credentials from `getBaseProps()` |
| V5 Input Validation | partial | The test reads back typed values; no new parsing code |
| V6 Cryptography | no | None added |
| V8 Data Protection / logging | yes | Do not log passwords or record payloads in the new test or in the workflow steps (AGENTS.md). The fork-count step prints only CPU counts |
| CI supply chain | yes | Workflow `permissions` default is unchanged; the new steps use no third-party actions and no secrets; the `${{ ... }}` expressions in `run:` are confined to the existing concurrency key, with no untrusted input interpolated into shell |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Known-vulnerable transitive jar shadowing the fixed one (`org.lz4:1.8.0` beside `at.yawk` on the test classpath) | Tampering / DoS | Pin and assert the jar origin in the live test; keep `runtimeClasspath` and `clickhouseDependencies` at one fixed `at.yawk` jar |
| Jackson CVE-2026-91776 / -91777 (unbounded type-id cache, quadratic forward-reference resolution) in the shipped archive | DoS | Bump to 2.22.3 (BLD-11) |
| lz4 advisories (shared-temp native extraction race, stack/CPU exhaustion in stream classes) | Tampering / DoS | Bump to 1.12.0 or 1.11.4; client-v2 does not call the affected stream classes, but the native-extraction advisory applies to any `LZ4Factory.fastestInstance()` use |
| Branch push publishing local planning history or tags | Information disclosure | Explicit refspec, verify `.gsd-ws-arg` untracked, confirm `git ls-remote --tags` before and after (Pitfall 7) |
| Workflow expression injection | Tampering | No new `${{ github.* }}` interpolation into `run:` scripts |

## Sources

### Primary (HIGH confidence)
- Maven Central `maven-metadata.xml` for `at/yawk/lz4/lz4-java`, `com/fasterxml/jackson/core/jackson-databind`, `jackson-annotations`, `com/squareup/okhttp3/okhttp`, `org/json/json`, `org/mockito/mockito-junit-jupiter` - versions and dates, fetched 2026-10-09
- `jackson-bom-2.22.3.pom` on Central - annotations pairing
- OSV API (`api.osv.dev`) - per-version advisory queries for all bumped artifacts
- Local repo files read this session: `gradle/libs.versions.toml`, `build.gradle.kts`, `.github/workflows/tests.yaml`, `.planning/scripts/fork-anchors.txt`, `.planning/scripts/verify-merge-invariants.sh`, `NetworkCompressionPinLiveTest.java`, `ClickHouseHelperClient.java`, `ClickHouseTestHelpers.java`, `ClickHouseBase.java`, `.planning/MERGE-CHECKLIST-v1.7.0.md`, `13-08-SUMMARY.md`
- `javap` on `client-v2-0.9.6.jar` and on `lz4-java-1.12.0.jar` (downloaded into separate scratch directories)
- Live `dependencyInsight` and a Gradle init-script classpath print, 2026-10-09
- GitHub REST API (unauthenticated) for runs, jobs, annotations, logs and artifacts on `ne1r0n/clickhouse-kafka-connect`

### Secondary (MEDIUM confidence)
- docs.github.com workflow syntax page (branches-only filter sentence, cancel-in-progress) and hosted-runner specs page (4 vCPU public, 2 vCPU private)
- GitHub release notes: yawkat/lz4-java v1.11.0-v1.12.0, stleary/JSON-java 20260719/20260814, mockito v5.24.0, square/okhttp CHANGELOG 5.5.0, FasterXML jackson-databind `VERSION-2.x`
- Raw source of `JSONParserConfiguration.java` at tag 20260814

### Tertiary (LOW confidence)
- None relied on. Items not verified are in the Assumptions Log.

## Metadata

**Confidence breakdown:**
- Standard stack (versions, existence): HIGH - read directly from Central metadata.
- Advisory status: HIGH for the dates and versions queried; the advisories are two days old, so re-run the OSV query on the day of the bump commit and paste the output.
- Architecture (order, CI shape): HIGH for workflow semantics (documented, plus the observed incident); MEDIUM for the glob and runner-CPU details (A2, A3).
- Live LZ4 behaviour: MEDIUM - bytecode and API inspection only; Docker was stopped (A1).
- Pitfalls: HIGH where marked VERIFIED.

**Research date:** 2026-10-09
**Valid until:** about 7 days for the advisory and version facts (the lz4 line is moving fast: five releases since July); 30 days for the workflow-semantics facts. Re-run the OSV lookup and the Central metadata check at execution time, and before Phase 15.
