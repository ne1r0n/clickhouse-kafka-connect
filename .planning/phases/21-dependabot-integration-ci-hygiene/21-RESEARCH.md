# Phase 21: Dependabot Integration & CI Hygiene - Research

**Researched:** 2026-10-17 (00:30-01:30 +03:00, which is 2026-10-16 21:30-22:30 UTC)
**Domain:** git merge integration, a bash invariant gate, a Docker-free JUnit "pin guard" over YAML/Kotlin text, and public GitHub REST API checks
**Confidence:** HIGH for everything run in a rehearsal this session. MEDIUM for the GitHub web-UI labels (not observable without a login).

## Summary

The merge is mechanical: `git merge --no-commit --no-ff origin/custom_build` onto `sync/v1.7.0` has no conflicts and changes exactly `build.gradle.kts` (1 line) and `gradle/libs.versions.toml` (2 lines). With the D-03 bookkeeping edits added inside the merge commit, the existing gate fails on exactly one line, `6473e24f ... slf4j-reload4j:2.0.17 expected 1 got 0 (BLD-08)`. A prototype of the D-01/D-02 exemption (about 40 lines of bash, bash-3.2 safe) turns that into `anchor_exempt=1 result=PASS`. Twelve prototype self-test cases (ST5-ST9) are green. They fail the right way against the old gate (7 FAIL) and against a deliberately over-broad mutation (2 FAIL).

**One finding contradicts the locked acceptance wording and needs a user decision before plan 21-01 commits anything: `spotlessCheck` is already red on `sync/v1.7.0` today, under spotless 8.10.1, on 46 files.** Spotless 8.10.4 produces the byte-identical violation set (`spotlessApply` diff md5 `3393873370f9b48d2239131eedba8ff8` before and after the merge). 37 of the 46 files are byte-identical to upstream `v1.7.0`. Every changed line is an import line. Earlier milestones declined a tree-wide reformat on purpose (Phase 7, 08-CONTEXT D-15, 11-02: "44 files ... Left alone"). So "spotlessCheck passes under 8.10.4" can only be met by a 46-file reformat commit. See Open Question 1.

`testUnit` on the merged tree is green: 556 tests, 554 succeeded, 0 failed, 2 skipped. The Lombok "Ambiguous: Jackson2 and Jackson3" warning is absent under 1.18.48 with `lombok.config` in place. Removing `lombok.config` makes the warning come back, so FORK-DELTA's BLD-14 rationale text is still true for 1.18.48. `slf4j-api` now resolves to 2.0.20 on all three classpaths (it was 2.0.17 / 2.0.17 / 2.0.18).

The pin-guard design in this document was run against the real `tests.yaml`, `build.gradle.kts` and `dependabot.yml` in a scratch harness (all pass) and against about 45 good/bad fixtures. It also found one extra weakness in the old `WRITE_SCOPE` regex: it accepts a quoted `contents: "write"`.

**Primary recommendation:** Do the work in four plans in this order: (1) DEPS-01 merge, with the spotless decision made first; (2) the exemption file, gate edit and self-test cases; (3) pin guard, extract then fixtures then fix in one green commit; (4) OPS, run orchestrator-inline, with Phase 21 verification held until 2026-10-17 12:00 UTC (15:00 local). That window had not opened when this research ran.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Anchors invariant past `6473e24f` (DEPS-01)**
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

**Pin-guard red proof (CI-18, CI-19)**
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

**Pin-guard scope (CI-18, CI-19, 17-REVIEW WR-01)**
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

**OPS timing & evidence (OPS-01..OPS-03)**
- **D-11:** OPS-01's observation passes on a REST API check **at or after 2026-10-17 12:00 UTC** that finds no `schedule` run of `integrationTests.yaml` created after the disable time.
  - Scheduled runs on the fork start about 4.5 h after the `00 05 * * *` cron: 2026-10-15 09:39 UTC and 2026-10-16 09:32 UTC.
  - 12:00 UTC leaves margin past that delay.
- **D-12:** **Phase 22 waits.** Phase 21 is verified and closed only after D-11's window has passed, and phases stay strictly sequential.
- **D-13:** The OPS confirmations are given **in free text, in the user's own words, at the start of Phase 21 execution**.
  - The orchestrator runs this inline: it shows the three checks with exact web-UI steps and records each answer verbatim beside the API facts. A screenshot is acceptable, as in gg2.
  - Clicking an option does not count as "in their own words".
  - The OPS-01 disable date comes from the user's answer. The API timestamp corroborates it.

