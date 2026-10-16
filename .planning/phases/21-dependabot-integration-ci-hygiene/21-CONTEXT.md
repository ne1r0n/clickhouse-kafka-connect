# Phase 21: Dependabot Integration & CI Hygiene - Context

**Gathered:** 2026-10-17
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase does four things:
- It merges `origin/custom_build`'s three Dependabot commits (lombok 1.18.48 #1, spotless 8.10.4 #2, slf4j-reload4j 2.0.20 #3; tip `6473e24f`) into `sync/v1.7.0` before any code work, and keeps the anchors invariant meaningful past them (DEPS-01).
- It closes the 17-REVIEW pin-guard gaps in `TestsWorkflowPinGuardUnitTest` (CI-18, CI-19), plus the guard half of 17-REVIEW WR-01 (D-09).
- The user closes the three GitHub-side loose ends: OPS-01 nightly disabled, OPS-02 the Dependabot `gradle` entry checked, OPS-03 the token revoked.
- No connector behaviour changes, and nothing is pushed.

Requirements: DEPS-01, CI-18, CI-19, OPS-01, OPS-02, OPS-03.

Locked by ROADMAP/STATE and not re-opened here:
- The DEPS-01 merge is a merge: no rebase, no force, and no separate `.planning/` commit interleaved with it.
- If spotless 8.10.4 demands reformatting, it is its own commit. `spotlessCheck` and `testUnit` must be green, and the Lombok "Ambiguous: Jackson2 and Jackson3" warning must stay absent, before any Phase 22–23 code plan starts.
- CI-18/CI-19 read `tests.yaml` and `build.gradle.kts` and never edit them. The `timeout-minutes: 45` anchor survives.
- This phase pushes nothing. The merged tree's first CI run is Phase 24's rehearsal.
- Dependabot PR #4 (client `0.10.0`) stays open, unmerged and not ignored.
- OPS-01..03 are user actions in the GitHub web UI (D-11 milestone rule). No agent changes repository settings, and a relayed answer is not accepted.
- `integrationTests.yaml` is not edited, deleted or fixed.
- The 17-REVIEW closures made here are written into the archived disposition file by Phase 23 (DEBT-12), not here.

**Correction to the ROADMAP note (found during this discussion).** The note says plan time decides "which `PRE_TAG` later gates use" to keep the invariant meaningful. A `PRE_TAG` change alone cannot do it:
- The gates run `verify-merge-invariants.sh 54e63648 HEAD`.
- Invariant 0 requires `PRE_TAG`, or for a merge tag each of its parents, to be an ancestor of `54e63648`'s first parent `b1d26fd2`.
- Excluding `6473e24f` from `PRE_TAG..HEAD` would need `6473e24f` to be an ancestor of `PRE_TAG`. No commit satisfies both.

So D-01..D-04 replace the `PRE_TAG` idea. `PRE_TAG` stays at its default `pre-v1.7.0-sync` (`c61d90ce`), and the gate command is unchanged.

</domain>

<decisions>
## Implementation Decisions

### Anchors invariant past `6473e24f` (DEPS-01)
- **D-01:** Later gates get past `6473e24f` through a **SHA-pinned exemption** in `verify-merge-invariants.sh`'s invariant 3. Every other check is unchanged:
  - The full range `pre-v1.7.0-sync..HEAD` is still checked.
  - The gate invocation stays `verify-merge-invariants.sh 54e63648 HEAD` in Phases 22–24.
  - Any violation other than the exempted one still fails, including a different row at `6473e24f`.
  - The script gains a small amount of logic.
- **D-02:** The exemption is a **reusable list file** beside `fork-anchors.txt`, with one row today for `6473e24f`.
  - Each row identifies **the exact anchor row** (path and fixed-string) at one full 40-hex commit SHA, and carries a reason.
  - It must not key on the backs-id alone: `BLD-08` backs **two** rows (`build.gradle.kts slf4j-reload4j:2.0.17` and `gradle/libs.versions.toml kafka = "4.1.1"`), and only the first is exempt at `6473e24f`.
  - If Phase 24 has to absorb another published bump of an anchored line, it adds a row and does not edit the script again.
  - A malformed exemption row fails the gate.
- **D-03:** The BLD-08 anchor row and the two FORK-DELTA rows that name `slf4j-reload4j:2.0.17` (BLD-08, `docs/FORK-DELTA.md:63`) and "Lombok 1.18.46" (BLD-14, `docs/FORK-DELTA.md:65`) move **inside the merge commit**.
  - Procedure: `git merge --no-commit origin/custom_build`, edit those rows, then commit the merge.
  - The merge commit then satisfies its own anchors file, so `6473e24f` is the only exemption.
  - The merge message states that it carries these bookkeeping edits, which are in neither parent.
  - Raw numbers in FORK-DELTA still wait for Phase 24.
  - **Reversibility:** costly. Before Phase 24's push, a local reset redoes it. Once Phase 24 publishes it, the merge commit's shape is permanent on `custom_build`.
