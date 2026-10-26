---
phase: 29-release-publication
plan: "07"
subsystem: release
tags: [release, suite-gate, rehearsal, ci, ops-05]

requires:
  - phase: 29-release-publication
    provides: "plans 29-01..29-06: the bump, evidence, documents, review and dispositions"
provides:
  - "E-60 suite gate on gated_head 153cb710 (two full runs and Docker-stopped testUnit, all equal to the phase base)"
  - "E-61 publication checks (anchors gate, OPS-01/04, pre-push, dry-run merge and rollback, spotless baseline-equal)"
  - "E-62: sync/v1.7.0 on origin at X0 ef19fbc8 with exactly one green tests.yaml run (40017036598)"
  - "the release bundle for plan 29-08"

requirements-completed: [OPS-05]

key-files:
  modified:
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md
---

# Phase 29 Plan 07: Suite gate and CI rehearsal

Tasks 1 and 2 ran in a dispatched gsd-executor (commits `1c258925`, `ef19fbc8`) and returned the Task 3 blocking-human checkpoint. Task 4 ran inline in the orchestrator after the user's own answer.

## Rehearsal approval

User's verbatim answer, given directly to the orchestrator in the session: **"push"**. Before the push the orchestrator re-ran FREEZE-OK, OPS-01-04-OK, PUSH-READY, MERGE-DRY-RUN-OK and ROLLBACK-ON-TIP-OK on X0; origin `custom_build` was still `69cd680b`.

Push command, as run (no force flag, no tag refspec):

```sh
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0
```

X0 = `ef19fbc8f3c1ddee16c84180b84fa47b2ddce12c`. Run `40017036598`, `tests.yaml`, push on `sync/v1.7.0`, success, 2026-10-23T20:09:34Z to 20:16:02Z, one job on `ubuntu-24.04`; the only `tests.yaml` run on X0, none cancelled. REHEARSAL-PUSHED-ONLY and REHEARSAL-RUN-GREEN print.

## Gate totals against the base

`testUnit` (Docker stopped) 1278 tests, 0 failed, 2 skipped; two full runs 1865 tests, 0 failed, 18 skipped; equal to E-05; server 26.9.19.9, image `sha256:bdb5c911…` dated 2026-10-14; suite environment `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 CLICKHOUSE_CLUSTER_NAME=unset` (PT-15); `integrationTest` excluded (PT-16, reason in the bundle and E-60).

## Deviations

None in the plan's tasks. Plan 29-06's documents checks that were re-run as part of the gate used the known variants without the stale `git diff --quiet c1dc6e59 ...` clause (see 29-06-SUMMARY).

## Approval bundle


## 1. The tree

