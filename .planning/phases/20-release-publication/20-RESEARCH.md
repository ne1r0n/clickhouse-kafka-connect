# Phase 20: Release & Publication - Research

**Researched:** 2026-10-14
**Domain:** Fork release engineering: operator rollout note, version bump and changelog, a two-run suite gate, and a gated `--no-ff` publication to `origin/custom_build` (Java 17 / Gradle / GitHub Actions / ClickHouse `latest` x client V2)
**Confidence:** HIGH on repo mechanics and the dedup measurement; MEDIUM on rollout-note wording that has not been drafted yet

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `docs/ROLLOUT-v1.7.0_gg2.md` is a **delta note** on top of gg1: it covers only what changed gg1 -> gg2 and opens by pointing operators coming from pre-gg1 at `docs/ROLLOUT-v1.7.0_gg1.md` first. No content duplicated from the gg1 note. - **Reversibility:** one-way once pushed - `custom_build` is published; later restructuring needs a new commit.
- **D-02:** The 999.1 item gives operators a **ready, runnable SQL detection query per broken shape** - `Nullable(Enum)` slots read back as unexpected NULL (the pre-fix all-NULL read, ledger L-13) and the `Nullable(Tuple)` wrong-row + phantom-row symptom (19-01 spike side (a)) - each proven live on ClickHouse `latest` before publication. The note states rows are not repaired by the connector.
- **D-03:** Phase 16 writing disciplines carry: plain-language prose with planning IDs only as trailing ledger tags (16-D-02), and every quoted command/SQL in changed text run live with output matching the text, transcripts recorded in a Phase 20 command-evidence file under `.planning/phases/20-release-publication/` (16-D-07/D-08).
- **D-04:** The "verified on `latest`" evidence is a **fresh live probe**, not a citation of existing pins: insert a block, retry the same offset range with changed bytes (the gg2 serialization shape), and observe the dedup outcome against `replicated_deduplication_window`, with `SELECT version()` recorded in the phase evidence file.
- **D-05:** The probe is a **committed live JUnit test** beside the existing exactly-once tests, permanently pinning both halves: a byte-identical retry dedups, a changed-bytes retry duplicates (Phase 19 spike D-05 precedent: committed, not throwaway). - **Reversibility:** costly - the test becomes the quoted evidence surface for the rollout item; replacing it later means rebuilding the measurement.
- **D-06:** The rollout item gives a **procedure plus self-check**: stop connectors cleanly so no offset range is left in `BEFORE` state before upgrading, and a self-check query against the KeeperMap state table confirming no `BEFORE`-state ranges before restarting on gg2. Note that `exactlyOnce=false` pipelines are not affected by the dedup-token half.
- **D-07:** The pre-gate review is `gsd-code-reviewer`, incremental, over **everything Phase 20 itself commits**: the gg2 rollout note, the CHANGELOG section, the FORK-DELTA edits, the VERSION bump, and the new dedup live test (16-D-09 shape). Findings go into `20-REVIEW-DISPOSITION.md` before the approval is asked for. Phases 17-19 code is not re-reviewed - each already has a dispositioned review.
- **D-08:** Of the six open 19-REVIEW advisories, the **FORK-DELTA per-twin wording finding is fixed in Phase 20** (the phase edits that doc anyway); the code-side warnings (unreachable totality paths, vacuous test assertions) and the three infos are **carried to the milestone close record with a written reason each** - no code churn between Phase 19's green gate and the release. Dispositions recorded in `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` (Phase 16 D-10 precedent: flip rows where they live).
- **D-09:** `/gsd-secure-phase 19` runs as a Phase 20 prerequisite, **before the merge gate** - the release ships with every phase's threat check done and any finding can still be weighed at the approval.
- **D-10:** The `1.7.0_gg2` CHANGELOG section covers **behaviour plus a short Build/CI subsection**: Important Changes and Bug Fixes drawn from the Phase 18/19 behaviour ledgers (`18-BEHAVIOUR-LEDGER.md`, `19-BEHAVIOUR-LEDGER.md`), plus a brief note on the CI hardening (SHA-pinned actions, `ubuntu-24.04`, read-only token, `test-head.yaml` deleted) and `lombok.config` (artifact proven byte-identical). gg1's section structure is the template.
- **D-11:** After the tag is pushed, the orchestrator **presents the three manual checks with exact steps, waits for the user's answers in-session, and records the outcomes** in the milestone close record: Dependabot version-updates enablement, the default-branch intent (`main` at Phase 17, undecided), and that `dependabot.yml`'s `exclude-paths` key did not invalidate the config (17-REVIEW WR-01: the `gradle` entry must still run). No agent changes repository settings (D-05 milestone rule, F1).
- **D-12:** A **milestone audit runs** after Phase 20 verification, before `/gsd-complete-milestone`, replacing the stale start-of-milestone snapshot (`.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md`, 2026-10-10, untracked) - so `v1.7.0_gg2-hardening` closes clean instead of a third override closeout.

Also locked by ROADMAP/STATE (CONTEXT `<domain>`): the Phase 11/15/16 publication recipe (scratch push of `sync/v1.7.0` with an explicit refspec, never at a SHA another pushed branch holds; `--no-ff` merge from a throwaway worktree so the merged tree equals the rehearsed tip; push `custom_build` only; wait for its single run; push the tag by name; delete the scratch branch); the Phase 16 milestone-specific merge checks (two parents, first parent = `origin/custom_build`, merged tree = rehearsed tip; `verify-merge-invariants.sh` upstream-sync parent checks do NOT apply and must not be "fixed"; the anchors invariant still runs on the branch tip); the rollback point is the Phase 16 merge commit; every approval gate runs orchestrator-inline and needs the user's own explicit answer (a relayed approval is refused); the `VERSION` anchor row moves in the bump commit; gg1 rollout and CHANGELOG text stay as released; PUB-07 owns the AGENTS.md pre-PR obligation; IN-04 is copied into the milestone close record; mandated rollout content includes the 999.1 value change, the dedup-token retry consequence, the empty-CDC skip change, the MBean `version=` change, that the `Nullable(Tuple)` limitation is closed and its discovery WARN is gone, and ledger L-38.

### Claude's Discretion

- Plan split (likely shape: docs + live probe plan, suite-gate plan, inline publication plan - the gate must sit orchestrator-inline).
- Item ordering inside the gg2 note, exact plain-language titles, and the TOC shape.
- Exact SQL of the detection queries and the KeeperMap self-check, provided each is proven live per D-03.
- The dedup live test's class name, placement, and parameterisation.
- The merge commit message (by precedent it names what it publishes so a future `git revert -m 1` reads well) and the approval-bundle contents (Phase 15 D-12 precedent: measured, not cited).
- The scripted form of the merge checks (two parents, first parent, tree equality) and the FORK-DELTA raw-numbers refresh commands.

### Deferred Ideas (OUT OF SCOPE)