- **D-04:** The exemption's narrowness is proven by **new fail-first cases in `.planning/scripts/verify-merge-invariants-selftest.sh`**, in the ST1–ST4 style, using dangling objects only. They stay and run with each gate. They must show that:
  - the range fails without the exemption;
  - an exemption for a different row does not hide the `slf4j-reload4j` violation;
  - the `slf4j-reload4j` exemption does not hide a violation of the other BLD-08 row (the `kafka` line) at the same commit;
  - a malformed exemption row fails.

  Also confirm that the self-test's default merge lookup, `git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD | tail -1`, still resolves to `54e63648` once the DEPS-01 merge adds a second first-parent merge, and fix its "single merge" comment.

### Pin-guard red proof (CI-18, CI-19)
- **D-05:** "Seen red on a fixture" is met by **committed fixture tests** in `testUnit`.
  - Each rule runs on small in-test fixtures, both a good input it must accept and a bad input it must reject.
  - They are permanent because the IN-02 fixes have no other standing guard: the real `tests.yaml` contains neither a trailing-comment `runs-on` nor a `: write` step text.
- **D-06:** **Extract the rules and keep the tests.**
  - Each rule becomes a static method in `TestsWorkflowPinGuardUnitTest` that takes a list of lines and returns the offending line or none.
  - The five existing `@Test` methods keep their names, `@DisplayName`s and assertion messages, and call these methods on the real files.
  - The fixture tests call the same methods. No regex is duplicated, and no existing pin is weakened.
- **D-07:** **Red run in evidence, one green commit.**
  1. Write the fixture tests first and run them against the **old** regexes.
  2. Record the failing test names and messages in the phase evidence file.
  3. Fix the regexes and commit the tests and the fix together.

  Every commit keeps `testUnit` green.

