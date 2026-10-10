# Phase 15: Suite Gate, Publication & Rollout - Research

**Researched:** 2026-10-10
**Domain:** release engineering for a public Kafka Connect sink fork (git publication, GitHub Actions evidence, release documents) plus two small `src/main` changes (Debezium timestamp encoding, a startup WARN)
**Confidence:** HIGH for repo-derived facts and live ClickHouse probes; MEDIUM for GitHub-side behaviour (documented, and exercised once in Phase 11)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Carried forward (locked earlier; not re-opened)**
- **Phase 11 D-01:** `custom_build` moves only by a forced `--no-ff` merge, so a single `git revert -m 1` undoes the release. Local and remote `custom_build` are both `c61d90ce`, an ancestor of `sync/v1.7.0`. A fast-forward would therefore be possible, and `--no-ff` must be explicit.
- **Phase 11 D-02 and Phase 14 D-11:** CI-08's evidence is the run on the published SHA, located by `head_sha` plus workflow path (`.github/workflows/tests.yaml`), with exactly one completed run and no cancelled sibling.
- **Phase 11 D-03:** there is one blocking human gate, immediately before the `custom_build` push. `gsd-executor` refuses approvals relayed by the orchestrator, so the orchestrator runs every post-gate task inline.
- **Phase 11 D-04:** push the branch and the named tags explicitly. Never use `git push --tags`: the local repo holds upstream's `v0.0.x`/`v1.x` tags, and they must not reach `origin` (PUB-04). Delete `origin/sync/v1.7.0` once it is merged.
- **Phase 11 D-10:** the CI duration is measured against the 22.5-minute threshold, half of `timeout-minutes: 45`.
- **Phase 11 D-11:** a red CI run is attributed before anything is touched (re-run locally on `latest` first). There are no blind re-runs.
- **Phase 14 D-10:** push the published SHA on **one branch only**. Two branches at one SHA share a concurrency group and cancel each other.
- **Inherited rules:** existing tests are never deleted or weakened. Type coverage asserts typed SQL read-back. Gradle is always invoked as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. `spotlessApply` runs only on touched files. `verify-merge-invariants.sh` runs at the end of the phase. A commit that changes a `fork-anchors.txt` anchor updates that file in the same commit.
- **Already true for PUB-04:** the 22 post-`c61d90ce` planning commits from local `sync/v1.6.0` are ancestors of `sync/v1.7.0`, so the `--no-ff` merge publishes them. `.planning/.gsd-ws-arg` is not tracked; re-check both facts at the gate.

**Open review findings vs. publication**
- **D-01: Fix 13-REVIEW WR-02 before the gate.** `MetadataFreeJsonWriter.writeLogicalValue` (around lines 186–226) gains two cases, using the existing decimal-seconds encoding:
  - `io.debezium.time.Timestamp`: epoch milliseconds, three fractional digits.
  - `io.debezium.time.NanoTimestamp`: epoch nanoseconds, nine fractional digits.

  Today a raw millisecond long is stored as seconds, silently, while RowBinary stores it correctly.
  - The pins that currently assert pass-through change with the code: `MetadataFreeJsonWriterTest#cdcMicroTimestampConvertsAndOtherDebeziumTimesPassThrough` and the live `MetadataFreeJsonTypeParityLiveTest`. They flip to the correct value, with typed read-back parity against RowBinary into `DateTime64(3)`/`DateTime64(9)`. This is a tightening, not a weakening, and the rationale is recorded next to each changed assertion. A seen-RED mutation check proves the new pin is not vacuous.
  - `ZonedTimestamp` strings stay pass-through (out of the finding). Research checks whether they parse under the server's default `date_time_input_format`, for documentation only.
  - Record it as behaviour ledger entry **L-20**. It goes into CHANGELOG, the FORK-DELTA metadata-free row, and the JSON-route type notes in the rollout doc.
  - **Reversibility:** reversible in code, but it changes the bytes sent for those fields, which D-07's exactly-once pre-upgrade check covers.
- **D-02: WR-05 is documented and paired with a startup WARN. The root cause is not fixed** (backlog 999.1 stays).
  - When table metadata is discovered, a column whose raw ClickHouse type contains `Nullable(Enum8|Enum16)` or `Nullable(Tuple(...))` inside an `Array`/`Map` gets one WARN, naming the database, table, column and raw type. The WARN says that a null entry in that slot fails the whole batch with `DataException`. Top-level `Nullable(Tuple)` is included if research shows it is affected the same way.
  - Detection must work from the raw type string, because `Column.extractColumn` drops exactly the nullability being detected.
  - It must not log per record, and must not repeat on every `TableMappingRefresher` cycle: once per table per process, or once per changed description. The planner picks the mechanism.
  - It logs metadata only, never data. No writer-path change.
  - Not reachable in metadata-free mode, where no DESCRIBE runs. That is acceptable, because the RowBinary guard is what throws.
  - Pinned by a test, and recorded as ledger entry **L-21**. It goes into FORK-DELTA Known costs (pointing at 999.1) and the rollout doc's self-check.
- **D-03: Publish every other open finding as open, and record a disposition.**
  - Covers 13-REVIEW WR-01, WR-03 and WR-04 (upstream routes or upstream lines in a high-risk file, so prefer-upstream applies), 14-REVIEW WR-01 and WR-02 (test-only and CI-only), and all info items in both reviews.
  - Set each row in `13-REVIEW-DISPOSITION.md` and `14-REVIEW-DISPOSITION.md` to `deferred` with a one-line reason in the Source cell. 13-REVIEW WR-02 becomes `fixed` (D-01), and WR-05 becomes `deferred` with the reason "WARN added (D-02); root cause backlog 999.1".
  - Findings an operator can observe go into FORK-DELTA "Known costs and open items": 13 WR-01's all-defaults rows on the `bypassRowBinary=true` route, and 13 WR-03's message-less exception.
  - The full list goes into the approval bundle.
- **D-04: Main-code scope carve-out.** Phase 15's `src/main` changes are limited to the D-01 and D-02 commits, plus fixes that the D-10 code review of those commits forces.
  - Expected files: `MetadataFreeJsonWriter.java` for D-01. For D-02, one metadata-discovery site: `ClickHouseHelperClient` describe or `ClickHouseWriter` mapping. The planner picks.
  - Any other `src/main` change stops for a user decision, as Phase 11 D-17 did.
  - The `VERSION` bump touches no `src/main` file, but changes the generated `Version.java`.

**Rollout checklist and release documents**
- **D-05: The checklist lives at `docs/ROLLOUT-v1.7.0_gg1.md`**, a per-release file next to `FORK-DELTA.md`. The next sync adds a new file rather than rewriting this one. The CHANGELOG `1.7.0_gg1` section and FORK-DELTA both link to it.
- **D-06: It is generic, with a self-check step per item.** It names no internal hostnames, connector names or prod versions, because the fork is public. Each item says what changed and gives a concrete check an operator can run against their own setup. Items, all required:
  - **JMX MBean name, before and after.** The `connector=<name>` segment and the per-release `version=` segment change from `v1.6.0_gg1` to `v1.7.0_gg1`. Check: find dashboards or alerts that match on the full MBean name.
  - **Compression.** Semantics of `setCompressionMethod` (default `true`) and the unconditional `compress=false`. The caveat that `compress=true` in `jdbcConnectionProperties` re-enables response compression on client V2. The server-profile check of `network_compression_method`. Source: the FORK-DELTA compression row and Recorded Facts 25 and 26.
  - **R-01 behaviour change.** Explicit `insertFormat=json` with `Struct`/CDC records now lands rows. Includes the backfill procedure copied as written from `13-BEHAVIOUR-LEDGER.md` §"R-01 backfill procedure" (offset reset plus KeeperMap state clear). Check: grep connector configs for `insertFormat=json`.
  - **JSON-route type notes.** L-02 to L-05, L-08 (including the `input_format_null_as_default` caveat) and L-20 (D-01).
  - **Nested-null `DataException`** (L-10, L-11, L-13). DLQ-able under `errors.tolerance=all`, task-stopping under `none`. Includes the WR-05 limitation and its WARN (D-02, L-21). Check: query `system.columns` for `Array`/`Map` types containing `Nullable(Enum` or `Nullable(Tuple`.
  - **The TYPE-08 Int256/UInt256 message change** (L-15) and the `Array(Tuple)` missing-field failure (L-17).
  - **The exactly-once pre-upgrade check** (D-07).
  - **Rollback.** Tag `pre-v1.7.0-sync` (`c61d90ce`), `git revert -m 1 <merge>` on `custom_build`, redeploying the `v1.6.0_gg1` artifact, and a note that rolling back re-introduces R-01's silent loss.
- **D-07: The rollout doc includes an exactly-once pre-upgrade check.** It goes beyond ROLL-01's R-01 backfill.
  - Under `exactlyOnce=true`, a batch left in `BEFORE_PROCESSING` by the old jar is retried by the new jar. If the new jar encodes it differently, ClickHouse block dedup does not match it, and the rows can be written twice.
  - Procedure: stop or pause the connector cleanly. Query the KeeperMap state table for any topic-partition not in `AFTER_PROCESSING`. Upgrade only when none remain.
  - **Research must establish which encodings actually changed** between `v1.6.0_gg1` and `v1.7.0_gg1` on routes that were already writing rows, so the doc can say when the check is required and when it is precautionary. Candidates: upstream #820 null-optional-Map bytes, the D-09/L-09 nested empty-container coercion, R-01 routing (old route wrote no rows), D-01's Timestamp encoding, and compression (believed request-neutral; confirm). It must also give the exact KeeperMap query, using the state table/`zkPath` naming from `KeeperStateProvider`.