- The five carried 19-REVIEW advisories (two code warnings, three infos) - milestone close record, written reason each (D-08); any fix belongs to a future milestone.
- Upgrading the other upstream workflows (`integrationTests.yaml`, `release.yaml`, `pre-release.yaml`, `increment-version.yml`) off `@v3`-era actions - already in REQUIREMENTS Future.
- Repairing rows already stored wrongly by 999.1 - operator data; the connector never rewrites it (REQUIREMENTS Out of Scope; the rollout note documents detection only).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ROLL-02 | `docs/ROLLOUT-v1.7.0_gg2.md` gives operators the 999.1 value change (rows already stored wrongly are not repaired), the dedup-token retry consequence verified on `latest`, the empty-CDC skip change and the MBean `version=` change, reviewed before the publication gate | Ledger rows L-23..L-40 map to items (Ledger-to-Document Map); the dedup measurement (Key Finding 1) with the live SQL results; KeeperMap self-check proven live with the repo's own keeper config; gg1 note as the item-format template |
| PUB-07 | `VERSION` is `v1.7.0_gg2`, `CHANGELOG.md` has a plain-language `1.7.0_gg2` section, `docs/FORK-DELTA.md` has a row for every new fork-side patch | Version-bump mechanics (anchors row, `clean`, generated `Version.java`); CHANGELOG template; FORK-DELTA coverage audit and the stale-text list; raw-numbers commands |
| SUITE-05 | Full suite green twice on `latest` x V2 (second `--rerun`), 0 failures/errors, every skip named, `testUnit` green with Docker stopped | Proven Phase 19 gate command shape, expected counts and skip set, colima stop/start procedure |
| PUB-08 | Non-force `--no-ff` merge after the user's own explicit approval, exactly one green CI run on the published SHA, tag only after green, rollback tag at a commit carrying the branch-only trigger | Publication recipe with ordering constraints, the rollback-tag facts, CI-run observation, tag/branch-trigger facts |
</phase_requirements>

## Summary

Phase 20 is release engineering on a tree whose code is frozen: 119 commits on `sync/v1.7.0` ahead of `origin/custom_build` (`be6b8861`), of which only 38 files under `src/`, `build.gradle.kts`, `lombok.config`, `.github/` and `docs/FORK-DELTA.md` are non-`.planning` changes [VERIFIED: `git rev-list --left-right --count origin/custom_build...HEAD` printed `4 119`; `git diff --stat origin/custom_build HEAD -- . ':(exclude).planning'` ended `38 files changed, 5012 insertions(+), 273 deletions(-)`; non-`src` paths are `.github/dependabot.yml`, `.github/workflows/test-head.yaml`, `.github/workflows/tests.yaml`, `build.gradle.kts`, `docs/FORK-DELTA.md`, `lombok.config`]. The work is almost all documents plus one new committed live test, then gates, then the approved publication. Nearly every mechanism (scratch push, worktree merge, REST-API run check, colima stop/start, two-run gate command) already exists from Phases 11, 15, 16 and 19 and should be reused, not reinvented.

**The most important finding for planning contradicts a locked decision's premise.** D-05 says the dedup live test pins "a changed-bytes retry duplicates". Measured on ClickHouse `26.9.17.8` this session, an insert carrying the same `insert_deduplication_token` is **dropped even when its bytes differ**; a different token with identical data **lands**. The connector sends the token `topic-partition-minOffset-maxOffset` on every partition-aware insert. So the real gg1 -> gg2 retry consequence is "the retried gg2 block is dropped if the gg1 attempt landed (its old content, including phantom rows, stays), and lands if it did not", never "rows written twice". The planner must make the live test discover and pin the observed outcome rather than assert duplication, and must flag the premise change to the user (Open Question 1), because the published gg1 note item 1 reasons the other way (it says deduplication "hashes the parsed data" and that a differently encoded retry can "write rows twice") and D-06's "exactlyOnce=false is not affected" needs qualification.

**Primary recommendation:** Order the plans so that all non-`.planning` content is frozen before the rehearsal push: (1) live dedup test and its evidence, (2) VERSION bump + anchors row + CHANGELOG + FORK-DELTA + gg2 rollout note, (3) `gsd-code-reviewer` over Phase 20's own commits and `/gsd-secure-phase 19`, (4) the gate (cache-busted `testUnit` with Docker stopped, then two full runs), (5) rehearsal push, approval bundle, user's own approval, merge/push/tag, all orchestrator-inline.

## Key Finding 1: What the dedup token really does (measured)

All results below were produced this session against `clickhouse/clickhouse-server:latest` (`SELECT version()` -> `26.9.17.8`), over the HTTP interface, `insert_deduplication_token` passed as a URL setting. [VERIFIED: live probe, this session]

| Probe | Table engine | Outcome |
|-------|--------------|---------|
| Insert 2 rows with token `T1`; retry with **1 row** (changed bytes) and token `T1` | `MergeTree` with `non_replicated_deduplication_window=100` | Retry **dropped** |
| Retry the identical 2 rows with token `T1` | same | Dropped |
| Same 2 rows with a **different** token `T2` | same | **Landed** (id 0 count 2, id 1 count 2) |
| No token: identical 2-row insert twice, then a 1-row insert | same | Identical repeat dropped by data hash; changed data landed (3 rows total) |
| Same token twice | plain `MergeTree`, no window setting (default `non_replicated_deduplication_window` = 0) | **No dedup**, count 2 |
| Insert 2 rows with `T1`; 1-row retry with `T1`; 1-row insert with `T2` | `ReplicatedMergeTree` on the embedded keeper from `src/test/resources/clickhouse_keeper_map.xml` | Changed-bytes same-token retry dropped; different token landed (id 0 count 1, id 1 count 2) |