**Locked by ROADMAP/STATE and not re-opened (CONTEXT `<domain>`)**
- The DEPS-01 merge is a merge: no rebase, no force, and no separate `.planning/` commit interleaved with it.
- If spotless 8.10.4 demands reformatting, it is its own commit. `spotlessCheck` and `testUnit` must be green, and the Lombok "Ambiguous: Jackson2 and Jackson3" warning must stay absent, before any Phase 22–23 code plan starts.
- CI-18/CI-19 read `tests.yaml` and `build.gradle.kts` and never edit them. The `timeout-minutes: 45` anchor survives.
- This phase pushes nothing. The merged tree's first CI run is Phase 24's rehearsal.
- Dependabot PR #4 (client `0.10.0`) stays open, unmerged and not ignored.
- OPS-01..03 are user actions in the GitHub web UI (D-11 milestone rule). No agent changes repository settings, and a relayed answer is not accepted.
- `integrationTests.yaml` is not edited, deleted or fixed.
- The 17-REVIEW closures made here are written into the archived disposition file by Phase 23 (DEBT-12), not here.
- `PRE_TAG` stays at its default `pre-v1.7.0-sync` (`c61d90ce`), and the gate command is unchanged (the ROADMAP's `PRE_TAG` note is superseded by D-01..D-04).

### Claude's Discretion
- The exemption file's name, column order, and whether it is read from `RANGE_END`'s tree (like `CONFLICTED_PATHS_FILE`) or the working tree. Recommendation: read it from `RANGE_END`'s tree, use TAB-separated columns like `fork-anchors.txt`, and require the full 40-hex SHA.
- Whether to rehearse the merge and the gate in a throwaway worktree before committing on `sync/v1.7.0`. Recommendation: yes. Expected result: no conflicts, and only the BLD-08 `slf4j-reload4j` row at `6473e24f` violating.
- The fixture format. Recommendation: inline Java text blocks in the test class, not resource files.
- The exact regexes and the step-block parsing for D-08 and D-10. They stay line-level, with no YAML library, per Phase 17 D-06.
- The plan split and commit order, within the fixed order: the DEPS-01 merge first, then any spotless reformat commit, the gate, and the CI-18/CI-19 work.
- The evidence file layout. Phase 17/20 precedent: a phase evidence file containing the merge dry-run, the gate and self-test output, the red fixture run, the `dependencyInsight` output, the Lombok-warning check and the OPS records.

### Deferred Ideas (OUT OF SCOPE)
- Aligning the catalog's `slf4j = "2.0.18"` with the `slf4j-api` version that `slf4j-reload4j:2.0.20` pulls in would be a dependency bump, which needs the user's approval. It is not done here. Research records the resolved versions only.
- Dependabot PR #4 (client `0.10.0`) is already a Future Requirement and is not discussed further.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEPS-01 | Working branch contains `origin/custom_build`'s three Dependabot commits (tip `6473e24f`) by merge; `spotlessCheck` passes under spotless 8.10.4; `testUnit` green | Rehearsal: clean merge, 2 files / 3 lines; D-03 edit set (3 files); gate prototype; testUnit 556/554/0/2; Lombok check; `dependencyInsight` table; **spotlessCheck baseline is red (Open Question 1)** |
| CI-18 | Guard fails if `Report test fork count` stops reading `reportTestForks`, or the task disappears from `build.gradle.kts` | Section "Pin guard design": step-block parser, `GRADLEW_REPORT`, `nprocProblem`, `forkTaskProblem` with fixtures, all checked on the real files |
| CI-19 | Guard accepts `runs-on: ubuntu-24.04 # comment` and no longer flags step `name:`/`run:` text containing `: write` | `RUNS_ON` and `WRITE_SCALAR`/`WRITE_FLOW` with must-reject and must-accept fixtures; old-regex red results listed |
| OPS-01 | User disables nightly `integrationTests.yaml`; REST API shows no `schedule` run after the disable date | REST facts and exact commands (state `disabled_manually`, `updated_at` `2026-10-16T09:38:23Z`, 0 later runs); D-11 window |
| OPS-02 | Dependabot `gradle` entry shows a recent "Last checked" with no config error | Public REST corroboration (`Dependabot Updates` runs); web-UI steps; the D-09 guard closes the code half |
| OPS-03 | Token revoked on GitHub; Keychain item `gsd17-gh-actions-read` deleted | Keychain lookup returns exit 44 (absent); GitHub-side revocation is user-only |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` -> `AGENTS.md` and `.claude/CLAUDE.md`:

- **Tests are only added, never deleted or weakened.** The five existing `@Test` methods keep their names, `@DisplayName`s and messages (also D-06).
- **Docker-free tests must not be tagged `clickhouse` and must not extend `ClickHouseBase`**, or `testUnit` skips them. `TestsWorkflowPinGuardUnitTest` already extends nothing.
- **Gradle always runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`** (carried rule; `config.json` `build_command`/`test_command` already do).
- **Spotless runs only on touched files.** This repo cannot file-scope it: `-PspotlessFiles` was re-tested this session and still reformatted all 46 files. Use the throwaway-worktree recipe under Pitfall 3.
- **Do not upgrade dependencies without asking.** The Dependabot commits are already merged on `origin/custom_build` by the user. The catalog `slf4j = "2.0.18"` stays.
- **Do not commit without the user asking.** Inside GSD execution, the plan's commits are the sanctioned path. Nothing is pushed in this phase.
- **Rebasability:** keep changes additive and small (PROJECT constraint). This is why a 46-file reformat needs the user's call.
- **Do not log secrets or data in plaintext.** The evidence file records no token value. The OPS-03 record names the Keychain item only.
- The `GSD Workflow Enforcement` block applies: edits happen through a GSD command, not ad hoc.
- Stale in `.claude/CLAUDE.md`: it says Gradle 7.4.2 and `slf4j-reload4j 2.0.17`. The real wrapper is Gradle 9.6.1 (`gradle/wrapper/gradle-wrapper.properties`: `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`). Do not treat that file as a version source.

## Architectural Responsibility Map

This phase has no browser, SSR, API or database tier. The "tiers" are the tooling layers that own each capability.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Branch integration of Dependabot bumps | git history on `sync/v1.7.0` | Gradle build (resolves the new versions) | A merge commit is the unit; the build only proves it resolves |
| Anchors exemption (D-01/D-02) | `.planning/scripts/verify-merge-invariants.sh` | `.planning/scripts/fork-anchor-exemptions.txt` (data) | The gate owns policy; the data file keeps later phases from editing the script |
| Exemption narrowness proof (D-04) | `.planning/scripts/verify-merge-invariants-selftest.sh` | dangling git objects | Self-test must not create refs; it synthesizes trees and commits |
| CI pin guard (CI-18, CI-19, D-09) | `testUnit` (JUnit, Docker-free) | none | Reads three text files; no YAML parser, no Gradle run |
| OPS-01..03 observation | GitHub web UI (user) | public REST API (corroboration), macOS Keychain lookup | Settings are the user's; the orchestrator only observes |

## Standard Stack

### Core

No new libraries, plugins or dependencies. [VERIFIED: rehearsal build, this session]

| Tool | Version | Purpose | Why |
|------|---------|---------|-----|
| git | 2.50.1 (Apple Git-155) | merge, `rev-list`, `commit-tree`, `merge-tree` | Used by the existing gate |
| bash | 3.2.57 (macOS `/bin/bash`) | gate and self-test | **No `declare -A`.** The exemption lookup is a `grep -qxF` on a keys file for that reason |
| JUnit Jupiter | 5.11.3 (per `.claude/CLAUDE.md`) | pin guard | Existing |
| Gradle wrapper | 9.6.1 | `testUnit`, `dependencyInsight`, `spotlessCheck` | Existing |
| Spotless plugin | 8.10.1 -> 8.10.4 (Dependabot #2) | import order and unused-import removal only (`build.gradle.kts` L495-501) | Existing |
| Lombok | 1.18.46 -> 1.18.48 (Dependabot #1) | boilerplate | Existing |
| slf4j-reload4j | 2.0.17 -> 2.0.20 (Dependabot #3) | log binding | Existing |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Line-level pin guard | SnakeYAML / TestKit run of `reportTestForks` | Rejected by Phase 17 D-06 and this phase's D-08 (no new dependency, Docker-free) |
| `declare -A` exemption map | `grep -qxF` on a keys file | bash 3.2 has no associative arrays; the keys file is portable |

**Installation:** none.

## Package Legitimacy Audit

No external package is installed or added in this phase. The three bumped coordinates (`org.projectlombok:lombok`, `com.diffplug.spotless` plugin, `org.slf4j:slf4j-reload4j`) were published on `origin/custom_build` by the user's merges of Dependabot PRs #1-#3, with CI green on each (STATE.md [Publish]). The gate and the audit command were not run, because nothing is installed by this phase.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
DEPS-01 (plan 1)
 origin/custom_build (6473e24f) ---+
                                   +--> git merge --no-commit --no-ff --> edit 3 bookkeeping files --> git commit (merge)
 sync/v1.7.0 (dcdbba01) -----------+                                          |
                                                                              v
                    build resolves lombok 1.18.48 / spotless 8.10.4 / slf4j-reload4j 2.0.20
                    spotlessCheck (baseline red, see OQ-1)  testUnit  Lombok-warning grep  dependencyInsight x3

Gate (plan 2)
 verify-merge-invariants.sh 54e63648 HEAD
   inv0 shape ---> inv1 provenance (54e63648 only) ---> inv2 markers (every commit in PRE_TAG..HEAD)
   inv3: for each commit c in range, with c's OWN fork-anchors.txt:
           count mismatch? --> key "c TAB path TAB pattern" in exempt-keys? --yes--> exempt_used++
                                                                         \--no---> ANCHOR VIOLATION
   exempt-keys <-- parsed from RANGE_END:.planning/scripts/fork-anchor-exemptions.txt
                   (malformed row --> ANCHOR EXEMPTION VIOLATION)
 selftest: synthetic x_* commits on pre-v1.7.0-sync + synthetic y_* range-end carrying a test exemption file

Pin guard (plan 3)
 tests.yaml --nonCommentLines--> rule(List<String>) --> Optional<String> offending message --+
 build.gradle.kts --ktsCode-----> rule(List<String>) --------------------------------------+--> @Test (real file)
 dependabot.yml + .github/workflows listing --> rule(List<String>, Predicate<String>) ------+--> @Test (fixtures, good and bad)

OPS (plan 4, orchestrator-inline)
 user free-text answers + REST facts (curl) + Keychain lookup --> 21-EVIDENCE.md
 final REST check at or after 2026-10-17T12:00:00Z
```

### Recommended Project Structure

```text
.planning/scripts/
  verify-merge-invariants.sh            # edit: exemption read + lookup + summary field
  verify-merge-invariants-selftest.sh   # edit: comment fix, ST5-ST9
  fork-anchors.txt                      # edit in the MERGE commit: L43 2.0.17 -> 2.0.20
  fork-anchor-exemptions.txt            # new: one row for 6473e24f
src/test/java/com/clickhouse/kafka/connect/ci/
  TestsWorkflowPinGuardUnitTest.java    # the only test file edited
.planning/phases/21-dependabot-integration-ci-hygiene/
  21-EVIDENCE.md                        # Phase 17 format (see Evidence layout)
```

### Pattern 1: Merge rehearsal (done; replay exactly in the real tree)

Rehearsal commands and results, run in a throwaway worktree (since removed) from `dcdbba01`: [VERIFIED: command output, this session]

```bash
git worktree add --detach /tmp/p21-rehearsal sync/v1.7.0     # HEAD is now at dcdbba01
cd /tmp/p21-rehearsal && git merge --no-commit --no-ff origin/custom_build
# Automatic merge went well; stopped before committing as requested        rc=0
git status --short                  # M  build.gradle.kts / M  gradle/libs.versions.toml
git diff --cached --stat            # build.gradle.kts | 2 +-   gradle/libs.versions.toml | 4 ++--   (3 insertions, 3 deletions)
git ls-files -u                     # empty: no unmerged paths
```

The staged diff is exactly `slf4j-reload4j:2.0.17 -> 2.0.20` (`build.gradle.kts` L106), `lombok = "1.18.46" -> "1.18.48"` and `spotless = "8.10.1" -> "8.10.4"` (`gradle/libs.versions.toml`). `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` is **8 21** now (CONTEXT says 8 19; two more `.planning/` commits landed since). After the merge the left side is 0.

D-03 bookkeeping edits, inside the same merge commit (after `--no-commit`, before `git commit`):

| File | Line | Edit |
|------|------|------|
| `.planning/scripts/fork-anchors.txt` | 43 | `build.gradle.kts<TAB>slf4j-reload4j:2.0.17<TAB>1<TAB>BLD-08` -> `...slf4j-reload4j:2.0.20...` |
| `docs/FORK-DELTA.md` | 63 | `` `org.slf4j:slf4j-reload4j:2.0.17` is declared as a literal coordinate `` -> `2.0.20` |
| `docs/FORK-DELTA.md` | 65 | "The catalog's Lombok 1.18.46 warns at compile time" -> `1.18.48` (still true, see Pitfall 6) |

`fork-anchors.txt` L48 `gradle/libs.versions.toml	kafka = "4.1.1"	1	BLD-08` is unchanged. Do not touch it.

The edit set is complete for tracked files outside `.planning/`: a `grep -rn "2\.0\.17\|1\.18\.46\|8\.10\.1"` over the repo (excluding `.git`, `build`, `.gradle`, `.planning`) hits only `CHANGELOG.md` L293 and L383 (history, leave), `build.gradle.kts`, `docs/FORK-DELTA.md` L63/L65, `gradle/libs.versions.toml` and `.claude/CLAUDE.md` L57 (a generated stack summary; not a gate; leave or refresh as the planner prefers).

### Pattern 2: SHA- and row-pinned exemption (prototype, verified)

**Where it fits.** Invariant 3 is `verify-merge-invariants.sh` L187-253. The loop at L211 walks every commit `c` in `RANGE_COMMITS`; each commit is checked against its own copy of the anchors file. The violation to forgive is the `elif [ "${a_actual}" != "${a_expected}" ]` branch at L247-250. Two properties come for free:

- `c` is the full 40-hex SHA from `git rev-list`, so the exemption key can be an exact full-SHA match.
- `CONFLICTED_PATHS_FILE` is read with `git show "${RANGE_END}:${CONFLICTED_PATHS_FILE}"` (L145). The exemption file is read the same way, from `RANGE_END`'s tree.

Because the key is a SHA, a commit cannot exempt itself: its SHA does not exist when its contents are written. Only already-published commits can be listed. [ASSUMED: design rationale, not a tool result]

**Exemption file** `.planning/scripts/fork-anchor-exemptions.txt`. Four TAB-separated fields: `40-hex SHA`, `path`, `fixed-string`, `reason`. The `path` and `fixed-string` are the anchor row's first two fields **as that commit's own copy of `fork-anchors.txt` spells them**. That is the pre-D-03 text `slf4j-reload4j:2.0.17`, because `6473e24f`'s anchors copy still says 2.0.17. They are not HEAD's text (which becomes 2.0.20 in the merge).

```text
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	build.gradle.kts	slf4j-reload4j:2.0.17	Dependabot #3 (origin/custom_build) bumped slf4j-reload4j to 2.0.20 without touching fork-anchors.txt; the merge commit moves the row (D-03)
```

**Gate patch** (45 lines changed; applied in a throwaway, `bash -n` clean, run under bash 3.2.57):

```bash
# header + defaults
ANCHOR_EXEMPTIONS_FILE="${ANCHOR_EXEMPTIONS_FILE-.planning/scripts/fork-anchor-exemptions.txt}"
exempt_used=0

# inside `if [ -n "${ANCHORS_FILE}" ]`, before `for c in ${RANGE_COMMITS}`:
: > "${WORK_DIR}/exempt-keys"
if [ -n "${ANCHOR_EXEMPTIONS_FILE}" ] \
  && git show "${RANGE_END}:${ANCHOR_EXEMPTIONS_FILE}" > "${WORK_DIR}/exempt-end" 2>/dev/null; then
  while IFS= read -r row <&5 || [ -n "${row}" ]; do
    is_data_row "${row}" || continue
    tabs="${row//[^${TAB}]/}"
    e_sha="${row%%${TAB}*}";        rest="${row#*${TAB}}"
    e_path="${rest%%${TAB}*}";      rest="${rest#*${TAB}}"
    e_pattern="${rest%%${TAB}*}";   e_reason="${rest#*${TAB}}"
    if [ "${#tabs}" -ne 3 ] || ! [[ "${e_sha}" =~ ^[0-9a-f]{40}$ ]] \
      || [ -z "${e_path}" ] || [ -z "${e_pattern}" ] || [ -z "${e_reason}" ]; then
      echo "ANCHOR EXEMPTION VIOLATION: malformed row (need a full 40-hex SHA, path, fixed-string and reason, TAB-separated): ${row}" >&2
      fail=1; continue
    fi
    printf '%s\t%s\t%s\n' "${e_sha}" "${e_path}" "${e_pattern}" >> "${WORK_DIR}/exempt-keys"
  done 5< "${WORK_DIR}/exempt-end"
fi

# replaces the L247-250 mismatch branch
elif [ "${a_actual}" != "${a_expected}" ]; then
  if grep -qxF -e "${c}${TAB}${a_path}${TAB}${a_pattern}" "${WORK_DIR}/exempt-keys"; then
    exempt_used=$((exempt_used + 1))
  else
    echo "ANCHOR VIOLATION: ${short_c} ${a_path} '${a_pattern}' expected ${a_expected} got ${a_actual} (${a_backs})" >&2
    fail=1
  fi
fi

# summary line gains one field, before result=
echo "invariants: provenance_paths=... marker_commits=${marker_commits} anchor_exempt=${exempt_used} result=${result}"
```

Design choices, each deliberate:

- Only the **count-mismatch** branch is forgiven. A missing path, a failed search or a malformed anchor row at that SHA still fails.
- **Missing exemption file at `RANGE_END` means no exemptions**, not an error. `6473e24f` then fails on its own, so the gate fails closed anyway. Empty `ANCHOR_EXEMPTIONS_FILE=` disables the feature (used by ST5-disabled and by the "fails without the exemption" real-history evidence run).
- The summary field `anchor_exempt=N` goes **before** `result=`. Nothing else parses the line: the only consumers are `selftest` (`tail -1 | grep 'result=PASS'`) and prose quotes in archived audits. The selftest's `result=PASS` grep still matches.
- An unused row (SHA not in range) is **not** an error. The count is reported, and ST9 pins `anchor_exempt=1`. Failing on unused rows would break any run whose range does not contain the SHA.
- Optional hardening the planner may add: pin the forgiven `got` count as a fifth column. Not recommended: CONTEXT D-02 specifies path, fixed-string, SHA and reason only.

**Rehearsal gate results** (all with the unmodified current script unless stated; `54e63648` as merge arg; each run took 4:46-4:59 wall with three jobs in parallel, about 2.5 min of CPU alone): [VERIFIED: command output, this session]

| Range end | Exemption | Result |
|-----------|-----------|--------|
| merge A (merge only, no D-03 edits) | n/a | FAIL, two lines: `ANCHOR VIOLATION: 108cd757 build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)` and the same line for `6473e24f` |
| merge B (merge + D-03 edits in the merge commit) | none (old script) | FAIL, exactly one line: `ANCHOR VIOLATION: 6473e24f build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)`; summary `provenance_paths=21 anchor_commits=378 anchor_rows=59 marker_commits=425 result=FAIL` |
| C = B + exemption file + patched script | one row for `6473e24f` | **`provenance_paths=21 anchor_commits=379 anchor_rows=59 marker_commits=426 anchor_exempt=1 result=PASS`**, rc=0, empty stderr |

Merge A proves D-03 is required, not optional: without the bookkeeping edits the merge commit itself violates its own anchors file, and that SHA is not yet known when an exemption row would have to be written. The newly included commits `f328fd21`, `537beb1f`, `be6b8861`, `a246ca01`, `557e48c0` and `ce8ab0e0` all pass. Only `6473e24f` violates.

### Pattern 3: Self-test cases ST5-ST9 (prototype, verified)

Existing style (`verify-merge-invariants-selftest.sh` L33-65): `report`, `synth_tree <base> <path> <content-file>` (dangling tree = base tree with one path replaced) and `expect_failure <name> <needle> <env...> -- <script-args...>`. The new cases add two small helpers (`exemption_range`, `exemption_row`) and build a **tiny synthetic range** so each case takes about 5 s instead of the 2+ minutes a full-history range costs:

- `x_slf4j` = `commit-tree` of `6473e24f`'s tree, parent `pre-v1.7.0-sync`. It violates only the BLD-08 slf4j row.
- `x_both` = the same tree with `kafka = "4.1.1"` bumped to `"4.1.2"` via `synth_tree`. It violates both BLD-08 rows.
- `y_*` = `synth_tree HEAD <exemptions path> <file>`, child of `x_*`. This is the range end, and it carries the test exemption file. It satisfies its own anchors because it is HEAD's tree. The rows are built from the synthetic SHAs at run time, so the real `6473e24f` row is irrelevant to these cases.

| Case | Setup | Expected |
|------|-------|----------|
| ST5 | empty exemption file | fail, stderr matches `ANCHOR VIOLATION: .* build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0` |
| ST5-disabled | valid row, `ANCHOR_EXEMPTIONS_FILE=` | same failure (the row is what saves it) |
| ST6-other-row | row for `x_slf4j` + `gradle/libs.versions.toml` + `kafka = "4.1.1"` | slf4j violation still reported |
| ST6-other-commit | row for another SHA (`pre-v1.7.0-sync`) with the slf4j row | slf4j violation still reported |
| ST7 | `x_both`, row for the slf4j row only | fail with the **kafka** needle; a second check that `slf4j-reload4j:2.0.17` is **absent** from stderr (proves the exemption really fired) |
| ST8-* | one malformed row each: abbreviated 7-hex SHA, uppercase SHA, no reason field, empty reason, extra fifth field | fail, `ANCHOR EXEMPTION VIOLATION` |
| ST9 | exact row on `x_slf4j` (positive control) | rc 0 and last line contains `anchor_exempt=1 result=PASS` |

Why ST9 is not optional: without a positive control, ST6-ST8 pass for any unrelated failure.

Results: [VERIFIED: command output, this session]
- New cases alone: 12/12 PASS, 65 s.
- Full self-test on the rehearsal tip (merge B + patched gate + exemption file): ST1-ST4 and ST5-ST9 all PASS, `selftest: no failures`, 6 min 38 s wall. The default lookup resolved `54e63648`.
- Against the **old** gate: 7 FAIL (`ST7-exempted-row-silent`, five `ST8-*`, `ST9`). ST5/ST6 pass on the old gate because they assert narrowness, not the feature, so ST7-silent and ST9 are the fail-first anchors. Say so in the evidence file.
- Against a mutated gate that keys on the SHA only (`grep -qF -e "${c}"`): `ST6-other-row` and `ST7` FAIL. That proves the narrowness cases can fail.

**Default merge lookup (D-04 last clause).** `git rev-list --first-parent --merges pre-v1.7.0-sync..<merge B> | tail -1` prints the DEPS-01 merge first and `54e6364858cd728e3427ce2058fc00380895c689` last, so `tail -1` still resolves `54e63648`. `rev-list` prints newest first, so `tail -1` is the *oldest* first-parent merge. The "single first-parent merge" wording at selftest L6 is wrong after DEPS-01. Fix L6 and add a one-line comment above L28 (both in the prototype).

**ST4 weakness to know about.** ST4's needle is just `ANCHOR VIOLATION` (selftest L109, run over the full range). Once the DEPS-01 merge is in and **before** the exemption is wired, ST4 passes vacuously because `6473e24f` already violates. After wiring it is meaningful again. Optional strengthening, which is allowed because it does not weaken the test: use the first row's pattern (`TARGET_TABLE_FILTER`) as the needle.

### Pattern 4: Pin guard design (D-06 extraction, D-08, D-09, D-10)

All code below was executed in a scratch harness (JDK 21, single-file source, run from the repo root against the real files) and passed. It uses only Java 17 APIs. [VERIFIED: command output, this session]

**Shape.** Each rule is `static Optional<String> xxx(List<String> lines)` returning the offending message (empty = accept). The existing five tests keep their `@Test`, `@DisplayName` and message text, and become `assertTrue(rule(lines).isEmpty(), message)` or `assertEquals(Optional.empty(), rule(lines))` on the real file. Messages are produced by the rule, so they stay byte-identical to today's (`"Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '<line>'"` and so on). Rules that today assert "at least N / at least one found" (`MIN_PINNED_ACTIONS`, "No 'runs-on:' line", "No checkout step") keep that guard inside the rule, so an **empty list is rejected** and a fixture for it is mandatory.

New constants and regexes (final forms):

```java
// CI-19 runs-on: strip a trailing " #..." (YAML needs whitespace before '#') before the equality check
private static final Pattern RUNS_ON = Pattern.compile("^\\s*runs-on:\\s*(.*?)\\s*(?:\\s#.*)?$");

// CI-19 write scope: a key whose WHOLE scalar value is write / write-all (optionally quoted, optional trailing
// comment), except the step keys name: and run:. Free text such as "name: write summary" no longer matches.
private static final Pattern WRITE_SCALAR = Pattern.compile(
        "^\\s*(?!name:|run:)[\\w-]+:\\s*[\"']?write(?:-all)?[\"']?\\s*(?:\\s#.*)?$");
// flow mapping on a permissions line: permissions: { contents: write }
private static final Pattern WRITE_FLOW = Pattern.compile("^\\s*permissions:\\s*\\{[^}]*:\\s*write\\b[^}]*\\}.*$");

// CI-18 tests.yaml
private static final Pattern FORK_STEP = Pattern.compile("^(\\s*)-\\s+name:\\s*Report test fork count\\s*(?:\\s#.*)?$");
private static final Pattern GRADLEW_REPORT = Pattern.compile(
        "^(?!\\s*(?:echo|printf)\\b).*\\./gradlew\\s.*\\breportTestForks\\b.*$");
// CI-18 build.gradle.kts
private static final Pattern REGISTER_FORKS = Pattern.compile("^tasks\\.register\\(\"reportTestForks\"\\)\\s*\\{\\s*$");
```

**D-08 `tests.yaml` rule.** Step boundary by **indent**, not `LIST_ITEM`: the step ends at the first non-blank line whose indent is <= the `- name:` line's indent. `LIST_ITEM` (any `- ` line) also works on today's file but a shell line starting with `- ` inside the `run:` block would end the step early.

```java
static Optional<String> forkCountStepProblem(List<String> lines) {
    for (int i = 0; i < lines.size(); i++) {
        Matcher m = FORK_STEP.matcher(lines.get(i));
        if (!m.matches()) continue;
        int stepIndent = m.group(1).length();
        for (int j = i + 1; j < lines.size(); j++) {
            String l = lines.get(j);
            if (!l.isBlank() && indent(l) <= stepIndent) break;
            if (GRADLEW_REPORT.matcher(l).matches()) return nprocProblem(lines);
        }
        return Optional.of("Step 'Report test fork count' must call './gradlew ... reportTestForks'");
    }
    return Optional.of("No step named 'Report test fork count' in tests.yaml");
}

static Optional<String> nprocProblem(List<String> lines) {
    for (String l : lines) {
        if (!l.contains("nproc")) continue;
        String s = l.strip();
        // the one message line may print nproc as context; anything else is a shell copy of the formula
        if (s.startsWith("msg=") && !s.replace("nproc=$(nproc)", "").contains("nproc")) continue;
        return Optional.of("nproc may only appear in the msg= context line, found: '" + s + "'");
    }
    return Optional.empty();
}
```

On the real `tests.yaml` (comment lines removed by `nonCommentLines`): the step is found, L53 `forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)"` matches `GRADLEW_REPORT`, and the only `nproc` line is L60 `msg="runner nproc=$(nproc), ..."`, so the rule returns empty. The `::error::reportTestForks did not print an integer` line (L56) does not match, because it has no `./gradlew`.

**D-08 `build.gradle.kts` rule.** Strip `/* ... */` blocks and `//` lines first. The file has a `/* Will in side the Confluent Archive` block comment near L108, and L254 is a `//` comment that mentions `reportTestForks`, so a naive `contains` would be satisfied by a comment. Then find the `tasks.register("reportTestForks") {` line at **column 0** and read to the first line that equals `}` (the task's closing brace is at column 0, L262):

```java
static List<String> ktsCode(String text) {
    String noBlock = text.replaceAll("(?s)/\\*.*?\\*/", "");
    return noBlock.lines().filter(l -> !l.stripLeading().startsWith("//")).collect(Collectors.toList());
}
static Optional<String> forkTaskProblem(List<String> lines) {
    for (int i = 0; i < lines.size(); i++) {
        if (!REGISTER_FORKS.matcher(lines.get(i)).matches()) continue;
        boolean readsTest = false, readsForks = false;
        for (int j = i + 1; j < lines.size() && !lines.get(j).equals("}"); j++) {
            readsTest |= lines.get(j).contains("tasks.named<Test>(\"test\")");
            readsForks |= lines.get(j).contains("maxParallelForks");
        }
        return readsTest && readsForks ? Optional.empty()
            : Optional.of("reportTestForks must read maxParallelForks from the test task");
    }
    return Optional.of("tasks.register(\"reportTestForks\") not found in build.gradle.kts");
}
```

Read `build.gradle.kts` as UTF-8 explicitly (`Files.readString(path, StandardCharsets.UTF_8)`), like `nonCommentLines`; the test JVM runs with `file.encoding=windows-1252`.

**D-09 rule.** Parse the `exclude-paths:` list **inside the `github-actions` entry only** (a list under the `gradle` entry must not count). The existence check takes a `Predicate<String>` so fixtures can inject a fake file set, and the real test passes `p -> Files.exists(Path.of(p))` (Gradle runs tests with the project dir as cwd, same as the existing `Path.of(".github", ...)` reads):

```java
private static final List<String> UPSTREAM_WORKFLOWS = List.of(
    ".github/workflows/increment-version.yml", ".github/workflows/integrationTests.yaml",
    ".github/workflows/pre-release.yaml", ".github/workflows/release.yaml");
// excludePaths(lines): find "- package-ecosystem: github-actions"; within that entry (until the next
// list item at the same or lower indent) read the items under "exclude-paths:" (item regex: ^\s*-\s*["']?([^"'#]+?)["']?\s*(?:#.*)?$)
static Optional<String> dependabotExcludeProblem(List<String> lines, Predicate<String> exists) { ... }
```

On the real file `excludePaths` returns exactly the four paths, and all four exist.

**Fixtures.** Use `List.of("...", "...")` for fixtures, not text blocks: the indent-aware parsers depend on exact leading spaces, and a text block strips common leading whitespace relative to the closing delimiter. Use a text block only where indentation does not matter.

| Rule | Must ACCEPT | Must REJECT |
|------|-------------|-------------|
| runs-on | `    runs-on: ubuntu-24.04 # pinned`; `    runs-on: ubuntu-24.04    # padded   ` | `    runs-on: ubuntu-latest # x`; `    runs-on: ubuntu-latest`; `    runs-on: ubuntu-22.04 # x`; `    runs-on: ubuntu-24.04#x` (no space, so not a comment); `    runs-on: # ubuntu-24.04` (empty value); `    runs-on:`; `    runs-on: ${{ matrix.os }}`; empty list |
| write scope | `      - name: write summary`; `        name: write summary`; `      - name: Write summary: write`; `        run: echo "mode: write"`; `          echo "contents: write" >> "$GITHUB_STEP_SUMMARY"`; `        run: write`; `      - run: echo write-all` | `  contents: write`; `permissions: write-all`; `      contents: write`; `    permissions: write-all`; `  contents: "write"`; `  contents: 'write'`; `  packages: write # why`; `  id-token: write`; `permissions: { contents: write }`; `permissions: {contents: write, issues: read}`; **job-level block** `[permissions:, contents: read, jobs:, build:, "    permissions:", "      contents: write"]` |
| fork step (tests.yaml) | the real step shape (name, `run: |`, `forks="$(./gradlew -q ... reportTestForks ...)"`, `msg="runner nproc=$(nproc), ..."`, `echo "$msg"`, blank, next step); step as the last step in the file | shell formula `forks=$(( $(nproc) / 2 ))` with only the `::error::reportTestForks` mention; the call placed in the NEXT step only; `echo "./gradlew reportTestForks"` (echo, not a call); a real call plus a `forks=$(( $(nproc) / 2 ))` line; step renamed (`Count forks`); empty list; `msg="$(( $(nproc) / 2 ))"` |
| build.gradle.kts | the real register block | task deleted, only in a `//` comment; renamed to `reportForks`; constant `println(6)`; reads `tasks.named<Test>("testUnit")`; whole block inside `/* ... */`; a `//` mention followed by an unrelated task that contains `maxParallelForks` |
| dependabot | real shape; all four paths, all existing | one path missing (`release.yaml`); a typo (`integrationTest.yaml`); a stale entry for a path that does not exist; all four paths only under the `gradle` entry; empty list |

**Which fixtures the OLD regexes get wrong (the D-07 red run).** Measured with the old patterns in the harness: [VERIFIED: command output, this session]

| Fixture | Old rule result | Verdict |
|---------|-----------------|---------|
| `runs-on: ubuntu-24.04 # pinned` (good) | rejected (group is `ubuntu-24.04 # pinned`) | RED: false failure (CI-19) |
| `name: write summary`, `run: echo "mode: write"`, `echo "contents: write" >> ...` (good) | rejected (`.*:\s*write\b.*`) | RED: false failure (CI-19) |
| `contents: "write"`, `contents: 'write'` (bad) | **accepted** | RED: a real hole in the old regex, not in the CONTEXT |
| `contents: write`, `permissions: write-all`, job-level block, `permissions: {...write}`, `runs-on: ubuntu-latest # x`, `ubuntu-22.04 # x`, `runs-on:` (bad) | rejected | already green: these are the "must still reject" regression guards of D-10 |
| every CI-18 / D-09 rule | no old implementation | RED by construction: ship the rule methods as `Optional.empty()` stubs for the red run |

Practical sequence for D-07 so every commit stays green:
1. Extract the existing five rules as static methods **with the old regexes unchanged** (behaviour-preserving). Add the CI-18 / D-09 methods as stubs returning `Optional.empty()`.
2. Add the fixture tests and the three new real-file tests. Run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'`. Record the red test names and messages in the evidence file. Expect the three RED groups above and the stubs.
3. Swap in the new regexes and real rule bodies. The full class is green. Commit tests and fix together.

Two small consistency points for the planner (discretion, not required by D-10):
- `everyJobRunsOnUbuntu2404` also does `line.contains(FLOATING_RUNNER)` on the raw line. `runs-on: ubuntu-24.04 # was ubuntu-latest` would still fail it. Strip the same trailing comment before that check too, and add that fixture, so the "outside comments" message stays true. The only text this could hide is a `#` run inside a shell string, which is not a runner.
- `TOP_LEVEL_PERMISSIONS = "permissions:"` is matched by exact line equality, so `permissions: # note` fails closed. Out of scope here, so note it only.

### Anti-Patterns to Avoid

- **Keying the exemption on the backs-id** (`BLD-08`): it backs two rows. ST6-other-row and ST7 exist to catch this.
- **Writing the exemption row's pattern as HEAD's text.** It must be the text in `6473e24f`'s own anchors copy (`2.0.17`).
- **`declare -A` in the gate or self-test.** It fails on the macOS bash 3.2 that runs these scripts.
- **A `.planning/`-only commit between `git merge --no-commit` and `git commit`**, or a GSD state commit mid-merge (see Pitfall 2).
- **Reading `build.gradle.kts` with a bare `contains("reportTestForks")`**: a comment satisfies it.
- **Running a YAML library or TestKit for the guard** (Phase 17 D-06, D-08).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dangling test commits for the self-test | New helper scaffolding | The existing `synth_tree` + `git commit-tree` in `verify-merge-invariants-selftest.sh` | Already creates no refs; matches ST2-ST4 |
| Exemption lookup | An associative array or JSON file | `grep -qxF -e "<key>" <keys-file>` | bash 3.2 has no `declare -A` |
| Pin-guard file reading | A new reader | The existing `nonCommentLines(Path)` (UTF-8 explicit) | The test JVM uses `windows-1252` |
| OPS-01 state check | A scraper | The public REST workflow endpoint, plus a control query | Authoritative, unauthenticated, 60 req/h |
| Reformatting 46 files by hand | Hand edits | `./gradlew spotlessApply` in a throwaway worktree, if the user chooses reformat | The diff is import-only and machine-generated |

**Key insight:** every piece this phase needs already exists in the repo as a helper, so each plan is an additive edit to one or two files.

## Runtime State Inventory

Not a rename or migration phase. The version-string edits in D-03 are in tracked files only.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None: no datastore keys the changed strings | none |
| Live service config | `integrationTests.yaml` workflow state lives in GitHub (disabled), not in git | OPS-01 observation only |
| OS-registered state | macOS Keychain item `gsd17-gh-actions-read`: `security find-generic-password -s gsd17-gh-actions-read` exits 44 (not found) | none; re-run at OPS-03 |
| Secrets/env vars | The GitHub token behind that Keychain item (expiry 1 day per STATE.md; [ASSUMED] now expired) | user revokes/deletes it on GitHub (OPS-03) |
| Build artifacts | Generated `Version.java` is unaffected (`VERSION` is not touched here) | none |

## Common Pitfalls

### Pitfall 1: The merge commit violates its own anchors file unless D-03 is applied in the same commit
**What goes wrong:** committing the plain merge makes `build.gradle.kts` say `2.0.20` while its own `fork-anchors.txt` still says `2.0.17`. Merge A in the rehearsal produced a second violation line for the merge SHA itself. That SHA cannot be exempted in advance.
**How to avoid:** `git merge --no-commit --no-ff origin/custom_build`, make the three edits, then `git commit` once. Verify with `git show --stat HEAD` that the merge commit shows 4 files vs the first parent: `build.gradle.kts`, `gradle/libs.versions.toml`, `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`.
**Warning signs:** `ANCHOR VIOLATION: <merge-sha> ... slf4j-reload4j:2.0.17`.

### Pitfall 2: Dirty `.planning/state.json` and GSD commits during the merge
**What goes wrong:** `git status` shows ` M .planning/state.json` at research time. The merge does not touch it, so the merge runs, but a GSD state commit between `--no-commit` and `git commit` would either fail mid-merge or fold the state change into the merge commit. The rehearsal was in a clean worktree and did not exercise this.
**How to avoid:** commit or stash state before the merge, and run the merge, edits and `git commit` back to back in one step with no GSD tool call in between. Write the merge message with `git commit -m` (or `-F`) and state that it carries bookkeeping edits that are in neither parent. No hooks exist (`core.hooksPath` unset, no `.git/hooks`).

### Pitfall 3: `spotlessCheck` is red at baseline; spotless cannot be file-scoped here
**What goes wrong:** treating "spotlessCheck passes" as a pass/fail gate fails before any work.
**Facts:** [VERIFIED: command output, this session]
- `spotlessCheck` on `sync/v1.7.0` (8.10.1) and on the merged tree (8.10.4): `Violations also present in 44 other files`, 46 files in total, 260 insertions / 337 deletions after `spotlessApply`, **identical diff** (md5 `3393873370f9b48d2239131eedba8ff8`).
- 17 files in `src/main`, 19 in `src/test`, 5 in `src/integrationTest`, 5 in `src/testFixtures`. **Zero non-import changed lines.** 9 of the 46 are fork-modified; 37 are byte-identical to upstream `v1.7.0`.
- A full `spotlessApply` leaves all 59 anchor rows intact (checked with a mirror of invariant 3 against the formatted tree: `rows=59 bad=0`; negative control detects a real change).
- `-PspotlessFiles='.*ClickHouseCloudTest\.java'` still changed all 46 files. `TestsWorkflowPinGuardUnitTest.java` is not among the 46.
- Precedent: 11-02-SUMMARY (v1.6.0): "`spotlessCheck` fails tree-wide on 44 files for pre-existing import-order violations ... Left alone"; 08-CONTEXT D-15: a tree-wide `spotlessApply` "was already declined in Phase 7".
**How to avoid / per-file recipe** (needed whichever option is chosen, for the pin-guard edit):
```bash
git worktree add --detach /tmp/p21-fmt HEAD
cp src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java /tmp/p21-fmt/src/test/java/com/clickhouse/kafka/connect/ci/
(cd /tmp/p21-fmt && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply \
  && git diff --exit-code -- src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java)
git worktree remove --force /tmp/p21-fmt
```
Import order is ASCII with static imports first, so uppercase sorts before lowercase: `java.util.List`, `java.util.Optional`, `java.util.Set`, `java.util.function.Predicate`, `java.util.regex.Matcher`.
**Also:** `git stash` is shared across worktrees. A `git stash` run in a throwaway worktree lands in the main repo's stash list (one of mine did and was dropped). Use `git checkout -- .` in throwaway trees.

### Pitfall 4: The gate takes minutes; do not run it in the foreground without a plan
**What goes wrong:** about 2.5 min CPU alone, 4:46-4:59 wall with three jobs in parallel. The self-test is 6:38 wall. **How to avoid:** run both with `run_in_background`, redirect stdout and stderr to files, and read them when notified. The new self-test cases are cheap (about 5 s each) because their range is synthetic.

### Pitfall 5: zsh `path` is tied to `PATH`
**What goes wrong:** `while read -r path ...` in zsh overwrote `PATH` (a later `git` returned "command not found"). **How to avoid:** use `a_path`, or run helper scripts under `bash`. The checked-in scripts are bash and already use `a_path`.

### Pitfall 6: Lombok warning check needs a control
**What goes wrong:** "no warning" is meaningless without proving the warning can appear. **How to avoid:** the recorded method is below. Run it on a clean compile (`clean compileJava`), because an up-to-date `compileJava` prints nothing.
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava compileTestJava compileIntegrationTestJava compileTestFixturesJava \
  > /tmp/p21-compile.txt 2>&1; grep -c -i "ambiguous" /tmp/p21-compile.txt     # expect 0
mv lombok.config /tmp/lombok.config.bak && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava > /tmp/p21-nocfg.txt 2>&1
grep -n -i "ambiguous" /tmp/p21-nocfg.txt     # control: expect the warning
mv /tmp/lombok.config.bak lombok.config
```
Observed: 0 matches with `lombok.config`; without it, `ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'. See https://projectlombok.org/features/experimental/Jacksonized`. Do the control in a throwaway worktree, not the real tree. The build is `BUILD SUCCESSFUL`; the 100 / 4 / 74 warnings printed are `[deprecation]` and `[unchecked]` lines, none about Lombok.

### Pitfall 7: Gate exemption row cannot reference HEAD's anchor text
**What goes wrong:** after D-03, `fork-anchors.txt` says `2.0.20` at HEAD, so a "stale-row" check against HEAD's anchors would call the exemption stale. **How to avoid:** do not add such a check. The row refers to `6473e24f`'s own anchors copy.

### Pitfall 8: OPS-01 window has not opened, and `gh` exists on this machine
**Facts:** research ran at 2026-10-16 ~22:00 UTC. D-11's window opens 2026-10-17 12:00 UTC = 15:00 local (+03:00). The first cron after the disable is 2026-10-17 05:00 UTC; the fork's runs start about 4.5 h late. STATE.md says `gh` is not installed, but `command -v gh` printed `/opt/homebrew/bin/gh` here. It was **not used**, nothing was authenticated, and D-11 says the REST API is the path. The planner should not start using it; flag the discrepancy to the user.

## Code Examples

### dependencyInsight before and after (D-03 context item)

Commands (per classpath, `-q --console=plain`, `env -u TESTCONTAINERS_HOST_OVERRIDE`):
```bash
./gradlew dependencyInsight --dependency org.slf4j:slf4j-api --configuration runtimeClasspath            # and testRuntimeClasspath, integrationTestRuntimeClasspath
./gradlew dependencyInsight --dependency org.slf4j:slf4j-reload4j --configuration runtimeClasspath
```
Results. "Before" is current `sync/v1.7.0`; "after" is the rehearsal merge. [VERIFIED: command output, this session]

| Configuration | Selected `slf4j-api` before | after | Selection reason (after) |
|---------------|-----------------------------|-------|--------------------------|
| `runtimeClasspath` | 2.0.17 | **2.0.20** | conflict resolution among 2.0.20, 2.0.7, 1.7.36; chosen via `slf4j-reload4j:2.0.20` |
| `testRuntimeClasspath` | 2.0.17 | **2.0.20** | conflict resolution among 2.0.20, 2.0.7, 1.7.36, 1.7.30, 2.0.3, 1.7.25, 1.7.35, 1.7.2, 1.7.32; via `slf4j-reload4j:2.0.20` |
| `integrationTestRuntimeClasspath` | 2.0.18 (from the catalog's `slf4j = "2.0.18"` via `slf4j-simple`) | **2.0.20** | conflict resolution among 2.0.20, 2.0.18, 2.0.7, 1.7.36, 1.7.30 |
| `slf4j-reload4j` on `runtimeClasspath` | 2.0.17 | 2.0.20 | declared literal |

Phase 24 rollout note: the shipped runtime now carries `slf4j-api` 2.0.20 (was 2.0.17), and the integration-test classpath moved from 2.0.18 to 2.0.20, so the 2.0.18 / 2.0.17 mismatch there is gone. The catalog entry `slf4j = "2.0.18"` is unchanged and still pins `slf4j-simple` itself (not edited, deferred). The classpath still carries two bindings (reload4j and simple) in the integration set. That is pre-existing and not touched.

### Build and test results on the merged tree
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava compileTestJava compileIntegrationTestJava compileTestFixturesJava`: BUILD SUCCESSFUL, no Lombok warning.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit`: `Test result: SUCCESS`, `Tests summary: 556 tests, 554 succeeded, 0 failed, 2 skipped`. The two skips are `ConvertToMapOrJsonPerformanceTestJson > testWideStructPerformance()` and `testDeepNestedStructPerformance()`. Run with Docker (colima `vz`) up. `testUnit` itself is Docker-free; Phase 24 runs it with Docker stopped.
- `spotlessCheck`: FAILED, same as baseline (Pitfall 3).

### OPS REST checks (no auth; limit 60 req/h)

[VERIFIED: command output, this session] Repository `ne1r0n/clickhouse-kafka-connect`.

```bash
R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect
# OPS-01 state: state, updated_at (= the disable time), path
curl -s $R/actions/workflows/integrationTests.yaml          # id 384641581, "state": "disabled_manually", "updated_at": "2026-10-16T09:38:23Z"
# OPS-01 observation: schedule runs created after the disable time (expect total_count 0)
curl -s "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E2026-10-16T09:38:23Z&per_page=100"
# control, proves the filter and the API are working (expect 2: ids 38881153720 and 38714528739)
curl -s "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E2026-10-01T00:00:00Z"
# repo-wide cross-check (expect total_count 0)
curl -s "$R/actions/runs?event=schedule&created=%3E2026-10-16T09:38:23Z&per_page=100"
# record GitHub's own clock for the D-11 window check
curl -sI "$R/actions/workflows/integrationTests.yaml" | grep -i '^date:'
```
Observed at research time:
- Workflow list: `384641581 Integration Tests .github/workflows/integrationTests.yaml disabled_manually 2026-10-16T09:38:23.000Z`. The list also shows `384810012 Dependabot Updates dynamic/dependabot/dependabot-updates active` and `92415971 ClickHouse Kafka Connect Tests CI ... active`.
- Schedule runs: `38881153720` (2026-10-16T09:32:28Z, failure) and `38714528739` (2026-10-15T09:39:18Z, failure), both `custom_build` @ `a246ca01`. The query for runs created after `2026-10-16T09:38:23Z` returned `total_count` 0 (workflow-scoped and repo-wide). A control with a cutoff of `2026-10-16T09:32:29Z` also returned 0; `>2026-10-01` returned 2.
- Pass condition (D-11): run the query at or after `2026-10-17T12:00:00Z`; `total_count` must be 0 and the `Date:` header must be >= that time. The disable date to record comes from the **user's answer**; `updated_at` only corroborates it.
- `integrationTests.yaml` is byte-identical to upstream `v1.7.0` on both `sync/v1.7.0` and `origin/custom_build` (`git diff --quiet v1.7.0 <ref> -- .github/workflows/integrationTests.yaml` succeeds).
- Default branch is `custom_build`, tip `6473e24f372a8b37dfd8cc8cf3124e6cff52edca`, `protected: false`. PRs: #1, #2, #3 merged on 2026-10-16 (10:32 UTC); #4 open; #5 closed.
- **OPS-02 corroboration (public):** `GET $R/actions/workflows/384810012/runs` lists two completed, successful Dependabot jobs: `gradle in /. - Update #1627525145` (2026-10-14T21:05:35Z) and `github_actions in /. - Update #1627650108` (2026-10-14T21:09:55Z). The `gradle` entry has run and succeeded. The schedule is `monthly`, so "recent" means about 3 days old now, and the next run is not due until about mid-November. The UI "Last checked" field itself is not exposed by the public API, so the user's own reading is the evidence.
- **OPS-03:** `security find-generic-password -s gsd17-gh-actions-read >/dev/null 2>&1; echo $?` printed `44` (item not found). Re-run it in the OPS-03 record. The GitHub-side revocation cannot be observed by anyone but the user.

### Web-UI steps to show the user (D-13)

Show these three, ask for free-text answers, and record each verbatim with the API facts beside it.

1. **OPS-01, nightly disabled.** Open `https://github.com/ne1r0n/clickhouse-kafka-connect` -> **Actions** -> in the left sidebar click **Integration Tests** -> the "Show workflow options" (three-dots) menu -> **Disable workflow**. Already disabled per the API; ask them to say in their own words when they did it and what the page shows now. A disabled workflow offers **Enable workflow** in the same menu. [CITED: docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/disabling-and-enabling-a-workflow] The banner text itself is [ASSUMED].
2. **OPS-02, Dependabot `gradle` entry.** **Insights** -> **Dependency graph** -> **Dependabot** tab. Read, for the `gradle` entry in `/`, the "Last checked" time and whether any configuration error is shown. **Recent update jobs** next to the manifest opens the job list; **view logs** shows a job. [CITED: docs.github.com/en/code-security/dependabot/working-with-dependabot/viewing-dependabot-job-logs] "Last checked" wording and the error indicator are [ASSUMED] (the docs fetched do not name them; the gg2 close record in `20-MILESTONE-CLOSE-RECORD.md` uses the same words). **Tell the user not to click "Check for updates"** if it is offered: it would trigger a job and could reopen or create PRs (#4, #5), which the scope decisions keep untouched. [ASSUMED] that the button exists and behaves that way.
3. **OPS-03, token.** On GitHub: avatar -> **Settings** -> **Developer settings** -> **Personal access tokens** -> **Fine-grained tokens** -> the token created for Phase 17 (Actions: Read, single repository) -> **Delete** (it is called revoke in the task text). Then the local Keychain check: `security find-generic-password -s gsd17-gh-actions-read` must report not found (exit 44, already true). The token had a 1-day expiry per STATE.md, so it may already read "expired"; ask the user to still confirm it is gone from the list. [ASSUMED] for the exact menu labels.

Keep the answers' wording intact in the evidence file. A screenshot is acceptable, as in gg2 (`20-MILESTONE-CLOSE-RECORD.md` §3).

### Evidence file layout (Phase 17 precedent)

Use `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` in the Phase 17 format [VERIFIED: `17-EVIDENCE.md` L1-23, read this session]:
- Header: Phase, Branch (`sync/v1.7.0`), Purpose ("Nothing here was pushed."), Entry format.
- An `## Index` table (`Entry | Requirement | Verdict`).
- One `### E-NN: <title>` per entry: the command as run, the relevant output in a `text` fence, a UTC timestamp, and a last line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured (<reason>)`.

Suggested entries:

| Entry | Content | Requirement |
|-------|---------|-------------|
| E-01 | merge rehearsal result (clean, 2 files) and the real merge: `git show --stat HEAD`, `git merge-base --is-ancestor 6473e24f HEAD`, left-right count 0 | DEPS-01 |
| E-02 | `dependencyInsight` x3 before/after (table above) | DEPS-01 |
| E-03 | spotless baseline comparison and the user's decision (Open Question 1) | DEPS-01 |
| E-04 | `testUnit` result and the Lombok-warning check with its control | DEPS-01 |
| E-05 | gate without exemption (exactly one line) and with it (`anchor_exempt=1 result=PASS`) | DEPS-01, D-01 |
| E-06 | self-test: full run output; plus the fail-first run against the old gate | D-04 |
| E-07 | pin-guard red run (old regexes and stubs) with test names and messages | CI-18, CI-19, D-07 |
| E-08 | pin-guard green run, and the old-vs-new fixture table | CI-18, CI-19, D-09 |
| E-09 | OPS-01..03 answers verbatim plus the REST/Keychain facts and the final check at/after 12:00 UTC | OPS-01..03 |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `PRE_TAG` change to get past a published bump | SHA- and row-pinned exemption list (D-01..D-04) | this phase | `PRE_TAG` alone cannot work: invariant 0 requires `PRE_TAG` (or each parent of a merge tag) to be an ancestor of `54e63648`'s first parent, and `6473e24f` is not |
| `WRITE_SCOPE` `.*:\s*write\b.*` | scalar-value match plus flow-mapping match | this phase | fixes false failures on step text and closes the quoted `"write"` hole |
| Fork count recomputed in shell (`nproc / 2`) | `reportTestForks` Gradle task read by the step (CI-14) | Phase 17 | this phase pins it |

**Deprecated/outdated:** `.claude/CLAUDE.md` Gradle 7.4.2 and slf4j-reload4j 2.0.17 lines are stale (wrapper is 9.6.1; slf4j-reload4j 2.0.20 after the merge).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A SHA-keyed row cannot exempt its own commit, because the SHA does not exist when the row is written | Pattern 2 | Low: affects only the design rationale |
| A2 | GitHub UI labels: "Last checked", the config-error indicator, the "Check for updates" button and its effect, the disabled-workflow banner text, and the exact fine-grained-token menu names | Web-UI steps | Low: the orchestrator may need to adjust wording on the spot |
| A3 | The Phase 17 token (1-day expiry per STATE.md) has already expired | Runtime State Inventory | Low: the user still deletes it |
| A4 | `.claude/CLAUDE.md` L57 (slf4j 2.0.17) is a generated summary, safe to leave | Pattern 1 | Low: only the doc is stale |
| A5 | The user prefers no tree-wide reformat (inferred from the Phase 7/8/11 precedent, not stated for this phase) | Open Question 1 | Medium: the decision is the user's |

## Open Questions

1. **Reformat 46 files, or define the spotless gate as "unchanged from baseline"?** (needs the user, before plan 21-01 commits)
   - What we know: `spotlessCheck` is red today on 8.10.1 and identically red on 8.10.4 (46 files, import lines only, 37 upstream-identical files). The anchors survive a full `spotlessApply`. CONTEXT says "If spotless 8.10.4 demands reformatting, it is its own commit" and the ROADMAP wants `spotlessCheck` green before Phase 22. 8.10.4 demands nothing new: the violations predate the bump.
   - What's unclear: whether the user wants the reformat commit, which touches files Phases 22-23 will also edit (`ClickHouseWriter.java` is one of the 46) and 37 upstream-identical files that make future upstream merges noisier.
   - Recommendation: raise one `checkpoint:decision` in plan 21-01 with the facts above. Default recommendation is **not** to reformat, and to record the gate as "no new violations: `spotlessApply` diff on the merged tree equals the 8.10.1 baseline diff (md5 `3393873370f9b48d2239131eedba8ff8`, 46 files)", with the baseline check repeatable by the recipe in Pitfall 3. If the user chooses the reformat, it is its own commit after the merge, made with `spotlessApply` in the real tree, followed by the anchors check (`rows=59 bad=0`) and `testUnit`. Do not sign this off by relaying an answer.

2. **Should FORK-DELTA's "mechanical checks" paragraph (`docs/FORK-DELTA.md` L206) mention the exemption file?**
   - What we know: it names the script and `fork-anchors.txt` only. D-03 says raw numbers wait for Phase 24.
   - Recommendation: add one sentence in Phase 24 with the other FORK-DELTA edits, or in the plan-2 commit if the planner wants the doc to match the script immediately. Not required by any success criterion.

3. **Does the user want ST4's needle tightened?**
   - Recommendation: yes, to `TARGET_TABLE_FILTER`, as an additive strengthening in the same commit. Not required.

4. **`gh` presence (`/opt/homebrew/bin/gh`) versus STATE.md "not installed".**
   - Recommendation: do not use it; mention it to the user once. REST via `curl` is enough for everything in this phase.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| git | merge, gate, self-test | yes | 2.50.1 | none needed |
| bash | gate, self-test | yes | 3.2.57 (`/bin/bash`) | none: write bash-3.2-safe code |
| JDK | Gradle | yes | system 21.0.12.1; the build's toolchain is 17 and worked | none needed |
| Gradle wrapper | build, `testUnit`, `dependencyInsight` | yes | 9.6.1 | none needed |
| Docker (colima profile `vz`) | only for the full suite later (`testUnit` is Docker-free) | yes, running (Server 27.4.0) | | not needed this phase |
| python3 | helper one-liners, patch scripts | yes | 3.14.8 (run with `-I`) | `awk`/`sed` |
| curl | OPS REST checks | yes | 8.7.1 | `WebFetch` |
| network to `api.github.com` | OPS checks | yes (unauthenticated, 60 req/h) | | none |
| `security` (Keychain CLI) | OPS-03 lookup | yes | | none |
| `gh` | not used | present at `/opt/homebrew/bin/gh`, contradicts STATE.md | | do not use |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none.

## Validation Architecture

`workflow.nyquist_validation` is `false` in `.planning/config.json`, but the orchestrator asked for this section explicitly, so it is included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.11.3 via Gradle 9.6.1; bash self-test for the gate |
| Config file | `build.gradle.kts` (`testUnit` task excludes the `clickhouse` tag) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'` |
| Full suite command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` (about 10 s here: 556 tests) |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | Failing direction | File Exists? |
|--------|----------|-----------|-------------------|-------------------|-------------|
| DEPS-01 | Merge landed, no rebase | git check | `git merge-base --is-ancestor 6473e24f HEAD && git rev-list --left-right --count origin/custom_build...HEAD` (expect `0 N`); `git cat-file -p HEAD` shows two parents | exit 1 if not an ancestor | n/a |
| DEPS-01 | Versions resolve | build | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q dependencyInsight --dependency org.slf4j:slf4j-reload4j --configuration runtimeClasspath` and `grep -n 'lombok\|spotless' gradle/libs.versions.toml` | wrong version printed | n/a |
| DEPS-01 | `testUnit` green | unit | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` | any failure | yes |
| DEPS-01 | No Lombok warning | build + control | Pitfall 6 commands (expect 0 matches; control must show the warning) | non-zero count | n/a |
| DEPS-01 | `spotlessCheck` | **decision pending** | baseline-equality: `spotlessApply` diff md5 vs `3393873370f9b48d2239131eedba8ff8`, or green after a reformat commit | md5 differs: a NEW violation | n/a |
| DEPS-01 / D-01 | Gate passes with the exemption | script | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (background; expect `anchor_exempt=1 result=PASS`) | any `ANCHOR VIOLATION` | script yes |
| D-01 | Exemption is what saves it | script | `ANCHOR_EXEMPTIONS_FILE= bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (expect exactly one line, `6473e24f ... slf4j-reload4j:2.0.17 expected 1 got 0 (BLD-08)`, rc 1) | PASS here means the exemption is not needed, which is wrong | script yes |
| D-02, D-04 | Narrowness | self-test | `bash .planning/scripts/verify-merge-invariants-selftest.sh` (background, about 6.6 min; expect `selftest: no failures`) | ST5-ST9 cases FAIL; also run against the old gate: 7 FAIL expected | Wave 0 (new cases) |
| D-04 | Default lookup resolves `54e63648` | shell | `git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD \| tail -1` prints `54e6364858cd728e3427ce2058fc00380895c689` | other SHA | n/a |
| CI-18 | tests.yaml step reads `reportTestForks`; no shell `nproc` formula | unit | `... --tests '*TestsWorkflowPinGuardUnitTest'` | red against stubs, then green | Wave 0 |
| CI-18 | task present in `build.gradle.kts` and reads `maxParallelForks` from `test` | unit | same | red on fixtures: deleted, renamed, constant, other task, block comment | Wave 0 |
| CI-19 | `runs-on: ubuntu-24.04 # comment` accepted; step text not a write scope; real write scopes still rejected | unit | same | red on the old regexes: 3 fixture groups (table in Pattern 4) | Wave 0 |
| D-09 | four upstream workflows excluded, all listed paths exist | unit | same | red on missing/typo/stale/wrong-entry fixtures | Wave 0 |
| OPS-01 | no schedule run after the disable time | REST | the Code Examples `curl` queries, at or after `2026-10-17T12:00:00Z` | `total_count` > 0, or checked before 12:00 UTC | n/a |
| OPS-02 | gradle entry fine | user + REST | user's words; `curl $R/actions/workflows/384810012/runs` shows the gradle job | user reports an error / no recent check | n/a |
| OPS-03 | token revoked | user + local | user's words; `security find-generic-password -s gsd17-gh-actions-read >/dev/null 2>&1; echo $?` prints 44 | exit 0 (item present) | n/a |
| Phase-wide | No connector code changed | git | `git diff --stat <phase-base>..HEAD -- src/main` empty; `git diff --quiet <base> HEAD -- .github/workflows .github/dependabot.yml`; `git diff --quiet v1.7.0 HEAD -- .github/workflows/integrationTests.yaml` | non-empty diff | n/a |

### Sampling Rate
- **Per task commit:** the quick command above (pin guard) or `bash -n` on touched scripts.
- **Per wave merge:** `testUnit`, plus the new self-test cases.
- **Phase gate:** full self-test, the gate with and without the exemption (both in the background), `testUnit`, the OPS REST check at or after 12:00 UTC, and the user's own OPS answers recorded. Phase 21 is not closed before D-11's window.

### Wave 0 Gaps
- [ ] `src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java`: extracted rules, fixture tests, three real-file tests (fork step, build task, dependabot excludes). No framework install needed.
- [ ] `.planning/scripts/fork-anchor-exemptions.txt` (new data file).
- [ ] `.planning/scripts/verify-merge-invariants-selftest.sh`: ST5-ST9, comment fix.
- [ ] `21-EVIDENCE.md`.

## Security Domain

`security_enforcement` is on (absent = enabled; `security_asvs_level: 1`). This phase changes no connector code and handles no record data.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / supply chain | yes | Dependency bumps arrive only by the user's own merge of Dependabot PRs; the merge is rehearsed and gated; the CI pin guard keeps actions SHA-pinned and the token read-only |
| V2 Authentication | no | no auth code |
| V3 Session Management | no | none |
| V4 Access Control | yes (CI) | the pin guard pins `permissions: contents: read`; D-10 keeps `contents: write`, `write-all` and job-level blocks rejected, and the new `WRITE_SCALAR` also rejects the quoted `"write"` form that the old regex accepted |
| V5 Input Validation | yes (small) | exemption rows are validated strictly (`^[0-9a-f]{40}$`, 4 TAB fields, non-empty reason) and a malformed row fails the gate |
| V6 Cryptography | no | no crypto |
| V7/V8 Data protection & logging | yes | the evidence file and the OPS record contain no token value, no host credential and no record value. Name the Keychain item, never its content |
| V14 Configuration | yes | OPS-01 (a failing nightly job removed), OPS-02 (Dependabot config valid), OPS-03 (stale credential revoked) |

### Known Threat Patterns for this phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Exemption list used to hide a new anchor violation | Tampering | SHA-pinned to a published commit (a future commit's SHA is unknowable), row-pinned (ST6, ST7), strictly parsed (ST8), reviewed as data, and the gate output names `anchor_exempt=N` |
| Pin guard weakened by a regex tweak | Tampering | "must-reject" fixtures are permanent (D-05, D-10) |
| Long-lived read-only token left behind | Information disclosure | OPS-03: delete on GitHub, confirm the Keychain item is gone |
| Stale scheduled workflow running unmaintained code with repo secrets | Elevation of privilege | OPS-01: nightly disabled in settings, file left byte-identical to upstream; Phase 24 re-checks |
| A relayed or inferred "approval" for an OPS item | Spoofing | D-13: free text from the user in the session; a relayed answer is refused |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `.planning/scripts/verify-merge-invariants.sh` (L1-257), `verify-merge-invariants-selftest.sh` (L1-117), `fork-anchors.txt` (L1-75), `src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java` (L1-178), `.github/workflows/tests.yaml`, `.github/dependabot.yml`, `build.gradle.kts` (L245-270, L490-510), `lombok.config`, `docs/FORK-DELTA.md` (L55-70, L204-208), `.planning/{ROADMAP,STATE,REQUIREMENTS,config.json}`, 21-CONTEXT.md.
- Command output from rehearsals run this session: merge dry-run, gate runs A/B/C, self-test (full, new cases, old-gate, mutation), `dependencyInsight` x4 before/after, compile with and without `lombok.config`, `spotlessCheck`/`spotlessApply` before and after, `testUnit`, scratch pin-guard harness, REST queries, Keychain lookup.
- GitHub REST API (public, unauthenticated): workflows list, workflow `integrationTests.yaml`, its runs, `Dependabot Updates` runs, repo and PR lists.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW.md` (WR-01, IN-01, IN-02), `17-EVIDENCE.md` (layout), `20-release-publication/20-MILESTONE-CLOSE-RECORD.md` (OPS steps and wording).

### Secondary (MEDIUM confidence)
- [docs.github.com disabling-and-enabling-a-workflow](https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/disabling-and-enabling-a-workflow): Actions -> workflow -> "Show workflow options" kebab -> Disable workflow; scheduled-workflow auto-disable notes.
- [docs.github.com viewing-dependabot-job-logs](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/viewing-dependabot-job-logs): Insights -> Dependency graph -> Dependabot -> Recent update jobs -> view logs.

### Tertiary (LOW confidence)
- GitHub UI wording not covered by the docs fetched (A2): marked [ASSUMED] inline.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependencies, versions read from the build.
- Architecture (gate, self-test, pin guard): HIGH, prototyped and run.
- Pitfalls: HIGH for the git/spotless/Lombok findings (measured); MEDIUM for the UI labels.

**Research date:** 2026-10-17
**Valid until:** about 7 days for anything tied to `origin/custom_build` (`6473e24f`), because the user can merge more Dependabot PRs onto it. Re-run `git rev-parse origin/custom_build` before plan 21-01; if it moved, add one exemption row per newly published bump of an anchored line (D-02) and redo the rehearsal.
