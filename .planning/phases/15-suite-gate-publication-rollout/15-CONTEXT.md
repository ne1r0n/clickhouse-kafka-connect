# Phase 15: Suite Gate, Publication & Rollout - Context

**Gathered:** 2026-10-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish `v1.7.0_gg1`. In order:

- Land two small, user-approved main-code changes from the open Phase 13 review: the 13-REVIEW WR-02 fix (D-01) and the WR-05 startup WARN (D-02).
- Write the release documents: `VERSION`, a `CHANGELOG.md` `1.7.0_gg1` section, the `docs/FORK-DELTA.md` update, and a new `docs/ROLLOUT-v1.7.0_gg1.md`.
- Close out the open review dispositions and run the retroactive security audits for Phases 13 and 14.
- Pass the full suite twice on ClickHouse `latest` × client V2, the second run with `--rerun`. Then rehearse CI on `sync/v1.7.0`.
- Stop at one blocking human approval.
- Bring `custom_build` forward by a non-force `--no-ff` merge, push tags `v1.7.0_gg1` and `pre-v1.7.0-sync`, and prove exactly one green `tests.yaml` run on the published SHA (CI-08 end to end).

Requirements: SUITE-04, PUB-01, PUB-02, PUB-03, PUB-04, ROLL-01.

**Naming trap.** PUB-01's "WR-02 strictness" means the *milestone* follow-up WR-02: a nested null in a non-nullable slot now throws `DataException` (WRT-08, ledger L-10). It is **not** 13-REVIEW WR-02, the Debezium Timestamp encoding fixed by D-01 below. This document always writes the review finding as "13-REVIEW WR-02".

**Not in this phase:**
- Fixing the Enum/Tuple nullability root cause. It stays backlog 999.1.
- Fixing any other open review finding (D-03).
- Client `0.10.0`.
- Actually deploying to production. The rollout doc is a checklist for operators, not a deployment.
- Publishing the post-publication planning tail (D-14).

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- **Phase 11 D-01:** `custom_build` moves only by a forced `--no-ff` merge, so a single `git revert -m 1` undoes the release. Local and remote `custom_build` are both `c61d90ce`, an ancestor of `sync/v1.7.0`. A fast-forward would therefore be possible, and `--no-ff` must be explicit.
- **Phase 11 D-02 and Phase 14 D-11:** CI-08's evidence is the run on the published SHA, located by `head_sha` plus workflow path (`.github/workflows/tests.yaml`), with exactly one completed run and no cancelled sibling.
- **Phase 11 D-03:** there is one blocking human gate, immediately before the `custom_build` push. `gsd-executor` refuses approvals relayed by the orchestrator, so the orchestrator runs every post-gate task inline.
- **Phase 11 D-04:** push the branch and the named tags explicitly. Never use `git push --tags`: the local repo holds upstream's `v0.0.x`/`v1.x` tags, and they must not reach `origin` (PUB-04). Delete `origin/sync/v1.7.0` once it is merged.
- **Phase 11 D-10:** the CI duration is measured against the 22.5-minute threshold, half of `timeout-minutes: 45`.
- **Phase 11 D-11:** a red CI run is attributed before anything is touched (re-run locally on `latest` first). There are no blind re-runs.
- **Phase 14 D-10:** push the published SHA on **one branch only**. Two branches at one SHA share a concurrency group and cancel each other.
- **Inherited rules:** existing tests are never deleted or weakened. Type coverage asserts typed SQL read-back. Gradle is always invoked as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. `spotlessApply` runs only on touched files. `verify-merge-invariants.sh` runs at the end of the phase. A commit that changes a `fork-anchors.txt` anchor updates that file in the same commit.
- **Already true for PUB-04:** the 22 post-`c61d90ce` planning commits from local `sync/v1.6.0` are ancestors of `sync/v1.7.0`, so the `--no-ff` merge publishes them. `.planning/.gsd-ws-arg` is not tracked; re-check both facts at the gate.

### Open review findings vs. publication
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

### Rollout checklist and release documents
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

### Pre-approval audits and sequencing
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

### Publish mechanics
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and locked decisions
- `.planning/ROADMAP.md` §"Phase 15: Suite Gate, Publication & Rollout": the goal, the five success criteria, and the note (blocking-human gate, inline post-gate tasks, protected `custom_build`). Also §"Sync Mechanics".
- `.planning/REQUIREMENTS.md`: the locked-decisions table, SUITE-04, PUB-01..04 and ROLL-01. Read the dated corrections under WRT-09/WRT-08 (D-09 narrowing), JSONP-08 (D-13) and BLD-13 (lz4 floor 1.11.4); FORK-DELTA prose must match them.
- `.planning/STATE.md` §Blockers/Concerns: the env quirk, no `gh` (REST API only, 60 requests per hour), the [Publish] and [CI] residuals, the [Prod] rollout list, and the [Phase 13] 13-REVIEW WR-02 note that D-01 resolves.
- `.planning/milestones/v1.6.0_gg1-sync-phases/11-full-suite-ci-publication/11-CONTEXT.md`: D-01..D-04 and D-10..D-17, the publication template this phase mirrors.
- `.planning/phases/14-dependency-bumps-ci/14-CONTEXT.md`: D-10 (branch-only trigger, one branch per SHA), D-11 and D-13 (the scratch push already done at `54b5ce2f`).