Server defaults read from `system.merge_tree_settings` on the same server: `non_replicated_deduplication_window 0`, `replicated_deduplication_window 10000`, `replicated_deduplication_window_seconds 3600`, `replicated_deduplication_window_for_async_inserts 10000`. From `system.settings`: `insert_deduplicate 1`, `async_insert_deduplicate 0`, `insert_deduplication_token` empty. [VERIFIED: live query, this session] The official settings page states "only 100 of the most recent inserts for each partition are deduplicated" for replicated tables [CITED: https://clickhouse.com/docs/reference/settings/session-settings/insert]; the measured `10000` wins for the rollout wording, and the live test should read the value rather than quote either. The `3600` second window means a retry after more than an hour is not deduplicated even inside the count window - say so in the note.

Corroboration: the token "will be used for deduplication instead of calculating a checksum from the inserted data" and an insert with the same token but different data "will be deduplicated" [CITED: https://kb.altinity.com/altinity-kb-schema-design/insert_deduplication/]. The setting "is used for deduplication *only* when not empty" and is "tracked per partition" [CITED: clickhouse.com settings page above].

Connector facts that make this apply [VERIFIED by reading the files this session]:
- `QueryIdentifier.getDeduplicationToken()` returns `null` when `partition == -1`, otherwise `String.format("%s-%s-%s-%s", topic, partition, minOffset, maxOffset)` (`src/main/java/com/clickhouse/kafka/connect/util/QueryIdentifier.java`).
- `Processing.doInsert(List<Record>, RangeContainer)` builds `new QueryIdentifier(topic, ...getPartition(), rangeContainer.getMinOffset(), rangeContainer.getMaxOffset(), UUID.randomUUID().toString())` (`Processing.java:62-64`); the other overload `doInsert(List<Record>)` builds the partition-less identifier, so no token (`Processing.java:88`).
- `ClickHouseWriter` sets the token on all three insert paths: `insertSettings.setDeduplicationToken(deduplicationToken)` at lines 1257, 1532 and 1680, and `request.set("insert_deduplication_token", deduplicationToken)` at line 1759. It is not conditional on `exactlyOnce`. So `exactlyOnce=false` connectors also send the token for partition-aware batches (the token-less path is `ignorePartitionsWhenBatching` per `ClickHouseSinkTask.java:107-111` text "the dedup token has no partition component (null)").
- Existing Docker-free pins: `EmptyCdcExactlyOnceStateTest` asserts `TOPIC + "-0-10-" + lastOffset` as the token and byte-identical bodies on retry (`src/test/java/.../processing/EmptyCdcExactlyOnceStateTest.java`); `CapturingClickHouseWriter` captures `body()` and `deduplicationToken()`.

Consequences to carry into the rollout item (the planner/author decides final wording; the live test fixes the facts):
1. Upgrade with a batch left in `BEFORE_PROCESSING` by a gg1 jar: if the gg1 attempt reached the server, the gg2 retry (same token, different bytes: no phantom row for an empty CDC record, correct Enum/Tuple values) is **dropped**, so the old attempt's rows - phantom rows or wrongly stored Enum/Tuple values - stay. If the gg1 attempt did not land, the gg2 block lands correctly. Nothing is written twice. (Ledger L-30 already says the first half in prose.)
2. The condition for any dedup at all: a table with a non-zero window (`ReplicatedMergeTree` default 10000 blocks / 3600 s; non-replicated needs `non_replicated_deduplication_window`, default 0).
3. `exactlyOnce=false`: the token is still sent for partition-aware batches, and a re-delivered identical range after a restart is dropped the same way on a replicated table. Whether to call that "not affected" (D-06) is a wording decision for the user; the safe statement is that the connector keeps no offset state in that mode, so the stop-cleanly procedure and the KeeperMap check do not apply, while the token behaviour on replicated tables still does.
4. Rollback from gg2 to gg1 has the mirror image (a gg1 retry of a gg2 range is dropped by token).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Operator rollout note, CHANGELOG, FORK-DELTA | Repository docs (`docs/`, `CHANGELOG.md`) | `.planning/` evidence | Ships inside the merged tree; evidence rides under the `.planning/` allow-list |
| Dedup consequence evidence | ClickHouse server (block deduplication) | Connector writer (token construction) | The outcome is decided by the server; the connector only chooses the token |
| Version identity (`v1.7.0_gg2`) | Build (`VERSION` -> `generateVersionFile`) | JMX `version=` and client name | `Version.ARTIFACT_VERSION` feeds both; stale until `clean` |
| Suite gate (two runs + Docker-stopped `testUnit`) | Local Gradle + colima `vz` | CI single run on published SHA | Local runs prove the tree; CI run proves the runner |
| Publication (merge, push, tag) | Git / `origin` | GitHub Actions (`tests.yaml`) | Branch-only trigger decides which pushes start a run |
| Approval gates | Orchestrator session | - | Executors refuse relayed approvals |

## Standard Stack

No new libraries or build dependencies are introduced by this phase. The dedup live test uses the existing JUnit 5 / Testcontainers / client-v2 stack through `ClickHouseBase` and `ClickHouseTestHelpers`. [VERIFIED: CONTEXT code_context; `.claude/CLAUDE.md` Technology Stack]

### Core (existing, reused)
| Tool | Version | Purpose |
|------|---------|---------|
| Gradle wrapper | 7.4.2 per `.claude/CLAUDE.md` [CITED: project instructions] | `clean`, `test`, `testUnit`, `reportTestForks` |
| JUnit 5 / Mockito / Testcontainers | 5.11.3 / 5.21.0 / 1.21.4 per project instructions | New live test lives beside `EmptyCdcSkipRouteLiveTest` etc. |
| `.planning/scripts/verify-merge-invariants.sh` + `fork-anchors.txt` | in repo | Anchors invariant on the branch tip (`bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`) |
| GitHub REST API (unauthenticated) | - | `actions/runs?head_sha=` for run observation; 60 requests/hour |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Unauthenticated REST for run status | `gh` | `gh` is deliberately not installed (STATE [Tooling]) |
| Reading job logs | Read-only Keychain token `gsd17-gh-actions-read` | Only needed for log-level evidence; the run's conclusion needs no token. Offer it up front if log content is wanted; revoke afterwards |

**Installation:** none. **Package Legitimacy Audit:** no external packages are added in this phase, so no `package-legitimacy` check applies. Packages removed (SLOP): none. Flagged (SUS): none.

## Architecture Patterns

### Publication flow (orchestrator-inline from step 5)

```
 docs + live test + VERSION bump (non-.planning content FROZEN here)
        |
        v
 gsd-code-reviewer over Phase 20 commits  -> 20-REVIEW-DISPOSITION.md (open 0)
 /gsd-secure-phase 19                      -> 19-SECURITY.md
        |
        v
 SUITE GATE (local):  clean -> testUnit --rerun with colima vz stopped
                      -> colima start -p vz -> test --rerun (run 1) -> test --rerun (run 2)
                      -> verify-merge-invariants.sh 54e63648 HEAD  (anchors)
        |
        v
 rehearsal: push sync/v1.7.0 as ONE scratch branch (explicit refspec)  -> exactly 1 green tests.yaml run on X
        |
        v
 approval bundle (measured) -> USER'S OWN explicit answer (orchestrator-inline)
        |
        v
 rollback-tag re-check on be6b8861 (item 8 commands) ; create rollback tag (no push triggers CI)
        |
        v
 throwaway worktree on custom_build: git merge --no-ff <X by hash>  -> M
 checks: 2 parents, M^1 = origin/custom_build, tree(M) = tree(X), no state.json / .gsd-ws-arg
        |
        v
 push custom_build only (refs/heads/custom_build:refs/heads/custom_build, no force, no tag)
        |
        v
 exactly ONE tests.yaml run on M, green, re-check >= 120 s later (no cancelled sibling)
        |
        v
 push tag v1.7.0_gg2 by name -> delete scratch branch -> 3 manual checks (D-11) -> audit (D-12)
```

### Ordering constraints that are easy to get wrong

1. **Everything outside `.planning/` must be final before the rehearsal push.** The rule "merged tree equals the rehearsed tip" is checked with `git diff --quiet M X`; Phase 16 allowed only `.planning/`-only commits between the rehearsed X0 and the merged X [VERIFIED: 16-03-PLAN/16-03-SUMMARY: "X0..X touches only .planning/"]. `docs/` and `CHANGELOG.md` are NOT under `.planning/`, so a late rollout-note fix after the rehearsal forces a new rehearsal. Review the docs before the rehearsal.
2. **VERSION bump before the gate runs.** `Version.java` is generated and "not an input of the task" so a bump needs `clean` [CITED: docs/FORK-DELTA.md row for `VERSION`]. `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java` exists in the working tree now and is stale for the bump [VERIFIED: `ls` this session]. No test or `src/main` file hard-codes `gg1` [VERIFIED: `grep -rn "gg1" src build.gradle.kts gradle .github` printed nothing], so the bump needs no source edits.
3. **Raw-numbers refresh after the last `src`/`build`/`gradle`/`.github` change** (the new live test adds lines to the `src` scope). The FORK-DELTA text says "after" figures are measured at the commit that carries the document and reproduce against the tag; for gg2 the tag points at merge commit M, whose `src` tree equals X, so figures measured on X reproduce with `git diff --shortstat v1.7.0 v1.7.0_gg2 -- ...`.
4. **State and audit files:** `.planning/state.json` has uncommitted local edits and `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` is untracked [VERIFIED: `git status --short` at session start]. A worktree merge by hash cannot carry them, but the Phase 16 gate also asserts `.planning/.gsd-ws-arg` is absent from M's tree and the merge worktree is clean. Keep that check. The stale audit file is replaced by the D-12 audit, not committed as is.

### Recommended plan split (Claude's discretion, per CONTEXT)

| Plan | Contents | Gate type |
|------|----------|-----------|
| 20-01 | Dedup live test (probe first, then pin), `20-COMMAND-EVIDENCE.md` skeleton with `SELECT version()` | autonomous |
| 20-02 | VERSION bump + `fork-anchors.txt` VERSION row in one commit; CHANGELOG `1.7.0_gg2`; FORK-DELTA edits (stale text, per-twin wording fix, raw numbers, any missing rows); 19-REVIEW-DISPOSITION flips | autonomous |
| 20-03 | `docs/ROLLOUT-v1.7.0_gg2.md` with every quoted command/SQL run live and recorded | autonomous |
| 20-04 | `/gsd-secure-phase 19`; `gsd-code-reviewer` over Phase 20 commits; dispositions to `open: 0` | autonomous (reviewer rule: findings as `### ID: title`) |
| 20-05 | Suite gate (Docker-stopped `testUnit`, two full runs, anchors invariant, scope check) | autonomous, long (run in background) |
| 20-06 | Rehearsal push, approval bundle, user approval, merge, push, CI wait, tag, scratch delete | `autonomous: false`, orchestrator-inline after the gate |
| 20-07 | Manual checks presentation (D-11), IN-04 and advisories into close record | orchestrator-inline |

### Anti-Patterns to Avoid

- **Asserting "changed-bytes retry duplicates" in the live test.** It will fail (measured). Probe, then pin the observed result (Key Finding 1).
- **"Fixing" `verify-merge-invariants.sh` parent checks** for this merge. Milestone rule says they do not apply [CITED: ROADMAP Phase 20 note].
- **Pushing a tag or a second branch at the same SHA** while a branch run is in flight. Two branches at one SHA cancel each other (STATE [CI]).
- **Committing bookkeeping onto `custom_build`** from an executor. `custom_build` is GSD's protected base branch; post-publication `.planning` commits live on local `sync/v1.7.0` (memory note on Phase 11 gotchas).
- **Editing the gg1 rollout note or the gg1 CHANGELOG section.** Locked as released; gg2's correction of the gg1 dedup reasoning goes into the gg2 note.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Two-run gate verdict | Eyeballing Gradle output | The Phase 19 `19-10-PLAN.md` Task 1 `<automated>` command (XML sums, skip-set equality, stale-timestamp check), with updated expected constants | Proven; guards against Gradle cache echo and stale XML |
| Docker-stopped `testUnit` | Bare `colima stop` | `colima stop --profile vz`, then `docker info` must fail, then `testUnit --rerun`, then `colima start --profile vz` | Bare `colima` targets the `default` profile; precedent 15-07-SUMMARY |
| Merge checks | New script from scratch | Adapt Phase 16's `MERGE-SHAPE-OK` / allow-list / `TREE(M)==TREE(X)` blocks from `16-COMMAND-EVIDENCE.md` and `16-03-PLAN.md` | Already proven against the real repo with a dry-run merge |
| Rollback-command proof | Reading the text | The E-16 `demo2.sh` pattern (16-COMMAND-EVIDENCE): run item 8's commands on a throwaway branch against the actual merge commit | Phase 16 found item 8 broken on the post-publication tip |
| Run observation | Browser | `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M>` filtered by workflow path | Used in Phases 15-17; no token |
| KeeperMap in a test/probe server | Custom keeper image | Mount `src/test/resources/clickhouse_keeper_map.xml` to `/etc/clickhouse-server/config.d/keeper_map.xml` (what `ClickHouseBase` does at lines 120-124) | Gives both KeeperMap and `ReplicatedMergeTree` on a standalone server |

**Key insight:** every risky step in this phase has a prior proven script; the new risk is only ordering (what may change after the rehearsal) and the dedup premise.

## Runtime State Inventory

Not a rename or migration phase; the `VERSION` string change is a version bump, not a rename. The categories that do carry the version string, answered explicitly:

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | None - the connector's KeeperMap state stores offsets and state names, not the version | None |
| Live service config | JMX MBean name `...,version=v1.7.0_gg1` and client name `clickhouse-kafka-connect/v1.7.0_gg1` are runtime-derived from `Version.ARTIFACT_VERSION`; operator dashboards/exporter rules and `system.query_log.http_user_agent` filters may spell the old value | Rollout note MBean/client-name item (gg1 item 7 already tells operators to wildcard `version=`; gg2 note states the new values and points back) |
| OS-registered state | None in this repo | None |
| Secrets/env vars | Keychain item `gsd17-gh-actions-read` (read-only Actions token) from Phase 17 | Revoke on GitHub and `security delete-generic-password -s gsd17-gh-actions-read` when no longer needed (STATE [Env]) |
| Build artifacts | Stale generated `Version.java` (gg1) in `build/`; local `build/` result dirs | `./gradlew clean` before the gate; verify `ARTIFACT_VERSION` after |

## Ledger-to-Document Map

Source rows: `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` (L-23..L-30) and `.planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md` (L-31..L-41), each with its own "Phase 20 destination" column [VERIFIED: both files read this session].

| Ledger | Operator-visible change | CHANGELOG | Rollout note (ROLL-02) | FORK-DELTA |
|--------|------------------------|-----------|------------------------|-----------|
| L-23 | Message-less writer exception surfaces as itself (V1 RowBinary); message-less `SocketTimeoutException` now retried | yes | optional log note | row exists (item 43) |
| L-24 | `Utils.handleException` no longer NPEs on null-message IOException | yes | optional | row exists (item 52) |
| L-25 | Missing non-nullable value -> `DataException` naming column and type (exact text in ledger) | yes | yes | row exists (item 42) |
| L-26 | ERROR line `Column <name> is not nullable and no value is provided` gone; alerts must move | yes | **yes - log-alert note** | covered in row 42 |
| L-27 | DateTime parse-failure ERROR log text changed, value never logged | yes | log-alert note | row exists (item 40) |
| L-28 | Conversion `DataException` gains ` for column '<name>'` | yes | - | row 40 |
| L-29 | Generic write-failure log text changed, no stack trace; rethrown message residual (backlog 999.3) | yes | log-alert note | row exists (item 41) |
| L-30 | Empty-CDC skip on bypass and RowBinary routes; phantom rows not removed; phantom-row query; upgrade note on retried batch | yes | **yes - mandated** | row exists (item 44) |
| L-31..L-33 | Enum fix; omitted Enum field stores NULL; nested null entries store NULL (reverses L-13); rows not repaired | yes | **yes - mandated** | row exists (item 49) |
| L-34 | Discovery WARN narrowed to Tuple (intermediate; superseded by L-39) | fold into L-39 | fold | - |
| L-35..L-37 | `Nullable(Tuple)` round-trips; Array/Map of Nullable(Tuple); omitted field NULL | yes | **yes - mandated** | row exists (item 50) |
| L-38 | Single-field `Nullable(Tuple(a T))` with null `a` now throws `DataException` naming `tuple field 'a'`; stops the task under `errors.tolerance=none` | yes | **yes - carried WR-02** | stated in Known costs item 1 |
| L-39 | Nullable(Tuple) discovery WARN removed; log alerts keyed on it stop matching | yes | **yes - mandated** | row 50 |
| L-40 | New fail-open WARN `Could not take element nullability for column {} of table {}.{} from its declared type {} ({}); keeping the nullability DESCRIBE reported for it` | yes | yes - tells operators what the new WARN means | row 50 |
| L-41 | `SimpleAggregateFunction(f, Nullable(T))` element handling | no | no | row 50 text only |

Also needed beyond the ledgers: the MBean `version=` segment and client name move `v1.7.0_gg1` -> `v1.7.0_gg2` (mandated; same mechanism as gg1 item 7, wildcard guidance should be restated in one line, not duplicated), the rollback item for gg2, and the corrected dedup reasoning (Key Finding 1).

Prior-phase operator-visible items that are NOT behaviour ledgers but belong in the CHANGELOG Build/CI subsection (D-10): SHA-pinned Node 24 actions, `ubuntu-24.04`, `permissions: contents: read`, `persist-credentials: false`, `reportTestForks` task, `test-head.yaml` deleted, `lombok.config`, test classpath lz4 duplicate excluded, Dependabot `github-actions` entry [VERIFIED: `.github/workflows/tests.yaml` read this session; FORK-DELTA rows 58-65].

## FORK-DELTA state (PUB-07, D-08)

Coverage: rows already exist for the Phase 17-19 patches: CI workflow row (58), `test-head.yaml` deletion (59), Dependabot (60), `testRuntimeClasspath` exclude (64), `lombok.config` (65); Phase 18 rows for null-message catch (43), `Utils.handleException` (52), missing-value `DataException` (42), DateTime logs (40), generic catch (41), empty-CDC skip (44); Phase 19 rows for the Enum branch (49) and declared-nullability pass (50) [VERIFIED: `docs/FORK-DELTA.md` read this session; row numbers are line numbers in the Read output]. Planner action: audit mechanically that every `fork-anchors.txt` Phase 18/19 row's `backs-ID` (WRT-10/11/12, JSONP-09, COLS-10) appears in a FORK-DELTA "Pinned by" cell, rather than re-deriving coverage by eye.

Stale text that Phase 20 must change (all still say gg1):
- Intro (lines 5 and 7): "published as `v1.7.0_gg1`", rollout pointer - add the gg2 note pointer, keep gg1's.
- Line 33: "currently `v1.7.0_gg1`" for `Version.ARTIFACT_VERSION`.
- Line 61: the `VERSION` row "reads `v1.7.0_gg1`".
- Lines 107-123: raw numbers - "after" endpoint `v1.7.0` to `v1.7.0_gg1` (95 files / 13587 / 484; 31 / 1826 / 130). Re-measure at X; decide whether to keep gg1 as an extra row (recommended, so the table shows the gg1 -> gg2 growth).
- Line 192: "the release is `v1.7.0_gg1`" paragraph in "For the next syncer".
- Line 50 (D-08 fix, 19-REVIEW WR-03): the row states `CANNOT_READ_ALL_DATA` on the plain twin and `ATTEMPT_TO_READ_AFTER_EOF` on the defaults twin, but `NullableTupleMatrixLiveTest` only requires either code (`READ_PAST_END_CODES`). Fix by softening the doc to "a read-past-end code (`CANNOT_READ_ALL_DATA` or `ATTEMPT_TO_READ_AFTER_EOF`)" - the review's cheaper option and consistent with D-08's "no code churn" [CITED: 19-REVIEW.md WR-03 Fix]. Note 19-SPIKE.md measured `CANNOT_READ_ALL_DATA` on both twins for C1 (Bytes read 0, expected 1 on DEFAULT), so the doc's per-twin code is also contradicted by spike data; the softened wording is safe [VERIFIED: 19-SPIKE.md matrix tables, read this session].
- "Named skip inventory" (lines 162-186): 18 skips; Phase 19's gate measured 18 with the same names [VERIFIED: 19-10-SUMMARY]. Re-confirm from the Phase 20 gate; the new live test adds no skip.

The disposition flip for WR-03 goes in `19-REVIEW-DISPOSITION.md` (Source cell = the Phase 20 commit SHA); the other five advisories (WR-01, WR-02, IN-01, IN-02, IN-03; IN-04 is already `deferred`) get written reasons there and in the milestone close record. Note the disposition file currently shows `open: 6` while 19-10-SUMMARY records dispositions at `open 0` and the file read this session still lists WR-01/02/03, IN-01/02/03 as `open` - these are the post-19-10 advisories from the fresh review at `7d50944b` [VERIFIED: 19-REVIEW-DISPOSITION.md frontmatter `open: 6`, `total: 8`]. The planner should treat the file as authoritative and flip rows there.

## CHANGELOG and VERSION mechanics (PUB-07)

- Heading style of gg1: `# 1.7.0_gg1, 2026-10-10`, followed by a plain intro paragraph linking the FORK-DELTA and the rollout checklist, then `## Important Changes`, `## Bug Fixes`, `## Dependencies` [VERIFIED: `CHANGELOG.md` lines 1-45 read this session]. gg2 goes above it as `# 1.7.0_gg2, <publication date>`; add a short `## Build and CI` subsection (D-10). Phase 17-19 changed no shipped dependency versions in the ledgers, so a Dependencies subsection is probably empty - verify with `git diff origin/custom_build HEAD -- gradle/libs.versions.toml`.
- Writing discipline (16-D-02): plain language, no bare planning IDs; ledger tags only as trailing "(ledger L-xx)" in the rollout note. The CHANGELOG gg1 section has none.
- `VERSION` file content is the single line `v1.7.0_gg1` [VERIFIED: `cat VERSION`]; `build.gradle.kts:48` is `version = file("VERSION").readText().trim()`; `generateVersionFile` writes `ARTIFACT_VERSION = "${project.version}"` [VERIFIED: `build.gradle.kts` lines 48, 435-455]. "After `clean` the generated version agrees" is checked by `./gradlew clean compileJava` then reading `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`.
- Anchors row to move in the SAME commit as the bump: the tab-separated line `VERSION	v1.7.0_gg1	1	BLD-07` in `.planning/scripts/fork-anchors.txt` [VERIFIED: file read this session]. The invariant script evaluates anchors at every commit in the range, so a split commit fails it.
- `Version` is consumed by `MBeanNameUnitTest` (via the generated constant); no literal.

## Suite gate (SUITE-05)

Proven command shape: `19-10-PLAN.md` Task 1 `<automated>` [VERIFIED: read this session]. It deletes both result directories, runs `testUnit --rerun`, then `test --rerun` twice, and checks the XML of each run (timestamps newer than the last code commit, missing classes, skip set equal to the 18 named skips: 7 named methods plus exactly 11 `ClickHouseSinkTaskWithSchemaTest#[n]` cases). Adapt it:
- Update the "must-exist" class lists and the expected totals. Phase 19's last gate: `testUnit` 45 classes / 556 tests / 0 / 0 / 2 skipped; full runs 84 classes / 980 tests / 0 / 0 / 18 skipped [VERIFIED: 19-10-SUMMARY]. Phase 20 adds at least one class, so expect 85+ classes; do not hard-code 84/980.
- Prefix every Gradle call with `env -u TESTCONTAINERS_HOST_OVERRIDE` [VERIFIED: STATE [Env]; `.planning/config.json` `test_command`].
- Begin with `./gradlew clean` (first run only) so the bump is in the artifact; second run uses `--rerun` (a bare second `./gradlew test` echoes the first run) [CITED: STATE decisions].
- Copy run 1's result XML aside before run 2 (15-07-SUMMARY precedent `build/phase15-run1-results/`).
- Record `SELECT version()` for the gate (the Phase 19 gate read it from the `NULLABLE_TUPLE_MATRIX server version=` log line; the new dedup test should log the server version the same way so the gate and the evidence file agree). Floating `latest` drifts between runs: if a red appears on unchanged code, check the image date first (memory: Phase 11).
- `test_gate_timeout` is 2400 s in config; Phase 15 full runs took about 2m45s each, Phase 19 gate "dominated by gate runs" at roughly 10 min total. Run the chain with `run_in_background` and await.
- **Docker-stopped `testUnit`:** `colima stop --profile vz` -> `/opt/homebrew/Cellar/docker/29.8.2/bin/docker info` must exit non-zero -> `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` -> `colima start --profile vz` -> `docker info` succeeds. Earlier result: 35 classes, 454 tests, 0 failures, 2 skipped, BUILD SUCCESSFUL in 4 s [VERIFIED: 15-07-SUMMARY]. Never run a bare `colima start` (starts the unneeded `default` profile; memory note). `colima list` this session: profile `vz` Running, 8 CPU, 16 GiB [VERIFIED]. The gate must run `testUnit` with Docker stopped as its own step; stopping the daemon mid-chain means the two full runs come before or after it, never during.
- Skip naming: the `FORK-DELTA` "Named skip inventory" is the naming surface; the gate compares sets, not counts.

## Publication mechanics (PUB-08)

Facts verified this session:
- `origin/custom_build` = `be6b8861c25f3d7611194e7150b18362c5c75e32`; local `custom_build` is the same SHA; merge-base of `HEAD` and `origin/custom_build` is `8497dc1d`; `git rev-list --left-right --count origin/custom_build...HEAD` = `4 119` (left count 4, not 0; matches memory note "left count is 2, not 0" in shape - the number grows with each republication; the check must not assert 0) [VERIFIED: git commands].
- Origin refs: heads `custom_build`, `main`; tags `pre-v1.6.0-sync`, `pre-v1.7.0-sync` (`c61d90ce`), `v1.6.0_gg1` (`c61d90ce`), `v1.7.0_gg1` (`f328fd21`, lightweight) [VERIFIED: `git ls-remote`]. The `v1.7.0_gg1` tag points at the first release merge, not at `be6b8861`; gg2's tag should point at merge commit M, lightweight like gg1 [ASSUMED: tag style by precedent].
- **Branch-only trigger already on the rollback point.** `git show be6b8861:.github/workflows/tests.yaml` contains `on:\n  push:\n    branches: ['**']` and the comment "The trigger is branch pushes only (CI-08). A tag pushed onto an already-pushed SHA joins the same SHA-keyed concurrency group and cancels the branch run, as happened on c61d90ce." [VERIFIED: command output this session]. That satisfies the roadmap condition "rollback tag points at a commit that already carries the branch-only trigger" if the tag is created on `be6b8861`. The tag name is undecided in CONTEXT; recommend `pre-v1.7.0_gg2` [ASSUMED]. Create it before the merge push, push it with an explicit refspec, and confirm via the API that it started no run (branch-only filter). Note `be6b8861`'s workflow is the pre-Phase-17 one (`ubuntu-latest`, `@v4`), so a rollback to it also restores the unpinned workflow and the floating runner label (moves to Ubuntu 26 from 2026-10-19 per STATE [CI]); mention in the rollback item.
- **Re-run the rollback check on the actual merge commit before pushing** (STATE [Publish] 2026-10-11; memory: Phase 16 found `git revert -m 1` broken once docs merges sit on top). gg1 item 8 uses `git read-tree -u --reset pre-v1.7.0-sync` + commit from a clean fast-forwarded `custom_build`, and "go forward again" via `git revert <rollback-commit>` [VERIFIED: ROLLOUT-v1.7.0_gg1.md item 8]. The gg2 rollback item needs the same commands with the new tag name, proven by an E-16-style script on a dry-run merge (never pushed) of the final X into `be6b8861`. Because the gg1 note's rollback points at `pre-v1.7.0-sync` (the pre-gg1 tree), the gg2 note must say clearly that gg2 rollback goes to `pre-v1.7.0_gg2` (gg1 state) and that the gg1 rollback item is untouched.
- **Tag push and CI:** with the branch-only trigger the tag push starts no run; before the trigger change a tag on the same SHA cancelled the branch run (c61d90ce). Still push the tag only after M's single run is green (PUB-08, STATE decisions).
- **One run per SHA:** `concurrency.group` = workflow + `github.sha` with `cancel-in-progress: true` [VERIFIED: tests.yaml]. Never push two branches that point at M; delete the scratch branch only after the tag (CONTEXT recipe); the scratch branch tip X differs from M (M is the merge commit), so the rehearsal run and the release run are different SHAs.
- **Observing the run:** `actions/runs?head_sha=<M>` filtered to `.github/workflows/tests.yaml`, event `push`, `head_branch` `custom_build`, status `completed`, conclusion `success`, no cancelled sibling, re-checked at least 2 minutes later (Phase 16 `CI-STILL-ONE-RUN`). Runs took about 6.5 minutes in Phases 15/16. Rate limit 60/hour unauthenticated (use sparse polling).
- **Merge command shape** (Phase 16): `git worktree add /tmp/<dir>/cb custom_build`; `git -C ... merge --no-ff -F <msg-file> <X-full-sha>`; checks; `git -C ... push origin refs/heads/custom_build:refs/heads/custom_build` (no force, no tag refspec); snapshot origin heads/tags before and after and assert only `custom_build` moved (and, later, the new tags). The merge commit message should name what it publishes (v1.7.0_gg2, the rollout note, the CHANGELOG section) and `git revert -m 1 <this commit>` hint is NOT recommended: use the read-tree rollback (Phase 16 UAT finding) [CITED: STATE decisions 2026-10-11].
- **Gate wording:** approval must be the user's own words given directly to the orchestrator; record verbatim in the summary; a dispatched executor stops at the gate.
- **Deadline context:** today is 2026-10-14; `ubuntu-latest` switches to Ubuntu 26 on 2026-10-19. Phase 17 pinned `tests.yaml` to `ubuntu-24.04` and proved it with CI-17 run `38172811430`, so the date is no longer a hard deadline for the release run [VERIFIED: STATE [CI]].

## Common Pitfalls

### Pitfall 1: Pinning a duplication outcome the server does not produce
**What goes wrong:** the committed test asserts the changed-bytes retry produces duplicate rows and fails (or, worse, is bent until it passes).
**Why it happens:** the gg1 note and D-05 reasoned from "dedup hashes the data"; with an explicit token the server uses the token (Key Finding 1).
**How to avoid:** write the probe first as a throwaway-in-the-test, read the outcome, then pin it. Pin three observations: identical retry dropped; changed-bytes same-token retry dropped; different token lands. Assert the `replicated_deduplication_window` value is non-zero before relying on it. Seen-red check: set the window to 0 or change the token and watch the test fail.
**Warning signs:** a test named "duplicates" in the evidence file.

### Pitfall 2: KeeperMap self-check not actually run
**What goes wrong:** the gg1 note admits its KeeperMap query "was not executed against a live KeeperMap table" because a bare server lacks `keeper_map_path_prefix`. D-03 now forbids that for changed text.
**How to avoid:** use the repo's keeper config. Proven this session on `26.9.17.8`: `CREATE TABLE connect_state (\`key\` String, minOffset BIGINT, maxOffset BIGINT, state String) ENGINE=KeeperMap('/kafka-connect') PRIMARY KEY \`key\``, rows `orders-0 AFTER_PROCESSING`, `orders-1 BEFORE_PROCESSING`, `orders-v2-0 BEFORE_PROCESSING`; the gg1 item 1 query scoped to `IN ('orders')` returned exactly `orders-1 10 19 BEFORE_PROCESSING` and excluded `orders-v2-0` [VERIFIED: live probe]. The PRIMARY KEY clause and the connector's own `CREATE TABLE` text should be read from `KeeperStateProvider.java` before quoting (not opened this session) [ASSUMED: DDL shape; real table is created by the connector].
**Warning signs:** "derived from source" caveats left in the new note.

### Pitfall 3: Detection queries that cannot detect
**What goes wrong:** D-02 asks for a runnable query per broken shape, but stored data alone cannot always distinguish a legitimately NULL value from a wrongly stored one. Pre-fix Enum behaviour per the ledger: a top-level NULL failed the batch; non-NULL values were written without the null marker so the server read the first byte as the null flag ("stored as NULL or shifted"); `{a:'a', b:null}` read back `{'a':NULL,'b':NULL}` (L-13). The Tuple symptom (19-SPIKE C2 id 1, plain twin) is a wrong row plus a phantom row under an id no record carries.
**How to avoid:** the only credible proof is end to end: write fixture rows with the **gg1 code** (a throwaway worktree of `be6b8861`, run the 19-02 RED shape) into twin tables, run each detection query and show it flags them, then run it against rows written by the gg2 code and show no flags. The query will be a suspect finder (for example `Nullable(Enum)` columns that are NULL for a window of rows where the source never nulls, or Tuple rows whose sibling columns are default-valued with an id absent from the source), and the note must say it finds suspects, not proofs. Budget for this being the longest task in the docs plan.
**Warning signs:** queries proven only on gg2-written data.

### Pitfall 4: Docs reviewed after the rehearsal
Late doc edits break "merged tree = rehearsed tip". Budget 2-3 review rounds for operator command text (memory: Phase 16) before the rehearsal.

### Pitfall 5: Links into `.planning/phases/...` that move on milestone close
gg1's note links its ledger at `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`, a path that only exists after archiving (16-D-02 corrected an earlier stale `.planning/phases/13-...` path). The gg2 ledgers are at `.planning/phases/18-writer-correctness/...` and `.planning/phases/19-nullable-enum-tuple-mapping/...` today and will be archived by `/gsd-complete-milestone`; the archive directory name is probably `.planning/milestones/v1.7.0_gg2-hardening-phases/` by analogy with `v1.7.0_gg1-sync-phases/` [ASSUMED]. Decide the link text (Open Question 3); the rollout note says it "restates everything an operator needs", so dead links degrade gracefully.

### Pitfall 6: Review appended with wrong headings
When asking `gsd-code-reviewer` to append to an existing REVIEW.md, require each finding as `### <CR|BL|WR|IN>-NN: <title>` or the disposition parser silently drops findings (memory: Phase 15). Phase 20 has no existing REVIEW file, so create `20-REVIEW.md` fresh but state the heading rule anyway.

### Pitfall 7: Security auditor accepts a wrong procedure
The auditor checks that a threat's mitigation exists, not that the docs procedure is right (memory: Phase 15). Cross-check any rollout-note KeeperMap steps against the code review. Never write a `TRUNCATE` instruction for the shared state table (the merged-tree no-TRUNCATE check from Phase 16 should run again on M).

### Pitfall 8: Stale generated version
A VERSION bump without `clean` leaves `Version.java` at gg1; MBean and client-name tests then still pass against the old constant. The gate must start with `clean` and a check of `ARTIFACT_VERSION`.

### Pitfall 9: Dedup test flakiness from a shared container
`ClickHouseBase` starts one lazily shared container per JVM fork and the per-class database drop is the only isolation. Use a unique database/table per test (existing helpers) and `uniqueConnectorName(TestInfo)`; create tables before `start()` (STATE decisions); read back with typed SQL and `assertEquals`, never `countRows` (STATE decisions).

### Pitfall 10: Rollback tag created on the wrong commit
It must be `be6b8861` (a commit that carries the branch-only trigger and the corrected item 8), not the old `c61d90ce`. Assert `git merge-base --is-ancestor` and the trigger by `git show <tag>:.github/workflows/tests.yaml | grep -n "branches: \['\*\*'\]"`.

## Code Examples

### Probe (what the committed test must reproduce) - SQL over HTTP, verified on 26.9.17.8
```sql
-- Source: live probe this session, ClickHouse 26.9.17.8
CREATE TABLE r1 (id UInt32, v String)
ENGINE = ReplicatedMergeTree('/clickhouse/tables/r1', 'r1') ORDER BY id;
-- insert A, setting insert_deduplication_token = 'T1'   -> 2 rows (id 1, id 0)
-- insert B (1 row, different bytes), token 'T1'          -> dropped
-- insert C (1 row), token 'T2'                           -> lands
SELECT id, v, count() FROM r1 GROUP BY id, v ORDER BY id, v;  -- id 0: 1, id 1: 2
```
The token for the real connector shape is `QueryIdentifier(topic, partition, min, max, queryId).getDeduplicationToken()`, i.e. `topic-partition-min-max` (`QueryIdentifier.java`). For a test that exercises the real writer's byte shapes, reuse `CapturingClickHouseWriter` (captures `body()` and `deduplicationToken()`) to obtain the two bodies - the gg1 shape (an all-defaults row for the empty CDC record) and the gg2 shape (skipped) - and post both through the same client path with the captured token. [VERIFIED: `EmptyCdcExactlyOnceStateTest` read this session]

### KeeperMap self-check, proven live
```sql
SELECT `key`, minOffset, maxOffset, state
FROM connect_state   -- the zkDatabase table
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('orders')
ORDER BY `key`;
-- output on the probe fixture: orders-1  10  19  BEFORE_PROCESSING
```
This is the gg1 item 1 query (shared KeeperMap table, topic-scoped). D-06's gg2 text can quote it by pointing at gg1 item 1 or restate it with this proven output.

### Merge-shape check (adapt from Phase 16)
```sh
# Source: .planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md (E-gate block) and 16-03-SUMMARY.md
test "$(git rev-list --parents -n 1 "$M" | wc -w)" = 3          # two parents
test "$(git rev-parse "$M^1")" = "$(git rev-parse origin/custom_build)"
git diff --quiet "$M" "$X"                                       # tree(M) == tree(X)
! git ls-tree -r --name-only "$M" | grep -x '\.planning/\.gsd-ws-arg'
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| gg1 note: dedup "hashes the parsed data", required/precautionary split by route | With an explicit token the token is the key (measured) | Measured this session | The gg2 delta must correct the reasoning; the "required" route distinction concerns which routes' stored values change, not whether duplicates appear |
| `ubuntu-latest`, tag-pinned actions | `ubuntu-24.04`, SHA-pinned, `permissions: contents: read` | Phase 17 (2026-10-12) | Rollback to `be6b8861` restores the old workflow |
| Rollback by `git revert -m 1` | `git read-tree -u --reset <tag>` + commit | Phase 16 UAT 2026-10-11 | gg2 item 8 must use the read-tree form |
| KeeperMap SQL "not executed" | Runnable with the repo's embedded-keeper config | Found this session | Satisfies D-03 for changed SQL |

**Deprecated/outdated:** the `test-head.yaml` workflow (deleted in Phase 17); the `NanoTimestamp`-style notes in gg1 stay as released.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Rollback tag name `pre-v1.7.0_gg2` | Publication mechanics | Naming only; user can choose another |
| A2 | `v1.7.0_gg2` tag is lightweight and points at merge commit M (as gg1's points at its release merge) | Publication mechanics | A different tag style changes `git show`/rollback wording |
| A3 | Archive directory for this milestone will be `.planning/milestones/v1.7.0_gg2-hardening-phases/` | Pitfall 5 | Dead ledger links if wrong |
| A4 | The connector's KeeperMap table DDL (PRIMARY KEY clause) matches the fixture I used; `KeeperStateProvider.java` was not opened | Pitfall 2 | Query text unaffected (columns read from the gg1 note), but quoted DDL must be read from source before use |
| A5 | Phase 17-19 changed no shipped dependency versions, so the CHANGELOG Dependencies subsection is empty | CHANGELOG | Missing a bump entry; verify with `git diff origin/custom_build HEAD -- gradle/libs.versions.toml` |
| A6 | Statement that a re-delivered identical range under `exactlyOnce=false` is dropped on a replicated table; derived from the code path (token always sent) plus the measured server behaviour, not from a connector-level live test | Key Finding 1 | D-06 wording ("not affected") may be right or wrong depending on the reader; the live test should cover a non-EO-shaped insert too |

## Open Questions

1. **D-05's premise is contradicted by measurement. Which wording does the user want?**
   - Known: same token + changed bytes -> dropped, on `MergeTree` with window and on `ReplicatedMergeTree`, `26.9.17.8`. The gg1 note item 1 says a differently encoded retry "can write rows twice".
   - Unclear: whether the user wants the gg2 note to explicitly retract that statement (the gg1 text itself must stay as released, D-01/CONTEXT) and whether the "required vs precautionary" routing in gg1 item 1 needs a gg2 correction.
   - Recommendation: surface to the user before the note is written (it changes the operator advice from "risk of duplicates" to "risk of old rows persisting"). Pin "dropped" in the test; have the note say the exact consequence, the stop-cleanly procedure still being the right procedure.
2. **Does D-06's "`exactlyOnce=false` is not affected" survive?** The token is sent in that mode. Recommendation: reword to "keeps no offset state, so the stop-and-check procedure does not apply; a re-delivered identical range is still deduplicated by token on a replicated table".
3. **Ledger link targets in the gg2 note** (Pitfall 5). Recommendation: link to the archive path predicted in A3, or name the ledger files without relative links.
4. **Rollback tag name and whether it is pushed.** gg1's `pre-v1.7.0-sync` is on origin. Recommendation: create `pre-v1.7.0_gg2` on `be6b8861` and push it before the release push, verifying no run starts.
5. **Detection-query proof cost (Pitfall 3).** Needs a gg1-code worktree to write fixtures. Recommendation: accept; alternatively ask the user whether suspect-finder queries proven on hand-built wrong rows (mimicking the spike symptom) are acceptable.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | Live tests, probes, gate | yes | colima `vz` Running, 8 CPU / 16 GiB; Docker client 29.8.2 (`/opt/homebrew/Cellar/docker/29.8.2/bin/docker`, not on PATH) | `colima start --profile vz` |
| ClickHouse server image | Live tests, probes | yes (local, 40 h old at probe time) | `clickhouse/clickhouse-server:latest` -> `26.9.17.8` | A foreground `docker pull` took over 4 minutes and was killed; Testcontainers' always-pull policy for floating tags (FORK-DELTA row 75) will pull anyway - expect slow first test start |
| JDK | Gradle toolchain | JDK 21 on PATH, Gradle toolchain resolves 17 per build config | `openjdk 21.0.12.1` on PATH | toolchain auto-provisioned per `build.gradle.kts` (existing gates ran this way) |
| `TESTCONTAINERS_HOST_OVERRIDE` | Gradle | set to the literal `null` in this shell | - | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (mandatory) |
| `gh` CLI | CI observation | no (deliberate) | - | unauthenticated REST API; optional Keychain read-only token for logs |
| `git`, `curl`, `python3 -I`, `colima` | scripts | yes | - | - |
| Network to `origin` / GitHub API | Rehearsal, publication | not exercised this session beyond `git ls-remote` (worked) | - | - |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** `gh` (REST API).

## Security Domain

`security_enforcement` is enabled in `.planning/config.json` (`security_asvs_level: 1`, `security_block_on: high`) [VERIFIED: config read]. Phase 20 changes docs, a version string and one test; there is no new runtime attack surface.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (no auth code changed) | - |
| V3 Session Management | no | - |
| V4 Access Control | no | - |
| V5 Input Validation | limited - new SQL/shell text in operator docs | Run every quoted command; placeholders only, no host/credential values |
| V6 Cryptography | no | - |
| V14 Configuration / CI supply chain | yes | Actions stay SHA-pinned; publication pushes use explicit refspecs, no force |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Operator doc instructs a destructive action on shared state (`TRUNCATE` of the KeeperMap table, T-15-14) | Tampering | No `TRUNCATE` instruction in the merged tree (rerun Phase 16's check on M); scope deletes by key |
| Secrets in docs/evidence (hostnames, tokens, record values) | Information disclosure | Placeholders only; secret scan of changed files as in Phase 16 (`SECRET-SCAN-CLEAN`); the Keychain PAT never printed or written |
| Relayed or fabricated approval | Spoofing/Repudiation | Approval only from the user's direct answer; verbatim record; executors refuse relayed approvals |
| Force push / tag clobber / unintended branch publication | Tampering | Explicit refspecs, no `--force`, before/after snapshots of origin heads and tags |
| Wrong-tree publication | Tampering | `git diff --quiet M X`, first-parent check, allow-list of non-`.planning` diffs |
| Logging record data in new probe/test | Information disclosure | Test logs counts and server version only (project rule: no data in logs) |

D-09 requires `/gsd-secure-phase 19` before the merge gate; `19-SECURITY.md` does not exist yet [CITED: STATE Operator Next Steps], and `18-SECURITY.md` / `16-SECURITY.md` exist as precedents [VERIFIED: directory listings].

## Project Constraints (from CLAUDE.md)

From `./CLAUDE.md` -> `AGENTS.md` and `./.claude/CLAUDE.md` [VERIFIED: system-reminder contents]:
- Pre-PR obligations: tests and `integrationTest` pass; `./gradlew spotlessApply` run (or `spotlessCheck` passes); new functionality has feature-test coverage; **`VERSION` bumped and `CHANGELOG.md` updated** (PUB-07 owns this).
- ClickHouse-backed feature tests extend `ClickHouseBase`; always read data back and `assertEquals`; DRY; do not delete or weaken existing tests; descriptive test names; read `ClickHouseTestHelpers.java` before writing tests.
- Java 17, Google Java Style via Spotless, Lombok, SLF4J levels (TRACE per-record, DEBUG batch, INFO lifecycle), try-with-resources, constants extracted as static strings, comments only for non-obvious invariants.
- Do not log secrets or record data; do not upgrade dependencies without asking.
- High-risk files (`Processing.java`, `KeeperStateProvider.java`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `RangeContainer.java`): Phase 20 must not modify them; the dedup test only reads their behaviour.
- Do NOT make commits to the branch without explicitly asking the user (AGENTS.md Review rules); GSD phase commits are the established exception through the workflow, but merge/push/tag commit-to-origin actions need the user's own approval. Do not change a PR title/description without asking.
- GSD workflow enforcement: file edits start through a GSD command; no direct edits outside GSD.
- Test target: `latest` x client V2 only; rebasability: keep changes additive.
- Attribution lines for commits/PRs per the session system reminder (`Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`).
- Spotless caveat: `ClickHouseWriter.java` stays dirty under the Spotless IDE hook by design (STATE Phase 18 decision); new test files should be Spotless-clean.

## Sources

### Primary (HIGH confidence)
- Live probes this session on `clickhouse/clickhouse-server:latest` = `26.9.17.8` (token dedup matrix, merge-tree dedup defaults, ReplicatedMergeTree on embedded keeper, KeeperMap query)
- Repository files read this session: `20-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md`, `ROADMAP.md` (Phase 16-20 sections), `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md`, `CHANGELOG.md` (top), both behaviour ledgers, `19-SPIKE.md`, `19-REVIEW.md`, `19-REVIEW-DISPOSITION.md`, `19-10-SUMMARY.md`/`19-10-PLAN.md` gate command, `16-CONTEXT.md`, `16-03-SUMMARY.md`, `16-COMMAND-EVIDENCE.md`, `15-07-SUMMARY.md`, `.github/workflows/tests.yaml`, `git show be6b8861:.github/workflows/tests.yaml`, `QueryIdentifier.java`, `Processing.java`, `ClickHouseWriter.java` (token sites), `EmptyCdcExactlyOnceStateTest.java`, `build.gradle.kts`, `.planning/scripts/fork-anchors.txt`, `src/test/resources/clickhouse_keeper_map.xml`, `.planning/config.json`, memory notes (Phases 11, 15, 16, 17, colima)
- Git state commands (`ls-remote`, `rev-list`, `diff --stat`, tag list)

### Secondary (MEDIUM confidence)
- https://clickhouse.com/docs/reference/settings/session-settings/insert - token semantics (page's replicated-window default of 100 conflicts with the measured 10000; measured wins)
- https://kb.altinity.com/altinity-kb-schema-design/insert_deduplication/ - token replaces checksum; same token different data is dropped; non-replicated default off

### Tertiary (LOW confidence)
- None relied upon. A web search surfaced third-party blog posts on idempotent ingestion; not used.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new packages; everything is reuse of proven scripts.
- Architecture/ordering: HIGH - derived from Phase 16 and 19 artifacts and verified git state.
- Dedup consequence: HIGH for server behaviour (measured on `26.9.17.8`, three engines, doc-corroborated); MEDIUM for the non-EO wording (A6).
- Pitfalls: HIGH for 1, 2, 4, 8, 10 (observed); MEDIUM for 3 (design risk) and 5 (path prediction).

**Research date:** 2026-10-14
**Valid until:** 2026-10-19 for the runner-label context; the `latest` image drifts, so re-record `SELECT version()` at each gate. The dedup behaviour should be re-measured by the committed test at gate time.