- X0 = `ef19fbc8f3c1ddee16c84180b84fa47b2ddce12c`, on origin as `sync/v1.7.0`. It is 272 commits ahead of `origin/custom_build` (`69cd680bf548bfb0329142752d37f8a6c3190c26`, unchanged) and 0 behind; `69cd680b` is an ancestor.
- Outside `.planning/`: 180 files, +7813 / -513 against `69cd680b`. 59 files outside `src/test/resources`: CI (`.github/workflows/tests.yaml`, `.github/scripts/jacoco_summary.py`), `.gitignore`, `build.gradle.kts`, `gradle/libs.versions.toml`, `VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, the four rollout notes, 12 files under `src/main` and 36 test files; the rest is 121 test data files (DESCRIBE captures). The full list is in the rehearsal bundle.

## 2. The gate and the rehearsal

- Suite environment `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 CLICKHOUSE_CLUSTER_NAME=unset` on every command (`suite_env:`), `suite_jvm_clickhouse_version: latest`; server 26.9.19.9, image dated 2026-10-14 (unchanged from the phase base).
- `testUnit` with Docker stopped: 1278 tests, 0 failed, 2 skipped. Two full runs: 1865 tests, 103 XML files, 0 failed, 18 skipped each; all equal to the phase base (E-05).
- `integrationTest` was not run (PT-16): compile-only since SYNC-10, needs a Confluent stack and ClickHouse Cloud, `src/integrationTest` byte-identical to upstream `v1.7.0`, nightly workflow red by inheritance and disabled. `compileIntegrationTestJava` is green in the gate and in the CI run.
- Anchors gate: `result=PASS`, 94 rows; self-test: no failures. Spotless: baseline-equal (the same 47-file rewrite as at the phase base), not a clean `spotlessCheck`.
- Rehearsal run on X0: run `40017036598` (https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/40017036598), `tests.yaml`, event push on `sync/v1.7.0`, success, about 6.5 minutes, runner label `ubuntu-24.04`; it was the only `tests.yaml` run on X0 and nothing was cancelled.

## 3. Reviews and carried items

- Review dispositions for Phases 25 to 29 all read `open: 0` (7, 15, 4, 9 and 20 findings); `threats_open: 0` for Phases 25 to 28.
- Phase 29's review: four fresh-reviewer rounds, 20 findings, all fixed; round 3 continued on your answer; IN-09 was fixed without a further round (recorded deviation).
- 16 findings are deferred to `29-MILESTONE-CLOSE-RECORD.md`: 25-REVIEW WR-01, WR-02, WR-03, IN-01..IN-04; 26-REVIEW IN-02; 27-REVIEW WR-01, IN-01..IN-03; 28-REVIEW IN-01, IN-02, IN-04, IN-05. The close record also lists the user reminders (item 8).

## 4. The documents this release ships

- `docs/ROLLOUT-v1.7.0_gg4.md`, seven items: 1 Names and Enum labels as written in CREATE TABLE; 2 Tuple elements named size; 3 Writer error text; 4 In-flight batches; 5 JMX MBean name and client name; 6 Dependencies, build and the Connect worker; 7 Rollback.
- Corrections to the gg1, gg2 and gg3 notes (the `enableDbTopicSplit` key rule; the in-flight query scope for connectors with more than two topics).
- `CHANGELOG.md`: `# 1.7.0_gg4, 2026-10-23`, rename notice first. The heading date equals today (UTC); a release merge after 2026-10-23T23:59:59Z would make it wrong and require a document edit and a new rehearsal.
- `docs/FORK-DELTA.md` raw numbers after gg4: `src build.gradle.kts gradle .github` 277 files, 29679 insertions, 643 deletions; `src/main` 37 files, 2877 insertions, 241 deletions.

## 5. The merge plan (each step after your approval of this bundle)

1. Re-check origin: `custom_build` still `69cd680b` (OPS-05).
2. In a scratch worktree at `69cd680b`, a `--no-ff` merge of X0 by hash: merge commit M.
3. Checks on M: two parents, tree equal to X0's, no TRUNCATE advice, the rollback script re-run on M.
4. `git push origin refs/heads/custom_build:refs/heads/custom_build`, then watch the single CI run on M and re-check origin once more afterwards.
5. `git push origin refs/tags/v1.7.0_gg4:refs/tags/v1.7.0_gg4` and `git push origin refs/tags/pre-v1.7.0_gg4:refs/tags/pre-v1.7.0_gg4` (the rollback tag on `69cd680b`).
6. `git push origin --delete refs/heads/sync/v1.7.0`.

## 6. What publishing makes permanent, and the way back

- A push to `custom_build` and the two tags are public the moment they land; clones and forks keep them even if you later move the branch. Nothing here uses a force push.
- Rollback path (item 7 of the gg4 note): restore the tree of `pre-v1.7.0_gg4` with a revert or read-tree commit (never a force push of `custom_build`), and redeploy the archive built from tag `v1.7.0_gg3`. Do not build a rollback archive from `pre-v1.7.0_gg4`: it holds 25 nested jars (E-04).

## 7. What the release contains and does not

- Contains: the Phase 25 to 28 changes (dependency absorption and mini upstream sync, size-named Tuple elements in discovery, the writer failure-path test debt, raw identifier names in discovery) plus the Phase 29 documents and the version bump.
- Does not contain: any `src/main` change made in Phase 29. Dependabot PRs #4 (client 0.10.0) and #10 (protobuf-java 4.36.2) are not merged and not ignored.

## 8. Reminders after the tag (no agent changes repository settings)

- Keep PRs #4 and #10 open; do not merge, close or ignore them.
- Check the next Dependabot pull request for the `dependencies` label and absence of the "labels could not be found" comment (OPS-04).
- The Dependabot freeze ends with the tag; merging Dependabot pull requests is your call afterwards.
- `/gsd-audit-milestone` before `/gsd-complete-milestone`; the milestone housekeeping is in the close record.

## Decision

Reply `publish` to run steps 1 to 6 above, or `hold` plus the item you want addressed. Answer directly; a relayed approval is not acted on.

## Self-Check: PASSED