- **D-08: CHANGELOG gets a `1.7.0_gg1` section, in upstream style.**
  - The heading is `# 1.7.0_gg1, <publish date>`, at the top of `CHANGELOG.md`, using upstream's headings (Important Changes / Bug Fixes / Dependencies).
  - It lists only the fork's own changes since `v1.6.0_gg1`. It states that it builds on upstream `1.7.0` (the section below it) and links FORK-DELTA and the rollout doc. Upstream's sections are left untouched, and no `1.6.0_gg1` section is backfilled.
  - Content source: `13-BEHAVIOUR-LEDGER.md` §"Phase 15 inputs by destination", plus L-20 and L-21. It must name R-01, WR-02 strictness (L-10), compression (D-17 retired onto `setCompressionMethod`) and lz4 (`1.12.0`). The dependency bumps (Jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0) go under Dependencies.
- **FORK-DELTA (PUB-02)** follows the ledger's destination list and adds the following. It does not need new decisions.
  - D-17 retired (already partly there; verify it).
  - R-01 removed from Known costs, and Known costs item 2 closed (L-19).
  - The LZ4 rule and version 1.12.0.
  - The MAP guard scoped to Nullable.
  - The P9-IN-01 wording: "evaluation order", not "null-safety".
  - Known JSON-route gaps, updated: L-05 shrinks because of D-01.
  - The nested-null split, and 999.1 pointers.
  - D-02's WARN, D-03's operator-visible open items, and the named skip inventory re-checked against the gate runs.
  - Every entry is grep-confirmed against the tree (Phase 11 D-15).

**Pre-approval audits and sequencing**
- **D-09: Run `/gsd-secure-phase 13` and `/gsd-secure-phase 14` before the approval gate.**
  - Phase 13: `MetadataFreeJsonWriter` serializes untrusted record bytes into a JSON body (escaping, raw-byte strings, data-free error messages).
  - Phase 14: the `tests.yaml` trigger change, `$GITHUB_STEP_SUMMARY`/annotation writes, and the dependency bumps.
  - An open high-severity threat is a **blocking checkpoint**, not automatic scope (`security_block_on: high`). Any fix it forces lands before the gate, under D-04's stop rule.
