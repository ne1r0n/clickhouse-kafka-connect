# Phase 45: Upstream Tail Merge & Dependency Check - Research

**Researched:** 2026-11-04
**Domain:** Fork upstream-sync recipe (scratch branch, rollback tag, one mechanical merge commit, per-path checklist, invariant gate) plus a re-expression of upstream PR #857 under the fork's value-free writer policy, and a manual dependency/CVE check
**Confidence:** HIGH (the whole merge was rehearsed end to end in a throwaway clone outside the repo, and the real gates were run on it; the real repo is untouched, `git status` shows only the two pre-existing dirty `.planning/config.json` and `.planning/state.json`)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md is "Auto-generated (discuss skipped via workflow.skip_discuss)". Its `## Decisions` section holds only the `### Claude's Discretion` block below.

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase skipped.

Phase boundary restated by CONTEXT.md and the ROADMAP note: nothing is pushed; no dependency moves without the user's approval (AGENTS.md); every dependency bump is the user's decision in their own words; the record-context coordinates, how upstream's new tests are kept, and any dependency bump are flagged below as user decisions.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UPS-01 | Rollback tag `pre-v1.7.0_gg8`, scratch branch, upstream `83808df9` merged in one mechanical commit with a generated per-path checklist | Pre-flight re-measured (no STOP); the exact 6-path conflict set and the 3 further auto-merged fork-touched hunks; rehearsed merge passed the unedited gate (`provenance_paths=11 ... anchor_rows=190 result=PASS`) once one auto-merged hunk was resolved to the fork side; commit sequence in "Architecture Patterns" |
| UPS-02 | #857 merged value-free; record context measured first; gg3 pins unchanged | Measured per failure shape (table in "#857 measurements"); the 216-case matrix is blind to #857 (it bypasses the code #857 changed); the one existing pin that does catch it is the live `WriterErrorPathLiveTest` WRT-10; classification does not move on the production path; the real defects are a log-line value leak for one shape and a message-prefix change |
| UPS-03 | Slash column (#839) round-trips on `latest` x V2 under raw-identifier matching | Upstream's `SlashColumnRoundTripTest` passes unchanged against the merged tree on ClickHouse `latest` (V1 and V2, 4 cases, live) |
| UPS-04 | `fromJsonRow` reconciled with Phase 28; NAME-01..07 pins unchanged; differences pinned | Fork side wins the hunk; the NAME pins pass (RawIdentifierNameLiveTest 73, RawNameMatrixFixturesUnitTest 50, RawNameMatrixLiveTest 25, DeclaredNullabilityTest 69, 0 failures); exactly one upstream assertion differs by design (decoded vs literal-JSON `comment`/`default_expression`) |
| UPS-05 | slf4j-simple 2.0.20, spotless 8.10.4, commons-lang3 3.21.0 taken; invariant script PASS; anchor count updated | All three are already on HEAD (identical lines on both sides); resolution configurations named; shipped commons-lang3 is 3.20.0, not 3.21.0 (explained) |
| DEP-01 | Manual dependency and CVE check recorded | Scope proposal, advisory sources verified working with Dependabot alerts disabled, and a pre-measurement of the whole shipped set (zero advisories) |
| DEP-02 | Each approved bump in its own commit, gates green | Stale-version shortlist; every shipped bump changes the Confluent archive and so Phase 49's allow-list |
</phase_requirements>

## STOP-condition check (done first, as the roadmap asks)

| Check | Result | Evidence |
|---|---|---|
| `git ls-remote upstream main` | `83808df9a0993cf49b2ceaa4633c782e550c0180 refs/heads/main` — has NOT moved past `83808df9` | [VERIFIED: `git ls-remote upstream main`, run 2026-11-04] |
| Upstream tag past `v1.7.0` | NONE. `git ls-remote --tags upstream | sed ... | sort -V | tail -5` ends `v1.7.0`; `git ls-remote --tags upstream 'v1.7*'` prints only `379b8d018c090ba5279c2a9f86dbfe4934a19329 refs/tags/v1.7.0`. Upstream's `VERSION` at `83808df9` reads `v1.7.1` and no tag carries it | [VERIFIED: commands run 2026-11-04; `git diff 9bed6f79 83808df9 -- VERSION` shows `-v1.7.0` / `+v1.7.1`] |
| Conflict set vs roadmap | Identical: `git merge-tree --write-tree --name-only HEAD 83808df9...` exit 1, six paths: `.github/workflows/tests.yaml`, `CHANGELOG.md`, `VERSION`, `gradle/libs.versions.toml`, `.../db/helper/ClickHouseFieldDescriptor.java`, `.../db/helper/ClickHouseHelperClient.java` | [VERIFIED: output saved this session; merge base `9bed6f7970670a55c66be8efda246889e1f95bd0`, the previous tail] |
| Pre-merge tip | `sync/v1.7.0` = `f848bf3569f6d0e29244326d3c392ed014fa8bc6`, code tree byte-equal to `origin/custom_build` (`git diff --stat origin/custom_build HEAD -- . ':!.planning'` prints nothing); `94834763` (the published `--no-ff` merge) is NOT an ancestor of HEAD, whose merge base with it is `fc570937` | [VERIFIED: commands run 2026-11-04] |

**No STOP condition. Planning proceeds.** The plan must still re-run `git ls-remote upstream main`, the tag list and `git merge-tree` immediately before the merge, record the output, and compare to the SHA above. If upstream moved or tagged, stop (re-measure, or FUT-01 tagged sync: the user decides scope). `git ls-remote origin` failed with `Permission denied (publickey)` once this session after succeeding earlier; nothing in this phase needs origin, because nothing is pushed.

## Summary

The recipe repeats cleanly. The merge is mechanical: six conflicted paths, all resolved by "take the fork side of the hunk", plus one auto-merged hunk that must also be resolved to the fork side (below). Everything the tail breaks is repaired in separate, small commits afterwards. I rehearsed it in `/tmp/gsd45probe/repo` (throwaway `git clone --local`, outside the repo): merge, the unedited invariant gate, compile gates, `testUnit` with Docker unreachable, the live upstream tests on `latest`, and one full `test --rerun`. Numbers below are measured there.

Five findings change the plan relative to the roadmap text.

1. **The classification matrix does not gate #857.** `WriterFailureClassificationUnitTest` (216 tests today, not 184; the 184 figure is Phase 22's count and the class has grown) drives `ScriptedWriteFailureWriter`, which overrides `doInsertRawBinaryV1/V2` and calls `doWriteColValue` directly, so it never runs `writeRecordsRowBinary`, the method #857 changed. The matrix passes green on the merged tree while #857's wrap is live. The only existing pin that catches #857 is the live `WriterErrorPathLiveTest` WRT-10 (needs Docker, so it is in `test`, not `testUnit`). A new Docker-free pin through `writeRecordsRowBinary` is required.
2. **#857 does not move retry classification on the production path.** `Processing.doInsert` wraps every writer failure in `new RuntimeException(queryId.toString(), e)` (`Processing.java:72`), so `Utils.handleException` never sees a top-level `DataException` from a writer, and its root-cause rules see the same root. Measured for 9 shapes x 4 settings: no outcome differs. (Feeding `handleException` a raw `DataException` directly does show flips; that is a probe artifact, see Pitfall 3.) The roadmap's "changes retry classification at once" is therefore not borne out; what #857 does change is text and logging.
3. **The real #857 defects.** (a) Its ERROR log line and `DataException` message embed `cause.getMessage()`; for the one shape where the fork passes a Jackson exception through (a timeout beneath it), that text carries a Map key or value. (b) It prefixes every fork message with `Failed to serialize record for topic ..., column ...:` and repeats the column, which breaks the live WRT-10 pin (`startsWith("column 'at.a' (")`). (c) It rewraps the fork's pass-through exceptions, so their class stops being the thrown class at the writer boundary.
4. **An auto-merged hunk silently adds a dead resource** in `ClickHouseHelperClient.describeTableV1` (an unused `BufferedReader reader` over the same stream). It moves two anchor rows (`StandardCharsets.UTF_8` 1 to 2, `InputStreamReader` 2 to 3) and fails gate invariant 3 on the merge commit itself. Resolving that one hunk to the fork side inside the merge adds no authored line (set membership holds) and the gate passes.
5. **UPS-05 is already satisfied by HEAD.** The three bumps are identical on both sides and arrive as no change. The merge adds only `kafka-connect-json` (test classpath).

Rehearsed result after the merge, before any repair: `compileJava` passes; `compileTestJava` fails with exactly 2 javac errors, both `new SinkTaskStatistics(0)` in upstream's `ClickHouseWriterRecordContextTest` (the fork's only constructor is `(String, int)`). After patching that in the probe: `testUnit` 1837 tests, 1 failed, 2 skipped, 21 s; full `test --rerun` on `latest`: 2245 tests, 2 failed, 19 skipped, 2 m 47 s. The 2 failures are `FieldDescriptorEscapeTest.preservesEscapedCommentsAndDefaultExpressions` (upstream's expectation, differs from the fork by design) and `WriterErrorPathLiveTest` WRT-10 (#857). Nothing else is red.

**Primary recommendation:** Merge as the fork recipe says, resolving the six conflicts and the one auto-merged `describeTableV1` hunk to the fork side. Re-express #857 as "keep the seam, keep the fork's exception untouched, log coordinates only" (Option A in the decisions table) behind a new Docker-free pin on `writeRecordsRowBinary`. Take #839 and `FieldDescriptorEscapeTest` with one named fork difference. Run the DEP-01 check against the resolved `clickhouseDependencies` tree (the shipped set), read advisories from OSV.dev and the GitHub advisory database, and propose no bump unless a CVE appears (the pre-measurement found none).

## Decisions that belong to the user at plan time

| # | Decision | Options measured | Recommended default (planner may propose, user confirms) |
|---|---|---|---|
| U-1 | Does the fork keep record coordinates (topic, partition, offset: metadata, not values) on a serialization failure, and where? | **A:** fork exception unchanged; one metadata-only ERROR log line per failure (coordinates, column, exception class name; never `getMessage()`); no new exception layer. WRT-10, the 216 matrix and gg3 text pins stay green unchanged. **B:** keep #857 as merged: red on WRT-10, leaks one shape into the ERROR line; violates "gg3 pins pass unchanged". **C:** drop coordinates entirely (revert the loop to the fork's body, keep only the package-private visibility): no new value risk, no operator benefit. **D:** an outer exception that is not a `DataException` carrying coordinates in its message: WRT-10 would pass (its `findFirst` DataException is the inner one) but adds a new exception type to the chain for little gain | **A.** Any design that keeps the pins unchanged leaves upstream's `serializationFailureNamesTopicPartitionOffsetAndColumn` red, because it asserts the coordinates are in the `DataException` message (see U-2) |
| U-2 | How upstream's three new tests are kept | `SlashColumnRoundTripTest`: green as is (live, V1+V2). `ClickHouseWriterRecordContextTest`: cannot be green "as is" (compile error on `SinkTaskStatistics(0)`); needs at least a 2-site edit to `(null, 0)`; under Option A its first test also needs its assertion moved from message to log line, or a named fork copy beside an unchanged-except-compile original. `FieldDescriptorEscapeTest`: 8 of 9 green as is; `preservesEscapedCommentsAndDefaultExpressions` is red by design | Edit the minimum in the two upstream files and name each edit in FORK-DELTA (Phase 34 precedent: only compile-forced or intent-forced edits to upstream-owned tests), or keep upstream's file byte-identical, `@Disabled` the one method with a reason, and add a fork-owned test with the fork's expectation. `@Disabled` changes the skipped count (baseline 18 to 19 full-suite skips in the rehearsal), which the two-run equality gate then sees. AGENTS.md forbids deleting or weakening tests, so a fork-owned replacement beside an untouched original is the safest shape |
| U-3 | Does upstream's `# 1.7.1, Unreleased` CHANGELOG block (the #839 fix and the #857 note) get absorbed in this phase or in Phase 49 (RLS-01)? | The merge takes the fork side of `CHANGELOG.md` (as the roadmap note says), so those bullets are not carried | Absorb at the repair commit that makes the fork's behaviour true, in the fork's words, not upstream's (upstream's #857 bullet describes a message the fork does not produce); Phase 49 then only moves counts |
| U-4 | Any dependency bump (DEP-02) | None required by CVE (pre-measurement: 0 advisories on every shipped coordinate). Stale but advisory-free: guava 33.7.2-jre to 33.8.0-jre, commons-io 2.20.0 to 2.22.0, commons-codec 1.19.0 to 1.23.0 (transitive, shipped), spotless 8.10.4 to 8.11.0, versions plugin 0.66.0 to 0.67.0, jacoco 0.8.15 to 0.8.16, Confluent 7.9.1 to 7.9.10 (patch) | Propose nothing unless the recorded check finds an open CVE; list the above as "stale, no advisory" and record the user's own words per proposal, or "none approved" |
| U-5 | DEP-01 scope | See "DEP-01 scope and sources" | The shipped `clickhouseDependencies` tree is first-class; test fixtures, integration tests, Gradle plugins, wrapper and workflow actions are recorded in separate tables |

## Architectural Responsibility Map

This phase is a source-merge and test phase inside one JVM sink connector; there is no browser/SSR/CDN tier. Tiers are connector layers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Per-record failure context on a serialization error | `sink.db.ClickHouseWriter` (`writeRecordsRowBinary`, `doWriteColValue`) | `sink.processing.Processing.doInsert` (wraps in `RuntimeException(queryId)`) | The writer owns the text; Processing owns the wrap that keeps classification stable |
| Retry classification | `util.Utils.handleException` (root-cause rules) | `ProxySinkTask.put`, `ChunkFlusher.putDirect` | One decision function; both entries pass the same arguments |
| DESCRIBE row decoding (names, types) | `sink.db.helper.ClickHouseFieldDescriptor.fromJsonRow` | `DescribeTableBuilder`, `DeclaredNullability.normalise` (decode the type text's JSON layer later) | The fork keeps the type text literal on purpose; names are decoded once |
| DESCRIBE transport (UTF-8 lines, V1 and V2) | `ClickHouseHelperClient.readDescribeRows` | — | One reader for both clients (HLP-04) |
| Merge provenance and anchors | `.planning/scripts/verify-merge-invariants.sh` | `fork-anchors.txt`, `conflicted-paths-*.txt` | Git-level gate; no runtime tier |
| Dependency and CVE record | Gradle resolution (`clickhouseDependencies`, `*RuntimeClasspath`) | OSV.dev, GitHub advisory database | Read-only evidence; no runtime tier |

## Standard Stack

No new library arrives except one test-scope artifact. No version in the shipped set changes by the merge.

### Core (merge-relevant, unchanged)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| ClickHouse Java client | 0.11.0 | Driver | `clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)` [VERIFIED: `gradle/libs.versions.toml:2`, Read this session] |
| Kafka Connect API | 4.4.0 | `ConfigException`, `RetriableException`, `DataException` | `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` [VERIFIED: `gradle/libs.versions.toml:3`] |
| slf4j-simple | 2.0.20 | integration-test logging | `slf4j = "2.0.20"` (line 28); resolves on `integrationTestRuntimeClasspath` [VERIFIED: `dependencyInsight` on the rehearsal tree] |
| commons-lang3 | 3.21.0 (test, test fixtures); 3.20.0 shipped | `RandomStringUtils` in test fixtures; transitive of the client | `commonsLang3 = "3.21.0"` (line 27); `testFixturesImplementation(libs.commons.lang3)` [VERIFIED: `build.gradle.kts:154` via grep, and `dependencyInsight`] |
| Spotless | 8.10.4 | formatting plugin | `spotless = "8.10.4"` (line 31); `buildEnvironment` lists `com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:8.10.4` [VERIFIED] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `org.apache.kafka:connect-json` | 4.4.0 (via `version.ref = "kafka"`) | test-only: upstream's `SlashColumnRoundTripTest` uses `JsonConverter` | `testImplementation(libs.kafka.connect.json)` arrives with the merge |

`connect-json` checks (the roadmap research flag):
- It uses the fork's `kafka` ref, so it is the same 4.4.0 as the pinned Connect API. [VERIFIED: merged `libs.versions.toml` line `kafka-connect-json = { module = "org.apache.kafka:connect-json", version.ref = "kafka" }`]
- It is on `testRuntimeClasspath` and `testCompileClasspath` only; `dependencyInsight --configuration runtimeClasspath --dependency connect-json` prints "No dependencies matching given input were found", so it does not ship and the Confluent archive and allow-list are unaffected. [VERIFIED: `dependencyInsight`, rehearsal tree]
- On `testRuntimeClasspath` the only requester of `connect-json` is the root project, so the Confluent test stack (`io.confluent:*:7.9.1`) does not bring a competing `connect-json`; resolution is plain `4.4.0`. [VERIFIED: `dependencyInsight` shows `\--- root project` as sole requester]
- It is not in the Gradle cache: `./gradlew --offline compileTestJava` fails with "No cached version of org.apache.kafka:connect-json:4.4.0 available for offline mode". The first build after the merge needs network (Docker-free `testUnit` included). It is on Maven Central, `<release>4.4.0</release>`. [VERIFIED: offline failure output; `repo1.maven.org/maven2/org/apache/kafka/connect-json/maven-metadata.xml`]
- BLD-16 (`ConnectApiReferencesUnitTest`) derives from `build/classes/java/main`, so a test-scope artifact cannot move it; it passed in the rehearsal's `testUnit`.

**Installation:** none beyond the catalog line the merge brings.

## Package Legitimacy Audit

The `package-legitimacy` seam supports only `npm|pypi|crates` (`gsd-tools package-legitimacy check --ecosystem <npm|pypi|crates>`; `maven` returns the usage error), so no seam verdict exists for a Maven coordinate. Evidence used instead:

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `org.apache.kafka:connect-json` | Maven Central | same group, same version line as the already-pinned `connect-api` | n/a | github.com/apache/kafka | Not checked by seam (unsupported ecosystem); coordinate comes from upstream's own `build.gradle.kts` and `libs.versions.toml` diff and is in the same `org.apache.kafka` group as `connect-api` | Approved as part of the merge (test scope only); OSV reports 0 advisories for `connect-json@4.4.0` |

**Packages removed due to [SLOP] verdict:** none. **Packages flagged as suspicious [SUS]:** none. The claim "Apache project artifact" is [CITED: Maven Central group `org.apache.kafka`] and rests on group identity, not a seam verdict; if the user wants a stricter gate, add a `checkpoint:human-verify` before the first resolve.

## Architecture Patterns

### System Architecture Diagram

```
 upstream/main 83808df9 ─► (0) pre-flight record ─► ls-remote x2 (main, tags), merge-tree (exit 1, 6 paths)
                                   │
 sync/v1.7.0 tip f848bf35 ─tag──► pre-v1.7.0_gg8 (local, never pushed; = M's first parent)
                                   │
                    (1) git merge --no-ff --no-commit 83808df9...
                                   │ 6 conflicted paths: fork side per hunk
                                   │ +1 auto-merged hunk (describeTableV1 dead resource): fork side
                                   ▼
                    M  (one mechanical commit; no .planning file inside; no authored line)
                                   │ gate: PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=<full sha> (needs the record commit)
                                   ▼
          (2) record commit: conflicted-paths-v1.7.0-tail-3.txt + MERGE-CHECKLIST-v1.7.0-tail-3.md
                                   ▼
      state after M: compileJava OK | compileTestJava 2 errors | testUnit 1 red | live suite 2 red
                                   ▼
 (3a) compile fix for ClickHouseWriterRecordContextTest ─► (3b) UPS-02 re-expression + Docker-free pin
        + anchor rows (same commit as the code)              + log-line pin (never a message pin on a value)
                                   ▼
 (3c) UPS-04 upstream-test disposition + fork-owned fromJsonRow difference pins + FORK-DELTA rows
                                   ▼
 (4) UPS-03 live evidence | UPS-05 dependencyInsight evidence | (5) DEP-01 record | (6) DEP-02 per approved bump
                                   ▼
 (7) phase gate: cache-busted testUnit (Docker unreachable) | two `test --rerun` on latest x V2 | tail gate once
```

### Measured conflict set and resolution rules

Resolution for every hunk: fork (HEAD) side. All lines of the result then exist in P1 or P2 (invariant 1). [VERIFIED: `git merge -X ours` rehearsal plus the unedited gate]

| Path | Hunks | Resolution | Why / what auto-merges around it |
|---|---|---|---|
| `.github/workflows/tests.yaml` | 1 | fork side | The fork's `tests.yaml` has one job, `build:`, and no `cloud-tests` job (`grep` finds no `CLOUD`/`CONNECTORS_TEAM` text; line 2 says the fork tests only ClickHouse `latest`). Upstream's secret rename (`INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT` to `CONNECTORS_TEAM_TESTS_CLOUD_HOST`) sits inside a job the fork lacks, so the fork side drops it. Anchor `timeout-minutes: 45` (CI-05) unaffected |
| `CHANGELOG.md` | 1 | fork side whole | Upstream adds a `# 1.7.1, Unreleased` block at the top and a `## Bug Fixes` bullet under `# Unreleased`; the fork absorbed `Unreleased` in Phase 38, so nothing pairs with it. Difference from Phase 34 (which unioned): here the rehearsal left `CHANGELOG.md` byte-identical to HEAD. Absorption is decision U-3 |
| `VERSION` | 1 | fork side (`v1.7.0_gg7`) | Anchor row `VERSION	v1.7.0_gg7	1	BLD-07` stays 1 until Phase 49 bumps through `bump_version.py` |
| `gradle/libs.versions.toml` | 1 | fork side (`shadow = "9.6.1"`, upstream `8.3.5`) | The `commonsLang3`, `slf4j`, `spotless` lines auto-merge as no-ops (HEAD already holds them); the new `kafka-connect-json` library line auto-merges in |
| `.../db/helper/ClickHouseFieldDescriptor.java` | 1 | fork side | The fork's Phase 28 `fromJsonRow` (decode only `name`, keep other strings as literal JSON text, message never embeds the row) wins over upstream's one-line `readValue(json, ...)` |
| `.../db/helper/ClickHouseHelperClient.java` | 3 | fork side in all three (imports; `describeTableV1` body; `describeTableV2` body) | Fork already routes both clients through `readDescribeRows` with `StandardCharsets.UTF_8`, so #839's transport change is already in the fork's shape |

**Auto-merged hunks in fork-touched files that need a decision (the checklist's Section 2 must list them):**

| File | Auto-merged hunk | Decision |
|---|---|---|
| `ClickHouseHelperClient.java` (`describeTableV1`) | adds `BufferedReader reader = new BufferedReader(new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))` as a second try-resource while the body still calls `readDescribeRows(builder, response.getInputStream())`; the `reader` variable is never read | **Resolve to the fork side inside M** (restore `.executeAndWait()) {`). Measured otherwise: gate at M reports `ANCHOR VIOLATION ... 'StandardCharsets.UTF_8' expected 1 got 2 (HLP-04)` and `'InputStreamReader' expected 2 got 3 (HLP-04)`, and the anchors file cannot be edited inside M. With the hunk dropped: `invariants: provenance_paths=11 anchor_commits=1 anchor_rows=190 marker_commits=19 anchor_exempt=0 result=PASS` |
| `ClickHouseHelperClient.java` | removes the `ClickHouseRecord`/`ClickHouseValue`/`JsonProcessingException` import lines the fork no longer uses | taken as merged (compiles) |
| `ClickHouseWriter.java` (high-risk file) | #857: `writeRecordsRowBinary` private to package-private, new `writeColumnWithRecordContext` and `serializationFailure`, `import ...SinkRecord` | taken as merged in M (mechanical); re-expressed afterwards (UPS-02) |
| `build.gradle.kts` | `testImplementation(libs.kafka.connect.json)` | taken as merged |
| three new upstream tests | `ClickHouseWriterRecordContextTest`, `FieldDescriptorEscapeTest`, `SlashColumnRoundTripTest` | taken as merged in M; dispositions below |

M, as rehearsed, changes exactly 6 files (`build.gradle.kts`, `libs.versions.toml`, `ClickHouseWriter.java`, 3 new tests), +298 / -5. [VERIFIED: `git diff --stat HEAD^1 HEAD` in the probe]

### Recommended commit sequence (all on `sync/v1.7.0`, nothing pushed)

| # | Commit | Contents | Gate at that commit |
|---|---|---|---|
| 0 | (no commit) | Record `git ls-remote upstream main`, the tag list, `git fetch upstream --tags`, the `merge-tree` output; `git tag pre-v1.7.0_gg8 HEAD` (local only) | tag equals M's first parent |
| 1 | M `Merge upstream tail 83808df9 (...)` | `git merge --no-ff --no-commit 83808df9a0993cf49b2ceaa4633c782e550c0180`, resolve per tables, `git commit`. Subject/body pattern: `git log -1 38265d26` (`Merge upstream tail a6f1df83 ...`; the body lists the conflicts and says the record follows in the next commit) | tail gate with the record commit as range end |
| 2 | record | `.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt` (6 sorted lines) + `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md`; Recorded Facts continue at **46** (tail-2 ends at 45) | tail gate re-run with `CONFLICTED_PATHS_FILE=` that file |
| 3 | `fix(45)` compile | `ClickHouseWriterRecordContextTest`: `new SinkTaskStatistics(0)` to `(null, 0)` at lines 41 and 63 (per U-2) | `compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` |
| 4 | `fix(45)` UPS-02 | writer re-expression + new Docker-free pin + anchor rows in the same commit | cache-busted `testUnit`; anchors |
| 5 | `test(45)` UPS-04 | `fromJsonRow` fork-difference pins, FORK-DELTA row text | `testUnit`; live NAME pins |
| 6 | docs/evidence | FORK-DELTA, CHANGELOG (per U-3), evidence file | phase gate |
| 7+ | DEP-01 record; one commit per approved bump | per DEP-02 | `testUnit` Docker down + full suite |

**Rules inherited from earlier tails** (Phase 34 research and `MERGE-CHECKLIST-v1.7.0-tail-2.md`, read this session):
- No `.planning` file inside M (fails the provenance invariant) and no `.planning` commit between `git merge --no-commit` and M. The dirty `.planning/config.json` and `.planning/state.json` are untouched by upstream and do not block `git merge`.
- Anchor rows change only in the same commit as, or after, the code they pin (the gate checks every commit in `PRE_TAG..HEAD` against that commit's own copy of `fork-anchors.txt`).
- `UPSTREAM_REF` is the full SHA, never `upstream/main`. `PRE_TAG` must be an ancestor of M's first parent; tag HEAD itself. Tagging the published merge `94834763` would fail the "every parent is an ancestor" clause (its parent `72e8ccff` is not an ancestor of HEAD). [VERIFIED: `git merge-base HEAD origin/custom_build` = `fc570937`]

### Gate invocation (unedited script, defaults read at `verify-merge-invariants.sh:26-30`)

```bash
PRE_TAG=pre-v1.7.0_gg8 \
UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 \
CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt \
.planning/scripts/verify-merge-invariants.sh <M-full-sha> HEAD
```

Verbatim defaults: `PRE_TAG="${PRE_TAG-pre-v1.7.0-sync}"`, `UPSTREAM_REF="${UPSTREAM_REF-refs/tags/v1.7.0}"`, `CONFLICTED_PATHS_FILE="${CONFLICTED_PATHS_FILE-.planning/scripts/conflicted-paths-v1.7.0.txt}"`. Predicted stable counts on the real run: `provenance_paths=11` (6 conflicted + 6 changed, `libs.versions.toml` in both) and `anchor_rows=190` before new rows; `anchor_commits` and `marker_commits` scale with real history, assert `result=PASS` and `provenance_paths=11`, not the others. The default-environment cumulative gate (`verify-merge-invariants.sh 54e63648 HEAD`) is the slow one (about 52 minutes at Phase 33); run it once in the background, and prove the two scripts unedited with `git diff --quiet pre-v1.7.0_gg8 HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh`.

### #857 measurements (UPS-02: "measured before re-expressed")

Method: the merged tree, a throwaway test (deleted, never in the repo) that builds a real `ClickHouseWriter`, calls the now package-private `writeRecordsRowBinary` with a one-column table and a one-record batch (sentinel `SECRET-RECORD-VALUE-9f3` as the value, or as text inside a scripted exception), and prints every cause link. Then it wraps the result exactly as `Processing.doInsert` does (`new RuntimeException("qid", e)`, `Processing.java:72`, quoted: `throw new RuntimeException(queryId.toString(), e);//This way the queryId will propagate`) and feeds `Utils.handleException` for tolerance x retryOnSocket. "before" is the same chain without #857's layer. [VERIFIED: probe runs 2026-11-04, `/tmp/gsd45probe/probe857b-out.txt`]

| Failure shape | What #857 puts in the `DataException` message | Record value present? | Outcome before vs after (through the Processing wrap, all 4 tolerance x socket settings) |
|---|---|---|---|
| INVALID_UUID, UNPARSEABLE_INT128, UNKNOWN_ENUM, JSON_FLAG_OFF (fork already wraps in a value-free `DataException` with a `WithheldMessageException` cause) | `Failed to serialize record for topic `topicX`, partition `3`, offset `42`, column `c_uuid`: Failed to write column `c_uuid`: Kafka value of type STRING (value class: java.lang.String) could not be written as ClickHouse type UUID: java.lang.IllegalArgumentException` (column named twice) | no | unchanged |
| CLASS_CAST (fork's `WRITE_TYPE_MISMATCH_FAILURE_FORMAT` text) | same prefix + `Failed to write column `c_u64`: cannot convert Kafka value of type STRING (value class: java.lang.String) to ClickHouse type UINT64` | no | unchanged |
| IO_PLAIN, IO_READ_TIMEOUT_PREFIX, SOCKET_TIMEOUT, SOCKET_EXCEPTION, UNKNOWN_HOST (fork passes the IO chain through) | prefix + the IO exception's own message (`stream closed by test`, `Read timed out after 30000 ms`, `timed out`, `Connection reset`) | no | unchanged (RETRIABLE for the timeout and host shapes, RETRIABLE for SOCKET_EXCEPTION only with `retryOnSocketException=true`, else RUNTIME_WRAP / RETURNS) |
| NULL_IN_NON_NULLABLE (fork's exact `RuntimeException`, passed through) | prefix + `An attempt to write null into not nullable column 'c_i32'` | no | unchanged |
| JACKSON_OVER_SOCKET_TIMEOUT (the fork passes the Jackson exception through because a timeout is beneath it) | prefix + the Jackson message `reference chain: Map["SECRET-RECORD-VALUE-9f3"]` | **yes** | unchanged |

Where the sentinel appears for the Jackson shape: in the `DataException` message (after), in the printed trace (before AND after), in the pass-through exception's own message (before AND after). #857 adds one surface: its `LOGGER.error(message, cause)` writes the message, so the value reaches the ERROR log line, which the fork's baseline log line (`WRITE_VALUE_FAILURE_LOG_FORMAT`, exception class name only) never carried. The baseline pass-through already exposes this shape in the exception message and trace; that is the gg3 design (a classifier-preserving pass-through cannot be rewrapped with a withheld copy, because the copy's class is no longer `SocketTimeoutException`), so treat it as a recorded pre-existing limit, not an #857 regression, and word the pin as "no surface is added by the re-expression".

Two more measured facts:
- Fed a raw `DataException` directly (no Processing wrap), `handleException` classifies IO_READ_TIMEOUT_PREFIX, SOCKET_TIMEOUT, JACKSON_OVER_SOCKET_TIMEOUT and SOCKET_EXCEPTION (socket retry on) as DATA_EXCEPTION at tolerance `none` where they were RETRIABLE. That path does not occur through `ProxySinkTask.put` or `ChunkFlusher.putDirect`, because `Utils.handleException`'s `e instanceof DataException && !errorsTolerance` branch only fires for a top-level `DataException`. The re-expression must keep it that way; do not add a path that hands the writer's `DataException` straight to `handleException`. [VERIFIED: first probe output, plus `Utils.java` handleException, Read this session]
- The matrix expects `Outcome.DATA_EXCEPTION` for no case: `expected(...)` returns RETRIABLE or `errorsTolerance ? RETURNS : RUNTIME_WRAP`. [VERIFIED: `WriterFailureClassificationUnitTest.java:279-284` via sed]

**What fails today on the bare merge** (so the red points at #857): `WriterErrorPathLiveTest` WRT-10, quoted: `dataException.getMessage().startsWith("column 'at.a' (")` and `dataException.getMessage().endsWith(MISSING_VALUE_HINT)` (`WriterErrorPathLiveTest.java:397-400`). It walks the chain for the FIRST `DataException`; with #857 that is the outer wrapper, whose message begins `Failed to serialize record for topic ...`. Measured failure text: `... column `at`: column 'at.a' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT ==> expected: <true> but was: <false>`. Any design that adds an outer `DataException` fails this pin, which is why Option A (no outer layer) is the one that keeps "pass unchanged".

**Pin the plan must add (Docker-free, extends nothing, no Mockito inline mocks):** a new fork-owned unit test that calls `writeRecordsRowBinary` directly (it is package-private now; `CapturingClickHouseWriter` already reflects on the name) for each row of the table above and asserts: (1) the thrown exception is the same instance/class the fork's `doWriteColValue` produced (Option A), (2) no sentinel in any message, cause text or captured log line other than the documented Jackson limit, (3) the coordinate log line carries topic/partition/offset/column/class and no value, (4) the Processing-wrapped outcome equals the pre-#857 outcome for every shape x tolerance x socket setting. The existing `LogCapture.attach(ClickHouseWriter.class)` utility captures log events (used by the matrix).

### Upstream tests: measured status on the merged tree

| Upstream test | Result | Detail |
|---|---|---|
| `SlashColumnRoundTripTest` | 4 tests, 0 failures, live on ClickHouse `latest` (image `739e099abd11`), V1 and V2 | DESCRIBE keeps `a/b`, `a__b`, `café/温度`; `KeyToValue` + `ClickHouseSinkTask.put` inserts; typed read-back with `getDouble` equal. This satisfies the "DESCRIBE and insert, with typed read-back" part of UPS-03 for V2 on `latest`; the evidence file must still record `SELECT version()` |
| `FieldDescriptorEscapeTest` | 9 tests, 1 failure | `preservesEscapedCommentsAndDefaultExpressions`: `expected: <'a\\b'> but was: <'a\\\\b'>`. Upstream decodes every string field; the fork decodes only `name` and keeps `type`, `comment`, `default_expression`, `codec_expression`, `ttl_expression` as the server's literal JSON text. The 8 others pass (name decode of `a/b`, `a\\b`, `a"b`, newline, tab, `café/温度`; malformed JSON throws `JsonProcessingException`) |
| `ClickHouseWriterRecordContextTest` | does not compile (2 javac errors, `SinkTaskStatistics(int)`); after the 2-site `(null, 0)` patch both tests pass against #857 as merged | Under Option A the first test's `message.contains("topic `...`")` assertions go red (U-2) |

`fromJsonRow` differences to pin and name in FORK-DELTA (UPS-04), measured against upstream's one-liner: (1) only `name` is decoded; (2) `type` keeps its JSON escapes (the type parser, `DeclaredNullability.normalise` and `Column.extractEnumValues`, decode that layer later, which is what makes enum labels and Tuple element names round-trip); (3) `comment`, `default_expression`, `codec_expression`, `ttl_expression` also stay literal; none of these four is read anywhere in `src/main` (`grep getComment|getDefaultExpression|getCodecExpression|getTtlExpression` finds no use), so the difference is observable only through the getters; (4) a malformed row throws `JsonProcessingException` whose message never embeds the row (the fork's contract, kept). The NAME pins pass unchanged: the existing FORK-DELTA row already says "`fromJsonRow` decodes only the `name` of a `DESCRIBE` row and keeps every other string ... as the server's literal JSON text". The #839 behaviour (a slash name matched) holds because `\/` is decoded by the same `name` path.

### Pattern: compile gates that catch what conflict markers do not

After M, run `compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava`, a cache-busted `testUnit` with Docker unreachable, and the full suite. Expected red set after M, exactly: 2 javac errors in one file; then 1 `testUnit` red (`FieldDescriptorEscapeTest`); then 1 live red (WRT-10). Anything else red is a new finding. BLD-16, the pin guard classes and the workflow guard classes pass on the merged tree (they are inside the 1837 `testUnit` tests, 1 failed). [VERIFIED: rehearsal runs]

### Anti-Patterns to Avoid
- **`git merge -X ours` as the real merge.** It is fine for rehearsal, but it keeps the dead `reader` resource (an auto-merged hunk) and would move two anchors. Use per-hunk resolution and drop that hunk.
- **Trusting the 216-case matrix as the #857 gate.** It never executes `writeRecordsRowBinary`.
- **Probing `handleException` with a raw writer `DataException`.** Production wraps it first.
- **Adding an upstream-compatible `SinkTaskStatistics(int)` constructor to main.** Phase 34 anchored `new SinkTaskStatistics(id)` at 0 for `ProxySinkTask`; fix the test instead.
- **Disabling or deleting an upstream test in place.** AGENTS.md: only add tests, never delete or weaken. Prefer a fork-owned replacement beside an untouched original, or a named edit.
- **Embedding `cause.getMessage()` anywhere new in `ClickHouseWriter`.** STATE.md's standing rule: "A new writer message must not embed a record value."

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Conflict-set and provenance checking | A new script | `verify-merge-invariants.sh` (unedited) and `git merge-tree --write-tree --name-only` | Editing the script voids the "unedited gate" criterion |
| The per-path checklist | Hand-transcribed tables | Generate Section 1 from `conflicted-paths-v1.7.0-tail-3.txt` and Section 2 from `git diff --name-status 9bed6f7970670a55c66be8efda246889e1f95bd0 83808df9a0993cf49b2ceaa4633c782e550c0180` (11 paths: 8 modified, 3 added), per the tail-2 checklist's rule 1; add the auto-merged-hunk rows by `git diff` of the merge result against each parent | The roadmap says "generated"; the tail-2 file's format is the template (Sections 1-3, Gate commands, Recorded Facts) |
| Advisory lookup | A scraper | OSV.dev `POST https://api.osv.dev/v1/query` and `gh api "advisories?ecosystem=maven&affects=<g:a>@<v>"` | Both verified working; neither needs Dependabot alerts (`gh api repos/ne1r0n/clickhouse-kafka-connect/dependabot/alerts` returns `403 Dependabot alerts are disabled for this repository`) |
| Latest-version lookup | Manual browsing | `./gradlew dependencyUpdates` (plugin `com.github.ben-manes.versions` 0.66.0 is wired at `build.gradle.kts:31-37`, stable-only) for declared deps; `maven-metadata.xml` `<release>` for transitive ones | The plugin lists declared coordinates only (it did not list `commons-io` or `commons-codec`) |
| Test tally | Console grep | Sum the JUnit XML under `build/test-results/test` and `testUnit` | Gradle's console summary omits parameterized-case skips |

## Common Pitfalls

### Pitfall 1: The mechanical merge leaves a dead resource and a moved anchor
**What goes wrong:** The merge compiles and tests green, yet the gate's invariant 3 fails on M for two HLP-04 rows.
**Why:** one non-conflicting hunk of #839 applies to a method the fork already rewrote.
**How to avoid:** resolve that hunk to the fork side in M and list it in the checklist as an auto-merged hunk with that resolution. **Warning sign:** `ANCHOR VIOLATION: <sha> .../ClickHouseHelperClient.java 'StandardCharsets.UTF_8' expected 1 got 2 (HLP-04)`.

### Pitfall 2: A green matrix that does not test the change
**What goes wrong:** the 216-case matrix, `testUnit` and the Docker-free gates are all green while #857's wrap is live; only the live WRT-10 catches it, and only in `test`.
**How to avoid:** the new `writeRecordsRowBinary` pin (above), run in `testUnit`.

### Pitfall 3: A probe artifact that looks like a retry-classification regression
**What goes wrong:** `Utils.handleException(thrownDataException, false, ...)` shows RETRIABLE becoming DATA_EXCEPTION for timeout shapes.
**Why:** that call skips `Processing.doInsert`'s `RuntimeException(queryId, e)` wrap, which production always applies.
**How to avoid:** any classification pin goes through `ProxySinkTask.put`/`ChunkFlusher.putDirect` or applies the same wrap. Do not claim a classification change without the wrap.

### Pitfall 4: Upstream's `SinkTaskStatistics(0)` and the first-build network need
`new SinkTaskStatistics(0)` cannot compile (fork constructor is `(String connectorName, int taskId)`, `SinkTaskStatistics.java:34`); `connect-json` is not cached, so a first offline build fails with the "No cached version" message. Expect and record both.

### Pitfall 5: Count baselines in the roadmap are stale
`WriterFailureClassificationUnitTest` is 216 tests in both HEAD's last run (`build/test-results/test`, 2026-11-03, `tests="216" skipped="0" failures="0"`) and the rehearsal; 184 was Phase 22's number. Record the real count before and after, and say 216 in the evidence. Full-suite baseline for the rehearsal: 2245 tests, 19 skipped (HEAD's own total was not re-run this session; record it from a clean run before the merge so the equality is real, not inferred).

### Pitfall 6: `dependencyUpdates` is slow and blocks other Gradle runs
In this session it ran past 8 minutes while other invocations ran; run it in the background (or use `maven-metadata.xml` for the shortlist) and do not run a second copy.

### Pitfall 7: Operational traps from memory notes
- `TESTCONTAINERS_HOST_OVERRIDE` is the literal `"null"` in this shell: run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.
- zsh expands `$M:path` as a history modifier: write `${M}:path`; no `timeout` binary on macOS.
- `./gradlew test` exit 255 is an intermittent OpenJ9 GPF in Mockito inline mocking: delete the `core.*.dmp` (about 8 GB), `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one (never `git clean`), re-run once and record both runs; `-PjavaVersion=21` is the escape hatch. No crash occurred in the rehearsal's full run.
- Two full runs must use `--rerun` (a bare second `./gradlew test` echoes the first).
- `latest` drifts: a red full suite on unchanged code means check the image date first. Local `clickhouse/clickhouse-server:latest` is `739e099abd11`, created 9 days before this session, the same id prefix as Phase 33/34's recorded digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`. Record `SELECT version()` in the evidence.
- Do not push anything and do not push `pre-v1.7.0_gg8`.

## Code Examples

### Re-measure and record immediately before the merge
```bash
git ls-remote upstream main
git ls-remote --tags upstream | sed 's#.*refs/tags/##' | sort -V | tail -5
git ls-remote --tags upstream 'v1.7*'
git fetch upstream --tags
git merge-tree --write-tree --name-only HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180 > /tmp/mt45.txt 2>&1; echo "exit=$?"   # expect exit=1
awk 'NR==1{next} /^$/{exit} {print}' /tmp/mt45.txt | LC_ALL=C sort -u                                                       # the 6 paths
```
Source: this session; the same derivation the gate uses (`verify-merge-invariants.sh` lines 105-106).

### Rehearse in a throwaway clone (what produced this research)
```bash
mkdir -p /tmp/gsd45probe && cd /tmp/gsd45probe && git clone -q --local <repo> repo && cd repo
git checkout -q <HEAD> && git fetch -q <repo> 83808df9a0993cf49b2ceaa4633c782e550c0180
git merge --no-commit --no-ff -X ours 83808df9a0993cf49b2ceaa4633c782e550c0180
```
`-X ours` is for rehearsal only; the real merge resolves per hunk and drops the dead `reader` resource.

### Anchors mirror (same semantics as gate invariant 3, working-tree version)
```bash
bad=0; rows=0
while IFS=$'\t' read -r p pat cnt id; do
  case "$p" in ''|'#'*) continue;; esac
  rows=$((rows+1)); got=$(grep -cF -e "$pat" "$p" 2>/dev/null)
  [ "${got:-0}" = "$cnt" ] || { echo "ANCHOR MOVED $p [$pat] expected $cnt got ${got:-0}"; bad=$((bad+1)); }
done < .planning/scripts/fork-anchors.txt
echo "rows=$rows bad=$bad"
```
Candidate new anchor rows (measure counts on the real tip before committing; each lands in the same commit as its code): a `0` row for `BufferedReader reader = new BufferedReader(new InputStreamReader(response.getInputStream(` in `ClickHouseHelperClient.java` (the dropped dead resource); the `writeColumnWithRecordContext(` seam in `ClickHouseWriter.java`; `testImplementation(libs.kafka.connect.json)` in `build.gradle.kts` (1) and the `kafka-connect-json = { module = ...` line in `libs.versions.toml` (1). Existing rows that must keep their counts: `ClickHouseHelperClient.java StandardCharsets.UTF_8 1 HLP-04`, `InputStreamReader 2 HLP-04`, `readDescribeRows( 3 NAME-03`, `ClickHouseFieldDescriptor.java currentTokenLocation() 1 NAME-03`, `json.replace( 0 NAME-03`.

### UPS-05 and DEP evidence commands
```bash
./gradlew -q dependencyInsight --configuration integrationTestRuntimeClasspath --dependency slf4j-simple   # 2.0.20
./gradlew -q dependencyInsight --configuration testRuntimeClasspath --dependency commons-lang3             # 3.21.0
./gradlew -q dependencyInsight --configuration testFixturesRuntimeClasspath --dependency commons-lang3     # 3.21.0
./gradlew -q buildEnvironment | grep -i spotless                                                           # 8.10.4
./gradlew -q dependencyInsight --configuration testRuntimeClasspath --dependency connect-json              # 4.4.0, root project only
./gradlew -q dependencies --configuration clickhouseDependencies                                           # the shipped set
curl -s -X POST https://api.osv.dev/v1/query -d '{"package":{"name":"<g:a>","ecosystem":"Maven"},"version":"<v>"}'
gh api "advisories?ecosystem=maven&affects=<g:a>@<v>&per_page=5" --jq '.[].ghsa_id'
```

## DEP-01 scope and sources

**Scope recommendation (U-5):** DEP-01 says "every runtime and test dependency". Put the shipped set first, because only it reaches production workers.
1. **Shipped runtime** = the `clickhouseDependencies` configuration (declared at `build.gradle.kts:112-121`: httpclient5, clickhouse-client, client-v2, lz4-java, clickhouse-http-client, guava with the comment `// temp to override client's with CVE`, gson, jackson-core, jackson-databind, jackson-annotations) and its transitives. Rehearsal tree resolves about 30 artifacts: `httpcore5`/`httpcore5-h2` 5.4.4, `commons-codec` 1.19.0, `commons-io` 2.20.0, `commons-compress` 1.28.0, `commons-lang3` 3.20.0 (the client's pin, `3.18.0 -> 3.20.0` by conflict resolution), `RoaringBitmap` 1.0.6, `zstd-jni` 1.5.7-23, `asm` 9.7, `slf4j-api` 2.0.7, `failureaccess` 1.0.3, `jspecify` 1.0.1, `error_prone_annotations` 2.50.0, `j2objc-annotations` 3.1, plus the declared ones. Shipped `commons-lang3` is 3.20.0 while `libs.versions.toml` says 3.21.0: the catalog line governs test fixtures only. UPS-05's wording "commons-lang3 3.21.0 resolves on its classpath" is true for `testRuntimeClasspath` and `testFixturesRuntimeClasspath`, not `runtimeClasspath`; name the configuration in the evidence.
2. Test and test-fixture deps (`testRuntimeClasspath`, `testFixturesRuntimeClasspath`), integration-test deps (`integrationTestRuntimeClasspath`).
3. Gradle plugins (spotless, shadow, protobuf plugin, versions, jacoco) via `buildEnvironment`; the Gradle wrapper (`gradle-9.8.2-bin.zip`); the JDK toolchain (17).
4. Workflow actions: the fork's SHA-pinned actions (`actions/checkout` v7.0.1, `actions/upload-artifact` v7.0.2, `actions/setup-java` v6.1.0, `gradle/actions/setup-gradle` v6.4.1) and the inherited tag-pinned ones in upstream-owned workflows (`actions/checkout@v3`, `actions/setup-java@v3`, `gradle/gradle-build-action@v2`, `actions/download-artifact@v4`).
5. Container images used by tests (ClickHouse, Confluent worker) are listed as "not Maven", with versions, no advisory feed.

**Where advisories are read (Dependabot alerts are disabled):** [VERIFIED: both sources queried this session]
- OSV.dev: `curl -X POST https://api.osv.dev/v1/query` with Maven coordinates and an exact version; control `jackson-databind@2.9.0` returned 74 advisories, so the channel works.
- GitHub global advisory database through the signed-in `gh` (account `ne1r0n`): `gh api "advisories?ecosystem=maven&affects=com.fasterxml.jackson.core:jackson-databind@2.9.0"` returned advisories; `ecosystem=actions` also works.
- Not usable: the repository's own Dependabot alerts (HTTP 403 "Dependabot alerts are disabled for this repository").
Record each query result with its date, since both databases change daily.

**Pre-measurement (2026-11-04; the plan's recorded DEP-01 run supersedes it):** OSV returned **0 advisories** for every coordinate checked: the 12 shipped declared artifacts, the 12 shipped transitives listed above, `commons-lang3` 3.20.0 and 3.21.0, `slf4j-simple`/`slf4j-reload4j` 2.0.20, `connect-api`/`connect-json` 4.4.0, `lombok`, `okhttp`, `testcontainers`, `junit-jupiter`, `mockito-core`, `hamcrest`, `org.json`, `kafka-schema-registry-client`, `protobuf-java` 3.25.9, and the spotless, shadow and versions plugins. GitHub advisories for `actions/checkout`, `upload-artifact`, `setup-java` and `gradle/actions` at the pinned versions: 0; `gradle/gradle-build-action@2.0.0` (inherited, tag `v2`) returned 1 advisory for that old version (the plan's run must read the exact resolved version of the `v2` tag, which is unpinned). Caveat: OSV version matching on Maven is by exact version string; a clean answer for a coordinate means no recorded advisory affects that exact version, not that the artifact is audited.

**Latest versions** (`maven-metadata.xml` `<release>` and `dependencyUpdates`, 2026-11-04): up to date: clickhouse 0.11.0, jackson 2.22.4 (annotations 2.22), gson 2.15.0, lz4-java 1.12.1, commons-lang3 3.21.0, commons-compress 1.28.0, connect-api and connect-json 4.4.0, lombok, testcontainers, mockito, okhttp, org.json, shadow 9.6.1, protobuf plugin 0.10.0. Newer stable: guava 33.7.2-jre to 33.8.0-jre; commons-io 2.20.0 to 2.22.0 and commons-codec 1.19.0 to 1.23.0 (transitive, shipped); spotless 8.10.4 to 8.11.0; versions plugin 0.66.0 to 0.67.0; jacoco 0.8.15 to 0.8.16; Confluent 7.9.1 to 7.9.10 (patch) or 8.4.0 (major); junit-bom 5.14.4 to 6.1.3 (major); protobuf-java and protoc 3.25.9 to 4.36.2 (major, and `libprotobuf` is pinned deliberately); Gradle 9.8.2 to 9.9.0-rc-1 (release candidate only). Pre-releases not proposed: httpclient5 `5.7-beta1`, slf4j `2.1.0-alpha1`. [VERIFIED: `repo1.maven.org` metadata and the plugin's `build/dependencyUpdates/report.txt`]

**DEP-02 mechanics:** a shipped-set bump changes the Confluent archive, so it also changes Phase 49's `confluent-archive-allowlist-v1.7.0_gg8.txt` rows (gson's 2.14.0 to 2.15.0 bump needed three allow-list rows in gg7). One commit per approved bump, `testUnit` green with `DOCKER_HOST=unix:///nonexistent.sock`-style unreachable Docker, full suite green on `latest` x V2, anchors and the invariant gate re-run. If the user approves none, the record says so and nothing moves. This research proposes no bump.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Upstream `fromJsonRow` doubles every backslash then reads (gg4 retired this) | Upstream (#839): plain `readValue(json, ...)` | `83808df9` | Decodes all fields; the fork keeps its Phase 28 design and names the difference |
| Upstream wrote a serialization failure as the bare cause | Upstream (#857): `Failed to serialize record for topic, partition, offset, column: <cause message>` | `83808df9` | Operator-friendly, but embeds `cause.getMessage()`; the fork keeps coordinates out of messages |
| `describeTable` read `response.records()` | Upstream reads JSONEachRow lines as UTF-8 | `83808df9` | Fork already has `readDescribeRows`; the fork's shape wins |

**Deprecated/outdated:** the roadmap's "184-case" figure (now 216); the roadmap's "taken as it is, it changes ... the retry classification" (not borne out on the production path).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Option A (log-only coordinates, fork exception untouched) is what the user will choose for U-1 | Decisions U-1 | Low: the plan lists the options; a different choice changes the re-expression commit and which pins stay unchanged |
| A2 | OSV exact-version "0" results are sufficient for the DEP-01 headline | DEP-01 | Medium: a transitive or shaded jar the Maven resolution does not list is not covered; the recorded run should also read the shipped archive's jar list once |
| A3 | `gradle/gradle-build-action@v2` and the other tag-pinned actions live only in upstream-inherited, disabled workflows and are out of the shipped set | DEP-01 scope | Low: the plan can re-check which workflows call them |
| A4 | The 216-test class count at HEAD equals the rehearsal's because the merge does not touch that file | Pitfall 5 | None: verified by the 2026-11-03 XML (`tests="216"`) and `git diff --quiet HEAD 83808df9 -- <file>` printing nothing |
| A5 | A second full run on the real tip will equal the first (no flaky live test) | Gates | Medium: `latest` drifts; one full run in the rehearsal passed apart from the 2 known reds |

## Open Questions

1. **How is the live-suite baseline obtained before the merge?**
   - Known: rehearsal full run is 2245 tests, 19 skipped; `testUnit` 1837, 2 skipped (with the 2-site patch).
   - Unclear: the pre-merge totals on HEAD in the same environment (not re-run, to avoid editing nothing and spending 3 minutes twice).
   - Recommendation: run `testUnit` and one `test --rerun` on `pre-v1.7.0_gg8` as plan task 0, so "the tail adds N tests" is measured.
2. **Should the baseline Jackson-over-timeout exposure (pre-existing) be closed?**
   - Known: the exception passes through by gg3 design; its message can quote a Map key; it exists before and after #857.
   - Unclear: whether SC-2's "no error message, DLQ header or log line carries a record value" is meant to include it.
   - Recommendation: record it as a limit with the measurement, pin "the re-expression adds no surface", and let the user say if the stricter reading applies (closing it would change classification-relevant pass-through, which is a main-code change outside this phase's scope, so it stops for a user decision per STATE.md).
3. **Does the user want the live WRT-10 pin to stay the only unchanged live pin, or add a live variant for the new coordinate log line?** Recommendation: Docker-free only; the log line is writer-local.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| git, `merge-tree --write-tree` | merge, gate | yes | system git | — |
| Network to `github.com/ClickHouse` (upstream) and Maven Central | pre-flight, first build (`connect-json`), advisories | yes | `ls-remote` and Gradle resolution succeeded | — |
| SSH to `origin` | not needed (nothing pushed) | flaky | one `Permission denied (publickey)` this session | none needed |
| Docker (colima profile `vz`) | full suite x2, live pins | yes | `colima list`: `vz` Running (8 CPU, 16 GiB); `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` | `testUnit` runs with Docker unreachable (verified with `DOCKER_HOST=unix:///nonexistent.sock`) |
| `clickhouse/clickhouse-server:latest` | live pins, full suite | yes (local) | image `739e099abd11`; also local `26.3`, `25.8` | — |
| JDK | Gradle toolchain 17 | yes | default `java` is OpenJDK 21.0.12.1; toolchain resolved a Semeru 17.0.9 (OpenJ9) JVM for tests | `-PjavaVersion=21` |
| Gradle wrapper | build | yes | 9.8.2 | — |
| `gh` | advisory read | yes | signed in as `ne1r0n`; Dependabot alerts disabled for the repo | OSV.dev |
| OSV.dev | DEP-01 | yes | answered with and without results | `gh api advisories` |

**Missing dependencies with no fallback:** none.

## Validation Architecture

Skipped: `.planning/config.json` has `"nyquist_validation": false` [VERIFIED: file read this session]. The phase gate, which the roadmap defines, is: cache-busted `testUnit` with Docker unreachable; two `--rerun` full suites on `latest` x V2 with `SELECT version()` recorded; the invariant script once on an explicit SHA.

## Security Domain

`security_enforcement` is enabled (`"security_enforcement": true`, `"security_asvs_level": 1`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V5 Input Validation | yes (DESCRIBE JSON rows parsed by `fromJsonRow`) | Jackson with a fork contract that a malformed row throws `JsonProcessingException` whose message never embeds the row (upstream's `rejectsMalformedJson` and the fork's own pins) |
| V7 Error Handling and Logging | **yes, central** | Writer failure text is value-free: class names and column/type metadata only; `WithheldMessageException` for causes; no `cause.getMessage()` in new text or logs |
| V10 Malicious Code / V14 Configuration (supply chain) | yes | Maven coordinates taken from upstream's diff; OSV and GitHub advisory reads recorded with dates; no bump without the user's approval |
| V2, V3, V4, V6 | no | no auth, session, access-control or crypto change in this phase |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Record value disclosed through an exception message, DLQ header or log line | Information disclosure | Option A re-expression; new Docker-free pin on `writeRecordsRowBinary`; the Jackson-over-timeout shape recorded as a pre-existing limit |
| Vulnerable shipped dependency | Elevation / Tampering | DEP-01 recorded check on the resolved `clickhouseDependencies` tree; user-approved bumps only |
| Slopsquatted or look-alike coordinate | Tampering | Only `connect-json`, from the same Apache group and version ref as `connect-api`; Maven Central metadata checked |
| Merge silently carries an unreviewed hunk | Tampering | Per-hunk checklist including auto-merged hunks of fork-touched files; unedited provenance gate |

## Project Constraints (from CLAUDE.md)

- AGENTS.md: do not change dependencies or upgrade for CVE without asking the user first; only add tests, never delete or weaken; Spotless/Google Java Style (the repo has 46 pre-existing violating files, so check per touched file); bump `VERSION` and update `CHANGELOG.md` in the release phase (49) unless a plan says otherwise; no Mockito inline mocks (OpenJ9 crash); write read-back asserts; "Do NOT make commits to the branch without explicitly asking the user" applies to review work, while this phase's commits are the GSD workflow's own (the roadmap states this phase pushes nothing).
- `.claude/CLAUDE.md`: behaviour preservation (code changes limited to genuine bug fixes; the fork runs in production); tests target ClickHouse `latest` with client V2; rebasability (additive changes, small targeted fixes); key high-risk files include `ClickHouseWriter.java`, so the re-expression needs thorough tests. GSD workflow enforcement: file edits go through a GSD command.
- STATE.md standing rules that bind this phase: writer failure text stays value-free (`IOException` chain, `ConnectException` and exact `RuntimeException` pass through unchanged); publish one SHA on one branch; a main-code change outside a phase's declared scope stops for a user decision; two-run gates use `--rerun`; any new reader of ClickHouse output names UTF-8 explicitly.

## Sources

### Primary (HIGH confidence)
- This session's commands: `git ls-remote upstream`, `git merge-tree`, `git diff 9bed6f79 83808df9`, the rehearsal clone `/tmp/gsd45probe/repo` (merge, gate run, `compileJava`/`compileTestJava`, `testUnit`, live tests, full suite, `dependencyInsight`, `buildEnvironment`, `dependencies --configuration clickhouseDependencies`, `dependencyUpdates`)
- Files read this session: `45-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md` (lines 255-280, 366-397), roadmap Phase 45 section, `verify-merge-invariants.sh`, `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md`, Phase 34's `34-RESEARCH.md`, `gradle/libs.versions.toml` (1-40), `build.gradle.kts` (84-133), `WriterErrorPathLiveTest.java` (385-403), `Processing.java` (66-74); upstream sources via `git show 83808df9:...`
- OSV.dev API and the GitHub advisory REST API (queried with a control that returned results)
- `repo1.maven.org/maven2/.../maven-metadata.xml`

### Secondary (MEDIUM confidence)
- `docs/FORK-DELTA.md` rows 52, 57, 60, 111, 234 (existing fork contract text for `fromJsonRow`, `readDescribeRows`)

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, resolved by `dependencyInsight` on the rehearsal tree
- Architecture / merge recipe: HIGH, rehearsed with the real gate
- #857 behaviour: HIGH for the 9 measured shapes through the real Processing wrap; MEDIUM for shapes not in the table (the writer has more failure kinds; the new pin should enumerate the matrix's 27 kinds)
- DEP-01 pre-measurement: MEDIUM (OSV exact-version semantics; the recorded run supersedes it)
- Pitfalls: HIGH

**Research date:** 2026-11-04
**Valid until:** 2026-11-11 (upstream `main` and `latest` move; re-run the STOP check and the merge-tree immediately before the merge)