### Prose sources
- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`: L-01..L-19, §"R-01 backfill procedure" (copied as written), §"Nested-null semantics by route", §"Known JSON-route gaps", §"Phase 15 inputs by destination" (the CHANGELOG, FORK-DELTA and ROLL-01 mapping).
- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-REVIEW.md`: WR-02 (the D-01 fix and its suggested code), WR-05 (the D-02 WARN suggestion), WR-01/03/04 (D-03).
- `.planning/phases/14-dependency-bumps-ci/14-REVIEW.md`: WR-01/WR-02 and info items (D-03).
- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md` and `.planning/phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md`: the rows D-03 updates.
- Phase 14 SUMMARYs (`.planning/phases/14-dependency-bumps-ci/14-0*-SUMMARY.md`): the lz4 1.12.0 OSV and `dependencyInsight` evidence, the CI fork count, and the run `38037397969`, all for CHANGELOG and FORK-DELTA.
- `.planning/MERGE-CHECKLIST-v1.7.0.md`: Recorded Facts (compression 25/26, lz4) cited by FORK-DELTA rows.

### Documents being written
- `docs/FORK-DELTA.md`: all sections. "Known costs and open items" and "Named skip inventory" are the gate's reference.
- `CHANGELOG.md`: upstream's `# 1.7.0, 2026-09-30` section style.
- `VERSION`, and `build.gradle.kts` around line 424 (where `Version.java` is generated).

### Code touched (D-01, D-02)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java`: `writeLogicalValue` (around 186–226), the logical-name constants (around 47–48), and the Javadoc (around 183).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java` and `src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java`: the pins that flip.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`: the Enum branch (around 207) and Tuple (around 234) that drop nullability. **Read-only context for D-02. Do not change.**
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`: `describeTable`/`describeTableV1/V2` (around 450–555), a candidate D-02 site.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`: `extractTablesMapping` call (around 162), the describe fallbacks (around 1201 and 1756), and the nested-null guards (around 683–742), for context.
- `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java`: the state table and query shape for D-07's KeeperMap check.

### CI and publication
- `.github/workflows/tests.yaml`: the branch-only trigger and the SHA-keyed concurrency group. `git show c61d90ce:.github/workflows/tests.yaml` shows the old `on: [push]`, the reason for the extra `pre-v1.7.0-sync` run.
- `.planning/scripts/verify-merge-invariants.sh` and `.planning/scripts/fork-anchors.txt`: the phase-end gate.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MetadataFreeJsonWriter.writeLogicalValue` already has the decimal-seconds pattern for Connect `Timestamp` (3 digits) and `MicroTimestamp` (6 digits). D-01 adds two cases of the same shape.
- `MetadataFreeJsonTypeParityLiveTest` already runs twin-table RowBinary-vs-JSON parity with typed read-back. D-01's live pin is one more cell, with the same seen-RED mutation approach as 13-02.
- `ClickHouseHelperClientForkUnitTest` tests describe-row predicates Docker-free (for example `shouldSkipDescribeRow`). That is the pattern for D-02's raw-type detection predicate.
- `13-BEHAVIOUR-LEDGER.md` already holds draft-quality prose for most CHANGELOG, FORK-DELTA and rollout items, keyed by destination.
- CI runs are checked through the unauthenticated REST API: `actions/runs?head_sha=<sha>`, filtered by `path`; `/jobs`; `/artifacts`. The fork-count value comes from a `::notice` annotation, because job logs return 403 without admin rights.

### Established Patterns
- Behaviour ledger entries (L-nn) for every observable change, consumed by the release documents.
- A seen-RED mutation for every new pin. Evidence and Recorded Facts go into SUMMARYs and checklists, not into committed tests that drift with `latest`.
- A pin that changes says why next to the assertion: a tightening, never a weakening (WRT-09 precedent).
- Publication gates use measured evidence only (11 D-03, D-12).

### Integration Points
- D-02's WARN hooks into table-metadata discovery, which `ClickHouseWriter.start()` and `TableMappingRefresher` both run. Dedup is needed so the periodic refresh doesn't repeat it.
- `VERSION` feeds the generated `Version.java`, which feeds the MBean `version=` segment (`MBeanNameUnitTest`/`MBeanRegistrationLiveTest` read it dynamically; confirm). That is why VERSION must land before the gate.

</code_context>

<specifics>
## Specific Ideas

- The rollout doc is written for "anyone building from this fork". Each item answers what changed, how to tell whether you are affected, and what to do. It has no deployment specifics.
- The tag-push order exists because of the real cancel on `c61d90ce` (memory: Phase 11 orchestration gotchas). Even with the branch-only trigger, tags go out only after the `custom_build` run completes.
- The approval bundle should make the one-way push decidable without opening any planning file.

</specifics>

<deferred>
## Deferred Ideas

- **Enum8/Enum16/Tuple nullability root cause** (13-REVIEW WR-05 fix proper): stays ROADMAP backlog 999.1. D-02 only warns.
- **13-REVIEW WR-01, WR-03, WR-04; 14-REVIEW WR-01, WR-02; all info items**: published open with `deferred` dispositions (D-03). These are candidates for a future hardening phase or upstream PRs.
- **`ZonedTimestamp` handling on the metadata-free route**: documented only, if research finds a parse issue.
- **Publishing the post-publication planning tail**: rides on the next milestone's `--no-ff` merge (D-14).
- **Test-classpath `org.lz4:lz4-java 1.8.0` duplicate**: stays backlog 999.2.
- **Client `0.10.0`**: still a future requirement.

</deferred>

---

*Phase: 15-suite-gate-publication-rollout*
*Context gathered: 2026-10-10*