### Pin-guard scope (CI-18, CI-19, 17-REVIEW WR-01)
- **D-08:** CI-18 has two checks.
  - **`tests.yaml`:** find the step named `Report test fork count` and require a `./gradlew … reportTestForks` call in its `run:` block. A mention elsewhere, such as the `::error::reportTestForks …` text, does not count. Reject `nproc` on any non-comment line except the one message line that prints it as context, so a shell copy of the `nproc / 2` formula fails (17-REVIEW IN-01's suggested fix).
  - **`build.gradle.kts`:** a text check that `tasks.register("reportTestForks")` is present and that the task reads `maxParallelForks` from the `test` task. A deleted or renamed task, or one that prints a constant, fails. There is no Gradle run, no TestKit and no new dependency.
- **D-09:** Add the **guard half of 17-REVIEW WR-01** in the same class, with fixtures:
  - each of the four upstream workflow files (`increment-version.yml`, `integrationTests.yaml`, `pre-release.yaml`, `release.yaml`) appears under `exclude-paths` in `.github/dependabot.yml`;
  - every path listed there exists in `.github/workflows/`.

  Together with OPS-02, WR-01 then ends as `fixed`. Phase 23 (DEBT-12) records that disposition.
- **D-10 (constraint):** CI-19's narrowed write-scope rule stops flagging step `name:`/`run:` text such as `name: write summary`. It must **still reject**:
  - `contents: write`;
  - `permissions: write-all`;
  - a job-level write block.

  Each of these is a must-reject fixture, so the IN-02 fix cannot weaken the existing read-only pin. The `runs-on` fix strips a trailing ` # …` comment before the equality check. `runs-on: ubuntu-latest # x` must still fail.

### OPS timing & evidence (OPS-01..OPS-03)
- **D-11:** OPS-01's observation passes on a REST API check **at or after 2026-10-17 12:00 UTC** that finds no `schedule` run of `integrationTests.yaml` created after the disable time.
  - Scheduled runs on the fork start about 4.5 h after the `00 05 * * *` cron: 2026-10-15 09:39 UTC and 2026-10-16 09:32 UTC.
  - 12:00 UTC leaves margin past that delay.
- **D-12:** **Phase 22 waits.** Phase 21 is verified and closed only after D-11's window has passed, and phases stay strictly sequential.
- **D-13:** The OPS confirmations are given **in free text, in the user's own words, at the start of Phase 21 execution**.
  - The orchestrator runs this inline: it shows the three checks with exact web-UI steps and records each answer verbatim beside the API facts. A screenshot is acceptable, as in gg2.
  - Clicking an option does not count as "in their own words".
  - The OPS-01 disable date comes from the user's answer. The API timestamp corroborates it.

### Claude's Discretion
- The exemption file's name, column order, and whether it is read from `RANGE_END`'s tree (like `CONFLICTED_PATHS_FILE`) or the working tree. Recommendation: read it from `RANGE_END`'s tree, use TAB-separated columns like `fork-anchors.txt`, and require the full 40-hex SHA.
- Whether to rehearse the merge and the gate in a throwaway worktree before committing on `sync/v1.7.0`. Recommendation: yes. Expected result: no conflicts, and only the BLD-08 `slf4j-reload4j` row at `6473e24f` violating.
- The fixture format. Recommendation: inline Java text blocks in the test class, not resource files.
- The exact regexes and the step-block parsing for D-08 and D-10. They stay line-level, with no YAML library, per Phase 17 D-06.
- The plan split and commit order, within the fixed order: the DEPS-01 merge first, then any spotless reformat commit, the gate, and the CI-18/CI-19 work.
- The evidence file layout. Phase 17/20 precedent: a phase evidence file containing the merge dry-run, the gate and self-test output, the red fixture run, the `dependencyInsight` output, the Lombok-warning check and the OPS records.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` §"Phase 21: Dependabot Integration & CI Hygiene": goal, success criteria 1–5, and the DEPS-01 notes. The `PRE_TAG` note is superseded by D-01..D-04 (see `<domain>`).
- `.planning/ROADMAP.md` §"Phase 24: Release & Publication": it runs the anchors invariant "from the `PRE_TAG` Phase 21 chose", which per this context stays the default `pre-v1.7.0-sync`, plus the exemption.
- `.planning/REQUIREMENTS.md`: DEPS-01, CI-18, CI-19, OPS-01..OPS-03, plus the scope decisions of 2026-10-16 (PR #4 open, nightly disabled in settings, file untouched).
- `.planning/STATE.md` §Blockers/Concerns: [Publish] (divergence, the BLD-08 anchor), [CI] (nightly, WR-01 residue), [Env] (the `TESTCONTAINERS_HOST_OVERRIDE` workaround, the Keychain token), [Tooling] (no `gh`; unauthenticated REST API).

### Findings being closed
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW.md`: WR-01 (the `exclude-paths` guard and the manual check), IN-01 (`reportTestForks` not pinned), IN-02 (the `runs-on` comment and `WRITE_SCOPE` brittleness).
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW-DISPOSITION.md`: all four rows still `open`. Phase 23 flips them; Phase 21 only produces the closures.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md` §"Manual checks" 3 and §"Housekeeping": the carried-forward OPS-02 and OPS-03 wording and steps.

### Prior decisions carried
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-CONTEXT.md`: D-06 (the pin guard: line-level, no YAML library, Docker-free in `testUnit`), D-09 (the one-off flip habit, superseded here by D-05 for fixtures), the carried rules (anchors change in the same commit, `spotlessApply` on touched files only, `env -u TESTCONTAINERS_HOST_OVERRIDE`).
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-CONTEXT.md`: D-11 (manual checks presented inline, answers recorded verbatim, no agent setting changes).

### Files read or edited
- `.planning/scripts/verify-merge-invariants.sh`: invariant 0 (shape and `PRE_TAG` ancestry, L38-84) and invariant 3 (anchors, L187-253), where the D-01/D-02 exemption goes.
- `.planning/scripts/verify-merge-invariants-selftest.sh`: ST1–ST4 and the default merge lookup (L26-29). D-04's new cases go here.
- `.planning/scripts/fork-anchors.txt`: L43 `build.gradle.kts slf4j-reload4j:2.0.17 1 BLD-08`, which moves to `2.0.20` in the merge commit (D-03). L48 `gradle/libs.versions.toml kafka = "4.1.1" 1 BLD-08` is unchanged and must not be exempted. The `tests.yaml timeout-minutes: 45` row survives.
- `docs/FORK-DELTA.md`: L63 (the BLD-08 slf4j-reload4j row) and L65 (the BLD-14 Lombok row), edited in the merge commit (D-03).
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java`: the only test file this phase edits (D-05..D-10). It is Phase-21-only per the ROADMAP shared-files table.
- `.github/workflows/tests.yaml` (L51-64, the "Report test fork count" step), `build.gradle.kts` (L254-262, `reportTestForks`), `.github/dependabot.yml` (the `exclude-paths` list): read, never edited.
- `gradle/libs.versions.toml`: `lombok`, `spotless` and `slf4j` (2.0.18, used by `slf4j-simple`) entries. Read only.

</canonical_refs>

<code_context>
## Existing Code Insights

### Current state (scouted 2026-10-16/17)
- `origin/custom_build` = `6473e24f`. `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` = `8 19`. The left side is the three Dependabot commits plus the five custom_build publication merges `c61d90ce`, `f328fd21`, `537beb1f`, `be6b8861` and `a246ca01`. After DEPS-01 it is 0.
- The Dependabot diff `3879b883..6473e24f` touches only `build.gradle.kts` (one line) and `gradle/libs.versions.toml` (two lines), so no conflict is expected with the 19 `.planning/`-only local commits.
- `pre-v1.7.0-sync` = `c61d90ce` and `pre-v1.7.0_gg2` = `be6b8861`. After the merge, the anchors range `c61d90ce..HEAD` newly includes `f328fd21`, `537beb1f`, `be6b8861`, `a246ca01`, `557e48c0`, `ce8ab0e0` and `6473e24f`. Only `6473e24f` is expected to violate.
- Spotless is configured with `importOrder()` and `removeUnusedImports()` only (`build.gradle.kts` L495-501). `googleJavaFormat()` is commented out, so 8.10.1 → 8.10.4 is unlikely to cause mass reformatting.
- `slf4j-reload4j:2.0.20` brings `slf4j-api:2.0.20`, above the catalog's `slf4j = "2.0.18"`, which only `slf4j-simple` uses.
  - Research records `dependencyInsight` for `slf4j-api` on `runtimeClasspath`, `testRuntimeClasspath` and `integrationTestRuntimeClasspath` before and after the merge.
  - Any change goes into the evidence file for Phase 24's rollout note.
  - No catalog edit: that would be an unapproved bump.
- OPS facts from the public REST API and a local lookup:
  - `integrationTests.yaml` has been `disabled_manually` since `2026-10-16T09:38:23Z`.
  - Its last scheduled runs were `2026-10-15T09:39:18Z` and `2026-10-16T09:32:28Z`, both failures.
  - Dependabot gradle-ecosystem PRs #1–#5 were opened on 2026-10-14 between 21:55 and 21:59 UTC. #1–#3 were merged, #4 is open, and #5 (junit-bom 6.1.3) was closed. This is corroborating evidence that the `gradle` entry runs.
  - The Keychain item `gsd17-gh-actions-read` is already absent.

### Reusable Assets
- `TestsWorkflowPinGuardUnitTest` already has a `nonCommentLines(Path)` helper (UTF-8 explicit, because the test JVM uses `windows-1252`), a `LIST_ITEM` step-boundary pattern used by the checkout check, and constants per rule. D-06 refactors around these.
- `verify-merge-invariants-selftest.sh` has `synth_tree`, `expect_failure` and `report` helpers that build dangling trees and commits. D-04's cases reuse them.

### Established Patterns
- Anchor rows are `path TAB fixed-string TAB count TAB backs-id`, and each commit is checked against its own copy. An anchor changes only in the commit that rewrites the code (Phase 17 carried rule).
- Phase 20 gates ran `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`, which takes about 2 minutes and walks every commit, so run it in the background.
- Docker-free tests must not be tagged `clickhouse` and must not extend `ClickHouseBase`, so that `testUnit` picks them up.

### Integration Points
- The exemption list is consumed by every later anchors gate in Phases 22–24 and by Phase 24's pre-gate bundle.
- The CI-18/CI-19/WR-01 closures feed Phase 23's DEBT-12 disposition rows. The OPS records feed Phase 23 (WR-01 residue) and Phase 24 (the OPS-01 re-check).

</code_context>

<specifics>
## Specific Ideas

- The user consistently chose the narrowest durable mechanism with a standing guard over one-off evidence: a SHA- and row-pinned exemption with self-test cases, and committed fixtures for the guard's own rules.
- The exemption must not over-exempt. Keying on the exact row, not the backs-id, follows directly from `BLD-08` backing two rows.
- Phase 24 should not need another script edit if the user merges more Dependabot PRs. It adds an exemption row only if a bumped line is anchored. See memory `dependabot-merges-on-custom-build`.

</specifics>

<deferred>
## Deferred Ideas

- Aligning the catalog's `slf4j = "2.0.18"` with the `slf4j-api` version that `slf4j-reload4j:2.0.20` pulls in would be a dependency bump, which needs the user's approval. It is not done here. Research records the resolved versions only.
- Dependabot PR #4 (client `0.10.0`) is already a Future Requirement and is not discussed further.

</deferred>

---

*Phase: 21-dependabot-integration-ci-hygiene*
*Context gathered: 2026-10-17*