- **D-10: Order is code, then review, then docs and VERSION, then the gate.**
  1. Land the D-01 and D-02 commits.
  2. Code-review just those commits and resolve the findings. Fixes come before anything else.
  3. Write `VERSION=v1.7.0_gg1`, CHANGELOG, FORK-DELTA, the rollout doc, the ledger entries L-20/L-21 and the disposition updates.
  4. Run the secure audits (D-09). They can run alongside step 3.
  5. Only then run the gate on the exact tree that will be published:
     - Run 1: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean test`.
     - Run 2: `... ./gradlew test --rerun`.
     - Both need 0 failures, 0 errors, and a skip set equal by (class, method) to FORK-DELTA's named skips.
     - Assert that the generated `Version.java` reads `v1.7.0_gg1` after `clean`.
     - Run `testUnit` with Docker genuinely stopped (colima profile `vz` down, `docker info` failing).
     - Run `verify-merge-invariants.sh` and record its exit code.

  Any `src/main` edit after the gate re-opens the gate.

**Publish mechanics**
- **D-11: Rehearse the final tree on `sync/v1.7.0` and require green.** After the local gate, push `sync/v1.7.0` (branch only, no tag), and require its `tests.yaml` run green and under the D-10 threshold. That run goes into the approval bundle. The `--no-ff` merge then has a byte-identical tree. Reversible: deleting the remote branch undoes it, though Phase 14 D-13 already published the earlier commits.
- **D-12: The approval bundle is all measured, none of it cited from a planning document.** It contains:
  - Both runs' totals and skip sets, plus the ClickHouse server version each run hit.
  - The `testUnit` result with Docker down.
  - The invariant script's exit code.
  - The rehearsal run's URL, conclusion and duration.
  - The secure-audit verdicts.
  - D-03's list of knowingly-open findings.
  - A summary of the CHANGELOG, FORK-DELTA and rollout doc diffs.
  - A confirmation that `.planning/.gsd-ws-arg` is untracked and that only the named tags will be pushed.

  The gate is `blocking-human`. **Reversibility:** n/a. It is a gate.
- **D-13: Publication sequence after approval.** The orchestrator runs these steps inline.
  1. Check out local `custom_build` (`c61d90ce`).
  2. Run `git merge --no-ff sync/v1.7.0`, with a message pointing at `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md` and `.planning/MERGE-CHECKLIST-v1.7.0.md`. A future `git revert -m 1` will name this commit.
  3. Push `custom_build` **alone**, non-force.
  4. Wait for its `tests.yaml` run to complete. Assert exactly one completed run for that `head_sha` plus path, no cancelled sibling, and conclusion `success`. That is CI-08's end-to-end proof.
  5. Only then push the tags **by name**:
     - `v1.7.0_gg1`, on the merge commit. It starts no run under the branch-only trigger.
     - `pre-v1.7.0-sync`, on `c61d90ce`. It *does* start one run there, because `c61d90ce`'s `tests.yaml` still has `on: [push]`. Record that run as expected and harmless: it is a different SHA and a different concurrency group.
  6. Delete `origin/sync/v1.7.0`.

  **Reversibility:** **one-way** for the `custom_build` push, which can only be reverted forward. Tags are deletable, but consumers may already have fetched them.
- **D-14: The planning tail after publication stays local.** Phase 15's verification, UAT and `/gsd-complete-milestone` bookkeeping commit to local `sync/v1.7.0`, because `custom_build` is GSD's protected base branch, and stay unpushed. STATE carries a dated note, and the next milestone's `--no-ff` merge publishes them, as PUB-04 does now. Do **not** delete local `sync/v1.7.0`. Do not make a second publication push.

### Claude's Discretion
- **`latest` drift between the two runs.** Record the server version both runs hit. If it differs, both runs must still be green and the bundle says so. If a run goes red on unchanged code, check the image date first (the STATE gate rule, ZSTD precedent) and attribute the failure under 11 D-11 before doing anything else.
- **D-02 details.** The exact WARN wording, the dedup mechanism, and the test style (log capture vs. an extracted pure predicate tested Docker-free). The pure-predicate approach is preferred, matching `ClickHouseHelperClientForkUnitTest`.
- **D-01 test placement.** Which `MetadataFreeJsonWriterTest` method is split or renamed when the pass-through pin flips. The Javadoc at `MetadataFreeJsonWriter.java:183` that names `io.debezium.time.Timestamp` as pass-through must be corrected in the same commit.
- **Commit granularity.** Following the one-behaviour-per-commit rule (7 D-19 → 11): D-01, D-02, VERSION, CHANGELOG, FORK-DELTA, rollout doc, and dispositions plus ledger can each be their own commit, or the doc commits can be grouped.
- **Links.** Whether `README.md` or `AGENTS.md` link `docs/ROLLOUT-v1.7.0_gg1.md` (AGENTS.md already links FORK-DELTA).
- **Local `sync/v1.6.0`.** Delete it after publication. It is fully contained in `sync/v1.7.0` and will then be on `origin` through `custom_build`.
- **Release workflows.** Whether the `release.yaml`/`pre-release.yaml` (`workflow_dispatch`) Confluent archive build is triggered. The default is not to: publication here means branch plus tags, as for `v1.6.0_gg1`.

### Deferred Ideas (OUT OF SCOPE)
- **Enum8/Enum16/Tuple nullability root cause** (13-REVIEW WR-05 fix proper): stays ROADMAP backlog 999.1. D-02 only warns.
- **13-REVIEW WR-01, WR-03, WR-04; 14-REVIEW WR-01, WR-02; all info items**: published open with `deferred` dispositions (D-03). These are candidates for a future hardening phase or upstream PRs.
- **`ZonedTimestamp` handling on the metadata-free route**: documented only, if research finds a parse issue.
- **Publishing the post-publication planning tail**: rides on the next milestone's `--no-ff` merge (D-14).
- **Test-classpath `org.lz4:lz4-java 1.8.0` duplicate**: stays backlog 999.2.
- **Client `0.10.0`**: still a future requirement.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SUITE-04 | Full suite on `latest` x V2 passes twice (second with `--rerun`), 0 failures/errors, skip set equal to FORK-DELTA's named skips; `testUnit` passes with Docker stopped | Gate mechanics, XML-sum and skip-set one-liners (Code Examples 5), copy Run 1 results aside before Run 2 (Pitfall 6), colima `vz` stop/start precedent, baseline numbers (69 classes / 805 tests / 18 skips, testUnit 35 / 431 / 2) |
| PUB-01 | `VERSION` is `v1.7.0_gg1` after `clean`; CHANGELOG `1.7.0_gg1` section | `generateVersionFile` has outputs but no inputs (Pitfall 1); `fork-anchors.txt` row `VERSION v1.6.0_gg1` must move in the same commit (Pitfall 2); dependency delta from `git diff c61d90ce HEAD -- gradle/libs.versions.toml` |
| PUB-02 | FORK-DELTA reflects the new state | Stale-spot inventory of `docs/FORK-DELTA.md` (Architecture Patterns, "FORK-DELTA stale spots"); ledger destination list; D-01/D-02 additions |
| PUB-03 | Non-force `--no-ff` merge after human approval; tags on `origin`; CI green on published SHA | Publication sequence with merge-shape asserts, dirty `state.json` trap (Pitfall 3), merge-by-rehearsed-SHA (Pitfall 4), locator script, post-tag re-check (Pitfall 5) |
| PUB-04 | `.planning/` history after `c61d90ce` on `origin` by merge, no `.gsd-ws-arg`, no local-only tag | Verified: file untracked and absent from tree, its add/remove commits are already ancestors of `c61d90ce`; assertions listed in Code Examples 6 |
| ROLL-01 | Operator rollout checklist | Route matrix of what changed on routes that were already writing rows (Summary finding 2), exact KeeperMap query, `system.columns` self-check (live-verified), profile-constraint query, MBean/client-name facts, ZonedTimestamp finding |
</phase_requirements>

## Summary

Phase 15 is a rehearse / measure / approve / publish phase that mirrors Phase 11, plus two small user-approved `src/main` changes. Everything the planner needs is already in the repo: Phase 11's plan 11-06 is a complete template for the gate and publication tasks, Phase 14 supplies the CI locator and result-sum one-liners, and `13-BEHAVIOUR-LEDGER.md` supplies the prose. The real risks are not the suite but ordering and bookkeeping traps: a stale generated `Version.java`, a `fork-anchors.txt` row that pins the old VERSION, a dirty tracked `.planning/state.json` that makes `git checkout custom_build` fail, merging a SHA other than the one CI rehearsed, and a tag push that starts an unexpected run.

Four findings change or sharpen CONTEXT and should reach the planner (and, for 1 and 2, the user) before plans are written. **(1) D-02's WARN and D-06's self-check understate the defect.** Because `Column.extractColumn` drops nullability for Enum and Tuple, a `Nullable(Enum8|Enum16)` slot is written without its null marker even for non-null values. That happens at the top level too, and not only inside Array/Map. The batch does not fail loudly there: values are stored as NULL or shifted. A WARN that says only "a null entry fails with DataException" would be wrong for those columns. **(2) D-07's "required vs precautionary" answer is narrower than CONTEXT assumed.** The one combination where the new jar re-encodes a route that was already writing rows is explicit `insertFormat=json` plus `bypassRowBinary=true` with `Struct`/CDC records: v1.6.0_gg1 sent those through the legacy Jackson path, v1.7.0_gg1 sends them through `MetadataFreeJsonWriter`. Compression, #820, and the nested empty-container coercion do not change the values of any batch the old jar could have completed. **(3) D-01 is verified against the live server.** Decimal-seconds with 3 or 9 fractional digits stores exact ticks, scientific notation from `BigDecimal.toString()` is accepted, and raw milliseconds read as seconds clamp to `9999-12-31`, confirming the bug. `ZonedTimestamp` strings parse under the current server default (`date_time_input_format=best_effort`) and fail under `basic`. **(4) CI-08's end-to-end proof is only complete if re-asserted after the tags are pushed**, because a stray tag-triggered run would appear after the branch run finished.

**Primary recommendation:** Plan five sequential stages: (A) D-01 and D-02 code with tests, (B) code review of just those commits, (C) VERSION/CHANGELOG/FORK-DELTA/rollout/dispositions/ledger/anchors plus the two retroactive security audits, (D) the local gate then the rehearsal push, (E) one blocking-human checkpoint followed by orchestrator-inline publication that merges the rehearsed SHA by hash. Reuse the Phase 11/14 scripts and one-liners; hand-roll nothing.

## Architectural Responsibility Map

The browser/SSR/CDN tiers do not apply. This phase's tiers are the places a capability can live in a release pipeline.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Debezium `Timestamp`/`NanoTimestamp` encoding (D-01) | Connector writer (`MetadataFreeJsonWriter`) | Docs (ledger L-20, rollout JSON-route notes) | Only the writer knows the logical type name; no column metadata exists on this route |
| Nullable(Enum/Tuple) startup WARN (D-02) | Metadata discovery (`ClickHouseHelperClient` describe loops) | Docs (FORK-DELTA Known costs, rollout self-check) | The raw type string exists only in the DESCRIBE row; `Column` has already dropped it |
| Version identity (`VERSION` to `Version.java` to MBean/client name) | Build (`build.gradle.kts generateVersionFile`) | Anchors file, FORK-DELTA | A stale generated file silently publishes the old version |
| Exactly-once pre-upgrade check | Operator, against ClickHouse + KeeperMap state table | Rollout doc | Only the operator can query their state table |
| CI-08 proof (one run per published SHA) | GitHub Actions + unauthenticated REST API | Workflow trigger in `tests.yaml` | Evidence is external; the local repo can only configure the trigger |
| Publication (merge, push, tags) | Git remote `origin` | Orchestrator (inline), human gate | One-way; protected `custom_build` |
| Retroactive security verdicts | `/gsd-secure-phase` for 13 and 14 | Approval bundle | Gate input; blocks on `high` |

## Standard Stack

No new libraries or packages. The phase uses tools already present on the machine and in the repo.

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Gradle wrapper | 9.6.1 [VERIFIED: gradle/wrapper/gradle-wrapper.properties] | Gate runs; `--rerun` is supported | Project build; CLAUDE.md's "7.4.2" is stale |
| JDK toolchain | 17 (Gradle provisions `~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`) [VERIFIED: ls] | Test JVM | `build.gradle.kts` toolchain |
| ClickHouse server image | `clickhouse/clickhouse-server:latest`, locally 26.9.14.10 created 2026-10-09T16:14:51Z [VERIFIED: docker image inspect + `clickhouse local`] | Gate target | Project constraint: `latest` x V2 |
| Jackson (existing) | 2.22.3 [VERIFIED: gradle/libs.versions.toml] | `writeNumber(BigDecimal)` for D-01 | Already used for MicroTimestamp |
| GitHub REST API (unauthenticated) | 60 req/h, 58 left at research time [VERIFIED: GET /rate_limit] | CI evidence | `gh` deliberately not installed |
| `python3 -I`, `jq`, `curl`, `git`, `colima` | 3.14.8 / 1.8.2 / system / system / 0.10.3 [VERIFIED: command -v] | Result sums, locator, publication | Phase 11/14 precedent |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| `.planning/scripts/verify-merge-invariants.sh <merge-sha> [range-end]` | Phase-end invariant gate (shape, provenance, markers, anchors) | Final gate step; record exit code. Merge sha = `54e6364858cd728e3427ce2058fc00380895c689` [CITED: MERGE-CHECKLIST-v1.7.0.md line 3] |
| `/gsd-secure-phase 13`, `/gsd-secure-phase 14` | Retroactive threat-register verification | D-09; requires `<threat_model>` blocks, present in all 12 PLAN files of phases 13 and 14 [VERIFIED: grep -c] |
| `/gsd-code-review` | D-10 step 2 review of just the D-01/D-02 commits | Config `code_review: true`, depth `standard` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Check out `custom_build` in the main tree | `git worktree add` a throwaway path for the merge and pushes | Avoids the dirty-`state.json` checkout failure and keeps `.planning/` in place for GSD. Slightly different from D-13 step 1's wording; the planner must pick one (Open Question 4) |
| Instance-level WARN dedup set | Static `ConcurrentHashMap.newKeySet()` | Instance set warns once per task per column (N tasks, N warnings); static gives "once per process" as D-02 asks. Static state needs a package-private reset for tests |

**Installation:** none.

**Version verification:** no package is added or changed in this phase. Phase 14 already OSV-checked lz4-java 1.12.0, Jackson 2.22.3, org.json 20260814, okhttp 5.5.0, mockito 5.24.0 (Recorded Facts 33, 35, 36).

## Package Legitimacy Audit

No external package is installed or changed in Phase 15 (the VERSION bump, docs and two source edits touch no dependency declaration). The package-legitimacy gate has no surface here.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| none | - | - | - | - | - | - |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Stage A  code            Stage B  review         Stage C  documents + audits
 -----------------        ---------------         -----------------------------------------
 D-01 MetadataFree  --+   /gsd-code-review         VERSION + fork-anchors.txt (same commit)
 JsonWriter + tests   |   on the D-01/D-02          CHANGELOG 1.7.0_gg1
 D-02 describe WARN --+-> commits only; fixes  -->  FORK-DELTA rewrite (grep-confirmed)
 + predicate test         land first                docs/ROLLOUT-v1.7.0_gg1.md
                                                    ledger L-20/L-21(/L-22), dispositions,
                                                    MERGE-CHECKLIST 2 pending rows
                                                    /gsd-secure-phase 13 and 14 (parallel)
                                  |
                                  v
 Stage D  local gate (exact tree to be published; no src/main edit allowed after this)
 ---------------------------------------------------------------------------------------
 compileIntegrationTestJava (cheap pre-check)
 Run 1: clean test  ->  assert Version.java == v1.7.0_gg1  ->  copy build/test-results/test aside
 Run 2: test --rerun ->  XML sums + skip set vs FORK-DELTA   (record server build label each run)
 colima stop --profile vz -> docker info fails -> testUnit --rerun -> colima start --profile vz
 verify-merge-invariants.sh <54e63648...> HEAD  -> exit code
 git push origin sync/v1.7.0 (branch only) -> ONE tests.yaml run on X -> green + duration
                                  |
                                  v
 Stage E  blocking-human gate (bundle is measured, not cited)
 ---------------------------------------------------------------------------------------
        approve                                   hold
          |                                         |
          v                                         v
 orchestrator inline (no executor):         fix on sync/v1.7.0, re-run D
 merge --no-ff X on custom_build (by SHA X = rehearsed commit)
 assert shape (2 parents, ^2 == X, tree(M) == tree(X)) -> push custom_build ALONE, no force
 poll REST: head_sha=M, path=tests.yaml -> exactly 1 run, success
 push tags by name (v1.7.0_gg1 on M, pre-v1.7.0-sync on c61d90ce)  [one push command, tags only]
 re-assert head_sha=M still 1 run, no run with head_branch=v1.7.0_gg1   <- CI-08 end to end
 expect +1 run on c61d90ce (head_branch=pre-v1.7.0-sync, old workflow): record, do not act
 delete origin/sync/v1.7.0 -> local sync/v1.6.0 deleted -> planning tail commits stay local
```

### Recommended Project Structure

Phase 15 adds or changes files only in these places:

```
VERSION                                            # v1.6.0_gg1 -> v1.7.0_gg1 (trailing newline kept)
CHANGELOG.md                                       # new top section "# 1.7.0_gg1, <date>"
docs/FORK-DELTA.md                                 # rewritten rows (see inventory below)
docs/ROLLOUT-v1.7.0_gg1.md                         # new; generic, no internal names
src/main/.../sink/db/MetadataFreeJsonWriter.java   # D-01 (2 constants, 2 cases, Javadoc at ~183 and class Javadoc)
src/main/.../sink/db/helper/ClickHouseHelperClient.java  # D-02 hook (recommended site) + pure predicate
src/test/.../db/MetadataFreeJsonWriterTest.java    # flip the pass-through pin
src/test/.../sink/MetadataFreeJsonTypeParityLiveTest.java  # NEW cell (no Timestamp cell exists today)
src/test/.../db/helper/ClickHouseHelperClientForkUnitTest.java  # D-02 predicate test
.planning/scripts/fork-anchors.txt                 # VERSION row; any anchor a D-02/D-01 edit moves
.planning/phases/13-*/13-BEHAVIOUR-LEDGER.md       # L-20, L-21 (and L-22, below)
.planning/phases/13-*/13-REVIEW-DISPOSITION.md, 14-*/14-REVIEW-DISPOSITION.md
.planning/MERGE-CHECKLIST-v1.7.0.md               # flip the 2 rows still `pending` (owner 13)
```

### Pattern 1: One behaviour per commit, anchors move with the code

**What:** each commit changes one thing; a commit that changes a `fork-anchors.txt` count updates the file in that same commit (the script evaluates the anchors file at every commit in `pre-v1.7.0-sync..HEAD`).
**When to use:** every commit in this phase. The VERSION commit must also change the row `VERSION	v1.6.0_gg1	1	BLD-07` to `v1.7.0_gg1`. [VERIFIED: .planning/scripts/fork-anchors.txt row 37]
**Anchors a D-02 edit in `ClickHouseHelperClient.java` can disturb** (fixed-string line counts): `buildTableFilterClause` 4, `shouldSkipDescribeRow` 3, `StandardCharsets.UTF_8` 1, `InputStreamReader` 2, `.compressServerResponse(false)` 1, `NETWORK_COMPRESSION_METHOD_` 0. Do not mention these strings in new comments or Javadoc, or update the file in the same commit. In `ClickHouseWriter.java`: `shouldSkipTableMetadata()` 8, `buildRowBinaryUnsupportedTypeMessage` 5.

### Pattern 2: Rehearse the exact SHA, merge that SHA by hash

**What:** the rehearsal push publishes `sync/v1.7.0` at commit X. The approval bundle describes X. The merge is `git merge --no-ff X` (by hash, after asserting `X == origin/sync/v1.7.0 == local sync/v1.7.0 at rehearsal time`), so the merge tree is byte-identical to what CI ran. Phase 11 asserted `M^2 == scratch tip` and `git diff --quiet M <scratch>` for exactly this reason.
**Consequence:** nothing may be committed to `sync/v1.7.0` between the rehearsal push and the merge. The gate approval, bundle and every later bookkeeping commit go to the local tail after publication (D-14).
**Tree-equivalence of the gate:** the gate runs on a tree T0. Docs-only plan commits (`.planning/` bookkeeping) may land between T0 and X. Assert it: `git diff --name-only <gated-HEAD> X -- . ':!.planning'` must be empty, otherwise the gate re-opens (CONTEXT D-10).

### Pattern 3: Route matrix (what each config sends, v1.6.0_gg1 vs now)

Derived from `git show c61d90ce:.../ClickHouseWriter.java` (`doInsertBatch`, `writeRecordsJson`) and the current file. [VERIFIED: Read of both versions, this session]

| Config | Records | v1.6.0_gg1 (`c61d90ce`) | now |
|--------|---------|--------------------------|-----|
| `insertFormat=json` explicit, `bypassRowBinary=false` | Struct / CDC | RowBinary against column-less synthetic table: **0 rows** (R-01) | JSON writer = `MetadataFreeJsonWriter`: rows land |
| `insertFormat=json` explicit, **`bypassRowBinary=true`** | Struct / CDC | JSON via **legacy Jackson path** (rows land; `Date`/`Timestamp` as epoch millis read as seconds, `byte[]` base64) | JSON via **`MetadataFreeJsonWriter`** (decimal seconds, epoch days, raw bytes): **values change for the same input** |
| `insertFormat` unset/other, `bypassRowBinary=true` | Struct / CDC | legacy Jackson path, table metadata present | unchanged (L-03); `cleanupExtraFields` now maps a null non-nullable Map to `{}` (same stored value) |
| any | schemaless Map / String | JSON / string path | unchanged |
| RowBinary (default for Avro/Protobuf) | Struct | RowBinary | nested-null changes (L-09..L-13, L-17), #820 top-level Map |

The second row is the only one where a batch the old jar could have completed is re-encoded by the new jar.

### FORK-DELTA stale spots (PUB-02 work list)

`docs/FORK-DELTA.md` (173 lines) was written for the v1.6.0 sync and has only been patched for v1.7.0 in a few rows. Each of these must be re-derived against the tree and grep-confirmed (Phase 11 D-15). [VERIFIED: Read of docs/FORK-DELTA.md this session]

| Location | Problem |
|----------|---------|
| Introduction (lines 1-5) | Names `sync/v1.6.0`, commit `09cae191`; must describe the v1.7.0 merged tree |
| Row "connector name ... `connector=<name>`" (line 31) | "currently `v1.6.0_gg1`" |
| Row "metadata-free insertFormat=json path" (line 33) | Says "five guard sites" and "carries a known silent-data-loss route" (R-01). Anchor `shouldSkipTableMetadata()` is now 8 lines. Must describe routing on the predicate, `MetadataFreeJsonWriter`, empty-CDC skip, null semantics (L-01, L-02, L-06, L-07, L-08, L-20) |
| Row "RowBinary fails fast for DYNAMIC and UNKNOWN" (line 35) | Add INT256/UINT256 (L-15) |
| Row `shouldSkipDescribeRow` (line 38) | "children at any depth of JSON/Dynamic" (L-16) |
| Row CI workflow (line 47) | Add branch-only trigger (CI-08), fork-count step (CI-09), `compileIntegrationTestJava` step (CI-10) |
| Row `VERSION` (line 48) | "reads `v1.6.0_gg1`" |
| Row dependency pins (line 51) | Add lz4-java 1.12.0, Jackson 2.22.3; client 0.9.6 already marked upstream's |
| Retired rows 77, 78 (D-17, lz4 rule) | Present and correct for D-17; the lz4 row needs "now `lz4Java = "1.12.0"` in the catalog, selected by upstream's group-based capability rule" |
| "Adopted wholesale" rows (84-86) and "The raw numbers" (88-110) | Measured against `v1.6.0`/`sync/v1.6.0`; re-measure against `v1.7.0` and the final tip with the four documented commands |
| Known costs 1-5 (112-137) | 1 (R-01) and 2 (harness WR-01) go; 3's four findings are fixed in Phase 13 (COLS-07, CFG-04, DEBT-02, P9-IN-01 reworded) and must not be listed as open; 4's "WR-01 (map)" is closed by WRT-07. Replace with: 999.1 nested-null/Enum/Tuple limitation + D-02 WARN, 13 WR-01 (all-defaults rows on the `bypassRowBinary=true`-without-explicit-json route), 13 WR-03 (message-less exception), the JSON-route gaps (L-03, L-04, L-05 narrowed), 14-REVIEW open items that an operator can observe, item 5 (upstream capability not adopted) kept |
| Named skip inventory (139-163) | Re-check against both gate runs; the 18 count and the compile-only #800/cluster note stay if the runs agree |
| "For the next syncer" (165-173) | Mostly current; add the v1.7.0 sync facts (merge sha, rollout doc link) |

### Anti-Patterns to Avoid
- **Merging `sync/v1.7.0` by branch name after the tail has grown.** Merge the rehearsed SHA.
- **Pushing the branch and tags in one command.** Tags go in a separate push after the branch run completes.
- **Trusting `Version.java` after editing `VERSION` without `clean`.** See Pitfall 1.
- **Editing `13-REVIEW-DISPOSITION.md` table only.** The YAML frontmatter carries the same dispositions and the `open:` count; edit both.
- **Scripted find-and-replace on `ROADMAP.md` without slicing to the `### Phase 15:` section.** Wave headings repeat across phases; an unanchored replace truncated this file once (`c1480f8f`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Locating the CI run for a SHA | Parsing the Actions web UI, or `gh` | `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<sha>` filtered with `jq` on `.path == ".github/workflows/tests.yaml"` | Verified working; 60/h budget; the Phase 11 plan 11-06 task 3 verify block is a template |
| Summing test results and skip sets | Reading Gradle console output | The two `python3 -I` XML one-liners from `14-03-PLAN.md` lines 101-102 (Code Examples 5) | Console output echoes cached results; XML is what `--rerun` rewrites |
| Decimal-seconds encoding | String formatting of the timestamp | `BigDecimal.valueOf(long, scale)` + `gen.writeNumber(BigDecimal)` | The pattern for Timestamp (3) and MicroTimestamp (6) already exists; exactness verified on the server |
| KeeperMap state clearing | Editing Keeper znodes | `DELETE FROM <state table> WHERE key IN (...)` or `TRUNCATE TABLE` | KeeperMap supports both [CITED: clickhouse.com/docs/engines/table-engines/special/keeper-map] |
| Merge-shape verification | Eyeballing `git log --graph` | `git rev-list --parents -n 1 M` (3 tokens), `git rev-parse M^2`, `git diff --quiet M X` | Phase 11's assertions; fast-forward fails them |
| Raw-type detection for D-02 | Parsing types through `Column.extractColumn` | A pure `static boolean` over the raw DESCRIBE type string | `Column` has already dropped the nullability; a pure predicate tests Docker-free like `shouldSkipDescribeRow` |
| Pushing "all my tags" | `git push --tags` | Explicit `refs/tags/<name>` refspecs | Local repo holds upstream's `v1.x` tags and `v1.6.0_gg1-sync`, `v1.2.9_gg4-tests` [VERIFIED: git tag -l] |

**Key insight:** every mechanism this phase needs already exists and has been exercised once (Phase 11 for publication, Phase 14 for the CI locator and sums). The work is composing them in the right order with the right assertions, and writing documents that are true of the final tree.

## Runtime State Inventory

The VERSION bump renames the `version=` MBean segment and the ClickHouse client name. Answering the five categories for that rename:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | The KeeperMap state table holds `topic-partition` keys and offset ranges; none embeds the version string. [VERIFIED: KeeperStateProvider.java:70-77] | None for the rename. Exactly-once pre-upgrade check (D-07) for in-flight batches |
| Live service config | Operator dashboards/alerts that match the full MBean name `...,name=SinkTask<id>,version=<ver>` break on every release. Between `v1.6.0_gg1` and now only `version=` changes: `git diff v1.6.0_gg1 HEAD -- src/main/.../util/jmx/` is empty, so `connector=<name>` is unchanged [VERIFIED: git diff] | Rollout doc item with a self-check (find matchers on the full name; switch to wildcard on `version=`) |
| OS-registered state | None in this repo | None |
| Secrets / env vars | None reference the version | None |
| Build artifacts | `build/generated/sources/version/.../Version.java` currently reads `v1.6.0_gg1` [VERIFIED: cat]; a stale copy survives an edit of `VERSION` | Always `clean` before the gate; assert the file content |
| Client name (extra) | `CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/" + Version.ARTIFACT_VERSION` is sent as the client name on every request [VERIFIED: ClickHouseHelperClient.java:110,115,191] | Rollout doc: filters on the client name in `system.query_log` change with the release |

## Common Pitfalls

### Pitfall 1: Stale generated `Version.java`
**What goes wrong:** `VERSION` says `v1.7.0_gg1`, the shipped jar and the MBean/client name still say `v1.6.0_gg1`.
**Why it happens:** `generateVersionFile` declares `outputs.dir` only and no inputs; the value comes from `project.version`, read from the `VERSION` file at configuration time. [VERIFIED: build.gradle.kts:48 and 417-437] A task with outputs and no inputs is up to date while its outputs are unchanged [ASSUMED: Gradle up-to-date semantics, not exercised this session].
**How to avoid:** Run 1 is `clean test`; then assert `grep -F 'ARTIFACT_VERSION = "v1.7.0_gg1"' build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`. Currently the file holds `"v1.6.0_gg1"` [VERIFIED].
**Warning signs:** `Version.java` mtime older than the `VERSION` commit; MBean tests pass either way (they read `Version.ARTIFACT_VERSION` dynamically and no source file hard-codes `gg1` [VERIFIED: git grep]).

### Pitfall 2: The anchors file pins the old VERSION
**What goes wrong:** `verify-merge-invariants.sh` fails invariant 3 on the VERSION commit (or on every later commit).
**Why:** row `VERSION	v1.6.0_gg1	1	BLD-07` expects one matching line. [VERIFIED: fork-anchors.txt]
**How to avoid:** change the row to `v1.7.0_gg1` in the same commit as `VERSION`. Optionally add anchors for new kept patches (the D-01 logical-name constants; the D-02 predicate) in the commit that introduces them.

### Pitfall 3: Dirty tracked `.planning/state.json` blocks `git checkout custom_build`
**What goes wrong:** D-13 step 1 fails with "local changes would be overwritten".
**Why:** `.planning/state.json` is tracked, currently modified, and its content differs between `c61d90ce` and `HEAD` [VERIFIED: git status, git diff --quiet]. GSD rewrites it during normal operation.
**How to avoid:** do the merge and both pushes in a throwaway `git worktree add <path outside repo> custom_build` (removes the problem, leaves the main tree on `sync/v1.7.0`), or commit/stash `state.json` first. Remove the worktree afterwards. [ASSUMED: worktree is compatible with GSD tooling; Phase 11 did not use it]

### Pitfall 4: Merging a SHA CI never ran
**What goes wrong:** the approval bundle describes tree X, the merge publishes X plus later local commits.
**How to avoid:** Pattern 2. Assert `git rev-parse origin/sync/v1.7.0` equals the rehearsal SHA at the gate and again before the merge; merge that hash; assert `git diff --quiet M X`.

### Pitfall 5: Tag push joins or starts a run
**What goes wrong:** Phase 11 saw a `custom_build` run `cancelled` by a tag push on the same SHA (run 37619430314 cancelled, 37619434848 `v1.6.0_gg1` success on `c61d90ce`). [VERIFIED: REST this session]
**Now:** `tests.yaml` has `on: push: branches: ['**']` and no `tags` key, so a tag-only push starts no run [CITED: docs.github.com workflow syntax: "If you define only `tags`/`tags-ignore` or only `branches`/`branches-ignore`, the workflow won't run for events affecting the undefined Git ref"]. This has never been exercised end to end under the new workflow.
**How to avoid:** push the branch alone; wait for completion; push the tags; wait ~2 minutes; **re-run the locator for `head_sha=M`** and assert still exactly one run and zero runs whose `head_branch` is `v1.7.0_gg1`. Without that re-check, D-13's step-4 assertion proves the branch run only, not CI-08.
**The expected extra run:** `pre-v1.7.0-sync` at `c61d90ce` starts a third run for that SHA (old `on: [push]`), `head_branch=pre-v1.7.0-sync`. It tests the old tree against today's `latest`; if it goes red it is rollback-tag noise, record it and apply 11 D-11 attribution only if asked. [ASSUMED: it will be green]

### Pitfall 6: `--rerun` overwrites Run 1's evidence
**What goes wrong:** Run 2's XML replaces Run 1's, so Run 1's totals and skip set can no longer be shown.
**How to avoid:** after Run 1, `cp -R build/test-results/test build/phase15-run1-results` (Phase 14 kept `build/phase14-jackson-test-results` the same way) and read the server build label (`docker image inspect clickhouse/clickhouse-server:latest --format '{{index .Config.Labels "com.clickhouse.build.version"}}'`) after **each** run; the harness pulls floating tags every run, so `latest` can move between Run 1 and Run 2.

### Pitfall 7: Stopping Docker the wrong way
**What goes wrong:** bare `colima stop`/`start` target the `default` profile; the daemon stays up and `testUnit` "passes" with Docker running, or the wrong VM is booted.
**How to avoid:** `colima stop --profile vz`, then `docker info` must fail (precedent: 11-04-SUMMARY lines 175-178: rc=1, "no such file or directory" on the vz socket), then `testUnit --rerun`, then `colima start --profile vz` and `docker info` succeeding. The `docker` CLI is not on PATH; use `/opt/homebrew/Cellar/docker/29.8.2/bin/docker` [VERIFIED: ls]. Only `portainer` runs in the VM right now [VERIFIED: docker ps].

### Pitfall 8: GitHub REST budget
**What goes wrong:** polling at 90 s for three waits (rehearsal ~7 min, published ~7 min, plus the extra tag run) plus locator re-queries can exceed 60 requests per hour and return 403/rate-limit JSON that a `jq` count silently reads as zero.
**How to avoid:** poll `GET /actions/runs/<id>` once the id is known (one request per poll), 60-90 s interval, check `/rate_limit` first (it is free), and treat any response without a `workflow_runs` array as an error, not as zero. Phase 14's run took 6m32s, Phase 11's 350 s [CITED: 14-04-SUMMARY, 11-05-SUMMARY].

### Pitfall 9: `latest` drift and the CI image
**What goes wrong:** a red full suite on unchanged code.
**Precedent:** 26.9 switched `network_compression_method` to ZSTD (D-17). Local `latest` moved 26.9.12.8 (Oct 6) to 26.9.13.15 (Phase 13) to 26.9.14.10 (Oct 9) [CITED: Recorded Facts 25, 29, 32; VERIFIED: image inspect]. Per 11 D-11: check image date and server build label first, attribute, never blind re-run.

### Pitfall 10: Naming collision on "WR-02"
PUB-01's "WR-02 strictness" is the milestone follow-up (nested null in a non-nullable slot throws `DataException`, WRT-08, L-10). The Debezium timestamp finding is **13-REVIEW WR-02**. CHANGELOG, FORK-DELTA and the rollout doc must always write the latter in full. [CITED: 15-CONTEXT.md]

### Pitfall 11: D-02 detection scope and duplicate WARNs
**What goes wrong:** the WARN fires for sub-column rows as well as the root row, once per task, and on every re-describe.
**Facts:** DESCRIBE with `describe_include_subcolumns=1` returns the full raw type on the root row and repeats pieces on sub-column rows: for `m Map(String, Nullable(Enum16(...)))` the rows are `m`, `m.size0`, `m.keys`, `m.keys.size`, `m.values  Array(Nullable(Enum16(...)))`, `m.values.null`. [VERIFIED: live probe on 26.9.14.10] `describeTable` is called from `extractTablesMapping` (re-describes only when the column count grew), and from `ClickHouseWriter` at ~1201 and ~1756 (fallback paths without that cache) [VERIFIED: grep]. `extractTablesMapping` describes every table in the database, not only topic-mapped ones.
**How to avoid:** check root rows only (`!fieldDescriptor.isSubcolumn()`); dedup on `database.table.column:rawType` in a process-wide set; hook once inside both `describeTableV1` and `describeTableV2` loops right after the `shouldSkipDescribeRow` check, using one shared private method (DRY).

### Pitfall 12: Understating the Enum/Tuple defect
See Summary finding 1 and Open Question 1. The nested guards in `doWriteColValue` (ARRAY lines ~711-725, MAP ~682-693, TUPLE ~732-742) only throw when an entry is null. For non-null entries the slot is still treated as non-nullable, so `Array(Nullable(Enum8))` and `Map(K, Nullable(Enum))` elements are written without the per-element null marker, and top-level `Nullable(Enum8|Enum16)` is written without its marker. Upstream `v1.7.0`'s Enum branch is identical (no `.isNullable(isNull)`), so this is an upstream defect, not a fork regression. [VERIFIED: Read of ClickHouseWriter.java:1026-1070 and 640-770; git show v1.7.0:.../Column.java:196-210; CITED: 13-RESEARCH.md Spike b2/b3, "a top-level Nullable(Enum8) stores NULL for a real value"]

### Pitfall 13: Disposition files carry state in two places
`13-REVIEW-DISPOSITION.md` and `14-REVIEW-DISPOSITION.md` each have YAML `findings:` entries with `disposition:`, an `open:`/`total:` count, and a Markdown table whose Source cell holds the deferral reason. The file's own footer says `deferred` is set by hand with the reason in Source. Edit frontmatter and table together. [VERIFIED: Read of both files]

### Pitfall 14: `MERGE-CHECKLIST-v1.7.0.md` still has two `pending` rows
Rule 2 of that file says "Phase 15 asserts zero `pending` rows before publication". Two rows are `pending`, both owned by phase 13: the ClickHouseWriter hunk `@@ -556,0 +558,6 @@` (#820 shadow, closed in substance by WRT-06/WRT-07/WRT-08) and `@@ -1323 +1330,6 @@` (`cleanupExtraFields` null Map, JSONP-07/08). [VERIFIED: grep -n '| pending |', lines 72 and 75] Flip them with evidence (test names, commit ids) before the gate.

## Code Examples

### 1. D-01 writer cases (extends the existing pattern)
Source: derived from 13-REVIEW WR-02's suggested fix and the existing `DEBEZIUM_MICRO_TIMESTAMP` case in `MetadataFreeJsonWriter.writeLogicalValue` [VERIFIED: Read of the file lines 47-48, 186-226].
```java
private static final String DEBEZIUM_TIMESTAMP = "io.debezium.time.Timestamp";
private static final String DEBEZIUM_NANO_TIMESTAMP = "io.debezium.time.NanoTimestamp";
private static final int NANOS_SCALE = 9;

case DEBEZIUM_TIMESTAMP:
    if (value instanceof Number millis) {
        gen.writeNumber(BigDecimal.valueOf(millis.longValue(), MILLIS_SCALE));
        return true;
    }
    return false;
case DEBEZIUM_NANO_TIMESTAMP:
    if (value instanceof Number nanos) {
        gen.writeNumber(BigDecimal.valueOf(nanos.longValue(), NANOS_SCALE));
        return true;
    }
    return false;
```
`io.debezium.time.Timestamp` is INT64 epoch milliseconds and `NanoTimestamp` INT64 epoch nanoseconds [CITED: javadoc.io/doc/io.debezium/debezium-core, package io.debezium.time]. Update the Javadoc at line ~183 ("including {@code io.debezium.time.Timestamp} and ZonedTimestamp") to name only `ZonedTimestamp`, and the class Javadoc list of encodings.

Live evidence for this encoding (ClickHouse 26.9.14.10, `clickhouse-client`, JSONEachRow, table `DateTime64(3,'UTC')` / `DateTime64(9,'UTC')`) [VERIFIED: live probe this session]:

| Input JSON number | Column | Stored |
|---|---|---|
| `1704067200.123` | `DateTime64(3)` | `2024-01-01 00:00:00.123` |
| `1704067200.123456789` | `DateTime64(9)` | ticks `1704067200123456789` (exact) |
| `0.000000000`, `0E-9`, `1E-9`, `9.99E-7` | `DateTime64(9)` | ticks `0`, `0`, `1`, `999` (scientific notation from `BigDecimal.toString()` is accepted; `BigDecimal.valueOf(0L, 9).toString()` is `0E-9`, verified with `java`) |
| `1704067200123` (raw millis, today's pass-through) | `DateTime64(3)` | `9999-12-31 23:59:59.000` (clamped; this is the bug) |

Not probed: negative (pre-1970) values. Parity cells must use matching precision: RowBinary writes the raw long as ticks (`doWriteDates` DateTime64 INT64 branch), so Timestamp into `DateTime64(3)` and NanoTimestamp into `DateTime64(9)` are the only cells where RowBinary is itself correct; a millis value into `DateTime64(6)` is correct on the JSON route and wrong on RowBinary.

`ZonedTimestamp` (documentation only, per CONTEXT): the string `2024-01-01T00:00:00Z` (also with fractional seconds and with a `+01:00` offset) parses into `DateTime64(3)` under the server's current default `date_time_input_format = best_effort` (`changed = 0`), and fails with `CANNOT_PARSE_INPUT_ASSERTION_FAILED` under `date_time_input_format=basic`. [VERIFIED: live probe, 26.9.14.10] An operator who pins `basic` in a profile breaks ZonedTimestamp columns on the metadata-free route.

### 2. D-02 pure predicate and hook
Source: proposed; modelled on `shouldSkipDescribeRow`'s Docker-free unit test. The rows below are what `system.columns`/DESCRIBE return [VERIFIED: live probe].
```java
private static final Pattern NULLABLE_ENUM_OR_TUPLE = Pattern.compile("Nullable\\((?:Enum8|Enum16|Tuple)\\(");

static boolean hasNullableEnumOrTuple(String rawType) {
    return rawType != null && NULLABLE_ENUM_OR_TUPLE.matcher(rawType).find();
}

// inside both describeTableV1 and describeTableV2 loops, after shouldSkipDescribeRow(...), before Column.extractColumn:
warnOnceIfNullableEnumOrTuple(database, tableName, fieldDescriptor);   // root rows only: !isSubcolumn()
```
Key = `database + '.' + table + '.' + column + ':' + rawType` in a `ConcurrentHashMap.newKeySet()`; log only metadata (never record data). Test with the pure predicate on: `Array(Nullable(Enum8('x' = 1)))`, `Map(String, Nullable(Enum16('y' = 1)))`, `Nullable(Tuple(a Int32))`, `Nullable(Enum8('z' = 1))` (all true) and `Array(Nullable(Int32))`, `Enum8('a' = 1)`, `Tuple(a Nullable(Int32))` (false). Enum names containing the literal text `Nullable(Enum8(` would false-positive; harmless for a WARN.

### 3. D-07 exactly-once pre-upgrade check
Source: `KeeperStateProvider.java:70-77` (create statement), `:99` (select), `ClickHouseSinkConfig.java:314-315` (defaults `zkPath=/kafka-connect`, `zkDatabase=connect_state`), `State.java` (NONE, BEFORE_PROCESSING, IN_PROCESSING, AFTER_PROCESSING). [VERIFIED: Read]
```sql
-- the table is named after the zkDatabase setting (default connect_state), created in the connector's database
SELECT `key`, minOffset, maxOffset, state
FROM `connect_state`
WHERE state != 'AFTER_PROCESSING'
ORDER BY `key`;
```
`key` is `<topic>-<partition>` (with `enableDbTopicSplit` the topic is prefixed `<database><dbTopicSplitChar>`). Zero rows means the upgrade is safe from the retry hazard. The connector's `Processing.doLogic` writes only BEFORE_PROCESSING and AFTER_PROCESSING after the first NONE; a row left in BEFORE_PROCESSING after a clean stop means a batch was in flight or failed. The KeeperMap engine requires `keeper_map_path_prefix` in the server config (a bare server returned `KeeperMap is disabled because 'keeper_map_path_prefix' config is not defined`), so the check could not be executed here; the SQL is derived from the code, not run. For the R-01 backfill's second step, `DELETE FROM connect_state WHERE `key` IN ('<topic>-<partition>', ...)` or `TRUNCATE TABLE connect_state` [CITED: KeeperMap docs]; keep the ledger's prose "as written" and add the statement as an example, not run.

**When the check is required.** Dedup compares the hash of the inserted part (field names, types, and the data stream) [CITED: clickhouse.com docs, replicated_deduplication_window], so a re-encoding matters only if it changes the parsed values. Non-replicated MergeTree tables do not deduplicate unless `non_replicated_deduplication_window` is set (default 0) [CITED: same page], which the doc should state because it bounds what exactly-once can promise.

| Candidate | Changes parsed values of a batch the old jar could have completed? | Check |
|---|---|---|
| Explicit `insertFormat=json` + `bypassRowBinary=true` + Struct/CDC (route matrix row 2) | **Yes**: Timestamp/Date/bytes/Decimal handling and empty-CDC skipping differ | **Required** if any such connector runs with `exactlyOnce=true` |
| D-01 Timestamp/NanoTimestamp | Only on the `MetadataFreeJsonWriter` routes: Struct/CDC with explicit json. Row 1 of the matrix wrote nothing before; row 2 is already covered above | Subsumed by the row above |
| R-01 routing (row 1) | Old route wrote no rows, so there is nothing to deduplicate against; the backfill procedure applies instead | Precautionary |
| #820 null optional Map into a non-nullable Map | Old bytes were `size, stray marker` (stream desync: error or garbage), new are `00`; no completed old attempt to match | Precautionary |
| L-09 nested empty-container coercion | Without DEFAULT columns the old insert failed server-side (`ATTEMPT_TO_READ_AFTER_EOF`, nothing stored); with DEFAULT columns the "accidental" empty value equals the new bytes | Precautionary |
| L-10/L-11/L-13/L-17 new `DataException` | An old attempt that silently stored a mis-encoded batch is retried by a jar that now throws: the task stops (not a duplicate, a stuck retry) | Mention under the nested-null item |
| `cleanupExtraFields` null Map to `{}` (bypass route with metadata) | Jackson's `NON_NULL` inclusion drops a null map entry, so the column took its default; `{}` stores the same empty map | None |
| Compression (`setCompressionMethod`, `compress=false`, `network_compression_method=lz4`) | Transport and response framing only; stored values identical. Consistent with Recorded Facts 25/26 (all four live rows inserted and read back equal) | None |
| `KeeperStateProvider` read now counts streamed rows (EOS-01) | State read, not data | None |

### 4. Self-check queries for the rollout doc (live-verified syntax)
```sql
-- Nullable(Enum)/Nullable(Tuple) anywhere in a column type (top level, Array, Map, Tuple field)
SELECT database, table, name, type FROM system.columns
WHERE database = '<db>' AND (type LIKE '%Nullable(Enum%' OR type LIKE '%Nullable(Tuple%');

-- compression: is network_compression_method pinned or constrained by a profile?
SELECT profile_name, user_name, role_name, setting_name, value, min, max, writability
FROM system.settings_profile_elements WHERE setting_name = 'network_compression_method';
```
Both run on 26.9.14.10 (column names confirmed with `DESCRIBE TABLE system.settings_profile_elements`). The server default is `ZSTD` with `changed = 0`. A `CONST`/readonly constraint on this setting would reject the connector's per-request `network_compression_method=lz4` [ASSUMED; not probed]. The doc's own narrower D-06 wording ("`Array`/`Map` types containing `Nullable(Enum` or `Nullable(Tuple`") misses top-level `Nullable(Enum8)`; see Open Question 1.

### 5. Gate sums (reuse verbatim)
Source: `14-03-PLAN.md` lines 101-102. Adjust the skipped count only if the final tree changes the named skips.
```bash
python3 -I -c "import glob,xml.etree.ElementTree as E;R=[E.parse(p).getroot() for p in glob.glob('build/test-results/test/TEST-*.xml')];S=lambda k:sum(int(r.get(k)) for r in R);print('classes',len(R),'tests',S('tests'),'failures',S('failures'),'errors',S('errors'),'skipped',S('skipped'));raise SystemExit(0 if R and S('failures')==0 and S('errors')==0 and S('skipped')==18 else 1)"
python3 -I -c "import glob,xml.etree.ElementTree as E;print('\n'.join(sorted(c.get('classname').rsplit('.',1)[-1]+'#'+c.get('name') for p in glob.glob('build/test-results/test/TEST-*.xml') for c in E.parse(p).getroot().iter('testcase') if c.find('skipped') is not None)))"
```
Baseline at Phase 14: full suite 69 classes, 805 tests, 18 skipped; `testUnit` 35 classes, 431 tests, 2 skipped [CITED: MERGE-CHECKLIST-v1.7.0.md Recorded Fact 36]. Phase 15 adds tests, so assert zero failures/errors and skip-set equality, not a fixed total. The 18-entry set is the named skip inventory (7 `@Disabled` methods plus 11 `exactlyOnceStateMismatchTest` cases).

### 6. Publication assertions
```bash
# PUB-04 (run on the tip that will be merged, and again on M)
git ls-tree -r --name-only "$X" | grep -c '^\.planning/\.gsd-ws-arg$'        # 0
git log --format=%H c61d90ce.."$X" -- .planning/.gsd-ws-arg | wc -l           # 0
git merge-base --is-ancestor sync/v1.6.0 "$X" && echo v1.6.0-tail-contained
# merge shape (M = merge commit)
git rev-list --parents -n 1 "$M" | wc -w                                      # 3
[ "$(git rev-parse "$M^2")" = "$X" ] && git diff --quiet "$M" "$X" && echo SHAPE_OK
git merge-base --is-ancestor origin/custom_build "$M" && echo NON_FORCE_PUSH_OK
# pushes: explicit refspecs, no --force, no --tags
git push origin refs/heads/custom_build:refs/heads/custom_build
git push origin refs/tags/v1.7.0_gg1 refs/tags/pre-v1.7.0-sync                 # only after the branch run completed
# only the named tags may be on origin afterwards (4 total with the two from Phase 11)
git ls-remote --tags origin | wc -l                                            # 4
```
Both existing tags on origin are **lightweight** (no `^{}` peeled lines in `git ls-remote --tags origin`) [VERIFIED]; create `v1.7.0_gg1` the same way (`git tag v1.7.0_gg1 "$M"`) for consistency; `pre-v1.7.0-sync` already exists locally at `c61d90ce` and must not be moved.

CI locator (CI-08), with the post-tag re-check:
```bash
REPO=ne1r0n/clickhouse-kafka-connect; P=.github/workflows/tests.yaml
curl -sS "https://api.github.com/repos/$REPO/actions/runs?head_sha=$M" |
  jq -e --arg p "$P" '[.workflow_runs[] | select(.path==$p)] as $r
     | {n:($r|length), cancelled:([$r[]|select(.conclusion=="cancelled")]|length),
        tagRuns:([$r[]|select(.head_branch=="v1.7.0_gg1")]|length),
        conclusions:[$r[]|.conclusion]}'
# expect n=1, cancelled=0, tagRuns=0, conclusions=["success"]; run it before AND ~2 min after the tag push
```
`jq -e` plus an explicit check that `.workflow_runs` exists protects against a rate-limit body being read as zero runs.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Fork pinned `network_compression_method=lz4` unconditionally (D-17) | Upstream `setCompressionMethod` (default true) plus unconditional `compress=false` | Phase 12 / upstream 1.7.0 | Rollout item; V2 needs at least one of the two controls under an operator `compress=true` |
| `tests.yaml` on `[push]` | `on: push: branches: ['**']` | Phase 14 (CI-08) | Tag pushes start no run; except `pre-v1.7.0-sync`, whose commit still carries the old workflow |
| ClickHouse `network_compression_method` default LZ4 | ZSTD from 26.9 | 26.9 | Why `latest` drift must be attributed before any red run is touched |
| `date_time_input_format` | `best_effort` on 26.9.14.10 (`changed = 0`) [VERIFIED: probe] | observed this session | ZonedTimestamp strings parse on the JSON route; `basic` would break them |
| `ubuntu-latest` | Moves to Ubuntu 26 from 2026-10-19 [CITED: STATE.md Blockers, not re-verified] | upcoming | Publish before then if possible; a runner image change would also confound attribution |

**Deprecated/outdated:** `.claude/CLAUDE.md` still says Gradle 7.4.2 and `VERSION` "currently `v1.2.9_gg4`"; the repo is on Gradle 9.6.1 and `v1.6.0_gg1`. Out of scope, noted so nobody trusts it.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | ClickHouse dedup compares parsed block content, so only value-changing re-encodings matter for D-07 (inferred from the docs sentence "calculated from the composition of the field names and types and the data of the inserted part") | Code Examples 3 | If the hash covered the raw request body, compression/#820/JSON-null changes would also be "required" checks |
| A2 | `generateVersionFile` stays up to date after `VERSION` changes because it declares no inputs | Pitfall 1 | Low; `clean` is mandated either way |
| A3 | A throwaway `git worktree` for the merge is compatible with GSD tooling and the protected-branch rule | Pitfall 3, Alternatives | Fall back to committing/stashing `state.json`, then checkout |
| A4 | The extra run on `c61d90ce` for `pre-v1.7.0-sync` will be green against `latest` 26.9.x | Pitfall 5 | A red run on the rollback tag is cosmetic but must be attributed if the user asks |
| A5 | A `CONST`/readonly profile constraint on `network_compression_method` would reject the connector's per-request lz4 setting | Code Examples 4 | The rollout self-check would over- or under-warn |
| A6 | `ubuntu-latest` changes to Ubuntu 26 on 2026-10-19 (from STATE.md, not re-verified) | State of the Art | If earlier, CI behaviour could shift between rehearsal and publication |
| A7 | Negative (pre-1970) epoch values encode correctly with decimal seconds on the server | Code Examples 1 | Edge case on a rare input; add a unit byte pin and optionally a live cell |
| A8 | A static process-wide dedup set is acceptable for the WARN | Pitfall 11 | Tests need a reset hook; an instance set gives one WARN per task |
| A9 | Top-level `Nullable(Tuple)` is affected the same way as the nested case (bare `RuntimeException` on null; single-field works by coincidence; multi-field fails) | Open Question 1 | Taken from 13-RESEARCH Spike b3, not re-probed at top level this session |

## Open Questions

1. **Should D-02's WARN and D-06's check widen to top-level `Nullable(Enum8|Enum16)` and describe the defect accurately? (needs user confirmation)**
   - What we know: `Column.extractColumn`'s Enum and Tuple branches ignore `isNull`, in upstream v1.7.0 as well. The writer then omits the null marker for those slots. Non-null Enum values in `Array(Nullable(Enum))`, `Map(K, Nullable(Enum))` and top-level `Nullable(Enum)` are mis-encoded (NULL or shifted) without any exception; only null entries throw.
   - What's unclear: CONTEXT D-02 specifies the sentence "a null entry in that slot fails the whole batch with `DataException`" and scopes Enum to inside Array/Map.
   - Recommendation: have the WARN cover any `Nullable(Enum8|Enum16)` and `Nullable(Tuple(` anywhere in the raw root type, with wording that says values may be stored as NULL or shifted and null entries fail the batch, plus the 999.1 pointer. Use the broader `system.columns` query in Code Examples 4 for the rollout self-check. Without confirmation, plan the D-02 text as written and put the broader statement in the rollout doc only.

2. **Record the route-matrix finding as ledger entry L-22 and in the rollout doc (needs a ledger entry, not a code change).**
   - What we know: explicit `insertFormat=json` plus `bypassRowBinary=true` with Struct/CDC records wrote rows through Jackson in `v1.6.0_gg1` and now goes through `MetadataFreeJsonWriter`. Stored values for Timestamp, Date, Time and bytes fields change, and empty CDC records are skipped. FORK-DELTA's current text implies that combination is unpinned and ledger L-01/L-03 do not describe it.
   - Recommendation: add L-22 ("explicit json + bypassRowBinary=true now uses the metadata-free writer"), cover it in JSON-route type notes and mark the D-07 check as required for it. Check whether a unit or live pin already exists for that combination before claiming one (`MetadataFreeJsonInsertUnitTest` pins routing, not values).

3. **CHANGELOG date.** `# 1.7.0_gg1, <publish date>` is part of the rehearsed tree. Recommendation: use the planned publish day; if publication slips past midnight, changing the date is a docs-only edit that must be made before the rehearsal push (otherwise it re-opens Pattern 2's byte-identical claim). Today is Saturday 2026-10-10.

4. **Merge in a worktree or in the main tree?** Recommendation: worktree (Pitfall 3). Either way the plan must state how `state.json` is handled and must remove the worktree afterwards.

5. **Where does the approval record live?** It cannot be committed to `sync/v1.7.0` before the merge (Pattern 2). Recommendation: capture the user's verbatim answer in the publication SUMMARY, committed to the local tail after the merge, and keep the bundle in the checkpoint message.

6. **D-02 WARN noise.** `extractTablesMapping` describes every table in the database, so the WARN covers tables the connector may never write. Recommendation: accept (the user chose WARN); mention `targetTableFilter` narrows discovery.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | Gate runs, live pins | yes | Docker 29.8.2 client; colima 0.10.3; `vz` Running, 8 CPU / 16 GiB | None; start with `colima start --profile vz` |
| ClickHouse `latest` image | Gate runs | yes (cached) | 26.9.14.10, created 2026-10-09T16:14:51Z | Harness pulls floating tags each run |
| JDK 17 toolchain | Test JVM | yes | `~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`; also Semeru 17 and Homebrew 21 | Gradle provisions |
| `git` + SSH to `origin` | Publication | yes | `git ls-remote origin` succeeded | None |
| GitHub REST API (unauthenticated) | CI evidence | yes | 58 of 60 requests left at 09:43 UTC | Web UI (manual) |
| `python3`, `jq`, `curl`, `node` | Sums, locator, GSD tools | yes | 3.14.8, 1.8.2, system, 24.14.1 | None |
| `gh` CLI | - | no (deliberate) | - | REST API |
| `yq` | `yq` calls in AGENTS.md matrix lookup | no | - | Not needed here (single `latest` x V2 target) |
| ClickHouse Keeper / `keeper_map_path_prefix` | Running the D-07 query | no (not in a bare server) | - | Query is derived from code; operator runs it |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** `gh` (REST API), `yq` (unneeded).

## Security Domain

`security_enforcement` is enabled (ASVS level 1, `security_block_on: high`). Validation Architecture (Nyquist) is skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth change; connector credentials untouched |
| V3 Session Management | no | - |
| V4 Access Control | no | Publication uses the maintainer's existing SSH access; no token is created or logged |
| V5 Input Validation | yes | Jackson generator escaping for field names and strings in `MetadataFreeJsonWriter` (`gen.writeFieldName`, `writeUTF8String` escapes quote, backslash, control bytes); D-09 audit of Phase 13 |
| V6 Cryptography | no | - |
| V7 Error Handling and Logging | yes | The D-02 WARN logs schema metadata only (database, table, column, raw type); the nested-null `DataException` message is built from `Column` metadata only (L-10); AGENTS.md forbids logging data |
| V10 Malicious Code / V14 Configuration | yes | Workflow trigger change, `$GITHUB_STEP_SUMMARY` writes, five dependency bumps (D-09 audit of Phase 14); `actions/*` pinned by major tag only (existing) |

### Known Threat Patterns for this phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Internal hostnames, connector names, prod versions or credentials landing in a public rollout doc, CHANGELOG or approval bundle | Information Disclosure | Rollout doc is generic with placeholders (D-06); run a secret/hostname scan over `docs/`, `CHANGELOG.md` and the diff to be published before the rehearsal push (Phase 11 did this pre-push, T-11-01); the Phase 15 commit authors' git email is a public noreply for `ne1r0n` repo config [VERIFIED: git config] |
| Upstream or local-only tags published by accident | Tampering | Explicit `refs/tags/<name>` refspecs only; assert `git ls-remote --tags origin` lists exactly four tags afterwards |
| Force push or fast-forward replacing the reviewable merge commit | Tampering | `--no-ff` explicit; shape assertions; `git merge-base --is-ancestor origin/custom_build M` before the push; no `--force*` anywhere |
| Publishing a tree other than the approved one | Repudiation | Merge the rehearsed SHA by hash; gate approval recorded verbatim in the publication SUMMARY |
| CI evidence taken from a different run (rehearsal, wrong workflow, rate-limit body) | Spoofing | Locator keyed on `head_sha` plus `.path`; exactly-one assertion; error on a missing `workflow_runs` array |
| Table or column names with control characters injected into the D-02 WARN (names come from the server's catalog) | Tampering (log injection) | Low risk; log through SLF4J parameters, not string concatenation into a format string |
| Dependency supply chain | Tampering | No dependency change in Phase 15; Phase 14 evidence (OSV, `dependencyInsight`) stands |

An open **high** threat in either retroactive audit is a blocking checkpoint (D-09); a fix lands before the gate under D-04's stop rule.

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` -> `AGENTS.md` and `.claude/CLAUDE.md`; research must not recommend against them.

- **GSD enforcement:** file edits start through a GSD command; no direct repo edits outside a GSD workflow.
- **Tests:** reuse existing JUnit 5 / Mockito / Testcontainers setup, `ClickHouseBase`, `ClickHouseTestHelpers` (read it before writing tests); only add tests, never delete or weaken existing ones; new functionality needs feature-test coverage; type coverage asserts typed read-back; descriptive test names; negative tests for error handling.
- **Target:** ClickHouse `latest` + client V2 only; Docker is the colima profile `vz`.
- **Behaviour preservation:** code changes are limited to genuine bug fixes; Phase 15's only `src/main` changes are the user-approved D-01 and D-02 (plus review-forced fixes).
- **Rebasability:** keep changes additive and small.
- **Style:** Java 17, Lombok, SLF4J levels (TRACE per record, DEBUG per batch, INFO lifecycle), Google-style conventions via Spotless (`spotlessApply` on touched files only), static string constants extracted, DRY, comments only for non-obvious invariants.
- **Observability/security:** error messages clear and actionable, no secrets or data in logs, hot paths free of per-record allocation (the D-02 WARN is per table, not per record).
- **Release hygiene:** `VERSION` bumped and `CHANGELOG.md` updated (AGENTS.md "Before submitting a PR").
- **Dependencies:** do not upgrade any dependency without asking.
- **Git:** no commits to the branch and no PR title/description changes without asking; in this milestone the GSD plan flow plus the one blocking-human gate is the ask. `custom_build` is GSD's protected base branch.
- **Anchors:** a commit that changes a `fork-anchors.txt` anchor updates that file in the same commit; `verify-merge-invariants.sh` runs at phase end.

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `.planning/phases/15-.../15-CONTEXT.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/ROADMAP.md` (Phase 15, Sync Mechanics), `.planning/MERGE-CHECKLIST-v1.7.0.md` (Recorded Facts 25-37), `13-BEHAVIOUR-LEDGER.md`, `13-REVIEW.md`, both `*-REVIEW-DISPOSITION.md`, `13-VERIFICATION.md`, `14-VERIFICATION.md`, `docs/FORK-DELTA.md`, `CHANGELOG.md`, `VERSION`, `build.gradle.kts`, `.github/workflows/tests.yaml` (and `git show c61d90ce:` of it), the other five workflows' triggers, `.planning/scripts/verify-merge-invariants.sh` and `fork-anchors.txt`, `MetadataFreeJsonWriter.java`, `ClickHouseWriter.java` (current and `c61d90ce`), `ClickHouseHelperClient.java`, `Column.java` (current and `v1.7.0`), `KeeperStateProvider.java`, `State.java`, `Processing.java`, `ClickHouseSinkConfig.java`, and Phase 11 plan 11-06 plus summaries 11-04/11-05.
- Git/GitHub state queried this session: `git ls-remote origin`, `git rev-parse`, `git diff --stat`, `git tag -l`, REST `actions/runs?head_sha=` for `54b5ce2f` (1 run, success, 6m32s) and `c61d90ce` (2 runs: `custom_build` cancelled, `v1.6.0_gg1` success), REST `/rate_limit`.
- Live ClickHouse 26.9.14.10 probes (throwaway containers, removed): decimal-seconds and scientific-notation parsing, ZonedTimestamp under `best_effort`/`basic`, `system.columns` and `DESCRIBE ... describe_include_subcolumns=1` shapes, `system.settings_profile_elements` columns, KeeperMap needing `keeper_map_path_prefix`.
- `java` run of `BigDecimal.valueOf(v, 9)` string forms.

### Secondary (MEDIUM confidence)
- docs.github.com workflow syntax: branches-only filter does not run for tag pushes (WebFetch quote).
- clickhouse.com KeeperMap engine page: supports INSERT, DELETE, TRUNCATE, ALTER UPDATE/DELETE.
- clickhouse.com `replicated_deduplication_window` / `non_replicated_deduplication_window` (search result text): hash covers field names, types and part data; non-replicated window default 0.
- javadoc.io `io.debezium.time` (search result): `NanoTimestamp` INT64 nanoseconds.

### Tertiary (LOW confidence)
- Gradle up-to-date semantics for output-only tasks (not exercised).
- `ubuntu-latest` migration date (from STATE.md).

## Metadata

**Confidence breakdown:**
- Standard stack / tooling: HIGH - everything probed on this machine.
- Architecture (sequence, assertions): HIGH - derived from Phase 11/14 artifacts and re-verified git/REST state.
- Route matrix and D-07 analysis: HIGH for the code diff; MEDIUM for the dedup-semantics inference (A1).
- D-01 encoding: HIGH - live server evidence at 3 and 9 digits; pre-1970 values unprobed.
- D-02 design: MEDIUM - detection facts verified live; the dedup mechanism and static-set choice are recommendations.
- Pitfalls: HIGH for repo-derived ones; MEDIUM for GitHub-side ones.

**Research date:** 2026-10-10
**Valid until:** 2026-10-17 (the ClickHouse `latest` image, the `ubuntu-latest` move on 2026-10-19, and the origin ref state are the fast-moving inputs; re-check `git ls-remote origin` and the image build label at plan execution)
